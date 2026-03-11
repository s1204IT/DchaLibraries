package com.android.settings.security;

import android.R;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;

public class ConfigureKeyGuardDialog extends InstrumentedDialogFragment implements DialogInterface.OnClickListener, DialogInterface.OnDismissListener {
    private boolean mConfigureConfirmed;

    @Override
    public int getMetricsCategory() {
        return 1010;
    }

    @Override
    public Dialog onCreateDialog(Bundle bundle) {
        return new AlertDialog.Builder(getActivity()).setTitle(R.string.dialog_alert_title).setMessage(com.android.settings.R.string.credentials_configure_lock_screen_hint).setPositiveButton(com.android.settings.R.string.credentials_configure_lock_screen_button, this).setNegativeButton(R.string.cancel, this).create();
    }

    @Override
    public void onClick(DialogInterface dialogInterface, int i) {
        this.mConfigureConfirmed = i == -1;
    }

    @Override
    public void onDismiss(DialogInterface dialogInterface) {
        if (this.mConfigureConfirmed) {
            this.mConfigureConfirmed = false;
            startPasswordSetup();
        } else {
            Activity activity = getActivity();
            if (activity != null) {
                activity.finish();
            }
        }
    }

    void startPasswordSetup() {
        Intent intent = new Intent("android.app.action.SET_NEW_PASSWORD");
        intent.putExtra("minimum_quality", 65536);
        startActivity(intent);
    }
}
