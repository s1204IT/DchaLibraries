package com.android.packageinstaller.permission.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.BenesseExtension;
import android.os.Bundle;
import android.util.Log;
import com.android.packageinstaller.R;

public class OverlayWarningDialog extends Activity implements DialogInterface.OnClickListener, DialogInterface.OnDismissListener {
    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        new AlertDialog.Builder(this).setTitle(R.string.screen_overlay_title).setMessage(R.string.screen_overlay_message).setPositiveButton(R.string.screen_overlay_button, this).setOnDismissListener(this).show();
    }

    @Override
    public void onDismiss(DialogInterface dialogInterface) {
        finish();
    }

    @Override
    public void onClick(DialogInterface dialogInterface, int i) {
        finish();
        if (BenesseExtension.getDchaState() != 0) {
            return;
        }
        try {
            startActivity(new Intent("android.settings.action.MANAGE_OVERLAY_PERMISSION"));
        } catch (ActivityNotFoundException e) {
            Log.w("OverlayWarningDialog", "No manage overlay settings", e);
        }
    }
}
