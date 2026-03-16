package java.util;

import java.io.Serializable;
import java.lang.reflect.Array;

public class Arrays {

    private static class ArrayList<E> extends AbstractList<E> implements List<E>, Serializable, RandomAccess {
        private static final long serialVersionUID = -2764017481108945198L;
        private final E[] a;

        ArrayList(E[] storage) {
            if (storage == null) {
                throw new NullPointerException("storage == null");
            }
            this.a = storage;
        }

        @Override
        public boolean contains(Object object) {
            if (object != null) {
                E[] arr$ = this.a;
                for (E element : arr$) {
                    if (object.equals(element)) {
                        return true;
                    }
                }
            } else {
                E[] arr$2 = this.a;
                for (E element2 : arr$2) {
                    if (element2 == null) {
                        return true;
                    }
                }
            }
            return false;
        }

        @Override
        public E get(int location) {
            try {
                return this.a[location];
            } catch (ArrayIndexOutOfBoundsException e) {
                throw java.util.ArrayList.throwIndexOutOfBoundsException(location, this.a.length);
            }
        }

        @Override
        public int indexOf(Object object) {
            if (object != null) {
                for (int i = 0; i < this.a.length; i++) {
                    if (object.equals(this.a[i])) {
                        return i;
                    }
                }
            } else {
                for (int i2 = 0; i2 < this.a.length; i2++) {
                    if (this.a[i2] == null) {
                        return i2;
                    }
                }
            }
            return -1;
        }

        @Override
        public int lastIndexOf(Object object) {
            if (object != null) {
                for (int i = this.a.length - 1; i >= 0; i--) {
                    if (object.equals(this.a[i])) {
                        return i;
                    }
                }
            } else {
                for (int i2 = this.a.length - 1; i2 >= 0; i2--) {
                    if (this.a[i2] == null) {
                        return i2;
                    }
                }
            }
            return -1;
        }

        @Override
        public E set(int location, E object) {
            E result = this.a[location];
            this.a[location] = object;
            return result;
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
            if (size > tArr.length) {
                tArr = (T[]) ((Object[]) Array.newInstance(tArr.getClass().getComponentType(), size));
            }
            System.arraycopy(this.a, 0, tArr, 0, size);
            if (size < tArr.length) {
                tArr[size] = null;
            }
            return tArr;
        }
    }

    private Arrays() {
    }

    @SafeVarargs
    public static <T> List<T> asList(T... array) {
        return new ArrayList(array);
    }

    public static int binarySearch(byte[] array, byte value) {
        return binarySearch(array, 0, array.length, value);
    }

    public static int binarySearch(byte[] array, int startIndex, int endIndex, byte value) {
        checkBinarySearchBounds(startIndex, endIndex, array.length);
        int lo = startIndex;
        int hi = endIndex - 1;
        while (lo <= hi) {
            int mid = (lo + hi) >>> 1;
            byte midVal = array[mid];
            if (midVal < value) {
                lo = mid + 1;
            } else {
                if (midVal <= value) {
                    return mid;
                }
                hi = mid - 1;
            }
        }
        return lo ^ (-1);
    }

    public static int binarySearch(char[] array, char value) {
        return binarySearch(array, 0, array.length, value);
    }

    public static int binarySearch(char[] array, int startIndex, int endIndex, char value) {
        checkBinarySearchBounds(startIndex, endIndex, array.length);
        int lo = startIndex;
        int hi = endIndex - 1;
        while (lo <= hi) {
            int mid = (lo + hi) >>> 1;
            char midVal = array[mid];
            if (midVal < value) {
                lo = mid + 1;
            } else {
                if (midVal <= value) {
                    return mid;
                }
                hi = mid - 1;
            }
        }
        return lo ^ (-1);
    }

    public static int binarySearch(double[] array, double value) {
        return binarySearch(array, 0, array.length, value);
    }

    public static int binarySearch(double[] array, int startIndex, int endIndex, double value) {
        checkBinarySearchBounds(startIndex, endIndex, array.length);
        int lo = startIndex;
        int hi = endIndex - 1;
        while (lo <= hi) {
            int mid = (lo + hi) >>> 1;
            double midVal = array[mid];
            if (midVal < value) {
                lo = mid + 1;
            } else if (midVal > value) {
                hi = mid - 1;
            } else if (midVal == 0.0d || midVal != value) {
                long midValBits = Double.doubleToLongBits(midVal);
                long valueBits = Double.doubleToLongBits(value);
                if (midValBits < valueBits) {
                    lo = mid + 1;
                } else if (midValBits > valueBits) {
                    hi = mid - 1;
                } else {
                    return mid;
                }
            } else {
                return mid;
            }
        }
        return lo ^ (-1);
    }

