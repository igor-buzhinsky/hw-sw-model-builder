package hwswbuilder;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Arrays;

import static hwswbuilder.TestUtil.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Tests of the features that call external programs. NuSMV is replaced with a shell script that records the models
 * it is given, so the tests check exactly what the tool asks the model checker to verify. The comprehensive verifier
 * test additionally needs the C preprocessor (cpp) and SWI-Prolog; the Prolog executable may be set with
 * -Dhwswbuilder.prolog=... (default: swipl).
 */
class ExternalToolsTest {
    @TempDir
    Path tmp;

    @BeforeEach
    void requirePosixShell() {
        assumeFalse(isWindows(), "fake NuSMV is a POSIX shell script");
    }

    private String checkSymmetry(boolean fakeOutcome, Path record) throws Exception {
        final Path dir = copyCaseStudy("cat_sitting", tmp);
        final Path fake = fakeNuSMV(tmp, record, fakeOutcome);
        return captureStdout(() -> Main.main(new String[] {
                dir.resolve("cat_sitting.conf").toString(),
                "--nusmvCommand", fake.toString(),
                "--configSubstitutions", "DELAY_APU=0; DELAY_ALU=0",
                "--checkSymmetry"}));
    }

    /**
     * Splits the record of the fake NuSMV into individual models and returns the SHA-256 digest of each one.
     */
    private static String digestPerModel(String record) throws Exception {
        final StringBuilder sb = new StringBuilder();
        for (String model : record.replace("\r\n", "\n").split("(?m)^(?==== )")) {
            final byte[] hash = MessageDigest.getInstance("SHA-256").digest(model.getBytes(StandardCharsets.UTF_8));
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            sb.append("  ").append(model.substring(0, model.indexOf('\n'))).append("\n");
        }
        return sb.toString();
    }

    @Test
    void symmetryCheckPasses() throws Exception {
        final Path record = tmp.resolve("models.txt");
        final String out = checkSymmetry(true, record);
        // APU: 1! = 1 permutation, ALU: 3! = 6 permutations
        assertEquals(7, out.split("Trying permutation ", -1).length - 1, out);
        assertEquals(2, out.split("Symmetry check has passed!", -1).length - 1, out);
        assertMatchesGolden("cat_sitting_symmetry_models.txt", read(record));
    }

    @Test
    void symmetryCheckReportsNondeterminism() throws Exception {
        final Path record = tmp.resolve("models.txt");
        final String out = checkSymmetry(false, record);
        assertTrue(out.contains("Symmetry check failed with the identity variable permutation, which means that "
                + "unit SITTING_APU is IO-nondeterministic."), out);
        assertEquals(1, out.split("Trying permutation ", -1).length - 1, out);
    }

    @Test
    @Timeout(600)
    void comprehensiveVerifierSelectsEssentialConfigurations() throws Exception {
        final String prolog = System.getProperty("hwswbuilder.prolog", "swipl");
        assumeTrue(commandWorks("cpp", "--version"), "cpp is not available");
        assumeTrue(commandWorks(prolog, "--version"), "Prolog (" + prolog + ") is not available");

        final Path dir = copyCaseStudy("pas", tmp);
        final Path requirements = tmp.resolve("requirements.txt");
        Files.write(requirements, Arrays.asList(
                "#define ALWAYS(x) G (x)",
                "[[REQ pattern=ltl viewpoint=SAS_ALU single_failures=PAS,SAS]] "
                        + "LTLSPEC ALWAYS(SAS_ALU[[viewpoint_division]].RODS_DOWN -> TRUE)",
                // same viewpoint and failures: the Prolog analysis result must be reused
                "[[REQ pattern=ltl viewpoint=SAS_ALU single_failures=PAS,SAS]] "
                        + "LTLSPEC ALWAYS(SAS_ALU[[viewpoint_division]].RODS_UP -> TRUE)",
                "[[REQ pattern=ltl viewpoint=PACS2 single_failures=PAS,PS all_failures=SAS]] "
                        + "LTLSPEC ALWAYS(!(PACS2[[viewpoint_division]].CMDOFF & PACS2[[viewpoint_division]].CMDON))",
                // here, some configurations are strictly dominated by others
                "[[REQ pattern=ltl viewpoint=SAS_ALU single_failures=PS,PAS,SAS]] "
                        + "LTLSPEC ALWAYS(SAS_ALU[[viewpoint_division]].RODS_UP -> TRUE)",
                "[[REQ pattern=isolated viewpoint=PS_APU]] "
                        + "LTLSPEC ALWAYS(PS_APU[[viewpoint_division]].HLEG_P > 70 -> TRUE)",
                "[[REQ pattern=ag_ef viewpoint=PAS]] CTLSPEC AG EF PAS_DIV1.RODS_UP_GROUP_1",
                "-- lines without annotations are ignored",
                "LTLSPEC G TRUE"
        ), StandardCharsets.UTF_8);

        final Path record = tmp.resolve("models.txt");
        final Path fake = fakeNuSMV(tmp, record, true);
        final Path log0 = tmp.resolve("log_0f.txt");
        final Path log1 = tmp.resolve("log_1f.txt");
        final String out = captureStdout(() -> ComprehensiveVerifier.main(new String[] {
                dir.resolve("ps_sas_pacs_pas.conf").toString(),
                "--configSubstitutions", "MAX_DELAY_PS_APU=0; MAX_DELAY_PS_ALU=0; MAX_DELAY_SAS_APU=0; "
                        + "MAX_DELAY_SAS_ALU=0; MAX_DELAY_PACS=0; MAX_DELAY_PAS=0",
                "--prologFilename", tmp.resolve("tmp.pl").toString(),
                "--prologExecutable", prolog,
                "--requirementsFilename", requirements.toString(),
                "--nusmvCommand", fake.toString(),
                "--logFilenameWithoutFailures", log0.toString(),
                "--logFilenameWithFailures", log1.toString(),
                "--optimizeOutUnreachable"}));

        assertTrue(out.contains("6 requirements:"), out);
        // four Prolog analyses: two for SAS_ALU (the first one is reused once), PACS2, PAS;
        // none for the isolated pattern
        assertEquals(4, out.split("Running prolog interpreter", -1).length - 1, out);
        // the tool treats Prolog errors as negative answers, so make sure that Prolog actually works
        assertTrue(out.matches("(?s).*Reply: true\\R.*"), "Prolog never confirmed a domination: " + out);
        assertMatchesGolden("comprehensive_log_0f.txt", read(log0));
        assertMatchesGolden("comprehensive_log_1f.txt", read(log1));
        // the models themselves are large, so only their digests are compared
        assertMatchesGolden("comprehensive_models.sha256", digestPerModel(read(record)));
    }
}
