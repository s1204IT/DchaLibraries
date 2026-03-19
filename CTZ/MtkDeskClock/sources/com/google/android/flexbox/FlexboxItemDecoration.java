package com.google.android.flexbox;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import java.util.List;

public class FlexboxItemDecoration extends RecyclerView.ItemDecoration {
    public static final int BOTH = 3;
    public static final int HORIZONTAL = 1;
    private static final int[] LIST_DIVIDER_ATTRS = {android.R.attr.listDivider};
    public static final int VERTICAL = 2;
    private Drawable mDrawable;
    private int mOrientation;

    public FlexboxItemDecoration(Context context) {
        TypedArray a = context.obtainStyledAttributes(LIST_DIVIDER_ATTRS);
        this.mDrawable = a.getDrawable(0);
        a.recycle();
        setOrientation(3);
    }

    public void setDrawable(Drawable drawable) {
        if (drawable == null) {
            throw new IllegalArgumentException("Drawable cannot be null.");
        }
        this.mDrawable = drawable;
    }

    public void setOrientation(int orientation) {
        this.mOrientation = orientation;
    }

    @Override
    public void onDraw(Canvas canvas, RecyclerView parent, RecyclerView.State state) {
        drawHorizontalDecorations(canvas, parent);
        drawVerticalDecorations(canvas, parent);
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        int position = parent.getChildAdapterPosition(view);
        if (position == 0) {
            return;
        }
        if (!needsHorizontalDecoration() && !needsVerticalDecoration()) {
            outRect.set(0, 0, 0, 0);
            return;
        }
        FlexboxLayoutManager layoutManager = (FlexboxLayoutManager) parent.getLayoutManager();
        List<FlexLine> flexLines = layoutManager.getFlexLines();
        int flexDirection = layoutManager.getFlexDirection();
        setOffsetAlongMainAxis(outRect, position, layoutManager, flexLines, flexDirection);
        setOffsetAlongCrossAxis(outRect, position, layoutManager, flexLines);
    }

    private void setOffsetAlongCrossAxis(Rect outRect, int position, FlexboxLayoutManager layoutManager, List<FlexLine> flexLines) {
        if (flexLines.size() == 0) {
            return;
        }
        int flexLineIndex = layoutManager.getPositionToFlexLineIndex(position);
        if (flexLineIndex == 0) {
            return;
        }
        if (layoutManager.isMainAxisDirectionHorizontal()) {
            if (!needsHorizontalDecoration()) {
                outRect.top = 0;
                outRect.bottom = 0;
                return;
            } else {
                outRect.top = this.mDrawable.getIntrinsicHeight();
                outRect.bottom = 0;
                return;
            }
        }
        if (!needsVerticalDecoration()) {
            return;
        }
        if (layoutManager.isLayoutRtl()) {
            outRect.right = this.mDrawable.getIntrinsicWidth();
            outRect.left = 0;
        } else {
            outRect.left = this.mDrawable.getIntrinsicWidth();
            outRect.right = 0;
        }
    }

    private void setOffsetAlongMainAxis(Rect outRect, int position, FlexboxLayoutManager layoutManager, List<FlexLine> flexLines, int flexDirection) {
        if (isFirstItemInLine(position, flexLines, layoutManager)) {
            return;
        }
        if (layoutManager.isMainAxisDirectionHorizontal()) {
            if (!needsVerticalDecoration()) {
                outRect.left = 0;
                outRect.right = 0;
                return;
            } else if (layoutManager.isLayoutRtl()) {
                outRect.right = this.mDrawable.getIntrinsicWidth();
                outRect.left = 0;
                return;
            } else {
                outRect.left = this.mDrawable.getIntrinsicWidth();
                outRect.right = 0;
                return;
            }
        }
        if (!needsHorizontalDecoration()) {
            outRect.top = 0;
            outRect.bottom = 0;
        } else if (flexDirection == 3) {
            outRect.bottom = this.mDrawable.getIntrinsicHeight();
            outRect.top = 0;
        } else {
            outRect.top = this.mDrawable.getIntrinsicHeight();
            outRect.bottom = 0;
        }
    }

