package hwswbuilder.command;

import hwswbuilder.structures.*;
import org.apache.commons.lang3.tuple.Pair;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class Command {
    final Map<String, String> arguments;
    final Workspace workspace;

    public static final int NA_INDEX = -1;
    public static final int ALL_INDEX = -2;

    Command(Map<String, String> arguments, Workspace workspace) {
        this.arguments = arguments;
        this.workspace = workspace;
    }

    String getArg(String name) {
        if (arguments.containsKey(name)) {
            return arguments.get(name);
        }
        throw new RuntimeException("Mandatory argument " + name + " is missing");
    }

    String parentFromFilename(String filename) {
        final String dir = new File(filename).getParent();
        return dir == null ? "." : dir;
    }

    int getIntArg(String name, int minValue, int maxValue) {
        return getIntFromString(getArg(name), name, minValue, maxValue);
    }

    int getIntArgOrNA(String name, int minValue, int maxValue) {
        final String arg = getArg(name).toUpperCase();
        return arg.equals("NA") ? NA_INDEX : getIntArg(name, minValue, maxValue);
    }

    int getIntArgOrNAOrALL(String name, int minValue, int maxValue) {
        final String arg = getArg(name).toUpperCase();
        return arg.equals("NA") ? NA_INDEX : arg.equals("ALL") ? ALL_INDEX
                : getIntArg(name, minValue, maxValue);
    }

    private int getIntFromString(String str, String name, int minValue, int maxValue) {
        final int value;
        try {
            value = Integer.parseInt(str);
        } catch (NumberFormatException e) {
            throw new RuntimeException("Invalid value for " + name + ": " + str);
        }
        if (value < minValue) {
            throw new RuntimeException(name + " is set to " + value + " but may not be less than "
                    + minValue);
        }
        if (value > maxValue) {
            throw new RuntimeException(name + " is set to " + value + " but may not be greater than "
                    + maxValue);
        }
        return value;
    }

    int getDivisions() {
        return getIntArg("divisions", 1, Integer.MAX_VALUE);
    }

    NamedEntity<?> resolveEntity(String name) {
        final List<String> tokens = Arrays.asList(name.split("\\."));
        if (tokens.isEmpty()) {
            throw new RuntimeException("Empty entity name");
        }
        // resolve network
        final UnitGroup unitGroup = workspace.unitGroups.get(tokens.get(0));
        if (unitGroup != null) {
            // resolve unit in this network
            if (tokens.size() == 1) {
                throw new RuntimeException("Unit not specified for unit group " + name);
            }
            final String unitName = tokens.get(1);
            final Unit unit = workspace.units.get(Pair.of(unitGroup, unitName));
            if (unit == null) {
                throw new RuntimeException("Unit " + unitName + " not found in unit group " + name);
            }
            final int index;
            final String outputName;
            switch (tokens.size()) {
                case 2:
                    return unit;
                case 3:
                    // two cases are possible
                    final String newToken = tokens.get(2);
                    try {
                        // first just parse, maybe it is an incorrect int
                        Integer.parseInt(newToken);
                        index = getIntFromString(tokens.get(2), "index", 1, unit.divisions);
                        return new Indexing<>(unit, index);
                    } catch (NumberFormatException e) {
                        outputName = tokens.get(2);
                        final UnitOutput o = unit.outputs().get(outputName);
                        if (o == null) {
                            throw new RuntimeException("Unit " + unit + " has no output " + outputName);
                        }
                        return o;
                    }
                case 4:
                    outputName = tokens.get(2);
                    final UnitOutput o = unit.outputs().get(outputName);
                    if (o == null) {
                        throw new RuntimeException("Unit " + unit + " has no output " + outputName);
                    }
                    index = getIntFromString(tokens.get(3), "index", 1, o.divisions);
                    return new Indexing<>(o, index);
                default:
                    throw new RuntimeException("Entity name " + name + " is too long");
            }
        }

        if (!Arrays.asList(new String[] { "input", "output", "const" }).contains(tokens.get(0))) {
            throw new RuntimeException("Unknown entity name " + tokens.get(0)
                    + "; only `input', `output', `const' and unit names are allowed");
        }
        if (tokens.size() == 1) {
            throw new RuntimeException("Input/output name was not specified in " + name);
        }
        final String ioName = tokens.get(1);
        if (tokens.get(0).equals("const")) {
            return new Constant(tokens.get(1));
        }
        final IndexableEntity<?> io = (tokens.get(0).equals("input")
                ? workspace.inputs : workspace.outputs).get(ioName);
        if (io == null) {
            throw new RuntimeException("Input/output " + ioName + " is not defined.");
        }
        if (tokens.size() == 2) {
            return io;
        }
        final int index = getIntFromString(tokens.get(2), "index", 1, io.divisions);
        if (tokens.size() > 3) {
            throw new RuntimeException("Entity name " + name + " cannot proceed beyond indexing");
        }
        return new Indexing<>(io, index);
    }

    public static Command fromLine(String line, Workspace workspace, String configFilename,
                                   boolean loadUnitGroupsOnly) {
        final List<String> parts = Arrays.asList(line.split("\\s+"));
        assert !parts.isEmpty();
        final Map<String, String> arguments = new HashMap<>();
        for (String elem : parts.subList(1, parts.size())) {
            final String[] tokens = elem.split("=", 2);
            if (tokens.length != 2) {
                throw new RuntimeException("Invalid command argument: " + elem);
            }
            arguments.put(tokens[0], tokens[1]);
        }

        if (loadUnitGroupsOnly) {
            if (parts.get(0).equals("unit_group")) {
                return new UnitGroupCommand(arguments, workspace);
            } else {
                return new NopCommand(arguments, workspace);
            }
        }

        switch (parts.get(0)) {
            case "settings":
                return new SettingsCommand(arguments, workspace, configFilename);
            case "add_requirements":
                return new AddRequirementsCommand(arguments, workspace, configFilename);
            case "unit_group":
                return new UnitGroupCommand(arguments, workspace);
            case "input":
                return new InputCommand(arguments, workspace);
            case "output":
                return new OutputCommand(arguments, workspace);
            case "unit":
                return new UnitCommand(arguments, workspace);
            case "parallel_connection":
                return new ParallelConnectionCommand(arguments, workspace);
            case "all_to_all_connection":
                return new AllToAllConnectionCommand(arguments, workspace);
            case "single_connection":
                return new SingleConnectionCommand(arguments, workspace);
            case "symmetry":
                return new SymmetryCommand(arguments, workspace);
        }
        throw new RuntimeException("Unknown command in configuration file: " + parts.get(0));
    }

    public abstract void apply() throws IOException;

    @Override
    public String toString() {
        return getClass().getSimpleName() + arguments;
    }
}
