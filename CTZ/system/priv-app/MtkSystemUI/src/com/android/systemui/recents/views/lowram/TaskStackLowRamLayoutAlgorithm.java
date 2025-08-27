package com.android.systemui.recents.views.lowram;

import android.content.Context;
import android.graphics.Rect;
import android.view.ViewConfiguration;
import com.android.systemui.R;
import com.android.systemui.recents.Recents;
import com.android.systemui.recents.RecentsActivityLaunchState;
import com.android.systemui.recents.views.TaskStackLayoutAlgorithm;
import com.android.systemui.recents.views.TaskViewTransform;
import com.android.systemui.shared.recents.model.Task;
import com.android.systemui.shared.recents.utilities.Utilities;
import java.util.ArrayList;

/* loaded from: classes.dex */
public class TaskStackLowRamLayoutAlgorithm {
    private int mFlingThreshold;
    private int mPadding;
    private int mPaddingEndTopBottom;
    private int mPaddingLeftRight;
    private int mTopOffset;
    private Rect mWindowRect;
    private Rect mTaskRect = new Rect();
    private Rect mSystemInsets = new Rect();

    public TaskStackLowRamLayoutAlgorithm(Context context) {
        reloadOnConfigurationChange(context);
    }

    public void reloadOnConfigurationChange(Context context) {
        this.mPadding = context.getResources().getDimensionPixelSize(R.dimen.recents_layout_side_margin_phone);
        this.mFlingThreshold = ViewConfiguration.get(context).getScaledMinimumFlingVelocity();
    }

    public void initialize(Rect rect) {
        this.mWindowRect = rect;
        if (this.mWindowRect.height() > 0) {
            int iHeight = this.mWindowRect.height() - this.mSystemInsets.bottom;
            int iWidth = (this.mWindowRect.width() - this.mSystemInsets.right) - this.mSystemInsets.left;
            int iMin = Math.min(iWidth, iHeight) - (this.mPadding * 2);
            this.mTaskRect.set(0, 0, iMin, iWidth > iHeight ? (iMin * 2) / 3 : iMin);
            this.mPaddingLeftRight = (iWidth - this.mTaskRect.width()) / 2;
            this.mPaddingEndTopBottom = (iHeight - this.mTaskRect.height()) / 2;
            this.mTopOffset = (getTotalHeightOfTasks(9) - iHeight) / 2;
        }
    }

    public void setSystemInsets(Rect rect) {
        this.mSystemInsets = rect;
    }

    public TaskStackLayoutAlgorithm.VisibilityReport computeStackVisibilityReport(ArrayList<Task> arrayList) {
        int i;
        RecentsActivityLaunchState launchState = Recents.getConfiguration().getLaunchState();
        if (launchState.launchedFromHome || launchState.launchedFromPipApp || launchState.launchedWithNextPipApp) {
            i = 2;
        } else {
            i = 3;
        }
        int iMin = Math.min(i, arrayList.size());
        return new TaskStackLayoutAlgorithm.VisibilityReport(iMin, iMin);
    }

    public void getFrontOfStackTransform(TaskViewTransform taskViewTransform, TaskStackLayoutAlgorithm taskStackLayoutAlgorithm) {
        if (this.mWindowRect == null) {
            taskViewTransform.reset();
        } else {
            fillStackTransform(taskViewTransform, (((this.mWindowRect.height() - this.mSystemInsets.bottom) + this.mTaskRect.height()) / 2) + this.mTaskRect.height() + (this.mPadding * 2), taskStackLayoutAlgorithm.mMaxTranslationZ, true);
        }
    }

    public void getBackOfStackTransform(TaskViewTransform taskViewTransform, TaskStackLayoutAlgorithm taskStackLayoutAlgorithm) {
        if (this.mWindowRect == null) {
            taskViewTransform.reset();
        } else {
            fillStackTransform(taskViewTransform, (((this.mWindowRect.height() - this.mSystemInsets.bottom) - this.mTaskRect.height()) / 2) - ((this.mTaskRect.height() + this.mPadding) * 2), taskStackLayoutAlgorithm.mMaxTranslationZ, true);
        }
    }

