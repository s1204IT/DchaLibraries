package com.android.org.bouncycastle.math.raw;

import com.android.org.bouncycastle.util.Pack;
import java.math.BigInteger;

public abstract class Nat224 {
    private static final long M = 4294967295L;

    public static int add(int[] iArr, int[] iArr2, int[] iArr3) {
        long j = 0 + (((long) iArr[0]) & M) + (((long) iArr2[0]) & M);
        iArr3[0] = (int) j;
        long j2 = (j >>> 32) + (((long) iArr[1]) & M) + (((long) iArr2[1]) & M);
        iArr3[1] = (int) j2;
        long j3 = (j2 >>> 32) + (((long) iArr[2]) & M) + (((long) iArr2[2]) & M);
        iArr3[2] = (int) j3;
        long j4 = (j3 >>> 32) + (((long) iArr[3]) & M) + (((long) iArr2[3]) & M);
        iArr3[3] = (int) j4;
        long j5 = (j4 >>> 32) + (((long) iArr[4]) & M) + (((long) iArr2[4]) & M);
        iArr3[4] = (int) j5;
        long j6 = (j5 >>> 32) + (((long) iArr[5]) & M) + (((long) iArr2[5]) & M);
        iArr3[5] = (int) j6;
        long j7 = (j6 >>> 32) + (((long) iArr[6]) & M) + (((long) iArr2[6]) & M);
        iArr3[6] = (int) j7;
        return (int) (j7 >>> 32);
    }

    public static int add(int[] iArr, int i, int[] iArr2, int i2, int[] iArr3, int i3) {
        long j = 0 + (((long) iArr[i + 0]) & M) + (((long) iArr2[i2 + 0]) & M);
        iArr3[i3 + 0] = (int) j;
        long j2 = (j >>> 32) + (((long) iArr[i + 1]) & M) + (((long) iArr2[i2 + 1]) & M);
        iArr3[i3 + 1] = (int) j2;
        long j3 = (j2 >>> 32) + (((long) iArr[i + 2]) & M) + (((long) iArr2[i2 + 2]) & M);
        iArr3[i3 + 2] = (int) j3;
        long j4 = (j3 >>> 32) + (((long) iArr[i + 3]) & M) + (((long) iArr2[i2 + 3]) & M);
        iArr3[i3 + 3] = (int) j4;
        long j5 = (j4 >>> 32) + (((long) iArr[i + 4]) & M) + (((long) iArr2[i2 + 4]) & M);
        iArr3[i3 + 4] = (int) j5;
        long j6 = (j5 >>> 32) + (((long) iArr[i + 5]) & M) + (((long) iArr2[i2 + 5]) & M);
        iArr3[i3 + 5] = (int) j6;
        long j7 = (j6 >>> 32) + (((long) iArr[i + 6]) & M) + (((long) iArr2[i2 + 6]) & M);
        iArr3[i3 + 6] = (int) j7;
        return (int) (j7 >>> 32);
    }

    public static int addBothTo(int[] iArr, int[] iArr2, int[] iArr3) {
        long j = 0 + (((long) iArr[0]) & M) + (((long) iArr2[0]) & M) + (((long) iArr3[0]) & M);
        iArr3[0] = (int) j;
        long j2 = (j >>> 32) + (((long) iArr[1]) & M) + (((long) iArr2[1]) & M) + (((long) iArr3[1]) & M);
        iArr3[1] = (int) j2;
        long j3 = (j2 >>> 32) + (((long) iArr[2]) & M) + (((long) iArr2[2]) & M) + (((long) iArr3[2]) & M);
        iArr3[2] = (int) j3;
        long j4 = (j3 >>> 32) + (((long) iArr[3]) & M) + (((long) iArr2[3]) & M) + (((long) iArr3[3]) & M);
        iArr3[3] = (int) j4;
        long j5 = (j4 >>> 32) + (((long) iArr[4]) & M) + (((long) iArr2[4]) & M) + (((long) iArr3[4]) & M);
        iArr3[4] = (int) j5;
        long j6 = (j5 >>> 32) + (((long) iArr[5]) & M) + (((long) iArr2[5]) & M) + (((long) iArr3[5]) & M);
        iArr3[5] = (int) j6;
        long j7 = (j6 >>> 32) + (((long) iArr[6]) & M) + (((long) iArr2[6]) & M) + (((long) iArr3[6]) & M);
        iArr3[6] = (int) j7;
        return (int) (j7 >>> 32);
    }

    public static int addBothTo(int[] iArr, int i, int[] iArr2, int i2, int[] iArr3, int i3) {
        int i4 = i3 + 0;
        long j = 0 + (((long) iArr[i + 0]) & M) + (((long) iArr2[i2 + 0]) & M) + (((long) iArr3[i4]) & M);
        iArr3[i4] = (int) j;
        int i5 = i3 + 1;
        long j2 = (j >>> 32) + (((long) iArr[i + 1]) & M) + (((long) iArr2[i2 + 1]) & M) + (((long) iArr3[i5]) & M);
        iArr3[i5] = (int) j2;
        int i6 = i3 + 2;
        long j3 = (j2 >>> 32) + (((long) iArr[i + 2]) & M) + (((long) iArr2[i2 + 2]) & M) + (((long) iArr3[i6]) & M);
        iArr3[i6] = (int) j3;
        int i7 = i3 + 3;
        long j4 = (j3 >>> 32) + (((long) iArr[i + 3]) & M) + (((long) iArr2[i2 + 3]) & M) + (((long) iArr3[i7]) & M);
        iArr3[i7] = (int) j4;
        int i8 = i3 + 4;
        long j5 = (j4 >>> 32) + (((long) iArr[i + 4]) & M) + (((long) iArr2[i2 + 4]) & M) + (((long) iArr3[i8]) & M);
        iArr3[i8] = (int) j5;
        int i9 = i3 + 5;
        long j6 = (j5 >>> 32) + (((long) iArr[i + 5]) & M) + (((long) iArr2[i2 + 5]) & M) + (((long) iArr3[i9]) & M);
        iArr3[i9] = (int) j6;
        int i10 = i3 + 6;
        long j7 = (j6 >>> 32) + (((long) iArr[i + 6]) & M) + (((long) iArr2[i2 + 6]) & M) + (((long) iArr3[i10]) & M);
        iArr3[i10] = (int) j7;
        return (int) (j7 >>> 32);
    }

