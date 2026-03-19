package com.android.services.telephony;

import android.os.PersistableBundle;
import android.telecom.Conference;
import android.telecom.Connection;
import android.telecom.DisconnectCause;
import android.telecom.PhoneAccountHandle;
import com.android.internal.telephony.Call;
import com.android.phone.PhoneUtils;
import com.mediatek.phone.ext.ExtensionManager;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

final class TelephonyConferenceController {
    private static final int TELEPHONY_CONFERENCE_MAX_SIZE = 5;
    private final TelephonyConnectionServiceProxy mConnectionService;
    private TelephonyConference mTelephonyConference;
    private TelephonyConference mHandoverTelephonyConference = null;
    private final Connection.Listener mConnectionListener = new Connection.Listener() {
        public void onStateChanged(Connection connection, int i) {
            Log.v(this, "onStateChange triggered in Conf Controller : connection = " + connection + " state = " + i, new Object[0]);
            TelephonyConferenceController.this.recalculate();
        }

        public void onDisconnected(Connection connection, DisconnectCause disconnectCause) {
            TelephonyConferenceController.this.recalculate();
        }

        public void onDestroyed(Connection connection) {
            TelephonyConferenceController.this.remove(connection);
        }
    };
    private final List<TelephonyConnection> mTelephonyConnections = new ArrayList();
    private boolean mTriggerRecalculate = false;

    public TelephonyConferenceController(TelephonyConnectionServiceProxy telephonyConnectionServiceProxy) {
        this.mConnectionService = telephonyConnectionServiceProxy;
    }

    boolean shouldRecalculate() {
        Log.d(this, "shouldRecalculate is " + this.mTriggerRecalculate, new Object[0]);
        return this.mTriggerRecalculate;
    }

    void add(TelephonyConnection telephonyConnection) {
        if (this.mTelephonyConnections.contains(telephonyConnection)) {
            Log.w(this, "add - connection already tracked; connection=%s", telephonyConnection);
            return;
        }
        this.mTelephonyConnections.add(telephonyConnection);
        telephonyConnection.addConnectionListener(this.mConnectionListener);
        recalculate();
    }

    void remove(Connection connection) {
        if (!this.mTelephonyConnections.contains(connection)) {
            Log.d(this, "remove - connection not tracked; connection=%s", connection);
            return;
        }
        connection.removeConnectionListener(this.mConnectionListener);
        this.mTelephonyConnections.remove(connection);
        recalculate();
    }

    void recalculate() {
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
        boolean z;
        Log.v(this, "recalculateConferenceable : %d", Integer.valueOf(this.mTelephonyConnections.size()));
        HashSet<Connection> hashSet = new HashSet(this.mTelephonyConnections.size());
        for (TelephonyConnection telephonyConnection : this.mTelephonyConnections) {
            Log.d(this, "recalc - %s %s supportsConf? %s", Integer.valueOf(telephonyConnection.getState()), telephonyConnection, Boolean.valueOf(telephonyConnection.isConferenceSupported()));
            if (telephonyConnection.isConferenceSupported() && !participatesInFullConference(telephonyConnection)) {
                switch (telephonyConnection.getState()) {
                    case 4:
                    case 5:
                        hashSet.add(telephonyConnection);
                        continue;
                }
            }
            telephonyConnection.setConferenceableConnections(Collections.emptyList());
        }
        PersistableBundle carrierConfig = getCarrierConfig();
        if (carrierConfig != null) {
            z = carrierConfig.getBoolean("mtk_key_multiline_allow_cross_line_conference_bool");
        } else {
            z = false;
        }
        if (!z && !ExtensionManager.getDigitsUtilExt().areConnectionsInSameLine(this.mTelephonyConnections, this.mTelephonyConference)) {
            return;
        }
        Log.v(this, "conferenceable: " + hashSet.size(), new Object[0]);
        for (final Connection connection : hashSet) {
            connection.setConferenceableConnections((List) hashSet.stream().filter(new Predicate() {
                @Override
                public final boolean test(Object obj) {
                    return TelephonyConferenceController.lambda$recalculateConferenceable$0(connection, (Connection) obj);
                }
            }).collect(Collectors.toList()));
        }
        if (this.mTelephonyConference != null) {
            if (!isFullConference(this.mTelephonyConference)) {
                this.mTelephonyConference.setConferenceableConnections((List) this.mTelephonyConnections.stream().filter(new Predicate() {
                    @Override
                    public final boolean test(Object obj) {
                        return TelephonyConferenceController.lambda$recalculateConferenceable$1(this.f$0, (TelephonyConnection) obj);
                    }
                }).collect(Collectors.toList()));
            } else {
                Log.d(this, "cannot merge anymore due it is full", new Object[0]);
                this.mTelephonyConference.setConferenceableConnections(Collections.emptyList());
            }
        }
    }

    static boolean lambda$recalculateConferenceable$0(Connection connection, Connection connection2) {
        return connection != connection2;
    }

    public static boolean lambda$recalculateConferenceable$1(TelephonyConferenceController telephonyConferenceController, TelephonyConnection telephonyConnection) {
        return telephonyConnection.isConferenceSupported() && telephonyConnection.getConference() == null && !telephonyConferenceController.isFullConference(telephonyConferenceController.mTelephonyConference);
    }

