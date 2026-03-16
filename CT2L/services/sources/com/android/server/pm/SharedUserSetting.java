package com.android.server.pm;

import android.util.ArraySet;

final class SharedUserSetting extends GrantedPermissions {
    final String name;
    final ArraySet<PackageSetting> packages;
    final PackageSignatures signatures;
    int uidFlags;
    int userId;

    SharedUserSetting(String _name, int _pkgFlags) {
        super(_pkgFlags);
        this.packages = new ArraySet<>();
        this.signatures = new PackageSignatures();
        this.uidFlags = _pkgFlags;
        this.name = _name;
    }

    public String toString() {
        return "SharedUserSetting{" + Integer.toHexString(System.identityHashCode(this)) + " " + this.name + "/" + this.userId + "}";
    }

    void removePackage(PackageSetting packageSetting) {
        if (this.packages.remove(packageSetting) && (this.pkgFlags & packageSetting.pkgFlags) != 0) {
            int aggregatedFlags = this.uidFlags;
            for (PackageSetting ps : this.packages) {
                aggregatedFlags |= ps.pkgFlags;
            }
            setFlags(aggregatedFlags);
        }
    }

    void addPackage(PackageSetting packageSetting) {
        if (this.packages.add(packageSetting)) {
            setFlags(this.pkgFlags | packageSetting.pkgFlags);
        }
    }
}
