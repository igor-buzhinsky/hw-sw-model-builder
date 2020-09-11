package hwswbuilder.structures;

public class UnitOutput extends IndexableEntity<Unit> {
    final String nusmvType;
    final Unit parentAsUnit;

    UnitOutput(String name, String nusmvType, Unit parent) {
        super(name, parent, parent.divisions);
        this.nusmvType = nusmvType;
        this.parentAsUnit = parent;
    }

    @Override
    public Unit parent() {
        return parentAsUnit;
    }

    @Override
    public String toNuSMV() {
        return parent.toNuSMV() + "." + name;
    }

    @Override
    String appendIndex(int index) {
        return parent.appendIndex(index) + "." + name;
    }
}
