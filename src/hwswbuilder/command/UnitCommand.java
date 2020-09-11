package hwswbuilder.command;

import hwswbuilder.structures.UnitGroup;
import hwswbuilder.structures.Unit;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

class UnitCommand extends Command {
    UnitCommand(Map<String, String> arguments, Workspace workspace) {
        super(arguments, workspace);
    }

    @Override
    public void apply() throws IOException {
        final String parentName = getArg("in");
        final UnitGroup parent = workspace.unitGroups.get(parentName);
        if (parent == null) {
            throw new RuntimeException("Unit group not (yet?) created: " + parentName);
        }
        final String name = getArg("name");
        final Map<String, String> outputsWithTypes = new LinkedHashMap<>();
        final String outputsStr = getArg("nusmv_outputs");
        for (String chunk : outputsStr.split(";")) {
            final String[] tokens = chunk.split(":");
            if (tokens.length != 2) {
                throw new RuntimeException(String.format(
                        "Output definition %s for unit %s.%s must be in the form name:nusmv_type",
                        chunk, parentName, name));
            }
            outputsWithTypes.put(tokens[0], tokens[1]);
        }
        final int divisionToRetain = getIntArgOrNA("single_division_to_retain", 1, parent.divisions);
        if (divisionToRetain > parent.divisions) {
            throw new RuntimeException(String.format("Division index must not exceed %d for unit group %s",
                    parent.divisions, parent.toNuSMV()));
        }
        workspace.units.put(Pair.of(parent, name), new Unit(name, parent,
                getArg("nusmv_module_name"), getArg("filename"),
                divisionToRetain, getIntArg("max_delay", 0, Integer.MAX_VALUE), outputsWithTypes,
                workspace.configDir()));
    }
}
