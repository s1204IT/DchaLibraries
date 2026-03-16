package com.android.camera.util;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.admin.DevicePolicyManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.hardware.camera2.CameraCharacteristics;
import android.location.Location;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.accessibility.AccessibilityEventCompat;
import android.telephony.TelephonyManager;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Toast;
import com.android.camera.CameraActivity;
import com.android.camera.CameraDisabledException;
import com.android.camera.debug.Log;
import com.android.camera2.R;
import com.android.ex.camera2.portability.CameraCapabilities;
import com.android.ex.camera2.portability.CameraSettings;
import java.io.Closeable;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public class CameraUtil {
    public static final String ACTION_CAMERA_SHUTTER_CLICK = "com.android.camera.action.SHUTTER_CLICK";
    public static final String ACTION_CAMERA_STARTED = "com.android.camera.action.CAMERA_STARTED";
    public static final String ACTION_CAMERA_STOPPED = "com.android.camera.action.CAMERA_STOPPED";
    public static final String ACTION_NEW_PICTURE = "android.hardware.action.NEW_PICTURE";
    public static final String ACTION_NEW_VIDEO = "android.hardware.action.NEW_VIDEO";
    private static final String EXTRAS_CAMERA_FACING = "android.intent.extras.CAMERA_FACING";
    public static final String KEY_RETURN_DATA = "return-data";
    public static final String KEY_SHOW_WHEN_LOCKED = "showWhenLocked";
    public static final String KEY_TREAT_UP_AS_BACK = "treat-up-as-back";
    private static final String MAPS_CLASS_NAME = "com.google.android.maps.MapsActivity";
    private static final String MAPS_PACKAGE_NAME = "com.google.android.apps.maps";
    private static final int MAX_PREVIEW_FPS_TIMES_1000 = 400000;
    public static final int ORIENTATION_HYSTERESIS = 5;
    private static final int PREFERRED_PREVIEW_FPS_TIMES_1000 = 30000;
    public static final String REVIEW_ACTION = "com.android.camera.action.REVIEW";
    private static ImageFileNamer sImageFileNamer;
    private static final Log.Tag TAG = new Log.Tag("Util");
    private static float sPixelDensity = 1.0f;
    private static int[] sLocation = new int[2];

    private CameraUtil() {
    }

    public static void initialize(Context context) {
        DisplayMetrics metrics = new DisplayMetrics();
        WindowManager wm = (WindowManager) context.getSystemService("window");
        wm.getDefaultDisplay().getMetrics(metrics);
        sPixelDensity = metrics.density;
        sImageFileNamer = new ImageFileNamer(context.getString(R.string.image_file_name_format));
    }

    public static int dpToPixel(int dp) {
        return Math.round(sPixelDensity * dp);
    }

    public static Bitmap rotate(Bitmap b, int degrees) {
        return rotateAndMirror(b, degrees, false);
    }

    public static Bitmap rotateAndMirror(Bitmap b, int degrees, boolean mirror) {
        if ((degrees != 0 || mirror) && b != null) {
            Matrix m = new Matrix();
            if (mirror) {
                m.postScale(-1.0f, 1.0f);
                degrees = (degrees + 360) % 360;
                if (degrees == 0 || degrees == 180) {
                    m.postTranslate(b.getWidth(), 0.0f);
                } else if (degrees == 90 || degrees == 270) {
                    m.postTranslate(b.getHeight(), 0.0f);
                } else {
                    throw new IllegalArgumentException("Invalid degrees=" + degrees);
                }
            }
            if (degrees != 0) {
                m.postRotate(degrees, b.getWidth() / 2.0f, b.getHeight() / 2.0f);
            }
            try {
                Bitmap b2 = Bitmap.createBitmap(b, 0, 0, b.getWidth(), b.getHeight(), m, true);
                if (b != b2) {
                    b.recycle();
                    return b2;
                }
                return b;
            } catch (OutOfMemoryError e) {
                return b;
            }
        }
        return b;
    }

    public static int computeSampleSize(BitmapFactory.Options options, int minSideLength, int maxNumOfPixels) {
        int initialSize = computeInitialSampleSize(options, minSideLength, maxNumOfPixels);
        if (initialSize <= 8) {
            int roundedSize = 1;
            while (roundedSize < initialSize) {
                roundedSize <<= 1;
            }
            return roundedSize;
        }
        int roundedSize2 = ((initialSize + 7) / 8) * 8;
        return roundedSize2;
    }

    private static int computeInitialSampleSize(BitmapFactory.Options options, int minSideLength, int maxNumOfPixels) {
        double w = options.outWidth;
        double h = options.outHeight;
        int lowerBound = maxNumOfPixels < 0 ? 1 : (int) Math.ceil(Math.sqrt((w * h) / ((double) maxNumOfPixels)));
        int upperBound = minSideLength < 0 ? 128 : (int) Math.min(Math.floor(w / ((double) minSideLength)), Math.floor(h / ((double) minSideLength)));
        if (upperBound >= lowerBound) {
            if (maxNumOfPixels < 0 && minSideLength < 0) {
                return 1;
            }
            if (minSideLength >= 0) {
                return upperBound;
            }
            return lowerBound;
        }
        return lowerBound;
    }

    public static Bitmap makeBitmap(byte[] jpegData, int maxNumOfPixels) {
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(jpegData, 0, jpegData.length, options);
            if (options.mCancel || options.outWidth == -1 || options.outHeight == -1) {
                return null;
            }
            options.inSampleSize = computeSampleSize(options, -1, maxNumOfPixels);
            options.inJustDecodeBounds = false;
            options.inDither = false;
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            return BitmapFactory.decodeByteArray(jpegData, 0, jpegData.length, options);
        } catch (OutOfMemoryError ex) {
            Log.e(TAG, "Got oom exception ", ex);
            return null;
        }
    }

    public static void closeSilently(Closeable c) {
        if (c != null) {
            try {
                c.close();
            } catch (Throwable th) {
            }
        }
    }

    public static void Assert(boolean cond) {
        if (!cond) {
            throw new AssertionError();
        }
    }

    public static void showErrorAndFinish(final Activity activity, int msgId) {
        DialogInterface.OnClickListener buttonListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                activity.finish();
            }
        };
        TypedValue out = new TypedValue();
        activity.getTheme().resolveAttribute(android.R.attr.alertDialogIcon, out, true);
        if (!activity.isFinishing()) {
            Log.e(TAG, "Show fatal error dialog");
            new AlertDialog.Builder(activity).setCancelable(false).setTitle(R.string.camera_error_title).setMessage(msgId).setNeutralButton(R.string.dialog_ok, buttonListener).setIcon(out.resourceId).show();
        }
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
        int n2 = n - 1;
        int n3 = n2 | (n2 >>> 16);
        int n4 = n3 | (n3 >>> 8);
        int n5 = n4 | (n4 >>> 4);
        int n6 = n5 | (n5 >>> 2);
        return (n6 | (n6 >>> 1)) + 1;
    }

    public static float distance(float x, float y, float sx, float sy) {
        float dx = x - sx;
        float dy = y - sy;
        return (float) Math.sqrt((dx * dx) + (dy * dy));
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

    public static float lerp(float a, float b, float t) {
        return ((b - a) * t) + a;
    }

    public static PointF normalizedSensorCoordsForNormalizedDisplayCoords(float nx, float ny, int sensorOrientation) {
        switch (sensorOrientation) {
            case 0:
                return new PointF(nx, ny);
            case 90:
                return new PointF(ny, 1.0f - nx);
            case 180:
                return new PointF(1.0f - nx, 1.0f - ny);
            case 270:
                return new PointF(1.0f - ny, nx);
            default:
                return null;
        }
    }

    public static Size constrainToAspectRatio(Size size, float aspectRatio) {
        float width = size.getWidth();
        float height = size.getHeight();
        float currentAspectRatio = (1.0f * width) / height;
        if (currentAspectRatio > aspectRatio) {
            if (width > height) {
                width = height * aspectRatio;
            } else {
                height = width / aspectRatio;
            }
        } else if (currentAspectRatio < aspectRatio) {
            if (width < height) {
                width = height * aspectRatio;
            } else {
                height = width / aspectRatio;
            }
        }
        return new Size((int) width, (int) height);
    }

    public static int getDisplayRotation(Context context) {
        WindowManager windowManager = (WindowManager) context.getSystemService("window");
        int rotation = windowManager.getDefaultDisplay().getRotation();
        switch (rotation) {
            case 0:
            default:
                return 0;
            case 1:
                return 90;
            case 2:
                return 180;
            case 3:
                return 270;
        }
    }

    public static boolean isDefaultToPortrait(Context context) {
        int naturalWidth;
        int naturalHeight;
        Display currentDisplay = ((WindowManager) context.getSystemService("window")).getDefaultDisplay();
        Point displaySize = new Point();
        currentDisplay.getSize(displaySize);
        int orientation = currentDisplay.getRotation();
        if (orientation == 0 || orientation == 2) {
            naturalWidth = displaySize.x;
            naturalHeight = displaySize.y;
        } else {
            naturalWidth = displaySize.y;
            naturalHeight = displaySize.x;
        }
        return naturalWidth < naturalHeight;
    }

    public static int roundOrientation(int orientation, int orientationHistory) {
        boolean changeOrientation;
        if (orientationHistory == -1) {
            changeOrientation = true;
        } else {
            int dist = Math.abs(orientation - orientationHistory);
            changeOrientation = Math.min(dist, 360 - dist) >= 50;
        }
        if (changeOrientation) {
            return (((orientation + 45) / 90) * 90) % 360;
        }
        return orientationHistory;
    }

    private static Size getDefaultDisplaySize(Context context) {
        WindowManager windowManager = (WindowManager) context.getSystemService("window");
        Point res = new Point();
        windowManager.getDefaultDisplay().getSize(res);
        return new Size(res);
    }

    public static com.android.ex.camera2.portability.Size getOptimalPreviewSize(Context context, List<com.android.ex.camera2.portability.Size> sizes, double targetRatio) {
        int optimalPickIndex = getOptimalPreviewSizeIndex(context, Size.convert(sizes), targetRatio);
        if (optimalPickIndex == -1) {
            return null;
        }
        return sizes.get(optimalPickIndex);
    }

    public static int getOptimalPreviewSizeIndex(Context context, List<Size> sizes, double targetRatio) {
        double ASPECT_TOLERANCE;
        if (ApiHelper.IS_HTC && targetRatio > 1.3433d && targetRatio < 1.35d) {
            Log.w(TAG, "4:3 ratio out of normal tolerance, increasing tolerance to 0.02");
            ASPECT_TOLERANCE = 0.02d;
        } else {
            ASPECT_TOLERANCE = 0.01d;
        }
        if (sizes == null) {
            return -1;
        }
        int optimalSizeIndex = -1;
        double minDiff = Double.MAX_VALUE;
        Size defaultDisplaySize = getDefaultDisplaySize(context);
        int targetHeight = Math.min(defaultDisplaySize.getWidth(), defaultDisplaySize.getHeight());
        for (int i = 0; i < sizes.size(); i++) {
            Size size = sizes.get(i);
            double ratio = ((double) size.getWidth()) / ((double) size.getHeight());
            if (Math.abs(ratio - targetRatio) <= ASPECT_TOLERANCE) {
                double heightDiff = Math.abs(size.getHeight() - targetHeight);
                if (heightDiff < minDiff) {
                    optimalSizeIndex = i;
                    minDiff = heightDiff;
                } else if (heightDiff == minDiff && size.getHeight() < targetHeight) {
                    optimalSizeIndex = i;
                    minDiff = heightDiff;
                }
            }
        }
        if (optimalSizeIndex == -1) {
            Log.w(TAG, "No preview size match the aspect ratio. available sizes: " + sizes);
            double minDiff2 = Double.MAX_VALUE;
            for (int i2 = 0; i2 < sizes.size(); i2++) {
                Size size2 = sizes.get(i2);
                if (Math.abs(size2.getHeight() - targetHeight) < minDiff2) {
                    optimalSizeIndex = i2;
                    minDiff2 = Math.abs(size2.getHeight() - targetHeight);
                }
            }
            return optimalSizeIndex;
        }
        return optimalSizeIndex;
    }

    public static com.android.ex.camera2.portability.Size getOptimalVideoSnapshotPictureSize(List<com.android.ex.camera2.portability.Size> sizes, int targetWidth, int targetHeight) {
        if (sizes == null) {
            return null;
        }
        com.android.ex.camera2.portability.Size optimalSize = null;
        for (com.android.ex.camera2.portability.Size size : sizes) {
            if (size.height() == targetHeight && size.width() == targetWidth) {
                return size;
            }
        }
        double targetRatio = ((double) targetWidth) / ((double) targetHeight);
        for (com.android.ex.camera2.portability.Size size2 : sizes) {
            double ratio = ((double) size2.width()) / ((double) size2.height());
            if (Math.abs(ratio - targetRatio) <= 0.001d && (optimalSize == null || size2.width() > optimalSize.width())) {
                optimalSize = size2;
            }
        }
        if (optimalSize == null) {
            Log.w(TAG, "No picture size match the aspect ratio");
            for (com.android.ex.camera2.portability.Size size3 : sizes) {
                if (optimalSize == null || size3.width() > optimalSize.width()) {
                    optimalSize = size3;
                }
            }
        }
        return optimalSize;
    }

    public static boolean isMmsCapable(Context context) {
        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService("phone");
        if (telephonyManager == null) {
            return false;
        }
        try {
            Class<?>[] partypes = new Class[0];
            Method sIsVoiceCapable = TelephonyManager.class.getMethod("isVoiceCapable", partypes);
            Object[] arglist = new Object[0];
            Object retobj = sIsVoiceCapable.invoke(telephonyManager, arglist);
            return ((Boolean) retobj).booleanValue();
        } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            return true;
        }
    }

    public static int getCameraFacingIntentExtras(Activity currentActivity) {
        int backCameraId;
        int intentCameraId = currentActivity.getIntent().getIntExtra(EXTRAS_CAMERA_FACING, -1);
        if (isFrontCameraIntent(intentCameraId)) {
            int frontCameraId = ((CameraActivity) currentActivity).getCameraProvider().getFirstFrontCameraId();
            if (frontCameraId == -1) {
                return -1;
            }
            return frontCameraId;
        }
        if (!isBackCameraIntent(intentCameraId) || (backCameraId = ((CameraActivity) currentActivity).getCameraProvider().getFirstBackCameraId()) == -1) {
            return -1;
        }
        return backCameraId;
    }

    private static boolean isFrontCameraIntent(int intentCameraId) {
        return intentCameraId == 1;
    }

    private static boolean isBackCameraIntent(int intentCameraId) {
        return intentCameraId == 0;
    }

    public static boolean pointInView(float x, float y, View v) {
        v.getLocationInWindow(sLocation);
        return x >= ((float) sLocation[0]) && x < ((float) (sLocation[0] + v.getWidth())) && y >= ((float) sLocation[1]) && y < ((float) (sLocation[1] + v.getHeight()));
    }

    public static int[] getRelativeLocation(View reference, View view) {
        reference.getLocationInWindow(sLocation);
        int referenceX = sLocation[0];
        int referenceY = sLocation[1];
        view.getLocationInWindow(sLocation);
        int[] iArr = sLocation;
        iArr[0] = iArr[0] - referenceX;
        int[] iArr2 = sLocation;
        iArr2[1] = iArr2[1] - referenceY;
        return sLocation;
    }

    public static boolean isUriValid(Uri uri, ContentResolver resolver) {
        boolean z = false;
        if (uri != null) {
            try {
                ParcelFileDescriptor pfd = resolver.openFileDescriptor(uri, "r");
                if (pfd == null) {
                    Log.e(TAG, "Fail to open URI. URI=" + uri);
                } else {
                    pfd.close();
                    z = true;
                }
            } catch (IOException e) {
            }
        }
        return z;
    }

    public static void dumpRect(RectF rect, String msg) {
        Log.v(TAG, msg + "=(" + rect.left + com.android.ex.camera2.portability.Size.DELIMITER + rect.top + com.android.ex.camera2.portability.Size.DELIMITER + rect.right + com.android.ex.camera2.portability.Size.DELIMITER + rect.bottom + ")");
    }

    public static void rectFToRect(RectF rectF, Rect rect) {
        rect.left = Math.round(rectF.left);
        rect.top = Math.round(rectF.top);
        rect.right = Math.round(rectF.right);
        rect.bottom = Math.round(rectF.bottom);
    }

    public static Rect rectFToRect(RectF rectF) {
        Rect rect = new Rect();
        rectFToRect(rectF, rect);
        return rect;
    }

    public static RectF rectToRectF(Rect r) {
        return new RectF(r.left, r.top, r.right, r.bottom);
    }

    public static void prepareMatrix(Matrix matrix, boolean mirror, int displayOrientation, int viewWidth, int viewHeight) {
        matrix.setScale(mirror ? -1.0f : 1.0f, 1.0f);
        matrix.postRotate(displayOrientation);
        matrix.postScale(viewWidth / 2000.0f, viewHeight / 2000.0f);
        matrix.postTranslate(viewWidth / 2.0f, viewHeight / 2.0f);
    }

    public static void prepareMatrix(Matrix matrix, boolean mirror, int displayOrientation, Rect previewRect) {
        matrix.setScale(mirror ? -1.0f : 1.0f, 1.0f);
        matrix.postRotate(displayOrientation);
        Matrix mapping = new Matrix();
        mapping.setRectToRect(new RectF(-1000.0f, -1000.0f, 1000.0f, 1000.0f), rectToRectF(previewRect), Matrix.ScaleToFit.FILL);
        matrix.setConcat(mapping, matrix);
    }

    public static String createJpegName(long dateTaken) {
        String strGenerateName;
        synchronized (sImageFileNamer) {
            strGenerateName = sImageFileNamer.generateName(dateTaken);
        }
        return strGenerateName;
    }

    public static void broadcastNewPicture(Context context, Uri uri) {
        context.sendBroadcast(new Intent(ACTION_NEW_PICTURE, uri));
        context.sendBroadcast(new Intent("com.android.camera.NEW_PICTURE", uri));
    }

    public static void fadeIn(View view, float startAlpha, float endAlpha, long duration) {
        if (view.getVisibility() != 0) {
            view.setVisibility(0);
            Animation animation = new AlphaAnimation(startAlpha, endAlpha);
            animation.setDuration(duration);
            view.startAnimation(animation);
        }
    }

    public static Bitmap downSample(byte[] data, int downSampleFactor) {
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inSampleSize = downSampleFactor;
        return BitmapFactory.decodeByteArray(data, 0, data.length, opts);
    }

    public static void setGpsParameters(CameraSettings settings, Location loc) {
        settings.clearGpsData();
        boolean hasLatLon = false;
        if (loc != null) {
            double lat = loc.getLatitude();
            double lon = loc.getLongitude();
            hasLatLon = (lat == 0.0d && lon == 0.0d) ? false : true;
        }
        if (!hasLatLon) {
            settings.setGpsData(new CameraSettings.GpsData(0.0d, 0.0d, 0.0d, System.currentTimeMillis() / 1000, null));
            return;
        }
        Log.d(TAG, "Set gps location");
        long utcTimeSeconds = loc.getTime() / 1000;
        settings.setGpsData(new CameraSettings.GpsData(loc.getLatitude(), loc.getLongitude(), loc.hasAltitude() ? loc.getAltitude() : 0.0d, utcTimeSeconds != 0 ? utcTimeSeconds : System.currentTimeMillis(), loc.getProvider().toUpperCase()));
    }

    public static int[] getPhotoPreviewFpsRange(CameraCapabilities capabilities) {
        return getPhotoPreviewFpsRange(capabilities.getSupportedPreviewFpsRange());
    }

    public static int[] getPhotoPreviewFpsRange(List<int[]> frameRates) {
        if (frameRates.size() == 0) {
            Log.e(TAG, "No suppoted frame rates returned!");
            return null;
        }
        int lowestMinRate = MAX_PREVIEW_FPS_TIMES_1000;
        for (int[] rate : frameRates) {
            int minFps = rate[0];
            if (rate[1] >= PREFERRED_PREVIEW_FPS_TIMES_1000 && minFps <= PREFERRED_PREVIEW_FPS_TIMES_1000 && minFps < lowestMinRate) {
                lowestMinRate = minFps;
            }
        }
        int resultIndex = -1;
        int highestMaxRate = 0;
        for (int i = 0; i < frameRates.size(); i++) {
            int[] rate2 = frameRates.get(i);
            int minFps2 = rate2[0];
            int maxFps = rate2[1];
            if (minFps2 == lowestMinRate && highestMaxRate < maxFps) {
                highestMaxRate = maxFps;
                resultIndex = i;
            }
        }
        if (resultIndex >= 0) {
            return frameRates.get(resultIndex);
        }
        Log.e(TAG, "Can't find an appropiate frame rate range!");
        return null;
    }

    public static int[] getMaxPreviewFpsRange(List<int[]> frameRates) {
        return (frameRates == null || frameRates.size() <= 0) ? new int[0] : frameRates.get(frameRates.size() - 1);
    }

    public static void throwIfCameraDisabled(Context context) throws CameraDisabledException {
        DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService("device_policy");
        if (dpm.getCameraDisabled(null)) {
            throw new CameraDisabledException();
        }
    }

    private static void getGaussianMask(float[] mask) {
        int len = mask.length;
        int mid = len / 2;
        float sigma = len;
        float sum = 0.0f;
        for (int i = 0; i <= mid; i++) {
            float ex = ((float) Math.exp(((-(i - mid)) * (i - mid)) / (mid * mid))) / ((2.0f * sigma) * sigma);
            int symmetricIndex = (len - 1) - i;
            mask[i] = ex;
            mask[symmetricIndex] = ex;
            sum += mask[i];
            if (i != symmetricIndex) {
                sum += mask[symmetricIndex];
            }
        }
        for (int i2 = 0; i2 < mask.length; i2++) {
            mask[i2] = mask[i2] / sum;
        }
    }

    public static int addPixel(int pixel, int newPixel, float weight) {
        int r = ((pixel & 16711680) + ((int) ((newPixel & 16711680) * weight))) & 16711680;
        int g = ((pixel & MotionEventCompat.ACTION_POINTER_INDEX_MASK) + ((int) ((newPixel & MotionEventCompat.ACTION_POINTER_INDEX_MASK) * weight))) & MotionEventCompat.ACTION_POINTER_INDEX_MASK;
        int b = ((pixel & MotionEventCompat.ACTION_MASK) + ((int) ((newPixel & MotionEventCompat.ACTION_MASK) * weight))) & MotionEventCompat.ACTION_MASK;
        return (-16777216) | r | g | b;
    }

    public static void blur(int[] src, int[] out, int w, int h, int size) {
        float[] k = new float[size];
        int off = size / 2;
        getGaussianMask(k);
        int[] tmp = new int[src.length];
        int rowPointer = 0;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int sum = 0;
                for (int i = 0; i < k.length; i++) {
                    int dx = (x + i) - off;
                    sum = addPixel(sum, src[rowPointer + clamp(dx, 0, w - 1)], k[i]);
                }
                tmp[x + rowPointer] = sum;
            }
            rowPointer += w;
        }
        for (int x2 = 0; x2 < w; x2++) {
            int rowPointer2 = 0;
            for (int y2 = 0; y2 < h; y2++) {
                int sum2 = 0;
                for (int i2 = 0; i2 < k.length; i2++) {
                    int dy = (y2 + i2) - off;
                    sum2 = addPixel(sum2, tmp[(clamp(dy, 0, h - 1) * w) + x2], k[i2]);
                }
                out[x2 + rowPointer2] = sum2;
                rowPointer2 += w;
            }
        }
    }

    public static Point resizeToFill(int imageWidth, int imageHeight, int imageRotation, int boundWidth, int boundHeight) {
        if (imageRotation % 180 != 0) {
            imageWidth = imageHeight;
            imageHeight = imageWidth;
        }
        if (imageWidth == -2 || imageHeight == -2) {
            imageWidth = boundWidth;
            imageHeight = boundHeight;
        }
        Point p = new Point();
        p.x = boundWidth;
        p.y = boundHeight;
        if (imageWidth * boundHeight > boundWidth * imageHeight) {
            p.y = (p.x * imageHeight) / imageWidth;
        } else {
            p.x = (p.y * imageWidth) / imageHeight;
        }
        return p;
    }

    private static class ImageFileNamer {
        private final SimpleDateFormat mFormat;
        private long mLastDate;
        private int mSameSecondCount;

        public ImageFileNamer(String format) {
            this.mFormat = new SimpleDateFormat(format);
        }

        public String generateName(long dateTaken) {
            Date date = new Date(dateTaken);
            String result = this.mFormat.format(date);
            if (dateTaken / 1000 == this.mLastDate / 1000) {
                this.mSameSecondCount++;
                return result + "_" + this.mSameSecondCount;
            }
            this.mLastDate = dateTaken;
            this.mSameSecondCount = 0;
            return result;
        }
    }

    public static void playVideo(Activity activity, Uri uri, String title) {
        try {
            CameraActivity cameraActivity = (CameraActivity) activity;
            boolean isSecureCamera = cameraActivity.isSecureCamera();
            if (!isSecureCamera) {
                Intent intent = IntentHelper.getVideoPlayerIntent(uri).putExtra("android.intent.extra.TITLE", title).putExtra(KEY_TREAT_UP_AS_BACK, true);
                cameraActivity.launchActivityByIntent(intent);
            } else {
                activity.finish();
            }
        } catch (ActivityNotFoundException e) {
            Toast.makeText(activity, activity.getString(R.string.video_err), 0).show();
        }
    }

    public static void showOnMap(Activity activity, double[] latLong) {
        try {
            String uri = String.format(Locale.ENGLISH, "http://maps.google.com/maps?f=q&q=(%f,%f)", Double.valueOf(latLong[0]), Double.valueOf(latLong[1]));
            ComponentName compName = new ComponentName(MAPS_PACKAGE_NAME, MAPS_CLASS_NAME);
            Intent mapsIntent = new Intent("android.intent.action.VIEW", Uri.parse(uri)).setComponent(compName);
            mapsIntent.addFlags(AccessibilityEventCompat.TYPE_GESTURE_DETECTION_END);
            activity.startActivity(mapsIntent);
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, "GMM activity not found!", e);
            String url = String.format(Locale.ENGLISH, "geo:%f,%f", Double.valueOf(latLong[0]), Double.valueOf(latLong[1]));
            activity.startActivity(new Intent("android.intent.action.VIEW", Uri.parse(url)));
        }
    }

    public static String dumpStackTrace(int level) {
        StackTraceElement[] elems = Thread.currentThread().getStackTrace();
        int level2 = level == 0 ? elems.length : Math.min(level + 3, elems.length);
        String ret = new String();
        for (int i = 3; i < level2; i++) {
            ret = ret + "\t" + elems[i].toString() + '\n';
        }
        return ret;
    }

    public static int getCameraThemeColorId(int modeIndex, Context context) {
        TypedArray colorRes = context.getResources().obtainTypedArray(R.array.camera_mode_theme_color);
        if (modeIndex < colorRes.length() && modeIndex >= 0) {
            return colorRes.getResourceId(modeIndex, 0);
        }
        Log.e(TAG, "Invalid mode index: " + modeIndex);
        return 0;
    }

    public static int getCameraModeIconResId(int modeIndex, Context context) {
        TypedArray cameraModesIcons = context.getResources().obtainTypedArray(R.array.camera_mode_icon);
        if (modeIndex < cameraModesIcons.length() && modeIndex >= 0) {
            return cameraModesIcons.getResourceId(modeIndex, 0);
        }
        Log.e(TAG, "Invalid mode index: " + modeIndex);
        return 0;
    }

    public static String getCameraModeText(int modeIndex, Context context) {
        String[] cameraModesText = context.getResources().getStringArray(R.array.camera_mode_text);
        if (modeIndex >= 0 && modeIndex < cameraModesText.length) {
            return cameraModesText[modeIndex];
        }
        Log.e(TAG, "Invalid mode index: " + modeIndex);
        return new String();
    }

    public static String getCameraModeContentDescription(int modeIndex, Context context) {
        String[] cameraModesDesc = context.getResources().getStringArray(R.array.camera_mode_content_description);
        if (modeIndex >= 0 && modeIndex < cameraModesDesc.length) {
            return cameraModesDesc[modeIndex];
        }
        Log.e(TAG, "Invalid mode index: " + modeIndex);
        return new String();
    }

    public static int getCameraShutterIconId(int modeIndex, Context context) {
        TypedArray shutterIcons = context.getResources().obtainTypedArray(R.array.camera_mode_shutter_icon);
        if (modeIndex < 0 || modeIndex >= shutterIcons.length()) {
            Log.e(TAG, "Invalid mode index: " + modeIndex);
            throw new IllegalStateException("Invalid mode index: " + modeIndex);
        }
        return shutterIcons.getResourceId(modeIndex, 0);
    }

    public static int getCameraModeParentModeId(int modeIndex, Context context) {
        int[] cameraModeParent = context.getResources().getIntArray(R.array.camera_mode_nested_in_nav_drawer);
        if (modeIndex >= 0 && modeIndex < cameraModeParent.length) {
            return cameraModeParent[modeIndex];
        }
        Log.e(TAG, "Invalid mode index: " + modeIndex);
        return 0;
    }

    public static int getCameraModeCoverIconResId(int modeIndex, Context context) {
        TypedArray cameraModesIcons = context.getResources().obtainTypedArray(R.array.camera_mode_cover_icon);
        if (modeIndex < cameraModesIcons.length() && modeIndex >= 0) {
            return cameraModesIcons.getResourceId(modeIndex, 0);
        }
        Log.e(TAG, "Invalid mode index: " + modeIndex);
        return 0;
    }

    public static int getNumCpuCores() {
        try {
            File dir = new File("/sys/devices/system/cpu/");
            File[] files = dir.listFiles(new FileFilter() {
                @Override
                public boolean accept(File pathname) {
                    return Pattern.matches("cpu[0-9]+", pathname.getName());
                }
            });
            return files.length;
        } catch (Exception e) {
            Log.e(TAG, "Failed to count number of cores, defaulting to 1", e);
            return 1;
        }
    }

    public static int getJpegRotation(int deviceOrientationDegrees, CameraCharacteristics characteristics) {
        if (deviceOrientationDegrees == -1) {
            return 0;
        }
        int facing = ((Integer) characteristics.get(CameraCharacteristics.LENS_FACING)).intValue();
        int sensorOrientation = ((Integer) characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION)).intValue();
        if (facing == 0) {
            return (sensorOrientation + deviceOrientationDegrees) % 360;
        }
        return ((sensorOrientation - deviceOrientationDegrees) + 360) % 360;
    }
}
