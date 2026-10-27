package hwswbuilder;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

import static hwswbuilder.TestUtil.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Tests of how the tools react to failures of NuSMV and Prolog.
 */
@Timeout(120)
class ExternalToolFailureTest {
    @TempDir
    Path tmp;

    @BeforeEach
    void requirePosixShell() {
        assumeFalse(isWindows(), "fake tools are POSIX shell scripts");
    }

    private Path catSitting() throws Exception {
        return copyCaseStudy("cat_sitting", tmp).resolve("cat_sitting.conf");
    }

    @Test
    void nonZeroNuSMVExitStatusIsPropagated() throws Exception {
        final Path fake = fakeNuSMV(tmp, tmp.resolve("models.txt"), true, 3);
        final int[] exitCode = new int[1];
        captureStdout(() -> exitCode[0] = new Main().run(new String[] {
                catSitting().toString(), "--nusmvCommand", fake.toString(),
                "-s", "DELAY_APU=0; DELAY_ALU=0"}));
        assertEquals(3, exitCode[0]);
    }

    @Test
    void zeroNuSMVExitStatus() throws Exception {
        final Path fake = fakeNuSMV(tmp, tmp.resolve("models.txt"), true, 0);
        final int[] exitCode = new int[1];
        captureStdout(() -> exitCode[0] = new Main().run(new String[] {
                catSitting().toString(), "--nusmvCommand", fake.toString(),
                "-s", "DELAY_APU=0; DELAY_ALU=0"}));
        assertEquals(0, exitCode[0]);
    }

    @Test
    void symmetryCheckReportsNuSMVFailure() throws Exception {
        final Path fake = fakeNuSMV(tmp, tmp.resolve("models.txt"), true, 2);
        final String out = captureStdout(() -> Main.main(new String[] {
                catSitting().toString(), "--nusmvCommand", fake.toString(),
                "-s", "DELAY_APU=0; DELAY_ALU=0", "--checkSymmetry"}));
        assertTrue(out.contains("*** NuSMV failed to perform a symmetry check (exit status 2)."), out);
        assertFalse(out.contains("Symmetry check has passed"), out);
    }

    @Test
    void comprehensiveVerifierLogsNuSMVFailure() throws Exception {
        assumeTrue(commandWorks("cpp", "--version"), "cpp is not available");
        final Path dir = copyCaseStudy("pas", tmp);
        final Path requirements = tmp.resolve("requirements.txt");
        // the isolated pattern does not need Prolog
        Files.write(requirements, Collections.singletonList("[[REQ pattern=isolated viewpoint=PS_APU]] "
                + "LTLSPEC G TRUE"), StandardCharsets.UTF_8);
        final Path fake = fakeNuSMV(tmp, tmp.resolve("models.txt"), true, 4);
        final Path log0 = tmp.resolve("log_0f.txt");
        final Path log1 = tmp.resolve("log_1f.txt");
        captureStdout(() -> ComprehensiveVerifier.main(new String[] {
                dir.resolve("ps_sas_pacs_pas.conf").toString(),
                "--configSubstitutions", "MAX_DELAY_PS_APU=0; MAX_DELAY_PS_ALU=0; MAX_DELAY_SAS_APU=0; "
                        + "MAX_DELAY_SAS_ALU=0; MAX_DELAY_PACS=0; MAX_DELAY_PAS=0",
                "--prologFilename", tmp.resolve("tmp.pl").toString(),
                "--requirementsFilename", requirements.toString(),
                "--nusmvCommand", fake.toString(),
                "--logFilenameWithoutFailures", log0.toString(),
                "--logFilenameWithFailures", log1.toString()}));
        for (Path log : new Path[] { log0, log1 }) {
            final String content = read(log);
            assertTrue(content.contains("-- specification AG (fake) is true"), content);
            assertTrue(content.contains("-- NuSMV command exited with non-zero status 4"), content);
        }
    }

    private RuntimeException dominationGraphError(Path prolog) throws Exception {
        final Path conf = catSitting();
        return assertThrows(RuntimeException.class, () -> captureStdout(() -> Main.main(new String[] {
                conf.toString(), "-s", "DELAY_APU=0; DELAY_ALU=0",
                "--prologFilename", tmp.resolve("cat.pl").toString(),
                "--prologExecutable", prolog.toString(),
                "--generateDominationGraph", "SITTING_ALU"})));
    }

    @Test
    void prologErrorIsNotTreatedAsNegativeAnswer() throws Exception {
        final Path prolog = script(tmp, "erroneous-prolog.sh",
                "echo 'For built-in help, use ?- help(Topic). or ?- apropos(Word).'",
                "echo",
                "while read line; do echo 'ERROR: Unknown procedure: permutation/2'; done");
        final String message = dominationGraphError(prolog).getMessage();
        assertTrue(message.contains("Unexpected reply of Prolog interpreter"), message);
        assertTrue(message.contains("ERROR: Unknown procedure: permutation/2"), message);
    }

    @Test
    void prologTerminatingEarlyIsReported() throws Exception {
        final Path prolog = script(tmp, "silent-prolog.sh", "exit 0");
        final String message = dominationGraphError(prolog).getMessage();
        assertTrue(message.contains("terminated before printing its welcome message"), message);
    }

    @Test
    void prologTerminatingWithoutAnswerIsReported() throws Exception {
        final Path prolog = script(tmp, "quitting-prolog.sh",
                "echo 'For built-in help, use ?- help(Topic). or ?- apropos(Word).'",
                "echo",
                "exit 0");
        final String message = dominationGraphError(prolog).getMessage();
        assertTrue(message.contains("terminated without answering query dominates("), message);
    }
}
