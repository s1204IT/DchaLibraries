package java.math;

class Logical {
    private Logical() {
    }

    static BigInteger not(BigInteger bigInteger) {
        if (bigInteger.sign == 0) {
            return BigInteger.MINUS_ONE;
        }
        if (bigInteger.equals(BigInteger.MINUS_ONE)) {
            return BigInteger.ZERO;
        }
        int[] iArr = new int[bigInteger.numberLength + 1];
        int i = 0;
        if (bigInteger.sign > 0) {
            if (bigInteger.digits[bigInteger.numberLength - 1] != -1) {
                while (bigInteger.digits[i] == -1) {
                    i++;
                }
            } else {
                while (i < bigInteger.numberLength && bigInteger.digits[i] == -1) {
                    i++;
                }
                if (i == bigInteger.numberLength) {
                    iArr[i] = 1;
                    return new BigInteger(-bigInteger.sign, i + 1, iArr);
                }
            }
        } else {
            while (bigInteger.digits[i] == 0) {
                iArr[i] = -1;
                i++;
            }
        }
        iArr[i] = bigInteger.digits[i] + bigInteger.sign;
        int i2 = i + 1;
        while (i2 < bigInteger.numberLength) {
            iArr[i2] = bigInteger.digits[i2];
            i2++;
        }
        return new BigInteger(-bigInteger.sign, i2, iArr);
    }

    static BigInteger and(BigInteger bigInteger, BigInteger bigInteger2) {
        if (bigInteger2.sign == 0 || bigInteger.sign == 0) {
            return BigInteger.ZERO;
        }
        if (bigInteger2.equals(BigInteger.MINUS_ONE)) {
            return bigInteger;
        }
        if (bigInteger.equals(BigInteger.MINUS_ONE)) {
            return bigInteger2;
        }
        if (bigInteger.sign > 0) {
            if (bigInteger2.sign > 0) {
                return andPositive(bigInteger, bigInteger2);
            }
            return andDiffSigns(bigInteger, bigInteger2);
        }
        if (bigInteger2.sign > 0) {
            return andDiffSigns(bigInteger2, bigInteger);
        }
        if (bigInteger.numberLength > bigInteger2.numberLength) {
            return andNegative(bigInteger, bigInteger2);
        }
        return andNegative(bigInteger2, bigInteger);
    }

    static BigInteger andPositive(BigInteger bigInteger, BigInteger bigInteger2) {
        int iMin = Math.min(bigInteger.numberLength, bigInteger2.numberLength);
        int iMax = Math.max(bigInteger.getFirstNonzeroDigit(), bigInteger2.getFirstNonzeroDigit());
        if (iMax >= iMin) {
            return BigInteger.ZERO;
        }
        int[] iArr = new int[iMin];
        while (iMax < iMin) {
            iArr[iMax] = bigInteger.digits[iMax] & bigInteger2.digits[iMax];
            iMax++;
        }
        return new BigInteger(1, iMin, iArr);
    }

    static BigInteger andDiffSigns(BigInteger bigInteger, BigInteger bigInteger2) {
        int firstNonzeroDigit = bigInteger.getFirstNonzeroDigit();
        int firstNonzeroDigit2 = bigInteger2.getFirstNonzeroDigit();
        if (firstNonzeroDigit2 >= bigInteger.numberLength) {
            return BigInteger.ZERO;
        }
        int i = bigInteger.numberLength;
        int[] iArr = new int[i];
        int iMax = Math.max(firstNonzeroDigit, firstNonzeroDigit2);
        if (iMax == firstNonzeroDigit2) {
            iArr[iMax] = (-bigInteger2.digits[iMax]) & bigInteger.digits[iMax];
            iMax++;
        }
        int iMin = Math.min(bigInteger2.numberLength, bigInteger.numberLength);
        while (iMax < iMin) {
            iArr[iMax] = (~bigInteger2.digits[iMax]) & bigInteger.digits[iMax];
            iMax++;
        }
        if (iMax >= bigInteger2.numberLength) {
            while (iMax < bigInteger.numberLength) {
                iArr[iMax] = bigInteger.digits[iMax];
                iMax++;
            }
        }
        return new BigInteger(1, i, iArr);
    }