    public static int addTo(int[] iArr, int[] iArr2) {
        long j = 0 + (((long) iArr[0]) & M) + (((long) iArr2[0]) & M);
        iArr2[0] = (int) j;
        long j2 = (j >>> 32) + (((long) iArr[1]) & M) + (((long) iArr2[1]) & M);
        iArr2[1] = (int) j2;
        long j3 = (j2 >>> 32) + (((long) iArr[2]) & M) + (((long) iArr2[2]) & M);
        iArr2[2] = (int) j3;
        long j4 = (j3 >>> 32) + (((long) iArr[3]) & M) + (((long) iArr2[3]) & M);
        iArr2[3] = (int) j4;
        long j5 = (j4 >>> 32) + (((long) iArr[4]) & M) + (((long) iArr2[4]) & M);
        iArr2[4] = (int) j5;
        long j6 = (j5 >>> 32) + (((long) iArr[5]) & M) + (((long) iArr2[5]) & M);
        iArr2[5] = (int) j6;
        long j7 = (j6 >>> 32) + (((long) iArr[6]) & M) + (M & ((long) iArr2[6]));
        iArr2[6] = (int) j7;
        return (int) (j7 >>> 32);
    }

    public static int addTo(int[] iArr, int i, int[] iArr2, int i2, int i3) {
        int i4 = i2 + 0;
        long j = (((long) i3) & M) + (((long) iArr[i + 0]) & M) + (((long) iArr2[i4]) & M);
        iArr2[i4] = (int) j;
        int i5 = i2 + 1;
        long j2 = (j >>> 32) + (((long) iArr[i + 1]) & M) + (((long) iArr2[i5]) & M);
        iArr2[i5] = (int) j2;
        int i6 = i2 + 2;
        long j3 = (j2 >>> 32) + (((long) iArr[i + 2]) & M) + (((long) iArr2[i6]) & M);
        iArr2[i6] = (int) j3;
        int i7 = i2 + 3;
        long j4 = (j3 >>> 32) + (((long) iArr[i + 3]) & M) + (((long) iArr2[i7]) & M);
        iArr2[i7] = (int) j4;
        int i8 = i2 + 4;
        long j5 = (j4 >>> 32) + (((long) iArr[i + 4]) & M) + (((long) iArr2[i8]) & M);
        iArr2[i8] = (int) j5;
        int i9 = i2 + 5;
        long j6 = (j5 >>> 32) + (((long) iArr[i + 5]) & M) + (((long) iArr2[i9]) & M);
        iArr2[i9] = (int) j6;
        int i10 = i2 + 6;
        long j7 = (j6 >>> 32) + (((long) iArr[i + 6]) & M) + (M & ((long) iArr2[i10]));
        iArr2[i10] = (int) j7;
        return (int) (j7 >>> 32);
    }

    public static int addToEachOther(int[] iArr, int i, int[] iArr2, int i2) {
        int i3 = i + 0;
        int i4 = i2 + 0;
        long j = 0 + (((long) iArr[i3]) & M) + (((long) iArr2[i4]) & M);
        int i5 = (int) j;
        iArr[i3] = i5;
        iArr2[i4] = i5;
        int i6 = i + 1;
        int i7 = i2 + 1;
        long j2 = (j >>> 32) + (((long) iArr[i6]) & M) + (((long) iArr2[i7]) & M);
        int i8 = (int) j2;
        iArr[i6] = i8;
        iArr2[i7] = i8;
        int i9 = i + 2;
        int i10 = i2 + 2;
        long j3 = (j2 >>> 32) + (((long) iArr[i9]) & M) + (((long) iArr2[i10]) & M);
        int i11 = (int) j3;
        iArr[i9] = i11;
        iArr2[i10] = i11;
        int i12 = i + 3;
        int i13 = i2 + 3;
        long j4 = (j3 >>> 32) + (((long) iArr[i12]) & M) + (((long) iArr2[i13]) & M);
        int i14 = (int) j4;
        iArr[i12] = i14;
        iArr2[i13] = i14;
        int i15 = i + 4;
        int i16 = i2 + 4;
        long j5 = (j4 >>> 32) + (((long) iArr[i15]) & M) + (((long) iArr2[i16]) & M);
        int i17 = (int) j5;
        iArr[i15] = i17;
        iArr2[i16] = i17;
        int i18 = i + 5;
        int i19 = i2 + 5;
        long j6 = (j5 >>> 32) + (((long) iArr[i18]) & M) + (((long) iArr2[i19]) & M);
        int i20 = (int) j6;
        iArr[i18] = i20;
        iArr2[i19] = i20;
        int i21 = i + 6;
        int i22 = i2 + 6;
        long j7 = (j6 >>> 32) + (((long) iArr[i21]) & M) + (M & ((long) iArr2[i22]));
        int i23 = (int) j7;
        iArr[i21] = i23;
        iArr2[i22] = i23;
        return (int) (j7 >>> 32);
    }

    public static void copy(int[] iArr, int[] iArr2) {
        iArr2[0] = iArr[0];
        iArr2[1] = iArr[1];
        iArr2[2] = iArr[2];
        iArr2[3] = iArr[3];
        iArr2[4] = iArr[4];
        iArr2[5] = iArr[5];
        iArr2[6] = iArr[6];
    }

    public static int[] create() {
        return new int[7];
    }

    public static int[] createExt() {
        return new int[14];
    }

    public static boolean diff(int[] iArr, int i, int[] iArr2, int i2, int[] iArr3, int i3) {
        boolean zGte = gte(iArr, i, iArr2, i2);
        if (zGte) {
            sub(iArr, i, iArr2, i2, iArr3, i3);
        } else {
            sub(iArr2, i2, iArr, i, iArr3, i3);
        }
        return zGte;
    }

    public static boolean eq(int[] iArr, int[] iArr2) {
        for (int i = 6; i >= 0; i--) {
            if (iArr[i] != iArr2[i]) {
                return false;
            }
        }
        return true;
    }

    public static int[] fromBigInteger(BigInteger bigInteger) {
        if (bigInteger.signum() < 0 || bigInteger.bitLength() > 224) {
            throw new IllegalArgumentException();
        }
        int[] iArrCreate = create();
        int i = 0;
        while (bigInteger.signum() != 0) {
            iArrCreate[i] = bigInteger.intValue();
            bigInteger = bigInteger.shiftRight(32);
            i++;
        }
        return iArrCreate;
    }

    public static int getBit(int[] iArr, int i) {
        if (i == 0) {
            return iArr[0] & 1;
        }
        int i2 = i >> 5;
        if (i2 < 0 || i2 >= 7) {
            return 0;
        }
        return (iArr[i2] >>> (i & 31)) & 1;
    }

    public static boolean gte(int[] iArr, int[] iArr2) {
        for (int i = 6; i >= 0; i--) {
            int i2 = iArr[i] ^ Integer.MIN_VALUE;
            int i3 = Integer.MIN_VALUE ^ iArr2[i];
            if (i2 < i3) {
                return false;
            }
            if (i2 > i3) {
                return true;
            }
        }
        return true;
    }

