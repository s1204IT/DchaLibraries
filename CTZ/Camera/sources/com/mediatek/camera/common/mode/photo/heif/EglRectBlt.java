package com.mediatek.camera.common.mode.photo.heif;

import android.graphics.Rect;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class EglRectBlt {
    private Texture2dProgram mProgram;
    private final int mTexHeight;
    private final int mTexWidth;
    private static final float[] FULL_RECTANGLE_COORDS = {-1.0f, -1.0f, 1.0f, -1.0f, -1.0f, 1.0f, 1.0f, 1.0f};
    private static final FloatBuffer FULL_RECTANGLE_BUF = createFloatBuffer(FULL_RECTANGLE_COORDS);
    private final float[] mTexCoords = new float[8];
    private final FloatBuffer mTexCoordArray = createFloatBuffer(this.mTexCoords);

    public static FloatBuffer createFloatBuffer(float[] fArr) {
        ByteBuffer byteBufferAllocateDirect = ByteBuffer.allocateDirect(fArr.length * 4);
        byteBufferAllocateDirect.order(ByteOrder.nativeOrder());
        FloatBuffer floatBufferAsFloatBuffer = byteBufferAllocateDirect.asFloatBuffer();
        floatBufferAsFloatBuffer.put(fArr);
        floatBufferAsFloatBuffer.position(0);
        return floatBufferAsFloatBuffer;
    }

    public EglRectBlt(Texture2dProgram texture2dProgram, int i, int i2) {
        this.mProgram = texture2dProgram;
        this.mTexWidth = i;
        this.mTexHeight = i2;
    }

    public void release(boolean z) {
        if (this.mProgram != null) {
            if (z) {
                this.mProgram.release();
            }
            this.mProgram = null;
        }
    }

    public int createTextureObject() {
        return this.mProgram.createTextureObject();
    }

    public void copyRect(int i, float[] fArr, Rect rect) {
        setTexRect(rect);
        this.mProgram.draw(Texture2dProgram.IDENTITY_MATRIX, FULL_RECTANGLE_BUF, 0, 4, 2, 8, fArr, this.mTexCoordArray, i, 8);
    }

    void setTexRect(Rect rect) {
        this.mTexCoords[0] = rect.left / this.mTexWidth;
        this.mTexCoords[1] = 1.0f - (rect.bottom / this.mTexHeight);
        this.mTexCoords[2] = rect.right / this.mTexWidth;
        this.mTexCoords[3] = 1.0f - (rect.bottom / this.mTexHeight);
        this.mTexCoords[4] = rect.left / this.mTexWidth;
        this.mTexCoords[5] = 1.0f - (rect.top / this.mTexHeight);
        this.mTexCoords[6] = rect.right / this.mTexWidth;
        this.mTexCoords[7] = 1.0f - (rect.top / this.mTexHeight);
        this.mTexCoordArray.put(this.mTexCoords);
        this.mTexCoordArray.position(0);
    }
}
