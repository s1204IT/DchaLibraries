package com.android.shell;

import android.content.Context;
import android.content.SharedPreferences;

public class BugreportPrefs {
    public static int getWarningState(Context context, int def) {
        SharedPreferences prefs = context.getSharedPreferences("bugreports", 0);
        return prefs.getInt("warning-state", def);
    }

    public static void setWarningState(Context context, int value) {
        SharedPreferences prefs = context.getSharedPreferences("bugreports", 0);
        prefs.edit().putInt("warning-state", value).apply();
    }
}
