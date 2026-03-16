package com.android.keyguard;

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

public class KeyguardSimPinView extends KeyguardPinBasedInputView {
    final Handler h;
    private CheckSimPin mCheckSimPinThread;
    private AlertDialog mRemainingAttemptsDialog;
    private ImageView mSimImageView;
    private int[] mSimRetryStat;
    private ProgressDialog mSimUnlockProgressDialog;
    private int mSubId;
    KeyguardUpdateMonitorCallback mUpdateMonitorCallback;

    private String getPinPasswordHitMessage(int attemptsRemaining) {
        String displayMessage;
        KeyguardUpdateMonitor monitor = KeyguardUpdateMonitor.getInstance(this.mContext);
        int count = TelephonyManager.getDefault().getSimCount();
        Resources rez = getResources();
        if (count < 2) {
            displayMessage = rez.getString(R.string.kg_sim_pin_instructions);
        } else {
            SubscriptionInfo info = monitor.getSubscriptionInfoForSubId(this.mSubId);
            String displayName = info != null ? info.getDisplayName() : "";
            displayMessage = rez.getString(R.string.kg_sim_pin_instructions_multi, displayName);
        }
        return displayMessage + " (" + attemptsRemaining + ")";
    }

    public KeyguardSimPinView(Context context) {
        this(context, null);
    }

