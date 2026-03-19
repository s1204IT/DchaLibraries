package com.android.services.telephony;

import android.telecom.Conference;
import android.telecom.Connection;
import android.telecom.PhoneAccountHandle;
import java.util.ArrayList;
import java.util.Collection;

public interface TelephonyConnectionServiceProxy {
    void addConference(ImsConference imsConference);

    void addConference(TelephonyConference telephonyConference);

    void addConnectionToConferenceController(TelephonyConnection telephonyConnection);

    void addExistingConnection(PhoneAccountHandle phoneAccountHandle, Connection connection);

    void addExistingConnection(PhoneAccountHandle phoneAccountHandle, Connection connection, Conference conference);

    Collection<Connection> getAllConnections();

    void performImsConferenceSRVCC(Conference conference, ArrayList<com.android.internal.telephony.Connection> arrayList, String str);

    void removeConnection(Connection connection);
}
