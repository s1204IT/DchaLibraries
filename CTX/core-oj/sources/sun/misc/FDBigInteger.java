package sun.misc;

import java.math.BigInteger;
import java.util.Arrays;

public class FDBigInteger {
    static final boolean $assertionsDisabled = false;
    private static final long LONG_MASK = 4294967295L;
    public static final FDBigInteger ZERO;
    private int[] data;
    private boolean isImmutable = $assertionsDisabled;
    private int nWords;
    private int offset;
    static final int[] SMALL_5_POW = {1, 5, 25, 125, 625, 3125, 15625, 78125, 390625, 1953125, 9765625, 48828125, 244140625, 1220703125};
    static final long[] LONG_5_POW = {1, 5, 25, 125, 625, 3125, 15625, 78125, 390625, 1953125, 9765625, 48828125, 244140625, 1220703125, 6103515625L, 30517578125L, 152587890625L, 762939453125L, 3814697265625L, 19073486328125L, 95367431640625L, 476837158203125L, 2384185791015625L, 11920928955078125L, 59604644775390625L, 298023223876953125L, 1490116119384765625L};
    private static final int MAX_FIVE_POW = 340;
    private static final FDBigInteger[] POW_5_CACHE = new FDBigInteger[MAX_FIVE_POW];

    static {
        int i = 0;
        while (i < SMALL_5_POW.length) {
            FDBigInteger fDBigInteger = new FDBigInteger(new int[]{SMALL_5_POW[i]}, 0);
            fDBigInteger.makeImmutable();
            POW_5_CACHE[i] = fDBigInteger;
            i++;
        }
        FDBigInteger fDBigIntegerMult = POW_5_CACHE[i - 1];
        while (i < MAX_FIVE_POW) {
            FDBigInteger[] fDBigIntegerArr = POW_5_CACHE;
            fDBigIntegerMult = fDBigIntegerMult.mult(5);
            fDBigIntegerArr[i] = fDBigIntegerMult;
            fDBigIntegerMult.makeImmutable();
            i++;
        }
        ZERO = new FDBigInteger(new int[0], 0);
        ZERO.makeImmutable();
    }

    private FDBigInteger(int[] iArr, int i) {
        this.data = iArr;
        this.offset = i;
        this.nWords = iArr.length;
        trimLeadingZeros();
    }

    public FDBigInteger(long j, char[] cArr, int i, int i2) {
        int i3 = 0;
        this.data = new int[Math.max((i2 + 8) / 9, 2)];
        this.data[0] = (int) j;
        this.data[1] = (int) (j >>> 32);
        this.offset = 0;
        this.nWords = 2;
        int i4 = i2 - 5;
        while (i < i4) {
            int i5 = i + 5;
            int i6 = cArr[i] - '0';
            i++;
            while (i < i5) {
                i6 = ((i6 * 10) + cArr[i]) - 48;
                i++;
            }
            multAddMe(100000, i6);
        }
        int i7 = 1;
        while (i < i2) {
            i3 = ((i3 * 10) + cArr[i]) - 48;
            i7 *= 10;
            i++;
        }
        if (i7 != 1) {
            multAddMe(i7, i3);
        }
        trimLeadingZeros();
    }

    public static FDBigInteger valueOfPow52(int i, int i2) {
        if (i != 0) {
            if (i2 == 0) {
                return big5pow(i);
            }
            if (i < SMALL_5_POW.length) {
                int i3 = SMALL_5_POW[i];
                int i4 = i2 >> 5;
                int i5 = i2 & 31;
                if (i5 == 0) {
                    return new FDBigInteger(new int[]{i3}, i4);
                }
                return new FDBigInteger(new int[]{i3 << i5, i3 >>> (32 - i5)}, i4);
            }
            return big5pow(i).leftShift(i2);
        }
        return valueOfPow2(i2);
    }

