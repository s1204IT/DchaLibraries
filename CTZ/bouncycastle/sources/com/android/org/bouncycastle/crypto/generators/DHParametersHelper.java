package com.android.org.bouncycastle.crypto.generators;

import com.android.org.bouncycastle.math.ec.WNafUtil;
import com.android.org.bouncycastle.util.BigIntegers;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.logging.Logger;

class DHParametersHelper {
    private static final Logger logger = Logger.getLogger(DHParametersHelper.class.getName());
    private static final BigInteger ONE = BigInteger.valueOf(1);
    private static final BigInteger TWO = BigInteger.valueOf(2);

    DHParametersHelper() {
    }

    static BigInteger[] generateSafePrimes(int i, int i2, SecureRandom secureRandom) {
        logger.info("Generating safe primes. This may take a long time.");
        long jCurrentTimeMillis = System.currentTimeMillis();
        int i3 = i - 1;
        int i4 = i >>> 2;
        int i5 = 0;
        while (true) {
            i5++;
            BigInteger bigInteger = new BigInteger(i3, 2, secureRandom);
            BigInteger bigIntegerAdd = bigInteger.shiftLeft(1).add(ONE);
            if (bigIntegerAdd.isProbablePrime(i2) && (i2 <= 2 || bigInteger.isProbablePrime(i2 - 2))) {
                if (WNafUtil.getNafWeight(bigIntegerAdd) >= i4) {
                    long jCurrentTimeMillis2 = System.currentTimeMillis() - jCurrentTimeMillis;
                    logger.info("Generated safe primes: " + i5 + " tries took " + jCurrentTimeMillis2 + "ms");
                    return new BigInteger[]{bigIntegerAdd, bigInteger};
                }
            }
        }
    }

    static BigInteger selectGenerator(BigInteger bigInteger, BigInteger bigInteger2, SecureRandom secureRandom) {
        BigInteger bigIntegerModPow;
        BigInteger bigIntegerSubtract = bigInteger.subtract(TWO);
        do {
            bigIntegerModPow = BigIntegers.createRandomInRange(TWO, bigIntegerSubtract, secureRandom).modPow(TWO, bigInteger);
        } while (bigIntegerModPow.equals(ONE));
        return bigIntegerModPow;
    }
}
