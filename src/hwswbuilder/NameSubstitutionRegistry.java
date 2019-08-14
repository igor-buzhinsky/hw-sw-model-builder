package hwswbuilder;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NameSubstitutionRegistry {
    private final Map<String, String> substitutions = new HashMap<>();

    public void addEntry(String from, String to) {
        substitutions.put(from, to);
    }

    private String resolve(String name) {
        assert name != null;
        return substitutions.getOrDefault(name, name);
    }

    public static String deferName(String name) {
        return "@<<" + name + ">>@";
    }

    public String replaceWithFinal(String code) {
        final Matcher m = Pattern.compile("@<<([\\w.]+)>>@").matcher(code);
        final StringBuilder sb = new StringBuilder();
        while (m.find()) {
            m.appendReplacement(sb, resolve(m.group(1)));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + substitutions;
    }
}
