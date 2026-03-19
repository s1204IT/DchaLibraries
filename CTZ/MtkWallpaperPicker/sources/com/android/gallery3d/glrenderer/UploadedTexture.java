package com.android.gallery3d.glrenderer;

import android.graphics.Bitmap;
import android.opengl.GLUtils;
import android.util.Pair;
import com.android.gallery3d.common.Utils;
import java.util.HashMap;

public abstract class UploadedTexture extends BasicTexture {
    private static HashMap<BorderKey, Bitmap> sBorderLines = new HashMap<>();
    protected Bitmap mBitmap;
    private boolean mContentValid;

    protected abstract void onFreeBitmap(Bitmap bitmap);

    protected abstract Bitmap onGetBitmap();

    private static class BorderKey extends Pair<Bitmap.Config, Integer> {
        public BorderKey(Bitmap.Config config, boolean z, int i) {
            super(config, Integer.valueOf(z ? i : -i));
        }
    }

    protected UploadedTexture() {
        super(null, 0, 0);
        this.mContentValid = true;
    }

    private static Bitmap getBorderLine(boolean z, Bitmap.Config config, int i) {
        Bitmap bitmapCreateBitmap;
        BorderKey borderKey = new BorderKey(config, z, i);
        Bitmap bitmap = sBorderLines.get(borderKey);
        if (bitmap == null) {
            if (z) {
                bitmapCreateBitmap = Bitmap.createBitmap(1, i, config);
            } else {
                bitmapCreateBitmap = Bitmap.createBitmap(i, 1, config);
            }
            bitmap = bitmapCreateBitmap;
            sBorderLines.put(borderKey, bitmap);
        }
        return bitmap;
    }

    private Bitmap getBitmap() {
        if (this.mBitmap == null) {
            this.mBitmap = onGetBitmap();
            int width = this.mBitmap.getWidth();
            int height = this.mBitmap.getHeight();
            if (this.mWidth == -1) {
                setSize(width, height);
            }
        }
        return this.mBitmap;
    }

    private void freeBitmap() {
        Utils.assertTrue(this.mBitmap != null);
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

    public void updateContent(GLCanvas gLCanvas) {
        if (!isLoaded()) {
            uploadToCanvas(gLCanvas);
        } else if (!this.mContentValid) {
            Bitmap bitmap = getBitmap();
            gLCanvas.texSubImage2D(this, 0, 0, bitmap, GLUtils.getInternalFormat(bitmap), GLUtils.getType(bitmap));
            freeBitmap();
            this.mContentValid = true;
        }
    }

    private void uploadToCanvas(GLCanvas gLCanvas) {
        Bitmap bitmap = getBitmap();
        if (bitmap != null) {
            try {
                int width = bitmap.getWidth();
                int height = bitmap.getHeight();
                int textureWidth = getTextureWidth();
                int textureHeight = getTextureHeight();
                Utils.assertTrue(width <= textureWidth && height <= textureHeight);
                this.mId = gLCanvas.getGLId().generateTexture();
                gLCanvas.setTextureParameters(this);
                if (width == textureWidth && height == textureHeight) {
                    gLCanvas.initializeTexture(this, bitmap);
                } else {
                    int internalFormat = GLUtils.getInternalFormat(bitmap);
                    int type = GLUtils.getType(bitmap);
                    Bitmap.Config config = bitmap.getConfig();
                    gLCanvas.initializeTextureSize(this, internalFormat, type);
                    gLCanvas.texSubImage2D(this, 0, 0, bitmap, internalFormat, type);
                    if (width < textureWidth) {
                        gLCanvas.texSubImage2D(this, width, 0, getBorderLine(true, config, textureHeight), internalFormat, type);
                    }
                    if (height < textureHeight) {
                        gLCanvas.texSubImage2D(this, 0, height, getBorderLine(false, config, textureWidth), internalFormat, type);
                    }
                }
                freeBitmap();
                setAssociatedCanvas(gLCanvas);
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
    protected boolean onBind(GLCanvas gLCanvas) {
        updateContent(gLCanvas);
        return isContentValid();
    }

    @Override
    public void recycle() {
        super.recycle();
        if (this.mBitmap != null) {
            freeBitmap();
        }
    }
}
