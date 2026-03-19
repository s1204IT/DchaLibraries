package com.android.server.telecom;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Trace;
import android.os.UserHandle;
import android.os.UserManager;
import android.telecom.DefaultDialerManager;
import android.telecom.Log;
import android.telecom.PhoneAccountHandle;
import android.telephony.PhoneNumberUtils;
import android.widget.Toast;
import com.android.server.telecom.components.ErrorDialogActivity;
import com.mediatek.server.telecom.MtkUtil;

public class CallIntentProcessor {
    private final CallsManager mCallsManager;
    private final Context mContext;

    public interface Adapter {
        void processIncomingCallIntent(CallsManager callsManager, Intent intent);

        void processUnknownCallIntent(CallsManager callsManager, Intent intent);
    }

    public static class AdapterImpl implements Adapter {
        @Override
        public void processIncomingCallIntent(CallsManager callsManager, Intent intent) {
            CallIntentProcessor.processIncomingCallIntent(callsManager, intent);
        }

        @Override
        public void processUnknownCallIntent(CallsManager callsManager, Intent intent) {
            CallIntentProcessor.processUnknownCallIntent(callsManager, intent);
        }
    }

    public CallIntentProcessor(Context context, CallsManager callsManager) {
        this.mContext = context;
        this.mCallsManager = callsManager;
    }

    public void processIntent(Intent intent) {
        boolean booleanExtra = intent.getBooleanExtra("is_unknown_call", false);
        Log.i(this, "onReceive - isUnknownCall: %s", new Object[]{Boolean.valueOf(booleanExtra)});
        Trace.beginSection("processNewCallCallIntent");
        if (booleanExtra) {
            processUnknownCallIntent(this.mCallsManager, intent);
        } else {
            if (!this.mCallsManager.getInCallController().isBoundAndConnectedToServices()) {
                this.mCallsManager.getInCallController().bindToServices(null);
            }
            processOutgoingCallIntent(this.mContext, this.mCallsManager, intent);
        }
        Trace.endSection();
    }

    static void processOutgoingCallIntent(Context context, CallsManager callsManager, Intent intent) {
        Uri data = intent.getData();
        String scheme = data.getScheme();
        String schemeSpecificPart = data.getSchemeSpecificPart();
        if ("tel".equals(scheme) && PhoneNumberUtils.isUriNumber(schemeSpecificPart)) {
            data = Uri.fromParts((!PhoneNumberUtils.isUriNumber(schemeSpecificPart) || MtkUtil.isImsCallIntent(intent)) ? "tel" : "sip", schemeSpecificPart, null);
        }
        Uri uri = data;
        PhoneAccountHandle phoneAccountHandleWithSlotId = (PhoneAccountHandle) intent.getParcelableExtra("android.telecom.extra.PHONE_ACCOUNT_HANDLE");
        Bundle bundleExtra = intent.hasExtra("android.telecom.extra.OUTGOING_CALL_EXTRAS") ? intent.getBundleExtra("android.telecom.extra.OUTGOING_CALL_EXTRAS") : null;
        Bundle bundle = bundleExtra == null ? new Bundle() : bundleExtra;
        if (intent.hasExtra("android.telecom.extra.CALL_SUBJECT")) {
            bundle.putString("android.telecom.extra.CALL_SUBJECT", intent.getStringExtra("android.telecom.extra.CALL_SUBJECT"));
        }
        bundle.putInt("android.telecom.extra.START_CALL_WITH_VIDEO_STATE", intent.getIntExtra("android.telecom.extra.START_CALL_WITH_VIDEO_STATE", 0));
        if (!callsManager.isSelfManaged(phoneAccountHandleWithSlotId, (UserHandle) intent.getParcelableExtra("initiating_user"))) {
            if (fixInitiatingUserIfNecessary(context, intent)) {
                Toast.makeText(context, R.string.toast_personal_call_msg, 1).show();
            }
        } else {
            Log.i(CallIntentProcessor.class, "processOutgoingCallIntent: skip initiating user check", new Object[0]);
        }
        copyMtkExtras(intent.getExtras(), bundle);
        UserHandle userHandle = (UserHandle) intent.getParcelableExtra("initiating_user");
        if (intent.hasExtra("com.android.phone.extra.slot")) {
            phoneAccountHandleWithSlotId = MtkUtil.getPhoneAccountHandleWithSlotId(context, intent.getIntExtra("com.android.phone.extra.slot", -1), phoneAccountHandleWithSlotId);
        }
        Call callStartOutgoingCall = callsManager.startOutgoingCall(uri, phoneAccountHandleWithSlotId, bundle, userHandle, intent);
        if (callStartOutgoingCall != null) {
            sendNewOutgoingCallIntent(context, callStartOutgoingCall, callsManager, intent);
        } else {
            callsManager.getInCallController().unbindUselessService();
        }
    }

