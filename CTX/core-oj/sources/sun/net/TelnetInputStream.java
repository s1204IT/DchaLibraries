package sun.net;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

public class TelnetInputStream extends FilterInputStream {
    public boolean binaryMode;
    boolean seenCR;
    boolean stickyCRLF;

    public TelnetInputStream(InputStream inputStream, boolean z) {
        super(inputStream);
        this.stickyCRLF = false;
        this.seenCR = false;
        this.binaryMode = false;
        this.binaryMode = z;
    }

    public void setStickyCRLF(boolean z) {
        this.stickyCRLF = z;
    }

    @Override
    public int read() throws IOException {
        if (this.binaryMode) {
            return super.read();
        }
        if (this.seenCR) {
            this.seenCR = false;
            return 10;
        }
        int i = super.read();
        if (i == 13) {
            int i2 = super.read();
            if (i2 == 0) {
                return 13;
            }
            if (i2 != 10) {
                throw new TelnetProtocolException("misplaced CR in input");
            }
            if (!this.stickyCRLF) {
                return 10;
            }
            this.seenCR = true;
            return 13;
        }
        return i;
    }

    @Override
    public int read(byte[] bArr) throws IOException {
        return read(bArr, 0, bArr.length);
    }

    @Override
    public int read(byte[] bArr, int i, int i2) throws IOException {
        int i3;
        if (this.binaryMode) {
            return super.read(bArr, i, i2);
        }
        int i4 = i;
        while (true) {
            i2--;
            if (i2 < 0 || (i3 = read()) == -1) {
                break;
            }
            bArr[i4] = (byte) i3;
            i4++;
        }
        if (i4 > i) {
            return i4 - i;
        }
        return -1;
    }
}
