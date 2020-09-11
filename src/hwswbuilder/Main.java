package hwswbuilder;

import hwswbuilder.command.Command;
import hwswbuilder.command.Workspace;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.spi.BooleanOptionHandler;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {
    @Argument(usage = "configuration file", metaVar = "files", required = true)
    private String configFilename;

    @Option(name = "--nusmvCommand", aliases = { "-c" },
            usage = "will run <command> <output SMV file> in the end",
            metaVar = "<command>")
    private String nusmvCommand;

    @Option(name = "--configSubstitutions", aliases = { "-s" },
            usage = "fill in placeholders in the config file, e.g. V1={3,3};file=x.smv will substitute" +
                    " ${V1} with {3,3} and ${file} with x.smv",
            metaVar = "<substitutions>")
    private String substitutions;

    @Option(name = "--mimicMODCHK", handler = BooleanOptionHandler.class,
            usage = "produce models that look closer to the ones which can be developed in MODCHK " +
                    "(may require some custom basic blocks)")
    private boolean mimicMODCHK;

    @Option(name = "--checkSymmetry", handler = BooleanOptionHandler.class,
            usage = "perform symmetry checks in addition to generating the model; " +
                    "NuSMV will be run as in --nusmvCommand")
    private boolean checkSymmetry;

    @Option(name = "--printToConsole", handler = BooleanOptionHandler.class,
            usage = "print generated files and more to console")
    private boolean printToConsole;

    @Option(name = "--prologFilename", metaVar = "filename",
            usage = "generate a Prolog representation of a configuration")
    private String prologFilename;

    @Option(name = "--prologExecutable", metaVar = "command",
            usage = "path to Prolog executable (default: prolog)")
    private String prologExecutable = "prolog";

    @Option(name = "--generateDominationGraph",
            usage = "generate a graph for the specified module")
    private String generateDominationGraph;

    @Option(name = "--prologThoroughQueries", handler = BooleanOptionHandler.class,
            usage = "make queries even if their results may be inferred based on transitivity or reflexivity")
    private boolean prologThoroughQueries;

    @Option(name = "--prologIncludeNoFailures", handler = BooleanOptionHandler.class,
            usage = "consider configurations with completely correct subsystems")
    private boolean prologIncludeNoFailures;

    @Option(name = "--prologIncludeCCF", handler = BooleanOptionHandler.class,
            usage = "consider also failures in all division of each non-inspected unit group")
    private boolean prologIncludeCCF;

    public static void main(String[] args) {
        new Main().run(args);
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
            System.out.print("Usage: java -jar hw_sw_builder.jar ");
            parser.printSingleLineUsage(System.out);
            System.out.println();
            parser.printUsage(System.out);
            return false;
        }
    }

    private static final Pattern PLACEHOLDER = Pattern.compile("\\$\\{(\\w+)}");

    static String substitutePlaceholders(String line, String substitutions, boolean replaceUnfilledWithNA) {
        if (substitutions != null) {
            final String[] replacements = substitutions.split("; *");
            for (String replacement : replacements) {
                final String[] tokens = replacement.split("=");
                if (tokens.length != 2) {
                    throw new RuntimeException("In --configSubstitutions, all ;-separated replacements" +
                            " must have form x=y");
                }
                line = line.replace("${" + tokens[0] + "}", tokens[1]);
            }
        }
        if (replaceUnfilledWithNA) {
            line = line.replaceAll(PLACEHOLDER.pattern(), "NA");
        } else {
            final Matcher m = PLACEHOLDER.matcher(line);
            if (m.find()) {
                throw new RuntimeException("Unfilled placeholder " + m.group(0) + " in the configuration file," +
                        " use --configSubstitutions " + m.group(1) + "=<value> to assign it");
            }
        }
        return line;
    }

    static Workspace createWorkspace(String configFilename, String substitutions, boolean mimicMODCHK,
                                     boolean loadUnitGroupsOnly, boolean printCommands) throws IOException {
        final Workspace workspace = new Workspace(configFilename, mimicMODCHK);
        final List<Command> commands = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(configFilename))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.replaceAll("#.*$", "");
                line = substitutePlaceholders(line, substitutions, loadUnitGroupsOnly).trim();
                if (line.isEmpty()) {
                    continue;
                }
                commands.add(Command.fromLine(line, workspace, configFilename, loadUnitGroupsOnly));
            }
        }
        for (Command c : commands) {
            if (printCommands) {
                System.out.println("  " + c + "...");
            }
            c.apply();
        }
        return workspace;
    }

    private void launcher() throws IOException {
        System.out.println("Processing " + configFilename + "...");
        final Workspace workspace = createWorkspace(configFilename, substitutions, mimicMODCHK, false,
                printToConsole);

        if (generateDominationGraph != null && prologFilename == null) {
            throw new RuntimeException("--generateDominationGraph is requested but --prologFilename is not specified");
        }

        if (prologFilename != null) {
            final String out = workspace.toProlog(prologIncludeNoFailures, prologIncludeCCF);
            if (printToConsole) {
                System.out.println(out);
            }
            try (PrintWriter pw = new PrintWriter(new File(prologFilename))) {
                pw.println(out);
            }
            if (generateDominationGraph != null) {
                workspace.queryProlog(prologFilename, prologExecutable, generateDominationGraph,
                        prologThoroughQueries, prologIncludeNoFailures, prologIncludeCCF);
            }
        }

        final String out = workspace.toNuSMV();
        if (printToConsole) {
            System.out.println(out);
        }
        try (PrintWriter pw = new PrintWriter(workspace.outputFilename())) {
            pw.println(out);
        }
        if (checkSymmetry) {
            if (nusmvCommand == null) {
                throw new RuntimeException("--nusmvCommand not specified, thus cannot check symmetry");
            }
            workspace.checkSymmetries(nusmvCommand);
        }
        if (nusmvCommand != null && !checkSymmetry) {
            final List<String> args = new ArrayList<>(Arrays.asList(nusmvCommand.split(" +")));
            args.add(workspace.outputFilename());
            System.out.println("Running: " + String.join(" ", args));
            final Process p = new ProcessBuilder().inheritIO().command(args).start();
            try {
                p.waitFor();
            } catch (InterruptedException ignored) {
            }
        }
    }
}
