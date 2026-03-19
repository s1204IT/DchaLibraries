package com.android.server;

import android.content.ContentResolver;
import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.os.FileUtils;
import android.os.RecoverySystem;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.provider.Settings;
import android.util.ExceptionUtils;
import android.util.MathUtils;
import android.util.Slog;
import android.util.SparseArray;
import com.android.internal.util.ArrayUtils;
import com.android.server.backup.BackupAgentTimeoutParameters;
import com.android.server.backup.BackupManagerConstants;
import com.android.server.pm.PackageManagerServiceUtils;
import java.io.File;

public class RescueParty {
    private static final int LEVEL_FACTORY_RESET = 4;
    private static final int LEVEL_NONE = 0;
    private static final int LEVEL_RESET_SETTINGS_TRUSTED_DEFAULTS = 3;
    private static final int LEVEL_RESET_SETTINGS_UNTRUSTED_CHANGES = 2;
    private static final int LEVEL_RESET_SETTINGS_UNTRUSTED_DEFAULTS = 1;
    private static final String PROP_DISABLE_RESCUE = "persist.sys.disable_rescue";
    private static final String PROP_ENABLE_RESCUE = "persist.sys.enable_rescue";
    private static final String PROP_RESCUE_BOOT_COUNT = "sys.rescue_boot_count";
    private static final String PROP_RESCUE_BOOT_START = "sys.rescue_boot_start";
    private static final String PROP_RESCUE_LEVEL = "sys.rescue_level";
    private static final String PROP_VIRTUAL_DEVICE = "ro.hardware.virtual_device";
    private static final String TAG = "RescueParty";
    private static final Threshold sBoot = new BootThreshold();
    private static SparseArray<Threshold> sApps = new SparseArray<>();

    private static boolean isDisabled() {
        if (SystemProperties.getBoolean(PROP_ENABLE_RESCUE, false)) {
            return false;
        }
        if (Build.IS_ENG) {
            Slog.v(TAG, "Disabled because of eng build");
            return true;
        }
        if (Build.IS_USERDEBUG && isUsbActive()) {
            Slog.v(TAG, "Disabled because of active USB connection");
            return true;
        }
        if (!SystemProperties.getBoolean(PROP_DISABLE_RESCUE, false)) {
            return false;
        }
        Slog.v(TAG, "Disabled because of manual property");
        return true;
    }

    public static void noteBoot(Context context) {
        if (!isDisabled() && sBoot.incrementAndTest()) {
            sBoot.reset();
            incrementRescueLevel(sBoot.uid);
            executeRescueLevel(context);
        }
    }

    public static void notePersistentAppCrash(Context context, int i) {
        if (isDisabled()) {
            return;
        }
        Threshold appThreshold = sApps.get(i);
        if (appThreshold == null) {
            appThreshold = new AppThreshold(i);
            sApps.put(i, appThreshold);
        }
        if (appThreshold.incrementAndTest()) {
            appThreshold.reset();
            incrementRescueLevel(appThreshold.uid);
            executeRescueLevel(context);
        }
    }

    public static boolean isAttemptingFactoryReset() {
        return SystemProperties.getInt(PROP_RESCUE_LEVEL, 0) == 4;
    }

    private static void incrementRescueLevel(int i) {
        int iConstrain = MathUtils.constrain(SystemProperties.getInt(PROP_RESCUE_LEVEL, 0) + 1, 0, 4);
        SystemProperties.set(PROP_RESCUE_LEVEL, Integer.toString(iConstrain));
        EventLogTags.writeRescueLevel(iConstrain, i);
        PackageManagerServiceUtils.logCriticalInfo(5, "Incremented rescue level to " + levelToString(iConstrain) + " triggered by UID " + i);
    }

    public static void onSettingsProviderPublished(Context context) {
        executeRescueLevel(context);
    }

    private static void executeRescueLevel(Context context) {
        int i = SystemProperties.getInt(PROP_RESCUE_LEVEL, 0);
        if (i == 0) {
            return;
        }
        Slog.w(TAG, "Attempting rescue level " + levelToString(i));
        try {
            executeRescueLevelInternal(context, i);
            EventLogTags.writeRescueSuccess(i);
            PackageManagerServiceUtils.logCriticalInfo(3, "Finished rescue level " + levelToString(i));
        } catch (Throwable th) {
            String completeMessage = ExceptionUtils.getCompleteMessage(th);
            EventLogTags.writeRescueFailure(i, completeMessage);
            PackageManagerServiceUtils.logCriticalInfo(6, "Failed rescue level " + levelToString(i) + ": " + completeMessage);
        }
    }

    private static void executeRescueLevelInternal(Context context, int i) throws Exception {
        switch (i) {
            case 1:
                resetAllSettings(context, 2);
                break;
            case 2:
                resetAllSettings(context, 3);
                break;
            case 3:
                resetAllSettings(context, 4);
                break;
            case 4:
                RecoverySystem.rebootPromptAndWipeUserData(context, TAG);
                break;
        }
    }

