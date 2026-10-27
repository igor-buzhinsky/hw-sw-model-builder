package hwswbuilder;

import hwswbuilder.command.Workspace;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static hwswbuilder.TestUtil.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end regression tests: the generated NuSMV models and Prolog representations of the bundled case studies
 * must stay byte-for-byte identical to the golden files.
 */
class GoldenModelTest {
    @TempDir
    Path tmp;

    private static final String CAT_CONF = "cat_sitting.conf";
    private static final String PAS_CONF = "ps_sas_pacs_pas.conf";

    private static String pasSubstitutions(String vanishingFailures, String pasFault, String psFault, String sasFault,
                                           int delayBefore, int delayAfter) {
        return String.join("; ",
                "VANISHING_FAILURES=" + vanishingFailures,
                "REQ_FILENAME=empty-requirements.smv",
                "PAS_FAULT_DIVISION=" + pasFault,
                "PS_FAULT_DIVISION=" + psFault,
                "SAS_FAULT_DIVISION=" + sasFault,
                "MAX_DELAY_PS_APU=" + delayBefore,
                "MAX_DELAY_PS_ALU=" + delayAfter,
                "MAX_DELAY_SAS_APU=" + delayBefore,
                "MAX_DELAY_SAS_ALU=" + delayAfter,
                "MAX_DELAY_PACS=" + delayAfter,
                "MAX_DELAY_PAS=" + delayBefore);
    }

    private Path pasCopy() throws Exception {
        final Path dir = copyCaseStudy("pas", tmp);
        Files.write(dir.resolve("empty-requirements.smv"), new byte[0]);
        return dir;
    }

    private String runMain(Path dir, String conf, String... extraArgs) throws Exception {
        final String[] args = new String[extraArgs.length + 1];
        args[0] = dir.resolve(conf).toString();
        System.arraycopy(extraArgs, 0, args, 1, extraArgs.length);
        final String stdout = captureStdout(() -> Main.main(args));
        assertTrue(stdout.startsWith("Processing "), stdout);
        return read(dir.resolve("out.smv"));
    }

    @Test
    void catSittingWithoutDelays() throws Exception {
        final Path dir = copyCaseStudy("cat_sitting", tmp);
        assertMatchesGolden("cat_sitting_no_delays.smv",
                runMain(dir, CAT_CONF, "--configSubstitutions", "DELAY_APU=0; DELAY_ALU=0"));
    }

    @Test
    void catSittingWithDelays() throws Exception {
        final Path dir = copyCaseStudy("cat_sitting", tmp);
        assertMatchesGolden("cat_sitting_delays.smv",
                runMain(dir, CAT_CONF, "-s", "DELAY_APU=2; DELAY_ALU=4"));
    }

    @Test
    void catSittingMimicMODCHK() throws Exception {
        final Path dir = copyCaseStudy("cat_sitting", tmp);
        assertMatchesGolden("cat_sitting_mimic_modchk.smv",
                runMain(dir, CAT_CONF, "-s", "DELAY_APU=1; DELAY_ALU=1", "--mimicMODCHK"));
    }

    @Test
    void catSittingProlog() throws Exception {
        final Path dir = copyCaseStudy("cat_sitting", tmp);
        final Path prolog = tmp.resolve("cat_sitting.pl");
        runMain(dir, CAT_CONF, "-s", "DELAY_APU=0; DELAY_ALU=0", "--prologFilename", prolog.toString(),
                "--prologIncludeNoFailures", "--prologIncludeCCF");
        assertMatchesGolden("cat_sitting.pl", read(prolog));
    }

    @Test
    void pasFaultFree() throws Exception {
        final Path dir = pasCopy();
        assertMatchesGolden("pas_fault_free.smv",
                runMain(dir, PAS_CONF, "-s", pasSubstitutions("false", "NA", "NA", "NA", 0, 0)));
    }

    @Test
    void pasWithFailuresAndDelays() throws Exception {
        final Path dir = pasCopy();
        assertMatchesGolden("pas_failures_delays.smv",
                runMain(dir, PAS_CONF, "-s", pasSubstitutions("true", "1", "2", "ALL", 3, 6)));
    }

    @Test
    void pasProlog() throws Exception {
        final Path dir = pasCopy();
        final Path prolog = tmp.resolve("pas.pl");
        runMain(dir, PAS_CONF, "-s", pasSubstitutions("false", "NA", "NA", "NA", 0, 0),
                "--prologFilename", prolog.toString());
        assertMatchesGolden("pas.pl", read(prolog));
    }

    @Test
    void pasConeOfInfluenceOptimization() throws Exception {
        final Path dir = pasCopy();
        final String conf = dir.resolve(PAS_CONF).toString();
        final String[] model = new String[1];
        captureStdout(() -> {
            final Workspace workspace = Main.createWorkspace(conf,
                    pasSubstitutions("false", "NA", "1", "NA", 0, 0), false, false, false);
            workspace.coiOptimize(workspace.getUnitByName("SAS_ALU"), 2);
            model[0] = workspace.toNuSMV();
        });
        assertMatchesGolden("pas_coi_sas_alu_div2.smv", model[0]);
    }
}
