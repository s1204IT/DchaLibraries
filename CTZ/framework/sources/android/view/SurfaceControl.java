package android.view;

import android.graphics.Bitmap;
import android.graphics.GraphicBuffer;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.Region;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Process;
import android.os.UserHandle;
import android.util.ArrayMap;
import android.util.Log;
import android.util.proto.ProtoOutputStream;
import android.view.Display;
import android.view.Surface;
import com.android.internal.annotations.GuardedBy;
import dalvik.system.CloseGuard;
import java.io.Closeable;
import libcore.util.NativeAllocationRegistry;

public class SurfaceControl implements Parcelable {
    public static final int BUILT_IN_DISPLAY_ID_HDMI = 1;
    public static final int BUILT_IN_DISPLAY_ID_MAIN = 0;
    public static final int CURSOR_WINDOW = 8192;
    public static final int FX_SURFACE_CONTAINER = 524288;
    public static final int FX_SURFACE_DIM = 131072;
    public static final int FX_SURFACE_MASK = 983040;
    public static final int FX_SURFACE_NORMAL = 0;
    public static final int HIDDEN = 4;
    public static final int NON_PREMULTIPLIED = 256;
    public static final int OPAQUE = 1024;
    public static final int POWER_MODE_DOZE = 1;
    public static final int POWER_MODE_DOZE_SUSPEND = 3;
    public static final int POWER_MODE_NORMAL = 2;
    public static final int POWER_MODE_OFF = 0;
    public static final int POWER_MODE_ON_SUSPEND = 4;
    public static final int PROTECTED_APP = 2048;
    public static final int SECURE = 128;
    private static final int SURFACE_HIDDEN = 1;
    private static final int SURFACE_OPAQUE = 2;
    private static final String TAG = "SurfaceControl";
    public static final int WINDOW_TYPE_DONT_SCREENSHOT = 441731;
    static Transaction sGlobalTransaction;
    private final CloseGuard mCloseGuard;

    @GuardedBy("mSizeLock")
    private int mHeight;
    private final String mName;
    long mNativeObject;
    private final Object mSizeLock;

    @GuardedBy("mSizeLock")
    private int mWidth;
    static long sTransactionNestCount = 0;
    public static final Parcelable.Creator<SurfaceControl> CREATOR = new Parcelable.Creator<SurfaceControl>() {
        @Override
        public SurfaceControl createFromParcel(Parcel parcel) {
            return new SurfaceControl(parcel);
        }

        @Override
        public SurfaceControl[] newArray(int i) {
            return new SurfaceControl[i];
        }
    };

    private static native void nativeApplyTransaction(long j, boolean z);

    private static native GraphicBuffer nativeCaptureLayers(IBinder iBinder, Rect rect, float f);

    private static native boolean nativeClearAnimationFrameStats();

    private static native boolean nativeClearContentFrameStats(long j);

    private static native long nativeCreate(SurfaceSession surfaceSession, String str, int i, int i2, int i3, int i4, long j, int i5, int i6) throws Surface.OutOfResourcesException;

    private static native IBinder nativeCreateDisplay(String str, boolean z);

    private static native long nativeCreateTransaction();

    private static native void nativeDeferTransactionUntil(long j, long j2, IBinder iBinder, long j3);

    private static native void nativeDeferTransactionUntilSurface(long j, long j2, long j3, long j4);

    private static native void nativeDestroy(long j);

    private static native void nativeDestroy(long j, long j2);

    private static native void nativeDestroyDisplay(IBinder iBinder);

    private static native void nativeDisconnect(long j);

    private static native int nativeGetActiveColorMode(IBinder iBinder);

    private static native int nativeGetActiveConfig(IBinder iBinder);

    private static native boolean nativeGetAnimationFrameStats(WindowAnimationFrameStats windowAnimationFrameStats);

    private static native IBinder nativeGetBuiltInDisplay(int i);

    private static native boolean nativeGetContentFrameStats(long j, WindowContentFrameStats windowContentFrameStats);

    private static native int[] nativeGetDisplayColorModes(IBinder iBinder);

    private static native PhysicalDisplayInfo[] nativeGetDisplayConfigs(IBinder iBinder);

    private static native IBinder nativeGetHandle(long j);

    private static native Display.HdrCapabilities nativeGetHdrCapabilities(IBinder iBinder);

    private static native long nativeGetNativeTransactionFinalizer();

    private static native boolean nativeGetTransformToDisplayInverse(long j);

    private static native void nativeMergeTransaction(long j, long j2);

    private static native long nativeReadFromParcel(Parcel parcel);

    private static native void nativeRelease(long j);

    private static native void nativeReparent(long j, long j2, IBinder iBinder);

    private static native void nativeReparentChildren(long j, long j2, IBinder iBinder);

    private static native Bitmap nativeScreenshot(IBinder iBinder, Rect rect, int i, int i2, int i3, int i4, boolean z, boolean z2, int i5);

    private static native void nativeScreenshot(IBinder iBinder, Surface surface, Rect rect, int i, int i2, int i3, int i4, boolean z, boolean z2);

