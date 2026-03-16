package com.android.server.telecom;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;

public class ErrorDialogActivity extends Activity {
    private static final String TAG = ErrorDialogActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        if (getIntent().getBooleanExtra("show_missing_voicemail", false)) {
            showMissingVoicemailErrorDialog();
            return;
        }
        int intExtra = getIntent().getIntExtra("error_message_id", -1);
        if (intExtra == -1) {
            Log.w(TAG, "ErrorDialogActivity called with no error type extra.", new Object[0]);
            finish();
        } else {
            showGenericErrorDialog(intExtra);
        }
    }

    private void showGenericErrorDialog(int i) {
        CharSequence text = getResources().getText(i);
        DialogInterface.OnClickListener onClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i2) {
                ErrorDialogActivity.this.finish();
            }
        };
        new AlertDialog.Builder(this).setMessage(text).setPositiveButton(android.R.string.ok, onClickListener).setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                ErrorDialogActivity.this.finish();
            }
        }).create().show();
    }

    private void showMissingVoicemailErrorDialog() {
        new AlertDialog.Builder(this).setTitle(R.string.no_vm_number).setMessage(R.string.no_vm_number_msg).setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                ErrorDialogActivity.this.finish();
            }
        }).setNegativeButton(R.string.add_vm_number_str, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                ErrorDialogActivity.this.addVoiceMailNumberPanel(dialogInterface);
            }
        }).setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                ErrorDialogActivity.this.finish();
            }
        }).show();
    }

    private void addVoiceMailNumberPanel(DialogInterface dialogInterface) {
        if (dialogInterface != null) {
            dialogInterface.dismiss();
        }
        startActivity(new Intent("com.android.phone.CallFeaturesSetting.ADD_VOICEMAIL"));
        finish();
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(0, 0);
    }
}
