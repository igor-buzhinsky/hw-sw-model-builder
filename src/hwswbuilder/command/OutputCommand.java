package hwswbuilder.command;

import hwswbuilder.structures.Output;

import java.util.Map;

class OutputCommand extends Command {
    OutputCommand(Map<String, String> arguments, Workspace workspace) {
        super(arguments, workspace);
    }

    @Override
    public void apply() {
        final String name = getArg("name");
        workspace.outputs.put(name, new Output(name, getDivisions()));
    }
}
