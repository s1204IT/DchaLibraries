package java.util.concurrent;

import sun.misc.Unsafe;

public class Exchanger<V> {
    private static final int ABASE;
    private static final int ASHIFT = 7;
    private static final long BLOCKER;
    private static final long BOUND;
    static final int FULL;
    private static final long MATCH;
    private static final int MMASK = 255;
    private static final int NCPU = Runtime.getRuntime().availableProcessors();
    private static final Object NULL_ITEM;
    private static final int SEQ = 256;
    private static final long SLOT;
    private static final int SPINS = 1024;
    private static final Object TIMED_OUT;
    private static final Unsafe U;
    private volatile Node[] arena;
    private volatile int bound;
    private final Participant participant = new Participant();
    private volatile Node slot;

    static {
        FULL = NCPU >= 510 ? MMASK : NCPU >>> 1;
        NULL_ITEM = new Object();
        TIMED_OUT = new Object();
        U = Unsafe.getUnsafe();
        try {
            BOUND = U.objectFieldOffset(Exchanger.class.getDeclaredField("bound"));
            SLOT = U.objectFieldOffset(Exchanger.class.getDeclaredField("slot"));
            MATCH = U.objectFieldOffset(Node.class.getDeclaredField("match"));
            BLOCKER = U.objectFieldOffset(Thread.class.getDeclaredField("parkBlocker"));
            int iArrayIndexScale = U.arrayIndexScale(Node[].class);
            if (((iArrayIndexScale - 1) & iArrayIndexScale) != 0 || iArrayIndexScale > 128) {
                throw new Error("Unsupported array scale");
            }
            ABASE = U.arrayBaseOffset(Node[].class) + 128;
        } catch (ReflectiveOperationException e) {
            throw new Error(e);
        }
    }

    static final class Node {
        int bound;
        int collides;
        int hash;
        int index;
        Object item;
        volatile Object match;
        volatile Thread parked;

        Node() {
        }
    }

    static final class Participant extends ThreadLocal<Node> {
        Participant() {
        }

        @Override
        public Node initialValue() {
            return new Node();
        }
    }

