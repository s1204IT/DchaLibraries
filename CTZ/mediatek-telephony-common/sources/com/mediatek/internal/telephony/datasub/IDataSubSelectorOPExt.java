package com.mediatek.internal.telephony.datasub;

import android.content.Intent;

public interface IDataSubSelectorOPExt {
    void handleAirPlaneModeOff(Intent intent);

    void handleDefaultDataChanged(Intent intent);

    void handlePlmnChanged(Intent intent);

    void handleSimStateChanged(Intent intent);

    void handleSubinfoRecordUpdated(Intent intent);

    void init(DataSubSelector dataSubSelector, ISimSwitchForDSSExt iSimSwitchForDSSExt);

    void subSelector(Intent intent);
}
