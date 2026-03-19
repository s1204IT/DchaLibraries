package com.mediatek.internal.telephony;

import android.content.Context;
import com.android.internal.telephony.SmsUsageMonitor;

public class MtkSmsUsageMonitor extends SmsUsageMonitor {
    private static final String[] SKIP_SEND_LIMIT_PACKAGES = {"com.android.mms", "com.mediatek.autotest"};
    private static final String TAG = "MtkSmsUsageMonitor";

    public MtkSmsUsageMonitor(Context context) {
        super(context);
    }

    public boolean check(String str, int i) {
        for (String str2 : SKIP_SEND_LIMIT_PACKAGES) {
            if (str.equals(str2)) {
                return true;
            }
        }
        return super.check(str, i);
    }
}
