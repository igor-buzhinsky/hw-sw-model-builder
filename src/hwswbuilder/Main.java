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

    private String substitutePlaceholders(String line) {
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
        final Matcher m = PLACEHOLDER.matcher(line);
        if (m.find()) {
            throw new RuntimeException("Unfilled placeholder " + m.group(0) + " in the configuration file," +
                    " use --configSubstitutions " + m.group(1) + "=<value> to assign it");
        }
        return line;
    }

    private void launcher() throws IOException {
        System.out.println("Processing " + configFilename + "...");
        final Workspace workspace = new Workspace(mimicMODCHK);
        final List<Command> commands = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(configFilename))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.replaceAll("#.*$", "");
                line = substitutePlaceholders(line).trim();
                if (line.isEmpty()) {
                    continue;
                }
                commands.add(Command.fromLine(line, workspace, configFilename));
            }
        }
        for (Command c : commands) {
            System.out.println("  " + c + "...");
            c.apply();
        }

        final String out = workspace.toNuSMV(configFilename);
        System.out.println(out);
        try (PrintWriter pw = new PrintWriter(new File(workspace.outputFilename()))) {
            pw.println(out);
        }
        if (checkSymmetry) {
            if (nusmvCommand == null) {
                throw new RuntimeException("--nusmvCommand not specified, thus cannot check symmetry");
            }
            workspace.checkSymmetries(configFilename, nusmvCommand);
        }
        if (nusmvCommand != null && !checkSymmetry) {
            final List<String> args = new ArrayList<>(Arrays.asList(nusmvCommand
                    .split(" +")));
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
