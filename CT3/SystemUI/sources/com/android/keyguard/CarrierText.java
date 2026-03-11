package com.android.keyguard;

import android.R;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.TypedArray;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.telephony.ServiceState;
import android.telephony.SubscriptionInfo;
import android.text.TextUtils;
import android.text.method.SingleLineTransformationMethod;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import com.android.internal.telephony.IccCardConstants;
import com.android.settingslib.WirelessUtils;
import com.mediatek.keyguard.Plugin.KeyguardPluginFactory;
import com.mediatek.keyguard.ext.ICarrierTextExt;
import com.mediatek.keyguard.ext.IOperatorSIMString;
import java.util.List;
import java.util.Locale;

public class CarrierText extends TextView {

    private static final int[] f1x8dbfd0b5 = null;

    private static final int[] f2comandroidkeyguardCarrierText$StatusModeSwitchesValues = null;
    private static CharSequence mSeparator;
    private final BroadcastReceiver mBroadcastReceiver;
    private KeyguardUpdateMonitorCallback mCallback;
    private String[] mCarrier;
    private boolean[] mCarrierNeedToShow;
    private ICarrierTextExt mCarrierTextExt;
    private Context mContext;
    private IOperatorSIMString mIOperatorSIMString;
    private final boolean mIsEmergencyCallCapable;
    private KeyguardUpdateMonitor mKeyguardUpdateMonitor;
    private int mNumOfPhone;
    private StatusMode[] mStatusMode;
    private KeyguardUpdateMonitor mUpdateMonitor;
    private WifiManager mWifiManager;

    private static int[] m430xf663cf59() {
        if (f1x8dbfd0b5 != null) {
            return f1x8dbfd0b5;
        }
        int[] iArr = new int[IccCardConstants.State.values().length];
        try {
            iArr[IccCardConstants.State.ABSENT.ordinal()] = 1;
        } catch (NoSuchFieldError e) {
        }
        try {
            iArr[IccCardConstants.State.CARD_IO_ERROR.ordinal()] = 17;
        } catch (NoSuchFieldError e2) {
        }
        try {
            iArr[IccCardConstants.State.NETWORK_LOCKED.ordinal()] = 2;
        } catch (NoSuchFieldError e3) {
        }
        try {
            iArr[IccCardConstants.State.NOT_READY.ordinal()] = 3;
        } catch (NoSuchFieldError e4) {
        }
        try {
            iArr[IccCardConstants.State.PERM_DISABLED.ordinal()] = 4;
        } catch (NoSuchFieldError e5) {
        }
        try {
            iArr[IccCardConstants.State.PIN_REQUIRED.ordinal()] = 5;
        } catch (NoSuchFieldError e6) {
        }
        try {
            iArr[IccCardConstants.State.PUK_REQUIRED.ordinal()] = 6;
        } catch (NoSuchFieldError e7) {
        }
        try {
            iArr[IccCardConstants.State.READY.ordinal()] = 7;
        } catch (NoSuchFieldError e8) {
        }
        try {
            iArr[IccCardConstants.State.UNKNOWN.ordinal()] = 8;
        } catch (NoSuchFieldError e9) {
        }
        f1x8dbfd0b5 = iArr;
        return iArr;
    }

    private static int[] m431getcomandroidkeyguardCarrierText$StatusModeSwitchesValues() {
        if (f2comandroidkeyguardCarrierText$StatusModeSwitchesValues != null) {
            return f2comandroidkeyguardCarrierText$StatusModeSwitchesValues;
        }
        int[] iArr = new int[StatusMode.valuesCustom().length];
        try {
            iArr[StatusMode.NetworkLocked.ordinal()] = 1;
        } catch (NoSuchFieldError e) {
        }
        try {
            iArr[StatusMode.NetworkSearching.ordinal()] = 17;
        } catch (NoSuchFieldError e2) {
        }
        try {
            iArr[StatusMode.Normal.ordinal()] = 2;
        } catch (NoSuchFieldError e3) {
        }
        try {
            iArr[StatusMode.SimLocked.ordinal()] = 3;
        } catch (NoSuchFieldError e4) {
        }
        try {
            iArr[StatusMode.SimMissing.ordinal()] = 4;
        } catch (NoSuchFieldError e5) {
        }
        try {
            iArr[StatusMode.SimMissingLocked.ordinal()] = 5;
        } catch (NoSuchFieldError e6) {
        }
        try {
            iArr[StatusMode.SimNotReady.ordinal()] = 6;
        } catch (NoSuchFieldError e7) {
        }
        try {
            iArr[StatusMode.SimPermDisabled.ordinal()] = 7;
        } catch (NoSuchFieldError e8) {
        }
        try {
            iArr[StatusMode.SimPukLocked.ordinal()] = 8;
        } catch (NoSuchFieldError e9) {
        }
        try {
            iArr[StatusMode.SimUnknown.ordinal()] = 18;
        } catch (NoSuchFieldError e10) {
        }
        f2comandroidkeyguardCarrierText$StatusModeSwitchesValues = iArr;
        return iArr;
    }

