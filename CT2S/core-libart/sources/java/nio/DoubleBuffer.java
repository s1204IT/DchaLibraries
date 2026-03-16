package java.nio;

import java.util.Arrays;

public abstract class DoubleBuffer extends Buffer implements Comparable<DoubleBuffer> {
    public abstract DoubleBuffer asReadOnlyBuffer();

    public abstract DoubleBuffer compact();

    public abstract DoubleBuffer duplicate();

    public abstract double get();

    public abstract double get(int i);

    @Override
    public abstract boolean isDirect();

    public abstract ByteOrder order();

    abstract double[] protectedArray();

    abstract int protectedArrayOffset();

    abstract boolean protectedHasArray();

    public abstract DoubleBuffer put(double d);

    public abstract DoubleBuffer put(int i, double d);

    public abstract DoubleBuffer slice();

    public static DoubleBuffer allocate(int capacity) {
        if (capacity < 0) {
            throw new IllegalArgumentException("capacity < 0: " + capacity);
        }
        return new DoubleArrayBuffer(new double[capacity]);
    }

    public static DoubleBuffer wrap(double[] array) {
        return wrap(array, 0, array.length);
    }

    public static DoubleBuffer wrap(double[] array, int start, int doubleCount) {
        Arrays.checkOffsetAndCount(array.length, start, doubleCount);
        DoubleBuffer buf = new DoubleArrayBuffer(array);
        buf.position = start;
        buf.limit = start + doubleCount;
        return buf;
    }

    DoubleBuffer(int capacity, long effectiveDirectAddress) {
        super(3, capacity, effectiveDirectAddress);
    }

    @Override
    public final double[] array() {
        return protectedArray();
    }

    @Override
    public final int arrayOffset() {
        return protectedArrayOffset();
    }

    @Override
    public int compareTo(DoubleBuffer otherBuffer) {
        int thisPos = this.position;
        int otherPos = otherBuffer.position;
        for (int compareRemaining = remaining() < otherBuffer.remaining() ? remaining() : otherBuffer.remaining(); compareRemaining > 0; compareRemaining--) {
            double thisDouble = get(thisPos);
            double otherDouble = otherBuffer.get(otherPos);
            if (thisDouble != otherDouble && (thisDouble == thisDouble || otherDouble == otherDouble)) {
                return thisDouble < otherDouble ? -1 : 1;
            }
            thisPos++;
            otherPos++;
        }
        return remaining() - otherBuffer.remaining();
    }

    public boolean equals(Object other) {
        if (!(other instanceof DoubleBuffer)) {
            return false;
        }
        DoubleBuffer otherBuffer = (DoubleBuffer) other;
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
            double a = get(myPosition2);
            int otherPosition3 = otherPosition2 + 1;
            double b = otherBuffer.get(otherPosition2);
            equalSoFar = a == b || !(a == a || b == b);
            otherPosition2 = otherPosition3;
            myPosition2 = myPosition3;
        }
        return equalSoFar;
    }

    public DoubleBuffer get(double[] dst) {
        return get(dst, 0, dst.length);
    }

    public DoubleBuffer get(double[] dst, int dstOffset, int doubleCount) {
        Arrays.checkOffsetAndCount(dst.length, dstOffset, doubleCount);
        if (doubleCount > remaining()) {
            throw new BufferUnderflowException();
        }
        for (int i = dstOffset; i < dstOffset + doubleCount; i++) {
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
            long l = Double.doubleToLongBits(get(myPosition));
            hash = (((int) l) + hash) ^ ((int) (l >> 32));
        }
        return hash;
    }

    public final DoubleBuffer put(double[] src) {
        return put(src, 0, src.length);
    }

    public DoubleBuffer put(double[] src, int srcOffset, int doubleCount) {
        Arrays.checkOffsetAndCount(src.length, srcOffset, doubleCount);
        if (doubleCount > remaining()) {
            throw new BufferOverflowException();
        }
        for (int i = srcOffset; i < srcOffset + doubleCount; i++) {
            put(src[i]);
        }
        return this;
    }

    public DoubleBuffer put(DoubleBuffer src) {
        if (isReadOnly()) {
            throw new ReadOnlyBufferException();
        }
        if (src == this) {
            throw new IllegalArgumentException("src == this");
        }
        if (src.remaining() > remaining()) {
            throw new BufferOverflowException();
        }
        double[] doubles = new double[src.remaining()];
        src.get(doubles);
        put(doubles);
        return this;
    }
}
