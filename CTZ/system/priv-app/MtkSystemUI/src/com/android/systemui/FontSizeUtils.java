package com.android.systemui;

import android.view.View;
import android.widget.TextView;
/* loaded from: classes.dex */
public class FontSizeUtils {
    public static void updateFontSize(View view, int i, int i2) {
        updateFontSize((TextView) view.findViewById(i), i2);
    }

    public static void updateFontSize(TextView textView, int i) {
        if (textView != null) {
            textView.setTextSize(0, textView.getResources().getDimensionPixelSize(i));
        }
    }
}
