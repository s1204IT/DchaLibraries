package android.util.proto;

import android.net.wifi.WifiEnterpriseConfig;
import android.net.wifi.WifiNetworkScoreCache;
import android.os.BatteryStats;
import android.util.Log;
import com.android.internal.midi.MidiConstants;
import java.util.ArrayList;

public final class EncodedBuffer {
    private static final String TAG = "EncodedBuffer";
    private int mBufferCount;
    private final ArrayList<byte[]> mBuffers;
    private final int mChunkSize;
    private int mReadBufIndex;
    private byte[] mReadBuffer;
    private int mReadIndex;
    private int mReadLimit;
    private int mReadableSize;
    private int mWriteBufIndex;
    private byte[] mWriteBuffer;
    private int mWriteIndex;

    public EncodedBuffer() {
        this(0);
    }

    public EncodedBuffer(int i) {
        this.mBuffers = new ArrayList<>();
        this.mReadLimit = -1;
        this.mReadableSize = -1;
        this.mChunkSize = i <= 0 ? 8192 : i;
        this.mWriteBuffer = new byte[this.mChunkSize];
        this.mBuffers.add(this.mWriteBuffer);
        this.mBufferCount = 1;
    }

    public void startEditing() {
        this.mReadableSize = (this.mWriteBufIndex * this.mChunkSize) + this.mWriteIndex;
        this.mReadLimit = this.mWriteIndex;
        this.mWriteBuffer = this.mBuffers.get(0);
        this.mWriteIndex = 0;
        this.mWriteBufIndex = 0;
        this.mReadBuffer = this.mWriteBuffer;
        this.mReadBufIndex = 0;
        this.mReadIndex = 0;
    }

    public void rewindRead() {
        this.mReadBuffer = this.mBuffers.get(0);
        this.mReadBufIndex = 0;
        this.mReadIndex = 0;
    }

    public int getReadableSize() {
        return this.mReadableSize;
    }

    public int getReadPos() {
        return (this.mReadBufIndex * this.mChunkSize) + this.mReadIndex;
    }

    public void skipRead(int i) {
        if (i < 0) {
            throw new RuntimeException("skipRead with negative amount=" + i);
        }
        if (i == 0) {
            return;
        }
        if (i <= this.mChunkSize - this.mReadIndex) {
            this.mReadIndex += i;
            return;
        }
        int i2 = i - (this.mChunkSize - this.mReadIndex);
        this.mReadIndex = i2 % this.mChunkSize;
        if (this.mReadIndex == 0) {
            this.mReadIndex = this.mChunkSize;
            this.mReadBufIndex += i2 / this.mChunkSize;
        } else {
            this.mReadBufIndex += 1 + (i2 / this.mChunkSize);
        }
        this.mReadBuffer = this.mBuffers.get(this.mReadBufIndex);
    }

    public byte readRawByte() {
        if (this.mReadBufIndex > this.mBufferCount || (this.mReadBufIndex == this.mBufferCount - 1 && this.mReadIndex >= this.mReadLimit)) {
            throw new IndexOutOfBoundsException("Trying to read too much data mReadBufIndex=" + this.mReadBufIndex + " mBufferCount=" + this.mBufferCount + " mReadIndex=" + this.mReadIndex + " mReadLimit=" + this.mReadLimit);
        }
        if (this.mReadIndex >= this.mChunkSize) {
            this.mReadBufIndex++;
            this.mReadBuffer = this.mBuffers.get(this.mReadBufIndex);
            this.mReadIndex = 0;
        }
        byte[] bArr = this.mReadBuffer;
        int i = this.mReadIndex;
        this.mReadIndex = i + 1;
        return bArr[i];
    }

    public long readRawUnsigned() {
        int i = 0;
        long j = 0;
        do {
            byte rawByte = readRawByte();
            j |= ((long) (rawByte & 127)) << i;
            if ((rawByte & 128) == 0) {
                return j;
            }
            i += 7;
        } while (i <= 64);
        throw new ProtoParseException("Varint too long -- " + getDebugString());
    }

