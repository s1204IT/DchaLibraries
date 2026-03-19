package com.android.keyguard;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.internal.telephony.ITelephony;
import com.android.internal.telephony.IccCardConstants;

public class KeyguardSimPinView extends KeyguardPinBasedInputView {
    private static final boolean DEBUG = KeyguardConstants.DEBUG_SIM_STATES;
    private CheckSimPin mCheckSimPinThread;
    KeyguardUtils mKeyguardUtils;
    private int mPhoneId;
    private int mRemainingAttempts;
    private AlertDialog mRemainingAttemptsDialog;
    private boolean mShowDefaultMessage;
    private ProgressDialog mSimUnlockProgressDialog;
    private int mSubId;
    KeyguardUpdateMonitorCallback mUpdateMonitorCallback;

    static class AnonymousClass3 {
        static final int[] $SwitchMap$com$android$internal$telephony$IccCardConstants$State = new int[IccCardConstants.State.values().length];

        static {
            try {
                $SwitchMap$com$android$internal$telephony$IccCardConstants$State[IccCardConstants.State.ABSENT.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$IccCardConstants$State[IccCardConstants.State.READY.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
        }
    }

    public KeyguardSimPinView(Context context) {
        this(context, null);
    }

    public KeyguardSimPinView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mSimUnlockProgressDialog = null;
        this.mShowDefaultMessage = true;
        this.mRemainingAttempts = -1;
        this.mSubId = -1;
        this.mPhoneId = 0;
        this.mUpdateMonitorCallback = new KeyguardUpdateMonitorCallback() {
            @Override
            public void onSimStateChangedUsingPhoneId(int i, IccCardConstants.State state) {
                if (KeyguardSimPinView.DEBUG) {
                    Log.d("KeyguardSimPinView", "onSimStateChangedUsingSubId: " + state + ", phoneId=" + i);
                }
                switch (AnonymousClass3.$SwitchMap$com$android$internal$telephony$IccCardConstants$State[state.ordinal()]) {
                    case 1:
                        if (i == KeyguardSimPinView.this.mPhoneId) {
                            KeyguardUpdateMonitor.getInstance(KeyguardSimPinView.this.getContext()).reportSimUnlocked(KeyguardSimPinView.this.mPhoneId);
                            if (KeyguardSimPinView.this.mCallback != null) {
                                KeyguardSimPinView.this.mCallback.dismiss(true, KeyguardUpdateMonitor.getCurrentUser());
                            }
                        }
                        break;
                    case 2:
                        KeyguardSimPinView.this.mRemainingAttempts = -1;
                        KeyguardSimPinView.this.resetState();
                        break;
                    default:
                        KeyguardSimPinView.this.resetState();
                        break;
                }
            }
        };
        this.mKeyguardUtils = new KeyguardUtils(context);
    }

    @Override
    public void resetState() {
        super.resetState();
        if (DEBUG) {
            Log.v("KeyguardSimPinView", "Resetting state");
        }
        this.mSecurityMessageDisplay.setMessage(com.android.systemui.R.string.kg_sim_pin_instructions);
    }

    @Override
    protected void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        resetState();
    }

    @Override
    protected int getPromptReasonStringRes(int i) {
        return 0;
    }

    private String getPinPasswordErrorMessage(int i, boolean z) {
        String string;
        if (i == 0) {
            string = getContext().getString(com.android.systemui.R.string.kg_password_wrong_pin_code_pukked);
        } else if (i > 0) {
            string = getContext().getResources().getQuantityString(z ? com.android.systemui.R.plurals.kg_password_default_pin_message : com.android.systemui.R.plurals.kg_password_wrong_pin_code, i, Integer.valueOf(i));
        } else {
            string = getContext().getString(z ? com.android.systemui.R.string.kg_sim_pin_instructions : com.android.systemui.R.string.kg_password_pin_failed);
        }
        if (KeyguardEsimArea.isEsimLocked(this.mContext, this.mSubId)) {
            string = getResources().getString(com.android.systemui.R.string.kg_sim_lock_esim_instructions, string);
        }
        if (DEBUG) {
            Log.d("KeyguardSimPinView", "getPinPasswordErrorMessage: attemptsRemaining=" + i + " displayMessage=" + string);
        }
        return string;
    }

    @Override
    protected boolean shouldLockout(long j) {
        return false;
    }

