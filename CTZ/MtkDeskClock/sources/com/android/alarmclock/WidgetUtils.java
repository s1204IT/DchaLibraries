package com.android.alarmclock;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import com.android.deskclock.R;
import com.android.deskclock.Utils;

public final class WidgetUtils {
    private WidgetUtils() {
    }

    public static float getScaleRatio(Context context, Bundle bundle, int i, int i2) {
        int i3;
        if (bundle == null) {
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            if (appWidgetManager == null) {
                return 1.0f;
            }
            bundle = appWidgetManager.getAppWidgetOptions(i);
        }
        if (bundle == null || (i3 = bundle.getInt("appWidgetMinWidth")) == 0) {
            return 1.0f;
        }
        Resources resources = context.getResources();
        float fMin = Math.min((resources.getDisplayMetrics().density * i3) / resources.getDimension(R.dimen.min_digital_widget_width), getHeightScaleRatio(context, bundle, i)) * 0.83f;
        if (i2 > 0) {
            if (fMin > 1.0f) {
                return 1.0f;
            }
            return fMin;
        }
        float fMin2 = Math.min(fMin, 1.6f);
        if (Utils.isPortrait(context)) {
            return Math.max(fMin2, 0.71f);
        }
        return Math.max(fMin2, 0.45f);
    }

    private static float getHeightScaleRatio(Context context, Bundle bundle, int i) {
        int i2;
        if (bundle == null) {
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            if (appWidgetManager == null) {
                return 1.0f;
            }
            bundle = appWidgetManager.getAppWidgetOptions(i);
        }
        if (bundle == null || (i2 = bundle.getInt("appWidgetMinHeight")) == 0) {
            return 1.0f;
        }
        Resources resources = context.getResources();
        float dimension = (resources.getDisplayMetrics().density * i2) / resources.getDimension(R.dimen.min_digital_widget_height);
        if (Utils.isPortrait(context)) {
            return dimension * 1.75f;
        }
        return dimension;
    }
}
