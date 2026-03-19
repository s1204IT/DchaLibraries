package com.android.server.clipboard;

import android.app.ActivityManagerNative;
import android.app.AppGlobals;
import android.app.AppOpsManager;
import android.app.IActivityManager;
import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ContentProvider;
import android.content.Context;
import android.content.IClipboard;
import android.content.IOnPrimaryClipChangedListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.IPackageManager;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.UserInfo;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.os.IUserManager;
import android.os.Parcel;
import android.os.Process;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.util.Slog;
import android.util.SparseArray;
import java.util.HashSet;
import java.util.List;

public class ClipboardService extends IClipboard.Stub {
    private static final String TAG = "ClipboardService";
    private final AppOpsManager mAppOps;
    private final Context mContext;
    private final IBinder mPermissionOwner;
    private final PackageManager mPm;
    private SparseArray<PerUserClipboard> mClipboards = new SparseArray<>();
    private final IActivityManager mAm = ActivityManagerNative.getDefault();
    private final IUserManager mUm = ServiceManager.getService("user");

    private class ListenerInfo {
        final String mPackageName;
        final int mUid;

        ListenerInfo(int uid, String packageName) {
            this.mUid = uid;
            this.mPackageName = packageName;
        }
    }

    private class PerUserClipboard {
        ClipData primaryClip;
        final int userId;
        final RemoteCallbackList<IOnPrimaryClipChangedListener> primaryClipListeners = new RemoteCallbackList<>();
        final HashSet<String> activePermissionOwners = new HashSet<>();

        PerUserClipboard(int userId) {
            this.userId = userId;
        }
    }

