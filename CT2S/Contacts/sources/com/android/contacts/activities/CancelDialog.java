package com.android.contacts.activities;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import com.android.contacts.DealWithContactsService;
import com.android.contacts.R;

public class CancelDialog extends DialogFragment {
    public static void show(FragmentManager fragmentManager, int mode) {
        CancelDialog dialog = new CancelDialog();
        Bundle args = new Bundle();
        args.putInt("mode", mode);
        dialog.setArguments(args);
        dialog.show(fragmentManager, "CancelDialog");
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        int mode = getArguments().getInt("mode");
        String message = null;
        if (mode == 0) {
            message = getString(R.string.confirm_import_contacts);
        } else if (1 == mode) {
            message = getString(R.string.confirm_export_contacts);
        } else if (2 == mode) {
            message = getString(R.string.confirm_delete_contacts);
        }
        DialogInterface.OnClickListener okListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                CancelDialog.this.getActivity().stopService(new Intent(CancelDialog.this.getActivity(), (Class<?>) DealWithContactsService.class));
                CancelDialog.this.getActivity().finish();
            }
        };
        DialogInterface.OnClickListener cancelListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                CancelDialog.this.getActivity().finish();
            }
        };
        return new AlertDialog.Builder(getActivity()).setMessage(message).setNegativeButton(android.R.string.cancel, cancelListener).setPositiveButton(android.R.string.ok, okListener).setCancelable(true).create();
    }
}
