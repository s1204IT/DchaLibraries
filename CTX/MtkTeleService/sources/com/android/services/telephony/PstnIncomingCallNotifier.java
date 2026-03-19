package com.android.services.telephony;

import android.net.Uri;
import android.os.AsyncResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.text.TextUtils;
import com.android.internal.telephony.Call;
import com.android.internal.telephony.CallStateException;
import com.android.internal.telephony.Connection;
import com.android.internal.telephony.GsmCdmaPhone;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.cdma.CdmaCallWaitingNotification;
import com.android.internal.telephony.imsphone.ImsExternalConnection;
import com.android.internal.telephony.imsphone.ImsPhoneConnection;
import com.android.phone.PhoneUtils;
import com.google.common.base.Preconditions;
import com.mediatek.internal.telephony.imsphone.MtkImsPhoneConnection;
import com.mediatek.phone.ext.ExtensionManager;
import java.util.Objects;

public final class PstnIncomingCallNotifier {
    private static final int EVENT_CDMA_CALL_WAITING = 101;
    private static final int EVENT_NEW_RINGING_CONNECTION = 100;
    private static final int EVENT_UNKNOWN_CONNECTION = 102;
    private final Handler mHandler;
    private final Phone mPhone;
    private PhoneAccountHandle mPhoneAccountHandle;

