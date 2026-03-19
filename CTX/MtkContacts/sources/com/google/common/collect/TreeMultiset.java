package com.google.common.collect;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multisets;
import com.google.common.primitives.Ints;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.Set;

public final class TreeMultiset<E> extends AbstractSortedMultiset<E> implements Serializable {
    private static final long serialVersionUID = 1;
    private final transient AvlNode<E> header;
    private final transient GeneralRange<E> range;
    private final transient Reference<AvlNode<E>> rootReference;

    private enum Aggregate {
        SIZE {
            @Override
            int nodeAggregate(AvlNode<?> avlNode) {
                return ((AvlNode) avlNode).elemCount;
            }

            @Override
            long treeAggregate(AvlNode<?> avlNode) {
                if (avlNode == null) {
                    return 0L;
                }
                return ((AvlNode) avlNode).totalCount;
            }
        },
        DISTINCT {
            @Override
            int nodeAggregate(AvlNode<?> avlNode) {
                return 1;
            }

            @Override
            long treeAggregate(AvlNode<?> avlNode) {
                if (avlNode == null) {
                    return 0L;
                }
                return ((AvlNode) avlNode).distinctElements;
            }
        };

        abstract int nodeAggregate(AvlNode<?> avlNode);

        abstract long treeAggregate(AvlNode<?> avlNode);
    }

    @Override
    public boolean add(Object obj) {
        return super.add(obj);
    }

    @Override
    public boolean addAll(Collection collection) {
        return super.addAll(collection);
    }

    @Override
    public void clear() {
        super.clear();
    }

    @Override
    public Comparator comparator() {
        return super.comparator();
    }

    @Override
    public boolean contains(Object obj) {
        return super.contains(obj);
    }

    @Override
    public SortedMultiset descendingMultiset() {
        return super.descendingMultiset();
    }

    @Override
    public NavigableSet elementSet() {
        return super.elementSet();
    }

    @Override
    public Set entrySet() {
        return super.entrySet();
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }

