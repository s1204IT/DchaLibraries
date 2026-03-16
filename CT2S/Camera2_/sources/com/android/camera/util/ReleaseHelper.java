package com.android.camera.util;

import android.app.Activity;
import com.android.camera.data.LocalData;
import com.android.camera.settings.SettingsManager;

public class ReleaseHelper {
    public static void showReleaseInfoDialogOnStart(Activity activity, SettingsManager settingsManager) {
    }

    public static void showReleaseInfoDialog(Activity activity, Callback<Void> callback) {
        callback.onCallback(null);
    }

    public static boolean shouldShowReleaseInfoDialogOnShare(LocalData data) {
        return false;
    }

    public static boolean shouldLogVerbose() {
        return false;
    }
}
