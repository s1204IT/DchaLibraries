package com.android.org.bouncycastle.crypto.generators;

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

    static BigInteger[] generateSafePrimes(int size, int certainty, SecureRandom random) {
        BigInteger q;
        BigInteger p;
        logger.info("Generating safe primes. This may take a long time.");
        long start = System.currentTimeMillis();
        int tries = 0;
        int qLength = size - 1;
        while (true) {
            tries++;
            q = new BigInteger(qLength, 2, random);
            p = q.shiftLeft(1).add(ONE);
            if (p.isProbablePrime(certainty) && (certainty <= 2 || q.isProbablePrime(certainty))) {
                break;
            }
        }
        long end = System.currentTimeMillis();
        long duration = end - start;
        logger.info("Generated safe primes: " + tries + " tries took " + duration + "ms");
        return new BigInteger[]{p, q};
    }

    static BigInteger selectGenerator(BigInteger p, BigInteger q, SecureRandom random) {
        BigInteger g;
        BigInteger pMinusTwo = p.subtract(TWO);
        do {
            BigInteger h = BigIntegers.createRandomInRange(TWO, pMinusTwo, random);
            g = h.modPow(TWO, p);
        } while (g.equals(ONE));
        return g;
    }
}
