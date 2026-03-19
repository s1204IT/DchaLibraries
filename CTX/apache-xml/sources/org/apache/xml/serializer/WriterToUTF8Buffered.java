package org.apache.xml.serializer;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;

final class WriterToUTF8Buffered extends Writer implements WriterChain {
    private static final int BYTES_MAX = 16384;
    private static final int CHARS_MAX = 5461;
    private final OutputStream m_os;
    private final byte[] m_outputBytes = new byte[16387];
    private final char[] m_inputChars = new char[5463];
    private int count = 0;

    public WriterToUTF8Buffered(OutputStream outputStream) {
        this.m_os = outputStream;
    }

    @Override
    public void write(int i) throws IOException {
        if (this.count >= 16384) {
            flushBuffer();
        }
        if (i < 128) {
            byte[] bArr = this.m_outputBytes;
            int i2 = this.count;
            this.count = i2 + 1;
            bArr[i2] = (byte) i;
            return;
        }
        if (i < 2048) {
            byte[] bArr2 = this.m_outputBytes;
            int i3 = this.count;
            this.count = i3 + 1;
            bArr2[i3] = (byte) (192 + (i >> 6));
            byte[] bArr3 = this.m_outputBytes;
            int i4 = this.count;
            this.count = i4 + 1;
            bArr3[i4] = (byte) (128 + (i & 63));
            return;
        }
        if (i < 65536) {
            byte[] bArr4 = this.m_outputBytes;
            int i5 = this.count;
            this.count = i5 + 1;
            bArr4[i5] = (byte) (224 + (i >> 12));
            byte[] bArr5 = this.m_outputBytes;
            int i6 = this.count;
            this.count = i6 + 1;
            bArr5[i6] = (byte) (((i >> 6) & 63) + 128);
            byte[] bArr6 = this.m_outputBytes;
            int i7 = this.count;
            this.count = i7 + 1;
            bArr6[i7] = (byte) (128 + (i & 63));
            return;
        }
        byte[] bArr7 = this.m_outputBytes;
        int i8 = this.count;
        this.count = i8 + 1;
        bArr7[i8] = (byte) (240 + (i >> 18));
        byte[] bArr8 = this.m_outputBytes;
        int i9 = this.count;
        this.count = i9 + 1;
        bArr8[i9] = (byte) (((i >> 12) & 63) + 128);
        byte[] bArr9 = this.m_outputBytes;
        int i10 = this.count;
        this.count = i10 + 1;
        bArr9[i10] = (byte) (((i >> 6) & 63) + 128);
        byte[] bArr10 = this.m_outputBytes;
        int i11 = this.count;
        this.count = i11 + 1;
        bArr10[i11] = (byte) (128 + (i & 63));
    }

    @Override
    public void write(char[] cArr, int i, int i2) throws IOException {
        int i3;
        char c;
        int i4 = 3 * i2;
        int i5 = 1;
        if (i4 >= 16384 - this.count) {
            flushBuffer();
            if (i4 > 16384) {
                int i6 = i2 / CHARS_MAX;
                if (i2 % CHARS_MAX > 0) {
                    i6++;
                }
                int i7 = i;
                while (i5 <= i6) {
                    int i8 = ((int) ((((long) i2) * ((long) i5)) / ((long) i6))) + i;
                    int i9 = i8 - 1;
                    char c2 = cArr[i9];
                    char c3 = cArr[i9];
                    if (c2 >= 55296 && c2 <= 56319) {
                        i8 = i8 < i + i2 ? i8 + 1 : i8 - 1;
                    }
                    write(cArr, i7, i8 - i7);
                    i5++;
                    i7 = i8;
                }
                return;
            }
        }
        int i10 = i2 + i;
        byte[] bArr = this.m_outputBytes;
        int i11 = this.count;
        while (i < i10 && (c = cArr[i]) < 128) {
            bArr[i11] = (byte) c;
            i++;
            i11++;
        }
        while (i < i10) {
            char c4 = cArr[i];
            if (c4 < 128) {
                i3 = i11 + 1;
                bArr[i11] = (byte) c4;
            } else {
                if (c4 < 2048) {
                    int i12 = i11 + 1;
                    bArr[i11] = (byte) (192 + (c4 >> 6));
                    i11 = i12 + 1;
                    bArr[i12] = (byte) ((c4 & '?') + 128);
                } else if (c4 >= 55296 && c4 <= 56319) {
                    i++;
                    char c5 = cArr[i];
                    int i13 = i11 + 1;
                    int i14 = c4 + '@';
                    bArr[i11] = (byte) (((i14 >> 8) & 240) | 240);
                    int i15 = i13 + 1;
                    bArr[i13] = (byte) (((i14 >> 2) & 63) | 128);
                    int i16 = i15 + 1;
                    bArr[i15] = (byte) (128 | (((c5 >> 6) & 15) + ((c4 << 4) & 48)));
                    i11 = i16 + 1;
                    bArr[i16] = (byte) ((c5 & '?') | 128);
                } else {
                    int i17 = i11 + 1;
                    bArr[i11] = (byte) (224 + (c4 >> '\f'));
                    int i18 = i17 + 1;
                    bArr[i17] = (byte) (((c4 >> 6) & 63) + 128);
                    i3 = i18 + 1;
                    bArr[i18] = (byte) ((c4 & '?') + 128);
                }
                i++;
            }
            i11 = i3;
            i++;
        }
        this.count = i11;
    }

