package com.mediatek.camera.common.mode.photo.heif;

import android.annotation.SuppressLint;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;
import com.mediatek.camera.common.mode.photo.heif.HeifEncoder;
import java.io.FileDescriptor;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.TimeoutException;

public final class HeifWriter implements AutoCloseable {
    private final Handler mHandler;
    private final HandlerThread mHandlerThread;
    private HeifEncoder mHeifEncoder;
    private final int mInputMode;
    private final int mMaxImages;
    private MediaMuxer mMuxer;
    private int mNumTiles;
    private int mOutputIndex;
    private final int mPrimaryIndex;
    private final ResultWaiter mResultWaiter;
    private final int mRotation;
    private boolean mStarted;
    private int[] mTrackIndexArray;

    static int access$1008(HeifWriter heifWriter) {
        int i = heifWriter.mOutputIndex;
        heifWriter.mOutputIndex = i + 1;
        return i;
    }

    public static final class Builder {
        private final FileDescriptor mFd;
        private boolean mGridEnabled;
        private Handler mHandler;
        private final int mHeight;
        private final int mInputMode;
        private int mMaxImages;
        private final String mPath;
        private int mPrimaryIndex;
        private int mQuality;
        private int mRotation;
        private final int mWidth;

        public Builder(String str, int i, int i2, int i3) {
            this(str, null, i, i2, i3);
        }

        private Builder(String str, FileDescriptor fileDescriptor, int i, int i2, int i3) {
            this.mGridEnabled = true;
            this.mQuality = 100;
            this.mMaxImages = 1;
            this.mPrimaryIndex = 0;
            this.mRotation = 0;
            if (i <= 0 || i2 <= 0) {
                throw new IllegalArgumentException("Invalid image size: " + i + "x" + i2);
            }
            this.mPath = str;
            this.mFd = fileDescriptor;
            this.mWidth = i;
            this.mHeight = i2;
            this.mInputMode = i3;
        }

        public Builder setRotation(int i) {
            if (i != 0 && i != 90 && i != 180 && i != 270) {
                throw new IllegalArgumentException("Invalid rotation angle: " + i);
            }
            this.mRotation = i;
            return this;
        }

        public Builder setGridEnabled(boolean z) {
            this.mGridEnabled = z;
            return this;
        }

        public HeifWriter build() throws IOException {
            return new HeifWriter(this.mPath, this.mFd, this.mWidth, this.mHeight, this.mRotation, this.mGridEnabled, this.mQuality, this.mMaxImages, this.mPrimaryIndex, this.mInputMode, this.mHandler);
        }
    }

    @SuppressLint({"WrongConstant"})
    private HeifWriter(String str, FileDescriptor fileDescriptor, int i, int i2, int i3, boolean z, int i4, int i5, int i6, int i7, Handler handler) throws IOException {
        MediaMuxer mediaMuxer;
        this.mResultWaiter = new ResultWaiter();
        if (i6 >= i5) {
            throw new IllegalArgumentException("Invalid maxImages (" + i5 + ") or primaryIndex (" + i6 + ")");
        }
        Log.d("HeifWriter", "width: " + i + ", height: " + i2 + ", rotation: " + i3 + ", gridEnabled: " + z + ", quality: " + i4 + ", maxImages: " + i5 + ", primaryIndex: " + i6 + ", inputMode: " + i7);
        MediaFormat.createVideoFormat("image/vnd.android.heic", i, i2);
        this.mNumTiles = 1;
        this.mRotation = i3;
        this.mInputMode = i7;
        this.mMaxImages = i5;
        this.mPrimaryIndex = i6;
        Looper looper = handler != null ? handler.getLooper() : null;
        if (looper == null) {
            this.mHandlerThread = new HandlerThread("HeifEncoderThread", -2);
            this.mHandlerThread.start();
            looper = this.mHandlerThread.getLooper();
        } else {
            this.mHandlerThread = null;
        }
        this.mHandler = new Handler(looper);
        if (str != null) {
            mediaMuxer = new MediaMuxer(str, 3);
        } else {
            mediaMuxer = new MediaMuxer(fileDescriptor, 3);
        }
        this.mMuxer = mediaMuxer;
        this.mHeifEncoder = new HeifEncoder(i, i2, z, i4, this.mInputMode, this.mHandler, new HeifCallback());
    }

    public void start() {
        checkStarted(false);
        this.mStarted = true;
        this.mHeifEncoder.start();
    }

    public void addYuvBuffer(int i, byte[] bArr) {
        checkStartedAndMode(0);
        synchronized (this) {
            if (this.mHeifEncoder != null) {
                this.mHeifEncoder.addYuvBuffer(i, bArr);
            }
        }
    }

    public void stop(long j) throws Exception {
        checkStarted(true);
        synchronized (this) {
            if (this.mHeifEncoder != null) {
                this.mHeifEncoder.stopAsync();
            }
        }
        this.mResultWaiter.waitForResult(j);
    }

    private void checkStarted(boolean z) {
        if (this.mStarted != z) {
            throw new IllegalStateException("Already started");
        }
    }

    private void checkMode(int i) {
        if (this.mInputMode != i) {
            throw new IllegalStateException("Not valid in input mode " + this.mInputMode);
        }
    }

    private void checkStartedAndMode(int i) {
        checkStarted(true);
        checkMode(i);
    }

