package java.util.stream;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;
import java.util.Iterator;
import java.util.Objects;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.CountedCompleter;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import java.util.function.LongConsumer;
import java.util.function.LongFunction;
import java.util.stream.Node;
import java.util.stream.Nodes;
import java.util.stream.Sink;
import java.util.stream.SpinedBuffer;

final class Nodes {
    static final String BAD_SIZE = "Stream size exceeds max array size";
    static final long MAX_ARRAY_SIZE = 2147483639;
    private static final Node EMPTY_NODE = new EmptyNode.OfRef();
    private static final Node.OfInt EMPTY_INT_NODE = new EmptyNode.OfInt();
    private static final Node.OfLong EMPTY_LONG_NODE = new EmptyNode.OfLong();
    private static final Node.OfDouble EMPTY_DOUBLE_NODE = new EmptyNode.OfDouble();
    private static final int[] EMPTY_INT_ARRAY = new int[0];
    private static final long[] EMPTY_LONG_ARRAY = new long[0];
    private static final double[] EMPTY_DOUBLE_ARRAY = new double[0];

    private Nodes() {
        throw new Error("no instances");
    }

    static <T> Node<T> emptyNode(StreamShape streamShape) {
        switch (streamShape) {
            case REFERENCE:
                return EMPTY_NODE;
            case INT_VALUE:
                return EMPTY_INT_NODE;
            case LONG_VALUE:
                return EMPTY_LONG_NODE;
            case DOUBLE_VALUE:
                return EMPTY_DOUBLE_NODE;
            default:
                throw new IllegalStateException("Unknown shape " + ((Object) streamShape));
        }
    }

    static <T> Node<T> conc(StreamShape streamShape, Node<T> node, Node<T> node2) {
        switch (streamShape) {
            case REFERENCE:
                return new ConcNode(node, node2);
            case INT_VALUE:
                return new ConcNode.OfInt((Node.OfInt) node, (Node.OfInt) node2);
            case LONG_VALUE:
                return new ConcNode.OfLong((Node.OfLong) node, (Node.OfLong) node2);
            case DOUBLE_VALUE:
                return new ConcNode.OfDouble((Node.OfDouble) node, (Node.OfDouble) node2);
            default:
                throw new IllegalStateException("Unknown shape " + ((Object) streamShape));
        }
    }

    static <T> Node<T> node(T[] tArr) {
        return new ArrayNode(tArr);
    }

    static <T> Node<T> node(Collection<T> collection) {
        return new CollectionNode(collection);
    }

    static <T> Node.Builder<T> builder(long j, IntFunction<T[]> intFunction) {
        if (j >= 0 && j < MAX_ARRAY_SIZE) {
            return new FixedNodeBuilder(j, intFunction);
        }
        return builder();
    }

    static <T> Node.Builder<T> builder() {
        return new SpinedNodeBuilder();
    }

    static Node.OfInt node(int[] iArr) {
        return new IntArrayNode(iArr);
    }

    static Node.Builder.OfInt intBuilder(long j) {
        if (j >= 0 && j < MAX_ARRAY_SIZE) {
            return new IntFixedNodeBuilder(j);
        }
        return intBuilder();
    }

    static Node.Builder.OfInt intBuilder() {
        return new IntSpinedNodeBuilder();
    }

    static Node.OfLong node(long[] jArr) {
        return new LongArrayNode(jArr);
    }

    static Node.Builder.OfLong longBuilder(long j) {
        if (j >= 0 && j < MAX_ARRAY_SIZE) {
            return new LongFixedNodeBuilder(j);
        }
        return longBuilder();
    }

    static Node.Builder.OfLong longBuilder() {
        return new LongSpinedNodeBuilder();
    }

    static Node.OfDouble node(double[] dArr) {
        return new DoubleArrayNode(dArr);
    }

    static Node.Builder.OfDouble doubleBuilder(long j) {
        if (j >= 0 && j < MAX_ARRAY_SIZE) {
            return new DoubleFixedNodeBuilder(j);
        }
        return doubleBuilder();
    }

    static Node.Builder.OfDouble doubleBuilder() {
        return new DoubleSpinedNodeBuilder();
    }

    public static <P_IN, P_OUT> Node<P_OUT> collect(PipelineHelper<P_OUT> pipelineHelper, Spliterator<P_IN> spliterator, boolean z, IntFunction<P_OUT[]> intFunction) {
        long jExactOutputSizeIfKnown = pipelineHelper.exactOutputSizeIfKnown(spliterator);
        if (jExactOutputSizeIfKnown < 0 || !spliterator.hasCharacteristics(16384)) {
            Node<P_OUT> node = (Node) new CollectorTask.OfRef(pipelineHelper, intFunction, spliterator).invoke();
            return z ? flatten(node, intFunction) : node;
        }
        if (jExactOutputSizeIfKnown >= MAX_ARRAY_SIZE) {
            throw new IllegalArgumentException(BAD_SIZE);
        }
        P_OUT[] p_outArrApply = intFunction.apply((int) jExactOutputSizeIfKnown);
        new SizedCollectorTask.OfRef(spliterator, pipelineHelper, p_outArrApply).invoke();
        return node(p_outArrApply);
    }

    public static <P_IN> Node.OfInt collectInt(PipelineHelper<Integer> pipelineHelper, Spliterator<P_IN> spliterator, boolean z) {
        long jExactOutputSizeIfKnown = pipelineHelper.exactOutputSizeIfKnown(spliterator);
        if (jExactOutputSizeIfKnown < 0 || !spliterator.hasCharacteristics(16384)) {
            Node.OfInt ofInt = (Node.OfInt) new CollectorTask.OfInt(pipelineHelper, spliterator).invoke();
            return z ? flattenInt(ofInt) : ofInt;
        }
        if (jExactOutputSizeIfKnown >= MAX_ARRAY_SIZE) {
            throw new IllegalArgumentException(BAD_SIZE);
        }
        int[] iArr = new int[(int) jExactOutputSizeIfKnown];
        new SizedCollectorTask.OfInt(spliterator, pipelineHelper, iArr).invoke();
        return node(iArr);
    }

    public static <P_IN> Node.OfLong collectLong(PipelineHelper<Long> pipelineHelper, Spliterator<P_IN> spliterator, boolean z) {
        long jExactOutputSizeIfKnown = pipelineHelper.exactOutputSizeIfKnown(spliterator);
        if (jExactOutputSizeIfKnown < 0 || !spliterator.hasCharacteristics(16384)) {
            Node.OfLong ofLong = (Node.OfLong) new CollectorTask.OfLong(pipelineHelper, spliterator).invoke();
            return z ? flattenLong(ofLong) : ofLong;
        }
        if (jExactOutputSizeIfKnown >= MAX_ARRAY_SIZE) {
            throw new IllegalArgumentException(BAD_SIZE);
        }
        long[] jArr = new long[(int) jExactOutputSizeIfKnown];
        new SizedCollectorTask.OfLong(spliterator, pipelineHelper, jArr).invoke();
        return node(jArr);
    }

