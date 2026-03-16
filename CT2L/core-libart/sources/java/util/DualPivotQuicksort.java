package java.util;

final class DualPivotQuicksort {
    private static final int COUNTING_SORT_THRESHOLD_FOR_BYTE = 128;
    private static final int COUNTING_SORT_THRESHOLD_FOR_SHORT_OR_CHAR = 32768;
    private static final int INSERTION_SORT_THRESHOLD = 32;
    private static final int NUM_BYTE_VALUES = 256;
    private static final int NUM_CHAR_VALUES = 65536;
    private static final int NUM_SHORT_VALUES = 65536;

    private DualPivotQuicksort() {
    }

    public static void sort(int[] a) {
        doSort(a, 0, a.length - 1);
    }

    public static void sort(int[] a, int fromIndex, int toIndex) {
        Arrays.checkStartAndEnd(a.length, fromIndex, toIndex);
        doSort(a, fromIndex, toIndex - 1);
    }

    private static void doSort(int[] a, int left, int right) {
        if ((right - left) + 1 < 32) {
            for (int i = left + 1; i <= right; i++) {
                int ai = a[i];
                int j = i - 1;
                while (j >= left && ai < a[j]) {
                    a[j + 1] = a[j];
                    j--;
                }
                a[j + 1] = ai;
            }
            return;
        }
        dualPivotQuicksort(a, left, right);
    }

    private static void dualPivotQuicksort(int[] a, int left, int right) {
        int less;
        int less2;
        int sixth = ((right - left) + 1) / 6;
        int e1 = left + sixth;
        int e5 = right - sixth;
        int e3 = (left + right) >>> 1;
        int e4 = e3 + sixth;
        int e2 = e3 - sixth;
        int ae1 = a[e1];
        int ae2 = a[e2];
        int ae3 = a[e3];
        int ae4 = a[e4];
        int ae5 = a[e5];
        if (ae1 > ae2) {
            ae1 = ae2;
            ae2 = ae1;
        }
        if (ae4 > ae5) {
            ae4 = ae5;
            ae5 = ae4;
        }
        if (ae1 > ae3) {
            int t = ae1;
            ae1 = ae3;
            ae3 = t;
        }
        if (ae2 > ae3) {
            int t2 = ae2;
            ae2 = ae3;
            ae3 = t2;
        }
        if (ae1 > ae4) {
            int t3 = ae1;
            ae1 = ae4;
            ae4 = t3;
        }
        if (ae3 > ae4) {
            int t4 = ae3;
            ae3 = ae4;
            ae4 = t4;
        }
        if (ae2 > ae5) {
            int t5 = ae2;
            ae2 = ae5;
            ae5 = t5;
        }
        if (ae2 > ae3) {
            int t6 = ae2;
            ae2 = ae3;
            ae3 = t6;
        }
        if (ae4 > ae5) {
            int t7 = ae4;
            ae4 = ae5;
            ae5 = t7;
        }
        a[e1] = ae1;
        a[e3] = ae3;
        a[e5] = ae5;
        int pivot1 = ae2;
        a[e2] = a[left];
        int pivot2 = ae4;
        a[e4] = a[right];
        int less3 = left + 1;
        int great = right - 1;
        boolean pivotsDiffer = pivot1 != pivot2;
        if (pivotsDiffer) {
            int k = less3;
            loop0: while (true) {
                int less4 = less3;
                if (k > great) {
                    less2 = less4;
                    break;
                }
                int ak = a[k];
                if (ak < pivot1) {
                    if (k != less4) {
                        a[k] = a[less4];
                        a[less4] = ak;
                    }
                    less3 = less4 + 1;
                } else if (ak > pivot2) {
                    while (a[great] > pivot2) {
                        int great2 = great - 1;
                        if (great == k) {
                            great = great2;
                            less2 = less4;
                            break loop0;
                        }
                        great = great2;
                    }
                    if (a[great] < pivot1) {
                        a[k] = a[less4];
                        less3 = less4 + 1;
                        a[less4] = a[great];
                        a[great] = ak;
                        great--;
                    } else {
                        a[k] = a[great];
                        a[great] = ak;
                        great--;
                        less3 = less4;
                    }
                } else {
                    less3 = less4;
                }
                k++;
            }
        } else {
            int k2 = less3;
            while (true) {
                less = less3;
                if (k2 > great) {
                    break;
                }
                int ak2 = a[k2];
                if (ak2 == pivot1) {
                    less3 = less;
                } else if (ak2 < pivot1) {
                    if (k2 != less) {
                        a[k2] = a[less];
                        a[less] = ak2;
                    }
                    less3 = less + 1;
                } else {
                    while (a[great] > pivot1) {
                        great--;
                    }
                    if (a[great] < pivot1) {
                        a[k2] = a[less];
                        less3 = less + 1;
                        a[less] = a[great];
                        a[great] = ak2;
                        great--;
                    } else {
                        a[k2] = pivot1;
                        a[great] = ak2;
                        great--;
                        less3 = less;
                    }
                }
                k2++;
            }
            less2 = less;
        }
        a[left] = a[less2 - 1];
        a[less2 - 1] = pivot1;
        a[right] = a[great + 1];
        a[great + 1] = pivot2;
        doSort(a, left, less2 - 2);
        doSort(a, great + 2, right);
        if (pivotsDiffer) {
            if (less2 < e1 && great > e5) {
                while (a[less2] == pivot1) {
                    less2++;
                }
                while (a[great] == pivot2) {
                    great--;
                }
                int k3 = less2;
                loop4: while (true) {
                    int less5 = less2;
                    if (k3 > great) {
                        less2 = less5;
                        break;
                    }
                    int ak3 = a[k3];
                    if (ak3 == pivot2) {
                        while (a[great] == pivot2) {
                            int great3 = great - 1;
                            if (great == k3) {
                                great = great3;
                                less2 = less5;
                                break loop4;
                            }
                            great = great3;
                        }
                        if (a[great] == pivot1) {
                            a[k3] = a[less5];
                            less2 = less5 + 1;
                            a[less5] = pivot1;
                        } else {
                            a[k3] = a[great];
                            less2 = less5;
                        }
                        a[great] = pivot2;
                        great--;
                    } else if (ak3 == pivot1) {
                        a[k3] = a[less5];
                        less2 = less5 + 1;
                        a[less5] = pivot1;
                    } else {
                        less2 = less5;
                    }
                    k3++;
                }
            }
            doSort(a, less2, great);
        }
    }

