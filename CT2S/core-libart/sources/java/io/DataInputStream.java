package java.io;

import java.nio.ByteOrder;
import java.nio.charset.ModifiedUtf8;
import libcore.io.Memory;
import libcore.io.Streams;

public class DataInputStream extends FilterInputStream implements DataInput {
    private final byte[] scratch;

    public DataInputStream(InputStream in) {
        super(in);
        this.scratch = new byte[8];
    }

    @Override
    public final int read(byte[] buffer) throws IOException {
        return super.read(buffer);
    }

    @Override
    public final int read(byte[] buffer, int byteOffset, int byteCount) throws IOException {
        return this.in.read(buffer, byteOffset, byteCount);
    }

    @Override
    public final boolean readBoolean() throws IOException {
        int temp = this.in.read();
        if (temp < 0) {
            throw new EOFException();
        }
        return temp != 0;
    }

    @Override
    public final byte readByte() throws IOException {
        int temp = this.in.read();
        if (temp < 0) {
            throw new EOFException();
        }
        return (byte) temp;
    }

    @Override
    public final char readChar() throws IOException {
        return (char) readShort();
    }

    @Override
    public final double readDouble() throws IOException {
        return Double.longBitsToDouble(readLong());
    }

    @Override
    public final float readFloat() throws IOException {
        return Float.intBitsToFloat(readInt());
    }

    @Override
    public final void readFully(byte[] dst) throws IOException {
        readFully(dst, 0, dst.length);
    }

    @Override
    public final void readFully(byte[] dst, int offset, int byteCount) throws IOException {
        Streams.readFully(this.in, dst, offset, byteCount);
    }

    @Override
    public final int readInt() throws IOException {
        Streams.readFully(this.in, this.scratch, 0, 4);
        return Memory.peekInt(this.scratch, 0, ByteOrder.BIG_ENDIAN);
    }

    @Override
    @Deprecated
    public final String readLine() throws IOException {
        StringBuilder line = new StringBuilder(80);
        boolean foundTerminator = false;
        while (true) {
            int nextByte = this.in.read();
            switch (nextByte) {
                case -1:
                    if (line.length() == 0 && !foundTerminator) {
                        return null;
                    }
                    return line.toString();
                case 10:
                    return line.toString();
                case 13:
                    if (foundTerminator) {
                        ((PushbackInputStream) this.in).unread(nextByte);
                        return line.toString();
                    }
                    foundTerminator = true;
                    if (this.in.getClass() != PushbackInputStream.class) {
                        this.in = new PushbackInputStream(this.in);
                    }
                    break;
                    break;
                default:
                    if (foundTerminator) {
                        ((PushbackInputStream) this.in).unread(nextByte);
                        return line.toString();
                    }
                    line.append((char) nextByte);
                    break;
                    break;
            }
        }
    }

    @Override
    public final long readLong() throws IOException {
        Streams.readFully(this.in, this.scratch, 0, 8);
        return Memory.peekLong(this.scratch, 0, ByteOrder.BIG_ENDIAN);
    }

    @Override
    public final short readShort() throws IOException {
        Streams.readFully(this.in, this.scratch, 0, 2);
        return Memory.peekShort(this.scratch, 0, ByteOrder.BIG_ENDIAN);
    }

    @Override
    public final int readUnsignedByte() throws IOException {
        int temp = this.in.read();
        if (temp < 0) {
            throw new EOFException();
        }
        return temp;
    }

    @Override
    public final int readUnsignedShort() throws IOException {
        return readShort() & 65535;
    }

    @Override
    public final String readUTF() throws IOException {
        return decodeUTF(readUnsignedShort());
    }

    String decodeUTF(int utfSize) throws IOException {
        return decodeUTF(utfSize, this);
    }

    private static String decodeUTF(int utfSize, DataInput in) throws IOException {
        byte[] buf = new byte[utfSize];
        in.readFully(buf, 0, utfSize);
        return ModifiedUtf8.decode(buf, new char[utfSize], 0, utfSize);
    }

    public static final String readUTF(DataInput in) throws IOException {
        return decodeUTF(in.readUnsignedShort(), in);
    }

    @Override
    public final int skipBytes(int count) throws IOException {
        int skipped = 0;
        while (skipped < count) {
            long skip = this.in.skip(count - skipped);
            if (skip == 0) {
                break;
            }
            skipped = (int) (((long) skipped) + skip);
        }
        return skipped;
    }
}