    public static <P_IN> Node.OfDouble collectDouble(PipelineHelper<Double> pipelineHelper, Spliterator<P_IN> spliterator, boolean z) {
        long jExactOutputSizeIfKnown = pipelineHelper.exactOutputSizeIfKnown(spliterator);
        if (jExactOutputSizeIfKnown < 0 || !spliterator.hasCharacteristics(16384)) {
            Node.OfDouble ofDouble = (Node.OfDouble) new CollectorTask.OfDouble(pipelineHelper, spliterator).invoke();
            return z ? flattenDouble(ofDouble) : ofDouble;
        }
        if (jExactOutputSizeIfKnown >= MAX_ARRAY_SIZE) {
            throw new IllegalArgumentException(BAD_SIZE);
        }
        double[] dArr = new double[(int) jExactOutputSizeIfKnown];
        new SizedCollectorTask.OfDouble(spliterator, pipelineHelper, dArr).invoke();
        return node(dArr);
    }

    public static <T> Node<T> flatten(Node<T> node, IntFunction<T[]> intFunction) {
        if (node.getChildCount() > 0) {
            long jCount = node.count();
            if (jCount >= MAX_ARRAY_SIZE) {
                throw new IllegalArgumentException(BAD_SIZE);
            }
            T[] tArrApply = intFunction.apply((int) jCount);
            new ToArrayTask.OfRef(node, tArrApply, 0).invoke();
            return node(tArrApply);
        }
        return node;
    }

    public static Node.OfInt flattenInt(Node.OfInt ofInt) {
        if (ofInt.getChildCount() > 0) {
            long jCount = ofInt.count();
            if (jCount >= MAX_ARRAY_SIZE) {
                throw new IllegalArgumentException(BAD_SIZE);
            }
            int[] iArr = new int[(int) jCount];
            new ToArrayTask.OfInt(ofInt, iArr, 0).invoke();
            return node(iArr);
        }
        return ofInt;
    }

    public static Node.OfLong flattenLong(Node.OfLong ofLong) {
        if (ofLong.getChildCount() > 0) {
            long jCount = ofLong.count();
            if (jCount >= MAX_ARRAY_SIZE) {
                throw new IllegalArgumentException(BAD_SIZE);
            }
            long[] jArr = new long[(int) jCount];
            new ToArrayTask.OfLong(ofLong, jArr, 0).invoke();
            return node(jArr);
        }
        return ofLong;
    }

    public static Node.OfDouble flattenDouble(Node.OfDouble ofDouble) {
        if (ofDouble.getChildCount() > 0) {
            long jCount = ofDouble.count();
            if (jCount >= MAX_ARRAY_SIZE) {
                throw new IllegalArgumentException(BAD_SIZE);
            }
            double[] dArr = new double[(int) jCount];
            new ToArrayTask.OfDouble(ofDouble, dArr, 0).invoke();
            return node(dArr);
        }
        return ofDouble;
    }

    private static abstract class EmptyNode<T, T_ARR, T_CONS> implements Node<T> {
        EmptyNode() {
        }

        @Override
        public T[] asArray(IntFunction<T[]> intFunction) {
            return intFunction.apply(0);
        }

        public void copyInto(T_ARR t_arr, int i) {
        }

        @Override
        public long count() {
            return 0L;
        }

        public void forEach(T_CONS t_cons) {
        }

        private static class OfRef<T> extends EmptyNode<T, T[], Consumer<? super T>> {
            @Override
            public void copyInto(Object[] objArr, int i) {
                super.copyInto(objArr, i);
            }

            @Override
            public void forEach(Consumer consumer) {
                super.forEach(consumer);
            }

            private OfRef() {
            }

            @Override
            public Spliterator<T> spliterator() {
                return Spliterators.emptySpliterator();
            }
        }

        private static final class OfInt extends EmptyNode<Integer, int[], IntConsumer> implements Node.OfInt {
            OfInt() {
            }

            @Override
            public Spliterator.OfInt spliterator() {
                return Spliterators.emptyIntSpliterator();
            }

            @Override
            public int[] asPrimitiveArray() {
                return Nodes.EMPTY_INT_ARRAY;
            }
        }

        private static final class OfLong extends EmptyNode<Long, long[], LongConsumer> implements Node.OfLong {
            OfLong() {
            }

            @Override
            public Spliterator.OfLong spliterator() {
                return Spliterators.emptyLongSpliterator();
            }

            @Override
            public long[] asPrimitiveArray() {
                return Nodes.EMPTY_LONG_ARRAY;
            }
        }

        private static final class OfDouble extends EmptyNode<Double, double[], DoubleConsumer> implements Node.OfDouble {
            OfDouble() {
            }

            @Override
            public Spliterator.OfDouble spliterator() {
                return Spliterators.emptyDoubleSpliterator();
            }

            @Override
            public double[] asPrimitiveArray() {
                return Nodes.EMPTY_DOUBLE_ARRAY;
            }
        }
    }

    private static class ArrayNode<T> implements Node<T> {
        final T[] array;
        int curSize;

        ArrayNode(long j, IntFunction<T[]> intFunction) {
            if (j >= Nodes.MAX_ARRAY_SIZE) {
                throw new IllegalArgumentException(Nodes.BAD_SIZE);
            }
            this.array = intFunction.apply((int) j);
            this.curSize = 0;
        }

        ArrayNode(T[] tArr) {
            this.array = tArr;
            this.curSize = tArr.length;
        }

        @Override
        public Spliterator<T> spliterator() {
            return Arrays.spliterator(this.array, 0, this.curSize);
        }

        @Override
        public void copyInto(T[] tArr, int i) {
            System.arraycopy(this.array, 0, tArr, i, this.curSize);
        }

        @Override
        public T[] asArray(IntFunction<T[]> intFunction) {
            if (this.array.length == this.curSize) {
                return this.array;
            }
            throw new IllegalStateException();
        }

        @Override
        public long count() {
            return this.curSize;
        }

        @Override
        public void forEach(Consumer<? super T> consumer) {
            for (int i = 0; i < this.curSize; i++) {
                consumer.accept(this.array[i]);
            }
        }

        public String toString() {
            return String.format("ArrayNode[%d][%s]", Integer.valueOf(this.array.length - this.curSize), Arrays.toString(this.array));
        }
    }

    private static final class CollectionNode<T> implements Node<T> {
        private final Collection<T> c;

        CollectionNode(Collection<T> collection) {
            this.c = collection;
        }

        @Override
        public Spliterator<T> spliterator() {
            return this.c.stream().spliterator2();
        }

        @Override
        public void copyInto(T[] tArr, int i) {
            Iterator<T> it = this.c.iterator();
            while (it.hasNext()) {
                tArr[i] = it.next();
                i++;
            }
        }

        @Override
        public T[] asArray(IntFunction<T[]> intFunction) {
            return (T[]) this.c.toArray(intFunction.apply(this.c.size()));
        }

        @Override
        public long count() {
            return this.c.size();
        }

        @Override
        public void forEach(Consumer<? super T> consumer) {
            this.c.forEach(consumer);
        }

        public String toString() {
            return String.format("CollectionNode[%d][%s]", Integer.valueOf(this.c.size()), this.c);
        }
    }

    private static abstract class AbstractConcNode<T, T_NODE extends Node<T>> implements Node<T> {
        protected final T_NODE left;
        protected final T_NODE right;
        private final long size;

        AbstractConcNode(T_NODE t_node, T_NODE t_node2) {
            this.left = t_node;
            this.right = t_node2;
            this.size = t_node.count() + t_node2.count();
        }

        @Override
        public int getChildCount() {
            return 2;
        }

        @Override
        public T_NODE getChild(int i) {
            if (i == 0) {
                return this.left;
            }
            if (i == 1) {
                return this.right;
            }
            throw new IndexOutOfBoundsException();
        }

        @Override
        public long count() {
            return this.size;
        }
    }

