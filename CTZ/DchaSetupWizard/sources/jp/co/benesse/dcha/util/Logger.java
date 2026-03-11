package jp.co.benesse.dcha.util;

import android.content.Context;
import android.util.Log;

public final class Logger {
    public static final int ALL = 2;
    public static final int NONE = 7;
    private static final int OUTPUT_LEVEL = 7;
    private static final int TARGET_INDEX = 7;
    private static final StringBuffer MSG_BUFFER = new StringBuffer();
    private static final StringBuffer META_BUFFER = new StringBuffer();

    public static int d(Context context, Object... objArr) {
        return 0;
    }

    public static int d(String str, Object... objArr) {
        return 0;
    }

    public static int e(Context context, Object... objArr) {
        return 0;
    }

    public static int e(String str, Object... objArr) {
        return 0;
    }

    public static int i(Context context, Object... objArr) {
        return 0;
    }

    public static int i(String str, Object... objArr) {
        return 0;
    }

    public static final int println(int i, Context context, Object... objArr) {
        return 0;
    }

    public static final int println(int i, String str, Object... objArr) {
        return 0;
    }

    public static int v(Context context, Object... objArr) {
        return 0;
    }

    public static int v(String str, Object... objArr) {
        return 0;
    }

    public static int w(Context context, Object... objArr) {
        return 0;
    }

    public static int w(String str, Object... objArr) {
        return 0;
    }

    private static final int writeLog(int i, Context context, Object... objArr) {
        return write(i, context.getPackageName(), objArr);
    }

    private static final int writeLog(int i, String str, Object... objArr) {
        return write(i, str, objArr);
    }

    private static final synchronized int write(int i, String str, Object... objArr) {
        if (7 > i) {
            return 0;
        }
        int iPrintln = Log.println(i, str, getMessage(objArr));
        Object obj = objArr[objArr.length - 1];
        if (obj instanceof Throwable) {
            iPrintln += Log.println(6, str, Log.getStackTraceString((Throwable) obj));
        }
        return iPrintln;
    }

    private static final String getMessage(Object[] objArr) {
        MSG_BUFFER.setLength(0);
        MSG_BUFFER.append(getMetaInfo());
        for (Object obj : objArr) {
            if (!(obj instanceof Throwable)) {
                MSG_BUFFER.append(" ");
                MSG_BUFFER.append(obj);
            }
        }
        String string = MSG_BUFFER.toString();
        MSG_BUFFER.setLength(0);
        return string;
    }

    private static final String getMetaInfo() {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        int length = stackTrace.length;
        return getMetaInfo(stackTrace[length <= 7 ? length - 1 : 7]);
    }

    private static final String getMetaInfo(StackTraceElement stackTraceElement) {
        String className = stackTraceElement.getClassName();
        String strSubstring = className.substring(className.lastIndexOf(".") + 1);
        String methodName = stackTraceElement.getMethodName();
        int lineNumber = stackTraceElement.getLineNumber();
        META_BUFFER.setLength(0);
        META_BUFFER.append("[");
        META_BUFFER.append(strSubstring);
        META_BUFFER.append("#");
        META_BUFFER.append(methodName);
        META_BUFFER.append(":");
        META_BUFFER.append(lineNumber);
        META_BUFFER.append("]");
        return META_BUFFER.toString();
    }
}
