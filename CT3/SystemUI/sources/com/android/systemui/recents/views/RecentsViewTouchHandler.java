package com.android.systemui.recents.views;

import android.app.ActivityManager;
import android.graphics.Point;
import android.graphics.Rect;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.view.ViewDebug;
import com.android.internal.policy.DividerSnapAlgorithm;
import com.android.systemui.recents.Recents;
import com.android.systemui.recents.RecentsConfiguration;
import com.android.systemui.recents.events.EventBus;
import com.android.systemui.recents.events.activity.ConfigurationChangedEvent;
import com.android.systemui.recents.events.ui.HideIncompatibleAppOverlayEvent;
import com.android.systemui.recents.events.ui.ShowIncompatibleAppOverlayEvent;
import com.android.systemui.recents.events.ui.dragndrop.DragDropTargetChangedEvent;
import com.android.systemui.recents.events.ui.dragndrop.DragEndEvent;
import com.android.systemui.recents.events.ui.dragndrop.DragStartEvent;
import com.android.systemui.recents.events.ui.dragndrop.DragStartInitializeDropTargetsEvent;
import com.android.systemui.recents.misc.SystemServicesProxy;
import com.android.systemui.recents.model.Task;
import com.android.systemui.recents.model.TaskStack;
import java.util.ArrayList;
import java.util.Iterator;

public class RecentsViewTouchHandler {
    private DividerSnapAlgorithm mDividerSnapAlgorithm;

    @ViewDebug.ExportedProperty(category = "recents")
    private boolean mDragRequested;
    private float mDragSlop;

    @ViewDebug.ExportedProperty(deepExport = true, prefix = "drag_task")
    private Task mDragTask;

    @ViewDebug.ExportedProperty(category = "recents")
    private boolean mIsDragging;
    private DropTarget mLastDropTarget;
    private RecentsView mRv;

    @ViewDebug.ExportedProperty(deepExport = true, prefix = "drag_task_view_")
    private TaskView mTaskView;

    @ViewDebug.ExportedProperty(category = "recents")
    private Point mTaskViewOffset = new Point();

    @ViewDebug.ExportedProperty(category = "recents")
    private Point mDownPos = new Point();
    private ArrayList<DropTarget> mDropTargets = new ArrayList<>();
    private ArrayList<TaskStack.DockState> mVisibleDockStates = new ArrayList<>();

    public RecentsViewTouchHandler(RecentsView rv) {
        this.mRv = rv;
        this.mDragSlop = ViewConfiguration.get(rv.getContext()).getScaledTouchSlop();
        updateSnapAlgorithm();
    }

    private void updateSnapAlgorithm() {
        Rect insets = new Rect();
        SystemServicesProxy.getInstance(this.mRv.getContext()).getStableInsets(insets);
        this.mDividerSnapAlgorithm = DividerSnapAlgorithm.create(this.mRv.getContext(), insets);
    }

    public void registerDropTargetForCurrentDrag(DropTarget target) {
        this.mDropTargets.add(target);
    }

    public TaskStack.DockState[] getDockStatesForCurrentOrientation() {
        boolean isLandscape = this.mRv.getResources().getConfiguration().orientation == 2;
        RecentsConfiguration config = Recents.getConfiguration();
        if (isLandscape) {
            if (!config.isLargeScreen) {
                TaskStack.DockState[] dockStates = DockRegion.PHONE_LANDSCAPE;
                return dockStates;
            }
            TaskStack.DockState[] dockStates2 = DockRegion.TABLET_LANDSCAPE;
            return dockStates2;
        }
        if (config.isLargeScreen) {
            TaskStack.DockState[] dockStates3 = DockRegion.TABLET_PORTRAIT;
            return dockStates3;
        }
        TaskStack.DockState[] dockStates4 = DockRegion.PHONE_PORTRAIT;
        return dockStates4;
    }

    public ArrayList<TaskStack.DockState> getVisibleDockStates() {
        return this.mVisibleDockStates;
    }

    public boolean onInterceptTouchEvent(MotionEvent ev) {
        handleTouchEvent(ev);
        return this.mDragRequested;
    }

    public boolean onTouchEvent(MotionEvent ev) {
        handleTouchEvent(ev);
        return this.mDragRequested;
    }

