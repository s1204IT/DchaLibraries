package com.android.contacts.editor;

import android.R;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;

public class SuggestionEditConfirmationDialogFragment extends DialogFragment {
    public static void show(ContactEditorFragment contactEditorFragment, Uri uri, long j) {
        Bundle bundle = new Bundle();
        bundle.putParcelable("contactUri", uri);
        bundle.putLong("rawContactId", j);
        SuggestionEditConfirmationDialogFragment suggestionEditConfirmationDialogFragment = new SuggestionEditConfirmationDialogFragment();
        suggestionEditConfirmationDialogFragment.setArguments(bundle);
        suggestionEditConfirmationDialogFragment.setTargetFragment(contactEditorFragment, 0);
        suggestionEditConfirmationDialogFragment.show(contactEditorFragment.getFragmentManager(), "edit");
    }

    @Override
    public Dialog onCreateDialog(Bundle bundle) {
        return new AlertDialog.Builder(getActivity()).setIconAttribute(R.attr.alertDialogIcon).setMessage(com.android.contacts.R.string.aggregation_suggestion_edit_dialog_message).setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                ((ContactEditorFragment) SuggestionEditConfirmationDialogFragment.this.getTargetFragment()).doEditSuggestedContact((Uri) SuggestionEditConfirmationDialogFragment.this.getArguments().getParcelable("contactUri"), SuggestionEditConfirmationDialogFragment.this.getArguments().getLong("rawContactId"));
            }
        }).setNegativeButton(R.string.no, null).create();
    }
}
