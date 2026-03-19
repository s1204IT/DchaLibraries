package java.util;

final class DualPivotQuicksort {
    private static final int COUNTING_SORT_THRESHOLD_FOR_BYTE = 29;
    private static final int COUNTING_SORT_THRESHOLD_FOR_SHORT_OR_CHAR = 3200;
    private static final int INSERTION_SORT_THRESHOLD = 47;
    private static final int MAX_RUN_COUNT = 67;
    private static final int MAX_RUN_LENGTH = 33;
    private static final int NUM_BYTE_VALUES = 256;
    private static final int NUM_CHAR_VALUES = 65536;
    private static final int NUM_SHORT_VALUES = 65536;
    private static final int QUICKSORT_THRESHOLD = 286;

    private DualPivotQuicksort() {
    }

    static void sort(int[] iArr, int i, int i2, int[] iArr2, int i3, int i4) {
        int i5;
        int i6;
        int i7;
        int[] iArr3 = iArr;
        int[] iArr4 = iArr2;
        int i8 = 1;
        if (i2 - i < QUICKSORT_THRESHOLD) {
            sort(iArr3, i, i2, true);
            return;
        }
        int[] iArr5 = new int[68];
        int i9 = 0;
        iArr5[0] = i;
        int i10 = i;
        int i11 = 0;
        while (i10 < i2) {
            int i12 = i10 + 1;
            if (iArr3[i10] < iArr3[i12]) {
                do {
                    i10++;
                    if (i10 > i2) {
                        break;
                    }
                } while (iArr3[i10 - 1] <= iArr3[i10]);
            } else if (iArr3[i10] > iArr3[i12]) {
                do {
                    i10++;
                    if (i10 > i2) {
                        break;
                    }
                } while (iArr3[i10 - 1] >= iArr3[i10]);
                int i13 = iArr5[i11] - 1;
                int i14 = i10;
                while (true) {
                    i13++;
                    i14--;
                    if (i13 >= i14) {
                        break;
                    }
                    int i15 = iArr3[i13];
                    iArr3[i13] = iArr3[i14];
                    iArr3[i14] = i15;
                }
            } else {
                int i16 = MAX_RUN_LENGTH;
                do {
                    i10++;
                    if (i10 <= i2 && iArr3[i10 - 1] == iArr3[i10]) {
                        i16--;
                    }
                } while (i16 != 0);
                sort(iArr3, i, i2, true);
                return;
            }
            i11++;
            if (i11 != MAX_RUN_COUNT) {
                iArr5[i11] = i10;
            } else {
                sort(iArr3, i, i2, true);
                return;
            }
        }
        int i17 = i2 + 1;
        if (iArr5[i11] == i2) {
            i11++;
            iArr5[i11] = i17;
        } else if (i11 == 1) {
            return;
        }
        int i18 = 1;
        byte b = 0;
        while (true) {
            i18 <<= 1;
            if (i18 >= i11) {
                break;
            } else {
                b = (byte) (b ^ 1);
            }
        }
        int i19 = i17 - i;
        if (iArr4 == null || i4 < i19 || i3 + i19 > iArr4.length) {
            iArr4 = new int[i19];
            i5 = 0;
        } else {
            i5 = i3;
        }
        if (b == 0) {
            System.arraycopy((Object) iArr3, i, (Object) iArr4, i5, i19);
            i7 = i5 - i;
            i6 = 0;
            int[] iArr6 = iArr4;
            iArr4 = iArr3;
            iArr3 = iArr6;
        } else {
            i6 = i5 - i;
            i7 = 0;
        }
        while (i11 > i8) {
            int i20 = i9;
            for (int i21 = 2; i21 <= i11; i21 += 2) {
                int i22 = iArr5[i21];
                int i23 = iArr5[i21 - 1];
                int i24 = iArr5[i21 - 2];
                int i25 = i23;
                int i26 = i24;
                while (i24 < i22) {
                    if (i25 >= i22 || (i26 < i23 && iArr3[i26 + i7] <= iArr3[i25 + i7])) {
                        iArr4[i24 + i6] = iArr3[i26 + i7];
                        i26++;
                    } else {
                        iArr4[i24 + i6] = iArr3[i25 + i7];
                        i25++;
                    }
                    i24++;
                }
                i20++;
                iArr5[i20] = i22;
            }
            if ((i11 & 1) != 0) {
                int i27 = iArr5[i11 - 1];
                int i28 = i17;
                while (true) {
                    i28--;
                    if (i28 < i27) {
                        break;
                    } else {
                        iArr4[i28 + i6] = iArr3[i28 + i7];
                    }
                }
                i20++;
                iArr5[i20] = i17;
            }
            i11 = i20;
            i8 = 1;
            i9 = 0;
            int[] iArr7 = iArr4;
            iArr4 = iArr3;
            iArr3 = iArr7;
            int i29 = i7;
            i7 = i6;
            i6 = i29;
        }
    }

    private static void sort(int[] iArr, int i, int i2, boolean z) {
        int i3 = (i2 - i) + 1;
        if (i3 < INSERTION_SORT_THRESHOLD) {
            if (z) {
                int i4 = i;
                while (i4 < i2) {
                    int i5 = i4 + 1;
                    int i6 = iArr[i5];
                    while (true) {
                        if (i6 < iArr[i4]) {
                            iArr[i4 + 1] = iArr[i4];
                            int i7 = i4 - 1;
                            if (i4 != i) {
                                i4 = i7;
                            } else {
                                i4 = i7;
                                break;
                            }
                        }
                    }
                    iArr[i4 + 1] = i6;
                    i4 = i5;
                }
                return;
            }
            while (i < i2) {
                i++;
                if (iArr[i] < iArr[i - 1]) {
                    while (true) {
                        int i8 = i + 1;
                        if (i8 > i2) {
                            break;
                        }
                        int i9 = iArr[i];
                        int i10 = iArr[i8];
                        if (i9 < i10) {
                            i10 = i9;
                            i9 = iArr[i8];
                        }
                        while (true) {
                            i--;
                            if (i9 >= iArr[i]) {
                                break;
                            } else {
                                iArr[i + 2] = iArr[i];
                            }
                        }
                        int i11 = i + 1;
                        iArr[i11 + 1] = i9;
                        while (true) {
                            i11--;
                            if (i10 < iArr[i11]) {
                                iArr[i11 + 1] = iArr[i11];
                            }
                        }
                        iArr[i11 + 1] = i10;
                        i = i8 + 1;
                    }
                    int i12 = iArr[i2];
                    while (true) {
                        i2--;
                        if (i12 < iArr[i2]) {
                            iArr[i2 + 1] = iArr[i2];
                        } else {
                            iArr[i2 + 1] = i12;
                            return;
                        }
                    }
                }
            }
            return;
        }
        int i13 = (i3 >> 3) + (i3 >> 6) + 1;
        int i14 = (i + i2) >>> 1;
        int i15 = i14 - i13;
        int i16 = i15 - i13;
        int i17 = i14 + i13;
        int i18 = i13 + i17;
        if (iArr[i15] < iArr[i16]) {
            int i19 = iArr[i15];
            iArr[i15] = iArr[i16];
            iArr[i16] = i19;
        }
        if (iArr[i14] < iArr[i15]) {
            int i20 = iArr[i14];
            iArr[i14] = iArr[i15];
            iArr[i15] = i20;
            if (i20 < iArr[i16]) {
                iArr[i15] = iArr[i16];
                iArr[i16] = i20;
            }
        }
        if (iArr[i17] < iArr[i14]) {
            int i21 = iArr[i17];
            iArr[i17] = iArr[i14];
            iArr[i14] = i21;
            if (i21 < iArr[i15]) {
                iArr[i14] = iArr[i15];
                iArr[i15] = i21;
                if (i21 < iArr[i16]) {
                    iArr[i15] = iArr[i16];
                    iArr[i16] = i21;
                }
            }
        }
        if (iArr[i18] < iArr[i17]) {
            int i22 = iArr[i18];
            iArr[i18] = iArr[i17];
            iArr[i17] = i22;
            if (i22 < iArr[i14]) {
                iArr[i17] = iArr[i14];
                iArr[i14] = i22;
                if (i22 < iArr[i15]) {
                    iArr[i14] = iArr[i15];
                    iArr[i15] = i22;
                    if (i22 < iArr[i16]) {
                        iArr[i15] = iArr[i16];
                        iArr[i16] = i22;
                    }
                }
            }
        }
        if (iArr[i16] != iArr[i15] && iArr[i15] != iArr[i14] && iArr[i14] != iArr[i17] && iArr[i17] != iArr[i18]) {
            int i23 = iArr[i15];
            int i24 = iArr[i17];
            iArr[i15] = iArr[i];
            iArr[i17] = iArr[i2];
            int i25 = i;
            do {
                i25++;
            } while (iArr[i25] < i23);
            int i26 = i2;
            do {
                i26--;
            } while (iArr[i26] > i24);
            int i27 = i25 - 1;
            loop9: while (true) {
                i27++;
                if (i27 > i26) {
                    break;
                }
                int i28 = iArr[i27];
                if (i28 < i23) {
                    iArr[i27] = iArr[i25];
                    iArr[i25] = i28;
                    i25++;
                } else if (i28 > i24) {
                    while (iArr[i26] > i24) {
                        int i29 = i26 - 1;
                        if (i26 != i27) {
                            i26 = i29;
                        } else {
                            i26 = i29;
                            break loop9;
                        }
                    }
                    if (iArr[i26] < i23) {
                        iArr[i27] = iArr[i25];
                        iArr[i25] = iArr[i26];
                        i25++;
                    } else {
                        iArr[i27] = iArr[i26];
                    }
                    iArr[i26] = i28;
                    i26--;
                } else {
                    continue;
                }
            }
            int i30 = i25 - 1;
            iArr[i] = iArr[i30];
            iArr[i30] = i23;
            int i31 = i26 + 1;
            iArr[i2] = iArr[i31];
            iArr[i31] = i24;
            sort(iArr, i, i25 - 2, z);
            sort(iArr, i26 + 2, i2, false);
            if (i25 < i16 && i18 < i26) {
                while (iArr[i25] == i23) {
                    i25++;
                }
                while (iArr[i26] == i24) {
                    i26--;
                }
                int i32 = i25 - 1;
                loop13: while (true) {
                    i32++;
                    if (i32 > i26) {
                        break;
                    }
                    int i33 = iArr[i32];
                    if (i33 == i23) {
                        iArr[i32] = iArr[i25];
                        iArr[i25] = i33;
                        i25++;
                    } else if (i33 == i24) {
                        while (iArr[i26] == i24) {
                            int i34 = i26 - 1;
                            if (i26 != i32) {
                                i26 = i34;
                            } else {
                                i26 = i34;
                                break loop13;
                            }
                        }
                        if (iArr[i26] == i23) {
                            iArr[i32] = iArr[i25];
                            iArr[i25] = i23;
                            i25++;
                        } else {
                            iArr[i32] = iArr[i26];
                        }
                        iArr[i26] = i33;
                        i26--;
                    } else {
                        continue;
                    }
                }
            }
            sort(iArr, i25, i26, false);
            return;
        }
        int i35 = iArr[i14];
        int i36 = i;
        int i37 = i36;
        int i38 = i2;
        while (i36 <= i38) {
            if (iArr[i36] != i35) {
                int i39 = iArr[i36];
                if (i39 < i35) {
                    iArr[i36] = iArr[i37];
                    iArr[i37] = i39;
                    i37++;
                } else {
                    while (iArr[i38] > i35) {
                        i38--;
                    }
                    if (iArr[i38] < i35) {
                        iArr[i36] = iArr[i37];
                        iArr[i37] = iArr[i38];
                        i37++;
                    } else {
                        iArr[i36] = i35;
                    }
                    iArr[i38] = i39;
                    i38--;
                }
            }
            i36++;
        }
        sort(iArr, i, i37 - 1, z);
        sort(iArr, i38 + 1, i2, false);
    }

