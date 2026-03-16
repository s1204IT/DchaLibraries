package java.util;

class TimSort<T> {
    private static final boolean DEBUG = false;
    private static final int INITIAL_TMP_STORAGE_LENGTH = 256;
    private static final int MIN_GALLOP = 7;
    private static final int MIN_MERGE = 32;
    private final T[] a;
    private final Comparator<? super T> c;
    private final int[] runBase;
    private final int[] runLen;
    private T[] tmp;
    private int minGallop = 7;
    private int stackSize = 0;

    private TimSort(T[] tArr, Comparator<? super T> comparator) {
        int i;
        this.a = tArr;
        this.c = comparator;
        int length = tArr.length;
        this.tmp = (T[]) new Object[length < 512 ? length >>> 1 : 256];
        if (length < 120) {
            i = 5;
        } else {
            i = length < 1542 ? 10 : length < 119151 ? 19 : 40;
        }
        this.runBase = new int[i];
        this.runLen = new int[i];
    }

    static <T> void sort(T[] a, Comparator<? super T> c) {
        sort(a, 0, a.length, c);
    }

    static <T> void sort(T[] a, int lo, int hi, Comparator<? super T> c) {
        if (c == null) {
            Arrays.sort(a, lo, hi);
            return;
        }
        Arrays.checkStartAndEnd(a.length, lo, hi);
        int nRemaining = hi - lo;
        if (nRemaining >= 2) {
            if (nRemaining < 32) {
                int initRunLen = countRunAndMakeAscending(a, lo, hi, c);
                binarySort(a, lo, hi, lo + initRunLen, c);
                return;
            }
            TimSort<T> ts = new TimSort<>(a, c);
            int minRun = minRunLength(nRemaining);
            do {
                int runLen = countRunAndMakeAscending(a, lo, hi, c);
                if (runLen < minRun) {
                    int force = nRemaining <= minRun ? nRemaining : minRun;
                    binarySort(a, lo, lo + force, lo + runLen, c);
                    runLen = force;
                }
                ts.pushRun(lo, runLen);
                ts.mergeCollapse();
                lo += runLen;
                nRemaining -= runLen;
            } while (nRemaining != 0);
            ts.mergeForceCollapse();
        }
    }

    private static <T> void binarySort(T[] a, int lo, int hi, int start, Comparator<? super T> c) {
        if (start == lo) {
            start++;
        }
        while (start < hi) {
            T pivot = a[start];
            int left = lo;
            int right = start;
            while (left < right) {
                int mid = (left + right) >>> 1;
                if (c.compare(pivot, a[mid]) < 0) {
                    right = mid;
                } else {
                    left = mid + 1;
                }
            }
            int n = start - left;
            switch (n) {
                case 1:
                    break;
                case 2:
                    a[left + 2] = a[left + 1];
                    break;
                default:
                    System.arraycopy(a, left, a, left + 1, n);
                    a[left] = pivot;
                    start++;
                    break;
            }
            a[left + 1] = a[left];
            a[left] = pivot;
            start++;
        }
    }

    private static <T> int countRunAndMakeAscending(T[] a, int lo, int hi, Comparator<? super T> c) {
        int runHi;
        int runHi2 = lo + 1;
        if (runHi2 == hi) {
            return 1;
        }
        int runHi3 = runHi2 + 1;
        if (c.compare(a[runHi2], a[lo]) < 0) {
            runHi = runHi3;
            while (runHi < hi && c.compare(a[runHi], a[runHi - 1]) < 0) {
                runHi++;
            }
            reverseRange(a, lo, runHi);
        } else {
            runHi = runHi3;
            while (runHi < hi && c.compare(a[runHi], a[runHi - 1]) >= 0) {
                runHi++;
            }
        }
        return runHi - lo;
    }

    private static void reverseRange(Object[] a, int lo, int hi) {
        int hi2 = hi - 1;
        for (int lo2 = lo; lo2 < hi2; lo2++) {
            Object t = a[lo2];
            a[lo2] = a[hi2];
            a[hi2] = t;
            hi2--;
        }
    }

    private static int minRunLength(int n) {
        int r = 0;
        while (n >= 32) {
            r |= n & 1;
            n >>= 1;
        }
        return n + r;
    }

    private void pushRun(int runBase, int runLen) {
        this.runBase[this.stackSize] = runBase;
        this.runLen[this.stackSize] = runLen;
        this.stackSize++;
    }

    private void mergeCollapse() {
        while (this.stackSize > 1) {
            int n = this.stackSize - 2;
            if (n > 0 && this.runLen[n - 1] <= this.runLen[n] + this.runLen[n + 1]) {
                if (this.runLen[n - 1] < this.runLen[n + 1]) {
                    n--;
                }
                mergeAt(n);
            } else if (this.runLen[n] <= this.runLen[n + 1]) {
                mergeAt(n);
            } else {
                return;
            }
        }
    }

    private void mergeForceCollapse() {
        while (this.stackSize > 1) {
            int n = this.stackSize - 2;
            if (n > 0 && this.runLen[n - 1] < this.runLen[n + 1]) {
                n--;
            }
            mergeAt(n);
        }
    }

