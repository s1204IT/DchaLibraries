package com.android.gallery3d.glrenderer;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;
import com.android.gallery3d.ui.GLRoot;
import java.util.ArrayDeque;
import java.util.ArrayList;

public class TiledTexture implements Texture {
    private static Paint sBitmapPaint;
    private static Canvas sCanvas;
    private static Tile sFreeTileHead = null;
    private static final Object sFreeTileLock = new Object();
    private static Paint sPaint;
    private static Bitmap sUploadBitmap;
    private final int mHeight;
    private final Tile[] mTiles;
    private final int mWidth;
    private int mUploadIndex = 0;
    private final RectF mSrcRect = new RectF();
    private final RectF mDestRect = new RectF();

    public static class Uploader implements GLRoot.OnGLIdleListener {
        private final GLRoot mGlRoot;
        private final ArrayDeque<TiledTexture> mTextures = new ArrayDeque<>(8);
        private boolean mIsQueued = false;

        public Uploader(GLRoot glRoot) {
            this.mGlRoot = glRoot;
        }

        public synchronized void clear() {
            this.mTextures.clear();
        }

        public synchronized void addTexture(TiledTexture t) {
            if (!t.isReady()) {
                this.mTextures.addLast(t);
                if (!this.mIsQueued) {
                    this.mIsQueued = true;
                    this.mGlRoot.addOnGLIdleListener(this);
                }
            }
        }