    static void sort(long[] jArr, int i, int i2, long[] jArr2, int i3, int i4) {
        int i5;
        int i6;
        int i7;
        long[] jArr3 = jArr;
        long[] jArr4 = jArr2;
        if (i2 - i < QUICKSORT_THRESHOLD) {
            sort(jArr3, i, i2, true);
            return;
        }
        int[] iArr = new int[68];
        iArr[0] = i;
        int i8 = i;
        int i9 = 0;
        while (i8 < i2) {
            int i10 = i8 + 1;
            if (jArr3[i8] < jArr3[i10]) {
                do {
                    i8++;
                    if (i8 > i2) {
                        break;
                    }
                } while (jArr3[i8 - 1] <= jArr3[i8]);
            } else if (jArr3[i8] > jArr3[i10]) {
                do {
                    i8++;
                    if (i8 > i2) {
                        break;
                    }
                } while (jArr3[i8 - 1] >= jArr3[i8]);
                int i11 = iArr[i9] - 1;
                int i12 = i8;
                while (true) {
                    i11++;
                    i12--;
                    if (i11 >= i12) {
                        break;
                    }
                    long j = jArr3[i11];
                    jArr3[i11] = jArr3[i12];
                    jArr3[i12] = j;
                }
            } else {
                int i13 = MAX_RUN_LENGTH;
                do {
                    i8++;
                    if (i8 <= i2 && jArr3[i8 - 1] == jArr3[i8]) {
                        i13--;
                    }
                } while (i13 != 0);
                sort(jArr3, i, i2, true);
                return;
            }
            i9++;
            if (i9 != MAX_RUN_COUNT) {
                iArr[i9] = i8;
            } else {
                sort(jArr3, i, i2, true);
                return;
            }
        }
        int i14 = i2 + 1;
        if (iArr[i9] == i2) {
            i9++;
            iArr[i9] = i14;
        } else if (i9 == 1) {
            return;
        }
        int i15 = 1;
        byte b = 0;
        while (true) {
            i15 <<= 1;
            if (i15 >= i9) {
                break;
            } else {
                b = (byte) (b ^ 1);
            }
        }
        int i16 = i14 - i;
        if (jArr4 == null || i4 < i16 || i3 + i16 > jArr4.length) {
            jArr4 = new long[i16];
            i5 = 0;
        } else {
            i5 = i3;
        }
        if (b == 0) {
            System.arraycopy((Object) jArr3, i, (Object) jArr4, i5, i16);
            i7 = i5 - i;
            i6 = 0;
            long[] jArr5 = jArr4;
            jArr4 = jArr3;
            jArr3 = jArr5;
        } else {
            i6 = i5 - i;
            i7 = 0;
        }
        while (i9 > 1) {
            int i17 = 0;
            for (int i18 = 2; i18 <= i9; i18 += 2) {
                int i19 = iArr[i18];
                int i20 = iArr[i18 - 1];
                int i21 = iArr[i18 - 2];
                int i22 = i20;
                int i23 = i21;
                while (i21 < i19) {
                    if (i22 >= i19 || (i23 < i20 && jArr3[i23 + i7] <= jArr3[i22 + i7])) {
                        jArr4[i21 + i6] = jArr3[i23 + i7];
                        i23++;
                    } else {
                        jArr4[i21 + i6] = jArr3[i22 + i7];
                        i22++;
                    }
                    i21++;
                }
                i17++;
                iArr[i17] = i19;
            }
            if ((i9 & 1) != 0) {
                int i24 = iArr[i9 - 1];
                int i25 = i14;
                while (true) {
                    i25--;
                    if (i25 < i24) {
                        break;
                    } else {
                        jArr4[i25 + i6] = jArr3[i25 + i7];
                    }
                }
                i17++;
                iArr[i17] = i14;
            }
            i9 = i17;
            long[] jArr6 = jArr4;
            jArr4 = jArr3;
            jArr3 = jArr6;
            int i26 = i7;
            i7 = i6;
            i6 = i26;
        }
    }

