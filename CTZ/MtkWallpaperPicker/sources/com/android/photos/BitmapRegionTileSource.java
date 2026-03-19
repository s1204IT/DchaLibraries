package com.android.photos;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.net.Uri;
import android.opengl.GLUtils;
import android.util.Log;
import com.android.gallery3d.common.ExifOrientation;
import com.android.gallery3d.common.Utils;
import com.android.gallery3d.glrenderer.BasicTexture;
import com.android.gallery3d.glrenderer.BitmapTexture;
import com.android.photos.views.TiledImageRenderer;
import com.android.wallpaperpicker.common.InputStreamProvider;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

@TargetApi(15)
public class BitmapRegionTileSource implements TiledImageRenderer.TileSource {
    SimpleBitmapRegionDecoder mDecoder;
    int mHeight;
    private BitmapFactory.Options mOptions;
    private BasicTexture mPreview;
    private final int mRotation;
    int mTileSize;
    private Rect mWantRegion = new Rect();
    int mWidth;

    public static abstract class BitmapSource {
        private SimpleBitmapRegionDecoder mDecoder;
        private Bitmap mPreview;
        private int mRotation;
        private State mState = State.NOT_LOADED;

        public interface InBitmapProvider {
            Bitmap forPixelCount(int i);
        }

        public enum State {
            NOT_LOADED,
            LOADED,
            ERROR_LOADING
        }

        public abstract int getExifRotation();

        public abstract SimpleBitmapRegionDecoder loadBitmapRegionDecoder();

        public abstract Bitmap loadPreviewBitmap(BitmapFactory.Options options);

