package com.android.settings.vpn2;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.os.Bundle;
import com.android.settings.R;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;

/* loaded from: classes.dex */
public class ConfirmLockdownFragment extends InstrumentedDialogFragment implements DialogInterface.OnClickListener {

    public interface ConfirmLockdownListener {
        void onConfirmLockdown(Bundle bundle, boolean z, boolean z2);
    }

    @Override // com.android.settingslib.core.instrumentation.Instrumentable
    public int getMetricsCategory() {
        return 548;
    }

    public static boolean shouldShow(boolean z, boolean z2, boolean z3) {
        return z || (z3 && !z2);
    }

    public static void show(Fragment fragment, boolean z, boolean z2, boolean z3, boolean z4, Bundle bundle) {
        if (fragment.getFragmentManager().findFragmentByTag("ConfirmLockdown") != null) {
            return;
        }
        Bundle bundle2 = new Bundle();
        bundle2.putBoolean("replacing", z);
        bundle2.putBoolean("always_on", z2);
        bundle2.putBoolean("lockdown_old", z3);
        bundle2.putBoolean("lockdown_new", z4);
        bundle2.putParcelable("options", bundle);
        ConfirmLockdownFragment confirmLockdownFragment = new ConfirmLockdownFragment();
        confirmLockdownFragment.setArguments(bundle2);
        confirmLockdownFragment.setTargetFragment(fragment, 0);
        confirmLockdownFragment.show(fragment.getFragmentManager(), "ConfirmLockdown");
    }

    @Override // android.app.DialogFragment
    public Dialog onCreateDialog(Bundle bundle) {
        int i;
        int i2;
        int i3;
        boolean z = getArguments().getBoolean("replacing");
        getArguments().getBoolean("always_on");
        boolean z2 = getArguments().getBoolean("lockdown_old");
        boolean z3 = getArguments().getBoolean("lockdown_new");
        if (z3) {
            i = R.string.vpn_require_connection_title;
        } else {
            i = z ? R.string.vpn_replace_vpn_title : R.string.vpn_set_vpn_title;
        }
        if (z) {
            i2 = R.string.vpn_replace;
        } else {
            i2 = z3 ? R.string.vpn_turn_on : R.string.okay;
        }
        if (z3) {
            if (z) {
                i3 = R.string.vpn_replace_always_on_vpn_enable_message;
            } else {
                i3 = R.string.vpn_first_always_on_vpn_message;
            }
        } else if (z2) {
            i3 = R.string.vpn_replace_always_on_vpn_disable_message;
        } else {
            i3 = R.string.vpn_replace_vpn_message;
        }
        return new AlertDialog.Builder(getActivity()).setTitle(i).setMessage(i3).setNegativeButton(R.string.cancel, (DialogInterface.OnClickListener) null).setPositiveButton(i2, this).create();
    }

    @Override // android.content.DialogInterface.OnClickListener
    public void onClick(DialogInterface dialogInterface, int i) {
        if (getTargetFragment() instanceof ConfirmLockdownListener) {
            ((ConfirmLockdownListener) getTargetFragment()).onConfirmLockdown((Bundle) getArguments().getParcelable("options"), getArguments().getBoolean("always_on"), getArguments().getBoolean("lockdown_new"));
        }
    }
}
