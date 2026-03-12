package com.android.systemui.statusbar;

import android.content.Context;
import android.telephony.SubscriptionInfo;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.widget.ImageView;
import android.widget.LinearLayout;
import com.android.systemui.R;
import com.android.systemui.statusbar.policy.NetworkControllerImpl;
import com.android.systemui.statusbar.policy.SecurityController;
import java.util.ArrayList;
import java.util.List;

public class SignalClusterView extends LinearLayout implements NetworkControllerImpl.SignalCluster, SecurityController.SecurityControllerCallback {
    static final boolean DEBUG = Log.isLoggable("SignalClusterView", 3);
    ImageView mAirplane;
    private int mAirplaneContentDescription;
    private int mAirplaneIconId;
    private int mEndPadding;
    private int mEndPaddingNothingVisible;
    private boolean mIsAirplaneMode;
    LinearLayout mMobileSignalGroup;
    NetworkControllerImpl mNC;
    ImageView mNoSims;
    private boolean mNoSimsVisible;
    private ArrayList<PhoneState> mPhoneStates;
    SecurityController mSC;
    private int mSecondaryTelephonyPadding;
    ImageView mVpn;
    private boolean mVpnVisible;
    private int mWideTypeIconStartPadding;
    ImageView mWifi;
    View mWifiAirplaneSpacer;
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
        this.mWifiVisible = false;
        this.mWifiStrengthId = 0;
        this.mIsAirplaneMode = false;
        this.mAirplaneIconId = 0;
        this.mPhoneStates = new ArrayList<>();
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
        this.mWideTypeIconStartPadding = getContext().getResources().getDimensionPixelSize(R.dimen.wide_type_icon_start_padding);
        this.mSecondaryTelephonyPadding = getContext().getResources().getDimensionPixelSize(R.dimen.secondary_telephony_padding);
        this.mEndPadding = getContext().getResources().getDimensionPixelSize(R.dimen.signal_cluster_battery_padding);
        this.mEndPaddingNothingVisible = getContext().getResources().getDimensionPixelSize(R.dimen.no_signal_cluster_battery_padding);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        this.mVpn = (ImageView) findViewById(R.id.vpn);
        this.mWifiGroup = (ViewGroup) findViewById(R.id.wifi_combo);
        this.mWifi = (ImageView) findViewById(R.id.wifi_signal);
        this.mAirplane = (ImageView) findViewById(R.id.airplane);
        this.mNoSims = (ImageView) findViewById(R.id.no_sims);
        this.mWifiAirplaneSpacer = findViewById(R.id.wifi_airplane_spacer);
        this.mWifiSignalSpacer = findViewById(R.id.wifi_signal_spacer);
        this.mMobileSignalGroup = (LinearLayout) findViewById(R.id.mobile_signal_group);
        for (PhoneState state : this.mPhoneStates) {
            this.mMobileSignalGroup.addView(state.mMobileGroup);
        }
        apply();
    }

    @Override
    protected void onDetachedFromWindow() {
        this.mVpn = null;
        this.mWifiGroup = null;
        this.mWifi = null;
        this.mAirplane = null;
        this.mMobileSignalGroup.removeAllViews();
        this.mMobileSignalGroup = null;
        super.onDetachedFromWindow();
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
    public void setWifiIndicators(boolean visible, int strengthIcon, String contentDescription) {
        this.mWifiVisible = visible;
        this.mWifiStrengthId = strengthIcon;
        this.mWifiDescription = contentDescription;
        apply();
    }

    @Override
    public void setMobileDataIndicators(boolean visible, int strengthIcon, int typeIcon, String contentDescription, String typeContentDescription, boolean isTypeIconWide, int subId) {
        PhoneState state = getOrInflateState(subId);
        state.mMobileVisible = visible;
        state.mMobileStrengthId = strengthIcon;
        state.mMobileTypeId = typeIcon;
        state.mMobileDescription = contentDescription;
        state.mMobileTypeDescription = typeContentDescription;
        state.mIsMobileTypeIconWide = isTypeIconWide;
        apply();
    }

    @Override
    public void setNoSims(boolean show) {
        this.mNoSimsVisible = show;
    }

    @Override
    public void setSubs(List<SubscriptionInfo> subs) {
        this.mPhoneStates.clear();
        if (this.mMobileSignalGroup != null) {
            this.mMobileSignalGroup.removeAllViews();
        }
        int n = subs.size();
        for (int i = 0; i < n; i++) {
            inflatePhoneState(subs.get(i).getSubscriptionId());
        }
    }

    private PhoneState getOrInflateState(int subId) {
        for (PhoneState state : this.mPhoneStates) {
            if (state.mSubId == subId) {
                return state;
            }
        }
        return inflatePhoneState(subId);
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
    public void setIsAirplaneMode(boolean is, int airplaneIconId, int contentDescription) {
        this.mIsAirplaneMode = is;
        this.mAirplaneIconId = airplaneIconId;
        this.mAirplaneContentDescription = contentDescription;
        apply();
    }

    @Override
    public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
        if (this.mWifiVisible && this.mWifiGroup != null && this.mWifiGroup.getContentDescription() != null) {
            event.getText().add(this.mWifiGroup.getContentDescription());
        }
        for (PhoneState state : this.mPhoneStates) {
            state.populateAccessibilityEvent(event);
        }
        return super.dispatchPopulateAccessibilityEvent(event);
    }

    @Override
    public void onRtlPropertiesChanged(int layoutDirection) {
        super.onRtlPropertiesChanged(layoutDirection);
        if (this.mWifi != null) {
            this.mWifi.setImageDrawable(null);
        }
        for (PhoneState state : this.mPhoneStates) {
            if (state.mMobile != null) {
                state.mMobile.setImageDrawable(null);
            }
            if (state.mMobileType != null) {
                state.mMobileType.setImageDrawable(null);
            }
        }
        if (this.mAirplane != null) {
            this.mAirplane.setImageDrawable(null);
        }
        apply();
    }

    @Override
    public boolean hasOverlappingRendering() {
        return false;
    }

    public void apply() {
        boolean anythingVisible = true;
        if (this.mWifiGroup != null) {
            this.mVpn.setVisibility(this.mVpnVisible ? 0 : 8);
            if (DEBUG) {
                Object[] objArr = new Object[1];
                objArr[0] = this.mVpnVisible ? "VISIBLE" : "GONE";
                Log.d("SignalClusterView", String.format("vpn: %s", objArr));
            }
            if (this.mWifiVisible) {
                this.mWifi.setImageResource(this.mWifiStrengthId);
                this.mWifiGroup.setContentDescription(this.mWifiDescription);
                this.mWifiGroup.setVisibility(0);
            } else {
                this.mWifiGroup.setVisibility(8);
            }
            if (DEBUG) {
                Object[] objArr2 = new Object[2];
                objArr2[0] = this.mWifiVisible ? "VISIBLE" : "GONE";
                objArr2[1] = Integer.valueOf(this.mWifiStrengthId);
                Log.d("SignalClusterView", String.format("wifi: %s sig=%d", objArr2));
            }
            boolean anyMobileVisible = false;
            int firstMobileTypeId = 0;
            for (PhoneState state : this.mPhoneStates) {
                if (state.apply(anyMobileVisible) && !anyMobileVisible) {
                    firstMobileTypeId = state.mMobileTypeId;
                    anyMobileVisible = true;
                }
            }
            if (this.mIsAirplaneMode) {
                this.mAirplane.setImageResource(this.mAirplaneIconId);
                this.mAirplane.setContentDescription(this.mAirplaneContentDescription != 0 ? this.mContext.getString(this.mAirplaneContentDescription) : null);
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
            this.mNoSims.setVisibility(this.mNoSimsVisible ? 0 : 8);
            if (!this.mNoSimsVisible && !this.mWifiVisible && !this.mIsAirplaneMode && !anyMobileVisible && !this.mVpnVisible) {
                anythingVisible = false;
            }
            setPaddingRelative(0, 0, anythingVisible ? this.mEndPadding : this.mEndPaddingNothingVisible, 0);
        }
    }

    private class PhoneState {
        private boolean mIsMobileTypeIconWide;
        private ImageView mMobile;
        private String mMobileDescription;
        private ViewGroup mMobileGroup;
        private ImageView mMobileType;
        private String mMobileTypeDescription;
        private final int mSubId;
        private boolean mMobileVisible = false;
        private int mMobileStrengthId = 0;
        private int mMobileTypeId = 0;

        public PhoneState(int subId, Context context) {
            ViewGroup root = (ViewGroup) LayoutInflater.from(context).inflate(R.layout.mobile_signal_group, (ViewGroup) null);
            setViews(root);
            this.mSubId = subId;
        }

        public void setViews(ViewGroup root) {
            this.mMobileGroup = root;
            this.mMobile = (ImageView) root.findViewById(R.id.mobile_signal);
            this.mMobileType = (ImageView) root.findViewById(R.id.mobile_type);
        }

        public boolean apply(boolean isSecondaryIcon) {
            if (this.mMobileVisible && !SignalClusterView.this.mIsAirplaneMode) {
                this.mMobile.setImageResource(this.mMobileStrengthId);
                this.mMobileType.setImageResource(this.mMobileTypeId);
                this.mMobileGroup.setContentDescription(this.mMobileTypeDescription + " " + this.mMobileDescription);
                this.mMobileGroup.setVisibility(0);
            } else {
                this.mMobileGroup.setVisibility(8);
            }
            this.mMobileGroup.setPaddingRelative(isSecondaryIcon ? SignalClusterView.this.mSecondaryTelephonyPadding : 0, 0, 0, 0);
            this.mMobile.setPaddingRelative(this.mIsMobileTypeIconWide ? SignalClusterView.this.mWideTypeIconStartPadding : 0, 0, 0, 0);
            if (SignalClusterView.DEBUG) {
                Object[] objArr = new Object[3];
                objArr[0] = this.mMobileVisible ? "VISIBLE" : "GONE";
                objArr[1] = Integer.valueOf(this.mMobileStrengthId);
                objArr[2] = Integer.valueOf(this.mMobileTypeId);
                Log.d("SignalClusterView", String.format("mobile: %s sig=%d typ=%d", objArr));
            }
            this.mMobileType.setVisibility(this.mMobileTypeId == 0 ? 8 : 0);
            return this.mMobileVisible;
        }

        public void populateAccessibilityEvent(AccessibilityEvent event) {
            if (this.mMobileVisible && this.mMobileGroup != null && this.mMobileGroup.getContentDescription() != null) {
                event.getText().add(this.mMobileGroup.getContentDescription());
            }
        }
    }
}
