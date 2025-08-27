package com.android.settings.datetime.timezone;

import android.content.Context;
import android.support.v7.preference.Preference;
import com.android.settings.R;

/* loaded from: classes.dex */
public class RegionZonePreferenceController extends BaseTimeZonePreferenceController {
    private static final String PREFERENCE_KEY = "region_zone";
    private boolean mIsClickable;
    private TimeZoneInfo mTimeZoneInfo;

    public RegionZonePreferenceController(Context context) {
        super(context, PREFERENCE_KEY);
    }

    @Override // com.android.settings.datetime.timezone.BaseTimeZonePreferenceController, com.android.settings.core.BasePreferenceController
    public int getAvailabilityStatus() {
        return 0;
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public void updateState(Preference preference) {
        super.updateState(preference);
        preference.setEnabled(isClickable());
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public CharSequence getSummary() {
        return this.mTimeZoneInfo == null ? "" : SpannableUtil.getResourcesText(this.mContext.getResources(), R.string.zone_info_exemplar_location_and_offset, this.mTimeZoneInfo.getExemplarLocation(), this.mTimeZoneInfo.getGmtOffset());
    }

    public void setTimeZoneInfo(TimeZoneInfo timeZoneInfo) {
        this.mTimeZoneInfo = timeZoneInfo;
    }

    public TimeZoneInfo getTimeZoneInfo() {
        return this.mTimeZoneInfo;
    }

    public void setClickable(boolean z) {
        this.mIsClickable = z;
    }

    public boolean isClickable() {
        return this.mIsClickable;
    }
}
