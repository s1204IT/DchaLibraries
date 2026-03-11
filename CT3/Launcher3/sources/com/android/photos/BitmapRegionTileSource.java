package com.android.photos;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.net.Uri;
import android.opengl.GLUtils;
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

        public abstract SimpleBitmapRegionDecoder loadBitmapRegionDecoder();

        public abstract Bitmap loadPreviewBitmap(BitmapFactory.Options options);

        public abstract boolean readExif(ExifInterface exifInterface);

        public enum State {
            NOT_LOADED,
            LOADED,
            ERROR_LOADING;

            public static State[] valuesCustom() {
                return values();
            }
        }

        public boolean loadInBackground(InBitmapProvider bitmapProvider) {
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
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inPreferredConfig = Bitmap.Config.ARGB_8888;
            opts.inPreferQualityOverSpeed = true;
            float scale = 1024.0f / Math.max(width, height);
            opts.inSampleSize = BitmapUtils.computeSampleSizeLarger(scale);
            opts.inJustDecodeBounds = false;
            opts.inMutable = true;
            if (bitmapProvider != null) {
                int expectedPixles = (width / opts.inSampleSize) * (height / opts.inSampleSize);
                Bitmap reusableBitmap = bitmapProvider.forPixelCount(expectedPixles);
                if (reusableBitmap != null) {
                    opts.inBitmap = reusableBitmap;
                    try {
                        this.mPreview = loadPreviewBitmap(opts);
                    } catch (IllegalArgumentException e) {
                        Log.d("BitmapRegionTileSource", "Unable to reuse bitmap", e);
                        opts.inBitmap = null;
                        this.mPreview = null;
                    }
                }
            }
            if (this.mPreview == null) {
                this.mPreview = loadPreviewBitmap(opts);
            }
            if (this.mPreview == null) {
                this.mState = State.ERROR_LOADING;
                return false;
            }
            if (this.mPreview != null) {
                this.mPreview = decodePreview(this.mPreview, 1024);
            }
            try {
                GLUtils.getInternalFormat(this.mPreview);
                GLUtils.getType(this.mPreview);
                this.mState = State.LOADED;
            } catch (IllegalArgumentException e2) {
                Log.d("BitmapRegionTileSource", "Image cannot be rendered on a GL surface", e2);
                this.mState = State.ERROR_LOADING;
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

        public Bitmap decodePreview(Bitmap bitmap, int targetSize) {
            Bitmap result = bitmap;
            if (bitmap == null) {
                return null;
            }
            float scale = targetSize / Math.max(bitmap.getWidth(), bitmap.getHeight());
            if (scale <= 0.5d) {
                result = BitmapUtils.resizeBitmapByScale(bitmap, scale, true);
            }
            return ensureGLCompatibleBitmap(result);
        }

        private static Bitmap ensureGLCompatibleBitmap(Bitmap bitmap) {
            if (bitmap == null || bitmap.getConfig() != null) {
                return bitmap;
            }
            Bitmap newBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, false);
            bitmap.recycle();
            return newBitmap;
        }
    }

    public static class UriBitmapSource extends BitmapSource {
        private Context mContext;
        private Uri mUri;

        public UriBitmapSource(Context context, Uri uri) {
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
            } catch (FileNotFoundException | OutOfMemoryError e) {
                Log.e("BitmapRegionTileSource", "Failed to load URI " + this.mUri, e);
                return null;
            }
        }

        @Override
        public boolean readExif(ExifInterface ei) {
            InputStream is = null;
            try {
                is = regenerateInputStream();
                ei.readExif(is);
                Utils.closeSilently(is);
                return true;
            } catch (NullPointerException e) {
                Log.d("BitmapRegionTileSource", "Failed to read EXIF for URI " + this.mUri, e);
                return false;
            } catch (FileNotFoundException e2) {
                Log.d("BitmapRegionTileSource", "Failed to load URI " + this.mUri, e2);
                return false;
            } catch (IOException e3) {
                Log.d("BitmapRegionTileSource", "Failed to load URI " + this.mUri, e3);
                return false;
            } finally {
                Utils.closeSilently(is);
            }
        }
    }

    public static class ResourceBitmapSource extends BitmapSource {
        private Resources mRes;
        private int mResId;

        public ResourceBitmapSource(Resources res, int resId) {
            this.mRes = res;
            this.mResId = resId;
        }

        private InputStream regenerateInputStream() {
            InputStream is = this.mRes.openRawResource(this.mResId);
            return new BufferedInputStream(is);
        }

        @Override
        public SimpleBitmapRegionDecoder loadBitmapRegionDecoder() {
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
        }

        @Override
        public Bitmap loadPreviewBitmap(BitmapFactory.Options options) {
            return BitmapFactory.decodeResource(this.mRes, this.mResId, options);
        }

        @Override
        public boolean readExif(ExifInterface ei) {
            try {
                InputStream is = regenerateInputStream();
                ei.readExif(is);
                Utils.closeSilently(is);
                return true;
            } catch (IOException e) {
                Log.e("BitmapRegionTileSource", "Error reading resource", e);
                return false;
            }
        }
    }

    public BitmapRegionTileSource(Context context, BitmapSource source, byte[] tempStorage) {
        this.mTileSize = TiledImageRenderer.suggestedTileSize(context);
        this.mRotation = source.getRotation();
        this.mDecoder = source.getBitmapRegionDecoder();
        if (this.mDecoder == null) {
            return;
        }
        this.mWidth = this.mDecoder.getWidth();
        this.mHeight = this.mDecoder.getHeight();
        this.mOptions = new BitmapFactory.Options();
        this.mOptions.inPreferredConfig = Bitmap.Config.ARGB_8888;
        this.mOptions.inPreferQualityOverSpeed = true;
        this.mOptions.inTempStorage = tempStorage;
        Bitmap preview = source.getPreviewBitmap();
        if (preview != null && preview.getWidth() <= 2048 && preview.getHeight() <= 2048) {
            this.mPreview = new BitmapTexture(preview);
            if (this.mWidth == 0) {
                this.mWidth = preview.getWidth();
            }
            if (this.mHeight != 0) {
                return;
            }
            this.mHeight = preview.getHeight();
            return;
        }
        Object[] objArr = new Object[4];
        objArr[0] = Integer.valueOf(this.mWidth);
        objArr[1] = Integer.valueOf(this.mHeight);
        objArr[2] = Integer.valueOf(preview == null ? -1 : preview.getWidth());
        objArr[3] = Integer.valueOf(preview != null ? preview.getHeight() : -1);
        Log.w("BitmapRegionTileSource", String.format("Failed to create preview of apropriate size!  in: %dx%d, out: %dx%d", objArr));
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
    public Bitmap getTile(int level, int x, int y, Bitmap bitmap) {
        int tileSize = getTileSize();
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
}
