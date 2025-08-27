package com.android.launcher3;

import android.app.WallpaperManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Build;
import android.os.Bundle;
import android.os.DeadObjectException;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.TransactionTooLargeException;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.TtsSpan;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Pair;
import android.util.TypedValue;
import android.view.View;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/* loaded from: classes.dex */
public final class Utilities {
    public static final boolean ATLEAST_LOLLIPOP_MR1;
    public static final boolean ATLEAST_MARSHMALLOW;
    public static final boolean ATLEAST_NOUGAT;
    public static final boolean ATLEAST_NOUGAT_MR1;
    public static final boolean ATLEAST_OREO;
    public static final boolean ATLEAST_OREO_MR1;
    public static final boolean ATLEAST_P;
    public static final int COLOR_EXTRACTION_JOB_ID = 1;
    private static final int CORE_POOL_SIZE;
    private static final int CPU_COUNT;
    public static final String EXTRA_WALLPAPER_OFFSET = "com.android.launcher3.WALLPAPER_OFFSET";
    public static final boolean IS_DEBUG_DEVICE;
    private static final int KEEP_ALIVE = 1;
    private static final int MAXIMUM_POOL_SIZE;
    public static final int SINGLE_FRAME_MS = 16;
    private static final String TAG = "Launcher.Utilities";
    public static final Executor THREAD_POOL_EXECUTOR;
    public static final int WALLPAPER_COMPAT_JOB_ID = 2;
    private static final Pattern sTrimPattern = Pattern.compile("^[\\s|\\p{javaSpaceChar}]*(.*)[\\s|\\p{javaSpaceChar}]*$");
    private static final int[] sLoc0 = new int[2];
    private static final int[] sLoc1 = new int[2];
    private static final float[] sPoint = new float[2];
    private static final Matrix sMatrix = new Matrix();
    private static final Matrix sInverseMatrix = new Matrix();

    static {
        ATLEAST_P = Build.VERSION.SDK_INT >= 28;
        ATLEAST_OREO_MR1 = Build.VERSION.SDK_INT >= 27;
        ATLEAST_OREO = Build.VERSION.SDK_INT >= 26;
        ATLEAST_NOUGAT_MR1 = Build.VERSION.SDK_INT >= 25;
        ATLEAST_NOUGAT = Build.VERSION.SDK_INT >= 24;
        ATLEAST_MARSHMALLOW = Build.VERSION.SDK_INT >= 23;
        ATLEAST_LOLLIPOP_MR1 = Build.VERSION.SDK_INT >= 22;
        IS_DEBUG_DEVICE = Build.TYPE.toLowerCase().contains("debug") || Build.TYPE.toLowerCase().equals("eng");
        CPU_COUNT = Runtime.getRuntime().availableProcessors();
        CORE_POOL_SIZE = CPU_COUNT + 1;
        MAXIMUM_POOL_SIZE = (CPU_COUNT * 2) + 1;
        THREAD_POOL_EXECUTOR = new ThreadPoolExecutor(CORE_POOL_SIZE, MAXIMUM_POOL_SIZE, 1L, TimeUnit.SECONDS, new LinkedBlockingQueue());
    }

    public static boolean isPropertyEnabled(String str) {
        return Log.isLoggable(str, 2);
    }

    public static float getDescendantCoordRelativeToAncestor(View view, View view2, int[] iArr, boolean z) {
        sPoint[0] = iArr[0];
        sPoint[1] = iArr[1];
        float scaleX = 1.0f;
        for (View view3 = view; view3 != view2 && view3 != null; view3 = (View) view3.getParent()) {
            if (view3 != view || z) {
                float[] fArr = sPoint;
                fArr[0] = fArr[0] - view3.getScrollX();
                float[] fArr2 = sPoint;
                fArr2[1] = fArr2[1] - view3.getScrollY();
            }
            view3.getMatrix().mapPoints(sPoint);
            float[] fArr3 = sPoint;
            fArr3[0] = fArr3[0] + view3.getLeft();
            float[] fArr4 = sPoint;
            fArr4[1] = fArr4[1] + view3.getTop();
            scaleX *= view3.getScaleX();
        }
        iArr[0] = Math.round(sPoint[0]);
        iArr[1] = Math.round(sPoint[1]);
        return scaleX;
    }

