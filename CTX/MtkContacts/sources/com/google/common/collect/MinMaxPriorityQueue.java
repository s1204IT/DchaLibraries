package com.google.common.collect;

import com.google.common.base.Preconditions;
import com.google.common.math.IntMath;
import java.util.AbstractQueue;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Queue;

public final class MinMaxPriorityQueue<E> extends AbstractQueue<E> {
    private static final int DEFAULT_CAPACITY = 11;
    private static final int EVEN_POWERS_OF_TWO = 1431655765;
    private static final int ODD_POWERS_OF_TWO = -1431655766;

    private final Heap maxHeap;
    final int maximumSize;

    private final Heap minHeap;
    private int modCount;
    private Object[] queue;
    private int size;

    public static <E extends Comparable<E>> MinMaxPriorityQueue<E> create() {
        return new Builder(Ordering.natural()).create();
    }

    public static <E extends Comparable<E>> MinMaxPriorityQueue<E> create(Iterable<? extends E> iterable) {
        return new Builder(Ordering.natural()).create(iterable);
    }

    public static <B> Builder<B> orderedBy(Comparator<B> comparator) {
        return new Builder<>(comparator);
    }

    public static Builder<Comparable> expectedSize(int i) {
        return new Builder(Ordering.natural()).expectedSize(i);
    }

    public static Builder<Comparable> maximumSize(int i) {
        return new Builder(Ordering.natural()).maximumSize(i);
    }

    public static final class Builder<B> {
        private static final int UNSET_EXPECTED_SIZE = -1;
        private final Comparator<B> comparator;
        private int expectedSize;
        private int maximumSize;

        private Builder(Comparator<B> comparator) {
            this.expectedSize = -1;
            this.maximumSize = Integer.MAX_VALUE;
            this.comparator = (Comparator) Preconditions.checkNotNull(comparator);
        }

        public Builder<B> expectedSize(int i) {
            Preconditions.checkArgument(i >= 0);
            this.expectedSize = i;
            return this;
        }

        public Builder<B> maximumSize(int i) {
            Preconditions.checkArgument(i > 0);
            this.maximumSize = i;
            return this;
        }

        public <T extends B> MinMaxPriorityQueue<T> create() {
            return create(Collections.emptySet());
        }

        public <T extends B> MinMaxPriorityQueue<T> create(Iterable<? extends T> iterable) {
            MinMaxPriorityQueue<T> minMaxPriorityQueue = new MinMaxPriorityQueue<>(this, MinMaxPriorityQueue.initialQueueSize(this.expectedSize, this.maximumSize, iterable));
            Iterator<? extends T> it = iterable.iterator();
            while (it.hasNext()) {
                minMaxPriorityQueue.offer(it.next());
            }
            return minMaxPriorityQueue;
        }

        private <T extends B> Ordering<T> ordering() {
            return Ordering.from(this.comparator);
        }
    }

    private MinMaxPriorityQueue(Builder<? super E> builder, int i) {
        Ordering ordering = builder.ordering();
        this.minHeap = new Heap(ordering);
        this.maxHeap = new Heap(ordering.reverse());
        this.minHeap.otherHeap = this.maxHeap;
        this.maxHeap.otherHeap = this.minHeap;
        this.maximumSize = ((Builder) builder).maximumSize;
        this.queue = new Object[i];
    }

    @Override
    public int size() {
        return this.size;
    }

    @Override
    public boolean add(E e) {
        offer(e);
        return true;
    }

    @Override
    public boolean addAll(Collection<? extends E> collection) {
        Iterator<? extends E> it = collection.iterator();
        boolean z = false;
        while (it.hasNext()) {
            offer(it.next());
            z = true;
        }
        return z;
    }

    @Override
    public boolean offer(E e) {
        Preconditions.checkNotNull(e);
        this.modCount++;
        int i = this.size;
        this.size = i + 1;
        growIfNeeded();
        heapForIndex(i).bubbleUp(i, e);
        return this.size <= this.maximumSize || pollLast() != e;
    }

    @Override
    public E poll() {
        if (isEmpty()) {
            return null;
        }
        return removeAndGet(0);
    }

    E elementData(int i) {
        return (E) this.queue[i];
    }

    @Override
    public E peek() {
        if (isEmpty()) {
            return null;
        }
        return elementData(0);
    }

    private int getMaxElementIndex() {
        switch (this.size) {
            case 1:
                return 0;
            case 2:
                return 1;
            default:
                return this.maxHeap.compareElements(1, 2) <= 0 ? 1 : 2;
        }
    }

    public E pollFirst() {
        return poll();
    }

    public E removeFirst() {
        return remove();
    }

    public E peekFirst() {
        return peek();
    }

    public E pollLast() {
        if (isEmpty()) {
            return null;
        }
        return removeAndGet(getMaxElementIndex());
    }

