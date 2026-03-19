package com.android.photos.data;

import android.graphics.Bitmap;
import android.util.Pools;
import android.util.SparseArray;

public class SparseArrayBitmapPool {
    private int mCapacityBytes;
    private Pools.Pool<Node> mNodePool;
    private SparseArray<Node> mStore = new SparseArray<>();
    private int mSizeBytes = 0;
    private Node mPoolNodesHead = null;
    private Node mPoolNodesTail = null;

    protected static class Node {
        Bitmap bitmap;
        Node nextInBucket;
        Node nextInPool;
        Node prevInBucket;
        Node prevInPool;

        protected Node() {
        }
    }

    public SparseArrayBitmapPool(int i, Pools.Pool<Node> pool) {
        this.mCapacityBytes = i;
        if (pool == null) {
            this.mNodePool = new Pools.SimplePool(32);
        } else {
            this.mNodePool = pool;
        }
    }

    private void freeUpCapacity(int i) {
        int i2 = this.mCapacityBytes - i;
        while (this.mPoolNodesTail != null && this.mSizeBytes > i2) {
            unlinkAndRecycleNode(this.mPoolNodesTail, true);
        }
    }

    private void unlinkAndRecycleNode(Node node, boolean z) {
        if (node.prevInBucket != null) {
            node.prevInBucket.nextInBucket = node.nextInBucket;
        } else {
            this.mStore.put(node.bitmap.getWidth(), node.nextInBucket);
        }
        if (node.nextInBucket != null) {
            node.nextInBucket.prevInBucket = node.prevInBucket;
        }
        if (node.prevInPool != null) {
            node.prevInPool.nextInPool = node.nextInPool;
        } else {
            this.mPoolNodesHead = node.nextInPool;
        }
        if (node.nextInPool != null) {
            node.nextInPool.prevInPool = node.prevInPool;
        } else {
            this.mPoolNodesTail = node.prevInPool;
        }
        node.nextInBucket = null;
        node.nextInPool = null;
        node.prevInBucket = null;
        node.prevInPool = null;
        this.mSizeBytes -= node.bitmap.getByteCount();
        if (z) {
            node.bitmap.recycle();
        }
        node.bitmap = null;
        this.mNodePool.release(node);
    }

    public synchronized Bitmap get(int i, int i2) {
        for (Node node = this.mStore.get(i); node != null; node = node.nextInBucket) {
            if (node.bitmap.getHeight() == i2) {
                Bitmap bitmap = node.bitmap;
                unlinkAndRecycleNode(node, false);
                return bitmap;
            }
        }
        return null;
    }

    public synchronized boolean put(Bitmap bitmap) {
        if (bitmap == null) {
            return false;
        }
        int byteCount = bitmap.getByteCount();
        freeUpCapacity(byteCount);
        Node node = (Node) this.mNodePool.acquire();
        if (node == null) {
            node = new Node();
        }
        node.bitmap = bitmap;
        node.prevInBucket = null;
        node.prevInPool = null;
        node.nextInPool = this.mPoolNodesHead;
        this.mPoolNodesHead = node;
        int width = bitmap.getWidth();
        node.nextInBucket = this.mStore.get(width);
        if (node.nextInBucket != null) {
            node.nextInBucket.prevInBucket = node;
        }
        this.mStore.put(width, node);
        if (node.nextInPool == null) {
            this.mPoolNodesTail = node;
        } else {
            node.nextInPool.prevInPool = node;
        }
        this.mSizeBytes += byteCount;
        return true;
    }

    public synchronized void clear() {
        freeUpCapacity(this.mCapacityBytes);
    }
}
