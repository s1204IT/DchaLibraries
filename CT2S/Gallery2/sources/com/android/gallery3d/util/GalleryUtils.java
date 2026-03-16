package com.android.gallery3d.util;

import android.annotation.TargetApi;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.Color;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.view.WindowManager;
import com.android.gallery3d.R;
import com.android.gallery3d.app.GalleryActivity;
import com.android.gallery3d.app.PackagesMonitor;
import com.android.gallery3d.common.ApiHelper;
import com.android.gallery3d.data.MediaItem;
import com.android.gallery3d.ui.TiledScreenNail;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class GalleryUtils {
    private static boolean sCameraAvailable;
    private static volatile Thread sCurrentThread;
    private static volatile boolean sWarned;
    private static float sPixelDensity = -1.0f;
    private static boolean sCameraAvailableInitialized = false;

    public static void initialize(Context context) {
        DisplayMetrics metrics = new DisplayMetrics();
        WindowManager wm = (WindowManager) context.getSystemService("window");
        wm.getDefaultDisplay().getMetrics(metrics);
        sPixelDensity = metrics.density;
        Resources r = context.getResources();
        TiledScreenNail.setPlaceholderColor(r.getColor(R.color.bitmap_screennail_placeholder));
        initializeThumbnailSizes(metrics, r);
    }

    private static void initializeThumbnailSizes(DisplayMetrics metrics, Resources r) {
        int maxPixels = Math.max(metrics.heightPixels, metrics.widthPixels);
        MediaItem.setThumbnailSizes(maxPixels / 2, maxPixels / 5);
        TiledScreenNail.setMaxSide(maxPixels / 2);
    }

    public static float[] intColorToFloatARGBArray(int from) {
        return new float[]{Color.alpha(from) / 255.0f, Color.red(from) / 255.0f, Color.green(from) / 255.0f, Color.blue(from) / 255.0f};
    }

    public static float dpToPixel(float dp) {
        return sPixelDensity * dp;
    }

    public static int dpToPixel(int dp) {
        return Math.round(dpToPixel(dp));
    }

    public static int meterToPixel(float meter) {
        return Math.round(dpToPixel(39.37f * meter * 160.0f));
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

    public static void setRenderThread() {
        sCurrentThread = Thread.currentThread();
    }

    public static void assertNotInRenderThread() {
        if (!sWarned && Thread.currentThread() == sCurrentThread) {
            sWarned = true;
            android.util.Log.w("GalleryUtils", new Throwable("Should not do this in render thread"));
        }
    }

    public static double fastDistanceMeters(double latRad1, double lngRad1, double latRad2, double lngRad2) {
        if (Math.abs(latRad1 - latRad2) > 0.017453292519943295d || Math.abs(lngRad1 - lngRad2) > 0.017453292519943295d) {
            return accurateDistanceMeters(latRad1, lngRad1, latRad2, lngRad2);
        }
        double sineLat = latRad1 - latRad2;
        double sineLng = lngRad1 - lngRad2;
        double cosTerms = Math.cos((latRad1 + latRad2) / 2.0d);
        double trigTerm = (sineLat * sineLat) + (cosTerms * cosTerms * sineLng * sineLng);
        return 6367000.0d * Math.sqrt(trigTerm);
    }

    public static double accurateDistanceMeters(double lat1, double lng1, double lat2, double lng2) {
        double dlat = Math.sin(0.5d * (lat2 - lat1));
        double dlng = Math.sin(0.5d * (lng2 - lng1));
        double x = (dlat * dlat) + (dlng * dlng * Math.cos(lat1) * Math.cos(lat2));
        return 2.0d * Math.atan2(Math.sqrt(x), Math.sqrt(Math.max(0.0d, 1.0d - x))) * 6367000.0d;
    }

    public static final double toMile(double meter) {
        return meter / 1609.0d;
    }

    public static boolean isEditorAvailable(Context context, String mimeType) {
        int version = PackagesMonitor.getPackagesVersion(context);
        String updateKey = "editor-update-" + mimeType;
        String hasKey = "has-editor-" + mimeType;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if (prefs.getInt(updateKey, 0) != version) {
            PackageManager packageManager = context.getPackageManager();
            List<ResolveInfo> infos = packageManager.queryIntentActivities(new Intent("android.intent.action.EDIT").setType(mimeType), 0);
            prefs.edit().putInt(updateKey, version).putBoolean(hasKey, infos.isEmpty() ? false : true).commit();
        }
        return prefs.getBoolean(hasKey, true);
    }

    public static boolean isCameraAvailable(Context context) {
        if (sCameraAvailableInitialized) {
            return sCameraAvailable;
        }
        PackageManager pm = context.getPackageManager();
        Intent cameraIntent = IntentHelper.getCameraIntent(context);
        List<ResolveInfo> apps = pm.queryIntentActivities(cameraIntent, 0);
        sCameraAvailableInitialized = true;
        sCameraAvailable = apps.isEmpty() ? false : true;
        return sCameraAvailable;
    }

    public static void startCameraActivity(Context context) {
        Intent intent = new Intent("android.media.action.STILL_IMAGE_CAMERA").setFlags(335544320);
        try {
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            android.util.Log.e("GalleryUtils", "Camera activity previously detected but cannot be found", e);
        }
    }

    public static void startGalleryActivity(Context context) {
        Intent intent = new Intent(context, (Class<?>) GalleryActivity.class).setFlags(335544320);
        context.startActivity(intent);
    }

    public static boolean isValidLocation(double latitude, double longitude) {
        return (latitude == 0.0d && longitude == 0.0d) ? false : true;
    }

    public static String formatLatitudeLongitude(String format, double latitude, double longitude) {
        return String.format(Locale.ENGLISH, format, Double.valueOf(latitude), Double.valueOf(longitude));
    }

    public static void showOnMap(Context context, double latitude, double longitude) {
        try {
            String uri = formatLatitudeLongitude("http://maps.google.com/maps?f=q&q=(%f,%f)", latitude, longitude);
            ComponentName compName = new ComponentName("com.google.android.apps.maps", "com.google.android.maps.MapsActivity");
            Intent mapsIntent = new Intent("android.intent.action.VIEW", Uri.parse(uri)).setComponent(compName);
            context.startActivity(mapsIntent);
        } catch (ActivityNotFoundException e) {
            android.util.Log.e("GalleryUtils", "GMM activity not found!", e);
            String url = formatLatitudeLongitude("geo:%f,%f", latitude, longitude);
            Intent mapsIntent2 = new Intent("android.intent.action.VIEW", Uri.parse(url));
            context.startActivity(mapsIntent2);
        }
    }

    public static void setViewPointMatrix(float[] matrix, float x, float y, float z) {
        Arrays.fill(matrix, 0, 16, 0.0f);
        float f = -z;
        matrix[15] = f;
        matrix[5] = f;
        matrix[0] = f;
        matrix[8] = x;
        matrix[9] = y;
        matrix[11] = 1.0f;
        matrix[10] = 1.0f;
    }

    public static int getBucketId(String path) {
        return path.toLowerCase().hashCode();
    }

    public static String searchDirForPath(File dir, int bucketId) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    String path = file.getAbsolutePath();
                    if (getBucketId(path) != bucketId) {
                        String path2 = searchDirForPath(file, bucketId);
                        if (path2 != null) {
                            return path2;
                        }
                    } else {
                        return path;
                    }
                }
            }
        }
        return null;
    }

    public static String formatDuration(Context context, int duration) {
        int h = duration / 3600;
        int m = (duration - (h * 3600)) / 60;
        int s = duration - ((h * 3600) + (m * 60));
        if (h == 0) {
            String durationValue = String.format(context.getString(R.string.details_ms), Integer.valueOf(m), Integer.valueOf(s));
            return durationValue;
        }
        String durationValue2 = String.format(context.getString(R.string.details_hms), Integer.valueOf(h), Integer.valueOf(m), Integer.valueOf(s));
        return durationValue2;
    }

    @TargetApi(11)
    public static int determineTypeBits(Context context, Intent intent) {
        int typeBits;
        String type = intent.resolveType(context);
        if ("*/*".equals(type)) {
            typeBits = 3;
        } else if ("image/*".equals(type) || "vnd.android.cursor.dir/image".equals(type)) {
            typeBits = 1;
        } else if ("video/*".equals(type) || "vnd.android.cursor.dir/video".equals(type)) {
            typeBits = 2;
        } else {
            typeBits = 3;
        }
        if (ApiHelper.HAS_INTENT_EXTRA_LOCAL_ONLY && intent.getBooleanExtra("android.intent.extra.LOCAL_ONLY", false)) {
            return typeBits | 4;
        }
        return typeBits;
    }

    public static int getSelectionModePrompt(int typeBits) {
        if ((typeBits & 2) != 0) {
            return (typeBits & 1) == 0 ? R.string.select_video : R.string.select_item;
        }
        return R.string.select_image;
    }
}
