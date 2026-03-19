package com.android.internal.telephony;

import android.icu.util.TimeZone;
import android.text.TextUtils;
import java.util.Date;
import libcore.util.CountryTimeZones;
import libcore.util.TimeZoneFinder;

public class TimeZoneLookupHelper {
    private static final int MS_PER_HOUR = 3600000;
    private CountryTimeZones mLastCountryTimeZones;

    public static final class OffsetResult {
        public final boolean isOnlyMatch;
        public final String zoneId;

        public OffsetResult(String str, boolean z) {
            this.zoneId = str;
            this.isOnlyMatch = z;
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            OffsetResult offsetResult = (OffsetResult) obj;
            if (this.isOnlyMatch != offsetResult.isOnlyMatch) {
                return false;
            }
            return this.zoneId.equals(offsetResult.zoneId);
        }

        public int hashCode() {
            return (31 * this.zoneId.hashCode()) + (this.isOnlyMatch ? 1 : 0);
        }

        public String toString() {
            return "Result{zoneId='" + this.zoneId + "', isOnlyMatch=" + this.isOnlyMatch + '}';
        }
    }

    public static final class CountryResult {
        public final boolean allZonesHaveSameOffset;
        public final long whenMillis;
        public final String zoneId;

        public CountryResult(String str, boolean z, long j) {
            this.zoneId = str;
            this.allZonesHaveSameOffset = z;
            this.whenMillis = j;
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            CountryResult countryResult = (CountryResult) obj;
            if (this.allZonesHaveSameOffset != countryResult.allZonesHaveSameOffset || this.whenMillis != countryResult.whenMillis) {
                return false;
            }
            return this.zoneId.equals(countryResult.zoneId);
        }

        public int hashCode() {
            return (31 * ((this.zoneId.hashCode() * 31) + (this.allZonesHaveSameOffset ? 1 : 0))) + ((int) (this.whenMillis ^ (this.whenMillis >>> 32)));
        }

        public String toString() {
            return "CountryResult{zoneId='" + this.zoneId + "', allZonesHaveSameOffset=" + this.allZonesHaveSameOffset + ", whenMillis=" + this.whenMillis + '}';
        }
    }

    public OffsetResult lookupByNitzCountry(NitzData nitzData, String str) {
        CountryTimeZones countryTimeZones = getCountryTimeZones(str);
        if (countryTimeZones == null) {
            return null;
        }
        CountryTimeZones.OffsetResult offsetResultLookupByOffsetWithBias = countryTimeZones.lookupByOffsetWithBias(nitzData.getLocalOffsetMillis(), nitzData.isDst(), nitzData.getCurrentTimeInMillis(), TimeZone.getDefault());
        if (offsetResultLookupByOffsetWithBias == null) {
            return null;
        }
        return new OffsetResult(offsetResultLookupByOffsetWithBias.mTimeZone.getID(), offsetResultLookupByOffsetWithBias.mOneMatch);
    }

    public OffsetResult lookupByNitz(NitzData nitzData) {
        return lookupByNitzStatic(nitzData);
    }

    public CountryResult lookupByCountry(String str, long j) {
        CountryTimeZones countryTimeZones = getCountryTimeZones(str);
        if (countryTimeZones == null || countryTimeZones.getDefaultTimeZoneId() == null) {
            return null;
        }
        return new CountryResult(countryTimeZones.getDefaultTimeZoneId(), countryTimeZones.isDefaultOkForCountryTimeZoneDetection(j), j);
    }

    static java.util.TimeZone guessZoneByNitzStatic(NitzData nitzData) {
        OffsetResult offsetResultLookupByNitzStatic = lookupByNitzStatic(nitzData);
        if (offsetResultLookupByNitzStatic != null) {
            return java.util.TimeZone.getTimeZone(offsetResultLookupByNitzStatic.zoneId);
        }
        return null;
    }

    private static OffsetResult lookupByNitzStatic(NitzData nitzData) {
        int localOffsetMillis = nitzData.getLocalOffsetMillis();
        boolean zIsDst = nitzData.isDst();
        long currentTimeInMillis = nitzData.getCurrentTimeInMillis();
        OffsetResult offsetResultLookupByInstantOffsetDst = lookupByInstantOffsetDst(currentTimeInMillis, localOffsetMillis, zIsDst);
        if (offsetResultLookupByInstantOffsetDst == null) {
            return lookupByInstantOffsetDst(currentTimeInMillis, localOffsetMillis, !zIsDst);
        }
        return offsetResultLookupByInstantOffsetDst;
    }

    private static OffsetResult lookupByInstantOffsetDst(long j, int i, boolean z) {
        int i2;
        if (z) {
            i2 = i - 3600000;
        } else {
            i2 = i;
        }
        String[] availableIDs = java.util.TimeZone.getAvailableIDs(i2);
        Date date = new Date(j);
        int length = availableIDs.length;
        boolean z2 = false;
        int i3 = 0;
        java.util.TimeZone timeZone = null;
        while (true) {
            if (i3 < length) {
                java.util.TimeZone timeZone2 = java.util.TimeZone.getTimeZone(availableIDs[i3]);
                if (timeZone2.getOffset(j) == i && timeZone2.inDaylightTime(date) == z) {
                    if (timeZone != null) {
                        break;
                    }
                    timeZone = timeZone2;
                }
                i3++;
            } else {
                z2 = true;
                break;
            }
        }
        if (timeZone == null) {
            return null;
        }
        return new OffsetResult(timeZone.getID(), z2);
    }

    public boolean countryUsesUtc(String str, long j) {
        CountryTimeZones countryTimeZones;
        return (TextUtils.isEmpty(str) || (countryTimeZones = getCountryTimeZones(str)) == null || !countryTimeZones.hasUtcZone(j)) ? false : true;
    }

    private CountryTimeZones getCountryTimeZones(String str) {
        synchronized (this) {
            if (this.mLastCountryTimeZones != null && this.mLastCountryTimeZones.isForCountryCode(str)) {
                return this.mLastCountryTimeZones;
            }
            CountryTimeZones countryTimeZonesLookupCountryTimeZones = TimeZoneFinder.getInstance().lookupCountryTimeZones(str);
            if (countryTimeZonesLookupCountryTimeZones != null) {
                this.mLastCountryTimeZones = countryTimeZonesLookupCountryTimeZones;
            }
            return countryTimeZonesLookupCountryTimeZones;
        }
    }
}
