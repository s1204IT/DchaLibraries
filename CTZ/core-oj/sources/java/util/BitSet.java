package java.util;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamField;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.LongBuffer;
import java.util.PrimitiveIterator;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

public class BitSet implements Cloneable, Serializable {
    static final boolean $assertionsDisabled = false;
    private static final int ADDRESS_BITS_PER_WORD = 6;
    private static final int BITS_PER_WORD = 64;
    private static final int BIT_INDEX_MASK = 63;
    private static final long WORD_MASK = -1;
    private static final ObjectStreamField[] serialPersistentFields = {new ObjectStreamField("bits", long[].class)};
    private static final long serialVersionUID = 7997698588986878753L;
    private transient boolean sizeIsSticky;
    private long[] words;
    private transient int wordsInUse;

    private static int wordIndex(int i) {
        return i >> 6;
    }

    private void checkInvariants() {
    }

    private void recalculateWordsInUse() {
        int i = this.wordsInUse - 1;
        while (i >= 0 && this.words[i] == 0) {
            i--;
        }
        this.wordsInUse = i + 1;
    }

    public BitSet() {
        this.wordsInUse = 0;
        this.sizeIsSticky = $assertionsDisabled;
        initWords(64);
        this.sizeIsSticky = $assertionsDisabled;
    }

    public BitSet(int i) {
        this.wordsInUse = 0;
        this.sizeIsSticky = $assertionsDisabled;
        if (i < 0) {
            throw new NegativeArraySizeException("nbits < 0: " + i);
        }
        initWords(i);
        this.sizeIsSticky = true;
    }

    private void initWords(int i) {
        this.words = new long[wordIndex(i - 1) + 1];
    }

    private BitSet(long[] jArr) {
        this.wordsInUse = 0;
        this.sizeIsSticky = $assertionsDisabled;
        this.words = jArr;
        this.wordsInUse = jArr.length;
        checkInvariants();
    }

    public static BitSet valueOf(long[] jArr) {
        int length = jArr.length;
        while (length > 0 && jArr[length - 1] == 0) {
            length--;
        }
        return new BitSet(Arrays.copyOf(jArr, length));
    }

    public static BitSet valueOf(LongBuffer longBuffer) {
        LongBuffer longBufferSlice = longBuffer.slice();
        int iRemaining = longBufferSlice.remaining();
        while (iRemaining > 0 && longBufferSlice.get(iRemaining - 1) == 0) {
            iRemaining--;
        }
        long[] jArr = new long[iRemaining];
        longBufferSlice.get(jArr);
        return new BitSet(jArr);
    }

    public static BitSet valueOf(byte[] bArr) {
        return valueOf(ByteBuffer.wrap(bArr));
    }

    public static BitSet valueOf(ByteBuffer byteBuffer) {
        ByteBuffer byteBufferOrder = byteBuffer.slice().order(ByteOrder.LITTLE_ENDIAN);
        int iRemaining = byteBufferOrder.remaining();
        while (iRemaining > 0 && byteBufferOrder.get(iRemaining - 1) == 0) {
            iRemaining--;
        }
        long[] jArr = new long[(iRemaining + 7) / 8];
        byteBufferOrder.limit(iRemaining);
        int i = 0;
        while (byteBufferOrder.remaining() >= 8) {
            jArr[i] = byteBufferOrder.getLong();
            i++;
        }
        int iRemaining2 = byteBufferOrder.remaining();
        for (int i2 = 0; i2 < iRemaining2; i2++) {
            jArr[i] = jArr[i] | ((((long) byteBufferOrder.get()) & 255) << (8 * i2));
        }
        return new BitSet(jArr);
    }

    public byte[] toByteArray() {
        int i = this.wordsInUse;
        if (i == 0) {
            return new byte[0];
        }
        int i2 = i - 1;
        int i3 = 8 * i2;
        for (long j = this.words[i2]; j != 0; j >>>= 8) {
            i3++;
        }
        byte[] bArr = new byte[i3];
        ByteBuffer byteBufferOrder = ByteBuffer.wrap(bArr).order(ByteOrder.LITTLE_ENDIAN);
        for (int i4 = 0; i4 < i2; i4++) {
            byteBufferOrder.putLong(this.words[i4]);
        }
        for (long j2 = this.words[i2]; j2 != 0; j2 >>>= 8) {
            byteBufferOrder.put((byte) (255 & j2));
        }
        return bArr;
    }

