package java.io;

import java.util.Arrays;
import java.util.Enumeration;
import java.util.Vector;

public class SequenceInputStream extends InputStream {
    private Enumeration<? extends InputStream> e;
    private InputStream in;

    public SequenceInputStream(InputStream s1, InputStream s2) {
        if (s1 == null) {
            throw new NullPointerException("s1 == null");
        }
        Vector<InputStream> inVector = new Vector<>(1);
        inVector.addElement(s2);
        this.e = inVector.elements();
        this.in = s1;
    }

    public SequenceInputStream(Enumeration<? extends InputStream> e) {
        this.e = e;
        if (e.hasMoreElements()) {
            this.in = e.nextElement();
            if (this.in == null) {
                throw new NullPointerException("element is null");
            }
        }
    }

    @Override
    public int available() throws IOException {
        if (this.e == null || this.in == null) {
            return 0;
        }
        return this.in.available();
    }

    @Override
    public void close() throws IOException {
        while (this.in != null) {
            nextStream();
        }
        this.e = null;
    }

    private void nextStream() throws IOException {
        if (this.in != null) {
            this.in.close();
        }
        if (this.e.hasMoreElements()) {
            this.in = this.e.nextElement();
            if (this.in == null) {
                throw new NullPointerException("element is null");
            }
            return;
        }
        this.in = null;
    }

    @Override
    public int read() throws IOException {
        while (this.in != null) {
            int result = this.in.read();
            if (result < 0) {
                nextStream();
            } else {
                return result;
            }
        }
        return -1;
    }

    @Override
    public int read(byte[] buffer, int byteOffset, int byteCount) throws IOException {
        if (this.in == null) {
            return -1;
        }
        Arrays.checkOffsetAndCount(buffer.length, byteOffset, byteCount);
        while (this.in != null) {
            int result = this.in.read(buffer, byteOffset, byteCount);
            if (result < 0) {
                nextStream();
            } else {
                return result;
            }
        }
        return -1;
    }
}
