package com.android.server.wm;

import android.animation.Animator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.content.ClipData;
import android.content.ClipDescription;
import android.graphics.Point;
import android.hardware.input.InputManager;
import android.os.IBinder;
import android.os.Process;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.UserManagerInternal;
import android.util.Slog;
import android.view.Display;
import android.view.DragEvent;
import android.view.InputChannel;
import android.view.SurfaceControl;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import com.android.internal.view.IDragAndDropPermissions;
import com.android.server.LocalServices;
import com.android.server.input.InputApplicationHandle;
import com.android.server.input.InputWindowHandle;
import com.android.server.usb.descriptors.UsbACInterface;
import com.mediatek.server.wm.WmsExt;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.function.Consumer;

class DragState {
    private static final String ANIMATED_PROPERTY_ALPHA = "alpha";
    private static final String ANIMATED_PROPERTY_SCALE = "scale";
    private static final String ANIMATED_PROPERTY_X = "x";
    private static final String ANIMATED_PROPERTY_Y = "y";
    private static final int DRAG_FLAGS_URI_ACCESS = 3;
    private static final int DRAG_FLAGS_URI_PERMISSIONS = 195;
    private static final long MAX_ANIMATION_DURATION_MS = 375;
    private static final long MIN_ANIMATION_DURATION_MS = 195;
    private ValueAnimator mAnimator;
    boolean mCrossProfileCopyAllowed;
    float mCurrentX;
    float mCurrentY;
    ClipData mData;
    ClipDescription mDataDescription;
    DisplayContent mDisplayContent;
    final DragDropController mDragDropController;
    boolean mDragInProgress;
    boolean mDragResult;
    int mFlags;
    InputInterceptor mInputInterceptor;
    IBinder mLocalWin;
    float mOriginalAlpha;
    float mOriginalX;
    float mOriginalY;
    int mPid;
    final WindowManagerService mService;
    int mSourceUserId;
    SurfaceControl mSurfaceControl;
    WindowState mTargetWindow;
    float mThumbOffsetX;
    float mThumbOffsetY;
    IBinder mToken;
    int mTouchSource;
    int mUid;
    volatile boolean mAnimationCompleted = false;
    private final Interpolator mCubicEaseOutInterpolator = new DecelerateInterpolator(1.5f);
    private Point mDisplaySize = new Point();
    ArrayList<WindowState> mNotifiedWindows = new ArrayList<>();

    DragState(WindowManagerService windowManagerService, DragDropController dragDropController, IBinder iBinder, SurfaceControl surfaceControl, int i, IBinder iBinder2) {
        this.mService = windowManagerService;
        this.mDragDropController = dragDropController;
        this.mToken = iBinder;
        this.mSurfaceControl = surfaceControl;
        this.mFlags = i;
        this.mLocalWin = iBinder2;
    }

    void closeLocked() {
        float f;
        float f2;
        if (this.mInputInterceptor != null) {
            if (WindowManagerDebugConfig.DEBUG_DRAG) {
                Slog.d(WmsExt.TAG, "unregistering drag input channel");
            }
            this.mDragDropController.sendHandlerMessage(1, this.mInputInterceptor);
            this.mInputInterceptor = null;
            this.mService.mInputMonitor.updateInputWindowsLw(true);
        }
        if (this.mDragInProgress) {
            int iMyPid = Process.myPid();
            if (WindowManagerDebugConfig.DEBUG_DRAG) {
                Slog.d(WmsExt.TAG, "broadcasting DRAG_ENDED");
            }
            for (WindowState windowState : this.mNotifiedWindows) {
                if (this.mDragResult || windowState.mSession.mPid != this.mPid) {
                    f = 0.0f;
                    f2 = 0.0f;
                } else {
                    f = this.mCurrentX;
                    f2 = this.mCurrentY;
                }
                DragEvent dragEventObtain = DragEvent.obtain(4, f, f2, null, null, null, null, this.mDragResult);
                try {
                    windowState.mClient.dispatchDragEvent(dragEventObtain);
                } catch (RemoteException e) {
                    Slog.w(WmsExt.TAG, "Unable to drag-end window " + windowState);
                }
                if (iMyPid != windowState.mSession.mPid) {
                    dragEventObtain.recycle();
                }
            }
            this.mNotifiedWindows.clear();
            this.mDragInProgress = false;
        }
        if (isFromSource(UsbACInterface.FORMAT_III_IEC1937_MPEG1_Layer1)) {
            this.mService.restorePointerIconLocked(this.mDisplayContent, this.mCurrentX, this.mCurrentY);
            this.mTouchSource = 0;
        }
        if (this.mSurfaceControl != null) {
            this.mSurfaceControl.destroy();
            this.mSurfaceControl = null;
        }
        if (this.mAnimator != null && !this.mAnimationCompleted) {
            Slog.wtf(WmsExt.TAG, "Unexpectedly destroying mSurfaceControl while animation is running");
        }
        this.mFlags = 0;
        this.mLocalWin = null;
        this.mToken = null;
        this.mData = null;
        this.mThumbOffsetY = 0.0f;
        this.mThumbOffsetX = 0.0f;
        this.mNotifiedWindows = null;
        this.mDragDropController.onDragStateClosedLocked(this);
    }

