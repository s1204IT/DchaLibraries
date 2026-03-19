package java.util.zip;

import java.nio.file.attribute.FileTime;
import java.util.Date;
import java.util.concurrent.TimeUnit;

class ZipUtils {
    private static final long WINDOWS_EPOCH_IN_MICROSECONDS = -11644473600000000L;

    ZipUtils() {
    }

    public static final FileTime winTimeToFileTime(long j) {
        return FileTime.from((j / 10) + WINDOWS_EPOCH_IN_MICROSECONDS, TimeUnit.MICROSECONDS);
    }

    public static final long fileTimeToWinTime(FileTime fileTime) {
        return (fileTime.to(TimeUnit.MICROSECONDS) - WINDOWS_EPOCH_IN_MICROSECONDS) * 10;
    }

    public static final FileTime unixTimeToFileTime(long j) {
        return FileTime.from(j, TimeUnit.SECONDS);
    }

    public static final long fileTimeToUnixTime(FileTime fileTime) {
        return fileTime.to(TimeUnit.SECONDS);
    }

    private static long dosToJavaTime(long j) {
        return new Date((int) (((j >> 25) & 127) + 80), (int) (((j >> 21) & 15) - 1), (int) ((j >> 16) & 31), (int) ((j >> 11) & 31), (int) ((j >> 5) & 63), (int) ((j << 1) & 62)).getTime();
    }

    public static long extendedDosToJavaTime(long j) {
        return dosToJavaTime(j) + (j >> 32);
    }

    private static long javaToDosTime(long j) {
        Date date = new Date(j);
        int year = date.getYear() + 1900;
        if (year < 1980) {
            return 2162688L;
        }
        return ((long) (((year - 1980) << 25) | ((date.getMonth() + 1) << 21) | (date.getDate() << 16) | (date.getHours() << 11) | (date.getMinutes() << 5) | (date.getSeconds() >> 1))) & 4294967295L;
    }

    public static long javaToExtendedDosTime(long j) {
        if (j < 0) {
            return 2162688L;
        }
        long jJavaToDosTime = javaToDosTime(j);
        if (jJavaToDosTime != 2162688) {
            return jJavaToDosTime + ((j % 2000) << 32);
        }
        return 2162688L;
    }

    public static final int get16(byte[] bArr, int i) {
        return (Byte.toUnsignedInt(bArr[i + 1]) << 8) | Byte.toUnsignedInt(bArr[i]);
    }

    public static final long get32(byte[] bArr, int i) {
        return ((((long) get16(bArr, i + 2)) << 16) | ((long) get16(bArr, i))) & 4294967295L;
    }

    public static final long get64(byte[] bArr, int i) {
        return (get32(bArr, i + 4) << 32) | get32(bArr, i);
    }
}
