package com.android.launcher3;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.appwidget.AppWidgetHostView;
import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import com.android.launcher3.CellLayout;
import com.android.launcher3.accessibility.DragViewStateAnnouncer;
import com.android.launcher3.dragndrop.DragLayer;
import com.android.launcher3.util.FocusLogic;
import com.android.launcher3.views.BaseDragLayer;
import com.android.launcher3.widget.LauncherAppWidgetHostView;

/* loaded from: classes.dex */
public class AppWidgetResizeFrame extends AbstractFloatingView implements View.OnKeyListener {
    private static final float DIMMED_HANDLE_ALPHA = 0.0f;
    private static final int HANDLE_COUNT = 4;
    private static final int INDEX_BOTTOM = 3;
    private static final int INDEX_LEFT = 0;
    private static final int INDEX_RIGHT = 2;
    private static final int INDEX_TOP = 1;
    private static final float RESIZE_THRESHOLD = 0.66f;
    private static final int SNAP_DURATION = 150;
    private static Point[] sCellSize;
    private static final Rect sTmpRect = new Rect();
    private final int mBackgroundPadding;
    private final IntRange mBaselineX;
    private final IntRange mBaselineY;
    private boolean mBottomBorderActive;
    private int mBottomTouchRegionAdjustment;
    private CellLayout mCellLayout;
    private int mDeltaX;
    private int mDeltaXAddOn;
    private final IntRange mDeltaXRange;
    private int mDeltaY;
    private int mDeltaYAddOn;
    private final IntRange mDeltaYRange;
    private final int[] mDirectionVector;
    private final View[] mDragHandles;
    private DragLayer mDragLayer;
    private final int[] mLastDirectionVector;
    private final Launcher mLauncher;
    private boolean mLeftBorderActive;
    private int mMinHSpan;
    private int mMinVSpan;
    private int mResizeMode;
    private boolean mRightBorderActive;
    private int mRunningHInc;
    private int mRunningVInc;
    private final DragViewStateAnnouncer mStateAnnouncer;
    private final IntRange mTempRange1;
    private final IntRange mTempRange2;
    private boolean mTopBorderActive;
    private int mTopTouchRegionAdjustment;
    private final int mTouchTargetWidth;
    private Rect mWidgetPadding;
    private LauncherAppWidgetHostView mWidgetView;
    private int mXDown;
    private int mYDown;

    public AppWidgetResizeFrame(Context context) {
        this(context, null);
    }

