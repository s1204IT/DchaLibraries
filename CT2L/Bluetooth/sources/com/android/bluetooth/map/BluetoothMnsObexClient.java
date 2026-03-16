package com.android.bluetooth.map;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.ParcelUuid;
import android.util.Log;
import android.util.SparseBooleanArray;
import java.io.IOException;
import java.io.OutputStream;
import javax.obex.ClientOperation;
import javax.obex.ClientSession;
import javax.obex.HeaderSet;
import javax.obex.ObexTransport;

public class BluetoothMnsObexClient {
    public static final ParcelUuid BLUETOOTH_UUID_OBEX_MNS = ParcelUuid.fromString("00001133-0000-1000-8000-00805F9B34FB");
    private static final boolean D = true;
    public static final int MSG_MNS_NOTIFICATION_REGISTRATION = 1;
    public static final int MSG_MNS_SEND_EVENT = 2;
    private static final String TAG = "BluetoothMnsObexClient";
    private static final String TYPE_EVENT = "x-bt/MAP-event-report";
    private static final boolean V = false;
    private Handler mCallback;
    private ClientSession mClientSession;
    public Handler mHandler;
    BluetoothDevice mRemoteDevice;
    private ObexTransport mTransport;
    private volatile boolean mWaitingForRemote;
    private boolean mConnected = false;
    private SparseBooleanArray mRegisteredMasIds = new SparseBooleanArray(1);
    private HeaderSet mHsConnect = null;

    public BluetoothMnsObexClient(BluetoothDevice remoteDevice, Handler callback) {
        this.mHandler = null;
        this.mCallback = null;
        if (remoteDevice == null) {
            throw new NullPointerException("Obex transport is null");
        }
        this.mRemoteDevice = remoteDevice;
        HandlerThread thread = new HandlerThread(TAG);
        thread.start();
        Looper looper = thread.getLooper();
        this.mHandler = new MnsObexClientHandler(looper);
        this.mCallback = callback;
    }

    public Handler getMessageHandler() {
        return this.mHandler;
    }

