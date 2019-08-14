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
    private final Map<List<String>, Pair<String, String>> customDelayModules
            = new LinkedHashMap<>();

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
            "MODULE INJECT_FAILURE(IN1, IN1_FAULT, IN1_CONNECTED, SUBSTITUTE, SUBSTITUTE_FAULT, SUBSTITUTE_CONNECTED)",
            "VAR",
            "    OUT1_FAULT: boolean;",
            "    FAILURE: boolean;",
            "DEFINE",
            "    OUT1 := SUBSTITUTE;",
            "INIT !FAILURE -> OUT1_FAULT = IN1_FAULT & SUBSTITUTE = IN1",
            "TRANS !next(FAILURE) -> next(OUT1_FAULT) = next(IN1_FAULT) & next(SUBSTITUTE) = next(IN1)"
    ));

    public String nuSMVDeclaration() {
        final List<String> parts = new ArrayList<>() {{ add(FAILURE_MODULE + SEP); }};
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
//        MODULE NONDET_BINARY_PAIRED_DELAY(IN0, IN0_FAULT, IN0_CONNECTED, IN1, IN1_FAULT, IN1_CONNECTED)
//        VAR
//        OUT0: boolean;
//        OUT0_FAULT: boolean;
//        OUT1: boolean;
//        OUT1_FAULT: boolean;
//        delaying: boolean;
//        ASSIGN
//
//        init(OUT0) := IN0;
//        next(OUT0) := delaying ? IN0 : next(IN0);
//        init(OUT0_FAULT) := IN0_FAULT;
//        next(OUT0_FAULT) := delaying ? IN0_FAULT : next(IN0_FAULT);
//
//        init(OUT1) := IN1;
//        next(OUT1) := delaying ? IN1 : next(IN1);
//        init(OUT1_FAULT) := IN1_FAULT;
//        next(OUT1_FAULT) := delaying ? IN1_FAULT : next(IN1_FAULT);
//        TRANS
//                -- disable delay removals which may lead to losing a pulse of length 1
//        delaying & (IN0 != next(IN0) | IN0_FAULT != next(IN0_FAULT)
//                | IN1 != next(IN1) | IN1_FAULT != next(IN1_FAULT)) -> next(delaying)

    }
}
