package com.android.settings.development;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.os.Bundle;
import com.android.settings.R;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;

/* loaded from: classes.dex */
public class EnableAdbWarningDialog extends InstrumentedDialogFragment implements DialogInterface.OnClickListener, DialogInterface.OnDismissListener {
    public static void show(Fragment fragment) {
        FragmentManager fragmentManager = fragment.getActivity().getFragmentManager();
        if (fragmentManager.findFragmentByTag("EnableAdbDialog") == null) {
            EnableAdbWarningDialog enableAdbWarningDialog = new EnableAdbWarningDialog();
            enableAdbWarningDialog.setTargetFragment(fragment, 0);
            enableAdbWarningDialog.show(fragmentManager, "EnableAdbDialog");
        }
    }

    @Override // com.android.settingslib.core.instrumentation.Instrumentable
    public int getMetricsCategory() {
        return 1222;
    }

    @Override // android.app.DialogFragment
    public Dialog onCreateDialog(Bundle bundle) {
        return new AlertDialog.Builder(getActivity()).setTitle(R.string.adb_warning_title).setMessage(R.string.adb_warning_message).setPositiveButton(android.R.string.yes, this).setNegativeButton(android.R.string.no, this).create();
    }

    @Override // android.content.DialogInterface.OnClickListener
    public void onClick(DialogInterface dialogInterface, int i) {
        AdbDialogHost adbDialogHost = (AdbDialogHost) getTargetFragment();
        if (adbDialogHost == null) {
            return;
        }
        if (i == -1) {
            adbDialogHost.onEnableAdbDialogConfirmed();
        } else {
            adbDialogHost.onEnableAdbDialogDismissed();
        }
    }

    @Override // android.app.DialogFragment, android.content.DialogInterface.OnDismissListener
    public void onDismiss(DialogInterface dialogInterface) {
        super.onDismiss(dialogInterface);
        AdbDialogHost adbDialogHost = (AdbDialogHost) getTargetFragment();
        if (adbDialogHost == null) {
            return;
        }
        adbDialogHost.onEnableAdbDialogDismissed();
    }
}
