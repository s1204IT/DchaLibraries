package com.android.gallery3d.common;

import android.database.Cursor;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import java.io.Closeable;
import java.io.IOException;

public class Utils {
    private static final boolean IS_DEBUG_BUILD;
    private static long[] sCrcTable = new long[NotificationCompat.FLAG_LOCAL_ONLY];

    static {
        IS_DEBUG_BUILD = Build.TYPE.equals("eng") || Build.TYPE.equals("userdebug");
        for (int i = 0; i < 256; i++) {
            long part = i;
            for (int j = 0; j < 8; j++) {
                long x = (((int) part) & 1) != 0 ? -7661587058870466123L : 0L;
                part = (part >> 1) ^ x;
            }
            sCrcTable[i] = part;
        }
    }

    public static void assertTrue(boolean cond) {
        if (!cond) {
            throw new AssertionError();
        }
    }

    public static void fail(String message, Object... args) {
        if (args.length != 0) {
            message = String.format(message, args);
        }
        throw new AssertionError(message);
    }

    public static <T> T checkNotNull(T object) {
        if (object == null) {
            throw new NullPointerException();
        }
        return object;
    }

    public static boolean equals(Object a, Object b) {
        return a == b || (a != null && a.equals(b));
    }

    public static int nextPowerOf2(int n) {
        if (n <= 0 || n > 1073741824) {
            throw new IllegalArgumentException("n is invalid: " + n);
        }
        int n2 = n - 1;
        int n3 = n2 | (n2 >> 16);
        int n4 = n3 | (n3 >> 8);
        int n5 = n4 | (n4 >> 4);
        int n6 = n5 | (n5 >> 2);
        return (n6 | (n6 >> 1)) + 1;
    }

    public static int prevPowerOf2(int n) {
        if (n <= 0) {
            throw new IllegalArgumentException();
        }
        return Integer.highestOneBit(n);
    }

    public static int clamp(int x, int min, int max) {
        if (x > max) {
            return max;
        }
        return x < min ? min : x;
    }

    public static float clamp(float x, float min, float max) {
        if (x > max) {
            return max;
        }
        return x < min ? min : x;
    }

    public static long clamp(long x, long min, long max) {
        if (x > max) {
            return max;
        }
        return x < min ? min : x;
    }

    public static boolean isOpaque(int color) {
        return (color >>> 24) == 255;
    }

    public static void swap(int[] array, int i, int j) {
        int temp = array[i];
        array[i] = array[j];
        array[j] = temp;
    }

    public static final long crc64Long(String in) {
        if (in == null || in.length() == 0) {
            return 0L;
        }
        return crc64Long(getBytes(in));
    }

    public static final long crc64Long(byte[] buffer) {
        long crc = -1;
        for (byte b : buffer) {
            crc = sCrcTable[(((int) crc) ^ b) & 255] ^ (crc >> 8);
        }
        return crc;
    }

    public static byte[] getBytes(String in) {
        byte[] result = new byte[in.length() * 2];
        char[] arr$ = in.toCharArray();
        int output = 0;
        for (char ch : arr$) {
            int output2 = output + 1;
            result[output] = (byte) (ch & 255);
            output = output2 + 1;
            result[output2] = (byte) (ch >> '\b');
        }
        return result;
    }

    public static void closeSilently(Closeable c) {
        if (c != null) {
            try {
                c.close();
            } catch (IOException t) {
                Log.w("Utils", "close fail ", t);
            }
        }
    }

    public static int compare(long a, long b) {
        if (a < b) {
            return -1;
        }
        return a == b ? 0 : 1;
    }

    public static int ceilLog2(float value) {
        int i = 0;
        while (i < 31 && (1 << i) < value) {
            i++;
        }
        return i;
    }

    public static int floorLog2(float value) {
        int i = 0;
        while (i < 31 && (1 << i) <= value) {
            i++;
        }
        return i - 1;
    }

    public static void closeSilently(ParcelFileDescriptor fd) {
        if (fd != null) {
            try {
                fd.close();
            } catch (Throwable t) {
                Log.w("Utils", "fail to close", t);
            }
        }
    }

    public static void closeSilently(Cursor cursor) {
        if (cursor != null) {
            try {
                cursor.close();
            } catch (Throwable t) {
                Log.w("Utils", "fail to close", t);
            }
        }
    }

    public static String ensureNotNull(String value) {
        return value == null ? "" : value;
    }

    public static void waitWithoutInterrupt(Object object) {
        try {
            object.wait();
        } catch (InterruptedException e) {
            Log.w("Utils", "unexpected interrupt: " + object);
        }
    }

    public static String maskDebugInfo(Object info) {
        if (info == null) {
            return null;
        }
        String s = info.toString();
        int length = Math.min(s.length(), "********************************".length());
        return !IS_DEBUG_BUILD ? "********************************".substring(0, length) : s;
    }
}
