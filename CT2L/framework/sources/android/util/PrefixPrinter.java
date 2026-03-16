package android.util;

import android.net.ProxyInfo;

public class PrefixPrinter implements Printer {
    private final String mPrefix;
    private final Printer mPrinter;

    public static Printer create(Printer printer, String prefix) {
        return (prefix == null || prefix.equals(ProxyInfo.LOCAL_EXCL_LIST)) ? printer : new PrefixPrinter(printer, prefix);
    }

    private PrefixPrinter(Printer printer, String prefix) {
        this.mPrinter = printer;
        this.mPrefix = prefix;
    }

    @Override
    public void println(String str) {
        this.mPrinter.println(this.mPrefix + str);
    }
}
