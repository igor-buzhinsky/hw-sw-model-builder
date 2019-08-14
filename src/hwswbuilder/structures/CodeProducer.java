package hwswbuilder.structures;

import hwswbuilder.command.Workspace;

import java.util.Arrays;
import java.util.List;

public interface CodeProducer {
    void nuSMVDeclaration(Workspace workspace);

    class InputInfo {
        final String argName;
        final String nusmvType;
        final boolean injectFailure;
        final boolean isConstant;

        private InputInfo(String argName, String nusmvType, boolean injectFailure, boolean isConstant) {
            this.argName = argName;
            this.nusmvType = nusmvType;
            this.injectFailure = injectFailure;
            this.isConstant = isConstant;
        }

        InputInfo(String argName, String nusmvType, int division, boolean injectFailure) {
            this(argName + "_DIV" + division, nusmvType, injectFailure, false);
        }

        InputInfo(String binConstValue) {
            this(binConstValue, "boolean", false, true);
        }

        String failureName(boolean full) {
            assert !isConstant;
            return argName + "_AFTER_FAILURE" + (full ? ".OUT1" : "");
        }

        /**
         * Failure declaration with custom argument name.
         * @param arg: argument name.
         * @return fault: module declaration.
         */
        List<String> failureDecl(String arg) {
            assert !isConstant;
            return Arrays.asList(
                    String.format("%s_SUBS: %s", arg, nusmvType),
                    String.format("%s: INJECT_FAILURE(%s, %s_FAULT, TRUE, %s_SUBS, FALSE, TRUE)",
                            failureName(false), arg, arg, arg)
            );
        }

        @Override
        public String toString() {
            return "InputInfo{" +
                    "argName='" + argName + '\'' +
                    ", nusmvType='" + nusmvType + '\'' +
                    ", injectFailure=" + injectFailure +
                    ", isConstant=" + isConstant +
                    '}';
        }
    }
}