    public static boolean gte(int[] iArr, int i, int[] iArr2, int i2) {
        for (int i3 = 6; i3 >= 0; i3--) {
            int i4 = iArr[i + i3] ^ Integer.MIN_VALUE;
            int i5 = Integer.MIN_VALUE ^ iArr2[i2 + i3];
            if (i4 < i5) {
                return false;
            }
            if (i4 > i5) {
                return true;
            }
        }
        return true;
    }

    public static boolean isOne(int[] iArr) {
        if (iArr[0] != 1) {
            return false;
        }
        for (int i = 1; i < 7; i++) {
            if (iArr[i] != 0) {
                return false;
            }
        }
        return true;
    }

    public static boolean isZero(int[] iArr) {
        for (int i = 0; i < 7; i++) {
            if (iArr[i] != 0) {
                return false;
            }
        }
        return true;
    }

    public static void mul(int[] iArr, int[] iArr2, int[] iArr3) {
        long j = ((long) iArr2[0]) & M;
        long j2 = ((long) iArr2[1]) & M;
        long j3 = ((long) iArr2[2]) & M;
        long j4 = ((long) iArr2[3]) & M;
        long j5 = ((long) iArr2[4]) & M;
        long j6 = ((long) iArr2[5]) & M;
        long j7 = ((long) iArr2[6]) & M;
        long j8 = ((long) iArr[0]) & M;
        long j9 = 0 + (j8 * j);
        iArr3[0] = (int) j9;
        long j10 = (j9 >>> 32) + (j8 * j2);
        iArr3[1] = (int) j10;
        long j11 = (j10 >>> 32) + (j8 * j3);
        iArr3[2] = (int) j11;
        long j12 = (j11 >>> 32) + (j8 * j4);
        iArr3[3] = (int) j12;
        long j13 = (j12 >>> 32) + (j8 * j5);
        iArr3[4] = (int) j13;
        long j14 = (j13 >>> 32) + (j8 * j6);
        iArr3[5] = (int) j14;
        long j15 = (j14 >>> 32) + (j8 * j7);
        iArr3[6] = (int) j15;
        iArr3[7] = (int) (j15 >>> 32);
        int i = 1;
        for (int i2 = 7; i < i2; i2 = 7) {
            long j16 = ((long) iArr[i]) & M;
            int i3 = i + 0;
            long j17 = j7;
            long j18 = 0 + (j16 * j) + (((long) iArr3[i3]) & M);
            iArr3[i3] = (int) j18;
            int i4 = i + 1;
            long j19 = j6;
            long j20 = (j18 >>> 32) + (j16 * j2) + (((long) iArr3[i4]) & M);
            iArr3[i4] = (int) j20;
            int i5 = i + 2;
            long j21 = (j20 >>> 32) + (j16 * j3) + (((long) iArr3[i5]) & M);
            iArr3[i5] = (int) j21;
            int i6 = i + 3;
            long j22 = (j21 >>> 32) + (j16 * j4) + (((long) iArr3[i6]) & M);
            iArr3[i6] = (int) j22;
            int i7 = i + 4;
            long j23 = (j22 >>> 32) + (j16 * j5) + (((long) iArr3[i7]) & M);
            iArr3[i7] = (int) j23;
            int i8 = i + 5;
            long j24 = (j23 >>> 32) + (j16 * j19) + (((long) iArr3[i8]) & M);
            iArr3[i8] = (int) j24;
            int i9 = i + 6;
            long j25 = (j24 >>> 32) + (j16 * j17) + (((long) iArr3[i9]) & M);
            iArr3[i9] = (int) j25;
            iArr3[i + 7] = (int) (j25 >>> 32);
            i = i4;
            j7 = j17;
            j6 = j19;
            j3 = j3;
        }
    }

    public static void mul(int[] iArr, int i, int[] iArr2, int i2, int[] iArr3, int i3) {
        long j = ((long) iArr2[i2 + 0]) & M;
        long j2 = ((long) iArr2[i2 + 1]) & M;
        long j3 = ((long) iArr2[i2 + 2]) & M;
        long j4 = ((long) iArr2[i2 + 3]) & M;
        long j5 = ((long) iArr2[i2 + 4]) & M;
        long j6 = ((long) iArr2[i2 + 5]) & M;
        long j7 = ((long) iArr2[i2 + 6]) & M;
        long j8 = ((long) iArr[i + 0]) & M;
        long j9 = 0 + (j8 * j);
        iArr3[i3 + 0] = (int) j9;
        long j10 = (j9 >>> 32) + (j8 * j2);
        iArr3[i3 + 1] = (int) j10;
        long j11 = (j10 >>> 32) + (j8 * j3);
        iArr3[i3 + 2] = (int) j11;
        long j12 = (j11 >>> 32) + (j8 * j4);
        iArr3[i3 + 3] = (int) j12;
        long j13 = (j12 >>> 32) + (j8 * j5);
        iArr3[i3 + 4] = (int) j13;
        long j14 = (j13 >>> 32) + (j8 * j6);
        iArr3[i3 + 5] = (int) j14;
        long j15 = (j14 >>> 32) + (j8 * j7);
        iArr3[i3 + 6] = (int) j15;
        iArr3[i3 + 7] = (int) (j15 >>> 32);
        int i4 = 1;
        int i5 = i3;
        int i6 = 1;
        while (i6 < 7) {
            i5 += i4;
            long j16 = ((long) iArr[i + i6]) & M;
            int i7 = i5 + 0;
            long j17 = 0 + (j16 * j) + (((long) iArr3[i7]) & M);
            iArr3[i7] = (int) j17;
            int i8 = i5 + 1;
            long j18 = (j17 >>> 32) + (j16 * j2) + (((long) iArr3[i8]) & M);
            iArr3[i8] = (int) j18;
            int i9 = i5 + 2;
            long j19 = (j18 >>> 32) + (j16 * j3) + (((long) iArr3[i9]) & M);
            iArr3[i9] = (int) j19;
            int i10 = i5 + 3;
            long j20 = (j19 >>> 32) + (j16 * j4) + (((long) iArr3[i10]) & M);
            iArr3[i10] = (int) j20;
            int i11 = i5 + 4;
            long j21 = (j20 >>> 32) + (j16 * j5) + (((long) iArr3[i11]) & M);
            iArr3[i11] = (int) j21;
            int i12 = i5 + 5;
            long j22 = (j21 >>> 32) + (j16 * j6) + (((long) iArr3[i12]) & M);
            iArr3[i12] = (int) j22;
            int i13 = i5 + 6;
            long j23 = (j22 >>> 32) + (j16 * j7) + (((long) iArr3[i13]) & M);
            iArr3[i13] = (int) j23;
            iArr3[i5 + 7] = (int) (j23 >>> 32);
            i6++;
            j2 = j2;
            j3 = j3;
            i4 = 1;
        }
    }

