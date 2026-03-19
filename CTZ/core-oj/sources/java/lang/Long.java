package java.lang;

import java.math.BigInteger;
import sun.util.locale.LanguageTag;

public final class Long extends Number implements Comparable<Long> {
    public static final int BYTES = 8;
    public static final long MAX_VALUE = Long.MAX_VALUE;
    public static final long MIN_VALUE = Long.MIN_VALUE;
    public static final int SIZE = 64;
    public static final Class<Long> TYPE = Class.getPrimitiveClass("long");
    private static final long serialVersionUID = 4290774380558885855L;
    private final long value;

    public static String toString(long j, int i) {
        if (i < 2 || i > 36) {
            i = 10;
        }
        if (i == 10) {
            return toString(j);
        }
        char[] cArr = new char[65];
        int i2 = 64;
        boolean z = j < 0;
        if (!z) {
            j = -j;
        }
        while (j <= (-i)) {
            long j2 = i;
            cArr[i2] = Integer.digits[(int) (-(j % j2))];
            j /= j2;
            i2--;
        }
        cArr[i2] = Integer.digits[(int) (-j)];
        if (z) {
            i2--;
            cArr[i2] = '-';
        }
        return new String(cArr, i2, 65 - i2);
    }

    public static String toUnsignedString(long j, int i) {
        if (j >= 0) {
            return toString(j, i);
        }
        if (i == 2) {
            return toBinaryString(j);
        }
        if (i == 4) {
            return toUnsignedString0(j, 2);
        }
        if (i == 8) {
            return toOctalString(j);
        }
        if (i != 10) {
            if (i == 16) {
                return toHexString(j);
            }
            if (i == 32) {
                return toUnsignedString0(j, 5);
            }
            return toUnsignedBigInteger(j).toString(i);
        }
        long j2 = (j >>> 1) / 5;
        return toString(j2) + (j - (10 * j2));
    }

    private static BigInteger toUnsignedBigInteger(long j) {
        if (j >= 0) {
            return BigInteger.valueOf(j);
        }
        return BigInteger.valueOf(Integer.toUnsignedLong((int) (j >>> 32))).shiftLeft(32).add(BigInteger.valueOf(Integer.toUnsignedLong((int) j)));
    }

    public static String toHexString(long j) {
        return toUnsignedString0(j, 4);
    }

    public static String toOctalString(long j) {
        return toUnsignedString0(j, 3);
    }

    public static String toBinaryString(long j) {
        return toUnsignedString0(j, 1);
    }

    static String toUnsignedString0(long j, int i) {
        int iMax = Math.max(((64 - numberOfLeadingZeros(j)) + (i - 1)) / i, 1);
        char[] cArr = new char[iMax];
        formatUnsignedLong(j, i, cArr, 0, iMax);
        return new String(cArr);
    }

    static int formatUnsignedLong(long j, int i, char[] cArr, int i2, int i3) {
        int i4 = (1 << i) - 1;
        do {
            i3--;
            cArr[i2 + i3] = Integer.digits[((int) j) & i4];
            j >>>= i;
            if (j == 0) {
                break;
            }
        } while (i3 > 0);
        return i3;
    }

    public static String toString(long j) {
        if (j == Long.MIN_VALUE) {
            return "-9223372036854775808";
        }
        int iStringSize = j < 0 ? stringSize(-j) + 1 : stringSize(j);
        char[] cArr = new char[iStringSize];
        getChars(j, iStringSize, cArr);
        return new String(cArr);
    }

    public static String toUnsignedString(long j) {
        return toUnsignedString(j, 10);
    }

