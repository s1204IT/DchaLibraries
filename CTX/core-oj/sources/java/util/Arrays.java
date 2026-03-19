package java.util;

import android.R;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.ArrayPrefixHelpers;
import java.util.ArraysParallelSortHelpers;
import java.util.Spliterator;
import java.util.concurrent.ForkJoinPool;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.DoubleBinaryOperator;
import java.util.function.IntBinaryOperator;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import java.util.function.IntToDoubleFunction;
import java.util.function.IntToLongFunction;
import java.util.function.IntUnaryOperator;
import java.util.function.LongBinaryOperator;
import java.util.function.UnaryOperator;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class Arrays {
    private static final int INSERTIONSORT_THRESHOLD = 7;
    public static final int MIN_ARRAY_SORT_GRAN = 8192;

    private Arrays() {
    }

    static final class NaturalOrder implements Comparator<Object> {
        static final NaturalOrder INSTANCE = new NaturalOrder();

        NaturalOrder() {
        }

        @Override
        public int compare(Object obj, Object obj2) {
            return ((Comparable) obj).compareTo(obj2);
        }
    }

    private static void rangeCheck(int i, int i2, int i3) {
        if (i2 > i3) {
            throw new IllegalArgumentException("fromIndex(" + i2 + ") > toIndex(" + i3 + ")");
        }
        if (i2 < 0) {
            throw new ArrayIndexOutOfBoundsException(i2);
        }
        if (i3 > i) {
            throw new ArrayIndexOutOfBoundsException(i3);
        }
    }

    public static void checkOffsetAndCount(int i, int i2, int i3) {
        if ((i2 | i3) < 0 || i2 > i || i - i2 < i3) {
            throw new ArrayIndexOutOfBoundsException(i, i2, i3);
        }
    }

    public static void sort(int[] iArr) {
        DualPivotQuicksort.sort(iArr, 0, iArr.length - 1, (int[]) null, 0, 0);
    }

    public static void sort(int[] iArr, int i, int i2) {
        rangeCheck(iArr.length, i, i2);
        DualPivotQuicksort.sort(iArr, i, i2 - 1, (int[]) null, 0, 0);
    }

    public static void sort(long[] jArr) {
        DualPivotQuicksort.sort(jArr, 0, jArr.length - 1, (long[]) null, 0, 0);
    }

    public static void sort(long[] jArr, int i, int i2) {
        rangeCheck(jArr.length, i, i2);
        DualPivotQuicksort.sort(jArr, i, i2 - 1, (long[]) null, 0, 0);
    }

    public static void sort(short[] sArr) {
        DualPivotQuicksort.sort(sArr, 0, sArr.length - 1, (short[]) null, 0, 0);
    }

    public static void sort(short[] sArr, int i, int i2) {
        rangeCheck(sArr.length, i, i2);
        DualPivotQuicksort.sort(sArr, i, i2 - 1, (short[]) null, 0, 0);
    }

    public static void sort(char[] cArr) {
        DualPivotQuicksort.sort(cArr, 0, cArr.length - 1, (char[]) null, 0, 0);
    }

    public static void sort(char[] cArr, int i, int i2) {
        rangeCheck(cArr.length, i, i2);
        DualPivotQuicksort.sort(cArr, i, i2 - 1, (char[]) null, 0, 0);
    }

    public static void sort(byte[] bArr) {
        DualPivotQuicksort.sort(bArr, 0, bArr.length - 1);
    }

    public static void sort(byte[] bArr, int i, int i2) {
        rangeCheck(bArr.length, i, i2);
        DualPivotQuicksort.sort(bArr, i, i2 - 1);
    }

    public static void sort(float[] fArr) {
        DualPivotQuicksort.sort(fArr, 0, fArr.length - 1, (float[]) null, 0, 0);
    }

    public static void sort(float[] fArr, int i, int i2) {
        rangeCheck(fArr.length, i, i2);
        DualPivotQuicksort.sort(fArr, i, i2 - 1, (float[]) null, 0, 0);
    }

    public static void sort(double[] dArr) {
        DualPivotQuicksort.sort(dArr, 0, dArr.length - 1, (double[]) null, 0, 0);
    }

    public static void sort(double[] dArr, int i, int i2) {
        rangeCheck(dArr.length, i, i2);
        DualPivotQuicksort.sort(dArr, i, i2 - 1, (double[]) null, 0, 0);
    }

    public static void parallelSort(byte[] bArr) {
        int commonPoolParallelism;
        int length = bArr.length;
        if (length <= 8192 || (commonPoolParallelism = ForkJoinPool.getCommonPoolParallelism()) == 1) {
            DualPivotQuicksort.sort(bArr, 0, length - 1);
        } else {
            int i = length / (commonPoolParallelism << 2);
            new ArraysParallelSortHelpers.FJByte.Sorter(null, bArr, new byte[length], 0, length, 0, i <= 8192 ? 8192 : i).invoke();
        }
    }

    public static void parallelSort(byte[] bArr, int i, int i2) {
        int commonPoolParallelism;
        rangeCheck(bArr.length, i, i2);
        int i3 = i2 - i;
        if (i3 <= 8192 || (commonPoolParallelism = ForkJoinPool.getCommonPoolParallelism()) == 1) {
            DualPivotQuicksort.sort(bArr, i, i2 - 1);
        } else {
            int i4 = i3 / (commonPoolParallelism << 2);
            new ArraysParallelSortHelpers.FJByte.Sorter(null, bArr, new byte[i3], i, i3, 0, i4 <= 8192 ? 8192 : i4).invoke();
        }
    }

    public static void parallelSort(char[] cArr) {
        int commonPoolParallelism;
        int length = cArr.length;
        if (length <= 8192 || (commonPoolParallelism = ForkJoinPool.getCommonPoolParallelism()) == 1) {
            DualPivotQuicksort.sort(cArr, 0, length - 1, (char[]) null, 0, 0);
        } else {
            int i = length / (commonPoolParallelism << 2);
            new ArraysParallelSortHelpers.FJChar.Sorter(null, cArr, new char[length], 0, length, 0, i <= 8192 ? 8192 : i).invoke();
        }
    }

    public static void parallelSort(char[] cArr, int i, int i2) {
        int commonPoolParallelism;
        rangeCheck(cArr.length, i, i2);
        int i3 = i2 - i;
        if (i3 <= 8192 || (commonPoolParallelism = ForkJoinPool.getCommonPoolParallelism()) == 1) {
            DualPivotQuicksort.sort(cArr, i, i2 - 1, (char[]) null, 0, 0);
        } else {
            int i4 = i3 / (commonPoolParallelism << 2);
            new ArraysParallelSortHelpers.FJChar.Sorter(null, cArr, new char[i3], i, i3, 0, i4 <= 8192 ? 8192 : i4).invoke();
        }
    }

    public static void parallelSort(short[] sArr) {
        int commonPoolParallelism;
        int length = sArr.length;
        if (length <= 8192 || (commonPoolParallelism = ForkJoinPool.getCommonPoolParallelism()) == 1) {
            DualPivotQuicksort.sort(sArr, 0, length - 1, (short[]) null, 0, 0);
        } else {
            int i = length / (commonPoolParallelism << 2);
            new ArraysParallelSortHelpers.FJShort.Sorter(null, sArr, new short[length], 0, length, 0, i <= 8192 ? 8192 : i).invoke();
        }
    }

    public static void parallelSort(short[] sArr, int i, int i2) {
        int commonPoolParallelism;
        rangeCheck(sArr.length, i, i2);
        int i3 = i2 - i;
        if (i3 <= 8192 || (commonPoolParallelism = ForkJoinPool.getCommonPoolParallelism()) == 1) {
            DualPivotQuicksort.sort(sArr, i, i2 - 1, (short[]) null, 0, 0);
        } else {
            int i4 = i3 / (commonPoolParallelism << 2);
            new ArraysParallelSortHelpers.FJShort.Sorter(null, sArr, new short[i3], i, i3, 0, i4 <= 8192 ? 8192 : i4).invoke();
        }
    }

    public static void parallelSort(int[] iArr) {
        int commonPoolParallelism;
        int length = iArr.length;
        if (length <= 8192 || (commonPoolParallelism = ForkJoinPool.getCommonPoolParallelism()) == 1) {
            DualPivotQuicksort.sort(iArr, 0, length - 1, (int[]) null, 0, 0);
        } else {
            int i = length / (commonPoolParallelism << 2);
            new ArraysParallelSortHelpers.FJInt.Sorter(null, iArr, new int[length], 0, length, 0, i <= 8192 ? 8192 : i).invoke();
        }
    }

    public static void parallelSort(int[] iArr, int i, int i2) {
        int commonPoolParallelism;
        rangeCheck(iArr.length, i, i2);
        int i3 = i2 - i;
        if (i3 <= 8192 || (commonPoolParallelism = ForkJoinPool.getCommonPoolParallelism()) == 1) {
            DualPivotQuicksort.sort(iArr, i, i2 - 1, (int[]) null, 0, 0);
        } else {
            int i4 = i3 / (commonPoolParallelism << 2);
            new ArraysParallelSortHelpers.FJInt.Sorter(null, iArr, new int[i3], i, i3, 0, i4 <= 8192 ? 8192 : i4).invoke();
        }
    }

    public static void parallelSort(long[] jArr) {
        int commonPoolParallelism;
        int length = jArr.length;
        if (length <= 8192 || (commonPoolParallelism = ForkJoinPool.getCommonPoolParallelism()) == 1) {
            DualPivotQuicksort.sort(jArr, 0, length - 1, (long[]) null, 0, 0);
        } else {
            int i = length / (commonPoolParallelism << 2);
            new ArraysParallelSortHelpers.FJLong.Sorter(null, jArr, new long[length], 0, length, 0, i <= 8192 ? 8192 : i).invoke();
        }
    }

    public static void parallelSort(long[] jArr, int i, int i2) {
        int commonPoolParallelism;
        rangeCheck(jArr.length, i, i2);
        int i3 = i2 - i;
        if (i3 <= 8192 || (commonPoolParallelism = ForkJoinPool.getCommonPoolParallelism()) == 1) {
            DualPivotQuicksort.sort(jArr, i, i2 - 1, (long[]) null, 0, 0);
        } else {
            int i4 = i3 / (commonPoolParallelism << 2);
            new ArraysParallelSortHelpers.FJLong.Sorter(null, jArr, new long[i3], i, i3, 0, i4 <= 8192 ? 8192 : i4).invoke();
        }
    }

    public static void parallelSort(float[] fArr) {
        int commonPoolParallelism;
        int length = fArr.length;
        if (length <= 8192 || (commonPoolParallelism = ForkJoinPool.getCommonPoolParallelism()) == 1) {
            DualPivotQuicksort.sort(fArr, 0, length - 1, (float[]) null, 0, 0);
        } else {
            int i = length / (commonPoolParallelism << 2);
            new ArraysParallelSortHelpers.FJFloat.Sorter(null, fArr, new float[length], 0, length, 0, i <= 8192 ? 8192 : i).invoke();
        }
    }

    public static void parallelSort(float[] fArr, int i, int i2) {
        int commonPoolParallelism;
        rangeCheck(fArr.length, i, i2);
        int i3 = i2 - i;
        if (i3 <= 8192 || (commonPoolParallelism = ForkJoinPool.getCommonPoolParallelism()) == 1) {
            DualPivotQuicksort.sort(fArr, i, i2 - 1, (float[]) null, 0, 0);
        } else {
            int i4 = i3 / (commonPoolParallelism << 2);
            new ArraysParallelSortHelpers.FJFloat.Sorter(null, fArr, new float[i3], i, i3, 0, i4 <= 8192 ? 8192 : i4).invoke();
        }
    }

    public static void parallelSort(double[] dArr) {
        int commonPoolParallelism;
        int length = dArr.length;
        if (length <= 8192 || (commonPoolParallelism = ForkJoinPool.getCommonPoolParallelism()) == 1) {
            DualPivotQuicksort.sort(dArr, 0, length - 1, (double[]) null, 0, 0);
        } else {
            int i = length / (commonPoolParallelism << 2);
            new ArraysParallelSortHelpers.FJDouble.Sorter(null, dArr, new double[length], 0, length, 0, i <= 8192 ? 8192 : i).invoke();
        }
    }

    public static void parallelSort(double[] dArr, int i, int i2) {
        int commonPoolParallelism;
        rangeCheck(dArr.length, i, i2);
        int i3 = i2 - i;
        if (i3 <= 8192 || (commonPoolParallelism = ForkJoinPool.getCommonPoolParallelism()) == 1) {
            DualPivotQuicksort.sort(dArr, i, i2 - 1, (double[]) null, 0, 0);
        } else {
            int i4 = i3 / (commonPoolParallelism << 2);
            new ArraysParallelSortHelpers.FJDouble.Sorter(null, dArr, new double[i3], i, i3, 0, i4 <= 8192 ? 8192 : i4).invoke();
        }
    }

    public static <T extends Comparable<? super T>> void parallelSort(T[] tArr) {
        int commonPoolParallelism;
        int length = tArr.length;
        if (length <= 8192 || (commonPoolParallelism = ForkJoinPool.getCommonPoolParallelism()) == 1) {
            TimSort.sort(tArr, 0, length, NaturalOrder.INSTANCE, null, 0, 0);
        } else {
            int i = length / (commonPoolParallelism << 2);
            new ArraysParallelSortHelpers.FJObject.Sorter(null, tArr, (Comparable[]) Array.newInstance(tArr.getClass().getComponentType(), length), 0, length, 0, i <= 8192 ? 8192 : i, NaturalOrder.INSTANCE).invoke();
        }
    }

    public static <T extends Comparable<? super T>> void parallelSort(T[] tArr, int i, int i2) {
        int commonPoolParallelism;
        rangeCheck(tArr.length, i, i2);
        int i3 = i2 - i;
        if (i3 <= 8192 || (commonPoolParallelism = ForkJoinPool.getCommonPoolParallelism()) == 1) {
            TimSort.sort(tArr, i, i2, NaturalOrder.INSTANCE, null, 0, 0);
        } else {
            int i4 = i3 / (commonPoolParallelism << 2);
            new ArraysParallelSortHelpers.FJObject.Sorter(null, tArr, (Comparable[]) Array.newInstance(tArr.getClass().getComponentType(), i3), i, i3, 0, i4 <= 8192 ? 8192 : i4, NaturalOrder.INSTANCE).invoke();
        }
    }

    public static <T> void parallelSort(T[] tArr, Comparator<? super T> comparator) {
        int commonPoolParallelism;
        if (comparator == null) {
            comparator = NaturalOrder.INSTANCE;
        }
        int length = tArr.length;
        if (length <= 8192 || (commonPoolParallelism = ForkJoinPool.getCommonPoolParallelism()) == 1) {
            TimSort.sort(tArr, 0, length, comparator, null, 0, 0);
        } else {
            int i = length / (commonPoolParallelism << 2);
            new ArraysParallelSortHelpers.FJObject.Sorter(null, tArr, (Object[]) Array.newInstance(tArr.getClass().getComponentType(), length), 0, length, 0, i <= 8192 ? 8192 : i, comparator).invoke();
        }
    }

    public static <T> void parallelSort(T[] tArr, int i, int i2, Comparator<? super T> comparator) {
        int commonPoolParallelism;
        rangeCheck(tArr.length, i, i2);
        if (comparator == null) {
            comparator = NaturalOrder.INSTANCE;
        }
        int i3 = i2 - i;
        if (i3 <= 8192 || (commonPoolParallelism = ForkJoinPool.getCommonPoolParallelism()) == 1) {
            TimSort.sort(tArr, i, i2, comparator, null, 0, 0);
        } else {
            int i4 = i3 / (commonPoolParallelism << 2);
            new ArraysParallelSortHelpers.FJObject.Sorter(null, tArr, (Object[]) Array.newInstance(tArr.getClass().getComponentType(), i3), i, i3, 0, i4 <= 8192 ? 8192 : i4, comparator).invoke();
        }
    }

    public static void sort(Object[] objArr) {
        ComparableTimSort.sort(objArr, 0, objArr.length, null, 0, 0);
    }

    public static void sort(Object[] objArr, int i, int i2) {
        rangeCheck(objArr.length, i, i2);
        ComparableTimSort.sort(objArr, i, i2, null, 0, 0);
    }

    private static void mergeSort(Object[] objArr, Object[] objArr2, int i, int i2, int i3) {
        int i4 = i2 - i;
        if (i4 < 7) {
            for (int i5 = i; i5 < i2; i5++) {
                for (int i6 = i5; i6 > i; i6--) {
                    int i7 = i6 - 1;
                    if (((Comparable) objArr2[i7]).compareTo(objArr2[i6]) > 0) {
                        swap(objArr2, i6, i7);
                    }
                }
            }
            return;
        }
        int i8 = i + i3;
        int i9 = i2 + i3;
        int i10 = (i8 + i9) >>> 1;
        int i11 = -i3;
        mergeSort(objArr2, objArr, i8, i10, i11);
        mergeSort(objArr2, objArr, i10, i9, i11);
        if (((Comparable) objArr[i10 - 1]).compareTo(objArr[i10]) <= 0) {
            System.arraycopy(objArr, i8, objArr2, i, i4);
            return;
        }
        int i12 = i10;
        while (i < i2) {
            if (i12 >= i9 || (i8 < i10 && ((Comparable) objArr[i8]).compareTo(objArr[i12]) <= 0)) {
                objArr2[i] = objArr[i8];
                i8++;
            } else {
                objArr2[i] = objArr[i12];
                i12++;
            }
            i++;
        }
    }

    private static void swap(Object[] objArr, int i, int i2) {
        Object obj = objArr[i];
        objArr[i] = objArr[i2];
        objArr[i2] = obj;
    }

    public static <T> void sort(T[] tArr, Comparator<? super T> comparator) {
        if (comparator == null) {
            sort(tArr);
        } else {
            TimSort.sort(tArr, 0, tArr.length, comparator, null, 0, 0);
        }
    }

    public static <T> void sort(T[] tArr, int i, int i2, Comparator<? super T> comparator) {
        if (comparator == null) {
            sort(tArr, i, i2);
        } else {
            rangeCheck(tArr.length, i, i2);
            TimSort.sort(tArr, i, i2, comparator, null, 0, 0);
        }
    }

    public static <T> void parallelPrefix(T[] tArr, BinaryOperator<T> binaryOperator) {
        Objects.requireNonNull(binaryOperator);
        if (tArr.length > 0) {
            new ArrayPrefixHelpers.CumulateTask(null, binaryOperator, tArr, 0, tArr.length).invoke();
        }
    }

    public static <T> void parallelPrefix(T[] tArr, int i, int i2, BinaryOperator<T> binaryOperator) {
        Objects.requireNonNull(binaryOperator);
        rangeCheck(tArr.length, i, i2);
        if (i < i2) {
            new ArrayPrefixHelpers.CumulateTask(null, binaryOperator, tArr, i, i2).invoke();
        }
    }

    public static void parallelPrefix(long[] jArr, LongBinaryOperator longBinaryOperator) {
        Objects.requireNonNull(longBinaryOperator);
        if (jArr.length > 0) {
            new ArrayPrefixHelpers.LongCumulateTask(null, longBinaryOperator, jArr, 0, jArr.length).invoke();
        }
    }

    public static void parallelPrefix(long[] jArr, int i, int i2, LongBinaryOperator longBinaryOperator) {
        Objects.requireNonNull(longBinaryOperator);
        rangeCheck(jArr.length, i, i2);
        if (i < i2) {
            new ArrayPrefixHelpers.LongCumulateTask(null, longBinaryOperator, jArr, i, i2).invoke();
        }
    }

    public static void parallelPrefix(double[] dArr, DoubleBinaryOperator doubleBinaryOperator) {
        Objects.requireNonNull(doubleBinaryOperator);
        if (dArr.length > 0) {
            new ArrayPrefixHelpers.DoubleCumulateTask(null, doubleBinaryOperator, dArr, 0, dArr.length).invoke();
        }
    }

    public static void parallelPrefix(double[] dArr, int i, int i2, DoubleBinaryOperator doubleBinaryOperator) {
        Objects.requireNonNull(doubleBinaryOperator);
        rangeCheck(dArr.length, i, i2);
        if (i < i2) {
            new ArrayPrefixHelpers.DoubleCumulateTask(null, doubleBinaryOperator, dArr, i, i2).invoke();
        }
    }

    public static void parallelPrefix(int[] iArr, IntBinaryOperator intBinaryOperator) {
        Objects.requireNonNull(intBinaryOperator);
        if (iArr.length > 0) {
            new ArrayPrefixHelpers.IntCumulateTask(null, intBinaryOperator, iArr, 0, iArr.length).invoke();
        }
    }

    public static void parallelPrefix(int[] iArr, int i, int i2, IntBinaryOperator intBinaryOperator) {
        Objects.requireNonNull(intBinaryOperator);
        rangeCheck(iArr.length, i, i2);
        if (i < i2) {
            new ArrayPrefixHelpers.IntCumulateTask(null, intBinaryOperator, iArr, i, i2).invoke();
        }
    }

    public static int binarySearch(long[] jArr, long j) {
        return binarySearch0(jArr, 0, jArr.length, j);
    }

    public static int binarySearch(long[] jArr, int i, int i2, long j) {
        rangeCheck(jArr.length, i, i2);
        return binarySearch0(jArr, i, i2, j);
    }

    private static int binarySearch0(long[] jArr, int i, int i2, long j) {
        int i3 = i2 - 1;
        while (i <= i3) {
            int i4 = (i + i3) >>> 1;
            long j2 = jArr[i4];
            if (j2 < j) {
                i = i4 + 1;
            } else if (j2 > j) {
                i3 = i4 - 1;
            } else {
                return i4;
            }
        }
        return -(i + 1);
    }

    public static int binarySearch(int[] iArr, int i) {
        return binarySearch0(iArr, 0, iArr.length, i);
    }

    public static int binarySearch(int[] iArr, int i, int i2, int i3) {
        rangeCheck(iArr.length, i, i2);
        return binarySearch0(iArr, i, i2, i3);
    }

    private static int binarySearch0(int[] iArr, int i, int i2, int i3) {
        int i4 = i2 - 1;
        while (i <= i4) {
            int i5 = (i + i4) >>> 1;
            int i6 = iArr[i5];
            if (i6 < i3) {
                i = i5 + 1;
            } else if (i6 > i3) {
                i4 = i5 - 1;
            } else {
                return i5;
            }
        }
        return -(i + 1);
    }

    public static int binarySearch(short[] sArr, short s) {
        return binarySearch0(sArr, 0, sArr.length, s);
    }

    public static int binarySearch(short[] sArr, int i, int i2, short s) {
        rangeCheck(sArr.length, i, i2);
        return binarySearch0(sArr, i, i2, s);
    }

    private static int binarySearch0(short[] sArr, int i, int i2, short s) {
        int i3 = i2 - 1;
        while (i <= i3) {
            int i4 = (i + i3) >>> 1;
            short s2 = sArr[i4];
            if (s2 < s) {
                i = i4 + 1;
            } else if (s2 > s) {
                i3 = i4 - 1;
            } else {
                return i4;
            }
        }
        return -(i + 1);
    }

    public static int binarySearch(char[] cArr, char c) {
        return binarySearch0(cArr, 0, cArr.length, c);
    }

    public static int binarySearch(char[] cArr, int i, int i2, char c) {
        rangeCheck(cArr.length, i, i2);
        return binarySearch0(cArr, i, i2, c);
    }

    private static int binarySearch0(char[] cArr, int i, int i2, char c) {
        int i3 = i2 - 1;
        while (i <= i3) {
            int i4 = (i + i3) >>> 1;
            char c2 = cArr[i4];
            if (c2 < c) {
                i = i4 + 1;
            } else if (c2 > c) {
                i3 = i4 - 1;
            } else {
                return i4;
            }
        }
        return -(i + 1);
    }

    public static int binarySearch(byte[] bArr, byte b) {
        return binarySearch0(bArr, 0, bArr.length, b);
    }

    public static int binarySearch(byte[] bArr, int i, int i2, byte b) {
        rangeCheck(bArr.length, i, i2);
        return binarySearch0(bArr, i, i2, b);
    }

    private static int binarySearch0(byte[] bArr, int i, int i2, byte b) {
        int i3 = i2 - 1;
        while (i <= i3) {
            int i4 = (i + i3) >>> 1;
            byte b2 = bArr[i4];
            if (b2 < b) {
                i = i4 + 1;
            } else if (b2 > b) {
                i3 = i4 - 1;
            } else {
                return i4;
            }
        }
        return -(i + 1);
    }

    public static int binarySearch(double[] dArr, double d) {
        return binarySearch0(dArr, 0, dArr.length, d);
    }

    public static int binarySearch(double[] dArr, int i, int i2, double d) {
        rangeCheck(dArr.length, i, i2);
        return binarySearch0(dArr, i, i2, d);
    }

    private static int binarySearch0(double[] dArr, int i, int i2, double d) {
        int i3 = i2 - 1;
        while (i <= i3) {
            int i4 = (i + i3) >>> 1;
            double d2 = dArr[i4];
            if (d2 >= d) {
                if (d2 <= d) {
                    long jDoubleToLongBits = Double.doubleToLongBits(d2);
                    long jDoubleToLongBits2 = Double.doubleToLongBits(d);
                    if (jDoubleToLongBits == jDoubleToLongBits2) {
                        return i4;
                    }
                    if (jDoubleToLongBits < jDoubleToLongBits2) {
                    }
                }
                i3 = i4 - 1;
            }
            i = i4 + 1;
        }
        return -(i + 1);
    }

    public static int binarySearch(float[] fArr, float f) {
        return binarySearch0(fArr, 0, fArr.length, f);
    }

    public static int binarySearch(float[] fArr, int i, int i2, float f) {
        rangeCheck(fArr.length, i, i2);
        return binarySearch0(fArr, i, i2, f);
    }

    private static int binarySearch0(float[] fArr, int i, int i2, float f) {
        int i3 = i2 - 1;
        while (i <= i3) {
            int i4 = (i + i3) >>> 1;
            float f2 = fArr[i4];
            if (f2 >= f) {
                if (f2 <= f) {
                    int iFloatToIntBits = Float.floatToIntBits(f2);
                    int iFloatToIntBits2 = Float.floatToIntBits(f);
                    if (iFloatToIntBits == iFloatToIntBits2) {
                        return i4;
                    }
                    if (iFloatToIntBits < iFloatToIntBits2) {
                    }
                }
                i3 = i4 - 1;
            }
            i = i4 + 1;
        }
        return -(i + 1);
    }

    public static int binarySearch(Object[] objArr, Object obj) {
        return binarySearch0(objArr, 0, objArr.length, obj);
    }

    public static int binarySearch(Object[] objArr, int i, int i2, Object obj) {
        rangeCheck(objArr.length, i, i2);
        return binarySearch0(objArr, i, i2, obj);
    }

    private static int binarySearch0(Object[] objArr, int i, int i2, Object obj) {
        int i3 = i2 - 1;
        while (i <= i3) {
            int i4 = (i + i3) >>> 1;
            int iCompareTo = ((Comparable) objArr[i4]).compareTo(obj);
            if (iCompareTo < 0) {
                i = i4 + 1;
            } else if (iCompareTo > 0) {
                i3 = i4 - 1;
            } else {
                return i4;
            }
        }
        return -(i + 1);
    }

    public static <T> int binarySearch(T[] tArr, T t, Comparator<? super T> comparator) {
        return binarySearch0(tArr, 0, tArr.length, t, comparator);
    }

    public static <T> int binarySearch(T[] tArr, int i, int i2, T t, Comparator<? super T> comparator) {
        rangeCheck(tArr.length, i, i2);
        return binarySearch0(tArr, i, i2, t, comparator);
    }

    private static <T> int binarySearch0(T[] tArr, int i, int i2, T t, Comparator<? super T> comparator) {
        if (comparator == null) {
            return binarySearch0(tArr, i, i2, t);
        }
        int i3 = i2 - 1;
        while (i <= i3) {
            int i4 = (i + i3) >>> 1;
            int iCompare = comparator.compare(tArr[i4], t);
            if (iCompare < 0) {
                i = i4 + 1;
            } else if (iCompare > 0) {
                i3 = i4 - 1;
            } else {
                return i4;
            }
        }
        return -(i + 1);
    }

    public static boolean equals(long[] jArr, long[] jArr2) {
        int length;
        if (jArr == jArr2) {
            return true;
        }
        if (jArr == null || jArr2 == null || jArr2.length != (length = jArr.length)) {
            return false;
        }
        for (int i = 0; i < length; i++) {
            if (jArr[i] != jArr2[i]) {
                return false;
            }
        }
        return true;
    }

    public static boolean equals(int[] iArr, int[] iArr2) {
        int length;
        if (iArr == iArr2) {
            return true;
        }
        if (iArr == null || iArr2 == null || iArr2.length != (length = iArr.length)) {
            return false;
        }
        for (int i = 0; i < length; i++) {
            if (iArr[i] != iArr2[i]) {
                return false;
            }
        }
        return true;
    }

    public static boolean equals(short[] sArr, short[] sArr2) {
        int length;
        if (sArr == sArr2) {
            return true;
        }
        if (sArr == null || sArr2 == null || sArr2.length != (length = sArr.length)) {
            return false;
        }
        for (int i = 0; i < length; i++) {
            if (sArr[i] != sArr2[i]) {
                return false;
            }
        }
        return true;
    }

    public static boolean equals(char[] cArr, char[] cArr2) {
        int length;
        if (cArr == cArr2) {
            return true;
        }
        if (cArr == null || cArr2 == null || cArr2.length != (length = cArr.length)) {
            return false;
        }
        for (int i = 0; i < length; i++) {
            if (cArr[i] != cArr2[i]) {
                return false;
            }
        }
        return true;
    }

    public static boolean equals(byte[] bArr, byte[] bArr2) {
        int length;
        if (bArr == bArr2) {
            return true;
        }
        if (bArr == null || bArr2 == null || bArr2.length != (length = bArr.length)) {
            return false;
        }
        for (int i = 0; i < length; i++) {
            if (bArr[i] != bArr2[i]) {
                return false;
            }
        }
        return true;
    }

    public static boolean equals(boolean[] zArr, boolean[] zArr2) {
        int length;
        if (zArr == zArr2) {
            return true;
        }
        if (zArr == null || zArr2 == null || zArr2.length != (length = zArr.length)) {
            return false;
        }
        for (int i = 0; i < length; i++) {
            if (zArr[i] != zArr2[i]) {
                return false;
            }
        }
        return true;
    }

    public static boolean equals(double[] dArr, double[] dArr2) {
        int length;
        if (dArr == dArr2) {
            return true;
        }
        if (dArr == null || dArr2 == null || dArr2.length != (length = dArr.length)) {
            return false;
        }
        for (int i = 0; i < length; i++) {
            if (Double.doubleToLongBits(dArr[i]) != Double.doubleToLongBits(dArr2[i])) {
                return false;
            }
        }
        return true;
    }

    public static boolean equals(float[] fArr, float[] fArr2) {
        int length;
        if (fArr == fArr2) {
            return true;
        }
        if (fArr == null || fArr2 == null || fArr2.length != (length = fArr.length)) {
            return false;
        }
        for (int i = 0; i < length; i++) {
            if (Float.floatToIntBits(fArr[i]) != Float.floatToIntBits(fArr2[i])) {
                return false;
            }
        }
        return true;
    }

    public static boolean equals(Object[] objArr, Object[] objArr2) {
        int length;
        if (objArr == objArr2) {
            return true;
        }
        if (objArr == null || objArr2 == null || objArr2.length != (length = objArr.length)) {
            return false;
        }
        for (int i = 0; i < length; i++) {
            Object obj = objArr[i];
            Object obj2 = objArr2[i];
            if (obj == null) {
                if (obj2 != null) {
                    return false;
                }
            } else {
                if (!obj.equals(obj2)) {
                    return false;
                }
            }
        }
        return true;
    }

    public static void fill(long[] jArr, long j) {
        int length = jArr.length;
        for (int i = 0; i < length; i++) {
            jArr[i] = j;
        }
    }

    public static void fill(long[] jArr, int i, int i2, long j) {
        rangeCheck(jArr.length, i, i2);
        while (i < i2) {
            jArr[i] = j;
            i++;
        }
    }

    public static void fill(int[] iArr, int i) {
        int length = iArr.length;
        for (int i2 = 0; i2 < length; i2++) {
            iArr[i2] = i;
        }
    }

    public static void fill(int[] iArr, int i, int i2, int i3) {
        rangeCheck(iArr.length, i, i2);
        while (i < i2) {
            iArr[i] = i3;
            i++;
        }
    }

    public static void fill(short[] sArr, short s) {
        int length = sArr.length;
        for (int i = 0; i < length; i++) {
            sArr[i] = s;
        }
    }

    public static void fill(short[] sArr, int i, int i2, short s) {
        rangeCheck(sArr.length, i, i2);
        while (i < i2) {
            sArr[i] = s;
            i++;
        }
    }

    public static void fill(char[] cArr, char c) {
        int length = cArr.length;
        for (int i = 0; i < length; i++) {
            cArr[i] = c;
        }
    }

    public static void fill(char[] cArr, int i, int i2, char c) {
        rangeCheck(cArr.length, i, i2);
        while (i < i2) {
            cArr[i] = c;
            i++;
        }
    }

    public static void fill(byte[] bArr, byte b) {
        int length = bArr.length;
        for (int i = 0; i < length; i++) {
            bArr[i] = b;
        }
    }

    public static void fill(byte[] bArr, int i, int i2, byte b) {
        rangeCheck(bArr.length, i, i2);
        while (i < i2) {
            bArr[i] = b;
            i++;
        }
    }

    public static void fill(boolean[] zArr, boolean z) {
        int length = zArr.length;
        for (int i = 0; i < length; i++) {
            zArr[i] = z;
        }
    }

    public static void fill(boolean[] zArr, int i, int i2, boolean z) {
        rangeCheck(zArr.length, i, i2);
        while (i < i2) {
            zArr[i] = z;
            i++;
        }
    }

    public static void fill(double[] dArr, double d) {
        int length = dArr.length;
        for (int i = 0; i < length; i++) {
            dArr[i] = d;
        }
    }

    public static void fill(double[] dArr, int i, int i2, double d) {
        rangeCheck(dArr.length, i, i2);
        while (i < i2) {
            dArr[i] = d;
            i++;
        }
    }

    public static void fill(float[] fArr, float f) {
        int length = fArr.length;
        for (int i = 0; i < length; i++) {
            fArr[i] = f;
        }
    }

    public static void fill(float[] fArr, int i, int i2, float f) {
        rangeCheck(fArr.length, i, i2);
        while (i < i2) {
            fArr[i] = f;
            i++;
        }
    }

    public static void fill(Object[] objArr, Object obj) {
        int length = objArr.length;
        for (int i = 0; i < length; i++) {
            objArr[i] = obj;
        }
    }

    public static void fill(Object[] objArr, int i, int i2, Object obj) {
        rangeCheck(objArr.length, i, i2);
        while (i < i2) {
            objArr[i] = obj;
            i++;
        }
    }

    public static <T> T[] copyOf(T[] tArr, int i) {
        return (T[]) copyOf(tArr, i, tArr.getClass());
    }

    public static <T, U> T[] copyOf(U[] uArr, int i, Class<? extends T[]> cls) {
        T[] tArr;
        if (cls == Object[].class) {
            tArr = (T[]) new Object[i];
        } else {
            tArr = (T[]) ((Object[]) Array.newInstance(cls.getComponentType(), i));
        }
        System.arraycopy(uArr, 0, tArr, 0, Math.min(uArr.length, i));
        return tArr;
    }

    public static byte[] copyOf(byte[] bArr, int i) {
        byte[] bArr2 = new byte[i];
        System.arraycopy(bArr, 0, bArr2, 0, Math.min(bArr.length, i));
        return bArr2;
    }

    public static short[] copyOf(short[] sArr, int i) {
        short[] sArr2 = new short[i];
        System.arraycopy((Object) sArr, 0, (Object) sArr2, 0, Math.min(sArr.length, i));
        return sArr2;
    }

    public static int[] copyOf(int[] iArr, int i) {
        int[] iArr2 = new int[i];
        System.arraycopy((Object) iArr, 0, (Object) iArr2, 0, Math.min(iArr.length, i));
        return iArr2;
    }

    public static long[] copyOf(long[] jArr, int i) {
        long[] jArr2 = new long[i];
        System.arraycopy((Object) jArr, 0, (Object) jArr2, 0, Math.min(jArr.length, i));
        return jArr2;
    }

    public static char[] copyOf(char[] cArr, int i) {
        char[] cArr2 = new char[i];
        System.arraycopy((Object) cArr, 0, (Object) cArr2, 0, Math.min(cArr.length, i));
        return cArr2;
    }

    public static float[] copyOf(float[] fArr, int i) {
        float[] fArr2 = new float[i];
        System.arraycopy((Object) fArr, 0, (Object) fArr2, 0, Math.min(fArr.length, i));
        return fArr2;
    }

    public static double[] copyOf(double[] dArr, int i) {
        double[] dArr2 = new double[i];
        System.arraycopy((Object) dArr, 0, (Object) dArr2, 0, Math.min(dArr.length, i));
        return dArr2;
    }

    public static boolean[] copyOf(boolean[] zArr, int i) {
        boolean[] zArr2 = new boolean[i];
        System.arraycopy((Object) zArr, 0, (Object) zArr2, 0, Math.min(zArr.length, i));
        return zArr2;
    }

    public static <T> T[] copyOfRange(T[] tArr, int i, int i2) {
        return (T[]) copyOfRange(tArr, i, i2, tArr.getClass());
    }

    public static <T, U> T[] copyOfRange(U[] uArr, int i, int i2, Class<? extends T[]> cls) {
        T[] tArr;
        int i3 = i2 - i;
        if (i3 < 0) {
            throw new IllegalArgumentException(i + " > " + i2);
        }
        if (cls == Object[].class) {
            tArr = (T[]) new Object[i3];
        } else {
            tArr = (T[]) ((Object[]) Array.newInstance(cls.getComponentType(), i3));
        }
        System.arraycopy(uArr, i, tArr, 0, Math.min(uArr.length - i, i3));
        return tArr;
    }

    public static byte[] copyOfRange(byte[] bArr, int i, int i2) {
        int i3 = i2 - i;
        if (i3 < 0) {
            throw new IllegalArgumentException(i + " > " + i2);
        }
        byte[] bArr2 = new byte[i3];
        System.arraycopy(bArr, i, bArr2, 0, Math.min(bArr.length - i, i3));
        return bArr2;
    }

    public static short[] copyOfRange(short[] sArr, int i, int i2) {
        int i3 = i2 - i;
        if (i3 < 0) {
            throw new IllegalArgumentException(i + " > " + i2);
        }
        short[] sArr2 = new short[i3];
        System.arraycopy((Object) sArr, i, (Object) sArr2, 0, Math.min(sArr.length - i, i3));
        return sArr2;
    }

    public static int[] copyOfRange(int[] iArr, int i, int i2) {
        int i3 = i2 - i;
        if (i3 < 0) {
            throw new IllegalArgumentException(i + " > " + i2);
        }
        int[] iArr2 = new int[i3];
        System.arraycopy((Object) iArr, i, (Object) iArr2, 0, Math.min(iArr.length - i, i3));
        return iArr2;
    }

    public static long[] copyOfRange(long[] jArr, int i, int i2) {
        int i3 = i2 - i;
        if (i3 < 0) {
            throw new IllegalArgumentException(i + " > " + i2);
        }
        long[] jArr2 = new long[i3];
        System.arraycopy((Object) jArr, i, (Object) jArr2, 0, Math.min(jArr.length - i, i3));
        return jArr2;
    }

    public static char[] copyOfRange(char[] cArr, int i, int i2) {
        int i3 = i2 - i;
        if (i3 < 0) {
            throw new IllegalArgumentException(i + " > " + i2);
        }
        char[] cArr2 = new char[i3];
        System.arraycopy((Object) cArr, i, (Object) cArr2, 0, Math.min(cArr.length - i, i3));
        return cArr2;
    }

    public static float[] copyOfRange(float[] fArr, int i, int i2) {
        int i3 = i2 - i;
        if (i3 < 0) {
            throw new IllegalArgumentException(i + " > " + i2);
        }
        float[] fArr2 = new float[i3];
        System.arraycopy((Object) fArr, i, (Object) fArr2, 0, Math.min(fArr.length - i, i3));
        return fArr2;
    }

    public static double[] copyOfRange(double[] dArr, int i, int i2) {
        int i3 = i2 - i;
        if (i3 < 0) {
            throw new IllegalArgumentException(i + " > " + i2);
        }
        double[] dArr2 = new double[i3];
        System.arraycopy((Object) dArr, i, (Object) dArr2, 0, Math.min(dArr.length - i, i3));
        return dArr2;
    }

    public static boolean[] copyOfRange(boolean[] zArr, int i, int i2) {
        int i3 = i2 - i;
        if (i3 < 0) {
            throw new IllegalArgumentException(i + " > " + i2);
        }
        boolean[] zArr2 = new boolean[i3];
        System.arraycopy((Object) zArr, i, (Object) zArr2, 0, Math.min(zArr.length - i, i3));
        return zArr2;
    }

    @SafeVarargs
    public static <T> List<T> asList(T... tArr) {
        return new ArrayList(tArr);
    }

    private static class ArrayList<E> extends AbstractList<E> implements RandomAccess, Serializable {
        private static final long serialVersionUID = -2764017481108945198L;
        private final E[] a;

        ArrayList(E[] eArr) {
            this.a = (E[]) ((Object[]) Objects.requireNonNull(eArr));
        }

        @Override
        public int size() {
            return this.a.length;
        }

        @Override
        public Object[] toArray() {
            return (Object[]) this.a.clone();
        }

        @Override
        public <T> T[] toArray(T[] tArr) {
            int size = size();
            if (tArr.length < size) {
                return (T[]) Arrays.copyOf(this.a, size, tArr.getClass());
            }
            System.arraycopy(this.a, 0, tArr, 0, size);
            if (tArr.length > size) {
                tArr[size] = null;
            }
            return tArr;
        }

        @Override
        public E get(int i) {
            return this.a[i];
        }

        @Override
        public E set(int i, E e) {
            E e2 = this.a[i];
            this.a[i] = e;
            return e2;
        }

        @Override
        public int indexOf(Object obj) {
            E[] eArr = this.a;
            int i = 0;
            if (obj == null) {
                while (i < eArr.length) {
                    if (eArr[i] != null) {
                        i++;
                    } else {
                        return i;
                    }
                }
                return -1;
            }
            while (i < eArr.length) {
                if (!obj.equals(eArr[i])) {
                    i++;
                } else {
                    return i;
                }
            }
            return -1;
        }

        @Override
        public boolean contains(Object obj) {
            return indexOf(obj) != -1;
        }

        @Override
        public Spliterator<E> spliterator() {
            return Spliterators.spliterator(this.a, 16);
        }

        @Override
        public void forEach(Consumer<? super E> consumer) {
            Objects.requireNonNull(consumer);
            for (R.color colorVar : this.a) {
                consumer.accept(colorVar);
            }
        }

        @Override
        public void replaceAll(UnaryOperator<E> unaryOperator) {
            Objects.requireNonNull(unaryOperator);
            E[] eArr = this.a;
            for (int i = 0; i < eArr.length; i++) {
                eArr[i] = unaryOperator.apply(eArr[i]);
            }
        }

        @Override
        public void sort(Comparator<? super E> comparator) {
            Arrays.sort(this.a, comparator);
        }
    }

    public static int hashCode(long[] jArr) {
        if (jArr == null) {
            return 0;
        }
        int i = 1;
        for (long j : jArr) {
            i = (31 * i) + ((int) (j ^ (j >>> 32)));
        }
        return i;
    }

    public static int hashCode(int[] iArr) {
        if (iArr == null) {
            return 0;
        }
        int i = 1;
        for (int i2 : iArr) {
            i = (31 * i) + i2;
        }
        return i;
    }

    public static int hashCode(short[] sArr) {
        if (sArr == null) {
            return 0;
        }
        int i = 1;
        for (short s : sArr) {
            i = (31 * i) + s;
        }
        return i;
    }

    public static int hashCode(char[] cArr) {
        if (cArr == null) {
            return 0;
        }
        int i = 1;
        for (char c : cArr) {
            i = (31 * i) + c;
        }
        return i;
    }

    public static int hashCode(byte[] bArr) {
        if (bArr == null) {
            return 0;
        }
        int i = 1;
        for (byte b : bArr) {
            i = (31 * i) + b;
        }
        return i;
    }

    public static int hashCode(boolean[] zArr) {
        if (zArr == null) {
            return 0;
        }
        int i = 1;
        for (boolean z : zArr) {
            i = (z ? 1231 : 1237) + (31 * i);
        }
        return i;
    }

    public static int hashCode(float[] fArr) {
        if (fArr == null) {
            return 0;
        }
        int iFloatToIntBits = 1;
        for (float f : fArr) {
            iFloatToIntBits = Float.floatToIntBits(f) + (31 * iFloatToIntBits);
        }
        return iFloatToIntBits;
    }

    public static int hashCode(double[] dArr) {
        if (dArr == null) {
            return 0;
        }
        int i = 1;
        for (double d : dArr) {
            long jDoubleToLongBits = Double.doubleToLongBits(d);
            i = ((int) (jDoubleToLongBits ^ (jDoubleToLongBits >>> 32))) + (31 * i);
        }
        return i;
    }

    public static int hashCode(Object[] objArr) {
        if (objArr == null) {
            return 0;
        }
        int length = objArr.length;
        int iHashCode = 1;
        for (int i = 0; i < length; i++) {
            Object obj = objArr[i];
            iHashCode = (obj == null ? 0 : obj.hashCode()) + (31 * iHashCode);
        }
        return iHashCode;
    }

    public static int deepHashCode(Object[] objArr) {
        int iHashCode;
        if (objArr == null) {
            return 0;
        }
        int i = 1;
        for (Object obj : objArr) {
            if (obj != null) {
                Class<?> componentType = obj.getClass().getComponentType();
                if (componentType == null) {
                    iHashCode = obj.hashCode();
                } else if (obj instanceof Object[]) {
                    iHashCode = deepHashCode((Object[]) obj);
                } else if (componentType == Byte.TYPE) {
                    iHashCode = hashCode((byte[]) obj);
                } else if (componentType == Short.TYPE) {
                    iHashCode = hashCode((short[]) obj);
                } else if (componentType == Integer.TYPE) {
                    iHashCode = hashCode((int[]) obj);
                } else if (componentType == Long.TYPE) {
                    iHashCode = hashCode((long[]) obj);
                } else if (componentType == Character.TYPE) {
                    iHashCode = hashCode((char[]) obj);
                } else if (componentType == Float.TYPE) {
                    iHashCode = hashCode((float[]) obj);
                } else if (componentType == Double.TYPE) {
                    iHashCode = hashCode((double[]) obj);
                } else if (componentType == Boolean.TYPE) {
                    iHashCode = hashCode((boolean[]) obj);
                } else {
                    iHashCode = obj.hashCode();
                }
            } else {
                iHashCode = 0;
            }
            i = (31 * i) + iHashCode;
        }
        return i;
    }

    public static boolean deepEquals(Object[] objArr, Object[] objArr2) {
        int length;
        if (objArr == objArr2) {
            return true;
        }
        if (objArr == null || objArr2 == null || objArr2.length != (length = objArr.length)) {
            return false;
        }
        for (int i = 0; i < length; i++) {
            Object obj = objArr[i];
            Object obj2 = objArr2[i];
            if (obj != obj2 && (obj == null || obj2 == null || !deepEquals0(obj, obj2))) {
                return false;
            }
        }
        return true;
    }

    static boolean deepEquals0(Object obj, Object obj2) {
        Class<?> componentType = obj.getClass().getComponentType();
        if (componentType != obj2.getClass().getComponentType()) {
            return false;
        }
        if (obj instanceof Object[]) {
            return deepEquals((Object[]) obj, (Object[]) obj2);
        }
        if (componentType == Byte.TYPE) {
            return equals((byte[]) obj, (byte[]) obj2);
        }
        if (componentType == Short.TYPE) {
            return equals((short[]) obj, (short[]) obj2);
        }
        if (componentType == Integer.TYPE) {
            return equals((int[]) obj, (int[]) obj2);
        }
        if (componentType == Long.TYPE) {
            return equals((long[]) obj, (long[]) obj2);
        }
        if (componentType == Character.TYPE) {
            return equals((char[]) obj, (char[]) obj2);
        }
        if (componentType == Float.TYPE) {
            return equals((float[]) obj, (float[]) obj2);
        }
        if (componentType == Double.TYPE) {
            return equals((double[]) obj, (double[]) obj2);
        }
        if (componentType == Boolean.TYPE) {
            return equals((boolean[]) obj, (boolean[]) obj2);
        }
        return obj.equals(obj2);
    }

    public static String toString(long[] jArr) {
        if (jArr == null) {
            return "null";
        }
        int length = jArr.length - 1;
        if (length == -1) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        int i = 0;
        while (true) {
            sb.append(jArr[i]);
            if (i == length) {
                sb.append(']');
                return sb.toString();
            }
            sb.append(", ");
            i++;
        }
    }

    public static String toString(int[] iArr) {
        if (iArr == null) {
            return "null";
        }
        int length = iArr.length - 1;
        if (length == -1) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        int i = 0;
        while (true) {
            sb.append(iArr[i]);
            if (i == length) {
                sb.append(']');
                return sb.toString();
            }
            sb.append(", ");
            i++;
        }
    }

    public static String toString(short[] sArr) {
        if (sArr == null) {
            return "null";
        }
        int length = sArr.length - 1;
        if (length == -1) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        int i = 0;
        while (true) {
            sb.append((int) sArr[i]);
            if (i == length) {
                sb.append(']');
                return sb.toString();
            }
            sb.append(", ");
            i++;
        }
    }

    public static String toString(char[] cArr) {
        if (cArr == null) {
            return "null";
        }
        int length = cArr.length - 1;
        if (length == -1) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        int i = 0;
        while (true) {
            sb.append(cArr[i]);
            if (i == length) {
                sb.append(']');
                return sb.toString();
            }
            sb.append(", ");
            i++;
        }
    }

    public static String toString(byte[] bArr) {
        if (bArr == null) {
            return "null";
        }
        int length = bArr.length - 1;
        if (length == -1) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        int i = 0;
        while (true) {
            sb.append((int) bArr[i]);
            if (i == length) {
                sb.append(']');
                return sb.toString();
            }
            sb.append(", ");
            i++;
        }
    }

    public static String toString(boolean[] zArr) {
        if (zArr == null) {
            return "null";
        }
        int length = zArr.length - 1;
        if (length == -1) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        int i = 0;
        while (true) {
            sb.append(zArr[i]);
            if (i == length) {
                sb.append(']');
                return sb.toString();
            }
            sb.append(", ");
            i++;
        }
    }

    public static String toString(float[] fArr) {
        if (fArr == null) {
            return "null";
        }
        int length = fArr.length - 1;
        if (length == -1) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        int i = 0;
        while (true) {
            sb.append(fArr[i]);
            if (i == length) {
                sb.append(']');
                return sb.toString();
            }
            sb.append(", ");
            i++;
        }
    }

    public static String toString(double[] dArr) {
        if (dArr == null) {
            return "null";
        }
        int length = dArr.length - 1;
        if (length == -1) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        int i = 0;
        while (true) {
            sb.append(dArr[i]);
            if (i == length) {
                sb.append(']');
                return sb.toString();
            }
            sb.append(", ");
            i++;
        }
    }

    public static String toString(Object[] objArr) {
        if (objArr == null) {
            return "null";
        }
        int length = objArr.length - 1;
        if (length == -1) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        int i = 0;
        while (true) {
            sb.append(String.valueOf(objArr[i]));
            if (i == length) {
                sb.append(']');
                return sb.toString();
            }
            sb.append(", ");
            i++;
        }
    }

    public static String deepToString(Object[] objArr) {
        if (objArr == null) {
            return "null";
        }
        int length = 20 * objArr.length;
        if (objArr.length != 0 && length <= 0) {
            length = Integer.MAX_VALUE;
        }
        StringBuilder sb = new StringBuilder(length);
        deepToString(objArr, sb, new HashSet());
        return sb.toString();
    }

    private static void deepToString(Object[] objArr, StringBuilder sb, Set<Object[]> set) {
        if (objArr == null) {
            sb.append("null");
            return;
        }
        int length = objArr.length - 1;
        if (length == -1) {
            sb.append("[]");
            return;
        }
        set.add(objArr);
        sb.append('[');
        int i = 0;
        while (true) {
            Object obj = objArr[i];
            if (obj == null) {
                sb.append("null");
            } else {
                Class<?> cls = obj.getClass();
                if (cls.isArray()) {
                    if (cls == byte[].class) {
                        sb.append(toString((byte[]) obj));
                    } else if (cls == short[].class) {
                        sb.append(toString((short[]) obj));
                    } else if (cls == int[].class) {
                        sb.append(toString((int[]) obj));
                    } else if (cls == long[].class) {
                        sb.append(toString((long[]) obj));
                    } else if (cls == char[].class) {
                        sb.append(toString((char[]) obj));
                    } else if (cls == float[].class) {
                        sb.append(toString((float[]) obj));
                    } else if (cls == double[].class) {
                        sb.append(toString((double[]) obj));
                    } else if (cls == boolean[].class) {
                        sb.append(toString((boolean[]) obj));
                    } else if (set.contains(obj)) {
                        sb.append("[...]");
                    } else {
                        deepToString((Object[]) obj, sb, set);
                    }
                } else {
                    sb.append(obj.toString());
                }
            }
            if (i != length) {
                sb.append(", ");
                i++;
            } else {
                sb.append(']');
                set.remove(objArr);
                return;
            }
        }
    }

    public static <T> void setAll(T[] tArr, IntFunction<? extends T> intFunction) {
        Objects.requireNonNull(intFunction);
        for (int i = 0; i < tArr.length; i++) {
            tArr[i] = intFunction.apply(i);
        }
    }

    public static <T> void parallelSetAll(final T[] tArr, final IntFunction<? extends T> intFunction) {
        Objects.requireNonNull(intFunction);
        IntStream.range(0, tArr.length).parallel().forEach(new IntConsumer() {
            @Override
            public final void accept(int i) {
                Arrays.lambda$parallelSetAll$0(tArr, intFunction, i);
            }
        });
    }

    static void lambda$parallelSetAll$0(Object[] objArr, IntFunction intFunction, int i) {
        objArr[i] = intFunction.apply(i);
    }

    public static void setAll(int[] iArr, IntUnaryOperator intUnaryOperator) {
        Objects.requireNonNull(intUnaryOperator);
        for (int i = 0; i < iArr.length; i++) {
            iArr[i] = intUnaryOperator.applyAsInt(i);
        }
    }

    public static void parallelSetAll(final int[] iArr, final IntUnaryOperator intUnaryOperator) {
        Objects.requireNonNull(intUnaryOperator);
        IntStream.range(0, iArr.length).parallel().forEach(new IntConsumer() {
            @Override
            public final void accept(int i) {
                Arrays.lambda$parallelSetAll$1(iArr, intUnaryOperator, i);
            }
        });
    }

    static void lambda$parallelSetAll$1(int[] iArr, IntUnaryOperator intUnaryOperator, int i) {
        iArr[i] = intUnaryOperator.applyAsInt(i);
    }

    public static void setAll(long[] jArr, IntToLongFunction intToLongFunction) {
        Objects.requireNonNull(intToLongFunction);
        for (int i = 0; i < jArr.length; i++) {
            jArr[i] = intToLongFunction.applyAsLong(i);
        }
    }

    public static void parallelSetAll(final long[] jArr, final IntToLongFunction intToLongFunction) {
        Objects.requireNonNull(intToLongFunction);
        IntStream.range(0, jArr.length).parallel().forEach(new IntConsumer() {
            @Override
            public final void accept(int i) {
                Arrays.lambda$parallelSetAll$2(jArr, intToLongFunction, i);
            }
        });
    }

    static void lambda$parallelSetAll$2(long[] jArr, IntToLongFunction intToLongFunction, int i) {
        jArr[i] = intToLongFunction.applyAsLong(i);
    }

    public static void setAll(double[] dArr, IntToDoubleFunction intToDoubleFunction) {
        Objects.requireNonNull(intToDoubleFunction);
        for (int i = 0; i < dArr.length; i++) {
            dArr[i] = intToDoubleFunction.applyAsDouble(i);
        }
    }

    public static void parallelSetAll(final double[] dArr, final IntToDoubleFunction intToDoubleFunction) {
        Objects.requireNonNull(intToDoubleFunction);
        IntStream.range(0, dArr.length).parallel().forEach(new IntConsumer() {
            @Override
            public final void accept(int i) {
                Arrays.lambda$parallelSetAll$3(dArr, intToDoubleFunction, i);
            }
        });
    }

    static void lambda$parallelSetAll$3(double[] dArr, IntToDoubleFunction intToDoubleFunction, int i) {
        dArr[i] = intToDoubleFunction.applyAsDouble(i);
    }

    public static <T> Spliterator<T> spliterator(T[] tArr) {
        return Spliterators.spliterator(tArr, 1040);
    }

    public static <T> Spliterator<T> spliterator(T[] tArr, int i, int i2) {
        return Spliterators.spliterator(tArr, i, i2, 1040);
    }

    public static Spliterator.OfInt spliterator(int[] iArr) {
        return Spliterators.spliterator(iArr, 1040);
    }

    public static Spliterator.OfInt spliterator(int[] iArr, int i, int i2) {
        return Spliterators.spliterator(iArr, i, i2, 1040);
    }

    public static Spliterator.OfLong spliterator(long[] jArr) {
        return Spliterators.spliterator(jArr, 1040);
    }

    public static Spliterator.OfLong spliterator(long[] jArr, int i, int i2) {
        return Spliterators.spliterator(jArr, i, i2, 1040);
    }

    public static Spliterator.OfDouble spliterator(double[] dArr) {
        return Spliterators.spliterator(dArr, 1040);
    }

    public static Spliterator.OfDouble spliterator(double[] dArr, int i, int i2) {
        return Spliterators.spliterator(dArr, i, i2, 1040);
    }

    public static <T> Stream<T> stream(T[] tArr) {
        return stream(tArr, 0, tArr.length);
    }

    public static <T> Stream<T> stream(T[] tArr, int i, int i2) {
        return StreamSupport.stream(spliterator(tArr, i, i2), false);
    }

    public static IntStream stream(int[] iArr) {
        return stream(iArr, 0, iArr.length);
    }

    public static IntStream stream(int[] iArr, int i, int i2) {
        return StreamSupport.intStream(spliterator(iArr, i, i2), false);
    }

    public static LongStream stream(long[] jArr) {
        return stream(jArr, 0, jArr.length);
    }

    public static LongStream stream(long[] jArr, int i, int i2) {
        return StreamSupport.longStream(spliterator(jArr, i, i2), false);
    }

    public static DoubleStream stream(double[] dArr) {
        return stream(dArr, 0, dArr.length);
    }

    public static DoubleStream stream(double[] dArr, int i, int i2) {
        return StreamSupport.doubleStream(spliterator(dArr, i, i2), false);
    }
}
