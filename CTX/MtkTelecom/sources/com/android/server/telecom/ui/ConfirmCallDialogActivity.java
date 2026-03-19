package com.android.server.telecom.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.telecom.Log;
import com.android.server.telecom.R;
import com.android.server.telecom.components.TelecomBroadcastReceiver;

public class ConfirmCallDialogActivity extends Activity {
    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        showDialog(getIntent().getStringExtra("android.telecom.extra.OUTGOING_CALL_ID"), getIntent().getCharSequenceExtra("android.telecom.extra.ONGOING_APP_NAME"));
    }

    private void showDialog(final String str, CharSequence charSequence) {
        Log.i(this, "showDialog: confirming callId=%s, ongoing=%s", new Object[]{str, charSequence});
        new AlertDialog.Builder(this).setMessage(getString(R.string.alert_outgoing_call, new Object[]{charSequence})).setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Intent intent = new Intent("com.android.server.telecom.PROCEED_WITH_CALL", null, ConfirmCallDialogActivity.this, TelecomBroadcastReceiver.class);
                intent.putExtra("android.telecom.extra.OUTGOING_CALL_ID", str);
                ConfirmCallDialogActivity.this.sendBroadcast(intent);
                dialogInterface.dismiss();
                ConfirmCallDialogActivity.this.finish();
            }
        }).setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Intent intent = new Intent("com.android.server.telecom.CANCEL_CALL", null, ConfirmCallDialogActivity.this, TelecomBroadcastReceiver.class);
                intent.putExtra("android.telecom.extra.OUTGOING_CALL_ID", str);
                ConfirmCallDialogActivity.this.sendBroadcast(intent);
                dialogInterface.dismiss();
                ConfirmCallDialogActivity.this.finish();
            }
        }).setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                Intent intent = new Intent("com.android.server.telecom.CANCEL_CALL", null, ConfirmCallDialogActivity.this, TelecomBroadcastReceiver.class);
                intent.putExtra("android.telecom.extra.OUTGOING_CALL_ID", str);
                ConfirmCallDialogActivity.this.sendBroadcast(intent);
                dialogInterface.dismiss();
                ConfirmCallDialogActivity.this.finish();
            }
        }).create().show();
    }
}
