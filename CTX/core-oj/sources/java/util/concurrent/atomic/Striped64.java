package java.util.concurrent.atomic;

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

        Cell(long j) {
            this.value = j;
        }

        final boolean cas(long j, long j2) {
            return U.compareAndSwapLong(this, VALUE, j, j2);
        }

        final void reset() {
            U.putLongVolatile(this, VALUE, 0L);
        }

        final void reset(long j) {
            U.putLongVolatile(this, VALUE, j);
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

    final boolean casBase(long j, long j2) {
        return U.compareAndSwapLong(this, BASE, j, j2);
    }

    final boolean casCellsBusy() {
        return U.compareAndSwapInt(this, CELLSBUSY, 0, 1);
    }

    static final int getProbe() {
        return U.getInt(Thread.currentThread(), PROBE);
    }

    static final int advanceProbe(int i) {
        int i2 = i ^ (i << 13);
        int i3 = i2 ^ (i2 >>> 17);
        int i4 = i3 ^ (i3 << 5);
        U.putInt(Thread.currentThread(), PROBE, i4);
        return i4;
    }

    final void longAccumulate(long j, LongBinaryOperator longBinaryOperator, boolean z) {
        int length;
        int length2;
        int probe = getProbe();
        if (probe == 0) {
            ThreadLocalRandom.current();
            probe = getProbe();
            z = true;
        }
        while (true) {
            boolean z2 = false;
            while (true) {
                Cell[] cellArr = this.cells;
                if (cellArr != null && (length = cellArr.length) > 0) {
                    Cell cell = cellArr[(length - 1) & probe];
                    if (cell != null) {
                        if (z) {
                            long j2 = cell.value;
                            if (cell.cas(j2, longBinaryOperator == null ? j2 + j : longBinaryOperator.applyAsLong(j2, j))) {
                                return;
                            }
                            if (length < NCPU && this.cells == cellArr) {
                                if (!z2) {
                                    z2 = true;
                                } else if (this.cellsBusy == 0 && casCellsBusy()) {
                                    try {
                                        break;
                                    } finally {
                                    }
                                }
                            }
                        } else {
                            z = true;
                        }
                        probe = advanceProbe(probe);
                    } else if (this.cellsBusy == 0) {
                        Cell cell2 = new Cell(j);
                        if (this.cellsBusy == 0 && casCellsBusy()) {
                            try {
                                Cell[] cellArr2 = this.cells;
                                if (cellArr2 != null && (length2 = cellArr2.length) > 0) {
                                    int i = (length2 - 1) & probe;
                                    if (cellArr2[i] == null) {
                                        cellArr2[i] = cell2;
                                        return;
                                    }
                                }
                                this.cellsBusy = 0;
                            } finally {
                            }
                        }
                    }
                    z2 = false;
                    probe = advanceProbe(probe);
                } else if (this.cellsBusy == 0 && this.cells == cellArr && casCellsBusy()) {
                    try {
                        if (this.cells == cellArr) {
                            Cell[] cellArr3 = new Cell[2];
                            cellArr3[probe & 1] = new Cell(j);
                            this.cells = cellArr3;
                            return;
                        }
                    } finally {
                    }
                } else {
                    long j3 = this.base;
                    if (casBase(j3, longBinaryOperator == null ? j3 + j : longBinaryOperator.applyAsLong(j3, j))) {
                        return;
                    }
                }
            }
        }
    }

    private static long apply(DoubleBinaryOperator doubleBinaryOperator, long j, double d) {
        double dLongBitsToDouble = Double.longBitsToDouble(j);
        return Double.doubleToRawLongBits(doubleBinaryOperator == null ? dLongBitsToDouble + d : doubleBinaryOperator.applyAsDouble(dLongBitsToDouble, d));
    }

    final void doubleAccumulate(double d, DoubleBinaryOperator doubleBinaryOperator, boolean z) {
        int length;
        int length2;
        int probe = getProbe();
        if (probe == 0) {
            ThreadLocalRandom.current();
            probe = getProbe();
            z = true;
        }
        while (true) {
            boolean z2 = false;
            while (true) {
                Cell[] cellArr = this.cells;
                if (cellArr != null && (length = cellArr.length) > 0) {
                    Cell cell = cellArr[(length - 1) & probe];
                    if (cell != null) {
                        if (z) {
                            long j = cell.value;
                            if (cell.cas(j, apply(doubleBinaryOperator, j, d))) {
                                return;
                            }
                            if (length < NCPU && this.cells == cellArr) {
                                if (!z2) {
                                    z2 = true;
                                } else if (this.cellsBusy == 0 && casCellsBusy()) {
                                    try {
                                        break;
                                    } finally {
                                    }
                                }
                            }
                        } else {
                            z = true;
                        }
                        probe = advanceProbe(probe);
                    } else if (this.cellsBusy == 0) {
                        Cell cell2 = new Cell(Double.doubleToRawLongBits(d));
                        if (this.cellsBusy == 0 && casCellsBusy()) {
                            try {
                                Cell[] cellArr2 = this.cells;
                                if (cellArr2 != null && (length2 = cellArr2.length) > 0) {
                                    int i = (length2 - 1) & probe;
                                    if (cellArr2[i] == null) {
                                        cellArr2[i] = cell2;
                                        return;
                                    }
                                }
                                this.cellsBusy = 0;
                            } finally {
                            }
                        }
                    }
                    z2 = false;
                    probe = advanceProbe(probe);
                } else if (this.cellsBusy == 0 && this.cells == cellArr && casCellsBusy()) {
                    try {
                        if (this.cells == cellArr) {
                            Cell[] cellArr3 = new Cell[2];
                            cellArr3[probe & 1] = new Cell(Double.doubleToRawLongBits(d));
                            this.cells = cellArr3;
                            return;
                        }
                    } finally {
                    }
                } else {
                    long j2 = this.base;
                    if (casBase(j2, apply(doubleBinaryOperator, j2, d))) {
                        return;
                    }
                }
            }
        }
    }
}
