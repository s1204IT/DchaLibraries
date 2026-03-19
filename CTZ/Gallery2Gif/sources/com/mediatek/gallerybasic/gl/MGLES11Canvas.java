package com.mediatek.gallerybasic.gl;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.RectF;
import android.opengl.GLU;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import com.mediatek.gallerybasic.gl.MGLCanvas;
import com.mediatek.gallerybasic.util.Log;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import javax.microedition.khronos.opengles.GL11;
import javax.microedition.khronos.opengles.GL11Ext;
import javax.microedition.khronos.opengles.GL11ExtensionPack;

public class MGLES11Canvas implements MGLCanvas {
    private static final int INVALID_FRAME_BUFFER = -1;
    private static final int MSCALE_X = 0;
    private static final int MSCALE_Y = 5;
    private static final int MSKEW_X = 4;
    private static final int MSKEW_Y = 1;
    private static final int OFFSET_DRAW_LINE = 4;
    private static final int OFFSET_DRAW_RECT = 6;
    private static final int OFFSET_FILL_RECT = 0;
    private static final float OPAQUE_ALPHA = 0.95f;
    private static final String TAG = "MtkGallery2/MGLES11Canvas";
    private float mAlpha;
    private int mBoxCoords;
    int mCountDrawLine;
    int mCountDrawMesh;
    int mCountFillRect;
    int mCountTextureOES;
    int mCountTextureRect;
    private GL11 mGL;
    private GLState mGLState;
    private ConfigState mRecycledRestoreAction;
    private int mScreenHeight;
    private int mScreenWidth;
    private MRawTexture mTargetTexture;
    private static final float[] BOX_COORDINATES = {0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f, 1.0f, 0.0f};
    private static float[] sCropRect = new float[4];
    private final float[] mMatrixValues = new float[16];
    private final float[] mTextureMatrixValues = new float[16];
    private final float[] mMapPointsBuffer = new float[4];
    private final float[] mTextureColor = new float[4];
    private final ArrayList<MRawTexture> mTargetStack = new ArrayList<>();
    private final ArrayList<ConfigState> mRestoreStack = new ArrayList<>();
    private final RectF mDrawTextureSourceRect = new RectF();
    private final RectF mDrawTextureTargetRect = new RectF();
    private final float[] mTempMatrix = new float[32];
    private final IntArray mUnboundTextures = new IntArray();
    private final IntArray mDeleteBuffers = new IntArray();
    private boolean mBlendEnabled = true;
    private int[] mFrameBuffer = new int[1];
    MGLCanvas.Generator mGenerator = new GLES11Generator();

    public MGLES11Canvas(GL11 gl11, boolean z) {
        this.mGL = gl11;
        if (z) {
            this.mGLState = new GLState(gl11);
            FloatBuffer floatBufferAsFloatBuffer = allocateDirectNativeOrderBuffer((BOX_COORDINATES.length * 32) / 8).asFloatBuffer();
            floatBufferAsFloatBuffer.put(BOX_COORDINATES, 0, BOX_COORDINATES.length).position(0);
            int[] iArr = new int[1];
            glGenBuffers(1, iArr, 0);
            this.mBoxCoords = iArr[0];
            gl11.glBindBuffer(34962, this.mBoxCoords);
            gl11.glBufferData(34962, floatBufferAsFloatBuffer.capacity() * 4, floatBufferAsFloatBuffer, 35044);
            gl11.glVertexPointer(2, 5126, 0, 0);
            gl11.glTexCoordPointer(2, 5126, 0, 0);
            gl11.glClientActiveTexture(33985);
            gl11.glTexCoordPointer(2, 5126, 0, 0);
            gl11.glClientActiveTexture(33984);
            gl11.glEnableClientState(32888);
        }
    }

    @Override
    public void setSize(int i, int i2) {
        com.android.gallery3d.common.Utils.assertTrue(i >= 0 && i2 >= 0);
        if (this.mTargetTexture == null) {
            this.mScreenWidth = i;
            this.mScreenHeight = i2;
        }
        this.mAlpha = 1.0f;
        GL11 gl11 = this.mGL;
        gl11.glViewport(0, 0, i, i2);
        gl11.glMatrixMode(5889);
        gl11.glLoadIdentity();
        float f = i2;
        GLU.gluOrtho2D(gl11, 0.0f, i, 0.0f, f);
        gl11.glMatrixMode(5888);
        gl11.glLoadIdentity();
        float[] fArr = this.mMatrixValues;
        Matrix.setIdentityM(fArr, 0);
        if (this.mTargetTexture == null) {
            Matrix.translateM(fArr, 0, 0.0f, f, 0.0f);
            Matrix.scaleM(fArr, 0, 1.0f, -1.0f, 1.0f);
        }
    }

