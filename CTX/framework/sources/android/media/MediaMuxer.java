package android.media;

import android.media.MediaCodec;
import dalvik.system.CloseGuard;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.nio.ByteBuffer;
import java.util.Map;

public final class MediaMuxer {
    private static final int MUXER_STATE_INITIALIZED = 0;
    private static final int MUXER_STATE_STARTED = 1;
    private static final int MUXER_STATE_STOPPED = 2;
    private static final int MUXER_STATE_UNINITIALIZED = -1;
    private long mNativeObject;
    private int mState = -1;
    private final CloseGuard mCloseGuard = CloseGuard.get();
    private int mLastTrackIndex = -1;

    @Retention(RetentionPolicy.SOURCE)
    public @interface Format {
    }

    private static native int nativeAddTrack(long j, String[] strArr, Object[] objArr);

    private static native void nativeRelease(long j);

    private static native void nativeSetLocation(long j, int i, int i2);

    private static native void nativeSetOrientationHint(long j, int i);

    private static native long nativeSetup(FileDescriptor fileDescriptor, int i) throws IOException, IllegalArgumentException;

    private static native void nativeStart(long j);

    private static native void nativeStop(long j);

    private static native void nativeWriteSampleData(long j, int i, ByteBuffer byteBuffer, int i2, int i3, long j2, int i4);

    static {
        System.loadLibrary("media_jni");
    }

    public static final class OutputFormat {
        public static final int MUXER_OUTPUT_3GPP = 2;
        public static final int MUXER_OUTPUT_FIRST = 0;
        public static final int MUXER_OUTPUT_HEIF = 3;
        public static final int MUXER_OUTPUT_LAST = 3;
        public static final int MUXER_OUTPUT_MPEG_4 = 0;
        public static final int MUXER_OUTPUT_WEBM = 1;

        private OutputFormat() {
        }
    }

    public MediaMuxer(String str, int i) throws Throwable {
        if (str == null) {
            throw new IllegalArgumentException("path must not be null");
        }
        RandomAccessFile randomAccessFile = null;
        try {
            RandomAccessFile randomAccessFile2 = new RandomAccessFile(str, "rws");
            try {
                randomAccessFile2.setLength(0L);
                setUpMediaMuxer(randomAccessFile2.getFD(), i);
                randomAccessFile2.close();
            } catch (Throwable th) {
                th = th;
                randomAccessFile = randomAccessFile2;
                if (randomAccessFile != null) {
                    randomAccessFile.close();
                }
                throw th;
            }
        } catch (Throwable th2) {
            th = th2;
        }
    }

    public MediaMuxer(FileDescriptor fileDescriptor, int i) throws IOException {
        setUpMediaMuxer(fileDescriptor, i);
    }

    private void setUpMediaMuxer(FileDescriptor fileDescriptor, int i) throws IOException {
        if (i < 0 || i > 3) {
            throw new IllegalArgumentException("format: " + i + " is invalid");
        }
        this.mNativeObject = nativeSetup(fileDescriptor, i);
        this.mState = 0;
        this.mCloseGuard.open("release");
    }

    public void setOrientationHint(int i) {
        if (i != 0 && i != 90 && i != 180 && i != 270) {
            throw new IllegalArgumentException("Unsupported angle: " + i);
        }
        if (this.mState == 0) {
            nativeSetOrientationHint(this.mNativeObject, i);
            return;
        }
        throw new IllegalStateException("Can't set rotation degrees due to wrong state.");
    }

    public void setLocation(float f, float f2) {
        int i = (int) (((double) (f * 10000.0f)) + 0.5d);
        int i2 = (int) (((double) (10000.0f * f2)) + 0.5d);
        if (i > 900000 || i < -900000) {
            throw new IllegalArgumentException("Latitude: " + f + " out of range.");
        }
        if (i2 > 1800000 || i2 < -1800000) {
            throw new IllegalArgumentException("Longitude: " + f2 + " out of range");
        }
        if (this.mState == 0 && this.mNativeObject != 0) {
            nativeSetLocation(this.mNativeObject, i, i2);
            return;
        }
        throw new IllegalStateException("Can't set location due to wrong state.");
    }

    public void start() {
        if (this.mNativeObject == 0) {
            throw new IllegalStateException("Muxer has been released!");
        }
        if (this.mState == 0) {
            nativeStart(this.mNativeObject);
            this.mState = 1;
            return;
        }
        throw new IllegalStateException("Can't start due to wrong state.");
    }

    public void stop() {
        if (this.mState == 1) {
            nativeStop(this.mNativeObject);
            this.mState = 2;
            return;
        }
        throw new IllegalStateException("Can't stop due to wrong state.");
    }

    protected void finalize() throws Throwable {
        try {
            if (this.mCloseGuard != null) {
                this.mCloseGuard.warnIfOpen();
            }
            if (this.mNativeObject != 0) {
                nativeRelease(this.mNativeObject);
                this.mNativeObject = 0L;
            }
        } finally {
            super.finalize();
        }
    }

    public int addTrack(MediaFormat mediaFormat) {
        if (mediaFormat == null) {
            throw new IllegalArgumentException("format must not be null.");
        }
        if (this.mState != 0) {
            throw new IllegalStateException("Muxer is not initialized.");
        }
        if (this.mNativeObject == 0) {
            throw new IllegalStateException("Muxer has been released!");
        }
        Map<String, Object> map = mediaFormat.getMap();
        int size = map.size();
        if (size > 0) {
            String[] strArr = new String[size];
            Object[] objArr = new Object[size];
            int i = 0;
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                strArr[i] = entry.getKey();
                objArr[i] = entry.getValue();
                i++;
            }
            int iNativeAddTrack = nativeAddTrack(this.mNativeObject, strArr, objArr);
            if (this.mLastTrackIndex >= iNativeAddTrack) {
                throw new IllegalArgumentException("Invalid format.");
            }
            this.mLastTrackIndex = iNativeAddTrack;
            return iNativeAddTrack;
        }
        throw new IllegalArgumentException("format must not be empty.");
    }

    public void writeSampleData(int i, ByteBuffer byteBuffer, MediaCodec.BufferInfo bufferInfo) {
        if (i < 0 || i > this.mLastTrackIndex) {
            throw new IllegalArgumentException("trackIndex is invalid");
        }
        if (byteBuffer == null) {
            throw new IllegalArgumentException("byteBuffer must not be null");
        }
        if (bufferInfo == null) {
            throw new IllegalArgumentException("bufferInfo must not be null");
        }
        if (bufferInfo.size < 0 || bufferInfo.offset < 0 || bufferInfo.offset + bufferInfo.size > byteBuffer.capacity() || bufferInfo.presentationTimeUs < 0) {
            throw new IllegalArgumentException("bufferInfo must specify a valid buffer offset, size and presentation time");
        }
        if (this.mNativeObject == 0) {
            throw new IllegalStateException("Muxer has been released!");
        }
        if (this.mState != 1) {
            throw new IllegalStateException("Can't write, muxer is not started");
        }
        nativeWriteSampleData(this.mNativeObject, i, byteBuffer, bufferInfo.offset, bufferInfo.size, bufferInfo.presentationTimeUs, bufferInfo.flags);
    }

    public void release() {
        if (this.mState == 1) {
            stop();
        }
        if (this.mNativeObject != 0) {
            nativeRelease(this.mNativeObject);
            this.mNativeObject = 0L;
            this.mCloseGuard.close();
        }
        this.mState = -1;
    }
}