    private static native GraphicBuffer nativeScreenshotToBuffer(IBinder iBinder, Rect rect, int i, int i2, int i3, int i4, boolean z, boolean z2, int i5, boolean z3);

    private static native boolean nativeSetActiveColorMode(IBinder iBinder, int i);

    private static native boolean nativeSetActiveConfig(IBinder iBinder, int i);

    private static native void nativeSetAlpha(long j, long j2, float f);

    private static native void nativeSetAnimationTransaction(long j);

    private static native void nativeSetColor(long j, long j2, float[] fArr);

    private static native void nativeSetDisplayLayerStack(long j, IBinder iBinder, int i);

    private static native void nativeSetDisplayPowerMode(IBinder iBinder, int i);

    private static native void nativeSetDisplayProjection(long j, IBinder iBinder, int i, int i2, int i3, int i4, int i5, int i6, int i7, int i8, int i9);

    private static native void nativeSetDisplaySize(long j, IBinder iBinder, int i, int i2);

    private static native void nativeSetDisplaySurface(long j, IBinder iBinder, long j2);

    private static native void nativeSetEarlyWakeup(long j);

    private static native void nativeSetFinalCrop(long j, long j2, int i, int i2, int i3, int i4);

    private static native void nativeSetFlags(long j, long j2, int i, int i2);

    private static native void nativeSetGeometryAppliesWithResize(long j, long j2);

    private static native void nativeSetLayer(long j, long j2, int i);

    private static native void nativeSetLayerStack(long j, long j2, int i);

    private static native void nativeSetMatrix(long j, long j2, float f, float f2, float f3, float f4);

    private static native void nativeSetOverrideScalingMode(long j, long j2, int i);

    private static native void nativeSetPosition(long j, long j2, float f, float f2);

    private static native void nativeSetRelativeLayer(long j, long j2, IBinder iBinder, int i);

    private static native void nativeSetSize(long j, long j2, int i, int i2);

    private static native void nativeSetTransparentRegionHint(long j, long j2, Region region);

    private static native void nativeSetWindowCrop(long j, long j2, int i, int i2, int i3, int i4);

    private static native void nativeSeverChildren(long j, long j2);

    private static native void nativeWriteToParcel(long j, Parcel parcel);

    public static class Builder {
        private int mHeight;
        private String mName;
        private SurfaceControl mParent;
        private SurfaceSession mSession;
        private int mWidth;
        private int mFlags = 4;
        private int mFormat = -1;
        private int mWindowType = -1;
        private int mOwnerUid = -1;

        public Builder(SurfaceSession surfaceSession) {
            this.mSession = surfaceSession;
        }

        public SurfaceControl build() {
            if (this.mWidth <= 0 || this.mHeight <= 0) {
                throw new IllegalArgumentException("width and height must be set");
            }
            return new SurfaceControl(this.mSession, this.mName, this.mWidth, this.mHeight, this.mFormat, this.mFlags, this.mParent, this.mWindowType, this.mOwnerUid);
        }

        public Builder setName(String str) {
            this.mName = str;
            return this;
        }

        public Builder setSize(int i, int i2) {
            if (i <= 0 || i2 <= 0) {
                throw new IllegalArgumentException("width and height must be positive");
            }
            this.mWidth = i;
            this.mHeight = i2;
            return this;
        }

        public Builder setFormat(int i) {
            this.mFormat = i;
            return this;
        }

        public Builder setProtected(boolean z) {
            if (z) {
                this.mFlags |= 2048;
            } else {
                this.mFlags &= -2049;
            }
            return this;
        }

        public Builder setSecure(boolean z) {
            if (z) {
                this.mFlags |= 128;
            } else {
                this.mFlags &= -129;
            }
            return this;
        }

        public Builder setOpaque(boolean z) {
            if (z) {
                this.mFlags |= 1024;
            } else {
                this.mFlags &= -1025;
            }
            return this;
        }

        public Builder setParent(SurfaceControl surfaceControl) {
            this.mParent = surfaceControl;
            return this;
        }

        public Builder setMetadata(int i, int i2) {
            if (UserHandle.getAppId(Process.myUid()) != 1000) {
                throw new UnsupportedOperationException("It only makes sense to set Surface metadata from the WindowManager");
            }
            this.mWindowType = i;
            this.mOwnerUid = i2;
            return this;
        }

        public Builder setColorLayer(boolean z) {
            if (z) {
                setFlags(131072, SurfaceControl.FX_SURFACE_MASK);
            } else {
                setBufferLayer();
            }
            return this;
        }

        public Builder setContainerLayer(boolean z) {
            if (z) {
                setFlags(524288, SurfaceControl.FX_SURFACE_MASK);
            } else {
                setBufferLayer();
            }
            return this;
        }

        public Builder setBufferLayer() {
            return setFlags(0, SurfaceControl.FX_SURFACE_MASK);
        }

        public Builder setFlags(int i) {
            this.mFlags = i;
            return this;
        }

        private Builder setFlags(int i, int i2) {
            this.mFlags = i | ((~i2) & this.mFlags);
            return this;
        }
    }

