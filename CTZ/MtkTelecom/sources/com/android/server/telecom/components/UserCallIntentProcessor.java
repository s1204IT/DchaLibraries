package com.android.server.telecom.components;

import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.UserHandle;
import android.os.UserManager;
import android.telecom.DefaultDialerManager;
import android.telecom.Log;
import android.telecom.TelecomManager;
import android.telecom.VideoProfile;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import com.android.server.telecom.PhoneNumberUtilsAdapterImpl;
import com.android.server.telecom.R;
import com.android.server.telecom.TelecomSystem;
import com.android.server.telecom.TelephonyUtil;
import com.android.server.telecom.UserUtil;
import com.mediatek.server.telecom.MtkUtil;

public class UserCallIntentProcessor {
    private static final String TAG = UserCallIntentProcessor.class.getSimpleName();
    private final Context mContext;
    private final UserHandle mUserHandle;

    public UserCallIntentProcessor(Context context, UserHandle userHandle) {
        this.mContext = context;
        this.mUserHandle = userHandle;
    }

    public void processIntent(Intent intent, String str, boolean z, boolean z2) {
        Log.d(this, "[processIntent]extras: " + MtkUtil.dumpBundle(intent.getExtras()), new Object[0]);
        if (MtkUtil.isConferenceInvitation(intent.getExtras())) {
            intent.setData(Uri.fromParts("tel", "1234567890987654321", null));
        }
        if (!isVoiceCapable()) {
            return;
        }
        String action = intent.getAction();
        if ("android.intent.action.CALL".equals(action) || "android.intent.action.CALL_PRIVILEGED".equals(action) || "android.intent.action.CALL_EMERGENCY".equals(action)) {
            processOutgoingCallIntent(intent, str, z, z2);
        }
    }

    private void processOutgoingCallIntent(Intent intent, String str, boolean z, boolean z2) {
        boolean zShouldProcessAsEmergency;
        Intent intentCreateAdminSupportIntent;
        if (intent.getBooleanExtra("com.android.phone.extra.SEND_EMPTY_FLASH", false)) {
            Log.w(this, "Empty flash obtained from the call intent.", new Object[0]);
            return;
        }
        Uri data = intent.getData();
        String scheme = data.getScheme();
        String schemeSpecificPart = data.getSchemeSpecificPart();
        if ("tel".equals(scheme) && PhoneNumberUtils.isUriNumber(schemeSpecificPart)) {
            data = Uri.fromParts((!PhoneNumberUtils.isUriNumber(schemeSpecificPart) || MtkUtil.isImsCallIntent(intent)) ? "tel" : "sip", schemeSpecificPart, null);
        }
        PhoneNumberUtilsAdapterImpl phoneNumberUtilsAdapterImpl = new PhoneNumberUtilsAdapterImpl();
        boolean zIsPotentialLocalEmergencyNumber = phoneNumberUtilsAdapterImpl.isPotentialLocalEmergencyNumber(this.mContext, phoneNumberUtilsAdapterImpl.getNumberFromIntent(intent, this.mContext));
        if (zIsPotentialLocalEmergencyNumber) {
            zShouldProcessAsEmergency = TelephonyUtil.shouldProcessAsEmergency(this.mContext, data);
        } else {
            zShouldProcessAsEmergency = false;
        }
        if (!UserUtil.isManagedProfile(this.mContext, this.mUserHandle) && !zShouldProcessAsEmergency) {
            UserManager userManager = (UserManager) this.mContext.getSystemService("user");
            if (userManager.hasBaseUserRestriction("no_outgoing_calls", this.mUserHandle)) {
                showErrorDialogForRestrictedOutgoingCall(this.mContext, R.string.outgoing_call_not_allowed_user_restriction);
                Log.w(this, "Rejecting non-emergency phone call due to DISALLOW_OUTGOING_CALLS restriction", new Object[0]);
                return;
            } else if (userManager.hasUserRestriction("no_outgoing_calls", this.mUserHandle)) {
                DevicePolicyManager devicePolicyManager = (DevicePolicyManager) this.mContext.getSystemService(DevicePolicyManager.class);
                if (devicePolicyManager != null && (intentCreateAdminSupportIntent = devicePolicyManager.createAdminSupportIntent("no_outgoing_calls")) != null) {
                    this.mContext.startActivity(intentCreateAdminSupportIntent);
                    return;
                }
                return;
            }
        }
        if (!z && !zShouldProcessAsEmergency) {
            showErrorDialogForRestrictedOutgoingCall(this.mContext, R.string.outgoing_call_not_allowed_no_permission);
            Log.w(this, "Rejecting non-emergency phone call because android.permission.CALL_PHONE permission is not granted.", new Object[0]);
            return;
        }
        int intExtra = intent.getIntExtra("android.telecom.extra.START_CALL_WITH_VIDEO_STATE", 0);
        Log.d(this, "processOutgoingCallIntent videoState = " + intExtra, new Object[0]);
        if (VideoProfile.isVideo(intExtra) && zShouldProcessAsEmergency) {
            Log.d(this, "Emergency call, converting video call to voice...", new Object[0]);
            intent.putExtra("android.telecom.extra.START_CALL_WITH_VIDEO_STATE", 0);
        }
        intent.putExtra("is_privileged_dialer", isDefaultOrSystemDialer(str));
        intent.putExtra("initiating_user", this.mUserHandle);
        if (blockAndLaunchSystemDialer(intent, this.mContext, zIsPotentialLocalEmergencyNumber)) {
            return;
        }
        sendIntentToDestination(intent, z2);
    }

