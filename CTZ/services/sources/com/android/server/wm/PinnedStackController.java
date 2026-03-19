package com.android.server.wm;

import android.R;
import android.app.RemoteAction;
import android.content.pm.ParceledListSlice;
import android.content.res.Resources;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Size;
import android.util.Slog;
import android.util.TypedValue;
import android.util.proto.ProtoOutputStream;
import android.view.DisplayInfo;
import android.view.Gravity;
import android.view.IPinnedStackController;
import android.view.IPinnedStackListener;
import com.android.internal.policy.PipSnapAlgorithm;
import com.android.server.UiThread;
import com.android.server.backup.BackupManagerConstants;
import com.android.server.wm.PinnedStackController;
import java.io.PrintWriter;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

class PinnedStackController {
    public static final float INVALID_SNAP_FRACTION = -1.0f;
    private static final String TAG = "WindowManager";
    private float mAspectRatio;
    private final PinnedStackControllerCallback mCallbacks;
    private int mCurrentMinSize;
    private float mDefaultAspectRatio;
    private int mDefaultMinSize;
    private int mDefaultStackGravity;
    private final DisplayContent mDisplayContent;
    private int mImeHeight;
    private boolean mIsImeShowing;
    private boolean mIsMinimized;
    private boolean mIsShelfShowing;
    private float mMaxAspectRatio;
    private float mMinAspectRatio;
    private IPinnedStackListener mPinnedStackListener;
    private final PinnedStackListenerDeathHandler mPinnedStackListenerDeathHandler;
    private Point mScreenEdgeInsets;
    private final WindowManagerService mService;
    private int mShelfHeight;
    private final PipSnapAlgorithm mSnapAlgorithm;
    private final Handler mHandler = UiThread.getHandler();
    private ArrayList<RemoteAction> mActions = new ArrayList<>();
    private final DisplayInfo mDisplayInfo = new DisplayInfo();
    private final Rect mStableInsets = new Rect();
    private float mReentrySnapFraction = -1.0f;
    private WeakReference<AppWindowToken> mLastPipActivity = null;
    private final DisplayMetrics mTmpMetrics = new DisplayMetrics();
    private final Rect mTmpInsets = new Rect();
    private final Rect mTmpRect = new Rect();
    private final Rect mTmpAnimatingBoundsRect = new Rect();
    private final Point mTmpDisplaySize = new Point();

    private class PinnedStackControllerCallback extends IPinnedStackController.Stub {
        private PinnedStackControllerCallback() {
        }

        public void setIsMinimized(final boolean z) {
            PinnedStackController.this.mHandler.post(new Runnable() {
                @Override
                public final void run() {
                    PinnedStackController.PinnedStackControllerCallback.lambda$setIsMinimized$0(this.f$0, z);
                }
            });
        }

        public static void lambda$setIsMinimized$0(PinnedStackControllerCallback pinnedStackControllerCallback, boolean z) {
            PinnedStackController.this.mIsMinimized = z;
            PinnedStackController.this.mSnapAlgorithm.setMinimized(z);
        }

        public void setMinEdgeSize(final int i) {
            PinnedStackController.this.mHandler.post(new Runnable() {
                @Override
                public final void run() {
                    PinnedStackController.PinnedStackControllerCallback pinnedStackControllerCallback = this.f$0;
                    PinnedStackController.this.mCurrentMinSize = Math.max(PinnedStackController.this.mDefaultMinSize, i);
                }
            });
        }

        public int getDisplayRotation() {
            int i;
            synchronized (PinnedStackController.this.mService.mWindowMap) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    i = PinnedStackController.this.mDisplayInfo.rotation;
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
            return i;
        }
    }

    private class PinnedStackListenerDeathHandler implements IBinder.DeathRecipient {
        private PinnedStackListenerDeathHandler() {
        }

