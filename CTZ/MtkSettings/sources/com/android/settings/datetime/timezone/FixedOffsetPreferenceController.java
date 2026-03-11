package com.android.settings.datetime.timezone;

import android.content.Context;
import com.android.settings.R;

public class FixedOffsetPreferenceController extends BaseTimeZonePreferenceController {
    private static final String PREFERENCE_KEY = "fixed_offset";
    private TimeZoneInfo mTimeZoneInfo;

    public FixedOffsetPreferenceController(Context context) {
        super(context, PREFERENCE_KEY);
    }

    @Override
    public CharSequence getSummary() {
        if (this.mTimeZoneInfo == null) {
            return "";
        }
        String standardName = this.mTimeZoneInfo.getStandardName();
        if (standardName == null) {
            return this.mTimeZoneInfo.getGmtOffset();
        }
        return SpannableUtil.getResourcesText(this.mContext.getResources(), R.string.zone_info_offset_and_name, this.mTimeZoneInfo.getGmtOffset(), standardName);
    }

    public void setTimeZoneInfo(TimeZoneInfo timeZoneInfo) {
        this.mTimeZoneInfo = timeZoneInfo;
    }

    public TimeZoneInfo getTimeZoneInfo() {
        return this.mTimeZoneInfo;
    }
}