    class InputInterceptor {
        InputChannel mClientChannel;
        InputApplicationHandle mDragApplicationHandle;
        InputWindowHandle mDragWindowHandle;
        DragInputEventReceiver mInputEventReceiver;
        InputChannel mServerChannel;

        InputInterceptor(Display display) {
            InputChannel[] inputChannelArrOpenInputChannelPair = InputChannel.openInputChannelPair("drag");
            this.mServerChannel = inputChannelArrOpenInputChannelPair[0];
            this.mClientChannel = inputChannelArrOpenInputChannelPair[1];
            DragState.this.mService.mInputManager.registerInputChannel(this.mServerChannel, null);
            this.mInputEventReceiver = new DragInputEventReceiver(this.mClientChannel, DragState.this.mService.mH.getLooper(), DragState.this.mDragDropController);
            this.mDragApplicationHandle = new InputApplicationHandle(null);
            this.mDragApplicationHandle.name = "drag";
            this.mDragApplicationHandle.dispatchingTimeoutNanos = 30000000000L;
            this.mDragWindowHandle = new InputWindowHandle(this.mDragApplicationHandle, null, null, display.getDisplayId());
            this.mDragWindowHandle.name = "drag";
            this.mDragWindowHandle.inputChannel = this.mServerChannel;
            this.mDragWindowHandle.layer = DragState.this.getDragLayerLocked();
            this.mDragWindowHandle.layoutParamsFlags = 0;
            this.mDragWindowHandle.layoutParamsType = 2016;
            this.mDragWindowHandle.dispatchingTimeoutNanos = 30000000000L;
            this.mDragWindowHandle.visible = true;
            this.mDragWindowHandle.canReceiveKeys = false;
            this.mDragWindowHandle.hasFocus = true;
            this.mDragWindowHandle.hasWallpaper = false;
            this.mDragWindowHandle.paused = false;
            this.mDragWindowHandle.ownerPid = Process.myPid();
            this.mDragWindowHandle.ownerUid = Process.myUid();
            this.mDragWindowHandle.inputFeatures = 0;
            this.mDragWindowHandle.scaleFactor = 1.0f;
            this.mDragWindowHandle.touchableRegion.setEmpty();
            this.mDragWindowHandle.frameLeft = 0;
            this.mDragWindowHandle.frameTop = 0;
            this.mDragWindowHandle.frameRight = DragState.this.mDisplaySize.x;
            this.mDragWindowHandle.frameBottom = DragState.this.mDisplaySize.y;
            if (WindowManagerDebugConfig.DEBUG_ORIENTATION) {
                Slog.d(WmsExt.TAG, "Pausing rotation during drag");
            }
            DragState.this.mService.pauseRotationLocked();
        }

        void tearDown() {
            DragState.this.mService.mInputManager.unregisterInputChannel(this.mServerChannel);
            this.mInputEventReceiver.dispose();
            this.mInputEventReceiver = null;
            this.mClientChannel.dispose();
            this.mServerChannel.dispose();
            this.mClientChannel = null;
            this.mServerChannel = null;
            this.mDragWindowHandle = null;
            this.mDragApplicationHandle = null;
            if (WindowManagerDebugConfig.DEBUG_ORIENTATION) {
                Slog.d(WmsExt.TAG, "Resuming rotation after drag");
            }
            DragState.this.mService.resumeRotationLocked();
        }
    }

