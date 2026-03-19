package com.android.gallery3d.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Build;
import android.support.v4.util.LongSparseArray;
import android.util.DisplayMetrics;
import android.view.WindowManager;
import com.android.gallery3d.app.GalleryContext;
import com.android.gallery3d.app.PhotoDataAdapter;
import com.android.gallery3d.common.Utils;
import com.android.gallery3d.data.DecodeUtils;
import com.android.gallery3d.glrenderer.GLCanvas;
import com.android.gallery3d.glrenderer.UploadedTexture;
import com.android.gallery3d.ui.GLRoot;
import com.android.gallery3d.util.ThreadPool;
import com.android.photos.data.GalleryBitmapPool;
import com.mediatek.gallerybasic.util.DebugUtils;
import com.mediatek.galleryportable.SystemPropertyUtils;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

public class TileImageView extends GLView {
    private static final int HIGH_RESOLUTION_THRESHOLD = 1920;
    public static final int SIZE_UNKNOWN = -1;
    private static final int STATE_ACTIVATED = 1;
    private static final int STATE_DECODED = 8;
    private static final int STATE_DECODE_FAIL = 16;
    private static final int STATE_DECODING = 4;
    private static final int STATE_IN_QUEUE = 2;
    private static final int STATE_RECYCLED = 64;
    private static final int STATE_RECYCLING = 32;
    private static final String TAG = "Gallery2/TileImageView";
    private static final int UPLOAD_LIMIT = 1;
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
    private ArrayList<Thread> mTileDecoderThread;
    private final TileUploader mTileUploader;
    private final TileQueue mUploadQueue;
    private int mUploadQuota;
    public static long sScreenNailShowEnd = 0;
    public static boolean sPerformanceCaseRunning = false;
    private static final int TILE_DECODER_NUM = SystemPropertyUtils.getInt("gallery.tile.thread", 4);
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
        DisplayMetrics displayMetrics = new DisplayMetrics();
        WindowManager windowManager = (WindowManager) context.getSystemService("window");
        if (Build.VERSION.SDK_INT >= 17) {
            windowManager.getDefaultDisplay().getRealMetrics(displayMetrics);
        } else {
            windowManager.getDefaultDisplay().getMetrics(displayMetrics);
        }
        return displayMetrics.heightPixels >= HIGH_RESOLUTION_THRESHOLD || displayMetrics.widthPixels >= HIGH_RESOLUTION_THRESHOLD;
    }

    public TileImageView(GalleryContext galleryContext) {
        this.mRecycledQueue = new TileQueue();
        this.mUploadQueue = new TileQueue();
        this.mDecodeQueue = new TileQueue();
        this.mTileUploader = new TileUploader();
        this.mThreadPool = galleryContext.getThreadPool();
        Log.d(TAG, "<TileImageView> TILE_DECODER_NUM = " + TILE_DECODER_NUM);
        this.mTileDecoderThread = new ArrayList<>();
        for (int i = 0; i < TILE_DECODER_NUM; i++) {
            TileDecoder tileDecoder = new TileDecoder();
            tileDecoder.setName("TileDecoder-" + i);
            tileDecoder.start();
            Log.d(TAG, "<TileImageView> create Thread-" + i + ", id = " + tileDecoder.getId());
            this.mTileDecoderThread.add(tileDecoder);
        }
        if (sTileSize == 0) {
            if (isHighResolution(galleryContext.getAndroidContext())) {
                sTileSize = 512;
            } else {
                sTileSize = 254;
            }
        }
    }

    public void setModel(TileSource tileSource) {
        this.mModel = tileSource;
        if (tileSource != null) {
            notifyModelInvalidated();
        }
    }

    public void setScreenNail(ScreenNail screenNail) {
        this.mScreenNail = screenNail;
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
    protected void onLayout(boolean z, int i, int i2, int i3, int i4) {
        super.onLayout(z, i, i2, i3, i4);
        if (z) {
            layoutTiles(this.mCenterX, this.mCenterY, this.mScale, this.mRotation);
        }
    }

    private void layoutTiles(int i, int i2, float f, int i3) {
        int i4;
        int width = getWidth();
        int height = getHeight();
        this.mLevel = Utils.clamp(Utils.floorLog2(1.0f / f), 0, this.mLevelCount);
        if (this.mLevel != this.mLevelCount) {
            getRange(this.mTileRange, i, i2, this.mLevel, f, i3);
            this.mOffsetX = Math.round((width / 2.0f) + ((r7.left - i) * f));
            this.mOffsetY = Math.round((height / 2.0f) + ((r7.top - i2) * f));
            i4 = ((float) (1 << this.mLevel)) * f > 0.75f ? this.mLevel - 1 : this.mLevel;
        } else {
            i4 = this.mLevel - 2;
            this.mOffsetX = Math.round((width / 2.0f) - (i * f));
            this.mOffsetY = Math.round((height / 2.0f) - (i2 * f));
        }
        int iMax = Math.max(0, Math.min(i4, this.mLevelCount - 2));
        int iMin = Math.min(iMax + 2, this.mLevelCount);
        Rect[] rectArr = this.mActiveRange;
        for (int i5 = iMax; i5 < iMin; i5++) {
            getRange(rectArr[i5 - iMax], i, i2, i5, i3);
        }
        if (i3 % 90 != 0) {
            return;
        }
        synchronized (this) {
            this.mDecodeQueue.clean();
            this.mUploadQueue.clean();
            int i6 = 0;
            this.mBackgroundTileUploaded = false;
            int size = this.mActiveTiles.size();
            while (i6 < size) {
                Tile tileValueAt = this.mActiveTiles.valueAt(i6);
                int i7 = tileValueAt.mTileLevel;
                if (i7 < iMax || i7 >= iMin || !rectArr[i7 - iMax].contains(tileValueAt.mX, tileValueAt.mY)) {
                    this.mActiveTiles.removeAt(i6);
                    i6--;
                    size--;
                    recycleTile(tileValueAt);
                }
                i6++;
            }
        }
        for (int i8 = iMax; i8 < iMin; i8++) {
            int i9 = sTileSize << i8;
            Rect rect = rectArr[i8 - iMax];
            int i10 = rect.bottom;
            for (int i11 = rect.top; i11 < i10; i11 += i9) {
                int i12 = rect.right;
                for (int i13 = rect.left; i13 < i12; i13 += i9) {
                    activateTile(i13, i11, i8);
                }
            }
        }
        invalidate();
    }

    protected synchronized void invalidateTiles() {
        this.mDecodeQueue.clean();
        this.mUploadQueue.clean();
        int size = this.mActiveTiles.size();
        for (int i = 0; i < size; i++) {
            recycleTile(this.mActiveTiles.valueAt(i));
        }
        this.mActiveTiles.clear();
    }

    private void getRange(Rect rect, int i, int i2, int i3, int i4) {
        getRange(rect, i, i2, i3, 1.0f / (1 << (i3 + 1)), i4);
    }

    private void getRange(Rect rect, int i, int i2, int i3, float f, int i4) {
        double radians = Math.toRadians(-i4);
        double width = getWidth();
        double height = getHeight();
        double dCos = Math.cos(radians);
        double dSin = Math.sin(radians);
        double d = dCos * width;
        double d2 = dSin * height;
        int iCeil = (int) Math.ceil(Math.max(Math.abs(d - d2), Math.abs(d + d2)));
        double d3 = dSin * width;
        double d4 = dCos * height;
        int iCeil2 = (int) Math.ceil(Math.max(Math.abs(d3 + d4), Math.abs(d3 - d4)));
        float f2 = iCeil;
        float f3 = 2.0f * f;
        int iFloor = (int) Math.floor(i - (f2 / f3));
        float f4 = iCeil2;
        int iFloor2 = (int) Math.floor(i2 - (f4 / f3));
        int iCeil3 = (int) Math.ceil(iFloor + (f2 / f));
        int iCeil4 = (int) Math.ceil(iFloor2 + (f4 / f));
        int i5 = sTileSize << i3;
        rect.set(Math.max(0, (iFloor / i5) * i5), Math.max(0, i5 * (iFloor2 / i5)), Math.min(this.mImageWidth, iCeil3), Math.min(this.mImageHeight, iCeil4));
    }

    public void getImageCenter(Point point) {
        int i;
        int i2;
        int width = getWidth();
        int height = getHeight();
        if (this.mRotation % 180 == 0) {
            i = (this.mImageWidth / 2) - this.mCenterX;
            i2 = (this.mImageHeight / 2) - this.mCenterY;
        } else {
            i = (this.mImageHeight / 2) - this.mCenterY;
            i2 = (this.mImageWidth / 2) - this.mCenterX;
        }
        point.x = Math.round((width / 2.0f) + (i * this.mScale));
        point.y = Math.round((height / 2.0f) + (i2 * this.mScale));
    }

    public boolean setPosition(int i, int i2, float f, int i3) {
        if (this.mCenterX == i && this.mCenterY == i2 && this.mScale == f && this.mRotation == i3) {
            return false;
        }
        this.mCenterX = i;
        this.mCenterY = i2;
        this.mScale = f;
        this.mRotation = i3;
        layoutTiles(i, i2, f, i3);
        invalidate();
        return true;
    }

    public void freeTextures() {
        this.mIsTextureFreed = true;
        if (this.mTileDecoderThread != null) {
            for (int i = 0; i < TILE_DECODER_NUM; i++) {
                if (this.mTileDecoderThread.get(i) != null) {
                    this.mTileDecoderThread.get(i).interrupt();
                }
            }
            this.mTileDecoderThread.clear();
            this.mTileDecoderThread = null;
        }
        int size = this.mActiveTiles.size();
        for (int i2 = 0; i2 < size; i2++) {
            this.mActiveTiles.valueAt(i2).recycle();
        }
        this.mActiveTiles.clear();
        this.mTileRange.set(0, 0, 0, 0);
        synchronized (this) {
            this.mUploadQueue.clean();
            this.mDecodeQueue.clean();
            Tile tilePop = this.mRecycledQueue.pop();
            while (tilePop != null) {
                tilePop.recycle();
                tilePop = this.mRecycledQueue.pop();
            }
        }
        setScreenNail(null);
    }

    public void prepareTextures() {
        Object[] objArr = 0;
        if (this.mTileDecoderThread == null) {
            this.mTileDecoderThread = new ArrayList<>();
            for (int i = 0; i < TILE_DECODER_NUM; i++) {
                TileDecoder tileDecoder = new TileDecoder();
                tileDecoder.setName("TileDecoder-" + i);
                tileDecoder.start();
                Log.d(TAG, "<TileImageView> create Thread-" + i + ", id = " + tileDecoder.getId());
                this.mTileDecoderThread.add(tileDecoder);
            }
        }
        if (this.mIsTextureFreed) {
            layoutTiles(this.mCenterX, this.mCenterY, this.mScale, this.mRotation);
            this.mIsTextureFreed = false;
            setScreenNail(this.mModel != null ? this.mModel.getScreenNail() : null);
        }
    }

    @Override
    protected void render(GLCanvas gLCanvas) {
        this.mUploadQuota = 1;
        this.mRenderComplete = true;
        int i = this.mLevel;
        int i2 = this.mRotation;
        int i3 = i2 != 0 ? 2 : 0;
        if (i3 != 0) {
            gLCanvas.save(i3);
            if (i2 != 0) {
                gLCanvas.translate(getWidth() / 2, getHeight() / 2);
                gLCanvas.rotate(i2, 0.0f, 0.0f, 1.0f);
                gLCanvas.translate(-r3, -r4);
            }
        }
        try {
            if (i != this.mLevelCount && !isScreenNailAnimating()) {
                if (this.mScreenNail != null) {
                    this.mScreenNail.noDraw();
                }
                int i4 = sTileSize << i;
                float f = i4 * this.mScale;
                Rect rect = this.mTileRange;
                int i5 = rect.top;
                int i6 = 0;
                while (i5 < rect.bottom) {
                    float f2 = this.mOffsetY + (i6 * f);
                    int i7 = rect.left;
                    int i8 = 0;
                    while (i7 < rect.right) {
                        drawTile(gLCanvas, i7, i5, i, this.mOffsetX + (i8 * f), f2, f);
                        i7 += i4;
                        i8++;
                        i5 = i5;
                        i6 = i6;
                        rect = rect;
                    }
                    i5 += i4;
                    i6++;
                }
                if (sScreenNailShowEnd == 0 && sPerformanceCaseRunning) {
                    sScreenNailShowEnd = System.currentTimeMillis();
                    Log.d(TAG, "[CMCC Performance test][Gallery2][Gallery] load 1M image time end [" + sScreenNailShowEnd + "]");
                }
            } else if (this.mScreenNail != null) {
                this.mScreenNail.draw(gLCanvas, this.mOffsetX, this.mOffsetY, Math.round(this.mImageWidth * this.mScale), Math.round(this.mImageHeight * this.mScale));
                if (isScreenNailAnimating()) {
                    invalidate();
                } else if (PhotoDataAdapter.sCurrentScreenNailDone && sPerformanceCaseRunning && sScreenNailShowEnd == 0) {
                    sScreenNailShowEnd = System.currentTimeMillis();
                    Log.d(TAG, "[CMCC Performance test][Gallery2][Gallery] load 1M image time end [" + sScreenNailShowEnd + "]");
                }
            }
            if (this.mRenderComplete) {
                if (!this.mBackgroundTileUploaded) {
                    uploadBackgroundTiles(gLCanvas);
                    return;
                }
                return;
            }
            invalidate();
        } finally {
            if (i3 != 0) {
                gLCanvas.restore();
            }
        }
    }

    private boolean isScreenNailAnimating() {
        return (this.mScreenNail instanceof BitmapScreenNail) && ((BitmapScreenNail) this.mScreenNail).isAnimating();
    }

    private void uploadBackgroundTiles(GLCanvas gLCanvas) {
        this.mBackgroundTileUploaded = true;
        int size = this.mActiveTiles.size();
        for (int i = 0; i < size; i++) {
            Tile tileValueAt = this.mActiveTiles.valueAt(i);
            if (!tileValueAt.isContentValid()) {
                queueForDecode(tileValueAt);
            }
        }
    }

    void queueForUpload(Tile tile) {
        GLRoot gLRoot;
        synchronized (this) {
            this.mUploadQueue.push(tile);
        }
        if (this.mTileUploader.mActive.compareAndSet(false, true) && (gLRoot = getGLRoot()) != null) {
            gLRoot.addOnGLIdleListener(this.mTileUploader);
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
            boolean zDecode = tile.decode();
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
                tile.mTileState = zDecode ? 8 : 16;
                return zDecode;
            }
        }
    }

    private synchronized Tile obtainTile(int i, int i2, int i3) {
        Tile tilePop = this.mRecycledQueue.pop();
        if (tilePop != null) {
            tilePop.mTileState = 1;
            tilePop.update(i, i2, i3);
            return tilePop;
        }
        return new Tile(i, i2, i3);
    }

    synchronized void recycleTile(Tile tile) {
        if (tile.mTileState == 4) {
            tile.mTileState = 32;
            return;
        }
        tile.mTileState = 64;
        if (tile.mDecodedTile != null) {
            GalleryBitmapPool.getInstance().put(tile.mDecodedTile);
            tile.mDecodedTile = null;
        }
        this.mRecycledQueue.push(tile);
    }

    private void activateTile(int i, int i2, int i3) {
        long jMakeTileKey = makeTileKey(i, i2, i3);
        Tile tile = this.mActiveTiles.get(jMakeTileKey);
        if (tile != null) {
            if (tile.mTileState == 2) {
                tile.mTileState = 1;
            }
        } else {
            this.mActiveTiles.put(jMakeTileKey, obtainTile(i, i2, i3));
        }
    }

    private Tile getTile(int i, int i2, int i3) {
        return this.mActiveTiles.get(makeTileKey(i, i2, i3));
    }

    private static long makeTileKey(int i, int i2, int i3) {
        return (((((long) i) << 16) | ((long) i2)) << 16) | ((long) i3);
    }

    private class TileUploader implements GLRoot.OnGLIdleListener {
        AtomicBoolean mActive;

        private TileUploader() {
            this.mActive = new AtomicBoolean(false);
        }

        @Override
        public boolean onGLIdle(GLCanvas gLCanvas, boolean z) {
            Log.d(TileImageView.TAG, "<TileUploader.onGLIdle> begin");
            if (z) {
                Log.d(TileImageView.TAG, "<TileUploader.onGLIdle> renderRequested, return");
                return true;
            }
            Tile tilePop = null;
            int i = 1;
            while (true) {
                if (i <= 0) {
                    break;
                }
                synchronized (TileImageView.this) {
                    tilePop = TileImageView.this.mUploadQueue.pop();
                }
                if (tilePop == null) {
                    Log.d(TileImageView.TAG, "<TileUploader.onGLIdle> [while] tile is null, break");
                    break;
                }
                if (!tilePop.isContentValid()) {
                    boolean zIsLoaded = tilePop.isLoaded();
                    if (tilePop.mTileState != 8) {
                        Log.d(TileImageView.TAG, "<TileUploader.onGLIdle> [while] tile not DECODED, break");
                        break;
                    }
                    tilePop.updateContent(gLCanvas);
                    if (!zIsLoaded) {
                        tilePop.draw(gLCanvas, 0, 0);
                    }
                    i--;
                }
            }
            if (tilePop == null) {
                this.mActive.set(false);
            }
            StringBuilder sb = new StringBuilder();
            sb.append("<TileUploader.onGLIdle> end and return ");
            sb.append(tilePop != null);
            Log.d(TileImageView.TAG, sb.toString());
            return tilePop != null;
        }
    }

    public void drawTile(GLCanvas gLCanvas, int i, int i2, int i3, float f, float f2, float f3) {
        RectF rectF = this.mSourceRect;
        RectF rectF2 = this.mTargetRect;
        rectF2.set(f, f2, f + f3, f3 + f2);
        rectF.set(0.0f, 0.0f, sTileSize, sTileSize);
        Tile tile = getTile(i, i2, i3);
        if (tile != null) {
            if (!tile.isContentValid()) {
                if (tile.mTileState == 8) {
                    if (this.mUploadQuota > 0) {
                        this.mUploadQuota--;
                        tile.updateContent(gLCanvas);
                    } else {
                        this.mRenderComplete = false;
                    }
                } else if (tile.mTileState != 16) {
                    this.mRenderComplete = false;
                    queueForDecode(tile);
                }
            }
            synchronized (this) {
                if (drawTile(tile, gLCanvas, rectF, rectF2)) {
                    return;
                }
            }
        }
        if (this.mScreenNail != null) {
            int i4 = sTileSize << i3;
            float width = this.mScreenNail.getWidth() / this.mImageWidth;
            float height = this.mScreenNail.getHeight() / this.mImageHeight;
            rectF.set(i * width, i2 * height, (i + i4) * width, (i2 + i4) * height);
            this.mScreenNail.draw(gLCanvas, rectF, rectF2);
        }
    }

    static boolean drawTile(Tile tile, GLCanvas gLCanvas, RectF rectF, RectF rectF2) {
        while (!tile.isContentValid()) {
            Tile parentTile = tile.getParentTile();
            if (parentTile == null) {
                return false;
            }
            if (tile.mX == parentTile.mX) {
                rectF.left /= 2.0f;
                rectF.right /= 2.0f;
            } else {
                rectF.left = (sTileSize + rectF.left) / 2.0f;
                rectF.right = (sTileSize + rectF.right) / 2.0f;
            }
            if (tile.mY == parentTile.mY) {
                rectF.top /= 2.0f;
                rectF.bottom /= 2.0f;
            } else {
                rectF.top = (sTileSize + rectF.top) / 2.0f;
                rectF.bottom = (sTileSize + rectF.bottom) / 2.0f;
            }
            tile = parentTile;
        }
        gLCanvas.drawTexture(tile, rectF, rectF2);
        if (DebugUtils.TILE) {
            float alpha = gLCanvas.getAlpha();
            gLCanvas.setAlpha(0.3f);
            gLCanvas.fillRect(rectF2.left, rectF2.top, rectF2.width(), rectF2.height(), -65536);
            gLCanvas.setAlpha(alpha);
            return true;
        }
        return true;
    }

    private class Tile extends UploadedTexture {
        public Bitmap mDecodedTile;
        public Tile mNext;
        public int mTileLevel;
        public volatile int mTileState = 1;
        public int mX;
        public int mY;

        public Tile(int i, int i2, int i3) {
            this.mX = i;
            this.mY = i2;
            this.mTileLevel = i3;
        }

        @Override
        protected void onFreeBitmap(Bitmap bitmap) {
            GalleryBitmapPool.getInstance().put(bitmap);
        }

        boolean decode() {
            try {
                this.mDecodedTile = DecodeUtils.ensureGLCompatibleBitmap(TileImageView.this.mModel.getTile(this.mTileLevel, this.mX, this.mY, TileImageView.sTileSize));
            } catch (Throwable th) {
                Log.w(TileImageView.TAG, "fail to decode tile", th);
            }
            return this.mDecodedTile != null;
        }

        @Override
        protected Bitmap onGetBitmap() {
            if (this.mTileState != 8) {
                Log.e(TileImageView.TAG, "<onGetBitmap>mTileState:" + this.mTileState);
            }
            Utils.assertTrue(this.mTileState == 8);
            setSize(Math.min(TileImageView.sTileSize, (TileImageView.this.mImageWidth - this.mX) >> this.mTileLevel), Math.min(TileImageView.sTileSize, (TileImageView.this.mImageHeight - this.mY) >> this.mTileLevel));
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

        public void update(int i, int i2, int i3) {
            this.mX = i;
            this.mY = i2;
            this.mTileLevel = i3;
            invalidateContent();
        }

        public Tile getParentTile() {
            if (this.mTileLevel + 1 == TileImageView.this.mLevelCount) {
                return null;
            }
            int i = TileImageView.sTileSize << (this.mTileLevel + 1);
            return TileImageView.this.getTile((this.mX / i) * i, i * (this.mY / i), this.mTileLevel + 1);
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
            boolean z = this.mHead == null;
            if (tile == this.mHead) {
                Log.e(TileImageView.TAG, "<TileQueue.push> push tile same as head, return, tile = " + tile);
                return z;
            }
            tile.mNext = this.mHead;
            this.mHead = tile;
            return z;
        }

        public void clean() {
            this.mHead = null;
        }
    }

    private class TileDecoder extends Thread {
        private TileDecoder() {
        }

        @Override
        public void run() {
            Tile tilePop;
            while (!isInterrupted()) {
                synchronized (TileImageView.this) {
                    tilePop = TileImageView.this.mDecodeQueue.pop();
                    if (tilePop == null && !isInterrupted()) {
                        Log.d(TileImageView.TAG, "<TileDecoder.run> wait, this = " + this);
                        try {
                            TileImageView.this.wait();
                        } catch (InterruptedException e) {
                            interrupt();
                        }
                    }
                }
                if (tilePop != null) {
                    Log.d(TileImageView.TAG, "<TileDecoder.run> decodeTile, this = " + this + ", tile = " + tilePop);
                    if (TileImageView.this.decodeTile(tilePop)) {
                        TileImageView.this.queueForUpload(tilePop);
                    }
                }
            }
            Log.d(TileImageView.TAG, "<TileDecoder.run> exit, this = " + this);
        }
    }
}