    public AppWidgetResizeFrame(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public AppWidgetResizeFrame(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.mDragHandles = new View[4];
        this.mDirectionVector = new int[2];
        this.mLastDirectionVector = new int[2];
        this.mTempRange1 = new IntRange();
        this.mTempRange2 = new IntRange();
        this.mDeltaXRange = new IntRange();
        this.mBaselineX = new IntRange();
        this.mDeltaYRange = new IntRange();
        this.mBaselineY = new IntRange();
        this.mTopTouchRegionAdjustment = 0;
        this.mBottomTouchRegionAdjustment = 0;
        this.mLauncher = Launcher.getLauncher(context);
        this.mStateAnnouncer = DragViewStateAnnouncer.createFor(this);
        this.mBackgroundPadding = getResources().getDimensionPixelSize(R.dimen.resize_frame_background_padding);
        this.mTouchTargetWidth = 2 * this.mBackgroundPadding;
    }

    @Override // android.view.View
    protected void onFinishInflate() {
        super.onFinishInflate();
        ViewGroup viewGroup = (ViewGroup) getChildAt(0);
        for (int i = 0; i < 4; i++) {
            this.mDragHandles[i] = viewGroup.getChildAt(i);
        }
    }

    public static void showForWidget(LauncherAppWidgetHostView launcherAppWidgetHostView, CellLayout cellLayout) {
        Launcher launcher = Launcher.getLauncher(cellLayout.getContext());
        AbstractFloatingView.closeAllOpenViews(launcher);
        DragLayer dragLayer = launcher.getDragLayer();
        AppWidgetResizeFrame appWidgetResizeFrame = (AppWidgetResizeFrame) launcher.getLayoutInflater().inflate(R.layout.app_widget_resize_frame, (ViewGroup) dragLayer, false);
        appWidgetResizeFrame.setupForWidget(launcherAppWidgetHostView, cellLayout, dragLayer);
        ((BaseDragLayer.LayoutParams) appWidgetResizeFrame.getLayoutParams()).customPosition = true;
        dragLayer.addView(appWidgetResizeFrame);
        appWidgetResizeFrame.mIsOpen = true;
        appWidgetResizeFrame.snapToWidget(false);
    }

    private void setupForWidget(LauncherAppWidgetHostView launcherAppWidgetHostView, CellLayout cellLayout, DragLayer dragLayer) {
        this.mCellLayout = cellLayout;
        this.mWidgetView = launcherAppWidgetHostView;
        LauncherAppWidgetProviderInfo launcherAppWidgetProviderInfo = (LauncherAppWidgetProviderInfo) launcherAppWidgetHostView.getAppWidgetInfo();
        this.mResizeMode = launcherAppWidgetProviderInfo.resizeMode;
        this.mDragLayer = dragLayer;
        this.mMinHSpan = launcherAppWidgetProviderInfo.minSpanX;
        this.mMinVSpan = launcherAppWidgetProviderInfo.minSpanY;
        this.mWidgetPadding = AppWidgetHostView.getDefaultPaddingForWidget(getContext(), launcherAppWidgetHostView.getAppWidgetInfo().provider, null);
        if (this.mResizeMode == 1) {
            this.mDragHandles[1].setVisibility(8);
            this.mDragHandles[3].setVisibility(8);
        } else if (this.mResizeMode == 2) {
            this.mDragHandles[0].setVisibility(8);
            this.mDragHandles[2].setVisibility(8);
        }
        this.mCellLayout.markCellsAsUnoccupiedForView(this.mWidgetView);
        setOnKeyListener(this);
    }

    public boolean beginResizeIfPointInRegion(int i, int i2) {
        boolean z = (this.mResizeMode & 1) != 0;
        boolean z2 = (this.mResizeMode & 2) != 0;
        this.mLeftBorderActive = i < this.mTouchTargetWidth && z;
        this.mRightBorderActive = i > getWidth() - this.mTouchTargetWidth && z;
        this.mTopBorderActive = i2 < this.mTouchTargetWidth + this.mTopTouchRegionAdjustment && z2;
        this.mBottomBorderActive = i2 > (getHeight() - this.mTouchTargetWidth) + this.mBottomTouchRegionAdjustment && z2;
        boolean z3 = this.mLeftBorderActive || this.mRightBorderActive || this.mTopBorderActive || this.mBottomBorderActive;
        if (z3) {
            this.mDragHandles[0].setAlpha(this.mLeftBorderActive ? 1.0f : 0.0f);
            this.mDragHandles[2].setAlpha(this.mRightBorderActive ? 1.0f : 0.0f);
            this.mDragHandles[1].setAlpha(this.mTopBorderActive ? 1.0f : 0.0f);
            this.mDragHandles[3].setAlpha(this.mBottomBorderActive ? 1.0f : 0.0f);
        }
        if (this.mLeftBorderActive) {
            this.mDeltaXRange.set(-getLeft(), getWidth() - (this.mTouchTargetWidth * 2));
        } else if (this.mRightBorderActive) {
            this.mDeltaXRange.set((this.mTouchTargetWidth * 2) - getWidth(), this.mDragLayer.getWidth() - getRight());
        } else {
            this.mDeltaXRange.set(0, 0);
        }
        this.mBaselineX.set(getLeft(), getRight());
        if (this.mTopBorderActive) {
            this.mDeltaYRange.set(-getTop(), getHeight() - (2 * this.mTouchTargetWidth));
        } else if (this.mBottomBorderActive) {
            this.mDeltaYRange.set((2 * this.mTouchTargetWidth) - getHeight(), this.mDragLayer.getHeight() - getBottom());
        } else {
            this.mDeltaYRange.set(0, 0);
        }
        this.mBaselineY.set(getTop(), getBottom());
        return z3;
    }

    public void visualizeResizeForDelta(int i, int i2) {
        this.mDeltaX = this.mDeltaXRange.clamp(i);
        this.mDeltaY = this.mDeltaYRange.clamp(i2);
        BaseDragLayer.LayoutParams layoutParams = (BaseDragLayer.LayoutParams) getLayoutParams();
        this.mDeltaX = this.mDeltaXRange.clamp(i);
        this.mBaselineX.applyDelta(this.mLeftBorderActive, this.mRightBorderActive, this.mDeltaX, this.mTempRange1);
        layoutParams.x = this.mTempRange1.start;
        layoutParams.width = this.mTempRange1.size();
        this.mDeltaY = this.mDeltaYRange.clamp(i2);
        this.mBaselineY.applyDelta(this.mTopBorderActive, this.mBottomBorderActive, this.mDeltaY, this.mTempRange1);
        layoutParams.y = this.mTempRange1.start;
        layoutParams.height = this.mTempRange1.size();
        resizeWidgetIfNeeded(false);
        getSnappedRectRelativeToDragLayer(sTmpRect);
        if (this.mLeftBorderActive) {
            layoutParams.width = (sTmpRect.width() + sTmpRect.left) - layoutParams.x;
        }
        if (this.mTopBorderActive) {
            layoutParams.height = (sTmpRect.height() + sTmpRect.top) - layoutParams.y;
        }
        if (this.mRightBorderActive) {
            layoutParams.x = sTmpRect.left;
        }
        if (this.mBottomBorderActive) {
            layoutParams.y = sTmpRect.top;
        }
        requestLayout();
    }

    private static int getSpanIncrement(float f) {
        if (Math.abs(f) > RESIZE_THRESHOLD) {
            return Math.round(f);
        }
        return 0;
    }

    private void resizeWidgetIfNeeded(boolean z) {
        float cellWidth = this.mCellLayout.getCellWidth();
        float cellHeight = this.mCellLayout.getCellHeight();
        int spanIncrement = getSpanIncrement(((this.mDeltaX + this.mDeltaXAddOn) / cellWidth) - this.mRunningHInc);
        int spanIncrement2 = getSpanIncrement(((this.mDeltaY + this.mDeltaYAddOn) / cellHeight) - this.mRunningVInc);
        if (!z && spanIncrement == 0 && spanIncrement2 == 0) {
            return;
        }
        this.mDirectionVector[0] = 0;
        this.mDirectionVector[1] = 0;
        CellLayout.LayoutParams layoutParams = (CellLayout.LayoutParams) this.mWidgetView.getLayoutParams();
        int i = layoutParams.cellHSpan;
        int i2 = layoutParams.cellVSpan;
        int i3 = layoutParams.useTmpCoords ? layoutParams.tmpCellX : layoutParams.cellX;
        int i4 = layoutParams.useTmpCoords ? layoutParams.tmpCellY : layoutParams.cellY;
        this.mTempRange1.set(i3, i + i3);
        int iApplyDeltaAndBound = this.mTempRange1.applyDeltaAndBound(this.mLeftBorderActive, this.mRightBorderActive, spanIncrement, this.mMinHSpan, this.mCellLayout.getCountX(), this.mTempRange2);
        int i5 = this.mTempRange2.start;
        int size = this.mTempRange2.size();
        if (iApplyDeltaAndBound != 0) {
            this.mDirectionVector[0] = this.mLeftBorderActive ? -1 : 1;
        }
        this.mTempRange1.set(i4, i2 + i4);
        int iApplyDeltaAndBound2 = this.mTempRange1.applyDeltaAndBound(this.mTopBorderActive, this.mBottomBorderActive, spanIncrement2, this.mMinVSpan, this.mCellLayout.getCountY(), this.mTempRange2);
        int i6 = this.mTempRange2.start;
        int size2 = this.mTempRange2.size();
        if (iApplyDeltaAndBound2 != 0) {
            this.mDirectionVector[1] = this.mTopBorderActive ? -1 : 1;
        }
        if (!z && iApplyDeltaAndBound2 == 0 && iApplyDeltaAndBound == 0) {
            return;
        }
        if (z) {
            this.mDirectionVector[0] = this.mLastDirectionVector[0];
            this.mDirectionVector[1] = this.mLastDirectionVector[1];
        } else {
            this.mLastDirectionVector[0] = this.mDirectionVector[0];
            this.mLastDirectionVector[1] = this.mDirectionVector[1];
        }
        if (this.mCellLayout.createAreaForResize(i5, i6, size, size2, this.mWidgetView, this.mDirectionVector, z)) {
            if (this.mStateAnnouncer != null && (layoutParams.cellHSpan != size || layoutParams.cellVSpan != size2)) {
                this.mStateAnnouncer.announce(this.mLauncher.getString(R.string.widget_resized, new Object[]{Integer.valueOf(size), Integer.valueOf(size2)}));
            }
            layoutParams.tmpCellX = i5;
            layoutParams.tmpCellY = i6;
            layoutParams.cellHSpan = size;
            layoutParams.cellVSpan = size2;
            this.mRunningVInc += iApplyDeltaAndBound2;
            this.mRunningHInc += iApplyDeltaAndBound;
            if (!z) {
                updateWidgetSizeRanges(this.mWidgetView, this.mLauncher, size, size2);
            }
        }
        this.mWidgetView.requestLayout();
    }

    static void updateWidgetSizeRanges(AppWidgetHostView appWidgetHostView, Launcher launcher, int i, int i2) {
        getWidgetSizeRanges(launcher, i, i2, sTmpRect);
        appWidgetHostView.updateAppWidgetSize(null, sTmpRect.left, sTmpRect.top, sTmpRect.right, sTmpRect.bottom);
    }

    public static Rect getWidgetSizeRanges(Context context, int i, int i2, Rect rect) {
        if (sCellSize == null) {
            InvariantDeviceProfile idp = LauncherAppState.getIDP(context);
            sCellSize = new Point[2];
            sCellSize[0] = idp.landscapeProfile.getCellSize();
            sCellSize[1] = idp.portraitProfile.getCellSize();
        }
        if (rect == null) {
            rect = new Rect();
        }
        float f = context.getResources().getDisplayMetrics().density;
        rect.set((int) ((i * sCellSize[1].x) / f), (int) ((sCellSize[0].y * i2) / f), (int) ((sCellSize[0].x * i) / f), (int) ((i2 * sCellSize[1].y) / f));
        return rect;
    }

    @Override // android.view.ViewGroup, android.view.View
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        resizeWidgetIfNeeded(true);
    }

