package hwswbuilder.command;

import hwswbuilder.structures.Unit;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

class SymmetryCommand extends Command {
    SymmetryCommand(Map<String, String> arguments, Workspace workspace) {
        super(arguments, workspace);
    }

    @Override
    public void apply() {
        final String groupName = getArg("group");
        final String unitName = getArg("unit");
        final Unit unit = (Unit) resolveEntity(groupName + "." + unitName);
        final String strIndices = getArg("input_variable_indices");
        final List<Integer> inputVariableIndices = new ArrayList<>();
        for (String token : strIndices.split(",")) {
            final int value;
            try {
                value = Integer.parseInt(token);
            } catch (NumberFormatException e) {
                throw new RuntimeException("Formatting error in comma-separated list of integers: " + strIndices);
            }
            if (value < 1) {
                throw new RuntimeException("Invalid input variable index " + value + "; indexing starts from 1");
            }
            inputVariableIndices.add(value);
        }
        workspace.addSymmetry(unit, inputVariableIndices);
    }
}
