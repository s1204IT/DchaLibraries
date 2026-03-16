package com.android.timezonepicker;

import android.content.Context;
import android.text.Spannable;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.util.SparseArray;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Formatter;
import java.util.Locale;
import java.util.TimeZone;

public class TimeZoneInfo implements Comparable<TimeZoneInfo> {
    public static boolean is24HourFormat;
    private static long mGmtDisplayNameUpdateTime;
    public String mCountry;
    public String mDisplayName;
    int mRawoffset;
    public int[] mTransitions;
    TimeZone mTz;
    public String mTzId;
    private static final String TAG = null;
    public static int NUM_OF_TRANSITIONS = 6;
    public static long time = System.currentTimeMillis() / 1000;
    private static final Spannable.Factory mSpannableFactory = Spannable.Factory.getInstance();
    private static StringBuilder mSB = new StringBuilder(50);
    private static Formatter mFormatter = new Formatter(mSB, Locale.getDefault());
    private static SparseArray<CharSequence> mGmtDisplayNameCache = new SparseArray<>();
    private Time recycledTime = new Time();
    SparseArray<String> mLocalTimeCache = new SparseArray<>();
    long mLocalTimeCacheReferenceTime = 0;

    public TimeZoneInfo(TimeZone tz, String country) {
        this.mTz = tz;
        this.mTzId = tz.getID();
        this.mCountry = country;
        this.mRawoffset = tz.getRawOffset();
        try {
            this.mTransitions = getTransitions(tz, time);
        } catch (IllegalAccessException ignored) {
            ignored.printStackTrace();
        } catch (NoSuchFieldException e) {
        }
    }

    public String getLocalTime(long referenceTime) {
        this.recycledTime.timezone = TimeZone.getDefault().getID();
        this.recycledTime.set(referenceTime);
        int currYearDay = (this.recycledTime.year * 366) + this.recycledTime.yearDay;
        this.recycledTime.timezone = this.mTzId;
        this.recycledTime.set(referenceTime);
        String localTimeStr = null;
        int hourMinute = (this.recycledTime.hour * 60) + this.recycledTime.minute;
        if (this.mLocalTimeCacheReferenceTime != referenceTime) {
            this.mLocalTimeCacheReferenceTime = referenceTime;
            this.mLocalTimeCache.clear();
        } else {
            String localTimeStr2 = this.mLocalTimeCache.get(hourMinute);
            localTimeStr = localTimeStr2;
        }
        if (localTimeStr == null) {
            String format = "%I:%M %p";
            if (currYearDay != (this.recycledTime.year * 366) + this.recycledTime.yearDay) {
                if (is24HourFormat) {
                    format = "%b %d %H:%M";
                } else {
                    format = "%b %d %I:%M %p";
                }
            } else if (is24HourFormat) {
                format = "%H:%M";
            }
            String localTimeStr3 = this.recycledTime.format(format);
            this.mLocalTimeCache.put(hourMinute, localTimeStr3);
            return localTimeStr3;
        }
        return localTimeStr;
    }

    public int getNowOffsetMillis() {
        return this.mTz.getOffset(System.currentTimeMillis());
    }

    public synchronized CharSequence getGmtDisplayName(Context context) {
        int cacheKey;
        CharSequence displayName;
        long nowMinute = System.currentTimeMillis() / 60000;
        long now = nowMinute * 60000;
        int gmtOffset = this.mTz.getOffset(now);
        boolean hasFutureDST = this.mTz.useDaylightTime();
        if (hasFutureDST) {
            cacheKey = (int) (((long) gmtOffset) + 129600000);
        } else {
            cacheKey = (int) (((long) gmtOffset) - 129600000);
        }
        displayName = null;
        if (mGmtDisplayNameUpdateTime != nowMinute) {
            mGmtDisplayNameUpdateTime = nowMinute;
            mGmtDisplayNameCache.clear();
        } else {
            CharSequence displayName2 = mGmtDisplayNameCache.get(cacheKey);
            displayName = displayName2;
        }
        if (displayName == null) {
            mSB.setLength(0);
            int flags = 524288 | 1;
            if (is24HourFormat) {
                flags |= 128;
            }
            DateUtils.formatDateRange(context, mFormatter, now, now, flags, this.mTzId);
            mSB.append("  ");
            int gmtStart = mSB.length();
            TimeZonePickerUtils.appendGmtOffset(mSB, gmtOffset);
            int gmtEnd = mSB.length();
            int symbolStart = 0;
            int symbolEnd = 0;
            if (hasFutureDST) {
                mSB.append(' ');
                symbolStart = mSB.length();
                mSB.append(TimeZonePickerUtils.getDstSymbol());
                symbolEnd = mSB.length();
            }
            Spannable spannableText = mSpannableFactory.newSpannable(mSB);
            spannableText.setSpan(new ForegroundColorSpan(-7829368), gmtStart, gmtEnd, 33);
            if (hasFutureDST) {
                spannableText.setSpan(new ForegroundColorSpan(-4210753), symbolStart, symbolEnd, 33);
            }
            displayName = spannableText;
            mGmtDisplayNameCache.put(cacheKey, displayName);
        }
        return displayName;
    }