        public boolean loadInBackground(InBitmapProvider inBitmapProvider) {
            Bitmap bitmapForPixelCount;
            this.mRotation = getExifRotation();
            this.mDecoder = loadBitmapRegionDecoder();
            if (this.mDecoder == null) {
                this.mState = State.ERROR_LOADING;
                return false;
            }
            int width = this.mDecoder.getWidth();
            int height = this.mDecoder.getHeight();
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            options.inPreferQualityOverSpeed = true;
            options.inSampleSize = Utils.computeSampleSizeLarger(1024.0f / Math.max(width, height));
            options.inJustDecodeBounds = false;
            options.inMutable = true;
            if (inBitmapProvider != null && (bitmapForPixelCount = inBitmapProvider.forPixelCount((width / options.inSampleSize) * (height / options.inSampleSize))) != null) {
                options.inBitmap = bitmapForPixelCount;
                try {
                    this.mPreview = loadPreviewBitmap(options);
                } catch (IllegalArgumentException e) {
                    Log.d("BitmapRegionTileSource", "Unable to reuse bitmap", e);
                    options.inBitmap = null;
                    this.mPreview = null;
                }
            }
            if (this.mPreview == null) {
                this.mPreview = loadPreviewBitmap(options);
            }
            if (this.mPreview == null) {
                this.mState = State.ERROR_LOADING;
                return false;
            }
            try {
                GLUtils.getInternalFormat(this.mPreview);
                GLUtils.getType(this.mPreview);
                this.mState = State.LOADED;
            } catch (IllegalArgumentException e2) {
                Log.d("BitmapRegionTileSource", "Image cannot be rendered on a GL surface", e2);
                this.mPreview = decodePreview(this.mPreview, 1024);
                try {
                    GLUtils.getInternalFormat(this.mPreview);
                    GLUtils.getType(this.mPreview);
                    this.mState = State.LOADED;
                } catch (IllegalArgumentException e3) {
                    this.mState = State.ERROR_LOADING;
                }
            }
            return this.mState == State.LOADED;
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

        public int getRotation() {
            return this.mRotation;
        }

        public Bitmap decodePreview(Bitmap bitmap, int i) {
            if (bitmap == null) {
                return null;
            }
            float fMax = i / Math.max(bitmap.getWidth(), bitmap.getHeight());
            if (fMax <= 0.5d) {
                bitmap = resizeBitmapByScale(bitmap, fMax, true);
            }
            return ensureGLCompatibleBitmap(bitmap);
        }

        private static Bitmap resizeBitmapByScale(Bitmap bitmap, float f, boolean z) {
            int iRound = Math.round(bitmap.getWidth() * f);
            int iRound2 = Math.round(bitmap.getHeight() * f);
            if (iRound == bitmap.getWidth() && iRound2 == bitmap.getHeight()) {
                return bitmap;
            }
            Bitmap bitmapCreateBitmap = Bitmap.createBitmap(iRound, iRound2, getConfig(bitmap));
            Canvas canvas = new Canvas(bitmapCreateBitmap);
            canvas.scale(f, f);
            canvas.drawBitmap(bitmap, 0.0f, 0.0f, new Paint(6));
            if (z) {
                bitmap.recycle();
            }
            return bitmapCreateBitmap;
        }

        private static Bitmap.Config getConfig(Bitmap bitmap) {
            Bitmap.Config config = bitmap.getConfig();
            if (config == null) {
                return Bitmap.Config.ARGB_8888;
            }
            return config;
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

    public static class InputStreamSource extends BitmapSource {
        private final Context mContext;
        private final InputStreamProvider mStreamProvider;

        public InputStreamSource(Context context, Uri uri) {
            this(InputStreamProvider.fromUri(context, uri), context);
        }

        public InputStreamSource(Resources resources, int i, Context context) {
            this(InputStreamProvider.fromResource(resources, i), context);
        }

        public InputStreamSource(InputStreamProvider inputStreamProvider, Context context) {
            this.mStreamProvider = inputStreamProvider;
            this.mContext = context;
        }

        @Override
        public SimpleBitmapRegionDecoder loadBitmapRegionDecoder() {
            try {
                InputStream inputStreamNewStreamNotNull = this.mStreamProvider.newStreamNotNull();
                SimpleBitmapRegionDecoderWrapper simpleBitmapRegionDecoderWrapperNewInstance = SimpleBitmapRegionDecoderWrapper.newInstance(inputStreamNewStreamNotNull, false);
                Utils.closeSilently(inputStreamNewStreamNotNull);
                if (simpleBitmapRegionDecoderWrapperNewInstance == null) {
                    InputStream inputStreamNewStreamNotNull2 = this.mStreamProvider.newStreamNotNull();
                    DumbBitmapRegionDecoder dumbBitmapRegionDecoderNewInstance = DumbBitmapRegionDecoder.newInstance(inputStreamNewStreamNotNull2);
                    Utils.closeSilently(inputStreamNewStreamNotNull2);
                    return dumbBitmapRegionDecoderNewInstance;
                }
                return simpleBitmapRegionDecoderWrapperNewInstance;
            } catch (IOException e) {
                Log.e("InputStreamSource", "Failed to load stream", e);
                return null;
            }
        }

        @Override
        public int getExifRotation() {
            return this.mStreamProvider.getRotationFromExif(this.mContext);
        }

        @Override
        public Bitmap loadPreviewBitmap(BitmapFactory.Options options) {
            try {
                InputStream inputStreamNewStreamNotNull = this.mStreamProvider.newStreamNotNull();
                Bitmap bitmapDecodeStream = BitmapFactory.decodeStream(inputStreamNewStreamNotNull, null, options);
                Utils.closeSilently(inputStreamNewStreamNotNull);
                return bitmapDecodeStream;
            } catch (IOException | OutOfMemoryError e) {
                Log.e("InputStreamSource", "Failed to load stream", e);
                return null;
            }
        }
    }

    public static class FilePathBitmapSource extends InputStreamSource {
        private String mPath;

        public FilePathBitmapSource(File file, Context context) {
            super(context, Uri.fromFile(file));
            this.mPath = file.getAbsolutePath();
        }

        @Override
        public int getExifRotation() {
            return ExifOrientation.readRotation(this.mPath);
        }
    }

    public BitmapRegionTileSource(Context context, BitmapSource bitmapSource, byte[] bArr) {
        this.mTileSize = TiledImageRenderer.suggestedTileSize(context);
        this.mRotation = bitmapSource.getRotation();
        this.mDecoder = bitmapSource.getBitmapRegionDecoder();
        if (this.mDecoder != null) {
            this.mWidth = this.mDecoder.getWidth();
            this.mHeight = this.mDecoder.getHeight();
            this.mOptions = new BitmapFactory.Options();
            this.mOptions.inPreferredConfig = Bitmap.Config.ARGB_8888;
            this.mOptions.inPreferQualityOverSpeed = true;
            this.mOptions.inTempStorage = bArr;
            Bitmap previewBitmap = bitmapSource.getPreviewBitmap();
            if (previewBitmap != null && previewBitmap.getWidth() <= 2048 && previewBitmap.getHeight() <= 2048) {
                this.mPreview = new BitmapTexture(previewBitmap);
                return;
            }
            Object[] objArr = new Object[4];
            objArr[0] = Integer.valueOf(this.mWidth);
            objArr[1] = Integer.valueOf(this.mHeight);
            objArr[2] = Integer.valueOf(previewBitmap == null ? -1 : previewBitmap.getWidth());
            objArr[3] = Integer.valueOf(previewBitmap != null ? previewBitmap.getHeight() : -1);
            Log.w("BitmapRegionTileSource", String.format("Failed to create preview of apropriate size!  in: %dx%d, out: %dx%d", objArr));
        }
    }

    public Bitmap getBitmap() {
        if (this.mPreview instanceof BitmapTexture) {
            return ((BitmapTexture) this.mPreview).getBitmap();
        }
        return null;
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
}
