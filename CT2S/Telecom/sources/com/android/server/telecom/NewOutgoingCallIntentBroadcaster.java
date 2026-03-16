package com.android.server.telecom;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Trace;
import android.os.UserHandle;
import android.telecom.GatewayInfo;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;

class NewOutgoingCallIntentBroadcaster {
    private final Call mCall;
    private final CallsManager mCallsManager;
    private final Context mContext;
    private final Intent mIntent;
    private final boolean mIsDefaultOrSystemPhoneApp;

    NewOutgoingCallIntentBroadcaster(Context context, CallsManager callsManager, Call call, Intent intent, boolean z) {
        this.mContext = context;
        this.mCallsManager = callsManager;
        this.mCall = call;
        this.mIntent = intent;
        this.mIsDefaultOrSystemPhoneApp = z;
    }

    private class NewOutgoingCallBroadcastIntentReceiver extends BroadcastReceiver {
        private NewOutgoingCallBroadcastIntentReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            boolean z;
            Trace.beginSection("onReceiveNewOutgoingCallBroadcast");
            Log.v(this, "onReceive: %s", intent);
            String resultData = getResultData();
            Log.v(this, "- got number from resultData: %s", Log.pii(resultData));
            if (resultData != null) {
                if (PhoneNumberUtils.isPotentialLocalEmergencyNumber(NewOutgoingCallIntentBroadcaster.this.mContext, resultData)) {
                    Log.w(this, "Cannot modify outgoing call to emergency number %s.", resultData);
                    z = true;
                } else {
                    z = false;
                }
            } else {
                Log.v(this, "Call cancelled (null number), returning...", new Object[0]);
                z = true;
            }
            if (z) {
                if (NewOutgoingCallIntentBroadcaster.this.mCall != null) {
                    NewOutgoingCallIntentBroadcaster.this.mCall.disconnect(true);
                }
                Trace.endSection();
                return;
            }
            Uri uriFromParts = Uri.fromParts(PhoneNumberUtils.isUriNumber(resultData) ? "sip" : "tel", resultData, null);
            Uri data = NewOutgoingCallIntentBroadcaster.this.mIntent.getData();
            if (data.getSchemeSpecificPart().equals(resultData)) {
                Log.v(this, "Call number unmodified after new outgoing call intent broadcast.", new Object[0]);
            } else {
                Log.v(this, "Retrieved modified handle after outgoing call intent broadcast: Original: %s, Modified: %s", Log.pii(data), Log.pii(uriFromParts));
            }
            NewOutgoingCallIntentBroadcaster.this.mCallsManager.placeOutgoingCall(NewOutgoingCallIntentBroadcaster.this.mCall, uriFromParts, NewOutgoingCallIntentBroadcaster.getGateWayInfoFromIntent(intent, uriFromParts), NewOutgoingCallIntentBroadcaster.this.mIntent.getBooleanExtra("android.telecom.extra.START_CALL_WITH_SPEAKERPHONE", false), NewOutgoingCallIntentBroadcaster.this.mIntent.getIntExtra("android.telecom.extra.START_CALL_WITH_VIDEO_STATE", 0));
            Trace.endSection();
        }
    }

    int processIntent() {
        boolean z;
        Log.v(this, "Processing call intent in OutgoingCallIntentBroadcaster.", new Object[0]);
        Intent intent = this.mIntent;
        String action = intent.getAction();
        Uri data = intent.getData();
        if (data == null) {
            Log.w(this, "Empty handle obtained from the call intent.", new Object[0]);
            return 7;
        }
        if ("voicemail".equals(data.getScheme())) {
            if ("android.intent.action.CALL".equals(action) || "android.intent.action.CALL_PRIVILEGED".equals(action)) {
                Log.i(this, "Placing call immediately instead of waiting for  OutgoingCallBroadcastReceiver: %s", intent);
                this.mCallsManager.placeOutgoingCall(this.mCall, data, null, this.mIntent.getBooleanExtra("android.telecom.extra.START_CALL_WITH_SPEAKERPHONE", false), 0);
                return 0;
            }
            Log.i(this, "Unhandled intent %s. Ignoring and not placing call.", intent);
            return 44;
        }
        String numberFromIntent = PhoneNumberUtils.getNumberFromIntent(intent, this.mContext);
        if (TextUtils.isEmpty(numberFromIntent)) {
            Log.w(this, "Empty number obtained from the call intent.", new Object[0]);
            return 38;
        }
        boolean zIsUriNumber = PhoneNumberUtils.isUriNumber(numberFromIntent);
        if (!zIsUriNumber) {
            numberFromIntent = PhoneNumberUtils.stripSeparators(PhoneNumberUtils.convertKeypadLettersToDigits(numberFromIntent));
        }
        boolean zIsPotentialEmergencyNumber = isPotentialEmergencyNumber(numberFromIntent);
        Log.v(this, "isPotentialEmergencyNumber = %s", Boolean.valueOf(zIsPotentialEmergencyNumber));
        rewriteCallIntentAction(intent, zIsPotentialEmergencyNumber);
        String action2 = intent.getAction();
        if ("android.intent.action.CALL".equals(action2)) {
            if (!zIsPotentialEmergencyNumber) {
                z = false;
            } else {
                if (!this.mIsDefaultOrSystemPhoneApp) {
                    Log.w(this, "Cannot call potential emergency number %s with CALL Intent %s unless caller is system or default dialer.", numberFromIntent, intent);
                    launchSystemDialer(intent.getData());
                    return 44;
                }
                z = true;
            }
        } else if ("android.intent.action.CALL_EMERGENCY".equals(action2)) {
            if (!zIsPotentialEmergencyNumber) {
                Log.w(this, "Cannot call non-potential-emergency number %s with EMERGENCY_CALL Intent %s.", numberFromIntent, intent);
                return 44;
            }
            z = true;
        } else {
            Log.w(this, "Unhandled Intent %s. Ignoring and not placing call.", intent);
            return 7;
        }
        if (z) {
            Log.i(this, "Placing call immediately instead of waiting for  OutgoingCallBroadcastReceiver: %s", intent);
            this.mCallsManager.placeOutgoingCall(this.mCall, Uri.fromParts(zIsUriNumber ? "sip" : "tel", numberFromIntent, null), null, this.mIntent.getBooleanExtra("android.telecom.extra.START_CALL_WITH_SPEAKERPHONE", false), this.mIntent.getIntExtra("android.telecom.extra.START_CALL_WITH_VIDEO_STATE", 0));
        }
        broadcastIntent(intent, numberFromIntent, !z);
        return 0;
    }

    private void broadcastIntent(Intent intent, String str, boolean z) {
        Intent intent2 = new Intent("android.intent.action.NEW_OUTGOING_CALL");
        if (str != null) {
            intent2.putExtra("android.intent.extra.PHONE_NUMBER", str);
        }
        intent2.addFlags(268435456);
        Log.v(this, "Broadcasting intent: %s.", intent2);
        checkAndCopyProviderExtras(intent, intent2);
        this.mContext.sendOrderedBroadcastAsUser(intent2, UserHandle.CURRENT, "android.permission.PROCESS_OUTGOING_CALLS", z ? new NewOutgoingCallBroadcastIntentReceiver() : null, null, -1, str, null);
    }

    public void checkAndCopyProviderExtras(Intent intent, Intent intent2) {
        if (intent != null) {
            if (hasGatewayProviderExtras(intent)) {
                intent2.putExtra("com.android.phone.extra.GATEWAY_PROVIDER_PACKAGE", intent.getStringExtra("com.android.phone.extra.GATEWAY_PROVIDER_PACKAGE"));
                intent2.putExtra("com.android.phone.extra.GATEWAY_URI", intent.getStringExtra("com.android.phone.extra.GATEWAY_URI"));
                Log.d(this, "Found and copied gateway provider extras to broadcast intent.", new Object[0]);
                return;
            }
            Log.d(this, "No provider extras found in call intent.", new Object[0]);
        }
    }

    private boolean hasGatewayProviderExtras(Intent intent) {
        return (TextUtils.isEmpty(intent.getStringExtra("com.android.phone.extra.GATEWAY_PROVIDER_PACKAGE")) || TextUtils.isEmpty(intent.getStringExtra("com.android.phone.extra.GATEWAY_URI"))) ? false : true;
    }

    private static Uri getGatewayUriFromString(String str) {
        if (TextUtils.isEmpty(str)) {
            return null;
        }
        return Uri.parse(str);
    }

    public static GatewayInfo getGateWayInfoFromIntent(Intent intent, Uri uri) {
        if (intent == null) {
            return null;
        }
        String stringExtra = intent.getStringExtra("com.android.phone.extra.GATEWAY_PROVIDER_PACKAGE");
        Uri gatewayUriFromString = getGatewayUriFromString(intent.getStringExtra("com.android.phone.extra.GATEWAY_URI"));
        if (TextUtils.isEmpty(stringExtra) || gatewayUriFromString == null) {
            return null;
        }
        return new GatewayInfo(stringExtra, gatewayUriFromString, uri);
    }

    private void launchSystemDialer(Uri uri) {
        Intent intent = new Intent();
        Resources resources = this.mContext.getResources();
        intent.setClassName(resources.getString(R.string.ui_default_package), resources.getString(R.string.dialer_default_class));
        intent.setAction("android.intent.action.DIAL");
        intent.setData(uri);
        intent.setFlags(268435456);
        Log.v(this, "calling startActivity for default dialer: %s", intent);
        this.mContext.startActivityAsUser(intent, UserHandle.CURRENT);
    }

    private boolean isPotentialEmergencyNumber(String str) {
        Log.v(this, "Checking restrictions for number : %s", Log.pii(str));
        return str != null && PhoneNumberUtils.isPotentialLocalEmergencyNumber(this.mContext, str);
    }

    private void rewriteCallIntentAction(Intent intent, boolean z) {
        String str;
        if ("android.intent.action.CALL_PRIVILEGED".equals(intent.getAction())) {
            if (z) {
                Log.i(this, "ACTION_CALL_PRIVILEGED is used while the number is a potential emergency number. Using ACTION_CALL_EMERGENCY as an action instead.", new Object[0]);
                str = "android.intent.action.CALL_EMERGENCY";
            } else {
                str = "android.intent.action.CALL";
            }
            Log.v(this, " - updating action from CALL_PRIVILEGED to %s", str);
            intent.setAction(str);
        }
    }
}
