package com.android.settings.development;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.os.Bundle;
import com.android.settings.R;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;

public class ClearAdbKeysWarningDialog extends InstrumentedDialogFragment implements DialogInterface.OnClickListener, DialogInterface.OnDismissListener {
    public static void show(Fragment fragment) {
        FragmentManager fragmentManager = fragment.getActivity().getFragmentManager();
        if (fragmentManager.findFragmentByTag("ClearAdbKeysDlg") == null) {
            ClearAdbKeysWarningDialog clearAdbKeysWarningDialog = new ClearAdbKeysWarningDialog();
            clearAdbKeysWarningDialog.setTargetFragment(fragment, 0);
            clearAdbKeysWarningDialog.show(fragmentManager, "ClearAdbKeysDlg");
        }
    }

    @Override
    public int getMetricsCategory() {
        return 1223;
    }

    @Override
    public Dialog onCreateDialog(Bundle bundle) {
        return new AlertDialog.Builder(getActivity()).setMessage(R.string.adb_keys_warning_message).setPositiveButton(android.R.string.ok, this).setNegativeButton(android.R.string.cancel, (DialogInterface.OnClickListener) null).create();
    }

    @Override
    public void onClick(DialogInterface dialogInterface, int i) {
        AdbClearKeysDialogHost adbClearKeysDialogHost = (AdbClearKeysDialogHost) getTargetFragment();
        if (adbClearKeysDialogHost == null) {
            return;
        }
        adbClearKeysDialogHost.onAdbClearKeysDialogConfirmed();
    }
}
