package com.android.launcher3.util;

import android.R;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.drawable.Drawable;

/* loaded from: classes.dex */
public class Themes {
    public static int getColorAccent(Context context) {
        return getAttrColor(context, R.attr.colorAccent);
    }

    public static int getAttrColor(Context context, int i) {
        TypedArray typedArrayObtainStyledAttributes = context.obtainStyledAttributes(new int[]{i});
        int color = typedArrayObtainStyledAttributes.getColor(0, 0);
        typedArrayObtainStyledAttributes.recycle();
        return color;
    }

    public static boolean getAttrBoolean(Context context, int i) {
        TypedArray typedArrayObtainStyledAttributes = context.obtainStyledAttributes(new int[]{i});
        boolean z = typedArrayObtainStyledAttributes.getBoolean(0, false);
        typedArrayObtainStyledAttributes.recycle();
        return z;
    }

    public static Drawable getAttrDrawable(Context context, int i) {
        TypedArray typedArrayObtainStyledAttributes = context.obtainStyledAttributes(new int[]{i});
        Drawable drawable = typedArrayObtainStyledAttributes.getDrawable(0);
        typedArrayObtainStyledAttributes.recycle();
        return drawable;
    }

    public static int getAttrInteger(Context context, int i) {
        TypedArray typedArrayObtainStyledAttributes = context.obtainStyledAttributes(new int[]{i});
        int integer = typedArrayObtainStyledAttributes.getInteger(0, 0);
        typedArrayObtainStyledAttributes.recycle();
        return integer;
    }

    public static int getAlpha(Context context, int i) {
        TypedArray typedArrayObtainStyledAttributes = context.obtainStyledAttributes(new int[]{i});
        float f = typedArrayObtainStyledAttributes.getFloat(0, 0.0f);
        typedArrayObtainStyledAttributes.recycle();
        return (int) ((255.0f * f) + 0.5f);
    }

    public static void setColorScaleOnMatrix(int i, ColorMatrix colorMatrix) {
        colorMatrix.setScale(Color.red(i) / 255.0f, Color.green(i) / 255.0f, Color.blue(i) / 255.0f, Color.alpha(i) / 255.0f);
    }

    public static void setColorChangeOnMatrix(int i, int i2, ColorMatrix colorMatrix) {
        colorMatrix.reset();
        colorMatrix.getArray()[4] = Color.red(i2) - Color.red(i);
        colorMatrix.getArray()[9] = Color.green(i2) - Color.green(i);
        colorMatrix.getArray()[14] = Color.blue(i2) - Color.blue(i);
        colorMatrix.getArray()[19] = Color.alpha(i2) - Color.alpha(i);
    }
}