    private enum StatusMode {
        Normal,
        NetworkLocked,
        SimMissing,
        SimMissingLocked,
        SimPukLocked,
        SimLocked,
        SimPermDisabled,
        SimNotReady,
        SimUnknown,
        NetworkSearching;

        public static StatusMode[] valuesCustom() {
            return values();
        }
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
        initMembers();
    }

    public CarrierText(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                String action = intent.getAction();
                if (!"android.intent.action.ACTION_SHUTDOWN_IPO".equals(action)) {
                    return;
                }
                Log.w("CarrierText", "receive IPO_SHUTDOWN & clear carrier text.");
                CarrierText.this.setText("");
            }
        };
        this.mCallback = new KeyguardUpdateMonitorCallback() {
            @Override
            public void onRefreshCarrierInfo() {
                CarrierText.this.updateCarrierText();
            }

            @Override
            public void onSimStateChangedUsingPhoneId(int phoneId, IccCardConstants.State simState) {
                CarrierText.this.updateCarrierText();
            }

            @Override
            public void onFinishedGoingToSleep(int why) {
                CarrierText.this.setSelected(false);
            }

            @Override
            public void onStartedWakingUp() {
                CarrierText.this.setSelected(true);
            }
        };
        this.mContext = context;
        this.mIsEmergencyCallCapable = context.getResources().getBoolean(R.^attr-private.frameDuration);
        this.mUpdateMonitor = KeyguardUpdateMonitor.getInstance(this.mContext);
        initMembers();
        this.mIOperatorSIMString = KeyguardPluginFactory.getOperatorSIMString(this.mContext);
        this.mCarrierTextExt = KeyguardPluginFactory.getCarrierTextExt(this.mContext);
        TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R$styleable.CarrierText, 0, 0);
        try {
            boolean useAllCaps = a.getBoolean(R$styleable.CarrierText_allCaps, false);
            a.recycle();
            setTransformationMethod(new CarrierTextTransformationMethod(this.mContext, useAllCaps));
            this.mWifiManager = (WifiManager) context.getSystemService("wifi");
        } catch (Throwable th) {
            a.recycle();
            throw th;
        }
    }

    protected void updateCarrierText() {
        ServiceState ss;
        boolean anySimReadyAndInService = false;
        if (isWifiOnlyDevice()) {
            Log.d("CarrierText", "updateCarrierText() - WifiOnly deivce, not show carrier text.");
            setText("");
            return;
        }
        showOrHideCarrier();
        for (int phoneId = 0; phoneId < this.mNumOfPhone; phoneId++) {
            int subId = KeyguardUtils.getSubIdUsingPhoneId(phoneId);
            IccCardConstants.State simState = this.mKeyguardUpdateMonitor.getSimStateOfPhoneId(phoneId);
            SubscriptionInfo subInfo = this.mKeyguardUpdateMonitor.getSubscriptionInfoForSubId(subId);
            CharSequence carrierName = subInfo == null ? null : subInfo.getCarrierName();
            Log.d("CarrierText", "updateCarrierText(): subId = " + subId + " , phoneId = " + phoneId + ", simState = " + simState + ", carrierName = " + carrierName);
            if (simState == IccCardConstants.State.READY && (ss = this.mKeyguardUpdateMonitor.mServiceStates.get(Integer.valueOf(subId))) != null && ss.getDataRegState() == 0 && (ss.getRilDataRadioTechnology() != 18 || (this.mWifiManager.isWifiEnabled() && this.mWifiManager.getConnectionInfo() != null && this.mWifiManager.getConnectionInfo().getBSSID() != null))) {
                Log.d("CarrierText", "SIM ready and in service: subId=" + subId + ", ss=" + ss);
                anySimReadyAndInService = true;
            }
            CharSequence carrierTextForSimState = getCarrierTextForSimState(phoneId, simState, carrierName, null, null);
            if (carrierTextForSimState != null) {
                CharSequence carrierTextForSimState2 = this.mIOperatorSIMString.getOperatorSIMString(carrierTextForSimState.toString(), phoneId, IOperatorSIMString.SIMChangedTag.DELSIM, this.mContext);
                if (carrierTextForSimState2 != null) {
                    carrierTextForSimState = this.mCarrierTextExt.customizeCarrierTextCapital(carrierTextForSimState2.toString()).toString();
                } else {
                    carrierTextForSimState = null;
                }
            }
            if (carrierTextForSimState != null) {
                this.mCarrier[phoneId] = carrierTextForSimState.toString();
            } else {
                this.mCarrier[phoneId] = null;
            }
        }
        String carrierFinalContent = null;
        String divider = this.mCarrierTextExt.customizeCarrierTextDivider(mSeparator.toString());
        for (int i = 0; i < this.mNumOfPhone; i++) {
            if (this.mCarrierNeedToShow[i] && this.mCarrier[i] != null) {
                if (carrierFinalContent == null) {
                    carrierFinalContent = this.mCarrier[i];
                } else {
                    carrierFinalContent = carrierFinalContent + divider + this.mCarrier[i];
                }
            }
        }
        if (!anySimReadyAndInService && WirelessUtils.isAirplaneModeOn(this.mContext)) {
            carrierFinalContent = getContext().getString(R$string.airplane_mode);
        }
        Log.d("CarrierText", "updateCarrierText() - after combination, carrierFinalContent = " + carrierFinalContent);
        setText(carrierFinalContent);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mSeparator = getResources().getString(R.string.install_carrier_app_notification_button);
        boolean shouldMarquee = KeyguardUpdateMonitor.getInstance(this.mContext).isDeviceInteractive();
        setSelected(shouldMarquee);
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
        registerBroadcastReceiver();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (this.mKeyguardUpdateMonitor != null) {
            this.mKeyguardUpdateMonitor.removeCallback(this.mCallback);
        }
        unregisterBroadcastReceiver();
    }

    private CharSequence getCarrierTextForSimState(int phoneId, IccCardConstants.State simState, CharSequence text, CharSequence hnbName, CharSequence csgId) {
        CharSequence carrierText;
        StatusMode status = getStatusForIccState(simState);
        switch (m431getcomandroidkeyguardCarrierText$StatusModeSwitchesValues()[status.ordinal()]) {
            case 1:
                carrierText = makeCarrierStringOnEmergencyCapable(this.mContext.getText(R$string.keyguard_network_locked_message), text, hnbName, csgId);
                break;
            case 2:
                carrierText = text;
                break;
            case 3:
                carrierText = makeCarrierStringOnEmergencyCapable(getContext().getText(R$string.keyguard_sim_locked_message), text, hnbName, csgId);
                break;
            case 4:
                CharSequence simMessage = getContext().getText(R$string.keyguard_missing_sim_message_short);
                CharSequence carrierText2 = makeCarrierStringOnEmergencyCapable(simMessage, text, hnbName, csgId);
                carrierText = this.mCarrierTextExt.customizeCarrierTextWhenSimMissing(this.mCarrierTextExt.customizeCarrierText(carrierText2, simMessage, phoneId));
                break;
            case 5:
                carrierText = null;
                break;
            case 6:
                carrierText = null;
                break;
            case 7:
                carrierText = getContext().getText(R$string.keyguard_permanent_disabled_sim_message_short);
                break;
            case 8:
                carrierText = makeCarrierStringOnEmergencyCapable(getContext().getText(R$string.keyguard_sim_puk_locked_message), text, hnbName, csgId);
                break;
            default:
                carrierText = text;
                break;
        }
        if (carrierText != null) {
            carrierText = this.mCarrierTextExt.customizeCarrierTextWhenCardTypeLocked(carrierText, this.mContext, phoneId, false).toString();
        }
        Log.d("CarrierText", "getCarrierTextForSimState simState=" + simState + " text(carrierName)=" + text + " HNB=" + hnbName + " CSG=" + csgId + " carrierText=" + carrierText);
        return carrierText;
    }

    private CharSequence makeCarrierStringOnEmergencyCapable(CharSequence simMessage, CharSequence emergencyCallMessage, CharSequence hnbName, CharSequence csgId) {
        CharSequence emergencyCallMessageExtend = emergencyCallMessage;
        if (!TextUtils.isEmpty(emergencyCallMessage)) {
            emergencyCallMessageExtend = appendCsgInfo(emergencyCallMessage, hnbName, csgId);
        }
        if (this.mIsEmergencyCallCapable) {
            return concatenate(simMessage, emergencyCallMessageExtend);
        }
        return simMessage;
    }

    private StatusMode getStatusForIccState(IccCardConstants.State simState) {
        boolean missingAndNotProvisioned = true;
        if (simState == null) {
            return StatusMode.SimUnknown;
        }
        if (KeyguardUpdateMonitor.getInstance(this.mContext).isDeviceProvisioned()) {
            missingAndNotProvisioned = false;
        } else if (simState != IccCardConstants.State.ABSENT && simState != IccCardConstants.State.PERM_DISABLED) {
            missingAndNotProvisioned = false;
        }
        if (missingAndNotProvisioned) {
            return StatusMode.SimMissingLocked;
        }
        switch (m430xf663cf59()[simState.ordinal()]) {
            case 1:
                return StatusMode.SimMissing;
            case 2:
                return StatusMode.NetworkLocked;
            case 3:
                return StatusMode.SimNotReady;
            case 4:
                return StatusMode.SimPermDisabled;
            case 5:
                return StatusMode.SimLocked;
            case 6:
                return StatusMode.SimPukLocked;
            case 7:
                return StatusMode.Normal;
            case 8:
                return StatusMode.SimUnknown;
            default:
                return StatusMode.SimMissing;
        }
    }

    private static CharSequence concatenate(CharSequence plmn, CharSequence spn) {
        boolean plmnValid = !TextUtils.isEmpty(plmn);
        boolean spnValid = !TextUtils.isEmpty(spn);
        if (plmnValid && spnValid) {
            return new StringBuilder().append(plmn).append(mSeparator).append(spn).toString();
        }
        if (plmnValid) {
            return plmn;
        }
        if (spnValid) {
            return spn;
        }
        return "";
    }

    private boolean isWifiOnlyDevice() {
        ConnectivityManager cm = (ConnectivityManager) getContext().getSystemService("connectivity");
        return !cm.isNetworkSupported(0);
    }

    private boolean showOrHideCarrier() {
        int mNumOfSIM = 0;
        for (int i = 0; i < this.mNumOfPhone; i++) {
            IccCardConstants.State simState = this.mKeyguardUpdateMonitor.getSimStateOfPhoneId(i);
            StatusMode statusMode = getStatusForIccState(simState);
            boolean simMissing = statusMode == StatusMode.SimMissing || statusMode == StatusMode.SimMissingLocked || statusMode == StatusMode.SimUnknown;
            boolean simMissing2 = this.mCarrierTextExt.showCarrierTextWhenSimMissing(simMissing, i);
            Log.d("CarrierText", "showOrHideCarrier() - after showCarrierTextWhenSimMissing,phone#" + i + " simMissing = " + simMissing2);
            if (!simMissing2) {
                this.mCarrierNeedToShow[i] = true;
                mNumOfSIM++;
            } else {
                this.mCarrierNeedToShow[i] = false;
            }
        }
        List<SubscriptionInfo> subs = this.mKeyguardUpdateMonitor.getSubscriptionInfo(false);
        if (mNumOfSIM == 0) {
            String defaultPlmn = this.mUpdateMonitor.getDefaultPlmn().toString();
            int index = 0;
            int i2 = 0;
            while (true) {
                if (i2 >= subs.size()) {
                    break;
                }
                SubscriptionInfo info = subs.get(i2);
                info.getSubscriptionId();
                int phoneId = info.getSimSlotIndex();
                CharSequence carrierName = info.getCarrierName();
                if (carrierName == null || defaultPlmn.contentEquals(carrierName)) {
                    i2++;
                } else {
                    index = phoneId;
                    break;
                }
            }
            this.mCarrierNeedToShow[index] = true;
        }
        return mNumOfSIM == 0;
    }

    private CharSequence appendCsgInfo(CharSequence srcText, CharSequence hnbName, CharSequence csgId) {
        if (!TextUtils.isEmpty(hnbName)) {
            CharSequence outText = concatenate(srcText, hnbName);
            return outText;
        }
        if (TextUtils.isEmpty(csgId)) {
            return srcText;
        }
        CharSequence outText2 = concatenate(srcText, csgId);
        return outText2;
    }

    private class CarrierTextTransformationMethod extends SingleLineTransformationMethod {
        private final boolean mAllCaps;
        private final Locale mLocale;

        public CarrierTextTransformationMethod(Context context, boolean allCaps) {
            this.mLocale = context.getResources().getConfiguration().locale;
            this.mAllCaps = allCaps;
        }

        @Override
        public CharSequence getTransformation(CharSequence source, View view) {
            CharSequence source2 = super.getTransformation(source, view);
            if (this.mAllCaps && source2 != null) {
                return source2.toString().toUpperCase(this.mLocale);
            }
            return source2;
        }
    }

    private void registerBroadcastReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.ACTION_SHUTDOWN_IPO");
        this.mContext.registerReceiver(this.mBroadcastReceiver, filter);
    }

    private void unregisterBroadcastReceiver() {
        this.mContext.unregisterReceiver(this.mBroadcastReceiver);
    }
}