    public static FDBigInteger valueOfMulPow52(long j, int i, int i2) {
        int[] iArr;
        int i3 = (int) j;
        int i4 = (int) (j >>> 32);
        int i5 = i2 >> 5;
        int i6 = i2 & 31;
        if (i != 0) {
            if (i < SMALL_5_POW.length) {
                long j2 = ((long) SMALL_5_POW[i]) & LONG_MASK;
                long j3 = (((long) i3) & LONG_MASK) * j2;
                int i7 = (int) j3;
                long j4 = ((((long) i4) & LONG_MASK) * j2) + (j3 >>> 32);
                int i8 = (int) j4;
                int i9 = (int) (j4 >>> 32);
                if (i6 != 0) {
                    int i10 = 32 - i6;
                    return new FDBigInteger(new int[]{i7 << i6, (i7 >>> i10) | (i8 << i6), (i8 >>> i10) | (i9 << i6), i9 >>> i10}, i5);
                }
                return new FDBigInteger(new int[]{i7, i8, i9}, i5);
            }
            FDBigInteger fDBigIntegerBig5pow = big5pow(i);
            if (i4 == 0) {
                iArr = new int[fDBigIntegerBig5pow.nWords + 1 + (i2 != 0 ? 1 : 0)];
                mult(fDBigIntegerBig5pow.data, fDBigIntegerBig5pow.nWords, i3, iArr);
            } else {
                int[] iArr2 = new int[fDBigIntegerBig5pow.nWords + 2 + (i2 != 0 ? 1 : 0)];
                mult(fDBigIntegerBig5pow.data, fDBigIntegerBig5pow.nWords, i3, i4, iArr2);
                iArr = iArr2;
            }
            return new FDBigInteger(iArr, fDBigIntegerBig5pow.offset).leftShift(i2);
        }
        if (i2 == 0) {
            return new FDBigInteger(new int[]{i3, i4}, 0);
        }
        if (i6 != 0) {
            int i11 = 32 - i6;
            return new FDBigInteger(new int[]{i3 << i6, (i3 >>> i11) | (i4 << i6), i4 >>> i11}, i5);
        }
        return new FDBigInteger(new int[]{i3, i4}, i5);
    }

    private static FDBigInteger valueOfPow2(int i) {
        return new FDBigInteger(new int[]{1 << (i & 31)}, i >> 5);
    }

    private void trimLeadingZeros() {
        int i = this.nWords;
        if (i > 0) {
            int i2 = i - 1;
            if (this.data[i2] == 0) {
                while (i2 > 0 && this.data[i2 - 1] == 0) {
                    i2--;
                }
                this.nWords = i2;
                if (i2 == 0) {
                    this.offset = 0;
                }
            }
        }
    }

    public int getNormalizationBias() {
        if (this.nWords == 0) {
            throw new IllegalArgumentException("Zero value cannot be normalized");
        }
        int iNumberOfLeadingZeros = Integer.numberOfLeadingZeros(this.data[this.nWords - 1]);
        return iNumberOfLeadingZeros < 4 ? 28 + iNumberOfLeadingZeros : iNumberOfLeadingZeros - 4;
    }

    private static void leftShift(int[] iArr, int i, int[] iArr2, int i2, int i3, int i4) {
        while (i > 0) {
            int i5 = iArr[i - 1];
            iArr2[i] = (i4 << i2) | (i5 >>> i3);
            i--;
            i4 = i5;
        }
        iArr2[0] = i4 << i2;
    }

