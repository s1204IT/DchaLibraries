package com.android.server.am;

import android.os.Binder;
import android.os.IBinder;
import android.util.ArraySet;
import android.util.proto.ProtoOutputStream;
import com.android.server.am.ActivityManagerService;
import com.google.android.collect.Sets;
import java.io.PrintWriter;
import java.util.Iterator;

final class UriPermissionOwner {
    Binder externalToken;
    private ArraySet<UriPermission> mReadPerms;
    private ArraySet<UriPermission> mWritePerms;
    final Object owner;
    final ActivityManagerService service;

    class ExternalToken extends Binder {
        ExternalToken() {
        }

        UriPermissionOwner getOwner() {
            return UriPermissionOwner.this;
        }
    }

    UriPermissionOwner(ActivityManagerService activityManagerService, Object obj) {
        this.service = activityManagerService;
        this.owner = obj;
    }

    Binder getExternalTokenLocked() {
        if (this.externalToken == null) {
            this.externalToken = new ExternalToken();
        }
        return this.externalToken;
    }

    static UriPermissionOwner fromExternalToken(IBinder iBinder) {
        if (iBinder instanceof ExternalToken) {
            return ((ExternalToken) iBinder).getOwner();
        }
        return null;
    }

    void removeUriPermissionsLocked() {
        removeUriPermissionsLocked(3);
    }

    void removeUriPermissionsLocked(int i) {
        removeUriPermissionLocked(null, i);
    }

    void removeUriPermissionLocked(ActivityManagerService.GrantUri grantUri, int i) {
        if ((i & 1) != 0 && this.mReadPerms != null) {
            Iterator<UriPermission> it = this.mReadPerms.iterator();
            while (it.hasNext()) {
                UriPermission next = it.next();
                if (grantUri == null || grantUri.equals(next.uri)) {
                    next.removeReadOwner(this);
                    this.service.removeUriPermissionIfNeededLocked(next);
                    it.remove();
                }
            }
            if (this.mReadPerms.isEmpty()) {
                this.mReadPerms = null;
            }
        }
        if ((i & 2) != 0 && this.mWritePerms != null) {
            Iterator<UriPermission> it2 = this.mWritePerms.iterator();
            while (it2.hasNext()) {
                UriPermission next2 = it2.next();
                if (grantUri == null || grantUri.equals(next2.uri)) {
                    next2.removeWriteOwner(this);
                    this.service.removeUriPermissionIfNeededLocked(next2);
                    it2.remove();
                }
            }
            if (this.mWritePerms.isEmpty()) {
                this.mWritePerms = null;
            }
        }
    }

    public void addReadPermission(UriPermission uriPermission) {
        if (this.mReadPerms == null) {
            this.mReadPerms = Sets.newArraySet();
        }
        this.mReadPerms.add(uriPermission);
    }

    public void addWritePermission(UriPermission uriPermission) {
        if (this.mWritePerms == null) {
            this.mWritePerms = Sets.newArraySet();
        }
        this.mWritePerms.add(uriPermission);
    }

    public void removeReadPermission(UriPermission uriPermission) {
        this.mReadPerms.remove(uriPermission);
        if (this.mReadPerms.isEmpty()) {
            this.mReadPerms = null;
        }
    }

    public void removeWritePermission(UriPermission uriPermission) {
        this.mWritePerms.remove(uriPermission);
        if (this.mWritePerms.isEmpty()) {
            this.mWritePerms = null;
        }
    }

    public void dump(PrintWriter printWriter, String str) {
        if (this.mReadPerms != null) {
            printWriter.print(str);
            printWriter.print("readUriPermissions=");
            printWriter.println(this.mReadPerms);
        }
        if (this.mWritePerms != null) {
            printWriter.print(str);
            printWriter.print("writeUriPermissions=");
            printWriter.println(this.mWritePerms);
        }
    }

    public void writeToProto(ProtoOutputStream protoOutputStream, long j) {
        long jStart = protoOutputStream.start(j);
        protoOutputStream.write(1138166333441L, this.owner.toString());
        if (this.mReadPerms != null) {
            synchronized (this.mReadPerms) {
                Iterator<UriPermission> it = this.mReadPerms.iterator();
                while (it.hasNext()) {
                    it.next().uri.writeToProto(protoOutputStream, 2246267895810L);
                }
            }
        }
        if (this.mWritePerms != null) {
            synchronized (this.mWritePerms) {
                Iterator<UriPermission> it2 = this.mWritePerms.iterator();
                while (it2.hasNext()) {
                    it2.next().uri.writeToProto(protoOutputStream, 2246267895811L);
                }
            }
        }
        protoOutputStream.end(jStart);
    }

    public String toString() {
        return this.owner.toString();
    }
}