    static BigInteger andNegative(BigInteger bigInteger, BigInteger bigInteger2) {
        int i;
        int firstNonzeroDigit = bigInteger.getFirstNonzeroDigit();
        int firstNonzeroDigit2 = bigInteger2.getFirstNonzeroDigit();
        if (firstNonzeroDigit >= bigInteger2.numberLength) {
            return bigInteger;
        }
        int iMax = Math.max(firstNonzeroDigit2, firstNonzeroDigit);
        if (firstNonzeroDigit2 > firstNonzeroDigit) {
            i = (-bigInteger2.digits[iMax]) & (~bigInteger.digits[iMax]);
        } else if (firstNonzeroDigit2 < firstNonzeroDigit) {
            i = (~bigInteger2.digits[iMax]) & (-bigInteger.digits[iMax]);
        } else {
            i = (-bigInteger2.digits[iMax]) & (-bigInteger.digits[iMax]);
        }
        if (i == 0) {
            do {
                iMax++;
                if (iMax >= bigInteger2.numberLength) {
                    break;
                }
                i = ~(bigInteger.digits[iMax] | bigInteger2.digits[iMax]);
            } while (i == 0);
            if (i == 0) {
                while (iMax < bigInteger.numberLength && (i = ~bigInteger.digits[iMax]) == 0) {
                    iMax++;
                }
                if (i == 0) {
                    int i2 = bigInteger.numberLength + 1;
                    int[] iArr = new int[i2];
                    iArr[i2 - 1] = 1;
                    return new BigInteger(-1, i2, iArr);
                }
            }
        }
        int i3 = bigInteger.numberLength;
        int[] iArr2 = new int[i3];
        iArr2[iMax] = -i;
        int i4 = iMax + 1;
        while (i4 < bigInteger2.numberLength) {
            iArr2[i4] = bigInteger.digits[i4] | bigInteger2.digits[i4];
            i4++;
        }
        while (i4 < bigInteger.numberLength) {
            iArr2[i4] = bigInteger.digits[i4];
            i4++;
        }
        return new BigInteger(-1, i3, iArr2);
    }

    static BigInteger andNot(BigInteger bigInteger, BigInteger bigInteger2) {
        if (bigInteger2.sign == 0) {
            return bigInteger;
        }
        if (bigInteger.sign == 0) {
            return BigInteger.ZERO;
        }
        if (bigInteger.equals(BigInteger.MINUS_ONE)) {
            return bigInteger2.not();
        }
        if (bigInteger2.equals(BigInteger.MINUS_ONE)) {
            return BigInteger.ZERO;
        }
        if (bigInteger.sign > 0) {
            if (bigInteger2.sign > 0) {
                return andNotPositive(bigInteger, bigInteger2);
            }
            return andNotPositiveNegative(bigInteger, bigInteger2);
        }
        if (bigInteger2.sign > 0) {
            return andNotNegativePositive(bigInteger, bigInteger2);
        }
        return andNotNegative(bigInteger, bigInteger2);
    }

    static BigInteger andNotPositive(BigInteger bigInteger, BigInteger bigInteger2) {
        int[] iArr = new int[bigInteger.numberLength];
        int iMin = Math.min(bigInteger.numberLength, bigInteger2.numberLength);
        int firstNonzeroDigit = bigInteger.getFirstNonzeroDigit();
        while (firstNonzeroDigit < iMin) {
            iArr[firstNonzeroDigit] = bigInteger.digits[firstNonzeroDigit] & (~bigInteger2.digits[firstNonzeroDigit]);
            firstNonzeroDigit++;
        }
        while (firstNonzeroDigit < bigInteger.numberLength) {
            iArr[firstNonzeroDigit] = bigInteger.digits[firstNonzeroDigit];
            firstNonzeroDigit++;
        }
        return new BigInteger(1, bigInteger.numberLength, iArr);
    }