    static final class ConcNode<T> extends AbstractConcNode<T, Node<T>> implements Node<T> {
        ConcNode(Node<T> node, Node<T> node2) {
            super(node, node2);
        }

        @Override
        public Spliterator<T> spliterator() {
            return new InternalNodeSpliterator.OfRef(this);
        }

        @Override
        public void copyInto(T[] tArr, int i) {
            Objects.requireNonNull(tArr);
            this.left.copyInto(tArr, i);
            this.right.copyInto(tArr, i + ((int) this.left.count()));
        }

        @Override
        public T[] asArray(IntFunction<T[]> intFunction) {
            long jCount = count();
            if (jCount >= Nodes.MAX_ARRAY_SIZE) {
                throw new IllegalArgumentException(Nodes.BAD_SIZE);
            }
            T[] tArrApply = intFunction.apply((int) jCount);
            copyInto(tArrApply, 0);
            return tArrApply;
        }

        @Override
        public void forEach(Consumer<? super T> consumer) {
            this.left.forEach(consumer);
            this.right.forEach(consumer);
        }

        @Override
        public Node<T> truncate(long j, long j2, IntFunction<T[]> intFunction) {
            if (j == 0 && j2 == count()) {
                return this;
            }
            long jCount = this.left.count();
            if (j >= jCount) {
                return this.right.truncate(j - jCount, j2 - jCount, intFunction);
            }
            if (j2 <= jCount) {
                return this.left.truncate(j, j2, intFunction);
            }
            return Nodes.conc(getShape(), this.left.truncate(j, jCount, intFunction), this.right.truncate(0L, j2 - jCount, intFunction));
        }

        public String toString() {
            if (count() < 32) {
                return String.format("ConcNode[%s.%s]", this.left, this.right);
            }
            return String.format("ConcNode[size=%d]", Long.valueOf(count()));
        }

        private static abstract class OfPrimitive<E, T_CONS, T_ARR, T_SPLITR extends Spliterator.OfPrimitive<E, T_CONS, T_SPLITR>, T_NODE extends Node.OfPrimitive<E, T_CONS, T_ARR, T_SPLITR, T_NODE>> extends AbstractConcNode<E, T_NODE> implements Node.OfPrimitive<E, T_CONS, T_ARR, T_SPLITR, T_NODE> {
            @Override
            public Node.OfPrimitive getChild(int i) {
                return (Node.OfPrimitive) super.getChild(i);
            }

            OfPrimitive(T_NODE t_node, T_NODE t_node2) {
                super(t_node, t_node2);
            }

            @Override
            public void forEach(T_CONS t_cons) {
                ((Node.OfPrimitive) this.left).forEach(t_cons);
                ((Node.OfPrimitive) this.right).forEach(t_cons);
            }

            @Override
            public void copyInto(T_ARR t_arr, int i) {
                ((Node.OfPrimitive) this.left).copyInto(t_arr, i);
                ((Node.OfPrimitive) this.right).copyInto(t_arr, i + ((int) ((Node.OfPrimitive) this.left).count()));
            }

            @Override
            public T_ARR asPrimitiveArray() {
                long jCount = count();
                if (jCount >= Nodes.MAX_ARRAY_SIZE) {
                    throw new IllegalArgumentException(Nodes.BAD_SIZE);
                }
                T_ARR t_arrNewArray = newArray((int) jCount);
                copyInto(t_arrNewArray, 0);
                return t_arrNewArray;
            }

            public String toString() {
                if (count() < 32) {
                    return String.format("%s[%s.%s]", getClass().getName(), this.left, this.right);
                }
                return String.format("%s[size=%d]", getClass().getName(), Long.valueOf(count()));
            }
        }

        static final class OfInt extends OfPrimitive<Integer, IntConsumer, int[], Spliterator.OfInt, Node.OfInt> implements Node.OfInt {
            OfInt(Node.OfInt ofInt, Node.OfInt ofInt2) {
                super(ofInt, ofInt2);
            }

            @Override
            public Spliterator.OfInt spliterator() {
                return new InternalNodeSpliterator.OfInt(this);
            }
        }

        static final class OfLong extends OfPrimitive<Long, LongConsumer, long[], Spliterator.OfLong, Node.OfLong> implements Node.OfLong {
            OfLong(Node.OfLong ofLong, Node.OfLong ofLong2) {
                super(ofLong, ofLong2);
            }

            @Override
            public Spliterator.OfLong spliterator() {
                return new InternalNodeSpliterator.OfLong(this);
            }
        }

        static final class OfDouble extends OfPrimitive<Double, DoubleConsumer, double[], Spliterator.OfDouble, Node.OfDouble> implements Node.OfDouble {
            OfDouble(Node.OfDouble ofDouble, Node.OfDouble ofDouble2) {
                super(ofDouble, ofDouble2);
            }

            @Override
            public Spliterator.OfDouble spliterator() {
                return new InternalNodeSpliterator.OfDouble(this);
            }
        }
    }

    private static abstract class InternalNodeSpliterator<T, S extends Spliterator<T>, N extends Node<T>> implements Spliterator<T> {
        int curChildIndex;
        N curNode;
        S lastNodeSpliterator;
        S tryAdvanceSpliterator;
        Deque<N> tryAdvanceStack;

        InternalNodeSpliterator(N n) {
            this.curNode = n;
        }

        protected final Deque<N> initStack() {
            ArrayDeque arrayDeque = new ArrayDeque(8);
            int childCount = this.curNode.getChildCount();
            while (true) {
                childCount--;
                if (childCount >= this.curChildIndex) {
                    arrayDeque.addFirst(this.curNode.getChild(childCount));
                } else {
                    return arrayDeque;
                }
            }
        }

        protected final N findNextLeafNode(Deque<N> deque) {
            while (true) {
                N n = (N) deque.pollFirst();
                if (n != null) {
                    if (n.getChildCount() != 0) {
                        for (int childCount = n.getChildCount() - 1; childCount >= 0; childCount--) {
                            deque.addFirst(n.getChild(childCount));
                        }
                    } else if (n.count() > 0) {
                        return n;
                    }
                } else {
                    return null;
                }
            }
        }

        protected final boolean initTryAdvance() {
            if (this.curNode == null) {
                return false;
            }
            if (this.tryAdvanceSpliterator == null) {
                if (this.lastNodeSpliterator == null) {
                    this.tryAdvanceStack = initStack();
                    Node nodeFindNextLeafNode = findNextLeafNode(this.tryAdvanceStack);
                    if (nodeFindNextLeafNode != null) {
                        this.tryAdvanceSpliterator = (S) nodeFindNextLeafNode.spliterator();
                        return true;
                    }
                    this.curNode = null;
                    return false;
                }
                this.tryAdvanceSpliterator = this.lastNodeSpliterator;
                return true;
            }
            return true;
        }

        @Override
        public final S trySplit() {
            if (this.curNode == null || this.tryAdvanceSpliterator != null) {
                return null;
            }
            if (this.lastNodeSpliterator != null) {
                return (S) this.lastNodeSpliterator.trySplit();
            }
            if (this.curChildIndex < this.curNode.getChildCount() - 1) {
                N n = this.curNode;
                int i = this.curChildIndex;
                this.curChildIndex = i + 1;
                return n.getChild(i).spliterator();
            }
            this.curNode = (N) this.curNode.getChild(this.curChildIndex);
            if (this.curNode.getChildCount() == 0) {
                this.lastNodeSpliterator = (S) this.curNode.spliterator();
                return (S) this.lastNodeSpliterator.trySplit();
            }
            this.curChildIndex = 0;
            N n2 = this.curNode;
            int i2 = this.curChildIndex;
            this.curChildIndex = i2 + 1;
            return n2.getChild(i2).spliterator();
        }

