package com.android.bluetooth.map;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.bluetooth.SdpMnsRecord;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.ParcelUuid;
import android.util.Log;
import android.util.SparseBooleanArray;
import com.android.bluetooth.BluetoothObexTransport;
import com.android.bluetooth.sap.SapService;
import java.io.IOException;
import java.io.OutputStream;
import javax.obex.ClientOperation;
import javax.obex.ClientSession;
import javax.obex.HeaderSet;
import javax.obex.ObexTransport;

public class BluetoothMnsObexClient {
    private static final int MNS_NOTIFICATION_DELAY = 10;
    private static final int MNS_SDP_SEARCH_DELAY = 6000;
    public static final int MSG_MNS_NOTIFICATION_REGISTRATION = 1;
    public static final int MSG_MNS_SDP_SEARCH_REGISTRATION = 3;
    public static final int MSG_MNS_SEND_EVENT = 2;
    private static final String TAG = "BluetoothMnsObexClient";
    private static final String TYPE_EVENT = "x-bt/MAP-event-report";
    private static final boolean V = false;
    private Handler mCallback;
    private ClientSession mClientSession;
    public Handler mHandler;
    private SdpMnsRecord mMnsRecord;
    BluetoothDevice mRemoteDevice;
    private ObexTransport mTransport;
    private volatile boolean mWaitingForRemote;
    private static final boolean D = BluetoothMapService.DEBUG;
    public static final ParcelUuid BLUETOOTH_UUID_OBEX_MNS = ParcelUuid.fromString("00001133-0000-1000-8000-00805F9B34FB");
    private boolean mConnected = false;
    private SparseBooleanArray mRegisteredMasIds = new SparseBooleanArray(1);
    private HeaderSet mHsConnect = null;
    public MnsSdpSearchInfo mMnsLstRegRqst = null;

    public BluetoothMnsObexClient(BluetoothDevice bluetoothDevice, SdpMnsRecord sdpMnsRecord, Handler handler) {
        this.mHandler = null;
        this.mCallback = null;
        if (bluetoothDevice == null) {
            throw new NullPointerException("Obex transport is null");
        }
        this.mRemoteDevice = bluetoothDevice;
        HandlerThread handlerThread = new HandlerThread(TAG);
        handlerThread.start();
        this.mHandler = new MnsObexClientHandler(handlerThread.getLooper());
        this.mCallback = handler;
        this.mMnsRecord = sdpMnsRecord;
    }

    public Handler getMessageHandler() {
        return this.mHandler;
    }

    class MnsSdpSearchInfo {
        public int lastMasId;
        public int lastNotificationStatus;
        private boolean mIsSearchInProgress;

        MnsSdpSearchInfo(boolean z, int i, int i2) {
            this.mIsSearchInProgress = z;
            this.lastMasId = i;
            this.lastNotificationStatus = i2;
        }

        public boolean isSearchInProgress() {
            return this.mIsSearchInProgress;
        }

        public void setIsSearchInProgress(boolean z) {
            this.mIsSearchInProgress = z;
        }
    }

    private final class MnsObexClientHandler extends Handler {
        private MnsObexClientHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message message) throws Throwable {
            switch (message.what) {
                case 1:
                    if (!BluetoothMnsObexClient.this.isValidMnsRecord()) {
                        if (BluetoothMnsObexClient.D) {
                            Log.d(BluetoothMnsObexClient.TAG, "MNS SDP info not available yet - Cannot Connect.");
                        }
                    } else {
                        BluetoothMnsObexClient.this.handleRegistration(message.arg1, message.arg2);
                    }
                    break;
                case 2:
                    BluetoothMnsObexClient.this.sendEventHandler((byte[]) message.obj, message.arg1);
                    break;
                case 3:
                    BluetoothMnsObexClient.this.notifyMnsSdpSearch();
                    BluetoothMnsObexClient.this.mMnsLstRegRqst = BluetoothMnsObexClient.this.new MnsSdpSearchInfo(true, message.arg1, message.arg2);
                    BluetoothMnsObexClient.this.mHandler.sendMessageDelayed(BluetoothMnsObexClient.this.mHandler.obtainMessage(1, message.arg1, message.arg2), 6000L);
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
            if (D) {
                Log.d(TAG, "OBEX session disconnected");
                try {
                } catch (IOException e2) {
                    Log.w(TAG, "OBEX session close error:" + e2.getMessage());
                }
                if (this.mClientSession != null) {
                    if (D) {
                        Log.d(TAG, "OBEX session close mClientSession");
                    }
                    this.mClientSession.close();
                    this.mClientSession = null;
                    if (D) {
                        Log.d(TAG, "OBEX session closed");
                        if (this.mTransport != null) {
                        }
                    }
                } else if (this.mTransport != null) {
                    try {
                        if (D) {
                            Log.d(TAG, "Close Obex Transport");
                        }
                        this.mTransport.close();
                        this.mTransport = null;
                        this.mConnected = false;
                        if (D) {
                            Log.d(TAG, "Obex Transport Closed");
                        }
                    } catch (IOException e3) {
                        Log.e(TAG, "mTransport.close error: " + e3.getMessage());
                    }
                }
            } else if (this.mClientSession != null) {
            }
        }
    }

