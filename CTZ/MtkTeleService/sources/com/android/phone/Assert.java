package com.android.phone;

import android.os.Looper;

public class Assert {
    private static Boolean sIsMainThreadForTest;

    public static void isTrue(boolean z) {
        if (!z) {
            throw new AssertionError("Expected condition to be true");
        }
    }

    public static void isMainThread() {
        if (sIsMainThreadForTest != null) {
            isTrue(sIsMainThreadForTest.booleanValue());
        } else {
            isTrue(Looper.getMainLooper().equals(Looper.myLooper()));
        }
    }

    public static void isNotMainThread() {
        if (sIsMainThreadForTest != null) {
            isTrue(!sIsMainThreadForTest.booleanValue());
        } else {
            isTrue(!Looper.getMainLooper().equals(Looper.myLooper()));
        }
    }

    public static void fail() {
        fail("Fail");
    }

    public static void fail(String str) {
        throw new AssertionError(str);
    }

    public static void setIsMainThreadForTesting(Boolean bool) {
        sIsMainThreadForTest = bool;
    }
}
