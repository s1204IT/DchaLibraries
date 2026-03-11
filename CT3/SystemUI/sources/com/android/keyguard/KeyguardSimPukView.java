package com.android.keyguard;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.internal.telephony.ITelephony;
import com.android.internal.telephony.IccCardConstants;

public class KeyguardSimPukView extends KeyguardPinBasedInputView {
    private CheckSimPuk mCheckSimPukThread;
    KeyguardUtils mKeyguardUtils;
    private int mPhoneId;
    private String mPinText;
    private String mPukText;
    private AlertDialog mRemainingAttemptsDialog;
    private ProgressDialog mSimUnlockProgressDialog;
    private StateMachine mStateMachine;
    KeyguardUpdateMonitorCallback mUpdateMonitorCallback;

    private class StateMachine {
        final int CONFIRM_PIN;
        final int DONE;
        final int ENTER_PIN;
        final int ENTER_PUK;
        private int state;

        StateMachine(KeyguardSimPukView this$0, StateMachine stateMachine) {
            this();
        }

        private StateMachine() {
            this.ENTER_PUK = 0;
            this.ENTER_PIN = 1;
            this.CONFIRM_PIN = 2;
            this.DONE = 3;
            this.state = 0;
        }

        public void next() {
            int msg = 0;
            if (this.state == 0) {
                if (KeyguardSimPukView.this.checkPuk()) {
                    this.state = 1;
                    msg = R$string.kg_puk_enter_pin_hint;
                } else {
                    msg = R$string.kg_invalid_sim_puk_hint;
                }
            } else if (this.state == 1) {
                if (KeyguardSimPukView.this.checkPin()) {
                    this.state = 2;
                    msg = R$string.kg_enter_confirm_pin_hint;
                } else {
                    msg = R$string.kg_invalid_sim_pin_hint;
                }
            } else if (this.state == 2) {
                if (KeyguardSimPukView.this.confirmPin()) {
                    this.state = 3;
                    msg = R$string.keyguard_sim_unlock_progress_dialog_message;
                    KeyguardSimPukView.this.updateSim();
                } else {
                    this.state = 1;
                    msg = R$string.kg_invalid_confirm_pin_hint;
                }
            }
            KeyguardSimPukView.this.resetPasswordText(true, true);
            if (msg == 0) {
                return;
            }
            KeyguardSimPukView.this.mSecurityMessageDisplay.setMessage(msg, true);
        }

        void reset() {
            KeyguardSimPukView.this.mPinText = "";
            KeyguardSimPukView.this.mPukText = "";
            this.state = 0;
            KeyguardSimPukView.this.mSecurityMessageDisplay.setMessage(R$string.kg_puk_enter_puk_hint, true);
            KeyguardSimPukView.this.mPasswordEntry.requestFocus();
        }
    }

    @Override
    protected int getPromtReasonStringRes(int reason) {
        return 0;
    }

    public String getPukPasswordErrorMessage(int attemptsRemaining) {
        String displayMessage;
        if (attemptsRemaining == 0) {
            displayMessage = getContext().getString(R$string.kg_password_wrong_puk_code_dead);
        } else if (attemptsRemaining > 0) {
            displayMessage = getContext().getResources().getQuantityString(R$plurals.kg_password_wrong_puk_code, attemptsRemaining, Integer.valueOf(attemptsRemaining));
        } else {
            displayMessage = getContext().getString(R$string.kg_password_puk_failed);
        }
        Log.d("KeyguardSimPukView", "getPukPasswordErrorMessage: attemptsRemaining=" + attemptsRemaining + " displayMessage=" + displayMessage);
        return displayMessage;
    }

    public KeyguardSimPukView(Context context) {
        this(context, null);
    }

