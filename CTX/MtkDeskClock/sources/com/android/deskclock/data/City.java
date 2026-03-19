package com.android.deskclock.data;

import com.google.android.flexbox.BuildConfig;
import java.text.Collator;
import java.util.Comparator;
import java.util.Locale;
import java.util.TimeZone;

public final class City {
    private final String mId;
    private final int mIndex;
    private final String mIndexString;
    private final String mName;
    private String mNameUpperCase;
    private String mNameUpperCaseNoSpecialCharacters;
    private final String mPhoneticName;
    private final TimeZone mTimeZone;

    City(String str, int i, String str2, String str3, String str4, TimeZone timeZone) {
        this.mId = str;
        this.mIndex = i;
        this.mIndexString = str2;
        this.mName = str3;
        this.mPhoneticName = str4;
        this.mTimeZone = timeZone;
    }

    public String getId() {
        return this.mId;
    }

    public int getIndex() {
        return this.mIndex;
    }

    public String getName() {
        return this.mName;
    }

    public TimeZone getTimeZone() {
        return this.mTimeZone;
    }

    public String getIndexString() {
        return this.mIndexString;
    }

    public String getPhoneticName() {
        return this.mPhoneticName;
    }

    public String getNameUpperCase() {
        if (this.mNameUpperCase == null) {
            this.mNameUpperCase = this.mName.toUpperCase();
        }
        return this.mNameUpperCase;
    }

    private String getNameUpperCaseNoSpecialCharacters() {
        if (this.mNameUpperCaseNoSpecialCharacters == null) {
            this.mNameUpperCaseNoSpecialCharacters = removeSpecialCharacters(getNameUpperCase());
        }
        return this.mNameUpperCaseNoSpecialCharacters;
    }

    public boolean matches(String str) {
        return getNameUpperCaseNoSpecialCharacters().startsWith(str);
    }

    public String toString() {
        return String.format(Locale.US, "City {id=%s, index=%d, indexString=%s, name=%s, phonetic=%s, tz=%s}", this.mId, Integer.valueOf(this.mIndex), this.mIndexString, this.mName, this.mPhoneticName, this.mTimeZone.getID());
    }

    public static String removeSpecialCharacters(String str) {
        return str.replaceAll("[ -.']", BuildConfig.FLAVOR);
    }

    public static final class UtcOffsetComparator implements Comparator<City> {
        private final Comparator<City> mDelegate1 = new UtcOffsetIndexComparator();
        private final Comparator<City> mDelegate2 = new NameComparator();

        @Override
        public int compare(City city, City city2) {
            int iCompare = this.mDelegate1.compare(city, city2);
            if (iCompare == 0) {
                return this.mDelegate2.compare(city, city2);
            }
            return iCompare;
        }
    }

    public static final class UtcOffsetIndexComparator implements Comparator<City> {
        private final long now = System.currentTimeMillis();

        @Override
        public int compare(City city, City city2) {
            return Integer.compare(city.getTimeZone().getOffset(this.now), city2.getTimeZone().getOffset(this.now));
        }
    }

    public static final class NameComparator implements Comparator<City> {
        private final Comparator<City> mDelegate = new NameIndexComparator();
        private final Collator mNameCollator = Collator.getInstance();

        @Override
        public int compare(City city, City city2) {
            int iCompare = this.mDelegate.compare(city, city2);
            if (iCompare == 0) {
                return this.mNameCollator.compare(city.getPhoneticName(), city2.getPhoneticName());
            }
            return iCompare;
        }
    }

    public static final class NameIndexComparator implements Comparator<City> {
        private final Collator mNameCollator = Collator.getInstance();

        @Override
        public int compare(City city, City city2) {
            int iCompare = Integer.compare(city.getIndex(), city2.getIndex());
            if (iCompare == 0) {
                return this.mNameCollator.compare(city.getIndexString(), city2.getIndexString());
            }
            return iCompare;
        }
    }
}
