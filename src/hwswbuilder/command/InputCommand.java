package hwswbuilder.command;

import hwswbuilder.structures.Input;

import java.util.Map;

class InputCommand extends Command {
    InputCommand(Map<String, String> arguments, Workspace workspace) {
        super(arguments, workspace);
    }

    @Override
    public void apply() {
        final String name = getArg("name");
        workspace.inputs.put(name, new Input(name, getDivisions(), getArg("nusmv_type")));
    }
}
