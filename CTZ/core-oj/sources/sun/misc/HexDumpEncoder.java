package sun.misc;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

public class HexDumpEncoder extends CharacterEncoder {
    private int currentByte;
    private int offset;
    private byte[] thisLine = new byte[16];
    private int thisLineLength;

    static void hexDigit(PrintStream printStream, byte b) {
        char c;
        char c2;
        char c3 = (char) ((b >> 4) & 15);
        if (c3 > '\t') {
            c = (char) ((c3 - '\n') + 65);
        } else {
            c = (char) (c3 + '0');
        }
        printStream.write(c);
        char c4 = (char) (b & 15);
        if (c4 > '\t') {
            c2 = (char) ((c4 - '\n') + 65);
        } else {
            c2 = (char) (c4 + '0');
        }
        printStream.write(c2);
    }

    @Override
    protected int bytesPerAtom() {
        return 1;
    }

    @Override
    protected int bytesPerLine() {
        return 16;
    }

    @Override
    protected void encodeBufferPrefix(OutputStream outputStream) throws IOException {
        this.offset = 0;
        super.encodeBufferPrefix(outputStream);
    }

    @Override
    protected void encodeLinePrefix(OutputStream outputStream, int i) throws IOException {
        hexDigit(this.pStream, (byte) ((this.offset >>> 8) & 255));
        hexDigit(this.pStream, (byte) (this.offset & 255));
        this.pStream.print(": ");
        this.currentByte = 0;
        this.thisLineLength = i;
    }

    @Override
    protected void encodeAtom(OutputStream outputStream, byte[] bArr, int i, int i2) throws IOException {
        this.thisLine[this.currentByte] = bArr[i];
        hexDigit(this.pStream, bArr[i]);
        this.pStream.print(" ");
        this.currentByte++;
        if (this.currentByte == 8) {
            this.pStream.print("  ");
        }
    }

    @Override
    protected void encodeLineSuffix(OutputStream outputStream) throws IOException {
        if (this.thisLineLength < 16) {
            for (int i = this.thisLineLength; i < 16; i++) {
                this.pStream.print("   ");
                if (i == 7) {
                    this.pStream.print("  ");
                }
            }
        }
        this.pStream.print(" ");
        for (int i2 = 0; i2 < this.thisLineLength; i2++) {
            if (this.thisLine[i2] < 32 || this.thisLine[i2] > 122) {
                this.pStream.print(".");
            } else {
                this.pStream.write(this.thisLine[i2]);
            }
        }
        this.pStream.println();
        this.offset += this.thisLineLength;
    }
}
