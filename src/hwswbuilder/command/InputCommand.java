package hwswbuilder.command;

import hwswbuilder.structures.Input;
import hwswbuilder.structures.UnitGroup;

import java.util.Map;

class InputCommand extends Command {
    InputCommand(Map<String, String> arguments, Workspace workspace) {
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
        workspace.inputs.put(name, new Input(name, parent, getArg("nusmv_type")));
    }
}
