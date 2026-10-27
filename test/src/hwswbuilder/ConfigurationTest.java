package hwswbuilder;

import hwswbuilder.command.Workspace;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests of placeholder substitution and of the parsing and validation of configuration files.
 */
class ConfigurationTest {
    @TempDir
    Path tmp;

    // --- placeholder substitution ---

    @Test
    void substitutesPlaceholders() {
        assertEquals("max_delay=3 file=x.smv",
                Main.substitutePlaceholders("max_delay=${D} file=${F}", "D=3; F=x.smv", false));
    }

    @Test
    void substitutionsMaySkipSpacesAfterSemicolons() {
        assertEquals("a=1 b=2", Main.substitutePlaceholders("a=${A} b=${B}", "A=1;B=2", false));
    }

    @Test
    void substitutesBracesInValues() {
        assertEquals("nusmv_type={3,3}", Main.substitutePlaceholders("nusmv_type=${V1}", "V1={3,3}", false));
    }

    @Test
    void lineWithoutPlaceholdersIsUnchanged() {
        assertEquals("unit_group name=G", Main.substitutePlaceholders("unit_group name=G", null, false));
    }

    @Test
    void unfilledPlaceholderIsAnError() {
        final RuntimeException e = assertThrows(RuntimeException.class,
                () -> Main.substitutePlaceholders("x=${MISSING}", "OTHER=1", false));
        assertTrue(e.getMessage().contains("${MISSING}"), e.getMessage());
        assertTrue(e.getMessage().contains("--configSubstitutions MISSING=<value>"), e.getMessage());
    }

    @Test
    void unfilledPlaceholdersMayBeReplacedWithNA() {
        assertEquals("a=NA b=2", Main.substitutePlaceholders("a=${A} b=${B}", "B=2", true));
    }

    @Test
    void malformedSubstitutionIsAnError() {
        assertThrows(RuntimeException.class, () -> Main.substitutePlaceholders("a=${A}", "A", false));
        assertThrows(RuntimeException.class, () -> Main.substitutePlaceholders("a=${A}", "A=1=2", false));
    }

    // --- configuration files ---

    private static final String UNIT_MODULE = String.join("\n",
            "MODULE NOT_GATE(IN, IN_FAULT, IN_CONNECTED)",
            "DEFINE",
            "    OUT := !IN;",
            "    OUT_FAULT := IN_FAULT;");

    private static final List<String> HEADER = Arrays.asList(
            "settings basic_blocks_filename=blocks.smv output_filename=out.smv",
            "unit_group name=G divisions=2 failing_division=NA",
            "input in=G name=X nusmv_type=boolean",
            "output in=G name=Y",
            "unit in=G name=U filename=unit.smv nusmv_module_name=NOT_GATE max_delay=0 "
                    + "nusmv_outputs=OUT:boolean single_division_to_retain=NA");

    private Workspace load(String substitutions, String... extraLines) throws IOException {
        Files.write(tmp.resolve("blocks.smv"), new byte[0]);
        Files.write(tmp.resolve("unit.smv"), UNIT_MODULE.getBytes(StandardCharsets.UTF_8));
        final List<String> lines = new ArrayList<>(HEADER);
        lines.addAll(Arrays.asList(extraLines));
        final Path conf = tmp.resolve("test.conf");
        Files.write(conf, lines, StandardCharsets.UTF_8);
        return Main.createWorkspace(conf.toString(), substitutions, false, false, false);
    }

    private String loadError(String... extraLines) {
        return assertThrows(RuntimeException.class, () -> load(null, extraLines)).getMessage();
    }

    @Test
    void minimalConfigurationProducesModel() throws IOException {
        final Workspace w = load(null,
                "",
                "   # comment line",
                "parallel_connection from=input.X to=G.U # trailing comment",
                "parallel_connection from=G.U.OUT to=output.Y");
        final String model = w.toNuSMV();
        assertTrue(model.contains("MODULE main"), model);
        assertTrue(model.contains("NOT_GATE_DIV1: WRAPPER_NOT_GATE_DIV1("), model);
        assertTrue(model.contains("NOT_GATE_DIV2: WRAPPER_NOT_GATE_DIV2("), model);
        assertTrue(model.contains("Y_DIV1 := NOT_GATE_DIV1.OUT;"), model);
        assertTrue(model.contains("FAILURE := NOT_GATE_DIV1.FAILURE | NOT_GATE_DIV2.FAILURE;"), model);
        assertEquals(tmp.resolve("out.smv").toString(), w.outputFilename());
    }