        @Override
        public final long estimateSize() {
            long jCount = 0;
            if (this.curNode == null) {
                return 0L;
            }
            if (this.lastNodeSpliterator != null) {
                return this.lastNodeSpliterator.estimateSize();
            }
            for (int i = this.curChildIndex; i < this.curNode.getChildCount(); i++) {
                jCount += this.curNode.getChild(i).count();
            }
            return jCount;
        }

        @Override
        public final int characteristics() {
            return 64;
        }

        private static final class OfRef<T> extends InternalNodeSpliterator<T, Spliterator<T>, Node<T>> {
            OfRef(Node<T> node) {
                super(node);
            }

            @Override
            public boolean tryAdvance(Consumer<? super T> consumer) {
                Node<T> nodeFindNextLeafNode;
                if (!initTryAdvance()) {
                    return false;
                }
                boolean zTryAdvance = this.tryAdvanceSpliterator.tryAdvance(consumer);
                if (!zTryAdvance) {
                    if (this.lastNodeSpliterator == null && (nodeFindNextLeafNode = findNextLeafNode(this.tryAdvanceStack)) != null) {
                        this.tryAdvanceSpliterator = nodeFindNextLeafNode.spliterator();
                        return this.tryAdvanceSpliterator.tryAdvance(consumer);
                    }
                    this.curNode = null;
                }
                return zTryAdvance;
            }

            @Override
            public void forEachRemaining(Consumer<? super T> consumer) {
                if (this.curNode == null) {
                    return;
                }
                if (this.tryAdvanceSpliterator == null) {
                    if (this.lastNodeSpliterator == null) {
                        Deque dequeInitStack = initStack();
                        while (true) {
                            Node nodeFindNextLeafNode = findNextLeafNode(dequeInitStack);
                            if (nodeFindNextLeafNode != null) {
                                nodeFindNextLeafNode.forEach(consumer);
                            } else {
                                this.curNode = null;
                                return;
                            }
                        }
                    } else {
                        this.lastNodeSpliterator.forEachRemaining(consumer);
                    }
                } else {
                    while (tryAdvance(consumer)) {
                    }
                }
            }
        }

        private static abstract class OfPrimitive<T, T_CONS, T_ARR, T_SPLITR extends Spliterator.OfPrimitive<T, T_CONS, T_SPLITR>, N extends Node.OfPrimitive<T, T_CONS, T_ARR, T_SPLITR, N>> extends InternalNodeSpliterator<T, T_SPLITR, N> implements Spliterator.OfPrimitive<T, T_CONS, T_SPLITR> {
            @Override
            public Spliterator.OfPrimitive trySplit() {
                return (Spliterator.OfPrimitive) super.trySplit();
            }

            OfPrimitive(N n) {
                super(n);
            }

            @Override
            public boolean tryAdvance(T_CONS t_cons) {
                N nFindNextLeafNode;
                if (!initTryAdvance()) {
                    return false;
                }
                boolean zTryAdvance = ((Spliterator.OfPrimitive) this.tryAdvanceSpliterator).tryAdvance(t_cons);
                if (!zTryAdvance) {
                    if (this.lastNodeSpliterator == null && (nFindNextLeafNode = findNextLeafNode(this.tryAdvanceStack)) != null) {
                        this.tryAdvanceSpliterator = nFindNextLeafNode.spliterator();
                        return ((Spliterator.OfPrimitive) this.tryAdvanceSpliterator).tryAdvance(t_cons);
                    }
                    this.curNode = null;
                }
                return zTryAdvance;
            }

            @Override
            public void forEachRemaining(T_CONS t_cons) {
                if (this.curNode == null) {
                    return;
                }
                if (this.tryAdvanceSpliterator == null) {
                    if (this.lastNodeSpliterator == null) {
                        Deque<N> dequeInitStack = initStack();
                        while (true) {
                            N nFindNextLeafNode = findNextLeafNode(dequeInitStack);
                            if (nFindNextLeafNode != null) {
                                nFindNextLeafNode.forEach(t_cons);
                            } else {
                                this.curNode = null;
                                return;
                            }
                        }
                    } else {
                        ((Spliterator.OfPrimitive) this.lastNodeSpliterator).forEachRemaining(t_cons);
                    }
                } else {
                    while (tryAdvance(t_cons)) {
                    }
                }
            }
        }

        private static final class OfInt extends OfPrimitive<Integer, IntConsumer, int[], Spliterator.OfInt, Node.OfInt> implements Spliterator.OfInt {
            @Override
            public void forEachRemaining(IntConsumer intConsumer) {
                super.forEachRemaining(intConsumer);
            }

            @Override
            public boolean tryAdvance(IntConsumer intConsumer) {
                return super.tryAdvance(intConsumer);
            }

            @Override
            public Spliterator.OfInt trySplit() {
                return (Spliterator.OfInt) super.trySplit();
            }

            OfInt(Node.OfInt ofInt) {
                super(ofInt);
            }
        }

        private static final class OfLong extends OfPrimitive<Long, LongConsumer, long[], Spliterator.OfLong, Node.OfLong> implements Spliterator.OfLong {
            @Override
            public void forEachRemaining(LongConsumer longConsumer) {
                super.forEachRemaining(longConsumer);
            }

            @Override
            public boolean tryAdvance(LongConsumer longConsumer) {
                return super.tryAdvance(longConsumer);
            }

            @Override
            public Spliterator.OfLong trySplit() {
                return (Spliterator.OfLong) super.trySplit();
            }

            OfLong(Node.OfLong ofLong) {
                super(ofLong);
            }
        }

        private static final class OfDouble extends OfPrimitive<Double, DoubleConsumer, double[], Spliterator.OfDouble, Node.OfDouble> implements Spliterator.OfDouble {
            @Override
            public void forEachRemaining(DoubleConsumer doubleConsumer) {
                super.forEachRemaining(doubleConsumer);
            }

            @Override
            public boolean tryAdvance(DoubleConsumer doubleConsumer) {
                return super.tryAdvance(doubleConsumer);
            }

            @Override
            public Spliterator.OfDouble trySplit() {
                return (Spliterator.OfDouble) super.trySplit();
            }

            OfDouble(Node.OfDouble ofDouble) {
                super(ofDouble);
            }
        }
    }

    private static final class FixedNodeBuilder<T> extends ArrayNode<T> implements Node.Builder<T> {
        static final boolean $assertionsDisabled = false;

        FixedNodeBuilder(long j, IntFunction<T[]> intFunction) {
            super(j, intFunction);
        }

        @Override
        public Node<T> build2() {
            if (this.curSize < this.array.length) {
                throw new IllegalStateException(String.format("Current size %d is less than fixed size %d", Integer.valueOf(this.curSize), Integer.valueOf(this.array.length)));
            }
            return this;
        }

        @Override
        public void begin(long j) {
            if (j != this.array.length) {
                throw new IllegalStateException(String.format("Begin size %d is not equal to fixed size %d", Long.valueOf(j), Integer.valueOf(this.array.length)));
            }
            this.curSize = 0;
        }

        @Override
        public void accept(T t) {
            if (this.curSize < this.array.length) {
                T[] tArr = this.array;
                int i = this.curSize;
                this.curSize = i + 1;
                tArr[i] = t;
                return;
            }
            throw new IllegalStateException(String.format("Accept exceeded fixed size of %d", Integer.valueOf(this.array.length)));
        }

