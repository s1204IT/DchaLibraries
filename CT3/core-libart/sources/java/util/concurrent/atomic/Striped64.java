package java.util.concurrent.atomic;

import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.DoubleBinaryOperator;
import java.util.function.LongBinaryOperator;
import sun.misc.Unsafe;

abstract class Striped64 extends Number {
    private static final long BASE;
    private static final long CELLSBUSY;
    private static final long PROBE;
    volatile transient long base;
    volatile transient Cell[] cells;
    volatile transient int cellsBusy;
    static final int NCPU = Runtime.getRuntime().availableProcessors();
    private static final Unsafe U = Unsafe.getUnsafe();

    static final class Cell {
        private static final Unsafe U = Unsafe.getUnsafe();
        private static final long VALUE;
        volatile long value;

        Cell(long x) {
            this.value = x;
        }

        final boolean cas(long cmp, long val) {
            return U.compareAndSwapLong(this, VALUE, cmp, val);
        }

        final void reset() {
            U.putLongVolatile(this, VALUE, 0L);
        }

        final void reset(long identity) {
            U.putLongVolatile(this, VALUE, identity);
        }

        static {
            try {
                VALUE = U.objectFieldOffset(Cell.class.getDeclaredField("value"));
            } catch (ReflectiveOperationException e) {
                throw new Error(e);
            }
        }
    }

    static {
        try {
            BASE = U.objectFieldOffset(Striped64.class.getDeclaredField("base"));
            CELLSBUSY = U.objectFieldOffset(Striped64.class.getDeclaredField("cellsBusy"));
            PROBE = U.objectFieldOffset(Thread.class.getDeclaredField("threadLocalRandomProbe"));
        } catch (ReflectiveOperationException e) {
            throw new Error(e);
        }
    }

    Striped64() {
    }

    final boolean casBase(long cmp, long val) {
        return U.compareAndSwapLong(this, BASE, cmp, val);
    }

    final boolean casCellsBusy() {
        return U.compareAndSwapInt(this, CELLSBUSY, 0, 1);
    }

    static final int getProbe() {
        return U.getInt(Thread.currentThread(), PROBE);
    }

    static final int advanceProbe(int probe) {
        int probe2 = probe ^ (probe << 13);
        int probe3 = probe2 ^ (probe2 >>> 17);
        int probe4 = probe3 ^ (probe3 << 5);
        U.putInt(Thread.currentThread(), PROBE, probe4);
        return probe4;
    }

    final void longAccumulate(long x, LongBinaryOperator fn, boolean wasUncontended) {
        int n;
        int m;
        int h = getProbe();
        if (h == 0) {
            ThreadLocalRandom.current();
            h = getProbe();
            wasUncontended = true;
        }
        boolean collide = false;
        while (true) {
            Cell[] as = this.cells;
            if (as != null && (n = as.length) > 0) {
                Cell a = as[(n - 1) & h];
                if (a == null) {
                    if (this.cellsBusy == 0) {
                        Cell r = new Cell(x);
                        if (this.cellsBusy == 0 && casCellsBusy()) {
                            try {
                                Cell[] rs = this.cells;
                                if (rs != null && (m = rs.length) > 0) {
                                    int j = (m - 1) & h;
                                    if (rs[j] == null) {
                                        rs[j] = r;
                                        return;
                                    }
                                }
                            } finally {
                            }
                        }
                    }
                    collide = false;
                    h = advanceProbe(h);
                } else {
                    if (!wasUncontended) {
                        wasUncontended = true;
                    } else {
                        long v = a.value;
                        if (a.cas(v, fn == null ? v + x : fn.applyAsLong(v, x))) {
                            return;
                        }
                        if (n >= NCPU || this.cells != as) {
                            collide = false;
                        } else if (!collide) {
                            collide = true;
                        } else if (this.cellsBusy == 0 && casCellsBusy()) {
                            try {
                                if (this.cells == as) {
                                    this.cells = (Cell[]) Arrays.copyOf(as, n << 1);
                                }
                                this.cellsBusy = 0;
                                collide = false;
                            } finally {
                            }
                        }
                    }
                    h = advanceProbe(h);
                }
            } else if (this.cellsBusy == 0 && this.cells == as && casCellsBusy()) {
                try {
                    if (this.cells == as) {
                        Cell[] rs2 = new Cell[2];
                        rs2[h & 1] = new Cell(x);
                        this.cells = rs2;
                        return;
                    }
                } finally {
                }
            } else {
                long v2 = this.base;
                if (casBase(v2, fn == null ? v2 + x : fn.applyAsLong(v2, x))) {
                    return;
                }
            }
        }
    }

    private static long apply(DoubleBinaryOperator fn, long v, double x) {
        double d = Double.longBitsToDouble(v);
        return Double.doubleToRawLongBits(fn == null ? d + x : fn.applyAsDouble(d, x));
    }

    final void doubleAccumulate(double x, DoubleBinaryOperator fn, boolean wasUncontended) {
        int n;
        int m;
        int h = getProbe();
        if (h == 0) {
            ThreadLocalRandom.current();
            h = getProbe();
            wasUncontended = true;
        }
        boolean collide = false;
        while (true) {
            Cell[] as = this.cells;
            if (as != null && (n = as.length) > 0) {
                Cell a = as[(n - 1) & h];
                if (a == null) {
                    if (this.cellsBusy == 0) {
                        Cell r = new Cell(Double.doubleToRawLongBits(x));
                        if (this.cellsBusy == 0 && casCellsBusy()) {
                            try {
                                Cell[] rs = this.cells;
                                if (rs != null && (m = rs.length) > 0) {
                                    int j = (m - 1) & h;
                                    if (rs[j] == null) {
                                        rs[j] = r;
                                        return;
                                    }
                                }
                            } finally {
                            }
                        }
                    }
                    collide = false;
                    h = advanceProbe(h);
                } else {
                    if (!wasUncontended) {
                        wasUncontended = true;
                    } else {
                        long v = a.value;
                        if (a.cas(v, apply(fn, v, x))) {
                            return;
                        }
                        if (n >= NCPU || this.cells != as) {
                            collide = false;
                        } else if (!collide) {
                            collide = true;
                        } else if (this.cellsBusy == 0 && casCellsBusy()) {
                            try {
                                if (this.cells == as) {
                                    this.cells = (Cell[]) Arrays.copyOf(as, n << 1);
                                }
                                this.cellsBusy = 0;
                                collide = false;
                            } finally {
                            }
                        }
                    }
                    h = advanceProbe(h);
                }
            } else if (this.cellsBusy == 0 && this.cells == as && casCellsBusy()) {
                try {
                    if (this.cells == as) {
                        Cell[] rs2 = new Cell[2];
                        rs2[h & 1] = new Cell(Double.doubleToRawLongBits(x));
                        this.cells = rs2;
                        return;
                    }
                } finally {
                }
            } else {
                long v2 = this.base;
                if (casBase(v2, apply(fn, v2, x))) {
                    return;
                }
            }
        }
    }
}
