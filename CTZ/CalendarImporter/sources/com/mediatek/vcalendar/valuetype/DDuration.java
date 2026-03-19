package com.mediatek.vcalendar.valuetype;

import com.mediatek.vcalendar.utils.LogUtil;
import com.mediatek.vcalendar.utils.StringUtil;

public final class DDuration {
    private static final String DAY_AGGR = "D";
    private static final int D_FLAG = 2;
    private static final int FLAGS_CONT = 7;
    private static final String HOUR_AGGR = "H";
    private static final int H_FLAG = 4;
    public static final long MILLIS_IN_MIN = 60000;
    public static final long MILLIS_IN_SECOND = 1000;
    private static final String MINUS = "-";
    public static final long MINUTES_IN_DAY = 1440;
    public static final long MINUTES_IN_HOUR = 60;
    public static final long MINUTES_IN_WEEK = 10080;
    private static final String MIN_AGGR = "M";
    private static final int M_FLAG = 5;
    private static final String P = "P";
    private static final int P_FLAG = 0;
    private static final String SECOND_AGGR = "S";
    private static final int S_FLAG = 6;
    private static final String T = "T";
    private static final String TAG = "Duration";
    private static final int T_FLAG = 3;
    private static final String WEEK_AGGR = "W";
    private static final int W_FLAG = 1;

    private DDuration() {
    }

    public static String getDurationString(long j) {
        LogUtil.d(TAG, "getDurationString(): minutes = " + j);
        StringBuilder sb = new StringBuilder();
        if (j < 0) {
            sb.append(MINUS);
            j *= -1;
        }
        sb.append(P);
        long j2 = j / MINUTES_IN_WEEK;
        long j3 = j % MINUTES_IN_WEEK;
        if (j2 > 0) {
            sb.append(j2);
            sb.append(WEEK_AGGR);
        }
        long j4 = j3 / MINUTES_IN_DAY;
        long j5 = j3 % MINUTES_IN_DAY;
        if (j4 > 0) {
            sb.append(j4);
            sb.append(DAY_AGGR);
        }
        if (j5 >= 0) {
            sb.append(T);
        }
        long j6 = j5 / 60;
        long j7 = j5 % 60;
        if (j6 > 0) {
            sb.append(j6);
            sb.append(HOUR_AGGR);
        }
        if (j7 >= 0) {
            sb.append(j7);
            sb.append(MIN_AGGR);
        }
        return sb.toString();
    }

    public static long getDurationMillis(String str) {
        int i;
        LogUtil.d(TAG, "getDurationMillis(): duration = " + str);
        if (StringUtil.isNullOrEmpty(str)) {
            LogUtil.e(TAG, "getDurationMillis(): the given duration is null or empty.");
            return -1L;
        }
        if (!str.contains(P)) {
            LogUtil.e(TAG, "getDurationMillis(): the given duration is not a rfc5545 duration.");
            return -1L;
        }
        int[] iArr = {str.indexOf(P), str.indexOf(WEEK_AGGR), str.indexOf(DAY_AGGR), str.indexOf(T), str.indexOf(HOUR_AGGR), str.indexOf(MIN_AGGR), str.indexOf(SECOND_AGGR)};
        int i2 = iArr[0] + 1;
        long jIntValue = 0;
        for (int i3 = 0; i3 < 7; i3++) {
            if (iArr[i3] != -1 && i2 <= (i = iArr[i3])) {
                if (i2 == i) {
                    i2++;
                } else {
                    String strSubstring = str.substring(i2, i);
                    LogUtil.d(TAG, "getDurationMillis(): subString = " + strSubstring);
                    switch (i3) {
                        case 1:
                            jIntValue += ((long) Long.valueOf(strSubstring).intValue()) * MINUTES_IN_WEEK * MILLIS_IN_MIN;
                            break;
                        case 2:
                            jIntValue += ((long) Long.valueOf(strSubstring).intValue()) * MINUTES_IN_DAY * MILLIS_IN_MIN;
                            break;
                        case 4:
                            jIntValue += ((long) Long.valueOf(strSubstring).intValue()) * 60 * MILLIS_IN_MIN;
                            break;
                        case 5:
                            jIntValue += ((long) Long.valueOf(strSubstring).intValue()) * MILLIS_IN_MIN;
                            break;
                        case 6:
                            jIntValue += ((long) Long.valueOf(strSubstring).intValue()) * 1000;
                            break;
                    }
                    i2 = i + 1;
                }
            }
        }
        if (str.contains(MINUS)) {
            jIntValue *= -1;
        }
        LogUtil.d(TAG, "getDurationMillis(): duration millis = " + jIntValue);
        return jIntValue;
    }
}
