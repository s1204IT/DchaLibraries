package android.media;

import android.graphics.Rect;
import android.media.Image;
import android.os.Bundle;
import android.os.Handler;
import android.os.IHwBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PersistableBundle;
import android.view.Surface;
import com.android.internal.midi.MidiConstants;
import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.nio.ByteBuffer;
import java.nio.NioUtils;
import java.nio.ReadOnlyBufferException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public final class MediaCodec {
    public static final int BUFFER_FLAG_CODEC_CONFIG = 2;
    public static final int BUFFER_FLAG_END_OF_STREAM = 4;
    public static final int BUFFER_FLAG_KEY_FRAME = 1;
    public static final int BUFFER_FLAG_MUXER_DATA = 16;
    public static final int BUFFER_FLAG_PARTIAL_FRAME = 8;
    public static final int BUFFER_FLAG_SYNC_FRAME = 1;
    private static final int CB_ERROR = 3;
    private static final int CB_INPUT_AVAILABLE = 1;
    private static final int CB_OUTPUT_AVAILABLE = 2;
    private static final int CB_OUTPUT_FORMAT_CHANGE = 4;
    public static final int CONFIGURE_FLAG_ENCODE = 1;
    public static final int CRYPTO_MODE_AES_CBC = 2;
    public static final int CRYPTO_MODE_AES_CTR = 1;
    public static final int CRYPTO_MODE_UNENCRYPTED = 0;
    private static final int EVENT_CALLBACK = 1;
    private static final int EVENT_FRAME_RENDERED = 3;
    private static final int EVENT_SET_CALLBACK = 2;
    public static final int INFO_OUTPUT_BUFFERS_CHANGED = -3;
    public static final int INFO_OUTPUT_FORMAT_CHANGED = -2;
    public static final int INFO_TRY_AGAIN_LATER = -1;
    public static final String PARAMETER_KEY_REQUEST_SYNC_FRAME = "request-sync";
    public static final String PARAMETER_KEY_SUSPEND = "drop-input-frames";
    public static final String PARAMETER_KEY_VIDEO_BITRATE = "video-bitrate";
    public static final int VIDEO_SCALING_MODE_SCALE_TO_FIT = 1;
    public static final int VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING = 2;
    private final Object mBufferLock;
    private ByteBuffer[] mCachedInputBuffers;
    private ByteBuffer[] mCachedOutputBuffers;
    private Callback mCallback;
    private EventHandler mCallbackHandler;
    private MediaCodecInfo mCodecInfo;
    private final BufferMap mDequeuedInputBuffers;
    private final BufferMap mDequeuedOutputBuffers;
    private EventHandler mEventHandler;
    private long mNativeContext;
    private EventHandler mOnFrameRenderedHandler;
    private OnFrameRenderedListener mOnFrameRenderedListener;
    private final Object mListenerLock = new Object();
    private final Object mCodecInfoLock = new Object();
    private boolean mHasSurface = false;
    private final Map<Integer, BufferInfo> mDequeuedOutputInfos = new HashMap();

    @Retention(RetentionPolicy.SOURCE)
    public @interface BufferFlag {
    }

    public static abstract class Callback {
        public abstract void onError(MediaCodec mediaCodec, CodecException codecException);

        public abstract void onInputBufferAvailable(MediaCodec mediaCodec, int i);

        public abstract void onOutputBufferAvailable(MediaCodec mediaCodec, int i, BufferInfo bufferInfo);

        public abstract void onOutputFormatChanged(MediaCodec mediaCodec, MediaFormat mediaFormat);
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface ConfigureFlag {
    }

    public interface OnFrameRenderedListener {
        void onFrameRendered(MediaCodec mediaCodec, long j, long j2);
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface OutputBufferInfo {
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface VideoScalingMode {
    }

    private final native ByteBuffer getBuffer(boolean z, int i);

    private final native ByteBuffer[] getBuffers(boolean z);

    private final native Map<String, Object> getFormatNative(boolean z);

    private final native Image getImage(boolean z, int i);

    private final native Map<String, Object> getOutputFormatNative(int i);

    private final native MediaCodecInfo getOwnCodecInfo();

    private final native void native_configure(String[] strArr, Object[] objArr, Surface surface, MediaCrypto mediaCrypto, IHwBinder iHwBinder, int i);

    private static final native PersistentSurface native_createPersistentInputSurface();

    private final native int native_dequeueInputBuffer(long j);

    private final native int native_dequeueOutputBuffer(BufferInfo bufferInfo, long j);

    private native void native_enableOnFrameRenderedListener(boolean z);

    private final native void native_finalize();

    private final native void native_flush();

    private native PersistableBundle native_getMetrics();

    private static final native void native_init();

    private final native void native_queueInputBuffer(int i, int i2, int i3, long j, int i4) throws CryptoException;

    private final native void native_queueSecureInputBuffer(int i, int i2, CryptoInfo cryptoInfo, long j, int i3) throws CryptoException;

    private final native void native_release();

    private static final native void native_releasePersistentInputSurface(Surface surface);

    private final native void native_reset();

    private final native void native_setCallback(Callback callback);

    private final native void native_setInputSurface(Surface surface);

    private native void native_setSurface(Surface surface);

    private final native void native_setup(String str, boolean z, boolean z2);

    private final native void native_start();

    private final native void native_stop();

    private final native void releaseOutputBuffer(int i, boolean z, boolean z2, long j);

    private final native void setParameters(String[] strArr, Object[] objArr);

    public final native Surface createInputSurface();

    public final native String getName();

    public final native void setVideoScalingMode(int i);

    public final native void signalEndOfInputStream();

    public static final class BufferInfo {
        public int flags;
        public int offset;
        public long presentationTimeUs;
        public int size;

        public void set(int i, int i2, long j, int i3) {
            this.offset = i;
            this.size = i2;
            this.presentationTimeUs = j;
            this.flags = i3;
        }

        public BufferInfo dup() {
            BufferInfo bufferInfo = new BufferInfo();
            bufferInfo.set(this.offset, this.size, this.presentationTimeUs, this.flags);
            return bufferInfo;
        }
    }

    private class EventHandler extends Handler {
        private MediaCodec mCodec;

        public EventHandler(MediaCodec mediaCodec, Looper looper) {
            super(looper);
            this.mCodec = mediaCodec;
        }

        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case 1:
                    handleCallback(message);
                    return;
                case 2:
                    MediaCodec.this.mCallback = (Callback) message.obj;
                    return;
                case 3:
                    synchronized (MediaCodec.this.mListenerLock) {
                        Map map = (Map) message.obj;
                        int i = 0;
                        while (true) {
                            Object obj = map.get(i + "-media-time-us");
                            Object obj2 = map.get(i + "-system-nano");
                            if (obj != null && obj2 != null && MediaCodec.this.mOnFrameRenderedListener != null) {
                                MediaCodec.this.mOnFrameRenderedListener.onFrameRendered(this.mCodec, ((Long) obj).longValue(), ((Long) obj2).longValue());
                                i++;
                            }
                        }
                        break;
                    }
                    return;
                default:
                    return;
            }
        }

        private void handleCallback(Message message) {
            if (MediaCodec.this.mCallback == null) {
                return;
            }
            switch (message.arg1) {
                case 1:
                    int i = message.arg2;
                    synchronized (MediaCodec.this.mBufferLock) {
                        MediaCodec.this.validateInputByteBuffer(MediaCodec.this.mCachedInputBuffers, i);
                        break;
                    }
                    MediaCodec.this.mCallback.onInputBufferAvailable(this.mCodec, i);
                    return;
                case 2:
                    int i2 = message.arg2;
                    BufferInfo bufferInfo = (BufferInfo) message.obj;
                    synchronized (MediaCodec.this.mBufferLock) {
                        MediaCodec.this.validateOutputByteBuffer(MediaCodec.this.mCachedOutputBuffers, i2, bufferInfo);
                        break;
                    }
                    MediaCodec.this.mCallback.onOutputBufferAvailable(this.mCodec, i2, bufferInfo);
                    return;
                case 3:
                    MediaCodec.this.mCallback.onError(this.mCodec, (CodecException) message.obj);
                    return;
                case 4:
                    MediaCodec.this.mCallback.onOutputFormatChanged(this.mCodec, new MediaFormat((Map) message.obj));
                    return;
                default:
                    return;
            }
        }
    }

    public static MediaCodec createDecoderByType(String str) throws IOException {
        return new MediaCodec(str, true, false);
    }

    public static MediaCodec createEncoderByType(String str) throws IOException {
        return new MediaCodec(str, true, true);
    }

    public static MediaCodec createByCodecName(String str) throws IOException {
        return new MediaCodec(str, false, false);
    }

    private MediaCodec(String str, boolean z, boolean z2) {
        this.mDequeuedInputBuffers = new BufferMap();
        this.mDequeuedOutputBuffers = new BufferMap();
        Looper looperMyLooper = Looper.myLooper();
        if (looperMyLooper != null) {
            this.mEventHandler = new EventHandler(this, looperMyLooper);
        } else {
            Looper mainLooper = Looper.getMainLooper();
            if (mainLooper != null) {
                this.mEventHandler = new EventHandler(this, mainLooper);
            } else {
                this.mEventHandler = null;
            }
        }
        this.mCallbackHandler = this.mEventHandler;
        this.mOnFrameRenderedHandler = this.mEventHandler;
        this.mBufferLock = new Object();
        native_setup(str, z, z2);
    }

    protected void finalize() {
        native_finalize();
    }

    public final void reset() {
        freeAllTrackedBuffers();
        native_reset();
    }

    public final void release() {
        freeAllTrackedBuffers();
        native_release();
    }

    public void configure(MediaFormat mediaFormat, Surface surface, MediaCrypto mediaCrypto, int i) {
        configure(mediaFormat, surface, mediaCrypto, null, i);
    }

    public void configure(MediaFormat mediaFormat, Surface surface, int i, MediaDescrambler mediaDescrambler) {
        configure(mediaFormat, surface, null, mediaDescrambler != null ? mediaDescrambler.getBinder() : null, i);
    }

    private void configure(MediaFormat mediaFormat, Surface surface, MediaCrypto mediaCrypto, IHwBinder iHwBinder, int i) {
        Object[] objArr;
        if (mediaCrypto != null && iHwBinder != null) {
            throw new IllegalArgumentException("Can't use crypto and descrambler together!");
        }
        String[] strArr = null;
        if (mediaFormat != null) {
            Map<String, Object> map = mediaFormat.getMap();
            String[] strArr2 = new String[map.size()];
            Object[] objArr2 = new Object[map.size()];
            int i2 = 0;
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                if (entry.getKey().equals(MediaFormat.KEY_AUDIO_SESSION_ID)) {
                    try {
                        int iIntValue = ((Integer) entry.getValue()).intValue();
                        strArr2[i2] = "audio-hw-sync";
                        objArr2[i2] = Integer.valueOf(AudioSystem.getAudioHwSyncForSession(iIntValue));
                    } catch (Exception e) {
                        throw new IllegalArgumentException("Wrong Session ID Parameter!");
                    }
                } else {
                    strArr2[i2] = entry.getKey();
                    objArr2[i2] = entry.getValue();
                }
                i2++;
            }
            strArr = strArr2;
            objArr = objArr2;
        } else {
            objArr = null;
        }
        this.mHasSurface = surface != null;
        native_configure(strArr, objArr, surface, mediaCrypto, iHwBinder, i);
    }

    public void setOutputSurface(Surface surface) {
        if (!this.mHasSurface) {
            throw new IllegalStateException("codec was not configured for an output surface");
        }
        native_setSurface(surface);
    }

    public static Surface createPersistentInputSurface() {
        return native_createPersistentInputSurface();
    }

    static class PersistentSurface extends Surface {
        private long mPersistentObject;

        PersistentSurface() {
        }

        @Override
        public void release() {
            MediaCodec.native_releasePersistentInputSurface(this);
            super.release();
        }
    }

    public void setInputSurface(Surface surface) {
        if (!(surface instanceof PersistentSurface)) {
            throw new IllegalArgumentException("not a PersistentSurface");
        }
        native_setInputSurface(surface);
    }

    public final void start() {
        native_start();
        synchronized (this.mBufferLock) {
            cacheBuffers(true);
            cacheBuffers(false);
        }
    }

    public final void stop() {
        native_stop();
        freeAllTrackedBuffers();
        synchronized (this.mListenerLock) {
            if (this.mCallbackHandler != null) {
                this.mCallbackHandler.removeMessages(2);
                this.mCallbackHandler.removeMessages(1);
            }
            if (this.mOnFrameRenderedHandler != null) {
                this.mOnFrameRenderedHandler.removeMessages(3);
            }
        }
    }

    public final void flush() {
        synchronized (this.mBufferLock) {
            invalidateByteBuffers(this.mCachedInputBuffers);
            invalidateByteBuffers(this.mCachedOutputBuffers);
            this.mDequeuedInputBuffers.clear();
            this.mDequeuedOutputBuffers.clear();
        }
        native_flush();
    }

    public static final class CodecException extends IllegalStateException {
        private static final int ACTION_RECOVERABLE = 2;
        private static final int ACTION_TRANSIENT = 1;
        public static final int ERROR_INSUFFICIENT_RESOURCE = 1100;
        public static final int ERROR_RECLAIMED = 1101;
        private final int mActionCode;
        private final String mDiagnosticInfo;
        private final int mErrorCode;

        @Retention(RetentionPolicy.SOURCE)
        public @interface ReasonCode {
        }

        CodecException(int i, int i2, String str) {
            super(str);
            this.mErrorCode = i;
            this.mActionCode = i2;
            this.mDiagnosticInfo = "android.media.MediaCodec.error_" + (i < 0 ? "neg_" : "") + Math.abs(i);
        }

        public boolean isTransient() {
            return this.mActionCode == 1;
        }

        public boolean isRecoverable() {
            return this.mActionCode == 2;
        }

        public int getErrorCode() {
            return this.mErrorCode;
        }

        public String getDiagnosticInfo() {
            return this.mDiagnosticInfo;
        }
    }

    public static final class CryptoException extends RuntimeException {
        public static final int ERROR_INSUFFICIENT_OUTPUT_PROTECTION = 4;
        public static final int ERROR_KEY_EXPIRED = 2;
        public static final int ERROR_NO_KEY = 1;
        public static final int ERROR_RESOURCE_BUSY = 3;
        public static final int ERROR_SESSION_NOT_OPENED = 5;
        public static final int ERROR_UNSUPPORTED_OPERATION = 6;
        private int mErrorCode;

        @Retention(RetentionPolicy.SOURCE)
        public @interface CryptoErrorCode {
        }

        public CryptoException(int i, String str) {
            super(str);
            this.mErrorCode = i;
        }

        public int getErrorCode() {
            return this.mErrorCode;
        }
    }

    public final void queueInputBuffer(int i, int i2, int i3, long j, int i4) throws CryptoException {
        synchronized (this.mBufferLock) {
            invalidateByteBuffer(this.mCachedInputBuffers, i);
            this.mDequeuedInputBuffers.remove(i);
        }
        try {
            native_queueInputBuffer(i, i2, i3, j, i4);
        } catch (CryptoException | IllegalStateException e) {
            revalidateByteBuffer(this.mCachedInputBuffers, i);
            throw e;
        }
    }

    public static final class CryptoInfo {
        public byte[] iv;
        public byte[] key;
        public int mode;
        public int[] numBytesOfClearData;
        public int[] numBytesOfEncryptedData;
        public int numSubSamples;
        private Pattern pattern;
        private final Pattern zeroPattern = new Pattern(0, 0);

        public static final class Pattern {
            private int mEncryptBlocks;
            private int mSkipBlocks;

            public Pattern(int i, int i2) {
                set(i, i2);
            }

            public void set(int i, int i2) {
                this.mEncryptBlocks = i;
                this.mSkipBlocks = i2;
            }

            public int getSkipBlocks() {
                return this.mSkipBlocks;
            }

            public int getEncryptBlocks() {
                return this.mEncryptBlocks;
            }
        }

        public void set(int i, int[] iArr, int[] iArr2, byte[] bArr, byte[] bArr2, int i2) {
            this.numSubSamples = i;
            this.numBytesOfClearData = iArr;
            this.numBytesOfEncryptedData = iArr2;
            this.key = bArr;
            this.iv = bArr2;
            this.mode = i2;
            this.pattern = this.zeroPattern;
        }

        public void setPattern(Pattern pattern) {
            this.pattern = pattern;
        }

        private void setPattern(int i, int i2) {
            this.pattern = new Pattern(i, i2);
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(this.numSubSamples + " subsamples, key [");
            for (int i = 0; i < this.key.length; i++) {
                sb.append("0123456789abcdef".charAt((this.key[i] & 240) >> 4));
                sb.append("0123456789abcdef".charAt(this.key[i] & MidiConstants.STATUS_CHANNEL_MASK));
            }
            sb.append("], iv [");
            for (int i2 = 0; i2 < this.key.length; i2++) {
                sb.append("0123456789abcdef".charAt((this.iv[i2] & 240) >> 4));
                sb.append("0123456789abcdef".charAt(this.iv[i2] & MidiConstants.STATUS_CHANNEL_MASK));
            }
            sb.append("], clear ");
            sb.append(Arrays.toString(this.numBytesOfClearData));
            sb.append(", encrypted ");
            sb.append(Arrays.toString(this.numBytesOfEncryptedData));
            return sb.toString();
        }
    }

    public final void queueSecureInputBuffer(int i, int i2, CryptoInfo cryptoInfo, long j, int i3) throws CryptoException {
        synchronized (this.mBufferLock) {
            invalidateByteBuffer(this.mCachedInputBuffers, i);
            this.mDequeuedInputBuffers.remove(i);
        }
        try {
            native_queueSecureInputBuffer(i, i2, cryptoInfo, j, i3);
        } catch (CryptoException | IllegalStateException e) {
            revalidateByteBuffer(this.mCachedInputBuffers, i);
            throw e;
        }
    }

    public final int dequeueInputBuffer(long j) {
        int iNative_dequeueInputBuffer = native_dequeueInputBuffer(j);
        if (iNative_dequeueInputBuffer >= 0) {
            synchronized (this.mBufferLock) {
                validateInputByteBuffer(this.mCachedInputBuffers, iNative_dequeueInputBuffer);
            }
        }
        return iNative_dequeueInputBuffer;
    }

    public final int dequeueOutputBuffer(BufferInfo bufferInfo, long j) {
        int iNative_dequeueOutputBuffer = native_dequeueOutputBuffer(bufferInfo, j);
        synchronized (this.mBufferLock) {
            try {
                if (iNative_dequeueOutputBuffer == -3) {
                    cacheBuffers(false);
                } else if (iNative_dequeueOutputBuffer >= 0) {
                    validateOutputByteBuffer(this.mCachedOutputBuffers, iNative_dequeueOutputBuffer, bufferInfo);
                    if (this.mHasSurface) {
                        this.mDequeuedOutputInfos.put(Integer.valueOf(iNative_dequeueOutputBuffer), bufferInfo.dup());
                    }
                }
            } catch (Throwable th) {
                throw th;
            }
        }
        return iNative_dequeueOutputBuffer;
    }

    public final void releaseOutputBuffer(int i, boolean z) {
        synchronized (this.mBufferLock) {
            invalidateByteBuffer(this.mCachedOutputBuffers, i);
            this.mDequeuedOutputBuffers.remove(i);
            if (this.mHasSurface) {
                this.mDequeuedOutputInfos.remove(Integer.valueOf(i));
            }
        }
        releaseOutputBuffer(i, z, false, 0L);
    }

    public final void releaseOutputBuffer(int i, long j) {
        synchronized (this.mBufferLock) {
            invalidateByteBuffer(this.mCachedOutputBuffers, i);
            this.mDequeuedOutputBuffers.remove(i);
            if (this.mHasSurface) {
                this.mDequeuedOutputInfos.remove(Integer.valueOf(i));
            }
        }
        releaseOutputBuffer(i, true, true, j);
    }

    public final MediaFormat getOutputFormat() {
        return new MediaFormat(getFormatNative(false));
    }

    public final MediaFormat getInputFormat() {
        return new MediaFormat(getFormatNative(true));
    }

    public final MediaFormat getOutputFormat(int i) {
        return new MediaFormat(getOutputFormatNative(i));
    }

    private static class BufferMap {
        private final Map<Integer, CodecBuffer> mMap;

        private BufferMap() {
            this.mMap = new HashMap();
        }

        private static class CodecBuffer {
            private ByteBuffer mByteBuffer;
            private Image mImage;

            private CodecBuffer() {
            }

            public void free() {
                if (this.mByteBuffer != null) {
                    NioUtils.freeDirectBuffer(this.mByteBuffer);
                    this.mByteBuffer = null;
                }
                if (this.mImage != null) {
                    this.mImage.close();
                    this.mImage = null;
                }
            }

            public void setImage(Image image) {
                free();
                this.mImage = image;
            }

            public void setByteBuffer(ByteBuffer byteBuffer) {
                free();
                this.mByteBuffer = byteBuffer;
            }
        }

        public void remove(int i) {
            CodecBuffer codecBuffer = this.mMap.get(Integer.valueOf(i));
            if (codecBuffer != null) {
                codecBuffer.free();
                this.mMap.remove(Integer.valueOf(i));
            }
        }

        public void put(int i, ByteBuffer byteBuffer) {
            CodecBuffer codecBuffer = this.mMap.get(Integer.valueOf(i));
            if (codecBuffer == null) {
                codecBuffer = new CodecBuffer();
                this.mMap.put(Integer.valueOf(i), codecBuffer);
            }
            codecBuffer.setByteBuffer(byteBuffer);
        }

        public void put(int i, Image image) {
            CodecBuffer codecBuffer = this.mMap.get(Integer.valueOf(i));
            if (codecBuffer == null) {
                codecBuffer = new CodecBuffer();
                this.mMap.put(Integer.valueOf(i), codecBuffer);
            }
            codecBuffer.setImage(image);
        }

        public void clear() {
            Iterator<CodecBuffer> it = this.mMap.values().iterator();
            while (it.hasNext()) {
                it.next().free();
            }
            this.mMap.clear();
        }
    }

    private final void invalidateByteBuffer(ByteBuffer[] byteBufferArr, int i) {
        ByteBuffer byteBuffer;
        if (byteBufferArr != null && i >= 0 && i < byteBufferArr.length && (byteBuffer = byteBufferArr[i]) != null) {
            byteBuffer.setAccessible(false);
        }
    }

    private final void validateInputByteBuffer(ByteBuffer[] byteBufferArr, int i) {
        ByteBuffer byteBuffer;
        if (byteBufferArr != null && i >= 0 && i < byteBufferArr.length && (byteBuffer = byteBufferArr[i]) != null) {
            byteBuffer.setAccessible(true);
            byteBuffer.clear();
        }
    }

    private final void revalidateByteBuffer(ByteBuffer[] byteBufferArr, int i) {
        ByteBuffer byteBuffer;
        synchronized (this.mBufferLock) {
            if (byteBufferArr != null && i >= 0) {
                if (i < byteBufferArr.length && (byteBuffer = byteBufferArr[i]) != null) {
                    byteBuffer.setAccessible(true);
                }
            }
        }
    }

    private final void validateOutputByteBuffer(ByteBuffer[] byteBufferArr, int i, BufferInfo bufferInfo) {
        ByteBuffer byteBuffer;
        if (byteBufferArr != null && i >= 0 && i < byteBufferArr.length && (byteBuffer = byteBufferArr[i]) != null) {
            byteBuffer.setAccessible(true);
            byteBuffer.limit(bufferInfo.offset + bufferInfo.size).position(bufferInfo.offset);
        }
    }

    private final void invalidateByteBuffers(ByteBuffer[] byteBufferArr) {
        if (byteBufferArr != null) {
            for (ByteBuffer byteBuffer : byteBufferArr) {
                if (byteBuffer != null) {
                    byteBuffer.setAccessible(false);
                }
            }
        }
    }

    private final void freeByteBuffer(ByteBuffer byteBuffer) {
        if (byteBuffer != null) {
            NioUtils.freeDirectBuffer(byteBuffer);
        }
    }

    private final void freeByteBuffers(ByteBuffer[] byteBufferArr) {
        if (byteBufferArr != null) {
            for (ByteBuffer byteBuffer : byteBufferArr) {
                freeByteBuffer(byteBuffer);
            }
        }
    }

    private final void freeAllTrackedBuffers() {
        synchronized (this.mBufferLock) {
            freeByteBuffers(this.mCachedInputBuffers);
            freeByteBuffers(this.mCachedOutputBuffers);
            this.mCachedInputBuffers = null;
            this.mCachedOutputBuffers = null;
            this.mDequeuedInputBuffers.clear();
            this.mDequeuedOutputBuffers.clear();
        }
    }

    private final void cacheBuffers(boolean z) {
        ByteBuffer[] buffers;
        try {
            buffers = getBuffers(z);
            try {
                invalidateByteBuffers(buffers);
            } catch (IllegalStateException e) {
            }
        } catch (IllegalStateException e2) {
            buffers = null;
        }
        if (z) {
            this.mCachedInputBuffers = buffers;
        } else {
            this.mCachedOutputBuffers = buffers;
        }
    }

    public ByteBuffer[] getInputBuffers() {
        if (this.mCachedInputBuffers == null) {
            throw new IllegalStateException();
        }
        return this.mCachedInputBuffers;
    }

    public ByteBuffer[] getOutputBuffers() {
        if (this.mCachedOutputBuffers == null) {
            throw new IllegalStateException();
        }
        return this.mCachedOutputBuffers;
    }

    public ByteBuffer getInputBuffer(int i) {
        ByteBuffer buffer = getBuffer(true, i);
        synchronized (this.mBufferLock) {
            invalidateByteBuffer(this.mCachedInputBuffers, i);
            this.mDequeuedInputBuffers.put(i, buffer);
        }
        return buffer;
    }

    public Image getInputImage(int i) {
        Image image = getImage(true, i);
        synchronized (this.mBufferLock) {
            invalidateByteBuffer(this.mCachedInputBuffers, i);
            this.mDequeuedInputBuffers.put(i, image);
        }
        return image;
    }

    public ByteBuffer getOutputBuffer(int i) {
        ByteBuffer buffer = getBuffer(false, i);
        synchronized (this.mBufferLock) {
            invalidateByteBuffer(this.mCachedOutputBuffers, i);
            this.mDequeuedOutputBuffers.put(i, buffer);
        }
        return buffer;
    }

    public Image getOutputImage(int i) {
        Image image = getImage(false, i);
        synchronized (this.mBufferLock) {
            invalidateByteBuffer(this.mCachedOutputBuffers, i);
            this.mDequeuedOutputBuffers.put(i, image);
        }
        return image;
    }

    public PersistableBundle getMetrics() {
        return native_getMetrics();
    }

    public final void setParameters(Bundle bundle) {
        if (bundle == null) {
            return;
        }
        String[] strArr = new String[bundle.size()];
        Object[] objArr = new Object[bundle.size()];
        int i = 0;
        for (String str : bundle.keySet()) {
            strArr[i] = str;
            objArr[i] = bundle.get(str);
            i++;
        }
        setParameters(strArr, objArr);
    }

    public void setCallback(Callback callback, Handler handler) {
        if (callback != null) {
            synchronized (this.mListenerLock) {
                EventHandler eventHandlerOn = getEventHandlerOn(handler, this.mCallbackHandler);
                if (eventHandlerOn != this.mCallbackHandler) {
                    this.mCallbackHandler.removeMessages(2);
                    this.mCallbackHandler.removeMessages(1);
                    this.mCallbackHandler = eventHandlerOn;
                }
            }
        } else if (this.mCallbackHandler != null) {
            this.mCallbackHandler.removeMessages(2);
            this.mCallbackHandler.removeMessages(1);
        }
        if (this.mCallbackHandler != null) {
            this.mCallbackHandler.sendMessage(this.mCallbackHandler.obtainMessage(2, 0, 0, callback));
            native_setCallback(callback);
        }
    }

    public void setCallback(Callback callback) {
        setCallback(callback, null);
    }

    public void setOnFrameRenderedListener(OnFrameRenderedListener onFrameRenderedListener, Handler handler) {
        synchronized (this.mListenerLock) {
            this.mOnFrameRenderedListener = onFrameRenderedListener;
            if (onFrameRenderedListener != null) {
                EventHandler eventHandlerOn = getEventHandlerOn(handler, this.mOnFrameRenderedHandler);
                if (eventHandlerOn != this.mOnFrameRenderedHandler) {
                    this.mOnFrameRenderedHandler.removeMessages(3);
                }
                this.mOnFrameRenderedHandler = eventHandlerOn;
            } else if (this.mOnFrameRenderedHandler != null) {
                this.mOnFrameRenderedHandler.removeMessages(3);
            }
            native_enableOnFrameRenderedListener(onFrameRenderedListener != null);
        }
    }

    private EventHandler getEventHandlerOn(Handler handler, EventHandler eventHandler) {
        if (handler == null) {
            return this.mEventHandler;
        }
        Looper looper = handler.getLooper();
        if (eventHandler.getLooper() == looper) {
            return eventHandler;
        }
        return new EventHandler(this, looper);
    }

    private void postEventFromNative(int i, int i2, int i3, Object obj) {
        synchronized (this.mListenerLock) {
            EventHandler eventHandler = this.mEventHandler;
            if (i == 1) {
                eventHandler = this.mCallbackHandler;
            } else if (i == 3) {
                eventHandler = this.mOnFrameRenderedHandler;
            }
            if (eventHandler != null) {
                eventHandler.sendMessage(eventHandler.obtainMessage(i, i2, i3, obj));
            }
        }
    }

    public MediaCodecInfo getCodecInfo() {
        MediaCodecInfo mediaCodecInfo;
        String name = getName();
        synchronized (this.mCodecInfoLock) {
            if (this.mCodecInfo == null) {
                this.mCodecInfo = getOwnCodecInfo();
                if (this.mCodecInfo == null) {
                    this.mCodecInfo = MediaCodecList.getInfoFor(name);
                }
            }
            mediaCodecInfo = this.mCodecInfo;
        }
        return mediaCodecInfo;
    }

    static {
        System.loadLibrary("media_jni");
        native_init();
    }

    public static class MediaImage extends Image {
        private static final int TYPE_YUV = 1;
        private final ByteBuffer mBuffer;
        private final int mHeight;
        private final ByteBuffer mInfo;
        private final boolean mIsReadOnly;
        private final Image.Plane[] mPlanes;
        private long mTimestamp;
        private final int mWidth;
        private final int mXOffset;
        private final int mYOffset;
        private final int mTransform = 0;
        private final int mScalingMode = 0;
        private final int mFormat = 35;

        @Override
        public int getFormat() {
            throwISEIfImageIsInvalid();
            return this.mFormat;
        }

        @Override
        public int getHeight() {
            throwISEIfImageIsInvalid();
            return this.mHeight;
        }

        @Override
        public int getWidth() {
            throwISEIfImageIsInvalid();
            return this.mWidth;
        }

        @Override
        public int getTransform() {
            throwISEIfImageIsInvalid();
            return 0;
        }

        @Override
        public int getScalingMode() {
            throwISEIfImageIsInvalid();
            return 0;
        }

        @Override
        public long getTimestamp() {
            throwISEIfImageIsInvalid();
            return this.mTimestamp;
        }

        @Override
        public Image.Plane[] getPlanes() {
            throwISEIfImageIsInvalid();
            return (Image.Plane[]) Arrays.copyOf(this.mPlanes, this.mPlanes.length);
        }

        @Override
        public void close() {
            if (this.mIsImageValid) {
                NioUtils.freeDirectBuffer(this.mBuffer);
                this.mIsImageValid = false;
            }
        }

        @Override
        public void setCropRect(Rect rect) {
            if (this.mIsReadOnly) {
                throw new ReadOnlyBufferException();
            }
            super.setCropRect(rect);
        }

        public MediaImage(ByteBuffer byteBuffer, ByteBuffer byteBuffer2, boolean z, long j, int i, int i2, Rect rect) {
            Rect rect2;
            this.mTimestamp = j;
            this.mIsImageValid = true;
            this.mIsReadOnly = byteBuffer.isReadOnly();
            this.mBuffer = byteBuffer.duplicate();
            this.mXOffset = i;
            this.mYOffset = i2;
            this.mInfo = byteBuffer2;
            if (byteBuffer2.remaining() == 104) {
                int i3 = byteBuffer2.getInt();
                if (i3 != 1) {
                    throw new UnsupportedOperationException("unsupported type: " + i3);
                }
                int i4 = byteBuffer2.getInt();
                if (i4 != 3) {
                    throw new RuntimeException("unexpected number of planes: " + i4);
                }
                this.mWidth = byteBuffer2.getInt();
                this.mHeight = byteBuffer2.getInt();
                if (this.mWidth < 1 || this.mHeight < 1) {
                    throw new UnsupportedOperationException("unsupported size: " + this.mWidth + "x" + this.mHeight);
                }
                int i5 = byteBuffer2.getInt();
                if (i5 != 8) {
                    throw new UnsupportedOperationException("unsupported bit depth: " + i5);
                }
                int i6 = byteBuffer2.getInt();
                if (i6 != 8) {
                    throw new UnsupportedOperationException("unsupported allocated bit depth: " + i6);
                }
                this.mPlanes = new MediaPlane[i4];
                int i7 = 0;
                while (i7 < i4) {
                    int i8 = byteBuffer2.getInt();
                    int i9 = byteBuffer2.getInt();
                    int i10 = byteBuffer2.getInt();
                    int i11 = byteBuffer2.getInt();
                    int i12 = byteBuffer2.getInt();
                    if (i11 == i12) {
                        if (i11 == (i7 == 0 ? 1 : 2)) {
                            if (i9 < 1 || i10 < 1) {
                                throw new UnsupportedOperationException("unexpected strides: " + i9 + " pixel, " + i10 + " row on plane " + i7);
                            }
                            byteBuffer.clear();
                            byteBuffer.position(this.mBuffer.position() + i8 + ((i / i11) * i9) + ((i2 / i12) * i10));
                            byteBuffer.limit(byteBuffer.position() + Utils.divUp(i5, 8) + (((this.mHeight / i12) - 1) * i10) + (((this.mWidth / i11) - 1) * i9));
                            this.mPlanes[i7] = new MediaPlane(byteBuffer.slice(), i10, i9);
                            i7++;
                        }
                    }
                    throw new UnsupportedOperationException("unexpected subsampling: " + i11 + "x" + i12 + " on plane " + i7);
                }
                if (rect == null) {
                    rect2 = new Rect(0, 0, this.mWidth, this.mHeight);
                } else {
                    rect2 = rect;
                }
                rect2.offset(-i, -i2);
                super.setCropRect(rect2);
                return;
            }
            throw new UnsupportedOperationException("unsupported info length: " + byteBuffer2.remaining());
        }

        private class MediaPlane extends Image.Plane {
            private final int mColInc;
            private final ByteBuffer mData;
            private final int mRowInc;

            public MediaPlane(ByteBuffer byteBuffer, int i, int i2) {
                this.mData = byteBuffer;
                this.mRowInc = i;
                this.mColInc = i2;
            }

            @Override
            public int getRowStride() {
                MediaImage.this.throwISEIfImageIsInvalid();
                return this.mRowInc;
            }

            @Override
            public int getPixelStride() {
                MediaImage.this.throwISEIfImageIsInvalid();
                return this.mColInc;
            }

            @Override
            public ByteBuffer getBuffer() {
                MediaImage.this.throwISEIfImageIsInvalid();
                return this.mData;
            }
        }
    }

    public static final class MetricsConstants {
        public static final String CODEC = "android.media.mediacodec.codec";
        public static final String ENCODER = "android.media.mediacodec.encoder";
        public static final String HEIGHT = "android.media.mediacodec.height";
        public static final String MIME_TYPE = "android.media.mediacodec.mime";
        public static final String MODE = "android.media.mediacodec.mode";
        public static final String MODE_AUDIO = "audio";
        public static final String MODE_VIDEO = "video";
        public static final String ROTATION = "android.media.mediacodec.rotation";
        public static final String SECURE = "android.media.mediacodec.secure";
        public static final String WIDTH = "android.media.mediacodec.width";

        private MetricsConstants() {
        }
    }
}