    public int readRawFixed32() {
        return (readRawByte() & 255) | ((readRawByte() & 255) << 8) | ((readRawByte() & 255) << 16) | ((readRawByte() & 255) << 24);
    }

    private void nextWriteBuffer() {
        this.mWriteBufIndex++;
        if (this.mWriteBufIndex >= this.mBufferCount) {
            this.mWriteBuffer = new byte[this.mChunkSize];
            this.mBuffers.add(this.mWriteBuffer);
            this.mBufferCount++;
        } else {
            this.mWriteBuffer = this.mBuffers.get(this.mWriteBufIndex);
        }
        this.mWriteIndex = 0;
    }

    public void writeRawByte(byte b) {
        if (this.mWriteIndex >= this.mChunkSize) {
            nextWriteBuffer();
        }
        byte[] bArr = this.mWriteBuffer;
        int i = this.mWriteIndex;
        this.mWriteIndex = i + 1;
        bArr[i] = b;
    }

    public static int getRawVarint32Size(int i) {
        if ((i & WifiNetworkScoreCache.INVALID_NETWORK_SCORE) == 0) {
            return 1;
        }
        if ((i & (-16384)) == 0) {
            return 2;
        }
        if (((-2097152) & i) == 0) {
            return 3;
        }
        return (i & (-268435456)) == 0 ? 4 : 5;
    }

    public void writeRawVarint32(int i) {
        while ((i & WifiNetworkScoreCache.INVALID_NETWORK_SCORE) != 0) {
            writeRawByte((byte) ((i & 127) | 128));
            i >>>= 7;
        }
        writeRawByte((byte) i);
    }

    public static int getRawZigZag32Size(int i) {
        return getRawVarint32Size(zigZag32(i));
    }

    public void writeRawZigZag32(int i) {
        writeRawVarint32(zigZag32(i));
    }

    public static int getRawVarint64Size(long j) {
        if (((-128) & j) == 0) {
            return 1;
        }
        if (((-16384) & j) == 0) {
            return 2;
        }
        if (((-2097152) & j) == 0) {
            return 3;
        }
        if (((-268435456) & j) == 0) {
            return 4;
        }
        if (((-34359738368L) & j) == 0) {
            return 5;
        }
        if (((-4398046511104L) & j) == 0) {
            return 6;
        }
        if (((-562949953421312L) & j) == 0) {
            return 7;
        }
        if ((BatteryStats.STEP_LEVEL_MODIFIED_MODE_MASK & j) == 0) {
            return 8;
        }
        return (j & Long.MIN_VALUE) == 0 ? 9 : 10;
    }

    public void writeRawVarint64(long j) {
        while (((-128) & j) != 0) {
            writeRawByte((byte) ((127 & j) | 128));
            j >>>= 7;
        }
        writeRawByte((byte) j);
    }

    public static int getRawZigZag64Size(long j) {
        return getRawVarint64Size(zigZag64(j));
    }

    public void writeRawZigZag64(long j) {
        writeRawVarint64(zigZag64(j));
    }

    public void writeRawFixed32(int i) {
        writeRawByte((byte) i);
        writeRawByte((byte) (i >> 8));
        writeRawByte((byte) (i >> 16));
        writeRawByte((byte) (i >> 24));
    }

    public void writeRawFixed64(long j) {
        writeRawByte((byte) j);
        writeRawByte((byte) (j >> 8));
        writeRawByte((byte) (j >> 16));
        writeRawByte((byte) (j >> 24));
        writeRawByte((byte) (j >> 32));
        writeRawByte((byte) (j >> 40));
        writeRawByte((byte) (j >> 48));
        writeRawByte((byte) (j >> 56));
    }

    public void writeRawBuffer(byte[] bArr) {
        if (bArr != null && bArr.length > 0) {
            writeRawBuffer(bArr, 0, bArr.length);
        }
    }

