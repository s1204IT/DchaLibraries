package android.app;

import android.annotation.SystemApi;
import android.app.IWallpaperManagerCallback;
import android.app.WallpaperManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.DeadSystemException;
import android.os.FileUtils;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import android.view.WindowManagerGlobal;
import com.android.internal.R;
import com.mediatek.wallpaper.MtkWallpaperFactory;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import libcore.io.IoUtils;

public class WallpaperManager {
    public static final String ACTION_CHANGE_LIVE_WALLPAPER = "android.service.wallpaper.CHANGE_LIVE_WALLPAPER";
    public static final String ACTION_CROP_AND_SET_WALLPAPER = "android.service.wallpaper.CROP_AND_SET_WALLPAPER";
    public static final String ACTION_LIVE_WALLPAPER_CHOOSER = "android.service.wallpaper.LIVE_WALLPAPER_CHOOSER";
    public static final String COMMAND_DROP = "android.home.drop";
    public static final String COMMAND_SECONDARY_TAP = "android.wallpaper.secondaryTap";
    public static final String COMMAND_TAP = "android.wallpaper.tap";
    public static final String EXTRA_LIVE_WALLPAPER_COMPONENT = "android.service.wallpaper.extra.LIVE_WALLPAPER_COMPONENT";
    public static final String EXTRA_NEW_WALLPAPER_ID = "android.service.wallpaper.extra.ID";
    public static final int FLAG_LOCK = 2;
    public static final int FLAG_SYSTEM = 1;
    private static final String PROP_LOCK_WALLPAPER = "ro.config.lock_wallpaper";
    private static final String PROP_WALLPAPER = "ro.config.wallpaper";
    private static final String PROP_WALLPAPER_COMPONENT = "ro.config.wallpaper_component";
    public static final String WALLPAPER_PREVIEW_META_DATA = "android.wallpaper.preview";
    private static Globals sGlobals;
    private final Context mContext;
    private float mWallpaperXStep = -1.0f;
    private float mWallpaperYStep = -1.0f;
    private static String TAG = "WallpaperManager";
    private static boolean DEBUG = false;
    private static MtkWallpaperFactory mMtkWallpaperFactory = MtkWallpaperFactory.getInstance();
    private static final Object sSync = new Object[0];

    @Retention(RetentionPolicy.SOURCE)
    public @interface SetWallpaperFlags {
    }

    static class FastBitmapDrawable extends Drawable {
        private final Bitmap mBitmap;
        private int mDrawLeft;
        private int mDrawTop;
        private final int mHeight;
        private final Paint mPaint;
        private final int mWidth;

