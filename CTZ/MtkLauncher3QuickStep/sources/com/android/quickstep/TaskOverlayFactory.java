package com.android.quickstep;

import android.content.Context;
import android.graphics.Matrix;
import android.view.View;
import com.android.launcher3.R;
import com.android.launcher3.Utilities;
import com.android.launcher3.util.Preconditions;
import com.android.systemui.shared.recents.model.Task;
import com.android.systemui.shared.recents.model.ThumbnailData;

public class TaskOverlayFactory {
    private static TaskOverlayFactory sInstance;

    public static TaskOverlayFactory get(Context context) {
        Preconditions.assertUIThread();
        if (sInstance == null) {
            sInstance = (TaskOverlayFactory) Utilities.getOverrideObject(TaskOverlayFactory.class, context.getApplicationContext(), R.string.task_overlay_factory_class);
        }
        return sInstance;
    }

    public TaskOverlay createOverlay(View view) {
        return new TaskOverlay();
    }

    public static class TaskOverlay {
        public void setTaskInfo(Task task, ThumbnailData thumbnailData, Matrix matrix) {
        }

        public void reset() {
        }
    }
}
