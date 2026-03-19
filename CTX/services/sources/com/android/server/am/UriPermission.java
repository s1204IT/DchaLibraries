package com.android.server.am;

import android.app.GrantedUriPermission;
import android.os.Binder;
import android.os.UserHandle;
import android.util.ArraySet;
import android.util.Log;
import android.util.Slog;
import com.android.server.am.ActivityManagerService;
import com.google.android.collect.Sets;
import java.io.PrintWriter;
import java.util.Comparator;
import java.util.Iterator;

final class UriPermission {
    private static final long INVALID_TIME = Long.MIN_VALUE;
    public static final int STRENGTH_GLOBAL = 2;
    public static final int STRENGTH_NONE = 0;
    public static final int STRENGTH_OWNED = 1;
    public static final int STRENGTH_PERSISTABLE = 3;
    private static final String TAG = "UriPermission";
    private ArraySet<UriPermissionOwner> mReadOwners;
    private ArraySet<UriPermissionOwner> mWriteOwners;
    final String sourcePkg;
    private String stringName;
    final String targetPkg;
    final int targetUid;
    final int targetUserId;
    final ActivityManagerService.GrantUri uri;
    int modeFlags = 0;
    int ownedModeFlags = 0;
    int globalModeFlags = 0;
    int persistableModeFlags = 0;
    int persistedModeFlags = 0;
    long persistedCreateTime = INVALID_TIME;

    UriPermission(String str, String str2, int i, ActivityManagerService.GrantUri grantUri) {
        this.targetUserId = UserHandle.getUserId(i);
        this.sourcePkg = str;
        this.targetPkg = str2;
        this.targetUid = i;
        this.uri = grantUri;
    }

    private void updateModeFlags() {
        int i = this.modeFlags;
        this.modeFlags = this.ownedModeFlags | this.globalModeFlags | this.persistableModeFlags | this.persistedModeFlags;
        if (Log.isLoggable(TAG, 2) && this.modeFlags != i) {
            Slog.d(TAG, "Permission for " + this.targetPkg + " to " + this.uri + " is changing from 0x" + Integer.toHexString(i) + " to 0x" + Integer.toHexString(this.modeFlags) + " via calling UID " + Binder.getCallingUid() + " PID " + Binder.getCallingPid(), new Throwable());
        }
    }

    void initPersistedModes(int i, long j) {
        int i2 = i & 3;
        this.persistableModeFlags = i2;
        this.persistedModeFlags = i2;
        this.persistedCreateTime = j;
        updateModeFlags();
    }

    void grantModes(int i, UriPermissionOwner uriPermissionOwner) {
        boolean z;
        if ((i & 64) == 0) {
            z = false;
        } else {
            z = true;
        }
        int i2 = i & 3;
        if (z) {
            this.persistableModeFlags |= i2;
        }
        if (uriPermissionOwner == null) {
            this.globalModeFlags = i2 | this.globalModeFlags;
        } else {
            if ((i2 & 1) != 0) {
                addReadOwner(uriPermissionOwner);
            }
            if ((i2 & 2) != 0) {
                addWriteOwner(uriPermissionOwner);
            }
        }
        updateModeFlags();
    }

    boolean takePersistableModes(int i) {
        int i2 = i & 3;
        if ((this.persistableModeFlags & i2) != i2) {
            Slog.w(TAG, "Requested flags 0x" + Integer.toHexString(i2) + ", but only 0x" + Integer.toHexString(this.persistableModeFlags) + " are allowed");
            return false;
        }
        int i3 = this.persistedModeFlags;
        this.persistedModeFlags = (i2 & this.persistableModeFlags) | this.persistedModeFlags;
        if (this.persistedModeFlags != 0) {
            this.persistedCreateTime = System.currentTimeMillis();
        }
        updateModeFlags();
        return this.persistedModeFlags != i3;
    }

    boolean releasePersistableModes(int i) {
        int i2 = this.persistedModeFlags;
        int i3 = this.persistableModeFlags;
        int i4 = ~(i & 3);
        this.persistableModeFlags = i3 & i4;
        this.persistedModeFlags = i4 & this.persistedModeFlags;
        if (this.persistedModeFlags == 0) {
            this.persistedCreateTime = INVALID_TIME;
        }
        updateModeFlags();
        return this.persistedModeFlags != i2;
    }

    boolean revokeModes(int i, boolean z) {
        boolean z2 = (i & 64) != 0;
        int i2 = i & 3;
        int i3 = this.persistedModeFlags;
        if ((i2 & 1) != 0) {
            if (z2) {
                this.persistableModeFlags &= -2;
                this.persistedModeFlags &= -2;
            }
            this.globalModeFlags &= -2;
            if (this.mReadOwners != null && z) {
                this.ownedModeFlags &= -2;
                Iterator<UriPermissionOwner> it = this.mReadOwners.iterator();
                while (it.hasNext()) {
                    it.next().removeReadPermission(this);
                }
                this.mReadOwners = null;
            }
        }
        if ((i2 & 2) != 0) {
            if (z2) {
                this.persistableModeFlags &= -3;
                this.persistedModeFlags &= -3;
            }
            this.globalModeFlags &= -3;
            if (this.mWriteOwners != null && z) {
                this.ownedModeFlags &= -3;
                Iterator<UriPermissionOwner> it2 = this.mWriteOwners.iterator();
                while (it2.hasNext()) {
                    it2.next().removeWritePermission(this);
                }
                this.mWriteOwners = null;
            }
        }
        if (this.persistedModeFlags == 0) {
            this.persistedCreateTime = INVALID_TIME;
        }
        updateModeFlags();
        return this.persistedModeFlags != i3;
    }

