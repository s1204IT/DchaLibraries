package com.android.deskclock.worldclock;

import java.text.Collator;
import java.util.Comparator;

public class CityNameComparator implements Comparator<CityObj> {
    private Collator mCollator = Collator.getInstance();

    @Override
    public int compare(CityObj c1, CityObj c2) {
        return this.mCollator.compare(c1.mCityName, c2.mCityName);
    }
}
