package com.android.services.telephony;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.telecom.Connection;
import android.telecom.DisconnectCause;
import com.android.phone.PhoneGlobals;
import com.android.phone.PhoneUtils;
import com.mediatek.services.telephony.MtkGsmCdmaConnection;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

final class CdmaConferenceController {
    private static final int ADD_OUTGOING_CONNECTION_DELAY_MILLIS = 6000;
    private static final int ADD_WAITING_CONNECTION_DELAY_MILLIS = 1000;
    private static final int UPDATE_CALL_CAPABILITIE_DELAY_MILLIS = 200;
    private CdmaConference mConference;
    private final TelephonyConnectionService mConnectionService;
    private final Connection.Listener mConnectionListener = new Connection.Listener() {
        public void onStateChanged(Connection connection, int i) {
            Log.d(this, "onStateChanged, conn=" + connection + ", state=" + i, new Object[0]);
            if (i != 6) {
                CdmaConferenceController.this.recalculateConference();
            }
        }

        public void onDisconnected(Connection connection, DisconnectCause disconnectCause) {
            Log.d(this, "onDisconnected, conn=" + connection + ", cause=" + disconnectCause, new Object[0]);
        }

        public void onDestroyed(Connection connection) {
            Log.d(this, "onDestroyed, conn=" + connection, new Object[0]);
            CdmaConferenceController.this.remove((MtkGsmCdmaConnection) connection);
        }
    };
    private final List<MtkGsmCdmaConnection> mCdmaConnections = new ArrayList();
    private final List<MtkGsmCdmaConnection> mPendingOutgoingConnections = new ArrayList();
    private final Handler mHandler = new Handler();
    private int mConfConnCount = 0;
    private MtkGsmCdmaConnection mSecondCall = null;
    private List<MtkGsmCdmaConnection> mConnectionsToReset = new ArrayList();
    private Runnable mDelayRunnable = new Runnable() {
        @Override
        public void run() {
            Log.d(this, "mDelayRunnable, mSecondCall=" + CdmaConferenceController.this.mSecondCall, new Object[0]);
            if (CdmaConferenceController.this.mSecondCall != null) {
                CdmaConferenceController.this.mSecondCall.forceAsDialing(false);
                CdmaConferenceController.this.addInternal(CdmaConferenceController.this.mSecondCall);
                CdmaConferenceController.this.mSecondCall = null;
            }
            Log.d(this, "mDelayRunnable, mConnectionsToReset=" + CdmaConferenceController.this.mConnectionsToReset, new Object[0]);
            for (MtkGsmCdmaConnection mtkGsmCdmaConnection : CdmaConferenceController.this.mConnectionsToReset) {
                Log.d(this, "mDelayRunnable, reset state for conn=" + mtkGsmCdmaConnection, new Object[0]);
                mtkGsmCdmaConnection.resetStateForConference();
            }
            CdmaConferenceController.this.mConnectionsToReset.clear();
        }
    };
    private CdmaConferenceBroadcastReceiver mReceiver = new CdmaConferenceBroadcastReceiver();

    public CdmaConferenceController(TelephonyConnectionService telephonyConnectionService) {
        this.mConnectionService = telephonyConnectionService;
        try {
            PhoneGlobals.getInstance().registerReceiver(this.mReceiver, new IntentFilter("android.intent.action.EMERGENCY_CALLBACK_MODE_CHANGED"));
        } catch (IllegalStateException e) {
            Log.e(this, e, "Can't get PhoneGlobals", new Object[0]);
        }
    }