    private static void sort(long[] jArr, int i, int i2, boolean z) {
        int i3 = i;
        int i4 = i2;
        int i5 = (i4 - i3) + 1;
        if (i5 < INSERTION_SORT_THRESHOLD) {
            if (z) {
                int i6 = i3;
                while (i6 < i4) {
                    int i7 = i6 + 1;
                    long j = jArr[i7];
                    while (true) {
                        if (j < jArr[i6]) {
                            jArr[i6 + 1] = jArr[i6];
                            int i8 = i6 - 1;
                            if (i6 != i3) {
                                i6 = i8;
                            } else {
                                i6 = i8;
                                break;
                            }
                        }
                    }
                    jArr[i6 + 1] = j;
                    i6 = i7;
                }
                return;
            }
            while (i3 < i4) {
                i3++;
                if (jArr[i3] < jArr[i3 - 1]) {
                    while (true) {
                        int i9 = i3 + 1;
                        if (i9 > i4) {
                            break;
                        }
                        long j2 = jArr[i3];
                        long j3 = jArr[i9];
                        if (j2 < j3) {
                            j2 = jArr[i9];
                            j3 = j2;
                        }
                        while (true) {
                            i3--;
                            if (j2 >= jArr[i3]) {
                                break;
                            } else {
                                jArr[i3 + 2] = jArr[i3];
                            }
                        }
                        int i10 = i3 + 1;
                        jArr[i10 + 1] = j2;
                        while (true) {
                            i10--;
                            if (j3 < jArr[i10]) {
                                jArr[i10 + 1] = jArr[i10];
                            }
                        }
                        jArr[i10 + 1] = j3;
                        i3 = i9 + 1;
                    }
                    long j4 = jArr[i4];
                    while (true) {
                        i4--;
                        if (j4 < jArr[i4]) {
                            jArr[i4 + 1] = jArr[i4];
                        } else {
                            jArr[i4 + 1] = j4;
                            return;
                        }
                    }
                }
            }
            return;
        }
        int i11 = (i5 >> 3) + (i5 >> 6) + 1;
        int i12 = (i3 + i4) >>> 1;
        int i13 = i12 - i11;
        int i14 = i13 - i11;
        int i15 = i12 + i11;
        int i16 = i11 + i15;
        if (jArr[i13] < jArr[i14]) {
            long j5 = jArr[i13];
            jArr[i13] = jArr[i14];
            jArr[i14] = j5;
        }
        if (jArr[i12] < jArr[i13]) {
            long j6 = jArr[i12];
            jArr[i12] = jArr[i13];
            jArr[i13] = j6;
            if (j6 < jArr[i14]) {
                jArr[i13] = jArr[i14];
                jArr[i14] = j6;
            }
        }
        if (jArr[i15] < jArr[i12]) {
            long j7 = jArr[i15];
            jArr[i15] = jArr[i12];
            jArr[i12] = j7;
            if (j7 < jArr[i13]) {
                jArr[i12] = jArr[i13];
                jArr[i13] = j7;
                if (j7 < jArr[i14]) {
                    jArr[i13] = jArr[i14];
                    jArr[i14] = j7;
                }
            }
        }
        if (jArr[i16] < jArr[i15]) {
            long j8 = jArr[i16];
            jArr[i16] = jArr[i15];
            jArr[i15] = j8;
            if (j8 < jArr[i12]) {
                jArr[i15] = jArr[i12];
                jArr[i12] = j8;
                if (j8 < jArr[i13]) {
                    jArr[i12] = jArr[i13];
                    jArr[i13] = j8;
                    if (j8 < jArr[i14]) {
                        jArr[i13] = jArr[i14];
                        jArr[i14] = j8;
                    }
                }
            }
        }
        if (jArr[i14] != jArr[i13] && jArr[i13] != jArr[i12] && jArr[i12] != jArr[i15] && jArr[i15] != jArr[i16]) {
            long j9 = jArr[i13];
            long j10 = jArr[i15];
            jArr[i13] = jArr[i3];
            jArr[i15] = jArr[i4];
            int i17 = i3;
            do {
                i17++;
            } while (jArr[i17] < j9);
            int i18 = i4;
            do {
                i18--;
            } while (jArr[i18] > j10);
            int i19 = i17 - 1;
            loop9: while (true) {
                i19++;
                if (i19 > i18) {
                    break;
                }
                long j11 = jArr[i19];
                if (j11 < j9) {
                    jArr[i19] = jArr[i17];
                    jArr[i17] = j11;
                    i17++;
                } else if (j11 > j10) {
                    while (jArr[i18] > j10) {
                        int i20 = i18 - 1;
                        if (i18 != i19) {
                            i18 = i20;
                        } else {
                            i18 = i20;
                            break loop9;
                        }
                    }
                    if (jArr[i18] < j9) {
                        jArr[i19] = jArr[i17];
                        jArr[i17] = jArr[i18];
                        i17++;
                    } else {
                        jArr[i19] = jArr[i18];
                    }
                    jArr[i18] = j11;
                    i18--;
                } else {
                    continue;
                }
            }
            int i21 = i17 - 1;
            jArr[i3] = jArr[i21];
            jArr[i21] = j9;
            int i22 = i18 + 1;
            jArr[i4] = jArr[i22];
            jArr[i22] = j10;
            sort(jArr, i3, i17 - 2, z);
            sort(jArr, i18 + 2, i4, false);
            if (i17 < i14 && i16 < i18) {
                while (jArr[i17] == j9) {
                    i17++;
                }
                while (jArr[i18] == j10) {
                    i18--;
                }
                int i23 = i17 - 1;
                loop13: while (true) {
                    i23++;
                    if (i23 > i18) {
                        break;
                    }
                    long j12 = jArr[i23];
                    if (j12 == j9) {
                        jArr[i23] = jArr[i17];
                        jArr[i17] = j12;
                        i17++;
                    } else if (j12 == j10) {
                        while (jArr[i18] == j10) {
                            int i24 = i18 - 1;
                            if (i18 != i23) {
                                i18 = i24;
                            } else {
                                i18 = i24;
                                break loop13;
                            }
                        }
                        if (jArr[i18] == j9) {
                            jArr[i23] = jArr[i17];
                            jArr[i17] = j9;
                            i17++;
                        } else {
                            jArr[i23] = jArr[i18];
                        }
                        jArr[i18] = j12;
                        i18--;
                    } else {
                        continue;
                    }
                }
            }
            sort(jArr, i17, i18, false);
            return;
        }
        long j13 = jArr[i12];
        int i25 = i3;
        int i26 = i25;
        int i27 = i4;
        while (i25 <= i27) {
            if (jArr[i25] != j13) {
                long j14 = jArr[i25];
                if (j14 < j13) {
                    jArr[i25] = jArr[i26];
                    jArr[i26] = j14;
                    i26++;
                } else {
                    while (jArr[i27] > j13) {
                        i27--;
                    }
                    if (jArr[i27] < j13) {
                        jArr[i25] = jArr[i26];
                        jArr[i26] = jArr[i27];
                        i26++;
                    } else {
                        jArr[i25] = j13;
                    }
                    jArr[i27] = j14;
                    i27--;
                }
            }
            i25++;
        }
        sort(jArr, i3, i26 - 1, z);
        sort(jArr, i27 + 1, i4, false);
    }

    static void sort(short[] sArr, int i, int i2, short[] sArr2, int i3, int i4) {
        if (i2 - i > COUNTING_SORT_THRESHOLD_FOR_SHORT_OR_CHAR) {
            int i5 = 65536;
            int[] iArr = new int[65536];
            int i6 = i - 1;
            while (true) {
                i6++;
                if (i6 > i2) {
                    break;
                }
                int i7 = sArr[i6] - Short.MIN_VALUE;
                iArr[i7] = iArr[i7] + 1;
            }
            int i8 = i2 + 1;
            while (i8 > i) {
                do {
                    i5--;
                } while (iArr[i5] == 0);
                short s = (short) (i5 - 32768);
                int i9 = iArr[i5];
                do {
                    i8--;
                    sArr[i8] = s;
                    i9--;
                } while (i9 > 0);
            }
            return;
        }
        doSort(sArr, i, i2, sArr2, i3, i4);
    }

    private static void doSort(short[] sArr, int i, int i2, short[] sArr2, int i3, int i4) {
        int i5;
        int i6;
        int i7;
        short[] sArr3 = sArr;
        short[] sArr4 = sArr2;
        int i8 = 1;
        if (i2 - i < QUICKSORT_THRESHOLD) {
            sort(sArr3, i, i2, true);
            return;
        }
        int[] iArr = new int[68];
        int i9 = 0;
        iArr[0] = i;
        int i10 = i;
        int i11 = 0;
        while (i10 < i2) {
            int i12 = i10 + 1;
            if (sArr3[i10] < sArr3[i12]) {
                do {
                    i10++;
                    if (i10 > i2) {
                        break;
                    }
                } while (sArr3[i10 - 1] <= sArr3[i10]);
            } else if (sArr3[i10] > sArr3[i12]) {
                do {
                    i10++;
                    if (i10 > i2) {
                        break;
                    }
                } while (sArr3[i10 - 1] >= sArr3[i10]);
                int i13 = iArr[i11] - 1;
                int i14 = i10;
                while (true) {
                    i13++;
                    i14--;
                    if (i13 >= i14) {
                        break;
                    }
                    short s = sArr3[i13];
                    sArr3[i13] = sArr3[i14];
                    sArr3[i14] = s;
                }
            } else {
                int i15 = MAX_RUN_LENGTH;
                do {
                    i10++;
                    if (i10 <= i2 && sArr3[i10 - 1] == sArr3[i10]) {
                        i15--;
                    }
                } while (i15 != 0);
                sort(sArr3, i, i2, true);
                return;
            }
            i11++;
            if (i11 != MAX_RUN_COUNT) {
                iArr[i11] = i10;
            } else {
                sort(sArr3, i, i2, true);
                return;
            }
        }
        int i16 = i2 + 1;
        if (iArr[i11] == i2) {
            i11++;
            iArr[i11] = i16;
        } else if (i11 == 1) {
            return;
        }
        int i17 = 1;
        byte b = 0;
        while (true) {
            i17 <<= 1;
            if (i17 >= i11) {
                break;
            } else {
                b = (byte) (b ^ 1);
            }
        }
        int i18 = i16 - i;
        if (sArr4 == null || i4 < i18 || i3 + i18 > sArr4.length) {
            sArr4 = new short[i18];
            i5 = 0;
        } else {
            i5 = i3;
        }
        if (b == 0) {
            System.arraycopy((Object) sArr3, i, (Object) sArr4, i5, i18);
            i7 = i5 - i;
            i6 = 0;
            short[] sArr5 = sArr4;
            sArr4 = sArr3;
            sArr3 = sArr5;
        } else {
            i6 = i5 - i;
            i7 = 0;
        }
        while (i11 > i8) {
            int i19 = i9;
            for (int i20 = 2; i20 <= i11; i20 += 2) {
                int i21 = iArr[i20];
                int i22 = iArr[i20 - 1];
                int i23 = iArr[i20 - 2];
                int i24 = i22;
                int i25 = i23;
                while (i23 < i21) {
                    if (i24 >= i21 || (i25 < i22 && sArr3[i25 + i7] <= sArr3[i24 + i7])) {
                        sArr4[i23 + i6] = sArr3[i25 + i7];
                        i25++;
                    } else {
                        sArr4[i23 + i6] = sArr3[i24 + i7];
                        i24++;
                    }
                    i23++;
                }
                i19++;
                iArr[i19] = i21;
            }
            if ((i11 & 1) != 0) {
                int i26 = iArr[i11 - 1];
                int i27 = i16;
                while (true) {
                    i27--;
                    if (i27 < i26) {
                        break;
                    } else {
                        sArr4[i27 + i6] = sArr3[i27 + i7];
                    }
                }
                i19++;
                iArr[i19] = i16;
            }
            i11 = i19;
            i8 = 1;
            i9 = 0;
            short[] sArr6 = sArr4;
            sArr4 = sArr3;
            sArr3 = sArr6;
            int i28 = i7;
            i7 = i6;
            i6 = i28;
        }
    }

