package com.android.server.hdmi;

import android.os.SystemClock;
import android.util.Log;
import android.util.Pair;
import android.util.Slog;
import com.android.server.backup.BackupManagerConstants;
import java.util.HashMap;

final class HdmiLogger {
    private static final long ERROR_LOG_DURATTION_MILLIS = 20000;
    private static final String TAG = "HDMI";
    private static final boolean DEBUG = Log.isLoggable(TAG, 3);
    private static final ThreadLocal<HdmiLogger> sLogger = new ThreadLocal<>();
    private final HashMap<String, Pair<Long, Integer>> mWarningTimingCache = new HashMap<>();
    private final HashMap<String, Pair<Long, Integer>> mErrorTimingCache = new HashMap<>();

    private HdmiLogger() {
    }

    static final void warning(String str, Object... objArr) {
        getLogger().warningInternal(toLogString(str, objArr));
    }

    private void warningInternal(String str) {
        String strUpdateLog = updateLog(this.mWarningTimingCache, str);
        if (!strUpdateLog.isEmpty()) {
            Slog.w(TAG, strUpdateLog);
        }
    }

    static final void error(String str, Object... objArr) {
        getLogger().errorInternal(toLogString(str, objArr));
    }

    private void errorInternal(String str) {
        String strUpdateLog = updateLog(this.mErrorTimingCache, str);
        if (!strUpdateLog.isEmpty()) {
            Slog.e(TAG, strUpdateLog);
        }
    }

    static final void debug(String str, Object... objArr) {
        getLogger().debugInternal(toLogString(str, objArr));
    }

    private void debugInternal(String str) {
        if (DEBUG) {
            Slog.d(TAG, str);
        }
    }

    private static final String toLogString(String str, Object[] objArr) {
        if (objArr.length > 0) {
            return String.format(str, objArr);
        }
        return str;
    }

    private static HdmiLogger getLogger() {
        HdmiLogger hdmiLogger = sLogger.get();
        if (hdmiLogger == null) {
            HdmiLogger hdmiLogger2 = new HdmiLogger();
            sLogger.set(hdmiLogger2);
            return hdmiLogger2;
        }
        return hdmiLogger;
    }

    private static String updateLog(HashMap<String, Pair<Long, Integer>> map, String str) {
        long jUptimeMillis = SystemClock.uptimeMillis();
        Pair<Long, Integer> pair = map.get(str);
        if (shouldLogNow(pair, jUptimeMillis)) {
            String strBuildMessage = buildMessage(str, pair);
            map.put(str, new Pair<>(Long.valueOf(jUptimeMillis), 1));
            return strBuildMessage;
        }
        increaseLogCount(map, str);
        return BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
    }

    private static String buildMessage(String str, Pair<Long, Integer> pair) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        sb.append(pair == null ? 1 : ((Integer) pair.second).intValue());
        sb.append("]:");
        sb.append(str);
        return sb.toString();
    }

    private static void increaseLogCount(HashMap<String, Pair<Long, Integer>> map, String str) {
        Pair<Long, Integer> pair = map.get(str);
        if (pair != null) {
            map.put(str, new Pair<>((Long) pair.first, Integer.valueOf(((Integer) pair.second).intValue() + 1)));
        }
    }

    private static boolean shouldLogNow(Pair<Long, Integer> pair, long j) {
        return pair == null || j - ((Long) pair.first).longValue() > ERROR_LOG_DURATTION_MILLIS;
    }
}
