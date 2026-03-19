package com.android.phone;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.os.AsyncResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PersistableBundle;
import android.text.method.DigitsKeyListener;
import android.text.method.PasswordTransformationMethod;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.android.internal.telephony.CommandException;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.imsphone.ImsPhone;
import com.android.phone.settings.fdn.EditPinPreference;
import com.mediatek.phone.ext.ExtensionManager;
import com.mediatek.settings.CallSettingUtils;
import java.lang.ref.WeakReference;

public class CallBarringEditPreference extends EditPinPreference {
    private static final boolean DBG = true;
    private static final String LOG_TAG = "CallBarringEditPreference";
    private static final int PW_LENGTH = 4;
    private int mButtonClicked;
    private CharSequence mDialogMessageDisabled;
    private CharSequence mDialogMessageEnabled;
    private CharSequence mDisableText;
    private CharSequence mEnableText;
    private String mFacility;
    private final MyHandler mHandler;
    boolean mIsActivated;
    private Phone mPhone;
    private int mServiceClass;
    private boolean mShowPassword;
    private CharSequence mSummaryOff;
    private CharSequence mSummaryOn;
    private TimeConsumingPreferenceListener mTcpListener;

    public CallBarringEditPreference(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mIsActivated = false;
        this.mHandler = new MyHandler(this);
        this.mServiceClass = 1;
        TypedArray typedArrayObtainStyledAttributes = context.obtainStyledAttributes(attributeSet, android.R.styleable.CheckBoxPreference, 0, 0);
        this.mSummaryOn = typedArrayObtainStyledAttributes.getString(0);
        this.mSummaryOff = typedArrayObtainStyledAttributes.getString(1);
        this.mDisableText = context.getText(R.string.disable);
        this.mEnableText = context.getText(R.string.enable);
        typedArrayObtainStyledAttributes.recycle();
        this.mPhone = PhoneFactory.getDefaultPhone();
        TypedArray typedArrayObtainStyledAttributes2 = context.obtainStyledAttributes(attributeSet, R.styleable.CallBarringEditPreference, 0, R.style.EditPhoneNumberPreference);
        this.mFacility = typedArrayObtainStyledAttributes2.getString(2);
        this.mDialogMessageEnabled = typedArrayObtainStyledAttributes2.getString(1);
        this.mDialogMessageDisabled = typedArrayObtainStyledAttributes2.getString(0);
        typedArrayObtainStyledAttributes2.recycle();
    }

    public CallBarringEditPreference(Context context) {
        this(context, null);
    }

    void init(TimeConsumingPreferenceListener timeConsumingPreferenceListener, boolean z, Phone phone) {
        Log.d(LOG_TAG, "init: phone id = " + phone.getPhoneId());
        this.mPhone = phone;
        this.mTcpListener = timeConsumingPreferenceListener;
        if (!z) {
            this.mPhone.getCallBarring(this.mFacility, "", this.mHandler.obtainMessage(0), this.mServiceClass);
            if (this.mTcpListener != null) {
                this.mTcpListener.onStarted(this, DBG);
            }
        }
    }

    @Override
    public void onClick(DialogInterface dialogInterface, int i) {
        super.onClick(dialogInterface, i);
        this.mButtonClicked = i;
    }

    protected boolean needInputMethod() {
        return this.mShowPassword;
    }

    void setInputMethodNeeded(boolean z) {
        this.mShowPassword = z;
    }

    @Override
    protected void showDialog(Bundle bundle) {
        setShowPassword();
        if (this.mShowPassword) {
            setDialogMessage(getContext().getString(R.string.messageCallBarring));
        } else {
            setDialogMessage(this.mIsActivated ? this.mDialogMessageEnabled : this.mDialogMessageDisabled);
        }
        Log.d(LOG_TAG, "showDialog: mShowPassword: " + this.mShowPassword + ", mIsActivated: " + this.mIsActivated);
        super.showDialog(bundle);
    }

