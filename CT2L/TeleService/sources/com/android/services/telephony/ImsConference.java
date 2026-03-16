package com.android.services.telephony;

import android.net.Uri;
import android.telecom.Conference;
import android.telecom.ConferenceParticipant;
import android.telecom.Connection;
import android.telecom.DisconnectCause;
import android.telecom.PhoneAccountHandle;
import com.android.internal.telephony.Call;
import com.android.internal.telephony.CallStateException;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.imsphone.ImsPhoneConnection;
import com.android.phone.PhoneUtils;
import com.android.services.telephony.TelephonyConnection;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ImsConference extends Conference {
    private TelephonyConnection mConferenceHost;
    private final Connection.Listener mConferenceHostListener;
    private final ConcurrentHashMap<Uri, ConferenceParticipantConnection> mConferenceParticipantConnections;
    private final Connection.Listener mParticipantListener;
    private final TelephonyConnection.TelephonyConnectionListener mTelephonyConnectionListener;
    private TelephonyConnectionService mTelephonyConnectionService;

    public ImsConference(TelephonyConnectionService telephonyConnectionService, TelephonyConnection conferenceHost) {
        super(null);
        this.mParticipantListener = new Connection.Listener() {
            public void onDestroyed(Connection connection) {
                ConferenceParticipantConnection participant = (ConferenceParticipantConnection) connection;
                ImsConference.this.removeConferenceParticipant(participant);
                ImsConference.this.updateManageConference();
            }
        };
        this.mTelephonyConnectionListener = new TelephonyConnection.TelephonyConnectionListener() {
            @Override
            public void onOriginalConnectionConfigured(TelephonyConnection c) {
                if (c == ImsConference.this.mConferenceHost) {
                    ImsConference.this.handleOriginalConnectionChange();
                }
            }
        };
        this.mConferenceHostListener = new Connection.Listener() {
            public void onStateChanged(Connection c, int state) {
                ImsConference.this.setState(state);
            }

            public void onDisconnected(Connection c, DisconnectCause disconnectCause) {
                ImsConference.this.setDisconnected(disconnectCause);
            }

            public void onDestroyed(Connection connection) {
                ImsConference.this.disconnectConferenceParticipants();
            }

            public void onConferenceParticipantsChanged(Connection c, List<ConferenceParticipant> participants) {
                if (c != null) {
                    Log.v(this, "onConferenceParticipantsChanged: %d participants", Integer.valueOf(participants.size()));
                    TelephonyConnection telephonyConnection = (TelephonyConnection) c;
                    ImsConference.this.handleConferenceParticipantsUpdate(telephonyConnection, participants);
                }
            }
        };
        this.mConferenceParticipantConnections = new ConcurrentHashMap<>(8, 0.9f, 1);
        setConnectTimeMillis(conferenceHost.getOriginalConnection().getConnectTime());
        this.mTelephonyConnectionService = telephonyConnectionService;
        setConferenceHost(conferenceHost);
        if (conferenceHost != null && conferenceHost.getCall() != null && conferenceHost.getCall().getPhone() != null) {
            this.mPhoneAccount = PhoneUtils.makePstnPhoneAccountHandle(conferenceHost.getCall().getPhone());
            Log.v(this, "set phacc to " + this.mPhoneAccount, new Object[0]);
        }
        setConnectionCapabilities(67);
    }

    public Connection getPrimaryConnection() {
        return null;
    }

    @Override
    public void onDisconnect() {
        Call call;
        Log.v(this, "onDisconnect: hanging up conference host.", new Object[0]);
        if (this.mConferenceHost != null && (call = this.mConferenceHost.getCall()) != null) {
            try {
                call.hangup();
            } catch (CallStateException e) {
                Log.e(this, e, "Exception thrown trying to hangup conference", new Object[0]);
            }
        }
    }

    @Override
    public void onSeparate(Connection connection) {
        Log.wtf(this, "Cannot separate connections from an IMS conference.", new Object[0]);
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
        if (this.mConferenceHost != null) {
            this.mConferenceHost.performHold();
        }
    }

    @Override
    public void onUnhold() {
        if (this.mConferenceHost != null) {
            this.mConferenceHost.performUnhold();
        }
    }

    @Override
    public void onPlayDtmfTone(char c) {
        if (this.mConferenceHost != null) {
            this.mConferenceHost.onPlayDtmfTone(c);
        }
    }

    @Override
    public void onStopDtmfTone() {
        if (this.mConferenceHost != null) {
            this.mConferenceHost.onStopDtmfTone();
        }
    }

    @Override
    public void onConnectionAdded(Connection connection) {
    }

    private void updateManageConference() {
        boolean couldManageConference = can(128);
        boolean canManageConference = !this.mConferenceParticipantConnections.isEmpty();
        Object[] objArr = new Object[2];
        objArr[0] = couldManageConference ? "Y" : "N";
        objArr[1] = canManageConference ? "Y" : "N";
        Log.v(this, "updateManageConference was:%s is:%s", objArr);
        if (couldManageConference != canManageConference) {
            getConnectionCapabilities();
            if (canManageConference) {
                addCapability(128);
            } else {
                removeCapability(128);
            }
        }
    }

    private void setConferenceHost(TelephonyConnection conferenceHost) {
        if (Log.VERBOSE) {
            Log.v(this, "setConferenceHost " + conferenceHost, new Object[0]);
        }
        this.mConferenceHost = conferenceHost;
        this.mConferenceHost.addConnectionListener(this.mConferenceHostListener);
        this.mConferenceHost.addTelephonyConnectionListener(this.mTelephonyConnectionListener);
    }

    private void handleConferenceParticipantsUpdate(TelephonyConnection parent, List<ConferenceParticipant> participants) {
        boolean newParticipantsAdded = false;
        boolean oldParticipantsRemoved = false;
        ArrayList<ConferenceParticipant> newParticipants = new ArrayList<>(participants.size());
        HashSet<Uri> participantEndpoints = new HashSet<>(participants.size());
        for (ConferenceParticipant participant : participants) {
            Uri endpoint = participant.getEndpoint();
            participantEndpoints.add(endpoint);
            if (!this.mConferenceParticipantConnections.containsKey(endpoint)) {
                createConferenceParticipantConnection(parent, participant);
                newParticipants.add(participant);
                newParticipantsAdded = true;
            } else {
                ConferenceParticipantConnection connection = this.mConferenceParticipantConnections.get(endpoint);
                connection.updateState(participant.getState());
            }
        }
        if (newParticipantsAdded) {
            for (ConferenceParticipant newParticipant : newParticipants) {
                ConferenceParticipantConnection connection2 = this.mConferenceParticipantConnections.get(newParticipant.getEndpoint());
                connection2.updateState(newParticipant.getState());
            }
        }
        Iterator<Map.Entry<Uri, ConferenceParticipantConnection>> entryIterator = this.mConferenceParticipantConnections.entrySet().iterator();
        while (entryIterator.hasNext()) {
            Map.Entry<Uri, ConferenceParticipantConnection> entry = entryIterator.next();
            if (!participantEndpoints.contains(entry.getKey())) {
                ConferenceParticipantConnection participant2 = entry.getValue();
                participant2.removeConnectionListener(this.mParticipantListener);
                removeConnection(participant2);
                entryIterator.remove();
                oldParticipantsRemoved = true;
            }
        }
        if (newParticipantsAdded || oldParticipantsRemoved) {
            updateManageConference();
        }
    }

    private void createConferenceParticipantConnection(TelephonyConnection parent, ConferenceParticipant participant) {
        ConferenceParticipantConnection connection = new ConferenceParticipantConnection(parent.getOriginalConnection(), participant);
        connection.addConnectionListener(this.mParticipantListener);
        if (Log.VERBOSE) {
            Log.v(this, "createConferenceParticipantConnection: %s", connection);
        }
        this.mConferenceParticipantConnections.put(participant.getEndpoint(), connection);
        PhoneAccountHandle phoneAccountHandle = PhoneUtils.makePstnPhoneAccountHandle(parent.getPhone());
        this.mTelephonyConnectionService.addExistingConnection(phoneAccountHandle, connection);
        addConnection(connection);
    }

    private void removeConferenceParticipant(ConferenceParticipantConnection participant) {
        if (Log.VERBOSE) {
            Log.v(this, "removeConferenceParticipant: %s", participant);
        }
        participant.removeConnectionListener(this.mParticipantListener);
        participant.getEndpoint();
        this.mConferenceParticipantConnections.remove(participant.getEndpoint());
    }

    private void disconnectConferenceParticipants() {
        Log.v(this, "disconnectConferenceParticipants", new Object[0]);
        for (ConferenceParticipantConnection connection : this.mConferenceParticipantConnections.values()) {
            removeConferenceParticipant(connection);
            connection.setDisconnected(new DisconnectCause(4));
            connection.destroy();
        }
        this.mConferenceParticipantConnections.clear();
    }

    private void handleOriginalConnectionChange() {
        if (this.mConferenceHost == null) {
            Log.w(this, "handleOriginalConnectionChange; conference host missing.", new Object[0]);
            return;
        }
        com.android.internal.telephony.Connection originalConnection = this.mConferenceHost.getOriginalConnection();
        if (!(originalConnection instanceof ImsPhoneConnection)) {
            if (Log.VERBOSE) {
                Log.v(this, "Original connection for conference host is no longer an IMS connection; new connection: %s", originalConnection);
            }
            PhoneAccountHandle phoneAccountHandle = PhoneUtils.makePstnPhoneAccountHandle(this.mConferenceHost.getPhone());
            this.mTelephonyConnectionService.addExistingConnection(phoneAccountHandle, this.mConferenceHost);
            this.mConferenceHost.removeConnectionListener(this.mConferenceHostListener);
            this.mConferenceHost.removeTelephonyConnectionListener(this.mTelephonyConnectionListener);
            this.mConferenceHost = null;
            setDisconnected(new DisconnectCause(9));
            destroy();
        }
    }

    public void setState(int state) {
        DisconnectCause disconnectCause;
        Log.v(this, "setState %s", Connection.stateToString(state));
        switch (state) {
            case 4:
                setActive();
                break;
            case 5:
                setOnHold();
                break;
            case 6:
                if (this.mConferenceHost == null) {
                    disconnectCause = new DisconnectCause(4);
                } else {
                    disconnectCause = DisconnectCauseUtil.toTelecomDisconnectCause(this.mConferenceHost.getOriginalConnection().getDisconnectCause());
                }
                setDisconnected(disconnectCause);
                destroy();
                break;
        }
    }

    @Override
    public String toString() {
        return "[ImsConference objId:" + System.identityHashCode(this) + " state:" + Connection.stateToString(getState()) + " hostConnection:" + this.mConferenceHost + " participants:" + this.mConferenceParticipantConnections.size() + "]";
    }
}
