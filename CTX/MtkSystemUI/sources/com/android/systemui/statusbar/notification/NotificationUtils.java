package com.android.systemui.statusbar.notification;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.widget.ImageView;
import com.android.internal.util.NotificationColorUtil;
import com.android.systemui.R;

public class NotificationUtils {
    private static final int[] sLocationBase = new int[2];
    private static final int[] sLocationOffset = new int[2];

    public static boolean isGrayscale(ImageView imageView, NotificationColorUtil notificationColorUtil) {
        Object tag = imageView.getTag(R.id.icon_is_grayscale);
        if (tag != null) {
            return Boolean.TRUE.equals(tag);
        }
        boolean zIsGrayscaleIcon = notificationColorUtil.isGrayscaleIcon(imageView.getDrawable());
        imageView.setTag(R.id.icon_is_grayscale, Boolean.valueOf(zIsGrayscaleIcon));
        return zIsGrayscaleIcon;
    }

    public static float interpolate(float f, float f2, float f3) {
        return (f * (1.0f - f3)) + (f2 * f3);
    }

    public static int interpolateColors(int i, int i2, float f) {
        return Color.argb((int) interpolate(Color.alpha(i), Color.alpha(i2), f), (int) interpolate(Color.red(i), Color.red(i2), f), (int) interpolate(Color.green(i), Color.green(i2), f), (int) interpolate(Color.blue(i), Color.blue(i2), f));
    }

    public static float getRelativeYOffset(View view, View view2) {
        view2.getLocationOnScreen(sLocationBase);
        view.getLocationOnScreen(sLocationOffset);
        return sLocationOffset[1] - sLocationBase[1];
    }

    public static int getFontScaledHeight(Context context, int i) {
        return (int) (context.getResources().getDimensionPixelSize(i) * Math.max(1.0f, context.getResources().getDisplayMetrics().scaledDensity / context.getResources().getDisplayMetrics().density));
    }
}
