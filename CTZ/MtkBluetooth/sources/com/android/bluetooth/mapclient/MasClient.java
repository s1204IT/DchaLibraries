package com.android.bluetooth.mapclient;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.bluetooth.SdpMasRecord;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;
import com.android.bluetooth.BluetoothObexTransport;
import com.android.internal.util.StateMachine;
import java.io.IOException;
import java.lang.ref.WeakReference;
import javax.obex.ClientSession;
import javax.obex.HeaderSet;

public class MasClient {
    private static final byte[] BLUETOOTH_UUID_OBEX_MAS = {-69, 88, 43, 64, 66, 12, 17, -37, -80, -34, 8, 0, 32, 12, -102, 102};
    private static final int CONNECT = 0;
    private static final boolean DBG = false;
    private static final int DISCONNECT = 1;
    private static final int MAP_FEATURE_NOTIFICATION = 2;
    private static final int MAP_FEATURE_NOTIFICATION_REGISTRATION = 1;
    static final int MAP_SUPPORTED_FEATURES = 3;
    private static final byte OAP_TAGID_MAP_SUPPORTED_FEATURES = 41;
    private static final int REQUEST = 2;
    private static final String TAG = "MasClient";
    private static final boolean VDBG = false;
    private final StateMachine mCallback;
    private boolean mConnected = false;
    private Handler mHandler;
    private BluetoothDevice mRemoteDevice;
    SdpMasRecord mSdpMasRecord;
    private ClientSession mSession;
    private BluetoothSocket mSocket;
    private HandlerThread mThread;
    private BluetoothObexTransport mTransport;

    public enum CharsetType {
        NATIVE,
        UTF_8
    }

    public MasClient(BluetoothDevice bluetoothDevice, StateMachine stateMachine, SdpMasRecord sdpMasRecord) {
        if (bluetoothDevice == null) {
            throw new NullPointerException("Obex transport is null");
        }
        this.mRemoteDevice = bluetoothDevice;
        this.mCallback = stateMachine;
        this.mSdpMasRecord = sdpMasRecord;
        this.mThread = new HandlerThread("Client");
        this.mThread.start();
        this.mHandler = new MasClientHandler(this.mThread.getLooper(), this);
        this.mHandler.obtainMessage(0).sendToTarget();
    }

    private void connect() {
        try {
            this.mSocket = this.mRemoteDevice.createRfcommSocket(this.mSdpMasRecord.getRfcommCannelNumber());
            Log.d(TAG, this.mRemoteDevice.toString() + "Socket: " + this.mSocket.toString());
            this.mSocket.connect();
            this.mTransport = new BluetoothObexTransport(this.mSocket);
            this.mSession = new ClientSession(this.mTransport);
            HeaderSet headerSet = new HeaderSet();
            headerSet.setHeader(70, BLUETOOTH_UUID_OBEX_MAS);
            ObexAppParameters obexAppParameters = new ObexAppParameters();
            obexAppParameters.add(OAP_TAGID_MAP_SUPPORTED_FEATURES, 3);
            obexAppParameters.addToHeaderSet(headerSet);
            HeaderSet headerSetConnect = this.mSession.connect(headerSet);
            Log.d(TAG, "Connection results" + headerSetConnect.getResponseCode());
            if (headerSetConnect.getResponseCode() == 160) {
                this.mConnected = true;
                this.mCallback.sendMessage(1001);
            } else {
                disconnect();
            }
        } catch (IOException e) {
            Log.e(TAG, "Caught an exception " + e.toString());
            disconnect();
        }
    }

    private void disconnect() {
        if (this.mSession != null) {
            try {
                this.mSession.disconnect((HeaderSet) null);
            } catch (IOException e) {
                Log.e(TAG, "Caught an exception while disconnecting:" + e.toString());
            }
            try {
                this.mSession.close();
            } catch (IOException e2) {
                Log.e(TAG, "Caught an exception while closing:" + e2.toString());
            }
        }
        this.mConnected = false;
        this.mCallback.sendMessage(1002);
    }

    private void executeRequest(Request request) {
        try {
            request.execute(this.mSession);
            this.mCallback.sendMessage(1003, request);
        } catch (IOException e) {
            disconnect();
        }
    }

    public boolean makeRequest(Request request) {
        if (!this.mHandler.sendMessage(this.mHandler.obtainMessage(2, request))) {
            Log.e(TAG, "Adding messages failed, state: " + this.mConnected);
            return false;
        }
        return true;
    }

    public void shutdown() {
        this.mHandler.obtainMessage(1).sendToTarget();
        this.mThread.quitSafely();
    }

    private static class MasClientHandler extends Handler {
        WeakReference<MasClient> mInst;

        MasClientHandler(Looper looper, MasClient masClient) {
            super(looper);
            this.mInst = new WeakReference<>(masClient);
        }

        @Override
        public void handleMessage(android.os.Message message) {
            MasClient masClient = this.mInst.get();
            if (masClient.mConnected || message.what == 0) {
                switch (message.what) {
                    case 0:
                        masClient.connect();
                        break;
                    case 1:
                        masClient.disconnect();
                        break;
                    case 2:
                        masClient.executeRequest((Request) message.obj);
                        break;
                }
            }
            Log.w(MasClient.TAG, "Cannot execute " + message + " when not CONNECTED.");
        }
    }
}
