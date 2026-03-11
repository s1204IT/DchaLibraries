package com.android.systemui;

import android.app.WallpaperManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Trace;
import android.service.wallpaper.WallpaperService;
import android.util.Log;
import android.view.Display;
import android.view.DisplayInfo;
import android.view.SurfaceHolder;
import android.view.WindowManager;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.PrintWriter;

public class ImageWallpaper extends WallpaperService {
    private DrawableEngine mEngine;
    private WallpaperManager mWallpaperManager;

    @Override
    public void onCreate() {
        super.onCreate();
        this.mWallpaperManager = (WallpaperManager) getSystemService(WallpaperManager.class);
    }

    @Override
    public void onTrimMemory(int i) {
        if (this.mEngine != null) {
            this.mEngine.trimMemory(i);
        }
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
        private Display mDefaultDisplay;
        private int mDisplayHeightAtLastSurfaceSizeUpdate;
        private int mDisplayWidthAtLastSurfaceSizeUpdate;
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
        private boolean mSurfaceRedrawNeeded;
        private boolean mSurfaceValid;
        private final DisplayInfo mTmpDisplayInfo;
        private final Runnable mUnloadWallpaperCallback;
        boolean mVisible;
        float mXOffset;
        float mYOffset;

        DrawableEngine() {
            super(ImageWallpaper.this);
            this.mUnloadWallpaperCallback = new Runnable() {
                @Override
                public final void run() {
                    this.f$0.unloadWallpaper(false);
                }
            };
            this.mBackgroundWidth = -1;
            this.mBackgroundHeight = -1;
            this.mLastSurfaceWidth = -1;
            this.mLastSurfaceHeight = -1;
            this.mLastRotation = -1;
            this.mXOffset = 0.0f;
            this.mYOffset = 0.0f;
            this.mScale = 1.0f;
            this.mTmpDisplayInfo = new DisplayInfo();
            this.mVisible = true;
            this.mRotationAtLastSurfaceSizeUpdate = -1;
            this.mDisplayWidthAtLastSurfaceSizeUpdate = -1;
            this.mDisplayHeightAtLastSurfaceSizeUpdate = -1;
            this.mLastRequestedWidth = -1;
            this.mLastRequestedHeight = -1;
            setFixedSizeAllowed(true);
        }

        void trimMemory(int i) {
            if (i >= 10 && i <= 15 && this.mBackground != null) {
                unloadWallpaper(true);
            }
        }

        @Override
        public void onCreate(SurfaceHolder surfaceHolder) {
            super.onCreate(surfaceHolder);
            this.mDefaultDisplay = ((WindowManager) ImageWallpaper.this.getSystemService(WindowManager.class)).getDefaultDisplay();
            setOffsetNotificationsEnabled(false);
            updateSurfaceSize(surfaceHolder, getDefaultDisplayInfo(), false);
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            this.mBackground = null;
            unloadWallpaper(true);
        }

        boolean updateSurfaceSize(SurfaceHolder surfaceHolder, DisplayInfo displayInfo, boolean z) {
            boolean z2;
            if (this.mBackgroundWidth <= 0 || this.mBackgroundHeight <= 0) {
                loadWallpaper(z);
                z2 = false;
            } else {
                z2 = true;
            }
            int iMax = Math.max(displayInfo.logicalWidth, this.mBackgroundWidth);
            int iMax2 = Math.max(displayInfo.logicalHeight, this.mBackgroundHeight);
            surfaceHolder.setFixedSize(iMax, iMax2);
            this.mLastRequestedWidth = iMax;
            this.mLastRequestedHeight = iMax2;
            return z2;
        }

        @Override
        public void onVisibilityChanged(boolean z) {
            if (this.mVisible != z) {
                this.mVisible = z;
                if (z) {
                    drawFrame();
                }
            }
        }

        @Override
        public void onOffsetsChanged(float f, float f2, float f3, float f4, int i, int i2) {
            if (this.mXOffset != f || this.mYOffset != f2) {
                this.mXOffset = f;
                this.mYOffset = f2;
                this.mOffsetsChanged = true;
            }
            drawFrame();
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder surfaceHolder, int i, int i2, int i3) {
            super.onSurfaceChanged(surfaceHolder, i, i2, i3);
            drawFrame();
        }

        @Override
        public void onSurfaceDestroyed(SurfaceHolder surfaceHolder) {
            super.onSurfaceDestroyed(surfaceHolder);
            this.mLastSurfaceHeight = -1;
            this.mLastSurfaceWidth = -1;
            this.mSurfaceValid = false;
        }

        @Override
        public void onSurfaceCreated(SurfaceHolder surfaceHolder) {
            super.onSurfaceCreated(surfaceHolder);
            this.mLastSurfaceHeight = -1;
            this.mLastSurfaceWidth = -1;
            this.mSurfaceValid = true;
        }

        @Override
        public void onSurfaceRedrawNeeded(SurfaceHolder surfaceHolder) {
            super.onSurfaceRedrawNeeded(surfaceHolder);
            if (this.mBackground == null) {
                updateBitmap(ImageWallpaper.this.mWallpaperManager.getBitmap(true));
            }
            this.mSurfaceRedrawNeeded = true;
            drawFrame();
        }

        private DisplayInfo getDefaultDisplayInfo() {
            this.mDefaultDisplay.getDisplayInfo(this.mTmpDisplayInfo);
            return this.mTmpDisplayInfo;
        }

        void drawFrame() {
            if (this.mSurfaceValid) {
                try {
                    Trace.traceBegin(8L, "drawWallpaper");
                    DisplayInfo defaultDisplayInfo = getDefaultDisplayInfo();
                    int i = defaultDisplayInfo.rotation;
                    if (i != this.mLastRotation) {
                        if (!updateSurfaceSize(getSurfaceHolder(), defaultDisplayInfo, true)) {
                            return;
                        }
                        this.mRotationAtLastSurfaceSizeUpdate = i;
                        this.mDisplayWidthAtLastSurfaceSizeUpdate = defaultDisplayInfo.logicalWidth;
                        this.mDisplayHeightAtLastSurfaceSizeUpdate = defaultDisplayInfo.logicalHeight;
                    }
                    SurfaceHolder surfaceHolder = getSurfaceHolder();
                    Rect surfaceFrame = surfaceHolder.getSurfaceFrame();
                    int iWidth = surfaceFrame.width();
                    int iHeight = surfaceFrame.height();
                    boolean z = (iWidth == this.mLastSurfaceWidth && iHeight == this.mLastSurfaceHeight) ? false : true;
                    boolean z2 = z || i != this.mLastRotation || this.mSurfaceRedrawNeeded;
                    if (z2 || this.mOffsetsChanged) {
                        this.mLastRotation = i;
                        this.mSurfaceRedrawNeeded = false;
                        if (this.mBackground == null) {
                            loadWallpaper(true);
                            return;
                        }
                        this.mScale = Math.max(1.0f, Math.max(iWidth / this.mBackground.getWidth(), iHeight / this.mBackground.getHeight()));
                        int width = ((int) (this.mBackground.getWidth() * this.mScale)) - iWidth;
                        int height = ((int) (this.mBackground.getHeight() * this.mScale)) - iHeight;
                        int i2 = (int) (width * this.mXOffset);
                        int i3 = (int) (height * this.mYOffset);
                        this.mOffsetsChanged = false;
                        if (z) {
                            this.mLastSurfaceWidth = iWidth;
                            this.mLastSurfaceHeight = iHeight;
                        }
                        if (!z2 && i2 == this.mLastXTranslation && i3 == this.mLastYTranslation) {
                            return;
                        }
                        this.mLastXTranslation = i2;
                        this.mLastYTranslation = i3;
                        drawWallpaperWithCanvas(surfaceHolder, width, height, i2, i3);
                        scheduleUnloadWallpaper();
                    }
                } finally {
                    Trace.traceEnd(8L);
                }
            }
        }

        private void loadWallpaper(boolean z) {
            this.mNeedsDrawAfterLoadingWallpaper = z | this.mNeedsDrawAfterLoadingWallpaper;
            if (this.mLoader != null) {
                return;
            }
            this.mLoader = new AsyncTask<Void, Void, Bitmap>() {
                @Override
                public Bitmap doInBackground(Void... voidArr) {
                    try {
                        return ImageWallpaper.this.mWallpaperManager.getBitmap(true);
                    } catch (OutOfMemoryError | RuntimeException e) {
                        if (isCancelled()) {
                            return null;
                        }
                        Log.w("ImageWallpaper", "Unable to load wallpaper!", e);
                        try {
                            ImageWallpaper.this.mWallpaperManager.clear();
                        } catch (IOException e2) {
                            Log.w("ImageWallpaper", "Unable reset to default wallpaper!", e2);
                        }
                        if (isCancelled()) {
                            return null;
                        }
                        try {
                            return ImageWallpaper.this.mWallpaperManager.getBitmap(true);
                        } catch (OutOfMemoryError | RuntimeException e3) {
                            Log.w("ImageWallpaper", "Unable to load default wallpaper!", e3);
                            return null;
                        }
                    }
                }

                @Override
                public void onPostExecute(Bitmap bitmap) {
                    DrawableEngine.this.updateBitmap(bitmap);
                    if (DrawableEngine.this.mNeedsDrawAfterLoadingWallpaper) {
                        DrawableEngine.this.drawFrame();
                    }
                    DrawableEngine.this.mLoader = null;
                    DrawableEngine.this.mNeedsDrawAfterLoadingWallpaper = false;
                }
            }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new Void[0]);
        }