    public static void sort(long[] a) {
        doSort(a, 0, a.length - 1);
    }

    public static void sort(long[] a, int fromIndex, int toIndex) {
        Arrays.checkStartAndEnd(a.length, fromIndex, toIndex);
        doSort(a, fromIndex, toIndex - 1);
    }

    private static void doSort(long[] a, int left, int right) {
        if ((right - left) + 1 < 32) {
            for (int i = left + 1; i <= right; i++) {
                long ai = a[i];
                int j = i - 1;
                while (j >= left && ai < a[j]) {
                    a[j + 1] = a[j];
                    j--;
                }
                a[j + 1] = ai;
            }
            return;
        }
        dualPivotQuicksort(a, left, right);
    }

    private static void dualPivotQuicksort(long[] a, int left, int right) {
        int less;
        int less2;
        int sixth = ((right - left) + 1) / 6;
        int e1 = left + sixth;
        int e5 = right - sixth;
        int e3 = (left + right) >>> 1;
        int e4 = e3 + sixth;
        int e2 = e3 - sixth;
        long ae1 = a[e1];
        long ae2 = a[e2];
        long ae3 = a[e3];
        long ae4 = a[e4];
        long ae5 = a[e5];
        if (ae1 > ae2) {
            ae1 = ae2;
            ae2 = ae1;
        }
        if (ae4 > ae5) {
            ae4 = ae5;
            ae5 = ae4;
        }
        if (ae1 > ae3) {
            long t = ae1;
            ae1 = ae3;
            ae3 = t;
        }
        if (ae2 > ae3) {
            long t2 = ae2;
            ae2 = ae3;
            ae3 = t2;
        }
        if (ae1 > ae4) {
            long t3 = ae1;
            ae1 = ae4;
            ae4 = t3;
        }
        if (ae3 > ae4) {
            long t4 = ae3;
            ae3 = ae4;
            ae4 = t4;
        }
        if (ae2 > ae5) {
            long t5 = ae2;
            ae2 = ae5;
            ae5 = t5;
        }
        if (ae2 > ae3) {
            long t6 = ae2;
            ae2 = ae3;
            ae3 = t6;
        }
        if (ae4 > ae5) {
            long t7 = ae4;
            ae4 = ae5;
            ae5 = t7;
        }
        a[e1] = ae1;
        a[e3] = ae3;
        a[e5] = ae5;
        long pivot1 = ae2;
        a[e2] = a[left];
        long pivot2 = ae4;
        a[e4] = a[right];
        int less3 = left + 1;
        int great = right - 1;
        boolean pivotsDiffer = pivot1 != pivot2;
        if (pivotsDiffer) {
            int k = less3;
            loop0: while (true) {
                int less4 = less3;
                if (k > great) {
                    less2 = less4;
                    break;
                }
                long ak = a[k];
                if (ak < pivot1) {
                    if (k != less4) {
                        a[k] = a[less4];
                        a[less4] = ak;
                    }
                    less3 = less4 + 1;
                } else if (ak > pivot2) {
                    while (a[great] > pivot2) {
                        int great2 = great - 1;
                        if (great == k) {
                            great = great2;
                            less2 = less4;
                            break loop0;
                        }
                        great = great2;
                    }
                    if (a[great] < pivot1) {
                        a[k] = a[less4];
                        less3 = less4 + 1;
                        a[less4] = a[great];
                        a[great] = ak;
                        great--;
                    } else {
                        a[k] = a[great];
                        a[great] = ak;
                        great--;
                        less3 = less4;
                    }
                } else {
                    less3 = less4;
                }
                k++;
            }
        } else {
            int k2 = less3;
            while (true) {
                less = less3;
                if (k2 > great) {
                    break;
                }
                long ak2 = a[k2];
                if (ak2 == pivot1) {
                    less3 = less;
                } else if (ak2 < pivot1) {
                    if (k2 != less) {
                        a[k2] = a[less];
                        a[less] = ak2;
                    }
                    less3 = less + 1;
                } else {
                    while (a[great] > pivot1) {
                        great--;
                    }
                    if (a[great] < pivot1) {
                        a[k2] = a[less];
                        less3 = less + 1;
                        a[less] = a[great];
                        a[great] = ak2;
                        great--;
                    } else {
                        a[k2] = pivot1;
                        a[great] = ak2;
                        great--;
                        less3 = less;
                    }
                }
                k2++;
            }
            less2 = less;
        }
        a[left] = a[less2 - 1];
        a[less2 - 1] = pivot1;
        a[right] = a[great + 1];
        a[great + 1] = pivot2;
        doSort(a, left, less2 - 2);
        doSort(a, great + 2, right);
        if (pivotsDiffer) {
            if (less2 < e1 && great > e5) {
                while (a[less2] == pivot1) {
                    less2++;
                }
                while (a[great] == pivot2) {
                    great--;
                }
                int k3 = less2;
                loop4: while (true) {
                    int less5 = less2;
                    if (k3 > great) {
                        less2 = less5;
                        break;
                    }
                    long ak3 = a[k3];
                    if (ak3 == pivot2) {
                        while (a[great] == pivot2) {
                            int great3 = great - 1;
                            if (great == k3) {
                                great = great3;
                                less2 = less5;
                                break loop4;
                            }
                            great = great3;
                        }
                        if (a[great] == pivot1) {
                            a[k3] = a[less5];
                            less2 = less5 + 1;
                            a[less5] = pivot1;
                        } else {
                            a[k3] = a[great];
                            less2 = less5;
                        }
                        a[great] = pivot2;
                        great--;
                    } else if (ak3 == pivot1) {
                        a[k3] = a[less5];
                        less2 = less5 + 1;
                        a[less5] = pivot1;
                    } else {
                        less2 = less5;
                    }
                    k3++;
                }
            }
            doSort(a, less2, great);
        }
    }

