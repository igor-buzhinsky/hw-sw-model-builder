package hwswbuilder.structures;

public class Indexing<P extends IndexableEntity> extends NamedEntity<P> {
    public final int index;

    public Indexing(P parent, int index) {
        super(parent.name, parent);
        if (index <= 0 || index > parent.divisions) {
            throw new RuntimeException("Invalid index " + index + " of entity " + parent);
        }
        this.index = index;
    }

    @Override
    public String toNuSMV() {
        return parent.appendIndex(index);
    }
}
