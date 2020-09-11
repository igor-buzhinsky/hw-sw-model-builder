package hwswbuilder.structures;

import hwswbuilder.command.Command;

public class UnitGroup extends IndexableEntity<NamedEntity<?>> {
    private final int divisionOfFailure;

    public UnitGroup(String name, int divisions, int divisionOfFailure) {
        super(name, null, divisions);
        this.divisionOfFailure = divisionOfFailure;
    }

    boolean shouldInjectFailure(int division) {
        return divisionOfFailure == Command.ALL_INDEX || divisionOfFailure == division;
    }

    @Override
    public String toNuSMV() {
        return name;
    }

    @Override
    String appendIndex(int index) {
        throw new RuntimeException("Plain unit group indexing has no NuSMV representation");
    }
}
