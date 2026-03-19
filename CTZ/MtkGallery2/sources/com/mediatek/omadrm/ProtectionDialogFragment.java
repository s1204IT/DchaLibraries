package com.mediatek.omadrm;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;

public class ProtectionDialogFragment extends DialogFragment {
    private static final String TAG = "OmaDrmUtils/ProtectionDialogFragment";
    private static AlertDialog.Builder mBuilder;

    public static ProtectionDialogFragment newInstance(AlertDialog.Builder builder) {
        mBuilder = builder;
        return new ProtectionDialogFragment();
    }

    @Override
    public Dialog onCreateDialog(Bundle bundle) {
        return mBuilder.create();
    }

    @Override
    public void onPause() {
        Log.d(TAG, "showProtectionInfoDialog: dismiss when activity paused");
        dismissAllowingStateLoss();
        super.onPause();
    }

    @Override
    public void onDismiss(DialogInterface dialogInterface) {
        Log.d(TAG, "showProtectionInfoDialog onDismiss dialog: " + dialogInterface);
        OmaDrmUtils.clearProtectionInfoDialog();
        super.onDismiss(dialogInterface);
    }
}
