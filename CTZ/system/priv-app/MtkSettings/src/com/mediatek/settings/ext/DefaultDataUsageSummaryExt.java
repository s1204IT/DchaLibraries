package com.mediatek.settings.ext;

import android.content.Context;
import android.content.IntentFilter;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.view.View;

/* loaded from: classes.dex */
public class DefaultDataUsageSummaryExt implements IDataUsageSummaryExt {
    public DefaultDataUsageSummaryExt(Context context) {
    }

    @Override // com.mediatek.settings.ext.IDataUsageSummaryExt
    public boolean onDisablingData(int i) {
        return true;
    }

    @Override // com.mediatek.settings.ext.IDataUsageSummaryExt
    public boolean isAllowDataEnable(int i) {
        return true;
    }

    @Override // com.mediatek.settings.ext.IDataUsageSummaryExt
    public void onBindViewHolder(Context context, View view, View.OnClickListener onClickListener) {
    }

    @Override // com.mediatek.settings.ext.IDataUsageSummaryExt
    public void setPreferenceSummary(Preference preference) {
    }

    @Override // com.mediatek.settings.ext.IDataUsageSummaryExt
    public boolean customDualReceiver(String str) {
        return false;
    }

    @Override // com.mediatek.settings.ext.IDataUsageSummaryExt
    public void customReceiver(IntentFilter intentFilter) {
    }

    @Override // com.mediatek.settings.ext.IDataUsageSummaryExt
    public boolean isAllowDataDisableForOtherSubscription() {
        return false;
    }

    @Override // com.mediatek.settings.ext.IDataUsageSummaryExt
    public boolean customTempdata(int i) {
        return false;
    }

    @Override // com.mediatek.settings.ext.IDataUsageSummaryExt
    public void customTempdataHide(SwitchPreference switchPreference) {
    }
}
