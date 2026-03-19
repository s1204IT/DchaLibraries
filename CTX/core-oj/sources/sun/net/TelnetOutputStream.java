package sun.net;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class TelnetOutputStream extends BufferedOutputStream {
    public boolean binaryMode;
    boolean seenCR;
    boolean stickyCRLF;

    public TelnetOutputStream(OutputStream outputStream, boolean z) {
        super(outputStream);
        this.stickyCRLF = false;
        this.seenCR = false;
        this.binaryMode = false;
        this.binaryMode = z;
    }

    public void setStickyCRLF(boolean z) {
        this.stickyCRLF = z;
    }

    @Override
    public void write(int i) throws IOException {
        if (this.binaryMode) {
            super.write(i);
            return;
        }
        if (this.seenCR) {
            if (i != 10) {
                super.write(0);
            }
            super.write(i);
            if (i != 13) {
                this.seenCR = false;
                return;
            }
            return;
        }
        if (i == 10) {
            super.write(13);
            super.write(10);
            return;
        }
        if (i == 13) {
            if (this.stickyCRLF) {
                this.seenCR = true;
            } else {
                super.write(13);
                i = 0;
            }
        }
        super.write(i);
    }

    @Override
    public void write(byte[] bArr, int i, int i2) throws IOException {
        if (this.binaryMode) {
            super.write(bArr, i, i2);
            return;
        }
        while (true) {
            i2--;
            if (i2 >= 0) {
                write(bArr[i]);
                i++;
            } else {
                return;
            }
        }
    }
}
