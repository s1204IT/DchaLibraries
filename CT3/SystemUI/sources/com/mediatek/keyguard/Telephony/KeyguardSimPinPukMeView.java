package com.mediatek.keyguard.Telephony;

import android.R;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.INotificationManager;
import android.app.ITransientNotification;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.telephony.SubscriptionInfo;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.WindowManagerImpl;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.internal.telephony.ITelephony;
import com.android.internal.telephony.IccCardConstants;
import com.android.keyguard.EmergencyCarrierArea;
import com.android.keyguard.KeyguardPinBasedInputView;
import com.android.keyguard.KeyguardSecurityModel;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.keyguard.KeyguardUpdateMonitorCallback;
import com.android.keyguard.KeyguardUtils;
import com.android.keyguard.R$id;
import com.android.keyguard.R$plurals;
import com.android.keyguard.R$string;
import com.mediatek.internal.telephony.ITelephonyEx;
import com.mediatek.keyguard.Plugin.KeyguardPluginFactory;
import com.mediatek.keyguard.ext.IKeyguardUtilExt;
import com.mediatek.keyguard.ext.IOperatorSIMString;

public class KeyguardSimPinPukMeView extends KeyguardPinBasedInputView {
    private Runnable mDismissSimPinPukRunnable;
    private Handler mHandler;
    private IOperatorSIMString mIOperatorSIMString;
    private IKeyguardUtilExt mKeyguardUtilExt;
    private KeyguardUtils mKeyguardUtils;
    private IccCardConstants.State mLastSimState;
    private String mNewPinText;
    private int mNextRepollStatePhoneId;
    private int mPhoneId;
    private String mPukText;
    private AlertDialog mRemainingAttemptsDialog;
    private StringBuffer mSb;
    private KeyguardSecurityModel mSecurityModel;
    private AlertDialog mSimCardDialog;
    private volatile boolean mSimCheckInProgress;
    private ImageView mSimImageView;
    private ProgressDialog mSimUnlockProgressDialog;
    private int mUnlockEnterState;
    KeyguardUpdateMonitor mUpdateMonitor;
    KeyguardUpdateMonitorCallback mUpdateMonitorCallback;
    private String[] strLockName;

    public KeyguardSimPinPukMeView(Context context) {
        this(context, null);
    }

    public KeyguardSimPinPukMeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mSimUnlockProgressDialog = null;
        this.mUpdateMonitor = null;
        this.mSb = null;
        this.mNextRepollStatePhoneId = -1;
        this.mLastSimState = IccCardConstants.State.UNKNOWN;
        this.strLockName = new String[]{" [NP]", " [NSP]", " [SP]", " [CP]", " [SIMP]"};
        this.mHandler = new Handler(Looper.myLooper(), null, true);
        this.mDismissSimPinPukRunnable = new Runnable() {
            @Override
            public void run() {
                KeyguardSimPinPukMeView.this.mUpdateMonitor.reportSimUnlocked(KeyguardSimPinPukMeView.this.mPhoneId);
            }
        };
        this.mUpdateMonitorCallback = new KeyguardUpdateMonitorCallback() {
            @Override
            public void onSimStateChangedUsingPhoneId(int phoneId, IccCardConstants.State simState) {
                Log.d("KeyguardSimPinPukMeView", "onSimStateChangedUsingPhoneId: " + simState + ", phoneId = " + phoneId + ", mPhoneId = " + KeyguardSimPinPukMeView.this.mPhoneId);
                Log.d("KeyguardSimPinPukMeView", "onSimStateChangedUsingPhoneId: mCallback = " + KeyguardSimPinPukMeView.this.mCallback);
                if (phoneId != KeyguardSimPinPukMeView.this.mPhoneId) {
                    if (phoneId == KeyguardSimPinPukMeView.this.mNextRepollStatePhoneId) {
                        Log.d("KeyguardSimPinPukMeView", "onSimStateChanged: mNextRepollStatePhoneId = " + KeyguardSimPinPukMeView.this.mNextRepollStatePhoneId);
                        if (KeyguardSimPinPukMeView.this.mSimUnlockProgressDialog != null) {
                            KeyguardSimPinPukMeView.this.mSimUnlockProgressDialog.hide();
                        }
                        if (IccCardConstants.State.READY != simState) {
                            KeyguardSimPinPukMeView.this.mCallback.dismiss(true);
                            KeyguardSimPinPukMeView.this.mLastSimState = simState;
                            return;
                        } else {
                            KeyguardSimPinPukMeView.this.mLastSimState = IccCardConstants.State.NETWORK_LOCKED;
                            KeyguardSimPinPukMeView.this.simStateReadyProcess();
                            return;
                        }
                    }
                    return;
                }
                KeyguardSimPinPukMeView.this.resetState(true);
                if (KeyguardSimPinPukMeView.this.mSimUnlockProgressDialog != null) {
                    KeyguardSimPinPukMeView.this.mSimUnlockProgressDialog.hide();
                }
                KeyguardSimPinPukMeView.this.mHandler.removeCallbacks(KeyguardSimPinPukMeView.this.mDismissSimPinPukRunnable);
                if (IccCardConstants.State.READY == simState) {
                    KeyguardSimPinPukMeView.this.simStateReadyProcess();
                } else if (IccCardConstants.State.NOT_READY == simState || IccCardConstants.State.ABSENT == simState) {
                    Log.d("KeyguardSimPinPukMeView", "onSimStateChangedUsingPhoneId: not ready, phoneId = " + phoneId);
                    KeyguardSimPinPukMeView.this.mCallback.dismiss(true);
                    KeyguardSimPinPukMeView.this.mSimCheckInProgress = false;
                    Log.d("KeyguardSimPinPukMeView", "set mSimCheckInProgress false");
                } else if (IccCardConstants.State.NETWORK_LOCKED == simState) {
                    if (!KeyguardUtils.isMediatekSimMeLockSupport()) {
                        KeyguardSimPinPukMeView.this.mCallback.dismiss(true);
                    } else if (KeyguardSimPinPukMeView.this.getRetryMeCount(KeyguardSimPinPukMeView.this.mPhoneId) == 0) {
                        Log.d("KeyguardSimPinPukMeView", "onSimStateChanged: ME retrycount is 0, dismiss it");
                        KeyguardSimPinPukMeView.this.mUpdateMonitor.setPinPukMeDismissFlagOfPhoneId(phoneId, true);
                        KeyguardSimPinPukMeView.this.mCallback.dismiss(true);
                    }
                }
                KeyguardSimPinPukMeView.this.mLastSimState = simState;
                Log.d("KeyguardSimPinPukMeView", "assign mLastSimState=" + KeyguardSimPinPukMeView.this.mLastSimState);
            }

            @Override
            public void onAirPlaneModeChanged(boolean airPlaneModeEnabled) {
                Log.d("KeyguardSimPinPukMeView", "onAirPlaneModeChanged(airPlaneModeEnabled = " + airPlaneModeEnabled + ")");
                if (!airPlaneModeEnabled) {
                    return;
                }
                Log.d("KeyguardSimPinPukMeView", "Flight-Mode turns on & keyguard is showing, dismiss keyguard.");
                KeyguardSimPinPukMeView.this.mPasswordEntry.reset(true, true);
                KeyguardSimPinPukMeView.this.mCallback.userActivity();
                KeyguardSimPinPukMeView.this.mCallback.dismiss(true);
            }
        };
        this.mKeyguardUtils = new KeyguardUtils(context);
        this.mSb = new StringBuffer();
        this.mUpdateMonitor = KeyguardUpdateMonitor.getInstance(getContext());
        this.mSecurityModel = new KeyguardSecurityModel(getContext());
        try {
            this.mKeyguardUtilExt = KeyguardPluginFactory.getKeyguardUtilExt(context);
            this.mIOperatorSIMString = KeyguardPluginFactory.getOperatorSIMString(context);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setPhoneId(int phoneId) {
        this.mPhoneId = phoneId;
        Log.i("KeyguardSimPinPukMeView", "setPhoneId=" + phoneId);
        resetState();
        if (this.mSimCardDialog == null) {
            return;
        }
        if (this.mSimCardDialog.isShowing()) {
            this.mSimCardDialog.dismiss();
        }
        this.mSimCardDialog = null;
    }

    @Override
    public void resetState() {
        resetState(false);
    }

    public void resetState(boolean forceReload) {
        Log.v("KeyguardSimPinPukMeView", "Resetting state");
        super.resetState();
        TextView forText = (TextView) findViewById(R$id.slot_num_text);
        forText.setText(this.mContext.getString(R$string.kg_slot_id, Integer.valueOf(this.mPhoneId + 1)) + " ");
        Resources rez = getResources();
        String msg = "";
        int count = KeyguardUtils.getNumOfPhone();
        int color = -1;
        IccCardConstants.State simState = this.mUpdateMonitor.getSimStateOfPhoneId(this.mPhoneId);
        if (count < 2) {
            if (simState == IccCardConstants.State.PIN_REQUIRED) {
                msg = rez.getString(R$string.kg_sim_pin_instructions);
                this.mUnlockEnterState = 0;
            } else if (simState == IccCardConstants.State.PUK_REQUIRED) {
                msg = rez.getString(R$string.kg_puk_enter_puk_hint);
                this.mUnlockEnterState = 1;
            } else if (IccCardConstants.State.NETWORK_LOCKED == simState && KeyguardUtils.isMediatekSimMeLockSupport()) {
                int category = this.mUpdateMonitor.getSimMeCategoryOfPhoneId(this.mPhoneId);
                msg = rez.getString(R$string.simlock_entersimmelock) + this.strLockName[category] + getRetryMeString(this.mPhoneId);
                this.mUnlockEnterState = 5;
            }
        } else {
            int subId = KeyguardUtils.getSubIdUsingPhoneId(this.mPhoneId);
            SubscriptionInfo info = this.mUpdateMonitor.getSubscriptionInfoForSubId(subId, forceReload);
            String displayName = info != null ? info.getDisplayName() : "";
            if (info == null) {
                displayName = "CARD " + Integer.toString(this.mPhoneId + 1);
                Log.d("KeyguardSimPinPukMeView", "we set a displayname");
            }
            Log.d("KeyguardSimPinPukMeView", "resetState() - subId = " + subId + ", displayName = " + displayName);
            if (simState == IccCardConstants.State.PIN_REQUIRED) {
                msg = rez.getString(R$string.kg_sim_pin_instructions_multi, displayName);
                this.mUnlockEnterState = 0;
            } else if (simState == IccCardConstants.State.PUK_REQUIRED) {
                msg = rez.getString(R$string.kg_puk_enter_puk_hint_multi, displayName);
                this.mUnlockEnterState = 1;
            } else if (IccCardConstants.State.NETWORK_LOCKED == simState && KeyguardUtils.isMediatekSimMeLockSupport()) {
                int category2 = this.mUpdateMonitor.getSimMeCategoryOfPhoneId(this.mPhoneId);
                msg = rez.getString(R$string.simlock_entersimmelock) + this.strLockName[category2] + getRetryMeString(this.mPhoneId);
                this.mUnlockEnterState = 5;
            }
            if (info != null) {
                color = info.getIconTint();
            }
        }
        this.mKeyguardUtilExt.customizePinPukLockView(this.mPhoneId, this.mSimImageView, forText);
        this.mSimImageView.setImageTintList(ColorStateList.valueOf(color));
        String msg2 = this.mIOperatorSIMString.getOperatorSIMString(msg, this.mPhoneId, IOperatorSIMString.SIMChangedTag.DELSIM, this.mContext);
        Log.d("KeyguardSimPinPukMeView", "resetState() - mSecurityMessageDisplay.setMessage = " + msg2);
        this.mSecurityMessageDisplay.setMessage((CharSequence) msg2, true);
    }

    public String getPinPasswordErrorMessage(int attemptsRemaining) {
        String displayMessage;
        if (attemptsRemaining == 0) {
            displayMessage = getContext().getString(R$string.kg_password_wrong_pin_code_pukked);
        } else if (attemptsRemaining > 0) {
            displayMessage = getContext().getResources().getQuantityString(R$plurals.kg_password_wrong_pin_code, attemptsRemaining, Integer.valueOf(attemptsRemaining));
        } else {
            displayMessage = getContext().getString(R$string.kg_password_pin_failed);
        }
        Log.d("KeyguardSimPinPukMeView", "getPinPasswordErrorMessage: attemptsRemaining=" + attemptsRemaining + " displayMessage=" + displayMessage);
        return displayMessage;
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
        Log.d("KeyguardSimPinPukMeView", "getPukPasswordErrorMessage: attemptsRemaining=" + attemptsRemaining + " displayMessage=" + displayMessage);
        return displayMessage;
    }

    @Override
    protected boolean shouldLockout(long deadline) {
        return false;
    }

    @Override
    protected int getPasswordTextViewId() {
        return R$id.simPinPukMeEntry;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mPhoneId = -1;
        this.mSecurityMessageDisplay.setTimeout(0);
        if (this.mEcaView instanceof EmergencyCarrierArea) {
            ((EmergencyCarrierArea) this.mEcaView).setCarrierTextVisible(true);
            View ctView = this.mEcaView.findViewById(R$id.carrier_text);
            this.mKeyguardUtilExt.customizeCarrierTextGravity((TextView) ctView);
        }
        this.mSimImageView = (ImageView) findViewById(R$id.keyguard_sim);
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        Log.d("KeyguardSimPinPukMeView", "onAttachedToWindow");
        this.mUpdateMonitor.registerCallback(this.mUpdateMonitorCallback);
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        Log.d("KeyguardSimPinPukMeView", "onDetachedFromWindow");
        this.mHandler.removeCallbacks(this.mDismissSimPinPukRunnable);
        this.mUpdateMonitor.removeCallback(this.mUpdateMonitorCallback);
    }

    @Override
    public void onResume(int reason) {
        if (this.mSimUnlockProgressDialog != null) {
            this.mSimUnlockProgressDialog.dismiss();
            this.mSimUnlockProgressDialog = null;
        }
        InputMethodManager imm = (InputMethodManager) this.mContext.getSystemService("input_method");
        if (!imm.isActive()) {
            return;
        }
        Log.i("KeyguardSimPinPukMeView", "IME is showing, we should hide it");
        imm.hideSoftInputFromWindow(getWindowToken(), 2);
    }

    @Override
    public void onPause() {
        if (this.mSimUnlockProgressDialog == null) {
            return;
        }
        this.mSimUnlockProgressDialog.dismiss();
        this.mSimUnlockProgressDialog = null;
    }

    public void setInputInvalidAlertDialog(CharSequence message, boolean shouldDisplay) {
        StringBuilder sb = new StringBuilder(message);
        if (shouldDisplay) {
            AlertDialog newDialog = new AlertDialog.Builder(this.mContext).setMessage(sb).setPositiveButton(R.string.ok, (DialogInterface.OnClickListener) null).setCancelable(true).create();
            newDialog.getWindow().setType(2009);
            newDialog.getWindow().addFlags(2);
            newDialog.show();
            return;
        }
        Toast.makeText(this.mContext, sb).show();
    }

    public String getRetryMeString(int phoneId) {
        int meRetryCount = getRetryMeCount(phoneId);
        return "(" + this.mContext.getString(R$string.retries_left, Integer.valueOf(meRetryCount)) + ")";
    }

    public int getRetryMeCount(int phoneId) {
        return this.mUpdateMonitor.getSimMeLeftRetryCountOfPhoneId(phoneId);
    }

    public void minusRetryMeCount(int phoneId) {
        this.mUpdateMonitor.minusSimMeLeftRetryCountOfPhoneId(phoneId);
    }

    private boolean validatePin(String pin, boolean isPUK) {
        int pinMinimum = isPUK ? 8 : 4;
        if (pin == null || pin.length() < pinMinimum || pin.length() > 8) {
            return false;
        }
        return true;
    }

    private void updatePinEnterScreen() {
        switch (this.mUnlockEnterState) {
            case 1:
                this.mPukText = this.mPasswordEntry.getText().toString();
                if (validatePin(this.mPukText, true)) {
                    this.mUnlockEnterState = 2;
                    this.mSb.delete(0, this.mSb.length());
                    this.mSb.append(this.mContext.getText(R$string.keyguard_password_enter_new_pin_code));
                    Log.d("KeyguardSimPinPukMeView", "updatePinEnterScreen() - STATE_ENTER_PUK, validatePin = true ,mSecurityMessageDisplay.setMessage = " + this.mSb.toString());
                    this.mSecurityMessageDisplay.setMessage((CharSequence) this.mSb.toString(), true);
                } else {
                    Log.d("KeyguardSimPinPukMeView", "updatePinEnterScreen() - STATE_ENTER_PUK, validatePin = false ,mSecurityMessageDisplay.setMessage = R.string.invalidPuk");
                    this.mSecurityMessageDisplay.setMessage(R$string.invalidPuk, true);
                }
                break;
            case 2:
                this.mNewPinText = this.mPasswordEntry.getText().toString();
                if (validatePin(this.mNewPinText, false)) {
                    this.mUnlockEnterState = 3;
                    this.mSb.delete(0, this.mSb.length());
                    this.mSb.append(this.mContext.getText(R$string.keyguard_password_Confirm_pin_code));
                    Log.d("KeyguardSimPinPukMeView", "updatePinEnterScreen() - STATE_ENTER_NEW, validatePin = true ,mSecurityMessageDisplay.setMessage = " + this.mSb.toString());
                    this.mSecurityMessageDisplay.setMessage((CharSequence) this.mSb.toString(), true);
                } else {
                    Log.d("KeyguardSimPinPukMeView", "updatePinEnterScreen() - STATE_ENTER_NEW, validatePin = false ,mSecurityMessageDisplay.setMessage = R.string.keyguard_code_length_prompt");
                    this.mSecurityMessageDisplay.setMessage(R$string.keyguard_code_length_prompt, true);
                }
                break;
            case 3:
                if (!this.mNewPinText.equals(this.mPasswordEntry.getText().toString())) {
                    this.mUnlockEnterState = 2;
                    this.mSb.delete(0, this.mSb.length());
                    this.mSb.append(this.mContext.getText(R$string.keyguard_code_donnot_mismatch));
                    this.mSb.append(this.mContext.getText(R$string.keyguard_password_enter_new_pin_code));
                    Log.d("KeyguardSimPinPukMeView", "updatePinEnterScreen() - STATE_REENTER_NEW, true ,mSecurityMessageDisplay.setMessage = " + this.mSb.toString());
                    this.mSecurityMessageDisplay.setMessage((CharSequence) this.mSb.toString(), true);
                } else {
                    Log.d("KeyguardSimPinPukMeView", "updatePinEnterScreen() - STATE_REENTER_NEW, false ,mSecurityMessageDisplay.setMessage = empty string.");
                    this.mUnlockEnterState = 4;
                    this.mSecurityMessageDisplay.setMessage((CharSequence) "", true);
                }
                break;
        }
        this.mPasswordEntry.reset(true, true);
        this.mCallback.userActivity();
    }

    private abstract class CheckSimPinPuk extends Thread {
        private final String mPin;
        private final String mPuk;
        private int[] mResult;

        abstract void onSimCheckResponse(int i, int i2);

        protected CheckSimPinPuk(String pin, int phoneId) {
            this.mPin = pin;
            this.mPuk = null;
        }

        protected CheckSimPinPuk(String puk, String pin, int phoneId) {
            this.mPin = pin;
            this.mPuk = puk;
        }

        @Override
        public void run() {
            try {
                Log.d("KeyguardSimPinPukMeView", "CheckSimPinPuk, mPhoneId =" + KeyguardSimPinPukMeView.this.mPhoneId);
                if (KeyguardSimPinPukMeView.this.mUpdateMonitor.getSimStateOfPhoneId(KeyguardSimPinPukMeView.this.mPhoneId) == IccCardConstants.State.PIN_REQUIRED) {
                    ITelephony phoneService = ITelephony.Stub.asInterface(ServiceManager.checkService("phone"));
                    if (phoneService != null) {
                        int subId = KeyguardUtils.getSubIdUsingPhoneId(KeyguardSimPinPukMeView.this.mPhoneId);
                        this.mResult = phoneService.supplyPinReportResultForSubscriber(subId, this.mPin);
                    } else {
                        Log.d("KeyguardSimPinPukMeView", "phoneService is gone, skip supplyPinForSubscriber().");
                    }
                } else if (KeyguardSimPinPukMeView.this.mUpdateMonitor.getSimStateOfPhoneId(KeyguardSimPinPukMeView.this.mPhoneId) == IccCardConstants.State.PUK_REQUIRED) {
                    ITelephony phoneService2 = ITelephony.Stub.asInterface(ServiceManager.checkService("phone"));
                    if (phoneService2 != null) {
                        int subId2 = KeyguardUtils.getSubIdUsingPhoneId(KeyguardSimPinPukMeView.this.mPhoneId);
                        this.mResult = phoneService2.supplyPukReportResultForSubscriber(subId2, this.mPuk, this.mPin);
                    } else {
                        Log.d("KeyguardSimPinPukMeView", "phoneService is gone, skip supplyPukForSubscriber().");
                    }
                }
                if (this.mResult == null) {
                    KeyguardSimPinPukMeView.this.mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            CheckSimPinPuk.this.onSimCheckResponse(2, -1);
                        }
                    });
                    Log.d("KeyguardSimPinPukMeView", "there is an error with sim fw");
                } else {
                    Log.v("KeyguardSimPinPukMeView", "supplyPinReportResultForSubscriber returned: " + this.mResult[0] + " " + this.mResult[1]);
                    Log.d("KeyguardSimPinPukMeView", "CheckSimPinPuk.run(),mResult is true(success), so we postDelayed a timeout runnable object");
                    KeyguardSimPinPukMeView.this.mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            CheckSimPinPuk.this.onSimCheckResponse(CheckSimPinPuk.this.mResult[0], CheckSimPinPuk.this.mResult[1]);
                        }
                    });
                }
            } catch (RemoteException e) {
                KeyguardSimPinPukMeView.this.mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        CheckSimPinPuk.this.onSimCheckResponse(2, -1);
                    }
                });
            }
        }
    }

    private abstract class CheckSimMe extends Thread {
        private final String mPasswd;
        private int mResult;

        abstract void onSimMeCheckResponse(int i);

        protected CheckSimMe(String passwd, int phoneId) {
            this.mPasswd = passwd;
        }

        @Override
        public void run() {
            try {
                Log.d("KeyguardSimPinPukMeView", "CheckMe, mPhoneId =" + KeyguardSimPinPukMeView.this.mPhoneId);
                int subId = KeyguardUtils.getSubIdUsingPhoneId(KeyguardSimPinPukMeView.this.mPhoneId);
                this.mResult = ITelephonyEx.Stub.asInterface(ServiceManager.getService("phoneEx")).supplyNetworkDepersonalization(subId, this.mPasswd);
                Log.d("KeyguardSimPinPukMeView", "CheckMe, mPhoneId =" + KeyguardSimPinPukMeView.this.mPhoneId + " mResult=" + this.mResult);
                if (this.mResult == 0) {
                    Log.d("KeyguardSimPinPukMeView", "CheckSimMe.run(), VERIFY_RESULT_PASS == ret, so we postDelayed a timeout runnable object");
                }
                KeyguardSimPinPukMeView.this.mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        CheckSimMe.this.onSimMeCheckResponse(CheckSimMe.this.mResult);
                    }
                });
            } catch (RemoteException e) {
                KeyguardSimPinPukMeView.this.mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        CheckSimMe.this.onSimMeCheckResponse(2);
                    }
                });
            }
        }
    }

    private Dialog getSimUnlockProgressDialog() {
        if (this.mSimUnlockProgressDialog == null) {
            this.mSimUnlockProgressDialog = new ProgressDialog(this.mContext);
            String msg = this.mContext.getString(R$string.kg_sim_unlock_progress_dialog_message);
            this.mSimUnlockProgressDialog.setMessage(this.mIOperatorSIMString.getOperatorSIMString(msg, this.mPhoneId, IOperatorSIMString.SIMChangedTag.DELSIM, this.mContext));
            this.mSimUnlockProgressDialog.setIndeterminate(true);
            this.mSimUnlockProgressDialog.setCancelable(false);
            if (!(this.mContext instanceof Activity)) {
                this.mSimUnlockProgressDialog.getWindow().setType(2009);
            }
        }
        return this.mSimUnlockProgressDialog;
    }

    @Override
    protected void verifyPasswordAndUnlock() {
        String entry = this.mPasswordEntry.getText().toString();
        if (!validatePin(entry, false) && (this.mUpdateMonitor.getSimStateOfPhoneId(this.mPhoneId) == IccCardConstants.State.PIN_REQUIRED || (this.mUpdateMonitor.getSimStateOfPhoneId(this.mPhoneId) == IccCardConstants.State.NETWORK_LOCKED && KeyguardUtils.isMediatekSimMeLockSupport()))) {
            if (this.mUpdateMonitor.getSimStateOfPhoneId(this.mPhoneId) == IccCardConstants.State.PIN_REQUIRED) {
                this.mSecurityMessageDisplay.setMessage(R$string.kg_invalid_sim_pin_hint, true);
            } else {
                this.mSecurityMessageDisplay.setMessage(R$string.keyguard_code_length_prompt, true);
            }
            this.mPasswordEntry.reset(true, true);
            this.mPasswordEntry.setEnabled(true);
            this.mCallback.userActivity();
            return;
        }
        this.mPasswordEntry.setEnabled(true);
        dealWithPinOrPukUnlock();
    }

    private void dealWithPinOrPukUnlock() {
        if (this.mUpdateMonitor.getSimStateOfPhoneId(this.mPhoneId) == IccCardConstants.State.PIN_REQUIRED) {
            Log.d("KeyguardSimPinPukMeView", "onClick, check PIN, mPhoneId=" + this.mPhoneId);
            checkPin(this.mPhoneId);
        } else if (this.mUpdateMonitor.getSimStateOfPhoneId(this.mPhoneId) == IccCardConstants.State.PUK_REQUIRED) {
            Log.d("KeyguardSimPinPukMeView", "onClick, check PUK, mPhoneId=" + this.mPhoneId);
            checkPuk(this.mPhoneId);
        } else if (this.mUpdateMonitor.getSimStateOfPhoneId(this.mPhoneId) == IccCardConstants.State.NETWORK_LOCKED && KeyguardUtils.isMediatekSimMeLockSupport()) {
            Log.d("KeyguardSimPinPukMeView", "onClick, check ME, mPhoneId=" + this.mPhoneId);
            checkMe(this.mPhoneId);
        } else {
            Log.d("KeyguardSimPinPukMeView", "wrong status, mPhoneId=" + this.mPhoneId);
        }
    }

    private void checkPin(int phoneId) {
        getSimUnlockProgressDialog().show();
        Log.d("KeyguardSimPinPukMeView", "mSimCheckInProgress: " + this.mSimCheckInProgress);
        if (this.mSimCheckInProgress) {
            return;
        }
        this.mSimCheckInProgress = true;
        new CheckSimPinPuk(this, this.mPasswordEntry.getText().toString(), phoneId) {
            @Override
            void onSimCheckResponse(int result, int attemptsRemaining) {
                this.resetPasswordText(true, result != 0);
                if (result == 0) {
                    this.mKeyguardUtilExt.showToastWhenUnlockPinPuk(this.mContext, 501);
                    Log.d("KeyguardSimPinPukMeView", "checkPin() success");
                    this.mUpdateMonitor.reportSimUnlocked(this.mPhoneId);
                    this.mCallback.dismiss(true);
                } else {
                    if (this.mSimUnlockProgressDialog != null) {
                        this.mSimUnlockProgressDialog.hide();
                    }
                    if (result == 1) {
                        if (attemptsRemaining <= 2) {
                            this.getSimRemainingAttemptsDialog(attemptsRemaining).show();
                            this.mSecurityMessageDisplay.setMessage((CharSequence) this.getPinPasswordErrorMessage(attemptsRemaining), true);
                        } else {
                            this.mSecurityMessageDisplay.setMessage((CharSequence) this.getPinPasswordErrorMessage(attemptsRemaining), true);
                        }
                    } else {
                        this.mSecurityMessageDisplay.setMessage((CharSequence) this.getContext().getString(R$string.kg_password_pin_failed), true);
                    }
                    Log.d("KeyguardSimPinPukMeView", "verifyPasswordAndUnlock  CheckSimPin.onSimCheckResponse: " + result + " attemptsRemaining=" + attemptsRemaining);
                    this.resetPasswordText(true, result != 0);
                }
                this.mCallback.userActivity();
                this.mSimCheckInProgress = false;
            }
        }.start();
    }

    private void checkPuk(int phoneId) {
        updatePinEnterScreen();
        if (this.mUnlockEnterState != 4) {
            return;
        }
        getSimUnlockProgressDialog().show();
        Log.d("KeyguardSimPinPukMeView", "mSimCheckInProgress: " + this.mSimCheckInProgress);
        if (this.mSimCheckInProgress) {
            return;
        }
        this.mSimCheckInProgress = true;
        new CheckSimPinPuk(this, this.mPukText, this.mNewPinText, phoneId) {
            @Override
            void onSimCheckResponse(int result, int attemptsRemaining) {
                this.resetPasswordText(true, result != 0);
                if (result == 0) {
                    Log.d("KeyguardSimPinPukMeView", "checkPuk onSimCheckResponse, success!");
                    this.mKeyguardUtilExt.showToastWhenUnlockPinPuk(this.mContext, 502);
                    this.mUpdateMonitor.reportSimUnlocked(this.mPhoneId);
                    this.mCallback.dismiss(true);
                } else {
                    if (this.mSimUnlockProgressDialog != null) {
                        this.mSimUnlockProgressDialog.hide();
                    }
                    this.mUnlockEnterState = 1;
                    if (result == 1) {
                        if (attemptsRemaining <= 2) {
                            this.getPukRemainingAttemptsDialog(attemptsRemaining).show();
                            this.mSecurityMessageDisplay.setMessage((CharSequence) this.getPukPasswordErrorMessage(attemptsRemaining), true);
                        } else {
                            this.mSecurityMessageDisplay.setMessage((CharSequence) this.getPukPasswordErrorMessage(attemptsRemaining), true);
                        }
                    } else {
                        this.mSecurityMessageDisplay.setMessage((CharSequence) this.getContext().getString(R$string.kg_password_puk_failed), true);
                    }
                    Log.d("KeyguardSimPinPukMeView", "verifyPasswordAndUnlock  UpdateSim.onSimCheckResponse:  attemptsRemaining=" + attemptsRemaining);
                }
                this.mCallback.userActivity();
                this.mSimCheckInProgress = false;
            }
        }.start();
    }

    private void checkMe(int phoneId) {
        getSimUnlockProgressDialog().show();
        if (this.mSimCheckInProgress) {
            return;
        }
        this.mSimCheckInProgress = true;
        new CheckSimMe(this, this.mPasswordEntry.getText().toString(), phoneId) {
            @Override
            void onSimMeCheckResponse(int ret) {
                Log.d("KeyguardSimPinPukMeView", "checkMe onSimChangedResponse, ret = " + ret);
                if (ret == 0) {
                    Log.d("KeyguardSimPinPukMeView", "checkMe VERIFY_RESULT_PASS == ret(we had sent runnable before");
                    this.mUpdateMonitor.reportSimUnlocked(this.mPhoneId);
                    this.mCallback.dismiss(true);
                } else if (1 == ret) {
                    this.mSb.delete(0, this.mSb.length());
                    this.minusRetryMeCount(this.mPhoneId);
                    if (this.mSimUnlockProgressDialog != null) {
                        this.mSimUnlockProgressDialog.hide();
                    }
                    if (this.mUnlockEnterState == 5) {
                        if (this.getRetryMeCount(this.mPhoneId) == 0) {
                            this.setInputInvalidAlertDialog(this.mContext.getText(R$string.simlock_slot_locked_message), true);
                            this.mUpdateMonitor.setPinPukMeDismissFlagOfPhoneId(this.mPhoneId, true);
                            this.mCallback.dismiss(true);
                        } else {
                            int category = this.mUpdateMonitor.getSimMeCategoryOfPhoneId(this.mPhoneId);
                            this.mSb.append(this.mContext.getText(R$string.keyguard_wrong_code_input));
                            this.mSb.append(this.mContext.getText(R$string.simlock_entersimmelock));
                            this.mSb.append(this.strLockName[category] + this.getRetryMeString(this.mPhoneId));
                        }
                        Log.d("KeyguardSimPinPukMeView", "checkMe() - VERIFY_INCORRECT_PASSWORD == ret, mSecurityMessageDisplay.setMessage = " + this.mSb.toString());
                        this.mSecurityMessageDisplay.setMessage((CharSequence) this.mSb.toString(), true);
                        this.mPasswordEntry.reset(true, true);
                    }
                } else if (2 == ret) {
                    if (this.mSimUnlockProgressDialog != null) {
                        this.mSimUnlockProgressDialog.hide();
                    }
                    this.setInputInvalidAlertDialog("Exception happen, fail to unlock", true);
                    this.mUpdateMonitor.setPinPukMeDismissFlagOfPhoneId(this.mPhoneId, true);
                    this.mCallback.dismiss(true);
                }
                this.mCallback.userActivity();
                this.mSimCheckInProgress = false;
            }
        }.start();
    }

    public Dialog getSimRemainingAttemptsDialog(int remaining) {
        String msg = getPinPasswordErrorMessage(remaining);
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

    public void simStateReadyProcess() {
        this.mNextRepollStatePhoneId = getNextRepollStatePhoneId();
        Log.d("KeyguardSimPinPukMeView", "simStateReadyProcess mNextRepollStatePhoneId =" + this.mNextRepollStatePhoneId);
        if (this.mNextRepollStatePhoneId != -1) {
            try {
                getSimUnlockProgressDialog().show();
                Log.d("KeyguardSimPinPukMeView", "repollIccStateForNetworkLock phoneId =" + this.mNextRepollStatePhoneId);
                int subId = KeyguardUtils.getSubIdUsingPhoneId(this.mNextRepollStatePhoneId);
                ITelephonyEx.Stub.asInterface(ServiceManager.getService("phoneEx")).repollIccStateForNetworkLock(subId, true);
                return;
            } catch (RemoteException e) {
                Log.d("KeyguardSimPinPukMeView", "repollIccStateForNetworkLock exception caught");
                return;
            }
        }
        this.mCallback.dismiss(true);
    }

    private int getNextRepollStatePhoneId() {
        if (IccCardConstants.State.NETWORK_LOCKED == this.mLastSimState && KeyguardUtils.isMediatekSimMeLockSupport()) {
            for (int i = 0; i < KeyguardUtils.getNumOfPhone(); i++) {
                if (this.mSecurityModel.isPinPukOrMeRequiredOfPhoneId(i)) {
                    IccCardConstants.State simState = this.mUpdateMonitor.getSimStateOfPhoneId(i);
                    if (simState == IccCardConstants.State.NETWORK_LOCKED) {
                        return i;
                    }
                    return -1;
                }
            }
            return -1;
        }
        return -1;
    }

    public static class Toast {
        final Context mContext;
        private INotificationManager mService;
        View mView;
        int mY;
        final Handler mHandler = new Handler();
        int mGravity = 81;
        final TN mTN = new TN();

        public Toast(Context context) {
            this.mContext = context;
            this.mY = context.getResources().getDimensionPixelSize(R.dimen.accessibility_magnification_thumbnail_padding);
        }

        public static Toast makeText(Context context, CharSequence text) {
            Toast result = new Toast(context);
            LayoutInflater inflate = (LayoutInflater) context.getSystemService("layout_inflater");
            View v = inflate.inflate(R.layout.overlay_display_window, (ViewGroup) null);
            TextView tv = (TextView) v.findViewById(R.id.message);
            tv.setText(text);
            result.mView = v;
            return result;
        }

        public void show() {
            if (this.mView == null) {
                throw new RuntimeException("setView must have been called");
            }
            INotificationManager service = getService();
            String pkg = this.mContext.getPackageName();
            TN tn = this.mTN;
            try {
                service.enqueueToast(pkg, tn, 0);
            } catch (RemoteException e) {
            }
        }

        private INotificationManager getService() {
            if (this.mService != null) {
                return this.mService;
            }
            this.mService = INotificationManager.Stub.asInterface(ServiceManager.getService("notification"));
            return this.mService;
        }

        private class TN extends ITransientNotification.Stub {
            WindowManagerImpl mWM;
            final Runnable mShow = new Runnable() {
                @Override
                public void run() {
                    TN.this.handleShow();
                }
            };
            final Runnable mHide = new Runnable() {
                @Override
                public void run() {
                    TN.this.handleHide();
                }
            };
            private final WindowManager.LayoutParams mParams = new WindowManager.LayoutParams();

            TN() {
                WindowManager.LayoutParams params = this.mParams;
                params.height = -2;
                params.width = -2;
                params.flags = 152;
                params.format = -3;
                params.windowAnimations = R.style.Animation.Toast;
                params.type = 2009;
                params.setTitle("Toast");
            }

            public void show() {
                Toast.this.mHandler.post(this.mShow);
            }

            public void hide() {
                Toast.this.mHandler.post(this.mHide);
            }

            public void handleShow() {
                this.mWM = (WindowManagerImpl) Toast.this.mContext.getSystemService("window");
                int gravity = Toast.this.mGravity;
                this.mParams.gravity = gravity;
                if ((gravity & 7) == 7) {
                    this.mParams.horizontalWeight = 1.0f;
                }
                if ((gravity & 112) == 112) {
                    this.mParams.verticalWeight = 1.0f;
                }
                this.mParams.y = Toast.this.mY;
                if (Toast.this.mView == null) {
                    return;
                }
                if (Toast.this.mView.getParent() != null) {
                    this.mWM.removeView(Toast.this.mView);
                }
                this.mWM.addView(Toast.this.mView, this.mParams);
            }

            public void handleHide() {
                if (Toast.this.mView == null) {
                    return;
                }
                if (Toast.this.mView.getParent() != null) {
                    this.mWM.removeView(Toast.this.mView);
                }
                Toast.this.mView = null;
            }
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        Log.d("KeyguardSimPinPukMeView", "onWindowFocusChanged(hasWindowFocus = " + hasWindowFocus + ")");
        if (!hasWindowFocus) {
            return;
        }
        resetPasswordText(true, false);
        KeyguardUtils.requestImeStatusRefresh(this.mContext);
    }

    @Override
    public void startAppearAnimation() {
    }

    @Override
    public boolean startDisappearAnimation(Runnable finishRunnable) {
        return false;
    }
}
