package com.android.internal.telephony;

import android.telephony.Rlog;
import android.text.TextUtils;
import com.android.internal.telephony.metrics.TelephonyMetrics;
import java.io.FileDescriptor;
import java.io.PrintWriter;

public class DebugService {
    private static String TAG = "DebugService";

    public DebugService() {
        log("DebugService:");
    }

    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        if (strArr != null && strArr.length > 0 && (TextUtils.equals(strArr[0], "--metrics") || TextUtils.equals(strArr[0], "--metricsproto"))) {
            log("Collecting telephony metrics..");
            TelephonyMetrics.getInstance().dump(fileDescriptor, printWriter, strArr);
        } else {
            log("Dump telephony.");
            PhoneFactory.dump(fileDescriptor, printWriter, strArr);
        }
    }

    private static void log(String str) {
        Rlog.d(TAG, "DebugService " + str);
    }
}
