package com.android.services.telephony;

import android.telecom.Conference;
import android.telecom.Connection;
import android.telecom.DisconnectCause;
import android.telecom.IConferenceable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class ImsConferenceController {
    private final TelephonyConnectionService mConnectionService;
    private final Conference.Listener mConferenceListener = new Conference.Listener() {
        public void onDestroyed(Conference conference) {
            if (Log.VERBOSE) {
                Log.v(ImsConferenceController.class, "onDestroyed: %s", conference);
            }
            ImsConferenceController.this.mImsConferences.remove(conference);
        }
    };
    private final Connection.Listener mConnectionListener = new Connection.Listener() {
        public void onStateChanged(Connection c, int state) {
            Log.v(this, "onStateChanged: %s", Log.pii(c.getAddress()));
            ImsConferenceController.this.recalculate();
        }

        public void onDisconnected(Connection c, DisconnectCause disconnectCause) {
            Log.v(this, "onDisconnected: %s", Log.pii(c.getAddress()));
            ImsConferenceController.this.recalculate();
        }

        public void onDestroyed(Connection connection) {
            ImsConferenceController.this.remove(connection);
        }

        public void onConferenceStarted() {
            Log.v(this, "onConferenceStarted", new Object[0]);
            ImsConferenceController.this.recalculateConference();
        }
    };
    private final ArrayList<TelephonyConnection> mTelephonyConnections = new ArrayList<>();
    private final ArrayList<ImsConference> mImsConferences = new ArrayList<>(1);

    public ImsConferenceController(TelephonyConnectionService connectionService) {
        this.mConnectionService = connectionService;
    }

    void add(TelephonyConnection connection) {
        if (Log.VERBOSE) {
            Log.v(this, "add connection %s", connection);
        }
        this.mTelephonyConnections.add(connection);
        connection.addConnectionListener(this.mConnectionListener);
        recalculateConference();
    }

    void remove(Connection connection) {
        if (Log.VERBOSE) {
            Log.v(this, "remove connection: %s", connection);
        }
        this.mTelephonyConnections.remove(connection);
        recalculateConferenceable();
    }

    private void recalculate() {
        recalculateConferenceable();
        recalculateConference();
    }

    private void recalculateConferenceable() {
        Log.v(this, "recalculateConferenceable : %d", Integer.valueOf(this.mTelephonyConnections.size()));
        List<IConferenceable> activeConnections = new ArrayList<>(this.mTelephonyConnections.size());
        List<IConferenceable> backgroundConnections = new ArrayList<>(this.mTelephonyConnections.size());
        for (Connection connection : this.mTelephonyConnections) {
            if (Log.DEBUG) {
                Log.d(this, "recalc - %s %s", Integer.valueOf(connection.getState()), connection);
            }
            switch (connection.getState()) {
                case 4:
                    activeConnections.add(connection);
                    break;
                case 5:
                    backgroundConnections.add(connection);
                    break;
                default:
                    connection.setConferenceableConnections(Collections.emptyList());
                    break;
            }
        }
        for (Conference conference : this.mImsConferences) {
            if (Log.DEBUG) {
                Log.d(this, "recalc - %s %s", Integer.valueOf(conference.getState()), conference);
            }
            switch (conference.getState()) {
                case 4:
                    activeConnections.add(conference);
                    break;
                case 5:
                    backgroundConnections.add(conference);
                    break;
            }
        }
        Log.v(this, "active: %d, holding: %d", Integer.valueOf(activeConnections.size()), Integer.valueOf(backgroundConnections.size()));
        for (IConferenceable iConferenceable : activeConnections) {
            if (iConferenceable instanceof Connection) {
                Connection connection2 = (Connection) iConferenceable;
                connection2.setConferenceables(backgroundConnections);
            }
        }
        for (IConferenceable iConferenceable2 : backgroundConnections) {
            if (iConferenceable2 instanceof Connection) {
                Connection connection3 = (Connection) iConferenceable2;
                connection3.setConferenceables(activeConnections);
            }
        }
        for (ImsConference conference2 : this.mImsConferences) {
            List<Connection> nonConferencedConnections = new ArrayList<>(this.mTelephonyConnections.size());
            for (Connection c : this.mTelephonyConnections) {
                if (c.getConference() == null) {
                    nonConferencedConnections.add(c);
                }
            }
            if (Log.VERBOSE) {
                Log.v(this, "conference conferenceable: %s", nonConferencedConnections);
            }
            conference2.setConferenceableConnections(nonConferencedConnections);
        }
    }

    private void recalculateConference() {
        Log.v(this, "recalculateConference", new Object[0]);
        Iterator<TelephonyConnection> it = this.mTelephonyConnections.iterator();
        while (it.hasNext()) {
            TelephonyConnection connection = it.next();
            if (connection.isImsConnection() && connection.getOriginalConnection() != null && connection.getOriginalConnection().isMultiparty()) {
                startConference(connection);
                it.remove();
            }
        }
    }

    private void startConference(TelephonyConnection connection) {
        if (Log.VERBOSE) {
            Log.v(this, "Start new ImsConference - connection: %s", connection);
        }
        TelephonyConnection conferenceHostConnection = connection.cloneConnection();
        ImsConference conference = new ImsConference(this.mConnectionService, conferenceHostConnection);
        conference.setState(connection.getState());
        this.mConnectionService.addConference(conference);
        conference.addListener(this.mConferenceListener);
        connection.removeConnectionListener(this.mConnectionListener);
        connection.clearOriginalConnection();
        connection.setDisconnected(new DisconnectCause(9));
        connection.destroy();
        this.mImsConferences.add(conference);
    }
}
