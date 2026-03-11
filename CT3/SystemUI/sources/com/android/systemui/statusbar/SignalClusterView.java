package com.android.systemui.statusbar;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Rect;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.AnimatedVectorDrawable;
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
import com.android.systemui.R;
import com.android.systemui.statusbar.phone.StatusBarIconController;
import com.android.systemui.statusbar.policy.NetworkController;
import com.android.systemui.statusbar.policy.NetworkControllerImpl;
import com.android.systemui.statusbar.policy.SecurityController;
import com.android.systemui.tuner.TunerService;
import com.mediatek.systemui.PluginManager;
import com.mediatek.systemui.ext.ISystemUIStatusBarExt;
import com.mediatek.systemui.statusbar.util.FeatureOptions;
import java.util.ArrayList;
import java.util.List;

public class SignalClusterView extends LinearLayout implements NetworkController.SignalCallback, SecurityController.SecurityControllerCallback, TunerService.Tunable {
    static final boolean DEBUG = Log.isLoggable("SignalClusterView", 3);
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
    private final float mIconScaleFactor;
    private int mIconTint;
    private boolean mIsAirplaneMode;
    boolean mIsWfcEnable;
    private int mLastAirplaneIconId;
    private int mLastEthernetIconId;
    private int mLastWifiStrengthId;
    private final int mMobileDataIconStartPadding;
    LinearLayout mMobileSignalGroup;
    private final int mMobileSignalGroupEndPadding;
    NetworkControllerImpl mNC;
    ImageView mNoSims;
    View mNoSimsCombo;
    ImageView mNoSimsDark;
    private boolean mNoSimsVisible;
    private ArrayList<PhoneState> mPhoneStates;
    SecurityController mSC;
    private final int mSecondaryTelephonyPadding;
    private ISystemUIStatusBarExt mStatusBarExt;
    private final Rect mTintArea;
    ImageView mVpn;
    private boolean mVpnVisible;
    private final int mWideTypeIconStartPadding;
    ImageView mWifi;
    View mWifiAirplaneSpacer;
    ImageView mWifiDark;
    private String mWifiDescription;
    ViewGroup mWifiGroup;
    View mWifiSignalSpacer;
    private int mWifiStrengthId;
    private boolean mWifiVisible;

    public SignalClusterView(Context context) {
        this(context, null);
    }

