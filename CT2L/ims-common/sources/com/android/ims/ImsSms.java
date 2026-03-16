package com.android.ims;

import android.content.Context;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.Registrant;
import android.os.RemoteException;
import android.telephony.Rlog;
import android.util.SparseArray;
import com.android.ims.internal.IImsSms;
import com.android.ims.internal.IImsSmsListener;

public class ImsSms implements ImsSmsInterface {
    private static final boolean DBG = true;
    private static final int DEFAULT_WAKE_LOCK_TIMEOUT = 120000;
    static final int EVENT_SEND = 10;
    static final int EVENT_WAKE_LOCK_TIMEOUT = 11;
    static final int IMS_REQUEST_ACKNOWLEDGE_INCOMING_GSM_SMS_WITH_PDU = 5;
    static final int IMS_REQUEST_CDMA_SEND_SMS = 2;
    static final int IMS_REQUEST_CDMA_SMS_ACKNOWLEDGE = 4;
    static final int IMS_REQUEST_SEND_SMS = 1;
    static final int IMS_REQUEST_SMS_ACKNOWLEDGE = 3;
    private static final String TAG = "ImsSms";
    public Registrant mImsCdmaSmsRegistrant;
    public Registrant mImsGsmSmsRegistrant;
    private final IImsSms mImsSms;
    public Registrant mImsSmsStatusRegistrant;
    SparseArray<ImsSmsRequest> mRequestList = new SparseArray<>();
    ImsSmsSender mSender;
    HandlerThread mSenderThread;
    PowerManager.WakeLock mWakeLock;
    final int mWakeLockTimeout;

    static String requestToString(int request) {
        switch (request) {
            case 1:
                return "IMS_REQUEST_SEND_SMS";
            case 2:
                return "IMS_REQUEST_CDMA_SEND_SMS";
            case 3:
                return "IMS_REQUEST_SMS_ACKNOWLEDGE";
            case 4:
                return "IMS_REQUEST_CDMA_SMS_ACKNOWLEDGE";
            case 5:
                return "IMS_REQUEST_ACKNOWLEDGE_INCOMING_GSM_SMS_WITH_PDU";
            default:
                return "unexpect request";
        }
    }

    class ImsSmsSender extends Handler implements Runnable {
        public ImsSmsSender(Looper looper) {
            super(looper);
        }

        @Override
        public void run() {
        }

        @Override
        public void handleMessage(Message msg) {
            ImsSmsRequest rr = (ImsSmsRequest) msg.obj;
            switch (msg.what) {
                case 10:
                    try {
                        synchronized (ImsSms.this.mRequestList) {
                            ImsSms.this.mRequestList.append(rr.mSerial, rr);
                            break;
                        }
                        int length = 0;
                        rr.mParcel.setDataPosition(0);
                        switch (rr.mRequest) {
                            case 1:
                                length = rr.mParcel.readInt();
                                int retry = rr.mParcel.readInt();
                                int messageRef = rr.mParcel.readInt();
                                String smscPDU = rr.mParcel.readString();
                                String pdu = rr.mParcel.readString();
                                ImsSms.this.mImsSms.sendImsGsmSms(rr.mSerial, smscPDU, pdu, retry, messageRef);
                                break;
                            case 2:
                                length = rr.mParcel.readInt();
                                int retry2 = rr.mParcel.readInt();
                                int messageRef2 = rr.mParcel.readInt();
                                byte[] pdu_data = new byte[length - 2];
                                rr.mParcel.readByteArray(pdu_data);
                                ImsSms.this.mImsSms.sendImsCdmaSms(rr.mSerial, pdu_data, retry2, messageRef2);
                                break;
                            case 3:
                                length = rr.mParcel.readInt();
                                boolean success = rr.mParcel.readInt() == 0 ? false : ImsSms.DBG;
                                int cause = rr.mParcel.readInt();
                                ImsSms.this.mImsSms.acknowledgeLastIncomingGsmSms(rr.mSerial, success, cause);
                                break;
                            case 4:
                                length = rr.mParcel.readInt();
                                boolean success2 = rr.mParcel.readInt() == 0 ? false : ImsSms.DBG;
                                int cause2 = rr.mParcel.readInt();
                                ImsSms.this.mImsSms.acknowledgeLastIncomingCdmaSms(rr.mSerial, success2, cause2);
                                break;
                            case 5:
                                length = rr.mParcel.readInt();
                                boolean success3 = rr.mParcel.readString().equals("0") ? false : ImsSms.DBG;
                                String ackPdu = rr.mParcel.readString();
                                ImsSms.this.mImsSms.acknowledgeIncomingGsmSmsWithPdu(rr.mSerial, success3, ackPdu);
                                break;
                        }
                        ImsSms.this.log("parcel length = " + length);
                        rr.mParcel.recycle();
                        rr.mParcel = null;
                        return;
                    } catch (RemoteException e) {
                        ImsSmsRequest req = ImsSms.this.findAndRemoveRequestFromList(rr.mSerial);
                        if (req != null) {
                            rr.release();
                            ImsSms.this.decrementWakeLock();
                            return;
                        }
                        return;
                    } catch (RuntimeException e2) {
                        ImsSmsRequest req2 = ImsSms.this.findAndRemoveRequestFromList(rr.mSerial);
                        if (req2 != null) {
                            rr.release();
                            ImsSms.this.decrementWakeLock();
                            return;
                        }
                        return;
                    }
                case 11:
                    if (ImsSms.this.clearWakeLock()) {
                        ImsSms.this.removeRequestList();
                        return;
                    }
                    return;
                default:
                    return;
            }
        }
    }