    public static void mapCoordInSelfToDescendant(View view, View view2, int[] iArr) {
        sMatrix.reset();
        while (view != view2) {
            sMatrix.postTranslate(-view.getScrollX(), -view.getScrollY());
            sMatrix.postConcat(view.getMatrix());
            sMatrix.postTranslate(view.getLeft(), view.getTop());
            view = (View) view.getParent();
        }
        sMatrix.postTranslate(-view.getScrollX(), -view.getScrollY());
        sMatrix.invert(sInverseMatrix);
        sPoint[0] = iArr[0];
        sPoint[1] = iArr[1];
        sInverseMatrix.mapPoints(sPoint);
        iArr[0] = Math.round(sPoint[0]);
        iArr[1] = Math.round(sPoint[1]);
    }

    public static boolean pointInView(View view, float f, float f2, float f3) {
        float f4 = -f3;
        return f >= f4 && f2 >= f4 && f < ((float) view.getWidth()) + f3 && f2 < ((float) view.getHeight()) + f3;
    }

    public static int[] getCenterDeltaInScreenSpace(View view, View view2) {
        view.getLocationInWindow(sLoc0);
        view2.getLocationInWindow(sLoc1);
        sLoc0[0] = (int) (r0[0] + ((view.getMeasuredWidth() * view.getScaleX()) / 2.0f));
        sLoc0[1] = (int) (r0[1] + ((view.getMeasuredHeight() * view.getScaleY()) / 2.0f));
        sLoc1[0] = (int) (r6[0] + ((view2.getMeasuredWidth() * view2.getScaleX()) / 2.0f));
        sLoc1[1] = (int) (r6[1] + ((view2.getMeasuredHeight() * view2.getScaleY()) / 2.0f));
        return new int[]{sLoc1[0] - sLoc0[0], sLoc1[1] - sLoc0[1]};
    }

    public static void scaleRectFAboutCenter(RectF rectF, float f) {
        if (f != 1.0f) {
            float fCenterX = rectF.centerX();
            float fCenterY = rectF.centerY();
            rectF.offset(-fCenterX, -fCenterY);
            rectF.left *= f;
            rectF.top *= f;
            rectF.right *= f;
            rectF.bottom *= f;
            rectF.offset(fCenterX, fCenterY);
        }
    }

    public static void scaleRectAboutCenter(Rect rect, float f) {
        if (f != 1.0f) {
            int iCenterX = rect.centerX();
            int iCenterY = rect.centerY();
            rect.offset(-iCenterX, -iCenterY);
            scaleRect(rect, f);
            rect.offset(iCenterX, iCenterY);
        }
    }

    public static void scaleRect(Rect rect, float f) {
        if (f != 1.0f) {
            rect.left = (int) ((rect.left * f) + 0.5f);
            rect.top = (int) ((rect.top * f) + 0.5f);
            rect.right = (int) ((rect.right * f) + 0.5f);
            rect.bottom = (int) ((rect.bottom * f) + 0.5f);
        }
    }

    public static void insetRect(Rect rect, Rect rect2) {
        rect.left = Math.min(rect.right, rect.left + rect2.left);
        rect.top = Math.min(rect.bottom, rect.top + rect2.top);
        rect.right = Math.max(rect.left, rect.right - rect2.right);
        rect.bottom = Math.max(rect.top, rect.bottom - rect2.bottom);
    }

    public static float shrinkRect(Rect rect, float f, float f2) {
        float fMin = Math.min(Math.min(f, f2), 1.0f);
        if (fMin < 1.0f) {
            int iWidth = (int) (rect.width() * (f - fMin) * 0.5f);
            rect.left += iWidth;
            rect.right -= iWidth;
            int iHeight = (int) (rect.height() * (f2 - fMin) * 0.5f);
            rect.top += iHeight;
            rect.bottom -= iHeight;
        }
        return fMin;
    }

