package com.mediatek.settings.ext;

import android.content.Context;
import android.content.ContextWrapper;
import android.util.Log;

public class DefaultWWOPJoynSettingsExt extends ContextWrapper implements IWWOPJoynSettingsExt {
    private static final String TAG = "DefaultWWOPJoynSettingsExt";

    public DefaultWWOPJoynSettingsExt(Context context) {
        super(context);
    }

    @Override
    public boolean isJoynSettingsEnabled() {
        Log.d("@M_DefaultWWOPJoynSettingsExt", "isJoynSettingsEnabled");
        return false;
    }
}
