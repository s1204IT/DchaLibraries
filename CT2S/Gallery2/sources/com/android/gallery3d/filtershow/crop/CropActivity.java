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
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import com.android.gallery3d.R;
import com.android.gallery3d.common.Utils;
import com.android.gallery3d.filtershow.cache.ImageLoader;
import com.android.gallery3d.filtershow.tools.SaveImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class CropActivity extends Activity {
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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
            View mSaveButton = actionBar.getCustomView();
            mSaveButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    CropActivity.this.startFinishOutput();
                }
            });
        }
        if (intent.getData() != null) {
            this.mSourceUri = intent.getData();
            startLoadBitmap(this.mSourceUri);
        } else {
            pickImage();
        }
    }

    private void enableSave(boolean enable) {
        if (this.mSaveButton != null) {
            this.mSaveButton.setEnabled(enable);
        }
    }

    @Override
    protected void onDestroy() {
        if (this.mLoadBitmapTask != null) {
            this.mLoadBitmapTask.cancel(false);
        }
        super.onDestroy();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        this.mCropView.configChanged();
    }

    private void pickImage() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction("android.intent.action.GET_CONTENT");
        startActivityForResult(Intent.createChooser(intent, getString(R.string.select_image)), 1);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == -1 && requestCode == 1) {
            this.mSourceUri = data.getData();
            startLoadBitmap(this.mSourceUri);
        }
    }

    private int getScreenImageSize() {
        DisplayMetrics outMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(outMetrics);
        return Math.max(outMetrics.heightPixels, outMetrics.widthPixels);
    }

    private void startLoadBitmap(Uri uri) {
        if (uri != null) {
            enableSave(false);
            View loading = findViewById(R.id.loading);
            loading.setVisibility(0);
            this.mLoadBitmapTask = new LoadBitmapTask();
            this.mLoadBitmapTask.execute(uri);
            return;
        }
        cannotLoadImage();
        done();
    }

    private void doneLoadBitmap(Bitmap bitmap, RectF bounds, int orientation) {
        View loading = findViewById(R.id.loading);
        loading.setVisibility(8);
        this.mOriginalBitmap = bitmap;
        this.mOriginalBounds = bounds;
        this.mOriginalRotation = orientation;
        if (bitmap != null && bitmap.getWidth() != 0 && bitmap.getHeight() != 0) {
            RectF imgBounds = new RectF(0.0f, 0.0f, bitmap.getWidth(), bitmap.getHeight());
            this.mCropView.initialize(bitmap, imgBounds, imgBounds, orientation);
            if (this.mCropExtras != null) {
                int aspectX = this.mCropExtras.getAspectX();
                int aspectY = this.mCropExtras.getAspectY();
                this.mOutputX = this.mCropExtras.getOutputX();
                this.mOutputY = this.mCropExtras.getOutputY();
                if (this.mOutputX > 0 && this.mOutputY > 0) {
                    this.mCropView.applyAspect(this.mOutputX, this.mOutputY);
                }
                float spotX = this.mCropExtras.getSpotlightX();
                float spotY = this.mCropExtras.getSpotlightY();
                if (spotX > 0.0f && spotY > 0.0f) {
                    this.mCropView.setWallpaperSpotlight(spotX, spotY);
                }
                if (aspectX > 0 && aspectY > 0) {
                    this.mCropView.applyAspect(aspectX, aspectY);
                }
            }
            enableSave(true);
            return;
        }
        Log.w("CropActivity", "could not load image for cropping");
        cannotLoadImage();
        setResult(0, new Intent());
        done();
    }

    private void cannotLoadImage() {
        CharSequence text = getString(R.string.cannot_load_image);
        Toast toast = Toast.makeText(this, text, 0);
        toast.show();
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
        protected Bitmap doInBackground(Uri... params) {
            Uri uri = params[0];
            Bitmap bmap = ImageLoader.loadConstrainedBitmap(uri, this.mContext, this.mBitmapSize, this.mOriginalBounds, false);
            this.mOrientation = ImageLoader.getMetadataRotation(this.mContext, uri);
            return bmap;
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            CropActivity.this.doneLoadBitmap(result, new RectF(this.mOriginalBounds), this.mOrientation);
        }
    }

    protected void startFinishOutput() {
        if (!this.finalIOGuard) {
            this.finalIOGuard = true;
            enableSave(false);
            Uri destinationUri = null;
            int flags = 0;
            if (this.mOriginalBitmap != null && this.mCropExtras != null) {
                if (this.mCropExtras.getExtraOutput() != null && (destinationUri = this.mCropExtras.getExtraOutput()) != null) {
                    flags = 0 | 4;
                }
                if (this.mCropExtras.getSetAsWallpaper()) {
                    flags |= 1;
                }
                if (this.mCropExtras.getReturnData()) {
                    flags |= 2;
                }
            }
            if (flags == 0 && (destinationUri = SaveImage.makeAndInsertUri(this, this.mSourceUri)) != null) {
                flags |= 4;
            }
            if ((flags & 7) != 0 && this.mOriginalBitmap != null) {
                RectF photo = new RectF(0.0f, 0.0f, this.mOriginalBitmap.getWidth(), this.mOriginalBitmap.getHeight());
                RectF crop = getBitmapCrop(photo);
                startBitmapIO(flags, this.mOriginalBitmap, this.mSourceUri, destinationUri, crop, photo, this.mOriginalBounds, this.mCropExtras == null ? null : this.mCropExtras.getOutputFormat(), this.mOriginalRotation);
            } else {
                setResult(0, new Intent());
                done();
            }
        }
    }

    private void startBitmapIO(int flags, Bitmap currentBitmap, Uri sourceUri, Uri destUri, RectF cropBounds, RectF photoBounds, RectF currentBitmapBounds, String format, int rotation) {
        if (cropBounds != null && photoBounds != null && currentBitmap != null && currentBitmap.getWidth() != 0 && currentBitmap.getHeight() != 0 && cropBounds.width() != 0.0f && cropBounds.height() != 0.0f && photoBounds.width() != 0.0f && photoBounds.height() != 0.0f && (flags & 7) != 0) {
            if ((flags & 1) != 0) {
                Toast.makeText(this, R.string.setting_wallpaper, 1).show();
            }
            View loading = findViewById(R.id.loading);
            loading.setVisibility(0);
            BitmapIOTask ioTask = new BitmapIOTask(sourceUri, destUri, format, flags, cropBounds, photoBounds, currentBitmapBounds, rotation, this.mOutputX, this.mOutputY);
            ioTask.execute(currentBitmap);
        }
    }

    private void doneBitmapIO(boolean success, Intent intent) {
        View loading = findViewById(R.id.loading);
        loading.setVisibility(8);
        if (success) {
            setResult(-1, intent);
        } else {
            setResult(0, intent);
        }
        done();
    }

    private class BitmapIOTask extends AsyncTask<Bitmap, Void, Boolean> {
        static final boolean $assertionsDisabled;
        RectF mCrop;
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

        static {
            $assertionsDisabled = !CropActivity.class.desiredAssertionStatus();
        }

        private void regenerateInputStream() {
            if (this.mInUri == null) {
                Log.w("CropActivity", "cannot read original file, no input URI given");
                return;
            }
            Utils.closeSilently(this.mInStream);
            try {
                this.mInStream = CropActivity.this.getContentResolver().openInputStream(this.mInUri);
            } catch (FileNotFoundException e) {
                Log.w("CropActivity", "cannot read file: " + this.mInUri.toString(), e);
            }
        }

        public BitmapIOTask(Uri sourceUri, Uri destUri, String outputFormat, int flags, RectF cropBounds, RectF photoBounds, RectF originalBitmapBounds, int rotation, int outputX, int outputY) {
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
            this.mOutputFormat = outputFormat;
            this.mOutStream = null;
            this.mOutUri = destUri;
            this.mInUri = sourceUri;
            this.mFlags = flags;
            this.mCrop = cropBounds;
            this.mPhoto = photoBounds;
            this.mOrig = originalBitmapBounds;
            this.mWPManager = WallpaperManager.getInstance(CropActivity.this.getApplicationContext());
            this.mResultIntent = new Intent();
            this.mRotation = rotation < 0 ? -rotation : rotation;
            this.mRotation %= 360;
            this.mRotation = (this.mRotation / 90) * 90;
            CropActivity.this.mOutputX = outputX;
            CropActivity.this.mOutputY = outputY;
            if ((flags & 4) != 0) {
                if (this.mOutUri == null) {
                    Log.w("CropActivity", "cannot write file, no output URI given");
                } else {
                    try {
                        this.mOutStream = CropActivity.this.getContentResolver().openOutputStream(this.mOutUri);
                    } catch (FileNotFoundException e) {
                        Log.w("CropActivity", "cannot write file: " + this.mOutUri.toString(), e);
                    }
                }
            }
            if ((flags & 5) != 0) {
                regenerateInputStream();
            }
        }

        @Override
        protected Boolean doInBackground(Bitmap... params) {
            boolean failure = false;
            Bitmap img = params[0];
            if (this.mCrop != null && this.mPhoto != null && this.mOrig != null) {
                RectF trueCrop = CropMath.getScaledCropBounds(this.mCrop, this.mPhoto, this.mOrig);
                Matrix m = new Matrix();
                m.setRotate(this.mRotation);
                m.mapRect(trueCrop);
                if (trueCrop != null) {
                    Rect rounded = new Rect();
                    trueCrop.roundOut(rounded);
                    this.mResultIntent.putExtra("cropped-rect", rounded);
                }
            }
            if ((this.mFlags & 2) != 0) {
                if (!$assertionsDisabled && img == null) {
                    throw new AssertionError();
                }
                Bitmap ret = CropActivity.getCroppedImage(img, this.mCrop, this.mPhoto);
                if (ret != null) {
                    ret = CropActivity.getDownsampledBitmap(ret, 750000);
                }
                if (ret == null) {
                    Log.w("CropActivity", "could not downsample bitmap to return in data");
                    failure = true;
                } else {
                    if (this.mRotation > 0) {
                        Matrix m2 = new Matrix();
                        m2.setRotate(this.mRotation);
                        Bitmap tmp = Bitmap.createBitmap(ret, 0, 0, ret.getWidth(), ret.getHeight(), m2, true);
                        if (tmp != null) {
                            ret = tmp;
                        }
                    }
                    this.mResultIntent.putExtra("data", ret);
                }
            }
            if ((this.mFlags & 5) != 0 && this.mInStream != null) {
                RectF trueCrop2 = CropMath.getScaledCropBounds(this.mCrop, this.mPhoto, this.mOrig);
                if (trueCrop2 == null) {
                    Log.w("CropActivity", "cannot find crop for full size image");
                    return false;
                }
                Rect roundedTrueCrop = new Rect();
                trueCrop2.roundOut(roundedTrueCrop);
                if (roundedTrueCrop.width() <= 0 || roundedTrueCrop.height() <= 0) {
                    Log.w("CropActivity", "crop has bad values for full size image");
                    return false;
                }
                BitmapRegionDecoder decoder = null;
                try {
                    decoder = BitmapRegionDecoder.newInstance(this.mInStream, true);
                } catch (IOException e) {
                    Log.w("CropActivity", "cannot open region decoder for file: " + this.mInUri.toString(), e);
                }
                Bitmap crop = null;
                if (decoder != null) {
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inMutable = true;
                    crop = decoder.decodeRegion(roundedTrueCrop, options);
                    decoder.recycle();
                }
                if (crop == null) {
                    regenerateInputStream();
                    Bitmap fullSize = null;
                    if (this.mInStream != null) {
                        fullSize = BitmapFactory.decodeStream(this.mInStream);
                    }
                    if (fullSize != null) {
                        crop = Bitmap.createBitmap(fullSize, roundedTrueCrop.left, roundedTrueCrop.top, roundedTrueCrop.width(), roundedTrueCrop.height());
                    }
                }
                if (crop != null) {
                    if (CropActivity.this.mOutputX > 0 && CropActivity.this.mOutputY > 0) {
                        Matrix m3 = new Matrix();
                        RectF cropRect = new RectF(0.0f, 0.0f, crop.getWidth(), crop.getHeight());
                        if (this.mRotation > 0) {
                            m3.setRotate(this.mRotation);
                            m3.mapRect(cropRect);
                        }
                        RectF returnRect = new RectF(0.0f, 0.0f, CropActivity.this.mOutputX, CropActivity.this.mOutputY);
                        m3.setRectToRect(cropRect, returnRect, Matrix.ScaleToFit.FILL);
                        m3.preRotate(this.mRotation);
                        Bitmap tmp2 = Bitmap.createBitmap((int) returnRect.width(), (int) returnRect.height(), Bitmap.Config.ARGB_8888);
                        if (tmp2 != null) {
                            Canvas c = new Canvas(tmp2);
                            c.drawBitmap(crop, m3, new Paint());
                            crop = tmp2;
                        }
                    } else if (this.mRotation > 0) {
                        Matrix m4 = new Matrix();
                        m4.setRotate(this.mRotation);
                        Bitmap tmp3 = Bitmap.createBitmap(crop, 0, 0, crop.getWidth(), crop.getHeight(), m4, true);
                        if (tmp3 != null) {
                            crop = tmp3;
                        }
                    }
                    Bitmap.CompressFormat cf = CropActivity.convertExtensionToCompressFormat(CropActivity.getFileExtension(this.mOutputFormat));
                    if (this.mFlags == 4) {
                        if (this.mOutStream == null || !crop.compress(cf, 90, this.mOutStream)) {
                            Log.w("CropActivity", "failed to compress bitmap to file: " + this.mOutUri.toString());
                            failure = true;
                        } else {
                            this.mResultIntent.setData(this.mOutUri);
                        }
                    } else {
                        ByteArrayOutputStream tmpOut = new ByteArrayOutputStream(2048);
                        if (crop.compress(cf, 90, tmpOut)) {
                            if ((this.mFlags & 4) != 0) {
                                if (this.mOutStream == null) {
                                    Log.w("CropActivity", "failed to compress bitmap to file: " + this.mOutUri.toString());
                                    failure = true;
                                } else {
                                    try {
                                        this.mOutStream.write(tmpOut.toByteArray());
                                        this.mResultIntent.setData(this.mOutUri);
                                    } catch (IOException e2) {
                                        Log.w("CropActivity", "failed to compress bitmap to file: " + this.mOutUri.toString(), e2);
                                        failure = true;
                                    }
                                }
                            }
                            if ((this.mFlags & 1) != 0 && this.mWPManager != null) {
                                if (this.mWPManager == null) {
                                    Log.w("CropActivity", "no wallpaper manager");
                                    failure = true;
                                } else {
                                    try {
                                        this.mWPManager.setStream(new ByteArrayInputStream(tmpOut.toByteArray()));
                                    } catch (IOException e3) {
                                        Log.w("CropActivity", "cannot write stream to wallpaper", e3);
                                        failure = true;
                                    }
                                }
                            }
                        } else {
                            Log.w("CropActivity", "cannot compress bitmap");
                            failure = true;
                        }
                    }
                } else {
                    Log.w("CropActivity", "cannot decode file: " + this.mInUri.toString());
                    return false;
                }
            }
            return Boolean.valueOf(!failure);
        }

        @Override
        protected void onPostExecute(Boolean result) {
            Utils.closeSilently(this.mOutStream);
            Utils.closeSilently(this.mInStream);
            CropActivity.this.doneBitmapIO(result.booleanValue(), this.mResultIntent);
        }
    }

    private void done() {
        finish();
    }

    protected static Bitmap getCroppedImage(Bitmap image, RectF cropBounds, RectF photoBounds) {
        RectF imageBounds = new RectF(0.0f, 0.0f, image.getWidth(), image.getHeight());
        RectF crop = CropMath.getScaledCropBounds(cropBounds, photoBounds, imageBounds);
        if (crop == null) {
            return null;
        }
        Rect intCrop = new Rect();
        crop.roundOut(intCrop);
        return Bitmap.createBitmap(image, intCrop.left, intCrop.top, intCrop.width(), intCrop.height());
    }

    protected static Bitmap getDownsampledBitmap(Bitmap image, int max_size) {
        if (image == null || image.getWidth() == 0 || image.getHeight() == 0 || max_size < 16) {
            throw new IllegalArgumentException("Bad argument to getDownsampledBitmap()");
        }
        int shifts = 0;
        for (int size = CropMath.getBitmapSize(image); size > max_size; size /= 4) {
            shifts++;
        }
        Bitmap ret = Bitmap.createScaledBitmap(image, image.getWidth() >> shifts, image.getHeight() >> shifts, true);
        if (ret == null) {
            return null;
        }
        if (CropMath.getBitmapSize(ret) > max_size) {
            return Bitmap.createScaledBitmap(ret, ret.getWidth() >> 1, ret.getHeight() >> 1, true);
        }
        return ret;
    }

    protected static CropExtras getExtrasFromIntent(Intent intent) {
        Bundle extras = intent.getExtras();
        if (extras != null) {
            return new CropExtras(extras.getInt("outputX", 0), extras.getInt("outputY", 0), extras.getBoolean("scale", true) && extras.getBoolean("scaleUpIfNeeded", false), extras.getInt("aspectX", 0), extras.getInt("aspectY", 0), extras.getBoolean("set-as-wallpaper", false), extras.getBoolean("return-data", false), (Uri) extras.getParcelable("output"), extras.getString("outputFormat"), extras.getBoolean("showWhenLocked", false), extras.getFloat("spotlightX"), extras.getFloat("spotlightY"));
        }
        return null;
    }

    protected static Bitmap.CompressFormat convertExtensionToCompressFormat(String extension) {
        return extension.equals("png") ? Bitmap.CompressFormat.PNG : Bitmap.CompressFormat.JPEG;
    }

    protected static String getFileExtension(String requestFormat) {
        String outputFormat = (requestFormat == null ? "jpg" : requestFormat).toLowerCase();
        return (outputFormat.equals("png") || outputFormat.equals("gif")) ? "png" : "jpg";
    }

    private RectF getBitmapCrop(RectF imageBounds) {
        RectF crop = this.mCropView.getCrop();
        RectF photo = this.mCropView.getPhoto();
        if (crop == null || photo == null) {
            Log.w("CropActivity", "could not get crop");
            return null;
        }
        return CropMath.getScaledCropBounds(crop, photo, imageBounds);
    }
}