    private static void sort(short[] sArr, int i, int i2, boolean z) {
        int i3 = (i2 - i) + 1;
        if (i3 < INSERTION_SORT_THRESHOLD) {
            if (z) {
                int i4 = i;
                while (i4 < i2) {
                    int i5 = i4 + 1;
                    short s = sArr[i5];
                    while (true) {
                        if (s < sArr[i4]) {
                            sArr[i4 + 1] = sArr[i4];
                            int i6 = i4 - 1;
                            if (i4 != i) {
                                i4 = i6;
                            } else {
                                i4 = i6;
                                break;
                            }
                        }
                    }
                    sArr[i4 + 1] = s;
                    i4 = i5;
                }
                return;
            }
            while (i < i2) {
                i++;
                if (sArr[i] < sArr[i - 1]) {
                    while (true) {
                        int i7 = i + 1;
                        if (i7 > i2) {
                            break;
                        }
                        short s2 = sArr[i];
                        short s3 = sArr[i7];
                        if (s2 < s3) {
                            s3 = s2;
                            s2 = sArr[i7];
                        }
                        while (true) {
                            i--;
                            if (s2 >= sArr[i]) {
                                break;
                            } else {
                                sArr[i + 2] = sArr[i];
                            }
                        }
                        int i8 = i + 1;
                        sArr[i8 + 1] = s2;
                        while (true) {
                            i8--;
                            if (s3 < sArr[i8]) {
                                sArr[i8 + 1] = sArr[i8];
                            }
                        }
                        sArr[i8 + 1] = s3;
                        i = i7 + 1;
                    }
                    short s4 = sArr[i2];
                    while (true) {
                        i2--;
                        if (s4 < sArr[i2]) {
                            sArr[i2 + 1] = sArr[i2];
                        } else {
                            sArr[i2 + 1] = s4;
                            return;
                        }
                    }
                }
            }
            return;
        }
        int i9 = (i3 >> 3) + (i3 >> 6) + 1;
        int i10 = (i + i2) >>> 1;
        int i11 = i10 - i9;
        int i12 = i11 - i9;
        int i13 = i10 + i9;
        int i14 = i9 + i13;
        if (sArr[i11] < sArr[i12]) {
            short s5 = sArr[i11];
            sArr[i11] = sArr[i12];
            sArr[i12] = s5;
        }
        if (sArr[i10] < sArr[i11]) {
            short s6 = sArr[i10];
            sArr[i10] = sArr[i11];
            sArr[i11] = s6;
            if (s6 < sArr[i12]) {
                sArr[i11] = sArr[i12];
                sArr[i12] = s6;
            }
        }
        if (sArr[i13] < sArr[i10]) {
            short s7 = sArr[i13];
            sArr[i13] = sArr[i10];
            sArr[i10] = s7;
            if (s7 < sArr[i11]) {
                sArr[i10] = sArr[i11];
                sArr[i11] = s7;
                if (s7 < sArr[i12]) {
                    sArr[i11] = sArr[i12];
                    sArr[i12] = s7;
                }
            }
        }
        if (sArr[i14] < sArr[i13]) {
            short s8 = sArr[i14];
            sArr[i14] = sArr[i13];
            sArr[i13] = s8;
            if (s8 < sArr[i10]) {
                sArr[i13] = sArr[i10];
                sArr[i10] = s8;
                if (s8 < sArr[i11]) {
                    sArr[i10] = sArr[i11];
                    sArr[i11] = s8;
                    if (s8 < sArr[i12]) {
                        sArr[i11] = sArr[i12];
                        sArr[i12] = s8;
                    }
                }
            }
        }
        if (sArr[i12] != sArr[i11] && sArr[i11] != sArr[i10] && sArr[i10] != sArr[i13] && sArr[i13] != sArr[i14]) {
            short s9 = sArr[i11];
            short s10 = sArr[i13];
            sArr[i11] = sArr[i];
            sArr[i13] = sArr[i2];
            int i15 = i;
            do {
                i15++;
            } while (sArr[i15] < s9);
            int i16 = i2;
            do {
                i16--;
            } while (sArr[i16] > s10);
            int i17 = i15 - 1;
            loop9: while (true) {
                i17++;
                if (i17 > i16) {
                    break;
                }
                short s11 = sArr[i17];
                if (s11 < s9) {
                    sArr[i17] = sArr[i15];
                    sArr[i15] = s11;
                    i15++;
                } else if (s11 > s10) {
                    while (sArr[i16] > s10) {
                        int i18 = i16 - 1;
                        if (i16 != i17) {
                            i16 = i18;
                        } else {
                            i16 = i18;
                            break loop9;
                        }
                    }
                    if (sArr[i16] < s9) {
                        sArr[i17] = sArr[i15];
                        sArr[i15] = sArr[i16];
                        i15++;
                    } else {
                        sArr[i17] = sArr[i16];
                    }
                    sArr[i16] = s11;
                    i16--;
                } else {
                    continue;
                }
            }
            int i19 = i15 - 1;
            sArr[i] = sArr[i19];
            sArr[i19] = s9;
            int i20 = i16 + 1;
            sArr[i2] = sArr[i20];
            sArr[i20] = s10;
            sort(sArr, i, i15 - 2, z);
            sort(sArr, i16 + 2, i2, false);
            if (i15 < i12 && i14 < i16) {
                while (sArr[i15] == s9) {
                    i15++;
                }
                while (sArr[i16] == s10) {
                    i16--;
                }
                int i21 = i15 - 1;
                loop13: while (true) {
                    i21++;
                    if (i21 > i16) {
                        break;
                    }
                    short s12 = sArr[i21];
                    if (s12 == s9) {
                        sArr[i21] = sArr[i15];
                        sArr[i15] = s12;
                        i15++;
                    } else if (s12 == s10) {
                        while (sArr[i16] == s10) {
                            int i22 = i16 - 1;
                            if (i16 != i21) {
                                i16 = i22;
                            } else {
                                i16 = i22;
                                break loop13;
                            }
                        }
                        if (sArr[i16] == s9) {
                            sArr[i21] = sArr[i15];
                            sArr[i15] = s9;
                            i15++;
                        } else {
                            sArr[i21] = sArr[i16];
                        }
                        sArr[i16] = s12;
                        i16--;
                    } else {
                        continue;
                    }
                }
            }
            sort(sArr, i15, i16, false);
            return;
        }
        short s13 = sArr[i10];
        int i23 = i;
        int i24 = i23;
        int i25 = i2;
        while (i23 <= i25) {
            if (sArr[i23] != s13) {
                short s14 = sArr[i23];
                if (s14 < s13) {
                    sArr[i23] = sArr[i24];
                    sArr[i24] = s14;
                    i24++;
                } else {
                    while (sArr[i25] > s13) {
                        i25--;
                    }
                    if (sArr[i25] < s13) {
                        sArr[i23] = sArr[i24];
                        sArr[i24] = sArr[i25];
                        i24++;
                    } else {
                        sArr[i23] = s13;
                    }
                    sArr[i25] = s14;
                    i25--;
                }
            }
            i23++;
        }
        sort(sArr, i, i24 - 1, z);
        sort(sArr, i25 + 1, i2, false);
    }

    static void sort(char[] cArr, int i, int i2, char[] cArr2, int i3, int i4) {
        if (i2 - i > COUNTING_SORT_THRESHOLD_FOR_SHORT_OR_CHAR) {
            int i5 = 65536;
            int[] iArr = new int[65536];
            int i6 = i - 1;
            while (true) {
                i6++;
                if (i6 > i2) {
                    break;
                }
                char c = cArr[i6];
                iArr[c] = iArr[c] + 1;
            }
            int i7 = i2 + 1;
            while (i7 > i) {
                do {
                    i5--;
                } while (iArr[i5] == 0);
                char c2 = (char) i5;
                int i8 = iArr[i5];
                do {
                    i7--;
                    cArr[i7] = c2;
                    i8--;
                } while (i8 > 0);
            }
            return;
        }
        doSort(cArr, i, i2, cArr2, i3, i4);
    }

