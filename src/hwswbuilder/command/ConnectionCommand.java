package hwswbuilder.command;

import java.util.Map;

abstract class ConnectionCommand extends Command {
    ConnectionCommand(Map<String, String> arguments, Workspace workspace) {
        super(arguments, workspace);
    }
}