        public void updateBitmap(Bitmap bitmap) {
            this.mBackground = null;
            this.mBackgroundWidth = -1;
            this.mBackgroundHeight = -1;
            if (bitmap != null) {
                this.mBackground = bitmap;
                this.mBackgroundWidth = this.mBackground.getWidth();
                this.mBackgroundHeight = this.mBackground.getHeight();
            }
            updateSurfaceSize(getSurfaceHolder(), getDefaultDisplayInfo(), false);
        }

        public void unloadWallpaper(boolean z) {
            if (this.mLoader != null) {
                this.mLoader.cancel(false);
                this.mLoader = null;
            }
            this.mBackground = null;
            if (z) {
                this.mBackgroundWidth = -1;
                this.mBackgroundHeight = -1;
            }
            getSurfaceHolder().getSurface().hwuiDestroy();
            ImageWallpaper.this.mWallpaperManager.forgetLoadedWallpaper();
        }

        private void scheduleUnloadWallpaper() {
            Handler mainThreadHandler = ImageWallpaper.this.getMainThreadHandler();
            mainThreadHandler.removeCallbacks(this.mUnloadWallpaperCallback);
            mainThreadHandler.postDelayed(this.mUnloadWallpaperCallback, 5000L);
        }

        @Override
        protected void dump(String str, FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
            super.dump(str, fileDescriptor, printWriter, strArr);
            printWriter.print(str);
            printWriter.println("ImageWallpaper.DrawableEngine:");
            printWriter.print(str);
            printWriter.print(" mBackground=");
            printWriter.print(this.mBackground);
            printWriter.print(" mBackgroundWidth=");
            printWriter.print(this.mBackgroundWidth);
            printWriter.print(" mBackgroundHeight=");
            printWriter.println(this.mBackgroundHeight);
            printWriter.print(str);
            printWriter.print(" mLastRotation=");
            printWriter.print(this.mLastRotation);
            printWriter.print(" mLastSurfaceWidth=");
            printWriter.print(this.mLastSurfaceWidth);
            printWriter.print(" mLastSurfaceHeight=");
            printWriter.println(this.mLastSurfaceHeight);
            printWriter.print(str);
            printWriter.print(" mXOffset=");
            printWriter.print(this.mXOffset);
            printWriter.print(" mYOffset=");
            printWriter.println(this.mYOffset);
            printWriter.print(str);
            printWriter.print(" mVisible=");
            printWriter.print(this.mVisible);
            printWriter.print(" mOffsetsChanged=");
            printWriter.println(this.mOffsetsChanged);
            printWriter.print(str);
            printWriter.print(" mLastXTranslation=");
            printWriter.print(this.mLastXTranslation);
            printWriter.print(" mLastYTranslation=");
            printWriter.print(this.mLastYTranslation);
            printWriter.print(" mScale=");
            printWriter.println(this.mScale);
            printWriter.print(str);
            printWriter.print(" mLastRequestedWidth=");
            printWriter.print(this.mLastRequestedWidth);
            printWriter.print(" mLastRequestedHeight=");
            printWriter.println(this.mLastRequestedHeight);
            printWriter.print(str);
            printWriter.println(" DisplayInfo at last updateSurfaceSize:");
            printWriter.print(str);
            printWriter.print("  rotation=");
            printWriter.print(this.mRotationAtLastSurfaceSizeUpdate);
            printWriter.print("  width=");
            printWriter.print(this.mDisplayWidthAtLastSurfaceSizeUpdate);
            printWriter.print("  height=");
            printWriter.println(this.mDisplayHeightAtLastSurfaceSizeUpdate);
        }

