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
import com.android.internal.R;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class WallpaperManager {
    public static final String ACTION_CHANGE_LIVE_WALLPAPER = "android.service.wallpaper.CHANGE_LIVE_WALLPAPER";
    public static final String ACTION_CROP_AND_SET_WALLPAPER = "android.service.wallpaper.CROP_AND_SET_WALLPAPER";
    public static final String ACTION_LIVE_WALLPAPER_CHOOSER = "android.service.wallpaper.LIVE_WALLPAPER_CHOOSER";
    public static final String COMMAND_DROP = "android.home.drop";
    public static final String COMMAND_SECONDARY_TAP = "android.wallpaper.secondaryTap";
    public static final String COMMAND_TAP = "android.wallpaper.tap";
    public static final String EXTRA_LIVE_WALLPAPER_COMPONENT = "android.service.wallpaper.extra.LIVE_WALLPAPER_COMPONENT";
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
        public void setColorFilter(ColorFilter cf) {
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
        private static final int MSG_CLEAR_WALLPAPER = 1;
        private Bitmap mDefaultWallpaper;
        private IWallpaperManager mService;
        private Bitmap mWallpaper;

        Globals(Looper looper) {
            IBinder b = ServiceManager.getService(Context.WALLPAPER_SERVICE);
            this.mService = IWallpaperManager.Stub.asInterface(b);
        }

        @Override
        public void onWallpaperChanged() {
            synchronized (this) {
                this.mWallpaper = null;
                this.mDefaultWallpaper = null;
            }
        }

        public Bitmap peekWallpaperBitmap(Context context, boolean returnDefault) {
            Bitmap bitmap;
            synchronized (this) {
                if (this.mWallpaper != null) {
                    bitmap = this.mWallpaper;
                } else if (this.mDefaultWallpaper != null) {
                    bitmap = this.mDefaultWallpaper;
                } else {
                    this.mWallpaper = null;
                    try {
                        this.mWallpaper = getCurrentWallpaperLocked(context);
                    } catch (OutOfMemoryError e) {
                        Log.w(WallpaperManager.TAG, "No memory load current wallpaper", e);
                    }
                    if (returnDefault) {
                        if (this.mWallpaper == null) {
                            this.mDefaultWallpaper = getDefaultWallpaperLocked(context);
                            bitmap = this.mDefaultWallpaper;
                        } else {
                            this.mDefaultWallpaper = null;
                            bitmap = this.mWallpaper;
                        }
                    } else {
                        bitmap = this.mWallpaper;
                    }
                }
            }
            return bitmap;
        }

        public void forgetLoadedWallpaper() {
            synchronized (this) {
                this.mWallpaper = null;
                this.mDefaultWallpaper = null;
            }
        }

        private Bitmap getCurrentWallpaperLocked(Context context) {
            if (this.mService == null) {
                Log.w(WallpaperManager.TAG, "WallpaperService not running");
                return null;
            }
            try {
                Bundle params = new Bundle();
                ParcelFileDescriptor fd = this.mService.getWallpaper(this, params);
                if (fd == null) {
                    return null;
                }
                try {
                    try {
                        BitmapFactory.Options options = new BitmapFactory.Options();
                        return BitmapFactory.decodeFileDescriptor(fd.getFileDescriptor(), null, options);
                    } catch (OutOfMemoryError e) {
                        Log.w(WallpaperManager.TAG, "Can't decode file", e);
                        try {
                            fd.close();
                            return null;
                        } catch (IOException e2) {
                            return null;
                        }
                    }
                } finally {
                    try {
                        fd.close();
                    } catch (IOException e3) {
                    }
                }
            } catch (RemoteException e4) {
                return null;
            }
        }

        private Bitmap getDefaultWallpaperLocked(Context context) {
            Bitmap bitmapDecodeStream = null;
            InputStream is = WallpaperManager.openDefaultWallpaper(context);
            if (is != null) {
                try {
                    try {
                        BitmapFactory.Options options = new BitmapFactory.Options();
                        bitmapDecodeStream = BitmapFactory.decodeStream(is, null, options);
                        try {
                            is.close();
                        } catch (IOException e) {
                        }
                    } catch (OutOfMemoryError e2) {
                        Log.w(WallpaperManager.TAG, "Can't decode stream", e2);
                    }
                } finally {
                    try {
                        is.close();
                    } catch (IOException e3) {
                    }
                }
            }
            return bitmapDecodeStream;
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
        Bitmap bm = sGlobals.peekWallpaperBitmap(this.mContext, true);
        if (bm == null) {
            return null;
        }
        Drawable dr = new BitmapDrawable(this.mContext.getResources(), bm);
        dr.setDither(false);
        return dr;
    }

    public Drawable getBuiltInDrawable() {
        return getBuiltInDrawable(0, 0, false, 0.0f, 0.0f);
    }

    public Drawable getBuiltInDrawable(int outWidth, int outHeight, boolean scaleToFit, float horizontalAlignment, float verticalAlignment) {
        RectF cropRectF;
        if (sGlobals.mService == null) {
            Log.w(TAG, "WallpaperService not running");
            return null;
        }
        Resources resources = this.mContext.getResources();
        float horizontalAlignment2 = Math.max(0.0f, Math.min(1.0f, horizontalAlignment));
        float verticalAlignment2 = Math.max(0.0f, Math.min(1.0f, verticalAlignment));
        InputStream is = new BufferedInputStream(openDefaultWallpaper(this.mContext));
        if (is == null) {
            Log.e(TAG, "default wallpaper input stream is null");
            return null;
        }
        if (outWidth <= 0 || outHeight <= 0) {
            Bitmap fullSize = BitmapFactory.decodeStream(is, null, null);
            return new BitmapDrawable(resources, fullSize);
        }
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(is, null, options);
        if (options.outWidth != 0 && options.outHeight != 0) {
            int inWidth = options.outWidth;
            int inHeight = options.outHeight;
            InputStream is2 = new BufferedInputStream(openDefaultWallpaper(this.mContext));
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
                InputStream is3 = new BufferedInputStream(openDefaultWallpaper(this.mContext));
                Bitmap fullSize2 = null;
                if (is3 != null) {
                    BitmapFactory.Options options3 = new BitmapFactory.Options();
                    if (scaleDownSampleSize > 1) {
                        options3.inSampleSize = scaleDownSampleSize;
                    }
                    fullSize2 = BitmapFactory.decodeStream(is3, null, options3);
                }
                if (fullSize2 != null) {
                    crop = Bitmap.createBitmap(fullSize2, roundedTrueCrop.left, roundedTrueCrop.top, roundedTrueCrop.width(), roundedTrueCrop.height());
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
        Bitmap bm = sGlobals.peekWallpaperBitmap(this.mContext, false);
        if (bm == null) {
            return null;
        }
        Drawable dr = new BitmapDrawable(this.mContext.getResources(), bm);
        dr.setDither(false);
        return dr;
    }

    public Drawable getFastDrawable() {
        Bitmap bm = sGlobals.peekWallpaperBitmap(this.mContext, true);
        if (bm != null) {
            return new FastBitmapDrawable(bm);
        }
        return null;
    }

    public Drawable peekFastDrawable() {
        Bitmap bm = sGlobals.peekWallpaperBitmap(this.mContext, false);
        if (bm != null) {
            return new FastBitmapDrawable(bm);
        }
        return null;
    }

    public Bitmap getBitmap() {
        return sGlobals.peekWallpaperBitmap(this.mContext, true);
    }

    public void forgetLoadedWallpaper() {
        sGlobals.forgetLoadedWallpaper();
    }

    public WallpaperInfo getWallpaperInfo() {
        WallpaperInfo wallpaperInfo = null;
        try {
            if (sGlobals.mService != null) {
                wallpaperInfo = sGlobals.mService.getWallpaperInfo();
            } else {
                Log.w(TAG, "WallpaperService not running");
            }
        } catch (RemoteException e) {
        }
        return wallpaperInfo;
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
            if (cropAppList.size() <= 0) {
                cropAndSetWallpaperIntent.setPackage("com.android.wallpapercropper");
                List<ResolveInfo> cropAppList2 = packageManager.queryIntentActivities(cropAndSetWallpaperIntent, 0);
                if (cropAppList2.size() <= 0) {
                    throw new IllegalArgumentException("Cannot use passed URI to set wallpaper; check that the type returned by ContentProvider matches image/*");
                }
            }
        }
        return cropAndSetWallpaperIntent;
    }

    public void setResource(int resid) throws Throwable {
        if (sGlobals.mService == null) {
            Log.w(TAG, "WallpaperService not running");
            return;
        }
        try {
            Resources resources = this.mContext.getResources();
            ParcelFileDescriptor fd = sGlobals.mService.setWallpaper("res:" + resources.getResourceName(resid));
            if (fd != null) {
                FileOutputStream fos = null;
                try {
                    FileOutputStream fos2 = new ParcelFileDescriptor.AutoCloseOutputStream(fd);
                    try {
                        setWallpaper(resources.openRawResource(resid), fos2);
                        if (fos2 != null) {
                            fos2.close();
                        }
                    } catch (Throwable th) {
                        th = th;
                        fos = fos2;
                        if (fos != null) {
                            fos.close();
                        }
                        throw th;
                    }
                } catch (Throwable th2) {
                    th = th2;
                }
            }
        } catch (RemoteException e) {
        }
    }

    public void setBitmap(Bitmap bitmap) throws Throwable {
        FileOutputStream fos;
        if (sGlobals.mService != null) {
            try {
                ParcelFileDescriptor fd = sGlobals.mService.setWallpaper(null);
                if (fd != null) {
                    FileOutputStream fos2 = null;
                    try {
                        fos = new ParcelFileDescriptor.AutoCloseOutputStream(fd);
                    } catch (Throwable th) {
                        th = th;
                    }
                    try {
                        bitmap.compress(Bitmap.CompressFormat.PNG, 90, fos);
                        if (fos != null) {
                            fos.close();
                            return;
                        }
                        return;
                    } catch (Throwable th2) {
                        th = th2;
                        fos2 = fos;
                        if (fos2 != null) {
                            fos2.close();
                        }
                        throw th;
                    }
                }
                return;
            } catch (RemoteException e) {
                return;
            }
        }
        Log.w(TAG, "WallpaperService not running");
    }

    public void setStream(InputStream data) throws Throwable {
        if (sGlobals.mService != null) {
            try {
                ParcelFileDescriptor fd = sGlobals.mService.setWallpaper(null);
                if (fd != null) {
                    FileOutputStream fos = null;
                    try {
                        FileOutputStream fos2 = new ParcelFileDescriptor.AutoCloseOutputStream(fd);
                        try {
                            setWallpaper(data, fos2);
                            if (fos2 != null) {
                                fos2.close();
                            }
                        } catch (Throwable th) {
                            th = th;
                            fos = fos2;
                            if (fos != null) {
                                fos.close();
                            }
                            throw th;
                        }
                    } catch (Throwable th2) {
                        th = th2;
                    }
                }
            } catch (RemoteException e) {
            }
        } else {
            Log.w(TAG, "WallpaperService not running");
        }
    }

    private void setWallpaper(InputStream data, FileOutputStream fos) throws IOException {
        byte[] buffer = new byte[32768];
        while (true) {
            int amt = data.read(buffer);
            if (amt > 0) {
                fos.write(buffer, 0, amt);
            } else {
                return;
            }
        }
    }

    public boolean hasResourceWallpaper(int resid) {
        if (sGlobals.mService == null) {
            Log.w(TAG, "WallpaperService not running");
            return false;
        }
        try {
            Resources resources = this.mContext.getResources();
            String name = "res:" + resources.getResourceName(resid);
            return sGlobals.mService.hasNamedWallpaper(name);
        } catch (RemoteException e) {
            return false;
        }
    }

    public int getDesiredMinimumWidth() {
        if (sGlobals.mService != null) {
            try {
                return sGlobals.mService.getWidthHint();
            } catch (RemoteException e) {
                return 0;
            }
        }
        Log.w(TAG, "WallpaperService not running");
        return 0;
    }

    public int getDesiredMinimumHeight() {
        if (sGlobals.mService != null) {
            try {
                return sGlobals.mService.getHeightHint();
            } catch (RemoteException e) {
                return 0;
            }
        }
        Log.w(TAG, "WallpaperService not running");
        return 0;
    }

    public void suggestDesiredDimensions(int minimumWidth, int minimumHeight) {
        int maximumTextureSize;
        try {
            try {
                maximumTextureSize = SystemProperties.getInt("sys.max_texture_size", 0);
            } catch (RemoteException e) {
                return;
            }
        } catch (Exception e2) {
            maximumTextureSize = 0;
        }
        if (maximumTextureSize > 0 && (minimumWidth > maximumTextureSize || minimumHeight > maximumTextureSize)) {
            float aspect = minimumHeight / minimumWidth;
            if (minimumWidth > minimumHeight) {
                minimumWidth = maximumTextureSize;
                minimumHeight = (int) (((double) (minimumWidth * aspect)) + 0.5d);
            } else {
                minimumHeight = maximumTextureSize;
                minimumWidth = (int) (((double) (minimumHeight / aspect)) + 0.5d);
            }
        }
        if (sGlobals.mService != null) {
            sGlobals.mService.setDimensionHints(minimumWidth, minimumHeight);
        } else {
            Log.w(TAG, "WallpaperService not running");
        }
    }

    public void setDisplayPadding(Rect padding) {
        try {
            if (sGlobals.mService != null) {
                sGlobals.mService.setDisplayPadding(padding);
            } else {
                Log.w(TAG, "WallpaperService not running");
            }
        } catch (RemoteException e) {
        }
    }

    public void setDisplayOffset(IBinder windowToken, int x, int y) {
        try {
            WindowManagerGlobal.getWindowSession().setWallpaperDisplayOffset(windowToken, x, y);
        } catch (RemoteException e) {
        }
    }

    public void setWallpaperOffsets(IBinder windowToken, float xOffset, float yOffset) {
        try {
            WindowManagerGlobal.getWindowSession().setWallpaperPosition(windowToken, xOffset, yOffset, this.mWallpaperXStep, this.mWallpaperYStep);
        } catch (RemoteException e) {
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
        }
    }

    public void clearWallpaperOffsets(IBinder windowToken) {
        try {
            WindowManagerGlobal.getWindowSession().setWallpaperPosition(windowToken, -1.0f, -1.0f, -1.0f, -1.0f);
        } catch (RemoteException e) {
        }
    }

    public void clear() throws Throwable {
        setStream(openDefaultWallpaper(this.mContext));
    }

    public static InputStream openDefaultWallpaper(Context context) {
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
        return context.getResources().openRawResource(R.drawable.default_wallpaper);
    }

    public static ComponentName getDefaultWallpaperComponent(Context context) {
        ComponentName cn;
        ComponentName cn2;
        String flat = SystemProperties.get(PROP_WALLPAPER_COMPONENT);
        if (TextUtils.isEmpty(flat) || (cn2 = ComponentName.unflattenFromString(flat)) == null) {
            String flat2 = context.getString(R.string.default_wallpaper_component);
            if (TextUtils.isEmpty(flat2) || (cn = ComponentName.unflattenFromString(flat2)) == null) {
                return null;
            }
            return cn;
        }
        return cn2;
    }
}
