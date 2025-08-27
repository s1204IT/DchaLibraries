package com.android.settings.fuelgauge.anomaly;

import android.content.Context;
import android.net.Uri;
import android.provider.Settings;
import android.util.KeyValueListParser;
import android.util.Log;
import java.util.Arrays;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/* loaded from: classes.dex */
public class AnomalyDetectionPolicy {
    static final String KEY_ANOMALY_DETECTION_ENABLED = "anomaly_detection_enabled";
    static final String KEY_BLUETOOTH_SCAN_DETECTION_ENABLED = "bluetooth_scan_enabled";
    static final String KEY_BLUETOOTH_SCAN_THRESHOLD = "bluetooth_scan_threshold";
    static final String KEY_WAKELOCK_DETECTION_ENABLED = "wakelock_enabled";
    static final String KEY_WAKELOCK_THRESHOLD = "wakelock_threshold";
    static final String KEY_WAKEUP_ALARM_DETECTION_ENABLED = "wakeup_alarm_enabled";
    static final String KEY_WAKEUP_ALARM_THRESHOLD = "wakeup_alarm_threshold";
    static final String KEY_WAKEUP_BLACKLISTED_TAGS = "wakeup_blacklisted_tags";
    final boolean anomalyDetectionEnabled;
    final boolean bluetoothScanDetectionEnabled;
    public final long bluetoothScanThreshold;
    private final KeyValueListParser mParser = new KeyValueListParser(',');
    final boolean wakeLockDetectionEnabled;
    public final long wakeLockThreshold;
    final boolean wakeupAlarmDetectionEnabled;
    public final long wakeupAlarmThreshold;
    public final Set<String> wakeupBlacklistedTags;

    public AnomalyDetectionPolicy(Context context) {
        try {
            this.mParser.setString(Settings.Global.getString(context.getContentResolver(), "anomaly_detection_constants"));
        } catch (IllegalArgumentException e) {
            Log.e("AnomalyDetectionPolicy", "Bad anomaly detection constants");
        }
        this.anomalyDetectionEnabled = this.mParser.getBoolean(KEY_ANOMALY_DETECTION_ENABLED, false);
        this.wakeLockDetectionEnabled = this.mParser.getBoolean(KEY_WAKELOCK_DETECTION_ENABLED, false);
        this.wakeupAlarmDetectionEnabled = this.mParser.getBoolean(KEY_WAKEUP_ALARM_DETECTION_ENABLED, false);
        this.bluetoothScanDetectionEnabled = this.mParser.getBoolean(KEY_BLUETOOTH_SCAN_DETECTION_ENABLED, false);
        this.wakeLockThreshold = this.mParser.getLong(KEY_WAKELOCK_THRESHOLD, 3600000L);
        this.wakeupAlarmThreshold = this.mParser.getLong(KEY_WAKEUP_ALARM_THRESHOLD, 10L);
        this.wakeupBlacklistedTags = parseStringSet(KEY_WAKEUP_BLACKLISTED_TAGS, null);
        this.bluetoothScanThreshold = this.mParser.getLong(KEY_BLUETOOTH_SCAN_THRESHOLD, 1800000L);
    }

    public boolean isAnomalyDetectorEnabled(int i) {
        switch (i) {
            case 0:
                return this.wakeLockDetectionEnabled;
            case 1:
                return this.wakeupAlarmDetectionEnabled;
            case 2:
                return this.bluetoothScanDetectionEnabled;
            default:
                return false;
        }
    }

    private Set<String> parseStringSet(String str, Set<String> set) {
        String string = this.mParser.getString(str, (String) null);
        if (string != null) {
            return (Set) Arrays.stream(string.split(":")).map(new Function() { // from class: com.android.settings.fuelgauge.anomaly.-$$Lambda$AnomalyDetectionPolicy$MGZTkxm_LWhWFo0-u65o5bz97bA
                @Override // java.util.function.Function
                public final Object apply(Object obj) {
                    return ((String) obj).trim();
                }
            }).map(new Function() { // from class: com.android.settings.fuelgauge.anomaly.-$$Lambda$AnomalyDetectionPolicy$xFZhNZfuK_aveGITeM1VIXBhSVQ
                @Override // java.util.function.Function
                public final Object apply(Object obj) {
                    return Uri.decode((String) obj);
                }
            }).collect(Collectors.toSet());
        }
        return set;
    }
}
