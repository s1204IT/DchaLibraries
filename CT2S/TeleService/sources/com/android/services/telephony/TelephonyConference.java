package com.android.services.telephony;

import android.telecom.Conference;
import android.telecom.Connection;
import android.telecom.PhoneAccountHandle;
import com.android.internal.telephony.Call;
import com.android.internal.telephony.CallStateException;
import com.android.internal.telephony.Phone;
import java.util.List;

public class TelephonyConference extends Conference {
    public TelephonyConference(PhoneAccountHandle phoneAccount) {
        super(phoneAccount);
        setConnectionCapabilities(195);
        setActive();
    }

    @Override
    public void onDisconnect() {
        for (Connection connection : getConnections()) {
            if (disconnectCall(connection)) {
                return;
            }
        }
    }

    private boolean disconnectCall(Connection connection) {
        Call call = getMultipartyCallForConnection(connection, "onDisconnect");
        if (call == null) {
            return false;
        }
        Log.d(this, "Found multiparty call to hangup for conference.", new Object[0]);
        try {
            call.hangup();
            return true;
        } catch (CallStateException e) {
            Log.e(this, e, "Exception thrown trying to hangup conference", new Object[0]);
            return false;
        }
    }

    @Override
    public void onSeparate(Connection connection) {
        com.android.internal.telephony.Connection radioConnection = getOriginalConnection(connection);
        try {
            radioConnection.separate();
        } catch (CallStateException e) {
            Log.e(this, e, "Exception thrown trying to separate a conference call", new Object[0]);
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
            Log.e(this, e, "Exception thrown trying to merge call into a conference", new Object[0]);
        }
    }

    @Override
    public void onHold() {
        TelephonyConnection connection = getFirstConnection();
        if (connection != null) {
            connection.performHold();
        }
    }

    @Override
    public void onUnhold() {
        TelephonyConnection connection = getFirstConnection();
        if (connection != null) {
            connection.performUnhold();
        }
    }

    @Override
    public void onPlayDtmfTone(char c) {
        TelephonyConnection connection = getFirstConnection();
        if (connection != null) {
            connection.onPlayDtmfTone(c);
        }
    }

    @Override
    public void onStopDtmfTone() {
        TelephonyConnection connection = getFirstConnection();
        if (connection != null) {
            connection.onStopDtmfTone();
        }
    }

    @Override
    public void onConnectionAdded(Connection connection) {
        if ((connection instanceof TelephonyConnection) && ((TelephonyConnection) connection).wasImsConnection()) {
            removeCapability(128);
        }
    }

    public Connection getPrimaryConnection() {
        List<Connection> connections = getConnections();
        if (connections == null || connections.isEmpty()) {
            return null;
        }
        Connection connection = connections.get(0);
        for (Connection connection2 : connections) {
            com.android.internal.telephony.Connection radioConnection = getOriginalConnection(connection2);
            if (radioConnection != null && radioConnection.isMultiparty()) {
                return connection2;
            }
        }
        return connection;
    }

    private Call getMultipartyCallForConnection(Connection connection, String tag) {
        Call call;
        com.android.internal.telephony.Connection radioConnection = getOriginalConnection(connection);
        if (radioConnection == null || (call = radioConnection.getCall()) == null || !call.isMultiparty()) {
            return null;
        }
        return call;
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
}
