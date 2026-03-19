package com.android.server;

import android.os.Binder;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.util.Slog;
import com.android.internal.os.BinderCallsStats;
import com.android.server.backup.BackupManagerConstants;
import java.io.FileDescriptor;
import java.io.PrintWriter;

public class BinderCallsStatsService extends Binder {
    private static final String PERSIST_SYS_BINDER_CALLS_DETAILED_TRACKING = "persist.sys.binder_calls_detailed_tracking";
    private static final String TAG = "BinderCallsStatsService";

    public static void start() {
        ServiceManager.addService("binder_calls_stats", new BinderCallsStatsService());
        if (SystemProperties.getBoolean(PERSIST_SYS_BINDER_CALLS_DETAILED_TRACKING, false)) {
            Slog.i(TAG, "Enabled CPU usage tracking for binder calls. Controlled by persist.sys.binder_calls_detailed_tracking or via dumpsys binder_calls_stats --enable-detailed-tracking");
            BinderCallsStats.getInstance().setDetailedTracking(true);
        }
    }

    public static void reset() {
        Slog.i(TAG, "Resetting stats");
        BinderCallsStats.getInstance().reset();
    }

    @Override
    protected void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        if (strArr != null) {
            for (String str : strArr) {
                if (!"-a".equals(str)) {
                    if ("--reset".equals(str)) {
                        reset();
                        printWriter.println("binder_calls_stats reset.");
                        return;
                    }
                    if ("--enable-detailed-tracking".equals(str)) {
                        SystemProperties.set(PERSIST_SYS_BINDER_CALLS_DETAILED_TRACKING, "1");
                        BinderCallsStats.getInstance().setDetailedTracking(true);
                        printWriter.println("Detailed tracking enabled");
                        return;
                    } else if ("--disable-detailed-tracking".equals(str)) {
                        SystemProperties.set(PERSIST_SYS_BINDER_CALLS_DETAILED_TRACKING, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
                        BinderCallsStats.getInstance().setDetailedTracking(false);
                        printWriter.println("Detailed tracking disabled");
                        return;
                    } else {
                        if ("-h".equals(str)) {
                            printWriter.println("binder_calls_stats commands:");
                            printWriter.println("  --reset: Reset stats");
                            printWriter.println("  --enable-detailed-tracking: Enables detailed tracking");
                            printWriter.println("  --disable-detailed-tracking: Disables detailed tracking");
                            return;
                        }
                        printWriter.println("Unknown option: " + str);
                    }
                }
            }
        }
        BinderCallsStats.getInstance().dump(printWriter);
    }
}
