package hwswbuilder.structures;

import java.util.Arrays;

public class Constant extends NamedEntity<Constant> {
    public Constant(String name) {
        super(name, null);
        if (!name.matches("^(TRUE|FALSE|(-?[1-9][0-9]*))$")) {
            throw new RuntimeException("Incorrect constant " + name + "; only TRUE, FALSE and integers are allowed");
        }
    }

    String nusmvType() {
        return Arrays.asList("TRUE", "FALSE").contains(name) ? "boolean" : "{" + name + "}";
    }

    @Override
    public String toNuSMV() {
        return name;
    }
}
