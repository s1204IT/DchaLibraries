package java.nio;

import java.util.Arrays;

public abstract class FloatBuffer extends Buffer implements Comparable<FloatBuffer> {
    public abstract FloatBuffer asReadOnlyBuffer();

    public abstract FloatBuffer compact();

    public abstract FloatBuffer duplicate();

    public abstract float get();

    public abstract float get(int i);

    @Override
    public abstract boolean isDirect();

    public abstract ByteOrder order();

    abstract float[] protectedArray();

    abstract int protectedArrayOffset();

    abstract boolean protectedHasArray();

    public abstract FloatBuffer put(float f);

    public abstract FloatBuffer put(int i, float f);

    public abstract FloatBuffer slice();

    public static FloatBuffer allocate(int capacity) {
        if (capacity < 0) {
            throw new IllegalArgumentException("capacity < 0: " + capacity);
        }
        return new FloatArrayBuffer(new float[capacity]);
    }

    public static FloatBuffer wrap(float[] array) {
        return wrap(array, 0, array.length);
    }

    public static FloatBuffer wrap(float[] array, int start, int floatCount) {
        Arrays.checkOffsetAndCount(array.length, start, floatCount);
        FloatBuffer buf = new FloatArrayBuffer(array);
        buf.position = start;
        buf.limit = start + floatCount;
        return buf;
    }

    FloatBuffer(int capacity, long effectiveDirectAddress) {
        super(2, capacity, effectiveDirectAddress);
    }

    @Override
    public final float[] array() {
        return protectedArray();
    }

    @Override
    public final int arrayOffset() {
        return protectedArrayOffset();
    }

    @Override
    public int compareTo(FloatBuffer otherBuffer) {
        int thisPos = this.position;
        int otherPos = otherBuffer.position;
        for (int compareRemaining = remaining() < otherBuffer.remaining() ? remaining() : otherBuffer.remaining(); compareRemaining > 0; compareRemaining--) {
            float thisFloat = get(thisPos);
            float otherFloat = otherBuffer.get(otherPos);
            if (thisFloat != otherFloat && (thisFloat == thisFloat || otherFloat == otherFloat)) {
                return thisFloat < otherFloat ? -1 : 1;
            }
            thisPos++;
            otherPos++;
        }
        return remaining() - otherBuffer.remaining();
    }

    public boolean equals(Object other) {
        if (!(other instanceof FloatBuffer)) {
            return false;
        }
        FloatBuffer otherBuffer = (FloatBuffer) other;
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
            float a = get(myPosition2);
            int otherPosition3 = otherPosition2 + 1;
            float b = otherBuffer.get(otherPosition2);
            equalSoFar = a == b || !(a == a || b == b);
            otherPosition2 = otherPosition3;
            myPosition2 = myPosition3;
        }
        return equalSoFar;
    }

    public FloatBuffer get(float[] dst) {
        return get(dst, 0, dst.length);
    }

    public FloatBuffer get(float[] dst, int dstOffset, int floatCount) {
        Arrays.checkOffsetAndCount(dst.length, dstOffset, floatCount);
        if (floatCount > remaining()) {
            throw new BufferUnderflowException();
        }
        for (int i = dstOffset; i < dstOffset + floatCount; i++) {
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
            hash += Float.floatToIntBits(get(myPosition));
        }
        return hash;
    }

    public final FloatBuffer put(float[] src) {
        return put(src, 0, src.length);
    }

    public FloatBuffer put(float[] src, int srcOffset, int floatCount) {
        Arrays.checkOffsetAndCount(src.length, srcOffset, floatCount);
        if (floatCount > remaining()) {
            throw new BufferOverflowException();
        }
        for (int i = srcOffset; i < srcOffset + floatCount; i++) {
            put(src[i]);
        }
        return this;
    }

    public FloatBuffer put(FloatBuffer src) {
        if (isReadOnly()) {
            throw new ReadOnlyBufferException();
        }
        if (src == this) {
            throw new IllegalArgumentException("src == this");
        }
        if (src.remaining() > remaining()) {
            throw new BufferOverflowException();
        }
        float[] contents = new float[src.remaining()];
        src.get(contents);
        put(contents);
        return this;
    }
}