    private static void doSort(char[] cArr, int i, int i2, char[] cArr2, int i3, int i4) {
        int i5;
        int i6;
        int i7;
        char[] cArr3 = cArr;
        char[] cArr4 = cArr2;
        int i8 = 1;
        if (i2 - i < QUICKSORT_THRESHOLD) {
            sort(cArr3, i, i2, true);
            return;
        }
        int[] iArr = new int[68];
        int i9 = 0;
        iArr[0] = i;
        int i10 = i;
        int i11 = 0;
        while (i10 < i2) {
            int i12 = i10 + 1;
            if (cArr3[i10] < cArr3[i12]) {
                do {
                    i10++;
                    if (i10 > i2) {
                        break;
                    }
                } while (cArr3[i10 - 1] <= cArr3[i10]);
            } else if (cArr3[i10] > cArr3[i12]) {
                do {
                    i10++;
                    if (i10 > i2) {
                        break;
                    }
                } while (cArr3[i10 - 1] >= cArr3[i10]);
                int i13 = iArr[i11] - 1;
                int i14 = i10;
                while (true) {
                    i13++;
                    i14--;
                    if (i13 >= i14) {
                        break;
                    }
                    char c = cArr3[i13];
                    cArr3[i13] = cArr3[i14];
                    cArr3[i14] = c;
                }
            } else {
                int i15 = MAX_RUN_LENGTH;
                do {
                    i10++;
                    if (i10 <= i2 && cArr3[i10 - 1] == cArr3[i10]) {
                        i15--;
                    }
                } while (i15 != 0);
                sort(cArr3, i, i2, true);
                return;
            }
            i11++;
            if (i11 != MAX_RUN_COUNT) {
                iArr[i11] = i10;
            } else {
                sort(cArr3, i, i2, true);
                return;
            }
        }
        int i16 = i2 + 1;
        if (iArr[i11] == i2) {
            i11++;
            iArr[i11] = i16;
        } else if (i11 == 1) {
            return;
        }
        int i17 = 1;
        byte b = 0;
        while (true) {
            i17 <<= 1;
            if (i17 >= i11) {
                break;
            } else {
                b = (byte) (b ^ 1);
            }
        }
        int i18 = i16 - i;
        if (cArr4 == null || i4 < i18 || i3 + i18 > cArr4.length) {
            cArr4 = new char[i18];
            i5 = 0;
        } else {
            i5 = i3;
        }
        if (b == 0) {
            System.arraycopy((Object) cArr3, i, (Object) cArr4, i5, i18);
            i7 = i5 - i;
            i6 = 0;
            char[] cArr5 = cArr4;
            cArr4 = cArr3;
            cArr3 = cArr5;
        } else {
            i6 = i5 - i;
            i7 = 0;
        }
        while (i11 > i8) {
            int i19 = i9;
            for (int i20 = 2; i20 <= i11; i20 += 2) {
                int i21 = iArr[i20];
                int i22 = iArr[i20 - 1];
                int i23 = iArr[i20 - 2];
                int i24 = i22;
                int i25 = i23;
                while (i23 < i21) {
                    if (i24 >= i21 || (i25 < i22 && cArr3[i25 + i7] <= cArr3[i24 + i7])) {
                        cArr4[i23 + i6] = cArr3[i25 + i7];
                        i25++;
                    } else {
                        cArr4[i23 + i6] = cArr3[i24 + i7];
                        i24++;
                    }
                    i23++;
                }
                i19++;
                iArr[i19] = i21;
            }
            if ((i11 & 1) != 0) {
                int i26 = iArr[i11 - 1];
                int i27 = i16;
                while (true) {
                    i27--;
                    if (i27 < i26) {
                        break;
                    } else {
                        cArr4[i27 + i6] = cArr3[i27 + i7];
                    }
                }
                i19++;
                iArr[i19] = i16;
            }
            i11 = i19;
            i8 = 1;
            i9 = 0;
            char[] cArr6 = cArr4;
            cArr4 = cArr3;
            cArr3 = cArr6;
            int i28 = i7;
            i7 = i6;
            i6 = i28;
        }
    }

    private static void sort(char[] cArr, int i, int i2, boolean z) {
        int i3 = (i2 - i) + 1;
        if (i3 < INSERTION_SORT_THRESHOLD) {
            if (z) {
                int i4 = i;
                while (i4 < i2) {
                    int i5 = i4 + 1;
                    char c = cArr[i5];
                    while (true) {
                        if (c < cArr[i4]) {
                            cArr[i4 + 1] = cArr[i4];
                            int i6 = i4 - 1;
                            if (i4 != i) {
                                i4 = i6;
                            } else {
                                i4 = i6;
                                break;
                            }
                        }
                    }
                    cArr[i4 + 1] = c;
                    i4 = i5;
                }
                return;
            }
            while (i < i2) {
                i++;
                if (cArr[i] < cArr[i - 1]) {
                    while (true) {
                        int i7 = i + 1;
                        if (i7 > i2) {
                            break;
                        }
                        char c2 = cArr[i];
                        char c3 = cArr[i7];
                        if (c2 < c3) {
                            c3 = c2;
                            c2 = cArr[i7];
                        }
                        while (true) {
                            i--;
                            if (c2 >= cArr[i]) {
                                break;
                            } else {
                                cArr[i + 2] = cArr[i];
                            }
                        }
                        int i8 = i + 1;
                        cArr[i8 + 1] = c2;
                        while (true) {
                            i8--;
                            if (c3 < cArr[i8]) {
                                cArr[i8 + 1] = cArr[i8];
                            }
                        }
                        cArr[i8 + 1] = c3;
                        i = i7 + 1;
                    }
                    char c4 = cArr[i2];
                    while (true) {
                        i2--;
                        if (c4 < cArr[i2]) {
                            cArr[i2 + 1] = cArr[i2];
                        } else {
                            cArr[i2 + 1] = c4;
                            return;
                        }
                    }
                }
            }
            return;
        }
        int i9 = (i3 >> 3) + (i3 >> 6) + 1;
        int i10 = (i + i2) >>> 1;
        int i11 = i10 - i9;
        int i12 = i11 - i9;
        int i13 = i10 + i9;
        int i14 = i9 + i13;
        if (cArr[i11] < cArr[i12]) {
            char c5 = cArr[i11];
            cArr[i11] = cArr[i12];
            cArr[i12] = c5;
        }
        if (cArr[i10] < cArr[i11]) {
            char c6 = cArr[i10];
            cArr[i10] = cArr[i11];
            cArr[i11] = c6;
            if (c6 < cArr[i12]) {
                cArr[i11] = cArr[i12];
                cArr[i12] = c6;
            }
        }
        if (cArr[i13] < cArr[i10]) {
            char c7 = cArr[i13];
            cArr[i13] = cArr[i10];
            cArr[i10] = c7;
            if (c7 < cArr[i11]) {
                cArr[i10] = cArr[i11];
                cArr[i11] = c7;
                if (c7 < cArr[i12]) {
                    cArr[i11] = cArr[i12];
                    cArr[i12] = c7;
                }
            }
        }
        if (cArr[i14] < cArr[i13]) {
            char c8 = cArr[i14];
            cArr[i14] = cArr[i13];
            cArr[i13] = c8;
            if (c8 < cArr[i10]) {
                cArr[i13] = cArr[i10];
                cArr[i10] = c8;
                if (c8 < cArr[i11]) {
                    cArr[i10] = cArr[i11];
                    cArr[i11] = c8;
                    if (c8 < cArr[i12]) {
                        cArr[i11] = cArr[i12];
                        cArr[i12] = c8;
                    }
                }
            }
        }
        if (cArr[i12] != cArr[i11] && cArr[i11] != cArr[i10] && cArr[i10] != cArr[i13] && cArr[i13] != cArr[i14]) {
            char c9 = cArr[i11];
            char c10 = cArr[i13];
            cArr[i11] = cArr[i];
            cArr[i13] = cArr[i2];
            int i15 = i;
            do {
                i15++;
            } while (cArr[i15] < c9);
            int i16 = i2;
            do {
                i16--;
            } while (cArr[i16] > c10);
            int i17 = i15 - 1;
            loop9: while (true) {
                i17++;
                if (i17 > i16) {
                    break;
                }
                char c11 = cArr[i17];
                if (c11 < c9) {
                    cArr[i17] = cArr[i15];
                    cArr[i15] = c11;
                    i15++;
                } else if (c11 > c10) {
                    while (cArr[i16] > c10) {
                        int i18 = i16 - 1;
                        if (i16 != i17) {
                            i16 = i18;
                        } else {
                            i16 = i18;
                            break loop9;
                        }
                    }
                    if (cArr[i16] < c9) {
                        cArr[i17] = cArr[i15];
                        cArr[i15] = cArr[i16];
                        i15++;
                    } else {
                        cArr[i17] = cArr[i16];
                    }
                    cArr[i16] = c11;
                    i16--;
                } else {
                    continue;
                }
            }
            int i19 = i15 - 1;
            cArr[i] = cArr[i19];
            cArr[i19] = c9;
            int i20 = i16 + 1;
            cArr[i2] = cArr[i20];
            cArr[i20] = c10;
            sort(cArr, i, i15 - 2, z);
            sort(cArr, i16 + 2, i2, false);
            if (i15 < i12 && i14 < i16) {
                while (cArr[i15] == c9) {
                    i15++;
                }
                while (cArr[i16] == c10) {
                    i16--;
                }
                int i21 = i15 - 1;
                loop13: while (true) {
                    i21++;
                    if (i21 > i16) {
                        break;
                    }
                    char c12 = cArr[i21];
                    if (c12 == c9) {
                        cArr[i21] = cArr[i15];
                        cArr[i15] = c12;
                        i15++;
                    } else if (c12 == c10) {
                        while (cArr[i16] == c10) {
                            int i22 = i16 - 1;
                            if (i16 != i21) {
                                i16 = i22;
                            } else {
                                i16 = i22;
                                break loop13;
                            }
                        }
                        if (cArr[i16] == c9) {
                            cArr[i21] = cArr[i15];
                            cArr[i15] = c9;
                            i15++;
                        } else {
                            cArr[i21] = cArr[i16];
                        }
                        cArr[i16] = c12;
                        i16--;
                    } else {
                        continue;
                    }
                }
            }
            sort(cArr, i15, i16, false);
            return;
        }
        char c13 = cArr[i10];
        int i23 = i;
        int i24 = i23;
        int i25 = i2;
        while (i23 <= i25) {
            if (cArr[i23] != c13) {
                char c14 = cArr[i23];
                if (c14 < c13) {
                    cArr[i23] = cArr[i24];
                    cArr[i24] = c14;
                    i24++;
                } else {
                    while (cArr[i25] > c13) {
                        i25--;
                    }
                    if (cArr[i25] < c13) {
                        cArr[i23] = cArr[i24];
                        cArr[i24] = cArr[i25];
                        i24++;
                    } else {
                        cArr[i23] = c13;
                    }
                    cArr[i25] = c14;
                    i25--;
                }
            }
            i23++;
        }
        sort(cArr, i, i24 - 1, z);
        sort(cArr, i25 + 1, i2, false);
    }

