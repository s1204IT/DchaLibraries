package com.android.internal.util;

import java.util.Collection;

public class Preconditions {
    public static <T> T checkNotNull(T reference) {
        if (reference == null) {
            throw new NullPointerException();
        }
        return reference;
    }

    public static <T> T checkNotNull(T reference, Object errorMessage) {
        if (reference == null) {
            throw new NullPointerException(String.valueOf(errorMessage));
        }
        return reference;
    }

    public static void checkState(boolean expression) {
        if (!expression) {
            throw new IllegalStateException();
        }
    }

    public static void checkFlagsArgument(int requestedFlags, int allowedFlags) {
        if ((requestedFlags & allowedFlags) != requestedFlags) {
            throw new IllegalArgumentException("Requested flags 0x" + Integer.toHexString(requestedFlags) + ", but only 0x" + Integer.toHexString(allowedFlags) + " are allowed");
        }
    }

    public static int checkArgumentNonnegative(int value, String errorMessage) {
        if (value < 0) {
            throw new IllegalArgumentException(errorMessage);
        }
        return value;
    }

    public static long checkArgumentNonnegative(long value, String errorMessage) {
        if (value < 0) {
            throw new IllegalArgumentException(errorMessage);
        }
        return value;
    }

    public static int checkArgumentPositive(int value, String errorMessage) {
        if (value <= 0) {
            throw new IllegalArgumentException(errorMessage);
        }
        return value;
    }

    public static float checkArgumentFinite(float value, String valueName) {
        if (Float.isNaN(value)) {
            throw new IllegalArgumentException(valueName + " must not be NaN");
        }
        if (Float.isInfinite(value)) {
            throw new IllegalArgumentException(valueName + " must not be infinite");
        }
        return value;
    }

    public static float checkArgumentInRange(float value, float lower, float upper, String valueName) {
        if (Float.isNaN(value)) {
            throw new IllegalArgumentException(valueName + " must not be NaN");
        }
        if (value < lower) {
            throw new IllegalArgumentException(String.format("%s is out of range of [%f, %f] (too low)", valueName, Float.valueOf(lower), Float.valueOf(upper)));
        }
        if (value > upper) {
            throw new IllegalArgumentException(String.format("%s is out of range of [%f, %f] (too high)", valueName, Float.valueOf(lower), Float.valueOf(upper)));
        }
        return value;
    }

    public static int checkArgumentInRange(int value, int lower, int upper, String valueName) {
        if (value < lower) {
            throw new IllegalArgumentException(String.format("%s is out of range of [%d, %d] (too low)", valueName, Integer.valueOf(lower), Integer.valueOf(upper)));
        }
        if (value > upper) {
            throw new IllegalArgumentException(String.format("%s is out of range of [%d, %d] (too high)", valueName, Integer.valueOf(lower), Integer.valueOf(upper)));
        }
        return value;
    }

    public static <T> T[] checkArrayElementsNotNull(T[] value, String valueName) {
        if (value == null) {
            throw new NullPointerException(valueName + " must not be null");
        }
        for (int i = 0; i < value.length; i++) {
            if (value[i] == null) {
                throw new NullPointerException(String.format("%s[%d] must not be null", valueName, Integer.valueOf(i)));
            }
        }
        return value;
    }

    public static <T> Collection<T> checkCollectionElementsNotNull(Collection<T> value, String valueName) {
        if (value == null) {
            throw new NullPointerException(valueName + " must not be null");
        }
        long ctr = 0;
        for (T elem : value) {
            if (elem == null) {
                throw new NullPointerException(String.format("%s[%d] must not be null", valueName, Long.valueOf(ctr)));
            }
            ctr++;
        }
        return value;
    }

    public static <T> Collection<T> checkCollectionNotEmpty(Collection<T> value, String valueName) {
        if (value == null) {
            throw new NullPointerException(valueName + " must not be null");
        }
        if (value.isEmpty()) {
            throw new IllegalArgumentException(valueName + " is empty");
        }
        return value;
    }

    public static float[] checkArrayElementsInRange(float[] value, float lower, float upper, String valueName) {
        checkNotNull(value, valueName + " must not be null");
        for (int i = 0; i < value.length; i++) {
            float v = value[i];
            if (Float.isNaN(v)) {
                throw new IllegalArgumentException(valueName + "[" + i + "] must not be NaN");
            }
            if (v < lower) {
                throw new IllegalArgumentException(String.format("%s[%d] is out of range of [%f, %f] (too low)", valueName, Integer.valueOf(i), Float.valueOf(lower), Float.valueOf(upper)));
            }
            if (v > upper) {
                throw new IllegalArgumentException(String.format("%s[%d] is out of range of [%f, %f] (too high)", valueName, Integer.valueOf(i), Float.valueOf(lower), Float.valueOf(upper)));
            }
        }
        return value;
    }
}
