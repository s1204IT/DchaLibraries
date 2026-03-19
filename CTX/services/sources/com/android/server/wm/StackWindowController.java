package com.android.server.wm;

import android.app.WindowConfiguration;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Slog;
import android.util.SparseArray;
import android.view.DisplayCutout;
import android.view.DisplayInfo;
import com.android.internal.annotations.VisibleForTesting;
import com.mediatek.server.wm.WmsExt;
import java.lang.ref.WeakReference;

public class StackWindowController extends WindowContainerController<TaskStack, StackWindowListener> {
    private final H mHandler;
    private final int mStackId;
    private final Rect mTmpDisplayBounds;
    private final Rect mTmpNonDecorInsets;
    private final Rect mTmpRect;
    private final Rect mTmpStableInsets;

    @Override
    public void onOverrideConfigurationChanged(Configuration configuration) {
        super.onOverrideConfigurationChanged(configuration);
    }

    public StackWindowController(int i, StackWindowListener stackWindowListener, int i2, boolean z, Rect rect) {
        this(i, stackWindowListener, i2, z, rect, WindowManagerService.getInstance());
    }

    @VisibleForTesting
    public StackWindowController(int i, StackWindowListener stackWindowListener, int i2, boolean z, Rect rect, WindowManagerService windowManagerService) {
        super(stackWindowListener, windowManagerService);
        this.mTmpRect = new Rect();
        this.mTmpStableInsets = new Rect();
        this.mTmpNonDecorInsets = new Rect();
        this.mTmpDisplayBounds = new Rect();
        this.mStackId = i;
        this.mHandler = new H(new WeakReference(this), windowManagerService.mH.getLooper());
        synchronized (this.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                DisplayContent displayContent = this.mRoot.getDisplayContent(i2);
                if (displayContent == null) {
                    throw new IllegalArgumentException("Trying to add stackId=" + i + " to unknown displayId=" + i2);
                }
                displayContent.createStack(i, z, this);
                getRawBounds(rect);
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
    }

    @Override
    public void removeContainer() {
        synchronized (this.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                if (this.mContainer != 0) {
                    ((TaskStack) this.mContainer).removeIfPossible();
                    super.removeContainer();
                }
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
    }

    public void reparent(int i, Rect rect, boolean z) {
        synchronized (this.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                if (this.mContainer == 0) {
                    throw new IllegalArgumentException("Trying to move unknown stackId=" + this.mStackId + " to displayId=" + i);
                }
                DisplayContent displayContent = this.mRoot.getDisplayContent(i);
                if (displayContent == null) {
                    throw new IllegalArgumentException("Trying to move stackId=" + this.mStackId + " to unknown displayId=" + i);
                }
                displayContent.moveStackToDisplay((TaskStack) this.mContainer, z);
                getRawBounds(rect);
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
    }

    public void positionChildAt(TaskWindowContainerController taskWindowContainerController, int i) {
        synchronized (this.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                if (WindowManagerDebugConfig.DEBUG_STACK) {
                    Slog.i(WmsExt.TAG, "positionChildAt: positioning task=" + taskWindowContainerController + " at " + i);
                }
                if (taskWindowContainerController.mContainer == 0) {
                    if (WindowManagerDebugConfig.DEBUG_STACK) {
                        Slog.i(WmsExt.TAG, "positionChildAt: could not find task=" + this);
                    }
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return;
                }
                if (this.mContainer == 0) {
                    if (WindowManagerDebugConfig.DEBUG_STACK) {
                        Slog.i(WmsExt.TAG, "positionChildAt: could not find stack for task=" + this.mContainer);
                    }
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return;
                }
                ((Task) taskWindowContainerController.mContainer).positionAt(i);
                ((TaskStack) this.mContainer).getDisplayContent().layoutAndAssignWindowLayersIfNeeded();
                WindowManagerService.resetPriorityAfterLockedSection();
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    public void positionChildAtTop(TaskWindowContainerController taskWindowContainerController, boolean z) {
        if (taskWindowContainerController == null) {
            return;
        }
        synchronized (this.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                Task task = (Task) taskWindowContainerController.mContainer;
                if (task == null) {
                    Slog.e(WmsExt.TAG, "positionChildAtTop: task=" + taskWindowContainerController + " not found");
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return;
                }
                ((TaskStack) this.mContainer).positionChildAt(Integer.MAX_VALUE, task, z);
                if (this.mService.mAppTransition.isTransitionSet()) {
                    task.setSendingToBottom(false);
                }
                ((TaskStack) this.mContainer).getDisplayContent().layoutAndAssignWindowLayersIfNeeded();
                WindowManagerService.resetPriorityAfterLockedSection();
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    public void positionChildAtBottom(TaskWindowContainerController taskWindowContainerController, boolean z) {
        if (taskWindowContainerController == null) {
            return;
        }
        synchronized (this.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                Task task = (Task) taskWindowContainerController.mContainer;
                if (task == null) {
                    Slog.e(WmsExt.TAG, "positionChildAtBottom: task=" + taskWindowContainerController + " not found");
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return;
                }
                ((TaskStack) this.mContainer).positionChildAt(Integer.MIN_VALUE, task, z);
                if (this.mService.mAppTransition.isTransitionSet()) {
                    task.setSendingToBottom(true);
                }
                ((TaskStack) this.mContainer).getDisplayContent().layoutAndAssignWindowLayersIfNeeded();
                WindowManagerService.resetPriorityAfterLockedSection();
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    public void resize(Rect rect, SparseArray<Rect> sparseArray, SparseArray<Rect> sparseArray2) {
        synchronized (this.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                if (this.mContainer == 0) {
                    throw new IllegalArgumentException("resizeStack: stack " + this + " not found.");
                }
                ((TaskStack) this.mContainer).prepareFreezingTaskBounds();
                if (((TaskStack) this.mContainer).setBounds(rect, sparseArray, sparseArray2) && ((TaskStack) this.mContainer).isVisible()) {
                    ((TaskStack) this.mContainer).getDisplayContent().setLayoutNeeded();
                    this.mService.mWindowPlacerLocked.performSurfacePlacement();
                }
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
    }

    public void onPipAnimationEndResize() {
        synchronized (this.mService.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                ((TaskStack) this.mContainer).onPipAnimationEndResize();
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
    }

    public void getStackDockedModeBounds(Rect rect, Rect rect2, Rect rect3, boolean z) {
        synchronized (this.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                if (this.mContainer != 0) {
                    ((TaskStack) this.mContainer).getStackDockedModeBoundsLocked(rect, rect2, rect3, z);
                    WindowManagerService.resetPriorityAfterLockedSection();
                } else {
                    rect2.setEmpty();
                    rect3.setEmpty();
                    WindowManagerService.resetPriorityAfterLockedSection();
                }
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    public void prepareFreezingTaskBounds() {
        synchronized (this.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                if (this.mContainer == 0) {
                    throw new IllegalArgumentException("prepareFreezingTaskBounds: stack " + this + " not found.");
                }
                ((TaskStack) this.mContainer).prepareFreezingTaskBounds();
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
    }

    public void getRawBounds(Rect rect) {
        synchronized (this.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                if (((TaskStack) this.mContainer).matchParentBounds()) {
                    rect.setEmpty();
                } else {
                    ((TaskStack) this.mContainer).getRawBounds(rect);
                }
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
    }

    public void getBounds(Rect rect) {
        synchronized (this.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                if (this.mContainer != 0) {
                    ((TaskStack) this.mContainer).getBounds(rect);
                    WindowManagerService.resetPriorityAfterLockedSection();
                } else {
                    rect.setEmpty();
                    WindowManagerService.resetPriorityAfterLockedSection();
                }
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    public void getBoundsForNewConfiguration(Rect rect) {
        synchronized (this.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                ((TaskStack) this.mContainer).getBoundsForNewConfiguration(rect);
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
    }

    public void adjustConfigurationForBounds(Rect rect, Rect rect2, Rect rect3, Rect rect4, boolean z, boolean z2, float f, Configuration configuration, Configuration configuration2, int i) {
        int iMin;
        int iMin2;
        synchronized (this.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                DisplayInfo displayInfo = ((TaskStack) this.mContainer).getDisplayContent().getDisplayInfo();
                DisplayCutout displayCutout = displayInfo.displayCutout;
                this.mService.mPolicy.getStableInsetsLw(displayInfo.rotation, displayInfo.logicalWidth, displayInfo.logicalHeight, displayCutout, this.mTmpStableInsets);
                this.mService.mPolicy.getNonDecorInsetsLw(displayInfo.rotation, displayInfo.logicalWidth, displayInfo.logicalHeight, displayCutout, this.mTmpNonDecorInsets);
                this.mTmpDisplayBounds.set(0, 0, displayInfo.logicalWidth, displayInfo.logicalHeight);
                Rect appBounds = configuration2.windowConfiguration.getAppBounds();
                configuration.windowConfiguration.setBounds(rect);
                configuration.windowConfiguration.setAppBounds(!rect.isEmpty() ? rect : null);
                boolean z3 = true;
                if (WindowConfiguration.isFloating(i)) {
                    if (i == 2 && rect.width() == this.mTmpDisplayBounds.width() && rect.height() == this.mTmpDisplayBounds.height()) {
                        rect4.inset(this.mTmpStableInsets);
                        rect3.inset(this.mTmpNonDecorInsets);
                        configuration.windowConfiguration.getAppBounds().offsetTo(0, 0);
                    } else {
                        z3 = false;
                    }
                    iMin = (int) (rect4.width() / f);
                    iMin2 = (int) (rect4.height() / f);
                } else {
                    intersectDisplayBoundsExcludeInsets(rect3, rect2 != null ? rect2 : rect, this.mTmpNonDecorInsets, this.mTmpDisplayBounds, z, z2);
                    intersectDisplayBoundsExcludeInsets(rect4, rect2 != null ? rect2 : rect, this.mTmpStableInsets, this.mTmpDisplayBounds, z, z2);
                    iMin = Math.min((int) (rect4.width() / f), configuration2.screenWidthDp);
                    iMin2 = Math.min((int) (rect4.height() / f), configuration2.screenHeightDp);
                }
                if (z3 && configuration.windowConfiguration.getAppBounds() != null) {
                    configuration.windowConfiguration.getAppBounds().intersect(appBounds);
                }
                configuration.screenWidthDp = iMin;
                configuration.screenHeightDp = iMin2;
                configuration.smallestScreenWidthDp = getSmallestWidthForTaskBounds(rect2 != null ? rect2 : rect, f, i);
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
    }

    private void intersectDisplayBoundsExcludeInsets(Rect rect, Rect rect2, Rect rect3, Rect rect4, boolean z, boolean z2) {
        int i;
        this.mTmpRect.set(rect2);
        this.mService.intersectDisplayInsetBounds(rect4, rect3, this.mTmpRect);
        int i2 = this.mTmpRect.left - rect2.left;
        int i3 = this.mTmpRect.top - rect2.top;
        if (!z) {
            i = rect2.right - this.mTmpRect.right;
        } else {
            i = 0;
        }
        rect.inset(i2, i3, i, z2 ? 0 : rect2.bottom - this.mTmpRect.bottom);
    }

    private int getSmallestWidthForTaskBounds(Rect rect, float f, int i) {
        DisplayContent displayContent = ((TaskStack) this.mContainer).getDisplayContent();
        DisplayInfo displayInfo = displayContent.getDisplayInfo();
        if (rect == null || (rect.width() == displayInfo.logicalWidth && rect.height() == displayInfo.logicalHeight)) {
            return displayContent.getConfiguration().smallestScreenWidthDp;
        }
        if (WindowConfiguration.isFloating(i)) {
            return (int) (Math.min(rect.width(), rect.height()) / f);
        }
        return displayContent.getDockedDividerController().getSmallestWidthDpForBounds(rect);
    }

    void requestResize(Rect rect) {
        this.mHandler.obtainMessage(0, rect).sendToTarget();
    }

    public String toString() {
        return "{StackWindowController stackId=" + this.mStackId + "}";
    }

    private static final class H extends Handler {
        static final int REQUEST_RESIZE = 0;
        private final WeakReference<StackWindowController> mController;

        H(WeakReference<StackWindowController> weakReference, Looper looper) {
            super(looper);
            this.mController = weakReference;
        }

        @Override
        public void handleMessage(Message message) {
            StackWindowController stackWindowController = this.mController.get();
            StackWindowListener stackWindowListener = stackWindowController != null ? (StackWindowListener) stackWindowController.mListener : null;
            if (stackWindowListener != null && message.what == 0) {
                stackWindowListener.requestResize((Rect) message.obj);
            }
        }
    }
}
