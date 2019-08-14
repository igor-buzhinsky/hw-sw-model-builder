package hwswbuilder.structures;

public class UnitGroup extends IndexableEntity<NamedEntity> {
    private final int divisionOfFailure;

    public UnitGroup(String name, int divisions, int divisionOfFailure) {
        super(name, null, divisions);
        this.divisionOfFailure = divisionOfFailure;
    }

    boolean shouldInjectFailure(int division) {
        return division == divisionOfFailure;
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
