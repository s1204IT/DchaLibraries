package com.android.documentsui.base;

import android.R;
import android.app.Activity;
import android.content.Context;
import android.graphics.Point;
import android.util.TypedValue;

public final class Display {
    public static float screenWidth(Activity activity) {
        activity.getWindowManager().getDefaultDisplay().getSize(new Point());
        return r0.x;
    }

    public static float density(Context context) {
        return context.getResources().getDisplayMetrics().density;
    }

    public static float actionBarHeight(Context context) {
        int iComplexToDimensionPixelSize;
        TypedValue typedValue = new TypedValue();
        if (context.getTheme().resolveAttribute(R.attr.actionBarSize, typedValue, true)) {
            iComplexToDimensionPixelSize = TypedValue.complexToDimensionPixelSize(typedValue.data, context.getResources().getDisplayMetrics());
        } else {
            iComplexToDimensionPixelSize = 0;
        }
        return iComplexToDimensionPixelSize;
    }
}
