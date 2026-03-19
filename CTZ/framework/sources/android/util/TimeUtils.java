package android.util;

import android.os.SystemClock;
import android.text.format.DateFormat;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import libcore.util.TimeZoneFinder;
import libcore.util.ZoneInfoDB;

public class TimeUtils {
    public static final int HUNDRED_DAY_FIELD_LEN = 19;
    public static final long NANOS_PER_MS = 1000000;
    private static final int SECONDS_PER_DAY = 86400;
    private static final int SECONDS_PER_HOUR = 3600;
    private static final int SECONDS_PER_MINUTE = 60;
    private static SimpleDateFormat sLoggingFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static final Object sFormatSync = new Object();
    private static char[] sFormatStr = new char[29];
    private static char[] sTmpFormatStr = new char[29];

    public static TimeZone getTimeZone(int i, boolean z, long j, String str) {
        android.icu.util.TimeZone icuTimeZone = getIcuTimeZone(i, z, j, str);
        if (icuTimeZone != null) {
            return TimeZone.getTimeZone(icuTimeZone.getID());
        }
        return null;
    }

    private static android.icu.util.TimeZone getIcuTimeZone(int i, boolean z, long j, String str) {
        if (str == null) {
            return null;
        }
        return TimeZoneFinder.getInstance().lookupTimeZoneByCountryAndOffset(str, i, z, j, android.icu.util.TimeZone.getDefault());
    }

    public static String getTimeZoneDatabaseVersion() {
        return ZoneInfoDB.getInstance().getVersion();
    }

    private static int accumField(int i, int i2, boolean z, int i3) {
        int i4 = 0;
        if (i > 999) {
            while (i != 0) {
                i4++;
                i /= 10;
            }
            return i4 + i2;
        }
        if (i > 99 || (z && i3 >= 3)) {
            return 3 + i2;
        }
        if (i > 9 || (z && i3 >= 2)) {
            return 2 + i2;
        }
        if (!z && i <= 0) {
            return 0;
        }
        return 1 + i2;
    }

    private static int printFieldLocked(char[] cArr, int i, char c, int i2, boolean z, int i3) {
        int i4;
        if (z || i > 0) {
            if (i > 999) {
                int i5 = 0;
                while (i != 0 && i5 < sTmpFormatStr.length) {
                    sTmpFormatStr[i5] = (char) ((i % 10) + 48);
                    i5++;
                    i /= 10;
                }
                for (int i6 = i5 - 1; i6 >= 0; i6--) {
                    cArr[i2] = sTmpFormatStr[i6];
                    i2++;
                }
            } else {
                if ((z && i3 >= 3) || i > 99) {
                    int i7 = i / 100;
                    cArr[i2] = (char) (i7 + 48);
                    i4 = i2 + 1;
                    i -= i7 * 100;
                } else {
                    i4 = i2;
                }
                if ((z && i3 >= 2) || i > 9 || i2 != i4) {
                    int i8 = i / 10;
                    cArr[i4] = (char) (i8 + 48);
                    i4++;
                    i -= i8 * 10;
                }
                cArr[i4] = (char) (i + 48);
                i2 = i4 + 1;
            }
            cArr[i2] = c;
            return i2 + 1;
        }
        return i2;
    }

