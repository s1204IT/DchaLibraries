package com.android.settings;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.UserHandle;
import android.util.Log;

public class RemoteBugreportActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int notificationType = getIntent().getIntExtra("android.app.extra.bugreport_notification_type", -1);
        if (notificationType == 2) {
            AlertDialog dialog = new AlertDialog.Builder(this).setMessage(R.string.sharing_remote_bugreport_dialog_message).setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog2) {
                    RemoteBugreportActivity.this.finish();
                }
            }).setNegativeButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog2, int which) {
                    RemoteBugreportActivity.this.finish();
                }
            }).create();
            dialog.show();
        } else if (notificationType != 1 && notificationType != 3) {
            Log.e("RemoteBugreportActivity", "Incorrect dialog type, no dialog shown. Received: " + notificationType);
        } else {
            AlertDialog dialog2 = new AlertDialog.Builder(this).setTitle(R.string.share_remote_bugreport_dialog_title).setMessage(notificationType == 1 ? R.string.share_remote_bugreport_dialog_message : R.string.share_remote_bugreport_dialog_message_finished).setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog3) {
                    RemoteBugreportActivity.this.finish();
                }
            }).setNegativeButton(R.string.decline_remote_bugreport_action, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog3, int which) {
                    Intent intent = new Intent("com.android.server.action.BUGREPORT_SHARING_DECLINED");
                    RemoteBugreportActivity.this.sendBroadcastAsUser(intent, UserHandle.SYSTEM, "android.permission.DUMP");
                    RemoteBugreportActivity.this.finish();
                }
            }).setPositiveButton(R.string.share_remote_bugreport_action, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog3, int which) {
                    Intent intent = new Intent("com.android.server.action.BUGREPORT_SHARING_ACCEPTED");
                    RemoteBugreportActivity.this.sendBroadcastAsUser(intent, UserHandle.SYSTEM, "android.permission.DUMP");
                    RemoteBugreportActivity.this.finish();
                }
            }).create();
            dialog2.show();
        }
    }
}
