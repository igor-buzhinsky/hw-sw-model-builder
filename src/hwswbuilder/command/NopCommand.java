package hwswbuilder.command;

import java.util.Map;

class NopCommand extends Command {
    NopCommand(Map<String, String> arguments, Workspace workspace) {
        super(arguments, workspace);
    }

    @Override
    public void apply() {
    }
}