    public static int binarySearch(float[] array, float value) {
        return binarySearch(array, 0, array.length, value);
    }

    public static int binarySearch(float[] array, int startIndex, int endIndex, float value) {
        checkBinarySearchBounds(startIndex, endIndex, array.length);
        int lo = startIndex;
        int hi = endIndex - 1;
        while (lo <= hi) {
            int mid = (lo + hi) >>> 1;
            float midVal = array[mid];
            if (midVal < value) {
                lo = mid + 1;
            } else if (midVal > value) {
                hi = mid - 1;
            } else if (midVal == 0.0f || midVal != value) {
                int midValBits = Float.floatToIntBits(midVal);
                int valueBits = Float.floatToIntBits(value);
                if (midValBits < valueBits) {
                    lo = mid + 1;
                } else if (midValBits > valueBits) {
                    hi = mid - 1;
                } else {
                    return mid;
                }
            } else {
                return mid;
            }
        }
        return lo ^ (-1);
    }

    public static int binarySearch(int[] array, int value) {
        return binarySearch(array, 0, array.length, value);
    }

    public static int binarySearch(int[] array, int startIndex, int endIndex, int value) {
        checkBinarySearchBounds(startIndex, endIndex, array.length);
        int lo = startIndex;
        int hi = endIndex - 1;
        while (lo <= hi) {
            int mid = (lo + hi) >>> 1;
            int midVal = array[mid];
            if (midVal < value) {
                lo = mid + 1;
            } else {
                if (midVal <= value) {
                    return mid;
                }
                hi = mid - 1;
            }
        }
        return lo ^ (-1);
    }

    public static int binarySearch(long[] array, long value) {
        return binarySearch(array, 0, array.length, value);
    }

    public static int binarySearch(long[] array, int startIndex, int endIndex, long value) {
        checkBinarySearchBounds(startIndex, endIndex, array.length);
        int lo = startIndex;
        int hi = endIndex - 1;
        while (lo <= hi) {
            int mid = (lo + hi) >>> 1;
            long midVal = array[mid];
            if (midVal < value) {
                lo = mid + 1;
            } else {
                if (midVal <= value) {
                    return mid;
                }
                hi = mid - 1;
            }
        }
        return lo ^ (-1);
    }

    public static int binarySearch(Object[] array, Object value) {
        return binarySearch(array, 0, array.length, value);
    }

    public static int binarySearch(Object[] array, int startIndex, int endIndex, Object value) {
        checkBinarySearchBounds(startIndex, endIndex, array.length);
        int lo = startIndex;
        int hi = endIndex - 1;
        while (lo <= hi) {
            int mid = (lo + hi) >>> 1;
            int midValCmp = ((Comparable) array[mid]).compareTo(value);
            if (midValCmp < 0) {
                lo = mid + 1;
            } else {
                if (midValCmp <= 0) {
                    return mid;
                }
                hi = mid - 1;
            }
        }
        return lo ^ (-1);
    }

    public static <T> int binarySearch(T[] array, T value, Comparator<? super T> comparator) {
        return binarySearch(array, 0, array.length, value, comparator);
    }

    public static <T> int binarySearch(T[] array, int startIndex, int endIndex, T value, Comparator<? super T> comparator) {
        if (comparator == null) {
            return binarySearch(array, startIndex, endIndex, value);
        }
        checkBinarySearchBounds(startIndex, endIndex, array.length);
        int lo = startIndex;
        int hi = endIndex - 1;
        while (lo <= hi) {
            int mid = (lo + hi) >>> 1;
            int midValCmp = comparator.compare(array[mid], value);
            if (midValCmp < 0) {
                lo = mid + 1;
            } else if (midValCmp > 0) {
                hi = mid - 1;
            } else {
                return mid;
            }
        }
        return lo ^ (-1);
    }

    public static int binarySearch(short[] array, short value) {
        return binarySearch(array, 0, array.length, value);
    }

    public static int binarySearch(short[] array, int startIndex, int endIndex, short value) {
        checkBinarySearchBounds(startIndex, endIndex, array.length);
        int lo = startIndex;
        int hi = endIndex - 1;
        while (lo <= hi) {
            int mid = (lo + hi) >>> 1;
            short midVal = array[mid];
            if (midVal < value) {
                lo = mid + 1;
            } else {
                if (midVal <= value) {
                    return mid;
                }
                hi = mid - 1;
            }
        }
        return lo ^ (-1);
    }

