package com.android.commands.monkey;

import android.util.Log;

public abstract class Logger {
    private static final String TAG = "Monkey";
    public static Logger out = new Logger() {
        @Override
        public void println(String str) {
            if (stdout) {
                System.out.println(str);
            }
            if (logcat) {
                Log.i(Logger.TAG, str);
            }
        }
    };
    public static Logger err = new Logger() {
        @Override
        public void println(String str) {
            if (stdout) {
                System.err.println(str);
            }
            if (logcat) {
                Log.w(Logger.TAG, str);
            }
        }
    };
    public static boolean stdout = true;
    public static boolean logcat = true;

    public abstract void println(String str);

    public static void error(String str, Throwable th) {
        err.println(str);
        err.println(Log.getStackTraceString(th));
    }
}
