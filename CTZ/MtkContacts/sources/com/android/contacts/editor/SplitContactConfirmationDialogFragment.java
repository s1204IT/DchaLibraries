package com.android.contacts.editor;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import com.android.contacts.ContactSaveService;
import com.android.contacts.R;

public class SplitContactConfirmationDialogFragment extends DialogFragment {
    private boolean mHasPendingChanges;

    public interface Listener {
        void onSplitContactCanceled();

        void onSplitContactConfirmed(boolean z);
    }

    public static void show(ContactEditorFragment contactEditorFragment, boolean z) {
        Bundle bundle = new Bundle();
        bundle.putBoolean("hasPendingChanges", z);
        SplitContactConfirmationDialogFragment splitContactConfirmationDialogFragment = new SplitContactConfirmationDialogFragment();
        splitContactConfirmationDialogFragment.setTargetFragment(contactEditorFragment, 0);
        splitContactConfirmationDialogFragment.setArguments(bundle);
        splitContactConfirmationDialogFragment.show(contactEditorFragment.getFragmentManager(), ContactSaveService.ACTION_SPLIT_CONTACT);
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        this.mHasPendingChanges = getArguments() != null && getArguments().getBoolean("hasPendingChanges");
    }

    @Override
    public Dialog onCreateDialog(Bundle bundle) {
        int i;
        int i2;
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        if (this.mHasPendingChanges) {
            i = R.string.splitConfirmationWithPendingChanges;
        } else {
            i = R.string.splitConfirmation;
        }
        builder.setMessage(i);
        if (this.mHasPendingChanges) {
            i2 = R.string.splitConfirmationWithPendingChanges_positive_button;
        } else {
            i2 = R.string.splitConfirmation_positive_button;
        }
        builder.setPositiveButton(i2, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i3) {
                SplitContactConfirmationDialogFragment.this.getListener().onSplitContactConfirmed(SplitContactConfirmationDialogFragment.this.mHasPendingChanges);
            }
        });
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i3) {
                SplitContactConfirmationDialogFragment.this.onCancel(dialogInterface);
            }
        });
        builder.setCancelable(false);
        return builder.create();
    }

    private Listener getListener() {
        if (getTargetFragment() == null) {
            return (Listener) getActivity();
        }
        return (Listener) getTargetFragment();
    }

    @Override
    public void onCancel(DialogInterface dialogInterface) {
        super.onCancel(dialogInterface);
        getListener().onSplitContactCanceled();
    }
}
