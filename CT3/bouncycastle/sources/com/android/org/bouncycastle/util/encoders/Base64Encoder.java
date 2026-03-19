package com.android.org.bouncycastle.util.encoders;

import java.io.IOException;
import java.io.OutputStream;

public class Base64Encoder implements Encoder {
    protected final byte[] encodingTable = {65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79, 80, 81, 82, 83, 84, 85, 86, 87, 88, 89, 90, 97, 98, 99, 100, 101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111, 112, 113, 114, 115, 116, 117, 118, 119, 120, 121, 122, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 43, 47};
    protected byte padding = 61;
    protected final byte[] decodingTable = new byte[128];

    protected void initialiseDecodingTable() {
        for (int i = 0; i < this.decodingTable.length; i++) {
            this.decodingTable[i] = -1;
        }
        for (int i2 = 0; i2 < this.encodingTable.length; i2++) {
            this.decodingTable[this.encodingTable[i2]] = (byte) i2;
        }
    }

    public Base64Encoder() {
        initialiseDecodingTable();
    }

    @Override
    public int encode(byte[] data, int off, int length, OutputStream out) throws IOException {
        int modulus = length % 3;
        int dataLength = length - modulus;
        for (int i = off; i < off + dataLength; i += 3) {
            int a1 = data[i] & 255;
            int a2 = data[i + 1] & 255;
            int a3 = data[i + 2] & 255;
            out.write(this.encodingTable[(a1 >>> 2) & 63]);
            out.write(this.encodingTable[((a1 << 4) | (a2 >>> 4)) & 63]);
            out.write(this.encodingTable[((a2 << 2) | (a3 >>> 6)) & 63]);
            out.write(this.encodingTable[a3 & 63]);
        }
        switch (modulus) {
            case 1:
                int d1 = data[off + dataLength] & 255;
                int b1 = (d1 >>> 2) & 63;
                int b2 = (d1 << 4) & 63;
                out.write(this.encodingTable[b1]);
                out.write(this.encodingTable[b2]);
                out.write(this.padding);
                out.write(this.padding);
                break;
            case 2:
                int d12 = data[off + dataLength] & 255;
                int d2 = data[off + dataLength + 1] & 255;
                int b12 = (d12 >>> 2) & 63;
                int b22 = ((d12 << 4) | (d2 >>> 4)) & 63;
                int b3 = (d2 << 2) & 63;
                out.write(this.encodingTable[b12]);
                out.write(this.encodingTable[b22]);
                out.write(this.encodingTable[b3]);
                out.write(this.padding);
                break;
        }
        return (modulus == 0 ? 0 : 4) + ((dataLength / 3) * 4);
    }

    private boolean ignore(char c) {
        return c == '\n' || c == '\r' || c == '\t' || c == ' ';
    }

    @Override
    public int decode(byte[] data, int off, int length, OutputStream out) throws IOException {
        int outLen = 0;
        int end = off + length;
        while (end > off && ignore((char) data[end - 1])) {
            end--;
        }
        int finish = end - 4;
        int i = nextI(data, off, finish);
        while (true) {
            int i2 = i;
            if (i2 < finish) {
                byte b1 = this.decodingTable[data[i2]];
                int i3 = nextI(data, i2 + 1, finish);
                byte b2 = this.decodingTable[data[i3]];
                int i4 = nextI(data, i3 + 1, finish);
                byte b3 = this.decodingTable[data[i4]];
                int i5 = nextI(data, i4 + 1, finish);
                int i6 = i5 + 1;
                byte b4 = this.decodingTable[data[i5]];
                if ((b1 | b2 | b3 | b4) < 0) {
                    throw new IOException("invalid characters encountered in base64 data");
                }
                out.write((b1 << 2) | (b2 >> 4));
                out.write((b2 << 4) | (b3 >> 2));
                out.write((b3 << 6) | b4);
                outLen += 3;
                i = nextI(data, i6, finish);
            } else {
                return outLen + decodeLastBlock(out, (char) data[end - 4], (char) data[end - 3], (char) data[end - 2], (char) data[end - 1]);
            }
        }
    }

    private int nextI(byte[] data, int i, int finish) {
        while (i < finish && ignore((char) data[i])) {
            i++;
        }
        return i;
    }

    @Override
    public int decode(String data, OutputStream out) throws IOException {
        int length = 0;
        int end = data.length();
        while (end > 0 && ignore(data.charAt(end - 1))) {
            end--;
        }
        int finish = end - 4;
        int i = nextI(data, 0, finish);
        int i2 = i;
        while (i2 < finish) {
            byte b1 = this.decodingTable[data.charAt(i2)];
            int i3 = nextI(data, i2 + 1, finish);
            byte b2 = this.decodingTable[data.charAt(i3)];
            int i4 = nextI(data, i3 + 1, finish);
            byte b3 = this.decodingTable[data.charAt(i4)];
            int i5 = nextI(data, i4 + 1, finish);
            int i6 = i5 + 1;
            byte b4 = this.decodingTable[data.charAt(i5)];
            if ((b1 | b2 | b3 | b4) < 0) {
                throw new IOException("invalid characters encountered in base64 data");
            }
            out.write((b1 << 2) | (b2 >> 4));
            out.write((b2 << 4) | (b3 >> 2));
            out.write((b3 << 6) | b4);
            length += 3;
            i2 = nextI(data, i6, finish);
        }
        return length + decodeLastBlock(out, data.charAt(end - 4), data.charAt(end - 3), data.charAt(end - 2), data.charAt(end - 1));
    }

    private int decodeLastBlock(OutputStream out, char c1, char c2, char c3, char c4) throws IOException {
        if (c3 == this.padding) {
            byte b1 = this.decodingTable[c1];
            byte b2 = this.decodingTable[c2];
            if ((b1 | b2) < 0) {
                throw new IOException("invalid characters encountered at end of base64 data");
            }
            out.write((b1 << 2) | (b2 >> 4));
            return 1;
        }
        if (c4 == this.padding) {
            byte b12 = this.decodingTable[c1];
            byte b22 = this.decodingTable[c2];
            byte b3 = this.decodingTable[c3];
            if ((b12 | b22 | b3) < 0) {
                throw new IOException("invalid characters encountered at end of base64 data");
            }
            out.write((b12 << 2) | (b22 >> 4));
            out.write((b22 << 4) | (b3 >> 2));
            return 2;
        }
        byte b13 = this.decodingTable[c1];
        byte b23 = this.decodingTable[c2];
        byte b32 = this.decodingTable[c3];
        byte b4 = this.decodingTable[c4];
        if ((b13 | b23 | b32 | b4) < 0) {
            throw new IOException("invalid characters encountered at end of base64 data");
        }
        out.write((b13 << 2) | (b23 >> 4));
        out.write((b23 << 4) | (b32 >> 2));
        out.write((b32 << 6) | b4);
        return 3;
    }

    private int nextI(String data, int i, int finish) {
        while (i < finish && ignore(data.charAt(i))) {
            i++;
        }
        return i;
    }
}
