package android.view;

import android.content.res.CompatibilityInfo;
import android.graphics.Canvas;
import android.graphics.GraphicBuffer;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import com.mediatek.view.SurfaceExt;
import com.mediatek.view.SurfaceFactory;
import dalvik.system.CloseGuard;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class Surface implements Parcelable {
    public static final Parcelable.Creator<Surface> CREATOR = new Parcelable.Creator<Surface>() {
        @Override
        public Surface createFromParcel(Parcel parcel) {
            try {
                Surface surface = new Surface();
                surface.readFromParcel(parcel);
                return surface;
            } catch (Exception e) {
                Log.e(Surface.TAG, "Exception creating surface from parcel", e);
                return null;
            }
        }

        @Override
        public Surface[] newArray(int i) {
            return new Surface[i];
        }
    };
    public static final int ROTATION_0 = 0;
    public static final int ROTATION_180 = 2;
    public static final int ROTATION_270 = 3;
    public static final int ROTATION_90 = 1;
    public static final int SCALING_MODE_FREEZE = 0;
    public static final int SCALING_MODE_NO_SCALE_CROP = 3;
    public static final int SCALING_MODE_SCALE_CROP = 2;
    public static final int SCALING_MODE_SCALE_TO_WINDOW = 1;
    private static final String TAG = "Surface";
    private Matrix mCompatibleMatrix;
    private int mGenerationId;
    private HwuiContext mHwuiContext;
    private boolean mIsAutoRefreshEnabled;
    private boolean mIsSharedBufferModeEnabled;
    private boolean mIsSingleBuffered;
    private long mLockedObject;
    private String mName;
    long mNativeObject;
    private final CloseGuard mCloseGuard = CloseGuard.get();
    final Object mLock = new Object();
    private final Canvas mCanvas = new CompatibleCanvas();
    private SurfaceExt mSurfaceExt = SurfaceFactory.getInstance().getSurfaceExt();

    @Retention(RetentionPolicy.SOURCE)
    public @interface Rotation {
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface ScalingMode {
    }

    private static native long nHwuiCreate(long j, long j2, boolean z);

    private static native void nHwuiDestroy(long j);

    private static native void nHwuiDraw(long j);

    private static native void nHwuiSetSurface(long j, long j2);

    private static native void nativeAllocateBuffers(long j);

    private static native int nativeAttachAndQueueBuffer(long j, GraphicBuffer graphicBuffer);

    private static native long nativeCreateFromSurfaceControl(long j);

    private static native long nativeCreateFromSurfaceTexture(SurfaceTexture surfaceTexture) throws OutOfResourcesException;

    private static native int nativeForceScopedDisconnect(long j);

    private static native long nativeGetFromSurfaceControl(long j);

    private static native int nativeGetHeight(long j);

    private static native long nativeGetNextFrameNumber(long j);

    private static native int nativeGetWidth(long j);

    private static native boolean nativeIsConsumerRunningBehind(long j);

    private static native boolean nativeIsValid(long j);

    private static native long nativeLockCanvas(long j, Canvas canvas, Rect rect) throws OutOfResourcesException;

    private static native long nativeReadFromParcel(long j, Parcel parcel);

    private static native void nativeRelease(long j);

    private static native int nativeSetAutoRefreshEnabled(long j, boolean z);

    private static native int nativeSetScalingMode(long j, int i);

    private static native int nativeSetSharedBufferModeEnabled(long j, boolean z);

    private static native void nativeUnlockCanvasAndPost(long j, Canvas canvas);

    private static native void nativeWriteToParcel(long j, Parcel parcel);

    public Surface() {
    }

    public Surface(SurfaceTexture surfaceTexture) {
        if (surfaceTexture == null) {
            throw new IllegalArgumentException("surfaceTexture must not be null");
        }
        this.mIsSingleBuffered = surfaceTexture.isSingleBuffered();
        synchronized (this.mLock) {
            this.mName = surfaceTexture.toString();
            setNativeObjectLocked(nativeCreateFromSurfaceTexture(surfaceTexture));
        }
    }

    private Surface(long j) {
        synchronized (this.mLock) {
            setNativeObjectLocked(j);
        }
    }

    protected void finalize() throws Throwable {
        try {
            if (this.mCloseGuard != null) {
                this.mCloseGuard.warnIfOpen();
            }
            release();
        } finally {
            super.finalize();
        }
    }

    public void release() {
        synchronized (this.mLock) {
            if (this.mNativeObject != 0) {
                nativeRelease(this.mNativeObject);
                setNativeObjectLocked(0L);
            }
            if (this.mHwuiContext != null) {
                this.mHwuiContext.destroy();
                this.mHwuiContext = null;
            }
        }
    }

    public void destroy() {
        release();
    }

    public void hwuiDestroy() {
        if (this.mHwuiContext != null) {
            this.mHwuiContext.destroy();
            this.mHwuiContext = null;
        }
    }

    public boolean isValid() {
        synchronized (this.mLock) {
            if (this.mNativeObject == 0) {
                return false;
            }
            return nativeIsValid(this.mNativeObject);
        }
    }

    public int getGenerationId() {
        int i;
        synchronized (this.mLock) {
            i = this.mGenerationId;
        }
        return i;
    }

    public long getNextFrameNumber() {
        long jNativeGetNextFrameNumber;
        synchronized (this.mLock) {
            checkNotReleasedLocked();
            jNativeGetNextFrameNumber = nativeGetNextFrameNumber(this.mNativeObject);
        }
        return jNativeGetNextFrameNumber;
    }

    public boolean isConsumerRunningBehind() {
        boolean zNativeIsConsumerRunningBehind;
        synchronized (this.mLock) {
            checkNotReleasedLocked();
            zNativeIsConsumerRunningBehind = nativeIsConsumerRunningBehind(this.mNativeObject);
        }
        return zNativeIsConsumerRunningBehind;
    }

    public Canvas lockCanvas(Rect rect) throws OutOfResourcesException, IllegalArgumentException {
        synchronized (this.mLock) {
            if (this.mSurfaceExt.isInWhiteList()) {
                return lockHardwareCanvas();
            }
            Log.d(TAG, "lockCanvas");
            checkNotReleasedLocked();
            if (this.mLockedObject != 0) {
                throw new IllegalArgumentException("Surface was already locked");
            }
            this.mLockedObject = nativeLockCanvas(this.mNativeObject, this.mCanvas, rect);
            return this.mCanvas;
        }
    }

    public void unlockCanvasAndPost(Canvas canvas) {
        synchronized (this.mLock) {
            checkNotReleasedLocked();
            if (this.mHwuiContext != null) {
                this.mHwuiContext.unlockAndPost(canvas);
            } else {
                unlockSwCanvasAndPost(canvas);
            }
        }
    }

    private void unlockSwCanvasAndPost(Canvas canvas) {
        if (canvas != this.mCanvas) {
            throw new IllegalArgumentException("canvas object must be the same instance that was previously returned by lockCanvas");
        }
        if (this.mNativeObject != this.mLockedObject) {
            Log.w(TAG, "WARNING: Surface's mNativeObject (0x" + Long.toHexString(this.mNativeObject) + ") != mLockedObject (0x" + Long.toHexString(this.mLockedObject) + ")");
        }
        if (this.mLockedObject == 0) {
            throw new IllegalStateException("Surface was not locked");
        }
        try {
            nativeUnlockCanvasAndPost(this.mLockedObject, canvas);
        } finally {
            nativeRelease(this.mLockedObject);
            this.mLockedObject = 0L;
        }
    }

    public Canvas lockHardwareCanvas() {
        Canvas canvasLockCanvas;
        Log.d(TAG, "lockHardwareCanvas");
        synchronized (this.mLock) {
            checkNotReleasedLocked();
            if (this.mHwuiContext == null) {
                this.mHwuiContext = new HwuiContext(false);
            }
            canvasLockCanvas = this.mHwuiContext.lockCanvas(nativeGetWidth(this.mNativeObject), nativeGetHeight(this.mNativeObject));
        }
        return canvasLockCanvas;
    }

    public Canvas lockHardwareWideColorGamutCanvas() {
        Canvas canvasLockCanvas;
        synchronized (this.mLock) {
            checkNotReleasedLocked();
            if (this.mHwuiContext != null && !this.mHwuiContext.isWideColorGamut()) {
                this.mHwuiContext.destroy();
                this.mHwuiContext = null;
            }
            if (this.mHwuiContext == null) {
                this.mHwuiContext = new HwuiContext(true);
            }
            canvasLockCanvas = this.mHwuiContext.lockCanvas(nativeGetWidth(this.mNativeObject), nativeGetHeight(this.mNativeObject));
        }
        return canvasLockCanvas;
    }

    @Deprecated
    public void unlockCanvas(Canvas canvas) {
        throw new UnsupportedOperationException();
    }

    void setCompatibilityTranslator(CompatibilityInfo.Translator translator) {
        if (translator != null) {
            float f = translator.applicationScale;
            this.mCompatibleMatrix = new Matrix();
            this.mCompatibleMatrix.setScale(f, f);
        }
    }

    public void copyFrom(SurfaceControl surfaceControl) {
        if (surfaceControl == null) {
            throw new IllegalArgumentException("other must not be null");
        }
        long j = surfaceControl.mNativeObject;
        if (j == 0) {
            throw new NullPointerException("null SurfaceControl native object. Are you using a released SurfaceControl?");
        }
        long jNativeGetFromSurfaceControl = nativeGetFromSurfaceControl(j);
        synchronized (this.mLock) {
            if (this.mNativeObject != 0) {
                nativeRelease(this.mNativeObject);
            }
            setNativeObjectLocked(jNativeGetFromSurfaceControl);
        }
    }

    public void createFrom(SurfaceControl surfaceControl) {
        if (surfaceControl == null) {
            throw new IllegalArgumentException("other must not be null");
        }
        long j = surfaceControl.mNativeObject;
        if (j == 0) {
            throw new NullPointerException("null SurfaceControl native object. Are you using a released SurfaceControl?");
        }
        long jNativeCreateFromSurfaceControl = nativeCreateFromSurfaceControl(j);
        synchronized (this.mLock) {
            if (this.mNativeObject != 0) {
                nativeRelease(this.mNativeObject);
            }
            setNativeObjectLocked(jNativeCreateFromSurfaceControl);
        }
    }

    @Deprecated
    public void transferFrom(Surface surface) {
        long j;
        if (surface == null) {
            throw new IllegalArgumentException("other must not be null");
        }
        if (surface != this) {
            synchronized (surface.mLock) {
                j = surface.mNativeObject;
                surface.setNativeObjectLocked(0L);
            }
            synchronized (this.mLock) {
                if (this.mNativeObject != 0) {
                    nativeRelease(this.mNativeObject);
                }
                setNativeObjectLocked(j);
            }
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public void readFromParcel(Parcel parcel) {
        if (parcel == null) {
            throw new IllegalArgumentException("source must not be null");
        }
        synchronized (this.mLock) {
            this.mName = parcel.readString();
            this.mIsSingleBuffered = parcel.readInt() != 0;
            setNativeObjectLocked(nativeReadFromParcel(this.mNativeObject, parcel));
        }
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        if (parcel == null) {
            throw new IllegalArgumentException("dest must not be null");
        }
        synchronized (this.mLock) {
            parcel.writeString(this.mName);
            parcel.writeInt(this.mIsSingleBuffered ? 1 : 0);
            nativeWriteToParcel(this.mNativeObject, parcel);
        }
        if ((i & 1) != 0) {
            release();
        }
    }

    public String toString() {
        String str;
        synchronized (this.mLock) {
            str = "Surface(name=" + this.mName + ")/@0x" + Integer.toHexString(System.identityHashCode(this));
        }
        return str;
    }

    private void setNativeObjectLocked(long j) {
        if (this.mNativeObject != j) {
            if (this.mNativeObject == 0 && j != 0) {
                this.mCloseGuard.open("release");
            } else if (this.mNativeObject != 0 && j == 0) {
                this.mCloseGuard.close();
            }
            this.mNativeObject = j;
            this.mGenerationId++;
            if (this.mHwuiContext != null) {
                this.mHwuiContext.updateSurface();
            }
        }
    }

    private void checkNotReleasedLocked() {
        if (this.mNativeObject == 0) {
            throw new IllegalStateException("Surface has already been released.");
        }
    }

    public void allocateBuffers() {
        synchronized (this.mLock) {
            checkNotReleasedLocked();
            nativeAllocateBuffers(this.mNativeObject);
        }
    }

    void setScalingMode(int i) {
        synchronized (this.mLock) {
            checkNotReleasedLocked();
            if (nativeSetScalingMode(this.mNativeObject, i) != 0) {
                throw new IllegalArgumentException("Invalid scaling mode: " + i);
            }
        }
    }

    void forceScopedDisconnect() {
        synchronized (this.mLock) {
            checkNotReleasedLocked();
            if (nativeForceScopedDisconnect(this.mNativeObject) != 0) {
                throw new RuntimeException("Failed to disconnect Surface instance (bad object?)");
            }
        }
    }

    public void attachAndQueueBuffer(GraphicBuffer graphicBuffer) {
        synchronized (this.mLock) {
            checkNotReleasedLocked();
            if (nativeAttachAndQueueBuffer(this.mNativeObject, graphicBuffer) != 0) {
                throw new RuntimeException("Failed to attach and queue buffer to Surface (bad object?)");
            }
        }
    }

    public boolean isSingleBuffered() {
        return this.mIsSingleBuffered;
    }

    public void setSharedBufferModeEnabled(boolean z) {
        if (this.mIsSharedBufferModeEnabled != z) {
            if (nativeSetSharedBufferModeEnabled(this.mNativeObject, z) != 0) {
                throw new RuntimeException("Failed to set shared buffer mode on Surface (bad object?)");
            }
            this.mIsSharedBufferModeEnabled = z;
        }
    }

    public boolean isSharedBufferModeEnabled() {
        return this.mIsSharedBufferModeEnabled;
    }

    public void setAutoRefreshEnabled(boolean z) {
        if (this.mIsAutoRefreshEnabled != z) {
            if (nativeSetAutoRefreshEnabled(this.mNativeObject, z) != 0) {
                throw new RuntimeException("Failed to set auto refresh on Surface (bad object?)");
            }
            this.mIsAutoRefreshEnabled = z;
        }
    }

    public boolean isAutoRefreshEnabled() {
        return this.mIsAutoRefreshEnabled;
    }

    public static class OutOfResourcesException extends RuntimeException {
        public OutOfResourcesException() {
        }

        public OutOfResourcesException(String str) {
            super(str);
        }
    }

    public static String rotationToString(int i) {
        switch (i) {
            case 0:
                return "ROTATION_0";
            case 1:
                return "ROTATION_90";
            case 2:
                return "ROTATION_180";
            case 3:
                return "ROTATION_270";
            default:
                return Integer.toString(i);
        }
    }

    private final class CompatibleCanvas extends Canvas {
        private Matrix mOrigMatrix;

        private CompatibleCanvas() {
            this.mOrigMatrix = null;
        }

        @Override
        public void setMatrix(Matrix matrix) {
            if (Surface.this.mCompatibleMatrix == null || this.mOrigMatrix == null || this.mOrigMatrix.equals(matrix)) {
                super.setMatrix(matrix);
                return;
            }
            Matrix matrix2 = new Matrix(Surface.this.mCompatibleMatrix);
            matrix2.preConcat(matrix);
            super.setMatrix(matrix2);
        }

        @Override
        public void getMatrix(Matrix matrix) {
            super.getMatrix(matrix);
            if (this.mOrigMatrix == null) {
                this.mOrigMatrix = new Matrix();
            }
            this.mOrigMatrix.set(matrix);
        }
    }

    private final class HwuiContext {
        private DisplayListCanvas mCanvas;
        private long mHwuiRenderer;
        private final boolean mIsWideColorGamut;
        private final RenderNode mRenderNode = RenderNode.create("HwuiCanvas", null);

        HwuiContext(boolean z) {
            this.mRenderNode.setClipToBounds(false);
            this.mIsWideColorGamut = z;
            this.mHwuiRenderer = Surface.nHwuiCreate(this.mRenderNode.mNativeRenderNode, Surface.this.mNativeObject, z);
        }

        Canvas lockCanvas(int i, int i2) {
            if (this.mCanvas != null) {
                throw new IllegalStateException("Surface was already locked!");
            }
            this.mCanvas = this.mRenderNode.start(i, i2);
            return this.mCanvas;
        }

        void unlockAndPost(Canvas canvas) {
            if (canvas != this.mCanvas) {
                throw new IllegalArgumentException("canvas object must be the same instance that was previously returned by lockCanvas");
            }
            this.mRenderNode.end(this.mCanvas);
            this.mCanvas = null;
            Surface.nHwuiDraw(this.mHwuiRenderer);
        }

        void updateSurface() {
            Surface.nHwuiSetSurface(this.mHwuiRenderer, Surface.this.mNativeObject);
        }

        void destroy() {
            if (this.mHwuiRenderer != 0) {
                Surface.nHwuiDestroy(this.mHwuiRenderer);
                this.mHwuiRenderer = 0L;
            }
        }

        boolean isWideColorGamut() {
            return this.mIsWideColorGamut;
        }
    }
}
