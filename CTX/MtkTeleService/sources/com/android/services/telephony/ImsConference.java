package com.android.services.telephony;

import android.content.Context;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.telecom.Conference;
import android.telecom.ConferenceParticipant;
import android.telecom.Connection;
import android.telecom.DisconnectCause;
import android.telecom.PhoneAccountHandle;
import android.telecom.StatusHints;
import android.telephony.PhoneNumberUtils;
import android.util.Pair;
import android.widget.Toast;
import com.android.internal.telephony.Call;
import com.android.internal.telephony.CallStateException;
import com.android.internal.telephony.Phone;
import com.android.phone.PhoneGlobals;
import com.android.phone.PhoneUtils;
import com.android.phone.R;
import com.android.services.telephony.TelephonyConnection;
import com.mediatek.internal.telephony.imsphone.MtkImsPhone;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class ImsConference extends Conference implements Holdable {
    public static final int IMS_CONFERENCE_MAX_SIZE = 5;
    private TelephonyConnection mConferenceHost;
    private Uri[] mConferenceHostAddress;
    private final Connection.Listener mConferenceHostListener;
    private PhoneAccountHandle mConferenceHostPhoneAccountHandle;
    private final HashMap<Pair<Uri, Uri>, ConferenceParticipantConnection> mConferenceParticipantConnections;
    private int mHostCallState;
    private boolean mIsDuringAddingParticipants;
    private boolean mIsHoldable;
    private final Connection.Listener mParticipantListener;
    private TelecomAccountRegistry mTelecomAccountRegistry;
    private final TelephonyConnection.TelephonyConnectionListener mTelephonyConnectionListener;
    private TelephonyConnectionServiceProxy mTelephonyConnectionService;
    private final Object mUpdateSyncRoot;

    public void updateConferenceParticipantsAfterCreation() {
        if (this.mConferenceHost != null) {
            android.telecom.Log.v(this, "updateConferenceStateAfterCreation :: process participant update", new Object[0]);
            handleConferenceParticipantsUpdate(this.mConferenceHost, this.mConferenceHost.getConferenceParticipants());
        } else {
            android.telecom.Log.v(this, "updateConferenceStateAfterCreation :: null mConferenceHost", new Object[0]);
        }
    }

    public ImsConference(TelecomAccountRegistry telecomAccountRegistry, TelephonyConnectionServiceProxy telephonyConnectionServiceProxy, TelephonyConnection telephonyConnection, PhoneAccountHandle phoneAccountHandle) {
        long connectTimeReal;
        super(phoneAccountHandle);
        this.mParticipantListener = new Connection.Listener() {
            public void onDestroyed(Connection connection) {
                ImsConference.this.removeConferenceParticipant((ConferenceParticipantConnection) connection);
                ImsConference.this.updateManageConference();
            }
        };
        this.mTelephonyConnectionListener = new TelephonyConnection.TelephonyConnectionListener() {
            @Override
            public void onOriginalConnectionConfigured(TelephonyConnection telephonyConnection2) {
                if (telephonyConnection2 == ImsConference.this.mConferenceHost) {
                    ImsConference.this.handleOriginalConnectionChange();
                }
            }

            @Override
            public void onConferenceParticipantsInvited(boolean z) {
                ImsConference.this.mIsDuringAddingParticipants = false;
            }

            @Override
            public void onConferenceConnectionsConfigured(ArrayList<com.android.internal.telephony.Connection> arrayList) {
                ImsConference.this.handleConferenceSRVCC(arrayList);
            }
        };
        this.mConferenceHostListener = new Connection.Listener() {
            public void onStateChanged(Connection connection, int i) {
                ImsConference.this.setState(i);
            }

            public void onDisconnected(Connection connection, DisconnectCause disconnectCause) {
                ImsConference.this.setDisconnected(disconnectCause);
            }

            public void onConferenceParticipantsChanged(Connection connection, List<ConferenceParticipant> list) {
                if (connection == null || list == null) {
                    return;
                }
                android.telecom.Log.v(this, "onConferenceParticipantsChanged: %d participants", new Object[]{Integer.valueOf(list.size())});
                ImsConference.this.handleConferenceParticipantsUpdate((TelephonyConnection) connection, list);
            }

            public void onVideoStateChanged(Connection connection, int i) {
                android.telecom.Log.d(this, "onVideoStateChanged video state %d", new Object[]{Integer.valueOf(i)});
                ImsConference.this.setVideoState(connection, i);
            }

            public void onVideoProviderChanged(Connection connection, Connection.VideoProvider videoProvider) {
                android.telecom.Log.d(this, "onVideoProviderChanged: Connection: %s, VideoProvider: %s", new Object[]{connection, videoProvider});
                ImsConference.this.setVideoProvider(connection, videoProvider);
            }

            public void onConnectionCapabilitiesChanged(Connection connection, int i) {
                android.telecom.Log.d(this, "onConnectionCapabilitiesChanged: Connection: %s, connectionCapabilities: %s", new Object[]{connection, Integer.valueOf(i)});
                ImsConference.this.setConnectionCapabilities(ImsConference.this.applyHostCapabilities(ImsConference.this.getConnectionCapabilities(), i, ImsConference.this.mConferenceHost != null ? ImsConference.this.mConferenceHost.isCarrierVideoConferencingSupported() : false));
            }

            public void onConnectionPropertiesChanged(Connection connection, int i) {
                android.telecom.Log.d(this, "onConnectionPropertiesChanged: Connection: %s, connectionProperties: %s", new Object[]{connection, Integer.valueOf(i)});
                ImsConference.this.setConnectionProperties(ImsConference.this.applyHostProperties(ImsConference.this.getConnectionProperties(), i));
            }

            public void onStatusHintsChanged(Connection connection, StatusHints statusHints) {
                android.telecom.Log.v(this, "onStatusHintsChanged", new Object[0]);
                ImsConference.this.updateStatusHints();
            }

            public void onExtrasChanged(Connection connection, Bundle bundle) {
                android.telecom.Log.v(this, "onExtrasChanged: c=" + connection + " Extras=" + bundle, new Object[0]);
                ImsConference.this.putExtras(bundle);
            }

            public void onExtrasRemoved(Connection connection, List<String> list) {
                android.telecom.Log.v(this, "onExtrasRemoved: c=" + connection + " key=" + list, new Object[0]);
                ImsConference.this.removeExtras(list);
            }
        };
        this.mConferenceParticipantConnections = new HashMap<>();
        this.mUpdateSyncRoot = new Object();
        this.mHostCallState = 1;
        this.mIsDuringAddingParticipants = false;
        this.mTelecomAccountRegistry = telecomAccountRegistry;
        long connectTime = 0;
        if (telephonyConnection.getOriginalConnection() != null) {
            connectTime = telephonyConnection.getOriginalConnection().getConnectTime();
            connectTimeReal = telephonyConnection.getOriginalConnection().getConnectTimeReal();
        } else {
            connectTimeReal = 0;
        }
        setConnectionTime(connectTime);
        setConnectionStartElapsedRealTime(connectTimeReal);
        telephonyConnection.setConnectTimeMillis(connectTime);
        telephonyConnection.setConnectionStartElapsedRealTime(connectTimeReal);
        this.mTelephonyConnectionService = telephonyConnectionServiceProxy;
        setConferenceHost(telephonyConnection);
        int i = 2097216;
        if (canHoldImsCalls()) {
            i = 2097219;
            this.mIsHoldable = true;
        }
        if (telephonyConnection != null && telephonyConnection.getOriginalConnection() != null && telephonyConnection.getOriginalConnection().getCall().getPhone() != null) {
            MtkImsPhone phone = telephonyConnection.getOriginalConnection().getCall().getPhone();
            if ((phone instanceof MtkImsPhone) && phone.isFeatureSupported(MtkImsPhone.FeatureType.VOLTE_ENHANCED_CONFERENCE)) {
                i |= 268435456;
            }
        }
        setConnectionCapabilities(applyHostCapabilities(i, this.mConferenceHost.getConnectionCapabilities(), this.mConferenceHost.isCarrierVideoConferencingSupported()));
        int iApplyHostProperties = applyHostProperties(32768, this.mConferenceHost.getConnectionProperties());
        android.telecom.Log.d(this, "MtkImsConference: properties=" + iApplyHostProperties, new Object[0]);
        setConnectionProperties(iApplyHostProperties);
    }

    private int applyHostCapabilities(int i, int i2, boolean z) {
        int iChangeBitmask;
        int iChangeBitmask2 = changeBitmask(i, 768, can(i2, 768));
        boolean z2 = false;
        if (z) {
            iChangeBitmask = changeBitmask(changeBitmask(iChangeBitmask2, 3072, can(i2, 3072)), 524288, can(i2, 524288));
        } else {
            android.telecom.Log.v(this, "applyHostCapabilities : video conferencing not supported", new Object[0]);
            iChangeBitmask = changeBitmask(changeBitmask(iChangeBitmask2, 3072, false), 524288, false);
        }
        int iChangeBitmask3 = changeBitmask(iChangeBitmask, 8388608, can(i2, 8388608));
        if (this.mConferenceHost.getVideoPauseSupported() && isVideoCapable()) {
            z2 = true;
        }
        return changeBitmask(changeBitmask(iChangeBitmask3, 1048576, z2), 1, can(i2, 1));
    }

    private int applyHostProperties(int i, int i2) {
        return changeBitmask(changeBitmask(changeBitmask(changeBitmask(i, 4, can(i2, 4)), 8, can(i2, 8)), 16, can(i2, 16)), 32768, can(i2, 32768));
    }

    public Connection getPrimaryConnection() {
        return null;
    }

    @Override
    public Connection.VideoProvider getVideoProvider() {
        if (this.mConferenceHost != null) {
            return this.mConferenceHost.getVideoProvider();
        }
        return null;
    }

    @Override
    public int getVideoState() {
        if (this.mConferenceHost != null) {
            return this.mConferenceHost.getVideoState();
        }
        return 0;
    }

    @Override
    public void onDisconnect() {
        Call call;
        android.telecom.Log.v(this, "onDisconnect: hanging up conference host.", new Object[0]);
        if (this.mConferenceHost != null && (call = this.mConferenceHost.getCall()) != null) {
            try {
                call.hangup();
            } catch (CallStateException e) {
                android.telecom.Log.e(this, e, "Exception thrown trying to hangup conference", new Object[0]);
            }
        }
    }

    @Override
    public void onSeparate(Connection connection) {
        android.telecom.Log.wtf(this, "Cannot separate connections from an IMS conference.", new Object[0]);
    }

    @Override
    public void onMerge(Connection connection) {
        android.telecom.Log.v(this, "onMerge()", new Object[0]);
        if (this.mConferenceHost == null) {
            android.telecom.Log.w(this, "mConferenceHost is null!", new Object[0]);
            return;
        }
        if (this.mIsDuringAddingParticipants) {
            toastWhenIsAddingParticipants();
            return;
        }
        if (getNumberOfParticipants() >= getMaximumConferenceSize()) {
            android.telecom.Log.v(this, "Conference is full!", new Object[0]);
            if (this.mConferenceHost.getPhone() != null && isShowToastWhenConferenceFull()) {
                toastWhenConferenceIsFull(this.mConferenceHost.getPhone().getContext());
            }
            if (ignoreAddRequestToFullConference()) {
                android.telecom.Log.v(this, "Ignore the add request", new Object[0]);
                return;
            }
        }
        try {
            Phone phone = this.mConferenceHost.getPhone();
            if (phone != null) {
                phone.conference();
            }
        } catch (CallStateException e) {
            android.telecom.Log.e(this, e, "Exception thrown trying to merge call into a conference", new Object[0]);
        }
    }

    @Override
    public void onHold() {
        if (this.mIsDuringAddingParticipants) {
            toastWhenIsAddingParticipants();
        } else {
            if (this.mConferenceHost == null) {
                return;
            }
            this.mConferenceHost.performHold();
        }
    }

    @Override
    public void onUnhold() {
        if (this.mIsDuringAddingParticipants) {
            toastWhenIsAddingParticipants();
        } else {
            if (this.mConferenceHost == null) {
                return;
            }
            this.mConferenceHost.performUnhold();
        }
    }

    @Override
    public void onPlayDtmfTone(char c) {
        if (this.mConferenceHost == null) {
            return;
        }
        this.mConferenceHost.onPlayDtmfTone(c);
    }

    @Override
    public void onStopDtmfTone() {
        if (this.mConferenceHost == null) {
            return;
        }
        this.mConferenceHost.onStopDtmfTone();
    }

    @Override
    public void onConnectionAdded(Connection connection) {
    }

    @Override
    public void setHoldable(boolean z) {
        this.mIsHoldable = z;
        if (!this.mIsHoldable) {
            removeCapability(1);
        } else {
            addCapability(1);
        }
        this.mConferenceHost.setHoldable(this.mIsHoldable);
    }

    @Override
    public boolean isChildHoldable() {
        return false;
    }

    private int changeBitmask(int i, int i2, boolean z) {
        if (z) {
            return i | i2;
        }
        return i & (~i2);
    }

    public boolean isConferenceHost() {
        com.android.internal.telephony.Connection originalConnection;
        return this.mConferenceHost != null && (originalConnection = this.mConferenceHost.getOriginalConnection()) != null && originalConnection.isMultiparty() && originalConnection.isConferenceHost();
    }

    private void updateManageConference() {
        int i;
        boolean zCan = can(128);
        boolean z = !this.mConferenceParticipantConnections.isEmpty();
        Object[] objArr = new Object[2];
        objArr[0] = zCan ? "Y" : "N";
        objArr[1] = z ? "Y" : "N";
        android.telecom.Log.v(this, "updateManageConference was :%s is:%s", objArr);
        if (zCan != z) {
            int connectionCapabilities = getConnectionCapabilities();
            if (z) {
                i = (128 | connectionCapabilities) & (-2097153);
            } else {
                i = (connectionCapabilities & (-129)) | 2097152;
            }
            setConnectionCapabilities(i);
        }
    }

    private void setConferenceHost(TelephonyConnection telephonyConnection) {
        if (android.telecom.Log.VERBOSE) {
            android.telecom.Log.v(this, "setConferenceHost " + telephonyConnection, new Object[0]);
        }
        this.mConferenceHost = telephonyConnection;
        if (this.mConferenceHost.getPhone() != null && this.mConferenceHost.getPhone().getPhoneType() == 5) {
            Phone phone = this.mConferenceHost.getPhone();
            this.mConferenceHostPhoneAccountHandle = PhoneUtils.makePstnPhoneAccountHandle(phone.getDefaultPhone());
            ArrayList arrayList = new ArrayList();
            if (phone.getCurrentSubscriberUris() != null) {
                arrayList.addAll(new ArrayList(Arrays.asList(phone.getCurrentSubscriberUris())));
            }
            this.mConferenceHostAddress = new Uri[arrayList.size()];
            this.mConferenceHostAddress = (Uri[]) arrayList.toArray(this.mConferenceHostAddress);
            for (Uri uri : this.mConferenceHostAddress) {
                android.telecom.Log.d(this, "hostAddress: " + uri, new Object[0]);
            }
        }
        this.mConferenceHost.addConnectionListener(this.mConferenceHostListener);
        this.mConferenceHost.addTelephonyConnectionListener(this.mTelephonyConnectionListener);
        setConnectionCapabilities(applyHostCapabilities(getConnectionCapabilities(), this.mConferenceHost.getConnectionCapabilities(), this.mConferenceHost.isCarrierVideoConferencingSupported()));
        setConnectionProperties(applyHostProperties(getConnectionProperties(), this.mConferenceHost.getConnectionProperties()));
        setState(this.mConferenceHost.getState());
        updateStatusHints();
        if (telephonyConnection != null && telephonyConnection.getOriginalConnection() != null) {
            Bundle connectionExtras = telephonyConnection.getOriginalConnection().getConnectionExtras();
            android.telecom.Log.d(this, "set extras" + connectionExtras, new Object[0]);
            if (connectionExtras != null) {
                putExtras(connectionExtras);
            }
        }
    }

    private void handleConferenceParticipantsUpdate(TelephonyConnection telephonyConnection, List<ConferenceParticipant> list) {
        if (list == null) {
            return;
        }
        boolean z = false;
        if (telephonyConnection != null && !telephonyConnection.isManageImsConferenceCallSupported()) {
            android.telecom.Log.i(this, "handleConferenceParticipantsUpdate: manage conference is disallowed", new Object[0]);
            return;
        }
        android.telecom.Log.i(this, "handleConferenceParticipantsUpdate: size=%d", new Object[]{Integer.valueOf(list.size())});
        synchronized (this.mUpdateSyncRoot) {
            ArrayList<ConferenceParticipant> arrayList = new ArrayList(list.size());
            HashSet hashSet = new HashSet(list.size());
            boolean z2 = false;
            for (ConferenceParticipant conferenceParticipant : list) {
                Pair pair = new Pair(conferenceParticipant.getHandle(), conferenceParticipant.getEndpoint());
                hashSet.add(pair);
                if (!this.mConferenceParticipantConnections.containsKey(pair)) {
                    if (!isParticipantHost(this.mConferenceHostAddress, conferenceParticipant.getHandle())) {
                        createConferenceParticipantConnection(telephonyConnection, conferenceParticipant);
                        arrayList.add(conferenceParticipant);
                        z2 = true;
                    }
                } else {
                    ConferenceParticipantConnection conferenceParticipantConnection = this.mConferenceParticipantConnections.get(pair);
                    android.telecom.Log.i(this, "handleConferenceParticipantsUpdate: updateState, participant = %s", new Object[]{conferenceParticipant});
                    conferenceParticipantConnection.updateState(conferenceParticipant.getState());
                }
            }
            if (z2) {
                for (ConferenceParticipant conferenceParticipant2 : arrayList) {
                    this.mConferenceParticipantConnections.get(new Pair(conferenceParticipant2.getHandle(), conferenceParticipant2.getEndpoint())).updateState(conferenceParticipant2.getState());
                }
            }
            Iterator<Map.Entry<Pair<Uri, Uri>, ConferenceParticipantConnection>> it = this.mConferenceParticipantConnections.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<Pair<Uri, Uri>, ConferenceParticipantConnection> next = it.next();
                if (!hashSet.contains(next.getKey())) {
                    ConferenceParticipantConnection value = next.getValue();
                    value.setDisconnected(new DisconnectCause(4));
                    value.removeConnectionListener(this.mParticipantListener);
                    this.mTelephonyConnectionService.removeConnection(value);
                    removeConnection(value);
                    it.remove();
                    z = true;
                }
            }
            if (z2 || z) {
                updateManageConference();
            }
        }
    }

    private void createConferenceParticipantConnection(TelephonyConnection telephonyConnection, ConferenceParticipant conferenceParticipant) {
        ConferenceParticipantConnection conferenceParticipantConnection = new ConferenceParticipantConnection(telephonyConnection.getOriginalConnection(), conferenceParticipant);
        conferenceParticipantConnection.addConnectionListener(this.mParticipantListener);
        conferenceParticipantConnection.setConnectTimeMillis(telephonyConnection.getConnectTimeMillis());
        conferenceParticipantConnection.setConnectionStartElapsedRealTime(telephonyConnection.getConnectElapsedTimeMillis());
        android.telecom.Log.i(this, "createConferenceParticipantConnection: participant=%s, connection=%s", new Object[]{conferenceParticipant, conferenceParticipantConnection});
        synchronized (this.mUpdateSyncRoot) {
            this.mConferenceParticipantConnections.put(new Pair<>(conferenceParticipant.getHandle(), conferenceParticipant.getEndpoint()), conferenceParticipantConnection);
        }
        this.mTelephonyConnectionService.addExistingConnection(this.mConferenceHostPhoneAccountHandle, conferenceParticipantConnection, this);
        addConnection(conferenceParticipantConnection);
    }

    private void removeConferenceParticipant(ConferenceParticipantConnection conferenceParticipantConnection) {
        android.telecom.Log.i(this, "removeConferenceParticipant: %s", new Object[]{conferenceParticipantConnection});
        conferenceParticipantConnection.removeConnectionListener(this.mParticipantListener);
        synchronized (this.mUpdateSyncRoot) {
            this.mConferenceParticipantConnections.remove(conferenceParticipantConnection.getUserEntity());
        }
        this.mTelephonyConnectionService.removeConnection(conferenceParticipantConnection);
    }

    private void disconnectConferenceParticipants() {
        android.telecom.Log.v(this, "disconnectConferenceParticipants", new Object[0]);
        synchronized (this.mUpdateSyncRoot) {
            for (ConferenceParticipantConnection conferenceParticipantConnection : this.mConferenceParticipantConnections.values()) {
                conferenceParticipantConnection.removeConnectionListener(this.mParticipantListener);
                conferenceParticipantConnection.setDisconnected(new DisconnectCause(4));
                this.mTelephonyConnectionService.removeConnection(conferenceParticipantConnection);
                conferenceParticipantConnection.destroy();
            }
            this.mConferenceParticipantConnections.clear();
        }
    }

    private boolean isParticipantHost(Uri[] uriArr, Uri uri) {
        if (uriArr == null || uriArr.length == 0 || uri == null) {
            android.telecom.Log.v(this, "isParticipantHost(N) : host or participant uri null", new Object[0]);
            return false;
        }
        String[] strArrSplit = uri.getSchemeSpecificPart().split("[@;:]");
        if (strArrSplit.length == 0) {
            android.telecom.Log.v(this, "isParticipantHost(N) : no number in participant handle", new Object[0]);
            return false;
        }
        String str = strArrSplit[0];
        for (Uri uri2 : uriArr) {
            if (uri2 != null) {
                String schemeSpecificPart = uri2.getSchemeSpecificPart();
                boolean zCompare = PhoneNumberUtils.compare(schemeSpecificPart, str);
                Object[] objArr = new Object[3];
                objArr[0] = zCompare ? "Y" : "N";
                objArr[1] = android.telecom.Log.pii(schemeSpecificPart);
                objArr[2] = android.telecom.Log.pii(str);
                android.telecom.Log.v(this, "isParticipantHost(%s) : host: %s, participant %s", objArr);
                if (zCompare) {
                    return true;
                }
            }
        }
        return false;
    }

    private void handleOriginalConnectionChange() {
        PhoneAccountHandle phoneAccountHandleMakePstnPhoneAccountHandle;
        if (this.mConferenceHost == null) {
            android.telecom.Log.w(this, "handleOriginalConnectionChange; conference host missing.", new Object[0]);
            return;
        }
        com.android.internal.telephony.Connection originalConnection = this.mConferenceHost.getOriginalConnection();
        if (originalConnection != null && originalConnection.getPhoneType() != 5) {
            android.telecom.Log.i(this, "handleOriginalConnectionChange : handover from IMS connection to new connection: %s", new Object[]{originalConnection});
            if (this.mConferenceHost.getPhone() != null) {
                if (this.mConferenceHost.getPhone().getPhoneType() == 5) {
                    phoneAccountHandleMakePstnPhoneAccountHandle = PhoneUtils.makePstnPhoneAccountHandle(this.mConferenceHost.getPhone().getDefaultPhone());
                } else {
                    phoneAccountHandleMakePstnPhoneAccountHandle = PhoneUtils.makePstnPhoneAccountHandle(this.mConferenceHost.getPhone());
                }
            } else {
                phoneAccountHandleMakePstnPhoneAccountHandle = null;
            }
            if (this.mConferenceHost.getPhone().getPhoneType() == 1) {
                android.telecom.Log.i(this, "handleOriginalConnectionChange : SRVCC to GSM", new Object[0]);
                GsmConnection gsmConnection = new GsmConnection(originalConnection, getTelecomCallId(), this.mConferenceHost.isOutgoingCall());
                gsmConnection.setConferenceSupported(true);
                gsmConnection.setConnectionProperties(gsmConnection.getConnectionProperties() | 64);
                gsmConnection.updateState();
                gsmConnection.setConnectTimeMillis(this.mConferenceHost.getConnectTimeMillis());
                gsmConnection.setConnectionStartElapsedRealTime(this.mConferenceHost.getConnectElapsedTimeMillis());
                this.mTelephonyConnectionService.addExistingConnection(phoneAccountHandleMakePstnPhoneAccountHandle, gsmConnection);
                this.mTelephonyConnectionService.addConnectionToConferenceController(gsmConnection);
            }
            this.mConferenceHost.removeConnectionListener(this.mConferenceHostListener);
            this.mConferenceHost.removeTelephonyConnectionListener(this.mTelephonyConnectionListener);
            this.mConferenceHost = null;
            setDisconnected(new DisconnectCause(9));
            disconnectConferenceParticipants();
            destroy();
        }
        updateStatusHints();
    }

    public void setState(int i) {
        DisconnectCause disconnectCause;
        android.telecom.Log.v(this, "setState %s", new Object[]{Connection.stateToString(i)});
        switch (i) {
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
                if (this.mConferenceHost == null || this.mConferenceHost.getOriginalConnection() == null) {
                    disconnectCause = new DisconnectCause(4);
                } else {
                    disconnectCause = DisconnectCauseUtil.toTelecomDisconnectCause(this.mConferenceHost.getOriginalConnection().getDisconnectCause());
                }
                setDisconnected(disconnectCause);
                disconnectConferenceParticipants();
                destroy();
                break;
        }
    }

    private boolean isVideoCapable() {
        int connectionCapabilities = this.mConferenceHost.getConnectionCapabilities();
        return can(connectionCapabilities, 768) && can(connectionCapabilities, 3072);
    }

    private void updateStatusHints() {
        if (this.mConferenceHost == null) {
            setStatusHints(null);
            return;
        }
        if (this.mConferenceHost.isWifi()) {
            Phone phone = this.mConferenceHost.getPhone();
            if (phone != null) {
                Context context = phone.getContext();
                setStatusHints(new StatusHints(context.getString(R.string.status_hint_label_wifi_call), Icon.createWithResource(context.getResources(), R.drawable.ic_signal_wifi_4_bar_24dp), null));
                return;
            }
            return;
        }
        setStatusHints(null);
    }

    @Override
    public String toString() {
        return "[ImsConference objId:" + System.identityHashCode(this) + " telecomCallID:" + getTelecomCallId() + " state:" + Connection.stateToString(getState()) + " hostConnection:" + this.mConferenceHost + " participants:" + this.mConferenceParticipantConnections.size() + "]";
    }

    private boolean canHoldImsCalls() {
        PersistableBundle carrierConfig = getCarrierConfig();
        return carrierConfig == null || carrierConfig.getBoolean("allow_hold_in_ims_call");
    }

    private PersistableBundle getCarrierConfig() {
        Phone phone;
        if (this.mConferenceHost == null || (phone = this.mConferenceHost.getPhone()) == null) {
            return null;
        }
        return PhoneGlobals.getInstance().getCarrierConfigForSubId(phone.getSubId());
    }

    public boolean isMaximumConferenceSizeEnforced() {
        PersistableBundle carrierConfig = getCarrierConfig();
        return carrierConfig != null && carrierConfig.getBoolean("is_ims_conference_size_enforced_bool");
    }

    public int getMaximumConferenceSize() {
        PersistableBundle carrierConfig = getCarrierConfig();
        if (carrierConfig == null) {
            android.telecom.Log.w(this, "getMaximumConferenceSize - failed to get conference size", new Object[0]);
            return 5;
        }
        return carrierConfig.getInt("ims_conference_size_limit_int");
    }

    public int getNumberOfParticipants() {
        return this.mConferenceParticipantConnections.size();
    }

    public boolean isFullConference() {
        return isMaximumConferenceSizeEnforced() && getNumberOfParticipants() >= getMaximumConferenceSize();
    }

    public void onInviteConferenceParticipants(List<String> list) {
        android.telecom.Log.v(this, "onInviteConferenceParticipants()", new Object[0]);
        if (this.mConferenceHost == null) {
            android.telecom.Log.w(this, "mConferenceHost is null!", new Object[0]);
            return;
        }
        Iterator<String> it = list.iterator();
        while (it.hasNext()) {
            if (hasExistedInConference(it.next())) {
                it.remove();
            }
        }
        if (list.size() == 0) {
            return;
        }
        if (getNumberOfParticipants() >= getMaximumConferenceSize()) {
            android.telecom.Log.v(this, "Conference is full!", new Object[0]);
            if (this.mConferenceHost.getPhone() != null && isShowToastWhenConferenceFull()) {
                toastWhenConferenceIsFull(this.mConferenceHost.getPhone().getContext());
            }
            if (ignoreAddRequestToFullConference()) {
                android.telecom.Log.v(this, "Ignore the add request", new Object[0]);
                return;
            }
        }
        this.mConferenceHost.performInviteConferenceParticipants(list);
        this.mIsDuringAddingParticipants = true;
    }

    @Override
    protected final void setRinging() {
        setState(2);
    }

    private boolean hasExistedInConference(String str) {
        Iterator<Map.Entry<Pair<Uri, Uri>, ConferenceParticipantConnection>> it = this.mConferenceParticipantConnections.entrySet().iterator();
        while (it.hasNext()) {
            String schemeSpecificPart = ((Uri) it.next().getKey().first).getSchemeSpecificPart();
            android.telecom.Log.w(this, "The invited number is %s and participant number is %s", new Object[]{str, schemeSpecificPart});
            if (PhoneNumberUtils.compare(str, schemeSpecificPart)) {
                android.telecom.Log.v(this, "The invited number has already existed in the conference", new Object[0]);
                return true;
            }
        }
        return false;
    }

    private void toastWhenIsAddingParticipants() {
        Phone phone;
        if (this.mConferenceHost != null && (phone = this.mConferenceHost.getPhone()) != null) {
            Context context = phone.getContext();
            Toast.makeText(context, context.getString(R.string.volte_is_adding_participants), 0).show();
        }
    }

    static void toastWhenConferenceIsFull(Context context) {
        if (context == null) {
            return;
        }
        Toast.makeText(context, context.getString(R.string.volte_conf_member_reach_max), 0).show();
    }

    private void handleConferenceSRVCC(ArrayList<com.android.internal.telephony.Connection> arrayList) {
        android.telecom.Log.w(this, "handleConferenceSRVCC", new Object[0]);
        if (this.mConferenceHost == null) {
            android.telecom.Log.w(this, "onConferenceConnectionsConfigured: conference host missing.", new Object[0]);
            return;
        }
        if (arrayList == null || arrayList.size() < 2) {
            android.telecom.Log.w(this, "onConferenceConnectionsConfigured: failed at radioConnections.", new Object[0]);
            return;
        }
        disconnectConferenceParticipants();
        if (this.mTelephonyConnectionService instanceof TelephonyConnectionServiceProxy) {
            this.mTelephonyConnectionService.performImsConferenceSRVCC(this, arrayList, getTelecomCallId());
        }
        this.mConferenceHost.removeConnectionListener(this.mConferenceHostListener);
        this.mConferenceHost.removeTelephonyConnectionListener(this.mTelephonyConnectionListener);
        this.mConferenceHost = null;
        destroy();
    }

    public void onHangupAll() {
        android.telecom.Log.w(this, "onHangupAll()", new Object[0]);
        if (this.mConferenceHost == null) {
            return;
        }
        try {
            MtkImsPhone phone = this.mConferenceHost.getPhone();
            if (phone != null && (phone instanceof MtkImsPhone)) {
                phone.hangupAll();
            } else {
                android.telecom.Log.w(this, "Attempting to hangupAll a conference without backing phone.", new Object[0]);
            }
        } catch (CallStateException e) {
            android.telecom.Log.e(this, e, "Call to phone.hangupAll() failed with exception", new Object[0]);
        }
    }

    private boolean isShowToastWhenConferenceFull() {
        boolean z;
        PersistableBundle carrierConfig = getCarrierConfig();
        if (carrierConfig != null) {
            z = carrierConfig.getBoolean("show_toast_when_conference_full");
        } else {
            z = true;
        }
        android.telecom.Log.v(this, "showToast: %s", new Object[]{Boolean.valueOf(z)});
        return z;
    }

    private boolean ignoreAddRequestToFullConference() {
        PersistableBundle carrierConfig = getCarrierConfig();
        if (carrierConfig != null) {
            return carrierConfig.getBoolean("no_merge_req_after_max_connection");
        }
        return true;
    }

    public TelephonyConnection getHostConnection() {
        return this.mConferenceHost;
    }
}
