package mf.org.apache.xerces.impl.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.Locale;
import mf.org.apache.xerces.impl.msg.XMLMessageFormatter;
import mf.org.apache.xerces.util.MessageFormatter;

public final class UTF8Reader extends Reader {
    private static final boolean DEBUG_READ = false;
    public static final int DEFAULT_BUFFER_SIZE = 2048;
    protected final byte[] fBuffer;
    private final MessageFormatter fFormatter;
    protected final InputStream fInputStream;
    private final Locale fLocale;
    protected int fOffset;
    private int fSurrogate;

    public UTF8Reader(InputStream inputStream) {
        this(inputStream, 2048, new XMLMessageFormatter(), Locale.getDefault());
    }

    public UTF8Reader(InputStream inputStream, MessageFormatter messageFormatter, Locale locale) {
        this(inputStream, 2048, messageFormatter, locale);
    }

    public UTF8Reader(InputStream inputStream, int size, MessageFormatter messageFormatter, Locale locale) {
        this(inputStream, new byte[size], messageFormatter, locale);
    }

    public UTF8Reader(InputStream inputStream, byte[] buffer, MessageFormatter messageFormatter, Locale locale) {
        this.fSurrogate = -1;
        this.fInputStream = inputStream;
        this.fBuffer = buffer;
        this.fFormatter = messageFormatter;
        this.fLocale = locale;
    }

    @Override
    public int read() throws IOException {
        int b0;
        int b1;
        int b2;
        int b3;
        int b12;
        int b22;
        int b13;
        int c = this.fSurrogate;
        if (this.fSurrogate != -1) {
            this.fSurrogate = -1;
            return c;
        }
        int index = 0;
        if (0 == this.fOffset) {
            b0 = this.fInputStream.read();
        } else {
            int index2 = 0 + 1;
            int index3 = this.fBuffer[0];
            b0 = index3 & 255;
            index = index2;
        }
        if (b0 == -1) {
            return -1;
        }
        if (b0 < 128) {
            return (char) b0;
        }
        if ((b0 & 224) == 192 && (b0 & 30) != 0) {
            if (index == this.fOffset) {
                b13 = this.fInputStream.read();
            } else {
                int i = index + 1;
                b13 = this.fBuffer[index] & 255;
            }
            if (b13 == -1) {
                expectedByte(2, 2);
            }
            if ((b13 & 192) != 128) {
                invalidByte(2, 2, b13);
            }
            return ((b0 << 6) & 1984) | (b13 & 63);
        }
        if ((b0 & 240) == 224) {
            if (index == this.fOffset) {
                b12 = this.fInputStream.read();
            } else {
                b12 = this.fBuffer[index] & 255;
                index++;
            }
            if (b12 == -1) {
                expectedByte(2, 3);
            }
            if ((b12 & 192) != 128 || ((b0 == 237 && b12 >= 160) || ((b0 & 15) == 0 && (b12 & 32) == 0))) {
                invalidByte(2, 3, b12);
            }
            if (index == this.fOffset) {
                b22 = this.fInputStream.read();
            } else {
                int i2 = index + 1;
                b22 = this.fBuffer[index] & 255;
            }
            if (b22 == -1) {
                expectedByte(3, 3);
            }
            if ((b22 & 192) != 128) {
                invalidByte(3, 3, b22);
            }
            return ((b0 << 12) & 61440) | ((b12 << 6) & 4032) | (b22 & 63);
        }
        if ((b0 & 248) != 240) {
            invalidByte(1, 1, b0);
            return c;
        }
        if (index == this.fOffset) {
            b1 = this.fInputStream.read();
        } else {
            b1 = this.fBuffer[index] & 255;
            index++;
        }
        if (b1 == -1) {
            expectedByte(2, 4);
        }
        if ((b1 & 192) != 128 || ((b1 & 48) == 0 && (b0 & 7) == 0)) {
            invalidByte(2, 3, b1);
        }
        if (index == this.fOffset) {
            b2 = this.fInputStream.read();
        } else {
            b2 = this.fBuffer[index] & 255;
            index++;
        }
        if (b2 == -1) {
            expectedByte(3, 4);
        }
        if ((b2 & 192) != 128) {
            invalidByte(3, 3, b2);
        }
        if (index == this.fOffset) {
            b3 = this.fInputStream.read();
        } else {
            int i3 = index + 1;
            b3 = this.fBuffer[index] & 255;
        }
        if (b3 == -1) {
            expectedByte(4, 4);
        }
        if ((b3 & 192) != 128) {
            invalidByte(4, 4, b3);
        }
        int uuuuu = ((b0 << 2) & 28) | ((b1 >> 4) & 3);
        if (uuuuu > 16) {
            invalidSurrogate(uuuuu);
        }
        int wwww = uuuuu - 1;
        int hs = 55296 | ((wwww << 6) & 960) | ((b1 << 2) & 60) | (3 & (b2 >> 4));
        int ls = 56320 | ((b2 << 6) & 960) | (b3 & 63);
        this.fSurrogate = ls;
        return hs;
    }

