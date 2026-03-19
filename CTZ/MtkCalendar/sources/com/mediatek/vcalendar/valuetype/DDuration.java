package com.mediatek.vcalendar.valuetype;

import com.mediatek.vcalendar.utils.LogUtil;
import com.mediatek.vcalendar.utils.StringUtil;

public final class DDuration {
    public static String getDurationString(long j) {
        LogUtil.d("Duration", "getDurationString(): minutes = " + j);
        StringBuilder sb = new StringBuilder();
        if (j < 0) {
            sb.append("-");
            j *= -1;
        }
        sb.append("P");
        long j2 = j / 10080;
        long j3 = j % 10080;
        if (j2 > 0) {
            sb.append(j2);
            sb.append("W");
        }
        long j4 = j3 / 1440;
        long j5 = j3 % 1440;
        if (j4 > 0) {
            sb.append(j4);
            sb.append("D");
        }
        if (j5 >= 0) {
            sb.append("T");
        }
        long j6 = j5 / 60;
        long j7 = j5 % 60;
        if (j6 > 0) {
            sb.append(j6);
            sb.append("H");
        }
        if (j7 >= 0) {
            sb.append(j7);
            sb.append("M");
        }
        return sb.toString();
    }

    public static long getDurationMillis(String str) {
        int i;
        LogUtil.d("Duration", "getDurationMillis(): duration = " + str);
        if (StringUtil.isNullOrEmpty(str)) {
            LogUtil.e("Duration", "getDurationMillis(): the given duration is null or empty.");
            return -1L;
        }
        if (!str.contains("P")) {
            LogUtil.e("Duration", "getDurationMillis(): the given duration is not a rfc5545 duration.");
            return -1L;
        }
        int[] iArr = {str.indexOf("P"), str.indexOf("W"), str.indexOf("D"), str.indexOf("T"), str.indexOf("H"), str.indexOf("M"), str.indexOf("S")};
        int i2 = iArr[0] + 1;
        long jIntValue = 0;
        for (int i3 = 0; i3 < 7; i3++) {
            if (iArr[i3] != -1 && i2 <= (i = iArr[i3])) {
                if (i2 == i) {
                    i2++;
                } else {
                    String strSubstring = str.substring(i2, i);
                    LogUtil.d("Duration", "getDurationMillis(): subString = " + strSubstring);
                    switch (i3) {
                        case 1:
                            jIntValue += ((long) Long.valueOf(strSubstring).intValue()) * 10080 * 60000;
                            break;
                        case 2:
                            jIntValue += ((long) Long.valueOf(strSubstring).intValue()) * 1440 * 60000;
                            break;
                        case 4:
                            jIntValue += ((long) Long.valueOf(strSubstring).intValue()) * 60 * 60000;
                            break;
                        case 5:
                            jIntValue += ((long) Long.valueOf(strSubstring).intValue()) * 60000;
                            break;
                        case 6:
                            jIntValue += ((long) Long.valueOf(strSubstring).intValue()) * 1000;
                            break;
                    }
                    i2 = i + 1;
                }
            }
        }
        if (str.contains("-")) {
            jIntValue *= -1;
        }
        LogUtil.d("Duration", "getDurationMillis(): duration millis = " + jIntValue);
        return jIntValue;
    }
}
