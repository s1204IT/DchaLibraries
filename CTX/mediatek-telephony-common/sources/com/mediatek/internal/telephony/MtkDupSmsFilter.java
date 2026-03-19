package com.mediatek.internal.telephony;

import android.content.Context;
import android.os.SystemProperties;
import android.util.Log;
import com.mediatek.internal.telephony.ppl.PplMessageManager;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class MtkDupSmsFilter implements IMtkDupSmsFilter {
    protected static final long DEFAULT_DUP_SMS_KEEP_PERIOD = 300000;
    protected static final int EVENT_CLEAR_SMS_LIST = 1;
    protected static final String KEY_DUP_SMS_KEEP_PERIOD = "dev.dup_sms_keep_period";
    protected Context mContext;
    protected int mPhoneId = -1;
    protected HashMap<Long, byte[]> mSmsMap;
    private static String TAG = "MtkDupSmsFilter";
    private static boolean DBG = SystemProperties.get("ro.build.type").equals("eng");

    public MtkDupSmsFilter(Context context) {
        this.mContext = null;
        this.mSmsMap = null;
        Log.d("@M_" + TAG, "call constructor");
        if (context == null) {
            Log.d("@M_" + TAG, "FAIL! context is null");
            return;
        }
        this.mContext = context;
        this.mSmsMap = new HashMap<>();
    }

    @Override
    public void setPhoneId(int i) {
        this.mPhoneId = i;
    }

    @Override
    public boolean containDupSms(byte[] bArr) {
        Log.d("@M_" + TAG, "call containDupSms");
        if (isTestIccCard()) {
            return false;
        }
        removeExpiredItem();
        Iterator<Map.Entry<Long, byte[]>> it = this.mSmsMap.entrySet().iterator();
        while (it.hasNext()) {
            if (isDupSms(bArr, it.next().getValue())) {
                Log.d("@M_" + TAG, "find a duplicated sms");
                return true;
            }
        }
        synchronized (this.mSmsMap) {
            this.mSmsMap.put(Long.valueOf(System.currentTimeMillis()), bArr);
        }
        Log.d("@M_" + TAG, "Not duplicated sms");
        return false;
    }

    protected boolean isDupSms(byte[] bArr, byte[] bArr2) {
        if (bArr == null || bArr2 == null) {
            Log.d("@M_" + TAG, "Return false directly because either newPdu or oldPdu is null.");
            return false;
        }
        byte[] bArr3 = (byte[]) bArr.clone();
        byte[] bArr4 = (byte[]) bArr2.clone();
        int firstBytePosition = getFirstBytePosition(bArr3);
        int firstBytePosition2 = getFirstBytePosition(bArr4);
        if (DBG) {
            Log.d("@M_" + TAG, "newClonePdu " + Arrays.toString(bArr3) + ", newFirstBytePos " + firstBytePosition + ", oldClonePdu " + Arrays.toString(bArr4) + ", oldFirstBytePos " + firstBytePosition2);
        }
        bArr3[firstBytePosition] = (byte) (bArr3[firstBytePosition] | 4);
        bArr4[firstBytePosition2] = (byte) (bArr4[firstBytePosition2] | 4);
        if (DBG) {
            Log.d("@M_" + TAG, "Mark TP-MMS as 1, newPdu first byte: " + ((int) bArr[firstBytePosition]) + "->" + ((int) bArr3[firstBytePosition]) + ", oldPdu first byte: " + ((int) bArr2[firstBytePosition2]) + "->" + ((int) bArr4[firstBytePosition2]));
        }
        if (!Arrays.equals(bArr3, bArr4)) {
            return false;
        }
        return true;
    }

    private synchronized void removeExpiredItem() {
        Log.d("@M_" + TAG, "call removeExpiredItem");
        long j = SystemProperties.getLong(KEY_DUP_SMS_KEEP_PERIOD, DEFAULT_DUP_SMS_KEEP_PERIOD);
        Iterator<Map.Entry<Long, byte[]>> it = this.mSmsMap.entrySet().iterator();
        while (it.hasNext()) {
            if (it.next().getKey().longValue() < System.currentTimeMillis() - j) {
                it.remove();
            }
        }
        Log.d("@M_" + TAG, "mSmsMap has " + this.mSmsMap.size() + " items after removeExpiredItem");
    }

    private boolean isTestIccCard() {
        int i = -1;
        if (this.mPhoneId == 0) {
            i = SystemProperties.getInt("vendor.gsm.sim.ril.testsim", -1);
        } else if (this.mPhoneId == 1) {
            i = SystemProperties.getInt("vendor.gsm.sim.ril.testsim.2", -1);
        } else if (this.mPhoneId == 2) {
            i = SystemProperties.getInt("vendor.gsm.sim.ril.testsim.3", -1);
        } else if (this.mPhoneId == 3) {
            i = SystemProperties.getInt("vendor.gsm.sim.ril.testsim.4", -1);
        }
        Log.d("@M_" + TAG, "Phone id: " + this.mPhoneId + "isTestIccCard: " + i);
        return i == 1;
    }

    private int getFirstBytePosition(byte[] bArr) {
        int i = bArr[0] & PplMessageManager.Type.INVALID;
        if (i == 0) {
            return 1;
        }
        return 1 + i;
    }
}
