package com.android.server.connectivity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.RemoteException;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.util.Log;
import com.android.internal.app.IBatteryStats;
import com.android.internal.telephony.IccCardConstants;
import com.android.server.am.BatteryStatsService;
import com.android.server.policy.PhoneWindowManager;

public class DataConnectionStats extends BroadcastReceiver {
    private static final boolean DEBUG = false;
    private static final String TAG = "DataConnectionStats";
    private final Context mContext;
    private ServiceState mServiceState;
    private SignalStrength mSignalStrength;
    private IccCardConstants.State mSimState = IccCardConstants.State.READY;
    private int mDataState = 0;
    private final PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
        @Override
        public void onSignalStrengthsChanged(SignalStrength signalStrength) {
            DataConnectionStats.this.mSignalStrength = signalStrength;
        }

        @Override
        public void onServiceStateChanged(ServiceState serviceState) {
            DataConnectionStats.this.mServiceState = serviceState;
            DataConnectionStats.this.notePhoneDataConnectionState();
        }

        @Override
        public void onDataConnectionStateChanged(int i, int i2) {
            DataConnectionStats.this.mDataState = i;
            DataConnectionStats.this.notePhoneDataConnectionState();
        }

        @Override
        public void onDataActivity(int i) {
            DataConnectionStats.this.notePhoneDataConnectionState();
        }
    };
    private final IBatteryStats mBatteryStats = BatteryStatsService.getService();

    public DataConnectionStats(Context context) {
        this.mContext = context;
    }

    public void startMonitoring() {
        ((TelephonyManager) this.mContext.getSystemService("phone")).listen(this.mPhoneStateListener, 449);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.SIM_STATE_CHANGED");
        intentFilter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
        intentFilter.addAction("android.net.conn.INET_CONDITION_ACTION");
        this.mContext.registerReceiver(this, intentFilter);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action.equals("android.intent.action.SIM_STATE_CHANGED")) {
            updateSimState(intent);
            notePhoneDataConnectionState();
        } else if (action.equals("android.net.conn.CONNECTIVITY_CHANGE") || action.equals("android.net.conn.INET_CONDITION_ACTION")) {
            notePhoneDataConnectionState();
        }
    }

    private void notePhoneDataConnectionState() {
        if (this.mServiceState == null) {
            return;
        }
        try {
            this.mBatteryStats.notePhoneDataConnectionState(this.mServiceState.getDataNetworkType(), ((this.mSimState == IccCardConstants.State.READY || this.mSimState == IccCardConstants.State.UNKNOWN) || isCdma()) && hasService() && this.mDataState == 2);
        } catch (RemoteException e) {
            Log.w(TAG, "Error noting data connection state", e);
        }
    }

    private final void updateSimState(Intent intent) {
        String stringExtra = intent.getStringExtra("ss");
        if ("ABSENT".equals(stringExtra)) {
            this.mSimState = IccCardConstants.State.ABSENT;
            return;
        }
        if ("READY".equals(stringExtra)) {
            this.mSimState = IccCardConstants.State.READY;
            return;
        }
        if ("LOCKED".equals(stringExtra)) {
            String stringExtra2 = intent.getStringExtra(PhoneWindowManager.SYSTEM_DIALOG_REASON_KEY);
            if ("PIN".equals(stringExtra2)) {
                this.mSimState = IccCardConstants.State.PIN_REQUIRED;
                return;
            } else if ("PUK".equals(stringExtra2)) {
                this.mSimState = IccCardConstants.State.PUK_REQUIRED;
                return;
            } else {
                this.mSimState = IccCardConstants.State.NETWORK_LOCKED;
                return;
            }
        }
        this.mSimState = IccCardConstants.State.UNKNOWN;
    }

    private boolean isCdma() {
        return (this.mSignalStrength == null || this.mSignalStrength.isGsm()) ? false : true;
    }

    private boolean hasService() {
        return (this.mServiceState == null || this.mServiceState.getState() == 1 || this.mServiceState.getState() == 3) ? false : true;
    }
}