        @Override
        public boolean onGLIdle(GLCanvas canvas, boolean renderRequested) {
            boolean z;
            ArrayDeque<TiledTexture> deque = this.mTextures;
            synchronized (this) {
                long now = SystemClock.uptimeMillis();
                long dueTime = now + 4;
                while (now < dueTime && !deque.isEmpty()) {
                    TiledTexture t = deque.peekFirst();
                    if (t.uploadNextTile(canvas)) {
                        deque.removeFirst();
                        this.mGlRoot.requestRender();
                    }
                    now = SystemClock.uptimeMillis();
                }
                this.mIsQueued = !this.mTextures.isEmpty();
                z = this.mIsQueued;
            }
            return z;
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
        public void setSize(int width, int height) {
            this.contentWidth = width;
            this.contentHeight = height;
            this.mWidth = width + 2;
            this.mHeight = height + 2;
            this.mTextureWidth = NotificationCompat.FLAG_LOCAL_ONLY;
            this.mTextureHeight = NotificationCompat.FLAG_LOCAL_ONLY;
        }

        @Override
        protected Bitmap onGetBitmap() {
            Bitmap localBitmapRef = this.bitmap;
            this.bitmap = null;
            if (localBitmapRef != null) {
                int x = 1 - this.offsetX;
                int y = 1 - this.offsetY;
                int r = localBitmapRef.getWidth() + x;
                int b = localBitmapRef.getHeight() + y;
                TiledTexture.sCanvas.drawBitmap(localBitmapRef, x, y, TiledTexture.sBitmapPaint);
                if (x > 0) {
                    TiledTexture.sCanvas.drawLine(x - 1, 0.0f, x - 1, 256.0f, TiledTexture.sPaint);
                }
                if (y > 0) {
                    TiledTexture.sCanvas.drawLine(0.0f, y - 1, 256.0f, y - 1, TiledTexture.sPaint);
                }
                if (r < 254) {
                    TiledTexture.sCanvas.drawLine(r, 0.0f, r, 256.0f, TiledTexture.sPaint);
                }
                if (b < 254) {
                    TiledTexture.sCanvas.drawLine(0.0f, b, 256.0f, b, TiledTexture.sPaint);
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
        Tile result;
        synchronized (sFreeTileLock) {
            result = sFreeTileHead;
            if (result == null) {
                result = new Tile();
            } else {
                sFreeTileHead = result.nextFreeTile;
                result.nextFreeTile = null;
            }
        }
        return result;
    }

    private boolean uploadNextTile(GLCanvas canvas) {
        if (this.mUploadIndex == this.mTiles.length) {
            return true;
        }
        synchronized (this.mTiles) {
            Tile[] tileArr = this.mTiles;
            int i = this.mUploadIndex;
            this.mUploadIndex = i + 1;
            Tile next = tileArr[i];
            if (next.bitmap != null) {
                boolean hasBeenLoad = next.isLoaded();
                next.updateContent(canvas);
                if (!hasBeenLoad) {
                    next.draw(canvas, 0, 0);
                }
            }
        }
        return this.mUploadIndex == this.mTiles.length;
    }

    public TiledTexture(Bitmap bitmap) {
        this.mWidth = bitmap.getWidth();
        this.mHeight = bitmap.getHeight();
        ArrayList<Tile> list = new ArrayList<>();
        int w = this.mWidth;
        for (int x = 0; x < w; x += 254) {
            int h = this.mHeight;
            for (int y = 0; y < h; y += 254) {
                Tile tile = obtainTile();
                tile.offsetX = x;
                tile.offsetY = y;
                tile.bitmap = bitmap;
                tile.setSize(Math.min(254, this.mWidth - x), Math.min(254, this.mHeight - y));
                list.add(tile);
            }
        }
        this.mTiles = (Tile[]) list.toArray(new Tile[list.size()]);
    }

    public boolean isReady() {
        return this.mUploadIndex == this.mTiles.length;
    }

    public void recycle() {
        synchronized (this.mTiles) {
            int n = this.mTiles.length;
            for (int i = 0; i < n; i++) {
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
        sUploadBitmap = Bitmap.createBitmap(NotificationCompat.FLAG_LOCAL_ONLY, NotificationCompat.FLAG_LOCAL_ONLY, Bitmap.Config.ARGB_8888);
        sCanvas = new Canvas(sUploadBitmap);
        sBitmapPaint = new Paint(2);
        sBitmapPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC));
        sPaint = new Paint();
        sPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC));
        sPaint.setColor(0);
    }

    private static void mapRect(RectF output, RectF src, float x0, float y0, float x, float y, float scaleX, float scaleY) {
        output.set(((src.left - x0) * scaleX) + x, ((src.top - y0) * scaleY) + y, ((src.right - x0) * scaleX) + x, ((src.bottom - y0) * scaleY) + y);
    }

    public void drawMixed(GLCanvas canvas, int color, float ratio, int x, int y, int width, int height) {
        RectF src = this.mSrcRect;
        RectF dest = this.mDestRect;
        float scaleX = width / this.mWidth;
        float scaleY = height / this.mHeight;
        synchronized (this.mTiles) {
            int n = this.mTiles.length;
            for (int i = 0; i < n; i++) {
                Tile t = this.mTiles[i];
                src.set(0.0f, 0.0f, t.contentWidth, t.contentHeight);
                src.offset(t.offsetX, t.offsetY);
                mapRect(dest, src, 0.0f, 0.0f, x, y, scaleX, scaleY);
                src.offset(1 - t.offsetX, 1 - t.offsetY);
                canvas.drawMixed(t, color, ratio, this.mSrcRect, this.mDestRect);
            }
        }
    }

    @Override
    public void draw(GLCanvas canvas, int x, int y, int width, int height) {
        RectF src = this.mSrcRect;
        RectF dest = this.mDestRect;
        float scaleX = width / this.mWidth;
        float scaleY = height / this.mHeight;
        synchronized (this.mTiles) {
            int n = this.mTiles.length;
            for (int i = 0; i < n; i++) {
                Tile t = this.mTiles[i];
                src.set(0.0f, 0.0f, t.contentWidth, t.contentHeight);
                src.offset(t.offsetX, t.offsetY);
                mapRect(dest, src, 0.0f, 0.0f, x, y, scaleX, scaleY);
                src.offset(1 - t.offsetX, 1 - t.offsetY);
                canvas.drawTexture(t, this.mSrcRect, this.mDestRect);
            }
        }
    }

    public void draw(GLCanvas canvas, RectF source, RectF target) {
        RectF src = this.mSrcRect;
        RectF dest = this.mDestRect;
        float x0 = source.left;
        float y0 = source.top;
        float x = target.left;
        float y = target.top;
        float scaleX = target.width() / source.width();
        float scaleY = target.height() / source.height();
        synchronized (this.mTiles) {
            int n = this.mTiles.length;
            for (int i = 0; i < n; i++) {
                Tile t = this.mTiles[i];
                src.set(0.0f, 0.0f, t.contentWidth, t.contentHeight);
                src.offset(t.offsetX, t.offsetY);
                if (src.intersect(source)) {
                    mapRect(dest, src, x0, y0, x, y, scaleX, scaleY);
                    src.offset(1 - t.offsetX, 1 - t.offsetY);
                    canvas.drawTexture(t, src, dest);
                }
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
    public void draw(GLCanvas canvas, int x, int y) {
        draw(canvas, x, y, this.mWidth, this.mHeight);
    }

    @Override
    public boolean isOpaque() {
        return false;
    }
}
