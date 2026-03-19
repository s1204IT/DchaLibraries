package com.android.deskclock.data;

import android.text.TextUtils;

public final class TimeZones {
    private final CharSequence[] mTimeZoneIds;
    private final CharSequence[] mTimeZoneNames;

    TimeZones(CharSequence[] charSequenceArr, CharSequence[] charSequenceArr2) {
        this.mTimeZoneIds = charSequenceArr;
        this.mTimeZoneNames = charSequenceArr2;
    }

    public CharSequence[] getTimeZoneIds() {
        return this.mTimeZoneIds;
    }

    public CharSequence[] getTimeZoneNames() {
        return this.mTimeZoneNames;
    }

    CharSequence getTimeZoneName(CharSequence charSequence) {
        for (int i = 0; i < this.mTimeZoneIds.length; i++) {
            if (TextUtils.equals(charSequence, this.mTimeZoneIds[i])) {
                return this.mTimeZoneNames[i];
            }
        }
        return null;
    }

    boolean contains(String str) {
        return getTimeZoneName(str) != null;
    }
}
