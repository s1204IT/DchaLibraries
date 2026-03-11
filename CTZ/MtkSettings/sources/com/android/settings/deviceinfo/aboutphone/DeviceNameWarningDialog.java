package com.android.settings.deviceinfo.aboutphone;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.os.Bundle;
import com.android.settings.R;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;

public class DeviceNameWarningDialog extends InstrumentedDialogFragment implements DialogInterface.OnClickListener {
    public static void show(MyDeviceInfoFragment myDeviceInfoFragment) {
        FragmentManager fragmentManager = myDeviceInfoFragment.getActivity().getFragmentManager();
        if (fragmentManager.findFragmentByTag("DeviceNameWarningDlg") != null) {
            return;
        }
        DeviceNameWarningDialog deviceNameWarningDialog = new DeviceNameWarningDialog();
        deviceNameWarningDialog.setTargetFragment(myDeviceInfoFragment, 0);
        deviceNameWarningDialog.show(fragmentManager, "DeviceNameWarningDlg");
    }

    @Override
    public int getMetricsCategory() {
        return 1219;
    }

    @Override
    public Dialog onCreateDialog(Bundle bundle) {
        return new AlertDialog.Builder(getActivity()).setTitle(R.string.my_device_info_device_name_preference_title).setMessage(R.string.about_phone_device_name_warning).setCancelable(false).setPositiveButton(android.R.string.ok, this).setNegativeButton(android.R.string.cancel, this).create();
    }

    @Override
    public void onClick(DialogInterface dialogInterface, int i) {
        MyDeviceInfoFragment myDeviceInfoFragment = (MyDeviceInfoFragment) getTargetFragment();
        if (i == -1) {
            myDeviceInfoFragment.onSetDeviceNameConfirm();
        }
    }
}
