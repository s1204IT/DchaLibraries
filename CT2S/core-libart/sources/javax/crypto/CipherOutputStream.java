package javax.crypto;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import libcore.io.Streams;

public class CipherOutputStream extends FilterOutputStream {
    private final Cipher cipher;

    public CipherOutputStream(OutputStream os, Cipher c) {
        super(os);
        this.cipher = c;
    }

    protected CipherOutputStream(OutputStream os) {
        this(os, new NullCipher());
    }

    @Override
    public void write(int b) throws IOException {
        Streams.writeSingleByte(this, b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        byte[] result;
        if (len != 0 && (result = this.cipher.update(b, off, len)) != null) {
            this.out.write(result);
        }
    }

    @Override
    public void flush() throws IOException {
        this.out.flush();
    }

    @Override
    public void close() throws IOException {
        byte[] result;
        try {
            try {
                if (this.cipher != null && (result = this.cipher.doFinal()) != null) {
                    this.out.write(result);
                }
                if (this.out != null) {
                    this.out.flush();
                }
            } catch (BadPaddingException e) {
                throw new IOException(e.getMessage());
            } catch (IllegalBlockSizeException e2) {
                throw new IOException(e2.getMessage());
            }
        } finally {
            if (this.out != null) {
                this.out.close();
            }
        }
    }
}
