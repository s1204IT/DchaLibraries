package com.android.phone;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.BenesseExtension;
import android.telephony.PhoneNumberUtils;
import android.util.Log;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.TelephonyCapabilities;

public class SpecialCharSequenceMgr {
    private static final boolean DBG = false;
    private static final String MMI_IMEI_DISPLAY = "*#06#";
    private static final String MMI_REGULATORY_INFO_DISPLAY = "*#07#";
    private static final String TAG = "PhoneGlobals";

    private SpecialCharSequenceMgr() {
    }

    static boolean handleChars(Context context, String str) {
        return handleChars(context, str, null);
    }

    static boolean handleChars(Context context, String str, Activity activity) {
        String strStripSeparators = PhoneNumberUtils.stripSeparators(str);
        if (handleIMEIDisplay(context, strStripSeparators) || handleRegulatoryInfoDisplay(context, strStripSeparators) || handlePinEntry(context, strStripSeparators, activity) || handleAdnEntry(context, strStripSeparators) || handleSecretCode(strStripSeparators)) {
            return true;
        }
        return false;
    }

    static boolean handleCharsForLockedDevice(Context context, String str, Activity activity) {
        if (handlePinEntry(context, PhoneNumberUtils.stripSeparators(str), activity)) {
            return true;
        }
        return false;
    }

    private static boolean handleSecretCode(String str) {
        int length = str.length();
        if (length > 8 && str.startsWith("*#*#") && str.endsWith("#*#*")) {
            PhoneGlobals.getPhone().sendDialerSpecialCode(str.substring(4, length - 4));
            return true;
        }
        return false;
    }

    private static boolean handleAdnEntry(Context context, String str) {
        int length;
        if (!PhoneGlobals.getInstance().getKeyguardManager().inKeyguardRestrictedInputMode() && (length = str.length()) > 1 && length < 5 && str.endsWith("#")) {
            try {
                int i = Integer.parseInt(str.substring(0, length - 1));
                Intent intent = new Intent("android.intent.action.PICK");
                intent.setClassName("com.android.phone", "com.android.phone.SimContacts");
                intent.setFlags(268435456);
                intent.putExtra("index", i);
                PhoneGlobals.getInstance().startActivity(intent);
                return true;
            } catch (NumberFormatException e) {
            }
        }
        return false;
    }

    private static boolean handlePinEntry(Context context, String str, Activity activity) {
        if ((str.startsWith("**04") || str.startsWith("**05")) && str.endsWith("#")) {
            PhoneGlobals phoneGlobals = PhoneGlobals.getInstance();
            boolean zHandlePinMmi = PhoneGlobals.getPhone().handlePinMmi(str);
            if (zHandlePinMmi && str.startsWith("**05")) {
                phoneGlobals.setPukEntryActivity(activity);
            }
            return zHandlePinMmi;
        }
        return false;
    }

    private static boolean handleIMEIDisplay(Context context, String str) {
        if (str.equals(MMI_IMEI_DISPLAY)) {
            showDeviceIdPanel(context);
            return true;
        }
        return false;
    }

    private static void showDeviceIdPanel(Context context) {
        Phone phone = PhoneGlobals.getPhone();
        int deviceIdLabel = TelephonyCapabilities.getDeviceIdLabel(phone);
        AlertDialog alertDialogCreate = new AlertDialog.Builder(context).setTitle(deviceIdLabel).setMessage(phone.getDeviceId()).setPositiveButton(R.string.ok, (DialogInterface.OnClickListener) null).setCancelable(false).create();
        alertDialogCreate.getWindow().setType(2007);
        alertDialogCreate.show();
    }

    private static boolean handleRegulatoryInfoDisplay(Context context, String str) {
        if (!str.equals(MMI_REGULATORY_INFO_DISPLAY) || BenesseExtension.getDchaState() != 0) {
            return false;
        }
        log("handleRegulatoryInfoDisplay() sending intent to settings app");
        try {
            context.startActivity(new Intent("android.settings.SHOW_REGULATORY_INFO"));
            return true;
        } catch (ActivityNotFoundException e) {
            Log.e("PhoneGlobals", "startActivity() failed: " + e);
            return true;
        }
    }

    private static void log(String str) {
        Log.d("PhoneGlobals", "[SpecialCharSequenceMgr] " + str);
    }
}
