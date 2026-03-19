package com.android.server.wm.utils;

import android.graphics.Rect;
import android.util.Size;
import android.view.DisplayCutout;
import java.util.List;
import java.util.Objects;

public class WmDisplayCutout {
    public static final WmDisplayCutout NO_CUTOUT = new WmDisplayCutout(DisplayCutout.NO_CUTOUT, null);
    private final Size mFrameSize;
    private final DisplayCutout mInner;

    public WmDisplayCutout(DisplayCutout displayCutout, Size size) {
        this.mInner = displayCutout;
        this.mFrameSize = size;
    }

    public static WmDisplayCutout computeSafeInsets(DisplayCutout displayCutout, int i, int i2) {
        if (displayCutout == DisplayCutout.NO_CUTOUT || displayCutout.isBoundsEmpty()) {
            return NO_CUTOUT;
        }
        Size size = new Size(i, i2);
        return new WmDisplayCutout(displayCutout.replaceSafeInsets(computeSafeInsets(size, displayCutout)), size);
    }

    public WmDisplayCutout inset(int i, int i2, int i3, int i4) {
        DisplayCutout displayCutoutInset = this.mInner.inset(i, i2, i3, i4);
        if (this.mInner == displayCutoutInset) {
            return this;
        }
        return new WmDisplayCutout(displayCutoutInset, this.mFrameSize == null ? null : new Size((this.mFrameSize.getWidth() - i) - i3, (this.mFrameSize.getHeight() - i2) - i4));
    }

    public WmDisplayCutout calculateRelativeTo(Rect rect) {
        if (this.mInner.isEmpty()) {
            return this;
        }
        return inset(rect.left, rect.top, this.mFrameSize.getWidth() - rect.right, this.mFrameSize.getHeight() - rect.bottom);
    }

    public WmDisplayCutout computeSafeInsets(int i, int i2) {
        return computeSafeInsets(this.mInner, i, i2);
    }

    private static Rect computeSafeInsets(Size size, DisplayCutout displayCutout) {
        if (size.getWidth() < size.getHeight()) {
            List<Rect> boundingRects = displayCutout.replaceSafeInsets(new Rect(0, size.getHeight() / 2, 0, size.getHeight() / 2)).getBoundingRects();
            return new Rect(0, findInsetForSide(size, boundingRects, 48), 0, findInsetForSide(size, boundingRects, 80));
        }
        if (size.getWidth() > size.getHeight()) {
            List<Rect> boundingRects2 = displayCutout.replaceSafeInsets(new Rect(size.getWidth() / 2, 0, size.getWidth() / 2, 0)).getBoundingRects();
            return new Rect(findInsetForSide(size, boundingRects2, 3), 0, findInsetForSide(size, boundingRects2, 5), 0);
        }
        throw new UnsupportedOperationException("not implemented: display=" + size + " cutout=" + displayCutout);
    }

    private static int findInsetForSide(Size size, List<Rect> list, int i) {
        int size2 = list.size();
        int iMax = 0;
        for (int i2 = 0; i2 < size2; i2++) {
            Rect rect = list.get(i2);
            if (i != 3) {
                if (i != 5) {
                    if (i != 48) {
                        if (i == 80) {
                            if (rect.bottom == size.getHeight()) {
                                iMax = Math.max(iMax, size.getHeight() - rect.top);
                            }
                        } else {
                            throw new IllegalArgumentException("unknown gravity: " + i);
                        }
                    } else if (rect.top == 0) {
                        iMax = Math.max(iMax, rect.bottom);
                    }
                } else if (rect.right == size.getWidth()) {
                    iMax = Math.max(iMax, size.getWidth() - rect.left);
                }
            } else if (rect.left == 0) {
                iMax = Math.max(iMax, rect.right);
            }
        }
        return iMax;
    }

    public DisplayCutout getDisplayCutout() {
        return this.mInner;
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof WmDisplayCutout)) {
            return false;
        }
        WmDisplayCutout wmDisplayCutout = (WmDisplayCutout) obj;
        return Objects.equals(this.mInner, wmDisplayCutout.mInner) && Objects.equals(this.mFrameSize, wmDisplayCutout.mFrameSize);
    }

    public int hashCode() {
        return Objects.hash(this.mInner, this.mFrameSize);
    }
}
