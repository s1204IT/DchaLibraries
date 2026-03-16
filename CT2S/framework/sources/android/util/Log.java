package android.util;

import android.net.ProxyInfo;
import com.android.internal.os.RuntimeInit;
import com.android.internal.util.FastPrintWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.net.UnknownHostException;

public final class Log {
    public static final int ASSERT = 7;
    public static final int DEBUG = 3;
    public static final int ERROR = 6;
    public static final int INFO = 4;
    public static final int LOG_ID_CRASH = 4;
    public static final int LOG_ID_EVENTS = 2;
    public static final int LOG_ID_MAIN = 0;
    public static final int LOG_ID_RADIO = 1;
    public static final int LOG_ID_SYSTEM = 3;
    public static final int VERBOSE = 2;
    public static final int WARN = 5;
    private static TerribleFailureHandler sWtfHandler = new TerribleFailureHandler() {
        @Override
        public void onTerribleFailure(String tag, TerribleFailure what, boolean system) {
            RuntimeInit.wtf(tag, what, system);
        }
    };

    public interface TerribleFailureHandler {
        void onTerribleFailure(String str, TerribleFailure terribleFailure, boolean z);
    }

    public static native boolean isLoggable(String str, int i);

    public static native int printk_native(String str, String str2);

    public static native int println_native(int i, int i2, String str, String str2);

    private static class TerribleFailure extends Exception {
        TerribleFailure(String msg, Throwable cause) {
            super(msg, cause);
        }
    }

    private Log() {
    }

    public static int v(String tag, String msg) {
        return println_native(0, 2, tag, msg);
    }

    public static int v(String tag, String msg, Throwable tr) {
        return println_native(0, 2, tag, msg + '\n' + getStackTraceString(tr));
    }

    public static int d(String tag, String msg) {
        return println_native(0, 3, tag, msg);
    }

    public static int d(String tag, String msg, Throwable tr) {
        return println_native(0, 3, tag, msg + '\n' + getStackTraceString(tr));
    }

    public static int i(String tag, String msg) {
        return println_native(0, 4, tag, msg);
    }

    public static int i(String tag, String msg, Throwable tr) {
        return println_native(0, 4, tag, msg + '\n' + getStackTraceString(tr));
    }

    public static int w(String tag, String msg) {
        return println_native(0, 5, tag, msg);
    }

    public static int w(String tag, String msg, Throwable tr) {
        return println_native(0, 5, tag, msg + '\n' + getStackTraceString(tr));
    }

    public static int k(String tag, String msg) {
        printk_native(tag, msg);
        return println_native(0, 5, tag, msg);
    }

    public static int k(String tag, String msg, Throwable tr) {
        printk_native(tag, msg);
        return println_native(0, 5, tag, msg + '\n' + getStackTraceString(tr));
    }

    public static int w(String tag, Throwable tr) {
        return println_native(0, 5, tag, getStackTraceString(tr));
    }

    public static int e(String tag, String msg) {
        return println_native(0, 6, tag, msg);
    }

    public static int e(String tag, String msg, Throwable tr) {
        return println_native(0, 6, tag, msg + '\n' + getStackTraceString(tr));
    }

    public static int wtf(String tag, String msg) {
        return wtf(0, tag, msg, null, false, false);
    }

    public static int wtfStack(String tag, String msg) {
        return wtf(0, tag, msg, null, true, false);
    }

    public static int wtf(String tag, Throwable tr) {
        return wtf(0, tag, tr.getMessage(), tr, false, false);
    }

    public static int wtf(String tag, String msg, Throwable tr) {
        return wtf(0, tag, msg, tr, false, false);
    }

    static int wtf(int logId, String tag, String msg, Throwable tr, boolean localStack, boolean system) {
        TerribleFailure what = new TerribleFailure(msg, tr);
        StringBuilder sbAppend = new StringBuilder().append(msg).append('\n');
        if (localStack) {
            tr = what;
        }
        int bytes = println_native(logId, 7, tag, sbAppend.append(getStackTraceString(tr)).toString());
        sWtfHandler.onTerribleFailure(tag, what, system);
        return bytes;
    }

    static void wtfQuiet(int logId, String tag, String msg, boolean system) {
        TerribleFailure what = new TerribleFailure(msg, null);
        sWtfHandler.onTerribleFailure(tag, what, system);
    }

    public static TerribleFailureHandler setWtfHandler(TerribleFailureHandler handler) {
        if (handler == null) {
            throw new NullPointerException("handler == null");
        }
        TerribleFailureHandler oldHandler = sWtfHandler;
        sWtfHandler = handler;
        return oldHandler;
    }

    public static String getStackTraceString(Throwable tr) {
        if (tr == null) {
            return ProxyInfo.LOCAL_EXCL_LIST;
        }
        for (Throwable t = tr; t != null; t = t.getCause()) {
            if (t instanceof UnknownHostException) {
                return ProxyInfo.LOCAL_EXCL_LIST;
            }
        }
        StringWriter sw = new StringWriter();
        PrintWriter pw = new FastPrintWriter((Writer) sw, false, 256);
        tr.printStackTrace(pw);
        pw.flush();
        return sw.toString();
    }

    public static int println(int priority, String tag, String msg) {
        return println_native(0, priority, tag, msg);
    }
}
