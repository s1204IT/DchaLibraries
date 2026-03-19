package com.mediatek.gallerybasic.gl;

import android.graphics.Bitmap;
import android.opengl.GLUtils;
import java.util.HashMap;

public abstract class MUploadedTexture extends MBasicTexture {
    private static final String TAG = "MtkGallery2/MUploadedTexture";
    private static final int UPLOAD_LIMIT = 100;
    private static int sUploadedCount;
    protected Bitmap mBitmap;
    private int mBorder;
    private boolean mContentValid;
    private boolean mIsUploading;
    private boolean mOpaque;
    private boolean mThrottled;
    private static HashMap<BorderKey, Bitmap> sBorderLines = new HashMap<>();
    private static BorderKey sBorderKey = new BorderKey();

    protected abstract void onFreeBitmap(Bitmap bitmap);

    protected abstract Bitmap onGetBitmap();

    protected MUploadedTexture() {
        this(false);
    }

    protected MUploadedTexture(boolean z) {
        super(null, 0, 0);
        this.mContentValid = true;
        this.mIsUploading = false;
        this.mOpaque = true;
        this.mThrottled = false;
        if (z) {
            setBorder(true);
            this.mBorder = 1;
        }
    }

    protected void setIsUploading(boolean z) {
        this.mIsUploading = z;
    }

    public boolean isUploading() {
        return this.mIsUploading;
    }

    private static class BorderKey implements Cloneable {
        public Bitmap.Config config;
        public int length;
        public boolean vertical;

        private BorderKey() {
        }

        public int hashCode() {
            int iHashCode = this.config.hashCode() ^ this.length;
            return this.vertical ? iHashCode : -iHashCode;
        }

        public boolean equals(Object obj) {
            if (!(obj instanceof BorderKey)) {
                return false;
            }
            BorderKey borderKey = (BorderKey) obj;
            return this.vertical == borderKey.vertical && this.config == borderKey.config && this.length == borderKey.length;
        }

        public BorderKey m6clone() {
            try {
                return (BorderKey) super.clone();
            } catch (CloneNotSupportedException e) {
                throw new AssertionError(e);
            }
        }
    }

    protected void setThrottled(boolean z) {
        this.mThrottled = z;
    }

    private static Bitmap getBorderLine(boolean z, Bitmap.Config config, int i) {
        Bitmap bitmapCreateBitmap;
        BorderKey borderKey = sBorderKey;
        borderKey.vertical = z;
        borderKey.config = config;
        borderKey.length = i;
        Bitmap bitmap = sBorderLines.get(borderKey);
        if (bitmap == null) {
            if (z) {
                bitmapCreateBitmap = Bitmap.createBitmap(1, i, config);
            } else {
                bitmapCreateBitmap = Bitmap.createBitmap(i, 1, config);
            }
            bitmap = bitmapCreateBitmap;
            sBorderLines.put(borderKey.m6clone(), bitmap);
        }
        return bitmap;
    }

    private Bitmap getBitmap() {
        if (this.mBitmap == null) {
            this.mBitmap = onGetBitmap();
            int width = this.mBitmap.getWidth() + (this.mBorder * 2);
            int height = this.mBitmap.getHeight() + (this.mBorder * 2);
            if (this.mWidth == -1) {
                setSize(width, height);
            }
        }
        return this.mBitmap;
    }

    private void freeBitmap() {
        com.android.gallery3d.common.Utils.assertTrue(this.mBitmap != null);
        onFreeBitmap(this.mBitmap);
        this.mBitmap = null;
    }

    @Override
    public int getWidth() {
        if (this.mWidth == -1) {
            getBitmap();
        }
        return this.mWidth;
    }

    @Override
    public int getHeight() {
        if (this.mWidth == -1) {
            getBitmap();
        }
        return this.mHeight;
    }

    protected void invalidateContent() {
        if (this.mBitmap != null) {
            freeBitmap();
        }
        this.mContentValid = false;
        this.mWidth = -1;
        this.mHeight = -1;
    }

    public boolean isContentValid() {
        return isLoaded() && this.mContentValid;
    }

    public void updateContent(MGLCanvas mGLCanvas) {
        if (!isLoaded()) {
            if (this.mThrottled) {
                int i = sUploadedCount + 1;
                sUploadedCount = i;
                if (i > UPLOAD_LIMIT) {
                    return;
                }
            }
            uploadToCanvas(mGLCanvas);
            return;
        }
        if (!this.mContentValid) {
            Bitmap bitmap = getBitmap();
            mGLCanvas.texSubImage2D(this, this.mBorder, this.mBorder, bitmap, GLUtils.getInternalFormat(bitmap), GLUtils.getType(bitmap));
            freeBitmap();
            this.mContentValid = true;
        }
    }

    public static void resetUploadLimit() {
        sUploadedCount = 0;
    }

    public static boolean uploadLimitReached() {
        return sUploadedCount > UPLOAD_LIMIT;
    }

    private void uploadToCanvas(MGLCanvas mGLCanvas) {
        Bitmap bitmap = getBitmap();
        if (bitmap != null) {
            try {
                int width = bitmap.getWidth();
                int height = bitmap.getHeight();
                int i = this.mBorder;
                int i2 = this.mBorder;
                int textureWidth = getTextureWidth();
                int textureHeight = getTextureHeight();
                com.android.gallery3d.common.Utils.assertTrue(width <= textureWidth && height <= textureHeight);
                this.mId = mGLCanvas.generateTexture();
                mGLCanvas.setTextureParameters(this);
                if (width == textureWidth && height == textureHeight) {
                    mGLCanvas.initializeTexture(this, bitmap);
                } else {
                    int internalFormat = GLUtils.getInternalFormat(bitmap);
                    int type = GLUtils.getType(bitmap);
                    Bitmap.Config config = bitmap.getConfig();
                    mGLCanvas.initializeTextureSize(this, internalFormat, type);
                    mGLCanvas.texSubImage2D(this, this.mBorder, this.mBorder, bitmap, internalFormat, type);
                    if (this.mBorder > 0) {
                        mGLCanvas.texSubImage2D(this, 0, 0, getBorderLine(true, config, textureHeight), internalFormat, type);
                        mGLCanvas.texSubImage2D(this, 0, 0, getBorderLine(false, config, textureWidth), internalFormat, type);
                    }
                    if (this.mBorder + width < textureWidth) {
                        mGLCanvas.texSubImage2D(this, this.mBorder + width, 0, getBorderLine(true, config, textureHeight), internalFormat, type);
                    }
                    if (this.mBorder + height < textureHeight) {
                        mGLCanvas.texSubImage2D(this, 0, this.mBorder + height, getBorderLine(false, config, textureWidth), internalFormat, type);
                    }
                }
                freeBitmap();
                setAssociatedCanvas(mGLCanvas);
                this.mState = 1;
                this.mContentValid = true;
                return;
            } catch (Throwable th) {
                freeBitmap();
                throw th;
            }
        }
        this.mState = -1;
        throw new RuntimeException("Texture load fail, no bitmap");
    }

    @Override
    protected boolean onBind(MGLCanvas mGLCanvas) {
        updateContent(mGLCanvas);
        return isContentValid();
    }

    @Override
    protected int getTarget() {
        return 3553;
    }

    public void setOpaque(boolean z) {
        this.mOpaque = z;
    }

    @Override
    public boolean isOpaque() {
        return this.mOpaque;
    }

    @Override
    public void recycle() {
        super.recycle();
        if (this.mBitmap != null) {
            freeBitmap();
        }
    }
}