    public static void sort(short[] a) {
        doSort(a, 0, a.length - 1);
    }

    public static void sort(short[] a, int fromIndex, int toIndex) {
        Arrays.checkStartAndEnd(a.length, fromIndex, toIndex);
        doSort(a, fromIndex, toIndex - 1);
    }

    private static void doSort(short[] a, int left, int right) {
        if ((right - left) + 1 < 32) {
            for (int i = left + 1; i <= right; i++) {
                short ai = a[i];
                int j = i - 1;
                while (j >= left && ai < a[j]) {
                    a[j + 1] = a[j];
                    j--;
                }
                a[j + 1] = ai;
            }
            return;
        }
        if ((right - left) + 1 > 32768) {
            int[] count = new int[65536];
            for (int i2 = left; i2 <= right; i2++) {
                int i3 = a[i2] - Short.MIN_VALUE;
                count[i3] = count[i3] + 1;
            }
            int i4 = 0;
            int k = left;
            while (i4 < count.length && k <= right) {
                short value = (short) (i4 - 32768);
                int s = count[i4];
                int k2 = k;
                while (s > 0) {
                    a[k2] = value;
                    s--;
                    k2++;
                }
                i4++;
                k = k2;
            }
            return;
        }
        dualPivotQuicksort(a, left, right);
    }

    private static void dualPivotQuicksort(short[] a, int left, int right) {
        int less;
        int less2;
        int sixth = ((right - left) + 1) / 6;
        int e1 = left + sixth;
        int e5 = right - sixth;
        int e3 = (left + right) >>> 1;
        int e4 = e3 + sixth;
        int e2 = e3 - sixth;
        short ae1 = a[e1];
        short ae2 = a[e2];
        short ae3 = a[e3];
        short ae4 = a[e4];
        short ae5 = a[e5];
        if (ae1 > ae2) {
            ae1 = ae2;
            ae2 = ae1;
        }
        if (ae4 > ae5) {
            ae4 = ae5;
            ae5 = ae4;
        }
        if (ae1 > ae3) {
            short t = ae1;
            ae1 = ae3;
            ae3 = t;
        }
        if (ae2 > ae3) {
            short t2 = ae2;
            ae2 = ae3;
            ae3 = t2;
        }
        if (ae1 > ae4) {
            short t3 = ae1;
            ae1 = ae4;
            ae4 = t3;
        }
        if (ae3 > ae4) {
            short t4 = ae3;
            ae3 = ae4;
            ae4 = t4;
        }
        if (ae2 > ae5) {
            short t5 = ae2;
            ae2 = ae5;
            ae5 = t5;
        }
        if (ae2 > ae3) {
            short t6 = ae2;
            ae2 = ae3;
            ae3 = t6;
        }
        if (ae4 > ae5) {
            short t7 = ae4;
            ae4 = ae5;
            ae5 = t7;
        }
        a[e1] = ae1;
        a[e3] = ae3;
        a[e5] = ae5;
        short pivot1 = ae2;
        a[e2] = a[left];
        short pivot2 = ae4;
        a[e4] = a[right];
        int less3 = left + 1;
        int great = right - 1;
        boolean pivotsDiffer = pivot1 != pivot2;
        if (pivotsDiffer) {
            int k = less3;
            loop0: while (true) {
                int less4 = less3;
                if (k > great) {
                    less2 = less4;
                    break;
                }
                short ak = a[k];
                if (ak < pivot1) {
                    if (k != less4) {
                        a[k] = a[less4];
                        a[less4] = ak;
                    }
                    less3 = less4 + 1;
                } else if (ak > pivot2) {
                    while (a[great] > pivot2) {
                        int great2 = great - 1;
                        if (great == k) {
                            great = great2;
                            less2 = less4;
                            break loop0;
                        }
                        great = great2;
                    }
                    if (a[great] < pivot1) {
                        a[k] = a[less4];
                        less3 = less4 + 1;
                        a[less4] = a[great];
                        a[great] = ak;
                        great--;
                    } else {
                        a[k] = a[great];
                        a[great] = ak;
                        great--;
                        less3 = less4;
                    }
                } else {
                    less3 = less4;
                }
                k++;
            }
        } else {
            int k2 = less3;
            while (true) {
                less = less3;
                if (k2 > great) {
                    break;
                }
                short ak2 = a[k2];
                if (ak2 == pivot1) {
                    less3 = less;
                } else if (ak2 < pivot1) {
                    if (k2 != less) {
                        a[k2] = a[less];
                        a[less] = ak2;
                    }
                    less3 = less + 1;
                } else {
                    while (a[great] > pivot1) {
                        great--;
                    }
                    if (a[great] < pivot1) {
                        a[k2] = a[less];
                        less3 = less + 1;
                        a[less] = a[great];
                        a[great] = ak2;
                        great--;
                    } else {
                        a[k2] = pivot1;
                        a[great] = ak2;
                        great--;
                        less3 = less;
                    }
                }
                k2++;
            }
            less2 = less;
        }
        a[left] = a[less2 - 1];
        a[less2 - 1] = pivot1;
        a[right] = a[great + 1];
        a[great + 1] = pivot2;
        doSort(a, left, less2 - 2);
        doSort(a, great + 2, right);
        if (pivotsDiffer) {
            if (less2 < e1 && great > e5) {
                while (a[less2] == pivot1) {
                    less2++;
                }
                while (a[great] == pivot2) {
                    great--;
                }
                int k3 = less2;
                loop4: while (true) {
                    int less5 = less2;
                    if (k3 > great) {
                        less2 = less5;
                        break;
                    }
                    short ak3 = a[k3];
                    if (ak3 == pivot2) {
                        while (a[great] == pivot2) {
                            int great3 = great - 1;
                            if (great == k3) {
                                great = great3;
                                less2 = less5;
                                break loop4;
                            }
                            great = great3;
                        }
                        if (a[great] == pivot1) {
                            a[k3] = a[less5];
                            less2 = less5 + 1;
                            a[less5] = pivot1;
                        } else {
                            a[k3] = a[great];
                            less2 = less5;
                        }
                        a[great] = pivot2;
                        great--;
                    } else if (ak3 == pivot1) {
                        a[k3] = a[less5];
                        less2 = less5 + 1;
                        a[less5] = pivot1;
                    } else {
                        less2 = less5;
                    }
                    k3++;
                }
            }
            doSort(a, less2, great);
        }
    }