    static void sort(byte[] bArr, int i, int i2) {
        if (i2 - i > COUNTING_SORT_THRESHOLD_FOR_BYTE) {
            int i3 = 256;
            int[] iArr = new int[256];
            int i4 = i - 1;
            while (true) {
                i4++;
                if (i4 > i2) {
                    break;
                }
                int i5 = bArr[i4] + 128;
                iArr[i5] = iArr[i5] + 1;
            }
            int i6 = i2 + 1;
            while (i6 > i) {
                do {
                    i3--;
                } while (iArr[i3] == 0);
                byte b = (byte) (i3 - 128);
                int i7 = iArr[i3];
                do {
                    i6--;
                    bArr[i6] = b;
                    i7--;
                } while (i7 > 0);
            }
            return;
        }
        int i8 = i;
        while (i8 < i2) {
            int i9 = i8 + 1;
            byte b2 = bArr[i9];
            while (true) {
                if (b2 < bArr[i8]) {
                    bArr[i8 + 1] = bArr[i8];
                    int i10 = i8 - 1;
                    if (i8 != i) {
                        i8 = i10;
                    } else {
                        i8 = i10;
                        break;
                    }
                }
            }
            bArr[i8 + 1] = b2;
            i8 = i9;
        }
    }

    static void sort(float[] fArr, int i, int i2, float[] fArr2, int i3, int i4) {
        while (i <= i2 && Float.isNaN(fArr[i2])) {
            i2--;
        }
        int i5 = i2;
        while (true) {
            i2--;
            if (i2 < i) {
                break;
            }
            float f = fArr[i2];
            if (f != f) {
                fArr[i2] = fArr[i5];
                fArr[i5] = f;
                i5--;
            }
        }
        doSort(fArr, i, i5, fArr2, i3, i4);
        int i6 = i5;
        while (i < i6) {
            int i7 = (i + i6) >>> 1;
            if (fArr[i7] < 0.0f) {
                i = i7 + 1;
            } else {
                i6 = i7;
            }
        }
        while (i <= i5 && Float.floatToRawIntBits(fArr[i]) < 0) {
            i++;
        }
        int i8 = i - 1;
        while (true) {
            i++;
            if (i <= i5) {
                float f2 = fArr[i];
                if (f2 == 0.0f) {
                    if (Float.floatToRawIntBits(f2) < 0) {
                        fArr[i] = 0.0f;
                        i8++;
                        fArr[i8] = -0.0f;
                    }
                } else {
                    return;
                }
            } else {
                return;
            }
        }
    }

    private static void doSort(float[] fArr, int i, int i2, float[] fArr2, int i3, int i4) {
        int i5;
        int i6;
        int i7;
        float[] fArr3 = fArr;
        float[] fArr4 = fArr2;
        if (i2 - i < QUICKSORT_THRESHOLD) {
            sort(fArr3, i, i2, true);
            return;
        }
        int[] iArr = new int[68];
        iArr[0] = i;
        int i8 = i;
        int i9 = 0;
        while (i8 < i2) {
            int i10 = i8 + 1;
            if (fArr3[i8] < fArr3[i10]) {
                do {
                    i8++;
                    if (i8 > i2) {
                        break;
                    }
                } while (fArr3[i8 - 1] <= fArr3[i8]);
            } else if (fArr3[i8] > fArr3[i10]) {
                do {
                    i8++;
                    if (i8 > i2) {
                        break;
                    }
                } while (fArr3[i8 - 1] >= fArr3[i8]);
                int i11 = iArr[i9] - 1;
                int i12 = i8;
                while (true) {
                    i11++;
                    i12--;
                    if (i11 >= i12) {
                        break;
                    }
                    float f = fArr3[i11];
                    fArr3[i11] = fArr3[i12];
                    fArr3[i12] = f;
                }
            } else {
                int i13 = MAX_RUN_LENGTH;
                do {
                    i8++;
                    if (i8 <= i2 && fArr3[i8 - 1] == fArr3[i8]) {
                        i13--;
                    }
                } while (i13 != 0);
                sort(fArr3, i, i2, true);
                return;
            }
            i9++;
            if (i9 != MAX_RUN_COUNT) {
                iArr[i9] = i8;
            } else {
                sort(fArr3, i, i2, true);
                return;
            }
        }
        int i14 = i2 + 1;
        if (iArr[i9] == i2) {
            i9++;
            iArr[i9] = i14;
        } else if (i9 == 1) {
            return;
        }
        int i15 = 1;
        byte b = 0;
        while (true) {
            i15 <<= 1;
            if (i15 >= i9) {
                break;
            } else {
                b = (byte) (b ^ 1);
            }
        }
        int i16 = i14 - i;
        if (fArr4 == null || i4 < i16 || i3 + i16 > fArr4.length) {
            fArr4 = new float[i16];
            i5 = 0;
        } else {
            i5 = i3;
        }
        if (b == 0) {
            System.arraycopy((Object) fArr3, i, (Object) fArr4, i5, i16);
            i7 = i5 - i;
            i6 = 0;
            float[] fArr5 = fArr4;
            fArr4 = fArr3;
            fArr3 = fArr5;
        } else {
            i6 = i5 - i;
            i7 = 0;
        }
        while (i9 > 1) {
            int i17 = 0;
            for (int i18 = 2; i18 <= i9; i18 += 2) {
                int i19 = iArr[i18];
                int i20 = iArr[i18 - 1];
                int i21 = iArr[i18 - 2];
                int i22 = i20;
                int i23 = i21;
                while (i21 < i19) {
                    if (i22 >= i19 || (i23 < i20 && fArr3[i23 + i7] <= fArr3[i22 + i7])) {
                        fArr4[i21 + i6] = fArr3[i23 + i7];
                        i23++;
                    } else {
                        fArr4[i21 + i6] = fArr3[i22 + i7];
                        i22++;
                    }
                    i21++;
                }
                i17++;
                iArr[i17] = i19;
            }
            if ((i9 & 1) != 0) {
                int i24 = iArr[i9 - 1];
                int i25 = i14;
                while (true) {
                    i25--;
                    if (i25 < i24) {
                        break;
                    } else {
                        fArr4[i25 + i6] = fArr3[i25 + i7];
                    }
                }
                i17++;
                iArr[i17] = i14;
            }
            i9 = i17;
            float[] fArr6 = fArr4;
            fArr4 = fArr3;
            fArr3 = fArr6;
            int i26 = i7;
            i7 = i6;
            i6 = i26;
        }
    }

