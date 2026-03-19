package com.android.documentsui.prefs;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import com.android.documentsui.base.RootInfo;

public class LocalPreferences {
    static final boolean $assertionsDisabled = false;

    public static int getViewMode(Context context, RootInfo rootInfo, int i) {
        return getPrefs(context).getInt(createKey(rootInfo), i);
    }

    public static void setViewMode(Context context, RootInfo rootInfo, int i) {
        getPrefs(context).edit().putInt(createKey(rootInfo), i).apply();
    }

    private static SharedPreferences getPrefs(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }

    private static String createKey(RootInfo rootInfo) {
        return "rootViewMode-" + rootInfo.authority + rootInfo.rootId;
    }

    public static boolean shouldBackup(String str) {
        if (str != null) {
            return str.startsWith("rootViewMode-");
        }
        return false;
    }
}
