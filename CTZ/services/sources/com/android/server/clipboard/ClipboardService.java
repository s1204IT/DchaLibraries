package com.android.server.clipboard;

import android.app.ActivityManager;
import android.app.AppGlobals;
import android.app.AppOpsManager;
import android.app.IActivityManager;
import android.app.KeyguardManager;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ContentProvider;
import android.content.Context;
import android.content.IClipboard;
import android.content.IOnPrimaryClipChangedListener;
import android.content.Intent;
import android.content.pm.IPackageManager;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.UserInfo;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.os.IUserManager;
import android.os.Parcel;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.util.Slog;
import android.util.SparseArray;
import com.android.server.SystemService;
import com.android.server.backup.BackupManagerConstants;
import com.android.server.clipboard.HostClipboardMonitor;
import java.util.HashSet;
import java.util.List;

public class ClipboardService extends SystemService {
    private static final boolean IS_EMULATOR = SystemProperties.getBoolean("ro.kernel.qemu", false);
    private static final String TAG = "ClipboardService";
    private final IActivityManager mAm;
    private final AppOpsManager mAppOps;
    private final SparseArray<PerUserClipboard> mClipboards;
    private HostClipboardMonitor mHostClipboardMonitor;
    private Thread mHostMonitorThread;
    private final IBinder mPermissionOwner;
    private final PackageManager mPm;
    private final IUserManager mUm;

    public ClipboardService(Context context) {
        super(context);
        IBinder iBinderNewUriPermissionOwner = null;
        this.mHostClipboardMonitor = null;
        this.mHostMonitorThread = null;
        this.mClipboards = new SparseArray<>();
        this.mAm = ActivityManager.getService();
        this.mPm = getContext().getPackageManager();
        this.mUm = ServiceManager.getService("user");
        this.mAppOps = (AppOpsManager) getContext().getSystemService("appops");
        try {
            iBinderNewUriPermissionOwner = this.mAm.newUriPermissionOwner("clipboard");
        } catch (RemoteException e) {
            Slog.w("clipboard", "AM dead", e);
        }
        this.mPermissionOwner = iBinderNewUriPermissionOwner;
        if (IS_EMULATOR) {
            this.mHostClipboardMonitor = new HostClipboardMonitor(new HostClipboardMonitor.HostClipboardCallback() {
                @Override
                public void onHostClipboardUpdated(String str) {
                    ClipData clipData = new ClipData("host clipboard", new String[]{"text/plain"}, new ClipData.Item(str));
                    synchronized (ClipboardService.this.mClipboards) {
                        ClipboardService.this.setPrimaryClipInternal(ClipboardService.this.getClipboard(0), clipData, 1000);
                    }
                }
            });
            this.mHostMonitorThread = new Thread(this.mHostClipboardMonitor);
            this.mHostMonitorThread.start();
        }
    }

    @Override
    public void onStart() {
        publishBinderService("clipboard", new ClipboardImpl());
    }

    @Override
    public void onCleanupUser(int i) {
        synchronized (this.mClipboards) {
            this.mClipboards.remove(i);
        }
    }

    private class ListenerInfo {
        final String mPackageName;
        final int mUid;

        ListenerInfo(int i, String str) {
            this.mUid = i;
            this.mPackageName = str;
        }
    }

    private class PerUserClipboard {
        ClipData primaryClip;
        final int userId;
        final RemoteCallbackList<IOnPrimaryClipChangedListener> primaryClipListeners = new RemoteCallbackList<>();
        int primaryClipUid = 9999;
        final HashSet<String> activePermissionOwners = new HashSet<>();

        PerUserClipboard(int i) {
            this.userId = i;
        }
    }

    private class ClipboardImpl extends IClipboard.Stub {
        private ClipboardImpl() {
        }

        public boolean onTransact(int i, Parcel parcel, Parcel parcel2, int i2) throws RemoteException {
            try {
                return super.onTransact(i, parcel, parcel2, i2);
            } catch (RuntimeException e) {
                if (!(e instanceof SecurityException)) {
                    Slog.wtf("clipboard", "Exception: ", e);
                }
                throw e;
            }
        }

