package android.util;

import java.io.PrintWriter;

public class PrintWriterPrinter implements Printer {
    private final PrintWriter mPW;

    public PrintWriterPrinter(PrintWriter printWriter) {
        this.mPW = printWriter;
    }

    @Override
    public void println(String str) {
        this.mPW.println(str);
    }
}
