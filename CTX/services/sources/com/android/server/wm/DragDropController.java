package com.android.server.wm;

import android.content.ClipData;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Slog;
import android.view.IWindow;
import android.view.SurfaceControl;
import android.view.SurfaceSession;
import com.android.internal.util.Preconditions;
import com.android.server.input.InputWindowHandle;
import com.android.server.wm.DragState;
import com.android.server.wm.WindowManagerInternal;
import com.mediatek.server.wm.WmsExt;
import java.util.concurrent.atomic.AtomicReference;

class DragDropController {
    private static final float DRAG_SHADOW_ALPHA_TRANSPARENT = 0.7071f;
    private static final long DRAG_TIMEOUT_MS = 5000;
    static final int MSG_ANIMATION_END = 2;
    static final int MSG_DRAG_END_TIMEOUT = 0;
    static final int MSG_TEAR_DOWN_DRAG_AND_DROP_INPUT = 1;
    private AtomicReference<WindowManagerInternal.IDragDropCallback> mCallback = new AtomicReference<>(new WindowManagerInternal.IDragDropCallback() {
    });
    private DragState mDragState;
    private final Handler mHandler;
    private WindowManagerService mService;

    boolean dragDropActiveLocked() {
        return this.mDragState != null;
    }

    InputWindowHandle getInputWindowHandleLocked() {
        return this.mDragState.getInputWindowHandle();
    }

    void registerCallback(WindowManagerInternal.IDragDropCallback iDragDropCallback) {
        Preconditions.checkNotNull(iDragDropCallback);
        this.mCallback.set(iDragDropCallback);
    }

    DragDropController(WindowManagerService windowManagerService, Looper looper) {
        this.mService = windowManagerService;
        this.mHandler = new DragHandler(windowManagerService, looper);
    }

    void sendDragStartedIfNeededLocked(WindowState windowState) {
        this.mDragState.sendDragStartedIfNeededLocked(windowState);
    }