        private FastBitmapDrawable(Bitmap bitmap) {
            this.mBitmap = bitmap;
            this.mWidth = bitmap.getWidth();
            this.mHeight = bitmap.getHeight();
            setBounds(0, 0, this.mWidth, this.mHeight);
            this.mPaint = new Paint();
            this.mPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC));
        }

        @Override
        public void draw(Canvas canvas) {
            canvas.drawBitmap(this.mBitmap, this.mDrawLeft, this.mDrawTop, this.mPaint);
        }

        @Override
        public int getOpacity() {
            return -1;
        }

        @Override
        public void setBounds(int i, int i2, int i3, int i4) {
            this.mDrawLeft = i + (((i3 - i) - this.mWidth) / 2);
            this.mDrawTop = i2 + (((i4 - i2) - this.mHeight) / 2);
        }

        @Override
        public void setAlpha(int i) {
            throw new UnsupportedOperationException("Not supported with this drawable");
        }

        @Override
        public void setColorFilter(ColorFilter colorFilter) {
            throw new UnsupportedOperationException("Not supported with this drawable");
        }

        @Override
        public void setDither(boolean z) {
            throw new UnsupportedOperationException("Not supported with this drawable");
        }

        @Override
        public void setFilterBitmap(boolean z) {
            throw new UnsupportedOperationException("Not supported with this drawable");
        }

        @Override
        public int getIntrinsicWidth() {
            return this.mWidth;
        }

        @Override
        public int getIntrinsicHeight() {
            return this.mHeight;
        }

        @Override
        public int getMinimumWidth() {
            return this.mWidth;
        }

        @Override
        public int getMinimumHeight() {
            return this.mHeight;
        }
    }

    private static class Globals extends IWallpaperManagerCallback.Stub {
        private Bitmap mCachedWallpaper;
        private int mCachedWallpaperUserId;
        private boolean mColorCallbackRegistered;
        private final ArrayList<Pair<OnColorsChangedListener, Handler>> mColorListeners = new ArrayList<>();
        private Bitmap mDefaultWallpaper;
        private Handler mMainLooperHandler;
        private final IWallpaperManager mService;

        Globals(IWallpaperManager iWallpaperManager, Looper looper) {
            this.mService = iWallpaperManager;
            this.mMainLooperHandler = new Handler(looper);
            forgetLoadedWallpaper();
        }

        @Override
        public void onWallpaperChanged() {
            forgetLoadedWallpaper();
        }

        public void addOnColorsChangedListener(OnColorsChangedListener onColorsChangedListener, Handler handler, int i) {
            synchronized (this) {
                if (!this.mColorCallbackRegistered) {
                    try {
                        this.mService.registerWallpaperColorsCallback(this, i);
                        this.mColorCallbackRegistered = true;
                    } catch (RemoteException e) {
                        Log.w(WallpaperManager.TAG, "Can't register for color updates", e);
                    }
                    this.mColorListeners.add(new Pair<>(onColorsChangedListener, handler));
                } else {
                    this.mColorListeners.add(new Pair<>(onColorsChangedListener, handler));
                }
            }
        }

        public void removeOnColorsChangedListener(final OnColorsChangedListener onColorsChangedListener, int i) {
            synchronized (this) {
                this.mColorListeners.removeIf(new Predicate() {
                    @Override
                    public final boolean test(Object obj) {
                        return WallpaperManager.Globals.lambda$removeOnColorsChangedListener$0(onColorsChangedListener, (Pair) obj);
                    }
                });
                if (this.mColorListeners.size() == 0 && this.mColorCallbackRegistered) {
                    this.mColorCallbackRegistered = false;
                    try {
                        this.mService.unregisterWallpaperColorsCallback(this, i);
                    } catch (RemoteException e) {
                        Log.w(WallpaperManager.TAG, "Can't unregister color updates", e);
                    }
                }
            }
        }

        static boolean lambda$removeOnColorsChangedListener$0(OnColorsChangedListener onColorsChangedListener, Pair pair) {
            return pair.first == onColorsChangedListener;
        }

        @Override
        public void onWallpaperColorsChanged(final WallpaperColors wallpaperColors, final int i, final int i2) {
            synchronized (this) {
                for (final Pair<OnColorsChangedListener, Handler> pair : this.mColorListeners) {
                    Handler handler = pair.second;
                    if (pair.second == null) {
                        handler = this.mMainLooperHandler;
                    }
                    handler.post(new Runnable() {
                        @Override
                        public final void run() {
                            WallpaperManager.Globals.lambda$onWallpaperColorsChanged$1(this.f$0, pair, wallpaperColors, i, i2);
                        }
                    });
                }
            }
        }

        public static void lambda$onWallpaperColorsChanged$1(Globals globals, Pair pair, WallpaperColors wallpaperColors, int i, int i2) {
            boolean zContains;
            synchronized (WallpaperManager.sGlobals) {
                zContains = globals.mColorListeners.contains(pair);
            }
            if (zContains) {
                ((OnColorsChangedListener) pair.first).onColorsChanged(wallpaperColors, i, i2);
            }
        }

        WallpaperColors getWallpaperColors(int i, int i2) {
            if (i != 2 && i != 1) {
                throw new IllegalArgumentException("Must request colors for exactly one kind of wallpaper");
            }
            try {
                return this.mService.getWallpaperColors(i, i2);
            } catch (RemoteException e) {
                return null;
            }
        }

        public Bitmap peekWallpaperBitmap(Context context, boolean z, int i) {
            return peekWallpaperBitmap(context, z, i, context.getUserId(), false);
        }

        public Bitmap peekWallpaperBitmap(Context context, boolean z, int i, int i2, boolean z2) {
            if (this.mService != null) {
                try {
                    if (!this.mService.isWallpaperSupported(context.getOpPackageName())) {
                        return null;
                    }
                } catch (RemoteException e) {
                    throw e.rethrowFromSystemServer();
                }
            }
            synchronized (this) {
                if (this.mCachedWallpaper != null && this.mCachedWallpaperUserId == i2 && !this.mCachedWallpaper.isRecycled()) {
                    return this.mCachedWallpaper;
                }
                this.mCachedWallpaper = null;
                this.mCachedWallpaperUserId = 0;
                try {
                    try {
                        this.mCachedWallpaper = getCurrentWallpaperLocked(context, i2, z2);
                        this.mCachedWallpaperUserId = i2;
                    } catch (SecurityException e2) {
                        if (context.getApplicationInfo().targetSdkVersion < 27) {
                            Log.w(WallpaperManager.TAG, "No permission to access wallpaper, suppressing exception to avoid crashing legacy app.");
                        } else {
                            throw e2;
                        }
                    }
                } catch (OutOfMemoryError e3) {
                    Log.w(WallpaperManager.TAG, "Out of memory loading the current wallpaper: " + e3);
                }
                if (this.mCachedWallpaper != null) {
                    return this.mCachedWallpaper;
                }
                if (!z) {
                    return null;
                }
                Bitmap defaultWallpaper = this.mDefaultWallpaper;
                if (defaultWallpaper == null) {
                    defaultWallpaper = getDefaultWallpaper(context, i);
                    synchronized (this) {
                        this.mDefaultWallpaper = defaultWallpaper;
                    }
                }
                return defaultWallpaper;
            }
        }

        void forgetLoadedWallpaper() {
            synchronized (this) {
                this.mCachedWallpaper = null;
                this.mCachedWallpaperUserId = 0;
                this.mDefaultWallpaper = null;
            }
        }

        private Bitmap getCurrentWallpaperLocked(Context context, int i, boolean z) {
            if (this.mService == null) {
                Log.w(WallpaperManager.TAG, "WallpaperService not running");
                return null;
            }
            try {
                ParcelFileDescriptor wallpaper = this.mService.getWallpaper(context.getOpPackageName(), this, 1, new Bundle(), i);
                if (wallpaper != null) {
                    try {
                        try {
                            BitmapFactory.Options options = new BitmapFactory.Options();
                            if (z) {
                                options.inPreferredConfig = Bitmap.Config.HARDWARE;
                            }
                            return BitmapFactory.decodeFileDescriptor(wallpaper.getFileDescriptor(), null, options);
                        } catch (OutOfMemoryError e) {
                            Log.w(WallpaperManager.TAG, "Can't decode file", e);
                            IoUtils.closeQuietly(wallpaper);
                            return null;
                        }
                    } finally {
                        IoUtils.closeQuietly(wallpaper);
                    }
                }
                return null;
            } catch (RemoteException e2) {
                throw e2.rethrowFromSystemServer();
            }
        }

        private Bitmap getDefaultWallpaper(Context context, int i) {
            InputStream inputStreamOpenDefaultWallpaper = WallpaperManager.openDefaultWallpaper(context, i);
            if (inputStreamOpenDefaultWallpaper != null) {
                try {
                    return BitmapFactory.decodeStream(inputStreamOpenDefaultWallpaper, null, new BitmapFactory.Options());
                } catch (OutOfMemoryError e) {
                    Log.w(WallpaperManager.TAG, "Can't decode stream", e);
                    return null;
                } finally {
                    IoUtils.closeQuietly(inputStreamOpenDefaultWallpaper);
                }
            }
            return null;
        }
    }

    static void initGlobals(IWallpaperManager iWallpaperManager, Looper looper) {
        synchronized (sSync) {
            if (sGlobals == null) {
                sGlobals = new Globals(iWallpaperManager, looper);
            }
        }
    }

    WallpaperManager(IWallpaperManager iWallpaperManager, Context context, Handler handler) {
        this.mContext = context;
        initGlobals(iWallpaperManager, context.getMainLooper());
    }

    public static WallpaperManager getInstance(Context context) {
        return (WallpaperManager) context.getSystemService(Context.WALLPAPER_SERVICE);
    }

    public IWallpaperManager getIWallpaperManager() {
        return sGlobals.mService;
    }

    public Drawable getDrawable() {
        Bitmap bitmapPeekWallpaperBitmap = sGlobals.peekWallpaperBitmap(this.mContext, true, 1);
        if (bitmapPeekWallpaperBitmap != null) {
            BitmapDrawable bitmapDrawable = new BitmapDrawable(this.mContext.getResources(), bitmapPeekWallpaperBitmap);
            bitmapDrawable.setDither(false);
            return bitmapDrawable;
        }
        return null;
    }

    public Drawable getBuiltInDrawable() {
        return getBuiltInDrawable(0, 0, false, 0.0f, 0.0f, 1);
    }

    public Drawable getBuiltInDrawable(int i) {
        return getBuiltInDrawable(0, 0, false, 0.0f, 0.0f, i);
    }

    public Drawable getBuiltInDrawable(int i, int i2, boolean z, float f, float f2) {
        return getBuiltInDrawable(i, i2, z, f, f2, 1);
    }

    public Drawable getBuiltInDrawable(int i, int i2, boolean z, float f, float f2, int i3) {
        RectF rectF;
        BitmapRegionDecoder bitmapRegionDecoderNewInstance;
        Bitmap bitmapCreateBitmap;
        Bitmap bitmapCreateBitmap2;
        if (sGlobals.mService != null) {
            if (i3 != 1 && i3 != 2) {
                throw new IllegalArgumentException("Must request exactly one kind of wallpaper");
            }
            Resources resources = this.mContext.getResources();
            float fMax = Math.max(0.0f, Math.min(1.0f, f));
            float fMax2 = Math.max(0.0f, Math.min(1.0f, f2));
            InputStream inputStreamOpenDefaultWallpaper = openDefaultWallpaper(this.mContext, i3);
            if (inputStreamOpenDefaultWallpaper == null) {
                if (DEBUG) {
                    Log.w(TAG, "default wallpaper stream " + i3 + " is null");
                }
                return null;
            }
            BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStreamOpenDefaultWallpaper);
            if (i <= 0 || i2 <= 0) {
                return new BitmapDrawable(resources, BitmapFactory.decodeStream(bufferedInputStream, null, null));
            }
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(bufferedInputStream, null, options);
            if (options.outWidth != 0 && options.outHeight != 0) {
                int i4 = options.outWidth;
                int i5 = options.outHeight;
                BufferedInputStream bufferedInputStream2 = new BufferedInputStream(openDefaultWallpaper(this.mContext, i3));
                int iMin = Math.min(i4, i);
                int iMin2 = Math.min(i5, i2);
                if (z) {
                    rectF = getMaxCropRect(i4, i5, iMin, iMin2, fMax, fMax2);
                } else {
                    float f3 = (i4 - iMin) * fMax;
                    float f4 = (i5 - iMin2) * fMax2;
                    rectF = new RectF(f3, f4, iMin + f3, iMin2 + f4);
                }
                Rect rect = new Rect();
                rectF.roundOut(rect);
                if (rect.width() <= 0 || rect.height() <= 0) {
                    Log.w(TAG, "crop has bad values for full size image");
                    return null;
                }
                int iMin3 = Math.min(rect.width() / iMin, rect.height() / iMin2);
                try {
                    bitmapRegionDecoderNewInstance = BitmapRegionDecoder.newInstance((InputStream) bufferedInputStream2, true);
                } catch (IOException e) {
                    Log.w(TAG, "cannot open region decoder for default wallpaper");
                    bitmapRegionDecoderNewInstance = null;
                }
                if (bitmapRegionDecoderNewInstance != null) {
                    BitmapFactory.Options options2 = new BitmapFactory.Options();
                    if (iMin3 > 1) {
                        options2.inSampleSize = iMin3;
                    }
                    bitmapCreateBitmap = bitmapRegionDecoderNewInstance.decodeRegion(rect, options2);
                    bitmapRegionDecoderNewInstance.recycle();
                } else {
                    bitmapCreateBitmap = null;
                }
                if (bitmapCreateBitmap == null) {
                    BufferedInputStream bufferedInputStream3 = new BufferedInputStream(openDefaultWallpaper(this.mContext, i3));
                    BitmapFactory.Options options3 = new BitmapFactory.Options();
                    if (iMin3 > 1) {
                        options3.inSampleSize = iMin3;
                    }
                    Bitmap bitmapDecodeStream = BitmapFactory.decodeStream(bufferedInputStream3, null, options3);
                    if (bitmapDecodeStream != null) {
                        bitmapCreateBitmap = Bitmap.createBitmap(bitmapDecodeStream, rect.left, rect.top, rect.width(), rect.height());
                    }
                }
                if (bitmapCreateBitmap == null) {
                    Log.w(TAG, "cannot decode default wallpaper");
                    return null;
                }
                if (iMin > 0 && iMin2 > 0 && (bitmapCreateBitmap.getWidth() != iMin || bitmapCreateBitmap.getHeight() != iMin2)) {
                    Matrix matrix = new Matrix();
                    RectF rectF2 = new RectF(0.0f, 0.0f, bitmapCreateBitmap.getWidth(), bitmapCreateBitmap.getHeight());
                    RectF rectF3 = new RectF(0.0f, 0.0f, iMin, iMin2);
                    matrix.setRectToRect(rectF2, rectF3, Matrix.ScaleToFit.FILL);
                    bitmapCreateBitmap2 = Bitmap.createBitmap((int) rectF3.width(), (int) rectF3.height(), Bitmap.Config.ARGB_8888);
                    if (bitmapCreateBitmap2 != null) {
                        Canvas canvas = new Canvas(bitmapCreateBitmap2);
                        Paint paint = new Paint();
                        paint.setFilterBitmap(true);
                        canvas.drawBitmap(bitmapCreateBitmap, matrix, paint);
                    }
                } else {
                    bitmapCreateBitmap2 = bitmapCreateBitmap;
                }
                return new BitmapDrawable(resources, bitmapCreateBitmap2);
            }
            Log.e(TAG, "default wallpaper dimensions are 0");
            return null;
        }
        Log.w(TAG, "WallpaperService not running");
        throw new RuntimeException(new DeadSystemException());
    }

    private static RectF getMaxCropRect(int i, int i2, int i3, int i4, float f, float f2) {
        RectF rectF = new RectF();
        float f3 = i;
        float f4 = i2;
        float f5 = i3;
        float f6 = i4;
        if (f3 / f4 > f5 / f6) {
            rectF.top = 0.0f;
            rectF.bottom = f4;
            float f7 = f5 * (f4 / f6);
            rectF.left = (f3 - f7) * f;
            rectF.right = rectF.left + f7;
        } else {
            rectF.left = 0.0f;
            rectF.right = f3;
            float f8 = f6 * (f3 / f5);
            rectF.top = (f4 - f8) * f2;
            rectF.bottom = rectF.top + f8;
        }
        return rectF;
    }

    public Drawable peekDrawable() {
        Bitmap bitmapPeekWallpaperBitmap = sGlobals.peekWallpaperBitmap(this.mContext, false, 1);
        if (bitmapPeekWallpaperBitmap != null) {
            BitmapDrawable bitmapDrawable = new BitmapDrawable(this.mContext.getResources(), bitmapPeekWallpaperBitmap);
            bitmapDrawable.setDither(false);
            return bitmapDrawable;
        }
        return null;
    }

    public Drawable getFastDrawable() {
        Bitmap bitmapPeekWallpaperBitmap = sGlobals.peekWallpaperBitmap(this.mContext, true, 1);
        if (bitmapPeekWallpaperBitmap == null) {
            return null;
        }
        return new FastBitmapDrawable(bitmapPeekWallpaperBitmap);
    }

    public Drawable peekFastDrawable() {
        Bitmap bitmapPeekWallpaperBitmap = sGlobals.peekWallpaperBitmap(this.mContext, false, 1);
        if (bitmapPeekWallpaperBitmap == null) {
            return null;
        }
        return new FastBitmapDrawable(bitmapPeekWallpaperBitmap);
    }

    public Bitmap getBitmap() {
        return getBitmap(false);
    }

    public Bitmap getBitmap(boolean z) {
        return getBitmapAsUser(this.mContext.getUserId(), z);
    }

    public Bitmap getBitmapAsUser(int i, boolean z) {
        return sGlobals.peekWallpaperBitmap(this.mContext, true, 1, i, z);
    }

    public ParcelFileDescriptor getWallpaperFile(int i) {
        return getWallpaperFile(i, this.mContext.getUserId());
    }

    public void addOnColorsChangedListener(OnColorsChangedListener onColorsChangedListener, Handler handler) {
        addOnColorsChangedListener(onColorsChangedListener, handler, this.mContext.getUserId());
    }

    public void addOnColorsChangedListener(OnColorsChangedListener onColorsChangedListener, Handler handler, int i) {
        sGlobals.addOnColorsChangedListener(onColorsChangedListener, handler, i);
    }

    public void removeOnColorsChangedListener(OnColorsChangedListener onColorsChangedListener) {
        removeOnColorsChangedListener(onColorsChangedListener, this.mContext.getUserId());
    }

    public void removeOnColorsChangedListener(OnColorsChangedListener onColorsChangedListener, int i) {
        sGlobals.removeOnColorsChangedListener(onColorsChangedListener, i);
    }

    public WallpaperColors getWallpaperColors(int i) {
        return getWallpaperColors(i, this.mContext.getUserId());
    }

    public WallpaperColors getWallpaperColors(int i, int i2) {
        return sGlobals.getWallpaperColors(i, i2);
    }

    public ParcelFileDescriptor getWallpaperFile(int i, int i2) {
        if (i == 1 || i == 2) {
            if (sGlobals.mService == null) {
                Log.w(TAG, "WallpaperService not running");
                throw new RuntimeException(new DeadSystemException());
            }
            try {
                return sGlobals.mService.getWallpaper(this.mContext.getOpPackageName(), null, i, new Bundle(), i2);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            } catch (SecurityException e2) {
                if (this.mContext.getApplicationInfo().targetSdkVersion < 27) {
                    Log.w(TAG, "No permission to access wallpaper, suppressing exception to avoid crashing legacy app.");
                    return null;
                }
                throw e2;
            }
        }
        throw new IllegalArgumentException("Must request exactly one kind of wallpaper");
    }

    public void forgetLoadedWallpaper() {
        sGlobals.forgetLoadedWallpaper();
    }

    public WallpaperInfo getWallpaperInfo() {
        try {
            if (sGlobals.mService != null) {
                return sGlobals.mService.getWallpaperInfo(this.mContext.getUserId());
            }
            Log.w(TAG, "WallpaperService not running");
            throw new RuntimeException(new DeadSystemException());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public int getWallpaperId(int i) {
        return getWallpaperIdForUser(i, this.mContext.getUserId());
    }

    public int getWallpaperIdForUser(int i, int i2) {
        try {
            if (sGlobals.mService != null) {
                return sGlobals.mService.getWallpaperIdForUser(i, i2);
            }
            Log.w(TAG, "WallpaperService not running");
            throw new RuntimeException(new DeadSystemException());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public Intent getCropAndSetWallpaperIntent(Uri uri) {
        if (uri == null) {
            throw new IllegalArgumentException("Image URI must not be null");
        }
        if (!"content".equals(uri.getScheme())) {
            throw new IllegalArgumentException("Image URI must be of the content scheme type");
        }
        PackageManager packageManager = this.mContext.getPackageManager();
        Intent intent = new Intent(ACTION_CROP_AND_SET_WALLPAPER, uri);
        intent.addFlags(1);
        ResolveInfo resolveInfoResolveActivity = packageManager.resolveActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), 65536);
        if (resolveInfoResolveActivity != null) {
            intent.setPackage(resolveInfoResolveActivity.activityInfo.packageName);
            if (packageManager.queryIntentActivities(intent, 0).size() > 0) {
                return intent;
            }
        }
        intent.setPackage(this.mContext.getString(R.string.config_wallpaperCropperPackage));
        if (packageManager.queryIntentActivities(intent, 0).size() > 0) {
            return intent;
        }
        throw new IllegalArgumentException("Cannot use passed URI to set wallpaper; check that the type returned by ContentProvider matches image/*");
    }

    public void setResource(int i) throws Throwable {
        setResource(i, 3);
    }

    public int setResource(int i, int i2) throws Throwable {
        ParcelFileDescriptor.AutoCloseOutputStream autoCloseOutputStream;
        if (sGlobals.mService == null) {
            Log.w(TAG, "WallpaperService not running");
            throw new RuntimeException(new DeadSystemException());
        }
        Bundle bundle = new Bundle();
        WallpaperSetCompletion wallpaperSetCompletion = new WallpaperSetCompletion();
        try {
            Resources resources = this.mContext.getResources();
            ParcelFileDescriptor wallpaper = sGlobals.mService.setWallpaper("res:" + resources.getResourceName(i), this.mContext.getOpPackageName(), null, false, bundle, i2, wallpaperSetCompletion, this.mContext.getUserId());
            if (wallpaper != null) {
                try {
                    autoCloseOutputStream = new ParcelFileDescriptor.AutoCloseOutputStream(wallpaper);
                    try {
                        copyStreamToWallpaperFile(resources.openRawResource(i), autoCloseOutputStream);
                        autoCloseOutputStream.close();
                        wallpaperSetCompletion.waitForCompletion();
                        IoUtils.closeQuietly(autoCloseOutputStream);
                    } catch (Throwable th) {
                        th = th;
                        IoUtils.closeQuietly(autoCloseOutputStream);
                        throw th;
                    }
                } catch (Throwable th2) {
                    th = th2;
                    autoCloseOutputStream = null;
                }
            }
            return bundle.getInt(EXTRA_NEW_WALLPAPER_ID, 0);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void setBitmap(Bitmap bitmap) throws IOException {
        setBitmap(bitmap, null, true);
    }

    public int setBitmap(Bitmap bitmap, Rect rect, boolean z) throws IOException {
        return setBitmap(bitmap, rect, z, 3);
    }

    public int setBitmap(Bitmap bitmap, Rect rect, boolean z, int i) throws IOException {
        return setBitmap(bitmap, rect, z, i, this.mContext.getUserId());
    }

    public int setBitmap(Bitmap bitmap, Rect rect, boolean z, int i, int i2) throws Throwable {
        ParcelFileDescriptor.AutoCloseOutputStream autoCloseOutputStream;
        validateRect(rect);
        if (sGlobals.mService == null) {
            Log.w(TAG, "WallpaperService not running");
            throw new RuntimeException(new DeadSystemException());
        }
        Bundle bundle = new Bundle();
        WallpaperSetCompletion wallpaperSetCompletion = new WallpaperSetCompletion();
        try {
            ParcelFileDescriptor wallpaper = sGlobals.mService.setWallpaper(null, this.mContext.getOpPackageName(), rect, z, bundle, i, wallpaperSetCompletion, i2);
            if (wallpaper != null) {
                try {
                    autoCloseOutputStream = new ParcelFileDescriptor.AutoCloseOutputStream(wallpaper);
                    try {
                        bitmap.compress(Bitmap.CompressFormat.PNG, 90, autoCloseOutputStream);
                        autoCloseOutputStream.close();
                        wallpaperSetCompletion.waitForCompletion();
                        IoUtils.closeQuietly(autoCloseOutputStream);
                    } catch (Throwable th) {
                        th = th;
                        IoUtils.closeQuietly(autoCloseOutputStream);
                        throw th;
                    }
                } catch (Throwable th2) {
                    th = th2;
                    autoCloseOutputStream = null;
                }
            }
            return bundle.getInt(EXTRA_NEW_WALLPAPER_ID, 0);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    private final void validateRect(Rect rect) {
        if (rect != null && rect.isEmpty()) {
            throw new IllegalArgumentException("visibleCrop rectangle must be valid and non-empty");
        }
    }

    public void setStream(InputStream inputStream) throws IOException {
        setStream(inputStream, null, true);
    }

    private void copyStreamToWallpaperFile(InputStream inputStream, FileOutputStream fileOutputStream) throws IOException {
        FileUtils.copy(inputStream, fileOutputStream);
    }

    public int setStream(InputStream inputStream, Rect rect, boolean z) throws IOException {
        return setStream(inputStream, rect, z, 3);
    }

    public int setStream(InputStream inputStream, Rect rect, boolean z, int i) throws Throwable {
        ParcelFileDescriptor.AutoCloseOutputStream autoCloseOutputStream;
        validateRect(rect);
        if (sGlobals.mService == null) {
            Log.w(TAG, "WallpaperService not running");
            throw new RuntimeException(new DeadSystemException());
        }
        Bundle bundle = new Bundle();
        WallpaperSetCompletion wallpaperSetCompletion = new WallpaperSetCompletion();
        try {
            ParcelFileDescriptor wallpaper = sGlobals.mService.setWallpaper(null, this.mContext.getOpPackageName(), rect, z, bundle, i, wallpaperSetCompletion, this.mContext.getUserId());
            if (wallpaper != null) {
                try {
                    autoCloseOutputStream = new ParcelFileDescriptor.AutoCloseOutputStream(wallpaper);
                    try {
                        copyStreamToWallpaperFile(inputStream, autoCloseOutputStream);
                        autoCloseOutputStream.close();
                        wallpaperSetCompletion.waitForCompletion();
                        IoUtils.closeQuietly(autoCloseOutputStream);
                    } catch (Throwable th) {
                        th = th;
                        IoUtils.closeQuietly(autoCloseOutputStream);
                        throw th;
                    }
                } catch (Throwable th2) {
                    th = th2;
                    autoCloseOutputStream = null;
                }
            }
            return bundle.getInt(EXTRA_NEW_WALLPAPER_ID, 0);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean hasResourceWallpaper(int i) {
        if (sGlobals.mService == null) {
            Log.w(TAG, "WallpaperService not running");
            throw new RuntimeException(new DeadSystemException());
        }
        try {
            return sGlobals.mService.hasNamedWallpaper("res:" + this.mContext.getResources().getResourceName(i));
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public int getDesiredMinimumWidth() {
        if (sGlobals.mService != null) {
            try {
                return sGlobals.mService.getWidthHint();
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        Log.w(TAG, "WallpaperService not running");
        throw new RuntimeException(new DeadSystemException());
    }

    public int getDesiredMinimumHeight() {
        if (sGlobals.mService != null) {
            try {
                return sGlobals.mService.getHeightHint();
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        Log.w(TAG, "WallpaperService not running");
        throw new RuntimeException(new DeadSystemException());
    }

    public void suggestDesiredDimensions(int i, int i2) {
        int i3 = 0;
        try {
            try {
                i3 = SystemProperties.getInt("sys.max_texture_size", 0);
            } catch (Exception e) {
            }
            if (i3 > 0 && (i > i3 || i2 > i3)) {
                float f = i2 / i;
                if (i > i2) {
                    i2 = (int) (((double) (i3 * f)) + 0.5d);
                    i = i3;
                } else {
                    i = (int) (((double) (i3 / f)) + 0.5d);
                    i2 = i3;
                }
            }
            if (sGlobals.mService == null) {
                Log.w(TAG, "WallpaperService not running");
                throw new RuntimeException(new DeadSystemException());
            }
            sGlobals.mService.setDimensionHints(i, i2, this.mContext.getOpPackageName());
        } catch (RemoteException e2) {
            throw e2.rethrowFromSystemServer();
        }
    }

    public void setDisplayPadding(Rect rect) {
        try {
            if (sGlobals.mService != null) {
                sGlobals.mService.setDisplayPadding(rect, this.mContext.getOpPackageName());
            } else {
                Log.w(TAG, "WallpaperService not running");
                throw new RuntimeException(new DeadSystemException());
            }
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @SystemApi
    public void setDisplayOffset(IBinder iBinder, int i, int i2) {
        try {
            WindowManagerGlobal.getWindowSession().setWallpaperDisplayOffset(iBinder, i, i2);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void clearWallpaper() {
        clearWallpaper(2, this.mContext.getUserId());
        clearWallpaper(1, this.mContext.getUserId());
    }

    @SystemApi
    public void clearWallpaper(int i, int i2) {
        if (sGlobals.mService != null) {
            try {
                sGlobals.mService.clearWallpaper(this.mContext.getOpPackageName(), i, i2);
                return;
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        Log.w(TAG, "WallpaperService not running");
        throw new RuntimeException(new DeadSystemException());
    }

    @SystemApi
    public boolean setWallpaperComponent(ComponentName componentName) {
        return setWallpaperComponent(componentName, this.mContext.getUserId());
    }

    public boolean setWallpaperComponent(ComponentName componentName, int i) {
        if (sGlobals.mService != null) {
            try {
                sGlobals.mService.setWallpaperComponentChecked(componentName, this.mContext.getOpPackageName(), i);
                return true;
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        Log.w(TAG, "WallpaperService not running");
        throw new RuntimeException(new DeadSystemException());
    }

    public void setWallpaperOffsets(IBinder iBinder, float f, float f2) {
        try {
            WindowManagerGlobal.getWindowSession().setWallpaperPosition(iBinder, f, f2, this.mWallpaperXStep, this.mWallpaperYStep);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void setWallpaperOffsetSteps(float f, float f2) {
        this.mWallpaperXStep = f;
        this.mWallpaperYStep = f2;
    }

    public void sendWallpaperCommand(IBinder iBinder, String str, int i, int i2, int i3, Bundle bundle) {
        try {
            WindowManagerGlobal.getWindowSession().sendWallpaperCommand(iBinder, str, i, i2, i3, bundle, false);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean isWallpaperSupported() {
        if (sGlobals.mService != null) {
            try {
                return sGlobals.mService.isWallpaperSupported(this.mContext.getOpPackageName());
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        Log.w(TAG, "WallpaperService not running");
        throw new RuntimeException(new DeadSystemException());
    }

    public boolean isSetWallpaperAllowed() {
        if (sGlobals.mService != null) {
            try {
                return sGlobals.mService.isSetWallpaperAllowed(this.mContext.getOpPackageName());
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        Log.w(TAG, "WallpaperService not running");
        throw new RuntimeException(new DeadSystemException());
    }

    public void clearWallpaperOffsets(IBinder iBinder) {
        try {
            WindowManagerGlobal.getWindowSession().setWallpaperPosition(iBinder, -1.0f, -1.0f, -1.0f, -1.0f);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void clear() throws IOException {
        setStream(openDefaultWallpaper(this.mContext, 1), null, false);
    }

    public void clear(int i) throws IOException {
        if ((i & 1) != 0) {
            clear();
        }
        if ((i & 2) != 0) {
            clearWallpaper(2, this.mContext.getUserId());
        }
    }

    public static InputStream openDefaultWallpaper(Context context, int i) {
        InputStream inputStreamOpenDefaultWallpaper = mMtkWallpaperFactory.openDefaultWallpaper(context, i);
        if (inputStreamOpenDefaultWallpaper != null) {
            return inputStreamOpenDefaultWallpaper;
        }
        if (i == 2) {
            return null;
        }
        String str = SystemProperties.get(PROP_WALLPAPER);
        if (!TextUtils.isEmpty(str)) {
            File file = new File(str);
            if (file.exists()) {
                try {
                    return new FileInputStream(file);
                } catch (IOException e) {
                }
            }
        }
        try {
            return context.getResources().openRawResource(R.drawable.default_wallpaper);
        } catch (Resources.NotFoundException e2) {
            return null;
        }
    }

    public static ComponentName getDefaultWallpaperComponent(Context context) {
        ComponentName componentNameUnflattenFromString;
        ComponentName componentNameUnflattenFromString2;
        String str = SystemProperties.get(PROP_WALLPAPER_COMPONENT);
        if (!TextUtils.isEmpty(str) && (componentNameUnflattenFromString2 = ComponentName.unflattenFromString(str)) != null) {
            return componentNameUnflattenFromString2;
        }
        String string = context.getString(R.string.default_wallpaper_component);
        if (!TextUtils.isEmpty(string) && (componentNameUnflattenFromString = ComponentName.unflattenFromString(string)) != null) {
            return componentNameUnflattenFromString;
        }
        return null;
    }

    public boolean setLockWallpaperCallback(IWallpaperManagerCallback iWallpaperManagerCallback) {
        if (sGlobals.mService != null) {
            try {
                return sGlobals.mService.setLockWallpaperCallback(iWallpaperManagerCallback);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        Log.w(TAG, "WallpaperService not running");
        throw new RuntimeException(new DeadSystemException());
    }

    public boolean isWallpaperBackupEligible(int i) {
        if (sGlobals.mService != null) {
            try {
                return sGlobals.mService.isWallpaperBackupEligible(i, this.mContext.getUserId());
            } catch (RemoteException e) {
                Log.e(TAG, "Exception querying wallpaper backup eligibility: " + e.getMessage());
                return false;
            }
        }
        Log.w(TAG, "WallpaperService not running");
        throw new RuntimeException(new DeadSystemException());
    }

    private class WallpaperSetCompletion extends IWallpaperManagerCallback.Stub {
        final CountDownLatch mLatch = new CountDownLatch(1);

        public WallpaperSetCompletion() {
        }

        public void waitForCompletion() {
            try {
                this.mLatch.await(30L, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
            }
        }

        @Override
        public void onWallpaperChanged() throws RemoteException {
            this.mLatch.countDown();
        }

        @Override
        public void onWallpaperColorsChanged(WallpaperColors wallpaperColors, int i, int i2) throws RemoteException {
            WallpaperManager.sGlobals.onWallpaperColorsChanged(wallpaperColors, i, i2);
        }
    }

    public interface OnColorsChangedListener {
        void onColorsChanged(WallpaperColors wallpaperColors, int i);

        default void onColorsChanged(WallpaperColors wallpaperColors, int i, int i2) {
            onColorsChanged(wallpaperColors, i);
        }
    }
}
