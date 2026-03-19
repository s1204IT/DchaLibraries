package com.mediatek.internal.telephony.util;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemProperties;
import android.telephony.Rlog;
import com.android.internal.telephony.SMSDispatcher;
import com.mediatek.internal.telephony.ppl.IPplSmsFilter;
import com.mediatek.internal.telephony.ppl.PplSmsFilterExtension;

public final class MtkSmsCommonUtil {
    public static final String IS_EMERGENCY_CB_PRIMARY = "isPrimary";
    public static final String SELECT_BY_REFERENCE = "address=? AND reference_number=? AND count=? AND deleted=0 AND sub_id=?";
    private static final String TAG = "MtkSmsCommonUtil";
    private static final boolean ENG = "eng".equals(Build.TYPE);
    public static PplSmsFilterExtension sPplSmsFilter = null;
    private static final boolean IS_PRIVACY_PROTECTION_LOCK_SUPPORT = SystemProperties.get("ro.vendor.mtk_privacy_protection_lock").equals("1");
    private static final boolean IS_WAPPUSH_SUPPORT = SystemProperties.get("ro.vendor.mtk_wappush_support").equals("1");

    private MtkSmsCommonUtil() {
    }

    public static boolean isPrivacyLockSupport() {
        return IS_PRIVACY_PROTECTION_LOCK_SUPPORT;
    }

    public static boolean isWapPushSupport() {
        return IS_WAPPUSH_SUPPORT;
    }

    public static void filterOutByPpl(Context context, SMSDispatcher.SmsTracker smsTracker) {
        if (!isPrivacyLockSupport()) {
            return;
        }
        if (sPplSmsFilter == null) {
            sPplSmsFilter = new PplSmsFilterExtension(context);
        }
        if (ENG) {
            Rlog.d(TAG, "[PPL] Phone privacy check start");
        }
        Bundle bundle = new Bundle();
        PplSmsFilterExtension pplSmsFilterExtension = sPplSmsFilter;
        bundle.putString(IPplSmsFilter.KEY_MSG_CONTENT, smsTracker.mFullMessageText);
        PplSmsFilterExtension pplSmsFilterExtension2 = sPplSmsFilter;
        bundle.putString(IPplSmsFilter.KEY_DST_ADDR, smsTracker.mDestAddress);
        PplSmsFilterExtension pplSmsFilterExtension3 = sPplSmsFilter;
        bundle.putString(IPplSmsFilter.KEY_FORMAT, smsTracker.mFormat);
        PplSmsFilterExtension pplSmsFilterExtension4 = sPplSmsFilter;
        bundle.putInt(IPplSmsFilter.KEY_SUB_ID, smsTracker.mSubId);
        PplSmsFilterExtension pplSmsFilterExtension5 = sPplSmsFilter;
        bundle.putInt(IPplSmsFilter.KEY_SMS_TYPE, 1);
        boolean zPplFilter = sPplSmsFilter.pplFilter(bundle);
        if (zPplFilter) {
            smsTracker.mPersistMessage = false;
        }
        if (ENG) {
            Rlog.d(TAG, "[PPL] Phone privacy check end, Need to filter(result) = " + zPplFilter);
        }
    }

    public static int phonePrivacyLockCheck(byte[][] bArr, String str, Context context, int i) {
        if (!isPrivacyLockSupport()) {
            return 0;
        }
        if (sPplSmsFilter == null) {
            sPplSmsFilter = new PplSmsFilterExtension(context);
        }
        Bundle bundle = new Bundle();
        PplSmsFilterExtension pplSmsFilterExtension = sPplSmsFilter;
        bundle.putSerializable(IPplSmsFilter.KEY_PDUS, bArr);
        PplSmsFilterExtension pplSmsFilterExtension2 = sPplSmsFilter;
        bundle.putString(IPplSmsFilter.KEY_FORMAT, str);
        PplSmsFilterExtension pplSmsFilterExtension3 = sPplSmsFilter;
        bundle.putInt(IPplSmsFilter.KEY_SUB_ID, i);
        PplSmsFilterExtension pplSmsFilterExtension4 = sPplSmsFilter;
        bundle.putInt(IPplSmsFilter.KEY_SMS_TYPE, 0);
        boolean zPplFilter = sPplSmsFilter.pplFilter(bundle);
        if (ENG) {
            Rlog.d(TAG, "[Ppl] Phone privacy check end, Need to filter(result) = " + zPplFilter);
        }
        return zPplFilter ? -1 : 0;
    }
}
