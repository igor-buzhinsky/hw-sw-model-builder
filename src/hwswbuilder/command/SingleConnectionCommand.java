package hwswbuilder.command;

import hwswbuilder.structures.*;

import java.util.Map;

class SingleConnectionCommand extends ConnectionCommand {
    SingleConnectionCommand(Map<String, String> arguments, Workspace workspace) {
        super(arguments, workspace);
    }

    @Override
    public void apply() {
        final NamedEntity<?> from = resolveEntity(getArg("from"));
        final NamedEntity<?> to = resolveEntity(getArg("to"));
        final boolean toIsUnit = to instanceof Indexing && to.parent() instanceof Unit;
        final RuntimeException failure = new RuntimeException("Cannot add single connection from "
                + from + " (" + from.getClass().getSimpleName() + ") to "
                + to + " (" + to.getClass().getSimpleName() + ")");
        if (from instanceof Constant) {
            if (to instanceof Output) {
                throw new RuntimeException("Connecting constants to outputs is currently not supported");
            } else if (toIsUnit) {
                ((Unit) to.parent()).addInputConnection(from, ((Indexing<?>) to).index);
            } else {
                throw failure;
            }
        } else if (from instanceof Indexing) {
            if (to instanceof Output) {
                ((Output) to).addInputConnection((Indexing<?>) from);
            } else if (toIsUnit) {
                ((Unit) to.parent()).addInputConnection(from, ((Indexing<?>) to).index);
            } else {
                throw failure;
            }
        } else {
            throw new RuntimeException("The source of a single connection must have a defined index");
        }
    }
}
