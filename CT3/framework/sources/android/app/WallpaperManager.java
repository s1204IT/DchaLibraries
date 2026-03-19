package android.app;

import android.app.IWallpaperManager;
import android.app.IWallpaperManagerCallback;
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
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.util.Log;
import android.view.WindowManagerGlobal;
import com.mediatek.common.MPlugin;
import com.mediatek.common.wallpaper.IWallpaperPlugin;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
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
    private static final Object sSync = new Object[0];

    static class FastBitmapDrawable extends Drawable {
        private final Bitmap mBitmap;
        private int mDrawLeft;
        private int mDrawTop;
        private final int mHeight;
        private final Paint mPaint;
        private final int mWidth;

        FastBitmapDrawable(Bitmap bitmap, FastBitmapDrawable fastBitmapDrawable) {
            this(bitmap);
        }

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
        public void setBounds(int left, int top, int right, int bottom) {
            this.mDrawLeft = (((right - left) - this.mWidth) / 2) + left;
            this.mDrawTop = (((bottom - top) - this.mHeight) / 2) + top;
        }

        @Override
        public void setAlpha(int alpha) {
            throw new UnsupportedOperationException("Not supported with this drawable");
        }

        @Override
        public void setColorFilter(ColorFilter colorFilter) {
            throw new UnsupportedOperationException("Not supported with this drawable");
        }

        @Override
        public void setDither(boolean dither) {
            throw new UnsupportedOperationException("Not supported with this drawable");
        }

        @Override
        public void setFilterBitmap(boolean filter) {
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

    static class Globals extends IWallpaperManagerCallback.Stub {
        private Bitmap mCachedWallpaper;
        private int mCachedWallpaperUserId;
        private Bitmap mDefaultWallpaper;
        private IWallpaperManager mService;

        Globals(Looper looper) {
            IBinder b = ServiceManager.getService(Context.WALLPAPER_SERVICE);
            this.mService = IWallpaperManager.Stub.asInterface(b);
            forgetLoadedWallpaper();
        }

        @Override
        public void onWallpaperChanged() {
            forgetLoadedWallpaper();
        }

        public Bitmap peekWallpaperBitmap(Context context, boolean returnDefault, int which) {
            return peekWallpaperBitmap(context, returnDefault, which, context.getUserId());
        }

        public Bitmap peekWallpaperBitmap(Context context, boolean returnDefault, int which, int userId) {
            synchronized (this) {
                if (this.mService != null) {
                    try {
                        if (!this.mService.isWallpaperSupported(context.getOpPackageName())) {
                            return null;
                        }
                        if (this.mCachedWallpaper == null && this.mCachedWallpaperUserId == userId) {
                            return this.mCachedWallpaper;
                        }
                        this.mCachedWallpaper = null;
                        this.mCachedWallpaperUserId = 0;
                        try {
                            this.mCachedWallpaper = getCurrentWallpaperLocked(userId);
                            this.mCachedWallpaperUserId = userId;
                        } catch (OutOfMemoryError e) {
                            Log.w(WallpaperManager.TAG, "No memory load current wallpaper", e);
                        }
                        if (this.mCachedWallpaper == null) {
                            return this.mCachedWallpaper;
                        }
                        if (!returnDefault) {
                            return null;
                        }
                        if (this.mDefaultWallpaper == null) {
                            this.mDefaultWallpaper = getDefaultWallpaperLocked(context, which);
                        }
                        return this.mDefaultWallpaper;
                    } catch (RemoteException e2) {
                        throw e2.rethrowFromSystemServer();
                    }
                }
                if (this.mCachedWallpaper == null) {
                }
                this.mCachedWallpaper = null;
                this.mCachedWallpaperUserId = 0;
                this.mCachedWallpaper = getCurrentWallpaperLocked(userId);
                this.mCachedWallpaperUserId = userId;
                if (this.mCachedWallpaper == null) {
                }
            }
        }

        public void forgetLoadedWallpaper() {
            synchronized (this) {
                this.mCachedWallpaper = null;
                this.mCachedWallpaperUserId = 0;
                this.mDefaultWallpaper = null;
            }
        }

        private Bitmap getCurrentWallpaperLocked(int userId) {
            if (this.mService == null) {
                Log.w(WallpaperManager.TAG, "WallpaperService not running");
                return null;
            }
            try {
                Bundle params = new Bundle();
                ParcelFileDescriptor fd = this.mService.getWallpaper(this, 1, params, userId);
                if (fd != null) {
                    try {
                        try {
                            BitmapFactory.Options options = new BitmapFactory.Options();
                            return BitmapFactory.decodeFileDescriptor(fd.getFileDescriptor(), null, options);
                        } catch (OutOfMemoryError e) {
                            Log.w(WallpaperManager.TAG, "Can't decode file", e);
                            return null;
                        }
                    } finally {
                        IoUtils.closeQuietly(fd);
                    }
                }
                return null;
            } catch (RemoteException e2) {
                throw e2.rethrowFromSystemServer();
            }
        }

        private InputStream openDefaultWallpaperRes(Context context, int which) {
            IWallpaperPlugin mWallpaperPlugin = null;
            try {
                mWallpaperPlugin = (IWallpaperPlugin) MPlugin.createInstance(IWallpaperPlugin.class.getName(), context);
            } catch (Exception e) {
                Log.e(WallpaperManager.TAG, "Catch IWallpaperPlugin exception: ", e);
            }
            if (mWallpaperPlugin == null || mWallpaperPlugin.getPluginResources(context) == null) {
                InputStream is = context.getResources().openRawResource(17302107);
                return is;
            }
            Log.d(WallpaperManager.TAG, "get the wallpaper image from the plug-in");
            InputStream is2 = mWallpaperPlugin.getPluginResources(context).openRawResource(mWallpaperPlugin.getPluginDefaultImage());
            return is2;
        }

        private Bitmap getDefaultWallpaperLocked(Context context, int which) {
            InputStream is = openDefaultWallpaperRes(context, which);
            if (is != null) {
                try {
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    return BitmapFactory.decodeStream(is, null, options);
                } catch (OutOfMemoryError e) {
                    Log.w(WallpaperManager.TAG, "Can't decode stream", e);
                    return null;
                } finally {
                    IoUtils.closeQuietly(is);
                }
            }
            return null;
        }
    }

    static void initGlobals(Looper looper) {
        synchronized (sSync) {
            if (sGlobals == null) {
                sGlobals = new Globals(looper);
            }
        }
    }

    WallpaperManager(Context context, Handler handler) {
        this.mContext = context;
        initGlobals(context.getMainLooper());
    }

    public static WallpaperManager getInstance(Context context) {
        return (WallpaperManager) context.getSystemService(Context.WALLPAPER_SERVICE);
    }

    public IWallpaperManager getIWallpaperManager() {
        return sGlobals.mService;
    }

    public Drawable getDrawable() {
        Bitmap bm = sGlobals.peekWallpaperBitmap(this.mContext, true, 1);
        if (bm == null) {
            return null;
        }
        Drawable dr = new BitmapDrawable(this.mContext.getResources(), bm);
        dr.setDither(false);
        return dr;
    }

    public Drawable getBuiltInDrawable() {
        return getBuiltInDrawable(0, 0, false, 0.0f, 0.0f, 1);
    }

    public Drawable getBuiltInDrawable(int which) {
        return getBuiltInDrawable(0, 0, false, 0.0f, 0.0f, which);
    }

    public Drawable getBuiltInDrawable(int outWidth, int outHeight, boolean scaleToFit, float horizontalAlignment, float verticalAlignment) {
        return getBuiltInDrawable(outWidth, outHeight, scaleToFit, horizontalAlignment, verticalAlignment, 1);
    }

    public Drawable getBuiltInDrawable(int outWidth, int outHeight, boolean scaleToFit, float horizontalAlignment, float verticalAlignment, int which) {
        RectF cropRectF;
        if (sGlobals.mService == null) {
            Log.w(TAG, "WallpaperService not running");
            throw new RuntimeException(new DeadSystemException());
        }
        if (which != 1 && which != 2) {
            throw new IllegalArgumentException("Must request exactly one kind of wallpaper");
        }
        Resources resources = this.mContext.getResources();
        float horizontalAlignment2 = Math.max(0.0f, Math.min(1.0f, horizontalAlignment));
        float verticalAlignment2 = Math.max(0.0f, Math.min(1.0f, verticalAlignment));
        InputStream wpStream = sGlobals.openDefaultWallpaperRes(this.mContext, which);
        if (wpStream == null) {
            if (DEBUG) {
                Log.w(TAG, "default wallpaper stream " + which + " is null");
                return null;
            }
            return null;
        }
        InputStream is = new BufferedInputStream(wpStream);
        if (outWidth <= 0 || outHeight <= 0) {
            return new BitmapDrawable(resources, BitmapFactory.decodeStream(is, null, null));
        }
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(is, null, options);
        if (options.outWidth != 0 && options.outHeight != 0) {
            int inWidth = options.outWidth;
            int inHeight = options.outHeight;
            InputStream is2 = new BufferedInputStream(sGlobals.openDefaultWallpaperRes(this.mContext, which));
            int outWidth2 = Math.min(inWidth, outWidth);
            int outHeight2 = Math.min(inHeight, outHeight);
            if (scaleToFit) {
                cropRectF = getMaxCropRect(inWidth, inHeight, outWidth2, outHeight2, horizontalAlignment2, verticalAlignment2);
            } else {
                float left = (inWidth - outWidth2) * horizontalAlignment2;
                float right = left + outWidth2;
                float top = (inHeight - outHeight2) * verticalAlignment2;
                float bottom = top + outHeight2;
                cropRectF = new RectF(left, top, right, bottom);
            }
            Rect roundedTrueCrop = new Rect();
            cropRectF.roundOut(roundedTrueCrop);
            if (roundedTrueCrop.width() <= 0 || roundedTrueCrop.height() <= 0) {
                Log.w(TAG, "crop has bad values for full size image");
                return null;
            }
            int scaleDownSampleSize = Math.min(roundedTrueCrop.width() / outWidth2, roundedTrueCrop.height() / outHeight2);
            BitmapRegionDecoder decoder = null;
            try {
                decoder = BitmapRegionDecoder.newInstance(is2, true);
            } catch (IOException e) {
                Log.w(TAG, "cannot open region decoder for default wallpaper");
            }
            Bitmap crop = null;
            if (decoder != null) {
                BitmapFactory.Options options2 = new BitmapFactory.Options();
                if (scaleDownSampleSize > 1) {
                    options2.inSampleSize = scaleDownSampleSize;
                }
                crop = decoder.decodeRegion(roundedTrueCrop, options2);
                decoder.recycle();
            }
            if (crop == null) {
                InputStream is3 = new BufferedInputStream(sGlobals.openDefaultWallpaperRes(this.mContext, which));
                BitmapFactory.Options options3 = new BitmapFactory.Options();
                if (scaleDownSampleSize > 1) {
                    options3.inSampleSize = scaleDownSampleSize;
                }
                Bitmap fullSize = BitmapFactory.decodeStream(is3, null, options3);
                if (fullSize != null) {
                    crop = Bitmap.createBitmap(fullSize, roundedTrueCrop.left, roundedTrueCrop.top, roundedTrueCrop.width(), roundedTrueCrop.height());
                }
            }
            if (crop == null) {
                Log.w(TAG, "cannot decode default wallpaper");
                return null;
            }
            if (outWidth2 > 0 && outHeight2 > 0 && (crop.getWidth() != outWidth2 || crop.getHeight() != outHeight2)) {
                Matrix m = new Matrix();
                RectF cropRect = new RectF(0.0f, 0.0f, crop.getWidth(), crop.getHeight());
                RectF returnRect = new RectF(0.0f, 0.0f, outWidth2, outHeight2);
                m.setRectToRect(cropRect, returnRect, Matrix.ScaleToFit.FILL);
                Bitmap tmp = Bitmap.createBitmap((int) returnRect.width(), (int) returnRect.height(), Bitmap.Config.ARGB_8888);
                if (tmp != null) {
                    Canvas c = new Canvas(tmp);
                    Paint p = new Paint();
                    p.setFilterBitmap(true);
                    c.drawBitmap(crop, m, p);
                    crop = tmp;
                }
            }
            return new BitmapDrawable(resources, crop);
        }
        Log.e(TAG, "default wallpaper dimensions are 0");
        return null;
    }

    private static RectF getMaxCropRect(int inWidth, int inHeight, int outWidth, int outHeight, float horizontalAlignment, float verticalAlignment) {
        RectF cropRect = new RectF();
        if (inWidth / inHeight > outWidth / outHeight) {
            cropRect.top = 0.0f;
            cropRect.bottom = inHeight;
            float cropWidth = outWidth * (inHeight / outHeight);
            cropRect.left = (inWidth - cropWidth) * horizontalAlignment;
            cropRect.right = cropRect.left + cropWidth;
        } else {
            cropRect.left = 0.0f;
            cropRect.right = inWidth;
            float cropHeight = outHeight * (inWidth / outWidth);
            cropRect.top = (inHeight - cropHeight) * verticalAlignment;
            cropRect.bottom = cropRect.top + cropHeight;
        }
        return cropRect;
    }

    public Drawable peekDrawable() {
        Bitmap bm = sGlobals.peekWallpaperBitmap(this.mContext, false, 1);
        if (bm == null) {
            return null;
        }
        Drawable dr = new BitmapDrawable(this.mContext.getResources(), bm);
        dr.setDither(false);
        return dr;
    }

    public Drawable getFastDrawable() {
        FastBitmapDrawable fastBitmapDrawable = null;
        Bitmap bm = sGlobals.peekWallpaperBitmap(this.mContext, true, 1);
        if (bm != null) {
            return new FastBitmapDrawable(bm, fastBitmapDrawable);
        }
        return null;
    }

    public Drawable peekFastDrawable() {
        FastBitmapDrawable fastBitmapDrawable = null;
        Bitmap bm = sGlobals.peekWallpaperBitmap(this.mContext, false, 1);
        if (bm != null) {
            return new FastBitmapDrawable(bm, fastBitmapDrawable);
        }
        return null;
    }

    public Bitmap getBitmap() {
        return getBitmapAsUser(this.mContext.getUserId());
    }

    public Bitmap getBitmapAsUser(int userId) {
        return sGlobals.peekWallpaperBitmap(this.mContext, true, 1, userId);
    }

    public ParcelFileDescriptor getWallpaperFile(int which) {
        return getWallpaperFile(which, this.mContext.getUserId());
    }

    public ParcelFileDescriptor getWallpaperFile(int which, int userId) {
        if (which != 1 && which != 2) {
            throw new IllegalArgumentException("Must request exactly one kind of wallpaper");
        }
        if (sGlobals.mService == null) {
            Log.w(TAG, "WallpaperService not running");
            throw new RuntimeException(new DeadSystemException());
        }
        try {
            Bundle outParams = new Bundle();
            return sGlobals.mService.getWallpaper(null, which, outParams, userId);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void forgetLoadedWallpaper() {
        sGlobals.forgetLoadedWallpaper();
    }

    public WallpaperInfo getWallpaperInfo() {
        try {
            if (sGlobals.mService == null) {
                Log.w(TAG, "WallpaperService not running");
                throw new RuntimeException(new DeadSystemException());
            }
            return sGlobals.mService.getWallpaperInfo();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public int getWallpaperId(int which) {
        return getWallpaperIdForUser(which, this.mContext.getUserId());
    }

    public int getWallpaperIdForUser(int which, int userId) {
        try {
            if (sGlobals.mService == null) {
                Log.w(TAG, "WallpaperService not running");
                throw new RuntimeException(new DeadSystemException());
            }
            return sGlobals.mService.getWallpaperIdForUser(which, userId);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public Intent getCropAndSetWallpaperIntent(Uri imageUri) {
        if (imageUri == null) {
            throw new IllegalArgumentException("Image URI must not be null");
        }
        if (!"content".equals(imageUri.getScheme())) {
            throw new IllegalArgumentException("Image URI must be of the content scheme type");
        }
        PackageManager packageManager = this.mContext.getPackageManager();
        Intent cropAndSetWallpaperIntent = new Intent(ACTION_CROP_AND_SET_WALLPAPER, imageUri);
        cropAndSetWallpaperIntent.addFlags(1);
        Intent homeIntent = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME);
        ResolveInfo resolvedHome = packageManager.resolveActivity(homeIntent, 65536);
        if (resolvedHome != null) {
            cropAndSetWallpaperIntent.setPackage(resolvedHome.activityInfo.packageName);
            List<ResolveInfo> cropAppList = packageManager.queryIntentActivities(cropAndSetWallpaperIntent, 0);
            if (cropAppList.size() > 0) {
                return cropAndSetWallpaperIntent;
            }
        }
        String cropperPackage = this.mContext.getString(17039471);
        cropAndSetWallpaperIntent.setPackage(cropperPackage);
        List<ResolveInfo> cropAppList2 = packageManager.queryIntentActivities(cropAndSetWallpaperIntent, 0);
        if (cropAppList2.size() > 0) {
            return cropAndSetWallpaperIntent;
        }
        throw new IllegalArgumentException("Cannot use passed URI to set wallpaper; check that the type returned by ContentProvider matches image/*");
    }

    public void setResource(int resid) throws Throwable {
        setResource(resid, 1);
    }

    public int setResource(int resid, int which) throws Throwable {
        if (sGlobals.mService == null) {
            Log.w(TAG, "WallpaperService not running");
            throw new RuntimeException(new DeadSystemException());
        }
        Bundle result = new Bundle();
        WallpaperSetCompletion completion = new WallpaperSetCompletion();
        try {
            Resources resources = this.mContext.getResources();
            ParcelFileDescriptor fd = sGlobals.mService.setWallpaper("res:" + resources.getResourceName(resid), this.mContext.getOpPackageName(), null, false, result, which, completion);
            if (fd != null) {
                FileOutputStream fos = null;
                try {
                    FileOutputStream fos2 = new ParcelFileDescriptor.AutoCloseOutputStream(fd);
                    try {
                        copyStreamToWallpaperFile(resources.openRawResource(resid), fos2);
                        fos2.close();
                        completion.waitForCompletion();
                        IoUtils.closeQuietly(fos2);
                    } catch (Throwable th) {
                        th = th;
                        fos = fos2;
                        IoUtils.closeQuietly(fos);
                        throw th;
                    }
                } catch (Throwable th2) {
                    th = th2;
                }
            }
            return result.getInt(EXTRA_NEW_WALLPAPER_ID, 0);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void setBitmap(Bitmap bitmap) throws IOException {
        setBitmap(bitmap, null, true);
    }

    public int setBitmap(Bitmap fullImage, Rect visibleCropHint, boolean allowBackup) throws IOException {
        return setBitmap(fullImage, visibleCropHint, allowBackup, 1);
    }

    public int setBitmap(Bitmap fullImage, Rect visibleCropHint, boolean allowBackup, int which) throws Throwable {
        validateRect(visibleCropHint);
        if (sGlobals.mService == null) {
            Log.w(TAG, "WallpaperService not running");
            throw new RuntimeException(new DeadSystemException());
        }
        Bundle result = new Bundle();
        WallpaperSetCompletion completion = new WallpaperSetCompletion();
        try {
            ParcelFileDescriptor fd = sGlobals.mService.setWallpaper(null, this.mContext.getOpPackageName(), visibleCropHint, allowBackup, result, which, completion);
            if (fd != null) {
                FileOutputStream fos = null;
                try {
                    FileOutputStream fos2 = new ParcelFileDescriptor.AutoCloseOutputStream(fd);
                    try {
                        fullImage.compress(Bitmap.CompressFormat.PNG, 90, fos2);
                        fos2.close();
                        completion.waitForCompletion();
                        IoUtils.closeQuietly(fos2);
                    } catch (Throwable th) {
                        th = th;
                        fos = fos2;
                        IoUtils.closeQuietly(fos);
                        throw th;
                    }
                } catch (Throwable th2) {
                    th = th2;
                }
            }
            return result.getInt(EXTRA_NEW_WALLPAPER_ID, 0);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    private final void validateRect(Rect rect) {
        if (rect == null || !rect.isEmpty()) {
        } else {
            throw new IllegalArgumentException("visibleCrop rectangle must be valid and non-empty");
        }
    }

    public void setStream(InputStream bitmapData) throws IOException {
        setStream(bitmapData, null, true);
    }

    private void copyStreamToWallpaperFile(InputStream data, FileOutputStream fos) throws IOException {
        byte[] buffer = new byte[32768];
        while (true) {
            int amt = data.read(buffer);
            if (amt <= 0) {
                return;
            } else {
                fos.write(buffer, 0, amt);
            }
        }
    }

    public int setStream(InputStream bitmapData, Rect visibleCropHint, boolean allowBackup) throws IOException {
        return setStream(bitmapData, visibleCropHint, allowBackup, 1);
    }

    public int setStream(InputStream bitmapData, Rect visibleCropHint, boolean allowBackup, int which) throws Throwable {
        validateRect(visibleCropHint);
        if (sGlobals.mService == null) {
            Log.w(TAG, "WallpaperService not running");
            throw new RuntimeException(new DeadSystemException());
        }
        Bundle result = new Bundle();
        WallpaperSetCompletion completion = new WallpaperSetCompletion();
        try {
            ParcelFileDescriptor fd = sGlobals.mService.setWallpaper(null, this.mContext.getOpPackageName(), visibleCropHint, allowBackup, result, which, completion);
            if (fd != null) {
                FileOutputStream fos = null;
                try {
                    FileOutputStream fos2 = new ParcelFileDescriptor.AutoCloseOutputStream(fd);
                    try {
                        copyStreamToWallpaperFile(bitmapData, fos2);
                        fos2.close();
                        completion.waitForCompletion();
                        IoUtils.closeQuietly(fos2);
                    } catch (Throwable th) {
                        th = th;
                        fos = fos2;
                        IoUtils.closeQuietly(fos);
                        throw th;
                    }
                } catch (Throwable th2) {
                    th = th2;
                }
            }
            return result.getInt(EXTRA_NEW_WALLPAPER_ID, 0);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean hasResourceWallpaper(int resid) {
        if (sGlobals.mService == null) {
            Log.w(TAG, "WallpaperService not running");
            throw new RuntimeException(new DeadSystemException());
        }
        try {
            Resources resources = this.mContext.getResources();
            String name = "res:" + resources.getResourceName(resid);
            return sGlobals.mService.hasNamedWallpaper(name);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public int getDesiredMinimumWidth() {
        if (sGlobals.mService == null) {
            Log.w(TAG, "WallpaperService not running");
            throw new RuntimeException(new DeadSystemException());
        }
        try {
            return sGlobals.mService.getWidthHint();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public int getDesiredMinimumHeight() {
        if (sGlobals.mService == null) {
            Log.w(TAG, "WallpaperService not running");
            throw new RuntimeException(new DeadSystemException());
        }
        try {
            return sGlobals.mService.getHeightHint();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void suggestDesiredDimensions(int minimumWidth, int minimumHeight) {
        int maximumTextureSize;
        try {
            try {
                maximumTextureSize = SystemProperties.getInt("sys.max_texture_size", 0);
            } catch (Exception e) {
                maximumTextureSize = 0;
            }
            if (maximumTextureSize > 0 && (minimumWidth > maximumTextureSize || minimumHeight > maximumTextureSize)) {
                float aspect = minimumHeight / minimumWidth;
                if (minimumWidth > minimumHeight) {
                    minimumWidth = maximumTextureSize;
                    minimumHeight = (int) (((double) (maximumTextureSize * aspect)) + 0.5d);
                } else {
                    minimumHeight = maximumTextureSize;
                    minimumWidth = (int) (((double) (maximumTextureSize / aspect)) + 0.5d);
                }
            }
            if (sGlobals.mService == null) {
                Log.w(TAG, "WallpaperService not running");
                throw new RuntimeException(new DeadSystemException());
            }
            sGlobals.mService.setDimensionHints(minimumWidth, minimumHeight, this.mContext.getOpPackageName());
        } catch (RemoteException e2) {
            throw e2.rethrowFromSystemServer();
        }
    }

    public void setDisplayPadding(Rect padding) {
        try {
            if (sGlobals.mService == null) {
                Log.w(TAG, "WallpaperService not running");
                throw new RuntimeException(new DeadSystemException());
            }
            sGlobals.mService.setDisplayPadding(padding, this.mContext.getOpPackageName());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void setDisplayOffset(IBinder windowToken, int x, int y) {
        try {
            WindowManagerGlobal.getWindowSession().setWallpaperDisplayOffset(windowToken, x, y);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void clearWallpaper() {
        clearWallpaper(1, this.mContext.getUserId());
    }

    public void clearWallpaper(int which, int userId) {
        if (sGlobals.mService == null) {
            Log.w(TAG, "WallpaperService not running");
            throw new RuntimeException(new DeadSystemException());
        }
        try {
            sGlobals.mService.clearWallpaper(this.mContext.getOpPackageName(), which, userId);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean setWallpaperComponent(ComponentName name) {
        if (sGlobals.mService == null) {
            Log.w(TAG, "WallpaperService not running");
            throw new RuntimeException(new DeadSystemException());
        }
        try {
            sGlobals.mService.setWallpaperComponentChecked(name, this.mContext.getOpPackageName());
            return true;
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void setWallpaperOffsets(IBinder windowToken, float xOffset, float yOffset) {
        try {
            WindowManagerGlobal.getWindowSession().setWallpaperPosition(windowToken, xOffset, yOffset, this.mWallpaperXStep, this.mWallpaperYStep);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void setWallpaperOffsetSteps(float xStep, float yStep) {
        this.mWallpaperXStep = xStep;
        this.mWallpaperYStep = yStep;
    }

    public void sendWallpaperCommand(IBinder windowToken, String action, int x, int y, int z, Bundle extras) {
        try {
            WindowManagerGlobal.getWindowSession().sendWallpaperCommand(windowToken, action, x, y, z, extras, false);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean isWallpaperSupported() {
        if (sGlobals.mService == null) {
            Log.w(TAG, "WallpaperService not running");
            throw new RuntimeException(new DeadSystemException());
        }
        try {
            return sGlobals.mService.isWallpaperSupported(this.mContext.getOpPackageName());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean isSetWallpaperAllowed() {
        if (sGlobals.mService == null) {
            Log.w(TAG, "WallpaperService not running");
            throw new RuntimeException(new DeadSystemException());
        }
        try {
            return sGlobals.mService.isSetWallpaperAllowed(this.mContext.getOpPackageName());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void clearWallpaperOffsets(IBinder windowToken) {
        try {
            WindowManagerGlobal.getWindowSession().setWallpaperPosition(windowToken, -1.0f, -1.0f, -1.0f, -1.0f);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void clear() throws IOException {
        setStream(sGlobals.openDefaultWallpaperRes(this.mContext, 1), null, false);
    }

    public void clear(int which) throws IOException {
        if ((which & 1) != 0) {
            clear();
        }
        if ((which & 2) == 0) {
            return;
        }
        clearWallpaper(2, this.mContext.getUserId());
    }

    public static InputStream openDefaultWallpaper(Context context, int which) {
        if (which == 2) {
            return null;
        }
        String path = SystemProperties.get(PROP_WALLPAPER);
        if (!TextUtils.isEmpty(path)) {
            File file = new File(path);
            if (file.exists()) {
                try {
                    return new FileInputStream(file);
                } catch (IOException e) {
                }
            }
        }
        try {
            return context.getResources().openRawResource(17302107);
        } catch (Resources.NotFoundException e2) {
            return null;
        }
    }

    public static ComponentName getDefaultWallpaperComponent(Context context) {
        ComponentName cn;
        ComponentName cn2;
        String flat = SystemProperties.get(PROP_WALLPAPER_COMPONENT);
        if (!TextUtils.isEmpty(flat) && (cn2 = ComponentName.unflattenFromString(flat)) != null) {
            return cn2;
        }
        String flat2 = context.getString(17039421);
        if (TextUtils.isEmpty(flat2) || (cn = ComponentName.unflattenFromString(flat2)) == null) {
            return null;
        }
        return cn;
    }

    public boolean setLockWallpaperCallback(IWallpaperManagerCallback callback) {
        if (sGlobals.mService == null) {
            Log.w(TAG, "WallpaperService not running");
            throw new RuntimeException(new DeadSystemException());
        }
        try {
            return sGlobals.mService.setLockWallpaperCallback(callback);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean isWallpaperBackupEligible() {
        if (sGlobals.mService == null) {
            Log.w(TAG, "WallpaperService not running");
            throw new RuntimeException(new DeadSystemException());
        }
        try {
            return sGlobals.mService.isWallpaperBackupEligible(this.mContext.getUserId());
        } catch (RemoteException e) {
            Log.e(TAG, "Exception querying wallpaper backup eligibility: " + e.getMessage());
            return false;
        }
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
    }
}
