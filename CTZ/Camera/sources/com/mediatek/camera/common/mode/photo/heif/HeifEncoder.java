package com.mediatek.camera.common.mode.photo.heif;

import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.media.Image;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCrypto;
import android.media.MediaFormat;
import android.opengl.GLES20;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;
import android.util.Range;
import android.view.Surface;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;

public final class HeifEncoder implements SurfaceTexture.OnFrameAvailableListener, AutoCloseable {
    private final Callback mCallback;
    private ByteBuffer mCurrentBuffer;
    private final Rect mDstRect;
    private SurfaceEOSTracker mEOSTracker;
    private MediaCodec mEncoder;
    private EglWindowSurface mEncoderEglSurface;
    private Surface mEncoderSurface;
    private final int mGridCols;
    private final int mGridHeight;
    private final int mGridRows;
    private final int mGridWidth;
    private final Handler mHandler;
    private final HandlerThread mHandlerThread;
    private final int mHeight;
    private boolean mInputEOS;
    private int mInputIndex;
    private final int mInputMode;
    private Surface mInputSurface;
    private SurfaceTexture mInputTexture;
    private final int mNumTiles;
    private EglRectBlt mRectBlt;
    private final Rect mSrcRect;
    private int mTextureId;
    private final int mWidth;
    private final ArrayList<ByteBuffer> mEmptyBuffers = new ArrayList<>();
    private final ArrayList<ByteBuffer> mFilledBuffers = new ArrayList<>();
    private final ArrayList<Integer> mCodecInputBuffers = new ArrayList<>();
    private final float[] mTmpMatrix = new float[16];

    public static abstract class Callback {
        public abstract void onComplete(HeifEncoder heifEncoder);

        public abstract void onDrainOutputBuffer(HeifEncoder heifEncoder, ByteBuffer byteBuffer);

        public abstract void onError(HeifEncoder heifEncoder, MediaCodec.CodecException codecException);

        public abstract void onOutputFormatChanged(HeifEncoder heifEncoder, MediaFormat mediaFormat);
    }

