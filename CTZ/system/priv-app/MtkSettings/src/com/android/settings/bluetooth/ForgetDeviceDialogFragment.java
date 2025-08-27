package com.android.settings.bluetooth;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.R;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.LocalBluetoothManager;

/* loaded from: classes.dex */
public class ForgetDeviceDialogFragment extends InstrumentedDialogFragment {
    private CachedBluetoothDevice mDevice;

    public static ForgetDeviceDialogFragment newInstance(String str) {
        Bundle bundle = new Bundle(1);
        bundle.putString("device_address", str);
        ForgetDeviceDialogFragment forgetDeviceDialogFragment = new ForgetDeviceDialogFragment();
        forgetDeviceDialogFragment.setArguments(bundle);
        return forgetDeviceDialogFragment;
    }

    @VisibleForTesting
    CachedBluetoothDevice getDevice(Context context) {
        String string = getArguments().getString("device_address");
        LocalBluetoothManager localBtManager = Utils.getLocalBtManager(context);
        return localBtManager.getCachedDeviceManager().findDevice(localBtManager.getBluetoothAdapter().getRemoteDevice(string));
    }

    @Override // com.android.settingslib.core.instrumentation.Instrumentable
    public int getMetricsCategory() {
        return 1031;
    }

    @Override // android.app.DialogFragment
    public Dialog onCreateDialog(Bundle bundle) {
        DialogInterface.OnClickListener onClickListener = new DialogInterface.OnClickListener() { // from class: com.android.settings.bluetooth.-$$Lambda$ForgetDeviceDialogFragment$EDf2UTKPcHIZGnJUVoyf7QwuxfU
            @Override // android.content.DialogInterface.OnClickListener
            public final void onClick(DialogInterface dialogInterface, int i) {
                ForgetDeviceDialogFragment.lambda$onCreateDialog$0(this.f$0, dialogInterface, i);
            }
        };
        Context context = getContext();
        this.mDevice = getDevice(context);
        AlertDialog alertDialogCreate = new AlertDialog.Builder(context).setPositiveButton(R.string.bluetooth_unpair_dialog_forget_confirm_button, onClickListener).setNegativeButton(android.R.string.cancel, (DialogInterface.OnClickListener) null).create();
        alertDialogCreate.setTitle(R.string.bluetooth_unpair_dialog_title);
        alertDialogCreate.setMessage(context.getString(R.string.bluetooth_unpair_dialog_body, this.mDevice.getName()));
        return alertDialogCreate;
    }

    public static /* synthetic */ void lambda$onCreateDialog$0(ForgetDeviceDialogFragment forgetDeviceDialogFragment, DialogInterface dialogInterface, int i) {
        forgetDeviceDialogFragment.mDevice.unpair();
        Activity activity = forgetDeviceDialogFragment.getActivity();
        if (activity != null) {
            activity.finish();
        }
    }
}
