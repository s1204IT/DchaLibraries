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

public class RequestIgnoreBatteryOptimizations extends AlertActivity implements DialogInterface.OnClickListener {
    IDeviceIdleController mDeviceIdleService;
    String mPackageName;

    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
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
        if (((PowerManager) getSystemService(PowerManager.class)).isIgnoringBatteryOptimizations(this.mPackageName)) {
            Log.i("RequestIgnoreBatteryOptimizations", "Not should prompt, already ignoring optimizations: " + this.mPackageName);
            finish();
            return;
        }
        try {
            ApplicationInfo applicationInfo = getPackageManager().getApplicationInfo(this.mPackageName, 0);
            if (getPackageManager().checkPermission("android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS", this.mPackageName) != 0) {
                Log.w("RequestIgnoreBatteryOptimizations", "Requested package " + this.mPackageName + " does not hold permission android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS");
                finish();
                return;
            }
            AlertController.AlertParams alertParams = this.mAlertParams;
            alertParams.mTitle = getText(R.string.high_power_prompt_title);
            alertParams.mMessage = getString(R.string.high_power_prompt_body, new Object[]{applicationInfo.loadLabel(getPackageManager())});
            alertParams.mPositiveButtonText = getText(R.string.allow);
            alertParams.mNegativeButtonText = getText(R.string.deny);
            alertParams.mPositiveButtonListener = this;
            alertParams.mNegativeButtonListener = this;
            setupAlert();
        } catch (PackageManager.NameNotFoundException e) {
            Log.w("RequestIgnoreBatteryOptimizations", "Requested package doesn't exist: " + this.mPackageName);
            finish();
        }
    }

    @Override
    public void onClick(DialogInterface dialogInterface, int i) {
        if (i == -1) {
            try {
                this.mDeviceIdleService.addPowerSaveWhitelistApp(this.mPackageName);
            } catch (RemoteException e) {
                Log.w("RequestIgnoreBatteryOptimizations", "Unable to reach IDeviceIdleController", e);
            }
            setResult(-1);
        }
    }
}
