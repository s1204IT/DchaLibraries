package com.android.settings.dashboard;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import com.android.settings.R;

public class NoHomeDialogFragment extends DialogFragment {
    public static void show(Activity parent) {
        NoHomeDialogFragment dialog = new NoHomeDialogFragment();
        dialog.show(parent.getFragmentManager(), (String) null);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new AlertDialog.Builder(getActivity()).setMessage(R.string.only_one_home_message).setPositiveButton(android.R.string.ok, (DialogInterface.OnClickListener) null).create();
    }
}
