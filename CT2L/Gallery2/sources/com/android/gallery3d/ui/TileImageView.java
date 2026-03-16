package com.android.gallery3d.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.RectF;
import android.support.v4.app.NotificationCompat;
import android.support.v4.util.LongSparseArray;
import android.util.DisplayMetrics;
import android.util.FloatMath;
import android.view.WindowManager;
import com.android.gallery3d.app.GalleryContext;
import com.android.gallery3d.common.Utils;
import com.android.gallery3d.data.DecodeUtils;
import com.android.gallery3d.glrenderer.GLCanvas;
import com.android.gallery3d.glrenderer.UploadedTexture;
import com.android.gallery3d.ui.GLRoot;
import com.android.gallery3d.util.Future;
import com.android.gallery3d.util.ThreadPool;
import com.android.photos.data.GalleryBitmapPool;
import java.util.concurrent.atomic.AtomicBoolean;

public class TileImageView extends GLView {
    private static int sTileSize;
    private boolean mBackgroundTileUploaded;
    protected int mCenterX;
    protected int mCenterY;
    private final TileQueue mDecodeQueue;
    private boolean mIsTextureFreed;
    protected int mLevelCount;
    private TileSource mModel;
    private int mOffsetX;
    private int mOffsetY;
    private final TileQueue mRecycledQueue;
    private boolean mRenderComplete;
    protected int mRotation;
    protected float mScale;
    private ScreenNail mScreenNail;
    private final ThreadPool mThreadPool;
    private Future<Void> mTileDecoder;
    private final TileUploader mTileUploader;
    private final TileQueue mUploadQueue;
    private int mUploadQuota;
    private int mLevel = 0;
    private final RectF mSourceRect = new RectF();
    private final RectF mTargetRect = new RectF();
    private final LongSparseArray<Tile> mActiveTiles = new LongSparseArray<>();
    protected int mImageWidth = -1;
    protected int mImageHeight = -1;
    private final Rect mTileRange = new Rect();
    private final Rect[] mActiveRange = {new Rect(), new Rect()};

    public interface TileSource {
        int getImageHeight();

        int getImageWidth();

        int getLevelCount();

        ScreenNail getScreenNail();

        Bitmap getTile(int i, int i2, int i3, int i4);
    }

    public static boolean isHighResolution(Context context) {
        DisplayMetrics metrics = new DisplayMetrics();
        WindowManager wm = (WindowManager) context.getSystemService("window");
        wm.getDefaultDisplay().getMetrics(metrics);
        return metrics.heightPixels > 2048 || metrics.widthPixels > 2048;
    }

    public TileImageView(GalleryContext context) {
        this.mRecycledQueue = new TileQueue();
        this.mUploadQueue = new TileQueue();
        this.mDecodeQueue = new TileQueue();
        this.mTileUploader = new TileUploader();
        this.mThreadPool = context.getThreadPool();
        this.mTileDecoder = this.mThreadPool.submit(new TileDecoder());
        if (sTileSize == 0) {
            if (isHighResolution(context.getAndroidContext())) {
                sTileSize = NotificationCompat.FLAG_GROUP_SUMMARY;
            } else {
                sTileSize = NotificationCompat.FLAG_LOCAL_ONLY;
            }
        }
    }

    public void setModel(TileSource model) {
        this.mModel = model;
        if (model != null) {
            notifyModelInvalidated();
        }
    }

    public void setScreenNail(ScreenNail s) {
        this.mScreenNail = s;
    }

    public void notifyModelInvalidated() {
        invalidateTiles();
        if (this.mModel == null) {
            this.mScreenNail = null;
            this.mImageWidth = 0;
            this.mImageHeight = 0;
            this.mLevelCount = 0;
        } else {
            setScreenNail(this.mModel.getScreenNail());
            this.mImageWidth = this.mModel.getImageWidth();
            this.mImageHeight = this.mModel.getImageHeight();
            this.mLevelCount = this.mModel.getLevelCount();
        }
        layoutTiles(this.mCenterX, this.mCenterY, this.mScale, this.mRotation);
        invalidate();
    }

    @Override
    protected void onLayout(boolean changeSize, int left, int top, int right, int bottom) {
        super.onLayout(changeSize, left, top, right, bottom);
        if (changeSize) {
            layoutTiles(this.mCenterX, this.mCenterY, this.mScale, this.mRotation);
        }
    }