    private SurfaceControl(SurfaceSession surfaceSession, String str, int i, int i2, int i3, int i4, SurfaceControl surfaceControl, int i5, int i6) throws Surface.OutOfResourcesException, IllegalArgumentException {
        this.mCloseGuard = CloseGuard.get();
        this.mSizeLock = new Object();
        if (surfaceSession == null) {
            throw new IllegalArgumentException("session must not be null");
        }
        if (str == null) {
            throw new IllegalArgumentException("name must not be null");
        }
        if ((i4 & 4) == 0) {
            Log.w(TAG, "Surfaces should always be created with the HIDDEN flag set to ensure that they are not made visible prematurely before all of the surface's properties have been configured.  Set the other properties and make the surface visible within a transaction.  New surface name: " + str, new Throwable());
        }
        this.mName = str;
        this.mWidth = i;
        this.mHeight = i2;
        this.mNativeObject = nativeCreate(surfaceSession, str, i, i2, i3, i4, surfaceControl != null ? surfaceControl.mNativeObject : 0L, i5, i6);
        if (this.mNativeObject == 0) {
            throw new Surface.OutOfResourcesException("Couldn't allocate SurfaceControl native object");
        }
        this.mCloseGuard.open("release");
    }

    public SurfaceControl(SurfaceControl surfaceControl) {
        this.mCloseGuard = CloseGuard.get();
        this.mSizeLock = new Object();
        this.mName = surfaceControl.mName;
        this.mWidth = surfaceControl.mWidth;
        this.mHeight = surfaceControl.mHeight;
        this.mNativeObject = surfaceControl.mNativeObject;
        surfaceControl.mCloseGuard.close();
        surfaceControl.mNativeObject = 0L;
        this.mCloseGuard.open("release");
    }

    private SurfaceControl(Parcel parcel) {
        this.mCloseGuard = CloseGuard.get();
        this.mSizeLock = new Object();
        this.mName = parcel.readString();
        this.mWidth = parcel.readInt();
        this.mHeight = parcel.readInt();
        this.mNativeObject = nativeReadFromParcel(parcel);
        if (this.mNativeObject == 0) {
            throw new IllegalArgumentException("Couldn't read SurfaceControl from parcel=" + parcel);
        }
        this.mCloseGuard.open("release");
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(this.mName);
        parcel.writeInt(this.mWidth);
        parcel.writeInt(this.mHeight);
        nativeWriteToParcel(this.mNativeObject, parcel);
    }

    public void writeToProto(ProtoOutputStream protoOutputStream, long j) {
        long jStart = protoOutputStream.start(j);
        protoOutputStream.write(1120986464257L, System.identityHashCode(this));
        protoOutputStream.write(1138166333442L, this.mName);
        protoOutputStream.end(jStart);
    }

    protected void finalize() throws Throwable {
        try {
            if (this.mCloseGuard != null) {
                this.mCloseGuard.warnIfOpen();
            }
            if (this.mNativeObject != 0) {
                nativeRelease(this.mNativeObject);
            }
        } finally {
            super.finalize();
        }
    }

    public void release() {
        if (this.mNativeObject != 0) {
            nativeRelease(this.mNativeObject);
            this.mNativeObject = 0L;
        }
        this.mCloseGuard.close();
    }

    public void destroy() {
        if (this.mNativeObject != 0) {
            nativeDestroy(this.mNativeObject);
            this.mNativeObject = 0L;
        }
        this.mCloseGuard.close();
    }

    public void disconnect() {
        if (this.mNativeObject != 0) {
            nativeDisconnect(this.mNativeObject);
        }
    }

    private void checkNotReleased() {
        if (this.mNativeObject == 0) {
            throw new NullPointerException("mNativeObject is null. Have you called release() already?");
        }
    }

    public static void openTransaction() {
        synchronized (SurfaceControl.class) {
            if (sGlobalTransaction == null) {
                sGlobalTransaction = new Transaction();
            }
            synchronized (SurfaceControl.class) {
                sTransactionNestCount++;
            }
        }
    }

    private static void closeTransaction(boolean z) {
        synchronized (SurfaceControl.class) {
            if (sTransactionNestCount == 0) {
                Log.e(TAG, "Call to SurfaceControl.closeTransaction without matching openTransaction");
            } else {
                long j = sTransactionNestCount - 1;
                sTransactionNestCount = j;
                if (j > 0) {
                    return;
                }
            }
            sGlobalTransaction.apply(z);
        }
    }

    @Deprecated
    public static void mergeToGlobalTransaction(Transaction transaction) {
        synchronized (SurfaceControl.class) {
            sGlobalTransaction.merge(transaction);
        }
    }

    public static void closeTransaction() {
        closeTransaction(false);
    }

    public static void closeTransactionSync() {
        closeTransaction(true);
    }

    public void deferTransactionUntil(IBinder iBinder, long j) {
        synchronized (SurfaceControl.class) {
            sGlobalTransaction.deferTransactionUntil(this, iBinder, j);
        }
    }