        @Override
        public void binderDied() {
            if (PinnedStackController.this.mPinnedStackListener != null) {
                PinnedStackController.this.mPinnedStackListener.asBinder().unlinkToDeath(PinnedStackController.this.mPinnedStackListenerDeathHandler, 0);
            }
            PinnedStackController.this.mPinnedStackListener = null;
        }
    }

    PinnedStackController(WindowManagerService windowManagerService, DisplayContent displayContent) {
        this.mPinnedStackListenerDeathHandler = new PinnedStackListenerDeathHandler();
        this.mCallbacks = new PinnedStackControllerCallback();
        this.mAspectRatio = -1.0f;
        this.mService = windowManagerService;
        this.mDisplayContent = displayContent;
        this.mSnapAlgorithm = new PipSnapAlgorithm(windowManagerService.mContext);
        this.mDisplayInfo.copyFrom(this.mDisplayContent.getDisplayInfo());
        reloadResources();
        this.mAspectRatio = this.mDefaultAspectRatio;
    }

    void onConfigurationChanged() {
        reloadResources();
    }

    private void reloadResources() {
        Size size;
        Resources resources = this.mService.mContext.getResources();
        this.mDefaultMinSize = resources.getDimensionPixelSize(R.dimen.button_layout_height);
        this.mCurrentMinSize = this.mDefaultMinSize;
        this.mDefaultAspectRatio = resources.getFloat(R.dimen.alertDialog_material_side_margin_body);
        String string = resources.getString(R.string.activity_chooser_view_dialog_title_default);
        if (!string.isEmpty()) {
            size = Size.parseSize(string);
        } else {
            size = null;
        }
        this.mDefaultStackGravity = resources.getInteger(R.integer.config_bg_current_drain_window);
        this.mDisplayContent.getDisplay().getRealMetrics(this.mTmpMetrics);
        this.mScreenEdgeInsets = size == null ? new Point() : new Point(dpToPx(size.getWidth(), this.mTmpMetrics), dpToPx(size.getHeight(), this.mTmpMetrics));
        this.mMinAspectRatio = resources.getFloat(R.dimen.alertDialog_material_text_size_title);
        this.mMaxAspectRatio = resources.getFloat(R.dimen.alertDialog_material_text_size_body_1);
    }

    void registerPinnedStackListener(IPinnedStackListener iPinnedStackListener) {
        try {
            iPinnedStackListener.asBinder().linkToDeath(this.mPinnedStackListenerDeathHandler, 0);
            iPinnedStackListener.onListenerRegistered(this.mCallbacks);
            this.mPinnedStackListener = iPinnedStackListener;
            notifyImeVisibilityChanged(this.mIsImeShowing, this.mImeHeight);
            notifyShelfVisibilityChanged(this.mIsShelfShowing, this.mShelfHeight);
            notifyMovementBoundsChanged(false, false);
            notifyActionsChanged(this.mActions);
            notifyMinimizeChanged(this.mIsMinimized);
        } catch (RemoteException e) {
            Log.e("WindowManager", "Failed to register pinned stack listener", e);
        }
    }

    public boolean isValidPictureInPictureAspectRatio(float f) {
        return Float.compare(this.mMinAspectRatio, f) <= 0 && Float.compare(f, this.mMaxAspectRatio) <= 0;
    }

    Rect transformBoundsToAspectRatio(Rect rect, float f, boolean z) {
        float snapFraction = this.mSnapAlgorithm.getSnapFraction(rect, getMovementBounds(rect));
        Size sizeForAspectRatio = this.mSnapAlgorithm.getSizeForAspectRatio(f, z ? this.mCurrentMinSize : this.mDefaultMinSize, this.mDisplayInfo.logicalWidth, this.mDisplayInfo.logicalHeight);
        int iCenterX = (int) (rect.centerX() - (sizeForAspectRatio.getWidth() / 2.0f));
        int iCenterY = (int) (rect.centerY() - (sizeForAspectRatio.getHeight() / 2.0f));
        rect.set(iCenterX, iCenterY, sizeForAspectRatio.getWidth() + iCenterX, sizeForAspectRatio.getHeight() + iCenterY);
        this.mSnapAlgorithm.applySnapFraction(rect, getMovementBounds(rect), snapFraction);
        if (this.mIsMinimized) {
            applyMinimizedOffset(rect, getMovementBounds(rect));
        }
        return rect;
    }