    private PersistableBundle getCarrierConfig() {
        TelephonyConnection next;
        Iterator<TelephonyConnection> it = this.mTelephonyConnections.iterator();
        PersistableBundle carrierConfig = null;
        while (it.hasNext() && ((next = it.next()) == null || (carrierConfig = next.getCarrierConfig()) == null)) {
        }
        return carrierConfig;
    }

    private void recalculateConference() {
        boolean z;
        PhoneAccountHandle handleByConnectionIfRequired;
        boolean z2;
        HashSet<Connection> hashSet = new HashSet();
        int i = 0;
        for (TelephonyConnection telephonyConnection : this.mTelephonyConnections) {
            com.android.internal.telephony.Connection originalConnection = telephonyConnection.getOriginalConnection();
            if (originalConnection != null) {
                Call.State state = originalConnection.getState();
                Call call = originalConnection.getCall();
                if (state == Call.State.ACTIVE || state == Call.State.HOLDING) {
                    if (call != null && call.isMultiparty()) {
                        i++;
                        hashSet.add(telephonyConnection);
                    }
                }
            }
        }
        Log.d(this, "Recalculate conference calls %s %s.", this.mTelephonyConference, hashSet);
        if (this.mTelephonyConference != null && this.mHandoverTelephonyConference != null) {
            Log.d(this, "SRVCC: abnormal case!", new Object[0]);
        }
        Collection<Connection> allConnections = this.mConnectionService.getAllConnections();
        Iterator it = hashSet.iterator();
        while (true) {
            if (it.hasNext()) {
                Connection connection = (Connection) it.next();
                Log.v(this, "Finding connection in Connection Service for " + connection, new Object[0]);
                if (!allConnections.contains(connection)) {
                    Log.v(this, "Finding connection in Connection Service Failed", new Object[0]);
                    z = false;
                    break;
                }
            } else {
                z = true;
                break;
            }
        }
        Log.d(this, "Is there a match for all connections in connection service " + z, new Object[0]);
        if (i < 2) {
            Log.d(this, "not enough connections to be a conference!", new Object[0]);
            if (this.mTelephonyConference != null) {
                Log.d(this, "with a conference to destroy!", new Object[0]);
                this.mTelephonyConference.destroy();
                this.mTelephonyConference = null;
                return;
            }
            return;
        }
        if (this.mTelephonyConference != null) {
            List<Connection> connections = this.mTelephonyConference.getConnections();
            for (Connection connection2 : connections) {
                if ((connection2 instanceof TelephonyConnection) && !hashSet.contains(connection2)) {
                    this.mTelephonyConference.removeConnection(connection2);
                }
            }
            if (z) {
                this.mTriggerRecalculate = false;
                for (Connection connection3 : hashSet) {
                    if (!connections.contains(connection3)) {
                        this.mTelephonyConference.addConnection(connection3);
                    }
                }
            } else {
                Log.d(this, "Trigger recalculate later", new Object[0]);
                this.mTriggerRecalculate = true;
            }
        } else if (z) {
            this.mTriggerRecalculate = false;
            if (!hashSet.isEmpty()) {
                TelephonyConnection telephonyConnection2 = (TelephonyConnection) hashSet.iterator().next();
                handleByConnectionIfRequired = ExtensionManager.getDigitsUtilExt().getHandleByConnectionIfRequired(PhoneUtils.makePstnPhoneAccountHandle(telephonyConnection2.getPhone()), telephonyConnection2);
            } else {
                handleByConnectionIfRequired = null;
            }
            this.mTelephonyConference = new TelephonyConference(handleByConnectionIfRequired);
            if (this.mHandoverTelephonyConference != null) {
                Log.d(this, "SRVCC: assign handover conference to telephonyConference", new Object[0]);
                this.mTelephonyConference = this.mHandoverTelephonyConference;
                this.mHandoverTelephonyConference = null;
                z2 = true;
            } else {
                z2 = false;
            }
            for (Connection connection4 : hashSet) {
                Log.d(this, "Adding a connection to a conference call: %s %s", this.mTelephonyConference, connection4);
                this.mTelephonyConference.addConnection(connection4);
            }
            if (z2) {
                Log.d(this, "SRVCC: skip adding conference to connectionService", new Object[0]);
            } else {
                this.mConnectionService.addConference(this.mTelephonyConference);
            }
        } else {
            Log.d(this, "Trigger recalculate later", new Object[0]);
            this.mTriggerRecalculate = true;
        }
        if (this.mTelephonyConference != null) {
            Connection primaryConnection = this.mTelephonyConference.getPrimaryConnection();
            Log.v(this, "Primary Conferenced connection is " + primaryConnection, new Object[0]);
            if (primaryConnection != null) {
                switch (primaryConnection.getState()) {
                    case 4:
                        Log.v(this, "Setting conference to active", new Object[0]);
                        this.mTelephonyConference.setActive();
                        break;
                    case 5:
                        Log.v(this, "Setting conference to hold", new Object[0]);
                        this.mTelephonyConference.setOnHold();
                        break;
                }
            }
        }
    }

    public void setHandoveredConference(TelephonyConference telephonyConference) {
        Log.d(this, "config the handover conference!", new Object[0]);
        this.mHandoverTelephonyConference = telephonyConference;
    }
}