    public void deferTransactionUntil(Surface surface, long j) {
        synchronized (SurfaceControl.class) {
            sGlobalTransaction.deferTransactionUntilSurface(this, surface, j);
        }
    }

    public void reparentChildren(IBinder iBinder) {
        synchronized (SurfaceControl.class) {
            sGlobalTransaction.reparentChildren(this, iBinder);
        }
    }

    public void reparent(IBinder iBinder) {
        synchronized (SurfaceControl.class) {
            sGlobalTransaction.reparent(this, iBinder);
        }
    }

    public void detachChildren() {
        synchronized (SurfaceControl.class) {
            sGlobalTransaction.detachChildren(this);
        }
    }

    public void setOverrideScalingMode(int i) {
        checkNotReleased();
        synchronized (SurfaceControl.class) {
            sGlobalTransaction.setOverrideScalingMode(this, i);
        }
    }

    public IBinder getHandle() {
        return nativeGetHandle(this.mNativeObject);
    }

    public static void setAnimationTransaction() {
        synchronized (SurfaceControl.class) {
            sGlobalTransaction.setAnimationTransaction();
        }
    }

    public void setLayer(int i) {
        checkNotReleased();
        synchronized (SurfaceControl.class) {
            sGlobalTransaction.setLayer(this, i);
        }
    }

    public void setRelativeLayer(SurfaceControl surfaceControl, int i) {
        checkNotReleased();
        synchronized (SurfaceControl.class) {
            sGlobalTransaction.setRelativeLayer(this, surfaceControl, i);
        }
    }

    public void setPosition(float f, float f2) {
        checkNotReleased();
        synchronized (SurfaceControl.class) {
            sGlobalTransaction.setPosition(this, f, f2);
        }
    }

    public void setGeometryAppliesWithResize() {
        checkNotReleased();
        synchronized (SurfaceControl.class) {
            sGlobalTransaction.setGeometryAppliesWithResize(this);
        }
    }

    public void setSize(int i, int i2) {
        checkNotReleased();
        synchronized (SurfaceControl.class) {
            sGlobalTransaction.setSize(this, i, i2);
        }
    }

    public void hide() {
        checkNotReleased();
        synchronized (SurfaceControl.class) {
            sGlobalTransaction.hide(this);
        }
    }

    public void show() {
        checkNotReleased();
        synchronized (SurfaceControl.class) {
            sGlobalTransaction.show(this);
        }
    }

    public void setTransparentRegionHint(Region region) {
        checkNotReleased();
        synchronized (SurfaceControl.class) {
            sGlobalTransaction.setTransparentRegionHint(this, region);
        }
    }

    public boolean clearContentFrameStats() {
        checkNotReleased();
        return nativeClearContentFrameStats(this.mNativeObject);
    }

    public boolean getContentFrameStats(WindowContentFrameStats windowContentFrameStats) {
        checkNotReleased();
        return nativeGetContentFrameStats(this.mNativeObject, windowContentFrameStats);
    }

    public static boolean clearAnimationFrameStats() {
        return nativeClearAnimationFrameStats();
    }

    public static boolean getAnimationFrameStats(WindowAnimationFrameStats windowAnimationFrameStats) {
        return nativeGetAnimationFrameStats(windowAnimationFrameStats);
    }

    public void setAlpha(float f) {
        checkNotReleased();
        synchronized (SurfaceControl.class) {
            sGlobalTransaction.setAlpha(this, f);
        }
    }

    public void setColor(float[] fArr) {
        checkNotReleased();
        synchronized (SurfaceControl.class) {
            sGlobalTransaction.setColor(this, fArr);
        }
    }

    public void setMatrix(float f, float f2, float f3, float f4) {
        checkNotReleased();
        synchronized (SurfaceControl.class) {
            sGlobalTransaction.setMatrix(this, f, f2, f3, f4);
        }
    }

    public void setMatrix(Matrix matrix, float[] fArr) {
        checkNotReleased();
        matrix.getValues(fArr);
        synchronized (SurfaceControl.class) {
            sGlobalTransaction.setMatrix(this, fArr[0], fArr[3], fArr[1], fArr[4]);
            sGlobalTransaction.setPosition(this, fArr[2], fArr[5]);
        }
    }

    public void setWindowCrop(Rect rect) {
        checkNotReleased();
        synchronized (SurfaceControl.class) {
            sGlobalTransaction.setWindowCrop(this, rect);
        }
    }

    public void setFinalCrop(Rect rect) {
        checkNotReleased();
        synchronized (SurfaceControl.class) {
            sGlobalTransaction.setFinalCrop(this, rect);
        }
    }

    public void setLayerStack(int i) {
        checkNotReleased();
        synchronized (SurfaceControl.class) {
            sGlobalTransaction.setLayerStack(this, i);
        }
    }

    public void setOpaque(boolean z) {
        checkNotReleased();
        synchronized (SurfaceControl.class) {
            sGlobalTransaction.setOpaque(this, z);
        }
    }

    public void setSecure(boolean z) {
        checkNotReleased();
        synchronized (SurfaceControl.class) {
            sGlobalTransaction.setSecure(this, z);
        }
    }

