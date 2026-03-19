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
    public long[] mTransitions;
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

    public TimeZoneInfo(TimeZone timeZone, String str) {
        this.mTz = timeZone;
        this.mTzId = timeZone.getID();
        this.mCountry = str;
        this.mRawoffset = timeZone.getRawOffset();
        try {
            this.mTransitions = getTransitions(timeZone, time);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e2) {
        }
    }

    public String getLocalTime(long j) {
        String str;
        this.recycledTime.timezone = TimeZone.getDefault().getID();
        this.recycledTime.set(j);
        int i = (this.recycledTime.year * 366) + this.recycledTime.yearDay;
        this.recycledTime.timezone = this.mTzId;
        this.recycledTime.set(j);
        int i2 = (this.recycledTime.hour * 60) + this.recycledTime.minute;
        if (this.mLocalTimeCacheReferenceTime != j) {
            this.mLocalTimeCacheReferenceTime = j;
            this.mLocalTimeCache.clear();
            str = null;
        } else {
            str = this.mLocalTimeCache.get(i2);
        }
        if (str == null) {
            String str2 = "%I:%M %p";
            if (i != (this.recycledTime.year * 366) + this.recycledTime.yearDay) {
                if (is24HourFormat) {
                    str2 = "%b %d %H:%M";
                } else {
                    str2 = "%b %d %I:%M %p";
                }
            } else if (is24HourFormat) {
                str2 = "%H:%M";
            }
            String str3 = this.recycledTime.format(str2);
            this.mLocalTimeCache.put(i2, str3);
            return str3;
        }
        return str;
    }

    public int getNowOffsetMillis() {
        return this.mTz.getOffset(System.currentTimeMillis());
    }

    public synchronized CharSequence getGmtDisplayName(Context context) {
        ?? NewSpannable;
        int length;
        long jCurrentTimeMillis = System.currentTimeMillis() / 60000;
        long j = jCurrentTimeMillis * 60000;
        int offset = this.mTz.getOffset(j);
        boolean zUseDaylightTime = this.mTz.useDaylightTime();
        int i = zUseDaylightTime ? (int) (((long) offset) + 129600000) : (int) (((long) offset) - 129600000);
        NewSpannable = 0;
        if (mGmtDisplayNameUpdateTime != jCurrentTimeMillis) {
            mGmtDisplayNameUpdateTime = jCurrentTimeMillis;
            mGmtDisplayNameCache.clear();
        } else {
            NewSpannable = mGmtDisplayNameCache.get(i);
        }
        if (NewSpannable == 0) {
            int length2 = 0;
            mSB.setLength(0);
            int i2 = 524289;
            if (is24HourFormat) {
                i2 = 524417;
            }
            DateUtils.formatDateRange(context, mFormatter, j, j, i2, this.mTzId);
            mSB.append("  ");
            int length3 = mSB.length();
            TimeZonePickerUtils.appendGmtOffset(mSB, offset);
            int length4 = mSB.length();
            if (zUseDaylightTime) {
                mSB.append(' ');
                length2 = mSB.length();
                mSB.append(TimeZonePickerUtils.getDstSymbol());
                length = mSB.length();
            } else {
                length = 0;
            }
            NewSpannable = mSpannableFactory.newSpannable(mSB);
            NewSpannable.setSpan(new ForegroundColorSpan(-7829368), length3, length4, 33);
            if (zUseDaylightTime) {
                NewSpannable.setSpan(new ForegroundColorSpan(-4210753), length2, length, 33);
            }
            mGmtDisplayNameCache.put(i, (CharSequence) NewSpannable);
        }
        return NewSpannable;
    }

    private static long[] getTransitions(TimeZone timeZone, long j) throws IllegalAccessException, NoSuchFieldException {
        Field declaredField = timeZone.getClass().getDeclaredField("mTransitions");
        declaredField.setAccessible(true);
        long[] jArr = (long[]) declaredField.get(timeZone);
        if (jArr.length != 0) {
            long[] jArr2 = new long[NUM_OF_TRANSITIONS];
            int i = 0;
            for (int i2 = 0; i2 < jArr.length; i2++) {
                if (jArr[i2] >= j) {
                    int i3 = i + 1;
                    jArr2[i] = jArr[i2];
                    if (i3 == NUM_OF_TRANSITIONS) {
                        return jArr2;
                    }
                    i = i3;
                }
            }
            return jArr2;
        }
        return null;
    }

    public boolean hasSameRules(TimeZoneInfo timeZoneInfo) {
        return this.mRawoffset == timeZoneInfo.mRawoffset && Arrays.equals(this.mTransitions, timeZoneInfo.mTransitions);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        String str = this.mCountry;
        TimeZone timeZone = this.mTz;
        sb.append(this.mTzId);
        sb.append(',');
        sb.append(timeZone.getDisplayName(false, 1));
        sb.append(',');
        sb.append(timeZone.getDisplayName(false, 0));
        sb.append(',');
        if (timeZone.useDaylightTime()) {
            sb.append(timeZone.getDisplayName(true, 1));
            sb.append(',');
            sb.append(timeZone.getDisplayName(true, 0));
        } else {
            sb.append(',');
        }
        sb.append(',');
        sb.append(timeZone.getRawOffset() / 3600000.0f);
        sb.append(',');
        sb.append(timeZone.getDSTSavings() / 3600000.0f);
        sb.append(',');
        sb.append(str);
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
    public int compareTo(TimeZoneInfo timeZoneInfo) {
        if (getNowOffsetMillis() != timeZoneInfo.getNowOffsetMillis()) {
            return timeZoneInfo.getNowOffsetMillis() < getNowOffsetMillis() ? -1 : 1;
        }
        if (this.mCountry == null && timeZoneInfo.mCountry != null) {
            return 1;
        }
        if (timeZoneInfo.mCountry == null) {
            return -1;
        }
        int iCompareTo = this.mCountry.compareTo(timeZoneInfo.mCountry);
        if (iCompareTo != 0) {
            return iCompareTo;
        }
        if (Arrays.equals(this.mTransitions, timeZoneInfo.mTransitions)) {
            Log.e(TAG, "Not expected to be comparing tz with the same country, same offset, same dst, same transitions:\n" + toString() + "\n" + timeZoneInfo.toString());
        }
        if (this.mDisplayName != null && timeZoneInfo.mDisplayName != null) {
            return this.mDisplayName.compareTo(timeZoneInfo.mDisplayName);
        }
        return this.mTz.getDisplayName(Locale.getDefault()).compareTo(timeZoneInfo.mTz.getDisplayName(Locale.getDefault()));
    }
}
