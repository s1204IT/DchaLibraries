package com.android.server.am;

import android.app.RemoteAction;
import android.content.res.Configuration;
import android.graphics.Rect;
import com.android.server.wm.PinnedStackWindowController;
import com.android.server.wm.PinnedStackWindowListener;
import java.util.ArrayList;
import java.util.List;

class PinnedActivityStack extends ActivityStack<PinnedStackWindowController> implements PinnedStackWindowListener {
    PinnedActivityStack(ActivityDisplay activityDisplay, int i, ActivityStackSupervisor activityStackSupervisor, boolean z) {
        super(activityDisplay, i, activityStackSupervisor, 2, 1, z);
    }

    @Override
    PinnedStackWindowController createStackWindowController(int i, boolean z, Rect rect) {
        return new PinnedStackWindowController(this.mStackId, this, i, z, rect, this.mStackSupervisor.mWindowManager);
    }

    Rect getDefaultPictureInPictureBounds(float f) {
        return getWindowContainerController().getPictureInPictureBounds(f, null);
    }

    void animateResizePinnedStack(Rect rect, Rect rect2, int i, boolean z) {
        if (skipResizeAnimation(rect2 == null)) {
            this.mService.moveTasksToFullscreenStack(this.mStackId, true);
        } else {
            getWindowContainerController().animateResizePinnedStack(rect2, rect, i, z);
        }
    }

    private boolean skipResizeAnimation(boolean z) {
        if (!z) {
            return false;
        }
        Configuration configuration = getParent().getConfiguration();
        ActivityRecord activityRecord = topRunningNonOverlayTaskActivity();
        return (activityRecord == null || activityRecord.isConfigurationCompatible(configuration)) ? false : true;
    }

    void setPictureInPictureAspectRatio(float f) {
        getWindowContainerController().setPictureInPictureAspectRatio(f);
    }

    void setPictureInPictureActions(List<RemoteAction> list) {
        getWindowContainerController().setPictureInPictureActions(list);
    }

    boolean isAnimatingBoundsToFullscreen() {
        return getWindowContainerController().isAnimatingBoundsToFullscreen();
    }

    @Override
    boolean deferScheduleMultiWindowModeChanged() {
        return ((PinnedStackWindowController) this.mWindowContainerController).deferScheduleMultiWindowModeChanged();
    }

    @Override
    public void updatePictureInPictureModeForPinnedStackAnimation(Rect rect, boolean z) {
        synchronized (this) {
            ArrayList<TaskRecord> allTasks = getAllTasks();
            for (int i = 0; i < allTasks.size(); i++) {
                this.mStackSupervisor.updatePictureInPictureMode(allTasks.get(i), rect, z);
            }
        }
    }
}