    public KeyguardSimPinView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mSimUnlockProgressDialog = null;
        this.mUpdateMonitorCallback = new KeyguardUpdateMonitorCallback() {
            @Override
            public void onSimStateChanged(int subId, int slotId, IccCardConstants.State simState) {
                Log.v("KeyguardSimPinView", "onSimStateChanged(subId=" + subId + ",state=" + simState + ")");
                KeyguardSimPinView.this.resetState();
            }
        };
        this.mSimRetryStat = new int[]{3, 3, 10, 10};
        this.h = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == 1) {
                    if (KeyguardSimPinView.this.mSimUnlockProgressDialog != null) {
                        KeyguardSimPinView.this.mSimUnlockProgressDialog.hide();
                    }
                    KeyguardSimPinView.this.mSimRetryStat = (int[]) msg.obj;
                    KeyguardSimPinView.this.mSecurityMessageDisplay.setMessage((CharSequence) KeyguardSimPinView.this.getPinPasswordHitMessage(KeyguardSimPinView.this.mSimRetryStat[0]), true);
                    removeMessages(1);
                }
            }
        };
    }

    @Override
    public void resetState() {
        String msg;
        super.resetState();
        Log.v("KeyguardSimPinView", "Resetting state");
        KeyguardUpdateMonitor monitor = KeyguardUpdateMonitor.getInstance(this.mContext);
        this.mSubId = monitor.getNextSubIdForState(IccCardConstants.State.PIN_REQUIRED);
        if (SubscriptionManager.isValidSubscriptionId(this.mSubId)) {
            int count = TelephonyManager.getDefault().getSimCount();
            Resources rez = getResources();
            int color = -1;
            if (count < 2) {
                msg = rez.getString(R.string.kg_sim_pin_instructions);
            } else {
                SubscriptionInfo info = monitor.getSubscriptionInfoForSubId(this.mSubId);
                String displayName = info != null ? info.getDisplayName() : "";
                msg = rez.getString(R.string.kg_sim_pin_instructions_multi, displayName);
                if (info != null) {
                    color = info.getIconTint();
                }
            }
            this.mSecurityMessageDisplay.setMessage((CharSequence) msg, true);
            this.mSimImageView.setImageTintList(ColorStateList.valueOf(color));
            new GetSimLockInfo(this.mSubId) {
            }.start();
        }
    }

    private String getPinPasswordErrorMessage(int attemptsRemaining) {
        String displayMessage;
        if (attemptsRemaining == 0) {
            displayMessage = getContext().getString(R.string.kg_password_wrong_pin_code_pukked);
        } else if (attemptsRemaining > 0) {
            displayMessage = getContext().getResources().getQuantityString(R.plurals.kg_password_wrong_pin_code, attemptsRemaining, Integer.valueOf(attemptsRemaining));
        } else {
            displayMessage = getContext().getString(R.string.kg_password_pin_failed);
        }
        Log.d("KeyguardSimPinView", "getPinPasswordErrorMessage: attemptsRemaining=" + attemptsRemaining + " displayMessage=" + displayMessage);
        return displayMessage;
    }

    @Override
    protected boolean shouldLockout(long deadline) {
        return false;
    }

    @Override
    protected int getPasswordTextViewId() {
        return R.id.simPinEntry;
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

    private abstract class CheckSimPin extends Thread {
        private final String mPin;
        private int mSubId;

        abstract void onSimCheckResponse(int i, int i2);

        protected CheckSimPin(String pin, int subId) {
            this.mPin = pin;
            this.mSubId = subId;
        }

        @Override
        public void run() {
            try {
                Log.v("KeyguardSimPinView", "call supplyPinReportResultForSubscriber(subid=" + this.mSubId + ")");
                final int[] result = ITelephony.Stub.asInterface(ServiceManager.checkService("phone")).supplyPinReportResultForSubscriber(this.mSubId, this.mPin);
                Log.v("KeyguardSimPinView", "supplyPinReportResult returned: " + result[0] + " " + result[1]);
                KeyguardSimPinView.this.post(new Runnable() {
                    @Override
                    public void run() {
                        CheckSimPin.this.onSimCheckResponse(result[0], result[1]);
                    }
                });
            } catch (RemoteException e) {
                Log.e("KeyguardSimPinView", "RemoteException for supplyPinReportResult:", e);
                KeyguardSimPinView.this.post(new Runnable() {
                    @Override
                    public void run() {
                        CheckSimPin.this.onSimCheckResponse(2, -1);
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
            this.mSimUnlockProgressDialog.getWindow().setType(2009);
        }
        return this.mSimUnlockProgressDialog;
    }

    private Dialog getSimRemainingAttemptsDialog(int remaining) {
        String msg = getPinPasswordErrorMessage(remaining);
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

    @Override
    protected void verifyPasswordAndUnlock() {
        String entry = this.mPasswordEntry.getText();
        if (entry.length() < 4) {
            this.mSecurityMessageDisplay.setMessage(R.string.kg_invalid_sim_pin_hint, true);
            resetPasswordText(true);
            this.mCallback.userActivity();
        } else {
            getSimUnlockProgressDialog().show();
            if (this.mCheckSimPinThread == null) {
                this.mCheckSimPinThread = new CheckSimPin(this.mPasswordEntry.getText(), this.mSubId) {
                    @Override
                    void onSimCheckResponse(final int result, final int attemptsRemaining) {
                        KeyguardSimPinView.this.post(new Runnable() {
                            @Override
                            public void run() {
                                if (KeyguardSimPinView.this.mSimUnlockProgressDialog != null) {
                                    KeyguardSimPinView.this.mSimUnlockProgressDialog.hide();
                                }
                                KeyguardSimPinView.this.resetPasswordText(true);
                                if (result == 0) {
                                    KeyguardUpdateMonitor.getInstance(KeyguardSimPinView.this.getContext()).reportSimUnlocked(KeyguardSimPinView.this.mSubId);
                                    KeyguardSimPinView.this.mCallback.dismiss(true);
                                } else {
                                    if (result == 1) {
                                        if (attemptsRemaining <= 2) {
                                            KeyguardSimPinView.this.getSimRemainingAttemptsDialog(attemptsRemaining).show();
                                            KeyguardSimPinView.this.mSecurityMessageDisplay.setMessage((CharSequence) KeyguardSimPinView.this.getPinPasswordHitMessage(attemptsRemaining), true);
                                        } else {
                                            KeyguardSimPinView.this.mSecurityMessageDisplay.setMessage((CharSequence) KeyguardSimPinView.this.getPinPasswordErrorMessage(attemptsRemaining), true);
                                        }
                                    } else {
                                        KeyguardSimPinView.this.mSecurityMessageDisplay.setMessage((CharSequence) KeyguardSimPinView.this.getContext().getString(R.string.kg_password_pin_failed), true);
                                    }
                                    Log.d("KeyguardSimPinView", "verifyPasswordAndUnlock  CheckSimPin.onSimCheckResponse: " + result + " attemptsRemaining=" + attemptsRemaining);
                                }
                                KeyguardSimPinView.this.mCallback.userActivity();
                                KeyguardSimPinView.this.mCheckSimPinThread = null;
                            }
                        });
                    }
                };
                this.mCheckSimPinThread.start();
            }
        }
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
                KeyguardSimPinView.this.h.sendMessage(msg);
            } catch (RemoteException e) {
                Message msg2 = Message.obtain();
                msg2.what = 1;
                msg2.obj = KeyguardSimPinView.this.mSimRetryStat;
                KeyguardSimPinView.this.h.sendMessage(msg2);
            }
        }
    }
}
