package android.icu.impl.coll;

import java.util.Arrays;

public final class CollationWeights {
    static final boolean $assertionsDisabled = false;
    private int middleLength;
    private int rangeCount;
    private int rangeIndex;
    private int[] minBytes = new int[5];
    private int[] maxBytes = new int[5];
    private WeightRange[] ranges = new WeightRange[7];

    public void initForPrimary(boolean z) {
        this.middleLength = 1;
        this.minBytes[1] = 3;
        this.maxBytes[1] = 255;
        if (z) {
            this.minBytes[2] = 4;
            this.maxBytes[2] = 254;
        } else {
            this.minBytes[2] = 2;
            this.maxBytes[2] = 255;
        }
        this.minBytes[3] = 2;
        this.maxBytes[3] = 255;
        this.minBytes[4] = 2;
        this.maxBytes[4] = 255;
    }

    public void initForSecondary() {
        this.middleLength = 3;
        this.minBytes[1] = 0;
        this.maxBytes[1] = 0;
        this.minBytes[2] = 0;
        this.maxBytes[2] = 0;
        this.minBytes[3] = 2;
        this.maxBytes[3] = 255;
        this.minBytes[4] = 2;
        this.maxBytes[4] = 255;
    }

    public void initForTertiary() {
        this.middleLength = 3;
        this.minBytes[1] = 0;
        this.maxBytes[1] = 0;
        this.minBytes[2] = 0;
        this.maxBytes[2] = 0;
        this.minBytes[3] = 2;
        this.maxBytes[3] = 63;
        this.minBytes[4] = 2;
        this.maxBytes[4] = 63;
    }

    public boolean allocWeights(long j, long j2, int i) {
        if (!getWeightRanges(j, j2)) {
            return false;
        }
        while (true) {
            int i2 = this.ranges[0].length;
            if (allocWeightsInShortRanges(i, i2)) {
                break;
            }
            if (i2 == 4) {
                return false;
            }
            if (allocWeightsInMinLengthRanges(i, i2)) {
                break;
            }
            for (int i3 = 0; i3 < this.rangeCount && this.ranges[i3].length == i2; i3++) {
                lengthenRange(this.ranges[i3]);
            }
        }
        this.rangeIndex = 0;
        if (this.rangeCount < this.ranges.length) {
            this.ranges[this.rangeCount] = null;
            return true;
        }
        return true;
    }

    public long nextWeight() {
        if (this.rangeIndex >= this.rangeCount) {
            return 4294967295L;
        }
        WeightRange weightRange = this.ranges[this.rangeIndex];
        long j = weightRange.start;
        int i = weightRange.count - 1;
        weightRange.count = i;
        if (i == 0) {
            this.rangeIndex++;
        } else {
            weightRange.start = incWeight(j, weightRange.length);
        }
        return j;
    }

    private static final class WeightRange implements Comparable<WeightRange> {
        int count;
        long end;
        int length;
        long start;

        private WeightRange() {
        }

        @Override
        public int compareTo(WeightRange weightRange) {
            long j = this.start;
            long j2 = weightRange.start;
            if (j < j2) {
                return -1;
            }
            if (j > j2) {
                return 1;
            }
            return 0;
        }
    }

    public static int lengthOfWeight(long j) {
        if ((16777215 & j) == 0) {
            return 1;
        }
        if ((65535 & j) == 0) {
            return 2;
        }
        if ((j & 255) == 0) {
            return 3;
        }
        return 4;
    }

    private static int getWeightTrail(long j, int i) {
        return ((int) (j >> (8 * (4 - i)))) & 255;
    }

    private static long setWeightTrail(long j, int i, int i2) {
        int i3 = 8 * (4 - i);
        return (j & (CollationRootElements.PRIMARY_SENTINEL << i3)) | (((long) i2) << i3);
    }

    private static int getWeightByte(long j, int i) {
        return getWeightTrail(j, i);
    }

    private static long setWeightByte(long j, int i, int i2) {
        long j2;
        int i3 = i * 8;
        if (i3 < 32) {
            j2 = 4294967295 >> i3;
        } else {
            j2 = 0;
        }
        int i4 = 32 - i3;
        return (j & (j2 | (CollationRootElements.PRIMARY_SENTINEL << i4))) | (((long) i2) << i4);
    }

    private static long truncateWeight(long j, int i) {
        return j & (4294967295 << (8 * (4 - i)));
    }

