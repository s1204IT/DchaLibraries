package com.android.settings.datetime.timezone;

import android.content.Context;
import android.icu.impl.OlsonTimeZone;
import android.icu.text.DateFormat;
import android.icu.text.DisplayContext;
import android.icu.util.Calendar;
import android.icu.util.TimeZone;
import android.icu.util.TimeZoneTransition;
import android.support.v7.preference.Preference;
import com.android.settings.R;
import java.util.Date;

/* loaded from: classes.dex */
public class TimeZoneInfoPreferenceController extends BaseTimeZonePreferenceController {
    private static final String PREFERENCE_KEY = "footer_preference";
    Date mDate;
    private final DateFormat mDateFormat;
    private TimeZoneInfo mTimeZoneInfo;

    public TimeZoneInfoPreferenceController(Context context) {
        super(context, PREFERENCE_KEY);
        this.mDateFormat = DateFormat.getDateInstance(1);
        this.mDateFormat.setContext(DisplayContext.CAPITALIZATION_NONE);
        this.mDate = new Date();
    }

    @Override // com.android.settings.datetime.timezone.BaseTimeZonePreferenceController, com.android.settings.core.BasePreferenceController
    public int getAvailabilityStatus() {
        return 0;
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public void updateState(Preference preference) {
        preference.setTitle(this.mTimeZoneInfo == null ? "" : formatInfo(this.mTimeZoneInfo));
        preference.setVisible(this.mTimeZoneInfo != null);
    }

    public void setTimeZoneInfo(TimeZoneInfo timeZoneInfo) {
        this.mTimeZoneInfo = timeZoneInfo;
    }

    public TimeZoneInfo getTimeZoneInfo() {
        return this.mTimeZoneInfo;
    }

    private CharSequence formatOffsetAndName(TimeZoneInfo timeZoneInfo) {
        String genericName = timeZoneInfo.getGenericName();
        if (genericName == null) {
            if (timeZoneInfo.getTimeZone().inDaylightTime(this.mDate)) {
                genericName = timeZoneInfo.getDaylightName();
            } else {
                genericName = timeZoneInfo.getStandardName();
            }
        }
        if (genericName == null) {
            return timeZoneInfo.getGmtOffset().toString();
        }
        return SpannableUtil.getResourcesText(this.mContext.getResources(), R.string.zone_info_offset_and_name, timeZoneInfo.getGmtOffset(), genericName);
    }

    private CharSequence formatInfo(TimeZoneInfo timeZoneInfo) {
        CharSequence offsetAndName = formatOffsetAndName(timeZoneInfo);
        TimeZone timeZone = timeZoneInfo.getTimeZone();
        if (!timeZone.observesDaylightTime()) {
            return this.mContext.getString(R.string.zone_info_footer_no_dst, offsetAndName);
        }
        TimeZoneTransition timeZoneTransitionFindNextDstTransition = findNextDstTransition(timeZone);
        if (timeZoneTransitionFindNextDstTransition == null) {
            return null;
        }
        boolean z = timeZoneTransitionFindNextDstTransition.getTo().getDSTSavings() != 0;
        String daylightName = z ? timeZoneInfo.getDaylightName() : timeZoneInfo.getStandardName();
        if (daylightName == null) {
            if (z) {
                daylightName = this.mContext.getString(R.string.zone_time_type_dst);
            } else {
                daylightName = this.mContext.getString(R.string.zone_time_type_standard);
            }
        }
        Calendar calendar = Calendar.getInstance(timeZone);
        calendar.setTimeInMillis(timeZoneTransitionFindNextDstTransition.getTime());
        return SpannableUtil.getResourcesText(this.mContext.getResources(), R.string.zone_info_footer, offsetAndName, daylightName, this.mDateFormat.format(calendar));
    }

    private TimeZoneTransition findNextDstTransition(TimeZone timeZone) {
        if (!(timeZone instanceof OlsonTimeZone)) {
            return null;
        }
        OlsonTimeZone olsonTimeZone = (OlsonTimeZone) timeZone;
        TimeZoneTransition nextTransition = olsonTimeZone.getNextTransition(this.mDate.getTime(), false);
        while (nextTransition.getTo().getDSTSavings() == nextTransition.getFrom().getDSTSavings() && (nextTransition = olsonTimeZone.getNextTransition(nextTransition.getTime(), false)) != null) {
        }
        return nextTransition;
    }
}
