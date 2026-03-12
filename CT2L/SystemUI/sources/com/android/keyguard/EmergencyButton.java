package com.android.keyguard;

import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.UserHandle;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import com.android.internal.telephony.IccCardConstants;
import com.android.internal.widget.LockPatternUtils;

public class EmergencyButton extends Button {
    KeyguardUpdateMonitorCallback mInfoCallback;
    private LockPatternUtils mLockPatternUtils;
    private PowerManager mPowerManager;

    public EmergencyButton(Context context) {
        this(context, null);
    }

    public EmergencyButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mInfoCallback = new KeyguardUpdateMonitorCallback() {
            @Override
            public void onSimStateChanged(int subId, int slotId, IccCardConstants.State simState) {
                EmergencyButton.this.updateEmergencyCallButton();
            }

            @Override
            public void onPhoneStateChanged(int phoneState) {
                EmergencyButton.this.updateEmergencyCallButton();
            }
        };
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
        updateEmergencyCallButton();
    }

    public void takeEmergencyCallAction() {
        this.mPowerManager.userActivity(SystemClock.uptimeMillis(), true);
        if (this.mLockPatternUtils.isInCall()) {
            this.mLockPatternUtils.resumeCall();
            return;
        }
        KeyguardUpdateMonitor.getInstance(this.mContext).reportEmergencyCallAction(true);
        Intent intent = new Intent("com.android.phone.EmergencyDialer.DIAL");
        intent.setFlags(276824064);
        getContext().startActivityAsUser(intent, new UserHandle(this.mLockPatternUtils.getCurrentUser()));
    }

    public void updateEmergencyCallButton() {
        boolean enabled = false;
        if (this.mLockPatternUtils.isInCall()) {
            enabled = true;
        } else if (this.mLockPatternUtils.isEmergencyCallCapable()) {
            boolean simLocked = KeyguardUpdateMonitor.getInstance(this.mContext).isSimPinVoiceSecure();
            if (simLocked) {
                enabled = this.mLockPatternUtils.isEmergencyCallEnabledWhileSimLocked();
            } else {
                enabled = this.mLockPatternUtils.isSecure();
            }
        }
        this.mLockPatternUtils.updateEmergencyCallButtonState(this, enabled, false);
    }
}