    public static int mulAddTo(int[] iArr, int[] iArr2, int[] iArr3) {
        long j = ((long) iArr2[0]) & M;
        long j2 = ((long) iArr2[1]) & M;
        long j3 = ((long) iArr2[2]) & M;
        long j4 = ((long) iArr2[3]) & M;
        long j5 = ((long) iArr2[4]) & M;
        long j6 = ((long) iArr2[5]) & M;
        long j7 = ((long) iArr2[6]) & M;
        long j8 = 0;
        int i = 0;
        while (i < 7) {
            long j9 = j7;
            long j10 = ((long) iArr[i]) & M;
            int i2 = i + 0;
            long j11 = j5;
            long j12 = 0 + (j10 * j) + (((long) iArr3[i2]) & M);
            iArr3[i2] = (int) j12;
            int i3 = i + 1;
            long j13 = j2;
            long j14 = (j12 >>> 32) + (j10 * j2) + (((long) iArr3[i3]) & M);
            iArr3[i3] = (int) j14;
            int i4 = i + 2;
            long j15 = (j14 >>> 32) + (j10 * j3) + (((long) iArr3[i4]) & M);
            iArr3[i4] = (int) j15;
            int i5 = i + 3;
            long j16 = (j15 >>> 32) + (j10 * j4) + (((long) iArr3[i5]) & M);
            iArr3[i5] = (int) j16;
            int i6 = i + 4;
            long j17 = (j16 >>> 32) + (j10 * j11) + (((long) iArr3[i6]) & M);
            iArr3[i6] = (int) j17;
            int i7 = i + 5;
            long j18 = (j17 >>> 32) + (j10 * j6) + (((long) iArr3[i7]) & M);
            iArr3[i7] = (int) j18;
            int i8 = i + 6;
            long j19 = (j18 >>> 32) + (j10 * j9) + (((long) iArr3[i8]) & M);
            iArr3[i8] = (int) j19;
            int i9 = i + 7;
            long j20 = (j19 >>> 32) + j8 + (((long) iArr3[i9]) & M);
            iArr3[i9] = (int) j20;
            j8 = j20 >>> 32;
            j7 = j9;
            i = i3;
            j5 = j11;
            j2 = j13;
            j3 = j3;
        }
        return (int) j8;
    }

    public static int mulAddTo(int[] iArr, int i, int[] iArr2, int i2, int[] iArr3, int i3) {
        long j = ((long) iArr2[i2 + 0]) & M;
        long j2 = ((long) iArr2[i2 + 1]) & M;
        long j3 = ((long) iArr2[i2 + 2]) & M;
        long j4 = ((long) iArr2[i2 + 3]) & M;
        long j5 = ((long) iArr2[i2 + 4]) & M;
        long j6 = ((long) iArr2[i2 + 5]) & M;
        long j7 = ((long) iArr2[i2 + 6]) & M;
        int i4 = 0;
        int i5 = i3;
        long j8 = 0;
        while (i4 < 7) {
            long j9 = ((long) iArr[i + i4]) & M;
            int i6 = i5 + 0;
            long j10 = j;
            long j11 = 0 + (j9 * j) + (((long) iArr3[i6]) & M);
            int i7 = i4;
            iArr3[i6] = (int) j11;
            int i8 = i5 + 1;
            long j12 = j2;
            long j13 = (j11 >>> 32) + (j9 * j2) + (((long) iArr3[i8]) & M);
            iArr3[i8] = (int) j13;
            int i9 = i5 + 2;
            long j14 = j3;
            long j15 = (j13 >>> 32) + (j9 * j3) + (((long) iArr3[i9]) & M);
            iArr3[i9] = (int) j15;
            int i10 = i5 + 3;
            long j16 = (j15 >>> 32) + (j9 * j4) + (((long) iArr3[i10]) & M);
            iArr3[i10] = (int) j16;
            int i11 = i5 + 4;
            long j17 = (j16 >>> 32) + (j9 * j5) + (((long) iArr3[i11]) & M);
            iArr3[i11] = (int) j17;
            int i12 = i5 + 5;
            long j18 = (j17 >>> 32) + (j9 * j6) + (((long) iArr3[i12]) & M);
            iArr3[i12] = (int) j18;
            int i13 = i5 + 6;
            long j19 = (j18 >>> 32) + (j9 * j7) + (((long) iArr3[i13]) & M);
            iArr3[i13] = (int) j19;
            int i14 = i5 + 7;
            long j20 = (j19 >>> 32) + j8 + (((long) iArr3[i14]) & M);
            iArr3[i14] = (int) j20;
            j8 = j20 >>> 32;
            i4 = i7 + 1;
            i5 = i8;
            j = j10;
            j2 = j12;
            j3 = j14;
            j4 = j4;
        }
        return (int) j8;
    }

    public static long mul33Add(int i, int[] iArr, int i2, int[] iArr2, int i3, int[] iArr3, int i4) {
        long j = ((long) i) & M;
        long j2 = ((long) iArr[i2 + 0]) & M;
        long j3 = 0 + (j * j2) + (((long) iArr2[i3 + 0]) & M);
        iArr3[i4 + 0] = (int) j3;
        long j4 = ((long) iArr[i2 + 1]) & M;
        long j5 = (j3 >>> 32) + (j * j4) + j2 + (((long) iArr2[i3 + 1]) & M);
        iArr3[i4 + 1] = (int) j5;
        long j6 = ((long) iArr[i2 + 2]) & M;
        long j7 = (j5 >>> 32) + (j * j6) + j4 + (((long) iArr2[i3 + 2]) & M);
        iArr3[i4 + 2] = (int) j7;
        long j8 = ((long) iArr[i2 + 3]) & M;
        long j9 = (j7 >>> 32) + (j * j8) + j6 + (((long) iArr2[i3 + 3]) & M);
        iArr3[i4 + 3] = (int) j9;
        long j10 = ((long) iArr[i2 + 4]) & M;
        long j11 = (j9 >>> 32) + (j * j10) + j8 + (((long) iArr2[i3 + 4]) & M);
        iArr3[i4 + 4] = (int) j11;
        long j12 = ((long) iArr[i2 + 5]) & M;
        long j13 = (j11 >>> 32) + (j * j12) + j10 + (((long) iArr2[i3 + 5]) & M);
        iArr3[i4 + 5] = (int) j13;
        long j14 = ((long) iArr[i2 + 6]) & M;
        long j15 = (j13 >>> 32) + (j * j14) + j12 + (((long) iArr2[i3 + 6]) & M);
        iArr3[i4 + 6] = (int) j15;
        return (j15 >>> 32) + j14;
    }