    public KeyguardSimPukView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mSimUnlockProgressDialog = null;
        this.mStateMachine = new StateMachine(this, null);
        this.mPhoneId = 0;
        this.mUpdateMonitorCallback = new KeyguardUpdateMonitorCallback() {

            private static final int[] f6x8dbfd0b5 = null;

            private static int[] m483xf663cf59() {
                if (f6x8dbfd0b5 != null) {
                    return f6x8dbfd0b5;
                }
                int[] iArr = new int[IccCardConstants.State.values().length];
                try {
                    iArr[IccCardConstants.State.ABSENT.ordinal()] = 1;
                } catch (NoSuchFieldError e) {
                }
                try {
                    iArr[IccCardConstants.State.CARD_IO_ERROR.ordinal()] = 3;
                } catch (NoSuchFieldError e2) {
                }
                try {
                    iArr[IccCardConstants.State.NETWORK_LOCKED.ordinal()] = 4;
                } catch (NoSuchFieldError e3) {
                }
                try {
                    iArr[IccCardConstants.State.NOT_READY.ordinal()] = 2;
                } catch (NoSuchFieldError e4) {
                }
                try {
                    iArr[IccCardConstants.State.PERM_DISABLED.ordinal()] = 5;
                } catch (NoSuchFieldError e5) {
                }
                try {
                    iArr[IccCardConstants.State.PIN_REQUIRED.ordinal()] = 6;
                } catch (NoSuchFieldError e6) {
                }
                try {
                    iArr[IccCardConstants.State.PUK_REQUIRED.ordinal()] = 7;
                } catch (NoSuchFieldError e7) {
                }
                try {
                    iArr[IccCardConstants.State.READY.ordinal()] = 8;
                } catch (NoSuchFieldError e8) {
                }
                try {
                    iArr[IccCardConstants.State.UNKNOWN.ordinal()] = 9;
                } catch (NoSuchFieldError e9) {
                }
                f6x8dbfd0b5 = iArr;
                return iArr;
            }

            @Override
            public void onSimStateChangedUsingPhoneId(int phoneId, IccCardConstants.State simState) {
                Log.d("KeyguardSimPukView", "onSimStateChangedUsingPhoneId: " + simState + ", phoneId=" + phoneId);
                switch (m483xf663cf59()[simState.ordinal()]) {
                    case 1:
                    case 2:
                        if (phoneId == KeyguardSimPukView.this.mPhoneId) {
                            KeyguardUpdateMonitor.getInstance(KeyguardSimPukView.this.getContext()).reportSimUnlocked(KeyguardSimPukView.this.mPhoneId);
                            KeyguardSimPukView.this.mCallback.dismiss(true);
                        }
                        break;
                }
            }
        };
        this.mKeyguardUtils = new KeyguardUtils(context);
    }

    @Override
    public void resetState() {
        super.resetState();
        this.mStateMachine.reset();
    }

    @Override
    protected boolean shouldLockout(long deadline) {
        return false;
    }

    @Override
    protected int getPasswordTextViewId() {
        return R$id.pukEntry;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mPhoneId = KeyguardUpdateMonitor.getInstance(getContext()).getSimPukLockPhoneId();
        if (KeyguardUtils.getNumOfPhone() > 1) {
            View simIcon = findViewById(R$id.keyguard_sim);
            if (simIcon != null) {
                simIcon.setVisibility(8);
            }
            View simInfoMsg = findViewById(R$id.sim_info_message);
            if (simInfoMsg != null) {
                simInfoMsg.setVisibility(0);
            }
            dealwithSIMInfoChanged();
        }
        this.mSecurityMessageDisplay.setTimeout(0);
        if (!(this.mEcaView instanceof EmergencyCarrierArea)) {
            return;
        }
        ((EmergencyCarrierArea) this.mEcaView).setCarrierTextVisible(true);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        KeyguardUpdateMonitor.getInstance(this.mContext).registerCallback(this.mUpdateMonitorCallback);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        KeyguardUpdateMonitor.getInstance(this.mContext).removeCallback(this.mUpdateMonitorCallback);
    }

    @Override
    public void onPause() {
        if (this.mSimUnlockProgressDialog == null) {
            return;
        }
        this.mSimUnlockProgressDialog.dismiss();
        this.mSimUnlockProgressDialog = null;
    }

    private abstract class CheckSimPuk extends Thread {
        private final String mPin;
        private final String mPuk;

        abstract void onSimLockChangedResponse(int i, int i2);

        protected CheckSimPuk(String puk, String pin) {
            this.mPuk = puk;
            this.mPin = pin;
        }

        @Override
        public void run() {
            try {
                Log.v("KeyguardSimPukView", "call supplyPukReportResultForSubscriber() mPhoneId = " + KeyguardSimPukView.this.mPhoneId);
                int subId = KeyguardUtils.getSubIdUsingPhoneId(KeyguardSimPukView.this.mPhoneId);
                final int[] result = ITelephony.Stub.asInterface(ServiceManager.checkService("phone")).supplyPukReportResultForSubscriber(subId, this.mPuk, this.mPin);
                Log.v("KeyguardSimPukView", "supplyPukReportResultForSubscriber returned: " + result[0] + " " + result[1]);
                KeyguardSimPukView.this.post(new Runnable() {
                    @Override
                    public void run() {
                        CheckSimPuk.this.onSimLockChangedResponse(result[0], result[1]);
                    }
                });
            } catch (RemoteException e) {
                Log.e("KeyguardSimPukView", "RemoteException for supplyPukReportResult:", e);
                KeyguardSimPukView.this.post(new Runnable() {
                    @Override
                    public void run() {
                        CheckSimPuk.this.onSimLockChangedResponse(2, -1);
                    }
                });
            }
        }
    }

    private Dialog getSimUnlockProgressDialog() {
        if (this.mSimUnlockProgressDialog == null) {
            this.mSimUnlockProgressDialog = new ProgressDialog(this.mContext);
            this.mSimUnlockProgressDialog.setMessage(this.mContext.getString(R$string.kg_sim_unlock_progress_dialog_message));
            this.mSimUnlockProgressDialog.setIndeterminate(true);
            this.mSimUnlockProgressDialog.setCancelable(false);
            if (!(this.mContext instanceof Activity)) {
                this.mSimUnlockProgressDialog.getWindow().setType(2009);
            }
        }
        return this.mSimUnlockProgressDialog;
    }

    public Dialog getPukRemainingAttemptsDialog(int remaining) {
        String msg = getPukPasswordErrorMessage(remaining);
        if (this.mRemainingAttemptsDialog == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this.mContext);
            builder.setMessage(msg);
            builder.setCancelable(false);
            builder.setNeutralButton(R$string.ok, (DialogInterface.OnClickListener) null);
            this.mRemainingAttemptsDialog = builder.create();
            this.mRemainingAttemptsDialog.getWindow().setType(2009);
        } else {
            this.mRemainingAttemptsDialog.setMessage(msg);
        }
        return this.mRemainingAttemptsDialog;
    }

    public boolean checkPuk() {
        if (this.mPasswordEntry.getText().length() == 8) {
            this.mPukText = this.mPasswordEntry.getText();
            return true;
        }
        return false;
    }

    public boolean checkPin() {
        int length = this.mPasswordEntry.getText().length();
        if (length >= 4 && length <= 8) {
            this.mPinText = this.mPasswordEntry.getText();
            return true;
        }
        return false;
    }

    public boolean confirmPin() {
        return this.mPinText.equals(this.mPasswordEntry.getText());
    }

    public void updateSim() {
        getSimUnlockProgressDialog().show();
        if (this.mCheckSimPukThread != null) {
            return;
        }
        this.mCheckSimPukThread = new CheckSimPuk(this, this.mPukText, this.mPinText) {
            @Override
            void onSimLockChangedResponse(final int result, final int attemptsRemaining) {
                this.post(new Runnable() {
                    @Override
                    public void run() {
                        if (this.mSimUnlockProgressDialog != null) {
                            this.mSimUnlockProgressDialog.hide();
                        }
                        if (result == 0) {
                            KeyguardUpdateMonitor.getInstance(this.getContext()).reportSimUnlocked(this.mPhoneId);
                            this.mCallback.dismiss(true);
                        } else {
                            if (result == 1) {
                                if (attemptsRemaining <= 2) {
                                    this.getPukRemainingAttemptsDialog(attemptsRemaining).show();
                                } else {
                                    this.mSecurityMessageDisplay.setMessage((CharSequence) this.getPukPasswordErrorMessage(attemptsRemaining), true);
                                }
                            } else {
                                this.mSecurityMessageDisplay.setMessage((CharSequence) this.getContext().getString(R$string.kg_password_puk_failed), true);
                            }
                            Log.d("KeyguardSimPukView", "verifyPasswordAndUnlock  UpdateSim.onSimCheckResponse:  attemptsRemaining=" + attemptsRemaining);
                            this.mStateMachine.reset();
                        }
                        this.mCheckSimPukThread = null;
                    }
                });
            }
        };
        this.mCheckSimPukThread.start();
    }

    @Override
    protected void verifyPasswordAndUnlock() {
        this.mStateMachine.next();
    }

    @Override
    public void startAppearAnimation() {
    }

    @Override
    public boolean startDisappearAnimation(Runnable finishRunnable) {
        return false;
    }

    private void dealwithSIMInfoChanged() {
        String operName = null;
        try {
            operName = this.mKeyguardUtils.getOptrNameUsingPhoneId(this.mPhoneId, this.mContext);
        } catch (IndexOutOfBoundsException e) {
            Log.w("KeyguardSimPukView", "getOptrNameBySlot exception, mPhoneId=" + this.mPhoneId);
        }
        Log.i("KeyguardSimPukView", "dealwithSIMInfoChanged, mPhoneId=" + this.mPhoneId + ", operName=" + operName);
        TextView forText = (TextView) findViewById(R$id.for_text);
        ImageView subIcon = (ImageView) findViewById(R$id.sub_icon);
        TextView simCardName = (TextView) findViewById(R$id.sim_card_name);
        if (operName == null) {
            Log.d("KeyguardSimPukView", "mPhoneId " + this.mPhoneId + " is new subInfo record");
            setForTextNewCard(this.mPhoneId, forText);
            subIcon.setVisibility(8);
            simCardName.setVisibility(8);
            return;
        }
        Log.d("KeyguardSimPukView", "dealwithSIMInfoChanged, show operName for mPhoneId=" + this.mPhoneId);
        forText.setText(this.mContext.getString(R$string.kg_slot_id, Integer.valueOf(this.mPhoneId + 1)) + " ");
        if (operName == null) {
            operName = this.mContext.getString(R$string.kg_detecting_simcard);
        }
        simCardName.setText(operName);
        Bitmap iconBitmap = this.mKeyguardUtils.getOptrBitmapUsingPhoneId(this.mPhoneId, this.mContext);
        subIcon.setImageBitmap(iconBitmap);
        subIcon.setVisibility(0);
        simCardName.setVisibility(0);
    }

    private void setForTextNewCard(int phoneId, TextView forText) {
        StringBuffer forSb = new StringBuffer();
        forSb.append(this.mContext.getString(R$string.kg_slot_id, Integer.valueOf(phoneId + 1)));
        forSb.append(" ");
        forSb.append(this.mContext.getText(R$string.kg_new_simcard));
        forText.setText(forSb.toString());
    }
}
