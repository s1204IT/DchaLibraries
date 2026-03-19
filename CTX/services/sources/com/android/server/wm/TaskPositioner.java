package com.android.server.wm;

import android.graphics.Point;
import android.graphics.Rect;
import android.os.Looper;
import android.os.Process;
import android.os.RemoteException;
import android.os.Trace;
import android.util.DisplayMetrics;
import android.util.Slog;
import android.view.BatchedInputEventReceiver;
import android.view.Choreographer;
import android.view.Display;
import android.view.InputChannel;
import android.view.InputEvent;
import android.view.MotionEvent;
import com.android.internal.annotations.VisibleForTesting;
import com.android.server.input.InputApplicationHandle;
import com.android.server.input.InputWindowHandle;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

class TaskPositioner {
    private static final int CTRL_BOTTOM = 8;
    private static final int CTRL_LEFT = 1;
    private static final int CTRL_NONE = 0;
    private static final int CTRL_RIGHT = 2;
    private static final int CTRL_TOP = 4;
    private static final boolean DEBUG_ORIENTATION_VIOLATIONS = false;

    @VisibleForTesting
    static final float MIN_ASPECT = 1.2f;
    public static final float RESIZING_HINT_ALPHA = 0.5f;
    public static final int RESIZING_HINT_DURATION_MS = 0;
    static final int SIDE_MARGIN_DIP = 100;
    private static final String TAG = "WindowManager";
    private static final String TAG_LOCAL = "TaskPositioner";
    private static Factory sFactory;
    InputChannel mClientChannel;
    private Display mDisplay;
    InputApplicationHandle mDragApplicationHandle;
    InputWindowHandle mDragWindowHandle;
    private WindowPositionerEventReceiver mInputEventReceiver;
    private int mMinVisibleHeight;
    private int mMinVisibleWidth;
    private boolean mPreserveOrientation;
    private boolean mResizing;
    InputChannel mServerChannel;
    private final WindowManagerService mService;
    private int mSideMargin;
    private float mStartDragX;
    private float mStartDragY;
    private boolean mStartOrientationWasLandscape;
    private Task mTask;
    private final DisplayMetrics mDisplayMetrics = new DisplayMetrics();
    private Rect mTmpRect = new Rect();
    private final Rect mWindowOriginalBounds = new Rect();
    private final Rect mWindowDragBounds = new Rect();
    private final Point mMaxVisibleSize = new Point();
    private int mCtrlType = 0;
    private boolean mDragEnded = false;

    @Retention(RetentionPolicy.SOURCE)
    @interface CtrlType {
    }

    private final class WindowPositionerEventReceiver extends BatchedInputEventReceiver {
        public WindowPositionerEventReceiver(InputChannel inputChannel, Looper looper, Choreographer choreographer) {
            super(inputChannel, looper, choreographer);
        }

