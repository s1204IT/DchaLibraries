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

    public CharSequence getGmtDisplayName(Context context, String id, long millis, boolean grayGmt) {
        TimeZone timezone = TimeZone.getTimeZone(id);
        if (timezone == null) {
            return null;
        }
        Locale defaultLocale = Locale.getDefault();
        if (!defaultLocale.equals(this.mDefaultLocale)) {
            this.mDefaultLocale = defaultLocale;
            cacheOverrides(context);
        }
        return buildGmtDisplayName(timezone, millis, grayGmt);
    }

    private CharSequence buildGmtDisplayName(TimeZone tz, long timeMillis, boolean grayGmt) {
        Time time = new Time(tz.getID());
        time.set(timeMillis);
        StringBuilder sb = new StringBuilder();
        String displayName = getDisplayName(tz, time.isDst != 0);
        sb.append(displayName);
        sb.append("  ");
        int gmtOffset = tz.getOffset(timeMillis);
        int gmtStart = sb.length();
        appendGmtOffset(sb, gmtOffset);
        int gmtEnd = sb.length();
        int symbolStart = 0;
        int symbolEnd = 0;
        if (tz.useDaylightTime()) {
            sb.append(" ");
            symbolStart = sb.length();
            sb.append(getDstSymbol());
            symbolEnd = sb.length();
        }
        Spannable spannableText = mSpannableFactory.newSpannable(sb);
        if (grayGmt) {
            spannableText.setSpan(new ForegroundColorSpan(-7829368), gmtStart, gmtEnd, 33);
        }
        if (tz.useDaylightTime()) {
            spannableText.setSpan(new ForegroundColorSpan(-4210753), symbolStart, symbolEnd, 33);
        }
        return spannableText;
    }

    public static void appendGmtOffset(StringBuilder sb, int gmtOffset) {
        sb.append("GMT");
        if (gmtOffset < 0) {
            sb.append('-');
        } else {
            sb.append('+');
        }
        int p = Math.abs(gmtOffset);
        sb.append(((long) p) / 3600000);
        int min = (p / 60000) % 60;
        if (min != 0) {
            sb.append(':');
            if (min < 10) {
                sb.append('0');
            }
            sb.append(min);
        }
    }

    public static char getDstSymbol() {
        return Build.VERSION.SDK_INT >= 16 ? (char) 9728 : '*';
    }

    private String getDisplayName(TimeZone tz, boolean daylightTime) {
        if (this.mOverrideIds == null || this.mOverrideLabels == null) {
            return tz.getDisplayName(daylightTime, 1, Locale.getDefault());
        }
        int i = 0;
        while (true) {
            if (i >= this.mOverrideIds.length) {
                break;
            }
            if (!tz.getID().equals(this.mOverrideIds[i])) {
                i++;
            } else {
                if (this.mOverrideLabels.length > i) {
                    return this.mOverrideLabels[i];
                }
                Log.e("TimeZonePickerUtils", "timezone_rename_ids len=" + this.mOverrideIds.length + " timezone_rename_labels len=" + this.mOverrideLabels.length);
            }
        }
        return tz.getDisplayName(daylightTime, 1, Locale.getDefault());
    }

    private void cacheOverrides(Context context) {
        Resources res = context.getResources();
        this.mOverrideIds = res.getStringArray(R.array.timezone_rename_ids);
        this.mOverrideLabels = res.getStringArray(R.array.timezone_rename_labels);
    }
}
