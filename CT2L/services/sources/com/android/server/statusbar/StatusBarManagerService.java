package com.android.server.statusbar;

import android.R;
import android.content.Context;
import android.content.res.Resources;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.Slog;
import com.android.internal.statusbar.IStatusBar;
import com.android.internal.statusbar.IStatusBarService;
import com.android.internal.statusbar.StatusBarIcon;
import com.android.internal.statusbar.StatusBarIconList;
import com.android.server.LocalServices;
import com.android.server.notification.NotificationDelegate;
import com.android.server.wm.WindowManagerService;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class StatusBarManagerService extends IStatusBarService.Stub {
    private static final boolean SPEW = false;
    private static final String TAG = "StatusBarManagerService";
    private volatile IStatusBar mBar;
    private final Context mContext;
    private int mCurrentUserId;
    private int mImeBackDisposition;
    private NotificationDelegate mNotificationDelegate;
    private boolean mShowImeSwitcher;
    private final WindowManagerService mWindowManager;
    private Handler mHandler = new Handler();
    private StatusBarIconList mIcons = new StatusBarIconList();
    private final ArrayList<DisableRecord> mDisableRecords = new ArrayList<>();
    private IBinder mSysUiVisToken = new Binder();
    private int mDisabled = 0;
    private Object mLock = new Object();
    private int mSystemUiVisibility = 0;
    private boolean mMenuVisible = SPEW;
    private int mImeWindowVis = 0;
    private IBinder mImeToken = null;
    private final StatusBarManagerInternal mInternalService = new StatusBarManagerInternal() {
        private boolean mNotificationLightOn;

        @Override
        public void setNotificationDelegate(NotificationDelegate delegate) {
            StatusBarManagerService.this.mNotificationDelegate = delegate;
        }

        @Override
        public void buzzBeepBlinked() {
            if (StatusBarManagerService.this.mBar != null) {
                try {
                    StatusBarManagerService.this.mBar.buzzBeepBlinked();
                } catch (RemoteException e) {
                }
            }
        }

        @Override
        public void notificationLightPulse(int argb, int onMillis, int offMillis) {
            this.mNotificationLightOn = true;
            if (StatusBarManagerService.this.mBar != null) {
                try {
                    StatusBarManagerService.this.mBar.notificationLightPulse(argb, onMillis, offMillis);
                } catch (RemoteException e) {
                }
            }
        }

        @Override
        public void notificationLightOff() {
            if (this.mNotificationLightOn) {
                this.mNotificationLightOn = StatusBarManagerService.SPEW;
                if (StatusBarManagerService.this.mBar != null) {
                    try {
                        StatusBarManagerService.this.mBar.notificationLightOff();
                    } catch (RemoteException e) {
                    }
                }
            }
        }

        @Override
        public void showScreenPinningRequest() {
            if (StatusBarManagerService.this.mBar != null) {
                try {
                    StatusBarManagerService.this.mBar.showScreenPinningRequest();
                } catch (RemoteException e) {
                }
            }
        }
    };

    private class DisableRecord implements IBinder.DeathRecipient {
        String pkg;
        IBinder token;
        int userId;
        int what;

        private DisableRecord() {
        }

        @Override
        public void binderDied() {
            Slog.i(StatusBarManagerService.TAG, "binder died for pkg=" + this.pkg);
            StatusBarManagerService.this.disableInternal(this.userId, 0, this.token, this.pkg);
            this.token.unlinkToDeath(this, 0);
        }
    }

    public StatusBarManagerService(Context context, WindowManagerService windowManager) {
        this.mContext = context;
        this.mWindowManager = windowManager;
        Resources res = context.getResources();
        this.mIcons.defineSlots(res.getStringArray(R.array.config_allowedSecureInstantAppSettings));
        LocalServices.addService(StatusBarManagerInternal.class, this.mInternalService);
    }

    public void expandNotificationsPanel() {
        enforceExpandStatusBar();
        if (this.mBar != null) {
            try {
                this.mBar.animateExpandNotificationsPanel();
            } catch (RemoteException e) {
            }
        }
    }

    public void collapsePanels() {
        enforceExpandStatusBar();
        if (this.mBar != null) {
            try {
                this.mBar.animateCollapsePanels();
            } catch (RemoteException e) {
            }
        }
    }

    public void expandSettingsPanel() {
        enforceExpandStatusBar();
        if (this.mBar != null) {
            try {
                this.mBar.animateExpandSettingsPanel();
            } catch (RemoteException e) {
            }
        }
    }

    public void disable(int what, IBinder token, String pkg) {
        disableInternal(this.mCurrentUserId, what, token, pkg);
    }

    private void disableInternal(int userId, int what, IBinder token, String pkg) {
        enforceStatusBar();
        synchronized (this.mLock) {
            disableLocked(userId, what, token, pkg);
        }
    }

    private void disableLocked(int userId, int what, IBinder token, String pkg) {
        manageDisableListLocked(userId, what, token, pkg);
        final int net = gatherDisableActionsLocked(this.mCurrentUserId);
        if (net != this.mDisabled) {
            this.mDisabled = net;
            this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    StatusBarManagerService.this.mNotificationDelegate.onSetDisabled(net);
                }
            });
            if (this.mBar != null) {
                try {
                    this.mBar.disable(net);
                } catch (RemoteException e) {
                }
            }
        }
    }

    public void setIcon(String slot, String iconPackage, int iconId, int iconLevel, String contentDescription) {
        enforceStatusBar();
        synchronized (this.mIcons) {
            int index = this.mIcons.getSlotIndex(slot);
            if (index < 0) {
                throw new SecurityException("invalid status bar icon slot: " + slot);
            }
            StatusBarIcon icon = new StatusBarIcon(iconPackage, UserHandle.OWNER, iconId, iconLevel, 0, contentDescription);
            this.mIcons.setIcon(index, icon);
            if (this.mBar != null) {
                try {
                    this.mBar.setIcon(index, icon);
                } catch (RemoteException e) {
                }
            }
        }
    }

    public void setIconVisibility(String slot, boolean visible) {
        enforceStatusBar();
        synchronized (this.mIcons) {
            int index = this.mIcons.getSlotIndex(slot);
            if (index < 0) {
                throw new SecurityException("invalid status bar icon slot: " + slot);
            }
            StatusBarIcon icon = this.mIcons.getIcon(index);
            if (icon != null) {
                if (icon.visible != visible) {
                    icon.visible = visible;
                    if (this.mBar != null) {
                        try {
                            this.mBar.setIcon(index, icon);
                        } catch (RemoteException e) {
                        }
                    }
                }
            }
        }
    }

    public void removeIcon(String slot) {
        enforceStatusBar();
        synchronized (this.mIcons) {
            int index = this.mIcons.getSlotIndex(slot);
            if (index < 0) {
                throw new SecurityException("invalid status bar icon slot: " + slot);
            }
            this.mIcons.removeIcon(index);
            if (this.mBar != null) {
                try {
                    this.mBar.removeIcon(index);
                } catch (RemoteException e) {
                }
            }
        }
    }

    public void topAppWindowChanged(final boolean menuVisible) {
        enforceStatusBar();
        synchronized (this.mLock) {
            this.mMenuVisible = menuVisible;
            this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (StatusBarManagerService.this.mBar != null) {
                        try {
                            StatusBarManagerService.this.mBar.topAppWindowChanged(menuVisible);
                        } catch (RemoteException e) {
                        }
                    }
                }
            });
        }
    }

    public void setImeWindowStatus(final IBinder token, final int vis, final int backDisposition, final boolean showImeSwitcher) {
        enforceStatusBar();
        synchronized (this.mLock) {
            this.mImeWindowVis = vis;
            this.mImeBackDisposition = backDisposition;
            this.mImeToken = token;
            this.mShowImeSwitcher = showImeSwitcher;
            this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (StatusBarManagerService.this.mBar != null) {
                        try {
                            StatusBarManagerService.this.mBar.setImeWindowStatus(token, vis, backDisposition, showImeSwitcher);
                        } catch (RemoteException e) {
                        }
                    }
                }
            });
        }
    }

    public void setSystemUiVisibility(int vis, int mask, String cause) {
        enforceStatusBarService();
        synchronized (this.mLock) {
            updateUiVisibilityLocked(vis, mask);
            disableLocked(this.mCurrentUserId, 67043328 & vis, this.mSysUiVisToken, cause);
        }
    }

    private void updateUiVisibilityLocked(final int vis, final int mask) {
        if (this.mSystemUiVisibility != vis) {
            this.mSystemUiVisibility = vis;
            this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (StatusBarManagerService.this.mBar != null) {
                        try {
                            StatusBarManagerService.this.mBar.setSystemUiVisibility(vis, mask);
                        } catch (RemoteException e) {
                        }
                    }
                }
            });
        }
    }

    public void toggleRecentApps() {
        if (this.mBar != null) {
            try {
                this.mBar.toggleRecentApps();
            } catch (RemoteException e) {
            }
        }
    }

    public void preloadRecentApps() {
        if (this.mBar != null) {
            try {
                this.mBar.preloadRecentApps();
            } catch (RemoteException e) {
            }
        }
    }

    public void cancelPreloadRecentApps() {
        if (this.mBar != null) {
            try {
                this.mBar.cancelPreloadRecentApps();
            } catch (RemoteException e) {
            }
        }
    }

    public void showRecentApps(boolean triggeredFromAltTab) {
        if (this.mBar != null) {
            try {
                this.mBar.showRecentApps(triggeredFromAltTab);
            } catch (RemoteException e) {
            }
        }
    }

    public void hideRecentApps(boolean triggeredFromAltTab, boolean triggeredFromHomeKey) {
        if (this.mBar != null) {
            try {
                this.mBar.hideRecentApps(triggeredFromAltTab, triggeredFromHomeKey);
            } catch (RemoteException e) {
            }
        }
    }

    public void setCurrentUser(int newUserId) {
        this.mCurrentUserId = newUserId;
    }

    public void setWindowState(int window, int state) {
        if (this.mBar != null) {
            try {
                this.mBar.setWindowState(window, state);
            } catch (RemoteException e) {
            }
        }
    }

    private void enforceStatusBar() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.STATUS_BAR", TAG);
    }

    private void enforceExpandStatusBar() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.EXPAND_STATUS_BAR", TAG);
    }

    private void enforceStatusBarService() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.STATUS_BAR_SERVICE", TAG);
    }

    public void registerStatusBar(IStatusBar bar, StatusBarIconList iconList, int[] switches, List<IBinder> binders) {
        enforceStatusBarService();
        Slog.i(TAG, "registerStatusBar bar=" + bar);
        this.mBar = bar;
        synchronized (this.mIcons) {
            iconList.copyFrom(this.mIcons);
        }
        synchronized (this.mLock) {
            switches[0] = gatherDisableActionsLocked(this.mCurrentUserId);
            switches[1] = this.mSystemUiVisibility;
            switches[2] = this.mMenuVisible ? 1 : 0;
            switches[3] = this.mImeWindowVis;
            switches[4] = this.mImeBackDisposition;
            switches[5] = this.mShowImeSwitcher ? 1 : 0;
            binders.add(this.mImeToken);
        }
    }

    public void onPanelRevealed(boolean clearNotificationEffects) {
        enforceStatusBarService();
        long identity = Binder.clearCallingIdentity();
        try {
            this.mNotificationDelegate.onPanelRevealed(clearNotificationEffects);
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    public void clearNotificationEffects() throws RemoteException {
        enforceStatusBarService();
        long identity = Binder.clearCallingIdentity();
        try {
            this.mNotificationDelegate.clearEffects();
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    public void onPanelHidden() throws RemoteException {
        enforceStatusBarService();
        long identity = Binder.clearCallingIdentity();
        try {
            this.mNotificationDelegate.onPanelHidden();
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    public void onNotificationClick(String key) {
        enforceStatusBarService();
        int callingUid = Binder.getCallingUid();
        int callingPid = Binder.getCallingPid();
        long identity = Binder.clearCallingIdentity();
        try {
            this.mNotificationDelegate.onNotificationClick(callingUid, callingPid, key);
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    public void onNotificationActionClick(String key, int actionIndex) {
        enforceStatusBarService();
        int callingUid = Binder.getCallingUid();
        int callingPid = Binder.getCallingPid();
        long identity = Binder.clearCallingIdentity();
        try {
            this.mNotificationDelegate.onNotificationActionClick(callingUid, callingPid, key, actionIndex);
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    public void onNotificationError(String pkg, String tag, int id, int uid, int initialPid, String message, int userId) {
        enforceStatusBarService();
        int callingUid = Binder.getCallingUid();
        int callingPid = Binder.getCallingPid();
        long identity = Binder.clearCallingIdentity();
        try {
            this.mNotificationDelegate.onNotificationError(callingUid, callingPid, pkg, tag, id, uid, initialPid, message, userId);
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    public void onNotificationClear(String pkg, String tag, int id, int userId) {
        enforceStatusBarService();
        int callingUid = Binder.getCallingUid();
        int callingPid = Binder.getCallingPid();
        long identity = Binder.clearCallingIdentity();
        try {
            this.mNotificationDelegate.onNotificationClear(callingUid, callingPid, pkg, tag, id, userId);
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    public void onNotificationVisibilityChanged(String[] newlyVisibleKeys, String[] noLongerVisibleKeys) throws RemoteException {
        enforceStatusBarService();
        long identity = Binder.clearCallingIdentity();
        try {
            this.mNotificationDelegate.onNotificationVisibilityChanged(newlyVisibleKeys, noLongerVisibleKeys);
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    public void onNotificationExpansionChanged(String key, boolean userAction, boolean expanded) throws RemoteException {
        enforceStatusBarService();
        long identity = Binder.clearCallingIdentity();
        try {
            this.mNotificationDelegate.onNotificationExpansionChanged(key, userAction, expanded);
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    public void onClearAllNotifications(int userId) {
        enforceStatusBarService();
        int callingUid = Binder.getCallingUid();
        int callingPid = Binder.getCallingPid();
        long identity = Binder.clearCallingIdentity();
        try {
            this.mNotificationDelegate.onClearAll(callingUid, callingPid, userId);
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    void manageDisableListLocked(int userId, int what, IBinder token, String pkg) {
        int N = this.mDisableRecords.size();
        DisableRecord tok = null;
        int i = 0;
        while (true) {
            if (i >= N) {
                break;
            }
            DisableRecord t = this.mDisableRecords.get(i);
            if (t.token != token || t.userId != userId) {
                i++;
            } else {
                tok = t;
                break;
            }
        }
        if (what == 0 || !token.isBinderAlive()) {
            if (tok != null) {
                this.mDisableRecords.remove(i);
                tok.token.unlinkToDeath(tok, 0);
                return;
            }
            return;
        }
        if (tok == null) {
            tok = new DisableRecord();
            tok.userId = userId;
            try {
                token.linkToDeath(tok, 0);
                this.mDisableRecords.add(tok);
            } catch (RemoteException e) {
                return;
            }
        }
        tok.what = what;
        tok.token = token;
        tok.pkg = pkg;
    }

    int gatherDisableActionsLocked(int userId) {
        int N = this.mDisableRecords.size();
        int net = 0;
        for (int i = 0; i < N; i++) {
            DisableRecord rec = this.mDisableRecords.get(i);
            if (rec.userId == userId) {
                net |= rec.what;
            }
        }
        return net;
    }

    protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        if (this.mContext.checkCallingOrSelfPermission("android.permission.DUMP") != 0) {
            pw.println("Permission Denial: can't dump StatusBar from from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid());
            return;
        }
        synchronized (this.mIcons) {
            this.mIcons.dump(pw);
        }
        synchronized (this.mLock) {
            pw.println("  mDisabled=0x" + Integer.toHexString(this.mDisabled));
            int N = this.mDisableRecords.size();
            pw.println("  mDisableRecords.size=" + N);
            for (int i = 0; i < N; i++) {
                DisableRecord tok = this.mDisableRecords.get(i);
                pw.println("    [" + i + "] userId=" + tok.userId + " what=0x" + Integer.toHexString(tok.what) + " pkg=" + tok.pkg + " token=" + tok.token);
            }
        }
    }
}
