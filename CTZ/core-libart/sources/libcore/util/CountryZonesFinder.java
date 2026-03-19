package libcore.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import libcore.util.CountryTimeZones;

public final class CountryZonesFinder {
    private final List<CountryTimeZones> countryTimeZonesList;

    CountryZonesFinder(List<CountryTimeZones> list) {
        this.countryTimeZonesList = new ArrayList(list);
    }

    public static CountryZonesFinder createForTests(List<CountryTimeZones> list) {
        return new CountryZonesFinder(list);
    }

    public List<String> lookupAllCountryIsoCodes() {
        ArrayList arrayList = new ArrayList(this.countryTimeZonesList.size());
        Iterator<CountryTimeZones> it = this.countryTimeZonesList.iterator();
        while (it.hasNext()) {
            arrayList.add(it.next().getCountryIso());
        }
        return Collections.unmodifiableList(arrayList);
    }

    public List<CountryTimeZones> lookupCountryTimeZonesForZoneId(String str) {
        ArrayList arrayList = new ArrayList(2);
        for (CountryTimeZones countryTimeZones : this.countryTimeZonesList) {
            if (CountryTimeZones.TimeZoneMapping.containsTimeZoneId(countryTimeZones.getTimeZoneMappings(), str)) {
                arrayList.add(countryTimeZones);
            }
        }
        return Collections.unmodifiableList(arrayList);
    }

    public CountryTimeZones lookupCountryTimeZones(String str) {
        String strNormalizeCountryIso = TimeZoneFinder.normalizeCountryIso(str);
        for (CountryTimeZones countryTimeZones : this.countryTimeZonesList) {
            if (countryTimeZones.getCountryIso().equals(strNormalizeCountryIso)) {
                return countryTimeZones;
            }
        }
        return null;
    }
}