    void saveReentrySnapFraction(AppWindowToken appWindowToken, Rect rect) {
        this.mReentrySnapFraction = getSnapFraction(rect);
        this.mLastPipActivity = new WeakReference<>(appWindowToken);
    }

    void resetReentrySnapFraction(AppWindowToken appWindowToken) {
        if (this.mLastPipActivity != null && this.mLastPipActivity.get() == appWindowToken) {
            this.mReentrySnapFraction = -1.0f;
            this.mLastPipActivity = null;
        }
    }

    Rect getDefaultOrLastSavedBounds() {
        return getDefaultBounds(this.mReentrySnapFraction);
    }

    Rect getDefaultBounds(float f) {
        Rect rect;
        synchronized (this.mService.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                Rect rect2 = new Rect();
                getInsetBounds(rect2);
                rect = new Rect();
                Size sizeForAspectRatio = this.mSnapAlgorithm.getSizeForAspectRatio(this.mDefaultAspectRatio, this.mDefaultMinSize, this.mDisplayInfo.logicalWidth, this.mDisplayInfo.logicalHeight);
                if (f != -1.0f) {
                    rect.set(0, 0, sizeForAspectRatio.getWidth(), sizeForAspectRatio.getHeight());
                    this.mSnapAlgorithm.applySnapFraction(rect, getMovementBounds(rect), f);
                } else {
                    Gravity.apply(this.mDefaultStackGravity, sizeForAspectRatio.getWidth(), sizeForAspectRatio.getHeight(), rect2, 0, Math.max(this.mIsImeShowing ? this.mImeHeight : 0, this.mIsShelfShowing ? this.mShelfHeight : 0), rect);
                }
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
        return rect;
    }

    synchronized void onDisplayInfoChanged() {
        this.mDisplayInfo.copyFrom(this.mDisplayContent.getDisplayInfo());
        notifyMovementBoundsChanged(false, false);
    }

    boolean onTaskStackBoundsChanged(Rect rect, Rect rect2) {
        synchronized (this.mService.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                DisplayInfo displayInfo = this.mDisplayContent.getDisplayInfo();
                if (this.mDisplayInfo.equals(displayInfo)) {
                    rect2.setEmpty();
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return false;
                }
                if (rect.isEmpty()) {
                    this.mDisplayInfo.copyFrom(displayInfo);
                    rect2.setEmpty();
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return false;
                }
                this.mTmpRect.set(rect);
                Rect rect3 = this.mTmpRect;
                float snapFraction = getSnapFraction(rect3);
                this.mDisplayInfo.copyFrom(displayInfo);
                Rect movementBounds = getMovementBounds(rect3, false, false);
                this.mSnapAlgorithm.applySnapFraction(rect3, movementBounds, snapFraction);
                if (this.mIsMinimized) {
                    applyMinimizedOffset(rect3, movementBounds);
                }
                notifyMovementBoundsChanged(false, false);
                rect2.set(rect3);
                WindowManagerService.resetPriorityAfterLockedSection();
                return true;
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    void setAdjustedForIme(boolean z, int i) {
        boolean z2 = z && i > 0;
        if (!z2) {
            i = 0;
        }
        if (z2 == this.mIsImeShowing && i == this.mImeHeight) {
            return;
        }
        this.mIsImeShowing = z2;
        this.mImeHeight = i;
        notifyImeVisibilityChanged(z2, i);
        notifyMovementBoundsChanged(true, false);
    }

    void setAdjustedForShelf(boolean z, int i) {
        boolean z2 = z && i > 0;
        if (z2 == this.mIsShelfShowing && i == this.mShelfHeight) {
            return;
        }
        this.mIsShelfShowing = z2;
        this.mShelfHeight = i;
        notifyShelfVisibilityChanged(z2, i);
        notifyMovementBoundsChanged(false, true);
    }

    void setAspectRatio(float f) {
        if (Float.compare(this.mAspectRatio, f) != 0) {
            this.mAspectRatio = f;
            notifyMovementBoundsChanged(false, false);
        }
    }

    float getAspectRatio() {
        return this.mAspectRatio;
    }

    void setActions(List<RemoteAction> list) {
        this.mActions.clear();
        if (list != null) {
            this.mActions.addAll(list);
        }
        notifyActionsChanged(this.mActions);
    }

    private void notifyImeVisibilityChanged(boolean z, int i) {
        if (this.mPinnedStackListener != null) {
            try {
                this.mPinnedStackListener.onImeVisibilityChanged(z, i);
            } catch (RemoteException e) {
                Slog.e("WindowManager", "Error delivering bounds changed event.", e);
            }
        }
    }

    private void notifyShelfVisibilityChanged(boolean z, int i) {
        if (this.mPinnedStackListener != null) {
            try {
                this.mPinnedStackListener.onShelfVisibilityChanged(z, i);
            } catch (RemoteException e) {
                Slog.e("WindowManager", "Error delivering bounds changed event.", e);
            }
        }
    }

    private void notifyMinimizeChanged(boolean z) {
        if (this.mPinnedStackListener != null) {
            try {
                this.mPinnedStackListener.onMinimizedStateChanged(z);
            } catch (RemoteException e) {
                Slog.e("WindowManager", "Error delivering minimize changed event.", e);
            }
        }
    }

    private void notifyActionsChanged(List<RemoteAction> list) {
        if (this.mPinnedStackListener != null) {
            try {
                this.mPinnedStackListener.onActionsChanged(new ParceledListSlice(list));
            } catch (RemoteException e) {
                Slog.e("WindowManager", "Error delivering actions changed event.", e);
            }
        }
    }

    private void notifyMovementBoundsChanged(boolean z, boolean z2) {
        synchronized (this.mService.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                if (this.mPinnedStackListener == null) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return;
                }
                try {
                    Rect rect = new Rect();
                    getInsetBounds(rect);
                    Rect defaultBounds = getDefaultBounds(-1.0f);
                    if (isValidPictureInPictureAspectRatio(this.mAspectRatio)) {
                        transformBoundsToAspectRatio(defaultBounds, this.mAspectRatio, false);
                    }
                    Rect rect2 = this.mTmpAnimatingBoundsRect;
                    TaskStack pinnedStack = this.mDisplayContent.getPinnedStack();
                    if (pinnedStack != null) {
                        pinnedStack.getAnimationOrCurrentBounds(rect2);
                    } else {
                        rect2.set(defaultBounds);
                    }
                    this.mPinnedStackListener.onMovementBoundsChanged(rect, defaultBounds, rect2, z, z2, this.mDisplayInfo.rotation);
                } catch (RemoteException e) {
                    Slog.e("WindowManager", "Error delivering actions changed event.", e);
                }
                WindowManagerService.resetPriorityAfterLockedSection();
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    private void getInsetBounds(Rect rect) {
        synchronized (this.mService.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                this.mService.mPolicy.getStableInsetsLw(this.mDisplayInfo.rotation, this.mDisplayInfo.logicalWidth, this.mDisplayInfo.logicalHeight, this.mDisplayInfo.displayCutout, this.mTmpInsets);
                rect.set(this.mTmpInsets.left + this.mScreenEdgeInsets.x, this.mTmpInsets.top + this.mScreenEdgeInsets.y, (this.mDisplayInfo.logicalWidth - this.mTmpInsets.right) - this.mScreenEdgeInsets.x, (this.mDisplayInfo.logicalHeight - this.mTmpInsets.bottom) - this.mScreenEdgeInsets.y);
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
    }

    private Rect getMovementBounds(Rect rect) {
        Rect movementBounds;
        synchronized (this.mService.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                movementBounds = getMovementBounds(rect, true, true);
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
        return movementBounds;
    }

    private Rect getMovementBounds(Rect rect, boolean z, boolean z2) {
        Rect rect2;
        synchronized (this.mService.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                rect2 = new Rect();
                getInsetBounds(rect2);
                PipSnapAlgorithm pipSnapAlgorithm = this.mSnapAlgorithm;
                int i = 0;
                int i2 = (z && this.mIsImeShowing) ? this.mImeHeight : 0;
                if (z2 && this.mIsShelfShowing) {
                    i = this.mShelfHeight;
                }
                pipSnapAlgorithm.getMovementBounds(rect, rect2, rect2, Math.max(i2, i));
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
        return rect2;
    }

    private void applyMinimizedOffset(Rect rect, Rect rect2) {
        synchronized (this.mService.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                this.mTmpDisplaySize.set(this.mDisplayInfo.logicalWidth, this.mDisplayInfo.logicalHeight);
                this.mService.getStableInsetsLocked(this.mDisplayContent.getDisplayId(), this.mStableInsets);
                this.mSnapAlgorithm.applyMinimizedOffset(rect, rect2, this.mTmpDisplaySize, this.mStableInsets);
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
    }

    private float getSnapFraction(Rect rect) {
        return this.mSnapAlgorithm.getSnapFraction(rect, getMovementBounds(rect));
    }

    private int dpToPx(float f, DisplayMetrics displayMetrics) {
        return (int) TypedValue.applyDimension(1, f, displayMetrics);
    }

    void dump(String str, PrintWriter printWriter) {
        printWriter.println(str + "PinnedStackController");
        printWriter.print(str + "  defaultBounds=");
        getDefaultBounds(-1.0f).printShortString(printWriter);
        printWriter.println();
        this.mService.getStackBounds(2, 1, this.mTmpRect);
        printWriter.print(str + "  movementBounds=");
        getMovementBounds(this.mTmpRect).printShortString(printWriter);
        printWriter.println();
        printWriter.println(str + "  mIsImeShowing=" + this.mIsImeShowing);
        printWriter.println(str + "  mImeHeight=" + this.mImeHeight);
        printWriter.println(str + "  mIsShelfShowing=" + this.mIsShelfShowing);
        printWriter.println(str + "  mShelfHeight=" + this.mShelfHeight);
        printWriter.println(str + "  mReentrySnapFraction=" + this.mReentrySnapFraction);
        printWriter.println(str + "  mIsMinimized=" + this.mIsMinimized);
        if (this.mActions.isEmpty()) {
            printWriter.println(str + "  mActions=[]");
        } else {
            printWriter.println(str + "  mActions=[");
            for (int i = 0; i < this.mActions.size(); i++) {
                RemoteAction remoteAction = this.mActions.get(i);
                printWriter.print(str + "    Action[" + i + "]: ");
                remoteAction.dump(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS, printWriter);
            }
            printWriter.println(str + "  ]");
        }
        printWriter.println(str + " mDisplayInfo=" + this.mDisplayInfo);
    }

    void writeToProto(ProtoOutputStream protoOutputStream, long j) {
        long jStart = protoOutputStream.start(j);
        getDefaultBounds(-1.0f).writeToProto(protoOutputStream, 1146756268033L);
        this.mService.getStackBounds(2, 1, this.mTmpRect);
        getMovementBounds(this.mTmpRect).writeToProto(protoOutputStream, 1146756268034L);
        protoOutputStream.end(jStart);
    }
}
