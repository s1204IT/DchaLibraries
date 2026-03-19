package java.io;

import java.nio.ByteOrder;
import libcore.io.Memory;
import sun.security.util.DerValue;

public class DataInputStream extends FilterInputStream implements DataInput {
    private byte[] bytearr;
    private char[] chararr;
    private char[] lineBuffer;
    private byte[] readBuffer;

    public DataInputStream(InputStream inputStream) {
        super(inputStream);
        this.bytearr = new byte[80];
        this.chararr = new char[80];
        this.readBuffer = new byte[8];
    }

    @Override
    public final int read(byte[] bArr) throws IOException {
        return this.in.read(bArr, 0, bArr.length);
    }

    @Override
    public final int read(byte[] bArr, int i, int i2) throws IOException {
        return this.in.read(bArr, i, i2);
    }

    @Override
    public final void readFully(byte[] bArr) throws IOException {
        readFully(bArr, 0, bArr.length);
    }

    @Override
    public final void readFully(byte[] bArr, int i, int i2) throws IOException {
        if (i2 < 0) {
            throw new IndexOutOfBoundsException();
        }
        int i3 = 0;
        while (i3 < i2) {
            int i4 = this.in.read(bArr, i + i3, i2 - i3);
            if (i4 < 0) {
                throw new EOFException();
            }
            i3 += i4;
        }
    }

    @Override
    public final int skipBytes(int i) throws IOException {
        int i2 = 0;
        while (i2 < i) {
            int iSkip = (int) this.in.skip(i - i2);
            if (iSkip <= 0) {
                break;
            }
            i2 += iSkip;
        }
        return i2;
    }

    @Override
    public final boolean readBoolean() throws IOException {
        int i = this.in.read();
        if (i >= 0) {
            return i != 0;
        }
        throw new EOFException();
    }

    @Override
    public final byte readByte() throws IOException {
        int i = this.in.read();
        if (i < 0) {
            throw new EOFException();
        }
        return (byte) i;
    }

    @Override
    public final int readUnsignedByte() throws IOException {
        int i = this.in.read();
        if (i < 0) {
            throw new EOFException();
        }
        return i;
    }

    @Override
    public final short readShort() throws IOException {
        readFully(this.readBuffer, 0, 2);
        return Memory.peekShort(this.readBuffer, 0, ByteOrder.BIG_ENDIAN);
    }

    @Override
    public final int readUnsignedShort() throws IOException {
        readFully(this.readBuffer, 0, 2);
        return Memory.peekShort(this.readBuffer, 0, ByteOrder.BIG_ENDIAN) & 65535;
    }

    @Override
    public final char readChar() throws IOException {
        readFully(this.readBuffer, 0, 2);
        return (char) Memory.peekShort(this.readBuffer, 0, ByteOrder.BIG_ENDIAN);
    }

    @Override
    public final int readInt() throws IOException {
        readFully(this.readBuffer, 0, 4);
        return Memory.peekInt(this.readBuffer, 0, ByteOrder.BIG_ENDIAN);
    }

    @Override
    public final long readLong() throws IOException {
        readFully(this.readBuffer, 0, 8);
        return (((long) this.readBuffer[0]) << 56) + (((long) (this.readBuffer[1] & Character.DIRECTIONALITY_UNDEFINED)) << 48) + (((long) (this.readBuffer[2] & Character.DIRECTIONALITY_UNDEFINED)) << 40) + (((long) (this.readBuffer[3] & Character.DIRECTIONALITY_UNDEFINED)) << 32) + (((long) (this.readBuffer[4] & Character.DIRECTIONALITY_UNDEFINED)) << 24) + ((long) ((this.readBuffer[5] & Character.DIRECTIONALITY_UNDEFINED) << 16)) + ((long) ((this.readBuffer[6] & Character.DIRECTIONALITY_UNDEFINED) << 8)) + ((long) ((this.readBuffer[7] & Character.DIRECTIONALITY_UNDEFINED) << 0));
    }

    @Override
    public final float readFloat() throws IOException {
        return Float.intBitsToFloat(readInt());
    }

    @Override
    public final double readDouble() throws IOException {
        return Double.longBitsToDouble(readLong());
    }

