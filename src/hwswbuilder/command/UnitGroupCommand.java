package hwswbuilder.command;

import hwswbuilder.structures.UnitGroup;

import java.util.Map;

class UnitGroupCommand extends Command {
    UnitGroupCommand(Map<String, String> arguments, Workspace workspace) {
        super(arguments, workspace);
    }

    @Override
    public void apply() {
        final String name = getArg("name");
        final int divisions = getDivisions();
        final int failingDivision = getIntArgOrNA("failing_division", 1, divisions);
        if (failingDivision > divisions) {
            throw new RuntimeException(String.format("Division index must not exceed %d for unit group %s",
                    divisions, name));
        }
        workspace.unitGroups.put(name, new UnitGroup(name, divisions, failingDivision));
    }
}
