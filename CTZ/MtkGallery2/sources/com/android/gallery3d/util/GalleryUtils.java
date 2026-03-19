package com.android.gallery3d.util;

import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.net.Uri;
import android.os.BenesseExtension;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.view.WindowManager;
import android.widget.Toast;
import com.android.gallery3d.R;
import com.android.gallery3d.app.GalleryActivity;
import com.android.gallery3d.app.PackagesMonitor;
import com.android.gallery3d.common.ApiHelper;
import com.android.gallery3d.data.MediaItem;
import com.android.gallery3d.glrenderer.TiledTexture;
import com.android.gallery3d.ui.TiledScreenNail;
import com.mediatek.gallery3d.util.FeatureConfig;
import com.mediatek.gallerybasic.util.DecodeSpecLimitor;
import com.mediatek.galleryportable.SystemPropertyUtils;
import java.io.File;
import java.util.Arrays;
import java.util.Locale;
import mf.org.apache.xerces.impl.xs.SchemaSymbols;

public class GalleryUtils {
    private static boolean sCameraAvailable;
    private static volatile Thread sCurrentThread;
    public static int sRealResolutionMaxSize;
    private static volatile boolean sWarned;
    private static float sPixelDensity = -1.0f;
    private static boolean sCameraAvailableInitialized = false;

    public static void initialize(Context context) {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        WindowManager windowManager = (WindowManager) context.getSystemService("window");
        windowManager.getDefaultDisplay().getMetrics(displayMetrics);
        sPixelDensity = displayMetrics.density;
        Resources resources = context.getResources();
        TiledScreenNail.setPlaceholderColor(resources.getColor(R.color.bitmap_screennail_placeholder));
        initializeThumbnailSizes(displayMetrics, resources);
        DisplayMetrics displayMetrics2 = new DisplayMetrics();
        if (Build.VERSION.SDK_INT >= 17) {
            windowManager.getDefaultDisplay().getRealMetrics(displayMetrics2);
        } else {
            windowManager.getDefaultDisplay().getMetrics(displayMetrics2);
        }
        sRealResolutionMaxSize = Math.max(displayMetrics2.widthPixels, displayMetrics2.heightPixels);
        com.mediatek.gallery3d.util.Log.d("Gallery2/GalleryUtils", "<initialize> device width " + displayMetrics2.widthPixels + " height " + displayMetrics2.heightPixels);
        initializeTiledTxtureSize(displayMetrics);
        boolean zIsLowRamDevice = isLowRamDevice(context);
        DecodeSpecLimitor.sIsLowRamDevice = zIsLowRamDevice;
        FeatureConfig.sIsLowRamDevice = zIsLowRamDevice;
        com.mediatek.gallery3d.util.Log.d("Gallery2/GalleryUtils", "<initialize> sIsLowRamDevice = " + FeatureConfig.sIsLowRamDevice);
        MediaItem.sHighQualityThumbnailSize = sRealResolutionMaxSize;
    }

    private static void initializeThumbnailSizes(DisplayMetrics displayMetrics, Resources resources) {
        int iMax = Math.max(displayMetrics.heightPixels, displayMetrics.widthPixels);
        int i = iMax / 2;
        MediaItem.setThumbnailSizes(i, iMax / 5);
        TiledScreenNail.setMaxSide(i);
    }

