package com.android.keyguard;

import android.os.SystemProperties;

/* loaded from: classes.dex */
public class KeyguardConstants {
    private static final boolean IS_ENG_BUILD = SystemProperties.get("ro.build.type").equals("eng");
    public static final boolean DEBUG = IS_ENG_BUILD;
    public static final boolean DEBUG_SIM_STATES = IS_ENG_BUILD;
}