    private void mergeAt(int i) {
        int len2;
        int base1 = this.runBase[i];
        int len1 = this.runLen[i];
        int base2 = this.runBase[i + 1];
        int len22 = this.runLen[i + 1];
        this.runLen[i] = len1 + len22;
        if (i == this.stackSize - 3) {
            this.runBase[i + 1] = this.runBase[i + 2];
            this.runLen[i + 1] = this.runLen[i + 2];
        }
        this.stackSize--;
        int k = gallopRight(this.a[base2], this.a, base1, len1, 0, this.c);
        int base12 = base1 + k;
        int len12 = len1 - k;
        if (len12 != 0 && (len2 = gallopLeft(this.a[(base12 + len12) - 1], this.a, base2, len22, len22 - 1, this.c)) != 0) {
            if (len12 <= len2) {
                mergeLo(base12, len12, base2, len2);
            } else {
                mergeHi(base12, len12, base2, len2);
            }
        }
    }

    private static <T> int gallopLeft(T key, T[] a, int base, int len, int hint, Comparator<? super T> c) {
        int lastOfs;
        int ofs;
        int lastOfs2 = 0;
        int ofs2 = 1;
        if (c.compare(key, a[base + hint]) > 0) {
            int maxOfs = len - hint;
            while (ofs2 < maxOfs && c.compare(key, a[base + hint + ofs2]) > 0) {
                lastOfs2 = ofs2;
                ofs2 = (ofs2 * 2) + 1;
                if (ofs2 <= 0) {
                    ofs2 = maxOfs;
                }
            }
            if (ofs2 > maxOfs) {
                ofs2 = maxOfs;
            }
            lastOfs = lastOfs2 + hint;
            ofs = ofs2 + hint;
        } else {
            int maxOfs2 = hint + 1;
            while (ofs2 < maxOfs2 && c.compare(key, a[(base + hint) - ofs2]) <= 0) {
                lastOfs2 = ofs2;
                ofs2 = (ofs2 * 2) + 1;
                if (ofs2 <= 0) {
                    ofs2 = maxOfs2;
                }
            }
            if (ofs2 > maxOfs2) {
                ofs2 = maxOfs2;
            }
            int tmp = lastOfs2;
            lastOfs = hint - ofs2;
            ofs = hint - tmp;
        }
        int lastOfs3 = lastOfs + 1;
        while (lastOfs3 < ofs) {
            int m = lastOfs3 + ((ofs - lastOfs3) >>> 1);
            if (c.compare(key, a[base + m]) > 0) {
                lastOfs3 = m + 1;
            } else {
                ofs = m;
            }
        }
        return ofs;
    }

    private static <T> int gallopRight(T key, T[] a, int base, int len, int hint, Comparator<? super T> c) {
        int lastOfs;
        int ofs;
        int ofs2 = 1;
        int lastOfs2 = 0;
        if (c.compare(key, a[base + hint]) < 0) {
            int maxOfs = hint + 1;
            while (ofs2 < maxOfs && c.compare(key, a[(base + hint) - ofs2]) < 0) {
                lastOfs2 = ofs2;
                ofs2 = (ofs2 * 2) + 1;
                if (ofs2 <= 0) {
                    ofs2 = maxOfs;
                }
            }
            if (ofs2 > maxOfs) {
                ofs2 = maxOfs;
            }
            int tmp = lastOfs2;
            lastOfs = hint - ofs2;
            ofs = hint - tmp;
        } else {
            int maxOfs2 = len - hint;
            while (ofs2 < maxOfs2 && c.compare(key, a[base + hint + ofs2]) >= 0) {
                lastOfs2 = ofs2;
                ofs2 = (ofs2 * 2) + 1;
                if (ofs2 <= 0) {
                    ofs2 = maxOfs2;
                }
            }
            if (ofs2 > maxOfs2) {
                ofs2 = maxOfs2;
            }
            lastOfs = lastOfs2 + hint;
            ofs = ofs2 + hint;
        }
        int lastOfs3 = lastOfs + 1;
        while (lastOfs3 < ofs) {
            int m = lastOfs3 + ((ofs - lastOfs3) >>> 1);
            if (c.compare(key, a[base + m]) < 0) {
                ofs = m;
            } else {
                lastOfs3 = m + 1;
            }
        }
        return ofs;
    }

