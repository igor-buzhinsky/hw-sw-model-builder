package hwswbuilder.command;

import hwswbuilder.structures.Output;
import hwswbuilder.structures.UnitGroup;

import java.util.Map;

class OutputCommand extends Command {
    OutputCommand(Map<String, String> arguments, Workspace workspace) {
        super(arguments, workspace);
    }

    @Override
    public void apply() {
        final String parentName = getArg("in");
        final UnitGroup parent = workspace.unitGroups.get(parentName);
        if (parent == null) {
            throw new RuntimeException("Unit group not (yet?) created: " + parentName);
        }
        final String name = getArg("name");
        workspace.outputs.put(name, new Output(name, parent));
    }
}
