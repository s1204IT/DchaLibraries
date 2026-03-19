package com.android.internal.policy;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Rect;
import android.hardware.display.DisplayManager;
import android.view.DisplayInfo;
import com.android.internal.R;
import java.util.ArrayList;

public class DividerSnapAlgorithm {
    private static final int MIN_DISMISS_VELOCITY_DP_PER_SECOND = 600;
    private static final int MIN_FLING_VELOCITY_DP_PER_SECOND = 400;
    private static final int SNAP_FIXED_RATIO = 1;
    private static final int SNAP_MODE_16_9 = 0;
    private static final int SNAP_MODE_MINIMIZED = 3;
    private static final int SNAP_ONLY_1_1 = 2;
    private final SnapTarget mDismissEndTarget;
    private final SnapTarget mDismissStartTarget;
    private final int mDisplayHeight;
    private final int mDisplayWidth;
    private final int mDividerSize;
    private final SnapTarget mFirstSplitTarget;
    private final float mFixedRatio;
    private final Rect mInsets;
    private boolean mIsHorizontalDivision;
    private final SnapTarget mLastSplitTarget;
    private final SnapTarget mMiddleTarget;
    private final float mMinDismissVelocityPxPerSecond;
    private final float mMinFlingVelocityPxPerSecond;
    private final int mMinimalSizeResizableTask;
    private final int mSnapMode;
    private final ArrayList<SnapTarget> mTargets;
    private final int mTaskHeightInMinimizedMode;

    public static DividerSnapAlgorithm create(Context context, Rect rect) {
        DisplayInfo displayInfo = new DisplayInfo();
        ((DisplayManager) context.getSystemService(DisplayManager.class)).getDisplay(0).getDisplayInfo(displayInfo);
        return new DividerSnapAlgorithm(context.getResources(), displayInfo.logicalWidth, displayInfo.logicalHeight, context.getResources().getDimensionPixelSize(R.dimen.docked_stack_divider_thickness) - (2 * context.getResources().getDimensionPixelSize(R.dimen.docked_stack_divider_insets)), context.getApplicationContext().getResources().getConfiguration().orientation == 1, rect);
    }

    public DividerSnapAlgorithm(Resources resources, int i, int i2, int i3, boolean z, Rect rect) {
        this(resources, i, i2, i3, z, rect, -1, false);
    }

    public DividerSnapAlgorithm(Resources resources, int i, int i2, int i3, boolean z, Rect rect, int i4) {
        this(resources, i, i2, i3, z, rect, i4, false);
    }

    public DividerSnapAlgorithm(Resources resources, int i, int i2, int i3, boolean z, Rect rect, int i4, boolean z2) {
        this.mTargets = new ArrayList<>();
        this.mInsets = new Rect();
        this.mMinFlingVelocityPxPerSecond = 400.0f * resources.getDisplayMetrics().density;
        this.mMinDismissVelocityPxPerSecond = 600.0f * resources.getDisplayMetrics().density;
        this.mDividerSize = i3;
        this.mDisplayWidth = i;
        this.mDisplayHeight = i2;
        this.mIsHorizontalDivision = z;
        this.mInsets.set(rect);
        this.mSnapMode = z2 ? 3 : resources.getInteger(R.integer.config_dockedStackDividerSnapMode);
        this.mFixedRatio = resources.getFraction(R.fraction.docked_stack_divider_fixed_ratio, 1, 1);
        this.mMinimalSizeResizableTask = resources.getDimensionPixelSize(R.dimen.default_minimal_size_resizable_task);
        this.mTaskHeightInMinimizedMode = resources.getDimensionPixelSize(R.dimen.task_height_of_minimized_mode);
        calculateTargets(z, i4);
        this.mFirstSplitTarget = this.mTargets.get(1);
        this.mLastSplitTarget = this.mTargets.get(this.mTargets.size() - 2);
        this.mDismissStartTarget = this.mTargets.get(0);
        this.mDismissEndTarget = this.mTargets.get(this.mTargets.size() - 1);
        this.mMiddleTarget = this.mTargets.get(this.mTargets.size() / 2);
    }

    public boolean isSplitScreenFeasible() {
        int i;
        int i2 = this.mInsets.top;
        int i3 = this.mIsHorizontalDivision ? this.mInsets.bottom : this.mInsets.right;
        if (this.mIsHorizontalDivision) {
            i = this.mDisplayHeight;
        } else {
            i = this.mDisplayWidth;
        }
        return (((i - i3) - i2) - this.mDividerSize) / 2 >= this.mMinimalSizeResizableTask;
    }

