package com.android.wallpapercropper;

import android.app.ActionBar;
import android.app.Activity;
import android.app.WallpaperManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
        if (!enableRotation()) {
            setRequestedOrientation(1);
        }
    }

    protected void init() {
        setContentView(R.layout.wallpaper_cropper);
        this.mCropView = (CropView) findViewById(R.id.cropView);
        Intent cropIntent = getIntent();
        final Uri imageUri = cropIntent.getData();
        if (imageUri == null) {
            Log.e("Launcher3.CropActivity", "No URI passed in intent, exiting WallpaperCropActivity");
            finish();
            return;
        }
        ActionBar actionBar = getActionBar();
        actionBar.setCustomView(R.layout.actionbar_set_wallpaper);
        actionBar.getCustomView().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                WallpaperCropActivity.this.cropImageAndSetWallpaper(imageUri, null, true);
            }
        });
        this.mSetWallpaperButton = findViewById(R.id.set_wallpaper_button);
        final BitmapRegionTileSource.UriBitmapSource bitmapSource = new BitmapRegionTileSource.UriBitmapSource(this, imageUri, 1024);
        this.mSetWallpaperButton.setVisibility(4);
        Runnable onLoad = new Runnable() {
            @Override
            public void run() {
                if (bitmapSource.getLoadingState() == BitmapRegionTileSource.BitmapSource.State.LOADED) {
                    WallpaperCropActivity.this.mSetWallpaperButton.setVisibility(0);
                } else {
                    Toast.makeText(WallpaperCropActivity.this, WallpaperCropActivity.this.getString(R.string.wallpaper_load_fail), 1).show();
                    WallpaperCropActivity.this.finish();
                }
            }
        };
        setCropViewTileSource(bitmapSource, true, false, onLoad);
    }

    @Override
    protected void onDestroy() {
        if (this.mCropView != null) {
            this.mCropView.destroy();
        }
        super.onDestroy();
    }

    public void setCropViewTileSource(final BitmapRegionTileSource.BitmapSource bitmapSource, final boolean touchEnabled, final boolean moveToLeft, final Runnable postExecute) {
        final View progressView = findViewById(R.id.loading);
        final AsyncTask<Void, Void, Void> loadBitmapTask = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... args) {
                if (!isCancelled()) {
                    try {
                        bitmapSource.loadInBackground();
                        return null;
                    } catch (SecurityException securityException) {
                        if (WallpaperCropActivity.this.isDestroyed()) {
                            cancel(false);
                            return null;
                        }
                        throw securityException;
                    }
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void arg) {
                if (!isCancelled()) {
                    progressView.setVisibility(4);
                    if (bitmapSource.getLoadingState() == BitmapRegionTileSource.BitmapSource.State.LOADED) {
                        WallpaperCropActivity.this.mCropView.setTileSource(new BitmapRegionTileSource(this, bitmapSource), null);
                        WallpaperCropActivity.this.mCropView.setTouchEnabled(touchEnabled);
                        if (moveToLeft) {
                            WallpaperCropActivity.this.mCropView.moveToLeft();
                        }
                    }
                }
                if (postExecute != null) {
                    postExecute.run();
                }
            }
        };
        progressView.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (loadBitmapTask.getStatus() != AsyncTask.Status.FINISHED) {
                    progressView.setVisibility(0);
                }
            }
        }, 1000L);
        loadBitmapTask.execute(new Void[0]);
    }

    public boolean enableRotation() {
        return getResources().getBoolean(R.bool.allow_rotation);
    }

    private static float wallpaperTravelToScreenWidthRatio(int width, int height) {
        float aspectRatio = width / height;
        return (0.30769226f * aspectRatio) + 1.0076923f;
    }

    protected static Point getDefaultWallpaperSize(Resources res, WindowManager windowManager) {
        int defaultWidth;
        int defaultHeight;
        if (sDefaultWallpaperSize == null) {
            Point minDims = new Point();
            Point maxDims = new Point();
            windowManager.getDefaultDisplay().getCurrentSizeRange(minDims, maxDims);
            int maxDim = Math.max(maxDims.x, maxDims.y);
            int minDim = Math.max(minDims.x, minDims.y);
            if (Build.VERSION.SDK_INT >= 17) {
                Point realSize = new Point();
                windowManager.getDefaultDisplay().getRealSize(realSize);
                maxDim = Math.max(realSize.x, realSize.y);
                minDim = Math.min(realSize.x, realSize.y);
            }
            if (isScreenLarge(res)) {
                defaultWidth = (int) (maxDim * wallpaperTravelToScreenWidthRatio(maxDim, minDim));
                defaultHeight = maxDim;
            } else {
                defaultWidth = Math.max((int) (minDim * 2.0f), maxDim);
                defaultHeight = maxDim;
            }
            sDefaultWallpaperSize = new Point(defaultWidth, defaultHeight);
        }
        return sDefaultWallpaperSize;
    }

    private static boolean isScreenLarge(Resources res) {
        Configuration config = res.getConfiguration();
        return config.smallestScreenWidthDp >= 720;
    }

    protected void cropImageAndSetWallpaper(Uri uri, OnBitmapCroppedHandler onBitmapCroppedHandler, final boolean finishActivityWhenDone) {
        float extraSpace;
        boolean centerCrop = getResources().getBoolean(R.bool.center_crop);
        boolean ltr = this.mCropView.getLayoutDirection() == 0;
        Display d = getWindowManager().getDefaultDisplay();
        Point displaySize = new Point();
        d.getSize(displaySize);
        boolean isPortrait = displaySize.x < displaySize.y;
        Point defaultWallpaperSize = getDefaultWallpaperSize(getResources(), getWindowManager());
        RectF cropRect = this.mCropView.getCrop();
        Point inSize = this.mCropView.getSourceDimensions();
        int cropRotation = this.mCropView.getImageRotation();
        float cropScale = this.mCropView.getWidth() / cropRect.width();
        Matrix rotateMatrix = new Matrix();
        rotateMatrix.setRotate(cropRotation);
        float[] rotatedInSize = {inSize.x, inSize.y};
        rotateMatrix.mapPoints(rotatedInSize);
        rotatedInSize[0] = Math.abs(rotatedInSize[0]);
        rotatedInSize[1] = Math.abs(rotatedInSize[1]);
        cropRect.left = Math.max(0.0f, cropRect.left);
        cropRect.right = Math.min(rotatedInSize[0], cropRect.right);
        cropRect.top = Math.max(0.0f, cropRect.top);
        cropRect.bottom = Math.min(rotatedInSize[1], cropRect.bottom);
        if (centerCrop) {
            extraSpace = 2.0f * Math.min(rotatedInSize[0] - cropRect.right, cropRect.left);
        } else {
            extraSpace = ltr ? rotatedInSize[0] - cropRect.right : cropRect.left;
        }
        float maxExtraSpace = (defaultWallpaperSize.x / cropScale) - cropRect.width();
        float extraSpace2 = Math.min(extraSpace, maxExtraSpace);
        if (centerCrop) {
            cropRect.left -= extraSpace2 / 2.0f;
            cropRect.right += extraSpace2 / 2.0f;
        } else if (ltr) {
            cropRect.right += extraSpace2;
        } else {
            cropRect.left -= extraSpace2;
        }
        if (isPortrait) {
            cropRect.bottom = cropRect.top + (defaultWallpaperSize.y / cropScale);
        } else {
            float extraPortraitHeight = (defaultWallpaperSize.y / cropScale) - cropRect.height();
            float expandHeight = Math.min(Math.min(rotatedInSize[1] - cropRect.bottom, cropRect.top), extraPortraitHeight / 2.0f);
            cropRect.top -= expandHeight;
            cropRect.bottom += expandHeight;
        }
        int outWidth = Math.round(cropRect.width() * cropScale);
        int outHeight = Math.round(cropRect.height() * cropScale);
        Runnable onEndCrop = new Runnable() {
            @Override
            public void run() {
                if (finishActivityWhenDone) {
                    WallpaperCropActivity.this.setResult(-1);
                    WallpaperCropActivity.this.finish();
                }
            }
        };
        BitmapCropTask cropTask = new BitmapCropTask(this, uri, cropRect, cropRotation, outWidth, outHeight, true, false, onEndCrop);
        if (onBitmapCroppedHandler != null) {
            cropTask.setOnBitmapCropped(onBitmapCroppedHandler);
        }
        cropTask.execute(new Void[0]);
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

        public BitmapCropTask(Context c, Uri inUri, RectF cropBounds, int rotation, int outWidth, int outHeight, boolean setWallpaper, boolean saveCroppedBitmap, Runnable onEndRunnable) {
            this.mInUri = null;
            this.mContext = c;
            this.mInUri = inUri;
            init(cropBounds, rotation, outWidth, outHeight, setWallpaper, saveCroppedBitmap, onEndRunnable);
        }

        private void init(RectF cropBounds, int rotation, int outWidth, int outHeight, boolean setWallpaper, boolean saveCroppedBitmap, Runnable onEndRunnable) {
            this.mCropBounds = cropBounds;
            this.mRotation = rotation;
            this.mOutWidth = outWidth;
            this.mOutHeight = outHeight;
            this.mSetWallpaper = setWallpaper;
            this.mSaveCroppedBitmap = saveCroppedBitmap;
            this.mOnEndRunnable = onEndRunnable;
        }

        public void setOnBitmapCropped(OnBitmapCroppedHandler handler) {
            this.mOnBitmapCroppedHandler = handler;
        }

        private InputStream regenerateInputStream() {
            InputStream bufferedInputStream;
            if (this.mInUri == null && this.mInResId == 0 && this.mInFilePath == null && this.mInImageBytes == null) {
                Log.w("Launcher3.CropActivity", "cannot read original file, no input URI, resource ID, or image byte array given");
            } else {
                try {
                    if (this.mInUri != null) {
                        bufferedInputStream = new BufferedInputStream(this.mContext.getContentResolver().openInputStream(this.mInUri));
                    } else if (this.mInFilePath != null) {
                        bufferedInputStream = this.mContext.openFileInput(this.mInFilePath);
                    } else if (this.mInImageBytes != null) {
                        bufferedInputStream = new BufferedInputStream(new ByteArrayInputStream(this.mInImageBytes));
                    } else {
                        bufferedInputStream = new BufferedInputStream(this.mResources.openRawResource(this.mInResId));
                    }
                    return bufferedInputStream;
                } catch (FileNotFoundException e) {
                    Log.w("Launcher3.CropActivity", "cannot read file: " + this.mInUri.toString(), e);
                }
            }
            return null;
        }

        public Point getImageBounds() {
            InputStream is = regenerateInputStream();
            if (is == null) {
                return null;
            }
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(is, null, options);
            Utils.closeSilently(is);
            if (options.outWidth == 0 || options.outHeight == 0) {
                return null;
            }
            return new Point(options.outWidth, options.outHeight);
        }

        public boolean cropBitmap() {
            boolean failure = false;
            WallpaperManager wallpaperManager = this.mSetWallpaper ? WallpaperManager.getInstance(this.mContext.getApplicationContext()) : null;
            if (this.mSetWallpaper && this.mNoCrop) {
                try {
                    InputStream is = regenerateInputStream();
                    if (is != null) {
                        wallpaperManager.setStream(is);
                        Utils.closeSilently(is);
                    }
                } catch (IOException e) {
                    Log.w("Launcher3.CropActivity", "cannot write stream to wallpaper", e);
                    failure = true;
                }
                return !failure;
            }
            Rect roundedTrueCrop = new Rect();
            Matrix rotateMatrix = new Matrix();
            Matrix inverseRotateMatrix = new Matrix();
            Point bounds = getImageBounds();
            if (this.mRotation > 0) {
                rotateMatrix.setRotate(this.mRotation);
                inverseRotateMatrix.setRotate(-this.mRotation);
                this.mCropBounds.roundOut(roundedTrueCrop);
                this.mCropBounds = new RectF(roundedTrueCrop);
                if (bounds == null) {
                    Log.w("Launcher3.CropActivity", "cannot get bounds for image");
                    return false;
                }
                float[] rotatedBounds = {bounds.x, bounds.y};
                rotateMatrix.mapPoints(rotatedBounds);
                rotatedBounds[0] = Math.abs(rotatedBounds[0]);
                rotatedBounds[1] = Math.abs(rotatedBounds[1]);
                this.mCropBounds.offset((-rotatedBounds[0]) / 2.0f, (-rotatedBounds[1]) / 2.0f);
                inverseRotateMatrix.mapRect(this.mCropBounds);
                this.mCropBounds.offset(bounds.x / 2, bounds.y / 2);
            }
            this.mCropBounds.roundOut(roundedTrueCrop);
            if (roundedTrueCrop.width() <= 0 || roundedTrueCrop.height() <= 0) {
                Log.w("Launcher3.CropActivity", "crop has bad values for full size image");
                return false;
            }
            int scaleDownSampleSize = Math.max(1, Math.min(roundedTrueCrop.width() / this.mOutWidth, roundedTrueCrop.height() / this.mOutHeight));
            BitmapRegionDecoder decoder = null;
            InputStream is2 = null;
            try {
                is2 = regenerateInputStream();
            } catch (IOException e2) {
                Log.w("Launcher3.CropActivity", "cannot open region decoder for file: " + this.mInUri.toString(), e2);
            } finally {
                Utils.closeSilently(is2);
            }
            if (is2 == null) {
                Log.w("Launcher3.CropActivity", "cannot get input stream for uri=" + this.mInUri.toString());
                return false;
            }
            decoder = BitmapRegionDecoder.newInstance(is2, false);
            Utils.closeSilently(is2);
            Bitmap crop = null;
            if (decoder != null) {
                BitmapFactory.Options options = new BitmapFactory.Options();
                if (scaleDownSampleSize > 1) {
                    options.inSampleSize = scaleDownSampleSize;
                }
                crop = decoder.decodeRegion(roundedTrueCrop, options);
                decoder.recycle();
            }
            if (crop == null) {
                InputStream is3 = regenerateInputStream();
                Bitmap fullSize = null;
                if (is3 != null) {
                    BitmapFactory.Options options2 = new BitmapFactory.Options();
                    if (scaleDownSampleSize > 1) {
                        options2.inSampleSize = scaleDownSampleSize;
                    }
                    fullSize = BitmapFactory.decodeStream(is3, null, options2);
                    Utils.closeSilently(is3);
                }
                if (fullSize != null) {
                    int scaleDownSampleSize2 = bounds.x / fullSize.getWidth();
                    this.mCropBounds.left /= scaleDownSampleSize2;
                    this.mCropBounds.top /= scaleDownSampleSize2;
                    this.mCropBounds.bottom /= scaleDownSampleSize2;
                    this.mCropBounds.right /= scaleDownSampleSize2;
                    this.mCropBounds.roundOut(roundedTrueCrop);
                    if (roundedTrueCrop.width() > fullSize.getWidth()) {
                        roundedTrueCrop.right = roundedTrueCrop.left + fullSize.getWidth();
                    }
                    if (roundedTrueCrop.right > fullSize.getWidth()) {
                        int adjustment = roundedTrueCrop.left - Math.max(0, roundedTrueCrop.right - roundedTrueCrop.width());
                        roundedTrueCrop.left -= adjustment;
                        roundedTrueCrop.right -= adjustment;
                    }
                    if (roundedTrueCrop.height() > fullSize.getHeight()) {
                        roundedTrueCrop.bottom = roundedTrueCrop.top + fullSize.getHeight();
                    }
                    if (roundedTrueCrop.bottom > fullSize.getHeight()) {
                        int adjustment2 = roundedTrueCrop.top - Math.max(0, roundedTrueCrop.bottom - roundedTrueCrop.height());
                        roundedTrueCrop.top -= adjustment2;
                        roundedTrueCrop.bottom -= adjustment2;
                    }
                    crop = Bitmap.createBitmap(fullSize, roundedTrueCrop.left, roundedTrueCrop.top, roundedTrueCrop.width(), roundedTrueCrop.height());
                }
            }
            if (crop == null) {
                Log.w("Launcher3.CropActivity", "cannot decode file: " + this.mInUri.toString());
                return false;
            }
            if ((this.mOutWidth > 0 && this.mOutHeight > 0) || this.mRotation > 0) {
                float[] dimsAfter = {crop.getWidth(), crop.getHeight()};
                rotateMatrix.mapPoints(dimsAfter);
                dimsAfter[0] = Math.abs(dimsAfter[0]);
                dimsAfter[1] = Math.abs(dimsAfter[1]);
                if (this.mOutWidth <= 0 || this.mOutHeight <= 0) {
                    this.mOutWidth = Math.round(dimsAfter[0]);
                    this.mOutHeight = Math.round(dimsAfter[1]);
                }
                RectF cropRect = new RectF(0.0f, 0.0f, dimsAfter[0], dimsAfter[1]);
                RectF returnRect = new RectF(0.0f, 0.0f, this.mOutWidth, this.mOutHeight);
                Matrix m = new Matrix();
                if (this.mRotation == 0) {
                    m.setRectToRect(cropRect, returnRect, Matrix.ScaleToFit.FILL);
                } else {
                    Matrix m1 = new Matrix();
                    m1.setTranslate((-crop.getWidth()) / 2.0f, (-crop.getHeight()) / 2.0f);
                    Matrix m2 = new Matrix();
                    m2.setRotate(this.mRotation);
                    Matrix m3 = new Matrix();
                    m3.setTranslate(dimsAfter[0] / 2.0f, dimsAfter[1] / 2.0f);
                    Matrix m4 = new Matrix();
                    m4.setRectToRect(cropRect, returnRect, Matrix.ScaleToFit.FILL);
                    Matrix c1 = new Matrix();
                    c1.setConcat(m2, m1);
                    Matrix c2 = new Matrix();
                    c2.setConcat(m4, m3);
                    m.setConcat(c2, c1);
                }
                Bitmap tmp = Bitmap.createBitmap((int) returnRect.width(), (int) returnRect.height(), Bitmap.Config.ARGB_8888);
                if (tmp != null) {
                    Canvas c = new Canvas(tmp);
                    Paint p = new Paint();
                    p.setFilterBitmap(true);
                    c.drawBitmap(crop, m, p);
                    crop = tmp;
                }
            }
            if (this.mSaveCroppedBitmap) {
                this.mCroppedBitmap = crop;
            }
            Bitmap.CompressFormat cf = WallpaperCropActivity.convertExtensionToCompressFormat(WallpaperCropActivity.getFileExtension(this.mOutputFormat));
            ByteArrayOutputStream tmpOut = new ByteArrayOutputStream(2048);
            if (!crop.compress(cf, 90, tmpOut)) {
                Log.w("Launcher3.CropActivity", "cannot compress bitmap");
                failure = true;
            } else if (this.mSetWallpaper && wallpaperManager != null) {
                try {
                    byte[] outByteArray = tmpOut.toByteArray();
                    wallpaperManager.setStream(new ByteArrayInputStream(outByteArray));
                    if (this.mOnBitmapCroppedHandler != null) {
                        this.mOnBitmapCroppedHandler.onBitmapCropped(outByteArray);
                    }
                } catch (IOException e3) {
                    Log.w("Launcher3.CropActivity", "cannot write stream to wallpaper", e3);
                    failure = true;
                }
            }
            return !failure;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            return Boolean.valueOf(cropBitmap());
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (this.mOnEndRunnable != null) {
                this.mOnEndRunnable.run();
            }
        }
    }

    protected static Bitmap.CompressFormat convertExtensionToCompressFormat(String extension) {
        return extension.equals("png") ? Bitmap.CompressFormat.PNG : Bitmap.CompressFormat.JPEG;
    }

    protected static String getFileExtension(String requestFormat) {
        String outputFormat = (requestFormat == null ? "jpg" : requestFormat).toLowerCase();
        return (outputFormat.equals("png") || outputFormat.equals("gif")) ? "png" : "jpg";
    }
}
