package hwswbuilder.command;

import java.nio.file.Paths;
import java.util.Map;

class SettingsCommand extends Command {
    private final String configFilename;

    SettingsCommand(Map<String, String> arguments, Workspace workspace, String configFilename) {
        super(arguments, workspace);
        this.configFilename = configFilename;
    }

    @Override
    public void apply() {
        final String dir = parentFromFilename(configFilename);
        for (Map.Entry<String, String> argument : arguments.entrySet()) {
            final String value = argument.getValue();
            switch (argument.getKey()) {
                case "basic_blocks_filename":
                    workspace.basicBlocksFilename = Paths.get(dir, value).toString();
                    break;
                case "output_filename":
                    workspace.outputFilename = Paths.get(dir, value).toString();
                    break;
                case "vanishing_failures":
                    workspace.vanishingFailures = Boolean.parseBoolean(value);
                    break;
            }
        }
    }
}
