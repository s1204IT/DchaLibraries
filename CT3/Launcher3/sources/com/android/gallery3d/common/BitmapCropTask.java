package com.android.gallery3d.common;

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
import android.util.Log;
import android.widget.Toast;
import com.android.launcher3.NycWallpaperUtils;
import com.android.launcher3.R;
import com.android.launcher3.Utilities;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class BitmapCropTask extends AsyncTask<Integer, Void, Boolean> {
    Context mContext;
    RectF mCropBounds;
    Bitmap mCroppedBitmap;
    String mInFilePath;
    byte[] mInImageBytes;
    int mInResId;
    Uri mInUri;
    boolean mNoCrop;
    OnBitmapCroppedHandler mOnBitmapCroppedHandler;
    OnEndCropHandler mOnEndCropHandler;
    int mOutHeight;
    int mOutWidth;
    Resources mResources;
    int mRotation;
    boolean mSaveCroppedBitmap;
    boolean mSetWallpaper;

    public interface OnBitmapCroppedHandler {
        void onBitmapCropped(byte[] bArr, Rect rect);
    }

    public interface OnEndCropHandler {
        void run(boolean z);
    }

    public BitmapCropTask(byte[] imageBytes, RectF cropBounds, int rotation, int outWidth, int outHeight, boolean setWallpaper, boolean saveCroppedBitmap, OnEndCropHandler onEndCropHandler) {
        this.mInUri = null;
        this.mInResId = 0;
        this.mCropBounds = null;
        this.mInImageBytes = imageBytes;
        init(cropBounds, rotation, outWidth, outHeight, setWallpaper, saveCroppedBitmap, onEndCropHandler);
    }

    public BitmapCropTask(Context c, Uri inUri, RectF cropBounds, int rotation, int outWidth, int outHeight, boolean setWallpaper, boolean saveCroppedBitmap, OnEndCropHandler onEndCropHandler) {
        this.mInUri = null;
        this.mInResId = 0;
        this.mCropBounds = null;
        this.mContext = c;
        this.mInUri = inUri;
        init(cropBounds, rotation, outWidth, outHeight, setWallpaper, saveCroppedBitmap, onEndCropHandler);
    }

    public BitmapCropTask(Context c, Resources res, int inResId, RectF cropBounds, int rotation, int outWidth, int outHeight, boolean setWallpaper, boolean saveCroppedBitmap, OnEndCropHandler onEndCropHandler) {
        this.mInUri = null;
        this.mInResId = 0;
        this.mCropBounds = null;
        this.mContext = c;
        this.mInResId = inResId;
        this.mResources = res;
        init(cropBounds, rotation, outWidth, outHeight, setWallpaper, saveCroppedBitmap, onEndCropHandler);
    }

    private void init(RectF cropBounds, int rotation, int outWidth, int outHeight, boolean setWallpaper, boolean saveCroppedBitmap, OnEndCropHandler onEndCropHandler) {
        this.mCropBounds = cropBounds;
        this.mRotation = rotation;
        this.mOutWidth = outWidth;
        this.mOutHeight = outHeight;
        this.mSetWallpaper = setWallpaper;
        this.mSaveCroppedBitmap = saveCroppedBitmap;
        this.mOnEndCropHandler = onEndCropHandler;
    }

    public void setOnBitmapCropped(OnBitmapCroppedHandler handler) {
        this.mOnBitmapCroppedHandler = handler;
    }

    public void setNoCrop(boolean value) {
        this.mNoCrop = value;
    }

    public void setOnEndRunnable(OnEndCropHandler onEndCropHandler) {
        this.mOnEndCropHandler = onEndCropHandler;
    }

    private InputStream regenerateInputStream() {
        if (this.mInUri == null && this.mInResId == 0 && this.mInFilePath == null && this.mInImageBytes == null) {
            Log.w("BitmapCropTask", "cannot read original file, no input URI, resource ID, or image byte array given");
        } else {
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
                Log.w("BitmapCropTask", "cannot read file: " + this.mInUri.toString(), e);
            } catch (SecurityException e2) {
                Log.w("BitmapCropTask", "security exception: " + this.mInUri.toString(), e2);
            }
        }
        return null;
    }

    public Point getImageBounds() {
        InputStream is = regenerateInputStream();
        if (is != null) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(is, null, options);
            Utils.closeSilently(is);
            if (options.outWidth != 0 && options.outHeight != 0) {
                return new Point(options.outWidth, options.outHeight);
            }
        }
        return null;
    }

    public void setCropBounds(RectF cropBounds) {
        this.mCropBounds = cropBounds;
    }

    public Bitmap getCroppedBitmap() {
        return this.mCroppedBitmap;
    }

    public boolean cropBitmap(int whichWallpaper) {
        InputStream is;
        boolean failure = false;
        if (this.mSetWallpaper && this.mNoCrop) {
            try {
                is = regenerateInputStream();
                setWallpaper(is, null, whichWallpaper);
            } catch (IOException e) {
                Log.w("BitmapCropTask", "cannot write stream to wallpaper", e);
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
                Log.w("BitmapCropTask", "cannot get bounds for image");
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
            Log.w("BitmapCropTask", "crop has bad values for full size image");
            return false;
        }
        if (this.mOutWidth <= 0) {
            Log.w("BitmapCropTask", "mOutWidth is zero, mOutWidth:" + this.mOutWidth);
            this.mOutWidth = 1;
        }
        if (this.mOutHeight <= 0) {
            Log.w("BitmapCropTask", "mOutHeight is zero, mOutHeight:" + this.mOutHeight);
            this.mOutHeight = 1;
        }
        int scaleDownSampleSize = Math.max(1, Math.min(roundedTrueCrop.width() / this.mOutWidth, roundedTrueCrop.height() / this.mOutHeight));
        BitmapRegionDecoder decoder = null;
        try {
            try {
                is = regenerateInputStream();
            } catch (IOException e2) {
                Log.w("BitmapCropTask", "cannot open region decoder for file: " + this.mInUri.toString(), e2);
                Utils.closeSilently(null);
            }
            if (is == null) {
                Log.w("BitmapCropTask", "cannot get input stream for uri=" + this.mInUri.toString());
                Utils.closeSilently(is);
                return false;
            }
            decoder = BitmapRegionDecoder.newInstance(is, false);
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
                is = regenerateInputStream();
                Bitmap fullSize = null;
                if (is != null) {
                    BitmapFactory.Options options2 = new BitmapFactory.Options();
                    if (scaleDownSampleSize > 1) {
                        options2.inSampleSize = scaleDownSampleSize;
                    }
                    try {
                        fullSize = BitmapFactory.decodeStream(is, null, options2);
                    } catch (OutOfMemoryError e3) {
                        Log.e("BitmapCropTask", "Failed to decodeStreamI " + is, e3);
                        return false;
                    } finally {
                        Utils.closeSilently(is);
                    }
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
                        roundedTrueCrop.offset(-(roundedTrueCrop.right - fullSize.getWidth()), 0);
                    }
                    if (roundedTrueCrop.height() > fullSize.getHeight()) {
                        roundedTrueCrop.bottom = roundedTrueCrop.top + fullSize.getHeight();
                    }
                    if (roundedTrueCrop.bottom > fullSize.getHeight()) {
                        roundedTrueCrop.offset(0, -(roundedTrueCrop.bottom - fullSize.getHeight()));
                    }
                    if (roundedTrueCrop.width() <= 0 || roundedTrueCrop.height() <= 0) {
                        Log.w("BitmapCropTask", "crop has bad values for full size image");
                        return false;
                    }
                    try {
                        crop = Bitmap.createBitmap(fullSize, roundedTrueCrop.left, roundedTrueCrop.top, roundedTrueCrop.width(), roundedTrueCrop.height());
                    } catch (OutOfMemoryError e4) {
                        Log.w("BitmapCropTask", "Wallpaper too large, createBitmap fail");
                    }
                }
            }
            if (crop == null) {
                Log.w("BitmapCropTask", "cannot decode file: " + this.mInUri.toString());
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
            ByteArrayOutputStream tmpOut = new ByteArrayOutputStream(2048);
            if (!crop.compress(Bitmap.CompressFormat.JPEG, 90, tmpOut)) {
                Log.w("BitmapCropTask", "cannot compress bitmap");
                failure = true;
            } else if (this.mSetWallpaper) {
                try {
                    byte[] outByteArray = tmpOut.toByteArray();
                    setWallpaper(new ByteArrayInputStream(outByteArray), null, whichWallpaper);
                    if (this.mOnBitmapCroppedHandler != null) {
                        this.mOnBitmapCroppedHandler.onBitmapCropped(outByteArray, new Rect(0, 0, crop.getWidth(), crop.getHeight()));
                    }
                } catch (IOException e5) {
                    Log.w("BitmapCropTask", "cannot write stream to wallpaper", e5);
                    failure = true;
                }
            }
            return !failure;
        } catch (Throwable th) {
            Utils.closeSilently(null);
            throw th;
        }
    }

    @Override
    protected Boolean doInBackground(Integer... params) {
        return Boolean.valueOf(cropBitmap(params.length == 0 ? 1 : params[0].intValue()));
    }

    @Override
    protected void onPostExecute(Boolean cropSucceeded) {
        if (!cropSucceeded.booleanValue()) {
            Toast.makeText(this.mContext, R.string.wallpaper_set_fail, 0).show();
        }
        if (this.mOnEndCropHandler == null) {
            return;
        }
        this.mOnEndCropHandler.run(cropSucceeded.booleanValue());
    }

    private void setWallpaper(InputStream in, Rect crop, int whichWallpaper) throws IOException {
        if (!Utilities.ATLEAST_N) {
            WallpaperManager.getInstance(this.mContext.getApplicationContext()).setStream(in);
        } else {
            NycWallpaperUtils.setStream(this.mContext, in, crop, true, whichWallpaper);
        }
    }
}