    public static int mulByWord(int i, int[] iArr) {
        long j = ((long) i) & M;
        long j2 = 0 + ((((long) iArr[0]) & M) * j);
        iArr[0] = (int) j2;
        long j3 = (j2 >>> 32) + ((((long) iArr[1]) & M) * j);
        iArr[1] = (int) j3;
        long j4 = (j3 >>> 32) + ((((long) iArr[2]) & M) * j);
        iArr[2] = (int) j4;
        long j5 = (j4 >>> 32) + ((((long) iArr[3]) & M) * j);
        iArr[3] = (int) j5;
        long j6 = (j5 >>> 32) + ((((long) iArr[4]) & M) * j);
        iArr[4] = (int) j6;
        long j7 = (j6 >>> 32) + ((((long) iArr[5]) & M) * j);
        iArr[5] = (int) j7;
        long j8 = (j7 >>> 32) + (j * (M & ((long) iArr[6])));
        iArr[6] = (int) j8;
        return (int) (j8 >>> 32);
    }

    public static int mulByWordAddTo(int i, int[] iArr, int[] iArr2) {
        long j = ((long) i) & M;
        long j2 = 0 + ((((long) iArr2[0]) & M) * j) + (((long) iArr[0]) & M);
        iArr2[0] = (int) j2;
        long j3 = (j2 >>> 32) + ((((long) iArr2[1]) & M) * j) + (((long) iArr[1]) & M);
        iArr2[1] = (int) j3;
        long j4 = (j3 >>> 32) + ((((long) iArr2[2]) & M) * j) + (((long) iArr[2]) & M);
        iArr2[2] = (int) j4;
        long j5 = (j4 >>> 32) + ((((long) iArr2[3]) & M) * j) + (((long) iArr[3]) & M);
        iArr2[3] = (int) j5;
        long j6 = (j5 >>> 32) + ((((long) iArr2[4]) & M) * j) + (((long) iArr[4]) & M);
        iArr2[4] = (int) j6;
        long j7 = (j6 >>> 32) + ((((long) iArr2[5]) & M) * j) + (((long) iArr[5]) & M);
        iArr2[5] = (int) j7;
        long j8 = (j7 >>> 32) + (j * (((long) iArr2[6]) & M)) + (M & ((long) iArr[6]));
        iArr2[6] = (int) j8;
        return (int) (j8 >>> 32);
    }

    public static int mulWordAddTo(int i, int[] iArr, int i2, int[] iArr2, int i3) {
        long j = ((long) i) & M;
        int i4 = i3 + 0;
        long j2 = 0 + ((((long) iArr[i2 + 0]) & M) * j) + (((long) iArr2[i4]) & M);
        iArr2[i4] = (int) j2;
        int i5 = i3 + 1;
        long j3 = (j2 >>> 32) + ((((long) iArr[i2 + 1]) & M) * j) + (((long) iArr2[i5]) & M);
        iArr2[i5] = (int) j3;
        int i6 = i3 + 2;
        long j4 = (j3 >>> 32) + ((((long) iArr[i2 + 2]) & M) * j) + (((long) iArr2[i6]) & M);
        iArr2[i6] = (int) j4;
        int i7 = i3 + 3;
        long j5 = (j4 >>> 32) + ((((long) iArr[i2 + 3]) & M) * j) + (((long) iArr2[i7]) & M);
        iArr2[i7] = (int) j5;
        int i8 = i3 + 4;
        long j6 = (j5 >>> 32) + ((((long) iArr[i2 + 4]) & M) * j) + (((long) iArr2[i8]) & M);
        iArr2[i8] = (int) j6;
        int i9 = i3 + 5;
        long j7 = (j6 >>> 32) + ((((long) iArr[i2 + 5]) & M) * j) + (((long) iArr2[i9]) & M);
        iArr2[i9] = (int) j7;
        int i10 = i3 + 6;
        long j8 = (j7 >>> 32) + (j * (((long) iArr[i2 + 6]) & M)) + (((long) iArr2[i10]) & M);
        iArr2[i10] = (int) j8;
        return (int) (j8 >>> 32);
    }

    public static int mul33DWordAdd(int i, long j, int[] iArr, int i2) {
        long j2 = ((long) i) & M;
        long j3 = j & M;
        int i3 = i2 + 0;
        long j4 = (j2 * j3) + (((long) iArr[i3]) & M) + 0;
        iArr[i3] = (int) j4;
        long j5 = j >>> 32;
        long j6 = (j2 * j5) + j3;
        int i4 = i2 + 1;
        long j7 = (j4 >>> 32) + j6 + (((long) iArr[i4]) & M);
        iArr[i4] = (int) j7;
        int i5 = i2 + 2;
        long j8 = (j7 >>> 32) + j5 + (((long) iArr[i5]) & M);
        iArr[i5] = (int) j8;
        long j9 = j8 >>> 32;
        int i6 = i2 + 3;
        long j10 = j9 + (((long) iArr[i6]) & M);
        iArr[i6] = (int) j10;
        if ((j10 >>> 32) == 0) {
            return 0;
        }
        return Nat.incAt(7, iArr, i2, 4);
    }

    public static int mul33WordAdd(int i, int i2, int[] iArr, int i3) {
        long j = ((long) i) & M;
        long j2 = ((long) i2) & M;
        int i4 = i3 + 0;
        long j3 = (j * j2) + (((long) iArr[i4]) & M) + 0;
        iArr[i4] = (int) j3;
        int i5 = i3 + 1;
        long j4 = (j3 >>> 32) + j2 + (((long) iArr[i5]) & M);
        iArr[i5] = (int) j4;
        long j5 = j4 >>> 32;
        int i6 = i3 + 2;
        long j6 = j5 + (((long) iArr[i6]) & M);
        iArr[i6] = (int) j6;
        if ((j6 >>> 32) == 0) {
            return 0;
        }
        return Nat.incAt(7, iArr, i3, 3);
    }

    public static int mulWordDwordAdd(int i, long j, int[] iArr, int i2) {
        long j2 = ((long) i) & M;
        int i3 = i2 + 0;
        long j3 = ((j & M) * j2) + (((long) iArr[i3]) & M) + 0;
        iArr[i3] = (int) j3;
        long j4 = j2 * (j >>> 32);
        int i4 = i2 + 1;
        long j5 = (j3 >>> 32) + j4 + (((long) iArr[i4]) & M);
        iArr[i4] = (int) j5;
        int i5 = i2 + 2;
        long j6 = (j5 >>> 32) + (((long) iArr[i5]) & M);
        iArr[i5] = (int) j6;
        if ((j6 >>> 32) == 0) {
            return 0;
        }
        return Nat.incAt(7, iArr, i2, 3);
    }

