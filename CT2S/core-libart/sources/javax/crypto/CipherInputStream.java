package javax.crypto;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import libcore.io.Streams;

public class CipherInputStream extends FilterInputStream {
    private final Cipher cipher;
    private boolean finished;
    private final byte[] inputBuffer;
    private byte[] outputBuffer;
    private int outputIndex;
    private int outputLength;

    public CipherInputStream(InputStream is, Cipher c) {
        super(is);
        this.cipher = c;
        int blockSize = Math.max(c.getBlockSize(), 1);
        int bufferSize = Math.max(blockSize, (8192 / blockSize) * blockSize);
        this.inputBuffer = new byte[bufferSize];
        this.outputBuffer = new byte[(blockSize > 1 ? blockSize * 2 : 0) + bufferSize];
    }

    protected CipherInputStream(InputStream is) {
        this(is, new NullCipher());
    }

    private boolean fillBuffer() throws IOException {
        if (this.finished) {
            return false;
        }
        this.outputIndex = 0;
        this.outputLength = 0;
        while (this.outputLength == 0) {
            int outputSize = this.cipher.getOutputSize(this.inputBuffer.length);
            if (this.outputBuffer == null || this.outputBuffer.length < outputSize) {
                this.outputBuffer = new byte[outputSize];
            }
            int byteCount = this.in.read(this.inputBuffer);
            if (byteCount == -1) {
                try {
                    this.outputLength = this.cipher.doFinal(this.outputBuffer, 0);
                    this.finished = true;
                    return this.outputLength != 0;
                } catch (Exception e) {
                    throw new IOException("Error while finalizing cipher", e);
                }
            }
            try {
                this.outputLength = this.cipher.update(this.inputBuffer, 0, byteCount, this.outputBuffer, 0);
            } catch (ShortBufferException e2) {
                throw new AssertionError(e2);
            }
        }
        return true;
    }

    @Override
    public int read() throws IOException {
        if (this.in == null) {
            throw new NullPointerException("in == null");
        }
        if (this.outputIndex == this.outputLength && !fillBuffer()) {
            return -1;
        }
        byte[] bArr = this.outputBuffer;
        int i = this.outputIndex;
        this.outputIndex = i + 1;
        return bArr[i] & Character.DIRECTIONALITY_UNDEFINED;
    }

    @Override
    public int read(byte[] buf, int off, int len) throws IOException {
        if (this.in == null) {
            throw new NullPointerException("in == null");
        }
        if (this.outputIndex == this.outputLength && !fillBuffer()) {
            return -1;
        }
        int available = this.outputLength - this.outputIndex;
        if (available < len) {
            len = available;
        }
        if (buf != null) {
            System.arraycopy(this.outputBuffer, this.outputIndex, buf, off, len);
        }
        this.outputIndex += len;
        return len;
    }

    @Override
    public long skip(long byteCount) throws IOException {
        return Streams.skipByReading(this, byteCount);
    }

    @Override
    public int available() throws IOException {
        return this.outputLength - this.outputIndex;
    }

    @Override
    public void close() throws IOException {
        this.in.close();
        try {
            this.cipher.doFinal();
        } catch (GeneralSecurityException e) {
        }
    }

    @Override
    public boolean markSupported() {
        return false;
    }
}
