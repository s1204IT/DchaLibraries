package javax.crypto;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class CipherOutputStream extends FilterOutputStream {
    private Cipher cipher;
    private boolean closed;
    private byte[] ibuffer;
    private byte[] obuffer;
    private OutputStream output;

    public CipherOutputStream(OutputStream outputStream, Cipher cipher) {
        super(outputStream);
        this.ibuffer = new byte[1];
        this.closed = false;
        this.output = outputStream;
        this.cipher = cipher;
    }

    protected CipherOutputStream(OutputStream outputStream) {
        super(outputStream);
        this.ibuffer = new byte[1];
        this.closed = false;
        this.output = outputStream;
        this.cipher = new NullCipher();
    }

    @Override
    public void write(int i) throws IOException {
        this.ibuffer[0] = (byte) i;
        this.obuffer = this.cipher.update(this.ibuffer, 0, 1);
        if (this.obuffer != null) {
            this.output.write(this.obuffer);
            this.obuffer = null;
        }
    }

    @Override
    public void write(byte[] bArr) throws IOException {
        write(bArr, 0, bArr.length);
    }

    @Override
    public void write(byte[] bArr, int i, int i2) throws IOException {
        this.obuffer = this.cipher.update(bArr, i, i2);
        if (this.obuffer != null) {
            this.output.write(this.obuffer);
            this.obuffer = null;
        }
    }

    @Override
    public void flush() throws IOException {
        if (this.obuffer != null) {
            this.output.write(this.obuffer);
            this.obuffer = null;
        }
        this.output.flush();
    }

    @Override
    public void close() throws IOException {
        if (this.closed) {
            return;
        }
        this.closed = true;
        try {
            this.obuffer = this.cipher.doFinal();
            try {
                flush();
            } catch (IOException e) {
            }
            this.out.close();
        } catch (BadPaddingException | IllegalBlockSizeException e2) {
            this.obuffer = null;
            throw new IOException(e2);
        }
    }
}
