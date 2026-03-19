package com.android.internal.policy;

import android.graphics.Rect;

public class DockedDividerUtils {
    public static void calculateBoundsForPosition(int i, int i2, Rect rect, int i3, int i4, int i5) {
        rect.set(0, 0, i3, i4);
        switch (i2) {
            case 1:
                rect.right = i;
                break;
            case 2:
                rect.bottom = i;
                break;
            case 3:
                rect.left = i + i5;
                break;
            case 4:
                rect.top = i + i5;
                break;
        }
        boolean z = true;
        if (i2 != 1 && i2 != 2) {
            z = false;
        }
        sanitizeStackBounds(rect, z);
    }

    public static void sanitizeStackBounds(Rect rect, boolean z) {
        if (z) {
            if (rect.left >= rect.right) {
                rect.left = rect.right - 1;
            }
            if (rect.top >= rect.bottom) {
                rect.top = rect.bottom - 1;
                return;
            }
            return;
        }
        if (rect.right <= rect.left) {
            rect.right = rect.left + 1;
        }
        if (rect.bottom <= rect.top) {
            rect.bottom = rect.top + 1;
        }
    }

    public static int calculatePositionForBounds(Rect rect, int i, int i2) {
        switch (i) {
            case 1:
                return rect.right;
            case 2:
                return rect.bottom;
            case 3:
                return rect.left - i2;
            case 4:
                return rect.top - i2;
            default:
                return 0;
        }
    }

    public static int calculateMiddlePosition(boolean z, Rect rect, int i, int i2, int i3) {
        int i4;
        int i5 = z ? rect.top : rect.left;
        if (z) {
            i4 = i2 - rect.bottom;
        } else {
            i4 = i - rect.right;
        }
        return (i5 + ((i4 - i5) / 2)) - (i3 / 2);
    }

    public static int getDockSideFromCreatedMode(boolean z, boolean z2) {
        if (z) {
            if (z2) {
                return 2;
            }
            return 1;
        }
        if (z2) {
            return 4;
        }
        return 3;
    }

    public static int invertDockSide(int i) {
        switch (i) {
            case 1:
                return 3;
            case 2:
                return 4;
            case 3:
                return 1;
            case 4:
                return 2;
            default:
                return -1;
        }
    }
}
