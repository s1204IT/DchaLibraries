package com.android.phone.vvm;

import android.util.LocalLog;
import android.util.Log;
import com.android.internal.util.IndentingPrintWriter;
import java.io.FileDescriptor;
import java.io.PrintWriter;

public class VvmLog {
    private static final LocalLog sLocalLog = new LocalLog(100);

    public static void log(String str, String str2) {
        sLocalLog.log(str + ": " + str2);
    }

    public static void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        IndentingPrintWriter indentingPrintWriter = new IndentingPrintWriter(printWriter, "  ");
        indentingPrintWriter.increaseIndent();
        sLocalLog.dump(fileDescriptor, indentingPrintWriter, strArr);
        indentingPrintWriter.decreaseIndent();
    }

    public static int e(String str, String str2) {
        log(str, str2);
        return Log.e(str, str2);
    }

    public static int e(String str, String str2, Throwable th) {
        log(str, str2 + " " + th);
        return Log.e(str, str2, th);
    }

    public static int w(String str, String str2) {
        log(str, str2);
        return Log.w(str, str2);
    }

    public static int i(String str, String str2) {
        log(str, str2);
        return Log.i(str, str2);
    }

    public static int wtf(String str, String str2) {
        log(str, str2);
        return Log.wtf(str, str2);
    }
}
