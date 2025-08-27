package com.android.systemui.recents.views;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Point;
import android.os.Bundle;
import android.util.SparseArray;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;
import com.android.systemui.R;
import com.android.systemui.recents.Recents;
import com.android.systemui.recents.events.EventBus;
import com.android.systemui.recents.events.ui.dragndrop.DragEndEvent;
import com.android.systemui.recents.events.ui.dragndrop.DragStartEvent;

/* loaded from: classes.dex */
public class TaskViewAccessibilityDelegate extends View.AccessibilityDelegate {
    protected final SparseArray<AccessibilityNodeInfo.AccessibilityAction> mActions = new SparseArray<>();
    private final TaskView mTaskView;

    public TaskViewAccessibilityDelegate(TaskView taskView) {
        this.mTaskView = taskView;
        Context context = taskView.getContext();
        this.mActions.put(R.id.action_split_task_to_top, new AccessibilityNodeInfo.AccessibilityAction(R.id.action_split_task_to_top, context.getString(R.string.recents_accessibility_split_screen_top)));
        this.mActions.put(R.id.action_split_task_to_left, new AccessibilityNodeInfo.AccessibilityAction(R.id.action_split_task_to_left, context.getString(R.string.recents_accessibility_split_screen_left)));
        this.mActions.put(R.id.action_split_task_to_right, new AccessibilityNodeInfo.AccessibilityAction(R.id.action_split_task_to_right, context.getString(R.string.recents_accessibility_split_screen_right)));
    }

    @Override // android.view.View.AccessibilityDelegate
    public void onInitializeAccessibilityNodeInfo(View view, AccessibilityNodeInfo accessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(view, accessibilityNodeInfo);
        if (ActivityManager.supportsSplitScreenMultiWindow(this.mTaskView.getContext()) && !Recents.getSystemServices().hasDockedTask()) {
            for (DockState dockState : Recents.getConfiguration().getDockStatesForCurrentOrientation()) {
                if (dockState == DockState.TOP) {
                    accessibilityNodeInfo.addAction(this.mActions.get(R.id.action_split_task_to_top));
                } else if (dockState == DockState.LEFT) {
                    accessibilityNodeInfo.addAction(this.mActions.get(R.id.action_split_task_to_left));
                } else if (dockState == DockState.RIGHT) {
                    accessibilityNodeInfo.addAction(this.mActions.get(R.id.action_split_task_to_right));
                }
            }
        }
    }

    @Override // android.view.View.AccessibilityDelegate
    public boolean performAccessibilityAction(View view, int i, Bundle bundle) {
        if (i == R.id.action_split_task_to_top) {
            simulateDragIntoMultiwindow(DockState.TOP);
            return true;
        }
        if (i == R.id.action_split_task_to_left) {
            simulateDragIntoMultiwindow(DockState.LEFT);
            return true;
        }
        if (i == R.id.action_split_task_to_right) {
            simulateDragIntoMultiwindow(DockState.RIGHT);
            return true;
        }
        return super.performAccessibilityAction(view, i, bundle);
    }

    private void simulateDragIntoMultiwindow(DockState dockState) {
        EventBus.getDefault().send(new DragStartEvent(this.mTaskView.getTask(), this.mTaskView, new Point(0, 0), false));
        EventBus.getDefault().send(new DragEndEvent(this.mTaskView.getTask(), this.mTaskView, dockState));
    }
}