    InputChannel getInputChannel() {
        if (this.mInputInterceptor == null) {
            return null;
        }
        return this.mInputInterceptor.mServerChannel;
    }

    InputWindowHandle getInputWindowHandle() {
        if (this.mInputInterceptor == null) {
            return null;
        }
        return this.mInputInterceptor.mDragWindowHandle;
    }

    void register(Display display) {
        display.getRealSize(this.mDisplaySize);
        if (WindowManagerDebugConfig.DEBUG_DRAG) {
            Slog.d(WmsExt.TAG, "registering drag input channel");
        }
        if (this.mInputInterceptor != null) {
            Slog.e(WmsExt.TAG, "Duplicate register of drag input channel");
        } else {
            this.mInputInterceptor = new InputInterceptor(display);
            this.mService.mInputMonitor.updateInputWindowsLw(true);
        }
    }

    int getDragLayerLocked() {
        return (this.mService.mPolicy.getWindowLayerFromTypeLw(2016) * 10000) + 1000;
    }

    void broadcastDragStartedLocked(final float f, final float f2) {
        this.mCurrentX = f;
        this.mOriginalX = f;
        this.mCurrentY = f2;
        this.mOriginalY = f2;
        this.mDataDescription = this.mData != null ? this.mData.getDescription() : null;
        this.mNotifiedWindows.clear();
        this.mDragInProgress = true;
        this.mSourceUserId = UserHandle.getUserId(this.mUid);
        this.mCrossProfileCopyAllowed = true ^ ((UserManagerInternal) LocalServices.getService(UserManagerInternal.class)).getUserRestriction(this.mSourceUserId, "no_cross_profile_copy_paste");
        if (WindowManagerDebugConfig.DEBUG_DRAG) {
            Slog.d(WmsExt.TAG, "broadcasting DRAG_STARTED at (" + f + ", " + f2 + ")");
        }
        this.mDisplayContent.forAllWindows(new Consumer() {
            @Override
            public final void accept(Object obj) {
                DragState dragState = this.f$0;
                dragState.sendDragStartedLocked((WindowState) obj, f, f2, dragState.mDataDescription);
            }
        }, false);
    }

    private void sendDragStartedLocked(WindowState windowState, float f, float f2, ClipDescription clipDescription) {
        if (this.mDragInProgress && isValidDropTarget(windowState)) {
            DragEvent dragEventObtainDragEvent = obtainDragEvent(windowState, 1, f, f2, null, clipDescription, null, null, false);
            try {
                try {
                    windowState.mClient.dispatchDragEvent(dragEventObtainDragEvent);
                    this.mNotifiedWindows.add(windowState);
                    if (Process.myPid() == windowState.mSession.mPid) {
                        return;
                    }
                } catch (RemoteException e) {
                    Slog.w(WmsExt.TAG, "Unable to drag-start window " + windowState);
                    if (Process.myPid() == windowState.mSession.mPid) {
                        return;
                    }
                }
                dragEventObtainDragEvent.recycle();
            } catch (Throwable th) {
                if (Process.myPid() != windowState.mSession.mPid) {
                    dragEventObtainDragEvent.recycle();
                }
                throw th;
            }
        }
    }

    private boolean isValidDropTarget(WindowState windowState) {
        if (windowState == null || !windowState.isPotentialDragTarget()) {
            return false;
        }
        if (((this.mFlags & 256) == 0 || !targetWindowSupportsGlobalDrag(windowState)) && this.mLocalWin != windowState.mClient.asBinder()) {
            return false;
        }
        return this.mCrossProfileCopyAllowed || this.mSourceUserId == UserHandle.getUserId(windowState.getOwningUid());
    }

    private boolean targetWindowSupportsGlobalDrag(WindowState windowState) {
        return windowState.mAppToken == null || windowState.mAppToken.mTargetSdk >= 24;
    }

