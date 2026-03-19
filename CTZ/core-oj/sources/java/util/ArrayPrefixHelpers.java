package java.util;

import java.util.concurrent.CountedCompleter;
import java.util.concurrent.ForkJoinPool;
import java.util.function.BinaryOperator;
import java.util.function.DoubleBinaryOperator;
import java.util.function.IntBinaryOperator;
import java.util.function.LongBinaryOperator;

class ArrayPrefixHelpers {
    static final int CUMULATE = 1;
    static final int FINISHED = 4;
    static final int MIN_PARTITION = 16;
    static final int SUMMED = 2;

    private ArrayPrefixHelpers() {
    }

    static final class CumulateTask<T> extends CountedCompleter<Void> {
        private static final long serialVersionUID = 5293554502939613543L;
        final T[] array;
        final int fence;
        final BinaryOperator<T> function;
        final int hi;
        T in;
        CumulateTask<T> left;
        final int lo;
        final int origin;
        T out;
        CumulateTask<T> right;
        final int threshold;

        public CumulateTask(CumulateTask<T> cumulateTask, BinaryOperator<T> binaryOperator, T[] tArr, int i, int i2) {
            super(cumulateTask);
            this.function = binaryOperator;
            this.array = tArr;
            this.origin = i;
            this.lo = i;
            this.fence = i2;
            this.hi = i2;
            int commonPoolParallelism = (i2 - i) / (ForkJoinPool.getCommonPoolParallelism() << 3);
            this.threshold = commonPoolParallelism > 16 ? commonPoolParallelism : 16;
        }

        CumulateTask(CumulateTask<T> cumulateTask, BinaryOperator<T> binaryOperator, T[] tArr, int i, int i2, int i3, int i4, int i5) {
            super(cumulateTask);
            this.function = binaryOperator;
            this.array = tArr;
            this.origin = i;
            this.fence = i2;
            this.threshold = i3;
            this.lo = i4;
            this.hi = i5;
        }

