package com.android.services.telephony.sip;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.UserHandle;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.util.Log;

public class SipIncomingCallReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (isRunningInSystemUser() && SipUtil.isVoipSupported(context) && action.equals("com.android.phone.SIP_INCOMING_CALL")) {
            takeCall(context, intent);
        }
    }

    private void takeCall(Context context, Intent intent) {
        try {
            PhoneAccountHandle phoneAccountHandle = (PhoneAccountHandle) intent.getParcelableExtra("com.android.services.telephony.sip.phone_account");
            if (phoneAccountHandle != null) {
                Bundle bundle = new Bundle();
                bundle.putParcelable("com.android.services.telephony.sip.incoming_call_intent", intent);
                TelecomManager telecomManagerFrom = TelecomManager.from(context);
                PhoneAccount phoneAccount = telecomManagerFrom.getPhoneAccount(phoneAccountHandle);
                if (phoneAccount != null && phoneAccount.isEnabled()) {
                    telecomManagerFrom.addNewIncomingCall(phoneAccountHandle, bundle);
                } else {
                    log("takeCall, PhoneAccount is disabled. Not accepting incoming call...");
                }
            }
        } catch (ClassCastException e) {
            log("takeCall, Bad account handle detected. Bailing!");
        }
    }

    private boolean isRunningInSystemUser() {
        return UserHandle.myUserId() == 0;
    }

    private static void log(String str) {
        Log.d("SIP", "[SipIncomingCallReceiver] " + str);
    }
}
