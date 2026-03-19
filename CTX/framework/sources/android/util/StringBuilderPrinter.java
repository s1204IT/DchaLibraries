package android.util;

public class StringBuilderPrinter implements Printer {
    private final StringBuilder mBuilder;

    public StringBuilderPrinter(StringBuilder sb) {
        this.mBuilder = sb;
    }

    @Override
    public void println(String str) {
        this.mBuilder.append(str);
        int length = str.length();
        if (length <= 0 || str.charAt(length - 1) != '\n') {
            this.mBuilder.append('\n');
        }
    }
}
