package com.android.contacts;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.telephony.CarrierConfigManager;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import com.android.contacts.compat.CompatUtils;
import com.android.contacts.util.PermissionsUtil;
import com.android.contacts.util.PhoneNumberHelper;
import com.android.contactsbind.FeedbackHelper;
import com.android.contactsbind.experiments.Flags;
import com.mediatek.contacts.util.Log;
import java.util.Iterator;

public class CallUtil {
    public static Intent getCallWithSubjectIntent(String str, PhoneAccountHandle phoneAccountHandle, String str2) {
        Intent callIntent = getCallIntent(getCallUri(str));
        callIntent.putExtra("android.telecom.extra.CALL_SUBJECT", str2);
        if (phoneAccountHandle != null) {
            callIntent.putExtra("android.telecom.extra.PHONE_ACCOUNT_HANDLE", phoneAccountHandle);
        }
        return callIntent;
    }

    public static Intent getCallIntent(String str) {
        Uri callUri = getCallUri(str);
        return PhoneNumberUtils.isEmergencyNumber(str) ? getCallIntentForEmergencyNumber(callUri) : getCallIntent(callUri);
    }

    private static Intent getCallIntentForEmergencyNumber(Uri uri) {
        return new Intent("android.intent.action.DIAL", uri);
    }

    public static Intent getCallIntent(Uri uri) {
        return new Intent("android.intent.action.CALL", uri);
    }

    public static Intent getVideoCallIntent(String str, String str2) {
        Intent intent = new Intent("android.intent.action.CALL", getCallUri(str));
        intent.putExtra("android.telecom.extra.START_CALL_WITH_VIDEO_STATE", 3);
        if (!TextUtils.isEmpty(str2)) {
            intent.putExtra("com.android.phone.CALL_ORIGIN", str2);
        }
        return intent;
    }

    public static Uri getCallUri(String str) {
        if (PhoneNumberHelper.isUriNumber(str)) {
            return Uri.fromParts("sip", str, null);
        }
        return Uri.fromParts("tel", str, null);
    }

    public static int getVideoCallingAvailability(Context context) {
        if (!PermissionsUtil.hasPermission(context, "android.permission.READ_PHONE_STATE") || !CompatUtils.isVideoCompatible()) {
            Log.d("CallUtil", "[getVideoCallingAvailability] do not have read phone state permission");
            return 0;
        }
        TelecomManager telecomManager = (TelecomManager) context.getSystemService("telecom");
        if (telecomManager == null) {
            Log.d("CallUtil", "[getVideoCallingAvailability] telecommMgr is null");
            return 0;
        }
        try {
            Iterator<PhoneAccountHandle> it = telecomManager.getCallCapablePhoneAccounts().iterator();
            while (it.hasNext()) {
                PhoneAccount phoneAccount = telecomManager.getPhoneAccount(it.next());
                if (phoneAccount != null && phoneAccount.hasCapabilities(8)) {
                    if (CompatUtils.isVideoPresenceCompatible()) {
                        return phoneAccount.hasCapabilities(256) ? 3 : 1;
                    }
                    Log.d("CallUtil", "[getVideoCallingAvailability] not videoPresenceCompatible");
                    return 1;
                }
            }
            return 0;
        } catch (SecurityException e) {
            FeedbackHelper.sendFeedback(context, "CallUtil", "Security exception when getting call capable phone accounts", e);
            Log.d("CallUtil", "[getVideoCallingAvailability] Security exception");
            return 0;
        }
    }

    public static boolean isCallWithSubjectSupported(Context context) {
        TelecomManager telecomManager;
        if (!PermissionsUtil.hasPermission(context, "android.permission.READ_PHONE_STATE") || !CompatUtils.isCallSubjectCompatible() || (telecomManager = (TelecomManager) context.getSystemService("telecom")) == null) {
            return false;
        }
        try {
            Iterator<PhoneAccountHandle> it = telecomManager.getCallCapablePhoneAccounts().iterator();
            while (it.hasNext()) {
                PhoneAccount phoneAccount = telecomManager.getPhoneAccount(it.next());
                if (phoneAccount != null && phoneAccount.hasCapabilities(64)) {
                    return true;
                }
            }
            return false;
        } catch (SecurityException e) {
            FeedbackHelper.sendFeedback(context, "CallUtil", "Security exception when getting call capable phone accounts", e);
            return false;
        }
    }

    public static boolean isTachyonEnabled(Context context) {
        TelecomManager telecomManager;
        if (!PermissionsUtil.hasPermission(context, "android.permission.READ_PHONE_STATE") || !CompatUtils.isNCompatible() || (telecomManager = (TelecomManager) context.getSystemService("telecom")) == null) {
            return false;
        }
        try {
            Iterator<PhoneAccountHandle> it = telecomManager.getCallCapablePhoneAccounts().iterator();
            while (it.hasNext()) {
                PhoneAccount phoneAccount = telecomManager.getPhoneAccount(it.next());
                if (phoneAccount != null) {
                    Bundle extras = phoneAccount.getExtras();
                    boolean z = extras != null && extras.getBoolean("android.telecom.extra.SUPPORTS_VIDEO_CALLING_FALLBACK");
                    if (Log.isLoggable("CallUtil", 3)) {
                        Log.d("CallUtil", "Device video fallback config: " + z);
                    }
                    PersistableBundle config = ((CarrierConfigManager) context.getSystemService(CarrierConfigManager.class)).getConfig();
                    boolean z2 = config != null && config.getBoolean("allow_video_calling_fallback_bool");
                    if (Log.isLoggable("CallUtil", 3)) {
                        Log.d("CallUtil", "Carrier video fallback config: " + z2);
                    }
                    boolean z3 = Flags.getInstance().getBoolean("QuickContact__video_call_integration");
                    if (Log.isLoggable("CallUtil", 3)) {
                        Log.d("CallUtil", "Experiment video fallback config: " + z3);
                    }
                    return z && z2 && z3;
                }
            }
            return false;
        } catch (SecurityException e) {
            FeedbackHelper.sendFeedback(context, "CallUtil", "Security exception when getting call capable phone accounts", e);
            return false;
        }
    }
}
