package com.android.settings;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.admin.DevicePolicyManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.RemoteException;
import android.view.WindowManagerGlobal;

public class MonitoringCertInfoActivity extends Activity implements DialogInterface.OnClickListener {
    private boolean hasDeviceOwner = false;

    @Override
    protected void onCreate(Bundle savedStates) {
        int buttonLabel;
        super.onCreate(savedStates);
        DevicePolicyManager dpm = (DevicePolicyManager) getSystemService("device_policy");
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.ssl_ca_cert_dialog_title);
        builder.setCancelable(true);
        this.hasDeviceOwner = dpm.getDeviceOwner() != null;
        if (this.hasDeviceOwner) {
            String message = getResources().getString(R.string.ssl_ca_cert_info_message, dpm.getDeviceOwnerName());
            builder.setMessage(message);
            buttonLabel = R.string.done_button;
        } else {
            builder.setIcon(android.R.drawable.stat_notify_error);
            builder.setMessage(R.string.ssl_ca_cert_warning_message);
            buttonLabel = R.string.ssl_ca_cert_settings_button;
        }
        builder.setPositiveButton(buttonLabel, this);
        Dialog dialog = builder.create();
        dialog.getWindow().setType(2003);
        try {
            WindowManagerGlobal.getWindowManagerService().dismissKeyguard();
        } catch (RemoteException e) {
        }
        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog2) {
                MonitoringCertInfoActivity.this.finish();
            }
        });
        dialog.show();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (this.hasDeviceOwner) {
            finish();
            return;
        }
        Intent intent = new Intent("com.android.settings.TRUSTED_CREDENTIALS_USER");
        intent.setFlags(335544320);
        startActivity(intent);
        finish();
    }
}
