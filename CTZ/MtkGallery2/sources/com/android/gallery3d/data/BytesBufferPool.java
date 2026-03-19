package com.android.gallery3d.data;

import java.util.ArrayList;

public class BytesBufferPool {
    private final int mBufferSize;
    private final ArrayList<BytesBuffer> mList;
    private final int mPoolSize;

    public static class BytesBuffer {
        public byte[] data;
        public int length;
        public int offset;

        private BytesBuffer(int i) {
            this.data = new byte[i];
        }
    }

    public BytesBufferPool(int i, int i2) {
        this.mList = new ArrayList<>(i);
        this.mPoolSize = i;
        this.mBufferSize = i2;
    }

    public synchronized BytesBuffer get() {
        int size;
        size = this.mList.size();
        return size > 0 ? this.mList.remove(size - 1) : new BytesBuffer(this.mBufferSize);
    }

    public synchronized void recycle(BytesBuffer bytesBuffer) {
        if (bytesBuffer.data.length != this.mBufferSize) {
            return;
        }
        if (this.mList.size() < this.mPoolSize) {
            bytesBuffer.offset = 0;
            bytesBuffer.length = 0;
            this.mList.add(bytesBuffer);
        }
    }

    public synchronized void clear() {
        this.mList.clear();
    }
}
