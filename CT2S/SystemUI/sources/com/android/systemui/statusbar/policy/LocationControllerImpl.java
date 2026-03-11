package com.android.systemui.statusbar.policy;

import android.app.ActivityManager;
import android.app.AppOpsManager;
import android.app.StatusBarManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import com.android.systemui.R;
import com.android.systemui.statusbar.policy.LocationController;
import java.util.ArrayList;
import java.util.List;

public class LocationControllerImpl extends BroadcastReceiver implements LocationController {
    private static final int[] mHighPowerRequestAppOpArray = {42};
    private AppOpsManager mAppOpsManager;
    private boolean mAreActiveLocationRequests;
    private Context mContext;
    private ArrayList<LocationController.LocationSettingsChangeCallback> mSettingsChangeCallbacks = new ArrayList<>();
    private StatusBarManager mStatusBarManager;

    public LocationControllerImpl(Context context) {
        this.mContext = context;
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.location.HIGH_POWER_REQUEST_CHANGE");
        context.registerReceiverAsUser(this, UserHandle.ALL, filter, null, null);
        this.mAppOpsManager = (AppOpsManager) context.getSystemService("appops");
        this.mStatusBarManager = (StatusBarManager) context.getSystemService("statusbar");
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.location.MODE_CHANGED");
        context.registerReceiverAsUser(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                String action = intent.getAction();
                if ("android.location.MODE_CHANGED".equals(action)) {
                    LocationControllerImpl.this.locationSettingsChanged();
                }
            }
        }, UserHandle.ALL, intentFilter, null, new Handler());
        updateActiveLocationRequests();
        refreshViews();
    }

    @Override
    public void addSettingsChangedCallback(LocationController.LocationSettingsChangeCallback cb) {
        this.mSettingsChangeCallbacks.add(cb);
        locationSettingsChanged(cb);
    }

    @Override
    public void removeSettingsChangedCallback(LocationController.LocationSettingsChangeCallback cb) {
        this.mSettingsChangeCallbacks.remove(cb);
    }

    @Override
    public boolean setLocationEnabled(boolean enabled) {
        int currentUserId = ActivityManager.getCurrentUser();
        if (isUserLocationRestricted(currentUserId)) {
            return false;
        }
        ContentResolver cr = this.mContext.getContentResolver();
        int mode = enabled ? 3 : 0;
        return Settings.Secure.putIntForUser(cr, "location_mode", mode, currentUserId);
    }

    @Override
    public boolean isLocationEnabled() {
        ContentResolver resolver = this.mContext.getContentResolver();
        int mode = Settings.Secure.getIntForUser(resolver, "location_mode", 0, ActivityManager.getCurrentUser());
        return mode != 0;
    }

    private boolean isUserLocationRestricted(int userId) {
        UserManager um = (UserManager) this.mContext.getSystemService("user");
        return um.hasUserRestriction("no_share_location", new UserHandle(userId));
    }

    private boolean areActiveHighPowerLocationRequests() {
        List<AppOpsManager.PackageOps> packages = this.mAppOpsManager.getPackagesForOps(mHighPowerRequestAppOpArray);
        if (packages != null) {
            int numPackages = packages.size();
            for (int packageInd = 0; packageInd < numPackages; packageInd++) {
                AppOpsManager.PackageOps packageOp = packages.get(packageInd);
                List<AppOpsManager.OpEntry> opEntries = packageOp.getOps();
                if (opEntries != null) {
                    int numOps = opEntries.size();
                    for (int opInd = 0; opInd < numOps; opInd++) {
                        AppOpsManager.OpEntry opEntry = opEntries.get(opInd);
                        if (opEntry.getOp() == 42 && opEntry.isRunning()) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private void refreshViews() {
        if (this.mAreActiveLocationRequests) {
            this.mStatusBarManager.setIcon("location", R.drawable.stat_sys_location, 0, this.mContext.getString(R.string.accessibility_location_active));
        } else {
            this.mStatusBarManager.removeIcon("location");
        }
    }

    private void updateActiveLocationRequests() {
        boolean hadActiveLocationRequests = this.mAreActiveLocationRequests;
        this.mAreActiveLocationRequests = areActiveHighPowerLocationRequests();
        if (this.mAreActiveLocationRequests != hadActiveLocationRequests) {
            refreshViews();
        }
    }

    public void locationSettingsChanged() {
        boolean isEnabled = isLocationEnabled();
        for (LocationController.LocationSettingsChangeCallback cb : this.mSettingsChangeCallbacks) {
            cb.onLocationSettingsChanged(isEnabled);
        }
    }

    private void locationSettingsChanged(LocationController.LocationSettingsChangeCallback cb) {
        cb.onLocationSettingsChanged(isLocationEnabled());
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if ("android.location.HIGH_POWER_REQUEST_CHANGE".equals(action)) {
            updateActiveLocationRequests();
        }
    }
}
