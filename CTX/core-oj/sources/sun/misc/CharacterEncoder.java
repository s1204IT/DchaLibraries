package sun.misc;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.ByteBuffer;

public abstract class CharacterEncoder {
    protected PrintStream pStream;

    protected abstract int bytesPerAtom();

    protected abstract int bytesPerLine();

    protected abstract void encodeAtom(OutputStream outputStream, byte[] bArr, int i, int i2) throws IOException;

    protected void encodeBufferPrefix(OutputStream outputStream) throws IOException {
        this.pStream = new PrintStream(outputStream);
    }

    protected void encodeBufferSuffix(OutputStream outputStream) throws IOException {
    }

    protected void encodeLinePrefix(OutputStream outputStream, int i) throws IOException {
    }

    protected void encodeLineSuffix(OutputStream outputStream) throws IOException {
        this.pStream.println();
    }

    protected int readFully(InputStream inputStream, byte[] bArr) throws IOException {
        for (int i = 0; i < bArr.length; i++) {
            int i2 = inputStream.read();
            if (i2 == -1) {
                return i;
            }
            bArr[i] = (byte) i2;
        }
        return bArr.length;
    }

    public void encode(InputStream inputStream, OutputStream outputStream) throws IOException {
        byte[] bArr = new byte[bytesPerLine()];
        encodeBufferPrefix(outputStream);
        while (true) {
            int fully = readFully(inputStream, bArr);
            if (fully == 0) {
                break;
            }
            encodeLinePrefix(outputStream, fully);
            int iBytesPerAtom = 0;
            while (iBytesPerAtom < fully) {
                if (bytesPerAtom() + iBytesPerAtom <= fully) {
                    encodeAtom(outputStream, bArr, iBytesPerAtom, bytesPerAtom());
                } else {
                    encodeAtom(outputStream, bArr, iBytesPerAtom, fully - iBytesPerAtom);
                }
                iBytesPerAtom += bytesPerAtom();
            }
            if (fully < bytesPerLine()) {
                break;
            } else {
                encodeLineSuffix(outputStream);
            }
        }
        encodeBufferSuffix(outputStream);
    }

    public void encode(byte[] bArr, OutputStream outputStream) throws IOException {
        encode(new ByteArrayInputStream(bArr), outputStream);
    }

    public String encode(byte[] bArr) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try {
            encode(new ByteArrayInputStream(bArr), byteArrayOutputStream);
            return byteArrayOutputStream.toString("8859_1");
        } catch (Exception e) {
            throw new Error("CharacterEncoder.encode internal error");
        }
    }

    private byte[] getBytes(ByteBuffer byteBuffer) {
        byte[] bArrArray;
        if (byteBuffer.hasArray()) {
            bArrArray = byteBuffer.array();
            if (bArrArray.length == byteBuffer.capacity() && bArrArray.length == byteBuffer.remaining()) {
                byteBuffer.position(byteBuffer.limit());
            } else {
                bArrArray = null;
            }
        }
        if (bArrArray == null) {
            byte[] bArr = new byte[byteBuffer.remaining()];
            byteBuffer.get(bArr);
            return bArr;
        }
        return bArrArray;
    }

    public void encode(ByteBuffer byteBuffer, OutputStream outputStream) throws IOException {
        encode(getBytes(byteBuffer), outputStream);
    }

    public String encode(ByteBuffer byteBuffer) {
        return encode(getBytes(byteBuffer));
    }

    public void encodeBuffer(InputStream inputStream, OutputStream outputStream) throws IOException {
        int fully;
        byte[] bArr = new byte[bytesPerLine()];
        encodeBufferPrefix(outputStream);
        do {
            fully = readFully(inputStream, bArr);
            if (fully == 0) {
                break;
            }
            encodeLinePrefix(outputStream, fully);
            int iBytesPerAtom = 0;
            while (iBytesPerAtom < fully) {
                if (bytesPerAtom() + iBytesPerAtom <= fully) {
                    encodeAtom(outputStream, bArr, iBytesPerAtom, bytesPerAtom());
                } else {
                    encodeAtom(outputStream, bArr, iBytesPerAtom, fully - iBytesPerAtom);
                }
                iBytesPerAtom += bytesPerAtom();
            }
            encodeLineSuffix(outputStream);
        } while (fully >= bytesPerLine());
        encodeBufferSuffix(outputStream);
    }

    public void encodeBuffer(byte[] bArr, OutputStream outputStream) throws IOException {
        encodeBuffer(new ByteArrayInputStream(bArr), outputStream);
    }

    public String encodeBuffer(byte[] bArr) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try {
            encodeBuffer(new ByteArrayInputStream(bArr), byteArrayOutputStream);
            return byteArrayOutputStream.toString();
        } catch (Exception e) {
            throw new Error("CharacterEncoder.encodeBuffer internal error");
        }
    }

    public void encodeBuffer(ByteBuffer byteBuffer, OutputStream outputStream) throws IOException {
        encodeBuffer(getBytes(byteBuffer), outputStream);
    }

    public String encodeBuffer(ByteBuffer byteBuffer) {
        return encodeBuffer(getBytes(byteBuffer));
    }
}
