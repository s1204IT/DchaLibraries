package com.android.provision;

import android.app.Activity;
import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.Settings;

public class DefaultActivity extends Activity {
    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        Settings.Global.putInt(getContentResolver(), "device_provisioned", 1);
        Settings.Secure.putInt(getContentResolver(), "user_setup_complete", 1);
        PackageManager pm = getPackageManager();
        ComponentName name = new ComponentName(this, (Class<?>) DefaultActivity.class);
        pm.setComponentEnabledSetting(name, 2, 1);
        finish();
    }
}
