package com.mediatek.settings.ext;

import android.content.Context;
import android.content.IntentFilter;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.view.View;

public class DefaultDataUsageSummaryExt implements IDataUsageSummaryExt {
    public DefaultDataUsageSummaryExt(Context context) {
    }

    @Override
    public boolean onDisablingData(int i) {
        return true;
    }

    @Override
    public boolean isAllowDataEnable(int i) {
        return true;
    }

    @Override
    public void onBindViewHolder(Context context, View view, View.OnClickListener onClickListener) {
    }

    @Override
    public void setPreferenceSummary(Preference preference) {
    }

    @Override
    public boolean customDualReceiver(String str) {
        return false;
    }

    @Override
    public void customReceiver(IntentFilter intentFilter) {
    }

    @Override
    public boolean isAllowDataDisableForOtherSubscription() {
        return false;
    }

    @Override
    public boolean customTempdata(int i) {
        return false;
    }

    @Override
    public void customTempdataHide(SwitchPreference switchPreference) {
    }
}
