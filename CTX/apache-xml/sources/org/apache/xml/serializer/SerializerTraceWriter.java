package org.apache.xml.serializer;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;

final class SerializerTraceWriter extends Writer implements WriterChain {
    private byte[] buf;
    private int buf_length;
    private int count;
    private final SerializerTrace m_tracer;
    private final Writer m_writer;

    private void setBufferSize(int i) {
        this.buf = new byte[i + 3];
        this.buf_length = i;
        this.count = 0;
    }

    public SerializerTraceWriter(Writer writer, SerializerTrace serializerTrace) {
        this.m_writer = writer;
        this.m_tracer = serializerTrace;
        setBufferSize(1024);
    }

    private void flushBuffer() throws IOException {
        if (this.count > 0) {
            char[] cArr = new char[this.count];
            for (int i = 0; i < this.count; i++) {
                cArr[i] = (char) this.buf[i];
            }
            if (this.m_tracer != null) {
                this.m_tracer.fireGenerateEvent(12, cArr, 0, cArr.length);
            }
            this.count = 0;
        }
    }

    @Override
    public void flush() throws IOException {
        if (this.m_writer != null) {
            this.m_writer.flush();
        }
        flushBuffer();
    }

    @Override
    public void close() throws IOException {
        if (this.m_writer != null) {
            this.m_writer.close();
        }
        flushBuffer();
    }

    @Override
    public void write(int i) throws IOException {
        if (this.m_writer != null) {
            this.m_writer.write(i);
        }
        if (this.count >= this.buf_length) {
            flushBuffer();
        }
        if (i < 128) {
            byte[] bArr = this.buf;
            int i2 = this.count;
            this.count = i2 + 1;
            bArr[i2] = (byte) i;
            return;
        }
        if (i < 2048) {
            byte[] bArr2 = this.buf;
            int i3 = this.count;
            this.count = i3 + 1;
            bArr2[i3] = (byte) (192 + (i >> 6));
            byte[] bArr3 = this.buf;
            int i4 = this.count;
            this.count = i4 + 1;
            bArr3[i4] = (byte) (128 + (i & 63));
            return;
        }
        byte[] bArr4 = this.buf;
        int i5 = this.count;
        this.count = i5 + 1;
        bArr4[i5] = (byte) (224 + (i >> 12));
        byte[] bArr5 = this.buf;
        int i6 = this.count;
        this.count = i6 + 1;
        bArr5[i6] = (byte) (((i >> 6) & 63) + 128);
        byte[] bArr6 = this.buf;
        int i7 = this.count;
        this.count = i7 + 1;
        bArr6[i7] = (byte) (128 + (i & 63));
    }

    @Override
    public void write(char[] cArr, int i, int i2) throws IOException {
        if (this.m_writer != null) {
            this.m_writer.write(cArr, i, i2);
        }
        int i3 = (i2 << 1) + i2;
        if (i3 >= this.buf_length) {
            flushBuffer();
            setBufferSize(2 * i3);
        }
        if (i3 > this.buf_length - this.count) {
            flushBuffer();
        }
        int i4 = i2 + i;
        while (i < i4) {
            char c = cArr[i];
            if (c < 128) {
                byte[] bArr = this.buf;
                int i5 = this.count;
                this.count = i5 + 1;
                bArr[i5] = (byte) c;
            } else if (c < 2048) {
                byte[] bArr2 = this.buf;
                int i6 = this.count;
                this.count = i6 + 1;
                bArr2[i6] = (byte) (192 + (c >> 6));
                byte[] bArr3 = this.buf;
                int i7 = this.count;
                this.count = i7 + 1;
                bArr3[i7] = (byte) (128 + (c & '?'));
            } else {
                byte[] bArr4 = this.buf;
                int i8 = this.count;
                this.count = i8 + 1;
                bArr4[i8] = (byte) (224 + (c >> '\f'));
                byte[] bArr5 = this.buf;
                int i9 = this.count;
                this.count = i9 + 1;
                bArr5[i9] = (byte) (((c >> 6) & 63) + 128);
                byte[] bArr6 = this.buf;
                int i10 = this.count;
                this.count = i10 + 1;
                bArr6[i10] = (byte) (128 + (c & '?'));
            }
            i++;
        }
    }

    @Override
    public void write(String str) throws IOException {
        if (this.m_writer != null) {
            this.m_writer.write(str);
        }
        int length = str.length();
        int i = (length << 1) + length;
        if (i >= this.buf_length) {
            flushBuffer();
            setBufferSize(2 * i);
        }
        if (i > this.buf_length - this.count) {
            flushBuffer();
        }
        for (int i2 = 0; i2 < length; i2++) {
            char cCharAt = str.charAt(i2);
            if (cCharAt < 128) {
                byte[] bArr = this.buf;
                int i3 = this.count;
                this.count = i3 + 1;
                bArr[i3] = (byte) cCharAt;
            } else if (cCharAt < 2048) {
                byte[] bArr2 = this.buf;
                int i4 = this.count;
                this.count = i4 + 1;
                bArr2[i4] = (byte) (192 + (cCharAt >> 6));
                byte[] bArr3 = this.buf;
                int i5 = this.count;
                this.count = i5 + 1;
                bArr3[i5] = (byte) (128 + (cCharAt & '?'));
            } else {
                byte[] bArr4 = this.buf;
                int i6 = this.count;
                this.count = i6 + 1;
                bArr4[i6] = (byte) (224 + (cCharAt >> '\f'));
                byte[] bArr5 = this.buf;
                int i7 = this.count;
                this.count = i7 + 1;
                bArr5[i7] = (byte) (((cCharAt >> 6) & 63) + 128);
                byte[] bArr6 = this.buf;
                int i8 = this.count;
                this.count = i8 + 1;
                bArr6[i8] = (byte) (128 + (cCharAt & '?'));
            }
        }
    }

    @Override
    public Writer getWriter() {
        return this.m_writer;
    }

    @Override
    public OutputStream getOutputStream() {
        if (this.m_writer instanceof WriterChain) {
            return ((WriterChain) this.m_writer).getOutputStream();
        }
        return null;
    }
}