    public E removeLast() {
        if (isEmpty()) {
            throw new NoSuchElementException();
        }
        return removeAndGet(getMaxElementIndex());
    }

    public E peekLast() {
        if (isEmpty()) {
            return null;
        }
        return elementData(getMaxElementIndex());
    }

    MoveDesc<E> removeAt(int i) {
        Preconditions.checkPositionIndex(i, this.size);
        this.modCount++;
        this.size--;
        if (this.size == i) {
            this.queue[this.size] = null;
            return null;
        }
        E eElementData = elementData(this.size);
        int correctLastElement = heapForIndex(this.size).getCorrectLastElement(eElementData);
        E eElementData2 = elementData(this.size);
        this.queue[this.size] = null;
        MoveDesc<E> moveDescFillHole = fillHole(i, eElementData2);
        if (correctLastElement < i) {
            if (moveDescFillHole == null) {
                return new MoveDesc<>(eElementData, eElementData2);
            }
            return new MoveDesc<>(eElementData, moveDescFillHole.replaced);
        }
        return moveDescFillHole;
    }

    private MoveDesc<E> fillHole(int i, E e) {
        MinMaxPriorityQueue<E>.Heap heapHeapForIndex = heapForIndex(i);
        int iFillHoleAt = heapHeapForIndex.fillHoleAt(i);
        int iBubbleUpAlternatingLevels = heapHeapForIndex.bubbleUpAlternatingLevels(iFillHoleAt, e);
        if (iBubbleUpAlternatingLevels == iFillHoleAt) {
            return heapHeapForIndex.tryCrossOverAndBubbleUp(i, iFillHoleAt, e);
        }
        if (iBubbleUpAlternatingLevels < i) {
            return new MoveDesc<>(e, elementData(i));
        }
        return null;
    }

    static class MoveDesc<E> {
        final E replaced;
        final E toTrickle;

        MoveDesc(E e, E e2) {
            this.toTrickle = e;
            this.replaced = e2;
        }
    }

    private E removeAndGet(int i) {
        E eElementData = elementData(i);
        removeAt(i);
        return eElementData;
    }

    private MinMaxPriorityQueue<E>.Heap heapForIndex(int i) {
        return isEvenLevel(i) ? this.minHeap : this.maxHeap;
    }

    static boolean isEvenLevel(int i) {
        boolean z;
        int i2 = i + 1;
        if (i2 > 0) {
            z = true;
        } else {
            z = false;
        }
        Preconditions.checkState(z, "negative index");
        if ((EVEN_POWERS_OF_TWO & i2) > (i2 & ODD_POWERS_OF_TWO)) {
            return true;
        }
        return false;
    }

    boolean isIntact() {
        for (int i = 1; i < this.size; i++) {
            if (!heapForIndex(i).verifyIndex(i)) {
                return false;
            }
        }
        return true;
    }

    private class Heap {
        final Ordering<E> ordering;
        MinMaxPriorityQueue<E>.Heap otherHeap;

        Heap(Ordering<E> ordering) {
            this.ordering = ordering;
        }

        int compareElements(int i, int i2) {
            return this.ordering.compare((E) MinMaxPriorityQueue.this.elementData(i), (E) MinMaxPriorityQueue.this.elementData(i2));
        }

        MoveDesc<E> tryCrossOverAndBubbleUp(int i, int i2, E e) {
            Object objElementData;
            int iCrossOver = crossOver(i2, e);
            if (iCrossOver == i2) {
                return null;
            }
            if (iCrossOver < i) {
                objElementData = MinMaxPriorityQueue.this.elementData(i);
            } else {
                objElementData = MinMaxPriorityQueue.this.elementData(getParentIndex(i));
            }
            if (this.otherHeap.bubbleUpAlternatingLevels(iCrossOver, e) >= i) {
                return null;
            }
            return new MoveDesc<>(e, objElementData);
        }

        void bubbleUp(int i, E e) {
            Heap heap;
            int iCrossOverUp = crossOverUp(i, e);
            if (iCrossOverUp != i) {
                heap = this.otherHeap;
            } else {
                iCrossOverUp = i;
                heap = this;
            }
            heap.bubbleUpAlternatingLevels(iCrossOverUp, e);
        }

        int bubbleUpAlternatingLevels(int i, E e) {
            while (i > 2) {
                int grandparentIndex = getGrandparentIndex(i);
                Object objElementData = MinMaxPriorityQueue.this.elementData(grandparentIndex);
                if (this.ordering.compare((E) objElementData, e) <= 0) {
                    break;
                }
                MinMaxPriorityQueue.this.queue[i] = objElementData;
                i = grandparentIndex;
            }
            MinMaxPriorityQueue.this.queue[i] = e;
            return i;
        }

