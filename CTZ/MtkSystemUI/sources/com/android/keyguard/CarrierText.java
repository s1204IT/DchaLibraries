package com.android.keyguard;

import android.content.Context;
import android.content.res.TypedArray;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.telephony.ServiceState;
import android.telephony.SubscriptionInfo;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.text.method.SingleLineTransformationMethod;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import com.android.internal.telephony.IccCardConstants;
import com.android.settingslib.WirelessUtils;
import com.mediatek.keyguard.ext.ICarrierTextExt;
import com.mediatek.keyguard.ext.IOperatorSIMString;
import com.mediatek.keyguard.ext.OpKeyguardCustomizationFactoryBase;
import java.util.List;
import java.util.Locale;

public class CarrierText extends TextView {
    private static final boolean DEBUG = KeyguardConstants.DEBUG;
    private static CharSequence mSeparator;
    private KeyguardUpdateMonitorCallback mCallback;
    private String[] mCarrier;
    private boolean[] mCarrierNeedToShow;
    private ICarrierTextExt mCarrierTextExt;
    private Context mContext;
    private int mFlags;
    private IOperatorSIMString mIOperatorSIMString;
    private final boolean mIsEmergencyCallCapable;
    private KeyguardUpdateMonitor mKeyguardUpdateMonitor;
    private int mNumOfPhone;
    private boolean[] mSimErrorState;
    private StatusMode[] mStatusMode;
    private WifiManager mWifiManager;

    private enum StatusMode {
        Normal,
        NetworkLocked,
        SimMissing,
        SimMissingLocked,
        SimPukLocked,
        SimLocked,
        SimPermDisabled,
        SimNotReady,
        SimIoError,
        SimUnknown,
        NetworkSearching
    }

    public void setDisplayFlags(int i) {
        this.mFlags = i;
    }

    private void initMembers() {
        this.mNumOfPhone = KeyguardUtils.getNumOfPhone();
        this.mCarrier = new String[this.mNumOfPhone];
        this.mCarrierNeedToShow = new boolean[this.mNumOfPhone];
        this.mStatusMode = new StatusMode[this.mNumOfPhone];
        for (int i = 0; i < this.mNumOfPhone; i++) {
            this.mStatusMode[i] = StatusMode.Normal;
        }
    }

    public CarrierText(Context context) {
        this(context, null);
    }

    public CarrierText(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mSimErrorState = new boolean[TelephonyManager.getDefault().getPhoneCount()];
        this.mCallback = new KeyguardUpdateMonitorCallback() {
            @Override
            public void onRefreshCarrierInfo() {
                CarrierText.this.updateCarrierText();
            }

            @Override
            public void onFinishedGoingToSleep(int i) {
                CarrierText.this.setSelected(false);
            }

            @Override
            public void onStartedWakingUp() {
                CarrierText.this.setSelected(true);
            }

            @Override
            public void onSimStateChangedUsingPhoneId(int i, IccCardConstants.State state) {
                CarrierText.this.updateCarrierText();
            }
        };
        this.mContext = context;
        this.mIsEmergencyCallCapable = context.getResources().getBoolean(android.R.^attr-private.popupPromptView);
        this.mKeyguardUpdateMonitor = KeyguardUpdateMonitor.getInstance(this.mContext);
        initMembers();
        this.mIOperatorSIMString = OpKeyguardCustomizationFactoryBase.getOpFactory(this.mContext).makeOperatorSIMString();
        this.mCarrierTextExt = OpKeyguardCustomizationFactoryBase.getOpFactory(this.mContext).makeCarrierText();
        TypedArray typedArrayObtainStyledAttributes = context.getTheme().obtainStyledAttributes(attributeSet, R.styleable.CarrierText, 0, 0);
        try {
            boolean z = typedArrayObtainStyledAttributes.getBoolean(0, false);
            typedArrayObtainStyledAttributes.recycle();
            setTransformationMethod(new CarrierTextTransformationMethod(this.mContext, z));
            this.mWifiManager = (WifiManager) context.getSystemService("wifi");
        } catch (Throwable th) {
            typedArrayObtainStyledAttributes.recycle();
            throw th;
        }
    }