    private final Object arenaExchange(Object obj, boolean z, long j) {
        long j2;
        Node node;
        Node[] nodeArr;
        int i;
        long j3;
        int i2;
        Thread thread;
        Object obj2 = obj;
        Node[] nodeArr2 = this.arena;
        Node node2 = this.participant.get();
        long j4 = j;
        int i3 = node2.index;
        while (true) {
            Unsafe unsafe = U;
            long j5 = (i3 << 7) + ABASE;
            Node node3 = (Node) unsafe.getObjectVolatile(nodeArr2, j5);
            if (node3 != null) {
                j2 = j4;
                node = node3;
                if (U.compareAndSwapObject(nodeArr2, j5, node3, null)) {
                    Object obj3 = node.item;
                    node.match = obj2;
                    Thread thread2 = node.parked;
                    if (thread2 != null) {
                        U.unpark(thread2);
                    }
                    return obj3;
                }
            } else {
                j2 = j4;
                node = node3;
            }
            int i4 = this.bound;
            int i5 = i4 & MMASK;
            if (i3 > i5 || node != null) {
                int i6 = i5;
                nodeArr = nodeArr2;
                if (node2.bound != i4) {
                    node2.bound = i4;
                    node2.collides = 0;
                    if (i3 == i6 && i6 != 0) {
                        i = i6 - 1;
                        i6 = i;
                    }
                    node2.index = i6;
                    i3 = i6;
                } else {
                    int i7 = node2.collides;
                    if (i7 < i6 || i6 == FULL || !U.compareAndSwapInt(this, BOUND, i4, i4 + 256 + 1)) {
                        node2.collides = i7 + 1;
                        if (i3 != 0) {
                            i = i3 - 1;
                            i6 = i;
                        }
                    } else {
                        i6++;
                    }
                    node2.index = i6;
                    i3 = i6;
                }
                nodeArr2 = nodeArr;
                obj2 = obj;
            } else {
                node2.item = obj2;
                Thread thread3 = null;
                if (U.compareAndSwapObject(nodeArr2, j5, null, node2)) {
                    long jNanoTime = (z && i5 == 0) ? System.nanoTime() + j2 : 0L;
                    Thread threadCurrentThread = Thread.currentThread();
                    int i8 = node2.hash;
                    long jNanoTime2 = j2;
                    int i9 = 1024;
                    while (true) {
                        Object obj4 = node2.match;
                        if (obj4 != null) {
                            U.putOrderedObject(node2, MATCH, thread3);
                            node2.item = thread3;
                            node2.hash = i8;
                            return obj4;
                        }
                        if (i9 > 0) {
                            int i10 = (i8 << 1) ^ i8;
                            int i11 = i10 ^ (i10 >>> 3);
                            int id = i11 ^ (i11 << 10);
                            if (id == 0) {
                                id = 1024 | ((int) threadCurrentThread.getId());
                            } else if (id < 0) {
                                int i12 = i9 - 1;
                                if ((i12 & 511) == 0) {
                                    Thread.yield();
                                }
                                i8 = id;
                                i9 = i12;
                            }
                            i8 = id;
                        } else if (U.getObjectVolatile(nodeArr2, j5) != node2) {
                            nodeArr = nodeArr2;
                            j3 = j5;
                            i9 = 1024;
                            thread = thread3;
                            thread3 = thread;
                            nodeArr2 = nodeArr;
                            j5 = j3;
                        } else {
                            if (!threadCurrentThread.isInterrupted() && i5 == 0) {
                                if (z) {
                                    jNanoTime2 = jNanoTime - System.nanoTime();
                                }
                                U.putObject(threadCurrentThread, BLOCKER, this);
                                node2.parked = threadCurrentThread;
                                if (U.getObjectVolatile(nodeArr2, j5) == node2) {
                                    U.park(false, jNanoTime2);
                                }
                                node2.parked = thread3;
                                U.putObject(threadCurrentThread, BLOCKER, thread3);
                            }
                            long j6 = jNanoTime2;
                            if (U.getObjectVolatile(nodeArr2, j5) == node2) {
                                Node[] nodeArr3 = nodeArr2;
                                long j7 = j5;
                                nodeArr = nodeArr2;
                                i2 = i8;
                                j3 = j5;
                                thread = thread3;
                                if (U.compareAndSwapObject(nodeArr3, j7, node2, null)) {
                                    if (i5 != 0) {
                                        U.compareAndSwapInt(this, BOUND, i4, (i4 + 256) - 1);
                                    }
                                    node2.item = thread;
                                    node2.hash = i2;
                                    int i13 = node2.index >>> 1;
                                    node2.index = i13;
                                    if (Thread.interrupted()) {
                                        return thread;
                                    }
                                    if (z && i5 == 0 && j6 <= 0) {
                                        return TIMED_OUT;
                                    }
                                    i3 = i13;
                                    j4 = j6;
                                }
                            } else {
                                nodeArr = nodeArr2;
                                j3 = j5;
                                i2 = i8;
                                thread = thread3;
                            }
                            jNanoTime2 = j6;
                            i8 = i2;
                            thread3 = thread;
                            nodeArr2 = nodeArr;
                            j5 = j3;
                        }
                        nodeArr = nodeArr2;
                        j3 = j5;
                        thread = thread3;
                        thread3 = thread;
                        nodeArr2 = nodeArr;
                        j5 = j3;
                    }
                    nodeArr2 = nodeArr;
                    obj2 = obj;
                } else {
                    nodeArr = nodeArr2;
                    node2.item = null;
                }
            }
            j4 = j2;
            nodeArr2 = nodeArr;
            obj2 = obj;
        }
    }

