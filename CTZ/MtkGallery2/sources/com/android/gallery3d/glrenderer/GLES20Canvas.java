package com.android.gallery3d.glrenderer;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.RectF;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import com.android.gallery3d.util.IntArray;
import com.mediatek.gallery3d.util.Log;
import com.mediatek.gallerybasic.gl.MGLCanvas;
import com.mediatek.gallerybasic.gl.MGLES20Canvas;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Arrays;

public class GLES20Canvas implements GLCanvas {
    private int mBoxCoordinates;
    private int mDrawProgram;
    private int mHeight;
    private MGLES20Canvas mMGLCanvas;
    private int mMeshProgram;
    private int mOesTextureProgram;
    private int mScreenHeight;
    private int mScreenWidth;
    private int mTextureProgram;
    private int mWidth;
    private static final String TAG = GLES20Canvas.class.getSimpleName();
    private static final float[] BOX_COORDINATES = {0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f, 1.0f, 0.0f};
    private static final float[] BOUNDS_COORDINATES = {0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f, 0.0f, 1.0f};
    private static final GLId mGLId = new GLES20IdImpl();
    private float[] mMatrices = new float[128];
    private float[] mAlphas = new float[8];
    private IntArray mSaveFlags = new IntArray();
    private int mCurrentAlphaIndex = 0;
    private int mCurrentMatrixIndex = 0;
    private float[] mProjectionMatrix = new float[16];
    ShaderParameter[] mDrawParameters = {new AttributeShaderParameter("aPosition"), new UniformShaderParameter("uMatrix"), new UniformShaderParameter("uColor")};
    ShaderParameter[] mTextureParameters = {new AttributeShaderParameter("aPosition"), new UniformShaderParameter("uMatrix"), new UniformShaderParameter("uTextureMatrix"), new UniformShaderParameter("uTextureSampler"), new UniformShaderParameter("uAlpha")};
    ShaderParameter[] mOesTextureParameters = {new AttributeShaderParameter("aPosition"), new UniformShaderParameter("uMatrix"), new UniformShaderParameter("uTextureMatrix"), new UniformShaderParameter("uTextureSampler"), new UniformShaderParameter("uAlpha")};
    ShaderParameter[] mMeshParameters = {new AttributeShaderParameter("aPosition"), new UniformShaderParameter("uMatrix"), new AttributeShaderParameter("aTextureCoordinate"), new UniformShaderParameter("uTextureSampler"), new UniformShaderParameter("uAlpha")};
    private final IntArray mUnboundTextures = new IntArray();
    private final IntArray mDeleteBuffers = new IntArray();
    private int mCountDrawMesh = 0;
    private int mCountTextureRect = 0;
    private int mCountFillRect = 0;
    private int mCountDrawLine = 0;
    private int[] mFrameBuffer = new int[1];
    private ArrayList<RawTexture> mTargetTextures = new ArrayList<>();
    private final float[] mTempMatrix = new float[32];
    private final float[] mTempColor = new float[4];
    private final RectF mTempSourceRect = new RectF();
    private final RectF mTempTargetRect = new RectF();
    private final float[] mTempTextureMatrix = new float[16];
    private final int[] mTempIntArray = new int[1];

    private static abstract class ShaderParameter {
        public int handle;
        protected final String mName;

        public abstract void loadHandle(int i);

        public ShaderParameter(String str) {
            this.mName = str;
        }
    }

    private static class UniformShaderParameter extends ShaderParameter {
        public UniformShaderParameter(String str) {
            super(str);
        }

        @Override
        public void loadHandle(int i) {
            this.handle = GLES20.glGetUniformLocation(i, this.mName);
            GLES20Canvas.checkError();
        }
    }

    private static class AttributeShaderParameter extends ShaderParameter {
        public AttributeShaderParameter(String str) {
            super(str);
        }

        @Override
        public void loadHandle(int i) {
            this.handle = GLES20.glGetAttribLocation(i, this.mName);
            GLES20Canvas.checkError();
        }
    }

