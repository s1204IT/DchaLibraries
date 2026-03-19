package com.android.server.statusbar;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Rect;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.util.ArrayMap;
import android.util.Slog;
import com.android.internal.statusbar.IStatusBar;
import com.android.internal.statusbar.IStatusBarService;
import com.android.internal.statusbar.NotificationVisibility;
import com.android.internal.statusbar.StatusBarIcon;
import com.android.server.LocalServices;
import com.android.server.notification.NotificationDelegate;
import com.android.server.notification.NotificationManagerService;
import com.android.server.wm.WindowManagerService;
import com.mediatek.common.dm.DmAgent;
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
    private int mDockedStackSysUiVisibility;
    private int mFullscreenStackSysUiVisibility;
    private int mImeBackDisposition;
    private NotificationDelegate mNotificationDelegate;
    private boolean mShowImeSwitcher;
    private final WindowManagerService mWindowManager;
    private Handler mHandler = new Handler();
    private ArrayMap<String, StatusBarIcon> mIcons = new ArrayMap<>();
    private final ArrayList<DisableRecord> mDisableRecords = new ArrayList<>();
    private IBinder mSysUiVisToken = new Binder();
    private int mDisabled1 = 0;
    private int mDisabled2 = 0;
    private final Object mLock = new Object();
    private int mSystemUiVisibility = 0;
    private final Rect mFullscreenStackBounds = new Rect();
    private final Rect mDockedStackBounds = new Rect();
    private boolean mMenuVisible = false;
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
            if (StatusBarManagerService.this.mBar == null) {
                return;
            }
            try {
                StatusBarManagerService.this.mBar.buzzBeepBlinked();
            } catch (RemoteException e) {
            }
        }

        @Override
        public void notificationLightPulse(int argb, int onMillis, int offMillis) {
            this.mNotificationLightOn = true;
            if (StatusBarManagerService.this.mBar == null) {
                return;
            }
            try {
                StatusBarManagerService.this.mBar.notificationLightPulse(argb, onMillis, offMillis);
            } catch (RemoteException e) {
            }
        }

        @Override
        public void notificationLightOff() {
            if (!this.mNotificationLightOn) {
                return;
            }
            this.mNotificationLightOn = false;
            if (StatusBarManagerService.this.mBar == null) {
                return;
            }
            try {
                StatusBarManagerService.this.mBar.notificationLightOff();
            } catch (RemoteException e) {
            }
        }

        @Override
        public void showScreenPinningRequest(int taskId) {
            if (StatusBarManagerService.this.mBar == null) {
                return;
            }
            try {
                StatusBarManagerService.this.mBar.showScreenPinningRequest(taskId);
            } catch (RemoteException e) {
            }
        }

        @Override
        public void showAssistDisclosure() {
            if (StatusBarManagerService.this.mBar == null) {
                return;
            }
            try {
                StatusBarManagerService.this.mBar.showAssistDisclosure();
            } catch (RemoteException e) {
            }
        }

        @Override
        public void startAssist(Bundle args) {
            if (StatusBarManagerService.this.mBar == null) {
                return;
            }
            try {
                StatusBarManagerService.this.mBar.startAssist(args);
            } catch (RemoteException e) {
            }
        }

        @Override
        public void onCameraLaunchGestureDetected(int source) {
            if (StatusBarManagerService.this.mBar == null) {
                return;
            }
            try {
                StatusBarManagerService.this.mBar.onCameraLaunchGestureDetected(source);
            } catch (RemoteException e) {
            }
        }

        @Override
        public void topAppWindowChanged(boolean menuVisible) {
            StatusBarManagerService.this.topAppWindowChanged(menuVisible);
        }

        @Override
        public void setSystemUiVisibility(int vis, int fullscreenStackVis, int dockedStackVis, int mask, Rect fullscreenBounds, Rect dockedBounds, String cause) {
            StatusBarManagerService.this.setSystemUiVisibility(vis, fullscreenStackVis, dockedStackVis, mask, fullscreenBounds, dockedBounds, cause);
        }

        @Override
        public void toggleSplitScreen() {
            StatusBarManagerService.this.enforceStatusBarService();
            if (StatusBarManagerService.this.mBar == null) {
                return;
            }
            try {
                StatusBarManagerService.this.mBar.toggleSplitScreen();
            } catch (RemoteException e) {
            }
        }

        @Override
        public void appTransitionFinished() {
            StatusBarManagerService.this.enforceStatusBarService();
            if (StatusBarManagerService.this.mBar == null) {
                return;
            }
            try {
                StatusBarManagerService.this.mBar.appTransitionFinished();
            } catch (RemoteException e) {
            }
        }

        @Override
        public void toggleRecentApps() {
            if (StatusBarManagerService.this.mBar == null) {
                return;
            }
            try {
                StatusBarManagerService.this.mBar.toggleRecentApps();
            } catch (RemoteException e) {
            }
        }

        @Override
        public void setCurrentUser(int newUserId) {
            StatusBarManagerService.this.mCurrentUserId = newUserId;
        }

        @Override
        public void preloadRecentApps() {
            if (StatusBarManagerService.this.mBar == null) {
                return;
            }
            try {
                StatusBarManagerService.this.mBar.preloadRecentApps();
            } catch (RemoteException e) {
            }
        }

        @Override
        public void cancelPreloadRecentApps() {
            if (StatusBarManagerService.this.mBar == null) {
                return;
            }
            try {
                StatusBarManagerService.this.mBar.cancelPreloadRecentApps();
            } catch (RemoteException e) {
            }
        }

        @Override
        public void showRecentApps(boolean triggeredFromAltTab, boolean fromHome) {
            if (StatusBarManagerService.this.mBar == null) {
                return;
            }
            try {
                StatusBarManagerService.this.mBar.showRecentApps(triggeredFromAltTab, fromHome);
            } catch (RemoteException e) {
            }
        }

        @Override
        public void hideRecentApps(boolean triggeredFromAltTab, boolean triggeredFromHomeKey) {
            if (StatusBarManagerService.this.mBar == null) {
                return;
            }
            try {
                StatusBarManagerService.this.mBar.hideRecentApps(triggeredFromAltTab, triggeredFromHomeKey);
            } catch (RemoteException e) {
            }
        }

        @Override
        public void dismissKeyboardShortcutsMenu() {
            if (StatusBarManagerService.this.mBar == null) {
                return;
            }
            try {
                StatusBarManagerService.this.mBar.dismissKeyboardShortcutsMenu();
            } catch (RemoteException e) {
            }
        }

        @Override
        public void toggleKeyboardShortcutsMenu(int deviceId) {
            if (StatusBarManagerService.this.mBar == null) {
                return;
            }
            try {
                StatusBarManagerService.this.mBar.toggleKeyboardShortcutsMenu(deviceId);
            } catch (RemoteException e) {
            }
        }

        @Override
        public void showTvPictureInPictureMenu() {
            if (StatusBarManagerService.this.mBar == null) {
                return;
            }
            try {
                StatusBarManagerService.this.mBar.showTvPictureInPictureMenu();
            } catch (RemoteException e) {
            }
        }

        @Override
        public void setWindowState(int window, int state) {
            if (StatusBarManagerService.this.mBar == null) {
                return;
            }
            try {
                StatusBarManagerService.this.mBar.setWindowState(window, state);
            } catch (RemoteException e) {
            }
        }

        @Override
        public void appTransitionPending() {
            if (StatusBarManagerService.this.mBar == null) {
                return;
            }
            try {
                StatusBarManagerService.this.mBar.appTransitionPending();
            } catch (RemoteException e) {
            }
        }

        @Override
        public void appTransitionCancelled() {
            if (StatusBarManagerService.this.mBar == null) {
                return;
            }
            try {
                StatusBarManagerService.this.mBar.appTransitionCancelled();
            } catch (RemoteException e) {
            }
        }

        @Override
        public void appTransitionStarting(long statusBarAnimationsStartTime, long statusBarAnimationsDuration) {
            if (StatusBarManagerService.this.mBar == null) {
                return;
            }
            try {
                StatusBarManagerService.this.mBar.appTransitionStarting(statusBarAnimationsStartTime, statusBarAnimationsDuration);
            } catch (RemoteException e) {
            }
        }
    };
    IBinder mDMToken = new Binder();
    private final BroadcastReceiver mPPLReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(NotificationManagerService.PPL_LOCK) || action.equals(NotificationManagerService.OMADM_LAWMO_LOCK)) {
                StatusBarManagerService.this.dmEnable(false);
            } else {
                if (!action.equals(NotificationManagerService.PPL_UNLOCK) && !action.equals(NotificationManagerService.OMADM_LAWMO_UNLOCK)) {
                    return;
                }
                StatusBarManagerService.this.dmEnable(true);
            }
        }
    };

    private class DisableRecord implements IBinder.DeathRecipient {
        String pkg;
        IBinder token;
        int userId;
        int what1;
        int what2;

        DisableRecord(StatusBarManagerService this$0, DisableRecord disableRecord) {
            this();
        }

        private DisableRecord() {
        }

        @Override
        public void binderDied() {
            Slog.i(StatusBarManagerService.TAG, "binder died for pkg=" + this.pkg);
            StatusBarManagerService.this.disableForUser(0, this.token, this.pkg, this.userId);
            StatusBarManagerService.this.disable2ForUser(0, this.token, this.pkg, this.userId);
            this.token.unlinkToDeath(this, 0);
        }
    }

    public StatusBarManagerService(Context context, WindowManagerService windowManager) {
        this.mContext = context;
        this.mWindowManager = windowManager;
        LocalServices.addService(StatusBarManagerInternal.class, this.mInternalService);
        registerDMLock();
    }

    public void expandNotificationsPanel() {
        enforceExpandStatusBar();
        if (this.mBar == null) {
            return;
        }
        try {
            this.mBar.animateExpandNotificationsPanel();
        } catch (RemoteException e) {
        }
    }

    public void collapsePanels() {
        enforceExpandStatusBar();
        if (this.mBar == null) {
            return;
        }
        try {
            this.mBar.animateCollapsePanels();
        } catch (RemoteException e) {
        }
    }

    public void expandSettingsPanel(String subPanel) {
        enforceExpandStatusBar();
        if (this.mBar == null) {
            return;
        }
        try {
            this.mBar.animateExpandSettingsPanel(subPanel);
        } catch (RemoteException e) {
        }
    }

    public void addTile(ComponentName component) {
        enforceStatusBarOrShell();
        if (this.mBar == null) {
            return;
        }
        try {
            this.mBar.addQsTile(component);
        } catch (RemoteException e) {
        }
    }

    public void remTile(ComponentName component) {
        enforceStatusBarOrShell();
        if (this.mBar == null) {
            return;
        }
        try {
            this.mBar.remQsTile(component);
        } catch (RemoteException e) {
        }
    }

    public void clickTile(ComponentName component) {
        enforceStatusBarOrShell();
        if (this.mBar == null) {
            return;
        }
        try {
            this.mBar.clickQsTile(component);
        } catch (RemoteException e) {
        }
    }

    public void disable(int what, IBinder token, String pkg) {
        disableForUser(what, token, pkg, this.mCurrentUserId);
    }

    public void disableForUser(int what, IBinder token, String pkg, int userId) {
        enforceStatusBar();
        synchronized (this.mLock) {
            disableLocked(userId, what, token, pkg, 1);
        }
    }

    public void disable2(int what, IBinder token, String pkg) {
        disable2ForUser(what, token, pkg, this.mCurrentUserId);
    }

    public void disable2ForUser(int what, IBinder token, String pkg, int userId) {
        enforceStatusBar();
        synchronized (this.mLock) {
            disableLocked(userId, what, token, pkg, 2);
        }
    }

    private void disableLocked(int userId, int what, IBinder token, String pkg, int whichFlag) {
        manageDisableListLocked(userId, what, token, pkg, whichFlag);
        final int net1 = gatherDisableActionsLocked(this.mCurrentUserId, 1);
        int net2 = gatherDisableActionsLocked(this.mCurrentUserId, 2);
        if (net1 == this.mDisabled1 && net2 == this.mDisabled2) {
            return;
        }
        this.mDisabled1 = net1;
        this.mDisabled2 = net2;
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                StatusBarManagerService.this.mNotificationDelegate.onSetDisabled(net1);
            }
        });
        if (this.mBar == null) {
            return;
        }
        try {
            Slog.d(TAG, "disable statusbar calling PID = " + Binder.getCallingPid());
            this.mBar.disable(net1, net2);
        } catch (RemoteException e) {
        }
    }

    public void setIcon(String slot, String iconPackage, int iconId, int iconLevel, String contentDescription) {
        enforceStatusBar();
        synchronized (this.mIcons) {
            StatusBarIcon icon = new StatusBarIcon(iconPackage, UserHandle.SYSTEM, iconId, iconLevel, 0, contentDescription);
            this.mIcons.put(slot, icon);
            if (this.mBar != null) {
                try {
                    this.mBar.setIcon(slot, icon);
                } catch (RemoteException e) {
                }
            }
        }
    }

    public void setIconVisibility(String slot, boolean visibility) {
        enforceStatusBar();
        synchronized (this.mIcons) {
            StatusBarIcon icon = this.mIcons.get(slot);
            if (icon == null) {
                return;
            }
            if (icon.visible != visibility) {
                icon.visible = visibility;
                if (this.mBar != null) {
                    try {
                        this.mBar.setIcon(slot, icon);
                    } catch (RemoteException e) {
                    }
                }
            }
        }
    }

    public void removeIcon(String slot) {
        enforceStatusBar();
        synchronized (this.mIcons) {
            this.mIcons.remove(slot);
            if (this.mBar != null) {
                try {
                    this.mBar.removeIcon(slot);
                } catch (RemoteException e) {
                }
            }
        }
    }

    private void topAppWindowChanged(final boolean menuVisible) {
        enforceStatusBar();
        synchronized (this.mLock) {
            this.mMenuVisible = menuVisible;
            this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (StatusBarManagerService.this.mBar == null) {
                        return;
                    }
                    try {
                        StatusBarManagerService.this.mBar.topAppWindowChanged(menuVisible);
                    } catch (RemoteException e) {
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
                    if (StatusBarManagerService.this.mBar == null) {
                        return;
                    }
                    try {
                        StatusBarManagerService.this.mBar.setImeWindowStatus(token, vis, backDisposition, showImeSwitcher);
                    } catch (RemoteException e) {
                    }
                }
            });
        }
    }

    public void setSystemUiVisibility(int vis, int mask, String cause) {
        setSystemUiVisibility(vis, 0, 0, mask, this.mFullscreenStackBounds, this.mDockedStackBounds, cause);
    }

    private void setSystemUiVisibility(int vis, int fullscreenStackVis, int dockedStackVis, int mask, Rect fullscreenBounds, Rect dockedBounds, String cause) {
        enforceStatusBarService();
        synchronized (this.mLock) {
            updateUiVisibilityLocked(vis, fullscreenStackVis, dockedStackVis, mask, fullscreenBounds, dockedBounds);
            disableLocked(this.mCurrentUserId, vis & 67043328, this.mSysUiVisToken, cause, 1);
        }
    }

    private void updateUiVisibilityLocked(final int vis, final int fullscreenStackVis, final int dockedStackVis, final int mask, final Rect fullscreenBounds, final Rect dockedBounds) {
        if (this.mSystemUiVisibility == vis && this.mFullscreenStackSysUiVisibility == fullscreenStackVis && this.mDockedStackSysUiVisibility == dockedStackVis && this.mFullscreenStackBounds.equals(fullscreenBounds) && this.mDockedStackBounds.equals(dockedBounds)) {
            return;
        }
        this.mSystemUiVisibility = vis;
        this.mFullscreenStackSysUiVisibility = fullscreenStackVis;
        this.mDockedStackSysUiVisibility = dockedStackVis;
        this.mFullscreenStackBounds.set(fullscreenBounds);
        this.mDockedStackBounds.set(dockedBounds);
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (StatusBarManagerService.this.mBar == null) {
                    return;
                }
                try {
                    StatusBarManagerService.this.mBar.setSystemUiVisibility(vis, fullscreenStackVis, dockedStackVis, mask, fullscreenBounds, dockedBounds);
                } catch (RemoteException e) {
                }
            }
        });
    }

    private void enforceStatusBarOrShell() {
        if (Binder.getCallingUid() == 2000) {
            return;
        }
        enforceStatusBar();
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

    public void registerStatusBar(IStatusBar bar, List<String> iconSlots, List<StatusBarIcon> iconList, int[] switches, List<IBinder> binders, Rect fullscreenStackBounds, Rect dockedStackBounds) {
        enforceStatusBarService();
        Slog.i(TAG, "registerStatusBar bar=" + bar);
        this.mBar = bar;
        synchronized (this.mIcons) {
            for (String slot : this.mIcons.keySet()) {
                iconSlots.add(slot);
                iconList.add(this.mIcons.get(slot));
            }
        }
        synchronized (this.mLock) {
            switches[0] = gatherDisableActionsLocked(this.mCurrentUserId, 1);
            switches[1] = this.mSystemUiVisibility;
            switches[2] = this.mMenuVisible ? 1 : 0;
            switches[3] = this.mImeWindowVis;
            switches[4] = this.mImeBackDisposition;
            switches[5] = this.mShowImeSwitcher ? 1 : 0;
            switches[6] = gatherDisableActionsLocked(this.mCurrentUserId, 2);
            switches[7] = this.mFullscreenStackSysUiVisibility;
            switches[8] = this.mDockedStackSysUiVisibility;
            binders.add(this.mImeToken);
            fullscreenStackBounds.set(this.mFullscreenStackBounds);
            dockedStackBounds.set(this.mDockedStackBounds);
        }
    }

    public void onPanelRevealed(boolean clearNotificationEffects, int numItems) {
        enforceStatusBarService();
        long identity = Binder.clearCallingIdentity();
        try {
            this.mNotificationDelegate.onPanelRevealed(clearNotificationEffects, numItems);
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

    public void onNotificationVisibilityChanged(NotificationVisibility[] newlyVisibleKeys, NotificationVisibility[] noLongerVisibleKeys) throws RemoteException {
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

    public void onShellCommand(FileDescriptor in, FileDescriptor out, FileDescriptor err, String[] args, ResultReceiver resultReceiver) throws RemoteException {
        new StatusBarShellCommand(this).exec(this, in, out, err, args, resultReceiver);
    }

    void manageDisableListLocked(int userId, int what, IBinder token, String pkg, int which) {
        DisableRecord disableRecord = null;
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
            if (tok == null) {
                return;
            }
            this.mDisableRecords.remove(i);
            tok.token.unlinkToDeath(tok, 0);
            return;
        }
        if (tok == null) {
            tok = new DisableRecord(this, disableRecord);
            tok.userId = userId;
            try {
                token.linkToDeath(tok, 0);
                this.mDisableRecords.add(tok);
            } catch (RemoteException e) {
                return;
            }
        }
        if (which == 1) {
            tok.what1 = what;
        } else {
            tok.what2 = what;
        }
        tok.token = token;
        tok.pkg = pkg;
    }

    int gatherDisableActionsLocked(int userId, int which) {
        int N = this.mDisableRecords.size();
        int net = 0;
        for (int i = 0; i < N; i++) {
            DisableRecord rec = this.mDisableRecords.get(i);
            if (rec.userId == userId) {
                net |= which == 1 ? rec.what1 : rec.what2;
            }
        }
        return net;
    }

    protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        if (this.mContext.checkCallingOrSelfPermission("android.permission.DUMP") != 0) {
            pw.println("Permission Denial: can't dump StatusBar from from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid());
            return;
        }
        synchronized (this.mLock) {
            pw.println("  mDisabled1=0x" + Integer.toHexString(this.mDisabled1));
            pw.println("  mDisabled2=0x" + Integer.toHexString(this.mDisabled2));
            int N = this.mDisableRecords.size();
            pw.println("  mDisableRecords.size=" + N);
            for (int i = 0; i < N; i++) {
                DisableRecord tok = this.mDisableRecords.get(i);
                pw.println("    [" + i + "] userId=" + tok.userId + " what1=0x" + Integer.toHexString(tok.what1) + " what2=0x" + Integer.toHexString(tok.what2) + " pkg=" + tok.pkg + " token=" + tok.token);
            }
            pw.println("  mCurrentUserId=" + this.mCurrentUserId);
        }
    }

    private void registerDMLock() {
        try {
            IBinder binder = ServiceManager.getService("DmAgent");
            if (binder != null) {
                DmAgent agent = DmAgent.Stub.asInterface(binder);
                boolean locked = agent.isLockFlagSet();
                dmEnable(!locked);
            }
        } catch (RemoteException e) {
        }
        Slog.i(TAG, "registerDMLock");
        IntentFilter filter = new IntentFilter();
        filter.addAction(NotificationManagerService.PPL_LOCK);
        filter.addAction(NotificationManagerService.PPL_UNLOCK);
        filter.addAction(NotificationManagerService.OMADM_LAWMO_LOCK);
        filter.addAction(NotificationManagerService.OMADM_LAWMO_UNLOCK);
        this.mContext.registerReceiver(this.mPPLReceiver, filter);
    }

    private int dmEnable(boolean enable) {
        Slog.i(TAG, " enable state is " + enable);
        int net = 0;
        if (!enable) {
            net = 983040;
        }
        disable(net, this.mDMToken, this.mContext.getPackageName());
        return 0;
    }
}