    static BigInteger andNotPositiveNegative(BigInteger bigInteger, BigInteger bigInteger2) {
        int firstNonzeroDigit = bigInteger2.getFirstNonzeroDigit();
        int firstNonzeroDigit2 = bigInteger.getFirstNonzeroDigit();
        if (firstNonzeroDigit >= bigInteger.numberLength) {
            return bigInteger;
        }
        int iMin = Math.min(bigInteger.numberLength, bigInteger2.numberLength);
        int[] iArr = new int[iMin];
        while (firstNonzeroDigit2 < firstNonzeroDigit) {
            iArr[firstNonzeroDigit2] = bigInteger.digits[firstNonzeroDigit2];
            firstNonzeroDigit2++;
        }
        if (firstNonzeroDigit2 == firstNonzeroDigit) {
            iArr[firstNonzeroDigit2] = bigInteger.digits[firstNonzeroDigit2] & (bigInteger2.digits[firstNonzeroDigit2] - 1);
            firstNonzeroDigit2++;
        }
        while (firstNonzeroDigit2 < iMin) {
            iArr[firstNonzeroDigit2] = bigInteger.digits[firstNonzeroDigit2] & bigInteger2.digits[firstNonzeroDigit2];
            firstNonzeroDigit2++;
        }
        return new BigInteger(1, iMin, iArr);
    }

    static BigInteger andNotNegativePositive(BigInteger bigInteger, BigInteger bigInteger2) {
        int[] iArr;
        int firstNonzeroDigit = bigInteger.getFirstNonzeroDigit();
        int firstNonzeroDigit2 = bigInteger2.getFirstNonzeroDigit();
        if (firstNonzeroDigit >= bigInteger2.numberLength) {
            return bigInteger;
        }
        int iMax = Math.max(bigInteger.numberLength, bigInteger2.numberLength);
        if (firstNonzeroDigit2 > firstNonzeroDigit) {
            iArr = new int[iMax];
            int iMin = Math.min(bigInteger.numberLength, firstNonzeroDigit2);
            while (firstNonzeroDigit < iMin) {
                iArr[firstNonzeroDigit] = bigInteger.digits[firstNonzeroDigit];
                firstNonzeroDigit++;
            }
            if (firstNonzeroDigit == bigInteger.numberLength) {
                firstNonzeroDigit = firstNonzeroDigit2;
                while (firstNonzeroDigit < bigInteger2.numberLength) {
                    iArr[firstNonzeroDigit] = bigInteger2.digits[firstNonzeroDigit];
                    firstNonzeroDigit++;
                }
            }
        } else {
            int i = (-bigInteger.digits[firstNonzeroDigit]) & (~bigInteger2.digits[firstNonzeroDigit]);
            if (i == 0) {
                int iMin2 = Math.min(bigInteger2.numberLength, bigInteger.numberLength);
                do {
                    firstNonzeroDigit++;
                    if (firstNonzeroDigit >= iMin2) {
                        break;
                    }
                    i = ~(bigInteger.digits[firstNonzeroDigit] | bigInteger2.digits[firstNonzeroDigit]);
                } while (i == 0);
                if (i == 0) {
                    while (firstNonzeroDigit < bigInteger2.numberLength && (i = ~bigInteger2.digits[firstNonzeroDigit]) == 0) {
                        firstNonzeroDigit++;
                    }
                    while (firstNonzeroDigit < bigInteger.numberLength && (i = ~bigInteger.digits[firstNonzeroDigit]) == 0) {
                        firstNonzeroDigit++;
                    }
                    if (i == 0) {
                        int i2 = iMax + 1;
                        int[] iArr2 = new int[i2];
                        iArr2[i2 - 1] = 1;
                        return new BigInteger(-1, i2, iArr2);
                    }
                }
            }
            int[] iArr3 = new int[iMax];
            iArr3[firstNonzeroDigit] = -i;
            firstNonzeroDigit++;
            iArr = iArr3;
        }
        int iMin3 = Math.min(bigInteger2.numberLength, bigInteger.numberLength);
        while (firstNonzeroDigit < iMin3) {
            iArr[firstNonzeroDigit] = bigInteger.digits[firstNonzeroDigit] | bigInteger2.digits[firstNonzeroDigit];
            firstNonzeroDigit++;
        }
        while (firstNonzeroDigit < bigInteger.numberLength) {
            iArr[firstNonzeroDigit] = bigInteger.digits[firstNonzeroDigit];
            firstNonzeroDigit++;
        }
        while (firstNonzeroDigit < bigInteger2.numberLength) {
            iArr[firstNonzeroDigit] = bigInteger2.digits[firstNonzeroDigit];
            firstNonzeroDigit++;
        }
        return new BigInteger(-1, iMax, iArr);
    }