        private void drawWallpaperWithCanvas(SurfaceHolder surfaceHolder, int i, int i2, int i3, int i4) {
            Canvas canvasLockHardwareCanvas = surfaceHolder.lockHardwareCanvas();
            if (canvasLockHardwareCanvas != null) {
                float f = i3;
                try {
                    float width = f + (this.mBackground.getWidth() * this.mScale);
                    float f2 = i4;
                    float height = f2 + (this.mBackground.getHeight() * this.mScale);
                    if (i < 0 || i2 < 0) {
                        canvasLockHardwareCanvas.save(2);
                        canvasLockHardwareCanvas.clipRect(f, f2, width, height, Region.Op.DIFFERENCE);
                        canvasLockHardwareCanvas.drawColor(-16777216);
                        canvasLockHardwareCanvas.restore();
                    }
                    if (this.mBackground != null) {
                        RectF rectF = new RectF(f, f2, width, height);
                        Log.i("ImageWallpaper", "Redrawing in rect: " + rectF + " with surface size: " + this.mLastRequestedWidth + "x" + this.mLastRequestedHeight);
                        canvasLockHardwareCanvas.drawBitmap(this.mBackground, (Rect) null, rectF, (Paint) null);
                    }
                } finally {
                    surfaceHolder.unlockCanvasAndPost(canvasLockHardwareCanvas);
                }
            }
        }
    }
}
