package com.android.deskclock.worldclock;

import java.util.Comparator;
import java.util.TimeZone;

public class CityGmtOffsetComparator implements Comparator<CityObj> {
    private CityNameComparator mNameComparator = null;

    @Override
    public int compare(CityObj c1, CityObj c2) {
        long currentTime = System.currentTimeMillis();
        int offset = TimeZone.getTimeZone(c1.mTimeZone).getOffset(currentTime);
        int offset2 = TimeZone.getTimeZone(c2.mTimeZone).getOffset(currentTime);
        if (offset < offset2) {
            return -1;
        }
        if (offset > offset2) {
            return 1;
        }
        return getCityNameComparator().compare(c1, c2);
    }

    private CityNameComparator getCityNameComparator() {
        if (this.mNameComparator == null) {
            this.mNameComparator = new CityNameComparator();
        }
        return this.mNameComparator;
    }
}
