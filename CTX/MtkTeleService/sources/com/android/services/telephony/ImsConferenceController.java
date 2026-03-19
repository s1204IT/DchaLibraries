package com.android.services.telephony;

import android.telecom.Conference;
import android.telecom.Conferenceable;
import android.telecom.Connection;
import android.telecom.DisconnectCause;
import android.telecom.PhoneAccountHandle;
import com.android.phone.PhoneUtils;
import com.mediatek.phone.ext.ExtensionManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class ImsConferenceController {
    private final TelephonyConnectionServiceProxy mConnectionService;
    private TelecomAccountRegistry mTelecomAccountRegistry;
    private final Conference.Listener mConferenceListener = new Conference.Listener() {
        public void onDestroyed(Conference conference) {
            if (Log.VERBOSE) {
                Log.v(ImsConferenceController.class, "onDestroyed: %s", conference);
            }
            ImsConferenceController.this.mImsConferences.remove(conference);
        }

        public void onStateChanged(Conference conference, int i, int i2) {
            Log.d(this, "onStateChanged: %d -> %d", Integer.valueOf(i), Integer.valueOf(i2));
            ImsConferenceController.this.recalculate();
        }
    };
    private final Connection.Listener mConnectionListener = new Connection.Listener() {
        public void onStateChanged(Connection connection, int i) {
            Log.v(this, "onStateChanged: %s", Log.pii(connection.getAddress()));
            ImsConferenceController.this.recalculate();
        }

        public void onDisconnected(Connection connection, DisconnectCause disconnectCause) {
            Log.v(this, "onDisconnected: %s", Log.pii(connection.getAddress()));
            ImsConferenceController.this.recalculate();
        }

        public void onDestroyed(Connection connection) {
            ImsConferenceController.this.remove(connection);
        }

        public void onConferenceStarted() {
            Log.v(this, "onConferenceStarted", new Object[0]);
            ImsConferenceController.this.recalculate();
        }

        public void onConferenceSupportedChanged(Connection connection, boolean z) {
            Log.v(this, "onConferenceSupportedChanged", new Object[0]);
            ImsConferenceController.this.recalculate();
        }
    };
    private final ArrayList<TelephonyConnection> mTelephonyConnections = new ArrayList<>();
    private final ArrayList<ImsConference> mImsConferences = new ArrayList<>(1);

    public ImsConferenceController(TelecomAccountRegistry telecomAccountRegistry, TelephonyConnectionServiceProxy telephonyConnectionServiceProxy) {
        this.mConnectionService = telephonyConnectionServiceProxy;
        this.mTelecomAccountRegistry = telecomAccountRegistry;
    }

    void add(TelephonyConnection telephonyConnection) {
        if ((telephonyConnection.getConnectionProperties() & 16) == 16) {
            return;
        }
        if (this.mTelephonyConnections.contains(telephonyConnection)) {
            Log.w(this, "add - connection already tracked; connection=%s", telephonyConnection);
            return;
        }
        if (Log.VERBOSE) {
            Log.v(this, "add connection %s", telephonyConnection);
        }
        this.mTelephonyConnections.add(telephonyConnection);
        telephonyConnection.addConnectionListener(this.mConnectionListener);
        recalculateConference();
    }

    void remove(Connection connection) {
        if ((connection.getConnectionProperties() & 16) == 16) {
            return;
        }
        if (!this.mTelephonyConnections.contains(connection)) {
            Log.d(this, "remove - connection not tracked; connection=%s", connection);
            return;
        }
        if (Log.VERBOSE) {
            Log.v(this, "remove connection: %s", connection);
        }
        connection.removeConnectionListener(this.mConnectionListener);
        this.mTelephonyConnections.remove(connection);
        recalculateConferenceable();
    }

    private void recalculate() {
        recalculateConferenceable();
        recalculateConference();
    }

    private void recalculateConferenceable() {
        Log.v(this, "recalculateConferenceable : %d", Integer.valueOf(this.mTelephonyConnections.size()));
        if (this.mImsConferences.size() > 0) {
            if (!ExtensionManager.getDigitsUtilExt().areConnectionsInSameLine(this.mTelephonyConnections, this.mImsConferences.get(0))) {
                return;
            }
        } else if (!ExtensionManager.getDigitsUtilExt().areConnectionsInSameLine(this.mTelephonyConnections, null)) {
            return;
        }
        HashSet<Conferenceable> hashSet = new HashSet(this.mTelephonyConnections.size() + this.mImsConferences.size());
        HashSet hashSet2 = new HashSet();
        for (TelephonyConnection telephonyConnection : this.mTelephonyConnections) {
            if (Log.DEBUG) {
                Log.d(this, "recalc - %s %s supportsConf? %s", Integer.valueOf(telephonyConnection.getState()), telephonyConnection, Boolean.valueOf(telephonyConnection.isConferenceSupported()));
            }
            if (isMemberOfPeerConference(telephonyConnection)) {
                if (Log.VERBOSE) {
                    Log.v(this, "Skipping connection in peer conference: %s", telephonyConnection);
                }
            } else if (!telephonyConnection.isConferenceSupported()) {
                telephonyConnection.setConferenceables(Collections.emptyList());
            } else if (!ExtensionManager.getRttUtilExt().isRttCallAndNotAllowMerge(telephonyConnection.getOriginalConnection())) {
                switch (telephonyConnection.getState()) {
                    case 4:
                    case 5:
                        hashSet.add(telephonyConnection);
                        break;
                    default:
                        telephonyConnection.setConferenceables(Collections.emptyList());
                        break;
                }
            }
        }
        for (ImsConference imsConference : this.mImsConferences) {
            if (Log.DEBUG) {
                Log.d(this, "recalc - %s %s", Integer.valueOf(imsConference.getState()), imsConference);
            }
            if (!imsConference.isConferenceHost()) {
                if (Log.VERBOSE) {
                    Log.v(this, "skipping conference (not hosted on this device): %s", imsConference);
                }
            } else {
                switch (imsConference.getState()) {
                    case 4:
                    case 5:
                        if (!imsConference.isFullConference()) {
                            hashSet2.addAll(imsConference.getConnections());
                            hashSet.add(imsConference);
                        }
                        break;
                }
            }
        }
        Log.v(this, "conferenceableSet size: " + hashSet.size(), new Object[0]);
        for (final Conferenceable conferenceable : hashSet) {
            if (conferenceable instanceof Connection) {
                List<Conferenceable> list = (List) hashSet.stream().filter(new Predicate() {
                    @Override
                    public final boolean test(Object obj) {
                        return ImsConferenceController.lambda$recalculateConferenceable$0(conferenceable, (Conferenceable) obj);
                    }
                }).collect(Collectors.toList());
                list.addAll(hashSet2);
                ((Connection) conferenceable).setConferenceables(list);
            } else if (conferenceable instanceof ImsConference) {
                ImsConference imsConference2 = (ImsConference) conferenceable;
                if (imsConference2.isFullConference()) {
                    imsConference2.setConferenceableConnections(Collections.emptyList());
                }
                imsConference2.setConferenceableConnections((List) hashSet.stream().filter(new Predicate() {
                    @Override
                    public final boolean test(Object obj) {
                        return ImsConferenceController.lambda$recalculateConferenceable$1((Conferenceable) obj);
                    }
                }).map(new Function() {
                    @Override
                    public final Object apply(Object obj) {
                        return ImsConferenceController.lambda$recalculateConferenceable$2((Conferenceable) obj);
                    }
                }).collect(Collectors.toList()));
            }
        }
    }

    static boolean lambda$recalculateConferenceable$0(Conferenceable conferenceable, Conferenceable conferenceable2) {
        return conferenceable != conferenceable2;
    }

    static boolean lambda$recalculateConferenceable$1(Conferenceable conferenceable) {
        return conferenceable instanceof Connection;
    }

    static Connection lambda$recalculateConferenceable$2(Conferenceable conferenceable) {
        return (Connection) conferenceable;
    }

    private boolean isMemberOfPeerConference(Connection connection) {
        com.android.internal.telephony.Connection originalConnection;
        return (connection instanceof TelephonyConnection) && (originalConnection = ((TelephonyConnection) connection).getOriginalConnection()) != null && originalConnection.isMultiparty() && originalConnection.isMemberOfPeerConference();
    }

    private void recalculateConference() {
        Log.v(this, "recalculateConference", new Object[0]);
        Iterator<TelephonyConnection> it = this.mTelephonyConnections.iterator();
        while (it.hasNext()) {
            TelephonyConnection next = it.next();
            if (next.isImsConnection() && next.getOriginalConnection() != null && next.getOriginalConnection().isMultiparty()) {
                startConference(next);
                it.remove();
            }
        }
    }

    private void startConference(TelephonyConnection telephonyConnection) {
        PhoneAccountHandle handleByConnectionIfRequired;
        if (Log.VERBOSE) {
            Log.v(this, "Start new ImsConference - connection: %s", telephonyConnection);
        }
        TelephonyConnection telephonyConnectionCloneConnection = telephonyConnection.cloneConnection();
        telephonyConnectionCloneConnection.setVideoPauseSupported(telephonyConnection.getVideoPauseSupported());
        telephonyConnectionCloneConnection.setManageImsConferenceCallSupported(telephonyConnection.isManageImsConferenceCallSupported());
        ImsConference imsConference = null;
        if (telephonyConnection.getPhone() != null && telephonyConnection.getPhone().getPhoneType() == 5) {
            handleByConnectionIfRequired = ExtensionManager.getDigitsUtilExt().getHandleByConnectionIfRequired(PhoneUtils.makePstnPhoneAccountHandle(telephonyConnection.getPhone().getDefaultPhone()), telephonyConnection);
        } else {
            handleByConnectionIfRequired = null;
        }
        if (telephonyConnectionCloneConnection.getState() == 6) {
            Log.d(this, "Skip adding a disconnected conference call", new Object[0]);
        } else {
            imsConference = new ImsConference(this.mTelecomAccountRegistry, this.mConnectionService, telephonyConnectionCloneConnection, handleByConnectionIfRequired);
            imsConference.setState(telephonyConnectionCloneConnection.getState());
            imsConference.addListener(this.mConferenceListener);
            imsConference.updateConferenceParticipantsAfterCreation();
            this.mConnectionService.addConference(imsConference);
            telephonyConnectionCloneConnection.setTelecomCallId(imsConference.getTelecomCallId());
        }
        telephonyConnection.removeConnectionListener(this.mConnectionListener);
        telephonyConnection.clearOriginalConnection();
        telephonyConnection.setDisconnected(new DisconnectCause(9, android.telephony.DisconnectCause.toString(45)));
        telephonyConnection.destroy();
        if (telephonyConnectionCloneConnection.getState() != 6 && imsConference != null) {
            this.mImsConferences.add(imsConference);
        }
    }

    public ImsConference createConference(TelephonyConnection telephonyConnection) {
        PhoneAccountHandle phoneAccountHandleMakePstnPhoneAccountHandle;
        Log.d(this, "Create new ImsConference - connection: %s", telephonyConnection);
        if (telephonyConnection.getPhone() != null && telephonyConnection.getPhone().getPhoneType() == 5) {
            phoneAccountHandleMakePstnPhoneAccountHandle = PhoneUtils.makePstnPhoneAccountHandle(telephonyConnection.getPhone().getDefaultPhone());
        } else {
            phoneAccountHandleMakePstnPhoneAccountHandle = null;
        }
        ImsConference imsConference = new ImsConference(this.mTelecomAccountRegistry, this.mConnectionService, telephonyConnection, phoneAccountHandleMakePstnPhoneAccountHandle);
        imsConference.setConnectionCapabilities(imsConference.getConnectionCapabilities() & (-2));
        imsConference.setState(telephonyConnection.getState());
        imsConference.addListener(this.mConferenceListener);
        this.mImsConferences.add(imsConference);
        return imsConference;
    }

    public boolean isConferenceExist() {
        Log.d(this, "isConferenceExist: %d", Integer.valueOf(this.mImsConferences.size()));
        return this.mImsConferences.size() > 0;
    }
}
