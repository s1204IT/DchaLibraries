package com.android.gallery3d.glrenderer;

import android.graphics.Bitmap;
import android.graphics.RectF;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.util.Log;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Arrays;

public class GLES20Canvas implements GLCanvas {
    private int mBoxCoordinates;
    private int mHeight;
    private int mTextureProgram;
    private int mWidth;
    private static final String TAG = GLES20Canvas.class.getSimpleName();
    private static final float[] BOX_COORDINATES = {0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f, 1.0f, 0.0f};
    private static final GLId mGLId = new GLES20IdImpl();
    private float[] mMatrices = new float[128];
    private IntArray mSaveFlags = new IntArray();
    private int mCurrentMatrixIndex = 0;
    private float[] mProjectionMatrix = new float[16];
    private ShaderParameter[] mTextureParameters = {new AttributeShaderParameter("aPosition"), new UniformShaderParameter("uMatrix"), new UniformShaderParameter("uTextureMatrix"), new UniformShaderParameter("uTextureSampler"), new UniformShaderParameter("uAlpha")};
    private final IntArray mUnboundTextures = new IntArray();
    private final float[] mTempMatrix = new float[32];
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
        this.mBoxCoordinates = uploadBuffer(createBuffer(BOX_COORDINATES));
        this.mTextureProgram = assembleProgram(loadShader(35633, "uniform mat4 uMatrix;\nuniform mat4 uTextureMatrix;\nattribute vec2 aPosition;\nvarying vec2 vTextureCoord;\nvoid main() {\n  vec4 pos = vec4(aPosition, 0.0, 1.0);\n  gl_Position = uMatrix * pos;\n  vTextureCoord = (uTextureMatrix * pos).xy;\n}\n"), loadShader(35632, "precision mediump float;\nvarying vec2 vTextureCoord;\nuniform float uAlpha;\nuniform sampler2D uTextureSampler;\nvoid main() {\n  gl_FragColor = texture2D(uTextureSampler, vTextureCoord);\n  gl_FragColor *= uAlpha;\n}\n"), this.mTextureParameters);
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

    public void setSize(int i, int i2) {
        this.mWidth = i;
        this.mHeight = i2;
        GLES20.glViewport(0, 0, this.mWidth, this.mHeight);
        checkError();
        Matrix.setIdentityM(this.mMatrices, this.mCurrentMatrixIndex);
        float f = i;
        float f2 = i2;
        Matrix.orthoM(this.mProjectionMatrix, 0, 0.0f, f, 0.0f, f2, -1.0f, 1.0f);
        Matrix.translateM(this.mMatrices, this.mCurrentMatrixIndex, 0.0f, f2, 0.0f);
        Matrix.scaleM(this.mMatrices, this.mCurrentMatrixIndex, 1.0f, -1.0f, 1.0f);
    }

    public void clearBuffer() {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        checkError();
        GLES20.glClear(16384);
        checkError();
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
    public void save(int i) {
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
        if ((this.mSaveFlags.removeLast() & 2) == 2) {
            this.mCurrentMatrixIndex -= 16;
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
        rectF.set(0.0f, 0.0f, basicTexture.getWidth(), basicTexture.getHeight());
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
        draw(shaderParameterArrPrepareTexture, 5, 4, rectF.left, rectF.top, rectF.width(), rectF.height());
    }

    private ShaderParameter[] prepareTexture(BasicTexture basicTexture) {
        ShaderParameter[] shaderParameterArr = this.mTextureParameters;
        prepareTexture(basicTexture, this.mTextureProgram, shaderParameterArr);
        return shaderParameterArr;
    }

    private void prepareTexture(BasicTexture basicTexture, int i, ShaderParameter[] shaderParameterArr) {
        deleteRecycledResources();
        GLES20.glUseProgram(i);
        checkError();
        GLES20.glDisable(3042);
        checkError();
        GLES20.glActiveTexture(33984);
        checkError();
        basicTexture.onBind(this);
        GLES20.glBindTexture(3553, basicTexture.getId());
        checkError();
        GLES20.glUniform1i(shaderParameterArr[3].handle, 0);
        checkError();
        GLES20.glUniform1f(shaderParameterArr[4].handle, 1.0f);
        checkError();
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

    public void deleteRecycledResources() {
        synchronized (this.mUnboundTextures) {
            IntArray intArray = this.mUnboundTextures;
            if (this.mUnboundTextures.size() > 0) {
                mGLId.glDeleteTextures(null, intArray.size(), intArray.getInternalArray(), 0);
                intArray.clear();
            }
        }
    }

    @Override
    public void setTextureParameters(BasicTexture basicTexture) {
        GLES20.glBindTexture(3553, basicTexture.getId());
        checkError();
        GLES20.glTexParameteri(3553, 10242, 33071);
        GLES20.glTexParameteri(3553, 10243, 33071);
        GLES20.glTexParameterf(3553, 10241, 9729.0f);
        GLES20.glTexParameterf(3553, 10240, 9729.0f);
    }

    @Override
    public void initializeTextureSize(BasicTexture basicTexture, int i, int i2) {
        GLES20.glBindTexture(3553, basicTexture.getId());
        checkError();
        GLES20.glTexImage2D(3553, 0, i, basicTexture.getTextureWidth(), basicTexture.getTextureHeight(), 0, i, i2, null);
    }

    @Override
    public void initializeTexture(BasicTexture basicTexture, Bitmap bitmap) {
        GLES20.glBindTexture(3553, basicTexture.getId());
        checkError();
        GLUtils.texImage2D(3553, 0, bitmap, 0);
    }

    @Override
    public void texSubImage2D(BasicTexture basicTexture, int i, int i2, Bitmap bitmap, int i3, int i4) {
        GLES20.glBindTexture(3553, basicTexture.getId());
        checkError();
        GLUtils.texSubImage2D(3553, 0, i, i2, bitmap, i3, i4);
    }

    public int uploadBuffer(FloatBuffer floatBuffer) {
        return uploadBuffer(floatBuffer, 4);
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
    public GLId getGLId() {
        return mGLId;
    }
}
