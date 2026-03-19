package com.mediatek.internal.telephony;

import android.content.Context;
import android.os.Build;
import android.os.ServiceManager;
import android.telephony.Rlog;
import android.text.TextUtils;
import com.mediatek.internal.telephony.IMtkSub;

public class MtkSubscriptionControllerEx extends IMtkSub.Stub {
    private static final boolean DBG = true;
    private static final String LOG_TAG = "MtkSubscriptionControllerEx";
    private Context mContext;
    private static final boolean ENGDEBUG = TextUtils.equals(Build.TYPE, "eng");
    private static MtkSubscriptionControllerEx sInstance = null;

    protected MtkSubscriptionControllerEx(Context context) {
        this.mContext = context;
        if (ServiceManager.getService("isubstub") == null) {
            ServiceManager.addService("isubstub", this);
            Rlog.d(LOG_TAG, "[MtkSubscriptionControllerEx] init by Context, this = " + this);
        }
        Rlog.d(LOG_TAG, "[MtkSubscriptionControllerEx] init by Context");
    }

    protected static void MtkInitStub(Context context) {
        synchronized (MtkSubscriptionControllerEx.class) {
            if (sInstance == null) {
                sInstance = new MtkSubscriptionControllerEx(context);
                Rlog.d(LOG_TAG, "[MtkSubscriptionControllerEx] sInstance = " + sInstance);
            } else {
                Rlog.w(LOG_TAG, "init() called multiple times!  sInstance = " + sInstance);
            }
        }
    }

    public MtkSubscriptionInfo getSubInfo(String str, int i) {
        return MtkSubscriptionController.getMtkInstance().getSubscriptionInfo(str, i);
    }

    public MtkSubscriptionInfo getSubInfoForIccId(String str, String str2) {
        return MtkSubscriptionController.getMtkInstance().getSubscriptionInfoForIccId(str, str2);
    }

    public int getSubIdUsingPhoneId(int i) {
        return MtkSubscriptionController.getMtkInstance().getSubIdUsingPhoneId(i);
    }

    public void setDefaultFallbackSubId(int i) {
        MtkSubscriptionController.getMtkInstance().setDefaultFallbackSubId(i);
    }

    public void setDefaultDataSubIdWithoutCapabilitySwitch(int i) {
        MtkSubscriptionController.getMtkInstance().setDefaultDataSubIdWithoutCapabilitySwitch(i);
    }
}
