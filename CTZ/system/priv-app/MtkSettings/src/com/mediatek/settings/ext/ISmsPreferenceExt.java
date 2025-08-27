package com.mediatek.settings.ext;

import android.content.Context;
import android.support.v7.preference.ListPreference;

/* loaded from: classes.dex */
public interface ISmsPreferenceExt {
    boolean canSetSummary();

    void createBroadcastReceiver(Context context, ListPreference listPreference);

    void deregisterBroadcastReceiver(Context context);

    boolean getBroadcastIntent(Context context, String str);
}