        @Override
        public final void compute() {
            T[] tArr;
            int i;
            int pendingCount;
            int i2;
            T tApply;
            int i3;
            CumulateTask<T> cumulateTask;
            int i4;
            int i5;
            int pendingCount2;
            CumulateTask<T> cumulateTask2;
            CumulateTask<T> cumulateTask3;
            int pendingCount3;
            BinaryOperator<T> binaryOperator = this.function;
            if (binaryOperator == null || (tArr = this.array) == null) {
                throw new NullPointerException();
            }
            int i6 = this.threshold;
            int i7 = this.origin;
            int i8 = this.fence;
            CumulateTask<T> cumulateTask4 = this;
            while (true) {
                int i9 = cumulateTask4.lo;
                if (i9 >= 0 && (i = cumulateTask4.hi) <= tArr.length) {
                    if (i - i9 > i6) {
                        CumulateTask<T> cumulateTask5 = cumulateTask4.left;
                        CumulateTask<T> cumulateTask6 = cumulateTask4.right;
                        if (cumulateTask5 == null) {
                            int i10 = (i9 + i) >>> 1;
                            CumulateTask<T> cumulateTask7 = cumulateTask4;
                            int i11 = i6;
                            cumulateTask3 = cumulateTask;
                            CumulateTask<T> cumulateTask8 = new CumulateTask<>(cumulateTask7, binaryOperator, tArr, i7, i8, i11, i10, i);
                            cumulateTask4.right = cumulateTask3;
                            i5 = i6;
                            CumulateTask<T> cumulateTask9 = new CumulateTask<>(cumulateTask7, binaryOperator, tArr, i7, i8, i11, i9, i10);
                            cumulateTask4.left = cumulateTask9;
                            cumulateTask4 = cumulateTask9;
                        } else {
                            i5 = i6;
                            T t = cumulateTask4.in;
                            cumulateTask5.in = t;
                            if (cumulateTask6 != null) {
                                T tApply2 = cumulateTask5.out;
                                if (i9 != i7) {
                                    tApply2 = binaryOperator.apply(t, tApply2);
                                }
                                cumulateTask6.in = tApply2;
                                do {
                                    pendingCount3 = cumulateTask6.getPendingCount();
                                    if ((pendingCount3 & 1) != 0) {
                                        cumulateTask6 = null;
                                        break;
                                    }
                                } while (!cumulateTask6.compareAndSetPendingCount(pendingCount3, pendingCount3 | 1));
                                while (true) {
                                    pendingCount2 = cumulateTask5.getPendingCount();
                                    if ((pendingCount2 & 1) != 0) {
                                        if (cumulateTask5.compareAndSetPendingCount(pendingCount2, pendingCount2 | 1)) {
                                            if (cumulateTask6 == null) {
                                                cumulateTask6 = null;
                                            }
                                            cumulateTask2 = cumulateTask6;
                                        }
                                    } else {
                                        cumulateTask5 = cumulateTask6;
                                        cumulateTask2 = null;
                                        break;
                                    }
                                }
                                if (cumulateTask5 == null) {
                                    cumulateTask4 = cumulateTask5;
                                    cumulateTask3 = cumulateTask2;
                                } else {
                                    return;
                                }
                            } else {
                                cumulateTask6 = null;
                                while (true) {
                                    pendingCount2 = cumulateTask5.getPendingCount();
                                    if ((pendingCount2 & 1) != 0) {
                                    }
                                }
                                if (cumulateTask5 == null) {
                                }
                            }
                        }
                        if (cumulateTask3 != null) {
                            cumulateTask3.fork();
                        }
                        i6 = i5;
                    } else {
                        do {
                            pendingCount = cumulateTask4.getPendingCount();
                            if ((pendingCount & 4) == 0) {
                                i2 = (pendingCount & 1) != 0 ? 4 : i9 > i7 ? 2 : 6;
                            } else {
                                return;
                            }
                        } while (!cumulateTask4.compareAndSetPendingCount(pendingCount, pendingCount | i2));
                        if (i2 != 2) {
                            if (i9 == i7) {
                                tApply = tArr[i7];
                                i4 = i7 + 1;
                            } else {
                                tApply = cumulateTask4.in;
                                i4 = i9;
                            }
                            while (i4 < i) {
                                tApply = binaryOperator.apply(tApply, tArr[i4]);
                                tArr[i4] = tApply;
                                i4++;
                            }
                        } else if (i < i8) {
                            tApply = tArr[i9];
                            for (int i12 = i9 + 1; i12 < i; i12++) {
                                tApply = binaryOperator.apply(tApply, tArr[i12]);
                            }
                        } else {
                            tApply = cumulateTask4.in;
                        }
                        cumulateTask4.out = tApply;
                        while (true) {
                            CumulateTask<T> cumulateTask10 = (CumulateTask) cumulateTask4.getCompleter();
                            if (cumulateTask10 == null) {
                                if ((i2 & 4) != 0) {
                                    cumulateTask4.quietlyComplete();
                                    return;
                                }
                                return;
                            }
                            int pendingCount4 = cumulateTask10.getPendingCount();
                            int i13 = pendingCount4 & i2;
                            if ((i13 & 4) == 0) {
                                if ((i13 & 2) != 0) {
                                    CumulateTask<T> cumulateTask11 = cumulateTask10.left;
                                    if (cumulateTask11 != null && (cumulateTask = cumulateTask10.right) != null) {
                                        T tApply3 = cumulateTask11.out;
                                        if (cumulateTask.hi != i8) {
                                            tApply3 = binaryOperator.apply(tApply3, cumulateTask.out);
                                        }
                                        cumulateTask10.out = tApply3;
                                    }
                                    if ((pendingCount4 & 1) != 0 || cumulateTask10.lo != i7) {
                                        i3 = 0;
                                    } else {
                                        i3 = 1;
                                    }
                                    int i14 = pendingCount4 | i2 | i3;
                                    if (i14 == pendingCount4 || cumulateTask10.compareAndSetPendingCount(pendingCount4, i14)) {
                                        if (i3 != 0) {
                                            cumulateTask10.fork();
                                        }
                                        cumulateTask4 = cumulateTask10;
                                        i2 = 2;
                                    }
                                } else if (cumulateTask10.compareAndSetPendingCount(pendingCount4, pendingCount4 | i2)) {
                                    return;
                                }
                            } else {
                                cumulateTask4 = cumulateTask10;
                            }
                        }
                    }
                } else {
                    return;
                }
            }
        }
    }

