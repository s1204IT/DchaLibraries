package com.android.phone;

import android.app.AlertDialog;
import android.content.Context;
import android.os.SystemProperties;
import android.util.Log;

public class PhoneDisplayMessage {
    private static final boolean DBG;
    private static AlertDialog sDisplayMessageDialog;

    static {
        DBG = SystemProperties.getInt("ro.debuggable", 0) == 1;
        sDisplayMessageDialog = null;
    }

    public static void displayNetworkMessage(Context context, String infoMsg) {
        if (DBG) {
            log("displayInfoRecord: infoMsg=" + infoMsg);
        }
        String title = (String) context.getText(R.string.network_info_message);
        displayMessage(context, title, infoMsg);
    }

    public static void displayErrorMessage(Context context, String errorMsg) {
        if (DBG) {
            log("displayErrorMessage: errorMsg=" + errorMsg);
        }
        String title = (String) context.getText(R.string.network_error_message);
        displayMessage(context, title, errorMsg);
    }

    public static void displayMessage(Context context, String title, String msg) {
        if (DBG) {
            log("displayMessage: msg=" + msg);
        }
        if (sDisplayMessageDialog != null) {
            sDisplayMessageDialog.dismiss();
        }
        sDisplayMessageDialog = new AlertDialog.Builder(context).setIcon(android.R.drawable.ic_dialog_info).setTitle(title).setMessage(msg).setCancelable(true).create();
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

    private static void log(String msg) {
        Log.d("PhoneDisplayMessage", "[PhoneDisplayMessage] " + msg);
    }
}