    public HeifEncoder(int i, int i2, boolean z, int i3, int i4, Handler handler, Callback callback) throws IOException {
        MediaCodecInfo.CodecCapabilities codecCapabilities;
        boolean z2;
        int i5;
        int i6;
        int i7;
        MediaFormat mediaFormatCreateVideoFormat;
        Log.d("HeifEncoder", "width: " + i + ", height: " + i2 + ", useGrid: " + z + ", quality: " + i3 + ", inputMode: " + i4);
        if (i < 0 || i2 < 0 || i3 < 0 || i3 > 100) {
            throw new IllegalArgumentException("invalid encoder inputs");
        }
        int i8 = 512;
        boolean z3 = (i > 512 || i2 > 512) & z;
        try {
            this.mEncoder = MediaCodec.createEncoderByType("image/vnd.android.heic");
            MediaCodecInfo.CodecCapabilities capabilitiesForType = this.mEncoder.getCodecInfo().getCapabilitiesForType("image/vnd.android.heic");
            if (!capabilitiesForType.getVideoCapabilities().isSizeSupported(i, i2)) {
                this.mEncoder.release();
                this.mEncoder = null;
                throw new Exception();
            }
            codecCapabilities = capabilitiesForType;
            z2 = true;
        } catch (Exception e) {
            this.mEncoder = MediaCodec.createEncoderByType("video/hevc");
            MediaCodecInfo.CodecCapabilities capabilitiesForType2 = this.mEncoder.getCodecInfo().getCapabilitiesForType("video/hevc");
            z3 |= !capabilitiesForType2.getVideoCapabilities().isSizeSupported(i, i2);
            codecCapabilities = capabilitiesForType2;
            z2 = false;
        }
        this.mInputMode = i4;
        this.mCallback = callback;
        Looper looper = handler != null ? handler.getLooper() : null;
        if (looper == null) {
            this.mHandlerThread = new HandlerThread("HeifEncoderThread", -2);
            this.mHandlerThread.start();
            looper = this.mHandlerThread.getLooper();
        } else {
            this.mHandlerThread = null;
        }
        this.mHandler = new Handler(looper);
        boolean z4 = i4 == 1 || i4 == 2;
        int i9 = z4 ? 2130708361 : 2135033992;
        this.mWidth = i;
        this.mHeight = i2;
        if (z3) {
            i6 = ((i2 + 512) - 1) / 512;
            i7 = ((i + 512) - 1) / 512;
            i5 = 512;
        } else {
            i8 = this.mWidth;
            i5 = this.mHeight;
            i6 = 1;
            i7 = 1;
        }
        if (z2) {
            mediaFormatCreateVideoFormat = MediaFormat.createVideoFormat("image/vnd.android.heic", this.mWidth, this.mHeight);
        } else {
            mediaFormatCreateVideoFormat = MediaFormat.createVideoFormat("video/hevc", i8, i5);
        }
        if (z3) {
            mediaFormatCreateVideoFormat.setInteger("tile-width", i8);
            mediaFormatCreateVideoFormat.setInteger("tile-height", i5);
            mediaFormatCreateVideoFormat.setInteger("grid-cols", i7);
            mediaFormatCreateVideoFormat.setInteger("grid-rows", i6);
        }
        if (z2) {
            this.mGridWidth = i;
            this.mGridHeight = i2;
            this.mGridRows = 1;
            this.mGridCols = 1;
        } else {
            this.mGridWidth = i8;
            this.mGridHeight = i5;
            this.mGridRows = i6;
            this.mGridCols = i7;
        }
        this.mNumTiles = this.mGridRows * this.mGridCols;
        mediaFormatCreateVideoFormat.setInteger("i-frame-interval", 0);
        mediaFormatCreateVideoFormat.setInteger("color-format", i9);
        mediaFormatCreateVideoFormat.setInteger("frame-rate", this.mNumTiles);
        mediaFormatCreateVideoFormat.setInteger("capture-rate", this.mNumTiles * 30);
        MediaCodecInfo.EncoderCapabilities encoderCapabilities = codecCapabilities.getEncoderCapabilities();
        if (encoderCapabilities.isBitrateModeSupported(0)) {
            Log.d("HeifEncoder", "Setting bitrate mode to constant quality");
            Range<Integer> qualityRange = encoderCapabilities.getQualityRange();
            Log.d("HeifEncoder", "Quality range: " + qualityRange);
            mediaFormatCreateVideoFormat.setInteger("bitrate-mode", 0);
            mediaFormatCreateVideoFormat.setInteger("quality", (int) (((double) ((Integer) qualityRange.getLower()).intValue()) + (((double) ((((Integer) qualityRange.getUpper()).intValue() - ((Integer) qualityRange.getLower()).intValue()) * i3)) / 100.0d)));
        } else {
            if (encoderCapabilities.isBitrateModeSupported(2)) {
                Log.d("HeifEncoder", "Setting bitrate mode to constant bitrate");
                mediaFormatCreateVideoFormat.setInteger("bitrate-mode", 2);
            } else {
                Log.d("HeifEncoder", "Setting bitrate mode to variable bitrate");
                mediaFormatCreateVideoFormat.setInteger("bitrate-mode", 1);
            }
            mediaFormatCreateVideoFormat.setInteger("bitrate", (int) (((((((double) (i * i2)) * 1.5d) * 8.0d) * 0.25d) * ((double) i3)) / 100.0d));
        }
        this.mEncoder.setCallback(new EncoderCallback(), this.mHandler);
        this.mEncoder.configure(mediaFormatCreateVideoFormat, (Surface) null, (MediaCrypto) null, 1);
        if (z4) {
            this.mEncoderSurface = this.mEncoder.createInputSurface();
            boolean z5 = this.mNumTiles > 1;
            this.mEOSTracker = new SurfaceEOSTracker(z5);
            if (i4 == 1) {
                if (z5) {
                    this.mEncoderEglSurface = new EglWindowSurface(this.mEncoderSurface);
                    this.mEncoderEglSurface.makeCurrent();
                    this.mRectBlt = new EglRectBlt(new Texture2dProgram(i4 == 2 ? 0 : 1), this.mWidth, this.mHeight);
                    this.mTextureId = this.mRectBlt.createTextureObject();
                    if (i4 == 1) {
                        this.mInputTexture = new SurfaceTexture(this.mTextureId, true);
                        this.mInputTexture.setOnFrameAvailableListener(this);
                        this.mInputTexture.setDefaultBufferSize(this.mWidth, this.mHeight);
                        this.mInputSurface = new Surface(this.mInputTexture);
                    }
                    this.mEncoderEglSurface.makeUnCurrent();
                } else {
                    this.mInputSurface = this.mEncoderSurface;
                }
            }
        } else {
            for (int i10 = 0; i10 < 2; i10++) {
                this.mEmptyBuffers.add(ByteBuffer.allocateDirect(((this.mWidth * this.mHeight) * 3) / 2));
            }
        }
        this.mDstRect = new Rect(0, 0, this.mGridWidth, this.mGridHeight);
        this.mSrcRect = new Rect();
    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        synchronized (this) {
            if (this.mEncoderEglSurface == null) {
                return;
            }
            this.mEncoderEglSurface.makeCurrent();
            surfaceTexture.updateTexImage();
            surfaceTexture.getTransformMatrix(this.mTmpMatrix);
            long timestamp = surfaceTexture.getTimestamp();
            Log.d("HeifEncoder", "onFrameAvailable: timestampUs " + (timestamp / 1000));
            if (this.mEOSTracker.updateLastInputAndEncoderTime(timestamp, computePresentationTime((this.mInputIndex + this.mNumTiles) - 1))) {
                GLES20.glViewport(0, 0, this.mGridWidth, this.mGridHeight);
                for (int i = 0; i < this.mGridRows; i++) {
                    for (int i2 = 0; i2 < this.mGridCols; i2++) {
                        int i3 = this.mGridWidth * i2;
                        int i4 = this.mGridHeight * i;
                        this.mSrcRect.set(i3, i4, this.mGridWidth + i3, this.mGridHeight + i4);
                        this.mRectBlt.copyRect(this.mTextureId, this.mTmpMatrix, this.mSrcRect);
                        EglWindowSurface eglWindowSurface = this.mEncoderEglSurface;
                        int i5 = this.mInputIndex;
                        this.mInputIndex = i5 + 1;
                        eglWindowSurface.setPresentationTime(computePresentationTime(i5) * 1000);
                        this.mEncoderEglSurface.swapBuffers();
                    }
                }
            }
            surfaceTexture.releaseTexImage();
            this.mEncoderEglSurface.makeUnCurrent();
        }
    }