    @Override
    public void setAlpha(float f) {
        com.android.gallery3d.common.Utils.assertTrue(f >= 0.0f && f <= 1.0f);
        this.mAlpha = f;
    }

    @Override
    public float getAlpha() {
        return this.mAlpha;
    }

    @Override
    public void multiplyAlpha(float f) {
        com.android.gallery3d.common.Utils.assertTrue(f >= 0.0f && f <= 1.0f);
        this.mAlpha *= f;
    }

    private static ByteBuffer allocateDirectNativeOrderBuffer(int i) {
        return ByteBuffer.allocateDirect(i).order(ByteOrder.nativeOrder());
    }

    @Override
    public void fillRect(float f, float f2, float f3, float f4, int i) {
        this.mGLState.setColorMode(i, this.mAlpha);
        GL11 gl11 = this.mGL;
        saveTransform();
        translate(f, f2);
        scale(f3, f4, 1.0f);
        gl11.glLoadMatrixf(this.mMatrixValues, 0);
        gl11.glDrawArrays(MSCALE_Y, 0, 4);
        restoreTransform();
        this.mCountFillRect++;
    }

    @Override
    public void translate(float f, float f2, float f3) {
        Matrix.translateM(this.mMatrixValues, 0, f, f2, f3);
    }

    @Override
    public void translate(float f, float f2) {
        float[] fArr = this.mMatrixValues;
        fArr[12] = fArr[12] + (fArr[0] * f) + (fArr[4] * f2);
        fArr[13] = fArr[13] + (fArr[1] * f) + (fArr[MSCALE_Y] * f2);
        fArr[14] = fArr[14] + (fArr[2] * f) + (fArr[OFFSET_DRAW_RECT] * f2);
        fArr[15] = fArr[15] + (fArr[3] * f) + (fArr[7] * f2);
    }

    @Override
    public void scale(float f, float f2, float f3) {
        Matrix.scaleM(this.mMatrixValues, 0, f, f2, f3);
    }

    @Override
    public void rotate(float f, float f2, float f3, float f4) {
        if (f == 0.0f) {
            return;
        }
        float[] fArr = this.mTempMatrix;
        Matrix.setRotateM(fArr, 0, f, f2, f3, f4);
        Matrix.multiplyMM(fArr, 16, this.mMatrixValues, 0, fArr, 0);
        System.arraycopy(fArr, 16, this.mMatrixValues, 0, 16);
    }

    @Override
    public void multiplyMatrix(float[] fArr, int i) {
        float[] fArr2 = this.mTempMatrix;
        Matrix.multiplyMM(fArr2, 0, this.mMatrixValues, 0, fArr, i);
        System.arraycopy(fArr2, 0, this.mMatrixValues, 0, 16);
    }

    private void textureRect(float f, float f2, float f3, float f4) {
        GL11 gl11 = this.mGL;
        saveTransform();
        translate(f, f2);
        scale(f3, f4, 1.0f);
        gl11.glLoadMatrixf(this.mMatrixValues, 0);
        gl11.glDrawArrays(MSCALE_Y, 0, 4);
        restoreTransform();
        this.mCountTextureRect++;
    }

    @Override
    public void drawMesh(MBasicTexture mBasicTexture, int i, int i2, int i3, int i4, int i5, int i6) {
        float f = this.mAlpha;
        if (bindTexture(mBasicTexture)) {
            this.mGLState.setBlendEnabled(this.mBlendEnabled && (!mBasicTexture.isOpaque() || f < OPAQUE_ALPHA));
            this.mGLState.setTextureAlpha(f);
            setTextureCoords(0.0f, 0.0f, 1.0f, 1.0f);
            saveTransform();
            translate(i, i2);
            this.mGL.glLoadMatrixf(this.mMatrixValues, 0);
            this.mGL.glBindBuffer(34962, i3);
            this.mGL.glVertexPointer(2, 5126, 0, 0);
            this.mGL.glBindBuffer(34962, i4);
            this.mGL.glTexCoordPointer(2, 5126, 0, 0);
            this.mGL.glBindBuffer(34963, i5);
            this.mGL.glDrawElements(MSCALE_Y, i6, 5121, 0);
            this.mGL.glBindBuffer(34962, this.mBoxCoords);
            this.mGL.glVertexPointer(2, 5126, 0, 0);
            this.mGL.glTexCoordPointer(2, 5126, 0, 0);
            restoreTransform();
            this.mCountDrawMesh++;
        }
    }