        @Override
        public void end() {
            if (this.curSize < this.array.length) {
                throw new IllegalStateException(String.format("End size %d is less than fixed size %d", Integer.valueOf(this.curSize), Integer.valueOf(this.array.length)));
            }
        }

        @Override
        public String toString() {
            return String.format("FixedNodeBuilder[%d][%s]", Integer.valueOf(this.array.length - this.curSize), Arrays.toString(this.array));
        }
    }

    private static final class SpinedNodeBuilder<T> extends SpinedBuffer<T> implements Node<T>, Node.Builder<T> {
        static final boolean $assertionsDisabled = false;
        private boolean building = false;

        SpinedNodeBuilder() {
        }

        @Override
        public Spliterator<T> spliterator() {
            return super.spliterator();
        }

        @Override
        public void forEach(Consumer<? super T> consumer) {
            super.forEach(consumer);
        }

        @Override
        public void begin(long j) {
            this.building = true;
            clear();
            ensureCapacity(j);
        }

        @Override
        public void accept(T t) {
            super.accept(t);
        }

        @Override
        public void end() {
            this.building = false;
        }

        @Override
        public void copyInto(T[] tArr, int i) {
            super.copyInto(tArr, i);
        }

        @Override
        public T[] asArray(IntFunction<T[]> intFunction) {
            return (T[]) super.asArray(intFunction);
        }

        @Override
        public Node<T> build2() {
            return this;
        }
    }

    private static class IntArrayNode implements Node.OfInt {
        final int[] array;
        int curSize;

        IntArrayNode(long j) {
            if (j >= Nodes.MAX_ARRAY_SIZE) {
                throw new IllegalArgumentException(Nodes.BAD_SIZE);
            }
            this.array = new int[(int) j];
            this.curSize = 0;
        }

        IntArrayNode(int[] iArr) {
            this.array = iArr;
            this.curSize = iArr.length;
        }

        @Override
        public Spliterator.OfInt spliterator() {
            return Arrays.spliterator(this.array, 0, this.curSize);
        }

        @Override
        public int[] asPrimitiveArray() {
            if (this.array.length == this.curSize) {
                return this.array;
            }
            return Arrays.copyOf(this.array, this.curSize);
        }

        @Override
        public void copyInto(int[] iArr, int i) {
            System.arraycopy((Object) this.array, 0, (Object) iArr, i, this.curSize);
        }

        @Override
        public long count() {
            return this.curSize;
        }

        @Override
        public void forEach(IntConsumer intConsumer) {
            for (int i = 0; i < this.curSize; i++) {
                intConsumer.accept(this.array[i]);
            }
        }

        public String toString() {
            return String.format("IntArrayNode[%d][%s]", Integer.valueOf(this.array.length - this.curSize), Arrays.toString(this.array));
        }
    }

    private static class LongArrayNode implements Node.OfLong {
        final long[] array;
        int curSize;

        LongArrayNode(long j) {
            if (j >= Nodes.MAX_ARRAY_SIZE) {
                throw new IllegalArgumentException(Nodes.BAD_SIZE);
            }
            this.array = new long[(int) j];
            this.curSize = 0;
        }

        LongArrayNode(long[] jArr) {
            this.array = jArr;
            this.curSize = jArr.length;
        }

        @Override
        public Spliterator.OfLong spliterator() {
            return Arrays.spliterator(this.array, 0, this.curSize);
        }

        @Override
        public long[] asPrimitiveArray() {
            if (this.array.length == this.curSize) {
                return this.array;
            }
            return Arrays.copyOf(this.array, this.curSize);
        }

        @Override
        public void copyInto(long[] jArr, int i) {
            System.arraycopy((Object) this.array, 0, (Object) jArr, i, this.curSize);
        }

        @Override
        public long count() {
            return this.curSize;
        }

        @Override
        public void forEach(LongConsumer longConsumer) {
            for (int i = 0; i < this.curSize; i++) {
                longConsumer.accept(this.array[i]);
            }
        }

        public String toString() {
            return String.format("LongArrayNode[%d][%s]", Integer.valueOf(this.array.length - this.curSize), Arrays.toString(this.array));
        }
    }

    private static class DoubleArrayNode implements Node.OfDouble {
        final double[] array;
        int curSize;

        DoubleArrayNode(long j) {
            if (j >= Nodes.MAX_ARRAY_SIZE) {
                throw new IllegalArgumentException(Nodes.BAD_SIZE);
            }
            this.array = new double[(int) j];
            this.curSize = 0;
        }

        DoubleArrayNode(double[] dArr) {
            this.array = dArr;
            this.curSize = dArr.length;
        }

        @Override
        public Spliterator.OfDouble spliterator() {
            return Arrays.spliterator(this.array, 0, this.curSize);
        }

        @Override
        public double[] asPrimitiveArray() {
            if (this.array.length == this.curSize) {
                return this.array;
            }
            return Arrays.copyOf(this.array, this.curSize);
        }

        @Override
        public void copyInto(double[] dArr, int i) {
            System.arraycopy((Object) this.array, 0, (Object) dArr, i, this.curSize);
        }

        @Override
        public long count() {
            return this.curSize;
        }

        @Override
        public void forEach(DoubleConsumer doubleConsumer) {
            for (int i = 0; i < this.curSize; i++) {
                doubleConsumer.accept(this.array[i]);
            }
        }

        public String toString() {
            return String.format("DoubleArrayNode[%d][%s]", Integer.valueOf(this.array.length - this.curSize), Arrays.toString(this.array));
        }
    }

    private static final class IntFixedNodeBuilder extends IntArrayNode implements Node.Builder.OfInt {
        static final boolean $assertionsDisabled = false;

        IntFixedNodeBuilder(long j) {
            super(j);
        }

        @Override
        public Node<Integer> build2() {
            if (this.curSize < this.array.length) {
                throw new IllegalStateException(String.format("Current size %d is less than fixed size %d", Integer.valueOf(this.curSize), Integer.valueOf(this.array.length)));
            }
            return this;
        }

        @Override
        public void begin(long j) {
            if (j != this.array.length) {
                throw new IllegalStateException(String.format("Begin size %d is not equal to fixed size %d", Long.valueOf(j), Integer.valueOf(this.array.length)));
            }
            this.curSize = 0;
        }

        @Override
        public void accept(int i) {
            if (this.curSize < this.array.length) {
                int[] iArr = this.array;
                int i2 = this.curSize;
                this.curSize = i2 + 1;
                iArr[i2] = i;
                return;
            }
            throw new IllegalStateException(String.format("Accept exceeded fixed size of %d", Integer.valueOf(this.array.length)));
        }

        @Override
        public void end() {
            if (this.curSize < this.array.length) {
                throw new IllegalStateException(String.format("End size %d is less than fixed size %d", Integer.valueOf(this.curSize), Integer.valueOf(this.array.length)));
            }
        }

        @Override
        public String toString() {
            return String.format("IntFixedNodeBuilder[%d][%s]", Integer.valueOf(this.array.length - this.curSize), Arrays.toString(this.array));
        }
    }

    private static final class LongFixedNodeBuilder extends LongArrayNode implements Node.Builder.OfLong {
        static final boolean $assertionsDisabled = false;

        LongFixedNodeBuilder(long j) {
            super(j);
        }

