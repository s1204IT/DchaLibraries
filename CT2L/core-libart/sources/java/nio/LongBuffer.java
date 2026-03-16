package java.nio;

import java.util.Arrays;

public abstract class LongBuffer extends Buffer implements Comparable<LongBuffer> {
    public abstract LongBuffer asReadOnlyBuffer();

    public abstract LongBuffer compact();

    public abstract LongBuffer duplicate();

    public abstract long get();

    public abstract long get(int i);

    @Override
    public abstract boolean isDirect();

    public abstract ByteOrder order();

    abstract long[] protectedArray();

    abstract int protectedArrayOffset();

    abstract boolean protectedHasArray();

    public abstract LongBuffer put(int i, long j);

    public abstract LongBuffer put(long j);

    public abstract LongBuffer slice();

    public static LongBuffer allocate(int capacity) {
        if (capacity < 0) {
            throw new IllegalArgumentException("capacity < 0: " + capacity);
        }
        return new LongArrayBuffer(new long[capacity]);
    }

    public static LongBuffer wrap(long[] array) {
        return wrap(array, 0, array.length);
    }

    public static LongBuffer wrap(long[] array, int start, int longCount) {
        Arrays.checkOffsetAndCount(array.length, start, longCount);
        LongBuffer buf = new LongArrayBuffer(array);
        buf.position = start;
        buf.limit = start + longCount;
        return buf;
    }

    LongBuffer(int capacity, long effectiveDirectAddress) {
        super(3, capacity, effectiveDirectAddress);
    }

    @Override
    public final long[] array() {
        return protectedArray();
    }

    @Override
    public final int arrayOffset() {
        return protectedArrayOffset();
    }

    @Override
    public int compareTo(LongBuffer otherBuffer) {
        int thisPos = this.position;
        int otherPos = otherBuffer.position;
        for (int compareRemaining = remaining() < otherBuffer.remaining() ? remaining() : otherBuffer.remaining(); compareRemaining > 0; compareRemaining--) {
            long thisLong = get(thisPos);
            long otherLong = otherBuffer.get(otherPos);
            if (thisLong != otherLong) {
                return thisLong < otherLong ? -1 : 1;
            }
            thisPos++;
            otherPos++;
        }
        return remaining() - otherBuffer.remaining();
    }

    public boolean equals(Object other) {
        if (!(other instanceof LongBuffer)) {
            return false;
        }
        LongBuffer otherBuffer = (LongBuffer) other;
        if (remaining() != otherBuffer.remaining()) {
            return false;
        }
        int myPosition = this.position;
        int otherPosition = otherBuffer.position;
        boolean equalSoFar = true;
        int otherPosition2 = otherPosition;
        int myPosition2 = myPosition;
        while (equalSoFar && myPosition2 < this.limit) {
            int myPosition3 = myPosition2 + 1;
            int otherPosition3 = otherPosition2 + 1;
            equalSoFar = get(myPosition2) == otherBuffer.get(otherPosition2);
            otherPosition2 = otherPosition3;
            myPosition2 = myPosition3;
        }
        return equalSoFar;
    }

    public LongBuffer get(long[] dst) {
        return get(dst, 0, dst.length);
    }

    public LongBuffer get(long[] dst, int dstOffset, int longCount) {
        Arrays.checkOffsetAndCount(dst.length, dstOffset, longCount);
        if (longCount > remaining()) {
            throw new BufferUnderflowException();
        }
        for (int i = dstOffset; i < dstOffset + longCount; i++) {
            dst[i] = get();
        }
        return this;
    }

    @Override
    public final boolean hasArray() {
        return protectedHasArray();
    }

    public int hashCode() {
        int hash = 0;
        for (int myPosition = this.position; myPosition < this.limit; myPosition++) {
            long l = get(myPosition);
            hash = (((int) l) + hash) ^ ((int) (l >> 32));
        }
        return hash;
    }

    public final LongBuffer put(long[] src) {
        return put(src, 0, src.length);
    }

    public LongBuffer put(long[] src, int srcOffset, int longCount) {
        Arrays.checkOffsetAndCount(src.length, srcOffset, longCount);
        if (longCount > remaining()) {
            throw new BufferOverflowException();
        }
        for (int i = srcOffset; i < srcOffset + longCount; i++) {
            put(src[i]);
        }
        return this;
    }

    public LongBuffer put(LongBuffer src) {
        if (isReadOnly()) {
            throw new ReadOnlyBufferException();
        }
        if (src == this) {
            throw new IllegalArgumentException("src == this");
        }
        if (src.remaining() > remaining()) {
            throw new BufferOverflowException();
        }
        long[] contents = new long[src.remaining()];
        src.get(contents);
        put(contents);
        return this;
    }
}
