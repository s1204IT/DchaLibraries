package com.android.internal.telephony;

import android.telephony.Rlog;
import com.android.internal.annotations.VisibleForTesting;
import java.util.Calendar;
import java.util.TimeZone;

@VisibleForTesting(visibility = VisibleForTesting.Visibility.PACKAGE)
public final class NitzData {
    private static final String LOG_TAG = "SST";
    private static final int MAX_NITZ_YEAR = 2037;
    private static final int MS_PER_QUARTER_HOUR = 900000;
    private final long mCurrentTimeMillis;
    private final Integer mDstOffset;
    private final TimeZone mEmulatorHostTimeZone;
    private final String mOriginalString;
    private final int mZoneOffset;

    private NitzData(String str, int i, Integer num, long j, TimeZone timeZone) {
        if (str == null) {
            throw new NullPointerException("originalString==null");
        }
        this.mOriginalString = str;
        this.mZoneOffset = i;
        this.mDstOffset = num;
        this.mCurrentTimeMillis = j;
        this.mEmulatorHostTimeZone = timeZone;
    }

    public static NitzData parse(String str) {
        Integer numValueOf;
        TimeZone timeZone;
        try {
            Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
            calendar.clear();
            calendar.set(16, 0);
            String[] strArrSplit = str.split("[/:,+-]");
            int i = 2000 + Integer.parseInt(strArrSplit[0]);
            if (i > MAX_NITZ_YEAR) {
                Rlog.e(LOG_TAG, "NITZ year: " + i + " exceeds limit, skip NITZ time update");
                return null;
            }
            int i2 = 1;
            calendar.set(1, i);
            calendar.set(2, Integer.parseInt(strArrSplit[1]) - 1);
            calendar.set(5, Integer.parseInt(strArrSplit[2]));
            calendar.set(10, Integer.parseInt(strArrSplit[3]));
            calendar.set(12, Integer.parseInt(strArrSplit[4]));
            calendar.set(13, Integer.parseInt(strArrSplit[5]));
            boolean z = str.indexOf(45) == -1;
            int i3 = Integer.parseInt(strArrSplit[6]);
            if (!z) {
                i2 = -1;
            }
            int i4 = i2 * i3 * MS_PER_QUARTER_HOUR;
            Integer numValueOf2 = strArrSplit.length >= 8 ? Integer.valueOf(Integer.parseInt(strArrSplit[7])) : null;
            if (numValueOf2 == null) {
                numValueOf = null;
            } else {
                numValueOf = Integer.valueOf(numValueOf2.intValue() * MS_PER_QUARTER_HOUR);
            }
            if (strArrSplit.length < 9) {
                timeZone = null;
            } else {
                timeZone = TimeZone.getTimeZone(strArrSplit[8].replace('!', '/'));
            }
            return new NitzData(str, i4, numValueOf, calendar.getTimeInMillis(), timeZone);
        } catch (RuntimeException e) {
            Rlog.e(LOG_TAG, "NITZ: Parsing NITZ time " + str + " ex=" + e);
            return null;
        }
    }

    public static NitzData createForTests(int i, Integer num, long j, TimeZone timeZone) {
        return new NitzData("Test data", i, num, j, timeZone);
    }

    public long getCurrentTimeInMillis() {
        return this.mCurrentTimeMillis;
    }

    public int getLocalOffsetMillis() {
        return this.mZoneOffset;
    }

    public Integer getDstAdjustmentMillis() {
        return this.mDstOffset;
    }

    public boolean isDst() {
        return (this.mDstOffset == null || this.mDstOffset.intValue() == 0) ? false : true;
    }

    public TimeZone getEmulatorHostTimeZone() {
        return this.mEmulatorHostTimeZone;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        NitzData nitzData = (NitzData) obj;
        if (this.mZoneOffset != nitzData.mZoneOffset || this.mCurrentTimeMillis != nitzData.mCurrentTimeMillis || !this.mOriginalString.equals(nitzData.mOriginalString)) {
            return false;
        }
        if (this.mDstOffset == null ? nitzData.mDstOffset != null : !this.mDstOffset.equals(nitzData.mDstOffset)) {
            return false;
        }
        if (this.mEmulatorHostTimeZone != null) {
            return this.mEmulatorHostTimeZone.equals(nitzData.mEmulatorHostTimeZone);
        }
        if (nitzData.mEmulatorHostTimeZone == null) {
            return true;
        }
        return false;
    }

    public int hashCode() {
        return (31 * ((((((this.mOriginalString.hashCode() * 31) + this.mZoneOffset) * 31) + (this.mDstOffset != null ? this.mDstOffset.hashCode() : 0)) * 31) + ((int) (this.mCurrentTimeMillis ^ (this.mCurrentTimeMillis >>> 32))))) + (this.mEmulatorHostTimeZone != null ? this.mEmulatorHostTimeZone.hashCode() : 0);
    }

    public String toString() {
        return "NitzData{mOriginalString=" + this.mOriginalString + ", mZoneOffset=" + this.mZoneOffset + ", mDstOffset=" + this.mDstOffset + ", mCurrentTimeMillis=" + this.mCurrentTimeMillis + ", mEmulatorHostTimeZone=" + this.mEmulatorHostTimeZone + '}';
    }
}
