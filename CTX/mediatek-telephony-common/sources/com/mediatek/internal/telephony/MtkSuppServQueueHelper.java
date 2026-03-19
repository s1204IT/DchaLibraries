package com.mediatek.internal.telephony;

import android.content.Context;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.telephony.Rlog;
import com.android.internal.telephony.Phone;
import java.util.Vector;

public class MtkSuppServQueueHelper {
    private static final boolean DBG = true;
    private static final int EVENT_SS_RESPONSE = 2;
    private static final int EVENT_SS_SEND = 1;
    public static final String LOG_TAG = "SuppServQueueHelper";
    private static MtkSuppServQueueHelper instance = null;
    private static Object pausedSync = new Object();
    private Context mContext;
    private SuppServQueueHelperHandler mHandler;
    private Phone[] mPhones;

    private MtkSuppServQueueHelper(Context context, Phone[] phoneArr) {
        this.mContext = context;
        this.mPhones = phoneArr;
    }

    public static MtkSuppServQueueHelper makeSuppServQueueHelper(Context context, Phone[] phoneArr) {
        if (context == null || phoneArr == null) {
            return null;
        }
        if (instance == null) {
            Rlog.d(LOG_TAG, "Create MtkSuppServQueueHelper singleton instance, phones.length = " + phoneArr.length);
            instance = new MtkSuppServQueueHelper(context, phoneArr);
        } else {
            instance.mContext = context;
            instance.mPhones = phoneArr;
        }
        return instance;
    }

    public void init(Looper looper) {
        Rlog.d(LOG_TAG, "Initialize SuppServQueueHelper!");
        this.mHandler = new SuppServQueueHelperHandler(looper);
    }

    public void dispose() {
        Rlog.d(LOG_TAG, "dispose.");
    }

    class SuppServQueueHelperHandler extends Handler implements Runnable {
        private boolean paused;
        private Vector<Message> requestBuffer;

        public SuppServQueueHelperHandler(Looper looper) {
            super(looper);
            this.requestBuffer = new Vector<>();
            this.paused = false;
        }

        @Override
        public void run() {
        }

        @Override
        public void handleMessage(Message message) {
            Rlog.d(MtkSuppServQueueHelper.LOG_TAG, "handleMessage(), msg.what = " + message.what + " , paused = " + this.paused);
            switch (message.what) {
                case 1:
                    synchronized (MtkSuppServQueueHelper.pausedSync) {
                        if (this.paused) {
                            Rlog.d(MtkSuppServQueueHelper.LOG_TAG, "A SS request ongoing, add it into the queue");
                            Message message2 = new Message();
                            message2.copyFrom(message);
                            this.requestBuffer.add(message2);
                        } else {
                            processRequest(message.obj, message.arg1);
                            this.paused = true;
                        }
                        break;
                    }
                    return;
                case 2:
                    synchronized (MtkSuppServQueueHelper.pausedSync) {
                        processResponse(message.obj);
                        this.paused = false;
                        if (this.requestBuffer.size() > 0) {
                            Message messageElementAt = this.requestBuffer.elementAt(0);
                            this.requestBuffer.removeElementAt(0);
                            sendMessage(messageElementAt);
                        }
                        break;
                    }
                    return;
                default:
                    Rlog.d(MtkSuppServQueueHelper.LOG_TAG, "handleMessage(), msg.what must be SEND or RESPONSE");
                    return;
            }
        }

