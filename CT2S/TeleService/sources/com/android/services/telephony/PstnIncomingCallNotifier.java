package com.android.services.telephony;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.AsyncResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.text.TextUtils;
import com.android.internal.telephony.Call;
import com.android.internal.telephony.CallStateException;
import com.android.internal.telephony.Connection;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneProxy;
import com.android.internal.telephony.cdma.CdmaCallWaitingNotification;
import com.google.common.base.Preconditions;
import java.util.Objects;

final class PstnIncomingCallNotifier {
    private Phone mPhoneBase;
    private final PhoneProxy mPhoneProxy;
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 100:
                    PstnIncomingCallNotifier.this.handleNewRingingConnection((AsyncResult) msg.obj);
                    break;
                case 101:
                    PstnIncomingCallNotifier.this.handleCdmaCallWaiting((AsyncResult) msg.obj);
                    break;
                case 102:
                    PstnIncomingCallNotifier.this.handleNewUnknownConnection((AsyncResult) msg.obj);
                    break;
            }
        }
    };
    private final BroadcastReceiver mRATReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("android.intent.action.RADIO_TECHNOLOGY".equals(action)) {
                String newPhone = intent.getStringExtra("phoneName");
                Log.d(this, "Radio technology switched. Now %s is active.", newPhone);
                PstnIncomingCallNotifier.this.registerForNotifications();
            }
        }
    };

    PstnIncomingCallNotifier(PhoneProxy phoneProxy) {
        Preconditions.checkNotNull(phoneProxy);
        this.mPhoneProxy = phoneProxy;
        registerForNotifications();
        IntentFilter intentFilter = new IntentFilter("android.intent.action.RADIO_TECHNOLOGY");
        this.mPhoneProxy.getContext().registerReceiver(this.mRATReceiver, intentFilter);
    }

    void teardown() {
        unregisterForNotifications();
        this.mPhoneProxy.getContext().unregisterReceiver(this.mRATReceiver);
    }

    private void registerForNotifications() {
        Phone newPhone = this.mPhoneProxy.getActivePhone();
        if (newPhone != this.mPhoneBase) {
            unregisterForNotifications();
            if (newPhone != null) {
                Log.i(this, "Registering: %s", newPhone);
                this.mPhoneBase = newPhone;
                this.mPhoneBase.registerForNewRingingConnection(this.mHandler, 100, (Object) null);
                this.mPhoneBase.registerForCallWaiting(this.mHandler, 101, (Object) null);
                this.mPhoneBase.registerForUnknownConnection(this.mHandler, 102, (Object) null);
            }
        }
    }

    private void unregisterForNotifications() {
        if (this.mPhoneBase != null) {
            Log.i(this, "Unregistering: %s", this.mPhoneBase);
            this.mPhoneBase.unregisterForNewRingingConnection(this.mHandler);
            this.mPhoneBase.unregisterForCallWaiting(this.mHandler);
            this.mPhoneBase.unregisterForUnknownConnection(this.mHandler);
        }
    }

    private void handleNewRingingConnection(AsyncResult asyncResult) {
        Call call;
        Log.d(this, "handleNewRingingConnection", new Object[0]);
        Connection connection = (Connection) asyncResult.result;
        if (connection != null && (call = connection.getCall()) != null && call.getState().isRinging()) {
            sendIncomingCallIntent(connection);
        }
    }

    private void handleCdmaCallWaiting(AsyncResult asyncResult) {
        Connection connection;
        String number;
        Log.d(this, "handleCdmaCallWaiting", new Object[0]);
        CdmaCallWaitingNotification ccwi = (CdmaCallWaitingNotification) asyncResult.result;
        Call call = this.mPhoneBase.getRingingCall();
        if (call.getState() == Call.State.WAITING && (connection = call.getLatestConnection()) != null && (number = connection.getAddress()) != null && Objects.equals(number, ccwi.number)) {
            sendIncomingCallIntent(connection);
        }
    }

    private void handleNewUnknownConnection(AsyncResult asyncResult) {
        Call call;
        Log.i(this, "handleNewUnknownConnection", new Object[0]);
        if (!(asyncResult.result instanceof Connection)) {
            Log.w(this, "handleNewUnknownConnection called with non-Connection object", new Object[0]);
            return;
        }
        Connection connection = (Connection) asyncResult.result;
        if (connection != null && (call = connection.getCall()) != null && call.getState().isAlive()) {
            addNewUnknownCall(connection);
        }
    }

    private void addNewUnknownCall(Connection connection) {
        Log.i(this, "addNewUnknownCall, connection is: %s", connection);
        if (!maybeSwapAnyWithUnknownConnection(connection)) {
            Log.i(this, "determined new connection is: %s", connection);
            Bundle extras = null;
            if (connection.getNumberPresentation() == 1 && !TextUtils.isEmpty(connection.getAddress())) {
                extras = new Bundle();
                Uri uri = Uri.fromParts("tel", connection.getAddress(), null);
                extras.putParcelable("android.telecom.extra.UNKNOWN_CALL_HANDLE", uri);
            }
            PhoneAccountHandle handle = findCorrectPhoneAccountHandle();
            if (handle == null) {
                try {
                    connection.hangup();
                    return;
                } catch (CallStateException e) {
                    return;
                }
            } else {
                TelecomManager.from(this.mPhoneProxy.getContext()).addNewUnknownCall(handle, extras);
                return;
            }
        }
        Log.i(this, "swapped an old connection, new one is: %s", connection);
    }

    private void sendIncomingCallIntent(Connection connection) {
        Bundle extras = null;
        if (connection.getNumberPresentation() == 1 && !TextUtils.isEmpty(connection.getAddress())) {
            extras = new Bundle();
            Uri uri = Uri.fromParts("tel", connection.getAddress(), null);
            extras.putParcelable("incoming_number", uri);
        }
        PhoneAccountHandle handle = findCorrectPhoneAccountHandle();
        if (handle == null) {
            try {
                connection.hangup();
            } catch (CallStateException e) {
            }
        } else {
            TelecomManager.from(this.mPhoneProxy.getContext()).addNewIncomingCall(handle, extras);
        }
    }

    private boolean maybeSwapAnyWithUnknownConnection(Connection unknown) {
        TelecomAccountRegistry registry;
        TelephonyConnectionService service;
        if (!unknown.isIncoming() && (registry = TelecomAccountRegistry.getInstance(null)) != null && (service = registry.getTelephonyConnectionService()) != null) {
            for (android.telecom.Connection telephonyConnection : service.getAllConnections()) {
                if (maybeSwapWithUnknownConnection((TelephonyConnection) telephonyConnection, unknown)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean maybeSwapWithUnknownConnection(TelephonyConnection telephonyConnection, Connection unknown) {
        Connection original = telephonyConnection.getOriginalConnection();
        if (original == null || original.isIncoming() || !Objects.equals(original.getAddress(), unknown.getAddress())) {
            return false;
        }
        telephonyConnection.setOriginalConnection(unknown);
        return true;
    }

    private PhoneAccountHandle findCorrectPhoneAccountHandle() {
        TelecomAccountRegistry telecomAccountRegistry = TelecomAccountRegistry.getInstance(null);
        PhoneAccountHandle handle = TelecomAccountRegistry.makePstnPhoneAccountHandle(this.mPhoneBase);
        if (!telecomAccountRegistry.hasAccountEntryForPhoneAccount(handle)) {
            PhoneAccountHandle emergencyHandle = TelecomAccountRegistry.makePstnPhoneAccountHandleWithPrefix(this.mPhoneBase, "", true);
            if (telecomAccountRegistry.hasAccountEntryForPhoneAccount(emergencyHandle)) {
                Log.i(this, "Receiving MT call in ECM. Using Emergency PhoneAccount Instead.", new Object[0]);
                return emergencyHandle;
            }
            Log.w(this, "PhoneAccount not found.", new Object[0]);
            return null;
        }
        return handle;
    }
}
