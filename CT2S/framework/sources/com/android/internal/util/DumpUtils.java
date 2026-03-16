package com.android.internal.util;

import android.os.Handler;
import java.io.PrintWriter;
import java.io.StringWriter;

public final class DumpUtils {

    public interface Dump {
        void dump(PrintWriter printWriter);
    }

    private DumpUtils() {
    }

    public static void dumpAsync(Handler handler, final Dump dump, PrintWriter pw, long timeout) {
        final StringWriter sw = new StringWriter();
        if (handler.runWithScissors(new Runnable() {
            @Override
            public void run() {
                PrintWriter lpw = new FastPrintWriter(sw);
                dump.dump(lpw);
                lpw.close();
            }
        }, timeout)) {
            pw.print(sw.toString());
        } else {
            pw.println("... timed out");
        }
    }
}
