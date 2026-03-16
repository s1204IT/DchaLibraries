package com.android.contacts.common.util;

import android.content.res.Resources;
import android.graphics.Outline;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import com.android.contacts.R;

public class ViewUtil {
    private static final ViewOutlineProvider OVAL_OUTLINE_PROVIDER = new ViewOutlineProvider() {
        @Override
        public void getOutline(View view, Outline outline) {
            outline.setOval(0, 0, view.getWidth(), view.getHeight());
        }
    };
    private static final ViewOutlineProvider RECT_OUTLINE_PROVIDER = new ViewOutlineProvider() {
        @Override
        public void getOutline(View view, Outline outline) {
            outline.setRect(0, 0, view.getWidth(), view.getHeight());
        }
    };

    public static int getConstantPreLayoutWidth(View view) {
        ViewGroup.LayoutParams p = view.getLayoutParams();
        if (p.width < 0) {
            throw new IllegalStateException("Expecting view's width to be a constant rather than a result of the layout pass");
        }
        return p.width;
    }

    public static boolean isViewLayoutRtl(View view) {
        return view.getLayoutDirection() == 1;
    }

    public static void addRectangularOutlineProvider(View view, Resources res) {
        view.setOutlineProvider(RECT_OUTLINE_PROVIDER);
    }

    public static void setupFloatingActionButton(View view, Resources res) {
        view.setOutlineProvider(OVAL_OUTLINE_PROVIDER);
        view.setTranslationZ(res.getDimensionPixelSize(R.dimen.floating_action_button_translation_z));
    }
}
