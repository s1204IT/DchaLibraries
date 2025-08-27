package com.android.settings.deviceinfo.firmwareversion;

import android.content.Context;
import android.os.SystemProperties;
import com.android.settings.R;
import com.android.settings.Utils;

/* loaded from: classes.dex */
public class BasebandVersionDialogController {
    static final String BASEBAND_PROPERTY = "gsm.version.baseband";
    static final int BASEBAND_VERSION_LABEL_ID = 2131361895;
    static final int BASEBAND_VERSION_VALUE_ID = 2131361896;
    private final FirmwareVersionDialogFragment mDialog;

    public BasebandVersionDialogController(FirmwareVersionDialogFragment firmwareVersionDialogFragment) {
        this.mDialog = firmwareVersionDialogFragment;
    }

    public void initialize() {
        Context context = this.mDialog.getContext();
        if (Utils.isWifiOnly(context)) {
            this.mDialog.removeSettingFromScreen(R.id.baseband_version_label);
            this.mDialog.removeSettingFromScreen(R.id.baseband_version_value);
        } else {
            this.mDialog.setText(R.id.baseband_version_value, SystemProperties.get(BASEBAND_PROPERTY, context.getString(R.string.device_info_default)));
        }
    }
}
