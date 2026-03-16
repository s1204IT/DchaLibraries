package com.android.server.pm;

import android.util.ArraySet;

class GrantedPermissions {
    int[] gids;
    ArraySet<String> grantedPermissions;
    int pkgFlags;

    GrantedPermissions(int pkgFlags) {
        this.grantedPermissions = new ArraySet<>();
        setFlags(pkgFlags);
    }

    GrantedPermissions(GrantedPermissions base) {
        this.grantedPermissions = new ArraySet<>();
        this.pkgFlags = base.pkgFlags;
        this.grantedPermissions = new ArraySet<>((ArraySet) base.grantedPermissions);
        if (base.gids != null) {
            this.gids = (int[]) base.gids.clone();
        }
    }

    void setFlags(int pkgFlags) {
        this.pkgFlags = 1610874881 & pkgFlags;
    }
}