    public long[] toLongArray() {
        return Arrays.copyOf(this.words, this.wordsInUse);
    }

    private void ensureCapacity(int i) {
        if (this.words.length < i) {
            this.words = Arrays.copyOf(this.words, Math.max(2 * this.words.length, i));
            this.sizeIsSticky = $assertionsDisabled;
        }
    }

    private void expandTo(int i) {
        int i2 = i + 1;
        if (this.wordsInUse < i2) {
            ensureCapacity(i2);
            this.wordsInUse = i2;
        }
    }

    private static void checkRange(int i, int i2) {
        if (i < 0) {
            throw new IndexOutOfBoundsException("fromIndex < 0: " + i);
        }
        if (i2 < 0) {
            throw new IndexOutOfBoundsException("toIndex < 0: " + i2);
        }
        if (i > i2) {
            throw new IndexOutOfBoundsException("fromIndex: " + i + " > toIndex: " + i2);
        }
    }

    public void flip(int i) {
        if (i < 0) {
            throw new IndexOutOfBoundsException("bitIndex < 0: " + i);
        }
        int iWordIndex = wordIndex(i);
        expandTo(iWordIndex);
        long[] jArr = this.words;
        jArr[iWordIndex] = jArr[iWordIndex] ^ (1 << i);
        recalculateWordsInUse();
        checkInvariants();
    }

    public void flip(int i, int i2) {
        checkRange(i, i2);
        if (i == i2) {
            return;
        }
        int iWordIndex = wordIndex(i);
        int iWordIndex2 = wordIndex(i2 - 1);
        expandTo(iWordIndex2);
        long j = (-1) << i;
        long j2 = (-1) >>> (-i2);
        if (iWordIndex == iWordIndex2) {
            long[] jArr = this.words;
            jArr[iWordIndex] = (j2 & j) ^ jArr[iWordIndex];
        } else {
            long[] jArr2 = this.words;
            jArr2[iWordIndex] = jArr2[iWordIndex] ^ j;
            while (true) {
                iWordIndex++;
                if (iWordIndex >= iWordIndex2) {
                    break;
                }
                long[] jArr3 = this.words;
                jArr3[iWordIndex] = ~jArr3[iWordIndex];
            }
            long[] jArr4 = this.words;
            jArr4[iWordIndex2] = j2 ^ jArr4[iWordIndex2];
        }
        recalculateWordsInUse();
        checkInvariants();
    }

    public void set(int i) {
        if (i < 0) {
            throw new IndexOutOfBoundsException("bitIndex < 0: " + i);
        }
        int iWordIndex = wordIndex(i);
        expandTo(iWordIndex);
        long[] jArr = this.words;
        jArr[iWordIndex] = jArr[iWordIndex] | (1 << i);
        checkInvariants();
    }

    public void set(int i, boolean z) {
        if (z) {
            set(i);
        } else {
            clear(i);
        }
    }

    public void set(int i, int i2) {
        checkRange(i, i2);
        if (i == i2) {
            return;
        }
        int iWordIndex = wordIndex(i);
        int iWordIndex2 = wordIndex(i2 - 1);
        expandTo(iWordIndex2);
        long j = (-1) << i;
        long j2 = (-1) >>> (-i2);
        if (iWordIndex == iWordIndex2) {
            long[] jArr = this.words;
            jArr[iWordIndex] = (j2 & j) | jArr[iWordIndex];
        } else {
            long[] jArr2 = this.words;
            jArr2[iWordIndex] = j | jArr2[iWordIndex];
            while (true) {
                iWordIndex++;
                if (iWordIndex >= iWordIndex2) {
                    break;
                } else {
                    this.words[iWordIndex] = -1;
                }
            }
            long[] jArr3 = this.words;
            jArr3[iWordIndex2] = j2 | jArr3[iWordIndex2];
        }
        checkInvariants();
    }

    public void set(int i, int i2, boolean z) {
        if (z) {
            set(i, i2);
        } else {
            clear(i, i2);
        }
    }

    public void clear(int i) {
        if (i < 0) {
            throw new IndexOutOfBoundsException("bitIndex < 0: " + i);
        }
        int iWordIndex = wordIndex(i);
        if (iWordIndex >= this.wordsInUse) {
            return;
        }
        long[] jArr = this.words;
        jArr[iWordIndex] = jArr[iWordIndex] & (~(1 << i));
        recalculateWordsInUse();
        checkInvariants();
    }

