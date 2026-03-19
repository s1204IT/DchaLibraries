package com.android.server.telecom;

import android.content.Context;
import android.os.UserHandle;
import android.telecom.Log;
import com.android.internal.annotations.VisibleForTesting;
import com.android.server.telecom.Timeouts;

@VisibleForTesting
public class EmergencyCallHelper {
    private final Context mContext;
    private final String mDefaultDialerPackage;
    private long mLastEmergencyCallTimestampMillis;
    private UserHandle mLocationPermissionGrantedToUser;
    private final Timeouts.Adapter mTimeoutsAdapter;

    @VisibleForTesting
    public EmergencyCallHelper(Context context, String str, Timeouts.Adapter adapter) {
        this.mContext = context;
        this.mDefaultDialerPackage = str;
        this.mTimeoutsAdapter = adapter;
    }

    void maybeGrantTemporaryLocationPermission(Call call, UserHandle userHandle) {
        if (shouldGrantTemporaryLocationPermission(call)) {
            grantLocationPermission(userHandle, call);
        }
        if (call != null && call.isEmergencyCall()) {
            recordEmergencyCallTime();
        }
    }

    void maybeRevokeTemporaryLocationPermission() {
        if (wasGrantedTemporaryLocationPermission()) {
            revokeLocationPermission();
        }
    }

    long getLastEmergencyCallTimeMillis() {
        return this.mLastEmergencyCallTimestampMillis;
    }

    private void recordEmergencyCallTime() {
        this.mLastEmergencyCallTimestampMillis = System.currentTimeMillis();
    }

    private boolean isInEmergencyCallbackWindow() {
        return System.currentTimeMillis() - getLastEmergencyCallTimeMillis() < this.mTimeoutsAdapter.getEmergencyCallbackWindowMillis(this.mContext.getContentResolver());
    }

    private boolean shouldGrantTemporaryLocationPermission(Call call) {
        if (!this.mContext.getResources().getBoolean(R.bool.grant_location_permission_enabled)) {
            Log.i(this, "ShouldGrantTemporaryLocationPermission, disabled by config", new Object[0]);
            return false;
        }
        if (call == null) {
            Log.i(this, "ShouldGrantTemporaryLocationPermission, no call", new Object[0]);
            return false;
        }
        if (!call.isEmergencyCall() && !isInEmergencyCallbackWindow()) {
            Log.i(this, "ShouldGrantTemporaryLocationPermission, not emergency", new Object[0]);
            return false;
        }
        if (hasLocationPermission()) {
            Log.i(this, "ShouldGrantTemporaryLocationPermission, already has location permission", new Object[0]);
            return false;
        }
        Log.i(this, "ShouldGrantTemporaryLocationPermission, returning true", new Object[0]);
        return true;
    }

    private void grantLocationPermission(UserHandle userHandle, Call call) {
        Log.i(this, "Granting temporary location permission to " + this.mDefaultDialerPackage + ", user: " + userHandle, new Object[0]);
        try {
            this.mContext.getPackageManager().grantRuntimePermission(this.mDefaultDialerPackage, "android.permission.ACCESS_FINE_LOCATION", userHandle);
            recordPermissionGrant(userHandle);
        } catch (Exception e) {
            Log.e(this, e, "Failed to grant location permission to " + this.mDefaultDialerPackage + ", user: " + userHandle, new Object[0]);
        }
    }

    private void revokeLocationPermission() {
        Log.i(this, "Revoking temporary location permission from " + this.mDefaultDialerPackage + ", user: " + this.mLocationPermissionGrantedToUser, new Object[0]);
        UserHandle userHandle = this.mLocationPermissionGrantedToUser;
        clearPermissionGrant();
        try {
            this.mContext.getPackageManager().revokeRuntimePermission(this.mDefaultDialerPackage, "android.permission.ACCESS_FINE_LOCATION", userHandle);
        } catch (Exception e) {
            Log.e(this, e, "Failed to revoke location permission from " + this.mDefaultDialerPackage + ", user: " + userHandle, new Object[0]);
        }
    }

    private boolean hasLocationPermission() {
        return this.mContext.getPackageManager().checkPermission("android.permission.ACCESS_FINE_LOCATION", this.mDefaultDialerPackage) == 0;
    }

    private void recordPermissionGrant(UserHandle userHandle) {
        this.mLocationPermissionGrantedToUser = userHandle;
    }

    private boolean wasGrantedTemporaryLocationPermission() {
        return this.mLocationPermissionGrantedToUser != null;
    }

    private void clearPermissionGrant() {
        this.mLocationPermissionGrantedToUser = null;
    }
}
