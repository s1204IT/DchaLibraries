package com.android.server.backup.transport;

import android.util.Log;
import android.util.Slog;
import com.android.internal.backup.IBackupTransport;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class TransportUtils {
    private static final String TAG = "TransportUtils";

    @Retention(RetentionPolicy.SOURCE)
    @interface Priority {
        public static final int DEBUG = 3;
        public static final int ERROR = 6;
        public static final int INFO = 4;
        public static final int VERBOSE = 2;
        public static final int WARN = 5;
        public static final int WTF = -1;
    }

    public static IBackupTransport checkTransportNotNull(IBackupTransport iBackupTransport) throws TransportNotAvailableException {
        if (iBackupTransport == null) {
            log(6, TAG, "Transport not available");
            throw new TransportNotAvailableException();
        }
        return iBackupTransport;
    }

    static void log(int i, String str, String str2) {
        if (i == -1) {
            Slog.wtf(str, str2);
        } else if (Log.isLoggable(str, i)) {
            Slog.println(i, str, str2);
        }
    }

    static String formatMessage(String str, String str2, String str3) {
        StringBuilder sb = new StringBuilder();
        if (str != null) {
            sb.append(str);
            sb.append(" ");
        }
        if (str2 != null) {
            sb.append("[");
            sb.append(str2);
            sb.append("] ");
        }
        sb.append(str3);
        return sb.toString();
    }

    private TransportUtils() {
    }
}
