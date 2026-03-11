package com.android.photos.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.RectF;
import android.support.v4.util.Pools$Pool;
import android.support.v4.util.Pools$SimplePool;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.LongSparseArray;
import android.view.View;
import android.view.WindowManager;
import com.android.gallery3d.common.Utils;
import com.android.gallery3d.glrenderer.BasicTexture;
import com.android.gallery3d.glrenderer.GLCanvas;
import com.android.gallery3d.glrenderer.UploadedTexture;

public class TiledImageRenderer {
    static Pools$Pool<Bitmap> sTilePool;
    private boolean mBackgroundTileUploaded;
    protected int mCenterX;
    protected int mCenterY;
    private boolean mLayoutTiles;
    protected int mLevelCount;
    TileSource mModel;
    private int mOffsetX;
    private int mOffsetY;
    private View mParent;
    private BasicTexture mPreview;
    private boolean mRenderComplete;
    protected int mRotation;
    protected float mScale;
    int mTileSize;
    private int mUploadQuota;
    private int mViewHeight;
    private int mViewWidth;
    int mLevel = 0;
    private final RectF mSourceRect = new RectF();
    private final RectF mTargetRect = new RectF();
    private final LongSparseArray<Tile> mActiveTiles = new LongSparseArray<>();
    final Object mQueueLock = new Object();
    private final TileQueue mRecycledQueue = new TileQueue();
    private final TileQueue mUploadQueue = new TileQueue();
    final TileQueue mDecodeQueue = new TileQueue();
    protected int mImageWidth = -1;
    protected int mImageHeight = -1;
    private final Rect mTileRange = new Rect();
    private final Rect[] mActiveRange = {new Rect(), new Rect()};
    private TileDecoder mTileDecoder = new TileDecoder();

    public interface TileSource {
        int getImageHeight();

        int getImageWidth();

        BasicTexture getPreview();

        int getRotation();

        Bitmap getTile(int i, int i2, int i3, Bitmap bitmap);

        int getTileSize();
    }

    static {
        final int i = 64;
        sTilePool = new Pools$SimplePool<T>(i) {
            private final Object mLock = new Object();

            @Override
            public T acquire() {
                T t;
                synchronized (this.mLock) {
                    t = (T) super.acquire();
                }
                return t;
            }

            @Override
            public boolean release(T element) {
                boolean zRelease;
                synchronized (this.mLock) {
                    zRelease = super.release(element);
                }
                return zRelease;
            }
        };
    }

    public static int suggestedTileSize(Context context) {
        return isHighResolution(context) ? 512 : 256;
    }

    private static boolean isHighResolution(Context context) {
        DisplayMetrics metrics = new DisplayMetrics();
        WindowManager wm = (WindowManager) context.getSystemService("window");
        wm.getDefaultDisplay().getMetrics(metrics);
        return metrics.heightPixels > 2048 || metrics.widthPixels > 2048;
    }

    public TiledImageRenderer(View parent) {
        this.mParent = parent;
        this.mTileDecoder.start();
    }

    private void invalidate() {
        this.mParent.postInvalidate();
    }

    public void setModel(TileSource model, int rotation) {
        if (this.mModel != model) {
            this.mModel = model;
            notifyModelInvalidated();
        }
        if (this.mRotation == rotation) {
            return;
        }
        this.mRotation = rotation;
        this.mLayoutTiles = true;
    }

    private void calculateLevelCount() {
        if (this.mPreview != null) {
            this.mLevelCount = Math.max(0, Utils.ceilLog2(this.mImageWidth / this.mPreview.getWidth()));
            return;
        }
        int levels = 1;
        int maxDim = Math.max(this.mImageWidth, this.mImageHeight);
        int t = this.mTileSize;
        while (t < maxDim) {
            t <<= 1;
            levels++;
        }
        this.mLevelCount = levels;
    }

    public void notifyModelInvalidated() {
        invalidateTiles();
        if (this.mModel == null) {
            this.mImageWidth = 0;
            this.mImageHeight = 0;
            this.mLevelCount = 0;
            this.mPreview = null;
        } else {
            this.mImageWidth = this.mModel.getImageWidth();
            this.mImageHeight = this.mModel.getImageHeight();
            this.mPreview = this.mModel.getPreview();
            this.mTileSize = this.mModel.getTileSize();
            calculateLevelCount();
        }
        this.mLayoutTiles = true;
    }