        private void processRequest(Object obj, int i) {
            MtkSuppSrvRequest mtkSuppSrvRequest = (MtkSuppSrvRequest) obj;
            mtkSuppSrvRequest.mParcel.setDataPosition(0);
            Message messageObtainMessage = MtkSuppServQueueHelper.this.mHandler.obtainMessage(2, mtkSuppSrvRequest);
            Rlog.d(MtkSuppServQueueHelper.LOG_TAG, "processRequest(), ss.mRequestCode = " + mtkSuppSrvRequest.mRequestCode + ", ss.mResultCallback = " + mtkSuppSrvRequest.mResultCallback + ", phoneId = " + i);
            int i2 = mtkSuppSrvRequest.mRequestCode;
            if (i2 != 18) {
                switch (i2) {
                    case 3:
                        MtkSuppServQueueHelper.this.mPhones[i].setOutgoingCallerIdDisplayInternal(mtkSuppSrvRequest.mParcel.readInt(), messageObtainMessage);
                        break;
                    case 4:
                        MtkSuppServQueueHelper.this.mPhones[i].getOutgoingCallerIdDisplayInternal(messageObtainMessage);
                        break;
                    default:
                        switch (i2) {
                            case 9:
                                MtkSuppServQueueHelper.this.mPhones[i].setCallBarringInternal(mtkSuppSrvRequest.mParcel.readString(), mtkSuppSrvRequest.mParcel.readInt() == 1, mtkSuppSrvRequest.mParcel.readString(), messageObtainMessage, mtkSuppSrvRequest.mParcel.readInt());
                                break;
                            case 10:
                                MtkSuppServQueueHelper.this.mPhones[i].getCallBarringInternal(mtkSuppSrvRequest.mParcel.readString(), mtkSuppSrvRequest.mParcel.readString(), messageObtainMessage, mtkSuppSrvRequest.mParcel.readInt());
                                break;
                            case 11:
                                MtkSuppServQueueHelper.this.mPhones[i].setCallForwardingOptionInternal(mtkSuppSrvRequest.mParcel.readInt(), mtkSuppSrvRequest.mParcel.readInt(), mtkSuppSrvRequest.mParcel.readString(), mtkSuppSrvRequest.mParcel.readInt(), mtkSuppSrvRequest.mParcel.readInt(), messageObtainMessage);
                                break;
                            case 12:
                                MtkSuppServQueueHelper.this.mPhones[i].getCallForwardingOptionInternal(mtkSuppSrvRequest.mParcel.readInt(), mtkSuppSrvRequest.mParcel.readInt(), messageObtainMessage);
                                break;
                            case 13:
                                MtkSuppServQueueHelper.this.mPhones[i].setCallWaitingInternal(mtkSuppSrvRequest.mParcel.readInt() == 1, messageObtainMessage);
                                break;
                            case 14:
                                MtkSuppServQueueHelper.this.mPhones[i].getCallWaitingInternal(messageObtainMessage);
                                break;
                            default:
                                Rlog.d(MtkSuppServQueueHelper.LOG_TAG, "processRequest(), no match mRequestCode");
                                break;
                        }
                        break;
                }
            }
            int i3 = mtkSuppSrvRequest.mParcel.readInt();
            int i4 = mtkSuppSrvRequest.mParcel.readInt();
            MtkSuppServHelper suppServHelper = MtkSuppServManager.getSuppServHelper(i);
            if (suppServHelper != null) {
                suppServHelper.queryCallForwardingOption(i3, i4 == 1, messageObtainMessage);
            }
        }

        private void processResponse(Object obj) {
            AsyncResult asyncResult = (AsyncResult) obj;
            MtkSuppSrvRequest mtkSuppSrvRequest = (MtkSuppSrvRequest) asyncResult.userObj;
            Message message = mtkSuppSrvRequest.mResultCallback;
            Rlog.d(MtkSuppServQueueHelper.LOG_TAG, "processResponse, resp = " + message + " , ar.result = " + asyncResult.result + " , ar.exception = " + asyncResult.exception);
            if (message != null) {
                AsyncResult.forMessage(message, asyncResult.result, asyncResult.exception);
                message.sendToTarget();
            }
            mtkSuppSrvRequest.setResultCallback(null);
            mtkSuppSrvRequest.mParcel.recycle();
        }
    }

