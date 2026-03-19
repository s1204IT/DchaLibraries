package com.android.gallery3d.glrenderer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import com.mediatek.gallery3d.layout.FancyHelper;

public class NinePatchTexture extends ResourceTexture {
    private NinePatchChunk mChunk;
    private SmallCache<NinePatchInstance> mInstanceCache;

    public NinePatchTexture(Context context, int i) {
        super(context, i);
        this.mInstanceCache = new SmallCache<>();
    }

    @Override
    protected Bitmap onGetBitmap() {
        NinePatchChunk ninePatchChunkDeserialize;
        if (this.mBitmap != null) {
            return this.mBitmap;
        }
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        Bitmap bitmapDecodeResource = BitmapFactory.decodeResource(this.mContext.getResources(), this.mResId, options);
        this.mBitmap = bitmapDecodeResource;
        setSize(bitmapDecodeResource.getWidth(), bitmapDecodeResource.getHeight());
        if (bitmapDecodeResource.getNinePatchChunk() == null) {
            ninePatchChunkDeserialize = null;
        } else {
            ninePatchChunkDeserialize = NinePatchChunk.deserialize(bitmapDecodeResource.getNinePatchChunk());
        }
        this.mChunk = ninePatchChunkDeserialize;
        if (this.mChunk == null) {
            throw new RuntimeException("invalid nine-patch image: " + this.mResId);
        }
        return bitmapDecodeResource;
    }

    public Rect getPaddings() {
        if (this.mChunk == null) {
            onGetBitmap();
        }
        return this.mChunk.mPaddings;
    }

    public NinePatchChunk getNinePatchChunk() {
        if (this.mChunk == null) {
            onGetBitmap();
        }
        return this.mChunk;
    }

    private static class SmallCache<V> {
        private int mCount;
        private int[] mKey;
        private V[] mValue;

        private SmallCache() {
            this.mKey = new int[16];
            this.mValue = (V[]) new Object[16];
        }

        public V put(int i, V v) {
            if (this.mCount == 16) {
                V v2 = this.mValue[15];
                this.mKey[15] = i;
                this.mValue[15] = v;
                return v2;
            }
            this.mKey[this.mCount] = i;
            this.mValue[this.mCount] = v;
            this.mCount++;
            return null;
        }

        public V get(int i) {
            for (int i2 = 0; i2 < this.mCount; i2++) {
                if (this.mKey[i2] == i) {
                    if (this.mCount > 8 && i2 > 0) {
                        int i3 = this.mKey[i2];
                        int i4 = i2 - 1;
                        this.mKey[i2] = this.mKey[i4];
                        this.mKey[i4] = i3;
                        V v = this.mValue[i2];
                        this.mValue[i2] = this.mValue[i4];
                        this.mValue[i4] = v;
                        if (FancyHelper.isFancyLayoutSupported()) {
                            return this.mValue[i4];
                        }
                    }
                    return this.mValue[i2];
                }
            }
            return null;
        }

        public void clear() {
            for (int i = 0; i < this.mCount; i++) {
                this.mValue[i] = null;
            }
            this.mCount = 0;
        }

        public int size() {
            return this.mCount;
        }

        public V valueAt(int i) {
            return this.mValue[i];
        }
    }

    private NinePatchInstance findInstance(GLCanvas gLCanvas, int i, int i2) {
        NinePatchInstance ninePatchInstancePut;
        int i3 = (i << 16) | i2;
        NinePatchInstance ninePatchInstance = this.mInstanceCache.get(i3);
        if (ninePatchInstance == null && (ninePatchInstancePut = this.mInstanceCache.put(i3, (ninePatchInstance = new NinePatchInstance(this, i, i2)))) != null) {
            ninePatchInstancePut.recycle(gLCanvas);
        }
        return ninePatchInstance;
    }

    @Override
    public void draw(GLCanvas gLCanvas, int i, int i2, int i3, int i4) {
        if (!isLoaded()) {
            this.mInstanceCache.clear();
        }
        if (i3 != 0 && i4 != 0) {
            findInstance(gLCanvas, i3, i4).draw(gLCanvas, this, i, i2);
        }
    }

    @Override
    public void recycle() {
        super.recycle();
        GLCanvas gLCanvas = this.mCanvasRef;
        if (gLCanvas == null) {
            return;
        }
        int size = this.mInstanceCache.size();
        for (int i = 0; i < size; i++) {
            this.mInstanceCache.valueAt(i).recycle(gLCanvas);
        }
        this.mInstanceCache.clear();
    }
}