    private float[] mapPoints(float[] fArr, int i, int i2, int i3, int i4) {
        float[] fArr2 = this.mMapPointsBuffer;
        float f = i;
        float f2 = i2;
        float f3 = (fArr[0] * f) + (fArr[4] * f2) + fArr[12];
        float f4 = (fArr[1] * f) + (fArr[MSCALE_Y] * f2) + fArr[13];
        float f5 = (fArr[3] * f) + (fArr[7] * f2) + fArr[15];
        fArr2[0] = f3 / f5;
        fArr2[1] = f4 / f5;
        float f6 = i3;
        float f7 = i4;
        float f8 = (fArr[0] * f6) + (fArr[4] * f7) + fArr[12];
        float f9 = (fArr[1] * f6) + (fArr[MSCALE_Y] * f7) + fArr[13];
        float f10 = (fArr[3] * f6) + (fArr[7] * f7) + fArr[15];
        fArr2[2] = f8 / f10;
        fArr2[3] = f9 / f10;
        return fArr2;
    }

    private void drawBoundTexture(MBasicTexture mBasicTexture, int i, int i2, int i3, int i4) {
        if (isMatrixRotatedOrFlipped(this.mMatrixValues)) {
            if (mBasicTexture.hasBorder()) {
                setTextureCoords(1.0f / mBasicTexture.getTextureWidth(), 1.0f / mBasicTexture.getTextureHeight(), (mBasicTexture.getWidth() - 1.0f) / mBasicTexture.getTextureWidth(), (mBasicTexture.getHeight() - 1.0f) / mBasicTexture.getTextureHeight());
            } else {
                setTextureCoords(0.0f, 0.0f, mBasicTexture.getWidth() / mBasicTexture.getTextureWidth(), mBasicTexture.getHeight() / mBasicTexture.getTextureHeight());
            }
            textureRect(i, i2, i3, i4);
            return;
        }
        float[] fArrMapPoints = mapPoints(this.mMatrixValues, i, i2 + i4, i + i3, i2);
        int i5 = (int) (fArrMapPoints[0] + 0.5f);
        int i6 = (int) (fArrMapPoints[1] + 0.5f);
        int i7 = ((int) (fArrMapPoints[2] + 0.5f)) - i5;
        int i8 = ((int) (fArrMapPoints[3] + 0.5f)) - i6;
        if (i7 > 0 && i8 > 0) {
            ((GL11Ext) this.mGL).glDrawTexiOES(i5, i6, 0, i7, i8);
            this.mCountTextureOES++;
        }
    }

    @Override
    public void drawTexture(MBasicTexture mBasicTexture, int i, int i2, int i3, int i4) {
        drawTexture(mBasicTexture, i, i2, i3, i4, this.mAlpha);
    }

    private void drawTexture(MBasicTexture mBasicTexture, int i, int i2, int i3, int i4, float f) {
        if (i3 <= 0 || i4 <= 0) {
            return;
        }
        this.mGLState.setBlendEnabled(this.mBlendEnabled && (!mBasicTexture.isOpaque() || f < OPAQUE_ALPHA));
        if (bindTexture(mBasicTexture)) {
            this.mGLState.setTextureAlpha(f);
            drawBoundTexture(mBasicTexture, i, i2, i3, i4);
        }
    }

    @Override
    public void drawTexture(MBasicTexture mBasicTexture, RectF rectF, RectF rectF2) {
        if (rectF2.width() <= 0.0f || rectF2.height() <= 0.0f) {
            return;
        }
        this.mDrawTextureSourceRect.set(rectF);
        this.mDrawTextureTargetRect.set(rectF2);
        RectF rectF3 = this.mDrawTextureSourceRect;
        RectF rectF4 = this.mDrawTextureTargetRect;
        this.mGLState.setBlendEnabled(this.mBlendEnabled && (!mBasicTexture.isOpaque() || this.mAlpha < OPAQUE_ALPHA));
        if (bindTexture(mBasicTexture)) {
            convertCoordinate(rectF3, rectF4, mBasicTexture);
            setTextureCoords(rectF3);
            this.mGLState.setTextureAlpha(this.mAlpha);
            textureRect(rectF4.left, rectF4.top, rectF4.width(), rectF4.height());
        }
    }

    @Override
    public void drawTexture(MBasicTexture mBasicTexture, float[] fArr, int i, int i2, int i3, int i4) {
        this.mGLState.setBlendEnabled(this.mBlendEnabled && (!mBasicTexture.isOpaque() || this.mAlpha < OPAQUE_ALPHA));
        if (bindTexture(mBasicTexture)) {
            setTextureCoords(fArr);
            this.mGLState.setTextureAlpha(this.mAlpha);
            textureRect(i, i2, i3, i4);
        }
    }