    public static void sort(char[] a) {
        doSort(a, 0, a.length - 1);
    }

    public static void sort(char[] a, int fromIndex, int toIndex) {
        Arrays.checkStartAndEnd(a.length, fromIndex, toIndex);
        doSort(a, fromIndex, toIndex - 1);
    }

    private static void doSort(char[] a, int left, int right) {
        if ((right - left) + 1 < 32) {
            for (int i = left + 1; i <= right; i++) {
                char ai = a[i];
                int j = i - 1;
                while (j >= left && ai < a[j]) {
                    a[j + 1] = a[j];
                    j--;
                }
                a[j + 1] = ai;
            }
            return;
        }
        if ((right - left) + 1 > 32768) {
            int[] count = new int[65536];
            for (int i2 = left; i2 <= right; i2++) {
                char c = a[i2];
                count[c] = count[c] + 1;
            }
            int i3 = 0;
            int k = left;
            while (i3 < count.length && k <= right) {
                int s = count[i3];
                int k2 = k;
                while (s > 0) {
                    a[k2] = (char) i3;
                    s--;
                    k2++;
                }
                i3++;
                k = k2;
            }
            return;
        }
        dualPivotQuicksort(a, left, right);
    }

    private static void dualPivotQuicksort(char[] a, int left, int right) {
        int less;
        int less2;
        int sixth = ((right - left) + 1) / 6;
        int e1 = left + sixth;
        int e5 = right - sixth;
        int e3 = (left + right) >>> 1;
        int e4 = e3 + sixth;
        int e2 = e3 - sixth;
        char ae1 = a[e1];
        char ae2 = a[e2];
        char ae3 = a[e3];
        char ae4 = a[e4];
        char ae5 = a[e5];
        if (ae1 > ae2) {
            ae1 = ae2;
            ae2 = ae1;
        }
        if (ae4 > ae5) {
            ae4 = ae5;
            ae5 = ae4;
        }
        if (ae1 > ae3) {
            char t = ae1;
            ae1 = ae3;
            ae3 = t;
        }
        if (ae2 > ae3) {
            char t2 = ae2;
            ae2 = ae3;
            ae3 = t2;
        }
        if (ae1 > ae4) {
            char t3 = ae1;
            ae1 = ae4;
            ae4 = t3;
        }
        if (ae3 > ae4) {
            char t4 = ae3;
            ae3 = ae4;
            ae4 = t4;
        }
        if (ae2 > ae5) {
            char t5 = ae2;
            ae2 = ae5;
            ae5 = t5;
        }
        if (ae2 > ae3) {
            char t6 = ae2;
            ae2 = ae3;
            ae3 = t6;
        }
        if (ae4 > ae5) {
            char t7 = ae4;
            ae4 = ae5;
            ae5 = t7;
        }
        a[e1] = ae1;
        a[e3] = ae3;
        a[e5] = ae5;
        char pivot1 = ae2;
        a[e2] = a[left];
        char pivot2 = ae4;
        a[e4] = a[right];
        int less3 = left + 1;
        int great = right - 1;
        boolean pivotsDiffer = pivot1 != pivot2;
        if (pivotsDiffer) {
            int k = less3;
            loop0: while (true) {
                int less4 = less3;
                if (k > great) {
                    less2 = less4;
                    break;
                }
                char ak = a[k];
                if (ak < pivot1) {
                    if (k != less4) {
                        a[k] = a[less4];
                        a[less4] = ak;
                    }
                    less3 = less4 + 1;
                } else if (ak > pivot2) {
                    while (a[great] > pivot2) {
                        int great2 = great - 1;
                        if (great == k) {
                            great = great2;
                            less2 = less4;
                            break loop0;
                        }
                        great = great2;
                    }
                    if (a[great] < pivot1) {
                        a[k] = a[less4];
                        less3 = less4 + 1;
                        a[less4] = a[great];
                        a[great] = ak;
                        great--;
                    } else {
                        a[k] = a[great];
                        a[great] = ak;
                        great--;
                        less3 = less4;
                    }
                } else {
                    less3 = less4;
                }
                k++;
            }
        } else {
            int k2 = less3;
            while (true) {
                less = less3;
                if (k2 > great) {
                    break;
                }
                char ak2 = a[k2];
                if (ak2 == pivot1) {
                    less3 = less;
                } else if (ak2 < pivot1) {
                    if (k2 != less) {
                        a[k2] = a[less];
                        a[less] = ak2;
                    }
                    less3 = less + 1;
                } else {
                    while (a[great] > pivot1) {
                        great--;
                    }
                    if (a[great] < pivot1) {
                        a[k2] = a[less];
                        less3 = less + 1;
                        a[less] = a[great];
                        a[great] = ak2;
                        great--;
                    } else {
                        a[k2] = pivot1;
                        a[great] = ak2;
                        great--;
                        less3 = less;
                    }
                }
                k2++;
            }
            less2 = less;
        }
        a[left] = a[less2 - 1];
        a[less2 - 1] = pivot1;
        a[right] = a[great + 1];
        a[great + 1] = pivot2;
        doSort(a, left, less2 - 2);
        doSort(a, great + 2, right);
        if (pivotsDiffer) {
            if (less2 < e1 && great > e5) {
                while (a[less2] == pivot1) {
                    less2++;
                }
                while (a[great] == pivot2) {
                    great--;
                }
                int k3 = less2;
                loop4: while (true) {
                    int less5 = less2;
                    if (k3 > great) {
                        less2 = less5;
                        break;
                    }
                    char ak3 = a[k3];
                    if (ak3 == pivot2) {
                        while (a[great] == pivot2) {
                            int great3 = great - 1;
                            if (great == k3) {
                                great = great3;
                                less2 = less5;
                                break loop4;
                            }
                            great = great3;
                        }
                        if (a[great] == pivot1) {
                            a[k3] = a[less5];
                            less2 = less5 + 1;
                            a[less5] = pivot1;
                        } else {
                            a[k3] = a[great];
                            less2 = less5;
                        }
                        a[great] = pivot2;
                        great--;
                    } else if (ak3 == pivot1) {
                        a[k3] = a[less5];
                        less2 = less5 + 1;
                        a[less5] = pivot1;
                    } else {
                        less2 = less5;
                    }
                    k3++;
                }
            }
            doSort(a, less2, great);
        }
    }