    private void layoutTiles(int centerX, int centerY, float scale, int rotation) {
        int fromLevel;
        int width = getWidth();
        int height = getHeight();
        this.mLevel = Utils.clamp(Utils.floorLog2(1.0f / scale), 0, this.mLevelCount);
        if (this.mLevel != this.mLevelCount) {
            Rect range = this.mTileRange;
            getRange(range, centerX, centerY, this.mLevel, scale, rotation);
            this.mOffsetX = Math.round((width / 2.0f) + ((range.left - centerX) * scale));
            this.mOffsetY = Math.round((height / 2.0f) + ((range.top - centerY) * scale));
            fromLevel = ((float) (1 << this.mLevel)) * scale > 0.75f ? this.mLevel - 1 : this.mLevel;
        } else {
            fromLevel = this.mLevel - 2;
            this.mOffsetX = Math.round((width / 2.0f) - (centerX * scale));
            this.mOffsetY = Math.round((height / 2.0f) - (centerY * scale));
        }
        int fromLevel2 = Math.max(0, Math.min(fromLevel, this.mLevelCount - 2));
        int endLevel = Math.min(fromLevel2 + 2, this.mLevelCount);
        Rect[] range2 = this.mActiveRange;
        for (int i = fromLevel2; i < endLevel; i++) {
            getRange(range2[i - fromLevel2], centerX, centerY, i, rotation);
        }
        if (rotation % 90 == 0) {
            synchronized (this) {
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
                int size = sTileSize << i3;
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
    }

    protected synchronized void invalidateTiles() {
        this.mDecodeQueue.clean();
        this.mUploadQueue.clean();
        int n = this.mActiveTiles.size();
        for (int i = 0; i < n; i++) {
            Tile tile = this.mActiveTiles.valueAt(i);
            recycleTile(tile);
        }
        this.mActiveTiles.clear();
    }

    private void getRange(Rect out, int cX, int cY, int level, int rotation) {
        getRange(out, cX, cY, level, 1.0f / (1 << (level + 1)), rotation);
    }

    private void getRange(Rect out, int cX, int cY, int level, float scale, int rotation) {
        double radians = Math.toRadians(-rotation);
        double w = getWidth();
        double h = getHeight();
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);
        int width = (int) Math.ceil(Math.max(Math.abs((cos * w) - (sin * h)), Math.abs((cos * w) + (sin * h))));
        int height = (int) Math.ceil(Math.max(Math.abs((sin * w) + (cos * h)), Math.abs((sin * w) - (cos * h))));
        int left = (int) FloatMath.floor(cX - (width / (2.0f * scale)));
        int top = (int) FloatMath.floor(cY - (height / (2.0f * scale)));
        int right = (int) FloatMath.ceil(left + (width / scale));
        int bottom = (int) FloatMath.ceil(top + (height / scale));
        int size = sTileSize << level;
        out.set(Math.max(0, (left / size) * size), Math.max(0, (top / size) * size), Math.min(this.mImageWidth, right), Math.min(this.mImageHeight, bottom));
    }

    public boolean setPosition(int centerX, int centerY, float scale, int rotation) {
        if (this.mCenterX == centerX && this.mCenterY == centerY && this.mScale == scale && this.mRotation == rotation) {
            return false;
        }
        this.mCenterX = centerX;
        this.mCenterY = centerY;
        this.mScale = scale;
        this.mRotation = rotation;
        layoutTiles(centerX, centerY, scale, rotation);
        invalidate();
        return true;
    }

    public void freeTextures() {
        this.mIsTextureFreed = true;
        if (this.mTileDecoder != null) {
            this.mTileDecoder.cancel();
            this.mTileDecoder.get();
            this.mTileDecoder = null;
        }
        int n = this.mActiveTiles.size();
        for (int i = 0; i < n; i++) {
            Tile texture = this.mActiveTiles.valueAt(i);
            texture.recycle();
        }
        this.mActiveTiles.clear();
        this.mTileRange.set(0, 0, 0, 0);
        synchronized (this) {
            this.mUploadQueue.clean();
            this.mDecodeQueue.clean();
            Tile tile = this.mRecycledQueue.pop();
            while (tile != null) {
                tile.recycle();
                tile = this.mRecycledQueue.pop();
            }
        }
        setScreenNail(null);
    }

    public void prepareTextures() {
        Object[] objArr = 0;
        if (this.mTileDecoder == null) {
            this.mTileDecoder = this.mThreadPool.submit(new TileDecoder());
        }
        if (this.mIsTextureFreed) {
            layoutTiles(this.mCenterX, this.mCenterY, this.mScale, this.mRotation);
            this.mIsTextureFreed = false;
            setScreenNail(this.mModel != null ? this.mModel.getScreenNail() : null);
        }
    }

    @Override
    protected void render(GLCanvas canvas) {
        this.mUploadQuota = 1;
        this.mRenderComplete = true;
        int level = this.mLevel;
        int rotation = this.mRotation;
        int flags = rotation != 0 ? 0 | 2 : 0;
        if (flags != 0) {
            canvas.save(flags);
            if (rotation != 0) {
                int centerX = getWidth() / 2;
                int centerY = getHeight() / 2;
                canvas.translate(centerX, centerY);
                canvas.rotate(rotation, 0.0f, 0.0f, 1.0f);
                canvas.translate(-centerX, -centerY);
            }
        }
        try {
            if (level != this.mLevelCount && !isScreenNailAnimating()) {
                if (this.mScreenNail != null) {
                    this.mScreenNail.noDraw();
                }
                int size = sTileSize << level;
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
            } else if (this.mScreenNail != null) {
                this.mScreenNail.draw(canvas, this.mOffsetX, this.mOffsetY, Math.round(this.mImageWidth * this.mScale), Math.round(this.mImageHeight * this.mScale));
                if (isScreenNailAnimating()) {
                    invalidate();
                }
            }
            if (this.mRenderComplete) {
                if (!this.mBackgroundTileUploaded) {
                    uploadBackgroundTiles(canvas);
                    return;
                }
                return;
            }
            invalidate();
        } finally {
            if (flags != 0) {
                canvas.restore();
            }
        }
    }

    private boolean isScreenNailAnimating() {
        return (this.mScreenNail instanceof TiledScreenNail) && ((TiledScreenNail) this.mScreenNail).isAnimating();
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

    void queueForUpload(Tile tile) {
        synchronized (this) {
            this.mUploadQueue.push(tile);
        }
        if (this.mTileUploader.mActive.compareAndSet(false, true)) {
            getGLRoot().addOnGLIdleListener(this.mTileUploader);
        }
    }

    synchronized void queueForDecode(Tile tile) {
        if (tile.mTileState == 1) {
            tile.mTileState = 2;
            if (this.mDecodeQueue.push(tile)) {
                notifyAll();
            }
        }
    }

    boolean decodeTile(Tile tile) {
        synchronized (this) {
            if (tile.mTileState != 2) {
                return false;
            }
            tile.mTileState = 4;
            boolean decodeComplete = tile.decode();
            synchronized (this) {
                if (tile.mTileState == 32) {
                    tile.mTileState = 64;
                    if (tile.mDecodedTile != null) {
                        GalleryBitmapPool.getInstance().put(tile.mDecodedTile);
                        tile.mDecodedTile = null;
                    }
                    this.mRecycledQueue.push(tile);
                    return false;
                }
                tile.mTileState = decodeComplete ? 8 : 16;
                return decodeComplete;
            }
        }
    }

    private synchronized Tile obtainTile(int x, int y, int level) {
        Tile tile;
        tile = this.mRecycledQueue.pop();
        if (tile != null) {
            tile.mTileState = 1;
            tile.update(x, y, level);
        } else {
            tile = new Tile(x, y, level);
        }
        return tile;
    }

    synchronized void recycleTile(Tile tile) {
        if (tile.mTileState == 4) {
            tile.mTileState = 32;
        } else {
            tile.mTileState = 64;
            if (tile.mDecodedTile != null) {
                GalleryBitmapPool.getInstance().put(tile.mDecodedTile);
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

    private Tile getTile(int x, int y, int level) {
        return this.mActiveTiles.get(makeTileKey(x, y, level));
    }

    private static long makeTileKey(int x, int y, int level) {
        long result = x;
        return (((result << 16) | ((long) y)) << 16) | ((long) level);
    }

    private class TileUploader implements GLRoot.OnGLIdleListener {
        AtomicBoolean mActive;

        private TileUploader() {
            this.mActive = new AtomicBoolean(false);
        }

        @Override
        public boolean onGLIdle(GLCanvas canvas, boolean renderRequested) {
            if (renderRequested) {
                return true;
            }
            int quota = 1;
            Tile tile = null;
            while (quota > 0) {
                synchronized (TileImageView.this) {
                    tile = TileImageView.this.mUploadQueue.pop();
                }
                if (tile == null) {
                    break;
                }
                if (!tile.isContentValid()) {
                    boolean hasBeenLoaded = tile.isLoaded();
                    Utils.assertTrue(tile.mTileState == 8);
                    tile.updateContent(canvas);
                    if (!hasBeenLoaded) {
                        tile.draw(canvas, 0, 0);
                    }
                    quota--;
                }
            }
            if (tile == null) {
                this.mActive.set(false);
            }
            return tile != null;
        }
    }

    public void drawTile(GLCanvas canvas, int tx, int ty, int level, float x, float y, float length) {
        RectF source = this.mSourceRect;
        RectF target = this.mTargetRect;
        target.set(x, y, x + length, y + length);
        source.set(0.0f, 0.0f, sTileSize, sTileSize);
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
        if (this.mScreenNail != null) {
            int size = sTileSize << level;
            float scaleX = this.mScreenNail.getWidth() / this.mImageWidth;
            float scaleY = this.mScreenNail.getHeight() / this.mImageHeight;
            source.set(tx * scaleX, ty * scaleY, (tx + size) * scaleX, (ty + size) * scaleY);
            this.mScreenNail.draw(canvas, source, target);
        }
    }

    static boolean drawTile(Tile tile, GLCanvas canvas, RectF source, RectF target) {
        while (!tile.isContentValid()) {
            Tile parent = tile.getParentTile();
            if (parent == null) {
                return false;
            }
            if (tile.mX == parent.mX) {
                source.left /= 2.0f;
                source.right /= 2.0f;
            } else {
                source.left = (sTileSize + source.left) / 2.0f;
                source.right = (sTileSize + source.right) / 2.0f;
            }
            if (tile.mY == parent.mY) {
                source.top /= 2.0f;
                source.bottom /= 2.0f;
            } else {
                source.top = (sTileSize + source.top) / 2.0f;
                source.bottom = (sTileSize + source.bottom) / 2.0f;
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
            GalleryBitmapPool.getInstance().put(bitmap);
        }

        boolean decode() {
            try {
                this.mDecodedTile = DecodeUtils.ensureGLCompatibleBitmap(TileImageView.this.mModel.getTile(this.mTileLevel, this.mX, this.mY, TileImageView.sTileSize));
            } catch (Throwable t) {
                Log.w("TileImageView", "fail to decode tile", t);
            }
            return this.mDecodedTile != null;
        }

        @Override
        protected Bitmap onGetBitmap() {
            Utils.assertTrue(this.mTileState == 8);
            int rightEdge = (TileImageView.this.mImageWidth - this.mX) >> this.mTileLevel;
            int bottomEdge = (TileImageView.this.mImageHeight - this.mY) >> this.mTileLevel;
            setSize(Math.min(TileImageView.sTileSize, rightEdge), Math.min(TileImageView.sTileSize, bottomEdge));
            Bitmap bitmap = this.mDecodedTile;
            this.mDecodedTile = null;
            this.mTileState = 1;
            return bitmap;
        }

        @Override
        public int getTextureWidth() {
            return TileImageView.sTileSize;
        }

        @Override
        public int getTextureHeight() {
            return TileImageView.sTileSize;
        }

        public void update(int x, int y, int level) {
            this.mX = x;
            this.mY = y;
            this.mTileLevel = level;
            invalidateContent();
        }

        public Tile getParentTile() {
            if (this.mTileLevel + 1 == TileImageView.this.mLevelCount) {
                return null;
            }
            int size = TileImageView.sTileSize << (this.mTileLevel + 1);
            int x = size * (this.mX / size);
            int y = size * (this.mY / size);
            return TileImageView.this.getTile(x, y, this.mTileLevel + 1);
        }

        public String toString() {
            return String.format("tile(%s, %s, %s / %s)", Integer.valueOf(this.mX / TileImageView.sTileSize), Integer.valueOf(this.mY / TileImageView.sTileSize), Integer.valueOf(TileImageView.this.mLevel), Integer.valueOf(TileImageView.this.mLevelCount));
        }
    }

    private static class TileQueue {
        private Tile mHead;

        private TileQueue() {
        }

        public Tile pop() {
            Tile tile = this.mHead;
            if (tile != null) {
                this.mHead = tile.mNext;
            }
            return tile;
        }

        public boolean push(Tile tile) {
            boolean wasEmpty = this.mHead == null;
            tile.mNext = this.mHead;
            this.mHead = tile;
            return wasEmpty;
        }

        public void clean() {
            this.mHead = null;
        }
    }

    private class TileDecoder implements ThreadPool.Job<Void> {
        private ThreadPool.CancelListener mNotifier;

        private TileDecoder() {
            this.mNotifier = new ThreadPool.CancelListener() {
                @Override
                public void onCancel() {
                    synchronized (TileImageView.this) {
                        TileImageView.this.notifyAll();
                    }
                }
            };
        }

        @Override
        public Void run(ThreadPool.JobContext jc) {
            Tile tile;
            jc.setMode(0);
            jc.setCancelListener(this.mNotifier);
            while (!jc.isCancelled()) {
                synchronized (TileImageView.this) {
                    tile = TileImageView.this.mDecodeQueue.pop();
                    if (tile == null && !jc.isCancelled()) {
                        Utils.waitWithoutInterrupt(TileImageView.this);
                    }
                }
                if (tile != null && TileImageView.this.decodeTile(tile)) {
                    TileImageView.this.queueForUpload(tile);
                }
            }
            return null;
        }
    }
}
