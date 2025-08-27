package com.android.systemui.statusbar;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.SystemProperties;
import android.telephony.SubscriptionInfo;
import android.util.ArraySet;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.widget.ImageView;
import android.widget.LinearLayout;
import com.android.settingslib.graph.SignalDrawable;
import com.android.systemui.Dependency;
import com.android.systemui.R;
import com.android.systemui.statusbar.phone.StatusBarIconController;
import com.android.systemui.statusbar.policy.DarkIconDispatcher;
import com.android.systemui.statusbar.policy.IconLogger;
import com.android.systemui.statusbar.policy.NetworkController;
import com.android.systemui.statusbar.policy.SecurityController;
import com.android.systemui.tuner.TunerService;
import com.android.systemui.util.Utils;
import com.mediatek.systemui.ext.ISystemUIStatusBarExt;
import com.mediatek.systemui.ext.OpSystemUICustomizationFactoryBase;
import com.mediatek.systemui.statusbar.util.FeatureOptions;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/* loaded from: classes.dex */
public class SignalClusterView extends LinearLayout implements DarkIconDispatcher.DarkReceiver, NetworkController.SignalCallback, SecurityController.SecurityControllerCallback, TunerService.Tunable {
    static final boolean DEBUG;
    private boolean mActivityEnabled;
    ImageView mAirplane;
    private String mAirplaneContentDescription;
    private int mAirplaneIconId;
    private boolean mBlockAirplane;
    private boolean mBlockEthernet;
    private boolean mBlockMobile;
    private boolean mBlockWifi;
    private float mDarkIntensity;
    private final int mEndPadding;
    private final int mEndPaddingNothingVisible;
    ImageView mEthernet;
    ImageView mEthernetDark;
    private String mEthernetDescription;
    ViewGroup mEthernetGroup;
    private int mEthernetIconId;
    private boolean mEthernetVisible;
    private boolean mForceBlockWifi;
    private final IconLogger mIconLogger;
    private final float mIconScaleFactor;
    private int mIconTint;
    private boolean mIsAirplaneMode;
    boolean mIsWfcEnable;
    private int mLastAirplaneIconId;
    private int mLastEthernetIconId;
    private int mLastVpnIconId;
    private int mLastWifiStrengthId;
    private final int mMobileDataIconStartPadding;
    LinearLayout mMobileSignalGroup;
    private final int mMobileSignalGroupEndPadding;
    private final NetworkController mNetworkController;
    private ArrayList<PhoneState> mPhoneStates;
    private final int mSecondaryTelephonyPadding;
    private final SecurityController mSecurityController;
    private ISystemUIStatusBarExt mStatusBarExt;
    private final Rect mTintArea;
    ImageView mVpn;
    private int mVpnIconId;
    private boolean mVpnVisible;
    ImageView mWifi;
    ImageView mWifiActivityIn;
    ImageView mWifiActivityOut;
    View mWifiAirplaneSpacer;
    ImageView mWifiDark;
    private String mWifiDescription;
    ViewGroup mWifiGroup;
    private boolean mWifiIn;
    private boolean mWifiOut;
    View mWifiSignalSpacer;
    private int mWifiStrengthId;
    private boolean mWifiVisible;

    static {
        DEBUG = Log.isLoggable("SignalClusterView", 3) || FeatureOptions.LOG_ENABLE;
    }

    public SignalClusterView(Context context) {
        this(context, null);
    }

