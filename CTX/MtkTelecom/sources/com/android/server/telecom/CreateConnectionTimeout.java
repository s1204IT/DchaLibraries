package com.android.server.telecom;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.telecom.Log;
import android.telecom.Logging.Runnable;
import android.telecom.PhoneAccountHandle;
import android.telephony.TelephonyManager;
import java.util.Collection;
import java.util.Objects;

final class CreateConnectionTimeout extends Runnable {
    private final Call mCall;
    private final ConnectionServiceWrapper mConnectionService;
    private final Context mContext;
    private final Handler mHandler;
    private boolean mIsCallTimedOut;
    private boolean mIsRegistered;
    private final PhoneAccountRegistrar mPhoneAccountRegistrar;

    CreateConnectionTimeout(Context context, PhoneAccountRegistrar phoneAccountRegistrar, ConnectionServiceWrapper connectionServiceWrapper, Call call) {
        super("CCT", (Object) null);
        this.mHandler = new Handler(Looper.getMainLooper());
        this.mContext = context;
        this.mPhoneAccountRegistrar = phoneAccountRegistrar;
        this.mConnectionService = connectionServiceWrapper;
        this.mCall = call;
    }

    boolean isTimeoutNeededForCall(Collection<PhoneAccountHandle> collection, PhoneAccountHandle phoneAccountHandle) {
        if (!this.mCall.isEmergencyCall()) {
            return false;
        }
        PhoneAccountHandle simCallManagerFromCall = this.mPhoneAccountRegistrar.getSimCallManagerFromCall(this.mCall);
        if (!collection.contains(simCallManagerFromCall) || Objects.equals(simCallManagerFromCall, phoneAccountHandle)) {
            return false;
        }
        if (!Objects.equals(simCallManagerFromCall.getComponentName(), this.mPhoneAccountRegistrar.getSystemSimCallManagerComponent())) {
            Log.d(this, "isTimeoutNeededForCall, not a system sim call manager", new Object[0]);
            return false;
        }
        Log.i(this, "isTimeoutNeededForCall, returning true", new Object[0]);
        return true;
    }

    void registerTimeout() {
        Log.d(this, "registerTimeout", new Object[0]);
        this.mIsRegistered = true;
        long timeoutLengthMillis = getTimeoutLengthMillis();
        if (timeoutLengthMillis <= 0) {
            Log.d(this, "registerTimeout, timeout set to %d, skipping", new Object[]{Long.valueOf(timeoutLengthMillis)});
        } else {
            this.mHandler.postDelayed(prepare(), timeoutLengthMillis);
        }
    }

    void unregisterTimeout() {
        Log.d(this, "unregisterTimeout", new Object[0]);
        this.mIsRegistered = false;
        this.mHandler.removeCallbacksAndMessages(null);
        cancel();
    }

    boolean isCallTimedOut() {
        return this.mIsCallTimedOut;
    }

    public void loggedRun() {
        if (this.mIsRegistered && isCallBeingPlaced(this.mCall)) {
            Log.i(this, "run, call timed out, calling disconnect", new Object[0]);
            this.mIsCallTimedOut = true;
            this.mConnectionService.disconnect(this.mCall);
        }
    }

    static boolean isCallBeingPlaced(Call call) {
        int state = call.getState();
        return state == 0 || state == 1 || state == 3 || state == 10;
    }

    private long getTimeoutLengthMillis() {
        if (((TelephonyManager) this.mContext.getSystemService("phone")).isRadioOn()) {
            return Timeouts.getEmergencyCallTimeoutMillis(this.mContext.getContentResolver());
        }
        return Timeouts.getEmergencyCallTimeoutRadioOffMillis(this.mContext.getContentResolver());
    }
}