    private boolean isDefaultOrSystemDialer(String str) {
        if (TextUtils.isEmpty(str)) {
            return false;
        }
        if (TextUtils.equals(DefaultDialerManager.getDefaultDialerApplication(this.mContext, this.mUserHandle.getIdentifier()), str)) {
            return true;
        }
        return TextUtils.equals(((TelecomManager) this.mContext.getSystemService("telecom")).getSystemDialerPackage(), str);
    }

    private boolean isVoiceCapable() {
        return this.mContext.getApplicationContext().getResources().getBoolean(android.R.^attr-private.popupPromptView);
    }

    private boolean sendIntentToDestination(Intent intent, boolean z) {
        intent.putExtra("is_incoming_call", false);
        intent.setFlags(268435456);
        intent.setClass(this.mContext, PrimaryCallReceiver.class);
        if (z) {
            Log.i(this, "sendIntentToDestination: send intent to Telecom directly.", new Object[0]);
            synchronized (TelecomSystem.getInstance().getLock()) {
                TelecomSystem.getInstance().getCallIntentProcessor().processIntent(intent);
            }
            return true;
        }
        Log.i(this, "sendIntentToDestination: trampoline to Telecom.", new Object[0]);
        this.mContext.sendBroadcastAsUser(intent, UserHandle.SYSTEM);
        return true;
    }

    private static void showErrorDialogForRestrictedOutgoingCall(Context context, int i) {
        Intent intent = new Intent(context, (Class<?>) ErrorDialogActivity.class);
        intent.setFlags(268435456);
        intent.putExtra("error_message_id", i);
        context.startActivityAsUser(intent, UserHandle.CURRENT);
    }

    private static boolean blockAndLaunchSystemDialer(Intent intent, Context context, boolean z) {
        boolean booleanExtra = intent.getBooleanExtra("is_privileged_dialer", false);
        if (!TextUtils.equals("android.intent.action.CALL", intent.getAction()) || !z || booleanExtra) {
            return false;
        }
        launchSystemDialer(intent.getData(), context);
        return true;
    }

    private static void launchSystemDialer(Uri uri, Context context) {
        Intent intent = new Intent();
        Resources resources = context.getResources();
        intent.setClassName(resources.getString(R.string.ui_default_package), resources.getString(R.string.dialer_default_class));
        intent.setAction("android.intent.action.DIAL");
        intent.setData(uri);
        intent.setFlags(268435456);
        Log.v(TAG, "calling startActivity for default dialer: %s", new Object[]{intent});
        context.startActivityAsUser(intent, UserHandle.CURRENT);
    }
}
