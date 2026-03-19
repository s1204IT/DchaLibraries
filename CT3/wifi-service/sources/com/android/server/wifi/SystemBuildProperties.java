package com.android.server.wifi;

import android.os.Build;

class SystemBuildProperties implements BuildProperties {
    SystemBuildProperties() {
    }

    @Override
    public boolean isEngBuild() {
        return Build.TYPE.equals("eng");
    }

    @Override
    public boolean isUserdebugBuild() {
        return Build.TYPE.equals("userdebug");
    }

    @Override
    public boolean isUserBuild() {
        return Build.TYPE.equals("user");
    }
}