    public static float mapRange(float f, float f2, float f3) {
        return f2 + (f * (f3 - f2));
    }

    public static boolean isSystemApp(Context context, Intent intent) throws PackageManager.NameNotFoundException {
        String packageName;
        PackageManager packageManager = context.getPackageManager();
        ComponentName component = intent.getComponent();
        if (component == null) {
            ResolveInfo resolveInfoResolveActivity = packageManager.resolveActivity(intent, 65536);
            if (resolveInfoResolveActivity != null && resolveInfoResolveActivity.activityInfo != null) {
                packageName = resolveInfoResolveActivity.activityInfo.packageName;
            } else {
                packageName = null;
            }
        } else {
            packageName = component.getPackageName();
        }
        if (packageName == null) {
            return false;
        }
        try {
            PackageInfo packageInfo = packageManager.getPackageInfo(packageName, 0);
            if (packageInfo != null && packageInfo.applicationInfo != null) {
                if ((packageInfo.applicationInfo.flags & 1) != 0) {
                    return true;
                }
            }
            return false;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    static Pair<String, Resources> findSystemApk(String str, PackageManager packageManager) {
        for (ResolveInfo resolveInfo : packageManager.queryBroadcastReceivers(new Intent(str), 0)) {
            if (resolveInfo.activityInfo != null && (resolveInfo.activityInfo.applicationInfo.flags & 1) != 0) {
                String str2 = resolveInfo.activityInfo.packageName;
                try {
                    return Pair.create(str2, packageManager.getResourcesForApplication(str2));
                } catch (PackageManager.NameNotFoundException e) {
                    Log.w(TAG, "Failed to find resources for " + str2);
                }
            }
        }
        return null;
    }

    public static byte[] flattenBitmap(Bitmap bitmap) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(bitmap.getWidth() * bitmap.getHeight() * 4);
        try {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
            byteArrayOutputStream.flush();
            byteArrayOutputStream.close();
            return byteArrayOutputStream.toByteArray();
        } catch (IOException e) {
            Log.w(TAG, "Could not write bitmap");
            return null;
        }
    }

    public static String trim(CharSequence charSequence) {
        if (charSequence == null) {
            return null;
        }
        return sTrimPattern.matcher(charSequence).replaceAll("$1");
    }

    public static int calculateTextHeight(float f) {
        Paint paint = new Paint();
        paint.setTextSize(f);
        Paint.FontMetrics fontMetrics = paint.getFontMetrics();
        return (int) Math.ceil(fontMetrics.bottom - fontMetrics.top);
    }

    public static void println(String str, Object... objArr) {
        StringBuilder sb = new StringBuilder();
        sb.append(str);
        sb.append(": ");
        boolean z = true;
        for (Object obj : objArr) {
            if (!z) {
                sb.append(", ");
            } else {
                z = false;
            }
            sb.append(obj);
        }
        System.out.println(sb.toString());
    }

    public static boolean isRtl(Resources resources) {
        return resources.getConfiguration().getLayoutDirection() == 1;
    }

    public static boolean isLauncherAppTarget(Intent intent) {
        if (intent == null || !"android.intent.action.MAIN".equals(intent.getAction()) || intent.getComponent() == null || intent.getCategories() == null || intent.getCategories().size() != 1 || !intent.hasCategory("android.intent.category.LAUNCHER") || !TextUtils.isEmpty(intent.getDataString())) {
            return false;
        }
        Bundle extras = intent.getExtras();
        return extras == null || extras.keySet().isEmpty();
    }

    public static float dpiFromPx(int i, DisplayMetrics displayMetrics) {
        return i / (displayMetrics.densityDpi / 160.0f);
    }

    public static int pxFromDp(float f, DisplayMetrics displayMetrics) {
        return Math.round(TypedValue.applyDimension(1, f, displayMetrics));
    }

    public static int pxFromSp(float f, DisplayMetrics displayMetrics) {
        return Math.round(TypedValue.applyDimension(2, f, displayMetrics));
    }

    public static String createDbSelectionQuery(String str, Iterable<?> iterable) {
        return String.format(Locale.ENGLISH, "%s IN (%s)", str, TextUtils.join(", ", iterable));
    }

    public static boolean isBootCompleted() {
        return "1".equals(getSystemProperty("sys.boot_completed", "1"));
    }

    public static String getSystemProperty(String str, String str2) {
        String str3;
        try {
            str3 = (String) Class.forName("android.os.SystemProperties").getDeclaredMethod("get", String.class).invoke(null, str);
        } catch (Exception e) {
            Log.d(TAG, "Unable to read system properties");
        }
        if (!TextUtils.isEmpty(str3)) {
            return str3;
        }
        return str2;
    }

    public static int boundToRange(int i, int i2, int i3) {
        return Math.max(i2, Math.min(i, i3));
    }

    public static float boundToRange(float f, float f2, float f3) {
        return Math.max(f2, Math.min(f, f3));
    }

    public static CharSequence wrapForTts(CharSequence charSequence, String str) {
        SpannableString spannableString = new SpannableString(charSequence);
        spannableString.setSpan(new TtsSpan.TextBuilder(str).build(), 0, spannableString.length(), 18);
        return spannableString;
    }

    public static int longCompare(long j, long j2) {
        if (j < j2) {
            return -1;
        }
        return j == j2 ? 0 : 1;
    }

    public static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(LauncherFiles.SHARED_PREFERENCES_KEY, 0);
    }