    private static void checkBinarySearchBounds(int startIndex, int endIndex, int length) {
        if (startIndex > endIndex) {
            throw new IllegalArgumentException();
        }
        if (startIndex < 0 || endIndex > length) {
            throw new ArrayIndexOutOfBoundsException();
        }
    }

    public static void fill(byte[] array, byte value) {
        for (int i = 0; i < array.length; i++) {
            array[i] = value;
        }
    }

    public static void fill(byte[] array, int start, int end, byte value) {
        checkStartAndEnd(array.length, start, end);
        for (int i = start; i < end; i++) {
            array[i] = value;
        }
    }

    public static void fill(short[] array, short value) {
        for (int i = 0; i < array.length; i++) {
            array[i] = value;
        }
    }

    public static void fill(short[] array, int start, int end, short value) {
        checkStartAndEnd(array.length, start, end);
        for (int i = start; i < end; i++) {
            array[i] = value;
        }
    }

    public static void fill(char[] array, char value) {
        for (int i = 0; i < array.length; i++) {
            array[i] = value;
        }
    }

    public static void fill(char[] array, int start, int end, char value) {
        checkStartAndEnd(array.length, start, end);
        for (int i = start; i < end; i++) {
            array[i] = value;
        }
    }

    public static void fill(int[] array, int value) {
        for (int i = 0; i < array.length; i++) {
            array[i] = value;
        }
    }

    public static void fill(int[] array, int start, int end, int value) {
        checkStartAndEnd(array.length, start, end);
        for (int i = start; i < end; i++) {
            array[i] = value;
        }
    }

    public static void fill(long[] array, long value) {
        for (int i = 0; i < array.length; i++) {
            array[i] = value;
        }
    }

    public static void fill(long[] array, int start, int end, long value) {
        checkStartAndEnd(array.length, start, end);
        for (int i = start; i < end; i++) {
            array[i] = value;
        }
    }

    public static void fill(float[] array, float value) {
        for (int i = 0; i < array.length; i++) {
            array[i] = value;
        }
    }

    public static void fill(float[] array, int start, int end, float value) {
        checkStartAndEnd(array.length, start, end);
        for (int i = start; i < end; i++) {
            array[i] = value;
        }
    }

    public static void fill(double[] array, double value) {
        for (int i = 0; i < array.length; i++) {
            array[i] = value;
        }
    }

    public static void fill(double[] array, int start, int end, double value) {
        checkStartAndEnd(array.length, start, end);
        for (int i = start; i < end; i++) {
            array[i] = value;
        }
    }

    public static void fill(boolean[] array, boolean value) {
        for (int i = 0; i < array.length; i++) {
            array[i] = value;
        }
    }

    public static void fill(boolean[] array, int start, int end, boolean value) {
        checkStartAndEnd(array.length, start, end);
        for (int i = start; i < end; i++) {
            array[i] = value;
        }
    }

    public static void fill(Object[] array, Object value) {
        for (int i = 0; i < array.length; i++) {
            array[i] = value;
        }
    }

    public static void fill(Object[] array, int start, int end, Object value) {
        checkStartAndEnd(array.length, start, end);
        for (int i = start; i < end; i++) {
            array[i] = value;
        }
    }

    public static int hashCode(boolean[] array) {
        if (array == null) {
            return 0;
        }
        int hashCode = 1;
        int len$ = array.length;
        for (int i$ = 0; i$ < len$; i$++) {
            boolean element = array[i$];
            hashCode = (hashCode * 31) + (element ? 1231 : 1237);
        }
        return hashCode;
    }

    public static int hashCode(int[] array) {
        if (array == null) {
            return 0;
        }
        int hashCode = 1;
        for (int element : array) {
            hashCode = (hashCode * 31) + element;
        }
        return hashCode;
    }

    public static int hashCode(short[] array) {
        if (array == null) {
            return 0;
        }
        int hashCode = 1;
        for (short element : array) {
            hashCode = (hashCode * 31) + element;
        }
        return hashCode;
    }

    public static int hashCode(char[] array) {
        if (array == null) {
            return 0;
        }
        int hashCode = 1;
        for (char element : array) {
            hashCode = (hashCode * 31) + element;
        }
        return hashCode;
    }

    public static int hashCode(byte[] array) {
        if (array == null) {
            return 0;
        }
        int hashCode = 1;
        for (byte element : array) {
            hashCode = (hashCode * 31) + element;
        }
        return hashCode;
    }

    public static int hashCode(long[] array) {
        if (array == null) {
            return 0;
        }
        int hashCode = 1;
        for (long elementValue : array) {
            hashCode = (hashCode * 31) + ((int) ((elementValue >>> 32) ^ elementValue));
        }
        return hashCode;
    }

