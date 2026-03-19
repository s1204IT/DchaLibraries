package com.android.phone;

import android.app.AlertDialog;
import android.content.Context;
import android.os.SystemProperties;
import android.util.Log;

public class PhoneDisplayMessage {
    private static final boolean DBG;
    private static final String LOG_TAG = "PhoneDisplayMessage";
    private static AlertDialog sDisplayMessageDialog;

    static {
        DBG = SystemProperties.getInt("ro.debuggable", 0) == 1;
        sDisplayMessageDialog = null;
    }

    public static void displayNetworkMessage(Context context, String str) {
        if (DBG) {
            log("displayInfoRecord: infoMsg=" + str);
        }
        displayMessage(context, (String) context.getText(R.string.network_info_message), str);
    }

    public static void displayErrorMessage(Context context, String str) {
        if (DBG) {
            log("displayErrorMessage: errorMsg=" + str);
        }
        displayMessage(context, (String) context.getText(R.string.network_error_message), str);
    }

    public static void displayMessage(Context context, String str, String str2) {
        if (DBG) {
            log("displayMessage: msg=" + str2);
        }
        if (sDisplayMessageDialog != null) {
            sDisplayMessageDialog.dismiss();
        }
        sDisplayMessageDialog = new AlertDialog.Builder(context).setIcon(android.R.drawable.ic_dialog_info).setTitle(str).setMessage(str2).setCancelable(true).create();
        sDisplayMessageDialog.getWindow().setType(2008);
        sDisplayMessageDialog.getWindow().addFlags(2);
        sDisplayMessageDialog.show();
        PhoneGlobals.getInstance().wakeUpScreen();
    }

    public static void dismissMessage() {
        if (DBG) {
            log("Dissmissing Display Info Record...");
        }
        if (sDisplayMessageDialog != null) {
            sDisplayMessageDialog.dismiss();
            sDisplayMessageDialog = null;
        }
    }

    private static void log(String str) {
        Log.d(LOG_TAG, "[PhoneDisplayMessage] " + str);
    }
}