    static void sendNewOutgoingCallIntent(Context context, Call call, CallsManager callsManager, Intent intent) {
        int iProcessIntent = new NewOutgoingCallIntentBroadcaster(context, callsManager, call, intent, callsManager.getPhoneNumberUtilsAdapter(), intent.getBooleanExtra("is_privileged_dialer", false)).processIntent();
        if (!(iProcessIntent == 0) && call != null) {
            disconnectCallAndShowErrorDialog(context, call, iProcessIntent);
        }
    }

    static boolean fixInitiatingUserIfNecessary(Context context, Intent intent) {
        UserHandle userHandle = (UserHandle) intent.getParcelableExtra("initiating_user");
        if (UserUtil.isManagedProfile(context, userHandle)) {
            if (DefaultDialerManager.getInstalledDialerApplications(context, userHandle.getIdentifier()).size() == 0) {
                UserHandle userHandle2 = UserManager.get(context).getProfileParent(userHandle.getIdentifier()).getUserHandle();
                intent.putExtra("initiating_user", userHandle2);
                Log.i(CallIntentProcessor.class, "fixInitiatingUserIfNecessary: no dialer installed for current user; setting initiator to parent %s" + userHandle2, new Object[0]);
                return true;
            }
        }
        return false;
    }

    static void processIncomingCallIntent(CallsManager callsManager, Intent intent) {
        PhoneAccountHandle phoneAccountHandle = (PhoneAccountHandle) intent.getParcelableExtra("android.telecom.extra.PHONE_ACCOUNT_HANDLE");
        if (phoneAccountHandle == null) {
            Log.w(CallIntentProcessor.class, "Rejecting incoming call due to null phone account", new Object[0]);
            return;
        }
        if (phoneAccountHandle.getComponentName() == null) {
            Log.w(CallIntentProcessor.class, "Rejecting incoming call due to null component name", new Object[0]);
            return;
        }
        Bundle bundle = null;
        if (intent.hasExtra("android.telecom.extra.INCOMING_CALL_EXTRAS")) {
            bundle = intent.getBundleExtra("android.telecom.extra.INCOMING_CALL_EXTRAS");
        }
        if (bundle == null) {
            bundle = new Bundle();
        }
        Log.d(CallIntentProcessor.class, "Processing incoming call from connection service [%s]", new Object[]{phoneAccountHandle.getComponentName()});
        callsManager.processIncomingCallIntent(phoneAccountHandle, bundle);
    }

    static void processUnknownCallIntent(CallsManager callsManager, Intent intent) {
        PhoneAccountHandle phoneAccountHandle = (PhoneAccountHandle) intent.getParcelableExtra("android.telecom.extra.PHONE_ACCOUNT_HANDLE");
        if (phoneAccountHandle == null) {
            Log.w(CallIntentProcessor.class, "Rejecting unknown call due to null phone account", new Object[0]);
        } else if (phoneAccountHandle.getComponentName() == null) {
            Log.w(CallIntentProcessor.class, "Rejecting unknown call due to null component name", new Object[0]);
        } else {
            callsManager.addNewUnknownCall(phoneAccountHandle, intent.getExtras());
        }
    }

    private static void disconnectCallAndShowErrorDialog(Context context, Call call, int i) {
        int i2;
        call.disconnect();
        Intent intent = new Intent(context, (Class<?>) ErrorDialogActivity.class);
        if (i == 7 || i == 38) {
            i2 = R.string.outgoing_call_error_no_phone_number_supplied;
        } else {
            i2 = -1;
        }
        if (i2 != -1) {
            intent.putExtra("error_message_id", i2);
            intent.setFlags(268435456);
            context.startActivityAsUser(intent, UserHandle.CURRENT);
        }
    }

    private static void copyMtkExtras(Bundle bundle, Bundle bundle2) {
        if (bundle == null || bundle2 == null) {
            return;
        }
        if (bundle.containsKey("mediatek.telecom.extra.EXTRA_START_VOLTE_CONFERENCE")) {
            bundle2.putBoolean("mediatek.telecom.extra.EXTRA_START_VOLTE_CONFERENCE", bundle.getBoolean("mediatek.telecom.extra.EXTRA_START_VOLTE_CONFERENCE", false));
        }
        if (bundle.containsKey("mediatek.telecom.extra.VOLTE_CONFERENCE_NUMBERS")) {
            bundle2.putStringArrayList("mediatek.telecom.extra.VOLTE_CONFERENCE_NUMBERS", bundle.getStringArrayList("mediatek.telecom.extra.VOLTE_CONFERENCE_NUMBERS"));
        }
        if (bundle.containsKey("mediatek.telecom.extra.EXTRA_INCOMING_VOLTE_CONFERENCE")) {
            bundle2.putBoolean("mediatek.telecom.extra.EXTRA_INCOMING_VOLTE_CONFERENCE", bundle.getBoolean("mediatek.telecom.extra.EXTRA_INCOMING_VOLTE_CONFERENCE", false));
        }
    }
}