    public SnapTarget calculateSnapTarget(int i, float f) {
        return calculateSnapTarget(i, f, true);
    }

    public SnapTarget calculateSnapTarget(int i, float f, boolean z) {
        if (i < this.mFirstSplitTarget.position && f < (-this.mMinDismissVelocityPxPerSecond)) {
            return this.mDismissStartTarget;
        }
        if (i > this.mLastSplitTarget.position && f > this.mMinDismissVelocityPxPerSecond) {
            return this.mDismissEndTarget;
        }
        if (Math.abs(f) < this.mMinFlingVelocityPxPerSecond) {
            return snap(i, z);
        }
        if (f < 0.0f) {
            return this.mFirstSplitTarget;
        }
        return this.mLastSplitTarget;
    }

    public SnapTarget calculateNonDismissingSnapTarget(int i) {
        SnapTarget snapTargetSnap = snap(i, false);
        if (snapTargetSnap == this.mDismissStartTarget) {
            return this.mFirstSplitTarget;
        }
        if (snapTargetSnap == this.mDismissEndTarget) {
            return this.mLastSplitTarget;
        }
        return snapTargetSnap;
    }

    public float calculateDismissingFraction(int i) {
        if (i < this.mFirstSplitTarget.position) {
            return 1.0f - ((i - getStartInset()) / (this.mFirstSplitTarget.position - getStartInset()));
        }
        if (i > this.mLastSplitTarget.position) {
            return (i - this.mLastSplitTarget.position) / ((this.mDismissEndTarget.position - this.mLastSplitTarget.position) - this.mDividerSize);
        }
        return 0.0f;
    }

    public SnapTarget getClosestDismissTarget(int i) {
        if (i < this.mFirstSplitTarget.position) {
            return this.mDismissStartTarget;
        }
        if (i > this.mLastSplitTarget.position) {
            return this.mDismissEndTarget;
        }
        if (i - this.mDismissStartTarget.position < this.mDismissEndTarget.position - i) {
            return this.mDismissStartTarget;
        }
        return this.mDismissEndTarget;
    }

    public SnapTarget getFirstSplitTarget() {
        return this.mFirstSplitTarget;
    }

    public SnapTarget getLastSplitTarget() {
        return this.mLastSplitTarget;
    }

    public SnapTarget getDismissStartTarget() {
        return this.mDismissStartTarget;
    }

    public SnapTarget getDismissEndTarget() {
        return this.mDismissEndTarget;
    }

    private int getStartInset() {
        if (this.mIsHorizontalDivision) {
            return this.mInsets.top;
        }
        return this.mInsets.left;
    }

    private int getEndInset() {
        if (this.mIsHorizontalDivision) {
            return this.mInsets.bottom;
        }
        return this.mInsets.right;
    }

    private SnapTarget snap(int i, boolean z) {
        int size = this.mTargets.size();
        int i2 = -1;
        float f = Float.MAX_VALUE;
        for (int i3 = 0; i3 < size; i3++) {
            SnapTarget snapTarget = this.mTargets.get(i3);
            float fAbs = Math.abs(i - snapTarget.position);
            if (z) {
                fAbs /= snapTarget.distanceMultiplier;
            }
            if (fAbs < f) {
                i2 = i3;
                f = fAbs;
            }
        }
        return this.mTargets.get(i2);
    }

    private void calculateTargets(boolean z, int i) {
        int i2;
        this.mTargets.clear();
        if (z) {
            i2 = this.mDisplayHeight;
        } else {
            i2 = this.mDisplayWidth;
        }
        int i3 = z ? this.mInsets.bottom : this.mInsets.right;
        int i4 = -this.mDividerSize;
        if (i == 3) {
            i4 += this.mInsets.left;
        }
        this.mTargets.add(new SnapTarget(i4, i4, 1, 0.35f));
        switch (this.mSnapMode) {
            case 0:
                addRatio16_9Targets(z, i2);
                break;
            case 1:
                addFixedDivisionTargets(z, i2);
                break;
            case 2:
                addMiddleTarget(z);
                break;
            case 3:
                addMinimizedTarget(z, i);
                break;
        }
        this.mTargets.add(new SnapTarget(i2 - i3, i2, 2, 0.35f));
    }

    private void addNonDismissingTargets(boolean z, int i, int i2, int i3) {
        maybeAddTarget(i, i - this.mInsets.top);
        addMiddleTarget(z);
        maybeAddTarget(i2, (i3 - this.mInsets.bottom) - (this.mDividerSize + i2));
    }

