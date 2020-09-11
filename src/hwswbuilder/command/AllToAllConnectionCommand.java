package hwswbuilder.command;

import hwswbuilder.structures.*;

import java.util.Map;

class AllToAllConnectionCommand extends ConnectionCommand {
    AllToAllConnectionCommand(Map<String, String> arguments, Workspace workspace) {
        super(arguments, workspace);
    }

    @Override
    public void apply() {
        final NamedEntity<?> from = resolveEntity(getArg("from"));
        final NamedEntity<?> to = resolveEntity(getArg("to"));
        if (from instanceof IndexableEntity && to instanceof IndexableEntity) {
            final IndexableEntity<?> fromInd = (IndexableEntity<?>) from;
            if (to instanceof Unit) {
                final Unit toAsUnit = (Unit) to;
                for (int i = 1; i <= fromInd.divisions; i++) {
                    final Indexing<?> indexing = new Indexing<>(fromInd, i);
                    for (int j = 1; j <= toAsUnit.divisions; j++) {
                        toAsUnit.addInputConnection(indexing, j);
                    }
                }
            } else {
                throw new RuntimeException("The destination of an all-to-all connection must be a unit in " + this);
            }
        } else {
            throw new RuntimeException("Connection points must be indexable in " + this);
        }
    }
}
