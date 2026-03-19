package com.mediatek.gallerybasic.util;

import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.util.DisplayMetrics;
import android.view.WindowManager;
import java.io.BufferedReader;
import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.regex.Pattern;

public class Utils {
    public static final boolean HAS_GLES20_REQUIRED;
    public static final int SIZE_SCHEME_FILE = 7;
    public static final String SPECIAL_CHARACTERS = "[#&$*^@!%]";
    private static final String TAG = "MtkGallery2/Utils";
    private static long sDeviceRam;
    private static float sPixelDensity = -1.0f;

    public interface VERSION_CODES {
        public static final int GINGERBREAD_MR1 = 10;
        public static final int HONEYCOMB = 11;
        public static final int HONEYCOMB_MR1 = 12;
        public static final int HONEYCOMB_MR2 = 13;
        public static final int ICE_CREAM_SANDWICH = 14;
        public static final int ICE_CREAM_SANDWICH_MR1 = 15;
        public static final int JELLY_BEAN = 16;
        public static final int JELLY_BEAN_MR1 = 17;
        public static final int JELLY_BEAN_MR2 = 18;
    }

    static {
        HAS_GLES20_REQUIRED = Build.VERSION.SDK_INT >= 11;
        sDeviceRam = -1L;
    }

    public static void initialize(Context context) {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        ((WindowManager) context.getSystemService("window")).getDefaultDisplay().getMetrics(displayMetrics);
        sPixelDensity = displayMetrics.density;
    }

    public static void waitWithoutInterrupt(Object obj) {
        try {
            obj.wait();
        } catch (InterruptedException e) {
            Log.w(TAG, "<waitWithoutInterrupt> unexpected interrupt: " + obj);
        }
    }

    public static void assertTrue(boolean z) {
        if (!z) {
            throw new AssertionError();
        }
    }

    public static <T> T checkNotNull(T t) {
        if (t == null) {
            throw new NullPointerException();
        }
        return t;
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

    public static void closeSilently(ParcelFileDescriptor parcelFileDescriptor) {
        if (parcelFileDescriptor != null) {
            try {
                parcelFileDescriptor.close();
            } catch (IOException e) {
                Log.w(TAG, "<closeSilently> fail to close ParcelFileDescriptor", e);
            }
        }
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

    public static float dpToPixel(float f) {
        return sPixelDensity * f;
    }

    public static int dpToPixel(int i) {
        return Math.round(dpToPixel(i));
    }

    public static int meterToPixel(float f) {
        return Math.round(dpToPixel(f * 39.37f * 160.0f));
    }

    public static String decodePath(String str) {
        try {
            return URLDecoder.decode(str, "utf-8");
        } catch (UnsupportedEncodingException e) {
            Log.d(TAG, "<decodePath> fail");
            e.printStackTrace();
            return null;
        }
    }

    public static String encodePath(String str) {
        try {
            return URLEncoder.encode(str, "utf-8");
        } catch (UnsupportedEncodingException e) {
            Log.w(TAG, "<encodePath> fail");
            e.printStackTrace();
            return null;
        }
    }

    public static long getDeviceRam() throws Throwable {
        ?? bufferedReader = -1;
        bufferedReader = -1;
        if (sDeviceRam != -1) {
            return sDeviceRam;
        }
        String str = null;
        str = null;
        str = null;
        str = null;
        str = null;
        ?? r1 = 0;
        try {
        } catch (Throwable th) {
            th = th;
            r1 = bufferedReader;
        }
        try {
            try {
                bufferedReader = new BufferedReader(new FileReader("/proc/meminfo"), 8);
            } catch (IOException e) {
                e.printStackTrace();
                bufferedReader = bufferedReader;
            }
            try {
                String line = bufferedReader.readLine();
                str = line != null ? line : null;
                bufferedReader.close();
                bufferedReader = bufferedReader;
            } catch (FileNotFoundException e2) {
                e = e2;
                e.printStackTrace();
                if (bufferedReader != 0) {
                    bufferedReader.close();
                    bufferedReader = bufferedReader;
                }
                if (str != null) {
                }
                return sDeviceRam;
            } catch (IOException e3) {
                e = e3;
                e.printStackTrace();
                if (bufferedReader != 0) {
                    bufferedReader.close();
                    bufferedReader = bufferedReader;
                }
                if (str != null) {
                }
                return sDeviceRam;
            }
        } catch (FileNotFoundException e4) {
            e = e4;
            bufferedReader = 0;
        } catch (IOException e5) {
            e = e5;
            bufferedReader = 0;
        } catch (Throwable th2) {
            th = th2;
            if (r1 != 0) {
                try {
                    r1.close();
                } catch (IOException e6) {
                    e6.printStackTrace();
                }
            }
            throw th;
        }
        if (str != null) {
            sDeviceRam = Integer.parseInt(str.substring(str.indexOf(58) + 1, str.indexOf(107)).trim());
            Log.d(TAG, "<getDeviceRam> " + sDeviceRam + "KB");
        }
        return sDeviceRam;
    }

    public static boolean hasSpecialCharaters(Uri uri) {
        if (uri == null) {
            return false;
        }
        return Pattern.compile(SPECIAL_CHARACTERS).matcher(uri.toString()).find();
    }
}