    private void drawVerticalDecorations(Canvas canvas, RecyclerView parent) {
        int right;
        int left;
        int top;
        int bottom;
        if (!needsVerticalDecoration()) {
            return;
        }
        FlexboxLayoutManager layoutManager = (FlexboxLayoutManager) parent.getLayoutManager();
        int parentTop = parent.getTop() - parent.getPaddingTop();
        int parentBottom = parent.getBottom() + parent.getPaddingBottom();
        int childCount = parent.getChildCount();
        int flexDirection = layoutManager.getFlexDirection();
        for (int i = 0; i < childCount; i++) {
            View child = parent.getChildAt(i);
            RecyclerView.LayoutParams lp = (RecyclerView.LayoutParams) child.getLayoutParams();
            if (layoutManager.isLayoutRtl()) {
                left = child.getRight() + lp.rightMargin;
                right = this.mDrawable.getIntrinsicWidth() + left;
            } else {
                int left2 = child.getLeft();
                right = left2 - lp.leftMargin;
                left = right - this.mDrawable.getIntrinsicWidth();
            }
            if (layoutManager.isMainAxisDirectionHorizontal()) {
                top = child.getTop() - lp.topMargin;
                bottom = child.getBottom() + lp.bottomMargin;
            } else if (flexDirection == 3) {
                int bottom2 = child.getBottom() + lp.bottomMargin + this.mDrawable.getIntrinsicHeight();
                bottom = Math.min(bottom2, parentBottom);
                int bottom3 = child.getTop();
                top = bottom3 - lp.topMargin;
            } else {
                int top2 = child.getTop();
                top = Math.max((top2 - lp.topMargin) - this.mDrawable.getIntrinsicHeight(), parentTop);
                bottom = child.getBottom() + lp.bottomMargin;
            }
            this.mDrawable.setBounds(left, top, right, bottom);
            this.mDrawable.draw(canvas);
        }
    }

    private void drawHorizontalDecorations(Canvas canvas, RecyclerView parent) {
        int bottom;
        int top;
        int left;
        int right;
        if (!needsHorizontalDecoration()) {
            return;
        }
        FlexboxLayoutManager layoutManager = (FlexboxLayoutManager) parent.getLayoutManager();
        int flexDirection = layoutManager.getFlexDirection();
        int parentLeft = parent.getLeft() - parent.getPaddingLeft();
        int parentRight = parent.getRight() + parent.getPaddingRight();
        int childCount = parent.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = parent.getChildAt(i);
            RecyclerView.LayoutParams lp = (RecyclerView.LayoutParams) child.getLayoutParams();
            if (flexDirection == 3) {
                top = child.getBottom() + lp.bottomMargin;
                bottom = this.mDrawable.getIntrinsicHeight() + top;
            } else {
                int top2 = child.getTop();
                bottom = top2 - lp.topMargin;
                top = bottom - this.mDrawable.getIntrinsicHeight();
            }
            if (layoutManager.isMainAxisDirectionHorizontal()) {
                if (layoutManager.isLayoutRtl()) {
                    int right2 = child.getRight() + lp.rightMargin + this.mDrawable.getIntrinsicWidth();
                    right = Math.min(right2, parentRight);
                    left = child.getLeft() - lp.leftMargin;
                } else {
                    int right3 = child.getLeft();
                    int left2 = (right3 - lp.leftMargin) - this.mDrawable.getIntrinsicWidth();
                    left = Math.max(left2, parentLeft);
                    int left3 = child.getRight();
                    right = left3 + lp.rightMargin;
                }
            } else {
                int right4 = child.getLeft();
                left = right4 - lp.leftMargin;
                right = child.getRight() + lp.rightMargin;
            }
            this.mDrawable.setBounds(left, top, right, bottom);
            this.mDrawable.draw(canvas);
        }
    }

    private boolean needsHorizontalDecoration() {
        return (this.mOrientation & 1) > 0;
    }

    private boolean needsVerticalDecoration() {
        return (this.mOrientation & 2) > 0;
    }

    private boolean isFirstItemInLine(int position, List<FlexLine> flexLines, FlexboxLayoutManager layoutManager) {
        int flexLineIndex = layoutManager.getPositionToFlexLineIndex(position);
        if ((flexLineIndex != -1 && flexLineIndex < layoutManager.getFlexLinesInternal().size() && layoutManager.getFlexLinesInternal().get(flexLineIndex).mFirstIndex == position) || position == 0) {
            return true;
        }
        if (flexLines.size() == 0) {
            return false;
        }
        FlexLine lastLine = flexLines.get(flexLines.size() - 1);
        return lastLine.mLastIndex == position + (-1);
    }
}
