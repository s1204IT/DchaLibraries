package java.util;

import java.lang.reflect.Array;

class TimSort<T> {
    static final boolean $assertionsDisabled = false;
    private static final int INITIAL_TMP_STORAGE_LENGTH = 256;
    private static final int MIN_GALLOP = 7;
    private static final int MIN_MERGE = 32;
    private final T[] a;
    private final Comparator<? super T> c;
    private final int[] runBase;
    private final int[] runLen;
    private T[] tmp;
    private int tmpBase;
    private int tmpLen;
    private int minGallop = 7;
    private int stackSize = 0;

    private TimSort(T[] tArr, Comparator<? super T> comparator, T[] tArr2, int i, int i2) {
        int i3;
        this.a = tArr;
        this.c = comparator;
        int length = tArr.length;
        int i4 = length < 512 ? length >>> 1 : 256;
        if (tArr2 == null || i2 < i4 || i + i4 > tArr2.length) {
            this.tmp = (T[]) ((Object[]) Array.newInstance(tArr.getClass().getComponentType(), i4));
            this.tmpBase = 0;
            this.tmpLen = i4;
        } else {
            this.tmp = tArr2;
            this.tmpBase = i;
            this.tmpLen = i2;
        }
        if (length < 120) {
            i3 = 5;
        } else if (length < 1542) {
            i3 = 10;
        } else {
            i3 = length < 119151 ? 24 : 49;
        }
        this.runBase = new int[i3];
        this.runLen = new int[i3];
    }

    static <T> void sort(T[] tArr, int i, int i2, Comparator<? super T> comparator, T[] tArr2, int i3, int i4) {
        int i5 = i2 - i;
        if (i5 < 2) {
            return;
        }
        if (i5 < 32) {
            binarySort(tArr, i, i2, countRunAndMakeAscending(tArr, i, i2, comparator) + i, comparator);
            return;
        }
        TimSort timSort = new TimSort(tArr, comparator, tArr2, i3, i4);
        int iMinRunLength = minRunLength(i5);
        do {
            int iCountRunAndMakeAscending = countRunAndMakeAscending(tArr, i, i2, comparator);
            if (iCountRunAndMakeAscending < iMinRunLength) {
                int i6 = i5 <= iMinRunLength ? i5 : iMinRunLength;
                binarySort(tArr, i, i + i6, iCountRunAndMakeAscending + i, comparator);
                iCountRunAndMakeAscending = i6;
            }
            timSort.pushRun(i, iCountRunAndMakeAscending);
            timSort.mergeCollapse();
            i += iCountRunAndMakeAscending;
            i5 -= iCountRunAndMakeAscending;
        } while (i5 != 0);
        timSort.mergeForceCollapse();
    }

    private static <T> void binarySort(T[] tArr, int i, int i2, int i3, Comparator<? super T> comparator) {
        if (i3 == i) {
            i3++;
        }
        while (i3 < i2) {
            T t = tArr[i3];
            int i4 = i;
            int i5 = i3;
            while (i4 < i5) {
                int i6 = (i4 + i5) >>> 1;
                if (comparator.compare(t, tArr[i6]) >= 0) {
                    i4 = i6 + 1;
                } else {
                    i5 = i6;
                }
            }
            int i7 = i3 - i4;
            switch (i7) {
                case 2:
                    tArr[i4 + 2] = tArr[i4 + 1];
                case 1:
                    tArr[i4 + 1] = tArr[i4];
                    break;
                default:
                    System.arraycopy(tArr, i4, tArr, i4 + 1, i7);
                    break;
            }
            tArr[i4] = t;
            i3++;
        }
    }

    private static <T> int countRunAndMakeAscending(T[] tArr, int i, int i2, Comparator<? super T> comparator) {
        int i3 = i + 1;
        if (i3 == i2) {
            return 1;
        }
        int i4 = i3 + 1;
        if (comparator.compare(tArr[i3], tArr[i]) < 0) {
            while (i4 < i2 && comparator.compare(tArr[i4], tArr[i4 - 1]) < 0) {
                i4++;
            }
            reverseRange(tArr, i, i4);
        } else {
            while (i4 < i2 && comparator.compare(tArr[i4], tArr[i4 - 1]) >= 0) {
                i4++;
            }
        }
        return i4 - i;
    }

