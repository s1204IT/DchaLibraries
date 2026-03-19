package com.android.internal.telephony.test;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

class LineReader {
    static final int BUFFER_SIZE = 4096;
    byte[] mBuffer = new byte[4096];
    InputStream mInStream;

    LineReader(InputStream inputStream) {
        this.mInStream = inputStream;
    }

    String getNextLine() {
        return getNextLine(false);
    }

    String getNextLineCtrlZ() {
        return getNextLine(true);
    }

    String getNextLine(boolean z) {
        int i = 0;
        while (true) {
            try {
                try {
                    int i2 = this.mInStream.read();
                    if (i2 >= 0) {
                        if (z && i2 == 26) {
                            break;
                        }
                        if (i2 == 13 || i2 == 10) {
                            if (i == 0) {
                            }
                        } else {
                            int i3 = i + 1;
                            try {
                                this.mBuffer[i] = (byte) i2;
                                i = i3;
                            } catch (IndexOutOfBoundsException e) {
                                i = i3;
                                System.err.println("ATChannel: buffer overflow");
                                try {
                                    return new String(this.mBuffer, 0, i, "US-ASCII");
                                } catch (UnsupportedEncodingException e2) {
                                    System.err.println("ATChannel: implausable UnsupportedEncodingException");
                                    return null;
                                }
                            }
                        }
                    } else {
                        return null;
                    }
                } catch (IOException e3) {
                    return null;
                }
            } catch (IndexOutOfBoundsException e4) {
            }
        }
    }
}
