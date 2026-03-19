package com.android.server.am;

import android.app.ActivityOptions;
import android.content.pm.ActivityInfo;
import android.graphics.Rect;
import android.util.Slog;
import com.android.internal.annotations.VisibleForTesting;
import com.android.server.am.LaunchParamsController;
import java.util.ArrayList;

class TaskLaunchParamsModifier implements LaunchParamsController.LaunchParamsModifier {
    private static final boolean ALLOW_RESTART = true;
    private static final int BOUNDS_CONFLICT_MIN_DISTANCE = 4;
    private static final int MARGIN_SIZE_DENOMINATOR = 4;
    private static final int MINIMAL_STEP = 1;
    private static final int SHIFT_POLICY_DIAGONAL_DOWN = 1;
    private static final int SHIFT_POLICY_HORIZONTAL_LEFT = 3;
    private static final int SHIFT_POLICY_HORIZONTAL_RIGHT = 2;
    private static final int STEP_DENOMINATOR = 16;
    private static final String TAG = "ActivityManager";
    private static final int WINDOW_SIZE_DENOMINATOR = 2;
    private final Rect mAvailableRect = new Rect();
    private final Rect mTmpProposal = new Rect();
    private final Rect mTmpOriginal = new Rect();

    TaskLaunchParamsModifier() {
    }

    @Override
    public int onCalculate(TaskRecord taskRecord, ActivityInfo.WindowLayout windowLayout, ActivityRecord activityRecord, ActivityRecord activityRecord2, ActivityOptions activityOptions, LaunchParamsController.LaunchParams launchParams, LaunchParamsController.LaunchParams launchParams2) {
        if (taskRecord == null || taskRecord.getStack() == null || !taskRecord.inFreeformWindowingMode()) {
            return 0;
        }
        ArrayList<TaskRecord> allTasks = taskRecord.getStack().getAllTasks();
        this.mAvailableRect.set(taskRecord.getParent().getBounds());
        Rect rect = launchParams2.mBounds;
        if (windowLayout == null) {
            positionCenter(allTasks, this.mAvailableRect, getFreeformWidth(this.mAvailableRect), getFreeformHeight(this.mAvailableRect), rect);
            return 2;
        }
        int finalWidth = getFinalWidth(windowLayout, this.mAvailableRect);
        int finalHeight = getFinalHeight(windowLayout, this.mAvailableRect);
        int i = windowLayout.gravity & 112;
        int i2 = windowLayout.gravity & 7;
        if (i == 48) {
            if (i2 == 5) {
                positionTopRight(allTasks, this.mAvailableRect, finalWidth, finalHeight, rect);
            } else {
                positionTopLeft(allTasks, this.mAvailableRect, finalWidth, finalHeight, rect);
            }
        } else if (i == 80) {
            if (i2 == 5) {
                positionBottomRight(allTasks, this.mAvailableRect, finalWidth, finalHeight, rect);
            } else {
                positionBottomLeft(allTasks, this.mAvailableRect, finalWidth, finalHeight, rect);
            }
        } else {
            Slog.w(TAG, "Received unsupported gravity: " + windowLayout.gravity + ", positioning in the center instead.");
            positionCenter(allTasks, this.mAvailableRect, finalWidth, finalHeight, rect);
        }
        return 2;
    }

    @VisibleForTesting
    static int getFreeformStartLeft(Rect rect) {
        return rect.left + (rect.width() / 4);
    }

    @VisibleForTesting
    static int getFreeformStartTop(Rect rect) {
        return rect.top + (rect.height() / 4);
    }

    @VisibleForTesting
    static int getFreeformWidth(Rect rect) {
        return rect.width() / 2;
    }

    @VisibleForTesting
    static int getFreeformHeight(Rect rect) {
        return rect.height() / 2;
    }

    @VisibleForTesting
    static int getHorizontalStep(Rect rect) {
        return Math.max(rect.width() / 16, 1);
    }

    @VisibleForTesting
    static int getVerticalStep(Rect rect) {
        return Math.max(rect.height() / 16, 1);
    }

    private int getFinalWidth(ActivityInfo.WindowLayout windowLayout, Rect rect) {
        int freeformWidth = getFreeformWidth(rect);
        if (windowLayout.width > 0) {
            freeformWidth = windowLayout.width;
        }
        if (windowLayout.widthFraction > 0.0f) {
            return (int) (rect.width() * windowLayout.widthFraction);
        }
        return freeformWidth;
    }

    private int getFinalHeight(ActivityInfo.WindowLayout windowLayout, Rect rect) {
        int freeformHeight = getFreeformHeight(rect);
        if (windowLayout.height > 0) {
            freeformHeight = windowLayout.height;
        }
        if (windowLayout.heightFraction > 0.0f) {
            return (int) (rect.height() * windowLayout.heightFraction);
        }
        return freeformHeight;
    }

