package com.android.bluetooth.hfpclient.connserv;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadsetClient;
import android.bluetooth.BluetoothHeadsetClientCall;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.telecom.Connection;
import android.telecom.DisconnectCause;
import android.telecom.PhoneAccount;
import android.telecom.TelecomManager;
import android.util.Log;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class HfpClientDeviceBlock {
    private static final boolean DBG = false;
    private HfpClientConference mConference;
    private final HfpClientConnectionService mConnServ;
    private final Map<UUID, HfpClientConnection> mConnections = new HashMap();
    private final Context mContext;
    private final BluetoothDevice mDevice;
    private BluetoothHeadsetClient mHeadsetProfile;
    private final PhoneAccount mPhoneAccount;
    private final String mTAG;
    private final TelecomManager mTelecomManager;

    HfpClientDeviceBlock(HfpClientConnectionService hfpClientConnectionService, BluetoothDevice bluetoothDevice, BluetoothHeadsetClient bluetoothHeadsetClient) {
        this.mConnServ = hfpClientConnectionService;
        this.mContext = hfpClientConnectionService;
        this.mDevice = bluetoothDevice;
        this.mTAG = "HfpClientDeviceBlock." + this.mDevice.getAddress();
        this.mPhoneAccount = HfpClientConnectionService.createAccount(this.mContext, bluetoothDevice);
        this.mTelecomManager = (TelecomManager) this.mContext.getSystemService("telecom");
        this.mTelecomManager.registerPhoneAccount(this.mPhoneAccount);
        this.mTelecomManager.enablePhoneAccount(this.mPhoneAccount.getAccountHandle(), true);
        this.mTelecomManager.setUserSelectedOutgoingPhoneAccount(this.mPhoneAccount.getAccountHandle());
        this.mHeadsetProfile = bluetoothHeadsetClient;
        if (this.mHeadsetProfile != null) {
            List currentCalls = this.mHeadsetProfile.getCurrentCalls(this.mDevice);
            if (currentCalls == null) {
                Log.w(this.mTAG, "Got connected but calls were null, ignoring the broadcast");
                return;
            }
            Iterator it = currentCalls.iterator();
            while (it.hasNext()) {
                handleCall((BluetoothHeadsetClientCall) it.next());
            }
            return;
        }
        Log.e(this.mTAG, "headset profile is null, ignoring broadcast.");
    }

    synchronized Connection onCreateIncomingConnection(BluetoothHeadsetClientCall bluetoothHeadsetClientCall) {
        HfpClientConnection hfpClientConnection = this.mConnections.get(bluetoothHeadsetClientCall.getUUID());
        if (hfpClientConnection != null) {
            hfpClientConnection.onAdded();
            return hfpClientConnection;
        }
        Log.e(this.mTAG, "Call " + bluetoothHeadsetClientCall + " ignored: connection does not exist");
        return null;
    }

    Connection onCreateOutgoingConnection(Uri uri) {
        HfpClientConnection hfpClientConnectionBuildConnection = buildConnection(null, uri);
        if (hfpClientConnectionBuildConnection != null) {
            hfpClientConnectionBuildConnection.onAdded();
        }
        return hfpClientConnectionBuildConnection;
    }

    synchronized Connection onCreateUnknownConnection(BluetoothHeadsetClientCall bluetoothHeadsetClientCall) {
        Uri.fromParts("tel", bluetoothHeadsetClientCall.getNumber(), null);
        HfpClientConnection hfpClientConnection = this.mConnections.get(bluetoothHeadsetClientCall.getUUID());
        if (hfpClientConnection != null) {
            hfpClientConnection.onAdded();
            return hfpClientConnection;
        }
        Log.e(this.mTAG, "Call " + bluetoothHeadsetClientCall + " ignored: connection does not exist");
        return null;
    }

    synchronized void onConference(Connection connection, Connection connection2) {
        if (this.mConference == null) {
            this.mConference = new HfpClientConference(this.mPhoneAccount.getAccountHandle(), this.mDevice, this.mHeadsetProfile);
        }
        if (connection.getConference() == null) {
            this.mConference.addConnection(connection);
        }
        if (connection2.getConference() == null) {
            this.mConference.addConnection(connection2);
        }
    }

    synchronized void cleanup() {
        Log.d(this.mTAG, "Resetting state for device " + this.mDevice);
        disconnectAll();
        this.mTelecomManager.unregisterPhoneAccount(this.mPhoneAccount.getAccountHandle());
    }

    synchronized void handleCall(BluetoothHeadsetClientCall bluetoothHeadsetClientCall) {
        HfpClientConnection hfpClientConnectionFindConnectionKey = findConnectionKey(bluetoothHeadsetClientCall);
        if (hfpClientConnectionFindConnectionKey != null && isDisconnectingToActive(hfpClientConnectionFindConnectionKey, bluetoothHeadsetClientCall)) {
            hfpClientConnectionFindConnectionKey.close(1);
            this.mConnections.remove(bluetoothHeadsetClientCall.getUUID());
            hfpClientConnectionFindConnectionKey = null;
        }
        if (hfpClientConnectionFindConnectionKey != null) {
            hfpClientConnectionFindConnectionKey.updateCall(bluetoothHeadsetClientCall);
            hfpClientConnectionFindConnectionKey.handleCallChanged();
        }
        if (hfpClientConnectionFindConnectionKey == null) {
            buildConnection(bluetoothHeadsetClientCall, null);
            Bundle bundle = new Bundle();
            if (bluetoothHeadsetClientCall.getState() == 2 || bluetoothHeadsetClientCall.getState() == 3 || bluetoothHeadsetClientCall.getState() == 0 || bluetoothHeadsetClientCall.getState() == 1) {
                bundle.putParcelable("android.telecom.extra.OUTGOING_CALL_EXTRAS", bluetoothHeadsetClientCall);
                this.mTelecomManager.addNewUnknownCall(this.mPhoneAccount.getAccountHandle(), bundle);
            } else if (bluetoothHeadsetClientCall.getState() == 4 || bluetoothHeadsetClientCall.getState() == 5) {
                bundle.putParcelable("android.telecom.extra.INCOMING_CALL_EXTRAS", bluetoothHeadsetClientCall);
                bundle.putBoolean("android.telecom.extra.CALL_EXTERNAL_RINGER", bluetoothHeadsetClientCall.isInBandRing());
                this.mTelecomManager.addNewIncomingCall(this.mPhoneAccount.getAccountHandle(), bundle);
            }
        } else if (bluetoothHeadsetClientCall.getState() == 7) {
            this.mConnections.remove(bluetoothHeadsetClientCall.getUUID());
        }
        updateConferenceableConnections();
    }

    private synchronized HfpClientConnection findConnectionKey(BluetoothHeadsetClientCall bluetoothHeadsetClientCall) {
        return this.mConnections.get(bluetoothHeadsetClientCall.getUUID());
    }

    private void disconnectAll() {
        Iterator<HfpClientConnection> it = this.mConnections.values().iterator();
        while (it.hasNext()) {
            it.next().onHfpDisconnected();
        }
        this.mConnections.clear();
        if (this.mConference != null) {
            this.mConference.destroy();
            this.mConference = null;
        }
    }

    private boolean isDisconnectingToActive(HfpClientConnection hfpClientConnection, BluetoothHeadsetClientCall bluetoothHeadsetClientCall) {
        if (hfpClientConnection.isClosing() && bluetoothHeadsetClientCall.getState() != 7) {
            return true;
        }
        return false;
    }

    private synchronized HfpClientConnection buildConnection(BluetoothHeadsetClientCall bluetoothHeadsetClientCall, Uri uri) {
        HfpClientConnection hfpClientConnection;
        if (this.mHeadsetProfile == null) {
            Log.e(this.mTAG, "Cannot create connection for call " + bluetoothHeadsetClientCall + " when Profile not available");
            return null;
        }
        if (bluetoothHeadsetClientCall == null && uri == null) {
            Log.e(this.mTAG, "Both call and number cannot be null.");
            return null;
        }
        if (bluetoothHeadsetClientCall != null) {
            hfpClientConnection = new HfpClientConnection(this.mConnServ, this.mDevice, this.mHeadsetProfile, bluetoothHeadsetClientCall);
        } else {
            hfpClientConnection = new HfpClientConnection(this.mConnServ, this.mDevice, this.mHeadsetProfile, uri);
        }
        if (hfpClientConnection.getState() != 6) {
            this.mConnections.put(hfpClientConnection.getUUID(), hfpClientConnection);
        }
        return hfpClientConnection;
    }

    private void updateConferenceableConnections() {
        if (this.mConference != null) {
            for (Connection connection : this.mConference.getConnections()) {
                if (!((HfpClientConnection) connection).inConference()) {
                    this.mConference.removeConnection(connection);
                }
            }
        }
        boolean z = false;
        for (HfpClientConnection hfpClientConnection : this.mConnections.values()) {
            if (hfpClientConnection.inConference()) {
                if (this.mConference == null) {
                    this.mConference = new HfpClientConference(this.mPhoneAccount.getAccountHandle(), this.mDevice, this.mHeadsetProfile);
                }
                if (this.mConference.addConnection(hfpClientConnection)) {
                    z = true;
                }
            }
        }
        if (this.mConference != null && this.mConference.getConnections().size() == 0) {
            this.mConference.setDisconnected(new DisconnectCause(2));
            this.mConference.destroy();
            this.mConference = null;
        }
        if (this.mConference != null && z) {
            this.mConnServ.addConference(this.mConference);
        }
    }
}