    public static int hashCode(float[] array) {
        if (array == null) {
            return 0;
        }
        int hashCode = 1;
        for (float element : array) {
            hashCode = (hashCode * 31) + Float.floatToIntBits(element);
        }
        return hashCode;
    }

    public static int hashCode(double[] array) {
        if (array == null) {
            return 0;
        }
        int hashCode = 1;
        for (double element : array) {
            long v = Double.doubleToLongBits(element);
            hashCode = (hashCode * 31) + ((int) ((v >>> 32) ^ v));
        }
        return hashCode;
    }

    public static int hashCode(Object[] array) {
        int elementHashCode;
        if (array == null) {
            return 0;
        }
        int hashCode = 1;
        for (Object element : array) {
            if (element == null) {
                elementHashCode = 0;
            } else {
                elementHashCode = element.hashCode();
            }
            hashCode = (hashCode * 31) + elementHashCode;
        }
        return hashCode;
    }

    public static int deepHashCode(Object[] array) {
        if (array == null) {
            return 0;
        }
        int hashCode = 1;
        for (Object element : array) {
            int elementHashCode = deepHashCodeElement(element);
            hashCode = (hashCode * 31) + elementHashCode;
        }
        return hashCode;
    }

    private static int deepHashCodeElement(Object element) {
        if (element == null) {
            return 0;
        }
        Class<?> cl = element.getClass().getComponentType();
        if (cl == null) {
            return element.hashCode();
        }
        if (element instanceof Object[]) {
            return deepHashCode((Object[]) element);
        }
        if (cl == Integer.TYPE) {
            return hashCode((int[]) element);
        }
        if (cl == Character.TYPE) {
            return hashCode((char[]) element);
        }
        if (cl == Boolean.TYPE) {
            return hashCode((boolean[]) element);
        }
        if (cl == Byte.TYPE) {
            return hashCode((byte[]) element);
        }
        if (cl == Long.TYPE) {
            return hashCode((long[]) element);
        }
        if (cl == Float.TYPE) {
            return hashCode((float[]) element);
        }
        if (cl == Double.TYPE) {
            return hashCode((double[]) element);
        }
        return hashCode((short[]) element);
    }

    public static boolean equals(byte[] array1, byte[] array2) {
        if (array1 == array2) {
            return true;
        }
        if (array1 == null || array2 == null || array1.length != array2.length) {
            return false;
        }
        for (int i = 0; i < array1.length; i++) {
            if (array1[i] != array2[i]) {
                return false;
            }
        }
        return true;
    }

    public static boolean equals(short[] array1, short[] array2) {
        if (array1 == array2) {
            return true;
        }
        if (array1 == null || array2 == null || array1.length != array2.length) {
            return false;
        }
        for (int i = 0; i < array1.length; i++) {
            if (array1[i] != array2[i]) {
                return false;
            }
        }
        return true;
    }

    public static boolean equals(char[] array1, char[] array2) {
        if (array1 == array2) {
            return true;
        }
        if (array1 == null || array2 == null || array1.length != array2.length) {
            return false;
        }
        for (int i = 0; i < array1.length; i++) {
            if (array1[i] != array2[i]) {
                return false;
            }
        }
        return true;
    }

    public static boolean equals(int[] array1, int[] array2) {
        if (array1 == array2) {
            return true;
        }
        if (array1 == null || array2 == null || array1.length != array2.length) {
            return false;
        }
        for (int i = 0; i < array1.length; i++) {
            if (array1[i] != array2[i]) {
                return false;
            }
        }
        return true;
    }

    public static boolean equals(long[] array1, long[] array2) {
        if (array1 == array2) {
            return true;
        }
        if (array1 == null || array2 == null || array1.length != array2.length) {
            return false;
        }
        for (int i = 0; i < array1.length; i++) {
            if (array1[i] != array2[i]) {
                return false;
            }
        }
        return true;
    }

    public static boolean equals(float[] array1, float[] array2) {
        if (array1 == array2) {
            return true;
        }
        if (array1 == null || array2 == null || array1.length != array2.length) {
            return false;
        }
        for (int i = 0; i < array1.length; i++) {
            if (Float.floatToIntBits(array1[i]) != Float.floatToIntBits(array2[i])) {
                return false;
            }
        }
        return true;
    }

    public static boolean equals(double[] array1, double[] array2) {
        if (array1 == array2) {
            return true;
        }
        if (array1 == null || array2 == null || array1.length != array2.length) {
            return false;
        }
        for (int i = 0; i < array1.length; i++) {
            if (Double.doubleToLongBits(array1[i]) != Double.doubleToLongBits(array2[i])) {
                return false;
            }
        }
        return true;
    }

