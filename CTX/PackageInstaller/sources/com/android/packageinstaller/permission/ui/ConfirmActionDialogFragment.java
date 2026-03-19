package com.android.packageinstaller.permission.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.ComponentCallbacks2;
import android.content.DialogInterface;
import android.os.Bundle;
import com.android.packageinstaller.R;

public final class ConfirmActionDialogFragment extends DialogFragment {

    public interface OnActionConfirmedListener {
        void onActionConfirmed(String str);
    }

    public static ConfirmActionDialogFragment newInstance(CharSequence charSequence, String str) {
        Bundle bundle = new Bundle();
        bundle.putCharSequence("MESSAGE", charSequence);
        bundle.putString("ACTION", str);
        ConfirmActionDialogFragment confirmActionDialogFragment = new ConfirmActionDialogFragment();
        confirmActionDialogFragment.setArguments(bundle);
        return confirmActionDialogFragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle bundle) {
        return new AlertDialog.Builder(getContext()).setMessage(getArguments().getString("MESSAGE")).setNegativeButton(R.string.cancel, (DialogInterface.OnClickListener) null).setPositiveButton(R.string.grant_dialog_button_deny_anyway, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                ComponentCallbacks2 activity = ConfirmActionDialogFragment.this.getActivity();
                if (activity instanceof OnActionConfirmedListener) {
                    ((OnActionConfirmedListener) activity).onActionConfirmed(ConfirmActionDialogFragment.this.getArguments().getString("ACTION"));
                }
            }
        }).create();
    }
}
