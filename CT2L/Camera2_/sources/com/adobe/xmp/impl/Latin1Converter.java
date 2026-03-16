package com.adobe.xmp.impl;

import android.support.v4.view.MotionEventCompat;
import java.io.UnsupportedEncodingException;

public class Latin1Converter {
    private static final int STATE_START = 0;
    private static final int STATE_UTF8CHAR = 11;

    private Latin1Converter() {
    }

    public static ByteBuffer convert(ByteBuffer buffer) {
        if (!"UTF-8".equals(buffer.getEncoding())) {
            return buffer;
        }
        byte[] readAheadBuffer = new byte[8];
        int readAhead = 0;
        int expectedBytes = 0;
        ByteBuffer out = new ByteBuffer((buffer.length() * 4) / 3);
        int state = 0;
        int i = 0;
        while (i < buffer.length()) {
            int b = buffer.charAt(i);
            switch (state) {
                case 11:
                    if (expectedBytes > 0 && (b & 192) == 128) {
                        int readAhead2 = readAhead + 1;
                        readAheadBuffer[readAhead] = (byte) b;
                        expectedBytes--;
                        if (expectedBytes == 0) {
                            out.append(readAheadBuffer, 0, readAhead2);
                            readAhead = 0;
                            state = 0;
                        } else {
                            readAhead = readAhead2;
                        }
                    } else {
                        byte[] utf8 = convertToUTF8(readAheadBuffer[0]);
                        out.append(utf8);
                        i -= readAhead;
                        readAhead = 0;
                        state = 0;
                    }
                    break;
                default:
                    if (b < 127) {
                        out.append((byte) b);
                    } else if (b >= 192) {
                        expectedBytes = -1;
                        for (int test = b; expectedBytes < 8 && (test & 128) == 128; test <<= 1) {
                            expectedBytes++;
                        }
                        readAheadBuffer[readAhead] = (byte) b;
                        state = 11;
                        readAhead++;
                    } else {
                        byte[] utf82 = convertToUTF8((byte) b);
                        out.append(utf82);
                    }
                    break;
            }
            i++;
        }
        if (state == 11) {
            for (int j = 0; j < readAhead; j++) {
                byte[] utf83 = convertToUTF8(readAheadBuffer[j]);
                out.append(utf83);
            }
            return out;
        }
        return out;
    }

    private static byte[] convertToUTF8(byte ch) {
        byte[] bytes;
        int c = ch & MotionEventCompat.ACTION_MASK;
        if (c >= 128) {
            try {
                if (c == 129 || c == 141 || c == 143 || c == 144 || c == 157) {
                    bytes = new byte[]{32};
                } else {
                    bytes = new String(new byte[]{ch}, "cp1252").getBytes("UTF-8");
                }
                return bytes;
            } catch (UnsupportedEncodingException e) {
            }
        }
        return new byte[]{ch};
    }
}