    @Override
    public int read(char[] ch, int offset, int length) throws IOException {
        int count;
        int length2;
        int total;
        int b1;
        char c;
        int b12;
        int b2;
        int b3;
        int b13;
        int b22;
        int b14;
        int out = offset;
        int i = -1;
        char c2 = 0;
        if (this.fOffset == 0) {
            length2 = length > this.fBuffer.length ? this.fBuffer.length : length;
            if (this.fSurrogate != -1) {
                ch[out] = (char) this.fSurrogate;
                this.fSurrogate = -1;
                length2--;
                out++;
            }
            int count2 = this.fInputStream.read(this.fBuffer, 0, length2);
            if (count2 == -1) {
                return -1;
            }
            count = count2 + (out - offset);
        } else {
            count = this.fOffset;
            this.fOffset = 0;
            length2 = length;
        }
        int total2 = count;
        int in = 0;
        while (in < total2) {
            byte byte1 = this.fBuffer[in];
            if (byte1 < 0) {
                break;
            }
            ch[out] = (char) byte1;
            in++;
            out++;
        }
        while (in < total2) {
            byte byte12 = this.fBuffer[in];
            if (byte12 >= 0) {
                ch[out] = (char) byte12;
                total = total2;
                out++;
            } else {
                int b0 = byte12 & 255;
                if ((b0 & 224) != 192 || (b0 & 30) == 0) {
                    int c3 = b0 & 240;
                    if (c3 == 224) {
                        int in2 = in + 1;
                        if (in2 < total2) {
                            b13 = this.fBuffer[in2] & 255;
                        } else {
                            b13 = this.fInputStream.read();
                            if (b13 == -1) {
                                if (out > offset) {
                                    this.fBuffer[c2] = (byte) b0;
                                    this.fOffset = 1;
                                    return out - offset;
                                }
                                expectedByte(2, 3);
                            }
                            count++;
                        }
                        if ((b13 & 192) != 128 || ((b0 == 237 && b13 >= 160) || ((b0 & 15) == 0 && (b13 & 32) == 0))) {
                            if (out > offset) {
                                this.fBuffer[c2] = (byte) b0;
                                this.fBuffer[1] = (byte) b13;
                                this.fOffset = 2;
                                return out - offset;
                            }
                            invalidByte(2, 3, b13);
                        }
                        in = in2 + 1;
                        if (in < total2) {
                            b22 = this.fBuffer[in] & 255;
                        } else {
                            b22 = this.fInputStream.read();
                            if (b22 == -1) {
                                if (out > offset) {
                                    this.fBuffer[c2] = (byte) b0;
                                    this.fBuffer[1] = (byte) b13;
                                    this.fOffset = 2;
                                    return out - offset;
                                }
                                expectedByte(3, 3);
                            }
                            count++;
                        }
                        if ((b22 & 192) != 128) {
                            if (out > offset) {
                                this.fBuffer[c2] = (byte) b0;
                                this.fBuffer[1] = (byte) b13;
                                this.fBuffer[2] = (byte) b22;
                                this.fOffset = 3;
                                return out - offset;
                            }
                            invalidByte(3, 3, b22);
                        }
                        int c4 = ((b0 << 12) & 61440) | ((b13 << 6) & 4032) | (b22 & 63);
                        ch[out] = (char) c4;
                        count -= 2;
                        total = total2;
                        out++;
                    } else if ((b0 & 248) == 240) {
                        int in3 = in + 1;
                        if (in3 < total2) {
                            b12 = this.fBuffer[in3] & 255;
                        } else {
                            b12 = this.fInputStream.read();
                            if (b12 == -1) {
                                if (out > offset) {
                                    this.fBuffer[0] = (byte) b0;
                                    this.fOffset = 1;
                                    return out - offset;
                                }
                                expectedByte(2, 4);
                            }
                            count++;
                        }
                        if ((b12 & 192) != 128 || ((b12 & 48) == 0 && (b0 & 7) == 0)) {
                            if (out > offset) {
                                this.fBuffer[0] = (byte) b0;
                                this.fBuffer[1] = (byte) b12;
                                this.fOffset = 2;
                                return out - offset;
                            }
                            invalidByte(2, 4, b12);
                        }
                        int in4 = in3 + 1;
                        if (in4 < total2) {
                            b2 = this.fBuffer[in4] & 255;
                        } else {
                            b2 = this.fInputStream.read();
                            if (b2 == -1) {
                                if (out > offset) {
                                    this.fBuffer[0] = (byte) b0;
                                    this.fBuffer[1] = (byte) b12;
                                    this.fOffset = 2;
                                    return out - offset;
                                }
                                expectedByte(3, 4);
                            }
                            count++;
                        }
                        if ((b2 & 192) != 128) {
                            if (out > offset) {
                                this.fBuffer[0] = (byte) b0;
                                this.fBuffer[1] = (byte) b12;
                                this.fBuffer[2] = (byte) b2;
                                this.fOffset = 3;
                                return out - offset;
                            }
                            invalidByte(3, 4, b2);
                        }
                        in = in4 + 1;
                        if (in < total2) {
                            b3 = this.fBuffer[in] & 255;
                        } else {
                            b3 = this.fInputStream.read();
                            if (b3 == -1) {
                                if (out > offset) {
                                    this.fBuffer[0] = (byte) b0;
                                    this.fBuffer[1] = (byte) b12;
                                    this.fBuffer[2] = (byte) b2;
                                    this.fOffset = 3;
                                    return out - offset;
                                }
                                expectedByte(4, 4);
                            }
                            count++;
                        }
                        if ((b3 & 192) != 128) {
                            if (out > offset) {
                                this.fBuffer[0] = (byte) b0;
                                this.fBuffer[1] = (byte) b12;
                                this.fBuffer[2] = (byte) b2;
                                this.fBuffer[3] = (byte) b3;
                                this.fOffset = 4;
                                return out - offset;
                            }
                            invalidByte(4, 4, b2);
                        }
                        int uuuuu = ((b0 << 2) & 28) | ((b12 >> 4) & 3);
                        if (uuuuu > 16) {
                            invalidSurrogate(uuuuu);
                        }
                        int wwww = uuuuu - 1;
                        int zzzz = b12 & 15;
                        int yyyyyy = b2 & 63;
                        int xxxxxx = b3 & 63;
                        int b15 = wwww << 6;
                        int hs = 55296 | (b15 & 960) | (zzzz << 2) | (yyyyyy >> 4);
                        int ls = 56320 | ((yyyyyy << 6) & 960) | xxxxxx;
                        int out2 = out + 1;
                        total = total2;
                        ch[out] = (char) hs;
                        count -= 2;
                        if (count <= length2) {
                            out = out2 + 1;
                            ch[out2] = (char) ls;
                        } else {
                            this.fSurrogate = ls;
                            count--;
                            out = out2;
                        }
                    } else {
                        total = total2;
                        if (out > offset) {
                            this.fBuffer[0] = (byte) b0;
                            this.fOffset = 1;
                            return out - offset;
                        }
                        b1 = 1;
                        c = 0;
                        invalidByte(1, 1, b0);
                        in += b1;
                        c2 = c;
                        total2 = total;
                        i = -1;
                    }
                    b1 = 1;
                    c = 0;
                    in += b1;
                    c2 = c;
                    total2 = total;
                    i = -1;
                } else {
                    in++;
                    if (in < total2) {
                        b14 = this.fBuffer[in] & 255;
                    } else {
                        b14 = this.fInputStream.read();
                        if (b14 == i) {
                            if (out > offset) {
                                this.fBuffer[c2] = (byte) b0;
                                this.fOffset = 1;
                                return out - offset;
                            }
                            expectedByte(2, 2);
                        }
                        count++;
                    }
                    if ((b14 & 192) != 128) {
                        if (out > offset) {
                            this.fBuffer[c2] = (byte) b0;
                            this.fBuffer[1] = (byte) b14;
                            this.fOffset = 2;
                            return out - offset;
                        }
                        invalidByte(2, 2, b14);
                    }
                    int c5 = ((b0 << 6) & 1984) | (b14 & 63);
                    ch[out] = (char) c5;
                    count--;
                    total = total2;
                    out++;
                }
            }
            b1 = 1;
            c = c2;
            in += b1;
            c2 = c;
            total2 = total;
            i = -1;
        }
        return count;
    }