    static void getChars(long j, int i, char[] cArr) {
        char c;
        if (j < 0) {
            c = '-';
            j = -j;
        } else {
            c = 0;
        }
        while (j > 2147483647L) {
            long j2 = j / 100;
            int i2 = (int) (j - (((j2 << 6) + (j2 << 5)) + (j2 << 2)));
            int i3 = i - 1;
            cArr[i3] = Integer.DigitOnes[i2];
            i = i3 - 1;
            cArr[i] = Integer.DigitTens[i2];
            j = j2;
        }
        int i4 = (int) j;
        while (i4 >= 65536) {
            int i5 = i4 / 100;
            int i6 = i4 - (((i5 << 6) + (i5 << 5)) + (i5 << 2));
            int i7 = i - 1;
            cArr[i7] = Integer.DigitOnes[i6];
            i = i7 - 1;
            cArr[i] = Integer.DigitTens[i6];
            i4 = i5;
        }
        while (true) {
            int i8 = (52429 * i4) >>> 19;
            i--;
            cArr[i] = Integer.digits[i4 - ((i8 << 3) + (i8 << 1))];
            if (i8 == 0) {
                break;
            } else {
                i4 = i8;
            }
        }
        if (c != 0) {
            cArr[i - 1] = c;
        }
    }

    static int stringSize(long j) {
        long j2 = 10;
        for (int i = 1; i < 19; i++) {
            if (j < j2) {
                return i;
            }
            j2 *= 10;
        }
        return 19;
    }

    public static long parseLong(String str, int i) throws NumberFormatException {
        if (str != null) {
            if (i >= 2) {
                if (i > 36) {
                    throw new NumberFormatException("radix " + i + " greater than Character.MAX_RADIX");
                }
                long j = 0;
                int length = str.length();
                long j2 = -9223372036854775807L;
                if (length > 0) {
                    boolean z = false;
                    char cCharAt = str.charAt(0);
                    int i2 = 1;
                    if (cCharAt < '0') {
                        if (cCharAt == '-') {
                            j2 = Long.MIN_VALUE;
                            z = true;
                        } else if (cCharAt != '+') {
                            throw NumberFormatException.forInputString(str);
                        }
                        if (length == 1) {
                            throw NumberFormatException.forInputString(str);
                        }
                    } else {
                        i2 = 0;
                    }
                    long j3 = i;
                    long j4 = j2 / j3;
                    while (i2 < length) {
                        int i3 = i2 + 1;
                        int iDigit = Character.digit(str.charAt(i2), i);
                        if (iDigit < 0) {
                            throw NumberFormatException.forInputString(str);
                        }
                        if (j < j4) {
                            throw NumberFormatException.forInputString(str);
                        }
                        long j5 = j * j3;
                        long j6 = iDigit;
                        if (j5 < j2 + j6) {
                            throw NumberFormatException.forInputString(str);
                        }
                        j = j5 - j6;
                        i2 = i3;
                    }
                    return z ? j : -j;
                }
                throw NumberFormatException.forInputString(str);
            }
            throw new NumberFormatException("radix " + i + " less than Character.MIN_RADIX");
        }
        throw new NumberFormatException("null");
    }

    public static long parseLong(String str) throws NumberFormatException {
        return parseLong(str, 10);
    }

    public static long parseUnsignedLong(String str, int i) throws NumberFormatException {
        if (str == null) {
            throw new NumberFormatException("null");
        }
        int length = str.length();
        if (length > 0) {
            if (str.charAt(0) == '-') {
                throw new NumberFormatException(String.format("Illegal leading minus sign on unsigned string %s.", str));
            }
            if (length <= 12 || (i == 10 && length <= 18)) {
                return parseLong(str, i);
            }
            int i2 = length - 1;
            long j = parseLong(str.substring(0, i2), i);
            int iDigit = Character.digit(str.charAt(i2), i);
            if (iDigit < 0) {
                throw new NumberFormatException("Bad digit at end of " + str);
            }
            long j2 = (((long) i) * j) + ((long) iDigit);
            if (compareUnsigned(j2, j) < 0) {
                throw new NumberFormatException(String.format("String value %s exceeds range of unsigned long.", str));
            }
            return j2;
        }
        throw NumberFormatException.forInputString(str);
    }

    public static long parseUnsignedLong(String str) throws NumberFormatException {
        return parseUnsignedLong(str, 10);
    }

    public static Long valueOf(String str, int i) throws NumberFormatException {
        return valueOf(parseLong(str, i));
    }

    public static Long valueOf(String str) throws NumberFormatException {
        return valueOf(parseLong(str, 10));
    }

    private static class LongCache {
        static final Long[] cache = new Long[256];

        private LongCache() {
        }