    public static int mulWord(int i, int[] iArr, int[] iArr2, int i2) {
        long j = ((long) i) & M;
        long j2 = 0;
        int i3 = 0;
        do {
            long j3 = j2 + ((((long) iArr[i3]) & M) * j);
            iArr2[i2 + i3] = (int) j3;
            j2 = j3 >>> 32;
            i3++;
        } while (i3 < 7);
        return (int) j2;
    }

    public static void square(int[] iArr, int[] iArr2) {
        long j = ((long) iArr[0]) & M;
        int i = 0;
        int i2 = 14;
        int i3 = 6;
        while (true) {
            int i4 = i3 - 1;
            long j2 = ((long) iArr[i3]) & M;
            long j3 = j2 * j2;
            int i5 = i2 - 1;
            iArr2[i5] = (i << 31) | ((int) (j3 >>> 33));
            i2 = i5 - 1;
            iArr2[i2] = (int) (j3 >>> 1);
            int i6 = (int) j3;
            if (i4 > 0) {
                i3 = i4;
                i = i6;
            } else {
                long j4 = j * j;
                long j5 = (j4 >>> 33) | (((long) (i6 << 31)) & M);
                iArr2[0] = (int) j4;
                long j6 = ((long) iArr[1]) & M;
                long j7 = ((long) iArr2[2]) & M;
                long j8 = j5 + (j6 * j);
                int i7 = (int) j8;
                iArr2[1] = (i7 << 1) | (((int) (j4 >>> 32)) & 1);
                long j9 = j7 + (j8 >>> 32);
                long j10 = ((long) iArr[2]) & M;
                long j11 = ((long) iArr2[3]) & M;
                long j12 = ((long) iArr2[4]) & M;
                long j13 = j9 + (j10 * j);
                int i8 = (int) j13;
                iArr2[2] = (i7 >>> 31) | (i8 << 1);
                long j14 = j11 + (j13 >>> 32) + (j10 * j6);
                long j15 = j12 + (j14 >>> 32);
                long j16 = j14 & M;
                long j17 = ((long) iArr[3]) & M;
                long j18 = (((long) iArr2[5]) & M) + (j15 >>> 32);
                long j19 = j15 & M;
                long j20 = (((long) iArr2[6]) & M) + (j18 >>> 32);
                long j21 = j18 & M;
                long j22 = j16 + (j17 * j);
                int i9 = (int) j22;
                iArr2[3] = (i8 >>> 31) | (i9 << 1);
                long j23 = j19 + (j22 >>> 32) + (j17 * j6);
                long j24 = j21 + (j23 >>> 32) + (j17 * j10);
                long j25 = j23 & M;
                long j26 = j20 + (j24 >>> 32);
                long j27 = j24 & M;
                long j28 = ((long) iArr[4]) & M;
                long j29 = (((long) iArr2[7]) & M) + (j26 >>> 32);
                long j30 = j26 & M;
                long j31 = (((long) iArr2[8]) & M) + (j29 >>> 32);
                long j32 = j29 & M;
                long j33 = j25 + (j28 * j);
                int i10 = (int) j33;
                iArr2[4] = (i9 >>> 31) | (i10 << 1);
                long j34 = j27 + (j33 >>> 32) + (j28 * j6);
                long j35 = j30 + (j34 >>> 32) + (j28 * j10);
                long j36 = j34 & M;
                long j37 = j32 + (j35 >>> 32) + (j28 * j17);
                long j38 = j35 & M;
                long j39 = j31 + (j37 >>> 32);
                long j40 = j37 & M;
                long j41 = ((long) iArr[5]) & M;
                long j42 = (((long) iArr2[9]) & M) + (j39 >>> 32);
                long j43 = j39 & M;
                long j44 = (((long) iArr2[10]) & M) + (j42 >>> 32);
                long j45 = j42 & M;
                long j46 = j36 + (j41 * j);
                int i11 = (int) j46;
                iArr2[5] = (i10 >>> 31) | (i11 << 1);
                int i12 = i11 >>> 31;
                long j47 = j38 + (j46 >>> 32) + (j41 * j6);
                long j48 = j40 + (j47 >>> 32) + (j41 * j10);
                long j49 = j47 & M;
                long j50 = j43 + (j48 >>> 32) + (j41 * j17);
                long j51 = j48 & M;
                long j52 = j45 + (j50 >>> 32) + (j41 * j28);
                long j53 = j50 & M;
                long j54 = j44 + (j52 >>> 32);
                long j55 = j52 & M;
                long j56 = ((long) iArr[6]) & M;
                long j57 = (((long) iArr2[11]) & M) + (j54 >>> 32);
                long j58 = j54 & M;
                long j59 = (((long) iArr2[12]) & M) + (j57 >>> 32);
                long j60 = j57 & M;
                long j61 = j49 + (j56 * j);
                int i13 = (int) j61;
                iArr2[6] = i12 | (i13 << 1);
                int i14 = i13 >>> 31;
                long j62 = j51 + (j61 >>> 32) + (j56 * j6);
                long j63 = j53 + (j62 >>> 32) + (j56 * j10);
                long j64 = j55 + (j63 >>> 32) + (j56 * j17);
                long j65 = j58 + (j64 >>> 32) + (j56 * j28);
                long j66 = j60 + (j65 >>> 32) + (j56 * j41);
                long j67 = j59 + (j66 >>> 32);
                int i15 = (int) j62;
                iArr2[7] = i14 | (i15 << 1);
                int i16 = (int) j63;
                iArr2[8] = (i15 >>> 31) | (i16 << 1);
                int i17 = i16 >>> 31;
                int i18 = (int) j64;
                iArr2[9] = i17 | (i18 << 1);
                int i19 = i18 >>> 31;
                int i20 = (int) j65;
                iArr2[10] = i19 | (i20 << 1);
                int i21 = i20 >>> 31;
                int i22 = (int) j66;
                iArr2[11] = i21 | (i22 << 1);
                int i23 = i22 >>> 31;
                int i24 = (int) j67;
                iArr2[12] = i23 | (i24 << 1);
                iArr2[13] = (i24 >>> 31) | ((iArr2[13] + ((int) (j67 >>> 32))) << 1);
                return;
            }
        }
    }

