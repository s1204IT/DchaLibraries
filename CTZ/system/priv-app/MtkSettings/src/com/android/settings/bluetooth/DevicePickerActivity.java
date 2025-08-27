package com.android.settings.bluetooth;

import android.app.Activity;
import android.os.Bundle;
import com.android.settings.R;

/* loaded from: classes.dex */
public final class DevicePickerActivity extends Activity {
    @Override // android.app.Activity
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        getWindow().addPrivateFlags(524288);
        setContentView(R.layout.bluetooth_device_picker);
    }
}
