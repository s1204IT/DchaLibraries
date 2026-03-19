package com.android.deskclock;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.support.annotation.AttrRes;
import android.support.annotation.ColorInt;
import android.support.v4.internal.view.SupportMenu;

public final class ThemeUtils {
    private static final int[] TEMP_ATTR = new int[1];

    private ThemeUtils() {
    }

    @ColorInt
    public static int resolveColor(Context context, @AttrRes int i) {
        return resolveColor(context, i, null);
    }

    @ColorInt
    public static int resolveColor(Context context, @AttrRes int i, @AttrRes int[] iArr) {
        TypedArray typedArrayObtainStyledAttributes;
        synchronized (TEMP_ATTR) {
            TEMP_ATTR[0] = i;
            typedArrayObtainStyledAttributes = context.obtainStyledAttributes(TEMP_ATTR);
        }
        try {
            if (iArr == null) {
                return typedArrayObtainStyledAttributes.getColor(0, SupportMenu.CATEGORY_MASK);
            }
            ColorStateList colorStateList = typedArrayObtainStyledAttributes.getColorStateList(0);
            return colorStateList != null ? colorStateList.getColorForState(iArr, SupportMenu.CATEGORY_MASK) : SupportMenu.CATEGORY_MASK;
        } finally {
            typedArrayObtainStyledAttributes.recycle();
        }
    }

    public static Drawable resolveDrawable(Context context, @AttrRes int i) {
        TypedArray typedArrayObtainStyledAttributes;
        synchronized (TEMP_ATTR) {
            TEMP_ATTR[0] = i;
            typedArrayObtainStyledAttributes = context.obtainStyledAttributes(TEMP_ATTR);
        }
        try {
            return typedArrayObtainStyledAttributes.getDrawable(0);
        } finally {
            typedArrayObtainStyledAttributes.recycle();
        }
    }
}