    public static void sort(byte[] a) {
        doSort(a, 0, a.length - 1);
    }

    public static void sort(byte[] a, int fromIndex, int toIndex) {
        Arrays.checkStartAndEnd(a.length, fromIndex, toIndex);
        doSort(a, fromIndex, toIndex - 1);
    }

    private static void doSort(byte[] a, int left, int right) {
        if ((right - left) + 1 < 32) {
            for (int i = left + 1; i <= right; i++) {
                byte ai = a[i];
                int j = i - 1;
                while (j >= left && ai < a[j]) {
                    a[j + 1] = a[j];
                    j--;
                }
                a[j + 1] = ai;
            }
            return;
        }
        if ((right - left) + 1 > 128) {
            int[] count = new int[256];
            for (int i2 = left; i2 <= right; i2++) {
                int i3 = a[i2] + Byte.MIN_VALUE;
                count[i3] = count[i3] + 1;
            }
            int i4 = 0;
            int k = left;
            while (i4 < count.length && k <= right) {
                byte value = (byte) (i4 - 128);
                int s = count[i4];
                int k2 = k;
                while (s > 0) {
                    a[k2] = value;
                    s--;
                    k2++;
                }
                i4++;
                k = k2;
            }
            return;
        }
        dualPivotQuicksort(a, left, right);
    }

    private static void dualPivotQuicksort(byte[] a, int left, int right) {
        int less;
        int less2;
        int sixth = ((right - left) + 1) / 6;
        int e1 = left + sixth;
        int e5 = right - sixth;
        int e3 = (left + right) >>> 1;
        int e4 = e3 + sixth;
        int e2 = e3 - sixth;
        byte ae1 = a[e1];
        byte ae2 = a[e2];
        byte ae3 = a[e3];
        byte ae4 = a[e4];
        byte ae5 = a[e5];
        if (ae1 > ae2) {
            ae1 = ae2;
            ae2 = ae1;
        }
        if (ae4 > ae5) {
            ae4 = ae5;
            ae5 = ae4;
        }
        if (ae1 > ae3) {
            byte t = ae1;
            ae1 = ae3;
            ae3 = t;
        }
        if (ae2 > ae3) {
            byte t2 = ae2;
            ae2 = ae3;
            ae3 = t2;
        }
        if (ae1 > ae4) {
            byte t3 = ae1;
            ae1 = ae4;
            ae4 = t3;
        }
        if (ae3 > ae4) {
            byte t4 = ae3;
            ae3 = ae4;
            ae4 = t4;
        }
        if (ae2 > ae5) {
            byte t5 = ae2;
            ae2 = ae5;
            ae5 = t5;
        }
        if (ae2 > ae3) {
            byte t6 = ae2;
            ae2 = ae3;
            ae3 = t6;
        }
        if (ae4 > ae5) {
            byte t7 = ae4;
            ae4 = ae5;
            ae5 = t7;
        }
        a[e1] = ae1;
        a[e3] = ae3;
        a[e5] = ae5;
        byte pivot1 = ae2;
        a[e2] = a[left];
        byte pivot2 = ae4;
        a[e4] = a[right];
        int less3 = left + 1;
        int great = right - 1;
        boolean pivotsDiffer = pivot1 != pivot2;
        if (pivotsDiffer) {
            int k = less3;
            loop0: while (true) {
                int less4 = less3;
                if (k > great) {
                    less2 = less4;
                    break;
                }
                byte ak = a[k];
                if (ak < pivot1) {
                    if (k != less4) {
                        a[k] = a[less4];
                        a[less4] = ak;
                    }
                    less3 = less4 + 1;
                } else if (ak > pivot2) {
                    while (a[great] > pivot2) {
                        int great2 = great - 1;
                        if (great == k) {
                            great = great2;
                            less2 = less4;
                            break loop0;
                        }
                        great = great2;
                    }
                    if (a[great] < pivot1) {
                        a[k] = a[less4];
                        less3 = less4 + 1;
                        a[less4] = a[great];
                        a[great] = ak;
                        great--;
                    } else {
                        a[k] = a[great];
                        a[great] = ak;
                        great--;
                        less3 = less4;
                    }
                } else {
                    less3 = less4;
                }
                k++;
            }
        } else {
            int k2 = less3;
            while (true) {
                less = less3;
                if (k2 > great) {
                    break;
                }
                byte ak2 = a[k2];
                if (ak2 == pivot1) {
                    less3 = less;
                } else if (ak2 < pivot1) {
                    if (k2 != less) {
                        a[k2] = a[less];
                        a[less] = ak2;
                    }
                    less3 = less + 1;
                } else {
                    while (a[great] > pivot1) {
                        great--;
                    }
                    if (a[great] < pivot1) {
                        a[k2] = a[less];
                        less3 = less + 1;
                        a[less] = a[great];
                        a[great] = ak2;
                        great--;
                    } else {
                        a[k2] = pivot1;
                        a[great] = ak2;
                        great--;
                        less3 = less;
                    }
                }
                k2++;
            }
            less2 = less;
        }
        a[left] = a[less2 - 1];
        a[less2 - 1] = pivot1;
        a[right] = a[great + 1];
        a[great + 1] = pivot2;
        doSort(a, left, less2 - 2);
        doSort(a, great + 2, right);
        if (pivotsDiffer) {
            if (less2 < e1 && great > e5) {
                while (a[less2] == pivot1) {
                    less2++;
                }
                while (a[great] == pivot2) {
                    great--;
                }
                int k3 = less2;
                loop4: while (true) {
                    int less5 = less2;
                    if (k3 > great) {
                        less2 = less5;
                        break;
                    }
                    byte ak3 = a[k3];
                    if (ak3 == pivot2) {
                        while (a[great] == pivot2) {
                            int great3 = great - 1;
                            if (great == k3) {
                                great = great3;
                                less2 = less5;
                                break loop4;
                            }
                            great = great3;
                        }
                        if (a[great] == pivot1) {
                            a[k3] = a[less5];
                            less2 = less5 + 1;
                            a[less5] = pivot1;
                        } else {
                            a[k3] = a[great];
                            less2 = less5;
                        }
                        a[great] = pivot2;
                        great--;
                    } else if (ak3 == pivot1) {
                        a[k3] = a[less5];
                        less2 = less5 + 1;
                        a[less5] = pivot1;
                    } else {
                        less2 = less5;
                    }
                    k3++;
                }
            }
            doSort(a, less2, great);
        }
    }