        public void onInputEvent(InputEvent inputEvent, int i) {
            if (!(inputEvent instanceof MotionEvent) || (inputEvent.getSource() & 2) == 0) {
                return;
            }
            MotionEvent motionEvent = (MotionEvent) inputEvent;
            try {
                try {
                    if (TaskPositioner.this.mDragEnded) {
                        finishInputEvent(inputEvent, true);
                        return;
                    }
                    float rawX = motionEvent.getRawX();
                    float rawY = motionEvent.getRawY();
                    switch (motionEvent.getAction()) {
                        case 0:
                            if (WindowManagerDebugConfig.DEBUG_TASK_POSITIONING) {
                                Slog.w("WindowManager", "ACTION_DOWN @ {" + rawX + ", " + rawY + "}");
                            }
                            break;
                        case 1:
                            if (WindowManagerDebugConfig.DEBUG_TASK_POSITIONING) {
                                Slog.w("WindowManager", "ACTION_UP @ {" + rawX + ", " + rawY + "}");
                            }
                            TaskPositioner.this.mDragEnded = true;
                            break;
                        case 2:
                            if (WindowManagerDebugConfig.DEBUG_TASK_POSITIONING) {
                                Slog.w("WindowManager", "ACTION_MOVE @ {" + rawX + ", " + rawY + "}");
                            }
                            synchronized (TaskPositioner.this.mService.mWindowMap) {
                                try {
                                    WindowManagerService.boostPriorityForLockedSection();
                                    TaskPositioner.this.mDragEnded = TaskPositioner.this.notifyMoveLocked(rawX, rawY);
                                    TaskPositioner.this.mTask.getDimBounds(TaskPositioner.this.mTmpRect);
                                } finally {
                                }
                                break;
                            }
                            WindowManagerService.resetPriorityAfterLockedSection();
                            if (!TaskPositioner.this.mTmpRect.equals(TaskPositioner.this.mWindowDragBounds)) {
                                Trace.traceBegin(32L, "wm.TaskPositioner.resizeTask");
                                try {
                                    TaskPositioner.this.mService.mActivityManager.resizeTask(TaskPositioner.this.mTask.mTaskId, TaskPositioner.this.mWindowDragBounds, 1);
                                    break;
                                } catch (RemoteException e) {
                                }
                                Trace.traceEnd(32L);
                            }
                            break;
                        case 3:
                            if (WindowManagerDebugConfig.DEBUG_TASK_POSITIONING) {
                                Slog.w("WindowManager", "ACTION_CANCEL @ {" + rawX + ", " + rawY + "}");
                            }
                            TaskPositioner.this.mDragEnded = true;
                            break;
                    }
                    if (TaskPositioner.this.mDragEnded) {
                        boolean z = TaskPositioner.this.mResizing;
                        synchronized (TaskPositioner.this.mService.mWindowMap) {
                            try {
                                WindowManagerService.boostPriorityForLockedSection();
                                TaskPositioner.this.endDragLocked();
                                TaskPositioner.this.mTask.getDimBounds(TaskPositioner.this.mTmpRect);
                            } finally {
                            }
                        }
                        WindowManagerService.resetPriorityAfterLockedSection();
                        if (z) {
                            try {
                                if (!TaskPositioner.this.mTmpRect.equals(TaskPositioner.this.mWindowDragBounds)) {
                                    TaskPositioner.this.mService.mActivityManager.resizeTask(TaskPositioner.this.mTask.mTaskId, TaskPositioner.this.mWindowDragBounds, 3);
                                }
                            } catch (RemoteException e2) {
                            }
                        }
                        TaskPositioner.this.mService.mTaskPositioningController.finishTaskPositioning();
                    }
                    finishInputEvent(inputEvent, true);
                } catch (Throwable th) {
                    finishInputEvent(inputEvent, false);
                    throw th;
                }
            } catch (Exception e3) {
                Slog.e("WindowManager", "Exception caught by drag handleMotion", e3);
                finishInputEvent(inputEvent, false);
            }
        }
    }

    TaskPositioner(WindowManagerService windowManagerService) {
        this.mService = windowManagerService;
    }

    @VisibleForTesting
    Rect getWindowDragBounds() {
        return this.mWindowDragBounds;
    }