    public SignalClusterView(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public SignalClusterView(Context context, AttributeSet attributeSet, int i) throws Resources.NotFoundException {
        super(context, attributeSet, i);
        this.mVpnVisible = false;
        this.mVpnIconId = 0;
        this.mLastVpnIconId = -1;
        this.mEthernetVisible = false;
        this.mEthernetIconId = 0;
        this.mLastEthernetIconId = -1;
        this.mWifiVisible = false;
        this.mWifiStrengthId = 0;
        this.mLastWifiStrengthId = -1;
        this.mIsAirplaneMode = false;
        this.mAirplaneIconId = 0;
        this.mLastAirplaneIconId = -1;
        this.mPhoneStates = new ArrayList<>();
        this.mIconTint = -1;
        this.mTintArea = new Rect();
        this.mIconLogger = (IconLogger) Dependency.get(IconLogger.class);
        Resources resources = getResources();
        this.mMobileSignalGroupEndPadding = resources.getDimensionPixelSize(R.dimen.mobile_signal_group_end_padding);
        this.mMobileDataIconStartPadding = resources.getDimensionPixelSize(R.dimen.mobile_data_icon_start_padding);
        this.mSecondaryTelephonyPadding = resources.getDimensionPixelSize(R.dimen.secondary_telephony_padding);
        this.mEndPadding = resources.getDimensionPixelSize(R.dimen.signal_cluster_battery_padding);
        this.mEndPaddingNothingVisible = resources.getDimensionPixelSize(R.dimen.no_signal_cluster_battery_padding);
        TypedValue typedValue = new TypedValue();
        resources.getValue(R.dimen.status_bar_icon_scale_factor, typedValue, true);
        this.mIconScaleFactor = typedValue.getFloat();
        this.mNetworkController = (NetworkController) Dependency.get(NetworkController.class);
        this.mSecurityController = (SecurityController) Dependency.get(SecurityController.class);
        addOnAttachStateChangeListener(new Utils.DisableStateTracker(0, 2));
        updateActivityEnabled();
        this.mStatusBarExt = OpSystemUICustomizationFactoryBase.getOpFactory(context).makeSystemUIStatusBar(context);
        this.mIsWfcEnable = SystemProperties.get("persist.vendor.mtk_wfc_support").equals("1");
    }

    @Override // com.android.systemui.tuner.TunerService.Tunable
    public void onTuningChanged(String str, String str2) {
        if (!"icon_blacklist".equals(str)) {
            return;
        }
        ArraySet<String> iconBlacklist = StatusBarIconController.getIconBlacklist(str2);
        boolean zContains = iconBlacklist.contains("airplane");
        boolean zContains2 = iconBlacklist.contains("mobile");
        boolean zContains3 = iconBlacklist.contains("wifi");
        boolean zContains4 = iconBlacklist.contains("ethernet");
        if (zContains != this.mBlockAirplane || zContains2 != this.mBlockMobile || zContains4 != this.mBlockEthernet || zContains3 != this.mBlockWifi) {
            this.mBlockAirplane = zContains;
            this.mBlockMobile = zContains2;
            this.mBlockEthernet = zContains4;
            this.mBlockWifi = zContains3 || this.mForceBlockWifi;
            this.mNetworkController.removeCallback((NetworkController.SignalCallback) this);
            this.mNetworkController.addCallback((NetworkController.SignalCallback) this);
        }
    }

    @Override // android.view.View
    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mVpn = (ImageView) findViewById(R.id.vpn);
        this.mEthernetGroup = (ViewGroup) findViewById(R.id.ethernet_combo);
        this.mEthernet = (ImageView) findViewById(R.id.ethernet);
        this.mEthernetDark = (ImageView) findViewById(R.id.ethernet_dark);
        this.mWifiGroup = (ViewGroup) findViewById(R.id.wifi_combo);
        this.mWifi = (ImageView) findViewById(R.id.wifi_signal);
        this.mWifiDark = (ImageView) findViewById(R.id.wifi_signal_dark);
        this.mWifiActivityIn = (ImageView) findViewById(R.id.wifi_in);
        this.mWifiActivityOut = (ImageView) findViewById(R.id.wifi_out);
        this.mAirplane = (ImageView) findViewById(R.id.airplane);
        this.mWifiAirplaneSpacer = findViewById(R.id.wifi_airplane_spacer);
        this.mWifiSignalSpacer = findViewById(R.id.wifi_signal_spacer);
        this.mMobileSignalGroup = (LinearLayout) findViewById(R.id.mobile_signal_group);
        maybeScaleVpnAndNoSimsIcons();
    }

    private void maybeScaleVpnAndNoSimsIcons() {
        if (this.mIconScaleFactor == 1.0f) {
            return;
        }
        this.mVpn.setImageDrawable(new ScalingDrawableWrapper(this.mVpn.getDrawable(), this.mIconScaleFactor));
    }

