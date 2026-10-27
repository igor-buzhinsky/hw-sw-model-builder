package hwswbuilder.command;

import hwswbuilder.CustomModuleHandler;
import hwswbuilder.NameSubstitutionRegistry;
import hwswbuilder.structures.*;
import org.apache.commons.lang3.tuple.Pair;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static hwswbuilder.structures.NamedEntity.PAD;
import static hwswbuilder.structures.NamedEntity.SEP;

/**
 * The system described by a configuration file. Generates the NuSMV model of the system; symmetry checking and
 * Prolog-based analysis are delegated to {@link SymmetryChecker}, {@link PrologGenerator} and
 * {@link DominationAnalyzer}.
 */
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

    // disable unit instances that are unreachable from the viewpoint
    public void coiOptimize(Unit viewpointUnit, int viewpointDivisions) {
        final List<Pair<Unit, Integer>> queue = new LinkedList<>();
        final Set<Pair<Unit, Integer>> processedInstances = new HashSet<>();
        queue.add(Pair.of(viewpointUnit, viewpointDivisions));
        while (!queue.isEmpty()) {
            final Pair<Unit, Integer> element = queue.remove(0);
            if (processedInstances.contains(element)) {
                continue;
            }
            final Unit u = element.getLeft();
            final int division = element.getRight();
            for (NamedEntity<?> e : u.getInputConnections().get(division)) {
                if (e instanceof Indexing) {
                    NamedEntity<?> p = e.parent();
                    if (p instanceof UnitOutput) {
                        final Unit referencedUnit = ((UnitOutput) p).parent();
                        final int referencedDivision = ((Indexing<?>) e).index;
                        queue.add(Pair.of(referencedUnit, referencedDivision));
                    }
                }
            }
            processedInstances.add(element);
        }
        final Map<Unit, Set<Integer>> divisionsToKeep = new HashMap<>();
        for (Unit u : units.values()) {
            divisionsToKeep.put(u, new HashSet<>());
        }
        for (Pair<Unit, Integer> p : processedInstances) {
            divisionsToKeep.get(p.getLeft()).add(p.getRight());
        }
        for (Map.Entry<Unit, Set<Integer>> e : divisionsToKeep.entrySet()) {
            final Unit u = e.getKey();
            for (int i = 1; i <= u.divisions; i++) {
                if (!e.getValue().contains(i)) {
                    u.disableDivision(i);
                    System.out.println("Optimization: disabled division " + i + " of unit " + u);
                }
            }
        }
    }

    // if true, then failures can deterministically disappear (useful in deadlock checking)
    public boolean vanishingFailures = false;

    // symmetry information of units with respect to their input variables
    final List<Pair<Unit, List<Integer>>> symmetries = new ArrayList<>();

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
        final List<String> lines = new ArrayList<>();
        lines.add("VAR");
        lines.addAll(varDeclarations);
        lines.add("DEFINE");
        lines.addAll(defineDeclarations);
        lines.add(failureFlagDeclaration(failureChunks));
        return String.join(SEP, lines);
    }

    String basicBlocksCode() throws IOException {
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
        return handler.nuSMVDeclaration() + registry.replaceWithFinal(sb.toString());
    }

    public String outputFilename() {
        return outputFilename;
    }

    void addSymmetry(Unit unit, List<Integer> inputVariableIndices) {
        symmetries.add(Pair.of(unit, inputVariableIndices));
    }

    public static String configSafeIndex(int index) {
        return index == Command.NA_INDEX ? "NA" : index == Command.ALL_INDEX ? "ALL" : String.valueOf(index);
    }

    public void checkSymmetries(String nusmvCommand) throws IOException {
        new SymmetryChecker(this).checkSymmetries(nusmvCommand);
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

    public String toProlog(boolean includeNAFailures, boolean includeALLFailures) throws IOException {
        return new PrologGenerator(this).toProlog(includeNAFailures, includeALLFailures);
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

    // prints the domination graph for all failure combinations
    public void queryProlog(String prologInputFile, String prologExecutable, String unitName,
                            boolean thoroughQueries, boolean includeNAFailures,
                            boolean includeALLFailures) throws IOException {
        final DominationAnalyzer analyzer = new DominationAnalyzer(this);
        analyzer.queryProlog(prologInputFile, prologExecutable, unitName, thoroughQueries,
                analyzer.getFailureCombinations(includeNAFailures, includeALLFailures));
    }

    // returns essential configurations
    public List<VerificationConfiguration> queryProlog(String prologInputFile, String prologExecutable,
                                                       String unitName, boolean thoroughQueries,
                                                       List<List<Integer>> failureCombinations) throws IOException {
        return new DominationAnalyzer(this).queryProlog(prologInputFile, prologExecutable, unitName,
                thoroughQueries, failureCombinations);
    }
}