    public ImsSms(Context context, IImsSms iImsSms) {
        log("ImsSms created");
        this.mImsSms = iImsSms;
        PowerManager pm = (PowerManager) context.getSystemService("power");
        this.mWakeLock = pm.newWakeLock(1, TAG);
        this.mWakeLock.setReferenceCounted(false);
        this.mWakeLockTimeout = DEFAULT_WAKE_LOCK_TIMEOUT;
        if (this.mImsSms != null) {
            try {
                this.mImsSms.setListener(new IImsSmsListenerProxy());
            } catch (RemoteException e) {
            }
        }
        this.mSenderThread = new HandlerThread("ImsSmsSender");
        this.mSenderThread.start();
        Looper looper = this.mSenderThread.getLooper();
        this.mSender = new ImsSmsSender(looper);
    }

    @Override
    public void sendImsGsmSms(String smscPDU, String pdu, int retry, int messageRef, Message result) {
        ImsSmsRequest rr = ImsSmsRequest.obtain(1, result);
        rr.mParcel.writeInt(4);
        rr.mParcel.writeInt(retry);
        rr.mParcel.writeInt(messageRef);
        rr.mParcel.writeString(smscPDU);
        rr.mParcel.writeString(pdu);
        log(rr.serialString() + "> " + requestToString(rr.mRequest));
        send(rr);
    }

    @Override
    public void sendImsCdmaSms(byte[] pdu, int retry, int messageRef, Message result) {
        ImsSmsRequest rr = ImsSmsRequest.obtain(2, result);
        rr.mParcel.writeInt(pdu.length + 2);
        rr.mParcel.writeInt(retry);
        rr.mParcel.writeInt(messageRef);
        rr.mParcel.writeByteArray(pdu);
        log(rr.serialString() + "> " + requestToString(rr.mRequest));
        send(rr);
    }

    @Override
    public void acknowledgeLastIncomingGsmSms(boolean success, int cause, Message result) {
        ImsSmsRequest rr = ImsSmsRequest.obtain(3, result);
        rr.mParcel.writeInt(2);
        rr.mParcel.writeInt(success ? 1 : 0);
        rr.mParcel.writeInt(cause);
        log(rr.serialString() + "> " + requestToString(rr.mRequest) + " " + success + " " + cause);
        send(rr);
    }

    @Override
    public void acknowledgeLastIncomingCdmaSms(boolean success, int cause, Message result) {
        ImsSmsRequest rr = ImsSmsRequest.obtain(4, result);
        rr.mParcel.writeInt(success ? 0 : 1);
        rr.mParcel.writeInt(cause);
        log(rr.serialString() + "> " + requestToString(rr.mRequest) + " " + success + " " + cause);
        send(rr);
    }

    @Override
    public void acknowledgeIncomingGsmSmsWithPdu(boolean success, String ackPdu, Message result) {
        ImsSmsRequest rr = ImsSmsRequest.obtain(5, result);
        rr.mParcel.writeInt(2);
        rr.mParcel.writeString(success ? "1" : "0");
        rr.mParcel.writeString(ackPdu);
        log(rr.serialString() + "> " + requestToString(rr.mRequest) + ' ' + success + " [" + ackPdu + ']');
        send(rr);
    }

    private void log(String s) {
        Rlog.d(TAG, s);
    }

    private void acquireWakeLock() {
        synchronized (this.mWakeLock) {
            this.mWakeLock.acquire();
            this.mSender.removeMessages(11);
            Message msg = this.mSender.obtainMessage(11);
            log("acaquireWakeLock");
            this.mSender.sendMessageDelayed(msg, this.mWakeLockTimeout);
        }
    }

    private void decrementWakeLock() {
        synchronized (this.mWakeLock) {
            this.mWakeLock.release();
            this.mSender.removeMessages(11);
        }
    }

