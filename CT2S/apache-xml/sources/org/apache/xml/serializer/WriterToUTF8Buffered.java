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

    public WriterToUTF8Buffered(OutputStream out) {
        this.m_os = out;
    }

    @Override
    public void write(int c) throws IOException {
        if (this.count >= 16384) {
            flushBuffer();
        }
        if (c < 128) {
            byte[] bArr = this.m_outputBytes;
            int i = this.count;
            this.count = i + 1;
            bArr[i] = (byte) c;
            return;
        }
        if (c < 2048) {
            byte[] bArr2 = this.m_outputBytes;
            int i2 = this.count;
            this.count = i2 + 1;
            bArr2[i2] = (byte) ((c >> 6) + 192);
            byte[] bArr3 = this.m_outputBytes;
            int i3 = this.count;
            this.count = i3 + 1;
            bArr3[i3] = (byte) ((c & 63) + 128);
            return;
        }
        if (c < 65536) {
            byte[] bArr4 = this.m_outputBytes;
            int i4 = this.count;
            this.count = i4 + 1;
            bArr4[i4] = (byte) ((c >> 12) + 224);
            byte[] bArr5 = this.m_outputBytes;
            int i5 = this.count;
            this.count = i5 + 1;
            bArr5[i5] = (byte) (((c >> 6) & 63) + 128);
            byte[] bArr6 = this.m_outputBytes;
            int i6 = this.count;
            this.count = i6 + 1;
            bArr6[i6] = (byte) ((c & 63) + 128);
            return;
        }
        byte[] bArr7 = this.m_outputBytes;
        int i7 = this.count;
        this.count = i7 + 1;
        bArr7[i7] = (byte) ((c >> 18) + 240);
        byte[] bArr8 = this.m_outputBytes;
        int i8 = this.count;
        this.count = i8 + 1;
        bArr8[i8] = (byte) (((c >> 12) & 63) + 128);
        byte[] bArr9 = this.m_outputBytes;
        int i9 = this.count;
        this.count = i9 + 1;
        bArr9[i9] = (byte) (((c >> 6) & 63) + 128);
        byte[] bArr10 = this.m_outputBytes;
        int i10 = this.count;
        this.count = i10 + 1;
        bArr10[i10] = (byte) ((c & 63) + 128);
    }

    @Override
    public void write(char[] chars, int start, int length) throws IOException {
        int count_loc;
        int count_loc2;
        char c;
        int chunks;
        int lengthx3 = length * 3;
        if (lengthx3 >= 16384 - this.count) {
            flushBuffer();
            if (lengthx3 > 16384) {
                int split = length / CHARS_MAX;
                if (length % CHARS_MAX > 0) {
                    chunks = split + 1;
                } else {
                    chunks = split;
                }
                int end_chunk = start;
                for (int chunk = 1; chunk <= chunks; chunk++) {
                    int start_chunk = end_chunk;
                    end_chunk = start + ((int) ((((long) length) * ((long) chunk)) / ((long) chunks)));
                    char c2 = chars[end_chunk - 1];
                    char c3 = chars[end_chunk - 1];
                    if (c2 >= 55296 && c2 <= 56319) {
                        end_chunk = end_chunk < start + length ? end_chunk + 1 : end_chunk - 1;
                    }
                    int len_chunk = end_chunk - start_chunk;
                    write(chars, start_chunk, len_chunk);
                }
                return;
            }
        }
        int n = length + start;
        byte[] buf_loc = this.m_outputBytes;
        int count_loc3 = this.count;
        int i = start;
        while (true) {
            count_loc = count_loc3;
            if (i >= n || (c = chars[i]) >= 128) {
                break;
            }
            count_loc3 = count_loc + 1;
            buf_loc[count_loc] = (byte) c;
            i++;
        }
        while (i < n) {
            char c4 = chars[i];
            if (c4 < 128) {
                count_loc2 = count_loc + 1;
                buf_loc[count_loc] = (byte) c4;
            } else if (c4 < 2048) {
                int count_loc4 = count_loc + 1;
                buf_loc[count_loc] = (byte) ((c4 >> 6) + 192);
                buf_loc[count_loc4] = (byte) ((c4 & '?') + 128);
                count_loc2 = count_loc4 + 1;
            } else if (c4 >= 55296 && c4 <= 56319) {
                i++;
                char low = chars[i];
                int count_loc5 = count_loc + 1;
                buf_loc[count_loc] = (byte) ((((c4 + '@') >> 8) & 240) | 240);
                int count_loc6 = count_loc5 + 1;
                buf_loc[count_loc5] = (byte) ((((c4 + '@') >> 2) & 63) | 128);
                int count_loc7 = count_loc6 + 1;
                buf_loc[count_loc6] = (byte) ((((low >> 6) & 15) + ((c4 << 4) & 48)) | 128);
                buf_loc[count_loc7] = (byte) ((low & '?') | 128);
                count_loc2 = count_loc7 + 1;
            } else {
                int count_loc8 = count_loc + 1;
                buf_loc[count_loc] = (byte) ((c4 >> '\f') + 224);
                int count_loc9 = count_loc8 + 1;
                buf_loc[count_loc8] = (byte) (((c4 >> 6) & 63) + 128);
                count_loc2 = count_loc9 + 1;
                buf_loc[count_loc9] = (byte) ((c4 & '?') + 128);
            }
            i++;
            count_loc = count_loc2;
        }
        this.count = count_loc;
    }

    @Override
    public void write(String s) throws IOException {
        int count_loc;
        int count_loc2;
        char c;
        int chunks;
        int length = s.length();
        int lengthx3 = length * 3;
        if (lengthx3 >= 16384 - this.count) {
            flushBuffer();
            if (lengthx3 > 16384) {
                int split = length / CHARS_MAX;
                if (length % CHARS_MAX > 0) {
                    chunks = split + 1;
                } else {
                    chunks = split;
                }
                int end_chunk = 0;
                for (int chunk = 1; chunk <= chunks; chunk++) {
                    int start_chunk = end_chunk;
                    end_chunk = ((int) ((((long) length) * ((long) chunk)) / ((long) chunks))) + 0;
                    s.getChars(start_chunk, end_chunk, this.m_inputChars, 0);
                    int len_chunk = end_chunk - start_chunk;
                    char c2 = this.m_inputChars[len_chunk - 1];
                    if (c2 >= 55296 && c2 <= 56319) {
                        end_chunk--;
                        len_chunk--;
                        if (chunk == chunks) {
                        }
                    }
                    write(this.m_inputChars, 0, len_chunk);
                }
                return;
            }
        }
        s.getChars(0, length, this.m_inputChars, 0);
        char[] chars = this.m_inputChars;
        byte[] buf_loc = this.m_outputBytes;
        int count_loc3 = this.count;
        int i = 0;
        while (true) {
            count_loc = count_loc3;
            if (i >= length || (c = chars[i]) >= 128) {
                break;
            }
            count_loc3 = count_loc + 1;
            buf_loc[count_loc] = (byte) c;
            i++;
        }
        while (i < length) {
            char c3 = chars[i];
            if (c3 < 128) {
                count_loc2 = count_loc + 1;
                buf_loc[count_loc] = (byte) c3;
            } else if (c3 < 2048) {
                int count_loc4 = count_loc + 1;
                buf_loc[count_loc] = (byte) ((c3 >> 6) + 192);
                buf_loc[count_loc4] = (byte) ((c3 & '?') + 128);
                count_loc2 = count_loc4 + 1;
            } else if (c3 >= 55296 && c3 <= 56319) {
                i++;
                char low = chars[i];
                int count_loc5 = count_loc + 1;
                buf_loc[count_loc] = (byte) ((((c3 + '@') >> 8) & 240) | 240);
                int count_loc6 = count_loc5 + 1;
                buf_loc[count_loc5] = (byte) ((((c3 + '@') >> 2) & 63) | 128);
                int count_loc7 = count_loc6 + 1;
                buf_loc[count_loc6] = (byte) ((((low >> 6) & 15) + ((c3 << 4) & 48)) | 128);
                buf_loc[count_loc7] = (byte) ((low & '?') | 128);
                count_loc2 = count_loc7 + 1;
            } else {
                int count_loc8 = count_loc + 1;
                buf_loc[count_loc] = (byte) ((c3 >> '\f') + 224);
                int count_loc9 = count_loc8 + 1;
                buf_loc[count_loc8] = (byte) (((c3 >> 6) & 63) + 128);
                count_loc2 = count_loc9 + 1;
                buf_loc[count_loc9] = (byte) ((c3 & '?') + 128);
            }
            i++;
            count_loc = count_loc2;
        }
        this.count = count_loc;
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
