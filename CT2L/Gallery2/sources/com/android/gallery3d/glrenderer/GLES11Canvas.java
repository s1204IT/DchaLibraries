package com.android.gallery3d.glrenderer;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.RectF;
import android.opengl.GLU;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.util.Log;
import com.android.gallery3d.common.Utils;
import com.android.gallery3d.util.IntArray;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import javax.microedition.khronos.opengles.GL11;
import javax.microedition.khronos.opengles.GL11Ext;
import javax.microedition.khronos.opengles.GL11ExtensionPack;
import junit.framework.Assert;

public class GLES11Canvas implements GLCanvas {
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
    private RawTexture mTargetTexture;
    private static final float[] BOX_COORDINATES = {0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f, 1.0f, 0.0f};
    private static float[] sCropRect = new float[4];
    private static GLId mGLId = new GLES11IdImpl();
    private final float[] mMatrixValues = new float[16];
    private final float[] mTextureMatrixValues = new float[16];
    private final float[] mMapPointsBuffer = new float[4];
    private final float[] mTextureColor = new float[4];
    private final ArrayList<RawTexture> mTargetStack = new ArrayList<>();
    private final ArrayList<ConfigState> mRestoreStack = new ArrayList<>();
    private final RectF mDrawTextureSourceRect = new RectF();
    private final RectF mDrawTextureTargetRect = new RectF();
    private final float[] mTempMatrix = new float[32];
    private final IntArray mUnboundTextures = new IntArray();
    private final IntArray mDeleteBuffers = new IntArray();
    private boolean mBlendEnabled = true;
    private int[] mFrameBuffer = new int[1];

    public GLES11Canvas(GL11 gl) {
        this.mGL = gl;
        this.mGLState = new GLState(gl);
        int size = (BOX_COORDINATES.length * 32) / 8;
        FloatBuffer xyBuffer = allocateDirectNativeOrderBuffer(size).asFloatBuffer();
        xyBuffer.put(BOX_COORDINATES, 0, BOX_COORDINATES.length).position(0);
        int[] name = new int[1];
        mGLId.glGenBuffers(1, name, 0);
        this.mBoxCoords = name[0];
        gl.glBindBuffer(34962, this.mBoxCoords);
        gl.glBufferData(34962, xyBuffer.capacity() * 4, xyBuffer, 35044);
        gl.glVertexPointer(2, 5126, 0, 0);
        gl.glTexCoordPointer(2, 5126, 0, 0);
        gl.glClientActiveTexture(33985);
        gl.glTexCoordPointer(2, 5126, 0, 0);
        gl.glClientActiveTexture(33984);
        gl.glEnableClientState(32888);
    }

    @Override
    public void setSize(int width, int height) {
        Assert.assertTrue(width >= 0 && height >= 0);
        if (this.mTargetTexture == null) {
            this.mScreenWidth = width;
            this.mScreenHeight = height;
        }
        this.mAlpha = 1.0f;
        GL11 gl = this.mGL;
        gl.glViewport(0, 0, width, height);
        gl.glMatrixMode(5889);
        gl.glLoadIdentity();
        GLU.gluOrtho2D(gl, 0.0f, width, 0.0f, height);
        gl.glMatrixMode(5888);
        gl.glLoadIdentity();
        float[] matrix = this.mMatrixValues;
        Matrix.setIdentityM(matrix, 0);
        if (this.mTargetTexture == null) {
            Matrix.translateM(matrix, 0, 0.0f, height, 0.0f);
            Matrix.scaleM(matrix, 0, 1.0f, -1.0f, 1.0f);
        }
    }

    @Override
    public void setAlpha(float alpha) {
        Assert.assertTrue(alpha >= 0.0f && alpha <= 1.0f);
        this.mAlpha = alpha;
    }

    @Override
    public float getAlpha() {
        return this.mAlpha;
    }

    @Override
    public void multiplyAlpha(float alpha) {
        Assert.assertTrue(alpha >= 0.0f && alpha <= 1.0f);
        this.mAlpha *= alpha;
    }

    private static ByteBuffer allocateDirectNativeOrderBuffer(int size) {
        return ByteBuffer.allocateDirect(size).order(ByteOrder.nativeOrder());
    }