    private static void convertCoordinate(RectF rectF, RectF rectF2, MBasicTexture mBasicTexture) {
        int width = mBasicTexture.getWidth();
        int height = mBasicTexture.getHeight();
        int textureWidth = mBasicTexture.getTextureWidth();
        int textureHeight = mBasicTexture.getTextureHeight();
        float f = textureWidth;
        rectF.left /= f;
        rectF.right /= f;
        float f2 = textureHeight;
        rectF.top /= f2;
        rectF.bottom /= f2;
        float f3 = width / f;
        if (rectF.right > f3) {
            rectF2.right = rectF2.left + ((rectF2.width() * (f3 - rectF.left)) / rectF.width());
            rectF.right = f3;
        }
        float f4 = height / f2;
        if (rectF.bottom > f4) {
            rectF2.bottom = rectF2.top + ((rectF2.height() * (f4 - rectF.top)) / rectF.height());
            rectF.bottom = f4;
        }
    }

    @Override
    public void drawMixed(MBasicTexture mBasicTexture, int i, float f, int i2, int i3, int i4, int i5) {
        drawMixed(mBasicTexture, i, f, i2, i3, i4, i5, this.mAlpha);
    }

    private boolean bindTexture(MBasicTexture mBasicTexture) {
        if (!mBasicTexture.onBind(this)) {
            return false;
        }
        int target = mBasicTexture.getTarget();
        this.mGLState.setTextureTarget(target);
        this.mGL.glBindTexture(target, mBasicTexture.getId());
        return true;
    }

    private void setTextureColor(float f, float f2, float f3, float f4) {
        float[] fArr = this.mTextureColor;
        fArr[0] = f;
        fArr[1] = f2;
        fArr[2] = f3;
        fArr[3] = f4;
    }

    private void setMixedColor(int i, float f, float f2) {
        float f3 = (1.0f - f) * f2;
        float f4 = (((f2 * f) / (1.0f - f3)) * (i >>> 24)) / 65025.0f;
        setTextureColor(((i >>> 16) & 255) * f4, ((i >>> 8) & 255) * f4, (i & 255) * f4, f3);
        GL11 gl11 = this.mGL;
        gl11.glTexEnvfv(8960, 8705, this.mTextureColor, 0);
        gl11.glTexEnvf(8960, 34161, 34165.0f);
        gl11.glTexEnvf(8960, 34162, 34165.0f);
        gl11.glTexEnvf(8960, 34177, 34166.0f);
        gl11.glTexEnvf(8960, 34193, 768.0f);
        gl11.glTexEnvf(8960, 34185, 34166.0f);
        gl11.glTexEnvf(8960, 34201, 770.0f);
        gl11.glTexEnvf(8960, 34178, 34166.0f);
        gl11.glTexEnvf(8960, 34194, 770.0f);
        gl11.glTexEnvf(8960, 34186, 34166.0f);
        gl11.glTexEnvf(8960, 34202, 770.0f);
    }

    @Override
    public void drawMixed(MBasicTexture mBasicTexture, int i, float f, RectF rectF, RectF rectF2) {
        if (rectF2.width() <= 0.0f || rectF2.height() <= 0.0f) {
            return;
        }
        if (f <= 0.01f) {
            drawTexture(mBasicTexture, rectF, rectF2);
            return;
        }
        if (f >= 1.0f) {
            fillRect(rectF2.left, rectF2.top, rectF2.width(), rectF2.height(), i);
            return;
        }
        float f2 = this.mAlpha;
        this.mDrawTextureSourceRect.set(rectF);
        this.mDrawTextureTargetRect.set(rectF2);
        RectF rectF3 = this.mDrawTextureSourceRect;
        RectF rectF4 = this.mDrawTextureTargetRect;
        this.mGLState.setBlendEnabled(this.mBlendEnabled && !(mBasicTexture.isOpaque() && com.android.gallery3d.common.Utils.isOpaque(i) && f2 >= OPAQUE_ALPHA));
        if (bindTexture(mBasicTexture)) {
            this.mGLState.setTexEnvMode(34160);
            setMixedColor(i, f, f2);
            convertCoordinate(rectF3, rectF4, mBasicTexture);
            setTextureCoords(rectF3);
            textureRect(rectF4.left, rectF4.top, rectF4.width(), rectF4.height());
            this.mGLState.setTexEnvMode(7681);
        }
    }

