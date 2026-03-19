package com.android.server.devicepolicy;

import android.util.KeyValueListParser;
import android.util.Slog;
import java.io.PrintWriter;
import java.util.concurrent.TimeUnit;

public class DevicePolicyConstants {
    private static final String DAS_DIED_SERVICE_RECONNECT_BACKOFF_INCREASE_KEY = "das_died_service_reconnect_backoff_increase";
    private static final String DAS_DIED_SERVICE_RECONNECT_BACKOFF_SEC_KEY = "das_died_service_reconnect_backoff_sec";
    private static final String DAS_DIED_SERVICE_RECONNECT_MAX_BACKOFF_SEC_KEY = "das_died_service_reconnect_max_backoff_sec";
    private static final String TAG = "DevicePolicyManager";
    public final double DAS_DIED_SERVICE_RECONNECT_BACKOFF_INCREASE;
    public final long DAS_DIED_SERVICE_RECONNECT_BACKOFF_SEC;
    public final long DAS_DIED_SERVICE_RECONNECT_MAX_BACKOFF_SEC;

    private DevicePolicyConstants(String str) {
        KeyValueListParser keyValueListParser = new KeyValueListParser(',');
        try {
            keyValueListParser.setString(str);
        } catch (IllegalArgumentException e) {
            Slog.e(TAG, "Bad device policy settings: " + str);
        }
        long j = keyValueListParser.getLong(DAS_DIED_SERVICE_RECONNECT_BACKOFF_SEC_KEY, TimeUnit.HOURS.toSeconds(1L));
        double d = keyValueListParser.getFloat(DAS_DIED_SERVICE_RECONNECT_BACKOFF_INCREASE_KEY, 2.0f);
        long j2 = keyValueListParser.getLong(DAS_DIED_SERVICE_RECONNECT_MAX_BACKOFF_SEC_KEY, TimeUnit.DAYS.toSeconds(1L));
        long jMax = Math.max(5L, j);
        double dMax = Math.max(1.0d, d);
        long jMax2 = Math.max(jMax, j2);
        this.DAS_DIED_SERVICE_RECONNECT_BACKOFF_SEC = jMax;
        this.DAS_DIED_SERVICE_RECONNECT_BACKOFF_INCREASE = dMax;
        this.DAS_DIED_SERVICE_RECONNECT_MAX_BACKOFF_SEC = jMax2;
    }

    public static DevicePolicyConstants loadFromString(String str) {
        return new DevicePolicyConstants(str);
    }

    public void dump(String str, PrintWriter printWriter) {
        printWriter.print(str);
        printWriter.println("Constants:");
        printWriter.print(str);
        printWriter.print("  DAS_DIED_SERVICE_RECONNECT_BACKOFF_SEC: ");
        printWriter.println(this.DAS_DIED_SERVICE_RECONNECT_BACKOFF_SEC);
        printWriter.print(str);
        printWriter.print("  DAS_DIED_SERVICE_RECONNECT_BACKOFF_INCREASE: ");
        printWriter.println(this.DAS_DIED_SERVICE_RECONNECT_BACKOFF_INCREASE);
        printWriter.print(str);
        printWriter.print("  DAS_DIED_SERVICE_RECONNECT_MAX_BACKOFF_SEC: ");
        printWriter.println(this.DAS_DIED_SERVICE_RECONNECT_MAX_BACKOFF_SEC);
    }
}
