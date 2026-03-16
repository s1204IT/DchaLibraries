package com.android.gallery3d.glrenderer;

import com.android.gallery3d.common.Utils;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

class NinePatchInstance {
    private int mIdxCount;
    private ByteBuffer mIndexBuffer;
    private int mIndexBufferName;
    private FloatBuffer mUvBuffer;
    private int mUvBufferName;
    private FloatBuffer mXyBuffer;
    private int mXyBufferName = -1;

    public NinePatchInstance(NinePatchTexture tex, int width, int height) {
        NinePatchChunk chunk = tex.getNinePatchChunk();
        if (width <= 0 || height <= 0) {
            throw new RuntimeException("invalid dimension");
        }
        if (chunk.mDivX.length != 2 || chunk.mDivY.length != 2) {
            throw new RuntimeException("unsupported nine patch");
        }
        float[] divX = new float[4];
        float[] divY = new float[4];
        float[] divU = new float[4];
        float[] divV = new float[4];
        int nx = stretch(divX, divU, chunk.mDivX, tex.getWidth(), width);
        int ny = stretch(divY, divV, chunk.mDivY, tex.getHeight(), height);
        prepareVertexData(divX, divY, divU, divV, nx, ny, chunk.mColor);
    }

    private static int stretch(float[] x, float[] u, int[] div, int source, int target) {
        int textureSize = Utils.nextPowerOf2(source);
        float textureBound = source / textureSize;
        float stretch = 0.0f;
        int n = div.length;
        for (int i = 0; i < n; i += 2) {
            stretch += div[i + 1] - div[i];
        }
        float remaining = (target - source) + stretch;
        float lastX = 0.0f;
        float lastU = 0.0f;
        x[0] = 0.0f;
        u[0] = 0.0f;
        int n2 = div.length;
        for (int i2 = 0; i2 < n2; i2 += 2) {
            x[i2 + 1] = (div[i2] - lastU) + lastX + 0.5f;
            u[i2 + 1] = Math.min((div[i2] + 0.5f) / textureSize, textureBound);
            float partU = div[i2 + 1] - div[i2];
            float partX = (remaining * partU) / stretch;
            remaining -= partX;
            stretch -= partU;
            lastX = x[i2 + 1] + partX;
            lastU = div[i2 + 1];
            x[i2 + 2] = lastX - 0.5f;
            u[i2 + 2] = Math.min((lastU - 0.5f) / textureSize, textureBound);
        }
        x[div.length + 1] = target;
        u[div.length + 1] = textureBound;
        int last = 0;
        int n3 = div.length + 2;
        for (int i3 = 1; i3 < n3; i3++) {
            if (x[i3] - x[last] >= 1.0f) {
                last++;
                x[last] = x[i3];
                u[last] = u[i3];
            }
        }
        return last + 1;
    }

    private void prepareVertexData(float[] x, float[] y, float[] u, float[] v, int nx, int ny, int[] color) {
        int start;
        int end;
        int inc;
        int pntCount;
        int pntCount2 = 0;
        float[] xy = new float[32];
        float[] uv = new float[32];
        int j = 0;
        while (j < ny) {
            int i = 0;
            while (true) {
                pntCount = pntCount2;
                if (i < nx) {
                    pntCount2 = pntCount + 1;
                    int xIndex = pntCount << 1;
                    int yIndex = xIndex + 1;
                    xy[xIndex] = x[i];
                    xy[yIndex] = y[j];
                    uv[xIndex] = u[i];
                    uv[yIndex] = v[j];
                    i++;
                }
            }
            j++;
            pntCount2 = pntCount;
        }
        int idxCount = 1;
        boolean isForward = false;
        byte[] index = new byte[24];
        for (int row = 0; row < ny - 1; row++) {
            idxCount--;
            isForward = !isForward;
            if (isForward) {
                start = 0;
                end = nx;
                inc = 1;
            } else {
                start = nx - 1;
                end = -1;
                inc = -1;
            }
            for (int col = start; col != end; col += inc) {
                int k = (row * nx) + col;
                if (col != start) {
                    int colorIdx = ((nx - 1) * row) + col;
                    if (isForward) {
                        colorIdx--;
                    }
                    if (color[colorIdx] == 0) {
                        index[idxCount] = index[idxCount - 1];
                        int idxCount2 = idxCount + 1;
                        index[idxCount2] = (byte) k;
                        idxCount = idxCount2 + 1;
                    }
                }
                int idxCount3 = idxCount + 1;
                index[idxCount] = (byte) k;
                idxCount = idxCount3 + 1;
                index[idxCount3] = (byte) (k + nx);
            }
        }
        this.mIdxCount = idxCount;
        int size = pntCount2 * 2 * 4;
        this.mXyBuffer = allocateDirectNativeOrderBuffer(size).asFloatBuffer();
        this.mUvBuffer = allocateDirectNativeOrderBuffer(size).asFloatBuffer();
        this.mIndexBuffer = allocateDirectNativeOrderBuffer(this.mIdxCount);
        this.mXyBuffer.put(xy, 0, pntCount2 * 2).position(0);
        this.mUvBuffer.put(uv, 0, pntCount2 * 2).position(0);
        this.mIndexBuffer.put(index, 0, idxCount).position(0);
    }

    private static ByteBuffer allocateDirectNativeOrderBuffer(int size) {
        return ByteBuffer.allocateDirect(size).order(ByteOrder.nativeOrder());
    }

    private void prepareBuffers(GLCanvas canvas) {
        this.mXyBufferName = canvas.uploadBuffer(this.mXyBuffer);
        this.mUvBufferName = canvas.uploadBuffer(this.mUvBuffer);
        this.mIndexBufferName = canvas.uploadBuffer(this.mIndexBuffer);
        this.mXyBuffer = null;
        this.mUvBuffer = null;
        this.mIndexBuffer = null;
    }

    public void draw(GLCanvas canvas, NinePatchTexture tex, int x, int y) {
        if (this.mXyBufferName == -1) {
            prepareBuffers(canvas);
        }
        canvas.drawMesh(tex, x, y, this.mXyBufferName, this.mUvBufferName, this.mIndexBufferName, this.mIdxCount);
    }

    public void recycle(GLCanvas canvas) {
        if (this.mXyBuffer == null) {
            canvas.deleteBuffer(this.mXyBufferName);
            canvas.deleteBuffer(this.mUvBufferName);
            canvas.deleteBuffer(this.mIndexBufferName);
            this.mXyBufferName = -1;
        }
    }
}
