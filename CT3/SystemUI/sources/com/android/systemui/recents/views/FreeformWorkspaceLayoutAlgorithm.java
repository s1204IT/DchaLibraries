package com.android.systemui.recents.views;

import android.content.Context;
import android.graphics.RectF;
import android.util.ArrayMap;
import com.android.systemui.R;
import com.android.systemui.recents.model.Task;
import java.util.Collections;
import java.util.List;

public class FreeformWorkspaceLayoutAlgorithm {
    private int mTaskPadding;
    private ArrayMap<Task.TaskKey, RectF> mTaskRectMap = new ArrayMap<>();

    public FreeformWorkspaceLayoutAlgorithm(Context context) {
        reloadOnConfigurationChange(context);
    }

    public void reloadOnConfigurationChange(Context context) {
        this.mTaskPadding = context.getResources().getDimensionPixelSize(R.dimen.recents_freeform_layout_task_padding) / 2;
    }

    public void update(List<Task> freeformTasks, TaskStackLayoutAlgorithm stackLayout) {
        float rowTaskWidth;
        Collections.reverse(freeformTasks);
        this.mTaskRectMap.clear();
        int numFreeformTasks = stackLayout.mNumFreeformTasks;
        if (freeformTasks.isEmpty()) {
            return;
        }
        int workspaceWidth = stackLayout.mFreeformRect.width();
        int workspaceHeight = stackLayout.mFreeformRect.height();
        float normalizedWorkspaceWidth = workspaceWidth / workspaceHeight;
        float[] normalizedTaskWidths = new float[numFreeformTasks];
        for (int i = 0; i < numFreeformTasks; i++) {
            Task task = freeformTasks.get(i);
            if (task.bounds != null) {
                rowTaskWidth = task.bounds.width() / task.bounds.height();
            } else {
                rowTaskWidth = normalizedWorkspaceWidth;
            }
            normalizedTaskWidths[i] = Math.min(rowTaskWidth, normalizedWorkspaceWidth);
        }
        float rowScale = 0.85f;
        float rowWidth = 0.0f;
        float maxRowWidth = 0.0f;
        int rowCount = 1;
        int i2 = 0;
        while (i2 < numFreeformTasks) {
            float width = normalizedTaskWidths[i2] * rowScale;
            if (rowWidth + width <= normalizedWorkspaceWidth) {
                rowWidth += width;
                i2++;
            } else if ((rowCount + 1) * rowScale > 1.0f) {
                rowScale = Math.min(normalizedWorkspaceWidth / (rowWidth + width), 1.0f / (rowCount + 1));
                rowCount = 1;
                rowWidth = 0.0f;
                i2 = 0;
            } else {
                rowWidth = width;
                rowCount++;
                i2++;
            }
            maxRowWidth = Math.max(rowWidth, maxRowWidth);
        }
        float defaultRowLeft = ((1.0f - (maxRowWidth / normalizedWorkspaceWidth)) * workspaceWidth) / 2.0f;
        int rowLeft = (int) defaultRowLeft;
        float rowTop = ((1.0f - (rowCount * rowScale)) * workspaceHeight) / 2.0f;
        float rowHeight = rowScale * workspaceHeight;
        for (int i3 = 0; i3 < numFreeformTasks; i3++) {
            Task task2 = freeformTasks.get(i3);
            int width2 = (int) (normalizedTaskWidths[i3] * rowHeight);
            if (rowLeft + width2 > workspaceWidth) {
                rowTop += rowHeight;
                rowLeft = (int) defaultRowLeft;
            }
            RectF rect = new RectF(rowLeft, rowTop, rowLeft + width2, rowTop + rowHeight);
            rect.inset(this.mTaskPadding, this.mTaskPadding);
            rowLeft += width2;
            this.mTaskRectMap.put(task2.key, rect);
        }
    }

    public boolean isTransformAvailable(Task task, TaskStackLayoutAlgorithm stackLayout) {
        if (stackLayout.mNumFreeformTasks == 0 || task == null) {
            return false;
        }
        return this.mTaskRectMap.containsKey(task.key);
    }

    public TaskViewTransform getTransform(Task task, TaskViewTransform transformOut, TaskStackLayoutAlgorithm stackLayout) {
        if (this.mTaskRectMap.containsKey(task.key)) {
            RectF ffRect = this.mTaskRectMap.get(task.key);
            transformOut.scale = 1.0f;
            transformOut.alpha = 1.0f;
            transformOut.translationZ = stackLayout.mMaxTranslationZ;
            transformOut.dimAlpha = 0.0f;
            transformOut.viewOutlineAlpha = 2.0f;
            transformOut.rect.set(ffRect);
            transformOut.rect.offset(stackLayout.mFreeformRect.left, stackLayout.mFreeformRect.top);
            transformOut.visible = true;
            return transformOut;
        }
        return null;
    }
}