    void sendDragStartedIfNeededLocked(WindowState windowState) {
        if (!this.mDragInProgress || isWindowNotified(windowState)) {
            return;
        }
        if (WindowManagerDebugConfig.DEBUG_DRAG) {
            Slog.d(WmsExt.TAG, "need to send DRAG_STARTED to new window " + windowState);
        }
        sendDragStartedLocked(windowState, this.mCurrentX, this.mCurrentY, this.mDataDescription);
    }

    private boolean isWindowNotified(WindowState windowState) {
        Iterator<WindowState> it = this.mNotifiedWindows.iterator();
        while (it.hasNext()) {
            if (it.next() == windowState) {
                return true;
            }
        }
        return false;
    }

    void endDragLocked() {
        if (this.mAnimator != null) {
            return;
        }
        if (!this.mDragResult) {
            this.mAnimator = createReturnAnimationLocked();
        } else {
            closeLocked();
        }
    }

    void cancelDragLocked() {
        if (this.mAnimator != null) {
            return;
        }
        if (!this.mDragInProgress) {
            closeLocked();
        } else {
            this.mAnimator = createCancelAnimationLocked();
        }
    }

    void notifyMoveLocked(float f, float f2) {
        if (this.mAnimator != null) {
            return;
        }
        this.mCurrentX = f;
        this.mCurrentY = f2;
        if (WindowManagerDebugConfig.SHOW_LIGHT_TRANSACTIONS) {
            Slog.i(WmsExt.TAG, ">>> OPEN TRANSACTION notifyMoveLocked");
        }
        this.mService.openSurfaceTransaction();
        try {
            this.mSurfaceControl.setPosition(f - this.mThumbOffsetX, f2 - this.mThumbOffsetY);
            if (WindowManagerDebugConfig.SHOW_TRANSACTIONS) {
                Slog.i(WmsExt.TAG, "  DRAG " + this.mSurfaceControl + ": pos=(" + ((int) (f - this.mThumbOffsetX)) + "," + ((int) (f2 - this.mThumbOffsetY)) + ")");
            }
            notifyLocationLocked(f, f2);
        } finally {
            this.mService.closeSurfaceTransaction("notifyMoveLw");
            if (WindowManagerDebugConfig.SHOW_LIGHT_TRANSACTIONS) {
                Slog.i(WmsExt.TAG, "<<< CLOSE TRANSACTION notifyMoveLocked");
            }
        }
    }

    void notifyLocationLocked(float f, float f2) {
        WindowState touchableWinAtPointLocked = this.mDisplayContent.getTouchableWinAtPointLocked(f, f2);
        if (touchableWinAtPointLocked != null && !isWindowNotified(touchableWinAtPointLocked)) {
            touchableWinAtPointLocked = null;
        }
        try {
            int iMyPid = Process.myPid();
            if (touchableWinAtPointLocked != this.mTargetWindow && this.mTargetWindow != null) {
                if (WindowManagerDebugConfig.DEBUG_DRAG) {
                    Slog.d(WmsExt.TAG, "sending DRAG_EXITED to " + this.mTargetWindow);
                }
                DragEvent dragEventObtainDragEvent = obtainDragEvent(this.mTargetWindow, 6, 0.0f, 0.0f, null, null, null, null, false);
                this.mTargetWindow.mClient.dispatchDragEvent(dragEventObtainDragEvent);
                if (iMyPid != this.mTargetWindow.mSession.mPid) {
                    dragEventObtainDragEvent.recycle();
                }
            }
            if (touchableWinAtPointLocked != null) {
                DragEvent dragEventObtainDragEvent2 = obtainDragEvent(touchableWinAtPointLocked, 2, f, f2, null, null, null, null, false);
                touchableWinAtPointLocked.mClient.dispatchDragEvent(dragEventObtainDragEvent2);
                if (iMyPid != touchableWinAtPointLocked.mSession.mPid) {
                    dragEventObtainDragEvent2.recycle();
                }
            }
        } catch (RemoteException e) {
            Slog.w(WmsExt.TAG, "can't send drag notification to windows");
        }
        this.mTargetWindow = touchableWinAtPointLocked;
    }

