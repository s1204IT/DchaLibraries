package com.android.phone.common.util;

import android.content.res.Resources;
import android.graphics.Outline;
import android.graphics.Paint;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.TextView;
import com.android.phone.R;

public class ViewUtil {
    private static final ViewOutlineProvider OVAL_OUTLINE_PROVIDER = new ViewOutlineProvider() {
        @Override
        public void getOutline(View view, Outline outline) {
            outline.setOval(0, 0, view.getWidth(), view.getHeight());
        }
    };

    public static void setupFloatingActionButton(View view, Resources res) {
        view.setOutlineProvider(OVAL_OUTLINE_PROVIDER);
        view.setTranslationZ(res.getDimensionPixelSize(R.dimen.floating_action_button_translation_z));
    }

    public static void resizeText(TextView textView, int originalTextSize, int minTextSize) {
        Paint paint = textView.getPaint();
        int width = textView.getWidth();
        if (width != 0) {
            textView.setTextSize(0, originalTextSize);
            float ratio = width / paint.measureText(textView.getText().toString());
            if (ratio <= 1.0f) {
                textView.setTextSize(0, Math.max(minTextSize, originalTextSize * ratio));
            }
        }
    }
}
