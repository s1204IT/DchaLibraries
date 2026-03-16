package com.android.services.telephony;

import android.os.Handler;
import android.telecom.Connection;
import android.telecom.DisconnectCause;
import java.util.ArrayList;
import java.util.List;

final class CdmaConferenceController {
    private CdmaConference mConference;
    private final TelephonyConnectionService mConnectionService;
    private final Connection.Listener mConnectionListener = new Connection.Listener() {
        public void onStateChanged(Connection c, int state) {
            CdmaConferenceController.this.recalculateConference();
        }

        public void onDisconnected(Connection c, DisconnectCause disconnectCause) {
            CdmaConferenceController.this.recalculateConference();
        }

        public void onDestroyed(Connection c) {
            CdmaConferenceController.this.remove((CdmaConnection) c);
        }
    };
    private final List<CdmaConnection> mCdmaConnections = new ArrayList();
    private final List<CdmaConnection> mPendingOutgoingConnections = new ArrayList();
    private final Handler mHandler = new Handler();

    public CdmaConferenceController(TelephonyConnectionService connectionService) {
        this.mConnectionService = connectionService;
    }

    void add(final CdmaConnection connection) {
        if (!this.mCdmaConnections.isEmpty() && connection.isOutgoing()) {
            connection.forceAsDialing(true);
            final List<CdmaConnection> connectionsToReset = new ArrayList<>(this.mCdmaConnections.size());
            for (CdmaConnection current : this.mCdmaConnections) {
                if (current.setHoldingForConference()) {
                    connectionsToReset.add(current);
                }
            }
            this.mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    connection.forceAsDialing(false);
                    CdmaConferenceController.this.addInternal(connection);
                    for (CdmaConnection current2 : connectionsToReset) {
                        current2.resetStateForConference();
                    }
                }
            }, 6000L);
            return;
        }
        addInternal(connection);
    }

    private void addInternal(CdmaConnection connection) {
        this.mCdmaConnections.add(connection);
        connection.addConnectionListener(this.mConnectionListener);
        recalculateConference();
    }

    private void remove(CdmaConnection connection) {
        connection.removeConnectionListener(this.mConnectionListener);
        this.mCdmaConnections.remove(connection);
        recalculateConference();
    }

    private void recalculateConference() {
        List<CdmaConnection> conferenceConnections = new ArrayList<>(this.mCdmaConnections.size());
        for (CdmaConnection connection : this.mCdmaConnections) {
            if (!connection.isCallWaiting() && connection.getState() != 6) {
                conferenceConnections.add(connection);
            }
        }
        Log.d(this, "recalculating conference calls %d", Integer.valueOf(conferenceConnections.size()));
        if (conferenceConnections.size() >= 2) {
            boolean isNewlyCreated = false;
            if (this.mConference == null) {
                Log.i(this, "Creating new Cdma conference call", new Object[0]);
                this.mConference = new CdmaConference(null);
                isNewlyCreated = true;
            }
            CdmaConnection newConnection = this.mCdmaConnections.get(this.mCdmaConnections.size() - 1);
            if (newConnection.isOutgoing()) {
                this.mConference.updateCapabilities(4);
            } else {
                this.mConference.updateCapabilities(8);
            }
            List<Connection> existingChildConnections = new ArrayList<>(this.mConference.getConnections());
            for (CdmaConnection connection2 : conferenceConnections) {
                if (!existingChildConnections.contains(connection2)) {
                    Log.i(this, "Adding connection to conference call: %s", connection2);
                    this.mConference.addConnection(connection2);
                }
                existingChildConnections.remove(connection2);
            }
            for (Connection oldConnection : existingChildConnections) {
                this.mConference.removeConnection(oldConnection);
                Log.i(this, "Removing connection from conference call: %s", oldConnection);
            }
            if (isNewlyCreated) {
                Log.d(this, "Adding the conference call", new Object[0]);
                this.mConnectionService.addConference(this.mConference);
                return;
            }
            return;
        }
        if (conferenceConnections.isEmpty() && this.mConference != null) {
            Log.i(this, "Destroying the CDMA conference connection.", new Object[0]);
            this.mConference.destroy();
            this.mConference = null;
        }
    }
}
