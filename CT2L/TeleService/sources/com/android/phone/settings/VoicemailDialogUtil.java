package com.android.phone.settings;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import com.android.phone.CallFeaturesSetting;
import com.android.phone.R;

public class VoicemailDialogUtil {
    public static Dialog getDialog(CallFeaturesSetting parent, int id) {
        int msgId;
        if (id == 500 || id == 400 || id == 501 || id == 502 || id == 600) {
            AlertDialog.Builder b = new AlertDialog.Builder(parent);
            int titleId = R.string.error_updating_title;
            switch (id) {
                case 400:
                    msgId = R.string.no_change;
                    titleId = R.string.voicemail;
                    b.setNegativeButton(R.string.close_dialog, parent);
                    break;
                case 500:
                    msgId = R.string.vm_change_failed;
                    b.setPositiveButton(R.string.close_dialog, parent);
                    break;
                case 501:
                    msgId = R.string.fw_change_failed;
                    b.setPositiveButton(R.string.close_dialog, parent);
                    break;
                case 502:
                    msgId = R.string.fw_get_in_vm_failed;
                    b.setPositiveButton(R.string.alert_dialog_yes, parent);
                    b.setNegativeButton(R.string.alert_dialog_no, parent);
                    break;
                case 600:
                    msgId = R.string.vm_changed;
                    titleId = R.string.voicemail;
                    b.setNegativeButton(R.string.close_dialog, parent);
                    break;
                default:
                    msgId = R.string.exception_error;
                    b.setNeutralButton(R.string.close_dialog, parent);
                    break;
            }
            b.setTitle(parent.getText(titleId));
            String message = parent.getText(msgId).toString();
            b.setMessage(message);
            b.setCancelable(false);
            AlertDialog dialog = b.create();
            dialog.getWindow().addFlags(4);
            return dialog;
        }
        if (id == 601 || id == 602 || id == 603) {
            ProgressDialog dialog2 = new ProgressDialog(parent);
            dialog2.setTitle(parent.getText(R.string.call_settings));
            dialog2.setIndeterminate(true);
            dialog2.setCancelable(false);
            dialog2.setMessage(parent.getText(id == 601 ? R.string.updating_settings : id == 603 ? R.string.reverting_settings : R.string.reading_settings));
            return dialog2;
        }
        return null;
    }
}