        int findMin(int i, int i2) {
            if (i >= MinMaxPriorityQueue.this.size) {
                return -1;
            }
            Preconditions.checkState(i > 0);
            int iMin = Math.min(i, MinMaxPriorityQueue.this.size - i2) + i2;
            for (int i3 = i + 1; i3 < iMin; i3++) {
                if (compareElements(i3, i) < 0) {
                    i = i3;
                }
            }
            return i;
        }

        int findMinChild(int i) {
            return findMin(getLeftChildIndex(i), 2);
        }

        int findMinGrandChild(int i) {
            int leftChildIndex = getLeftChildIndex(i);
            if (leftChildIndex < 0) {
                return -1;
            }
            return findMin(getLeftChildIndex(leftChildIndex), 4);
        }

        int crossOverUp(int i, E e) {
            int rightChildIndex;
            if (i == 0) {
                MinMaxPriorityQueue.this.queue[0] = e;
                return 0;
            }
            int parentIndex = getParentIndex(i);
            Object objElementData = MinMaxPriorityQueue.this.elementData(parentIndex);
            if (parentIndex != 0 && (rightChildIndex = getRightChildIndex(getParentIndex(parentIndex))) != parentIndex && getLeftChildIndex(rightChildIndex) >= MinMaxPriorityQueue.this.size) {
                Object objElementData2 = MinMaxPriorityQueue.this.elementData(rightChildIndex);
                if (this.ordering.compare((E) objElementData2, (E) objElementData) < 0) {
                    parentIndex = rightChildIndex;
                    objElementData = objElementData2;
                }
            }
            if (this.ordering.compare((E) objElementData, e) < 0) {
                MinMaxPriorityQueue.this.queue[i] = objElementData;
                MinMaxPriorityQueue.this.queue[parentIndex] = e;
                return parentIndex;
            }
            MinMaxPriorityQueue.this.queue[i] = e;
            return i;
        }

        int getCorrectLastElement(E e) {
            int rightChildIndex;
            int parentIndex = getParentIndex(MinMaxPriorityQueue.this.size);
            if (parentIndex != 0 && (rightChildIndex = getRightChildIndex(getParentIndex(parentIndex))) != parentIndex && getLeftChildIndex(rightChildIndex) >= MinMaxPriorityQueue.this.size) {
                Object objElementData = MinMaxPriorityQueue.this.elementData(rightChildIndex);
                if (this.ordering.compare((E) objElementData, e) < 0) {
                    MinMaxPriorityQueue.this.queue[rightChildIndex] = e;
                    MinMaxPriorityQueue.this.queue[MinMaxPriorityQueue.this.size] = objElementData;
                    return rightChildIndex;
                }
            }
            return MinMaxPriorityQueue.this.size;
        }

        int crossOver(int i, E e) {
            int iFindMinChild = findMinChild(i);
            if (iFindMinChild > 0 && this.ordering.compare((E) MinMaxPriorityQueue.this.elementData(iFindMinChild), e) < 0) {
                MinMaxPriorityQueue.this.queue[i] = MinMaxPriorityQueue.this.elementData(iFindMinChild);
                MinMaxPriorityQueue.this.queue[iFindMinChild] = e;
                return iFindMinChild;
            }
            return crossOverUp(i, e);
        }

        int fillHoleAt(int i) {
            while (true) {
                int iFindMinGrandChild = findMinGrandChild(i);
                if (iFindMinGrandChild > 0) {
                    MinMaxPriorityQueue.this.queue[i] = MinMaxPriorityQueue.this.elementData(iFindMinGrandChild);
                    i = iFindMinGrandChild;
                } else {
                    return i;
                }
            }
        }

        private boolean verifyIndex(int i) {
            if (getLeftChildIndex(i) < MinMaxPriorityQueue.this.size && compareElements(i, getLeftChildIndex(i)) > 0) {
                return false;
            }
            if (getRightChildIndex(i) < MinMaxPriorityQueue.this.size && compareElements(i, getRightChildIndex(i)) > 0) {
                return false;
            }
            if (i <= 0 || compareElements(i, getParentIndex(i)) <= 0) {
                return i <= 2 || compareElements(getGrandparentIndex(i), i) <= 0;
            }
            return false;
        }

        private int getLeftChildIndex(int i) {
            return (i * 2) + 1;
        }

        private int getRightChildIndex(int i) {
            return (i * 2) + 2;
        }

        private int getParentIndex(int i) {
            return (i - 1) / 2;
        }

        private int getGrandparentIndex(int i) {
            return getParentIndex(getParentIndex(i));
        }
    }

    private class QueueIterator implements Iterator<E> {
        private boolean canRemove;
        private int cursor;
        private int expectedModCount;
        private Queue<E> forgetMeNot;
        private E lastFromForgetMeNot;
        private List<E> skipMe;

