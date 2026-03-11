package com.android.settings.development;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.os.Bundle;
import com.android.settings.R;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;

public class DisableLogPersistWarningDialog extends InstrumentedDialogFragment implements DialogInterface.OnClickListener, DialogInterface.OnDismissListener {
    public static void show(LogPersistDialogHost logPersistDialogHost) {
        if (!(logPersistDialogHost instanceof Fragment)) {
            return;
        }
        Fragment fragment = (Fragment) logPersistDialogHost;
        FragmentManager fragmentManager = fragment.getActivity().getFragmentManager();
        if (fragmentManager.findFragmentByTag("DisableLogPersistDlg") == null) {
            DisableLogPersistWarningDialog disableLogPersistWarningDialog = new DisableLogPersistWarningDialog();
            disableLogPersistWarningDialog.setTargetFragment(fragment, 0);
            disableLogPersistWarningDialog.show(fragmentManager, "DisableLogPersistDlg");
        }
    }

    @Override
    public int getMetricsCategory() {
        return 1225;
    }

    @Override
    public Dialog onCreateDialog(Bundle bundle) {
        return new AlertDialog.Builder(getActivity()).setTitle(R.string.dev_logpersist_clear_warning_title).setMessage(R.string.dev_logpersist_clear_warning_message).setPositiveButton(android.R.string.yes, this).setNegativeButton(android.R.string.no, this).create();
    }

    @Override
    public void onClick(DialogInterface dialogInterface, int i) {
        LogPersistDialogHost logPersistDialogHost = (LogPersistDialogHost) getTargetFragment();
        if (logPersistDialogHost == null) {
            return;
        }
        if (i == -1) {
            logPersistDialogHost.onDisableLogPersistDialogConfirmed();
        } else {
            logPersistDialogHost.onDisableLogPersistDialogRejected();
        }
    }
}
