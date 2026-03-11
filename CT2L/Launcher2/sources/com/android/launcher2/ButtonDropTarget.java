package com.android.launcher2;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.TextView;
import com.android.launcher.R;
import com.android.launcher2.DragController;
import com.android.launcher2.DropTarget;

public class ButtonDropTarget extends TextView implements DragController.DragListener, DropTarget {
    protected boolean mActive;
    private int mBottomDragPadding;
    protected int mHoverColor;
    protected Launcher mLauncher;
    protected SearchDropTargetBar mSearchDropTargetBar;
    protected final int mTransitionDuration;

    public ButtonDropTarget(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ButtonDropTarget(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mHoverColor = 0;
        Resources r = getResources();
        this.mTransitionDuration = r.getInteger(R.integer.config_dropTargetBgTransitionDuration);
        this.mBottomDragPadding = r.getDimensionPixelSize(R.dimen.drop_target_drag_padding);
    }

    void setLauncher(Launcher launcher) {
        this.mLauncher = launcher;
    }

    @Override
    public boolean acceptDrop(DropTarget.DragObject d) {
        return false;
    }

    public void setSearchDropTargetBar(SearchDropTargetBar searchDropTargetBar) {
        this.mSearchDropTargetBar = searchDropTargetBar;
    }

    protected Drawable getCurrentDrawable() {
        Drawable[] drawables = getCompoundDrawablesRelative();
        for (int i = 0; i < drawables.length; i++) {
            if (drawables[i] != null) {
                return drawables[i];
            }
        }
        return null;
    }

    @Override
    public void onDrop(DropTarget.DragObject d) {
    }

    @Override
    public void onFlingToDelete(DropTarget.DragObject d, int x, int y, PointF vec) {
    }

    @Override
    public void onDragEnter(DropTarget.DragObject d) {
        d.dragView.setColor(this.mHoverColor);
    }

    @Override
    public void onDragOver(DropTarget.DragObject d) {
    }

    @Override
    public void onDragExit(DropTarget.DragObject d) {
        d.dragView.setColor(0);
    }

    @Override
    public void onDragStart(DragSource source, Object info, int dragAction) {
    }

    @Override
    public boolean isDropEnabled() {
        return this.mActive;
    }

    @Override
    public void onDragEnd() {
    }

    @Override
    public void getHitRect(Rect outRect) {
        super.getHitRect(outRect);
        outRect.bottom += this.mBottomDragPadding;
    }

    private boolean isRtl() {
        return getLayoutDirection() == 1;
    }

    Rect getIconRect(int viewWidth, int viewHeight, int drawableWidth, int drawableHeight) {
        int left;
        int right;
        DragLayer dragLayer = this.mLauncher.getDragLayer();
        Rect to = new Rect();
        dragLayer.getViewRectRelativeToSelf(this, to);
        if (isRtl()) {
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

    @Override
    public DropTarget getDropTargetDelegate(DropTarget.DragObject d) {
        return null;
    }

    @Override
    public void getLocationInDragLayer(int[] loc) {
        this.mLauncher.getDragLayer().getLocationInDragLayer(this, loc);
    }
}
