package java.util;

class ComparableTimSort {
    static final boolean $assertionsDisabled = false;
    private static final int INITIAL_TMP_STORAGE_LENGTH = 256;
    private static final int MIN_GALLOP = 7;
    private static final int MIN_MERGE = 32;
    private final Object[] a;
    private final int[] runBase;
    private final int[] runLen;
    private Object[] tmp;
    private int tmpBase;
    private int tmpLen;
    private int minGallop = 7;
    private int stackSize = 0;

    private ComparableTimSort(Object[] objArr, Object[] objArr2, int i, int i2) {
        int i3;
        this.a = objArr;
        int length = objArr.length;
        int i4 = length < 512 ? length >>> 1 : 256;
        if (objArr2 == null || i2 < i4 || i + i4 > objArr2.length) {
            this.tmp = new Object[i4];
            this.tmpBase = 0;
            this.tmpLen = i4;
        } else {
            this.tmp = objArr2;
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

    static void sort(Object[] objArr, int i, int i2, Object[] objArr2, int i3, int i4) {
        int i5 = i2 - i;
        if (i5 < 2) {
            return;
        }
        if (i5 < 32) {
            binarySort(objArr, i, i2, countRunAndMakeAscending(objArr, i, i2) + i);
            return;
        }
        ComparableTimSort comparableTimSort = new ComparableTimSort(objArr, objArr2, i3, i4);
        int iMinRunLength = minRunLength(i5);
        do {
            int iCountRunAndMakeAscending = countRunAndMakeAscending(objArr, i, i2);
            if (iCountRunAndMakeAscending < iMinRunLength) {
                int i6 = i5 <= iMinRunLength ? i5 : iMinRunLength;
                binarySort(objArr, i, i + i6, iCountRunAndMakeAscending + i);
                iCountRunAndMakeAscending = i6;
            }
            comparableTimSort.pushRun(i, iCountRunAndMakeAscending);
            comparableTimSort.mergeCollapse();
            i += iCountRunAndMakeAscending;
            i5 -= iCountRunAndMakeAscending;
        } while (i5 != 0);
        comparableTimSort.mergeForceCollapse();
    }

    private static void binarySort(Object[] objArr, int i, int i2, int i3) {
        if (i3 == i) {
            i3++;
        }
        while (i3 < i2) {
            Comparable comparable = (Comparable) objArr[i3];
            int i4 = i;
            int i5 = i3;
            while (i4 < i5) {
                int i6 = (i4 + i5) >>> 1;
                if (comparable.compareTo(objArr[i6]) >= 0) {
                    i4 = i6 + 1;
                } else {
                    i5 = i6;
                }
            }
            int i7 = i3 - i4;
            switch (i7) {
                case 2:
                    objArr[i4 + 2] = objArr[i4 + 1];
                case 1:
                    objArr[i4 + 1] = objArr[i4];
                    break;
                default:
                    System.arraycopy(objArr, i4, objArr, i4 + 1, i7);
                    break;
            }
            objArr[i4] = comparable;
            i3++;
        }
    }

    private static int countRunAndMakeAscending(Object[] objArr, int i, int i2) {
        int i3 = i + 1;
        if (i3 == i2) {
            return 1;
        }
        int i4 = i3 + 1;
        if (((Comparable) objArr[i3]).compareTo(objArr[i]) < 0) {
            while (i4 < i2 && ((Comparable) objArr[i4]).compareTo(objArr[i4 - 1]) < 0) {
                i4++;
            }
            reverseRange(objArr, i, i4);
        } else {
            while (i4 < i2 && ((Comparable) objArr[i4]).compareTo(objArr[i4 - 1]) >= 0) {
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
        int iGallopRight = gallopRight((Comparable) this.a[i5], this.a, i2, i3, 0);
        int i8 = i2 + iGallopRight;
        int i9 = i3 - iGallopRight;
        if (i9 == 0 || (iGallopLeft = gallopLeft((Comparable) this.a[(i8 + i9) - 1], this.a, i5, i6, i6 - 1)) == 0) {
            return;
        }
        if (i9 <= iGallopLeft) {
            mergeLo(i8, i9, i5, iGallopLeft);
        } else {
            mergeHi(i8, i9, i5, iGallopLeft);
        }
    }

    private static int gallopLeft(Comparable<Object> comparable, Object[] objArr, int i, int i2, int i3) {
        int i4;
        int i5;
        int i6 = i + i3;
        if (comparable.compareTo(objArr[i6]) > 0) {
            int i7 = i2 - i3;
            int i8 = 0;
            int i9 = 1;
            while (i9 < i7 && comparable.compareTo(objArr[i6 + i9]) > 0) {
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
            while (i14 < i12 && comparable.compareTo(objArr[i6 - i14]) <= 0) {
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
            if (comparable.compareTo(objArr[i + i17]) > 0) {
                i16 = i17 + 1;
            } else {
                i5 = i17;
            }
        }
        return i5;
    }

    private static int gallopRight(Comparable<Object> comparable, Object[] objArr, int i, int i2, int i3) {
        int i4;
        int i5;
        int i6 = i + i3;
        if (comparable.compareTo(objArr[i6]) < 0) {
            int i7 = i3 + 1;
            int i8 = 0;
            int i9 = 1;
            while (i9 < i7 && comparable.compareTo(objArr[i6 - i9]) < 0) {
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
            while (i13 < i11 && comparable.compareTo(objArr[i6 + i13]) >= 0) {
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
            if (comparable.compareTo(objArr[i + i17]) >= 0) {
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
        Object[] objArr = this.a;
        Object[] objArrEnsureCapacity = ensureCapacity(i2);
        int i8 = this.tmpBase;
        System.arraycopy(objArr, i, objArrEnsureCapacity, i8, i2);
        int i9 = i + 1;
        int i10 = i3 + 1;
        objArr[i] = objArr[i3];
        int i11 = i4 - 1;
        if (i11 == 0) {
            System.arraycopy(objArrEnsureCapacity, i8, objArr, i9, i2);
            return;
        }
        if (i2 == 1) {
            System.arraycopy(objArr, i10, objArr, i9, i11);
            objArr[i9 + i11] = objArrEnsureCapacity[i8];
            return;
        }
        int i12 = this.minGallop;
        loop0: while (true) {
            int i13 = 0;
            int i14 = 0;
            while (true) {
                if (((Comparable) objArr[i10]).compareTo(objArrEnsureCapacity[i8]) < 0) {
                    i5 = i9 + 1;
                    int i15 = i10 + 1;
                    objArr[i9] = objArr[i10];
                    i13++;
                    i11--;
                    if (i11 == 0) {
                        break loop0;
                    }
                    i9 = i5;
                    i10 = i15;
                    i14 = 0;
                } else {
                    int i16 = i9 + 1;
                    int i17 = i8 + 1;
                    objArr[i9] = objArrEnsureCapacity[i8];
                    i14++;
                    i2--;
                    if (i2 != 1) {
                        i9 = i16;
                        i8 = i17;
                        i13 = 0;
                    } else {
                        i5 = i16;
                        i8 = i17;
                        break loop0;
                    }
                }
                if ((i14 | i13) >= i12) {
                    break;
                }
            }
            i12 += 2;
            i9 = i6;
            i8 = i7;
        }
        if (i12 < 1) {
            i12 = 1;
        }
        this.minGallop = i12;
        if (i2 == 1) {
            System.arraycopy(objArr, i10, objArr, i5, i11);
            objArr[i5 + i11] = objArrEnsureCapacity[i8];
        } else {
            if (i2 == 0) {
                throw new IllegalArgumentException("Comparison method violates its general contract!");
            }
            System.arraycopy(objArrEnsureCapacity, i8, objArr, i5, i2);
        }
    }

    private void mergeHi(int i, int i2, int i3, int i4) {
        int i5;
        int i6;
        int i7;
        int i8;
        int i9 = i4;
        Object[] objArr = this.a;
        Object[] objArrEnsureCapacity = ensureCapacity(i9);
        int i10 = this.tmpBase;
        System.arraycopy(objArr, i3, objArrEnsureCapacity, i10, i9);
        int i11 = (i + i2) - 1;
        int i12 = (i10 + i9) - 1;
        int i13 = (i3 + i9) - 1;
        int i14 = i13 - 1;
        int i15 = i11 - 1;
        objArr[i13] = objArr[i11];
        int i16 = i2 - 1;
        if (i16 == 0) {
            System.arraycopy(objArrEnsureCapacity, i10, objArr, i14 - (i9 - 1), i9);
            return;
        }
        if (i9 == 1) {
            int i17 = i14 - i16;
            System.arraycopy(objArr, (i15 - i16) + 1, objArr, i17 + 1, i16);
            objArr[i17] = objArrEnsureCapacity[i12];
            return;
        }
        int i18 = this.minGallop;
        loop0: while (true) {
            int i19 = 0;
            int i20 = 0;
            while (true) {
                if (((Comparable) objArrEnsureCapacity[i12]).compareTo(objArr[i15]) < 0) {
                    int i21 = i14 - 1;
                    int i22 = i15 - 1;
                    objArr[i14] = objArr[i15];
                    i19++;
                    i16--;
                    if (i16 == 0) {
                        i14 = i21;
                        i5 = i12;
                        i15 = i22;
                        break loop0;
                    } else {
                        i14 = i21;
                        i15 = i22;
                        i20 = 0;
                        if ((i19 | i20) < i18) {
                            break;
                        }
                    }
                } else {
                    int i23 = i14 - 1;
                    i5 = i12 - 1;
                    objArr[i14] = objArrEnsureCapacity[i12];
                    i20++;
                    i9--;
                    if (i9 == 1) {
                        i14 = i23;
                        break loop0;
                    }
                    i14 = i23;
                    i12 = i5;
                    i19 = 0;
                    if ((i19 | i20) < i18) {
                    }
                }
            }
            i18 = i8 + 2;
            i12 = i5;
            i14 = i6;
            i15 = i7;
        }
        if (i18 < 1) {
            i18 = 1;
        }
        this.minGallop = i18;
        if (i9 == 1) {
            int i24 = i14 - i16;
            System.arraycopy(objArr, (i15 - i16) + 1, objArr, i24 + 1, i16);
            objArr[i24] = objArrEnsureCapacity[i5];
        } else {
            if (i9 == 0) {
                throw new IllegalArgumentException("Comparison method violates its general contract!");
            }
            System.arraycopy(objArrEnsureCapacity, i10, objArr, i14 - (i9 - 1), i9);
        }
    }

    private Object[] ensureCapacity(int i) {
        if (this.tmpLen < i) {
            int i2 = (i >> 1) | i;
            int i3 = i2 | (i2 >> 2);
            int i4 = i3 | (i3 >> 4);
            int i5 = i4 | (i4 >> 8);
            int i6 = (i5 | (i5 >> 16)) + 1;
            if (i6 >= 0) {
                i = Math.min(i6, this.a.length >>> 1);
            }
            this.tmp = new Object[i];
            this.tmpLen = i;
            this.tmpBase = 0;
        }
        return this.tmp;
    }
}
