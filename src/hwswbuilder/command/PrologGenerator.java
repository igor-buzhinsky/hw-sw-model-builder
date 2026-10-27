package hwswbuilder.command;

import hwswbuilder.structures.*;
import org.apache.commons.lang3.tuple.Pair;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Generates a Prolog representation of the configurations of a workspace (one configuration per combination of
 * failing divisions), which is used to determine which configurations dominate others.
 */
class PrologGenerator {
    private final Workspace workspace;
    private final Map<Unit, List<List<Integer>>> symmetryMap = new HashMap<>();
    private final List<String> generatedStrings = new ArrayList<>();
    private final Set<Pair<NamedEntity<?>, Integer>> processedCalls = new HashSet<>();
    private final Set<String> processedConstCalls = new HashSet<>();

    private LinkedHashMap<UnitGroup, Integer> chosenFailures = null;
    private String prefix = null;

    PrologGenerator(Workspace workspace) {
        this.workspace = workspace;
        for (Unit u : workspace.units.values()) {
            symmetryMap.put(u, new ArrayList<>());
        }
        for (Pair<Unit, List<Integer>> pair : workspace.symmetries) {
            symmetryMap.get(pair.getLeft()).add(pair.getRight());
        }
    }

    UnitGroup getUnitGroup(NamedEntity<?> input) {
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

    List<List<Integer>> getFailureCombinations(boolean includeNAFailures, boolean includeALLFailures) {
        final List<List<Integer>> failureCombinations = new ArrayList<>();
        for (UnitGroup g : workspace.unitGroups.values()) {
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
        return Workspace.cartesianProduct(failureCombinations);
    }

    String toProlog(boolean includeNAFailures, boolean includeALLFailures) throws IOException {
        final List<List<Integer>> failureCombinations = getFailureCombinations(includeNAFailures,
                includeALLFailures);
        final List<UnitGroup> orderedUnitGroups = new ArrayList<>(workspace.unitGroups.values());
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
            for (Unit u : workspace.units.values()) {
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
        for (UnitGroup g : workspace.unitGroups.values()) {
            final int failingDiv =  failureCombination.get(i++);
            stringAnnotations.add(g.toNuSMV() + "_" + Workspace.configSafeIndex(failingDiv));
        }
        return stringAnnotations.toString()
                .replace(", ", "_")
                .replace("[", "f__")
                .replace("]", "__");
    }

    String getOverallConfigurationName(List<Integer> failureCombination, Unit unit, int division) {
        return getPrefix(failureCombination) + unit.toNuSMV() + "_DIV" + division;
    }
}
