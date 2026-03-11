package com.android.settings.datetime.timezone.model;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import libcore.util.CountryTimeZones;

public class FilteredCountryTimeZones {
    private final CountryTimeZones mCountryTimeZones;
    private final List<String> mTimeZoneIds;

    public FilteredCountryTimeZones(CountryTimeZones countryTimeZones) {
        this.mCountryTimeZones = countryTimeZones;
        this.mTimeZoneIds = Collections.unmodifiableList((List) countryTimeZones.getTimeZoneMappings().stream().filter(new Predicate() {
            @Override
            public final boolean test(Object obj) {
                return FilteredCountryTimeZones.lambda$new$0((CountryTimeZones.TimeZoneMapping) obj);
            }
        }).map(new Function() {
            @Override
            public final Object apply(Object obj) {
                return ((CountryTimeZones.TimeZoneMapping) obj).timeZoneId;
            }
        }).collect(Collectors.toList()));
    }

    static boolean lambda$new$0(CountryTimeZones.TimeZoneMapping timeZoneMapping) {
        return timeZoneMapping.showInPicker && (timeZoneMapping.notUsedAfter == null || timeZoneMapping.notUsedAfter.longValue() >= 1514764800000L);
    }

    public List<String> getTimeZoneIds() {
        return this.mTimeZoneIds;
    }

    public String getRegionId() {
        return TimeZoneData.normalizeRegionId(this.mCountryTimeZones.getCountryIso());
    }
}
