package com.mediatek.calendar.extension;

import android.content.Context;
import com.mediatek.calendar.features.Features;

public class CalendarThemeExt implements ICalendarThemeExt {
    private Context mContext;

    public CalendarThemeExt(Context context) {
        this.mContext = context;
    }

    @Override
    public boolean isThemeManagerEnable() {
        return Features.isThemeManagerEnabled();
    }

    @Override
    public int getThemeColor() {
        this.mContext.getResources();
        return -432818715;
    }
}
