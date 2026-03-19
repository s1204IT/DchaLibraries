package com.google.common.primitives;

import com.google.common.base.Preconditions;
import java.io.Serializable;
import java.util.AbstractList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.RandomAccess;

public final class Ints {
    private static final byte[] asciiDigits = new byte[128];

    public static int hashCode(int i) {
        return i;
    }

    public static int saturatedCast(long j) {
        if (j > 2147483647L) {
            return Integer.MAX_VALUE;
        }
        if (j < -2147483648L) {
            return Integer.MIN_VALUE;
        }
        return (int) j;
    }

    private static int indexOf(int[] iArr, int i, int i2, int i3) {
        while (i2 < i3) {
            if (iArr[i2] != i) {
                i2++;
            } else {
                return i2;
            }
        }
        return -1;
    }

    private static int lastIndexOf(int[] iArr, int i, int i2, int i3) {
        for (int i4 = i3 - 1; i4 >= i2; i4--) {
            if (iArr[i4] == i) {
                return i4;
            }
        }
        return -1;
    }

    public static int[] toArray(Collection<? extends Number> collection) {
        if (collection instanceof IntArrayAsList) {
            return ((IntArrayAsList) collection).toIntArray();
        }
        Object[] array = collection.toArray();
        int length = array.length;
        int[] iArr = new int[length];
        for (int i = 0; i < length; i++) {
            iArr[i] = ((Number) Preconditions.checkNotNull(array[i])).intValue();
        }
        return iArr;
    }

    private static class IntArrayAsList extends AbstractList<Integer> implements Serializable, RandomAccess {
        private static final long serialVersionUID = 0;
        final int[] array;
        final int end;
        final int start;

        IntArrayAsList(int[] iArr, int i, int i2) {
            this.array = iArr;
            this.start = i;
            this.end = i2;
        }

        @Override
        public int size() {
            return this.end - this.start;
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

        @Override
        public Integer get(int i) {
            Preconditions.checkElementIndex(i, size());
            return Integer.valueOf(this.array[this.start + i]);
        }

        @Override
        public boolean contains(Object obj) {
            return (obj instanceof Integer) && Ints.indexOf(this.array, ((Integer) obj).intValue(), this.start, this.end) != -1;
        }

        @Override
        public int indexOf(Object obj) {
            int iIndexOf;
            if ((obj instanceof Integer) && (iIndexOf = Ints.indexOf(this.array, ((Integer) obj).intValue(), this.start, this.end)) >= 0) {
                return iIndexOf - this.start;
            }
            return -1;
        }

        @Override
        public int lastIndexOf(Object obj) {
            int iLastIndexOf;
            if ((obj instanceof Integer) && (iLastIndexOf = Ints.lastIndexOf(this.array, ((Integer) obj).intValue(), this.start, this.end)) >= 0) {
                return iLastIndexOf - this.start;
            }
            return -1;
        }

        @Override
        public Integer set(int i, Integer num) {
            Preconditions.checkElementIndex(i, size());
            int i2 = this.array[this.start + i];
            this.array[this.start + i] = ((Integer) Preconditions.checkNotNull(num)).intValue();
            return Integer.valueOf(i2);
        }

        @Override
        public List<Integer> subList(int i, int i2) {
            Preconditions.checkPositionIndexes(i, i2, size());
            if (i == i2) {
                return Collections.emptyList();
            }
            return new IntArrayAsList(this.array, this.start + i, this.start + i2);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj instanceof IntArrayAsList) {
                IntArrayAsList intArrayAsList = (IntArrayAsList) obj;
                int size = size();
                if (intArrayAsList.size() != size) {
                    return false;
                }
                for (int i = 0; i < size; i++) {
                    if (this.array[this.start + i] != intArrayAsList.array[intArrayAsList.start + i]) {
                        return false;
                    }
                }
                return true;
            }
            return super.equals(obj);
        }

        @Override
        public int hashCode() {
            int iHashCode = 1;
            for (int i = this.start; i < this.end; i++) {
                iHashCode = Ints.hashCode(this.array[i]) + (31 * iHashCode);
            }
            return iHashCode;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder(size() * 5);
            sb.append('[');
            sb.append(this.array[this.start]);
            int i = this.start;
            while (true) {
                i++;
                if (i < this.end) {
                    sb.append(", ");
                    sb.append(this.array[i]);
                } else {
                    sb.append(']');
                    return sb.toString();
                }
            }
        }

        int[] toIntArray() {
            int size = size();
            int[] iArr = new int[size];
            System.arraycopy(this.array, this.start, iArr, 0, size);
            return iArr;
        }
    }

    static {
        Arrays.fill(asciiDigits, (byte) -1);
        for (int i = 0; i <= 9; i++) {
            asciiDigits[48 + i] = (byte) i;
        }
        for (int i2 = 0; i2 <= 26; i2++) {
            byte b = (byte) (10 + i2);
            asciiDigits[65 + i2] = b;
            asciiDigits[97 + i2] = b;
        }
    }
}