    private void positionBottomLeft(ArrayList<TaskRecord> arrayList, Rect rect, int i, int i2, Rect rect2) {
        this.mTmpProposal.set(rect.left, rect.bottom - i2, rect.left + i, rect.bottom);
        position(arrayList, rect, this.mTmpProposal, false, 2, rect2);
    }

    private void positionBottomRight(ArrayList<TaskRecord> arrayList, Rect rect, int i, int i2, Rect rect2) {
        this.mTmpProposal.set(rect.right - i, rect.bottom - i2, rect.right, rect.bottom);
        position(arrayList, rect, this.mTmpProposal, false, 3, rect2);
    }

    private void positionTopLeft(ArrayList<TaskRecord> arrayList, Rect rect, int i, int i2, Rect rect2) {
        this.mTmpProposal.set(rect.left, rect.top, rect.left + i, rect.top + i2);
        position(arrayList, rect, this.mTmpProposal, false, 2, rect2);
    }

    private void positionTopRight(ArrayList<TaskRecord> arrayList, Rect rect, int i, int i2, Rect rect2) {
        this.mTmpProposal.set(rect.right - i, rect.top, rect.right, rect.top + i2);
        position(arrayList, rect, this.mTmpProposal, false, 3, rect2);
    }

    private void positionCenter(ArrayList<TaskRecord> arrayList, Rect rect, int i, int i2, Rect rect2) {
        int freeformStartLeft = getFreeformStartLeft(rect);
        int freeformStartTop = getFreeformStartTop(rect);
        this.mTmpProposal.set(freeformStartLeft, freeformStartTop, i + freeformStartLeft, i2 + freeformStartTop);
        position(arrayList, rect, this.mTmpProposal, true, 1, rect2);
    }

    private void position(ArrayList<TaskRecord> arrayList, Rect rect, Rect rect2, boolean z, int i, Rect rect3) {
        this.mTmpOriginal.set(rect2);
        boolean z2 = false;
        while (true) {
            if (!boundsConflict(rect2, arrayList)) {
                break;
            }
            shiftStartingPoint(rect2, rect, i);
            if (shiftedTooFar(rect2, rect, i)) {
                if (!z) {
                    rect2.set(this.mTmpOriginal);
                    break;
                } else {
                    rect2.set(rect.left, rect.top, rect.left + rect2.width(), rect.top + rect2.height());
                    z2 = true;
                    if (!z2) {
                    }
                }
            } else if (!z2 && (rect2.left > getFreeformStartLeft(rect) || rect2.top > getFreeformStartTop(rect))) {
                break;
            }
        }
        rect2.set(this.mTmpOriginal);
        rect3.set(rect2);
    }

    private boolean shiftedTooFar(Rect rect, Rect rect2, int i) {
        switch (i) {
            case 2:
                if (rect.right > rect2.right) {
                }
                break;
            case 3:
                if (rect.left < rect2.left) {
                }
                break;
            default:
                if (rect.right > rect2.right || rect.bottom > rect2.bottom) {
                }
                break;
        }
        return true;
    }

    private void shiftStartingPoint(Rect rect, Rect rect2, int i) {
        int horizontalStep = getHorizontalStep(rect2);
        int verticalStep = getVerticalStep(rect2);
        switch (i) {
            case 2:
                rect.offset(horizontalStep, 0);
                break;
            case 3:
                rect.offset(-horizontalStep, 0);
                break;
            default:
                rect.offset(horizontalStep, verticalStep);
                break;
        }
    }

    private static boolean boundsConflict(Rect rect, ArrayList<TaskRecord> arrayList) {
        for (int size = arrayList.size() - 1; size >= 0; size--) {
            TaskRecord taskRecord = arrayList.get(size);
            if (!taskRecord.mActivities.isEmpty() && !taskRecord.matchParentBounds()) {
                Rect overrideBounds = taskRecord.getOverrideBounds();
                if (closeLeftTopCorner(rect, overrideBounds) || closeRightTopCorner(rect, overrideBounds) || closeLeftBottomCorner(rect, overrideBounds) || closeRightBottomCorner(rect, overrideBounds)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static final boolean closeLeftTopCorner(Rect rect, Rect rect2) {
        return Math.abs(rect.left - rect2.left) < 4 && Math.abs(rect.top - rect2.top) < 4;
    }

    private static final boolean closeRightTopCorner(Rect rect, Rect rect2) {
        return Math.abs(rect.right - rect2.right) < 4 && Math.abs(rect.top - rect2.top) < 4;
    }

    private static final boolean closeLeftBottomCorner(Rect rect, Rect rect2) {
        return Math.abs(rect.left - rect2.left) < 4 && Math.abs(rect.bottom - rect2.bottom) < 4;
    }

    private static final boolean closeRightBottomCorner(Rect rect, Rect rect2) {
        return Math.abs(rect.right - rect2.right) < 4 && Math.abs(rect.bottom - rect2.bottom) < 4;
    }
}