        private QueueIterator() {
            this.cursor = -1;
            this.expectedModCount = MinMaxPriorityQueue.this.modCount;
        }

        @Override
        public boolean hasNext() {
            checkModCount();
            if (nextNotInSkipMe(this.cursor + 1) >= MinMaxPriorityQueue.this.size()) {
                return (this.forgetMeNot == null || this.forgetMeNot.isEmpty()) ? false : true;
            }
            return true;
        }

        @Override
        public E next() {
            checkModCount();
            int iNextNotInSkipMe = nextNotInSkipMe(this.cursor + 1);
            if (iNextNotInSkipMe < MinMaxPriorityQueue.this.size()) {
                this.cursor = iNextNotInSkipMe;
                this.canRemove = true;
                return (E) MinMaxPriorityQueue.this.elementData(this.cursor);
            }
            if (this.forgetMeNot != null) {
                this.cursor = MinMaxPriorityQueue.this.size();
                this.lastFromForgetMeNot = this.forgetMeNot.poll();
                if (this.lastFromForgetMeNot != null) {
                    this.canRemove = true;
                    return this.lastFromForgetMeNot;
                }
            }
            throw new NoSuchElementException("iterator moved past last element in queue.");
        }

        @Override
        public void remove() {
            CollectPreconditions.checkRemove(this.canRemove);
            checkModCount();
            this.canRemove = false;
            this.expectedModCount++;
            if (this.cursor < MinMaxPriorityQueue.this.size()) {
                MoveDesc<E> moveDescRemoveAt = MinMaxPriorityQueue.this.removeAt(this.cursor);
                if (moveDescRemoveAt != null) {
                    if (this.forgetMeNot == null) {
                        this.forgetMeNot = new ArrayDeque();
                        this.skipMe = new ArrayList(3);
                    }
                    this.forgetMeNot.add(moveDescRemoveAt.toTrickle);
                    this.skipMe.add(moveDescRemoveAt.replaced);
                }
                this.cursor--;
                return;
            }
            Preconditions.checkState(removeExact(this.lastFromForgetMeNot));
            this.lastFromForgetMeNot = null;
        }

        private boolean containsExact(Iterable<E> iterable, E e) {
            Iterator<E> it = iterable.iterator();
            while (it.hasNext()) {
                if (it.next() == e) {
                    return true;
                }
            }
            return false;
        }

        boolean removeExact(Object obj) {
            for (int i = 0; i < MinMaxPriorityQueue.this.size; i++) {
                if (MinMaxPriorityQueue.this.queue[i] == obj) {
                    MinMaxPriorityQueue.this.removeAt(i);
                    return true;
                }
            }
            return false;
        }

        void checkModCount() {
            if (MinMaxPriorityQueue.this.modCount != this.expectedModCount) {
                throw new ConcurrentModificationException();
            }
        }

        private int nextNotInSkipMe(int i) {
            if (this.skipMe != null) {
                while (i < MinMaxPriorityQueue.this.size() && containsExact(this.skipMe, MinMaxPriorityQueue.this.elementData(i))) {
                    i++;
                }
            }
            return i;
        }
    }

    @Override
    public Iterator<E> iterator() {
        return new QueueIterator();
    }

    @Override
    public void clear() {
        for (int i = 0; i < this.size; i++) {
            this.queue[i] = null;
        }
        this.size = 0;
    }

    @Override
    public Object[] toArray() {
        Object[] objArr = new Object[this.size];
        System.arraycopy(this.queue, 0, objArr, 0, this.size);
        return objArr;
    }

    public Comparator<? super E> comparator() {
        return this.minHeap.ordering;
    }

    int capacity() {
        return this.queue.length;
    }

    static int initialQueueSize(int i, int i2, Iterable<?> iterable) {
        if (i == -1) {
            i = DEFAULT_CAPACITY;
        }
        if (iterable instanceof Collection) {
            i = Math.max(i, ((Collection) iterable).size());
        }
        return capAtMaximumSize(i, i2);
    }

    private void growIfNeeded() {
        if (this.size > this.queue.length) {
            Object[] objArr = new Object[calculateNewCapacity()];
            System.arraycopy(this.queue, 0, objArr, 0, this.queue.length);
            this.queue = objArr;
        }
    }

    private int calculateNewCapacity() {
        int iCheckedMultiply;
        int length = this.queue.length;
        if (length < 64) {
            iCheckedMultiply = (length + 1) * 2;
        } else {
            iCheckedMultiply = IntMath.checkedMultiply(length / 2, 3);
        }
        return capAtMaximumSize(iCheckedMultiply, this.maximumSize);
    }

    private static int capAtMaximumSize(int i, int i2) {
        return Math.min(i - 1, i2) + 1;
    }
}
