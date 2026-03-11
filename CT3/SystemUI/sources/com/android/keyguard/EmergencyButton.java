package com.android.keyguard;

import android.R;
import android.app.ActivityManagerNative;
import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.UserHandle;
import android.telecom.TelecomManager;
import android.telephony.ServiceState;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Slog;
import android.view.View;
import android.widget.Button;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.telephony.IccCardConstants;
import com.android.internal.widget.LockPatternUtils;
import com.mediatek.internal.telephony.ITelephonyEx;
import com.mediatek.keyguard.AntiTheft.AntiTheftManager;
import com.mediatek.keyguard.Plugin.KeyguardPluginFactory;
import com.mediatek.keyguard.ext.IEmergencyButtonExt;

public class EmergencyButton extends Button {
    private static final Intent INTENT_EMERGENCY_DIAL = new Intent().setAction("com.android.phone.EmergencyDialer.DIAL").setPackage("com.android.phone").setFlags(343932928);
    private int mEccPhoneIdForNoneSecurityMode;
    private EmergencyButtonCallback mEmergencyButtonCallback;
    private IEmergencyButtonExt mEmergencyButtonExt;
    private final boolean mEnableEmergencyCallWhileSimLocked;
    KeyguardUpdateMonitorCallback mInfoCallback;
    private boolean mIsSecure;
    private final boolean mIsVoiceCapable;
    private boolean mLocateAtNonSecureView;
    private LockPatternUtils mLockPatternUtils;
    private PowerManager mPowerManager;
    private KeyguardUpdateMonitor mUpdateMonitor;

    public interface EmergencyButtonCallback {
        void onEmergencyButtonClickedWhenInCall();
    }

    public EmergencyButton(Context context) {
        this(context, null);
    }

