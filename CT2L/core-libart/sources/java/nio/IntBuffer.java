package java.nio;

import java.util.Arrays;

public abstract class IntBuffer extends Buffer implements Comparable<IntBuffer> {
    public abstract IntBuffer asReadOnlyBuffer();

    public abstract IntBuffer compact();

    public abstract IntBuffer duplicate();

    public abstract int get();

    public abstract int get(int i);

    @Override
    public abstract boolean isDirect();

    public abstract ByteOrder order();

    abstract int[] protectedArray();

    abstract int protectedArrayOffset();

    abstract boolean protectedHasArray();

    public abstract IntBuffer put(int i);

    public abstract IntBuffer put(int i, int i2);

    public abstract IntBuffer slice();

    public static IntBuffer allocate(int capacity) {
        if (capacity < 0) {
            throw new IllegalArgumentException("capacity < 0: " + capacity);
        }
        return new IntArrayBuffer(new int[capacity]);
    }

    public static IntBuffer wrap(int[] array) {
        return wrap(array, 0, array.length);
    }

    public static IntBuffer wrap(int[] array, int start, int intCount) {
        Arrays.checkOffsetAndCount(array.length, start, intCount);
        IntBuffer buf = new IntArrayBuffer(array);
        buf.position = start;
        buf.limit = start + intCount;
        return buf;
    }

    IntBuffer(int capacity, long effectiveDirectAddress) {
        super(2, capacity, effectiveDirectAddress);
    }

    @Override
    public final int[] array() {
        return protectedArray();
    }

    @Override
    public final int arrayOffset() {
        return protectedArrayOffset();
    }

    @Override
    public int compareTo(IntBuffer otherBuffer) {
        int thisPos = this.position;
        int otherPos = otherBuffer.position;
        for (int compareRemaining = remaining() < otherBuffer.remaining() ? remaining() : otherBuffer.remaining(); compareRemaining > 0; compareRemaining--) {
            int thisInt = get(thisPos);
            int otherInt = otherBuffer.get(otherPos);
            if (thisInt != otherInt) {
                return thisInt < otherInt ? -1 : 1;
            }
            thisPos++;
            otherPos++;
        }
        return remaining() - otherBuffer.remaining();
    }

    public boolean equals(Object other) {
        if (!(other instanceof IntBuffer)) {
            return false;
        }
        IntBuffer otherBuffer = (IntBuffer) other;
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

    public IntBuffer get(int[] dst) {
        return get(dst, 0, dst.length);
    }

    public IntBuffer get(int[] dst, int dstOffset, int intCount) {
        Arrays.checkOffsetAndCount(dst.length, dstOffset, intCount);
        if (intCount > remaining()) {
            throw new BufferUnderflowException();
        }
        for (int i = dstOffset; i < dstOffset + intCount; i++) {
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
            hash += get(myPosition);
        }
        return hash;
    }

    public final IntBuffer put(int[] src) {
        return put(src, 0, src.length);
    }

    public IntBuffer put(int[] src, int srcOffset, int intCount) {
        if (isReadOnly()) {
            throw new ReadOnlyBufferException();
        }
        Arrays.checkOffsetAndCount(src.length, srcOffset, intCount);
        if (intCount > remaining()) {
            throw new BufferOverflowException();
        }
        for (int i = srcOffset; i < srcOffset + intCount; i++) {
            put(src[i]);
        }
        return this;
    }

    public IntBuffer put(IntBuffer src) {
        if (isReadOnly()) {
            throw new ReadOnlyBufferException();
        }
        if (src == this) {
            throw new IllegalArgumentException("src == this");
        }
        if (src.remaining() > remaining()) {
            throw new BufferOverflowException();
        }
        int[] contents = new int[src.remaining()];
        src.get(contents);
        put(contents);
        return this;
    }
}