    public static void sort(float[] a) {
        sortNegZeroAndNaN(a, 0, a.length - 1);
    }

    public static void sort(float[] a, int fromIndex, int toIndex) {
        Arrays.checkStartAndEnd(a.length, fromIndex, toIndex);
        sortNegZeroAndNaN(a, fromIndex, toIndex - 1);
    }

    private static void sortNegZeroAndNaN(float[] a, int left, int right) {
        int k;
        int n;
        int NEGATIVE_ZERO = Float.floatToIntBits(-0.0f);
        int numNegativeZeros = 0;
        int k2 = left;
        int n2 = right;
        while (k2 <= n2) {
            float ak = a[k2];
            if (ak == 0.0f && NEGATIVE_ZERO == Float.floatToRawIntBits(ak)) {
                a[k2] = 0.0f;
                numNegativeZeros++;
                k = k2;
                n = n2;
            } else if (ak != ak) {
                k = k2 - 1;
                a[k2] = a[n2];
                n = n2 - 1;
                a[n2] = Float.NaN;
            } else {
                k = k2;
                n = n2;
            }
            k2 = k + 1;
            n2 = n;
        }
        doSort(a, left, n2);
        if (numNegativeZeros != 0) {
            int zeroIndex = findAnyZero(a, left, n2);
            for (int i = zeroIndex - 1; i >= left && a[i] == 0.0f; i--) {
                zeroIndex = i;
            }
            int m = zeroIndex + numNegativeZeros;
            for (int i2 = zeroIndex; i2 < m; i2++) {
                a[i2] = -0.0f;
            }
        }
    }

    private static int findAnyZero(float[] a, int low, int high) {
        while (true) {
            int middle = (low + high) >>> 1;
            float middleValue = a[middle];
            if (middleValue < 0.0f) {
                low = middle + 1;
            } else if (middleValue > 0.0f) {
                high = middle - 1;
            } else {
                return middle;
            }
        }
    }

    private static void doSort(float[] a, int left, int right) {
        if ((right - left) + 1 < 32) {
            for (int i = left + 1; i <= right; i++) {
                float ai = a[i];
                int j = i - 1;
                while (j >= left && ai < a[j]) {
                    a[j + 1] = a[j];
                    j--;
                }
                a[j + 1] = ai;
            }
            return;
        }
        dualPivotQuicksort(a, left, right);
    }

