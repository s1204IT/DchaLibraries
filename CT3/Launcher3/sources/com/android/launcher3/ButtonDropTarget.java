package com.android.launcher3;

import android.animation.AnimatorSet;
import android.animation.FloatArrayEvaluator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.widget.TextView;
import com.android.launcher3.DragController;
import com.android.launcher3.DropTarget;

public abstract class ButtonDropTarget extends TextView implements DropTarget, DragController.DragListener, View.OnClickListener {
    private static int DRAG_VIEW_DROP_DURATION = 285;
    protected boolean mActive;
    private int mBottomDragPadding;
    private AnimatorSet mCurrentColorAnim;
    ColorMatrix mCurrentFilter;
    protected Drawable mDrawable;
    ColorMatrix mDstFilter;
    protected int mHoverColor;
    protected Launcher mLauncher;
    protected ColorStateList mOriginalTextColor;
    protected SearchDropTargetBar mSearchDropTargetBar;
    ColorMatrix mSrcFilter;

    abstract void completeDrop(DropTarget.DragObject dragObject);

    protected abstract boolean supportsDrop(DragSource dragSource, Object obj);

    public ButtonDropTarget(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ButtonDropTarget(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mHoverColor = 0;
        this.mBottomDragPadding = getResources().getDimensionPixelSize(R.dimen.drop_target_drag_padding);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mOriginalTextColor = getTextColors();
        DeviceProfile grid = ((Launcher) getContext()).getDeviceProfile();
        if (!grid.isVerticalBarLayout()) {
            return;
        }
        setText("");
    }

    @TargetApi(17)
    protected void setDrawable(int resId) {
        this.mDrawable = getResources().getDrawable(resId);
        if (Utilities.ATLEAST_JB_MR1) {
            setCompoundDrawablesRelativeWithIntrinsicBounds(this.mDrawable, (Drawable) null, (Drawable) null, (Drawable) null);
        } else {
            setCompoundDrawablesWithIntrinsicBounds(this.mDrawable, (Drawable) null, (Drawable) null, (Drawable) null);
        }
    }

    public void setLauncher(Launcher launcher) {
        this.mLauncher = launcher;
    }

    public void setSearchDropTargetBar(SearchDropTargetBar searchDropTargetBar) {
        this.mSearchDropTargetBar = searchDropTargetBar;
    }

    @Override
    public void onFlingToDelete(DropTarget.DragObject d, PointF vec) {
    }

    @Override
    public final void onDragEnter(DropTarget.DragObject d) {
        d.dragView.setColor(this.mHoverColor);
        if (Utilities.ATLEAST_LOLLIPOP) {
            animateTextColor(this.mHoverColor);
        } else {
            if (this.mCurrentFilter == null) {
                this.mCurrentFilter = new ColorMatrix();
            }
            DragView.setColorScale(this.mHoverColor, this.mCurrentFilter);
            this.mDrawable.setColorFilter(new ColorMatrixColorFilter(this.mCurrentFilter));
            setTextColor(this.mHoverColor);
        }
        if (d.stateAnnouncer != null) {
            d.stateAnnouncer.cancel();
        }
        sendAccessibilityEvent(4);
    }

    @Override
    public void onDragOver(DropTarget.DragObject d) {
    }

    protected void resetHoverColor() {
        if (Utilities.ATLEAST_LOLLIPOP) {
            animateTextColor(this.mOriginalTextColor.getDefaultColor());
        } else {
            this.mDrawable.setColorFilter(null);
            setTextColor(this.mOriginalTextColor);
        }
    }

    @TargetApi(21)
    private void animateTextColor(int targetColor) {
        if (this.mCurrentColorAnim != null) {
            this.mCurrentColorAnim.cancel();
        }
        this.mCurrentColorAnim = new AnimatorSet();
        this.mCurrentColorAnim.setDuration(DragView.COLOR_CHANGE_DURATION);
        if (this.mSrcFilter == null) {
            this.mSrcFilter = new ColorMatrix();
            this.mDstFilter = new ColorMatrix();
            this.mCurrentFilter = new ColorMatrix();
        }
        DragView.setColorScale(getTextColor(), this.mSrcFilter);
        DragView.setColorScale(targetColor, this.mDstFilter);
        ValueAnimator anim1 = ValueAnimator.ofObject(new FloatArrayEvaluator(this.mCurrentFilter.getArray()), this.mSrcFilter.getArray(), this.mDstFilter.getArray());
        anim1.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                ButtonDropTarget.this.mDrawable.setColorFilter(new ColorMatrixColorFilter(ButtonDropTarget.this.mCurrentFilter));
                ButtonDropTarget.this.invalidate();
            }
        });
        this.mCurrentColorAnim.play(anim1);
        this.mCurrentColorAnim.play(ObjectAnimator.ofArgb(this, "textColor", targetColor));
        this.mCurrentColorAnim.start();
    }

    @Override
    public final void onDragExit(DropTarget.DragObject d) {
        if (!d.dragComplete) {
            d.dragView.setColor(0);
            resetHoverColor();
        } else {
            d.dragView.setColor(this.mHoverColor);
        }
    }

    @Override
    public final void onDragStart(DragSource source, Object info, int dragAction) {
        this.mActive = supportsDrop(source, info);
        this.mDrawable.setColorFilter(null);
        if (this.mCurrentColorAnim != null) {
            this.mCurrentColorAnim.cancel();
            this.mCurrentColorAnim = null;
        }
        setTextColor(this.mOriginalTextColor);
        ((ViewGroup) getParent()).setVisibility(this.mActive ? 0 : 8);
    }

    @Override
    public final boolean acceptDrop(DropTarget.DragObject dragObject) {
        return supportsDrop(dragObject.dragSource, dragObject.dragInfo);
    }

    @Override
    public boolean isDropEnabled() {
        return this.mActive;
    }

    @Override
    public void onDragEnd() {
        this.mActive = false;
    }

    @Override
    public void onDrop(final DropTarget.DragObject d) {
        DragLayer dragLayer = this.mLauncher.getDragLayer();
        Rect from = new Rect();
        dragLayer.getViewRectRelativeToSelf(d.dragView, from);
        int width = this.mDrawable.getIntrinsicWidth();
        int height = this.mDrawable.getIntrinsicHeight();
        Rect to = getIconRect(d.dragView.getMeasuredWidth(), d.dragView.getMeasuredHeight(), width, height);
        float scale = to.width() / from.width();
        this.mSearchDropTargetBar.deferOnDragEnd();
        Runnable onAnimationEndRunnable = new Runnable() {
            @Override
            public void run() {
                ButtonDropTarget.this.completeDrop(d);
                ButtonDropTarget.this.mSearchDropTargetBar.onDragEnd();
                ButtonDropTarget.this.mLauncher.exitSpringLoadedDragModeDelayed(true, 0, null);
            }
        };
        dragLayer.animateView(d.dragView, from, to, scale, 1.0f, 1.0f, 0.1f, 0.1f, DRAG_VIEW_DROP_DURATION, new DecelerateInterpolator(2.0f), new LinearInterpolator(), onAnimationEndRunnable, 0, null);
    }

    @Override
    public void prepareAccessibilityDrop() {
    }

    @Override
    public void getHitRectRelativeToDragLayer(Rect outRect) {
        super.getHitRect(outRect);
        outRect.bottom += this.mBottomDragPadding;
        int[] coords = new int[2];
        this.mLauncher.getDragLayer().getDescendantCoordRelativeToSelf(this, coords);
        outRect.offsetTo(coords[0], coords[1]);
    }

    protected Rect getIconRect(int viewWidth, int viewHeight, int drawableWidth, int drawableHeight) {
        int left;
        int right;
        DragLayer dragLayer = this.mLauncher.getDragLayer();
        Rect to = new Rect();
        dragLayer.getViewRectRelativeToSelf(this, to);
        if (Utilities.isRtl(getResources())) {
            right = to.right - getPaddingRight();
            left = right - drawableWidth;
        } else {
            left = to.left + getPaddingLeft();
            right = left + drawableWidth;
        }
        int top = to.top + ((getMeasuredHeight() - drawableHeight) / 2);
        int bottom = top + drawableHeight;
        to.set(left, top, right, bottom);
        int xOffset = (-(viewWidth - drawableWidth)) / 2;
        int yOffset = (-(viewHeight - drawableHeight)) / 2;
        to.offset(xOffset, yOffset);
        return to;
    }

    public void enableAccessibleDrag(boolean enable) {
        setOnClickListener(enable ? this : null);
    }

    @Override
    public void onClick(View v) {
        LauncherAppState.getInstance().getAccessibilityDelegate().handleAccessibleDrop(this, null, null);
    }

    public int getTextColor() {
        return getTextColors().getDefaultColor();
    }
}
