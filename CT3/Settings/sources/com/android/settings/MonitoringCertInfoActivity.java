package com.android.settings;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.admin.DevicePolicyManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.UserHandle;
import com.android.settingslib.RestrictedLockUtils;

public class MonitoringCertInfoActivity extends Activity implements DialogInterface.OnClickListener, DialogInterface.OnDismissListener {
    private int mUserId;

    @Override
    protected void onCreate(Bundle savedStates) {
        int titleId;
        super.onCreate(savedStates);
        this.mUserId = getIntent().getIntExtra("android.intent.extra.USER_ID", UserHandle.myUserId());
        DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(DevicePolicyManager.class);
        int numberOfCertificates = getIntent().getIntExtra("android.settings.extra.number_of_certificates", 1);
        if (RestrictedLockUtils.getProfileOrDeviceOwner(this, this.mUserId) != null) {
            titleId = R.plurals.ssl_ca_cert_settings_button;
        } else {
            titleId = R.plurals.ssl_ca_cert_dialog_title;
        }
        CharSequence title = getResources().getQuantityText(titleId, numberOfCertificates);
        setTitle(title);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);
        builder.setCancelable(true);
        builder.setPositiveButton(getResources().getQuantityText(R.plurals.ssl_ca_cert_settings_button, numberOfCertificates), this);
        builder.setNeutralButton(R.string.cancel, (DialogInterface.OnClickListener) null);
        builder.setOnDismissListener(this);
        if (dpm.getProfileOwnerAsUser(this.mUserId) != null) {
            builder.setMessage(getResources().getQuantityString(R.plurals.ssl_ca_cert_info_message, numberOfCertificates, dpm.getProfileOwnerNameAsUser(this.mUserId)));
        } else if (dpm.getDeviceOwnerComponentOnCallingUser() != null) {
            builder.setMessage(getResources().getQuantityString(R.plurals.ssl_ca_cert_info_message_device_owner, numberOfCertificates, dpm.getDeviceOwnerNameOnAnyUser()));
        } else {
            builder.setIcon(android.R.drawable.stat_notify_error);
            builder.setMessage(R.string.ssl_ca_cert_warning_message);
        }
        builder.show();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        Intent intent = new Intent("com.android.settings.TRUSTED_CREDENTIALS_USER");
        intent.setFlags(335544320);
        intent.putExtra("ARG_SHOW_NEW_FOR_USER", this.mUserId);
        startActivity(intent);
        finish();
    }

    @Override
    public void onDismiss(DialogInterface dialogInterface) {
        finish();
    }
}