    private static void dualPivotQuicksort(float[] a, int left, int right) {
        int less;
        int less2;
        int sixth = ((right - left) + 1) / 6;
        int e1 = left + sixth;
        int e5 = right - sixth;
        int e3 = (left + right) >>> 1;
        int e4 = e3 + sixth;
        int e2 = e3 - sixth;
        float ae1 = a[e1];
        float ae2 = a[e2];
        float ae3 = a[e3];
        float ae4 = a[e4];
        float ae5 = a[e5];
        if (ae1 > ae2) {
            ae1 = ae2;
            ae2 = ae1;
        }
        if (ae4 > ae5) {
            ae4 = ae5;
            ae5 = ae4;
        }
        if (ae1 > ae3) {
            float t = ae1;
            ae1 = ae3;
            ae3 = t;
        }
        if (ae2 > ae3) {
            float t2 = ae2;
            ae2 = ae3;
            ae3 = t2;
        }
        if (ae1 > ae4) {
            float t3 = ae1;
            ae1 = ae4;
            ae4 = t3;
        }
        if (ae3 > ae4) {
            float t4 = ae3;
            ae3 = ae4;
            ae4 = t4;
        }
        if (ae2 > ae5) {
            float t5 = ae2;
            ae2 = ae5;
            ae5 = t5;
        }
        if (ae2 > ae3) {
            float t6 = ae2;
            ae2 = ae3;
            ae3 = t6;
        }
        if (ae4 > ae5) {
            float t7 = ae4;
            ae4 = ae5;
            ae5 = t7;
        }
        a[e1] = ae1;
        a[e3] = ae3;
        a[e5] = ae5;
        float pivot1 = ae2;
        a[e2] = a[left];
        float pivot2 = ae4;
        a[e4] = a[right];
        int less3 = left + 1;
        int great = right - 1;
        boolean pivotsDiffer = pivot1 != pivot2;
        if (pivotsDiffer) {
            int k = less3;
            loop0: while (true) {
                int less4 = less3;
                if (k > great) {
                    less2 = less4;
                    break;
                }
                float ak = a[k];
                if (ak < pivot1) {
                    if (k != less4) {
                        a[k] = a[less4];
                        a[less4] = ak;
                    }
                    less3 = less4 + 1;
                } else if (ak > pivot2) {
                    while (a[great] > pivot2) {
                        int great2 = great - 1;
                        if (great == k) {
                            great = great2;
                            less2 = less4;
                            break loop0;
                        }
                        great = great2;
                    }
                    if (a[great] < pivot1) {
                        a[k] = a[less4];
                        less3 = less4 + 1;
                        a[less4] = a[great];
                        a[great] = ak;
                        great--;
                    } else {
                        a[k] = a[great];
                        a[great] = ak;
                        great--;
                        less3 = less4;
                    }
                } else {
                    less3 = less4;
                }
                k++;
            }
        } else {
            int k2 = less3;
            while (true) {
                less = less3;
                if (k2 > great) {
                    break;
                }
                float ak2 = a[k2];
                if (ak2 == pivot1) {
                    less3 = less;
                } else if (ak2 < pivot1) {
                    if (k2 != less) {
                        a[k2] = a[less];
                        a[less] = ak2;
                    }
                    less3 = less + 1;
                } else {
                    while (a[great] > pivot1) {
                        great--;
                    }
                    if (a[great] < pivot1) {
                        a[k2] = a[less];
                        less3 = less + 1;
                        a[less] = a[great];
                        a[great] = ak2;
                        great--;
                    } else {
                        a[k2] = pivot1;
                        a[great] = ak2;
                        great--;
                        less3 = less;
                    }
                }
                k2++;
            }
            less2 = less;
        }
        a[left] = a[less2 - 1];
        a[less2 - 1] = pivot1;
        a[right] = a[great + 1];
        a[great + 1] = pivot2;
        doSort(a, left, less2 - 2);
        doSort(a, great + 2, right);
        if (pivotsDiffer) {
            if (less2 < e1 && great > e5) {
                while (a[less2] == pivot1) {
                    less2++;
                }
                while (a[great] == pivot2) {
                    great--;
                }
                int k3 = less2;
                loop4: while (true) {
                    int less5 = less2;
                    if (k3 > great) {
                        less2 = less5;
                        break;
                    }
                    float ak3 = a[k3];
                    if (ak3 == pivot2) {
                        while (a[great] == pivot2) {
                            int great3 = great - 1;
                            if (great == k3) {
                                great = great3;
                                less2 = less5;
                                break loop4;
                            }
                            great = great3;
                        }
                        if (a[great] == pivot1) {
                            a[k3] = a[less5];
                            less2 = less5 + 1;
                            a[less5] = pivot1;
                        } else {
                            a[k3] = a[great];
                            less2 = less5;
                        }
                        a[great] = pivot2;
                        great--;
                    } else if (ak3 == pivot1) {
                        a[k3] = a[less5];
                        less2 = less5 + 1;
                        a[less5] = pivot1;
                    } else {
                        less2 = less5;
                    }
                    k3++;
                }
            }
            doSort(a, less2, great);
        }
    }

    public static void sort(double[] a) {
        sortNegZeroAndNaN(a, 0, a.length - 1);
    }

    public static void sort(double[] a, int fromIndex, int toIndex) {
        Arrays.checkStartAndEnd(a.length, fromIndex, toIndex);
        sortNegZeroAndNaN(a, fromIndex, toIndex - 1);
    }

    private static void sortNegZeroAndNaN(double[] a, int left, int right) {
        int k;
        int n;
        long NEGATIVE_ZERO = Double.doubleToLongBits(-0.0d);
        int numNegativeZeros = 0;
        int k2 = left;
        int n2 = right;
        while (k2 <= n2) {
            double ak = a[k2];
            if (ak == 0.0d && NEGATIVE_ZERO == Double.doubleToRawLongBits(ak)) {
                a[k2] = 0.0d;
                numNegativeZeros++;
                k = k2;
                n = n2;
            } else if (ak != ak) {
                k = k2 - 1;
                a[k2] = a[n2];
                n = n2 - 1;
                a[n2] = Double.NaN;
            } else {
                k = k2;
                n = n2;
            }
            k2 = k + 1;
            n2 = n;
        }
        doSort(a, left, n2);
        if (numNegativeZeros != 0) {
            int zeroIndex = findAnyZero(a, left, n2);
            for (int i = zeroIndex - 1; i >= left && a[i] == 0.0d; i--) {
                zeroIndex = i;
            }
            int m = zeroIndex + numNegativeZeros;
            for (int i2 = zeroIndex; i2 < m; i2++) {
                a[i2] = -0.0d;
            }
        }
    }

