package com.android.documentsui.inspector;

import android.content.Context;
import android.content.res.Resources;
import android.text.format.DateFormat;
import com.android.documentsui.R;
import java.util.Locale;

final class DateUtils {
    static String formatDate(Context context, long j) {
        int i;
        Resources resources = context.getResources();
        if (DateFormat.is24HourFormat(context)) {
            i = R.string.datetime_format_24;
        } else {
            i = R.string.datetime_format_12;
        }
        return DateFormat.format(DateFormat.getBestDateTimePattern(Locale.getDefault(), resources.getString(i)), j).toString();
    }
}