    IBinder performDrag(SurfaceSession surfaceSession, int i, int i2, IWindow iWindow, int i3, SurfaceControl surfaceControl, int i4, float f, float f2, float f3, float f4, ClipData clipData) {
        SurfaceControl surfaceControl2;
        AtomicReference<WindowManagerInternal.IDragDropCallback> atomicReference;
        WindowManagerInternal.IDragDropCallback iDragDropCallback;
        WindowManagerInternal.IDragDropCallback iDragDropCallback2;
        if (WindowManagerDebugConfig.DEBUG_DRAG) {
            Slog.d(WmsExt.TAG, "perform drag: win=" + iWindow + " surface=" + surfaceControl + " flags=" + Integer.toHexString(i3) + " data=" + clipData);
        }
        Binder binder = new Binder();
        boolean zPrePerformDrag = this.mCallback.get().prePerformDrag(iWindow, binder, i4, f, f2, f3, f4, clipData);
        try {
            synchronized (this.mService.mWindowMap) {
                try {
                    try {
                        WindowManagerService.boostPriorityForLockedSection();
                        try {
                            if (!zPrePerformDrag) {
                                Slog.w(WmsExt.TAG, "IDragDropCallback rejects the performDrag request");
                                if (surfaceControl != null) {
                                    surfaceControl.release();
                                }
                                if (this.mDragState != null && !this.mDragState.isInProgress()) {
                                    this.mDragState.closeLocked();
                                }
                                WindowManagerService.resetPriorityAfterLockedSection();
                                return null;
                            }
                            if (dragDropActiveLocked()) {
                                Slog.w(WmsExt.TAG, "Drag already in progress");
                                if (surfaceControl != null) {
                                    surfaceControl.release();
                                }
                                if (this.mDragState != null && !this.mDragState.isInProgress()) {
                                    this.mDragState.closeLocked();
                                }
                                WindowManagerService.resetPriorityAfterLockedSection();
                                return null;
                            }
                            WindowState windowStateWindowForClientLocked = this.mService.windowForClientLocked((Session) null, iWindow, false);
                            if (windowStateWindowForClientLocked == null) {
                                Slog.w(WmsExt.TAG, "Bad requesting window " + iWindow);
                                if (surfaceControl != null) {
                                    surfaceControl.release();
                                }
                                if (this.mDragState != null && !this.mDragState.isInProgress()) {
                                    this.mDragState.closeLocked();
                                }
                                WindowManagerService.resetPriorityAfterLockedSection();
                                return null;
                            }
                            DisplayContent displayContent = windowStateWindowForClientLocked.getDisplayContent();
                            if (displayContent == null) {
                                Slog.w(WmsExt.TAG, "display content is null");
                                if (surfaceControl != null) {
                                    surfaceControl.release();
                                }
                                if (this.mDragState != null && !this.mDragState.isInProgress()) {
                                    this.mDragState.closeLocked();
                                }
                                WindowManagerService.resetPriorityAfterLockedSection();
                                return null;
                            }
                            float f5 = (i3 & 512) == 0 ? DRAG_SHADOW_ALPHA_TRANSPARENT : 1.0f;
                            this.mDragState = new DragState(this.mService, this, new Binder(), surfaceControl, i3, iWindow.asBinder());
                            try {
                                this.mDragState.mPid = i;
                                this.mDragState.mUid = i2;
                                this.mDragState.mOriginalAlpha = f5;
                                this.mDragState.mToken = binder;
                                if (!this.mCallback.get().registerInputChannel(this.mDragState, displayContent.getDisplay(), this.mService.mInputManager, windowStateWindowForClientLocked.mInputChannel)) {
                                    Slog.e(WmsExt.TAG, "Unable to transfer touch focus");
                                    if (this.mDragState != null && !this.mDragState.isInProgress()) {
                                        this.mDragState.closeLocked();
                                    }
                                    WindowManagerService.resetPriorityAfterLockedSection();
                                    this.mCallback.get().postPerformDrag();
                                    return null;
                                }
                                surfaceControl2 = null;
                                try {
                                    this.mDragState.mDisplayContent = displayContent;
                                    this.mDragState.mData = clipData;
                                    this.mDragState.broadcastDragStartedLocked(f, f2);
                                    this.mDragState.overridePointerIconLocked(i4);
                                    this.mDragState.mThumbOffsetX = f3;
                                    this.mDragState.mThumbOffsetY = f4;
                                    SurfaceControl surfaceControl3 = this.mDragState.mSurfaceControl;
                                    if (WindowManagerDebugConfig.SHOW_LIGHT_TRANSACTIONS) {
                                        Slog.i(WmsExt.TAG, ">>> OPEN TRANSACTION performDrag");
                                    }
                                    SurfaceControl.Transaction pendingTransaction = windowStateWindowForClientLocked.getPendingTransaction();
                                    pendingTransaction.setAlpha(surfaceControl3, this.mDragState.mOriginalAlpha);
                                    pendingTransaction.setPosition(surfaceControl3, f - f3, f2 - f4);
                                    pendingTransaction.show(surfaceControl3);
                                    displayContent.reparentToOverlay(pendingTransaction, surfaceControl3);
                                    windowStateWindowForClientLocked.scheduleAnimation();
                                    if (WindowManagerDebugConfig.SHOW_LIGHT_TRANSACTIONS) {
                                        Slog.i(WmsExt.TAG, "<<< CLOSE TRANSACTION performDrag");
                                    }
                                    this.mDragState.notifyLocationLocked(f, f2);
                                    if (this.mDragState != null && !this.mDragState.isInProgress()) {
                                        this.mDragState.closeLocked();
                                    }
                                    WindowManagerService.resetPriorityAfterLockedSection();
                                    return binder;
                                } catch (Throwable th) {
                                    th = th;
                                }
                            } catch (Throwable th2) {
                                th = th2;
                                surfaceControl2 = null;
                            }
                        } catch (Throwable th3) {
                            th = th3;
                            surfaceControl2 = surfaceControl;
                        }
                    } catch (Throwable th4) {
                        WindowManagerService.resetPriorityAfterLockedSection();
                        throw th4;
                    }
                } catch (Throwable th5) {
                    th = th5;
                    surfaceControl2 = surfaceControl;
                }
                if (surfaceControl2 != null) {
                    surfaceControl2.release();
                }
                if (this.mDragState != null && !this.mDragState.isInProgress()) {
                    this.mDragState.closeLocked();
                }
                throw th;
            }
        } finally {
            this.mCallback.get().postPerformDrag();
        }
    }

    void reportDropResult(IWindow iWindow, boolean z) {
        AtomicReference<WindowManagerInternal.IDragDropCallback> atomicReference;
        WindowManagerInternal.IDragDropCallback iDragDropCallback;
        WindowManagerInternal.IDragDropCallback iDragDropCallback2;
        IBinder iBinderAsBinder = iWindow.asBinder();
        if (WindowManagerDebugConfig.DEBUG_DRAG) {
            Slog.d(WmsExt.TAG, "Drop result=" + z + " reported by " + iBinderAsBinder);
        }
        this.mCallback.get().preReportDropResult(iWindow, z);
        try {
            synchronized (this.mService.mWindowMap) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    if (this.mDragState == null) {
                        Slog.w(WmsExt.TAG, "Drop result given but no drag in progress");
                        WindowManagerService.resetPriorityAfterLockedSection();
                        return;
                    }
                    if (this.mDragState.mToken != iBinderAsBinder) {
                        Slog.w(WmsExt.TAG, "Invalid drop-result claim by " + iWindow);
                        throw new IllegalStateException("reportDropResult() by non-recipient");
                    }
                    this.mHandler.removeMessages(0, iWindow.asBinder());
                    if (this.mService.windowForClientLocked((Session) null, iWindow, false) != null) {
                        this.mDragState.mDragResult = z;
                        this.mDragState.endDragLocked();
                        WindowManagerService.resetPriorityAfterLockedSection();
                    } else {
                        Slog.w(WmsExt.TAG, "Bad result-reporting window " + iWindow);
                        WindowManagerService.resetPriorityAfterLockedSection();
                    }
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
        } finally {
            this.mCallback.get().postReportDropResult();
        }
    }