    private void closeInternal() {
        Log.d("HeifWriter", "closeInternal");
        if (this.mMuxer != null) {
            this.mMuxer.stop();
            this.mMuxer.release();
            this.mMuxer = null;
        }
        if (this.mHeifEncoder != null) {
            this.mHeifEncoder.close();
            synchronized (this) {
                this.mHeifEncoder = null;
            }
        }
    }

    private class HeifCallback extends HeifEncoder.Callback {
        private HeifCallback() {
        }

        @Override
        public void onOutputFormatChanged(HeifEncoder heifEncoder, MediaFormat mediaFormat) {
            int i;
            if (heifEncoder != HeifWriter.this.mHeifEncoder) {
                return;
            }
            Log.d("HeifWriter", "onOutputFormatChanged: " + mediaFormat);
            if (HeifWriter.this.mTrackIndexArray != null) {
                stopAndNotify(new IllegalStateException("Output format changed after muxer started"));
                return;
            }
            try {
                HeifWriter.this.mNumTiles = mediaFormat.getInteger("grid-rows") * mediaFormat.getInteger("grid-cols");
            } catch (ClassCastException | NullPointerException e) {
                HeifWriter.this.mNumTiles = 1;
            }
            HeifWriter.this.mTrackIndexArray = new int[HeifWriter.this.mMaxImages];
            if (HeifWriter.this.mRotation > 0) {
                Log.d("HeifWriter", "setting rotation: " + HeifWriter.this.mRotation);
                HeifWriter.this.mMuxer.setOrientationHint(HeifWriter.this.mRotation);
            }
            for (int i2 = 0; i2 < HeifWriter.this.mTrackIndexArray.length; i2++) {
                if (i2 == HeifWriter.this.mPrimaryIndex) {
                    i = 1;
                } else {
                    i = 0;
                }
                mediaFormat.setInteger("is-default", i);
                HeifWriter.this.mTrackIndexArray[i2] = HeifWriter.this.mMuxer.addTrack(mediaFormat);
            }
            HeifWriter.this.mMuxer.start();
        }

        @Override
        public void onDrainOutputBuffer(HeifEncoder heifEncoder, ByteBuffer byteBuffer) {
            if (heifEncoder != HeifWriter.this.mHeifEncoder) {
                return;
            }
            Log.d("HeifWriter", "onDrainOutputBuffer: " + HeifWriter.this.mOutputIndex);
            if (HeifWriter.this.mTrackIndexArray != null) {
                if (HeifWriter.this.mOutputIndex < HeifWriter.this.mMaxImages * HeifWriter.this.mNumTiles) {
                    MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
                    bufferInfo.set(byteBuffer.position(), byteBuffer.remaining(), 0L, 0);
                    HeifWriter.this.mMuxer.writeSampleData(HeifWriter.this.mTrackIndexArray[HeifWriter.this.mOutputIndex / HeifWriter.this.mNumTiles], byteBuffer, bufferInfo);
                }
                HeifWriter.access$1008(HeifWriter.this);
                if (HeifWriter.this.mOutputIndex == HeifWriter.this.mMaxImages * HeifWriter.this.mNumTiles) {
                    stopAndNotify(null);
                    return;
                }
                return;
            }
            stopAndNotify(new IllegalStateException("Output buffer received before format info"));
        }

        @Override
        public void onComplete(HeifEncoder heifEncoder) {
            if (heifEncoder != HeifWriter.this.mHeifEncoder) {
                return;
            }
            stopAndNotify(null);
        }

        @Override
        public void onError(HeifEncoder heifEncoder, MediaCodec.CodecException codecException) {
            if (heifEncoder != HeifWriter.this.mHeifEncoder) {
                return;
            }
            stopAndNotify(codecException);
        }

        private void stopAndNotify(Exception exc) {
            try {
                HeifWriter.this.closeInternal();
            } catch (Exception e) {
                if (exc == null) {
                    exc = e;
                }
            }
            HeifWriter.this.mResultWaiter.signalResult(exc);
        }
    }

    private static class ResultWaiter {
        private boolean mDone;
        private Exception mException;

        private ResultWaiter() {
        }

        synchronized void waitForResult(long j) throws Exception {
            if (j < 0) {
                throw new IllegalArgumentException("timeoutMs is negative");
            }
            if (j == 0) {
                while (!this.mDone) {
                    try {
                        wait();
                    } catch (InterruptedException e) {
                    }
                }
            } else {
                long jCurrentTimeMillis = System.currentTimeMillis();
                while (!this.mDone && j > 0) {
                    try {
                        wait(j);
                    } catch (InterruptedException e2) {
                    }
                    j -= System.currentTimeMillis() - jCurrentTimeMillis;
                }
            }
            if (!this.mDone) {
                this.mDone = true;
                this.mException = new TimeoutException("timed out waiting for result");
            }
            if (this.mException != null) {
                throw this.mException;
            }
        }

        synchronized void signalResult(Exception exc) {
            if (!this.mDone) {
                this.mDone = true;
                this.mException = exc;
                notifyAll();
            }
        }
    }

    @Override
    public void close() {
        this.mHandler.postAtFrontOfQueue(new Runnable() {
            @Override
            public void run() {
                try {
                    HeifWriter.this.closeInternal();
                } catch (Exception e) {
                }
            }
        });
    }
}
