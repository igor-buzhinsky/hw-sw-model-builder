package hwswbuilder.command;

import hwswbuilder.Processes;
import hwswbuilder.structures.Unit;
import org.apache.commons.lang3.tuple.Pair;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.IntStream;

import static hwswbuilder.structures.NamedEntity.SEP;

/**
 * Checks the symmetry declarations of a workspace by running NuSMV on models that compare a unit with its copy
 * whose input variables are permuted.
 */
class SymmetryChecker {
    private final Workspace workspace;

    SymmetryChecker(Workspace workspace) {
        this.workspace = workspace;
    }

    void checkSymmetries(String nusmvCommand) throws IOException {
        final String name1 = "original";
        final String name2 = "permuted";
        final Function<Integer, Integer> factorial =
                n -> IntStream.rangeClosed(1, n).reduce(1, (x, y) -> x * y);
        for (Pair<Unit, List<Integer>> pair : workspace.symmetries) {
            final Unit unit = pair.getLeft();

            // sorted list of input variables to permute
            final List<Integer> inputVarIndices = new ArrayList<>(pair.getRight());
            inputVarIndices.sort(Integer::compareTo);

            // identity order
            final int[] identityOrder = new int[unit.symmetryInputVariableNumber()];
            for (int i = 0; i < identityOrder.length; i++) {
                identityOrder[i] = i + 1;
            }

            // current permutation of indices (repeat for each permutation)
            final int[] permutation = new int[inputVarIndices.size()];
            for (int i = 0; i < permutation.length; i++) {
                permutation[i] = i + 1;
            }

            final int iterations = factorial.apply(permutation.length);
            System.out.println(String.format("*** Checking unit %s for symmetry w.r.t. input variables with indices " +
                    "%s...", unit, inputVarIndices));
            for (int i = 0; i < iterations; i++) {
                System.out.println(String.format("Trying permutation %s...", Arrays.toString(permutation)));
                final StringBuilder sb = new StringBuilder();
                sb.append(workspace.basicBlocksCode()).append(SEP);
                sb.append(SEP).append(unit.nusmvCode()).append(SEP);
                sb.append("MODULE main").append(SEP);
                sb.append("VAR").append(SEP);
                sb.append(unit.symmetryInputDeclarations());

                final int[] newOrder = new int[identityOrder.length];
                System.arraycopy(identityOrder, 0, newOrder, 0, newOrder.length);
                int currentPosition = 0;
                for (int j = 0; j < newOrder.length; j++) {
                    if (identityOrder[j] == inputVarIndices.get(currentPosition)) {
                        // replace with permuted value
                        newOrder[j] = identityOrder[inputVarIndices.get(permutation[currentPosition++] - 1) - 1];
                    }
                    if (currentPosition == inputVarIndices.size()) {
                        break;
                    }
                }

                sb.append(unit.symmetryDeclaration(name1, identityOrder)).append(SEP);
                sb.append(unit.symmetryDeclaration(name2, newOrder)).append(SEP);
                sb.append(unit.symmetryCheckingSpec(name1, name2)).append(SEP);

                // write to temp file
                final String filename = ".symmetry-check.smv";
                try (PrintWriter pw = new PrintWriter(new File(filename))) {
                    pw.println(sb.toString());
                }

                // run NuSMV
                final List<String> args = new ArrayList<>(Arrays.asList(nusmvCommand.split(" +")));
                args.add(filename);
                final Process p = new ProcessBuilder().redirectErrorStream(true).command(args).start();
                final InputStream out = p.getInputStream();
                Boolean outcome = null;
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(out))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.startsWith("-- specification ")) {
                            if (line.endsWith(" is true")) {
                                outcome = true;
                            } else if (line.endsWith(" is false")) {
                                outcome = false;
                            }
                        }
                    }
                }
                final int exitCode = Processes.waitFor(p);
                final String runMessage = "You can run NuSMV manually on the generated file "
                        + filename + " to investigate the cause.";
                if (exitCode != 0) {
                    System.out.println("*** NuSMV failed to perform a symmetry check (exit status " + exitCode
                            + "). " + runMessage);
                    return;
                } else if (outcome == null) {
                    System.out.println("*** NuSMV failed to perform a symmetry check. " + runMessage);
                    return;
                } else if (!outcome) {
                    System.out.print("*** Symmetry check failed with ");
                    if (i == 0) {
                        System.out.println("the identity variable permutation, which means that "
                                + "unit " + unit + " is IO-nondeterministic. " + runMessage);
                    } else {
                        System.out.println("variable permutation " + Arrays.toString(permutation)
                                + ". " + runMessage);
                    }
                    return;
                }

                NextPermutation.nextPermutation(permutation);
            }
            System.out.println("*** Symmetry check has passed!" + SEP);
        }
    }
}
