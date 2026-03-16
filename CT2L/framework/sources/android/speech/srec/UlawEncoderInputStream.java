package android.speech.srec;

import android.os.BatteryStats;
import android.view.WindowManager;
import java.io.IOException;
import java.io.InputStream;

public final class UlawEncoderInputStream extends InputStream {
    private static final int MAX_ULAW = 8192;
    private static final int SCALE_BITS = 16;
    private static final String TAG = "UlawEncoderInputStream";
    private InputStream mIn;
    private int mMax;
    private final byte[] mBuf = new byte[1024];
    private int mBufCount = 0;
    private final byte[] mOneByte = new byte[1];

    public static void encode(byte[] pcmBuf, int pcmOffset, byte[] ulawBuf, int ulawOffset, int length, int max) {
        int ulaw;
        if (max <= 0) {
            max = 8192;
        }
        int coef = 536870912 / max;
        int i = 0;
        int ulawOffset2 = ulawOffset;
        int pcmOffset2 = pcmOffset;
        while (i < length) {
            int pcmOffset3 = pcmOffset2 + 1;
            int i2 = pcmBuf[pcmOffset2] & 255;
            pcmOffset2 = pcmOffset3 + 1;
            int pcm = ((i2 + (pcmBuf[pcmOffset3] << 8)) * coef) >> 16;
            if (pcm >= 0) {
                if (pcm <= 0) {
                    ulaw = 255;
                } else {
                    ulaw = pcm <= 30 ? ((30 - pcm) >> 1) + 240 : pcm <= 94 ? ((94 - pcm) >> 2) + 224 : pcm <= 222 ? ((222 - pcm) >> 3) + 208 : pcm <= 478 ? ((478 - pcm) >> 4) + 192 : pcm <= 990 ? ((990 - pcm) >> 5) + 176 : pcm <= 2014 ? ((2014 - pcm) >> 6) + 160 : pcm <= 4062 ? ((4062 - pcm) >> 7) + 144 : pcm <= 8158 ? ((8158 - pcm) >> 8) + 128 : 128;
                }
            } else {
                ulaw = -1 <= pcm ? 127 : -31 <= pcm ? ((pcm + 31) >> 1) + 112 : -95 <= pcm ? ((pcm + 95) >> 2) + 96 : -223 <= pcm ? ((pcm + 223) >> 3) + 80 : -479 <= pcm ? ((pcm + 479) >> 4) + 64 : -991 <= pcm ? ((pcm + 991) >> 5) + 48 : -2015 <= pcm ? ((pcm + WindowManager.LayoutParams.TYPE_SECURE_SYSTEM_OVERLAY) >> 6) + 32 : -4063 <= pcm ? ((pcm + 4063) >> 7) + 16 : -8159 <= pcm ? ((pcm + 8159) >> 8) + 0 : 0;
            }
            ulawBuf[ulawOffset2] = (byte) ulaw;
            i++;
            ulawOffset2++;
        }
    }

    public static int maxAbsPcm(byte[] pcmBuf, int offset, int length) {
        int max = 0;
        int offset2 = offset;
        for (int i = 0; i < length; i++) {
            int offset3 = offset2 + 1;
            int i2 = pcmBuf[offset2] & BatteryStats.HistoryItem.CMD_NULL;
            offset2 = offset3 + 1;
            int pcm = i2 + (pcmBuf[offset3] << 8);
            if (pcm < 0) {
                pcm = -pcm;
            }
            if (pcm > max) {
                max = pcm;
            }
        }
        return max;
    }

    public UlawEncoderInputStream(InputStream in, int max) {
        this.mMax = 0;
        this.mIn = in;
        this.mMax = max;
    }

    @Override
    public int read(byte[] buf, int offset, int length) throws IOException {
        if (this.mIn == null) {
            throw new IllegalStateException("not open");
        }
        while (this.mBufCount < 2) {
            int n = this.mIn.read(this.mBuf, this.mBufCount, Math.min(length * 2, this.mBuf.length - this.mBufCount));
            if (n == -1) {
                return -1;
            }
            this.mBufCount += n;
        }
        int n2 = Math.min(this.mBufCount / 2, length);
        encode(this.mBuf, 0, buf, offset, n2, this.mMax);
        this.mBufCount -= n2 * 2;
        for (int i = 0; i < this.mBufCount; i++) {
            this.mBuf[i] = this.mBuf[(n2 * 2) + i];
        }
        return n2;
    }

    @Override
    public int read(byte[] buf) throws IOException {
        return read(buf, 0, buf.length);
    }

    @Override
    public int read() throws IOException {
        int n = read(this.mOneByte, 0, 1);
        if (n == -1) {
            return -1;
        }
        return this.mOneByte[0] & BatteryStats.HistoryItem.CMD_NULL;
    }

    @Override
    public void close() throws IOException {
        if (this.mIn != null) {
            InputStream in = this.mIn;
            this.mIn = null;
            in.close();
        }
    }

    @Override
    public int available() throws IOException {
        return (this.mIn.available() + this.mBufCount) / 2;
    }
}