    @Override
    protected int getPasswordTextViewId() {
        return com.android.systemui.R.id.simPinEntry;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mPhoneId = KeyguardUpdateMonitor.getInstance(getContext()).getSimPinLockPhoneId();
        if (KeyguardUtils.getNumOfPhone() > 1) {
            View viewFindViewById = findViewById(com.android.systemui.R.id.keyguard_sim);
            if (viewFindViewById != null) {
                viewFindViewById.setVisibility(8);
            }
            View viewFindViewById2 = findViewById(com.android.systemui.R.id.sim_info_message);
            if (viewFindViewById2 != null) {
                viewFindViewById2.setVisibility(0);
            }
            dealwithSIMInfoChanged();
        }
        if (this.mEcaView instanceof EmergencyCarrierArea) {
            ((EmergencyCarrierArea) this.mEcaView).setCarrierTextVisible(true);
        }
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
        if (this.mSimUnlockProgressDialog != null) {
            this.mSimUnlockProgressDialog.dismiss();
            this.mSimUnlockProgressDialog = null;
        }
    }

    private abstract class CheckSimPin extends Thread {
        private final String mPin;

        abstract void onSimCheckResponse(int i, int i2);

        protected CheckSimPin(String str) {
            this.mPin = str;
        }

        @Override
        public void run() {
            try {
                if (KeyguardSimPinView.DEBUG) {
                    Log.v("KeyguardSimPinView", "call supplyPinReportResultForSubscriber(subid=" + KeyguardSimPinView.this.mSubId + ")");
                }
                Log.d("KeyguardSimPinView", "call supplyPinReportResultForSubscriber() mPhoneId = " + KeyguardSimPinView.this.mPhoneId);
                KeyguardUtils.getSubIdUsingPhoneId(KeyguardSimPinView.this.mPhoneId);
                final int[] iArrSupplyPinReportResultForSubscriber = ITelephony.Stub.asInterface(ServiceManager.checkService("phone")).supplyPinReportResultForSubscriber(KeyguardSimPinView.this.mSubId, this.mPin);
                if (KeyguardSimPinView.DEBUG) {
                    Log.v("KeyguardSimPinView", "supplyPinReportResult returned: " + iArrSupplyPinReportResultForSubscriber[0] + " " + iArrSupplyPinReportResultForSubscriber[1]);
                }
                KeyguardSimPinView.this.post(new Runnable() {
                    @Override
                    public void run() {
                        CheckSimPin.this.onSimCheckResponse(iArrSupplyPinReportResultForSubscriber[0], iArrSupplyPinReportResultForSubscriber[1]);
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
            this.mSimUnlockProgressDialog.setMessage(this.mContext.getString(com.android.systemui.R.string.kg_sim_unlock_progress_dialog_message));
            this.mSimUnlockProgressDialog.setIndeterminate(true);
            this.mSimUnlockProgressDialog.setCancelable(false);
            this.mSimUnlockProgressDialog.getWindow().setType(2009);
        }
        return this.mSimUnlockProgressDialog;
    }

    private Dialog getSimRemainingAttemptsDialog(int i) {
        String pinPasswordErrorMessage = getPinPasswordErrorMessage(i, false);
        if (this.mRemainingAttemptsDialog == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this.mContext);
            builder.setMessage(pinPasswordErrorMessage);
            builder.setCancelable(false);
            builder.setNeutralButton(com.android.systemui.R.string.ok, (DialogInterface.OnClickListener) null);
            this.mRemainingAttemptsDialog = builder.create();
            this.mRemainingAttemptsDialog.getWindow().setType(2009);
        } else {
            this.mRemainingAttemptsDialog.setMessage(pinPasswordErrorMessage);
        }
        return this.mRemainingAttemptsDialog;
    }

    @Override
    protected void verifyPasswordAndUnlock() {
        if (this.mPasswordEntry.getText().length() < 4) {
            this.mSecurityMessageDisplay.setMessage(com.android.systemui.R.string.kg_invalid_sim_pin_hint);
            resetPasswordText(true, true);
            this.mCallback.userActivity();
        } else {
            getSimUnlockProgressDialog().show();
            if (this.mCheckSimPinThread == null) {
                this.mCheckSimPinThread = new CheckSimPin(this.mPasswordEntry.getText()) {
                    @Override
                    void onSimCheckResponse(final int i, final int i2) {
                        KeyguardSimPinView.this.post(new Runnable() {
                            @Override
                            public void run() {
                                KeyguardSimPinView.this.mRemainingAttempts = i2;
                                if (KeyguardSimPinView.this.mSimUnlockProgressDialog != null) {
                                    KeyguardSimPinView.this.mSimUnlockProgressDialog.hide();
                                }
                                if (i == 0) {
                                    KeyguardUpdateMonitor.getInstance(KeyguardSimPinView.this.getContext()).reportSimUnlocked(KeyguardSimPinView.this.mPhoneId);
                                    KeyguardSimPinView.this.mRemainingAttempts = -1;
                                    KeyguardSimPinView.this.mShowDefaultMessage = true;
                                    if (KeyguardSimPinView.this.mCallback != null) {
                                        KeyguardSimPinView.this.mCallback.dismiss(true, KeyguardUpdateMonitor.getCurrentUser());
                                    }
                                } else {
                                    KeyguardSimPinView.this.mShowDefaultMessage = false;
                                    if (i == 1) {
                                        if (i2 <= 2) {
                                            KeyguardSimPinView.this.getSimRemainingAttemptsDialog(i2).show();
                                        } else {
                                            KeyguardSimPinView.this.mSecurityMessageDisplay.setMessage(KeyguardSimPinView.this.getPinPasswordErrorMessage(i2, false));
                                        }
                                    } else {
                                        KeyguardSimPinView.this.mSecurityMessageDisplay.setMessage(KeyguardSimPinView.this.getContext().getString(com.android.systemui.R.string.kg_password_pin_failed));
                                    }
                                    if (KeyguardSimPinView.DEBUG) {
                                        Log.d("KeyguardSimPinView", "verifyPasswordAndUnlock  CheckSimPin.onSimCheckResponse: " + i + " attemptsRemaining=" + i2);
                                    }
                                    KeyguardSimPinView.this.resetPasswordText(true, i != 0);
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
    public boolean startDisappearAnimation(Runnable runnable) {
        return false;
    }

    @Override
    public CharSequence getTitle() {
        return getContext().getString(android.R.string.config_rearDisplayPhysicalAddress);
    }

    private void dealwithSIMInfoChanged() {
        String string;
        try {
            string = this.mKeyguardUtils.getOptrNameUsingPhoneId(this.mPhoneId, this.mContext);
        } catch (IndexOutOfBoundsException e) {
            Log.w("KeyguardSimPinView", "getOptrNameBySlot exception, mPhoneId=" + this.mPhoneId);
            string = null;
        }
        if (DEBUG) {
            Log.i("KeyguardSimPinView", "dealwithSIMInfoChanged, mPhoneId=" + this.mPhoneId + ", operName=" + string);
        }
        TextView textView = (TextView) findViewById(com.android.systemui.R.id.for_text);
        ImageView imageView = (ImageView) findViewById(com.android.systemui.R.id.sub_icon);
        TextView textView2 = (TextView) findViewById(com.android.systemui.R.id.sim_card_name);
        if (string == null) {
            if (DEBUG) {
                Log.d("KeyguardSimPinView", "mPhoneId " + this.mPhoneId + " is new subInfo record");
            }
            setForTextNewCard(this.mPhoneId, textView);
            imageView.setVisibility(8);
            textView2.setVisibility(8);
            return;
        }
        if (DEBUG) {
            Log.d("KeyguardSimPinView", "dealwithSIMInfoChanged, show operName for mPhoneId=" + this.mPhoneId);
        }
        textView.setText(this.mContext.getString(com.android.systemui.R.string.kg_slot_id, Integer.valueOf(this.mPhoneId + 1)) + " ");
        if (string == null) {
            string = this.mContext.getString(com.android.systemui.R.string.kg_detecting_simcard);
        }
        textView2.setText(string);
        imageView.setImageBitmap(this.mKeyguardUtils.getOptrBitmapUsingPhoneId(this.mPhoneId, this.mContext));
        imageView.setVisibility(0);
        textView2.setVisibility(0);
    }

    private void setForTextNewCard(int i, TextView textView) {
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append(this.mContext.getString(com.android.systemui.R.string.kg_slot_id, Integer.valueOf(i + 1)));
        stringBuffer.append(" ");
        stringBuffer.append(this.mContext.getText(com.android.systemui.R.string.kg_new_simcard));
        textView.setText(stringBuffer.toString());
    }
}
