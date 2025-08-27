package com.mediatek.systemui.ext;

import android.content.Context;
import android.content.res.ColorStateList;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import com.mediatek.systemui.ext.ISystemUIStatusBarExt;

/* loaded from: classes.dex */
public class DefaultSystemUIStatusBarExt implements ISystemUIStatusBarExt {
    public DefaultSystemUIStatusBarExt(Context context) {
    }

    @Override // com.mediatek.systemui.ext.ISystemUIStatusBarExt
    public boolean disableHostFunction() {
        return false;
    }

    @Override // com.mediatek.systemui.ext.ISystemUIStatusBarExt
    public boolean checkIfSlotIdChanged(int i, int i2) {
        return false;
    }

    @Override // com.mediatek.systemui.ext.ISystemUIStatusBarExt
    public void isDataDisabled(int i, boolean z) {
    }

    @Override // com.mediatek.systemui.ext.ISystemUIStatusBarExt
    public void getServiceStateForCustomizedView(int i) {
    }

    @Override // com.mediatek.systemui.ext.ISystemUIStatusBarExt
    public int getCustomizeCsState(ServiceState serviceState, int i) {
        return i;
    }

    @Override // com.mediatek.systemui.ext.ISystemUIStatusBarExt
    public boolean isInCsCall() {
        return false;
    }

    @Override // com.mediatek.systemui.ext.ISystemUIStatusBarExt
    public int getNetworkTypeIcon(int i, int i2, int i3, ServiceState serviceState) {
        return i2;
    }

    @Override // com.mediatek.systemui.ext.ISystemUIStatusBarExt
    public int getDataTypeIcon(int i, int i2, int i3, int i4, ServiceState serviceState) {
        return i2;
    }

    @Override // com.mediatek.systemui.ext.ISystemUIStatusBarExt
    public int getCustomizeSignalStrengthIcon(int i, int i2, SignalStrength signalStrength, int i3, ServiceState serviceState) {
        return i2;
    }

    @Override // com.mediatek.systemui.ext.ISystemUIStatusBarExt
    public int getCustomizeSignalStrengthLevel(int i, SignalStrength signalStrength, ServiceState serviceState) {
        return i;
    }

    @Override // com.mediatek.systemui.ext.ISystemUIStatusBarExt
    public void addCustomizedView(int i, Context context, ViewGroup viewGroup) {
    }

    @Override // com.mediatek.systemui.ext.ISystemUIStatusBarExt
    public void addSignalClusterCustomizedView(Context context, ViewGroup viewGroup, int i) {
    }

    @Override // com.mediatek.systemui.ext.ISystemUIStatusBarExt
    public void setCustomizedNetworkTypeView(int i, int i2, ImageView imageView) {
    }

    @Override // com.mediatek.systemui.ext.ISystemUIStatusBarExt
    public void setCustomizedDataTypeView(int i, int i2, boolean z, boolean z2) {
    }

    @Override // com.mediatek.systemui.ext.ISystemUIStatusBarExt
    public void setCustomizedMobileTypeView(int i, ImageView imageView) {
    }

    @Override // com.mediatek.systemui.ext.ISystemUIStatusBarExt
    public void setCustomizedSignalStrengthView(int i, int i2, ImageView imageView) {
    }

    @Override // com.mediatek.systemui.ext.ISystemUIStatusBarExt
    public int getCommonSignalIconId(int i) {
        return i;
    }

    @Override // com.mediatek.systemui.ext.ISystemUIStatusBarExt
    public void SetHostViewInvisible(ImageView imageView) {
    }

    @Override // com.mediatek.systemui.ext.ISystemUIStatusBarExt
    public void setImsRegInfo(int i, int i2, boolean z, boolean z2) {
    }

    @Override // com.mediatek.systemui.ext.ISystemUIStatusBarExt
    public void setDisVolteView(int i, int i2, ImageView imageView) {
    }

    @Override // com.mediatek.systemui.ext.ISystemUIStatusBarExt
    public void setCustomizedView(int i) {
    }

    @Override // com.mediatek.systemui.ext.ISystemUIStatusBarExt
    public void setCustomizedNoSimView(boolean z) {
    }

    @Override // com.mediatek.systemui.ext.ISystemUIStatusBarExt
    public void setCustomizedNoSimView(ImageView imageView) {
    }

    @Override // com.mediatek.systemui.ext.ISystemUIStatusBarExt
    public void setCustomizedVolteView(int i, ImageView imageView) {
    }

    @Override // com.mediatek.systemui.ext.ISystemUIStatusBarExt
    public void setCustomizedAirplaneView(View view, boolean z) {
    }

    @Override // com.mediatek.systemui.ext.ISystemUIStatusBarExt
    public void setCustomizedNoSimsVisible(boolean z) {
    }

    @Override // com.mediatek.systemui.ext.ISystemUIStatusBarExt
    public boolean updateSignalStrengthWifiOnlyMode(ServiceState serviceState, boolean z) {
        return z;
    }

    @Override // com.mediatek.systemui.ext.ISystemUIStatusBarExt
    public void registerOpStateListener() {
    }

    @Override // com.mediatek.systemui.ext.ISystemUIStatusBarExt
    public void setIconTint(int i, float f) {
    }

    @Override // com.mediatek.systemui.ext.ISystemUIStatusBarExt
    public void setIconTint(ColorStateList colorStateList) {
    }

    @Override // com.mediatek.systemui.ext.ISystemUIStatusBarExt
    public void setNoSimIconTint(int i, ImageView imageView) {
    }

    @Override // com.mediatek.systemui.ext.ISystemUIStatusBarExt
    public void setSimInserted(int i, boolean z) {
    }

    @Override // com.mediatek.systemui.ext.ISystemUIStatusBarExt
    public void setImsSlotId(int i) {
    }

    @Override // com.mediatek.systemui.ext.ISystemUIStatusBarExt
    public void setCustomizedPlmnTextTint(int i) {
    }

    @Override // com.mediatek.systemui.ext.ISystemUIStatusBarExt
    public boolean handleCallStateChanged(int i, int i2, String str, ServiceState serviceState) {
        return false;
    }

    @Override // com.mediatek.systemui.ext.ISystemUIStatusBarExt
    public boolean needShowRoamingIcons(boolean z) {
        return true;
    }

    @Override // com.mediatek.systemui.ext.ISystemUIStatusBarExt
    public int getClockAmPmStyle(int i) {
        return i;
    }

    @Override // com.mediatek.systemui.ext.ISystemUIStatusBarExt
    public boolean needShowWfcIcon() {
        return true;
    }

    @Override // com.mediatek.systemui.ext.ISystemUIStatusBarExt
    public void addCallback(ISystemUIStatusBarExt.StatusBarCallback statusBarCallback) {
    }

    @Override // com.mediatek.systemui.ext.ISystemUIStatusBarExt
    public String[] addSlot(String[] strArr) {
        return strArr;
    }
}