    void cancelDragAndDrop(IBinder iBinder) {
        if (WindowManagerDebugConfig.DEBUG_DRAG) {
            Slog.d(WmsExt.TAG, "cancelDragAndDrop");
        }
        this.mCallback.get().preCancelDragAndDrop(iBinder);
        try {
            synchronized (this.mService.mWindowMap) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    if (this.mDragState == null) {
                        Slog.w(WmsExt.TAG, "cancelDragAndDrop() without prepareDrag()");
                        throw new IllegalStateException("cancelDragAndDrop() without prepareDrag()");
                    }
                    if (this.mDragState.mToken != iBinder) {
                        Slog.w(WmsExt.TAG, "cancelDragAndDrop() does not match prepareDrag()");
                        throw new IllegalStateException("cancelDragAndDrop() does not match prepareDrag()");
                    }
                    this.mDragState.mDragResult = false;
                    this.mDragState.cancelDragLocked();
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
        } finally {
            this.mCallback.get().postCancelDragAndDrop();
        }
    }

    void handleMotionEvent(boolean z, float f, float f2) {
        synchronized (this.mService.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                if (!dragDropActiveLocked()) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return;
                }
                if (z) {
                    this.mDragState.notifyMoveLocked(f, f2);
                } else {
                    this.mDragState.notifyDropLocked(f, f2);
                }
                WindowManagerService.resetPriorityAfterLockedSection();
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    void dragRecipientEntered(IWindow iWindow) {
        if (WindowManagerDebugConfig.DEBUG_DRAG) {
            Slog.d(WmsExt.TAG, "Drag into new candidate view @ " + iWindow.asBinder());
        }
    }

    void dragRecipientExited(IWindow iWindow) {
        if (WindowManagerDebugConfig.DEBUG_DRAG) {
            Slog.d(WmsExt.TAG, "Drag from old candidate view @ " + iWindow.asBinder());
        }
    }

    void sendHandlerMessage(int i, Object obj) {
        this.mHandler.obtainMessage(i, obj).sendToTarget();
    }

    void sendTimeoutMessage(int i, Object obj) {
        this.mHandler.removeMessages(i, obj);
        this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(i, obj), DRAG_TIMEOUT_MS);
    }

    void onDragStateClosedLocked(DragState dragState) {
        if (this.mDragState != dragState) {
            Slog.wtf(WmsExt.TAG, "Unknown drag state is closed");
        } else {
            this.mDragState = null;
        }
    }

    private class DragHandler extends Handler {
        private final WindowManagerService mService;

        DragHandler(WindowManagerService windowManagerService, Looper looper) {
            super(looper);
            this.mService = windowManagerService;
        }

        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case 0:
                    IBinder iBinder = (IBinder) message.obj;
                    if (WindowManagerDebugConfig.DEBUG_DRAG) {
                        Slog.w(WmsExt.TAG, "Timeout ending drag to win " + iBinder);
                    }
                    synchronized (this.mService.mWindowMap) {
                        try {
                            WindowManagerService.boostPriorityForLockedSection();
                            if (DragDropController.this.mDragState != null) {
                                DragDropController.this.mDragState.mDragResult = false;
                                DragDropController.this.mDragState.endDragLocked();
                            }
                        } finally {
                            WindowManagerService.resetPriorityAfterLockedSection();
                        }
                        break;
                    }
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return;
                case 1:
                    if (WindowManagerDebugConfig.DEBUG_DRAG) {
                        Slog.d(WmsExt.TAG, "Drag ending; tearing down input channel");
                    }
                    DragState.InputInterceptor inputInterceptor = (DragState.InputInterceptor) message.obj;
                    if (inputInterceptor == null) {
                        return;
                    }
                    synchronized (this.mService.mWindowMap) {
                        try {
                            WindowManagerService.boostPriorityForLockedSection();
                            inputInterceptor.tearDown();
                        } finally {
                        }
                        break;
                    }
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return;
                case 2:
                    synchronized (this.mService.mWindowMap) {
                        try {
                            WindowManagerService.boostPriorityForLockedSection();
                            if (DragDropController.this.mDragState != null) {
                                DragDropController.this.mDragState.closeLocked();
                                WindowManagerService.resetPriorityAfterLockedSection();
                                return;
                            } else {
                                Slog.wtf(WmsExt.TAG, "mDragState unexpectedly became null while plyaing animation");
                                return;
                            }
                        } finally {
                            WindowManagerService.resetPriorityAfterLockedSection();
                        }
                    }
                default:
                    return;
            }
        }
    }
}
