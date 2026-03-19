package sun.util.calendar;

import java.net.HttpURLConnection;

public class CalendarUtils {
    public static final boolean isGregorianLeapYear(int i) {
        return i % 4 == 0 && (i % 100 != 0 || i % HttpURLConnection.HTTP_BAD_REQUEST == 0);
    }

    public static final boolean isJulianLeapYear(int i) {
        return i % 4 == 0;
    }

    public static final long floorDivide(long j, long j2) {
        return j >= 0 ? j / j2 : ((j + 1) / j2) - 1;
    }

    public static final int floorDivide(int i, int i2) {
        return i >= 0 ? i / i2 : ((i + 1) / i2) - 1;
    }

    public static final int floorDivide(int i, int i2, int[] iArr) {
        if (i >= 0) {
            iArr[0] = i % i2;
            return i / i2;
        }
        int i3 = ((i + 1) / i2) - 1;
        iArr[0] = i - (i2 * i3);
        return i3;
    }

    public static final int floorDivide(long j, int i, int[] iArr) {
        if (j >= 0) {
            long j2 = i;
            iArr[0] = (int) (j % j2);
            return (int) (j / j2);
        }
        int i2 = (int) (((j + 1) / ((long) i)) - 1);
        iArr[0] = (int) (j - ((long) (i * i2)));
        return i2;
    }

    public static final long mod(long j, long j2) {
        return j - (j2 * floorDivide(j, j2));
    }

    public static final int mod(int i, int i2) {
        return i - (i2 * floorDivide(i, i2));
    }

    public static final int amod(int i, int i2) {
        int iMod = mod(i, i2);
        return iMod == 0 ? i2 : iMod;
    }

    public static final long amod(long j, long j2) {
        long jMod = mod(j, j2);
        return jMod == 0 ? j2 : jMod;
    }

    public static final StringBuilder sprintf0d(StringBuilder sb, int i, int i2) {
        long j = i;
        if (j < 0) {
            sb.append('-');
            j = -j;
            i2--;
        }
        int i3 = 10;
        for (int i4 = 2; i4 < i2; i4++) {
            i3 *= 10;
        }
        for (int i5 = 1; i5 < i2 && j < i3; i5++) {
            sb.append('0');
            i3 /= 10;
        }
        sb.append(j);
        return sb;
    }

    public static final StringBuffer sprintf0d(StringBuffer stringBuffer, int i, int i2) {
        long j = i;
        if (j < 0) {
            stringBuffer.append('-');
            j = -j;
            i2--;
        }
        int i3 = 10;
        for (int i4 = 2; i4 < i2; i4++) {
            i3 *= 10;
        }
        for (int i5 = 1; i5 < i2 && j < i3; i5++) {
            stringBuffer.append('0');
            i3 /= 10;
        }
        stringBuffer.append(j);
        return stringBuffer;
    }
}