    public int getStrength(int i) {
        int i2 = i & 3;
        if ((this.persistableModeFlags & i2) == i2) {
            return 3;
        }
        if ((this.globalModeFlags & i2) == i2) {
            return 2;
        }
        if ((this.ownedModeFlags & i2) == i2) {
            return 1;
        }
        return 0;
    }

    private void addReadOwner(UriPermissionOwner uriPermissionOwner) {
        if (this.mReadOwners == null) {
            this.mReadOwners = Sets.newArraySet();
            this.ownedModeFlags |= 1;
            updateModeFlags();
        }
        if (this.mReadOwners.add(uriPermissionOwner)) {
            uriPermissionOwner.addReadPermission(this);
        }
    }

    void removeReadOwner(UriPermissionOwner uriPermissionOwner) {
        if (!this.mReadOwners.remove(uriPermissionOwner)) {
            Slog.wtf(TAG, "Unknown read owner " + uriPermissionOwner + " in " + this);
        }
        if (this.mReadOwners.size() == 0) {
            this.mReadOwners = null;
            this.ownedModeFlags &= -2;
            updateModeFlags();
        }
    }

    private void addWriteOwner(UriPermissionOwner uriPermissionOwner) {
        if (this.mWriteOwners == null) {
            this.mWriteOwners = Sets.newArraySet();
            this.ownedModeFlags |= 2;
            updateModeFlags();
        }
        if (this.mWriteOwners.add(uriPermissionOwner)) {
            uriPermissionOwner.addWritePermission(this);
        }
    }

    void removeWriteOwner(UriPermissionOwner uriPermissionOwner) {
        if (!this.mWriteOwners.remove(uriPermissionOwner)) {
            Slog.wtf(TAG, "Unknown write owner " + uriPermissionOwner + " in " + this);
        }
        if (this.mWriteOwners.size() == 0) {
            this.mWriteOwners = null;
            this.ownedModeFlags &= -3;
            updateModeFlags();
        }
    }

    public String toString() {
        if (this.stringName != null) {
            return this.stringName;
        }
        StringBuilder sb = new StringBuilder(128);
        sb.append("UriPermission{");
        sb.append(Integer.toHexString(System.identityHashCode(this)));
        sb.append(' ');
        sb.append(this.uri);
        sb.append('}');
        String string = sb.toString();
        this.stringName = string;
        return string;
    }

    void dump(PrintWriter printWriter, String str) {
        printWriter.print(str);
        printWriter.print("targetUserId=" + this.targetUserId);
        printWriter.print(" sourcePkg=" + this.sourcePkg);
        printWriter.println(" targetPkg=" + this.targetPkg);
        printWriter.print(str);
        printWriter.print("mode=0x" + Integer.toHexString(this.modeFlags));
        printWriter.print(" owned=0x" + Integer.toHexString(this.ownedModeFlags));
        printWriter.print(" global=0x" + Integer.toHexString(this.globalModeFlags));
        printWriter.print(" persistable=0x" + Integer.toHexString(this.persistableModeFlags));
        printWriter.print(" persisted=0x" + Integer.toHexString(this.persistedModeFlags));
        if (this.persistedCreateTime != INVALID_TIME) {
            printWriter.print(" persistedCreate=" + this.persistedCreateTime);
        }
        printWriter.println();
        if (this.mReadOwners != null) {
            printWriter.print(str);
            printWriter.println("readOwners:");
            for (UriPermissionOwner uriPermissionOwner : this.mReadOwners) {
                printWriter.print(str);
                printWriter.println("  * " + uriPermissionOwner);
            }
        }
        if (this.mWriteOwners != null) {
            printWriter.print(str);
            printWriter.println("writeOwners:");
            for (UriPermissionOwner uriPermissionOwner2 : this.mReadOwners) {
                printWriter.print(str);
                printWriter.println("  * " + uriPermissionOwner2);
            }
        }
    }

    public static class PersistedTimeComparator implements Comparator<UriPermission> {
        @Override
        public int compare(UriPermission uriPermission, UriPermission uriPermission2) {
            return Long.compare(uriPermission.persistedCreateTime, uriPermission2.persistedCreateTime);
        }
    }

    public static class Snapshot {
        final long persistedCreateTime;
        final int persistedModeFlags;
        final String sourcePkg;
        final String targetPkg;
        final int targetUserId;
        final ActivityManagerService.GrantUri uri;

        private Snapshot(UriPermission uriPermission) {
            this.targetUserId = uriPermission.targetUserId;
            this.sourcePkg = uriPermission.sourcePkg;
            this.targetPkg = uriPermission.targetPkg;
            this.uri = uriPermission.uri;
            this.persistedModeFlags = uriPermission.persistedModeFlags;
            this.persistedCreateTime = uriPermission.persistedCreateTime;
        }
    }

    public Snapshot snapshot() {
        return new Snapshot();
    }

    public android.content.UriPermission buildPersistedPublicApiObject() {
        return new android.content.UriPermission(this.uri.uri, this.persistedModeFlags, this.persistedCreateTime);
    }

    public GrantedUriPermission buildGrantedUriPermission() {
        return new GrantedUriPermission(this.uri.uri, this.targetPkg);
    }
}
