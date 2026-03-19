package com.android.wallpapercropper;

import android.app.ActionBar;
import android.app.Activity;
import android.app.WallpaperManager;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;
import com.android.gallery3d.common.Utils;
import com.android.photos.BitmapRegionTileSource;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class WallpaperCropActivity extends Activity {
    protected static Point sDefaultWallpaperSize;
    protected CropView mCropView;
    private View mSetWallpaperButton;

    public interface OnBitmapCroppedHandler {
        void onBitmapCropped(byte[] bArr);
    }

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        init();
        if (!enableRotation()) {
            setRequestedOrientation(1);
        }
    }

    protected void init() {
        setContentView(R.layout.wallpaper_cropper);
        this.mCropView = (CropView) findViewById(R.id.cropView);
        final Uri data = getIntent().getData();
        if (data == null) {
            Log.e("Launcher3.CropActivity", "No URI passed in intent, exiting WallpaperCropActivity");
            finish();
            return;
        }
        ActionBar actionBar = getActionBar();
        actionBar.setCustomView(R.layout.actionbar_set_wallpaper);
        actionBar.getCustomView().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                WallpaperCropActivity.this.cropImageAndSetWallpaper(data, null, true);
            }
        });
        this.mSetWallpaperButton = findViewById(R.id.set_wallpaper_button);
        final BitmapRegionTileSource.UriBitmapSource uriBitmapSource = new BitmapRegionTileSource.UriBitmapSource(this, data, 1024);
        this.mSetWallpaperButton.setVisibility(4);
        setCropViewTileSource(uriBitmapSource, true, false, new Runnable() {
            @Override
            public void run() {
                if (uriBitmapSource.getLoadingState() == BitmapRegionTileSource.BitmapSource.State.LOADED) {
                    WallpaperCropActivity.this.mSetWallpaperButton.setVisibility(0);
                } else {
                    Toast.makeText(WallpaperCropActivity.this, WallpaperCropActivity.this.getString(R.string.wallpaper_load_fail), 1).show();
                    WallpaperCropActivity.this.finish();
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        if (this.mCropView != null) {
            this.mCropView.destroy();
        }
        super.onDestroy();
    }

    public void setCropViewTileSource(final BitmapRegionTileSource.BitmapSource bitmapSource, final boolean z, final boolean z2, final Runnable runnable) {
        final View viewFindViewById = findViewById(R.id.loading);
        final AsyncTask<Void, Void, Void> asyncTask = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voidArr) {
                if (!isCancelled()) {
                    try {
                        bitmapSource.loadInBackground();
                        return null;
                    } catch (SecurityException e) {
                        if (WallpaperCropActivity.this.isDestroyed()) {
                            cancel(false);
                            return null;
                        }
                        throw e;
                    }
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void r4) {
                if (!isCancelled()) {
                    viewFindViewById.setVisibility(4);
                    if (bitmapSource.getLoadingState() == BitmapRegionTileSource.BitmapSource.State.LOADED) {
                        WallpaperCropActivity.this.mCropView.setTileSource(new BitmapRegionTileSource(this, bitmapSource), null);
                        WallpaperCropActivity.this.mCropView.setTouchEnabled(z);
                        if (z2) {
                            WallpaperCropActivity.this.mCropView.moveToLeft();
                        }
                    }
                }
                if (runnable != null) {
                    runnable.run();
                }
            }
        };
        viewFindViewById.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (asyncTask.getStatus() != AsyncTask.Status.FINISHED) {
                    viewFindViewById.setVisibility(0);
                }
            }
        }, 1000L);
        asyncTask.execute(new Void[0]);
    }

    public boolean enableRotation() {
        return getResources().getBoolean(R.bool.allow_rotation);
    }

    private static float wallpaperTravelToScreenWidthRatio(int i, int i2) {
        return (0.30769226f * (i / i2)) + 1.0076923f;
    }

    protected static Point getDefaultWallpaperSize(Resources resources, WindowManager windowManager) {
        int iMax;
        if (sDefaultWallpaperSize == null) {
            Point point = new Point();
            Point point2 = new Point();
            windowManager.getDefaultDisplay().getCurrentSizeRange(point, point2);
            int iMax2 = Math.max(point2.x, point2.y);
            int iMax3 = Math.max(point.x, point.y);
            if (Build.VERSION.SDK_INT >= 17) {
                Point point3 = new Point();
                windowManager.getDefaultDisplay().getRealSize(point3);
                iMax2 = Math.max(point3.x, point3.y);
                iMax3 = Math.min(point3.x, point3.y);
            }
            if (isScreenLarge(resources)) {
                iMax = (int) (iMax2 * wallpaperTravelToScreenWidthRatio(iMax2, iMax3));
            } else {
                iMax = Math.max((int) (iMax3 * 2.0f), iMax2);
            }
            sDefaultWallpaperSize = new Point(iMax, iMax2);
        }
        return sDefaultWallpaperSize;
    }

    private static boolean isScreenLarge(Resources resources) {
        return resources.getConfiguration().smallestScreenWidthDp >= 720;
    }

    protected void cropImageAndSetWallpaper(Uri uri, OnBitmapCroppedHandler onBitmapCroppedHandler, final boolean z) {
        float fMin;
        boolean z2 = getResources().getBoolean(R.bool.center_crop);
        boolean z3 = this.mCropView.getLayoutDirection() == 0;
        Display defaultDisplay = getWindowManager().getDefaultDisplay();
        Point point = new Point();
        defaultDisplay.getSize(point);
        boolean z4 = point.x < point.y;
        Point defaultWallpaperSize = getDefaultWallpaperSize(getResources(), getWindowManager());
        RectF crop = this.mCropView.getCrop();
        Point sourceDimensions = this.mCropView.getSourceDimensions();
        int imageRotation = this.mCropView.getImageRotation();
        float width = this.mCropView.getWidth() / crop.width();
        Matrix matrix = new Matrix();
        matrix.setRotate(imageRotation);
        float[] fArr = {sourceDimensions.x, sourceDimensions.y};
        matrix.mapPoints(fArr);
        fArr[0] = Math.abs(fArr[0]);
        fArr[1] = Math.abs(fArr[1]);
        crop.left = Math.max(0.0f, crop.left);
        crop.right = Math.min(fArr[0], crop.right);
        crop.top = Math.max(0.0f, crop.top);
        crop.bottom = Math.min(fArr[1], crop.bottom);
        if (z2) {
            fMin = Math.min(fArr[0] - crop.right, crop.left) * 2.0f;
        } else {
            fMin = z3 ? fArr[0] - crop.right : crop.left;
        }
        float fMin2 = Math.min(fMin, (defaultWallpaperSize.x / width) - crop.width());
        if (z2) {
            float f = fMin2 / 2.0f;
            crop.left -= f;
            crop.right += f;
        } else if (z3) {
            crop.right += fMin2;
        } else {
            crop.left -= fMin2;
        }
        if (!z4) {
            float fMin3 = Math.min(Math.min(fArr[1] - crop.bottom, crop.top), ((defaultWallpaperSize.y / width) - crop.height()) / 2.0f);
            crop.top -= fMin3;
            crop.bottom += fMin3;
        } else {
            crop.bottom = crop.top + (defaultWallpaperSize.y / width);
        }
        BitmapCropTask bitmapCropTask = new BitmapCropTask(this, uri, crop, imageRotation, Math.round(crop.width() * width), Math.round(crop.height() * width), true, false, new Runnable() {
            @Override
            public void run() {
                if (z) {
                    WallpaperCropActivity.this.setResult(-1);
                    WallpaperCropActivity.this.finish();
                }
            }
        });
        if (onBitmapCroppedHandler != null) {
            bitmapCropTask.setOnBitmapCropped(onBitmapCroppedHandler);
        }
        bitmapCropTask.execute(new Void[0]);
    }

    protected static class BitmapCropTask extends AsyncTask<Void, Void, Boolean> {
        Context mContext;
        Bitmap mCroppedBitmap;
        String mInFilePath;
        byte[] mInImageBytes;
        Uri mInUri;
        boolean mNoCrop;
        OnBitmapCroppedHandler mOnBitmapCroppedHandler;
        Runnable mOnEndRunnable;
        int mOutHeight;
        int mOutWidth;
        Resources mResources;
        int mRotation;
        boolean mSaveCroppedBitmap;
        boolean mSetWallpaper;
        int mInResId = 0;
        RectF mCropBounds = null;
        String mOutputFormat = "jpg";

        public BitmapCropTask(Context context, Uri uri, RectF rectF, int i, int i2, int i3, boolean z, boolean z2, Runnable runnable) {
            this.mInUri = null;
            this.mContext = context;
            this.mInUri = uri;
            init(rectF, i, i2, i3, z, z2, runnable);
        }

        private void init(RectF rectF, int i, int i2, int i3, boolean z, boolean z2, Runnable runnable) {
            this.mCropBounds = rectF;
            this.mRotation = i;
            this.mOutWidth = i2;
            this.mOutHeight = i3;
            this.mSetWallpaper = z;
            this.mSaveCroppedBitmap = z2;
            this.mOnEndRunnable = runnable;
        }

        public void setOnBitmapCropped(OnBitmapCroppedHandler onBitmapCroppedHandler) {
            this.mOnBitmapCroppedHandler = onBitmapCroppedHandler;
        }

        private InputStream regenerateInputStream() {
            if (this.mInUri == null && this.mInResId == 0 && this.mInFilePath == null && this.mInImageBytes == null) {
                Log.w("Launcher3.CropActivity", "cannot read original file, no input URI, resource ID, or image byte array given");
                return null;
            }
            try {
                if (this.mInUri != null) {
                    return new BufferedInputStream(this.mContext.getContentResolver().openInputStream(this.mInUri));
                }
                if (this.mInFilePath != null) {
                    return this.mContext.openFileInput(this.mInFilePath);
                }
                if (this.mInImageBytes != null) {
                    return new BufferedInputStream(new ByteArrayInputStream(this.mInImageBytes));
                }
                return new BufferedInputStream(this.mResources.openRawResource(this.mInResId));
            } catch (FileNotFoundException e) {
                Log.w("Launcher3.CropActivity", "cannot read file: " + this.mInUri.toString(), e);
                return null;
            }
        }

        public Point getImageBounds() {
            InputStream inputStreamRegenerateInputStream = regenerateInputStream();
            if (inputStreamRegenerateInputStream != null) {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeStream(inputStreamRegenerateInputStream, null, options);
                Utils.closeSilently(inputStreamRegenerateInputStream);
                if (options.outWidth != 0 && options.outHeight != 0) {
                    return new Point(options.outWidth, options.outHeight);
                }
            }
            return null;
        }

        public boolean cropBitmap() throws Throwable {
            InputStream inputStreamRegenerateInputStream;
            BitmapRegionDecoder bitmapRegionDecoderNewInstance;
            Bitmap bitmapCreateBitmap;
            Bitmap bitmapDecodeStream = null;
            WallpaperManager wallpaperManager = this.mSetWallpaper ? WallpaperManager.getInstance(this.mContext.getApplicationContext()) : null;
            boolean z = false;
            if (this.mSetWallpaper && this.mNoCrop) {
                try {
                    InputStream inputStreamRegenerateInputStream2 = regenerateInputStream();
                    if (inputStreamRegenerateInputStream2 != null) {
                        wallpaperManager.setStream(inputStreamRegenerateInputStream2);
                        Utils.closeSilently(inputStreamRegenerateInputStream2);
                    }
                } catch (IOException e) {
                    Log.w("Launcher3.CropActivity", "cannot write stream to wallpaper", e);
                    z = true;
                }
                return !z;
            }
            Rect rect = new Rect();
            Matrix matrix = new Matrix();
            Matrix matrix2 = new Matrix();
            Point imageBounds = getImageBounds();
            if (this.mRotation > 0) {
                matrix.setRotate(this.mRotation);
                matrix2.setRotate(-this.mRotation);
                this.mCropBounds.roundOut(rect);
                this.mCropBounds = new RectF(rect);
                if (imageBounds == null) {
                    Log.w("Launcher3.CropActivity", "cannot get bounds for image");
                    return false;
                }
                float[] fArr = {imageBounds.x, imageBounds.y};
                matrix.mapPoints(fArr);
                fArr[0] = Math.abs(fArr[0]);
                fArr[1] = Math.abs(fArr[1]);
                this.mCropBounds.offset((-fArr[0]) / 2.0f, (-fArr[1]) / 2.0f);
                matrix2.mapRect(this.mCropBounds);
                this.mCropBounds.offset(imageBounds.x / 2, imageBounds.y / 2);
            }
            this.mCropBounds.roundOut(rect);
            if (rect.width() <= 0 || rect.height() <= 0) {
                Log.w("Launcher3.CropActivity", "crop has bad values for full size image");
                return false;
            }
            int iWidth = rect.width() / this.mOutWidth;
            int iHeight = rect.height();
            ?? r12 = this.mOutHeight;
            int iMax = Math.max(1, Math.min(iWidth, iHeight / r12));
            try {
                try {
                    inputStreamRegenerateInputStream = regenerateInputStream();
                    try {
                    } catch (IOException e2) {
                        e = e2;
                        bitmapRegionDecoderNewInstance = null;
                    }
                } catch (Throwable th) {
                    th = th;
                    Utils.closeSilently(r12);
                    throw th;
                }
            } catch (IOException e3) {
                e = e3;
                inputStreamRegenerateInputStream = null;
                bitmapRegionDecoderNewInstance = null;
            } catch (Throwable th2) {
                th = th2;
                r12 = 0;
                Utils.closeSilently(r12);
                throw th;
            }
            if (inputStreamRegenerateInputStream == null) {
                Log.w("Launcher3.CropActivity", "cannot get input stream for uri=" + this.mInUri.toString());
                Utils.closeSilently(inputStreamRegenerateInputStream);
                return false;
            }
            bitmapRegionDecoderNewInstance = BitmapRegionDecoder.newInstance(inputStreamRegenerateInputStream, false);
            try {
                Utils.closeSilently(inputStreamRegenerateInputStream);
            } catch (IOException e4) {
                e = e4;
                Log.w("Launcher3.CropActivity", "cannot open region decoder for file: " + this.mInUri.toString(), e);
            }
            Utils.closeSilently(inputStreamRegenerateInputStream);
            if (bitmapRegionDecoderNewInstance != null) {
                BitmapFactory.Options options = new BitmapFactory.Options();
                if (iMax > 1) {
                    options.inSampleSize = iMax;
                }
                bitmapCreateBitmap = bitmapRegionDecoderNewInstance.decodeRegion(rect, options);
                bitmapRegionDecoderNewInstance.recycle();
            } else {
                bitmapCreateBitmap = null;
            }
            if (bitmapCreateBitmap == null) {
                InputStream inputStreamRegenerateInputStream3 = regenerateInputStream();
                if (inputStreamRegenerateInputStream3 != null) {
                    BitmapFactory.Options options2 = new BitmapFactory.Options();
                    if (iMax > 1) {
                        options2.inSampleSize = iMax;
                    }
                    bitmapDecodeStream = BitmapFactory.decodeStream(inputStreamRegenerateInputStream3, null, options2);
                    Utils.closeSilently(inputStreamRegenerateInputStream3);
                }
                if (bitmapDecodeStream != null) {
                    int width = imageBounds.x / bitmapDecodeStream.getWidth();
                    float f = width;
                    this.mCropBounds.left /= f;
                    this.mCropBounds.top /= f;
                    this.mCropBounds.bottom /= f;
                    this.mCropBounds.right /= f;
                    this.mCropBounds.roundOut(rect);
                    if (rect.width() > bitmapDecodeStream.getWidth()) {
                        rect.right = rect.left + bitmapDecodeStream.getWidth();
                    }
                    if (rect.right > bitmapDecodeStream.getWidth()) {
                        int iMax2 = rect.left - Math.max(0, rect.right - rect.width());
                        rect.left -= iMax2;
                        rect.right -= iMax2;
                    }
                    if (rect.height() > bitmapDecodeStream.getHeight()) {
                        rect.bottom = rect.top + bitmapDecodeStream.getHeight();
                    }
                    if (rect.bottom > bitmapDecodeStream.getHeight()) {
                        int iMax3 = rect.top - Math.max(0, rect.bottom - rect.height());
                        rect.top -= iMax3;
                        rect.bottom -= iMax3;
                    }
                    bitmapCreateBitmap = Bitmap.createBitmap(bitmapDecodeStream, rect.left, rect.top, rect.width(), rect.height());
                }
            }
            if (bitmapCreateBitmap == null) {
                Log.w("Launcher3.CropActivity", "cannot decode file: " + this.mInUri.toString());
                return false;
            }
            if ((this.mOutWidth > 0 && this.mOutHeight > 0) || this.mRotation > 0) {
                float[] fArr2 = {bitmapCreateBitmap.getWidth(), bitmapCreateBitmap.getHeight()};
                matrix.mapPoints(fArr2);
                fArr2[0] = Math.abs(fArr2[0]);
                fArr2[1] = Math.abs(fArr2[1]);
                if (this.mOutWidth <= 0 || this.mOutHeight <= 0) {
                    this.mOutWidth = Math.round(fArr2[0]);
                    this.mOutHeight = Math.round(fArr2[1]);
                }
                RectF rectF = new RectF(0.0f, 0.0f, fArr2[0], fArr2[1]);
                RectF rectF2 = new RectF(0.0f, 0.0f, this.mOutWidth, this.mOutHeight);
                Matrix matrix3 = new Matrix();
                if (this.mRotation == 0) {
                    matrix3.setRectToRect(rectF, rectF2, Matrix.ScaleToFit.FILL);
                } else {
                    Matrix matrix4 = new Matrix();
                    matrix4.setTranslate((-bitmapCreateBitmap.getWidth()) / 2.0f, (-bitmapCreateBitmap.getHeight()) / 2.0f);
                    Matrix matrix5 = new Matrix();
                    matrix5.setRotate(this.mRotation);
                    Matrix matrix6 = new Matrix();
                    matrix6.setTranslate(fArr2[0] / 2.0f, fArr2[1] / 2.0f);
                    Matrix matrix7 = new Matrix();
                    matrix7.setRectToRect(rectF, rectF2, Matrix.ScaleToFit.FILL);
                    Matrix matrix8 = new Matrix();
                    matrix8.setConcat(matrix5, matrix4);
                    Matrix matrix9 = new Matrix();
                    matrix9.setConcat(matrix7, matrix6);
                    matrix3.setConcat(matrix9, matrix8);
                }
                Bitmap bitmapCreateBitmap2 = Bitmap.createBitmap((int) rectF2.width(), (int) rectF2.height(), Bitmap.Config.ARGB_8888);
                if (bitmapCreateBitmap2 != null) {
                    Canvas canvas = new Canvas(bitmapCreateBitmap2);
                    Paint paint = new Paint();
                    paint.setFilterBitmap(true);
                    canvas.drawBitmap(bitmapCreateBitmap, matrix3, paint);
                    bitmapCreateBitmap = bitmapCreateBitmap2;
                }
            }
            if (this.mSaveCroppedBitmap) {
                this.mCroppedBitmap = bitmapCreateBitmap;
            }
            Bitmap.CompressFormat compressFormatConvertExtensionToCompressFormat = WallpaperCropActivity.convertExtensionToCompressFormat(WallpaperCropActivity.getFileExtension(this.mOutputFormat));
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(2048);
            if (bitmapCreateBitmap.compress(compressFormatConvertExtensionToCompressFormat, 90, byteArrayOutputStream)) {
                if (this.mSetWallpaper && wallpaperManager != null) {
                    try {
                        byte[] byteArray = byteArrayOutputStream.toByteArray();
                        wallpaperManager.setStream(new ByteArrayInputStream(byteArray));
                        if (this.mOnBitmapCroppedHandler != null) {
                            this.mOnBitmapCroppedHandler.onBitmapCropped(byteArray);
                        }
                    } catch (IOException e5) {
                        Log.w("Launcher3.CropActivity", "cannot write stream to wallpaper", e5);
                        z = true;
                    }
                }
                return !z;
            }
            Log.w("Launcher3.CropActivity", "cannot compress bitmap");
            z = true;
            return !z;
        }

        @Override
        protected Boolean doInBackground(Void... voidArr) {
            return Boolean.valueOf(cropBitmap());
        }

        @Override
        protected void onPostExecute(Boolean bool) {
            if (this.mOnEndRunnable != null) {
                this.mOnEndRunnable.run();
            }
        }
    }

    protected static Bitmap.CompressFormat convertExtensionToCompressFormat(String str) {
        return str.equals("png") ? Bitmap.CompressFormat.PNG : Bitmap.CompressFormat.JPEG;
    }

    protected static String getFileExtension(String str) {
        if (str == null) {
            str = "jpg";
        }
        String lowerCase = str.toLowerCase();
        if (lowerCase.equals("png") || lowerCase.equals("gif")) {
            return "png";
        }
        return "jpg";
    }
}