        @Override
        public Node<Long> build2() {
            if (this.curSize < this.array.length) {
                throw new IllegalStateException(String.format("Current size %d is less than fixed size %d", Integer.valueOf(this.curSize), Integer.valueOf(this.array.length)));
            }
            return this;
        }

        @Override
        public void begin(long j) {
            if (j != this.array.length) {
                throw new IllegalStateException(String.format("Begin size %d is not equal to fixed size %d", Long.valueOf(j), Integer.valueOf(this.array.length)));
            }
            this.curSize = 0;
        }

        @Override
        public void accept(long j) {
            if (this.curSize < this.array.length) {
                long[] jArr = this.array;
                int i = this.curSize;
                this.curSize = i + 1;
                jArr[i] = j;
                return;
            }
            throw new IllegalStateException(String.format("Accept exceeded fixed size of %d", Integer.valueOf(this.array.length)));
        }

        @Override
        public void end() {
            if (this.curSize < this.array.length) {
                throw new IllegalStateException(String.format("End size %d is less than fixed size %d", Integer.valueOf(this.curSize), Integer.valueOf(this.array.length)));
            }
        }

        @Override
        public String toString() {
            return String.format("LongFixedNodeBuilder[%d][%s]", Integer.valueOf(this.array.length - this.curSize), Arrays.toString(this.array));
        }
    }

    private static final class DoubleFixedNodeBuilder extends DoubleArrayNode implements Node.Builder.OfDouble {
        static final boolean $assertionsDisabled = false;

        DoubleFixedNodeBuilder(long j) {
            super(j);
        }

        @Override
        public Node<Double> build2() {
            if (this.curSize < this.array.length) {
                throw new IllegalStateException(String.format("Current size %d is less than fixed size %d", Integer.valueOf(this.curSize), Integer.valueOf(this.array.length)));
            }
            return this;
        }

        @Override
        public void begin(long j) {
            if (j != this.array.length) {
                throw new IllegalStateException(String.format("Begin size %d is not equal to fixed size %d", Long.valueOf(j), Integer.valueOf(this.array.length)));
            }
            this.curSize = 0;
        }

        @Override
        public void accept(double d) {
            if (this.curSize < this.array.length) {
                double[] dArr = this.array;
                int i = this.curSize;
                this.curSize = i + 1;
                dArr[i] = d;
                return;
            }
            throw new IllegalStateException(String.format("Accept exceeded fixed size of %d", Integer.valueOf(this.array.length)));
        }

        @Override
        public void end() {
            if (this.curSize < this.array.length) {
                throw new IllegalStateException(String.format("End size %d is less than fixed size %d", Integer.valueOf(this.curSize), Integer.valueOf(this.array.length)));
            }
        }

        @Override
        public String toString() {
            return String.format("DoubleFixedNodeBuilder[%d][%s]", Integer.valueOf(this.array.length - this.curSize), Arrays.toString(this.array));
        }
    }

    private static final class IntSpinedNodeBuilder extends SpinedBuffer.OfInt implements Node.OfInt, Node.Builder.OfInt {
        static final boolean $assertionsDisabled = false;
        private boolean building = false;

        IntSpinedNodeBuilder() {
        }

        @Override
        public Spliterator.OfInt spliterator() {
            return super.spliterator();
        }

        @Override
        public void forEach(IntConsumer intConsumer) {
            super.forEach(intConsumer);
        }

        @Override
        public void begin(long j) {
            this.building = true;
            clear();
            ensureCapacity(j);
        }

        @Override
        public void accept(int i) {
            super.accept(i);
        }

        @Override
        public void end() {
            this.building = false;
        }

        @Override
        public void copyInto(int[] iArr, int i) throws IndexOutOfBoundsException {
            super.copyInto(iArr, i);
        }

        @Override
        public int[] asPrimitiveArray() {
            return (int[]) super.asPrimitiveArray();
        }

        @Override
        public Node<Integer> build2() {
            return this;
        }
    }

    private static final class LongSpinedNodeBuilder extends SpinedBuffer.OfLong implements Node.OfLong, Node.Builder.OfLong {
        static final boolean $assertionsDisabled = false;
        private boolean building = false;

        LongSpinedNodeBuilder() {
        }

        @Override
        public Spliterator.OfLong spliterator() {
            return super.spliterator();
        }

        @Override
        public void forEach(LongConsumer longConsumer) {
            super.forEach(longConsumer);
        }

        @Override
        public void begin(long j) {
            this.building = true;
            clear();
            ensureCapacity(j);
        }

        @Override
        public void accept(long j) {
            super.accept(j);
        }

        @Override
        public void end() {
            this.building = false;
        }

        @Override
        public void copyInto(long[] jArr, int i) {
            super.copyInto(jArr, i);
        }

        @Override
        public long[] asPrimitiveArray() {
            return (long[]) super.asPrimitiveArray();
        }

        @Override
        public Node<Long> build2() {
            return this;
        }
    }

    private static final class DoubleSpinedNodeBuilder extends SpinedBuffer.OfDouble implements Node.OfDouble, Node.Builder.OfDouble {
        static final boolean $assertionsDisabled = false;
        private boolean building = false;

        DoubleSpinedNodeBuilder() {
        }

        @Override
        public Spliterator.OfDouble spliterator() {
            return super.spliterator();
        }

        @Override
        public void forEach(DoubleConsumer doubleConsumer) {
            super.forEach(doubleConsumer);
        }

        @Override
        public void begin(long j) {
            this.building = true;
            clear();
            ensureCapacity(j);
        }

        @Override
        public void accept(double d) {
            super.accept(d);
        }

        @Override
        public void end() {
            this.building = false;
        }

        @Override
        public void copyInto(double[] dArr, int i) {
            super.copyInto(dArr, i);
        }

        @Override
        public double[] asPrimitiveArray() {
            return (double[]) super.asPrimitiveArray();
        }

        @Override
        public Node<Double> build2() {
            return this;
        }
    }

    private static abstract class SizedCollectorTask<P_IN, P_OUT, T_SINK extends Sink<P_OUT>, K extends SizedCollectorTask<P_IN, P_OUT, T_SINK, K>> extends CountedCompleter<Void> implements Sink<P_OUT> {
        static final boolean $assertionsDisabled = false;
        protected int fence;
        protected final PipelineHelper<P_OUT> helper;
        protected int index;
        protected long length;
        protected long offset;
        protected final Spliterator<P_IN> spliterator;
        protected final long targetSize;

        abstract K makeChild(Spliterator<P_IN> spliterator, long j, long j2);

        SizedCollectorTask(Spliterator<P_IN> spliterator, PipelineHelper<P_OUT> pipelineHelper, int i) {
            this.spliterator = spliterator;
            this.helper = pipelineHelper;
            this.targetSize = AbstractTask.suggestTargetSize(spliterator.estimateSize());
            this.offset = 0L;
            this.length = i;
        }

        SizedCollectorTask(K k, Spliterator<P_IN> spliterator, long j, long j2, int i) {
            super(k);
            this.spliterator = spliterator;
            this.helper = k.helper;
            this.targetSize = k.targetSize;
            this.offset = j;
            this.length = j2;
            if (j < 0 || j2 < 0 || (j + j2) - 1 >= i) {
                throw new IllegalArgumentException(String.format("offset and length interval [%d, %d + %d) is not within array size interval [0, %d)", Long.valueOf(j), Long.valueOf(j), Long.valueOf(j2), Integer.valueOf(i)));
            }
        }

