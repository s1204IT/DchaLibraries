package java.util.stream;

abstract class AbstractSpinedBuffer {
    public static final int MAX_CHUNK_POWER = 30;
    public static final int MIN_CHUNK_POWER = 4;
    public static final int MIN_CHUNK_SIZE = 16;
    public static final int MIN_SPINE_SIZE = 8;
    protected int elementIndex;
    protected final int initialChunkPower;
    protected long[] priorElementCount;
    protected int spineIndex;

    public abstract void clear();

    protected AbstractSpinedBuffer() {
        this.initialChunkPower = 4;
    }

    protected AbstractSpinedBuffer(int i) {
        if (i < 0) {
            throw new IllegalArgumentException("Illegal Capacity: " + i);
        }
        this.initialChunkPower = Math.max(4, 32 - Integer.numberOfLeadingZeros(i - 1));
    }

    public boolean isEmpty() {
        return this.spineIndex == 0 && this.elementIndex == 0;
    }

    public long count() {
        if (this.spineIndex == 0) {
            return this.elementIndex;
        }
        return this.priorElementCount[this.spineIndex] + ((long) this.elementIndex);
    }

    protected int chunkSize(int i) {
        int iMin;
        if (i != 0 && i != 1) {
            iMin = Math.min((this.initialChunkPower + i) - 1, 30);
        } else {
            iMin = this.initialChunkPower;
        }
        return 1 << iMin;
    }
}
