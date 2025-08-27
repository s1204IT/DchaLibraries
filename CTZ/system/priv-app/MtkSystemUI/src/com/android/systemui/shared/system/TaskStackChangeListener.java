package com.android.systemui.shared.system;

import android.content.ComponentName;
import android.os.UserHandle;
import android.util.Log;
import com.android.systemui.shared.recents.model.ThumbnailData;

/* loaded from: classes.dex */
public abstract class TaskStackChangeListener {
    public void onTaskStackChangedBackground() {
    }

    public void onTaskStackChanged() {
    }

    public void onTaskSnapshotChanged(int i, ThumbnailData thumbnailData) {
    }

    public void onActivityPinned(String str, int i, int i2, int i3) {
    }

    public void onActivityUnpinned() {
    }

    public void onPinnedActivityRestartAttempt(boolean z) {
    }

    public void onPinnedStackAnimationStarted() {
    }

    public void onPinnedStackAnimationEnded() {
    }

    public void onActivityForcedResizable(String str, int i, int i2) {
    }

    public void onActivityDismissingDockedStack() {
    }

    public void onActivityLaunchOnSecondaryDisplayFailed() {
    }

    public void onTaskProfileLocked(int i, int i2) {
    }

    public void onTaskCreated(int i, ComponentName componentName) {
    }

    public void onTaskRemoved(int i) {
    }

    public void onTaskMovedToFront(int i) {
    }

    public void onActivityRequestedOrientationChanged(int i, int i2) {
    }

    protected final boolean checkCurrentUserId(int i, boolean z) {
        int iMyUserId = UserHandle.myUserId();
        if (iMyUserId != i) {
            if (z) {
                Log.d("TaskStackChangeListener", "UID mismatch. Process is uid=" + iMyUserId + " and the current user is uid=" + i);
                return false;
            }
            return false;
        }
        return true;
    }
}
