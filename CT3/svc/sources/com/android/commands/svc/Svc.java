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
        public void run(String[] args) {
            Command c;
            if (args.length == 2 && (c = Svc.lookupCommand(args[1])) != null) {
                System.err.println(c.longHelp());
                return;
            }
            System.err.println("Available commands:");
            int N = Svc.COMMANDS.length;
            int maxlen = 0;
            for (int i = 0; i < N; i++) {
                int len = Svc.COMMANDS[i].name().length();
                if (maxlen < len) {
                    maxlen = len;
                }
            }
            String format = "    %-" + maxlen + "s    %s";
            for (int i2 = 0; i2 < N; i2++) {
                Command c2 = Svc.COMMANDS[i2];
                System.err.println(String.format(format, c2.name(), c2.shortHelp()));
            }
        }
    };
    public static final Command[] COMMANDS = {COMMAND_HELP, new PowerCommand(), new DataCommand(), new WifiCommand(), new UsbCommand(), new NfcCommand()};

    public static abstract class Command {
        private String mName;

        public abstract String longHelp();

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
        Command c;
        if (args.length >= 1 && (c = lookupCommand(args[0])) != null) {
            c.run(args);
        } else {
            COMMAND_HELP.run(args);
        }
    }

    private static Command lookupCommand(String name) {
        int N = COMMANDS.length;
        for (int i = 0; i < N; i++) {
            Command c = COMMANDS[i];
            if (c.name().equals(name)) {
                return c;
            }
        }
        return null;
    }
}
