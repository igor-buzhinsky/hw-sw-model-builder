package hwswbuilder.command;

import hwswbuilder.structures.Unit;
import hwswbuilder.structures.UnitGroup;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

/**
 * Builds the domination graph of the verification configurations of a viewpoint unit by querying a SWI-Prolog
 * interpreter loaded with the output of {@link PrologGenerator}.
 */
class DominationAnalyzer {
    private final Workspace workspace;
    private final PrologGenerator pg;

    DominationAnalyzer(Workspace workspace) {
        this.workspace = workspace;
        pg = new PrologGenerator(workspace);
    }

    List<List<Integer>> getFailureCombinations(boolean includeNAFailures, boolean includeALLFailures) {
        return pg.getFailureCombinations(includeNAFailures, includeALLFailures);
    }

    // returns essential configurations
    List<VerificationConfiguration> queryProlog(String prologInputFile, String prologExecutable, String unitName,
                                                boolean thoroughQueries, List<List<Integer>> failureCombinations)
            throws IOException {
        final Unit unit = workspace.getUnitByName(unitName);
        final UnitGroup examinedUnitGroup = pg.getUnitGroup(unit);
        int indexOfExaminedUnitGroup = 0;
        boolean indexFound = false;
        for (UnitGroup g : workspace.unitGroups.values()) {
            if (g == examinedUnitGroup) {
                indexFound = true;
                break;
            }
            indexOfExaminedUnitGroup++;
        }
        if (!indexFound) {
            throw new AssertionError("Failed to find the unit group of unit "
                    + unit.toNuSMV() + ".");
        }

        final List<String> nodeNames = new ArrayList<>();
        final List<VerificationConfiguration> nodeVerificationConfigurations = new ArrayList<>();
        for (int div = 1; div <= unit.divisions; div++) {
            for (List<Integer> failureCombination : failureCombinations) {
                // looking at the failing division makes no sense --> exclude from the graph
                final int failingDivision = failureCombination.get(indexOfExaminedUnitGroup);
                if (failingDivision == div || failingDivision == Command.ALL_INDEX) {
                    continue;
                }
                final VerificationConfiguration conf = new VerificationConfiguration(unit, div, failureCombination);
                nodeNames.add(pg.getOverallConfigurationName(failureCombination, unit, div));
                nodeVerificationConfigurations.add(conf);
            }
        }
        final DominationGraph dg = new DominationGraph(nodeNames, nodeVerificationConfigurations, !thoroughQueries);

        System.out.println("Running prolog interpreter: " + prologExecutable);
        final Process process = new ProcessBuilder(Arrays.asList(prologExecutable, prologInputFile))
                .redirectErrorStream(true).start();
        try (PrintWriter pw = new PrintWriter(process.getOutputStream(), true);
             Scanner sc = new Scanner(process.getInputStream())) {
            String line;
            do {
                line = nextLine(sc, prologExecutable);
            } while (!line.startsWith("For help, use") && !line.startsWith("For built-in help, use"));
            nextLine(sc, prologExecutable);
            for (String nodeName1 : nodeNames) {
                for (String nodeName2 : nodeNames) {
                    final String query = "dominates(" + nodeName1 + ", " + nodeName2 + ")";
                    System.out.print("Query: " + query + " --> Reply: ");
                    final Boolean currentValue = dg.incident(nodeName1, nodeName2);
                    if (currentValue != null) {
                        System.out.println(currentValue + " (inferred without asking prolog)");
                        continue;
                    }
                    pw.println(query + ".");
                    if (!sc.hasNext()) {
                        throw new RuntimeException("Prolog interpreter " + prologExecutable
                                + " terminated without answering query " + query);
                    }
                    String token = sc.next();
                    final boolean complete = token.endsWith(".");
                    if (complete) {
                        token = token.substring(0, token.length() - 1);
                    }
                    if (!token.equals("true") && !token.equals("false")) {
                        final String rest = sc.hasNextLine() ? sc.nextLine() : "";
                        throw new RuntimeException("Unexpected reply of Prolog interpreter " + prologExecutable
                                + " to query " + query + ": " + (complete ? token + "." : token) + rest);
                    }
                    if (!complete) {
                        pw.println(".");
                    }
                    final boolean outcome = Boolean.parseBoolean(token);
                    dg.setIncident(nodeName1, nodeName2, outcome);
                    System.out.println(outcome);
                }
            }
        } finally {
            process.destroy();
        }
        dg.checkReflexivity();
        dg.checkTransitivity();
        dg.findEssentialConfigurations();
        System.out.println(dg.toString());
        return dg.essentialVerificationConfigurations();
    }

    private static String nextLine(Scanner sc, String prologExecutable) {
        if (!sc.hasNextLine()) {
            throw new RuntimeException("Prolog interpreter " + prologExecutable + " terminated before printing "
                    + "its welcome message (is it SWI-Prolog?)");
        }
        return sc.nextLine();
    }
}
