package com.android.systemui.statusbar.phone;

import android.R;
import android.content.Context;
import android.os.Handler;
import android.telephony.SubscriptionInfo;
import android.telephony.TelephonyManager;
import android.util.ArraySet;
import android.util.Log;
import com.android.systemui.Dependency;
import com.android.systemui.statusbar.policy.NetworkController;
import com.android.systemui.statusbar.policy.SecurityController;
import com.android.systemui.tuner.TunerService;
import com.mediatek.systemui.ext.ISystemUIStatusBarExt;
import com.mediatek.systemui.ext.OpSystemUICustomizationFactoryBase;
import com.mediatek.systemui.statusbar.util.FeatureOptions;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class StatusBarSignalPolicy implements NetworkController.SignalCallback, SecurityController.SecurityControllerCallback, TunerService.Tunable, ISystemUIStatusBarExt.StatusBarCallback {
    private static final boolean DEBUG;
    private boolean mActivityEnabled;
    private boolean mBlockAirplane;
    private boolean mBlockEthernet;
    private boolean mBlockMobile;
    private boolean mBlockWifi;
    private final Context mContext;
    private boolean mForceBlockWifi;
    private final StatusBarIconController mIconController;
    private final NetworkController mNetworkController;
    private final SecurityController mSecurityController;
    private final String mSlotAirplane;
    private final String mSlotEthernet;
    private final String mSlotMobile;
    private final String mSlotVpn;
    private final String mSlotWifi;
    private ISystemUIStatusBarExt mStatusBarExt;
    private final Handler mHandler = Handler.getMain();
    private boolean mIsAirplaneMode = false;
    private boolean mWifiVisible = false;
    private ArrayList<MobileIconState> mMobileStates = new ArrayList<>();
    private WifiIconState mWifiIconState = new WifiIconState();
    private boolean mNoSimsVisible = false;

    static {
        DEBUG = Log.isLoggable("StatusBarSignalPolicy", 3) || FeatureOptions.LOG_ENABLE;
    }

    public StatusBarSignalPolicy(Context context, StatusBarIconController statusBarIconController) {
        this.mContext = context;
        this.mSlotAirplane = this.mContext.getString(R.string.mediasize_iso_b4);
        this.mSlotMobile = this.mContext.getString(R.string.mediasize_iso_c8);
        this.mSlotWifi = this.mContext.getString(R.string.mediasize_japanese_jis_b6);
        this.mSlotEthernet = this.mContext.getString(R.string.mediasize_iso_c2);
        this.mSlotVpn = this.mContext.getString(R.string.mediasize_japanese_jis_b5);
        this.mActivityEnabled = this.mContext.getResources().getBoolean(com.android.systemui.R.bool.config_showActivity);
        this.mStatusBarExt = OpSystemUICustomizationFactoryBase.getOpFactory(context).makeSystemUIStatusBar(context);
        this.mStatusBarExt.addCallback(this);
        this.mIconController = statusBarIconController;
        this.mNetworkController = (NetworkController) Dependency.get(NetworkController.class);
        this.mSecurityController = (SecurityController) Dependency.get(SecurityController.class);
        this.mNetworkController.addCallback((NetworkController.SignalCallback) this);
        this.mSecurityController.addCallback(this);
    }

    public void updateVpn() {
        boolean zIsVpnEnabled = this.mSecurityController.isVpnEnabled();
        this.mIconController.setIcon(this.mSlotVpn, currentVpnIconId(this.mSecurityController.isVpnBranded()), null);
        this.mIconController.setIconVisibility(this.mSlotVpn, zIsVpnEnabled);
    }

    private int currentVpnIconId(boolean z) {
        return z ? com.android.systemui.R.drawable.stat_sys_branded_vpn : com.android.systemui.R.drawable.stat_sys_vpn_ic;
    }

    @Override
    public void onStateChanged() {
        this.mHandler.post(new Runnable() {
            @Override
            public final void run() {
                this.f$0.updateVpn();
            }
        });
    }

    @Override
    public void onTuningChanged(String str, String str2) {
        if (!"icon_blacklist".equals(str)) {
            return;
        }
        ArraySet<String> iconBlacklist = StatusBarIconController.getIconBlacklist(str2);
        boolean zContains = iconBlacklist.contains(this.mSlotAirplane);
        boolean zContains2 = iconBlacklist.contains(this.mSlotMobile);
        boolean zContains3 = iconBlacklist.contains(this.mSlotWifi);
        boolean zContains4 = iconBlacklist.contains(this.mSlotEthernet);
        if (zContains != this.mBlockAirplane || zContains2 != this.mBlockMobile || zContains4 != this.mBlockEthernet || zContains3 != this.mBlockWifi) {
            this.mBlockAirplane = zContains;
            this.mBlockMobile = zContains2;
            this.mBlockEthernet = zContains4;
            this.mBlockWifi = zContains3 || this.mForceBlockWifi;
            this.mNetworkController.removeCallback((NetworkController.SignalCallback) this);
        }
    }

    @Override
    public void setWifiIndicators(boolean z, NetworkController.IconState iconState, NetworkController.IconState iconState2, boolean z2, boolean z3, String str, boolean z4, String str2) {
        boolean z5 = false;
        boolean z6 = iconState.visible && !this.mBlockWifi;
        boolean z7 = z2 && this.mActivityEnabled && z6;
        boolean z8 = z3 && this.mActivityEnabled && z6;
        WifiIconState wifiIconStateCopy = this.mWifiIconState.copy();
        wifiIconStateCopy.visible = z6;
        wifiIconStateCopy.resId = iconState.icon;
        wifiIconStateCopy.activityIn = z7;
        wifiIconStateCopy.activityOut = z8;
        wifiIconStateCopy.slot = this.mSlotWifi;
        wifiIconStateCopy.airplaneSpacerVisible = this.mIsAirplaneMode;
        wifiIconStateCopy.contentDescription = iconState.contentDescription;
        MobileIconState firstMobileState = getFirstMobileState();
        if (firstMobileState != null && firstMobileState.typeId != 0) {
            z5 = true;
        }
        wifiIconStateCopy.signalSpacerVisible = z5;
        updateWifiIconWithState(wifiIconStateCopy);
        this.mWifiIconState = wifiIconStateCopy;
    }

    private void updateShowWifiSignalSpacer(WifiIconState wifiIconState) {
        MobileIconState firstMobileState = getFirstMobileState();
        wifiIconState.signalSpacerVisible = (firstMobileState == null || firstMobileState.typeId == 0) ? false : true;
    }

    private void updateWifiIconWithState(WifiIconState wifiIconState) {
        if (wifiIconState.visible && wifiIconState.resId > 0) {
            this.mIconController.setSignalIcon(this.mSlotWifi, wifiIconState);
            this.mIconController.setIconVisibility(this.mSlotWifi, true);
        } else {
            this.mIconController.setIconVisibility(this.mSlotWifi, false);
        }
    }

    @Override
    public void setMobileDataIndicators(NetworkController.IconState iconState, NetworkController.IconState iconState2, int i, int i2, int i3, int i4, boolean z, boolean z2, String str, String str2, boolean z3, int i5, boolean z4, boolean z5) {
        MobileIconState state = getState(i5);
        if (state == null) {
            return;
        }
        boolean z6 = false;
        boolean z7 = i != state.typeId && (i == 0 || state.typeId == 0);
        state.visible = iconState.visible && !this.mBlockMobile;
        state.strengthId = iconState.icon;
        state.typeId = i;
        state.contentDescription = iconState.contentDescription;
        state.typeContentDescription = str;
        state.roaming = z4;
        state.activityIn = z && this.mActivityEnabled;
        if (z2 && this.mActivityEnabled) {
            z6 = true;
        }
        state.activityOut = z6;
        state.networkIcon = i2;
        state.volteIcon = i3;
        state.mDataActivityIn = z;
        state.mDataActivityOut = z2;
        state.mDefaultData = z5;
        this.mIconController.setMobileIcons(this.mSlotMobile, MobileIconState.copyStates(this.mMobileStates));
        if (z7) {
            WifiIconState wifiIconStateCopy = this.mWifiIconState.copy();
            updateShowWifiSignalSpacer(wifiIconStateCopy);
            if (!Objects.equals(wifiIconStateCopy, this.mWifiIconState)) {
                updateWifiIconWithState(wifiIconStateCopy);
                this.mWifiIconState = wifiIconStateCopy;
            }
        }
    }

    private MobileIconState getState(int i) {
        for (MobileIconState mobileIconState : this.mMobileStates) {
            if (mobileIconState.subId == i) {
                return mobileIconState;
            }
        }
        Log.e("StatusBarSignalPolicy", "Unexpected subscription " + i);
        return null;
    }

    private MobileIconState getFirstMobileState() {
        if (this.mMobileStates.size() > 0) {
            return this.mMobileStates.get(0);
        }
        return null;
    }

    @Override
    public void setSubs(List<SubscriptionInfo> list) {
        if (DEBUG) {
            Log.d("StatusBarSignalPolicy", "setSubs: size = " + list.size() + ", subs = " + list);
        }
        if (hasCorrectSubs(list)) {
            if (DEBUG) {
                Log.d("StatusBarSignalPolicy", "setSubs: hasCorrectSubs and return");
                return;
            }
            return;
        }
        this.mIconController.removeAllIconsForSlot(this.mSlotMobile);
        this.mMobileStates.clear();
        int size = list.size();
        for (int i = 0; i < size; i++) {
            this.mMobileStates.add(new MobileIconState(list.get(i).getSubscriptionId(), list.get(i).getSimSlotIndex()));
        }
    }

    private boolean hasCorrectSubs(List<SubscriptionInfo> list) {
        int size = list.size();
        if (size != this.mMobileStates.size()) {
            return false;
        }
        for (int i = 0; i < size; i++) {
            if (this.mMobileStates.get(i).subId != list.get(i).getSubscriptionId()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void setNoSims(boolean z, boolean z2) {
        this.mNoSimsVisible = (!z || this.mBlockMobile || this.mIsAirplaneMode) ? false : true;
        this.mStatusBarExt.setCustomizedNoSimView(this.mNoSimsVisible);
    }

    @Override
    public void setEthernetIndicators(NetworkController.IconState iconState) {
        if (iconState.visible) {
            boolean z = this.mBlockEthernet;
        }
        int i = iconState.icon;
        String str = iconState.contentDescription;
        if (i > 0) {
            this.mIconController.setIcon(this.mSlotEthernet, i, str);
            this.mIconController.setIconVisibility(this.mSlotEthernet, true);
        } else {
            this.mIconController.setIconVisibility(this.mSlotEthernet, false);
        }
    }

    @Override
    public void setIsAirplaneMode(NetworkController.IconState iconState) {
        this.mIsAirplaneMode = iconState.visible && !this.mBlockAirplane;
        int i = iconState.icon;
        String str = iconState.contentDescription;
        if (this.mIsAirplaneMode && i > 0) {
            this.mIconController.setIcon(this.mSlotAirplane, i, str);
            this.mIconController.setIconVisibility(this.mSlotAirplane, true);
        } else {
            this.mIconController.setIconVisibility(this.mSlotAirplane, false);
        }
    }

    @Override
    public void setMobileDataEnabled(boolean z) {
    }

    public static abstract class SignalIconState {
        public boolean activityIn;
        public boolean activityOut;
        public String contentDescription;
        public String slot;
        public boolean visible;

        private SignalIconState() {
        }

        public boolean equals(Object obj) {
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            SignalIconState signalIconState = (SignalIconState) obj;
            return this.visible == signalIconState.visible && this.activityOut == signalIconState.activityOut && this.activityIn == signalIconState.activityIn && Objects.equals(this.contentDescription, signalIconState.contentDescription) && Objects.equals(this.slot, signalIconState.slot);
        }

        public int hashCode() {
            return Objects.hash(Boolean.valueOf(this.visible), Boolean.valueOf(this.activityOut), this.slot);
        }

        protected void copyTo(SignalIconState signalIconState) {
            signalIconState.visible = this.visible;
            signalIconState.activityIn = this.activityIn;
            signalIconState.activityOut = this.activityOut;
            signalIconState.slot = this.slot;
            signalIconState.contentDescription = this.contentDescription;
        }
    }

    public static class WifiIconState extends SignalIconState {
        public boolean airplaneSpacerVisible;
        public int resId;
        public boolean signalSpacerVisible;

        public WifiIconState() {
            super();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null || getClass() != obj.getClass() || !super.equals(obj)) {
                return false;
            }
            WifiIconState wifiIconState = (WifiIconState) obj;
            return this.resId == wifiIconState.resId && this.airplaneSpacerVisible == wifiIconState.airplaneSpacerVisible && this.signalSpacerVisible == wifiIconState.signalSpacerVisible;
        }

        public void copyTo(WifiIconState wifiIconState) {
            super.copyTo((SignalIconState) wifiIconState);
            wifiIconState.resId = this.resId;
            wifiIconState.airplaneSpacerVisible = this.airplaneSpacerVisible;
            wifiIconState.signalSpacerVisible = this.signalSpacerVisible;
        }

        public WifiIconState copy() {
            WifiIconState wifiIconState = new WifiIconState();
            copyTo(wifiIconState);
            return wifiIconState;
        }

        @Override
        public int hashCode() {
            return Objects.hash(Integer.valueOf(super.hashCode()), Integer.valueOf(this.resId), Boolean.valueOf(this.airplaneSpacerVisible), Boolean.valueOf(this.signalSpacerVisible));
        }

        public String toString() {
            return "WifiIconState(resId=" + this.resId + ", visible=" + this.visible + ")";
        }
    }

    public static class MobileIconState extends SignalIconState {
        public boolean mDataActivityIn;
        public boolean mDataActivityOut;
        public boolean mDefaultData;
        public boolean needsLeadingPadding;
        public int networkIcon;
        public int phoneId;
        public boolean roaming;
        public int strengthId;
        public int subId;
        public String typeContentDescription;
        public int typeId;
        public int volteIcon;

        private MobileIconState(int i) {
            super();
            this.subId = i;
        }

        private MobileIconState(int i, int i2) {
            this(i);
            this.phoneId = i2;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null || getClass() != obj.getClass() || !super.equals(obj)) {
                return false;
            }
            MobileIconState mobileIconState = (MobileIconState) obj;
            return this.subId == mobileIconState.subId && this.strengthId == mobileIconState.strengthId && this.typeId == mobileIconState.typeId && this.roaming == mobileIconState.roaming && this.needsLeadingPadding == mobileIconState.needsLeadingPadding && Objects.equals(this.typeContentDescription, mobileIconState.typeContentDescription) && this.networkIcon == mobileIconState.networkIcon && this.mDataActivityIn == mobileIconState.mDataActivityIn && this.mDataActivityOut == mobileIconState.mDataActivityOut && this.mDefaultData == mobileIconState.mDefaultData && this.volteIcon == mobileIconState.volteIcon && this.phoneId == this.phoneId;
        }

        @Override
        public int hashCode() {
            return Objects.hash(Integer.valueOf(super.hashCode()), Integer.valueOf(this.subId), Integer.valueOf(this.strengthId), Integer.valueOf(this.typeId), Boolean.valueOf(this.roaming), Boolean.valueOf(this.needsLeadingPadding), this.typeContentDescription, Integer.valueOf(this.networkIcon), Boolean.valueOf(this.mDataActivityIn), Boolean.valueOf(this.mDataActivityOut), Boolean.valueOf(this.mDefaultData), Integer.valueOf(this.volteIcon), Integer.valueOf(this.phoneId));
        }

        public MobileIconState copy() {
            MobileIconState mobileIconState = new MobileIconState(this.subId);
            copyTo(mobileIconState);
            return mobileIconState;
        }

        public void copyTo(MobileIconState mobileIconState) {
            super.copyTo((SignalIconState) mobileIconState);
            mobileIconState.subId = this.subId;
            mobileIconState.strengthId = this.strengthId;
            mobileIconState.typeId = this.typeId;
            mobileIconState.roaming = this.roaming;
            mobileIconState.needsLeadingPadding = this.needsLeadingPadding;
            mobileIconState.typeContentDescription = this.typeContentDescription;
            mobileIconState.networkIcon = this.networkIcon;
            mobileIconState.mDataActivityIn = this.mDataActivityIn;
            mobileIconState.mDataActivityOut = this.mDataActivityOut;
            mobileIconState.mDefaultData = this.mDefaultData;
            mobileIconState.volteIcon = this.volteIcon;
            mobileIconState.phoneId = this.phoneId;
        }

        public static List<MobileIconState> copyStates(List<MobileIconState> list) {
            ArrayList arrayList = new ArrayList();
            for (MobileIconState mobileIconState : list) {
                MobileIconState mobileIconState2 = new MobileIconState(mobileIconState.subId);
                mobileIconState.copyTo(mobileIconState2);
                arrayList.add(mobileIconState2);
            }
            return arrayList;
        }

        public String toString() {
            return "MobileIconState(subId=" + this.subId + ", strengthId=" + this.strengthId + ", roaming=" + this.roaming + ", typeId=" + this.typeId + ", networkIcon = " + this.networkIcon + ", mDataActivityIn: " + this.mDataActivityIn + ", mDataActivityOut: " + this.mDataActivityOut + ", mDefaultData: " + this.mDefaultData + ", volteIcon = " + this.volteIcon + ", visible=" + this.visible + ", phoneId = " + this.phoneId + ")";
        }
    }

    @Override
    public void setSystemIcon(String str, int i, CharSequence charSequence, boolean z) {
        if (str.equals("hdvoice")) {
            i = com.android.systemui.R.drawable.stat_sys_hd_voice_call;
        } else if (str.equals("nosim")) {
            int simCount = TelephonyManager.getDefault().getSimCount();
            if (simCount == 1) {
                i = com.android.systemui.R.drawable.stat_sys_signal_null_one_sim;
            } else if (simCount == 2) {
                i = com.android.systemui.R.drawable.stat_sys_signal_null_two_sims;
            }
        }
        if (DEBUG) {
            Log.d("StatusBarSignalPolicy", "setSystemIcon, slot = " + str + ", resourceId = " + i + ", contentDescription = " + ((Object) charSequence) + ", isVisible = " + z);
        }
        this.mIconController.setIcon(str, i, charSequence);
        this.mIconController.setIconVisibility(str, z);
    }
}
