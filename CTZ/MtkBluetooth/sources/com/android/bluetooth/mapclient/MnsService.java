package com.android.bluetooth.mapclient;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.util.Log;
import com.android.bluetooth.BluetoothObexTransport;
import com.android.bluetooth.IObexConnectionHandler;
import com.android.bluetooth.ObexServerSockets;
import com.android.bluetooth.sdp.SdpManager;
import java.io.IOException;
import javax.obex.Authenticator;
import javax.obex.ServerSession;

public class MnsService {
    static final int EVENT_REPORT = 1001;
    private static final int MNS_VERSION = 257;
    static final int MSG_EVENT = 1;
    private static final String TAG = "MnsService";
    private static MapClientService sContext;
    private int mSdpHandle;
    private volatile boolean mShutdown = false;
    private static final Boolean DBG = false;
    private static final Boolean VDBG = false;
    private static SocketAcceptor sAcceptThread = null;
    private static Handler sSessionHandler = null;
    private static BluetoothServerSocket sServerSocket = null;
    private static ObexServerSockets sServerSockets = null;

    MnsService(MapClientService mapClientService) {
        this.mSdpHandle = -1;
        if (VDBG.booleanValue()) {
            Log.v(TAG, "MnsService()");
        }
        sContext = mapClientService;
        sAcceptThread = new SocketAcceptor();
        sServerSockets = ObexServerSockets.createWithFixedChannels(sAcceptThread, 22, SdpManager.MNS_L2CAP_PSM);
        SdpManager defaultManager = SdpManager.getDefaultManager();
        if (defaultManager == null) {
            Log.e(TAG, "SdpManager is null");
        } else {
            this.mSdpHandle = defaultManager.createMapMnsRecord("MAP Message Notification Service", sServerSockets.getRfcommChannel(), -1, 257, 3);
        }
    }

    void stop() {
        if (VDBG.booleanValue()) {
            Log.v(TAG, "stop()");
        }
        this.mShutdown = true;
        cleanUpSdpRecord();
        if (sServerSockets != null) {
            sServerSockets.shutdown(false);
            sServerSockets = null;
        }
    }

    private void cleanUpSdpRecord() {
        if (this.mSdpHandle < 0) {
            Log.e(TAG, "cleanUpSdpRecord, SDP record never created");
            return;
        }
        int i = this.mSdpHandle;
        this.mSdpHandle = -1;
        SdpManager defaultManager = SdpManager.getDefaultManager();
        if (defaultManager == null) {
            Log.e(TAG, "cleanUpSdpRecord failed, sdpManager is null, sdpHandle=" + i);
            return;
        }
        Log.i(TAG, "cleanUpSdpRecord, mSdpHandle=" + i);
        if (!defaultManager.removeSdpRecord(i)) {
            Log.e(TAG, "cleanUpSdpRecord, removeSdpRecord failed, sdpHandle=" + i);
        }
    }

    private class SocketAcceptor implements IObexConnectionHandler {
        private boolean mInterrupted;

        private SocketAcceptor() {
            this.mInterrupted = false;
        }

        @Override
        public synchronized void onAcceptFailed() {
            Log.e(MnsService.TAG, "OnAcceptFailed");
            ObexServerSockets unused = MnsService.sServerSockets = null;
            if (MnsService.this.mShutdown) {
                Log.e(MnsService.TAG, "Failed to accept incomming connection - shutdown");
            }
        }

        @Override
        public synchronized boolean onConnect(BluetoothDevice bluetoothDevice, BluetoothSocket bluetoothSocket) {
            if (MnsService.DBG.booleanValue()) {
                Log.d(MnsService.TAG, "onConnect" + bluetoothDevice + " SOCKET: " + bluetoothSocket);
            }
            MceStateMachine mceStateMachineForDevice = MnsService.sContext.getMceStateMachineForDevice(bluetoothDevice);
            if (mceStateMachineForDevice == null) {
                Log.e(MnsService.TAG, "Error: NO statemachine for device: " + bluetoothDevice.getAddress() + " (name: " + bluetoothDevice.getName());
                return false;
            }
            try {
                new ServerSession(new BluetoothObexTransport(bluetoothSocket), new MnsObexServer(mceStateMachineForDevice, MnsService.sServerSockets), (Authenticator) null);
                return true;
            } catch (IOException e) {
                Log.e(MnsService.TAG, e.toString());
                return false;
            }
        }
    }
}
