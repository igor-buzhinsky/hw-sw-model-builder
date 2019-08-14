package hwswbuilder.structures;

import hwswbuilder.NameSubstitutionRegistry;
import hwswbuilder.command.Workspace;

import java.util.*;

public class Output extends IndexableEntity<NamedEntity> implements CodeProducer {
    private final Map<Integer, Indexing> inputConnections = new LinkedHashMap<>();

    public Output(String name, int divisions) {
        super(name, null, divisions);
    }

    public void addInputConnection(Indexing from) {
        inputConnections.put(inputConnections.size() + 1, from);
    }

    /**
     * Effective division: if not equal to the given division, then the instance of the
     * subnetwork for the given division is optimized out.
     * @param input: named entity whose subnetwork will be examined.
     * @param div: given division index.
     * @return effective division.
     */
    private static int effectiveDivision(NamedEntity input, int div) {
        for (NamedEntity current = input; current != null; current = current.parent()) {
            if (current instanceof Unit) {
                return ((Unit) current).effectiveDivision(div);
            }
        }
        return div;
    }

    @Override
    public void nuSMVDeclaration(Workspace workspace) {
        if (inputConnections.size() != divisions) {
            throw new RuntimeException(String.format(
                    "Dimension mismatch: output %s requires %d values but %d are connected",
                    name, divisions, inputConnections.size()));
        }
        for (int div : allDivisions) {
            final int effectiveDiv = effectiveDivision(inputConnections.get(div), div);
            final Indexing input = inputConnections.get(effectiveDiv);
            final boolean optimizing = div != effectiveDiv;
            final String optimizationText = optimizing
                    ? "; -- (replaced with the version from DIV1 due to unit optimization)" : "";
            final String deferredName = NameSubstitutionRegistry.deferName(input.toNuSMV());
            // fault processing
            String effectiveName = deferredName;
            if (input.parent instanceof UnitOutput) {
                final UnitOutput o = (UnitOutput) input.parent;
                if (o.parent.parent.shouldInjectFailure(div)) {
                    final InputInfo info = new InputInfo(name, o.nusmvType, div, true);
                    info.failureDecl(deferredName).forEach(s ->
                            workspace.addVar((optimizing ? "-- (optimized out) " : "") + s));
                    if (!optimizing) {
                        effectiveName = info.failureName(true);
                        workspace.addFailureChunk(info.failureName(false) + ".FAILURE");
                    }
                }
            }

            for (String postfix : Arrays.asList("", "_FAULT")) {
                workspace.addDefine(appendIndex(div) + postfix + " := " + effectiveName
                        + postfix + optimizationText);
            }
        }
    }

    @Override
    public String toNuSMV() {
        return name;
    }

    @Override
    public String toString() {
        return "Output{" + "name='" + name + '\'' + "} " + super.toString();
    }
}
