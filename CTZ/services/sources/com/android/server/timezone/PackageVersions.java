package com.android.server.timezone;

final class PackageVersions {
    final long mDataAppVersion;
    final long mUpdateAppVersion;

    PackageVersions(long j, long j2) {
        this.mUpdateAppVersion = j;
        this.mDataAppVersion = j2;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        PackageVersions packageVersions = (PackageVersions) obj;
        if (this.mUpdateAppVersion == packageVersions.mUpdateAppVersion && this.mDataAppVersion == packageVersions.mDataAppVersion) {
            return true;
        }
        return false;
    }

    public int hashCode() {
        return (31 * Long.hashCode(this.mUpdateAppVersion)) + Long.hashCode(this.mDataAppVersion);
    }

    public String toString() {
        return "PackageVersions{mUpdateAppVersion=" + this.mUpdateAppVersion + ", mDataAppVersion=" + this.mDataAppVersion + '}';
    }
}