    public static boolean equals(boolean[] array1, boolean[] array2) {
        if (array1 == array2) {
            return true;
        }
        if (array1 == null || array2 == null || array1.length != array2.length) {
            return false;
        }
        for (int i = 0; i < array1.length; i++) {
            if (array1[i] != array2[i]) {
                return false;
            }
        }
        return true;
    }

    public static boolean equals(Object[] array1, Object[] array2) {
        if (array1 == array2) {
            return true;
        }
        if (array1 == null || array2 == null || array1.length != array2.length) {
            return false;
        }
        for (int i = 0; i < array1.length; i++) {
            Object e1 = array1[i];
            Object e2 = array2[i];
            if (e1 == null) {
                if (e2 != null) {
                    return false;
                }
            } else {
                if (!e1.equals(e2)) {
                    return false;
                }
            }
        }
        return true;
    }

    public static boolean deepEquals(Object[] array1, Object[] array2) {
        if (array1 == array2) {
            return true;
        }
        if (array1 == null || array2 == null || array1.length != array2.length) {
            return false;
        }
        for (int i = 0; i < array1.length; i++) {
            Object e1 = array1[i];
            Object e2 = array2[i];
            if (!deepEqualsElements(e1, e2)) {
                return false;
            }
        }
        return true;
    }

    private static boolean deepEqualsElements(Object e1, Object e2) {
        if (e1 == e2) {
            return true;
        }
        if (e1 == null || e2 == null) {
            return false;
        }
        Class<?> cl1 = e1.getClass().getComponentType();
        Class<?> cl2 = e2.getClass().getComponentType();
        if (cl1 != cl2) {
            return false;
        }
        if (cl1 == null) {
            return e1.equals(e2);
        }
        if (e1 instanceof Object[]) {
            return deepEquals((Object[]) e1, (Object[]) e2);
        }
        if (cl1 == Integer.TYPE) {
            return equals((int[]) e1, (int[]) e2);
        }
        if (cl1 == Character.TYPE) {
            return equals((char[]) e1, (char[]) e2);
        }
        if (cl1 == Boolean.TYPE) {
            return equals((boolean[]) e1, (boolean[]) e2);
        }
        if (cl1 == Byte.TYPE) {
            return equals((byte[]) e1, (byte[]) e2);
        }
        if (cl1 == Long.TYPE) {
            return equals((long[]) e1, (long[]) e2);
        }
        if (cl1 == Float.TYPE) {
            return equals((float[]) e1, (float[]) e2);
        }
        if (cl1 == Double.TYPE) {
            return equals((double[]) e1, (double[]) e2);
        }
        return equals((short[]) e1, (short[]) e2);
    }

    public static void sort(byte[] array) {
        DualPivotQuicksort.sort(array);
    }

    public static void sort(byte[] array, int start, int end) {
        DualPivotQuicksort.sort(array, start, end);
    }

    public static void checkOffsetAndCount(int arrayLength, int offset, int count) {
        if ((offset | count) < 0 || offset > arrayLength || arrayLength - offset < count) {
            throw new ArrayIndexOutOfBoundsException(arrayLength, offset, count);
        }
    }

    public static void checkStartAndEnd(int len, int start, int end) {
        if (start < 0 || end > len) {
            throw new ArrayIndexOutOfBoundsException("start < 0 || end > len. start=" + start + ", end=" + end + ", len=" + len);
        }
        if (start > end) {
            throw new IllegalArgumentException("start > end: " + start + " > " + end);
        }
    }

    public static void sort(char[] array) {
        DualPivotQuicksort.sort(array);
    }

    public static void sort(char[] array, int start, int end) {
        DualPivotQuicksort.sort(array, start, end);
    }

    public static void sort(double[] array) {
        DualPivotQuicksort.sort(array);
    }

    public static void sort(double[] array, int start, int end) {
        DualPivotQuicksort.sort(array, start, end);
    }

    public static void sort(float[] array) {
        DualPivotQuicksort.sort(array);
    }

    public static void sort(float[] array, int start, int end) {
        DualPivotQuicksort.sort(array, start, end);
    }

    public static void sort(int[] array) {
        DualPivotQuicksort.sort(array);
    }

    public static void sort(int[] array, int start, int end) {
        DualPivotQuicksort.sort(array, start, end);
    }

    public static void sort(long[] array) {
        DualPivotQuicksort.sort(array);
    }

    public static void sort(long[] array, int start, int end) {
        DualPivotQuicksort.sort(array, start, end);
    }

    public static void sort(short[] array) {
        DualPivotQuicksort.sort(array);
    }

