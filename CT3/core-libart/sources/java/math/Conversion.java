package java.math;

import dalvik.system.VMDebug;

class Conversion {
    static final int[] digitFitInInt = {-1, -1, 31, 19, 15, 13, 11, 11, 10, 9, 9, 8, 8, 8, 8, 7, 7, 7, 7, 7, 7, 7, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 5};
    static final int[] bigRadices = {Integer.MIN_VALUE, 1162261467, VMDebug.KIND_THREAD_EXT_FREED_OBJECTS, 1220703125, 362797056, 1977326743, VMDebug.KIND_THREAD_EXT_FREED_OBJECTS, 387420489, 1000000000, 214358881, 429981696, 815730721, 1475789056, 170859375, VMDebug.KIND_THREAD_EXT_ALLOCATED_OBJECTS, 410338673, 612220032, 893871739, 1280000000, 1801088541, 113379904, 148035889, 191102976, 244140625, 308915776, 387420489, 481890304, 594823321, 729000000, 887503681, VMDebug.KIND_THREAD_EXT_FREED_OBJECTS, 1291467969, 1544804416, 1838265625, 60466176};

    private Conversion() {
    }

    static String bigInteger2String(BigInteger val, int radix) {
        val.prepareJavaRepresentation();
        int sign = val.sign;
        int numberLength = val.numberLength;
        int[] digits = val.digits;
        if (sign == 0) {
            return AndroidHardcodedSystemProperties.JAVA_VERSION;
        }
        if (numberLength == 1) {
            int highDigit = digits[numberLength - 1];
            long v = ((long) highDigit) & 4294967295L;
            if (sign < 0) {
                v = -v;
            }
            return Long.toString(v, radix);
        }
        if (radix == 10 || radix < 2 || radix > 36) {
            return val.toString();
        }
        double bitsForRadixDigit = Math.log(radix) / Math.log(2.0d);
        int resLengthInChars = ((int) (((double) (sign < 0 ? 1 : 0)) + (((double) val.abs().bitLength()) / bitsForRadixDigit))) + 1;
        char[] result = new char[resLengthInChars];
        int currentChar = resLengthInChars;
        if (radix != 16) {
            int[] temp = new int[numberLength];
            System.arraycopy(digits, 0, temp, 0, numberLength);
            int tempLen = numberLength;
            int charsPerInt = digitFitInInt[radix];
            int bigRadix = bigRadices[radix - 2];
            while (true) {
                int resDigit = Division.divideArrayByInt(temp, temp, tempLen, bigRadix);
                int previous = currentChar;
                do {
                    currentChar--;
                    result[currentChar] = Character.forDigit(resDigit % radix, radix);
                    resDigit /= radix;
                    if (resDigit == 0) {
                        break;
                    }
                } while (currentChar != 0);
                int delta = (charsPerInt - previous) + currentChar;
                for (int i = 0; i < delta && currentChar > 0; i++) {
                    currentChar--;
                    result[currentChar] = '0';
                }
                int i2 = tempLen - 1;
                while (i2 > 0 && temp[i2] == 0) {
                    i2--;
                }
                tempLen = i2 + 1;
                if (tempLen == 1 && temp[0] == 0) {
                    break;
                }
            }
        } else {
            for (int i3 = 0; i3 < numberLength; i3++) {
                for (int j = 0; j < 8 && currentChar > 0; j++) {
                    int resDigit2 = (digits[i3] >> (j << 2)) & 15;
                    currentChar--;
                    result[currentChar] = Character.forDigit(resDigit2, 16);
                }
            }
        }
        while (result[currentChar] == '0') {
            currentChar++;
        }
        if (sign == -1) {
            currentChar--;
            result[currentChar] = '-';
        }
        return new String(result, currentChar, resLengthInChars - currentChar);
    }

