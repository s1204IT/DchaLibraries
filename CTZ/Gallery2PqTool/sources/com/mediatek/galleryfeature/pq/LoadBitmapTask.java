package com.mediatek.galleryfeature.pq;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import com.mediatek.gallerybasic.util.Log;

public class LoadBitmapTask extends AsyncTask<Void, Void, Bitmap> {
    private static final String TAG = "MtkGallery2/LoadPQBitmapTask";
    private static Context sContext;
    private static String sPQMineType;
    private static String sPQUri;
    private static Bitmap sScreenNailBitmap;
    private static Bitmap sTileBitmap;
    private String mCurrentUri;
    private TiledBitmapDecoder mDecoderTiledBitmap;
    private boolean mIsFristDecode;
    private int mRotation = 0;
    private BitmapDecoder mScreenNailDecoder;

    public static void init(Context context) {
        sContext = context;
        Bundle extras = ((Activity) context).getIntent().getExtras();
        if (extras != null) {
            sPQMineType = extras.getString("PQMineType");
            sPQUri = extras.getString("PQUri");
        }
        Log.d(TAG, " <init>sPQUri=" + sPQUri);
    }

    public LoadBitmapTask(String str) {
        this.mCurrentUri = str;
    }

    @Override
    protected Bitmap doInBackground(Void... voidArr) {
        this.mIsFristDecode = sScreenNailBitmap == null && sTileBitmap == null;
        this.mRotation = PQUtils.getRotation(sContext, sPQUri);
        if (sPQMineType != null && sPQUri != null) {
            if (this.mIsFristDecode || !PQUtils.isSupportedByRegionDecoder(sPQMineType)) {
                this.mScreenNailDecoder = new BitmapDecoder(sContext, sPQUri, PictureQualityActivity.sTargetWidth);
                sScreenNailBitmap = this.mScreenNailDecoder.decodeScreenNailBitmap();
                if (sScreenNailBitmap != null) {
                    Log.d(TAG, "<doInBackground> sScreenNailBitmap=" + sScreenNailBitmap.getWidth() + " " + sScreenNailBitmap.getHeight());
                }
                return sScreenNailBitmap;
            }
            this.mDecoderTiledBitmap = new TiledBitmapDecoder(sContext, sPQUri);
            if (this.mDecoderTiledBitmap != null) {
                sTileBitmap = this.mDecoderTiledBitmap.decodeBitmap();
            }
            return sTileBitmap;
        }
        return null;
    }

    @Override
    protected void onPostExecute(Bitmap bitmap) {
        if (bitmap != null) {
            if (this.mRotation != 0) {
                bitmap = PQUtils.rotateBitmap(bitmap, this.mRotation, true);
            }
            PresentImage.getPresentImage().setBitmap(bitmap, this.mCurrentUri);
        }
    }

    public static boolean needRegionDecode() {
        return PQUtils.isSupportedByRegionDecoder(sPQMineType) && sTileBitmap == null;
    }

    public void free() {
        if (sScreenNailBitmap != null) {
            sScreenNailBitmap.recycle();
            sScreenNailBitmap = null;
        }
        if (sTileBitmap != null) {
            sTileBitmap.recycle();
            sTileBitmap = null;
        }
        if (this.mScreenNailDecoder != null) {
            this.mScreenNailDecoder = null;
        }
        if (this.mDecoderTiledBitmap != null) {
            this.mDecoderTiledBitmap.recycle();
            this.mDecoderTiledBitmap = null;
        }
    }
}
