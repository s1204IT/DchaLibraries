package java.math;

import dalvik.system.VMDebug;

class Conversion {
    static final int[] digitFitInInt = {-1, -1, 31, 19, 15, 13, 11, 11, 10, 9, 9, 8, 8, 8, 8, 7, 7, 7, 7, 7, 7, 7, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 5};
    static final int[] bigRadices = {Integer.MIN_VALUE, 1162261467, VMDebug.KIND_THREAD_EXT_FREED_OBJECTS, 1220703125, 362797056, 1977326743, VMDebug.KIND_THREAD_EXT_FREED_OBJECTS, 387420489, 1000000000, 214358881, 429981696, 815730721, 1475789056, 170859375, VMDebug.KIND_THREAD_EXT_ALLOCATED_OBJECTS, 410338673, 612220032, 893871739, 1280000000, 1801088541, 113379904, 148035889, 191102976, 244140625, 308915776, 387420489, 481890304, 594823321, 729000000, 887503681, VMDebug.KIND_THREAD_EXT_FREED_OBJECTS, 1291467969, 1544804416, 1838265625, 60466176};

    private Conversion() {
    }

    static String bigInteger2String(BigInteger bigInteger, int i) {
        int i2;
        bigInteger.prepareJavaRepresentation();
        int i3 = bigInteger.sign;
        int i4 = bigInteger.numberLength;
        int[] iArr = bigInteger.digits;
        if (i3 == 0) {
            return AndroidHardcodedSystemProperties.JAVA_VERSION;
        }
        if (i4 == 1) {
            long j = ((long) iArr[i4 - 1]) & 4294967295L;
            if (i3 < 0) {
                j = -j;
            }
            return Long.toString(j, i);
        }
        if (i != 10 && i >= 2 && i <= 36) {
            int iBitLength = ((int) ((((double) bigInteger.abs().bitLength()) / (Math.log(i) / Math.log(2.0d))) + ((double) (i3 < 0 ? 1 : 0)))) + 1;
            char[] cArr = new char[iBitLength];
            if (i != 16) {
                int[] iArr2 = new int[i4];
                System.arraycopy(iArr, 0, iArr2, 0, i4);
                int i5 = digitFitInInt[i];
                int i6 = bigRadices[i - 2];
                int i7 = iBitLength;
                while (true) {
                    int iDivideArrayByInt = Division.divideArrayByInt(iArr2, iArr2, i4, i6);
                    int i8 = i7;
                    do {
                        i8--;
                        cArr[i8] = Character.forDigit(iDivideArrayByInt % i, i);
                        iDivideArrayByInt /= i;
                        if (iDivideArrayByInt == 0) {
                            break;
                        }
                    } while (i8 != 0);
                    int i9 = (i5 - i7) + i8;
                    i2 = i8;
                    for (int i10 = 0; i10 < i9 && i2 > 0; i10++) {
                        i2--;
                        cArr[i2] = '0';
                    }
                    int i11 = i4 - 1;
                    while (i11 > 0 && iArr2[i11] == 0) {
                        i11--;
                    }
                    i4 = i11 + 1;
                    if (i4 == 1 && iArr2[0] == 0) {
                        break;
                    }
                    i7 = i2;
                }
            } else {
                i2 = iBitLength;
                for (int i12 = 0; i12 < i4; i12++) {
                    for (int i13 = 0; i13 < 8 && i2 > 0; i13++) {
                        i2--;
                        cArr[i2] = Character.forDigit((iArr[i12] >> (i13 << 2)) & 15, 16);
                    }
                }
            }
            while (cArr[i2] == '0') {
                i2++;
            }
            if (i3 == -1) {
                i2--;
                cArr[i2] = '-';
            }
            return new String(cArr, i2, iBitLength - i2);
        }
        return bigInteger.toString();
    }

