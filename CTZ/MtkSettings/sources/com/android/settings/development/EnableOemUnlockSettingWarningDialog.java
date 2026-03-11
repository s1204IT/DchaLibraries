package com.android.settings.development;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.os.Bundle;
import com.android.settings.R;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;

public class EnableOemUnlockSettingWarningDialog extends InstrumentedDialogFragment implements DialogInterface.OnClickListener, DialogInterface.OnDismissListener {
    public static void show(Fragment fragment) {
        FragmentManager fragmentManager = fragment.getActivity().getFragmentManager();
        if (fragmentManager.findFragmentByTag("EnableOemUnlockDlg") == null) {
            EnableOemUnlockSettingWarningDialog enableOemUnlockSettingWarningDialog = new EnableOemUnlockSettingWarningDialog();
            enableOemUnlockSettingWarningDialog.setTargetFragment(fragment, 0);
            enableOemUnlockSettingWarningDialog.show(fragmentManager, "EnableOemUnlockDlg");
        }
    }

    @Override
    public int getMetricsCategory() {
        return 1220;
    }

    @Override
    public Dialog onCreateDialog(Bundle bundle) {
        return new AlertDialog.Builder(getActivity()).setTitle(R.string.confirm_enable_oem_unlock_title).setMessage(R.string.confirm_enable_oem_unlock_text).setPositiveButton(R.string.enable_text, this).setNegativeButton(android.R.string.cancel, this).create();
    }

    @Override
    public void onClick(DialogInterface dialogInterface, int i) {
        OemUnlockDialogHost oemUnlockDialogHost = (OemUnlockDialogHost) getTargetFragment();
        if (oemUnlockDialogHost == null) {
            return;
        }
        if (i == -1) {
            oemUnlockDialogHost.onOemUnlockDialogConfirmed();
        } else {
            oemUnlockDialogHost.onOemUnlockDialogDismissed();
        }
    }

    @Override
    public void onDismiss(DialogInterface dialogInterface) {
        super.onDismiss(dialogInterface);
        OemUnlockDialogHost oemUnlockDialogHost = (OemUnlockDialogHost) getTargetFragment();
        if (oemUnlockDialogHost == null) {
            return;
        }
        oemUnlockDialogHost.onOemUnlockDialogDismissed();
    }
}