    public void start() {
        this.mEncoder.start();
    }

    public void addYuvBuffer(int i, byte[] bArr) {
        if (this.mInputMode != 0) {
            throw new IllegalStateException("addYuvBuffer is only allowed in buffer input mode");
        }
        if (bArr == null || bArr.length != ((this.mWidth * this.mHeight) * 3) / 2) {
            throw new IllegalArgumentException("invalid data");
        }
        addYuvBufferInternal(bArr);
    }

    public void stopAsync() {
        if (this.mInputMode == 2) {
            this.mEOSTracker.updateInputEOSTime(0L);
        } else if (this.mInputMode == 0) {
            addYuvBufferInternal(null);
        }
    }

    private long computePresentationTime(int i) {
        return 132 + ((((long) i) * 1000000) / ((long) this.mNumTiles));
    }

    private void addYuvBufferInternal(byte[] bArr) {
        ByteBuffer byteBufferAcquireEmptyBuffer = acquireEmptyBuffer();
        if (byteBufferAcquireEmptyBuffer == null) {
            return;
        }
        byteBufferAcquireEmptyBuffer.clear();
        if (bArr != null) {
            byteBufferAcquireEmptyBuffer.put(bArr);
        }
        byteBufferAcquireEmptyBuffer.flip();
        synchronized (this.mFilledBuffers) {
            this.mFilledBuffers.add(byteBufferAcquireEmptyBuffer);
        }
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                HeifEncoder.this.maybeCopyOneTileYUV();
            }
        });
    }

    private void maybeCopyOneTileYUV() {
        while (true) {
            ByteBuffer currentBuffer = getCurrentBuffer();
            if (currentBuffer != null && !this.mCodecInputBuffers.isEmpty()) {
                int iIntValue = this.mCodecInputBuffers.remove(0).intValue();
                boolean z = this.mInputIndex % this.mNumTiles == 0 && currentBuffer.remaining() == 0;
                if (!z) {
                    Image inputImage = this.mEncoder.getInputImage(iIntValue);
                    int i = this.mGridWidth * (this.mInputIndex % this.mGridCols);
                    int i2 = this.mGridHeight * ((this.mInputIndex / this.mGridCols) % this.mGridRows);
                    this.mSrcRect.set(i, i2, this.mGridWidth + i, this.mGridHeight + i2);
                    copyOneTileYUV(currentBuffer, inputImage, this.mWidth, this.mHeight, this.mSrcRect, this.mDstRect);
                }
                MediaCodec mediaCodec = this.mEncoder;
                int iCapacity = z ? 0 : this.mEncoder.getInputBuffer(iIntValue).capacity();
                int i3 = this.mInputIndex;
                this.mInputIndex = i3 + 1;
                mediaCodec.queueInputBuffer(iIntValue, 0, iCapacity, computePresentationTime(i3), z ? 4 : 0);
                if (z || this.mInputIndex % this.mNumTiles == 0) {
                    returnEmptyBufferAndNotify(z);
                }
            } else {
                return;
            }
        }
    }

    private static void copyOneTileYUV(ByteBuffer byteBuffer, Image image, int i, int i2, Rect rect, Rect rect2) {
        int i3;
        int i4;
        if (rect.width() != rect2.width() || rect.height() != rect2.height()) {
            throw new IllegalArgumentException("src and dst rect size are different!");
        }
        if (i % 2 == 0 && i2 % 2 == 0) {
            int i5 = 2;
            if (rect.left % 2 == 0 && rect.top % 2 == 0 && rect.right % 2 == 0 && rect.bottom % 2 == 0 && rect2.left % 2 == 0 && rect2.top % 2 == 0 && rect2.right % 2 == 0 && rect2.bottom % 2 == 0) {
                Image.Plane[] planes = image.getPlanes();
                int i6 = 0;
                while (i6 < planes.length) {
                    ByteBuffer buffer = planes[i6].getBuffer();
                    int pixelStride = planes[i6].getPixelStride();
                    int iMin = Math.min(rect.width(), i - rect.left);
                    int iMin2 = Math.min(rect.height(), i2 - rect.top);
                    if (i6 > 0) {
                        i3 = ((i * i2) * (i6 + 3)) / 4;
                        i4 = i5;
                    } else {
                        i3 = 0;
                        i4 = 1;
                    }
                    for (int i7 = 0; i7 < iMin2 / i4; i7++) {
                        byteBuffer.position(((((rect.top / i4) + i7) * i) / i4) + i3 + (rect.left / i4));
                        buffer.position((((rect2.top / i4) + i7) * planes[i6].getRowStride()) + ((rect2.left * pixelStride) / i4));
                        int i8 = 0;
                        while (true) {
                            int i9 = iMin / i4;
                            if (i8 < i9) {
                                buffer.put(byteBuffer.get());
                                if (pixelStride > 1 && i8 != i9 - 1) {
                                    buffer.position((buffer.position() + pixelStride) - 1);
                                }
                                i8++;
                            }
                        }
                    }
                    i6++;
                    i5 = 2;
                }
                return;
            }
        }
        throw new IllegalArgumentException("src or dst are not aligned!");
    }

    private ByteBuffer acquireEmptyBuffer() {
        ByteBuffer byteBufferRemove;
        synchronized (this.mEmptyBuffers) {
            while (!this.mInputEOS && this.mEmptyBuffers.isEmpty()) {
                try {
                    this.mEmptyBuffers.wait();
                } catch (InterruptedException e) {
                }
            }
            byteBufferRemove = this.mInputEOS ? null : this.mEmptyBuffers.remove(0);
        }
        return byteBufferRemove;
    }

    private ByteBuffer getCurrentBuffer() {
        if (!this.mInputEOS && this.mCurrentBuffer == null) {
            synchronized (this.mFilledBuffers) {
                this.mCurrentBuffer = this.mFilledBuffers.isEmpty() ? null : this.mFilledBuffers.remove(0);
            }
        }
        if (this.mInputEOS) {
            return null;
        }
        return this.mCurrentBuffer;
    }

    private void returnEmptyBufferAndNotify(boolean z) {
        synchronized (this.mEmptyBuffers) {
            this.mInputEOS = z | this.mInputEOS;
            this.mEmptyBuffers.add(this.mCurrentBuffer);
            this.mEmptyBuffers.notifyAll();
        }
        this.mCurrentBuffer = null;
    }

    private void stopInternal() {
        Log.d("HeifEncoder", "stopInternal");
        if (this.mEncoder != null) {
            this.mEncoder.stop();
            this.mEncoder.release();
            this.mEncoder = null;
        }
        synchronized (this.mEmptyBuffers) {
            this.mInputEOS = true;
            this.mEmptyBuffers.notifyAll();
        }
        synchronized (this) {
            if (this.mRectBlt != null) {
                this.mRectBlt.release(false);
                this.mRectBlt = null;
            }
            if (this.mEncoderEglSurface != null) {
                this.mEncoderEglSurface.release();
                this.mEncoderEglSurface = null;
            }
            if (this.mInputTexture != null) {
                this.mInputTexture.release();
                this.mInputTexture = null;
            }
        }
    }

    private class SurfaceEOSTracker {
        final boolean mCopyTiles;
        boolean mSignaled;
        long mInputEOSTimeNs = -1;
        long mLastInputTimeNs = -1;
        long mEncoderEOSTimeUs = -1;
        long mLastEncoderTimeUs = -1;
        long mLastOutputTimeUs = -1;

        SurfaceEOSTracker(boolean z) {
            this.mCopyTiles = z;
        }

        synchronized void updateInputEOSTime(long j) {
            if (this.mCopyTiles) {
                if (this.mInputEOSTimeNs < 0) {
                    this.mInputEOSTimeNs = j;
                }
            } else if (this.mEncoderEOSTimeUs < 0) {
                this.mEncoderEOSTimeUs = j / 1000;
            }
            updateEOSLocked();
        }

        synchronized boolean updateLastInputAndEncoderTime(long j, long j2) {
            boolean z;
            z = this.mInputEOSTimeNs < 0 || j <= this.mInputEOSTimeNs;
            if (z) {
                this.mLastEncoderTimeUs = j2;
            }
            this.mLastInputTimeNs = j;
            updateEOSLocked();
            return z;
        }

        synchronized void updateLastOutputTime(long j) {
            this.mLastOutputTimeUs = j;
            updateEOSLocked();
        }

        private void updateEOSLocked() {
            if (this.mSignaled) {
                return;
            }
            if (this.mEncoderEOSTimeUs < 0 && this.mInputEOSTimeNs >= 0 && this.mLastInputTimeNs >= this.mInputEOSTimeNs) {
                if (this.mLastEncoderTimeUs < 0) {
                    doSignalEOSLocked();
                    return;
                }
                this.mEncoderEOSTimeUs = this.mLastEncoderTimeUs;
            }
            if (this.mEncoderEOSTimeUs >= 0 && this.mEncoderEOSTimeUs <= this.mLastOutputTimeUs) {
                doSignalEOSLocked();
            }
        }

        private void doSignalEOSLocked() {
            HeifEncoder.this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (HeifEncoder.this.mEncoder != null) {
                        HeifEncoder.this.mEncoder.signalEndOfInputStream();
                    }
                }
            });
            this.mSignaled = true;
        }
    }

    private class EncoderCallback extends MediaCodec.Callback {
        private boolean mOutputEOS;

        private EncoderCallback() {
        }

        @Override
        public void onOutputFormatChanged(MediaCodec mediaCodec, MediaFormat mediaFormat) {
            if (mediaCodec != HeifEncoder.this.mEncoder) {
                return;
            }
            Log.d("HeifEncoder", "onOutputFormatChanged: " + mediaFormat);
            if (!"image/vnd.android.heic".equals(mediaFormat.getString("mime"))) {
                mediaFormat.setString("mime", "image/vnd.android.heic");
                mediaFormat.setInteger("width", HeifEncoder.this.mWidth);
                mediaFormat.setInteger("height", HeifEncoder.this.mHeight);
                if (HeifEncoder.this.mNumTiles > 1) {
                    mediaFormat.setInteger("tile-width", HeifEncoder.this.mGridWidth);
                    mediaFormat.setInteger("tile-height", HeifEncoder.this.mGridHeight);
                    mediaFormat.setInteger("grid-rows", HeifEncoder.this.mGridRows);
                    mediaFormat.setInteger("grid-cols", HeifEncoder.this.mGridCols);
                }
            }
            HeifEncoder.this.mCallback.onOutputFormatChanged(HeifEncoder.this, mediaFormat);
        }

        @Override
        public void onInputBufferAvailable(MediaCodec mediaCodec, int i) {
            if (mediaCodec != HeifEncoder.this.mEncoder || HeifEncoder.this.mInputEOS) {
                return;
            }
            Log.d("HeifEncoder", "onInputBufferAvailable: " + i);
            HeifEncoder.this.mCodecInputBuffers.add(Integer.valueOf(i));
            HeifEncoder.this.maybeCopyOneTileYUV();
        }

        @Override
        public void onOutputBufferAvailable(MediaCodec mediaCodec, int i, MediaCodec.BufferInfo bufferInfo) {
            if (mediaCodec != HeifEncoder.this.mEncoder || this.mOutputEOS) {
                return;
            }
            Log.d("HeifEncoder", "onOutputBufferAvailable: " + i + ", time " + bufferInfo.presentationTimeUs + ", size " + bufferInfo.size + ", flags " + bufferInfo.flags);
            if (bufferInfo.size > 0 && (bufferInfo.flags & 2) == 0) {
                ByteBuffer outputBuffer = mediaCodec.getOutputBuffer(i);
                outputBuffer.position(bufferInfo.offset);
                outputBuffer.limit(bufferInfo.offset + bufferInfo.size);
                if (HeifEncoder.this.mEOSTracker != null) {
                    HeifEncoder.this.mEOSTracker.updateLastOutputTime(bufferInfo.presentationTimeUs);
                }
                HeifEncoder.this.mCallback.onDrainOutputBuffer(HeifEncoder.this, outputBuffer);
            }
            this.mOutputEOS = ((bufferInfo.flags & 4) != 0) | this.mOutputEOS;
            mediaCodec.releaseOutputBuffer(i, false);
            if (this.mOutputEOS) {
                stopAndNotify(null);
            }
        }

        @Override
        public void onError(MediaCodec mediaCodec, MediaCodec.CodecException codecException) {
            if (mediaCodec != HeifEncoder.this.mEncoder) {
                return;
            }
            Log.e("HeifEncoder", "onError: " + codecException);
            stopAndNotify(codecException);
        }

        private void stopAndNotify(MediaCodec.CodecException codecException) {
            HeifEncoder.this.stopInternal();
            if (codecException == null) {
                HeifEncoder.this.mCallback.onComplete(HeifEncoder.this);
            } else {
                HeifEncoder.this.mCallback.onError(HeifEncoder.this, codecException);
            }
        }
    }

    @Override
    public void close() {
        synchronized (this.mEmptyBuffers) {
            this.mInputEOS = true;
            this.mEmptyBuffers.notifyAll();
        }
        this.mHandler.postAtFrontOfQueue(new Runnable() {
            @Override
            public void run() {
                HeifEncoder.this.stopInternal();
            }
        });
    }
}
