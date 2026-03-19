package com.android.bluetooth.sap;

import android.hardware.radio.V1_0.ISap;
import android.hardware.radio.V1_0.ISapCallback;
import android.os.Handler;
import android.os.IHwBinder;
import android.os.RemoteException;
import android.util.Log;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class SapRilReceiver {
    public static final boolean DEBUG = true;
    public static final int RIL_MAX_COMMAND_BYTES = 8192;
    private static final String SERVICE_NAME_RIL_BT = "slot1";
    private static final int SOCKET_OPEN_RETRY_MILLIS = 4000;
    private static final String TAG = "SapRilReceiver";
    public static final boolean VERBOSE = true;
    volatile ISap mSapProxy;
    private Handler mSapServerMsgHandler;
    private Handler mSapServiceHandler;
    Object mSapProxyLock = new Object();
    final AtomicLong mSapProxyCookie = new AtomicLong(0);
    public byte[] buffer = new byte[8192];
    SapCallback mSapCallback = new SapCallback();
    final SapProxyDeathRecipient mSapProxyDeathRecipient = new SapProxyDeathRecipient();

    final class SapProxyDeathRecipient implements IHwBinder.DeathRecipient {
        SapProxyDeathRecipient() {
        }

        public void serviceDied(long j) {
            Log.d(SapRilReceiver.TAG, "serviceDied");
            SapRilReceiver.this.mSapServerMsgHandler.sendMessageDelayed(SapRilReceiver.this.mSapServerMsgHandler.obtainMessage(5, Long.valueOf(j)), 3000L);
        }
    }

    private void sendSapMessage(SapMessage sapMessage) {
        if (sapMessage.getMsgType() < 256) {
            sendClientMessage(sapMessage);
        } else {
            sendRilIndMessage(sapMessage);
        }
    }

    private void removeOngoingReqAndSendMessage(int i, SapMessage sapMessage) {
        Integer numRemove = SapMessage.sOngoingRequests.remove(Integer.valueOf(i));
        StringBuilder sb = new StringBuilder();
        sb.append("removeOngoingReqAndSendMessage: token ");
        sb.append(i);
        sb.append(" reqType ");
        sb.append(numRemove == null ? "null" : SapMessage.getMsgTypeName(numRemove.intValue()));
        Log.d(TAG, sb.toString());
        sendSapMessage(sapMessage);
    }

    class SapCallback extends ISapCallback.Stub {
        SapCallback() {
        }

        public void connectResponse(int i, int i2, int i3) {
            Log.d(SapRilReceiver.TAG, "connectResponse: token " + i + " sapConnectRsp " + i2 + " maxMsgSize " + i3);
            SapService.notifyUpdateWakeLock(SapRilReceiver.this.mSapServiceHandler);
            SapMessage sapMessage = new SapMessage(1);
            sapMessage.setConnectionStatus(i2);
            if (i2 == 2) {
                sapMessage.setMaxMsgSize(i3);
            }
            sapMessage.setResultCode(-1);
            SapRilReceiver.this.removeOngoingReqAndSendMessage(i, sapMessage);
        }

        public void disconnectResponse(int i) {
            Log.d(SapRilReceiver.TAG, "disconnectResponse: token " + i);
            SapService.notifyUpdateWakeLock(SapRilReceiver.this.mSapServiceHandler);
            SapMessage sapMessage = new SapMessage(3);
            sapMessage.setResultCode(-1);
            SapRilReceiver.this.removeOngoingReqAndSendMessage(i, sapMessage);
        }

        public void disconnectIndication(int i, int i2) {
            Log.d(SapRilReceiver.TAG, "disconnectIndication: token " + i + " disconnectType " + i2);
            SapService.notifyUpdateWakeLock(SapRilReceiver.this.mSapServiceHandler);
            SapMessage sapMessage = new SapMessage(SapMessage.ID_RIL_UNSOL_DISCONNECT_IND);
            sapMessage.setDisconnectionType(i2);
            SapRilReceiver.this.sendSapMessage(sapMessage);
        }

        public void apduResponse(int i, int i2, ArrayList<Byte> arrayList) {
            Log.d(SapRilReceiver.TAG, "apduResponse: token " + i);
            SapService.notifyUpdateWakeLock(SapRilReceiver.this.mSapServiceHandler);
            SapMessage sapMessage = new SapMessage(6);
            sapMessage.setResultCode(i2);
            if (i2 == 0) {
                sapMessage.setApduResp(SapRilReceiver.arrayListToPrimitiveArray(arrayList));
            }
            SapRilReceiver.this.removeOngoingReqAndSendMessage(i, sapMessage);
        }

        public void transferAtrResponse(int i, int i2, ArrayList<Byte> arrayList) {
            Log.d(SapRilReceiver.TAG, "transferAtrResponse: token " + i + " resultCode " + i2);
            SapService.notifyUpdateWakeLock(SapRilReceiver.this.mSapServiceHandler);
            SapMessage sapMessage = new SapMessage(8);
            sapMessage.setResultCode(i2);
            if (i2 == 0) {
                sapMessage.setAtr(SapRilReceiver.arrayListToPrimitiveArray(arrayList));
            }
            SapRilReceiver.this.removeOngoingReqAndSendMessage(i, sapMessage);
        }

        public void powerResponse(int i, int i2) {
            SapMessage sapMessage;
            Log.d(SapRilReceiver.TAG, "powerResponse: token " + i + " resultCode " + i2);
            SapService.notifyUpdateWakeLock(SapRilReceiver.this.mSapServiceHandler);
            Integer numRemove = SapMessage.sOngoingRequests.remove(Integer.valueOf(i));
            StringBuilder sb = new StringBuilder();
            sb.append("powerResponse: reqType ");
            sb.append(numRemove == null ? "null" : SapMessage.getMsgTypeName(numRemove.intValue()));
            Log.d(SapRilReceiver.TAG, sb.toString());
            if (numRemove.intValue() == 9) {
                sapMessage = new SapMessage(10);
            } else if (numRemove.intValue() == 11) {
                sapMessage = new SapMessage(12);
            } else {
                return;
            }
            sapMessage.setResultCode(i2);
            SapRilReceiver.this.sendSapMessage(sapMessage);
        }

        public void resetSimResponse(int i, int i2) {
            Log.d(SapRilReceiver.TAG, "resetSimResponse: token " + i + " resultCode " + i2);
            SapService.notifyUpdateWakeLock(SapRilReceiver.this.mSapServiceHandler);
            SapMessage sapMessage = new SapMessage(14);
            sapMessage.setResultCode(i2);
            SapRilReceiver.this.removeOngoingReqAndSendMessage(i, sapMessage);
        }

        public void statusIndication(int i, int i2) {
            Log.d(SapRilReceiver.TAG, "statusIndication: token " + i + " status " + i2);
            SapService.notifyUpdateWakeLock(SapRilReceiver.this.mSapServiceHandler);
            SapMessage sapMessage = new SapMessage(17);
            sapMessage.setStatusChange(i2);
            SapRilReceiver.this.sendSapMessage(sapMessage);
        }

        public void transferCardReaderStatusResponse(int i, int i2, int i3) {
            Log.d(SapRilReceiver.TAG, "transferCardReaderStatusResponse: token " + i + " resultCode " + i2 + " cardReaderStatus " + i3);
            SapService.notifyUpdateWakeLock(SapRilReceiver.this.mSapServiceHandler);
            SapMessage sapMessage = new SapMessage(16);
            sapMessage.setResultCode(i2);
            if (i2 == 0) {
                sapMessage.setCardReaderStatus(i3);
            }
            SapRilReceiver.this.removeOngoingReqAndSendMessage(i, sapMessage);
        }

        public void errorResponse(int i) {
            Log.d(SapRilReceiver.TAG, "errorResponse: token " + i);
            SapService.notifyUpdateWakeLock(SapRilReceiver.this.mSapServiceHandler);
            SapRilReceiver.this.sendSapMessage(new SapMessage(511));
        }

        public void transferProtocolResponse(int i, int i2) {
            Log.d(SapRilReceiver.TAG, "transferProtocolResponse: token " + i + " resultCode " + i2);
            SapService.notifyUpdateWakeLock(SapRilReceiver.this.mSapServiceHandler);
            SapMessage sapMessage = new SapMessage(20);
            sapMessage.setResultCode(i2);
            SapRilReceiver.this.removeOngoingReqAndSendMessage(i, sapMessage);
        }
    }

    public static byte[] arrayListToPrimitiveArray(List<Byte> list) {
        byte[] bArr = new byte[list.size()];
        for (int i = 0; i < bArr.length; i++) {
            bArr[i] = list.get(i).byteValue();
        }
        return bArr;
    }

    public Object getSapProxyLock() {
        return this.mSapProxyLock;
    }

    public ISap getSapProxy() {
        synchronized (this.mSapProxyLock) {
            if (this.mSapProxy != null) {
                return this.mSapProxy;
            }
            try {
                this.mSapProxy = ISap.getService(SERVICE_NAME_RIL_BT);
                if (this.mSapProxy != null) {
                    this.mSapProxy.linkToDeath(this.mSapProxyDeathRecipient, this.mSapProxyCookie.incrementAndGet());
                    this.mSapProxy.setCallback(this.mSapCallback);
                } else {
                    Log.e(TAG, "getSapProxy: mSapProxy == null");
                }
            } catch (RemoteException | RuntimeException e) {
                this.mSapProxy = null;
                Log.e(TAG, "getSapProxy: exception: " + e);
            }
            if (this.mSapProxy == null) {
                this.mSapServerMsgHandler.sendMessageDelayed(this.mSapServerMsgHandler.obtainMessage(5, Long.valueOf(this.mSapProxyCookie.get())), 3000L);
            }
            return this.mSapProxy;
        }
    }

    public void resetSapProxy() {
        synchronized (this.mSapProxyLock) {
            Log.d(TAG, "resetSapProxy :" + this.mSapProxy);
            try {
            } catch (RemoteException | RuntimeException e) {
                Log.e(TAG, "resetSapProxy: exception: " + e);
            }
            if (this.mSapProxy != null) {
                this.mSapProxy.unlinkToDeath(this.mSapProxyDeathRecipient);
                this.mSapProxy = null;
            } else {
                this.mSapProxy = null;
            }
        }
    }

    public SapRilReceiver(Handler handler, Handler handler2) {
        this.mSapProxy = null;
        this.mSapServerMsgHandler = null;
        this.mSapServiceHandler = null;
        this.mSapServerMsgHandler = handler;
        this.mSapServiceHandler = handler2;
        synchronized (this.mSapProxyLock) {
            this.mSapProxy = getSapProxy();
        }
    }

    void notifyShutdown() {
        Log.i(TAG, "notifyShutdown()");
        synchronized (this.mSapProxyLock) {
            if (this.mSapProxy != null) {
                sendShutdownMessage();
            }
        }
    }

    private static int readMessage(InputStream inputStream, byte[] bArr) throws IOException {
        int i = 0;
        int i2 = 4;
        int i3 = 0;
        do {
            int i4 = inputStream.read(bArr, i3, i2);
            if (i4 < 0) {
                Log.e(TAG, "Hit EOS reading message length");
                return -1;
            }
            i3 += i4;
            i2 -= i4;
        } while (i2 > 0);
        int i5 = ((bArr[0] & 255) << 24) | ((bArr[1] & 255) << 16) | ((bArr[2] & 255) << 8) | (bArr[3] & 255);
        Log.e(TAG, "Message length found to be: " + i5);
        int i6 = i5;
        do {
            int i7 = inputStream.read(bArr, i, i6);
            if (i7 < 0) {
                Log.e(TAG, "Hit EOS reading message.  messageLength=" + i5 + " remaining=" + i6);
                return -1;
            }
            i += i7;
            i6 -= i7;
        } while (i6 > 0);
        return i5;
    }

    void sendRilConnectMessage() {
        if (this.mSapServerMsgHandler != null) {
            this.mSapServerMsgHandler.sendEmptyMessage(1);
        }
    }

    private void sendClientMessage(SapMessage sapMessage) {
        this.mSapServerMsgHandler.sendMessage(this.mSapServerMsgHandler.obtainMessage(0, sapMessage));
    }

    private void sendShutdownMessage() {
        if (this.mSapServerMsgHandler != null) {
            this.mSapServerMsgHandler.sendEmptyMessage(4);
        }
    }

    private void sendRilIndMessage(SapMessage sapMessage) {
        this.mSapServerMsgHandler.sendMessage(this.mSapServerMsgHandler.obtainMessage(3, sapMessage));
    }
}
