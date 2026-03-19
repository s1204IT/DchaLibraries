package java.math;

import dalvik.system.VMRuntime;

class Multiplication {
    static final int[] tenPows = {1, 10, 100, 1000, VMRuntime.SDK_VERSION_CUR_DEVELOPMENT, 100000, 1000000, 10000000, 100000000, 1000000000};
    static final int[] fivePows = {1, 5, 25, 125, 625, 3125, 15625, 78125, 390625, 1953125, 9765625, 48828125, 244140625, 1220703125};
    static final BigInteger[] bigTenPows = new BigInteger[32];
    static final BigInteger[] bigFivePows = new BigInteger[32];

    private Multiplication() {
    }

    static {
        long j = 1;
        int i = 0;
        while (i <= 18) {
            bigFivePows[i] = BigInteger.valueOf(j);
            bigTenPows[i] = BigInteger.valueOf(j << i);
            j *= 5;
            i++;
        }
        while (i < bigTenPows.length) {
            int i2 = i - 1;
            bigFivePows[i] = bigFivePows[i2].multiply(bigFivePows[1]);
            bigTenPows[i] = bigTenPows[i2].multiply(BigInteger.TEN);
            i++;
        }
    }

    static BigInteger multiplyByPositiveInt(BigInteger bigInteger, int i) {
        BigInt bigIntCopy = bigInteger.getBigInt().copy();
        bigIntCopy.multiplyByPositiveInt(i);
        return new BigInteger(bigIntCopy);
    }

    static BigInteger multiplyByTenPow(BigInteger bigInteger, long j) {
        if (j < tenPows.length) {
            return multiplyByPositiveInt(bigInteger, tenPows[(int) j]);
        }
        return bigInteger.multiply(powerOf10(j));
    }

    static BigInteger powerOf10(long j) {
        int i = (int) j;
        if (j < bigTenPows.length) {
            return bigTenPows[i];
        }
        if (j <= 50) {
            return BigInteger.TEN.pow(i);
        }
        try {
            if (j <= 2147483647L) {
                return bigFivePows[1].pow(i).shiftLeft(i);
            }
            BigInteger bigIntegerPow = bigFivePows[1].pow(Integer.MAX_VALUE);
            long j2 = j - 2147483647L;
            int i2 = (int) (j % 2147483647L);
            BigInteger bigIntegerMultiply = bigIntegerPow;
            for (long j3 = j2; j3 > 2147483647L; j3 -= 2147483647L) {
                bigIntegerMultiply = bigIntegerMultiply.multiply(bigIntegerPow);
            }
            BigInteger bigIntegerShiftLeft = bigIntegerMultiply.multiply(bigFivePows[1].pow(i2)).shiftLeft(Integer.MAX_VALUE);
            while (j2 > 2147483647L) {
                bigIntegerShiftLeft = bigIntegerShiftLeft.shiftLeft(Integer.MAX_VALUE);
                j2 -= 2147483647L;
            }
            return bigIntegerShiftLeft.shiftLeft(i2);
        } catch (OutOfMemoryError e) {
            throw new ArithmeticException(e.getMessage());
        }
    }

    static BigInteger multiplyByFivePow(BigInteger bigInteger, int i) {
        if (i < fivePows.length) {
            return multiplyByPositiveInt(bigInteger, fivePows[i]);
        }
        if (i < bigFivePows.length) {
            return bigInteger.multiply(bigFivePows[i]);
        }
        return bigInteger.multiply(bigFivePows[1].pow(i));
    }
}