    @Override
    public void drawRect(float x, float y, float width, float height, GLPaint paint) {
        GL11 gl = this.mGL;
        this.mGLState.setColorMode(paint.getColor(), this.mAlpha);
        this.mGLState.setLineWidth(paint.getLineWidth());
        saveTransform();
        translate(x, y);
        scale(width, height, 1.0f);
        gl.glLoadMatrixf(this.mMatrixValues, 0);
        gl.glDrawArrays(2, 6, 4);
        restoreTransform();
        this.mCountDrawLine++;
    }

    @Override
    public void drawLine(float x1, float y1, float x2, float y2, GLPaint paint) {
        GL11 gl = this.mGL;
        this.mGLState.setColorMode(paint.getColor(), this.mAlpha);
        this.mGLState.setLineWidth(paint.getLineWidth());
        saveTransform();
        translate(x1, y1);
        scale(x2 - x1, y2 - y1, 1.0f);
        gl.glLoadMatrixf(this.mMatrixValues, 0);
        gl.glDrawArrays(3, 4, 2);
        restoreTransform();
        this.mCountDrawLine++;
    }

    @Override
    public void fillRect(float x, float y, float width, float height, int color) {
        this.mGLState.setColorMode(color, this.mAlpha);
        GL11 gl = this.mGL;
        saveTransform();
        translate(x, y);
        scale(width, height, 1.0f);
        gl.glLoadMatrixf(this.mMatrixValues, 0);
        gl.glDrawArrays(5, 0, 4);
        restoreTransform();
        this.mCountFillRect++;
    }

    @Override
    public void translate(float x, float y, float z) {
        Matrix.translateM(this.mMatrixValues, 0, x, y, z);
    }

    @Override
    public void translate(float x, float y) {
        float[] m = this.mMatrixValues;
        m[12] = m[12] + (m[0] * x) + (m[4] * y);
        m[13] = m[13] + (m[1] * x) + (m[5] * y);
        m[14] = m[14] + (m[2] * x) + (m[6] * y);
        m[15] = m[15] + (m[3] * x) + (m[7] * y);
    }

    @Override
    public void scale(float sx, float sy, float sz) {
        Matrix.scaleM(this.mMatrixValues, 0, sx, sy, sz);
    }

    @Override
    public void rotate(float angle, float x, float y, float z) {
        if (angle != 0.0f) {
            float[] temp = this.mTempMatrix;
            Matrix.setRotateM(temp, 0, angle, x, y, z);
            Matrix.multiplyMM(temp, 16, this.mMatrixValues, 0, temp, 0);
            System.arraycopy(temp, 16, this.mMatrixValues, 0, 16);
        }
    }

    @Override
    public void multiplyMatrix(float[] matrix, int offset) {
        float[] temp = this.mTempMatrix;
        Matrix.multiplyMM(temp, 0, this.mMatrixValues, 0, matrix, offset);
        System.arraycopy(temp, 0, this.mMatrixValues, 0, 16);
    }

    private void textureRect(float x, float y, float width, float height) {
        GL11 gl = this.mGL;
        saveTransform();
        translate(x, y);
        scale(width, height, 1.0f);
        gl.glLoadMatrixf(this.mMatrixValues, 0);
        gl.glDrawArrays(5, 0, 4);
        restoreTransform();
        this.mCountTextureRect++;
    }

    @Override
    public void drawMesh(BasicTexture tex, int x, int y, int xyBuffer, int uvBuffer, int indexBuffer, int indexCount) {
        float alpha = this.mAlpha;
        if (bindTexture(tex)) {
            this.mGLState.setBlendEnabled(this.mBlendEnabled && (!tex.isOpaque() || alpha < 0.95f));
            this.mGLState.setTextureAlpha(alpha);
            setTextureCoords(0.0f, 0.0f, 1.0f, 1.0f);
            saveTransform();
            translate(x, y);
            this.mGL.glLoadMatrixf(this.mMatrixValues, 0);
            this.mGL.glBindBuffer(34962, xyBuffer);
            this.mGL.glVertexPointer(2, 5126, 0, 0);
            this.mGL.glBindBuffer(34962, uvBuffer);
            this.mGL.glTexCoordPointer(2, 5126, 0, 0);
            this.mGL.glBindBuffer(34963, indexBuffer);
            this.mGL.glDrawElements(5, indexCount, 5121, 0);
            this.mGL.glBindBuffer(34962, this.mBoxCoords);
            this.mGL.glVertexPointer(2, 5126, 0, 0);
            this.mGL.glTexCoordPointer(2, 5126, 0, 0);
            restoreTransform();
            this.mCountDrawMesh++;
        }
    }