    private void mergeLo(int base1, int len1, int base2, int len2) {
        int dest;
        int cursor1;
        T[] a = this.a;
        T[] tmp = ensureCapacity(len1);
        System.arraycopy(a, base1, tmp, 0, len1);
        int cursor12 = 0;
        int dest2 = base1 + 1;
        int cursor2 = base2 + 1;
        a[base1] = a[base2];
        int len22 = len2 - 1;
        if (len22 == 0) {
            System.arraycopy(tmp, 0, a, dest2, len1);
            return;
        }
        if (len1 == 1) {
            System.arraycopy(a, cursor2, a, dest2, len22);
            a[dest2 + len22] = tmp[0];
            return;
        }
        Comparator<? super T> c = this.c;
        int minGallop = this.minGallop;
        int dest3 = dest2;
        int cursor22 = cursor2;
        loop0: while (true) {
            int count1 = 0;
            int count2 = 0;
            while (true) {
                if (c.compare(a[cursor22], tmp[cursor12]) < 0) {
                    int dest4 = dest3 + 1;
                    int cursor23 = cursor22 + 1;
                    a[dest3] = a[cursor22];
                    count2++;
                    count1 = 0;
                    len22--;
                    if (len22 == 0) {
                        dest3 = dest4;
                        cursor22 = cursor23;
                        break loop0;
                    } else {
                        dest3 = dest4;
                        cursor22 = cursor23;
                        if ((count1 | count2) < minGallop) {
                            break;
                        }
                    }
                } else {
                    int dest5 = dest3 + 1;
                    int cursor13 = cursor12 + 1;
                    a[dest3] = tmp[cursor12];
                    count1++;
                    count2 = 0;
                    len1--;
                    if (len1 == 1) {
                        dest3 = dest5;
                        cursor12 = cursor13;
                        break loop0;
                    } else {
                        dest3 = dest5;
                        cursor12 = cursor13;
                        if ((count1 | count2) < minGallop) {
                        }
                    }
                }
            }
            minGallop += 2;
            dest3 = dest;
            cursor12 = cursor1;
        }
        if (minGallop < 1) {
            minGallop = 1;
        }
        this.minGallop = minGallop;
        if (len1 == 1) {
            System.arraycopy(a, cursor22, a, dest3, len22);
            a[dest3 + len22] = tmp[cursor12];
        } else {
            if (len1 == 0) {
                throw new IllegalArgumentException("Comparison method violates its general contract!");
            }
            System.arraycopy(tmp, cursor12, a, dest3, len1);
        }
    }

    private void mergeHi(int base1, int len1, int base2, int len2) {
        int dest;
        int cursor1;
        T[] a = this.a;
        T[] tmp = ensureCapacity(len2);
        System.arraycopy(a, base2, tmp, 0, len2);
        int cursor12 = (base1 + len1) - 1;
        int cursor2 = len2 - 1;
        int dest2 = (base2 + len2) - 1;
        int dest3 = dest2 - 1;
        int cursor13 = cursor12 - 1;
        a[dest2] = a[cursor12];
        int len12 = len1 - 1;
        if (len12 == 0) {
            System.arraycopy(tmp, 0, a, dest3 - (len2 - 1), len2);
            return;
        }
        if (len2 == 1) {
            int dest4 = dest3 - len12;
            System.arraycopy(a, (cursor13 - len12) + 1, a, dest4 + 1, len12);
            a[dest4] = tmp[cursor2];
            return;
        }
        Comparator<? super T> c = this.c;
        int minGallop = this.minGallop;
        loop0: while (true) {
            dest = dest3;
            cursor1 = cursor13;
            int count1 = 0;
            int count2 = 0;
            while (true) {
                if (c.compare(tmp[cursor2], a[cursor1]) < 0) {
                    int dest5 = dest - 1;
                    int cursor14 = cursor1 - 1;
                    a[dest] = a[cursor1];
                    count1++;
                    count2 = 0;
                    len12--;
                    if (len12 == 0) {
                        dest = dest5;
                        cursor1 = cursor14;
                        break loop0;
                    } else {
                        dest = dest5;
                        cursor1 = cursor14;
                        if ((count1 | count2) < minGallop) {
                            break;
                        }
                    }
                } else {
                    int dest6 = dest - 1;
                    int cursor22 = cursor2 - 1;
                    a[dest] = tmp[cursor2];
                    count2++;
                    count1 = 0;
                    len2--;
                    if (len2 == 1) {
                        dest = dest6;
                        cursor2 = cursor22;
                        break loop0;
                    } else {
                        dest = dest6;
                        cursor2 = cursor22;
                        if ((count1 | count2) < minGallop) {
                        }
                    }
                }
            }
            minGallop += 2;
        }
        if (minGallop < 1) {
            minGallop = 1;
        }
        this.minGallop = minGallop;
        if (len2 == 1) {
            int dest7 = dest - len12;
            System.arraycopy(a, (cursor1 - len12) + 1, a, dest7 + 1, len12);
            a[dest7] = tmp[cursor2];
        } else {
            if (len2 == 0) {
                throw new IllegalArgumentException("Comparison method violates its general contract!");
            }
            System.arraycopy(tmp, 0, a, dest - (len2 - 1), len2);
        }
    }

    private T[] ensureCapacity(int i) {
        int iMin;
        if (this.tmp.length < i) {
            int i2 = i | (i >> 1);
            int i3 = i2 | (i2 >> 2);
            int i4 = i3 | (i3 >> 4);
            int i5 = i4 | (i4 >> 8);
            int i6 = (i5 | (i5 >> 16)) + 1;
            if (i6 < 0) {
                iMin = i;
            } else {
                iMin = Math.min(i6, this.a.length >>> 1);
            }
            this.tmp = (T[]) new Object[iMin];
        }
        return this.tmp;
    }
}