    public TaskViewTransform getTransform(int i, float f, TaskViewTransform taskViewTransform, int i2, TaskStackLayoutAlgorithm taskStackLayoutAlgorithm) {
        int iHeight;
        if (i2 == 0) {
            taskViewTransform.reset();
            return taskViewTransform;
        }
        boolean z = true;
        if (i2 > 1) {
            iHeight = getTaskTopFromIndex(i) - percentageToScroll(f);
            if (this.mPadding + iHeight + getTaskRect().height() <= 0) {
                z = false;
            }
        } else {
            iHeight = (((this.mWindowRect.height() - this.mSystemInsets.bottom) - this.mTaskRect.height()) / 2) - percentageToScroll(f);
        }
        fillStackTransform(taskViewTransform, iHeight, taskStackLayoutAlgorithm.mMaxTranslationZ, z);
        return taskViewTransform;
    }

    public float getClosestTaskP(float f, int i, int i2) {
        int iPercentageToScroll = percentageToScroll(f);
        int taskTopFromIndex = getTaskTopFromIndex(0) - this.mPaddingEndTopBottom;
        int i3 = 1;
        while (i3 < i) {
            int taskTopFromIndex2 = getTaskTopFromIndex(i3) - this.mPaddingEndTopBottom;
            int i4 = taskTopFromIndex2 - iPercentageToScroll;
            if (i4 <= 0) {
                i3++;
                taskTopFromIndex = taskTopFromIndex2;
            } else {
                boolean z = i4 > Math.abs(iPercentageToScroll - taskTopFromIndex);
                if (Math.abs(i2) > this.mFlingThreshold) {
                    z = i2 > 0;
                }
                return z ? scrollToPercentage(taskTopFromIndex) : scrollToPercentage(taskTopFromIndex2);
            }
        }
        return scrollToPercentage(taskTopFromIndex);
    }

    public float scrollToPercentage(int i) {
        return i / (this.mTaskRect.height() + this.mPadding);
    }

    public int percentageToScroll(float f) {
        return (int) (f * (this.mTaskRect.height() + this.mPadding));
    }

    public float getMinScrollP() {
        return getScrollPForTask(0);
    }

    public float getMaxScrollP(int i) {
        return getScrollPForTask(i - 1);
    }

    public float getInitialScrollP(int i, boolean z) {
        if (z) {
            return getMaxScrollP(i);
        }
        if (i < 2) {
            return 0.0f;
        }
        return getScrollPForTask(i - 2);
    }

    public float getScrollPForTask(int i) {
        return scrollToPercentage(getTaskTopFromIndex(i) - this.mPaddingEndTopBottom);
    }

    public Rect getTaskRect() {
        return this.mTaskRect;
    }

    public float getMaxOverscroll() {
        return 0.6666666f;
    }

    private int getTaskTopFromIndex(int i) {
        return getTotalHeightOfTasks(i) - this.mTopOffset;
    }

    private int getTotalHeightOfTasks(int i) {
        return (this.mTaskRect.height() * i) + ((i + 1) * this.mPadding);
    }

    private void fillStackTransform(TaskViewTransform taskViewTransform, int i, int i2, boolean z) {
        taskViewTransform.scale = 1.0f;
        taskViewTransform.alpha = 1.0f;
        taskViewTransform.translationZ = i2;
        taskViewTransform.dimAlpha = 0.0f;
        taskViewTransform.viewOutlineAlpha = 1.0f;
        taskViewTransform.rect.set(getTaskRect());
        taskViewTransform.rect.offset(this.mPaddingLeftRight + this.mSystemInsets.left, i);
        Utilities.scaleRectAboutCenter(taskViewTransform.rect, taskViewTransform.scale);
        taskViewTransform.visible = z;
    }
}
