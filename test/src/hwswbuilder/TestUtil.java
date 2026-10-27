package hwswbuilder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Helpers shared by the tests.
 *
 * Golden files live in test/golden. To regenerate them (e.g. after an intended change of the generated models),
 * run the tests with -Dhwswbuilder.updateGolden=true and review the diff.
 */
public final class TestUtil {
    private TestUtil() {
    }

    public static Path repoRoot() {
        return Paths.get(System.getProperty("hwswbuilder.repoRoot", ".")).toAbsolutePath().normalize();
    }

    public static Path caseStudy(String name) {
        return repoRoot().resolve("case_studies").resolve(name);
    }

    /**
     * Copies a case study directory into the given directory so that the tool can write its outputs there.
     * @return the path of the copy.
     */
    public static Path copyCaseStudy(String name, Path targetParent) throws IOException {
        final Path source = caseStudy(name);
        final Path target = targetParent.resolve(name);
        try (Stream<Path> paths = Files.walk(source)) {
            for (Path p : (Iterable<Path>) paths::iterator) {
                final Path dest = target.resolve(source.relativize(p).toString());
                if (Files.isDirectory(p)) {
                    Files.createDirectories(dest);
                } else {
                    Files.copy(p, dest);
                }
            }
        }
        return target;
    }

    public static String read(Path path) throws IOException {
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }

    private static String normalizeLineEndings(String s) {
        return s.replace("\r\n", "\n");
    }

    public static void assertMatchesGolden(String goldenName, String actual) throws IOException {
        final Path golden = repoRoot().resolve("test").resolve("golden").resolve(goldenName);
        if (Boolean.getBoolean("hwswbuilder.updateGolden")) {
            Files.createDirectories(golden.getParent());
            Files.write(golden, normalizeLineEndings(actual).getBytes(StandardCharsets.UTF_8));
            return;
        }
        assertEquals(normalizeLineEndings(read(golden)), normalizeLineEndings(actual),
                "Output differs from golden file " + golden);
    }

    /**
     * Runs the given action while capturing everything it prints to System.out.
     */
    public static String captureStdout(ThrowingRunnable action) throws Exception {
        final PrintStream original = System.out;
        final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        System.setOut(new PrintStream(buffer, true, StandardCharsets.UTF_8));
        try {
            action.run();
        } finally {
            System.setOut(original);
        }
        return buffer.toString(StandardCharsets.UTF_8);
    }

    @FunctionalInterface
    public interface ThrowingRunnable {
        void run() throws Exception;
    }

    public static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }

    /**
     * Checks whether the given command can be started and exits successfully.
     */
    public static boolean commandWorks(String... command) {
        try {
            final Process p = new ProcessBuilder(command).redirectErrorStream(true)
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD).start();
            p.getOutputStream().close();
            return p.waitFor(30, TimeUnit.SECONDS) && p.exitValue() == 0;
        } catch (IOException | InterruptedException e) {
            return false;
        }
    }

    /**
     * Creates an executable shell script that stands in for NuSMV: it appends the model it receives to
     * {@code recordFile} and reports every specification as having the given outcome.
     */
    public static Path fakeNuSMV(Path dir, Path recordFile, boolean outcome) throws IOException {
        return fakeNuSMV(dir, recordFile, outcome, 0);
    }

    /**
     * Same as {@link #fakeNuSMV(Path, Path, boolean)}, but the script terminates with the given exit status.
     */
    public static Path fakeNuSMV(Path dir, Path recordFile, boolean outcome, int exitStatus) throws IOException {
        return script(dir, "fake-nusmv-" + outcome + "-" + exitStatus + ".sh",
                "model=\"$1\"",
                "echo \"=== $(basename \"$model\")\" >> '" + recordFile + "'",
                "cat \"$model\" >> '" + recordFile + "'",
                "echo '-- specification AG (fake) is " + outcome + "'",
                "exit " + exitStatus);
    }

    /**
     * Creates an executable shell script with the given lines.
     */
    public static Path script(Path dir, String name, String... lines) throws IOException {
        final Path script = dir.resolve(name);
        final String content = "#!/bin/sh\n" + String.join("\n", lines) + "\n";
        Files.write(script, content.getBytes(StandardCharsets.UTF_8));
        Files.setPosixFilePermissions(script, PosixFilePermissions.fromString("rwxr-xr-x"));
        return script;
    }
}
