package com.android.server.wm;

import android.R;
import android.app.ActivityManager;
import android.app.ActivityThread;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.GraphicBuffer;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.SystemClock;
import android.util.MergedConfiguration;
import android.util.Slog;
import android.view.DisplayCutout;
import android.view.IWindowSession;
import android.view.InputChannel;
import android.view.Surface;
import android.view.SurfaceControl;
import android.view.SurfaceSession;
import android.view.WindowManager;
import android.view.WindowManagerGlobal;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.policy.DecorView;
import com.android.internal.view.BaseIWindow;
import com.android.server.policy.WindowManagerPolicy;
import java.util.Objects;

class TaskSnapshotSurface implements WindowManagerPolicy.StartingSurface {
    private static final int FLAG_INHERIT_EXCLUDES = 830922808;
    private static final int MSG_REPORT_DRAW = 0;
    private static final int PRIVATE_FLAG_INHERITS = 131072;
    private static final long SIZE_MISMATCH_MINIMUM_TIME_MS = 450;
    private static final String TAG = "WindowManager";
    private static final String TITLE_FORMAT = "SnapshotStartingWindow for taskId=%s";
    private static Handler sHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message message) {
            boolean z;
            if (message.what == 0) {
                TaskSnapshotSurface taskSnapshotSurface = (TaskSnapshotSurface) message.obj;
                synchronized (taskSnapshotSurface.mService.mWindowMap) {
                    try {
                        WindowManagerService.boostPriorityForLockedSection();
                        z = taskSnapshotSurface.mHasDrawn;
                    } catch (Throwable th) {
                        WindowManagerService.resetPriorityAfterLockedSection();
                        throw th;
                    }
                }
                WindowManagerService.resetPriorityAfterLockedSection();
                if (z) {
                    taskSnapshotSurface.reportDrawn();
                }
            }
        }
    };
    private SurfaceControl mChildSurfaceControl;
    private final Handler mHandler;
    private boolean mHasDrawn;
    private final int mOrientationOnCreation;
    private final WindowManagerService mService;
    private long mShownTime;
    private boolean mSizeMismatch;
    private ActivityManager.TaskSnapshot mSnapshot;
    private final int mStatusBarColor;
    private final Surface mSurface;

    @VisibleForTesting
    final SystemBarBackgroundPainter mSystemBarBackgroundPainter;
    private final Rect mTaskBounds;
    private final CharSequence mTitle;
    private final Window mWindow;
    private final Rect mStableInsets = new Rect();
    private final Rect mContentInsets = new Rect();
    private final Rect mFrame = new Rect();
    private final Paint mBackgroundPaint = new Paint();
    private final IWindowSession mSession = WindowManagerGlobal.getWindowSession();

    static TaskSnapshotSurface create(WindowManagerService windowManagerService, AppWindowToken appWindowToken, ActivityManager.TaskSnapshot taskSnapshot) {
        int i;
        int navigationBarColor;
        int i2;
        int i3;
        int i4;
        Rect rect;
        Rect rect2;
        Rect rect3;
        MergedConfiguration mergedConfiguration;
        Rect rect4;
        int i5;
        DisplayCutout.ParcelableWrapper parcelableWrapper;
        Rect rect5;
        TaskSnapshotSurface taskSnapshotSurface;
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
        Window window = new Window();
        IWindowSession windowSession = WindowManagerGlobal.getWindowSession();
        window.setSession(windowSession);
        Surface surface = new Surface();
        Rect rect6 = new Rect();
        DisplayCutout.ParcelableWrapper parcelableWrapper2 = new DisplayCutout.ParcelableWrapper();
        Rect rect7 = new Rect();
        Rect rect8 = new Rect();
        Rect rect9 = new Rect();
        MergedConfiguration mergedConfiguration2 = new MergedConfiguration();
        synchronized (windowManagerService.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                WindowState windowStateFindMainWindow = appWindowToken.findMainWindow();
                Task task = appWindowToken.getTask();
                if (task == null) {
                    Slog.w("WindowManager", "TaskSnapshotSurface.create: Failed to find task for token=" + appWindowToken);
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return null;
                }
                AppWindowToken topFullscreenAppToken = appWindowToken.getTask().getTopFullscreenAppToken();
                if (topFullscreenAppToken == null) {
                    Slog.w("WindowManager", "TaskSnapshotSurface.create: Failed to find top fullscreen for task=" + task);
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return null;
                }
                WindowState topFullscreenWindow = topFullscreenAppToken.getTopFullscreenWindow();
                if (windowStateFindMainWindow != null && topFullscreenWindow != null) {
                    int systemUiVisibility = topFullscreenWindow.getSystemUiVisibility();
                    int i6 = topFullscreenWindow.getAttrs().flags;
                    int i7 = topFullscreenWindow.getAttrs().privateFlags;
                    layoutParams.packageName = windowStateFindMainWindow.getAttrs().packageName;
                    layoutParams.windowAnimations = windowStateFindMainWindow.getAttrs().windowAnimations;
                    layoutParams.dimAmount = windowStateFindMainWindow.getAttrs().dimAmount;
                    layoutParams.type = 3;
                    layoutParams.format = taskSnapshot.getSnapshot().getFormat();
                    layoutParams.flags = ((-830922809) & i6) | 8 | 16;
                    layoutParams.privateFlags = 131072 & i7;
                    layoutParams.token = appWindowToken.token;
                    layoutParams.width = -1;
                    layoutParams.height = -1;
                    layoutParams.systemUiVisibility = systemUiVisibility;
                    layoutParams.setTitle(String.format(TITLE_FORMAT, Integer.valueOf(task.mTaskId)));
                    ActivityManager.TaskDescription taskDescription = task.getTaskDescription();
                    if (taskDescription != null) {
                        int backgroundColor = taskDescription.getBackgroundColor();
                        int statusBarColor = taskDescription.getStatusBarColor();
                        navigationBarColor = taskDescription.getNavigationBarColor();
                        i2 = backgroundColor;
                        i = statusBarColor;
                    } else {
                        i = 0;
                        navigationBarColor = 0;
                        i2 = -1;
                    }
                    Rect rect10 = new Rect();
                    task.getBounds(rect10);
                    int i8 = topFullscreenWindow.getConfiguration().orientation;
                    WindowManagerService.resetPriorityAfterLockedSection();
                    try {
                        i3 = i8;
                        i5 = i6;
                        i4 = i7;
                        mergedConfiguration = mergedConfiguration2;
                        rect = rect10;
                        rect4 = rect9;
                        rect2 = rect8;
                        rect3 = rect7;
                        parcelableWrapper = parcelableWrapper2;
                        rect5 = rect6;
                        try {
                            int iAddToDisplay = windowSession.addToDisplay(window, window.mSeq, layoutParams, 8, appWindowToken.getDisplayContent().getDisplayId(), rect7, rect6, rect6, rect6, parcelableWrapper2, (InputChannel) null);
                            if (iAddToDisplay < 0) {
                                Slog.w("WindowManager", "Failed to add snapshot starting window res=" + iAddToDisplay);
                                return null;
                            }
                        } catch (RemoteException e) {
                        }
                    } catch (RemoteException e2) {
                        i3 = i8;
                        i4 = i7;
                        rect = rect10;
                        rect2 = rect8;
                        rect3 = rect7;
                        mergedConfiguration = mergedConfiguration2;
                        rect4 = rect9;
                        i5 = i6;
                        parcelableWrapper = parcelableWrapper2;
                        rect5 = rect6;
                    }
                    TaskSnapshotSurface taskSnapshotSurface2 = new TaskSnapshotSurface(windowManagerService, window, surface, taskSnapshot, layoutParams.getTitle(), i2, i, navigationBarColor, systemUiVisibility, i5, i4, rect, i3);
                    window.setOuter(taskSnapshotSurface2);
                    try {
                        taskSnapshotSurface = taskSnapshotSurface2;
                        try {
                            windowSession.relayout(window, window.mSeq, layoutParams, -1, -1, 0, 0, -1L, rect3, rect5, rect2, rect5, rect4, rect5, rect5, parcelableWrapper, mergedConfiguration, surface);
                        } catch (RemoteException e3) {
                        }
                    } catch (RemoteException e4) {
                        taskSnapshotSurface = taskSnapshotSurface2;
                    }
                    TaskSnapshotSurface taskSnapshotSurface3 = taskSnapshotSurface;
                    taskSnapshotSurface3.setFrames(rect3, rect2, rect4);
                    taskSnapshotSurface3.drawSnapshot();
                    return taskSnapshotSurface3;
                }
                Slog.w("WindowManager", "TaskSnapshotSurface.create: Failed to find main window for token=" + appWindowToken);
                WindowManagerService.resetPriorityAfterLockedSection();
                return null;
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    @VisibleForTesting
    TaskSnapshotSurface(WindowManagerService windowManagerService, Window window, Surface surface, ActivityManager.TaskSnapshot taskSnapshot, CharSequence charSequence, int i, int i2, int i3, int i4, int i5, int i6, Rect rect, int i7) {
        this.mService = windowManagerService;
        this.mHandler = new Handler(this.mService.mH.getLooper());
        this.mWindow = window;
        this.mSurface = surface;
        this.mSnapshot = taskSnapshot;
        this.mTitle = charSequence;
        this.mBackgroundPaint.setColor(i != 0 ? i : -1);
        this.mTaskBounds = rect;
        this.mSystemBarBackgroundPainter = new SystemBarBackgroundPainter(i5, i6, i4, i2, i3);
        this.mStatusBarColor = i2;
        this.mOrientationOnCreation = i7;
    }

    @Override
    public void remove() {
        synchronized (this.mService.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                long jUptimeMillis = SystemClock.uptimeMillis();
                if (this.mSizeMismatch && jUptimeMillis - this.mShownTime < SIZE_MISMATCH_MINIMUM_TIME_MS) {
                    this.mHandler.postAtTime(new $$Lambda$OevXHSXgaSE351ZqRnMoA024MM(this), this.mShownTime + SIZE_MISMATCH_MINIMUM_TIME_MS);
                    if (WindowManagerDebugConfig.DEBUG_STARTING_WINDOW) {
                        Slog.v("WindowManager", "Defer removing snapshot surface in " + (jUptimeMillis - this.mShownTime) + "ms");
                    }
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return;
                }
                WindowManagerService.resetPriorityAfterLockedSection();
                try {
                    if (WindowManagerDebugConfig.DEBUG_STARTING_WINDOW) {
                        Slog.v("WindowManager", "Removing snapshot surface");
                    }
                    this.mSession.remove(this.mWindow);
                } catch (RemoteException e) {
                }
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    @VisibleForTesting
    void setFrames(Rect rect, Rect rect2, Rect rect3) {
        this.mFrame.set(rect);
        this.mContentInsets.set(rect2);
        this.mStableInsets.set(rect3);
        this.mSizeMismatch = (this.mFrame.width() == this.mSnapshot.getSnapshot().getWidth() && this.mFrame.height() == this.mSnapshot.getSnapshot().getHeight()) ? false : true;
        this.mSystemBarBackgroundPainter.setInsets(rect2, rect3);
    }

    private void drawSnapshot() {
        GraphicBuffer snapshot = this.mSnapshot.getSnapshot();
        if (WindowManagerDebugConfig.DEBUG_STARTING_WINDOW) {
            Slog.v("WindowManager", "Drawing snapshot surface sizeMismatch=" + this.mSizeMismatch);
        }
        if (this.mSizeMismatch) {
            drawSizeMismatchSnapshot(snapshot);
        } else {
            drawSizeMatchSnapshot(snapshot);
        }
        synchronized (this.mService.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                this.mShownTime = SystemClock.uptimeMillis();
                this.mHasDrawn = true;
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
        reportDrawn();
        this.mSnapshot = null;
    }

    private void drawSizeMatchSnapshot(GraphicBuffer graphicBuffer) {
        this.mSurface.attachAndQueueBuffer(graphicBuffer);
        this.mSurface.release();
    }

    private void drawSizeMismatchSnapshot(GraphicBuffer graphicBuffer) {
        this.mChildSurfaceControl = new SurfaceControl.Builder(new SurfaceSession(this.mSurface)).setName(((Object) this.mTitle) + " - task-snapshot-surface").setSize(graphicBuffer.getWidth(), graphicBuffer.getHeight()).setFormat(graphicBuffer.getFormat()).build();
        Surface surface = new Surface();
        surface.copyFrom(this.mChildSurfaceControl);
        Rect rectCalculateSnapshotCrop = calculateSnapshotCrop();
        Rect rectCalculateSnapshotFrame = calculateSnapshotFrame(rectCalculateSnapshotCrop);
        SurfaceControl.openTransaction();
        try {
            this.mChildSurfaceControl.show();
            this.mChildSurfaceControl.setWindowCrop(rectCalculateSnapshotCrop);
            this.mChildSurfaceControl.setPosition(rectCalculateSnapshotFrame.left, rectCalculateSnapshotFrame.top);
            float scale = 1.0f / this.mSnapshot.getScale();
            this.mChildSurfaceControl.setMatrix(scale, 0.0f, 0.0f, scale);
            SurfaceControl.closeTransaction();
            surface.attachAndQueueBuffer(graphicBuffer);
            surface.release();
            Canvas canvasLockCanvas = this.mSurface.lockCanvas(null);
            drawBackgroundAndBars(canvasLockCanvas, rectCalculateSnapshotFrame);
            this.mSurface.unlockCanvasAndPost(canvasLockCanvas);
            this.mSurface.release();
        } catch (Throwable th) {
            SurfaceControl.closeTransaction();
            throw th;
        }
    }

    @VisibleForTesting
    Rect calculateSnapshotCrop() {
        Rect rect = new Rect();
        rect.set(0, 0, this.mSnapshot.getSnapshot().getWidth(), this.mSnapshot.getSnapshot().getHeight());
        Rect contentInsets = this.mSnapshot.getContentInsets();
        rect.inset((int) (contentInsets.left * this.mSnapshot.getScale()), this.mTaskBounds.top == 0 && this.mFrame.top == 0 ? 0 : (int) (contentInsets.top * this.mSnapshot.getScale()), (int) (contentInsets.right * this.mSnapshot.getScale()), (int) (contentInsets.bottom * this.mSnapshot.getScale()));
        return rect;
    }

    @VisibleForTesting
    Rect calculateSnapshotFrame(Rect rect) {
        Rect rect2 = new Rect(rect);
        float scale = this.mSnapshot.getScale();
        rect2.scale(1.0f / scale);
        rect2.offsetTo((int) ((-rect.left) / scale), (int) ((-rect.top) / scale));
        rect2.offset(DecorView.getColorViewLeftInset(this.mStableInsets.left, this.mContentInsets.left), 0);
        return rect2;
    }

    @VisibleForTesting
    void drawBackgroundAndBars(Canvas canvas, Rect rect) {
        int statusBarColorViewHeight = this.mSystemBarBackgroundPainter.getStatusBarColorViewHeight();
        boolean z = canvas.getWidth() > rect.right;
        boolean z2 = canvas.getHeight() > rect.bottom;
        if (z) {
            canvas.drawRect(rect.right, Color.alpha(this.mStatusBarColor) == 255 ? statusBarColorViewHeight : 0.0f, canvas.getWidth(), z2 ? rect.bottom : canvas.getHeight(), this.mBackgroundPaint);
        }
        if (z2) {
            canvas.drawRect(0.0f, rect.bottom, canvas.getWidth(), canvas.getHeight(), this.mBackgroundPaint);
        }
        this.mSystemBarBackgroundPainter.drawDecors(canvas, rect);
    }

    private void reportDrawn() {
        try {
            this.mSession.finishDrawing(this.mWindow);
        } catch (RemoteException e) {
        }
    }

    @VisibleForTesting
    static class Window extends BaseIWindow {
        private TaskSnapshotSurface mOuter;

        Window() {
        }

        public void setOuter(TaskSnapshotSurface taskSnapshotSurface) {
            this.mOuter = taskSnapshotSurface;
        }

        public void resized(Rect rect, Rect rect2, Rect rect3, Rect rect4, Rect rect5, Rect rect6, boolean z, MergedConfiguration mergedConfiguration, Rect rect7, boolean z2, boolean z3, int i, DisplayCutout.ParcelableWrapper parcelableWrapper) {
            if (mergedConfiguration != null && this.mOuter != null && this.mOuter.mOrientationOnCreation != mergedConfiguration.getMergedConfiguration().orientation) {
                Handler handler = TaskSnapshotSurface.sHandler;
                TaskSnapshotSurface taskSnapshotSurface = this.mOuter;
                Objects.requireNonNull(taskSnapshotSurface);
                handler.post(new $$Lambda$OevXHSXgaSE351ZqRnMoA024MM(taskSnapshotSurface));
            }
            if (z) {
                TaskSnapshotSurface.sHandler.obtainMessage(0, this.mOuter).sendToTarget();
            }
        }
    }

    static class SystemBarBackgroundPainter {
        private final int mNavigationBarColor;
        private final int mStatusBarColor;
        private final int mSysUiVis;
        private final int mWindowFlags;
        private final int mWindowPrivateFlags;
        private final Rect mContentInsets = new Rect();
        private final Rect mStableInsets = new Rect();
        private final Paint mStatusBarPaint = new Paint();
        private final Paint mNavigationBarPaint = new Paint();

        SystemBarBackgroundPainter(int i, int i2, int i3, int i4, int i5) {
            this.mWindowFlags = i;
            this.mWindowPrivateFlags = i2;
            this.mSysUiVis = i3;
            this.mStatusBarColor = DecorView.calculateStatusBarColor(i, ActivityThread.currentActivityThread().getSystemUiContext().getColor(R.color.car_card_ripple_background_light), i4);
            this.mNavigationBarColor = i5;
            this.mStatusBarPaint.setColor(this.mStatusBarColor);
            this.mNavigationBarPaint.setColor(i5);
        }

        void setInsets(Rect rect, Rect rect2) {
            this.mContentInsets.set(rect);
            this.mStableInsets.set(rect2);
        }

        int getStatusBarColorViewHeight() {
            if (DecorView.STATUS_BAR_COLOR_VIEW_ATTRIBUTES.isVisible(this.mSysUiVis, this.mStatusBarColor, this.mWindowFlags, (this.mWindowPrivateFlags & 131072) != 0)) {
                return DecorView.getColorViewTopInset(this.mStableInsets.top, this.mContentInsets.top);
            }
            return 0;
        }

        private boolean isNavigationBarColorViewVisible() {
            return DecorView.NAVIGATION_BAR_COLOR_VIEW_ATTRIBUTES.isVisible(this.mSysUiVis, this.mNavigationBarColor, this.mWindowFlags, false);
        }

        void drawDecors(Canvas canvas, Rect rect) {
            drawStatusBarBackground(canvas, rect, getStatusBarColorViewHeight());
            drawNavigationBarBackground(canvas);
        }

        @VisibleForTesting
        void drawStatusBarBackground(Canvas canvas, Rect rect, int i) {
            if (i > 0 && Color.alpha(this.mStatusBarColor) != 0) {
                if (rect == null || canvas.getWidth() > rect.right) {
                    canvas.drawRect(rect != null ? rect.right : 0, 0.0f, canvas.getWidth() - DecorView.getColorViewRightInset(this.mStableInsets.right, this.mContentInsets.right), i, this.mStatusBarPaint);
                }
            }
        }

        @VisibleForTesting
        void drawNavigationBarBackground(Canvas canvas) {
            Rect rect = new Rect();
            DecorView.getNavigationBarRect(canvas.getWidth(), canvas.getHeight(), this.mStableInsets, this.mContentInsets, rect);
            if (isNavigationBarColorViewVisible() && Color.alpha(this.mNavigationBarColor) != 0 && !rect.isEmpty()) {
                canvas.drawRect(rect, this.mNavigationBarPaint);
            }
        }
    }
}
