package com.android.gallery3d.glrenderer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;

public class NinePatchTexture extends ResourceTexture {
    private NinePatchChunk mChunk;
    private SmallCache<NinePatchInstance> mInstanceCache;

    public NinePatchTexture(Context context, int resId) {
        super(context, resId);
        this.mInstanceCache = new SmallCache<>();
    }

    @Override
    protected Bitmap onGetBitmap() {
        if (this.mBitmap != null) {
            return this.mBitmap;
        }
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        Bitmap bitmap = BitmapFactory.decodeResource(this.mContext.getResources(), this.mResId, options);
        this.mBitmap = bitmap;
        setSize(bitmap.getWidth(), bitmap.getHeight());
        byte[] chunkData = bitmap.getNinePatchChunk();
        this.mChunk = chunkData == null ? null : NinePatchChunk.deserialize(bitmap.getNinePatchChunk());
        if (this.mChunk == null) {
            throw new RuntimeException("invalid nine-patch image: " + this.mResId);
        }
        return bitmap;
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

        public V put(int key, V value) {
            if (this.mCount == 16) {
                V old = this.mValue[15];
                this.mKey[15] = key;
                this.mValue[15] = value;
                return old;
            }
            this.mKey[this.mCount] = key;
            this.mValue[this.mCount] = value;
            this.mCount++;
            return null;
        }

        public V get(int key) {
            for (int i = 0; i < this.mCount; i++) {
                if (this.mKey[i] == key) {
                    if (this.mCount > 8 && i > 0) {
                        int tmpKey = this.mKey[i];
                        this.mKey[i] = this.mKey[i - 1];
                        this.mKey[i - 1] = tmpKey;
                        V tmpValue = this.mValue[i];
                        this.mValue[i] = this.mValue[i - 1];
                        this.mValue[i - 1] = tmpValue;
                    }
                    return this.mValue[i];
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

    private NinePatchInstance findInstance(GLCanvas canvas, int w, int h) {
        NinePatchInstance removed;
        int key = (w << 16) | h;
        NinePatchInstance instance = this.mInstanceCache.get(key);
        if (instance == null && (removed = this.mInstanceCache.put(key, (instance = new NinePatchInstance(this, w, h)))) != null) {
            removed.recycle(canvas);
        }
        return instance;
    }

    @Override
    public void draw(GLCanvas canvas, int x, int y, int w, int h) {
        if (!isLoaded()) {
            this.mInstanceCache.clear();
        }
        if (w != 0 && h != 0) {
            findInstance(canvas, w, h).draw(canvas, this, x, y);
        }
    }

    @Override
    public void recycle() {
        super.recycle();
        GLCanvas canvas = this.mCanvasRef;
        if (canvas != null) {
            int n = this.mInstanceCache.size();
            for (int i = 0; i < n; i++) {
                NinePatchInstance instance = this.mInstanceCache.valueAt(i);
                instance.recycle(canvas);
            }
            this.mInstanceCache.clear();
        }
    }
}
