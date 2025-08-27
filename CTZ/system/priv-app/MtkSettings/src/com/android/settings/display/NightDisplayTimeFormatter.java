package com.android.settings.display;

import android.content.Context;
import com.android.internal.app.ColorDisplayController;
import com.android.settings.R;
import java.text.DateFormat;
import java.time.LocalTime;
import java.util.Calendar;
import java.util.TimeZone;

/* loaded from: classes.dex */
public class NightDisplayTimeFormatter {
    private DateFormat mTimeFormatter;

    NightDisplayTimeFormatter(Context context) {
        this.mTimeFormatter = android.text.format.DateFormat.getTimeFormat(context);
        this.mTimeFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    public String getFormattedTimeString(LocalTime localTime) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeZone(this.mTimeFormatter.getTimeZone());
        calendar.set(11, localTime.getHour());
        calendar.set(12, localTime.getMinute());
        calendar.set(13, 0);
        calendar.set(14, 0);
        return this.mTimeFormatter.format(calendar.getTime());
    }

    public String getAutoModeTimeSummary(Context context, ColorDisplayController colorDisplayController) {
        return context.getString(colorDisplayController.isActivated() ? R.string.night_display_summary_on : R.string.night_display_summary_off, getAutoModeSummary(context, colorDisplayController));
    }

    private String getAutoModeSummary(Context context, ColorDisplayController colorDisplayController) {
        int i;
        int i2;
        boolean zIsActivated = colorDisplayController.isActivated();
        int autoMode = colorDisplayController.getAutoMode();
        if (autoMode == 1) {
            if (zIsActivated) {
                return context.getString(R.string.night_display_summary_on_auto_mode_custom, getFormattedTimeString(colorDisplayController.getCustomEndTime()));
            }
            return context.getString(R.string.night_display_summary_off_auto_mode_custom, getFormattedTimeString(colorDisplayController.getCustomStartTime()));
        }
        if (autoMode == 2) {
            if (zIsActivated) {
                i2 = R.string.night_display_summary_on_auto_mode_twilight;
            } else {
                i2 = R.string.night_display_summary_off_auto_mode_twilight;
            }
            return context.getString(i2);
        }
        if (zIsActivated) {
            i = R.string.night_display_summary_on_auto_mode_never;
        } else {
            i = R.string.night_display_summary_off_auto_mode_never;
        }
        return context.getString(i);
    }
}