    private static void sort(float[] fArr, int i, int i2, boolean z) {
        int i3 = (i2 - i) + 1;
        if (i3 < INSERTION_SORT_THRESHOLD) {
            if (z) {
                int i4 = i;
                while (i4 < i2) {
                    int i5 = i4 + 1;
                    float f = fArr[i5];
                    while (true) {
                        if (f < fArr[i4]) {
                            fArr[i4 + 1] = fArr[i4];
                            int i6 = i4 - 1;
                            if (i4 != i) {
                                i4 = i6;
                            } else {
                                i4 = i6;
                                break;
                            }
                        }
                    }
                    fArr[i4 + 1] = f;
                    i4 = i5;
                }
                return;
            }
            while (i < i2) {
                i++;
                if (fArr[i] < fArr[i - 1]) {
                    while (true) {
                        int i7 = i + 1;
                        if (i7 > i2) {
                            break;
                        }
                        float f2 = fArr[i];
                        float f3 = fArr[i7];
                        if (f2 < f3) {
                            f3 = f2;
                            f2 = fArr[i7];
                        }
                        while (true) {
                            i--;
                            if (f2 >= fArr[i]) {
                                break;
                            } else {
                                fArr[i + 2] = fArr[i];
                            }
                        }
                        int i8 = i + 1;
                        fArr[i8 + 1] = f2;
                        while (true) {
                            i8--;
                            if (f3 < fArr[i8]) {
                                fArr[i8 + 1] = fArr[i8];
                            }
                        }
                        fArr[i8 + 1] = f3;
                        i = i7 + 1;
                    }
                    float f4 = fArr[i2];
                    while (true) {
                        i2--;
                        if (f4 < fArr[i2]) {
                            fArr[i2 + 1] = fArr[i2];
                        } else {
                            fArr[i2 + 1] = f4;
                            return;
                        }
                    }
                }
            }
            return;
        }
        int i9 = (i3 >> 3) + (i3 >> 6) + 1;
        int i10 = (i + i2) >>> 1;
        int i11 = i10 - i9;
        int i12 = i11 - i9;
        int i13 = i10 + i9;
        int i14 = i9 + i13;
        if (fArr[i11] < fArr[i12]) {
            float f5 = fArr[i11];
            fArr[i11] = fArr[i12];
            fArr[i12] = f5;
        }
        if (fArr[i10] < fArr[i11]) {
            float f6 = fArr[i10];
            fArr[i10] = fArr[i11];
            fArr[i11] = f6;
            if (f6 < fArr[i12]) {
                fArr[i11] = fArr[i12];
                fArr[i12] = f6;
            }
        }
        if (fArr[i13] < fArr[i10]) {
            float f7 = fArr[i13];
            fArr[i13] = fArr[i10];
            fArr[i10] = f7;
            if (f7 < fArr[i11]) {
                fArr[i10] = fArr[i11];
                fArr[i11] = f7;
                if (f7 < fArr[i12]) {
                    fArr[i11] = fArr[i12];
                    fArr[i12] = f7;
                }
            }
        }
        if (fArr[i14] < fArr[i13]) {
            float f8 = fArr[i14];
            fArr[i14] = fArr[i13];
            fArr[i13] = f8;
            if (f8 < fArr[i10]) {
                fArr[i13] = fArr[i10];
                fArr[i10] = f8;
                if (f8 < fArr[i11]) {
                    fArr[i10] = fArr[i11];
                    fArr[i11] = f8;
                    if (f8 < fArr[i12]) {
                        fArr[i11] = fArr[i12];
                        fArr[i12] = f8;
                    }
                }
            }
        }
        if (fArr[i12] != fArr[i11] && fArr[i11] != fArr[i10] && fArr[i10] != fArr[i13] && fArr[i13] != fArr[i14]) {
            float f9 = fArr[i11];
            float f10 = fArr[i13];
            fArr[i11] = fArr[i];
            fArr[i13] = fArr[i2];
            int i15 = i;
            do {
                i15++;
            } while (fArr[i15] < f9);
            int i16 = i2;
            do {
                i16--;
            } while (fArr[i16] > f10);
            int i17 = i15 - 1;
            loop9: while (true) {
                i17++;
                if (i17 > i16) {
                    break;
                }
                float f11 = fArr[i17];
                if (f11 < f9) {
                    fArr[i17] = fArr[i15];
                    fArr[i15] = f11;
                    i15++;
                } else if (f11 > f10) {
                    while (fArr[i16] > f10) {
                        int i18 = i16 - 1;
                        if (i16 != i17) {
                            i16 = i18;
                        } else {
                            i16 = i18;
                            break loop9;
                        }
                    }
                    if (fArr[i16] < f9) {
                        fArr[i17] = fArr[i15];
                        fArr[i15] = fArr[i16];
                        i15++;
                    } else {
                        fArr[i17] = fArr[i16];
                    }
                    fArr[i16] = f11;
                    i16--;
                } else {
                    continue;
                }
            }
            int i19 = i15 - 1;
            fArr[i] = fArr[i19];
            fArr[i19] = f9;
            int i20 = i16 + 1;
            fArr[i2] = fArr[i20];
            fArr[i20] = f10;
            sort(fArr, i, i15 - 2, z);
            sort(fArr, i16 + 2, i2, false);
            if (i15 < i12 && i14 < i16) {
                while (fArr[i15] == f9) {
                    i15++;
                }
                while (fArr[i16] == f10) {
                    i16--;
                }
                int i21 = i15 - 1;
                loop13: while (true) {
                    i21++;
                    if (i21 > i16) {
                        break;
                    }
                    float f12 = fArr[i21];
                    if (f12 == f9) {
                        fArr[i21] = fArr[i15];
                        fArr[i15] = f12;
                        i15++;
                    } else if (f12 == f10) {
                        while (fArr[i16] == f10) {
                            int i22 = i16 - 1;
                            if (i16 != i21) {
                                i16 = i22;
                            } else {
                                i16 = i22;
                                break loop13;
                            }
                        }
                        if (fArr[i16] == f9) {
                            fArr[i21] = fArr[i15];
                            fArr[i15] = fArr[i16];
                            i15++;
                        } else {
                            fArr[i21] = fArr[i16];
                        }
                        fArr[i16] = f12;
                        i16--;
                    } else {
                        continue;
                    }
                }
            }
            sort(fArr, i15, i16, false);
            return;
        }
        float f13 = fArr[i10];
        int i23 = i;
        int i24 = i23;
        int i25 = i2;
        while (i23 <= i25) {
            if (fArr[i23] != f13) {
                float f14 = fArr[i23];
                if (f14 < f13) {
                    fArr[i23] = fArr[i24];
                    fArr[i24] = f14;
                    i24++;
                } else {
                    while (fArr[i25] > f13) {
                        i25--;
                    }
                    if (fArr[i25] < f13) {
                        fArr[i23] = fArr[i24];
                        fArr[i24] = fArr[i25];
                        i24++;
                    } else {
                        fArr[i23] = fArr[i25];
                    }
                    fArr[i25] = f14;
                    i25--;
                }
            }
            i23++;
        }
        sort(fArr, i, i24 - 1, z);
        sort(fArr, i25 + 1, i2, false);
    }

    static void sort(double[] dArr, int i, int i2, double[] dArr2, int i3, int i4) {
        while (i <= i2 && Double.isNaN(dArr[i2])) {
            i2--;
        }
        int i5 = i2;
        while (true) {
            i2--;
            if (i2 < i) {
                break;
            }
            double d = dArr[i2];
            if (d != d) {
                dArr[i2] = dArr[i5];
                dArr[i5] = d;
                i5--;
            }
        }
        doSort(dArr, i, i5, dArr2, i3, i4);
        int i6 = i5;
        while (i < i6) {
            int i7 = (i + i6) >>> 1;
            if (dArr[i7] < 0.0d) {
                i = i7 + 1;
            } else {
                i6 = i7;
            }
        }
        while (i <= i5 && Double.doubleToRawLongBits(dArr[i]) < 0) {
            i++;
        }
        int i8 = i - 1;
        while (true) {
            i++;
            if (i <= i5) {
                double d2 = dArr[i];
                if (d2 != 0.0d) {
                    return;
                }
                if (Double.doubleToRawLongBits(d2) < 0) {
                    dArr[i] = 0.0d;
                    i8++;
                    dArr[i8] = -0.0d;
                }
            } else {
                return;
            }
        }
    }

    private static void doSort(double[] dArr, int i, int i2, double[] dArr2, int i3, int i4) {
        int i5;
        int i6;
        int i7;
        double[] dArr3 = dArr;
        double[] dArr4 = dArr2;
        if (i2 - i < QUICKSORT_THRESHOLD) {
            sort(dArr3, i, i2, true);
            return;
        }
        int[] iArr = new int[68];
        iArr[0] = i;
        int i8 = i;
        int i9 = 0;
        while (i8 < i2) {
            int i10 = i8 + 1;
            if (dArr3[i8] < dArr3[i10]) {
                do {
                    i8++;
                    if (i8 > i2) {
                        break;
                    }
                } while (dArr3[i8 - 1] <= dArr3[i8]);
            } else if (dArr3[i8] > dArr3[i10]) {
                do {
                    i8++;
                    if (i8 > i2) {
                        break;
                    }
                } while (dArr3[i8 - 1] >= dArr3[i8]);
                int i11 = iArr[i9] - 1;
                int i12 = i8;
                while (true) {
                    i11++;
                    i12--;
                    if (i11 >= i12) {
                        break;
                    }
                    double d = dArr3[i11];
                    dArr3[i11] = dArr3[i12];
                    dArr3[i12] = d;
                }
            } else {
                int i13 = MAX_RUN_LENGTH;
                do {
                    i8++;
                    if (i8 <= i2 && dArr3[i8 - 1] == dArr3[i8]) {
                        i13--;
                    }
                } while (i13 != 0);
                sort(dArr3, i, i2, true);
                return;
            }
            i9++;
            if (i9 != MAX_RUN_COUNT) {
                iArr[i9] = i8;
            } else {
                sort(dArr3, i, i2, true);
                return;
            }
        }
        int i14 = i2 + 1;
        if (iArr[i9] == i2) {
            i9++;
            iArr[i9] = i14;
        } else if (i9 == 1) {
            return;
        }
        int i15 = 1;
        byte b = 0;
        while (true) {
            i15 <<= 1;
            if (i15 >= i9) {
                break;
            } else {
                b = (byte) (b ^ 1);
            }
        }
        int i16 = i14 - i;
        if (dArr4 == null || i4 < i16 || i3 + i16 > dArr4.length) {
            dArr4 = new double[i16];
            i5 = 0;
        } else {
            i5 = i3;
        }
        if (b == 0) {
            System.arraycopy((Object) dArr3, i, (Object) dArr4, i5, i16);
            i7 = i5 - i;
            i6 = 0;
            double[] dArr5 = dArr4;
            dArr4 = dArr3;
            dArr3 = dArr5;
        } else {
            i6 = i5 - i;
            i7 = 0;
        }
        while (i9 > 1) {
            int i17 = 0;
            for (int i18 = 2; i18 <= i9; i18 += 2) {
                int i19 = iArr[i18];
                int i20 = iArr[i18 - 1];
                int i21 = iArr[i18 - 2];
                int i22 = i20;
                int i23 = i21;
                while (i21 < i19) {
                    if (i22 >= i19 || (i23 < i20 && dArr3[i23 + i7] <= dArr3[i22 + i7])) {
                        dArr4[i21 + i6] = dArr3[i23 + i7];
                        i23++;
                    } else {
                        dArr4[i21 + i6] = dArr3[i22 + i7];
                        i22++;
                    }
                    i21++;
                }
                i17++;
                iArr[i17] = i19;
            }
            if ((i9 & 1) != 0) {
                int i24 = iArr[i9 - 1];
                int i25 = i14;
                while (true) {
                    i25--;
                    if (i25 < i24) {
                        break;
                    } else {
                        dArr4[i25 + i6] = dArr3[i25 + i7];
                    }
                }
                i17++;
                iArr[i17] = i14;
            }
            i9 = i17;
            double[] dArr6 = dArr4;
            dArr4 = dArr3;
            dArr3 = dArr6;
            int i26 = i7;
            i7 = i6;
            i6 = i26;
        }
    }