    public FDBigInteger leftShift(int i) {
        int[] iArr;
        if (i == 0 || this.nWords == 0) {
            return this;
        }
        int i2 = i >> 5;
        int i3 = i & 31;
        if (this.isImmutable) {
            if (i3 == 0) {
                return new FDBigInteger(Arrays.copyOf(this.data, this.nWords), this.offset + i2);
            }
            int i4 = 32 - i3;
            int i5 = this.nWords - 1;
            int i6 = this.data[i5];
            int i7 = i6 >>> i4;
            if (i7 != 0) {
                int[] iArr2 = new int[this.nWords + 1];
                iArr2[this.nWords] = i7;
                iArr = iArr2;
            } else {
                iArr = new int[this.nWords];
            }
            leftShift(this.data, i5, iArr, i3, i4, i6);
            return new FDBigInteger(iArr, this.offset + i2);
        }
        if (i3 != 0) {
            int i8 = 32 - i3;
            int i9 = 0;
            if ((this.data[0] << i3) == 0) {
                int i10 = this.data[0];
                while (i9 < this.nWords - 1) {
                    int i11 = i9 + 1;
                    int i12 = this.data[i11];
                    this.data[i9] = (i10 >>> i8) | (i12 << i3);
                    i10 = i12;
                    i9 = i11;
                }
                int i13 = i10 >>> i8;
                this.data[i9] = i13;
                if (i13 == 0) {
                    this.nWords--;
                }
                this.offset++;
            } else {
                int i14 = this.nWords - 1;
                int i15 = this.data[i14];
                int i16 = i15 >>> i8;
                int[] iArr3 = this.data;
                int[] iArr4 = this.data;
                if (i16 != 0) {
                    if (this.nWords == this.data.length) {
                        iArr3 = new int[this.nWords + 1];
                        this.data = iArr3;
                    }
                    int i17 = this.nWords;
                    this.nWords = i17 + 1;
                    iArr3[i17] = i16;
                }
                leftShift(iArr4, i14, iArr3, i3, i8, i15);
            }
        }
        this.offset += i2;
        return this;
    }

    private int size() {
        return this.nWords + this.offset;
    }

    public int quoRemIteration(FDBigInteger fDBigInteger) throws IllegalArgumentException {
        FDBigInteger fDBigInteger2 = this;
        int size = size();
        int size2 = fDBigInteger.size();
        if (size < size2) {
            int iMultAndCarryBy10 = multAndCarryBy10(fDBigInteger2.data, fDBigInteger2.nWords, fDBigInteger2.data);
            if (iMultAndCarryBy10 != 0) {
                int[] iArr = fDBigInteger2.data;
                int i = fDBigInteger2.nWords;
                fDBigInteger2.nWords = i + 1;
                iArr[i] = iMultAndCarryBy10;
            } else {
                trimLeadingZeros();
            }
            return 0;
        }
        if (size <= size2) {
            long j = (((long) fDBigInteger2.data[fDBigInteger2.nWords - 1]) & LONG_MASK) / (((long) fDBigInteger.data[fDBigInteger.nWords - 1]) & LONG_MASK);
            if (fDBigInteger2.multDiffMe(j, fDBigInteger) != 0) {
                int i2 = fDBigInteger.offset - fDBigInteger2.offset;
                int[] iArr2 = fDBigInteger.data;
                int[] iArr3 = fDBigInteger2.data;
                long j2 = j;
                long j3 = 0;
                for (long j4 = 0; j3 == j4; j4 = 0) {
                    long j5 = j3;
                    int i3 = 0;
                    int i4 = i2;
                    while (i4 < fDBigInteger2.nWords) {
                        long j6 = j5 + (((long) iArr3[i4]) & LONG_MASK) + (((long) iArr2[i3]) & LONG_MASK);
                        iArr3[i4] = (int) j6;
                        j5 = j6 >>> 32;
                        i3++;
                        i4++;
                        iArr2 = iArr2;
                        fDBigInteger2 = this;
                    }
                    j2--;
                    j3 = j5;
                    iArr2 = iArr2;
                    fDBigInteger2 = this;
                }
                j = j2;
            }
            multAndCarryBy10(this.data, this.nWords, this.data);
            trimLeadingZeros();
            return (int) j;
        }
        throw new IllegalArgumentException("disparate values");
    }

