package com.android.gallery3d.glrenderer;

import android.graphics.Rect;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

class NinePatchChunk {
    public int[] mColor;
    public int[] mDivX;
    public int[] mDivY;
    public Rect mPaddings = new Rect();

    NinePatchChunk() {
    }

    private static void readIntArray(int[] iArr, ByteBuffer byteBuffer) {
        int length = iArr.length;
        for (int i = 0; i < length; i++) {
            iArr[i] = byteBuffer.getInt();
        }
    }

    private static void checkDivCount(int i) {
        if (i == 0 || (i & 1) != 0) {
            throw new RuntimeException("invalid nine-patch: " + i);
        }
    }

    public static NinePatchChunk deserialize(byte[] bArr) {
        ByteBuffer byteBufferOrder = ByteBuffer.wrap(bArr).order(ByteOrder.nativeOrder());
        if (byteBufferOrder.get() == 0) {
            return null;
        }
        NinePatchChunk ninePatchChunk = new NinePatchChunk();
        ninePatchChunk.mDivX = new int[byteBufferOrder.get()];
        ninePatchChunk.mDivY = new int[byteBufferOrder.get()];
        ninePatchChunk.mColor = new int[byteBufferOrder.get()];
        checkDivCount(ninePatchChunk.mDivX.length);
        checkDivCount(ninePatchChunk.mDivY.length);
        byteBufferOrder.getInt();
        byteBufferOrder.getInt();
        ninePatchChunk.mPaddings.left = byteBufferOrder.getInt();
        ninePatchChunk.mPaddings.right = byteBufferOrder.getInt();
        ninePatchChunk.mPaddings.top = byteBufferOrder.getInt();
        ninePatchChunk.mPaddings.bottom = byteBufferOrder.getInt();
        byteBufferOrder.getInt();
        readIntArray(ninePatchChunk.mDivX, byteBufferOrder);
        readIntArray(ninePatchChunk.mDivY, byteBufferOrder);
        readIntArray(ninePatchChunk.mColor, byteBufferOrder);
        return ninePatchChunk;
    }
}
