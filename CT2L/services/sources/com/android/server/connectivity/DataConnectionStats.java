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
        public void onServiceStateChanged(ServiceState state) {
            DataConnectionStats.this.mServiceState = state;
            DataConnectionStats.this.notePhoneDataConnectionState();
        }

        @Override
        public void onDataConnectionStateChanged(int state, int networkType) {
            DataConnectionStats.this.mDataState = state;
            DataConnectionStats.this.notePhoneDataConnectionState();
        }

        @Override
        public void onDataActivity(int direction) {
            DataConnectionStats.this.notePhoneDataConnectionState();
        }
    };
    private final IBatteryStats mBatteryStats = BatteryStatsService.getService();

    public DataConnectionStats(Context context) {
        this.mContext = context;
    }

    public void startMonitoring() {
        TelephonyManager phone = (TelephonyManager) this.mContext.getSystemService("phone");
        phone.listen(this.mPhoneStateListener, 449);
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.SIM_STATE_CHANGED");
        filter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
        filter.addAction("android.net.conn.INET_CONDITION_ACTION");
        this.mContext.registerReceiver(this, filter);
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
        if (this.mServiceState != null) {
            boolean simReadyOrUnknown = this.mSimState == IccCardConstants.State.READY || this.mSimState == IccCardConstants.State.UNKNOWN;
            boolean visible = (simReadyOrUnknown || isCdma()) && hasService() && this.mDataState == 2;
            int networkType = this.mServiceState.getDataNetworkType();
            try {
                this.mBatteryStats.notePhoneDataConnectionState(networkType, visible);
            } catch (RemoteException e) {
                Log.w(TAG, "Error noting data connection state", e);
            }
        }
    }

    private final void updateSimState(Intent intent) {
        String stateExtra = intent.getStringExtra("ss");
        if ("ABSENT".equals(stateExtra)) {
            this.mSimState = IccCardConstants.State.ABSENT;
            return;
        }
        if ("READY".equals(stateExtra)) {
            this.mSimState = IccCardConstants.State.READY;
            return;
        }
        if ("LOCKED".equals(stateExtra)) {
            String lockedReason = intent.getStringExtra("reason");
            if ("PIN".equals(lockedReason)) {
                this.mSimState = IccCardConstants.State.PIN_REQUIRED;
                return;
            } else if ("PUK".equals(lockedReason)) {
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
        if (this.mSignalStrength == null || this.mSignalStrength.isGsm()) {
            return DEBUG;
        }
        return true;
    }

    private boolean hasService() {
        if (this.mServiceState == null || this.mServiceState.getState() == 1 || this.mServiceState.getState() == 3) {
            return DEBUG;
        }
        return true;
    }
}