    public FDBigInteger multBy10() {
        if (this.nWords == 0) {
            return this;
        }
        if (this.isImmutable) {
            int[] iArr = new int[this.nWords + 1];
            iArr[this.nWords] = multAndCarryBy10(this.data, this.nWords, iArr);
            return new FDBigInteger(iArr, this.offset);
        }
        int iMultAndCarryBy10 = multAndCarryBy10(this.data, this.nWords, this.data);
        if (iMultAndCarryBy10 != 0) {
            if (this.nWords == this.data.length) {
                if (this.data[0] == 0) {
                    int[] iArr2 = this.data;
                    int[] iArr3 = this.data;
                    int i = this.nWords - 1;
                    this.nWords = i;
                    System.arraycopy((Object) iArr2, 1, (Object) iArr3, 0, i);
                    this.offset++;
                } else {
                    this.data = Arrays.copyOf(this.data, this.data.length + 1);
                }
            }
            int[] iArr4 = this.data;
            int i2 = this.nWords;
            this.nWords = i2 + 1;
            iArr4[i2] = iMultAndCarryBy10;
        } else {
            trimLeadingZeros();
        }
        return this;
    }

    public FDBigInteger multByPow52(int i, int i2) {
        FDBigInteger fDBigInteger;
        int i3;
        if (this.nWords == 0) {
            return this;
        }
        if (i != 0) {
            if (i2 == 0) {
                i3 = 0;
            } else {
                i3 = 1;
            }
            if (i < SMALL_5_POW.length) {
                int[] iArr = new int[this.nWords + 1 + i3];
                mult(this.data, this.nWords, SMALL_5_POW[i], iArr);
                fDBigInteger = new FDBigInteger(iArr, this.offset);
            } else {
                FDBigInteger fDBigIntegerBig5pow = big5pow(i);
                int[] iArr2 = new int[this.nWords + fDBigIntegerBig5pow.size() + i3];
                mult(this.data, this.nWords, fDBigIntegerBig5pow.data, fDBigIntegerBig5pow.nWords, iArr2);
                fDBigInteger = new FDBigInteger(iArr2, this.offset + fDBigIntegerBig5pow.offset);
            }
        } else {
            fDBigInteger = this;
        }
        return fDBigInteger.leftShift(i2);
    }

    private static void mult(int[] iArr, int i, int[] iArr2, int i2, int[] iArr3) {
        for (int i3 = 0; i3 < i; i3++) {
            long j = ((long) iArr[i3]) & LONG_MASK;
            long j2 = 0;
            for (int i4 = 0; i4 < i2; i4++) {
                int i5 = i3 + i4;
                long j3 = j2 + (((long) iArr3[i5]) & LONG_MASK) + ((((long) iArr2[i4]) & LONG_MASK) * j);
                iArr3[i5] = (int) j3;
                j2 = j3 >>> 32;
            }
            iArr3[i3 + i2] = (int) j2;
        }
    }