    private void onTouchUp() {
        int cellWidth = this.mCellLayout.getCellWidth();
        int cellHeight = this.mCellLayout.getCellHeight();
        this.mDeltaXAddOn = this.mRunningHInc * cellWidth;
        this.mDeltaYAddOn = this.mRunningVInc * cellHeight;
        this.mDeltaX = 0;
        this.mDeltaY = 0;
        post(new Runnable() { // from class: com.android.launcher3.AppWidgetResizeFrame.1
            @Override // java.lang.Runnable
            public void run() {
                AppWidgetResizeFrame.this.snapToWidget(true);
            }
        });
    }

    private void getSnappedRectRelativeToDragLayer(Rect rect) {
        float scaleToFit = this.mWidgetView.getScaleToFit();
        this.mDragLayer.getViewRectRelativeToSelf(this.mWidgetView, rect);
        int iWidth = (this.mBackgroundPadding * 2) + ((int) (((rect.width() - this.mWidgetPadding.left) - this.mWidgetPadding.right) * scaleToFit));
        int iHeight = (2 * this.mBackgroundPadding) + ((int) (((rect.height() - this.mWidgetPadding.top) - this.mWidgetPadding.bottom) * scaleToFit));
        int i = (int) ((rect.left - this.mBackgroundPadding) + (this.mWidgetPadding.left * scaleToFit));
        rect.left = i;
        rect.top = (int) ((rect.top - this.mBackgroundPadding) + (scaleToFit * this.mWidgetPadding.top));
        rect.right = rect.left + iWidth;
        rect.bottom = rect.top + iHeight;
    }