    private final Object slotExchange(Object obj, boolean z, long j) {
        Object obj2;
        long j2;
        Node node = this.participant.get();
        Thread threadCurrentThread = Thread.currentThread();
        if (threadCurrentThread.isInterrupted()) {
            return null;
        }
        while (true) {
            Node node2 = this.slot;
            if (node2 != null) {
                if (U.compareAndSwapObject(this, SLOT, node2, null)) {
                    Object obj3 = node2.item;
                    node2.match = obj;
                    Thread thread = node2.parked;
                    if (thread != null) {
                        U.unpark(thread);
                    }
                    return obj3;
                }
                if (NCPU > 1 && this.bound == 0 && U.compareAndSwapInt(this, BOUND, 0, 256)) {
                    this.arena = new Node[(FULL + 2) << 7];
                }
            } else {
                if (this.arena != null) {
                    return null;
                }
                node.item = obj;
                if (U.compareAndSwapObject(this, SLOT, null, node)) {
                    int i = node.hash;
                    long jNanoTime = z ? System.nanoTime() + j : 0L;
                    int i2 = NCPU > 1 ? 1024 : 1;
                    long j3 = j;
                    int id = i;
                    while (true) {
                        obj2 = node.match;
                        if (obj2 != null) {
                            break;
                        }
                        if (i2 > 0) {
                            int i3 = (id << 1) ^ id;
                            int i4 = i3 ^ (i3 >>> 3);
                            id = i4 ^ (i4 << 10);
                            if (id == 0) {
                                id = 1024 | ((int) threadCurrentThread.getId());
                            } else if (id < 0) {
                                i2--;
                                if ((i2 & 511) == 0) {
                                    Thread.yield();
                                }
                            }
                        } else if (this.slot != node) {
                            i2 = 1024;
                        } else if (threadCurrentThread.isInterrupted() || this.arena != null) {
                            j2 = j3;
                            if (U.compareAndSwapObject(this, SLOT, node, null)) {
                                j3 = j2;
                            } else {
                                obj2 = (!z || j2 > 0 || threadCurrentThread.isInterrupted()) ? null : TIMED_OUT;
                            }
                        } else {
                            if (z) {
                                long jNanoTime2 = jNanoTime - System.nanoTime();
                                if (jNanoTime2 > 0) {
                                    j3 = jNanoTime2;
                                } else {
                                    j2 = jNanoTime2;
                                    if (U.compareAndSwapObject(this, SLOT, node, null)) {
                                    }
                                }
                            }
                            U.putObject(threadCurrentThread, BLOCKER, this);
                            node.parked = threadCurrentThread;
                            if (this.slot == node) {
                                U.park(false, j3);
                            }
                            node.parked = null;
                            U.putObject(threadCurrentThread, BLOCKER, null);
                        }
                    }
                    U.putOrderedObject(node, MATCH, null);
                    node.item = null;
                    node.hash = id;
                    return obj2;
                }
                node.item = null;
            }
        }
    }

    public V exchange(V v) throws InterruptedException {
        V v2;
        if (v == null) {
            v = (V) NULL_ITEM;
        }
        if ((this.arena != null || (v2 = (V) slotExchange(v, false, 0L)) == null) && (Thread.interrupted() || (v2 = (V) arenaExchange(v, false, 0L)) == null)) {
            throw new InterruptedException();
        }
        if (v2 == NULL_ITEM) {
            return null;
        }
        return v2;
    }

    public V exchange(V v, long j, TimeUnit timeUnit) throws InterruptedException, TimeoutException {
        V v2;
        if (v == null) {
            v = (V) NULL_ITEM;
        }
        long nanos = timeUnit.toNanos(j);
        if ((this.arena != null || (v2 = (V) slotExchange(v, true, nanos)) == null) && (Thread.interrupted() || (v2 = (V) arenaExchange(v, true, nanos)) == null)) {
            throw new InterruptedException();
        }
        if (v2 == TIMED_OUT) {
            throw new TimeoutException();
        }
        if (v2 == NULL_ITEM) {
            return null;
        }
        return v2;
    }
}
