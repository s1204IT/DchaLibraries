package com.android.org.bouncycastle.math.ec.custom.sec;

import com.android.org.bouncycastle.math.raw.Nat;
import com.android.org.bouncycastle.math.raw.Nat512;
import java.math.BigInteger;

public class SecP521R1Field {
    private static final int P16 = 511;
    static final int[] P = {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, P16};

    public static void add(int[] iArr, int[] iArr2, int[] iArr3) {
        int iAdd = Nat.add(16, iArr, iArr2, iArr3) + iArr[16] + iArr2[16];
        if (iAdd > P16 || (iAdd == P16 && Nat.eq(16, iArr3, P))) {
            iAdd = (iAdd + Nat.inc(16, iArr3)) & P16;
        }
        iArr3[16] = iAdd;
    }

    public static void addOne(int[] iArr, int[] iArr2) {
        int iInc = Nat.inc(16, iArr, iArr2) + iArr[16];
        if (iInc > P16 || (iInc == P16 && Nat.eq(16, iArr2, P))) {
            iInc = (iInc + Nat.inc(16, iArr2)) & P16;
        }
        iArr2[16] = iInc;
    }

    public static int[] fromBigInteger(BigInteger bigInteger) {
        int[] iArrFromBigInteger = Nat.fromBigInteger(521, bigInteger);
        if (Nat.eq(17, iArrFromBigInteger, P)) {
            Nat.zero(17, iArrFromBigInteger);
        }
        return iArrFromBigInteger;
    }

    public static void half(int[] iArr, int[] iArr2) {
        int i = iArr[16];
        iArr2[16] = (Nat.shiftDownBit(16, iArr, i, iArr2) >>> 23) | (i >>> 1);
    }

    public static void multiply(int[] iArr, int[] iArr2, int[] iArr3) {
        int[] iArrCreate = Nat.create(33);
        implMultiply(iArr, iArr2, iArrCreate);
        reduce(iArrCreate, iArr3);
    }

    public static void negate(int[] iArr, int[] iArr2) {
        if (!Nat.isZero(17, iArr)) {
            Nat.sub(17, P, iArr, iArr2);
        } else {
            Nat.zero(17, iArr2);
        }
    }

    public static void reduce(int[] iArr, int[] iArr2) {
        int i = iArr[32];
        int iShiftDownBits = (Nat.shiftDownBits(16, iArr, 16, 9, i, iArr2, 0) >>> 23) + (i >>> 9) + Nat.addTo(16, iArr, iArr2);
        if (iShiftDownBits > P16 || (iShiftDownBits == P16 && Nat.eq(16, iArr2, P))) {
            iShiftDownBits = (iShiftDownBits + Nat.inc(16, iArr2)) & P16;
        }
        iArr2[16] = iShiftDownBits;
    }

    public static void reduce23(int[] iArr) {
        int i = iArr[16];
        int iAddWordTo = Nat.addWordTo(16, i >>> 9, iArr) + (i & P16);
        if (iAddWordTo > P16 || (iAddWordTo == P16 && Nat.eq(16, iArr, P))) {
            iAddWordTo = (iAddWordTo + Nat.inc(16, iArr)) & P16;
        }
        iArr[16] = iAddWordTo;
    }

    public static void square(int[] iArr, int[] iArr2) {
        int[] iArrCreate = Nat.create(33);
        implSquare(iArr, iArrCreate);
        reduce(iArrCreate, iArr2);
    }

    public static void squareN(int[] iArr, int i, int[] iArr2) {
        int[] iArrCreate = Nat.create(33);
        implSquare(iArr, iArrCreate);
        reduce(iArrCreate, iArr2);
        while (true) {
            i--;
            if (i > 0) {
                implSquare(iArr2, iArrCreate);
                reduce(iArrCreate, iArr2);
            } else {
                return;
            }
        }
    }

    public static void subtract(int[] iArr, int[] iArr2, int[] iArr3) {
        int iSub = (Nat.sub(16, iArr, iArr2, iArr3) + iArr[16]) - iArr2[16];
        if (iSub < 0) {
            iSub = (iSub + Nat.dec(16, iArr3)) & P16;
        }
        iArr3[16] = iSub;
    }

    public static void twice(int[] iArr, int[] iArr2) {
        int i = iArr[16];
        iArr2[16] = (Nat.shiftUpBit(16, iArr, i << 23, iArr2) | (i << 1)) & P16;
    }

    protected static void implMultiply(int[] iArr, int[] iArr2, int[] iArr3) {
        Nat512.mul(iArr, iArr2, iArr3);
        int i = iArr[16];
        int i2 = iArr2[16];
        iArr3[32] = Nat.mul31BothAdd(16, i, iArr2, i2, iArr, iArr3, 16) + (i * i2);
    }

    protected static void implSquare(int[] iArr, int[] iArr2) {
        Nat512.square(iArr, iArr2);
        int i = iArr[16];
        iArr2[32] = Nat.mulWordAddTo(16, i << 1, iArr, 0, iArr2, 16) + (i * i);
    }
}
