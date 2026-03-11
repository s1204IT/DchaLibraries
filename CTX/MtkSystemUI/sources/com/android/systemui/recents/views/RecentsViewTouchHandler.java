package com.android.systemui.recents.views;

import android.app.ActivityManager;
import android.graphics.Point;
import android.graphics.Rect;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.view.ViewDebug;
import com.android.internal.policy.DividerSnapAlgorithm;
import com.android.systemui.recents.Recents;
import com.android.systemui.recents.events.EventBus;
import com.android.systemui.recents.events.activity.ConfigurationChangedEvent;
import com.android.systemui.recents.events.activity.HideRecentsEvent;
import com.android.systemui.recents.events.ui.DismissAllTaskViewsEvent;
import com.android.systemui.recents.events.ui.HideIncompatibleAppOverlayEvent;
import com.android.systemui.recents.events.ui.ShowIncompatibleAppOverlayEvent;
import com.android.systemui.recents.events.ui.dragndrop.DragDropTargetChangedEvent;
import com.android.systemui.recents.events.ui.dragndrop.DragEndEvent;
import com.android.systemui.recents.events.ui.dragndrop.DragStartEvent;
import com.android.systemui.recents.events.ui.dragndrop.DragStartInitializeDropTargetsEvent;
import com.android.systemui.recents.misc.SystemServicesProxy;
import com.android.systemui.shared.recents.model.Task;
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
    private int mDeviceId = -1;
    private ArrayList<DropTarget> mDropTargets = new ArrayList<>();
    private ArrayList<DockState> mVisibleDockStates = new ArrayList<>();

    public RecentsViewTouchHandler(RecentsView recentsView) {
        this.mRv = recentsView;
        this.mDragSlop = ViewConfiguration.get(recentsView.getContext()).getScaledTouchSlop();
        updateSnapAlgorithm();
    }

    private void updateSnapAlgorithm() {
        Rect rect = new Rect();
        SystemServicesProxy.getInstance(this.mRv.getContext()).getStableInsets(rect);
        this.mDividerSnapAlgorithm = DividerSnapAlgorithm.create(this.mRv.getContext(), rect);
    }

    public void registerDropTargetForCurrentDrag(DropTarget dropTarget) {
        this.mDropTargets.add(dropTarget);
    }

    public ArrayList<DockState> getVisibleDockStates() {
        return this.mVisibleDockStates;
    }

    public boolean onInterceptTouchEvent(MotionEvent motionEvent) {
        return handleTouchEvent(motionEvent) || this.mDragRequested;
    }

    public boolean onTouchEvent(MotionEvent motionEvent) {
        handleTouchEvent(motionEvent);
        if (motionEvent.getAction() == 1 && this.mRv.getStack().getTaskCount() == 0) {
            EventBus.getDefault().send(new HideRecentsEvent(false, true));
        }
        return true;
    }

    public final void onBusEvent(DragStartEvent dragStartEvent) {
        InputDevice device;
        SystemServicesProxy systemServices = Recents.getSystemServices();
        this.mRv.getParent().requestDisallowInterceptTouchEvent(true);
        this.mDragRequested = true;
        this.mIsDragging = false;
        this.mDragTask = dragStartEvent.task;
        this.mTaskView = dragStartEvent.taskView;
        this.mDropTargets.clear();
        int[] iArr = new int[2];
        this.mRv.getLocationInWindow(iArr);
        this.mTaskViewOffset.set((this.mTaskView.getLeft() - iArr[0]) + dragStartEvent.tlOffset.x, (this.mTaskView.getTop() - iArr[1]) + dragStartEvent.tlOffset.y);
        if (dragStartEvent.isUserTouchInitiated) {
            float f = this.mDownPos.x - this.mTaskViewOffset.x;
            float f2 = this.mDownPos.y - this.mTaskViewOffset.y;
            this.mTaskView.setTranslationX(f);
            this.mTaskView.setTranslationY(f2);
        }
        this.mVisibleDockStates.clear();
        if (ActivityManager.supportsMultiWindow(this.mRv.getContext()) && !systemServices.hasDockedTask() && this.mDividerSnapAlgorithm.isSplitScreenFeasible()) {
            Recents.logDockAttempt(this.mRv.getContext(), dragStartEvent.task.getTopComponent(), dragStartEvent.task.resizeMode);
            if (!dragStartEvent.task.isDockable) {
                EventBus.getDefault().send(new ShowIncompatibleAppOverlayEvent());
            } else {
                for (DockState dockState : Recents.getConfiguration().getDockStatesForCurrentOrientation()) {
                    registerDropTargetForCurrentDrag(dockState);
                    dockState.update(this.mRv.getContext());
                    this.mVisibleDockStates.add(dockState);
                }
            }
        }
        EventBus.getDefault().send(new DragStartInitializeDropTargetsEvent(dragStartEvent.task, dragStartEvent.taskView, this));
        if (this.mDeviceId != -1 && (device = InputDevice.getDevice(this.mDeviceId)) != null) {
            device.setPointerType(1021);
        }
    }

    public final void onBusEvent(DragEndEvent dragEndEvent) {
        if (!this.mDragTask.isDockable) {
            EventBus.getDefault().send(new HideIncompatibleAppOverlayEvent());
        }
        this.mDragRequested = false;
        this.mDragTask = null;
        this.mTaskView = null;
        this.mLastDropTarget = null;
    }

    public final void onBusEvent(ConfigurationChangedEvent configurationChangedEvent) {
        if (configurationChangedEvent.fromDisplayDensityChange || configurationChangedEvent.fromDeviceOrientationChange) {
            updateSnapAlgorithm();
        }
    }

    void cancelStackActionButtonClick() {
        this.mRv.getStackActionButton().setPressed(false);
    }

    private boolean isWithinStackActionButton(float f, float f2) {
        Rect stackActionButtonBoundsFromStackLayout = this.mRv.getStackActionButtonBoundsFromStackLayout();
        return this.mRv.getStackActionButton().getVisibility() == 0 && this.mRv.getStackActionButton().pointInView(f - ((float) stackActionButtonBoundsFromStackLayout.left), f2 - ((float) stackActionButtonBoundsFromStackLayout.top), 0.0f);
    }

    private void changeStackActionButtonDrawableHotspot(float f, float f2) {
        Rect stackActionButtonBoundsFromStackLayout = this.mRv.getStackActionButtonBoundsFromStackLayout();
        this.mRv.getStackActionButton().drawableHotspotChanged(f - stackActionButtonBoundsFromStackLayout.left, f2 - stackActionButtonBoundsFromStackLayout.top);
    }

    private boolean handleTouchEvent(MotionEvent motionEvent) {
        boolean z;
        int actionMasked = motionEvent.getActionMasked();
        float x = motionEvent.getX();
        float y = motionEvent.getY();
        switch (actionMasked) {
            case 0:
                this.mDownPos.set((int) x, (int) y);
                this.mDeviceId = motionEvent.getDeviceId();
                if (isWithinStackActionButton(x, y)) {
                    changeStackActionButtonDrawableHotspot(x, y);
                    this.mRv.getStackActionButton().setPressed(true);
                }
                break;
            case 1:
            case 3:
                if (this.mRv.getStackActionButton().isPressed() && isWithinStackActionButton(x, y)) {
                    EventBus.getDefault().send(new DismissAllTaskViewsEvent());
                    z = true;
                } else {
                    z = false;
                }
                cancelStackActionButtonClick();
                if (this.mDragRequested) {
                    boolean z2 = actionMasked == 3;
                    if (z2) {
                        EventBus.getDefault().send(new DragDropTargetChangedEvent(this.mDragTask, null));
                    }
                    EventBus.getDefault().send(new DragEndEvent(this.mDragTask, this.mTaskView, z2 ? null : this.mLastDropTarget));
                    return z;
                }
                this.mDeviceId = -1;
                return z;
            case 2:
                float f = x - this.mTaskViewOffset.x;
                float f2 = y - this.mTaskViewOffset.y;
                if (this.mRv.getStackActionButton().isPressed() && isWithinStackActionButton(x, y)) {
                    changeStackActionButtonDrawableHotspot(x, y);
                }
                if (this.mDragRequested) {
                    if (!this.mIsDragging) {
                        this.mIsDragging = Math.hypot((double) (x - ((float) this.mDownPos.x)), (double) (y - ((float) this.mDownPos.y))) > ((double) this.mDragSlop);
                    }
                    if (this.mIsDragging) {
                        int measuredWidth = this.mRv.getMeasuredWidth();
                        int measuredHeight = this.mRv.getMeasuredHeight();
                        if (this.mLastDropTarget != null && this.mLastDropTarget.acceptsDrop((int) x, (int) y, measuredWidth, measuredHeight, this.mRv.mSystemInsets, true)) {
                            dropTarget = this.mLastDropTarget;
                        }
                        if (dropTarget == null) {
                            Iterator<DropTarget> it = this.mDropTargets.iterator();
                            while (true) {
                                if (it.hasNext()) {
                                    DropTarget next = it.next();
                                    Iterator<DropTarget> it2 = it;
                                    if (!next.acceptsDrop((int) x, (int) y, measuredWidth, measuredHeight, this.mRv.mSystemInsets, false)) {
                                        it = it2;
                                    } else {
                                        dropTarget = next;
                                    }
                                }
                            }
                        }
                        if (this.mLastDropTarget != dropTarget) {
                            this.mLastDropTarget = dropTarget;
                            EventBus.getDefault().send(new DragDropTargetChangedEvent(this.mDragTask, dropTarget));
                        }
                    }
                    this.mTaskView.setTranslationX(f);
                    this.mTaskView.setTranslationY(f2);
                }
                break;
        }
        return false;
    }
}