    private void drawMixed(MBasicTexture mBasicTexture, int i, float f, int i2, int i3, int i4, int i5, float f2) {
        if (f <= 0.01f) {
            drawTexture(mBasicTexture, i2, i3, i4, i5, f2);
            return;
        }
        if (f >= 1.0f) {
            fillRect(i2, i3, i4, i5, i);
            return;
        }
        this.mGLState.setBlendEnabled(this.mBlendEnabled && !(mBasicTexture.isOpaque() && com.android.gallery3d.common.Utils.isOpaque(i) && f2 >= OPAQUE_ALPHA));
        GL11 gl11 = this.mGL;
        if (bindTexture(mBasicTexture)) {
            this.mGLState.setTexEnvMode(34160);
            setMixedColor(i, f, f2);
            drawBoundTexture(mBasicTexture, i2, i3, i4, i5);
            this.mGLState.setTexEnvMode(7681);
        }
    }

    private static boolean isMatrixRotatedOrFlipped(float[] fArr) {
        return Math.abs(fArr[4]) > 1.0E-5f || Math.abs(fArr[1]) > 1.0E-5f || fArr[0] < -1.0E-5f || fArr[MSCALE_Y] > 1.0E-5f;
    }

    private static class GLState {
        private final GL11 mGL;
        private int mTexEnvMode = 7681;
        private float mTextureAlpha = 1.0f;
        private int mTextureTarget = 3553;
        private boolean mBlendEnabled = true;
        private float mLineWidth = 1.0f;
        private boolean mLineSmooth = false;

        public GLState(GL11 gl11) {
            this.mGL = gl11;
            gl11.glDisable(2896);
            gl11.glEnable(3024);
            gl11.glEnableClientState(32884);
            gl11.glEnableClientState(32888);
            gl11.glEnable(3553);
            gl11.glTexEnvf(8960, 8704, 7681.0f);
            gl11.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
            gl11.glEnable(3042);
            gl11.glBlendFunc(1, 771);
            gl11.glPixelStorei(3317, 2);
        }

        public void setTexEnvMode(int i) {
            if (this.mTexEnvMode == i) {
                return;
            }
            this.mTexEnvMode = i;
            this.mGL.glTexEnvf(8960, 8704, i);
        }

        public void setLineWidth(float f) {
            if (this.mLineWidth == f) {
                return;
            }
            this.mLineWidth = f;
            this.mGL.glLineWidth(f);
        }

        public void setTextureAlpha(float f) {
            if (this.mTextureAlpha == f) {
                return;
            }
            this.mTextureAlpha = f;
            if (f >= MGLES11Canvas.OPAQUE_ALPHA) {
                this.mGL.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
                setTexEnvMode(7681);
            } else {
                this.mGL.glColor4f(f, f, f, f);
                setTexEnvMode(8448);
            }
        }

        public void setColorMode(int i, float f) {
            setBlendEnabled(!com.android.gallery3d.common.Utils.isOpaque(i) || f < MGLES11Canvas.OPAQUE_ALPHA);
            this.mTextureAlpha = -1.0f;
            setTextureTarget(0);
            float f2 = ((((i >>> 24) * f) * 65535.0f) / 255.0f) / 255.0f;
            this.mGL.glColor4x(Math.round(((i >> 16) & 255) * f2), Math.round(((i >> 8) & 255) * f2), Math.round((i & 255) * f2), Math.round(255.0f * f2));
        }

        public void setTextureTarget(int i) {
            if (this.mTextureTarget == i) {
                return;
            }
            if (this.mTextureTarget != 0) {
                this.mGL.glDisable(this.mTextureTarget);
            }
            this.mTextureTarget = i;
            if (this.mTextureTarget != 0) {
                this.mGL.glEnable(this.mTextureTarget);
            }
        }

        public void setBlendEnabled(boolean z) {
            if (this.mBlendEnabled == z) {
                return;
            }
            this.mBlendEnabled = z;
            if (z) {
                this.mGL.glEnable(3042);
            } else {
                this.mGL.glDisable(3042);
            }
        }
    }

    @Override
    public void clearBuffer(float[] fArr) {
        if (fArr != null && fArr.length == 4) {
            this.mGL.glClearColor(fArr[1], fArr[2], fArr[3], fArr[0]);
        } else {
            this.mGL.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        }
        this.mGL.glClear(16384);
    }

    @Override
    public void clearBuffer() {
        clearBuffer(null);
    }

    private void setTextureCoords(RectF rectF) {
        setTextureCoords(rectF.left, rectF.top, rectF.right, rectF.bottom);
    }

    private void setTextureCoords(float f, float f2, float f3, float f4) {
        this.mGL.glMatrixMode(5890);
        this.mTextureMatrixValues[0] = f3 - f;
        this.mTextureMatrixValues[MSCALE_Y] = f4 - f2;
        this.mTextureMatrixValues[10] = 1.0f;
        this.mTextureMatrixValues[12] = f;
        this.mTextureMatrixValues[13] = f2;
        this.mTextureMatrixValues[15] = 1.0f;
        this.mGL.glLoadMatrixf(this.mTextureMatrixValues, 0);
        this.mGL.glMatrixMode(5888);
    }