    protected void updateCarrierText() {
        String string;
        CharSequence carrierName;
        ServiceState serviceState;
        if (isWifiOnlyDevice()) {
            Log.d("CarrierText", "updateCarrierText() - WifiOnly deivce, not show carrier text.");
            setText("");
            return;
        }
        boolean zShowOrHideCarrier = showOrHideCarrier();
        boolean z = false;
        int i = 0;
        while (true) {
            string = null;
            if (i >= this.mNumOfPhone) {
                break;
            }
            int subIdUsingPhoneId = KeyguardUtils.getSubIdUsingPhoneId(i);
            IccCardConstants.State simStateOfPhoneId = this.mKeyguardUpdateMonitor.getSimStateOfPhoneId(i);
            SubscriptionInfo subscriptionInfoForSubId = this.mKeyguardUpdateMonitor.getSubscriptionInfoForSubId(subIdUsingPhoneId);
            if (subscriptionInfoForSubId != null) {
                carrierName = subscriptionInfoForSubId.getCarrierName();
            } else {
                carrierName = null;
            }
            if (simStateOfPhoneId == IccCardConstants.State.READY && (serviceState = this.mKeyguardUpdateMonitor.mServiceStates.get(Integer.valueOf(subIdUsingPhoneId))) != null && serviceState.getDataRegState() == 0 && (serviceState.getRilDataRadioTechnology() != 18 || (this.mWifiManager.isWifiEnabled() && this.mWifiManager.getConnectionInfo() != null && this.mWifiManager.getConnectionInfo().getBSSID() != null))) {
                if (DEBUG) {
                    Log.d("CarrierText", "SIM ready and in service: subId=" + subIdUsingPhoneId + ", ss=" + serviceState);
                }
                z = true;
            }
            boolean z2 = z;
            CharSequence carrierTextForSimState = getCarrierTextForSimState(i, simStateOfPhoneId, carrierName, null, null);
            if (carrierTextForSimState != null) {
                String operatorSIMString = this.mIOperatorSIMString.getOperatorSIMString(carrierTextForSimState.toString(), i, IOperatorSIMString.SIMChangedTag.DELSIM, this.mContext);
                if (operatorSIMString != null) {
                    carrierTextForSimState = this.mCarrierTextExt.customizeCarrierTextCapital(operatorSIMString.toString()).toString();
                } else {
                    carrierTextForSimState = null;
                }
            }
            if (carrierTextForSimState != null) {
                this.mCarrier[i] = carrierTextForSimState.toString();
            } else {
                this.mCarrier[i] = null;
            }
            i++;
            z = z2;
        }
        String strCustomizeCarrierTextDivider = this.mCarrierTextExt.customizeCarrierTextDivider(mSeparator.toString());
        for (int i2 = 0; i2 < this.mNumOfPhone; i2++) {
            if (this.mCarrierNeedToShow[i2] && this.mCarrier[i2] != null) {
                string = string == null ? this.mCarrier[i2] : string + strCustomizeCarrierTextDivider + this.mCarrier[i2];
            }
        }
        if (!z && WirelessUtils.isAirplaneModeOn(this.mContext)) {
            string = getContext().getString(com.android.systemui.R.string.airplane_mode);
        }
        Log.d("CarrierText", "updateCarrierText() - after combination, carrierFinalContent = " + string + ", allSimsMissing = " + zShowOrHideCarrier);
        setText(string);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mSeparator = getResources().getString(android.R.string.config_wearMediaSessionsPackage);
        setSelected(KeyguardUpdateMonitor.getInstance(this.mContext).isDeviceInteractive());
        setLayerType(2, null);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (ConnectivityManager.from(this.mContext).isNetworkSupported(0)) {
            this.mKeyguardUpdateMonitor = KeyguardUpdateMonitor.getInstance(this.mContext);
            this.mKeyguardUpdateMonitor.registerCallback(this.mCallback);
        } else {
            this.mKeyguardUpdateMonitor = null;
            setText("");
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (this.mKeyguardUpdateMonitor != null) {
            this.mKeyguardUpdateMonitor.removeCallback(this.mCallback);
        }
    }

    @Override
    protected void onVisibilityChanged(View view, int i) {
        super.onVisibilityChanged(view, i);
        if (i == 0) {
            setEllipsize(TextUtils.TruncateAt.MARQUEE);
        } else {
            setEllipsize(TextUtils.TruncateAt.END);
        }
    }

    private CharSequence getCarrierTextForSimState(int i, IccCardConstants.State state, CharSequence charSequence, CharSequence charSequence2, CharSequence charSequence3) {
        CharSequence charSequenceMakeCarrierStringOnEmergencyCapable;
        switch (getStatusForIccState(state)) {
            case Normal:
            default:
                charSequenceMakeCarrierStringOnEmergencyCapable = charSequence;
                break;
            case SimNotReady:
            case SimMissingLocked:
                charSequenceMakeCarrierStringOnEmergencyCapable = null;
                break;
            case NetworkLocked:
                charSequenceMakeCarrierStringOnEmergencyCapable = makeCarrierStringOnEmergencyCapable(this.mContext.getText(com.android.systemui.R.string.keyguard_network_locked_message), charSequence, charSequence2, charSequence3);
                break;
            case SimMissing:
                CharSequence text = getContext().getText(com.android.systemui.R.string.keyguard_missing_sim_message_short);
                charSequenceMakeCarrierStringOnEmergencyCapable = this.mCarrierTextExt.customizeCarrierTextWhenSimMissing(this.mCarrierTextExt.customizeCarrierText(makeCarrierStringOnEmergencyCapable(text, charSequence, charSequence2, charSequence3), text, i));
                break;
            case SimPermDisabled:
                charSequenceMakeCarrierStringOnEmergencyCapable = makeCarrierStringOnEmergencyCapable(getContext().getText(com.android.systemui.R.string.keyguard_permanent_disabled_sim_message_short), charSequence, charSequence2, charSequence3);
                break;
            case SimLocked:
                charSequenceMakeCarrierStringOnEmergencyCapable = makeCarrierStringOnEmergencyCapable(getContext().getText(com.android.systemui.R.string.keyguard_sim_locked_message), charSequence, charSequence2, charSequence3);
                break;
            case SimPukLocked:
                charSequenceMakeCarrierStringOnEmergencyCapable = makeCarrierStringOnEmergencyCapable(getContext().getText(com.android.systemui.R.string.keyguard_sim_puk_locked_message), charSequence, charSequence2, charSequence3);
                break;
            case SimIoError:
                charSequenceMakeCarrierStringOnEmergencyCapable = makeCarrierStringOnEmergencyCapable(getContext().getText(com.android.systemui.R.string.keyguard_sim_error_message_short), charSequence, charSequence2, charSequence3);
                break;
        }
        if (charSequenceMakeCarrierStringOnEmergencyCapable != null) {
            charSequenceMakeCarrierStringOnEmergencyCapable = this.mCarrierTextExt.customizeCarrierTextWhenCardTypeLocked(charSequenceMakeCarrierStringOnEmergencyCapable, this.mContext, i, false).toString();
        }
        if (DEBUG) {
            Log.d("CarrierText", "getCarrierTextForSimState simState=" + state + " carrierName(from Sub)=" + ((Object) charSequence) + " HNB=" + ((Object) charSequence2) + " CSG=" + ((Object) charSequence3) + " carrierText=" + ((Object) charSequenceMakeCarrierStringOnEmergencyCapable));
        }
        return charSequenceMakeCarrierStringOnEmergencyCapable;
    }

    private CharSequence makeCarrierStringOnEmergencyCapable(CharSequence charSequence, CharSequence charSequence2, CharSequence charSequence3, CharSequence charSequence4) {
        if (!TextUtils.isEmpty(charSequence2)) {
            charSequence2 = appendCsgInfo(charSequence2, charSequence3, charSequence4);
        }
        if (this.mIsEmergencyCallCapable) {
            return concatenate(charSequence, charSequence2);
        }
        return charSequence;
    }

    private StatusMode getStatusForIccState(IccCardConstants.State state) {
        if (state == null) {
            return StatusMode.SimUnknown;
        }
        if (!KeyguardUpdateMonitor.getInstance(this.mContext).isDeviceProvisioned() && (state == IccCardConstants.State.ABSENT || state == IccCardConstants.State.PERM_DISABLED)) {
            return StatusMode.SimMissingLocked;
        }
        switch (AnonymousClass2.$SwitchMap$com$android$internal$telephony$IccCardConstants$State[state.ordinal()]) {
            case 1:
                return StatusMode.SimMissing;
            case 2:
                return StatusMode.NetworkLocked;
            case 3:
                return StatusMode.SimNotReady;
            case 4:
                return StatusMode.SimLocked;
            case 5:
                return StatusMode.SimPukLocked;
            case 6:
                return StatusMode.Normal;
            case 7:
                return StatusMode.SimPermDisabled;
            case 8:
                return StatusMode.SimUnknown;
            case 9:
                return StatusMode.SimIoError;
            default:
                return StatusMode.SimMissing;
        }
    }

    static class AnonymousClass2 {
        static final int[] $SwitchMap$com$android$internal$telephony$IccCardConstants$State = new int[IccCardConstants.State.values().length];

        static {
            try {
                $SwitchMap$com$android$internal$telephony$IccCardConstants$State[IccCardConstants.State.ABSENT.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$IccCardConstants$State[IccCardConstants.State.NETWORK_LOCKED.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$IccCardConstants$State[IccCardConstants.State.NOT_READY.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$IccCardConstants$State[IccCardConstants.State.PIN_REQUIRED.ordinal()] = 4;
            } catch (NoSuchFieldError e4) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$IccCardConstants$State[IccCardConstants.State.PUK_REQUIRED.ordinal()] = 5;
            } catch (NoSuchFieldError e5) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$IccCardConstants$State[IccCardConstants.State.READY.ordinal()] = 6;
            } catch (NoSuchFieldError e6) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$IccCardConstants$State[IccCardConstants.State.PERM_DISABLED.ordinal()] = 7;
            } catch (NoSuchFieldError e7) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$IccCardConstants$State[IccCardConstants.State.UNKNOWN.ordinal()] = 8;
            } catch (NoSuchFieldError e8) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$IccCardConstants$State[IccCardConstants.State.CARD_IO_ERROR.ordinal()] = 9;
            } catch (NoSuchFieldError e9) {
            }
            $SwitchMap$com$android$keyguard$CarrierText$StatusMode = new int[StatusMode.values().length];
            try {
                $SwitchMap$com$android$keyguard$CarrierText$StatusMode[StatusMode.Normal.ordinal()] = 1;
            } catch (NoSuchFieldError e10) {
            }
            try {
                $SwitchMap$com$android$keyguard$CarrierText$StatusMode[StatusMode.SimNotReady.ordinal()] = 2;
            } catch (NoSuchFieldError e11) {
            }
            try {
                $SwitchMap$com$android$keyguard$CarrierText$StatusMode[StatusMode.NetworkLocked.ordinal()] = 3;
            } catch (NoSuchFieldError e12) {
            }
            try {
                $SwitchMap$com$android$keyguard$CarrierText$StatusMode[StatusMode.SimMissing.ordinal()] = 4;
            } catch (NoSuchFieldError e13) {
            }
            try {
                $SwitchMap$com$android$keyguard$CarrierText$StatusMode[StatusMode.SimPermDisabled.ordinal()] = 5;
            } catch (NoSuchFieldError e14) {
            }
            try {
                $SwitchMap$com$android$keyguard$CarrierText$StatusMode[StatusMode.SimMissingLocked.ordinal()] = 6;
            } catch (NoSuchFieldError e15) {
            }
            try {
                $SwitchMap$com$android$keyguard$CarrierText$StatusMode[StatusMode.SimLocked.ordinal()] = 7;
            } catch (NoSuchFieldError e16) {
            }
            try {
                $SwitchMap$com$android$keyguard$CarrierText$StatusMode[StatusMode.SimPukLocked.ordinal()] = 8;
            } catch (NoSuchFieldError e17) {
            }
            try {
                $SwitchMap$com$android$keyguard$CarrierText$StatusMode[StatusMode.SimIoError.ordinal()] = 9;
            } catch (NoSuchFieldError e18) {
            }
        }
    }

    private static CharSequence concatenate(CharSequence charSequence, CharSequence charSequence2) {
        boolean z = !TextUtils.isEmpty(charSequence);
        boolean z2 = !TextUtils.isEmpty(charSequence2);
        if (z && z2) {
            StringBuilder sb = new StringBuilder();
            sb.append(charSequence);
            sb.append(mSeparator);
            sb.append(charSequence2);
            return sb.toString();
        }
        if (z) {
            return charSequence;
        }
        if (z2) {
            return charSequence2;
        }
        return "";
    }

    private class CarrierTextTransformationMethod extends SingleLineTransformationMethod {
        private final boolean mAllCaps;
        private final Locale mLocale;

        public CarrierTextTransformationMethod(Context context, boolean z) {
            this.mLocale = context.getResources().getConfiguration().locale;
            this.mAllCaps = z;
        }

        @Override
        public CharSequence getTransformation(CharSequence charSequence, View view) {
            CharSequence transformation = super.getTransformation(charSequence, view);
            if (this.mAllCaps && transformation != null) {
                return transformation.toString().toUpperCase(this.mLocale);
            }
            return transformation;
        }
    }

    private boolean isWifiOnlyDevice() {
        return !((ConnectivityManager) getContext().getSystemService("connectivity")).isNetworkSupported(0);
    }

    private boolean showOrHideCarrier() {
        int simSlotIndex;
        int i = 0;
        for (int i2 = 0; i2 < this.mNumOfPhone; i2++) {
            StatusMode statusForIccState = getStatusForIccState(this.mKeyguardUpdateMonitor.getSimStateOfPhoneId(i2));
            if (this.mCarrierTextExt.showCarrierTextWhenSimMissing(statusForIccState == StatusMode.SimMissing || statusForIccState == StatusMode.SimMissingLocked || statusForIccState == StatusMode.SimUnknown, i2)) {
                this.mCarrierNeedToShow[i2] = false;
            } else {
                this.mCarrierNeedToShow[i2] = true;
                i++;
            }
        }
        List<SubscriptionInfo> subscriptionInfo = this.mKeyguardUpdateMonitor.getSubscriptionInfo(false);
        if (i == 0) {
            String string = this.mKeyguardUpdateMonitor.getDefaultPlmn().toString();
            int i3 = 0;
            while (true) {
                if (i3 < subscriptionInfo.size()) {
                    SubscriptionInfo subscriptionInfo2 = subscriptionInfo.get(i3);
                    subscriptionInfo2.getSubscriptionId();
                    simSlotIndex = subscriptionInfo2.getSimSlotIndex();
                    CharSequence carrierName = subscriptionInfo2.getCarrierName();
                    if (carrierName != null && !string.contentEquals(carrierName)) {
                        break;
                    }
                    i3++;
                } else {
                    simSlotIndex = 0;
                    break;
                }
            }
            this.mCarrierNeedToShow[simSlotIndex] = true;
        }
        return i == 0;
    }

    private CharSequence appendCsgInfo(CharSequence charSequence, CharSequence charSequence2, CharSequence charSequence3) {
        if (!TextUtils.isEmpty(charSequence2)) {
            return concatenate(charSequence, charSequence2);
        }
        if (!TextUtils.isEmpty(charSequence3)) {
            return concatenate(charSequence, charSequence3);
        }
        return charSequence;
    }
}
