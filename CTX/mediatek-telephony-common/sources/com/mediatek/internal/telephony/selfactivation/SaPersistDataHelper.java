package com.mediatek.internal.telephony.selfactivation;

import android.content.Context;
import android.content.SharedPreferences;

public class SaPersistDataHelper {
    public static final String DATA_KEY_SA_STATE = "dataKeySaState";
    private static final String DATA_NAME_PREFIX = "selfActivationData";

    public static void putIntData(Context context, int i, String str, int i2) {
        getSharedPreference(context, i).edit().putInt(str, i2).apply();
    }

    public static int getIntData(Context context, int i, String str, int i2) {
        return getSharedPreference(context, i).getInt(str, i2);
    }

    public static void putStringData(Context context, int i, String str, String str2) {
        getSharedPreference(context, i).edit().putString(str, str2).apply();
    }

    public static String getStringData(Context context, int i, String str, String str2) {
        return getSharedPreference(context, i).getString(str, str2);
    }

    private static SharedPreferences getSharedPreference(Context context, int i) {
        return context.getSharedPreferences(getDataName(i), 0);
    }

    public static String getDataName(int i) {
        return DATA_NAME_PREFIX + i;
    }

    public static String toString(Context context, int i) {
        StringBuilder sb = new StringBuilder();
        sb.append("SaPersistDataHelper {");
        sb.append(" mDataName = " + getDataName(i));
        sb.append(" dataKeySaState = " + getIntData(context, i, DATA_KEY_SA_STATE, -1));
        sb.append("}");
        return sb.toString();
    }
}