    public SignalClusterView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SignalClusterView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mNoSimsVisible = false;
        this.mVpnVisible = false;
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
        Resources res = getResources();
        this.mMobileSignalGroupEndPadding = res.getDimensionPixelSize(R.dimen.mobile_signal_group_end_padding);
        this.mMobileDataIconStartPadding = res.getDimensionPixelSize(R.dimen.mobile_data_icon_start_padding);
        this.mWideTypeIconStartPadding = res.getDimensionPixelSize(R.dimen.wide_type_icon_start_padding);
        this.mSecondaryTelephonyPadding = res.getDimensionPixelSize(R.dimen.secondary_telephony_padding);
        this.mEndPadding = res.getDimensionPixelSize(R.dimen.signal_cluster_battery_padding);
        this.mEndPaddingNothingVisible = res.getDimensionPixelSize(R.dimen.no_signal_cluster_battery_padding);
        TypedValue typedValue = new TypedValue();
        res.getValue(R.dimen.status_bar_icon_scale_factor, typedValue, true);
        this.mIconScaleFactor = typedValue.getFloat();
        this.mStatusBarExt = PluginManager.getSystemUIStatusBarExt(context);
        this.mIsWfcEnable = SystemProperties.get("persist.mtk_wfc_support").equals("1");
    }

    @Override
    public void onTuningChanged(String key, String newValue) {
        if (!"icon_blacklist".equals(key)) {
            return;
        }
        ArraySet<String> blockList = StatusBarIconController.getIconBlacklist(newValue);
        boolean blockAirplane = blockList.contains("airplane");
        boolean blockMobile = blockList.contains("mobile");
        boolean blockWifi = blockList.contains("wifi");
        boolean blockEthernet = blockList.contains("ethernet");
        if (blockAirplane == this.mBlockAirplane && blockMobile == this.mBlockMobile && blockEthernet == this.mBlockEthernet && blockWifi == this.mBlockWifi) {
            return;
        }
        this.mBlockAirplane = blockAirplane;
        this.mBlockMobile = blockMobile;
        this.mBlockEthernet = blockEthernet;
        this.mBlockWifi = blockWifi;
        this.mNC.removeSignalCallback(this);
        this.mNC.addSignalCallback(this);
    }

    public void setNetworkController(NetworkControllerImpl nc) {
        if (DEBUG) {
            Log.d("SignalClusterView", "NetworkController=" + nc);
        }
        this.mNC = nc;
    }

    public void setSecurityController(SecurityController sc) {
        if (DEBUG) {
            Log.d("SignalClusterView", "SecurityController=" + sc);
        }
        this.mSC = sc;
        this.mSC.addCallback(this);
        this.mVpnVisible = this.mSC.isVpnEnabled();
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mVpn = (ImageView) findViewById(R.id.vpn);
        this.mEthernetGroup = (ViewGroup) findViewById(R.id.ethernet_combo);
        this.mEthernet = (ImageView) findViewById(R.id.ethernet);
        this.mEthernetDark = (ImageView) findViewById(R.id.ethernet_dark);
        this.mWifiGroup = (ViewGroup) findViewById(R.id.wifi_combo);
        this.mWifi = (ImageView) findViewById(R.id.wifi_signal);
        this.mWifiDark = (ImageView) findViewById(R.id.wifi_signal_dark);
        this.mAirplane = (ImageView) findViewById(R.id.airplane);
        this.mNoSims = (ImageView) findViewById(R.id.no_sims);
        this.mNoSimsDark = (ImageView) findViewById(R.id.no_sims_dark);
        this.mNoSimsCombo = findViewById(R.id.no_sims_combo);
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
        this.mNoSims.setImageDrawable(new ScalingDrawableWrapper(this.mNoSims.getDrawable(), this.mIconScaleFactor));
        this.mNoSimsDark.setImageDrawable(new ScalingDrawableWrapper(this.mNoSimsDark.getDrawable(), this.mIconScaleFactor));
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        for (PhoneState state : this.mPhoneStates) {
            this.mMobileSignalGroup.addView(state.mMobileGroup);
        }
        int endPadding = this.mMobileSignalGroup.getChildCount() > 0 ? this.mMobileSignalGroupEndPadding : 0;
        this.mMobileSignalGroup.setPaddingRelative(0, 0, endPadding, 0);
        TunerService.get(this.mContext).addTunable(this, "icon_blacklist");
        this.mStatusBarExt.setCustomizedNoSimView(this.mNoSims);
        this.mStatusBarExt.setCustomizedNoSimView(this.mNoSimsDark);
        this.mStatusBarExt.addSignalClusterCustomizedView(this.mContext, this, indexOfChild(findViewById(R.id.mobile_signal_group)));
        apply();
        applyIconTint();
        this.mNC.addSignalCallback(this);
    }

    @Override
    protected void onDetachedFromWindow() {
        this.mMobileSignalGroup.removeAllViews();
        TunerService.get(this.mContext).removeTunable(this);
        this.mSC.removeCallback(this);
        this.mNC.removeSignalCallback(this);
        super.onDetachedFromWindow();
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        applyIconTint();
    }

    @Override
    public void onStateChanged() {
        post(new Runnable() {
            @Override
            public void run() {
                SignalClusterView.this.mVpnVisible = SignalClusterView.this.mSC.isVpnEnabled();
                SignalClusterView.this.apply();
            }
        });
    }

    @Override
    public void setWifiIndicators(boolean enabled, NetworkController.IconState statusIcon, NetworkController.IconState qsIcon, boolean activityIn, boolean activityOut, String description) {
        boolean z = false;
        if (statusIcon.visible && !this.mBlockWifi) {
            z = true;
        }
        this.mWifiVisible = z;
        this.mWifiStrengthId = statusIcon.icon;
        this.mWifiDescription = statusIcon.contentDescription;
        apply();
    }

    @Override
    public void setMobileDataIndicators(NetworkController.IconState statusIcon, NetworkController.IconState qsIcon, int statusType, int networkType, int volteIcon, int qsType, boolean activityIn, boolean activityOut, String typeContentDescription, String description, boolean isWide, int subId) {
        PhoneState state = getState(subId);
        if (state == null) {
            return;
        }
        state.mMobileVisible = statusIcon.visible && !this.mBlockMobile;
        state.mMobileStrengthId = statusIcon.icon;
        state.mMobileTypeId = statusType;
        state.mMobileDescription = statusIcon.contentDescription;
        state.mMobileTypeDescription = typeContentDescription;
        if (statusType == 0) {
            isWide = false;
        }
        state.mIsMobileTypeIconWide = isWide;
        state.mNetworkIcon = networkType;
        state.mVolteIcon = volteIcon;
        state.mDataActivityIn = activityIn;
        state.mDataActivityOut = activityOut;
        apply();
    }

    @Override
    public void setEthernetIndicators(NetworkController.IconState state) {
        boolean z = false;
        if (state.visible && !this.mBlockEthernet) {
            z = true;
        }
        this.mEthernetVisible = z;
        this.mEthernetIconId = state.icon;
        this.mEthernetDescription = state.contentDescription;
        apply();
    }

    @Override
    public void setNoSims(boolean show) {
        boolean z = false;
        if (show && !this.mBlockMobile) {
            z = true;
        }
        this.mNoSimsVisible = z;
        apply();
    }

    @Override
    public void setSubs(List<SubscriptionInfo> subs) {
        if (hasCorrectSubs(subs)) {
            return;
        }
        for (PhoneState state : this.mPhoneStates) {
            if (state.mMobile != null) {
                state.maybeStopAnimatableDrawable(state.mMobile);
            }
            if (state.mMobileDark != null) {
                state.maybeStopAnimatableDrawable(state.mMobileDark);
            }
        }
        this.mPhoneStates.clear();
        if (this.mMobileSignalGroup != null) {
            this.mMobileSignalGroup.removeAllViews();
        }
        int n = subs.size();
        for (int i = 0; i < n; i++) {
            inflatePhoneState(subs.get(i).getSubscriptionId());
        }
        if (!isAttachedToWindow()) {
            return;
        }
        applyIconTint();
    }

    private boolean hasCorrectSubs(List<SubscriptionInfo> subs) {
        int N = subs.size();
        if (N != this.mPhoneStates.size()) {
            return false;
        }
        for (int i = 0; i < N; i++) {
            if (this.mPhoneStates.get(i).mSubId != subs.get(i).getSubscriptionId() || this.mStatusBarExt.checkIfSlotIdChanged(subs.get(i).getSubscriptionId(), subs.get(i).getSimSlotIndex())) {
                return false;
            }
        }
        return true;
    }

    private PhoneState getState(int subId) {
        for (PhoneState state : this.mPhoneStates) {
            if (state.mSubId == subId) {
                return state;
            }
        }
        Log.e("SignalClusterView", "Unexpected subscription " + subId);
        return null;
    }

    private PhoneState inflatePhoneState(int subId) {
        PhoneState state = new PhoneState(subId, this.mContext);
        if (this.mMobileSignalGroup != null) {
            this.mMobileSignalGroup.addView(state.mMobileGroup);
        }
        this.mPhoneStates.add(state);
        return state;
    }

    @Override
    public void setIsAirplaneMode(NetworkController.IconState icon) {
        boolean z = false;
        if (icon.visible && !this.mBlockAirplane) {
            z = true;
        }
        this.mIsAirplaneMode = z;
        this.mAirplaneIconId = icon.icon;
        this.mAirplaneContentDescription = icon.contentDescription;
        apply();
    }

    @Override
    public void setMobileDataEnabled(boolean enabled) {
    }

    public boolean dispatchPopulateAccessibilityEventInternal(AccessibilityEvent event) {
        if (this.mEthernetVisible && this.mEthernetGroup != null && this.mEthernetGroup.getContentDescription() != null) {
            event.getText().add(this.mEthernetGroup.getContentDescription());
        }
        if (this.mWifiVisible && this.mWifiGroup != null && this.mWifiGroup.getContentDescription() != null) {
            event.getText().add(this.mWifiGroup.getContentDescription());
        }
        for (PhoneState state : this.mPhoneStates) {
            state.populateAccessibilityEvent(event);
        }
        return super.dispatchPopulateAccessibilityEventInternal(event);
    }

    @Override
    public void onRtlPropertiesChanged(int layoutDirection) {
        super.onRtlPropertiesChanged(layoutDirection);
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
        for (PhoneState state : this.mPhoneStates) {
            if (state.mMobile != null) {
                state.maybeStopAnimatableDrawable(state.mMobile);
                state.mMobile.setImageDrawable(null);
                state.mLastMobileStrengthId = -1;
            }
            if (state.mMobileDark != null) {
                state.maybeStopAnimatableDrawable(state.mMobileDark);
                state.mMobileDark.setImageDrawable(null);
                state.mLastMobileStrengthId = -1;
            }
            if (state.mMobileType != null) {
                state.mMobileType.setImageDrawable(null);
                state.mLastMobileTypeId = -1;
            }
        }
        if (this.mAirplane != null) {
            this.mAirplane.setImageDrawable(null);
            this.mLastAirplaneIconId = -1;
        }
        apply();
    }

    @Override
    public boolean hasOverlappingRendering() {
        return false;
    }

    public void apply() {
        boolean anythingVisible = true;
        if (this.mWifiGroup == null) {
            return;
        }
        this.mVpn.setVisibility(this.mVpnVisible ? 0 : 8);
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
            this.mEthernetGroup.setVisibility(0);
        } else {
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
            this.mWifiGroup.setContentDescription(this.mWifiDescription);
            this.mWifiGroup.setVisibility(0);
        } else {
            this.mWifiGroup.setVisibility(8);
        }
        if (DEBUG) {
            Object[] objArr3 = new Object[2];
            objArr3[0] = this.mWifiVisible ? "VISIBLE" : "GONE";
            objArr3[1] = Integer.valueOf(this.mWifiStrengthId);
            Log.d("SignalClusterView", String.format("wifi: %s sig=%d", objArr3));
        }
        boolean anyMobileVisible = false;
        if (FeatureOptions.MTK_CTA_SET) {
            anyMobileVisible = true;
        }
        int firstMobileTypeId = 0;
        for (PhoneState state : this.mPhoneStates) {
            if (state.apply(anyMobileVisible) && !anyMobileVisible) {
                firstMobileTypeId = state.mMobileTypeId;
                anyMobileVisible = true;
            }
        }
        if (this.mIsAirplaneMode) {
            if (this.mLastAirplaneIconId != this.mAirplaneIconId) {
                setIconForView(this.mAirplane, this.mAirplaneIconId);
                this.mLastAirplaneIconId = this.mAirplaneIconId;
            }
            this.mAirplane.setContentDescription(this.mAirplaneContentDescription);
            this.mAirplane.setVisibility(0);
        } else {
            this.mAirplane.setVisibility(8);
        }
        if (this.mIsAirplaneMode && this.mWifiVisible) {
            this.mWifiAirplaneSpacer.setVisibility(0);
        } else {
            this.mWifiAirplaneSpacer.setVisibility(8);
        }
        if (((anyMobileVisible && firstMobileTypeId != 0) || this.mNoSimsVisible) && this.mWifiVisible) {
            this.mWifiSignalSpacer.setVisibility(0);
        } else {
            this.mWifiSignalSpacer.setVisibility(8);
        }
        this.mNoSimsCombo.setVisibility(this.mNoSimsVisible ? 0 : 8);
        this.mStatusBarExt.setCustomizedNoSimsVisible(this.mNoSimsVisible);
        this.mStatusBarExt.setCustomizedAirplaneView(this.mNoSimsCombo, this.mIsAirplaneMode);
        if (!this.mNoSimsVisible && !this.mWifiVisible && !this.mIsAirplaneMode && !anyMobileVisible && !this.mVpnVisible) {
            anythingVisible = this.mEthernetVisible;
        }
        setPaddingRelative(0, 0, anythingVisible ? this.mEndPadding : this.mEndPaddingNothingVisible, 0);
    }

    public void setIconForView(ImageView imageView, int iconId) {
        Drawable icon = imageView.getContext().getDrawable(iconId);
        if (this.mIconScaleFactor == 1.0f) {
            imageView.setImageDrawable(icon);
        } else {
            imageView.setImageDrawable(new ScalingDrawableWrapper(icon, this.mIconScaleFactor));
        }
    }

    public void setIconTint(int tint, float darkIntensity, Rect tintArea) {
        boolean changed = (tint == this.mIconTint && darkIntensity == this.mDarkIntensity && this.mTintArea.equals(tintArea)) ? false : true;
        this.mIconTint = tint;
        this.mDarkIntensity = darkIntensity;
        this.mTintArea.set(tintArea);
        if (!changed || !isAttachedToWindow()) {
            return;
        }
        applyIconTint();
    }

    private void applyIconTint() {
        setTint(this.mVpn, StatusBarIconController.getTint(this.mTintArea, this.mVpn, this.mIconTint));
        setTint(this.mAirplane, StatusBarIconController.getTint(this.mTintArea, this.mAirplane, this.mIconTint));
        applyDarkIntensity(StatusBarIconController.getDarkIntensity(this.mTintArea, this.mNoSims, this.mDarkIntensity), this.mNoSims, this.mNoSimsDark);
        applyDarkIntensity(StatusBarIconController.getDarkIntensity(this.mTintArea, this.mWifi, this.mDarkIntensity), this.mWifi, this.mWifiDark);
        applyDarkIntensity(StatusBarIconController.getDarkIntensity(this.mTintArea, this.mEthernet, this.mDarkIntensity), this.mEthernet, this.mEthernetDark);
        for (int i = 0; i < this.mPhoneStates.size(); i++) {
            this.mPhoneStates.get(i).setIconTint(this.mIconTint, this.mDarkIntensity, this.mTintArea);
        }
    }

    public void applyDarkIntensity(float darkIntensity, View lightIcon, View darkIcon) {
        lightIcon.setAlpha(1.0f - darkIntensity);
        darkIcon.setAlpha(darkIntensity);
    }

    public void setTint(ImageView v, int tint) {
        v.setImageTintList(ColorStateList.valueOf(tint));
    }

    private class PhoneState {
        private boolean mDataActivityIn;
        private boolean mDataActivityOut;
        private boolean mIsMobileTypeIconWide;
        private boolean mIsWfcCase;
        private ImageView mMobile;
        private ImageView mMobileDark;
        private String mMobileDescription;
        private ViewGroup mMobileGroup;
        private ImageView mMobileType;
        private String mMobileTypeDescription;
        private ImageView mNetworkType;
        private ISystemUIStatusBarExt mPhoneStateExt;
        private final int mSubId;
        private ImageView mVolteType;
        private boolean mMobileVisible = false;
        private int mMobileStrengthId = 0;
        private int mMobileTypeId = 0;
        private int mNetworkIcon = 0;
        private int mVolteIcon = 0;
        private int mLastMobileStrengthId = -1;
        private int mLastMobileTypeId = -1;

        public PhoneState(int subId, Context context) {
            ViewGroup root = (ViewGroup) LayoutInflater.from(context).inflate(R.layout.mobile_signal_group_ext, (ViewGroup) null);
            this.mPhoneStateExt = PluginManager.getSystemUIStatusBarExt(context);
            this.mPhoneStateExt.addCustomizedView(subId, context, root);
            setViews(root);
            this.mSubId = subId;
        }

        public void setViews(ViewGroup root) {
            this.mMobileGroup = root;
            this.mMobile = (ImageView) root.findViewById(R.id.mobile_signal);
            this.mMobileDark = (ImageView) root.findViewById(R.id.mobile_signal_dark);
            this.mMobileType = (ImageView) root.findViewById(R.id.mobile_type);
            this.mNetworkType = (ImageView) root.findViewById(R.id.network_type);
            this.mVolteType = (ImageView) root.findViewById(R.id.volte_indicator_ext);
        }

        public boolean apply(boolean isSecondaryIcon) {
            if (this.mMobileVisible && !SignalClusterView.this.mIsAirplaneMode) {
                if (this.mLastMobileStrengthId != this.mMobileStrengthId) {
                    updateAnimatableIcon(this.mMobile, this.mMobileStrengthId);
                    updateAnimatableIcon(this.mMobileDark, this.mMobileStrengthId);
                    this.mLastMobileStrengthId = this.mMobileStrengthId;
                }
                if (this.mLastMobileTypeId != this.mMobileTypeId) {
                    this.mMobileType.setImageResource(this.mMobileTypeId);
                    this.mLastMobileTypeId = this.mMobileTypeId;
                }
                this.mMobileGroup.setContentDescription(this.mMobileTypeDescription + " " + this.mMobileDescription);
                this.mMobileGroup.setVisibility(0);
                showViewInWfcCase();
            } else if (SignalClusterView.this.mIsAirplaneMode && SignalClusterView.this.mIsWfcEnable && this.mVolteIcon != 0) {
                this.mMobileGroup.setVisibility(0);
                hideViewInWfcCase();
            } else {
                this.mMobileGroup.setVisibility(8);
            }
            setCustomizeViewProperty();
            this.mMobileGroup.setPaddingRelative(isSecondaryIcon ? SignalClusterView.this.mSecondaryTelephonyPadding : 0, 0, 0, 0);
            this.mMobile.setPaddingRelative(this.mIsMobileTypeIconWide ? SignalClusterView.this.mWideTypeIconStartPadding : SignalClusterView.this.mMobileDataIconStartPadding, 0, 0, 0);
            this.mMobileDark.setPaddingRelative(this.mIsMobileTypeIconWide ? SignalClusterView.this.mWideTypeIconStartPadding : SignalClusterView.this.mMobileDataIconStartPadding, 0, 0, 0);
            if (SignalClusterView.DEBUG) {
                Object[] objArr = new Object[3];
                objArr[0] = this.mMobileVisible ? "VISIBLE" : "GONE";
                objArr[1] = Integer.valueOf(this.mMobileStrengthId);
                objArr[2] = Integer.valueOf(this.mMobileTypeId);
                Log.d("SignalClusterView", String.format("mobile: %s sig=%d typ=%d", objArr));
            }
            this.mMobileType.setVisibility(this.mMobileTypeId == 0 ? 8 : 0);
            setCustomizedOpViews();
            return this.mMobileVisible;
        }

        private void updateAnimatableIcon(ImageView view, int resId) {
            maybeStopAnimatableDrawable(view);
            SignalClusterView.this.setIconForView(view, resId);
            maybeStartAnimatableDrawable(view);
        }

        public void maybeStopAnimatableDrawable(ImageView view) {
            Drawable drawable = view.getDrawable();
            if (drawable instanceof ScalingDrawableWrapper) {
                drawable = ((ScalingDrawableWrapper) drawable).getDrawable();
            }
            if (!(drawable instanceof Animatable)) {
                return;
            }
            Animatable ad = (Animatable) drawable;
            if (!ad.isRunning()) {
                return;
            }
            ad.stop();
        }

        private void maybeStartAnimatableDrawable(ImageView view) {
            Drawable drawable = view.getDrawable();
            if (drawable instanceof ScalingDrawableWrapper) {
                drawable = ((ScalingDrawableWrapper) drawable).getDrawable();
            }
            if (!(drawable instanceof Animatable)) {
                return;
            }
            Animatable ad = (Animatable) drawable;
            if (ad instanceof AnimatedVectorDrawable) {
                ((AnimatedVectorDrawable) ad).forceAnimationOnUI();
            }
            if (ad.isRunning()) {
                return;
            }
            ad.start();
        }

        public void populateAccessibilityEvent(AccessibilityEvent event) {
            if (!this.mMobileVisible || this.mMobileGroup == null || this.mMobileGroup.getContentDescription() == null) {
                return;
            }
            event.getText().add(this.mMobileGroup.getContentDescription());
        }

        public void setIconTint(int tint, float darkIntensity, Rect tintArea) {
            SignalClusterView.this.applyDarkIntensity(StatusBarIconController.getDarkIntensity(tintArea, this.mMobile, darkIntensity), this.mMobile, this.mMobileDark);
            SignalClusterView.this.setTint(this.mMobileType, StatusBarIconController.getTint(tintArea, this.mMobileType, tint));
            SignalClusterView.this.setTint(this.mNetworkType, StatusBarIconController.getTint(tintArea, this.mNetworkType, tint));
            SignalClusterView.this.setTint(this.mVolteType, StatusBarIconController.getTint(tintArea, this.mVolteType, tint));
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
        }

        private void setNetworkIcon() {
            if (!FeatureOptions.MTK_CTA_SET) {
                return;
            }
            if (this.mNetworkIcon == 0) {
                this.mNetworkType.setVisibility(8);
            } else {
                this.mNetworkType.setImageResource(this.mNetworkIcon);
                this.mNetworkType.setVisibility(0);
            }
        }

        private void setCustomizedOpViews() {
            if (!this.mMobileVisible || SignalClusterView.this.mIsAirplaneMode) {
                return;
            }
            this.mPhoneStateExt.getServiceStateForCustomizedView(this.mSubId);
            this.mPhoneStateExt.setCustomizedAirplaneView(SignalClusterView.this.mNoSimsCombo, SignalClusterView.this.mIsAirplaneMode);
            this.mPhoneStateExt.setCustomizedNetworkTypeView(this.mSubId, this.mNetworkIcon, this.mNetworkType);
            this.mPhoneStateExt.setCustomizedDataTypeView(this.mSubId, this.mMobileTypeId, this.mDataActivityIn, this.mDataActivityOut);
            this.mPhoneStateExt.setCustomizedSignalStrengthView(this.mSubId, this.mMobileStrengthId, this.mMobile);
            this.mPhoneStateExt.setCustomizedSignalStrengthView(this.mSubId, this.mMobileStrengthId, this.mMobileDark);
            this.mPhoneStateExt.setCustomizedMobileTypeView(this.mSubId, this.mMobileType);
            this.mPhoneStateExt.setCustomizedView(this.mSubId);
        }

        private void hideViewInWfcCase() {
            Log.d("SignalClusterView", "hideViewInWfcCase, isWfcEnabled = " + SignalClusterView.this.mIsWfcEnable + " mSubId =" + this.mSubId);
            this.mMobile.setVisibility(8);
            this.mMobileDark.setVisibility(8);
            this.mMobileType.setVisibility(8);
            this.mNetworkType.setVisibility(8);
            this.mIsWfcCase = true;
        }

        private void showViewInWfcCase() {
            if (!this.mIsWfcCase) {
                return;
            }
            Log.d("SignalClusterView", "showViewInWfcCase: mSubId = " + this.mSubId);
            this.mMobile.setVisibility(0);
            this.mMobileDark.setVisibility(0);
            this.mMobileType.setVisibility(0);
            this.mNetworkType.setVisibility(0);
            this.mIsWfcCase = false;
        }
    }
}