    private void addRequest(MtkSuppSrvRequest mtkSuppSrvRequest, int i) {
        if (this.mHandler != null) {
            this.mHandler.obtainMessage(1, i, 0, mtkSuppSrvRequest).sendToTarget();
        }
    }

    public void getCallForwardingOptionForServiceClass(int i, int i2, Message message, int i3) {
        MtkSuppSrvRequest mtkSuppSrvRequestObtain = MtkSuppSrvRequest.obtain(12, message);
        mtkSuppSrvRequestObtain.mParcel.writeInt(i);
        mtkSuppSrvRequestObtain.mParcel.writeInt(i2);
        addRequest(mtkSuppSrvRequestObtain, i3);
    }

    public void setCallForwardingOptionForServiceClass(int i, int i2, String str, int i3, int i4, Message message, int i5) {
        MtkSuppSrvRequest mtkSuppSrvRequestObtain = MtkSuppSrvRequest.obtain(11, message);
        mtkSuppSrvRequestObtain.mParcel.writeInt(i);
        mtkSuppSrvRequestObtain.mParcel.writeInt(i2);
        mtkSuppSrvRequestObtain.mParcel.writeString(str);
        mtkSuppSrvRequestObtain.mParcel.writeInt(i3);
        mtkSuppSrvRequestObtain.mParcel.writeInt(i4);
        addRequest(mtkSuppSrvRequestObtain, i5);
    }

    public void getCallWaiting(Message message, int i) {
        addRequest(MtkSuppSrvRequest.obtain(14, message), i);
    }

    public void setCallWaiting(boolean z, Message message, int i) {
        MtkSuppSrvRequest mtkSuppSrvRequestObtain = MtkSuppSrvRequest.obtain(13, message);
        mtkSuppSrvRequestObtain.mParcel.writeInt(z ? 1 : 0);
        addRequest(mtkSuppSrvRequestObtain, i);
    }

    public void getCallBarring(String str, String str2, int i, Message message, int i2) {
        MtkSuppSrvRequest mtkSuppSrvRequestObtain = MtkSuppSrvRequest.obtain(10, message);
        mtkSuppSrvRequestObtain.mParcel.writeString(str);
        mtkSuppSrvRequestObtain.mParcel.writeString(str2);
        mtkSuppSrvRequestObtain.mParcel.writeInt(i);
        addRequest(mtkSuppSrvRequestObtain, i2);
    }

    public void setCallBarring(String str, boolean z, String str2, int i, Message message, int i2) {
        MtkSuppSrvRequest mtkSuppSrvRequestObtain = MtkSuppSrvRequest.obtain(9, message);
        mtkSuppSrvRequestObtain.mParcel.writeString(str);
        mtkSuppSrvRequestObtain.mParcel.writeInt(z ? 1 : 0);
        mtkSuppSrvRequestObtain.mParcel.writeString(str2);
        mtkSuppSrvRequestObtain.mParcel.writeInt(i);
        addRequest(mtkSuppSrvRequestObtain, i2);
    }

    public void getOutgoingCallerIdDisplay(Message message, int i) {
        addRequest(MtkSuppSrvRequest.obtain(4, message), i);
    }

    public void setOutgoingCallerIdDisplay(int i, Message message, int i2) {
        MtkSuppSrvRequest mtkSuppSrvRequestObtain = MtkSuppSrvRequest.obtain(3, message);
        mtkSuppSrvRequestObtain.mParcel.writeInt(i);
        addRequest(mtkSuppSrvRequestObtain, i2);
    }

    public void getCallForwardingOption(int i, int i2, Message message, int i3) {
        MtkSuppSrvRequest mtkSuppSrvRequestObtain = MtkSuppSrvRequest.obtain(18, message);
        mtkSuppSrvRequestObtain.mParcel.writeInt(i);
        mtkSuppSrvRequestObtain.mParcel.writeInt(i2);
        addRequest(mtkSuppSrvRequestObtain, i3);
    }
}
