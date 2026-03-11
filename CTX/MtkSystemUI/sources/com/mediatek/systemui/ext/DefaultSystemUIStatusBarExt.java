package com.mediatek.systemui.ext;

import android.content.Context;
import android.content.res.ColorStateList;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import com.mediatek.systemui.ext.ISystemUIStatusBarExt;

public class DefaultSystemUIStatusBarExt implements ISystemUIStatusBarExt {
    public DefaultSystemUIStatusBarExt(Context context) {
    }

    @Override
    public boolean disableHostFunction() {
        return false;
    }

    @Override
    public boolean checkIfSlotIdChanged(int i, int i2) {
        return false;
    }

    @Override
    public void isDataDisabled(int i, boolean z) {
    }

    @Override
    public void getServiceStateForCustomizedView(int i) {
    }

    @Override
    public int getCustomizeCsState(ServiceState serviceState, int i) {
        return i;
    }

    @Override
    public boolean isInCsCall() {
        return false;
    }

    @Override
    public int getNetworkTypeIcon(int i, int i2, int i3, ServiceState serviceState) {
        return i2;
    }

    @Override
    public int getDataTypeIcon(int i, int i2, int i3, int i4, ServiceState serviceState) {
        return i2;
    }

    @Override
    public int getCustomizeSignalStrengthIcon(int i, int i2, SignalStrength signalStrength, int i3, ServiceState serviceState) {
        return i2;
    }

    @Override
    public int getCustomizeSignalStrengthLevel(int i, SignalStrength signalStrength, ServiceState serviceState) {
        return i;
    }

    @Override
    public void addCustomizedView(int i, Context context, ViewGroup viewGroup) {
    }

    @Override
    public void addSignalClusterCustomizedView(Context context, ViewGroup viewGroup, int i) {
    }

    @Override
    public void setCustomizedNetworkTypeView(int i, int i2, ImageView imageView) {
    }

    @Override
    public void setCustomizedDataTypeView(int i, int i2, boolean z, boolean z2) {
    }

    @Override
    public void setCustomizedMobileTypeView(int i, ImageView imageView) {
    }

    @Override
    public void setCustomizedSignalStrengthView(int i, int i2, ImageView imageView) {
    }

    @Override
    public int getCommonSignalIconId(int i) {
        return i;
    }

    @Override
    public void SetHostViewInvisible(ImageView imageView) {
    }

    @Override
    public void setImsRegInfo(int i, int i2, boolean z, boolean z2) {
    }

    @Override
    public void setDisVolteView(int i, int i2, ImageView imageView) {
    }

    @Override
    public void setCustomizedView(int i) {
    }

    @Override
    public void setCustomizedNoSimView(boolean z) {
    }

    @Override
    public void setCustomizedNoSimView(ImageView imageView) {
    }

    @Override
    public void setCustomizedVolteView(int i, ImageView imageView) {
    }

    @Override
    public void setCustomizedAirplaneView(View view, boolean z) {
    }

    @Override
    public void setCustomizedNoSimsVisible(boolean z) {
    }

    @Override
    public boolean updateSignalStrengthWifiOnlyMode(ServiceState serviceState, boolean z) {
        return z;
    }

    @Override
    public void registerOpStateListener() {
    }

    @Override
    public void setIconTint(int i, float f) {
    }

    @Override
    public void setIconTint(ColorStateList colorStateList) {
    }

    @Override
    public void setNoSimIconTint(int i, ImageView imageView) {
    }

    @Override
    public void setSimInserted(int i, boolean z) {
    }

    @Override
    public void setImsSlotId(int i) {
    }

    @Override
    public void setCustomizedPlmnTextTint(int i) {
    }

    @Override
    public boolean handleCallStateChanged(int i, int i2, String str, ServiceState serviceState) {
        return false;
    }

    @Override
    public boolean needShowRoamingIcons(boolean z) {
        return true;
    }

    @Override
    public int getClockAmPmStyle(int i) {
        return i;
    }

    @Override
    public boolean needShowWfcIcon() {
        return true;
    }

    @Override
    public void addCallback(ISystemUIStatusBarExt.StatusBarCallback statusBarCallback) {
    }

    @Override
    public String[] addSlot(String[] strArr) {
        return strArr;
    }
}
