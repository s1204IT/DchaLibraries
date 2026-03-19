package android.hardware;

import android.os.Parcel;
import android.os.Parcelable;
import dalvik.annotation.optimization.FastNative;
import dalvik.system.CloseGuard;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import libcore.util.NativeAllocationRegistry;

public final class HardwareBuffer implements Parcelable, AutoCloseable {
    public static final int BLOB = 33;
    public static final Parcelable.Creator<HardwareBuffer> CREATOR = new Parcelable.Creator<HardwareBuffer>() {
        @Override
        public HardwareBuffer createFromParcel(Parcel parcel) {
            long jNReadHardwareBufferFromParcel = HardwareBuffer.nReadHardwareBufferFromParcel(parcel);
            if (jNReadHardwareBufferFromParcel != 0) {
                return new HardwareBuffer(jNReadHardwareBufferFromParcel);
            }
            return null;
        }

        @Override
        public HardwareBuffer[] newArray(int i) {
            return new HardwareBuffer[i];
        }
    };
    public static final int DS_24UI8 = 50;
    public static final int DS_FP32UI8 = 52;
    public static final int D_16 = 48;
    public static final int D_24 = 49;
    public static final int D_FP32 = 51;
    private static final long NATIVE_HARDWARE_BUFFER_SIZE = 232;
    public static final int RGBA_1010102 = 43;
    public static final int RGBA_8888 = 1;
    public static final int RGBA_FP16 = 22;
    public static final int RGBX_8888 = 2;
    public static final int RGB_565 = 4;
    public static final int RGB_888 = 3;
    public static final int S_UI8 = 53;
    public static final long USAGE_CPU_READ_OFTEN = 3;
    public static final long USAGE_CPU_READ_RARELY = 2;
    public static final long USAGE_CPU_WRITE_OFTEN = 48;
    public static final long USAGE_CPU_WRITE_RARELY = 32;
    public static final long USAGE_GPU_COLOR_OUTPUT = 512;
    public static final long USAGE_GPU_CUBE_MAP = 33554432;
    public static final long USAGE_GPU_DATA_BUFFER = 16777216;
    public static final long USAGE_GPU_MIPMAP_COMPLETE = 67108864;
    public static final long USAGE_GPU_SAMPLED_IMAGE = 256;
    public static final long USAGE_PROTECTED_CONTENT = 16384;
    public static final long USAGE_SENSOR_DIRECT_DATA = 8388608;
    public static final long USAGE_VIDEO_ENCODE = 65536;
    private Runnable mCleaner;
    private final CloseGuard mCloseGuard;
    private long mNativeObject;

    @Retention(RetentionPolicy.SOURCE)
    public @interface Format {
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface Usage {
    }

    private static native long nCreateHardwareBuffer(int i, int i2, int i3, int i4, long j);

    @FastNative
    private static native int nGetFormat(long j);

    @FastNative
    private static native int nGetHeight(long j);

    @FastNative
    private static native int nGetLayers(long j);

    private static native long nGetNativeFinalizer();

    @FastNative
    private static native long nGetUsage(long j);

    @FastNative
    private static native int nGetWidth(long j);

    private static native long nReadHardwareBufferFromParcel(Parcel parcel);

    private static native void nWriteHardwareBufferToParcel(long j, Parcel parcel);

    public static HardwareBuffer create(int i, int i2, int i3, int i4, long j) {
        if (!isSupportedFormat(i3)) {
            throw new IllegalArgumentException("Invalid pixel format " + i3);
        }
        if (i <= 0) {
            throw new IllegalArgumentException("Invalid width " + i);
        }
        if (i2 <= 0) {
            throw new IllegalArgumentException("Invalid height " + i2);
        }
        if (i4 <= 0) {
            throw new IllegalArgumentException("Invalid layer count " + i4);
        }
        if (i3 == 33 && i2 != 1) {
            throw new IllegalArgumentException("Height must be 1 when using the BLOB format");
        }
        long jNCreateHardwareBuffer = nCreateHardwareBuffer(i, i2, i3, i4, j);
        if (jNCreateHardwareBuffer == 0) {
            throw new IllegalArgumentException("Unable to create a HardwareBuffer, either the dimensions passed were too large, too many image layers were requested, or an invalid set of usage flags or invalid format was passed");
        }
        return new HardwareBuffer(jNCreateHardwareBuffer);
    }

    private HardwareBuffer(long j) {
        this.mCloseGuard = CloseGuard.get();
        this.mNativeObject = j;
        this.mCleaner = new NativeAllocationRegistry(HardwareBuffer.class.getClassLoader(), nGetNativeFinalizer(), NATIVE_HARDWARE_BUFFER_SIZE).registerNativeAllocation(this, this.mNativeObject);
        this.mCloseGuard.open("close");
    }

    protected void finalize() throws Throwable {
        try {
            this.mCloseGuard.warnIfOpen();
            close();
        } finally {
            super.finalize();
        }
    }

    public int getWidth() {
        if (isClosed()) {
            throw new IllegalStateException("This HardwareBuffer has been closed and its width cannot be obtained.");
        }
        return nGetWidth(this.mNativeObject);
    }

    public int getHeight() {
        if (isClosed()) {
            throw new IllegalStateException("This HardwareBuffer has been closed and its height cannot be obtained.");
        }
        return nGetHeight(this.mNativeObject);
    }

    public int getFormat() {
        if (isClosed()) {
            throw new IllegalStateException("This HardwareBuffer has been closed and its format cannot be obtained.");
        }
        return nGetFormat(this.mNativeObject);
    }

    public int getLayers() {
        if (isClosed()) {
            throw new IllegalStateException("This HardwareBuffer has been closed and its layer count cannot be obtained.");
        }
        return nGetLayers(this.mNativeObject);
    }

    public long getUsage() {
        if (isClosed()) {
            throw new IllegalStateException("This HardwareBuffer has been closed and its usage cannot be obtained.");
        }
        return nGetUsage(this.mNativeObject);
    }

    @Deprecated
    public void destroy() {
        close();
    }

    @Deprecated
    public boolean isDestroyed() {
        return isClosed();
    }

    @Override
    public void close() {
        if (!isClosed()) {
            this.mCloseGuard.close();
            this.mNativeObject = 0L;
            this.mCleaner.run();
            this.mCleaner = null;
        }
    }

    public boolean isClosed() {
        return this.mNativeObject == 0;
    }

    @Override
    public int describeContents() {
        return 1;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        if (isClosed()) {
            throw new IllegalStateException("This HardwareBuffer has been closed and cannot be written to a parcel.");
        }
        nWriteHardwareBufferToParcel(this.mNativeObject, parcel);
    }

    private static boolean isSupportedFormat(int i) {
        if (i == 22 || i == 33 || i == 43) {
            return true;
        }
        switch (i) {
            case 1:
            case 2:
            case 3:
            case 4:
                return true;
            default:
                switch (i) {
                    case 48:
                    case 49:
                    case 50:
                    case 51:
                    case 52:
                    case 53:
                        return true;
                    default:
                        return false;
                }
        }
    }
}
