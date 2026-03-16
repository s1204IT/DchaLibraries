package java.util.concurrent;

import dalvik.bytecode.Opcodes;
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
        FULL = NCPU >= 510 ? 255 : NCPU >>> 1;
        NULL_ITEM = new Object();
        TIMED_OUT = new Object();
        try {
            U = Unsafe.getUnsafe();
            BOUND = U.objectFieldOffset(Exchanger.class.getDeclaredField("bound"));
            SLOT = U.objectFieldOffset(Exchanger.class.getDeclaredField("slot"));
            MATCH = U.objectFieldOffset(Node.class.getDeclaredField("match"));
            BLOCKER = U.objectFieldOffset(Thread.class.getDeclaredField("parkBlocker"));
            int s = U.arrayIndexScale(Node[].class);
            ABASE = U.arrayBaseOffset(Node[].class) + 128;
            if (((s - 1) & s) != 0 || s > 128) {
                throw new Error("Unsupported array scale");
            }
        } catch (Exception e) {
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
        Object p0;
        Object p1;
        Object p2;
        Object p3;
        Object p4;
        Object p5;
        Object p6;
        Object p7;
        Object p8;
        Object p9;
        Object pa;
        volatile Thread parked;
        Object pb;
        Object pc;
        Object pd;
        Object pe;
        Object pf;
        Object q0;
        Object q1;
        Object q2;
        Object q3;
        Object q4;
        Object q5;
        Object q6;
        Object q7;
        Object q8;
        Object q9;
        Object qa;
        Object qb;
        Object qc;
        Object qd;
        Object qe;
        Object qf;

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

    private final Object arenaExchange(Object item, boolean timed, long ns) {
        Node[] a = this.arena;
        Node p = this.participant.get();
        int i = p.index;
        while (true) {
            Unsafe unsafe = U;
            long j = (i << 7) + ABASE;
            Node q = (Node) unsafe.getObjectVolatile(a, j);
            if (q != null && U.compareAndSwapObject(a, j, q, null)) {
                Object v = q.item;
                q.match = item;
                Thread w = q.parked;
                if (w == null) {
                    return v;
                }
                U.unpark(w);
                return v;
            }
            int b = this.bound;
            int m = b & 255;
            if (i <= m && q == null) {
                p.item = item;
                if (U.compareAndSwapObject(a, j, null, p)) {
                    long end = (timed && m == 0) ? System.nanoTime() + ns : 0L;
                    Thread t = Thread.currentThread();
                    int h = p.hash;
                    int spins = 1024;
                    while (true) {
                        Object v2 = p.match;
                        if (v2 != null) {
                            U.putOrderedObject(p, MATCH, null);
                            p.item = null;
                            p.hash = h;
                            return v2;
                        }
                        if (spins > 0) {
                            int h2 = h ^ (h << 1);
                            int h3 = h2 ^ (h2 >>> 3);
                            h = h3 ^ (h3 << 10);
                            if (h == 0) {
                                h = ((int) t.getId()) | 1024;
                            } else if (h < 0) {
                                spins--;
                                if ((spins & Opcodes.OP_CHECK_CAST_JUMBO) == 0) {
                                    Thread.yield();
                                }
                            }
                        } else if (U.getObjectVolatile(a, j) != p) {
                            spins = 1024;
                        } else {
                            if (!t.isInterrupted() && m == 0) {
                                if (timed) {
                                    ns = end - System.nanoTime();
                                    if (ns > 0) {
                                    }
                                }
                                U.putObject(t, BLOCKER, this);
                                p.parked = t;
                                if (U.getObjectVolatile(a, j) == p) {
                                    U.park(false, ns);
                                }
                                p.parked = null;
                                U.putObject(t, BLOCKER, null);
                            }
                            if (U.getObjectVolatile(a, j) == p && U.compareAndSwapObject(a, j, p, null)) {
                                if (m != 0) {
                                    U.compareAndSwapInt(this, BOUND, b, (b + 256) - 1);
                                }
                                p.item = null;
                                p.hash = h;
                                i = p.index >>> 1;
                                p.index = i;
                                if (Thread.interrupted()) {
                                    return null;
                                }
                                if (timed && m == 0 && ns <= 0) {
                                    return TIMED_OUT;
                                }
                            }
                        }
                    }
                } else {
                    p.item = null;
                }
            } else {
                if (p.bound != b) {
                    p.bound = b;
                    p.collides = 0;
                    i = (i != m || m == 0) ? m : m - 1;
                } else {
                    int c = p.collides;
                    if (c < m || m == FULL || !U.compareAndSwapInt(this, BOUND, b, b + 256 + 1)) {
                        p.collides = c + 1;
                        i = i == 0 ? m : i - 1;
                    } else {
                        i = m + 1;
                    }
                }
                p.index = i;
            }
        }
    }

    private final Object slotExchange(Object item, boolean timed, long ns) {
        Object v;
        Node p = this.participant.get();
        Thread t = Thread.currentThread();
        if (t.isInterrupted()) {
            return null;
        }
        while (true) {
            Node q = this.slot;
            if (q != null) {
                if (U.compareAndSwapObject(this, SLOT, q, null)) {
                    Object obj = q.item;
                    q.match = item;
                    Thread w = q.parked;
                    if (w == null) {
                        return obj;
                    }
                    U.unpark(w);
                    return obj;
                }
                if (NCPU > 1 && this.bound == 0 && U.compareAndSwapInt(this, BOUND, 0, 256)) {
                    this.arena = new Node[(FULL + 2) << 7];
                }
            } else {
                if (this.arena != null) {
                    return null;
                }
                p.item = item;
                if (!U.compareAndSwapObject(this, SLOT, null, p)) {
                    p.item = null;
                } else {
                    int h = p.hash;
                    long end = timed ? System.nanoTime() + ns : 0L;
                    int spins = NCPU > 1 ? 1024 : 1;
                    while (true) {
                        v = p.match;
                        if (v != null) {
                            break;
                        }
                        if (spins > 0) {
                            int h2 = h ^ (h << 1);
                            int h3 = h2 ^ (h2 >>> 3);
                            h = h3 ^ (h3 << 10);
                            if (h == 0) {
                                h = ((int) t.getId()) | 1024;
                            } else if (h < 0) {
                                spins--;
                                if ((spins & Opcodes.OP_CHECK_CAST_JUMBO) == 0) {
                                    Thread.yield();
                                }
                            }
                        } else if (this.slot != p) {
                            spins = 1024;
                        } else {
                            if (!t.isInterrupted() && this.arena == null) {
                                if (timed) {
                                    ns = end - System.nanoTime();
                                    if (ns > 0) {
                                    }
                                }
                                U.putObject(t, BLOCKER, this);
                                p.parked = t;
                                if (this.slot == p) {
                                    U.park(false, ns);
                                }
                                p.parked = null;
                                U.putObject(t, BLOCKER, null);
                            }
                            if (U.compareAndSwapObject(this, SLOT, p, null)) {
                                v = (!timed || ns > 0 || t.isInterrupted()) ? null : TIMED_OUT;
                            }
                        }
                    }
                    U.putOrderedObject(p, MATCH, null);
                    p.item = null;
                    p.hash = h;
                    return v;
                }
            }
        }
    }

    public V exchange(V v) throws InterruptedException {
        V v2;
        Object obj = v == null ? NULL_ITEM : v;
        if ((this.arena != null || (v2 = (V) slotExchange(obj, false, 0L)) == null) && (Thread.interrupted() || (v2 = (V) arenaExchange(obj, false, 0L)) == null)) {
            throw new InterruptedException();
        }
        if (v2 == NULL_ITEM) {
            return null;
        }
        return v2;
    }

    public V exchange(V v, long j, TimeUnit timeUnit) throws InterruptedException, TimeoutException {
        V v2;
        Object obj = v == null ? NULL_ITEM : v;
        long nanos = timeUnit.toNanos(j);
        if ((this.arena != null || (v2 = (V) slotExchange(obj, true, nanos)) == null) && (Thread.interrupted() || (v2 = (V) arenaExchange(obj, true, nanos)) == null)) {
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
