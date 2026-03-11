package com.android.systemui.statusbar.policy;

import android.R;
import android.app.ActivityManager;
import android.app.AppOpsManager;
import android.app.StatusBarManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import com.android.systemui.statusbar.policy.LocationController;
import java.util.ArrayList;
import java.util.List;

public class LocationControllerImpl extends BroadcastReceiver implements LocationController {
    private static final int[] mHighPowerRequestAppOpArray = {42};
    private AppOpsManager mAppOpsManager;
    private boolean mAreActiveLocationRequests;
    private Context mContext;
    public final String mSlotLocation;
    private StatusBarManager mStatusBarManager;
    private ArrayList<LocationController.LocationSettingsChangeCallback> mSettingsChangeCallbacks = new ArrayList<>();
    private final H mHandler = new H(this, null);

    public LocationControllerImpl(Context context, Looper bgLooper) {
        this.mContext = context;
        this.mSlotLocation = this.mContext.getString(R.string.config_defaultBrowser);
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.location.HIGH_POWER_REQUEST_CHANGE");
        filter.addAction("android.location.MODE_CHANGED");
        context.registerReceiverAsUser(this, UserHandle.ALL, filter, null, new Handler(bgLooper));
        this.mAppOpsManager = (AppOpsManager) context.getSystemService("appops");
        this.mStatusBarManager = (StatusBarManager) context.getSystemService("statusbar");
        updateActiveLocationRequests();
        refreshViews();
    }

    @Override
    public void addSettingsChangedCallback(LocationController.LocationSettingsChangeCallback cb) {
        this.mSettingsChangeCallbacks.add(cb);
        this.mHandler.sendEmptyMessage(1);
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
        int mode = enabled ? -1 : 0;
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
        return um.hasUserRestriction("no_share_location", UserHandle.of(userId));
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
            return false;
        }
        return false;
    }

    private void refreshViews() {
        if (this.mAreActiveLocationRequests) {
            this.mStatusBarManager.setIcon(this.mSlotLocation, com.android.systemui.R.drawable.stat_sys_location, 0, this.mContext.getString(com.android.systemui.R.string.accessibility_location_active));
        } else {
            this.mStatusBarManager.removeIcon(this.mSlotLocation);
        }
    }

    private void updateActiveLocationRequests() {
        boolean hadActiveLocationRequests = this.mAreActiveLocationRequests;
        this.mAreActiveLocationRequests = areActiveHighPowerLocationRequests();
        if (this.mAreActiveLocationRequests == hadActiveLocationRequests) {
            return;
        }
        refreshViews();
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if ("android.location.HIGH_POWER_REQUEST_CHANGE".equals(action)) {
            updateActiveLocationRequests();
        } else {
            if (!"android.location.MODE_CHANGED".equals(action)) {
                return;
            }
            this.mHandler.sendEmptyMessage(1);
        }
    }

    private final class H extends Handler {
        H(LocationControllerImpl this$0, H h) {
            this();
        }

        private H() {
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    locationSettingsChanged();
                    break;
            }
        }

        private void locationSettingsChanged() {
            boolean isEnabled = LocationControllerImpl.this.isLocationEnabled();
            for (LocationController.LocationSettingsChangeCallback cb : LocationControllerImpl.this.mSettingsChangeCallbacks) {
                cb.onLocationSettingsChanged(isEnabled);
            }
        }
    }
}