    private static int[] getTransitions(TimeZone tz, long time2) throws IllegalAccessException, NoSuchFieldException {
        Class<?> zoneInfoClass = tz.getClass();
        Field mTransitionsField = zoneInfoClass.getDeclaredField("mTransitions");
        mTransitionsField.setAccessible(true);
        int[] objTransitions = (int[]) mTransitionsField.get(tz);
        int[] transitions = null;
        if (objTransitions.length != 0) {
            transitions = new int[NUM_OF_TRANSITIONS];
            int numOfTransitions = 0;
            for (int i = 0; i < objTransitions.length; i++) {
                if (objTransitions[i] >= time2) {
                    int numOfTransitions2 = numOfTransitions + 1;
                    transitions[numOfTransitions] = objTransitions[i];
                    if (numOfTransitions2 == NUM_OF_TRANSITIONS) {
                        break;
                    }
                    numOfTransitions = numOfTransitions2;
                }
            }
        }
        return transitions;
    }

    public boolean hasSameRules(TimeZoneInfo tzi) {
        return this.mRawoffset == tzi.mRawoffset && Arrays.equals(this.mTransitions, tzi.mTransitions);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        String country = this.mCountry;
        TimeZone tz = this.mTz;
        sb.append(this.mTzId);
        sb.append(',');
        sb.append(tz.getDisplayName(false, 1));
        sb.append(',');
        sb.append(tz.getDisplayName(false, 0));
        sb.append(',');
        if (tz.useDaylightTime()) {
            sb.append(tz.getDisplayName(true, 1));
            sb.append(',');
            sb.append(tz.getDisplayName(true, 0));
        } else {
            sb.append(',');
        }
        sb.append(',');
        sb.append(tz.getRawOffset() / 3600000.0f);
        sb.append(',');
        sb.append(tz.getDSTSavings() / 3600000.0f);
        sb.append(',');
        sb.append(country);
        sb.append(',');
        sb.append(getLocalTime(1357041600000L));
        sb.append(',');
        sb.append(getLocalTime(1363348800000L));
        sb.append(',');
        sb.append(getLocalTime(1372680000000L));
        sb.append(',');
        sb.append(getLocalTime(1383307200000L));
        sb.append(',');
        sb.append('\n');
        return sb.toString();
    }

    @Override
    public int compareTo(TimeZoneInfo other) {
        if (getNowOffsetMillis() != other.getNowOffsetMillis()) {
            return other.getNowOffsetMillis() < getNowOffsetMillis() ? -1 : 1;
        }
        if (this.mCountry == null && other.mCountry != null) {
            return 1;
        }
        if (other.mCountry == null) {
            return -1;
        }
        int diff = this.mCountry.compareTo(other.mCountry);
        if (diff != 0) {
            return diff;
        }
        if (Arrays.equals(this.mTransitions, other.mTransitions)) {
            Log.e(TAG, "Not expected to be comparing tz with the same country, same offset, same dst, same transitions:\n" + toString() + "\n" + other.toString());
        }
        if (this.mDisplayName != null && other.mDisplayName != null) {
            return this.mDisplayName.compareTo(other.mDisplayName);
        }
        return this.mTz.getDisplayName(Locale.getDefault()).compareTo(other.mTz.getDisplayName(Locale.getDefault()));
    }
}
