package com.android.commands.uiautomator;

import android.os.Process;
import java.util.Arrays;

public class Launcher {
    private static Command HELP_COMMAND = new Command("help") {
        @Override
        public void run(String[] args) {
            System.err.println("Usage: uiautomator <subcommand> [options]\n");
            System.err.println("Available subcommands:\n");
            Command[] arr$ = Launcher.COMMANDS;
            for (Command command : arr$) {
                String shortHelp = command.shortHelp();
                String detailedOptions = command.detailedOptions();
                if (shortHelp == null) {
                    shortHelp = "";
                }
                if (detailedOptions == null) {
                    detailedOptions = "";
                }
                System.err.println(String.format("%s: %s", command.name(), shortHelp));
                System.err.println(detailedOptions);
            }
        }

        @Override
        public String detailedOptions() {
            return null;
        }

        @Override
        public String shortHelp() {
            return "displays help message";
        }
    };
    private static Command[] COMMANDS = {HELP_COMMAND, new RunTestCommand(), new DumpCommand(), new EventsCommand()};

    public static abstract class Command {
        private String mName;

        public abstract String detailedOptions();

        public abstract void run(String[] strArr);

        public abstract String shortHelp();

        public Command(String name) {
            this.mName = name;
        }

        public String name() {
            return this.mName;
        }
    }

    public static void main(String[] args) {
        Command command;
        Process.setArgV0("uiautomator");
        if (args.length >= 1 && (command = findCommand(args[0])) != null) {
            String[] args2 = new String[0];
            if (args.length > 1) {
                args2 = (String[]) Arrays.copyOfRange(args, 1, args.length);
            }
            command.run(args2);
            return;
        }
        HELP_COMMAND.run(args);
    }

    private static Command findCommand(String name) {
        Command[] arr$ = COMMANDS;
        for (Command command : arr$) {
            if (command.name().equals(name)) {
                return command;
            }
        }
        return null;
    }
}
