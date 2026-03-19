package libcore.io;

import android.icu.lang.UCharacterEnums;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

public final class Streams {
    private static AtomicReference<byte[]> skipBuffer = new AtomicReference<>();

    private Streams() {
    }

    public static int readSingleByte(InputStream inputStream) throws IOException {
        byte[] bArr = new byte[1];
        if (inputStream.read(bArr, 0, 1) != -1) {
            return bArr[0] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED;
        }
        return -1;
    }

    public static void writeSingleByte(OutputStream outputStream, int i) throws IOException {
        outputStream.write(new byte[]{(byte) (i & 255)});
    }

    public static void readFully(InputStream inputStream, byte[] bArr) throws IOException {
        readFully(inputStream, bArr, 0, bArr.length);
    }

    public static void readFully(InputStream inputStream, byte[] bArr, int i, int i2) throws IOException {
        if (i2 == 0) {
            return;
        }
        if (inputStream == null) {
            throw new NullPointerException("in == null");
        }
        if (bArr == null) {
            throw new NullPointerException("dst == null");
        }
        Arrays.checkOffsetAndCount(bArr.length, i, i2);
        while (i2 > 0) {
            int i3 = inputStream.read(bArr, i, i2);
            if (i3 < 0) {
                throw new EOFException();
            }
            i += i3;
            i2 -= i3;
        }
    }

    public static byte[] readFully(InputStream inputStream) throws IOException {
        try {
            return readFullyNoClose(inputStream);
        } finally {
            inputStream.close();
        }
    }

    public static byte[] readFullyNoClose(InputStream inputStream) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byte[] bArr = new byte[1024];
        while (true) {
            int i = inputStream.read(bArr);
            if (i != -1) {
                byteArrayOutputStream.write(bArr, 0, i);
            } else {
                return byteArrayOutputStream.toByteArray();
            }
        }
    }

    public static String readFully(Reader reader) throws IOException {
        try {
            StringWriter stringWriter = new StringWriter();
            char[] cArr = new char[1024];
            while (true) {
                int i = reader.read(cArr);
                if (i != -1) {
                    stringWriter.write(cArr, 0, i);
                } else {
                    return stringWriter.toString();
                }
            }
        } finally {
            reader.close();
        }
    }

    public static void skipAll(InputStream inputStream) throws IOException {
        do {
            inputStream.skip(Long.MAX_VALUE);
        } while (inputStream.read() != -1);
    }

    public static long skipByReading(InputStream inputStream, long j) throws IOException {
        int iMin;
        int i;
        byte[] andSet = skipBuffer.getAndSet(null);
        if (andSet == null) {
            andSet = new byte[4096];
        }
        long j2 = 0;
        while (j2 < j && (i = inputStream.read(andSet, 0, (iMin = (int) Math.min(j - j2, andSet.length)))) != -1) {
            j2 += (long) i;
            if (i < iMin) {
                break;
            }
        }
        skipBuffer.set(andSet);
        return j2;
    }

    public static int copy(InputStream inputStream, OutputStream outputStream) throws IOException {
        byte[] bArr = new byte[8192];
        int i = 0;
        while (true) {
            int i2 = inputStream.read(bArr);
            if (i2 != -1) {
                i += i2;
                outputStream.write(bArr, 0, i2);
            } else {
                return i;
            }
        }
    }

    public static String readAsciiLine(InputStream inputStream) throws IOException {
        StringBuilder sb = new StringBuilder(80);
        while (true) {
            int i = inputStream.read();
            if (i == -1) {
                throw new EOFException();
            }
            if (i != 10) {
                sb.append((char) i);
            } else {
                int length = sb.length();
                if (length > 0) {
                    int i2 = length - 1;
                    if (sb.charAt(i2) == '\r') {
                        sb.setLength(i2);
                    }
                }
                return sb.toString();
            }
        }
    }
}