    private void snapToWidget(boolean z) {
        getSnappedRectRelativeToDragLayer(sTmpRect);
        int iWidth = sTmpRect.width();
        int iHeight = sTmpRect.height();
        int i = sTmpRect.left;
        int i2 = sTmpRect.top;
        if (i2 < 0) {
            this.mTopTouchRegionAdjustment = -i2;
        } else {
            this.mTopTouchRegionAdjustment = 0;
        }
        int i3 = i2 + iHeight;
        if (i3 > this.mDragLayer.getHeight()) {
            this.mBottomTouchRegionAdjustment = -(i3 - this.mDragLayer.getHeight());
        } else {
            this.mBottomTouchRegionAdjustment = 0;
        }
        BaseDragLayer.LayoutParams layoutParams = (BaseDragLayer.LayoutParams) getLayoutParams();
        if (!z) {
            layoutParams.width = iWidth;
            layoutParams.height = iHeight;
            layoutParams.x = i;
            layoutParams.y = i2;
            for (int i4 = 0; i4 < 4; i4++) {
                this.mDragHandles[i4].setAlpha(1.0f);
            }
            requestLayout();
        } else {
            ObjectAnimator objectAnimatorOfPropertyValuesHolder = LauncherAnimUtils.ofPropertyValuesHolder(layoutParams, this, PropertyValuesHolder.ofInt("width", layoutParams.width, iWidth), PropertyValuesHolder.ofInt("height", layoutParams.height, iHeight), PropertyValuesHolder.ofInt("x", layoutParams.x, i), PropertyValuesHolder.ofInt("y", layoutParams.y, i2));
            objectAnimatorOfPropertyValuesHolder.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() { // from class: com.android.launcher3.AppWidgetResizeFrame.2
                @Override // android.animation.ValueAnimator.AnimatorUpdateListener
                public void onAnimationUpdate(ValueAnimator valueAnimator) {
                    AppWidgetResizeFrame.this.requestLayout();
                }
            });
            AnimatorSet animatorSetCreateAnimatorSet = LauncherAnimUtils.createAnimatorSet();
            animatorSetCreateAnimatorSet.play(objectAnimatorOfPropertyValuesHolder);
            for (int i5 = 0; i5 < 4; i5++) {
                animatorSetCreateAnimatorSet.play(LauncherAnimUtils.ofFloat(this.mDragHandles[i5], ALPHA, 1.0f));
            }
            animatorSetCreateAnimatorSet.setDuration(150L);
            animatorSetCreateAnimatorSet.start();
        }
        setFocusableInTouchMode(true);
        requestFocus();
    }

    @Override // android.view.View.OnKeyListener
    public boolean onKey(View view, int i, KeyEvent keyEvent) {
        if (!FocusLogic.shouldConsume(i)) {
            return false;
        }
        close(false);
        this.mWidgetView.requestFocus();
        return true;
    }

    private boolean handleTouchDown(MotionEvent motionEvent) {
        Rect rect = new Rect();
        int x = (int) motionEvent.getX();
        int y = (int) motionEvent.getY();
        getHitRect(rect);
        if (rect.contains(x, y) && beginResizeIfPointInRegion(x - getLeft(), y - getTop())) {
            this.mXDown = x;
            this.mYDown = y;
            return true;
        }
        return false;
    }

    @Override // com.android.launcher3.AbstractFloatingView, com.android.launcher3.util.TouchController
    public boolean onControllerTouchEvent(MotionEvent motionEvent) {
        int action = motionEvent.getAction();
        int x = (int) motionEvent.getX();
        int y = (int) motionEvent.getY();
        switch (action) {
            case 0:
                return handleTouchDown(motionEvent);
            case 1:
            case 3:
                visualizeResizeForDelta(x - this.mXDown, y - this.mYDown);
                onTouchUp();
                this.mYDown = 0;
                this.mXDown = 0;
                return true;
            case 2:
                visualizeResizeForDelta(x - this.mXDown, y - this.mYDown);
                return true;
            default:
                return true;
        }
    }

    @Override // com.android.launcher3.util.TouchController
    public boolean onControllerInterceptTouchEvent(MotionEvent motionEvent) {
        if (motionEvent.getAction() == 0 && handleTouchDown(motionEvent)) {
            return true;
        }
        close(false);
        return false;
    }

    @Override // com.android.launcher3.AbstractFloatingView
    protected void handleClose(boolean z) {
        this.mDragLayer.removeView(this);
    }

    @Override // com.android.launcher3.AbstractFloatingView
    public void logActionCommand(int i) {
    }

    @Override // com.android.launcher3.AbstractFloatingView
    protected boolean isOfType(int i) {
        return (i & 8) != 0;
    }

    private static class IntRange {
        public int end;
        public int start;

        private IntRange() {
        }

        public int clamp(int i) {
            return Utilities.boundToRange(i, this.start, this.end);
        }

        public void set(int i, int i2) {
            this.start = i;
            this.end = i2;
        }

        public int size() {
            return this.end - this.start;
        }

        public void applyDelta(boolean z, boolean z2, int i, IntRange intRange) {
            intRange.start = z ? this.start + i : this.start;
            intRange.end = z2 ? this.end + i : this.end;
        }

        public int applyDeltaAndBound(boolean z, boolean z2, int i, int i2, int i3, IntRange intRange) {
            int size;
            int size2;
            applyDelta(z, z2, i, intRange);
            if (intRange.start < 0) {
                intRange.start = 0;
            }
            if (intRange.end > i3) {
                intRange.end = i3;
            }
            if (intRange.size() < i2) {
                if (z) {
                    intRange.start = intRange.end - i2;
                } else if (z2) {
                    intRange.end = intRange.start + i2;
                }
            }
            if (z2) {
                size = intRange.size();
                size2 = size();
            } else {
                size = size();
                size2 = intRange.size();
            }
            return size - size2;
        }
    }
}
