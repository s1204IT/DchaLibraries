package sun.security.util;

import java.io.IOException;
import java.util.ArrayList;

class DerIndefLenConverter {
    private static final int CLASS_MASK = 192;
    private static final int FORM_MASK = 32;
    private static final int LEN_LONG = 128;
    private static final int LEN_MASK = 127;
    private static final int SKIP_EOC_BYTES = 2;
    private static final int TAG_MASK = 31;
    private byte[] data;
    private int dataPos;
    private int dataSize;
    private int index;
    private byte[] newData;
    private int newDataPos;
    private int unresolved = 0;
    private ArrayList<Object> ndefsList = new ArrayList<>();
    private int numOfTotalLenBytes = 0;

    private boolean isEOC(int i) {
        return (i & 31) == 0 && (i & 32) == 0 && (i & CLASS_MASK) == 0;
    }

    static boolean isLongForm(int i) {
        return (i & 128) == 128;
    }

    DerIndefLenConverter() {
    }

    static boolean isIndefinite(int i) {
        return isLongForm(i) && (i & 127) == 0;
    }

    private void parseTag() throws IOException {
        if (this.dataPos == this.dataSize) {
            return;
        }
        if (isEOC(this.data[this.dataPos]) && this.data[this.dataPos + 1] == 0) {
            int length = 0;
            Object obj = null;
            int size = this.ndefsList.size() - 1;
            while (size >= 0) {
                obj = this.ndefsList.get(size);
                if (obj instanceof Integer) {
                    break;
                }
                length += ((byte[]) obj).length - 3;
                size--;
            }
            if (size < 0) {
                throw new IOException("EOC does not have matching indefinite-length tag");
            }
            this.ndefsList.set(size, getLengthBytes((this.dataPos - ((Integer) obj).intValue()) + length));
            this.unresolved--;
            this.numOfTotalLenBytes += r0.length - 3;
        }
        this.dataPos++;
    }

    private void writeTag() {
        if (this.dataPos == this.dataSize) {
            return;
        }
        byte[] bArr = this.data;
        int i = this.dataPos;
        this.dataPos = i + 1;
        byte b = bArr[i];
        if (isEOC(b) && this.data[this.dataPos] == 0) {
            this.dataPos++;
            writeTag();
        } else {
            byte[] bArr2 = this.newData;
            int i2 = this.newDataPos;
            this.newDataPos = i2 + 1;
            bArr2[i2] = b;
        }
    }

    private int parseLength() throws IOException {
        if (this.dataPos == this.dataSize) {
            return 0;
        }
        byte[] bArr = this.data;
        int i = this.dataPos;
        this.dataPos = i + 1;
        int i2 = bArr[i] & Character.DIRECTIONALITY_UNDEFINED;
        if (isIndefinite(i2)) {
            this.ndefsList.add(new Integer(this.dataPos));
            this.unresolved++;
            return 0;
        }
        if (isLongForm(i2)) {
            int i3 = i2 & 127;
            if (i3 > 4) {
                throw new IOException("Too much data");
            }
            if (this.dataSize - this.dataPos < i3 + 1) {
                throw new IOException("Too little data");
            }
            int i4 = 0;
            for (int i5 = 0; i5 < i3; i5++) {
                byte[] bArr2 = this.data;
                int i6 = this.dataPos;
                this.dataPos = i6 + 1;
                i4 = (i4 << 8) + (bArr2[i6] & Character.DIRECTIONALITY_UNDEFINED);
            }
            if (i4 < 0) {
                throw new IOException("Invalid length bytes");
            }
            return i4;
        }
        return i2 & 127;
    }

    private void writeLengthAndValue() throws IOException {
        int i;
        if (this.dataPos == this.dataSize) {
            return;
        }
        byte[] bArr = this.data;
        int i2 = this.dataPos;
        this.dataPos = i2 + 1;
        int i3 = bArr[i2] & Character.DIRECTIONALITY_UNDEFINED;
        if (isIndefinite(i3)) {
            ArrayList<Object> arrayList = this.ndefsList;
            int i4 = this.index;
            this.index = i4 + 1;
            byte[] bArr2 = (byte[]) arrayList.get(i4);
            System.arraycopy(bArr2, 0, this.newData, this.newDataPos, bArr2.length);
            this.newDataPos += bArr2.length;
            return;
        }
        if (isLongForm(i3)) {
            int i5 = i3 & 127;
            i = 0;
            for (int i6 = 0; i6 < i5; i6++) {
                byte[] bArr3 = this.data;
                int i7 = this.dataPos;
                this.dataPos = i7 + 1;
                i = (i << 8) + (bArr3[i7] & Character.DIRECTIONALITY_UNDEFINED);
            }
            if (i < 0) {
                throw new IOException("Invalid length bytes");
            }
        } else {
            i = i3 & 127;
        }
        writeLength(i);
        writeValue(i);
    }

