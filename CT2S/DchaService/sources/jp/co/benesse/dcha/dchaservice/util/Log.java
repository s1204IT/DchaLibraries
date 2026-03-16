package jp.co.benesse.dcha.dchaservice.util;

public class Log {
    static final int LOGLEVEL = LogLevel.NONE.getLevel();

    enum LogLevel {
        NONE(0),
        ERROR(1),
        INFO(2),
        DEBUG(3),
        VERBOSE(4);

        private int level;

        LogLevel(int num) {
            this.level = num;
        }

        public int getLevel() {
            return this.level;
        }
    }

    public static void e(String tag, String msg, Throwable e) {
        if (LOGLEVEL >= LogLevel.ERROR.getLevel()) {
            android.util.Log.e(tag, msg, e);
        }
    }

    public static void e(String tag, String msg) {
        if (LOGLEVEL >= LogLevel.ERROR.getLevel()) {
            android.util.Log.e(tag, msg);
        }
    }

    public static void d(String tag, String msg) {
        if (LOGLEVEL >= LogLevel.DEBUG.getLevel()) {
            android.util.Log.d(tag, msg);
        }
    }

    public static void v(String tag, String msg) {
        if (LOGLEVEL >= LogLevel.VERBOSE.getLevel()) {
            android.util.Log.v(tag, msg);
        }
    }
}
