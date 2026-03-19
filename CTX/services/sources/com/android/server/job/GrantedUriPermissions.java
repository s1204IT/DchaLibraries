package com.android.server.job;

import android.app.IActivityManager;
import android.content.ClipData;
import android.content.ContentProvider;
import android.content.Intent;
import android.net.Uri;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.Slog;
import android.util.proto.ProtoOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;

public final class GrantedUriPermissions {
    private final int mGrantFlags;
    private final IBinder mPermissionOwner;
    private final int mSourceUserId;
    private final String mTag;
    private final ArrayList<Uri> mUris = new ArrayList<>();

    private GrantedUriPermissions(IActivityManager iActivityManager, int i, int i2, String str) throws RemoteException {
        this.mGrantFlags = i;
        this.mSourceUserId = UserHandle.getUserId(i2);
        this.mTag = str;
        this.mPermissionOwner = iActivityManager.newUriPermissionOwner("job: " + str);
    }

    public void revoke(IActivityManager iActivityManager) {
        for (int size = this.mUris.size() - 1; size >= 0; size--) {
            try {
                iActivityManager.revokeUriPermissionFromOwner(this.mPermissionOwner, this.mUris.get(size), this.mGrantFlags, this.mSourceUserId);
            } catch (RemoteException e) {
            }
        }
        this.mUris.clear();
    }

    public static boolean checkGrantFlags(int i) {
        return (i & 3) != 0;
    }

    public static GrantedUriPermissions createFromIntent(IActivityManager iActivityManager, Intent intent, int i, String str, int i2, String str2) {
        int flags = intent.getFlags();
        if (!checkGrantFlags(flags)) {
            return null;
        }
        GrantedUriPermissions grantedUriPermissionsGrantUri = null;
        Uri data = intent.getData();
        if (data != null) {
            grantedUriPermissionsGrantUri = grantUri(iActivityManager, data, i, str, i2, flags, str2, null);
        }
        ClipData clipData = intent.getClipData();
        if (clipData != null) {
            return grantClip(iActivityManager, clipData, i, str, i2, flags, str2, grantedUriPermissionsGrantUri);
        }
        return grantedUriPermissionsGrantUri;
    }

    public static GrantedUriPermissions createFromClip(IActivityManager iActivityManager, ClipData clipData, int i, String str, int i2, int i3, String str2) {
        if (!checkGrantFlags(i3) || clipData == null) {
            return null;
        }
        return grantClip(iActivityManager, clipData, i, str, i2, i3, str2, null);
    }

    private static GrantedUriPermissions grantClip(IActivityManager iActivityManager, ClipData clipData, int i, String str, int i2, int i3, String str2, GrantedUriPermissions grantedUriPermissions) {
        int itemCount = clipData.getItemCount();
        GrantedUriPermissions grantedUriPermissionsGrantItem = grantedUriPermissions;
        for (int i4 = 0; i4 < itemCount; i4++) {
            grantedUriPermissionsGrantItem = grantItem(iActivityManager, clipData.getItemAt(i4), i, str, i2, i3, str2, grantedUriPermissionsGrantItem);
        }
        return grantedUriPermissionsGrantItem;
    }

    private static GrantedUriPermissions grantUri(IActivityManager iActivityManager, Uri uri, int i, String str, int i2, int i3, String str2, GrantedUriPermissions grantedUriPermissions) {
        GrantedUriPermissions grantedUriPermissions2;
        IActivityManager iActivityManager2;
        int i4;
        int i5;
        try {
            int userIdFromUri = ContentProvider.getUserIdFromUri(uri, UserHandle.getUserId(i));
            Uri uriWithoutUserId = ContentProvider.getUriWithoutUserId(uri);
            if (grantedUriPermissions == null) {
                iActivityManager2 = iActivityManager;
                i4 = i;
                i5 = i3;
                grantedUriPermissions2 = new GrantedUriPermissions(iActivityManager2, i5, i4, str2);
            } else {
                iActivityManager2 = iActivityManager;
                i4 = i;
                i5 = i3;
                grantedUriPermissions2 = grantedUriPermissions;
            }
            try {
                iActivityManager2.grantUriPermissionFromOwner(grantedUriPermissions2.mPermissionOwner, i4, str, uriWithoutUserId, i5, userIdFromUri, i2);
                grantedUriPermissions2.mUris.add(uriWithoutUserId);
            } catch (RemoteException e) {
                Slog.e(JobSchedulerService.TAG, "AM dead");
            }
        } catch (RemoteException e2) {
            grantedUriPermissions2 = grantedUriPermissions;
        }
        return grantedUriPermissions2;
    }

    private static GrantedUriPermissions grantItem(IActivityManager iActivityManager, ClipData.Item item, int i, String str, int i2, int i3, String str2, GrantedUriPermissions grantedUriPermissions) {
        GrantedUriPermissions grantedUriPermissionsGrantUri = item.getUri() != null ? grantUri(iActivityManager, item.getUri(), i, str, i2, i3, str2, grantedUriPermissions) : grantedUriPermissions;
        Intent intent = item.getIntent();
        if (intent != null && intent.getData() != null) {
            return grantUri(iActivityManager, intent.getData(), i, str, i2, i3, str2, grantedUriPermissionsGrantUri);
        }
        return grantedUriPermissionsGrantUri;
    }

    public void dump(PrintWriter printWriter, String str) {
        printWriter.print(str);
        printWriter.print("mGrantFlags=0x");
        printWriter.print(Integer.toHexString(this.mGrantFlags));
        printWriter.print(" mSourceUserId=");
        printWriter.println(this.mSourceUserId);
        printWriter.print(str);
        printWriter.print("mTag=");
        printWriter.println(this.mTag);
        printWriter.print(str);
        printWriter.print("mPermissionOwner=");
        printWriter.println(this.mPermissionOwner);
        for (int i = 0; i < this.mUris.size(); i++) {
            printWriter.print(str);
            printWriter.print("#");
            printWriter.print(i);
            printWriter.print(": ");
            printWriter.println(this.mUris.get(i));
        }
    }

    public void dump(ProtoOutputStream protoOutputStream, long j) {
        long jStart = protoOutputStream.start(j);
        protoOutputStream.write(1120986464257L, this.mGrantFlags);
        protoOutputStream.write(1120986464258L, this.mSourceUserId);
        protoOutputStream.write(1138166333443L, this.mTag);
        protoOutputStream.write(1138166333444L, this.mPermissionOwner.toString());
        for (int i = 0; i < this.mUris.size(); i++) {
            Uri uri = this.mUris.get(i);
            if (uri != null) {
                protoOutputStream.write(2237677961221L, uri.toString());
            }
        }
        protoOutputStream.end(jStart);
    }
}
