package com.android.phone.settings;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import com.android.phone.R;
import com.android.phone.TimeConsumingPreferenceActivity;

public class VoicemailDialogUtil {
    public static Dialog getDialog(VoicemailSettingsActivity voicemailSettingsActivity, int i) {
        int i2;
        int i3;
        if (i == 500 || i == 400 || i == 501 || i == 502 || i == 600 || i == 800) {
            AlertDialog.Builder builder = new AlertDialog.Builder(voicemailSettingsActivity);
            int i4 = R.string.error_updating_title;
            if (i == 400) {
                i2 = R.string.no_change;
                builder.setNegativeButton(R.string.close_dialog, voicemailSettingsActivity);
            } else if (i == 600) {
                i2 = R.string.vm_changed;
                builder.setNegativeButton(R.string.close_dialog, voicemailSettingsActivity);
            } else {
                if (i != 800) {
                    switch (i) {
                        case TimeConsumingPreferenceActivity.RADIO_OFF_ERROR:
                            i2 = R.string.vm_change_failed;
                            builder.setPositiveButton(R.string.close_dialog, voicemailSettingsActivity);
                            break;
                        case 501:
                            i2 = R.string.fw_change_failed;
                            builder.setPositiveButton(R.string.close_dialog, voicemailSettingsActivity);
                            break;
                        case 502:
                            i2 = R.string.fw_get_in_vm_failed;
                            builder.setPositiveButton(R.string.alert_dialog_yes, voicemailSettingsActivity);
                            builder.setNegativeButton(R.string.alert_dialog_no, voicemailSettingsActivity);
                            break;
                        default:
                            i2 = R.string.exception_error;
                            builder.setNeutralButton(R.string.close_dialog, voicemailSettingsActivity);
                            break;
                    }
                } else {
                    i4 = R.string.tty_mode_option_title;
                    i2 = R.string.tty_mode_not_allowed_video_call;
                    builder.setIconAttribute(android.R.attr.alertDialogIcon);
                    builder.setPositiveButton(R.string.ok, voicemailSettingsActivity);
                }
                builder.setTitle(voicemailSettingsActivity.getText(i4));
                builder.setMessage(voicemailSettingsActivity.getText(i2).toString());
                builder.setCancelable(false);
                AlertDialog alertDialogCreate = builder.create();
                alertDialogCreate.getWindow().addFlags(4);
                return alertDialogCreate;
            }
            i4 = R.string.voicemail;
            builder.setTitle(voicemailSettingsActivity.getText(i4));
            builder.setMessage(voicemailSettingsActivity.getText(i2).toString());
            builder.setCancelable(false);
            AlertDialog alertDialogCreate2 = builder.create();
            alertDialogCreate2.getWindow().addFlags(4);
            return alertDialogCreate2;
        }
        if (i == 601 || i == 602 || i == 603) {
            ProgressDialog progressDialog = new ProgressDialog(voicemailSettingsActivity);
            progressDialog.setTitle(voicemailSettingsActivity.getText(R.string.call_settings));
            progressDialog.setIndeterminate(true);
            progressDialog.setCancelable(false);
            if (i == 601) {
                i3 = R.string.updating_settings;
            } else {
                i3 = i == 603 ? R.string.reverting_settings : R.string.reading_settings;
            }
            progressDialog.setMessage(voicemailSettingsActivity.getText(i3));
            return progressDialog;
        }
        return null;
    }
}