    static BigInteger andNotNegative(BigInteger bigInteger, BigInteger bigInteger2) {
        int firstNonzeroDigit = bigInteger.getFirstNonzeroDigit();
        int firstNonzeroDigit2 = bigInteger2.getFirstNonzeroDigit();
        if (firstNonzeroDigit >= bigInteger2.numberLength) {
            return BigInteger.ZERO;
        }
        int i = bigInteger2.numberLength;
        int[] iArr = new int[i];
        if (firstNonzeroDigit < firstNonzeroDigit2) {
            iArr[firstNonzeroDigit] = -bigInteger.digits[firstNonzeroDigit];
            int iMin = Math.min(bigInteger.numberLength, firstNonzeroDigit2);
            while (true) {
                firstNonzeroDigit++;
                if (firstNonzeroDigit >= iMin) {
                    break;
                }
                iArr[firstNonzeroDigit] = ~bigInteger.digits[firstNonzeroDigit];
            }
            if (firstNonzeroDigit != bigInteger.numberLength) {
                iArr[firstNonzeroDigit] = (~bigInteger.digits[firstNonzeroDigit]) & (bigInteger2.digits[firstNonzeroDigit] - 1);
            } else {
                while (firstNonzeroDigit < firstNonzeroDigit2) {
                    iArr[firstNonzeroDigit] = -1;
                    firstNonzeroDigit++;
                }
                iArr[firstNonzeroDigit] = bigInteger2.digits[firstNonzeroDigit] - 1;
            }
        } else if (firstNonzeroDigit2 >= firstNonzeroDigit) {
            iArr[firstNonzeroDigit] = (-bigInteger.digits[firstNonzeroDigit]) & (bigInteger2.digits[firstNonzeroDigit] - 1);
        } else {
            iArr[firstNonzeroDigit] = (-bigInteger.digits[firstNonzeroDigit]) & bigInteger2.digits[firstNonzeroDigit];
        }
        int iMin2 = Math.min(bigInteger.numberLength, bigInteger2.numberLength);
        int i2 = firstNonzeroDigit + 1;
        while (i2 < iMin2) {
            iArr[i2] = (~bigInteger.digits[i2]) & bigInteger2.digits[i2];
            i2++;
        }
        while (i2 < bigInteger2.numberLength) {
            iArr[i2] = bigInteger2.digits[i2];
            i2++;
        }
        return new BigInteger(1, i, iArr);
    }

    static BigInteger or(BigInteger bigInteger, BigInteger bigInteger2) {
        if (bigInteger2.equals(BigInteger.MINUS_ONE) || bigInteger.equals(BigInteger.MINUS_ONE)) {
            return BigInteger.MINUS_ONE;
        }
        if (bigInteger2.sign == 0) {
            return bigInteger;
        }
        if (bigInteger.sign == 0) {
            return bigInteger2;
        }
        if (bigInteger.sign > 0) {
            if (bigInteger2.sign > 0) {
                if (bigInteger.numberLength > bigInteger2.numberLength) {
                    return orPositive(bigInteger, bigInteger2);
                }
                return orPositive(bigInteger2, bigInteger);
            }
            return orDiffSigns(bigInteger, bigInteger2);
        }
        if (bigInteger2.sign > 0) {
            return orDiffSigns(bigInteger2, bigInteger);
        }
        if (bigInteger2.getFirstNonzeroDigit() > bigInteger.getFirstNonzeroDigit()) {
            return orNegative(bigInteger2, bigInteger);
        }
        return orNegative(bigInteger, bigInteger2);
    }

    static BigInteger orPositive(BigInteger bigInteger, BigInteger bigInteger2) {
        int i = bigInteger.numberLength;
        int[] iArr = new int[i];
        int i2 = 0;
        while (i2 < bigInteger2.numberLength) {
            iArr[i2] = bigInteger.digits[i2] | bigInteger2.digits[i2];
            i2++;
        }
        while (i2 < i) {
            iArr[i2] = bigInteger.digits[i2];
            i2++;
        }
        return new BigInteger(1, i, iArr);
    }