    private void addFixedDivisionTargets(boolean z, int i) {
        int i2;
        int i3 = z ? this.mInsets.top : this.mInsets.left;
        if (z) {
            i2 = this.mDisplayHeight - this.mInsets.bottom;
        } else {
            i2 = this.mDisplayWidth - this.mInsets.right;
        }
        int i4 = ((int) (this.mFixedRatio * (i2 - i3))) - (this.mDividerSize / 2);
        addNonDismissingTargets(z, i3 + i4, (i2 - i4) - this.mDividerSize, i);
    }

    private void addRatio16_9Targets(boolean z, int i) {
        int i2;
        int i3;
        int i4 = z ? this.mInsets.top : this.mInsets.left;
        if (z) {
            i2 = this.mDisplayHeight - this.mInsets.bottom;
        } else {
            i2 = this.mDisplayWidth - this.mInsets.right;
        }
        int i5 = z ? this.mInsets.left : this.mInsets.top;
        if (z) {
            i3 = this.mDisplayWidth - this.mInsets.right;
        } else {
            i3 = this.mDisplayHeight - this.mInsets.bottom;
        }
        int iFloor = (int) Math.floor(0.5625f * (i3 - i5));
        addNonDismissingTargets(z, i4 + iFloor, (i2 - iFloor) - this.mDividerSize, i);
    }

    private void maybeAddTarget(int i, int i2) {
        if (i2 >= this.mMinimalSizeResizableTask) {
            this.mTargets.add(new SnapTarget(i, i, 0));
        }
    }

    private void addMiddleTarget(boolean z) {
        int iCalculateMiddlePosition = DockedDividerUtils.calculateMiddlePosition(z, this.mInsets, this.mDisplayWidth, this.mDisplayHeight, this.mDividerSize);
        this.mTargets.add(new SnapTarget(iCalculateMiddlePosition, iCalculateMiddlePosition, 0));
    }

    private void addMinimizedTarget(boolean z, int i) {
        int i2 = this.mTaskHeightInMinimizedMode + this.mInsets.top;
        if (!z) {
            if (i == 1) {
                i2 += this.mInsets.left;
            } else if (i == 3) {
                i2 = ((this.mDisplayWidth - i2) - this.mInsets.right) - this.mDividerSize;
            }
        }
        this.mTargets.add(new SnapTarget(i2, i2, 0));
    }

    public SnapTarget getMiddleTarget() {
        return this.mMiddleTarget;
    }

    public SnapTarget getNextTarget(SnapTarget snapTarget) {
        int iIndexOf = this.mTargets.indexOf(snapTarget);
        if (iIndexOf != -1 && iIndexOf < this.mTargets.size() - 1) {
            return this.mTargets.get(iIndexOf + 1);
        }
        return snapTarget;
    }

    public SnapTarget getPreviousTarget(SnapTarget snapTarget) {
        int iIndexOf = this.mTargets.indexOf(snapTarget);
        if (iIndexOf != -1 && iIndexOf > 0) {
            return this.mTargets.get(iIndexOf - 1);
        }
        return snapTarget;
    }

    public boolean showMiddleSplitTargetForAccessibility() {
        return this.mTargets.size() + (-2) > 1;
    }

    public boolean isFirstSplitTargetAvailable() {
        return this.mFirstSplitTarget != this.mMiddleTarget;
    }

    public boolean isLastSplitTargetAvailable() {
        return this.mLastSplitTarget != this.mMiddleTarget;
    }

    public SnapTarget cycleNonDismissTarget(SnapTarget snapTarget, int i) {
        int iIndexOf = this.mTargets.indexOf(snapTarget);
        if (iIndexOf != -1) {
            SnapTarget snapTarget2 = this.mTargets.get(((iIndexOf + this.mTargets.size()) + i) % this.mTargets.size());
            if (snapTarget2 == this.mDismissStartTarget) {
                return this.mLastSplitTarget;
            }
            if (snapTarget2 == this.mDismissEndTarget) {
                return this.mFirstSplitTarget;
            }
            return snapTarget2;
        }
        return snapTarget;
    }

    public static class SnapTarget {
        public static final int FLAG_DISMISS_END = 2;
        public static final int FLAG_DISMISS_START = 1;
        public static final int FLAG_NONE = 0;
        private final float distanceMultiplier;
        public final int flag;
        public final int position;
        public final int taskPosition;

        public SnapTarget(int i, int i2, int i3) {
            this(i, i2, i3, 1.0f);
        }

        public SnapTarget(int i, int i2, int i3, float f) {
            this.position = i;
            this.taskPosition = i2;
            this.flag = i3;
            this.distanceMultiplier = f;
        }
    }
}
