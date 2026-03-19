package android.icu.impl.coll;

public final class UVector64 {
    private long[] buffer = new long[32];
    private int length = 0;

    public boolean isEmpty() {
        return this.length == 0;
    }

    public int size() {
        return this.length;
    }

    public long elementAti(int i) {
        return this.buffer[i];
    }

    public long[] getBuffer() {
        return this.buffer;
    }

    public void addElement(long j) {
        ensureAppendCapacity();
        long[] jArr = this.buffer;
        int i = this.length;
        this.length = i + 1;
        jArr[i] = j;
    }

    public void setElementAt(long j, int i) {
        this.buffer[i] = j;
    }

    public void insertElementAt(long j, int i) {
        ensureAppendCapacity();
        System.arraycopy(this.buffer, i, this.buffer, i + 1, this.length - i);
        this.buffer[i] = j;
        this.length++;
    }

    public void removeAllElements() {
        this.length = 0;
    }

    private void ensureAppendCapacity() {
        int i;
        int length;
        if (this.length >= this.buffer.length) {
            if (this.buffer.length <= 65535) {
                i = 4;
                length = this.buffer.length;
            } else {
                i = 2;
                length = this.buffer.length;
            }
            long[] jArr = new long[i * length];
            System.arraycopy(this.buffer, 0, jArr, 0, this.length);
            this.buffer = jArr;
        }
    }
}
