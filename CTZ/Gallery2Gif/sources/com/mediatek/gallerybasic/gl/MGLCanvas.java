package com.mediatek.gallerybasic.gl;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.RectF;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

public interface MGLCanvas {
    public static final int SAVE_FLAG_ALL = -1;
    public static final int SAVE_FLAG_ALPHA = 1;
    public static final int SAVE_FLAG_MATRIX = 2;

    public interface Generator {
        int generateTexture();

        void glGenBuffers(int i, int[] iArr, int i2);
    }

    void beginRenderTarget(MRawTexture mRawTexture);

    void clearBuffer();

    void clearBuffer(float[] fArr);

    void deleteBuffer(int i);

    void deleteRecycledResources();

    void drawMesh(MBasicTexture mBasicTexture, float f, float f2, float f3, float f4, int i, int i2, int i3);

    void drawMesh(MBasicTexture mBasicTexture, int i, int i2, int i3, int i4, int i5, int i6);

    void drawMixed(MBasicTexture mBasicTexture, int i, float f, int i2, int i3, int i4, int i5);

    void drawMixed(MBasicTexture mBasicTexture, int i, float f, RectF rectF, RectF rectF2);

    void drawTexture(MBasicTexture mBasicTexture, int i, int i2, int i3, int i4);

    void drawTexture(MBasicTexture mBasicTexture, RectF rectF, RectF rectF2);

    void drawTexture(MBasicTexture mBasicTexture, float[] fArr, int i, int i2, int i3, int i4);

    void dumpStatisticsAndClear();

    void endRenderTarget();

    void fillRect(float f, float f2, float f3, float f4, int i);

    int generateTexture();

    float getAlpha();

    void getBounds(Rect rect, int i, int i2, int i3, int i4);

    int getGLVersion();

    int getHeight();

    int getWidth();

    void glDeleteBuffers(int i, int[] iArr, int i2);

    void glDeleteFramebuffers(int i, int[] iArr, int i2);

    void glDeleteTextures(int i, int[] iArr, int i2);

    void glGenBuffers(int i, int[] iArr, int i2);

    void initializeTexture(MBasicTexture mBasicTexture, Bitmap bitmap);

    void initializeTextureSize(MBasicTexture mBasicTexture, int i, int i2);

    void multiplyAlpha(float f);

    void multiplyMatrix(float[] fArr, int i);

    void readPixels(int i, int i2, int i3, int i4, int i5, int i6, Buffer buffer);

    void recoverFromLightCycle();

    void restore();

    void rotate(float f, float f2, float f3, float f4);

    void save();

    void save(int i);

    Bitmap saveTexture(int i, int i2, int i3);

    void scale(float f, float f2, float f3);

    void setAlpha(float f);

    void setGenerator(Generator generator);

    void setLookAt(float f, float f2, float f3, float f4, float f5, float f6, float f7, float f8, float f9);

    void setPerspective(float f, float f2, float f3, float f4);

    void setSize(int i, int i2);

    void setTextureParameters(MBasicTexture mBasicTexture);

    void texSubImage2D(MBasicTexture mBasicTexture, int i, int i2, Bitmap bitmap, int i3, int i4);

    void translate(float f, float f2);

    void translate(float f, float f2, float f3);

    boolean unloadTexture(MBasicTexture mBasicTexture);

    int uploadBuffer(ByteBuffer byteBuffer);

    int uploadBuffer(FloatBuffer floatBuffer);
}
