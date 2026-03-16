package com.android.services.telephony;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.os.UserHandle;
import android.provider.Settings;
import android.telephony.ServiceState;
import com.android.internal.os.SomeArgs;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;

public class EmergencyCallHelper {
    private Callback mCallback;
    private final Context mContext;
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    SomeArgs args = (SomeArgs) msg.obj;
                    Phone phone = (Phone) args.arg1;
                    Callback callback = (Callback) args.arg2;
                    args.recycle();
                    EmergencyCallHelper.this.startSequenceInternal(phone, callback);
                    break;
                case 2:
                    EmergencyCallHelper.this.onServiceStateChanged((ServiceState) ((AsyncResult) msg.obj).result);
                    break;
                case 3:
                    EmergencyCallHelper.this.onRetryTimeout();
                    break;
                default:
                    Log.wtf(this, "handleMessage: unexpected message: %d.", Integer.valueOf(msg.what));
                    break;
            }
        }
    };
    private int mNumRetriesSoFar;
    private Phone mPhone;

    interface Callback {
        void onComplete(boolean z);
    }

    public EmergencyCallHelper(Context context) {
        Log.d(this, "EmergencyCallHelper constructor.", new Object[0]);
        this.mContext = context;
    }

    public void startTurnOnRadioSequence(Phone phone, Callback callback) {
        Log.d(this, "startTurnOnRadioSequence", new Object[0]);
        SomeArgs args = SomeArgs.obtain();
        args.arg1 = phone;
        args.arg2 = callback;
        this.mHandler.obtainMessage(1, args).sendToTarget();
    }

    private void startSequenceInternal(Phone phone, Callback callback) {
        Log.d(this, "startSequenceInternal()", new Object[0]);
        cleanup();
        this.mPhone = phone;
        this.mCallback = callback;
        powerOnRadio();
        startRetryTimer();
    }

    private void onServiceStateChanged(ServiceState state) {
        Log.d(this, "onServiceStateChanged(), new state = %s.", state);
        if (isOkToCall(state.getState(), this.mPhone.getState())) {
            Log.d(this, "onServiceStateChanged: ok to call!", new Object[0]);
            onComplete(true);
            cleanup();
            return;
        }
        Log.d(this, "onServiceStateChanged: not ready to call yet, keep waiting.", new Object[0]);
    }

    private boolean isOkToCall(int serviceState, PhoneConstants.State phoneState) {
        if (phoneState == PhoneConstants.State.OFFHOOK || serviceState == 0 || serviceState == 2) {
            return true;
        }
        return this.mNumRetriesSoFar == 5 && serviceState == 1;
    }

    private void onRetryTimeout() {
        PhoneConstants.State phoneState = this.mPhone.getState();
        int serviceState = this.mPhone.getServiceState().getState();
        Log.d(this, "onRetryTimeout():  phone state = %s, service state = %d, retries = %d.", phoneState, Integer.valueOf(serviceState), Integer.valueOf(this.mNumRetriesSoFar));
        if (isOkToCall(serviceState, phoneState)) {
            Log.d(this, "onRetryTimeout: Radio is on. Cleaning up.", new Object[0]);
            onComplete(true);
            cleanup();
            return;
        }
        this.mNumRetriesSoFar++;
        Log.d(this, "mNumRetriesSoFar is now " + this.mNumRetriesSoFar, new Object[0]);
        if (this.mNumRetriesSoFar > 5) {
            Log.w(this, "Hit MAX_NUM_RETRIES; giving up.", new Object[0]);
            cleanup();
        } else {
            Log.d(this, "Trying (again) to turn on the radio.", new Object[0]);
            powerOnRadio();
            startRetryTimer();
        }
    }

    private void powerOnRadio() {
        Log.d(this, "powerOnRadio().", new Object[0]);
        registerForServiceStateChanged();
        if (Settings.Global.getInt(this.mContext.getContentResolver(), "airplane_mode_on", 0) > 0) {
            Log.d(this, "==> Turning off airplane mode.", new Object[0]);
            Settings.Global.putInt(this.mContext.getContentResolver(), "airplane_mode_on", 0);
            Intent intent = new Intent("android.intent.action.AIRPLANE_MODE");
            intent.putExtra("state", false);
            this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
            return;
        }
        Log.d(this, "==> (Apparently) not in airplane mode; manually powering radio on.", new Object[0]);
        this.mPhone.setRadioPower(true);
    }

    private void cleanup() {
        Log.d(this, "cleanup()", new Object[0]);
        onComplete(false);
        unregisterForServiceStateChanged();
        cancelRetryTimer();
        this.mPhone = null;
        this.mNumRetriesSoFar = 0;
    }

    private void startRetryTimer() {
        cancelRetryTimer();
        this.mHandler.sendEmptyMessageDelayed(3, 5000L);
    }

    private void cancelRetryTimer() {
        this.mHandler.removeMessages(3);
    }

    private void registerForServiceStateChanged() {
        unregisterForServiceStateChanged();
        this.mPhone.registerForServiceStateChanged(this.mHandler, 2, (Object) null);
    }

    private void unregisterForServiceStateChanged() {
        if (this.mPhone != null) {
            this.mPhone.unregisterForServiceStateChanged(this.mHandler);
        }
        this.mHandler.removeMessages(2);
    }

    private void onComplete(boolean isRadioReady) {
        if (this.mCallback != null) {
            Callback tempCallback = this.mCallback;
            this.mCallback = null;
            tempCallback.onComplete(isRadioReady);
        }
    }
}
