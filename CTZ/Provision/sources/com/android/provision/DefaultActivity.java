package com.android.provision;

import android.app.Activity;
import android.content.ComponentName;
import android.os.Bundle;
import android.provider.Settings;

public class DefaultActivity extends Activity {
    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        Settings.Global.putInt(getContentResolver(), "device_provisioned", 1);
        Settings.Secure.putInt(getContentResolver(), "user_setup_complete", 1);
        getPackageManager().setComponentEnabledSetting(new ComponentName(this, (Class<?>) DefaultActivity.class), 2, 1);
        finish();
    }
}
