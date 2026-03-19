package libcore.util;

import android.icu.impl.PatternTokenizer;
import android.icu.util.TimeZone;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

public final class CountryTimeZones {
    private final String countryIso;
    private final String defaultTimeZoneId;
    private final boolean everUsesUtc;
    private TimeZone icuDefaultTimeZone;
    private List<TimeZone> icuTimeZones;
    private final List<TimeZoneMapping> timeZoneMappings;

    public static final class OffsetResult {
        public final boolean mOneMatch;
        public final TimeZone mTimeZone;

        public OffsetResult(TimeZone timeZone, boolean z) {
            this.mTimeZone = (TimeZone) java.util.Objects.requireNonNull(timeZone);
            this.mOneMatch = z;
        }

        public String toString() {
            return "Result{mTimeZone='" + this.mTimeZone + PatternTokenizer.SINGLE_QUOTE + ", mOneMatch=" + this.mOneMatch + '}';
        }
    }

    public static final class TimeZoneMapping {
        public final Long notUsedAfter;
        public final boolean showInPicker;
        public final String timeZoneId;

        TimeZoneMapping(String str, boolean z, Long l) {
            this.timeZoneId = str;
            this.showInPicker = z;
            this.notUsedAfter = l;
        }

        public static TimeZoneMapping createForTests(String str, boolean z, Long l) {
            return new TimeZoneMapping(str, z, l);
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            TimeZoneMapping timeZoneMapping = (TimeZoneMapping) obj;
            if (this.showInPicker == timeZoneMapping.showInPicker && java.util.Objects.equals(this.timeZoneId, timeZoneMapping.timeZoneId) && java.util.Objects.equals(this.notUsedAfter, timeZoneMapping.notUsedAfter)) {
                return true;
            }
            return false;
        }

        public int hashCode() {
            return java.util.Objects.hash(this.timeZoneId, Boolean.valueOf(this.showInPicker), this.notUsedAfter);
        }

        public String toString() {
            return "TimeZoneMapping{timeZoneId='" + this.timeZoneId + PatternTokenizer.SINGLE_QUOTE + ", showInPicker=" + this.showInPicker + ", notUsedAfter=" + this.notUsedAfter + '}';
        }

        public static boolean containsTimeZoneId(List<TimeZoneMapping> list, String str) {
            Iterator<TimeZoneMapping> it = list.iterator();
            while (it.hasNext()) {
                if (it.next().timeZoneId.equals(str)) {
                    return true;
                }
            }
            return false;
        }
    }

    private CountryTimeZones(String str, String str2, boolean z, List<TimeZoneMapping> list) {
        this.countryIso = (String) java.util.Objects.requireNonNull(str);
        this.defaultTimeZoneId = str2;
        this.everUsesUtc = z;
        this.timeZoneMappings = Collections.unmodifiableList(new ArrayList(list));
    }

    public static CountryTimeZones createValidated(String str, String str2, boolean z, List<TimeZoneMapping> list, String str3) {
        HashSet hashSet = new HashSet(Arrays.asList(ZoneInfoDB.getInstance().getAvailableIDs()));
        ArrayList arrayList = new ArrayList();
        for (TimeZoneMapping timeZoneMapping : list) {
            String str4 = timeZoneMapping.timeZoneId;
            if (hashSet.contains(str4)) {
                arrayList.add(timeZoneMapping);
            } else {
                System.logW("Skipping invalid zone: " + str4 + " at " + str3);
            }
        }
        if (!hashSet.contains(str2)) {
            System.logW("Invalid default time zone ID: " + str2 + " at " + str3);
            str2 = null;
        }
        return new CountryTimeZones(normalizeCountryIso(str), str2, z, arrayList);
    }

    public String getCountryIso() {
        return this.countryIso;
    }

    public boolean isForCountryCode(String str) {
        return this.countryIso.equals(normalizeCountryIso(str));
    }

    public synchronized TimeZone getDefaultTimeZone() {
        TimeZone validFrozenTimeZoneOrNull;
        if (this.icuDefaultTimeZone == null) {
            if (this.defaultTimeZoneId == null) {
                validFrozenTimeZoneOrNull = null;
            } else {
                validFrozenTimeZoneOrNull = getValidFrozenTimeZoneOrNull(this.defaultTimeZoneId);
            }
            this.icuDefaultTimeZone = validFrozenTimeZoneOrNull;
        }
        return this.icuDefaultTimeZone;
    }

    public String getDefaultTimeZoneId() {
        return this.defaultTimeZoneId;
    }

    public List<TimeZoneMapping> getTimeZoneMappings() {
        return this.timeZoneMappings;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        CountryTimeZones countryTimeZones = (CountryTimeZones) obj;
        if (this.everUsesUtc != countryTimeZones.everUsesUtc || !this.countryIso.equals(countryTimeZones.countryIso)) {
            return false;
        }
        if (this.defaultTimeZoneId == null ? countryTimeZones.defaultTimeZoneId != null : !this.defaultTimeZoneId.equals(countryTimeZones.defaultTimeZoneId)) {
            return false;
        }
        return this.timeZoneMappings.equals(countryTimeZones.timeZoneMappings);
    }

