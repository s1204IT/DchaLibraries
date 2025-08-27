package com.mediatek.settings.ext;

import android.content.Context;
import android.support.v7.preference.ListPreference;

/* loaded from: classes.dex */
public class DefaultSmsPreferenceExt implements ISmsPreferenceExt {
    @Override // com.mediatek.settings.ext.ISmsPreferenceExt
    public boolean canSetSummary() {
        return true;
    }

    @Override // com.mediatek.settings.ext.ISmsPreferenceExt
    public void createBroadcastReceiver(Context context, ListPreference listPreference) {
    }

    @Override // com.mediatek.settings.ext.ISmsPreferenceExt
    public boolean getBroadcastIntent(Context context, String str) {
        return true;
    }

    @Override // com.mediatek.settings.ext.ISmsPreferenceExt
    public void deregisterBroadcastReceiver(Context context) {
    }
}