    public static void square(int[] iArr, int i, int[] iArr2, int i2) {
        long j = iArr[i + 0];
        long j2 = M;
        long j3 = j & M;
        int i3 = 14;
        int i4 = 0;
        int i5 = 6;
        while (true) {
            int i6 = i5 - 1;
            long j4 = ((long) iArr[i + i5]) & j2;
            long j5 = j4 * j4;
            int i7 = i3 - 1;
            iArr2[i2 + i7] = ((int) (j5 >>> 33)) | (i4 << 31);
            i3 = i7 - 1;
            iArr2[i2 + i3] = (int) (j5 >>> 1);
            i4 = (int) j5;
            if (i6 > 0) {
                i5 = i6;
                j2 = 4294967295L;
            } else {
                long j6 = j3 * j3;
                long j7 = (((long) (i4 << 31)) & M) | (j6 >>> 33);
                iArr2[i2 + 0] = (int) j6;
                long j8 = ((long) iArr[i + 1]) & M;
                int i8 = i2 + 2;
                long j9 = ((long) iArr2[i8]) & M;
                long j10 = j7 + (j8 * j3);
                int i9 = (int) j10;
                iArr2[i2 + 1] = (i9 << 1) | (((int) (j6 >>> 32)) & 1);
                long j11 = j9 + (j10 >>> 32);
                long j12 = ((long) iArr[i + 2]) & M;
                int i10 = i2 + 3;
                long j13 = ((long) iArr2[i10]) & M;
                int i11 = i2 + 4;
                long j14 = ((long) iArr2[i11]) & M;
                long j15 = j11 + (j12 * j3);
                int i12 = (int) j15;
                iArr2[i8] = (i9 >>> 31) | (i12 << 1);
                long j16 = j13 + (j15 >>> 32) + (j12 * j8);
                long j17 = j14 + (j16 >>> 32);
                long j18 = j16 & M;
                long j19 = ((long) iArr[i + 3]) & M;
                int i13 = i2 + 5;
                long j20 = (((long) iArr2[i13]) & M) + (j17 >>> 32);
                long j21 = j17 & M;
                int i14 = i2 + 6;
                long j22 = (((long) iArr2[i14]) & M) + (j20 >>> 32);
                long j23 = j20 & M;
                long j24 = j18 + (j19 * j3);
                int i15 = (int) j24;
                iArr2[i10] = (i12 >>> 31) | (i15 << 1);
                int i16 = i15 >>> 31;
                long j25 = j21 + (j24 >>> 32) + (j19 * j8);
                long j26 = j23 + (j25 >>> 32) + (j19 * j12);
                long j27 = j25 & M;
                long j28 = j22 + (j26 >>> 32);
                long j29 = j26 & M;
                long j30 = ((long) iArr[i + 4]) & M;
                int i17 = i2 + 7;
                long j31 = (((long) iArr2[i17]) & M) + (j28 >>> 32);
                long j32 = j28 & M;
                int i18 = i2 + 8;
                long j33 = (((long) iArr2[i18]) & M) + (j31 >>> 32);
                long j34 = j31 & M;
                long j35 = j27 + (j30 * j3);
                int i19 = (int) j35;
                iArr2[i11] = i16 | (i19 << 1);
                int i20 = i19 >>> 31;
                long j36 = j29 + (j35 >>> 32) + (j30 * j8);
                long j37 = j32 + (j36 >>> 32) + (j30 * j12);
                long j38 = j36 & M;
                long j39 = j34 + (j37 >>> 32) + (j30 * j19);
                long j40 = j37 & M;
                long j41 = j33 + (j39 >>> 32);
                long j42 = j39 & M;
                long j43 = ((long) iArr[i + 5]) & M;
                int i21 = i2 + 9;
                long j44 = (((long) iArr2[i21]) & M) + (j41 >>> 32);
                long j45 = j41 & M;
                int i22 = i2 + 10;
                long j46 = (((long) iArr2[i22]) & M) + (j44 >>> 32);
                long j47 = j44 & M;
                long j48 = j38 + (j43 * j3);
                int i23 = (int) j48;
                iArr2[i13] = (i23 << 1) | i20;
                int i24 = i23 >>> 31;
                long j49 = j40 + (j48 >>> 32) + (j43 * j8);
                long j50 = j42 + (j49 >>> 32) + (j43 * j12);
                long j51 = j49 & M;
                long j52 = j45 + (j50 >>> 32) + (j43 * j19);
                long j53 = j50 & M;
                long j54 = j47 + (j52 >>> 32) + (j43 * j30);
                long j55 = j52 & M;
                long j56 = j46 + (j54 >>> 32);
                long j57 = j54 & M;
                long j58 = ((long) iArr[i + 6]) & M;
                int i25 = i2 + 11;
                long j59 = (((long) iArr2[i25]) & M) + (j56 >>> 32);
                long j60 = j56 & M;
                int i26 = i2 + 12;
                long j61 = (((long) iArr2[i26]) & M) + (j59 >>> 32);
                long j62 = j59 & M;
                long j63 = j51 + (j58 * j3);
                int i27 = (int) j63;
                iArr2[i14] = i24 | (i27 << 1);
                int i28 = i27 >>> 31;
                long j64 = j53 + (j63 >>> 32) + (j8 * j58);
                long j65 = j55 + (j64 >>> 32) + (j58 * j12);
                long j66 = j57 + (j65 >>> 32) + (j58 * j19);
                long j67 = j60 + (j66 >>> 32) + (j58 * j30);
                long j68 = j62 + (j67 >>> 32) + (j58 * j43);
                long j69 = j61 + (j68 >>> 32);
                int i29 = (int) j64;
                iArr2[i17] = i28 | (i29 << 1);
                int i30 = i29 >>> 31;
                int i31 = (int) j65;
                iArr2[i18] = i30 | (i31 << 1);
                int i32 = i31 >>> 31;
                int i33 = (int) j66;
                iArr2[i21] = i32 | (i33 << 1);
                int i34 = i33 >>> 31;
                int i35 = (int) j67;
                iArr2[i22] = i34 | (i35 << 1);
                int i36 = i35 >>> 31;
                int i37 = (int) j68;
                iArr2[i25] = i36 | (i37 << 1);
                int i38 = i37 >>> 31;
                int i39 = (int) j69;
                iArr2[i26] = i38 | (i39 << 1);
                int i40 = i39 >>> 31;
                int i41 = i2 + 13;
                iArr2[i41] = ((iArr2[i41] + ((int) (j69 >>> 32))) << 1) | i40;
                return;
            }
        }
    }

    public static int sub(int[] iArr, int[] iArr2, int[] iArr3) {
        long j = 0 + ((((long) iArr[0]) & M) - (((long) iArr2[0]) & M));
        iArr3[0] = (int) j;
        long j2 = (j >> 32) + ((((long) iArr[1]) & M) - (((long) iArr2[1]) & M));
        iArr3[1] = (int) j2;
        long j3 = (j2 >> 32) + ((((long) iArr[2]) & M) - (((long) iArr2[2]) & M));
        iArr3[2] = (int) j3;
        long j4 = (j3 >> 32) + ((((long) iArr[3]) & M) - (((long) iArr2[3]) & M));
        iArr3[3] = (int) j4;
        long j5 = (j4 >> 32) + ((((long) iArr[4]) & M) - (((long) iArr2[4]) & M));
        iArr3[4] = (int) j5;
        long j6 = (j5 >> 32) + ((((long) iArr[5]) & M) - (((long) iArr2[5]) & M));
        iArr3[5] = (int) j6;
        long j7 = (j6 >> 32) + ((((long) iArr[6]) & M) - (((long) iArr2[6]) & M));
        iArr3[6] = (int) j7;
        return (int) (j7 >> 32);
    }

