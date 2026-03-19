package android.icu.impl;

public final class USerializedSet {
    private char[] array = new char[8];
    private int arrayOffset;
    private int bmpLength;
    private int length;

    public final boolean getSet(char[] cArr, int i) {
        this.array = null;
        this.length = 0;
        this.bmpLength = 0;
        this.arrayOffset = 0;
        int i2 = i + 1;
        this.length = cArr[i];
        if ((this.length & 32768) != 0) {
            this.length &= 32767;
            int i3 = i2 + 1;
            if (cArr.length < this.length + i3) {
                this.length = 0;
                throw new IndexOutOfBoundsException();
            }
            this.bmpLength = cArr[i2];
            i2 = i3;
        } else {
            if (cArr.length < this.length + i2) {
                this.length = 0;
                throw new IndexOutOfBoundsException();
            }
            this.bmpLength = this.length;
        }
        this.array = new char[this.length];
        System.arraycopy(cArr, i2, this.array, 0, this.length);
        return true;
    }

    public final void setToOne(int i) {
        if (1114111 < i) {
            return;
        }
        if (i < 65535) {
            this.length = 2;
            this.bmpLength = 2;
            this.array[0] = (char) i;
            this.array[1] = (char) (i + 1);
            return;
        }
        if (i == 65535) {
            this.bmpLength = 1;
            this.length = 3;
            this.array[0] = 65535;
            this.array[1] = 1;
            this.array[2] = 0;
            return;
        }
        if (i < 1114111) {
            this.bmpLength = 0;
            this.length = 4;
            this.array[0] = (char) (i >> 16);
            this.array[1] = (char) i;
            int i2 = i + 1;
            this.array[2] = (char) (i2 >> 16);
            this.array[3] = (char) i2;
            return;
        }
        this.bmpLength = 0;
        this.length = 2;
        this.array[0] = 16;
        this.array[1] = 65535;
    }

    public final boolean getRange(int i, int[] iArr) {
        if (i < 0) {
            return false;
        }
        if (this.array == null) {
            this.array = new char[8];
        }
        if (iArr == null || iArr.length < 2) {
            throw new IllegalArgumentException();
        }
        int i2 = i * 2;
        if (i2 < this.bmpLength) {
            int i3 = i2 + 1;
            iArr[0] = this.array[i2];
            if (i3 < this.bmpLength) {
                iArr[1] = this.array[i3] - 1;
            } else if (i3 < this.length) {
                iArr[1] = ((this.array[i3] << 16) | this.array[i3 + 1]) - 1;
            } else {
                iArr[1] = 1114111;
            }
            return true;
        }
        int i4 = (i2 - this.bmpLength) * 2;
        int i5 = this.length - this.bmpLength;
        if (i4 >= i5) {
            return false;
        }
        int i6 = this.arrayOffset + this.bmpLength;
        int i7 = i6 + i4;
        iArr[0] = (this.array[i7] << 16) | this.array[i7 + 1];
        int i8 = i4 + 2;
        if (i8 < i5) {
            int i9 = i6 + i8;
            iArr[1] = ((this.array[i9] << 16) | this.array[i9 + 1]) - 1;
        } else {
            iArr[1] = 1114111;
        }
        return true;
    }

    public final boolean contains(int i) {
        if (i > 1114111) {
            return false;
        }
        if (i <= 65535) {
            int i2 = 0;
            while (i2 < this.bmpLength && ((char) i) >= this.array[i2]) {
                i2++;
            }
            return (i2 & 1) != 0;
        }
        char c = (char) (i >> 16);
        char c2 = (char) i;
        int i3 = this.bmpLength;
        while (i3 < this.length && (c > this.array[i3] || (c == this.array[i3] && c2 >= this.array[i3 + 1]))) {
            i3 += 2;
        }
        return ((i3 + this.bmpLength) & 2) != 0;
    }

    public final int countRanges() {
        return ((this.bmpLength + ((this.length - this.bmpLength) / 2)) + 1) / 2;
    }
}
