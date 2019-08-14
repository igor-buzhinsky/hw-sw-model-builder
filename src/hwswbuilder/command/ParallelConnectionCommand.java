package hwswbuilder.command;

import hwswbuilder.structures.*;

import java.util.Map;

class ParallelConnectionCommand extends ConnectionCommand {
    ParallelConnectionCommand(Map<String, String> arguments, Workspace workspace) {
        super(arguments, workspace);
    }

    @Override
    public void apply() {
        final NamedEntity from = resolveEntity(getArg("from"));
        final NamedEntity to = resolveEntity(getArg("to"));
        if (!(to instanceof IndexableEntity)) {
            throw new RuntimeException("Connection destination must be indexable in " + this);
        }
        final IndexableEntity toInd = (IndexableEntity) to;

        final RuntimeException failure = new RuntimeException("Cannot add connections from "
                + from + " (" + from.getClass().getSimpleName() + ") to "
                + to + " (" + to.getClass().getSimpleName() + ")");

        if (from instanceof Constant) {
            if (to instanceof Output) {
                throw new RuntimeException("Connecting constants to outputs is currently not supported");
            } else if (to instanceof Unit) {
                for (int i = 1; i <= toInd.divisions; i++) {
                    ((Unit) to).addInputConnection(from, i);
                }
            } else {
                throw failure;
            }
        } else if (from instanceof IndexableEntity) {
            final IndexableEntity fromInd = (IndexableEntity) from;
            if (fromInd.divisions != toInd.divisions) {
                throw new RuntimeException("Indexable connection points must have the same number of dimensions in "
                        + this);
            }
            for (int i = 1; i <= toInd.divisions; i++) {
                final Indexing indexing = new Indexing<>(fromInd, i);
                if (to instanceof Output) {
                    ((Output) to).addInputConnection(indexing);
                } else if (to instanceof Unit) {
                    ((Unit) to).addInputConnection(indexing, i);
                } else {
                    throw failure;
                }
            }
        } else {
            throw new RuntimeException("Connection source must be indexable or constant in " + this);
        }
    }
}