    public static void sort(short[] array, int start, int end) {
        DualPivotQuicksort.sort(array, start, end);
    }

    public static void sort(Object[] array) {
        ComparableTimSort.sort(array);
    }

    public static void sort(Object[] array, int start, int end) {
        ComparableTimSort.sort(array, start, end);
    }

    public static <T> void sort(T[] array, int start, int end, Comparator<? super T> comparator) {
        TimSort.sort(array, start, end, comparator);
    }

    public static <T> void sort(T[] array, Comparator<? super T> comparator) {
        TimSort.sort(array, comparator);
    }

    public static String toString(boolean[] array) {
        if (array == null) {
            return "null";
        }
        if (array.length == 0) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder(array.length * 7);
        sb.append('[');
        sb.append(array[0]);
        for (int i = 1; i < array.length; i++) {
            sb.append(", ");
            sb.append(array[i]);
        }
        sb.append(']');
        return sb.toString();
    }

    public static String toString(byte[] array) {
        if (array == null) {
            return "null";
        }
        if (array.length == 0) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder(array.length * 6);
        sb.append('[');
        sb.append((int) array[0]);
        for (int i = 1; i < array.length; i++) {
            sb.append(", ");
            sb.append((int) array[i]);
        }
        sb.append(']');
        return sb.toString();
    }

    public static String toString(char[] array) {
        if (array == null) {
            return "null";
        }
        if (array.length == 0) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder(array.length * 3);
        sb.append('[');
        sb.append(array[0]);
        for (int i = 1; i < array.length; i++) {
            sb.append(", ");
            sb.append(array[i]);
        }
        sb.append(']');
        return sb.toString();
    }

    public static String toString(double[] array) {
        if (array == null) {
            return "null";
        }
        if (array.length == 0) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder(array.length * 7);
        sb.append('[');
        sb.append(array[0]);
        for (int i = 1; i < array.length; i++) {
            sb.append(", ");
            sb.append(array[i]);
        }
        sb.append(']');
        return sb.toString();
    }

    public static String toString(float[] array) {
        if (array == null) {
            return "null";
        }
        if (array.length == 0) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder(array.length * 7);
        sb.append('[');
        sb.append(array[0]);
        for (int i = 1; i < array.length; i++) {
            sb.append(", ");
            sb.append(array[i]);
        }
        sb.append(']');
        return sb.toString();
    }

    public static String toString(int[] array) {
        if (array == null) {
            return "null";
        }
        if (array.length == 0) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder(array.length * 6);
        sb.append('[');
        sb.append(array[0]);
        for (int i = 1; i < array.length; i++) {
            sb.append(", ");
            sb.append(array[i]);
        }
        sb.append(']');
        return sb.toString();
    }

    public static String toString(long[] array) {
        if (array == null) {
            return "null";
        }
        if (array.length == 0) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder(array.length * 6);
        sb.append('[');
        sb.append(array[0]);
        for (int i = 1; i < array.length; i++) {
            sb.append(", ");
            sb.append(array[i]);
        }
        sb.append(']');
        return sb.toString();
    }

    public static String toString(short[] array) {
        if (array == null) {
            return "null";
        }
        if (array.length == 0) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder(array.length * 6);
        sb.append('[');
        sb.append((int) array[0]);
        for (int i = 1; i < array.length; i++) {
            sb.append(", ");
            sb.append((int) array[i]);
        }
        sb.append(']');
        return sb.toString();
    }

    public static String toString(Object[] array) {
        if (array == null) {
            return "null";
        }
        if (array.length == 0) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder(array.length * 7);
        sb.append('[');
        sb.append(array[0]);
        for (int i = 1; i < array.length; i++) {
            sb.append(", ");
            sb.append(array[i]);
        }
        sb.append(']');
        return sb.toString();
    }

    public static String deepToString(Object[] array) {
        if (array == null) {
            return "null";
        }
        StringBuilder buf = new StringBuilder(array.length * 9);
        deepToStringImpl(array, new Object[]{array}, buf);
        return buf.toString();
    }

