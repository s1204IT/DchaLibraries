package com.android.gallery3d.glrenderer;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.os.SystemClock;
import com.android.gallery3d.ui.GLRoot;
import com.mediatek.gallery3d.util.Log;
import java.util.ArrayDeque;
import java.util.ArrayList;

public class TiledTexture implements Texture {
    private static Paint sBitmapPaint;
    private static Canvas sCanvas;
    private static Paint sPaint;
    private static Bitmap sUploadBitmap;
    private final int mHeight;
    private final Tile[] mTiles;
    private final int mWidth;
    public static int CONTENT_SIZE = 254;
    public static int TILE_SIZE = CONTENT_SIZE + 2;
    private static Tile sFreeTileHead = null;
    private static final Object sFreeTileLock = new Object();
    private int mUploadIndex = 0;
    private final RectF mSrcRect = new RectF();
    private final RectF mDestRect = new RectF();
    public boolean mEnableDrawCover = false;

    public static class Uploader implements GLRoot.OnGLIdleListener {
        private final GLRoot mGlRoot;
        private final ArrayDeque<TiledTexture> mTextures = new ArrayDeque<>(8);
        private boolean mIsQueued = false;

        public Uploader(GLRoot gLRoot) {
            this.mGlRoot = gLRoot;
        }

        public synchronized void clear() {
            this.mTextures.clear();
            this.mIsQueued = false;
        }

        public synchronized void addTexture(TiledTexture tiledTexture) {
            if (tiledTexture.isReady()) {
                return;
            }
            this.mTextures.addLast(tiledTexture);
            if (this.mIsQueued) {
                return;
            }
            this.mIsQueued = true;
            this.mGlRoot.addOnGLIdleListener(this);
        }

        @Override
        public boolean onGLIdle(GLCanvas gLCanvas, boolean z) {
            boolean z2;
            ArrayDeque<TiledTexture> arrayDeque = this.mTextures;
            synchronized (this) {
                long jUptimeMillis = SystemClock.uptimeMillis();
                long j = 4 + jUptimeMillis;
                while (jUptimeMillis < j && !arrayDeque.isEmpty()) {
                    if (arrayDeque.peekFirst().uploadNextTile(gLCanvas)) {
                        arrayDeque.removeFirst();
                        this.mGlRoot.requestRender();
                    }
                    jUptimeMillis = SystemClock.uptimeMillis();
                }
                this.mIsQueued = !this.mTextures.isEmpty();
                z2 = this.mIsQueued;
            }
            return z2;
        }
    }

    private static class Tile extends UploadedTexture {
        public Bitmap bitmap;
        public int contentHeight;
        public int contentWidth;
        public Tile nextFreeTile;
        public int offsetX;
        public int offsetY;

        private Tile() {
        }

        @Override
        public void setSize(int i, int i2) {
            this.contentWidth = i;
            this.contentHeight = i2;
            this.mWidth = i + 2;
            this.mHeight = i2 + 2;
            this.mTextureWidth = TiledTexture.TILE_SIZE;
            this.mTextureHeight = TiledTexture.TILE_SIZE;
        }

        @Override
        protected Bitmap onGetBitmap() {
            if (TiledTexture.sCanvas == null) {
                Log.d("Gallery2/TiledTexture", "onGetBitmap(): sCanvas is null");
                return Bitmap.createBitmap(TiledTexture.TILE_SIZE, TiledTexture.TILE_SIZE, Bitmap.Config.ARGB_8888);
            }
            Bitmap bitmap = this.bitmap;
            this.bitmap = null;
            if (bitmap != null) {
                int i = 1 - this.offsetX;
                int i2 = 1 - this.offsetY;
                int width = bitmap.getWidth() + i;
                int height = bitmap.getHeight() + i2;
                TiledTexture.sCanvas.drawBitmap(bitmap, i, i2, TiledTexture.sBitmapPaint);
                if (i > 0) {
                    float f = i - 1;
                    TiledTexture.sCanvas.drawLine(f, 0.0f, f, TiledTexture.TILE_SIZE, TiledTexture.sPaint);
                }
                if (i2 > 0) {
                    float f2 = i2 - 1;
                    TiledTexture.sCanvas.drawLine(0.0f, f2, TiledTexture.TILE_SIZE, f2, TiledTexture.sPaint);
                }
                if (width < TiledTexture.CONTENT_SIZE) {
                    float f3 = width;
                    TiledTexture.sCanvas.drawLine(f3, 0.0f, f3, TiledTexture.TILE_SIZE, TiledTexture.sPaint);
                }
                if (height < TiledTexture.CONTENT_SIZE) {
                    float f4 = height;
                    TiledTexture.sCanvas.drawLine(0.0f, f4, TiledTexture.TILE_SIZE, f4, TiledTexture.sPaint);
                }
            }
            return TiledTexture.sUploadBitmap;
        }

