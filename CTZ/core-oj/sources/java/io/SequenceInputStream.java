package java.io;

import java.util.Enumeration;
import java.util.Vector;

public class SequenceInputStream extends InputStream {
    Enumeration<? extends InputStream> e;
    InputStream in;

    public SequenceInputStream(Enumeration<? extends InputStream> enumeration) {
        this.e = enumeration;
        try {
            nextStream();
        } catch (IOException e) {
            throw new Error("panic");
        }
    }

    public SequenceInputStream(InputStream inputStream, InputStream inputStream2) {
        Vector vector = new Vector(2);
        vector.addElement(inputStream);
        vector.addElement(inputStream2);
        this.e = vector.elements();
        try {
            nextStream();
        } catch (IOException e) {
            throw new Error("panic");
        }
    }

    final void nextStream() throws IOException {
        if (this.in != null) {
            this.in.close();
        }
        if (this.e.hasMoreElements()) {
            this.in = this.e.nextElement();
            if (this.in == null) {
                throw new NullPointerException();
            }
            return;
        }
        this.in = null;
    }

    @Override
    public int available() throws IOException {
        if (this.in == null) {
            return 0;
        }
        return this.in.available();
    }

    @Override
    public int read() throws IOException {
        while (this.in != null) {
            int i = this.in.read();
            if (i != -1) {
                return i;
            }
            nextStream();
        }
        return -1;
    }

    @Override
    public int read(byte[] bArr, int i, int i2) throws IOException {
        if (this.in == null) {
            return -1;
        }
        if (bArr == null) {
            throw new NullPointerException();
        }
        if (i < 0 || i2 < 0 || i2 > bArr.length - i) {
            throw new IndexOutOfBoundsException();
        }
        if (i2 == 0) {
            return 0;
        }
        do {
            int i3 = this.in.read(bArr, i, i2);
            if (i3 > 0) {
                return i3;
            }
            nextStream();
        } while (this.in != null);
        return -1;
    }

    @Override
    public void close() throws IOException {
        do {
            nextStream();
        } while (this.in != null);
    }
}
