package com.android.services.telephony;

import android.telecom.Conference;
import android.telecom.Connection;
import android.telecom.PhoneAccountHandle;
import com.android.internal.telephony.Call;
import com.android.internal.telephony.CallStateException;
import com.android.internal.telephony.Phone;
import com.mediatek.internal.telephony.MtkGsmCdmaPhone;
import java.util.Iterator;
import java.util.List;

public class TelephonyConference extends Conference implements Holdable {
    private static final String TAG = "TelephonyConf";
    private boolean mIsHoldable;

    public TelephonyConference(PhoneAccountHandle phoneAccountHandle) {
        super(phoneAccountHandle);
        setConnectionCapabilities(195);
        setActive();
        this.mIsHoldable = true;
    }

    @Override
    public void onDisconnect() {
        Iterator<Connection> it = getConnections().iterator();
        while (it.hasNext() && !disconnectCall(it.next())) {
        }
    }

    private boolean disconnectCall(Connection connection) {
        Call multipartyCallForConnection = getMultipartyCallForConnection(connection, "onDisconnect");
        if (multipartyCallForConnection != null) {
            Log.d(this, "Found multiparty call to hangup for conference.", new Object[0]);
            try {
                multipartyCallForConnection.hangup();
                return true;
            } catch (CallStateException e) {
                Log.e((Object) this, (Throwable) e, "Exception thrown trying to hangup conference", new Object[0]);
            }
        }
        return false;
    }

    @Override
    public void onSeparate(Connection connection) {
        try {
            getOriginalConnection(connection).separate();
        } catch (CallStateException e) {
            Log.e((Object) this, (Throwable) e, "Exception thrown trying to separate a conference call", new Object[0]);
        }
    }

    @Override
    public void onMerge(Connection connection) {
        try {
            Phone phone = ((TelephonyConnection) connection).getPhone();
            if (phone != null) {
                phone.conference();
            }
        } catch (CallStateException e) {
            Log.e((Object) this, (Throwable) e, "Exception thrown trying to merge call into a conference", new Object[0]);
        }
    }

    @Override
    public void onHold() {
        TelephonyConnection firstConnection = getFirstConnection();
        if (firstConnection != null) {
            firstConnection.performHold();
        }
    }

    @Override
    public void onUnhold() {
        TelephonyConnection firstConnection = getFirstConnection();
        if (firstConnection != null) {
            firstConnection.performUnhold();
        }
    }

    @Override
    public void onPlayDtmfTone(char c) {
        TelephonyConnection firstConnection = getFirstConnection();
        if (firstConnection != null) {
            firstConnection.onPlayDtmfTone(c);
        }
    }

    @Override
    public void onStopDtmfTone() {
        TelephonyConnection firstConnection = getFirstConnection();
        if (firstConnection != null) {
            firstConnection.onStopDtmfTone();
        }
    }

    @Override
    public void onConnectionAdded(Connection connection) {
        if ((connection instanceof TelephonyConnection) && ((TelephonyConnection) connection).wasImsConnection()) {
            removeCapability(128);
        }
        com.android.internal.telephony.Connection originalConnection = getOriginalConnection(connection);
        if (originalConnection != null) {
            setConnectionTime(originalConnection.getCall().getEarliestConnectTime());
        }
    }

    public Connection getPrimaryConnection() {
        List<Connection> connections = getConnections();
        if (connections == null || connections.isEmpty()) {
            return null;
        }
        Connection connection = connections.get(0);
        for (Connection connection2 : connections) {
            com.android.internal.telephony.Connection originalConnection = getOriginalConnection(connection2);
            if (originalConnection != null && originalConnection.isMultiparty()) {
                return connection2;
            }
        }
        return connection;
    }

    @Override
    public void setHoldable(boolean z) {
        this.mIsHoldable = z;
        if (!this.mIsHoldable) {
            removeCapability(1);
        } else {
            addCapability(1);
        }
    }

    @Override
    public boolean isChildHoldable() {
        return false;
    }

    private Call getMultipartyCallForConnection(Connection connection, String str) {
        Call call;
        com.android.internal.telephony.Connection originalConnection = getOriginalConnection(connection);
        if (originalConnection != null && (call = originalConnection.getCall()) != null && call.isMultiparty()) {
            return call;
        }
        return null;
    }

    protected com.android.internal.telephony.Connection getOriginalConnection(Connection connection) {
        if (connection instanceof TelephonyConnection) {
            return ((TelephonyConnection) connection).getOriginalConnection();
        }
        return null;
    }

    private TelephonyConnection getFirstConnection() {
        List<Connection> connections = getConnections();
        if (connections.isEmpty()) {
            return null;
        }
        return (TelephonyConnection) connections.get(0);
    }

    public void onHangupAll() {
        TelephonyConnection firstConnection = getFirstConnection();
        if (firstConnection != null) {
            try {
                MtkGsmCdmaPhone phone = firstConnection.getPhone();
                if (phone != null && (phone instanceof MtkGsmCdmaPhone)) {
                    phone.hangupAll();
                } else {
                    Log.w(TAG, "Attempting to hangupAll a connection without backing phone.", new Object[0]);
                }
            } catch (CallStateException e) {
                Log.e(TAG, (Throwable) e, "Exception thrown trying to hangupAll a conference", new Object[0]);
            }
        }
    }

    public void onDisconnect(String str) {
        Iterator<Connection> it = getConnections().iterator();
        while (it.hasNext() && !disconnectCall(it.next(), str)) {
        }
    }

    private boolean disconnectCall(Connection connection, String str) {
        Call multipartyCallForConnection = getMultipartyCallForConnection(connection, "onDisconnect");
        if (multipartyCallForConnection != null) {
            log("Found multiparty call to hangup for conference, with pending action");
            log("pendingCallAction = " + str);
            if ("answer".equals(str) || "unhold".equals(str)) {
                for (com.android.internal.telephony.Connection connection2 : multipartyCallForConnection.getConnections()) {
                    if (connection2 != null && connection2.isAlive()) {
                        try {
                            connection2.hangup();
                        } catch (CallStateException e) {
                            Log.e(TAG, (Throwable) e, "Exception thrown trying to hangup conference member", new Object[0]);
                            return false;
                        }
                    }
                }
                return true;
            }
            try {
                multipartyCallForConnection.hangup();
                return true;
            } catch (CallStateException e2) {
                Log.e(TAG, (Throwable) e2, "Exception thrown trying to hangup conference", new Object[0]);
            }
        }
        return false;
    }

    private void log(String str) {
        Log.d(TAG, str, new Object[0]);
    }
}