        @Override
        protected void onFreeBitmap(Bitmap bitmap) {
        }
    }

    private static void freeTile(Tile tile) {
        tile.invalidateContent();
        tile.bitmap = null;
        synchronized (sFreeTileLock) {
            tile.nextFreeTile = sFreeTileHead;
            sFreeTileHead = tile;
        }
    }

    private static Tile obtainTile() {
        synchronized (sFreeTileLock) {
            Tile tile = sFreeTileHead;
            if (tile == null) {
                return new Tile();
            }
            sFreeTileHead = tile.nextFreeTile;
            tile.nextFreeTile = null;
            return tile;
        }
    }

    private boolean uploadNextTile(GLCanvas gLCanvas) {
        if (this.mUploadIndex == this.mTiles.length) {
            return true;
        }
        synchronized (this.mTiles) {
            Tile[] tileArr = this.mTiles;
            int i = this.mUploadIndex;
            this.mUploadIndex = i + 1;
            Tile tile = tileArr[i];
            if (tile.bitmap != null) {
                boolean zIsLoaded = tile.isLoaded();
                tile.updateContent(gLCanvas);
                if (!zIsLoaded) {
                    tile.draw(gLCanvas, 0, 0);
                }
            }
        }
        return this.mUploadIndex == this.mTiles.length;
    }

    public TiledTexture(Bitmap bitmap) {
        this.mWidth = bitmap.getWidth();
        this.mHeight = bitmap.getHeight();
        ArrayList arrayList = new ArrayList();
        int i = this.mWidth;
        for (int i2 = 0; i2 < i; i2 += CONTENT_SIZE) {
            int i3 = this.mHeight;
            for (int i4 = 0; i4 < i3; i4 += CONTENT_SIZE) {
                Tile tileObtainTile = obtainTile();
                tileObtainTile.offsetX = i2;
                tileObtainTile.offsetY = i4;
                tileObtainTile.bitmap = bitmap;
                tileObtainTile.setSize(Math.min(CONTENT_SIZE, this.mWidth - i2), Math.min(CONTENT_SIZE, this.mHeight - i4));
                arrayList.add(tileObtainTile);
            }
        }
        this.mTiles = (Tile[]) arrayList.toArray(new Tile[arrayList.size()]);
    }

    public boolean isReady() {
        return this.mUploadIndex == this.mTiles.length;
    }

    public void recycle() {
        synchronized (this.mTiles) {
            int length = this.mTiles.length;
            for (int i = 0; i < length; i++) {
                freeTile(this.mTiles[i]);
            }
        }
    }

    public static void freeResources() {
        sUploadBitmap = null;
        sCanvas = null;
        sBitmapPaint = null;
        sPaint = null;
    }