    private static int formatDurationLocked(long j, int i) {
        char c;
        int i2;
        int i3;
        int i4;
        int i5;
        int i6;
        long j2 = j;
        if (sFormatStr.length < i) {
            sFormatStr = new char[i];
        }
        char[] cArr = sFormatStr;
        int i7 = 0;
        if (j2 == 0) {
            int i8 = i - 1;
            while (i7 < i8) {
                cArr[i7] = ' ';
                i7++;
            }
            cArr[i7] = '0';
            return i7 + 1;
        }
        if (j2 > 0) {
            c = '+';
        } else {
            c = '-';
            j2 = -j2;
        }
        int i9 = (int) (j2 % 1000);
        int iFloor = (int) Math.floor(j2 / 1000);
        if (iFloor >= SECONDS_PER_DAY) {
            i2 = iFloor / SECONDS_PER_DAY;
            iFloor -= SECONDS_PER_DAY * i2;
        } else {
            i2 = 0;
        }
        if (iFloor >= 3600) {
            i3 = iFloor / 3600;
            iFloor -= i3 * 3600;
        } else {
            i3 = 0;
        }
        if (iFloor >= 60) {
            int i10 = iFloor / 60;
            i4 = iFloor - (i10 * 60);
            i5 = i10;
        } else {
            i4 = iFloor;
            i5 = 0;
        }
        if (i != 0) {
            int iAccumField = accumField(i2, 1, false, 0);
            int iAccumField2 = iAccumField + accumField(i3, 1, iAccumField > 0, 2);
            int iAccumField3 = iAccumField2 + accumField(i5, 1, iAccumField2 > 0, 2);
            int iAccumField4 = iAccumField3 + accumField(i4, 1, iAccumField3 > 0, 2);
            i6 = 0;
            for (int iAccumField5 = iAccumField4 + accumField(i9, 2, true, iAccumField4 > 0 ? 3 : 0) + 1; iAccumField5 < i; iAccumField5++) {
                cArr[i6] = ' ';
                i6++;
            }
        } else {
            i6 = 0;
        }
        cArr[i6] = c;
        int i11 = i6 + 1;
        boolean z = i != 0;
        int iPrintFieldLocked = printFieldLocked(cArr, i2, DateFormat.DATE, i11, false, 0);
        int iPrintFieldLocked2 = printFieldLocked(cArr, i3, DateFormat.HOUR, iPrintFieldLocked, iPrintFieldLocked != i11, z ? 2 : 0);
        int iPrintFieldLocked3 = printFieldLocked(cArr, i5, DateFormat.MINUTE, iPrintFieldLocked2, iPrintFieldLocked2 != i11, z ? 2 : 0);
        int iPrintFieldLocked4 = printFieldLocked(cArr, i4, 's', iPrintFieldLocked3, iPrintFieldLocked3 != i11, z ? 2 : 0);
        int iPrintFieldLocked5 = printFieldLocked(cArr, i9, DateFormat.MINUTE, iPrintFieldLocked4, true, (!z || iPrintFieldLocked4 == i11) ? 0 : 3);
        cArr[iPrintFieldLocked5] = 's';
        return iPrintFieldLocked5 + 1;
    }

    public static void formatDuration(long j, StringBuilder sb) {
        synchronized (sFormatSync) {
            sb.append(sFormatStr, 0, formatDurationLocked(j, 0));
        }
    }

    public static void formatDuration(long j, StringBuilder sb, int i) {
        synchronized (sFormatSync) {
            sb.append(sFormatStr, 0, formatDurationLocked(j, i));
        }
    }

    public static void formatDuration(long j, PrintWriter printWriter, int i) {
        synchronized (sFormatSync) {
            printWriter.print(new String(sFormatStr, 0, formatDurationLocked(j, i)));
        }
    }

    public static String formatDuration(long j) {
        String str;
        synchronized (sFormatSync) {
            str = new String(sFormatStr, 0, formatDurationLocked(j, 0));
        }
        return str;
    }

    public static void formatDuration(long j, PrintWriter printWriter) {
        formatDuration(j, printWriter, 0);
    }

    public static void formatDuration(long j, long j2, PrintWriter printWriter) {
        if (j == 0) {
            printWriter.print("--");
        } else {
            formatDuration(j - j2, printWriter, 0);
        }
    }

    public static String formatUptime(long j) {
        long jUptimeMillis = j - SystemClock.uptimeMillis();
        if (jUptimeMillis > 0) {
            return j + " (in " + jUptimeMillis + " ms)";
        }
        if (jUptimeMillis < 0) {
            return j + " (" + (-jUptimeMillis) + " ms ago)";
        }
        return j + " (now)";
    }

    public static String logTimeOfDay(long j) {
        Calendar calendar = Calendar.getInstance();
        if (j >= 0) {
            calendar.setTimeInMillis(j);
            return String.format("%tm-%td %tH:%tM:%tS.%tL", calendar, calendar, calendar, calendar, calendar, calendar);
        }
        return Long.toString(j);
    }

    public static String formatForLogging(long j) {
        if (j <= 0) {
            return "unknown";
        }
        return sLoggingFormat.format(new Date(j));
    }
}
