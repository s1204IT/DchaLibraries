package com.android.phone;

import android.content.Context;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.preference.SwitchPreference;
import android.util.AttributeSet;
import android.util.Log;
import com.android.internal.telephony.CommandException;
import com.android.internal.telephony.Phone;
import com.mediatek.phone.ext.ExtensionManager;
import com.mediatek.settings.CallSettingUtils;

public class CallWaitingSwitchPreference extends SwitchPreference {
    private static final int DELAY_TIME = 1500;
    private static final String LOG_TAG = "CallWaitingSwitchPreference";
    private final boolean DBG;
    private final MyHandler mHandler;
    private Phone mPhone;
    private TimeConsumingPreferenceListener mTcpListener;

    public CallWaitingSwitchPreference(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.DBG = true;
        this.mHandler = new MyHandler();
    }

    public CallWaitingSwitchPreference(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, android.R.attr.switchPreferenceStyle);
    }

    public CallWaitingSwitchPreference(Context context) {
        this(context, null);
    }

    public void init(TimeConsumingPreferenceListener timeConsumingPreferenceListener, boolean z, Phone phone) {
        this.mPhone = phone;
        this.mTcpListener = timeConsumingPreferenceListener;
        if (!z) {
            this.mPhone.getCallWaiting(this.mHandler.obtainMessage(0, 0, 0));
            if (this.mTcpListener != null) {
                this.mTcpListener.onStarted(this, true);
            }
        }
    }

    @Override
    protected void onClick() {
        CallSettingUtils.DialogType dialogTipsType = CallSettingUtils.getDialogTipsType(getContext(), this.mPhone.getSubId());
        if (dialogTipsType == CallSettingUtils.DialogType.DATA_OPEN || dialogTipsType == CallSettingUtils.DialogType.DATA_ROAMING) {
            CallSettingUtils.showDialogTips(getContext(), this.mPhone.getSubId(), dialogTipsType, null);
            return;
        }
        super.onClick();
        this.mPhone.setCallWaiting(isChecked(), this.mHandler.obtainMessage(1));
        if (this.mTcpListener != null) {
            this.mTcpListener.onStarted(this, false);
        }
    }

    private class MyHandler extends Handler {
        static final int MESSAGE_GET_CALL_WAITING = 0;
        static final int MESSAGE_SET_CALL_WAITING = 1;

        private MyHandler() {
        }

        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case 0:
                    handleGetCallWaitingResponse(message);
                    break;
                case 1:
                    handleSetCallWaitingResponse(message);
                    break;
            }
        }

        private void handleGetCallWaitingResponse(Message message) {
            AsyncResult asyncResult = (AsyncResult) message.obj;
            boolean z = false;
            if (CallWaitingSwitchPreference.this.mTcpListener != null) {
                if (message.arg2 == 1) {
                    CallWaitingSwitchPreference.this.mTcpListener.onFinished(CallWaitingSwitchPreference.this, false);
                } else {
                    CallWaitingSwitchPreference.this.mTcpListener.onFinished(CallWaitingSwitchPreference.this, true);
                }
            }
            if (asyncResult.exception instanceof CommandException) {
                Log.d(CallWaitingSwitchPreference.LOG_TAG, "handleGetCallWaitingResponse: CommandException=" + asyncResult.exception);
                if (CallWaitingSwitchPreference.this.mTcpListener != null) {
                    CallWaitingSwitchPreference.this.mTcpListener.onException(CallWaitingSwitchPreference.this, (CommandException) asyncResult.exception);
                    return;
                }
                return;
            }
            if ((asyncResult.userObj instanceof Throwable) || asyncResult.exception != null) {
                Log.d(CallWaitingSwitchPreference.LOG_TAG, "handleGetCallWaitingResponse: Exception" + asyncResult.exception);
                if (CallWaitingSwitchPreference.this.mTcpListener != null) {
                    CallWaitingSwitchPreference.this.mTcpListener.onError(CallWaitingSwitchPreference.this, TimeConsumingPreferenceActivity.RESPONSE_ERROR);
                    return;
                }
                return;
            }
            Log.d(CallWaitingSwitchPreference.LOG_TAG, "handleGetCallWaitingResponse: CW state successfully queried.");
            ExtensionManager.getCallFeaturesSettingExt().resetImsPdnOverSSComplete(CallWaitingSwitchPreference.this.getContext(), message.arg2);
            ExtensionManager.getCallFeaturesSettingExt().disableCallFwdPref(CallWaitingSwitchPreference.this.getContext(), CallWaitingSwitchPreference.this.mPhone, CallWaitingSwitchPreference.this, -1);
            int[] iArr = (int[]) asyncResult.result;
            try {
                Log.i(CallWaitingSwitchPreference.LOG_TAG, "handleGetCallWaitingResponse cwArray[0]:cwArray[1] = " + iArr[0] + ":" + iArr[1]);
                CallWaitingSwitchPreference callWaitingSwitchPreference = CallWaitingSwitchPreference.this;
                if (iArr[0] == 1 && (iArr[1] & 1) == 1) {
                    z = true;
                }
                callWaitingSwitchPreference.setChecked(z);
            } catch (ArrayIndexOutOfBoundsException e) {
                Log.e(CallWaitingSwitchPreference.LOG_TAG, "handleGetCallWaitingResponse: improper result: err =" + e.getMessage());
            }
        }

        private void handleSetCallWaitingResponse(Message message) {
            final AsyncResult asyncResult = (AsyncResult) message.obj;
            if (asyncResult.exception != null) {
                Log.d(CallWaitingSwitchPreference.LOG_TAG, "handleSetCallWaitingResponse: ar.exception=" + asyncResult.exception);
                CallWaitingSwitchPreference.this.setChecked(CallWaitingSwitchPreference.this.isChecked() ^ true);
            }
            Log.d(CallWaitingSwitchPreference.LOG_TAG, "handleSetCallWaitingResponse: re get");
            postDelayed(new Runnable() {
                @Override
                public void run() {
                    Log.d(CallWaitingSwitchPreference.LOG_TAG, "handleSetCallWaitingResponse: re get start");
                    CallWaitingSwitchPreference.this.mPhone.getCallWaiting(MyHandler.this.obtainMessage(0, 1, 1, asyncResult.exception));
                }
            }, 1500L);
        }
    }
}
