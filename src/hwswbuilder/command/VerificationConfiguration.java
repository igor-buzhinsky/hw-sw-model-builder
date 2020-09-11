package hwswbuilder.command;

import hwswbuilder.structures.Unit;

import java.util.ArrayList;
import java.util.List;

public class VerificationConfiguration {
    private final Unit viewpointUnit;
    private final int viewpointDivision;
    private final List<Integer> failingDivisions;

    public int viewpointDivision() {
        return viewpointDivision;
    }

    public List<Integer> failingDivisions() {
        return new ArrayList<>(failingDivisions);
    }

    public VerificationConfiguration(Unit viewpointUnit, int viewpointDivision, List<Integer> failingDivisions) {
        this.viewpointUnit = viewpointUnit;
        this.viewpointDivision = viewpointDivision;
        this.failingDivisions = failingDivisions;
    }

    @Override
    public String toString() {
        return "VerificationConfiguration{" +
                "viewpointUnit=" + viewpointUnit +
                ", division=" + viewpointDivision +
                ", failingDivisions=" + failingDivisions +
                '}';
    }
}
