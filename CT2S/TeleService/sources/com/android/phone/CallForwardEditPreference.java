package com.android.phone;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import com.android.internal.telephony.CallForwardInfo;
import com.android.internal.telephony.CommandException;
import com.android.internal.telephony.Phone;

public class CallForwardEditPreference extends EditPhoneNumberPreference {
    private static final String[] SRC_TAGS = {"{0}"};
    CallForwardInfo callForwardInfo;
    private int mButtonClicked;
    private MyHandler mHandler;
    private Phone mPhone;
    private int mServiceClass;
    private CharSequence mSummaryOnTemplate;
    private TimeConsumingPreferenceListener mTcpListener;
    int reason;

    public CallForwardEditPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mHandler = new MyHandler();
        this.mSummaryOnTemplate = getSummaryOn();
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.CallForwardEditPreference, 0, R.style.EditPhoneNumberPreference);
        this.mServiceClass = a.getInt(0, 1);
        this.reason = a.getInt(1, 0);
        a.recycle();
        Log.d("CallForwardEditPreference", "mServiceClass=" + this.mServiceClass + ", reason=" + this.reason);
    }

    public CallForwardEditPreference(Context context) {
        this(context, null);
    }

    void init(TimeConsumingPreferenceListener listener, boolean skipReading, Phone phone) {
        this.mPhone = phone;
        this.mTcpListener = listener;
        if (!skipReading) {
            this.mPhone.getCallForwardingOption(this.reason, this.mHandler.obtainMessage(0, 0, 0, null));
            if (this.mTcpListener != null) {
                this.mTcpListener.onStarted(this, true);
            }
        }
    }

    @Override
    protected void onBindDialogView(View view) {
        this.mButtonClicked = -2;
        super.onBindDialogView(view);
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        super.onClick(dialog, which);
        this.mButtonClicked = which;
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
        Log.d("CallForwardEditPreference", "mButtonClicked=" + this.mButtonClicked + ", positiveResult=" + positiveResult);
        if (this.mButtonClicked != -2) {
            int action = (isToggled() || this.mButtonClicked == -1) ? 3 : 0;
            int time = this.reason != 2 ? 0 : 20;
            String number = getPhoneNumber();
            Log.d("CallForwardEditPreference", "callForwardInfo=" + this.callForwardInfo);
            if (action == 3 && this.callForwardInfo != null && this.callForwardInfo.status == 1 && number.equals(this.callForwardInfo.number)) {
                Log.d("CallForwardEditPreference", "no change, do nothing");
                return;
            }
            Log.d("CallForwardEditPreference", "reason=" + this.reason + ", action=" + action + ", number=" + number);
            setSummaryOn("");
            this.mPhone.setCallForwardingOption(action, this.reason, number, time, this.mHandler.obtainMessage(1, action, 1));
            if (this.mTcpListener != null) {
                this.mTcpListener.onStarted(this, false);
            }
        }
    }

    void handleCallForwardResult(CallForwardInfo cf) {
        this.callForwardInfo = cf;
        Log.d("CallForwardEditPreference", "handleGetCFResponse done, callForwardInfo=" + this.callForwardInfo);
        setToggled(this.callForwardInfo.status == 1);
        setPhoneNumber(this.callForwardInfo.number);
    }

    private void updateSummaryText() {
        CharSequence summaryOn;
        if (isToggled()) {
            String number = getRawPhoneNumber();
            if (number != null && number.length() > 0) {
                String[] values = {number};
                summaryOn = TextUtils.replace(this.mSummaryOnTemplate, SRC_TAGS, values);
            } else {
                summaryOn = getContext().getString(R.string.sum_cfu_enabled_no_number);
            }
            setSummaryOn(summaryOn);
        }
    }

    private class MyHandler extends Handler {
        private MyHandler() {
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    handleGetCFResponse(msg);
                    break;
                case 1:
                    handleSetCFResponse(msg);
                    break;
            }
        }

        private void handleGetCFResponse(Message msg) {
            CharSequence s;
            Log.d("CallForwardEditPreference", "handleGetCFResponse: done");
            CallForwardEditPreference.this.mTcpListener.onFinished(CallForwardEditPreference.this, msg.arg2 != 1);
            AsyncResult ar = (AsyncResult) msg.obj;
            CallForwardEditPreference.this.callForwardInfo = null;
            if (ar.exception != null) {
                Log.d("CallForwardEditPreference", "handleGetCFResponse: ar.exception=" + ar.exception);
                if (ar.exception instanceof CommandException) {
                    CallForwardEditPreference.this.mTcpListener.onException(CallForwardEditPreference.this, (CommandException) ar.exception);
                } else {
                    CallForwardEditPreference.this.mTcpListener.onError(CallForwardEditPreference.this, 300);
                }
            } else {
                if (ar.userObj instanceof Throwable) {
                    CallForwardEditPreference.this.mTcpListener.onError(CallForwardEditPreference.this, 400);
                }
                CallForwardInfo[] cfInfoArray = (CallForwardInfo[]) ar.result;
                if (cfInfoArray.length == 0) {
                    Log.d("CallForwardEditPreference", "handleGetCFResponse: cfInfoArray.length==0");
                    CallForwardEditPreference.this.setEnabled(false);
                    CallForwardEditPreference.this.mTcpListener.onError(CallForwardEditPreference.this, 400);
                } else {
                    int length = cfInfoArray.length;
                    for (int i = 0; i < length; i++) {
                        Log.d("CallForwardEditPreference", "handleGetCFResponse, cfInfoArray[" + i + "]=" + cfInfoArray[i]);
                        if ((CallForwardEditPreference.this.mServiceClass & cfInfoArray[i].serviceClass) != 0) {
                            CallForwardInfo info = cfInfoArray[i];
                            CallForwardEditPreference.this.handleCallForwardResult(info);
                            if (msg.arg2 == 1 && msg.arg1 == 0 && info.status == 1) {
                                switch (CallForwardEditPreference.this.reason) {
                                    case 1:
                                        s = CallForwardEditPreference.this.getContext().getText(R.string.disable_cfb_forbidden);
                                        break;
                                    case 2:
                                        s = CallForwardEditPreference.this.getContext().getText(R.string.disable_cfnry_forbidden);
                                        break;
                                    default:
                                        s = CallForwardEditPreference.this.getContext().getText(R.string.disable_cfnrc_forbidden);
                                        break;
                                }
                                AlertDialog.Builder builder = new AlertDialog.Builder(CallForwardEditPreference.this.getContext());
                                builder.setNeutralButton(R.string.close_dialog, (DialogInterface.OnClickListener) null);
                                builder.setTitle(CallForwardEditPreference.this.getContext().getText(R.string.error_updating_title));
                                builder.setMessage(s);
                                builder.setCancelable(true);
                                builder.create().show();
                            }
                        }
                    }
                }
            }
            CallForwardEditPreference.this.updateSummaryText();
        }

        private void handleSetCFResponse(Message msg) {
            AsyncResult ar = (AsyncResult) msg.obj;
            if (ar.exception != null) {
                Log.d("CallForwardEditPreference", "handleSetCFResponse: ar.exception=" + ar.exception);
            }
            Log.d("CallForwardEditPreference", "handleSetCFResponse: re get");
            CallForwardEditPreference.this.mPhone.getCallForwardingOption(CallForwardEditPreference.this.reason, obtainMessage(0, msg.arg1, 1, ar.exception));
        }
    }
}