    private static void sort(double[] dArr, int i, int i2, boolean z) {
        int i3 = i;
        int i4 = i2;
        int i5 = (i4 - i3) + 1;
        if (i5 < INSERTION_SORT_THRESHOLD) {
            if (z) {
                int i6 = i3;
                while (i6 < i4) {
                    int i7 = i6 + 1;
                    double d = dArr[i7];
                    while (true) {
                        if (d < dArr[i6]) {
                            dArr[i6 + 1] = dArr[i6];
                            int i8 = i6 - 1;
                            if (i6 != i3) {
                                i6 = i8;
                            } else {
                                i6 = i8;
                                break;
                            }
                        }
                    }
                    dArr[i6 + 1] = d;
                    i6 = i7;
                }
                return;
            }
            while (i3 < i4) {
                i3++;
                if (dArr[i3] < dArr[i3 - 1]) {
                    while (true) {
                        int i9 = i3 + 1;
                        if (i9 > i4) {
                            break;
                        }
                        double d2 = dArr[i3];
                        double d3 = dArr[i9];
                        if (d2 < d3) {
                            d2 = dArr[i9];
                            d3 = d2;
                        }
                        while (true) {
                            i3--;
                            if (d2 >= dArr[i3]) {
                                break;
                            } else {
                                dArr[i3 + 2] = dArr[i3];
                            }
                        }
                        int i10 = i3 + 1;
                        dArr[i10 + 1] = d2;
                        while (true) {
                            i10--;
                            if (d3 < dArr[i10]) {
                                dArr[i10 + 1] = dArr[i10];
                            }
                        }
                        dArr[i10 + 1] = d3;
                        i3 = i9 + 1;
                    }
                    double d4 = dArr[i4];
                    while (true) {
                        i4--;
                        if (d4 < dArr[i4]) {
                            dArr[i4 + 1] = dArr[i4];
                        } else {
                            dArr[i4 + 1] = d4;
                            return;
                        }
                    }
                }
            }
            return;
        }
        int i11 = (i5 >> 3) + (i5 >> 6) + 1;
        int i12 = (i3 + i4) >>> 1;
        int i13 = i12 - i11;
        int i14 = i13 - i11;
        int i15 = i12 + i11;
        int i16 = i11 + i15;
        if (dArr[i13] < dArr[i14]) {
            double d5 = dArr[i13];
            dArr[i13] = dArr[i14];
            dArr[i14] = d5;
        }
        if (dArr[i12] < dArr[i13]) {
            double d6 = dArr[i12];
            dArr[i12] = dArr[i13];
            dArr[i13] = d6;
            if (d6 < dArr[i14]) {
                dArr[i13] = dArr[i14];
                dArr[i14] = d6;
            }
        }
        if (dArr[i15] < dArr[i12]) {
            double d7 = dArr[i15];
            dArr[i15] = dArr[i12];
            dArr[i12] = d7;
            if (d7 < dArr[i13]) {
                dArr[i12] = dArr[i13];
                dArr[i13] = d7;
                if (d7 < dArr[i14]) {
                    dArr[i13] = dArr[i14];
                    dArr[i14] = d7;
                }
            }
        }
        if (dArr[i16] < dArr[i15]) {
            double d8 = dArr[i16];
            dArr[i16] = dArr[i15];
            dArr[i15] = d8;
            if (d8 < dArr[i12]) {
                dArr[i15] = dArr[i12];
                dArr[i12] = d8;
                if (d8 < dArr[i13]) {
                    dArr[i12] = dArr[i13];
                    dArr[i13] = d8;
                    if (d8 < dArr[i14]) {
                        dArr[i13] = dArr[i14];
                        dArr[i14] = d8;
                    }
                }
            }
        }
        if (dArr[i14] != dArr[i13] && dArr[i13] != dArr[i12] && dArr[i12] != dArr[i15] && dArr[i15] != dArr[i16]) {
            double d9 = dArr[i13];
            double d10 = dArr[i15];
            dArr[i13] = dArr[i3];
            dArr[i15] = dArr[i4];
            int i17 = i3;
            do {
                i17++;
            } while (dArr[i17] < d9);
            int i18 = i4;
            do {
                i18--;
            } while (dArr[i18] > d10);
            int i19 = i17 - 1;
            loop9: while (true) {
                i19++;
                if (i19 > i18) {
                    break;
                }
                double d11 = dArr[i19];
                if (d11 < d9) {
                    dArr[i19] = dArr[i17];
                    dArr[i17] = d11;
                    i17++;
                } else if (d11 > d10) {
                    while (dArr[i18] > d10) {
                        int i20 = i18 - 1;
                        if (i18 != i19) {
                            i18 = i20;
                        } else {
                            i18 = i20;
                            break loop9;
                        }
                    }
                    if (dArr[i18] < d9) {
                        dArr[i19] = dArr[i17];
                        dArr[i17] = dArr[i18];
                        i17++;
                    } else {
                        dArr[i19] = dArr[i18];
                    }
                    dArr[i18] = d11;
                    i18--;
                } else {
                    continue;
                }
            }
            int i21 = i17 - 1;
            dArr[i3] = dArr[i21];
            dArr[i21] = d9;
            int i22 = i18 + 1;
            dArr[i4] = dArr[i22];
            dArr[i22] = d10;
            sort(dArr, i3, i17 - 2, z);
            sort(dArr, i18 + 2, i4, false);
            if (i17 < i14 && i16 < i18) {
                while (dArr[i17] == d9) {
                    i17++;
                }
                while (dArr[i18] == d10) {
                    i18--;
                }
                int i23 = i17 - 1;
                loop13: while (true) {
                    i23++;
                    if (i23 > i18) {
                        break;
                    }
                    double d12 = dArr[i23];
                    if (d12 == d9) {
                        dArr[i23] = dArr[i17];
                        dArr[i17] = d12;
                        i17++;
                    } else if (d12 == d10) {
                        while (dArr[i18] == d10) {
                            int i24 = i18 - 1;
                            if (i18 != i23) {
                                i18 = i24;
                            } else {
                                i18 = i24;
                                break loop13;
                            }
                        }
                        if (dArr[i18] == d9) {
                            dArr[i23] = dArr[i17];
                            dArr[i17] = dArr[i18];
                            i17++;
                        } else {
                            dArr[i23] = dArr[i18];
                        }
                        dArr[i18] = d12;
                        i18--;
                    } else {
                        continue;
                    }
                }
            }
            sort(dArr, i17, i18, false);
            return;
        }
        double d13 = dArr[i12];
        int i25 = i3;
        int i26 = i25;
        int i27 = i4;
        while (i25 <= i27) {
            if (dArr[i25] != d13) {
                double d14 = dArr[i25];
                if (d14 < d13) {
                    dArr[i25] = dArr[i26];
                    dArr[i26] = d14;
                    i26++;
                } else {
                    while (dArr[i27] > d13) {
                        i27--;
                    }
                    if (dArr[i27] < d13) {
                        dArr[i25] = dArr[i26];
                        dArr[i26] = dArr[i27];
                        i26++;
                    } else {
                        dArr[i25] = dArr[i27];
                    }
                    dArr[i27] = d14;
                    i27--;
                }
            }
            i25++;
        }
        sort(dArr, i3, i26 - 1, z);
        sort(dArr, i27 + 1, i4, false);
    }
}
