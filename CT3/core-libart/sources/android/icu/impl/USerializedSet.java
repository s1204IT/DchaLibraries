package android.icu.impl;

public final class USerializedSet {
    private char[] array = new char[8];
    private int arrayOffset;
    private int bmpLength;
    private int length;

    public final boolean getSet(char[] src, int srcStart) {
        int srcStart2;
        this.array = null;
        this.length = 0;
        this.bmpLength = 0;
        this.arrayOffset = 0;
        int srcStart3 = srcStart + 1;
        this.length = src[srcStart];
        if ((this.length & 32768) > 0) {
            this.length &= 32767;
            if (src.length < srcStart3 + 1 + this.length) {
                this.length = 0;
                throw new IndexOutOfBoundsException();
            }
            srcStart2 = srcStart3 + 1;
            this.bmpLength = src[srcStart3];
        } else {
            if (src.length < this.length + srcStart3) {
                this.length = 0;
                throw new IndexOutOfBoundsException();
            }
            this.bmpLength = this.length;
            srcStart2 = srcStart3;
        }
        this.array = new char[this.length];
        System.arraycopy(src, srcStart2, this.array, 0, this.length);
        return true;
    }

    public final void setToOne(int c) {
        if (1114111 < c) {
            return;
        }
        if (c < 65535) {
            this.length = 2;
            this.bmpLength = 2;
            this.array[0] = (char) c;
            this.array[1] = (char) (c + 1);
            return;
        }
        if (c == 65535) {
            this.bmpLength = 1;
            this.length = 3;
            this.array[0] = 65535;
            this.array[1] = 1;
            this.array[2] = 0;
            return;
        }
        if (c < 1114111) {
            this.bmpLength = 0;
            this.length = 4;
            this.array[0] = (char) (c >> 16);
            this.array[1] = (char) c;
            int c2 = c + 1;
            this.array[2] = (char) (c2 >> 16);
            this.array[3] = (char) c2;
            return;
        }
        this.bmpLength = 0;
        this.length = 2;
        this.array[0] = 16;
        this.array[1] = 65535;
    }

    public final boolean getRange(int rangeIndex, int[] range) {
        if (rangeIndex < 0) {
            return false;
        }
        if (this.array == null) {
            this.array = new char[8];
        }
        if (range == null || range.length < 2) {
            throw new IllegalArgumentException();
        }
        int rangeIndex2 = rangeIndex * 2;
        if (rangeIndex2 < this.bmpLength) {
            int rangeIndex3 = rangeIndex2 + 1;
            range[0] = this.array[rangeIndex2];
            if (rangeIndex3 < this.bmpLength) {
                range[1] = this.array[rangeIndex3] - 1;
            } else if (rangeIndex3 < this.length) {
                range[1] = ((this.array[rangeIndex3] << 16) | this.array[rangeIndex3 + 1]) - 1;
            } else {
                range[1] = 1114111;
            }
            return true;
        }
        int rangeIndex4 = (rangeIndex2 - this.bmpLength) * 2;
        int suppLength = this.length - this.bmpLength;
        if (rangeIndex4 >= suppLength) {
            return false;
        }
        int offset = this.arrayOffset + this.bmpLength;
        range[0] = (this.array[offset + rangeIndex4] << 16) | this.array[offset + rangeIndex4 + 1];
        if (rangeIndex4 + 2 < suppLength) {
            range[1] = ((this.array[offset + r10] << 16) | this.array[(offset + r10) + 1]) - 1;
        } else {
            range[1] = 1114111;
        }
        return true;
    }

    public final boolean contains(int c) {
        if (c > 1114111) {
            return false;
        }
        if (c <= 65535) {
            int i = 0;
            while (i < this.bmpLength && ((char) c) >= this.array[i]) {
                i++;
            }
            return (i & 1) != 0;
        }
        char high = (char) (c >> 16);
        char low = (char) c;
        int i2 = this.bmpLength;
        while (i2 < this.length && (high > this.array[i2] || (high == this.array[i2] && low >= this.array[i2 + 1]))) {
            i2 += 2;
        }
        return ((this.bmpLength + i2) & 2) != 0;
    }

    public final int countRanges() {
        return ((this.bmpLength + ((this.length - this.bmpLength) / 2)) + 1) / 2;
    }
}