    public static int sub(int[] iArr, int i, int[] iArr2, int i2, int[] iArr3, int i3) {
        long j = 0 + ((((long) iArr[i + 0]) & M) - (((long) iArr2[i2 + 0]) & M));
        iArr3[i3 + 0] = (int) j;
        long j2 = (j >> 32) + ((((long) iArr[i + 1]) & M) - (((long) iArr2[i2 + 1]) & M));
        iArr3[i3 + 1] = (int) j2;
        long j3 = (j2 >> 32) + ((((long) iArr[i + 2]) & M) - (((long) iArr2[i2 + 2]) & M));
        iArr3[i3 + 2] = (int) j3;
        long j4 = (j3 >> 32) + ((((long) iArr[i + 3]) & M) - (((long) iArr2[i2 + 3]) & M));
        iArr3[i3 + 3] = (int) j4;
        long j5 = (j4 >> 32) + ((((long) iArr[i + 4]) & M) - (((long) iArr2[i2 + 4]) & M));
        iArr3[i3 + 4] = (int) j5;
        long j6 = (j5 >> 32) + ((((long) iArr[i + 5]) & M) - (((long) iArr2[i2 + 5]) & M));
        iArr3[i3 + 5] = (int) j6;
        long j7 = (j6 >> 32) + ((((long) iArr[i + 6]) & M) - (((long) iArr2[i2 + 6]) & M));
        iArr3[i3 + 6] = (int) j7;
        return (int) (j7 >> 32);
    }

    public static int subBothFrom(int[] iArr, int[] iArr2, int[] iArr3) {
        long j = 0 + (((((long) iArr3[0]) & M) - (((long) iArr[0]) & M)) - (((long) iArr2[0]) & M));
        iArr3[0] = (int) j;
        long j2 = (j >> 32) + (((((long) iArr3[1]) & M) - (((long) iArr[1]) & M)) - (((long) iArr2[1]) & M));
        iArr3[1] = (int) j2;
        long j3 = (j2 >> 32) + (((((long) iArr3[2]) & M) - (((long) iArr[2]) & M)) - (((long) iArr2[2]) & M));
        iArr3[2] = (int) j3;
        long j4 = (j3 >> 32) + (((((long) iArr3[3]) & M) - (((long) iArr[3]) & M)) - (((long) iArr2[3]) & M));
        iArr3[3] = (int) j4;
        long j5 = (j4 >> 32) + (((((long) iArr3[4]) & M) - (((long) iArr[4]) & M)) - (((long) iArr2[4]) & M));
        iArr3[4] = (int) j5;
        long j6 = (j5 >> 32) + (((((long) iArr3[5]) & M) - (((long) iArr[5]) & M)) - (((long) iArr2[5]) & M));
        iArr3[5] = (int) j6;
        long j7 = (j6 >> 32) + (((((long) iArr3[6]) & M) - (((long) iArr[6]) & M)) - (((long) iArr2[6]) & M));
        iArr3[6] = (int) j7;
        return (int) (j7 >> 32);
    }

    public static int subFrom(int[] iArr, int[] iArr2) {
        long j = 0 + ((((long) iArr2[0]) & M) - (((long) iArr[0]) & M));
        iArr2[0] = (int) j;
        long j2 = (j >> 32) + ((((long) iArr2[1]) & M) - (((long) iArr[1]) & M));
        iArr2[1] = (int) j2;
        long j3 = (j2 >> 32) + ((((long) iArr2[2]) & M) - (((long) iArr[2]) & M));
        iArr2[2] = (int) j3;
        long j4 = (j3 >> 32) + ((((long) iArr2[3]) & M) - (((long) iArr[3]) & M));
        iArr2[3] = (int) j4;
        long j5 = (j4 >> 32) + ((((long) iArr2[4]) & M) - (((long) iArr[4]) & M));
        iArr2[4] = (int) j5;
        long j6 = (j5 >> 32) + ((((long) iArr2[5]) & M) - (((long) iArr[5]) & M));
        iArr2[5] = (int) j6;
        long j7 = (j6 >> 32) + ((((long) iArr2[6]) & M) - (M & ((long) iArr[6])));
        iArr2[6] = (int) j7;
        return (int) (j7 >> 32);
    }

    public static int subFrom(int[] iArr, int i, int[] iArr2, int i2) {
        int i3 = i2 + 0;
        long j = 0 + ((((long) iArr2[i3]) & M) - (((long) iArr[i + 0]) & M));
        iArr2[i3] = (int) j;
        long j2 = j >> 32;
        int i4 = i2 + 1;
        long j3 = j2 + ((((long) iArr2[i4]) & M) - (((long) iArr[i + 1]) & M));
        iArr2[i4] = (int) j3;
        int i5 = i2 + 2;
        long j4 = (j3 >> 32) + ((((long) iArr2[i5]) & M) - (((long) iArr[i + 2]) & M));
        iArr2[i5] = (int) j4;
        int i6 = i2 + 3;
        long j5 = (j4 >> 32) + ((((long) iArr2[i6]) & M) - (((long) iArr[i + 3]) & M));
        iArr2[i6] = (int) j5;
        int i7 = i2 + 4;
        long j6 = (j5 >> 32) + ((((long) iArr2[i7]) & M) - (((long) iArr[i + 4]) & M));
        iArr2[i7] = (int) j6;
        int i8 = i2 + 5;
        long j7 = (j6 >> 32) + ((((long) iArr2[i8]) & M) - (((long) iArr[i + 5]) & M));
        iArr2[i8] = (int) j7;
        int i9 = i2 + 6;
        long j8 = (j7 >> 32) + ((((long) iArr2[i9]) & M) - (((long) iArr[i + 6]) & M));
        iArr2[i9] = (int) j8;
        return (int) (j8 >> 32);
    }

    public static BigInteger toBigInteger(int[] iArr) {
        byte[] bArr = new byte[28];
        for (int i = 0; i < 7; i++) {
            int i2 = iArr[i];
            if (i2 != 0) {
                Pack.intToBigEndian(i2, bArr, (6 - i) << 2);
            }
        }
        return new BigInteger(1, bArr);
    }

    public static void zero(int[] iArr) {
        iArr[0] = 0;
        iArr[1] = 0;
        iArr[2] = 0;
        iArr[3] = 0;
        iArr[4] = 0;
        iArr[5] = 0;
        iArr[6] = 0;
    }
}