    static final class LongCumulateTask extends CountedCompleter<Void> {
        private static final long serialVersionUID = -5074099945909284273L;
        final long[] array;
        final int fence;
        final LongBinaryOperator function;
        final int hi;
        long in;
        LongCumulateTask left;
        final int lo;
        final int origin;
        long out;
        LongCumulateTask right;
        final int threshold;

        public LongCumulateTask(LongCumulateTask longCumulateTask, LongBinaryOperator longBinaryOperator, long[] jArr, int i, int i2) {
            super(longCumulateTask);
            this.function = longBinaryOperator;
            this.array = jArr;
            this.origin = i;
            this.lo = i;
            this.fence = i2;
            this.hi = i2;
            int commonPoolParallelism = (i2 - i) / (ForkJoinPool.getCommonPoolParallelism() << 3);
            this.threshold = commonPoolParallelism > 16 ? commonPoolParallelism : 16;
        }

        LongCumulateTask(LongCumulateTask longCumulateTask, LongBinaryOperator longBinaryOperator, long[] jArr, int i, int i2, int i3, int i4, int i5) {
            super(longCumulateTask);
            this.function = longBinaryOperator;
            this.array = jArr;
            this.origin = i;
            this.fence = i2;
            this.threshold = i3;
            this.lo = i4;
            this.hi = i5;
        }

