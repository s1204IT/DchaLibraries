package com.android.commands.uiautomator;

import android.os.Process;
import java.util.Arrays;

public class Launcher {
    private static Command HELP_COMMAND = new Command("help") {
        @Override
        public void run(String[] strArr) {
            System.err.println("Usage: uiautomator <subcommand> [options]\n");
            System.err.println("Available subcommands:\n");
            for (Command command : Launcher.COMMANDS) {
                String strShortHelp = command.shortHelp();
                String strDetailedOptions = command.detailedOptions();
                if (strShortHelp == null) {
                    strShortHelp = "";
                }
                if (strDetailedOptions == null) {
                    strDetailedOptions = "";
                }
                System.err.println(String.format("%s: %s", command.name(), strShortHelp));
                System.err.println(strDetailedOptions);
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

        public Command(String str) {
            this.mName = str;
        }

        public String name() {
            return this.mName;
        }
    }

    public static void main(String[] strArr) {
        Command commandFindCommand;
        Process.setArgV0("uiautomator");
        if (strArr.length >= 1 && (commandFindCommand = findCommand(strArr[0])) != null) {
            String[] strArr2 = new String[0];
            if (strArr.length > 1) {
                strArr2 = (String[]) Arrays.copyOfRange(strArr, 1, strArr.length);
            }
            commandFindCommand.run(strArr2);
            return;
        }
        HELP_COMMAND.run(strArr);
    }

    private static Command findCommand(String str) {
        for (Command command : COMMANDS) {
            if (command.name().equals(str)) {
                return command;
            }
        }
        return null;
    }
}
