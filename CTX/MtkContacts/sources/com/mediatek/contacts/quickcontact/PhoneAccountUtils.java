package com.mediatek.contacts.quickcontact;

import android.content.ComponentName;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.text.TextUtils;
import com.mediatek.contacts.util.Log;

public class PhoneAccountUtils {
    public static PhoneAccountHandle getAccount(String str, String str2) {
        if (TextUtils.isEmpty(str) || TextUtils.isEmpty(str2)) {
            Log.w("PhoneAccountUtils", "[getAccount]componentString = " + str + ",accountId = " + str2);
            return null;
        }
        return new PhoneAccountHandle(ComponentName.unflattenFromString(str), str2);
    }

    public static Drawable getAccountIcon(Context context, PhoneAccountHandle phoneAccountHandle) {
        PhoneAccount accountOrNull = getAccountOrNull(context, phoneAccountHandle);
        if (accountOrNull == null) {
            Log.w("PhoneAccountUtils", "[getAccountIcon]account is null.");
            return null;
        }
        return accountOrNull.getIcon().loadDrawable(context);
    }

    public static String getAccountLabel(Context context, PhoneAccountHandle phoneAccountHandle) {
        PhoneAccount accountOrNull = getAccountOrNull(context, phoneAccountHandle);
        if (accountOrNull == null) {
            Log.w("PhoneAccountUtils", "[getAccountLabel]account is null.");
            return null;
        }
        return accountOrNull.getLabel().toString();
    }

    private static PhoneAccount getAccountOrNull(Context context, PhoneAccountHandle phoneAccountHandle) {
        TelecomManager telecomManager = (TelecomManager) context.getSystemService("telecom");
        PhoneAccount phoneAccount = telecomManager.getPhoneAccount(phoneAccountHandle);
        if (phoneAccount == null || telecomManager.getCallCapablePhoneAccounts().size() <= 1) {
            Log.sensitive("PhoneAccountUtils", "[getAccountOrNull]account = " + phoneAccount);
            return null;
        }
        return phoneAccount;
    }
}
