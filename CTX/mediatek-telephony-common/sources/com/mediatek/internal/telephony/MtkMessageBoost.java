package com.mediatek.internal.telephony;

import android.hardware.radio.V1_0.RadioResponseInfo;
import android.os.AsyncResult;
import android.os.Message;
import android.os.SystemClock;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.RIL;
import com.android.internal.telephony.RILRequest;
import java.util.ArrayList;

public class MtkMessageBoost {
    protected static final int MESSAGE_BOOT_TIME_MSEC = 10000;
    private static MtkMessageBoost sMtkMessageBoost;
    MtkRIL mMtkRil;
    protected int mPriorityFlag = 0;

    public static MtkMessageBoost init(MtkRIL mtkRIL) {
        MtkMessageBoost mtkMessageBoost;
        synchronized (MtkMessageBoost.class) {
            if (sMtkMessageBoost == null) {
                sMtkMessageBoost = new MtkMessageBoost(mtkRIL);
            }
            mtkMessageBoost = sMtkMessageBoost;
        }
        return mtkMessageBoost;
    }

    public static MtkMessageBoost getInstance() {
        MtkMessageBoost mtkMessageBoost;
        synchronized (MtkMessageBoost.class) {
            mtkMessageBoost = sMtkMessageBoost;
        }
        return mtkMessageBoost;
    }

    public MtkMessageBoost(MtkRIL mtkRIL) {
        this.mMtkRil = mtkRIL;
    }

    public void setPriorityFlag(int i, Phone phone) {
        this.mPriorityFlag = i | this.mPriorityFlag;
    }

    public void clearPriorityFlag(int i) {
        this.mPriorityFlag = (~i) & this.mPriorityFlag;
    }

    public int getPriorityFlag(int i) {
        return i & this.mPriorityFlag;
    }

    public static void sendMessageResponseWithPriority(Message message, Object obj) {
        if (message != null) {
            long jUptimeMillis = SystemClock.uptimeMillis() - 10000;
            AsyncResult.forMessage(message, obj, (Throwable) null);
            message.getTarget().sendMessageAtTime(message, jUptimeMillis);
        }
    }

    public void responseStringsWithPriority(RadioResponseInfo radioResponseInfo, String... strArr) {
        ArrayList<String> arrayList = new ArrayList<>();
        for (String str : strArr) {
            arrayList.add(str);
        }
        responseStringArrayListWithPriority(this.mMtkRil, radioResponseInfo, arrayList);
    }

    public void responseStringArrayListWithPriority(RIL ril, RadioResponseInfo radioResponseInfo, ArrayList<String> arrayList) {
        RILRequest rILRequestProcessResponse = ril.processResponse(radioResponseInfo);
        if (rILRequestProcessResponse != null) {
            String[] strArr = new String[arrayList.size()];
            for (int i = 0; i < arrayList.size(); i++) {
                strArr[i] = arrayList.get(i);
            }
            if (radioResponseInfo.error == 0) {
                sendMessageResponseWithPriority(rILRequestProcessResponse.mResult, strArr);
            }
            ril.processResponseDone(rILRequestProcessResponse, radioResponseInfo, strArr);
        }
    }

    public void responseIntsWithPriority(RadioResponseInfo radioResponseInfo, int... iArr) {
        ArrayList<Integer> arrayList = new ArrayList<>();
        for (int i : iArr) {
            arrayList.add(Integer.valueOf(i));
        }
        responseIntArrayListWithPriority(radioResponseInfo, arrayList);
    }

    public void responseIntArrayListWithPriority(RadioResponseInfo radioResponseInfo, ArrayList<Integer> arrayList) {
        RILRequest rILRequestProcessResponse = this.mMtkRil.processResponse(radioResponseInfo);
        if (rILRequestProcessResponse != null) {
            int[] iArr = new int[arrayList.size()];
            for (int i = 0; i < arrayList.size(); i++) {
                iArr[i] = arrayList.get(i).intValue();
            }
            if (radioResponseInfo.error == 0) {
                sendMessageResponseWithPriority(rILRequestProcessResponse.mResult, iArr);
            }
            this.mMtkRil.processResponseDone(rILRequestProcessResponse, radioResponseInfo, iArr);
        }
    }
}