        @Override
        public void compute() {
            Spliterator<P_IN> spliteratorTrySplit;
            Spliterator<P_IN> spliterator = this.spliterator;
            SizedCollectorTask<P_IN, P_OUT, T_SINK, K> sizedCollectorTaskMakeChild = this;
            while (spliterator.estimateSize() > sizedCollectorTaskMakeChild.targetSize && (spliteratorTrySplit = spliterator.trySplit()) != null) {
                sizedCollectorTaskMakeChild.setPendingCount(1);
                long jEstimateSize = spliteratorTrySplit.estimateSize();
                sizedCollectorTaskMakeChild.makeChild(spliteratorTrySplit, sizedCollectorTaskMakeChild.offset, jEstimateSize).fork();
                sizedCollectorTaskMakeChild = sizedCollectorTaskMakeChild.makeChild(spliterator, sizedCollectorTaskMakeChild.offset + jEstimateSize, sizedCollectorTaskMakeChild.length - jEstimateSize);
            }
            sizedCollectorTaskMakeChild.helper.wrapAndCopyInto(sizedCollectorTaskMakeChild, spliterator);
            sizedCollectorTaskMakeChild.propagateCompletion();
        }

        @Override
        public void begin(long j) {
            if (j > this.length) {
                throw new IllegalStateException("size passed to Sink.begin exceeds array length");
            }
            this.index = (int) this.offset;
            this.fence = this.index + ((int) this.length);
        }

        static final class OfRef<P_IN, P_OUT> extends SizedCollectorTask<P_IN, P_OUT, Sink<P_OUT>, OfRef<P_IN, P_OUT>> implements Sink<P_OUT> {
            private final P_OUT[] array;

            OfRef(Spliterator<P_IN> spliterator, PipelineHelper<P_OUT> pipelineHelper, P_OUT[] p_outArr) {
                super(spliterator, pipelineHelper, p_outArr.length);
                this.array = p_outArr;
            }

            OfRef(OfRef<P_IN, P_OUT> ofRef, Spliterator<P_IN> spliterator, long j, long j2) {
                super(ofRef, spliterator, j, j2, ofRef.array.length);
                this.array = ofRef.array;
            }

            @Override
            OfRef<P_IN, P_OUT> makeChild(Spliterator<P_IN> spliterator, long j, long j2) {
                return new OfRef<>(this, spliterator, j, j2);
            }

            @Override
            public void accept(P_OUT p_out) {
                if (this.index >= this.fence) {
                    throw new IndexOutOfBoundsException(Integer.toString(this.index));
                }
                P_OUT[] p_outArr = this.array;
                int i = this.index;
                this.index = i + 1;
                p_outArr[i] = p_out;
            }
        }

        static final class OfInt<P_IN> extends SizedCollectorTask<P_IN, Integer, Sink.OfInt, OfInt<P_IN>> implements Sink.OfInt {
            private final int[] array;

            OfInt(Spliterator<P_IN> spliterator, PipelineHelper<Integer> pipelineHelper, int[] iArr) {
                super(spliterator, pipelineHelper, iArr.length);
                this.array = iArr;
            }

            OfInt(OfInt<P_IN> ofInt, Spliterator<P_IN> spliterator, long j, long j2) {
                super(ofInt, spliterator, j, j2, ofInt.array.length);
                this.array = ofInt.array;
            }

            @Override
            OfInt<P_IN> makeChild(Spliterator<P_IN> spliterator, long j, long j2) {
                return new OfInt<>(this, spliterator, j, j2);
            }

            @Override
            public void accept(int i) {
                if (this.index >= this.fence) {
                    throw new IndexOutOfBoundsException(Integer.toString(this.index));
                }
                int[] iArr = this.array;
                int i2 = this.index;
                this.index = i2 + 1;
                iArr[i2] = i;
            }
        }

        static final class OfLong<P_IN> extends SizedCollectorTask<P_IN, Long, Sink.OfLong, OfLong<P_IN>> implements Sink.OfLong {
            private final long[] array;

            OfLong(Spliterator<P_IN> spliterator, PipelineHelper<Long> pipelineHelper, long[] jArr) {
                super(spliterator, pipelineHelper, jArr.length);
                this.array = jArr;
            }

            OfLong(OfLong<P_IN> ofLong, Spliterator<P_IN> spliterator, long j, long j2) {
                super(ofLong, spliterator, j, j2, ofLong.array.length);
                this.array = ofLong.array;
            }

            @Override
            OfLong<P_IN> makeChild(Spliterator<P_IN> spliterator, long j, long j2) {
                return new OfLong<>(this, spliterator, j, j2);
            }

            @Override
            public void accept(long j) {
                if (this.index >= this.fence) {
                    throw new IndexOutOfBoundsException(Integer.toString(this.index));
                }
                long[] jArr = this.array;
                int i = this.index;
                this.index = i + 1;
                jArr[i] = j;
            }
        }

        static final class OfDouble<P_IN> extends SizedCollectorTask<P_IN, Double, Sink.OfDouble, OfDouble<P_IN>> implements Sink.OfDouble {
            private final double[] array;

            OfDouble(Spliterator<P_IN> spliterator, PipelineHelper<Double> pipelineHelper, double[] dArr) {
                super(spliterator, pipelineHelper, dArr.length);
                this.array = dArr;
            }

            OfDouble(OfDouble<P_IN> ofDouble, Spliterator<P_IN> spliterator, long j, long j2) {
                super(ofDouble, spliterator, j, j2, ofDouble.array.length);
                this.array = ofDouble.array;
            }

            @Override
            OfDouble<P_IN> makeChild(Spliterator<P_IN> spliterator, long j, long j2) {
                return new OfDouble<>(this, spliterator, j, j2);
            }

            @Override
            public void accept(double d) {
                if (this.index >= this.fence) {
                    throw new IndexOutOfBoundsException(Integer.toString(this.index));
                }
                double[] dArr = this.array;
                int i = this.index;
                this.index = i + 1;
                dArr[i] = d;
            }
        }
    }

    private static abstract class ToArrayTask<T, T_NODE extends Node<T>, K extends ToArrayTask<T, T_NODE, K>> extends CountedCompleter<Void> {
        protected final T_NODE node;
        protected final int offset;

        abstract void copyNodeToArray();

        abstract K makeChild(int i, int i2);

        ToArrayTask(T_NODE t_node, int i) {
            this.node = t_node;
            this.offset = i;
        }

        ToArrayTask(K k, T_NODE t_node, int i) {
            super(k);
            this.node = t_node;
            this.offset = i;
        }

        @Override
        public void compute() {
            ToArrayTask<T, T_NODE, K> toArrayTaskMakeChild = this;
            while (toArrayTaskMakeChild.node.getChildCount() != 0) {
                toArrayTaskMakeChild.setPendingCount(toArrayTaskMakeChild.node.getChildCount() - 1);
                int i = 0;
                int iCount = 0;
                while (i < toArrayTaskMakeChild.node.getChildCount() - 1) {
                    ToArrayTask toArrayTaskMakeChild2 = toArrayTaskMakeChild.makeChild(i, toArrayTaskMakeChild.offset + iCount);
                    iCount = (int) (((long) iCount) + toArrayTaskMakeChild2.node.count());
                    toArrayTaskMakeChild2.fork();
                    i++;
                }
                toArrayTaskMakeChild = toArrayTaskMakeChild.makeChild(i, toArrayTaskMakeChild.offset + iCount);
            }
            toArrayTaskMakeChild.copyNodeToArray();
            toArrayTaskMakeChild.propagateCompletion();
        }