    private void setTextureCoords(float[] fArr) {
        this.mGL.glMatrixMode(5890);
        this.mGL.glLoadMatrixf(fArr, 0);
        this.mGL.glMatrixMode(5888);
    }

    @Override
    public boolean unloadTexture(MBasicTexture mBasicTexture) {
        synchronized (this.mUnboundTextures) {
            if (!mBasicTexture.isLoaded()) {
                return false;
            }
            this.mUnboundTextures.add(mBasicTexture.mId);
            return true;
        }
    }

    @Override
    public void deleteBuffer(int i) {
        synchronized (this.mUnboundTextures) {
            this.mDeleteBuffers.add(i);
        }
    }

    @Override
    public void deleteRecycledResources() {
        synchronized (this.mUnboundTextures) {
            IntArray intArray = this.mUnboundTextures;
            if (intArray.size() > 0) {
                glDeleteTextures(intArray.size(), intArray.getInternalArray(), 0);
                intArray.clear();
            }
            IntArray intArray2 = this.mDeleteBuffers;
            if (intArray2.size() > 0) {
                glDeleteBuffers(intArray2.size(), intArray2.getInternalArray(), 0);
                intArray2.clear();
            }
        }
    }

    @Override
    public void save() {
        save(-1);
    }

    @Override
    public void save(int i) {
        ConfigState configStateObtainRestoreConfig = obtainRestoreConfig();
        if ((i & 1) != 0) {
            configStateObtainRestoreConfig.mAlpha = this.mAlpha;
        } else {
            configStateObtainRestoreConfig.mAlpha = -1.0f;
        }
        if ((i & 2) != 0) {
            System.arraycopy(this.mMatrixValues, 0, configStateObtainRestoreConfig.mMatrix, 0, 16);
        } else {
            configStateObtainRestoreConfig.mMatrix[0] = Float.NEGATIVE_INFINITY;
        }
        this.mRestoreStack.add(configStateObtainRestoreConfig);
    }

    @Override
    public void restore() {
        if (this.mRestoreStack.isEmpty()) {
            throw new IllegalStateException();
        }
        ConfigState configStateRemove = this.mRestoreStack.remove(this.mRestoreStack.size() - 1);
        configStateRemove.restore(this);
        freeRestoreConfig(configStateRemove);
    }

    private void freeRestoreConfig(ConfigState configState) {
        configState.mNextFree = this.mRecycledRestoreAction;
        this.mRecycledRestoreAction = configState;
    }

    private ConfigState obtainRestoreConfig() {
        if (this.mRecycledRestoreAction != null) {
            ConfigState configState = this.mRecycledRestoreAction;
            this.mRecycledRestoreAction = configState.mNextFree;
            return configState;
        }
        return new ConfigState();
    }

    private static class ConfigState {
        float mAlpha;
        float[] mMatrix;
        ConfigState mNextFree;

        private ConfigState() {
            this.mMatrix = new float[16];
        }

        public void restore(MGLES11Canvas mGLES11Canvas) {
            if (this.mAlpha >= 0.0f) {
                mGLES11Canvas.setAlpha(this.mAlpha);
            }
            if (this.mMatrix[0] != Float.NEGATIVE_INFINITY) {
                System.arraycopy(this.mMatrix, 0, mGLES11Canvas.mMatrixValues, 0, 16);
            }
        }
    }

    @Override
    public void dumpStatisticsAndClear() {
        String str = String.format("MESH:%d, TEX_OES:%d, TEX_RECT:%d, FILL_RECT:%d, LINE:%d", Integer.valueOf(this.mCountDrawMesh), Integer.valueOf(this.mCountTextureRect), Integer.valueOf(this.mCountTextureOES), Integer.valueOf(this.mCountFillRect), Integer.valueOf(this.mCountDrawLine));
        this.mCountDrawMesh = 0;
        this.mCountTextureRect = 0;
        this.mCountTextureOES = 0;
        this.mCountFillRect = 0;
        this.mCountDrawLine = 0;
        Log.d(TAG, "<dumpStatisticsAndClear> " + str);
    }

    private void saveTransform() {
        System.arraycopy(this.mMatrixValues, 0, this.mTempMatrix, 0, 16);
    }

    private void restoreTransform() {
        System.arraycopy(this.mTempMatrix, 0, this.mMatrixValues, 0, 16);
    }

