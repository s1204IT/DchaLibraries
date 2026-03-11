package com.android.settings.deviceinfo.firmwareversion;

import com.android.settings.R;
import com.android.settingslib.DeviceInfoUtils;

public class KernelVersionDialogController {
    static int KERNEL_VERSION_VALUE_ID = R.id.kernel_version_value;
    private final FirmwareVersionDialogFragment mDialog;

    public KernelVersionDialogController(FirmwareVersionDialogFragment firmwareVersionDialogFragment) {
        this.mDialog = firmwareVersionDialogFragment;
    }

    public void initialize() {
        this.mDialog.setText(KERNEL_VERSION_VALUE_ID, DeviceInfoUtils.getFormattedKernelVersion(this.mDialog.getContext()));
    }
}
