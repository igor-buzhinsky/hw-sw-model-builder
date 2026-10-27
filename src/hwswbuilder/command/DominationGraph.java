package hwswbuilder.command;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Domination relation between verification configurations. Configurations that are strictly dominated by others or
 * equivalent to already chosen ones do not need to be verified.
 */
class DominationGraph {
    private final int n;
    private final List<String> nodeNames;
    private final List<VerificationConfiguration> nodeVerificationConfigurations;
    private final Map<String, Integer> nodeNameToIndex;
    private final boolean inferInformation;

    // incidence matrix
    private final Boolean[][] m;

    // whether this configuration should be verified
    private final boolean[] essential;

    DominationGraph(List<String> nodeNames, List<VerificationConfiguration> nodeVerificationConfigurations,
                    boolean inferInformation) {
        this.nodeNames = nodeNames;
        this.nodeVerificationConfigurations = nodeVerificationConfigurations;
        this.n = nodeNames.size();
        this.inferInformation = inferInformation;
        nodeNameToIndex = new HashMap<>();
        for (int i = 0; i < n; i++) {
            nodeNameToIndex.put(nodeNames.get(i), i);
        }
        m = new Boolean[n][n];
        if (inferInformation) {
            // each node always dominates itself
            for (int i = 0; i < n; i++) {
                m[i][i] = true;
            }
        }
        essential = new boolean[n];
    }

    List<VerificationConfiguration> essentialVerificationConfigurations() {
        final List<VerificationConfiguration> result = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            if (essential[i]) {
                result.add(nodeVerificationConfigurations.get(i));
            }
        }
        return result;
    }

    Boolean incident(String node1, String node2) {
        final int i1 = nodeNameToIndex.get(node1);
        final int i2 = nodeNameToIndex.get(node2);
        return m[i1][i2];
    }

    void setIncident(String node1, String node2, boolean value) {
        final int i1 = nodeNameToIndex.get(node1);
        final int i2 = nodeNameToIndex.get(node2);
        if (m[i1][i2] != null && m[i1][i2] != value) {
            throw new RuntimeException("Contradictory results for dominates(" + node1
                    + ", " + node2 + ")!");
        }
        m[i1][i2] = value;
        if (inferInformation) {
            boolean changed = true;
            while (changed) {
                changed = false;
                for (int i = 0; i < n; i++) {
                    for (int j = 0; j < n; j++) {
                        for (int k = 0; k < n; k++) {
                            // transitivity inference: m[i, j] & m[j, k] -> m[i, k]
                            if (m[i][j] != null && m[j][k] != null) {
                                if (m[i][j] && m[j][k]) {
                                    if (m[i][k] == null) {
                                        m[i][k] = true;
                                        changed = true;
                                    } else if (!m[i][k]) {
                                        throw new AssertionError();
                                    }
                                }
                            }
                            // reflexivity inference: m[i, j] & m[j, i] -> m[j, k] = m[i, k]
                            if (m[i][j] != null && m[j][i] != null) {
                                if (m[i][j] && m[j][i]) {
                                    if (m[j][k] == null && m[i][k] != null) {
                                        m[j][k] = m[i][k];
                                        changed = true;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                sb.append(m[i][j] ? 1 : 0);
            }
            sb.append(" # node ").append(nodeNames.get(i));
            if (essential[i]) {
                sb.append(" # essential configuration");
            }
            sb.append(System.lineSeparator());
        }
        return sb.toString();
    }

    void checkReflexivity() {
        for (int i = 0; i < n; i++) {
            if (!m[i][i]) {
                throw new AssertionError("Reflexivity check failed!");
            }
        }
    }

    void checkTransitivity() {
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                for (int k = 0; k < n; k++) {
                    if (m[i][j] && m[j][k] && !m[i][k]) {
                        throw new AssertionError("Transitivity check failed!");
                    }
                }
            }
        }
    }

    void findEssentialConfigurations() {
        l_main: for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                // if strictly dominated by some configuration,
                // no need to verify
                if (m[j][i] && !m[i][j]) {
                    continue l_main;
                }
                // if equivalent to some configuration previously identified as essential,
                // no need to verify
                if (m[j][i] && m[i][j] && essential[j]) {
                    continue l_main;
                }
            }
            // otherwise, need to verify this configuration
            essential[i] = true;
        }
    }
}