    public FDBigInteger leftInplaceSub(FDBigInteger fDBigInteger) {
        FDBigInteger fDBigInteger2 = this;
        if (fDBigInteger2.isImmutable) {
            fDBigInteger2 = new FDBigInteger((int[]) fDBigInteger2.data.clone(), fDBigInteger2.offset);
        }
        int i = fDBigInteger.offset - fDBigInteger2.offset;
        int[] iArr = fDBigInteger.data;
        int[] iArr2 = fDBigInteger2.data;
        int i2 = fDBigInteger.nWords;
        int i3 = fDBigInteger2.nWords;
        int i4 = 0;
        if (i < 0) {
            int i5 = i3 - i;
            if (i5 < iArr2.length) {
                int i6 = -i;
                System.arraycopy((Object) iArr2, 0, (Object) iArr2, i6, i3);
                Arrays.fill(iArr2, 0, i6, 0);
            } else {
                int[] iArr3 = new int[i5];
                System.arraycopy((Object) iArr2, 0, (Object) iArr3, -i, i3);
                fDBigInteger2.data = iArr3;
                iArr2 = iArr3;
            }
            fDBigInteger2.offset = fDBigInteger.offset;
            fDBigInteger2.nWords = i5;
            i = 0;
            i3 = i5;
        }
        long j = 0;
        while (i4 < i2 && i < i3) {
            long j2 = ((((long) iArr2[i]) & LONG_MASK) - (((long) iArr[i4]) & LONG_MASK)) + j;
            iArr2[i] = (int) j2;
            j = j2 >> 32;
            i4++;
            i++;
        }
        while (j != 0 && i < i3) {
            long j3 = (((long) iArr2[i]) & LONG_MASK) + j;
            iArr2[i] = (int) j3;
            j = j3 >> 32;
            i++;
        }
        fDBigInteger2.trimLeadingZeros();
        return fDBigInteger2;
    }

    public FDBigInteger rightInplaceSub(FDBigInteger fDBigInteger) {
        FDBigInteger fDBigInteger2 = fDBigInteger;
        if (fDBigInteger2.isImmutable) {
            fDBigInteger2 = new FDBigInteger((int[]) fDBigInteger2.data.clone(), fDBigInteger2.offset);
        }
        int i = this.offset - fDBigInteger2.offset;
        int[] iArrCopyOf = fDBigInteger2.data;
        int[] iArr = this.data;
        int i2 = fDBigInteger2.nWords;
        int i3 = this.nWords;
        if (i < 0) {
            if (i3 < iArrCopyOf.length) {
                int i4 = -i;
                System.arraycopy((Object) iArrCopyOf, 0, (Object) iArrCopyOf, i4, i2);
                Arrays.fill(iArrCopyOf, 0, i4, 0);
            } else {
                int[] iArr2 = new int[i3];
                System.arraycopy((Object) iArrCopyOf, 0, (Object) iArr2, -i, i2);
                fDBigInteger2.data = iArr2;
                iArrCopyOf = iArr2;
            }
            fDBigInteger2.offset = this.offset;
            i = 0;
        } else {
            int i5 = i3 + i;
            if (i5 >= iArrCopyOf.length) {
                iArrCopyOf = Arrays.copyOf(iArrCopyOf, i5);
                fDBigInteger2.data = iArrCopyOf;
            }
        }
        int i6 = 0;
        long j = 0;
        while (i6 < i) {
            long j2 = (0 - (LONG_MASK & ((long) iArrCopyOf[i6]))) + j;
            iArrCopyOf[i6] = (int) j2;
            j = j2 >> 32;
            i6++;
        }
        for (int i7 = 0; i7 < i3; i7++) {
            long j3 = ((((long) iArr[i7]) & LONG_MASK) - (((long) iArrCopyOf[i6]) & LONG_MASK)) + j;
            iArrCopyOf[i6] = (int) j3;
            j = j3 >> 32;
            i6++;
        }
        fDBigInteger2.nWords = i6;
        fDBigInteger2.trimLeadingZeros();
        return fDBigInteger2;
    }

    private static int checkZeroTail(int[] iArr, int i) {
        while (i > 0) {
            i--;
            if (iArr[i] != 0) {
                return 1;
            }
        }
        return 0;
    }

    public int cmp(FDBigInteger fDBigInteger) {
        int i = this.nWords + this.offset;
        int i2 = fDBigInteger.nWords + fDBigInteger.offset;
        if (i > i2) {
            return 1;
        }
        if (i < i2) {
            return -1;
        }
        int i3 = this.nWords;
        int i4 = fDBigInteger.nWords;
        while (i3 > 0 && i4 > 0) {
            i3--;
            int i5 = this.data[i3];
            i4--;
            int i6 = fDBigInteger.data[i4];
            if (i5 != i6) {
                if ((((long) i5) & LONG_MASK) >= (((long) i6) & LONG_MASK)) {
                    return 1;
                }
                return -1;
            }
        }
        if (i3 > 0) {
            return checkZeroTail(this.data, i3);
        }
        if (i4 > 0) {
            return -checkZeroTail(fDBigInteger.data, i4);
        }
        return 0;
    }

