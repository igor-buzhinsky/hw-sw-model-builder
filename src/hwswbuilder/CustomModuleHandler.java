package hwswbuilder;

import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static hwswbuilder.structures.NamedEntity.SEP;
import static hwswbuilder.structures.NamedEntity.PAD;

public class CustomModuleHandler {
    private int delayModulesCount = 0;

    // list of types -> Pair(module name, module code)
    private final Map<List<String>, Pair<String, String>> customDelayModules = new LinkedHashMap<>();

    // module name -> code
    private final Map<String, String> unitWrappers = new LinkedHashMap<>();

    /**
     * Gives a unique name for the given code.
     * @param code: unit module code without "MODULE <module name>".
     * @return module name.
     */
    public String unitWrapper(String code, String contentModuleName, int division) {
        final String moduleName = "WRAPPER_" + contentModuleName + "_DIV" + division;
        unitWrappers.put(moduleName, code);
        return moduleName;
    }

    private final static String FAILURE_MODULE = String.join(SEP, Arrays.asList(
            "MODULE INJECT_FAILURE(IN1, IN1_FAULT, IN1_CONNECTED, SUBSTITUTE, SUBSTITUTE_FAULT, SUBSTITUTE_CONNECTED, FAILURE_VANISHED)",
            "VAR",
            "    OUT1_FAULT: boolean;",
            "DEFINE",
            "    OUT1 := SUBSTITUTE;",
            "    FAILURE := IN1 != OUT1 | IN1_FAULT != OUT1_FAULT;"
    ));

    private final static String VANISHING_FAILURE_MODULE = String.join(SEP, Arrays.asList(
            "MODULE INJECT_VANISHING_FAILURE(IN1, IN1_FAULT, IN1_CONNECTED, SUBSTITUTE, SUBSTITUTE_FAULT, SUBSTITUTE_CONNECTED, FAILURE_VANISHED)",
            "VAR",
            "    OUT1_FAULT_: boolean;",
            "DEFINE",
            "    OUT1       := FAILURE_VANISHED ? IN1       : SUBSTITUTE;",
            "    OUT1_FAULT := FAILURE_VANISHED ? IN1_FAULT : OUT1_FAULT_;",
            "    FAILURE    := IN1 != OUT1 | IN1_FAULT != OUT1_FAULT;"
    ));

    public String nuSMVDeclaration() {
        final List<String> parts = new ArrayList<>() {{
            add(FAILURE_MODULE + SEP + SEP + VANISHING_FAILURE_MODULE + SEP);
        }};
        if (!customDelayModules.isEmpty()) {
            parts.add(customDelayModules.values().stream().map(Pair::getRight)
                    .collect(Collectors.joining(SEP)));
        }
        if (!unitWrappers.isEmpty()) {
            parts.add(unitWrappers.entrySet().stream()
                            .map(e -> "MODULE " + e.getKey() + e.getValue())
                            .collect(Collectors.joining(SEP)));
        }
        return String.join(SEP, parts);
    }

    public String delayModule(List<String> nusmvTypes) {
        final Pair<String, String> module = customDelayModules.get(nusmvTypes);
        if (module != null) {
            return module.getLeft();
        }
        final int index = delayModulesCount++;
        final StringBuilder sb = new StringBuilder();
        final String moduleName = "NONDET_DELAY_" + index;
        sb.append("MODULE ").append(moduleName).append("(");
        sb.append(IntStream.range(0, nusmvTypes.size())
                        .mapToObj(i -> String.format("IN%d, IN%d_FAULT, IN%d_CONNECTED", i, i, i))
                        .collect(Collectors.joining(", ")));
        sb.append(")").append(SEP);
        sb.append("VAR").append(SEP);
        for (int i = 0; i < nusmvTypes.size(); i++) {
            final String start = "    OUT" + i;
            sb.append(start).append(": ").append(nusmvTypes.get(i)).append(";").append(SEP);
            sb.append(start).append("_FAULT: boolean;").append(SEP);
        }
        sb.append("    delaying: boolean;").append(SEP);
        sb.append("ASSIGN").append(SEP);
        for (int i = 0; i < nusmvTypes.size(); i++) {
            for (String suffix : Arrays.asList("", "_FAULT")) {
                sb.append(PAD).append("init(OUT").append(i).append(suffix)
                        .append(") := IN").append(i).append(suffix).append(";").append(SEP);
                sb.append(PAD).append("next(OUT").append(i).append(suffix)
                        .append(") := delaying ? IN").append(i).append(suffix)
                        .append(" : next(IN").append(i).append(suffix).append(");").append(SEP);
            }
        }
        sb.append("TRANS delaying & (").append(IntStream.range(0, nusmvTypes.size())
                .mapToObj(i -> String.format("IN%d != next(IN%d) | IN%d_FAULT != next(IN%d_FAULT)", i, i, i, i))
                .collect(Collectors.joining(" | "))).append(") -> next(delaying)").append(SEP);
        customDelayModules.put(nusmvTypes, Pair.of(moduleName, sb.toString()));
        return moduleName;
    }
}
