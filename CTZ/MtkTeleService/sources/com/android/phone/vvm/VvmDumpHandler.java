package com.android.phone.vvm;

import android.content.Context;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.telephony.TelephonyManager;
import com.android.internal.util.IndentingPrintWriter;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.Iterator;

public class VvmDumpHandler {
    public static void dump(Context context, FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        TelephonyManager telephonyManagerFrom = TelephonyManager.from(context);
        IndentingPrintWriter indentingPrintWriter = new IndentingPrintWriter(printWriter, "  ");
        indentingPrintWriter.println("******* OmtpVvm *******");
        indentingPrintWriter.println("======= Configs =======");
        indentingPrintWriter.increaseIndent();
        Iterator<PhoneAccountHandle> it = TelecomManager.from(context).getCallCapablePhoneAccounts().iterator();
        while (it.hasNext()) {
            int subId = PhoneAccountHandleConverter.toSubId(it.next());
            indentingPrintWriter.println("VisualVoicemailPackageName:" + telephonyManagerFrom.createForSubscriptionId(subId).getVisualVoicemailPackageName());
            indentingPrintWriter.println("VisualVoicemailSmsFilterSettings(" + subId + "):" + telephonyManagerFrom.getActiveVisualVoicemailSmsFilterSettings(subId));
        }
        indentingPrintWriter.decreaseIndent();
        indentingPrintWriter.println("======== Logs =========");
        VvmLog.dump(fileDescriptor, indentingPrintWriter, strArr);
    }
}