    public int getWidth() {
        int i;
        synchronized (this.mSizeLock) {
            i = this.mWidth;
        }
        return i;
    }

    public int getHeight() {
        int i;
        synchronized (this.mSizeLock) {
            i = this.mHeight;
        }
        return i;
    }

    public String toString() {
        return "Surface(name=" + this.mName + ")/@0x" + Integer.toHexString(System.identityHashCode(this));
    }

    public static final class PhysicalDisplayInfo {
        public long appVsyncOffsetNanos;
        public float density;
        public int height;
        public long presentationDeadlineNanos;
        public float refreshRate;
        public boolean secure;
        public int width;
        public float xDpi;
        public float yDpi;

        public PhysicalDisplayInfo() {
        }

        public PhysicalDisplayInfo(PhysicalDisplayInfo physicalDisplayInfo) {
            copyFrom(physicalDisplayInfo);
        }

        public boolean equals(Object obj) {
            return (obj instanceof PhysicalDisplayInfo) && equals((PhysicalDisplayInfo) obj);
        }

        public boolean equals(PhysicalDisplayInfo physicalDisplayInfo) {
            return physicalDisplayInfo != null && this.width == physicalDisplayInfo.width && this.height == physicalDisplayInfo.height && this.refreshRate == physicalDisplayInfo.refreshRate && this.density == physicalDisplayInfo.density && this.xDpi == physicalDisplayInfo.xDpi && this.yDpi == physicalDisplayInfo.yDpi && this.secure == physicalDisplayInfo.secure && this.appVsyncOffsetNanos == physicalDisplayInfo.appVsyncOffsetNanos && this.presentationDeadlineNanos == physicalDisplayInfo.presentationDeadlineNanos;
        }

        public int hashCode() {
            return 0;
        }

        public void copyFrom(PhysicalDisplayInfo physicalDisplayInfo) {
            this.width = physicalDisplayInfo.width;
            this.height = physicalDisplayInfo.height;
            this.refreshRate = physicalDisplayInfo.refreshRate;
            this.density = physicalDisplayInfo.density;
            this.xDpi = physicalDisplayInfo.xDpi;
            this.yDpi = physicalDisplayInfo.yDpi;
            this.secure = physicalDisplayInfo.secure;
            this.appVsyncOffsetNanos = physicalDisplayInfo.appVsyncOffsetNanos;
            this.presentationDeadlineNanos = physicalDisplayInfo.presentationDeadlineNanos;
        }

        public String toString() {
            return "PhysicalDisplayInfo{" + this.width + " x " + this.height + ", " + this.refreshRate + " fps, density " + this.density + ", " + this.xDpi + " x " + this.yDpi + " dpi, secure " + this.secure + ", appVsyncOffset " + this.appVsyncOffsetNanos + ", bufferDeadline " + this.presentationDeadlineNanos + "}";
        }
    }

    public static void setDisplayPowerMode(IBinder iBinder, int i) {
        if (iBinder == null) {
            throw new IllegalArgumentException("displayToken must not be null");
        }
        nativeSetDisplayPowerMode(iBinder, i);
    }

    public static PhysicalDisplayInfo[] getDisplayConfigs(IBinder iBinder) {
        if (iBinder == null) {
            throw new IllegalArgumentException("displayToken must not be null");
        }
        return nativeGetDisplayConfigs(iBinder);
    }

    public static int getActiveConfig(IBinder iBinder) {
        if (iBinder == null) {
            throw new IllegalArgumentException("displayToken must not be null");
        }
        return nativeGetActiveConfig(iBinder);
    }

    public static boolean setActiveConfig(IBinder iBinder, int i) {
        if (iBinder == null) {
            throw new IllegalArgumentException("displayToken must not be null");
        }
        return nativeSetActiveConfig(iBinder, i);
    }

    public static int[] getDisplayColorModes(IBinder iBinder) {
        if (iBinder == null) {
            throw new IllegalArgumentException("displayToken must not be null");
        }
        return nativeGetDisplayColorModes(iBinder);
    }

    public static int getActiveColorMode(IBinder iBinder) {
        if (iBinder == null) {
            throw new IllegalArgumentException("displayToken must not be null");
        }
        return nativeGetActiveColorMode(iBinder);
    }

    public static boolean setActiveColorMode(IBinder iBinder, int i) {
        if (iBinder == null) {
            throw new IllegalArgumentException("displayToken must not be null");
        }
        return nativeSetActiveColorMode(iBinder, i);
    }

    public static void setDisplayProjection(IBinder iBinder, int i, Rect rect, Rect rect2) {
        synchronized (SurfaceControl.class) {
            sGlobalTransaction.setDisplayProjection(iBinder, i, rect, rect2);
        }
    }

    public static void setDisplayLayerStack(IBinder iBinder, int i) {
        synchronized (SurfaceControl.class) {
            sGlobalTransaction.setDisplayLayerStack(iBinder, i);
        }
    }

    public static void setDisplaySurface(IBinder iBinder, Surface surface) {
        synchronized (SurfaceControl.class) {
            sGlobalTransaction.setDisplaySurface(iBinder, surface);
        }
    }