    @Override
    protected void onBindView(View view) {
        CharSequence summary;
        int i;
        super.onBindView(view);
        TextView textView = (TextView) view.findViewById(android.R.id.summary);
        if (textView != null) {
            if (this.mIsActivated) {
                summary = this.mSummaryOn == null ? getSummary() : this.mSummaryOn;
            } else {
                summary = this.mSummaryOff == null ? getSummary() : this.mSummaryOff;
            }
            if (summary != null) {
                textView.setText(summary);
                i = 0;
            } else {
                i = 8;
            }
            if (i != textView.getVisibility()) {
                textView.setVisibility(i);
            }
        }
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        builder.setPositiveButton((CharSequence) null, (DialogInterface.OnClickListener) null);
        builder.setNeutralButton(this.mIsActivated ? this.mDisableText : this.mEnableText, this);
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);
        this.mButtonClicked = -2;
        EditText editText = (EditText) view.findViewById(android.R.id.edit);
        if (editText != null) {
            editText.setSingleLine(DBG);
            editText.setTransformationMethod(PasswordTransformationMethod.getInstance());
            editText.setKeyListener(DigitsKeyListener.getInstance());
            editText.setVisibility(this.mShowPassword ? 0 : 8);
        }
    }

    @Override
    protected void onDialogClosed(boolean z) {
        super.onDialogClosed(z);
        Log.d(LOG_TAG, "onDialogClosed: mButtonClicked=" + this.mButtonClicked + ", positiveResult=" + z);
        if (this.mButtonClicked != -2) {
            String string = null;
            if (this.mShowPassword && ((string = getEditText().getText().toString()) == null || string.length() != 4)) {
                Toast.makeText(getContext(), getContext().getString(R.string.call_barring_right_pwd_number), 0).show();
                return;
            }
            String str = string;
            CallSettingUtils.sensitiveLog(LOG_TAG, "onDialogClosed: password=", str);
            this.mPhone.setCallBarring(this.mFacility, this.mIsActivated ^ DBG, str, this.mHandler.obtainMessage(1), this.mServiceClass);
            if (this.mTcpListener != null) {
                this.mTcpListener.onStarted(this, false);
            }
        }
    }

    void handleCallBarringResult(boolean z) {
        this.mIsActivated = z;
        Log.d(LOG_TAG, "handleCallBarringResult: mIsActivated=" + this.mIsActivated);
    }

    void updateSummaryText() {
        notifyChanged();
        notifyDependencyChange(shouldDisableDependents());
    }

    private void setShowPassword() {
        ImsPhone imsPhone = this.mPhone != null ? (ImsPhone) this.mPhone.getImsPhone() : null;
        this.mShowPassword = imsPhone == null || !(imsPhone.getServiceState().getState() == 0 || imsPhone.isUtEnabled());
        PersistableBundle carrierConfigForSubId = PhoneGlobals.getInstance().getCarrierConfigForSubId(this.mPhone.getSubId());
        if (!this.mShowPassword && carrierConfigForSubId.getBoolean("mtk_show_call_barring_password_bool")) {
            Log.d(LOG_TAG, "setShowPassword for CSFB operators");
            this.mShowPassword = DBG;
        }
    }

    @Override
    public boolean shouldDisableDependents() {
        return this.mIsActivated;
    }

    private class MyHandler extends Handler {
        private static final int MESSAGE_GET_CALL_BARRING = 0;
        private static final int MESSAGE_SET_CALL_BARRING = 1;
        private final WeakReference<CallBarringEditPreference> mCallBarringEditPreference;

        private MyHandler(CallBarringEditPreference callBarringEditPreference) {
            this.mCallBarringEditPreference = new WeakReference<>(callBarringEditPreference);
        }

        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case 0:
                    handleGetCallBarringResponse(message);
                    break;
                case 1:
                    handleSetCallBarringResponse(message);
                    break;
            }
        }

        private void handleGetCallBarringResponse(Message message) {
            CallBarringEditPreference callBarringEditPreference = this.mCallBarringEditPreference.get();
            if (callBarringEditPreference == null) {
                return;
            }
            Log.d(CallBarringEditPreference.LOG_TAG, "handleGetCallBarringResponse: done");
            AsyncResult asyncResult = (AsyncResult) message.obj;
            if (message.arg2 == 1) {
                callBarringEditPreference.mTcpListener.onFinished(callBarringEditPreference, false);
            } else {
                callBarringEditPreference.mTcpListener.onFinished(callBarringEditPreference, CallBarringEditPreference.DBG);
                ImsPhone imsPhone = callBarringEditPreference.mPhone != null ? (ImsPhone) callBarringEditPreference.mPhone.getImsPhone() : null;
                if (!callBarringEditPreference.mShowPassword && (imsPhone == null || !imsPhone.isUtEnabled())) {
                    callBarringEditPreference.mShowPassword = CallBarringEditPreference.DBG;
                    Log.d(CallBarringEditPreference.LOG_TAG, "handleGetCallBarringResponse: mShowPassword changed for CSFB");
                }
            }
            if (asyncResult.exception != null) {
                Log.d(CallBarringEditPreference.LOG_TAG, "handleGetCallBarringResponse: ar.exception=" + asyncResult.exception);
                CallBarringEditPreference.this.handleCommandException(asyncResult);
            } else {
                if (asyncResult.userObj instanceof Throwable) {
                    callBarringEditPreference.mTcpListener.onError(callBarringEditPreference, TimeConsumingPreferenceActivity.RESPONSE_ERROR);
                }
                ExtensionManager.getCallFeaturesSettingExt().resetImsPdnOverSSComplete(CallBarringEditPreference.this.getContext(), message.arg2);
                int[] iArr = (int[]) asyncResult.result;
                if (iArr.length == 0) {
                    Log.d(CallBarringEditPreference.LOG_TAG, "handleGetCallBarringResponse: ar.result.length==0");
                    callBarringEditPreference.setEnabled(false);
                    callBarringEditPreference.mTcpListener.onError(callBarringEditPreference, TimeConsumingPreferenceActivity.RESPONSE_ERROR);
                } else {
                    int i = iArr[0];
                    Log.d(CallBarringEditPreference.LOG_TAG, "handleGetCallBarringResponse: result = " + i + " serviceClass = " + CallBarringEditPreference.this.mServiceClass);
                    callBarringEditPreference.handleCallBarringResult((i & CallBarringEditPreference.this.mServiceClass) != 0);
                }
            }
            callBarringEditPreference.updateSummaryText();
        }

        private void handleSetCallBarringResponse(Message message) {
            CallBarringEditPreference callBarringEditPreference = this.mCallBarringEditPreference.get();
            if (callBarringEditPreference == null) {
                return;
            }
            AsyncResult asyncResult = (AsyncResult) message.obj;
            if (asyncResult.exception != null || (asyncResult.userObj instanceof Throwable)) {
                Log.d(CallBarringEditPreference.LOG_TAG, "handleSetCallBarringResponse: ar.exception=" + asyncResult.exception);
            }
            Log.d(CallBarringEditPreference.LOG_TAG, "handleSetCallBarringResponse: re-get call barring option");
            callBarringEditPreference.mPhone.getCallBarring(callBarringEditPreference.mFacility, "", obtainMessage(0, 0, 1, asyncResult.exception), CallBarringEditPreference.this.mServiceClass);
        }
    }

    public void setServiceClass(int i) {
        this.mServiceClass = i;
        Log.d(LOG_TAG, "set service class to: " + this.mServiceClass);
    }

    private void handleCommandException(AsyncResult asyncResult) {
        if (asyncResult.exception instanceof CommandException) {
            CommandException commandException = asyncResult.exception;
            Log.d(LOG_TAG, "handleCommandException : Error =" + commandException.getCommandError());
            if (commandException.getCommandError() == CommandException.Error.REQUEST_NOT_SUPPORTED) {
                setEnabled(false);
                setSummary(this.mSummaryOff);
                return;
            } else {
                if (this.mTcpListener != null) {
                    this.mTcpListener.onException(this, commandException);
                    return;
                }
                return;
            }
        }
        if (this.mTcpListener != null) {
            this.mTcpListener.onError(this, TimeConsumingPreferenceActivity.EXCEPTION_ERROR);
        }
    }
}