    public void setViewSize(int width, int height) {
        this.mViewWidth = width;
        this.mViewHeight = height;
    }

    public void setPosition(int centerX, int centerY, float scale) {
        if (this.mCenterX == centerX && this.mCenterY == centerY && this.mScale == scale) {
            return;
        }
        this.mCenterX = centerX;
        this.mCenterY = centerY;
        this.mScale = scale;
        this.mLayoutTiles = true;
    }

    private void layoutTiles() {
        int fromLevel;
        if (this.mViewWidth == 0 || this.mViewHeight == 0 || !this.mLayoutTiles) {
            return;
        }
        this.mLayoutTiles = false;
        this.mLevel = Utils.clamp(Utils.floorLog2(1.0f / this.mScale), 0, this.mLevelCount);
        if (this.mLevel != this.mLevelCount) {
            Rect range = this.mTileRange;
            getRange(range, this.mCenterX, this.mCenterY, this.mLevel, this.mScale, this.mRotation);
            this.mOffsetX = Math.round((this.mViewWidth / 2.0f) + ((range.left - this.mCenterX) * this.mScale));
            this.mOffsetY = Math.round((this.mViewHeight / 2.0f) + ((range.top - this.mCenterY) * this.mScale));
            fromLevel = this.mScale * ((float) (1 << this.mLevel)) > 0.75f ? this.mLevel - 1 : this.mLevel;
        } else {
            fromLevel = this.mLevel - 2;
            this.mOffsetX = Math.round((this.mViewWidth / 2.0f) - (this.mCenterX * this.mScale));
            this.mOffsetY = Math.round((this.mViewHeight / 2.0f) - (this.mCenterY * this.mScale));
        }
        int fromLevel2 = Math.max(0, Math.min(fromLevel, this.mLevelCount - 2));
        int endLevel = Math.min(fromLevel2 + 2, this.mLevelCount);
        Rect[] range2 = this.mActiveRange;
        for (int i = fromLevel2; i < endLevel; i++) {
            getRange(range2[i - fromLevel2], this.mCenterX, this.mCenterY, i, this.mRotation);
        }
        if (this.mRotation % 90 != 0) {
            return;
        }
        synchronized (this.mQueueLock) {
            this.mDecodeQueue.clean();
            this.mUploadQueue.clean();
            this.mBackgroundTileUploaded = false;
            int n = this.mActiveTiles.size();
            int i2 = 0;
            while (i2 < n) {
                Tile tile = this.mActiveTiles.valueAt(i2);
                int level = tile.mTileLevel;
                if (level < fromLevel2 || level >= endLevel || !range2[level - fromLevel2].contains(tile.mX, tile.mY)) {
                    this.mActiveTiles.removeAt(i2);
                    i2--;
                    n--;
                    recycleTile(tile);
                }
                i2++;
            }
        }
        for (int i3 = fromLevel2; i3 < endLevel; i3++) {
            int size = this.mTileSize << i3;
            Rect r = range2[i3 - fromLevel2];
            int bottom = r.bottom;
            for (int y = r.top; y < bottom; y += size) {
                int right = r.right;
                for (int x = r.left; x < right; x += size) {
                    activateTile(x, y, i3);
                }
            }
        }
        invalidate();
    }

    private void invalidateTiles() {
        synchronized (this.mQueueLock) {
            this.mDecodeQueue.clean();
            this.mUploadQueue.clean();
            int n = this.mActiveTiles.size();
            for (int i = 0; i < n; i++) {
                Tile tile = this.mActiveTiles.valueAt(i);
                recycleTile(tile);
            }
            this.mActiveTiles.clear();
        }
    }

    private void getRange(Rect out, int cX, int cY, int level, int rotation) {
        getRange(out, cX, cY, level, 1.0f / (1 << (level + 1)), rotation);
    }

