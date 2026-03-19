package com.mediatek.internal.telephony;

import android.os.ServiceManager;
import android.telephony.Rlog;
import com.mediatek.internal.telephony.IMtkTelephonyRegistryEx;

public class MtkTelephonyRegistryEx extends IMtkTelephonyRegistryEx.Stub {
    private static final boolean DBG = false;
    private static final boolean DBG_LOC = false;
    private static final String LOG_TAG = "MtkTelephonyRegistryEx";
    private static final boolean VDBG = false;
    private static MtkTelephonyRegistryEx sInstance;

    static MtkTelephonyRegistryEx init() {
        MtkTelephonyRegistryEx mtkTelephonyRegistryEx;
        synchronized (MtkTelephonyRegistryEx.class) {
            if (sInstance == null) {
                sInstance = new MtkTelephonyRegistryEx();
            } else {
                Rlog.e(LOG_TAG, "init() called multiple times!  sInstance = " + sInstance);
            }
            mtkTelephonyRegistryEx = sInstance;
        }
        return mtkTelephonyRegistryEx;
    }

    protected MtkTelephonyRegistryEx() {
        publish();
    }

    private void publish() {
        Rlog.d(LOG_TAG, "publish: " + this);
        ServiceManager.addService("telephony.mtkregistry", this);
    }

    @Override
    public void notifyCallStateForPhoneInfo(int i, int i2, int i3, int i4, String str) {
    }

    private static boolean idMatchEx(int i, int i2, int i3, int i4, int i5) {
        Rlog.d(LOG_TAG, "idMatchEx: rSubId=" + i + ", subId=" + i2 + ", dSubId=" + i3 + ", rPhoneId=" + i4 + ", phoneId=" + i5);
        return i2 < 0 ? i4 == i5 : i == Integer.MAX_VALUE ? i2 == i3 : i == i2;
    }
}
