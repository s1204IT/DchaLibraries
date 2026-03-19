package com.android.wallpaperpicker.common;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.RectF;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;
import com.android.wallpaperpicker.R;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class CropAndSetWallpaperTask extends AsyncTask<Integer, Void, Boolean> {
    private final Context mContext;
    private final RectF mCropBounds;
    private OnBitmapCroppedHandler mOnBitmapCroppedHandler;
    private OnEndCropHandler mOnEndCropHandler;
    private int mOutHeight;
    private int mOutWidth;
    private int mRotation;
    private final InputStreamProvider mStreamProvider;

    public interface OnBitmapCroppedHandler {
        void onBitmapCropped(byte[] bArr);
    }

    public interface OnEndCropHandler {
        void run(boolean z);
    }

    public CropAndSetWallpaperTask(InputStreamProvider inputStreamProvider, Context context, RectF rectF, int i, int i2, int i3, OnEndCropHandler onEndCropHandler) {
        this.mStreamProvider = inputStreamProvider;
        this.mContext = context;
        this.mCropBounds = rectF;
        this.mRotation = i;
        this.mOutWidth = i2;
        this.mOutHeight = i3;
        this.mOnEndCropHandler = onEndCropHandler;
    }

    public void setOnBitmapCropped(OnBitmapCroppedHandler onBitmapCroppedHandler) {
        this.mOnBitmapCroppedHandler = onBitmapCroppedHandler;
    }

    public boolean cropBitmap(int i) throws Throwable {
        Bitmap croppedBitmap = this.mStreamProvider.readCroppedBitmap(this.mCropBounds, this.mOutWidth, this.mOutHeight, this.mRotation);
        boolean z = false;
        if (croppedBitmap == null) {
            return false;
        }
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(2048);
        if (croppedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, byteArrayOutputStream)) {
            try {
                byte[] byteArray = byteArrayOutputStream.toByteArray();
                WallpaperManagerCompat.getInstance(this.mContext).setStream(new ByteArrayInputStream(byteArray), null, true, i);
                if (this.mOnBitmapCroppedHandler != null) {
                    this.mOnBitmapCroppedHandler.onBitmapCropped(byteArray);
                }
            } catch (IOException e) {
                Log.w("CropAndSetWallpaperTask", "cannot write stream to wallpaper", e);
                z = true;
            }
            return !z;
        }
        Log.w("CropAndSetWallpaperTask", "cannot compress bitmap");
        z = true;
        return !z;
    }

    @Override
    protected Boolean doInBackground(Integer... numArr) {
        return Boolean.valueOf(cropBitmap(numArr[0].intValue()));
    }

    @Override
    protected void onPostExecute(Boolean bool) {
        if (!bool.booleanValue()) {
            Toast.makeText(this.mContext, R.string.wallpaper_set_fail, 0).show();
        }
        if (this.mOnEndCropHandler != null) {
            this.mOnEndCropHandler.run(bool.booleanValue());
        }
    }
}
