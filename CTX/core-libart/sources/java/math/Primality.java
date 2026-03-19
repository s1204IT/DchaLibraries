package java.math;

import android.icu.impl.coll.CollationFastLatin;
import android.icu.lang.UCharacter;
import java.util.Arrays;

class Primality {
    private static final int[] primes = {2, 3, 5, 7, 11, 13, 17, 19, 23, 29, 31, 37, 41, 43, 47, 53, 59, 61, 67, 71, 73, 79, 83, 89, 97, 101, 103, 107, 109, 113, 127, 131, 137, 139, 149, 151, 157, 163, 167, 173, 179, 181, 191, 193, 197, 199, 211, 223, 227, 229, 233, 239, 241, 251, UCharacter.UnicodeBlock.EARLY_DYNASTIC_CUNEIFORM_ID, UCharacter.UnicodeBlock.ADLAM_ID, UCharacter.UnicodeBlock.MONGOLIAN_SUPPLEMENT_ID, UCharacter.UnicodeBlock.OSAGE_ID, UCharacter.UnicodeBlock.NUSHU_ID, UCharacter.UnicodeBlock.COUNT, 283, 293, 307, 311, 313, 317, 331, 337, 347, 349, 353, 359, 367, 373, 379, CollationFastLatin.LATIN_MAX, 389, 397, 401, 409, 419, 421, 431, 433, 439, 443, 449, 457, 461, 463, 467, 479, 487, 491, 499, 503, 509, 521, 523, 541, 547, 557, 563, 569, 571, 577, 587, 593, 599, 601, 607, 613, 617, 619, 631, 641, 643, 647, 653, 659, 661, 673, 677, 683, 691, 701, 709, 719, 727, 733, 739, 743, 751, 757, 761, 769, 773, 787, 797, 809, 811, 821, 823, 827, 829, 839, 853, 857, 859, 863, 877, 881, 883, 887, 907, 911, 919, 929, 937, 941, 947, 953, 967, 971, 977, 983, 991, 997, 1009, 1013, 1019, 1021};
    private static final BigInteger[] BIprimes = new BigInteger[primes.length];

    private Primality() {
    }

    static {
        for (int i = 0; i < primes.length; i++) {
            BIprimes[i] = BigInteger.valueOf(primes[i]);
        }
    }

    static BigInteger nextProbablePrime(BigInteger bigInteger) {
        int i;
        int iLongInt;
        int[] iArr = new int[primes.length];
        boolean[] zArr = new boolean[1024];
        BigInt bigInt = bigInteger.getBigInt();
        int i2 = 0;
        if (bigInt.bitLength() <= 10 && (iLongInt = (int) bigInt.longInt()) < primes[primes.length - 1]) {
            while (iLongInt >= primes[i2]) {
                i2++;
            }
            return BIprimes[i2];
        }
        BigInt bigIntCopy = bigInt.copy();
        BigInt bigInt2 = new BigInt();
        bigIntCopy.addPositiveInt(BigInt.remainderByPositiveInt(bigInt, 2) + 1);
        for (int i3 = 0; i3 < primes.length; i3++) {
            iArr[i3] = BigInt.remainderByPositiveInt(bigIntCopy, primes[i3]) - 1024;
        }
        while (true) {
            Arrays.fill(zArr, false);
            for (int i4 = 0; i4 < primes.length; i4++) {
                iArr[i4] = (iArr[i4] + 1024) % primes[i4];
                if (iArr[i4] != 0) {
                    i = primes[i4] - iArr[i4];
                } else {
                    i = 0;
                }
                while (i < 1024) {
                    zArr[i] = true;
                    i += primes[i4];
                }
            }
            for (int i5 = 0; i5 < 1024; i5++) {
                if (!zArr[i5]) {
                    bigInt2.putCopy(bigIntCopy);
                    bigInt2.addPositiveInt(i5);
                    if (bigInt2.isPrime(100)) {
                        return new BigInteger(bigInt2);
                    }
                }
            }
            bigIntCopy.addPositiveInt(1024);
        }
    }
}