        static {
            for (int i = 0; i < cache.length; i++) {
                cache[i] = new Long(i - 128);
            }
        }
    }

    public static Long valueOf(long j) {
        if (j >= -128 && j <= 127) {
            return LongCache.cache[((int) j) + 128];
        }
        return new Long(j);
    }

    public static Long decode(String str) throws NumberFormatException {
        int i;
        String strSubstring;
        int i2;
        if (str.length() == 0) {
            throw new NumberFormatException("Zero length string");
        }
        int i3 = 0;
        char cCharAt = str.charAt(0);
        if (cCharAt != '-') {
            if (cCharAt == '+') {
                i = 0;
                i3 = 1;
            }
            int i4 = 16;
            if (!str.startsWith("0x", i3) || str.startsWith("0X", i3)) {
                i3 += 2;
            } else if (str.startsWith("#", i3)) {
                i3++;
            } else if (!str.startsWith("0", i3) || str.length() <= (i2 = 1 + i3)) {
                i4 = 10;
            } else {
                i4 = 8;
                i3 = i2;
            }
            if (!str.startsWith(LanguageTag.SEP, i3) || str.startsWith("+", i3)) {
                throw new NumberFormatException("Sign character in wrong position");
            }
            try {
                Long lValueOf = valueOf(str.substring(i3), i4);
                return i != 0 ? valueOf(-lValueOf.longValue()) : lValueOf;
            } catch (NumberFormatException e) {
                if (i != 0) {
                    strSubstring = LanguageTag.SEP + str.substring(i3);
                } else {
                    strSubstring = str.substring(i3);
                }
                return valueOf(strSubstring, i4);
            }
        }
        i3 = 1;
        i = i3;
        int i42 = 16;
        if (!str.startsWith("0x", i3)) {
            i3 += 2;
        }
        if (!str.startsWith(LanguageTag.SEP, i3)) {
        }
        throw new NumberFormatException("Sign character in wrong position");
    }

    public Long(long j) {
        this.value = j;
    }

    public Long(String str) throws NumberFormatException {
        this.value = parseLong(str, 10);
    }

    @Override
    public byte byteValue() {
        return (byte) this.value;
    }

    @Override
    public short shortValue() {
        return (short) this.value;
    }

    @Override
    public int intValue() {
        return (int) this.value;
    }

    @Override
    public long longValue() {
        return this.value;
    }

    @Override
    public float floatValue() {
        return this.value;
    }

    @Override
    public double doubleValue() {
        return this.value;
    }

    public String toString() {
        return toString(this.value);
    }

    public int hashCode() {
        return hashCode(this.value);
    }

    public static int hashCode(long j) {
        return (int) (j ^ (j >>> 32));
    }

    public boolean equals(Object obj) {
        return (obj instanceof Long) && this.value == ((Long) obj).longValue();
    }

    public static Long getLong(String str) {
        return getLong(str, (Long) null);
    }

    public static Long getLong(String str, long j) {
        Long l = getLong(str, (Long) null);
        return l == null ? valueOf(j) : l;
    }

    public static Long getLong(String str, Long l) {
        String property;
        try {
            property = System.getProperty(str);
        } catch (IllegalArgumentException | NullPointerException e) {
            property = null;
        }
        if (property != null) {
            try {
                return decode(property);
            } catch (NumberFormatException e2) {
            }
        }
        return l;
    }

    @Override
    public int compareTo(Long l) {
        return compare(this.value, l.value);
    }

    public static int compare(long j, long j2) {
        if (j < j2) {
            return -1;
        }
        return j == j2 ? 0 : 1;
    }

    public static int compareUnsigned(long j, long j2) {
        return compare(j - Long.MIN_VALUE, j2 - Long.MIN_VALUE);
    }

    public static long divideUnsigned(long j, long j2) {
        if (j2 < 0) {
            return compareUnsigned(j, j2) < 0 ? 0L : 1L;
        }
        if (j > 0) {
            return j / j2;
        }
        return toUnsignedBigInteger(j).divide(toUnsignedBigInteger(j2)).longValue();
    }

