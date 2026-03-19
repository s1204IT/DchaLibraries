package com.mediatek.gallerybasic.gl;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.RectF;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import com.mediatek.gallerybasic.gl.MGLCanvas;
import com.mediatek.gallerybasic.util.Log;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Arrays;

public class MGLES20Canvas implements MGLCanvas {
    private static final String ALPHA_UNIFORM = "uAlpha";
    private static final int BYTE_ARGB = 4;
    private static final String COLOR_UNIFORM = "uColor";
    private static final int COORDS_PER_VERTEX = 2;
    private static final int COORDS_PER_VERTEX_3 = 3;
    private static final int COUNT_FILL_VERTEX = 4;
    private static final int COUNT_LINE_VERTEX = 2;
    private static final int COUNT_RECT_VERTEX = 4;
    private static final String DRAW_FRAGMENT_SHADER = "precision mediump float;\nuniform vec4 uColor;\nvoid main() {\n  gl_FragColor = uColor;\n}\n";
    private static final String DRAW_VERTEX_SHADER = "uniform mat4 uMatrix;\nattribute vec2 aPosition;\nvoid main() {\n  vec4 pos = vec4(aPosition, 0.0, 1.0);\n  gl_Position = uMatrix * pos;\n}\n";
    private static final int FLOAT_SIZE = 4;
    private static final int INDEX_ALPHA = 4;
    private static final int INDEX_COLOR = 2;
    private static final int INDEX_MATRIX = 1;
    private static final int INDEX_POSITION = 0;
    private static final int INDEX_TEXTURE_COORD = 2;
    private static final int INDEX_TEXTURE_MATRIX = 2;
    private static final int INDEX_TEXTURE_SAMPLER = 3;
    private static final int INITIAL_RESTORE_STATE_SIZE = 8;
    private static final int INVALID_FRAME_BUFFER = -1;
    private static final int MATRIX_SIZE = 16;
    private static final String MATRIX_UNIFORM = "uMatrix";
    private static final String MESH_VERTEX_3D_SHADER = "uniform mat4 uMatrix;\nattribute vec3 aPosition;\nattribute vec2 aTextureCoordinate;\nvarying vec2 vTextureCoord;\nvoid main() {\n  vec4 pos = vec4(aPosition, 1.0);\n  gl_Position = uMatrix * pos;\n  vTextureCoord = aTextureCoordinate;\n}\n";
    private static final String MESH_VERTEX_SHADER = "uniform mat4 uMatrix;\nattribute vec2 aPosition;\nattribute vec2 aTextureCoordinate;\nvarying vec2 vTextureCoord;\nvoid main() {\n  vec4 pos = vec4(aPosition, 0.0, 1.0);\n  gl_Position = uMatrix * pos;\n  vTextureCoord = aTextureCoordinate;\n}\n";
    private static final String OES_TEXTURE_FRAGMENT_SHADER = "#extension GL_OES_EGL_image_external : require\nprecision mediump float;\nvarying vec2 vTextureCoord;\nuniform float uAlpha;\nuniform samplerExternalOES uTextureSampler;\nvoid main() {\n  gl_FragColor = texture2D(uTextureSampler, vTextureCoord);\n  gl_FragColor *= uAlpha;\n}\n";
    private static final int OFFSET_DRAW_LINE = 4;
    private static final int OFFSET_DRAW_RECT = 6;
    private static final int OFFSET_FILL_RECT = 0;
    private static final float OPAQUE_ALPHA = 0.95f;
    private static final String POSITION_ATTRIBUTE = "aPosition";
    private static final String TAG = "MtkGallery2/MGLES20Canvas";
    private static final String TEXTURE_COORD_ATTRIBUTE = "aTextureCoordinate";
    private static final String TEXTURE_FRAGMENT_SHADER = "precision mediump float;\nvarying vec2 vTextureCoord;\nuniform float uAlpha;\nuniform sampler2D uTextureSampler;\nvoid main() {\n  gl_FragColor = texture2D(uTextureSampler, vTextureCoord);\n  gl_FragColor *= uAlpha;\n}\n";
    private static final String TEXTURE_MATRIX_UNIFORM = "uTextureMatrix";
    private static final String TEXTURE_SAMPLER_UNIFORM = "uTextureSampler";
    private static final String TEXTURE_VERTEX_SHADER = "uniform mat4 uMatrix;\nuniform mat4 uTextureMatrix;\nattribute vec2 aPosition;\nvarying vec2 vTextureCoord;\nvoid main() {\n  vec4 pos = vec4(aPosition, 0.0, 1.0);\n  gl_Position = uMatrix * pos;\n  vTextureCoord = (uTextureMatrix * pos).xy;\n}\n";
    private static final int VERTEX_STRIDE = 8;
    private static final int VERTEX_STRIDE_3 = 12;
    private int mBoxCoordinates;
    private int mDrawProgram;
    private int mHeight;
    private int mMesh3DProgram;
    private int mMeshProgram;
    private int mOesTextureProgram;
    private int mScreenHeight;
    private int mScreenWidth;
    private int mTextureProgram;
    private int mWidth;
    private static final float[] BOX_COORDINATES = {0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f, 1.0f, 0.0f};
    private static final float[] BOUNDS_COORDINATES = {0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f, 0.0f, 1.0f};
    private float[] mMatrices = new float[128];
    private float[] mAlphas = new float[8];
    private IntArray mSaveFlags = new IntArray();
    private int mCurrentAlphaIndex = 0;
    private int mCurrentMatrixIndex = 0;
    private float[] mProjectionMatrix = new float[16];
    ShaderParameter[] mDrawParameters = {new AttributeShaderParameter(POSITION_ATTRIBUTE), new UniformShaderParameter(MATRIX_UNIFORM), new UniformShaderParameter(COLOR_UNIFORM)};
    ShaderParameter[] mTextureParameters = {new AttributeShaderParameter(POSITION_ATTRIBUTE), new UniformShaderParameter(MATRIX_UNIFORM), new UniformShaderParameter(TEXTURE_MATRIX_UNIFORM), new UniformShaderParameter(TEXTURE_SAMPLER_UNIFORM), new UniformShaderParameter(ALPHA_UNIFORM)};
    ShaderParameter[] mOesTextureParameters = {new AttributeShaderParameter(POSITION_ATTRIBUTE), new UniformShaderParameter(MATRIX_UNIFORM), new UniformShaderParameter(TEXTURE_MATRIX_UNIFORM), new UniformShaderParameter(TEXTURE_SAMPLER_UNIFORM), new UniformShaderParameter(ALPHA_UNIFORM)};
    ShaderParameter[] mMeshParameters = {new AttributeShaderParameter(POSITION_ATTRIBUTE), new UniformShaderParameter(MATRIX_UNIFORM), new AttributeShaderParameter(TEXTURE_COORD_ATTRIBUTE), new UniformShaderParameter(TEXTURE_SAMPLER_UNIFORM), new UniformShaderParameter(ALPHA_UNIFORM)};
    private final IntArray mUnboundTextures = new IntArray();
    private final IntArray mDeleteBuffers = new IntArray();
    private int mCountDrawMesh = 0;
    private int mCountTextureRect = 0;
    private int mCountFillRect = 0;
    private int mCountDrawLine = 0;
    private int[] mFrameBuffer = new int[1];
    private ArrayList<MRawTexture> mTargetTextures = new ArrayList<>();
    private float[] mTempMatrix = new float[32];
    private float[] mTempColor = new float[4];
    private RectF mTempSourceRect = new RectF();
    private RectF mTempTargetRect = new RectF();
    private float[] mTempTextureMatrix = new float[16];
    private int[] mTempIntArray = new int[1];
    MGLCanvas.Generator mGenerator = new GLES20Generator();

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
            MGLES20Canvas.checkError();
        }
    }

    private static class AttributeShaderParameter extends ShaderParameter {
        public AttributeShaderParameter(String str) {
            super(str);
        }

        @Override
        public void loadHandle(int i) {
            this.handle = GLES20.glGetAttribLocation(i, this.mName);
            MGLES20Canvas.checkError();
        }
    }

    public void syncState(float[] fArr, int i, float[] fArr2, int i2, int i3, int i4, int i5, float f) {
        reset();
        Matrix.setIdentityM(this.mTempTextureMatrix, 0);
        this.mAlphas[this.mCurrentAlphaIndex] = f;
        this.mMatrices = (float[]) fArr.clone();
        this.mCurrentMatrixIndex = i;
        this.mProjectionMatrix = (float[]) fArr2.clone();
        this.mWidth = i2;
        this.mHeight = i3;
        this.mScreenWidth = i4;
        this.mScreenHeight = i5;
    }

    private void reset() {
        this.mAlphas = new float[8];
        this.mCountDrawLine = 0;
        this.mCountDrawMesh = 0;
        this.mCountFillRect = 0;
        this.mCountTextureRect = 0;
        this.mCurrentAlphaIndex = 0;
        this.mCurrentMatrixIndex = 0;
        this.mHeight = 0;
        this.mMatrices = new float[128];
        this.mProjectionMatrix = new float[16];
        this.mSaveFlags = new IntArray();
        this.mScreenHeight = 0;
        this.mScreenWidth = 0;
        this.mTempColor = new float[4];
        this.mTempIntArray = new int[1];
        this.mTempMatrix = new float[32];
        this.mTempSourceRect = new RectF();
        this.mTempTargetRect = new RectF();
        this.mTempTextureMatrix = new float[16];
    }

    private void init() {
        Matrix.setIdentityM(this.mTempTextureMatrix, 0);
        Matrix.setIdentityM(this.mMatrices, this.mCurrentMatrixIndex);
        this.mAlphas[this.mCurrentAlphaIndex] = 1.0f;
        this.mTargetTextures.add(null);
        this.mBoxCoordinates = uploadBuffer(createBuffer(BOX_COORDINATES));
        int iLoadShader = loadShader(35633, DRAW_VERTEX_SHADER);
        int iLoadShader2 = loadShader(35633, TEXTURE_VERTEX_SHADER);
        int iLoadShader3 = loadShader(35633, MESH_VERTEX_SHADER);
        int iLoadShader4 = loadShader(35632, DRAW_FRAGMENT_SHADER);
        int iLoadShader5 = loadShader(35632, TEXTURE_FRAGMENT_SHADER);
        int iLoadShader6 = loadShader(35632, OES_TEXTURE_FRAGMENT_SHADER);
        this.mDrawProgram = assembleProgram(iLoadShader, iLoadShader4, this.mDrawParameters);
        this.mTextureProgram = assembleProgram(iLoadShader2, iLoadShader5, this.mTextureParameters);
        this.mOesTextureProgram = assembleProgram(iLoadShader2, iLoadShader6, this.mOesTextureParameters);
        this.mMeshProgram = assembleProgram(iLoadShader3, iLoadShader5, this.mMeshParameters);
        GLES20.glBlendFunc(1, 771);
        checkError();
        this.mMesh3DProgram = assembleProgram(loadShader(35633, MESH_VERTEX_3D_SHADER), loadShader(35632, TEXTURE_FRAGMENT_SHADER), this.mMeshParameters);
        checkError();
        this.mFrameBuffer[0] = -1;
    }

    public MGLES20Canvas() {
        init();
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
            Log.e(TAG, "<assembleProgram> Could not link program: ");
            Log.e(TAG, "<assembleProgram>" + GLES20.glGetProgramInfoLog(iGlCreateProgram));
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
    public void drawTexture(MBasicTexture mBasicTexture, int i, int i2, int i3, int i4) {
        if (i3 <= 0 || i4 <= 0) {
            return;
        }
        copyTextureCoordinates(mBasicTexture, this.mTempSourceRect);
        this.mTempTargetRect.set(i, i2, i + i3, i2 + i4);
        convertCoordinate(this.mTempSourceRect, this.mTempTargetRect, mBasicTexture);
        drawTextureRect(mBasicTexture, this.mTempSourceRect, this.mTempTargetRect);
    }

    private static void copyTextureCoordinates(MBasicTexture mBasicTexture, RectF rectF) {
        int width = mBasicTexture.getWidth();
        int height = mBasicTexture.getHeight();
        int i = 1;
        int i2 = 0;
        if (mBasicTexture.hasBorder()) {
            width--;
            height--;
            i2 = 1;
        } else {
            i = 0;
        }
        rectF.set(i, i2, width, height);
    }

    @Override
    public void drawTexture(MBasicTexture mBasicTexture, RectF rectF, RectF rectF2) {
        if (rectF2.width() <= 0.0f || rectF2.height() <= 0.0f) {
            return;
        }
        this.mTempSourceRect.set(rectF);
        this.mTempTargetRect.set(rectF2);
        convertCoordinate(this.mTempSourceRect, this.mTempTargetRect, mBasicTexture);
        drawTextureRect(mBasicTexture, this.mTempSourceRect, this.mTempTargetRect);
    }

    @Override
    public void drawTexture(MBasicTexture mBasicTexture, float[] fArr, int i, int i2, int i3, int i4) {
        if (i3 <= 0 || i4 <= 0) {
            return;
        }
        this.mTempTargetRect.set(i, i2, i + i3, i2 + i4);
        drawTextureRect(mBasicTexture, fArr, this.mTempTargetRect);
    }

    private void drawTextureRect(MBasicTexture mBasicTexture, RectF rectF, RectF rectF2) {
        setTextureMatrix(rectF);
        drawTextureRect(mBasicTexture, this.mTempTextureMatrix, rectF2);
    }

    private void setTextureMatrix(RectF rectF) {
        this.mTempTextureMatrix[0] = rectF.width();
        this.mTempTextureMatrix[5] = rectF.height();
        this.mTempTextureMatrix[12] = rectF.left;
        this.mTempTextureMatrix[13] = rectF.top;
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

    private void drawTextureRect(MBasicTexture mBasicTexture, float[] fArr, RectF rectF) {
        ShaderParameter[] shaderParameterArrPrepareTexture = prepareTexture(mBasicTexture);
        setPosition(shaderParameterArrPrepareTexture, 0);
        GLES20.glUniformMatrix4fv(shaderParameterArrPrepareTexture[2].handle, 1, false, fArr, 0);
        checkError();
        if (mBasicTexture.isFlippedVertically()) {
            save(2);
            translate(0.0f, rectF.centerY());
            scale(1.0f, -1.0f, 1.0f);
            translate(0.0f, -rectF.centerY());
        }
        draw(shaderParameterArrPrepareTexture, 5, 4, rectF.left, rectF.top, rectF.width(), rectF.height());
        if (mBasicTexture.isFlippedVertically()) {
            restore();
        }
        this.mCountTextureRect++;
    }

    private ShaderParameter[] prepareTexture(MBasicTexture mBasicTexture) {
        ShaderParameter[] shaderParameterArr;
        int i;
        if (mBasicTexture.getTarget() == 3553) {
            shaderParameterArr = this.mTextureParameters;
            i = this.mTextureProgram;
        } else {
            shaderParameterArr = this.mOesTextureParameters;
            i = this.mOesTextureProgram;
        }
        prepareTexture(mBasicTexture, i, shaderParameterArr);
        return shaderParameterArr;
    }

    private void prepareTexture(MBasicTexture mBasicTexture, int i, ShaderParameter[] shaderParameterArr) {
        GLES20.glUseProgram(i);
        checkError();
        enableBlending(!mBasicTexture.isOpaque() || getAlpha() < OPAQUE_ALPHA);
        GLES20.glActiveTexture(33984);
        checkError();
        mBasicTexture.onBind(this);
        GLES20.glBindTexture(mBasicTexture.getTarget(), mBasicTexture.getId());
        checkError();
        GLES20.glUniform1i(shaderParameterArr[3].handle, 0);
        checkError();
        GLES20.glUniform1f(shaderParameterArr[4].handle, getAlpha());
        checkError();
    }

    @Override
    public void drawMesh(MBasicTexture mBasicTexture, int i, int i2, int i3, int i4, int i5, int i6) {
        prepareTexture(mBasicTexture, this.mMeshProgram, this.mMeshParameters);
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
    public void drawMixed(MBasicTexture mBasicTexture, int i, float f, int i2, int i3, int i4, int i5) {
        copyTextureCoordinates(mBasicTexture, this.mTempSourceRect);
        this.mTempTargetRect.set(i2, i3, i2 + i4, i3 + i5);
        drawMixed(mBasicTexture, i, f, this.mTempSourceRect, this.mTempTargetRect);
    }

    @Override
    public void drawMixed(MBasicTexture mBasicTexture, int i, float f, RectF rectF, RectF rectF2) {
        if (rectF2.width() <= 0.0f || rectF2.height() <= 0.0f) {
            return;
        }
        save(1);
        float alpha = getAlpha();
        float fMin = Math.min(1.0f, Math.max(0.0f, f));
        setAlpha((1.0f - fMin) * alpha);
        drawTexture(mBasicTexture, rectF, rectF2);
        setAlpha(fMin * alpha);
        fillRect(rectF2.left, rectF2.top, rectF2.width(), rectF2.height(), i);
        restore();
    }

    @Override
    public boolean unloadTexture(MBasicTexture mBasicTexture) {
        boolean zIsLoaded = mBasicTexture.isLoaded();
        if (zIsLoaded) {
            synchronized (this.mUnboundTextures) {
                this.mUnboundTextures.add(mBasicTexture.getId());
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
    public void dumpStatisticsAndClear() {
        String str = String.format("MESH:%d, TEX_RECT:%d, FILL_RECT:%d, LINE:%d", Integer.valueOf(this.mCountDrawMesh), Integer.valueOf(this.mCountTextureRect), Integer.valueOf(this.mCountFillRect), Integer.valueOf(this.mCountDrawLine));
        this.mCountDrawMesh = 0;
        this.mCountTextureRect = 0;
        this.mCountFillRect = 0;
        this.mCountDrawLine = 0;
        Log.d(TAG, "<dumpStatisticsAndClear>" + str);
    }

    @Override
    public void endRenderTarget() {
        setRenderTarget(this.mTargetTextures.remove(this.mTargetTextures.size() - 1), getTargetTexture());
        restore();
    }

    @Override
    public void beginRenderTarget(MRawTexture mRawTexture) {
        save();
        MRawTexture targetTexture = getTargetTexture();
        this.mTargetTextures.add(mRawTexture);
        setRenderTarget(targetTexture, mRawTexture);
    }

    private MRawTexture getTargetTexture() {
        return this.mTargetTextures.get(this.mTargetTextures.size() - 1);
    }

    private void setRenderTarget(MBasicTexture mBasicTexture, MRawTexture mRawTexture) {
        if (mBasicTexture == null && mRawTexture != null) {
            if (this.mFrameBuffer[0] == -1) {
                GLES20.glGenFramebuffers(1, this.mFrameBuffer, 0);
                checkError();
            }
            GLES20.glBindFramebuffer(36160, this.mFrameBuffer[0]);
            checkError();
        } else if (mBasicTexture != null && mRawTexture == null) {
            GLES20.glBindFramebuffer(36160, 0);
            checkError();
        }
        if (mRawTexture == null) {
            setSize(this.mScreenWidth, this.mScreenHeight);
            return;
        }
        setSize(mRawTexture.getWidth(), mRawTexture.getHeight());
        if (!mRawTexture.isLoaded()) {
            mRawTexture.prepare(this);
        }
        GLES20.glFramebufferTexture2D(36160, 36064, mRawTexture.getTarget(), mRawTexture.getId(), 0);
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
    public void setTextureParameters(MBasicTexture mBasicTexture) {
        int target = mBasicTexture.getTarget();
        GLES20.glBindTexture(target, mBasicTexture.getId());
        checkError();
        GLES20.glTexParameteri(target, 10242, 33071);
        GLES20.glTexParameteri(target, 10243, 33071);
        GLES20.glTexParameterf(target, 10241, 9729.0f);
        GLES20.glTexParameterf(target, 10240, 9729.0f);
    }

    @Override
    public void initializeTextureSize(MBasicTexture mBasicTexture, int i, int i2) {
        int target = mBasicTexture.getTarget();
        GLES20.glBindTexture(target, mBasicTexture.getId());
        checkError();
        GLES20.glTexImage2D(target, 0, i, mBasicTexture.getTextureWidth(), mBasicTexture.getTextureHeight(), 0, i, i2, null);
    }

    @Override
    public void initializeTexture(MBasicTexture mBasicTexture, Bitmap bitmap) {
        int target = mBasicTexture.getTarget();
        GLES20.glBindTexture(target, mBasicTexture.getId());
        checkError();
        GLUtils.texImage2D(target, 0, bitmap, 0);
    }

    @Override
    public void texSubImage2D(MBasicTexture mBasicTexture, int i, int i2, Bitmap bitmap, int i3, int i4) {
        int target = mBasicTexture.getTarget();
        GLES20.glBindTexture(target, mBasicTexture.getId());
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
        glGenBuffers(1, this.mTempIntArray, 0);
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
            Log.e(TAG, "<checkError> GL error: " + iGlGetError, new Throwable());
        }
    }

    private static void printMatrix(String str, float[] fArr, int i) {
        StringBuilder sb = new StringBuilder(str);
        for (int i2 = 0; i2 < 16; i2++) {
            sb.append(' ');
            if (i2 % 4 == 0) {
                sb.append('\n');
            }
            sb.append(fArr[i + i2]);
        }
        Log.v(TAG, "<printMatrix> " + sb.toString());
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
    public void setPerspective(float f, float f2, float f3, float f4) {
        Matrix.perspectiveM(this.mProjectionMatrix, 0, f, f2, f3, f4);
        checkError();
    }

    @Override
    public void setLookAt(float f, float f2, float f3, float f4, float f5, float f6, float f7, float f8, float f9) {
        float[] fArr = this.mTempMatrix;
        Matrix.setLookAtM(fArr, 0, f, f2, f3, f4, f5, f6, f7, f8, f9);
        float[] fArr2 = this.mMatrices;
        int i = this.mCurrentMatrixIndex;
        Matrix.multiplyMM(fArr, 16, fArr2, i, fArr, 0);
        System.arraycopy(fArr, 16, fArr2, i, 16);
    }

    @Override
    public void drawMesh(MBasicTexture mBasicTexture, float f, float f2, float f3, float f4, int i, int i2, int i3) {
        prepareTexture(mBasicTexture, this.mMesh3DProgram, this.mMeshParameters);
        GLES20.glBindBuffer(34962, i);
        checkError();
        int i4 = this.mMeshParameters[0].handle;
        GLES20.glVertexAttribPointer(i4, 3, 5126, false, 12, 0);
        checkError();
        GLES20.glBindBuffer(34962, i2);
        checkError();
        int i5 = this.mMeshParameters[2].handle;
        GLES20.glVertexAttribPointer(i5, 2, 5126, false, 8, 0);
        checkError();
        GLES20.glEnableVertexAttribArray(i4);
        checkError();
        checkError();
        GLES20.glEnableVertexAttribArray(i5);
        checkError();
        rotate(f, f2, f3, f4);
        setMatrix(this.mMeshParameters, 0.0f, 0.0f, 1.0f, 1.0f);
        GLES20.glDrawArrays(4, 0, i3);
        checkError();
        GLES20.glDisableVertexAttribArray(i4);
        checkError();
        GLES20.glDisableVertexAttribArray(i5);
        checkError();
        GLES20.glBindBuffer(34963, 0);
        checkError();
        this.mCountDrawMesh++;
    }

    @Override
    public void readPixels(int i, int i2, int i3, int i4, int i5, int i6, Buffer buffer) {
        GLES20.glReadPixels(i, i2, i3, i4, i5, i6, buffer);
        checkError();
    }

    @Override
    public int getGLVersion() {
        return 2;
    }

    @Override
    public void glDeleteTextures(int i, int[] iArr, int i2) {
        GLES20.glDeleteTextures(i, iArr, i2);
        checkError();
    }

    @Override
    public void glDeleteBuffers(int i, int[] iArr, int i2) {
        GLES20.glDeleteBuffers(i, iArr, i2);
        checkError();
    }

    @Override
    public void glDeleteFramebuffers(int i, int[] iArr, int i2) {
        GLES20.glDeleteFramebuffers(i, iArr, i2);
        checkError();
    }

    class GLES20Generator implements MGLCanvas.Generator {
        GLES20Generator() {
        }

        @Override
        public int generateTexture() {
            int[] iArr = new int[1];
            GLES20.glGenTextures(1, iArr, 0);
            MGLES20Canvas.checkError();
            return iArr[0];
        }

        @Override
        public void glGenBuffers(int i, int[] iArr, int i2) {
            GLES20.glGenBuffers(i, iArr, i2);
            MGLES20Canvas.checkError();
        }
    }

    @Override
    public int generateTexture() {
        return this.mGenerator.generateTexture();
    }

    @Override
    public void glGenBuffers(int i, int[] iArr, int i2) {
        this.mGenerator.glGenBuffers(i, iArr, i2);
        checkError();
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
        ByteBuffer byteBufferAllocate = ByteBuffer.allocate(i2 * i3 * 4);
        GLES20.glReadPixels(0, 0, i2, i3, 6408, 5121, byteBufferAllocate);
        Bitmap bitmapCreateBitmap = Bitmap.createBitmap(i2, i3, Bitmap.Config.ARGB_8888);
        bitmapCreateBitmap.copyPixelsFromBuffer(byteBufferAllocate);
        return bitmapCreateBitmap;
    }
}
