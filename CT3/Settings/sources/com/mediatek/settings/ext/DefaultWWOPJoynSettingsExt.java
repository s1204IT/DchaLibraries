package com.mediatek.settings.ext;

import android.util.Log;

public class DefaultWWOPJoynSettingsExt implements IWWOPJoynSettingsExt {
    private static final String TAG = "DefaultWWOPJoynSettingsExt";

    @Override
    public boolean isJoynSettingsEnabled() {
        Log.d("@M_DefaultWWOPJoynSettingsExt", "isJoynSettingsEnabled");
        return false;
    }
}
