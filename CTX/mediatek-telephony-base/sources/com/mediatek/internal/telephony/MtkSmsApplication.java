package com.mediatek.internal.telephony;

import android.content.ComponentName;
import android.content.Context;
import android.os.Build;
import android.telephony.Rlog;
import android.telephony.SmsManager;
import com.android.internal.telephony.SmsApplication;

public final class MtkSmsApplication {
    private static final boolean DBG = "eng".equals(Build.TYPE);
    private static final String LOG_TAG = "MtkSmsApplication";

    public static ComponentName getDefaultSmsApplication(Context context, boolean z, int i) {
        ComponentName componentName;
        try {
            SmsApplication.SmsApplicationData application = SmsApplication.getApplication(context, z, i);
            if (application != null) {
                componentName = new ComponentName(application.mPackageName, application.mSmsReceiverClass);
            } else {
                componentName = null;
            }
            if (DBG) {
                Rlog.d(LOG_TAG, "getDefaultSmsApplication for userId " + i + " default component= " + componentName);
            }
            return componentName;
        } catch (Throwable th) {
            if (DBG) {
                Rlog.d(LOG_TAG, "getDefaultSmsApplication for userId " + i + " default component= " + ((Object) null));
            }
            throw th;
        }
    }

    private static String getDefaultSmsApplicationPackageName(Context context, int i) {
        ComponentName defaultSmsApplication = getDefaultSmsApplication(context, false, i);
        if (defaultSmsApplication != null) {
            return defaultSmsApplication.getPackageName();
        }
        return null;
    }

    public static boolean isDefaultSmsApplication(Context context, String str, int i) {
        if (str == null) {
            return false;
        }
        String defaultSmsApplicationPackageName = getDefaultSmsApplicationPackageName(context, i);
        if ((defaultSmsApplicationPackageName != null && defaultSmsApplicationPackageName.equals(str)) || "com.android.bluetooth".equals(str)) {
            return true;
        }
        if (SmsApplication.mSmsDbVisitorList == null) {
            SmsApplication.loadSmsDbVisitor();
        }
        if (SmsApplication.mSmsDbVisitorList != null) {
            for (int i2 = 0; i2 < SmsApplication.mSmsDbVisitorList.size(); i2++) {
                if (str.equals(SmsApplication.mSmsDbVisitorList.get(i2))) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean shouldWriteMessageForPackage(String str, Context context, int i) {
        if (SmsManager.getDefault().getAutoPersisting()) {
            return true;
        }
        boolean z = !isDefaultSmsApplication(context, str, i);
        if (DBG) {
            Rlog.d(LOG_TAG, "shouldWriteMessageForPackage for userId " + i + ", shouldWrite=" + z);
        }
        return z;
    }
}