    private static void deepToStringImpl(Object[] array, Object[] origArrays, StringBuilder sb) {
        if (array == null) {
            sb.append("null");
            return;
        }
        sb.append('[');
        for (int i = 0; i < array.length; i++) {
            if (i != 0) {
                sb.append(", ");
            }
            Object elem = array[i];
            if (elem == null) {
                sb.append("null");
            } else {
                Class<?> elemClass = elem.getClass();
                if (elemClass.isArray()) {
                    Class<?> elemElemClass = elemClass.getComponentType();
                    if (elemElemClass.isPrimitive()) {
                        if (Boolean.TYPE.equals(elemElemClass)) {
                            sb.append(toString((boolean[]) elem));
                        } else if (Byte.TYPE.equals(elemElemClass)) {
                            sb.append(toString((byte[]) elem));
                        } else if (Character.TYPE.equals(elemElemClass)) {
                            sb.append(toString((char[]) elem));
                        } else if (Double.TYPE.equals(elemElemClass)) {
                            sb.append(toString((double[]) elem));
                        } else if (Float.TYPE.equals(elemElemClass)) {
                            sb.append(toString((float[]) elem));
                        } else if (Integer.TYPE.equals(elemElemClass)) {
                            sb.append(toString((int[]) elem));
                        } else if (Long.TYPE.equals(elemElemClass)) {
                            sb.append(toString((long[]) elem));
                        } else if (Short.TYPE.equals(elemElemClass)) {
                            sb.append(toString((short[]) elem));
                        } else {
                            throw new AssertionError();
                        }
                    } else if (deepToStringImplContains(origArrays, elem)) {
                        sb.append("[...]");
                    } else {
                        Object[] newArray = (Object[]) elem;
                        Object[] newOrigArrays = new Object[origArrays.length + 1];
                        System.arraycopy(origArrays, 0, newOrigArrays, 0, origArrays.length);
                        newOrigArrays[origArrays.length] = newArray;
                        deepToStringImpl(newArray, newOrigArrays, sb);
                    }
                } else {
                    sb.append(array[i]);
                }
            }
        }
        sb.append(']');
    }

    private static boolean deepToStringImplContains(Object[] origArrays, Object array) {
        if (origArrays == null || origArrays.length == 0) {
            return false;
        }
        for (Object element : origArrays) {
            if (element == array) {
                return true;
            }
        }
        return false;
    }

    public static boolean[] copyOf(boolean[] original, int newLength) {
        if (newLength < 0) {
            throw new NegativeArraySizeException(Integer.toString(newLength));
        }
        return copyOfRange(original, 0, newLength);
    }

    public static byte[] copyOf(byte[] original, int newLength) {
        if (newLength < 0) {
            throw new NegativeArraySizeException(Integer.toString(newLength));
        }
        return copyOfRange(original, 0, newLength);
    }

    public static char[] copyOf(char[] original, int newLength) {
        if (newLength < 0) {
            throw new NegativeArraySizeException(Integer.toString(newLength));
        }
        return copyOfRange(original, 0, newLength);
    }

    public static double[] copyOf(double[] original, int newLength) {
        if (newLength < 0) {
            throw new NegativeArraySizeException(Integer.toString(newLength));
        }
        return copyOfRange(original, 0, newLength);
    }

    public static float[] copyOf(float[] original, int newLength) {
        if (newLength < 0) {
            throw new NegativeArraySizeException(Integer.toString(newLength));
        }
        return copyOfRange(original, 0, newLength);
    }

    public static int[] copyOf(int[] original, int newLength) {
        if (newLength < 0) {
            throw new NegativeArraySizeException(Integer.toString(newLength));
        }
        return copyOfRange(original, 0, newLength);
    }

    public static long[] copyOf(long[] original, int newLength) {
        if (newLength < 0) {
            throw new NegativeArraySizeException(Integer.toString(newLength));
        }
        return copyOfRange(original, 0, newLength);
    }

    public static short[] copyOf(short[] original, int newLength) {
        if (newLength < 0) {
            throw new NegativeArraySizeException(Integer.toString(newLength));
        }
        return copyOfRange(original, 0, newLength);
    }

    public static <T> T[] copyOf(T[] tArr, int i) {
        if (tArr == null) {
            throw new NullPointerException("original == null");
        }
        if (i < 0) {
            throw new NegativeArraySizeException(Integer.toString(i));
        }
        return (T[]) copyOfRange(tArr, 0, i);
    }

    public static <T, U> T[] copyOf(U[] uArr, int i, Class<? extends T[]> cls) {
        if (i < 0) {
            throw new NegativeArraySizeException(Integer.toString(i));
        }
        return (T[]) copyOfRange(uArr, 0, i, cls);
    }

    public static boolean[] copyOfRange(boolean[] original, int start, int end) {
        if (start > end) {
            throw new IllegalArgumentException();
        }
        int originalLength = original.length;
        if (start < 0 || start > originalLength) {
            throw new ArrayIndexOutOfBoundsException();
        }
        int resultLength = end - start;
        int copyLength = Math.min(resultLength, originalLength - start);
        boolean[] result = new boolean[resultLength];
        System.arraycopy(original, start, result, 0, copyLength);
        return result;
    }

