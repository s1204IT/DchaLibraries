package com.android.settings.deviceinfo.firmwareversion;

import android.os.Build;
import android.text.BidiFormatter;
import com.android.settings.R;

/* loaded from: classes.dex */
public class BuildNumberDialogController {
    static final int BUILD_NUMBER_VALUE_ID = 2131361929;
    private final FirmwareVersionDialogFragment mDialog;

    public BuildNumberDialogController(FirmwareVersionDialogFragment firmwareVersionDialogFragment) {
        this.mDialog = firmwareVersionDialogFragment;
    }

    public void initialize() {
        this.mDialog.setText(R.id.build_number_value, BidiFormatter.getInstance().unicodeWrap(Build.DISPLAY));
    }
}