    public PstnIncomingCallNotifier(Phone phone) {
        this.mPhoneAccountHandle = null;
        this.mHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                switch (message.what) {
                    case 100:
                        PstnIncomingCallNotifier.this.handleNewRingingConnection((AsyncResult) message.obj);
                        break;
                    case 101:
                        PstnIncomingCallNotifier.this.handleCdmaCallWaiting((AsyncResult) message.obj);
                        break;
                    case 102:
                        PstnIncomingCallNotifier.this.handleNewUnknownConnection((AsyncResult) message.obj);
                        break;
                }
            }
        };
        Preconditions.checkNotNull(phone);
        this.mPhone = phone;
        registerForNotifications();
    }

    public PstnIncomingCallNotifier(Phone phone, PhoneAccountHandle phoneAccountHandle) {
        this(phone);
        ExtensionManager.getDigitsUtilExt().setPhoneAccountHandle(this, phoneAccountHandle);
    }

    void teardown() {
        unregisterForNotifications();
    }

    private void registerForNotifications() {
        if (this.mPhone != null) {
            Log.i(this, "Registering: %s", this.mPhone);
            this.mPhone.registerForNewRingingConnection(this.mHandler, 100, (Object) null);
            this.mPhone.registerForCallWaiting(this.mHandler, 101, (Object) null);
            this.mPhone.registerForUnknownConnection(this.mHandler, 102, (Object) null);
        }
    }

    private void unregisterForNotifications() {
        if (this.mPhone != null) {
            Log.i(this, "Unregistering: %s", this.mPhone);
            this.mPhone.unregisterForNewRingingConnection(this.mHandler);
            this.mPhone.unregisterForCallWaiting(this.mHandler);
            this.mPhone.unregisterForUnknownConnection(this.mHandler);
        }
    }

    private void handleNewRingingConnection(AsyncResult asyncResult) {
        Call call;
        Log.d(this, "handleNewRingingConnection", new Object[0]);
        Connection connection = (Connection) asyncResult.result;
        if (connection == null || (call = connection.getCall()) == null || !call.getState().isRinging() || !ExtensionManager.getDigitsUtilExt().isConnectionMatched(connection, this.mPhoneAccountHandle, this.mPhone.getContext())) {
            return;
        }
        sendIncomingCallIntent(connection);
    }

    private void handleCdmaCallWaiting(AsyncResult asyncResult) {
        Connection latestConnection;
        Log.d(this, "handleCdmaCallWaiting", new Object[0]);
        CdmaCallWaitingNotification cdmaCallWaitingNotification = (CdmaCallWaitingNotification) asyncResult.result;
        Call ringingCall = this.mPhone.getRingingCall();
        if (ringingCall.getState() == Call.State.WAITING && (latestConnection = ringingCall.getLatestConnection()) != null) {
            String address = latestConnection.getAddress();
            int numberPresentation = latestConnection.getNumberPresentation();
            if (numberPresentation != 1 && numberPresentation == cdmaCallWaitingNotification.numberPresentation) {
                Log.i(this, "handleCdmaCallWaiting: inform telecom of waiting call; presentation = %d", Integer.valueOf(numberPresentation));
                sendIncomingCallIntent(latestConnection);
            } else if (!TextUtils.isEmpty(address) && Objects.equals(address, cdmaCallWaitingNotification.number)) {
                Log.i(this, "handleCdmaCallWaiting: inform telecom of waiting call; number = %s", Log.pii(address));
                sendIncomingCallIntent(latestConnection);
            } else {
                Log.w(this, "handleCdmaCallWaiting: presentation or number do not match, not informing telecom of call: %s", cdmaCallWaitingNotification);
            }
        }
    }

    private void handleNewUnknownConnection(AsyncResult asyncResult) {
        Log.i(this, "handleNewUnknownConnection", new Object[0]);
        if (!(asyncResult.result instanceof Connection)) {
            Log.w(this, "handleNewUnknownConnection called with non-Connection object", new Object[0]);
            return;
        }
        Connection connection = (Connection) asyncResult.result;
        if (connection != null) {
            Call.State state = connection.getState();
            if (state == Call.State.DISCONNECTED || state == Call.State.IDLE) {
                Log.i(this, "Skipping new unknown connection because it is idle. " + connection, new Object[0]);
                return;
            }
            Call call = connection.getCall();
            if (call == null || !call.getState().isAlive() || !ExtensionManager.getDigitsUtilExt().isConnectionMatched(connection, this.mPhoneAccountHandle, this.mPhone.getContext())) {
                return;
            }
            addNewUnknownCall(connection);
        }
    }

    private void addNewUnknownCall(Connection connection) {
        Log.i(this, "addNewUnknownCall, connection is: %s", connection);
        if (!maybeSwapAnyWithUnknownConnection(connection)) {
            Log.i(this, "determined new connection is: %s", connection);
            Bundle bundle = new Bundle();
            if (connection.getNumberPresentation() == 1 && !TextUtils.isEmpty(connection.getAddress())) {
                bundle.putParcelable("android.telecom.extra.UNKNOWN_CALL_HANDLE", Uri.fromParts("tel", connection.getAddress(), null));
            }
            if (connection instanceof ImsExternalConnection) {
                bundle.putInt("android.telephony.ImsExternalCallTracker.extra.EXTERNAL_CALL_ID", ((ImsExternalConnection) connection).getCallId());
            }
            bundle.putLong("android.telecom.extra.CALL_CREATED_TIME_MILLIS", SystemClock.elapsedRealtime());
            PhoneAccountHandle phoneAccountHandleFindCorrectPhoneAccountHandle = findCorrectPhoneAccountHandle();
            if (phoneAccountHandleFindCorrectPhoneAccountHandle == null) {
                try {
                    connection.hangup();
                    return;
                } catch (CallStateException e) {
                    return;
                }
            } else {
                TelecomManager.from(this.mPhone.getContext()).addNewUnknownCall(phoneAccountHandleFindCorrectPhoneAccountHandle, bundle);
                return;
            }
        }
        Log.i(this, "swapped an old connection, new one is: %s", connection);
    }

    private void sendIncomingCallIntent(Connection connection) {
        Bundle bundle = new Bundle();
        if (connection.getNumberPresentation() == 1 && !TextUtils.isEmpty(connection.getAddress())) {
            bundle.putParcelable("android.telecom.extra.INCOMING_CALL_ADDRESS", Uri.fromParts("tel", connection.getAddress(), null));
            if (connection instanceof MtkImsPhoneConnection) {
                boolean zIsIncomingCallMultiparty = ((MtkImsPhoneConnection) connection).isIncomingCallMultiparty();
                Log.d(this, "isIncomingMpty: " + zIsIncomingCallMultiparty, new Object[0]);
                bundle.putBoolean("mediatek.telecom.extra.EXTRA_INCOMING_VOLTE_CONFERENCE", zIsIncomingCallMultiparty);
            }
        }
        bundle.putLong("android.telecom.extra.CALL_CREATED_TIME_MILLIS", SystemClock.elapsedRealtime());
        if (connection.getPhoneType() == 5 && ((ImsPhoneConnection) connection).isRttEnabledForCall()) {
            bundle.putBoolean("android.telecom.extra.START_CALL_WITH_RTT", true);
        }
        PhoneAccountHandle phoneAccountHandleFindCorrectPhoneAccountHandle = findCorrectPhoneAccountHandle();
        if (phoneAccountHandleFindCorrectPhoneAccountHandle == null) {
            try {
                connection.hangup();
            } catch (CallStateException e) {
            }
        } else {
            TelecomManager.from(this.mPhone.getContext()).addNewIncomingCall(phoneAccountHandleFindCorrectPhoneAccountHandle, bundle);
        }
    }

    private PhoneAccountHandle findCorrectPhoneAccountHandle() {
        TelecomAccountRegistry telecomAccountRegistry = TelecomAccountRegistry.getInstance(null);
        PhoneAccountHandle correctPhoneAccountHandle = ExtensionManager.getDigitsUtilExt().getCorrectPhoneAccountHandle(PhoneUtils.makePstnPhoneAccountHandle(this.mPhone), this.mPhoneAccountHandle, this.mPhone.getContext());
        if (telecomAccountRegistry.hasAccountEntryForPhoneAccount(correctPhoneAccountHandle)) {
            return correctPhoneAccountHandle;
        }
        PhoneAccountHandle phoneAccountHandleMakePstnPhoneAccountHandleWithPrefix = PhoneUtils.makePstnPhoneAccountHandleWithPrefix(this.mPhone, "", true);
        if (telecomAccountRegistry.hasAccountEntryForPhoneAccount(phoneAccountHandleMakePstnPhoneAccountHandleWithPrefix)) {
            Log.i(this, "Receiving MT call in ECM. Using Emergency PhoneAccount Instead.", new Object[0]);
            return phoneAccountHandleMakePstnPhoneAccountHandleWithPrefix;
        }
        Log.w(this, "PhoneAccount not found.", new Object[0]);
        return null;
    }

    private boolean maybeSwapAnyWithUnknownConnection(Connection connection) {
        TelecomAccountRegistry telecomAccountRegistry;
        TelephonyConnectionService telephonyConnectionService;
        if (!connection.isIncoming() && (telecomAccountRegistry = TelecomAccountRegistry.getInstance(null)) != null && (telephonyConnectionService = telecomAccountRegistry.getTelephonyConnectionService()) != null) {
            for (android.telecom.Connection connection2 : telephonyConnectionService.getAllConnections()) {
                if ((connection2 instanceof TelephonyConnection) && maybeSwapWithUnknownConnection((TelephonyConnection) connection2, connection)) {
                    return true;
                }
            }
            return false;
        }
        return false;
    }

    private boolean maybeSwapWithUnknownConnection(TelephonyConnection telephonyConnection, Connection connection) {
        Connection originalConnection = telephonyConnection.getOriginalConnection();
        if (originalConnection == null || originalConnection.isIncoming() || !Objects.equals(originalConnection.getAddress(), connection.getAddress())) {
            return false;
        }
        if ((connection instanceof ImsExternalConnection) && !(telephonyConnection.getOriginalConnection() instanceof ImsExternalConnection)) {
            Log.v(this, "maybeSwapWithUnknownConnection - not swapping regular connection with external connection.", new Object[0]);
            return false;
        }
        telephonyConnection.setOriginalConnection(connection);
        if (!(originalConnection instanceof ImsExternalConnection) && originalConnection.getCall() != null && originalConnection.getCall().getPhone() != null && (originalConnection.getCall().getPhone() instanceof GsmCdmaPhone)) {
            originalConnection.getCall().getPhone().getCallTracker().cleanupCalls();
            Log.i(this, "maybeSwapWithUnknownConnection - Invoking call tracker cleanup for connection: " + originalConnection, new Object[0]);
        }
        return true;
    }

    public void setPhoneAccountHandle(PhoneAccountHandle phoneAccountHandle) {
        this.mPhoneAccountHandle = phoneAccountHandle;
    }
}