        @Override
        public final void compute() {
            long[] jArr;
            int i;
            int pendingCount;
            int i2;
            long jApplyAsLong;
            int i3;
            LongCumulateTask longCumulateTask;
            int i4;
            int i5;
            int pendingCount2;
            LongCumulateTask longCumulateTask2;
            LongCumulateTask longCumulateTask3;
            int pendingCount3;
            LongBinaryOperator longBinaryOperator = this.function;
            if (longBinaryOperator == null || (jArr = this.array) == null) {
                throw new NullPointerException();
            }
            int i6 = this.threshold;
            int i7 = this.origin;
            int i8 = this.fence;
            LongCumulateTask longCumulateTask4 = this;
            while (true) {
                int i9 = longCumulateTask4.lo;
                if (i9 >= 0 && (i = longCumulateTask4.hi) <= jArr.length) {
                    if (i - i9 > i6) {
                        LongCumulateTask longCumulateTask5 = longCumulateTask4.left;
                        LongCumulateTask longCumulateTask6 = longCumulateTask4.right;
                        if (longCumulateTask5 == null) {
                            int i10 = (i9 + i) >>> 1;
                            LongCumulateTask longCumulateTask7 = longCumulateTask4;
                            int i11 = i6;
                            longCumulateTask3 = longCumulateTask;
                            LongCumulateTask longCumulateTask8 = new LongCumulateTask(longCumulateTask7, longBinaryOperator, jArr, i7, i8, i11, i10, i);
                            longCumulateTask4.right = longCumulateTask3;
                            i5 = i6;
                            LongCumulateTask longCumulateTask9 = new LongCumulateTask(longCumulateTask7, longBinaryOperator, jArr, i7, i8, i11, i9, i10);
                            longCumulateTask4.left = longCumulateTask9;
                            longCumulateTask4 = longCumulateTask9;
                        } else {
                            i5 = i6;
                            long j = longCumulateTask4.in;
                            longCumulateTask5.in = j;
                            if (longCumulateTask6 != null) {
                                long jApplyAsLong2 = longCumulateTask5.out;
                                if (i9 != i7) {
                                    jApplyAsLong2 = longBinaryOperator.applyAsLong(j, jApplyAsLong2);
                                }
                                longCumulateTask6.in = jApplyAsLong2;
                                do {
                                    pendingCount3 = longCumulateTask6.getPendingCount();
                                    if ((pendingCount3 & 1) != 0) {
                                        longCumulateTask6 = null;
                                        break;
                                    }
                                } while (!longCumulateTask6.compareAndSetPendingCount(pendingCount3, pendingCount3 | 1));
                                while (true) {
                                    pendingCount2 = longCumulateTask5.getPendingCount();
                                    if ((pendingCount2 & 1) != 0) {
                                        if (longCumulateTask5.compareAndSetPendingCount(pendingCount2, pendingCount2 | 1)) {
                                            if (longCumulateTask6 == null) {
                                                longCumulateTask6 = null;
                                            }
                                            longCumulateTask2 = longCumulateTask6;
                                        }
                                    } else {
                                        longCumulateTask5 = longCumulateTask6;
                                        longCumulateTask2 = null;
                                        break;
                                    }
                                }
                                if (longCumulateTask5 == null) {
                                    longCumulateTask4 = longCumulateTask5;
                                    longCumulateTask3 = longCumulateTask2;
                                } else {
                                    return;
                                }
                            } else {
                                longCumulateTask6 = null;
                                while (true) {
                                    pendingCount2 = longCumulateTask5.getPendingCount();
                                    if ((pendingCount2 & 1) != 0) {
                                    }
                                }
                                if (longCumulateTask5 == null) {
                                }
                            }
                        }
                        if (longCumulateTask3 != null) {
                            longCumulateTask3.fork();
                        }
                        i6 = i5;
                    } else {
                        do {
                            pendingCount = longCumulateTask4.getPendingCount();
                            if ((pendingCount & 4) == 0) {
                                i2 = (pendingCount & 1) != 0 ? 4 : i9 > i7 ? 2 : 6;
                            } else {
                                return;
                            }
                        } while (!longCumulateTask4.compareAndSetPendingCount(pendingCount, pendingCount | i2));
                        if (i2 != 2) {
                            if (i9 == i7) {
                                jApplyAsLong = jArr[i7];
                                i4 = i7 + 1;
                            } else {
                                jApplyAsLong = longCumulateTask4.in;
                                i4 = i9;
                            }
                            while (i4 < i) {
                                jApplyAsLong = longBinaryOperator.applyAsLong(jApplyAsLong, jArr[i4]);
                                jArr[i4] = jApplyAsLong;
                                i4++;
                            }
                        } else if (i < i8) {
                            long j2 = jArr[i9];
                            jApplyAsLong = j2;
                            for (int i12 = i9 + 1; i12 < i; i12++) {
                                jApplyAsLong = longBinaryOperator.applyAsLong(jApplyAsLong, jArr[i12]);
                            }
                        } else {
                            jApplyAsLong = longCumulateTask4.in;
                        }
                        longCumulateTask4.out = jApplyAsLong;
                        int i13 = i2;
                        while (true) {
                            LongCumulateTask longCumulateTask10 = (LongCumulateTask) longCumulateTask4.getCompleter();
                            if (longCumulateTask10 == null) {
                                if ((i13 & 4) != 0) {
                                    longCumulateTask4.quietlyComplete();
                                    return;
                                }
                                return;
                            }
                            int pendingCount4 = longCumulateTask10.getPendingCount();
                            int i14 = pendingCount4 & i13;
                            if ((i14 & 4) == 0) {
                                if ((i14 & 2) != 0) {
                                    LongCumulateTask longCumulateTask11 = longCumulateTask10.left;
                                    if (longCumulateTask11 != null && (longCumulateTask = longCumulateTask10.right) != null) {
                                        long jApplyAsLong3 = longCumulateTask11.out;
                                        if (longCumulateTask.hi != i8) {
                                            jApplyAsLong3 = longBinaryOperator.applyAsLong(jApplyAsLong3, longCumulateTask.out);
                                        }
                                        longCumulateTask10.out = jApplyAsLong3;
                                    }
                                    if ((pendingCount4 & 1) != 0 || longCumulateTask10.lo != i7) {
                                        i3 = 0;
                                    } else {
                                        i3 = 1;
                                    }
                                    int i15 = pendingCount4 | i13 | i3;
                                    if (i15 == pendingCount4 || longCumulateTask10.compareAndSetPendingCount(pendingCount4, i15)) {
                                        if (i3 != 0) {
                                            longCumulateTask10.fork();
                                        }
                                        longCumulateTask4 = longCumulateTask10;
                                        i13 = 2;
                                    }
                                } else if (longCumulateTask10.compareAndSetPendingCount(pendingCount4, pendingCount4 | i13)) {
                                    return;
                                }
                            } else {
                                longCumulateTask4 = longCumulateTask10;
                            }
                        }
                    }
                } else {
                    return;
                }
            }
        }
    }

    static final class DoubleCumulateTask extends CountedCompleter<Void> {
        private static final long serialVersionUID = -586947823794232033L;
        final double[] array;
        final int fence;
        final DoubleBinaryOperator function;
        final int hi;
        double in;
        DoubleCumulateTask left;
        final int lo;
        final int origin;
        double out;
        DoubleCumulateTask right;
        final int threshold;