    public static long remainderUnsigned(long j, long j2) {
        if (j > 0 && j2 > 0) {
            return j % j2;
        }
        if (compareUnsigned(j, j2) < 0) {
            return j;
        }
        return toUnsignedBigInteger(j).remainder(toUnsignedBigInteger(j2)).longValue();
    }

    public static long highestOneBit(long j) {
        long j2 = j | (j >> 1);
        long j3 = j2 | (j2 >> 2);
        long j4 = j3 | (j3 >> 4);
        long j5 = j4 | (j4 >> 8);
        long j6 = j5 | (j5 >> 16);
        long j7 = j6 | (j6 >> 32);
        return j7 - (j7 >>> 1);
    }

    public static long lowestOneBit(long j) {
        return j & (-j);
    }

    public static int numberOfLeadingZeros(long j) {
        if (j == 0) {
            return 64;
        }
        int i = 1;
        int i2 = (int) (j >>> 32);
        if (i2 == 0) {
            i = 33;
            i2 = (int) j;
        }
        if ((i2 >>> 16) == 0) {
            i += 16;
            i2 <<= 16;
        }
        if ((i2 >>> 24) == 0) {
            i += 8;
            i2 <<= 8;
        }
        if ((i2 >>> 28) == 0) {
            i += 4;
            i2 <<= 4;
        }
        if ((i2 >>> 30) == 0) {
            i += 2;
            i2 <<= 2;
        }
        return i - (i2 >>> 31);
    }

    public static int numberOfTrailingZeros(long j) {
        if (j == 0) {
            return 64;
        }
        int i = 63;
        int i2 = (int) j;
        if (i2 == 0) {
            i2 = (int) (j >>> 32);
        } else {
            i = 31;
        }
        int i3 = i2 << 16;
        if (i3 != 0) {
            i -= 16;
        } else {
            i3 = i2;
        }
        int i4 = i3 << 8;
        if (i4 != 0) {
            i -= 8;
            i3 = i4;
        }
        int i5 = i3 << 4;
        if (i5 != 0) {
            i -= 4;
            i3 = i5;
        }
        int i6 = i3 << 2;
        if (i6 != 0) {
            i -= 2;
            i3 = i6;
        }
        return i - ((i3 << 1) >>> 31);
    }

    public static int bitCount(long j) {
        long j2 = j - ((j >>> 1) & 6148914691236517205L);
        long j3 = (j2 & 3689348814741910323L) + ((j2 >>> 2) & 3689348814741910323L);
        long j4 = 1085102592571150095L & (j3 + (j3 >>> 4));
        long j5 = j4 + (j4 >>> 8);
        long j6 = j5 + (j5 >>> 16);
        return ((int) (j6 + (j6 >>> 32))) & 127;
    }

    public static long rotateLeft(long j, int i) {
        return (j >>> (-i)) | (j << i);
    }

    public static long rotateRight(long j, int i) {
        return (j << (-i)) | (j >>> i);
    }

    public static long reverse(long j) {
        long j2 = ((j >>> 1) & 6148914691236517205L) | ((j & 6148914691236517205L) << 1);
        long j3 = ((j2 >>> 2) & 3689348814741910323L) | ((j2 & 3689348814741910323L) << 2);
        long j4 = ((j3 >>> 4) & 1085102592571150095L) | ((j3 & 1085102592571150095L) << 4);
        long j5 = ((j4 >>> 8) & 71777214294589695L) | ((j4 & 71777214294589695L) << 8);
        return (j5 >>> 48) | (j5 << 48) | ((j5 & 4294901760L) << 16) | (4294901760L & (j5 >>> 16));
    }

    public static int signum(long j) {
        return (int) (((-j) >>> 63) | (j >> 63));
    }

    public static long reverseBytes(long j) {
        long j2 = ((j >>> 8) & 71777214294589695L) | ((j & 71777214294589695L) << 8);
        return (j2 >>> 48) | (j2 << 48) | ((j2 & 4294901760L) << 16) | (4294901760L & (j2 >>> 16));
    }

    public static long sum(long j, long j2) {
        return j + j2;
    }

    public static long max(long j, long j2) {
        return Math.max(j, j2);
    }

    public static long min(long j, long j2) {
        return Math.min(j, j2);
    }
}
