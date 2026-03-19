package com.android.storagemanager.automatic;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import com.android.storagemanager.R;

public class WarningDialogFragment extends DialogFragment implements DialogInterface.OnClickListener {
    public static WarningDialogFragment newInstance() {
        return new WarningDialogFragment();
    }

    @Override
    public Dialog onCreateDialog(Bundle bundle) {
        return new AlertDialog.Builder(getActivity()).setMessage(R.string.automatic_storage_manager_activation_warning).setNegativeButton(android.R.string.ok, this).create();
    }

    @Override
    public void onClick(DialogInterface dialogInterface, int i) {
        finishActivity();
    }

    @Override
    public void onCancel(DialogInterface dialogInterface) {
        finishActivity();
    }

    private void finishActivity() {
        Activity activity = getActivity();
        if (activity != null) {
            activity.finish();
        }
    }
}