        public DoubleCumulateTask(DoubleCumulateTask doubleCumulateTask, DoubleBinaryOperator doubleBinaryOperator, double[] dArr, int i, int i2) {
            super(doubleCumulateTask);
            this.function = doubleBinaryOperator;
            this.array = dArr;
            this.origin = i;
            this.lo = i;
            this.fence = i2;
            this.hi = i2;
            int commonPoolParallelism = (i2 - i) / (ForkJoinPool.getCommonPoolParallelism() << 3);
            this.threshold = commonPoolParallelism > 16 ? commonPoolParallelism : 16;
        }

        DoubleCumulateTask(DoubleCumulateTask doubleCumulateTask, DoubleBinaryOperator doubleBinaryOperator, double[] dArr, int i, int i2, int i3, int i4, int i5) {
            super(doubleCumulateTask);
            this.function = doubleBinaryOperator;
            this.array = dArr;
            this.origin = i;
            this.fence = i2;
            this.threshold = i3;
            this.lo = i4;
            this.hi = i5;
        }

        @Override
        public final void compute() {
            double[] dArr;
            int i;
            int pendingCount;
            int i2;
            double dApplyAsDouble;
            int i3;
            DoubleCumulateTask doubleCumulateTask;
            int i4;
            int i5;
            int pendingCount2;
            DoubleCumulateTask doubleCumulateTask2;
            DoubleCumulateTask doubleCumulateTask3;
            int pendingCount3;
            DoubleBinaryOperator doubleBinaryOperator = this.function;
            if (doubleBinaryOperator == null || (dArr = this.array) == null) {
                throw new NullPointerException();
            }
            int i6 = this.threshold;
            int i7 = this.origin;
            int i8 = this.fence;
            DoubleCumulateTask doubleCumulateTask4 = this;
            while (true) {
                int i9 = doubleCumulateTask4.lo;
                if (i9 >= 0 && (i = doubleCumulateTask4.hi) <= dArr.length) {
                    if (i - i9 > i6) {
                        DoubleCumulateTask doubleCumulateTask5 = doubleCumulateTask4.left;
                        DoubleCumulateTask doubleCumulateTask6 = doubleCumulateTask4.right;
                        if (doubleCumulateTask5 == null) {
                            int i10 = (i9 + i) >>> 1;
                            DoubleCumulateTask doubleCumulateTask7 = doubleCumulateTask4;
                            int i11 = i6;
                            doubleCumulateTask3 = doubleCumulateTask;
                            DoubleCumulateTask doubleCumulateTask8 = new DoubleCumulateTask(doubleCumulateTask7, doubleBinaryOperator, dArr, i7, i8, i11, i10, i);
                            doubleCumulateTask4.right = doubleCumulateTask3;
                            i5 = i6;
                            DoubleCumulateTask doubleCumulateTask9 = new DoubleCumulateTask(doubleCumulateTask7, doubleBinaryOperator, dArr, i7, i8, i11, i9, i10);
                            doubleCumulateTask4.left = doubleCumulateTask9;
                            doubleCumulateTask4 = doubleCumulateTask9;
                        } else {
                            i5 = i6;
                            double d = doubleCumulateTask4.in;
                            doubleCumulateTask5.in = d;
                            if (doubleCumulateTask6 != null) {
                                double dApplyAsDouble2 = doubleCumulateTask5.out;
                                if (i9 != i7) {
                                    dApplyAsDouble2 = doubleBinaryOperator.applyAsDouble(d, dApplyAsDouble2);
                                }
                                doubleCumulateTask6.in = dApplyAsDouble2;
                                do {
                                    pendingCount3 = doubleCumulateTask6.getPendingCount();
                                    if ((pendingCount3 & 1) != 0) {
                                        doubleCumulateTask6 = null;
                                        break;
                                    }
                                } while (!doubleCumulateTask6.compareAndSetPendingCount(pendingCount3, pendingCount3 | 1));
                                while (true) {
                                    pendingCount2 = doubleCumulateTask5.getPendingCount();
                                    if ((pendingCount2 & 1) != 0) {
                                        if (doubleCumulateTask5.compareAndSetPendingCount(pendingCount2, pendingCount2 | 1)) {
                                            if (doubleCumulateTask6 == null) {
                                                doubleCumulateTask6 = null;
                                            }
                                            doubleCumulateTask2 = doubleCumulateTask6;
                                        }
                                    } else {
                                        doubleCumulateTask5 = doubleCumulateTask6;
                                        doubleCumulateTask2 = null;
                                        break;
                                    }
                                }
                                if (doubleCumulateTask5 == null) {
                                    doubleCumulateTask4 = doubleCumulateTask5;
                                    doubleCumulateTask3 = doubleCumulateTask2;
                                } else {
                                    return;
                                }
                            } else {
                                doubleCumulateTask6 = null;
                                while (true) {
                                    pendingCount2 = doubleCumulateTask5.getPendingCount();
                                    if ((pendingCount2 & 1) != 0) {
                                    }
                                }
                                if (doubleCumulateTask5 == null) {
                                }
                            }
                        }
                        if (doubleCumulateTask3 != null) {
                            doubleCumulateTask3.fork();
                        }
                        i6 = i5;
                    } else {
                        do {
                            pendingCount = doubleCumulateTask4.getPendingCount();
                            if ((pendingCount & 4) == 0) {
                                i2 = (pendingCount & 1) != 0 ? 4 : i9 > i7 ? 2 : 6;
                            } else {
                                return;
                            }
                        } while (!doubleCumulateTask4.compareAndSetPendingCount(pendingCount, pendingCount | i2));
                        if (i2 != 2) {
                            if (i9 == i7) {
                                dApplyAsDouble = dArr[i7];
                                i4 = i7 + 1;
                            } else {
                                dApplyAsDouble = doubleCumulateTask4.in;
                                i4 = i9;
                            }
                            while (i4 < i) {
                                dApplyAsDouble = doubleBinaryOperator.applyAsDouble(dApplyAsDouble, dArr[i4]);
                                dArr[i4] = dApplyAsDouble;
                                i4++;
                            }
                        } else if (i < i8) {
                            double d2 = dArr[i9];
                            dApplyAsDouble = d2;
                            for (int i12 = i9 + 1; i12 < i; i12++) {
                                dApplyAsDouble = doubleBinaryOperator.applyAsDouble(dApplyAsDouble, dArr[i12]);
                            }
                        } else {
                            dApplyAsDouble = doubleCumulateTask4.in;
                        }
                        doubleCumulateTask4.out = dApplyAsDouble;
                        int i13 = i2;
                        while (true) {
                            DoubleCumulateTask doubleCumulateTask10 = (DoubleCumulateTask) doubleCumulateTask4.getCompleter();
                            if (doubleCumulateTask10 == null) {
                                if ((i13 & 4) != 0) {
                                    doubleCumulateTask4.quietlyComplete();
                                    return;
                                }
                                return;
                            }
                            int pendingCount4 = doubleCumulateTask10.getPendingCount();
                            int i14 = pendingCount4 & i13;
                            if ((i14 & 4) == 0) {
                                if ((i14 & 2) != 0) {
                                    DoubleCumulateTask doubleCumulateTask11 = doubleCumulateTask10.left;
                                    if (doubleCumulateTask11 != null && (doubleCumulateTask = doubleCumulateTask10.right) != null) {
                                        double dApplyAsDouble3 = doubleCumulateTask11.out;
                                        if (doubleCumulateTask.hi != i8) {
                                            dApplyAsDouble3 = doubleBinaryOperator.applyAsDouble(dApplyAsDouble3, doubleCumulateTask.out);
                                        }
                                        doubleCumulateTask10.out = dApplyAsDouble3;
                                    }
                                    if ((pendingCount4 & 1) != 0 || doubleCumulateTask10.lo != i7) {
                                        i3 = 0;
                                    } else {
                                        i3 = 1;
                                    }
                                    int i15 = pendingCount4 | i13 | i3;
                                    if (i15 == pendingCount4 || doubleCumulateTask10.compareAndSetPendingCount(pendingCount4, i15)) {
                                        if (i3 != 0) {
                                            doubleCumulateTask10.fork();
                                        }
                                        doubleCumulateTask4 = doubleCumulateTask10;
                                        i13 = 2;
                                    }
                                } else if (doubleCumulateTask10.compareAndSetPendingCount(pendingCount4, pendingCount4 | i13)) {
                                    return;
                                }
                            } else {
                                doubleCumulateTask4 = doubleCumulateTask10;
                            }
                        }
                    }
                } else {
                    return;
                }
            }
        }
    }

