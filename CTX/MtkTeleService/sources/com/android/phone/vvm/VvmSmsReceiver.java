package com.android.phone.vvm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.SubscriptionManager;
import android.telephony.VisualVoicemailSms;

public class VvmSmsReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        VisualVoicemailSms visualVoicemailSms = (VisualVoicemailSms) intent.getExtras().getParcelable("android.provider.extra.VOICEMAIL_SMS");
        if (visualVoicemailSms.getPhoneAccountHandle() == null) {
            VvmLog.e("VvmSmsReceiver", "Received message for null phone account");
            return;
        }
        int subId = PhoneAccountHandleConverter.toSubId(visualVoicemailSms.getPhoneAccountHandle());
        if (!SubscriptionManager.isValidSubscriptionId(subId)) {
            VvmLog.e("VvmSmsReceiver", "Received message for invalid subId");
            return;
        }
        String string = intent.getExtras().getString("android.provider.extra.TARGET_PACAKGE");
        if (RemoteVvmTaskManager.hasRemoteService(context, subId, string)) {
            VvmLog.i("VvmSmsReceiver", "Sending SMS received event to remote service");
            RemoteVvmTaskManager.startSmsReceived(context, visualVoicemailSms, string);
        } else {
            VvmLog.w("VvmSmsReceiver", "No remote service to handle SMS received event");
        }
    }
}