    private void setRenderTarget(MRawTexture mRawTexture) {
        GL11ExtensionPack gL11ExtensionPack = (GL11ExtensionPack) this.mGL;
        if (this.mTargetTexture == null && mRawTexture != null) {
            if (this.mFrameBuffer[0] == -1) {
                glGenBuffers(1, this.mFrameBuffer, 0);
            }
            gL11ExtensionPack.glBindFramebufferOES(36160, this.mFrameBuffer[0]);
        }
        if (this.mTargetTexture != null && mRawTexture == null) {
            gL11ExtensionPack.glBindFramebufferOES(36160, 0);
        }
        this.mTargetTexture = mRawTexture;
        if (mRawTexture == null) {
            setSize(this.mScreenWidth, this.mScreenHeight);
            return;
        }
        setSize(mRawTexture.getWidth(), mRawTexture.getHeight());
        if (!mRawTexture.isLoaded()) {
            mRawTexture.prepare(this);
        }
        gL11ExtensionPack.glFramebufferTexture2DOES(36160, 36064, 3553, mRawTexture.getId(), 0);
        checkFramebufferStatus(gL11ExtensionPack);
    }

    @Override
    public void endRenderTarget() {
        setRenderTarget(this.mTargetStack.remove(this.mTargetStack.size() - 1));
        restore();
    }

    @Override
    public void beginRenderTarget(MRawTexture mRawTexture) {
        save();
        this.mTargetStack.add(this.mTargetTexture);
        setRenderTarget(mRawTexture);
    }

    private static void checkFramebufferStatus(GL11ExtensionPack gL11ExtensionPack) {
        String str;
        int iGlCheckFramebufferStatusOES = gL11ExtensionPack.glCheckFramebufferStatusOES(36160);
        if (iGlCheckFramebufferStatusOES != 36053) {
            switch (iGlCheckFramebufferStatusOES) {
                case 36054:
                    str = "FRAMEBUFFER_ATTACHMENT";
                    break;
                case 36055:
                    str = "FRAMEBUFFER_MISSING_ATTACHMENT";
                    break;
                case 36056:
                default:
                    str = "";
                    break;
                case 36057:
                    str = "FRAMEBUFFER_INCOMPLETE_DIMENSIONS";
                    break;
                case 36058:
                    str = "FRAMEBUFFER_FORMATS";
                    break;
                case 36059:
                    str = "FRAMEBUFFER_DRAW_BUFFER";
                    break;
                case 36060:
                    str = "FRAMEBUFFER_READ_BUFFER";
                    break;
                case 36061:
                    str = "FRAMEBUFFER_UNSUPPORTED";
                    break;
            }
            throw new RuntimeException(str + ":" + Integer.toHexString(iGlCheckFramebufferStatusOES));
        }
    }

    @Override
    public void setTextureParameters(MBasicTexture mBasicTexture) {
        int width = mBasicTexture.getWidth();
        int height = mBasicTexture.getHeight();
        sCropRect[0] = 0.0f;
        sCropRect[1] = height;
        sCropRect[2] = width;
        sCropRect[3] = -height;
        int target = mBasicTexture.getTarget();
        this.mGL.glBindTexture(target, mBasicTexture.getId());
        this.mGL.glTexParameterfv(target, 35741, sCropRect, 0);
        this.mGL.glTexParameteri(target, 10242, 33071);
        this.mGL.glTexParameteri(target, 10243, 33071);
        this.mGL.glTexParameterf(target, 10241, 9729.0f);
        this.mGL.glTexParameterf(target, 10240, 9729.0f);
    }

    @Override
    public void initializeTextureSize(MBasicTexture mBasicTexture, int i, int i2) {
        int target = mBasicTexture.getTarget();
        this.mGL.glBindTexture(target, mBasicTexture.getId());
        this.mGL.glTexImage2D(target, 0, i, mBasicTexture.getTextureWidth(), mBasicTexture.getTextureHeight(), 0, i, i2, null);
    }

    @Override
    public void initializeTexture(MBasicTexture mBasicTexture, Bitmap bitmap) {
        int target = mBasicTexture.getTarget();
        this.mGL.glBindTexture(target, mBasicTexture.getId());
        GLUtils.texImage2D(target, 0, bitmap, 0);
    }

    @Override
    public void texSubImage2D(MBasicTexture mBasicTexture, int i, int i2, Bitmap bitmap, int i3, int i4) {
        int target = mBasicTexture.getTarget();
        this.mGL.glBindTexture(target, mBasicTexture.getId());
        GLUtils.texSubImage2D(target, 0, i, i2, bitmap, i3, i4);
    }

    @Override
    public int uploadBuffer(FloatBuffer floatBuffer) {
        return uploadBuffer(floatBuffer, 4);
    }

    @Override
    public int uploadBuffer(ByteBuffer byteBuffer) {
        return uploadBuffer(byteBuffer, 1);
    }

