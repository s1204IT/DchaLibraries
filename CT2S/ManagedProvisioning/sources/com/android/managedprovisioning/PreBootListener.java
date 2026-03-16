package com.android.managedprovisioning;

import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.UserInfo;
import android.os.UserManager;
import com.android.managedprovisioning.task.DeleteNonRequiredAppsTask;
import java.util.List;

public class PreBootListener extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (context.getUserId() == 0) {
            DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService("device_policy");
            PackageManager pm = context.getPackageManager();
            if (dpm.getDeviceOwner() != null && DeleteNonRequiredAppsTask.shouldDeleteNonRequiredApps(context, 0)) {
                deleteNonRequiredApps(context, dpm.getDeviceOwner(), 0, R.array.required_apps_managed_device, R.array.vendor_required_apps_managed_device, false);
            }
            UserManager um = (UserManager) context.getSystemService("user");
            List<UserInfo> profiles = um.getProfiles(0);
            if (profiles.size() > 1) {
                pm.clearCrossProfileIntentFilters(0);
                for (UserInfo userInfo : profiles) {
                    if (userInfo.isManagedProfile()) {
                        pm.clearCrossProfileIntentFilters(userInfo.id);
                        CrossProfileIntentFiltersHelper.setFilters(pm, 0, userInfo.id);
                        ComponentName profileOwner = dpm.getProfileOwnerAsUser(userInfo.id);
                        if (profileOwner == null) {
                            ProvisionLogger.loge("No profile owner on managed profile " + userInfo.id);
                        } else {
                            deleteNonRequiredApps(context, profileOwner.getPackageName(), userInfo.id, R.array.required_apps_managed_profile, R.array.vendor_required_apps_managed_profile, true);
                        }
                    }
                }
            }
        }
    }

    private void deleteNonRequiredApps(Context context, String mdmPackageName, int userId, int requiredAppsList, int vendorRequiredAppsList, boolean disableInstallShortcutListeners) {
        new DeleteNonRequiredAppsTask(context, mdmPackageName, userId, requiredAppsList, vendorRequiredAppsList, false, disableInstallShortcutListeners, new DeleteNonRequiredAppsTask.Callback() {
            @Override
            public void onSuccess() {
            }

            @Override
            public void onError() {
                ProvisionLogger.loge("Error while checking if there are new system apps that need to be deleted");
            }
        }).run();
    }
}
