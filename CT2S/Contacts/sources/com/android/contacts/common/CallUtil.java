package com.android.contacts.common;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import com.android.contacts.common.util.PhoneNumberHelper;

public class CallUtil {
    public static Intent getCallIntent(String number) {
        return getCallIntent(number, (String) null, (PhoneAccountHandle) null);
    }

    public static Intent getCallIntent(Uri uri) {
        return getCallIntent(uri, (String) null, (PhoneAccountHandle) null);
    }

    public static Intent getCallIntent(String number, String callOrigin, PhoneAccountHandle accountHandle) {
        return getCallIntent(getCallUri(number), callOrigin, accountHandle);
    }

    public static Intent getCallIntent(Uri uri, String callOrigin, PhoneAccountHandle accountHandle) {
        return getCallIntent(uri, callOrigin, accountHandle, 0);
    }

    public static Intent getVideoCallIntent(String number, String callOrigin) {
        return getCallIntent(getCallUri(number), callOrigin, null, 3);
    }

    public static Intent getCallIntent(Uri uri, String callOrigin, PhoneAccountHandle accountHandle, int videoState) {
        Intent intent = new Intent("android.intent.action.CALL_PRIVILEGED", uri);
        intent.putExtra("android.telecom.extra.START_CALL_WITH_VIDEO_STATE", videoState);
        if (callOrigin != null) {
            intent.putExtra("com.android.phone.CALL_ORIGIN", callOrigin);
        }
        if (accountHandle != null) {
            intent.putExtra("android.telecom.extra.PHONE_ACCOUNT_HANDLE", accountHandle);
        }
        return intent;
    }

    public static Uri getCallUri(String number) {
        return PhoneNumberHelper.isUriNumber(number) ? Uri.fromParts("sip", number, null) : Uri.fromParts("tel", number, null);
    }

    public static boolean isVideoEnabled(Context context) {
        TelecomManager telecommMgr = (TelecomManager) context.getSystemService("telecom");
        if (telecommMgr == null) {
            return false;
        }
        return telecommMgr.isVideoEnabled();
    }
}
