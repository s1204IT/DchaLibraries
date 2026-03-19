package com.mediatek.phone.ext;

import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import com.android.internal.telephony.OperatorInfo;
import java.util.List;

public interface INetworkSettingExt {
    List<OperatorInfo> customizeNetworkList(List<OperatorInfo> list, int i);

    String customizeNetworkName(OperatorInfo operatorInfo, int i, String str);

    void customizeNetworkSelectKey(Object obj);

    void initOtherNetworkSetting(PreferenceCategory preferenceCategory);

    void initOtherNetworkSetting(PreferenceScreen preferenceScreen);

    boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference);

    boolean onPreferenceTreeClick(OperatorInfo operatorInfo, int i);
}
