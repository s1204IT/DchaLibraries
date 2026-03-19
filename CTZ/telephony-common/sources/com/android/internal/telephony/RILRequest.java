package com.android.internal.telephony;

import android.os.AsyncResult;
import android.os.Message;
import android.os.SystemClock;
import android.os.WorkSource;
import android.telephony.Rlog;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

public class RILRequest {
    static final String LOG_TAG = "RilRequest";
    private static final int MAX_POOL_SIZE = 4;
    String mClientId;
    RILRequest mNext;
    public int mRequest;
    public Message mResult;
    public int mSerial;
    long mStartTimeMs;
    int mWakeLockType;
    WorkSource mWorkSource;
    static Random sRandom = new Random();
    static AtomicInteger sNextSerial = new AtomicInteger(0);
    private static Object sPoolSync = new Object();
    private static RILRequest sPool = null;
    private static int sPoolSize = 0;

    public int getSerial() {
        return this.mSerial;
    }

    public int getRequest() {
        return this.mRequest;
    }

    public Message getResult() {
        return this.mResult;
    }

    private static RILRequest obtain(int i, Message message) {
        RILRequest rILRequest;
        synchronized (sPoolSync) {
            if (sPool != null) {
                rILRequest = sPool;
                sPool = rILRequest.mNext;
                rILRequest.mNext = null;
                sPoolSize--;
            } else {
                rILRequest = null;
            }
        }
        if (rILRequest == null) {
            rILRequest = new RILRequest();
        }
        rILRequest.mSerial = sNextSerial.getAndIncrement();
        rILRequest.mRequest = i;
        rILRequest.mResult = message;
        rILRequest.mWakeLockType = -1;
        rILRequest.mWorkSource = null;
        rILRequest.mStartTimeMs = SystemClock.elapsedRealtime();
        if (message != null && message.getTarget() == null) {
            throw new NullPointerException("Message target must not be null");
        }
        return rILRequest;
    }

    public static RILRequest obtain(int i, Message message, WorkSource workSource) {
        RILRequest rILRequestObtain = obtain(i, message);
        if (workSource != null) {
            rILRequestObtain.mWorkSource = workSource;
            rILRequestObtain.mClientId = rILRequestObtain.getWorkSourceClientId();
        } else {
            Rlog.e(LOG_TAG, "null workSource " + i);
        }
        return rILRequestObtain;
    }

    public String getWorkSourceClientId() {
        if (this.mWorkSource == null || this.mWorkSource.isEmpty()) {
            return null;
        }
        if (this.mWorkSource.size() > 0) {
            return this.mWorkSource.get(0) + ":" + this.mWorkSource.getName(0);
        }
        ArrayList workChains = this.mWorkSource.getWorkChains();
        if (workChains == null || workChains.isEmpty()) {
            return null;
        }
        WorkSource.WorkChain workChain = (WorkSource.WorkChain) workChains.get(0);
        return workChain.getAttributionUid() + ":" + workChain.getTags()[0];
    }

    void release() {
        synchronized (sPoolSync) {
            if (sPoolSize < 4) {
                this.mNext = sPool;
                sPool = this;
                sPoolSize++;
                this.mResult = null;
                if (this.mWakeLockType != -1 && this.mWakeLockType == 0) {
                    Rlog.e(LOG_TAG, "RILRequest releasing with held wake lock: " + serialString());
                }
            }
        }
    }

    private RILRequest() {
    }

    static void resetSerial() {
        sNextSerial.set(sRandom.nextInt());
    }

    public String serialString() {
        StringBuilder sb = new StringBuilder(8);
        String string = Long.toString((((long) this.mSerial) - (-2147483648L)) % 10000);
        sb.append('[');
        int length = string.length();
        for (int i = 0; i < 4 - length; i++) {
            sb.append('0');
        }
        sb.append(string);
        sb.append(']');
        return sb.toString();
    }

    void onError(int i, Object obj) {
        CommandException commandExceptionFromRilErrno = CommandException.fromRilErrno(i);
        Rlog.d(LOG_TAG, serialString() + "< " + RIL.requestToString(this.mRequest) + " error: " + commandExceptionFromRilErrno + " ret=" + RIL.retToString(this.mRequest, obj));
        if (this.mResult != null) {
            AsyncResult.forMessage(this.mResult, obj, commandExceptionFromRilErrno);
            this.mResult.sendToTarget();
        }
    }
}
