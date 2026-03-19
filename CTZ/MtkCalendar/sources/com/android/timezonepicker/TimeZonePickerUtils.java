package com.android.timezonepicker;

import android.content.Context;
import android.content.res.Resources;
import android.os.Build;
import android.text.Spannable;
import android.text.format.Time;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import java.util.Locale;
import java.util.TimeZone;

public class TimeZonePickerUtils {
    private static final Spannable.Factory mSpannableFactory = Spannable.Factory.getInstance();
    private Locale mDefaultLocale;
    private String[] mOverrideIds;
    private String[] mOverrideLabels;

    public TimeZonePickerUtils(Context context) {
        cacheOverrides(context);
    }

    public CharSequence getGmtDisplayName(Context context, String str, long j, boolean z) {
        TimeZone timeZone = TimeZone.getTimeZone(str);
        if (timeZone == null) {
            return null;
        }
        Locale locale = Locale.getDefault();
        if (!locale.equals(this.mDefaultLocale)) {
            this.mDefaultLocale = locale;
            cacheOverrides(context);
        }
        return buildGmtDisplayName(timeZone, j, z);
    }

    private CharSequence buildGmtDisplayName(TimeZone timeZone, long j, boolean z) {
        int length;
        Time time = new Time(timeZone.getID());
        time.set(j);
        StringBuilder sb = new StringBuilder();
        int length2 = 0;
        sb.append(getDisplayName(timeZone, time.isDst != 0));
        sb.append("  ");
        int offset = timeZone.getOffset(j);
        int length3 = sb.length();
        appendGmtOffset(sb, offset);
        int length4 = sb.length();
        if (timeZone.useDaylightTime()) {
            sb.append(" ");
            length2 = sb.length();
            sb.append(getDstSymbol());
            length = sb.length();
        } else {
            length = 0;
        }
        Spannable spannableNewSpannable = mSpannableFactory.newSpannable(sb);
        if (z) {
            spannableNewSpannable.setSpan(new ForegroundColorSpan(-7829368), length3, length4, 33);
        }
        if (timeZone.useDaylightTime()) {
            spannableNewSpannable.setSpan(new ForegroundColorSpan(-4210753), length2, length, 33);
        }
        return spannableNewSpannable;
    }

    public static void appendGmtOffset(StringBuilder sb, int i) {
        sb.append("GMT");
        if (i < 0) {
            sb.append('-');
        } else {
            sb.append('+');
        }
        int iAbs = Math.abs(i);
        sb.append(((long) iAbs) / 3600000);
        int i2 = (iAbs / 60000) % 60;
        if (i2 != 0) {
            sb.append(':');
            if (i2 < 10) {
                sb.append('0');
            }
            sb.append(i2);
        }
    }

    public static char getDstSymbol() {
        if (Build.VERSION.SDK_INT >= 16) {
            return (char) 9728;
        }
        return '*';
    }

    private String getDisplayName(TimeZone timeZone, boolean z) {
        if (this.mOverrideIds == null || this.mOverrideLabels == null) {
            return timeZone.getDisplayName(z, 1, Locale.getDefault());
        }
        int i = 0;
        while (true) {
            if (i >= this.mOverrideIds.length) {
                break;
            }
            if (!timeZone.getID().equals(this.mOverrideIds[i])) {
                i++;
            } else {
                if (this.mOverrideLabels.length > i) {
                    return this.mOverrideLabels[i];
                }
                Log.e("TimeZonePickerUtils", "timezone_rename_ids len=" + this.mOverrideIds.length + " timezone_rename_labels len=" + this.mOverrideLabels.length);
            }
        }
        return timeZone.getDisplayName(z, 1, Locale.getDefault());
    }

    private void cacheOverrides(Context context) {
        Resources resources = context.getResources();
        this.mOverrideIds = resources.getStringArray(R.array.timezone_rename_ids);
        this.mOverrideLabels = resources.getStringArray(R.array.timezone_rename_labels);
    }
}
