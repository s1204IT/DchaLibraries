package java.io;

public class DataOutputStream extends FilterOutputStream implements DataOutput {
    private byte[] bytearr;
    private byte[] writeBuffer;
    protected int written;

    public DataOutputStream(OutputStream outputStream) {
        super(outputStream);
        this.bytearr = null;
        this.writeBuffer = new byte[8];
    }

    private void incCount(int i) {
        int i2 = this.written + i;
        if (i2 < 0) {
            i2 = Integer.MAX_VALUE;
        }
        this.written = i2;
    }

    @Override
    public synchronized void write(int i) throws IOException {
        this.out.write(i);
        incCount(1);
    }

    @Override
    public synchronized void write(byte[] bArr, int i, int i2) throws IOException {
        this.out.write(bArr, i, i2);
        incCount(i2);
    }

    @Override
    public void flush() throws IOException {
        this.out.flush();
    }

    @Override
    public final void writeBoolean(boolean z) throws IOException {
        this.out.write(z ? 1 : 0);
        incCount(1);
    }

    @Override
    public final void writeByte(int i) throws IOException {
        this.out.write(i);
        incCount(1);
    }

    @Override
    public final void writeShort(int i) throws IOException {
        this.out.write((i >>> 8) & 255);
        this.out.write((i >>> 0) & 255);
        incCount(2);
    }

    @Override
    public final void writeChar(int i) throws IOException {
        this.out.write((i >>> 8) & 255);
        this.out.write((i >>> 0) & 255);
        incCount(2);
    }

    @Override
    public final void writeInt(int i) throws IOException {
        this.out.write((i >>> 24) & 255);
        this.out.write((i >>> 16) & 255);
        this.out.write((i >>> 8) & 255);
        this.out.write((i >>> 0) & 255);
        incCount(4);
    }

    @Override
    public final void writeLong(long j) throws IOException {
        this.writeBuffer[0] = (byte) (j >>> 56);
        this.writeBuffer[1] = (byte) (j >>> 48);
        this.writeBuffer[2] = (byte) (j >>> 40);
        this.writeBuffer[3] = (byte) (j >>> 32);
        this.writeBuffer[4] = (byte) (j >>> 24);
        this.writeBuffer[5] = (byte) (j >>> 16);
        this.writeBuffer[6] = (byte) (j >>> 8);
        this.writeBuffer[7] = (byte) (j >>> 0);
        this.out.write(this.writeBuffer, 0, 8);
        incCount(8);
    }

    @Override
    public final void writeFloat(float f) throws IOException {
        writeInt(Float.floatToIntBits(f));
    }

    @Override
    public final void writeDouble(double d) throws IOException {
        writeLong(Double.doubleToLongBits(d));
    }

    @Override
    public final void writeBytes(String str) throws IOException {
        int length = str.length();
        for (int i = 0; i < length; i++) {
            this.out.write((byte) str.charAt(i));
        }
        incCount(length);
    }

    @Override
    public final void writeChars(String str) throws IOException {
        int length = str.length();
        for (int i = 0; i < length; i++) {
            char cCharAt = str.charAt(i);
            this.out.write((cCharAt >>> '\b') & 255);
            this.out.write((cCharAt >>> 0) & 255);
        }
        incCount(length * 2);
    }

    @Override
    public final void writeUTF(String str) throws IOException {
        writeUTF(str, this);
    }

    static int writeUTF(String str, DataOutput dataOutput) throws IOException {
        byte[] bArr;
        int length = str.length();
        int i = 0;
        for (int i2 = 0; i2 < length; i2++) {
            char cCharAt = str.charAt(i2);
            i = (cCharAt < 1 || cCharAt > 127) ? cCharAt > 2047 ? i + 3 : i + 2 : i + 1;
        }
        if (i > 65535) {
            throw new UTFDataFormatException("encoded string too long: " + i + " bytes");
        }
        if (dataOutput instanceof DataOutputStream) {
            DataOutputStream dataOutputStream = (DataOutputStream) dataOutput;
            if (dataOutputStream.bytearr == null || dataOutputStream.bytearr.length < i + 2) {
                dataOutputStream.bytearr = new byte[(i * 2) + 2];
            }
            bArr = dataOutputStream.bytearr;
        } else {
            bArr = new byte[i + 2];
        }
        bArr[0] = (byte) ((i >>> 8) & 255);
        bArr[1] = (byte) ((i >>> 0) & 255);
        int i3 = 0;
        int i4 = 2;
        while (i3 < length) {
            char cCharAt2 = str.charAt(i3);
            if (cCharAt2 < 1 || cCharAt2 > 127) {
                break;
            }
            bArr[i4] = (byte) cCharAt2;
            i3++;
            i4++;
        }
        while (i3 < length) {
            char cCharAt3 = str.charAt(i3);
            if (cCharAt3 >= 1 && cCharAt3 <= 127) {
                bArr[i4] = (byte) cCharAt3;
                i4++;
            } else if (cCharAt3 > 2047) {
                int i5 = i4 + 1;
                bArr[i4] = (byte) (224 | ((cCharAt3 >> '\f') & 15));
                int i6 = i5 + 1;
                bArr[i5] = (byte) (((cCharAt3 >> 6) & 63) | 128);
                bArr[i6] = (byte) (((cCharAt3 >> 0) & 63) | 128);
                i4 = i6 + 1;
            } else {
                int i7 = i4 + 1;
                bArr[i4] = (byte) (192 | ((cCharAt3 >> 6) & 31));
                i4 = i7 + 1;
                bArr[i7] = (byte) (((cCharAt3 >> 0) & 63) | 128);
            }
            i3++;
        }
        int i8 = i + 2;
        dataOutput.write(bArr, 0, i8);
        return i8;
    }

    public final int size() {
        return this.written;
    }
}
