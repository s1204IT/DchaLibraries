package java.math;

class BitLevel {
    private BitLevel() {
    }

    static int bitLength(BigInteger bigInteger) {
        bigInteger.prepareJavaRepresentation();
        if (bigInteger.sign == 0) {
            return 0;
        }
        int i = bigInteger.numberLength << 5;
        int i2 = bigInteger.digits[bigInteger.numberLength - 1];
        if (bigInteger.sign < 0 && bigInteger.getFirstNonzeroDigit() == bigInteger.numberLength - 1) {
            i2--;
        }
        return i - Integer.numberOfLeadingZeros(i2);
    }

    static int bitCount(BigInteger bigInteger) {
        bigInteger.prepareJavaRepresentation();
        int iBitCount = 0;
        if (bigInteger.sign == 0) {
            return 0;
        }
        int firstNonzeroDigit = bigInteger.getFirstNonzeroDigit();
        if (bigInteger.sign > 0) {
            while (firstNonzeroDigit < bigInteger.numberLength) {
                iBitCount += Integer.bitCount(bigInteger.digits[firstNonzeroDigit]);
                firstNonzeroDigit++;
            }
            return iBitCount;
        }
        int iBitCount2 = Integer.bitCount(-bigInteger.digits[firstNonzeroDigit]);
        while (true) {
            iBitCount += iBitCount2;
            firstNonzeroDigit++;
            if (firstNonzeroDigit < bigInteger.numberLength) {
                iBitCount2 = Integer.bitCount(~bigInteger.digits[firstNonzeroDigit]);
            } else {
                return (bigInteger.numberLength << 5) - iBitCount;
            }
        }
    }

    static boolean testBit(BigInteger bigInteger, int i) {
        bigInteger.prepareJavaRepresentation();
        return (bigInteger.digits[i >> 5] & (1 << (i & 31))) != 0;
    }

    static boolean nonZeroDroppedBits(int i, int[] iArr) {
        int i2 = i >> 5;
        int i3 = i & 31;
        int i4 = 0;
        while (i4 < i2 && iArr[i4] == 0) {
            i4++;
        }
        return (i4 == i2 && (iArr[i4] << (32 - i3)) == 0) ? false : true;
    }

    static void shiftLeftOneBit(int[] iArr, int[] iArr2, int i) {
        int i2 = 0;
        for (int i3 = 0; i3 < i; i3++) {
            int i4 = iArr2[i3];
            iArr[i3] = i2 | (i4 << 1);
            i2 = i4 >>> 31;
        }
        if (i2 != 0) {
            iArr[i] = i2;
        }
    }

    static BigInteger shiftLeftOneBit(BigInteger bigInteger) {
        bigInteger.prepareJavaRepresentation();
        int i = bigInteger.numberLength;
        int i2 = i + 1;
        int[] iArr = new int[i2];
        shiftLeftOneBit(iArr, bigInteger.digits, i);
        return new BigInteger(bigInteger.sign, i2, iArr);
    }

    static BigInteger shiftRight(BigInteger bigInteger, int i) {
        bigInteger.prepareJavaRepresentation();
        int i2 = i >> 5;
        int i3 = i & 31;
        if (i2 >= bigInteger.numberLength) {
            return bigInteger.sign < 0 ? BigInteger.MINUS_ONE : BigInteger.ZERO;
        }
        int i4 = bigInteger.numberLength - i2;
        int i5 = i4 + 1;
        int[] iArr = new int[i5];
        shiftRight(iArr, i4, bigInteger.digits, i2, i3);
        if (bigInteger.sign < 0) {
            int i6 = 0;
            while (i6 < i2 && bigInteger.digits[i6] == 0) {
                i6++;
            }
            if (i6 < i2 || (i3 > 0 && (bigInteger.digits[i6] << (32 - i3)) != 0)) {
                int i7 = 0;
                while (i7 < i4 && iArr[i7] == -1) {
                    iArr[i7] = 0;
                    i7++;
                }
                if (i7 == i4) {
                    i4 = i5;
                }
                iArr[i7] = iArr[i7] + 1;
            }
        }
        return new BigInteger(bigInteger.sign, i4, iArr);
    }

    static boolean shiftRight(int[] iArr, int i, int[] iArr2, int i2, int i3) {
        int i4 = 0;
        int i5 = 0;
        boolean z = true;
        while (i5 < i2) {
            z &= iArr2[i5] == 0;
            i5++;
        }
        if (i3 == 0) {
            System.arraycopy(iArr2, i2, iArr, 0, i);
        } else {
            int i6 = 32 - i3;
            z &= (iArr2[i5] << i6) == 0;
            while (i4 < i - 1) {
                int i7 = i4 + i2;
                iArr[i4] = (iArr2[i7 + 1] << i6) | (iArr2[i7] >>> i3);
                i4++;
            }
            iArr[i4] = iArr2[i2 + i4] >>> i3;
        }
        return z;
    }

    static BigInteger flipBit(BigInteger bigInteger, int i) {
        int i2;
        bigInteger.prepareJavaRepresentation();
        if (bigInteger.sign != 0) {
            i2 = bigInteger.sign;
        } else {
            i2 = 1;
        }
        int i3 = i >> 5;
        int i4 = i3 + 1;
        int iMax = Math.max(i4, bigInteger.numberLength) + 1;
        int[] iArr = new int[iMax];
        int i5 = 1 << (i & 31);
        System.arraycopy(bigInteger.digits, 0, iArr, 0, bigInteger.numberLength);
        if (bigInteger.sign < 0) {
            if (i3 >= bigInteger.numberLength) {
                iArr[i3] = i5;
            } else {
                int firstNonzeroDigit = bigInteger.getFirstNonzeroDigit();
                if (i3 > firstNonzeroDigit) {
                    iArr[i3] = iArr[i3] ^ i5;
                } else if (i3 < firstNonzeroDigit) {
                    iArr[i3] = -i5;
                    while (i4 < firstNonzeroDigit) {
                        iArr[i4] = -1;
                        i4++;
                    }
                    int i6 = iArr[i4];
                    iArr[i4] = i6 - 1;
                    iArr[i4] = i6;
                } else {
                    iArr[i3] = -((-iArr[i3]) ^ i5);
                    if (iArr[i3] == 0) {
                        while (iArr[i4] == -1) {
                            iArr[i4] = 0;
                            i4++;
                        }
                        iArr[i4] = iArr[i4] + 1;
                    }
                }
            }
        } else {
            iArr[i3] = iArr[i3] ^ i5;
        }
        return new BigInteger(i2, iMax, iArr);
    }
}
