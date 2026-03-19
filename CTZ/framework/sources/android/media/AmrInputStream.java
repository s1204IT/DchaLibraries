package android.media;

import android.media.MediaCodec;
import android.util.Log;
import android.view.Surface;
import java.io.IOException;
import java.io.InputStream;

public final class AmrInputStream extends InputStream {
    private static final int SAMPLES_PER_FRAME = 160;
    private static final String TAG = "AmrInputStream";
    MediaCodec mCodec;
    MediaCodec.BufferInfo mInfo;
    private InputStream mInputStream;
    boolean mSawInputEOS;
    boolean mSawOutputEOS;
    private final byte[] mBuf = new byte[320];
    private int mBufIn = 0;
    private int mBufOut = 0;
    private byte[] mOneByte = new byte[1];

    public AmrInputStream(InputStream inputStream) {
        Log.w(TAG, "@@@@ AmrInputStream is not a public API @@@@");
        this.mInputStream = inputStream;
        MediaFormat mediaFormat = new MediaFormat();
        mediaFormat.setString(MediaFormat.KEY_MIME, MediaFormat.MIMETYPE_AUDIO_AMR_NB);
        mediaFormat.setInteger(MediaFormat.KEY_SAMPLE_RATE, 8000);
        mediaFormat.setInteger(MediaFormat.KEY_CHANNEL_COUNT, 1);
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, 12200);
        String strFindEncoderForFormat = new MediaCodecList(0).findEncoderForFormat(mediaFormat);
        if (strFindEncoderForFormat != null) {
            try {
                this.mCodec = MediaCodec.createByCodecName(strFindEncoderForFormat);
                this.mCodec.configure(mediaFormat, (Surface) null, (MediaCrypto) null, 1);
                this.mCodec.start();
            } catch (IOException e) {
                if (this.mCodec != null) {
                    this.mCodec.release();
                }
                this.mCodec = null;
            }
        }
        this.mInfo = new MediaCodec.BufferInfo();
    }

    @Override
    public int read() throws IOException {
        if (read(this.mOneByte, 0, 1) == 1) {
            return 255 & this.mOneByte[0];
        }
        return -1;
    }

    @Override
    public int read(byte[] bArr) throws IOException {
        return read(bArr, 0, bArr.length);
    }

    @Override
    public int read(byte[] bArr, int i, int i2) throws IOException {
        int i3;
        int iDequeueInputBuffer;
        if (this.mCodec == null) {
            throw new IllegalStateException("not open");
        }
        if (this.mBufOut >= this.mBufIn && !this.mSawOutputEOS) {
            this.mBufOut = 0;
            this.mBufIn = 0;
            while (!this.mSawInputEOS && (iDequeueInputBuffer = this.mCodec.dequeueInputBuffer(0L)) >= 0) {
                int i4 = 0;
                while (true) {
                    if (i4 >= 320) {
                        break;
                    }
                    int i5 = this.mInputStream.read(this.mBuf, i4, 320 - i4);
                    if (i5 == -1) {
                        this.mSawInputEOS = true;
                        break;
                    }
                    i4 += i5;
                }
                this.mCodec.getInputBuffer(iDequeueInputBuffer).put(this.mBuf, 0, i4);
                this.mCodec.queueInputBuffer(iDequeueInputBuffer, 0, i4, 0L, this.mSawInputEOS ? 4 : 0);
            }
            int iDequeueOutputBuffer = this.mCodec.dequeueOutputBuffer(this.mInfo, 0L);
            if (iDequeueOutputBuffer >= 0) {
                this.mBufIn = this.mInfo.size;
                this.mCodec.getOutputBuffer(iDequeueOutputBuffer).get(this.mBuf, 0, this.mBufIn);
                this.mCodec.releaseOutputBuffer(iDequeueOutputBuffer, false);
                if ((this.mInfo.flags & 4) != 0) {
                    this.mSawOutputEOS = true;
                }
            }
        }
        if (this.mBufOut >= this.mBufIn) {
            return (this.mSawInputEOS && this.mSawOutputEOS) ? -1 : 0;
        }
        if (i2 > this.mBufIn - this.mBufOut) {
            i3 = this.mBufIn - this.mBufOut;
        } else {
            i3 = i2;
        }
        System.arraycopy(this.mBuf, this.mBufOut, bArr, i, i3);
        this.mBufOut += i3;
        return i3;
    }

    @Override
    public void close() throws IOException {
        try {
            if (this.mInputStream != null) {
                this.mInputStream.close();
            }
            this.mInputStream = null;
            try {
                if (this.mCodec != null) {
                    this.mCodec.release();
                }
            } finally {
            }
        } catch (Throwable th) {
            this.mInputStream = null;
            try {
                if (this.mCodec != null) {
                    this.mCodec.release();
                }
                throw th;
            } finally {
            }
        }
    }

    protected void finalize() throws Throwable {
        if (this.mCodec != null) {
            Log.w(TAG, "AmrInputStream wasn't closed");
            this.mCodec.release();
        }
    }
}
