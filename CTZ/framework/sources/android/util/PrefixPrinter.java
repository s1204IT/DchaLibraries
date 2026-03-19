package android.util;

public class PrefixPrinter implements Printer {
    private final String mPrefix;
    private final Printer mPrinter;

    public static Printer create(Printer printer, String str) {
        if (str == null || str.equals("")) {
            return printer;
        }
        return new PrefixPrinter(printer, str);
    }

    private PrefixPrinter(Printer printer, String str) {
        this.mPrinter = printer;
        this.mPrefix = str;
    }

    @Override
    public void println(String str) {
        this.mPrinter.println(this.mPrefix + str);
    }
}
