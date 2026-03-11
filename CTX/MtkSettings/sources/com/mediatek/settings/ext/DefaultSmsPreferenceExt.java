package com.mediatek.settings.ext;

import android.content.Context;
import android.support.v7.preference.ListPreference;

public class DefaultSmsPreferenceExt implements ISmsPreferenceExt {
    @Override
    public boolean canSetSummary() {
        return true;
    }

    @Override
    public void createBroadcastReceiver(Context context, ListPreference listPreference) {
    }

    @Override
    public boolean getBroadcastIntent(Context context, String str) {
        return true;
    }

    @Override
    public void deregisterBroadcastReceiver(Context context) {
    }
}
