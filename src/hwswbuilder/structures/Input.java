package hwswbuilder.structures;

import hwswbuilder.command.Workspace;

public class Input extends IndexableEntity<UnitGroup> implements CodeProducer {
    final String nusmvType;

    public Input(String name, UnitGroup parentUnitGroup, String nusmvType) {
        super(name, parentUnitGroup, parentUnitGroup.divisions);
        this.nusmvType = nusmvType;
    }

    @Override
    public void nuSMVDeclaration(Workspace workspace) {
        for (int div : allDivisions) {
            final String fullName = appendIndex(div);
            workspace.addVar(fullName + ": " + nusmvType);
            if (workspace.mimicMODCHK) {
                workspace.addVar(fullName + "_FAULT: boolean");
                // fault masking
                final String nfModuleName = nusmvType.equals("boolean") ? "NF" : "NFA";
                final String nfName = name + "_NF_DIV" + div;
                final String nfNameWithOutput = nfName + ".OUT0";
                workspace.addVar(nfName + ": " + nfModuleName + "(" + fullName + ", " + fullName + "_FAULT, TRUE)");
                workspace.registry.addEntry(fullName, nfNameWithOutput);
            } else {
                workspace.addDefine(fullName + "_FAULT := FALSE");
            }
        }
    }

    @Override
    public String toNuSMV() {
        return name;
    }
}
