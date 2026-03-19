package com.android.phone.common.util;

import android.graphics.Outline;
import android.text.TextPaint;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.TextView;

public class ViewUtil {
    private static final ViewOutlineProvider OVAL_OUTLINE_PROVIDER = new ViewOutlineProvider() {
        @Override
        public void getOutline(View view, Outline outline) {
            outline.setOval(0, 0, view.getWidth(), view.getHeight());
        }
    };

    public static void resizeText(TextView textView, int i, int i2) {
        TextPaint paint = textView.getPaint();
        int width = textView.getWidth();
        if (width == 0) {
            return;
        }
        float f = i;
        textView.setTextSize(0, f);
        float fMeasureText = width / paint.measureText(textView.getText().toString());
        if (fMeasureText <= 1.0f) {
            textView.setTextSize(0, Math.max(i2, f * fMeasureText));
        }
    }
}
