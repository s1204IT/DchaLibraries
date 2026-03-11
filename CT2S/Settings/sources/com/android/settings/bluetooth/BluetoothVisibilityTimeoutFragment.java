package com.android.settings.bluetooth;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import com.android.settings.R;

public final class BluetoothVisibilityTimeoutFragment extends DialogFragment implements DialogInterface.OnClickListener {
    private final BluetoothDiscoverableEnabler mDiscoverableEnabler = LocalBluetoothManager.getInstance(getActivity()).getDiscoverableEnabler();

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new AlertDialog.Builder(getActivity()).setTitle(R.string.bluetooth_visibility_timeout).setSingleChoiceItems(R.array.bluetooth_visibility_timeout_entries, this.mDiscoverableEnabler.getDiscoverableTimeoutIndex(), this).setNegativeButton(android.R.string.cancel, (DialogInterface.OnClickListener) null).create();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        this.mDiscoverableEnabler.setDiscoverableTimeout(which);
        dismiss();
    }
}