    public EmergencyButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mUpdateMonitor = null;
        this.mEccPhoneIdForNoneSecurityMode = -1;
        this.mLocateAtNonSecureView = false;
        this.mInfoCallback = new KeyguardUpdateMonitorCallback() {
            @Override
            public void onSimStateChangedUsingPhoneId(int phoneId, IccCardConstants.State simState) {
                Log.d("EmergencyButton", "onSimStateChangedUsingSubId: " + simState + ", phoneId=" + phoneId);
                EmergencyButton.this.updateEmergencyCallButton();
            }

            @Override
            public void onPhoneStateChanged(int phoneState) {
                EmergencyButton.this.updateEmergencyCallButton();
            }

            @Override
            public void onRefreshCarrierInfo() {
                EmergencyButton.this.updateEmergencyCallButton();
            }
        };
        this.mIsVoiceCapable = context.getResources().getBoolean(R.^attr-private.frameDuration);
        this.mEnableEmergencyCallWhileSimLocked = this.mContext.getResources().getBoolean(R.^attr-private.enlargeVertexEntryArea);
        this.mUpdateMonitor = KeyguardUpdateMonitor.getInstance(this.mContext);
        try {
            this.mEmergencyButtonExt = KeyguardPluginFactory.getEmergencyButtonExt(context);
        } catch (Exception e) {
            Log.d("EmergencyButton", "EmergencyButton() - error in calling getEmergencyButtonExt().");
            e.printStackTrace();
        }
        TypedArray localAtNonSecureValue = context.obtainStyledAttributes(attrs, R$styleable.ECCButtonAttr);
        this.mLocateAtNonSecureView = localAtNonSecureValue.getBoolean(R$styleable.ECCButtonAttr_locateAtNonSecureView, this.mLocateAtNonSecureView);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        KeyguardUpdateMonitor.getInstance(this.mContext).registerCallback(this.mInfoCallback);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        KeyguardUpdateMonitor.getInstance(this.mContext).removeCallback(this.mInfoCallback);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mLockPatternUtils = new LockPatternUtils(this.mContext);
        this.mPowerManager = (PowerManager) this.mContext.getSystemService("power");
        setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EmergencyButton.this.takeEmergencyCallAction();
            }
        });
        this.mIsSecure = this.mLockPatternUtils.isSecure(KeyguardUpdateMonitor.getCurrentUser());
        updateEmergencyCallButton();
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        updateEmergencyCallButton();
        setText(R$string.kg_emergency_call_label);
    }

    public void takeEmergencyCallAction() {
        MetricsLogger.action(this.mContext, 200);
        this.mPowerManager.userActivity(SystemClock.uptimeMillis(), true);
        try {
            ActivityManagerNative.getDefault().stopSystemLockTaskMode();
        } catch (RemoteException e) {
            Slog.w("EmergencyButton", "Failed to stop app pinning");
        }
        if (isInCall()) {
            resumeCall();
            if (this.mEmergencyButtonCallback == null) {
                return;
            }
            this.mEmergencyButtonCallback.onEmergencyButtonClickedWhenInCall();
            return;
        }
        KeyguardUpdateMonitor.getInstance(this.mContext).reportEmergencyCallAction(true);
        int phoneId = getCurPhoneId();
        if (phoneId == -1) {
            phoneId = this.mEccPhoneIdForNoneSecurityMode;
        }
        this.mEmergencyButtonExt.customizeEmergencyIntent(INTENT_EMERGENCY_DIAL, phoneId);
        getContext().startActivityAsUser(INTENT_EMERGENCY_DIAL, ActivityOptions.makeCustomAnimation(getContext(), 0, 0).toBundle(), new UserHandle(KeyguardUpdateMonitor.getCurrentUser()));
    }

    public void updateEmergencyCallButton() {
        boolean show;
        boolean visible = false;
        if (isInCall()) {
            visible = true;
        } else if (this.mLockPatternUtils.isEmergencyCallCapable()) {
            boolean simLocked = KeyguardUpdateMonitor.getInstance(this.mContext).isSimPinVoiceSecure();
            visible = simLocked ? this.mEnableEmergencyCallWhileSimLocked : this.mLockPatternUtils.isSecure(KeyguardUpdateMonitor.getCurrentUser());
        }
        boolean antiTheftLocked = AntiTheftManager.isAntiTheftLocked();
        boolean eccShouldShow = eccButtonShouldShow();
        Log.i("EmergencyButton", "mLocateAtNonSecureView = " + this.mLocateAtNonSecureView);
        if (!this.mLocateAtNonSecureView || this.mEmergencyButtonExt.showEccInNonSecureUnlock()) {
            show = (visible || antiTheftLocked || this.mEmergencyButtonExt.showEccInNonSecureUnlock()) ? eccShouldShow : false;
            Log.i("EmergencyButton", "show = " + show + " --> visible= " + visible + ", antiTheftLocked=" + antiTheftLocked + ", mEmergencyButtonExt.showEccInNonSecureUnlock() =" + this.mEmergencyButtonExt.showEccInNonSecureUnlock() + ", eccShouldShow=" + eccShouldShow);
        } else {
            Log.i("EmergencyButton", "ECC Button is located on Notification Keygaurd and OP do not ask to show. So this is a normal case ,we never show it.");
            show = false;
        }
        if (!this.mLocateAtNonSecureView || show) {
            this.mLockPatternUtils.updateEmergencyCallButtonState(this, show, false);
        } else {
            Log.i("EmergencyButton", "If the button is on NotificationKeyguard and will not show, we should just set it View.GONE to give more space to IndicationText.");
            setVisibility(8);
        }
    }

    public void setCallback(EmergencyButtonCallback callback) {
        this.mEmergencyButtonCallback = callback;
    }

    private void resumeCall() {
        getTelecommManager().showInCallScreen(false);
    }

    private boolean isInCall() {
        return getTelecommManager().isInCall();
    }

    private TelecomManager getTelecommManager() {
        return (TelecomManager) this.mContext.getSystemService("telecom");
    }

    private boolean eccButtonShouldShow() {
        int phoneCount = KeyguardUtils.getNumOfPhone();
        boolean[] isServiceSupportEcc = new boolean[phoneCount];
        try {
            ITelephonyEx phoneEx = ITelephonyEx.Stub.asInterface(ServiceManager.checkService("phoneEx"));
            if (phoneEx != null) {
                this.mEccPhoneIdForNoneSecurityMode = -1;
                for (int i = 0; i < phoneCount; i++) {
                    int subId = KeyguardUtils.getSubIdUsingPhoneId(i);
                    Bundle bd = phoneEx.getServiceState(subId);
                    if (bd != null) {
                        ServiceState ss = ServiceState.newFromBundle(bd);
                        Log.i("EmergencyButton", "ss.getState() = " + ss.getState() + " ss.isEmergencyOnly()=" + ss.isEmergencyOnly() + " for simId=" + i);
                        if (ss.getState() == 0 || ss.isEmergencyOnly()) {
                            isServiceSupportEcc[i] = true;
                            if (this.mEccPhoneIdForNoneSecurityMode == -1) {
                                this.mEccPhoneIdForNoneSecurityMode = i;
                            }
                        } else {
                            isServiceSupportEcc[i] = false;
                        }
                    }
                }
            }
        } catch (RemoteException e) {
            Log.i("EmergencyButton", "getServiceState error e:" + e.getMessage());
        }
        return this.mEmergencyButtonExt.showEccByServiceState(isServiceSupportEcc, getCurPhoneId());
    }

    private int getCurPhoneId() {
        KeyguardSecurityModel securityModel = new KeyguardSecurityModel(this.mContext);
        return securityModel.getPhoneIdUsingSecurityMode(securityModel.getSecurityMode());
    }
}
