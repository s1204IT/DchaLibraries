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
                        try {
                            dump(printWriter);
                            printWriter.close();
                            String str4 = "Log written to " + file.getAbsolutePath();
                            printWriter.close();
                            str3 = str4;
                            printWriter2 = printWriter;
                        } catch (IOException e2) {
                            e = e2;
                            Log.e("FalsingLog", "Unable to write falsing log", e);
                            printWriter2 = printWriter;
                            if (printWriter != null) {
                                printWriter.close();
                                printWriter2 = printWriter;
                            }
                        }
                    } catch (Throwable th2) {
                        th = th2;
                        if (printWriter2 != 0) {
                            printWriter2.close();
                        }
                        throw th;
                    }
                } catch (IOException e3) {
                    printWriter = null;
                    e = e3;
                } catch (Throwable th3) {
                    th = th3;
                    printWriter2 = 0;
                    if (printWriter2 != 0) {
                    }
                    throw th;
                }
            }
            Log.e("FalsingLog", str + " " + str2 + "; " + str3);
        }
    }
}
