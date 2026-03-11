package com.android.launcher3.compat;

import java.util.HashMap;

public class PackageInstallerCompatV16 extends PackageInstallerCompat {
    PackageInstallerCompatV16() {
    }

    @Override
    public void onStop() {
    }

    @Override
    public HashMap<String, Integer> updateAndGetActiveSessionCache() {
        return new HashMap<>();
    }
}