    static BigInteger orNegative(BigInteger bigInteger, BigInteger bigInteger2) {
        int firstNonzeroDigit = bigInteger2.getFirstNonzeroDigit();
        int firstNonzeroDigit2 = bigInteger.getFirstNonzeroDigit();
        if (firstNonzeroDigit2 >= bigInteger2.numberLength) {
            return bigInteger2;
        }
        if (firstNonzeroDigit >= bigInteger.numberLength) {
            return bigInteger;
        }
        int iMin = Math.min(bigInteger.numberLength, bigInteger2.numberLength);
        int[] iArr = new int[iMin];
        if (firstNonzeroDigit == firstNonzeroDigit2) {
            iArr[firstNonzeroDigit2] = -((-bigInteger.digits[firstNonzeroDigit2]) | (-bigInteger2.digits[firstNonzeroDigit2]));
            firstNonzeroDigit = firstNonzeroDigit2;
        } else {
            while (firstNonzeroDigit < firstNonzeroDigit2) {
                iArr[firstNonzeroDigit] = bigInteger2.digits[firstNonzeroDigit];
                firstNonzeroDigit++;
            }
            iArr[firstNonzeroDigit] = bigInteger2.digits[firstNonzeroDigit] & (bigInteger.digits[firstNonzeroDigit] - 1);
        }
        while (true) {
            firstNonzeroDigit++;
            if (firstNonzeroDigit < iMin) {
                iArr[firstNonzeroDigit] = bigInteger.digits[firstNonzeroDigit] & bigInteger2.digits[firstNonzeroDigit];
            } else {
                return new BigInteger(-1, iMin, iArr);
            }
        }
    }

    static BigInteger orDiffSigns(BigInteger bigInteger, BigInteger bigInteger2) {
        int firstNonzeroDigit = bigInteger2.getFirstNonzeroDigit();
        int firstNonzeroDigit2 = bigInteger.getFirstNonzeroDigit();
        if (firstNonzeroDigit2 >= bigInteger2.numberLength) {
            return bigInteger2;
        }
        int i = bigInteger2.numberLength;
        int[] iArr = new int[i];
        if (firstNonzeroDigit < firstNonzeroDigit2) {
            while (firstNonzeroDigit < firstNonzeroDigit2) {
                iArr[firstNonzeroDigit] = bigInteger2.digits[firstNonzeroDigit];
                firstNonzeroDigit++;
            }
        } else if (firstNonzeroDigit2 < firstNonzeroDigit) {
            iArr[firstNonzeroDigit2] = -bigInteger.digits[firstNonzeroDigit2];
            int iMin = Math.min(bigInteger.numberLength, firstNonzeroDigit);
            while (true) {
                firstNonzeroDigit2++;
                if (firstNonzeroDigit2 >= iMin) {
                    break;
                }
                iArr[firstNonzeroDigit2] = ~bigInteger.digits[firstNonzeroDigit2];
            }
            if (firstNonzeroDigit2 != bigInteger.numberLength) {
                iArr[firstNonzeroDigit2] = ~((-bigInteger2.digits[firstNonzeroDigit2]) | bigInteger.digits[firstNonzeroDigit2]);
            } else {
                while (firstNonzeroDigit2 < firstNonzeroDigit) {
                    iArr[firstNonzeroDigit2] = -1;
                    firstNonzeroDigit2++;
                }
                iArr[firstNonzeroDigit2] = bigInteger2.digits[firstNonzeroDigit2] - 1;
            }
            firstNonzeroDigit = firstNonzeroDigit2 + 1;
        } else {
            iArr[firstNonzeroDigit2] = -((-bigInteger2.digits[firstNonzeroDigit2]) | bigInteger.digits[firstNonzeroDigit2]);
            firstNonzeroDigit = firstNonzeroDigit2 + 1;
        }
        int iMin2 = Math.min(bigInteger2.numberLength, bigInteger.numberLength);
        while (firstNonzeroDigit < iMin2) {
            iArr[firstNonzeroDigit] = bigInteger2.digits[firstNonzeroDigit] & (~bigInteger.digits[firstNonzeroDigit]);
            firstNonzeroDigit++;
        }
        while (firstNonzeroDigit < bigInteger2.numberLength) {
            iArr[firstNonzeroDigit] = bigInteger2.digits[firstNonzeroDigit];
            firstNonzeroDigit++;
        }
        return new BigInteger(-1, i, iArr);
    }

