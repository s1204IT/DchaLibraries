package com.android.settings.password;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.os.Bundle;
import com.android.settings.R;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;

/* loaded from: classes.dex */
public class SetupSkipDialog extends InstrumentedDialogFragment implements DialogInterface.OnClickListener {
    public static SetupSkipDialog newInstance(boolean z) {
        SetupSkipDialog setupSkipDialog = new SetupSkipDialog();
        Bundle bundle = new Bundle();
        bundle.putBoolean("frp_supported", z);
        setupSkipDialog.setArguments(bundle);
        return setupSkipDialog;
    }

    @Override // com.android.settingslib.core.instrumentation.Instrumentable
    public int getMetricsCategory() {
        return 573;
    }

    @Override // android.app.DialogFragment
    public Dialog onCreateDialog(Bundle bundle) {
        return onCreateDialogBuilder().create();
    }

    public AlertDialog.Builder onCreateDialogBuilder() {
        int i;
        Bundle arguments = getArguments();
        AlertDialog.Builder title = new AlertDialog.Builder(getContext()).setPositiveButton(R.string.skip_anyway_button_label, this).setNegativeButton(R.string.go_back_button_label, this).setTitle(R.string.lock_screen_intro_skip_title);
        if (arguments.getBoolean("frp_supported")) {
            i = R.string.lock_screen_intro_skip_dialog_text_frp;
        } else {
            i = R.string.lock_screen_intro_skip_dialog_text;
        }
        return title.setMessage(i);
    }

    @Override // android.content.DialogInterface.OnClickListener
    public void onClick(DialogInterface dialogInterface, int i) {
        if (i == -1) {
            Activity activity = getActivity();
            activity.setResult(11);
            activity.finish();
        }
    }

    public void show(FragmentManager fragmentManager) {
        show(fragmentManager, "skip_dialog");
    }
}
