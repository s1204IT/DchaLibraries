package com.android.phone;

import android.content.Context;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.preference.CheckBoxPreference;
import android.util.AttributeSet;
import android.util.Log;
import com.android.internal.telephony.CommandException;
import com.android.internal.telephony.Phone;

public class CallWaitingCheckBoxPreference extends CheckBoxPreference {
    private final boolean DBG;
    private final MyHandler mHandler;
    private Phone mPhone;
    private TimeConsumingPreferenceListener mTcpListener;

    public CallWaitingCheckBoxPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.DBG = true;
        this.mHandler = new MyHandler();
    }

    public CallWaitingCheckBoxPreference(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.checkBoxPreferenceStyle);
    }

    public CallWaitingCheckBoxPreference(Context context) {
        this(context, null);
    }

    void init(TimeConsumingPreferenceListener listener, boolean skipReading, Phone phone) {
        this.mPhone = phone;
        this.mTcpListener = listener;
        if (!skipReading) {
            this.mPhone.getCallWaiting(this.mHandler.obtainMessage(0, 0, 0));
            if (this.mTcpListener != null) {
                this.mTcpListener.onStarted(this, true);
            }
        }
    }

    @Override
    protected void onClick() {
        super.onClick();
        this.mPhone.setCallWaiting(isChecked(), this.mHandler.obtainMessage(1));
        if (this.mTcpListener != null) {
            this.mTcpListener.onStarted(this, false);
        }
    }

    private class MyHandler extends Handler {
        private MyHandler() {
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    handleGetCallWaitingResponse(msg);
                    break;
                case 1:
                    handleSetCallWaitingResponse(msg);
                    break;
            }
        }

        private void handleGetCallWaitingResponse(Message msg) {
            AsyncResult ar = (AsyncResult) msg.obj;
            if (CallWaitingCheckBoxPreference.this.mTcpListener != null) {
                if (msg.arg2 == 1) {
                    CallWaitingCheckBoxPreference.this.mTcpListener.onFinished(CallWaitingCheckBoxPreference.this, false);
                } else {
                    CallWaitingCheckBoxPreference.this.mTcpListener.onFinished(CallWaitingCheckBoxPreference.this, true);
                }
            }
            if (ar.exception instanceof CommandException) {
                Log.d("CallWaitingCheckBoxPreference", "handleGetCallWaitingResponse: CommandException=" + ar.exception);
                if (CallWaitingCheckBoxPreference.this.mTcpListener != null) {
                    CallWaitingCheckBoxPreference.this.mTcpListener.onException(CallWaitingCheckBoxPreference.this, (CommandException) ar.exception);
                    return;
                }
                return;
            }
            if ((ar.userObj instanceof Throwable) || ar.exception != null) {
                Log.d("CallWaitingCheckBoxPreference", "handleGetCallWaitingResponse: Exception" + ar.exception);
                if (CallWaitingCheckBoxPreference.this.mTcpListener != null) {
                    CallWaitingCheckBoxPreference.this.mTcpListener.onError(CallWaitingCheckBoxPreference.this, 400);
                    return;
                }
                return;
            }
            Log.d("CallWaitingCheckBoxPreference", "handleGetCallWaitingResponse: CW state successfully queried.");
            int[] cwArray = (int[]) ar.result;
            try {
                CallWaitingCheckBoxPreference.this.setChecked(cwArray[0] == 1 && (cwArray[1] & 1) == 1);
            } catch (ArrayIndexOutOfBoundsException e) {
                Log.e("CallWaitingCheckBoxPreference", "handleGetCallWaitingResponse: improper result: err =" + e.getMessage());
            }
        }

        private void handleSetCallWaitingResponse(Message msg) {
            AsyncResult ar = (AsyncResult) msg.obj;
            if (ar.exception != null) {
                Log.d("CallWaitingCheckBoxPreference", "handleSetCallWaitingResponse: ar.exception=" + ar.exception);
            }
            Log.d("CallWaitingCheckBoxPreference", "handleSetCallWaitingResponse: re get");
            CallWaitingCheckBoxPreference.this.mPhone.getCallWaiting(obtainMessage(0, 1, 1, ar.exception));
        }
    }
}
