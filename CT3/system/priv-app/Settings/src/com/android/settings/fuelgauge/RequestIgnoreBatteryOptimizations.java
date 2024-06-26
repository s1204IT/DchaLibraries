package com.android.settings.fuelgauge;

import android.content.DialogInterface;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.IDeviceIdleController;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;
import com.android.internal.app.AlertActivity;
import com.android.internal.app.AlertController;
import com.android.settings.R;
/* loaded from: classes.dex */
public class RequestIgnoreBatteryOptimizations extends AlertActivity implements DialogInterface.OnClickListener {
    IDeviceIdleController mDeviceIdleService;
    String mPackageName;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.mDeviceIdleService = IDeviceIdleController.Stub.asInterface(ServiceManager.getService("deviceidle"));
        Uri data = getIntent().getData();
        if (data == null) {
            Log.w("RequestIgnoreBatteryOptimizations", "No data supplied for IGNORE_BATTERY_OPTIMIZATION_SETTINGS in: " + getIntent());
            finish();
            return;
        }
        this.mPackageName = data.getSchemeSpecificPart();
        if (this.mPackageName == null) {
            Log.w("RequestIgnoreBatteryOptimizations", "No data supplied for IGNORE_BATTERY_OPTIMIZATION_SETTINGS in: " + getIntent());
            finish();
            return;
        }
        PowerManager power = (PowerManager) getSystemService(PowerManager.class);
        if (power.isIgnoringBatteryOptimizations(this.mPackageName)) {
            Log.i("RequestIgnoreBatteryOptimizations", "Not should prompt, already ignoring optimizations: " + this.mPackageName);
            finish();
            return;
        }
        try {
            ApplicationInfo ai = getPackageManager().getApplicationInfo(this.mPackageName, 0);
            if (getPackageManager().checkPermission("android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS", this.mPackageName) != 0) {
                Log.w("RequestIgnoreBatteryOptimizations", "Requested package " + this.mPackageName + " does not hold permission android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS");
                finish();
                return;
            }
            AlertController.AlertParams p = this.mAlertParams;
            p.mTitle = getText(R.string.high_power_prompt_title);
            p.mMessage = getString(R.string.high_power_prompt_body, new Object[]{ai.loadLabel(getPackageManager())});
            p.mPositiveButtonText = getText(R.string.yes);
            p.mNegativeButtonText = getText(R.string.no);
            p.mPositiveButtonListener = this;
            p.mNegativeButtonListener = this;
            setupAlert();
        } catch (PackageManager.NameNotFoundException e) {
            Log.w("RequestIgnoreBatteryOptimizations", "Requested package doesn't exist: " + this.mPackageName);
            finish();
        }
    }

    @Override // android.content.DialogInterface.OnClickListener
    public void onClick(DialogInterface dialog, int which) {
        switch (which) {
            case -2:
            default:
                return;
            case -1:
                try {
                    this.mDeviceIdleService.addPowerSaveWhitelistApp(this.mPackageName);
                } catch (RemoteException e) {
                    Log.w("RequestIgnoreBatteryOptimizations", "Unable to reach IDeviceIdleController", e);
                }
                setResult(-1);
                return;
        }
    }
}
