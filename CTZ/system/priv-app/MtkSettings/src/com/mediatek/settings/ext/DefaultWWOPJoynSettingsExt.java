package com.mediatek.settings.ext;

import android.content.Context;
import android.content.ContextWrapper;
import android.util.Log;

/* loaded from: classes.dex */
public class DefaultWWOPJoynSettingsExt extends ContextWrapper implements IWWOPJoynSettingsExt {
    private static final String TAG = "DefaultWWOPJoynSettingsExt";

    public DefaultWWOPJoynSettingsExt(Context context) {
        super(context);
    }

    @Override // com.mediatek.settings.ext.IWWOPJoynSettingsExt
    public boolean isJoynSettingsEnabled() {
        Log.d("@M_DefaultWWOPJoynSettingsExt", "isJoynSettingsEnabled");
        return false;
    }
}
