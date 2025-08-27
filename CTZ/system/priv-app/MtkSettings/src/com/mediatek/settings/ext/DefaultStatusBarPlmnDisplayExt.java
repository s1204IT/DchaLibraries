package com.mediatek.settings.ext;

import android.content.Context;
import android.content.ContextWrapper;
import android.support.v7.preference.PreferenceScreen;
import android.util.Log;

/* loaded from: classes.dex */
public class DefaultStatusBarPlmnDisplayExt extends ContextWrapper implements IStatusBarPlmnDisplayExt {
    static final String TAG = "DefaultStatusBarPlmnDisplayExt";

    public DefaultStatusBarPlmnDisplayExt(Context context) {
        super(context);
        Log.d("@M_DefaultStatusBarPlmnDisplayExt", "Into DefaultStatusBarPlmnPlugin");
    }

    @Override // com.mediatek.settings.ext.IStatusBarPlmnDisplayExt
    public void createCheckBox(PreferenceScreen preferenceScreen, int i) {
        Log.d("@M_DefaultStatusBarPlmnDisplayExt", "Into Default createCheckBox");
    }
}