    private static long incWeightTrail(long j, int i) {
        return j + (1 << (8 * (4 - i)));
    }

    private static long decWeightTrail(long j, int i) {
        return j - (1 << (8 * (4 - i)));
    }

    private int countBytes(int i) {
        return (this.maxBytes[i] - this.minBytes[i]) + 1;
    }

    private long incWeight(long j, int i) {
        while (true) {
            int weightByte = getWeightByte(j, i);
            if (weightByte < this.maxBytes[i]) {
                return setWeightByte(j, i, weightByte + 1);
            }
            j = setWeightByte(j, i, this.minBytes[i]);
            i--;
        }
    }

    private long incWeightByOffset(long j, int i, int i2) {
        while (true) {
            int weightByte = i2 + getWeightByte(j, i);
            if (weightByte <= this.maxBytes[i]) {
                return setWeightByte(j, i, weightByte);
            }
            int i3 = weightByte - this.minBytes[i];
            j = setWeightByte(j, i, this.minBytes[i] + (i3 % countBytes(i)));
            i2 = i3 / countBytes(i);
            i--;
        }
    }

    private void lengthenRange(WeightRange weightRange) {
        int i = weightRange.length + 1;
        weightRange.start = setWeightTrail(weightRange.start, i, this.minBytes[i]);
        weightRange.end = setWeightTrail(weightRange.end, i, this.maxBytes[i]);
        weightRange.count *= countBytes(i);
        weightRange.length = i;
    }

    private boolean getWeightRanges(long j, long j2) {
        boolean z;
        int iLengthOfWeight = lengthOfWeight(j);
        int iLengthOfWeight2 = lengthOfWeight(j2);
        if (j >= j2) {
            return false;
        }
        if (iLengthOfWeight < iLengthOfWeight2 && j == truncateWeight(j2, iLengthOfWeight)) {
            return false;
        }
        WeightRange[] weightRangeArr = new WeightRange[5];
        WeightRange weightRange = new WeightRange();
        WeightRange[] weightRangeArr2 = new WeightRange[5];
        while (iLengthOfWeight > this.middleLength) {
            int weightTrail = getWeightTrail(j, iLengthOfWeight);
            if (weightTrail < this.maxBytes[iLengthOfWeight]) {
                weightRangeArr[iLengthOfWeight] = new WeightRange();
                weightRangeArr[iLengthOfWeight].start = incWeightTrail(j, iLengthOfWeight);
                weightRangeArr[iLengthOfWeight].end = setWeightTrail(j, iLengthOfWeight, this.maxBytes[iLengthOfWeight]);
                weightRangeArr[iLengthOfWeight].length = iLengthOfWeight;
                weightRangeArr[iLengthOfWeight].count = this.maxBytes[iLengthOfWeight] - weightTrail;
            }
            j = truncateWeight(j, iLengthOfWeight - 1);
            iLengthOfWeight--;
        }
        if (j < 4278190080L) {
            weightRange.start = incWeightTrail(j, this.middleLength);
        } else {
            weightRange.start = 4294967295L;
        }
        while (iLengthOfWeight2 > this.middleLength) {
            int weightTrail2 = getWeightTrail(j2, iLengthOfWeight2);
            if (weightTrail2 > this.minBytes[iLengthOfWeight2]) {
                weightRangeArr2[iLengthOfWeight2] = new WeightRange();
                weightRangeArr2[iLengthOfWeight2].start = setWeightTrail(j2, iLengthOfWeight2, this.minBytes[iLengthOfWeight2]);
                weightRangeArr2[iLengthOfWeight2].end = decWeightTrail(j2, iLengthOfWeight2);
                weightRangeArr2[iLengthOfWeight2].length = iLengthOfWeight2;
                weightRangeArr2[iLengthOfWeight2].count = weightTrail2 - this.minBytes[iLengthOfWeight2];
            }
            j2 = truncateWeight(j2, iLengthOfWeight2 - 1);
            iLengthOfWeight2--;
        }
        weightRange.end = decWeightTrail(j2, this.middleLength);
        weightRange.length = this.middleLength;
        if (weightRange.end >= weightRange.start) {
            weightRange.count = ((int) ((weightRange.end - weightRange.start) >> (8 * (4 - this.middleLength)))) + 1;
        } else {
            int i = 4;
            while (true) {
                if (i <= this.middleLength) {
                    break;
                }
                if (weightRangeArr[i] != null && weightRangeArr2[i] != null && weightRangeArr[i].count > 0 && weightRangeArr2[i].count > 0) {
                    long j3 = weightRangeArr[i].end;
                    long j4 = weightRangeArr2[i].start;
                    if (j3 > j4) {
                        weightRangeArr[i].end = weightRangeArr2[i].end;
                        weightRangeArr[i].count = (getWeightTrail(weightRangeArr[i].end, i) - getWeightTrail(weightRangeArr[i].start, i)) + 1;
                    } else if (j3 != j4 && incWeight(j3, i) == j4) {
                        weightRangeArr[i].end = weightRangeArr2[i].end;
                        weightRangeArr[i].count += weightRangeArr2[i].count;
                    } else {
                        z = false;
                        if (!z) {
                            weightRangeArr2[i].count = 0;
                            while (true) {
                                i--;
                                if (i <= this.middleLength) {
                                    break;
                                }
                                weightRangeArr2[i] = null;
                                weightRangeArr[i] = null;
                            }
                        }
                    }
                    z = true;
                    if (!z) {
                    }
                }
                i--;
            }
        }
        this.rangeCount = 0;
        if (weightRange.count > 0) {
            this.ranges[0] = weightRange;
            this.rangeCount = 1;
        }
        for (int i2 = this.middleLength + 1; i2 <= 4; i2++) {
            if (weightRangeArr2[i2] != null && weightRangeArr2[i2].count > 0) {
                WeightRange[] weightRangeArr3 = this.ranges;
                int i3 = this.rangeCount;
                this.rangeCount = i3 + 1;
                weightRangeArr3[i3] = weightRangeArr2[i2];
            }
            if (weightRangeArr[i2] != null && weightRangeArr[i2].count > 0) {
                WeightRange[] weightRangeArr4 = this.ranges;
                int i4 = this.rangeCount;
                this.rangeCount = i4 + 1;
                weightRangeArr4[i4] = weightRangeArr[i2];
            }
        }
        return this.rangeCount > 0;
    }

