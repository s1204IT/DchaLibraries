package com.android.internal.widget;

import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;

public class BackgroundFallback {
    private Drawable mBackgroundFallback;

    public void setDrawable(Drawable drawable) {
        this.mBackgroundFallback = drawable;
    }

    public boolean hasFallback() {
        return this.mBackgroundFallback != null;
    }

    public void draw(ViewGroup viewGroup, ViewGroup viewGroup2, Canvas canvas, View view, View view2, View view3) {
        int i;
        View view4;
        if (!hasFallback()) {
            return;
        }
        int width = viewGroup.getWidth();
        int height = viewGroup.getHeight();
        int left = viewGroup2.getLeft();
        int top = viewGroup2.getTop();
        int childCount = viewGroup2.getChildCount();
        int i2 = width;
        int i3 = height;
        int i4 = 0;
        int iMax = 0;
        int iMax2 = 0;
        while (i4 < childCount) {
            View childAt = viewGroup2.getChildAt(i4);
            int i5 = childCount;
            Drawable background = childAt.getBackground();
            if (childAt == view) {
                if (background != null || !(childAt instanceof ViewGroup) || ((ViewGroup) childAt).getChildCount() != 0) {
                    int iMin = Math.min(i2, childAt.getLeft() + left);
                    int iMin2 = Math.min(i3, childAt.getTop() + top);
                    iMax = Math.max(iMax, childAt.getRight() + left);
                    iMax2 = Math.max(iMax2, childAt.getBottom() + top);
                    i3 = iMin2;
                    i2 = iMin;
                }
            } else if (childAt.getVisibility() != 0 || !isOpaque(background)) {
            }
            i4++;
            childCount = i5;
        }
        boolean z = true;
        for (int i6 = 0; i6 < 2; i6++) {
            if (i6 != 0) {
                view4 = view3;
            } else {
                view4 = view2;
            }
            if (view4 == null || view4.getVisibility() != 0 || view4.getAlpha() != 1.0f || !isOpaque(view4.getBackground())) {
                z = false;
            } else {
                if (view4.getTop() <= 0 && view4.getBottom() >= height && view4.getLeft() <= 0 && view4.getRight() >= i2) {
                    i2 = 0;
                }
                if (view4.getTop() <= 0 && view4.getBottom() >= height && view4.getLeft() <= iMax && view4.getRight() >= width) {
                    iMax = width;
                }
                if (view4.getTop() <= 0 && view4.getBottom() >= i3 && view4.getLeft() <= 0 && view4.getRight() >= width) {
                    i3 = 0;
                }
                if (view4.getTop() <= iMax2 && view4.getBottom() >= height && view4.getLeft() <= 0 && view4.getRight() >= width) {
                    iMax2 = height;
                }
                z &= view4.getTop() <= 0 && view4.getBottom() >= i3;
            }
        }
        if (z && (viewsCoverEntireWidth(view2, view3, width) || viewsCoverEntireWidth(view3, view2, width))) {
            i3 = 0;
        }
        if (i2 >= iMax || i3 >= iMax2) {
            return;
        }
        if (i3 > 0) {
            i = 0;
            this.mBackgroundFallback.setBounds(0, 0, width, i3);
            this.mBackgroundFallback.draw(canvas);
        } else {
            i = 0;
        }
        if (i2 > 0) {
            this.mBackgroundFallback.setBounds(i, i3, i2, height);
            this.mBackgroundFallback.draw(canvas);
        }
        if (iMax < width) {
            this.mBackgroundFallback.setBounds(iMax, i3, width, height);
            this.mBackgroundFallback.draw(canvas);
        }
        if (iMax2 < height) {
            this.mBackgroundFallback.setBounds(i2, iMax2, iMax, height);
            this.mBackgroundFallback.draw(canvas);
        }
    }

    private boolean isOpaque(Drawable drawable) {
        return drawable != null && drawable.getOpacity() == -1;
    }

    private boolean viewsCoverEntireWidth(View view, View view2, int i) {
        return view.getLeft() <= 0 && view.getRight() >= view2.getLeft() && view2.getRight() >= i;
    }
}