    private float[] mapPoints(float[] m, int x1, int y1, int x2, int y2) {
        float[] r = this.mMapPointsBuffer;
        float x3 = (m[0] * x1) + (m[4] * y1) + m[12];
        float y3 = (m[1] * x1) + (m[5] * y1) + m[13];
        float w3 = (m[3] * x1) + (m[7] * y1) + m[15];
        r[0] = x3 / w3;
        r[1] = y3 / w3;
        float x4 = (m[0] * x2) + (m[4] * y2) + m[12];
        float y4 = (m[1] * x2) + (m[5] * y2) + m[13];
        float w4 = (m[3] * x2) + (m[7] * y2) + m[15];
        r[2] = x4 / w4;
        r[3] = y4 / w4;
        return r;
    }

    private void drawBoundTexture(BasicTexture texture, int x, int y, int width, int height) {
        if (isMatrixRotatedOrFlipped(this.mMatrixValues)) {
            if (texture.hasBorder()) {
                setTextureCoords(1.0f / texture.getTextureWidth(), 1.0f / texture.getTextureHeight(), (texture.getWidth() - 1.0f) / texture.getTextureWidth(), (texture.getHeight() - 1.0f) / texture.getTextureHeight());
            } else {
                setTextureCoords(0.0f, 0.0f, texture.getWidth() / texture.getTextureWidth(), texture.getHeight() / texture.getTextureHeight());
            }
            textureRect(x, y, width, height);
            return;
        }
        float[] points = mapPoints(this.mMatrixValues, x, y + height, x + width, y);
        int x2 = (int) (points[0] + 0.5f);
        int y2 = (int) (points[1] + 0.5f);
        int width2 = ((int) (points[2] + 0.5f)) - x2;
        int height2 = ((int) (points[3] + 0.5f)) - y2;
        if (width2 > 0 && height2 > 0) {
            ((GL11Ext) this.mGL).glDrawTexiOES(x2, y2, 0, width2, height2);
            this.mCountTextureOES++;
        }
    }

    @Override
    public void drawTexture(BasicTexture texture, int x, int y, int width, int height) {
        drawTexture(texture, x, y, width, height, this.mAlpha);
    }

    private void drawTexture(BasicTexture texture, int x, int y, int width, int height, float alpha) {
        if (width > 0 && height > 0) {
            this.mGLState.setBlendEnabled(this.mBlendEnabled && (!texture.isOpaque() || alpha < 0.95f));
            if (bindTexture(texture)) {
                this.mGLState.setTextureAlpha(alpha);
                drawBoundTexture(texture, x, y, width, height);
            }
        }
    }

    @Override
    public void drawTexture(BasicTexture texture, RectF source, RectF target) {
        if (target.width() > 0.0f && target.height() > 0.0f) {
            this.mDrawTextureSourceRect.set(source);
            this.mDrawTextureTargetRect.set(target);
            RectF source2 = this.mDrawTextureSourceRect;
            RectF target2 = this.mDrawTextureTargetRect;
            this.mGLState.setBlendEnabled(this.mBlendEnabled && (!texture.isOpaque() || this.mAlpha < 0.95f));
            if (bindTexture(texture)) {
                convertCoordinate(source2, target2, texture);
                setTextureCoords(source2);
                this.mGLState.setTextureAlpha(this.mAlpha);
                textureRect(target2.left, target2.top, target2.width(), target2.height());
            }
        }
    }

