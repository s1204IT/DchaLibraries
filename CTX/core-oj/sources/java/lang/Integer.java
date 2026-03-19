package java.lang;

import java.time.Year;
import java.util.Locale;
import sun.misc.VM;
import sun.util.locale.LanguageTag;

public final class Integer extends Number implements Comparable<Integer> {
    public static final int BYTES = 4;
    public static final int MIN_VALUE = Integer.MIN_VALUE;
    public static final int SIZE = 32;
    private static final long serialVersionUID = 1360826667806852920L;
    private final int value;
    public static final Class<Integer> TYPE = Class.getPrimitiveClass("int");
    static final char[] digits = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', Locale.PRIVATE_USE_EXTENSION, 'y', 'z'};
    private static final String[] SMALL_NEG_VALUES = new String[100];
    private static final String[] SMALL_NONNEG_VALUES = new String[100];
    static final char[] DigitTens = {'0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '1', '1', '1', '1', '1', '1', '1', '1', '1', '1', '2', '2', '2', '2', '2', '2', '2', '2', '2', '2', '3', '3', '3', '3', '3', '3', '3', '3', '3', '3', '4', '4', '4', '4', '4', '4', '4', '4', '4', '4', '5', '5', '5', '5', '5', '5', '5', '5', '5', '5', '6', '6', '6', '6', '6', '6', '6', '6', '6', '6', '7', '7', '7', '7', '7', '7', '7', '7', '7', '7', '8', '8', '8', '8', '8', '8', '8', '8', '8', '8', '9', '9', '9', '9', '9', '9', '9', '9', '9', '9'};
    static final char[] DigitOnes = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9'};
    public static final int MAX_VALUE = Integer.MAX_VALUE;
    static final int[] sizeTable = {9, 99, 999, 9999, 99999, 999999, 9999999, 99999999, Year.MAX_VALUE, MAX_VALUE};

    public static String toString(int i, int i2) {
        if (i2 < 2 || i2 > 36) {
            i2 = 10;
        }
        if (i2 == 10) {
            return toString(i);
        }
        char[] cArr = new char[33];
        boolean z = i < 0;
        int i3 = 32;
        if (!z) {
            i = -i;
        }
        while (i <= (-i2)) {
            cArr[i3] = digits[-(i % i2)];
            i /= i2;
            i3--;
        }
        cArr[i3] = digits[-i];
        if (z) {
            i3--;
            cArr[i3] = '-';
        }
        return new String(cArr, i3, 33 - i3);
    }

    public static String toUnsignedString(int i, int i2) {
        return Long.toUnsignedString(toUnsignedLong(i), i2);
    }

    public static String toHexString(int i) {
        return toUnsignedString0(i, 4);
    }

    public static String toOctalString(int i) {
        return toUnsignedString0(i, 3);
    }

    public static String toBinaryString(int i) {
        return toUnsignedString0(i, 1);
    }

    private static String toUnsignedString0(int i, int i2) {
        int iMax = Math.max(((32 - numberOfLeadingZeros(i)) + (i2 - 1)) / i2, 1);
        char[] cArr = new char[iMax];
        formatUnsignedInt(i, i2, cArr, 0, iMax);
        return new String(cArr);
    }

    static int formatUnsignedInt(int i, int i2, char[] cArr, int i3, int i4) {
        int i5 = (1 << i2) - 1;
        do {
            i4--;
            cArr[i3 + i4] = digits[i & i5];
            i >>>= i2;
            if (i == 0) {
                break;
            }
        } while (i4 > 0);
        return i4;
    }

    public static String toString(int i) {
        if (i == Integer.MIN_VALUE) {
            return "-2147483648";
        }
        boolean z = i < 0;
        if (!(!z ? i >= 100 : i <= -100)) {
            int iStringSize = z ? stringSize(-i) + 1 : stringSize(i);
            char[] cArr = new char[iStringSize];
            getChars(i, iStringSize, cArr);
            return new String(cArr);
        }
        String[] strArr = z ? SMALL_NEG_VALUES : SMALL_NONNEG_VALUES;
        if (z) {
            i = -i;
            if (strArr[i] == null) {
                strArr[i] = i < 10 ? new String(new char[]{'-', DigitOnes[i]}) : new String(new char[]{'-', DigitTens[i], DigitOnes[i]});
            }
        } else if (strArr[i] == null) {
            strArr[i] = i < 10 ? new String(new char[]{DigitOnes[i]}) : new String(new char[]{DigitTens[i], DigitOnes[i]});
        }
        return strArr[i];
    }

    public static String toUnsignedString(int i) {
        return Long.toString(toUnsignedLong(i));
    }

    static void getChars(int i, int i2, char[] cArr) {
        char c;
        if (i < 0) {
            c = '-';
            i = -i;
        } else {
            c = 0;
        }
        while (i >= 65536) {
            int i3 = i / 100;
            int i4 = i - (((i3 << 6) + (i3 << 5)) + (i3 << 2));
            int i5 = i2 - 1;
            cArr[i5] = DigitOnes[i4];
            i2 = i5 - 1;
            cArr[i2] = DigitTens[i4];
            i = i3;
        }
        while (true) {
            int i6 = (52429 * i) >>> 19;
            i2--;
            cArr[i2] = digits[i - ((i6 << 3) + (i6 << 1))];
            if (i6 == 0) {
                break;
            } else {
                i = i6;
            }
        }
        if (c != 0) {
            cArr[i2 - 1] = c;
        }
    }

    static int stringSize(int i) {
        int i2 = 0;
        while (i > sizeTable[i2]) {
            i2++;
        }
        return i2 + 1;
    }

    public static int parseInt(String str, int i) throws NumberFormatException {
        boolean z;
        if (str == null) {
            throw new NumberFormatException("s == null");
        }
        if (i < 2) {
            throw new NumberFormatException("radix " + i + " less than Character.MIN_RADIX");
        }
        if (i > 36) {
            throw new NumberFormatException("radix " + i + " greater than Character.MAX_RADIX");
        }
        int length = str.length();
        int i2 = -2147483647;
        if (length > 0) {
            int i3 = 0;
            char cCharAt = str.charAt(0);
            int i4 = 1;
            if (cCharAt < '0') {
                if (cCharAt == '-') {
                    i2 = Integer.MIN_VALUE;
                    z = true;
                } else {
                    if (cCharAt != '+') {
                        throw NumberFormatException.forInputString(str);
                    }
                    z = false;
                }
                if (length == 1) {
                    throw NumberFormatException.forInputString(str);
                }
            } else {
                z = false;
                i4 = 0;
            }
            int i5 = i2 / i;
            while (i4 < length) {
                int i6 = i4 + 1;
                int iDigit = Character.digit(str.charAt(i4), i);
                if (iDigit < 0) {
                    throw NumberFormatException.forInputString(str);
                }
                if (i3 < i5) {
                    throw NumberFormatException.forInputString(str);
                }
                int i7 = i3 * i;
                if (i7 < i2 + iDigit) {
                    throw NumberFormatException.forInputString(str);
                }
                i3 = i7 - iDigit;
                i4 = i6;
            }
            return z ? i3 : -i3;
        }
        throw NumberFormatException.forInputString(str);
    }

    public static int parseInt(String str) throws NumberFormatException {
        return parseInt(str, 10);
    }

    public static int parseUnsignedInt(String str, int i) throws NumberFormatException {
        if (str == null) {
            throw new NumberFormatException("null");
        }
        int length = str.length();
        if (length > 0) {
            if (str.charAt(0) == '-') {
                throw new NumberFormatException(String.format("Illegal leading minus sign on unsigned string %s.", str));
            }
            if (length <= 5 || (i == 10 && length <= 9)) {
                return parseInt(str, i);
            }
            long j = Long.parseLong(str, i);
            if (((-4294967296L) & j) == 0) {
                return (int) j;
            }
            throw new NumberFormatException(String.format("String value %s exceeds range of unsigned int.", str));
        }
        throw NumberFormatException.forInputString(str);
    }

    public static int parseUnsignedInt(String str) throws NumberFormatException {
        return parseUnsignedInt(str, 10);
    }

    public static Integer valueOf(String str, int i) throws NumberFormatException {
        return valueOf(parseInt(str, i));
    }

    public static Integer valueOf(String str) throws NumberFormatException {
        return valueOf(parseInt(str, 10));
    }

    private static class IntegerCache {
        static final boolean $assertionsDisabled = false;
        static final Integer[] cache;
        static final int high;
        static final int low = -128;

        static {
            int iMin;
            String savedProperty = VM.getSavedProperty("java.lang.Integer.IntegerCache.high");
            if (savedProperty != null) {
                try {
                    iMin = Math.min(Math.max(Integer.parseInt(savedProperty), 127), 2147483518);
                } catch (NumberFormatException e) {
                    iMin = 127;
                }
            } else {
                iMin = 127;
            }
            high = iMin;
            int i = high;
            int i2 = low;
            cache = new Integer[(i - low) + 1];
            int i3 = 0;
            while (i3 < cache.length) {
                cache[i3] = new Integer(i2);
                i3++;
                i2++;
            }
        }

        private IntegerCache() {
        }
    }

    public static Integer valueOf(int i) {
        if (i >= -128 && i <= IntegerCache.high) {
            return IntegerCache.cache[i + 128];
        }
        return new Integer(i);
    }

    public Integer(int i) {
        this.value = i;
    }

    public Integer(String str) throws NumberFormatException {
        this.value = parseInt(str, 10);
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
        return this.value;
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

    public static int hashCode(int i) {
        return i;
    }

    public boolean equals(Object obj) {
        return (obj instanceof Integer) && this.value == ((Integer) obj).intValue();
    }

    public static Integer getInteger(String str) {
        return getInteger(str, (Integer) null);
    }

    public static Integer getInteger(String str, int i) {
        Integer integer = getInteger(str, (Integer) null);
        return integer == null ? valueOf(i) : integer;
    }

    public static Integer getInteger(String str, Integer num) {
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
        return num;
    }

    public static Integer decode(String str) throws NumberFormatException {
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
                Integer numValueOf = valueOf(str.substring(i3), i4);
                return i != 0 ? valueOf(-numValueOf.intValue()) : numValueOf;
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

    @Override
    public int compareTo(Integer num) {
        return compare(this.value, num.value);
    }

    public static int compare(int i, int i2) {
        if (i < i2) {
            return -1;
        }
        return i == i2 ? 0 : 1;
    }

    public static int compareUnsigned(int i, int i2) {
        return compare(i - Integer.MIN_VALUE, i2 - Integer.MIN_VALUE);
    }

    public static long toUnsignedLong(int i) {
        return ((long) i) & 4294967295L;
    }

    public static int divideUnsigned(int i, int i2) {
        return (int) (toUnsignedLong(i) / toUnsignedLong(i2));
    }

    public static int remainderUnsigned(int i, int i2) {
        return (int) (toUnsignedLong(i) % toUnsignedLong(i2));
    }

    public static int highestOneBit(int i) {
        int i2 = i | (i >> 1);
        int i3 = i2 | (i2 >> 2);
        int i4 = i3 | (i3 >> 4);
        int i5 = i4 | (i4 >> 8);
        int i6 = i5 | (i5 >> 16);
        return i6 - (i6 >>> 1);
    }

    public static int lowestOneBit(int i) {
        return i & (-i);
    }

    public static int numberOfLeadingZeros(int i) {
        if (i == 0) {
            return 32;
        }
        int i2 = 1;
        if ((i >>> 16) == 0) {
            i2 = 17;
            i <<= 16;
        }
        if ((i >>> 24) == 0) {
            i2 += 8;
            i <<= 8;
        }
        if ((i >>> 28) == 0) {
            i2 += 4;
            i <<= 4;
        }
        if ((i >>> 30) == 0) {
            i2 += 2;
            i <<= 2;
        }
        return i2 - (i >>> 31);
    }

    public static int numberOfTrailingZeros(int i) {
        int i2;
        if (i == 0) {
            return 32;
        }
        int i3 = i << 16;
        if (i3 != 0) {
            i2 = 15;
            i = i3;
        } else {
            i2 = 31;
        }
        int i4 = i << 8;
        if (i4 != 0) {
            i2 -= 8;
            i = i4;
        }
        int i5 = i << 4;
        if (i5 != 0) {
            i2 -= 4;
            i = i5;
        }
        int i6 = i << 2;
        if (i6 != 0) {
            i2 -= 2;
            i = i6;
        }
        return i2 - ((i << 1) >>> 31);
    }

    public static int bitCount(int i) {
        int i2 = i - ((i >>> 1) & 1431655765);
        int i3 = (i2 & 858993459) + ((i2 >>> 2) & 858993459);
        int i4 = 252645135 & (i3 + (i3 >>> 4));
        int i5 = i4 + (i4 >>> 8);
        return (i5 + (i5 >>> 16)) & 63;
    }

    public static int rotateLeft(int i, int i2) {
        return (i >>> (-i2)) | (i << i2);
    }

    public static int rotateRight(int i, int i2) {
        return (i << (-i2)) | (i >>> i2);
    }

    public static int reverse(int i) {
        int i2 = ((i >>> 1) & 1431655765) | ((i & 1431655765) << 1);
        int i3 = ((i2 >>> 2) & 858993459) | ((i2 & 858993459) << 2);
        int i4 = ((i3 >>> 4) & 252645135) | ((i3 & 252645135) << 4);
        return (i4 >>> 24) | (i4 << 24) | ((i4 & 65280) << 8) | (65280 & (i4 >>> 8));
    }

    public static int signum(int i) {
        return ((-i) >>> 31) | (i >> 31);
    }

    public static int reverseBytes(int i) {
        return (i << 24) | (i >>> 24) | ((i >> 8) & 65280) | ((i << 8) & 16711680);
    }

    public static int sum(int i, int i2) {
        return i + i2;
    }

    public static int max(int i, int i2) {
        return Math.max(i, i2);
    }

    public static int min(int i, int i2) {
        return Math.min(i, i2);
    }
}
