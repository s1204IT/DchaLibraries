package com.mediatek.systemui.ext;

import android.content.Context;
import android.content.res.ColorStateList;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

/* loaded from: classes.dex */
public interface ISystemUIStatusBarExt {
    void SetHostViewInvisible(ImageView imageView);

    void addCallback(StatusBarCallback statusBarCallback);

    void addCustomizedView(int i, Context context, ViewGroup viewGroup);

    @Deprecated
    void addSignalClusterCustomizedView(Context context, ViewGroup viewGroup, int i);

    String[] addSlot(String[] strArr);

    @Deprecated
    boolean checkIfSlotIdChanged(int i, int i2);

    boolean disableHostFunction();

    int getClockAmPmStyle(int i);

    int getCommonSignalIconId(int i);

    int getCustomizeCsState(ServiceState serviceState, int i);

    int getCustomizeSignalStrengthIcon(int i, int i2, SignalStrength signalStrength, int i3, ServiceState serviceState);

    int getCustomizeSignalStrengthLevel(int i, SignalStrength signalStrength, ServiceState serviceState);

    int getDataTypeIcon(int i, int i2, int i3, int i4, ServiceState serviceState);

    int getNetworkTypeIcon(int i, int i2, int i3, ServiceState serviceState);

    void getServiceStateForCustomizedView(int i);

    boolean handleCallStateChanged(int i, int i2, String str, ServiceState serviceState);

    void isDataDisabled(int i, boolean z);

    boolean isInCsCall();

    boolean needShowRoamingIcons(boolean z);

    boolean needShowWfcIcon();

    void registerOpStateListener();

    @Deprecated
    void setCustomizedAirplaneView(View view, boolean z);

    void setCustomizedDataTypeView(int i, int i2, boolean z, boolean z2);

    void setCustomizedMobileTypeView(int i, ImageView imageView);

    void setCustomizedNetworkTypeView(int i, int i2, ImageView imageView);

    @Deprecated
    void setCustomizedNoSimView(ImageView imageView);

    void setCustomizedNoSimView(boolean z);

    @Deprecated
    void setCustomizedNoSimsVisible(boolean z);

    void setCustomizedPlmnTextTint(int i);

    void setCustomizedSignalStrengthView(int i, int i2, ImageView imageView);

    void setCustomizedView(int i);

    void setCustomizedVolteView(int i, ImageView imageView);

    void setDisVolteView(int i, int i2, ImageView imageView);

    @Deprecated
    void setIconTint(int i, float f);

    void setIconTint(ColorStateList colorStateList);

    void setImsRegInfo(int i, int i2, boolean z, boolean z2);

    void setImsSlotId(int i);

    @Deprecated
    void setNoSimIconTint(int i, ImageView imageView);

    void setSimInserted(int i, boolean z);

    boolean updateSignalStrengthWifiOnlyMode(ServiceState serviceState, boolean z);

    public interface StatusBarCallback {
        default void setSystemIcon(String str, int i, CharSequence charSequence, boolean z) {
        }
    }
}
