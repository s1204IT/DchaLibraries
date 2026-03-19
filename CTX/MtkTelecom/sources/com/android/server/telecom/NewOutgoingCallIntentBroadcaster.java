package com.android.server.telecom;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.os.Trace;
import android.os.UserHandle;
import android.telecom.GatewayInfo;
import android.telecom.Log;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.text.TextUtils;
import com.android.internal.annotations.VisibleForTesting;
import com.android.server.telecom.TelecomSystem;
import com.mediatek.server.telecom.MtkUtil;

@VisibleForTesting
public class NewOutgoingCallIntentBroadcaster {
    private final Call mCall;
    private final CallsManager mCallsManager;
    private final Context mContext;
    private final Intent mIntent;
    private final boolean mIsDefaultOrSystemPhoneApp;
    private final TelecomSystem.SyncRoot mLock;
    private final PhoneNumberUtilsAdapter mPhoneNumberUtilsAdapter;

    @VisibleForTesting
    public NewOutgoingCallIntentBroadcaster(Context context, CallsManager callsManager, Call call, Intent intent, PhoneNumberUtilsAdapter phoneNumberUtilsAdapter, boolean z) {
        this.mContext = context;
        this.mCallsManager = callsManager;
        this.mCall = call;
        this.mIntent = intent;
        this.mPhoneNumberUtilsAdapter = phoneNumberUtilsAdapter;
        this.mIsDefaultOrSystemPhoneApp = z;
        this.mLock = this.mCallsManager.getLock();
    }

