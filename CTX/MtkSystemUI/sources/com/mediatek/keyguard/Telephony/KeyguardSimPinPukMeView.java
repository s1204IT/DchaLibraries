package com.mediatek.keyguard.Telephony;

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
import android.os.IBinder;
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
import android.widget.Button;
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
import com.android.systemui.R;
import com.mediatek.internal.telephony.IMtkTelephonyEx;
import com.mediatek.keyguard.ext.IKeyguardUtilExt;
import com.mediatek.keyguard.ext.IOperatorSIMString;
import com.mediatek.keyguard.ext.OpKeyguardCustomizationFactoryBase;

public class KeyguardSimPinPukMeView extends KeyguardPinBasedInputView {
    private Button mDismissButton;
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

    public KeyguardSimPinPukMeView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
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
            public void onSimStateChangedUsingPhoneId(int i, IccCardConstants.State state) {
                Log.d("KeyguardSimPinPukMeView", "onSimStateChangedUsingPhoneId: " + state + ", phoneId = " + i + ", mPhoneId = " + KeyguardSimPinPukMeView.this.mPhoneId);
                StringBuilder sb = new StringBuilder();
                sb.append("onSimStateChangedUsingPhoneId: mCallback = ");
                sb.append(KeyguardSimPinPukMeView.this.mCallback);
                Log.d("KeyguardSimPinPukMeView", sb.toString());
                if (i != KeyguardSimPinPukMeView.this.mPhoneId) {
                    if (i == KeyguardSimPinPukMeView.this.mNextRepollStatePhoneId) {
                        Log.d("KeyguardSimPinPukMeView", "onSimStateChanged: mNextRepollStatePhoneId = " + KeyguardSimPinPukMeView.this.mNextRepollStatePhoneId);
                        if (KeyguardSimPinPukMeView.this.mSimUnlockProgressDialog != null) {
                            KeyguardSimPinPukMeView.this.mSimUnlockProgressDialog.hide();
                        }
                        if (IccCardConstants.State.READY != state) {
                            KeyguardSimPinPukMeView.this.mCallback.dismiss(true, KeyguardUpdateMonitor.getCurrentUser());
                            KeyguardSimPinPukMeView.this.mLastSimState = state;
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
                if (IccCardConstants.State.READY == state) {
                    KeyguardSimPinPukMeView.this.simStateReadyProcess();
                } else if (IccCardConstants.State.NOT_READY == state || IccCardConstants.State.ABSENT == state) {
                    Log.d("KeyguardSimPinPukMeView", "onSimStateChangedUsingPhoneId: not ready, phoneId = " + i);
                    KeyguardSimPinPukMeView.this.mCallback.dismiss(true, KeyguardUpdateMonitor.getCurrentUser());
                    KeyguardSimPinPukMeView.this.mSimCheckInProgress = false;
                    Log.d("KeyguardSimPinPukMeView", "set mSimCheckInProgress false");
                } else if (IccCardConstants.State.NETWORK_LOCKED == state) {
                    if (!KeyguardUtils.isMediatekSimMeLockSupport()) {
                        KeyguardSimPinPukMeView.this.mCallback.dismiss(true, KeyguardUpdateMonitor.getCurrentUser());
                    } else if (KeyguardSimPinPukMeView.this.getRetryMeCount(KeyguardSimPinPukMeView.this.mPhoneId) == 0) {
                        Log.d("KeyguardSimPinPukMeView", "onSimStateChanged: ME retrycount is 0, dismiss it");
                        KeyguardSimPinPukMeView.this.mCallback.dismiss(true, KeyguardUpdateMonitor.getCurrentUser());
                    }
                }
                KeyguardSimPinPukMeView.this.mLastSimState = state;
                Log.d("KeyguardSimPinPukMeView", "assign mLastSimState=" + KeyguardSimPinPukMeView.this.mLastSimState);
            }

            @Override
            public void onAirPlaneModeChanged(boolean z) {
                Log.d("KeyguardSimPinPukMeView", "onAirPlaneModeChanged(airPlaneModeEnabled = " + z + ")");
                if (z) {
                    Log.d("KeyguardSimPinPukMeView", "Flight-Mode turns on & keyguard is showing, dismiss keyguard.");
                    KeyguardSimPinPukMeView.this.mPasswordEntry.reset(true, true);
                    KeyguardSimPinPukMeView.this.mCallback.userActivity();
                    KeyguardSimPinPukMeView.this.mCallback.dismiss(true, KeyguardUpdateMonitor.getCurrentUser());
                }
            }
        };
        this.mKeyguardUtils = new KeyguardUtils(context);
        this.mSb = new StringBuffer();
        this.mUpdateMonitor = KeyguardUpdateMonitor.getInstance(getContext());
        this.mSecurityModel = new KeyguardSecurityModel(getContext());
        try {
            this.mKeyguardUtilExt = OpKeyguardCustomizationFactoryBase.getOpFactory(context).makeKeyguardUtil();
            this.mIOperatorSIMString = OpKeyguardCustomizationFactoryBase.getOpFactory(context).makeOperatorSIMString();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setPhoneId(int i) {
        this.mPhoneId = i;
        Log.i("KeyguardSimPinPukMeView", "setPhoneId=" + i);
        resetState();
        if (this.mSimCardDialog != null) {
            if (this.mSimCardDialog.isShowing()) {
                this.mSimCardDialog.dismiss();
            }
            this.mSimCardDialog = null;
        }
    }

    @Override
    public void resetState() {
        resetState(false);
    }

    public void resetState(boolean z) {
        String string;
        Log.v("KeyguardSimPinPukMeView", "Resetting state");
        super.resetState();
        TextView textView = (TextView) findViewById(R.id.slot_num_text);
        textView.setText(this.mContext.getString(R.string.kg_slot_id, Integer.valueOf(this.mPhoneId + 1)) + " ");
        Resources resources = getResources();
        String string2 = "";
        int numOfPhone = KeyguardUtils.getNumOfPhone();
        IccCardConstants.State simStateOfPhoneId = this.mUpdateMonitor.getSimStateOfPhoneId(this.mPhoneId);
        showDismissIfNeed(simStateOfPhoneId);
        int iconTint = -1;
        if (numOfPhone < 2) {
            if (simStateOfPhoneId == IccCardConstants.State.PIN_REQUIRED) {
                string2 = resources.getString(R.string.kg_sim_pin_instructions);
                this.mUnlockEnterState = 0;
            } else if (simStateOfPhoneId == IccCardConstants.State.PUK_REQUIRED) {
                string2 = resources.getString(R.string.kg_puk_enter_puk_hint);
                this.mUnlockEnterState = 1;
            } else if (IccCardConstants.State.NETWORK_LOCKED == simStateOfPhoneId && KeyguardUtils.isMediatekSimMeLockSupport()) {
                string2 = resources.getString(R.string.simlock_entersimmelock) + this.strLockName[this.mUpdateMonitor.getSimMeCategoryOfPhoneId(this.mPhoneId)] + getRetryMeString(this.mPhoneId);
                this.mUnlockEnterState = 5;
            }
        } else {
            int subIdUsingPhoneId = KeyguardUtils.getSubIdUsingPhoneId(this.mPhoneId);
            SubscriptionInfo subscriptionInfoForSubId = this.mUpdateMonitor.getSubscriptionInfoForSubId(subIdUsingPhoneId, z);
            String displayName = subscriptionInfoForSubId != null ? subscriptionInfoForSubId.getDisplayName() : "";
            if (subscriptionInfoForSubId == null) {
                displayName = "CARD " + Integer.toString(this.mPhoneId + 1);
                Log.d("KeyguardSimPinPukMeView", "we set a displayname");
            }
            Log.d("KeyguardSimPinPukMeView", "resetState() - subId = " + subIdUsingPhoneId + ", displayName = " + ((Object) displayName));
            if (simStateOfPhoneId == IccCardConstants.State.PIN_REQUIRED) {
                string = resources.getString(R.string.kg_sim_pin_instructions_multi, displayName);
                this.mUnlockEnterState = 0;
            } else if (simStateOfPhoneId == IccCardConstants.State.PUK_REQUIRED) {
                string = resources.getString(R.string.kg_puk_enter_puk_hint_multi, displayName);
                this.mUnlockEnterState = 1;
            } else {
                if (IccCardConstants.State.NETWORK_LOCKED == simStateOfPhoneId && KeyguardUtils.isMediatekSimMeLockSupport()) {
                    string = resources.getString(R.string.simlock_entersimmelock) + this.strLockName[this.mUpdateMonitor.getSimMeCategoryOfPhoneId(this.mPhoneId)] + getRetryMeString(this.mPhoneId);
                    this.mUnlockEnterState = 5;
                }
                if (subscriptionInfoForSubId != null) {
                    iconTint = subscriptionInfoForSubId.getIconTint();
                }
            }
            string2 = string;
            if (subscriptionInfoForSubId != null) {
            }
        }
        this.mKeyguardUtilExt.customizePinPukLockView(this.mPhoneId, this.mSimImageView, textView);
        this.mSimImageView.setImageTintList(ColorStateList.valueOf(iconTint));
        String operatorSIMString = this.mIOperatorSIMString.getOperatorSIMString(string2, this.mPhoneId, IOperatorSIMString.SIMChangedTag.DELSIM, this.mContext);
        Log.d("KeyguardSimPinPukMeView", "resetState() - mSecurityMessageDisplay.setMessage = " + operatorSIMString);
        this.mSecurityMessageDisplay.setMessage(operatorSIMString);
    }

    private String getPinPasswordErrorMessage(int i) {
        String string;
        if (i == 0) {
            string = getContext().getString(R.string.kg_password_wrong_pin_code_pukked);
        } else if (i > 0) {
            string = getContext().getResources().getQuantityString(R.plurals.kg_password_wrong_pin_code, i, Integer.valueOf(i));
        } else {
            string = getContext().getString(R.string.kg_password_pin_failed);
        }
        Log.d("KeyguardSimPinPukMeView", "getPinPasswordErrorMessage: attemptsRemaining=" + i + " displayMessage=" + string);
        return string;
    }

    private String getPukPasswordErrorMessage(int i) {
        String string;
        if (i == 0) {
            string = getContext().getString(R.string.kg_password_wrong_puk_code_dead);
        } else if (i > 0) {
            string = getContext().getResources().getQuantityString(R.plurals.kg_password_wrong_puk_code, i, Integer.valueOf(i));
        } else {
            string = getContext().getString(R.string.kg_password_puk_failed);
        }
        Log.d("KeyguardSimPinPukMeView", "getPukPasswordErrorMessage: attemptsRemaining=" + i + " displayMessage=" + string);
        return string;
    }

    @Override
    protected boolean shouldLockout(long j) {
        return false;
    }

    @Override
    protected int getPasswordTextViewId() {
        return R.id.simPinPukMeEntry;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mPhoneId = -1;
        if (this.mEcaView instanceof EmergencyCarrierArea) {
            ((EmergencyCarrierArea) this.mEcaView).setCarrierTextVisible(true);
            this.mKeyguardUtilExt.customizeCarrierTextGravity((TextView) this.mEcaView.findViewById(R.id.carrier_text));
        }
        this.mSimImageView = (ImageView) findViewById(R.id.keyguard_sim);
        this.mDismissButton = (Button) findViewById(R.id.key_dismiss);
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
    public void onResume(int i) {
        if (this.mSimUnlockProgressDialog != null) {
            this.mSimUnlockProgressDialog.dismiss();
            this.mSimUnlockProgressDialog = null;
        }
        InputMethodManager inputMethodManager = (InputMethodManager) this.mContext.getSystemService("input_method");
        if (inputMethodManager.isActive()) {
            Log.i("KeyguardSimPinPukMeView", "IME is showing, we should hide it");
            inputMethodManager.hideSoftInputFromWindow(getWindowToken(), 2);
        }
    }

    @Override
    public void onPause() {
        if (this.mSimUnlockProgressDialog != null) {
            this.mSimUnlockProgressDialog.dismiss();
            this.mSimUnlockProgressDialog = null;
        }
    }

    private void setInputInvalidAlertDialog(CharSequence charSequence, boolean z) {
        StringBuilder sb = new StringBuilder(charSequence);
        if (z) {
            AlertDialog alertDialogCreate = new AlertDialog.Builder(this.mContext).setMessage(sb).setPositiveButton(android.R.string.ok, (DialogInterface.OnClickListener) null).setCancelable(true).create();
            alertDialogCreate.getWindow().setType(2009);
            alertDialogCreate.getWindow().addFlags(2);
            alertDialogCreate.show();
            return;
        }
        Toast.makeText(this.mContext, sb).show();
    }

    private String getRetryMeString(int i) {
        return "(" + this.mContext.getString(R.string.retries_left, Integer.valueOf(getRetryMeCount(i))) + ")";
    }

    private int getRetryMeCount(int i) {
        return this.mUpdateMonitor.getSimMeLeftRetryCountOfPhoneId(i);
    }

    private void minusRetryMeCount(int i) {
        this.mUpdateMonitor.minusSimMeLeftRetryCountOfPhoneId(i);
    }

    private boolean validatePin(String str, boolean z) {
        int i;
        if (!z) {
            i = 4;
        } else {
            i = 8;
        }
        if (str == null || str.length() < i || str.length() > 8) {
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
                    this.mSb.append(this.mContext.getText(R.string.keyguard_password_enter_new_pin_code));
                    Log.d("KeyguardSimPinPukMeView", "updatePinEnterScreen() - STATE_ENTER_UK, validatePin = true ,mSecurityMessageDisplay.setMessage = " + this.mSb.toString());
                    this.mSecurityMessageDisplay.setMessage(this.mSb.toString());
                } else {
                    Log.d("KeyguardSimPinPukMeView", "updatePinEnterScreen() - STATE_ENTER_UK, validatePin = false ,mSecurityMessageDisplay.setMessage = R.string.invalidPuk");
                    this.mSecurityMessageDisplay.setMessage(R.string.invalidPuk);
                }
                break;
            case 2:
                this.mNewPinText = this.mPasswordEntry.getText().toString();
                if (validatePin(this.mNewPinText, false)) {
                    this.mUnlockEnterState = 3;
                    this.mSb.delete(0, this.mSb.length());
                    this.mSb.append(this.mContext.getText(R.string.keyguard_password_Confirm_pin_code));
                    Log.d("KeyguardSimPinPukMeView", "updatePinEnterScreen() - STATE_ENTER_NEW, validatePin = true ,mSecurityMessageDisplay.setMessage = " + this.mSb.toString());
                    this.mSecurityMessageDisplay.setMessage(this.mSb.toString());
                } else {
                    Log.d("KeyguardSimPinPukMeView", "updatePinEnterScreen() - STATE_ENTER_NEW, validatePin = false ,mSecurityMessageDisplay.setMessage = R.string.keyguard_code_length_prompt");
                    this.mSecurityMessageDisplay.setMessage(R.string.keyguard_code_length_prompt);
                }
                break;
            case 3:
                if (!this.mNewPinText.equals(this.mPasswordEntry.getText().toString())) {
                    this.mUnlockEnterState = 2;
                    this.mSb.delete(0, this.mSb.length());
                    this.mSb.append(this.mContext.getText(R.string.keyguard_code_donnot_mismatch));
                    this.mSb.append(this.mContext.getText(R.string.keyguard_password_enter_new_pin_code));
                    Log.d("KeyguardSimPinPukMeView", "updatePinEnterScreen() - STATE_REENTER_NEW, true ,mSecurityMessageDisplay.setMessage = " + this.mSb.toString());
                    this.mSecurityMessageDisplay.setMessage(this.mSb.toString());
                } else {
                    Log.d("KeyguardSimPinPukMeView", "updatePinEnterScreen() - STATE_REENTER_NEW, false ,mSecurityMessageDisplay.setMessage = empty string.");
                    this.mUnlockEnterState = 4;
                    this.mSecurityMessageDisplay.setMessage("");
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

        protected CheckSimPinPuk(String str, int i) {
            this.mPin = str;
            this.mPuk = null;
        }

        protected CheckSimPinPuk(String str, String str2, int i) {
            this.mPin = str2;
            this.mPuk = str;
        }

        @Override
        public void run() {
            try {
                Log.d("KeyguardSimPinPukMeView", "CheckSimPinPuk, mPhoneId =" + KeyguardSimPinPukMeView.this.mPhoneId);
                if (KeyguardSimPinPukMeView.this.mUpdateMonitor.getSimStateOfPhoneId(KeyguardSimPinPukMeView.this.mPhoneId) != IccCardConstants.State.PIN_REQUIRED) {
                    if (KeyguardSimPinPukMeView.this.mUpdateMonitor.getSimStateOfPhoneId(KeyguardSimPinPukMeView.this.mPhoneId) == IccCardConstants.State.PUK_REQUIRED) {
                        ITelephony iTelephonyAsInterface = ITelephony.Stub.asInterface(ServiceManager.checkService("phone"));
                        if (iTelephonyAsInterface != null) {
                            this.mResult = iTelephonyAsInterface.supplyPukReportResultForSubscriber(KeyguardUtils.getSubIdUsingPhoneId(KeyguardSimPinPukMeView.this.mPhoneId), this.mPuk, this.mPin);
                        } else {
                            Log.d("KeyguardSimPinPukMeView", "phoneService is gone, skip supplyPukForSubscriber().");
                        }
                    }
                } else {
                    ITelephony iTelephonyAsInterface2 = ITelephony.Stub.asInterface(ServiceManager.checkService("phone"));
                    if (iTelephonyAsInterface2 != null) {
                        this.mResult = iTelephonyAsInterface2.supplyPinReportResultForSubscriber(KeyguardUtils.getSubIdUsingPhoneId(KeyguardSimPinPukMeView.this.mPhoneId), this.mPin);
                    } else {
                        Log.d("KeyguardSimPinPukMeView", "phoneService is gone, skip supplyPinForSubscriber().");
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
                    return;
                }
                Log.v("KeyguardSimPinPukMeView", "supplyPinReportResultForSubscriber returned: " + this.mResult[0] + " " + this.mResult[1]);
                Log.d("KeyguardSimPinPukMeView", "CheckSimPinPuk.run(),mResult is true(success), so we postDelayed a timeout runnable object");
                KeyguardSimPinPukMeView.this.mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        CheckSimPinPuk.this.onSimCheckResponse(CheckSimPinPuk.this.mResult[0], CheckSimPinPuk.this.mResult[1]);
                    }
                });
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

        protected CheckSimMe(String str, int i) {
            this.mPasswd = str;
        }

        @Override
        public void run() {
            try {
                Log.d("KeyguardSimPinPukMeView", "CheckMe, mPhoneId =" + KeyguardSimPinPukMeView.this.mPhoneId);
                this.mResult = IMtkTelephonyEx.Stub.asInterface(ServiceManager.getService("phoneEx")).supplyNetworkDepersonalization(KeyguardUtils.getSubIdUsingPhoneId(KeyguardSimPinPukMeView.this.mPhoneId), this.mPasswd);
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
            this.mSimUnlockProgressDialog.setMessage(this.mIOperatorSIMString.getOperatorSIMString(this.mContext.getString(R.string.kg_sim_unlock_progress_dialog_message), this.mPhoneId, IOperatorSIMString.SIMChangedTag.DELSIM, this.mContext));
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
        if (!validatePin(this.mPasswordEntry.getText().toString(), false) && (this.mUpdateMonitor.getSimStateOfPhoneId(this.mPhoneId) == IccCardConstants.State.PIN_REQUIRED || (this.mUpdateMonitor.getSimStateOfPhoneId(this.mPhoneId) == IccCardConstants.State.NETWORK_LOCKED && KeyguardUtils.isMediatekSimMeLockSupport()))) {
            if (this.mUpdateMonitor.getSimStateOfPhoneId(this.mPhoneId) == IccCardConstants.State.PIN_REQUIRED) {
                this.mSecurityMessageDisplay.setMessage(R.string.kg_invalid_sim_pin_hint);
            } else {
                this.mSecurityMessageDisplay.setMessage(R.string.keyguard_code_length_prompt);
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
            Log.d("KeyguardSimPinPukMeView", "onClick, checkPin, mPhoneId=" + this.mPhoneId);
            checkPin(this.mPhoneId);
            return;
        }
        if (this.mUpdateMonitor.getSimStateOfPhoneId(this.mPhoneId) == IccCardConstants.State.PUK_REQUIRED) {
            Log.d("KeyguardSimPinPukMeView", "onClick, checkPuk, mPhoneId=" + this.mPhoneId);
            checkPuk(this.mPhoneId);
            return;
        }
        if (this.mUpdateMonitor.getSimStateOfPhoneId(this.mPhoneId) == IccCardConstants.State.NETWORK_LOCKED && KeyguardUtils.isMediatekSimMeLockSupport()) {
            Log.d("KeyguardSimPinPukMeView", "onClick, checkMe, mPhoneId=" + this.mPhoneId);
            checkMe(this.mPhoneId);
            return;
        }
        Log.d("KeyguardSimPinPukMeView", "wrong status, mPhoneId=" + this.mPhoneId);
    }

    private void checkPin(int i) {
        getSimUnlockProgressDialog().show();
        Log.d("KeyguardSimPinPukMeView", "mSimCheckInProgress: " + this.mSimCheckInProgress);
        if (!this.mSimCheckInProgress) {
            this.mSimCheckInProgress = true;
            new CheckSimPinPuk(this.mPasswordEntry.getText().toString(), i) {
                @Override
                void onSimCheckResponse(int i2, int i3) {
                    KeyguardSimPinPukMeView.this.resetPasswordText(true, i2 != 0);
                    if (i2 == 0) {
                        KeyguardSimPinPukMeView.this.mKeyguardUtilExt.showToastWhenUnlockPinPuk(KeyguardSimPinPukMeView.this.mContext, 501);
                        Log.d("KeyguardSimPinPukMeView", "checkPin() success");
                        KeyguardSimPinPukMeView.this.mUpdateMonitor.reportSimUnlocked(KeyguardSimPinPukMeView.this.mPhoneId);
                        KeyguardSimPinPukMeView.this.mCallback.dismiss(true, KeyguardUpdateMonitor.getCurrentUser());
                    } else {
                        if (KeyguardSimPinPukMeView.this.mSimUnlockProgressDialog != null) {
                            KeyguardSimPinPukMeView.this.mSimUnlockProgressDialog.hide();
                        }
                        if (i2 != 1) {
                            KeyguardSimPinPukMeView.this.mSecurityMessageDisplay.setMessage(KeyguardSimPinPukMeView.this.getContext().getString(R.string.kg_password_pin_failed));
                        } else if (i3 <= 2) {
                            KeyguardSimPinPukMeView.this.getSimRemainingAttemptsDialog(i3).show();
                            KeyguardSimPinPukMeView.this.mSecurityMessageDisplay.setMessage(KeyguardSimPinPukMeView.this.getPinPasswordErrorMessage(i3));
                        } else {
                            KeyguardSimPinPukMeView.this.mSecurityMessageDisplay.setMessage(KeyguardSimPinPukMeView.this.getPinPasswordErrorMessage(i3));
                        }
                        Log.d("KeyguardSimPinPukMeView", "verifyPasswordAndUnlock  CheckSimPin.onSimCheckResponse: " + i2 + " attemptsRemaining=" + i3);
                        KeyguardSimPinPukMeView.this.resetPasswordText(true, i2 != 0);
                    }
                    KeyguardSimPinPukMeView.this.mCallback.userActivity();
                    KeyguardSimPinPukMeView.this.mSimCheckInProgress = false;
                }
            }.start();
        }
    }

    private void checkPuk(int i) {
        updatePinEnterScreen();
        if (this.mUnlockEnterState != 4) {
            return;
        }
        getSimUnlockProgressDialog().show();
        Log.d("KeyguardSimPinPukMeView", "mSimCheckInProgress: " + this.mSimCheckInProgress);
        if (!this.mSimCheckInProgress) {
            this.mSimCheckInProgress = true;
            new CheckSimPinPuk(this.mPukText, this.mNewPinText, i) {
                @Override
                void onSimCheckResponse(int i2, int i3) {
                    KeyguardSimPinPukMeView.this.resetPasswordText(true, i2 != 0);
                    if (i2 != 0) {
                        if (KeyguardSimPinPukMeView.this.mSimUnlockProgressDialog != null) {
                            KeyguardSimPinPukMeView.this.mSimUnlockProgressDialog.hide();
                        }
                        KeyguardSimPinPukMeView.this.mUnlockEnterState = 1;
                        if (i2 != 1) {
                            KeyguardSimPinPukMeView.this.mSecurityMessageDisplay.setMessage(KeyguardSimPinPukMeView.this.getContext().getString(R.string.kg_password_puk_failed));
                        } else if (i3 <= 2) {
                            KeyguardSimPinPukMeView.this.getPukRemainingAttemptsDialog(i3).show();
                            KeyguardSimPinPukMeView.this.mSecurityMessageDisplay.setMessage(KeyguardSimPinPukMeView.this.getPukPasswordErrorMessage(i3));
                        } else {
                            KeyguardSimPinPukMeView.this.mSecurityMessageDisplay.setMessage(KeyguardSimPinPukMeView.this.getPukPasswordErrorMessage(i3));
                        }
                        Log.d("KeyguardSimPinPukMeView", "verifyPasswordAndUnlock  UpdateSim.onSimCheckResponse:  attemptsRemaining=" + i3);
                    } else {
                        Log.d("KeyguardSimPinPukMeView", "checkPuk onSimCheckResponse, success!");
                        KeyguardSimPinPukMeView.this.mKeyguardUtilExt.showToastWhenUnlockPinPuk(KeyguardSimPinPukMeView.this.mContext, 502);
                        KeyguardSimPinPukMeView.this.mUpdateMonitor.reportSimUnlocked(KeyguardSimPinPukMeView.this.mPhoneId);
                        KeyguardSimPinPukMeView.this.mCallback.dismiss(true, KeyguardUpdateMonitor.getCurrentUser());
                    }
                    KeyguardSimPinPukMeView.this.mCallback.userActivity();
                    KeyguardSimPinPukMeView.this.mSimCheckInProgress = false;
                }
            }.start();
        }
    }

    private void checkMe(int i) {
        getSimUnlockProgressDialog().show();
        if (!this.mSimCheckInProgress) {
            this.mSimCheckInProgress = true;
            new CheckSimMe(this.mPasswordEntry.getText().toString(), i) {
                @Override
                void onSimMeCheckResponse(int i2) {
                    Log.d("KeyguardSimPinPukMeView", "checkMe onSimChangedResponse, ret = " + i2);
                    if (i2 == 0) {
                        Log.d("KeyguardSimPinPukMeView", "checkMe VERIFY_RESULT_PASS == ret(we had sent runnable before");
                        KeyguardSimPinPukMeView.this.mUpdateMonitor.reportSimUnlocked(KeyguardSimPinPukMeView.this.mPhoneId);
                        KeyguardSimPinPukMeView.this.mCallback.dismiss(true, KeyguardUpdateMonitor.getCurrentUser());
                    } else if (1 == i2) {
                        KeyguardSimPinPukMeView.this.mSb.delete(0, KeyguardSimPinPukMeView.this.mSb.length());
                        KeyguardSimPinPukMeView.this.minusRetryMeCount(KeyguardSimPinPukMeView.this.mPhoneId);
                        if (KeyguardSimPinPukMeView.this.mSimUnlockProgressDialog != null) {
                            KeyguardSimPinPukMeView.this.mSimUnlockProgressDialog.hide();
                        }
                        if (KeyguardSimPinPukMeView.this.mUnlockEnterState == 5) {
                            if (KeyguardSimPinPukMeView.this.getRetryMeCount(KeyguardSimPinPukMeView.this.mPhoneId) == 0) {
                                KeyguardSimPinPukMeView.this.setInputInvalidAlertDialog(KeyguardSimPinPukMeView.this.mContext.getText(R.string.simlock_slot_locked_message), true);
                                KeyguardSimPinPukMeView.this.mCallback.dismiss(true, KeyguardUpdateMonitor.getCurrentUser());
                            } else {
                                int simMeCategoryOfPhoneId = KeyguardSimPinPukMeView.this.mUpdateMonitor.getSimMeCategoryOfPhoneId(KeyguardSimPinPukMeView.this.mPhoneId);
                                KeyguardSimPinPukMeView.this.mSb.append(KeyguardSimPinPukMeView.this.mContext.getText(R.string.keyguard_wrong_code_input));
                                KeyguardSimPinPukMeView.this.mSb.append(KeyguardSimPinPukMeView.this.mContext.getText(R.string.simlock_entersimmelock));
                                KeyguardSimPinPukMeView.this.mSb.append(KeyguardSimPinPukMeView.this.strLockName[simMeCategoryOfPhoneId] + KeyguardSimPinPukMeView.this.getRetryMeString(KeyguardSimPinPukMeView.this.mPhoneId));
                            }
                            Log.d("KeyguardSimPinPukMeView", "checkMe() - VERIFY_INCORRECT_PASSWORD == ret, mSecurityMessageDisplay.setMessage = " + KeyguardSimPinPukMeView.this.mSb.toString());
                            KeyguardSimPinPukMeView.this.mSecurityMessageDisplay.setMessage(KeyguardSimPinPukMeView.this.mSb.toString());
                            KeyguardSimPinPukMeView.this.mPasswordEntry.reset(true, true);
                        }
                    } else if (2 == i2) {
                        if (KeyguardSimPinPukMeView.this.mSimUnlockProgressDialog != null) {
                            KeyguardSimPinPukMeView.this.mSimUnlockProgressDialog.hide();
                        }
                        KeyguardSimPinPukMeView.this.setInputInvalidAlertDialog("Exception happen, fail to unlock", true);
                        KeyguardSimPinPukMeView.this.mCallback.dismiss(true, KeyguardUpdateMonitor.getCurrentUser());
                    }
                    KeyguardSimPinPukMeView.this.mCallback.userActivity();
                    KeyguardSimPinPukMeView.this.mSimCheckInProgress = false;
                }
            }.start();
        }
    }

    private Dialog getSimRemainingAttemptsDialog(int i) {
        String pinPasswordErrorMessage = getPinPasswordErrorMessage(i);
        if (this.mRemainingAttemptsDialog == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this.mContext);
            builder.setMessage(pinPasswordErrorMessage);
            builder.setCancelable(false);
            builder.setNeutralButton(R.string.ok, (DialogInterface.OnClickListener) null);
            this.mRemainingAttemptsDialog = builder.create();
            this.mRemainingAttemptsDialog.getWindow().setType(2009);
        } else {
            this.mRemainingAttemptsDialog.setMessage(pinPasswordErrorMessage);
        }
        return this.mRemainingAttemptsDialog;
    }

    private Dialog getPukRemainingAttemptsDialog(int i) {
        String pukPasswordErrorMessage = getPukPasswordErrorMessage(i);
        if (this.mRemainingAttemptsDialog == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this.mContext);
            builder.setMessage(pukPasswordErrorMessage);
            builder.setCancelable(false);
            builder.setNeutralButton(R.string.ok, (DialogInterface.OnClickListener) null);
            this.mRemainingAttemptsDialog = builder.create();
            this.mRemainingAttemptsDialog.getWindow().setType(2009);
        } else {
            this.mRemainingAttemptsDialog.setMessage(pukPasswordErrorMessage);
        }
        return this.mRemainingAttemptsDialog;
    }

    private void simStateReadyProcess() {
        this.mNextRepollStatePhoneId = getNextRepollStatePhoneId();
        Log.d("KeyguardSimPinPukMeView", "simStateReadyProcess mNextRepollStatePhoneId =" + this.mNextRepollStatePhoneId);
        if (this.mNextRepollStatePhoneId != -1) {
            try {
                getSimUnlockProgressDialog().show();
                Log.d("KeyguardSimPinPukMeView", "repollIccStateForNetworkLock phoneId =" + this.mNextRepollStatePhoneId);
                IMtkTelephonyEx.Stub.asInterface(ServiceManager.getService("phoneEx")).repollIccStateForNetworkLock(KeyguardUtils.getSubIdUsingPhoneId(this.mNextRepollStatePhoneId), true);
                return;
            } catch (RemoteException e) {
                Log.d("KeyguardSimPinPukMeView", "repollIccStateForNetworkLock exception caught");
                return;
            }
        }
        this.mCallback.dismiss(true, KeyguardUpdateMonitor.getCurrentUser());
    }

    private int getNextRepollStatePhoneId() {
        if (IccCardConstants.State.NETWORK_LOCKED == this.mLastSimState && KeyguardUtils.isMediatekSimMeLockSupport()) {
            for (int i = 0; i < KeyguardUtils.getNumOfPhone(); i++) {
                if (this.mSecurityModel.isPinPukOrMeRequiredOfPhoneId(i)) {
                    if (this.mUpdateMonitor.getSimStateOfPhoneId(i) == IccCardConstants.State.NETWORK_LOCKED) {
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
            this.mY = context.getResources().getDimensionPixelSize(android.R.dimen.indeterminate_progress_alpha_27);
        }

        public static Toast makeText(Context context, CharSequence charSequence) {
            Toast toast = new Toast(context);
            View viewInflate = ((LayoutInflater) context.getSystemService("layout_inflater")).inflate(android.R.layout.preference_information_holo, (ViewGroup) null);
            ((TextView) viewInflate.findViewById(android.R.id.message)).setText(charSequence);
            toast.mView = viewInflate;
            return toast;
        }

        public void show() {
            if (this.mView == null) {
                throw new RuntimeException("setView must have been called");
            }
            try {
                getService().enqueueToast(this.mContext.getPackageName(), this.mTN, 0);
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
                WindowManager.LayoutParams layoutParams = this.mParams;
                layoutParams.height = -2;
                layoutParams.width = -2;
                layoutParams.flags = 152;
                layoutParams.format = -3;
                layoutParams.windowAnimations = android.R.style.Animation.Toast;
                layoutParams.type = 2009;
                layoutParams.setTitle("Toast");
            }

            public void show(IBinder iBinder) {
                Toast.this.mHandler.post(this.mShow);
            }

            public void hide() {
                Toast.this.mHandler.post(this.mHide);
            }

            public void handleShow() {
                this.mWM = (WindowManagerImpl) Toast.this.mContext.getSystemService("window");
                int i = Toast.this.mGravity;
                this.mParams.gravity = i;
                if ((i & 7) == 7) {
                    this.mParams.horizontalWeight = 1.0f;
                }
                if ((i & com.android.systemui.plugins.R.styleable.AppCompatTheme_windowActionBarOverlay) == 112) {
                    this.mParams.verticalWeight = 1.0f;
                }
                this.mParams.y = Toast.this.mY;
                if (Toast.this.mView != null) {
                    if (Toast.this.mView.getParent() != null) {
                        this.mWM.removeView(Toast.this.mView);
                    }
                    this.mWM.addView(Toast.this.mView, this.mParams);
                }
            }

            public void handleHide() {
                if (Toast.this.mView != null) {
                    if (Toast.this.mView.getParent() != null) {
                        this.mWM.removeView(Toast.this.mView);
                    }
                    Toast.this.mView = null;
                }
            }
        }
    }

    @Override
    public void onWindowFocusChanged(boolean z) {
        Log.d("KeyguardSimPinPukMeView", "onWindowFocusChanged(hasWindowFocus = " + z + ")");
        if (z) {
            resetPasswordText(true, false);
            KeyguardUtils.requestImeStatusRefresh(this.mContext);
        }
    }

    @Override
    public void startAppearAnimation() {
    }

    @Override
    public boolean startDisappearAnimation(Runnable runnable) {
        return false;
    }

    private void showDismissIfNeed(IccCardConstants.State state) {
        if (state == IccCardConstants.State.NETWORK_LOCKED && KeyguardUtils.isMediatekSimMeLockSupport() && KeyguardUtils.isDismissSimMeLockSupport()) {
            this.mDismissButton.setVisibility(0);
            this.mDismissButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    KeyguardSimPinPukMeView.this.mUpdateMonitor.setSimmeDismissFlagOfPhoneId(KeyguardSimPinPukMeView.this.mPhoneId, true);
                    KeyguardSimPinPukMeView.this.resetPasswordText(true, false);
                    KeyguardSimPinPukMeView.this.mCallback.dismiss(true, KeyguardUpdateMonitor.getCurrentUser());
                }
            });
        } else {
            this.mDismissButton.setVisibility(4);
            this.mDismissButton.setOnClickListener(null);
        }
    }
}