    void register(DisplayContent displayContent) {
        Display display = displayContent.getDisplay();
        if (WindowManagerDebugConfig.DEBUG_TASK_POSITIONING) {
            Slog.d("WindowManager", "Registering task positioner");
        }
        if (this.mClientChannel != null) {
            Slog.e("WindowManager", "Task positioner already registered");
            return;
        }
        this.mDisplay = display;
        this.mDisplay.getMetrics(this.mDisplayMetrics);
        InputChannel[] inputChannelArrOpenInputChannelPair = InputChannel.openInputChannelPair("WindowManager");
        this.mServerChannel = inputChannelArrOpenInputChannelPair[0];
        this.mClientChannel = inputChannelArrOpenInputChannelPair[1];
        this.mService.mInputManager.registerInputChannel(this.mServerChannel, null);
        this.mInputEventReceiver = new WindowPositionerEventReceiver(this.mClientChannel, this.mService.mAnimationHandler.getLooper(), this.mService.mAnimator.getChoreographer());
        this.mDragApplicationHandle = new InputApplicationHandle(null);
        this.mDragApplicationHandle.name = "WindowManager";
        this.mDragApplicationHandle.dispatchingTimeoutNanos = 30000000000L;
        this.mDragWindowHandle = new InputWindowHandle(this.mDragApplicationHandle, null, null, this.mDisplay.getDisplayId());
        this.mDragWindowHandle.name = "WindowManager";
        this.mDragWindowHandle.inputChannel = this.mServerChannel;
        this.mDragWindowHandle.layer = this.mService.getDragLayerLocked();
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
        Point point = new Point();
        this.mDisplay.getRealSize(point);
        this.mDragWindowHandle.frameRight = point.x;
        this.mDragWindowHandle.frameBottom = point.y;
        if (WindowManagerDebugConfig.DEBUG_ORIENTATION) {
            Slog.d("WindowManager", "Pausing rotation during re-position");
        }
        this.mService.pauseRotationLocked();
        this.mSideMargin = WindowManagerService.dipToPixel(100, this.mDisplayMetrics);
        this.mMinVisibleWidth = WindowManagerService.dipToPixel(48, this.mDisplayMetrics);
        this.mMinVisibleHeight = WindowManagerService.dipToPixel(32, this.mDisplayMetrics);
        this.mDisplay.getRealSize(this.mMaxVisibleSize);
        this.mDragEnded = false;
    }

    void unregister() {
        if (WindowManagerDebugConfig.DEBUG_TASK_POSITIONING) {
            Slog.d("WindowManager", "Unregistering task positioner");
        }
        if (this.mClientChannel == null) {
            Slog.e("WindowManager", "Task positioner not registered");
            return;
        }
        this.mService.mInputManager.unregisterInputChannel(this.mServerChannel);
        this.mInputEventReceiver.dispose();
        this.mInputEventReceiver = null;
        this.mClientChannel.dispose();
        this.mServerChannel.dispose();
        this.mClientChannel = null;
        this.mServerChannel = null;
        this.mDragWindowHandle = null;
        this.mDragApplicationHandle = null;
        this.mDisplay = null;
        this.mDragEnded = true;
        if (WindowManagerDebugConfig.DEBUG_ORIENTATION) {
            Slog.d("WindowManager", "Resuming rotation after re-position");
        }
        this.mService.resumeRotationLocked();
    }

    void startDrag(WindowState windowState, boolean z, boolean z2, float f, float f2) {
        if (WindowManagerDebugConfig.DEBUG_TASK_POSITIONING) {
            Slog.d("WindowManager", "startDrag: win=" + windowState + ", resize=" + z + ", preserveOrientation=" + z2 + ", {" + f + ", " + f2 + "}");
        }
        this.mTask = windowState.getTask();
        this.mTask.getDimBounds(this.mTmpRect);
        startDrag(z, z2, f, f2, this.mTmpRect);
    }

    @VisibleForTesting
    void startDrag(boolean z, boolean z2, float f, float f2, final Rect rect) {
        boolean z3;
        this.mCtrlType = 0;
        this.mStartDragX = f;
        this.mStartDragY = f2;
        this.mPreserveOrientation = z2;
        boolean z4 = true;
        if (z) {
            if (f < rect.left) {
                this.mCtrlType |= 1;
            }
            if (f > rect.right) {
                this.mCtrlType |= 2;
            }
            if (f2 < rect.top) {
                this.mCtrlType |= 4;
            }
            if (f2 > rect.bottom) {
                this.mCtrlType |= 8;
            }
            if (this.mCtrlType == 0) {
                z3 = false;
            } else {
                z3 = true;
            }
            this.mResizing = z3;
        }
        if (rect.width() < rect.height()) {
            z4 = false;
        }
        this.mStartOrientationWasLandscape = z4;
        this.mWindowOriginalBounds.set(rect);
        if (this.mResizing) {
            synchronized (this.mService.mWindowMap) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    notifyMoveLocked(f, f2);
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
            this.mService.mH.post(new Runnable() {
                @Override
                public final void run() {
                    TaskPositioner taskPositioner = this.f$0;
                    taskPositioner.mService.mActivityManager.resizeTask(taskPositioner.mTask.mTaskId, rect, 3);
                }
            });
        }
        this.mWindowDragBounds.set(rect);
    }