    public static SharedPreferences getDevicePrefs(Context context) {
        return context.getSharedPreferences(LauncherFiles.DEVICE_PREFERENCES_KEY, 0);
    }

    public static boolean isPowerSaverPreventingAnimation(Context context) {
        if (ATLEAST_P) {
            return false;
        }
        return ((PowerManager) context.getSystemService("power")).isPowerSaveMode();
    }

    public static boolean isWallpaperAllowed(Context context) {
        if (ATLEAST_NOUGAT) {
            try {
                WallpaperManager wallpaperManager = (WallpaperManager) context.getSystemService(WallpaperManager.class);
                return ((Boolean) wallpaperManager.getClass().getDeclaredMethod("isSetWallpaperAllowed", new Class[0]).invoke(wallpaperManager, new Object[0])).booleanValue();
            } catch (Exception e) {
                return true;
            }
        }
        return true;
    }

    public static void closeSilently(Closeable closeable) throws IOException {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
            }
        }
    }

    public static boolean containsAll(Bundle bundle, Bundle bundle2) {
        for (String str : bundle2.keySet()) {
            Object obj = bundle2.get(str);
            Object obj2 = bundle.get(str);
            if (obj == null) {
                if (obj2 != null) {
                    return false;
                }
            } else if (!obj.equals(obj2)) {
                return false;
            }
        }
        return true;
    }

    public static boolean isEmpty(Collection collection) {
        return collection == null || collection.isEmpty();
    }

    public static boolean isBinderSizeError(Exception exc) {
        return (exc.getCause() instanceof TransactionTooLargeException) || (exc.getCause() instanceof DeadObjectException);
    }

    public static <T> T getOverrideObject(Class<T> cls, Context context, int i) {
        String string = context.getString(i);
        if (!TextUtils.isEmpty(string)) {
            try {
                return (T) Class.forName(string).getDeclaredConstructor(Context.class).newInstance(context);
            } catch (ClassCastException | ClassNotFoundException | IllegalAccessException | InstantiationException | NoSuchMethodException | InvocationTargetException e) {
                Log.e(TAG, "Bad overriden class", e);
            }
        }
        try {
            return cls.newInstance();
        } catch (IllegalAccessException | InstantiationException e2) {
            throw new RuntimeException(e2);
        }
    }

    public static <T> HashSet<T> singletonHashSet(T t) {
        HashSet<T> hashSet = new HashSet<>(1);
        hashSet.add(t);
        return hashSet;
    }

    public static void postAsyncCallback(Handler handler, Runnable runnable) {
        Message messageObtain = Message.obtain(handler, runnable);
        messageObtain.setAsynchronous(true);
        handler.sendMessage(messageObtain);
    }
}
