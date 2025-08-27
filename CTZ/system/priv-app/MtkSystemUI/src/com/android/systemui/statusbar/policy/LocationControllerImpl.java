package com.android.systemui.statusbar.policy;

import android.app.ActivityManager;
import android.app.AppOpsManager;
import android.app.StatusBarManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.LocationManager;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.UserHandle;
import android.os.UserManager;
import com.android.settingslib.Utils;
import com.android.systemui.statusbar.policy.LocationController;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/* loaded from: classes.dex */
public class LocationControllerImpl extends BroadcastReceiver implements LocationController {
    private static final int[] mHighPowerRequestAppOpArray = {42};
    private AppOpsManager mAppOpsManager;
    private boolean mAreActiveLocationRequests;
    private Context mContext;
    private StatusBarManager mStatusBarManager;
    private ArrayList<LocationController.LocationChangeCallback> mSettingsChangeCallbacks = new ArrayList<>();
    private final H mHandler = new H();

    public LocationControllerImpl(Context context, Looper looper) {
        this.mContext = context;
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.location.HIGH_POWER_REQUEST_CHANGE");
        intentFilter.addAction("android.location.MODE_CHANGED");
        context.registerReceiverAsUser(this, UserHandle.ALL, intentFilter, null, new Handler(looper));
        this.mAppOpsManager = (AppOpsManager) context.getSystemService("appops");
        this.mStatusBarManager = (StatusBarManager) context.getSystemService("statusbar");
        updateActiveLocationRequests();
    }

    /* JADX DEBUG: Method merged with bridge method: addCallback(Ljava/lang/Object;)V */
    @Override // com.android.systemui.statusbar.policy.CallbackController
    public void addCallback(LocationController.LocationChangeCallback locationChangeCallback) {
        this.mSettingsChangeCallbacks.add(locationChangeCallback);
        this.mHandler.sendEmptyMessage(1);
    }

    /* JADX DEBUG: Method merged with bridge method: removeCallback(Ljava/lang/Object;)V */
    @Override // com.android.systemui.statusbar.policy.CallbackController
    public void removeCallback(LocationController.LocationChangeCallback locationChangeCallback) {
        this.mSettingsChangeCallbacks.remove(locationChangeCallback);
    }

    @Override // com.android.systemui.statusbar.policy.LocationController
    public boolean setLocationEnabled(boolean z) {
        int currentUser = ActivityManager.getCurrentUser();
        if (isUserLocationRestricted(currentUser)) {
            return false;
        }
        Utils.updateLocationEnabled(this.mContext, z, currentUser, 2);
        return true;
    }

    @Override // com.android.systemui.statusbar.policy.LocationController
    public boolean isLocationEnabled() {
        return ((LocationManager) this.mContext.getSystemService("location")).isLocationEnabledForUser(UserHandle.of(ActivityManager.getCurrentUser()));
    }

    @Override // com.android.systemui.statusbar.policy.LocationController
    public boolean isLocationActive() {
        return this.mAreActiveLocationRequests;
    }

    private boolean isUserLocationRestricted(int i) {
        return ((UserManager) this.mContext.getSystemService("user")).hasUserRestriction("no_share_location", UserHandle.of(i));
    }

    protected boolean areActiveHighPowerLocationRequests() {
        List packagesForOps = this.mAppOpsManager.getPackagesForOps(mHighPowerRequestAppOpArray);
        if (packagesForOps != null) {
            int size = packagesForOps.size();
            for (int i = 0; i < size; i++) {
                List ops = ((AppOpsManager.PackageOps) packagesForOps.get(i)).getOps();
                if (ops != null) {
                    int size2 = ops.size();
                    for (int i2 = 0; i2 < size2; i2++) {
                        AppOpsManager.OpEntry opEntry = (AppOpsManager.OpEntry) ops.get(i2);
                        if (opEntry.getOp() == 42 && opEntry.isRunning()) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private void updateActiveLocationRequests() {
        boolean z = this.mAreActiveLocationRequests;
        this.mAreActiveLocationRequests = areActiveHighPowerLocationRequests();
        if (this.mAreActiveLocationRequests != z) {
            this.mHandler.sendEmptyMessage(2);
        }
    }

    @Override // android.content.BroadcastReceiver
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if ("android.location.HIGH_POWER_REQUEST_CHANGE".equals(action)) {
            updateActiveLocationRequests();
        } else if ("android.location.MODE_CHANGED".equals(action)) {
            this.mHandler.sendEmptyMessage(1);
        }
    }

    private final class H extends Handler {
        private H() {
        }

        @Override // android.os.Handler
        public void handleMessage(Message message) {
            switch (message.what) {
                case 1:
                    locationSettingsChanged();
                    break;
                case 2:
                    locationActiveChanged();
                    break;
            }
        }

        private void locationActiveChanged() {
            com.android.systemui.util.Utils.safeForeach(LocationControllerImpl.this.mSettingsChangeCallbacks, new Consumer() { // from class: com.android.systemui.statusbar.policy.-$$Lambda$LocationControllerImpl$H$vKTe7eMzgWgCJvXCt8UIIkFyg78
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    ((LocationController.LocationChangeCallback) obj).onLocationActiveChanged(LocationControllerImpl.this.mAreActiveLocationRequests);
                }
            });
        }

        private void locationSettingsChanged() {
            final boolean zIsLocationEnabled = LocationControllerImpl.this.isLocationEnabled();
            com.android.systemui.util.Utils.safeForeach(LocationControllerImpl.this.mSettingsChangeCallbacks, new Consumer() { // from class: com.android.systemui.statusbar.policy.-$$Lambda$LocationControllerImpl$H$xXVOboFsQOHoRY-EFzvZu-IOYh0
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    ((LocationController.LocationChangeCallback) obj).onLocationSettingsChanged(zIsLocationEnabled);
                }
            });
        }
    }
}
