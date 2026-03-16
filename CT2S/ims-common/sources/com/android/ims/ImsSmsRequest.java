package com.android.ims;

import android.os.AsyncResult;
import android.os.Message;
import android.os.Parcel;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

class ImsSmsRequest {
    static final String LOG_TAG = "ImsSmsRequest";
    private static final int MAX_POOL_SIZE = 4;
    ImsSmsRequest mNext;
    Parcel mParcel;
    int mRequest;
    Message mResult;
    int mSerial;
    static Random sRandom = new Random();
    static AtomicInteger sNextSerial = new AtomicInteger(0);
    private static Object sPoolSync = new Object();
    private static ImsSmsRequest sPool = null;
    private static int sPoolSize = 0;

    static ImsSmsRequest obtain(int request, Message result) {
        ImsSmsRequest rr = null;
        synchronized (sPoolSync) {
            if (sPool != null) {
                rr = sPool;
                sPool = rr.mNext;
                rr.mNext = null;
                sPoolSize--;
            }
        }
        if (rr == null) {
            rr = new ImsSmsRequest();
        }
        rr.mSerial = sNextSerial.getAndIncrement();
        rr.mRequest = request;
        rr.mResult = result;
        rr.mParcel = Parcel.obtain();
        if (result != null && result.getTarget() == null) {
            throw new NullPointerException("Message target must not be null");
        }
        return rr;
    }

    void release() {
        synchronized (sPoolSync) {
            if (sPoolSize < 4) {
                this.mNext = sPool;
                sPool = this;
                sPoolSize++;
                this.mResult = null;
            }
        }
    }

    private ImsSmsRequest() {
    }

    static void resetSerial() {
        sNextSerial.set(sRandom.nextInt());
    }

    String serialString() {
        StringBuilder sb = new StringBuilder(8);
        long adjustedSerial = (((long) this.mSerial) - (-2147483648L)) % 10000;
        String sn = Long.toString(adjustedSerial);
        sb.append('[');
        int s = sn.length();
        for (int i = 0; i < 4 - s; i++) {
            sb.append('0');
        }
        sb.append(sn);
        sb.append(']');
        return sb.toString();
    }

    void onError(int error, Object ret) {
        ImsException ex = new ImsException("SMS over IMS error", ((ImsSmsResult) ret).mErrorCode);
        if (this.mResult != null) {
            AsyncResult.forMessage(this.mResult, ret, ex);
            this.mResult.sendToTarget();
        }
        if (this.mParcel != null) {
            this.mParcel.recycle();
            this.mParcel = null;
        }
    }
}