        public void setPrimaryClip(ClipData clipData, String str) {
            synchronized (this) {
                if (clipData != null) {
                    try {
                        if (clipData.getItemCount() > 0) {
                            int callingUid = Binder.getCallingUid();
                            if (ClipboardService.this.clipboardAccessAllowed(30, str, callingUid)) {
                                ClipboardService.this.checkDataOwnerLocked(clipData, callingUid);
                                ClipboardService.this.setPrimaryClipInternal(clipData, callingUid);
                                return;
                            }
                            return;
                        }
                    } catch (Throwable th) {
                        throw th;
                    }
                }
                throw new IllegalArgumentException("No items");
            }
        }

        public void clearPrimaryClip(String str) {
            synchronized (this) {
                int callingUid = Binder.getCallingUid();
                if (ClipboardService.this.clipboardAccessAllowed(30, str, callingUid)) {
                    ClipboardService.this.setPrimaryClipInternal(null, callingUid);
                }
            }
        }

        public ClipData getPrimaryClip(String str) {
            synchronized (this) {
                if (ClipboardService.this.clipboardAccessAllowed(29, str, Binder.getCallingUid()) && !ClipboardService.this.isDeviceLocked()) {
                    ClipboardService.this.addActiveOwnerLocked(Binder.getCallingUid(), str);
                    return ClipboardService.this.getClipboard().primaryClip;
                }
                return null;
            }
        }

        public ClipDescription getPrimaryClipDescription(String str) {
            synchronized (this) {
                if (ClipboardService.this.clipboardAccessAllowed(29, str, Binder.getCallingUid()) && !ClipboardService.this.isDeviceLocked()) {
                    PerUserClipboard clipboard = ClipboardService.this.getClipboard();
                    return clipboard.primaryClip != null ? clipboard.primaryClip.getDescription() : null;
                }
                return null;
            }
        }

        public boolean hasPrimaryClip(String str) {
            synchronized (this) {
                if (ClipboardService.this.clipboardAccessAllowed(29, str, Binder.getCallingUid()) && !ClipboardService.this.isDeviceLocked()) {
                    return ClipboardService.this.getClipboard().primaryClip != null;
                }
                return false;
            }
        }

        public void addPrimaryClipChangedListener(IOnPrimaryClipChangedListener iOnPrimaryClipChangedListener, String str) {
            synchronized (this) {
                ClipboardService.this.getClipboard().primaryClipListeners.register(iOnPrimaryClipChangedListener, ClipboardService.this.new ListenerInfo(Binder.getCallingUid(), str));
            }
        }

        public void removePrimaryClipChangedListener(IOnPrimaryClipChangedListener iOnPrimaryClipChangedListener) {
            synchronized (this) {
                ClipboardService.this.getClipboard().primaryClipListeners.unregister(iOnPrimaryClipChangedListener);
            }
        }

        public boolean hasClipboardText(String str) {
            synchronized (this) {
                boolean z = false;
                if (ClipboardService.this.clipboardAccessAllowed(29, str, Binder.getCallingUid()) && !ClipboardService.this.isDeviceLocked()) {
                    PerUserClipboard clipboard = ClipboardService.this.getClipboard();
                    if (clipboard.primaryClip == null) {
                        return false;
                    }
                    CharSequence text = clipboard.primaryClip.getItemAt(0).getText();
                    if (text != null && text.length() > 0) {
                        z = true;
                    }
                    return z;
                }
                return false;
            }
        }
    }

    private PerUserClipboard getClipboard() {
        return getClipboard(UserHandle.getCallingUserId());
    }

    private PerUserClipboard getClipboard(int i) {
        PerUserClipboard perUserClipboard;
        synchronized (this.mClipboards) {
            perUserClipboard = this.mClipboards.get(i);
            if (perUserClipboard == null) {
                perUserClipboard = new PerUserClipboard(i);
                this.mClipboards.put(i, perUserClipboard);
            }
        }
        return perUserClipboard;
    }