    public static void setDisplaySize(IBinder iBinder, int i, int i2) {
        synchronized (SurfaceControl.class) {
            sGlobalTransaction.setDisplaySize(iBinder, i, i2);
        }
    }

    public static Display.HdrCapabilities getHdrCapabilities(IBinder iBinder) {
        if (iBinder == null) {
            throw new IllegalArgumentException("displayToken must not be null");
        }
        return nativeGetHdrCapabilities(iBinder);
    }

    public static IBinder createDisplay(String str, boolean z) {
        if (str == null) {
            throw new IllegalArgumentException("name must not be null");
        }
        return nativeCreateDisplay(str, z);
    }

    public static void destroyDisplay(IBinder iBinder) {
        if (iBinder == null) {
            throw new IllegalArgumentException("displayToken must not be null");
        }
        nativeDestroyDisplay(iBinder);
    }

    public static IBinder getBuiltInDisplay(int i) {
        return nativeGetBuiltInDisplay(i);
    }

    public static void screenshot(IBinder iBinder, Surface surface, int i, int i2, int i3, int i4, boolean z) {
        screenshot(iBinder, surface, new Rect(), i, i2, i3, i4, false, z);
    }

    public static void screenshot(IBinder iBinder, Surface surface, int i, int i2) {
        screenshot(iBinder, surface, new Rect(), i, i2, 0, 0, true, false);
    }

    public static void screenshot(IBinder iBinder, Surface surface) {
        screenshot(iBinder, surface, new Rect(), 0, 0, 0, 0, true, false);
    }

    public static Bitmap screenshot(Rect rect, int i, int i2, int i3, int i4, boolean z, int i5) {
        return nativeScreenshot(getBuiltInDisplay(0), rect, i, i2, i3, i4, false, z, i5);
    }

    public static GraphicBuffer screenshotToBuffer(Rect rect, int i, int i2, int i3, int i4, boolean z, int i5) {
        return nativeScreenshotToBuffer(getBuiltInDisplay(0), rect, i, i2, i3, i4, false, z, i5, false);
    }

    public static GraphicBuffer screenshotToBufferWithSecureLayersUnsafe(Rect rect, int i, int i2, int i3, int i4, boolean z, int i5) {
        return nativeScreenshotToBuffer(getBuiltInDisplay(0), rect, i, i2, i3, i4, false, z, i5, true);
    }

    public static Bitmap screenshot(Rect rect, int i, int i2, int i3) {
        IBinder builtInDisplay = getBuiltInDisplay(0);
        if (i3 == 1 || i3 == 3) {
            i3 = i3 == 1 ? 3 : 1;
        }
        int i4 = i3;
        rotateCropForSF(rect, i4);
        return nativeScreenshot(builtInDisplay, rect, i, i2, 0, 0, true, false, i4);
    }

    private static void screenshot(IBinder iBinder, Surface surface, Rect rect, int i, int i2, int i3, int i4, boolean z, boolean z2) {
        if (iBinder == null) {
            throw new IllegalArgumentException("displayToken must not be null");
        }
        if (surface == null) {
            throw new IllegalArgumentException("consumer must not be null");
        }
        nativeScreenshot(iBinder, surface, rect, i, i2, i3, i4, z, z2);
    }

    private static void rotateCropForSF(Rect rect, int i) {
        if (i == 1 || i == 3) {
            int i2 = rect.top;
            rect.top = rect.left;
            rect.left = i2;
            int i3 = rect.right;
            rect.right = rect.bottom;
            rect.bottom = i3;
        }
    }

    public static GraphicBuffer captureLayers(IBinder iBinder, Rect rect, float f) {
        return nativeCaptureLayers(iBinder, rect, f);
    }

    public static class Transaction implements Closeable {
        public static final NativeAllocationRegistry sRegistry = new NativeAllocationRegistry(Transaction.class.getClassLoader(), SurfaceControl.nativeGetNativeTransactionFinalizer(), 512);
        private final ArrayMap<SurfaceControl, Point> mResizedSurfaces = new ArrayMap<>();
        private long mNativeObject = SurfaceControl.nativeCreateTransaction();
        Runnable mFreeNativeResources = sRegistry.registerNativeAllocation(this, this.mNativeObject);

        public void apply() {
            apply(false);
        }

        @Override
        public void close() {
            this.mFreeNativeResources.run();
            this.mNativeObject = 0L;
        }

        public void apply(boolean z) {
            applyResizedSurfaces();
            SurfaceControl.nativeApplyTransaction(this.mNativeObject, z);
        }

        private void applyResizedSurfaces() {
            for (int size = this.mResizedSurfaces.size() - 1; size >= 0; size--) {
                Point pointValueAt = this.mResizedSurfaces.valueAt(size);
                SurfaceControl surfaceControlKeyAt = this.mResizedSurfaces.keyAt(size);
                synchronized (surfaceControlKeyAt.mSizeLock) {
                    surfaceControlKeyAt.mWidth = pointValueAt.x;
                    surfaceControlKeyAt.mHeight = pointValueAt.y;
                }
            }
            this.mResizedSurfaces.clear();
        }

