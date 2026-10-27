package hwswbuilder;

import hwswbuilder.structures.Constant;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class ModuleGenerationTest {
    @Test
    void deferredNamesAreResolvedThroughRegistry() {
        final NameSubstitutionRegistry registry = new NameSubstitutionRegistry();
        registry.addEntry("X_DIV1", "X_NF_DIV1.OUT0");
        final String code = "a := " + NameSubstitutionRegistry.deferName("X_DIV1") + "; b := "
                + NameSubstitutionRegistry.deferName("U_DIV2.OUT") + ";";
        assertEquals("a := X_NF_DIV1.OUT0; b := U_DIV2.OUT;", registry.replaceWithFinal(code));
    }

    @Test
    void delayModulesAreSharedPerTypeList() {
        final CustomModuleHandler handler = new CustomModuleHandler();
        assertEquals("NONDET_DELAY_0", handler.delayModule(Arrays.asList("boolean")));
        assertEquals("NONDET_DELAY_1", handler.delayModule(Arrays.asList("boolean", "0..10")));
        assertEquals("NONDET_DELAY_0", handler.delayModule(Arrays.asList("boolean")));
        final String code = handler.nuSMVDeclaration();
        assertTrue(code.contains("MODULE NONDET_DELAY_0(IN0, IN0_FAULT, IN0_CONNECTED)"), code);
        assertTrue(code.contains("MODULE NONDET_DELAY_1(IN0, IN0_FAULT, IN0_CONNECTED, IN1, IN1_FAULT, "
                + "IN1_CONNECTED)"), code);
        assertTrue(code.contains("    OUT1: 0..10;"), code);
        assertFalse(code.contains("NONDET_DELAY_2"), code);
    }

    @Test
    void failureInjectionModulesAreAlwaysDeclared() {
        final String code = new CustomModuleHandler().nuSMVDeclaration();
        assertTrue(code.startsWith("MODULE INJECT_FAILURE("), code);
        assertTrue(code.contains("MODULE INJECT_VANISHING_FAILURE("), code);
    }

    @Test
    void unitWrappersAreNamedAfterModuleAndDivision() {
        final CustomModuleHandler handler = new CustomModuleHandler();
        assertEquals("WRAPPER_APU_DIV2", handler.unitWrapper("(A)\nVAR\n", "APU", 2));
        assertTrue(handler.nuSMVDeclaration().contains("MODULE WRAPPER_APU_DIV2(A)"));
    }

    @Test
    void constants() {
        assertEquals("TRUE", new Constant("TRUE").toNuSMV());
        assertEquals("FALSE", new Constant("FALSE").toNuSMV());
        assertEquals("-42", new Constant("-42").toNuSMV());
        assertEquals("100", new Constant("100").toNuSMV());
        assertEquals("0", new Constant("0").toNuSMV());
        assertThrows(RuntimeException.class, () -> new Constant("-0"));
        assertThrows(RuntimeException.class, () -> new Constant("007"));
        assertThrows(RuntimeException.class, () -> new Constant("true"));
        assertThrows(RuntimeException.class, () -> new Constant("1.5"));
        assertThrows(RuntimeException.class, () -> new Constant("x"));
    }
}
