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

    public SparseArrayBitmapPool(int capacityBytes, Pools.Pool<Node> nodePool) {
        this.mCapacityBytes = capacityBytes;
        if (nodePool == null) {
            this.mNodePool = new Pools.SimplePool(32);
        } else {
            this.mNodePool = nodePool;
        }
    }

    private void freeUpCapacity(int bytesNeeded) {
        int targetSize = this.mCapacityBytes - bytesNeeded;
        while (this.mPoolNodesTail != null && this.mSizeBytes > targetSize) {
            unlinkAndRecycleNode(this.mPoolNodesTail, true);
        }
    }

    private void unlinkAndRecycleNode(Node n, boolean recycleBitmap) {
        if (n.prevInBucket != null) {
            n.prevInBucket.nextInBucket = n.nextInBucket;
        } else {
            this.mStore.put(n.bitmap.getWidth(), n.nextInBucket);
        }
        if (n.nextInBucket != null) {
            n.nextInBucket.prevInBucket = n.prevInBucket;
        }
        if (n.prevInPool != null) {
            n.prevInPool.nextInPool = n.nextInPool;
        } else {
            this.mPoolNodesHead = n.nextInPool;
        }
        if (n.nextInPool != null) {
            n.nextInPool.prevInPool = n.prevInPool;
        } else {
            this.mPoolNodesTail = n.prevInPool;
        }
        n.nextInBucket = null;
        n.nextInPool = null;
        n.prevInBucket = null;
        n.prevInPool = null;
        this.mSizeBytes -= n.bitmap.getByteCount();
        if (recycleBitmap) {
            n.bitmap.recycle();
        }
        n.bitmap = null;
        this.mNodePool.release(n);
    }

    public synchronized Bitmap get(int width, int height) {
        Bitmap b;
        Node cur = this.mStore.get(width);
        while (true) {
            if (cur != null) {
                if (cur.bitmap.getHeight() == height) {
                    break;
                }
                cur = cur.nextInBucket;
            } else {
                b = null;
                break;
            }
        }
        return b;
    }

    public synchronized boolean put(Bitmap b) {
        boolean z;
        if (b == null) {
            z = false;
        } else {
            int bytes = b.getByteCount();
            freeUpCapacity(bytes);
            Node newNode = this.mNodePool.acquire();
            if (newNode == null) {
                newNode = new Node();
            }
            newNode.bitmap = b;
            newNode.prevInBucket = null;
            newNode.prevInPool = null;
            newNode.nextInPool = this.mPoolNodesHead;
            this.mPoolNodesHead = newNode;
            int key = b.getWidth();
            newNode.nextInBucket = this.mStore.get(key);
            if (newNode.nextInBucket != null) {
                newNode.nextInBucket.prevInBucket = newNode;
            }
            this.mStore.put(key, newNode);
            if (newNode.nextInPool == null) {
                this.mPoolNodesTail = newNode;
            } else {
                newNode.nextInPool.prevInPool = newNode;
            }
            this.mSizeBytes += bytes;
            z = true;
        }
        return z;
    }

    public synchronized void clear() {
        freeUpCapacity(this.mCapacityBytes);
    }
}
