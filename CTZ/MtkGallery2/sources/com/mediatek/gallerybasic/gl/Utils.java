package com.mediatek.gallerybasic.gl;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.text.TextUtils;
import com.mediatek.gallerybasic.util.Log;
import java.io.Closeable;
import java.io.IOException;
import java.io.InterruptedIOException;
import mf.org.apache.xerces.impl.xpath.XPath;

class Utils {
    private static final String DEBUG_TAG = "GalleryDebug";
    private static final long INITIALCRC = -1;
    private static final boolean IS_DEBUG_BUILD;
    private static final String MASK_STRING = "********************************";
    private static final long POLY64REV = -7661587058870466123L;
    private static final String TAG = "MtkGallery2/Utils";
    private static long[] sCrcTable = new long[256];

    Utils() {
    }

    static {
        IS_DEBUG_BUILD = Build.TYPE.equals("eng") || Build.TYPE.equals("userdebug");
        for (int i = 0; i < 256; i++) {
            long j = i;
            for (int i2 = 0; i2 < 8; i2++) {
                j = (j >> 1) ^ ((((int) j) & 1) != 0 ? POLY64REV : 0L);
            }
            sCrcTable[i] = j;
        }
    }

    public static void assertTrue(boolean z) {
        if (!z) {
            throw new AssertionError();
        }
    }

    public static void fail(String str, Object... objArr) {
        if (objArr.length != 0) {
            str = String.format(str, objArr);
        }
        throw new AssertionError(str);
    }

    public static <T> T checkNotNull(T t) {
        if (t == null) {
            throw new NullPointerException();
        }
        return t;
    }

    public static boolean equals(Object obj, Object obj2) {
        return obj == obj2 || (obj != null && obj.equals(obj2));
    }

    public static int nextPowerOf2(int i) {
        if (i <= 0 || i > 1073741824) {
            throw new IllegalArgumentException("n is invalid: " + i);
        }
        int i2 = i - 1;
        int i3 = i2 | (i2 >> 16);
        int i4 = i3 | (i3 >> 8);
        int i5 = i4 | (i4 >> 4);
        int i6 = i5 | (i5 >> 2);
        return (i6 | (i6 >> 1)) + 1;
    }

    public static int prevPowerOf2(int i) {
        if (i <= 0) {
            throw new IllegalArgumentException();
        }
        return Integer.highestOneBit(i);
    }

    public static int clamp(int i, int i2, int i3) {
        return i > i3 ? i3 : i < i2 ? i2 : i;
    }

    public static float clamp(float f, float f2, float f3) {
        return f > f3 ? f3 : f < f2 ? f2 : f;
    }

    public static long clamp(long j, long j2, long j3) {
        return j > j3 ? j3 : j < j2 ? j2 : j;
    }

    public static boolean isOpaque(int i) {
        return (i >>> 24) == 255;
    }

    public static void swap(int[] iArr, int i, int i2) {
        int i3 = iArr[i];
        iArr[i] = iArr[i2];
        iArr[i2] = i3;
    }

    public static final long crc64Long(String str) {
        if (str == null || str.length() == 0) {
            return 0L;
        }
        return crc64Long(getBytes(str));
    }

    public static final long crc64Long(byte[] bArr) {
        long j = -1;
        for (byte b : bArr) {
            j = (j >> 8) ^ sCrcTable[(((int) j) ^ b) & 255];
        }
        return j;
    }

    public static byte[] getBytes(String str) {
        byte[] bArr = new byte[str.length() * 2];
        int i = 0;
        for (char c : str.toCharArray()) {
            int i2 = i + 1;
            bArr[i] = (byte) (c & 255);
            i = i2 + 1;
            bArr[i2] = (byte) (c >> '\b');
        }
        return bArr;
    }

