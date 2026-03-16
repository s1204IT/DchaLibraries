package com.android.contacts.common.vcard;

import android.R;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;

public class ImportVCardDialogFragment extends DialogFragment {

    public interface Listener {
        void onImportVCardConfirmed();

        void onImportVCardDenied();
    }

    public static void show(Activity activity) {
        if (!(activity instanceof Listener)) {
            throw new IllegalArgumentException("Activity must implement " + Listener.class.getName());
        }
        ImportVCardDialogFragment dialog = new ImportVCardDialogFragment();
        dialog.show(activity.getFragmentManager(), "importVCardDialogFragment");
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new AlertDialog.Builder(getActivity()).setIconAttribute(R.attr.alertDialogIcon).setMessage(com.android.contacts.R.string.import_from_vcf_file_confirmation_message).setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int whichButton) {
                Listener listener = (Listener) ImportVCardDialogFragment.this.getActivity();
                if (listener != null) {
                    listener.onImportVCardConfirmed();
                }
            }
        }).setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int whichButton) {
                Listener listener = (Listener) ImportVCardDialogFragment.this.getActivity();
                if (listener != null) {
                    listener.onImportVCardDenied();
                }
            }
        }).create();
    }
}