    @Test
    void unitGroupsOnlyModeIgnoresOtherCommands() throws IOException {
        final Path conf = tmp.resolve("groups.conf");
        Files.write(conf, Arrays.asList(
                "unit_group name=A divisions=4 failing_division=${A_FAIL}",
                "this_is_not_a_command but=ignored"), StandardCharsets.UTF_8);
        final Workspace w = Main.createWorkspace(conf.toString(), null, false, true, false);
        assertEquals(Arrays.asList("A"), new ArrayList<>(w.unitGroups().keySet()));
        assertEquals(4, w.unitGroups().get("A").divisions);
    }

    @Test
    void unknownCommand() {
        assertTrue(loadError("frobnicate x=1").contains("Unknown command in configuration file: frobnicate"));
    }

    @Test
    void argumentWithoutValue() {
        assertTrue(loadError("parallel_connection from").contains("Invalid command argument: from"));
    }

    @Test
    void missingMandatoryArgument() {
        assertTrue(loadError("parallel_connection from=input.X")
                .contains("Mandatory argument to is missing"));
    }

    @Test
    void failingDivisionOutOfRange() {
        assertTrue(loadError("unit_group name=H divisions=2 failing_division=3")
                .contains("failing_division is set to 3 but may not be greater than 2"));
    }

    @Test
    void nonNumericDivisions() {
        assertTrue(loadError("unit_group name=H divisions=two failing_division=NA")
                .contains("Invalid value for divisions: two"));
    }

    @Test
    void unitGroupMustBeDeclaredFirst() {
        assertTrue(loadError("input in=NOPE name=Z nusmv_type=boolean")
                .contains("Unit group not (yet?) created: NOPE"));
    }

    @Test
    void unknownEntity() {
        assertTrue(loadError("parallel_connection from=foo.X to=G.U")
                .contains("Unknown entity name foo"));
    }

    @Test
    void unknownUnitOutput() {
        assertTrue(loadError("parallel_connection from=G.U.NOPE to=output.Y")
                .contains("has no output NOPE"));
    }

    @Test
    void divisionIndexOutOfRange() {
        assertTrue(loadError("single_connection from=input.X.3 to=G.U.1")
                .contains("index is set to 3 but may not be greater than 2"));
    }

    @Test
    void invalidConstant() {
        assertTrue(loadError("parallel_connection from=const.maybe to=G.U")
                .contains("Incorrect constant maybe"));
    }

    @Test
    void parallelConnectionRequiresMatchingDivisions() {
        assertTrue(loadError(
                "unit_group name=H divisions=3 failing_division=NA",
                "input in=H name=Z nusmv_type=boolean",
                "parallel_connection from=input.Z to=G.U")
                .contains("must have the same number of dimensions"));
    }

    @Test
    void allToAllConnectionRequiresUnitDestination() {
        assertTrue(loadError("all_to_all_connection from=input.X to=output.Y")
                .contains("The destination of an all-to-all connection must be a unit"));
    }

    @Test
    void outputMustBeFullyConnected() throws IOException {
        final Workspace w = load(null, "parallel_connection from=input.X to=G.U");
        final RuntimeException e = assertThrows(RuntimeException.class, w::toNuSMV);
        assertTrue(e.getMessage().contains("output Y requires 2 values but 0 are connected"), e.getMessage());
    }

    @Test
    void invalidSymmetryIndices() {
        assertTrue(loadError("symmetry group=G unit=U input_variable_indices=1,x")
                .contains("Formatting error in comma-separated list of integers"));
        assertTrue(loadError("symmetry group=G unit=U input_variable_indices=0")
                .contains("Invalid input variable index 0"));
    }

    @Test
    void placeholdersInConfiguration() throws IOException {
        final Workspace w = load("VANISH=true", "settings vanishing_failures=${VANISH}",
                "parallel_connection from=input.X to=G.U",
                "parallel_connection from=G.U.OUT to=output.Y");
        assertTrue(w.vanishingFailures);
    }
}
