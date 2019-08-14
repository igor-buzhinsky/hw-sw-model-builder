package hwswbuilder.command;

import java.nio.file.Paths;
import java.util.Map;

class AddRequirementsCommand extends Command {
    private final String configFilename;

    AddRequirementsCommand(Map<String, String> arguments, Workspace workspace, String configFilename) {
        super(arguments, workspace);
        this.configFilename = configFilename;
    }

    @Override
    public void apply() {
        final String dir = parentFromFilename(configFilename);
        workspace.requirementsFilenames.add(Paths.get(dir, getArg("filename")).toString());
    }
}
