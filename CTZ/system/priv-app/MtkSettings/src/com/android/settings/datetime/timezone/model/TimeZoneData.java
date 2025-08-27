package com.android.settings.datetime.timezone.model;

import android.support.v4.util.ArraySet;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import libcore.util.CountryTimeZones;
import libcore.util.CountryZonesFinder;
import libcore.util.TimeZoneFinder;

/* loaded from: classes.dex */
public class TimeZoneData {
    private static WeakReference<TimeZoneData> sCache = null;
    private final CountryZonesFinder mCountryZonesFinder;
    private final Set<String> mRegionIds;

    public static synchronized TimeZoneData getInstance() {
        TimeZoneData timeZoneData = sCache == null ? null : sCache.get();
        if (timeZoneData != null) {
            return timeZoneData;
        }
        TimeZoneData timeZoneData2 = new TimeZoneData(TimeZoneFinder.getInstance().getCountryZonesFinder());
        sCache = new WeakReference<>(timeZoneData2);
        return timeZoneData2;
    }

    public TimeZoneData(CountryZonesFinder countryZonesFinder) {
        this.mCountryZonesFinder = countryZonesFinder;
        this.mRegionIds = getNormalizedRegionIds(this.mCountryZonesFinder.lookupAllCountryIsoCodes());
    }

    public Set<String> getRegionIds() {
        return this.mRegionIds;
    }

    public Set<String> lookupCountryCodesForZoneId(String str) {
        if (str == null) {
            return Collections.emptySet();
        }
        List listLookupCountryTimeZonesForZoneId = this.mCountryZonesFinder.lookupCountryTimeZonesForZoneId(str);
        ArraySet arraySet = new ArraySet();
        Iterator it = listLookupCountryTimeZonesForZoneId.iterator();
        while (it.hasNext()) {
            FilteredCountryTimeZones filteredCountryTimeZones = new FilteredCountryTimeZones((CountryTimeZones) it.next());
            if (filteredCountryTimeZones.getTimeZoneIds().contains(str)) {
                arraySet.add(filteredCountryTimeZones.getRegionId());
            }
        }
        return arraySet;
    }

    public FilteredCountryTimeZones lookupCountryTimeZones(String str) {
        CountryTimeZones countryTimeZonesLookupCountryTimeZones;
        if (str != null) {
            countryTimeZonesLookupCountryTimeZones = this.mCountryZonesFinder.lookupCountryTimeZones(str);
        } else {
            countryTimeZonesLookupCountryTimeZones = null;
        }
        if (countryTimeZonesLookupCountryTimeZones == null) {
            return null;
        }
        return new FilteredCountryTimeZones(countryTimeZonesLookupCountryTimeZones);
    }

    private static Set<String> getNormalizedRegionIds(List<String> list) {
        HashSet hashSet = new HashSet(list.size());
        Iterator<String> it = list.iterator();
        while (it.hasNext()) {
            hashSet.add(normalizeRegionId(it.next()));
        }
        return Collections.unmodifiableSet(hashSet);
    }

    public static String normalizeRegionId(String str) {
        if (str == null) {
            return null;
        }
        return str.toUpperCase(Locale.US);
    }
}
