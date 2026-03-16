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

    public BitmapRegionTileSource(Context context, String path, int previewSize, int rotation) {
        this.mTileSize = TiledImageRenderer.suggestedTileSize(context);
        this.mRotation = rotation;
        try {
            this.mDecoder = BitmapRegionDecoder.newInstance(path, true);
            this.mWidth = this.mDecoder.getWidth();
            this.mHeight = this.mDecoder.getHeight();
        } catch (IOException e) {
            Log.w("BitmapRegionTileSource", "ctor failed", e);
        }
        this.mOptions = new BitmapFactory.Options();
        this.mOptions.inPreferredConfig = Bitmap.Config.ARGB_8888;
        this.mOptions.inPreferQualityOverSpeed = true;
        this.mOptions.inTempStorage = new byte[16384];
        if (previewSize != 0) {
            Bitmap preview = decodePreview(path, Math.min(previewSize, 1024));
            if (preview.getWidth() <= 2048 && preview.getHeight() <= 2048) {
                this.mPreview = new BitmapTexture(preview);
            } else {
                Log.w("BitmapRegionTileSource", String.format("Failed to create preview of apropriate size!  in: %dx%d, out: %dx%d", Integer.valueOf(this.mWidth), Integer.valueOf(this.mHeight), Integer.valueOf(preview.getWidth()), Integer.valueOf(preview.getHeight())));
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
    public Bitmap getTile(int level, int x, int y, Bitmap bitmap) {
        int tileSize = getTileSize();
        if (!REUSE_BITMAP) {
            return getTileWithoutReusingBitmap(level, x, y, tileSize);
        }
        int t = tileSize << level;
        this.mWantRegion.set(x, y, x + t, y + t);
        if (bitmap == null) {
            bitmap = Bitmap.createBitmap(tileSize, tileSize, Bitmap.Config.ARGB_8888);
        }
        this.mOptions.inSampleSize = 1 << level;
        this.mOptions.inBitmap = bitmap;
        try {
            bitmap = this.mDecoder.decodeRegion(this.mWantRegion, this.mOptions);
            if (bitmap == null) {
                Log.w("BitmapRegionTileSource", "fail in decoding region");
            }
            return bitmap;
        } finally {
            if (this.mOptions.inBitmap != bitmap && this.mOptions.inBitmap != null) {
                this.mOptions.inBitmap = null;
            }
        }
    }

    private Bitmap getTileWithoutReusingBitmap(int level, int x, int y, int tileSize) {
        int t = tileSize << level;
        this.mWantRegion.set(x, y, x + t, y + t);
        this.mOverlapRegion.set(0, 0, this.mWidth, this.mHeight);
        this.mOptions.inSampleSize = 1 << level;
        Bitmap bitmap = this.mDecoder.decodeRegion(this.mOverlapRegion, this.mOptions);
        if (bitmap == null) {
            Log.w("BitmapRegionTileSource", "fail in decoding region");
        }
        if (!this.mWantRegion.equals(this.mOverlapRegion)) {
            Bitmap result = Bitmap.createBitmap(tileSize, tileSize, Bitmap.Config.ARGB_8888);
            if (this.mCanvas == null) {
                this.mCanvas = new Canvas();
            }
            this.mCanvas.setBitmap(result);
            this.mCanvas.drawBitmap(bitmap, (this.mOverlapRegion.left - this.mWantRegion.left) >> level, (this.mOverlapRegion.top - this.mWantRegion.top) >> level, (Paint) null);
            this.mCanvas.setBitmap(null);
            return result;
        }
        return bitmap;
    }

    private Bitmap decodePreview(String file, int targetSize) {
        this.mOptions.inSampleSize = BitmapUtils.computeSampleSizeLarger(targetSize / Math.max(this.mWidth, this.mHeight));
        this.mOptions.inJustDecodeBounds = false;
        Bitmap result = BitmapFactory.decodeFile(file, this.mOptions);
        if (result == null) {
            return null;
        }
        float scale = targetSize / Math.max(result.getWidth(), result.getHeight());
        if (scale <= 0.5d) {
            result = BitmapUtils.resizeBitmapByScale(result, scale, true);
        }
        return ensureGLCompatibleBitmap(result);
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
