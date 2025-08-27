package com.android.settings;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.UserHandle;
import android.util.Log;

/* loaded from: classes.dex */
public class RemoteBugreportActivity extends Activity {
    @Override // android.app.Activity
    protected void onCreate(Bundle bundle) {
        int i;
        super.onCreate(bundle);
        int intExtra = getIntent().getIntExtra("android.app.extra.bugreport_notification_type", -1);
        if (intExtra == 2) {
            new AlertDialog.Builder(this).setMessage(R.string.sharing_remote_bugreport_dialog_message).setOnDismissListener(new DialogInterface.OnDismissListener() { // from class: com.android.settings.RemoteBugreportActivity.2
                @Override // android.content.DialogInterface.OnDismissListener
                public void onDismiss(DialogInterface dialogInterface) {
                    RemoteBugreportActivity.this.finish();
                }
            }).setNegativeButton(android.R.string.ok, new DialogInterface.OnClickListener() { // from class: com.android.settings.RemoteBugreportActivity.1
                @Override // android.content.DialogInterface.OnClickListener
                public void onClick(DialogInterface dialogInterface, int i2) {
                    RemoteBugreportActivity.this.finish();
                }
            }).create().show();
            return;
        }
        if (intExtra == 1 || intExtra == 3) {
            AlertDialog.Builder title = new AlertDialog.Builder(this).setTitle(R.string.share_remote_bugreport_dialog_title);
            if (intExtra == 1) {
                i = R.string.share_remote_bugreport_dialog_message;
            } else {
                i = R.string.share_remote_bugreport_dialog_message_finished;
            }
            title.setMessage(i).setOnDismissListener(new DialogInterface.OnDismissListener() { // from class: com.android.settings.RemoteBugreportActivity.5
                @Override // android.content.DialogInterface.OnDismissListener
                public void onDismiss(DialogInterface dialogInterface) {
                    RemoteBugreportActivity.this.finish();
                }
            }).setNegativeButton(R.string.decline_remote_bugreport_action, new DialogInterface.OnClickListener() { // from class: com.android.settings.RemoteBugreportActivity.4
                @Override // android.content.DialogInterface.OnClickListener
                public void onClick(DialogInterface dialogInterface, int i2) {
                    RemoteBugreportActivity.this.sendBroadcastAsUser(new Intent("com.android.server.action.REMOTE_BUGREPORT_SHARING_DECLINED"), UserHandle.SYSTEM, "android.permission.DUMP");
                    RemoteBugreportActivity.this.finish();
                }
            }).setPositiveButton(R.string.share_remote_bugreport_action, new DialogInterface.OnClickListener() { // from class: com.android.settings.RemoteBugreportActivity.3
                @Override // android.content.DialogInterface.OnClickListener
                public void onClick(DialogInterface dialogInterface, int i2) {
                    RemoteBugreportActivity.this.sendBroadcastAsUser(new Intent("com.android.server.action.REMOTE_BUGREPORT_SHARING_ACCEPTED"), UserHandle.SYSTEM, "android.permission.DUMP");
                    RemoteBugreportActivity.this.finish();
                }
            }).create().show();
            return;
        }
        Log.e("RemoteBugreportActivity", "Incorrect dialog type, no dialog shown. Received: " + intExtra);
    }
}