    void notifyDropLocked(float f, float f2) {
        if (this.mAnimator != null) {
            return;
        }
        this.mCurrentX = f;
        this.mCurrentY = f2;
        WindowState touchableWinAtPointLocked = this.mDisplayContent.getTouchableWinAtPointLocked(f, f2);
        if (!isWindowNotified(touchableWinAtPointLocked)) {
            this.mDragResult = false;
            endDragLocked();
            return;
        }
        if (WindowManagerDebugConfig.DEBUG_DRAG) {
            Slog.d(WmsExt.TAG, "sending DROP to " + touchableWinAtPointLocked);
        }
        int userId = UserHandle.getUserId(touchableWinAtPointLocked.getOwningUid());
        IDragAndDropPermissions dragAndDropPermissionsHandler = ((this.mFlags & 256) == 0 || (this.mFlags & 3) == 0 || this.mData == null) ? null : new DragAndDropPermissionsHandler(this.mData, this.mUid, touchableWinAtPointLocked.getOwningPackage(), this.mFlags & 195, this.mSourceUserId, userId);
        if (this.mSourceUserId != userId && this.mData != null) {
            this.mData.fixUris(this.mSourceUserId);
        }
        int iMyPid = Process.myPid();
        IBinder iBinderAsBinder = touchableWinAtPointLocked.mClient.asBinder();
        DragEvent dragEventObtainDragEvent = obtainDragEvent(touchableWinAtPointLocked, 3, f, f2, null, null, this.mData, dragAndDropPermissionsHandler, false);
        try {
            try {
                touchableWinAtPointLocked.mClient.dispatchDragEvent(dragEventObtainDragEvent);
                this.mDragDropController.sendTimeoutMessage(0, iBinderAsBinder);
            } catch (RemoteException e) {
                Slog.w(WmsExt.TAG, "can't send drop notification to win " + touchableWinAtPointLocked);
                endDragLocked();
                if (iMyPid != touchableWinAtPointLocked.mSession.mPid) {
                }
            }
            this.mToken = iBinderAsBinder;
        } finally {
            if (iMyPid != touchableWinAtPointLocked.mSession.mPid) {
                dragEventObtainDragEvent.recycle();
            }
        }
    }

    boolean isInProgress() {
        return this.mDragInProgress;
    }

    private static DragEvent obtainDragEvent(WindowState windowState, int i, float f, float f2, Object obj, ClipDescription clipDescription, ClipData clipData, IDragAndDropPermissions iDragAndDropPermissions, boolean z) {
        return DragEvent.obtain(i, windowState.translateToWindowX(f), windowState.translateToWindowY(f2), obj, clipDescription, clipData, iDragAndDropPermissions, z);
    }

    private ValueAnimator createReturnAnimationLocked() {
        final ValueAnimator valueAnimatorOfPropertyValuesHolder = ValueAnimator.ofPropertyValuesHolder(PropertyValuesHolder.ofFloat(ANIMATED_PROPERTY_X, this.mCurrentX - this.mThumbOffsetX, this.mOriginalX - this.mThumbOffsetX), PropertyValuesHolder.ofFloat(ANIMATED_PROPERTY_Y, this.mCurrentY - this.mThumbOffsetY, this.mOriginalY - this.mThumbOffsetY), PropertyValuesHolder.ofFloat(ANIMATED_PROPERTY_SCALE, 1.0f, 1.0f), PropertyValuesHolder.ofFloat(ANIMATED_PROPERTY_ALPHA, this.mOriginalAlpha, this.mOriginalAlpha / 2.0f));
        float f = this.mOriginalX - this.mCurrentX;
        float f2 = this.mOriginalY - this.mCurrentY;
        long jSqrt = MIN_ANIMATION_DURATION_MS + ((long) ((Math.sqrt((f * f) + (f2 * f2)) / Math.sqrt((this.mDisplaySize.x * this.mDisplaySize.x) + (this.mDisplaySize.y * this.mDisplaySize.y))) * 180.0d));
        AnimationListener animationListener = new AnimationListener();
        valueAnimatorOfPropertyValuesHolder.setDuration(jSqrt);
        valueAnimatorOfPropertyValuesHolder.setInterpolator(this.mCubicEaseOutInterpolator);
        valueAnimatorOfPropertyValuesHolder.addListener(animationListener);
        valueAnimatorOfPropertyValuesHolder.addUpdateListener(animationListener);
        this.mService.mAnimationHandler.post(new Runnable() {
            @Override
            public final void run() {
                valueAnimatorOfPropertyValuesHolder.start();
            }
        });
        return valueAnimatorOfPropertyValuesHolder;
    }

