package com.android.server.pm;

import com.android.server.pm.permission.PermissionsState;

abstract class SettingBase {
    protected final PermissionsState mPermissionsState;
    int pkgFlags;
    int pkgPrivateFlags;

    SettingBase(int i, int i2) {
        setFlags(i);
        setPrivateFlags(i2);
        this.mPermissionsState = new PermissionsState();
    }

    SettingBase(SettingBase settingBase) {
        this.mPermissionsState = new PermissionsState();
        doCopy(settingBase);
    }

    public void copyFrom(SettingBase settingBase) {
        doCopy(settingBase);
    }

    private void doCopy(SettingBase settingBase) {
        this.pkgFlags = settingBase.pkgFlags;
        this.pkgPrivateFlags = settingBase.pkgPrivateFlags;
        this.mPermissionsState.copyFrom(settingBase.mPermissionsState);
    }

    public PermissionsState getPermissionsState() {
        return this.mPermissionsState;
    }

    void setFlags(int i) {
        this.pkgFlags = i & 262145;
    }

    void setPrivateFlags(int i) {
        this.pkgPrivateFlags = i & 918028;
    }
}