    private static void reverseRange(Object[] objArr, int i, int i2) {
        int i3 = i2 - 1;
        while (i < i3) {
            Object obj = objArr[i];
            objArr[i] = objArr[i3];
            objArr[i3] = obj;
            i3--;
            i++;
        }
    }

    private static int minRunLength(int i) {
        int i2 = 0;
        while (i >= 32) {
            i2 |= i & 1;
            i >>= 1;
        }
        return i + i2;
    }

    private void pushRun(int i, int i2) {
        this.runBase[this.stackSize] = i;
        this.runLen[this.stackSize] = i2;
        this.stackSize++;
    }

    private void mergeCollapse() {
        while (this.stackSize > 1) {
            int i = this.stackSize - 2;
            if (i > 0) {
                int i2 = i - 1;
                int i3 = i + 1;
                if (this.runLen[i2] <= this.runLen[i] + this.runLen[i3]) {
                    if (this.runLen[i2] < this.runLen[i3]) {
                        i--;
                    }
                    mergeAt(i);
                }
            }
            if (this.runLen[i] <= this.runLen[i + 1]) {
                mergeAt(i);
            } else {
                return;
            }
        }
    }

    private void mergeForceCollapse() {
        while (this.stackSize > 1) {
            int i = this.stackSize - 2;
            if (i > 0 && this.runLen[i - 1] < this.runLen[i + 1]) {
                i--;
            }
            mergeAt(i);
        }
    }

    private void mergeAt(int i) {
        int iGallopLeft;
        int i2 = this.runBase[i];
        int i3 = this.runLen[i];
        int i4 = i + 1;
        int i5 = this.runBase[i4];
        int i6 = this.runLen[i4];
        this.runLen[i] = i3 + i6;
        if (i == this.stackSize - 3) {
            int i7 = i + 2;
            this.runBase[i4] = this.runBase[i7];
            this.runLen[i4] = this.runLen[i7];
        }
        this.stackSize--;
        int iGallopRight = gallopRight(this.a[i5], this.a, i2, i3, 0, this.c);
        int i8 = i2 + iGallopRight;
        int i9 = i3 - iGallopRight;
        if (i9 == 0 || (iGallopLeft = gallopLeft(this.a[(i8 + i9) - 1], this.a, i5, i6, i6 - 1, this.c)) == 0) {
            return;
        }
        if (i9 <= iGallopLeft) {
            mergeLo(i8, i9, i5, iGallopLeft);
        } else {
            mergeHi(i8, i9, i5, iGallopLeft);
        }
    }

    private static <T> int gallopLeft(T t, T[] tArr, int i, int i2, int i3, Comparator<? super T> comparator) {
        int i4;
        int i5;
        int i6 = i + i3;
        if (comparator.compare(t, tArr[i6]) > 0) {
            int i7 = i2 - i3;
            int i8 = 0;
            int i9 = 1;
            while (i9 < i7 && comparator.compare(t, tArr[i6 + i9]) > 0) {
                int i10 = (i9 << 1) + 1;
                if (i10 <= 0) {
                    i8 = i9;
                    i9 = i7;
                } else {
                    i8 = i9;
                    i9 = i10;
                }
            }
            if (i9 <= i7) {
                i7 = i9;
            }
            int i11 = i8 + i3;
            i5 = i7 + i3;
            i4 = i11;
        } else {
            int i12 = i3 + 1;
            int i13 = 0;
            int i14 = 1;
            while (i14 < i12 && comparator.compare(t, tArr[i6 - i14]) <= 0) {
                int i15 = (i14 << 1) + 1;
                if (i15 <= 0) {
                    i13 = i14;
                    i14 = i12;
                } else {
                    i13 = i14;
                    i14 = i15;
                }
            }
            if (i14 <= i12) {
                i12 = i14;
            }
            i4 = i3 - i12;
            i5 = i3 - i13;
        }
        int i16 = i4 + 1;
        while (i16 < i5) {
            int i17 = ((i5 - i16) >>> 1) + i16;
            if (comparator.compare(t, tArr[i + i17]) > 0) {
                i16 = i17 + 1;
            } else {
                i5 = i17;
            }
        }
        return i5;
    }

