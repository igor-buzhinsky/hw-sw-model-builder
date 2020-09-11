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
import java.util.stream.Collectors;
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
    final String configFilename;
    public final CustomModuleHandler handler = new CustomModuleHandler();
    public final NameSubstitutionRegistry registry = new NameSubstitutionRegistry();

    private final List<String> varDeclarations = new ArrayList<>();
    private final List<String> defineDeclarations = new ArrayList<>();
    private final List<String> failureChunks = new ArrayList<>();

    public final boolean mimicMODCHK;

    public Map<String, UnitGroup> unitGroups() {
        return new LinkedHashMap<>(unitGroups);
    }

    // if true, then failures can deterministically disappear (useful in deadlock checking)
    public boolean vanishingFailures = false;

    // symmetry information of units with respect to their input variables
    private final List<Pair<Unit, List<Integer>>> symmetries = new ArrayList<>();

    public Workspace(String configFilename, boolean mimicMODCHK) {
        this.configFilename = configFilename;
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

    public String configDir() {
        final String dir = new File(configFilename).getParent();
        return dir == null ? "." : dir;
    }

    public String toNuSMV() throws IOException {
        final StringBuilder sb = new StringBuilder();

        sb.append(basicBlocksCode()).append(SEP);

        for (Unit unit : units.values()) {
            sb.append(SEP).append(unit.nusmvCode()).append(SEP);
        }

        sb.append("MODULE main").append(SEP);

        // failure vanishing (for deadlock checking)
        if (vanishingFailures) {
            sb.append("VAR FAILURE_VANISHED: boolean;").append(SEP);
            //sb.append("TRANS FAILURE_VANISHED -> next(FAILURE_VANISHED);").append(SEP);
            sb.append("ASSIGN next(FAILURE_VANISHED) := {FAILURE_VANISHED, TRUE};").append(SEP);
        } else {
            sb.append("DEFINE FAILURE_VANISHED := FALSE;").append(SEP);
        }

        Stream.of(inputs, units, outputs).map(Map::values)
                .forEach(x -> x.forEach(y -> y.nuSMVDeclaration(this)));

        sb.append(getBody()).append(SEP);

        for (String filename : requirementsFilenames) {
            sb.append(SEP).append(new String(Files.readAllBytes(Paths.get(filename))));
        }
        // also include all ad-hoc generated auxiliary modules
        // System.out.println(registry);
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

    public static String configSafeIndex(int index) {
        return index == Command.NA_INDEX ? "NA" : index == Command.ALL_INDEX ? "ALL" : String.valueOf(index);
    }

    public void checkSymmetries(String nusmvCommand) throws IOException {
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

    public static List<List<Integer>> cartesianProduct(List<List<Integer>> lists) {
        return cartesianProduct(lists.size() - 1, lists);
    }

    private static List<List<Integer>> cartesianProduct(int index, List<List<Integer>> lists) {
        final List<List<Integer>> result = new ArrayList<>();
        if (index == -1) {
            result.add(new ArrayList<>());
        } else {
            for (int numToAdd : lists.get(index)) {
                for (List<Integer> list : cartesianProduct(index - 1, lists)) {
                    final List<Integer> added = new ArrayList<>(list);
                    added.add(numToAdd);
                    result.add(added);
                }
            }
        }
        return result;
    }

    private class PrologGenerator {
        private final Map<Unit, List<List<Integer>>> symmetryMap = new HashMap<>();
        private final List<String> generatedStrings = new ArrayList<>();
        private final Set<Pair<NamedEntity<?>, Integer>> processedCalls = new HashSet<>();
        private final Set<String> processedConstCalls = new HashSet<>();

        private LinkedHashMap<UnitGroup, Integer> chosenFailures = null;
        private String prefix = null;

        PrologGenerator() {
            for (Unit u : units.values()) {
                symmetryMap.put(u, new ArrayList<>());
            }
            for (Pair<Unit, List<Integer>> pair : symmetries) {
                symmetryMap.get(pair.getLeft()).add(pair.getRight());
            }
        }

        private UnitGroup getUnitGroup(NamedEntity<?> input) {
            for (NamedEntity<?> current = input; current != null; current = current.parent()) {
                if (current instanceof UnitGroup) {
                    return (UnitGroup) current;
                }
            }
            throw new AssertionError();
        }

        private void addConfFact(String moduleName, Integer division, List<List<List<String>>> prologChildren) {
            final String divisionString = division == null ? "" : "_DIV" + division;
            generatedStrings.add("configuration(" + prefix + moduleName + divisionString
                    + ", m_" + moduleName + ", " + prologChildren + ").");
        }

        private String generateUnitOutputName(UnitOutput o) {
            return o.toNuSMV().replace(".", "__");
        }

        private boolean hasFailure(UnitGroup g, int division) {
            final int chosen = chosenFailures.get(g);
            return chosen == division || chosen == Command.ALL_INDEX;
        }

        private void entityToProlog(NamedEntity<?> e) {
            if (e instanceof Constant) {
                if (!processedConstCalls.add(e.toNuSMV())) {
                    return;
                }
                addConfFact(e.toNuSMV(), null, Collections.emptyList());
                return;
            } else if (!(e instanceof Indexing)) {
                throw new AssertionError("Unexpected class: " + e.getClass().getSimpleName());
            }

            final Indexing<?> indexing = (Indexing<?>) e;
            final NamedEntity<?> entity = indexing.parent();
            final int division = indexing.index;
            if (!processedCalls.add(Pair.of(entity, division))) {
                return;
            }

            if (entity instanceof Unit) {
                final Unit unit = (Unit) entity;
                final UnitGroup g = unit.parent();
                // [division of this subnetwork][connection index]
                final Map<Integer, List<NamedEntity<?>>> allConnections = unit.getInputConnections();
                final List<NamedEntity<?>> connections = allConnections.get(division);
                final List<List<String>> prologChildren = new ArrayList<>();

                // 1. prepare a list of connections
                for (NamedEntity<?> child : connections) {
                    if (child instanceof Indexing) {
                        final Indexing<?> childAsIndexing = (Indexing<?>) child;
                        final int childDivision = childAsIndexing.index;
                        final NamedEntity<?> childEntity = childAsIndexing.parent();
                        final Indexing<?> childIndexing = (Indexing<?>) child;
                        final String moduleName = child.parent() instanceof UnitOutput
                                ? (generateUnitOutputName((UnitOutput) child.parent()) + "_DIV" + childDivision)
                                : childIndexing.toNuSMV();
                        final UnitGroup childUnitGroup = getUnitGroup(childEntity);
                        entityToProlog(childIndexing);
                        final boolean hasFailures = hasFailure(g, division)
                                || hasFailure(childUnitGroup, childDivision);
                        prologChildren.add(Arrays.asList(prefix + moduleName,
                                String.valueOf(hasFailures ? 1 : 0)));
                    } else if (child instanceof Constant) {
                        entityToProlog(child);
                        final boolean hasFailures = hasFailure(g, division);
                        prologChildren.add(Arrays.asList(prefix + child.toNuSMV(),
                                String.valueOf(hasFailures ? 1 : 0)));
                    } else {
                        throw new AssertionError("Unexpected class: "
                                + child.getClass().getSimpleName());
                    }
                }

                // 2. load symmetries and check their acceptability
                final List<List<Integer>> thisModuleSymmetries = symmetryMap.get(unit);
                final List<Integer> allSymmetryIndices = new ArrayList<>();
                thisModuleSymmetries.forEach(allSymmetryIndices::addAll);
                if (new HashSet<>(allSymmetryIndices).size() < allSymmetryIndices.size()) {
                    throw new RuntimeException("Symmetry lists " + thisModuleSymmetries + " for unit " + unit +
                            " are non-disjoint. Prolog code generation for such lists is not supported. Anyway," +
                            " such lists are suspicious. Are they correct?");
                }

                // 3. group connections according to symmetries
                final List<List<List<String>>> groupedChildren = new ArrayList<>();
                // index of symmetry (0 means "standalone") -> original index
                final Map<Integer, Integer> connectionIndexToSymmetryIndex = new HashMap<>();
                final boolean[] connectionPresentInSymmetry = new boolean[prologChildren.size()];
                for (int i = 0; i < thisModuleSymmetries.size(); i++) {
                    final List<Integer> symmetryGroup = thisModuleSymmetries.get(i);
                    for (int j : symmetryGroup) {
                        connectionIndexToSymmetryIndex.put(j - 1, i);
                        connectionPresentInSymmetry[j - 1] = true;
                    }
                }
                int currentDummySymmetryIndex = thisModuleSymmetries.size();
                for (int j = 0; j < prologChildren.size(); j++) {
                    if (!connectionPresentInSymmetry[j]) {
                        connectionIndexToSymmetryIndex.put(j, currentDummySymmetryIndex++);
                    }
                }
                // generatedStrings.add("% " + thisModuleSymmetries + " -> " + connectionIndexToSymmetryIndex);
                for (int i = 0; i < currentDummySymmetryIndex; i++) {
                    groupedChildren.add(new ArrayList<>());
                }
                for (int j = 0; j < prologChildren.size(); j++) {
                    groupedChildren.get(connectionIndexToSymmetryIndex.get(j)).add(prologChildren.get(j));
                }

                // 3. remove duplicates
                final List<List<List<String>>> deduplicatedGroupedChildren
                        = new ArrayList<>(new LinkedHashSet<>(groupedChildren));

                // 4. sort according to list length (to make shorter lists processed earlier)
                // (not sure whether this helps)
                deduplicatedGroupedChildren.sort(Comparator.comparingInt(List::size));

                addConfFact(unit.toNuSMV(), division, deduplicatedGroupedChildren);
            } else if (entity instanceof UnitOutput) {
                final UnitOutput output = (UnitOutput) entity;
                final Unit unit = output.parent();
                final Indexing<Unit> unitIndexing = new Indexing<>(unit, division);
                entityToProlog(unitIndexing);
                final List<String> args = Arrays.asList(prefix + unitIndexing.toNuSMV(),
                        String.valueOf(hasFailure(unit.parent(), division) ? 1 : 0));
                addConfFact(generateUnitOutputName(output), division,
                        Collections.singletonList(Collections.singletonList(args)));
            } else if (entity instanceof Input) {
                addConfFact(entity.toNuSMV(), division, Collections.emptyList());
            } else {
                throw new AssertionError(entity.getClass().getSimpleName());
            }
        }

        private List<List<Integer>> getFailureCombinations(boolean includeNAFailures, boolean includeALLFailures) {
            final List<List<Integer>> failureCombinations = new ArrayList<>();
            for (UnitGroup g : unitGroups.values()) {
                final List<Integer> numbers = IntStream.rangeClosed(1, g.divisions).boxed()
                        .collect(Collectors.toCollection(ArrayList::new));
                if (includeNAFailures) {
                    numbers.add(Command.NA_INDEX);
                }
                if (includeALLFailures && g.divisions > 1) {
                    numbers.add(Command.ALL_INDEX);
                }
                failureCombinations.add(numbers);
            }
            return cartesianProduct(failureCombinations);
        }

        public String toProlog(boolean includeNAFailures, boolean includeALLFailures) throws IOException {
            final List<List<Integer>> failureCombinations = getFailureCombinations(includeNAFailures,
                    includeALLFailures);
            final List<UnitGroup> orderedUnitGroups = new ArrayList<>(unitGroups.values());
            for (List<Integer> chosenFailures : failureCombinations) {
                processedCalls.clear();
                processedConstCalls.clear();
                final LinkedHashMap<UnitGroup, Integer> groupToChosenFailure = new LinkedHashMap<>();
                for (int i = 0; i < chosenFailures.size(); i++) {
                    groupToChosenFailure.put(orderedUnitGroups.get(i), chosenFailures.get(i));
                }
                generatedStrings.add("% configurations with failures: " + groupToChosenFailure.toString()
                        .replace(String.valueOf(Command.NA_INDEX), "NA")
                        .replace(String.valueOf(Command.ALL_INDEX), "ALL"));
                this.chosenFailures = groupToChosenFailure;
                this.prefix = getPrefix(new ArrayList<>(groupToChosenFailure.values()));
                for (Unit u : units.values()) {
                    for (int div = 1; div <= u.divisions; div++) {
                        entityToProlog(new Indexing<>(u, div));
                    }
                }
                generatedStrings.add("");
            }

            // load from jar
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(getClass()
                    .getResourceAsStream("/reasoning.pl")))) {
                generatedStrings.addAll(reader.lines().collect(Collectors.toList()));
            }

            return generatedStrings.stream().collect(Collectors.joining(System.lineSeparator()));
        }

        private String getPrefix(List<Integer> failureCombination) {
            final List<String> stringAnnotations = new ArrayList<>();
            int i = 0;
            for (UnitGroup g : unitGroups.values()) {
                final int failingDiv =  failureCombination.get(i++);
                stringAnnotations.add(g.toNuSMV() + "_" + configSafeIndex(failingDiv));
            }
            return stringAnnotations.toString()
                    .replace(", ", "_")
                    .replace("[", "f__")
                    .replace("]", "__");
        }

        private String getOverallConfigurationName(List<Integer> failureCombination, Unit unit, int division) {
            return getPrefix(failureCombination) + unit.toNuSMV() + "_DIV" + division;
        }
    }

    public String toProlog(boolean includeNAFailures, boolean includeALLFailures) throws IOException {
        return this.new PrologGenerator().toProlog(includeNAFailures, includeALLFailures);
    }

    public Unit getUnitByName(String name) {
        final List<Unit> matchingUnits = units.values().stream().filter(u -> u.toNuSMV().equals(name))
                .collect(Collectors.toList());
        if (matchingUnits.size() == 0) {
            throw new RuntimeException("No unit named " + name + " found.");
        } else if (matchingUnits.size() > 1) {
            throw new RuntimeException("More than one unit named " + name + " found.");
        }
        return matchingUnits.get(0);
    }

    private static class DominationGraph {
        private final int n;
        private final List<String> nodeNames;
        private final List<VerificationConfiguration> nodeVerificationConfigurations;
        private final Map<String, Integer> nodeNameToIndex;
        private final boolean inferInformation;

        // incidence matrix
        private final Boolean[][] m;

        // whether this configuration should be verified
        private final boolean[] essential;

        DominationGraph(List<String> nodeNames, List<VerificationConfiguration> nodeVerificationConfigurations,
                        boolean inferInformation) {
            this.nodeNames = nodeNames;
            this.nodeVerificationConfigurations = nodeVerificationConfigurations;
            this.n = nodeNames.size();
            this.inferInformation = inferInformation;
            nodeNameToIndex = new HashMap<>();
            for (int i = 0; i < n; i++) {
                nodeNameToIndex.put(nodeNames.get(i), i);
            }
            m = new Boolean[n][n];
            if (inferInformation) {
                // each node always dominates itself
                for (int i = 0; i < n; i++) {
                    m[i][i] = true;
                }
            }
            essential = new boolean[n];
        }

        List<VerificationConfiguration> essentialVerificationConfigurations() {
            final List<VerificationConfiguration> result = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                if (essential[i]) {
                    result.add(nodeVerificationConfigurations.get(i));
                }
            }
            return result;
        }

        Boolean incident(String node1, String node2) {
            final int i1 = nodeNameToIndex.get(node1);
            final int i2 = nodeNameToIndex.get(node2);
            return m[i1][i2];
        }

        void setIncident(String node1, String node2, boolean value) {
            final int i1 = nodeNameToIndex.get(node1);
            final int i2 = nodeNameToIndex.get(node2);
            if (m[i1][i2] != null && m[i1][i2] != value) {
                throw new RuntimeException("Contradictory results for dominates(" + node1
                        + ", " + node2 + ")!");
            }
            m[i1][i2] = value;
            if (inferInformation) {
                boolean changed = true;
                while (changed) {
                    changed = false;
                    for (int i = 0; i < n; i++) {
                        for (int j = 0; j < n; j++) {
                            for (int k = 0; k < n; k++) {
                                // transitivity inference: m[i, j] & m[j, k] -> m[i, k]
                                if (m[i][j] != null && m[j][k] != null) {
                                    if (m[i][j] && m[j][k]) {
                                        if (m[i][k] == null) {
                                            m[i][k] = true;
                                            changed = true;
                                        } else if (!m[i][k]) {
                                            throw new AssertionError();
                                        }
                                    }
                                }
                                // reflexivity inference: m[i, j] & m[j, i] -> m[j, k] = m[i, k]
                                if (m[i][j] != null && m[j][i] != null) {
                                    if (m[i][j] && m[j][i]) {
                                        if (m[j][k] == null && m[i][k] != null) {
                                            m[j][k] = m[i][k];
                                            changed = true;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder();
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    sb.append(m[i][j] ? 1 : 0);
                }
                sb.append(" # node ").append(nodeNames.get(i));
                if (essential[i]) {
                    sb.append(" # essential configuration");
                }
                sb.append(System.lineSeparator());
            }
            return sb.toString();
        }

        void checkReflexivity() {
            for (int i = 0; i < n; i++) {
                if (!m[i][i]) {
                    throw new AssertionError("Reflexivity check failed!");
                }
            }
        }

        void checkTransitivity() {
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    for (int k = 0; k < n; k++) {
                        if (m[i][j] && m[j][k] && !m[i][k]) {
                            throw new AssertionError("Transitivity check failed!");
                        }
                    }
                }
            }
        }

        void findEssentialConfigurations() {
            l_main: for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    // if strictly dominated by some configuration,
                    // no need to verify
                    if (m[j][i] && !m[i][j]) {
                        continue l_main;
                    }
                    // if equivalent to some configuration previously identified as essential,
                    // no need to verify
                    if (m[j][i] && m[i][j] && essential[j]) {
                        continue l_main;
                    }
                }
                // otherwise, need to verify this configuration
                essential[i] = true;
            }
        }
    }

    // returns essential configurations
    public void queryProlog(String prologInputFile, String prologExecutable, String unitName,
                                           boolean thoroughQueries, boolean includeNAFailures,
                                           boolean includeALLFailures) throws IOException {
        final PrologGenerator pg = this.new PrologGenerator();
        final List<List<Integer>> failureCombinations = pg.getFailureCombinations(includeNAFailures,
                includeALLFailures);
        queryProlog(prologInputFile, prologExecutable, unitName, thoroughQueries, failureCombinations);
    }

    // returns essential configurations
    public List<VerificationConfiguration> queryProlog(String prologInputFile, String prologExecutable, String unitName,
                            boolean thoroughQueries, List<List<Integer>> failureCombinations) throws IOException {
        final PrologGenerator pg = this.new PrologGenerator();
        final Unit unit = getUnitByName(unitName);
        final UnitGroup examinedUnitGroup = pg.getUnitGroup(unit);
        int indexOfExaminedUnitGroup = 0;
        boolean indexFound = false;
        for (UnitGroup g : unitGroups.values()) {
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

        // System.out.println(queries);

        System.out.println("Running prolog interpreter: " + prologExecutable);
        final Process process = new ProcessBuilder(Arrays.asList(prologExecutable, prologInputFile))
                .redirectErrorStream(true).start();
        try (PrintWriter pw = new PrintWriter(process.getOutputStream(), true);
             Scanner sc = new Scanner(process.getInputStream())) {
            String line;
            do {
                line = sc.nextLine();
            } while (!line.startsWith("For help, use") && !line.startsWith("For built-in help, use"));
            sc.nextLine();
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
                    String token = sc.next();
                    if (!token.endsWith(".")) {
                        pw.println(".");
                    } else {
                        token = token.substring(0, token.length() - 1);
                    }
                    final boolean outcome = Boolean.parseBoolean(token);
                    dg.setIncident(nodeName1, nodeName2, outcome);
                    System.out.println(outcome);
                }
            }
        }
        process.destroy();
        dg.checkReflexivity();
        dg.checkTransitivity();
        dg.findEssentialConfigurations();
        System.out.println(dg.toString());
        return dg.essentialVerificationConfigurations();
    }
}
