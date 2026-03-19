package com.android.services.telephony;

import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import com.android.internal.telephony.Call;
import com.android.internal.telephony.Connection;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.imsphone.ImsExternalConnection;
import com.google.common.base.Preconditions;
import java.util.Objects;

final class PstnNoAccountUnknownCallNotifier {
    private static final int EVENT_UNKNOWN_CONNECTION = 102;
    private static final String PROP_FORCE_DEBUG_KEY = "persist.vendor.log.tel_dbg";
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message message) {
            if (message.what == 102) {
                PstnNoAccountUnknownCallNotifier.this.handleNewUnknownConnection((AsyncResult) message.obj);
            }
        }
    };
    private final Phone mPhone;

    PstnNoAccountUnknownCallNotifier(Phone phone) {
        Preconditions.checkNotNull(phone);
        this.mPhone = phone;
        registerForNotifications();
    }

    void teardown() {
        unregisterForNotifications();
    }

    private void registerForNotifications() {
        if (this.mPhone != null) {
            Log.i(this, "Registering: %s", this.mPhone);
            this.mPhone.registerForUnknownConnection(this.mHandler, 102, (Object) null);
        }
    }

    private void unregisterForNotifications() {
        if (this.mPhone != null) {
            Log.i(this, "Unregistering: %s", this.mPhone);
            this.mPhone.unregisterForUnknownConnection(this.mHandler);
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
            if (call != null && call.getState().isAlive()) {
                addNewUnknownCall(connection);
            }
        }
    }

    private void addNewUnknownCall(Connection connection) {
        Log.i(this, "addNewUnknownCall, connection is: %s", connection);
        if (!maybeSwapAnyWithUnknownConnection(connection)) {
            Log.i(this, "For no account, no need to create new connection %s", connection);
        } else {
            Log.i(this, "swapped an old connection, new one is: %s", connection);
        }
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
        return true;
    }
}