    @Override // android.view.ViewGroup, android.view.View
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        this.mVpnVisible = this.mSecurityController.isVpnEnabled();
        this.mVpnIconId = currentVpnIconId(this.mSecurityController.isVpnBranded());
        if (DEBUG) {
            Log.d("SignalClusterView", "onAttachedToWindow, mPhoneStates = " + this.mPhoneStates);
        }
        Iterator<PhoneState> it = this.mPhoneStates.iterator();
        while (it.hasNext()) {
            PhoneState next = it.next();
            if (next.mMobileGroup.getParent() == null) {
                this.mMobileSignalGroup.addView(next.mMobileGroup);
            }
        }
        this.mMobileSignalGroup.setPaddingRelative(0, 0, this.mMobileSignalGroup.getChildCount() > 0 ? this.mMobileSignalGroupEndPadding : 0, 0);
        ((TunerService) Dependency.get(TunerService.class)).addTunable(this, "icon_blacklist");
        this.mStatusBarExt.addSignalClusterCustomizedView(this.mContext, this, indexOfChild(findViewById(R.id.mobile_signal_group)));
        apply();
        applyIconTint();
        if (DEBUG) {
            Log.d("SignalClusterView", "onAttachedToWindow, addCallback = " + this);
        }
        this.mNetworkController.addCallback((NetworkController.SignalCallback) this);
        this.mSecurityController.addCallback(this);
    }

    @Override // android.view.ViewGroup, android.view.View
    protected void onDetachedFromWindow() {
        this.mMobileSignalGroup.removeAllViews();
        ((TunerService) Dependency.get(TunerService.class)).removeTunable(this);
        this.mSecurityController.removeCallback(this);
        if (DEBUG) {
            Log.d("SignalClusterView", "onDetachedFromWindow, removeCallback = " + this);
        }
        this.mNetworkController.removeCallback((NetworkController.SignalCallback) this);
        super.onDetachedFromWindow();
    }

    @Override // android.widget.LinearLayout, android.view.ViewGroup, android.view.View
    protected void onLayout(boolean z, int i, int i2, int i3, int i4) {
        super.onLayout(z, i, i2, i3, i4);
        applyIconTint();
    }

    @Override // com.android.systemui.statusbar.policy.SecurityController.SecurityControllerCallback
    public void onStateChanged() {
        post(new Runnable() { // from class: com.android.systemui.statusbar.SignalClusterView.1
            @Override // java.lang.Runnable
            public void run() {
                SignalClusterView.this.mVpnVisible = SignalClusterView.this.mSecurityController.isVpnEnabled();
                SignalClusterView.this.mVpnIconId = SignalClusterView.this.currentVpnIconId(SignalClusterView.this.mSecurityController.isVpnBranded());
                SignalClusterView.this.apply();
            }
        });
    }

    private void updateActivityEnabled() {
        this.mActivityEnabled = this.mContext.getResources().getBoolean(R.bool.config_showActivity);
    }

    @Override // com.android.systemui.statusbar.policy.NetworkController.SignalCallback
    public void setWifiIndicators(boolean z, NetworkController.IconState iconState, NetworkController.IconState iconState2, boolean z2, boolean z3, String str, boolean z4, String str2) {
        boolean z5 = false;
        this.mWifiVisible = iconState.visible && !this.mBlockWifi;
        this.mWifiStrengthId = iconState.icon;
        this.mWifiDescription = iconState.contentDescription;
        this.mWifiIn = z2 && this.mActivityEnabled && this.mWifiVisible;
        if (z3 && this.mActivityEnabled && this.mWifiVisible) {
            z5 = true;
        }
        this.mWifiOut = z5;
        apply();
    }

    @Override // com.android.systemui.statusbar.policy.NetworkController.SignalCallback
    public void setMobileDataIndicators(NetworkController.IconState iconState, NetworkController.IconState iconState2, int i, int i2, int i3, int i4, boolean z, boolean z2, String str, String str2, boolean z3, int i5, boolean z4, boolean z5) {
        PhoneState state = getState(i5);
        if (state == null) {
            return;
        }
        boolean z6 = false;
        state.mMobileVisible = iconState.visible && !this.mBlockMobile;
        state.mMobileStrengthId = iconState.icon;
        state.mMobileTypeId = i;
        state.mMobileDescription = iconState.contentDescription;
        state.mMobileTypeDescription = str;
        state.mNetworkIcon = i2;
        state.mVolteIcon = i3;
        state.mRoaming = z4;
        state.mActivityIn = z && this.mActivityEnabled;
        if (z2 && this.mActivityEnabled) {
            z6 = true;
        }
        state.mActivityOut = z6;
        state.mDataActivityIn = z;
        state.mDataActivityOut = z2;
        apply();
    }

    @Override // com.android.systemui.statusbar.policy.NetworkController.SignalCallback
    public void setEthernetIndicators(NetworkController.IconState iconState) {
        this.mEthernetVisible = iconState.visible && !this.mBlockEthernet;
        this.mEthernetIconId = iconState.icon;
        this.mEthernetDescription = iconState.contentDescription;
        apply();
    }

    @Override // com.android.systemui.statusbar.policy.NetworkController.SignalCallback
    public void setNoSims(boolean z, boolean z2) {
    }

    @Override // com.android.systemui.statusbar.policy.NetworkController.SignalCallback
    public void setSubs(List<SubscriptionInfo> list) {
        if (DEBUG) {
            Log.d("SignalClusterView", "setSubs, subs = " + list);
        }
        if (hasCorrectSubs(list)) {
            return;
        }
        this.mPhoneStates.clear();
        if (this.mMobileSignalGroup != null) {
            this.mMobileSignalGroup.removeAllViews();
        }
        int size = list.size();
        for (int i = 0; i < size; i++) {
            inflatePhoneState(list.get(i).getSubscriptionId());
        }
        if (isAttachedToWindow()) {
            applyIconTint();
        }
    }

    private boolean hasCorrectSubs(List<SubscriptionInfo> list) {
        int size = list.size();
        if (size != this.mPhoneStates.size()) {
            return false;
        }
        for (int i = 0; i < size; i++) {
            if (this.mPhoneStates.get(i).mSubId != list.get(i).getSubscriptionId() || this.mStatusBarExt.checkIfSlotIdChanged(list.get(i).getSubscriptionId(), list.get(i).getSimSlotIndex())) {
                return false;
            }
        }
        return true;
    }

    private PhoneState getState(int i) {
        Iterator<PhoneState> it = this.mPhoneStates.iterator();
        while (it.hasNext()) {
            PhoneState next = it.next();
            if (next.mSubId == i) {
                return next;
            }
        }
        Log.e("SignalClusterView", "Unexpected subscription " + i);
        return null;
    }

    private PhoneState inflatePhoneState(int i) {
        PhoneState phoneState = new PhoneState(i, this.mContext);
        if (this.mMobileSignalGroup != null) {
            this.mMobileSignalGroup.addView(phoneState.mMobileGroup);
        }
        this.mPhoneStates.add(phoneState);
        return phoneState;
    }

    @Override // com.android.systemui.statusbar.policy.NetworkController.SignalCallback
    public void setIsAirplaneMode(NetworkController.IconState iconState) {
        this.mIsAirplaneMode = iconState.visible && !this.mBlockAirplane;
        this.mAirplaneIconId = iconState.icon;
        this.mAirplaneContentDescription = iconState.contentDescription;
        apply();
    }

    @Override // com.android.systemui.statusbar.policy.NetworkController.SignalCallback
    public void setMobileDataEnabled(boolean z) {
    }

    public boolean dispatchPopulateAccessibilityEventInternal(AccessibilityEvent accessibilityEvent) {
        if (this.mEthernetVisible && this.mEthernetGroup != null && this.mEthernetGroup.getContentDescription() != null) {
            accessibilityEvent.getText().add(this.mEthernetGroup.getContentDescription());
        }
        if (this.mWifiVisible && this.mWifiGroup != null && this.mWifiGroup.getContentDescription() != null) {
            accessibilityEvent.getText().add(this.mWifiGroup.getContentDescription());
        }
        Iterator<PhoneState> it = this.mPhoneStates.iterator();
        while (it.hasNext()) {
            it.next().populateAccessibilityEvent(accessibilityEvent);
        }
        return super.dispatchPopulateAccessibilityEventInternal(accessibilityEvent);
    }

    @Override // android.widget.LinearLayout, android.view.View
    public void onRtlPropertiesChanged(int i) {
        super.onRtlPropertiesChanged(i);
        if (this.mEthernet != null) {
            this.mEthernet.setImageDrawable(null);
            this.mEthernetDark.setImageDrawable(null);
            this.mLastEthernetIconId = -1;
        }
        if (this.mWifi != null) {
            this.mWifi.setImageDrawable(null);
            this.mWifiDark.setImageDrawable(null);
            this.mLastWifiStrengthId = -1;
        }
        Iterator<PhoneState> it = this.mPhoneStates.iterator();
        while (it.hasNext()) {
            PhoneState next = it.next();
            if (next.mMobileType != null) {
                next.mMobileType.setImageDrawable(null);
                next.mLastMobileTypeId = -1;
            }
        }
        if (this.mAirplane != null) {
            this.mAirplane.setImageDrawable(null);
            this.mLastAirplaneIconId = -1;
        }
        apply();
    }

    @Override // android.view.View
    public boolean hasOverlappingRendering() {
        return false;
    }

    private void apply() {
        if (this.mWifiGroup == null) {
            return;
        }
        if (this.mVpnVisible) {
            if (this.mLastVpnIconId != this.mVpnIconId) {
                setIconForView(this.mVpn, this.mVpnIconId);
                this.mLastVpnIconId = this.mVpnIconId;
            }
            this.mIconLogger.onIconShown("vpn");
            this.mVpn.setVisibility(0);
        } else {
            this.mIconLogger.onIconHidden("vpn");
            this.mVpn.setVisibility(8);
        }
        boolean z = true;
        if (DEBUG) {
            Object[] objArr = new Object[1];
            objArr[0] = this.mVpnVisible ? "VISIBLE" : "GONE";
            Log.d("SignalClusterView", String.format("vpn: %s", objArr));
        }
        if (this.mEthernetVisible) {
            if (this.mLastEthernetIconId != this.mEthernetIconId) {
                setIconForView(this.mEthernet, this.mEthernetIconId);
                setIconForView(this.mEthernetDark, this.mEthernetIconId);
                this.mLastEthernetIconId = this.mEthernetIconId;
            }
            this.mEthernetGroup.setContentDescription(this.mEthernetDescription);
            this.mIconLogger.onIconShown("ethernet");
            this.mEthernetGroup.setVisibility(0);
        } else {
            this.mIconLogger.onIconHidden("ethernet");
            this.mEthernetGroup.setVisibility(8);
        }
        if (DEBUG) {
            Object[] objArr2 = new Object[1];
            objArr2[0] = this.mEthernetVisible ? "VISIBLE" : "GONE";
            Log.d("SignalClusterView", String.format("ethernet: %s", objArr2));
        }
        if (this.mWifiVisible) {
            if (this.mWifiStrengthId != this.mLastWifiStrengthId) {
                setIconForView(this.mWifi, this.mWifiStrengthId);
                setIconForView(this.mWifiDark, this.mWifiStrengthId);
                this.mLastWifiStrengthId = this.mWifiStrengthId;
            }
            this.mIconLogger.onIconShown("wifi");
            this.mWifiGroup.setContentDescription(this.mWifiDescription);
            this.mWifiGroup.setVisibility(0);
        } else {
            this.mIconLogger.onIconHidden("wifi");
            this.mWifiGroup.setVisibility(8);
        }
        if (DEBUG) {
            Object[] objArr3 = new Object[2];
            objArr3[0] = this.mWifiVisible ? "VISIBLE" : "GONE";
            objArr3[1] = Integer.valueOf(this.mWifiStrengthId);
            Log.d("SignalClusterView", String.format("wifi: %s sig=%d", objArr3));
        }
        this.mWifiActivityIn.setVisibility(this.mWifiIn ? 0 : 8);
        this.mWifiActivityOut.setVisibility(this.mWifiOut ? 0 : 8);
        boolean z2 = FeatureOptions.MTK_CTA_SET;
        Iterator<PhoneState> it = this.mPhoneStates.iterator();
        int i = 0;
        while (it.hasNext()) {
            PhoneState next = it.next();
            if (next.apply(z2) && !z2) {
                i = next.mMobileTypeId;
                z2 = true;
            }
        }
        if (z2) {
            this.mIconLogger.onIconShown("mobile");
        } else {
            this.mIconLogger.onIconHidden("mobile");
        }
        if (this.mIsAirplaneMode) {
            if (this.mLastAirplaneIconId != this.mAirplaneIconId) {
                setIconForView(this.mAirplane, this.mAirplaneIconId);
                this.mLastAirplaneIconId = this.mAirplaneIconId;
            }
            this.mAirplane.setContentDescription(this.mAirplaneContentDescription);
            this.mIconLogger.onIconShown("airplane");
            this.mAirplane.setVisibility(0);
        } else {
            this.mIconLogger.onIconHidden("airplane");
            this.mAirplane.setVisibility(8);
        }
        if (this.mIsAirplaneMode && this.mWifiVisible) {
            this.mWifiAirplaneSpacer.setVisibility(0);
        } else {
            this.mWifiAirplaneSpacer.setVisibility(8);
        }
        if (z2 && i != 0 && this.mWifiVisible) {
            this.mWifiSignalSpacer.setVisibility(0);
        } else {
            this.mWifiSignalSpacer.setVisibility(8);
        }
        if (!this.mWifiVisible && !this.mIsAirplaneMode && !z2 && !this.mVpnVisible && !this.mEthernetVisible) {
            z = false;
        }
        setPaddingRelative(0, 0, z ? this.mEndPadding : this.mEndPaddingNothingVisible, 0);
    }

    private void setIconForView(ImageView imageView, int i) {
        Drawable drawable = imageView.getContext().getDrawable(i);
        if (this.mIconScaleFactor == 1.0f) {
            imageView.setImageDrawable(drawable);
        } else {
            imageView.setImageDrawable(new ScalingDrawableWrapper(drawable, this.mIconScaleFactor));
        }
    }

    @Override // com.android.systemui.statusbar.policy.DarkIconDispatcher.DarkReceiver
    public void onDarkChanged(Rect rect, float f, int i) {
        boolean z = (i == this.mIconTint && f == this.mDarkIntensity && this.mTintArea.equals(rect)) ? false : true;
        this.mIconTint = i;
        this.mDarkIntensity = f;
        this.mTintArea.set(rect);
        if (z && isAttachedToWindow()) {
            applyIconTint();
        }
    }

    private void applyIconTint() {
        setTint(this.mVpn, DarkIconDispatcher.getTint(this.mTintArea, this.mVpn, this.mIconTint));
        setTint(this.mAirplane, DarkIconDispatcher.getTint(this.mTintArea, this.mAirplane, this.mIconTint));
        this.mStatusBarExt.setCustomizedPlmnTextTint(this.mIconTint);
        applyDarkIntensity(DarkIconDispatcher.getDarkIntensity(this.mTintArea, this.mWifi, this.mDarkIntensity), this.mWifi, this.mWifiDark);
        setTint(this.mWifiActivityIn, DarkIconDispatcher.getTint(this.mTintArea, this.mWifiActivityIn, this.mIconTint));
        setTint(this.mWifiActivityOut, DarkIconDispatcher.getTint(this.mTintArea, this.mWifiActivityOut, this.mIconTint));
        applyDarkIntensity(DarkIconDispatcher.getDarkIntensity(this.mTintArea, this.mEthernet, this.mDarkIntensity), this.mEthernet, this.mEthernetDark);
        for (int i = 0; i < this.mPhoneStates.size(); i++) {
            this.mPhoneStates.get(i).setIconTint(this.mIconTint, this.mDarkIntensity, this.mTintArea);
        }
    }

    private void applyDarkIntensity(float f, View view, View view2) {
        view.setAlpha(1.0f - f);
        view2.setAlpha(f);
    }

    private void setTint(ImageView imageView, int i) {
        imageView.setImageTintList(ColorStateList.valueOf(i));
    }

    private int currentVpnIconId(boolean z) {
        return z ? R.drawable.stat_sys_branded_vpn : R.drawable.stat_sys_vpn_ic;
    }

    private class PhoneState {
        public boolean mActivityIn;
        public boolean mActivityOut;
        private boolean mDataActivityIn;
        private boolean mDataActivityOut;
        private boolean mIsWfcCase;
        private ImageView mMobile;
        private ImageView mMobileActivityIn;
        private ImageView mMobileActivityOut;
        private String mMobileDescription;
        private ViewGroup mMobileGroup;
        private ImageView mMobileRoaming;
        private View mMobileRoamingSpace;
        private SignalDrawable mMobileSignalDrawable;
        private ImageView mMobileType;
        private String mMobileTypeDescription;
        private ImageView mNetworkType;
        private ISystemUIStatusBarExt mPhoneStateExt;
        public boolean mRoaming;
        private final int mSubId;
        private ImageView mVolteType;
        private boolean mMobileVisible = false;
        private int mMobileStrengthId = 0;
        private int mMobileTypeId = 0;
        private int mNetworkIcon = 0;
        private int mVolteIcon = 0;
        private int mLastMobileStrengthId = -1;
        private int mLastMobileTypeId = -1;

        public PhoneState(int i, Context context) {
            ViewGroup viewGroup = (ViewGroup) LayoutInflater.from(context).inflate(R.layout.mobile_signal_group_ext, (ViewGroup) null);
            this.mPhoneStateExt = OpSystemUICustomizationFactoryBase.getOpFactory(context).makeSystemUIStatusBar(context);
            this.mPhoneStateExt.addCustomizedView(i, context, viewGroup);
            setViews(viewGroup);
            this.mSubId = i;
        }

        public void setViews(ViewGroup viewGroup) {
            this.mMobileGroup = viewGroup;
            this.mMobile = (ImageView) viewGroup.findViewById(R.id.mobile_signal);
            this.mMobileType = (ImageView) viewGroup.findViewById(R.id.mobile_type);
            this.mNetworkType = (ImageView) viewGroup.findViewById(R.id.network_type);
            this.mVolteType = (ImageView) viewGroup.findViewById(R.id.volte_indicator_ext);
            this.mMobileRoaming = (ImageView) viewGroup.findViewById(R.id.mobile_roaming);
            this.mMobileRoamingSpace = viewGroup.findViewById(R.id.mobile_roaming_space);
            this.mMobileActivityIn = (ImageView) viewGroup.findViewById(R.id.mobile_in);
            this.mMobileActivityOut = (ImageView) viewGroup.findViewById(R.id.mobile_out);
            this.mMobileSignalDrawable = new SignalDrawable(this.mMobile.getContext());
            this.mMobile.setImageDrawable(this.mMobileSignalDrawable);
        }

        public boolean apply(boolean z) {
            if (!this.mMobileVisible || SignalClusterView.this.mIsAirplaneMode) {
                if (SignalClusterView.this.mIsAirplaneMode && SignalClusterView.this.mIsWfcEnable && this.mVolteIcon != 0) {
                    this.mMobileGroup.setVisibility(0);
                    hideViewInWfcCase();
                } else {
                    if (SignalClusterView.DEBUG) {
                        Log.d("SignalClusterView", "setVisibility as GONE, this = " + this + ", mMobileVisible = " + this.mMobileVisible + ", mIsAirplaneMode = " + SignalClusterView.this.mIsAirplaneMode + ", mIsWfcEnable = " + SignalClusterView.this.mIsWfcEnable + ", mVolteIcon = " + this.mVolteIcon);
                    }
                    this.mMobileGroup.setVisibility(8);
                }
            } else {
                if (this.mLastMobileStrengthId != this.mMobileStrengthId) {
                    this.mMobile.getDrawable().setLevel(this.mMobileStrengthId);
                    this.mLastMobileStrengthId = this.mMobileStrengthId;
                }
                if (this.mLastMobileTypeId != this.mMobileTypeId) {
                    if (!this.mPhoneStateExt.disableHostFunction()) {
                        this.mMobileType.setImageResource(this.mMobileTypeId);
                    }
                    this.mLastMobileTypeId = this.mMobileTypeId;
                }
                this.mMobileGroup.setContentDescription(this.mMobileTypeDescription + " " + this.mMobileDescription);
                this.mMobileGroup.setVisibility(0);
                showViewInWfcCase();
            }
            setCustomizeViewProperty();
            this.mMobileGroup.setPaddingRelative(z ? SignalClusterView.this.mSecondaryTelephonyPadding : 0, 0, 0, 0);
            this.mMobile.setPaddingRelative(SignalClusterView.this.mMobileDataIconStartPadding, 0, 0, 0);
            if (SignalClusterView.DEBUG) {
                Object[] objArr = new Object[3];
                objArr[0] = this.mMobileVisible ? "VISIBLE" : "GONE";
                objArr[1] = Integer.valueOf(this.mMobileStrengthId);
                objArr[2] = Integer.valueOf(this.mMobileTypeId);
                Log.d("SignalClusterView", String.format("mobile: %s sig=%d typ=%d", objArr));
            }
            if (!this.mIsWfcCase) {
                this.mMobileType.setVisibility(this.mMobileTypeId != 0 ? 0 : 8);
                this.mMobileRoaming.setVisibility(this.mRoaming ? 0 : 8);
                this.mMobileRoamingSpace.setVisibility(this.mRoaming ? 0 : 8);
                this.mMobileActivityIn.setVisibility(this.mActivityIn ? 0 : 8);
                this.mMobileActivityOut.setVisibility(this.mActivityOut ? 0 : 8);
            }
            setCustomizedOpViews();
            return this.mMobileVisible;
        }

        public void populateAccessibilityEvent(AccessibilityEvent accessibilityEvent) {
            if (this.mMobileVisible && this.mMobileGroup != null && this.mMobileGroup.getContentDescription() != null) {
                accessibilityEvent.getText().add(this.mMobileGroup.getContentDescription());
            }
        }

        public void setIconTint(int i, float f, Rect rect) {
            this.mMobileSignalDrawable.setDarkIntensity(f);
            SignalClusterView.this.setTint(this.mMobileType, DarkIconDispatcher.getTint(rect, this.mMobileType, i));
            SignalClusterView.this.setTint(this.mMobileRoaming, DarkIconDispatcher.getTint(rect, this.mMobileRoaming, i));
            SignalClusterView.this.setTint(this.mMobileActivityIn, DarkIconDispatcher.getTint(rect, this.mMobileActivityIn, i));
            SignalClusterView.this.setTint(this.mMobileActivityOut, DarkIconDispatcher.getTint(rect, this.mMobileActivityOut, i));
            SignalClusterView.this.setTint(this.mNetworkType, DarkIconDispatcher.getTint(rect, this.mNetworkType, i));
            SignalClusterView.this.setTint(this.mVolteType, DarkIconDispatcher.getTint(rect, this.mVolteType, i));
            this.mPhoneStateExt.setIconTint(DarkIconDispatcher.getTint(rect, this.mNetworkType, i), f);
        }

        private void setCustomizeViewProperty() {
            setNetworkIcon();
            setVolteIcon();
        }

        private void setVolteIcon() {
            if (this.mVolteIcon == 0) {
                this.mVolteType.setVisibility(8);
            } else {
                this.mVolteType.setImageResource(this.mVolteIcon);
                this.mVolteType.setVisibility(0);
            }
            SignalClusterView.this.mStatusBarExt.setCustomizedVolteView(this.mVolteIcon, this.mVolteType);
            SignalClusterView.this.mStatusBarExt.setDisVolteView(this.mSubId, this.mVolteIcon, this.mVolteType);
        }

        private void setNetworkIcon() {
            if (!FeatureOptions.MTK_CTA_SET || this.mIsWfcCase) {
                return;
            }
            if (this.mNetworkIcon == 0) {
                this.mNetworkType.setVisibility(8);
                return;
            }
            if (!this.mPhoneStateExt.disableHostFunction()) {
                this.mNetworkType.setImageResource(this.mNetworkIcon);
            }
            this.mNetworkType.setVisibility(0);
        }

        private void setCustomizedOpViews() {
            this.mPhoneStateExt.SetHostViewInvisible(this.mMobileRoaming);
            this.mPhoneStateExt.SetHostViewInvisible(this.mMobileActivityIn);
            this.mPhoneStateExt.SetHostViewInvisible(this.mMobileActivityOut);
            if (this.mMobileVisible && !SignalClusterView.this.mIsAirplaneMode) {
                this.mPhoneStateExt.getServiceStateForCustomizedView(this.mSubId);
                this.mPhoneStateExt.setCustomizedNetworkTypeView(this.mSubId, this.mNetworkIcon, this.mNetworkType);
                this.mPhoneStateExt.setCustomizedDataTypeView(this.mSubId, this.mMobileTypeId, this.mDataActivityIn, this.mDataActivityOut);
                this.mPhoneStateExt.setCustomizedSignalStrengthView(this.mSubId, this.mMobileStrengthId, this.mMobile);
                this.mPhoneStateExt.setCustomizedMobileTypeView(this.mSubId, this.mMobileType);
                this.mPhoneStateExt.setCustomizedView(this.mSubId);
            }
        }

        private void hideViewInWfcCase() {
            Log.d("SignalClusterView", "hideViewInWfcCase, isWfcEnabled = " + SignalClusterView.this.mIsWfcEnable + " mSubId =" + this.mSubId);
            this.mMobile.setVisibility(8);
            this.mMobileType.setVisibility(8);
            this.mNetworkType.setVisibility(8);
            this.mMobileRoaming.setVisibility(8);
            this.mIsWfcCase = true;
        }

        private void showViewInWfcCase() {
            if (this.mIsWfcCase) {
                Log.d("SignalClusterView", "showViewInWfcCase: mSubId = " + this.mSubId);
                this.mMobile.setVisibility(0);
                this.mMobileType.setVisibility(0);
                this.mNetworkType.setVisibility(0);
                this.mMobileRoaming.setVisibility(this.mRoaming ? 0 : 8);
                this.mIsWfcCase = false;
            }
        }
    }
}