    public void writeRawBuffer(byte[] bArr, int i, int i2) {
        int i3;
        int i4;
        if (bArr == null) {
            return;
        }
        if (i2 >= this.mChunkSize - this.mWriteIndex) {
            i3 = this.mChunkSize - this.mWriteIndex;
        } else {
            i3 = i2;
        }
        if (i3 > 0) {
            System.arraycopy(bArr, i, this.mWriteBuffer, this.mWriteIndex, i3);
            this.mWriteIndex += i3;
            i2 -= i3;
            i += i3;
        }
        while (i2 > 0) {
            nextWriteBuffer();
            if (i2 >= this.mChunkSize) {
                i4 = this.mChunkSize;
            } else {
                i4 = i2;
            }
            System.arraycopy(bArr, i, this.mWriteBuffer, this.mWriteIndex, i4);
            this.mWriteIndex += i4;
            i2 -= i4;
            i += i4;
        }
    }

    public void writeFromThisBuffer(int i, int i2) {
        if (this.mReadLimit < 0) {
            throw new IllegalStateException("writeFromThisBuffer before startEditing");
        }
        if (i < getWritePos()) {
            throw new IllegalArgumentException("Can only move forward in the buffer -- srcOffset=" + i + " size=" + i2 + WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER + getDebugString());
        }
        if (i + i2 > this.mReadableSize) {
            throw new IllegalArgumentException("Trying to move more data than there is -- srcOffset=" + i + " size=" + i2 + WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER + getDebugString());
        }
        if (i2 == 0) {
            return;
        }
        if (i == (this.mWriteBufIndex * this.mChunkSize) + this.mWriteIndex) {
            if (i2 <= this.mChunkSize - this.mWriteIndex) {
                this.mWriteIndex += i2;
                return;
            }
            int i3 = i2 - (this.mChunkSize - this.mWriteIndex);
            this.mWriteIndex = i3 % this.mChunkSize;
            if (this.mWriteIndex == 0) {
                this.mWriteIndex = this.mChunkSize;
                this.mWriteBufIndex += i3 / this.mChunkSize;
            } else {
                this.mWriteBufIndex += 1 + (i3 / this.mChunkSize);
            }
            this.mWriteBuffer = this.mBuffers.get(this.mWriteBufIndex);
            return;
        }
        int i4 = i / this.mChunkSize;
        byte[] bArr = this.mBuffers.get(i4);
        int i5 = i % this.mChunkSize;
        while (i2 > 0) {
            if (this.mWriteIndex >= this.mChunkSize) {
                nextWriteBuffer();
            }
            if (i5 >= this.mChunkSize) {
                i4++;
                bArr = this.mBuffers.get(i4);
                i5 = 0;
            }
            int iMin = Math.min(i2, Math.min(this.mChunkSize - this.mWriteIndex, this.mChunkSize - i5));
            System.arraycopy(bArr, i5, this.mWriteBuffer, this.mWriteIndex, iMin);
            this.mWriteIndex += iMin;
            i5 += iMin;
            i2 -= iMin;
        }
    }

    public int getWritePos() {
        return (this.mWriteBufIndex * this.mChunkSize) + this.mWriteIndex;
    }

    public void rewindWriteTo(int i) {
        if (i > getWritePos()) {
            throw new RuntimeException("rewindWriteTo only can go backwards" + i);
        }
        this.mWriteBufIndex = i / this.mChunkSize;
        this.mWriteIndex = i % this.mChunkSize;
        if (this.mWriteIndex == 0 && this.mWriteBufIndex != 0) {
            this.mWriteIndex = this.mChunkSize;
            this.mWriteBufIndex--;
        }
        this.mWriteBuffer = this.mBuffers.get(this.mWriteBufIndex);
    }

    public int getRawFixed32At(int i) {
        int i2 = i + 1;
        int i3 = (this.mBuffers.get(i / this.mChunkSize)[i % this.mChunkSize] & 255) | ((this.mBuffers.get(i2 / this.mChunkSize)[i2 % this.mChunkSize] & 255) << 8);
        int i4 = i + 2;
        int i5 = i + 3;
        return ((this.mBuffers.get(i5 / this.mChunkSize)[i5 % this.mChunkSize] & 255) << 24) | i3 | ((this.mBuffers.get(i4 / this.mChunkSize)[i4 % this.mChunkSize] & 255) << 16);
    }

