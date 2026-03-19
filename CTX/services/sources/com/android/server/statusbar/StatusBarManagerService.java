package com.android.server.statusbar;

import android.R;
import android.app.ActivityThread;
import android.content.ComponentName;
import android.content.Context;
import android.graphics.Rect;
import android.hardware.biometrics.IBiometricPromptReceiver;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.ShellCallback;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Slog;
import com.android.internal.statusbar.IStatusBar;
import com.android.internal.statusbar.IStatusBarService;
import com.android.internal.statusbar.NotificationVisibility;
import com.android.internal.statusbar.StatusBarIcon;
import com.android.internal.util.DumpUtils;
import com.android.server.LocalServices;
import com.android.server.notification.NotificationDelegate;
import com.android.server.policy.GlobalActionsProvider;
import com.android.server.power.ShutdownThread;
import com.android.server.wm.WindowManagerService;
import com.mediatek.server.MtkSystemServiceFactory;
import com.mediatek.server.ppl.MtkPplManager;
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
    private GlobalActionsProvider.GlobalActionsListener mGlobalActionListener;
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
    public MtkPplManager mMtkPplManager = MtkSystemServiceFactory.getInstance().makeMtkPplManager();
    private final StatusBarManagerInternal mInternalService = new StatusBarManagerInternal() {
        private boolean mNotificationLightOn;

        @Override
        public void setNotificationDelegate(NotificationDelegate notificationDelegate) {
            StatusBarManagerService.this.mNotificationDelegate = notificationDelegate;
        }

        @Override
        public void showScreenPinningRequest(int i) {
            if (StatusBarManagerService.this.mBar != null) {
                try {
                    StatusBarManagerService.this.mBar.showScreenPinningRequest(i);
                } catch (RemoteException e) {
                }
            }
        }

        @Override
        public void showAssistDisclosure() {
            if (StatusBarManagerService.this.mBar != null) {
                try {
                    StatusBarManagerService.this.mBar.showAssistDisclosure();
                } catch (RemoteException e) {
                }
            }
        }

        @Override
        public void startAssist(Bundle bundle) {
            if (StatusBarManagerService.this.mBar != null) {
                try {
                    StatusBarManagerService.this.mBar.startAssist(bundle);
                } catch (RemoteException e) {
                }
            }
        }

        @Override
        public void onCameraLaunchGestureDetected(int i) {
            if (StatusBarManagerService.this.mBar != null) {
                try {
                    StatusBarManagerService.this.mBar.onCameraLaunchGestureDetected(i);
                } catch (RemoteException e) {
                }
            }
        }

        @Override
        public void topAppWindowChanged(boolean z) {
            StatusBarManagerService.this.topAppWindowChanged(z);
        }

        @Override
        public void setSystemUiVisibility(int i, int i2, int i3, int i4, Rect rect, Rect rect2, String str) {
            StatusBarManagerService.this.setSystemUiVisibility(i, i2, i3, i4, rect, rect2, str);
        }

        @Override
        public void toggleSplitScreen() {
            StatusBarManagerService.this.enforceStatusBarService();
            if (StatusBarManagerService.this.mBar != null) {
                try {
                    StatusBarManagerService.this.mBar.toggleSplitScreen();
                } catch (RemoteException e) {
                }
            }
        }

        @Override
        public void appTransitionFinished() {
            StatusBarManagerService.this.enforceStatusBarService();
            if (StatusBarManagerService.this.mBar != null) {
                try {
                    StatusBarManagerService.this.mBar.appTransitionFinished();
                } catch (RemoteException e) {
                }
            }
        }

        @Override
        public void toggleRecentApps() {
            if (StatusBarManagerService.this.mBar != null) {
                try {
                    StatusBarManagerService.this.mBar.toggleRecentApps();
                } catch (RemoteException e) {
                }
            }
        }

        @Override
        public void setCurrentUser(int i) {
            StatusBarManagerService.this.mCurrentUserId = i;
        }

        @Override
        public void preloadRecentApps() {
            if (StatusBarManagerService.this.mBar != null) {
                try {
                    StatusBarManagerService.this.mBar.preloadRecentApps();
                } catch (RemoteException e) {
                }
            }
        }

        @Override
        public void cancelPreloadRecentApps() {
            if (StatusBarManagerService.this.mBar != null) {
                try {
                    StatusBarManagerService.this.mBar.cancelPreloadRecentApps();
                } catch (RemoteException e) {
                }
            }
        }

        @Override
        public void showRecentApps(boolean z) {
            if (StatusBarManagerService.this.mBar != null) {
                try {
                    StatusBarManagerService.this.mBar.showRecentApps(z);
                } catch (RemoteException e) {
                }
            }
        }

        @Override
        public void hideRecentApps(boolean z, boolean z2) {
            if (StatusBarManagerService.this.mBar != null) {
                try {
                    StatusBarManagerService.this.mBar.hideRecentApps(z, z2);
                } catch (RemoteException e) {
                }
            }
        }

        @Override
        public void dismissKeyboardShortcutsMenu() {
            if (StatusBarManagerService.this.mBar != null) {
                try {
                    StatusBarManagerService.this.mBar.dismissKeyboardShortcutsMenu();
                } catch (RemoteException e) {
                }
            }
        }

        @Override
        public void toggleKeyboardShortcutsMenu(int i) {
            if (StatusBarManagerService.this.mBar != null) {
                try {
                    StatusBarManagerService.this.mBar.toggleKeyboardShortcutsMenu(i);
                } catch (RemoteException e) {
                }
            }
        }

        @Override
        public void showChargingAnimation(int i) {
            if (StatusBarManagerService.this.mBar != null) {
                try {
                    StatusBarManagerService.this.mBar.showWirelessChargingAnimation(i);
                } catch (RemoteException e) {
                }
            }
        }

        @Override
        public void showPictureInPictureMenu() {
            if (StatusBarManagerService.this.mBar != null) {
                try {
                    StatusBarManagerService.this.mBar.showPictureInPictureMenu();
                } catch (RemoteException e) {
                }
            }
        }

        @Override
        public void setWindowState(int i, int i2) {
            if (StatusBarManagerService.this.mBar != null) {
                try {
                    StatusBarManagerService.this.mBar.setWindowState(i, i2);
                } catch (RemoteException e) {
                }
            }
        }

        @Override
        public void appTransitionPending() {
            if (StatusBarManagerService.this.mBar != null) {
                try {
                    StatusBarManagerService.this.mBar.appTransitionPending();
                } catch (RemoteException e) {
                }
            }
        }

        @Override
        public void appTransitionCancelled() {
            if (StatusBarManagerService.this.mBar != null) {
                try {
                    StatusBarManagerService.this.mBar.appTransitionCancelled();
                } catch (RemoteException e) {
                }
            }
        }

        @Override
        public void appTransitionStarting(long j, long j2) {
            if (StatusBarManagerService.this.mBar != null) {
                try {
                    StatusBarManagerService.this.mBar.appTransitionStarting(j, j2);
                } catch (RemoteException e) {
                }
            }
        }

        @Override
        public void setTopAppHidesStatusBar(boolean z) {
            if (StatusBarManagerService.this.mBar != null) {
                try {
                    StatusBarManagerService.this.mBar.setTopAppHidesStatusBar(z);
                } catch (RemoteException e) {
                }
            }
        }

        @Override
        public boolean showShutdownUi(boolean z, String str) {
            if (StatusBarManagerService.this.mContext.getResources().getBoolean(R.^attr-private.mtpReserve) && StatusBarManagerService.this.mBar != null) {
                try {
                    StatusBarManagerService.this.mBar.showShutdownUi(z, str);
                    return true;
                } catch (RemoteException e) {
                }
            }
            return false;
        }

        @Override
        public void onProposedRotationChanged(int i, boolean z) {
            if (StatusBarManagerService.this.mBar != null) {
                try {
                    StatusBarManagerService.this.mBar.onProposedRotationChanged(i, z);
                } catch (RemoteException e) {
                }
            }
        }
    };
    private final GlobalActionsProvider mGlobalActionsProvider = new GlobalActionsProvider() {
        @Override
        public boolean isGlobalActionsDisabled() {
            return (StatusBarManagerService.this.mDisabled2 & 8) != 0;
        }

        @Override
        public void setGlobalActionsListener(GlobalActionsProvider.GlobalActionsListener globalActionsListener) {
            StatusBarManagerService.this.mGlobalActionListener = globalActionsListener;
            StatusBarManagerService.this.mGlobalActionListener.onGlobalActionsAvailableChanged(StatusBarManagerService.this.mBar != null);
        }

        @Override
        public void showGlobalActions() {
            if (StatusBarManagerService.this.mBar != null) {
                try {
                    StatusBarManagerService.this.mBar.showGlobalActionsMenu();
                } catch (RemoteException e) {
                }
            }
        }
    };

    private class DisableRecord implements IBinder.DeathRecipient {
        String pkg;
        IBinder token;
        int userId;
        int what1;
        int what2;

        public DisableRecord(int i, IBinder iBinder) {
            this.userId = i;
            this.token = iBinder;
            try {
                iBinder.linkToDeath(this, 0);
            } catch (RemoteException e) {
            }
        }

        @Override
        public void binderDied() {
            Slog.i(StatusBarManagerService.TAG, "binder died for pkg=" + this.pkg);
            StatusBarManagerService.this.disableForUser(0, this.token, this.pkg, this.userId);
            StatusBarManagerService.this.disable2ForUser(0, this.token, this.pkg, this.userId);
            this.token.unlinkToDeath(this, 0);
        }

        public void setFlags(int i, int i2, String str) {
            switch (i2) {
                case 1:
                    this.what1 = i;
                    break;
                case 2:
                    this.what2 = i;
                    break;
                default:
                    Slog.w(StatusBarManagerService.TAG, "Can't set unsupported disable flag " + i2 + ": 0x" + Integer.toHexString(i));
                    this.pkg = str;
                    break;
            }
        }

        public int getFlags(int i) {
            switch (i) {
                case 1:
                    return this.what1;
                case 2:
                    return this.what2;
                default:
                    Slog.w(StatusBarManagerService.TAG, "Can't get unsupported disable flag " + i);
                    return 0;
            }
        }

        public boolean isEmpty() {
            return this.what1 == 0 && this.what2 == 0;
        }

        public String toString() {
            return String.format("userId=%d what1=0x%08X what2=0x%08X pkg=%s token=%s", Integer.valueOf(this.userId), Integer.valueOf(this.what1), Integer.valueOf(this.what2), this.pkg, this.token);
        }
    }

    public StatusBarManagerService(Context context, WindowManagerService windowManagerService) {
        this.mContext = context;
        this.mWindowManager = windowManagerService;
        LocalServices.addService(StatusBarManagerInternal.class, this.mInternalService);
        LocalServices.addService(GlobalActionsProvider.class, this.mGlobalActionsProvider);
        this.mMtkPplManager.registerPplReceiver(this.mContext);
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

    public void togglePanel() {
        enforceExpandStatusBar();
        if (this.mBar != null) {
            try {
                this.mBar.togglePanel();
            } catch (RemoteException e) {
            }
        }
    }

    public void expandSettingsPanel(String str) {
        enforceExpandStatusBar();
        if (this.mBar != null) {
            try {
                this.mBar.animateExpandSettingsPanel(str);
            } catch (RemoteException e) {
            }
        }
    }

    public void addTile(ComponentName componentName) {
        enforceStatusBarOrShell();
        if (this.mBar != null) {
            try {
                this.mBar.addQsTile(componentName);
            } catch (RemoteException e) {
            }
        }
    }

    public void remTile(ComponentName componentName) {
        enforceStatusBarOrShell();
        if (this.mBar != null) {
            try {
                this.mBar.remQsTile(componentName);
            } catch (RemoteException e) {
            }
        }
    }

    public void clickTile(ComponentName componentName) {
        enforceStatusBarOrShell();
        if (this.mBar != null) {
            try {
                this.mBar.clickQsTile(componentName);
            } catch (RemoteException e) {
            }
        }
    }

    public void handleSystemKey(int i) throws RemoteException {
        enforceExpandStatusBar();
        if (this.mBar != null) {
            try {
                this.mBar.handleSystemKey(i);
            } catch (RemoteException e) {
            }
        }
    }

    public void showPinningEnterExitToast(boolean z) throws RemoteException {
        if (this.mBar != null) {
            try {
                this.mBar.showPinningEnterExitToast(z);
            } catch (RemoteException e) {
            }
        }
    }

    public void showPinningEscapeToast() throws RemoteException {
        if (this.mBar != null) {
            try {
                this.mBar.showPinningEscapeToast();
            } catch (RemoteException e) {
            }
        }
    }

    public void showFingerprintDialog(Bundle bundle, IBiometricPromptReceiver iBiometricPromptReceiver) {
        if (this.mBar != null) {
            try {
                this.mBar.showFingerprintDialog(bundle, iBiometricPromptReceiver);
            } catch (RemoteException e) {
            }
        }
    }

    public void onFingerprintAuthenticated() {
        if (this.mBar != null) {
            try {
                this.mBar.onFingerprintAuthenticated();
            } catch (RemoteException e) {
            }
        }
    }

    public void onFingerprintHelp(String str) {
        if (this.mBar != null) {
            try {
                this.mBar.onFingerprintHelp(str);
            } catch (RemoteException e) {
            }
        }
    }

    public void onFingerprintError(String str) {
        if (this.mBar != null) {
            try {
                this.mBar.onFingerprintError(str);
            } catch (RemoteException e) {
            }
        }
    }

    public void hideFingerprintDialog() {
        if (this.mBar != null) {
            try {
                this.mBar.hideFingerprintDialog();
            } catch (RemoteException e) {
            }
        }
    }

    public void disable(int i, IBinder iBinder, String str) {
        disableForUser(i, iBinder, str, this.mCurrentUserId);
    }

    public void disableForUser(int i, IBinder iBinder, String str, int i2) {
        enforceStatusBar();
        synchronized (this.mLock) {
            disableLocked(i2, i, iBinder, str, 1);
        }
    }

    public void disable2(int i, IBinder iBinder, String str) {
        disable2ForUser(i, iBinder, str, this.mCurrentUserId);
    }

    public void disable2ForUser(int i, IBinder iBinder, String str, int i2) {
        enforceStatusBar();
        synchronized (this.mLock) {
            disableLocked(i2, i, iBinder, str, 2);
        }
    }

    private void disableLocked(int i, int i2, IBinder iBinder, String str, int i3) {
        manageDisableListLocked(i, i2, iBinder, str, i3);
        final int iGatherDisableActionsLocked = gatherDisableActionsLocked(this.mCurrentUserId, 1);
        int iGatherDisableActionsLocked2 = gatherDisableActionsLocked(this.mCurrentUserId, 2);
        if (iGatherDisableActionsLocked != this.mDisabled1 || iGatherDisableActionsLocked2 != this.mDisabled2) {
            this.mDisabled1 = iGatherDisableActionsLocked;
            this.mDisabled2 = iGatherDisableActionsLocked2;
            this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    StatusBarManagerService.this.mNotificationDelegate.onSetDisabled(iGatherDisableActionsLocked);
                }
            });
            if (this.mBar != null) {
                try {
                    this.mBar.disable(iGatherDisableActionsLocked, iGatherDisableActionsLocked2);
                } catch (RemoteException e) {
                }
            }
        }
    }

    public void setIcon(String str, String str2, int i, int i2, String str3) {
        enforceStatusBar();
        synchronized (this.mIcons) {
            StatusBarIcon statusBarIcon = new StatusBarIcon(str2, UserHandle.SYSTEM, i, i2, 0, str3);
            this.mIcons.put(str, statusBarIcon);
            if (this.mBar != null) {
                try {
                    this.mBar.setIcon(str, statusBarIcon);
                } catch (RemoteException e) {
                }
            }
        }
    }

    public void setIconVisibility(String str, boolean z) {
        enforceStatusBar();
        synchronized (this.mIcons) {
            StatusBarIcon statusBarIcon = this.mIcons.get(str);
            if (statusBarIcon == null) {
                return;
            }
            if (statusBarIcon.visible != z) {
                statusBarIcon.visible = z;
                if (this.mBar != null) {
                    try {
                        this.mBar.setIcon(str, statusBarIcon);
                    } catch (RemoteException e) {
                    }
                }
            }
        }
    }

    public void removeIcon(String str) {
        enforceStatusBar();
        synchronized (this.mIcons) {
            this.mIcons.remove(str);
            if (this.mBar != null) {
                try {
                    this.mBar.removeIcon(str);
                } catch (RemoteException e) {
                }
            }
        }
    }

    private void topAppWindowChanged(final boolean z) {
        enforceStatusBar();
        synchronized (this.mLock) {
            this.mMenuVisible = z;
            this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (StatusBarManagerService.this.mBar != null) {
                        try {
                            StatusBarManagerService.this.mBar.topAppWindowChanged(z);
                        } catch (RemoteException e) {
                        }
                    }
                }
            });
        }
    }

    public void setImeWindowStatus(final IBinder iBinder, final int i, final int i2, final boolean z) {
        enforceStatusBar();
        synchronized (this.mLock) {
            this.mImeWindowVis = i;
            this.mImeBackDisposition = i2;
            this.mImeToken = iBinder;
            this.mShowImeSwitcher = z;
            this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (StatusBarManagerService.this.mBar != null) {
                        try {
                            StatusBarManagerService.this.mBar.setImeWindowStatus(iBinder, i, i2, z);
                        } catch (RemoteException e) {
                        }
                    }
                }
            });
        }
    }

    public void setSystemUiVisibility(int i, int i2, String str) {
        setSystemUiVisibility(i, 0, 0, i2, this.mFullscreenStackBounds, this.mDockedStackBounds, str);
    }

    private void setSystemUiVisibility(int i, int i2, int i3, int i4, Rect rect, Rect rect2, String str) {
        enforceStatusBarService();
        synchronized (this.mLock) {
            updateUiVisibilityLocked(i, i2, i3, i4, rect, rect2);
            disableLocked(this.mCurrentUserId, 67043328 & i, this.mSysUiVisToken, str, 1);
        }
    }

    private void updateUiVisibilityLocked(final int i, final int i2, final int i3, final int i4, final Rect rect, final Rect rect2) {
        if (this.mSystemUiVisibility != i || this.mFullscreenStackSysUiVisibility != i2 || this.mDockedStackSysUiVisibility != i3 || !this.mFullscreenStackBounds.equals(rect) || !this.mDockedStackBounds.equals(rect2)) {
            this.mSystemUiVisibility = i;
            this.mFullscreenStackSysUiVisibility = i2;
            this.mDockedStackSysUiVisibility = i3;
            this.mFullscreenStackBounds.set(rect);
            this.mDockedStackBounds.set(rect2);
            this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (StatusBarManagerService.this.mBar != null) {
                        try {
                            StatusBarManagerService.this.mBar.setSystemUiVisibility(i, i2, i3, i4, rect, rect2);
                        } catch (RemoteException e) {
                        }
                    }
                }
            });
        }
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

    public void registerStatusBar(IStatusBar iStatusBar, List<String> list, List<StatusBarIcon> list2, int[] iArr, List<IBinder> list3, Rect rect, Rect rect2) {
        enforceStatusBarService();
        Slog.i(TAG, "registerStatusBar bar=" + iStatusBar);
        this.mBar = iStatusBar;
        try {
            this.mBar.asBinder().linkToDeath(new IBinder.DeathRecipient() {
                @Override
                public void binderDied() {
                    StatusBarManagerService.this.mBar = null;
                    StatusBarManagerService.this.notifyBarAttachChanged();
                }
            }, 0);
        } catch (RemoteException e) {
        }
        notifyBarAttachChanged();
        synchronized (this.mIcons) {
            for (String str : this.mIcons.keySet()) {
                list.add(str);
                list2.add(this.mIcons.get(str));
            }
        }
        synchronized (this.mLock) {
            iArr[0] = gatherDisableActionsLocked(this.mCurrentUserId, 1);
            iArr[1] = this.mSystemUiVisibility;
            iArr[2] = this.mMenuVisible ? 1 : 0;
            iArr[3] = this.mImeWindowVis;
            iArr[4] = this.mImeBackDisposition;
            iArr[5] = this.mShowImeSwitcher ? 1 : 0;
            iArr[6] = gatherDisableActionsLocked(this.mCurrentUserId, 2);
            iArr[7] = this.mFullscreenStackSysUiVisibility;
            iArr[8] = this.mDockedStackSysUiVisibility;
            list3.add(this.mImeToken);
            rect.set(this.mFullscreenStackBounds);
            rect2.set(this.mDockedStackBounds);
        }
    }

    private void notifyBarAttachChanged() {
        this.mHandler.post(new Runnable() {
            @Override
            public final void run() {
                StatusBarManagerService.lambda$notifyBarAttachChanged$0(this.f$0);
            }
        });
    }

    public static void lambda$notifyBarAttachChanged$0(StatusBarManagerService statusBarManagerService) {
        if (statusBarManagerService.mGlobalActionListener == null) {
            return;
        }
        statusBarManagerService.mGlobalActionListener.onGlobalActionsAvailableChanged(statusBarManagerService.mBar != null);
    }

    public void onPanelRevealed(boolean z, int i) {
        enforceStatusBarService();
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            this.mNotificationDelegate.onPanelRevealed(z, i);
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public void clearNotificationEffects() throws RemoteException {
        enforceStatusBarService();
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            this.mNotificationDelegate.clearEffects();
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public void onPanelHidden() throws RemoteException {
        enforceStatusBarService();
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            this.mNotificationDelegate.onPanelHidden();
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public void shutdown() {
        enforceStatusBarService();
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            this.mHandler.post(new Runnable() {
                @Override
                public final void run() {
                    ShutdownThread.shutdown(StatusBarManagerService.getUiContext(), "userrequested", false);
                }
            });
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public void reboot(final boolean z) {
        enforceStatusBarService();
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            this.mHandler.post(new Runnable() {
                @Override
                public final void run() {
                    StatusBarManagerService.lambda$reboot$2(z);
                }
            });
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    static void lambda$reboot$2(boolean z) {
        if (z) {
            ShutdownThread.rebootSafeMode(getUiContext(), true);
        } else {
            ShutdownThread.reboot(getUiContext(), "userrequested", false);
        }
    }

    public void onGlobalActionsShown() {
        enforceStatusBarService();
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            if (this.mGlobalActionListener == null) {
                return;
            }
            this.mGlobalActionListener.onGlobalActionsShown();
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public void onGlobalActionsHidden() {
        enforceStatusBarService();
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            if (this.mGlobalActionListener == null) {
                return;
            }
            this.mGlobalActionListener.onGlobalActionsDismissed();
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public void onNotificationClick(String str, NotificationVisibility notificationVisibility) {
        enforceStatusBarService();
        int callingUid = Binder.getCallingUid();
        int callingPid = Binder.getCallingPid();
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            this.mNotificationDelegate.onNotificationClick(callingUid, callingPid, str, notificationVisibility);
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public void onNotificationActionClick(String str, int i, NotificationVisibility notificationVisibility) {
        enforceStatusBarService();
        int callingUid = Binder.getCallingUid();
        int callingPid = Binder.getCallingPid();
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            this.mNotificationDelegate.onNotificationActionClick(callingUid, callingPid, str, i, notificationVisibility);
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public void onNotificationError(String str, String str2, int i, int i2, int i3, String str3, int i4) {
        enforceStatusBarService();
        int callingUid = Binder.getCallingUid();
        int callingPid = Binder.getCallingPid();
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            this.mNotificationDelegate.onNotificationError(callingUid, callingPid, str, str2, i, i2, i3, str3, i4);
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public void onNotificationClear(String str, String str2, int i, int i2, String str3, int i3, NotificationVisibility notificationVisibility) {
        enforceStatusBarService();
        int callingUid = Binder.getCallingUid();
        int callingPid = Binder.getCallingPid();
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            this.mNotificationDelegate.onNotificationClear(callingUid, callingPid, str, str2, i, i2, str3, i3, notificationVisibility);
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public void onNotificationVisibilityChanged(NotificationVisibility[] notificationVisibilityArr, NotificationVisibility[] notificationVisibilityArr2) throws RemoteException {
        enforceStatusBarService();
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            this.mNotificationDelegate.onNotificationVisibilityChanged(notificationVisibilityArr, notificationVisibilityArr2);
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public void onNotificationExpansionChanged(String str, boolean z, boolean z2) throws RemoteException {
        enforceStatusBarService();
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            this.mNotificationDelegate.onNotificationExpansionChanged(str, z, z2);
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public void onNotificationDirectReplied(String str) throws RemoteException {
        enforceStatusBarService();
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            this.mNotificationDelegate.onNotificationDirectReplied(str);
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public void onNotificationSmartRepliesAdded(String str, int i) throws RemoteException {
        enforceStatusBarService();
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            this.mNotificationDelegate.onNotificationSmartRepliesAdded(str, i);
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public void onNotificationSmartReplySent(String str, int i) throws RemoteException {
        enforceStatusBarService();
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            this.mNotificationDelegate.onNotificationSmartReplySent(str, i);
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public void onNotificationSettingsViewed(String str) throws RemoteException {
        enforceStatusBarService();
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            this.mNotificationDelegate.onNotificationSettingsViewed(str);
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public void onClearAllNotifications(int i) {
        enforceStatusBarService();
        int callingUid = Binder.getCallingUid();
        int callingPid = Binder.getCallingPid();
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            this.mNotificationDelegate.onClearAll(callingUid, callingPid, i);
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public void onShellCommand(FileDescriptor fileDescriptor, FileDescriptor fileDescriptor2, FileDescriptor fileDescriptor3, String[] strArr, ShellCallback shellCallback, ResultReceiver resultReceiver) {
        new StatusBarShellCommand(this).exec(this, fileDescriptor, fileDescriptor2, fileDescriptor3, strArr, shellCallback, resultReceiver);
    }

    public String[] getStatusBarIcons() {
        return this.mContext.getResources().getStringArray(R.array.config_deviceSpecificSystemServices);
    }

    void manageDisableListLocked(int i, int i2, IBinder iBinder, String str, int i3) {
        DisableRecord disableRecord;
        int size = this.mDisableRecords.size();
        int i4 = 0;
        while (true) {
            if (i4 < size) {
                disableRecord = this.mDisableRecords.get(i4);
                if (disableRecord.token == iBinder && disableRecord.userId == i) {
                    break;
                } else {
                    i4++;
                }
            } else {
                disableRecord = null;
                break;
            }
        }
        if (!iBinder.isBinderAlive()) {
            if (disableRecord != null) {
                this.mDisableRecords.remove(i4);
                disableRecord.token.unlinkToDeath(disableRecord, 0);
                return;
            }
            return;
        }
        if (disableRecord != null) {
            disableRecord.setFlags(i2, i3, str);
            if (disableRecord.isEmpty()) {
                this.mDisableRecords.remove(i4);
                disableRecord.token.unlinkToDeath(disableRecord, 0);
                return;
            }
            return;
        }
        DisableRecord disableRecord2 = new DisableRecord(i, iBinder);
        disableRecord2.setFlags(i2, i3, str);
        this.mDisableRecords.add(disableRecord2);
    }

    int gatherDisableActionsLocked(int i, int i2) {
        int size = this.mDisableRecords.size();
        int flags = 0;
        for (int i3 = 0; i3 < size; i3++) {
            DisableRecord disableRecord = this.mDisableRecords.get(i3);
            if (disableRecord.userId == i) {
                flags |= disableRecord.getFlags(i2);
            }
        }
        return flags;
    }

    protected void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        if (DumpUtils.checkDumpPermission(this.mContext, TAG, printWriter)) {
            synchronized (this.mLock) {
                printWriter.println("  mDisabled1=0x" + Integer.toHexString(this.mDisabled1));
                printWriter.println("  mDisabled2=0x" + Integer.toHexString(this.mDisabled2));
                int size = this.mDisableRecords.size();
                printWriter.println("  mDisableRecords.size=" + size);
                for (int i = 0; i < size; i++) {
                    printWriter.println("    [" + i + "] " + this.mDisableRecords.get(i));
                }
                printWriter.println("  mCurrentUserId=" + this.mCurrentUserId);
                printWriter.println("  mIcons=");
                for (String str : this.mIcons.keySet()) {
                    printWriter.println("    ");
                    printWriter.print(str);
                    printWriter.print(" -> ");
                    StatusBarIcon statusBarIcon = this.mIcons.get(str);
                    printWriter.print(statusBarIcon);
                    if (!TextUtils.isEmpty(statusBarIcon.contentDescription)) {
                        printWriter.print(" \"");
                        printWriter.print(statusBarIcon.contentDescription);
                        printWriter.print("\"");
                    }
                    printWriter.println();
                }
            }
        }
    }

    private static final Context getUiContext() {
        return ActivityThread.currentActivityThread().getSystemUiContext();
    }
}
