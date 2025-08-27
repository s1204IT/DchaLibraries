package com.android.quickstep.util;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Rect;
import android.support.annotation.AnyThread;
import com.android.launcher3.DeviceProfile;
import com.android.launcher3.R;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/* loaded from: classes.dex */
public class LayoutUtils {
    private static final int MULTI_WINDOW_STRATEGY_DEVICE_PROFILE = 2;
    private static final int MULTI_WINDOW_STRATEGY_HALF_SCREEN = 1;

    @Retention(RetentionPolicy.SOURCE)
    private @interface MultiWindowStrategy {
    }

    public static void calculateLauncherTaskSize(Context context, DeviceProfile deviceProfile, Rect rect) throws Resources.NotFoundException {
        float f;
        if (deviceProfile.isVerticalBarLayout()) {
            f = 0.0f;
        } else {
            f = deviceProfile.hotseatBarSizePx + deviceProfile.verticalDragHandleSizePx;
        }
        calculateTaskSize(context, deviceProfile, f, 1, rect);
    }

    public static void calculateFallbackTaskSize(Context context, DeviceProfile deviceProfile, Rect rect) throws Resources.NotFoundException {
        calculateTaskSize(context, deviceProfile, 0.0f, 2, rect);
    }

    @AnyThread
    public static void calculateTaskSize(Context context, DeviceProfile deviceProfile, float f, int i, Rect rect) throws Resources.NotFoundException {
        float f2;
        float f3;
        int i2;
        float dimension;
        Resources resources = context.getResources();
        Rect insets = deviceProfile.getInsets();
        if (deviceProfile.isMultiWindowMode) {
            if (i == 1) {
                DeviceProfile fullScreenProfile = deviceProfile.getFullScreenProfile();
                f2 = fullScreenProfile.availableWidthPx;
                f3 = fullScreenProfile.availableHeightPx;
                float dimension2 = resources.getDimension(R.dimen.multi_window_task_divider_size) / 2.0f;
                if (fullScreenProfile.isLandscape) {
                    f2 = (f2 / 2.0f) - dimension2;
                } else {
                    f3 = (f3 / 2.0f) - dimension2;
                }
            } else {
                f2 = deviceProfile.widthPx;
                f3 = deviceProfile.heightPx;
            }
            dimension = resources.getDimension(R.dimen.multi_window_task_card_horz_space);
        } else {
            f2 = deviceProfile.availableWidthPx;
            f3 = deviceProfile.availableHeightPx;
            if (deviceProfile.isVerticalBarLayout()) {
                i2 = R.dimen.landscape_task_card_horz_space;
            } else {
                i2 = R.dimen.portrait_task_card_horz_space;
            }
            dimension = resources.getDimension(i2);
        }
        float dimension3 = resources.getDimension(R.dimen.task_thumbnail_top_margin);
        float dimension4 = resources.getDimension(R.dimen.task_card_vert_space);
        int i3 = (deviceProfile.widthPx - insets.left) - insets.right;
        float f4 = (deviceProfile.heightPx - insets.top) - insets.bottom;
        float f5 = ((f4 - dimension3) - f) - dimension4;
        float f6 = i3;
        float fMin = Math.min((f6 - dimension) / f2, f5 / f3);
        float f7 = f2 * fMin;
        float f8 = fMin * f3;
        float f9 = insets.left + ((f6 - f7) / 2.0f);
        float fMax = insets.top + Math.max(dimension3, ((f4 - f) - f8) / 2.0f);
        rect.set(Math.round(f9), Math.round(fMax), Math.round(f9 + f7), Math.round(fMax + f8));
    }
}