    private int uploadBuffer(Buffer buffer, int i) {
        int[] iArr = new int[1];
        glGenBuffers(iArr.length, iArr, 0);
        int i2 = iArr[0];
        this.mGL.glBindBuffer(34962, i2);
        this.mGL.glBufferData(34962, buffer.capacity() * i, buffer, 35044);
        return i2;
    }

    @Override
    public void recoverFromLightCycle() {
    }

    @Override
    public void getBounds(Rect rect, int i, int i2, int i3, int i4) {
    }

    @Override
    public void setPerspective(float f, float f2, float f3, float f4) {
        this.mGL.glMatrixMode(5889);
        this.mGL.glLoadIdentity();
        GLU.gluPerspective(this.mGL, f, f2, f3, f4);
        this.mGL.glMatrixMode(5888);
    }

    @Override
    public void setLookAt(float f, float f2, float f3, float f4, float f5, float f6, float f7, float f8, float f9) {
        this.mGL.glMatrixMode(5888);
        this.mGL.glLoadIdentity();
        GLU.gluLookAt(this.mGL, f, f2, f3, f4, f5, f6, f7, f8, f9);
        float[] fArr = new float[16];
        this.mGL.glGetFloatv(2982, fArr, 0);
        Matrix.multiplyMM(fArr, 0, fArr, 0, this.mMatrixValues, 0);
        System.arraycopy(fArr, 0, this.mMatrixValues, 0, 16);
        this.mGL.glLoadMatrixf(this.mMatrixValues, 0);
    }

    @Override
    public void drawMesh(MBasicTexture mBasicTexture, float f, float f2, float f3, float f4, int i, int i2, int i3) {
        float f5 = this.mAlpha;
        if (!bindTexture(mBasicTexture)) {
            return;
        }
        this.mGLState.setBlendEnabled(this.mBlendEnabled && (!mBasicTexture.isOpaque() || f5 < OPAQUE_ALPHA));
        this.mGLState.setTextureAlpha(f5);
        setTextureCoords(0.0f, 0.0f, 1.0f, 1.0f);
        saveTransform();
        rotate(f, f2, f3, f4);
        this.mGL.glLoadMatrixf(this.mMatrixValues, 0);
        this.mGL.glBindBuffer(34962, i);
        this.mGL.glVertexPointer(3, 5126, 0, 0);
        this.mGL.glBindBuffer(34962, i2);
        this.mGL.glTexCoordPointer(2, 5126, 0, 0);
        this.mGL.glDrawArrays(4, 0, i3);
        this.mGL.glBindBuffer(34962, this.mBoxCoords);
        this.mGL.glVertexPointer(2, 5126, 0, 0);
        this.mGL.glTexCoordPointer(2, 5126, 0, 0);
        restoreTransform();
        this.mCountDrawMesh++;
    }

    @Override
    public void readPixels(int i, int i2, int i3, int i4, int i5, int i6, Buffer buffer) {
        this.mGL.glReadPixels(i, i2, i3, i4, i5, i6, buffer);
    }

    @Override
    public int getGLVersion() {
        return 1;
    }

    @Override
    public void glDeleteTextures(int i, int[] iArr, int i2) {
        this.mGL.glDeleteTextures(i, iArr, i2);
    }

    @Override
    public void glDeleteBuffers(int i, int[] iArr, int i2) {
        this.mGL.glDeleteBuffers(i, iArr, i2);
    }

    @Override
    public void glDeleteFramebuffers(int i, int[] iArr, int i2) {
        ((GL11ExtensionPack) this.mGL).glDeleteFramebuffersOES(i, iArr, i2);
    }

    class GLES11Generator implements MGLCanvas.Generator {
        GLES11Generator() {
        }

        @Override
        public int generateTexture() {
            int[] iArr = new int[1];
            MGLES11Canvas.this.mGL.glGenTextures(1, iArr, 0);
            return iArr[0];
        }

        @Override
        public void glGenBuffers(int i, int[] iArr, int i2) {
            MGLES11Canvas.this.mGL.glGenBuffers(i, iArr, i2);
        }
    }

    @Override
    public int generateTexture() {
        return this.mGenerator.generateTexture();
    }

    @Override
    public void glGenBuffers(int i, int[] iArr, int i2) {
        this.mGenerator.glGenBuffers(i, iArr, i2);
    }

    @Override
    public void setGenerator(MGLCanvas.Generator generator) {
        this.mGenerator = generator;
    }

    @Override
    public int getWidth() {
        return this.mScreenWidth;
    }

    @Override
    public int getHeight() {
        return this.mScreenHeight;
    }

    @Override
    public Bitmap saveTexture(int i, int i2, int i3) {
        return null;
    }
}
