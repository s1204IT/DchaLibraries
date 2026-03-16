package com.android.server.telecom;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Trace;
import android.os.UserHandle;
import android.telecom.PhoneAccountHandle;
import android.telephony.PhoneNumberUtils;
import android.widget.Toast;

public class CallReceiver extends BroadcastReceiver {
    private static final String TAG = CallReceiver.class.getName();

    @Override
    public void onReceive(Context context, Intent intent) {
        boolean booleanExtra = intent.getBooleanExtra("is_unknown_call", false);
        Log.i(this, "onReceive - isUnknownCall: %s", Boolean.valueOf(booleanExtra));
        Trace.beginSection("processNewCallCallIntent");
        if (booleanExtra) {
            processUnknownCallIntent(intent);
        } else {
            processOutgoingCallIntent(context, intent);
        }
        Trace.endSection();
    }

    static void processOutgoingCallIntent(Context context, Intent intent) {
        Uri uriFromParts;
        if (!shouldPreventDuplicateVideoCall(context, intent)) {
            Uri data = intent.getData();
            String scheme = data.getScheme();
            String schemeSpecificPart = data.getSchemeSpecificPart();
            if ("voicemail".equals(scheme)) {
                uriFromParts = data;
            } else {
                uriFromParts = Uri.fromParts(PhoneNumberUtils.isUriNumber(schemeSpecificPart) ? "sip" : "tel", schemeSpecificPart, null);
            }
            PhoneAccountHandle phoneAccountHandle = (PhoneAccountHandle) intent.getParcelableExtra("android.telecom.extra.PHONE_ACCOUNT_HANDLE");
            Bundle bundleExtra = intent.hasExtra("android.telecom.extra.OUTGOING_CALL_EXTRAS") ? intent.getBundleExtra("android.telecom.extra.OUTGOING_CALL_EXTRAS") : null;
            if (bundleExtra == null) {
                bundleExtra = new Bundle();
            }
            boolean booleanExtra = intent.getBooleanExtra("is_default_dialer", false);
            Call callStartOutgoingCall = getCallsManager().startOutgoingCall(uriFromParts, phoneAccountHandle, bundleExtra, intent.getIntExtra("android.telecom.extra.START_CALL_WITH_VIDEO_STATE", 0));
            if (callStartOutgoingCall != null) {
                int iProcessIntent = new NewOutgoingCallIntentBroadcaster(context, getCallsManager(), callStartOutgoingCall, intent, booleanExtra).processIntent();
                if (!(iProcessIntent == 0) && callStartOutgoingCall != null) {
                    disconnectCallAndShowErrorDialog(context, callStartOutgoingCall, iProcessIntent);
                }
            }
        }
    }

    static void processIncomingCallIntent(Intent intent) {
        PhoneAccountHandle phoneAccountHandle = (PhoneAccountHandle) intent.getParcelableExtra("android.telecom.extra.PHONE_ACCOUNT_HANDLE");
        if (phoneAccountHandle == null) {
            Log.w(TAG, "Rejecting incoming call due to null phone account", new Object[0]);
            return;
        }
        if (phoneAccountHandle.getComponentName() == null) {
            Log.w(TAG, "Rejecting incoming call due to null component name", new Object[0]);
            return;
        }
        Bundle bundleExtra = null;
        if (intent.hasExtra("android.telecom.extra.INCOMING_CALL_EXTRAS")) {
            bundleExtra = intent.getBundleExtra("android.telecom.extra.INCOMING_CALL_EXTRAS");
        }
        if (bundleExtra == null) {
            bundleExtra = Bundle.EMPTY;
        }
        Log.d(TAG, "Processing incoming call from connection service [%s]", phoneAccountHandle.getComponentName());
        getCallsManager().processIncomingCallIntent(phoneAccountHandle, bundleExtra);
    }

    private void processUnknownCallIntent(Intent intent) {
        PhoneAccountHandle phoneAccountHandle = (PhoneAccountHandle) intent.getParcelableExtra("android.telecom.extra.PHONE_ACCOUNT_HANDLE");
        if (phoneAccountHandle == null) {
            Log.w(this, "Rejecting unknown call due to null phone account", new Object[0]);
        } else if (phoneAccountHandle.getComponentName() == null) {
            Log.w(this, "Rejecting unknown call due to null component name", new Object[0]);
        } else {
            getCallsManager().addNewUnknownCall(phoneAccountHandle, intent.getExtras());
        }
    }

    static CallsManager getCallsManager() {
        return CallsManager.getInstance();
    }

    private static void disconnectCallAndShowErrorDialog(Context context, Call call, int i) {
        int i2;
        call.disconnect();
        Intent intent = new Intent(context, (Class<?>) ErrorDialogActivity.class);
        switch (i) {
            case 7:
            case 38:
                i2 = R.string.outgoing_call_error_no_phone_number_supplied;
                break;
            default:
                i2 = -1;
                break;
        }
        if (i2 != -1) {
            intent.putExtra("error_message_id", i2);
        }
        intent.setFlags(268435456);
        context.startActivityAsUser(intent, UserHandle.CURRENT);
    }

    private static boolean shouldPreventDuplicateVideoCall(Context context, Intent intent) {
        if (intent.getIntExtra("android.telecom.extra.START_CALL_WITH_VIDEO_STATE", 0) == 0 || !getCallsManager().hasVideoCall()) {
            return false;
        }
        Toast.makeText(context, context.getResources().getString(R.string.duplicate_video_call_not_allowed), 1).show();
        return true;
    }
}
