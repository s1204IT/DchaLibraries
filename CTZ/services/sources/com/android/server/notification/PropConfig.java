package com.android.server.notification;

import android.content.Context;
import android.os.SystemProperties;

public class PropConfig {
    private static final String UNSET = "UNSET";

    public static int getInt(Context context, String str, int i) {
        return SystemProperties.getInt(str, context.getResources().getInteger(i));
    }

    public static String[] getStringArray(Context context, String str, int i) {
        String str2 = SystemProperties.get(str, UNSET);
        return !UNSET.equals(str2) ? str2.split(",") : context.getResources().getStringArray(i);
    }
}
