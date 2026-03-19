package javax.crypto;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

public class CipherInputStream extends FilterInputStream {
    private Cipher cipher;
    private boolean closed;
    private boolean done;
    private byte[] ibuffer;
    private InputStream input;
    private byte[] obuffer;
    private int ofinish;
    private int ostart;

    private int getMoreData() throws IOException {
        if (this.done) {
            return -1;
        }
        this.ofinish = 0;
        this.ostart = 0;
        int outputSize = this.cipher.getOutputSize(this.ibuffer.length);
        if (this.obuffer == null || outputSize > this.obuffer.length) {
            this.obuffer = new byte[outputSize];
        }
        int i = this.input.read(this.ibuffer);
        if (i == -1) {
            this.done = true;
            try {
                this.ofinish = this.cipher.doFinal(this.obuffer, 0);
            } catch (BadPaddingException | IllegalBlockSizeException e) {
                this.obuffer = null;
                throw new IOException(e);
            } catch (ShortBufferException e2) {
                this.obuffer = null;
                throw new IllegalStateException("ShortBufferException is not expected", e2);
            }
        } else {
            try {
                this.ofinish = this.cipher.update(this.ibuffer, 0, i, this.obuffer, 0);
            } catch (IllegalStateException e3) {
                this.obuffer = null;
                throw e3;
            } catch (ShortBufferException e4) {
                this.obuffer = null;
                throw new IllegalStateException("ShortBufferException is not expected", e4);
            }
        }
        return this.ofinish;
    }

    public CipherInputStream(InputStream inputStream, Cipher cipher) {
        super(inputStream);
        this.ibuffer = new byte[512];
        this.done = false;
        this.ostart = 0;
        this.ofinish = 0;
        this.closed = false;
        this.input = inputStream;
        this.cipher = cipher;
    }

    protected CipherInputStream(InputStream inputStream) {
        super(inputStream);
        this.ibuffer = new byte[512];
        this.done = false;
        this.ostart = 0;
        this.ofinish = 0;
        this.closed = false;
        this.input = inputStream;
        this.cipher = new NullCipher();
    }

    @Override
    public int read() throws IOException {
        if (this.ostart >= this.ofinish) {
            int moreData = 0;
            while (moreData == 0) {
                moreData = getMoreData();
            }
            if (moreData == -1) {
                return -1;
            }
        }
        byte[] bArr = this.obuffer;
        int i = this.ostart;
        this.ostart = i + 1;
        return bArr[i] & Character.DIRECTIONALITY_UNDEFINED;
    }

    @Override
    public int read(byte[] bArr) throws IOException {
        return read(bArr, 0, bArr.length);
    }

    @Override
    public int read(byte[] bArr, int i, int i2) throws IOException {
        if (this.ostart >= this.ofinish) {
            int moreData = 0;
            while (moreData == 0) {
                moreData = getMoreData();
            }
            if (moreData == -1) {
                return -1;
            }
        }
        if (i2 <= 0) {
            return 0;
        }
        int i3 = this.ofinish - this.ostart;
        if (i2 >= i3) {
            i2 = i3;
        }
        if (bArr != null) {
            System.arraycopy(this.obuffer, this.ostart, bArr, i, i2);
        }
        this.ostart += i2;
        return i2;
    }

    @Override
    public long skip(long j) throws IOException {
        long j2 = this.ofinish - this.ostart;
        if (j > j2) {
            j = j2;
        }
        if (j < 0) {
            return 0L;
        }
        this.ostart = (int) (((long) this.ostart) + j);
        return j;
    }

    @Override
    public int available() throws IOException {
        return this.ofinish - this.ostart;
    }

    @Override
    public void close() throws IOException {
        if (this.closed) {
            return;
        }
        this.closed = true;
        this.input.close();
        if (!this.done) {
            try {
                this.cipher.doFinal();
            } catch (BadPaddingException | IllegalBlockSizeException e) {
                if (e instanceof AEADBadTagException) {
                    throw new IOException(e);
                }
            }
        }
        this.ostart = 0;
        this.ofinish = 0;
    }

    @Override
    public boolean markSupported() {
        return false;
    }
}
