package java.io;

import java.util.Formatter;
import libcore.io.Libcore;

public final class Console implements Flushable {
    private static final Object CONSOLE_LOCK = new Object();
    private static final Console console = makeConsole();
    private final ConsoleReader reader;
    private final PrintWriter writer;

    public static Console getConsole() {
        return console;
    }

    private static Console makeConsole() {
        if (!Libcore.os.isatty(FileDescriptor.in) || !Libcore.os.isatty(FileDescriptor.out)) {
            return null;
        }
        try {
            return new Console(System.in, System.out);
        } catch (UnsupportedEncodingException ex) {
            throw new AssertionError(ex);
        }
    }

    private Console(InputStream in, OutputStream out) throws UnsupportedEncodingException {
        this.reader = new ConsoleReader(in);
        this.writer = new ConsoleWriter(out);
    }

    @Override
    public void flush() {
        this.writer.flush();
    }

    public Console format(String format, Object... args) {
        Formatter f = new Formatter(this.writer);
        f.format(format, args);
        f.flush();
        return this;
    }

    public Console printf(String format, Object... args) {
        return format(format, args);
    }

    public Reader reader() {
        return this.reader;
    }

    public String readLine() {
        try {
            return this.reader.readLine();
        } catch (IOException e) {
            throw new IOError(e);
        }
    }

    public String readLine(String format, Object... args) {
        String line;
        synchronized (CONSOLE_LOCK) {
            format(format, args);
            line = readLine();
        }
        return line;
    }

    public char[] readPassword() {
        throw new UnsupportedOperationException();
    }

    public char[] readPassword(String format, Object... args) {
        throw new UnsupportedOperationException();
    }

    public PrintWriter writer() {
        return this.writer;
    }

    private static class ConsoleReader extends BufferedReader {
        public ConsoleReader(InputStream in) throws UnsupportedEncodingException {
            super(new InputStreamReader(in, System.getProperty("file.encoding")), 256);
            this.lock = Console.CONSOLE_LOCK;
        }

        @Override
        public void close() {
        }
    }

    private static class ConsoleWriter extends PrintWriter {
        public ConsoleWriter(OutputStream out) {
            super(out, true);
            this.lock = Console.CONSOLE_LOCK;
        }

        @Override
        public void close() {
            flush();
        }
    }
}
