package com.google.common.math;

final class MathPreconditions {
    static int checkPositive(String str, int i) {
        if (i <= 0) {
            throw new IllegalArgumentException(str + " (" + i + ") must be > 0");
        }
        return i;
    }

    static long checkPositive(String str, long j) {
        if (j <= 0) {
            throw new IllegalArgumentException(str + " (" + j + ") must be > 0");
        }
        return j;
    }

    static int checkNonNegative(String str, int i) {
        if (i < 0) {
            throw new IllegalArgumentException(str + " (" + i + ") must be >= 0");
        }
        return i;
    }

    static long checkNonNegative(String str, long j) {
        if (j < 0) {
            throw new IllegalArgumentException(str + " (" + j + ") must be >= 0");
        }
        return j;
    }

    static void checkRoundingUnnecessary(boolean z) {
        if (!z) {
            throw new ArithmeticException("mode was UNNECESSARY, but rounding was necessary");
        }
    }

    static void checkNoOverflow(boolean z) {
        if (!z) {
            throw new ArithmeticException("overflow");
        }
    }
}
