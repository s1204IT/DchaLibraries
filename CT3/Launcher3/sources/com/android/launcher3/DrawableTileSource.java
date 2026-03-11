package com.android.launcher3;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import com.android.gallery3d.glrenderer.BasicTexture;
import com.android.gallery3d.glrenderer.BitmapTexture;
import com.android.photos.views.TiledImageRenderer;

public class DrawableTileSource implements TiledImageRenderer.TileSource {
    private Drawable mDrawable;
    private BitmapTexture mPreview;
    private int mPreviewSize;
    private int mTileSize;

    public DrawableTileSource(Context context, Drawable d, int previewSize) {
        this.mTileSize = TiledImageRenderer.suggestedTileSize(context);
        this.mDrawable = d;
        this.mPreviewSize = Math.min(previewSize, 1024);
    }

    @Override
    public int getTileSize() {
        return this.mTileSize;
    }

    @Override
    public int getImageWidth() {
        return this.mDrawable.getIntrinsicWidth();
    }

    @Override
    public int getImageHeight() {
        return this.mDrawable.getIntrinsicHeight();
    }

    @Override
    public int getRotation() {
        return 0;
    }

    @Override
    public BasicTexture getPreview() {
        if (this.mPreviewSize == 0) {
            return null;
        }
        if (this.mPreview == null) {
            float width = getImageWidth();
            float height = getImageHeight();
            while (true) {
                if (width <= 1024.0f && height <= 1024.0f) {
                    break;
                }
                width /= 2.0f;
                height /= 2.0f;
            }
            Bitmap b = Bitmap.createBitmap((int) width, (int) height, Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(b);
            this.mDrawable.setBounds(new Rect(0, 0, (int) width, (int) height));
            this.mDrawable.draw(c);
            c.setBitmap(null);
            this.mPreview = new BitmapTexture(b);
        }
        return this.mPreview;
    }

    @Override
    public Bitmap getTile(int level, int x, int y, Bitmap bitmap) {
        int tileSize = getTileSize();
        if (bitmap == null) {
            bitmap = Bitmap.createBitmap(tileSize, tileSize, Bitmap.Config.ARGB_8888);
        }
        Canvas c = new Canvas(bitmap);
        Rect bounds = new Rect(0, 0, getImageWidth(), getImageHeight());
        bounds.offset(-x, -y);
        this.mDrawable.setBounds(bounds);
        this.mDrawable.draw(c);
        c.setBitmap(null);
        return bitmap;
    }
}