    private static int findAnyZero(double[] a, int low, int high) {
        while (true) {
            int middle = (low + high) >>> 1;
            double middleValue = a[middle];
            if (middleValue < 0.0d) {
                low = middle + 1;
            } else if (middleValue > 0.0d) {
                high = middle - 1;
            } else {
                return middle;
            }
        }
    }

    private static void doSort(double[] a, int left, int right) {
        if ((right - left) + 1 < 32) {
            for (int i = left + 1; i <= right; i++) {
                double ai = a[i];
                int j = i - 1;
                while (j >= left && ai < a[j]) {
                    a[j + 1] = a[j];
                    j--;
                }
                a[j + 1] = ai;
            }
            return;
        }
        dualPivotQuicksort(a, left, right);
    }

    private static void dualPivotQuicksort(double[] a, int left, int right) {
        int less;
        int less2;
        int sixth = ((right - left) + 1) / 6;
        int e1 = left + sixth;
        int e5 = right - sixth;
        int e3 = (left + right) >>> 1;
        int e4 = e3 + sixth;
        int e2 = e3 - sixth;
        double ae1 = a[e1];
        double ae2 = a[e2];
        double ae3 = a[e3];
        double ae4 = a[e4];
        double ae5 = a[e5];
        if (ae1 > ae2) {
            ae1 = ae2;
            ae2 = ae1;
        }
        if (ae4 > ae5) {
            ae4 = ae5;
            ae5 = ae4;
        }
        if (ae1 > ae3) {
            double t = ae1;
            ae1 = ae3;
            ae3 = t;
        }
        if (ae2 > ae3) {
            double t2 = ae2;
            ae2 = ae3;
            ae3 = t2;
        }
        if (ae1 > ae4) {
            double t3 = ae1;
            ae1 = ae4;
            ae4 = t3;
        }
        if (ae3 > ae4) {
            double t4 = ae3;
            ae3 = ae4;
            ae4 = t4;
        }
        if (ae2 > ae5) {
            double t5 = ae2;
            ae2 = ae5;
            ae5 = t5;
        }
        if (ae2 > ae3) {
            double t6 = ae2;
            ae2 = ae3;
            ae3 = t6;
        }
        if (ae4 > ae5) {
            double t7 = ae4;
            ae4 = ae5;
            ae5 = t7;
        }
        a[e1] = ae1;
        a[e3] = ae3;
        a[e5] = ae5;
        double pivot1 = ae2;
        a[e2] = a[left];
        double pivot2 = ae4;
        a[e4] = a[right];
        int less3 = left + 1;
        int great = right - 1;
        boolean pivotsDiffer = pivot1 != pivot2;
        if (pivotsDiffer) {
            int k = less3;
            loop0: while (true) {
                int less4 = less3;
                if (k > great) {
                    less2 = less4;
                    break;
                }
                double ak = a[k];
                if (ak < pivot1) {
                    if (k != less4) {
                        a[k] = a[less4];
                        a[less4] = ak;
                    }
                    less3 = less4 + 1;
                } else if (ak > pivot2) {
                    while (a[great] > pivot2) {
                        int great2 = great - 1;
                        if (great == k) {
                            great = great2;
                            less2 = less4;
                            break loop0;
                        }
                        great = great2;
                    }
                    if (a[great] < pivot1) {
                        a[k] = a[less4];
                        less3 = less4 + 1;
                        a[less4] = a[great];
                        a[great] = ak;
                        great--;
                    } else {
                        a[k] = a[great];
                        a[great] = ak;
                        great--;
                        less3 = less4;
                    }
                } else {
                    less3 = less4;
                }
                k++;
            }
        } else {
            int k2 = less3;
            while (true) {
                less = less3;
                if (k2 > great) {
                    break;
                }
                double ak2 = a[k2];
                if (ak2 == pivot1) {
                    less3 = less;
                } else if (ak2 < pivot1) {
                    if (k2 != less) {
                        a[k2] = a[less];
                        a[less] = ak2;
                    }
                    less3 = less + 1;
                } else {
                    while (a[great] > pivot1) {
                        great--;
                    }
                    if (a[great] < pivot1) {
                        a[k2] = a[less];
                        less3 = less + 1;
                        a[less] = a[great];
                        a[great] = ak2;
                        great--;
                    } else {
                        a[k2] = pivot1;
                        a[great] = ak2;
                        great--;
                        less3 = less;
                    }
                }
                k2++;
            }
            less2 = less;
        }
        a[left] = a[less2 - 1];
        a[less2 - 1] = pivot1;
        a[right] = a[great + 1];
        a[great + 1] = pivot2;
        doSort(a, left, less2 - 2);
        doSort(a, great + 2, right);
        if (pivotsDiffer) {
            if (less2 < e1 && great > e5) {
                while (a[less2] == pivot1) {
                    less2++;
                }
                while (a[great] == pivot2) {
                    great--;
                }
                int k3 = less2;
                loop4: while (true) {
                    int less5 = less2;
                    if (k3 > great) {
                        less2 = less5;
                        break;
                    }
                    double ak3 = a[k3];
                    if (ak3 == pivot2) {
                        while (a[great] == pivot2) {
                            int great3 = great - 1;
                            if (great == k3) {
                                great = great3;
                                less2 = less5;
                                break loop4;
                            }
                            great = great3;
                        }
                        if (a[great] == pivot1) {
                            a[k3] = a[less5];
                            less2 = less5 + 1;
                            a[less5] = pivot1;
                        } else {
                            a[k3] = a[great];
                            less2 = less5;
                        }
                        a[great] = pivot2;
                        great--;
                    } else if (ak3 == pivot1) {
                        a[k3] = a[less5];
                        less2 = less5 + 1;
                        a[less5] = pivot1;
                    } else {
                        less2 = less5;
                    }
                    k3++;
                }
            }
            doSort(a, less2, great);
        }
    }
}