    static String toDecimalScaledString(BigInteger val, int scale) {
        val.prepareJavaRepresentation();
        int sign = val.sign;
        int numberLength = val.numberLength;
        int[] digits = val.digits;
        if (sign == 0) {
            switch (scale) {
                case 0:
                    return AndroidHardcodedSystemProperties.JAVA_VERSION;
                case 1:
                    return "0.0";
                case 2:
                    return "0.00";
                case 3:
                    return "0.000";
                case 4:
                    return "0.0000";
                case 5:
                    return "0.00000";
                case 6:
                    return "0.000000";
                default:
                    StringBuilder result1 = new StringBuilder();
                    if (scale < 0) {
                        result1.append("0E+");
                    } else {
                        result1.append("0E");
                    }
                    result1.append(-scale);
                    return result1.toString();
            }
        }
        int resLengthInChars = (numberLength * 10) + 1 + 7;
        char[] result = new char[resLengthInChars + 1];
        int currentChar = resLengthInChars;
        if (numberLength == 1) {
            int highDigit = digits[0];
            if (highDigit < 0) {
                long v = ((long) highDigit) & 4294967295L;
                do {
                    long prev = v;
                    v /= 10;
                    currentChar--;
                    result[currentChar] = (char) (((int) (prev - (10 * v))) + 48);
                } while (v != 0);
            } else {
                int v2 = highDigit;
                do {
                    int prev2 = v2;
                    v2 /= 10;
                    currentChar--;
                    result[currentChar] = (char) ((prev2 - (v2 * 10)) + 48);
                } while (v2 != 0);
            }
        } else {
            int[] temp = new int[numberLength];
            int tempLen = numberLength;
            System.arraycopy(digits, 0, temp, 0, numberLength);
            while (true) {
                long result11 = 0;
                for (int i1 = tempLen - 1; i1 >= 0; i1--) {
                    long temp1 = (result11 << 32) + (((long) temp[i1]) & 4294967295L);
                    long res = divideLongByBillion(temp1);
                    temp[i1] = (int) res;
                    result11 = (int) (res >> 32);
                }
                int resDigit = (int) result11;
                int previous = currentChar;
                do {
                    currentChar--;
                    result[currentChar] = (char) ((resDigit % 10) + 48);
                    resDigit /= 10;
                    if (resDigit == 0) {
                        break;
                    }
                } while (currentChar != 0);
                int delta = (9 - previous) + currentChar;
                for (int i = 0; i < delta && currentChar > 0; i++) {
                    currentChar--;
                    result[currentChar] = '0';
                }
                int j = tempLen - 1;
                while (temp[j] == 0) {
                    if (j == 0) {
                        break;
                    }
                    j--;
                }
                tempLen = j + 1;
            }
            while (result[currentChar] == '0') {
                currentChar++;
            }
        }
        boolean negNumber = sign < 0;
        int exponent = ((resLengthInChars - currentChar) - scale) - 1;
        if (scale == 0) {
            if (negNumber) {
                currentChar--;
                result[currentChar] = '-';
            }
            return new String(result, currentChar, resLengthInChars - currentChar);
        }
        if (scale > 0 && exponent >= -6) {
            if (exponent >= 0) {
                int insertPoint = currentChar + exponent;
                for (int j2 = resLengthInChars - 1; j2 >= insertPoint; j2--) {
                    result[j2 + 1] = result[j2];
                }
                result[insertPoint + 1] = '.';
                if (negNumber) {
                    currentChar--;
                    result[currentChar] = '-';
                }
                return new String(result, currentChar, (resLengthInChars - currentChar) + 1);
            }
            for (int j3 = 2; j3 < (-exponent) + 1; j3++) {
                currentChar--;
                result[currentChar] = '0';
            }
            int currentChar2 = currentChar - 1;
            result[currentChar2] = '.';
            int currentChar3 = currentChar2 - 1;
            result[currentChar3] = '0';
            if (negNumber) {
                currentChar3--;
                result[currentChar3] = '-';
            }
            return new String(result, currentChar3, resLengthInChars - currentChar3);
        }
        int startPoint = currentChar + 1;
        StringBuilder result12 = new StringBuilder((resLengthInChars + 16) - startPoint);
        if (negNumber) {
            result12.append('-');
        }
        if (resLengthInChars - startPoint >= 1) {
            result12.append(result[currentChar]);
            result12.append('.');
            result12.append(result, currentChar + 1, (resLengthInChars - currentChar) - 1);
        } else {
            result12.append(result, currentChar, resLengthInChars - currentChar);
        }
        result12.append('E');
        if (exponent > 0) {
            result12.append('+');
        }
        result12.append(Integer.toString(exponent));
        return result12.toString();
    }

