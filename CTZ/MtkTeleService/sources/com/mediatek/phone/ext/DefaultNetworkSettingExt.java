package com.mediatek.phone.ext;

import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.util.Log;
import com.android.internal.telephony.OperatorInfo;
import java.util.List;

public class DefaultNetworkSettingExt implements INetworkSettingExt {
    @Override
    public List<OperatorInfo> customizeNetworkList(List<OperatorInfo> list, int i) {
        return list;
    }

    @Override
    public String customizeNetworkName(OperatorInfo operatorInfo, int i, String str) {
        return str;
    }

    @Override
    public boolean onPreferenceTreeClick(OperatorInfo operatorInfo, int i) {
        return false;
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        Log.d("DefaultNetworkSettingExt", "onPreferenceTreeClick");
        return false;
    }

    @Override
    public void initOtherNetworkSetting(PreferenceScreen preferenceScreen) {
    }

    @Override
    public void initOtherNetworkSetting(PreferenceCategory preferenceCategory) {
    }

    @Override
    public void customizeNetworkSelectKey(Object obj) {
    }
}