    public final void onBusEvent(DragStartEvent event) {
        SystemServicesProxy ssp = Recents.getSystemServices();
        this.mRv.getParent().requestDisallowInterceptTouchEvent(true);
        this.mDragRequested = true;
        this.mIsDragging = false;
        this.mDragTask = event.task;
        this.mTaskView = event.taskView;
        this.mDropTargets.clear();
        int[] recentsViewLocation = new int[2];
        this.mRv.getLocationInWindow(recentsViewLocation);
        this.mTaskViewOffset.set((this.mTaskView.getLeft() - recentsViewLocation[0]) + event.tlOffset.x, (this.mTaskView.getTop() - recentsViewLocation[1]) + event.tlOffset.y);
        float x = this.mDownPos.x - this.mTaskViewOffset.x;
        float y = this.mDownPos.y - this.mTaskViewOffset.y;
        this.mTaskView.setTranslationX(x);
        this.mTaskView.setTranslationY(y);
        this.mVisibleDockStates.clear();
        if (ActivityManager.supportsMultiWindow() && !ssp.hasDockedTask() && this.mDividerSnapAlgorithm.isSplitScreenFeasible()) {
            Recents.logDockAttempt(this.mRv.getContext(), event.task.getTopComponent(), event.task.resizeMode);
            if (!event.task.isDockable) {
                EventBus.getDefault().send(new ShowIncompatibleAppOverlayEvent());
            } else {
                TaskStack.DockState[] dockStates = getDockStatesForCurrentOrientation();
                for (TaskStack.DockState dockState : dockStates) {
                    registerDropTargetForCurrentDrag(dockState);
                    dockState.update(this.mRv.getContext());
                    this.mVisibleDockStates.add(dockState);
                }
            }
        }
        EventBus.getDefault().send(new DragStartInitializeDropTargetsEvent(event.task, event.taskView, this));
    }

    public final void onBusEvent(DragEndEvent event) {
        if (!this.mDragTask.isDockable) {
            EventBus.getDefault().send(new HideIncompatibleAppOverlayEvent());
        }
        this.mDragRequested = false;
        this.mDragTask = null;
        this.mTaskView = null;
        this.mLastDropTarget = null;
    }

    public final void onBusEvent(ConfigurationChangedEvent event) {
        if (!event.fromDisplayDensityChange && !event.fromDeviceOrientationChange) {
            return;
        }
        updateSnapAlgorithm();
    }

    private void handleTouchEvent(MotionEvent ev) {
        int action = ev.getActionMasked();
        switch (action) {
            case 0:
                this.mDownPos.set((int) ev.getX(), (int) ev.getY());
                break;
            case 1:
            case 3:
                if (this.mDragRequested) {
                    boolean cancelled = action == 3;
                    if (cancelled) {
                        EventBus.getDefault().send(new DragDropTargetChangedEvent(this.mDragTask, null));
                    }
                    EventBus.getDefault().send(new DragEndEvent(this.mDragTask, this.mTaskView, !cancelled ? this.mLastDropTarget : null));
                }
                break;
            case 2:
                float evX = ev.getX();
                float evY = ev.getY();
                float x = evX - this.mTaskViewOffset.x;
                float y = evY - this.mTaskViewOffset.y;
                if (this.mDragRequested) {
                    if (!this.mIsDragging) {
                        this.mIsDragging = Math.hypot((double) (evX - ((float) this.mDownPos.x)), (double) (evY - ((float) this.mDownPos.y))) > ((double) this.mDragSlop);
                    }
                    if (this.mIsDragging) {
                        int width = this.mRv.getMeasuredWidth();
                        int height = this.mRv.getMeasuredHeight();
                        DropTarget currentDropTarget = null;
                        if (this.mLastDropTarget != null && this.mLastDropTarget.acceptsDrop((int) evX, (int) evY, width, height, true)) {
                            currentDropTarget = this.mLastDropTarget;
                        }
                        if (currentDropTarget == null) {
                            Iterator target$iterator = this.mDropTargets.iterator();
                            while (true) {
                                if (target$iterator.hasNext()) {
                                    DropTarget target = (DropTarget) target$iterator.next();
                                    if (target.acceptsDrop((int) evX, (int) evY, width, height, false)) {
                                        currentDropTarget = target;
                                    }
                                }
                            }
                        }
                        if (this.mLastDropTarget != currentDropTarget) {
                            this.mLastDropTarget = currentDropTarget;
                            EventBus.getDefault().send(new DragDropTargetChangedEvent(this.mDragTask, currentDropTarget));
                        }
                    }
                    this.mTaskView.setTranslationX(x);
                    this.mTaskView.setTranslationY(y);
                }
                break;
        }
    }
}
