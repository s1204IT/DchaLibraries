package com.android.server.timezone;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

final class PackageStatus {
    static final int CHECK_COMPLETED_FAILURE = 3;
    static final int CHECK_COMPLETED_SUCCESS = 2;
    static final int CHECK_STARTED = 1;
    final int mCheckStatus;
    final PackageVersions mVersions;

    @Retention(RetentionPolicy.SOURCE)
    @interface CheckStatus {
    }

    PackageStatus(int i, PackageVersions packageVersions) {
        this.mCheckStatus = i;
        if (i < 1 || i > 3) {
            throw new IllegalArgumentException("Unknown checkStatus " + i);
        }
        if (packageVersions == null) {
            throw new NullPointerException("versions == null");
        }
        this.mVersions = packageVersions;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        PackageStatus packageStatus = (PackageStatus) obj;
        if (this.mCheckStatus != packageStatus.mCheckStatus) {
            return false;
        }
        return this.mVersions.equals(packageStatus.mVersions);
    }

    public int hashCode() {
        return (31 * this.mCheckStatus) + this.mVersions.hashCode();
    }

    public String toString() {
        return "PackageStatus{mCheckStatus=" + this.mCheckStatus + ", mVersions=" + this.mVersions + '}';
    }
}