    public static byte[] copyOfRange(byte[] original, int start, int end) {
        if (start > end) {
            throw new IllegalArgumentException();
        }
        int originalLength = original.length;
        if (start < 0 || start > originalLength) {
            throw new ArrayIndexOutOfBoundsException();
        }
        int resultLength = end - start;
        int copyLength = Math.min(resultLength, originalLength - start);
        byte[] result = new byte[resultLength];
        System.arraycopy(original, start, result, 0, copyLength);
        return result;
    }

    public static char[] copyOfRange(char[] original, int start, int end) {
        if (start > end) {
            throw new IllegalArgumentException();
        }
        int originalLength = original.length;
        if (start < 0 || start > originalLength) {
            throw new ArrayIndexOutOfBoundsException();
        }
        int resultLength = end - start;
        int copyLength = Math.min(resultLength, originalLength - start);
        char[] result = new char[resultLength];
        System.arraycopy(original, start, result, 0, copyLength);
        return result;
    }

    public static double[] copyOfRange(double[] original, int start, int end) {
        if (start > end) {
            throw new IllegalArgumentException();
        }
        int originalLength = original.length;
        if (start < 0 || start > originalLength) {
            throw new ArrayIndexOutOfBoundsException();
        }
        int resultLength = end - start;
        int copyLength = Math.min(resultLength, originalLength - start);
        double[] result = new double[resultLength];
        System.arraycopy(original, start, result, 0, copyLength);
        return result;
    }

    public static float[] copyOfRange(float[] original, int start, int end) {
        if (start > end) {
            throw new IllegalArgumentException();
        }
        int originalLength = original.length;
        if (start < 0 || start > originalLength) {
            throw new ArrayIndexOutOfBoundsException();
        }
        int resultLength = end - start;
        int copyLength = Math.min(resultLength, originalLength - start);
        float[] result = new float[resultLength];
        System.arraycopy(original, start, result, 0, copyLength);
        return result;
    }

    public static int[] copyOfRange(int[] original, int start, int end) {
        if (start > end) {
            throw new IllegalArgumentException();
        }
        int originalLength = original.length;
        if (start < 0 || start > originalLength) {
            throw new ArrayIndexOutOfBoundsException();
        }
        int resultLength = end - start;
        int copyLength = Math.min(resultLength, originalLength - start);
        int[] result = new int[resultLength];
        System.arraycopy(original, start, result, 0, copyLength);
        return result;
    }

    public static long[] copyOfRange(long[] original, int start, int end) {
        if (start > end) {
            throw new IllegalArgumentException();
        }
        int originalLength = original.length;
        if (start < 0 || start > originalLength) {
            throw new ArrayIndexOutOfBoundsException();
        }
        int resultLength = end - start;
        int copyLength = Math.min(resultLength, originalLength - start);
        long[] result = new long[resultLength];
        System.arraycopy(original, start, result, 0, copyLength);
        return result;
    }

    public static short[] copyOfRange(short[] original, int start, int end) {
        if (start > end) {
            throw new IllegalArgumentException();
        }
        int originalLength = original.length;
        if (start < 0 || start > originalLength) {
            throw new ArrayIndexOutOfBoundsException();
        }
        int resultLength = end - start;
        int copyLength = Math.min(resultLength, originalLength - start);
        short[] result = new short[resultLength];
        System.arraycopy(original, start, result, 0, copyLength);
        return result;
    }

    public static <T> T[] copyOfRange(T[] tArr, int i, int i2) {
        int length = tArr.length;
        if (i > i2) {
            throw new IllegalArgumentException();
        }
        if (i < 0 || i > length) {
            throw new ArrayIndexOutOfBoundsException();
        }
        int i3 = i2 - i;
        int iMin = Math.min(i3, length - i);
        T[] tArr2 = (T[]) ((Object[]) Array.newInstance(tArr.getClass().getComponentType(), i3));
        System.arraycopy(tArr, i, tArr2, 0, iMin);
        return tArr2;
    }

    public static <T, U> T[] copyOfRange(U[] uArr, int i, int i2, Class<? extends T[]> cls) {
        if (i > i2) {
            throw new IllegalArgumentException();
        }
        int length = uArr.length;
        if (i < 0 || i > length) {
            throw new ArrayIndexOutOfBoundsException();
        }
        int i3 = i2 - i;
        int iMin = Math.min(i3, length - i);
        T[] tArr = (T[]) ((Object[]) Array.newInstance(cls.getComponentType(), i3));
        System.arraycopy(uArr, i, tArr, 0, iMin);
        return tArr;
    }
}