    private ValueAnimator createCancelAnimationLocked() {
        final ValueAnimator valueAnimatorOfPropertyValuesHolder = ValueAnimator.ofPropertyValuesHolder(PropertyValuesHolder.ofFloat(ANIMATED_PROPERTY_X, this.mCurrentX - this.mThumbOffsetX, this.mCurrentX), PropertyValuesHolder.ofFloat(ANIMATED_PROPERTY_Y, this.mCurrentY - this.mThumbOffsetY, this.mCurrentY), PropertyValuesHolder.ofFloat(ANIMATED_PROPERTY_SCALE, 1.0f, 0.0f), PropertyValuesHolder.ofFloat(ANIMATED_PROPERTY_ALPHA, this.mOriginalAlpha, 0.0f));
        AnimationListener animationListener = new AnimationListener();
        valueAnimatorOfPropertyValuesHolder.setDuration(MIN_ANIMATION_DURATION_MS);
        valueAnimatorOfPropertyValuesHolder.setInterpolator(this.mCubicEaseOutInterpolator);
        valueAnimatorOfPropertyValuesHolder.addListener(animationListener);
        valueAnimatorOfPropertyValuesHolder.addUpdateListener(animationListener);
        this.mService.mAnimationHandler.post(new Runnable() {
            @Override
            public final void run() {
                valueAnimatorOfPropertyValuesHolder.start();
            }
        });
        return valueAnimatorOfPropertyValuesHolder;
    }

    private boolean isFromSource(int i) {
        return (this.mTouchSource & i) == i;
    }

    void overridePointerIconLocked(int i) {
        this.mTouchSource = i;
        if (isFromSource(UsbACInterface.FORMAT_III_IEC1937_MPEG1_Layer1)) {
            InputManager.getInstance().setPointerIconType(1021);
        }
    }

    private class AnimationListener implements ValueAnimator.AnimatorUpdateListener, Animator.AnimatorListener {
        private AnimationListener() {
        }

        @Override
        public void onAnimationUpdate(ValueAnimator valueAnimator) {
            SurfaceControl.Transaction transaction = new SurfaceControl.Transaction();
            Throwable th = null;
            try {
                transaction.setPosition(DragState.this.mSurfaceControl, ((Float) valueAnimator.getAnimatedValue(DragState.ANIMATED_PROPERTY_X)).floatValue(), ((Float) valueAnimator.getAnimatedValue(DragState.ANIMATED_PROPERTY_Y)).floatValue());
                transaction.setAlpha(DragState.this.mSurfaceControl, ((Float) valueAnimator.getAnimatedValue(DragState.ANIMATED_PROPERTY_ALPHA)).floatValue());
                transaction.setMatrix(DragState.this.mSurfaceControl, ((Float) valueAnimator.getAnimatedValue(DragState.ANIMATED_PROPERTY_SCALE)).floatValue(), 0.0f, 0.0f, ((Float) valueAnimator.getAnimatedValue(DragState.ANIMATED_PROPERTY_SCALE)).floatValue());
                transaction.apply();
                transaction.close();
            } catch (Throwable th2) {
                if (0 != 0) {
                    try {
                        transaction.close();
                    } catch (Throwable th3) {
                        th.addSuppressed(th3);
                    }
                } else {
                    transaction.close();
                }
                throw th2;
            }
        }

        @Override
        public void onAnimationStart(Animator animator) {
        }

        @Override
        public void onAnimationCancel(Animator animator) {
        }

        @Override
        public void onAnimationRepeat(Animator animator) {
        }

        @Override
        public void onAnimationEnd(Animator animator) {
            DragState.this.mAnimationCompleted = true;
            DragState.this.mDragDropController.sendHandlerMessage(2, null);
        }
    }
}
