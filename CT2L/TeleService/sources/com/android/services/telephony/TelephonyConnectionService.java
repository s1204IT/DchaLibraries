package com.android.services.telephony;

import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import android.telecom.Connection;
import android.telecom.ConnectionRequest;
import android.telecom.ConnectionService;
import android.telecom.PhoneAccountHandle;
import android.telephony.PhoneNumberUtils;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import com.android.internal.telephony.Call;
import com.android.internal.telephony.CallStateException;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.PhoneProxy;
import com.android.internal.telephony.SubscriptionController;
import com.android.internal.telephony.cdma.CDMAPhone;
import com.android.phone.MMIDialogActivity;
import com.android.services.telephony.EmergencyCallHelper;
import com.android.services.telephony.TelephonyConnection;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

public class TelephonyConnectionService extends ConnectionService {
    private EmergencyCallHelper mEmergencyCallHelper;
    private EmergencyTonePlayer mEmergencyTonePlayer;
    private final TelephonyConferenceController mTelephonyConferenceController = new TelephonyConferenceController(this);
    private final CdmaConferenceController mCdmaConferenceController = new CdmaConferenceController(this);
    private final ImsConferenceController mImsConferenceController = new ImsConferenceController(this);
    private ComponentName mExpectedComponentName = null;
    private final TelephonyConnection.TelephonyConnectionListener mTelephonyConnectionListener = new TelephonyConnection.TelephonyConnectionListener() {
        @Override
        public void onOriginalConnectionConfigured(TelephonyConnection c) {
            TelephonyConnectionService.this.addConnectionToConferenceController(c);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        this.mExpectedComponentName = new ComponentName(this, getClass());
        this.mEmergencyTonePlayer = new EmergencyTonePlayer(this);
        TelecomAccountRegistry.getInstance(this).setTelephonyConnectionService(this);
    }

    @Override
    public Connection onCreateOutgoingConnection(PhoneAccountHandle connectionManagerPhoneAccount, final ConnectionRequest request) {
        String number;
        Log.i(this, "onCreateOutgoingConnection, request: " + request, new Object[0]);
        Uri handle = request.getAddress();
        if (handle == null) {
            Log.d(this, "onCreateOutgoingConnection, handle is null", new Object[0]);
            return Connection.createFailedConnection(DisconnectCauseUtil.toTelecomDisconnectCause(38, "No phone number supplied"));
        }
        String scheme = handle.getScheme();
        if ("voicemail".equals(scheme)) {
            Phone phone = getPhoneForAccount(request.getAccountHandle(), false);
            if (phone == null) {
                Log.d(this, "onCreateOutgoingConnection, phone is null", new Object[0]);
                return Connection.createFailedConnection(DisconnectCauseUtil.toTelecomDisconnectCause(18, "Phone is null"));
            }
            number = phone.getVoiceMailNumber();
            if (TextUtils.isEmpty(number)) {
                Log.d(this, "onCreateOutgoingConnection, no voicemail number set.", new Object[0]);
                return Connection.createFailedConnection(DisconnectCauseUtil.toTelecomDisconnectCause(40, "Voicemail scheme provided but no voicemail number set."));
            }
            handle = Uri.fromParts("tel", number, null);
        } else {
            if (!"tel".equals(scheme)) {
                Log.d(this, "onCreateOutgoingConnection, Handle %s is not type tel", scheme);
                return Connection.createFailedConnection(DisconnectCauseUtil.toTelecomDisconnectCause(7, "Handle scheme is not type tel"));
            }
            number = handle.getSchemeSpecificPart();
            if (TextUtils.isEmpty(number)) {
                Log.d(this, "onCreateOutgoingConnection, unable to parse number", new Object[0]);
                return Connection.createFailedConnection(DisconnectCauseUtil.toTelecomDisconnectCause(7, "Unable to parse number"));
            }
        }
        boolean isEmergencyNumber = PhoneNumberUtils.isPotentialEmergencyNumber(number);
        final Phone phone2 = getPhoneForAccount(request.getAccountHandle(), isEmergencyNumber);
        if (phone2 == null) {
            Log.d(this, "onCreateOutgoingConnection, phone is null", new Object[0]);
            return Connection.createFailedConnection(DisconnectCauseUtil.toTelecomDisconnectCause(18, "Phone is null"));
        }
        int state = phone2.getServiceState().getState();
        if (state == 1) {
            state = phone2.getServiceState().getDataRegState();
        }
        boolean useEmergencyCallHelper = false;
        if (isEmergencyNumber) {
            if (state == 3) {
                useEmergencyCallHelper = true;
            }
        } else {
            switch (state) {
                case 0:
                case 2:
                    break;
                case 1:
                    return Connection.createFailedConnection(DisconnectCauseUtil.toTelecomDisconnectCause(18, "ServiceState.STATE_OUT_OF_SERVICE"));
                case 3:
                    return Connection.createFailedConnection(DisconnectCauseUtil.toTelecomDisconnectCause(17, "ServiceState.STATE_POWER_OFF"));
                default:
                    Log.d(this, "onCreateOutgoingConnection, unknown service state: %d", Integer.valueOf(state));
                    return Connection.createFailedConnection(DisconnectCauseUtil.toTelecomDisconnectCause(43, "Unknown service state " + state));
            }
        }
        final TelephonyConnection connection = createConnectionFor(phone2, null, true);
        if (connection == null) {
            return Connection.createFailedConnection(DisconnectCauseUtil.toTelecomDisconnectCause(43, "Invalid phone type"));
        }
        connection.setAddress(handle, 1);
        connection.setInitializing();
        connection.setVideoState(request.getVideoState());
        if (useEmergencyCallHelper) {
            if (this.mEmergencyCallHelper == null) {
                this.mEmergencyCallHelper = new EmergencyCallHelper(this);
            }
            this.mEmergencyCallHelper.startTurnOnRadioSequence(phone2, new EmergencyCallHelper.Callback() {
                @Override
                public void onComplete(boolean isRadioReady) {
                    if (connection.getState() != 6) {
                        if (isRadioReady) {
                            connection.setInitialized();
                            TelephonyConnectionService.this.placeOutgoingConnection(connection, phone2, request);
                        } else {
                            Log.d(this, "onCreateOutgoingConnection, failed to turn on radio", new Object[0]);
                            connection.setDisconnected(DisconnectCauseUtil.toTelecomDisconnectCause(17, "Failed to turn on radio."));
                            connection.destroy();
                        }
                    }
                }
            });
            return connection;
        }
        placeOutgoingConnection(connection, phone2, request);
        return connection;
    }

    @Override
    public Connection onCreateIncomingConnection(PhoneAccountHandle connectionManagerPhoneAccount, ConnectionRequest request) {
        Log.i(this, "onCreateIncomingConnection, request: " + request, new Object[0]);
        PhoneAccountHandle accountHandle = request.getAccountHandle();
        boolean isEmergency = false;
        if (accountHandle != null && "E".equals(accountHandle.getId())) {
            Log.i(this, "Emergency PhoneAccountHandle is being used for incoming call... Treat as an Emergency Call.", new Object[0]);
            isEmergency = true;
        }
        Phone phone = getPhoneForAccount(accountHandle, isEmergency);
        if (phone == null) {
            return Connection.createFailedConnection(DisconnectCauseUtil.toTelecomDisconnectCause(36));
        }
        Call call = phone.getRingingCall();
        if (!call.getState().isRinging()) {
            Log.i(this, "onCreateIncomingConnection, no ringing call", new Object[0]);
            return Connection.createFailedConnection(DisconnectCauseUtil.toTelecomDisconnectCause(1, "Found no ringing call"));
        }
        com.android.internal.telephony.Connection originalConnection = call.getState() == Call.State.WAITING ? call.getLatestConnection() : call.getEarliestConnection();
        if (isOriginalConnectionKnown(originalConnection)) {
            Log.i(this, "onCreateIncomingConnection, original connection already registered", new Object[0]);
            return Connection.createCanceledConnection();
        }
        Connection connection = createConnectionFor(phone, originalConnection, false);
        if (connection == null) {
            Connection.createCanceledConnection();
            return Connection.createCanceledConnection();
        }
        return connection;
    }

    public Connection onCreateUnknownConnection(PhoneAccountHandle connectionManagerPhoneAccount, ConnectionRequest request) {
        Log.i(this, "onCreateUnknownConnection, request: " + request, new Object[0]);
        PhoneAccountHandle accountHandle = request.getAccountHandle();
        boolean isEmergency = false;
        if (accountHandle != null && "E".equals(accountHandle.getId())) {
            Log.i(this, "Emergency PhoneAccountHandle is being used for unknown call... Treat as an Emergency Call.", new Object[0]);
            isEmergency = true;
        }
        Phone phone = getPhoneForAccount(accountHandle, isEmergency);
        if (phone == null) {
            return Connection.createFailedConnection(DisconnectCauseUtil.toTelecomDisconnectCause(36));
        }
        List<com.android.internal.telephony.Connection> allConnections = new ArrayList<>();
        Call ringingCall = phone.getRingingCall();
        if (ringingCall.hasConnections()) {
            allConnections.addAll(ringingCall.getConnections());
        }
        Call foregroundCall = phone.getForegroundCall();
        if (foregroundCall.hasConnections()) {
            allConnections.addAll(foregroundCall.getConnections());
        }
        Call backgroundCall = phone.getBackgroundCall();
        if (backgroundCall.hasConnections()) {
            allConnections.addAll(phone.getBackgroundCall().getConnections());
        }
        com.android.internal.telephony.Connection unknownConnection = null;
        Iterator<com.android.internal.telephony.Connection> it = allConnections.iterator();
        while (true) {
            if (!it.hasNext()) {
                break;
            }
            com.android.internal.telephony.Connection telephonyConnection = it.next();
            if (!isOriginalConnectionKnown(telephonyConnection)) {
                unknownConnection = telephonyConnection;
                break;
            }
        }
        if (unknownConnection == null) {
            Log.i(this, "onCreateUnknownConnection, did not find previously unknown connection.", new Object[0]);
            return Connection.createCanceledConnection();
        }
        TelephonyConnection connection = createConnectionFor(phone, unknownConnection, !unknownConnection.isIncoming());
        if (connection == null) {
            return Connection.createCanceledConnection();
        }
        connection.updateState();
        return connection;
    }

    @Override
    public void onConference(Connection connection1, Connection connection2) {
        if ((connection1 instanceof TelephonyConnection) && (connection2 instanceof TelephonyConnection)) {
            ((TelephonyConnection) connection1).performConference((TelephonyConnection) connection2);
        }
    }

    private void placeOutgoingConnection(TelephonyConnection connection, Phone phone, ConnectionRequest request) {
        String number = connection.getAddress().getSchemeSpecificPart();
        try {
            com.android.internal.telephony.Connection originalConnection = phone.dial(number, request.getVideoState());
            if (originalConnection == null) {
                int telephonyDisconnectCause = 43;
                if (phone.getPhoneType() == 1) {
                    Log.d(this, "dialed MMI code", new Object[0]);
                    telephonyDisconnectCause = 39;
                    Intent intent = new Intent(this, (Class<?>) MMIDialogActivity.class);
                    intent.setFlags(276824064);
                    startActivity(intent);
                }
                Log.d(this, "placeOutgoingConnection, phone.dial returned null", new Object[0]);
                connection.setDisconnected(DisconnectCauseUtil.toTelecomDisconnectCause(telephonyDisconnectCause, "Connection is null"));
                return;
            }
            connection.setOriginalConnection(originalConnection);
        } catch (CallStateException e) {
            Log.e(this, e, "placeOutgoingConnection, phone.dial exception: " + e, new Object[0]);
            connection.setDisconnected(DisconnectCauseUtil.toTelecomDisconnectCause(43, e.getMessage()));
        }
    }

    private TelephonyConnection createConnectionFor(Phone phone, com.android.internal.telephony.Connection originalConnection, boolean isOutgoing) {
        TelephonyConnection returnConnection = null;
        int phoneType = phone.getPhoneType();
        if (phoneType == 1) {
            returnConnection = new GsmConnection(originalConnection);
        } else if (phoneType == 2) {
            boolean allowMute = allowMute(phone);
            returnConnection = new CdmaConnection(originalConnection, this.mEmergencyTonePlayer, allowMute, isOutgoing);
        }
        if (returnConnection != null) {
            returnConnection.addTelephonyConnectionListener(this.mTelephonyConnectionListener);
        }
        return returnConnection;
    }

    private boolean isOriginalConnectionKnown(com.android.internal.telephony.Connection originalConnection) {
        for (Connection connection : getAllConnections()) {
            if (connection instanceof TelephonyConnection) {
                TelephonyConnection telephonyConnection = (TelephonyConnection) connection;
                if (telephonyConnection.getOriginalConnection() == originalConnection) {
                    return true;
                }
            }
        }
        return false;
    }

    private Phone getPhoneForAccount(PhoneAccountHandle accountHandle, boolean isEmergency) {
        if (Objects.equals(this.mExpectedComponentName, accountHandle.getComponentName()) && accountHandle.getId() != null) {
            try {
                int phoneId = SubscriptionController.getInstance().getPhoneId(Integer.parseInt(accountHandle.getId()));
                return PhoneFactory.getPhone(phoneId);
            } catch (NumberFormatException e) {
                Log.w(this, "Could not get subId from account: " + accountHandle.getId(), new Object[0]);
                if (isEmergency) {
                    return PhoneFactory.getDefaultPhone();
                }
            }
        }
        if (isEmergency) {
            return getFirstPhoneForEmergencyCall();
        }
        return null;
    }

    private Phone getFirstPhoneForEmergencyCall() {
        Phone selectPhone = null;
        for (int i = 0; i < TelephonyManager.getDefault().getSimCount(); i++) {
            int[] subIds = SubscriptionController.getInstance().getSubIdUsingSlotId(i);
            if (subIds.length != 0) {
                int phoneId = SubscriptionController.getInstance().getPhoneId(subIds[0]);
                Phone phone = PhoneFactory.getPhone(phoneId);
                if (phone == null) {
                    continue;
                } else {
                    if (phone.getServiceState().getState() == 0) {
                        Log.d(this, "pickBestPhoneForEmergencyCall, radio on & in service, slotId:" + i, new Object[0]);
                        return phone;
                    }
                    if (3 != phone.getServiceState().getState()) {
                        if (TelephonyManager.getDefault().hasIccCard(i)) {
                            Log.d(this, "pickBestPhoneForEmergencyCall,radio on and SIM card inserted, slotId:" + i, new Object[0]);
                            selectPhone = phone;
                        } else if (selectPhone == null) {
                            Log.d(this, "pickBestPhoneForEmergencyCall, radio on, slotId:" + i, new Object[0]);
                            selectPhone = phone;
                        }
                    }
                }
            }
        }
        if (selectPhone == null) {
            Log.d(this, "pickBestPhoneForEmergencyCall, return default phone", new Object[0]);
            selectPhone = PhoneFactory.getDefaultPhone();
        }
        return selectPhone;
    }

    private boolean allowMute(Phone phone) {
        if (phone.getPhoneType() == 2) {
            PhoneProxy phoneProxy = (PhoneProxy) phone;
            CDMAPhone cdmaPhone = phoneProxy.getActivePhone();
            if (cdmaPhone != null && cdmaPhone.isInEcm()) {
                return false;
            }
        }
        return true;
    }

    public void removeConnection(Connection connection) {
        super.removeConnection(connection);
        if (connection instanceof TelephonyConnection) {
            TelephonyConnection telephonyConnection = (TelephonyConnection) connection;
            telephonyConnection.removeTelephonyConnectionListener(this.mTelephonyConnectionListener);
        }
    }

    public void addConnectionToConferenceController(TelephonyConnection connection) {
        if (connection.isImsConnection()) {
            Log.d(this, "Adding IMS connection to conference controller: " + connection, new Object[0]);
            this.mImsConferenceController.add(connection);
            return;
        }
        int phoneType = connection.getCall().getPhone().getPhoneType();
        if (phoneType == 1) {
            Log.d(this, "Adding GSM connection to conference controller: " + connection, new Object[0]);
            this.mTelephonyConferenceController.add(connection);
        } else if (phoneType == 2 && (connection instanceof CdmaConnection)) {
            Log.d(this, "Adding CDMA connection to conference controller: " + connection, new Object[0]);
            this.mCdmaConferenceController.add((CdmaConnection) connection);
        }
    }
}