    public int cmpPow52(int i, int i2) {
        if (i == 0) {
            int i3 = i2 >> 5;
            int i4 = i2 & 31;
            int i5 = this.nWords + this.offset;
            int i6 = i3 + 1;
            if (i5 > i6) {
                return 1;
            }
            if (i5 >= i6) {
                int i7 = this.data[this.nWords - 1];
                int i8 = 1 << i4;
                if (i7 == i8) {
                    return checkZeroTail(this.data, this.nWords - 1);
                }
                if ((((long) i7) & LONG_MASK) >= (((long) i8) & LONG_MASK)) {
                    return 1;
                }
                return -1;
            }
            return -1;
        }
        return cmp(big5pow(i).leftShift(i2));
    }

    public int addAndCmp(FDBigInteger fDBigInteger, FDBigInteger fDBigInteger2) {
        FDBigInteger fDBigInteger3;
        FDBigInteger fDBigInteger4;
        int size = fDBigInteger.size();
        int size2 = fDBigInteger2.size();
        if (size >= size2) {
            fDBigInteger4 = fDBigInteger;
            fDBigInteger3 = fDBigInteger2;
        } else {
            fDBigInteger3 = fDBigInteger;
            fDBigInteger4 = fDBigInteger2;
            size2 = size;
            size = size2;
        }
        int size3 = size();
        if (size == 0) {
            return size3 == 0 ? 0 : 1;
        }
        if (size2 == 0) {
            return cmp(fDBigInteger4);
        }
        if (size > size3) {
            return -1;
        }
        int i = size + 1;
        if (i < size3) {
            return 1;
        }
        long j = ((long) fDBigInteger4.data[fDBigInteger4.nWords - 1]) & LONG_MASK;
        if (size2 == size) {
            j += ((long) fDBigInteger3.data[fDBigInteger3.nWords - 1]) & LONG_MASK;
        }
        long j2 = j >>> 32;
        if (j2 == 0) {
            long j3 = j + 1;
            if ((j3 >>> 32) == 0) {
                if (size < size3) {
                    return 1;
                }
                long j4 = ((long) this.data[this.nWords - 1]) & LONG_MASK;
                if (j4 < j) {
                    return -1;
                }
                if (j4 > j3) {
                    return 1;
                }
            }
        } else if (i <= size3) {
            long j5 = ((long) this.data[this.nWords - 1]) & LONG_MASK;
            if (j5 < j2) {
                return -1;
            }
            if (j5 > j2 + 1) {
                return 1;
            }
        } else {
            return -1;
        }
        return cmp(fDBigInteger4.add(fDBigInteger3));
    }

    public void makeImmutable() {
        this.isImmutable = true;
    }

    private FDBigInteger mult(int i) {
        if (this.nWords == 0) {
            return this;
        }
        int[] iArr = new int[this.nWords + 1];
        mult(this.data, this.nWords, i, iArr);
        return new FDBigInteger(iArr, this.offset);
    }

    private FDBigInteger mult(FDBigInteger fDBigInteger) {
        if (this.nWords == 0) {
            return this;
        }
        if (size() == 1) {
            return fDBigInteger.mult(this.data[0]);
        }
        if (fDBigInteger.nWords == 0) {
            return fDBigInteger;
        }
        if (fDBigInteger.size() == 1) {
            return mult(fDBigInteger.data[0]);
        }
        int[] iArr = new int[this.nWords + fDBigInteger.nWords];
        mult(this.data, this.nWords, fDBigInteger.data, fDBigInteger.nWords, iArr);
        return new FDBigInteger(iArr, this.offset + fDBigInteger.offset);
    }

