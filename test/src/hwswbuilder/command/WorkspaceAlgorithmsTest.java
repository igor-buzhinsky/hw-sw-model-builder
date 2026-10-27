package hwswbuilder.command;

import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class WorkspaceAlgorithmsTest {
    @Test
    void nextPermutationEnumeratesAllPermutationsInLexicographicOrder() {
        final int[] p = { 1, 2, 3, 4 };
        final List<List<Integer>> seen = new ArrayList<>();
        for (int i = 0; i < 24; i++) {
            final List<Integer> current = new ArrayList<>();
            for (int x : p) {
                current.add(x);
            }
            seen.add(current);
            NextPermutation.nextPermutation(p);
        }
        assertEquals(24, new HashSet<>(seen).size());
        final List<List<Integer>> sorted = new ArrayList<>(seen);
        sorted.sort((a, b) -> {
            for (int i = 0; i < a.size(); i++) {
                final int c = Integer.compare(a.get(i), b.get(i));
                if (c != 0) {
                    return c;
                }
            }
            return 0;
        });
        assertEquals(sorted, seen);
        // after the last permutation, wraps around to the identity
        assertArrayEquals(new int[] { 1, 2, 3, 4 }, p);
    }

    @Test
    void nextPermutationOfSingleElementIsIdentity() {
        final int[] p = { 1 };
        NextPermutation.nextPermutation(p);
        assertArrayEquals(new int[] { 1 }, p);
    }

    @Test
    void cartesianProduct() {
        assertEquals(Arrays.asList(
                Arrays.asList(1, 3), Arrays.asList(2, 3),
                Arrays.asList(1, 4), Arrays.asList(2, 4)),
                Workspace.cartesianProduct(Arrays.asList(Arrays.asList(1, 2), Arrays.asList(3, 4))));
    }

    @Test
    void cartesianProductOfNothingIsSingleEmptyTuple() {
        assertEquals(Collections.singletonList(Collections.emptyList()),
                Workspace.cartesianProduct(Collections.emptyList()));
    }

    @Test
    void cartesianProductWithEmptyFactorIsEmpty() {
        assertTrue(Workspace.cartesianProduct(Arrays.asList(Arrays.asList(1, 2), Collections.emptyList()))
                .isEmpty());
    }

    @Test
    void configSafeIndex() {
        assertEquals("NA", Workspace.configSafeIndex(Command.NA_INDEX));
        assertEquals("ALL", Workspace.configSafeIndex(Command.ALL_INDEX));
        assertEquals("3", Workspace.configSafeIndex(3));
    }

    @Test
    void failureFlagDeclaration() {
        assertEquals("    FAILURE := FALSE;", Workspace.failureFlagDeclaration(Collections.emptyList()));
        assertEquals("    FAILURE := a.FAILURE | b.FAILURE;",
                Workspace.failureFlagDeclaration(Arrays.asList("a.FAILURE", "b.FAILURE")));
    }
}
