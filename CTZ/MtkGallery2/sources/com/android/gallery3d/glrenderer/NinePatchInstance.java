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

    public NinePatchInstance(NinePatchTexture ninePatchTexture, int i, int i2) {
        NinePatchChunk ninePatchChunk = ninePatchTexture.getNinePatchChunk();
        if (i <= 0 || i2 <= 0) {
            throw new RuntimeException("invalid dimension");
        }
        if (ninePatchChunk.mDivX.length != 2 || ninePatchChunk.mDivY.length != 2) {
            throw new RuntimeException("unsupported nine patch");
        }
        float[] fArr = new float[4];
        float[] fArr2 = new float[4];
        float[] fArr3 = new float[4];
        float[] fArr4 = new float[4];
        prepareVertexData(fArr, fArr2, fArr3, fArr4, stretch(fArr, fArr3, ninePatchChunk.mDivX, ninePatchTexture.getWidth(), i), stretch(fArr2, fArr4, ninePatchChunk.mDivY, ninePatchTexture.getHeight(), i2), ninePatchChunk.mColor);
    }

    private static int stretch(float[] fArr, float[] fArr2, int[] iArr, int i, int i2) {
        float fNextPowerOf2 = Utils.nextPowerOf2(i);
        float f = i / fNextPowerOf2;
        int i3 = 0;
        float f2 = 0.0f;
        float f3 = 0.0f;
        for (int i4 = 0; i4 < iArr.length; i4 += 2) {
            f3 += iArr[i4 + 1] - iArr[i4];
        }
        fArr[0] = 0.0f;
        fArr2[0] = 0.0f;
        int length = iArr.length;
        float f4 = 0.0f;
        float f5 = f3;
        float f6 = (i2 - i) + f3;
        int i5 = 0;
        while (i5 < length) {
            int i6 = i5 + 1;
            fArr[i6] = f2 + (iArr[i5] - f4) + 0.5f;
            fArr2[i6] = Math.min((iArr[i5] + 0.5f) / fNextPowerOf2, f);
            float f7 = iArr[i6] - iArr[i5];
            float f8 = (f6 * f7) / f5;
            f6 -= f8;
            f5 -= f7;
            f2 = fArr[i6] + f8;
            float f9 = iArr[i6];
            i5 += 2;
            fArr[i5] = f2 - 0.5f;
            fArr2[i5] = Math.min((f9 - 0.5f) / fNextPowerOf2, f);
            f4 = f9;
        }
        fArr[iArr.length + 1] = i2;
        fArr2[iArr.length + 1] = f;
        int length2 = iArr.length + 2;
        for (int i7 = 1; i7 < length2; i7++) {
            if (fArr[i7] - fArr[i3] >= 1.0f) {
                i3++;
                fArr[i3] = fArr[i7];
                fArr2[i3] = fArr2[i7];
            }
        }
        return i3 + 1;
    }

    private void prepareVertexData(float[] fArr, float[] fArr2, float[] fArr3, float[] fArr4, int i, int i2, int[] iArr) {
        int i3;
        int i4;
        int i5 = i2;
        float[] fArr5 = new float[32];
        float[] fArr6 = new float[32];
        int i6 = 0;
        int i7 = 0;
        while (i6 < i5) {
            int i8 = i7;
            int i9 = 0;
            while (i9 < i) {
                int i10 = i8 + 1;
                int i11 = i8 << 1;
                int i12 = i11 + 1;
                fArr5[i11] = fArr[i9];
                fArr5[i12] = fArr2[i6];
                fArr6[i11] = fArr3[i9];
                fArr6[i12] = fArr4[i6];
                i9++;
                i8 = i10;
            }
            i6++;
            i7 = i8;
        }
        byte[] bArr = new byte[24];
        int i13 = 1;
        int i14 = 1;
        int i15 = 0;
        int i16 = 0;
        while (i15 < i5 - 1) {
            int i17 = i14 - 1;
            i16 ^= i13;
            int i18 = -1;
            if (i16 == 0) {
                i3 = i - 1;
                i4 = -1;
            } else {
                i18 = i;
                i4 = i13;
                i3 = 0;
            }
            int i19 = i17;
            for (int i20 = i3; i20 != i18; i20 += i4) {
                int i21 = (i15 * i) + i20;
                if (i20 != i3) {
                    int i22 = ((i - 1) * i15) + i20;
                    if (i16 != 0) {
                        i22--;
                    }
                    if (iArr[i22] == 0) {
                        bArr[i19] = bArr[i19 - 1];
                        int i23 = i19 + 1;
                        bArr[i23] = (byte) i21;
                        i19 = i23 + 1;
                    }
                }
                int i24 = i19 + 1;
                bArr[i19] = (byte) i21;
                i19 = i24 + 1;
                bArr[i24] = (byte) (i21 + i);
            }
            i15++;
            i14 = i19;
            i5 = i2;
            i13 = 1;
        }
        this.mIdxCount = i14;
        int i25 = i7 * 2;
        int i26 = i25 * 4;
        this.mXyBuffer = allocateDirectNativeOrderBuffer(i26).asFloatBuffer();
        this.mUvBuffer = allocateDirectNativeOrderBuffer(i26).asFloatBuffer();
        this.mIndexBuffer = allocateDirectNativeOrderBuffer(this.mIdxCount);
        this.mXyBuffer.put(fArr5, 0, i25).position(0);
        this.mUvBuffer.put(fArr6, 0, i25).position(0);
        this.mIndexBuffer.put(bArr, 0, i14).position(0);
    }

    private static ByteBuffer allocateDirectNativeOrderBuffer(int i) {
        return ByteBuffer.allocateDirect(i).order(ByteOrder.nativeOrder());
    }

    private void prepareBuffers(GLCanvas gLCanvas) {
        this.mXyBufferName = gLCanvas.uploadBuffer(this.mXyBuffer);
        this.mUvBufferName = gLCanvas.uploadBuffer(this.mUvBuffer);
        this.mIndexBufferName = gLCanvas.uploadBuffer(this.mIndexBuffer);
        this.mXyBuffer = null;
        this.mUvBuffer = null;
        this.mIndexBuffer = null;
    }

    public void draw(GLCanvas gLCanvas, NinePatchTexture ninePatchTexture, int i, int i2) {
        if (this.mXyBufferName == -1) {
            prepareBuffers(gLCanvas);
        }
        gLCanvas.drawMesh(ninePatchTexture, i, i2, this.mXyBufferName, this.mUvBufferName, this.mIndexBufferName, this.mIdxCount);
    }

    public void recycle(GLCanvas gLCanvas) {
        if (this.mXyBuffer == null) {
            gLCanvas.deleteBuffer(this.mXyBufferName);
            gLCanvas.deleteBuffer(this.mUvBufferName);
            gLCanvas.deleteBuffer(this.mIndexBufferName);
            this.mXyBufferName = -1;
        }
    }
}
