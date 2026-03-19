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

        public BitmapSource(int i) {
            this.mPreviewSize = i;
        }

        public boolean loadInBackground() {
            Integer tagIntValue;
            ExifInterface exifInterface = new ExifInterface();
            if (readExif(exifInterface) && (tagIntValue = exifInterface.getTagIntValue(ExifInterface.TAG_ORIENTATION)) != null) {
                this.mRotation = ExifInterface.getRotationForOrientationValue(tagIntValue.shortValue());
            }
            this.mDecoder = loadBitmapRegionDecoder();
            if (this.mDecoder == null) {
                this.mState = State.ERROR_LOADING;
                return false;
            }
            int width = this.mDecoder.getWidth();
            int height = this.mDecoder.getHeight();
            if (this.mPreviewSize != 0) {
                int iMin = Math.min(this.mPreviewSize, 1024);
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inPreferredConfig = Bitmap.Config.ARGB_8888;
                options.inPreferQualityOverSpeed = true;
                options.inSampleSize = BitmapUtils.computeSampleSizeLarger(iMin / Math.max(width, height));
                options.inJustDecodeBounds = false;
                this.mPreview = loadPreviewBitmap(options);
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

        public UriBitmapSource(Context context, Uri uri, int i) {
            super(i);
            this.mContext = context;
            this.mUri = uri;
        }

        private InputStream regenerateInputStream() throws FileNotFoundException {
            return new BufferedInputStream(this.mContext.getContentResolver().openInputStream(this.mUri));
        }

        @Override
        public SimpleBitmapRegionDecoder loadBitmapRegionDecoder() {
            try {
                InputStream inputStreamRegenerateInputStream = regenerateInputStream();
                SimpleBitmapRegionDecoderWrapper simpleBitmapRegionDecoderWrapperNewInstance = SimpleBitmapRegionDecoderWrapper.newInstance(inputStreamRegenerateInputStream, false);
                Utils.closeSilently(inputStreamRegenerateInputStream);
                if (simpleBitmapRegionDecoderWrapperNewInstance == null) {
                    InputStream inputStreamRegenerateInputStream2 = regenerateInputStream();
                    DumbBitmapRegionDecoder dumbBitmapRegionDecoderNewInstance = DumbBitmapRegionDecoder.newInstance(inputStreamRegenerateInputStream2);
                    Utils.closeSilently(inputStreamRegenerateInputStream2);
                    return dumbBitmapRegionDecoderNewInstance;
                }
                return simpleBitmapRegionDecoderWrapperNewInstance;
            } catch (FileNotFoundException e) {
                Log.e("BitmapRegionTileSource", "Failed to load URI " + this.mUri, e);
                return null;
            }
        }

        @Override
        public Bitmap loadPreviewBitmap(BitmapFactory.Options options) {
            try {
                InputStream inputStreamRegenerateInputStream = regenerateInputStream();
                Bitmap bitmapDecodeStream = BitmapFactory.decodeStream(inputStreamRegenerateInputStream, null, options);
                Utils.closeSilently(inputStreamRegenerateInputStream);
                return bitmapDecodeStream;
            } catch (FileNotFoundException e) {
                Log.e("BitmapRegionTileSource", "Failed to load URI " + this.mUri, e);
                return null;
            }
        }

        @Override
        public boolean readExif(ExifInterface exifInterface) throws Throwable {
            InputStream inputStreamRegenerateInputStream;
            InputStream inputStream = null;
            try {
                try {
                    inputStreamRegenerateInputStream = regenerateInputStream();
                } catch (Throwable th) {
                    th = th;
                }
            } catch (FileNotFoundException e) {
                e = e;
            } catch (IOException e2) {
                e = e2;
            } catch (NullPointerException e3) {
                e = e3;
            }
            try {
                exifInterface.readExif(inputStreamRegenerateInputStream);
                Utils.closeSilently(inputStreamRegenerateInputStream);
                Utils.closeSilently(inputStreamRegenerateInputStream);
                return true;
            } catch (FileNotFoundException e4) {
                e = e4;
                inputStream = inputStreamRegenerateInputStream;
                Log.e("BitmapRegionTileSource", "Failed to load URI " + this.mUri, e);
                Utils.closeSilently(inputStream);
                return false;
            } catch (IOException e5) {
                e = e5;
                inputStream = inputStreamRegenerateInputStream;
                Log.e("BitmapRegionTileSource", "Failed to load URI " + this.mUri, e);
                Utils.closeSilently(inputStream);
                return false;
            } catch (NullPointerException e6) {
                e = e6;
                inputStream = inputStreamRegenerateInputStream;
                Log.e("BitmapRegionTileSource", "Failed to read EXIF for URI " + this.mUri, e);
                Utils.closeSilently(inputStream);
                return false;
            } catch (Throwable th2) {
                th = th2;
                inputStream = inputStreamRegenerateInputStream;
                Utils.closeSilently(inputStream);
                throw th;
            }
        }
    }

    public BitmapRegionTileSource(Context context, BitmapSource bitmapSource) {
        this.mTileSize = TiledImageRenderer.suggestedTileSize(context);
        this.mRotation = bitmapSource.getRotation();
        this.mDecoder = bitmapSource.getBitmapRegionDecoder();
        if (this.mDecoder != null) {
            this.mWidth = this.mDecoder.getWidth();
            this.mHeight = this.mDecoder.getHeight();
            this.mOptions = new BitmapFactory.Options();
            this.mOptions.inPreferredConfig = Bitmap.Config.ARGB_8888;
            this.mOptions.inPreferQualityOverSpeed = true;
            this.mOptions.inTempStorage = new byte[16384];
            int previewSize = bitmapSource.getPreviewSize();
            if (previewSize != 0) {
                Bitmap bitmapDecodePreview = decodePreview(bitmapSource, Math.min(previewSize, 1024));
                if (bitmapDecodePreview.getWidth() <= 2048 && bitmapDecodePreview.getHeight() <= 2048) {
                    this.mPreview = new BitmapTexture(bitmapDecodePreview);
                } else {
                    Log.w("BitmapRegionTileSource", String.format("Failed to create preview of apropriate size!  in: %dx%d, out: %dx%d", Integer.valueOf(this.mWidth), Integer.valueOf(this.mHeight), Integer.valueOf(bitmapDecodePreview.getWidth()), Integer.valueOf(bitmapDecodePreview.getHeight())));
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

    private Bitmap decodePreview(BitmapSource bitmapSource, int i) {
        Bitmap previewBitmap = bitmapSource.getPreviewBitmap();
        if (previewBitmap == null) {
            return null;
        }
        float fMax = i / Math.max(previewBitmap.getWidth(), previewBitmap.getHeight());
        if (fMax <= 0.5d) {
            previewBitmap = BitmapUtils.resizeBitmapByScale(previewBitmap, fMax, true);
        }
        return ensureGLCompatibleBitmap(previewBitmap);
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