    static BigInteger xor(BigInteger bigInteger, BigInteger bigInteger2) {
        if (bigInteger2.sign == 0) {
            return bigInteger;
        }
        if (bigInteger.sign == 0) {
            return bigInteger2;
        }
        if (bigInteger2.equals(BigInteger.MINUS_ONE)) {
            return bigInteger.not();
        }
        if (bigInteger.equals(BigInteger.MINUS_ONE)) {
            return bigInteger2.not();
        }
        if (bigInteger.sign > 0) {
            if (bigInteger2.sign > 0) {
                if (bigInteger.numberLength > bigInteger2.numberLength) {
                    return xorPositive(bigInteger, bigInteger2);
                }
                return xorPositive(bigInteger2, bigInteger);
            }
            return xorDiffSigns(bigInteger, bigInteger2);
        }
        if (bigInteger2.sign > 0) {
            return xorDiffSigns(bigInteger2, bigInteger);
        }
        if (bigInteger2.getFirstNonzeroDigit() > bigInteger.getFirstNonzeroDigit()) {
            return xorNegative(bigInteger2, bigInteger);
        }
        return xorNegative(bigInteger, bigInteger2);
    }

    static BigInteger xorPositive(BigInteger bigInteger, BigInteger bigInteger2) {
        int i = bigInteger.numberLength;
        int[] iArr = new int[i];
        int iMin = Math.min(bigInteger.getFirstNonzeroDigit(), bigInteger2.getFirstNonzeroDigit());
        while (iMin < bigInteger2.numberLength) {
            iArr[iMin] = bigInteger.digits[iMin] ^ bigInteger2.digits[iMin];
            iMin++;
        }
        while (iMin < bigInteger.numberLength) {
            iArr[iMin] = bigInteger.digits[iMin];
            iMin++;
        }
        return new BigInteger(1, i, iArr);
    }

    static BigInteger xorNegative(BigInteger bigInteger, BigInteger bigInteger2) {
        int iMax = Math.max(bigInteger.numberLength, bigInteger2.numberLength);
        int[] iArr = new int[iMax];
        int firstNonzeroDigit = bigInteger.getFirstNonzeroDigit();
        int firstNonzeroDigit2 = bigInteger2.getFirstNonzeroDigit();
        if (firstNonzeroDigit == firstNonzeroDigit2) {
            iArr[firstNonzeroDigit2] = (-bigInteger.digits[firstNonzeroDigit2]) ^ (-bigInteger2.digits[firstNonzeroDigit2]);
        } else {
            iArr[firstNonzeroDigit2] = -bigInteger2.digits[firstNonzeroDigit2];
            int iMin = Math.min(bigInteger2.numberLength, firstNonzeroDigit);
            while (true) {
                firstNonzeroDigit2++;
                if (firstNonzeroDigit2 >= iMin) {
                    break;
                }
                iArr[firstNonzeroDigit2] = ~bigInteger2.digits[firstNonzeroDigit2];
            }
            if (firstNonzeroDigit2 == bigInteger2.numberLength) {
                while (firstNonzeroDigit2 < firstNonzeroDigit) {
                    iArr[firstNonzeroDigit2] = -1;
                    firstNonzeroDigit2++;
                }
                iArr[firstNonzeroDigit2] = bigInteger.digits[firstNonzeroDigit2] - 1;
            } else {
                iArr[firstNonzeroDigit2] = (-bigInteger.digits[firstNonzeroDigit2]) ^ (~bigInteger2.digits[firstNonzeroDigit2]);
            }
        }
        int iMin2 = Math.min(bigInteger.numberLength, bigInteger2.numberLength);
        int i = firstNonzeroDigit2 + 1;
        while (i < iMin2) {
            iArr[i] = bigInteger.digits[i] ^ bigInteger2.digits[i];
            i++;
        }
        while (i < bigInteger.numberLength) {
            iArr[i] = bigInteger.digits[i];
            i++;
        }
        while (i < bigInteger2.numberLength) {
            iArr[i] = bigInteger2.digits[i];
            i++;
        }
        return new BigInteger(1, iMax, iArr);
    }

