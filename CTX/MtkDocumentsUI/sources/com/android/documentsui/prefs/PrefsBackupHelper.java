package com.android.documentsui.prefs;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import java.util.Map;

final class PrefsBackupHelper {
    private SharedPreferences mDefaultPreferences;

    PrefsBackupHelper(SharedPreferences sharedPreferences) {
        this.mDefaultPreferences = sharedPreferences;
    }

    PrefsBackupHelper(Context context) {
        this.mDefaultPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    void getBackupPreferences(SharedPreferences sharedPreferences) {
        SharedPreferences.Editor editorEdit = sharedPreferences.edit();
        editorEdit.clear();
        copyMatchingPreferences(this.mDefaultPreferences, editorEdit);
        editorEdit.apply();
    }

    void putBackupPreferences(SharedPreferences sharedPreferences) {
        SharedPreferences.Editor editorEdit = this.mDefaultPreferences.edit();
        copyMatchingPreferences(sharedPreferences, editorEdit);
        editorEdit.apply();
    }

    private void copyMatchingPreferences(SharedPreferences sharedPreferences, SharedPreferences.Editor editor) {
        for (Map.Entry<String, ?> entry : sharedPreferences.getAll().entrySet()) {
            if (Preferences.shouldBackup(entry.getKey())) {
                setPreference(editor, entry);
            }
        }
    }

    void setPreference(SharedPreferences.Editor editor, Map.Entry<String, ?> entry) {
        String key = entry.getKey();
        Object value = entry.getValue();
        if (value instanceof Integer) {
            editor.putInt(key, ((Integer) value).intValue());
        } else {
            if (value instanceof Boolean) {
                editor.putBoolean(key, ((Boolean) value).booleanValue());
                return;
            }
            StringBuilder sb = new StringBuilder();
            sb.append("DocumentsUI backup: invalid preference ");
            sb.append(value == null ? null : value.getClass());
            throw new IllegalArgumentException(sb.toString());
        }
    }
}
