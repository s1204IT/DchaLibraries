package android.util;

import java.io.PrintStream;

public class PrintStreamPrinter implements Printer {
    private final PrintStream mPS;

    public PrintStreamPrinter(PrintStream pw) {
        this.mPS = pw;
    }

    @Override
    public void println(String x) {
        this.mPS.println(x);
    }
}
