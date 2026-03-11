package com.android.settings.fingerprint;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import com.android.settings.R;
import com.android.setupwizardlib.util.SystemBarHelper;

public class SetupSkipDialog extends DialogFragment implements DialogInterface.OnClickListener {
    public static SetupSkipDialog newInstance(boolean isFrpSupported) {
        SetupSkipDialog dialog = new SetupSkipDialog();
        Bundle args = new Bundle();
        args.putBoolean("frp_supported", isFrpSupported);
        dialog.setArguments(args);
        return dialog;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog dialog = onCreateDialogBuilder().create();
        SystemBarHelper.hideSystemBars(dialog);
        return dialog;
    }

    @NonNull
    public AlertDialog.Builder onCreateDialogBuilder() {
        Bundle args = getArguments();
        return new AlertDialog.Builder(getContext()).setPositiveButton(R.string.skip_anyway_button_label, this).setNegativeButton(R.string.go_back_button_label, this).setMessage(args.getBoolean("frp_supported") ? R.string.lock_screen_intro_skip_dialog_text_frp : R.string.lock_screen_intro_skip_dialog_text);
    }

    @Override
    public void onClick(DialogInterface dialog, int button) {
        switch (button) {
            case -1:
                Activity activity = getActivity();
                activity.setResult(11);
                activity.finish();
                break;
        }
    }

    public void show(FragmentManager manager) {
        show(manager, "skip_dialog");
    }
}
