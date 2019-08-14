package hwswbuilder.structures;

import hwswbuilder.NameSubstitutionRegistry;
import hwswbuilder.command.Workspace;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class Unit extends IndexableEntity<UnitGroup> implements CodeProducer {
    private final String moduleName;
    private final String nusmvFilename;
    private final int singleDivisionToRetain;
    private final int maxDelay;
    private final Map<String, UnitOutput> outputs = new LinkedHashMap<>();

    private boolean shouldOptimizeOut(int div) {
        return singleDivisionToRetain > -1 && div != singleDivisionToRetain;
    }

    int effectiveDivision(int div) {
        return shouldOptimizeOut(div) ? singleDivisionToRetain : div;
    }

    // inputConnections[division of this subnetwork][connection index]
    private final Map<Integer, List<NamedEntity>> inputConnections = new LinkedHashMap<>();

    public final Map<String, UnitOutput> outputs() {
        return Collections.unmodifiableMap(outputs);
    }

    public Unit(String name, UnitGroup parentUnitGroup, String moduleName, String nusmvFilename,
                int singleDivisionToRetain, int maxDelay, Map<String, String> outputsWithTypes) {
        super(name, parentUnitGroup, parentUnitGroup.divisions);
        this.moduleName = moduleName;
        this.nusmvFilename = nusmvFilename;
        allDivisions.forEach(i -> inputConnections.put(inputConnections.size() + 1, new ArrayList<>()));
        this.singleDivisionToRetain = singleDivisionToRetain;
        this.maxDelay = maxDelay;
        outputsWithTypes.forEach((oName, oType) -> outputs.put(oName, new UnitOutput(oName, oType, this)));
    }

    public void addInputConnection(NamedEntity from, int targetDivision) {
        if (!allDivisions.contains(targetDivision)) {
            throw new RuntimeException("Division out of range: " + targetDivision
                    + " in subnetwork " + toNuSMV());
        }
        inputConnections.get(targetDivision).add(from);
    }

    @Override
    public String toNuSMV() {
        return moduleName;
    }

    private static void addArgument(Collection<String> destination, String argName, boolean addPadding) {
        Stream.of((addPadding ? (SEP + PAD + PAD) : "") + argName,
                argName + "_FAULT", "TRUE" /*_CONNECTED */).forEach(destination::add);
    }

    private static String wrapperInputFromUnitArgName(NamedEntity inputParent) {
        return inputParent.parent.toNuSMV() + "_" + inputParent.name;
    }

    private List<InputInfo> createInputs(NamedEntity inputConnection, int currentDivision) {
        if (inputConnection instanceof Constant) {
            return Collections.singletonList(new InputInfo(inputConnection.name));
        }
        final boolean alwaysInjectInputFailures = parent.shouldInjectFailure(currentDivision);
        final NamedEntity inputParent = inputConnection.parent;
        final Indexing inputConnectionInd = (Indexing) inputConnection;
        if (inputParent instanceof Input) {
            // for plain inputs, fault injection is governed by the current division
            final int division = inputConnectionInd.index;
            final String nusmvType = ((Input) inputParent).nusmvType;
            return Collections.singletonList(new InputInfo(inputParent.name, nusmvType, division,
                    alwaysInjectInputFailures));
        } else if (inputParent instanceof UnitOutput) {
            final List<InputInfo> result = new ArrayList<>();
            final UnitOutput o = (UnitOutput) inputParent;
            for (Map.Entry<String, UnitOutput> e : o.parent.outputs.entrySet()) {
                if (e.getKey().equals(o.name)) {
                    final int division = inputConnectionInd.index;
                    final String nusmvType = e.getValue().nusmvType;
                    // for outputs from subnetworks, fault injection may also be possible
                    // if the input subnetwork can have faults
                    final boolean injectFailures = alwaysInjectInputFailures
                            || o.parent.parent.shouldInjectFailure(currentDivision);
                    // need to specify the name of the parent subnetwork, otherwise name clashes
                    // become possible if multiple subnetworks have outputs with identical names
                    result.add(new InputInfo(wrapperInputFromUnitArgName(inputParent), nusmvType,
                            division, injectFailures));
                }
            }
            return result;
        } else {
            throw new AssertionError("Parent of Indexing of an unexpected type");
        }
    }

    /**
     * Generates a wrapper for a subnetwork.
     * @return module code.
     */
    private String getWrapper(int currentDivision, Workspace workspace) {
        final StringBuilder sb = new StringBuilder();
        final List<String> failureChunks = new ArrayList<>();
        final List<InputInfo> inputs = new ArrayList<>();
        inputConnections.get(currentDivision)
                .forEach(input -> inputs.addAll(createInputs(input, currentDivision)));
        sb.append("(");
        sb.append(inputs.stream().filter(i -> !i.isConstant)
                .map(i -> SEP + PAD + i.argName + ", " + i.argName + "_FAULT, "
                + i.argName + "_CONNECTED").collect(Collectors.joining(", ")));
        sb.append(")").append(SEP);
        sb.append("VAR").append(SEP);

        final Map<String, String> nameReplacements = new HashMap<>();

        // DELAY HANDLING
        final Map<Pair<NamedEntity, Integer>, List<InputInfo>> delayGroups
                = new LinkedHashMap<>();
        // all plain inputs are delayed independently
        for (NamedEntity input : inputConnections.get(currentDivision)) {
            // constants are not delayed
            if (input instanceof Indexing) {
                final Indexing ind = (Indexing) input;
                final NamedEntity parent = ind.parent;
                if (parent instanceof Input) {
                    final var info = new InputInfo(parent.name, ((Input) parent).nusmvType,
                            ind.index, false);
                    delayGroups.put(Pair.of(parent, ind.index), Collections.singletonList(info));
                } else if (parent instanceof UnitOutput) {
                    final var info = new InputInfo(wrapperInputFromUnitArgName(parent),
                            ((UnitOutput) parent).nusmvType, ind.index, false);
                    final var p = Pair.of(parent.parent, ind.index);
                    delayGroups.putIfAbsent(p, new ArrayList<>());
                    delayGroups.get(p).add(info);
                }
            }
        }
        // need one sequence of delay blocks per group
        for (var e : delayGroups.entrySet()) {
            final NamedEntity source = e.getKey().getLeft();
            final int sourceDiv = e.getKey().getRight();
            if (maxDelay > 0) {
                sb.append(PAD).append("-- delay modules for ").append(source.toNuSMV())
                        .append(" from division ").append(sourceDiv).append(SEP);
            }
            final List<InputInfo> infos = e.getValue();
            final String delayModuleName = maxDelay > 0
                    ? workspace.handler.delayModule(infos.stream()
                    .map(o -> o.nusmvType).collect(Collectors.toList())) : null;
            String delayedName = null;
            for (int i = 1; i <= maxDelay; i++) {
                delayedName = source.toNuSMV() + "_DIV" + sourceDiv + "_DELAYED" + i;
                final List<String> args = new ArrayList<>();
                int j = 0;
                for (InputInfo info : infos) {
                    final String prevName = i == 1
                            ? info.argName
                            : (source.toNuSMV() + "_DIV" + sourceDiv + "_DELAYED" + (i - 1) + ".OUT" + j);
                    addArgument(args, prevName, false);
                    j++;
                }
                sb.append(PAD).append(delayedName).append(": ").append(delayModuleName).append("(")
                        .append(String.join(", ", args)).append(");").append(SEP);
            }
            // name replacement
            if (delayedName != null) {
                int j = 0;
                // create a delayed name for each output
                for (InputInfo info : infos) {
                    nameReplacements.put(info.argName, delayedName + ".OUT" + j);
                    // sb.append("-- replace "+ info.argName + " -> " + delayedName + ".OUT" + j + SEP);
                    j++;
                }
            }
        }

        // fault processing
        inputs.stream().filter(i -> i.injectFailure).forEach(i -> {
            final String effectiveName = nameReplacements.getOrDefault(i.argName, i.argName);
            nameReplacements.put(i.argName, i.failureName(true));
            sb.append(PAD).append("-- fault injection modules for ").append(i.argName).append(SEP);
            i.failureDecl(effectiveName).forEach(s -> sb.append(PAD).append(s).append(";").append(SEP));
            failureChunks.add(i.failureName(false) + ".FAILURE");
        });

        sb.append(PAD).append("-- unit").append(SEP);
        sb.append(PAD + "content: ").append(moduleName).append("(");
        sb.append(inputs.stream().map(i -> {
            if (i.isConstant) {
                // specify the value of this constant (not present in wrapper arguments)
                return SEP + PAD + PAD + i.argName + ", FALSE, FALSE";
            } else {
                final String effectiveName = nameReplacements.getOrDefault(i.argName, i.argName);
                return SEP + PAD + PAD + effectiveName + ", " + effectiveName + "_FAULT, TRUE";
            }
        }).collect(Collectors.joining(", ")));
        sb.append(");").append(SEP);
        sb.append("DEFINE").append(SEP);
        sb.append(Workspace.failureFlagDeclaration(failureChunks)).append(SEP);
        for (String postfix : Arrays.asList("", "_FAULT")) {
            sb.append(outputs.keySet().stream()
                    .map(out -> PAD + out + postfix + " := content."+ out + postfix + ";")
                    .collect(Collectors.joining(SEP))).append(SEP);
        }
        return sb.toString();
    }

    @Override
    public void nuSMVDeclaration(Workspace workspace) {
        for (int div : allDivisions) {
            final boolean optimizing = shouldOptimizeOut(div);
            // fault handling
            final String effectiveModuleName = workspace.handler
                    .unitWrapper(getWrapper(div, workspace), moduleName, div);
            // input handling
            final List<String> arguments = new ArrayList<>();
            for (NamedEntity e : inputConnections.get(div)) {
                // constants are only substituted inside the wrapper
                if (!(e instanceof Constant)) {
                    final String deferredName = NameSubstitutionRegistry.deferName(e.toNuSMV());
                    addArgument(arguments, deferredName, !optimizing);
                }
            }
            final String fullName = appendIndex(div);
            final String comment = optimizing ? "-- (optimized out) " : "";
            workspace.addVar(comment + fullName + ": " + effectiveModuleName
                    + "(" + String.join(", ", arguments) + ")");
            if (!optimizing) {
                workspace.addFailureChunk(fullName + ".FAILURE");
            }
        }
    }

    public String nusmvFilename() {
        return nusmvFilename;
    }

    public int symmetryInputVariableNumber() {
        return inputConnections.get(1).size();
    }

    public String symmetryInputDeclarations() {
        final StringBuilder sb = new StringBuilder();
        int varIndex = 1;
        // if the configuration is correct, all divisions are the same in terms of
        // input variable types
        for (NamedEntity e : inputConnections.get(1)) {
            // resolve the type of corresponding input variable
            final RuntimeException unexpected = new RuntimeException(String.format(
                    "Input connection %s to unit %s of unexpected type", e, this));
            final String nusmvType;
            if (e instanceof Constant) {
                nusmvType = ((Constant) e).nusmvType();
            } else if (e instanceof Indexing) {
                if (e.parent instanceof Input) {
                    nusmvType = ((Input) e.parent).nusmvType;
                } else if (e.parent instanceof UnitOutput) {
                    nusmvType = ((UnitOutput) e.parent).nusmvType;
                } else {
                    throw unexpected;
                }
            } else {
                throw unexpected;
            }
            final String varName = "v" + varIndex++;
            for (String mid : Arrays.asList(": " + nusmvType, "_FAULT: boolean", "_CONNECTED: boolean")) {
                sb.append(PAD).append(varName).append(mid).append(";").append(SEP);
            }
        }
        return sb.toString();
    }

    public String symmetryDeclaration(String instanceName, int[] inputVariableIndices) {
        return PAD + instanceName + ": " + moduleName + "(" +
                IntStream.of(inputVariableIndices).mapToObj(i ->
                String.format("v%d, v%d_FAULT, v%d_CONNECTED", i, i, i))
                        .collect(Collectors.joining(", ")) + ");";
    }

    public String symmetryCheckingSpec(String instanceName1, String instanceName2) {
        return "CTLSPEC AG(" + outputs.values().stream().map(o ->
                String.format("%s.%s = %s.%s & %s.%s_FAULT = %s.%s_FAULT", instanceName1, o.name,
                instanceName2, o.name, instanceName1, o.name, instanceName2, o.name))
                .collect(Collectors.joining(" & ")) + ")";
    }
}
