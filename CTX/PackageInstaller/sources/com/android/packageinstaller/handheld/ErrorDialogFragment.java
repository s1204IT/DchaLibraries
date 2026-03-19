package com.android.packageinstaller.handheld;

import android.R;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import com.android.packageinstaller.UninstallerActivity;

public class ErrorDialogFragment extends DialogFragment {
    @Override
    public Dialog onCreateDialog(Bundle bundle) {
        AlertDialog.Builder positiveButton = new AlertDialog.Builder(getActivity()).setMessage(getArguments().getInt("com.android.packageinstaller.arg.text")).setPositiveButton(R.string.ok, (DialogInterface.OnClickListener) null);
        if (getArguments().containsKey("com.android.packageinstaller.arg.title")) {
            positiveButton.setTitle(getArguments().getInt("com.android.packageinstaller.arg.title"));
        }
        return positiveButton.create();
    }

    @Override
    public void onDismiss(DialogInterface dialogInterface) {
        super.onDismiss(dialogInterface);
        if (isAdded()) {
            if (getActivity() instanceof UninstallerActivity) {
                ((UninstallerActivity) getActivity()).dispatchAborted();
            }
            getActivity().setResult(1);
            getActivity().finish();
        }
    }
}