    private FDBigInteger add(FDBigInteger fDBigInteger) {
        FDBigInteger fDBigInteger2;
        FDBigInteger fDBigInteger3;
        long j;
        int size = size();
        int size2 = fDBigInteger.size();
        if (size >= size2) {
            fDBigInteger3 = this;
            fDBigInteger2 = fDBigInteger;
        } else {
            fDBigInteger2 = this;
            fDBigInteger3 = fDBigInteger;
            size2 = size;
            size = size2;
        }
        int[] iArr = new int[size + 1];
        int i = 0;
        long j2 = 0;
        while (i < size2) {
            if (i >= fDBigInteger3.offset) {
                j = ((long) fDBigInteger3.data[i - fDBigInteger3.offset]) & LONG_MASK;
            } else {
                j = 0;
            }
            long j3 = j2 + j + (i < fDBigInteger2.offset ? 0L : ((long) fDBigInteger2.data[i - fDBigInteger2.offset]) & LONG_MASK);
            iArr[i] = (int) j3;
            j2 = j3 >> 32;
            i++;
        }
        while (i < size) {
            long j4 = j2 + (i < fDBigInteger3.offset ? 0L : ((long) fDBigInteger3.data[i - fDBigInteger3.offset]) & LONG_MASK);
            iArr[i] = (int) j4;
            j2 = j4 >> 32;
            i++;
        }
        iArr[size] = (int) j2;
        return new FDBigInteger(iArr, 0);
    }

    private void multAddMe(int i, int i2) {
        long j = ((long) i) & LONG_MASK;
        long j2 = ((((long) this.data[0]) & LONG_MASK) * j) + (((long) i2) & LONG_MASK);
        this.data[0] = (int) j2;
        long j3 = j2 >>> 32;
        for (int i3 = 1; i3 < this.nWords; i3++) {
            long j4 = j3 + ((((long) this.data[i3]) & LONG_MASK) * j);
            this.data[i3] = (int) j4;
            j3 = j4 >>> 32;
        }
        if (j3 != 0) {
            int[] iArr = this.data;
            int i4 = this.nWords;
            this.nWords = i4 + 1;
            iArr[i4] = (int) j3;
        }
    }

    private long multDiffMe(long j, FDBigInteger fDBigInteger) {
        long j2 = 0;
        if (j == 0) {
            return 0L;
        }
        int i = fDBigInteger.offset - this.offset;
        if (i >= 0) {
            int[] iArr = fDBigInteger.data;
            int[] iArr2 = this.data;
            int i2 = 0;
            while (i2 < fDBigInteger.nWords) {
                long j3 = j2 + ((((long) iArr2[i]) & LONG_MASK) - ((((long) iArr[i2]) & LONG_MASK) * j));
                iArr2[i] = (int) j3;
                j2 = j3 >> 32;
                i2++;
                i++;
            }
            return j2;
        }
        int i3 = -i;
        int[] iArr3 = new int[this.nWords + i3];
        int[] iArr4 = fDBigInteger.data;
        long j4 = 0;
        int i4 = 0;
        int i5 = 0;
        while (i4 < i3 && i5 < fDBigInteger.nWords) {
            long j5 = j4 - ((((long) iArr4[i5]) & LONG_MASK) * j);
            iArr3[i4] = (int) j5;
            j4 = j5 >> 32;
            i5++;
            i4++;
        }
        int[] iArr5 = this.data;
        int i6 = 0;
        while (i5 < fDBigInteger.nWords) {
            long j6 = j4 + ((((long) iArr5[i6]) & LONG_MASK) - ((((long) iArr4[i5]) & LONG_MASK) * j));
            iArr3[i4] = (int) j6;
            j4 = j6 >> 32;
            i6++;
            i4++;
            i5++;
            iArr5 = iArr5;
            i3 = i3;
        }
        int i7 = i3;
        this.nWords += i7;
        this.offset -= i7;
        this.data = iArr3;
        return j4;
    }

