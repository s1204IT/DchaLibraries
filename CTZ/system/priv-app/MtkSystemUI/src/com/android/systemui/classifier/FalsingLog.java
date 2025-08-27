package com.android.systemui.classifier;

import android.app.ActivityThread;
import android.app.Application;
import android.os.Build;
import android.os.SystemProperties;
import android.util.Log;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;

/* loaded from: classes.dex */
public class FalsingLog {
    public static final boolean ENABLED = SystemProperties.getBoolean("debug.falsing_log", Build.IS_DEBUGGABLE);
    private static final boolean LOGCAT = SystemProperties.getBoolean("debug.falsing_logcat", false);
    private static final int MAX_SIZE = SystemProperties.getInt("debug.falsing_log_size", 100);
    private static FalsingLog sInstance;
    private final ArrayDeque<String> mLog = new ArrayDeque<>(MAX_SIZE);
    private final SimpleDateFormat mFormat = new SimpleDateFormat("MM-dd HH:mm:ss", Locale.US);

    private FalsingLog() {
    }

    public static void i(String str, String str2) {
        if (LOGCAT) {
            Log.i("FalsingLog", str + "\t" + str2);
        }
        log("I", str, str2);
    }

    public static void wLogcat(String str, String str2) {
        Log.w("FalsingLog", str + "\t" + str2);
        log("W", str, str2);
    }

    public static void e(String str, String str2) {
        if (LOGCAT) {
            Log.e("FalsingLog", str + "\t" + str2);
        }
        log("E", str, str2);
    }

    public static synchronized void log(String str, String str2, String str3) {
        if (ENABLED) {
            if (sInstance == null) {
                sInstance = new FalsingLog();
            }
            if (sInstance.mLog.size() >= MAX_SIZE) {
                sInstance.mLog.removeFirst();
            }
            sInstance.mLog.add(sInstance.mFormat.format(new Date()) + " " + str + " " + str2 + " " + str3);
        }
    }

    public static synchronized void dump(PrintWriter printWriter) {
        printWriter.println("FALSING LOG:");
        if (!ENABLED) {
            printWriter.println("Disabled, to enable: setprop debug.falsing_log 1");
            printWriter.println();
            return;
        }
        if (sInstance != null && !sInstance.mLog.isEmpty()) {
            Iterator<String> it = sInstance.mLog.iterator();
            while (it.hasNext()) {
                printWriter.println(it.next());
            }
            printWriter.println();
            return;
        }
        printWriter.println("<empty>");
        printWriter.println();
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [161=4] */
    /* JADX DEBUG: Failed to insert an additional move for type inference into block B:17:0x006e */
    /* JADX DEBUG: Failed to insert an additional move for type inference into block B:27:0x0083 */
    /* JADX DEBUG: Failed to insert an additional move for type inference into block B:38:0x0048 */
    /* JADX DEBUG: Multi-variable search result rejected for r3v1, resolved type: java.lang.String */
    /* JADX DEBUG: Multi-variable search result rejected for r3v2, resolved type: java.io.PrintWriter */
    /* JADX DEBUG: Multi-variable search result rejected for r3v4, resolved type: java.io.PrintWriter */
    /* JADX DEBUG: Multi-variable search result rejected for r3v5, resolved type: java.io.PrintWriter */
    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Removed duplicated region for block: B:29:0x0086 A[Catch: all -> 0x00b4, TryCatch #2 {, blocks: (B:4:0x0003, B:8:0x0009, B:11:0x0018, B:15:0x0069, B:32:0x0091, B:25:0x007f, B:29:0x0086, B:30:0x0089, B:31:0x008a), top: B:40:0x0003 }] */
    /* JADX WARN: Type inference failed for: r3v10 */
    /* JADX WARN: Type inference failed for: r3v11 */
    /* JADX WARN: Type inference failed for: r3v7 */
    /* JADX WARN: Type inference failed for: r3v9 */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public static synchronized void wtf(String str, String str2, Throwable th) {
        PrintWriter printWriter;
        IOException e;
        if (ENABLED) {
            e(str, str2);
            Application applicationCurrentApplication = ActivityThread.currentApplication();
            String str3 = "";
            if (!Build.IS_DEBUGGABLE || applicationCurrentApplication == null) {
                Log.e("FalsingLog", "Unable to write log, build must be debuggable.");
            } else {
                File dataDir = applicationCurrentApplication.getDataDir();
                PrintWriter printWriter2 = "falsing-" + new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(new Date()) + ".txt";
                File file = new File(dataDir, printWriter2);
                try {
                    try {
                        printWriter = new PrintWriter(file);
                    } catch (IOException e2) {
                        printWriter = null;
                        e = e2;
                    } catch (Throwable th2) {
                        th = th2;
                        printWriter2 = 0;
                        if (printWriter2 != 0) {
                        }
                        throw th;
                    }
                    try {
                        dump(printWriter);
                        printWriter.close();
                        String str4 = "Log written to " + file.getAbsolutePath();
                        printWriter.close();
                        str3 = str4;
                        printWriter2 = printWriter;
                    } catch (IOException e3) {
                        e = e3;
                        Log.e("FalsingLog", "Unable to write falsing log", e);
                        printWriter2 = printWriter;
                        if (printWriter != null) {
                            printWriter.close();
                            printWriter2 = printWriter;
                        }
                        Log.e("FalsingLog", str + " " + str2 + "; " + str3);
                    }
                } catch (Throwable th3) {
                    th = th3;
                    if (printWriter2 != 0) {
                        printWriter2.close();
                    }
                    throw th;
                }
            }
            Log.e("FalsingLog", str + " " + str2 + "; " + str3);
        }
    }
}
