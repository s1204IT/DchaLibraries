package com.mediatek.calendar.extension;

import android.content.Context;
import com.mediatek.calendar.MTKUtils;
import com.mediatek.calendar.edittext.EditTextExtImpl;
import com.mediatek.calendar.edittext.IEditTextExt;

public final class ExtensionFactory {
    private static CalendarThemeExt sCalendarThemeExt;

    public static IOptionsMenuExt getAllInOneOptionMenuExt(Context context) {
        return new ClearAllEventsExt(context);
    }

    public static IOptionsMenuExt getEventInfoOptionsMenuExt(Context context, long j) {
        if (MTKUtils.isEventShareAvailable(context)) {
            return new EventInfoOptionsMenuExt(context, j);
        }
        return new DefaultOptionsMenuExt();
    }

    public static synchronized ICalendarThemeExt getCalendarTheme(Context context) {
        if (sCalendarThemeExt == null) {
            sCalendarThemeExt = new CalendarThemeExt(context.getApplicationContext());
        }
        return sCalendarThemeExt;
    }

    public static IEditTextExt getEditTextExt() {
        return new EditTextExtImpl();
    }
}