    static String toDecimalScaledString(long value, int scale) {
        boolean negNumber = value < 0;
        if (negNumber) {
            value = -value;
        }
        if (value == 0) {
            switch (scale) {
                case 0:
                    return AndroidHardcodedSystemProperties.JAVA_VERSION;
                case 1:
                    return "0.0";
                case 2:
                    return "0.00";
                case 3:
                    return "0.000";
                case 4:
                    return "0.0000";
                case 5:
                    return "0.00000";
                case 6:
                    return "0.000000";
                default:
                    StringBuilder result1 = new StringBuilder();
                    if (scale < 0) {
                        result1.append("0E+");
                    } else {
                        result1.append("0E");
                    }
                    result1.append(scale == Integer.MIN_VALUE ? "2147483648" : Integer.toString(-scale));
                    return result1.toString();
            }
        }
        char[] result = new char[19];
        int currentChar = 18;
        long v = value;
        do {
            long prev = v;
            v /= 10;
            currentChar--;
            result[currentChar] = (char) ((prev - (10 * v)) + 48);
        } while (v != 0);
        long exponent = ((18 - ((long) currentChar)) - ((long) scale)) - 1;
        if (scale == 0) {
            if (negNumber) {
                currentChar--;
                result[currentChar] = '-';
            }
            return new String(result, currentChar, 18 - currentChar);
        }
        if (scale <= 0 || exponent < -6) {
            int startPoint = currentChar + 1;
            StringBuilder result12 = new StringBuilder(34 - startPoint);
            if (negNumber) {
                result12.append('-');
            }
            if (18 - startPoint >= 1) {
                result12.append(result[currentChar]);
                result12.append('.');
                result12.append(result, currentChar + 1, (18 - currentChar) - 1);
            } else {
                result12.append(result, currentChar, 18 - currentChar);
            }
            result12.append('E');
            if (exponent > 0) {
                result12.append('+');
            }
            result12.append(Long.toString(exponent));
            return result12.toString();
        }
        if (exponent >= 0) {
            int insertPoint = currentChar + ((int) exponent);
            for (int j = 17; j >= insertPoint; j--) {
                result[j + 1] = result[j];
            }
            result[insertPoint + 1] = '.';
            if (negNumber) {
                currentChar--;
                result[currentChar] = '-';
            }
            return new String(result, currentChar, (18 - currentChar) + 1);
        }
        for (int j2 = 2; j2 < (-exponent) + 1; j2++) {
            currentChar--;
            result[currentChar] = '0';
        }
        int currentChar2 = currentChar - 1;
        result[currentChar2] = '.';
        int currentChar3 = currentChar2 - 1;
        result[currentChar3] = '0';
        if (negNumber) {
            currentChar3--;
            result[currentChar3] = '-';
        }
        return new String(result, currentChar3, 18 - currentChar3);
    }

    static long divideLongByBillion(long a) {
        long quot;
        long rem;
        if (a >= 0) {
            quot = a / 1000000000;
            rem = a % 1000000000;
        } else {
            long aPos = a >>> 1;
            quot = aPos / 500000000;
            long rem2 = aPos % 500000000;
            rem = (rem2 << 1) + (1 & a);
        }
        return (rem << 32) | (4294967295L & quot);
    }

    static double bigInteger2Double(BigInteger val) {
        val.prepareJavaRepresentation();
        if (val.numberLength < 2 || (val.numberLength == 2 && val.digits[1] > 0)) {
            return val.longValue();
        }
        if (val.numberLength > 32) {
            return val.sign > 0 ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
        }
        int bitLen = val.abs().bitLength();
        long exponent = bitLen - 1;
        int delta = bitLen - 54;
        long lVal = val.abs().shiftRight(delta).longValue();
        long mantissa = lVal & 9007199254740991L;
        if (exponent == 1023) {
            if (mantissa == 9007199254740991L) {
                return val.sign > 0 ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
            }
            if (mantissa == 9007199254740990L) {
                return val.sign > 0 ? Double.MAX_VALUE : -1.7976931348623157E308d;
            }
        }
        if ((1 & mantissa) == 1 && ((2 & mantissa) == 2 || BitLevel.nonZeroDroppedBits(delta, val.digits))) {
            mantissa += 2;
        }
        long mantissa2 = mantissa >> 1;
        long resSign = val.sign < 0 ? Long.MIN_VALUE : 0L;
        long result = resSign | (((1023 + exponent) << 52) & 9218868437227405312L) | mantissa2;
        return Double.longBitsToDouble(result);
    }
}