        private static final class OfRef<T> extends ToArrayTask<T, Node<T>, OfRef<T>> {
            private final T[] array;

            private OfRef(Node<T> node, T[] tArr, int i) {
                super(node, i);
                this.array = tArr;
            }

            private OfRef(OfRef<T> ofRef, Node<T> node, int i) {
                super(ofRef, node, i);
                this.array = ofRef.array;
            }

            @Override
            OfRef<T> makeChild(int i, int i2) {
                return new OfRef<>(this, this.node.getChild(i), i2);
            }

            @Override
            void copyNodeToArray() {
                this.node.copyInto(this.array, this.offset);
            }
        }

        private static class OfPrimitive<T, T_CONS, T_ARR, T_SPLITR extends Spliterator.OfPrimitive<T, T_CONS, T_SPLITR>, T_NODE extends Node.OfPrimitive<T, T_CONS, T_ARR, T_SPLITR, T_NODE>> extends ToArrayTask<T, T_NODE, OfPrimitive<T, T_CONS, T_ARR, T_SPLITR, T_NODE>> {
            private final T_ARR array;

            private OfPrimitive(T_NODE t_node, T_ARR t_arr, int i) {
                super(t_node, i);
                this.array = t_arr;
            }

            private OfPrimitive(OfPrimitive<T, T_CONS, T_ARR, T_SPLITR, T_NODE> ofPrimitive, T_NODE t_node, int i) {
                super(ofPrimitive, t_node, i);
                this.array = ofPrimitive.array;
            }

            @Override
            OfPrimitive<T, T_CONS, T_ARR, T_SPLITR, T_NODE> makeChild(int i, int i2) {
                return new OfPrimitive<>(this, ((Node.OfPrimitive) this.node).getChild(i), i2);
            }

            @Override
            void copyNodeToArray() {
                ((Node.OfPrimitive) this.node).copyInto(this.array, this.offset);
            }
        }

        private static final class OfInt extends OfPrimitive<Integer, IntConsumer, int[], Spliterator.OfInt, Node.OfInt> {
            private OfInt(Node.OfInt ofInt, int[] iArr, int i) {
                super(ofInt, iArr, i);
            }
        }

        private static final class OfLong extends OfPrimitive<Long, LongConsumer, long[], Spliterator.OfLong, Node.OfLong> {
            private OfLong(Node.OfLong ofLong, long[] jArr, int i) {
                super(ofLong, jArr, i);
            }
        }

        private static final class OfDouble extends OfPrimitive<Double, DoubleConsumer, double[], Spliterator.OfDouble, Node.OfDouble> {
            private OfDouble(Node.OfDouble ofDouble, double[] dArr, int i) {
                super(ofDouble, dArr, i);
            }
        }
    }

    private static class CollectorTask<P_IN, P_OUT, T_NODE extends Node<P_OUT>, T_BUILDER extends Node.Builder<P_OUT>> extends AbstractTask<P_IN, P_OUT, T_NODE, CollectorTask<P_IN, P_OUT, T_NODE, T_BUILDER>> {
        protected final LongFunction<T_BUILDER> builderFactory;
        protected final BinaryOperator<T_NODE> concFactory;
        protected final PipelineHelper<P_OUT> helper;

        CollectorTask(PipelineHelper<P_OUT> pipelineHelper, Spliterator<P_IN> spliterator, LongFunction<T_BUILDER> longFunction, BinaryOperator<T_NODE> binaryOperator) {
            super(pipelineHelper, spliterator);
            this.helper = pipelineHelper;
            this.builderFactory = longFunction;
            this.concFactory = binaryOperator;
        }

        CollectorTask(CollectorTask<P_IN, P_OUT, T_NODE, T_BUILDER> collectorTask, Spliterator<P_IN> spliterator) {
            super(collectorTask, spliterator);
            this.helper = collectorTask.helper;
            this.builderFactory = collectorTask.builderFactory;
            this.concFactory = collectorTask.concFactory;
        }

        @Override
        protected CollectorTask<P_IN, P_OUT, T_NODE, T_BUILDER> makeChild(Spliterator<P_IN> spliterator) {
            return new CollectorTask<>(this, spliterator);
        }

        @Override
        protected T_NODE doLeaf() {
            return (T_NODE) ((Node.Builder) this.helper.wrapAndCopyInto(this.builderFactory.apply(this.helper.exactOutputSizeIfKnown(this.spliterator)), this.spliterator)).build2();
        }

        @Override
        public void onCompletion(CountedCompleter<?> countedCompleter) {
            if (!isLeaf()) {
                setLocalResult((Node) this.concFactory.apply((T_NODE) ((Node) ((CollectorTask) this.leftChild).getLocalResult()), (Node) ((CollectorTask) this.rightChild).getLocalResult()));
            }
            super.onCompletion(countedCompleter);
        }

        private static final class OfRef<P_IN, P_OUT> extends CollectorTask<P_IN, P_OUT, Node<P_OUT>, Node.Builder<P_OUT>> {
            OfRef(PipelineHelper<P_OUT> pipelineHelper, final IntFunction<P_OUT[]> intFunction, Spliterator<P_IN> spliterator) {
                super(pipelineHelper, spliterator, new LongFunction() {
                    @Override
                    public final Object apply(long j) {
                        return Nodes.builder(j, intFunction);
                    }
                }, new BinaryOperator() {
                    @Override
                    public final Object apply(Object obj, Object obj2) {
                        return new Nodes.ConcNode((Node) obj, (Node) obj2);
                    }
                });
            }
        }

        private static final class OfInt<P_IN> extends CollectorTask<P_IN, Integer, Node.OfInt, Node.Builder.OfInt> {
            OfInt(PipelineHelper<Integer> pipelineHelper, Spliterator<P_IN> spliterator) {
                super(pipelineHelper, spliterator, new LongFunction() {
                    @Override
                    public final Object apply(long j) {
                        return Nodes.intBuilder(j);
                    }
                }, new BinaryOperator() {
                    @Override
                    public final Object apply(Object obj, Object obj2) {
                        return new Nodes.ConcNode.OfInt((Node.OfInt) obj, (Node.OfInt) obj2);
                    }
                });
            }
        }

        private static final class OfLong<P_IN> extends CollectorTask<P_IN, Long, Node.OfLong, Node.Builder.OfLong> {
            OfLong(PipelineHelper<Long> pipelineHelper, Spliterator<P_IN> spliterator) {
                super(pipelineHelper, spliterator, new LongFunction() {
                    @Override
                    public final Object apply(long j) {
                        return Nodes.longBuilder(j);
                    }
                }, new BinaryOperator() {
                    @Override
                    public final Object apply(Object obj, Object obj2) {
                        return new Nodes.ConcNode.OfLong((Node.OfLong) obj, (Node.OfLong) obj2);
                    }
                });
            }
        }

        private static final class OfDouble<P_IN> extends CollectorTask<P_IN, Double, Node.OfDouble, Node.Builder.OfDouble> {
            OfDouble(PipelineHelper<Double> pipelineHelper, Spliterator<P_IN> spliterator) {
                super(pipelineHelper, spliterator, new LongFunction() {
                    @Override
                    public final Object apply(long j) {
                        return Nodes.doubleBuilder(j);
                    }
                }, new BinaryOperator() {
                    @Override
                    public final Object apply(Object obj, Object obj2) {
                        return new Nodes.ConcNode.OfDouble((Node.OfDouble) obj, (Node.OfDouble) obj2);
                    }
                });
            }
        }
    }
}
