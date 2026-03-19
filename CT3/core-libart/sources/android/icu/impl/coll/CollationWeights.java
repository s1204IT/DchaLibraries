package android.icu.impl.coll;

import java.util.Arrays;

public final class CollationWeights {

    static final boolean f52assertionsDisabled;
    private int middleLength;
    private int rangeCount;
    private int rangeIndex;
    private int[] minBytes = new int[5];
    private int[] maxBytes = new int[5];
    private WeightRange[] ranges = new WeightRange[7];

    static {
        f52assertionsDisabled = !CollationWeights.class.desiredAssertionStatus();
    }

    public void initForPrimary(boolean compressible) {
        this.middleLength = 1;
        this.minBytes[1] = 3;
        this.maxBytes[1] = 255;
        if (compressible) {
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

    public boolean allocWeights(long lowerLimit, long upperLimit, int n) {
        if (!getWeightRanges(lowerLimit, upperLimit)) {
            return false;
        }
        while (true) {
            int minLength = this.ranges[0].length;
            if (allocWeightsInShortRanges(n, minLength)) {
                break;
            }
            if (minLength == 4) {
                return false;
            }
            if (allocWeightsInMinLengthRanges(n, minLength)) {
                break;
            }
            for (int i = 0; this.ranges[i].length == minLength; i++) {
                lengthenRange(this.ranges[i]);
            }
        }
    }

    public long nextWeight() {
        if (this.rangeIndex >= this.rangeCount) {
            return 4294967295L;
        }
        WeightRange range = this.ranges[this.rangeIndex];
        long weight = range.start;
        int i = range.count - 1;
        range.count = i;
        if (i == 0) {
            this.rangeIndex++;
        } else {
            range.start = incWeight(weight, range.length);
            if (!f52assertionsDisabled) {
                if (!(range.start <= range.end)) {
                    throw new AssertionError();
                }
            }
        }
        return weight;
    }

    private static final class WeightRange implements Comparable<WeightRange> {
        int count;
        long end;
        int length;
        long start;

        WeightRange(WeightRange weightRange) {
            this();
        }

        private WeightRange() {
        }

        @Override
        public int compareTo(WeightRange other) {
            long l = this.start;
            long r = other.start;
            if (l < r) {
                return -1;
            }
            if (l > r) {
                return 1;
            }
            return 0;
        }
    }

    public static int lengthOfWeight(long weight) {
        if ((16777215 & weight) == 0) {
            return 1;
        }
        if ((65535 & weight) == 0) {
            return 2;
        }
        if ((255 & weight) == 0) {
            return 3;
        }
        return 4;
    }

    private static int getWeightTrail(long weight, int length) {
        return ((int) (weight >> ((4 - length) * 8))) & 255;
    }

    private static long setWeightTrail(long weight, int length, int trail) {
        int length2 = (4 - length) * 8;
        return ((CollationRootElements.PRIMARY_SENTINEL << length2) & weight) | (((long) trail) << length2);
    }

    private static int getWeightByte(long weight, int idx) {
        return getWeightTrail(weight, idx);
    }

    private static long setWeightByte(long weight, int idx, int b) {
        long mask;
        int idx2 = idx * 8;
        if (idx2 < 32) {
            mask = 4294967295 >> idx2;
        } else {
            mask = 0;
        }
        int idx3 = 32 - idx2;
        return (weight & (mask | (CollationRootElements.PRIMARY_SENTINEL << idx3))) | (((long) b) << idx3);
    }

    private static long truncateWeight(long weight, int length) {
        return (4294967295 << ((4 - length) * 8)) & weight;
    }

    private static long incWeightTrail(long weight, int length) {
        return (1 << ((4 - length) * 8)) + weight;
    }

    private static long decWeightTrail(long weight, int length) {
        return weight - (1 << ((4 - length) * 8));
    }

    private int countBytes(int idx) {
        return (this.maxBytes[idx] - this.minBytes[idx]) + 1;
    }

    private long incWeight(long weight, int length) {
        while (true) {
            int b = getWeightByte(weight, length);
            if (b < this.maxBytes[length]) {
                return setWeightByte(weight, length, b + 1);
            }
            weight = setWeightByte(weight, length, this.minBytes[length]);
            length--;
            if (!f52assertionsDisabled) {
                if (!(length > 0)) {
                    throw new AssertionError();
                }
            }
        }
    }

    private long incWeightByOffset(long weight, int length, int offset) {
        while (true) {
            int offset2 = offset + getWeightByte(weight, length);
            if (offset2 <= this.maxBytes[length]) {
                return setWeightByte(weight, length, offset2);
            }
            int offset3 = offset2 - this.minBytes[length];
            weight = setWeightByte(weight, length, this.minBytes[length] + (offset3 % countBytes(length)));
            offset = offset3 / countBytes(length);
            length--;
            if (!f52assertionsDisabled) {
                if (!(length > 0)) {
                    throw new AssertionError();
                }
            }
        }
    }

    private void lengthenRange(WeightRange range) {
        int length = range.length + 1;
        range.start = setWeightTrail(range.start, length, this.minBytes[length]);
        range.end = setWeightTrail(range.end, length, this.maxBytes[length]);
        range.count *= countBytes(length);
        range.length = length;
    }

    private boolean getWeightRanges(long lowerLimit, long upperLimit) {
        if (!f52assertionsDisabled) {
            if (!(lowerLimit != 0)) {
                throw new AssertionError();
            }
        }
        if (!f52assertionsDisabled) {
            if (!(upperLimit != 0)) {
                throw new AssertionError();
            }
        }
        int lowerLength = lengthOfWeight(lowerLimit);
        int upperLength = lengthOfWeight(upperLimit);
        if (!f52assertionsDisabled) {
            if (!(lowerLength >= this.middleLength)) {
                throw new AssertionError();
            }
        }
        if (lowerLimit >= upperLimit) {
            return false;
        }
        if (lowerLength < upperLength && lowerLimit == truncateWeight(upperLimit, lowerLength)) {
            return false;
        }
        WeightRange[] lower = new WeightRange[5];
        WeightRange middle = new WeightRange(null);
        WeightRange[] upper = new WeightRange[5];
        long weight = lowerLimit;
        for (int length = lowerLength; length > this.middleLength; length--) {
            int trail = getWeightTrail(weight, length);
            if (trail < this.maxBytes[length]) {
                lower[length] = new WeightRange(null);
                lower[length].start = incWeightTrail(weight, length);
                lower[length].end = setWeightTrail(weight, length, this.maxBytes[length]);
                lower[length].length = length;
                lower[length].count = this.maxBytes[length] - trail;
            }
            weight = truncateWeight(weight, length - 1);
        }
        if (weight < 4278190080L) {
            middle.start = incWeightTrail(weight, this.middleLength);
        } else {
            middle.start = 4294967295L;
        }
        long weight2 = upperLimit;
        for (int length2 = upperLength; length2 > this.middleLength; length2--) {
            int trail2 = getWeightTrail(weight2, length2);
            if (trail2 > this.minBytes[length2]) {
                upper[length2] = new WeightRange(null);
                upper[length2].start = setWeightTrail(weight2, length2, this.minBytes[length2]);
                upper[length2].end = decWeightTrail(weight2, length2);
                upper[length2].length = length2;
                upper[length2].count = trail2 - this.minBytes[length2];
            }
            weight2 = truncateWeight(weight2, length2 - 1);
        }
        middle.end = decWeightTrail(weight2, this.middleLength);
        middle.length = this.middleLength;
        if (middle.end >= middle.start) {
            middle.count = ((int) ((middle.end - middle.start) >> ((4 - this.middleLength) * 8))) + 1;
        } else {
            int length3 = 4;
            while (true) {
                if (length3 <= this.middleLength) {
                    break;
                }
                if (lower[length3] != null && upper[length3] != null && lower[length3].count > 0 && upper[length3].count > 0) {
                    long lowerEnd = lower[length3].end;
                    long upperStart = upper[length3].start;
                    boolean merged = false;
                    if (lowerEnd > upperStart) {
                        if (!f52assertionsDisabled) {
                            if (!(truncateWeight(lowerEnd, length3 + (-1)) == truncateWeight(upperStart, length3 + (-1)))) {
                                throw new AssertionError();
                            }
                        }
                        lower[length3].end = upper[length3].end;
                        lower[length3].count = (getWeightTrail(lower[length3].end, length3) - getWeightTrail(lower[length3].start, length3)) + 1;
                        merged = true;
                    } else if (lowerEnd == upperStart) {
                        if (!f52assertionsDisabled) {
                            if (!(this.minBytes[length3] < this.maxBytes[length3])) {
                                throw new AssertionError();
                            }
                        }
                    } else if (incWeight(lowerEnd, length3) == upperStart) {
                        lower[length3].end = upper[length3].end;
                        lower[length3].count += upper[length3].count;
                        merged = true;
                    }
                    if (merged) {
                        upper[length3].count = 0;
                        while (true) {
                            length3--;
                            if (length3 <= this.middleLength) {
                                break;
                            }
                            upper[length3] = null;
                            lower[length3] = null;
                        }
                    }
                }
                length3--;
            }
        }
        this.rangeCount = 0;
        if (middle.count > 0) {
            this.ranges[0] = middle;
            this.rangeCount = 1;
        }
        for (int length4 = this.middleLength + 1; length4 <= 4; length4++) {
            if (upper[length4] != null && upper[length4].count > 0) {
                WeightRange[] weightRangeArr = this.ranges;
                int i = this.rangeCount;
                this.rangeCount = i + 1;
                weightRangeArr[i] = upper[length4];
            }
            if (lower[length4] != null && lower[length4].count > 0) {
                WeightRange[] weightRangeArr2 = this.ranges;
                int i2 = this.rangeCount;
                this.rangeCount = i2 + 1;
                weightRangeArr2[i2] = lower[length4];
            }
        }
        return this.rangeCount > 0;
    }

    private boolean allocWeightsInShortRanges(int n, int minLength) {
        for (int i = 0; i < this.rangeCount && this.ranges[i].length <= minLength + 1; i++) {
            if (n <= this.ranges[i].count) {
                if (this.ranges[i].length > minLength) {
                    this.ranges[i].count = n;
                }
                this.rangeCount = i + 1;
                if (this.rangeCount > 1) {
                    Arrays.sort(this.ranges, 0, this.rangeCount);
                }
                return true;
            }
            n -= this.ranges[i].count;
        }
        return false;
    }

    private boolean allocWeightsInMinLengthRanges(int n, int minLength) {
        int count = 0;
        int minLengthRangeCount = 0;
        while (minLengthRangeCount < this.rangeCount && this.ranges[minLengthRangeCount].length == minLength) {
            count += this.ranges[minLengthRangeCount].count;
            minLengthRangeCount++;
        }
        int nextCountBytes = countBytes(minLength + 1);
        if (n > count * nextCountBytes) {
            return false;
        }
        long start = this.ranges[0].start;
        long end = this.ranges[0].end;
        for (int i = 1; i < minLengthRangeCount; i++) {
            if (this.ranges[i].start < start) {
                start = this.ranges[i].start;
            }
            if (this.ranges[i].end > end) {
                end = this.ranges[i].end;
            }
        }
        int count2 = (n - count) / (nextCountBytes - 1);
        int count1 = count - count2;
        if (count2 == 0 || (count2 * nextCountBytes) + count1 < n) {
            count2++;
            count1--;
            if (!f52assertionsDisabled) {
                if (!((count2 * nextCountBytes) + count1 >= n)) {
                    throw new AssertionError();
                }
            }
        }
        this.ranges[0].start = start;
        if (count1 == 0) {
            this.ranges[0].end = end;
            this.ranges[0].count = count;
            lengthenRange(this.ranges[0]);
            this.rangeCount = 1;
            return true;
        }
        this.ranges[0].end = incWeightByOffset(start, minLength, count1 - 1);
        this.ranges[0].count = count1;
        if (this.ranges[1] == null) {
            this.ranges[1] = new WeightRange(null);
        }
        this.ranges[1].start = incWeight(this.ranges[0].end, minLength);
        this.ranges[1].end = end;
        this.ranges[1].length = minLength;
        this.ranges[1].count = count2;
        lengthenRange(this.ranges[1]);
        this.rangeCount = 2;
        return true;
    }
}
