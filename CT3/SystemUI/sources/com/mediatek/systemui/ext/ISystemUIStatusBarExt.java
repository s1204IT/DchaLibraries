package com.mediatek.systemui.ext;

import android.content.Context;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

public interface ISystemUIStatusBarExt {
    void addCustomizedView(int i, Context context, ViewGroup viewGroup);

    void addSignalClusterCustomizedView(Context context, ViewGroup viewGroup, int i);

    boolean checkIfSlotIdChanged(int i, int i2);

    int getCustomizeCsState(ServiceState serviceState, int i);

    int getCustomizeSignalStrengthIcon(int i, int i2, SignalStrength signalStrength, int i3, ServiceState serviceState);

    int getCustomizeSignalStrengthLevel(int i, SignalStrength signalStrength, ServiceState serviceState);

    int getDataTypeIcon(int i, int i2, int i3, int i4, ServiceState serviceState);

    int getNetworkTypeIcon(int i, int i2, int i3, ServiceState serviceState);

    void getServiceStateForCustomizedView(int i);

    boolean needShowRoamingIcons(boolean z);

    void registerOpStateListener();

    void setCustomizedAirplaneView(View view, boolean z);

    void setCustomizedDataTypeView(int i, int i2, boolean z, boolean z2);

    void setCustomizedMobileTypeView(int i, ImageView imageView);

    void setCustomizedNetworkTypeView(int i, int i2, ImageView imageView);

    void setCustomizedNoSimView(ImageView imageView);

    void setCustomizedNoSimsVisible(boolean z);

    void setCustomizedSignalStrengthView(int i, int i2, ImageView imageView);

    void setCustomizedView(int i);

    void setCustomizedVolteView(int i, ImageView imageView);

    void setImsSlotId(int i);

    void setSimInserted(int i, boolean z);

    boolean updateSignalStrengthWifiOnlyMode(ServiceState serviceState, boolean z);
}
