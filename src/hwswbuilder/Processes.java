package hwswbuilder;

public final class Processes {
    private Processes() {
    }

    /**
     * Waits for an external process to terminate.
     * @param p: the process.
     * @return its exit code.
     */
    public static int waitFor(Process p) {
        try {
            return p.waitFor();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting for an external process", e);
        }
    }
}