        public Transaction show(SurfaceControl surfaceControl) {
            surfaceControl.checkNotReleased();
            SurfaceControl.nativeSetFlags(this.mNativeObject, surfaceControl.mNativeObject, 0, 1);
            return this;
        }

        public Transaction hide(SurfaceControl surfaceControl) {
            surfaceControl.checkNotReleased();
            SurfaceControl.nativeSetFlags(this.mNativeObject, surfaceControl.mNativeObject, 1, 1);
            return this;
        }

        public Transaction setPosition(SurfaceControl surfaceControl, float f, float f2) {
            surfaceControl.checkNotReleased();
            SurfaceControl.nativeSetPosition(this.mNativeObject, surfaceControl.mNativeObject, f, f2);
            return this;
        }

        public Transaction setSize(SurfaceControl surfaceControl, int i, int i2) {
            surfaceControl.checkNotReleased();
            this.mResizedSurfaces.put(surfaceControl, new Point(i, i2));
            SurfaceControl.nativeSetSize(this.mNativeObject, surfaceControl.mNativeObject, i, i2);
            return this;
        }

        public Transaction setLayer(SurfaceControl surfaceControl, int i) {
            surfaceControl.checkNotReleased();
            SurfaceControl.nativeSetLayer(this.mNativeObject, surfaceControl.mNativeObject, i);
            return this;
        }

        public Transaction setRelativeLayer(SurfaceControl surfaceControl, SurfaceControl surfaceControl2, int i) {
            surfaceControl.checkNotReleased();
            SurfaceControl.nativeSetRelativeLayer(this.mNativeObject, surfaceControl.mNativeObject, surfaceControl2.getHandle(), i);
            return this;
        }

        public Transaction setTransparentRegionHint(SurfaceControl surfaceControl, Region region) {
            surfaceControl.checkNotReleased();
            SurfaceControl.nativeSetTransparentRegionHint(this.mNativeObject, surfaceControl.mNativeObject, region);
            return this;
        }

        public Transaction setAlpha(SurfaceControl surfaceControl, float f) {
            surfaceControl.checkNotReleased();
            SurfaceControl.nativeSetAlpha(this.mNativeObject, surfaceControl.mNativeObject, f);
            return this;
        }

        public Transaction setMatrix(SurfaceControl surfaceControl, float f, float f2, float f3, float f4) {
            surfaceControl.checkNotReleased();
            SurfaceControl.nativeSetMatrix(this.mNativeObject, surfaceControl.mNativeObject, f, f2, f3, f4);
            return this;
        }

        public Transaction setMatrix(SurfaceControl surfaceControl, Matrix matrix, float[] fArr) {
            matrix.getValues(fArr);
            setMatrix(surfaceControl, fArr[0], fArr[3], fArr[1], fArr[4]);
            setPosition(surfaceControl, fArr[2], fArr[5]);
            return this;
        }

        public Transaction setWindowCrop(SurfaceControl surfaceControl, Rect rect) {
            surfaceControl.checkNotReleased();
            if (rect != null) {
                SurfaceControl.nativeSetWindowCrop(this.mNativeObject, surfaceControl.mNativeObject, rect.left, rect.top, rect.right, rect.bottom);
            } else {
                SurfaceControl.nativeSetWindowCrop(this.mNativeObject, surfaceControl.mNativeObject, 0, 0, 0, 0);
            }
            return this;
        }

        public Transaction setFinalCrop(SurfaceControl surfaceControl, Rect rect) {
            surfaceControl.checkNotReleased();
            if (rect != null) {
                SurfaceControl.nativeSetFinalCrop(this.mNativeObject, surfaceControl.mNativeObject, rect.left, rect.top, rect.right, rect.bottom);
            } else {
                SurfaceControl.nativeSetFinalCrop(this.mNativeObject, surfaceControl.mNativeObject, 0, 0, 0, 0);
            }
            return this;
        }

        public Transaction setLayerStack(SurfaceControl surfaceControl, int i) {
            surfaceControl.checkNotReleased();
            SurfaceControl.nativeSetLayerStack(this.mNativeObject, surfaceControl.mNativeObject, i);
            return this;
        }

        public Transaction deferTransactionUntil(SurfaceControl surfaceControl, IBinder iBinder, long j) {
            if (j >= 0) {
                surfaceControl.checkNotReleased();
                SurfaceControl.nativeDeferTransactionUntil(this.mNativeObject, surfaceControl.mNativeObject, iBinder, j);
                return this;
            }
            return this;
        }

        public Transaction deferTransactionUntilSurface(SurfaceControl surfaceControl, Surface surface, long j) {
            if (j >= 0) {
                surfaceControl.checkNotReleased();
                SurfaceControl.nativeDeferTransactionUntilSurface(this.mNativeObject, surfaceControl.mNativeObject, surface.mNativeObject, j);
                return this;
            }
            return this;
        }

        public Transaction reparentChildren(SurfaceControl surfaceControl, IBinder iBinder) {
            surfaceControl.checkNotReleased();
            SurfaceControl.nativeReparentChildren(this.mNativeObject, surfaceControl.mNativeObject, iBinder);
            return this;
        }

