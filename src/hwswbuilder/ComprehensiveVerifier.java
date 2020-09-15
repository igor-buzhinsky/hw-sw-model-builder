package hwswbuilder;

import hwswbuilder.command.Command;
import hwswbuilder.command.VerificationConfiguration;
import hwswbuilder.command.Workspace;
import hwswbuilder.structures.Unit;
import hwswbuilder.structures.UnitGroup;
import org.apache.commons.lang3.tuple.Pair;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.spi.BooleanOptionHandler;

import java.io.*;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class ComprehensiveVerifier {
    @Argument(usage = "configuration file", metaVar = "files", required = true)
    private String configFilename;

    @Option(name = "--requirementsFilename", usage = "requirements file", required = true)
    private String requirementsFilename;

    @Option(name = "--nusmvCommand", aliases = { "-c" },
            usage = "will run <command> <output SMV file> in the end", required = true,
            metaVar = "<command>")
    private String nusmvCommand;

    @Option(name = "--configSubstitutions", aliases = { "-s" },
            usage = "fill in placeholders in the config file, e.g. V1={3,3};file=x.smv will substitute" +
                    " ${V1} with {3,3} and ${file} with x.smv",
            metaVar = "<substitutions>")
    private String substitutions;

    @Option(name = "--checkSymmetryNuSMVCommand",
            usage = "perform symmetry checks in addition to generating the model; " +
                    "use the provided NuSMV command")
    private String checkSymmetryNuSMVCommand;

    @Option(name = "--printToConsole", handler = BooleanOptionHandler.class,
            usage = "print generated files and more to console")
    private boolean printToConsole;

    @Option(name = "--prologFilename", metaVar = "filename",
            usage = "generate a Prolog representation of a configuration")
    private String prologFilename;

    @Option(name = "--prologExecutable", metaVar = "command",
            usage = "path to Prolog executable (default: prolog)")
    private String prologExecutable = "prolog";

    @Option(name = "--prologThoroughQueries", handler = BooleanOptionHandler.class,
            usage = "make queries even if their results may be inferred based on transitivity or reflexivity")
    private boolean prologThoroughQueries;

    @Option(name = "--logFilenameWithoutFailures", metaVar = "filename",
            usage = "write log int this file for verification without failures", required = true)
    private String logFilenameWithoutFailures;

    @Option(name = "--logFilenameWithFailures", metaVar = "filename",
            usage = "write log int this file for verification with failures", required = true)
    private String logFilenameWithFailures;

    @Option(name = "--optimizeOutUnreachable", handler = BooleanOptionHandler.class,
            usage = "optimize out unreachable unit instances from the formal model")
    private boolean optimizeOutUnreachable;

    public static void main(String[] args) {
        new ComprehensiveVerifier().run(args);
    }

    private void run(String[] args) {
        Locale.setDefault(Locale.US);
        if (!parseArgs(args)) {
            return;
        }
        try {
            launcher();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private boolean parseArgs(String[] args) {
        final CmdLineParser parser = new CmdLineParser(this);
        try {
            parser.parseArgument(args);
            return true;
        } catch (CmdLineException e) {
            System.out.println();
            System.out.print("Usage: java -jar comprehensive_verifier.jar ");
            parser.printSingleLineUsage(System.out);
            System.out.println();
            parser.printUsage(System.out);
            return false;
        }
    }

    private final Map<Pair<String, List<List<Integer>>>, List<VerificationConfiguration>>
            memoizedPrologAlalysisResults = new HashMap<>();

    private List<VerificationConfiguration> memoizedPrologAnalysis(Workspace workspace,
            String viewpointUnit, List<List<Integer>> failingDivisions) throws IOException {
        final Pair<String, List<List<Integer>>> arg = Pair.of(viewpointUnit, failingDivisions);
        List<VerificationConfiguration> precomputed = memoizedPrologAlalysisResults.get(arg);
        if (precomputed == null) {
            // create Prolog file
            final long timeBefore = System.nanoTime();

            final String prologModel = workspace.toProlog(true, true);
            if (printToConsole) {
                System.out.println(prologModel);
            }
            try (PrintWriter pw = new PrintWriter(new File(prologFilename))) {
                pw.println(prologModel);
            }

            // call analyzer
            precomputed = workspace.queryProlog(prologFilename, prologExecutable,
                    viewpointUnit, prologThoroughQueries, Workspace.cartesianProduct(failingDivisions));
            final long timeAfter = System.nanoTime();
            System.out.println(String.format("Symmetry analysis time: %.1f s",
                    (timeAfter - timeBefore) / 1.0e9));
            memoizedPrologAlalysisResults.put(arg, precomputed);
        }
        return precomputed;
    }

    private List<String> getSubstitutionsString(String initial, String... added) {
        return new ArrayList<>() {{
            addAll(Arrays.asList(initial.split("; +")));
            addAll(Arrays.asList(added));
        }};
    }

    private static final String DUMMY_REQ_FILENAME = ".requirements.smv";

    private String dummyReqPath() {
        return Paths.get(new File(faultFreeWorkspace.outputFilename()).getParent(), DUMMY_REQ_FILENAME).toString();
    }

    private Workspace faultFreeWorkspace;
    private Map<String, UnitGroup> unitGroups;

    private class AnnotatedRequirement {
        final String pattern;
        final String viewpoint;
        final Unit viewpointUnit;
        final Set<String> singleFailures;
        final Set<String> allFailures;
        final String body;
        final String prefix;

        private Set<String> loadSet(Map<String, String> args, String argName) {
            final String arg = args.get(argName);
            return arg == null ? Collections.emptySet() : new HashSet<>(Arrays.asList(arg.split(",")));
        }

        AnnotatedRequirement(String req) {
            final String[] parts = req.split("]] ", 2);
            prefix = parts[0].replaceAll("^\\[\\[REQ ", "");
            final String[] prefixParts = prefix.split(" +");
            final Map<String, String> args = new LinkedHashMap<>();
            for (String prefixPart : prefixParts) {
                final String[] tokens = prefixPart.split("=");
                if (tokens.length != 2) {
                    throw new RuntimeException("Malformed arguments in " + req);
                }
                if (args.put(tokens[0], tokens[1]) != null) {
                    throw new RuntimeException("Duplicate arguments in " + req);
                }
            }
            pattern = args.get("pattern");
            if (!Arrays.asList("ltl", "isolated", "ag_ef").contains(pattern)) {
                throw new RuntimeException("Unknown requirement pattern in " + req);
            }
            viewpoint = args.get("viewpoint");
            if (viewpoint == null) {
                throw new RuntimeException("Missing viewpoint in " + req);
            }
            viewpointUnit = faultFreeWorkspace.getUnitByName(viewpoint);
            singleFailures = loadSet(args, "single_failures");
            allFailures = loadSet(args, "all_failures");
            body = parts[1].strip();
            if (Stream.of("CTLSPEC", "LTLSPEC", "PSLSPEC", "SPEC").noneMatch(body::startsWith)) {
                throw new RuntimeException("Unknown kind of requirement: " + req);
            }
            System.out.println("Loaded: " + this);
        }

        void modelCheck() throws IOException {
            // 1. for each unit group, determine divisions where failures should be injected
            final List<List<Integer>> failingDivisions = new ArrayList<>();
            final Map<String, Integer> unitGroupToIndex = new HashMap<>();
            for (Map.Entry<String, UnitGroup> entry : unitGroups.entrySet()) {
                final String unitGroupName = entry.getKey();
                final UnitGroup unitGroup = entry.getValue();
                unitGroupToIndex.put(unitGroupName, unitGroupToIndex.size());
                final List<Integer> possibleFailingDivisions = new ArrayList<>();
                if (allFailures.contains(unitGroupName)) {
                    possibleFailingDivisions.add(Command.ALL_INDEX);
                } else if (singleFailures.contains(unitGroupName)) {
                    possibleFailingDivisions.addAll(IntStream.rangeClosed(1, unitGroup.divisions).boxed()
                            .collect(Collectors.toList()));
                } else {
                    possibleFailingDivisions.add(Command.NA_INDEX);
                }
                failingDivisions.add(possibleFailingDivisions);
            }
            System.out.println("Failing divisions in the order of unit groups: " + failingDivisions);

            // 2. find configurations to be model-checked
            final List<VerificationConfiguration> essentialConfigurations = new ArrayList<>() {{
                // fault-free configuration
                add(new VerificationConfiguration(viewpointUnit, 1,
                        Collections.nCopies(unitGroups.size(), Command.NA_INDEX)));
            }};
            if (pattern.equals("isolated")) {
                // for isolated verification, assume all possible failures and division 1
                // TODO exclude all other system parts
                essentialConfigurations.add(new VerificationConfiguration(viewpointUnit, 1,
                        Collections.nCopies(unitGroups.size(), Command.ALL_INDEX)));
            } else {
                // call Prolog analyzer
                essentialConfigurations.addAll(memoizedPrologAnalysis(faultFreeWorkspace, viewpoint, failingDivisions));
            }
            System.out.println("Configurations to be verified: " + essentialConfigurations);

            // 3. model-check fault-free + all essential configurations
            boolean faultFree = true;
            for (VerificationConfiguration conf : essentialConfigurations) {
                System.out.println("Processing: " + conf);
                final List<String> augmentedSubstitutions = getSubstitutionsString(substitutions,
                        "VANISHING_FAILURES=" + (pattern.equals("ag_ef") && !faultFree),
                        "REQ_FILENAME=" + DUMMY_REQ_FILENAME);
                int index = 0;
                for (Map.Entry<String, UnitGroup> entry : unitGroups.entrySet()) {
                    final String unitGroupName = entry.getKey();
                    final int division = conf.failingDivisions().get(index++);
                    augmentedSubstitutions.add(unitGroupName + "_FAULT_DIVISION="
                            + Workspace.configSafeIndex(division));
                }
                final String substString = String.join("; ", augmentedSubstitutions);
                final Workspace workspace = Main.createWorkspace(configFilename, substString, false,
                        false, printToConsole);
                if (optimizeOutUnreachable) {
                    workspace.coiOptimize(workspace.getUnitByName(viewpointUnit.toNuSMV()), conf.viewpointDivision());
                }
                final String nusmvModel = workspace.toNuSMV();
                if (printToConsole) {
                    System.out.println(nusmvModel);
                }
                final String effectiveBody = body.replaceAll("\\[\\[viewpoint_division]]", "_DIV"
                        + conf.viewpointDivision());
                try (PrintWriter pw = new PrintWriter(workspace.outputFilename())) {
                    pw.println(nusmvModel);
                    pw.println(effectiveBody);
                }

                // run NuSMV
                final File logFile = new File(faultFree ? logFilenameWithoutFailures : logFilenameWithFailures);
                try (PrintWriter pw = new PrintWriter(new FileOutputStream(logFile, true))) {
                    final String logStr = "-- " + effectiveBody + " with substitutions [" + substString +
                            "] and annotation [" + prefix + "]";
                    pw.println("--");
                    pw.println(logStr);
                    System.out.println(logStr);
                }
                final List<String> args = new ArrayList<>(Arrays.asList(nusmvCommand.split(" +")));
                args.add(workspace.outputFilename());
                System.out.println("Running: " + String.join(" ", args));
                // redirect output and error to log file
                final Process p = new ProcessBuilder().inheritIO().redirectErrorStream(true)
                        .redirectOutput(ProcessBuilder.Redirect.appendTo(logFile)).command(args).start();
                try {
                    p.waitFor();
                } catch (InterruptedException ignored) {
                }

                faultFree = false;
            }
            //new BufferedReader(new InputStreamReader(System.in)).readLine();
        }

        @Override
        public String toString() {
            return "AnnotatedRequirement{" +
                    "pattern='" + pattern + '\'' +
                    ", viewpoint='" + viewpoint + '\'' +
                    ", singleFailures=" + singleFailures +
                    ", allFailures=" + allFailures +
                    ", body='" + body + '\'' +
                    '}';
        }
    }

    private void launcher() throws IOException {
        System.out.println("Configuration file: " + configFilename);
        System.out.println("Requirements file: " + requirementsFilename);

        // 1. C preprocessing and requirement extraction
        final List<String> reqLines = new ArrayList<>();
        final Process cpp = new ProcessBuilder("cpp", requirementsFilename).start();
        try (BufferedReader input = new BufferedReader(new InputStreamReader(cpp.getInputStream()))) {
            String line;
            while ((line = input.readLine()) != null) {
                if (line.startsWith("[[REQ ")) {
                    reqLines.add(line);
                }
            }
        }

        // 2. load unit groups
        final List<String> faultFreeSubstitutions = getSubstitutionsString(substitutions,
                "VANISHING_FAILURES=false", "REQ_FILENAME=" + DUMMY_REQ_FILENAME);
        final Workspace incompleteWorkspace = Main.createWorkspace(configFilename,
                String.join("; ", faultFreeSubstitutions), false, true,
                printToConsole);
        unitGroups = incompleteWorkspace.unitGroups();
        System.out.println("Order of unit groups: " + unitGroups.keySet());

        // 3. create workspace without any failures
        for (Map.Entry<String, UnitGroup> entry : unitGroups.entrySet()) {
            faultFreeSubstitutions.add(entry.getKey() + "_FAULT_DIVISION=NA");
        }
        System.out.println("Fault-free substitutions: " + faultFreeSubstitutions);

        faultFreeWorkspace = Main.createWorkspace(configFilename,
                String.join("; ", faultFreeSubstitutions), false, false,
                printToConsole);

        // 4. check symmetry
        if (checkSymmetryNuSMVCommand != null) {
            faultFreeWorkspace.checkSymmetries(checkSymmetryNuSMVCommand);
        }

        // 5. clear log and dummy requirement files
        for (String filename : Arrays.asList(logFilenameWithFailures, logFilenameWithoutFailures, dummyReqPath())) {
            try (PrintWriter pw = new PrintWriter(filename)) {
                pw.print("");
            }
        }

        // 6. check requirements
        System.out.println(reqLines.size() + " requirements:");
        for (String req : reqLines) {
            this.new AnnotatedRequirement(req).modelCheck();
        }
    }
}
