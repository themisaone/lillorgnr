package no.companyfetcher;

import no.companyfetcher.cli.CommandLineArgs;
import no.companyfetcher.cli.CommandLineArgs.Command;
import no.companyfetcher.config.Configuration;
import no.companyfetcher.gui.OrgNrGui;
import no.companyfetcher.output.ExcelMergeOptions;
import no.companyfetcher.tasks.ApplicationTasks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Launcher {

    private static final Logger log = LoggerFactory.getLogger(Launcher.class);

    public static void main(String[] args) {
        if (CommandLineArgs.isGuiMode(args)) {
            OrgNrGui.main(args);
            return;
        }

        log.info("START {}", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));

        Command command = CommandLineArgs.parseCommand(args);
        Configuration configuration = Configuration.load();

        String result = switch (command) {
            case FETCH -> ApplicationTasks.fetchAll(configuration);
            case PROF -> runMerge(configuration, args, ApplicationTasks::mergeProffToExcel);
            case AQUA -> runMerge(configuration, args, ApplicationTasks::mergeAquaToExcel);
            case MTB -> runMerge(configuration, args, ApplicationTasks::mergeFeeToExcel);
        };

        System.out.println(result.trim());
        log.info("FINISHED {}", command);
    }

    private static String runMerge(
            Configuration configuration,
            String[] args,
            MergeTask mergeTask
    ) {
        if (!CommandLineArgs.isMergeExcelMode(args)) {
            throw new IllegalArgumentException(commandRequiresMerge(args) + "\n" + CommandLineArgs.usage());
        }
        ExcelMergeOptions mergeOptions = CommandLineArgs.parseMergeOptions(args);
        return mergeTask.run(configuration, mergeOptions);
    }

    private static String commandRequiresMerge(String[] args) {
        Command command = CommandLineArgs.parseCommand(args);
        return command + " requires --merge-excel.";
    }

    @FunctionalInterface
    private interface MergeTask {
        String run(Configuration configuration, ExcelMergeOptions mergeOptions);
    }
}