        public Transaction reparent(SurfaceControl surfaceControl, IBinder iBinder) {
            surfaceControl.checkNotReleased();
            SurfaceControl.nativeReparent(this.mNativeObject, surfaceControl.mNativeObject, iBinder);
            return this;
        }

        public Transaction detachChildren(SurfaceControl surfaceControl) {
            surfaceControl.checkNotReleased();
            SurfaceControl.nativeSeverChildren(this.mNativeObject, surfaceControl.mNativeObject);
            return this;
        }

        public Transaction setOverrideScalingMode(SurfaceControl surfaceControl, int i) {
            surfaceControl.checkNotReleased();
            SurfaceControl.nativeSetOverrideScalingMode(this.mNativeObject, surfaceControl.mNativeObject, i);
            return this;
        }

        public Transaction setColor(SurfaceControl surfaceControl, float[] fArr) {
            surfaceControl.checkNotReleased();
            SurfaceControl.nativeSetColor(this.mNativeObject, surfaceControl.mNativeObject, fArr);
            return this;
        }

        public Transaction setGeometryAppliesWithResize(SurfaceControl surfaceControl) {
            surfaceControl.checkNotReleased();
            SurfaceControl.nativeSetGeometryAppliesWithResize(this.mNativeObject, surfaceControl.mNativeObject);
            return this;
        }

        public Transaction setSecure(SurfaceControl surfaceControl, boolean z) {
            surfaceControl.checkNotReleased();
            if (z) {
                SurfaceControl.nativeSetFlags(this.mNativeObject, surfaceControl.mNativeObject, 128, 128);
            } else {
                SurfaceControl.nativeSetFlags(this.mNativeObject, surfaceControl.mNativeObject, 0, 128);
            }
            return this;
        }

        public Transaction setOpaque(SurfaceControl surfaceControl, boolean z) {
            surfaceControl.checkNotReleased();
            if (z) {
                SurfaceControl.nativeSetFlags(this.mNativeObject, surfaceControl.mNativeObject, 2, 2);
            } else {
                SurfaceControl.nativeSetFlags(this.mNativeObject, surfaceControl.mNativeObject, 0, 2);
            }
            return this;
        }

        public Transaction destroy(SurfaceControl surfaceControl) {
            surfaceControl.checkNotReleased();
            surfaceControl.mCloseGuard.close();
            SurfaceControl.nativeDestroy(this.mNativeObject, surfaceControl.mNativeObject);
            return this;
        }

        public Transaction setDisplaySurface(IBinder iBinder, Surface surface) {
            if (iBinder == null) {
                throw new IllegalArgumentException("displayToken must not be null");
            }
            if (surface == null) {
                SurfaceControl.nativeSetDisplaySurface(this.mNativeObject, iBinder, 0L);
            } else {
                synchronized (surface.mLock) {
                    SurfaceControl.nativeSetDisplaySurface(this.mNativeObject, iBinder, surface.mNativeObject);
                }
            }
            return this;
        }

        public Transaction setDisplayLayerStack(IBinder iBinder, int i) {
            if (iBinder != null) {
                SurfaceControl.nativeSetDisplayLayerStack(this.mNativeObject, iBinder, i);
                return this;
            }
            throw new IllegalArgumentException("displayToken must not be null");
        }

        public Transaction setDisplayProjection(IBinder iBinder, int i, Rect rect, Rect rect2) {
            if (iBinder == null) {
                throw new IllegalArgumentException("displayToken must not be null");
            }
            if (rect == null) {
                throw new IllegalArgumentException("layerStackRect must not be null");
            }
            if (rect2 != null) {
                SurfaceControl.nativeSetDisplayProjection(this.mNativeObject, iBinder, i, rect.left, rect.top, rect.right, rect.bottom, rect2.left, rect2.top, rect2.right, rect2.bottom);
                return this;
            }
            throw new IllegalArgumentException("displayRect must not be null");
        }

        public Transaction setDisplaySize(IBinder iBinder, int i, int i2) {
            if (iBinder == null) {
                throw new IllegalArgumentException("displayToken must not be null");
            }
            if (i > 0 && i2 > 0) {
                SurfaceControl.nativeSetDisplaySize(this.mNativeObject, iBinder, i, i2);
                return this;
            }
            throw new IllegalArgumentException("width and height must be positive");
        }

        public Transaction setAnimationTransaction() {
            SurfaceControl.nativeSetAnimationTransaction(this.mNativeObject);
            return this;
        }

        public Transaction setEarlyWakeup() {
            SurfaceControl.nativeSetEarlyWakeup(this.mNativeObject);
            return this;
        }

        public Transaction merge(Transaction transaction) {
            this.mResizedSurfaces.putAll((ArrayMap<? extends SurfaceControl, ? extends Point>) transaction.mResizedSurfaces);
            transaction.mResizedSurfaces.clear();
            SurfaceControl.nativeMergeTransaction(this.mNativeObject, transaction.mNativeObject);
            return this;
        }
    }
}