    public void editRawFixed32(int i, int i2) {
        this.mBuffers.get(i / this.mChunkSize)[i % this.mChunkSize] = (byte) i2;
        int i3 = i + 1;
        this.mBuffers.get(i3 / this.mChunkSize)[i3 % this.mChunkSize] = (byte) (i2 >> 8);
        int i4 = i + 2;
        this.mBuffers.get(i4 / this.mChunkSize)[i4 % this.mChunkSize] = (byte) (i2 >> 16);
        int i5 = i + 3;
        this.mBuffers.get(i5 / this.mChunkSize)[i5 % this.mChunkSize] = (byte) (i2 >> 24);
    }

    private static int zigZag32(int i) {
        return (i >> 31) ^ (i << 1);
    }

    private static long zigZag64(long j) {
        return (j >> 63) ^ (j << 1);
    }

    public byte[] getBytes(int i) {
        byte[] bArr = new byte[i];
        int i2 = i / this.mChunkSize;
        int i3 = 0;
        int i4 = 0;
        while (i3 < i2) {
            System.arraycopy(this.mBuffers.get(i3), 0, bArr, i4, this.mChunkSize);
            i4 += this.mChunkSize;
            i3++;
        }
        int i5 = i - (i2 * this.mChunkSize);
        if (i5 > 0) {
            System.arraycopy(this.mBuffers.get(i3), 0, bArr, i4, i5);
        }
        return bArr;
    }

    public int getChunkCount() {
        return this.mBuffers.size();
    }

    public int getWriteIndex() {
        return this.mWriteIndex;
    }

    public int getWriteBufIndex() {
        return this.mWriteBufIndex;
    }

    public String getDebugString() {
        return "EncodedBuffer( mChunkSize=" + this.mChunkSize + " mBuffers.size=" + this.mBuffers.size() + " mBufferCount=" + this.mBufferCount + " mWriteIndex=" + this.mWriteIndex + " mWriteBufIndex=" + this.mWriteBufIndex + " mReadBufIndex=" + this.mReadBufIndex + " mReadIndex=" + this.mReadIndex + " mReadableSize=" + this.mReadableSize + " mReadLimit=" + this.mReadLimit + " )";
    }

    public void dumpBuffers(String str) {
        int size = this.mBuffers.size();
        int iDumpByteString = 0;
        for (int i = 0; i < size; i++) {
            iDumpByteString += dumpByteString(str, "{" + i + "} ", iDumpByteString, this.mBuffers.get(i));
        }
    }

    public static void dumpByteString(String str, String str2, byte[] bArr) {
        dumpByteString(str, str2, 0, bArr);
    }

    private static int dumpByteString(String str, String str2, int i, byte[] bArr) {
        StringBuffer stringBuffer = new StringBuffer();
        int length = bArr.length;
        for (int i2 = 0; i2 < length; i2++) {
            if (i2 % 16 == 0) {
                if (i2 != 0) {
                    Log.d(str, stringBuffer.toString());
                    stringBuffer = new StringBuffer();
                }
                stringBuffer.append(str2);
                stringBuffer.append('[');
                stringBuffer.append(i + i2);
                stringBuffer.append(']');
                stringBuffer.append(' ');
            } else {
                stringBuffer.append(' ');
            }
            byte b = bArr[i2];
            byte b2 = (byte) ((b >> 4) & 15);
            if (b2 < 10) {
                stringBuffer.append((char) (b2 + 48));
            } else {
                stringBuffer.append((char) (b2 + 87));
            }
            byte b3 = (byte) (b & MidiConstants.STATUS_CHANNEL_MASK);
            if (b3 < 10) {
                stringBuffer.append((char) (48 + b3));
            } else {
                stringBuffer.append((char) (87 + b3));
            }
        }
        Log.d(str, stringBuffer.toString());
        return length;
    }
}