    static String toDecimalScaledString(BigInteger bigInteger, int i) {
        char[] cArr;
        int i2;
        int i3;
        bigInteger.prepareJavaRepresentation();
        int i4 = bigInteger.sign;
        int i5 = bigInteger.numberLength;
        int[] iArr = bigInteger.digits;
        if (i4 == 0) {
            switch (i) {
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
                    StringBuilder sb = new StringBuilder();
                    if (i < 0) {
                        sb.append("0E+");
                    } else {
                        sb.append("0E");
                    }
                    sb.append(-i);
                    return sb.toString();
            }
        }
        int i6 = 1;
        int i7 = (i5 * 10) + 1 + 7;
        char[] cArr2 = new char[i7 + 1];
        long j = 0;
        if (i5 == 1) {
            int i8 = iArr[0];
            if (i8 < 0) {
                long j2 = 4294967295L & ((long) i8);
                i3 = i7;
                while (true) {
                    long j3 = j2 / 10;
                    i3--;
                    cArr2[i3] = (char) (((int) (j2 - (10 * j3))) + 48);
                    if (j3 == 0) {
                        break;
                    }
                    j2 = j3;
                }
            } else {
                int i9 = i7;
                while (true) {
                    int i10 = i8 / 10;
                    i9--;
                    cArr2[i9] = (char) ((i8 - (i10 * 10)) + 48);
                    if (i10 == 0) {
                        break;
                    }
                    i8 = i10;
                }
                i3 = i9;
            }
            cArr = cArr2;
        } else {
            int[] iArr2 = new int[i5];
            System.arraycopy(iArr, 0, iArr2, 0, i5);
            int i11 = i7;
            loop4: while (true) {
                int i12 = i5 - i6;
                int i13 = i12;
                while (i13 >= 0) {
                    long jDivideLongByBillion = divideLongByBillion((j << 32) + (((long) iArr2[i13]) & 4294967295L));
                    iArr2[i13] = (int) jDivideLongByBillion;
                    j = (int) (jDivideLongByBillion >> 32);
                    i13--;
                    cArr2 = cArr2;
                }
                cArr = cArr2;
                int i14 = (int) j;
                int i15 = i11;
                do {
                    i15--;
                    cArr[i15] = (char) ((i14 % 10) + 48);
                    i14 /= 10;
                    if (i14 == 0) {
                        break;
                    }
                } while (i15 != 0);
                int i16 = (9 - i11) + i15;
                i2 = i15;
                for (int i17 = 0; i17 < i16 && i2 > 0; i17++) {
                    i2--;
                    cArr[i2] = '0';
                }
                while (iArr2[i12] == 0) {
                    if (i12 == 0) {
                        break loop4;
                    }
                    i12--;
                }
                i6 = 1;
                i5 = i12 + 1;
                i11 = i2;
                cArr2 = cArr;
                j = 0;
            }
            i3 = i2;
            while (cArr[i3] == '0') {
                i3++;
            }
        }
        boolean z = i4 < 0;
        int i18 = i7 - i3;
        int i19 = (i18 - i) - 1;
        if (i == 0) {
            if (z) {
                i3--;
                cArr[i3] = '-';
            }
            return new String(cArr, i3, i7 - i3);
        }
        char[] cArr3 = cArr;
        if (i > 0 && i19 >= -6) {
            if (i19 >= 0) {
                int i20 = i19 + i3;
                for (int i21 = i7 - 1; i21 >= i20; i21--) {
                    cArr3[i21 + 1] = cArr3[i21];
                }
                cArr3[i20 + 1] = '.';
                if (z) {
                    i3--;
                    cArr3[i3] = '-';
                }
                return new String(cArr3, i3, (i7 - i3) + 1);
            }
            int i22 = 2;
            for (int i23 = 1; i22 < (-i19) + i23; i23 = 1) {
                i3--;
                cArr3[i3] = '0';
                i22++;
            }
            int i24 = i3 - 1;
            cArr3[i24] = '.';
            int i25 = i24 - 1;
            cArr3[i25] = '0';
            if (z) {
                i25--;
                cArr3[i25] = '-';
            }
            return new String(cArr3, i25, i7 - i25);
        }
        int i26 = i3 + 1;
        StringBuilder sb2 = new StringBuilder((16 + i7) - i26);
        if (z) {
            sb2.append('-');
        }
        if (i7 - i26 >= 1) {
            sb2.append(cArr3[i3]);
            sb2.append('.');
            sb2.append(cArr3, i26, i18 - 1);
        } else {
            sb2.append(cArr3, i3, i18);
        }
        sb2.append('E');
        if (i19 > 0) {
            sb2.append('+');
        }
        sb2.append(Integer.toString(i19));
        return sb2.toString();
    }

