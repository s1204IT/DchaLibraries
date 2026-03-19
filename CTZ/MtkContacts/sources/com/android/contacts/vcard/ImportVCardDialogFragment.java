package com.android.contacts.vcard;

import android.R;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;

public class ImportVCardDialogFragment extends DialogFragment {

    public interface Listener {
        void onImportVCardConfirmed(Uri uri, String str);

        void onImportVCardDenied();
    }

    public static void show(Activity activity, Uri uri, String str) {
        if (!(activity instanceof Listener)) {
            throw new IllegalArgumentException("Activity must implement " + Listener.class.getName());
        }
        Bundle bundle = new Bundle();
        bundle.putParcelable("sourceUri", uri);
        bundle.putString("sourceDisplayName", str);
        ImportVCardDialogFragment importVCardDialogFragment = new ImportVCardDialogFragment();
        importVCardDialogFragment.setArguments(bundle);
        importVCardDialogFragment.show(activity.getFragmentManager(), "importVCardDialog");
    }

    @Override
    public Dialog onCreateDialog(Bundle bundle) {
        final Uri uri = (Uri) getArguments().getParcelable("sourceUri");
        final String string = getArguments().getString("sourceDisplayName");
        return new AlertDialog.Builder(getActivity()).setIconAttribute(R.attr.alertDialogIcon).setMessage(com.android.contacts.R.string.import_from_vcf_file_confirmation_message).setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Listener listener = (Listener) ImportVCardDialogFragment.this.getActivity();
                if (listener != null) {
                    listener.onImportVCardConfirmed(uri, string);
                }
            }
        }).setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Listener listener = (Listener) ImportVCardDialogFragment.this.getActivity();
                if (listener != null) {
                    listener.onImportVCardDenied();
                }
            }
        }).create();
    }

    @Override
    public void onCancel(DialogInterface dialogInterface) {
        Listener listener = (Listener) getActivity();
        if (listener != null) {
            listener.onImportVCardDenied();
        }
    }
}
