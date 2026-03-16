package com.android.services.telephony.sip;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.util.Log;

public class SipBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (SipUtil.isVoipSupported(context)) {
            SipAccountRegistry sipAccountRegistry = SipAccountRegistry.getInstance();
            if (action.equals("com.android.phone.SIP_INCOMING_CALL")) {
                takeCall(context, intent);
                return;
            }
            if (action.equals("android.net.sip.SIP_SERVICE_UP") || action.equals("com.android.phone.SIP_CALL_OPTION_CHANGED")) {
                sipAccountRegistry.setup(context);
            } else if (action.equals("com.android.phone.SIP_REMOVE_PHONE")) {
                sipAccountRegistry.removeSipProfile(intent.getStringExtra("android:localSipUri"));
            }
        }
    }

    private void takeCall(Context context, Intent intent) {
        PhoneAccountHandle accountHandle = (PhoneAccountHandle) intent.getParcelableExtra("com.android.services.telephony.sip.phone_account");
        if (accountHandle != null) {
            Bundle extras = new Bundle();
            extras.putParcelable("com.android.services.telephony.sip.incoming_call_intent", intent);
            TelecomManager tm = TelecomManager.from(context);
            PhoneAccount phoneAccount = tm.getPhoneAccount(accountHandle);
            if (phoneAccount != null) {
                tm.addNewIncomingCall(accountHandle, extras);
            } else {
                log("takeCall, PhoneAccount is not registered. Not accepting incoming call...");
            }
        }
    }

    private static void log(String msg) {
        Log.d("SIP", "[SipBroadcastReceiver] " + msg);
    }
}