    static final class IntCumulateTask extends CountedCompleter<Void> {
        private static final long serialVersionUID = 3731755594596840961L;
        final int[] array;
        final int fence;
        final IntBinaryOperator function;
        final int hi;
        int in;
        IntCumulateTask left;
        final int lo;
        final int origin;
        int out;
        IntCumulateTask right;
        final int threshold;

        public IntCumulateTask(IntCumulateTask intCumulateTask, IntBinaryOperator intBinaryOperator, int[] iArr, int i, int i2) {
            super(intCumulateTask);
            this.function = intBinaryOperator;
            this.array = iArr;
            this.origin = i;
            this.lo = i;
            this.fence = i2;
            this.hi = i2;
            int commonPoolParallelism = (i2 - i) / (ForkJoinPool.getCommonPoolParallelism() << 3);
            this.threshold = commonPoolParallelism > 16 ? commonPoolParallelism : 16;
        }

        IntCumulateTask(IntCumulateTask intCumulateTask, IntBinaryOperator intBinaryOperator, int[] iArr, int i, int i2, int i3, int i4, int i5) {
            super(intCumulateTask);
            this.function = intBinaryOperator;
            this.array = iArr;
            this.origin = i;
            this.fence = i2;
            this.threshold = i3;
            this.lo = i4;
            this.hi = i5;
        }