    @Override
    public void drawTexture(BasicTexture texture, float[] mTextureTransform, int x, int y, int w, int h) {
        this.mGLState.setBlendEnabled(this.mBlendEnabled && (!texture.isOpaque() || this.mAlpha < 0.95f));
        if (bindTexture(texture)) {
            setTextureCoords(mTextureTransform);
            this.mGLState.setTextureAlpha(this.mAlpha);
            textureRect(x, y, w, h);
        }
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

    @Override
    public void drawMixed(BasicTexture from, int toColor, float ratio, int x, int y, int w, int h) {
        drawMixed(from, toColor, ratio, x, y, w, h, this.mAlpha);
    }

    private boolean bindTexture(BasicTexture texture) {
        if (!texture.onBind(this)) {
            return false;
        }
        int target = texture.getTarget();
        this.mGLState.setTextureTarget(target);
        this.mGL.glBindTexture(target, texture.getId());
        return true;
    }

    private void setTextureColor(float r, float g, float b, float alpha) {
        float[] color = this.mTextureColor;
        color[0] = r;
        color[1] = g;
        color[2] = b;
        color[3] = alpha;
    }

    private void setMixedColor(int toColor, float ratio, float alpha) {
        float combo = alpha * (1.0f - ratio);
        float scale = (alpha * ratio) / (1.0f - combo);
        float colorScale = ((toColor >>> 24) * scale) / 65025.0f;
        setTextureColor(((toColor >>> 16) & 255) * colorScale, ((toColor >>> 8) & 255) * colorScale, (toColor & 255) * colorScale, combo);
        GL11 gl = this.mGL;
        gl.glTexEnvfv(8960, 8705, this.mTextureColor, 0);
        gl.glTexEnvf(8960, 34161, 34165.0f);
        gl.glTexEnvf(8960, 34162, 34165.0f);
        gl.glTexEnvf(8960, 34177, 34166.0f);
        gl.glTexEnvf(8960, 34193, 768.0f);
        gl.glTexEnvf(8960, 34185, 34166.0f);
        gl.glTexEnvf(8960, 34201, 770.0f);
        gl.glTexEnvf(8960, 34178, 34166.0f);
        gl.glTexEnvf(8960, 34194, 770.0f);
        gl.glTexEnvf(8960, 34186, 34166.0f);
        gl.glTexEnvf(8960, 34202, 770.0f);
    }

    @Override
    public void drawMixed(BasicTexture from, int toColor, float ratio, RectF source, RectF target) {
        if (target.width() <= 0.0f || target.height() <= 0.0f) {
            return;
        }
        if (ratio <= 0.01f) {
            drawTexture(from, source, target);
            return;
        }
        if (ratio >= 1.0f) {
            fillRect(target.left, target.top, target.width(), target.height(), toColor);
            return;
        }
        float alpha = this.mAlpha;
        this.mDrawTextureSourceRect.set(source);
        this.mDrawTextureTargetRect.set(target);
        RectF source2 = this.mDrawTextureSourceRect;
        RectF target2 = this.mDrawTextureTargetRect;
        this.mGLState.setBlendEnabled(this.mBlendEnabled && !(from.isOpaque() && Utils.isOpaque(toColor) && alpha >= 0.95f));
        if (bindTexture(from)) {
            this.mGLState.setTexEnvMode(34160);
            setMixedColor(toColor, ratio, alpha);
            convertCoordinate(source2, target2, from);
            setTextureCoords(source2);
            textureRect(target2.left, target2.top, target2.width(), target2.height());
            this.mGLState.setTexEnvMode(7681);
        }
    }

    private void drawMixed(BasicTexture from, int toColor, float ratio, int x, int y, int width, int height, float alpha) {
        if (ratio <= 0.01f) {
            drawTexture(from, x, y, width, height, alpha);
            return;
        }
        if (ratio >= 1.0f) {
            fillRect(x, y, width, height, toColor);
            return;
        }
        this.mGLState.setBlendEnabled(this.mBlendEnabled && !(from.isOpaque() && Utils.isOpaque(toColor) && alpha >= 0.95f));
        GL11 gl11 = this.mGL;
        if (bindTexture(from)) {
            this.mGLState.setTexEnvMode(34160);
            setMixedColor(toColor, ratio, alpha);
            drawBoundTexture(from, x, y, width, height);
            this.mGLState.setTexEnvMode(7681);
        }
    }

    private static boolean isMatrixRotatedOrFlipped(float[] matrix) {
        return Math.abs(matrix[4]) > 1.0E-5f || Math.abs(matrix[1]) > 1.0E-5f || matrix[0] < -1.0E-5f || matrix[5] > 1.0E-5f;
    }

    private static class GLState {
        private final GL11 mGL;
        private int mTexEnvMode = 7681;
        private float mTextureAlpha = 1.0f;
        private int mTextureTarget = 3553;
        private boolean mBlendEnabled = true;
        private float mLineWidth = 1.0f;
        private boolean mLineSmooth = false;

        public GLState(GL11 gl) {
            this.mGL = gl;
            gl.glDisable(2896);
            gl.glEnable(3024);
            gl.glEnableClientState(32884);
            gl.glEnableClientState(32888);
            gl.glEnable(3553);
            gl.glTexEnvf(8960, 8704, 7681.0f);
            gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
            gl.glEnable(3042);
            gl.glBlendFunc(1, 771);
            gl.glPixelStorei(3317, 2);
        }

        public void setTexEnvMode(int mode) {
            if (this.mTexEnvMode != mode) {
                this.mTexEnvMode = mode;
                this.mGL.glTexEnvf(8960, 8704, mode);
            }
        }

        public void setLineWidth(float width) {
            if (this.mLineWidth != width) {
                this.mLineWidth = width;
                this.mGL.glLineWidth(width);
            }
        }

        public void setTextureAlpha(float alpha) {
            if (this.mTextureAlpha != alpha) {
                this.mTextureAlpha = alpha;
                if (alpha >= 0.95f) {
                    this.mGL.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
                    setTexEnvMode(7681);
                } else {
                    this.mGL.glColor4f(alpha, alpha, alpha, alpha);
                    setTexEnvMode(8448);
                }
            }
        }

        public void setColorMode(int color, float alpha) {
            setBlendEnabled(!Utils.isOpaque(color) || alpha < 0.95f);
            this.mTextureAlpha = -1.0f;
            setTextureTarget(0);
            float prealpha = ((((color >>> 24) * alpha) * 65535.0f) / 255.0f) / 255.0f;
            this.mGL.glColor4x(Math.round(((color >> 16) & 255) * prealpha), Math.round(((color >> 8) & 255) * prealpha), Math.round((color & 255) * prealpha), Math.round(255.0f * prealpha));
        }

        public void setTextureTarget(int target) {
            if (this.mTextureTarget != target) {
                if (this.mTextureTarget != 0) {
                    this.mGL.glDisable(this.mTextureTarget);
                }
                this.mTextureTarget = target;
                if (this.mTextureTarget != 0) {
                    this.mGL.glEnable(this.mTextureTarget);
                }
            }
        }

        public void setBlendEnabled(boolean enabled) {
            if (this.mBlendEnabled != enabled) {
                this.mBlendEnabled = enabled;
                if (enabled) {
                    this.mGL.glEnable(3042);
                } else {
                    this.mGL.glDisable(3042);
                }
            }
        }
    }

    @Override
    public void clearBuffer(float[] argb) {
        if (argb != null && argb.length == 4) {
            this.mGL.glClearColor(argb[1], argb[2], argb[3], argb[0]);
        } else {
            this.mGL.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        }
        this.mGL.glClear(16384);
    }

    @Override
    public void clearBuffer() {
        clearBuffer(null);
    }

    private void setTextureCoords(RectF source) {
        setTextureCoords(source.left, source.top, source.right, source.bottom);
    }

    private void setTextureCoords(float left, float top, float right, float bottom) {
        this.mGL.glMatrixMode(5890);
        this.mTextureMatrixValues[0] = right - left;
        this.mTextureMatrixValues[5] = bottom - top;
        this.mTextureMatrixValues[10] = 1.0f;
        this.mTextureMatrixValues[12] = left;
        this.mTextureMatrixValues[13] = top;
        this.mTextureMatrixValues[15] = 1.0f;
        this.mGL.glLoadMatrixf(this.mTextureMatrixValues, 0);
        this.mGL.glMatrixMode(5888);
    }

    private void setTextureCoords(float[] mTextureTransform) {
        this.mGL.glMatrixMode(5890);
        this.mGL.glLoadMatrixf(mTextureTransform, 0);
        this.mGL.glMatrixMode(5888);
    }

    @Override
    public boolean unloadTexture(BasicTexture t) {
        boolean z;
        synchronized (this.mUnboundTextures) {
            if (t.isLoaded()) {
                this.mUnboundTextures.add(t.mId);
                z = true;
            } else {
                z = false;
            }
        }
        return z;
    }

    @Override
    public void deleteBuffer(int bufferId) {
        synchronized (this.mUnboundTextures) {
            this.mDeleteBuffers.add(bufferId);
        }
    }

    @Override
    public void deleteRecycledResources() {
        synchronized (this.mUnboundTextures) {
            IntArray ids = this.mUnboundTextures;
            if (ids.size() > 0) {
                mGLId.glDeleteTextures(this.mGL, ids.size(), ids.getInternalArray(), 0);
                ids.clear();
            }
            IntArray ids2 = this.mDeleteBuffers;
            if (ids2.size() > 0) {
                mGLId.glDeleteBuffers(this.mGL, ids2.size(), ids2.getInternalArray(), 0);
                ids2.clear();
            }
        }
    }

    @Override
    public void save() {
        save(-1);
    }

    @Override
    public void save(int saveFlags) {
        ConfigState config = obtainRestoreConfig();
        if ((saveFlags & 1) != 0) {
            config.mAlpha = this.mAlpha;
        } else {
            config.mAlpha = -1.0f;
        }
        if ((saveFlags & 2) != 0) {
            System.arraycopy(this.mMatrixValues, 0, config.mMatrix, 0, 16);
        } else {
            config.mMatrix[0] = Float.NEGATIVE_INFINITY;
        }
        this.mRestoreStack.add(config);
    }

    @Override
    public void restore() {
        if (this.mRestoreStack.isEmpty()) {
            throw new IllegalStateException();
        }
        ConfigState config = this.mRestoreStack.remove(this.mRestoreStack.size() - 1);
        config.restore(this);
        freeRestoreConfig(config);
    }

    private void freeRestoreConfig(ConfigState action) {
        action.mNextFree = this.mRecycledRestoreAction;
        this.mRecycledRestoreAction = action;
    }

    private ConfigState obtainRestoreConfig() {
        if (this.mRecycledRestoreAction == null) {
            return new ConfigState();
        }
        ConfigState result = this.mRecycledRestoreAction;
        this.mRecycledRestoreAction = result.mNextFree;
        return result;
    }

    private static class ConfigState {
        float mAlpha;
        float[] mMatrix;
        ConfigState mNextFree;

        private ConfigState() {
            this.mMatrix = new float[16];
        }

        public void restore(GLES11Canvas canvas) {
            if (this.mAlpha >= 0.0f) {
                canvas.setAlpha(this.mAlpha);
            }
            if (this.mMatrix[0] != Float.NEGATIVE_INFINITY) {
                System.arraycopy(this.mMatrix, 0, canvas.mMatrixValues, 0, 16);
            }
        }
    }

    @Override
    public void dumpStatisticsAndClear() {
        String line = String.format("MESH:%d, TEX_OES:%d, TEX_RECT:%d, FILL_RECT:%d, LINE:%d", Integer.valueOf(this.mCountDrawMesh), Integer.valueOf(this.mCountTextureRect), Integer.valueOf(this.mCountTextureOES), Integer.valueOf(this.mCountFillRect), Integer.valueOf(this.mCountDrawLine));
        this.mCountDrawMesh = 0;
        this.mCountTextureRect = 0;
        this.mCountTextureOES = 0;
        this.mCountFillRect = 0;
        this.mCountDrawLine = 0;
        Log.d("GLCanvasImp", line);
    }

    private void saveTransform() {
        System.arraycopy(this.mMatrixValues, 0, this.mTempMatrix, 0, 16);
    }

    private void restoreTransform() {
        System.arraycopy(this.mTempMatrix, 0, this.mMatrixValues, 0, 16);
    }

    private void setRenderTarget(RawTexture texture) {
        GL11ExtensionPack gl11ep = (GL11ExtensionPack) this.mGL;
        if (this.mTargetTexture == null && texture != null) {
            mGLId.glGenBuffers(1, this.mFrameBuffer, 0);
            gl11ep.glBindFramebufferOES(36160, this.mFrameBuffer[0]);
        }
        if (this.mTargetTexture != null && texture == null) {
            gl11ep.glBindFramebufferOES(36160, 0);
            gl11ep.glDeleteFramebuffersOES(1, this.mFrameBuffer, 0);
        }
        this.mTargetTexture = texture;
        if (texture == null) {
            setSize(this.mScreenWidth, this.mScreenHeight);
            return;
        }
        setSize(texture.getWidth(), texture.getHeight());
        if (!texture.isLoaded()) {
            texture.prepare(this);
        }
        gl11ep.glFramebufferTexture2DOES(36160, 36064, 3553, texture.getId(), 0);
        checkFramebufferStatus(gl11ep);
    }

    @Override
    public void endRenderTarget() {
        RawTexture texture = this.mTargetStack.remove(this.mTargetStack.size() - 1);
        setRenderTarget(texture);
        restore();
    }

    @Override
    public void beginRenderTarget(RawTexture texture) {
        save();
        this.mTargetStack.add(this.mTargetTexture);
        setRenderTarget(texture);
    }

    private static void checkFramebufferStatus(GL11ExtensionPack gl11ep) {
        int status = gl11ep.glCheckFramebufferStatusOES(36160);
        if (status != 36053) {
            String msg = "";
            switch (status) {
                case 36054:
                    msg = "FRAMEBUFFER_ATTACHMENT";
                    break;
                case 36055:
                    msg = "FRAMEBUFFER_MISSING_ATTACHMENT";
                    break;
                case 36057:
                    msg = "FRAMEBUFFER_INCOMPLETE_DIMENSIONS";
                    break;
                case 36058:
                    msg = "FRAMEBUFFER_FORMATS";
                    break;
                case 36059:
                    msg = "FRAMEBUFFER_DRAW_BUFFER";
                    break;
                case 36060:
                    msg = "FRAMEBUFFER_READ_BUFFER";
                    break;
                case 36061:
                    msg = "FRAMEBUFFER_UNSUPPORTED";
                    break;
            }
            throw new RuntimeException(msg + ":" + Integer.toHexString(status));
        }
    }

    @Override
    public void setTextureParameters(BasicTexture texture) {
        int width = texture.getWidth();
        int height = texture.getHeight();
        sCropRect[0] = 0.0f;
        sCropRect[1] = height;
        sCropRect[2] = width;
        sCropRect[3] = -height;
        int target = texture.getTarget();
        this.mGL.glBindTexture(target, texture.getId());
        this.mGL.glTexParameterfv(target, 35741, sCropRect, 0);
        this.mGL.glTexParameteri(target, 10242, 33071);
        this.mGL.glTexParameteri(target, 10243, 33071);
        this.mGL.glTexParameterf(target, 10241, 9729.0f);
        this.mGL.glTexParameterf(target, 10240, 9729.0f);
    }

    @Override
    public void initializeTextureSize(BasicTexture texture, int format, int type) {
        int target = texture.getTarget();
        this.mGL.glBindTexture(target, texture.getId());
        int width = texture.getTextureWidth();
        int height = texture.getTextureHeight();
        this.mGL.glTexImage2D(target, 0, format, width, height, 0, format, type, null);
    }

    @Override
    public void initializeTexture(BasicTexture texture, Bitmap bitmap) {
        int target = texture.getTarget();
        this.mGL.glBindTexture(target, texture.getId());
        GLUtils.texImage2D(target, 0, bitmap, 0);
    }

    @Override
    public void texSubImage2D(BasicTexture texture, int xOffset, int yOffset, Bitmap bitmap, int format, int type) {
        int target = texture.getTarget();
        this.mGL.glBindTexture(target, texture.getId());
        GLUtils.texSubImage2D(target, 0, xOffset, yOffset, bitmap, format, type);
    }

    @Override
    public int uploadBuffer(FloatBuffer buf) {
        return uploadBuffer(buf, 4);
    }

    @Override
    public int uploadBuffer(ByteBuffer buf) {
        return uploadBuffer(buf, 1);
    }

    private int uploadBuffer(Buffer buf, int elementSize) {
        int[] bufferIds = new int[1];
        mGLId.glGenBuffers(bufferIds.length, bufferIds, 0);
        int bufferId = bufferIds[0];
        this.mGL.glBindBuffer(34962, bufferId);
        this.mGL.glBufferData(34962, buf.capacity() * elementSize, buf, 35044);
        return bufferId;
    }

    @Override
    public void recoverFromLightCycle() {
    }

    @Override
    public void getBounds(Rect bounds, int x, int y, int width, int height) {
    }

    @Override
    public GLId getGLId() {
        return mGLId;
    }
}
