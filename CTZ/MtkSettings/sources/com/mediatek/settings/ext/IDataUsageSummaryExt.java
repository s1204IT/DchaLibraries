package com.mediatek.settings.ext;

import android.content.Context;
import android.content.IntentFilter;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.view.View;

public interface IDataUsageSummaryExt {
    boolean customDualReceiver(String str);

    void customReceiver(IntentFilter intentFilter);

    boolean customTempdata(int i);

    void customTempdataHide(SwitchPreference switchPreference);

    boolean isAllowDataDisableForOtherSubscription();

    boolean isAllowDataEnable(int i);

    void onBindViewHolder(Context context, View view, View.OnClickListener onClickListener);

    boolean onDisablingData(int i);

    void setPreferenceSummary(Preference preference);
}
