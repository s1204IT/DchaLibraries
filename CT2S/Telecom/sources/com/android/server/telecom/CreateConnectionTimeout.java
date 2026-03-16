package com.android.server.telecom;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.telecom.PhoneAccountHandle;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;
import java.util.Collection;
import java.util.Objects;

final class CreateConnectionTimeout extends PhoneStateListener implements Runnable {
    private final Call mCall;
    private final ConnectionServiceWrapper mConnectionService;
    private final Context mContext;
    private final Handler mHandler = new Handler();
    private boolean mIsCallTimedOut;
    private boolean mIsRegistered;
    private final PhoneAccountRegistrar mPhoneAccountRegistrar;

    CreateConnectionTimeout(Context context, PhoneAccountRegistrar phoneAccountRegistrar, ConnectionServiceWrapper connectionServiceWrapper, Call call) {
        this.mContext = context;
        this.mPhoneAccountRegistrar = phoneAccountRegistrar;
        this.mConnectionService = connectionServiceWrapper;
        this.mCall = call;
    }

    boolean isTimeoutNeededForCall(Collection<PhoneAccountHandle> collection, PhoneAccountHandle phoneAccountHandle) {
        if (!TelephonyUtil.shouldProcessAsEmergency(this.mContext, this.mCall.getHandle())) {
            return false;
        }
        PhoneAccountHandle simCallManager = this.mPhoneAccountRegistrar.getSimCallManager();
        if (!collection.contains(simCallManager) || Objects.equals(simCallManager, phoneAccountHandle) || !isConnectedToWifi()) {
            return false;
        }
        Log.d(this, "isTimeoutNeededForCall, returning true", new Object[0]);
        return true;
    }

    void registerTimeout() {
        Log.d(this, "registerTimeout", new Object[0]);
        this.mIsRegistered = true;
        TelephonyManager telephonyManager = (TelephonyManager) this.mContext.getSystemService("phone");
        telephonyManager.listen(this, 1);
        telephonyManager.listen(this, 0);
    }

    void unregisterTimeout() {
        Log.d(this, "unregisterTimeout", new Object[0]);
        this.mIsRegistered = false;
        this.mHandler.removeCallbacksAndMessages(null);
    }

    boolean isCallTimedOut() {
        return this.mIsCallTimedOut;
    }

    @Override
    public void onServiceStateChanged(ServiceState serviceState) {
        long timeoutLengthMillis = getTimeoutLengthMillis(serviceState);
        if (!this.mIsRegistered) {
            Log.d(this, "onServiceStateChanged, timeout no longer registered, skipping", new Object[0]);
            return;
        }
        if (timeoutLengthMillis <= 0) {
            Log.d(this, "onServiceStateChanged, timeout set to %d, skipping", Long.valueOf(timeoutLengthMillis));
        } else if (serviceState.getState() == 0) {
            Log.d(this, "onServiceStateChanged, cellular service available, skipping", new Object[0]);
        } else {
            this.mHandler.postDelayed(this, timeoutLengthMillis);
        }
    }

    @Override
    public void run() {
        if (this.mIsRegistered && isCallBeingPlaced(this.mCall)) {
            Log.d(this, "run, call timed out, calling disconnect", new Object[0]);
            this.mIsCallTimedOut = true;
            this.mConnectionService.disconnect(this.mCall);
        }
    }

    static boolean isCallBeingPlaced(Call call) {
        int state = call.getState();
        return state == 0 || state == 1 || state == 3;
    }

    private long getTimeoutLengthMillis(ServiceState serviceState) {
        return serviceState.getState() == 3 ? Timeouts.getEmergencyCallTimeoutRadioOffMillis(this.mContext.getContentResolver()) : Timeouts.getEmergencyCallTimeoutMillis(this.mContext.getContentResolver());
    }

    private boolean isConnectedToWifi() {
        ConnectivityManager connectivityManager = (ConnectivityManager) this.mContext.getSystemService("connectivity");
        if (connectivityManager == null) {
            return false;
        }
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected() && activeNetworkInfo.getType() == 1;
    }
}
