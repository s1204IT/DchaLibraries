package com.android.server.wm;

import android.content.ClipData;
import android.graphics.Rect;
import android.graphics.Region;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.Trace;
import android.os.UserHandle;
import android.util.MergedConfiguration;
import android.util.Slog;
import android.view.DisplayCutout;
import android.view.IWindow;
import android.view.IWindowId;
import android.view.IWindowSession;
import android.view.IWindowSessionCallback;
import android.view.InputChannel;
import android.view.Surface;
import android.view.SurfaceControl;
import android.view.SurfaceSession;
import android.view.WindowManager;
import com.android.internal.os.logging.MetricsLoggerWrapper;
import com.android.internal.view.IInputContext;
import com.android.internal.view.IInputMethodClient;
import com.android.internal.view.IInputMethodManager;
import com.mediatek.server.wm.WmsExt;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Set;

class Session extends IWindowSession.Stub implements IBinder.DeathRecipient {
    private AlertWindowNotification mAlertWindowNotification;
    final IWindowSessionCallback mCallback;
    final boolean mCanAcquireSleepToken;
    final boolean mCanAddInternalSystemWindow;
    final boolean mCanHideNonSystemOverlayWindows;
    final IInputMethodClient mClient;
    private final DragDropController mDragDropController;
    private float mLastReportedAnimatorScale;
    private String mPackageName;
    private String mRelayoutTag;
    final WindowManagerService mService;
    private boolean mShowingAlertWindowNotificationAllowed;
    private final String mStringName;
    SurfaceSession mSurfaceSession;
    private int mNumWindow = 0;
    private final Set<WindowSurfaceController> mAppOverlaySurfaces = new HashSet();
    private final Set<WindowSurfaceController> mAlertWindowSurfaces = new HashSet();
    private boolean mClientDead = false;
    final int mUid = Binder.getCallingUid();
    final int mPid = Binder.getCallingPid();