    private void getRange(Rect out, int cX, int cY, int level, float scale, int rotation) {
        double radians = Math.toRadians(-rotation);
        double w = this.mViewWidth;
        double h = this.mViewHeight;
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);
        int width = (int) Math.ceil(Math.max(Math.abs((cos * w) - (sin * h)), Math.abs((cos * w) + (sin * h))));
        int height = (int) Math.ceil(Math.max(Math.abs((sin * w) + (cos * h)), Math.abs((sin * w) - (cos * h))));
        int left = (int) Math.floor(cX - (width / (2.0f * scale)));
        int top = (int) Math.floor(cY - (height / (2.0f * scale)));
        int right = (int) Math.ceil(left + (width / scale));
        int bottom = (int) Math.ceil(top + (height / scale));
        int size = this.mTileSize << level;
        out.set(Math.max(0, (left / size) * size), Math.max(0, (top / size) * size), Math.min(this.mImageWidth, right), Math.min(this.mImageHeight, bottom));
    }

    public void freeTextures() {
        this.mLayoutTiles = true;
        this.mTileDecoder.finishAndWait();
        synchronized (this.mQueueLock) {
            this.mUploadQueue.clean();
            this.mDecodeQueue.clean();
            Tile tile = this.mRecycledQueue.pop();
            while (tile != null) {
                tile.recycle();
                tile = this.mRecycledQueue.pop();
            }
        }
        int n = this.mActiveTiles.size();
        for (int i = 0; i < n; i++) {
            Tile texture = this.mActiveTiles.valueAt(i);
            texture.recycle();
        }
        this.mActiveTiles.clear();
        this.mTileRange.set(0, 0, 0, 0);
        while (sTilePool.acquire() != null) {
        }
    }

    public boolean draw(GLCanvas canvas) {
        layoutTiles();
        uploadTiles(canvas);
        this.mUploadQuota = 1;
        this.mRenderComplete = true;
        int level = this.mLevel;
        int rotation = this.mRotation;
        int flags = 0;
        if (rotation != 0) {
            flags = 2;
        }
        if (flags != 0) {
            canvas.save(flags);
            if (rotation != 0) {
                int centerX = this.mViewWidth / 2;
                int centerY = this.mViewHeight / 2;
                canvas.translate(centerX, centerY);
                canvas.rotate(rotation, 0.0f, 0.0f, 1.0f);
                canvas.translate(-centerX, -centerY);
            }
        }
        try {
            if (level != this.mLevelCount) {
                int size = this.mTileSize << level;
                float length = size * this.mScale;
                Rect r = this.mTileRange;
                int ty = r.top;
                int i = 0;
                while (ty < r.bottom) {
                    float y = this.mOffsetY + (i * length);
                    int tx = r.left;
                    int j = 0;
                    while (tx < r.right) {
                        float x = this.mOffsetX + (j * length);
                        drawTile(canvas, tx, ty, level, x, y, length);
                        tx += size;
                        j++;
                    }
                    ty += size;
                    i++;
                }
            } else if (this.mPreview != null) {
                this.mPreview.draw(canvas, this.mOffsetX, this.mOffsetY, Math.round(this.mImageWidth * this.mScale), Math.round(this.mImageHeight * this.mScale));
            }
            if (this.mRenderComplete) {
                if (!this.mBackgroundTileUploaded) {
                    uploadBackgroundTiles(canvas);
                }
            } else {
                invalidate();
            }
            return this.mRenderComplete || this.mPreview != null;
        } finally {
            if (flags != 0) {
                canvas.restore();
            }
        }
    }

    private void uploadBackgroundTiles(GLCanvas canvas) {
        this.mBackgroundTileUploaded = true;
        int n = this.mActiveTiles.size();
        for (int i = 0; i < n; i++) {
            Tile tile = this.mActiveTiles.valueAt(i);
            if (!tile.isContentValid()) {
                queueForDecode(tile);
            }
        }
    }

    private void queueForDecode(Tile tile) {
        synchronized (this.mQueueLock) {
            if (tile.mTileState == 1) {
                tile.mTileState = 2;
                if (this.mDecodeQueue.push(tile)) {
                    this.mQueueLock.notifyAll();
                }
            }
        }
    }

    void decodeTile(Tile tile) {
        synchronized (this.mQueueLock) {
            if (tile.mTileState != 2) {
                return;
            }
            tile.mTileState = 4;
            boolean decodeComplete = tile.decode();
            synchronized (this.mQueueLock) {
                if (tile.mTileState == 32) {
                    tile.mTileState = 64;
                    if (tile.mDecodedTile != null) {
                        sTilePool.release(tile.mDecodedTile);
                        tile.mDecodedTile = null;
                    }
                    this.mRecycledQueue.push(tile);
                    return;
                }
                tile.mTileState = decodeComplete ? 8 : 16;
                if (!decodeComplete) {
                    return;
                }
                this.mUploadQueue.push(tile);
                invalidate();
            }
        }
    }

    private Tile obtainTile(int x, int y, int level) {
        synchronized (this.mQueueLock) {
            Tile tile = this.mRecycledQueue.pop();
            if (tile != null) {
                tile.mTileState = 1;
                tile.update(x, y, level);
                return tile;
            }
            return new Tile(x, y, level);
        }
    }

    private void recycleTile(Tile tile) {
        synchronized (this.mQueueLock) {
            if (tile.mTileState == 4) {
                tile.mTileState = 32;
                return;
            }
            tile.mTileState = 64;
            if (tile.mDecodedTile != null) {
                sTilePool.release(tile.mDecodedTile);
                tile.mDecodedTile = null;
            }
            this.mRecycledQueue.push(tile);
        }
    }

    private void activateTile(int x, int y, int level) {
        long key = makeTileKey(x, y, level);
        Tile tile = this.mActiveTiles.get(key);
        if (tile != null) {
            if (tile.mTileState == 2) {
                tile.mTileState = 1;
            }
        } else {
            this.mActiveTiles.put(key, obtainTile(x, y, level));
        }
    }

    Tile getTile(int x, int y, int level) {
        return this.mActiveTiles.get(makeTileKey(x, y, level));
    }

    private static long makeTileKey(int x, int y, int level) {
        long result = x;
        return (((result << 16) | ((long) y)) << 16) | ((long) level);
    }

    private void uploadTiles(GLCanvas canvas) {
        int quota = 1;
        Tile tile = null;
        while (quota > 0) {
            synchronized (this.mQueueLock) {
                tile = this.mUploadQueue.pop();
            }
            if (tile == null) {
                break;
            }
            if (!tile.isContentValid()) {
                if (tile.mTileState == 8) {
                    tile.updateContent(canvas);
                    quota--;
                } else {
                    Log.w("TiledImageRenderer", "Tile in upload queue has invalid state: " + tile.mTileState);
                }
            }
        }
        if (tile == null) {
            return;
        }
        invalidate();
    }

    private void drawTile(GLCanvas canvas, int tx, int ty, int level, float x, float y, float length) {
        RectF source = this.mSourceRect;
        RectF target = this.mTargetRect;
        target.set(x, y, x + length, y + length);
        source.set(0.0f, 0.0f, this.mTileSize, this.mTileSize);
        Tile tile = getTile(tx, ty, level);
        if (tile != null) {
            if (!tile.isContentValid()) {
                if (tile.mTileState == 8) {
                    if (this.mUploadQuota > 0) {
                        this.mUploadQuota--;
                        tile.updateContent(canvas);
                    } else {
                        this.mRenderComplete = false;
                    }
                } else if (tile.mTileState != 16) {
                    this.mRenderComplete = false;
                    queueForDecode(tile);
                }
            }
            if (drawTile(tile, canvas, source, target)) {
                return;
            }
        }
        if (this.mPreview == null) {
            return;
        }
        int size = this.mTileSize << level;
        float scaleX = this.mPreview.getWidth() / this.mImageWidth;
        float scaleY = this.mPreview.getHeight() / this.mImageHeight;
        source.set(tx * scaleX, ty * scaleY, (tx + size) * scaleX, (ty + size) * scaleY);
        canvas.drawTexture(this.mPreview, source, target);
    }

    private boolean drawTile(Tile tile, GLCanvas canvas, RectF source, RectF target) {
        while (!tile.isContentValid()) {
            Tile parent = tile.getParentTile();
            if (parent == null) {
                return false;
            }
            if (tile.mX == parent.mX) {
                source.left /= 2.0f;
                source.right /= 2.0f;
            } else {
                source.left = (this.mTileSize + source.left) / 2.0f;
                source.right = (this.mTileSize + source.right) / 2.0f;
            }
            if (tile.mY == parent.mY) {
                source.top /= 2.0f;
                source.bottom /= 2.0f;
            } else {
                source.top = (this.mTileSize + source.top) / 2.0f;
                source.bottom = (this.mTileSize + source.bottom) / 2.0f;
            }
            tile = parent;
        }
        canvas.drawTexture(tile, source, target);
        return true;
    }

    private class Tile extends UploadedTexture {
        public Bitmap mDecodedTile;
        public Tile mNext;
        public int mTileLevel;
        public volatile int mTileState = 1;
        public int mX;
        public int mY;

        public Tile(int x, int y, int level) {
            this.mX = x;
            this.mY = y;
            this.mTileLevel = level;
        }

        @Override
        protected void onFreeBitmap(Bitmap bitmap) {
            TiledImageRenderer.sTilePool.release(bitmap);
        }

        boolean decode() {
            try {
                Bitmap reuse = TiledImageRenderer.sTilePool.acquire();
                if (reuse != null && reuse.getWidth() != TiledImageRenderer.this.mTileSize) {
                    reuse = null;
                }
                this.mDecodedTile = TiledImageRenderer.this.mModel.getTile(this.mTileLevel, this.mX, this.mY, reuse);
            } catch (Throwable t) {
                Log.w("TiledImageRenderer", "fail to decode tile", t);
            }
            return this.mDecodedTile != null;
        }

        @Override
        protected Bitmap onGetBitmap() {
            Utils.assertTrue(this.mTileState == 8);
            int rightEdge = (TiledImageRenderer.this.mImageWidth - this.mX) >> this.mTileLevel;
            int bottomEdge = (TiledImageRenderer.this.mImageHeight - this.mY) >> this.mTileLevel;
            setSize(Math.min(TiledImageRenderer.this.mTileSize, rightEdge), Math.min(TiledImageRenderer.this.mTileSize, bottomEdge));
            Bitmap bitmap = this.mDecodedTile;
            this.mDecodedTile = null;
            this.mTileState = 1;
            return bitmap;
        }

        @Override
        public int getTextureWidth() {
            return TiledImageRenderer.this.mTileSize;
        }

        @Override
        public int getTextureHeight() {
            return TiledImageRenderer.this.mTileSize;
        }

        public void update(int x, int y, int level) {
            this.mX = x;
            this.mY = y;
            this.mTileLevel = level;
            invalidateContent();
        }

        public Tile getParentTile() {
            if (this.mTileLevel + 1 == TiledImageRenderer.this.mLevelCount) {
                return null;
            }
            int size = TiledImageRenderer.this.mTileSize << (this.mTileLevel + 1);
            int x = size * (this.mX / size);
            int y = size * (this.mY / size);
            return TiledImageRenderer.this.getTile(x, y, this.mTileLevel + 1);
        }

        public String toString() {
            return String.format("tile(%s, %s, %s / %s)", Integer.valueOf(this.mX / TiledImageRenderer.this.mTileSize), Integer.valueOf(this.mY / TiledImageRenderer.this.mTileSize), Integer.valueOf(TiledImageRenderer.this.mLevel), Integer.valueOf(TiledImageRenderer.this.mLevelCount));
        }
    }

    static class TileQueue {
        private Tile mHead;

        TileQueue() {
        }

        public Tile pop() {
            Tile tile = this.mHead;
            if (tile != null) {
                this.mHead = tile.mNext;
            }
            return tile;
        }

        public boolean push(Tile tile) {
            if (contains(tile)) {
                Log.w("TiledImageRenderer", "Attempting to add a tile already in the queue!");
                return false;
            }
            boolean wasEmpty = this.mHead == null;
            tile.mNext = this.mHead;
            this.mHead = tile;
            return wasEmpty;
        }

        private boolean contains(Tile tile) {
            for (Tile other = this.mHead; other != null; other = other.mNext) {
                if (other == tile) {
                    return true;
                }
            }
            return false;
        }

        public void clean() {
            this.mHead = null;
        }
    }

    class TileDecoder extends Thread {
        TileDecoder() {
        }

        public void finishAndWait() {
            interrupt();
            try {
                join();
            } catch (InterruptedException e) {
                Log.w("TiledImageRenderer", "Interrupted while waiting for TileDecoder thread to finish!");
            }
        }

        private Tile waitForTile() throws InterruptedException {
            Tile tile;
            synchronized (TiledImageRenderer.this.mQueueLock) {
                while (true) {
                    tile = TiledImageRenderer.this.mDecodeQueue.pop();
                    if (tile == null) {
                        TiledImageRenderer.this.mQueueLock.wait();
                    }
                }
            }
            return tile;
        }

        @Override
        public void run() {
            while (!isInterrupted()) {
                try {
                    Tile tile = waitForTile();
                    TiledImageRenderer.this.decodeTile(tile);
                } catch (InterruptedException e) {
                    return;
                }
            }
        }
    }
}
