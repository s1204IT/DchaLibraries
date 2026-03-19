package org.apache.james.mime4j.decoder;

import java.io.IOException;
import java.io.InputStream;

public class QuotedPrintableInputStream extends InputStream {
    ByteQueue byteq = new ByteQueue();
    ByteQueue pushbackq = new ByteQueue();
    private byte state = 0;
    private InputStream stream;

    public QuotedPrintableInputStream(InputStream inputStream) {
        this.stream = inputStream;
    }

    @Override
    public void close() throws IOException {
        this.stream.close();
    }

    @Override
    public int read() throws IOException {
        fillBuffer();
        if (this.byteq.count() == 0) {
            return -1;
        }
        byte bDequeue = this.byteq.dequeue();
        if (bDequeue >= 0) {
            return bDequeue;
        }
        return bDequeue & 255;
    }

    private void populatePushbackQueue() throws IOException {
        int i;
        if (this.pushbackq.count() != 0) {
            return;
        }
        while (true) {
            i = this.stream.read();
            if (i == -1) {
                this.pushbackq.clear();
                return;
            }
            if (i != 13) {
                if (i != 32) {
                    switch (i) {
                        case 9:
                            break;
                        case 10:
                            break;
                        default:
                            this.pushbackq.enqueue((byte) i);
                            break;
                    }
                    return;
                }
                this.pushbackq.enqueue((byte) i);
            }
        }
        this.pushbackq.clear();
        this.pushbackq.enqueue((byte) i);
    }

    private void fillBuffer() throws IOException {
        byte b = 0;
        while (this.byteq.count() == 0) {
            if (this.pushbackq.count() == 0) {
                populatePushbackQueue();
                if (this.pushbackq.count() == 0) {
                    return;
                }
            }
            byte bDequeue = this.pushbackq.dequeue();
            switch (this.state) {
                case 0:
                    if (bDequeue != 61) {
                        this.byteq.enqueue(bDequeue);
                    } else {
                        this.state = (byte) 1;
                    }
                    break;
                case 1:
                    if (bDequeue == 13) {
                        this.state = (byte) 2;
                    } else if ((bDequeue >= 48 && bDequeue <= 57) || ((bDequeue >= 65 && bDequeue <= 70) || (bDequeue >= 97 && bDequeue <= 102))) {
                        this.state = (byte) 3;
                        b = bDequeue;
                    } else if (bDequeue == 61) {
                        this.byteq.enqueue((byte) 61);
                    } else {
                        this.state = (byte) 0;
                        this.byteq.enqueue((byte) 61);
                        this.byteq.enqueue(bDequeue);
                    }
                    break;
                case 2:
                    if (bDequeue == 10) {
                        this.state = (byte) 0;
                    } else {
                        this.state = (byte) 0;
                        this.byteq.enqueue((byte) 61);
                        this.byteq.enqueue((byte) 13);
                        this.byteq.enqueue(bDequeue);
                    }
                    break;
                case 3:
                    if ((bDequeue >= 48 && bDequeue <= 57) || ((bDequeue >= 65 && bDequeue <= 70) || (bDequeue >= 97 && bDequeue <= 102))) {
                        byte bAsciiCharToNumericValue = asciiCharToNumericValue(b);
                        byte bAsciiCharToNumericValue2 = asciiCharToNumericValue(bDequeue);
                        this.state = (byte) 0;
                        this.byteq.enqueue((byte) (bAsciiCharToNumericValue2 | (bAsciiCharToNumericValue << 4)));
                    } else {
                        this.state = (byte) 0;
                        this.byteq.enqueue((byte) 61);
                        this.byteq.enqueue(b);
                        this.byteq.enqueue(bDequeue);
                    }
                    break;
                default:
                    this.state = (byte) 0;
                    this.byteq.enqueue(bDequeue);
                    break;
            }
        }
    }

    private byte asciiCharToNumericValue(byte b) {
        if (b >= 48 && b <= 57) {
            return (byte) (b - 48);
        }
        if (b >= 65 && b <= 90) {
            return (byte) (10 + (b - 65));
        }
        if (b >= 97 && b <= 122) {
            return (byte) (10 + (b - 97));
        }
        throw new IllegalArgumentException(((char) b) + " is not a hexadecimal digit");
    }
}
