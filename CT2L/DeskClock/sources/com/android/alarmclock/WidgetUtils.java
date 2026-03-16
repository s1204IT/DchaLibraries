package com.android.alarmclock;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.widget.RemoteViews;
import com.android.deskclock.R;
import com.android.deskclock.Utils;

public class WidgetUtils {
    public static void setClockSize(Context context, RemoteViews clock, float scale) {
        float fontSize = context.getResources().getDimension(R.dimen.widget_big_font_size);
        clock.setTextViewTextSize(R.id.the_clock, 0, fontSize * scale);
    }

    public static float getScaleRatio(Context context, Bundle options, int id) {
        int minWidth;
        if (options == null) {
            AppWidgetManager widgetManager = AppWidgetManager.getInstance(context);
            if (widgetManager == null) {
                return 1.0f;
            }
            options = widgetManager.getAppWidgetOptions(id);
        }
        if (options == null || (minWidth = options.getInt("appWidgetMinWidth")) == 0) {
            return 1.0f;
        }
        Resources res = context.getResources();
        float density = res.getDisplayMetrics().density;
        float ratio = (minWidth * density) / res.getDimension(R.dimen.min_digital_widget_width);
        int minHeight = options.getInt("appWidgetMinHeight");
        if (minHeight > 0 && minHeight * density < res.getDimension(R.dimen.min_digital_widget_height)) {
            ratio = Math.min(ratio, getHeightScaleRatio(context, options, id));
        }
        if (ratio > 1.0f) {
            ratio = 1.0f;
        }
        return ratio;
    }

    private static float getHeightScaleRatio(Context context, Bundle options, int id) {
        int minHeight;
        if (options == null) {
            AppWidgetManager widgetManager = AppWidgetManager.getInstance(context);
            if (widgetManager == null) {
                return 1.0f;
            }
            options = widgetManager.getAppWidgetOptions(id);
        }
        if (options == null || (minHeight = options.getInt("appWidgetMinHeight")) == 0) {
            return 1.0f;
        }
        Resources res = context.getResources();
        float density = res.getDisplayMetrics().density;
        float lblBox = 1.35f * res.getDimension(R.dimen.label_font_size);
        if (res.getDimension(R.dimen.min_digital_widget_height) - lblBox <= 0.0f) {
            return 1.0f;
        }
        float ratio = ((minHeight * density) - lblBox) / (res.getDimension(R.dimen.min_digital_widget_height) - lblBox);
        if (ratio > 1.0f) {
            ratio = 1.0f;
        }
        return ratio;
    }

    public static boolean showList(Context context, int id, float scale) {
        Bundle options;
        AppWidgetManager widgetManager = AppWidgetManager.getInstance(context);
        if (widgetManager == null || (options = widgetManager.getAppWidgetOptions(id)) == null) {
            return true;
        }
        Resources res = context.getResources();
        String whichHeight = res.getConfiguration().orientation == 1 ? "appWidgetMaxHeight" : "appWidgetMinHeight";
        int height = options.getInt(whichHeight);
        if (height == 0) {
            return true;
        }
        float density = res.getDisplayMetrics().density;
        float lblBox = 1.35f * res.getDimension(R.dimen.label_font_size);
        float neededSize = res.getDimension(R.dimen.digital_widget_list_min_fixed_height) + (2.0f * lblBox) + (res.getDimension(R.dimen.digital_widget_list_min_scaled_height) * scale);
        return ((float) height) * density > neededSize;
    }

    public static void setTimeFormat(RemoteViews clock, int amPmFontSize, int clockId) {
        if (clock != null) {
            clock.setCharSequence(clockId, "setFormat12Hour", Utils.get12ModeFormat(amPmFontSize));
            clock.setCharSequence(clockId, "setFormat24Hour", Utils.get24ModeFormat());
        }
    }
}
