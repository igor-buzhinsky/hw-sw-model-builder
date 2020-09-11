package hwswbuilder.structures;

public abstract class NamedEntity<P extends NamedEntity<?>> {
    final String name;
    final P parent;
    public final static String SEP = System.lineSeparator();
    public final static String PAD = "    ";

    NamedEntity(String name, P parent) {
        this.name = name;
        this.parent = parent;
    }

    public abstract String toNuSMV();

    public NamedEntity<?> parent() {
        return parent;
    }

    @Override
    public String toString() {
        return toNuSMV();
    }
}