    void add(final MtkGsmCdmaConnection mtkGsmCdmaConnection) {
        Log.d(this, "add, conn=" + mtkGsmCdmaConnection + ", list=" + this.mCdmaConnections, new Object[0]);
        if (this.mCdmaConnections.contains(mtkGsmCdmaConnection)) {
            Log.w(this, "add - connection already tracked; connection=%s", mtkGsmCdmaConnection);
            return;
        }
        if (!this.mCdmaConnections.isEmpty() && mtkGsmCdmaConnection.isOutgoing()) {
            mtkGsmCdmaConnection.forceAsDialing(true);
            this.mSecondCall = mtkGsmCdmaConnection;
            this.mConnectionsToReset.clear();
            for (MtkGsmCdmaConnection mtkGsmCdmaConnection2 : this.mCdmaConnections) {
                Log.d(this, "current's state=" + mtkGsmCdmaConnection2.getState(), new Object[0]);
                if (mtkGsmCdmaConnection2.setHoldingForConference()) {
                    this.mConnectionsToReset.add(mtkGsmCdmaConnection2);
                } else {
                    Log.d(this, "Fail to setHoldingForConference", new Object[0]);
                }
            }
            Log.d(this, "Add second connection, mConnectionsToReset:" + this.mConnectionsToReset, new Object[0]);
            this.mHandler.postDelayed(this.mDelayRunnable, 6000L);
            return;
        }
        if (!this.mCdmaConnections.isEmpty() && mtkGsmCdmaConnection.isCallWaiting()) {
            Log.d(this, "Waiting call arrives, mSecondCall=" + this.mSecondCall + ", hasCallbacks=" + this.mHandler.hasCallbacks(this.mDelayRunnable), new Object[0]);
            if (this.mSecondCall != null && this.mHandler.hasCallbacks(this.mDelayRunnable)) {
                Log.d(this, "Merge the second call now", new Object[0]);
                this.mHandler.removeCallbacks(this.mDelayRunnable);
                this.mSecondCall.forceAsDialing(false);
                addInternal(this.mSecondCall);
                this.mSecondCall = null;
                Iterator<MtkGsmCdmaConnection> it = this.mConnectionsToReset.iterator();
                while (it.hasNext()) {
                    it.next().resetStateForConference();
                }
                this.mConnectionsToReset.clear();
            }
            Log.d(this, "add waiting call connection listenner.", new Object[0]);
            mtkGsmCdmaConnection.addConnectionListener(new Connection.Listener() {
                public void onStateChanged(final Connection connection, int i) {
                    Log.d(CdmaConferenceController.this, "Waiting call=" + connection + ", state=" + i + ", mCdmaConnections.size=" + CdmaConferenceController.this.mCdmaConnections.size(), new Object[0]);
                    if (i == 4) {
                        connection.removeConnectionListener(this);
                        CdmaConferenceController.this.mHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                CdmaConferenceController.this.addInternal((MtkGsmCdmaConnection) connection);
                            }
                        }, 1000L);
                    } else if (i == 6) {
                        connection.removeConnectionListener(this);
                    }
                }
            });
            return;
        }
        this.mHandler.post(new Runnable() {
            @Override
            public final void run() {
                this.f$0.addInternal(mtkGsmCdmaConnection);
            }
        });
    }

    private void addInternal(MtkGsmCdmaConnection mtkGsmCdmaConnection) {
        this.mCdmaConnections.add(mtkGsmCdmaConnection);
        mtkGsmCdmaConnection.addConnectionListener(this.mConnectionListener);
        recalculateConference();
    }

    void remove(MtkGsmCdmaConnection mtkGsmCdmaConnection) {
        if (!this.mCdmaConnections.contains(mtkGsmCdmaConnection)) {
            Log.d(this, "remove - connection not tracked; connection=%s", mtkGsmCdmaConnection);
            return;
        }
        mtkGsmCdmaConnection.removeConnectionListener(this.mConnectionListener);
        this.mCdmaConnections.remove(mtkGsmCdmaConnection);
        recalculateConference();
    }

    private void recalculateConference() {
        boolean z;
        ArrayList<MtkGsmCdmaConnection> arrayList = new ArrayList(this.mCdmaConnections.size());
        for (MtkGsmCdmaConnection mtkGsmCdmaConnection : this.mCdmaConnections) {
            if (!mtkGsmCdmaConnection.isCallWaiting() && mtkGsmCdmaConnection.getState() != 6) {
                arrayList.add(mtkGsmCdmaConnection);
            }
        }
        boolean z2 = false;
        boolean z3 = false;
        for (MtkGsmCdmaConnection mtkGsmCdmaConnection2 : arrayList) {
            if (mtkGsmCdmaConnection2.getState() == 2) {
                z2 = true;
            }
            if (mtkGsmCdmaConnection2.getState() == 3) {
                z3 = true;
            }
        }
        if (z2 && z3) {
            Log.d(this, "skip for MO/MT conflict", new Object[0]);
            return;
        }
        Log.d(this, "recalculating conference calls %d", Integer.valueOf(arrayList.size()));
        Log.d(this, "mConfConnCount=" + this.mConfConnCount, new Object[0]);
        if (arrayList.size() >= 2) {
            MtkGsmCdmaConnection mtkGsmCdmaConnection3 = this.mCdmaConnections.get(this.mCdmaConnections.size() - 1);
            if (this.mConference == null) {
                Log.i(this, "Creating new Cdma conference call", new Object[0]);
                this.mConference = new CdmaConference(PhoneUtils.makePstnPhoneAccountHandle(mtkGsmCdmaConnection3.getPhone()));
                Log.d(this, "First conn=" + this.mCdmaConnections.get(0), new Object[0]);
                if (this.mCdmaConnections.get(0).getOriginalConnection() != null) {
                    this.mConference.setConnectTimeMillis(this.mCdmaConnections.get(0).getOriginalConnection().getConnectTime());
                } else {
                    Log.d(this, "First connection's original connection is null!", new Object[0]);
                }
                z = true;
            } else {
                z = false;
            }
            if (mtkGsmCdmaConnection3.isOutgoing()) {
                if (this.mConfConnCount == arrayList.size()) {
                    Log.d(this, "The conference call has been merged, so do nothing.", new Object[0]);
                } else {
                    this.mConference.updateCapabilities(4);
                    this.mConference.removeCapabilities(8);
                    Log.d(this, "Update merge capability", new Object[0]);
                }
            } else {
                this.mConference.updateCapabilities(8);
                this.mConference.removeCapabilities(4);
                Log.d(this, "Update swap capability", new Object[0]);
            }
            ArrayList<Connection> arrayList2 = new ArrayList(this.mConference.getConnections());
            boolean z4 = false;
            for (MtkGsmCdmaConnection mtkGsmCdmaConnection4 : arrayList) {
                if (!arrayList2.contains(mtkGsmCdmaConnection4)) {
                    Log.i(this, "Adding connection to conference call: %s", mtkGsmCdmaConnection4);
                    this.mConference.addConnection(mtkGsmCdmaConnection4);
                    z4 = true;
                }
                arrayList2.remove(mtkGsmCdmaConnection4);
            }
            for (Connection connection : arrayList2) {
                this.mConference.removeConnection(connection);
                Log.i(this, "Removing connection from conference call: %s", connection);
            }
            if (z) {
                Log.d(this, "Adding the conference call", new Object[0]);
                this.mConference.resetConnectionState();
                this.mConnectionService.addConference(this.mConference);
            } else if (z4) {
                this.mConference.setActive();
                this.mConference.resetConnectionState();
            }
            if (this.mConference.getConnectionCapabilities() != this.mConference.buildConnectionCapabilities()) {
                this.mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (CdmaConferenceController.this.mConference != null) {
                            CdmaConferenceController.this.mConference.updateConnectionCapabilities();
                        }
                    }
                }, 200L);
            }
        } else if (arrayList.isEmpty() && this.mConference != null) {
            Log.i(this, "Destroying the CDMA conference connection.", new Object[0]);
            this.mConference.destroy();
            this.mConference = null;
            this.mSecondCall = null;
            this.mConnectionsToReset.clear();
        }
        this.mConfConnCount = arrayList.size();
    }

    private class CdmaConferenceBroadcastReceiver extends BroadcastReceiver {
        private CdmaConferenceBroadcastReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if ("android.intent.action.EMERGENCY_CALLBACK_MODE_CHANGED".equals(intent.getAction())) {
                boolean booleanExtra = intent.getBooleanExtra("phoneinECMState", false);
                Log.d(CdmaConferenceController.this, "Received ECM changed, isEcm=" + booleanExtra, new Object[0]);
                if (CdmaConferenceController.this.mConference != null) {
                    CdmaConferenceController.this.mConference.updateConnectionCapabilities();
                    return;
                }
                Iterator it = CdmaConferenceController.this.mCdmaConnections.iterator();
                while (it.hasNext()) {
                    ((MtkGsmCdmaConnection) it.next()).updateConnectionCapabilities();
                }
            }
        }
    }

    public void onDestroy() {
        try {
            PhoneGlobals.getInstance().unregisterReceiver(this.mReceiver);
        } catch (IllegalStateException e) {
            Log.e(this, e, "onDestroy, can't get PhoneGlobals", new Object[0]);
        }
    }
}