    public void clear(int i, int i2) {
        int iWordIndex;
        checkRange(i, i2);
        if (i == i2 || (iWordIndex = wordIndex(i)) >= this.wordsInUse) {
            return;
        }
        int iWordIndex2 = wordIndex(i2 - 1);
        if (iWordIndex2 >= this.wordsInUse) {
            i2 = length();
            iWordIndex2 = this.wordsInUse - 1;
        }
        long j = (-1) << i;
        long j2 = (-1) >>> (-i2);
        if (iWordIndex == iWordIndex2) {
            long[] jArr = this.words;
            jArr[iWordIndex] = (~(j2 & j)) & jArr[iWordIndex];
        } else {
            long[] jArr2 = this.words;
            jArr2[iWordIndex] = (~j) & jArr2[iWordIndex];
            while (true) {
                iWordIndex++;
                if (iWordIndex >= iWordIndex2) {
                    break;
                } else {
                    this.words[iWordIndex] = 0;
                }
            }
            long[] jArr3 = this.words;
            jArr3[iWordIndex2] = (~j2) & jArr3[iWordIndex2];
        }
        recalculateWordsInUse();
        checkInvariants();
    }

    public void clear() {
        while (this.wordsInUse > 0) {
            long[] jArr = this.words;
            int i = this.wordsInUse - 1;
            this.wordsInUse = i;
            jArr[i] = 0;
        }
    }

    public boolean get(int i) {
        if (i < 0) {
            throw new IndexOutOfBoundsException("bitIndex < 0: " + i);
        }
        checkInvariants();
        int iWordIndex = wordIndex(i);
        if (iWordIndex >= this.wordsInUse || (this.words[iWordIndex] & (1 << i)) == 0) {
            return $assertionsDisabled;
        }
        return true;
    }

    public BitSet get(int i, int i2) {
        int i3;
        long j;
        checkRange(i, i2);
        checkInvariants();
        int length = length();
        int i4 = 0;
        if (length <= i || i == i2) {
            return new BitSet(0);
        }
        if (i2 > length) {
            i2 = length;
        }
        int i5 = i2 - i;
        BitSet bitSet = new BitSet(i5);
        int iWordIndex = wordIndex(i5 - 1) + 1;
        int iWordIndex2 = wordIndex(i);
        int i6 = i & BIT_INDEX_MASK;
        boolean z = i6 == 0;
        while (true) {
            i3 = iWordIndex - 1;
            if (i4 >= i3) {
                break;
            }
            bitSet.words[i4] = z ? this.words[iWordIndex2] : (this.words[iWordIndex2] >>> i) | (this.words[iWordIndex2 + 1] << (-i));
            i4++;
            iWordIndex2++;
        }
        long j2 = (-1) >>> (-i2);
        long[] jArr = bitSet.words;
        if (((i2 - 1) & BIT_INDEX_MASK) < i6) {
            j = ((this.words[iWordIndex2 + 1] & j2) << (-i)) | (this.words[iWordIndex2] >>> i);
        } else {
            j = (this.words[iWordIndex2] & j2) >>> i;
        }
        jArr[i3] = j;
        bitSet.wordsInUse = iWordIndex;
        bitSet.recalculateWordsInUse();
        bitSet.checkInvariants();
        return bitSet;
    }

    public int nextSetBit(int i) {
        if (i < 0) {
            throw new IndexOutOfBoundsException("fromIndex < 0: " + i);
        }
        checkInvariants();
        int iWordIndex = wordIndex(i);
        if (iWordIndex >= this.wordsInUse) {
            return -1;
        }
        long j = this.words[iWordIndex] & ((-1) << i);
        while (j == 0) {
            iWordIndex++;
            if (iWordIndex == this.wordsInUse) {
                return -1;
            }
            j = this.words[iWordIndex];
        }
        return (iWordIndex * 64) + Long.numberOfTrailingZeros(j);
    }

    public int nextClearBit(int i) {
        if (i < 0) {
            throw new IndexOutOfBoundsException("fromIndex < 0: " + i);
        }
        checkInvariants();
        int iWordIndex = wordIndex(i);
        if (iWordIndex >= this.wordsInUse) {
            return i;
        }
        long j = (~this.words[iWordIndex]) & ((-1) << i);
        while (j == 0) {
            iWordIndex++;
            if (iWordIndex == this.wordsInUse) {
                return this.wordsInUse * 64;
            }
            j = ~this.words[iWordIndex];
        }
        return (iWordIndex * 64) + Long.numberOfTrailingZeros(j);
    }

