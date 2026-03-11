package jp.co.benesse.dcha.util;

import android.content.Context;

public final class Logger {
    private static final StringBuffer MSG_BUFFER = new StringBuffer();
    private static final StringBuffer META_BUFFER = new StringBuffer();

    private Logger() {
    }

    public static int e(Context context, Object... messages) {
        return 0;
    }

    public static int d(String packageName, Object... messages) {
        return 0;
    }

    public static int i(String packageName, Object... messages) {
        return 0;
    }

    public static int w(String packageName, Object... messages) {
        return 0;
    }

    public static int e(String packageName, Object... messages) {
        return 0;
    }
}
