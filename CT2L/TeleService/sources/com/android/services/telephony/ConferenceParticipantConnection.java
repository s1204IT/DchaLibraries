package com.android.services.telephony;

import android.net.Uri;
import android.telecom.ConferenceParticipant;
import android.telecom.Connection;
import android.telecom.DisconnectCause;

public class ConferenceParticipantConnection extends Connection {
    private final Uri mEndpoint;
    private final com.android.internal.telephony.Connection mParentConnection;

    public ConferenceParticipantConnection(com.android.internal.telephony.Connection parentConnection, ConferenceParticipant participant) {
        this.mParentConnection = parentConnection;
        setAddress(participant.getHandle(), 1);
        setCallerDisplayName(participant.getDisplayName(), 1);
        this.mEndpoint = participant.getEndpoint();
        setCapabilities();
    }

    public void updateState(int newState) {
        Log.v(this, "updateState endPoint: %s state: %s", Log.pii(this.mEndpoint), Connection.stateToString(newState));
        if (newState != getState()) {
            switch (newState) {
                case 0:
                    setInitializing();
                    break;
                case 1:
                default:
                    setActive();
                    break;
                case 2:
                    setRinging();
                    break;
                case 3:
                    setDialing();
                    break;
                case 4:
                    setActive();
                    break;
                case 5:
                    setOnHold();
                    break;
                case 6:
                    setDisconnected(new DisconnectCause(4));
                    destroy();
                    break;
            }
        }
    }

    @Override
    public void onDisconnect() {
        this.mParentConnection.onDisconnectConferenceParticipant(this.mEndpoint);
    }

    public Uri getEndpoint() {
        return this.mEndpoint;
    }

    private void setCapabilities() {
        setConnectionCapabilities(8192);
    }

    public String toString() {
        return "[ConferenceParticipantConnection objId:" + System.identityHashCode(this) + " endPoint:" + Log.pii(this.mEndpoint) + " parentConnection:" + Log.pii(this.mParentConnection.getAddress()) + " state:" + Connection.stateToString(getState()) + "]";
    }
}
