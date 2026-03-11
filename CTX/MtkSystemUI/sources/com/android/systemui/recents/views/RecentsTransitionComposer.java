package com.android.systemui.recents.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.util.Log;
import com.android.systemui.recents.Recents;
import com.android.systemui.shared.recents.model.Task;
import com.android.systemui.shared.recents.view.AppTransitionAnimationSpecCompat;
import com.android.systemui.shared.recents.view.RecentsTransition;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RecentsTransitionComposer {
    private Context mContext;
    private TaskViewTransform mTmpTransform = new TaskViewTransform();

    public RecentsTransitionComposer(Context context) {
        this.mContext = context;
    }

    private static AppTransitionAnimationSpecCompat composeAnimationSpec(TaskStackView taskStackView, TaskView taskView, TaskViewTransform taskViewTransform, boolean z) {
        Bitmap bitmapComposeHeaderBitmap;
        if (z) {
            bitmapComposeHeaderBitmap = composeHeaderBitmap(taskView, taskViewTransform);
            if (bitmapComposeHeaderBitmap == null) {
                return null;
            }
        } else {
            bitmapComposeHeaderBitmap = null;
        }
        Rect rect = new Rect();
        taskViewTransform.rect.round(rect);
        if (!Recents.getConfiguration().isLowRamDevice && taskView.getTask() != taskStackView.getStack().getFrontMostTask()) {
            rect.bottom = rect.top + taskStackView.getMeasuredHeight();
        }
        return new AppTransitionAnimationSpecCompat(taskView.getTask().key.id, bitmapComposeHeaderBitmap, rect);
    }

    public List<AppTransitionAnimationSpecCompat> composeDockAnimationSpec(TaskView taskView, Rect rect) {
        this.mTmpTransform.fillIn(taskView);
        Task task = taskView.getTask();
        return Collections.singletonList(new AppTransitionAnimationSpecCompat(task.key.id, composeTaskBitmap(taskView, this.mTmpTransform), rect));
    }

    public List<AppTransitionAnimationSpecCompat> composeAnimationSpecs(Task task, TaskStackView taskStackView, int i, int i2, Rect rect) {
        TaskView childViewForTask = taskStackView.getChildViewForTask(task);
        TaskStackLayoutAlgorithm stackAlgorithm = taskStackView.getStackAlgorithm();
        Rect rect2 = new Rect();
        stackAlgorithm.getFrontOfStackTransform().rect.round(rect2);
        if (i == 1 || i == 3 || i == 4 || i2 == 4 || i == 0) {
            ArrayList arrayList = new ArrayList();
            if (childViewForTask == null) {
                arrayList.add(composeOffscreenAnimationSpec(task, rect2));
            } else {
                this.mTmpTransform.fillIn(childViewForTask);
                stackAlgorithm.transformToScreenCoordinates(this.mTmpTransform, rect);
                AppTransitionAnimationSpecCompat appTransitionAnimationSpecCompatComposeAnimationSpec = composeAnimationSpec(taskStackView, childViewForTask, this.mTmpTransform, true);
                if (appTransitionAnimationSpecCompatComposeAnimationSpec != null) {
                    arrayList.add(appTransitionAnimationSpecCompatComposeAnimationSpec);
                }
            }
            return arrayList;
        }
        return Collections.emptyList();
    }

    private static AppTransitionAnimationSpecCompat composeOffscreenAnimationSpec(Task task, Rect rect) {
        return new AppTransitionAnimationSpecCompat(task.key.id, null, rect);
    }

    public static Bitmap composeTaskBitmap(TaskView taskView, TaskViewTransform taskViewTransform) {
        float f = taskViewTransform.scale;
        int iWidth = (int) (taskViewTransform.rect.width() * f);
        int iHeight = (int) (taskViewTransform.rect.height() * f);
        if (iWidth == 0 || iHeight == 0) {
            Log.e("RecentsTransitionComposer", "Could not compose thumbnail for task: " + taskView.getTask() + " at transform: " + taskViewTransform);
            return RecentsTransition.drawViewIntoHardwareBitmap(1, 1, null, 1.0f, 16777215);
        }
        return RecentsTransition.drawViewIntoHardwareBitmap(iWidth, iHeight, taskView, f, 0);
    }

    private static Bitmap composeHeaderBitmap(TaskView taskView, TaskViewTransform taskViewTransform) {
        float f = taskViewTransform.scale;
        int iWidth = (int) taskViewTransform.rect.width();
        int measuredHeight = (int) (taskView.mHeaderView.getMeasuredHeight() * f);
        if (iWidth == 0 || measuredHeight == 0) {
            return null;
        }
        return RecentsTransition.drawViewIntoHardwareBitmap(iWidth, measuredHeight, taskView.mHeaderView, f, 0);
    }
}