    private static <T> int gallopRight(T t, T[] tArr, int i, int i2, int i3, Comparator<? super T> comparator) {
        int i4;
        int i5;
        int i6 = i + i3;
        if (comparator.compare(t, tArr[i6]) < 0) {
            int i7 = i3 + 1;
            int i8 = 0;
            int i9 = 1;
            while (i9 < i7 && comparator.compare(t, tArr[i6 - i9]) < 0) {
                int i10 = (i9 << 1) + 1;
                if (i10 <= 0) {
                    i8 = i9;
                    i9 = i7;
                } else {
                    i8 = i9;
                    i9 = i10;
                }
            }
            if (i9 <= i7) {
                i7 = i9;
            }
            i5 = i3 - i7;
            i4 = i3 - i8;
        } else {
            int i11 = i2 - i3;
            int i12 = 0;
            int i13 = 1;
            while (i13 < i11 && comparator.compare(t, tArr[i6 + i13]) >= 0) {
                int i14 = (i13 << 1) + 1;
                if (i14 <= 0) {
                    i12 = i13;
                    i13 = i11;
                } else {
                    i12 = i13;
                    i13 = i14;
                }
            }
            if (i13 <= i11) {
                i11 = i13;
            }
            int i15 = i12 + i3;
            i4 = i3 + i11;
            i5 = i15;
        }
        int i16 = i5 + 1;
        while (i16 < i4) {
            int i17 = ((i4 - i16) >>> 1) + i16;
            if (comparator.compare(t, tArr[i + i17]) >= 0) {
                i16 = i17 + 1;
            } else {
                i4 = i17;
            }
        }
        return i4;
    }

    private void mergeLo(int i, int i2, int i3, int i4) {
        int i5;
        int i6;
        int i7;
        int i8;
        int i9;
        int i10;
        int i11;
        int i12;
        int i13 = i2;
        T[] tArr = this.a;
        T[] tArrEnsureCapacity = ensureCapacity(i13);
        int i14 = this.tmpBase;
        System.arraycopy(tArr, i, tArrEnsureCapacity, i14, i13);
        int i15 = i + 1;
        int i16 = i3 + 1;
        tArr[i] = tArr[i3];
        int i17 = i4 - 1;
        if (i17 == 0) {
            System.arraycopy(tArrEnsureCapacity, i14, tArr, i15, i13);
            return;
        }
        int i18 = 1;
        if (i13 == 1) {
            System.arraycopy(tArr, i16, tArr, i15, i17);
            tArr[i15 + i17] = tArrEnsureCapacity[i14];
            return;
        }
        Comparator<? super T> comparator = this.c;
        int i19 = this.minGallop;
        loop0: while (true) {
            int i20 = 0;
            int i21 = 0;
            while (true) {
                if (comparator.compare(tArr[i16], tArrEnsureCapacity[i14]) < 0) {
                    i8 = i15 + 1;
                    int i22 = i16 + 1;
                    tArr[i15] = tArr[i16];
                    i20 += i18;
                    i17--;
                    if (i17 == 0) {
                        i6 = i17;
                        i9 = i13;
                        i7 = i22;
                        i5 = i14;
                        break loop0;
                    }
                    i15 = i8;
                    i16 = i22;
                    i21 = 0;
                    if ((i21 | i20) < i19) {
                        break;
                    }
                } else {
                    int i23 = i15 + 1;
                    i5 = i14 + 1;
                    tArr[i15] = tArrEnsureCapacity[i14];
                    i21 += i18;
                    i13--;
                    if (i13 == i18) {
                        i6 = i17;
                        i7 = i16;
                        i8 = i23;
                        i9 = i13;
                        break loop0;
                    }
                    i15 = i23;
                    i14 = i5;
                    i20 = 0;
                    if ((i21 | i20) < i19) {
                    }
                }
            }
            i19 = i10 + 2;
            i14 = i12;
            i15 = i11;
            i17 = i6;
            i13 = i9;
            i16 = i7;
        }
        i19 = i10;
        if (i19 < i18) {
            i19 = i18;
        }
        this.minGallop = i19;
        if (i9 == i18) {
            System.arraycopy(tArr, i7, tArr, i8, i6);
            tArr[i8 + i6] = tArrEnsureCapacity[i5];
        } else {
            if (i9 == 0) {
                throw new IllegalArgumentException("Comparison method violates its general contract!");
            }
            System.arraycopy(tArrEnsureCapacity, i5, tArr, i8, i9);
        }
    }

