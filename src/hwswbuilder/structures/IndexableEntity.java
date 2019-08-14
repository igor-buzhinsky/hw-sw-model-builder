package hwswbuilder.structures;

import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public abstract class IndexableEntity<P extends NamedEntity> extends NamedEntity<P> {
    public final int divisions;
    final Collection<Integer> allDivisions;

    IndexableEntity(String name, P parent, int divisions) {
        super(name, parent);
        this.divisions = divisions;
        allDivisions = IntStream.rangeClosed(1, divisions).boxed().collect(Collectors.toUnmodifiableList());
    }

    String appendIndex(int index) {
        return toNuSMV() + "_DIV" + index;
    }
}
