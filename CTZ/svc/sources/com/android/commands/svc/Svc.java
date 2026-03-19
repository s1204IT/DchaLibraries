package com.android.commands.svc;

public class Svc {
    public static final Command COMMAND_HELP = new Command("help") {
        @Override
        public String shortHelp() {
            return "Show information about the subcommands";
        }

        @Override
        public String longHelp() {
            return shortHelp();
        }

        @Override
        public void run(String[] strArr) {
            Command commandLookupCommand;
            if (strArr.length == 2 && (commandLookupCommand = Svc.lookupCommand(strArr[1])) != null) {
                System.err.println(commandLookupCommand.longHelp());
                return;
            }
            System.err.println("Available commands:");
            int length = Svc.COMMANDS.length;
            int i = 0;
            for (int i2 = 0; i2 < length; i2++) {
                int length2 = Svc.COMMANDS[i2].name().length();
                if (i < length2) {
                    i = length2;
                }
            }
            String str = "    %-" + i + "s    %s";
            for (int i3 = 0; i3 < length; i3++) {
                Command command = Svc.COMMANDS[i3];
                System.err.println(String.format(str, command.name(), command.shortHelp()));
            }
        }
    };
    public static final Command[] COMMANDS = {COMMAND_HELP, new PowerCommand(), new DataCommand(), new WifiCommand(), new UsbCommand(), new NfcCommand(), new BluetoothCommand()};

    public static abstract class Command {
        private String mName;

        public abstract String longHelp();

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
        Command commandLookupCommand;
        if (strArr.length >= 1 && (commandLookupCommand = lookupCommand(strArr[0])) != null) {
            commandLookupCommand.run(strArr);
        } else {
            COMMAND_HELP.run(strArr);
        }
    }

    private static Command lookupCommand(String str) {
        int length = COMMANDS.length;
        for (int i = 0; i < length; i++) {
            Command command = COMMANDS[i];
            if (command.name().equals(str)) {
                return command;
            }
        }
        return null;
    }
}
