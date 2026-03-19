package com.mediatek.gallery3d.video;

import android.R;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.KeyEvent;

public class ErrorDialogFragment extends DialogFragment {
    private static final String KEY_MESSAGE = "message";

    public static ErrorDialogFragment newInstance(int i) {
        ErrorDialogFragment errorDialogFragment = new ErrorDialogFragment();
        Bundle bundle = new Bundle(1);
        bundle.putInt(KEY_MESSAGE, i);
        errorDialogFragment.setArguments(bundle);
        return errorDialogFragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle bundle) {
        Bundle arguments = getArguments();
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                ErrorDialogFragment.this.getActivity().finish();
            }
        }).setCancelable(false);
        if (arguments.getInt(KEY_MESSAGE) > 0) {
            builder.setMessage(getString(arguments.getInt(KEY_MESSAGE)));
        }
        AlertDialog alertDialogCreate = builder.create();
        alertDialogCreate.setCanceledOnTouchOutside(false);
        alertDialogCreate.setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface dialogInterface, int i, KeyEvent keyEvent) {
                if (i == 4 || i == 84 || i == 82) {
                    return true;
                }
                return false;
            }
        });
        return alertDialogCreate;
    }
}
