package com.android.systemui;

import android.app.ActivityManager;
import android.app.WallpaperManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.os.AsyncTask;
import android.os.SystemProperties;
import android.os.Trace;
import android.renderscript.Matrix4f;
import android.service.wallpaper.WallpaperService;
import android.util.Log;
import android.view.Display;
import android.view.DisplayInfo;
import android.view.GraphicBuffer;
import android.view.SurfaceHolder;
import android.view.WindowManager;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;

public class ImageWallpaper extends WallpaperService {
    private Context mContext;
    DrawableEngine mEngine;
    boolean mIsHwAccelerated;
    private boolean mIstabletConfig;
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null) {
                return;
            }
            String action = intent.getAction();
            if (!"android.intent.action.CONFIGURATION_CHANGED".equals(action) || ImageWallpaper.this.mEngine == null) {
                return;
            }
            ImageWallpaper.this.mEngine.drawFrame();
        }
    };
    WallpaperManager mWallpaperManager;

    @Override
    public void onCreate() {
        super.onCreate();
        this.mContext = getApplicationContext();
        this.mWallpaperManager = (WallpaperManager) getSystemService("wallpaper");
        this.mContext.registerReceiver(this.mReceiver, new IntentFilter("android.intent.action.CONFIGURATION_CHANGED"));
        if (!isEmulator()) {
            this.mIsHwAccelerated = ActivityManager.isHighEndGfx();
        }
        this.mIstabletConfig = this.mContext.getResources().getBoolean(R.bool.tablet_config);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        this.mContext.unregisterReceiver(this.mReceiver);
    }

    @Override
    public void onTrimMemory(int level) {
        if (this.mEngine == null) {
            return;
        }
        this.mEngine.trimMemory(level);
    }

    private static boolean isEmulator() {
        return "1".equals(SystemProperties.get("ro.kernel.qemu", "0"));
    }

    @Override
    public WallpaperService.Engine onCreateEngine() {
        this.mEngine = new DrawableEngine();
        return this.mEngine;
    }

    class DrawableEngine extends WallpaperService.Engine {
        Bitmap mBackground;
        int mBackgroundHeight;
        int mBackgroundWidth;
        private GraphicBuffer mBuffer;
        private Display mDefaultDisplay;
        private int mDisplayHeightAtLastSurfaceSizeUpdate;
        private int mDisplayWidthAtLastSurfaceSizeUpdate;
        private EGL10 mEgl;
        private EGLConfig mEglConfig;
        private EGLContext mEglContext;
        private EGLDisplay mEglDisplay;
        private EGLSurface mEglSurface;
        private int mLastRequestedHeight;
        private int mLastRequestedWidth;
        int mLastRotation;
        int mLastSurfaceHeight;
        int mLastSurfaceWidth;
        int mLastXTranslation;
        int mLastYTranslation;
        private AsyncTask<Void, Void, Bitmap> mLoader;
        private boolean mNeedsDrawAfterLoadingWallpaper;
        boolean mOffsetsChanged;
        private int mRotationAtLastSurfaceSizeUpdate;
        float mScale;
        private boolean mSurfaceValid;
        private final DisplayInfo mTmpDisplayInfo;
        boolean mVisible;
        float mXOffset;
        float mYOffset;
        private boolean mYv12Enhancement;

        public DrawableEngine() {
            super(ImageWallpaper.this);
            this.mBuffer = null;
            this.mBackgroundWidth = -1;
            this.mBackgroundHeight = -1;
            this.mLastSurfaceWidth = -1;
            this.mLastSurfaceHeight = -1;
            this.mLastRotation = -1;
            this.mXOffset = 0.5f;
            this.mYOffset = 0.5f;
            this.mScale = 1.0f;
            this.mTmpDisplayInfo = new DisplayInfo();
            this.mVisible = true;
            this.mRotationAtLastSurfaceSizeUpdate = -1;
            this.mDisplayWidthAtLastSurfaceSizeUpdate = -1;
            this.mDisplayHeightAtLastSurfaceSizeUpdate = -1;
            this.mLastRequestedWidth = -1;
            this.mLastRequestedHeight = -1;
            this.mYv12Enhancement = false;
            setFixedSizeAllowed(true);
        }

        public void trimMemory(int level) {
            if (level >= 10 && level <= 15) {
                if (this.mBackground != null) {
                    this.mBackground.recycle();
                    this.mBackground = null;
                }
                this.mBackgroundWidth = -1;
                this.mBackgroundHeight = -1;
                ImageWallpaper.this.mWallpaperManager.forgetLoadedWallpaper();
            }
            if (!"1".equals(SystemProperties.get("ro.mtk_gmo_ram_optimize", "0")) || !this.mYv12Enhancement || !Utils.useYv12 || this.mBuffer == null) {
                return;
            }
            this.mBuffer.destroy();
            this.mBuffer = null;
        }

        @Override
        public void onCreate(SurfaceHolder surfaceHolder) {
            super.onCreate(surfaceHolder);
            this.mDefaultDisplay = ((WindowManager) ImageWallpaper.this.getSystemService(WindowManager.class)).getDefaultDisplay();
            setOffsetNotificationsEnabled(false);
            if (ImageWallpaper.this.mIstabletConfig && (this.mBackgroundWidth <= 0 || this.mBackgroundHeight <= 0)) {
                ImageWallpaper.this.mWallpaperManager.forgetLoadedWallpaper();
                loadWallpaper(false);
            }
            updateSurfaceSize(surfaceHolder, getDefaultDisplayInfo(), false);
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            this.mBackground = null;
            if ("1".equals(SystemProperties.get("ro.mtk_gmo_ram_optimize", "0")) && this.mYv12Enhancement && Utils.useYv12 && this.mBuffer != null) {
                this.mBuffer.destroy();
                this.mBuffer = null;
            }
            ImageWallpaper.this.mWallpaperManager.forgetLoadedWallpaper();
        }

        boolean updateSurfaceSize(SurfaceHolder surfaceHolder, DisplayInfo displayInfo, boolean forDraw) {
            boolean hasWallpaper = true;
            if (this.mBackgroundWidth <= 0 || this.mBackgroundHeight <= 0) {
                ImageWallpaper.this.mWallpaperManager.forgetLoadedWallpaper();
                loadWallpaper(forDraw);
                hasWallpaper = false;
            }
            int surfaceWidth = Math.max(displayInfo.logicalWidth, this.mBackgroundWidth);
            int surfaceHeight = Math.max(displayInfo.logicalHeight, this.mBackgroundHeight);
            surfaceHolder.setFixedSize(surfaceWidth, surfaceHeight);
            this.mLastRequestedWidth = surfaceWidth;
            this.mLastRequestedHeight = surfaceHeight;
            return hasWallpaper;
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            if (this.mVisible == visible) {
                return;
            }
            this.mVisible = visible;
            if (!visible) {
                return;
            }
            drawFrame();
        }

        @Override
        public void onOffsetsChanged(float xOffset, float yOffset, float xOffsetStep, float yOffsetStep, int xPixels, int yPixels) {
            if (this.mXOffset != xOffset || this.mYOffset != yOffset) {
                this.mXOffset = xOffset;
                this.mYOffset = yOffset;
                this.mOffsetsChanged = true;
            }
            drawFrame();
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            super.onSurfaceChanged(holder, format, width, height);
            drawFrame();
        }

        @Override
        public void onSurfaceDestroyed(SurfaceHolder holder) {
            super.onSurfaceDestroyed(holder);
            this.mLastSurfaceHeight = -1;
            this.mLastSurfaceWidth = -1;
            this.mSurfaceValid = false;
        }

        @Override
        public void onSurfaceCreated(SurfaceHolder holder) {
            super.onSurfaceCreated(holder);
            this.mLastSurfaceHeight = -1;
            this.mLastSurfaceWidth = -1;
            this.mSurfaceValid = true;
        }

        @Override
        public void onSurfaceRedrawNeeded(SurfaceHolder holder) {
            super.onSurfaceRedrawNeeded(holder);
            drawFrame();
        }

        public DisplayInfo getDefaultDisplayInfo() {
            this.mDefaultDisplay.getDisplayInfo(this.mTmpDisplayInfo);
            return this.mTmpDisplayInfo;
        }

        void drawFrame() {
            boolean z;
            if (this.mSurfaceValid) {
                try {
                    Trace.traceBegin(8L, "drawWallpaper");
                    DisplayInfo displayInfo = getDefaultDisplayInfo();
                    int newRotation = displayInfo.rotation;
                    if (newRotation != this.mLastRotation || this.mDisplayWidthAtLastSurfaceSizeUpdate != displayInfo.logicalWidth || this.mDisplayHeightAtLastSurfaceSizeUpdate != displayInfo.logicalHeight) {
                        if (!updateSurfaceSize(getSurfaceHolder(), displayInfo, true)) {
                            Trace.traceEnd(8L);
                            if (ImageWallpaper.this.mIsHwAccelerated) {
                                return;
                            }
                            this.mBackground = null;
                            ImageWallpaper.this.mWallpaperManager.forgetLoadedWallpaper();
                            return;
                        }
                        this.mRotationAtLastSurfaceSizeUpdate = newRotation;
                        this.mDisplayWidthAtLastSurfaceSizeUpdate = displayInfo.logicalWidth;
                        this.mDisplayHeightAtLastSurfaceSizeUpdate = displayInfo.logicalHeight;
                    }
                    SurfaceHolder sh = getSurfaceHolder();
                    Rect frame = sh.getSurfaceFrame();
                    int dw = frame.width();
                    int dh = frame.height();
                    boolean surfaceDimensionsChanged = (dw == this.mLastSurfaceWidth && dh == this.mLastSurfaceHeight) ? false : true;
                    boolean redrawNeeded = surfaceDimensionsChanged || newRotation != this.mLastRotation;
                    if (!redrawNeeded && !this.mOffsetsChanged) {
                        if (z) {
                            return;
                        } else {
                            return;
                        }
                    }
                    this.mLastRotation = newRotation;
                    if (this.mBackground == null) {
                        ImageWallpaper.this.mWallpaperManager.forgetLoadedWallpaper();
                        loadWallpaper(true);
                        Trace.traceEnd(8L);
                        if (ImageWallpaper.this.mIsHwAccelerated) {
                            return;
                        }
                        this.mBackground = null;
                        ImageWallpaper.this.mWallpaperManager.forgetLoadedWallpaper();
                        return;
                    }
                    this.mScale = Math.max(1.0f, Math.max(dw / this.mBackground.getWidth(), dh / this.mBackground.getHeight()));
                    this.mScale = dw / this.mBackground.getWidth();
                    int availw = dw - ((int) (this.mBackground.getWidth() * this.mScale));
                    int availh = dh - ((int) (this.mBackground.getHeight() * this.mScale));
                    int xPixels = availw / 2;
                    int yPixels = availh / 2;
                    int availwUnscaled = ((int) (dw / this.mScale)) - this.mBackground.getWidth();
                    int availhUnscaled = ((int) (dh / this.mScale)) - this.mBackground.getHeight();
                    if (availwUnscaled < 0) {
                        xPixels += (int) ((availwUnscaled * (this.mXOffset - 0.5f) * this.mScale) + 0.5f);
                    }
                    if (availhUnscaled < 0) {
                        yPixels += (int) ((availhUnscaled * (this.mYOffset - 0.5f) * this.mScale) + 0.5f);
                    }
                    this.mOffsetsChanged = false;
                    if (surfaceDimensionsChanged) {
                        this.mLastSurfaceWidth = dw;
                        this.mLastSurfaceHeight = dh;
                    }
                    if (!redrawNeeded && xPixels == this.mLastXTranslation && yPixels == this.mLastYTranslation) {
                        Trace.traceEnd(8L);
                        if (ImageWallpaper.this.mIsHwAccelerated) {
                            return;
                        }
                        this.mBackground = null;
                        ImageWallpaper.this.mWallpaperManager.forgetLoadedWallpaper();
                        return;
                    }
                    this.mLastXTranslation = xPixels;
                    this.mLastYTranslation = yPixels;
                    if (!ImageWallpaper.this.mIsHwAccelerated) {
                        drawWallpaperWithCanvas(sh, availw, availh, xPixels, yPixels);
                    } else if (!drawWallpaperWithOpenGL(sh, availw, availh, xPixels, yPixels)) {
                        drawWallpaperWithCanvas(sh, availw, availh, xPixels, yPixels);
                    }
                    Trace.traceEnd(8L);
                    if (ImageWallpaper.this.mIsHwAccelerated) {
                        return;
                    }
                    this.mBackground = null;
                    ImageWallpaper.this.mWallpaperManager.forgetLoadedWallpaper();
                } finally {
                    Trace.traceEnd(8L);
                    if (!ImageWallpaper.this.mIsHwAccelerated) {
                        this.mBackground = null;
                        ImageWallpaper.this.mWallpaperManager.forgetLoadedWallpaper();
                    }
                }
            }
        }

        private void loadWallpaper(boolean needsDraw) {
            this.mNeedsDrawAfterLoadingWallpaper |= needsDraw;
            if (this.mLoader != null) {
                return;
            }
            this.mLoader = new AsyncTask<Void, Void, Bitmap>() {
                @Override
                public Bitmap doInBackground(Void... params) {
                    try {
                        return ImageWallpaper.this.mWallpaperManager.getBitmap();
                    } catch (OutOfMemoryError | RuntimeException e) {
                        if (e != null) {
                            Log.w("ImageWallpaper", "Unable to load wallpaper!", e);
                            try {
                                ImageWallpaper.this.mWallpaperManager.clear();
                            } catch (IOException ex) {
                                Log.w("ImageWallpaper", "Unable reset to default wallpaper!", ex);
                            }
                            try {
                                return ImageWallpaper.this.mWallpaperManager.getBitmap();
                            } catch (OutOfMemoryError | RuntimeException e2) {
                                Log.w("ImageWallpaper", "Unable to load default wallpaper!", e2);
                                return null;
                            }
                        }
                        return null;
                    }
                }

                @Override
                public void onPostExecute(Bitmap b) {
                    DrawableEngine.this.mBackground = null;
                    DrawableEngine.this.mBackgroundWidth = -1;
                    DrawableEngine.this.mBackgroundHeight = -1;
                    if (b != null) {
                        DrawableEngine.this.mBackground = b;
                        DrawableEngine.this.mBackgroundWidth = DrawableEngine.this.mBackground.getWidth();
                        DrawableEngine.this.mBackgroundHeight = DrawableEngine.this.mBackground.getHeight();
                    }
                    DrawableEngine.this.updateSurfaceSize(DrawableEngine.this.getSurfaceHolder(), DrawableEngine.this.getDefaultDisplayInfo(), false);
                    if (DrawableEngine.this.mNeedsDrawAfterLoadingWallpaper) {
                        DrawableEngine.this.drawFrame();
                    }
                    DrawableEngine.this.mLoader = null;
                    DrawableEngine.this.mNeedsDrawAfterLoadingWallpaper = false;
                }
            }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new Void[0]);
        }

        @Override
        protected void dump(String prefix, FileDescriptor fd, PrintWriter out, String[] args) {
            super.dump(prefix, fd, out, args);
            out.print(prefix);
            out.println("ImageWallpaper.DrawableEngine:");
            out.print(prefix);
            out.print(" mBackground=");
            out.print(this.mBackground);
            out.print(" mBackgroundWidth=");
            out.print(this.mBackgroundWidth);
            out.print(" mBackgroundHeight=");
            out.println(this.mBackgroundHeight);
            out.print(prefix);
            out.print(" mLastRotation=");
            out.print(this.mLastRotation);
            out.print(" mLastSurfaceWidth=");
            out.print(this.mLastSurfaceWidth);
            out.print(" mLastSurfaceHeight=");
            out.println(this.mLastSurfaceHeight);
            out.print(prefix);
            out.print(" mXOffset=");
            out.print(this.mXOffset);
            out.print(" mYOffset=");
            out.println(this.mYOffset);
            out.print(prefix);
            out.print(" mVisible=");
            out.print(this.mVisible);
            out.print(" mOffsetsChanged=");
            out.println(this.mOffsetsChanged);
            out.print(prefix);
            out.print(" mLastXTranslation=");
            out.print(this.mLastXTranslation);
            out.print(" mLastYTranslation=");
            out.print(this.mLastYTranslation);
            out.print(" mScale=");
            out.println(this.mScale);
            out.print(prefix);
            out.print(" mLastRequestedWidth=");
            out.print(this.mLastRequestedWidth);
            out.print(" mLastRequestedHeight=");
            out.println(this.mLastRequestedHeight);
            out.print(prefix);
            out.println(" DisplayInfo at last updateSurfaceSize:");
            out.print(prefix);
            out.print("  rotation=");
            out.print(this.mRotationAtLastSurfaceSizeUpdate);
            out.print("  width=");
            out.print(this.mDisplayWidthAtLastSurfaceSizeUpdate);
            out.print("  height=");
            out.println(this.mDisplayHeightAtLastSurfaceSizeUpdate);
        }

        private void drawWallpaperWithCanvas(SurfaceHolder sh, int w, int h, int left, int top) {
            Canvas c = sh.lockCanvas();
            if (c == null) {
                return;
            }
            try {
                float right = left + (this.mBackground.getWidth() * this.mScale);
                float bottom = top + (this.mBackground.getHeight() * this.mScale);
                if (w < 0 || h < 0) {
                    c.save(2);
                    c.clipRect(left, top, right, bottom, Region.Op.DIFFERENCE);
                    c.drawColor(-16777216);
                    c.restore();
                }
                if (this.mBackground != null) {
                    RectF dest = new RectF(left, top, right, bottom);
                    c.drawBitmap(this.mBackground, (Rect) null, dest, (Paint) null);
                }
            } finally {
                sh.unlockCanvasAndPost(c);
            }
        }

        private boolean drawWallpaperWithOpenGL(SurfaceHolder sh, int w, int h, int left, int top) {
            int texture;
            int program;
            if (!initGL(sh)) {
                return false;
            }
            float right = left + (this.mBackground.getWidth() * this.mScale);
            float bottom = top + (this.mBackground.getHeight() * this.mScale);
            Rect frame = sh.getSurfaceFrame();
            Matrix4f ortho = new Matrix4f();
            ortho.loadOrtho(0.0f, frame.width(), frame.height(), 0.0f, -1.0f, 1.0f);
            FloatBuffer triangleVertices = createMesh(left, top, right, bottom);
            if ("1".equals(SystemProperties.get("ro.mtk_gmo_ram_optimize", "0"))) {
                texture = loadTexture_ext(this.mBackground);
            } else {
                texture = loadTexture(this.mBackground);
            }
            if ("1".equals(SystemProperties.get("ro.mtk_gmo_ram_optimize", "0")) && this.mYv12Enhancement && Utils.useYv12) {
                program = buildProgram("attribute vec4 position;\nattribute vec2 texCoords;\nvarying vec2 outTexCoords;\nuniform mat4 projection;\n\nvoid main(void) {\n    outTexCoords = texCoords;\n    gl_Position = projection * position;\n}\n\n", "#extension GL_OES_EGL_image_external : require\nprecision mediump float;\n\nvarying vec2 outTexCoords;\nuniform samplerExternalOES texture;\n\nvoid main(void) {\n    gl_FragColor = texture2D(texture, outTexCoords);\n}\n\n");
            } else {
                program = buildProgram("attribute vec4 position;\nattribute vec2 texCoords;\nvarying vec2 outTexCoords;\nuniform mat4 projection;\n\nvoid main(void) {\n    outTexCoords = texCoords;\n    gl_Position = projection * position;\n}\n\n", "precision mediump float;\n\nvarying vec2 outTexCoords;\nuniform sampler2D texture;\n\nvoid main(void) {\n    gl_FragColor = texture2D(texture, outTexCoords);\n}\n\n");
            }
            int attribPosition = GLES20.glGetAttribLocation(program, "position");
            int attribTexCoords = GLES20.glGetAttribLocation(program, "texCoords");
            int uniformTexture = GLES20.glGetUniformLocation(program, "texture");
            int uniformProjection = GLES20.glGetUniformLocation(program, "projection");
            checkGlError();
            GLES20.glViewport(0, 0, frame.width(), frame.height());
            if ("1".equals(SystemProperties.get("ro.mtk_gmo_ram_optimize", "0")) && this.mYv12Enhancement) {
                GLES20.glBindTexture(36197, texture);
            } else {
                GLES20.glBindTexture(3553, texture);
            }
            GLES20.glUseProgram(program);
            GLES20.glEnableVertexAttribArray(attribPosition);
            GLES20.glEnableVertexAttribArray(attribTexCoords);
            GLES20.glUniform1i(uniformTexture, 0);
            GLES20.glUniformMatrix4fv(uniformProjection, 1, false, ortho.getArray(), 0);
            checkGlError();
            if (w > 0 || h > 0) {
                if ("1".equals(SystemProperties.get("ro.mtk_gmo_ram_optimize", "0")) && this.mYv12Enhancement && Utils.useYv12) {
                    GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
                } else {
                    GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
                }
                GLES20.glClear(16384);
            }
            triangleVertices.position(0);
            GLES20.glVertexAttribPointer(attribPosition, 3, 5126, false, 20, (Buffer) triangleVertices);
            triangleVertices.position(3);
            GLES20.glVertexAttribPointer(attribTexCoords, 3, 5126, false, 20, (Buffer) triangleVertices);
            GLES20.glDrawArrays(5, 0, 4);
            boolean status = this.mEgl.eglSwapBuffers(this.mEglDisplay, this.mEglSurface);
            checkEglError();
            finishGL(texture, program);
            return status;
        }

        private FloatBuffer createMesh(int left, int top, float right, float bottom) {
            float[] verticesData = {left, bottom, 0.0f, 0.0f, 1.0f, right, bottom, 0.0f, 1.0f, 1.0f, left, top, 0.0f, 0.0f, 0.0f, right, top, 0.0f, 1.0f, 0.0f};
            int bytes = verticesData.length * 4;
            FloatBuffer triangleVertices = ByteBuffer.allocateDirect(bytes).order(ByteOrder.nativeOrder()).asFloatBuffer();
            triangleVertices.put(verticesData).position(0);
            return triangleVertices;
        }

        private int loadTexture(Bitmap bitmap) {
            int[] textures = new int[1];
            this.mYv12Enhancement = false;
            GLES20.glActiveTexture(33984);
            GLES20.glGenTextures(1, textures, 0);
            checkGlError();
            int texture = textures[0];
            GLES20.glBindTexture(3553, texture);
            checkGlError();
            Log.d("ImageWallpaper", "inside loadTexture");
            GLES20.glTexParameteri(3553, 10241, 9729);
            GLES20.glTexParameteri(3553, 10240, 9729);
            GLES20.glTexParameteri(3553, 10242, 33071);
            GLES20.glTexParameteri(3553, 10243, 33071);
            GLUtils.texImage2D(3553, 0, 6408, bitmap, 5121, 0);
            checkGlError();
            return texture;
        }

        private int loadTexture_ext(Bitmap bitmap) {
            int[] textures = new int[1];
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            this.mYv12Enhancement = true;
            Log.d("ImageWallpaper", "loadTexture_ext bitmap width " + width + ", height" + height);
            if (width % 2 != 0) {
                width--;
            }
            if (height % 2 != 0) {
                height--;
            }
            if (this.mBuffer == null) {
                this.mBuffer = GraphicBuffer.create(width, height, 842094169, 288);
            }
            if (this.mBuffer == null || !Utils.useYv12) {
                Log.d("ImageWallpaper", "graphic buffer is null executing normal jpg useYv12 = " + Utils.useYv12);
                return loadTexture(bitmap);
            }
            GLES20.glActiveTexture(33984);
            GLES20.glGenTextures(1, textures, 0);
            checkGlError();
            int texture = textures[0];
            GLES20.glBindTexture(36197, texture);
            checkGlError();
            GLES20.glTexParameteri(36197, 10241, 9729);
            GLES20.glTexParameteri(36197, 10240, 9729);
            GLES20.glTexParameteri(36197, 10242, 33071);
            GLES20.glTexParameteri(36197, 10243, 33071);
            Utils.BitmapToYv12(bitmap, this.mBuffer);
            Utils.createTexture2D(texture, this.mBuffer);
            if (this.mBackground != null) {
                this.mBackground.recycle();
                this.mBackground = null;
            }
            ImageWallpaper.this.mWallpaperManager.forgetLoadedWallpaper();
            checkGlError();
            return texture;
        }

        private int buildProgram(String vertex, String fragment) {
            int fragmentShader;
            int vertexShader = buildShader(vertex, 35633);
            if (vertexShader == 0 || (fragmentShader = buildShader(fragment, 35632)) == 0) {
                return 0;
            }
            int program = GLES20.glCreateProgram();
            GLES20.glAttachShader(program, vertexShader);
            GLES20.glAttachShader(program, fragmentShader);
            GLES20.glLinkProgram(program);
            checkGlError();
            GLES20.glDeleteShader(vertexShader);
            GLES20.glDeleteShader(fragmentShader);
            int[] status = new int[1];
            GLES20.glGetProgramiv(program, 35714, status, 0);
            if (status[0] != 1) {
                String error = GLES20.glGetProgramInfoLog(program);
                Log.d("ImageWallpaperGL", "Error while linking program:\n" + error);
                GLES20.glDeleteProgram(program);
                return 0;
            }
            return program;
        }

        private int buildShader(String source, int type) {
            int shader = GLES20.glCreateShader(type);
            GLES20.glShaderSource(shader, source);
            checkGlError();
            GLES20.glCompileShader(shader);
            checkGlError();
            int[] status = new int[1];
            GLES20.glGetShaderiv(shader, 35713, status, 0);
            if (status[0] != 1) {
                String error = GLES20.glGetShaderInfoLog(shader);
                Log.d("ImageWallpaperGL", "Error while compiling shader:\n" + error);
                GLES20.glDeleteShader(shader);
                return 0;
            }
            return shader;
        }

        private void checkEglError() {
            int error = this.mEgl.eglGetError();
            if (error == 12288) {
                return;
            }
            Log.w("ImageWallpaperGL", "EGL error = " + GLUtils.getEGLErrorString(error));
        }

        private void checkGlError() {
            int error = GLES20.glGetError();
            if (error == 0) {
                return;
            }
            Log.w("ImageWallpaperGL", "GL error = 0x" + Integer.toHexString(error), new Throwable());
        }

        private void finishGL(int texture, int program) {
            int[] textures = {texture};
            GLES20.glDeleteTextures(1, textures, 0);
            GLES20.glDeleteProgram(program);
            this.mEgl.eglMakeCurrent(this.mEglDisplay, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_CONTEXT);
            this.mEgl.eglDestroySurface(this.mEglDisplay, this.mEglSurface);
            this.mEgl.eglDestroyContext(this.mEglDisplay, this.mEglContext);
            this.mEgl.eglTerminate(this.mEglDisplay);
        }

        private boolean initGL(SurfaceHolder surfaceHolder) {
            this.mEgl = (EGL10) EGLContext.getEGL();
            this.mEglDisplay = this.mEgl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);
            if (this.mEglDisplay == EGL10.EGL_NO_DISPLAY) {
                throw new RuntimeException("eglGetDisplay failed " + GLUtils.getEGLErrorString(this.mEgl.eglGetError()));
            }
            int[] version = new int[2];
            if (!this.mEgl.eglInitialize(this.mEglDisplay, version)) {
                throw new RuntimeException("eglInitialize failed " + GLUtils.getEGLErrorString(this.mEgl.eglGetError()));
            }
            this.mEglConfig = chooseEglConfig();
            if (this.mEglConfig == null) {
                throw new RuntimeException("eglConfig not initialized");
            }
            this.mEglContext = createContext(this.mEgl, this.mEglDisplay, this.mEglConfig);
            if (this.mEglContext == EGL10.EGL_NO_CONTEXT) {
                throw new RuntimeException("createContext failed " + GLUtils.getEGLErrorString(this.mEgl.eglGetError()));
            }
            int[] attribs = {12375, 1, 12374, 1, 12344};
            EGLSurface tmpSurface = this.mEgl.eglCreatePbufferSurface(this.mEglDisplay, this.mEglConfig, attribs);
            this.mEgl.eglMakeCurrent(this.mEglDisplay, tmpSurface, tmpSurface, this.mEglContext);
            int[] maxSize = new int[1];
            Rect frame = surfaceHolder.getSurfaceFrame();
            GLES20.glGetIntegerv(3379, maxSize, 0);
            this.mEgl.eglMakeCurrent(this.mEglDisplay, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_CONTEXT);
            this.mEgl.eglDestroySurface(this.mEglDisplay, tmpSurface);
            if (frame.width() > maxSize[0] || frame.height() > maxSize[0]) {
                this.mEgl.eglDestroyContext(this.mEglDisplay, this.mEglContext);
                this.mEgl.eglTerminate(this.mEglDisplay);
                Log.e("ImageWallpaperGL", "requested  texture size " + frame.width() + "x" + frame.height() + " exceeds the support maximum of " + maxSize[0] + "x" + maxSize[0]);
                return false;
            }
            this.mEglSurface = this.mEgl.eglCreateWindowSurface(this.mEglDisplay, this.mEglConfig, surfaceHolder, null);
            if (this.mEglSurface != null && this.mEglSurface != EGL10.EGL_NO_SURFACE) {
                if (this.mEgl.eglMakeCurrent(this.mEglDisplay, this.mEglSurface, this.mEglSurface, this.mEglContext)) {
                    return true;
                }
                throw new RuntimeException("eglMakeCurrent failed " + GLUtils.getEGLErrorString(this.mEgl.eglGetError()));
            }
            int error = this.mEgl.eglGetError();
            if (error != 12299 && error != 12291) {
                throw new RuntimeException("createWindowSurface failed " + GLUtils.getEGLErrorString(error));
            }
            Log.e("ImageWallpaperGL", "createWindowSurface returned " + GLUtils.getEGLErrorString(error) + ".");
            return false;
        }

        EGLContext createContext(EGL10 egl, EGLDisplay eglDisplay, EGLConfig eglConfig) {
            int[] attrib_list = {12440, 2, 12344};
            return egl.eglCreateContext(eglDisplay, eglConfig, EGL10.EGL_NO_CONTEXT, attrib_list);
        }

        private EGLConfig chooseEglConfig() {
            int[] configsCount = new int[1];
            EGLConfig[] configs = new EGLConfig[1];
            int[] configSpec = getConfig();
            if (!this.mEgl.eglChooseConfig(this.mEglDisplay, configSpec, configs, 1, configsCount)) {
                throw new IllegalArgumentException("eglChooseConfig failed " + GLUtils.getEGLErrorString(this.mEgl.eglGetError()));
            }
            if (configsCount[0] > 0) {
                return configs[0];
            }
            return null;
        }

        private int[] getConfig() {
            return new int[]{12352, 4, 12324, 8, 12323, 8, 12322, 8, 12321, 0, 12325, 0, 12326, 0, 12327, 12344, 12344};
        }
    }
}