    List<UserInfo> getRelatedProfiles(int i) {
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            return this.mUm.getProfiles(i, true);
        } catch (RemoteException e) {
            Slog.e(TAG, "Remote Exception calling UserManager: " + e);
            return null;
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    private boolean hasRestriction(String str, int i) {
        try {
            return this.mUm.hasUserRestriction(str, i);
        } catch (RemoteException e) {
            Slog.e(TAG, "Remote Exception calling UserManager.getUserRestrictions: ", e);
            return true;
        }
    }

    void setPrimaryClipInternal(ClipData clipData, int i) {
        int size;
        ClipData clipData2;
        CharSequence text;
        if (this.mHostClipboardMonitor != null) {
            if (clipData == null) {
                this.mHostClipboardMonitor.setHostClipboard(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
            } else if (clipData.getItemCount() > 0 && (text = clipData.getItemAt(0).getText()) != null) {
                this.mHostClipboardMonitor.setHostClipboard(text.toString());
            }
        }
        int userId = UserHandle.getUserId(i);
        setPrimaryClipInternal(getClipboard(userId), clipData, i);
        List<UserInfo> relatedProfiles = getRelatedProfiles(userId);
        if (relatedProfiles != null && (size = relatedProfiles.size()) > 1) {
            if (!(!hasRestriction("no_cross_profile_copy_paste", userId))) {
                clipData2 = null;
            } else {
                ClipData clipData3 = new ClipData(clipData);
                for (int itemCount = clipData3.getItemCount() - 1; itemCount >= 0; itemCount--) {
                    clipData3.setItemAt(itemCount, new ClipData.Item(clipData3.getItemAt(itemCount)));
                }
                clipData3.fixUrisLight(userId);
                clipData2 = clipData3;
            }
            for (int i2 = 0; i2 < size; i2++) {
                int i3 = relatedProfiles.get(i2).id;
                if (i3 != userId && (!hasRestriction("no_sharing_into_profile", i3))) {
                    setPrimaryClipInternal(getClipboard(i3), clipData2, i);
                }
            }
        }
    }

    void setPrimaryClipInternal(PerUserClipboard perUserClipboard, ClipData clipData, int i) {
        ClipDescription description;
        revokeUris(perUserClipboard);
        perUserClipboard.activePermissionOwners.clear();
        if (clipData == null && perUserClipboard.primaryClip == null) {
            return;
        }
        perUserClipboard.primaryClip = clipData;
        if (clipData != null) {
            perUserClipboard.primaryClipUid = i;
        } else {
            perUserClipboard.primaryClipUid = 9999;
        }
        if (clipData != null && (description = clipData.getDescription()) != null) {
            description.setTimestamp(System.currentTimeMillis());
        }
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        int iBeginBroadcast = perUserClipboard.primaryClipListeners.beginBroadcast();
        for (int i2 = 0; i2 < iBeginBroadcast; i2++) {
            try {
                ListenerInfo listenerInfo = (ListenerInfo) perUserClipboard.primaryClipListeners.getBroadcastCookie(i2);
                if (clipboardAccessAllowed(29, listenerInfo.mPackageName, listenerInfo.mUid)) {
                    perUserClipboard.primaryClipListeners.getBroadcastItem(i2).dispatchPrimaryClipChanged();
                }
            } catch (RemoteException e) {
            } catch (Throwable th) {
                perUserClipboard.primaryClipListeners.finishBroadcast();
                Binder.restoreCallingIdentity(jClearCallingIdentity);
                throw th;
            }
        }
        perUserClipboard.primaryClipListeners.finishBroadcast();
        Binder.restoreCallingIdentity(jClearCallingIdentity);
    }

    private boolean isDeviceLocked() {
        boolean z;
        int callingUserId = UserHandle.getCallingUserId();
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            KeyguardManager keyguardManager = (KeyguardManager) getContext().getSystemService(KeyguardManager.class);
            if (keyguardManager != null) {
                z = keyguardManager.isDeviceLocked(callingUserId);
            }
            return z;
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    private final void checkUriOwnerLocked(Uri uri, int i) {
        if (uri == null || !"content".equals(uri.getScheme())) {
            return;
        }
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            this.mAm.checkGrantUriPermission(i, (String) null, ContentProvider.getUriWithoutUserId(uri), 1, ContentProvider.getUserIdFromUri(uri, UserHandle.getUserId(i)));
        } catch (RemoteException e) {
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
            throw th;
        }
        Binder.restoreCallingIdentity(jClearCallingIdentity);
    }

    private final void checkItemOwnerLocked(ClipData.Item item, int i) {
        if (item.getUri() != null) {
            checkUriOwnerLocked(item.getUri(), i);
        }
        Intent intent = item.getIntent();
        if (intent != null && intent.getData() != null) {
            checkUriOwnerLocked(intent.getData(), i);
        }
    }

    private final void checkDataOwnerLocked(ClipData clipData, int i) {
        int itemCount = clipData.getItemCount();
        for (int i2 = 0; i2 < itemCount; i2++) {
            checkItemOwnerLocked(clipData.getItemAt(i2), i);
        }
    }

    private final void grantUriLocked(Uri uri, int i, String str, int i2) {
        if (uri == null || !"content".equals(uri.getScheme())) {
            return;
        }
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            this.mAm.grantUriPermissionFromOwner(this.mPermissionOwner, i, str, ContentProvider.getUriWithoutUserId(uri), 1, ContentProvider.getUserIdFromUri(uri, UserHandle.getUserId(i)), i2);
        } catch (RemoteException e) {
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
            throw th;
        }
        Binder.restoreCallingIdentity(jClearCallingIdentity);
    }

    private final void grantItemLocked(ClipData.Item item, int i, String str, int i2) {
        if (item.getUri() != null) {
            grantUriLocked(item.getUri(), i, str, i2);
        }
        Intent intent = item.getIntent();
        if (intent != null && intent.getData() != null) {
            grantUriLocked(intent.getData(), i, str, i2);
        }
    }

    private final void addActiveOwnerLocked(int i, String str) {
        PackageInfo packageInfo;
        IPackageManager packageManager = AppGlobals.getPackageManager();
        int callingUserId = UserHandle.getCallingUserId();
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            packageInfo = packageManager.getPackageInfo(str, 0, callingUserId);
        } catch (RemoteException e) {
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
            throw th;
        }
        if (packageInfo == null) {
            throw new IllegalArgumentException("Unknown package " + str);
        }
        if (!UserHandle.isSameApp(packageInfo.applicationInfo.uid, i)) {
            throw new SecurityException("Calling uid " + i + " does not own package " + str);
        }
        Binder.restoreCallingIdentity(jClearCallingIdentity);
        PerUserClipboard clipboard = getClipboard();
        if (clipboard.primaryClip != null && !clipboard.activePermissionOwners.contains(str)) {
            int itemCount = clipboard.primaryClip.getItemCount();
            for (int i2 = 0; i2 < itemCount; i2++) {
                grantItemLocked(clipboard.primaryClip.getItemAt(i2), clipboard.primaryClipUid, str, UserHandle.getUserId(i));
            }
            clipboard.activePermissionOwners.add(str);
        }
    }