        @Override
        public final void compute() {
            int[] iArr;
            int i;
            int pendingCount;
            int i2;
            int iApplyAsInt;
            int i3;
            IntCumulateTask intCumulateTask;
            int i4;
            int i5;
            int pendingCount2;
            IntCumulateTask intCumulateTask2;
            IntCumulateTask intCumulateTask3;
            int pendingCount3;
            IntBinaryOperator intBinaryOperator = this.function;
            if (intBinaryOperator == null || (iArr = this.array) == null) {
                throw new NullPointerException();
            }
            int i6 = this.threshold;
            int i7 = this.origin;
            int i8 = this.fence;
            IntCumulateTask intCumulateTask4 = this;
            while (true) {
                int i9 = intCumulateTask4.lo;
                if (i9 >= 0 && (i = intCumulateTask4.hi) <= iArr.length) {
                    if (i - i9 > i6) {
                        IntCumulateTask intCumulateTask5 = intCumulateTask4.left;
                        IntCumulateTask intCumulateTask6 = intCumulateTask4.right;
                        if (intCumulateTask5 == null) {
                            int i10 = (i9 + i) >>> 1;
                            IntCumulateTask intCumulateTask7 = intCumulateTask4;
                            int i11 = i6;
                            intCumulateTask3 = intCumulateTask;
                            IntCumulateTask intCumulateTask8 = new IntCumulateTask(intCumulateTask7, intBinaryOperator, iArr, i7, i8, i11, i10, i);
                            intCumulateTask4.right = intCumulateTask3;
                            i5 = i6;
                            IntCumulateTask intCumulateTask9 = new IntCumulateTask(intCumulateTask7, intBinaryOperator, iArr, i7, i8, i11, i9, i10);
                            intCumulateTask4.left = intCumulateTask9;
                            intCumulateTask4 = intCumulateTask9;
                        } else {
                            i5 = i6;
                            int i12 = intCumulateTask4.in;
                            intCumulateTask5.in = i12;
                            if (intCumulateTask6 != null) {
                                int iApplyAsInt2 = intCumulateTask5.out;
                                if (i9 != i7) {
                                    iApplyAsInt2 = intBinaryOperator.applyAsInt(i12, iApplyAsInt2);
                                }
                                intCumulateTask6.in = iApplyAsInt2;
                                do {
                                    pendingCount3 = intCumulateTask6.getPendingCount();
                                    if ((pendingCount3 & 1) != 0) {
                                        intCumulateTask6 = null;
                                        break;
                                    }
                                } while (!intCumulateTask6.compareAndSetPendingCount(pendingCount3, pendingCount3 | 1));
                                while (true) {
                                    pendingCount2 = intCumulateTask5.getPendingCount();
                                    if ((pendingCount2 & 1) != 0) {
                                        if (intCumulateTask5.compareAndSetPendingCount(pendingCount2, pendingCount2 | 1)) {
                                            if (intCumulateTask6 == null) {
                                                intCumulateTask6 = null;
                                            }
                                            intCumulateTask2 = intCumulateTask6;
                                        }
                                    } else {
                                        intCumulateTask5 = intCumulateTask6;
                                        intCumulateTask2 = null;
                                        break;
                                    }
                                }
                                if (intCumulateTask5 == null) {
                                    intCumulateTask4 = intCumulateTask5;
                                    intCumulateTask3 = intCumulateTask2;
                                } else {
                                    return;
                                }
                            } else {
                                intCumulateTask6 = null;
                                while (true) {
                                    pendingCount2 = intCumulateTask5.getPendingCount();
                                    if ((pendingCount2 & 1) != 0) {
                                    }
                                }
                                if (intCumulateTask5 == null) {
                                }
                            }
                        }
                        if (intCumulateTask3 != null) {
                            intCumulateTask3.fork();
                        }
                        i6 = i5;
                    } else {
                        do {
                            pendingCount = intCumulateTask4.getPendingCount();
                            if ((pendingCount & 4) == 0) {
                                i2 = (pendingCount & 1) != 0 ? 4 : i9 > i7 ? 2 : 6;
                            } else {
                                return;
                            }
                        } while (!intCumulateTask4.compareAndSetPendingCount(pendingCount, pendingCount | i2));
                        if (i2 != 2) {
                            if (i9 == i7) {
                                iApplyAsInt = iArr[i7];
                                i4 = i7 + 1;
                            } else {
                                iApplyAsInt = intCumulateTask4.in;
                                i4 = i9;
                            }
                            while (i4 < i) {
                                iApplyAsInt = intBinaryOperator.applyAsInt(iApplyAsInt, iArr[i4]);
                                iArr[i4] = iApplyAsInt;
                                i4++;
                            }
                        } else if (i < i8) {
                            iApplyAsInt = iArr[i9];
                            for (int i13 = i9 + 1; i13 < i; i13++) {
                                iApplyAsInt = intBinaryOperator.applyAsInt(iApplyAsInt, iArr[i13]);
                            }
                        } else {
                            iApplyAsInt = intCumulateTask4.in;
                        }
                        intCumulateTask4.out = iApplyAsInt;
                        while (true) {
                            IntCumulateTask intCumulateTask10 = (IntCumulateTask) intCumulateTask4.getCompleter();
                            if (intCumulateTask10 == null) {
                                if ((i2 & 4) != 0) {
                                    intCumulateTask4.quietlyComplete();
                                    return;
                                }
                                return;
                            }
                            int pendingCount4 = intCumulateTask10.getPendingCount();
                            int i14 = pendingCount4 & i2;
                            if ((i14 & 4) == 0) {
                                if ((i14 & 2) != 0) {
                                    IntCumulateTask intCumulateTask11 = intCumulateTask10.left;
                                    if (intCumulateTask11 != null && (intCumulateTask = intCumulateTask10.right) != null) {
                                        int iApplyAsInt3 = intCumulateTask11.out;
                                        if (intCumulateTask.hi != i8) {
                                            iApplyAsInt3 = intBinaryOperator.applyAsInt(iApplyAsInt3, intCumulateTask.out);
                                        }
                                        intCumulateTask10.out = iApplyAsInt3;
                                    }
                                    if ((pendingCount4 & 1) != 0 || intCumulateTask10.lo != i7) {
                                        i3 = 0;
                                    } else {
                                        i3 = 1;
                                    }
                                    int i15 = pendingCount4 | i2 | i3;
                                    if (i15 == pendingCount4 || intCumulateTask10.compareAndSetPendingCount(pendingCount4, i15)) {
                                        if (i3 != 0) {
                                            intCumulateTask10.fork();
                                        }
                                        intCumulateTask4 = intCumulateTask10;
                                        i2 = 2;
                                    }
                                } else if (intCumulateTask10.compareAndSetPendingCount(pendingCount4, pendingCount4 | i2)) {
                                    return;
                                }
                            } else {
                                intCumulateTask4 = intCumulateTask10;
                            }
                        }
                    }
                } else {
                    return;
                }
            }
        }
    }
}
