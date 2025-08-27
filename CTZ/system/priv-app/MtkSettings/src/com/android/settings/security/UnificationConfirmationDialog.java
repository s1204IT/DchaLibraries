package com.android.settings.security;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.os.Bundle;
import com.android.settings.R;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;

/* loaded from: classes.dex */
public class UnificationConfirmationDialog extends InstrumentedDialogFragment {
    public static UnificationConfirmationDialog newInstance(boolean z) {
        UnificationConfirmationDialog unificationConfirmationDialog = new UnificationConfirmationDialog();
        Bundle bundle = new Bundle();
        bundle.putBoolean("compliant", z);
        unificationConfirmationDialog.setArguments(bundle);
        return unificationConfirmationDialog;
    }

    public void show(SecuritySettings securitySettings) {
        FragmentManager childFragmentManager = securitySettings.getChildFragmentManager();
        if (childFragmentManager.findFragmentByTag("unification_dialog") == null) {
            show(childFragmentManager, "unification_dialog");
        }
    }

    @Override // android.app.DialogFragment
    public Dialog onCreateDialog(Bundle bundle) {
        final SecuritySettings securitySettings = (SecuritySettings) getParentFragment();
        final boolean z = getArguments().getBoolean("compliant");
        return new AlertDialog.Builder(getActivity()).setTitle(R.string.lock_settings_profile_unification_dialog_title).setMessage(z ? R.string.lock_settings_profile_unification_dialog_body : R.string.lock_settings_profile_unification_dialog_uncompliant_body).setPositiveButton(z ? R.string.lock_settings_profile_unification_dialog_confirm : R.string.lock_settings_profile_unification_dialog_uncompliant_confirm, new DialogInterface.OnClickListener() { // from class: com.android.settings.security.-$$Lambda$UnificationConfirmationDialog$-wYUc2a9Y89ehsHG44vpFDdnSk8
            @Override // android.content.DialogInterface.OnClickListener
            public final void onClick(DialogInterface dialogInterface, int i) {
                UnificationConfirmationDialog.lambda$onCreateDialog$0(z, securitySettings, dialogInterface, i);
            }
        }).setNegativeButton(R.string.cancel, (DialogInterface.OnClickListener) null).create();
    }

    static /* synthetic */ void lambda$onCreateDialog$0(boolean z, SecuritySettings securitySettings, DialogInterface dialogInterface, int i) {
        if (z) {
            securitySettings.launchConfirmDeviceLockForUnification();
        } else {
            securitySettings.unifyUncompliantLocks();
        }
    }

    @Override // android.app.DialogFragment, android.content.DialogInterface.OnDismissListener
    public void onDismiss(DialogInterface dialogInterface) {
        super.onDismiss(dialogInterface);
        ((SecuritySettings) getParentFragment()).updateUnificationPreference();
    }

    @Override // com.android.settingslib.core.instrumentation.Instrumentable
    public int getMetricsCategory() {
        return 532;
    }
}