    @Override
    @Deprecated
    public final String readLine() throws IOException {
        int i;
        char[] cArr = this.lineBuffer;
        if (cArr == null) {
            cArr = new char[128];
            this.lineBuffer = cArr;
        }
        int length = cArr.length;
        char[] cArr2 = cArr;
        int i2 = 0;
        while (true) {
            i = this.in.read();
            if (i == -1 || i == 10) {
                break;
            }
            if (i == 13) {
                int i3 = this.in.read();
                if (i3 != 10 && i3 != -1) {
                    if (!(this.in instanceof PushbackInputStream)) {
                        this.in = new PushbackInputStream(this.in);
                    }
                    ((PushbackInputStream) this.in).unread(i3);
                }
            } else {
                length--;
                if (length < 0) {
                    char[] cArr3 = new char[i2 + 128];
                    int length2 = (cArr3.length - i2) - 1;
                    System.arraycopy((Object) this.lineBuffer, 0, (Object) cArr3, 0, i2);
                    this.lineBuffer = cArr3;
                    cArr2 = cArr3;
                    length = length2;
                }
                cArr2[i2] = (char) i;
                i2++;
            }
        }
        if (i == -1 && i2 == 0) {
            return null;
        }
        return String.copyValueOf(cArr2, 0, i2);
    }

    @Override
    public final String readUTF() throws IOException {
        return readUTF(this);
    }

    public static final String readUTF(DataInput dataInput) throws IOException {
        byte[] bArr;
        char[] cArr;
        int i;
        int unsignedShort = dataInput.readUnsignedShort();
        if (dataInput instanceof DataInputStream) {
            DataInputStream dataInputStream = (DataInputStream) dataInput;
            if (dataInputStream.bytearr.length < unsignedShort) {
                int i2 = unsignedShort * 2;
                dataInputStream.bytearr = new byte[i2];
                dataInputStream.chararr = new char[i2];
            }
            cArr = dataInputStream.chararr;
            bArr = dataInputStream.bytearr;
        } else {
            bArr = new byte[unsignedShort];
            cArr = new char[unsignedShort];
        }
        dataInput.readFully(bArr, 0, unsignedShort);
        int i3 = 0;
        int i4 = 0;
        while (i3 < unsignedShort) {
            int i5 = bArr[i3] & Character.DIRECTIONALITY_UNDEFINED;
            if (i5 > 127) {
                break;
            }
            i3++;
            cArr[i4] = (char) i5;
            i4++;
        }
        while (i3 < unsignedShort) {
            int i6 = bArr[i3] & Character.DIRECTIONALITY_UNDEFINED;
            int i7 = i6 >> 4;
            switch (i7) {
                case 0:
                case 1:
                case 2:
                case 3:
                case 4:
                case 5:
                case 6:
                case 7:
                    i3++;
                    cArr[i4] = (char) i6;
                    i4++;
                    break;
                default:
                    switch (i7) {
                        case 12:
                        case 13:
                            i3 += 2;
                            if (i3 > unsignedShort) {
                                throw new UTFDataFormatException("malformed input: partial character at end");
                            }
                            byte b = bArr[i3 - 1];
                            if ((b & DerValue.TAG_PRIVATE) != 128) {
                                throw new UTFDataFormatException("malformed input around byte " + i3);
                            }
                            i = i4 + 1;
                            cArr[i4] = (char) (((i6 & 31) << 6) | (b & 63));
                            break;
                            break;
                        case 14:
                            i3 += 3;
                            if (i3 > unsignedShort) {
                                throw new UTFDataFormatException("malformed input: partial character at end");
                            }
                            byte b2 = bArr[i3 - 2];
                            int i8 = i3 - 1;
                            byte b3 = bArr[i8];
                            if ((b2 & DerValue.TAG_PRIVATE) != 128 || (b3 & DerValue.TAG_PRIVATE) != 128) {
                                throw new UTFDataFormatException("malformed input around byte " + i8);
                            }
                            i = i4 + 1;
                            cArr[i4] = (char) (((i6 & 15) << 12) | ((b2 & 63) << 6) | ((b3 & 63) << 0));
                            break;
                            break;
                        default:
                            throw new UTFDataFormatException("malformed input around byte " + i3);
                    }
                    i4 = i;
                    break;
            }
        }
        return new String(cArr, 0, i4);
    }
}