    @Override
    public long skip(long n) throws IOException {
        long remaining = n;
        char[] ch = new char[this.fBuffer.length];
        do {
            int length = ((long) ch.length) < remaining ? ch.length : (int) remaining;
            int count = read(ch, 0, length);
            if (count <= 0) {
                break;
            }
            remaining -= (long) count;
        } while (remaining > 0);
        long skipped = n - remaining;
        return skipped;
    }

    @Override
    public boolean ready() throws IOException {
        return false;
    }

    @Override
    public boolean markSupported() {
        return false;
    }

    @Override
    public void mark(int readAheadLimit) throws IOException {
        throw new IOException(this.fFormatter.formatMessage(this.fLocale, "OperationNotSupported", new Object[]{"mark()", "UTF-8"}));
    }

    @Override
    public void reset() throws IOException {
        this.fOffset = 0;
        this.fSurrogate = -1;
    }

    @Override
    public void close() throws IOException {
        this.fInputStream.close();
    }

    private void expectedByte(int position, int count) throws MalformedByteSequenceException {
        throw new MalformedByteSequenceException(this.fFormatter, this.fLocale, "http://www.w3.org/TR/1998/REC-xml-19980210", "ExpectedByte", new Object[]{Integer.toString(position), Integer.toString(count)});
    }

    private void invalidByte(int position, int count, int c) throws MalformedByteSequenceException {
        throw new MalformedByteSequenceException(this.fFormatter, this.fLocale, "http://www.w3.org/TR/1998/REC-xml-19980210", "InvalidByte", new Object[]{Integer.toString(position), Integer.toString(count)});
    }

    private void invalidSurrogate(int uuuuu) throws MalformedByteSequenceException {
        throw new MalformedByteSequenceException(this.fFormatter, this.fLocale, "http://www.w3.org/TR/1998/REC-xml-19980210", "InvalidHighSurrogate", new Object[]{Integer.toHexString(uuuuu)});
    }
}