    private final void revokeUriLocked(Uri uri, int i) {
        if (uri == null || !"content".equals(uri.getScheme())) {
            return;
        }
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            this.mAm.revokeUriPermissionFromOwner(this.mPermissionOwner, ContentProvider.getUriWithoutUserId(uri), 1, ContentProvider.getUserIdFromUri(uri, UserHandle.getUserId(i)));
        } catch (RemoteException e) {
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
            throw th;
        }
        Binder.restoreCallingIdentity(jClearCallingIdentity);
    }

    private final void revokeItemLocked(ClipData.Item item, int i) {
        if (item.getUri() != null) {
            revokeUriLocked(item.getUri(), i);
        }
        Intent intent = item.getIntent();
        if (intent != null && intent.getData() != null) {
            revokeUriLocked(intent.getData(), i);
        }
    }

    private final void revokeUris(PerUserClipboard perUserClipboard) {
        if (perUserClipboard.primaryClip == null) {
            return;
        }
        int itemCount = perUserClipboard.primaryClip.getItemCount();
        for (int i = 0; i < itemCount; i++) {
            revokeItemLocked(perUserClipboard.primaryClip.getItemAt(i), perUserClipboard.primaryClipUid);
        }
    }

    private boolean clipboardAccessAllowed(int i, String str, int i2) {
        if (this.mAppOps.noteOp(i, i2, str) != 0) {
            return false;
        }
        try {
            if (!AppGlobals.getPackageManager().isInstantApp(str, UserHandle.getUserId(i2))) {
                return true;
            }
            return this.mAm.isAppForeground(i2);
        } catch (RemoteException e) {
            Slog.e("clipboard", "Failed to get Instant App status for package " + str, e);
            return false;
        }
    }
}