    @Override
    public Multiset.Entry firstEntry() {
        return super.firstEntry();
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public boolean isEmpty() {
        return super.isEmpty();
    }

    @Override
    public Iterator iterator() {
        return super.iterator();
    }

    @Override
    public Multiset.Entry lastEntry() {
        return super.lastEntry();
    }

    @Override
    public Multiset.Entry pollFirstEntry() {
        return super.pollFirstEntry();
    }

    @Override
    public Multiset.Entry pollLastEntry() {
        return super.pollLastEntry();
    }

    @Override
    public boolean remove(Object obj) {
        return super.remove(obj);
    }

    @Override
    public boolean removeAll(Collection collection) {
        return super.removeAll(collection);
    }

    @Override
    public boolean retainAll(Collection collection) {
        return super.retainAll(collection);
    }

    @Override
    public SortedMultiset subMultiset(Object obj, BoundType boundType, Object obj2, BoundType boundType2) {
        return super.subMultiset(obj, boundType, obj2, boundType2);
    }

    @Override
    public String toString() {
        return super.toString();
    }

    public static <E extends Comparable> TreeMultiset<E> create() {
        return new TreeMultiset<>(Ordering.natural());
    }

    public static <E> TreeMultiset<E> create(Comparator<? super E> comparator) {
        if (comparator == null) {
            return new TreeMultiset<>(Ordering.natural());
        }
        return new TreeMultiset<>(comparator);
    }

    public static <E extends Comparable> TreeMultiset<E> create(Iterable<? extends E> iterable) {
        TreeMultiset<E> treeMultisetCreate = create();
        Iterables.addAll(treeMultisetCreate, iterable);
        return treeMultisetCreate;
    }

    TreeMultiset(Reference<AvlNode<E>> reference, GeneralRange<E> generalRange, AvlNode<E> avlNode) {
        super(generalRange.comparator());
        this.rootReference = reference;
        this.range = generalRange;
        this.header = avlNode;
    }

    TreeMultiset(Comparator<? super E> comparator) {
        super(comparator);
        this.range = GeneralRange.all(comparator);
        this.header = new AvlNode<>(null, 1);
        successor(this.header, this.header);
        this.rootReference = new Reference<>();
    }

    private long aggregateForEntries(Aggregate aggregate) {
        AvlNode<E> avlNode = this.rootReference.get();
        long jTreeAggregate = aggregate.treeAggregate(avlNode);
        if (this.range.hasLowerBound()) {
            jTreeAggregate -= aggregateBelowRange(aggregate, avlNode);
        }
        if (this.range.hasUpperBound()) {
            return jTreeAggregate - aggregateAboveRange(aggregate, avlNode);
        }
        return jTreeAggregate;
    }

    private long aggregateBelowRange(Aggregate aggregate, AvlNode<E> avlNode) {
        if (avlNode == null) {
            return 0L;
        }
        int iCompare = comparator().compare(this.range.getLowerEndpoint(), ((AvlNode) avlNode).elem);
        if (iCompare >= 0) {
            if (iCompare == 0) {
                switch (this.range.getLowerBoundType()) {
                    case OPEN:
                        return ((long) aggregate.nodeAggregate(avlNode)) + aggregate.treeAggregate(((AvlNode) avlNode).left);
                    case CLOSED:
                        return aggregate.treeAggregate(((AvlNode) avlNode).left);
                    default:
                        throw new AssertionError();
                }
            }
            return aggregate.treeAggregate(((AvlNode) avlNode).left) + ((long) aggregate.nodeAggregate(avlNode)) + aggregateBelowRange(aggregate, ((AvlNode) avlNode).right);
        }
        return aggregateBelowRange(aggregate, ((AvlNode) avlNode).left);
    }

    private long aggregateAboveRange(Aggregate aggregate, AvlNode<E> avlNode) {
        if (avlNode == null) {
            return 0L;
        }
        int iCompare = comparator().compare(this.range.getUpperEndpoint(), ((AvlNode) avlNode).elem);
        if (iCompare <= 0) {
            if (iCompare == 0) {
                switch (this.range.getUpperBoundType()) {
                    case OPEN:
                        return ((long) aggregate.nodeAggregate(avlNode)) + aggregate.treeAggregate(((AvlNode) avlNode).right);
                    case CLOSED:
                        return aggregate.treeAggregate(((AvlNode) avlNode).right);
                    default:
                        throw new AssertionError();
                }
            }
            return aggregate.treeAggregate(((AvlNode) avlNode).right) + ((long) aggregate.nodeAggregate(avlNode)) + aggregateAboveRange(aggregate, ((AvlNode) avlNode).left);
        }
        return aggregateAboveRange(aggregate, ((AvlNode) avlNode).right);
    }

    @Override
    public int size() {
        return Ints.saturatedCast(aggregateForEntries(Aggregate.SIZE));
    }

    @Override
    int distinctElements() {
        return Ints.saturatedCast(aggregateForEntries(Aggregate.DISTINCT));
    }

    @Override
    public int count(Object obj) {
        try {
            AvlNode<E> avlNode = this.rootReference.get();
            if (this.range.contains(obj) && avlNode != null) {
                return avlNode.count(comparator(), obj);
            }
            return 0;
        } catch (ClassCastException e) {
            return 0;
        } catch (NullPointerException e2) {
            return 0;
        }
    }

    @Override
    public int add(E e, int i) {
        CollectPreconditions.checkNonnegative(i, "occurrences");
        if (i == 0) {
            return count(e);
        }
        Preconditions.checkArgument(this.range.contains(e));
        AvlNode<E> avlNode = this.rootReference.get();
        if (avlNode == null) {
            comparator().compare(e, e);
            AvlNode<E> avlNode2 = new AvlNode<>(e, i);
            successor(this.header, avlNode2, this.header);
            this.rootReference.checkAndSet(avlNode, avlNode2);
            return 0;
        }
        int[] iArr = new int[1];
        this.rootReference.checkAndSet(avlNode, avlNode.add(comparator(), e, i, iArr));
        return iArr[0];
    }

    @Override
    public int remove(Object obj, int i) {
        CollectPreconditions.checkNonnegative(i, "occurrences");
        if (i == 0) {
            return count(obj);
        }
        AvlNode<E> avlNode = this.rootReference.get();
        int[] iArr = new int[1];
        try {
            if (this.range.contains(obj) && avlNode != null) {
                this.rootReference.checkAndSet(avlNode, avlNode.remove(comparator(), obj, i, iArr));
                return iArr[0];
            }
            return 0;
        } catch (ClassCastException e) {
            return 0;
        } catch (NullPointerException e2) {
            return 0;
        }
    }

    @Override
    public int setCount(E e, int i) {
        CollectPreconditions.checkNonnegative(i, "count");
        if (!this.range.contains(e)) {
            Preconditions.checkArgument(i == 0);
            return 0;
        }
        AvlNode<E> avlNode = this.rootReference.get();
        if (avlNode == null) {
            if (i > 0) {
                add(e, i);
            }
            return 0;
        }
        int[] iArr = new int[1];
        this.rootReference.checkAndSet(avlNode, avlNode.setCount(comparator(), e, i, iArr));
        return iArr[0];
    }

    @Override
    public boolean setCount(E e, int i, int i2) {
        CollectPreconditions.checkNonnegative(i2, "newCount");
        CollectPreconditions.checkNonnegative(i, "oldCount");
        Preconditions.checkArgument(this.range.contains(e));
        AvlNode<E> avlNode = this.rootReference.get();
        if (avlNode == null) {
            if (i != 0) {
                return false;
            }
            if (i2 > 0) {
                add(e, i2);
            }
            return true;
        }
        int[] iArr = new int[1];
        this.rootReference.checkAndSet(avlNode, avlNode.setCount(comparator(), e, i, i2, iArr));
        return iArr[0] == i;
    }

    private Multiset.Entry<E> wrapEntry(final AvlNode<E> avlNode) {
        return new Multisets.AbstractEntry<E>() {
            @Override
            public E getElement() {
                return (E) avlNode.getElement();
            }

            @Override
            public int getCount() {
                int count = avlNode.getCount();
                if (count == 0) {
                    return TreeMultiset.this.count(getElement());
                }
                return count;
            }
        };
    }

    private AvlNode<E> firstNode() {
        AvlNode<E> avlNode;
        if (this.rootReference.get() == null) {
            return null;
        }
        if (this.range.hasLowerBound()) {
            E lowerEndpoint = this.range.getLowerEndpoint();
            AvlNode<E> avlNodeCeiling = this.rootReference.get().ceiling(comparator(), lowerEndpoint);
            if (avlNodeCeiling == null) {
                return null;
            }
            if (this.range.getLowerBoundType() == BoundType.OPEN && comparator().compare(lowerEndpoint, avlNodeCeiling.getElement()) == 0) {
                avlNodeCeiling = ((AvlNode) avlNodeCeiling).succ;
            }
            avlNode = avlNodeCeiling;
        } else {
            avlNode = ((AvlNode) this.header).succ;
        }
        if (avlNode == this.header || !this.range.contains(avlNode.getElement())) {
            return null;
        }
        return avlNode;
    }

    private AvlNode<E> lastNode() {
        AvlNode<E> avlNode;
        if (this.rootReference.get() == null) {
            return null;
        }
        if (this.range.hasUpperBound()) {
            E upperEndpoint = this.range.getUpperEndpoint();
            AvlNode<E> avlNodeFloor = this.rootReference.get().floor(comparator(), upperEndpoint);
            if (avlNodeFloor == null) {
                return null;
            }
            if (this.range.getUpperBoundType() == BoundType.OPEN && comparator().compare(upperEndpoint, avlNodeFloor.getElement()) == 0) {
                avlNodeFloor = ((AvlNode) avlNodeFloor).pred;
            }
            avlNode = avlNodeFloor;
        } else {
            avlNode = ((AvlNode) this.header).pred;
        }
        if (avlNode == this.header || !this.range.contains(avlNode.getElement())) {
            return null;
        }
        return avlNode;
    }

    @Override
    Iterator<Multiset.Entry<E>> entryIterator() {
        return new Iterator<Multiset.Entry<E>>() {
            AvlNode<E> current;
            Multiset.Entry<E> prevEntry;

            {
                this.current = TreeMultiset.this.firstNode();
            }

            @Override
            public boolean hasNext() {
                if (this.current == null) {
                    return false;
                }
                if (TreeMultiset.this.range.tooHigh(this.current.getElement())) {
                    this.current = null;
                    return false;
                }
                return true;
            }

            @Override
            public Multiset.Entry<E> next() {
                if (hasNext()) {
                    Multiset.Entry<E> entryWrapEntry = TreeMultiset.this.wrapEntry(this.current);
                    this.prevEntry = entryWrapEntry;
                    if (((AvlNode) this.current).succ == TreeMultiset.this.header) {
                        this.current = null;
                    } else {
                        this.current = ((AvlNode) this.current).succ;
                    }
                    return entryWrapEntry;
                }
                throw new NoSuchElementException();
            }

            @Override
            public void remove() {
                CollectPreconditions.checkRemove(this.prevEntry != null);
                TreeMultiset.this.setCount(this.prevEntry.getElement(), 0);
                this.prevEntry = null;
            }
        };
    }

    @Override
    Iterator<Multiset.Entry<E>> descendingEntryIterator() {
        return new Iterator<Multiset.Entry<E>>() {
            AvlNode<E> current;
            Multiset.Entry<E> prevEntry = null;

            {
                this.current = TreeMultiset.this.lastNode();
            }

            @Override
            public boolean hasNext() {
                if (this.current == null) {
                    return false;
                }
                if (TreeMultiset.this.range.tooLow(this.current.getElement())) {
                    this.current = null;
                    return false;
                }
                return true;
            }

            @Override
            public Multiset.Entry<E> next() {
                if (hasNext()) {
                    Multiset.Entry<E> entryWrapEntry = TreeMultiset.this.wrapEntry(this.current);
                    this.prevEntry = entryWrapEntry;
                    if (((AvlNode) this.current).pred == TreeMultiset.this.header) {
                        this.current = null;
                    } else {
                        this.current = ((AvlNode) this.current).pred;
                    }
                    return entryWrapEntry;
                }
                throw new NoSuchElementException();
            }

            @Override
            public void remove() {
                CollectPreconditions.checkRemove(this.prevEntry != null);
                TreeMultiset.this.setCount(this.prevEntry.getElement(), 0);
                this.prevEntry = null;
            }
        };
    }

    @Override
    public SortedMultiset<E> headMultiset(E e, BoundType boundType) {
        return new TreeMultiset(this.rootReference, this.range.intersect(GeneralRange.upTo(comparator(), e, boundType)), this.header);
    }

    @Override
    public SortedMultiset<E> tailMultiset(E e, BoundType boundType) {
        return new TreeMultiset(this.rootReference, this.range.intersect(GeneralRange.downTo(comparator(), e, boundType)), this.header);
    }

    static int distinctElements(AvlNode<?> avlNode) {
        if (avlNode == null) {
            return 0;
        }
        return ((AvlNode) avlNode).distinctElements;
    }

    private static final class Reference<T> {
        private T value;

        private Reference() {
        }

        public T get() {
            return this.value;
        }

        public void checkAndSet(T t, T t2) {
            if (this.value != t) {
                throw new ConcurrentModificationException();
            }
            this.value = t2;
        }
    }

    private static final class AvlNode<E> extends Multisets.AbstractEntry<E> {
        private int distinctElements;
        private final E elem;
        private int elemCount;
        private int height;
        private AvlNode<E> left;
        private AvlNode<E> pred;
        private AvlNode<E> right;
        private AvlNode<E> succ;
        private long totalCount;

        AvlNode(E e, int i) {
            Preconditions.checkArgument(i > 0);
            this.elem = e;
            this.elemCount = i;
            this.totalCount = i;
            this.distinctElements = 1;
            this.height = 1;
            this.left = null;
            this.right = null;
        }

        public int count(Comparator<? super E> comparator, E e) {
            int iCompare = comparator.compare(e, this.elem);
            if (iCompare < 0) {
                if (this.left == null) {
                    return 0;
                }
                return this.left.count(comparator, e);
            }
            if (iCompare > 0) {
                if (this.right == null) {
                    return 0;
                }
                return this.right.count(comparator, e);
            }
            return this.elemCount;
        }

        private AvlNode<E> addRightChild(E e, int i) {
            this.right = new AvlNode<>(e, i);
            TreeMultiset.successor(this, this.right, this.succ);
            this.height = Math.max(2, this.height);
            this.distinctElements++;
            this.totalCount += (long) i;
            return this;
        }

        private AvlNode<E> addLeftChild(E e, int i) {
            this.left = new AvlNode<>(e, i);
            TreeMultiset.successor(this.pred, this.left, this);
            this.height = Math.max(2, this.height);
            this.distinctElements++;
            this.totalCount += (long) i;
            return this;
        }

        AvlNode<E> add(Comparator<? super E> comparator, E e, int i, int[] iArr) {
            int iCompare = comparator.compare(e, this.elem);
            if (iCompare < 0) {
                AvlNode<E> avlNode = this.left;
                if (avlNode == null) {
                    iArr[0] = 0;
                    return addLeftChild(e, i);
                }
                int i2 = avlNode.height;
                this.left = avlNode.add(comparator, e, i, iArr);
                if (iArr[0] == 0) {
                    this.distinctElements++;
                }
                this.totalCount += (long) i;
                return this.left.height == i2 ? this : rebalance();
            }
            if (iCompare > 0) {
                AvlNode<E> avlNode2 = this.right;
                if (avlNode2 == null) {
                    iArr[0] = 0;
                    return addRightChild(e, i);
                }
                int i3 = avlNode2.height;
                this.right = avlNode2.add(comparator, e, i, iArr);
                if (iArr[0] == 0) {
                    this.distinctElements++;
                }
                this.totalCount += (long) i;
                return this.right.height == i3 ? this : rebalance();
            }
            iArr[0] = this.elemCount;
            long j = i;
            Preconditions.checkArgument(((long) this.elemCount) + j <= 2147483647L);
            this.elemCount += i;
            this.totalCount += j;
            return this;
        }

        AvlNode<E> remove(Comparator<? super E> comparator, E e, int i, int[] iArr) {
            int iCompare = comparator.compare(e, this.elem);
            if (iCompare < 0) {
                AvlNode<E> avlNode = this.left;
                if (avlNode == null) {
                    iArr[0] = 0;
                    return this;
                }
                this.left = avlNode.remove(comparator, e, i, iArr);
                if (iArr[0] > 0) {
                    if (i >= iArr[0]) {
                        this.distinctElements--;
                        this.totalCount -= (long) iArr[0];
                    } else {
                        this.totalCount -= (long) i;
                    }
                }
                return iArr[0] == 0 ? this : rebalance();
            }
            if (iCompare <= 0) {
                iArr[0] = this.elemCount;
                if (i >= this.elemCount) {
                    return deleteMe();
                }
                this.elemCount -= i;
                this.totalCount -= (long) i;
                return this;
            }
            AvlNode<E> avlNode2 = this.right;
            if (avlNode2 == null) {
                iArr[0] = 0;
                return this;
            }
            this.right = avlNode2.remove(comparator, e, i, iArr);
            if (iArr[0] > 0) {
                if (i >= iArr[0]) {
                    this.distinctElements--;
                    this.totalCount -= (long) iArr[0];
                } else {
                    this.totalCount -= (long) i;
                }
            }
            return rebalance();
        }

        AvlNode<E> setCount(Comparator<? super E> comparator, E e, int i, int[] iArr) {
            int iCompare = comparator.compare(e, this.elem);
            if (iCompare < 0) {
                AvlNode<E> avlNode = this.left;
                if (avlNode == null) {
                    iArr[0] = 0;
                    return i > 0 ? addLeftChild(e, i) : this;
                }
                this.left = avlNode.setCount(comparator, e, i, iArr);
                if (i == 0 && iArr[0] != 0) {
                    this.distinctElements--;
                } else if (i > 0 && iArr[0] == 0) {
                    this.distinctElements++;
                }
                this.totalCount += (long) (i - iArr[0]);
                return rebalance();
            }
            if (iCompare <= 0) {
                iArr[0] = this.elemCount;
                if (i == 0) {
                    return deleteMe();
                }
                this.totalCount += (long) (i - this.elemCount);
                this.elemCount = i;
                return this;
            }
            AvlNode<E> avlNode2 = this.right;
            if (avlNode2 == null) {
                iArr[0] = 0;
                return i > 0 ? addRightChild(e, i) : this;
            }
            this.right = avlNode2.setCount(comparator, e, i, iArr);
            if (i == 0 && iArr[0] != 0) {
                this.distinctElements--;
            } else if (i > 0 && iArr[0] == 0) {
                this.distinctElements++;
            }
            this.totalCount += (long) (i - iArr[0]);
            return rebalance();
        }

        AvlNode<E> setCount(Comparator<? super E> comparator, E e, int i, int i2, int[] iArr) {
            int iCompare = comparator.compare(e, this.elem);
            if (iCompare < 0) {
                AvlNode<E> avlNode = this.left;
                if (avlNode == null) {
                    iArr[0] = 0;
                    if (i == 0 && i2 > 0) {
                        return addLeftChild(e, i2);
                    }
                    return this;
                }
                this.left = avlNode.setCount(comparator, e, i, i2, iArr);
                if (iArr[0] == i) {
                    if (i2 == 0 && iArr[0] != 0) {
                        this.distinctElements--;
                    } else if (i2 > 0 && iArr[0] == 0) {
                        this.distinctElements++;
                    }
                    this.totalCount += (long) (i2 - iArr[0]);
                }
                return rebalance();
            }
            if (iCompare <= 0) {
                iArr[0] = this.elemCount;
                if (i == this.elemCount) {
                    if (i2 == 0) {
                        return deleteMe();
                    }
                    this.totalCount += (long) (i2 - this.elemCount);
                    this.elemCount = i2;
                }
                return this;
            }
            AvlNode<E> avlNode2 = this.right;
            if (avlNode2 == null) {
                iArr[0] = 0;
                if (i == 0 && i2 > 0) {
                    return addRightChild(e, i2);
                }
                return this;
            }
            this.right = avlNode2.setCount(comparator, e, i, i2, iArr);
            if (iArr[0] == i) {
                if (i2 == 0 && iArr[0] != 0) {
                    this.distinctElements--;
                } else if (i2 > 0 && iArr[0] == 0) {
                    this.distinctElements++;
                }
                this.totalCount += (long) (i2 - iArr[0]);
            }
            return rebalance();
        }

        private AvlNode<E> deleteMe() {
            int i = this.elemCount;
            this.elemCount = 0;
            TreeMultiset.successor(this.pred, this.succ);
            if (this.left == null) {
                return this.right;
            }
            if (this.right == null) {
                return this.left;
            }
            if (this.left.height >= this.right.height) {
                AvlNode<E> avlNode = this.pred;
                avlNode.left = this.left.removeMax(avlNode);
                avlNode.right = this.right;
                avlNode.distinctElements = this.distinctElements - 1;
                avlNode.totalCount = this.totalCount - ((long) i);
                return avlNode.rebalance();
            }
            AvlNode<E> avlNode2 = this.succ;
            avlNode2.right = this.right.removeMin(avlNode2);
            avlNode2.left = this.left;
            avlNode2.distinctElements = this.distinctElements - 1;
            avlNode2.totalCount = this.totalCount - ((long) i);
            return avlNode2.rebalance();
        }

        private AvlNode<E> removeMin(AvlNode<E> avlNode) {
            if (this.left == null) {
                return this.right;
            }
            this.left = this.left.removeMin(avlNode);
            this.distinctElements--;
            this.totalCount -= (long) avlNode.elemCount;
            return rebalance();
        }

        private AvlNode<E> removeMax(AvlNode<E> avlNode) {
            if (this.right == null) {
                return this.left;
            }
            this.right = this.right.removeMax(avlNode);
            this.distinctElements--;
            this.totalCount -= (long) avlNode.elemCount;
            return rebalance();
        }

        private void recomputeMultiset() {
            this.distinctElements = 1 + TreeMultiset.distinctElements(this.left) + TreeMultiset.distinctElements(this.right);
            this.totalCount = ((long) this.elemCount) + totalCount(this.left) + totalCount(this.right);
        }

        private void recomputeHeight() {
            this.height = 1 + Math.max(height(this.left), height(this.right));
        }

        private void recompute() {
            recomputeMultiset();
            recomputeHeight();
        }

        private AvlNode<E> rebalance() {
            int iBalanceFactor = balanceFactor();
            if (iBalanceFactor == -2) {
                if (this.right.balanceFactor() > 0) {
                    this.right = this.right.rotateRight();
                }
                return rotateLeft();
            }
            if (iBalanceFactor == 2) {
                if (this.left.balanceFactor() < 0) {
                    this.left = this.left.rotateLeft();
                }
                return rotateRight();
            }
            recomputeHeight();
            return this;
        }

        private int balanceFactor() {
            return height(this.left) - height(this.right);
        }

        private AvlNode<E> rotateLeft() {
            Preconditions.checkState(this.right != null);
            AvlNode<E> avlNode = this.right;
            this.right = avlNode.left;
            avlNode.left = this;
            avlNode.totalCount = this.totalCount;
            avlNode.distinctElements = this.distinctElements;
            recompute();
            avlNode.recomputeHeight();
            return avlNode;
        }

        private AvlNode<E> rotateRight() {
            Preconditions.checkState(this.left != null);
            AvlNode<E> avlNode = this.left;
            this.left = avlNode.right;
            avlNode.right = this;
            avlNode.totalCount = this.totalCount;
            avlNode.distinctElements = this.distinctElements;
            recompute();
            avlNode.recomputeHeight();
            return avlNode;
        }

        private static long totalCount(AvlNode<?> avlNode) {
            if (avlNode == null) {
                return 0L;
            }
            return ((AvlNode) avlNode).totalCount;
        }

        private static int height(AvlNode<?> avlNode) {
            if (avlNode == null) {
                return 0;
            }
            return ((AvlNode) avlNode).height;
        }

        private AvlNode<E> ceiling(Comparator<? super E> comparator, E e) {
            int iCompare = comparator.compare(e, this.elem);
            if (iCompare < 0) {
                return this.left == null ? this : (AvlNode) MoreObjects.firstNonNull(this.left.ceiling(comparator, e), this);
            }
            if (iCompare == 0) {
                return this;
            }
            if (this.right == null) {
                return null;
            }
            return this.right.ceiling(comparator, e);
        }

        private AvlNode<E> floor(Comparator<? super E> comparator, E e) {
            int iCompare = comparator.compare(e, this.elem);
            if (iCompare > 0) {
                return this.right == null ? this : (AvlNode) MoreObjects.firstNonNull(this.right.floor(comparator, e), this);
            }
            if (iCompare == 0) {
                return this;
            }
            if (this.left == null) {
                return null;
            }
            return this.left.floor(comparator, e);
        }

        @Override
        public E getElement() {
            return this.elem;
        }

        @Override
        public int getCount() {
            return this.elemCount;
        }

        @Override
        public String toString() {
            return Multisets.immutableEntry(getElement(), getCount()).toString();
        }
    }

    private static <T> void successor(AvlNode<T> avlNode, AvlNode<T> avlNode2) {
        ((AvlNode) avlNode).succ = avlNode2;
        ((AvlNode) avlNode2).pred = avlNode;
    }

    private static <T> void successor(AvlNode<T> avlNode, AvlNode<T> avlNode2, AvlNode<T> avlNode3) {
        successor(avlNode, avlNode2);
        successor(avlNode2, avlNode3);
    }

    private void writeObject(ObjectOutputStream objectOutputStream) throws IOException {
        objectOutputStream.defaultWriteObject();
        objectOutputStream.writeObject(elementSet().comparator());
        Serialization.writeMultiset(this, objectOutputStream);
    }

    private void readObject(ObjectInputStream objectInputStream) throws ClassNotFoundException, IOException {
        objectInputStream.defaultReadObject();
        Comparator comparator = (Comparator) objectInputStream.readObject();
        Serialization.getFieldSetter(AbstractSortedMultiset.class, "comparator").set(this, comparator);
        Serialization.getFieldSetter(TreeMultiset.class, "range").set(this, GeneralRange.all(comparator));
        Serialization.getFieldSetter(TreeMultiset.class, "rootReference").set(this, new Reference());
        AvlNode avlNode = new AvlNode(null, 1);
        Serialization.getFieldSetter(TreeMultiset.class, "header").set(this, avlNode);
        successor(avlNode, avlNode);
        Serialization.populateMultiset(this, objectInputStream);
    }
}