    public static float[] intColorToFloatARGBArray(int i) {
        return new float[]{Color.alpha(i) / 255.0f, Color.red(i) / 255.0f, Color.green(i) / 255.0f, Color.blue(i) / 255.0f};
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

    public static void setRenderThread() {
        sCurrentThread = Thread.currentThread();
    }

    public static void assertNotInRenderThread() {
        if (!sWarned && Thread.currentThread() == sCurrentThread) {
            sWarned = true;
            com.mediatek.gallery3d.util.Log.w("Gallery2/GalleryUtils", new Throwable("Should not do this in render thread"));
        }
    }

    public static double fastDistanceMeters(double d, double d2, double d3, double d4) {
        double d5 = d - d3;
        if (Math.abs(d5) <= 0.017453292519943295d) {
            double d6 = d2 - d4;
            if (Math.abs(d6) <= 0.017453292519943295d) {
                double dCos = Math.cos((d + d3) / 2.0d);
                return 6367000.0d * Math.sqrt((d5 * d5) + (dCos * dCos * d6 * d6));
            }
        }
        return accurateDistanceMeters(d, d2, d3, d4);
    }

    public static double accurateDistanceMeters(double d, double d2, double d3, double d4) {
        double dSin = Math.sin((d3 - d) * 0.5d);
        double dSin2 = Math.sin(0.5d * (d4 - d2));
        double dCos = (dSin * dSin) + (dSin2 * dSin2 * Math.cos(d) * Math.cos(d3));
        return 2.0d * Math.atan2(Math.sqrt(dCos), Math.sqrt(Math.max(0.0d, 1.0d - dCos))) * 6367000.0d;
    }

    public static final double toMile(double d) {
        return d / 1609.0d;
    }

    public static boolean isEditorAvailable(Context context, String str) {
        int packagesVersion = PackagesMonitor.getPackagesVersion(context);
        String str2 = "editor-update-" + str;
        String str3 = "has-editor-" + str;
        SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        if (defaultSharedPreferences.getInt(str2, 0) != packagesVersion) {
            defaultSharedPreferences.edit().putInt(str2, packagesVersion).putBoolean(str3, !context.getPackageManager().queryIntentActivities(new Intent("android.intent.action.EDIT").setType(str), 0).isEmpty()).commit();
        }
        return defaultSharedPreferences.getBoolean(str3, true);
    }

    public static boolean isCameraAvailable(Context context) {
        if (sCameraAvailableInitialized) {
            return sCameraAvailable;
        }
        int componentEnabledSetting = context.getPackageManager().getComponentEnabledSetting(new ComponentName(context, "com.android.camera.CameraLauncher"));
        boolean z = true;
        sCameraAvailableInitialized = true;
        if (componentEnabledSetting != 0 && componentEnabledSetting != 1) {
            z = false;
        }
        sCameraAvailable = z;
        return sCameraAvailable;
    }

    public static void startCameraActivity(Context context) {
        try {
            context.startActivity(new Intent("android.media.action.STILL_IMAGE_CAMERA").setFlags(335577088));
        } catch (ActivityNotFoundException e) {
            com.mediatek.gallery3d.util.Log.e("Gallery2/GalleryUtils", "Camera activity previously detected but cannot be found", e);
        }
    }

    public static void startGalleryActivity(Context context) {
        context.startActivity(new Intent(context, (Class<?>) GalleryActivity.class).setFlags(335544320));
    }

    public static boolean isValidLocation(double d, double d2) {
        return (d == 0.0d && d2 == 0.0d) ? false : true;
    }

    public static String formatLatitudeLongitude(String str, double d, double d2) {
        return String.format(Locale.ENGLISH, str, Double.valueOf(d), Double.valueOf(d2));
    }

    public static void showOnMap(final Context context, double d, double d2) {
        if (BenesseExtension.getDchaState() != 0) {
            return;
        }
        try {
            String latitudeLongitude = formatLatitudeLongitude("http://maps.google.com/maps?f=q&q=(%f,%f)", d, d2);
            context.startActivity(new Intent("android.intent.action.VIEW", Uri.parse(latitudeLongitude)).setComponent(new ComponentName("com.google.android.apps.maps", "com.google.android.maps.MapsActivity")));
        } catch (ActivityNotFoundException e) {
            com.mediatek.gallery3d.util.Log.e("Gallery2/GalleryUtils", "GMM activity not found!", e);
            Intent intent = new Intent("android.intent.action.VIEW", Uri.parse(formatLatitudeLongitude("geo:%f,%f", d, d2)));
            if (context.getPackageManager().queryIntentActivities(intent, 0).isEmpty() && (context instanceof GalleryActivity)) {
                context.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(context, R.string.m_no_map_tip, 1).show();
                    }
                });
            } else {
                context.startActivity(intent);
            }
        }
    }

    public static void setViewPointMatrix(float[] fArr, float f, float f2, float f3) {
        Arrays.fill(fArr, 0, 16, 0.0f);
        float f4 = -f3;
        fArr[15] = f4;
        fArr[5] = f4;
        fArr[0] = f4;
        fArr[8] = f;
        fArr[9] = f2;
        fArr[11] = 1.0f;
        fArr[10] = 1.0f;
    }

    public static int getBucketId(String str) {
        return str.toLowerCase(Locale.ENGLISH).hashCode();
    }

    public static String searchDirForPath(File file, int i) {
        File[] fileArrListFiles = file.listFiles();
        if (fileArrListFiles != null) {
            for (File file2 : fileArrListFiles) {
                if (file2.isDirectory()) {
                    String absolutePath = file2.getAbsolutePath();
                    if (getBucketId(absolutePath) == i) {
                        return absolutePath;
                    }
                    String strSearchDirForPath = searchDirForPath(file2, i);
                    if (strSearchDirForPath != null) {
                        return strSearchDirForPath;
                    }
                }
            }
            return null;
        }
        return null;
    }

    public static String formatDuration(Context context, int i) {
        int i2 = i / 3600;
        int i3 = i2 * 3600;
        int i4 = (i - i3) / 60;
        int i5 = i - (i3 + (i4 * 60));
        return i2 == 0 ? String.format(context.getString(R.string.details_ms), Integer.valueOf(i4), Integer.valueOf(i5)) : String.format(context.getString(R.string.details_hms), Integer.valueOf(i2), Integer.valueOf(i4), Integer.valueOf(i5));
    }

    @TargetApi(11)
    public static int determineTypeBits(Context context, Intent intent) {
        String strResolveType = intent.resolveType(context);
        int i = 3;
        if (!"*/*".equals(strResolveType)) {
            if ("image/*".equals(strResolveType) || "vnd.android.cursor.dir/image".equals(strResolveType)) {
                i = 1;
            } else if ("video/*".equals(strResolveType) || "vnd.android.cursor.dir/video".equals(strResolveType)) {
                i = 2;
            }
        }
        if (ApiHelper.HAS_INTENT_EXTRA_LOCAL_ONLY && intent.getBooleanExtra("android.intent.extra.LOCAL_ONLY", false)) {
            return i | 4;
        }
        return i;
    }

    public static int getSelectionModePrompt(int i) {
        if ((i & 2) != 0) {
            if ((i & 1) == 0) {
                return R.string.select_video;
            }
            return R.string.select_item;
        }
        return R.string.select_image;
    }

    private static void initializeTiledTxtureSize(DisplayMetrics displayMetrics) {
        if (Math.max(displayMetrics.heightPixels, displayMetrics.widthPixels) > 1600) {
            TiledTexture.TILE_SIZE = 512;
            TiledTexture.CONTENT_SIZE = 510;
        }
    }

    private static boolean isLowRamDevice(Context context) {
        if (Build.VERSION.SDK_INT >= 19) {
            return ((ActivityManager) context.getSystemService("activity")).isLowRamDevice();
        }
        return SchemaSymbols.ATTVAL_TRUE.equals(SystemPropertyUtils.get("ro.config.low_ram", SchemaSymbols.ATTVAL_FALSE));
    }
}