    private static int multAndCarryBy10(int[] iArr, int i, int[] iArr2) {
        long j = 0;
        for (int i2 = 0; i2 < i; i2++) {
            long j2 = ((((long) iArr[i2]) & LONG_MASK) * 10) + j;
            iArr2[i2] = (int) j2;
            j = j2 >>> 32;
        }
        return (int) j;
    }

    private static void mult(int[] iArr, int i, int i2, int[] iArr2) {
        long j = ((long) i2) & LONG_MASK;
        long j2 = 0;
        for (int i3 = 0; i3 < i; i3++) {
            long j3 = ((((long) iArr[i3]) & LONG_MASK) * j) + j2;
            iArr2[i3] = (int) j3;
            j2 = j3 >>> 32;
        }
        iArr2[i] = (int) j2;
    }

    private static void mult(int[] iArr, int i, int i2, int i3, int[] iArr2) {
        long j = ((long) i2) & LONG_MASK;
        int i4 = 0;
        long j2 = 0;
        long j3 = 0;
        for (int i5 = 0; i5 < i; i5++) {
            long j4 = ((((long) iArr[i5]) & LONG_MASK) * j) + j3;
            iArr2[i5] = (int) j4;
            j3 = j4 >>> 32;
        }
        iArr2[i] = (int) j3;
        long j5 = ((long) i3) & LONG_MASK;
        while (i4 < i) {
            int i6 = i4 + 1;
            long j6 = (((long) iArr2[i6]) & LONG_MASK) + ((((long) iArr[i4]) & LONG_MASK) * j5) + j2;
            iArr2[i6] = (int) j6;
            j2 = j6 >>> 32;
            i4 = i6;
        }
        iArr2[i + 1] = (int) j2;
    }

    private static FDBigInteger big5pow(int i) {
        if (i < MAX_FIVE_POW) {
            return POW_5_CACHE[i];
        }
        return big5powRec(i);
    }

    private static FDBigInteger big5powRec(int i) {
        if (i < MAX_FIVE_POW) {
            return POW_5_CACHE[i];
        }
        int i2 = i >> 1;
        int i3 = i - i2;
        FDBigInteger fDBigIntegerBig5powRec = big5powRec(i2);
        if (i3 < SMALL_5_POW.length) {
            return fDBigIntegerBig5powRec.mult(SMALL_5_POW[i3]);
        }
        return fDBigIntegerBig5powRec.mult(big5powRec(i3));
    }

    public String toHexString() {
        if (this.nWords == 0) {
            return "0";
        }
        StringBuilder sb = new StringBuilder((this.nWords + this.offset) * 8);
        for (int i = this.nWords - 1; i >= 0; i--) {
            String hexString = Integer.toHexString(this.data[i]);
            for (int length = hexString.length(); length < 8; length++) {
                sb.append('0');
            }
            sb.append(hexString);
        }
        for (int i2 = this.offset; i2 > 0; i2--) {
            sb.append("00000000");
        }
        return sb.toString();
    }

    public BigInteger toBigInteger() {
        byte[] bArr = new byte[(this.nWords * 4) + 1];
        for (int i = 0; i < this.nWords; i++) {
            int i2 = this.data[i];
            bArr[(bArr.length - r5) - 1] = (byte) i2;
            bArr[(bArr.length - r5) - 2] = (byte) (i2 >> 8);
            bArr[(bArr.length - r5) - 3] = (byte) (i2 >> 16);
            bArr[(bArr.length - (4 * i)) - 4] = (byte) (i2 >> 24);
        }
        return new BigInteger(bArr).shiftLeft(this.offset * 32);
    }

    public String toString() {
        return toBigInteger().toString();
    }
}
