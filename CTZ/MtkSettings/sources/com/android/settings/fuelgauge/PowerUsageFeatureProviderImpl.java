package com.android.settings.fuelgauge;

import android.R;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.SparseIntArray;
import com.android.internal.os.BatterySipper;
import com.android.internal.util.ArrayUtils;

public class PowerUsageFeatureProviderImpl implements PowerUsageFeatureProvider {
    private static final String[] PACKAGES_SYSTEM = {"com.android.providers.media", "com.android.providers.calendar", "com.android.systemui"};
    protected Context mContext;
    protected PackageManager mPackageManager;

    public PowerUsageFeatureProviderImpl(Context context) {
        this.mPackageManager = context.getPackageManager();
        this.mContext = context.getApplicationContext();
    }

    @Override
    public boolean isTypeService(BatterySipper batterySipper) {
        return false;
    }

    @Override
    public boolean isTypeSystem(BatterySipper batterySipper) {
        int uid = batterySipper.uidObj == null ? -1 : batterySipper.getUid();
        batterySipper.mPackages = this.mPackageManager.getPackagesForUid(uid);
        if (uid >= 0 && uid < 10000) {
            return true;
        }
        if (batterySipper.mPackages != null) {
            for (String str : batterySipper.mPackages) {
                if (ArrayUtils.contains(PACKAGES_SYSTEM, str)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public Estimate getEnhancedBatteryPrediction(Context context) {
        return null;
    }

    @Override
    public SparseIntArray getEnhancedBatteryPredictionCurve(Context context, long j) {
        return null;
    }

    @Override
    public boolean isEnhancedBatteryPredictionEnabled(Context context) {
        return false;
    }

    @Override
    public String getEnhancedEstimateDebugString(String str) {
        return null;
    }

    @Override
    public boolean isEstimateDebugEnabled() {
        return false;
    }

    @Override
    public String getOldEstimateDebugString(String str) {
        return null;
    }

    @Override
    public String getAdvancedUsageScreenInfoString() {
        return null;
    }

    @Override
    public boolean getEarlyWarningSignal(Context context, String str) {
        return false;
    }

    @Override
    public boolean isSmartBatterySupported() {
        return this.mContext.getResources().getBoolean(R.^attr-private.notificationHeaderAppNameVisibility);
    }
}
