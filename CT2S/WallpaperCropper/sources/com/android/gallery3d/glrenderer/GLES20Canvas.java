package com.android.gallery3d.glrenderer;

import android.graphics.Bitmap;
import android.graphics.RectF;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.util.Log;
import com.android.gallery3d.util.IntArray;
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

        public ShaderParameter(String name) {
            this.mName = name;
        }
    }

    private static class UniformShaderParameter extends ShaderParameter {
        public UniformShaderParameter(String name) {
            super(name);
        }

        @Override
        public void loadHandle(int program) {
            this.handle = GLES20.glGetUniformLocation(program, this.mName);
            GLES20Canvas.checkError();
        }
    }

    private static class AttributeShaderParameter extends ShaderParameter {
        public AttributeShaderParameter(String name) {
            super(name);
        }

        @Override
        public void loadHandle(int program) {
            this.handle = GLES20.glGetAttribLocation(program, this.mName);
            GLES20Canvas.checkError();
        }
    }

    public GLES20Canvas() {
        Matrix.setIdentityM(this.mTempTextureMatrix, 0);
        Matrix.setIdentityM(this.mMatrices, this.mCurrentMatrixIndex);
        this.mAlphas[this.mCurrentAlphaIndex] = 1.0f;
        this.mTargetTextures.add(null);
        FloatBuffer boxBuffer = createBuffer(BOX_COORDINATES);
        this.mBoxCoordinates = uploadBuffer(boxBuffer);
        int drawVertexShader = loadShader(35633, "uniform mat4 uMatrix;\nattribute vec2 aPosition;\nvoid main() {\n  vec4 pos = vec4(aPosition, 0.0, 1.0);\n  gl_Position = uMatrix * pos;\n}\n");
        int textureVertexShader = loadShader(35633, "uniform mat4 uMatrix;\nuniform mat4 uTextureMatrix;\nattribute vec2 aPosition;\nvarying vec2 vTextureCoord;\nvoid main() {\n  vec4 pos = vec4(aPosition, 0.0, 1.0);\n  gl_Position = uMatrix * pos;\n  vTextureCoord = (uTextureMatrix * pos).xy;\n}\n");
        int meshVertexShader = loadShader(35633, "uniform mat4 uMatrix;\nattribute vec2 aPosition;\nattribute vec2 aTextureCoordinate;\nvarying vec2 vTextureCoord;\nvoid main() {\n  vec4 pos = vec4(aPosition, 0.0, 1.0);\n  gl_Position = uMatrix * pos;\n  vTextureCoord = aTextureCoordinate;\n}\n");
        int drawFragmentShader = loadShader(35632, "precision mediump float;\nuniform vec4 uColor;\nvoid main() {\n  gl_FragColor = uColor;\n}\n");
        int textureFragmentShader = loadShader(35632, "precision mediump float;\nvarying vec2 vTextureCoord;\nuniform float uAlpha;\nuniform sampler2D uTextureSampler;\nvoid main() {\n  gl_FragColor = texture2D(uTextureSampler, vTextureCoord);\n  gl_FragColor *= uAlpha;\n}\n");
        int oesTextureFragmentShader = loadShader(35632, "#extension GL_OES_EGL_image_external : require\nprecision mediump float;\nvarying vec2 vTextureCoord;\nuniform float uAlpha;\nuniform samplerExternalOES uTextureSampler;\nvoid main() {\n  gl_FragColor = texture2D(uTextureSampler, vTextureCoord);\n  gl_FragColor *= uAlpha;\n}\n");
        this.mDrawProgram = assembleProgram(drawVertexShader, drawFragmentShader, this.mDrawParameters);
        this.mTextureProgram = assembleProgram(textureVertexShader, textureFragmentShader, this.mTextureParameters);
        this.mOesTextureProgram = assembleProgram(textureVertexShader, oesTextureFragmentShader, this.mOesTextureParameters);
        this.mMeshProgram = assembleProgram(meshVertexShader, textureFragmentShader, this.mMeshParameters);
        GLES20.glBlendFunc(1, 771);
        checkError();
    }

    private static FloatBuffer createBuffer(float[] values) {
        int size = values.length * 4;
        FloatBuffer buffer = ByteBuffer.allocateDirect(size).order(ByteOrder.nativeOrder()).asFloatBuffer();
        buffer.put(values, 0, values.length).position(0);
        return buffer;
    }

    private int assembleProgram(int vertexShader, int fragmentShader, ShaderParameter[] params) {
        int program = GLES20.glCreateProgram();
        checkError();
        if (program == 0) {
            throw new RuntimeException("Cannot create GL program: " + GLES20.glGetError());
        }
        GLES20.glAttachShader(program, vertexShader);
        checkError();
        GLES20.glAttachShader(program, fragmentShader);
        checkError();
        GLES20.glLinkProgram(program);
        checkError();
        int[] mLinkStatus = this.mTempIntArray;
        GLES20.glGetProgramiv(program, 35714, mLinkStatus, 0);
        if (mLinkStatus[0] != 1) {
            Log.e(TAG, "Could not link program: ");
            Log.e(TAG, GLES20.glGetProgramInfoLog(program));
            GLES20.glDeleteProgram(program);
            program = 0;
        }
        for (ShaderParameter shaderParameter : params) {
            shaderParameter.loadHandle(program);
        }
        return program;
    }

    private static int loadShader(int type, String shaderCode) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, shaderCode);
        checkError();
        GLES20.glCompileShader(shader);
        checkError();
        return shader;
    }

    public void setSize(int width, int height) {
        this.mWidth = width;
        this.mHeight = height;
        GLES20.glViewport(0, 0, this.mWidth, this.mHeight);
        checkError();
        Matrix.setIdentityM(this.mMatrices, this.mCurrentMatrixIndex);
        Matrix.orthoM(this.mProjectionMatrix, 0, 0.0f, width, 0.0f, height, -1.0f, 1.0f);
        if (getTargetTexture() == null) {
            this.mScreenWidth = width;
            this.mScreenHeight = height;
            Matrix.translateM(this.mMatrices, this.mCurrentMatrixIndex, 0.0f, height, 0.0f);
            Matrix.scaleM(this.mMatrices, this.mCurrentMatrixIndex, 1.0f, -1.0f, 1.0f);
        }
    }

    public void clearBuffer() {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        checkError();
        GLES20.glClear(16384);
        checkError();
    }

    public float getAlpha() {
        return this.mAlphas[this.mCurrentAlphaIndex];
    }

    @Override
    public void translate(float x, float y) {
        int index = this.mCurrentMatrixIndex;
        float[] m = this.mMatrices;
        int i = index + 12;
        m[i] = m[i] + (m[index + 0] * x) + (m[index + 4] * y);
        int i2 = index + 13;
        m[i2] = m[i2] + (m[index + 1] * x) + (m[index + 5] * y);
        int i3 = index + 14;
        m[i3] = m[i3] + (m[index + 2] * x) + (m[index + 6] * y);
        int i4 = index + 15;
        m[i4] = m[i4] + (m[index + 3] * x) + (m[index + 7] * y);
    }

    public void scale(float sx, float sy, float sz) {
        Matrix.scaleM(this.mMatrices, this.mCurrentMatrixIndex, sx, sy, sz);
    }

    @Override
    public void rotate(float angle, float x, float y, float z) {
        if (angle != 0.0f) {
            float[] temp = this.mTempMatrix;
            Matrix.setRotateM(temp, 0, angle, x, y, z);
            float[] matrix = this.mMatrices;
            int index = this.mCurrentMatrixIndex;
            Matrix.multiplyMM(temp, 16, matrix, index, temp, 0);
            System.arraycopy(temp, 16, matrix, index, 16);
        }
    }

    @Override
    public void save(int saveFlags) {
        boolean saveAlpha = (saveFlags & 1) == 1;
        if (saveAlpha) {
            float currentAlpha = getAlpha();
            this.mCurrentAlphaIndex++;
            if (this.mAlphas.length <= this.mCurrentAlphaIndex) {
                this.mAlphas = Arrays.copyOf(this.mAlphas, this.mAlphas.length * 2);
            }
            this.mAlphas[this.mCurrentAlphaIndex] = currentAlpha;
        }
        boolean saveMatrix = (saveFlags & 2) == 2;
        if (saveMatrix) {
            int currentIndex = this.mCurrentMatrixIndex;
            this.mCurrentMatrixIndex += 16;
            if (this.mMatrices.length <= this.mCurrentMatrixIndex) {
                this.mMatrices = Arrays.copyOf(this.mMatrices, this.mMatrices.length * 2);
            }
            System.arraycopy(this.mMatrices, currentIndex, this.mMatrices, this.mCurrentMatrixIndex, 16);
        }
        this.mSaveFlags.add(saveFlags);
    }

    @Override
    public void restore() {
        int restoreFlags = this.mSaveFlags.removeLast();
        boolean restoreAlpha = (restoreFlags & 1) == 1;
        if (restoreAlpha) {
            this.mCurrentAlphaIndex--;
        }
        boolean restoreMatrix = (restoreFlags & 2) == 2;
        if (restoreMatrix) {
            this.mCurrentMatrixIndex -= 16;
        }
    }

    private void enableBlending(boolean enableBlending) {
        if (enableBlending) {
            GLES20.glEnable(3042);
            checkError();
        } else {
            GLES20.glDisable(3042);
            checkError();
        }
    }

    private void setPosition(ShaderParameter[] params, int offset) {
        GLES20.glBindBuffer(34962, this.mBoxCoordinates);
        checkError();
        GLES20.glVertexAttribPointer(params[0].handle, 2, 5126, false, 8, offset * 8);
        checkError();
        GLES20.glBindBuffer(34962, 0);
        checkError();
    }

    private void draw(ShaderParameter[] params, int type, int count, float x, float y, float width, float height) {
        setMatrix(params, x, y, width, height);
        int positionHandle = params[0].handle;
        GLES20.glEnableVertexAttribArray(positionHandle);
        checkError();
        GLES20.glDrawArrays(type, 0, count);
        checkError();
        GLES20.glDisableVertexAttribArray(positionHandle);
        checkError();
    }

    private void setMatrix(ShaderParameter[] params, float x, float y, float width, float height) {
        Matrix.translateM(this.mTempMatrix, 0, this.mMatrices, this.mCurrentMatrixIndex, x, y, 0.0f);
        Matrix.scaleM(this.mTempMatrix, 0, width, height, 1.0f);
        Matrix.multiplyMM(this.mTempMatrix, 16, this.mProjectionMatrix, 0, this.mTempMatrix, 0);
        GLES20.glUniformMatrix4fv(params[1].handle, 1, false, this.mTempMatrix, 16);
        checkError();
    }

    @Override
    public void drawTexture(BasicTexture texture, int x, int y, int width, int height) {
        if (width > 0 && height > 0) {
            copyTextureCoordinates(texture, this.mTempSourceRect);
            this.mTempTargetRect.set(x, y, x + width, y + height);
            convertCoordinate(this.mTempSourceRect, this.mTempTargetRect, texture);
            drawTextureRect(texture, this.mTempSourceRect, this.mTempTargetRect);
        }
    }

    private static void copyTextureCoordinates(BasicTexture texture, RectF outRect) {
        int left = 0;
        int top = 0;
        int right = texture.getWidth();
        int bottom = texture.getHeight();
        if (texture.hasBorder()) {
            left = 1;
            top = 1;
            right--;
            bottom--;
        }
        outRect.set(left, top, right, bottom);
    }

    @Override
    public void drawTexture(BasicTexture texture, RectF source, RectF target) {
        if (target.width() > 0.0f && target.height() > 0.0f) {
            this.mTempSourceRect.set(source);
            this.mTempTargetRect.set(target);
            convertCoordinate(this.mTempSourceRect, this.mTempTargetRect, texture);
            drawTextureRect(texture, this.mTempSourceRect, this.mTempTargetRect);
        }
    }

    private void drawTextureRect(BasicTexture texture, RectF source, RectF target) {
        setTextureMatrix(source);
        drawTextureRect(texture, this.mTempTextureMatrix, target);
    }

    private void setTextureMatrix(RectF source) {
        this.mTempTextureMatrix[0] = source.width();
        this.mTempTextureMatrix[5] = source.height();
        this.mTempTextureMatrix[12] = source.left;
        this.mTempTextureMatrix[13] = source.top;
    }

    private static void convertCoordinate(RectF source, RectF target, BasicTexture texture) {
        int width = texture.getWidth();
        int height = texture.getHeight();
        int texWidth = texture.getTextureWidth();
        int texHeight = texture.getTextureHeight();
        source.left /= texWidth;
        source.right /= texWidth;
        source.top /= texHeight;
        source.bottom /= texHeight;
        float xBound = width / texWidth;
        if (source.right > xBound) {
            target.right = target.left + ((target.width() * (xBound - source.left)) / source.width());
            source.right = xBound;
        }
        float yBound = height / texHeight;
        if (source.bottom > yBound) {
            target.bottom = target.top + ((target.height() * (yBound - source.top)) / source.height());
            source.bottom = yBound;
        }
    }

    private void drawTextureRect(BasicTexture texture, float[] textureMatrix, RectF target) {
        ShaderParameter[] params = prepareTexture(texture);
        setPosition(params, 0);
        GLES20.glUniformMatrix4fv(params[2].handle, 1, false, textureMatrix, 0);
        checkError();
        if (texture.isFlippedVertically()) {
            save(2);
            translate(0.0f, target.centerY());
            scale(1.0f, -1.0f, 1.0f);
            translate(0.0f, -target.centerY());
        }
        draw(params, 5, 4, target.left, target.top, target.width(), target.height());
        if (texture.isFlippedVertically()) {
            restore();
        }
        this.mCountTextureRect++;
    }

    private ShaderParameter[] prepareTexture(BasicTexture texture) {
        ShaderParameter[] params;
        int program;
        if (texture.getTarget() == 3553) {
            params = this.mTextureParameters;
            program = this.mTextureProgram;
        } else {
            params = this.mOesTextureParameters;
            program = this.mOesTextureProgram;
        }
        prepareTexture(texture, program, params);
        return params;
    }

    private void prepareTexture(BasicTexture texture, int program, ShaderParameter[] params) {
        GLES20.glUseProgram(program);
        checkError();
        enableBlending(!texture.isOpaque() || getAlpha() < 0.95f);
        GLES20.glActiveTexture(33984);
        checkError();
        texture.onBind(this);
        GLES20.glBindTexture(texture.getTarget(), texture.getId());
        checkError();
        GLES20.glUniform1i(params[3].handle, 0);
        checkError();
        GLES20.glUniform1f(params[4].handle, getAlpha());
        checkError();
    }

    @Override
    public boolean unloadTexture(BasicTexture texture) {
        boolean unload = texture.isLoaded();
        if (unload) {
            synchronized (this.mUnboundTextures) {
                this.mUnboundTextures.add(texture.getId());
            }
        }
        return unload;
    }

    private RawTexture getTargetTexture() {
        return this.mTargetTextures.get(this.mTargetTextures.size() - 1);
    }

    @Override
    public void setTextureParameters(BasicTexture texture) {
        int target = texture.getTarget();
        GLES20.glBindTexture(target, texture.getId());
        checkError();
        GLES20.glTexParameteri(target, 10242, 33071);
        GLES20.glTexParameteri(target, 10243, 33071);
        GLES20.glTexParameterf(target, 10241, 9729.0f);
        GLES20.glTexParameterf(target, 10240, 9729.0f);
    }

    @Override
    public void initializeTextureSize(BasicTexture texture, int format, int type) {
        int target = texture.getTarget();
        GLES20.glBindTexture(target, texture.getId());
        checkError();
        int width = texture.getTextureWidth();
        int height = texture.getTextureHeight();
        GLES20.glTexImage2D(target, 0, format, width, height, 0, format, type, null);
    }

    @Override
    public void initializeTexture(BasicTexture texture, Bitmap bitmap) {
        int target = texture.getTarget();
        GLES20.glBindTexture(target, texture.getId());
        checkError();
        GLUtils.texImage2D(target, 0, bitmap, 0);
    }

    @Override
    public void texSubImage2D(BasicTexture texture, int xOffset, int yOffset, Bitmap bitmap, int format, int type) {
        int target = texture.getTarget();
        GLES20.glBindTexture(target, texture.getId());
        checkError();
        GLUtils.texSubImage2D(target, 0, xOffset, yOffset, bitmap, format, type);
    }

    public int uploadBuffer(FloatBuffer buf) {
        return uploadBuffer(buf, 4);
    }

    private int uploadBuffer(Buffer buffer, int elementSize) {
        mGLId.glGenBuffers(1, this.mTempIntArray, 0);
        checkError();
        int bufferId = this.mTempIntArray[0];
        GLES20.glBindBuffer(34962, bufferId);
        checkError();
        GLES20.glBufferData(34962, buffer.capacity() * elementSize, buffer, 35044);
        checkError();
        return bufferId;
    }

    public static void checkError() {
        int error = GLES20.glGetError();
        if (error != 0) {
            Throwable t = new Throwable();
            Log.e(TAG, "GL error: " + error, t);
        }
    }

    @Override
    public GLId getGLId() {
        return mGLId;
    }
}
