package com.android.contacts.editor;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import com.android.contacts.R;

public class SplitContactConfirmationDialogFragment extends DialogFragment {

    public interface Listener {
        void onSplitContactConfirmed();
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.splitConfirmation_title);
        builder.setIconAttribute(android.R.attr.alertDialogIcon);
        builder.setMessage(R.string.splitConfirmation);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Listener targetListener = (Listener) SplitContactConfirmationDialogFragment.this.getTargetFragment();
                targetListener.onSplitContactConfirmed();
            }
        });
        builder.setNegativeButton(android.R.string.cancel, (DialogInterface.OnClickListener) null);
        builder.setCancelable(false);
        return builder.create();
    }
}
