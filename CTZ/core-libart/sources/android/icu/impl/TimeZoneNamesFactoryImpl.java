package android.icu.impl;

import android.icu.text.TimeZoneNames;
import android.icu.util.ULocale;

public class TimeZoneNamesFactoryImpl extends TimeZoneNames.Factory {
    @Override
    public TimeZoneNames getTimeZoneNames(ULocale uLocale) {
        return new TimeZoneNamesImpl(uLocale);
    }
}