    public Session(WindowManagerService windowManagerService, IWindowSessionCallback iWindowSessionCallback, IInputMethodClient iInputMethodClient, IInputContext iInputContext) {
        this.mService = windowManagerService;
        this.mCallback = iWindowSessionCallback;
        this.mClient = iInputMethodClient;
        this.mLastReportedAnimatorScale = windowManagerService.getCurrentAnimatorScale();
        this.mCanAddInternalSystemWindow = windowManagerService.mContext.checkCallingOrSelfPermission("android.permission.INTERNAL_SYSTEM_WINDOW") == 0;
        this.mCanHideNonSystemOverlayWindows = windowManagerService.mContext.checkCallingOrSelfPermission("android.permission.HIDE_NON_SYSTEM_OVERLAY_WINDOWS") == 0;
        this.mCanAcquireSleepToken = windowManagerService.mContext.checkCallingOrSelfPermission("android.permission.DEVICE_POWER") == 0;
        this.mShowingAlertWindowNotificationAllowed = this.mService.mShowAlertWindowNotifications;
        this.mDragDropController = this.mService.mDragDropController;
        StringBuilder sb = new StringBuilder();
        sb.append("Session{");
        sb.append(Integer.toHexString(System.identityHashCode(this)));
        sb.append(" ");
        sb.append(this.mPid);
        if (this.mUid < 10000) {
            sb.append(":");
            sb.append(this.mUid);
        } else {
            sb.append(":u");
            sb.append(UserHandle.getUserId(this.mUid));
            sb.append('a');
            sb.append(UserHandle.getAppId(this.mUid));
        }
        sb.append("}");
        this.mStringName = sb.toString();
        synchronized (this.mService.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                if (this.mService.mInputMethodManager == null && this.mService.mHaveInputMethods) {
                    this.mService.mInputMethodManager = IInputMethodManager.Stub.asInterface(ServiceManager.getService("input_method"));
                }
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            try {
                if (this.mService.mInputMethodManager != null) {
                    this.mService.mInputMethodManager.addClient(iInputMethodClient, iInputContext, this.mUid, this.mPid);
                } else {
                    iInputMethodClient.setUsingInputMethod(false);
                }
                iInputMethodClient.asBinder().linkToDeath(this, 0);
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        } catch (RemoteException e) {
            try {
                if (this.mService.mInputMethodManager != null) {
                    this.mService.mInputMethodManager.removeClient(iInputMethodClient);
                }
            } catch (RemoteException e2) {
            }
        }
    }

    public boolean onTransact(int i, Parcel parcel, Parcel parcel2, int i2) throws RemoteException {
        try {
            return super.onTransact(i, parcel, parcel2, i2);
        } catch (RuntimeException e) {
            if (!(e instanceof SecurityException)) {
                Slog.wtf(WmsExt.TAG, "Window Session Crash", e);
            }
            throw e;
        }
    }

    @Override
    public void binderDied() {
        try {
            if (this.mService.mInputMethodManager != null) {
                this.mService.mInputMethodManager.removeClient(this.mClient);
            }
        } catch (RemoteException e) {
        }
        synchronized (this.mService.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                this.mClient.asBinder().unlinkToDeath(this, 0);
                this.mClientDead = true;
                killSessionLocked();
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
    }

    public int add(IWindow iWindow, int i, WindowManager.LayoutParams layoutParams, int i2, Rect rect, Rect rect2, InputChannel inputChannel) {
        return addToDisplay(iWindow, i, layoutParams, i2, 0, new Rect(), rect, rect2, null, new DisplayCutout.ParcelableWrapper(), inputChannel);
    }

    public int addToDisplay(IWindow iWindow, int i, WindowManager.LayoutParams layoutParams, int i2, int i3, Rect rect, Rect rect2, Rect rect3, Rect rect4, DisplayCutout.ParcelableWrapper parcelableWrapper, InputChannel inputChannel) {
        return this.mService.addWindow(this, iWindow, i, layoutParams, i2, i3, rect, rect2, rect3, rect4, parcelableWrapper, inputChannel);
    }

    public int addWithoutInputChannel(IWindow iWindow, int i, WindowManager.LayoutParams layoutParams, int i2, Rect rect, Rect rect2) {
        return addToDisplayWithoutInputChannel(iWindow, i, layoutParams, i2, 0, rect, rect2);
    }

    public int addToDisplayWithoutInputChannel(IWindow iWindow, int i, WindowManager.LayoutParams layoutParams, int i2, int i3, Rect rect, Rect rect2) {
        return this.mService.addWindow(this, iWindow, i, layoutParams, i2, i3, new Rect(), rect, rect2, null, new DisplayCutout.ParcelableWrapper(), null);
    }

    public void remove(IWindow iWindow) {
        this.mService.removeWindow(this, iWindow);
    }

    public void prepareToReplaceWindows(IBinder iBinder, boolean z) {
        this.mService.setWillReplaceWindows(iBinder, z);
    }

    public int relayout(IWindow iWindow, int i, WindowManager.LayoutParams layoutParams, int i2, int i3, int i4, int i5, long j, Rect rect, Rect rect2, Rect rect3, Rect rect4, Rect rect5, Rect rect6, Rect rect7, DisplayCutout.ParcelableWrapper parcelableWrapper, MergedConfiguration mergedConfiguration, Surface surface) throws Throwable {
        Trace.traceBegin(32L, this.mRelayoutTag);
        int iRelayoutWindow = this.mService.relayoutWindow(this, iWindow, i, layoutParams, i2, i3, i4, i5, j, rect, rect2, rect3, rect4, rect5, rect6, rect7, parcelableWrapper, mergedConfiguration, surface);
        Trace.traceEnd(32L);
        return iRelayoutWindow;
    }

    public boolean outOfMemory(IWindow iWindow) {
        return this.mService.outOfMemoryWindow(this, iWindow);
    }

    public void setTransparentRegion(IWindow iWindow, Region region) {
        this.mService.setTransparentRegionWindow(this, iWindow, region);
    }

    public void setInsets(IWindow iWindow, int i, Rect rect, Rect rect2, Region region) {
        this.mService.setInsetsWindow(this, iWindow, i, rect, rect2, region);
    }

    public void getDisplayFrame(IWindow iWindow, Rect rect) {
        this.mService.getWindowDisplayFrame(this, iWindow, rect);
    }

    public void finishDrawing(IWindow iWindow) {
        if (WindowManagerService.localLOGV) {
            Slog.v(WmsExt.TAG, "IWindow finishDrawing called for " + iWindow);
        }
        this.mService.finishDrawingWindow(this, iWindow);
    }

    public void setInTouchMode(boolean z) {
        synchronized (this.mService.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                this.mService.mInTouchMode = z;
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
    }

    public boolean getInTouchMode() {
        boolean z;
        synchronized (this.mService.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                z = this.mService.mInTouchMode;
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
        return z;
    }

    public boolean performHapticFeedback(IWindow iWindow, int i, boolean z) {
        boolean zPerformHapticFeedbackLw;
        synchronized (this.mService.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    zPerformHapticFeedbackLw = this.mService.mPolicy.performHapticFeedbackLw(this.mService.windowForClientLocked(this, iWindow, true), i, z);
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
        return zPerformHapticFeedbackLw;
    }

    public IBinder performDrag(IWindow iWindow, int i, SurfaceControl surfaceControl, int i2, float f, float f2, float f3, float f4, ClipData clipData) {
        int callingPid = Binder.getCallingPid();
        int callingUid = Binder.getCallingUid();
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            return this.mDragDropController.performDrag(this.mSurfaceSession, callingPid, callingUid, iWindow, i, surfaceControl, i2, f, f2, f3, f4, clipData);
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public void reportDropResult(IWindow iWindow, boolean z) {
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            this.mDragDropController.reportDropResult(iWindow, z);
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public void cancelDragAndDrop(IBinder iBinder) {
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            this.mDragDropController.cancelDragAndDrop(iBinder);
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public void dragRecipientEntered(IWindow iWindow) {
        this.mDragDropController.dragRecipientEntered(iWindow);
    }

    public void dragRecipientExited(IWindow iWindow) {
        this.mDragDropController.dragRecipientExited(iWindow);
    }

    public boolean startMovingTask(IWindow iWindow, float f, float f2) {
        if (WindowManagerDebugConfig.DEBUG_TASK_POSITIONING) {
            Slog.d(WmsExt.TAG, "startMovingTask: {" + f + "," + f2 + "}");
        }
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            return this.mService.mTaskPositioningController.startMovingTask(iWindow, f, f2);
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public void setWallpaperPosition(IBinder iBinder, float f, float f2, float f3, float f4) {
        synchronized (this.mService.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    this.mService.mRoot.mWallpaperController.setWindowWallpaperPosition(this.mService.windowForClientLocked(this, iBinder, true), f, f2, f3, f4);
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
    }

    public void wallpaperOffsetsComplete(IBinder iBinder) {
        synchronized (this.mService.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                this.mService.mRoot.mWallpaperController.wallpaperOffsetsComplete(iBinder);
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
    }

    public void setWallpaperDisplayOffset(IBinder iBinder, int i, int i2) {
        synchronized (this.mService.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    this.mService.mRoot.mWallpaperController.setWindowWallpaperDisplayOffset(this.mService.windowForClientLocked(this, iBinder, true), i, i2);
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
    }

    public Bundle sendWallpaperCommand(IBinder iBinder, String str, int i, int i2, int i3, Bundle bundle, boolean z) {
        Bundle bundleSendWindowWallpaperCommand;
        synchronized (this.mService.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    bundleSendWindowWallpaperCommand = this.mService.mRoot.mWallpaperController.sendWindowWallpaperCommand(this.mService.windowForClientLocked(this, iBinder, true), str, i, i2, i3, bundle, z);
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
        return bundleSendWindowWallpaperCommand;
    }

    public void wallpaperCommandComplete(IBinder iBinder, Bundle bundle) {
        synchronized (this.mService.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                this.mService.mRoot.mWallpaperController.wallpaperCommandComplete(iBinder);
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
    }

    public void onRectangleOnScreenRequested(IBinder iBinder, Rect rect) {
        synchronized (this.mService.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    this.mService.onRectangleOnScreenRequested(iBinder, rect);
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
    }

    public IWindowId getWindowId(IBinder iBinder) {
        return this.mService.getWindowId(iBinder);
    }

    public void pokeDrawLock(IBinder iBinder) {
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            this.mService.pokeDrawLock(this, iBinder);
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public void updatePointerIcon(IWindow iWindow) {
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            this.mService.updatePointerIcon(iWindow);
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public void updateTapExcludeRegion(IWindow iWindow, int i, int i2, int i3, int i4, int i5) {
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            this.mService.updateTapExcludeRegion(iWindow, i, i2, i3, i4, i5);
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    void windowAddedLocked(String str) {
        this.mPackageName = str;
        this.mRelayoutTag = "relayoutWindow: " + this.mPackageName;
        if (this.mSurfaceSession == null) {
            if (WindowManagerService.localLOGV) {
                Slog.v(WmsExt.TAG, "First window added to " + this + ", creating SurfaceSession");
            }
            this.mSurfaceSession = new SurfaceSession();
            if (WindowManagerDebugConfig.SHOW_TRANSACTIONS) {
                Slog.i(WmsExt.TAG, "  NEW SURFACE SESSION " + this.mSurfaceSession);
            }
            this.mService.mSessions.add(this);
            if (this.mLastReportedAnimatorScale != this.mService.getCurrentAnimatorScale()) {
                this.mService.dispatchNewAnimatorScaleLocked(this);
            }
        }
        this.mNumWindow++;
    }

    void windowRemovedLocked() {
        this.mNumWindow--;
        killSessionLocked();
    }

    void onWindowSurfaceVisibilityChanged(WindowSurfaceController windowSurfaceController, boolean z, int i) {
        boolean zRemove;
        boolean zRemove2;
        if (!WindowManager.LayoutParams.isSystemAlertWindowType(i)) {
            return;
        }
        if (!this.mCanAddInternalSystemWindow) {
            if (z) {
                zRemove2 = this.mAlertWindowSurfaces.add(windowSurfaceController);
                MetricsLoggerWrapper.logAppOverlayEnter(this.mUid, this.mPackageName, zRemove2, i, true);
            } else {
                zRemove2 = this.mAlertWindowSurfaces.remove(windowSurfaceController);
                MetricsLoggerWrapper.logAppOverlayExit(this.mUid, this.mPackageName, zRemove2, i, true);
            }
            if (zRemove2) {
                if (this.mAlertWindowSurfaces.isEmpty()) {
                    cancelAlertWindowNotification();
                } else if (this.mAlertWindowNotification == null) {
                    this.mAlertWindowNotification = new AlertWindowNotification(this.mService, this.mPackageName);
                    if (this.mShowingAlertWindowNotificationAllowed) {
                        this.mAlertWindowNotification.post();
                    }
                }
            }
        }
        if (i != 2038) {
            return;
        }
        if (z) {
            zRemove = this.mAppOverlaySurfaces.add(windowSurfaceController);
            MetricsLoggerWrapper.logAppOverlayEnter(this.mUid, this.mPackageName, zRemove, i, false);
        } else {
            zRemove = this.mAppOverlaySurfaces.remove(windowSurfaceController);
            MetricsLoggerWrapper.logAppOverlayExit(this.mUid, this.mPackageName, zRemove, i, false);
        }
        if (zRemove) {
            setHasOverlayUi(!this.mAppOverlaySurfaces.isEmpty());
        }
    }

    void setShowingAlertWindowNotificationAllowed(boolean z) {
        this.mShowingAlertWindowNotificationAllowed = z;
        if (this.mAlertWindowNotification != null) {
            if (z) {
                this.mAlertWindowNotification.post();
            } else {
                this.mAlertWindowNotification.cancel(false);
            }
        }
    }

    private void killSessionLocked() {
        if (this.mNumWindow > 0 || !this.mClientDead) {
            return;
        }
        this.mService.mSessions.remove(this);
        if (this.mSurfaceSession == null) {
            return;
        }
        if (WindowManagerService.localLOGV) {
            Slog.v(WmsExt.TAG, "Last window removed from " + this + ", destroying " + this.mSurfaceSession);
        }
        if (WindowManagerDebugConfig.SHOW_TRANSACTIONS) {
            Slog.i(WmsExt.TAG, "  KILL SURFACE SESSION " + this.mSurfaceSession);
        }
        try {
            this.mSurfaceSession.kill();
        } catch (Exception e) {
            Slog.w(WmsExt.TAG, "Exception thrown when killing surface session " + this.mSurfaceSession + " in session " + this + ": " + e.toString());
        }
        this.mSurfaceSession = null;
        this.mAlertWindowSurfaces.clear();
        this.mAppOverlaySurfaces.clear();
        setHasOverlayUi(false);
        cancelAlertWindowNotification();
    }

    private void setHasOverlayUi(boolean z) {
        this.mService.mH.obtainMessage(58, this.mPid, z ? 1 : 0).sendToTarget();
    }

    private void cancelAlertWindowNotification() {
        if (this.mAlertWindowNotification == null) {
            return;
        }
        this.mAlertWindowNotification.cancel(true);
        this.mAlertWindowNotification = null;
    }

    void dump(PrintWriter printWriter, String str) {
        printWriter.print(str);
        printWriter.print("mNumWindow=");
        printWriter.print(this.mNumWindow);
        printWriter.print(" mCanAddInternalSystemWindow=");
        printWriter.print(this.mCanAddInternalSystemWindow);
        printWriter.print(" mAppOverlaySurfaces=");
        printWriter.print(this.mAppOverlaySurfaces);
        printWriter.print(" mAlertWindowSurfaces=");
        printWriter.print(this.mAlertWindowSurfaces);
        printWriter.print(" mClientDead=");
        printWriter.print(this.mClientDead);
        printWriter.print(" mSurfaceSession=");
        printWriter.println(this.mSurfaceSession);
        printWriter.print(str);
        printWriter.print("mPackageName=");
        printWriter.println(this.mPackageName);
    }

    public String toString() {
        return this.mStringName;
    }

    boolean hasAlertWindowSurfaces() {
        return !this.mAlertWindowSurfaces.isEmpty();
    }
}
