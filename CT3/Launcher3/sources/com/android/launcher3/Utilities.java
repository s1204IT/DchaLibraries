package com.android.launcher3;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.SearchManager;
import android.app.WallpaperManager;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.PaintDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.Process;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.TtsSpan;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Pair;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.View;
import android.widget.Toast;
import com.android.launcher3.compat.UserHandleCompat;
import com.android.launcher3.config.FeatureFlags;
import com.android.launcher3.util.IconNormalizer;
import com.mediatek.launcher3.LauncherLog;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Utilities {
    public static final boolean ATLEAST_JB_MR1;
    public static final boolean ATLEAST_JB_MR2;
    public static final boolean ATLEAST_KITKAT;
    public static final boolean ATLEAST_LOLLIPOP;
    public static final boolean ATLEAST_LOLLIPOP_MR1;
    public static final boolean ATLEAST_MARSHMALLOW;
    public static final boolean ATLEAST_N;
    private static final int CORE_POOL_SIZE;
    private static final int CPU_COUNT;
    private static final int MAXIMUM_POOL_SIZE;
    public static final Executor THREAD_POOL_EXECUTOR;
    static int sColorIndex;
    static int[] sColors;
    private static final int[] sLoc0;
    private static final int[] sLoc1;
    private static final Rect sOldBounds = new Rect();
    private static final Canvas sCanvas = new Canvas();
    private static final Pattern sTrimPattern = Pattern.compile("^[\\s|\\p{javaSpaceChar}]*(.*)[\\s|\\p{javaSpaceChar}]*$");

    static {
        sCanvas.setDrawFilter(new PaintFlagsDrawFilter(4, 2));
        sColors = new int[]{-65536, -16711936, -16776961};
        sColorIndex = 0;
        sLoc0 = new int[2];
        sLoc1 = new int[2];
        ATLEAST_N = Build.VERSION.SDK_INT >= 24;
        ATLEAST_MARSHMALLOW = Build.VERSION.SDK_INT >= 23;
        ATLEAST_LOLLIPOP_MR1 = Build.VERSION.SDK_INT >= 22;
        ATLEAST_LOLLIPOP = Build.VERSION.SDK_INT >= 21;
        ATLEAST_KITKAT = Build.VERSION.SDK_INT >= 19;
        ATLEAST_JB_MR1 = Build.VERSION.SDK_INT >= 17;
        ATLEAST_JB_MR2 = Build.VERSION.SDK_INT >= 18;
        CPU_COUNT = Runtime.getRuntime().availableProcessors();
        CORE_POOL_SIZE = CPU_COUNT + 1;
        MAXIMUM_POOL_SIZE = (CPU_COUNT * 2) + 1;
        THREAD_POOL_EXECUTOR = new ThreadPoolExecutor(CORE_POOL_SIZE, MAXIMUM_POOL_SIZE, 1L, TimeUnit.SECONDS, new LinkedBlockingQueue());
    }

    public static boolean isPropertyEnabled(String propertyName) {
        return "1".equals(LauncherLog.getSysProperty(propertyName));
    }

    public static boolean isAllowRotationPrefEnabled(Context context) {
        boolean allowRotationPref = false;
        if (ATLEAST_N) {
            int originalDensity = DisplayMetrics.DENSITY_DEVICE_STABLE;
            Resources res = context.getResources();
            int originalSmallestWidth = (res.getConfiguration().smallestScreenWidthDp * res.getDisplayMetrics().densityDpi) / originalDensity;
            allowRotationPref = originalSmallestWidth >= 600;
        }
        return getPrefs(context).getBoolean("pref_allowRotation", allowRotationPref);
    }

    public static Bitmap createIconBitmap(Cursor c, int iconIndex, Context context) {
        byte[] data = c.getBlob(iconIndex);
        try {
            return createIconBitmap(BitmapFactory.decodeByteArray(data, 0, data.length), context);
        } catch (Exception e) {
            return null;
        }
    }

    public static Bitmap createIconBitmap(String packageName, String resourceName, Context context) {
        PackageManager packageManager = context.getPackageManager();
        try {
            Resources resources = packageManager.getResourcesForApplication(packageName);
            if (resources != null) {
                int id = resources.getIdentifier(resourceName, null, null);
                return createIconBitmap(resources.getDrawableForDensity(id, LauncherAppState.getInstance().getInvariantDeviceProfile().fillResIconDpi), context);
            }
        } catch (Exception e) {
        }
        return null;
    }

    private static int getIconBitmapSize() {
        return LauncherAppState.getInstance().getInvariantDeviceProfile().iconBitmapSize;
    }

    public static Bitmap createIconBitmap(Bitmap icon, Context context) {
        int iconBitmapSize = getIconBitmapSize();
        if (iconBitmapSize == icon.getWidth() && iconBitmapSize == icon.getHeight()) {
            return icon;
        }
        return createIconBitmap(new BitmapDrawable(context.getResources(), icon), context);
    }

    @TargetApi(21)
    public static Bitmap createBadgedIconBitmap(Drawable icon, UserHandleCompat user, Context context) {
        float scale = FeatureFlags.LAUNCHER3_ICON_NORMALIZATION ? IconNormalizer.getInstance().getScale(icon) : 1.0f;
        Bitmap bitmap = createIconBitmap(icon, context, scale);
        if (ATLEAST_LOLLIPOP && user != null && !UserHandleCompat.myUserHandle().equals(user)) {
            BitmapDrawable drawable = new FixedSizeBitmapDrawable(bitmap);
            Drawable badged = context.getPackageManager().getUserBadgedIcon(drawable, user.getUser());
            if (badged instanceof BitmapDrawable) {
                return ((BitmapDrawable) badged).getBitmap();
            }
            return createIconBitmap(badged, context);
        }
        return bitmap;
    }

    public static Bitmap createIconBitmap(Drawable icon, Context context) {
        return createIconBitmap(icon, context, 1.0f);
    }

    public static Bitmap createIconBitmap(Drawable icon, Context context, float scale) {
        BitmapDrawable bitmapDrawable;
        Bitmap bitmap;
        Bitmap bitmap2;
        synchronized (sCanvas) {
            int iconBitmapSize = getIconBitmapSize();
            int width = iconBitmapSize;
            int height = iconBitmapSize;
            if (icon instanceof PaintDrawable) {
                PaintDrawable painter = (PaintDrawable) icon;
                painter.setIntrinsicWidth(iconBitmapSize);
                painter.setIntrinsicHeight(iconBitmapSize);
            } else if ((icon instanceof BitmapDrawable) && (bitmap = (bitmapDrawable = (BitmapDrawable) icon).getBitmap()) != null && bitmap.getDensity() == 0) {
                bitmapDrawable.setTargetDensity(context.getResources().getDisplayMetrics());
            }
            int sourceWidth = icon.getIntrinsicWidth();
            int sourceHeight = icon.getIntrinsicHeight();
            if (sourceWidth > 0 && sourceHeight > 0) {
                float ratio = sourceWidth / sourceHeight;
                if (sourceWidth > sourceHeight) {
                    height = (int) (iconBitmapSize / ratio);
                } else if (sourceHeight > sourceWidth) {
                    width = (int) (iconBitmapSize * ratio);
                }
            }
            bitmap2 = Bitmap.createBitmap(iconBitmapSize, iconBitmapSize, Bitmap.Config.ARGB_8888);
            Canvas canvas = sCanvas;
            canvas.setBitmap(bitmap2);
            int left = (iconBitmapSize - width) / 2;
            int top = (iconBitmapSize - height) / 2;
            sOldBounds.set(icon.getBounds());
            icon.setBounds(left, top, left + width, top + height);
            canvas.save(1);
            canvas.scale(scale, scale, iconBitmapSize / 2, iconBitmapSize / 2);
            icon.draw(canvas);
            canvas.restore();
            icon.setBounds(sOldBounds);
            canvas.setBitmap(null);
        }
        return bitmap2;
    }

    public static float getDescendantCoordRelativeToParent(View descendant, View root, int[] coord, boolean includeRootScroll) {
        ArrayList<View> ancestorChain = new ArrayList<>();
        float[] pt = {coord[0], coord[1]};
        for (View v = descendant; v != root && v != null; v = (View) v.getParent()) {
            ancestorChain.add(v);
        }
        ancestorChain.add(root);
        float scale = 1.0f;
        int count = ancestorChain.size();
        for (int i = 0; i < count; i++) {
            View v0 = ancestorChain.get(i);
            if (v0 != descendant || includeRootScroll) {
                pt[0] = pt[0] - v0.getScrollX();
                pt[1] = pt[1] - v0.getScrollY();
            }
            v0.getMatrix().mapPoints(pt);
            pt[0] = pt[0] + v0.getLeft();
            pt[1] = pt[1] + v0.getTop();
            scale *= v0.getScaleX();
        }
        coord[0] = Math.round(pt[0]);
        coord[1] = Math.round(pt[1]);
        return scale;
    }

    public static float mapCoordInSelfToDescendent(View descendant, View root, int[] coord) {
        ArrayList<View> ancestorChain = new ArrayList<>();
        float[] pt = {coord[0], coord[1]};
        for (View v = descendant; v != root; v = (View) v.getParent()) {
            ancestorChain.add(v);
        }
        ancestorChain.add(root);
        float scale = 1.0f;
        Matrix inverse = new Matrix();
        int count = ancestorChain.size();
        int i = count - 1;
        while (i >= 0) {
            View ancestor = ancestorChain.get(i);
            View view = i > 0 ? ancestorChain.get(i - 1) : null;
            pt[0] = pt[0] + ancestor.getScrollX();
            pt[1] = pt[1] + ancestor.getScrollY();
            if (view != null) {
                pt[0] = pt[0] - view.getLeft();
                pt[1] = pt[1] - view.getTop();
                view.getMatrix().invert(inverse);
                inverse.mapPoints(pt);
                scale *= view.getScaleX();
            }
            i--;
        }
        coord[0] = Math.round(pt[0]);
        coord[1] = Math.round(pt[1]);
        return scale;
    }

    public static boolean pointInView(View v, float localX, float localY, float slop) {
        return localX >= (-slop) && localY >= (-slop) && localX < ((float) v.getWidth()) + slop && localY < ((float) v.getHeight()) + slop;
    }

    public static void scaleRect(Rect r, float scale) {
        if (scale == 1.0f) {
            return;
        }
        r.left = (int) ((r.left * scale) + 0.5f);
        r.top = (int) ((r.top * scale) + 0.5f);
        r.right = (int) ((r.right * scale) + 0.5f);
        r.bottom = (int) ((r.bottom * scale) + 0.5f);
    }

    public static int[] getCenterDeltaInScreenSpace(View v0, View v1, int[] delta) {
        v0.getLocationInWindow(sLoc0);
        v1.getLocationInWindow(sLoc1);
        sLoc0[0] = (int) (r0[0] + ((v0.getMeasuredWidth() * v0.getScaleX()) / 2.0f));
        sLoc0[1] = (int) (r0[1] + ((v0.getMeasuredHeight() * v0.getScaleY()) / 2.0f));
        sLoc1[0] = (int) (r0[0] + ((v1.getMeasuredWidth() * v1.getScaleX()) / 2.0f));
        sLoc1[1] = (int) (r0[1] + ((v1.getMeasuredHeight() * v1.getScaleY()) / 2.0f));
        if (delta == null) {
            delta = new int[2];
        }
        delta[0] = sLoc1[0] - sLoc0[0];
        delta[1] = sLoc1[1] - sLoc0[1];
        return delta;
    }

    public static void scaleRectAboutCenter(Rect r, float scale) {
        int cx = r.centerX();
        int cy = r.centerY();
        r.offset(-cx, -cy);
        scaleRect(r, scale);
        r.offset(cx, cy);
    }

    public static void startActivityForResultSafely(Activity activity, Intent intent, int requestCode) {
        try {
            activity.startActivityForResult(intent, requestCode);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(activity, R.string.activity_not_found, 0).show();
        } catch (SecurityException e2) {
            Toast.makeText(activity, R.string.activity_not_found, 0).show();
            Log.e("Launcher.Utilities", "Launcher does not have the permission to launch " + intent + ". Make sure to create a MAIN intent-filter for the corresponding activity or use the exported attribute for this activity.", e2);
        }
    }

    static boolean isSystemApp(Context context, Intent intent) {
        PackageManager pm = context.getPackageManager();
        ComponentName cn = intent.getComponent();
        String packageName = null;
        if (cn == null) {
            ResolveInfo info = pm.resolveActivity(intent, 65536);
            if (info != null && info.activityInfo != null) {
                packageName = info.activityInfo.packageName;
            }
        } else {
            packageName = cn.getPackageName();
        }
        if (packageName == null) {
            return false;
        }
        try {
            PackageInfo info2 = pm.getPackageInfo(packageName, 0);
            if (info2 == null || info2.applicationInfo == null) {
                return false;
            }
            return (info2.applicationInfo.flags & 1) != 0;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    static int findDominantColorByHue(Bitmap bitmap, int samples) {
        int height = bitmap.getHeight();
        int width = bitmap.getWidth();
        int sampleStride = (int) Math.sqrt((height * width) / samples);
        if (sampleStride < 1) {
            sampleStride = 1;
        }
        float[] hsv = new float[3];
        float[] hueScoreHistogram = new float[360];
        float highScore = -1.0f;
        int bestHue = -1;
        for (int y = 0; y < height; y += sampleStride) {
            for (int x = 0; x < width; x += sampleStride) {
                int argb = bitmap.getPixel(x, y);
                int alpha = (argb >> 24) & 255;
                if (alpha >= 128) {
                    Color.colorToHSV(argb | (-16777216), hsv);
                    int hue = (int) hsv[0];
                    if (hue >= 0 && hue < hueScoreHistogram.length) {
                        hueScoreHistogram[hue] = hueScoreHistogram[hue] + (hsv[1] * hsv[2]);
                        if (hueScoreHistogram[hue] > highScore) {
                            highScore = hueScoreHistogram[hue];
                            bestHue = hue;
                        }
                    }
                }
            }
        }
        SparseArray<Float> rgbScores = new SparseArray<>();
        int bestColor = -16777216;
        float highScore2 = -1.0f;
        for (int y2 = 0; y2 < height; y2 += sampleStride) {
            for (int x2 = 0; x2 < width; x2 += sampleStride) {
                int rgb = bitmap.getPixel(x2, y2) | (-16777216);
                Color.colorToHSV(rgb, hsv);
                if (((int) hsv[0]) == bestHue) {
                    float s = hsv[1];
                    float v = hsv[2];
                    int bucket = ((int) (100.0f * s)) + ((int) (10000.0f * v));
                    float score = s * v;
                    Float oldTotal = rgbScores.get(bucket);
                    float newTotal = oldTotal == null ? score : oldTotal.floatValue() + score;
                    rgbScores.put(bucket, Float.valueOf(newTotal));
                    if (newTotal > highScore2) {
                        highScore2 = newTotal;
                        bestColor = rgb;
                    }
                }
            }
        }
        return bestColor;
    }

    static Pair<String, Resources> findSystemApk(String action, PackageManager pm) {
        Intent intent = new Intent(action);
        for (ResolveInfo info : pm.queryBroadcastReceivers(intent, 0)) {
            if (info.activityInfo != null && (info.activityInfo.applicationInfo.flags & 1) != 0) {
                String packageName = info.activityInfo.packageName;
                try {
                    Resources res = pm.getResourcesForApplication(packageName);
                    return Pair.create(packageName, res);
                } catch (PackageManager.NameNotFoundException e) {
                    Log.w("Launcher.Utilities", "Failed to find resources for " + packageName);
                }
            }
        }
        return null;
    }

    @TargetApi(19)
    public static boolean isViewAttachedToWindow(View v) {
        if (ATLEAST_KITKAT) {
            return v.isAttachedToWindow();
        }
        return v.getKeyDispatcherState() != null;
    }

    @TargetApi(17)
    public static AppWidgetProviderInfo getSearchWidgetProvider(Context context) {
        SearchManager searchManager = (SearchManager) context.getSystemService("search");
        ComponentName searchComponent = searchManager.getGlobalSearchActivity();
        if (searchComponent == null) {
            return null;
        }
        String providerPkg = searchComponent.getPackageName();
        AppWidgetProviderInfo defaultWidgetForSearchPackage = null;
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        for (AppWidgetProviderInfo info : appWidgetManager.getInstalledProviders()) {
            if (info.provider.getPackageName().equals(providerPkg)) {
                if (ATLEAST_JB_MR1) {
                    if ((info.widgetCategory & 4) != 0) {
                        return info;
                    }
                    if (defaultWidgetForSearchPackage == null) {
                        defaultWidgetForSearchPackage = info;
                    }
                } else {
                    return info;
                }
            }
        }
        return defaultWidgetForSearchPackage;
    }

    public static byte[] flattenBitmap(Bitmap bitmap) {
        int size = bitmap.getWidth() * bitmap.getHeight() * 4;
        ByteArrayOutputStream out = new ByteArrayOutputStream(size);
        try {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.flush();
            out.close();
            return out.toByteArray();
        } catch (IOException e) {
            Log.w("Launcher.Utilities", "Could not write bitmap");
            return null;
        }
    }

    public static boolean findVacantCell(int[] vacant, int spanX, int spanY, int xCount, int yCount, boolean[][] occupied) {
        for (int y = 0; y + spanY <= yCount; y++) {
            for (int x = 0; x + spanX <= xCount; x++) {
                boolean available = !occupied[x][y];
                for (int i = x; i < x + spanX; i++) {
                    for (int j = y; j < y + spanY; j++) {
                        available = available && !occupied[i][j];
                        if (!available) {
                            break;
                        }
                    }
                }
                if (available) {
                    vacant[0] = x;
                    vacant[1] = y;
                    return true;
                }
            }
        }
        return false;
    }

    public static String trim(CharSequence s) {
        if (s == null) {
            return null;
        }
        Matcher m = sTrimPattern.matcher(s);
        return m.replaceAll("$1");
    }

    @TargetApi(17)
    public static boolean isRtl(Resources res) {
        return ATLEAST_JB_MR1 && res.getConfiguration().getLayoutDirection() == 1;
    }

    public static void assertWorkerThread() {
        if (!LauncherAppState.isDogfoodBuild() || LauncherModel.sWorkerThread.getThreadId() == Process.myTid()) {
        } else {
            throw new IllegalStateException();
        }
    }

    public static boolean isLauncherAppTarget(Intent launchIntent) {
        if (launchIntent == null || !"android.intent.action.MAIN".equals(launchIntent.getAction()) || launchIntent.getComponent() == null || launchIntent.getCategories() == null || launchIntent.getCategories().size() != 1 || !launchIntent.hasCategory("android.intent.category.LAUNCHER") || !TextUtils.isEmpty(launchIntent.getDataString())) {
            return false;
        }
        Bundle extras = launchIntent.getExtras();
        if (extras == null) {
            return true;
        }
        Set<String> keys = extras.keySet();
        if (keys.size() == 1) {
            return keys.contains("profile");
        }
        return false;
    }

    public static float dpiFromPx(int size, DisplayMetrics metrics) {
        float densityRatio = metrics.densityDpi / 160.0f;
        return size / densityRatio;
    }

    public static int pxFromDp(float size, DisplayMetrics metrics) {
        return Math.round(TypedValue.applyDimension(1, size, metrics));
    }

    public static int pxFromSp(float size, DisplayMetrics metrics) {
        return Math.round(TypedValue.applyDimension(2, size, metrics));
    }

    public static String createDbSelectionQuery(String columnName, Iterable<?> values) {
        return String.format(Locale.ENGLISH, "%s IN (%s)", columnName, TextUtils.join(", ", values));
    }

    @TargetApi(21)
    public static CharSequence wrapForTts(CharSequence msg, String ttsMsg) {
        if (ATLEAST_LOLLIPOP) {
            SpannableString spanned = new SpannableString(msg);
            spanned.setSpan(new TtsSpan.TextBuilder(ttsMsg).build(), 0, spanned.length(), 18);
            return spanned;
        }
        return msg;
    }

    public static int longCompare(long lhs, long rhs) {
        if (lhs < rhs) {
            return -1;
        }
        return lhs == rhs ? 0 : 1;
    }

    public static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences("com.android.launcher3.prefs", 0);
    }

    @TargetApi(21)
    public static boolean isPowerSaverOn(Context context) {
        PowerManager powerManager = (PowerManager) context.getSystemService("power");
        if (ATLEAST_LOLLIPOP) {
            return powerManager.isPowerSaveMode();
        }
        return false;
    }

    public static boolean isWallapaperAllowed(Context context) {
        if (ATLEAST_N) {
            return ((WallpaperManager) context.getSystemService(WallpaperManager.class)).isSetWallpaperAllowed();
        }
        return true;
    }

    private static class FixedSizeBitmapDrawable extends BitmapDrawable {
        public FixedSizeBitmapDrawable(Bitmap bitmap) {
            super((Resources) null, bitmap);
        }

        @Override
        public int getIntrinsicHeight() {
            return getBitmap().getWidth();
        }

        @Override
        public int getIntrinsicWidth() {
            return getBitmap().getWidth();
        }
    }
}
