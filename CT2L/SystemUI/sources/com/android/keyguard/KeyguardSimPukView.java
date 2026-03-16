package com.android.keyguard;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ImageView;
import com.android.internal.telephony.ITelephony;
import com.android.internal.telephony.IccCardConstants;

public class KeyguardSimPukView extends KeyguardPinBasedInputView {
    final Handler h;
    private CheckSimPuk mCheckSimPukThread;
    private String mPinText;
    private String mPukText;
    private AlertDialog mRemainingAttemptsDialog;
    private ImageView mSimImageView;
    private int[] mSimRetryStat;
    private ProgressDialog mSimUnlockProgressDialog;
    private StateMachine mStateMachine;
    private int mSubId;
    KeyguardUpdateMonitorCallback mUpdateMonitorCallback;

    public KeyguardSimPukView(Context context) {
        this(context, null);
    }

    public KeyguardSimPukView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mSimUnlockProgressDialog = null;
        this.mStateMachine = new StateMachine();
        this.mUpdateMonitorCallback = new KeyguardUpdateMonitorCallback() {
            @Override
            public void onSimStateChanged(int subId, int slotId, IccCardConstants.State simState) {
                KeyguardSimPukView.this.resetState();
            }
        };
        this.mSimRetryStat = new int[]{3, 3, 10, 10};
        this.h = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == 1) {
                    if (KeyguardSimPukView.this.mSimUnlockProgressDialog != null) {
                        KeyguardSimPukView.this.mSimUnlockProgressDialog.hide();
                    }
                    KeyguardSimPukView.this.mSimRetryStat = (int[]) msg.obj;
                    KeyguardSimPukView.this.mSecurityMessageDisplay.setMessage((CharSequence) KeyguardSimPukView.this.getPukPasswordHitMessage(KeyguardSimPukView.this.mSimRetryStat[2]), true);
                    removeMessages(1);
                }
            }
        };
    }

    private String getPukPasswordHitMessage(int attemptsRemaining) {
        Resources rez = getResources();
        String displayMessage = rez.getString(R.string.kg_puk_enter_puk_hint);
        KeyguardUpdateMonitor monitor = KeyguardUpdateMonitor.getInstance(this.mContext);
        this.mSubId = monitor.getNextSubIdForState(IccCardConstants.State.PUK_REQUIRED);
        if (SubscriptionManager.isValidSubscriptionId(this.mSubId)) {
            int count = TelephonyManager.getDefault().getSimCount();
            if (count > 1) {
                SubscriptionInfo info = monitor.getSubscriptionInfoForSubId(this.mSubId);
                String displayName = info != null ? info.getDisplayName() : "";
                displayMessage = rez.getString(R.string.kg_puk_enter_puk_hint_multi, displayName);
            }
        }
        return displayMessage + " (" + attemptsRemaining + ")";
    }

    private class StateMachine {
        final int CONFIRM_PIN;
        final int DONE;
        final int ENTER_PIN;
        final int ENTER_PUK;
        private int state;

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
                    msg = R.string.kg_puk_enter_pin_hint;
                } else {
                    msg = R.string.kg_invalid_sim_puk_hint;
                }
            } else if (this.state == 1) {
                if (KeyguardSimPukView.this.checkPin()) {
                    this.state = 2;
                    msg = R.string.kg_enter_confirm_pin_hint;
                } else {
                    msg = R.string.kg_invalid_sim_pin_hint;
                }
            } else if (this.state == 2) {
                if (KeyguardSimPukView.this.confirmPin()) {
                    this.state = 3;
                    msg = R.string.keyguard_sim_unlock_progress_dialog_message;
                    KeyguardSimPukView.this.updateSim();
                } else {
                    this.state = 1;
                    msg = R.string.kg_invalid_confirm_pin_hint;
                }
            }
            KeyguardSimPukView.this.resetPasswordText(true);
            if (msg != 0) {
                KeyguardSimPukView.this.mSecurityMessageDisplay.setMessage(msg, true);
            }
        }

        void reset() {
            String msg;
            KeyguardSimPukView.this.mPinText = "";
            KeyguardSimPukView.this.mPukText = "";
            this.state = 0;
            KeyguardUpdateMonitor monitor = KeyguardUpdateMonitor.getInstance(KeyguardSimPukView.this.mContext);
            KeyguardSimPukView.this.mSubId = monitor.getNextSubIdForState(IccCardConstants.State.PUK_REQUIRED);
            if (SubscriptionManager.isValidSubscriptionId(KeyguardSimPukView.this.mSubId)) {
                int count = TelephonyManager.getDefault().getSimCount();
                Resources rez = KeyguardSimPukView.this.getResources();
                int color = -1;
                if (count >= 2) {
                    SubscriptionInfo info = monitor.getSubscriptionInfoForSubId(KeyguardSimPukView.this.mSubId);
                    String displayName = info != null ? info.getDisplayName() : "";
                    msg = rez.getString(R.string.kg_puk_enter_puk_hint_multi, displayName);
                    if (info != null) {
                        color = info.getIconTint();
                    }
                } else {
                    msg = rez.getString(R.string.kg_puk_enter_puk_hint);
                }
                KeyguardSimPukView.this.mSecurityMessageDisplay.setMessage((CharSequence) msg, true);
                KeyguardSimPukView.this.mSimImageView.setImageTintList(ColorStateList.valueOf(color));
                new GetSimLockInfo(KeyguardSimPukView.this.mSubId) {
                    {
                        KeyguardSimPukView keyguardSimPukView = KeyguardSimPukView.this;
                    }
                }.start();
            }
            KeyguardSimPukView.this.mPasswordEntry.requestFocus();
        }
    }

    private String getPukPasswordErrorMessage(int attemptsRemaining) {
        if (attemptsRemaining == 0) {
            String displayMessage = getContext().getString(R.string.kg_password_wrong_puk_code_dead);
            return displayMessage;
        }
        if (attemptsRemaining > 0) {
            String displayMessage2 = getContext().getResources().getQuantityString(R.plurals.kg_password_wrong_puk_code, attemptsRemaining, Integer.valueOf(attemptsRemaining));
            return displayMessage2;
        }
        String displayMessage3 = getContext().getString(R.string.kg_password_puk_failed);
        return displayMessage3;
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
        return R.id.pukEntry;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mSecurityMessageDisplay.setTimeout(0);
        if (this.mEcaView instanceof EmergencyCarrierArea) {
            ((EmergencyCarrierArea) this.mEcaView).setCarrierTextVisible(true);
        }
        this.mSimImageView = (ImageView) findViewById(R.id.keyguard_sim);
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
    public void showUsabilityHint() {
    }

    @Override
    public void onPause() {
        if (this.mSimUnlockProgressDialog != null) {
            this.mSimUnlockProgressDialog.dismiss();
            this.mSimUnlockProgressDialog = null;
        }
    }

    private abstract class CheckSimPuk extends Thread {
        private final String mPin;
        private final String mPuk;
        private final int mSubId;

        abstract void onSimLockChangedResponse(int i, int i2);

        protected CheckSimPuk(String puk, String pin, int subId) {
            this.mPuk = puk;
            this.mPin = pin;
            this.mSubId = subId;
        }

        @Override
        public void run() {
            try {
                final int[] result = ITelephony.Stub.asInterface(ServiceManager.checkService("phone")).supplyPukReportResultForSubscriber(this.mSubId, this.mPuk, this.mPin);
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
            this.mSimUnlockProgressDialog.setMessage(this.mContext.getString(R.string.kg_sim_unlock_progress_dialog_message));
            this.mSimUnlockProgressDialog.setIndeterminate(true);
            this.mSimUnlockProgressDialog.setCancelable(false);
            if (!(this.mContext instanceof Activity)) {
                this.mSimUnlockProgressDialog.getWindow().setType(2009);
            }
        }
        return this.mSimUnlockProgressDialog;
    }

    private Dialog getPukRemainingAttemptsDialog(int remaining) {
        String msg = getPukPasswordErrorMessage(remaining);
        if (this.mRemainingAttemptsDialog == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this.mContext);
            builder.setMessage(msg);
            builder.setCancelable(false);
            builder.setNeutralButton(R.string.ok, (DialogInterface.OnClickListener) null);
            this.mRemainingAttemptsDialog = builder.create();
            this.mRemainingAttemptsDialog.getWindow().setType(2009);
        } else {
            this.mRemainingAttemptsDialog.setMessage(msg);
        }
        return this.mRemainingAttemptsDialog;
    }

    private boolean checkPuk() {
        if (this.mPasswordEntry.getText().length() != 8) {
            return false;
        }
        this.mPukText = this.mPasswordEntry.getText();
        return true;
    }

    private boolean checkPin() {
        int length = this.mPasswordEntry.getText().length();
        if (length < 4 || length > 8) {
            return false;
        }
        this.mPinText = this.mPasswordEntry.getText();
        return true;
    }

    public boolean confirmPin() {
        return this.mPinText.equals(this.mPasswordEntry.getText());
    }

    private void updateSim() {
        getSimUnlockProgressDialog().show();
        if (this.mCheckSimPukThread == null) {
            this.mCheckSimPukThread = new CheckSimPuk(this.mPukText, this.mPinText, this.mSubId) {
                @Override
                void onSimLockChangedResponse(final int result, final int attemptsRemaining) {
                    KeyguardSimPukView.this.post(new Runnable() {
                        @Override
                        public void run() {
                            if (KeyguardSimPukView.this.mSimUnlockProgressDialog != null) {
                                KeyguardSimPukView.this.mSimUnlockProgressDialog.hide();
                            }
                            KeyguardSimPukView.this.resetPasswordText(true);
                            if (result == 0) {
                                KeyguardUpdateMonitor.getInstance(KeyguardSimPukView.this.getContext()).reportSimUnlocked(KeyguardSimPukView.this.mSubId);
                                KeyguardSimPukView.this.mCallback.dismiss(true);
                            } else {
                                if (result == 1) {
                                    if (attemptsRemaining <= 2) {
                                        KeyguardSimPukView.this.getPukRemainingAttemptsDialog(attemptsRemaining).show();
                                        KeyguardSimPukView.this.mSecurityMessageDisplay.setMessage((CharSequence) KeyguardSimPukView.this.getPukPasswordHitMessage(attemptsRemaining), true);
                                    } else {
                                        KeyguardSimPukView.this.mSecurityMessageDisplay.setMessage((CharSequence) KeyguardSimPukView.this.getPukPasswordErrorMessage(attemptsRemaining), true);
                                    }
                                } else {
                                    KeyguardSimPukView.this.mSecurityMessageDisplay.setMessage((CharSequence) KeyguardSimPukView.this.getContext().getString(R.string.kg_password_puk_failed), true);
                                }
                                KeyguardSimPukView.this.mStateMachine.reset();
                            }
                            KeyguardSimPukView.this.mCheckSimPukThread = null;
                        }
                    });
                }
            };
            this.mCheckSimPukThread.start();
        }
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

    private abstract class GetSimLockInfo extends Thread {
        private int mSubId;

        protected GetSimLockInfo(int subId) {
            this.mSubId = -1;
            this.mSubId = subId;
        }

        @Override
        public void run() {
            try {
                int[] result = ITelephony.Stub.asInterface(ServiceManager.checkService("phone")).getSimLockInfo(this.mSubId);
                Message msg = Message.obtain();
                msg.what = 1;
                msg.obj = result;
                KeyguardSimPukView.this.h.sendMessage(msg);
            } catch (RemoteException e) {
                Message msg2 = Message.obtain();
                msg2.what = 1;
                msg2.obj = KeyguardSimPukView.this.mSimRetryStat;
                KeyguardSimPukView.this.h.sendMessage(msg2);
            }
        }
    }
}
