package com.android.documentsui.prefs;

public final class Preferences {
    public static boolean shouldBackup(String str) {
        return LocalPreferences.shouldBackup(str) || ScopedPreferences.shouldBackup(str);
    }
}
