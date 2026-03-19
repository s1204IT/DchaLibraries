package com.android.timezone.distro;

public class StagedDistroOperation {
    private static final StagedDistroOperation UNINSTALL_STAGED = new StagedDistroOperation(true, null);
    public final DistroVersion distroVersion;
    public final boolean isUninstall;

    private StagedDistroOperation(boolean z, DistroVersion distroVersion) {
        this.isUninstall = z;
        this.distroVersion = distroVersion;
    }

    public static StagedDistroOperation install(DistroVersion distroVersion) {
        if (distroVersion == null) {
            throw new NullPointerException("distroVersion==null");
        }
        return new StagedDistroOperation(false, distroVersion);
    }

    public static StagedDistroOperation uninstall() {
        return UNINSTALL_STAGED;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        StagedDistroOperation stagedDistroOperation = (StagedDistroOperation) obj;
        if (this.isUninstall != stagedDistroOperation.isUninstall) {
            return false;
        }
        if (this.distroVersion != null) {
            return this.distroVersion.equals(stagedDistroOperation.distroVersion);
        }
        if (stagedDistroOperation.distroVersion == null) {
            return true;
        }
        return false;
    }

    public int hashCode() {
        return (31 * (this.isUninstall ? 1 : 0)) + (this.distroVersion != null ? this.distroVersion.hashCode() : 0);
    }

    public String toString() {
        return "StagedDistroOperation{isUninstall=" + this.isUninstall + ", distroVersion=" + this.distroVersion + '}';
    }
}