    static BigInteger xorDiffSigns(BigInteger bigInteger, BigInteger bigInteger2) {
        int[] iArr;
        int i;
        int iMax = Math.max(bigInteger2.numberLength, bigInteger.numberLength);
        int firstNonzeroDigit = bigInteger2.getFirstNonzeroDigit();
        int firstNonzeroDigit2 = bigInteger.getFirstNonzeroDigit();
        if (firstNonzeroDigit < firstNonzeroDigit2) {
            iArr = new int[iMax];
            iArr[firstNonzeroDigit] = bigInteger2.digits[firstNonzeroDigit];
            int iMin = Math.min(bigInteger2.numberLength, firstNonzeroDigit2);
            i = firstNonzeroDigit + 1;
            while (i < iMin) {
                iArr[i] = bigInteger2.digits[i];
                i++;
            }
            if (i == bigInteger2.numberLength) {
                while (i < bigInteger.numberLength) {
                    iArr[i] = bigInteger.digits[i];
                    i++;
                }
            }
        } else if (firstNonzeroDigit2 < firstNonzeroDigit) {
            iArr = new int[iMax];
            iArr[firstNonzeroDigit2] = -bigInteger.digits[firstNonzeroDigit2];
            int iMin2 = Math.min(bigInteger.numberLength, firstNonzeroDigit);
            int i2 = firstNonzeroDigit2 + 1;
            while (i2 < iMin2) {
                iArr[i2] = ~bigInteger.digits[i2];
                i2++;
            }
            if (i2 == firstNonzeroDigit) {
                iArr[i2] = ~(bigInteger.digits[i2] ^ (-bigInteger2.digits[i2]));
                i = i2 + 1;
            } else {
                while (i2 < firstNonzeroDigit) {
                    iArr[i2] = -1;
                    i2++;
                }
                i = i2;
                while (i < bigInteger2.numberLength) {
                    iArr[i] = bigInteger2.digits[i];
                    i++;
                }
            }
        } else {
            int i3 = bigInteger.digits[firstNonzeroDigit] ^ (-bigInteger2.digits[firstNonzeroDigit]);
            if (i3 == 0) {
                int iMin3 = Math.min(bigInteger.numberLength, bigInteger2.numberLength);
                do {
                    firstNonzeroDigit++;
                    if (firstNonzeroDigit >= iMin3) {
                        break;
                    }
                    i3 = bigInteger.digits[firstNonzeroDigit] ^ (~bigInteger2.digits[firstNonzeroDigit]);
                } while (i3 == 0);
                if (i3 == 0) {
                    while (firstNonzeroDigit < bigInteger.numberLength && (i3 = ~bigInteger.digits[firstNonzeroDigit]) == 0) {
                        firstNonzeroDigit++;
                    }
                    while (firstNonzeroDigit < bigInteger2.numberLength && (i3 = ~bigInteger2.digits[firstNonzeroDigit]) == 0) {
                        firstNonzeroDigit++;
                    }
                    if (i3 == 0) {
                        int i4 = iMax + 1;
                        int[] iArr2 = new int[i4];
                        iArr2[i4 - 1] = 1;
                        return new BigInteger(-1, i4, iArr2);
                    }
                }
            }
            iArr = new int[iMax];
            iArr[firstNonzeroDigit] = -i3;
            i = firstNonzeroDigit + 1;
        }
        int iMin4 = Math.min(bigInteger2.numberLength, bigInteger.numberLength);
        while (i < iMin4) {
            iArr[i] = ~((~bigInteger2.digits[i]) ^ bigInteger.digits[i]);
            i++;
        }
        while (i < bigInteger.numberLength) {
            iArr[i] = bigInteger.digits[i];
            i++;
        }
        while (i < bigInteger2.numberLength) {
            iArr[i] = bigInteger2.digits[i];
            i++;
        }
        return new BigInteger(-1, iMax, iArr);
    }
}
