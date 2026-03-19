package android.util;

import android.os.DeadSystemException;
import com.android.internal.os.RuntimeInit;
import com.android.internal.util.FastPrintWriter;
import com.android.internal.util.LineBreakBufferedWriter;
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
        public void onTerribleFailure(String str, TerribleFailure terribleFailure, boolean z) {
            RuntimeInit.wtf(str, terribleFailure, z);
        }
    };

    public interface TerribleFailureHandler {
        void onTerribleFailure(String str, TerribleFailure terribleFailure, boolean z);
    }

    public static native boolean isLoggable(String str, int i);

    private static native int logger_entry_max_payload_native();

    public static native int println_native(int i, int i2, String str, String str2);

    public static class TerribleFailure extends Exception {
        TerribleFailure(String str, Throwable th) {
            super(str, th);
        }
    }

    private Log() {
    }

    public static int v(String str, String str2) {
        return println_native(0, 2, str, str2);
    }

    public static int v(String str, String str2, Throwable th) {
        return printlns(0, 2, str, str2, th);
    }

    public static int d(String str, String str2) {
        return println_native(0, 3, str, str2);
    }

    public static int d(String str, String str2, Throwable th) {
        return printlns(0, 3, str, str2, th);
    }

    public static int i(String str, String str2) {
        return println_native(0, 4, str, str2);
    }

    public static int i(String str, String str2, Throwable th) {
        return printlns(0, 4, str, str2, th);
    }

    public static int w(String str, String str2) {
        return println_native(0, 5, str, str2);
    }

    public static int w(String str, String str2, Throwable th) {
        return printlns(0, 5, str, str2, th);
    }

    public static int w(String str, Throwable th) {
        return printlns(0, 5, str, "", th);
    }

    public static int e(String str, String str2) {
        return println_native(0, 6, str, str2);
    }

    public static int e(String str, String str2, Throwable th) {
        return printlns(0, 6, str, str2, th);
    }

    public static int wtf(String str, String str2) {
        return wtf(0, str, str2, null, false, false);
    }

    public static int wtfStack(String str, String str2) {
        return wtf(0, str, str2, null, true, false);
    }

    public static int wtf(String str, Throwable th) {
        return wtf(0, str, th.getMessage(), th, false, false);
    }

    public static int wtf(String str, String str2, Throwable th) {
        return wtf(0, str, str2, th, false, false);
    }

    static int wtf(int i, String str, String str2, Throwable th, boolean z, boolean z2) {
        TerribleFailure terribleFailure = new TerribleFailure(str2, th);
        if (z) {
            th = terribleFailure;
        }
        int iPrintlns = printlns(i, 6, str, str2, th);
        sWtfHandler.onTerribleFailure(str, terribleFailure, z2);
        return iPrintlns;
    }

    static void wtfQuiet(int i, String str, String str2, boolean z) {
        sWtfHandler.onTerribleFailure(str, new TerribleFailure(str2, null), z);
    }

    public static TerribleFailureHandler setWtfHandler(TerribleFailureHandler terribleFailureHandler) {
        if (terribleFailureHandler == null) {
            throw new NullPointerException("handler == null");
        }
        TerribleFailureHandler terribleFailureHandler2 = sWtfHandler;
        sWtfHandler = terribleFailureHandler;
        return terribleFailureHandler2;
    }

    public static String getStackTraceString(Throwable th) {
        if (th == null) {
            return "";
        }
        for (Throwable cause = th; cause != null; cause = cause.getCause()) {
            if (cause instanceof UnknownHostException) {
                return "";
            }
        }
        StringWriter stringWriter = new StringWriter();
        FastPrintWriter fastPrintWriter = new FastPrintWriter((Writer) stringWriter, false, 256);
        th.printStackTrace(fastPrintWriter);
        fastPrintWriter.flush();
        return stringWriter.toString();
    }

    public static int println(int i, String str, String str2) {
        return println_native(0, i, str, str2);
    }

    public static int printlns(int i, int i2, String str, String str2, Throwable th) {
        ImmediateLogWriter immediateLogWriter = new ImmediateLogWriter(i, i2, str);
        LineBreakBufferedWriter lineBreakBufferedWriter = new LineBreakBufferedWriter(immediateLogWriter, Math.max(((PreloadHolder.LOGGER_ENTRY_MAX_PAYLOAD - 2) - (str != null ? str.length() : 0)) - 32, 100));
        lineBreakBufferedWriter.println(str2);
        if (th != null) {
            Throwable cause = th;
            while (true) {
                if (cause == null || (cause instanceof UnknownHostException)) {
                    break;
                }
                if (cause instanceof DeadSystemException) {
                    lineBreakBufferedWriter.println("DeadSystemException: The system died; earlier logs will point to the root cause");
                    break;
                }
                cause = cause.getCause();
            }
            if (cause == null) {
                th.printStackTrace(lineBreakBufferedWriter);
            }
        }
        lineBreakBufferedWriter.flush();
        return immediateLogWriter.getWritten();
    }

    static class PreloadHolder {
        public static final int LOGGER_ENTRY_MAX_PAYLOAD = Log.logger_entry_max_payload_native();

        PreloadHolder() {
        }
    }

    private static class ImmediateLogWriter extends Writer {
        private int bufID;
        private int priority;
        private String tag;
        private int written = 0;

        public ImmediateLogWriter(int i, int i2, String str) {
            this.bufID = i;
            this.priority = i2;
            this.tag = str;
        }

        public int getWritten() {
            return this.written;
        }

        @Override
        public void write(char[] cArr, int i, int i2) {
            this.written += Log.println_native(this.bufID, this.priority, this.tag, new String(cArr, i, i2));
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() {
        }
    }
}
