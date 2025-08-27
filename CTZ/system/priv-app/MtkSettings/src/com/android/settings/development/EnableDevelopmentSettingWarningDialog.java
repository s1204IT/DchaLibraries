package com.android.settings.development;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.os.Bundle;
import com.android.settings.R;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;

/* loaded from: classes.dex */
public class EnableDevelopmentSettingWarningDialog extends InstrumentedDialogFragment implements DialogInterface.OnClickListener {
    public static void show(DevelopmentSettingsDashboardFragment developmentSettingsDashboardFragment) {
        EnableDevelopmentSettingWarningDialog enableDevelopmentSettingWarningDialog = new EnableDevelopmentSettingWarningDialog();
        enableDevelopmentSettingWarningDialog.setTargetFragment(developmentSettingsDashboardFragment, 0);
        FragmentManager fragmentManager = developmentSettingsDashboardFragment.getActivity().getFragmentManager();
        if (fragmentManager.findFragmentByTag("EnableDevSettingDlg") == null) {
            enableDevelopmentSettingWarningDialog.show(fragmentManager, "EnableDevSettingDlg");
        }
    }

    @Override // com.android.settingslib.core.instrumentation.Instrumentable
    public int getMetricsCategory() {
        return 1219;
    }

    @Override // android.app.DialogFragment
    public Dialog onCreateDialog(Bundle bundle) {
        return new AlertDialog.Builder(getActivity()).setMessage(R.string.dev_settings_warning_message).setTitle(R.string.dev_settings_warning_title).setPositiveButton(android.R.string.yes, this).setNegativeButton(android.R.string.no, this).create();
    }

    @Override // android.content.DialogInterface.OnClickListener
    public void onClick(DialogInterface dialogInterface, int i) {
        DevelopmentSettingsDashboardFragment developmentSettingsDashboardFragment = (DevelopmentSettingsDashboardFragment) getTargetFragment();
        if (i == -1) {
            developmentSettingsDashboardFragment.onEnableDevelopmentOptionsConfirmed();
        } else {
            developmentSettingsDashboardFragment.onEnableDevelopmentOptionsRejected();
        }
    }
}
