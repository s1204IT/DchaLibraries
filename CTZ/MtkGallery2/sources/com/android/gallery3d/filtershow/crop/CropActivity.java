package com.android.gallery3d.filtershow.crop;

import android.app.ActionBar;
import android.app.Activity;
import android.app.WallpaperManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.Toast;
import com.android.gallery3d.R;
import com.android.gallery3d.common.BitmapUtils;
import com.android.gallery3d.common.Utils;
import com.android.gallery3d.exif.ExifInterface;
import com.android.gallery3d.filtershow.cache.ImageLoader;
import com.android.gallery3d.filtershow.tools.SaveImage;
import com.mediatek.gallery3d.adapter.FeatureManager;
import com.mediatek.gallery3d.util.FeatureConfig;
import com.mediatek.gallery3d.util.Log;
import com.mediatek.gallery3d.util.PermissionHelper;
import com.mediatek.gallerybasic.base.IFilterShowImageLoader;
import com.mediatek.gallerybasic.util.DecodeSpecLimitor;
import com.mediatek.omadrm.OmaDrmInfoRequest;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.TimeZone;

public class CropActivity extends Activity {
    private static final int CROP_PHOTO_SIZE_LIMIT;
    private static IFilterShowImageLoader[] sExtLoaders;
    private CropExtras mCropExtras = null;
    private LoadBitmapTask mLoadBitmapTask = null;
    private int mOutputX = 0;
    private int mOutputY = 0;
    private Bitmap mOriginalBitmap = null;
    private RectF mOriginalBounds = null;
    private int mOriginalRotation = 0;
    private Uri mSourceUri = null;
    private CropView mCropView = null;
    private View mSaveButton = null;
    private boolean finalIOGuard = false;
    BitmapIOTask mIoTask = null;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        Intent intent = getIntent();
        setResult(0, new Intent());
        this.mCropExtras = getExtrasFromIntent(intent);
        if (this.mCropExtras != null && this.mCropExtras.getShowWhenLocked()) {
            getWindow().addFlags(524288);
        }
        setContentView(R.layout.crop_activity);
        this.mCropView = (CropView) findViewById(R.id.cropView);
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayOptions(16);
            actionBar.setCustomView(R.layout.filtershow_actionbar);
            this.mSaveButton = actionBar.getCustomView();
            this.mSaveButton.setEnabled(false);
            this.mSaveButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    CropActivity.this.startFinishOutput();
                }
            });
        }
        if (PermissionHelper.checkAndRequestForGallery(this)) {
            if (intent.getData() != null) {
                this.mSourceUri = intent.getData();
                startLoadBitmap(this.mSourceUri);
                return;
            } else {
                pickImage();
                return;
            }
        }
        findViewById(R.id.loading).setVisibility(4);
    }

    private void enableSave(boolean z) {
        if (this.mSaveButton != null) {
            this.mSaveButton.setEnabled(z);
        }
    }

    @Override
    protected void onDestroy() {
        if (this.mLoadBitmapTask != null) {
            this.mLoadBitmapTask.cancel(false);
        }
        if (this.mIoTask != null) {
            this.mIoTask.cancel(false);
            this.mIoTask = null;
        }
        super.onDestroy();
    }

    @Override
    public void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        this.mCropView.configChanged();
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayOptions(16);
            actionBar.setCustomView(R.layout.filtershow_actionbar);
            this.mSaveButton = actionBar.getCustomView();
            this.mSaveButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    CropActivity.this.startFinishOutput();
                }
            });
        }
    }

    private void pickImage() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction("android.intent.action.GET_CONTENT");
        startActivityForResult(Intent.createChooser(intent, getString(R.string.select_image)), 1);
    }

    @Override
    public void onActivityResult(int i, int i2, Intent intent) {
        if (i2 == -1 && i == 1) {
            this.mSourceUri = intent.getData();
            startLoadBitmap(this.mSourceUri);
        }
    }

    private int getScreenImageSize() {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        return Math.max(displayMetrics.heightPixels, displayMetrics.widthPixels);
    }

    private void startLoadBitmap(Uri uri) {
        try {
            if (!DecodeSpecLimitor.isOutOfSpecLimit(getApplicationContext(), uri) && uri != null) {
                enableSave(false);
                findViewById(R.id.loading).setVisibility(0);
                this.mLoadBitmapTask = new LoadBitmapTask();
                this.mLoadBitmapTask.execute(uri);
                return;
            }
            cannotLoadImage();
            done();
        } catch (SecurityException e) {
            Log.w("Gallery2/CropActivity", "cannot load file: " + uri.toString(), e);
            cannotLoadImage();
            done();
        }
    }

    private void doneLoadBitmap(Bitmap bitmap, RectF rectF, int i) {
        findViewById(R.id.loading).setVisibility(8);
        this.mSaveButton.setEnabled(true);
        this.mOriginalBitmap = bitmap;
        this.mOriginalBounds = rectF;
        this.mOriginalRotation = i;
        if (bitmap != null && bitmap.getWidth() != 0 && bitmap.getHeight() != 0) {
            RectF rectF2 = new RectF(0.0f, 0.0f, bitmap.getWidth(), bitmap.getHeight());
            this.mCropView.initialize(bitmap, rectF2, rectF2, i);
            if (this.mCropExtras != null) {
                int aspectX = this.mCropExtras.getAspectX();
                int aspectY = this.mCropExtras.getAspectY();
                this.mOutputX = this.mCropExtras.getOutputX();
                this.mOutputY = this.mCropExtras.getOutputY();
                if (this.mOutputX > 0 && this.mOutputY > 0) {
                    this.mCropView.applyAspect(this.mOutputX, this.mOutputY);
                }
                float spotlightX = this.mCropExtras.getSpotlightX();
                float spotlightY = this.mCropExtras.getSpotlightY();
                if (spotlightX > 0.0f && spotlightY > 0.0f) {
                    this.mCropView.setWallpaperSpotlight(spotlightX, spotlightY);
                }
                if (aspectX > 0 && aspectY > 0) {
                    this.mCropView.applyAspect(aspectX, aspectY);
                }
            }
            enableSave(true);
            return;
        }
        Log.w("Gallery2/CropActivity", "could not load image for cropping");
        cannotLoadImage();
        setResult(0, new Intent());
        done();
    }

    private void cannotLoadImage() {
        Toast.makeText(this, getString(R.string.cannot_load_image), 0).show();
    }

    private class LoadBitmapTask extends AsyncTask<Uri, Void, Bitmap> {
        int mBitmapSize;
        Context mContext;
        Rect mOriginalBounds = new Rect();
        int mOrientation = 0;

        public LoadBitmapTask() {
            this.mBitmapSize = CropActivity.this.getScreenImageSize();
            this.mContext = CropActivity.this.getApplicationContext();
        }

        @Override
        protected Bitmap doInBackground(Uri... uriArr) throws Throwable {
            Uri uri = uriArr[0];
            if (Runtime.getRuntime().maxMemory() < 134217728) {
                this.mBitmapSize /= 2;
            }
            Bitmap bitmapLoadConstrainedBitmap = ImageLoader.loadConstrainedBitmap(uri, this.mContext, this.mBitmapSize, this.mOriginalBounds, false);
            this.mOrientation = ImageLoader.getMetadataRotation(this.mContext, uri);
            return bitmapLoadConstrainedBitmap;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            CropActivity.this.doneLoadBitmap(bitmap, new RectF(this.mOriginalBounds), this.mOrientation);
        }
    }

    protected void startFinishOutput() {
        int i;
        Uri uriMakeAndInsertUri;
        if (this.finalIOGuard) {
            return;
        }
        this.finalIOGuard = true;
        enableSave(false);
        this.mCropView.enableTouchMotion(false);
        if (this.mOriginalBitmap != null && this.mCropExtras != null) {
            if (this.mCropExtras.getExtraOutput() == null) {
                i = 0;
                uriMakeAndInsertUri = null;
            } else {
                uriMakeAndInsertUri = this.mCropExtras.getExtraOutput();
                i = uriMakeAndInsertUri != null ? 4 : 0;
            }
            if (this.mCropExtras.getSetAsWallpaper()) {
                i |= 1;
            }
            if (this.mCropExtras.getReturnData()) {
                i |= 2;
            }
        } else {
            i = 0;
            uriMakeAndInsertUri = null;
        }
        if (i == 0 && (uriMakeAndInsertUri = SaveImage.makeAndInsertUri(this, this.mSourceUri)) != null) {
            i |= 4;
        }
        Uri uri = uriMakeAndInsertUri;
        int i2 = i;
        if ((i2 & 7) == 0 || this.mOriginalBitmap == null) {
            setResult(0, new Intent());
            done();
        } else {
            RectF rectF = new RectF(0.0f, 0.0f, this.mOriginalBitmap.getWidth(), this.mOriginalBitmap.getHeight());
            startBitmapIO(i2, this.mOriginalBitmap, this.mSourceUri, uri, getBitmapCrop(rectF), rectF, this.mOriginalBounds, this.mCropExtras != null ? this.mCropExtras.getOutputFormat() : null, this.mOriginalRotation);
        }
    }

    private void startBitmapIO(int i, Bitmap bitmap, Uri uri, Uri uri2, RectF rectF, RectF rectF2, RectF rectF3, String str, int i2) {
        if (rectF == null || rectF2 == null || bitmap == null || bitmap.getWidth() == 0 || bitmap.getHeight() == 0 || rectF.width() == 0.0f || rectF.height() == 0.0f || rectF2.width() == 0.0f || rectF2.height() == 0.0f || (i & 7) == 0) {
            return;
        }
        if ((i & 1) != 0) {
            Toast.makeText(this, R.string.setting_wallpaper, 1).show();
        }
        findViewById(R.id.loading).setVisibility(0);
        if (this.mIoTask != null) {
            this.mIoTask.cancel(false);
            this.mIoTask = null;
        }
        this.mIoTask = new BitmapIOTask(uri, uri2, str, i, rectF, rectF2, rectF3, i2, this.mOutputX, this.mOutputY);
        this.mIoTask.execute(bitmap);
    }

    private void doneBitmapIO(boolean z, Intent intent) {
        findViewById(R.id.loading).setVisibility(8);
        if (z) {
            setResult(-1, intent);
        } else {
            setResult(0, intent);
        }
        done();
    }

    private class BitmapIOTask extends AsyncTask<Bitmap, Void, Boolean> {
        static final boolean $assertionsDisabled = false;
        RectF mCrop;
        File mFile;
        int mFlags;
        InputStream mInStream = null;
        Uri mInUri;
        RectF mOrig;
        OutputStream mOutStream;
        Uri mOutUri;
        String mOutputFormat;
        RectF mPhoto;
        Intent mResultIntent;
        int mRotation;
        private final WallpaperManager mWPManager;

        private void regenerateInputStream() {
            if (this.mInUri == null) {
                Log.w("Gallery2/CropActivity", "cannot read original file, no input URI given");
                return;
            }
            Utils.closeSilently(this.mInStream);
            try {
                this.mInStream = CropActivity.this.getContentResolver().openInputStream(this.mInUri);
            } catch (FileNotFoundException e) {
                Log.w("Gallery2/CropActivity", "cannot read file: " + this.mInUri.toString(), e);
            }
        }

        public BitmapIOTask(Uri uri, Uri uri2, String str, int i, RectF rectF, RectF rectF2, RectF rectF3, int i2, int i3, int i4) {
            this.mOutStream = null;
            this.mOutputFormat = null;
            this.mOutUri = null;
            this.mInUri = null;
            this.mFlags = 0;
            this.mCrop = null;
            this.mPhoto = null;
            this.mOrig = null;
            this.mResultIntent = null;
            this.mRotation = 0;
            this.mOutputFormat = str;
            this.mOutStream = null;
            this.mOutUri = uri2;
            this.mInUri = uri;
            this.mFlags = i;
            this.mCrop = rectF;
            this.mPhoto = rectF2;
            this.mOrig = rectF3;
            this.mWPManager = WallpaperManager.getInstance(CropActivity.this.getApplicationContext());
            this.mResultIntent = new Intent();
            this.mRotation = i2 < 0 ? -i2 : i2;
            this.mRotation %= 360;
            this.mRotation = 90 * (this.mRotation / 90);
            CropActivity.this.mOutputX = i3;
            CropActivity.this.mOutputY = i4;
            if ((i & 5) != 0) {
                regenerateInputStream();
            }
        }

        @Override
        protected Boolean doInBackground(Bitmap... bitmapArr) {
            boolean z;
            BitmapRegionDecoder bitmapRegionDecoderNewInstance;
            Bitmap bitmapCreateBitmap;
            Bitmap bitmapLoadBitmapFromExt;
            Bitmap bitmap = bitmapArr[0];
            if (this.mCrop != null && this.mPhoto != null && this.mOrig != null) {
                RectF scaledCropBounds = CropMath.getScaledCropBounds(this.mCrop, this.mPhoto, this.mOrig);
                Matrix matrix = new Matrix();
                matrix.setRotate(this.mRotation);
                matrix.mapRect(scaledCropBounds);
                if (scaledCropBounds != null) {
                    Rect rect = new Rect();
                    scaledCropBounds.roundOut(rect);
                    this.mResultIntent.putExtra("cropped-rect", rect);
                }
            }
            if ((this.mFlags & 2) != 0 || (CropActivity.this.mCropExtras != null && CropActivity.this.mCropExtras.getReturnDataCompressed())) {
                Bitmap croppedImage = CropActivity.getCroppedImage(bitmap, this.mCrop, this.mPhoto);
                if (croppedImage != null && (CropActivity.this.mCropExtras == null || !CropActivity.this.mCropExtras.getScaleUp())) {
                    croppedImage = CropActivity.getDownsampledBitmap(croppedImage, 750000);
                }
                if (croppedImage == null) {
                    Log.w("Gallery2/CropActivity", "could not downsample bitmap to return in data");
                    z = true;
                } else {
                    if (this.mRotation > 0) {
                        Matrix matrix2 = new Matrix();
                        matrix2.setRotate(this.mRotation);
                        Bitmap bitmapCreateBitmap2 = Bitmap.createBitmap(croppedImage, 0, 0, croppedImage.getWidth(), croppedImage.getHeight(), matrix2, true);
                        if (bitmapCreateBitmap2 != null) {
                            croppedImage = bitmapCreateBitmap2;
                        }
                    }
                    Rect rect2 = new Rect(0, 0, croppedImage.getWidth(), croppedImage.getHeight());
                    if (CropActivity.this.mCropExtras != null && (CropActivity.this.mCropExtras.getScaleUp() || (CropActivity.this.mOutputX > 0 && CropActivity.this.mOutputY > 0))) {
                        Bitmap bitmapCreateBitmap3 = Bitmap.createBitmap(CropActivity.this.mOutputX, CropActivity.this.mOutputY, Bitmap.Config.ARGB_8888);
                        new Canvas(bitmapCreateBitmap3).drawBitmap(croppedImage, rect2, new Rect(0, 0, CropActivity.this.mOutputX, CropActivity.this.mOutputY), (Paint) null);
                        croppedImage = bitmapCreateBitmap3;
                    }
                    if (CropActivity.this.mCropExtras != null && CropActivity.this.mCropExtras.getReturnDataCompressed()) {
                        byte[] bArrCompressToBytes = BitmapUtils.compressToBytes(croppedImage);
                        croppedImage.recycle();
                        this.mResultIntent.putExtra("data-compress", bArrCompressToBytes);
                    } else {
                        this.mResultIntent.putExtra(OmaDrmInfoRequest.KEY_DATA, croppedImage);
                    }
                    this.mResultIntent.setData(this.mInUri);
                    z = false;
                }
            } else {
                z = false;
            }
            if ((this.mFlags & 5) != 0 && this.mInStream != null) {
                RectF scaledCropBounds2 = CropMath.getScaledCropBounds(this.mCrop, this.mPhoto, this.mOrig);
                if (scaledCropBounds2 == null) {
                    Log.w("Gallery2/CropActivity", "cannot find crop for full size image");
                    return false;
                }
                Rect rect3 = new Rect();
                scaledCropBounds2.roundOut(rect3);
                if (rect3.width() <= 0 || rect3.height() <= 0) {
                    Log.w("Gallery2/CropActivity", "crop has bad values for full size image");
                    return false;
                }
                try {
                    bitmapRegionDecoderNewInstance = BitmapRegionDecoder.newInstance(this.mInStream, true);
                } catch (IOException e) {
                    Log.w("Gallery2/CropActivity", "cannot open region decoder for file: " + this.mInUri.toString(), e);
                    bitmapRegionDecoderNewInstance = null;
                }
                if (bitmapRegionDecoderNewInstance != null) {
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inMutable = true;
                    int iCeil = (int) Math.ceil((((double) rect3.width()) * ((double) rect3.height())) / ((double) CropActivity.CROP_PHOTO_SIZE_LIMIT));
                    if (iCeil < 1) {
                        iCeil = 1;
                    }
                    Log.d("Gallery2/CropActivity", "<BitmapIOTask.doInBackground> sIsLowRamDevice " + FeatureConfig.sIsLowRamDevice + ", decode sample size " + iCeil);
                    options.inSampleSize = iCeil;
                    bitmapCreateBitmap = bitmapRegionDecoderNewInstance.decodeRegion(rect3, options);
                    bitmapRegionDecoderNewInstance.recycle();
                } else {
                    bitmapCreateBitmap = null;
                }
                if (bitmapCreateBitmap == null && (bitmapLoadBitmapFromExt = CropActivity.loadBitmapFromExt(CropActivity.this, CropActivity.this.mSourceUri)) != null) {
                    bitmapCreateBitmap = Bitmap.createBitmap(bitmapLoadBitmapFromExt, rect3.left, rect3.top, rect3.width(), rect3.height());
                    bitmapLoadBitmapFromExt.recycle();
                }
                if (bitmapCreateBitmap == null) {
                    regenerateInputStream();
                    Bitmap bitmapDecodeStream = this.mInStream != null ? BitmapFactory.decodeStream(this.mInStream) : null;
                    if (bitmapDecodeStream != null) {
                        bitmapCreateBitmap = Bitmap.createBitmap(bitmapDecodeStream, rect3.left, rect3.top, rect3.width(), rect3.height());
                    }
                }
                Bitmap bitmapReplaceBackgroundColor = com.mediatek.gallerybasic.util.BitmapUtils.replaceBackgroundColor(bitmapCreateBitmap, true);
                if (bitmapReplaceBackgroundColor != null) {
                    if (CropActivity.this.mOutputX > 0 && CropActivity.this.mOutputY > 0) {
                        Matrix matrix3 = new Matrix();
                        RectF rectF = new RectF(0.0f, 0.0f, bitmapReplaceBackgroundColor.getWidth(), bitmapReplaceBackgroundColor.getHeight());
                        if (this.mRotation > 0) {
                            matrix3.setRotate(this.mRotation);
                            matrix3.mapRect(rectF);
                        }
                        RectF rectF2 = new RectF(0.0f, 0.0f, CropActivity.this.mOutputX, CropActivity.this.mOutputY);
                        matrix3.setRectToRect(rectF, rectF2, Matrix.ScaleToFit.FILL);
                        matrix3.preRotate(this.mRotation);
                        Bitmap bitmapCreateBitmap4 = Bitmap.createBitmap((int) rectF2.width(), (int) rectF2.height(), Bitmap.Config.ARGB_8888);
                        if (bitmapCreateBitmap4 != null) {
                            new Canvas(bitmapCreateBitmap4).drawBitmap(bitmapReplaceBackgroundColor, matrix3, new Paint());
                            bitmapReplaceBackgroundColor = bitmapCreateBitmap4;
                        }
                    } else if (this.mRotation > 0) {
                        Matrix matrix4 = new Matrix();
                        matrix4.setRotate(this.mRotation);
                        Bitmap bitmapCreateBitmap5 = Bitmap.createBitmap(bitmapReplaceBackgroundColor, 0, 0, bitmapReplaceBackgroundColor.getWidth(), bitmapReplaceBackgroundColor.getHeight(), matrix4, true);
                        if (bitmapCreateBitmap5 != null) {
                            bitmapReplaceBackgroundColor = bitmapCreateBitmap5;
                        }
                    }
                    if ((this.mFlags & 4) != 0) {
                        this.mFile = CropActivity.this.getFile(this.mOutUri);
                        if (this.mFile != null) {
                            this.mOutStream = CropActivity.this.getOutputStream(this.mFile);
                        }
                        if (this.mOutStream == null) {
                            this.mOutStream = CropActivity.this.getOutputStream(this.mOutUri);
                        }
                    }
                    Bitmap.CompressFormat compressFormatConvertExtensionToCompressFormat = CropActivity.convertExtensionToCompressFormat(CropActivity.getFileExtension(this.mOutputFormat));
                    if (this.mOutputFormat == null || this.mOutputFormat.equalsIgnoreCase("jpg")) {
                        this.mOutStream = CropActivity.this.getExifData(this.mOutStream);
                    }
                    if (this.mFlags == 4) {
                        if (this.mOutStream == null || !bitmapReplaceBackgroundColor.compress(compressFormatConvertExtensionToCompressFormat, 90, this.mOutStream)) {
                            Log.w("Gallery2/CropActivity", "failed to compress bitmap to file: " + this.mOutUri.toString());
                            z = true;
                        } else {
                            SaveImage.updataImageDimensionInDB(CropActivity.this.getApplicationContext(), this.mFile, bitmapReplaceBackgroundColor.getWidth(), bitmapReplaceBackgroundColor.getHeight());
                            this.mResultIntent.setData(this.mOutUri);
                        }
                    } else {
                        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(2048);
                        if (bitmapReplaceBackgroundColor.compress(compressFormatConvertExtensionToCompressFormat, 90, byteArrayOutputStream)) {
                            if ((this.mFlags & 4) != 0) {
                                if (this.mOutStream == null) {
                                    Log.w("Gallery2/CropActivity", "failed to compress bitmap to file: " + this.mOutUri.toString());
                                } else {
                                    try {
                                        this.mOutStream.write(byteArrayOutputStream.toByteArray());
                                        SaveImage.updataImageDimensionInDB(CropActivity.this.getApplicationContext(), this.mFile, bitmapReplaceBackgroundColor.getWidth(), bitmapReplaceBackgroundColor.getHeight());
                                        this.mResultIntent.setData(this.mOutUri);
                                    } catch (IOException e2) {
                                        Log.w("Gallery2/CropActivity", "failed to compress bitmap to file: " + this.mOutUri.toString(), e2);
                                        z = true;
                                    }
                                }
                                z = true;
                            }
                            if ((this.mFlags & 1) != 0 && this.mWPManager != null) {
                                if (this.mWPManager == null) {
                                    Log.w("Gallery2/CropActivity", "no wallpaper manager");
                                } else {
                                    try {
                                        this.mWPManager.setStream(new ByteArrayInputStream(byteArrayOutputStream.toByteArray()));
                                    } catch (IOException e3) {
                                        Log.w("Gallery2/CropActivity", "cannot write stream to wallpaper", e3);
                                        z = true;
                                    }
                                }
                            }
                        } else {
                            Log.w("Gallery2/CropActivity", "cannot compress bitmap");
                        }
                        z = true;
                    }
                } else {
                    Log.w("Gallery2/CropActivity", "cannot decode file: " + this.mInUri.toString());
                    return false;
                }
            }
            return Boolean.valueOf(!z);
        }

        @Override
        protected void onPostExecute(Boolean bool) {
            Utils.closeSilently(this.mOutStream);
            Utils.closeSilently(this.mInStream);
            CropActivity.this.doneBitmapIO(bool.booleanValue(), this.mResultIntent);
        }
    }

    private void done() {
        finish();
    }

    protected static Bitmap getCroppedImage(Bitmap bitmap, RectF rectF, RectF rectF2) {
        RectF scaledCropBounds = CropMath.getScaledCropBounds(rectF, rectF2, new RectF(0.0f, 0.0f, bitmap.getWidth(), bitmap.getHeight()));
        if (scaledCropBounds == null) {
            return null;
        }
        Rect rect = new Rect();
        scaledCropBounds.roundOut(rect);
        return Bitmap.createBitmap(bitmap, rect.left, rect.top, rect.width(), rect.height());
    }

    protected static Bitmap getDownsampledBitmap(Bitmap bitmap, int i) {
        if (bitmap == null || bitmap.getWidth() == 0 || bitmap.getHeight() == 0 || i < 16) {
            throw new IllegalArgumentException("Bad argument to getDownsampledBitmap()");
        }
        int i2 = 0;
        for (int bitmapSize = CropMath.getBitmapSize(bitmap); bitmapSize > i; bitmapSize /= 4) {
            i2++;
        }
        Bitmap bitmapCreateScaledBitmap = Bitmap.createScaledBitmap(bitmap, bitmap.getWidth() >> i2, bitmap.getHeight() >> i2, true);
        if (bitmapCreateScaledBitmap == null) {
            return null;
        }
        if (CropMath.getBitmapSize(bitmapCreateScaledBitmap) > i) {
            return Bitmap.createScaledBitmap(bitmapCreateScaledBitmap, bitmapCreateScaledBitmap.getWidth() >> 1, bitmapCreateScaledBitmap.getHeight() >> 1, true);
        }
        return bitmapCreateScaledBitmap;
    }

    protected static CropExtras getExtrasFromIntent(Intent intent) {
        Bundle extras = intent.getExtras();
        if (extras != null) {
            return new CropExtras(extras.getInt("outputX", 0), extras.getInt("outputY", 0), extras.getBoolean("scale", true) && extras.getBoolean("scaleUpIfNeeded", false), extras.getInt("aspectX", 0), extras.getInt("aspectY", 0), extras.getBoolean("set-as-wallpaper", false), extras.getBoolean("return-data", false), (Uri) extras.getParcelable("output"), extras.getString("outputFormat"), extras.getBoolean("showWhenLocked", false), extras.getFloat("spotlightX"), extras.getFloat("spotlightY"), extras.getBoolean("return-data-compress"));
        }
        return null;
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

    private RectF getBitmapCrop(RectF rectF) {
        RectF crop = this.mCropView.getCrop();
        RectF photo = this.mCropView.getPhoto();
        if (crop == null || photo == null) {
            Log.w("Gallery2/CropActivity", "could not get crop");
            return null;
        }
        RectF scaledCropBounds = CropMath.getScaledCropBounds(crop, photo, rectF);
        if (scaledCropBounds != null) {
            if (scaledCropBounds.height() == 0.0f) {
                scaledCropBounds.inset(0.0f, -1.0f);
            }
            if (scaledCropBounds.width() == 0.0f) {
                scaledCropBounds.inset(-1.0f, 0.0f);
            }
        }
        return scaledCropBounds;
    }

    @Override
    public void onRequestPermissionsResult(int i, String[] strArr, int[] iArr) {
        if (PermissionHelper.isAllPermissionsGranted(strArr, iArr)) {
            Log.d("Gallery2/CropActivity", "<onRequestPermissionsResult> all permission granted");
            Intent intent = getIntent();
            if (intent.getData() != null) {
                this.mSourceUri = intent.getData();
                startLoadBitmap(this.mSourceUri);
                return;
            } else {
                pickImage();
                return;
            }
        }
        int i2 = 0;
        while (true) {
            if (i2 < strArr.length) {
                if ("android.permission.READ_EXTERNAL_STORAGE".equals(strArr[i2]) && iArr[i2] == -1) {
                    PermissionHelper.showDeniedPrompt(this);
                    break;
                } else if (!"android.permission.WRITE_EXTERNAL_STORAGE".equals(strArr[i2]) || iArr[i2] != -1) {
                    i2++;
                } else {
                    PermissionHelper.showDeniedPrompt(this);
                    break;
                }
            } else {
                break;
            }
        }
        Log.d("Gallery2/CropActivity", "<onRequestPermissionsResult> permission denied, finish");
        finish();
    }

    private File getFile(Uri uri) {
        if (uri == null) {
            return null;
        }
        return SaveImage.getOutPutFile(getApplicationContext(), uri);
    }

    private OutputStream getOutputStream(Uri uri) {
        if (uri == null) {
            return null;
        }
        try {
            return getContentResolver().openOutputStream(uri);
        } catch (FileNotFoundException e) {
            Log.w("Gallery2/CropActivity", "cannot getOutPutStrem: " + uri.toString(), e);
            return null;
        }
    }

    private OutputStream getOutputStream(File file) {
        if (file == null) {
            return null;
        }
        try {
            return new FileOutputStream(file);
        } catch (FileNotFoundException e) {
            Log.w("Gallery2/CropActivity", "cannot getOutPutStrem: ", e);
            return null;
        }
    }

    private OutputStream getExifData(OutputStream outputStream) {
        if (outputStream != null) {
            ExifInterface exifInterface = new ExifInterface();
            updateExifData(exifInterface, System.currentTimeMillis());
            return exifInterface.getExifWriterStream(outputStream);
        }
        return outputStream;
    }

    private void updateExifData(ExifInterface exifInterface, long j) {
        exifInterface.addDateTimeStampTag(ExifInterface.TAG_DATE_TIME, j, TimeZone.getDefault());
        exifInterface.setTag(exifInterface.buildTag(ExifInterface.TAG_ORIENTATION, (short) 1));
        exifInterface.removeCompressedThumbnail();
    }

    static {
        CROP_PHOTO_SIZE_LIMIT = (FeatureConfig.sIsLowRamDevice || FeatureConfig.IS_GMO_RAM_OPTIMIZE) ? 5242880 : 10485760;
    }

    private static Bitmap loadBitmapFromExt(Context context, Uri uri) {
        if (sExtLoaders == null) {
            sExtLoaders = (IFilterShowImageLoader[]) FeatureManager.getInstance().getImplement(IFilterShowImageLoader.class, new Object[0]);
        }
        Bitmap bitmapLoadBitmap = null;
        for (IFilterShowImageLoader iFilterShowImageLoader : sExtLoaders) {
            bitmapLoadBitmap = iFilterShowImageLoader.loadBitmap(context, uri, new BitmapFactory.Options());
            if (bitmapLoadBitmap != null) {
                return bitmapLoadBitmap;
            }
        }
        return bitmapLoadBitmap;
    }
}