    public class NewOutgoingCallBroadcastIntentReceiver extends BroadcastReceiver {
        public NewOutgoingCallBroadcastIntentReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            boolean z;
            try {
                Log.startSession("NOCBIR.oR");
                Trace.beginSection("onReceiveNewOutgoingCallBroadcast");
                synchronized (NewOutgoingCallIntentBroadcaster.this.mLock) {
                    Log.v(this, "onReceive: %s", new Object[]{intent});
                    String resultData = getResultData();
                    Log.i(this, "Received new-outgoing-call-broadcast for %s with data %s", new Object[]{NewOutgoingCallIntentBroadcaster.this.mCall, Log.pii(resultData)});
                    NewOutgoingCallIntentBroadcaster.this.broadcastToUndemote(resultData, NewOutgoingCallIntentBroadcaster.this.mCall.getInitiatingUser());
                    long newOutgoingCallCancelMillis = Timeouts.getNewOutgoingCallCancelMillis(NewOutgoingCallIntentBroadcaster.this.mContext.getContentResolver());
                    if (resultData == null) {
                        Log.v(this, "Call cancelled (null number), returning...", new Object[0]);
                        newOutgoingCallCancelMillis = NewOutgoingCallIntentBroadcaster.this.getDisconnectTimeoutFromApp(getResultExtras(false), newOutgoingCallCancelMillis);
                    } else {
                        if (!NewOutgoingCallIntentBroadcaster.this.mPhoneNumberUtilsAdapter.isPotentialLocalEmergencyNumber(NewOutgoingCallIntentBroadcaster.this.mContext, resultData)) {
                            z = false;
                            if (!z) {
                                if (NewOutgoingCallIntentBroadcaster.this.mCall != null) {
                                    NewOutgoingCallIntentBroadcaster.this.mCall.disconnect(newOutgoingCallCancelMillis);
                                }
                                return;
                            } else {
                                if (NewOutgoingCallIntentBroadcaster.this.mCall.isDisconnected()) {
                                    Log.w(this, "Call has already been disconnected, ignore the broadcast Call %s", new Object[]{NewOutgoingCallIntentBroadcaster.this.mCall});
                                    return;
                                }
                                Uri uriFromParts = Uri.fromParts((!NewOutgoingCallIntentBroadcaster.this.mPhoneNumberUtilsAdapter.isUriNumber(resultData) || MtkUtil.isImsCallIntent(NewOutgoingCallIntentBroadcaster.this.mIntent)) ? "tel" : "sip", resultData, null);
                                Uri data = NewOutgoingCallIntentBroadcaster.this.mIntent.getData();
                                if (data.getSchemeSpecificPart().equals(resultData)) {
                                    Log.v(this, "Call number unmodified after new outgoing call intent broadcast.", new Object[0]);
                                } else {
                                    Log.v(this, "Retrieved modified handle after outgoing call intent broadcast: Original: %s, Modified: %s", new Object[]{Log.pii(data), Log.pii(uriFromParts)});
                                }
                                NewOutgoingCallIntentBroadcaster.this.placeOutgoingCallImmediately(NewOutgoingCallIntentBroadcaster.this.mCall, uriFromParts, NewOutgoingCallIntentBroadcaster.getGateWayInfoFromIntent(intent, uriFromParts), NewOutgoingCallIntentBroadcaster.this.mIntent.getBooleanExtra("android.telecom.extra.START_CALL_WITH_SPEAKERPHONE", false), NewOutgoingCallIntentBroadcaster.this.mIntent.getIntExtra("android.telecom.extra.START_CALL_WITH_VIDEO_STATE", 0));
                                return;
                            }
                        }
                        Log.w(this, "Cannot modify outgoing call to emergency number %s.", new Object[]{resultData});
                        newOutgoingCallCancelMillis = 0;
                    }
                    z = true;
                    if (!z) {
                    }
                }
            } finally {
                Trace.endSection();
                Log.endSession();
            }
        }
    }

    @VisibleForTesting
    public int processIntent() {
        boolean zIsSelfManaged;
        boolean z;
        Uri uri;
        boolean z2;
        PhoneAccount phoneAccountUnchecked;
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
                placeOutgoingCallImmediately(this.mCall, data, null, this.mIntent.getBooleanExtra("android.telecom.extra.START_CALL_WITH_SPEAKERPHONE", false), 0);
                return 0;
            }
            Log.i(this, "Unhandled intent %s. Ignoring and not placing call.", new Object[]{intent});
            return 44;
        }
        PhoneAccountHandle phoneAccountHandle = (PhoneAccountHandle) this.mIntent.getParcelableExtra("android.telecom.extra.PHONE_ACCOUNT_HANDLE");
        if (phoneAccountHandle != null && (phoneAccountUnchecked = this.mCallsManager.getPhoneAccountRegistrar().getPhoneAccountUnchecked(phoneAccountHandle)) != null) {
            zIsSelfManaged = phoneAccountUnchecked.isSelfManaged();
        } else {
            zIsSelfManaged = false;
        }
        String str = "";
        if (!zIsSelfManaged) {
            String numberFromIntent = this.mPhoneNumberUtilsAdapter.getNumberFromIntent(intent, this.mContext);
            if (TextUtils.isEmpty(numberFromIntent)) {
                Log.w(this, "Empty number obtained from the call intent.", new Object[0]);
                return 38;
            }
            boolean zIsUriNumber = this.mPhoneNumberUtilsAdapter.isUriNumber(numberFromIntent);
            if (!zIsUriNumber) {
                numberFromIntent = this.mPhoneNumberUtilsAdapter.stripSeparators(this.mPhoneNumberUtilsAdapter.convertKeypadLettersToDigits(numberFromIntent));
            }
            boolean zIsPotentialEmergencyNumber = isPotentialEmergencyNumber(numberFromIntent);
            Log.v(this, "isPotentialEmergencyNumber = %s", new Object[]{Boolean.valueOf(zIsPotentialEmergencyNumber)});
            rewriteCallIntentAction(intent, zIsPotentialEmergencyNumber);
            String action2 = intent.getAction();
            if ("android.intent.action.CALL".equals(action2)) {
                if (zIsPotentialEmergencyNumber) {
                    if (!this.mIsDefaultOrSystemPhoneApp) {
                        Log.w(this, "Cannot call potential emergency number %s with CALL Intent %s unless caller is system or default dialer.", new Object[]{numberFromIntent, intent});
                        launchSystemDialer(intent.getData());
                        return 44;
                    }
                } else {
                    z2 = false;
                    Uri uriFromParts = Uri.fromParts((zIsUriNumber || MtkUtil.isImsCallIntent(intent)) ? "tel" : "sip", numberFromIntent, null);
                    str = numberFromIntent;
                    uri = uriFromParts;
                    z = true;
                }
            } else if ("android.intent.action.CALL_EMERGENCY".equals(action2)) {
                if (!zIsPotentialEmergencyNumber) {
                    Log.w(this, "Cannot call non-potential-emergency number %s with EMERGENCY_CALL Intent %s.", new Object[]{numberFromIntent, intent});
                    return 44;
                }
            } else {
                Log.w(this, "Unhandled Intent %s. Ignoring and not placing call.", new Object[]{intent});
                return 7;
            }
            z2 = true;
            if (zIsUriNumber) {
                Uri uriFromParts2 = Uri.fromParts((zIsUriNumber || MtkUtil.isImsCallIntent(intent)) ? "tel" : "sip", numberFromIntent, null);
                str = numberFromIntent;
                uri = uriFromParts2;
                z = true;
            }
        } else {
            Log.i(this, "Skipping NewOutgoingCallBroadcast for self-managed call.", new Object[0]);
            z = false;
            uri = data;
            z2 = true;
        }
        if (z2) {
            placeOutgoingCallImmediately(this.mCall, uri, null, this.mIntent.getBooleanExtra("android.telecom.extra.START_CALL_WITH_SPEAKERPHONE", false), this.mIntent.getIntExtra("android.telecom.extra.START_CALL_WITH_VIDEO_STATE", 0));
        }
        if (z) {
            UserHandle initiatingUser = this.mCall.getInitiatingUser();
            Log.i(this, "Sending NewOutgoingCallBroadcast for %s to %s", new Object[]{this.mCall, initiatingUser});
            broadcastIntent(intent, str, !z2, initiatingUser);
        }
        if (z2 || !z) {
            this.mCall.setNewOutgoingCallIntentBroadcastIsDone();
        }
        return 0;
    }

    private void broadcastIntent(Intent intent, String str, boolean z, UserHandle userHandle) {
        Intent intent2 = new Intent("android.intent.action.NEW_OUTGOING_CALL");
        if (str != null) {
            intent2.putExtra("android.intent.extra.PHONE_NUMBER", str);
        }
        intent2.addFlags(285212672);
        Log.v(this, "Broadcasting intent: %s.", new Object[]{intent2});
        checkAndCopyProviderExtras(intent, intent2);
        this.mContext.sendOrderedBroadcastAsUser(intent2, userHandle, "android.permission.PROCESS_OUTGOING_CALLS", 54, z ? new NewOutgoingCallBroadcastIntentReceiver() : null, null, -1, str, null);
        if (!z) {
            broadcastToUndemote(str, userHandle);
        }
    }

    public void checkAndCopyProviderExtras(Intent intent, Intent intent2) {
        if (intent == null) {
            return;
        }
        if (hasGatewayProviderExtras(intent)) {
            intent2.putExtra("com.android.phone.extra.GATEWAY_PROVIDER_PACKAGE", intent.getStringExtra("com.android.phone.extra.GATEWAY_PROVIDER_PACKAGE"));
            intent2.putExtra("com.android.phone.extra.GATEWAY_URI", intent.getStringExtra("com.android.phone.extra.GATEWAY_URI"));
            Log.d(this, "Found and copied gateway provider extras to broadcast intent.", new Object[0]);
            return;
        }
        Log.d(this, "No provider extras found in call intent.", new Object[0]);
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

    private void placeOutgoingCallImmediately(Call call, Uri uri, GatewayInfo gatewayInfo, boolean z, int i) {
        Log.i(this, "Placing call immediately instead of waiting for OutgoingCallBroadcastReceiver", new Object[0]);
        this.mCall.setNewOutgoingCallIntentBroadcastIsDone();
        this.mCallsManager.placeOutgoingCall(call, uri, gatewayInfo, z, i);
    }

    private void launchSystemDialer(Uri uri) {
        Intent intent = new Intent();
        Resources resources = this.mContext.getResources();
        intent.setClassName(resources.getString(R.string.ui_default_package), resources.getString(R.string.dialer_default_class));
        intent.setAction("android.intent.action.DIAL");
        intent.setData(uri);
        intent.setFlags(268435456);
        Log.v(this, "calling startActivity for default dialer: %s", new Object[]{intent});
        this.mContext.startActivityAsUser(intent, UserHandle.CURRENT);
    }

    private boolean isPotentialEmergencyNumber(String str) {
        Log.v(this, "Checking restrictions for number : %s", new Object[]{Log.pii(str)});
        return str != null && this.mPhoneNumberUtilsAdapter.isPotentialLocalEmergencyNumber(this.mContext, str);
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
            Log.v(this, " - updating action from CALL_PRIVILEGED to %s", new Object[]{str});
            intent.setAction(str);
        }
    }

    private long getDisconnectTimeoutFromApp(Bundle bundle, long j) {
        if (bundle != null) {
            long j2 = bundle.getLong("android.telecom.extra.NEW_OUTGOING_CALL_CANCEL_TIMEOUT", j);
            if (j2 < 0) {
                j2 = 0;
            }
            return Math.min(j2, Timeouts.getMaxNewOutgoingCallCancelMillis(this.mContext.getContentResolver()));
        }
        return j;
    }

    private void broadcastToUndemote(String str, UserHandle userHandle) {
        Intent intent = new Intent("mediatek.intent.action.NEW_OUTGOING_CALL");
        intent.setClassName("com.android.dialer", "com.android.dialer.interactions.UndemoteOutgoingCallReceiver");
        if (str != null) {
            intent.putExtra("android.intent.extra.PHONE_NUMBER", str);
        }
        intent.addFlags(285212672);
        this.mContext.sendBroadcastAsUser(intent, userHandle, "android.permission.PROCESS_OUTGOING_CALLS");
    }
}
