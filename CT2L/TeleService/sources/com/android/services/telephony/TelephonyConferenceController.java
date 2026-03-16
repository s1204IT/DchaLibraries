package com.android.services.telephony;

import android.telecom.Conference;
import android.telecom.Connection;
import android.telecom.DisconnectCause;
import com.android.internal.telephony.Call;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class TelephonyConferenceController {
    private final TelephonyConnectionService mConnectionService;
    private TelephonyConference mTelephonyConference;
    private final Connection.Listener mConnectionListener = new Connection.Listener() {
        public void onStateChanged(Connection c, int state) {
            TelephonyConferenceController.this.recalculate();
        }

        public void onDisconnected(Connection c, DisconnectCause disconnectCause) {
            TelephonyConferenceController.this.recalculate();
        }

        public void onDestroyed(Connection connection) {
            TelephonyConferenceController.this.remove(connection);
        }
    };
    private final List<TelephonyConnection> mTelephonyConnections = new ArrayList();

    public TelephonyConferenceController(TelephonyConnectionService connectionService) {
        this.mConnectionService = connectionService;
    }

    void add(TelephonyConnection connection) {
        this.mTelephonyConnections.add(connection);
        connection.addConnectionListener(this.mConnectionListener);
        recalculate();
    }

    void remove(Connection connection) {
        connection.removeConnectionListener(this.mConnectionListener);
        this.mTelephonyConnections.remove(connection);
        recalculate();
    }

    private void recalculate() {
        recalculateConference();
        recalculateConferenceable();
    }

    private boolean isFullConference(Conference conference) {
        return conference.getConnections().size() >= 5;
    }

    private boolean participatesInFullConference(Connection connection) {
        return connection.getConference() != null && isFullConference(connection.getConference());
    }

    private void recalculateConferenceable() {
        Log.v(this, "recalculateConferenceable : %d", Integer.valueOf(this.mTelephonyConnections.size()));
        List<Connection> activeConnections = new ArrayList<>(this.mTelephonyConnections.size());
        List<Connection> backgroundConnections = new ArrayList<>(this.mTelephonyConnections.size());
        for (Connection connection : this.mTelephonyConnections) {
            Log.d(this, "recalc - %s %s", Integer.valueOf(connection.getState()), connection);
            if (!participatesInFullConference(connection)) {
                switch (connection.getState()) {
                    case 4:
                        activeConnections.add(connection);
                        continue;
                    case 5:
                        backgroundConnections.add(connection);
                        continue;
                }
            }
            connection.setConferenceableConnections(Collections.emptyList());
        }
        Log.v(this, "active: %d, holding: %d", Integer.valueOf(activeConnections.size()), Integer.valueOf(backgroundConnections.size()));
        for (Connection connection2 : activeConnections) {
            connection2.setConferenceableConnections(backgroundConnections);
        }
        for (Connection connection3 : backgroundConnections) {
            connection3.setConferenceableConnections(activeConnections);
        }
        if (this.mTelephonyConference != null && !isFullConference(this.mTelephonyConference)) {
            List<Connection> nonConferencedConnections = new ArrayList<>(this.mTelephonyConnections.size());
            for (TelephonyConnection c : this.mTelephonyConnections) {
                if (c.getConference() == null) {
                    nonConferencedConnections.add(c);
                }
            }
            Log.v(this, "conference conferenceable: %s", nonConferencedConnections);
            this.mTelephonyConference.setConferenceableConnections(nonConferencedConnections);
        }
    }

    private void recalculateConference() {
        Set<Connection> conferencedConnections = new HashSet<>();
        int numGsmConnections = 0;
        for (TelephonyConnection connection : this.mTelephonyConnections) {
            com.android.internal.telephony.Connection radioConnection = connection.getOriginalConnection();
            if (radioConnection != null) {
                Call.State state = radioConnection.getState();
                Call call = radioConnection.getCall();
                if (state == Call.State.ACTIVE || state == Call.State.HOLDING) {
                    if (call != null && call.isMultiparty()) {
                        numGsmConnections++;
                        conferencedConnections.add(connection);
                    }
                }
            }
        }
        Log.d(this, "Recalculate conference calls %s %s.", this.mTelephonyConference, conferencedConnections);
        if (numGsmConnections < 2) {
            Log.d(this, "not enough connections to be a conference!", new Object[0]);
            if (this.mTelephonyConference != null) {
                Log.d(this, "with a conference to destroy!", new Object[0]);
                this.mTelephonyConference.destroy();
                this.mTelephonyConference = null;
            }
            return;
        }
        if (this.mTelephonyConference != null) {
            List<Connection> existingConnections = this.mTelephonyConference.getConnections();
            for (Connection connection2 : existingConnections) {
                if ((connection2 instanceof TelephonyConnection) && !conferencedConnections.contains(connection2)) {
                    this.mTelephonyConference.removeConnection(connection2);
                }
            }
            for (Connection connection3 : conferencedConnections) {
                if (!existingConnections.contains(connection3)) {
                    this.mTelephonyConference.addConnection(connection3);
                }
            }
        } else {
            this.mTelephonyConference = new TelephonyConference(null);
            for (Connection connection4 : conferencedConnections) {
                Log.d(this, "Adding a connection to a conference call: %s %s", this.mTelephonyConference, connection4);
                this.mTelephonyConference.addConnection(connection4);
            }
            this.mConnectionService.addConference(this.mTelephonyConference);
        }
        Connection conferencedConnection = this.mTelephonyConference.getPrimaryConnection();
        if (conferencedConnection != null) {
            switch (conferencedConnection.getState()) {
                case 4:
                    this.mTelephonyConference.setActive();
                    break;
                case 5:
                    this.mTelephonyConference.setOnHold();
                    break;
            }
        }
    }
}