    public ClipboardService(Context context) {
        this.mContext = context;
        this.mPm = context.getPackageManager();
        this.mAppOps = (AppOpsManager) context.getSystemService("appops");
        IBinder permOwner = null;
        try {
            permOwner = this.mAm.newUriPermissionOwner("clipboard");
        } catch (RemoteException e) {
            Slog.w("clipboard", "AM dead", e);
        }
        this.mPermissionOwner = permOwner;
        IntentFilter userFilter = new IntentFilter();
        userFilter.addAction("android.intent.action.USER_REMOVED");
        this.mContext.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                String action = intent.getAction();
                if (!"android.intent.action.USER_REMOVED".equals(action)) {
                    return;
                }
                ClipboardService.this.removeClipboard(intent.getIntExtra("android.intent.extra.user_handle", 0));
            }
        }, userFilter);
    }

    public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
        try {
            return super.onTransact(code, data, reply, flags);
        } catch (RuntimeException e) {
            if (!(e instanceof SecurityException)) {
                Slog.wtf("clipboard", "Exception: ", e);
            }
            throw e;
        }
    }

    private PerUserClipboard getClipboard() {
        return getClipboard(UserHandle.getCallingUserId());
    }

    private PerUserClipboard getClipboard(int userId) {
        PerUserClipboard puc;
        synchronized (this.mClipboards) {
            puc = this.mClipboards.get(userId);
            if (puc == null) {
                puc = new PerUserClipboard(userId);
                this.mClipboards.put(userId, puc);
            }
        }
        return puc;
    }

    private void removeClipboard(int userId) {
        synchronized (this.mClipboards) {
            this.mClipboards.remove(userId);
        }
    }

    public void setPrimaryClip(ClipData clip, String callingPackage) {
        int callingUid;
        synchronized (this) {
            if (clip != null) {
                if (clip.getItemCount() <= 0) {
                    throw new IllegalArgumentException("No items");
                }
                callingUid = Binder.getCallingUid();
                if (this.mAppOps.noteOp(30, callingUid, callingPackage) == 0) {
                    return;
                }
                checkDataOwnerLocked(clip, callingUid);
                int userId = UserHandle.getUserId(callingUid);
                PerUserClipboard clipboard = getClipboard(userId);
                revokeUris(clipboard);
                setPrimaryClipInternal(clipboard, clip);
                List<UserInfo> related = getRelatedProfiles(userId);
                if (related != null) {
                    int size = related.size();
                    if (size > 1) {
                        boolean canCopy = false;
                        try {
                            canCopy = !this.mUm.getUserRestrictions(userId).getBoolean("no_cross_profile_copy_paste");
                        } catch (RemoteException e) {
                            Slog.e(TAG, "Remote Exception calling UserManager: " + e);
                        }
                        if (!canCopy) {
                            clip = null;
                        } else {
                            clip.fixUrisLight(userId);
                        }
                        for (int i = 0; i < size; i++) {
                            int id = related.get(i).id;
                            if (id != userId) {
                                setPrimaryClipInternal(getClipboard(id), clip);
                            }
                        }
                    }
                }
                return;
            }
            callingUid = Binder.getCallingUid();
            if (this.mAppOps.noteOp(30, callingUid, callingPackage) == 0) {
            }
        }
    }

    List<UserInfo> getRelatedProfiles(int userId) {
        long origId = Binder.clearCallingIdentity();
        try {
            List<UserInfo> related = this.mUm.getProfiles(userId, true);
            return related;
        } catch (RemoteException e) {
            Slog.e(TAG, "Remote Exception calling UserManager: " + e);
            return null;
        } finally {
            Binder.restoreCallingIdentity(origId);
        }
    }

    void setPrimaryClipInternal(PerUserClipboard clipboard, ClipData clip) {
        clipboard.activePermissionOwners.clear();
        if (clip == null && clipboard.primaryClip == null) {
            return;
        }
        clipboard.primaryClip = clip;
        long ident = Binder.clearCallingIdentity();
        int n = clipboard.primaryClipListeners.beginBroadcast();
        for (int i = 0; i < n; i++) {
            try {
                ListenerInfo li = (ListenerInfo) clipboard.primaryClipListeners.getBroadcastCookie(i);
                if (this.mAppOps.checkOpNoThrow(29, li.mUid, li.mPackageName) == 0) {
                    clipboard.primaryClipListeners.getBroadcastItem(i).dispatchPrimaryClipChanged();
                }
            } catch (RemoteException e) {
            } catch (Throwable th) {
                clipboard.primaryClipListeners.finishBroadcast();
                Binder.restoreCallingIdentity(ident);
                throw th;
            }
        }
        clipboard.primaryClipListeners.finishBroadcast();
        Binder.restoreCallingIdentity(ident);
    }

    public ClipData getPrimaryClip(String pkg) {
        synchronized (this) {
            if (this.mAppOps.noteOp(29, Binder.getCallingUid(), pkg) != 0 || isDeviceLocked()) {
                return null;
            }
            addActiveOwnerLocked(Binder.getCallingUid(), pkg);
            return getClipboard().primaryClip;
        }
    }

    public ClipDescription getPrimaryClipDescription(String callingPackage) {
        synchronized (this) {
            if (this.mAppOps.checkOp(29, Binder.getCallingUid(), callingPackage) != 0 || isDeviceLocked()) {
                return null;
            }
            PerUserClipboard clipboard = getClipboard();
            return clipboard.primaryClip != null ? clipboard.primaryClip.getDescription() : null;
        }
    }

    public boolean hasPrimaryClip(String callingPackage) {
        synchronized (this) {
            if (this.mAppOps.checkOp(29, Binder.getCallingUid(), callingPackage) != 0 || isDeviceLocked()) {
                return false;
            }
            return getClipboard().primaryClip != null;
        }
    }

    public void addPrimaryClipChangedListener(IOnPrimaryClipChangedListener listener, String callingPackage) {
        synchronized (this) {
            getClipboard().primaryClipListeners.register(listener, new ListenerInfo(Binder.getCallingUid(), callingPackage));
        }
    }

    public void removePrimaryClipChangedListener(IOnPrimaryClipChangedListener listener) {
        synchronized (this) {
            getClipboard().primaryClipListeners.unregister(listener);
        }
    }

    public boolean hasClipboardText(String callingPackage) {
        boolean z = false;
        synchronized (this) {
            if (this.mAppOps.checkOp(29, Binder.getCallingUid(), callingPackage) != 0 || isDeviceLocked()) {
                return false;
            }
            PerUserClipboard clipboard = getClipboard();
            if (clipboard.primaryClip == null) {
                return false;
            }
            CharSequence text = clipboard.primaryClip.getItemAt(0).getText();
            if (text != null) {
                if (text.length() > 0) {
                    z = true;
                }
            }
            return z;
        }
    }

    private boolean isDeviceLocked() {
        int callingUserId = UserHandle.getCallingUserId();
        long token = Binder.clearCallingIdentity();
        try {
            KeyguardManager keyguardManager = (KeyguardManager) this.mContext.getSystemService(KeyguardManager.class);
            return keyguardManager != null ? keyguardManager.isDeviceLocked(callingUserId) : false;
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    private final void checkUriOwnerLocked(Uri uri, int uid) {
        if (!"content".equals(uri.getScheme())) {
            return;
        }
        long ident = Binder.clearCallingIdentity();
        try {
            this.mAm.checkGrantUriPermission(uid, (String) null, ContentProvider.getUriWithoutUserId(uri), 1, ContentProvider.getUserIdFromUri(uri, UserHandle.getUserId(uid)));
        } catch (RemoteException e) {
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    private final void checkItemOwnerLocked(ClipData.Item item, int uid) {
        if (item.getUri() != null) {
            checkUriOwnerLocked(item.getUri(), uid);
        }
        Intent intent = item.getIntent();
        if (intent == null || intent.getData() == null) {
            return;
        }
        checkUriOwnerLocked(intent.getData(), uid);
    }

    private final void checkDataOwnerLocked(ClipData data, int uid) {
        int N = data.getItemCount();
        for (int i = 0; i < N; i++) {
            checkItemOwnerLocked(data.getItemAt(i), uid);
        }
    }

    private final void grantUriLocked(Uri uri, String pkg, int userId) {
        long ident = Binder.clearCallingIdentity();
        try {
            int sourceUserId = ContentProvider.getUserIdFromUri(uri, userId);
            this.mAm.grantUriPermissionFromOwner(this.mPermissionOwner, Process.myUid(), pkg, ContentProvider.getUriWithoutUserId(uri), 1, sourceUserId, userId);
        } catch (RemoteException e) {
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    private final void grantItemLocked(ClipData.Item item, String pkg, int userId) {
        if (item.getUri() != null) {
            grantUriLocked(item.getUri(), pkg, userId);
        }
        Intent intent = item.getIntent();
        if (intent == null || intent.getData() == null) {
            return;
        }
        grantUriLocked(intent.getData(), pkg, userId);
    }

    private final void addActiveOwnerLocked(int uid, String pkg) {
        PackageInfo pi;
        IPackageManager pm = AppGlobals.getPackageManager();
        int targetUserHandle = UserHandle.getCallingUserId();
        long oldIdentity = Binder.clearCallingIdentity();
        try {
            pi = pm.getPackageInfo(pkg, 0, targetUserHandle);
        } catch (RemoteException e) {
        } finally {
            Binder.restoreCallingIdentity(oldIdentity);
        }
        if (pi == null) {
            throw new IllegalArgumentException("Unknown package " + pkg);
        }
        if (!UserHandle.isSameApp(pi.applicationInfo.uid, uid)) {
            throw new SecurityException("Calling uid " + uid + " does not own package " + pkg);
        }
        PerUserClipboard clipboard = getClipboard();
        if (clipboard.primaryClip == null || clipboard.activePermissionOwners.contains(pkg)) {
            return;
        }
        int N = clipboard.primaryClip.getItemCount();
        for (int i = 0; i < N; i++) {
            grantItemLocked(clipboard.primaryClip.getItemAt(i), pkg, UserHandle.getUserId(uid));
        }
        clipboard.activePermissionOwners.add(pkg);
    }

    private final void revokeUriLocked(Uri uri) {
        int userId = ContentProvider.getUserIdFromUri(uri, UserHandle.getUserId(Binder.getCallingUid()));
        long ident = Binder.clearCallingIdentity();
        try {
            this.mAm.revokeUriPermissionFromOwner(this.mPermissionOwner, ContentProvider.getUriWithoutUserId(uri), 3, userId);
        } catch (RemoteException e) {
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    private final void revokeItemLocked(ClipData.Item item) {
        if (item.getUri() != null) {
            revokeUriLocked(item.getUri());
        }
        Intent intent = item.getIntent();
        if (intent == null || intent.getData() == null) {
            return;
        }
        revokeUriLocked(intent.getData());
    }

    private final void revokeUris(PerUserClipboard clipboard) {
        if (clipboard.primaryClip == null) {
            return;
        }
        int N = clipboard.primaryClip.getItemCount();
        for (int i = 0; i < N; i++) {
            revokeItemLocked(clipboard.primaryClip.getItemAt(i));
        }
    }
}
