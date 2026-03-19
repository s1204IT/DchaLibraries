package com.android.server.pm;

import android.R;
import android.content.Context;
import android.util.SparseArray;
import com.android.internal.annotations.GuardedBy;

public class ProtectedPackages {
    private final Context mContext;

    @GuardedBy("this")
    private String mDeviceOwnerPackage;

    @GuardedBy("this")
    private int mDeviceOwnerUserId;

    @GuardedBy("this")
    private final String mDeviceProvisioningPackage;

    @GuardedBy("this")
    private SparseArray<String> mProfileOwnerPackages;

    public ProtectedPackages(Context context) {
        this.mContext = context;
        this.mDeviceProvisioningPackage = this.mContext.getResources().getString(R.string.activity_resolver_use_once);
    }

    public synchronized void setDeviceAndProfileOwnerPackages(int i, String str, SparseArray<String> sparseArray) {
        this.mDeviceOwnerUserId = i;
        SparseArray<String> sparseArrayClone = null;
        if (i == -10000) {
            str = null;
        }
        this.mDeviceOwnerPackage = str;
        if (sparseArray != null) {
            sparseArrayClone = sparseArray.clone();
        }
        this.mProfileOwnerPackages = sparseArrayClone;
    }

    private synchronized boolean hasDeviceOwnerOrProfileOwner(int i, String str) {
        if (str == null) {
            return false;
        }
        if (this.mDeviceOwnerPackage != null && this.mDeviceOwnerUserId == i && str.equals(this.mDeviceOwnerPackage)) {
            return true;
        }
        if (this.mProfileOwnerPackages != null) {
            if (str.equals(this.mProfileOwnerPackages.get(i))) {
                return true;
            }
        }
        return false;
    }

    public synchronized String getDeviceOwnerOrProfileOwnerPackage(int i) {
        if (this.mDeviceOwnerUserId == i) {
            return this.mDeviceOwnerPackage;
        }
        return this.mProfileOwnerPackages.get(i);
    }

    private synchronized boolean isProtectedPackage(String str) {
        boolean z;
        if (str != null) {
            z = str.equals(this.mDeviceProvisioningPackage);
        }
        return z;
    }

    public boolean isPackageStateProtected(int i, String str) {
        return hasDeviceOwnerOrProfileOwner(i, str) || isProtectedPackage(str);
    }

    public boolean isPackageDataProtected(int i, String str) {
        return hasDeviceOwnerOrProfileOwner(i, str) || isProtectedPackage(str);
    }
}
