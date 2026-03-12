package com.android.keyguard;

import android.content.Context;
import android.content.res.TypedArray;
import android.net.ConnectivityManager;
import android.telephony.SubscriptionInfo;
import android.text.TextUtils;
import android.text.method.SingleLineTransformationMethod;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;
import com.android.internal.telephony.IccCardConstants;
import com.android.internal.widget.LockPatternUtils;
import java.util.List;
import java.util.Locale;

public class CarrierText extends TextView {
    private static CharSequence mSeparator;
    private KeyguardUpdateMonitorCallback mCallback;
    private KeyguardUpdateMonitor mKeyguardUpdateMonitor;
    private LockPatternUtils mLockPatternUtils;

    private enum StatusMode {
        Normal,
        NetworkLocked,
        SimMissing,
        SimMissingLocked,
        SimPukLocked,
        SimLocked,
        SimPermDisabled,
        SimNotReady
    }

    public CarrierText(Context context) {
        this(context, null);
    }

    public CarrierText(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mCallback = new KeyguardUpdateMonitorCallback() {
            @Override
            public void onRefreshCarrierInfo() {
                CarrierText.this.updateCarrierText();
            }

            @Override
            public void onScreenTurnedOff(int why) {
                CarrierText.this.setSelected(false);
            }

            @Override
            public void onScreenTurnedOn() {
                CarrierText.this.setSelected(true);
            }
        };
        this.mLockPatternUtils = new LockPatternUtils(this.mContext);
        TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.CarrierText, 0, 0);
        try {
            boolean useAllCaps = a.getBoolean(R.styleable.CarrierText_allCaps, false);
            a.recycle();
            setTransformationMethod(new CarrierTextTransformationMethod(this.mContext, useAllCaps));
        } catch (Throwable th) {
            a.recycle();
            throw th;
        }
    }

    protected void updateCarrierText() {
        boolean allSimsMissing = true;
        CharSequence displayText = null;
        List<SubscriptionInfo> subs = this.mKeyguardUpdateMonitor.getSubscriptionInfo(false);
        int N = subs.size();
        for (int i = 0; i < N; i++) {
            IccCardConstants.State simState = this.mKeyguardUpdateMonitor.getSimState(subs.get(i).getSubscriptionId());
            CharSequence carrierName = subs.get(i).getCarrierName();
            CharSequence carrierTextForSimState = getCarrierTextForSimState(simState, carrierName);
            if (carrierTextForSimState != null) {
                allSimsMissing = false;
                displayText = concatenate(displayText, carrierTextForSimState);
            }
        }
        if (allSimsMissing) {
            if (N != 0) {
                displayText = makeCarrierStringOnEmergencyCapable(getContext().getText(R.string.keyguard_missing_sim_message_short), subs.get(0).getCarrierName());
            } else {
                displayText = makeCarrierStringOnEmergencyCapable(getContext().getText(R.string.keyguard_missing_sim_message_short), getContext().getText(android.R.string.emailTypeWork));
            }
        }
        setText(displayText);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mSeparator = getResources().getString(android.R.string.mediasize_iso_c0);
        boolean screenOn = KeyguardUpdateMonitor.getInstance(this.mContext).isScreenOn();
        setSelected(screenOn);
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

    private CharSequence getCarrierTextForSimState(IccCardConstants.State simState, CharSequence text) {
        StatusMode status = getStatusForIccState(simState);
        switch (status) {
            case Normal:
                return text;
            case SimNotReady:
                return "";
            case NetworkLocked:
                CharSequence carrierText = makeCarrierStringOnEmergencyCapable(this.mContext.getText(R.string.keyguard_network_locked_message), text);
                return carrierText;
            case SimMissing:
                return null;
            case SimPermDisabled:
                CharSequence carrierText2 = getContext().getText(R.string.keyguard_permanent_disabled_sim_message_short);
                return carrierText2;
            case SimMissingLocked:
                return null;
            case SimLocked:
                CharSequence carrierText3 = makeCarrierStringOnEmergencyCapable(getContext().getText(R.string.keyguard_sim_locked_message), text);
                return carrierText3;
            case SimPukLocked:
                CharSequence carrierText4 = makeCarrierStringOnEmergencyCapable(getContext().getText(R.string.keyguard_sim_puk_locked_message), text);
                return carrierText4;
            default:
                return null;
        }
    }

    private CharSequence makeCarrierStringOnEmergencyCapable(CharSequence simMessage, CharSequence emergencyCallMessage) {
        if (this.mLockPatternUtils.isEmergencyCallCapable()) {
            return concatenate(simMessage, emergencyCallMessage);
        }
        return simMessage;
    }

    private StatusMode getStatusForIccState(IccCardConstants.State simState) {
        if (simState == null) {
            return StatusMode.Normal;
        }
        boolean missingAndNotProvisioned = !KeyguardUpdateMonitor.getInstance(this.mContext).isDeviceProvisioned() && (simState == IccCardConstants.State.ABSENT || simState == IccCardConstants.State.PERM_DISABLED);
        if (missingAndNotProvisioned) {
            simState = IccCardConstants.State.NETWORK_LOCKED;
        }
        switch (AnonymousClass2.$SwitchMap$com$android$internal$telephony$IccCardConstants$State[simState.ordinal()]) {
            case 1:
                return StatusMode.SimMissing;
            case 2:
                return StatusMode.SimMissingLocked;
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
                return StatusMode.SimMissing;
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
            $SwitchMap$com$android$keyguard$CarrierText$StatusMode = new int[StatusMode.values().length];
            try {
                $SwitchMap$com$android$keyguard$CarrierText$StatusMode[StatusMode.Normal.ordinal()] = 1;
            } catch (NoSuchFieldError e9) {
            }
            try {
                $SwitchMap$com$android$keyguard$CarrierText$StatusMode[StatusMode.SimNotReady.ordinal()] = 2;
            } catch (NoSuchFieldError e10) {
            }
            try {
                $SwitchMap$com$android$keyguard$CarrierText$StatusMode[StatusMode.NetworkLocked.ordinal()] = 3;
            } catch (NoSuchFieldError e11) {
            }
            try {
                $SwitchMap$com$android$keyguard$CarrierText$StatusMode[StatusMode.SimMissing.ordinal()] = 4;
            } catch (NoSuchFieldError e12) {
            }
            try {
                $SwitchMap$com$android$keyguard$CarrierText$StatusMode[StatusMode.SimPermDisabled.ordinal()] = 5;
            } catch (NoSuchFieldError e13) {
            }
            try {
                $SwitchMap$com$android$keyguard$CarrierText$StatusMode[StatusMode.SimMissingLocked.ordinal()] = 6;
            } catch (NoSuchFieldError e14) {
            }
            try {
                $SwitchMap$com$android$keyguard$CarrierText$StatusMode[StatusMode.SimLocked.ordinal()] = 7;
            } catch (NoSuchFieldError e15) {
            }
            try {
                $SwitchMap$com$android$keyguard$CarrierText$StatusMode[StatusMode.SimPukLocked.ordinal()] = 8;
            } catch (NoSuchFieldError e16) {
            }
        }
    }

    private static CharSequence concatenate(CharSequence plmn, CharSequence spn) {
        boolean plmnValid = !TextUtils.isEmpty(plmn);
        boolean spnValid = !TextUtils.isEmpty(spn);
        if (plmnValid && spnValid) {
            if (!plmn.equals(spn)) {
                return new StringBuilder().append(plmn).append(mSeparator).append(spn).toString();
            }
            return plmn;
        }
        if (plmnValid) {
            return plmn;
        }
        return spnValid ? spn : "";
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
}
