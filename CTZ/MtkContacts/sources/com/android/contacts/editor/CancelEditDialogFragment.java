package com.android.contacts.editor;

import android.R;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;

public class CancelEditDialogFragment extends DialogFragment {

    public interface Listener {
        void onCancelEditConfirmed();
    }

    public static void show(ContactEditorFragment contactEditorFragment) {
        CancelEditDialogFragment cancelEditDialogFragment = new CancelEditDialogFragment();
        cancelEditDialogFragment.setTargetFragment(contactEditorFragment, 0);
        cancelEditDialogFragment.show(contactEditorFragment.getFragmentManager(), "cancelEditor");
    }

    @Override
    public Dialog onCreateDialog(Bundle bundle) {
        return new AlertDialog.Builder(getActivity()).setIconAttribute(R.attr.alertDialogIcon).setMessage(com.android.contacts.R.string.cancel_confirmation_dialog_message).setPositiveButton(com.android.contacts.R.string.cancel_confirmation_dialog_cancel_editing_button, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                ((Listener) CancelEditDialogFragment.this.getTargetFragment()).onCancelEditConfirmed();
            }
        }).setNegativeButton(com.android.contacts.R.string.cancel_confirmation_dialog_keep_editing_button, null).create();
    }
}