    private boolean clearWakeLock() {
        boolean z;
        synchronized (this.mWakeLock) {
            if (!this.mWakeLock.isHeld()) {
                z = false;
            } else {
                log("at time of clearing");
                this.mWakeLock.release();
                this.mSender.removeMessages(11);
                z = DBG;
            }
        }
        return z;
    }

    private void send(ImsSmsRequest rr) {
        Message msg = this.mSender.obtainMessage(10, rr);
        acquireWakeLock();
        msg.sendToTarget();
    }

    private ImsSmsRequest findAndRemoveRequestFromList(int serial) {
        ImsSmsRequest rr;
        synchronized (this.mRequestList) {
            rr = this.mRequestList.get(serial);
            if (rr != null) {
                this.mRequestList.remove(serial);
            }
        }
        return rr;
    }

    private void removeRequestList() {
        synchronized (this.mRequestList) {
            int count = this.mRequestList.size();
            log("WAKE_LOCK_TIMEOUT  mRequestList=" + count);
            for (int i = 0; i < count; i++) {
                ImsSmsRequest rr = this.mRequestList.valueAt(i);
                if (rr != null) {
                    log(i + ": [" + rr.mSerial + "] ");
                    rr.release();
                }
            }
        }
    }

    private class IImsSmsListenerProxy extends IImsSmsListener.Stub {
        private IImsSmsListenerProxy() {
        }

        public void onResponseSMSReceived(int serial, int error, ImsSmsResult result) throws RemoteException {
            ImsSmsRequest rr = ImsSms.this.findAndRemoveRequestFromList(serial);
            if (rr == null) {
                ImsSms.this.log("Unexpected response! sn: " + serial);
                return;
            }
            ImsSms.this.log(rr.serialString() + "< " + ImsSms.requestToString(rr.mRequest) + " " + ImsSms.requestToString(rr.mRequest));
            if (error == 0) {
                if (rr.mResult != null) {
                    AsyncResult.forMessage(rr.mResult, result, (Throwable) null);
                    rr.mResult.sendToTarget();
                }
            } else {
                rr.onError(error, result);
            }
            if (rr != null) {
                rr.release();
                ImsSms.this.decrementWakeLock();
            }
        }

        public void onNewCdmaSms(String message) throws RemoteException {
            ImsSms.this.log("[IMSUNSL]< NewImsCdmaSms " + message);
            if (ImsSms.this.mImsCdmaSmsRegistrant != null) {
                ImsSms.this.mImsCdmaSmsRegistrant.notifyRegistrant(new AsyncResult((Object) null, message, (Throwable) null));
            }
        }

        public void onNewGsmSms(String message) throws RemoteException {
            ImsSms.this.log("[IMSUNSL]< NewImsGsmSms " + message);
            if (ImsSms.this.mImsGsmSmsRegistrant != null) {
                ImsSms.this.mImsGsmSmsRegistrant.notifyRegistrant(new AsyncResult((Object) null, message, (Throwable) null));
            }
        }

        public void onNewGsmSmsStatusReport(String message) throws RemoteException {
            ImsSms.this.log("[IMSUNSL]< NewImsGsmSmsStatusReport " + message);
            if (ImsSms.this.mImsSmsStatusRegistrant != null) {
                ImsSms.this.mImsSmsStatusRegistrant.notifyRegistrant(new AsyncResult((Object) null, message, (Throwable) null));
            }
        }
    }

    public void setOnNewImsGsmSms(Handler h, int what, Object obj) {
        this.mImsGsmSmsRegistrant = new Registrant(h, what, obj);
    }

    public void unSetOnNewImsGsmSms(Handler h) {
        if (this.mImsGsmSmsRegistrant != null && this.mImsGsmSmsRegistrant.getHandler() == h) {
            this.mImsGsmSmsRegistrant.clear();
            this.mImsGsmSmsRegistrant = null;
        }
    }

    public void setOnNewImsCdmaSms(Handler h, int what, Object obj) {
        this.mImsCdmaSmsRegistrant = new Registrant(h, what, obj);
    }

    public void unSetOnNewImsCdmaSms(Handler h) {
        if (this.mImsCdmaSmsRegistrant != null && this.mImsCdmaSmsRegistrant.getHandler() == h) {
            this.mImsCdmaSmsRegistrant.clear();
            this.mImsCdmaSmsRegistrant = null;
        }
    }

    public void setOnImsSmsStatus(Handler h, int what, Object obj) {
        this.mImsSmsStatusRegistrant = new Registrant(h, what, obj);
    }

    public void unSetOnImsSmsStatus(Handler h) {
        if (this.mImsSmsStatusRegistrant != null && this.mImsSmsStatusRegistrant.getHandler() == h) {
            this.mImsSmsStatusRegistrant.clear();
            this.mImsSmsStatusRegistrant = null;
        }
    }
}
