package com.android.settings.development;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Bundle;
import com.android.settings.R;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;

public class OemLockInfoDialog extends InstrumentedDialogFragment {
    public static void show(Fragment fragment) {
        FragmentManager childFragmentManager = fragment.getChildFragmentManager();
        if (childFragmentManager.findFragmentByTag("OemLockInfoDialog") == null) {
            new OemLockInfoDialog().show(childFragmentManager, "OemLockInfoDialog");
        }
    }

    @Override
    public int getMetricsCategory() {
        return 1238;
    }

    @Override
    public Dialog onCreateDialog(Bundle bundle) {
        return new AlertDialog.Builder(getActivity()).setMessage(R.string.oem_lock_info_message).create();
    }
}