    public GLES20Canvas() {
        Matrix.setIdentityM(this.mTempTextureMatrix, 0);
        Matrix.setIdentityM(this.mMatrices, this.mCurrentMatrixIndex);
        this.mAlphas[this.mCurrentAlphaIndex] = 1.0f;
        this.mTargetTextures.add(null);
        this.mBoxCoordinates = uploadBuffer(createBuffer(BOX_COORDINATES));
        int iLoadShader = loadShader(35633, "uniform mat4 uMatrix;\nattribute vec2 aPosition;\nvoid main() {\n  vec4 pos = vec4(aPosition, 0.0, 1.0);\n  gl_Position = uMatrix * pos;\n}\n");
        int iLoadShader2 = loadShader(35633, "uniform mat4 uMatrix;\nuniform mat4 uTextureMatrix;\nattribute vec2 aPosition;\nvarying vec2 vTextureCoord;\nvoid main() {\n  vec4 pos = vec4(aPosition, 0.0, 1.0);\n  gl_Position = uMatrix * pos;\n  vTextureCoord = (uTextureMatrix * pos).xy;\n}\n");
        int iLoadShader3 = loadShader(35633, "uniform mat4 uMatrix;\nattribute vec2 aPosition;\nattribute vec2 aTextureCoordinate;\nvarying vec2 vTextureCoord;\nvoid main() {\n  vec4 pos = vec4(aPosition, 0.0, 1.0);\n  gl_Position = uMatrix * pos;\n  vTextureCoord = aTextureCoordinate;\n}\n");
        int iLoadShader4 = loadShader(35632, "precision mediump float;\nuniform vec4 uColor;\nvoid main() {\n  gl_FragColor = uColor;\n}\n");
        int iLoadShader5 = loadShader(35632, "precision mediump float;\nvarying vec2 vTextureCoord;\nuniform float uAlpha;\nuniform sampler2D uTextureSampler;\nvoid main() {\n  gl_FragColor = texture2D(uTextureSampler, vTextureCoord);\n  gl_FragColor *= uAlpha;\n}\n");
        int iLoadShader6 = loadShader(35632, "#extension GL_OES_EGL_image_external : require\nprecision mediump float;\nvarying vec2 vTextureCoord;\nuniform float uAlpha;\nuniform samplerExternalOES uTextureSampler;\nvoid main() {\n  gl_FragColor = texture2D(uTextureSampler, vTextureCoord);\n  gl_FragColor *= uAlpha;\n}\n");
        this.mDrawProgram = assembleProgram(iLoadShader, iLoadShader4, this.mDrawParameters);
        this.mTextureProgram = assembleProgram(iLoadShader2, iLoadShader5, this.mTextureParameters);
        this.mOesTextureProgram = assembleProgram(iLoadShader2, iLoadShader6, this.mOesTextureParameters);
        this.mMeshProgram = assembleProgram(iLoadShader3, iLoadShader5, this.mMeshParameters);
        GLES20.glBlendFunc(1, 771);
        checkError();
    }

