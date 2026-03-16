package com.android.server.pm;

import android.content.pm.PackageParser;
import java.io.File;

final class PackageSetting extends PackageSettingBase {
    int appId;
    PackageParser.Package pkg;
    SharedUserSetting sharedUser;

    PackageSetting(String name, String realName, File codePath, File resourcePath, String legacyNativeLibraryPathString, String primaryCpuAbiString, String secondaryCpuAbiString, String cpuAbiOverrideString, int pVersionCode, int pkgFlags) {
        super(name, realName, codePath, resourcePath, legacyNativeLibraryPathString, primaryCpuAbiString, secondaryCpuAbiString, cpuAbiOverrideString, pVersionCode, pkgFlags);
    }

    PackageSetting(PackageSetting orig) {
        super(orig);
        this.appId = orig.appId;
        this.pkg = orig.pkg;
        this.sharedUser = orig.sharedUser;
    }

    public String toString() {
        return "PackageSetting{" + Integer.toHexString(System.identityHashCode(this)) + " " + this.name + "/" + this.appId + "}";
    }

    public int[] getGids() {
        return this.sharedUser != null ? this.sharedUser.gids : this.gids;
    }

    public boolean isPrivileged() {
        return (this.pkgFlags & 1073741824) != 0;
    }
}
