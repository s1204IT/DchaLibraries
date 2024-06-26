package com.android.settings.password;
/* loaded from: classes.dex */
public enum ScreenLockType {
    NONE(0, "unlock_set_off"),
    SWIPE(0, "unlock_set_none"),
    PATTERN(65536, "unlock_set_pattern"),
    PIN(131072, 196608, "unlock_set_pin"),
    PASSWORD(262144, 393216, "unlock_set_password"),
    MANAGED(524288, "unlock_set_managed");
    
    public final int defaultQuality;
    public final int maxQuality;
    public final String preferenceKey;
    private static final ScreenLockType MIN_QUALITY = NONE;
    private static final ScreenLockType MAX_QUALITY = MANAGED;

    ScreenLockType(int i, String str) {
        this(i, i, str);
    }

    ScreenLockType(int i, int i2, String str) {
        this.defaultQuality = i;
        this.maxQuality = i2;
        this.preferenceKey = str;
    }

    public static ScreenLockType fromQuality(int i) {
        if (i != 0) {
            if (i != 65536) {
                if (i == 131072 || i == 196608) {
                    return PIN;
                }
                if (i == 262144 || i == 327680 || i == 393216) {
                    return PASSWORD;
                }
                if (i == 524288) {
                    return MANAGED;
                }
                return null;
            }
            return PATTERN;
        }
        return SWIPE;
    }

    public static ScreenLockType fromKey(String str) {
        ScreenLockType[] values;
        for (ScreenLockType screenLockType : values()) {
            if (screenLockType.preferenceKey.equals(str)) {
                return screenLockType;
            }
        }
        return null;
    }
}
