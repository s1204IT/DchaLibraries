package com.android.server.wm;

import android.content.ClipData;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.graphics.Region;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.util.Slog;
import android.view.Display;
import android.view.IWindow;
import android.view.IWindowId;
import android.view.IWindowSession;
import android.view.IWindowSessionCallback;
import android.view.InputChannel;
import android.view.Surface;
import android.view.SurfaceControl;
import android.view.SurfaceSession;
import android.view.WindowManager;
import com.android.internal.view.IInputContext;
import com.android.internal.view.IInputMethodClient;
import com.android.internal.view.IInputMethodManager;
import java.io.PrintWriter;

final class Session extends IWindowSession.Stub implements IBinder.DeathRecipient {
    final IWindowSessionCallback mCallback;
    final boolean mCanAddInternalSystemWindow;
    final boolean mCanHideNonSystemOverlayWindows;
    final IInputMethodClient mClient;
    final IInputContext mInputContext;
    float mLastReportedAnimatorScale;
    final WindowManagerService mService;
    final String mStringName;
    SurfaceSession mSurfaceSession;
    int mNumWindow = 0;
    boolean mClientDead = false;
    final int mUid = Binder.getCallingUid();
    final int mPid = Binder.getCallingPid();

    public Session(WindowManagerService service, IWindowSessionCallback callback, IInputMethodClient client, IInputContext inputContext) {
        this.mService = service;
        this.mCallback = callback;
        this.mClient = client;
        this.mInputContext = inputContext;
        this.mCanAddInternalSystemWindow = service.mContext.checkCallingOrSelfPermission("android.permission.INTERNAL_SYSTEM_WINDOW") == 0;
        this.mCanHideNonSystemOverlayWindows = service.mContext.checkCallingOrSelfPermission("android.permission.HIDE_NON_SYSTEM_OVERLAY_WINDOWS") == 0;
        this.mLastReportedAnimatorScale = service.getCurrentAnimatorScale();
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
            if (this.mService.mInputMethodManager == null && this.mService.mHaveInputMethods) {
                IBinder b = ServiceManager.getService("input_method");
                this.mService.mInputMethodManager = IInputMethodManager.Stub.asInterface(b);
            }
        }
        long ident = Binder.clearCallingIdentity();
        try {
            try {
                if (this.mService.mInputMethodManager != null) {
                    this.mService.mInputMethodManager.addClient(client, inputContext, this.mUid, this.mPid);
                } else {
                    client.setUsingInputMethod(false);
                }
                client.asBinder().linkToDeath(this, 0);
            } catch (RemoteException e) {
                try {
                    if (this.mService.mInputMethodManager != null) {
                        this.mService.mInputMethodManager.removeClient(client);
                    }
                } catch (RemoteException e2) {
                }
            }
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
        try {
            return super.onTransact(code, data, reply, flags);
        } catch (RuntimeException e) {
            if (!(e instanceof SecurityException)) {
                Slog.wtf("WindowManager", "Window Session Crash", e);
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
            this.mClient.asBinder().unlinkToDeath(this, 0);
            this.mClientDead = true;
            killSessionLocked();
        }
    }

    public int add(IWindow window, int seq, WindowManager.LayoutParams attrs, int viewVisibility, Rect outContentInsets, Rect outStableInsets, InputChannel outInputChannel) {
        return addToDisplay(window, seq, attrs, viewVisibility, 0, outContentInsets, outStableInsets, outInputChannel);
    }

    public int addToDisplay(IWindow window, int seq, WindowManager.LayoutParams attrs, int viewVisibility, int displayId, Rect outContentInsets, Rect outStableInsets, InputChannel outInputChannel) {
        return this.mService.addWindow(this, window, seq, attrs, viewVisibility, displayId, outContentInsets, outStableInsets, outInputChannel);
    }

    public int addWithoutInputChannel(IWindow window, int seq, WindowManager.LayoutParams attrs, int viewVisibility, Rect outContentInsets, Rect outStableInsets) {
        return addToDisplayWithoutInputChannel(window, seq, attrs, viewVisibility, 0, outContentInsets, outStableInsets);
    }

    public int addToDisplayWithoutInputChannel(IWindow window, int seq, WindowManager.LayoutParams attrs, int viewVisibility, int displayId, Rect outContentInsets, Rect outStableInsets) {
        return this.mService.addWindow(this, window, seq, attrs, viewVisibility, displayId, outContentInsets, outStableInsets, null);
    }

    public void remove(IWindow window) {
        this.mService.removeWindow(this, window);
    }

    public int relayout(IWindow window, int seq, WindowManager.LayoutParams attrs, int requestedWidth, int requestedHeight, int viewFlags, int flags, Rect outFrame, Rect outOverscanInsets, Rect outContentInsets, Rect outVisibleInsets, Rect outStableInsets, Configuration outConfig, Surface outSurface) {
        int res = this.mService.relayoutWindow(this, window, seq, attrs, requestedWidth, requestedHeight, viewFlags, flags, outFrame, outOverscanInsets, outContentInsets, outVisibleInsets, outStableInsets, outConfig, outSurface);
        return res;
    }

    public void performDeferredDestroy(IWindow window) {
        this.mService.performDeferredDestroyWindow(this, window);
    }

    public boolean outOfMemory(IWindow window) {
        return this.mService.outOfMemoryWindow(this, window);
    }

    public void setTransparentRegion(IWindow window, Region region) {
        this.mService.setTransparentRegionWindow(this, window, region);
    }

    public void setInsets(IWindow window, int touchableInsets, Rect contentInsets, Rect visibleInsets, Region touchableArea) {
        this.mService.setInsetsWindow(this, window, touchableInsets, contentInsets, visibleInsets, touchableArea);
    }

    public void getDisplayFrame(IWindow window, Rect outDisplayFrame) {
        this.mService.getWindowDisplayFrame(this, window, outDisplayFrame);
    }

    public void finishDrawing(IWindow window) {
        this.mService.finishDrawingWindow(this, window);
    }

    public void setInTouchMode(boolean mode) {
        synchronized (this.mService.mWindowMap) {
            this.mService.mInTouchMode = mode;
        }
    }

    public boolean getInTouchMode() {
        boolean z;
        synchronized (this.mService.mWindowMap) {
            z = this.mService.mInTouchMode;
        }
        return z;
    }

    public boolean performHapticFeedback(IWindow window, int effectId, boolean always) {
        boolean zPerformHapticFeedbackLw;
        synchronized (this.mService.mWindowMap) {
            long ident = Binder.clearCallingIdentity();
            try {
                zPerformHapticFeedbackLw = this.mService.mPolicy.performHapticFeedbackLw(this.mService.windowForClientLocked(this, window, true), effectId, always);
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }
        return zPerformHapticFeedbackLw;
    }

    public IBinder prepareDrag(IWindow window, int flags, int width, int height, Surface outSurface) {
        return this.mService.prepareDragSurface(window, this.mSurfaceSession, flags, width, height, outSurface);
    }

    public boolean performDrag(IWindow window, IBinder dragToken, float touchX, float touchY, float thumbCenterX, float thumbCenterY, ClipData data) {
        synchronized (this.mService.mWindowMap) {
            if (this.mService.mDragState == null) {
                Slog.w("WindowManager", "No drag prepared");
                throw new IllegalStateException("performDrag() without prepareDrag()");
            }
            if (dragToken != this.mService.mDragState.mToken) {
                Slog.w("WindowManager", "Performing mismatched drag");
                throw new IllegalStateException("performDrag() does not match prepareDrag()");
            }
            WindowState callingWin = this.mService.windowForClientLocked((Session) null, window, false);
            if (callingWin == null) {
                Slog.w("WindowManager", "Bad requesting window " + window);
                return false;
            }
            this.mService.mH.removeMessages(20, window.asBinder());
            DisplayContent displayContent = callingWin.getDisplayContent();
            if (displayContent == null) {
                return false;
            }
            Display display = displayContent.getDisplay();
            this.mService.mDragState.register(display);
            this.mService.mInputMonitor.updateInputWindowsLw(true);
            if (!this.mService.mInputManager.transferTouchFocus(callingWin.mInputChannel, this.mService.mDragState.mServerChannel)) {
                Slog.e("WindowManager", "Unable to transfer touch focus");
                this.mService.mDragState.unregister();
                this.mService.mDragState = null;
                this.mService.mInputMonitor.updateInputWindowsLw(true);
                return false;
            }
            this.mService.mDragState.mData = data;
            this.mService.mDragState.mCurrentX = touchX;
            this.mService.mDragState.mCurrentY = touchY;
            this.mService.mDragState.broadcastDragStartedLw(touchX, touchY);
            this.mService.mDragState.mThumbOffsetX = thumbCenterX;
            this.mService.mDragState.mThumbOffsetY = thumbCenterY;
            SurfaceControl surfaceControl = this.mService.mDragState.mSurfaceControl;
            SurfaceControl.openTransaction();
            try {
                surfaceControl.setPosition(touchX - thumbCenterX, touchY - thumbCenterY);
                surfaceControl.setAlpha(0.7071f);
                surfaceControl.setLayer(this.mService.mDragState.getDragLayerLw());
                surfaceControl.setLayerStack(display.getLayerStack());
                surfaceControl.show();
                return true;
            } finally {
                SurfaceControl.closeTransaction();
            }
        }
    }

    public void reportDropResult(IWindow window, boolean consumed) {
        IBinder token = window.asBinder();
        synchronized (this.mService.mWindowMap) {
            long ident = Binder.clearCallingIdentity();
            try {
                if (this.mService.mDragState == null) {
                    Slog.w("WindowManager", "Drop result given but no drag in progress");
                    return;
                }
                if (this.mService.mDragState.mToken != token) {
                    Slog.w("WindowManager", "Invalid drop-result claim by " + window);
                    throw new IllegalStateException("reportDropResult() by non-recipient");
                }
                this.mService.mH.removeMessages(21, window.asBinder());
                WindowState callingWin = this.mService.windowForClientLocked((Session) null, window, false);
                if (callingWin == null) {
                    Slog.w("WindowManager", "Bad result-reporting window " + window);
                } else {
                    this.mService.mDragState.mDragResult = consumed;
                    this.mService.mDragState.endDragLw();
                }
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }
    }

    public void dragRecipientEntered(IWindow window) {
    }

    public void dragRecipientExited(IWindow window) {
    }

    public void setWallpaperPosition(IBinder window, float x, float y, float xStep, float yStep) {
        synchronized (this.mService.mWindowMap) {
            long ident = Binder.clearCallingIdentity();
            try {
                this.mService.setWindowWallpaperPositionLocked(this.mService.windowForClientLocked(this, window, true), x, y, xStep, yStep);
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }
    }

    public void wallpaperOffsetsComplete(IBinder window) {
        this.mService.wallpaperOffsetsComplete(window);
    }

    public void setWallpaperDisplayOffset(IBinder window, int x, int y) {
        synchronized (this.mService.mWindowMap) {
            long ident = Binder.clearCallingIdentity();
            try {
                this.mService.setWindowWallpaperDisplayOffsetLocked(this.mService.windowForClientLocked(this, window, true), x, y);
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }
    }

    public Bundle sendWallpaperCommand(IBinder window, String action, int x, int y, int z, Bundle extras, boolean sync) {
        Bundle bundleSendWindowWallpaperCommandLocked;
        synchronized (this.mService.mWindowMap) {
            long ident = Binder.clearCallingIdentity();
            try {
                bundleSendWindowWallpaperCommandLocked = this.mService.sendWindowWallpaperCommandLocked(this.mService.windowForClientLocked(this, window, true), action, x, y, z, extras, sync);
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }
        return bundleSendWindowWallpaperCommandLocked;
    }

    public void wallpaperCommandComplete(IBinder window, Bundle result) {
        this.mService.wallpaperCommandComplete(window, result);
    }

    public void setUniverseTransform(IBinder window, float alpha, float offx, float offy, float dsdx, float dtdx, float dsdy, float dtdy) {
        synchronized (this.mService.mWindowMap) {
            long ident = Binder.clearCallingIdentity();
            try {
                this.mService.setUniverseTransformLocked(this.mService.windowForClientLocked(this, window, true), alpha, offx, offy, dsdx, dtdx, dsdy, dtdy);
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }
    }

    public void onRectangleOnScreenRequested(IBinder token, Rect rectangle) {
        synchronized (this.mService.mWindowMap) {
            long identity = Binder.clearCallingIdentity();
            try {
                this.mService.onRectangleOnScreenRequested(token, rectangle);
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }
    }

    public IWindowId getWindowId(IBinder window) {
        return this.mService.getWindowId(window);
    }

    void windowAddedLocked() {
        if (this.mSurfaceSession == null) {
            this.mSurfaceSession = new SurfaceSession();
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

    void killSessionLocked() {
        if (this.mNumWindow <= 0 && this.mClientDead) {
            this.mService.mSessions.remove(this);
            if (this.mSurfaceSession != null) {
                try {
                    this.mSurfaceSession.kill();
                } catch (Exception e) {
                    Slog.w("WindowManager", "Exception thrown when killing surface session " + this.mSurfaceSession + " in session " + this + ": " + e.toString());
                }
                this.mSurfaceSession = null;
            }
        }
    }

    void dump(PrintWriter pw, String prefix) {
        pw.print(prefix);
        pw.print("mNumWindow=");
        pw.print(this.mNumWindow);
        pw.print(" mClientDead=");
        pw.print(this.mClientDead);
        pw.print(" mSurfaceSession=");
        pw.println(this.mSurfaceSession);
    }

    public String toString() {
        return this.mStringName;
    }
}
