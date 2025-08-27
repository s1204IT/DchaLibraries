package com.android.settings.fuelgauge;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import com.android.settings.R;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;

/* loaded from: classes.dex */
public class ButtonActionDialogFragment extends InstrumentedDialogFragment implements DialogInterface.OnClickListener {
    int mId;

    interface AppButtonsDialogListener {
        void handleDialogClick(int i);
    }

    public static ButtonActionDialogFragment newInstance(int i) {
        ButtonActionDialogFragment buttonActionDialogFragment = new ButtonActionDialogFragment();
        Bundle bundle = new Bundle(1);
        bundle.putInt("id", i);
        buttonActionDialogFragment.setArguments(bundle);
        return buttonActionDialogFragment;
    }

    @Override // com.android.settingslib.core.instrumentation.Instrumentable
    public int getMetricsCategory() {
        return 558;
    }

    @Override // android.app.DialogFragment
    public Dialog onCreateDialog(Bundle bundle) {
        this.mId = getArguments().getInt("id");
        AlertDialog alertDialogCreateDialog = createDialog(this.mId);
        if (alertDialogCreateDialog == null) {
            throw new IllegalArgumentException("unknown id " + this.mId);
        }
        return alertDialogCreateDialog;
    }

    @Override // android.content.DialogInterface.OnClickListener
    public void onClick(DialogInterface dialogInterface, int i) {
        ((AppButtonsDialogListener) getTargetFragment()).handleDialogClick(this.mId);
    }

    private AlertDialog createDialog(int i) {
        Context context = getContext();
        switch (i) {
            case 0:
            case 1:
                return new AlertDialog.Builder(context).setMessage(R.string.app_disable_dlg_text).setPositiveButton(R.string.app_disable_dlg_positive, this).setNegativeButton(R.string.dlg_cancel, (DialogInterface.OnClickListener) null).create();
            case 2:
                return new AlertDialog.Builder(context).setTitle(R.string.force_stop_dlg_title).setMessage(R.string.force_stop_dlg_text).setPositiveButton(R.string.dlg_ok, this).setNegativeButton(R.string.dlg_cancel, (DialogInterface.OnClickListener) null).create();
            default:
                return null;
        }
    }
}
