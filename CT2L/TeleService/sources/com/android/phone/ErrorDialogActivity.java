package com.android.phone;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

public class ErrorDialogActivity extends Activity {
    private static final String TAG = ErrorDialogActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        boolean showVoicemailDialog = getIntent().getBooleanExtra("show_missing_voicemail", false);
        if (showVoicemailDialog) {
            showMissingVoicemailErrorDialog();
            return;
        }
        int error = getIntent().getIntExtra("error_message_id", -1);
        if (error == -1) {
            Log.e(TAG, "ErrorDialogActivity called with no error type extra.");
            finish();
        }
        showGenericErrorDialog(error);
    }

    private void showGenericErrorDialog(int resid) {
        CharSequence msg = getResources().getText(resid);
        DialogInterface.OnClickListener clickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                ErrorDialogActivity.this.finish();
            }
        };
        DialogInterface.OnCancelListener cancelListener = new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                ErrorDialogActivity.this.finish();
            }
        };
        AlertDialog errorDialog = new AlertDialog.Builder(this).setMessage(msg).setPositiveButton(R.string.ok, clickListener).setOnCancelListener(cancelListener).create();
        errorDialog.show();
    }

    private void showMissingVoicemailErrorDialog() {
        new AlertDialog.Builder(this).setTitle(R.string.no_vm_number).setMessage(R.string.no_vm_number_msg).setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                ErrorDialogActivity.this.dontAddVoiceMailNumber();
            }
        }).setNegativeButton(R.string.add_vm_number_str, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                ErrorDialogActivity.this.addVoiceMailNumberPanel(dialog);
            }
        }).setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                ErrorDialogActivity.this.dontAddVoiceMailNumber();
            }
        }).show();
    }

    private void addVoiceMailNumberPanel(DialogInterface dialog) {
        if (dialog != null) {
            dialog.dismiss();
        }
        Intent intent = new Intent("com.android.phone.CallFeaturesSetting.ADD_VOICEMAIL");
        intent.setClass(this, CallFeaturesSetting.class);
        startActivity(intent);
        finish();
    }

    private void dontAddVoiceMailNumber() {
        finish();
    }
}