    private final class MnsObexClientHandler extends Handler {
        private MnsObexClientHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    BluetoothMnsObexClient.this.handleRegistration(msg.arg1, msg.arg2);
                    break;
                case 2:
                    BluetoothMnsObexClient.this.sendEventHandler((byte[]) msg.obj, msg.arg1);
                    break;
            }
        }
    }

    public boolean isConnected() {
        return this.mConnected;
    }

    public synchronized void disconnect() {
        try {
        } catch (IOException e) {
            Log.w(TAG, "OBEX session disconnect error " + e.getMessage());
        }
        if (this.mClientSession != null) {
            this.mClientSession.disconnect((HeaderSet) null);
            Log.d(TAG, "OBEX session disconnected");
            try {
            } catch (IOException e2) {
                Log.w(TAG, "OBEX session close error:" + e2.getMessage());
            }
            if (this.mClientSession == null) {
                Log.d(TAG, "OBEX session close mClientSession");
                this.mClientSession.close();
                this.mClientSession = null;
                Log.d(TAG, "OBEX session closed");
                if (this.mTransport != null) {
                    try {
                        Log.d(TAG, "Close Obex Transport");
                        this.mTransport.close();
                        this.mTransport = null;
                        this.mConnected = false;
                        Log.d(TAG, "Obex Transport Closed");
                    } catch (IOException e3) {
                        Log.e(TAG, "mTransport.close error: " + e3.getMessage());
                    }
                }
            } else if (this.mTransport != null) {
            }
        } else if (this.mClientSession == null) {
        }
    }

    public void shutdown() {
        if (this.mHandler != null) {
            this.mHandler.removeCallbacksAndMessages(null);
            Looper looper = this.mHandler.getLooper();
            if (looper != null) {
                looper.quit();
            }
            this.mHandler = null;
        }
        disconnect();
        this.mRegisteredMasIds.clear();
    }

    public void handleRegistration(int masId, int notificationStatus) {
        Log.d(TAG, "handleRegistration( " + masId + ", " + notificationStatus + ")");
        if (notificationStatus == 0) {
            this.mRegisteredMasIds.delete(masId);
        } else if (notificationStatus == 1) {
            if (!isConnected()) {
                Log.d(TAG, "handleRegistration: connect");
                connect();
            }
            this.mRegisteredMasIds.put(masId, true);
        }
        if (this.mRegisteredMasIds.size() == 0) {
            Log.d(TAG, "handleRegistration: disconnect");
            disconnect();
        }
    }

    public void connect() {
        this.mConnected = true;
        try {
            BluetoothSocket btSocket = this.mRemoteDevice.createInsecureRfcommSocketToServiceRecord(BLUETOOTH_UUID_OBEX_MNS.getUuid());
            btSocket.connect();
            this.mTransport = new BluetoothMnsRfcommTransport(btSocket);
            try {
                this.mClientSession = new ClientSession(this.mTransport);
            } catch (IOException e1) {
                Log.e(TAG, "OBEX session create error " + e1.getMessage());
                this.mConnected = false;
            }
            if (this.mConnected && this.mClientSession != null) {
                boolean connected = false;
                HeaderSet hs = new HeaderSet();
                byte[] mnsTarget = {-69, 88, 43, 65, 66, 12, 17, -37, -80, -34, 8, 0, 32, 12, -102, 102};
                hs.setHeader(70, mnsTarget);
                synchronized (this) {
                    this.mWaitingForRemote = true;
                }
                try {
                    this.mHsConnect = this.mClientSession.connect(hs);
                    Log.d(TAG, "OBEX session created");
                    connected = true;
                } catch (IOException e) {
                    Log.e(TAG, "OBEX session connect error " + e.getMessage());
                }
                this.mConnected = connected;
            }
            synchronized (this) {
                this.mWaitingForRemote = false;
            }
        } catch (IOException e2) {
            Log.e(TAG, "BtSocket Connect error " + e2.getMessage(), e2);
            this.mConnected = false;
        }
    }

    public void sendEvent(byte[] eventBytes, int masInstanceId) {
        Message msg;
        if (this.mHandler != null && (msg = this.mHandler.obtainMessage(2, masInstanceId, 0, eventBytes)) != null) {
            msg.sendToTarget();
        }
        notifyUpdateWakeLock();
    }

    private int sendEventHandler(byte[] eventBytes, int masInstanceId) {
        boolean error = false;
        int responseCode = -1;
        int bytesWritten = 0;
        ClientSession clientSession = this.mClientSession;
        if (!this.mConnected || clientSession == null) {
            Log.w(TAG, "sendEvent after disconnect:" + this.mConnected);
            return -1;
        }
        HeaderSet request = new HeaderSet();
        BluetoothMapAppParams appParams = new BluetoothMapAppParams();
        appParams.setMasInstanceId(masInstanceId);
        ClientOperation putOperation = null;
        OutputStream outputStream = null;
        try {
            try {
                try {
                    request.setHeader(66, TYPE_EVENT);
                    request.setHeader(76, appParams.EncodeParams());
                    if (this.mHsConnect.mConnectionID != null) {
                        request.mConnectionID = new byte[4];
                        System.arraycopy(this.mHsConnect.mConnectionID, 0, request.mConnectionID, 0, 4);
                    } else {
                        Log.w(TAG, "sendEvent: no connection ID");
                    }
                    synchronized (this) {
                        this.mWaitingForRemote = true;
                    }
                    try {
                        putOperation = (ClientOperation) clientSession.put(request);
                    } catch (IOException e) {
                        Log.e(TAG, "Error when put HeaderSet " + e.getMessage());
                        error = true;
                    }
                    synchronized (this) {
                        this.mWaitingForRemote = false;
                    }
                    if (!error) {
                        try {
                            outputStream = putOperation.openOutputStream();
                        } catch (IOException e2) {
                            Log.e(TAG, "Error when opening OutputStream " + e2.getMessage());
                            error = true;
                        }
                    }
                    if (!error) {
                        int maxChunkSize = putOperation.getMaxPacketSize();
                        while (bytesWritten < eventBytes.length) {
                            int bytesToWrite = Math.min(maxChunkSize, eventBytes.length - bytesWritten);
                            outputStream.write(eventBytes, bytesWritten, bytesToWrite);
                            bytesWritten += bytesToWrite;
                        }
                        if (bytesWritten == eventBytes.length) {
                            Log.i(TAG, "SendEvent finished send length" + eventBytes.length);
                        } else {
                            error = true;
                            putOperation.abort();
                            Log.i(TAG, "SendEvent interrupted");
                        }
                    }
                    if (outputStream != null) {
                        try {
                            outputStream.close();
                        } catch (IOException e3) {
                            Log.e(TAG, "Error when closing stream after send " + e3.getMessage());
                        }
                    }
                    if (!error && putOperation != null) {
                        try {
                            responseCode = putOperation.getResponseCode();
                            if (responseCode != -1 && responseCode != 160) {
                                Log.i(TAG, "Response error code is " + responseCode);
                            }
                        } catch (IOException e4) {
                            Log.e(TAG, "Error when closing stream after send " + e4.getMessage());
                        }
                    }
                    if (putOperation != null) {
                        putOperation.close();
                    }
                } catch (Throwable th) {
                    if (0 != 0) {
                        try {
                            outputStream.close();
                        } catch (IOException e5) {
                            Log.e(TAG, "Error when closing stream after send " + e5.getMessage());
                        }
                    }
                    if (0 == 0 && 0 != 0) {
                        try {
                            int responseCode2 = putOperation.getResponseCode();
                            if (responseCode2 != -1 && responseCode2 != 160) {
                                Log.i(TAG, "Response error code is " + responseCode2);
                            }
                        } catch (IOException e6) {
                            Log.e(TAG, "Error when closing stream after send " + e6.getMessage());
                            throw th;
                        }
                    }
                    if (0 != 0) {
                        putOperation.close();
                    }
                    throw th;
                }
            } catch (IndexOutOfBoundsException e7) {
                handleSendException(e7.toString());
                if (0 != 0) {
                    try {
                        outputStream.close();
                    } catch (IOException e8) {
                        Log.e(TAG, "Error when closing stream after send " + e8.getMessage());
                    }
                }
                if (1 == 0 && 0 != 0) {
                    try {
                        responseCode = putOperation.getResponseCode();
                        if (responseCode != -1 && responseCode != 160) {
                            Log.i(TAG, "Response error code is " + responseCode);
                        }
                    } catch (IOException e9) {
                        Log.e(TAG, "Error when closing stream after send " + e9.getMessage());
                    }
                }
                if (0 != 0) {
                    putOperation.close();
                }
            }
        } catch (IOException e10) {
            handleSendException(e10.toString());
            if (0 != 0) {
                try {
                    outputStream.close();
                } catch (IOException e11) {
                    Log.e(TAG, "Error when closing stream after send " + e11.getMessage());
                }
            }
            if (1 == 0 && 0 != 0) {
                try {
                    responseCode = putOperation.getResponseCode();
                    if (responseCode != -1 && responseCode != 160) {
                        Log.i(TAG, "Response error code is " + responseCode);
                    }
                } catch (IOException e12) {
                    Log.e(TAG, "Error when closing stream after send " + e12.getMessage());
                }
            }
            if (0 != 0) {
                putOperation.close();
            }
        }
        return responseCode;
    }

    private void handleSendException(String exception) {
        Log.e(TAG, "Error when sending event: " + exception);
    }

    private void notifyUpdateWakeLock() {
        if (this.mCallback != null) {
            Message msg = Message.obtain(this.mCallback);
            msg.what = 5005;
            msg.sendToTarget();
        }
    }
}