    public int previousSetBit(int i) {
        if (i < 0) {
            if (i == -1) {
                return -1;
            }
            throw new IndexOutOfBoundsException("fromIndex < -1: " + i);
        }
        checkInvariants();
        int iWordIndex = wordIndex(i);
        if (iWordIndex >= this.wordsInUse) {
            return length() - 1;
        }
        long j = this.words[iWordIndex] & ((-1) >>> (-(i + 1)));
        while (j == 0) {
            int i2 = iWordIndex - 1;
            if (iWordIndex == 0) {
                return -1;
            }
            j = this.words[i2];
            iWordIndex = i2;
        }
        return (((iWordIndex + 1) * 64) - 1) - Long.numberOfLeadingZeros(j);
    }

    public int previousClearBit(int i) {
        if (i < 0) {
            if (i == -1) {
                return -1;
            }
            throw new IndexOutOfBoundsException("fromIndex < -1: " + i);
        }
        checkInvariants();
        int iWordIndex = wordIndex(i);
        if (iWordIndex >= this.wordsInUse) {
            return i;
        }
        long j = (~this.words[iWordIndex]) & ((-1) >>> (-(i + 1)));
        while (j == 0) {
            int i2 = iWordIndex - 1;
            if (iWordIndex == 0) {
                return -1;
            }
            j = ~this.words[i2];
            iWordIndex = i2;
        }
        return (((iWordIndex + 1) * 64) - 1) - Long.numberOfLeadingZeros(j);
    }

    public int length() {
        if (this.wordsInUse == 0) {
            return 0;
        }
        return ((this.wordsInUse - 1) * 64) + (64 - Long.numberOfLeadingZeros(this.words[this.wordsInUse - 1]));
    }

    public boolean isEmpty() {
        if (this.wordsInUse == 0) {
            return true;
        }
        return $assertionsDisabled;
    }

    public boolean intersects(BitSet bitSet) {
        for (int iMin = Math.min(this.wordsInUse, bitSet.wordsInUse) - 1; iMin >= 0; iMin--) {
            if ((this.words[iMin] & bitSet.words[iMin]) != 0) {
                return true;
            }
        }
        return $assertionsDisabled;
    }

    public int cardinality() {
        int iBitCount = 0;
        for (int i = 0; i < this.wordsInUse; i++) {
            iBitCount += Long.bitCount(this.words[i]);
        }
        return iBitCount;
    }

    public void and(BitSet bitSet) {
        if (this == bitSet) {
            return;
        }
        while (this.wordsInUse > bitSet.wordsInUse) {
            long[] jArr = this.words;
            int i = this.wordsInUse - 1;
            this.wordsInUse = i;
            jArr[i] = 0;
        }
        for (int i2 = 0; i2 < this.wordsInUse; i2++) {
            long[] jArr2 = this.words;
            jArr2[i2] = jArr2[i2] & bitSet.words[i2];
        }
        recalculateWordsInUse();
        checkInvariants();
    }

    public void or(BitSet bitSet) {
        if (this == bitSet) {
            return;
        }
        int iMin = Math.min(this.wordsInUse, bitSet.wordsInUse);
        if (this.wordsInUse < bitSet.wordsInUse) {
            ensureCapacity(bitSet.wordsInUse);
            this.wordsInUse = bitSet.wordsInUse;
        }
        for (int i = 0; i < iMin; i++) {
            long[] jArr = this.words;
            jArr[i] = jArr[i] | bitSet.words[i];
        }
        if (iMin < bitSet.wordsInUse) {
            System.arraycopy((Object) bitSet.words, iMin, (Object) this.words, iMin, this.wordsInUse - iMin);
        }
        checkInvariants();
    }

    public void xor(BitSet bitSet) {
        int iMin = Math.min(this.wordsInUse, bitSet.wordsInUse);
        if (this.wordsInUse < bitSet.wordsInUse) {
            ensureCapacity(bitSet.wordsInUse);
            this.wordsInUse = bitSet.wordsInUse;
        }
        for (int i = 0; i < iMin; i++) {
            long[] jArr = this.words;
            jArr[i] = jArr[i] ^ bitSet.words[i];
        }
        if (iMin < bitSet.wordsInUse) {
            System.arraycopy((Object) bitSet.words, iMin, (Object) this.words, iMin, bitSet.wordsInUse - iMin);
        }
        recalculateWordsInUse();
        checkInvariants();
    }

