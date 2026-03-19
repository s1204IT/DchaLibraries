package com.android.photos;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Build;
import android.util.Log;
import com.android.gallery3d.common.BitmapUtils;
import com.android.gallery3d.glrenderer.BasicTexture;
import com.android.gallery3d.glrenderer.BitmapTexture;
import com.android.photos.views.TiledImageRenderer;
import com.mediatek.plugin.preload.SoOperater;
import java.io.IOException;

@TargetApi(15)
public class BitmapRegionTileSource implements TiledImageRenderer.TileSource {
    private static final boolean REUSE_BITMAP;
    private Canvas mCanvas;
    BitmapRegionDecoder mDecoder;
    int mHeight;
    private BitmapFactory.Options mOptions;
    private BasicTexture mPreview;
    private final int mRotation;
    int mTileSize;
    int mWidth;
    private Rect mWantRegion = new Rect();
    private Rect mOverlapRegion = new Rect();

    static {
        REUSE_BITMAP = Build.VERSION.SDK_INT >= 16;
    }

    public BitmapRegionTileSource(Context context, String str, int i, int i2) {
        this.mTileSize = TiledImageRenderer.suggestedTileSize(context);
        this.mRotation = i2;
        try {
            this.mDecoder = BitmapRegionDecoder.newInstance(str, true);
            this.mWidth = this.mDecoder.getWidth();
            this.mHeight = this.mDecoder.getHeight();
        } catch (IOException e) {
            Log.w("BitmapRegionTileSource", "ctor failed", e);
        }
        this.mOptions = new BitmapFactory.Options();
        this.mOptions.inPreferredConfig = Bitmap.Config.ARGB_8888;
        this.mOptions.inPreferQualityOverSpeed = true;
        this.mOptions.inTempStorage = new byte[16384];
        if (i != 0) {
            Bitmap bitmapDecodePreview = decodePreview(str, Math.min(i, SoOperater.STEP));
            if (bitmapDecodePreview.getWidth() <= 2048 && bitmapDecodePreview.getHeight() <= 2048) {
                this.mPreview = new BitmapTexture(bitmapDecodePreview);
            } else {
                Log.w("BitmapRegionTileSource", String.format("Failed to create preview of apropriate size!  in: %dx%d, out: %dx%d", Integer.valueOf(this.mWidth), Integer.valueOf(this.mHeight), Integer.valueOf(bitmapDecodePreview.getWidth()), Integer.valueOf(bitmapDecodePreview.getHeight())));
            }
        }
    }

    @Override
    public int getTileSize() {
        return this.mTileSize;
    }

    @Override
    public int getImageWidth() {
        return this.mWidth;
    }

    @Override
    public int getImageHeight() {
        return this.mHeight;
    }

    @Override
    public BasicTexture getPreview() {
        return this.mPreview;
    }

    @Override
    public int getRotation() {
        return this.mRotation;
    }

    @Override
    public Bitmap getTile(int i, int i2, int i3, Bitmap bitmap) {
        int tileSize = getTileSize();
        if (!REUSE_BITMAP) {
            return getTileWithoutReusingBitmap(i, i2, i3, tileSize);
        }
        int i4 = tileSize << i;
        this.mWantRegion.set(i2, i3, i2 + i4, i4 + i3);
        if (bitmap == null) {
            bitmap = Bitmap.createBitmap(tileSize, tileSize, Bitmap.Config.ARGB_8888);
        }
        this.mOptions.inSampleSize = 1 << i;
        this.mOptions.inBitmap = bitmap;
        try {
            Bitmap bitmapDecodeRegion = this.mDecoder.decodeRegion(this.mWantRegion, this.mOptions);
            if (this.mOptions.inBitmap != bitmapDecodeRegion && this.mOptions.inBitmap != null) {
                this.mOptions.inBitmap = null;
            }
            if (bitmapDecodeRegion == null) {
                Log.w("BitmapRegionTileSource", "fail in decoding region");
            }
            return bitmapDecodeRegion;
        } catch (Throwable th) {
            if (this.mOptions.inBitmap != bitmap && this.mOptions.inBitmap != null) {
                this.mOptions.inBitmap = null;
            }
            throw th;
        }
    }

    private Bitmap getTileWithoutReusingBitmap(int i, int i2, int i3, int i4) {
        int i5 = i4 << i;
        this.mWantRegion.set(i2, i3, i2 + i5, i5 + i3);
        this.mOverlapRegion.set(0, 0, this.mWidth, this.mHeight);
        this.mOptions.inSampleSize = 1 << i;
        Bitmap bitmapDecodeRegion = this.mDecoder.decodeRegion(this.mOverlapRegion, this.mOptions);
        if (bitmapDecodeRegion == null) {
            Log.w("BitmapRegionTileSource", "fail in decoding region");
        }
        if (this.mWantRegion.equals(this.mOverlapRegion)) {
            return bitmapDecodeRegion;
        }
        Bitmap bitmapCreateBitmap = Bitmap.createBitmap(i4, i4, Bitmap.Config.ARGB_8888);
        if (this.mCanvas == null) {
            this.mCanvas = new Canvas();
        }
        this.mCanvas.setBitmap(bitmapCreateBitmap);
        this.mCanvas.drawBitmap(bitmapDecodeRegion, (this.mOverlapRegion.left - this.mWantRegion.left) >> i, (this.mOverlapRegion.top - this.mWantRegion.top) >> i, (Paint) null);
        this.mCanvas.setBitmap(null);
        return bitmapCreateBitmap;
    }

    private Bitmap decodePreview(String str, int i) {
        float f = i;
        this.mOptions.inSampleSize = BitmapUtils.computeSampleSizeLarger(f / Math.max(this.mWidth, this.mHeight));
        this.mOptions.inJustDecodeBounds = false;
        Bitmap bitmapDecodeFile = BitmapFactory.decodeFile(str, this.mOptions);
        if (bitmapDecodeFile == null) {
            return null;
        }
        float fMax = f / Math.max(bitmapDecodeFile.getWidth(), bitmapDecodeFile.getHeight());
        if (fMax <= 0.5d) {
            bitmapDecodeFile = BitmapUtils.resizeBitmapByScale(bitmapDecodeFile, fMax, true);
        }
        return ensureGLCompatibleBitmap(bitmapDecodeFile);
    }

    private static Bitmap ensureGLCompatibleBitmap(Bitmap bitmap) {
        if (bitmap == null || bitmap.getConfig() != null) {
            return bitmap;
        }
        Bitmap bitmapCopy = bitmap.copy(Bitmap.Config.ARGB_8888, false);
        bitmap.recycle();
        return bitmapCopy;
    }
}
