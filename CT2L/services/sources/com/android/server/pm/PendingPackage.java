package com.android.server.pm;

import java.io.File;

final class PendingPackage extends PackageSettingBase {
    final int sharedId;

    PendingPackage(String name, String realName, File codePath, File resourcePath, String legacyNativeLibraryPathString, String primaryCpuAbiString, String secondaryCpuAbiString, String cpuAbiOverrideString, int sharedId, int pVersionCode, int pkgFlags) {
        super(name, realName, codePath, resourcePath, legacyNativeLibraryPathString, primaryCpuAbiString, secondaryCpuAbiString, cpuAbiOverrideString, pVersionCode, pkgFlags);
        this.sharedId = sharedId;
    }
}