    public int hashCode() {
        return (31 * ((((this.countryIso.hashCode() * 31) + (this.defaultTimeZoneId != null ? this.defaultTimeZoneId.hashCode() : 0)) * 31) + this.timeZoneMappings.hashCode())) + (this.everUsesUtc ? 1 : 0);
    }

    public synchronized List<TimeZone> getIcuTimeZones() {
        TimeZone validFrozenTimeZoneOrNull;
        if (this.icuTimeZones == null) {
            ArrayList arrayList = new ArrayList(this.timeZoneMappings.size());
            Iterator<TimeZoneMapping> it = this.timeZoneMappings.iterator();
            while (it.hasNext()) {
                String str = it.next().timeZoneId;
                if (str.equals(this.defaultTimeZoneId)) {
                    validFrozenTimeZoneOrNull = getDefaultTimeZone();
                } else {
                    validFrozenTimeZoneOrNull = getValidFrozenTimeZoneOrNull(str);
                }
                if (validFrozenTimeZoneOrNull == null) {
                    System.logW("Skipping invalid zone: " + str);
                } else {
                    arrayList.add(validFrozenTimeZoneOrNull);
                }
            }
            this.icuTimeZones = Collections.unmodifiableList(arrayList);
        }
        return this.icuTimeZones;
    }

    public boolean hasUtcZone(long j) {
        if (!this.everUsesUtc) {
            return false;
        }
        Iterator<TimeZone> it = getIcuTimeZones().iterator();
        while (it.hasNext()) {
            if (it.next().getOffset(j) == 0) {
                return true;
            }
        }
        return false;
    }

    public boolean isDefaultOkForCountryTimeZoneDetection(long j) {
        if (this.timeZoneMappings.isEmpty()) {
            return false;
        }
        if (this.timeZoneMappings.size() == 1) {
            return true;
        }
        TimeZone defaultTimeZone = getDefaultTimeZone();
        if (defaultTimeZone == null) {
            return false;
        }
        int offset = defaultTimeZone.getOffset(j);
        for (TimeZone timeZone : getIcuTimeZones()) {
            if (timeZone != defaultTimeZone && offset != timeZone.getOffset(j)) {
                return false;
            }
        }
        return true;
    }

    @Deprecated
    public OffsetResult lookupByOffsetWithBias(int i, boolean z, long j, TimeZone timeZone) {
        if (this.timeZoneMappings == null || this.timeZoneMappings.isEmpty()) {
            return null;
        }
        TimeZone timeZone2 = null;
        boolean z2 = true;
        boolean z3 = false;
        for (TimeZone timeZone3 : getIcuTimeZones()) {
            if (offsetMatchesAtTime(timeZone3, i, z, j)) {
                if (timeZone2 == null) {
                    timeZone2 = timeZone3;
                } else {
                    z2 = false;
                }
                if (timeZone != null && timeZone3.getID().equals(timeZone.getID())) {
                    z3 = true;
                }
                if (timeZone2 != null && !z2 && (timeZone == null || z3)) {
                    break;
                }
            }
        }
        if (timeZone2 == null) {
            return null;
        }
        if (!z3) {
            timeZone = timeZone2;
        }
        return new OffsetResult(timeZone, z2);
    }

    private static boolean offsetMatchesAtTime(TimeZone timeZone, int i, boolean z, long j) {
        int[] iArr = new int[2];
        timeZone.getOffset(j, false, iArr);
        if (z != (iArr[1] != 0) || i != iArr[0] + iArr[1]) {
            return false;
        }
        return true;
    }

    public OffsetResult lookupByOffsetWithBias(int i, Integer num, long j, TimeZone timeZone) {
        if (this.timeZoneMappings == null || this.timeZoneMappings.isEmpty()) {
            return null;
        }
        TimeZone timeZone2 = null;
        boolean z = true;
        boolean z2 = false;
        for (TimeZone timeZone3 : getIcuTimeZones()) {
            if (offsetMatchesAtTime(timeZone3, i, num, j)) {
                if (timeZone2 == null) {
                    timeZone2 = timeZone3;
                } else {
                    z = false;
                }
                if (timeZone != null && timeZone3.getID().equals(timeZone.getID())) {
                    z2 = true;
                }
                if (timeZone2 != null && !z && (timeZone == null || z2)) {
                    break;
                }
            }
        }
        if (timeZone2 == null) {
            return null;
        }
        if (!z2) {
            timeZone = timeZone2;
        }
        return new OffsetResult(timeZone, z);
    }

    private static boolean offsetMatchesAtTime(TimeZone timeZone, int i, Integer num, long j) {
        int[] iArr = new int[2];
        timeZone.getOffset(j, false, iArr);
        if ((num != null && num.intValue() != iArr[1]) || i != iArr[0] + iArr[1]) {
            return false;
        }
        return true;
    }

    private static TimeZone getValidFrozenTimeZoneOrNull(String str) {
        TimeZone frozenTimeZone = TimeZone.getFrozenTimeZone(str);
        if (frozenTimeZone.getID().equals(TimeZone.UNKNOWN_ZONE_ID)) {
            return null;
        }
        return frozenTimeZone;
    }

    private static String normalizeCountryIso(String str) {
        return str.toLowerCase(Locale.US);
    }
}
