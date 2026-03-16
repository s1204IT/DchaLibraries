package com.android.photos;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import com.android.gallery3d.common.BitmapUtils;
import com.android.gallery3d.common.Utils;
import com.android.gallery3d.exif.ExifInterface;
import com.android.gallery3d.glrenderer.BasicTexture;
import com.android.gallery3d.glrenderer.BitmapTexture;
import com.android.photos.views.TiledImageRenderer;
import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

@TargetApi(15)
public class BitmapRegionTileSource implements TiledImageRenderer.TileSource {
    private static final boolean REUSE_BITMAP;
    private Canvas mCanvas;
    SimpleBitmapRegionDecoder mDecoder;
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

    public static abstract class BitmapSource {
        private SimpleBitmapRegionDecoder mDecoder;
        private Bitmap mPreview;
        private int mPreviewSize;
        private int mRotation;
        private State mState = State.NOT_LOADED;

        public enum State {
            NOT_LOADED,
            LOADED,
            ERROR_LOADING
        }

        public abstract SimpleBitmapRegionDecoder loadBitmapRegionDecoder();

        public abstract Bitmap loadPreviewBitmap(BitmapFactory.Options options);

        public abstract boolean readExif(ExifInterface exifInterface);

        public BitmapSource(int previewSize) {
            this.mPreviewSize = previewSize;
        }

        public boolean loadInBackground() {
            Integer ori;
            ExifInterface ei = new ExifInterface();
            if (readExif(ei) && (ori = ei.getTagIntValue(ExifInterface.TAG_ORIENTATION)) != null) {
                this.mRotation = ExifInterface.getRotationForOrientationValue(ori.shortValue());
            }
            this.mDecoder = loadBitmapRegionDecoder();
            if (this.mDecoder == null) {
                this.mState = State.ERROR_LOADING;
                return false;
            }
            int width = this.mDecoder.getWidth();
            int height = this.mDecoder.getHeight();
            if (this.mPreviewSize != 0) {
                int previewSize = Math.min(this.mPreviewSize, 1024);
                BitmapFactory.Options opts = new BitmapFactory.Options();
                opts.inPreferredConfig = Bitmap.Config.ARGB_8888;
                opts.inPreferQualityOverSpeed = true;
                float scale = previewSize / Math.max(width, height);
                opts.inSampleSize = BitmapUtils.computeSampleSizeLarger(scale);
                opts.inJustDecodeBounds = false;
                this.mPreview = loadPreviewBitmap(opts);
            }
            this.mState = State.LOADED;
            return true;
        }

        public State getLoadingState() {
            return this.mState;
        }

        public SimpleBitmapRegionDecoder getBitmapRegionDecoder() {
            return this.mDecoder;
        }

        public Bitmap getPreviewBitmap() {
            return this.mPreview;
        }

        public int getPreviewSize() {
            return this.mPreviewSize;
        }

        public int getRotation() {
            return this.mRotation;
        }
    }

    public static class UriBitmapSource extends BitmapSource {
        private Context mContext;
        private Uri mUri;

        public UriBitmapSource(Context context, Uri uri, int previewSize) {
            super(previewSize);
            this.mContext = context;
            this.mUri = uri;
        }

        private InputStream regenerateInputStream() throws FileNotFoundException {
            InputStream is = this.mContext.getContentResolver().openInputStream(this.mUri);
            return new BufferedInputStream(is);
        }

        @Override
        public SimpleBitmapRegionDecoder loadBitmapRegionDecoder() {
            try {
                InputStream is = regenerateInputStream();
                SimpleBitmapRegionDecoder regionDecoder = SimpleBitmapRegionDecoderWrapper.newInstance(is, false);
                Utils.closeSilently(is);
                if (regionDecoder == null) {
                    InputStream is2 = regenerateInputStream();
                    SimpleBitmapRegionDecoder regionDecoder2 = DumbBitmapRegionDecoder.newInstance(is2);
                    Utils.closeSilently(is2);
                    return regionDecoder2;
                }
                return regionDecoder;
            } catch (FileNotFoundException e) {
                Log.e("BitmapRegionTileSource", "Failed to load URI " + this.mUri, e);
                return null;
            }
        }

        @Override
        public Bitmap loadPreviewBitmap(BitmapFactory.Options options) {
            try {
                InputStream is = regenerateInputStream();
                Bitmap b = BitmapFactory.decodeStream(is, null, options);
                Utils.closeSilently(is);
                return b;
            } catch (FileNotFoundException e) {
                Log.e("BitmapRegionTileSource", "Failed to load URI " + this.mUri, e);
                return null;
            }
        }

        @Override
        public boolean readExif(ExifInterface ei) {
            boolean z = false;
            InputStream is = null;
            try {
                is = regenerateInputStream();
                ei.readExif(is);
                Utils.closeSilently(is);
                z = true;
            } catch (FileNotFoundException e) {
                Log.e("BitmapRegionTileSource", "Failed to load URI " + this.mUri, e);
            } catch (IOException e2) {
                Log.e("BitmapRegionTileSource", "Failed to load URI " + this.mUri, e2);
            } catch (NullPointerException e3) {
                Log.e("BitmapRegionTileSource", "Failed to read EXIF for URI " + this.mUri, e3);
            } finally {
                Utils.closeSilently(is);
            }
            return z;
        }
    }

    public BitmapRegionTileSource(Context context, BitmapSource source) {
        this.mTileSize = TiledImageRenderer.suggestedTileSize(context);
        this.mRotation = source.getRotation();
        this.mDecoder = source.getBitmapRegionDecoder();
        if (this.mDecoder != null) {
            this.mWidth = this.mDecoder.getWidth();
            this.mHeight = this.mDecoder.getHeight();
            this.mOptions = new BitmapFactory.Options();
            this.mOptions.inPreferredConfig = Bitmap.Config.ARGB_8888;
            this.mOptions.inPreferQualityOverSpeed = true;
            this.mOptions.inTempStorage = new byte[16384];
            int previewSize = source.getPreviewSize();
            if (previewSize != 0) {
                Bitmap preview = decodePreview(source, Math.min(previewSize, 1024));
                if (preview.getWidth() <= 2048 && preview.getHeight() <= 2048) {
                    this.mPreview = new BitmapTexture(preview);
                } else {
                    Log.w("BitmapRegionTileSource", String.format("Failed to create preview of apropriate size!  in: %dx%d, out: %dx%d", Integer.valueOf(this.mWidth), Integer.valueOf(this.mHeight), Integer.valueOf(preview.getWidth()), Integer.valueOf(preview.getHeight())));
                }
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

    private Bitmap decodePreview(BitmapSource source, int targetSize) {
        Bitmap result = source.getPreviewBitmap();
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