    static String toDecimalScaledString(long j, int i) {
        boolean z;
        if (j >= 0) {
            z = false;
        } else {
            z = true;
        }
        if (z) {
            j = -j;
        }
        if (j == 0) {
            switch (i) {
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
                    StringBuilder sb = new StringBuilder();
                    if (i < 0) {
                        sb.append("0E+");
                    } else {
                        sb.append("0E");
                    }
                    sb.append(i == Integer.MIN_VALUE ? "2147483648" : Integer.toString(-i));
                    return sb.toString();
            }
        }
        char[] cArr = new char[19];
        int i2 = 18;
        while (true) {
            long j2 = j / 10;
            i2--;
            cArr[i2] = (char) (48 + (j - (10 * j2)));
            if (j2 == 0) {
                break;
            }
            j = j2;
        }
        long j3 = ((((long) 18) - ((long) i2)) - ((long) i)) - 1;
        if (i == 0) {
            if (z) {
                i2--;
                cArr[i2] = '-';
            }
            return new String(cArr, i2, 18 - i2);
        }
        if (i <= 0 || j3 < -6) {
            int i3 = i2 + 1;
            StringBuilder sb2 = new StringBuilder(34 - i3);
            if (z) {
                sb2.append('-');
            }
            if (18 - i3 >= 1) {
                sb2.append(cArr[i2]);
                sb2.append('.');
                sb2.append(cArr, i3, (18 - i2) - 1);
            } else {
                sb2.append(cArr, i2, 18 - i2);
            }
            sb2.append('E');
            if (j3 > 0) {
                sb2.append('+');
            }
            sb2.append(Long.toString(j3));
            return sb2.toString();
        }
        if (j3 >= 0) {
            int i4 = ((int) j3) + i2;
            for (int i5 = 17; i5 >= i4; i5--) {
                cArr[i5 + 1] = cArr[i5];
            }
            cArr[i4 + 1] = '.';
            if (z) {
                i2--;
                cArr[i2] = '-';
            }
            return new String(cArr, i2, (18 - i2) + 1);
        }
        for (int i6 = 2; i6 < (-j3) + 1; i6++) {
            i2--;
            cArr[i2] = '0';
        }
        int i7 = i2 - 1;
        cArr[i7] = '.';
        int i8 = i7 - 1;
        cArr[i8] = '0';
        if (z) {
            i8--;
            cArr[i8] = '-';
        }
        return new String(cArr, i8, 18 - i8);
    }

    static long divideLongByBillion(long j) {
        long j2;
        long j3;
        if (j >= 0) {
            j3 = j / 1000000000;
            j2 = j % 1000000000;
        } else {
            long j4 = j >>> 1;
            j2 = (j & 1) + ((j4 % 500000000) << 1);
            j3 = j4 / 500000000;
        }
        return (j2 << 32) | (4294967295L & j3);
    }

    static double bigInteger2Double(BigInteger bigInteger) {
        bigInteger.prepareJavaRepresentation();
        if (bigInteger.numberLength < 2 || (bigInteger.numberLength == 2 && bigInteger.digits[1] > 0)) {
            return bigInteger.longValue();
        }
        if (bigInteger.numberLength > 32) {
            return bigInteger.sign > 0 ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
        }
        int iBitLength = bigInteger.abs().bitLength();
        long j = iBitLength - 1;
        int i = iBitLength - 54;
        long jLongValue = bigInteger.abs().shiftRight(i).longValue() & 9007199254740991L;
        if (j == 1023) {
            if (jLongValue == 9007199254740991L) {
                return bigInteger.sign > 0 ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
            }
            if (jLongValue == 9007199254740990L) {
                return bigInteger.sign > 0 ? Double.MAX_VALUE : -1.7976931348623157E308d;
            }
        }
        if ((jLongValue & 1) == 1 && ((jLongValue & 2) == 2 || BitLevel.nonZeroDroppedBits(i, bigInteger.digits))) {
            jLongValue += 2;
        }
        return Double.longBitsToDouble((jLongValue >> 1) | (bigInteger.sign < 0 ? Long.MIN_VALUE : 0L) | (((1023 + j) << 52) & 9218868437227405312L));
    }
}
