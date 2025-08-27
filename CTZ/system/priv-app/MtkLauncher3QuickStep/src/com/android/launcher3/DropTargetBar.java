package com.android.launcher3;

import android.animation.TimeInterpolator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewDebug;
import android.view.ViewPropertyAnimator;
import android.widget.FrameLayout;
import com.android.launcher3.DropTarget;
import com.android.launcher3.anim.AlphaUpdateListener;
import com.android.launcher3.anim.Interpolators;
import com.android.launcher3.dragndrop.DragController;
import com.android.launcher3.dragndrop.DragOptions;

/* loaded from: classes.dex */
public class DropTargetBar extends FrameLayout implements DragController.DragListener, Insettable {
    protected static final int DEFAULT_DRAG_FADE_DURATION = 175;
    protected static final TimeInterpolator DEFAULT_INTERPOLATOR = Interpolators.ACCEL;
    private ViewPropertyAnimator mCurrentAnimation;

    @ViewDebug.ExportedProperty(category = "launcher")
    protected boolean mDeferOnDragEnd;
    private ButtonDropTarget[] mDropTargets;
    private final Runnable mFadeAnimationEndRunnable;
    private boolean mIsVertical;

    @ViewDebug.ExportedProperty(category = "launcher")
    protected boolean mVisible;

    public DropTargetBar(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mFadeAnimationEndRunnable = new Runnable() { // from class: com.android.launcher3.-$$Lambda$DropTargetBar$u4vE0cw_SSJuVFB14YDC16eZe14
            @Override // java.lang.Runnable
            public final void run() {
                AlphaUpdateListener.updateVisibility(this.f$0);
            }
        };
        this.mVisible = false;
        this.mIsVertical = true;
    }

    public DropTargetBar(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.mFadeAnimationEndRunnable = new Runnable() { // from class: com.android.launcher3.-$$Lambda$DropTargetBar$u4vE0cw_SSJuVFB14YDC16eZe14
            @Override // java.lang.Runnable
            public final void run() {
                AlphaUpdateListener.updateVisibility(this.f$0);
            }
        };
        this.mVisible = false;
        this.mIsVertical = true;
    }

