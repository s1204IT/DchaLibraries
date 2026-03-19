package com.android.server.pm;

import android.os.SystemProperties;
import com.android.server.UiModeManagerService;
import dalvik.system.DexFile;

public class PackageManagerServiceCompilerMapping {
    static final int REASON_SHARED_INDEX = 6;
    public static final String[] REASON_STRINGS = {"first-boot", "boot", "install", "bg-dexopt", "ab-ota", "inactive", "shared"};

    static {
        if (7 != REASON_STRINGS.length) {
            throw new IllegalStateException("REASON_STRINGS not correct");
        }
        if (!"shared".equals(REASON_STRINGS[6])) {
            throw new IllegalStateException("REASON_STRINGS not correct because of shared index");
        }
    }

    private static String getSystemPropertyName(int i) {
        if (i < 0 || i >= REASON_STRINGS.length) {
            throw new IllegalArgumentException("reason " + i + " invalid");
        }
        return "pm.dexopt." + REASON_STRINGS[i];
    }

    private static String getAndCheckValidity(int i) {
        String str = SystemProperties.get(getSystemPropertyName(i));
        if (str == null || str.isEmpty() || !DexFile.isValidCompilerFilter(str)) {
            throw new IllegalStateException("Value \"" + str + "\" not valid (reason " + REASON_STRINGS[i] + ")");
        }
        if (!isFilterAllowedForReason(i, str)) {
            throw new IllegalStateException("Value \"" + str + "\" not allowed (reason " + REASON_STRINGS[i] + ")");
        }
        return str;
    }

    private static boolean isFilterAllowedForReason(int i, String str) {
        return (i == 6 && DexFile.isProfileGuidedCompilerFilter(str)) ? false : true;
    }

    static void checkProperties() {
        IllegalStateException illegalStateException = null;
        for (int i = 0; i <= 6; i++) {
            try {
                String systemPropertyName = getSystemPropertyName(i);
                if (systemPropertyName == null || systemPropertyName.isEmpty()) {
                    throw new IllegalStateException("Reason system property name \"" + systemPropertyName + "\" for reason " + REASON_STRINGS[i]);
                }
                getAndCheckValidity(i);
            } catch (Exception e) {
                if (illegalStateException == null) {
                    illegalStateException = new IllegalStateException("PMS compiler filter settings are bad.");
                }
                illegalStateException.addSuppressed(e);
            }
        }
        if (illegalStateException != null) {
            throw illegalStateException;
        }
    }

    public static String getCompilerFilterForReason(int i) {
        return getAndCheckValidity(i);
    }

    public static String getDefaultCompilerFilter() {
        String str = SystemProperties.get("dalvik.vm.dex2oat-filter");
        if (str == null || str.isEmpty() || !DexFile.isValidCompilerFilter(str) || DexFile.isProfileGuidedCompilerFilter(str)) {
            return "speed";
        }
        return str;
    }

    public static String getReasonName(int i) {
        if (i == -1) {
            return UiModeManagerService.Shell.NIGHT_MODE_STR_UNKNOWN;
        }
        if (i < 0 || i >= REASON_STRINGS.length) {
            throw new IllegalArgumentException("reason " + i + " invalid");
        }
        return REASON_STRINGS[i];
    }
}
