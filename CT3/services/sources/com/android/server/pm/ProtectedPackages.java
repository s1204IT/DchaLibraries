package com.android.server.pm;

import android.util.SparseArray;

public class ProtectedPackages {
    private String mDeviceOwnerPackage;
    private int mDeviceOwnerUserId;
    private final Object mLock = new Object();
    private SparseArray<String> mProfileOwnerPackages;

    public void setDeviceAndProfileOwnerPackages(int deviceOwnerUserId, String deviceOwnerPackage, SparseArray<String> profileOwnerPackages) {
        synchronized (this.mLock) {
            this.mDeviceOwnerUserId = deviceOwnerUserId;
            if (deviceOwnerUserId == -10000) {
                deviceOwnerPackage = null;
            }
            this.mDeviceOwnerPackage = deviceOwnerPackage;
            this.mProfileOwnerPackages = profileOwnerPackages != null ? profileOwnerPackages.clone() : null;
        }
    }

    private boolean hasDeviceOwnerOrProfileOwner(int userId, String packageName) {
        if (packageName == null) {
            return false;
        }
        synchronized (this.mLock) {
            if (this.mDeviceOwnerPackage != null && this.mDeviceOwnerUserId == userId && packageName.equals(this.mDeviceOwnerPackage)) {
                return true;
            }
            if (this.mProfileOwnerPackages != null) {
                if (packageName.equals(this.mProfileOwnerPackages.get(userId))) {
                    return true;
                }
            }
            return false;
        }
    }

    public boolean canPackageStateBeChanged(int userId, String packageName) {
        return hasDeviceOwnerOrProfileOwner(userId, packageName);
    }

    public boolean canPackageBeWiped(int userId, String packageName) {
        return hasDeviceOwnerOrProfileOwner(userId, packageName);
    }
}