    @Override // android.view.View
    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mDropTargets = new ButtonDropTarget[getChildCount()];
        for (int i = 0; i < this.mDropTargets.length; i++) {
            this.mDropTargets[i] = (ButtonDropTarget) getChildAt(i);
            this.mDropTargets[i].setDropTargetBar(this);
        }
    }

    @Override // com.android.launcher3.Insettable
    public void setInsets(Rect rect) {
        int i;
        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) getLayoutParams();
        DeviceProfile deviceProfile = Launcher.getLauncher(getContext()).getDeviceProfile();
        this.mIsVertical = deviceProfile.isVerticalBarLayout();
        layoutParams.leftMargin = rect.left;
        layoutParams.topMargin = rect.top;
        layoutParams.bottomMargin = rect.bottom;
        layoutParams.rightMargin = rect.right;
        int i2 = 1;
        if (deviceProfile.isVerticalBarLayout()) {
            layoutParams.width = deviceProfile.dropTargetBarSizePx;
            layoutParams.height = deviceProfile.availableHeightPx - (deviceProfile.edgeMarginPx * 2);
            layoutParams.gravity = deviceProfile.isSeascape() ? 5 : 3;
            if (!deviceProfile.isSeascape()) {
                i2 = 2;
            }
        } else {
            if (deviceProfile.isTablet) {
                i = (((deviceProfile.widthPx - (deviceProfile.edgeMarginPx * 2)) - (deviceProfile.inv.numColumns * deviceProfile.cellWidthPx)) / ((deviceProfile.inv.numColumns + 1) * 2)) + deviceProfile.edgeMarginPx;
            } else {
                i = deviceProfile.desiredWorkspaceLeftRightMarginPx - deviceProfile.defaultWidgetPadding.right;
            }
            layoutParams.width = deviceProfile.availableWidthPx - (2 * i);
            layoutParams.topMargin += deviceProfile.edgeMarginPx;
            layoutParams.height = deviceProfile.dropTargetBarSizePx;
            layoutParams.gravity = 49;
            i2 = 0;
        }
        setLayoutParams(layoutParams);
        for (ButtonDropTarget buttonDropTarget : this.mDropTargets) {
            buttonDropTarget.setToolTipLocation(i2);
        }
    }

    public void setup(DragController dragController) {
        dragController.addDragListener(this);
        for (int i = 0; i < this.mDropTargets.length; i++) {
            dragController.addDragListener(this.mDropTargets[i]);
            dragController.addDropTarget(this.mDropTargets[i]);
        }
    }

    @Override // android.widget.FrameLayout, android.view.View
    protected void onMeasure(int i, int i2) {
        int size = View.MeasureSpec.getSize(i);
        int size2 = View.MeasureSpec.getSize(i2);
        if (this.mIsVertical) {
            int iMakeMeasureSpec = View.MeasureSpec.makeMeasureSpec(size, 1073741824);
            int iMakeMeasureSpec2 = View.MeasureSpec.makeMeasureSpec(size2, Integer.MIN_VALUE);
            for (ButtonDropTarget buttonDropTarget : this.mDropTargets) {
                if (buttonDropTarget.getVisibility() != 8) {
                    buttonDropTarget.setTextVisible(false);
                    buttonDropTarget.measure(iMakeMeasureSpec, iMakeMeasureSpec2);
                }
            }
        } else {
            int visibleButtonsCount = size / getVisibleButtonsCount();
            boolean z = true;
            for (ButtonDropTarget buttonDropTarget2 : this.mDropTargets) {
                if (buttonDropTarget2.getVisibility() != 8) {
                    z = z && !buttonDropTarget2.isTextTruncated(visibleButtonsCount);
                }
            }
            int iMakeMeasureSpec3 = View.MeasureSpec.makeMeasureSpec(visibleButtonsCount, Integer.MIN_VALUE);
            int iMakeMeasureSpec4 = View.MeasureSpec.makeMeasureSpec(size2, 1073741824);
            for (ButtonDropTarget buttonDropTarget3 : this.mDropTargets) {
                if (buttonDropTarget3.getVisibility() != 8) {
                    buttonDropTarget3.setTextVisible(z);
                    buttonDropTarget3.measure(iMakeMeasureSpec3, iMakeMeasureSpec4);
                }
            }
        }
        setMeasuredDimension(size, size2);
    }

    @Override // android.widget.FrameLayout, android.view.ViewGroup, android.view.View
    protected void onLayout(boolean z, int i, int i2, int i3, int i4) throws Resources.NotFoundException {
        if (this.mIsVertical) {
            int dimensionPixelSize = getResources().getDimensionPixelSize(R.dimen.drop_target_vertical_gap);
            int i5 = dimensionPixelSize;
            for (ButtonDropTarget buttonDropTarget : this.mDropTargets) {
                if (buttonDropTarget.getVisibility() != 8) {
                    int measuredHeight = buttonDropTarget.getMeasuredHeight() + i5;
                    buttonDropTarget.layout(0, i5, buttonDropTarget.getMeasuredWidth(), measuredHeight);
                    i5 = measuredHeight + dimensionPixelSize;
                }
            }
            return;
        }
        int visibleButtonsCount = (i3 - i) / getVisibleButtonsCount();
        int i6 = visibleButtonsCount / 2;
        for (ButtonDropTarget buttonDropTarget2 : this.mDropTargets) {
            if (buttonDropTarget2.getVisibility() != 8) {
                int measuredWidth = buttonDropTarget2.getMeasuredWidth() / 2;
                buttonDropTarget2.layout(i6 - measuredWidth, 0, measuredWidth + i6, buttonDropTarget2.getMeasuredHeight());
                i6 += visibleButtonsCount;
            }
        }
    }

    private int getVisibleButtonsCount() {
        int i = 0;
        for (ButtonDropTarget buttonDropTarget : this.mDropTargets) {
            if (buttonDropTarget.getVisibility() != 8) {
                i++;
            }
        }
        return i;
    }

    private void animateToVisibility(boolean z) {
        if (this.mVisible != z) {
            this.mVisible = z;
            if (this.mCurrentAnimation != null) {
                this.mCurrentAnimation.cancel();
                this.mCurrentAnimation = null;
            }
            float f = this.mVisible ? 1.0f : 0.0f;
            if (Float.compare(getAlpha(), f) != 0) {
                setVisibility(0);
                this.mCurrentAnimation = animate().alpha(f).setInterpolator(DEFAULT_INTERPOLATOR).setDuration(175L).withEndAction(this.mFadeAnimationEndRunnable);
            }
        }
    }

    @Override // com.android.launcher3.dragndrop.DragController.DragListener
    public void onDragStart(DropTarget.DragObject dragObject, DragOptions dragOptions) {
        animateToVisibility(true);
    }

    protected void deferOnDragEnd() {
        this.mDeferOnDragEnd = true;
    }

    @Override // com.android.launcher3.dragndrop.DragController.DragListener
    public void onDragEnd() {
        if (!this.mDeferOnDragEnd) {
            animateToVisibility(false);
        } else {
            this.mDeferOnDragEnd = false;
        }
    }

    public ButtonDropTarget[] getDropTargets() {
        return this.mDropTargets;
    }
}
