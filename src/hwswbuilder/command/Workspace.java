package hwswbuilder.command;

import hwswbuilder.CustomModuleHandler;
import hwswbuilder.NameSubstitutionRegistry;
import hwswbuilder.structures.*;
import org.apache.commons.lang3.tuple.Pair;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static hwswbuilder.structures.NamedEntity.PAD;
import static hwswbuilder.structures.NamedEntity.SEP;

public class Workspace {
    String basicBlocksFilename;
    String outputFilename;
    final List<String> requirementsFilenames = new ArrayList<>();
    final Map<String, Input> inputs = new LinkedHashMap<>();
    final Map<String, Output> outputs = new LinkedHashMap<>();
    final Map<String, UnitGroup> unitGroups = new LinkedHashMap<>();
    final Map<Pair<UnitGroup, String>, Unit> units = new LinkedHashMap<>();
    public final CustomModuleHandler handler = new CustomModuleHandler();
    public final NameSubstitutionRegistry registry = new NameSubstitutionRegistry();

    private final List<String> varDeclarations = new ArrayList<>();
    private final List<String> defineDeclarations = new ArrayList<>();
    private final List<String> failureChunks = new ArrayList<>();

    public final boolean mimicMODCHK;

    // symmetry information of units with respect to their input variables
    private final List<Pair<Unit, List<Integer>>> symmetries = new ArrayList<>();

    public Workspace(boolean mimicMODCHK) {
        this.mimicMODCHK = mimicMODCHK;
    }

    public void addVar(String s) {
        varDeclarations.add(PAD + s + ";");
    }

    public void addDefine(String s) {
        defineDeclarations.add(PAD + s + ";");
    }

    public void addFailureChunk(String s) {
        failureChunks.add(s);
    }

    public static String failureFlagDeclaration(List<String> chunks) {
        return PAD + "FAILURE := " + (chunks.isEmpty() ? "FALSE"
                : String.join(" | ", chunks)) + ";";
    }

    private String getBody() {
        return String.join(SEP, new ArrayList<String>() {{
            add("VAR");
            addAll(varDeclarations);
            add("DEFINE");
            addAll(defineDeclarations);
            add(failureFlagDeclaration(failureChunks));
        }});
    }

    private String basicBlocksCode() throws IOException {
        return new String(Files.readAllBytes(Paths.get(basicBlocksFilename)));
    }

    private String getConfigDir(String configFilename) {
        final String dir = new File(configFilename).getParent();
        return dir == null ? "." : dir;
    }

    private String unitCode(String dir, Unit unit) throws IOException {
        return new String(Files.readAllBytes(Paths.get(dir, unit.nusmvFilename())));
    }

    public String toNuSMV(String configFilename) throws IOException {
        final StringBuilder sb = new StringBuilder();

        sb.append(basicBlocksCode()).append(SEP);

        final String dir = getConfigDir(configFilename);
        for (Unit unit : units.values()) {
            sb.append(SEP).append(unitCode(dir, unit)).append(SEP);
        }

        sb.append("MODULE main").append(SEP);

        Stream.of(inputs, units, outputs).map(Map::values)
                .forEach(x -> x.forEach(y -> y.nuSMVDeclaration(this)));

        sb.append(getBody()).append(SEP);

        for (String filename : requirementsFilenames) {
            sb.append(SEP).append(new String(Files.readAllBytes(Paths.get(filename))));
        }
        // also include all ad-hoc generated auxiliary modules
        System.out.println(registry);
        return handler.nuSMVDeclaration() + registry.replaceWithFinal(sb.toString());
    }

    public String outputFilename() {
        return outputFilename;
    }

    void addSymmetry(Unit unit, List<Integer> inputVariableIndices) {
        symmetries.add(Pair.of(unit, inputVariableIndices));
    }

    /**
     * Taken from
     * https://www.programcreek.com/2014/06/leetcode-next-permutation-java/
     */
    static class NextPermutation {
        static void nextPermutation(int[] nums) {
            //find first decreasing digit
            int mark = -1;
            for (int i = nums.length - 1; i > 0; i--) {
                if (nums[i] > nums[i - 1]) {
                    mark = i - 1;
                    break;
                }
            }

            if (mark == -1) {
                reverse(nums, 0, nums.length - 1);
                return;
            }

            int idx = nums.length - 1;
            for (int i = nums.length - 1; i >= mark+1; i--) {
                if (nums[i] > nums[mark]) {
                    idx = i;
                    break;
                }
            }

            swap(nums, mark, idx);

            reverse(nums, mark + 1, nums.length - 1);
        }

        static void swap(int[] nums, int i, int j) {
            int t = nums[i];
            nums[i] = nums[j];
            nums[j] = t;
        }

        static void reverse(int[] nums, int i, int j) {
            while (i < j) {
                swap(nums, i, j);
                i++;
                j--;
            }
        }
    }

    public void checkSymmetries(String configFilename, String nusmvCommand) throws IOException {
        final String name1 = "original";
        final String name2 = "permuted";
        final Function<Integer, Integer> factorial =
                n -> IntStream.rangeClosed(1, n).reduce(1, (x, y) -> x * y);
        for (Pair<Unit, List<Integer>> pair : symmetries) {
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
                sb.append(basicBlocksCode()).append(SEP);
                final String dir = getConfigDir(configFilename);
                sb.append(SEP).append(unitCode(dir, unit)).append(SEP);
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
                final List<String> args = new ArrayList<>(Arrays.asList(nusmvCommand
                        .split(" +")));
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
                            //System.out.println(line);
                        }
                    }
                }
                final String runMessage = "You can run NuSMV manually on the generated file "
                        + filename + " to investigate the cause.";
                if (outcome == null) {
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
