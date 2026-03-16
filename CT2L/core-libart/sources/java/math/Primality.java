package java.math;

import dalvik.bytecode.Opcodes;
import java.net.HttpURLConnection;
import java.util.Arrays;

class Primality {
    private static final int[] primes = {2, 3, 5, 7, 11, 13, 17, 19, 23, 29, 31, 37, 41, 43, 47, 53, 59, 61, 67, 71, 73, 79, 83, 89, 97, Opcodes.OP_SGET_CHAR, Opcodes.OP_SPUT, Opcodes.OP_SPUT_BYTE, Opcodes.OP_SPUT_SHORT, Opcodes.OP_INVOKE_STATIC, 127, Opcodes.OP_INT_TO_DOUBLE, Opcodes.OP_FLOAT_TO_DOUBLE, Opcodes.OP_DOUBLE_TO_LONG, Opcodes.OP_AND_INT, Opcodes.OP_XOR_INT, Opcodes.OP_MUL_LONG, Opcodes.OP_SHL_LONG, Opcodes.OP_SUB_FLOAT, Opcodes.OP_MUL_DOUBLE, Opcodes.OP_DIV_INT_2ADDR, Opcodes.OP_AND_INT_2ADDR, Opcodes.OP_REM_LONG_2ADDR, Opcodes.OP_OR_LONG_2ADDR, Opcodes.OP_USHR_LONG_2ADDR, Opcodes.OP_SUB_FLOAT_2ADDR, Opcodes.OP_DIV_INT_LIT16, Opcodes.OP_XOR_INT_LIT8, 227, 229, Opcodes.OP_IPUT_WIDE_VOLATILE, Opcodes.OP_EXECUTE_INLINE_RANGE, 241, Opcodes.OP_INVOKE_SUPER_QUICK_RANGE, 257, 263, 269, 271, 277, 281, 283, 293, 307, 311, 313, 317, 331, 337, 347, 349, 353, 359, 367, 373, 379, 383, 389, 397, HttpURLConnection.HTTP_UNAUTHORIZED, HttpURLConnection.HTTP_CONFLICT, 419, 421, 431, 433, 439, 443, 449, 457, 461, 463, 467, 479, 487, 491, 499, HttpURLConnection.HTTP_UNAVAILABLE, 509, 521, 523, 541, 547, 557, 563, 569, 571, 577, 587, 593, 599, 601, 607, 613, 617, 619, 631, 641, 643, 647, 653, 659, 661, 673, 677, 683, 691, 701, 709, 719, 727, 733, 739, 743, 751, 757, 761, 769, 773, 787, 797, 809, 811, 821, 823, 827, 829, 839, 853, 857, 859, 863, 877, 881, 883, 887, 907, 911, 919, 929, 937, 941, 947, 953, 967, 971, 977, 983, 991, 997, 1009, 1013, 1019, 1021};
    private static final BigInteger[] BIprimes = new BigInteger[primes.length];

    private Primality() {
    }

    static {
        for (int i = 0; i < primes.length; i++) {
            BIprimes[i] = BigInteger.valueOf(primes[i]);
        }
    }

    static BigInteger nextProbablePrime(BigInteger n) {
        int l;
        int[] modules = new int[primes.length];
        boolean[] isDivisible = new boolean[1024];
        BigInt ni = n.getBigInt();
        if (ni.bitLength() <= 10 && (l = (int) ni.longInt()) < primes[primes.length - 1]) {
            int i = 0;
            while (l >= primes[i]) {
                i++;
            }
            return BIprimes[i];
        }
        BigInt startPoint = ni.copy();
        BigInt probPrime = new BigInt();
        startPoint.addPositiveInt(BigInt.remainderByPositiveInt(ni, 2) + 1);
        for (int i2 = 0; i2 < primes.length; i2++) {
            modules[i2] = BigInt.remainderByPositiveInt(startPoint, primes[i2]) - 1024;
        }
        while (true) {
            Arrays.fill(isDivisible, false);
            for (int i3 = 0; i3 < primes.length; i3++) {
                modules[i3] = (modules[i3] + 1024) % primes[i3];
                for (int j = modules[i3] == 0 ? 0 : primes[i3] - modules[i3]; j < 1024; j += primes[i3]) {
                    isDivisible[j] = true;
                }
            }
            for (int j2 = 0; j2 < 1024; j2++) {
                if (!isDivisible[j2]) {
                    probPrime.putCopy(startPoint);
                    probPrime.addPositiveInt(j2);
                    if (probPrime.isPrime(100)) {
                        return new BigInteger(probPrime);
                    }
                }
            }
            startPoint.addPositiveInt(1024);
        }
    }
}