    public static void prepareResources() {
        sUploadBitmap = Bitmap.createBitmap(TILE_SIZE, TILE_SIZE, Bitmap.Config.ARGB_8888);
        sCanvas = new Canvas(sUploadBitmap);
        sBitmapPaint = new Paint(2);
        sBitmapPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC));
        sPaint = new Paint();
        sPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC));
        sPaint.setColor(0);
    }

    private static void mapRect(RectF rectF, RectF rectF2, float f, float f2, float f3, float f4, float f5, float f6) {
        rectF.set(((rectF2.left - f) * f5) + f3, ((rectF2.top - f2) * f6) + f4, f3 + ((rectF2.right - f) * f5), f4 + ((rectF2.bottom - f2) * f6));
    }

    public void drawMixed(GLCanvas gLCanvas, int i, float f, int i2, int i3, int i4, int i5) {
        RectF rectF = this.mSrcRect;
        RectF rectF2 = this.mDestRect;
        float f2 = i4 / this.mWidth;
        float f3 = i5 / this.mHeight;
        synchronized (this.mTiles) {
            int length = this.mTiles.length;
            int i6 = 0;
            while (i6 < length) {
                Tile tile = this.mTiles[i6];
                rectF.set(0.0f, 0.0f, tile.contentWidth, tile.contentHeight);
                rectF.offset(tile.offsetX, tile.offsetY);
                mapRect(rectF2, rectF, 0.0f, 0.0f, i2, i3, f2, f3);
                rectF.offset(1 - tile.offsetX, 1 - tile.offsetY);
                gLCanvas.drawMixed(tile, i, f, this.mSrcRect, this.mDestRect);
                i6++;
                rectF2 = rectF2;
            }
        }
    }

    @Override
    public void draw(GLCanvas gLCanvas, int i, int i2, int i3, int i4) throws Throwable {
        Tile[] tileArr;
        int i5;
        int i6;
        RectF rectF;
        RectF rectF2;
        RectF rectF3 = this.mSrcRect;
        RectF rectF4 = this.mDestRect;
        float f = i3 / this.mWidth;
        float f2 = i4 / this.mHeight;
        Tile[] tileArr2 = this.mTiles;
        synchronized (tileArr2) {
            try {
                int length = this.mTiles.length;
                int i7 = 0;
                while (i7 < length) {
                    Tile tile = this.mTiles[i7];
                    rectF3.set(0.0f, 0.0f, tile.contentWidth, tile.contentHeight);
                    rectF3.offset(tile.offsetX, tile.offsetY);
                    mapRect(rectF4, rectF3, 0.0f, 0.0f, i, i2, f, f2);
                    rectF3.offset(1 - tile.offsetX, 1 - tile.offsetY);
                    gLCanvas.drawTexture(tile, this.mSrcRect, this.mDestRect);
                    if (!this.mEnableDrawCover) {
                        i5 = i7;
                        i6 = length;
                        tileArr = tileArr2;
                        rectF = rectF4;
                        rectF2 = rectF3;
                    } else {
                        float alpha = gLCanvas.getAlpha();
                        gLCanvas.setAlpha(0.3f);
                        i5 = i7;
                        i6 = length;
                        tileArr = tileArr2;
                        rectF = rectF4;
                        rectF2 = rectF3;
                        try {
                            gLCanvas.fillRect(this.mDestRect.left, this.mDestRect.top, this.mDestRect.width(), this.mDestRect.height(), -16776961);
                            gLCanvas.setAlpha(alpha);
                        } catch (Throwable th) {
                            th = th;
                            throw th;
                        }
                    }
                    i7 = i5 + 1;
                    length = i6;
                    tileArr2 = tileArr;
                    rectF4 = rectF;
                    rectF3 = rectF2;
                }
            } catch (Throwable th2) {
                th = th2;
                tileArr = tileArr2;
            }
        }
    }

    public void draw(GLCanvas gLCanvas, RectF rectF, RectF rectF2) throws Throwable {
        Tile[] tileArr;
        int i;
        int i2;
        TiledTexture tiledTexture = this;
        RectF rectF3 = tiledTexture.mSrcRect;
        RectF rectF4 = tiledTexture.mDestRect;
        float f = rectF.left;
        float f2 = rectF.top;
        float f3 = rectF2.left;
        float f4 = rectF2.top;
        float fWidth = rectF2.width() / rectF.width();
        float fHeight = rectF2.height() / rectF.height();
        Tile[] tileArr2 = tiledTexture.mTiles;
        synchronized (tileArr2) {
            try {
                int length = tiledTexture.mTiles.length;
                int i3 = 0;
                while (i3 < length) {
                    Tile tile = tiledTexture.mTiles[i3];
                    rectF3.set(0.0f, 0.0f, tile.contentWidth, tile.contentHeight);
                    rectF3.offset(tile.offsetX, tile.offsetY);
                    if (rectF3.intersect(rectF)) {
                        i = i3;
                        i2 = length;
                        tileArr = tileArr2;
                        try {
                            mapRect(rectF4, rectF3, f, f2, f3, f4, fWidth, fHeight);
                            rectF3.offset(1 - tile.offsetX, 1 - tile.offsetY);
                            gLCanvas.drawTexture(tile, rectF3, rectF4);
                        } catch (Throwable th) {
                            th = th;
                            throw th;
                        }
                    } else {
                        i = i3;
                        i2 = length;
                        tileArr = tileArr2;
                    }
                    i3 = i + 1;
                    length = i2;
                    tileArr2 = tileArr;
                    tiledTexture = this;
                }
            } catch (Throwable th2) {
                th = th2;
                tileArr = tileArr2;
            }
        }
    }

    @Override
    public int getWidth() {
        return this.mWidth;
    }

    @Override
    public int getHeight() {
        return this.mHeight;
    }

    @Override
    public void draw(GLCanvas gLCanvas, int i, int i2) throws Throwable {
        draw(gLCanvas, i, i2, this.mWidth, this.mHeight);
    }

    @Override
    public boolean isOpaque() {
        return false;
    }
}