    public void andNot(BitSet bitSet) {
        for (int iMin = Math.min(this.wordsInUse, bitSet.wordsInUse) - 1; iMin >= 0; iMin--) {
            long[] jArr = this.words;
            jArr[iMin] = jArr[iMin] & (~bitSet.words[iMin]);
        }
        recalculateWordsInUse();
        checkInvariants();
    }

    public int hashCode() {
        int i = this.wordsInUse;
        long j = 1234;
        while (true) {
            i--;
            if (i >= 0) {
                j ^= this.words[i] * ((long) (i + 1));
            } else {
                return (int) ((j >> 32) ^ j);
            }
        }
    }

    public int size() {
        return this.words.length * 64;
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof BitSet)) {
            return $assertionsDisabled;
        }
        if (this == obj) {
            return true;
        }
        BitSet bitSet = (BitSet) obj;
        checkInvariants();
        bitSet.checkInvariants();
        if (this.wordsInUse != bitSet.wordsInUse) {
            return $assertionsDisabled;
        }
        for (int i = 0; i < this.wordsInUse; i++) {
            if (this.words[i] != bitSet.words[i]) {
                return $assertionsDisabled;
            }
        }
        return true;
    }

    public Object clone() {
        if (!this.sizeIsSticky) {
            trimToSize();
        }
        try {
            BitSet bitSet = (BitSet) super.clone();
            bitSet.words = (long[]) this.words.clone();
            bitSet.checkInvariants();
            return bitSet;
        } catch (CloneNotSupportedException e) {
            throw new InternalError(e);
        }
    }

    private void trimToSize() {
        if (this.wordsInUse != this.words.length) {
            this.words = Arrays.copyOf(this.words, this.wordsInUse);
            checkInvariants();
        }
    }

    private void writeObject(ObjectOutputStream objectOutputStream) throws IOException {
        checkInvariants();
        if (!this.sizeIsSticky) {
            trimToSize();
        }
        objectOutputStream.putFields().put("bits", this.words);
        objectOutputStream.writeFields();
    }

    private void readObject(ObjectInputStream objectInputStream) throws IOException, ClassNotFoundException {
        this.words = (long[]) objectInputStream.readFields().get("bits", (Object) null);
        this.wordsInUse = this.words.length;
        recalculateWordsInUse();
        this.sizeIsSticky = (this.words.length <= 0 || this.words[this.words.length - 1] != 0) ? $assertionsDisabled : true;
        checkInvariants();
    }

    public String toString() {
        checkInvariants();
        StringBuilder sb = new StringBuilder((6 * (this.wordsInUse > 128 ? cardinality() : this.wordsInUse * 64)) + 2);
        sb.append('{');
        int iNextSetBit = nextSetBit(0);
        if (iNextSetBit != -1) {
            sb.append(iNextSetBit);
            while (true) {
                int i = iNextSetBit + 1;
                if (i < 0 || (iNextSetBit = nextSetBit(i)) < 0) {
                    break;
                }
                int iNextClearBit = nextClearBit(iNextSetBit);
                do {
                    sb.append(", ");
                    sb.append(iNextSetBit);
                    iNextSetBit++;
                } while (iNextSetBit != iNextClearBit);
            }
        }
        sb.append('}');
        return sb.toString();
    }

    public IntStream stream() {
        return StreamSupport.intStream(new Supplier() {
            @Override
            public final Object get() {
                return Spliterators.spliterator(new PrimitiveIterator.OfInt() {
                    int next;

                    {
                        this.next = BitSet.this.nextSetBit(0);
                    }

                    @Override
                    public boolean hasNext() {
                        if (this.next != -1) {
                            return true;
                        }
                        return BitSet.$assertionsDisabled;
                    }

                    @Override
                    public int nextInt() {
                        if (this.next != -1) {
                            int i = this.next;
                            this.next = BitSet.this.nextSetBit(this.next + 1);
                            return i;
                        }
                        throw new NoSuchElementException();
                    }
                }, (long) r0.cardinality(), 21);
            }
        }, 16469, $assertionsDisabled);
    }
}