    private void mergeHi(int i, int i2, int i3, int i4) {
        int i5;
        int i6;
        int i7;
        int i8;
        int i9;
        int i10;
        int i11;
        int i12;
        int i13 = i4;
        T[] tArr = this.a;
        T[] tArrEnsureCapacity = ensureCapacity(i13);
        int i14 = this.tmpBase;
        System.arraycopy(tArr, i3, tArrEnsureCapacity, i14, i13);
        int i15 = (i + i2) - 1;
        int i16 = (i14 + i13) - 1;
        int i17 = (i3 + i13) - 1;
        int i18 = i17 - 1;
        int i19 = i15 - 1;
        tArr[i17] = tArr[i15];
        int i20 = i2 - 1;
        if (i20 == 0) {
            System.arraycopy(tArrEnsureCapacity, i14, tArr, i18 - (i13 - 1), i13);
            return;
        }
        if (i13 == 1) {
            int i21 = i18 - i20;
            System.arraycopy(tArr, (i19 - i20) + 1, tArr, i21 + 1, i20);
            tArr[i21] = tArrEnsureCapacity[i16];
            return;
        }
        Comparator<? super T> comparator = this.c;
        int i22 = this.minGallop;
        loop0: while (true) {
            int i23 = 0;
            int i24 = 0;
            while (true) {
                if (comparator.compare(tArrEnsureCapacity[i16], tArr[i19]) < 0) {
                    int i25 = i18 - 1;
                    int i26 = i19 - 1;
                    tArr[i18] = tArr[i19];
                    i23++;
                    i20--;
                    if (i20 == 0) {
                        i8 = i13;
                        i5 = i25;
                        i6 = i26;
                        i7 = i22;
                        break loop0;
                    }
                    i18 = i25;
                    i19 = i26;
                    i24 = 0;
                    if ((i23 | i24) < i22) {
                        break;
                    }
                } else {
                    int i27 = i18 - 1;
                    int i28 = i16 - 1;
                    tArr[i18] = tArrEnsureCapacity[i16];
                    i24++;
                    i13--;
                    if (i13 == 1) {
                        i16 = i28;
                        i5 = i27;
                        i6 = i19;
                        i7 = i22;
                        i8 = i13;
                        break loop0;
                    }
                    i16 = i28;
                    i18 = i27;
                    i23 = 0;
                    if ((i23 | i24) < i22) {
                    }
                }
            }
            i18 = i5;
            i16 = i10;
            i22 = i12 + 2;
            i20 = i9;
            int i29 = i8;
            i19 = i11;
            i13 = i29;
        }
        i16 = i10;
        if (i7 < 1) {
            i7 = 1;
        }
        this.minGallop = i7;
        if (i8 == 1) {
            int i30 = i5 - i20;
            System.arraycopy(tArr, (i6 - i20) + 1, tArr, i30 + 1, i20);
            tArr[i30] = tArrEnsureCapacity[i16];
        } else {
            if (i8 == 0) {
                throw new IllegalArgumentException("Comparison method violates its general contract!");
            }
            System.arraycopy(tArrEnsureCapacity, i14, tArr, i5 - (i8 - 1), i8);
        }
    }

    private T[] ensureCapacity(int i) {
        if (this.tmpLen < i) {
            int i2 = (i >> 1) | i;
            int i3 = i2 | (i2 >> 2);
            int i4 = i3 | (i3 >> 4);
            int i5 = i4 | (i4 >> 8);
            int i6 = (i5 | (i5 >> 16)) + 1;
            if (i6 >= 0) {
                i = Math.min(i6, this.a.length >>> 1);
            }
            this.tmp = (T[]) ((Object[]) Array.newInstance(this.a.getClass().getComponentType(), i));
            this.tmpLen = i;
            this.tmpBase = 0;
        }
        return this.tmp;
    }
}
