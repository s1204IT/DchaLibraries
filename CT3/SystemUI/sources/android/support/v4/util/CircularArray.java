package android.support.v4.util;

public final class CircularArray<E> {
    private int mCapacityBitmask;
    private E[] mElements;
    private int mHead;
    private int mTail;

    private void doubleCapacity() {
        int length = this.mElements.length;
        int i = length - this.mHead;
        int i2 = length << 1;
        if (i2 < 0) {
            throw new RuntimeException("Max array capacity exceeded");
        }
        E[] eArr = (E[]) new Object[i2];
        System.arraycopy(this.mElements, this.mHead, eArr, 0, i);
        System.arraycopy(this.mElements, 0, eArr, i, this.mHead);
        this.mElements = eArr;
        this.mHead = 0;
        this.mTail = length;
        this.mCapacityBitmask = i2 - 1;
    }

    public CircularArray() {
        this(8);
    }

    public CircularArray(int i) {
        int iHighestOneBit;
        if (i < 1) {
            throw new IllegalArgumentException("capacity must be >= 1");
        }
        if (i > 1073741824) {
            throw new IllegalArgumentException("capacity must be <= 2^30");
        }
        if (Integer.bitCount(i) != 1) {
            iHighestOneBit = Integer.highestOneBit(i - 1) << 1;
        } else {
            iHighestOneBit = i;
        }
        this.mCapacityBitmask = iHighestOneBit - 1;
        this.mElements = (E[]) new Object[iHighestOneBit];
    }

    public void addFirst(E e) {
        this.mHead = (this.mHead - 1) & this.mCapacityBitmask;
        this.mElements[this.mHead] = e;
        if (this.mHead != this.mTail) {
            return;
        }
        doubleCapacity();
    }

    public void addLast(E e) {
        this.mElements[this.mTail] = e;
        this.mTail = (this.mTail + 1) & this.mCapacityBitmask;
        if (this.mTail != this.mHead) {
            return;
        }
        doubleCapacity();
    }

    public void clear() {
        removeFromStart(size());
    }

    public void removeFromStart(int numOfElements) {
        if (numOfElements <= 0) {
            return;
        }
        if (numOfElements > size()) {
            throw new ArrayIndexOutOfBoundsException();
        }
        int end = this.mElements.length;
        if (numOfElements < end - this.mHead) {
            end = this.mHead + numOfElements;
        }
        for (int i = this.mHead; i < end; i++) {
            this.mElements[i] = null;
        }
        int removed = end - this.mHead;
        int numOfElements2 = numOfElements - removed;
        this.mHead = (this.mHead + removed) & this.mCapacityBitmask;
        if (numOfElements2 <= 0) {
            return;
        }
        for (int i2 = 0; i2 < numOfElements2; i2++) {
            this.mElements[i2] = null;
        }
        this.mHead = numOfElements2;
    }

    public void removeFromEnd(int numOfElements) {
        if (numOfElements <= 0) {
            return;
        }
        if (numOfElements > size()) {
            throw new ArrayIndexOutOfBoundsException();
        }
        int start = 0;
        if (numOfElements < this.mTail) {
            start = this.mTail - numOfElements;
        }
        for (int i = start; i < this.mTail; i++) {
            this.mElements[i] = null;
        }
        int removed = this.mTail - start;
        int numOfElements2 = numOfElements - removed;
        this.mTail -= removed;
        if (numOfElements2 <= 0) {
            return;
        }
        this.mTail = this.mElements.length;
        int newTail = this.mTail - numOfElements2;
        for (int i2 = newTail; i2 < this.mTail; i2++) {
            this.mElements[i2] = null;
        }
        this.mTail = newTail;
    }

    public E get(int n) {
        if (n < 0 || n >= size()) {
            throw new ArrayIndexOutOfBoundsException();
        }
        return this.mElements[(this.mHead + n) & this.mCapacityBitmask];
    }

    public int size() {
        return (this.mTail - this.mHead) & this.mCapacityBitmask;
    }
}