    @Override
    public void write(String str) throws IOException {
        int i;
        char c;
        int length = str.length();
        int i2 = 3 * length;
        int i3 = 0;
        int i4 = 1;
        if (i2 >= 16384 - this.count) {
            flushBuffer();
            if (i2 > 16384) {
                int i5 = length / CHARS_MAX;
                if (length % CHARS_MAX > 0) {
                    i5++;
                }
                int i6 = 0;
                while (i4 <= i5) {
                    int i7 = ((int) ((((long) length) * ((long) i4)) / ((long) i5))) + 0;
                    str.getChars(i6, i7, this.m_inputChars, 0);
                    int i8 = i7 - i6;
                    char c2 = this.m_inputChars[i8 - 1];
                    if (c2 >= 55296 && c2 <= 56319) {
                        i7--;
                        i8--;
                    }
                    write(this.m_inputChars, 0, i8);
                    i4++;
                    i6 = i7;
                }
                return;
            }
        }
        str.getChars(0, length, this.m_inputChars, 0);
        char[] cArr = this.m_inputChars;
        byte[] bArr = this.m_outputBytes;
        int i9 = this.count;
        while (i3 < length && (c = cArr[i3]) < 128) {
            bArr[i9] = (byte) c;
            i3++;
            i9++;
        }
        while (i3 < length) {
            char c3 = cArr[i3];
            if (c3 < 128) {
                i = i9 + 1;
                bArr[i9] = (byte) c3;
            } else {
                if (c3 < 2048) {
                    int i10 = i9 + 1;
                    bArr[i9] = (byte) (192 + (c3 >> 6));
                    i9 = i10 + 1;
                    bArr[i10] = (byte) ((c3 & '?') + 128);
                } else if (c3 >= 55296 && c3 <= 56319) {
                    i3++;
                    char c4 = cArr[i3];
                    int i11 = i9 + 1;
                    int i12 = c3 + '@';
                    bArr[i9] = (byte) (((i12 >> 8) & 240) | 240);
                    int i13 = i11 + 1;
                    bArr[i11] = (byte) (((i12 >> 2) & 63) | 128);
                    int i14 = i13 + 1;
                    bArr[i13] = (byte) (128 | (((c4 >> 6) & 15) + ((c3 << 4) & 48)));
                    i9 = i14 + 1;
                    bArr[i14] = (byte) ((c4 & '?') | 128);
                } else {
                    int i15 = i9 + 1;
                    bArr[i9] = (byte) (224 + (c3 >> '\f'));
                    int i16 = i15 + 1;
                    bArr[i15] = (byte) (((c3 >> 6) & 63) + 128);
                    i = i16 + 1;
                    bArr[i16] = (byte) ((c3 & '?') + 128);
                }
                i3++;
            }
            i9 = i;
            i3++;
        }
        this.count = i9;
    }

    public void flushBuffer() throws IOException {
        if (this.count > 0) {
            this.m_os.write(this.m_outputBytes, 0, this.count);
            this.count = 0;
        }
    }

    @Override
    public void flush() throws IOException {
        flushBuffer();
        this.m_os.flush();
    }

    @Override
    public void close() throws IOException {
        flushBuffer();
        this.m_os.close();
    }

    @Override
    public OutputStream getOutputStream() {
        return this.m_os;
    }

    @Override
    public Writer getWriter() {
        return null;
    }
}