    public static void closeSilently(Closeable closeable) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (IOException e) {
            Log.w(TAG, "<closeSilently> fail to close Closeable", e);
        }
    }

    public static int compare(long j, long j2) {
        if (j < j2) {
            return -1;
        }
        return j == j2 ? 0 : 1;
    }

    public static int ceilLog2(float f) {
        int i = 0;
        while (i < 31 && (1 << i) < f) {
            i++;
        }
        return i;
    }

    public static int floorLog2(float f) {
        int i = 0;
        while (i < 31 && (1 << i) <= f) {
            i++;
        }
        return i - 1;
    }

    public static void closeSilently(ParcelFileDescriptor parcelFileDescriptor) {
        if (parcelFileDescriptor != null) {
            try {
                parcelFileDescriptor.close();
            } catch (IOException e) {
                Log.w(TAG, "<closeSilently> fail to close fd", e);
            }
        }
    }

    public static void closeSilently(Cursor cursor) {
        if (cursor != null) {
            cursor.close();
        }
    }

    public static float interpolateAngle(float f, float f2, float f3) {
        float f4 = f2 - f;
        if (f4 < 0.0f) {
            f4 += 360.0f;
        }
        if (f4 > 180.0f) {
            f4 -= 360.0f;
        }
        float f5 = f + (f4 * f3);
        return f5 < 0.0f ? f5 + 360.0f : f5;
    }

    public static float interpolateScale(float f, float f2, float f3) {
        return f + (f3 * (f2 - f));
    }

    public static String ensureNotNull(String str) {
        return str == null ? "" : str;
    }

    public static float parseFloatSafely(String str, float f) {
        if (str == null) {
            return f;
        }
        try {
            return Float.parseFloat(str);
        } catch (NumberFormatException e) {
            return f;
        }
    }

    public static int parseIntSafely(String str, int i) {
        if (str == null) {
            return i;
        }
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException e) {
            return i;
        }
    }

    public static boolean isNullOrEmpty(String str) {
        return TextUtils.isEmpty(str);
    }

    public static void waitWithoutInterrupt(Object obj) {
        try {
            obj.wait();
        } catch (InterruptedException e) {
            Log.w(TAG, "<waitWithoutInterrupt> unexpected interrupt: " + obj);
        }
    }

    public static boolean handleInterrruptedException(Throwable th) {
        if ((th instanceof InterruptedIOException) || (th instanceof InterruptedException)) {
            Thread.currentThread().interrupt();
            return true;
        }
        return false;
    }

    public static String escapeXml(String str) {
        StringBuilder sb = new StringBuilder();
        int length = str.length();
        for (int i = 0; i < length; i++) {
            char cCharAt = str.charAt(i);
            if (cCharAt != '\"') {
                if (cCharAt != '<') {
                    if (cCharAt == '>') {
                        sb.append("&gt;");
                    } else {
                        switch (cCharAt) {
                            case XPath.Tokens.EXPRTOKEN_AXISNAME_DESCENDANT_OR_SELF:
                                sb.append("&amp;");
                                break;
                            case XPath.Tokens.EXPRTOKEN_AXISNAME_FOLLOWING:
                                sb.append("&#039;");
                                break;
                            default:
                                sb.append(cCharAt);
                                break;
                        }
                    }
                } else {
                    sb.append("&lt;");
                }
            } else {
                sb.append("&quot;");
            }
        }
        return sb.toString();
    }

    public static String getUserAgent(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return String.format("%s/%s; %s/%s/%s/%s; %s/%s/%s", packageInfo.packageName, packageInfo.versionName, Build.BRAND, Build.DEVICE, Build.MODEL, Build.ID, Integer.valueOf(Build.VERSION.SDK_INT), Build.VERSION.RELEASE, Build.VERSION.INCREMENTAL);
        } catch (PackageManager.NameNotFoundException e) {
            throw new IllegalStateException("getPackageInfo failed");
        }
    }

    public static String[] copyOf(String[] strArr, int i) {
        String[] strArr2 = new String[i];
        System.arraycopy(strArr, 0, strArr2, 0, Math.min(strArr.length, i));
        return strArr2;
    }

    public static String maskDebugInfo(Object obj) {
        if (obj == null) {
            return null;
        }
        String string = obj.toString();
        return IS_DEBUG_BUILD ? string : MASK_STRING.substring(0, Math.min(string.length(), MASK_STRING.length()));
    }

    public static void debug(String str, Object... objArr) {
        Log.v(DEBUG_TAG, "<debug> " + String.format(str, objArr));
    }
}