    public synchronized void shutdown() {
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

    public synchronized void handleRegistration(int i, int i2) {
        if (D) {
            Log.d(TAG, "handleRegistration( " + i + ", " + i2 + ")");
        }
        boolean z = true;
        if (i2 == 0) {
            this.mRegisteredMasIds.delete(i);
            if (this.mMnsLstRegRqst != null && this.mMnsLstRegRqst.lastMasId == i) {
                this.mMnsLstRegRqst = null;
            }
        } else if (i2 == 1) {
            if (!isConnected()) {
                if (D) {
                    Log.d(TAG, "handleRegistration: connect");
                }
                connect();
            }
            boolean zIsConnected = isConnected();
            this.mRegisteredMasIds.put(i, true);
            this.mMnsLstRegRqst = null;
            z = zIsConnected;
        }
        if (this.mRegisteredMasIds.size() == 0) {
            if (D) {
                Log.d(TAG, "handleRegistration: disconnect");
            }
            disconnect();
        }
        if (this.mCallback != null && z) {
            Message messageObtain = Message.obtain(this.mCallback);
            messageObtain.what = 5008;
            messageObtain.arg1 = i;
            messageObtain.arg2 = i2;
            messageObtain.sendToTarget();
        }
    }

    public boolean isValidMnsRecord() {
        return this.mMnsRecord != null;
    }

    public void setMnsRecord(SdpMnsRecord sdpMnsRecord) {
        if (isValidMnsRecord()) {
            Log.w(TAG, "MNS Record already available. Still update.");
        }
        this.mMnsRecord = sdpMnsRecord;
        if (this.mMnsLstRegRqst != null) {
            this.mMnsLstRegRqst.setIsSearchInProgress(false);
            if (this.mHandler.hasMessages(1)) {
                this.mHandler.removeMessages(1);
                if (!isValidMnsRecord()) {
                    this.mMnsLstRegRqst = null;
                    return;
                }
                Message messageObtainMessage = this.mHandler.obtainMessage(1);
                messageObtainMessage.arg1 = this.mMnsLstRegRqst.lastMasId;
                messageObtainMessage.arg2 = this.mMnsLstRegRqst.lastNotificationStatus;
                this.mHandler.sendMessageDelayed(messageObtainMessage, 10L);
            }
        }
    }

    public void connect() {
        BluetoothSocket bluetoothSocketCreateInsecureRfcommSocketToServiceRecord;
        boolean z = true;
        this.mConnected = true;
        try {
            if (isValidMnsRecord() && this.mMnsRecord.getL2capPsm() > 0) {
                bluetoothSocketCreateInsecureRfcommSocketToServiceRecord = this.mRemoteDevice.createL2capSocket(this.mMnsRecord.getL2capPsm());
            } else if (isValidMnsRecord() && this.mMnsRecord.getRfcommChannelNumber() > 0) {
                bluetoothSocketCreateInsecureRfcommSocketToServiceRecord = this.mRemoteDevice.createRfcommSocket(this.mMnsRecord.getRfcommChannelNumber());
            } else {
                Log.e(TAG, "Invalid SDP content - attempt a connect to UUID...");
                bluetoothSocketCreateInsecureRfcommSocketToServiceRecord = this.mRemoteDevice.createInsecureRfcommSocketToServiceRecord(BLUETOOTH_UUID_OBEX_MNS.getUuid());
            }
            bluetoothSocketCreateInsecureRfcommSocketToServiceRecord.connect();
            this.mTransport = new BluetoothObexTransport(bluetoothSocketCreateInsecureRfcommSocketToServiceRecord);
            try {
                this.mClientSession = new ClientSession(this.mTransport);
            } catch (IOException e) {
                Log.e(TAG, "OBEX session create error " + e.getMessage());
                this.mConnected = false;
            }
            if (this.mConnected && this.mClientSession != null) {
                HeaderSet headerSet = new HeaderSet();
                headerSet.setHeader(70, new byte[]{-69, 88, 43, 65, 66, 12, 17, -37, -80, -34, 8, 0, 32, 12, -102, 102});
                synchronized (this) {
                    this.mWaitingForRemote = true;
                }
                try {
                    this.mHsConnect = this.mClientSession.connect(headerSet);
                    if (D) {
                        Log.d(TAG, "OBEX session created");
                    }
                } catch (IOException e2) {
                    Log.e(TAG, "OBEX session connect error " + e2.getMessage());
                    z = false;
                }
                this.mConnected = z;
            }
            synchronized (this) {
                this.mWaitingForRemote = false;
            }
        } catch (IOException e3) {
            Log.e(TAG, "BtSocket Connect error " + e3.getMessage(), e3);
            this.mConnected = false;
        }
    }

    public void sendEvent(byte[] bArr, int i) {
        Message messageObtainMessage;
        if (this.mHandler != null && (messageObtainMessage = this.mHandler.obtainMessage(2, i, 0, bArr)) != null) {
            messageObtainMessage.sendToTarget();
        }
        notifyUpdateWakeLock();
    }

    private void notifyMnsSdpSearch() {
        if (this.mCallback != null) {
            Message messageObtain = Message.obtain(this.mCallback);
            messageObtain.what = SapService.MSG_CHANGE_STATE;
            messageObtain.sendToTarget();
        }
    }

    private int sendEventHandler(byte[] bArr, int i) throws Throwable {
        String str;
        StringBuilder sb;
        int i2;
        ClientOperation clientOperation = this.mClientSession;
        int i3 = -1;
        if (!this.mConnected || clientOperation == null) {
            Log.w(TAG, "sendEvent after disconnect:" + this.mConnected);
            return -1;
        }
        HeaderSet headerSet = new HeaderSet();
        BluetoothMapAppParams bluetoothMapAppParams = new BluetoothMapAppParams();
        bluetoothMapAppParams.setMasInstanceId(i);
        OutputStream outputStreamOpenOutputStream = null;
        int i4 = 0;
        try {
            try {
                try {
                    headerSet.setHeader(66, TYPE_EVENT);
                    headerSet.setHeader(76, bluetoothMapAppParams.encodeParams());
                    if (this.mHsConnect.mConnectionID != null) {
                        headerSet.mConnectionID = new byte[4];
                        System.arraycopy(this.mHsConnect.mConnectionID, 0, headerSet.mConnectionID, 0, 4);
                    } else {
                        Log.w(TAG, "sendEvent: no connection ID");
                    }
                    synchronized (this) {
                        this.mWaitingForRemote = true;
                    }
                    try {
                        clientOperation = (ClientOperation) clientOperation.put(headerSet);
                        i2 = 0;
                    } catch (IOException e) {
                        Log.e(TAG, "Error when put HeaderSet " + e.getMessage());
                        clientOperation = null;
                        i2 = 1;
                    }
                    try {
                        try {
                            synchronized (this) {
                                this.mWaitingForRemote = false;
                            }
                            if (i2 == 0) {
                                try {
                                    outputStreamOpenOutputStream = clientOperation.openOutputStream();
                                } catch (IOException e2) {
                                    Log.e(TAG, "Error when opening OutputStream " + e2.getMessage());
                                    i2 = 1;
                                }
                            }
                            if (i2 == 0) {
                                int maxPacketSize = clientOperation.getMaxPacketSize();
                                while (i4 < bArr.length) {
                                    int iMin = Math.min(maxPacketSize, bArr.length - i4);
                                    outputStreamOpenOutputStream.write(bArr, i4, iMin);
                                    i4 += iMin;
                                }
                                if (i4 == bArr.length) {
                                    Log.i(TAG, "SendEvent finished send length" + bArr.length);
                                } else {
                                    try {
                                        clientOperation.abort();
                                        Log.i(TAG, "SendEvent interrupted");
                                        i2 = 1;
                                    } catch (IOException e3) {
                                        e = e3;
                                        handleSendException(e.toString());
                                        if (outputStreamOpenOutputStream != null) {
                                            try {
                                                outputStreamOpenOutputStream.close();
                                            } catch (IOException e4) {
                                                Log.e(TAG, "Error when closing stream after send " + e4.getMessage());
                                            }
                                        }
                                        if (clientOperation != null) {
                                            try {
                                                clientOperation.close();
                                            } catch (IOException e5) {
                                                e = e5;
                                                str = TAG;
                                                sb = new StringBuilder();
                                                sb.append("Error when closing stream after send ");
                                                sb.append(e.getMessage());
                                                Log.e(str, sb.toString());
                                            }
                                        }
                                    } catch (IndexOutOfBoundsException e6) {
                                        e = e6;
                                        handleSendException(e.toString());
                                        if (outputStreamOpenOutputStream != null) {
                                            try {
                                                outputStreamOpenOutputStream.close();
                                            } catch (IOException e7) {
                                                Log.e(TAG, "Error when closing stream after send " + e7.getMessage());
                                            }
                                        }
                                        if (clientOperation != null) {
                                            try {
                                                clientOperation.close();
                                            } catch (IOException e8) {
                                                e = e8;
                                                str = TAG;
                                                sb = new StringBuilder();
                                                sb.append("Error when closing stream after send ");
                                                sb.append(e.getMessage());
                                                Log.e(str, sb.toString());
                                            }
                                        }
                                    }
                                }
                            }
                            if (outputStreamOpenOutputStream != null) {
                                try {
                                    outputStreamOpenOutputStream.close();
                                } catch (IOException e9) {
                                    Log.e(TAG, "Error when closing stream after send " + e9.getMessage());
                                }
                            }
                            if (i2 == 0 && clientOperation != null) {
                                try {
                                    int responseCode = clientOperation.getResponseCode();
                                    if (responseCode != -1 && responseCode != 160) {
                                        try {
                                            Log.i(TAG, "Response error code is " + responseCode);
                                        } catch (IOException e10) {
                                            e = e10;
                                            i3 = responseCode;
                                            Log.e(TAG, "Error when closing stream after send " + e.getMessage());
                                        }
                                    }
                                    i3 = responseCode;
                                } catch (IOException e11) {
                                    e = e11;
                                }
                            }
                            if (clientOperation != null) {
                                clientOperation.close();
                            }
                        } catch (IOException e12) {
                            e = e12;
                        }
                    } catch (IndexOutOfBoundsException e13) {
                        e = e13;
                    } catch (Throwable th) {
                        th = th;
                        i4 = i2;
                        if (0 != 0) {
                            try {
                                outputStreamOpenOutputStream.close();
                            } catch (IOException e14) {
                                Log.e(TAG, "Error when closing stream after send " + e14.getMessage());
                            }
                        }
                        if (i4 == 0 && clientOperation != null) {
                            try {
                                int responseCode2 = clientOperation.getResponseCode();
                                if (responseCode2 != -1 && responseCode2 != 160) {
                                    Log.i(TAG, "Response error code is " + responseCode2);
                                }
                            } catch (IOException e15) {
                                Log.e(TAG, "Error when closing stream after send " + e15.getMessage());
                                throw th;
                            }
                        }
                        if (clientOperation == null) {
                            throw th;
                        }
                        clientOperation.close();
                        throw th;
                    }
                } catch (IOException e16) {
                    e = e16;
                    clientOperation = null;
                }
            } catch (IndexOutOfBoundsException e17) {
                e = e17;
                clientOperation = null;
            } catch (Throwable th2) {
                th = th2;
                clientOperation = null;
            }
            return i3;
        } catch (Throwable th3) {
            th = th3;
            i4 = 1;
        }
    }

    private void handleSendException(String str) {
        Log.e(TAG, "Error when sending event: " + str);
    }

    private void notifyUpdateWakeLock() {
        if (this.mCallback != null) {
            Message messageObtain = Message.obtain(this.mCallback);
            messageObtain.what = SapService.MSG_ACQUIRE_WAKE_LOCK;
            messageObtain.sendToTarget();
        }
    }
}