    private boolean allocWeightsInShortRanges(int i, int i2) {
        int i3 = i;
        for (int i4 = 0; i4 < this.rangeCount && this.ranges[i4].length <= i2 + 1; i4++) {
            if (i3 <= this.ranges[i4].count) {
                if (this.ranges[i4].length > i2) {
                    this.ranges[i4].count = i3;
                }
                this.rangeCount = i4 + 1;
                if (this.rangeCount > 1) {
                    Arrays.sort(this.ranges, 0, this.rangeCount);
                }
                return true;
            }
            i3 -= this.ranges[i4].count;
        }
        return false;
    }

    private boolean allocWeightsInMinLengthRanges(int i, int i2) {
        int i3 = 0;
        int i4 = 0;
        while (i3 < this.rangeCount && this.ranges[i3].length == i2) {
            i4 += this.ranges[i3].count;
            i3++;
        }
        int iCountBytes = countBytes(i2 + 1);
        if (i > i4 * iCountBytes) {
            return false;
        }
        long j = this.ranges[0].start;
        long j2 = this.ranges[0].end;
        long j3 = j;
        for (int i5 = 1; i5 < i3; i5++) {
            if (this.ranges[i5].start < j3) {
                j3 = this.ranges[i5].start;
            }
            if (this.ranges[i5].end > j2) {
                j2 = this.ranges[i5].end;
            }
        }
        int i6 = (i - i4) / (iCountBytes - 1);
        int i7 = i4 - i6;
        if (i6 == 0 || (iCountBytes * i6) + i7 < i) {
            i6++;
            i7--;
        }
        this.ranges[0].start = j3;
        if (i7 == 0) {
            this.ranges[0].end = j2;
            this.ranges[0].count = i4;
            lengthenRange(this.ranges[0]);
            this.rangeCount = 1;
        } else {
            this.ranges[0].end = incWeightByOffset(j3, i2, i7 - 1);
            this.ranges[0].count = i7;
            if (this.ranges[1] == null) {
                this.ranges[1] = new WeightRange();
            }
            this.ranges[1].start = incWeight(this.ranges[0].end, i2);
            this.ranges[1].end = j2;
            this.ranges[1].length = i2;
            this.ranges[1].count = i6;
            lengthenRange(this.ranges[1]);
            this.rangeCount = 2;
        }
        return true;
    }
}
