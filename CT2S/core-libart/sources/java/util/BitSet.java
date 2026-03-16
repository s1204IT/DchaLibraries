package java.util;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.LongBuffer;

public class BitSet implements Serializable, Cloneable {
    private static final long ALL_ONES = -1;
    private static final long serialVersionUID = 7997698588986878753L;
    private long[] bits;
    private transient int longCount;

    private void shrinkSize() {
        int i = this.longCount - 1;
        while (i >= 0 && this.bits[i] == 0) {
            i--;
        }
        this.longCount = i + 1;
    }

    public BitSet() {
        this(new long[1]);
    }

    public BitSet(int bitCount) {
        if (bitCount < 0) {
            throw new NegativeArraySizeException(Integer.toString(bitCount));
        }
        this.bits = arrayForBits(bitCount);
        this.longCount = 0;
    }

    private BitSet(long[] bits) {
        this.bits = bits;
        this.longCount = bits.length;
        shrinkSize();
    }

    private static long[] arrayForBits(int bitCount) {
        return new long[(bitCount + 63) / 64];
    }

    public Object clone() {
        try {
            BitSet clone = (BitSet) super.clone();
            clone.bits = (long[]) this.bits.clone();
            clone.shrinkSize();
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BitSet)) {
            return false;
        }
        BitSet lhs = (BitSet) o;
        if (this.longCount != lhs.longCount) {
            return false;
        }
        for (int i = 0; i < this.longCount; i++) {
            if (this.bits[i] != lhs.bits[i]) {
                return false;
            }
        }
        return true;
    }

    private void ensureCapacity(int desiredLongCount) {
        if (desiredLongCount > this.bits.length) {
            int newLength = Math.max(desiredLongCount, this.bits.length * 2);
            long[] newBits = new long[newLength];
            System.arraycopy(this.bits, 0, newBits, 0, this.longCount);
            this.bits = newBits;
        }
    }

    public int hashCode() {
        long x = 1234;
        for (int i = 0; i < this.longCount; i++) {
            x ^= this.bits[i] * ((long) (i + 1));
        }
        return (int) ((x >> 32) ^ x);
    }

    public boolean get(int index) {
        if (index < 0) {
            checkIndex(index);
        }
        int arrayIndex = index / 64;
        return arrayIndex < this.longCount && (this.bits[arrayIndex] & (1 << index)) != 0;
    }

    public void set(int index) {
        if (index < 0) {
            checkIndex(index);
        }
        int arrayIndex = index / 64;
        if (arrayIndex >= this.bits.length) {
            ensureCapacity(arrayIndex + 1);
        }
        long[] jArr = this.bits;
        jArr[arrayIndex] = jArr[arrayIndex] | (1 << index);
        this.longCount = Math.max(this.longCount, arrayIndex + 1);
    }

    public void clear(int index) {
        if (index < 0) {
            checkIndex(index);
        }
        int arrayIndex = index / 64;
        if (arrayIndex < this.longCount) {
            long[] jArr = this.bits;
            jArr[arrayIndex] = jArr[arrayIndex] & ((1 << index) ^ (-1));
            shrinkSize();
        }
    }

    public void flip(int index) {
        if (index < 0) {
            checkIndex(index);
        }
        int arrayIndex = index / 64;
        if (arrayIndex >= this.bits.length) {
            ensureCapacity(arrayIndex + 1);
        }
        long[] jArr = this.bits;
        jArr[arrayIndex] = jArr[arrayIndex] ^ (1 << index);
        this.longCount = Math.max(this.longCount, arrayIndex + 1);
        shrinkSize();
    }

    private void checkIndex(int index) {
        if (index < 0) {
            throw new IndexOutOfBoundsException("index < 0: " + index);
        }
    }

    private void checkRange(int fromIndex, int toIndex) {
        if ((fromIndex | toIndex) < 0 || toIndex < fromIndex) {
            throw new IndexOutOfBoundsException("fromIndex=" + fromIndex + " toIndex=" + toIndex);
        }
    }

    public BitSet get(int fromIndex, int toIndex) {
        checkRange(fromIndex, toIndex);
        int last = this.longCount * 64;
        if (fromIndex >= last || fromIndex == toIndex) {
            return new BitSet(0);
        }
        if (toIndex > last) {
            toIndex = last;
        }
        int firstArrayIndex = fromIndex / 64;
        int lastArrayIndex = (toIndex - 1) / 64;
        long lowMask = (-1) << fromIndex;
        long highMask = (-1) >>> (-toIndex);
        if (firstArrayIndex == lastArrayIndex) {
            long result = (this.bits[firstArrayIndex] & (lowMask & highMask)) >>> fromIndex;
            if (result == 0) {
                return new BitSet(0);
            }
            return new BitSet(new long[]{result});
        }
        long[] newBits = new long[(lastArrayIndex - firstArrayIndex) + 1];
        newBits[0] = this.bits[firstArrayIndex] & lowMask;
        newBits[newBits.length - 1] = this.bits[lastArrayIndex] & highMask;
        for (int i = 1; i < lastArrayIndex - firstArrayIndex; i++) {
            newBits[i] = this.bits[firstArrayIndex + i];
        }
        int numBitsToShift = fromIndex % 64;
        int length = newBits.length;
        if (numBitsToShift != 0) {
            for (int i2 = 0; i2 < newBits.length; i2++) {
                newBits[i2] = newBits[i2] >>> numBitsToShift;
                if (i2 != newBits.length - 1) {
                    newBits[i2] = newBits[i2] | (newBits[i2 + 1] << (-numBitsToShift));
                }
                if (newBits[i2] != 0) {
                    int actualLen = i2 + 1;
                }
            }
        }
        return new BitSet(newBits);
    }

    public void set(int index, boolean state) {
        if (state) {
            set(index);
        } else {
            clear(index);
        }
    }

    public void set(int fromIndex, int toIndex, boolean state) {
        if (state) {
            set(fromIndex, toIndex);
        } else {
            clear(fromIndex, toIndex);
        }
    }

    public void clear() {
        Arrays.fill(this.bits, 0, this.longCount, 0L);
        this.longCount = 0;
    }

    public void set(int fromIndex, int toIndex) {
        checkRange(fromIndex, toIndex);
        if (fromIndex != toIndex) {
            int firstArrayIndex = fromIndex / 64;
            int lastArrayIndex = (toIndex - 1) / 64;
            if (lastArrayIndex >= this.bits.length) {
                ensureCapacity(lastArrayIndex + 1);
            }
            long lowMask = (-1) << fromIndex;
            long highMask = (-1) >>> (-toIndex);
            if (firstArrayIndex == lastArrayIndex) {
                long[] jArr = this.bits;
                jArr[firstArrayIndex] = jArr[firstArrayIndex] | (lowMask & highMask);
            } else {
                long[] jArr2 = this.bits;
                int i = firstArrayIndex + 1;
                jArr2[firstArrayIndex] = jArr2[firstArrayIndex] | lowMask;
                while (i < lastArrayIndex) {
                    long[] jArr3 = this.bits;
                    jArr3[i] = jArr3[i] | (-1);
                    i++;
                }
                long[] jArr4 = this.bits;
                int i2 = i + 1;
                jArr4[i] = jArr4[i] | highMask;
            }
            this.longCount = Math.max(this.longCount, lastArrayIndex + 1);
        }
    }

    public void clear(int fromIndex, int toIndex) {
        int last;
        checkRange(fromIndex, toIndex);
        if (fromIndex != toIndex && this.longCount != 0 && fromIndex < (last = this.longCount * 64)) {
            if (toIndex > last) {
                toIndex = last;
            }
            int firstArrayIndex = fromIndex / 64;
            int lastArrayIndex = (toIndex - 1) / 64;
            long lowMask = (-1) << fromIndex;
            long highMask = (-1) >>> (-toIndex);
            if (firstArrayIndex == lastArrayIndex) {
                long[] jArr = this.bits;
                jArr[firstArrayIndex] = jArr[firstArrayIndex] & ((lowMask & highMask) ^ (-1));
            } else {
                long[] jArr2 = this.bits;
                int i = firstArrayIndex + 1;
                jArr2[firstArrayIndex] = jArr2[firstArrayIndex] & ((-1) ^ lowMask);
                while (i < lastArrayIndex) {
                    this.bits[i] = 0;
                    i++;
                }
                long[] jArr3 = this.bits;
                int i2 = i + 1;
                jArr3[i] = jArr3[i] & ((-1) ^ highMask);
            }
            shrinkSize();
        }
    }

    public void flip(int fromIndex, int toIndex) {
        checkRange(fromIndex, toIndex);
        if (fromIndex != toIndex) {
            int firstArrayIndex = fromIndex / 64;
            int lastArrayIndex = (toIndex - 1) / 64;
            if (lastArrayIndex >= this.bits.length) {
                ensureCapacity(lastArrayIndex + 1);
            }
            long lowMask = (-1) << fromIndex;
            long highMask = (-1) >>> (-toIndex);
            if (firstArrayIndex == lastArrayIndex) {
                long[] jArr = this.bits;
                jArr[firstArrayIndex] = jArr[firstArrayIndex] ^ (lowMask & highMask);
            } else {
                long[] jArr2 = this.bits;
                int i = firstArrayIndex + 1;
                jArr2[firstArrayIndex] = jArr2[firstArrayIndex] ^ lowMask;
                while (i < lastArrayIndex) {
                    long[] jArr3 = this.bits;
                    jArr3[i] = jArr3[i] ^ (-1);
                    i++;
                }
                long[] jArr4 = this.bits;
                int i2 = i + 1;
                jArr4[i] = jArr4[i] ^ highMask;
            }
            this.longCount = Math.max(this.longCount, lastArrayIndex + 1);
            shrinkSize();
        }
    }

    public boolean intersects(BitSet bs) {
        long[] bsBits = bs.bits;
        int length = Math.min(this.longCount, bs.longCount);
        for (int i = 0; i < length; i++) {
            if ((this.bits[i] & bsBits[i]) != 0) {
                return true;
            }
        }
        return false;
    }

    public void and(BitSet bs) {
        int minSize = Math.min(this.longCount, bs.longCount);
        for (int i = 0; i < minSize; i++) {
            long[] jArr = this.bits;
            jArr[i] = jArr[i] & bs.bits[i];
        }
        Arrays.fill(this.bits, minSize, this.longCount, 0L);
        shrinkSize();
    }

    public void andNot(BitSet bs) {
        int minSize = Math.min(this.longCount, bs.longCount);
        for (int i = 0; i < minSize; i++) {
            long[] jArr = this.bits;
            jArr[i] = jArr[i] & (bs.bits[i] ^ (-1));
        }
        shrinkSize();
    }

    public void or(BitSet bs) {
        int minSize = Math.min(this.longCount, bs.longCount);
        int maxSize = Math.max(this.longCount, bs.longCount);
        ensureCapacity(maxSize);
        for (int i = 0; i < minSize; i++) {
            long[] jArr = this.bits;
            jArr[i] = jArr[i] | bs.bits[i];
        }
        if (bs.longCount > minSize) {
            System.arraycopy(bs.bits, minSize, this.bits, minSize, maxSize - minSize);
        }
        this.longCount = maxSize;
    }

    public void xor(BitSet bs) {
        int minSize = Math.min(this.longCount, bs.longCount);
        int maxSize = Math.max(this.longCount, bs.longCount);
        ensureCapacity(maxSize);
        for (int i = 0; i < minSize; i++) {
            long[] jArr = this.bits;
            jArr[i] = jArr[i] ^ bs.bits[i];
        }
        if (bs.longCount > minSize) {
            System.arraycopy(bs.bits, minSize, this.bits, minSize, maxSize - minSize);
        }
        this.longCount = maxSize;
        shrinkSize();
    }

    public int size() {
        return this.bits.length * 64;
    }

    public int length() {
        if (this.longCount == 0) {
            return 0;
        }
        return ((this.longCount - 1) * 64) + (64 - Long.numberOfLeadingZeros(this.bits[this.longCount - 1]));
    }

    public String toString() {
        StringBuilder sb = new StringBuilder(this.longCount / 2);
        sb.append('{');
        boolean comma = false;
        for (int i = 0; i < this.longCount; i++) {
            if (this.bits[i] != 0) {
                for (int j = 0; j < 64; j++) {
                    if ((this.bits[i] & (1 << j)) != 0) {
                        if (comma) {
                            sb.append(", ");
                        } else {
                            comma = true;
                        }
                        sb.append((i * 64) + j);
                    }
                }
            }
        }
        sb.append('}');
        return sb.toString();
    }

    public int nextSetBit(int index) {
        checkIndex(index);
        int arrayIndex = index / 64;
        if (arrayIndex >= this.longCount) {
            return -1;
        }
        long mask = (-1) << index;
        if ((this.bits[arrayIndex] & mask) != 0) {
            return (arrayIndex * 64) + Long.numberOfTrailingZeros(this.bits[arrayIndex] & mask);
        }
        do {
            arrayIndex++;
            if (arrayIndex >= this.longCount) {
                break;
            }
        } while (this.bits[arrayIndex] == 0);
        if (arrayIndex != this.longCount) {
            return (arrayIndex * 64) + Long.numberOfTrailingZeros(this.bits[arrayIndex]);
        }
        return -1;
    }

    public int nextClearBit(int index) {
        checkIndex(index);
        int arrayIndex = index / 64;
        if (arrayIndex < this.longCount) {
            long mask = (-1) << index;
            if (((this.bits[arrayIndex] ^ (-1)) & mask) != 0) {
                return (arrayIndex * 64) + Long.numberOfTrailingZeros((this.bits[arrayIndex] ^ (-1)) & mask);
            }
            do {
                arrayIndex++;
                if (arrayIndex >= this.longCount) {
                    break;
                }
            } while (this.bits[arrayIndex] == -1);
            if (arrayIndex == this.longCount) {
                return this.longCount * 64;
            }
            return (arrayIndex * 64) + Long.numberOfTrailingZeros(this.bits[arrayIndex] ^ (-1));
        }
        return index;
    }

    public int previousSetBit(int index) {
        if (index == -1) {
            return -1;
        }
        checkIndex(index);
        for (int i = index; i >= 0; i--) {
            if (get(i)) {
                return i;
            }
        }
        return -1;
    }

    public int previousClearBit(int index) {
        if (index == -1) {
            return -1;
        }
        checkIndex(index);
        for (int i = index; i >= 0; i--) {
            if (!get(i)) {
                return i;
            }
        }
        return -1;
    }

    public boolean isEmpty() {
        return this.longCount == 0;
    }

    public int cardinality() {
        int result = 0;
        for (int i = 0; i < this.longCount; i++) {
            result += Long.bitCount(this.bits[i]);
        }
        return result;
    }

    public static BitSet valueOf(long[] longs) {
        return new BitSet((long[]) longs.clone());
    }

    public static BitSet valueOf(LongBuffer longBuffer) {
        long[] longs = new long[longBuffer.remaining()];
        for (int i = 0; i < longs.length; i++) {
            longs[i] = longBuffer.get(longBuffer.position() + i);
        }
        return valueOf(longs);
    }

    public static BitSet valueOf(byte[] bytes) {
        return valueOf(ByteBuffer.wrap(bytes));
    }

    public static BitSet valueOf(ByteBuffer byteBuffer) {
        ByteBuffer byteBuffer2 = byteBuffer.slice().order(ByteOrder.LITTLE_ENDIAN);
        long[] longs = arrayForBits(byteBuffer2.remaining() * 8);
        int i = 0;
        while (byteBuffer2.remaining() >= 8) {
            longs[i] = byteBuffer2.getLong();
            i++;
        }
        int j = 0;
        while (byteBuffer2.hasRemaining()) {
            longs[i] = longs[i] | ((((long) byteBuffer2.get()) & 255) << (j * 8));
            j++;
        }
        return valueOf(longs);
    }

    public long[] toLongArray() {
        return Arrays.copyOf(this.bits, this.longCount);
    }

    public byte[] toByteArray() {
        int bitCount = length();
        byte[] result = new byte[(bitCount + 7) / 8];
        for (int i = 0; i < result.length; i++) {
            int lowBit = i * 8;
            int arrayIndex = lowBit / 64;
            result[i] = (byte) (this.bits[arrayIndex] >>> lowBit);
        }
        return result;
    }

    private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
        ois.defaultReadObject();
        this.longCount = this.bits.length;
        shrinkSize();
    }
}
