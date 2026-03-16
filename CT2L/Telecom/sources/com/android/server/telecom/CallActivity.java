package com.android.server.telecom;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.telecom.TelecomManager;
import android.telephony.PhoneNumberUtils;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.widget.Toast;
import com.android.internal.telephony.Dsds;

public class CallActivity extends Activity {
    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        processIntent(getIntent());
        finish();
        Log.d(this, "onCreate: end", new Object[0]);
    }

    private void processIntent(Intent intent) {
        if (isVoiceCapable()) {
            verifyCallAction(intent);
            String action = intent.getAction();
            if ("android.intent.action.CALL".equals(action) || "android.intent.action.CALL_PRIVILEGED".equals(action) || "android.intent.action.CALL_EMERGENCY".equals(action)) {
                processOutgoingCallIntent(intent);
            }
        }
    }

    private void verifyCallAction(Intent intent) {
        if (CallActivity.class.getName().equals(intent.getComponent().getClassName()) && !"android.intent.action.CALL".equals(intent.getAction())) {
            Log.w(this, "Attempt to deliver non-CALL action; forcing to CALL", new Object[0]);
            intent.setAction("android.intent.action.CALL");
        }
    }

    private void processOutgoingCallIntent(Intent intent) {
        Uri uriFromParts;
        Uri data = intent.getData();
        String scheme = data.getScheme();
        String schemeSpecificPart = data.getSchemeSpecificPart();
        if ("voicemail".equals(scheme)) {
            uriFromParts = data;
        } else {
            uriFromParts = Uri.fromParts(PhoneNumberUtils.isUriNumber(schemeSpecificPart) ? "sip" : "tel", schemeSpecificPart, null);
        }
        if (((UserManager) getSystemService("user")).hasUserRestriction("no_outgoing_calls") && !TelephonyUtil.shouldProcessAsEmergency(this, uriFromParts)) {
            Toast.makeText(this, getResources().getString(R.string.outgoing_call_not_allowed), 0).show();
            Log.d(this, "Rejecting non-emergency phone call due to DISALLOW_OUTGOING_CALLS restriction", new Object[0]);
            return;
        }
        if (intent.getIntExtra("android.telecom.extra.START_CALL_WITH_VIDEO_STATE", 0) == 3) {
            int masterSubId = Dsds.getMasterSubId();
            if (SubscriptionManager.isValidSubscriptionId(masterSubId)) {
                TelephonyManager telephonyManager = (TelephonyManager) getSystemService("phone");
                if (isNetworkOfChinaMobile(telephonyManager.getNetworkOperatorForSubscription(masterSubId)) && !telephonyManager.isVideoEnabled()) {
                    Log.w(this, "CMCC request: ims is not registered or vt is not enabled. Cancel this video call.", new Object[0]);
                    Toast.makeText(this, R.string.vt_not_supported, 0).show();
                    return;
                }
            }
        }
        intent.putExtra("is_default_dialer", isDefaultDialer());
        sendBroadcastToReceiver(intent);
    }

    private boolean isNetworkOfChinaMobile(String str) {
        if (str == null || !str.startsWith("460")) {
            return false;
        }
        String strSubstring = str.substring(3);
        if (!strSubstring.equals("00") && !strSubstring.equals("02") && !strSubstring.equals("07") && !strSubstring.equals("08")) {
            return false;
        }
        return true;
    }

    private boolean isDefaultDialer() {
        String callingPackage = getCallingPackage();
        if (TextUtils.isEmpty(callingPackage)) {
            return false;
        }
        ComponentName defaultPhoneApp = ((TelecomManager) getSystemService("telecom")).getDefaultPhoneApp();
        return defaultPhoneApp != null && TextUtils.equals(defaultPhoneApp.getPackageName(), callingPackage);
    }

    private boolean isVoiceCapable() {
        return getApplicationContext().getResources().getBoolean(android.R.^attr-private.externalRouteEnabledDrawable);
    }

    private boolean sendBroadcastToReceiver(Intent intent) {
        intent.putExtra("is_incoming_call", false);
        intent.setFlags(268435456);
        intent.setClass(this, CallReceiver.class);
        Log.d(this, "Sending broadcast as user to CallReceiver", new Object[0]);
        sendBroadcastAsUser(intent, UserHandle.OWNER);
        return true;
    }
}