    private void endDragLocked() {
        this.mResizing = false;
        this.mTask.setDragResizing(false, 0);
    }

    private boolean notifyMoveLocked(float f, float f2) {
        if (WindowManagerDebugConfig.DEBUG_TASK_POSITIONING) {
            Slog.d("WindowManager", "notifyMoveLocked: {" + f + "," + f2 + "}");
        }
        if (this.mCtrlType != 0) {
            resizeDrag(f, f2);
            this.mTask.setDragResizing(true, 0);
            return false;
        }
        this.mTask.mStack.getDimBounds(this.mTmpRect);
        int iMin = (int) f;
        int iMin2 = (int) f2;
        if (!this.mTmpRect.contains(iMin, iMin2)) {
            iMin = Math.min(Math.max(iMin, this.mTmpRect.left), this.mTmpRect.right);
            iMin2 = Math.min(Math.max(iMin2, this.mTmpRect.top), this.mTmpRect.bottom);
        }
        updateWindowDragBounds(iMin, iMin2, this.mTmpRect);
        return false;
    }

    @VisibleForTesting
    void resizeDrag(float f, float f2) {
        int iMax;
        int iMax2;
        int iMin;
        int iMin2;
        int iMax3;
        int iMax4;
        int iRound = Math.round(f - this.mStartDragX);
        int iRound2 = Math.round(f2 - this.mStartDragY);
        int i = this.mWindowOriginalBounds.left;
        int i2 = this.mWindowOriginalBounds.top;
        int i3 = this.mWindowOriginalBounds.right;
        int i4 = this.mWindowOriginalBounds.bottom;
        if (this.mPreserveOrientation) {
            boolean z = this.mStartOrientationWasLandscape;
        }
        int i5 = i3 - i;
        int i6 = i4 - i2;
        if ((this.mCtrlType & 1) != 0) {
            iMax = Math.max(this.mMinVisibleWidth, i5 - iRound);
        } else if ((this.mCtrlType & 2) != 0) {
            iMax = Math.max(this.mMinVisibleWidth, iRound + i5);
        } else {
            iMax = i5;
        }
        if ((this.mCtrlType & 4) != 0) {
            iMax2 = Math.max(this.mMinVisibleHeight, i6 - iRound2);
        } else if ((this.mCtrlType & 8) != 0) {
            iMax2 = Math.max(this.mMinVisibleHeight, iRound2 + i6);
        } else {
            iMax2 = i6;
        }
        float f3 = iMax / iMax2;
        if (!this.mPreserveOrientation || ((!this.mStartOrientationWasLandscape || f3 >= MIN_ASPECT) && (this.mStartOrientationWasLandscape || f3 <= 0.8333333002196431d))) {
            iMin = iMax;
            iMin2 = iMax2;
        } else {
            if (this.mStartOrientationWasLandscape) {
                iMax3 = Math.max(this.mMinVisibleWidth, Math.min(this.mMaxVisibleSize.x, iMax));
                iMin2 = Math.min(iMax2, Math.round(iMax3 / MIN_ASPECT));
                if (iMin2 < this.mMinVisibleHeight) {
                    iMin2 = this.mMinVisibleHeight;
                    iMax3 = Math.max(this.mMinVisibleWidth, Math.min(this.mMaxVisibleSize.x, Math.round(iMin2 * MIN_ASPECT)));
                }
                iMax4 = Math.max(this.mMinVisibleHeight, Math.min(this.mMaxVisibleSize.y, iMax2));
                iMin = Math.max(iMax, Math.round(iMax4 * MIN_ASPECT));
                if (iMin < this.mMinVisibleWidth) {
                    iMin = this.mMinVisibleWidth;
                    iMax4 = Math.max(this.mMinVisibleHeight, Math.min(this.mMaxVisibleSize.y, Math.round(iMin / MIN_ASPECT)));
                }
            } else {
                iMax3 = Math.max(this.mMinVisibleWidth, Math.min(this.mMaxVisibleSize.x, iMax));
                int iMax5 = Math.max(iMax2, Math.round(iMax3 * MIN_ASPECT));
                if (iMax5 < this.mMinVisibleHeight) {
                    int i7 = this.mMinVisibleHeight;
                    iMin2 = i7;
                    iMax3 = Math.max(this.mMinVisibleWidth, Math.min(this.mMaxVisibleSize.x, Math.round(i7 / MIN_ASPECT)));
                } else {
                    iMin2 = iMax5;
                }
                iMax4 = Math.max(this.mMinVisibleHeight, Math.min(this.mMaxVisibleSize.y, iMax2));
                iMin = Math.min(iMax, Math.round(iMax4 / MIN_ASPECT));
                if (iMin < this.mMinVisibleWidth) {
                    iMin = this.mMinVisibleWidth;
                    iMax4 = Math.max(this.mMinVisibleHeight, Math.min(this.mMaxVisibleSize.y, Math.round(iMin * MIN_ASPECT)));
                }
            }
            if ((iMax > i5 || iMax2 > i6) == (iMax3 * iMin2 > iMin * iMax4)) {
                iMin = iMax3;
            } else {
                iMin2 = iMax4;
            }
        }
        updateDraggedBounds(i, i2, i3, i4, iMin, iMin2);
    }

