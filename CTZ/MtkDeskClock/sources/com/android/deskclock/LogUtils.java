package com.android.deskclock;

import android.os.Build;
import android.util.Log;

public class LogUtils {
    private static final Logger DEFAULT_LOGGER = new Logger("AlarmClock");

    public static void v(String str, Object... objArr) {
        DEFAULT_LOGGER.v(str, objArr);
    }

    public static void d(String str, Object... objArr) {
        DEFAULT_LOGGER.d(str, objArr);
    }

    public static void i(String str, Object... objArr) {
        DEFAULT_LOGGER.i(str, objArr);
    }

    public static void w(String str, Object... objArr) {
        DEFAULT_LOGGER.w(str, objArr);
    }

    public static void e(String str, Object... objArr) {
        DEFAULT_LOGGER.e(str, objArr);
    }

    public static void e(String str, Throwable th) {
        DEFAULT_LOGGER.e(str, th);
    }

    public static void wtf(String str, Object... objArr) {
        DEFAULT_LOGGER.wtf(str, objArr);
    }

    public static void wtf(Throwable th) {
        DEFAULT_LOGGER.wtf(th);
    }

    public static final class Logger {
        public static final boolean DEBUG;
        public final String logTag;

        static {
            DEBUG = "eng".equals(Build.TYPE) || "userdebug".equals(Build.TYPE);
        }

        public Logger(String str) {
            this.logTag = str;
        }

        public boolean isVerboseLoggable() {
            return DEBUG || Log.isLoggable(this.logTag, 2);
        }

        public boolean isDebugLoggable() {
            return DEBUG || Log.isLoggable(this.logTag, 3);
        }

        public boolean isInfoLoggable() {
            return DEBUG || Log.isLoggable(this.logTag, 4);
        }

        public boolean isWarnLoggable() {
            return DEBUG || Log.isLoggable(this.logTag, 5);
        }

        public boolean isErrorLoggable() {
            return DEBUG || Log.isLoggable(this.logTag, 6);
        }

        public boolean isWtfLoggable() {
            return DEBUG || Log.isLoggable(this.logTag, 7);
        }

        public void v(String str, Object... objArr) {
            if (isVerboseLoggable()) {
                String str2 = this.logTag;
                if (objArr != null && objArr.length != 0) {
                    str = String.format(str, objArr);
                }
                Log.v(str2, str);
            }
        }

        public void d(String str, Object... objArr) {
            if (isDebugLoggable()) {
                String str2 = this.logTag;
                if (objArr != null && objArr.length != 0) {
                    str = String.format(str, objArr);
                }
                Log.d(str2, str);
            }
        }

        public void i(String str, Object... objArr) {
            if (isInfoLoggable()) {
                String str2 = this.logTag;
                if (objArr != null && objArr.length != 0) {
                    str = String.format(str, objArr);
                }
                Log.i(str2, str);
            }
        }

        public void w(String str, Object... objArr) {
            if (isWarnLoggable()) {
                String str2 = this.logTag;
                if (objArr != null && objArr.length != 0) {
                    str = String.format(str, objArr);
                }
                Log.w(str2, str);
            }
        }

        public void e(String str, Object... objArr) {
            if (isErrorLoggable()) {
                String str2 = this.logTag;
                if (objArr != null && objArr.length != 0) {
                    str = String.format(str, objArr);
                }
                Log.e(str2, str);
            }
        }

        public void e(String str, Throwable th) {
            if (isErrorLoggable()) {
                Log.e(this.logTag, str, th);
            }
        }

        public void wtf(String str, Object... objArr) {
            if (isWtfLoggable()) {
                String str2 = this.logTag;
                if (objArr != null && objArr.length != 0) {
                    str = String.format(str, objArr);
                }
                Log.wtf(str2, str);
            }
        }

        public void wtf(Throwable th) {
            if (isWtfLoggable()) {
                Log.wtf(this.logTag, th);
            }
        }
    }
}
