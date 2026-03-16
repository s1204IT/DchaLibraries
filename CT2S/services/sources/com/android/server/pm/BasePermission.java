package com.android.server.pm;

import android.content.pm.PackageParser;
import android.content.pm.PermissionInfo;

final class BasePermission {
    static final int TYPE_BUILTIN = 1;
    static final int TYPE_DYNAMIC = 2;
    static final int TYPE_NORMAL = 0;
    int[] gids;
    final String name;
    PackageSettingBase packageSetting;
    PermissionInfo pendingInfo;
    PackageParser.Permission perm;
    int protectionLevel = 2;
    String sourcePackage;
    final int type;
    int uid;

    BasePermission(String _name, String _sourcePackage, int _type) {
        this.name = _name;
        this.sourcePackage = _sourcePackage;
        this.type = _type;
    }

    public String toString() {
        return "BasePermission{" + Integer.toHexString(System.identityHashCode(this)) + " " + this.name + "}";
    }
}