    void updateDraggedBounds(int i, int i2, int i3, int i4, int i5, int i6) {
        if ((this.mCtrlType & 1) != 0) {
            i = i3 - i5;
        } else {
            i3 = i + i5;
        }
        if ((this.mCtrlType & 4) != 0) {
            i2 = i4 - i6;
        } else {
            i4 = i2 + i6;
        }
        this.mWindowDragBounds.set(i, i2, i3, i4);
        checkBoundsForOrientationViolations(this.mWindowDragBounds);
    }

    private void checkBoundsForOrientationViolations(Rect rect) {
    }

    private void updateWindowDragBounds(int i, int i2, Rect rect) {
        int iRound = Math.round(i - this.mStartDragX);
        int iRound2 = Math.round(i2 - this.mStartDragY);
        this.mWindowDragBounds.set(this.mWindowOriginalBounds);
        this.mWindowDragBounds.offsetTo(Math.min(Math.max(this.mWindowOriginalBounds.left + iRound, (rect.left + this.mMinVisibleWidth) - this.mWindowOriginalBounds.width()), rect.right - this.mMinVisibleWidth), Math.min(Math.max(this.mWindowOriginalBounds.top + iRound2, rect.top), rect.bottom - this.mMinVisibleHeight));
        if (WindowManagerDebugConfig.DEBUG_TASK_POSITIONING) {
            Slog.d("WindowManager", "updateWindowDragBounds: " + this.mWindowDragBounds);
        }
    }

    public String toShortString() {
        return "WindowManager";
    }

    static void setFactory(Factory factory) {
        sFactory = factory;
    }

    static TaskPositioner create(WindowManagerService windowManagerService) {
        if (sFactory == null) {
            sFactory = new Factory() {
            };
        }
        return sFactory.create(windowManagerService);
    }

    interface Factory {
        default TaskPositioner create(WindowManagerService windowManagerService) {
            return new TaskPositioner(windowManagerService);
        }
    }
}
