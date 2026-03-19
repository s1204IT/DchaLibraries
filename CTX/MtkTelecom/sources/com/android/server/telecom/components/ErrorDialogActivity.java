package com.android.server.telecom.components;

import android.R;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.telecom.Log;
import android.telephony.SubscriptionManager;

public class ErrorDialogActivity extends Activity {
    private static final String TAG = ErrorDialogActivity.class.getSimpleName();
    private AlertDialog mErrorDialog;
    private Intent mIntent = null;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        this.mIntent = getIntent();
    }

    private void showGenericErrorDialog(CharSequence charSequence) {
        DialogInterface.OnClickListener onClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                ErrorDialogActivity.this.finish();
            }
        };
        this.mErrorDialog = new AlertDialog.Builder(this).setMessage(charSequence).setPositiveButton(R.string.ok, onClickListener).setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                ErrorDialogActivity.this.finish();
            }
        }).create();
        this.mErrorDialog.show();
    }

    private void showErrorDialog() {
        if (this.mIntent.getBooleanExtra("show_missing_voicemail", false)) {
            showMissingVoicemailErrorDialog();
            return;
        }
        if (this.mIntent.getCharSequenceExtra("error_message_string") != null) {
            showGenericErrorDialog(this.mIntent.getCharSequenceExtra("error_message_string"));
            return;
        }
        int intExtra = this.mIntent.getIntExtra("error_message_id", -1);
        if (intExtra == -1) {
            Log.w(TAG, "ErrorDialogActivity called with no error type extra.", new Object[0]);
            finish();
        } else {
            showGenericErrorDialog(intExtra);
        }
    }

    private void showGenericErrorDialog(int i) {
        showGenericErrorDialog(getResources().getText(i));
    }

    private void showMissingVoicemailErrorDialog() {
        this.mErrorDialog = new AlertDialog.Builder(this).setTitle(com.android.server.telecom.R.string.no_vm_number).setMessage(com.android.server.telecom.R.string.no_vm_number_msg).setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                ErrorDialogActivity.this.finish();
            }
        }).setNegativeButton(com.android.server.telecom.R.string.add_vm_number_str, new DialogInterface.OnClickListener() {
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

    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        this.mIntent = intent;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (this.mErrorDialog != null) {
            this.mErrorDialog.dismiss();
            this.mErrorDialog = null;
        }
        if (this.mIntent.getBooleanExtra("show_roaming_alert_dialog", false)) {
            showRoamingAlertDialog();
        } else {
            showErrorDialog();
        }
    }

    private void showRoamingAlertDialog() {
        Log.d(TAG, "showRoamingAlertDialog", new Object[0]);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        if (this.mIntent.getBooleanExtra("is_domestic_roaming", false)) {
            builder.setMessage(com.android.server.telecom.R.string.roaming_network_dom);
        } else {
            builder.setMessage(com.android.server.telecom.R.string.roaming_network_int);
        }
        builder.setTitle(R.string.dialog_alert_title).setIcon(R.drawable.ic_dialog_alert).setPositiveButton(com.android.server.telecom.R.string.modify_roaming_settings, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Log.d(ErrorDialogActivity.TAG, "positive button clicked, show roaming settings", new Object[0]);
                ErrorDialogActivity.this.showRoamingSettings();
                ErrorDialogActivity.this.onDialogDismissed();
                ErrorDialogActivity.this.finish();
            }
        }).setNegativeButton(com.android.server.telecom.R.string.continue_roaming, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Log.d(ErrorDialogActivity.TAG, "negative button clicked, make call in roaming", new Object[0]);
                ErrorDialogActivity.this.makeCall();
                ErrorDialogActivity.this.onDialogDismissed();
                ErrorDialogActivity.this.finish();
            }
        }).setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                Log.d(ErrorDialogActivity.TAG, "cancel button clicked", new Object[0]);
                ErrorDialogActivity.this.onDialogDismissed();
                ErrorDialogActivity.this.finish();
            }
        }).create();
        this.mErrorDialog = builder.show();
        this.mErrorDialog.getWindow().addFlags(2);
        this.mErrorDialog.getWindow().addFlags(524288);
    }

    private void onDialogDismissed() {
        if (this.mErrorDialog != null) {
            this.mErrorDialog.dismiss();
            this.mErrorDialog = null;
        }
    }

    private void showRoamingSettings() {
        Intent intent = new Intent();
        intent.setAction("com.mediatek.services.telephony.ACTION_SHOW_ROAMING_SETTINGS");
        intent.putExtra("com.android.phone.settings.SubscriptionInfoHelper.SubscriptionId", SubscriptionManager.getDefaultSubscriptionId());
        startActivity(intent);
    }

    private void makeCall() {
        getApplicationContext().sendBroadcast(new Intent("com.mediatek.telecom.plugin.MAKE_CALL"));
        String stringExtra = this.mIntent.getStringExtra("contact_number");
        Intent intent = new Intent("android.intent.action.CALL");
        intent.setData(Uri.parse("tel:" + stringExtra));
        if (this.mIntent.getBooleanExtra("is_video_call", false)) {
            intent.putExtra("android.telecom.extra.START_CALL_WITH_VIDEO_STATE", 3);
        }
        startActivity(intent);
    }
}