    private void writeLength(int i) {
        if (i < 128) {
            byte[] bArr = this.newData;
            int i2 = this.newDataPos;
            this.newDataPos = i2 + 1;
            bArr[i2] = (byte) i;
            return;
        }
        if (i < 256) {
            byte[] bArr2 = this.newData;
            int i3 = this.newDataPos;
            this.newDataPos = i3 + 1;
            bArr2[i3] = -127;
            byte[] bArr3 = this.newData;
            int i4 = this.newDataPos;
            this.newDataPos = i4 + 1;
            bArr3[i4] = (byte) i;
            return;
        }
        if (i < 65536) {
            byte[] bArr4 = this.newData;
            int i5 = this.newDataPos;
            this.newDataPos = i5 + 1;
            bArr4[i5] = -126;
            byte[] bArr5 = this.newData;
            int i6 = this.newDataPos;
            this.newDataPos = i6 + 1;
            bArr5[i6] = (byte) (i >> 8);
            byte[] bArr6 = this.newData;
            int i7 = this.newDataPos;
            this.newDataPos = i7 + 1;
            bArr6[i7] = (byte) i;
            return;
        }
        if (i < 16777216) {
            byte[] bArr7 = this.newData;
            int i8 = this.newDataPos;
            this.newDataPos = i8 + 1;
            bArr7[i8] = -125;
            byte[] bArr8 = this.newData;
            int i9 = this.newDataPos;
            this.newDataPos = i9 + 1;
            bArr8[i9] = (byte) (i >> 16);
            byte[] bArr9 = this.newData;
            int i10 = this.newDataPos;
            this.newDataPos = i10 + 1;
            bArr9[i10] = (byte) (i >> 8);
            byte[] bArr10 = this.newData;
            int i11 = this.newDataPos;
            this.newDataPos = i11 + 1;
            bArr10[i11] = (byte) i;
            return;
        }
        byte[] bArr11 = this.newData;
        int i12 = this.newDataPos;
        this.newDataPos = i12 + 1;
        bArr11[i12] = -124;
        byte[] bArr12 = this.newData;
        int i13 = this.newDataPos;
        this.newDataPos = i13 + 1;
        bArr12[i13] = (byte) (i >> 24);
        byte[] bArr13 = this.newData;
        int i14 = this.newDataPos;
        this.newDataPos = i14 + 1;
        bArr13[i14] = (byte) (i >> 16);
        byte[] bArr14 = this.newData;
        int i15 = this.newDataPos;
        this.newDataPos = i15 + 1;
        bArr14[i15] = (byte) (i >> 8);
        byte[] bArr15 = this.newData;
        int i16 = this.newDataPos;
        this.newDataPos = i16 + 1;
        bArr15[i16] = (byte) i;
    }

    private byte[] getLengthBytes(int i) {
        if (i < 128) {
            return new byte[]{(byte) i};
        }
        if (i < 256) {
            return new byte[]{-127, (byte) i};
        }
        if (i < 65536) {
            return new byte[]{-126, (byte) (i >> 8), (byte) i};
        }
        if (i < 16777216) {
            return new byte[]{-125, (byte) (i >> 16), (byte) (i >> 8), (byte) i};
        }
        return new byte[]{-124, (byte) (i >> 24), (byte) (i >> 16), (byte) (i >> 8), (byte) i};
    }

    private int getNumOfLenBytes(int i) {
        if (i < 128) {
            return 1;
        }
        if (i < 256) {
            return 2;
        }
        if (i < 65536) {
            return 3;
        }
        if (i < 16777216) {
            return 4;
        }
        return 5;
    }

    private void parseValue(int i) {
        this.dataPos += i;
    }

    private void writeValue(int i) {
        for (int i2 = 0; i2 < i; i2++) {
            byte[] bArr = this.newData;
            int i3 = this.newDataPos;
            this.newDataPos = i3 + 1;
            byte[] bArr2 = this.data;
            int i4 = this.dataPos;
            this.dataPos = i4 + 1;
            bArr[i3] = bArr2[i4];
        }
    }

    byte[] convert(byte[] bArr) throws IOException {
        int i;
        this.data = bArr;
        this.dataPos = 0;
        this.index = 0;
        this.dataSize = this.data.length;
        while (true) {
            if (this.dataPos < this.dataSize) {
                parseTag();
                parseValue(parseLength());
                if (this.unresolved == 0) {
                    i = this.dataSize - this.dataPos;
                    this.dataSize = this.dataPos;
                    break;
                }
            } else {
                i = 0;
                break;
            }
        }
        if (this.unresolved != 0) {
            throw new IOException("not all indef len BER resolved");
        }
        this.newData = new byte[this.dataSize + this.numOfTotalLenBytes + i];
        this.dataPos = 0;
        this.newDataPos = 0;
        this.index = 0;
        while (this.dataPos < this.dataSize) {
            writeTag();
            writeLengthAndValue();
        }
        System.arraycopy(bArr, this.dataSize, this.newData, this.dataSize + this.numOfTotalLenBytes, i);
        return this.newData;
    }
}
