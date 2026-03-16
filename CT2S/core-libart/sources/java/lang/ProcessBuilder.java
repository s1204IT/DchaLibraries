package java.lang;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

public final class ProcessBuilder {
    private List<String> command;
    private File directory;
    private Map<String, String> environment;
    private boolean redirectErrorStream;

    public ProcessBuilder(String... command) {
        this(new ArrayList(Arrays.asList(command)));
    }

    public ProcessBuilder(List<String> command) {
        if (command == null) {
            throw new NullPointerException("command == null");
        }
        this.command = command;
        this.environment = new Hashtable(System.getenv());
    }

    public List<String> command() {
        return this.command;
    }

    public ProcessBuilder command(String... command) {
        return command(new ArrayList(Arrays.asList(command)));
    }

    public ProcessBuilder command(List<String> command) {
        if (command == null) {
            throw new NullPointerException("command == null");
        }
        this.command = command;
        return this;
    }

    public File directory() {
        return this.directory;
    }

    public ProcessBuilder directory(File directory) {
        this.directory = directory;
        return this;
    }

    public Map<String, String> environment() {
        return this.environment;
    }

    public boolean redirectErrorStream() {
        return this.redirectErrorStream;
    }

    public ProcessBuilder redirectErrorStream(boolean redirectErrorStream) {
        this.redirectErrorStream = redirectErrorStream;
        return this;
    }

    public Process start() throws IOException {
        String[] cmdArray = (String[]) this.command.toArray(new String[this.command.size()]);
        String[] envArray = new String[this.environment.size()];
        int i = 0;
        for (Map.Entry<String, String> entry : this.environment.entrySet()) {
            envArray[i] = entry.getKey() + "=" + entry.getValue();
            i++;
        }
        return ProcessManager.getInstance().exec(cmdArray, envArray, this.directory, this.redirectErrorStream);
    }
}