    private static FloatBuffer createBuffer(float[] fArr) {
        FloatBuffer floatBufferAsFloatBuffer = ByteBuffer.allocateDirect(fArr.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        floatBufferAsFloatBuffer.put(fArr, 0, fArr.length).position(0);
        return floatBufferAsFloatBuffer;
    }

    private int assembleProgram(int i, int i2, ShaderParameter[] shaderParameterArr) {
        int iGlCreateProgram = GLES20.glCreateProgram();
        checkError();
        if (iGlCreateProgram == 0) {
            throw new RuntimeException("Cannot create GL program: " + GLES20.glGetError());
        }
        GLES20.glAttachShader(iGlCreateProgram, i);
        checkError();
        GLES20.glAttachShader(iGlCreateProgram, i2);
        checkError();
        GLES20.glLinkProgram(iGlCreateProgram);
        checkError();
        int[] iArr = this.mTempIntArray;
        GLES20.glGetProgramiv(iGlCreateProgram, 35714, iArr, 0);
        if (iArr[0] != 1) {
            Log.e(TAG, "Could not link program: ");
            Log.e(TAG, GLES20.glGetProgramInfoLog(iGlCreateProgram));
            GLES20.glDeleteProgram(iGlCreateProgram);
            iGlCreateProgram = 0;
        }
        for (ShaderParameter shaderParameter : shaderParameterArr) {
            shaderParameter.loadHandle(iGlCreateProgram);
        }
        return iGlCreateProgram;
    }

    private static int loadShader(int i, String str) {
        int iGlCreateShader = GLES20.glCreateShader(i);
        GLES20.glShaderSource(iGlCreateShader, str);
        checkError();
        GLES20.glCompileShader(iGlCreateShader);
        checkError();
        return iGlCreateShader;
    }

    @Override
    public void setSize(int i, int i2) {
        this.mWidth = i;
        this.mHeight = i2;
        GLES20.glViewport(0, 0, this.mWidth, this.mHeight);
        checkError();
        Matrix.setIdentityM(this.mMatrices, this.mCurrentMatrixIndex);
        float f = i2;
        Matrix.orthoM(this.mProjectionMatrix, 0, 0.0f, i, 0.0f, f, -1.0f, 1.0f);
        if (getTargetTexture() == null) {
            this.mScreenWidth = i;
            this.mScreenHeight = i2;
            Matrix.translateM(this.mMatrices, this.mCurrentMatrixIndex, 0.0f, f, 0.0f);
            Matrix.scaleM(this.mMatrices, this.mCurrentMatrixIndex, 1.0f, -1.0f, 1.0f);
        }
    }

    @Override
    public void clearBuffer() {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        checkError();
        GLES20.glClear(16384);
        checkError();
    }

    @Override
    public void clearBuffer(float[] fArr) {
        GLES20.glClearColor(fArr[1], fArr[2], fArr[3], fArr[0]);
        checkError();
        GLES20.glClear(16384);
        checkError();
    }

    @Override
    public float getAlpha() {
        return this.mAlphas[this.mCurrentAlphaIndex];
    }

    @Override
    public void setAlpha(float f) {
        this.mAlphas[this.mCurrentAlphaIndex] = f;
    }

    @Override
    public void multiplyAlpha(float f) {
        setAlpha(getAlpha() * f);
    }

    @Override
    public void translate(float f, float f2, float f3) {
        Matrix.translateM(this.mMatrices, this.mCurrentMatrixIndex, f, f2, f3);
    }

    @Override
    public void translate(float f, float f2) {
        int i = this.mCurrentMatrixIndex;
        float[] fArr = this.mMatrices;
        int i2 = i + 12;
        fArr[i2] = fArr[i2] + (fArr[i + 0] * f) + (fArr[i + 4] * f2);
        int i3 = i + 13;
        fArr[i3] = fArr[i3] + (fArr[i + 1] * f) + (fArr[i + 5] * f2);
        int i4 = i + 14;
        fArr[i4] = fArr[i4] + (fArr[i + 2] * f) + (fArr[i + 6] * f2);
        int i5 = i + 15;
        fArr[i5] = fArr[i5] + (fArr[i + 3] * f) + (fArr[i + 7] * f2);
    }

    @Override
    public void scale(float f, float f2, float f3) {
        Matrix.scaleM(this.mMatrices, this.mCurrentMatrixIndex, f, f2, f3);
    }

    @Override
    public void rotate(float f, float f2, float f3, float f4) {
        if (f == 0.0f) {
            return;
        }
        float[] fArr = this.mTempMatrix;
        Matrix.setRotateM(fArr, 0, f, f2, f3, f4);
        float[] fArr2 = this.mMatrices;
        int i = this.mCurrentMatrixIndex;
        Matrix.multiplyMM(fArr, 16, fArr2, i, fArr, 0);
        System.arraycopy(fArr, 16, fArr2, i, 16);
    }

    @Override
    public void multiplyMatrix(float[] fArr, int i) {
        float[] fArr2 = this.mTempMatrix;
        float[] fArr3 = this.mMatrices;
        int i2 = this.mCurrentMatrixIndex;
        Matrix.multiplyMM(fArr2, 0, fArr3, i2, fArr, i);
        System.arraycopy(fArr2, 0, fArr3, i2, 16);
    }

    @Override
    public void save() {
        save(-1);
    }

    @Override
    public void save(int i) {
        if ((i & 1) == 1) {
            float alpha = getAlpha();
            this.mCurrentAlphaIndex++;
            if (this.mAlphas.length <= this.mCurrentAlphaIndex) {
                this.mAlphas = Arrays.copyOf(this.mAlphas, this.mAlphas.length * 2);
            }
            this.mAlphas[this.mCurrentAlphaIndex] = alpha;
        }
        if ((i & 2) == 2) {
            int i2 = this.mCurrentMatrixIndex;
            this.mCurrentMatrixIndex += 16;
            if (this.mMatrices.length <= this.mCurrentMatrixIndex) {
                this.mMatrices = Arrays.copyOf(this.mMatrices, this.mMatrices.length * 2);
            }
            System.arraycopy(this.mMatrices, i2, this.mMatrices, this.mCurrentMatrixIndex, 16);
        }
        this.mSaveFlags.add(i);
    }

    @Override
    public void restore() {
        int iRemoveLast = this.mSaveFlags.removeLast();
        if ((iRemoveLast & 1) == 1) {
            this.mCurrentAlphaIndex--;
        }
        if ((iRemoveLast & 2) == 2) {
            this.mCurrentMatrixIndex -= 16;
        }
    }

    @Override
    public void drawLine(float f, float f2, float f3, float f4, GLPaint gLPaint) {
        draw(3, 4, 2, f, f2, f3 - f, f4 - f2, gLPaint);
        this.mCountDrawLine++;
    }

    @Override
    public void drawRect(float f, float f2, float f3, float f4, GLPaint gLPaint) {
        draw(2, 6, 4, f, f2, f3, f4, gLPaint);
        this.mCountDrawLine++;
    }

    private void draw(int i, int i2, int i3, float f, float f2, float f3, float f4, GLPaint gLPaint) {
        draw(i, i2, i3, f, f2, f3, f4, gLPaint.getColor(), gLPaint.getLineWidth());
    }

    private void draw(int i, int i2, int i3, float f, float f2, float f3, float f4, int i4, float f5) {
        prepareDraw(i2, i4, f5);
        draw(this.mDrawParameters, i, i3, f, f2, f3, f4);
    }

    private void prepareDraw(int i, int i2, float f) {
        GLES20.glUseProgram(this.mDrawProgram);
        checkError();
        if (f > 0.0f) {
            GLES20.glLineWidth(f);
            checkError();
        }
        float[] color = getColor(i2);
        boolean z = color[3] < 1.0f;
        enableBlending(z);
        if (z) {
            GLES20.glBlendColor(color[0], color[1], color[2], color[3]);
            checkError();
        }
        GLES20.glUniform4fv(this.mDrawParameters[2].handle, 1, color, 0);
        setPosition(this.mDrawParameters, i);
        checkError();
    }

    private float[] getColor(int i) {
        float alpha = (((i >>> 24) & 255) / 255.0f) * getAlpha();
        this.mTempColor[0] = (((i >>> 16) & 255) / 255.0f) * alpha;
        this.mTempColor[1] = (((i >>> 8) & 255) / 255.0f) * alpha;
        this.mTempColor[2] = ((i & 255) / 255.0f) * alpha;
        this.mTempColor[3] = alpha;
        return this.mTempColor;
    }

    private void enableBlending(boolean z) {
        if (z) {
            GLES20.glEnable(3042);
            checkError();
        } else {
            GLES20.glDisable(3042);
            checkError();
        }
    }

    private void setPosition(ShaderParameter[] shaderParameterArr, int i) {
        GLES20.glBindBuffer(34962, this.mBoxCoordinates);
        checkError();
        GLES20.glVertexAttribPointer(shaderParameterArr[0].handle, 2, 5126, false, 8, i * 8);
        checkError();
        GLES20.glBindBuffer(34962, 0);
        checkError();
    }

    private void draw(ShaderParameter[] shaderParameterArr, int i, int i2, float f, float f2, float f3, float f4) {
        setMatrix(shaderParameterArr, f, f2, f3, f4);
        int i3 = shaderParameterArr[0].handle;
        GLES20.glEnableVertexAttribArray(i3);
        checkError();
        GLES20.glDrawArrays(i, 0, i2);
        checkError();
        GLES20.glDisableVertexAttribArray(i3);
        checkError();
    }

    private void setMatrix(ShaderParameter[] shaderParameterArr, float f, float f2, float f3, float f4) {
        Matrix.translateM(this.mTempMatrix, 0, this.mMatrices, this.mCurrentMatrixIndex, f, f2, 0.0f);
        Matrix.scaleM(this.mTempMatrix, 0, f3, f4, 1.0f);
        Matrix.multiplyMM(this.mTempMatrix, 16, this.mProjectionMatrix, 0, this.mTempMatrix, 0);
        GLES20.glUniformMatrix4fv(shaderParameterArr[1].handle, 1, false, this.mTempMatrix, 16);
        checkError();
    }

    @Override
    public void fillRect(float f, float f2, float f3, float f4, int i) {
        draw(5, 0, 4, f, f2, f3, f4, i, 0.0f);
        this.mCountFillRect++;
    }

    @Override
    public void drawTexture(BasicTexture basicTexture, int i, int i2, int i3, int i4) {
        if (i3 <= 0 || i4 <= 0) {
            return;
        }
        copyTextureCoordinates(basicTexture, this.mTempSourceRect);
        this.mTempTargetRect.set(i, i2, i + i3, i2 + i4);
        convertCoordinate(this.mTempSourceRect, this.mTempTargetRect, basicTexture);
        drawTextureRect(basicTexture, this.mTempSourceRect, this.mTempTargetRect);
    }

    private static void copyTextureCoordinates(BasicTexture basicTexture, RectF rectF) {
        int width = basicTexture.getWidth();
        int height = basicTexture.getHeight();
        int i = 1;
        int i2 = 0;
        if (basicTexture.hasBorder()) {
            width--;
            height--;
            i2 = 1;
        } else {
            i = 0;
        }
        rectF.set(i, i2, width, height);
    }

    @Override
    public void drawTexture(BasicTexture basicTexture, RectF rectF, RectF rectF2) {
        if (rectF2.width() <= 0.0f || rectF2.height() <= 0.0f) {
            return;
        }
        this.mTempSourceRect.set(rectF);
        this.mTempTargetRect.set(rectF2);
        convertCoordinate(this.mTempSourceRect, this.mTempTargetRect, basicTexture);
        drawTextureRect(basicTexture, this.mTempSourceRect, this.mTempTargetRect);
    }

    @Override
    public void drawTexture(BasicTexture basicTexture, float[] fArr, int i, int i2, int i3, int i4) {
        if (i3 <= 0 || i4 <= 0) {
            return;
        }
        this.mTempTargetRect.set(i, i2, i + i3, i2 + i4);
        drawTextureRect(basicTexture, fArr, this.mTempTargetRect);
    }

    private void drawTextureRect(BasicTexture basicTexture, RectF rectF, RectF rectF2) {
        setTextureMatrix(rectF);
        drawTextureRect(basicTexture, this.mTempTextureMatrix, rectF2);
    }

    private void setTextureMatrix(RectF rectF) {
        this.mTempTextureMatrix[0] = rectF.width();
        this.mTempTextureMatrix[5] = rectF.height();
        this.mTempTextureMatrix[12] = rectF.left;
        this.mTempTextureMatrix[13] = rectF.top;
    }

    private static void convertCoordinate(RectF rectF, RectF rectF2, BasicTexture basicTexture) {
        int width = basicTexture.getWidth();
        int height = basicTexture.getHeight();
        int textureWidth = basicTexture.getTextureWidth();
        int textureHeight = basicTexture.getTextureHeight();
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

    private void drawTextureRect(BasicTexture basicTexture, float[] fArr, RectF rectF) {
        ShaderParameter[] shaderParameterArrPrepareTexture = prepareTexture(basicTexture);
        setPosition(shaderParameterArrPrepareTexture, 0);
        GLES20.glUniformMatrix4fv(shaderParameterArrPrepareTexture[2].handle, 1, false, fArr, 0);
        checkError();
        if (basicTexture.isFlippedVertically()) {
            save(2);
            translate(0.0f, rectF.centerY());
            scale(1.0f, -1.0f, 1.0f);
            translate(0.0f, -rectF.centerY());
        }
        draw(shaderParameterArrPrepareTexture, 5, 4, rectF.left, rectF.top, rectF.width(), rectF.height());
        if (basicTexture.isFlippedVertically()) {
            restore();
        }
        this.mCountTextureRect++;
    }

    private ShaderParameter[] prepareTexture(BasicTexture basicTexture) {
        ShaderParameter[] shaderParameterArr;
        int i;
        if (basicTexture.getTarget() == 3553) {
            shaderParameterArr = this.mTextureParameters;
            i = this.mTextureProgram;
        } else {
            shaderParameterArr = this.mOesTextureParameters;
            i = this.mOesTextureProgram;
        }
        prepareTexture(basicTexture, i, shaderParameterArr);
        return shaderParameterArr;
    }

    private void prepareTexture(BasicTexture basicTexture, int i, ShaderParameter[] shaderParameterArr) {
        GLES20.glUseProgram(i);
        checkError();
        enableBlending(!basicTexture.isOpaque() || getAlpha() < 0.95f);
        GLES20.glActiveTexture(33984);
        checkError();
        basicTexture.onBind(this);
        GLES20.glBindTexture(basicTexture.getTarget(), basicTexture.getId());
        checkError();
        GLES20.glUniform1i(shaderParameterArr[3].handle, 0);
        checkError();
        GLES20.glUniform1f(shaderParameterArr[4].handle, getAlpha());
        checkError();
    }

    @Override
    public void drawMesh(BasicTexture basicTexture, int i, int i2, int i3, int i4, int i5, int i6) {
        prepareTexture(basicTexture, this.mMeshProgram, this.mMeshParameters);
        GLES20.glBindBuffer(34963, i5);
        checkError();
        GLES20.glBindBuffer(34962, i3);
        checkError();
        int i7 = this.mMeshParameters[0].handle;
        GLES20.glVertexAttribPointer(i7, 2, 5126, false, 8, 0);
        checkError();
        GLES20.glBindBuffer(34962, i4);
        checkError();
        int i8 = this.mMeshParameters[2].handle;
        GLES20.glVertexAttribPointer(i8, 2, 5126, false, 8, 0);
        checkError();
        GLES20.glBindBuffer(34962, 0);
        checkError();
        GLES20.glEnableVertexAttribArray(i7);
        checkError();
        GLES20.glEnableVertexAttribArray(i8);
        checkError();
        setMatrix(this.mMeshParameters, i, i2, 1.0f, 1.0f);
        GLES20.glDrawElements(5, i6, 5121, 0);
        checkError();
        GLES20.glDisableVertexAttribArray(i7);
        checkError();
        GLES20.glDisableVertexAttribArray(i8);
        checkError();
        GLES20.glBindBuffer(34963, 0);
        checkError();
        this.mCountDrawMesh++;
    }

    @Override
    public void drawMixed(BasicTexture basicTexture, int i, float f, int i2, int i3, int i4, int i5) {
        copyTextureCoordinates(basicTexture, this.mTempSourceRect);
        this.mTempTargetRect.set(i2, i3, i2 + i4, i3 + i5);
        drawMixed(basicTexture, i, f, this.mTempSourceRect, this.mTempTargetRect);
    }

    @Override
    public void drawMixed(BasicTexture basicTexture, int i, float f, RectF rectF, RectF rectF2) {
        if (rectF2.width() <= 0.0f || rectF2.height() <= 0.0f) {
            return;
        }
        save(1);
        float alpha = getAlpha();
        float fMin = Math.min(1.0f, Math.max(0.0f, f));
        setAlpha((1.0f - fMin) * alpha);
        drawTexture(basicTexture, rectF, rectF2);
        setAlpha(fMin * alpha);
        fillRect(rectF2.left, rectF2.top, rectF2.width(), rectF2.height(), i);
        restore();
    }

    @Override
    public boolean unloadTexture(BasicTexture basicTexture) {
        boolean zIsLoaded = basicTexture.isLoaded();
        if (zIsLoaded) {
            synchronized (this.mUnboundTextures) {
                this.mUnboundTextures.add(basicTexture.getId());
            }
        }
        return zIsLoaded;
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
            if (this.mUnboundTextures.size() > 0) {
                mGLId.glDeleteTextures(null, intArray.size(), intArray.getInternalArray(), 0);
                intArray.clear();
            }
            IntArray intArray2 = this.mDeleteBuffers;
            if (intArray2.size() > 0) {
                mGLId.glDeleteBuffers(null, intArray2.size(), intArray2.getInternalArray(), 0);
                intArray2.clear();
            }
        }
        if (this.mMGLCanvas != null) {
            this.mMGLCanvas.deleteRecycledResources();
        }
    }

    @Override
    public void dumpStatisticsAndClear() {
        String str = String.format("MESH:%d, TEX_RECT:%d, FILL_RECT:%d, LINE:%d", Integer.valueOf(this.mCountDrawMesh), Integer.valueOf(this.mCountTextureRect), Integer.valueOf(this.mCountFillRect), Integer.valueOf(this.mCountDrawLine));
        this.mCountDrawMesh = 0;
        this.mCountTextureRect = 0;
        this.mCountFillRect = 0;
        this.mCountDrawLine = 0;
        Log.d(TAG, str);
    }

    @Override
    public void endRenderTarget() {
        setRenderTarget(this.mTargetTextures.remove(this.mTargetTextures.size() - 1), getTargetTexture());
        restore();
    }

    @Override
    public void beginRenderTarget(RawTexture rawTexture) {
        save();
        RawTexture targetTexture = getTargetTexture();
        this.mTargetTextures.add(rawTexture);
        setRenderTarget(targetTexture, rawTexture);
    }

    private RawTexture getTargetTexture() {
        return this.mTargetTextures.get(this.mTargetTextures.size() - 1);
    }

    private void setRenderTarget(BasicTexture basicTexture, RawTexture rawTexture) {
        if (basicTexture == null && rawTexture != null) {
            GLES20.glGenFramebuffers(1, this.mFrameBuffer, 0);
            checkError();
            GLES20.glBindFramebuffer(36160, this.mFrameBuffer[0]);
            checkError();
        } else if (basicTexture != null && rawTexture == null) {
            GLES20.glBindFramebuffer(36160, 0);
            checkError();
            GLES20.glDeleteFramebuffers(1, this.mFrameBuffer, 0);
            checkError();
        }
        if (rawTexture == null) {
            setSize(this.mScreenWidth, this.mScreenHeight);
            return;
        }
        setSize(rawTexture.getWidth(), rawTexture.getHeight());
        if (!rawTexture.isLoaded()) {
            rawTexture.prepare(this);
        }
        GLES20.glFramebufferTexture2D(36160, 36064, rawTexture.getTarget(), rawTexture.getId(), 0);
        checkError();
        checkFramebufferStatus();
    }

    private static void checkFramebufferStatus() {
        String str;
        int iGlCheckFramebufferStatus = GLES20.glCheckFramebufferStatus(36160);
        if (iGlCheckFramebufferStatus != 36053) {
            switch (iGlCheckFramebufferStatus) {
                case 36054:
                    str = "GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT";
                    break;
                case 36055:
                    str = "GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT";
                    break;
                case 36057:
                    str = "GL_FRAMEBUFFER_INCOMPLETE_DIMENSIONS";
                    break;
                case 36061:
                    str = "GL_FRAMEBUFFER_UNSUPPORTED";
                    break;
                default:
                    str = "";
                    break;
            }
            throw new RuntimeException(str + ":" + Integer.toHexString(iGlCheckFramebufferStatus));
        }
    }

    @Override
    public void setTextureParameters(BasicTexture basicTexture) {
        int target = basicTexture.getTarget();
        GLES20.glBindTexture(target, basicTexture.getId());
        checkError();
        GLES20.glTexParameteri(target, 10242, 33071);
        GLES20.glTexParameteri(target, 10243, 33071);
        GLES20.glTexParameterf(target, 10241, 9729.0f);
        GLES20.glTexParameterf(target, 10240, 9729.0f);
    }

    @Override
    public void initializeTextureSize(BasicTexture basicTexture, int i, int i2) {
        int target = basicTexture.getTarget();
        GLES20.glBindTexture(target, basicTexture.getId());
        checkError();
        GLES20.glTexImage2D(target, 0, i, basicTexture.getTextureWidth(), basicTexture.getTextureHeight(), 0, i, i2, null);
    }

    @Override
    public void initializeTexture(BasicTexture basicTexture, Bitmap bitmap) {
        int target = basicTexture.getTarget();
        GLES20.glBindTexture(target, basicTexture.getId());
        checkError();
        GLUtils.texImage2D(target, 0, bitmap, 0);
    }

    @Override
    public void texSubImage2D(BasicTexture basicTexture, int i, int i2, Bitmap bitmap, int i3, int i4) {
        int target = basicTexture.getTarget();
        GLES20.glBindTexture(target, basicTexture.getId());
        checkError();
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
        mGLId.glGenBuffers(1, this.mTempIntArray, 0);
        checkError();
        int i2 = this.mTempIntArray[0];
        GLES20.glBindBuffer(34962, i2);
        checkError();
        GLES20.glBufferData(34962, buffer.capacity() * i, buffer, 35044);
        checkError();
        return i2;
    }

    public static void checkError() {
        int iGlGetError = GLES20.glGetError();
        if (iGlGetError != 0) {
            Throwable th = new Throwable();
            Log.e(TAG, "GL error: " + iGlGetError, th);
        }
    }

    @Override
    public void recoverFromLightCycle() {
        GLES20.glViewport(0, 0, this.mWidth, this.mHeight);
        GLES20.glDisable(2929);
        GLES20.glBlendFunc(1, 771);
        checkError();
    }

    @Override
    public void getBounds(Rect rect, int i, int i2, int i3, int i4) {
        Matrix.translateM(this.mTempMatrix, 0, this.mMatrices, this.mCurrentMatrixIndex, i, i2, 0.0f);
        Matrix.scaleM(this.mTempMatrix, 0, i3, i4, 1.0f);
        Matrix.multiplyMV(this.mTempMatrix, 16, this.mTempMatrix, 0, BOUNDS_COORDINATES, 0);
        Matrix.multiplyMV(this.mTempMatrix, 20, this.mTempMatrix, 0, BOUNDS_COORDINATES, 4);
        rect.left = Math.round(this.mTempMatrix[16]);
        rect.right = Math.round(this.mTempMatrix[20]);
        rect.top = Math.round(this.mTempMatrix[17]);
        rect.bottom = Math.round(this.mTempMatrix[21]);
        rect.sort();
    }

    @Override
    public GLId getGLId() {
        return mGLId;
    }

    @Override
    public MGLCanvas getMGLCanvas() {
        if (this.mMGLCanvas == null) {
            this.mMGLCanvas = new MGLES20Canvas();
            this.mMGLCanvas.setGenerator((MGLCanvas.Generator) mGLId);
        }
        this.mMGLCanvas.syncState(this.mMatrices, this.mCurrentMatrixIndex, this.mProjectionMatrix, this.mWidth, this.mHeight, this.mScreenWidth, this.mScreenHeight, getAlpha());
        return this.mMGLCanvas;
    }

    public static GLId getCameraGLId() {
        return mGLId;
    }
}
