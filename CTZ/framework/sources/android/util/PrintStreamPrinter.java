package android.util;

import java.io.PrintStream;

public class PrintStreamPrinter implements Printer {
    private final PrintStream mPS;

    public PrintStreamPrinter(PrintStream printStream) {
        this.mPS = printStream;
    }

    @Override
    public void println(String str) {
        this.mPS.println(str);
    }
}