    private static void resetAllSettings(Context context, int i) throws Exception {
        RuntimeException runtimeException;
        ContentResolver contentResolver = context.getContentResolver();
        try {
            Settings.Global.resetToDefaultsAsUser(contentResolver, null, i, 0);
            runtimeException = null;
        } catch (Throwable th) {
            runtimeException = new RuntimeException("Failed to reset global settings", th);
        }
        for (int i2 : getAllUserIds()) {
            try {
                Settings.Secure.resetToDefaultsAsUser(contentResolver, null, i, i2);
            } catch (Throwable th2) {
                runtimeException = new RuntimeException("Failed to reset secure settings for " + i2, th2);
            }
        }
        if (runtimeException != null) {
            throw runtimeException;
        }
    }

    private static abstract class Threshold {
        private final int triggerCount;
        private final long triggerWindow;
        private final int uid;

        public abstract int getCount();

        public abstract long getStart();

        public abstract void setCount(int i);

        public abstract void setStart(long j);

        public Threshold(int i, int i2, long j) {
            this.uid = i;
            this.triggerCount = i2;
            this.triggerWindow = j;
        }

        public void reset() {
            setCount(0);
            setStart(0L);
        }

        public boolean incrementAndTest() {
            long jElapsedRealtime = SystemClock.elapsedRealtime();
            long start = jElapsedRealtime - getStart();
            if (start > this.triggerWindow) {
                setCount(1);
                setStart(jElapsedRealtime);
                return false;
            }
            int count = getCount() + 1;
            setCount(count);
            EventLogTags.writeRescueNote(this.uid, count, start);
            Slog.w(RescueParty.TAG, "Noticed " + count + " events for UID " + this.uid + " in last " + (start / 1000) + " sec");
            return count >= this.triggerCount;
        }
    }

    private static class BootThreshold extends Threshold {
        public BootThreshold() {
            super(0, 5, BackupAgentTimeoutParameters.DEFAULT_FULL_BACKUP_AGENT_TIMEOUT_MILLIS);
        }

        @Override
        public int getCount() {
            return SystemProperties.getInt(RescueParty.PROP_RESCUE_BOOT_COUNT, 0);
        }

        @Override
        public void setCount(int i) {
            SystemProperties.set(RescueParty.PROP_RESCUE_BOOT_COUNT, Integer.toString(i));
        }

        @Override
        public long getStart() {
            return SystemProperties.getLong(RescueParty.PROP_RESCUE_BOOT_START, 0L);
        }

        @Override
        public void setStart(long j) {
            SystemProperties.set(RescueParty.PROP_RESCUE_BOOT_START, Long.toString(j));
        }
    }

    private static class AppThreshold extends Threshold {
        private int count;
        private long start;

        public AppThreshold(int i) {
            super(i, 5, 30000L);
        }

        @Override
        public int getCount() {
            return this.count;
        }

        @Override
        public void setCount(int i) {
            this.count = i;
        }

        @Override
        public long getStart() {
            return this.start;
        }

        @Override
        public void setStart(long j) {
            this.start = j;
        }
    }

    private static int[] getAllUserIds() {
        int[] iArrAppendInt = {0};
        try {
            for (File file : FileUtils.listFilesOrEmpty(Environment.getDataSystemDeDirectory())) {
                try {
                    int i = Integer.parseInt(file.getName());
                    if (i != 0) {
                        iArrAppendInt = ArrayUtils.appendInt(iArrAppendInt, i);
                    }
                } catch (NumberFormatException e) {
                }
            }
        } catch (Throwable th) {
            Slog.w(TAG, "Trouble discovering users", th);
        }
        return iArrAppendInt;
    }

    private static boolean isUsbActive() {
        if (SystemProperties.getBoolean(PROP_VIRTUAL_DEVICE, false)) {
            Slog.v(TAG, "Assuming virtual device is connected over USB");
            return true;
        }
        try {
            return "CONFIGURED".equals(FileUtils.readTextFile(new File("/sys/class/android_usb/android0/state"), 128, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS).trim());
        } catch (Throwable th) {
            Slog.w(TAG, "Failed to determine if device was on USB", th);
            return false;
        }
    }

    private static String levelToString(int i) {
        switch (i) {
            case 0:
                return "NONE";
            case 1:
                return "RESET_SETTINGS_UNTRUSTED_DEFAULTS";
            case 2:
                return "RESET_SETTINGS_UNTRUSTED_CHANGES";
            case 3:
                return "RESET_SETTINGS_TRUSTED_DEFAULTS";
            case 4:
                return "FACTORY_RESET";
            default:
                return Integer.toString(i);
        }
    }
}
