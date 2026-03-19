package android.icu.impl.coll;

import android.icu.impl.Normalizer2Impl;

public final class CollationRootElements {

    static final boolean f48assertionsDisabled;
    static final int IX_COMMON_SEC_AND_TER_CE = 3;
    static final int IX_COUNT = 5;
    static final int IX_FIRST_PRIMARY_INDEX = 2;
    static final int IX_FIRST_SECONDARY_INDEX = 1;
    public static final int IX_FIRST_TERTIARY_INDEX = 0;
    static final int IX_SEC_TER_BOUNDARIES = 4;
    public static final long PRIMARY_SENTINEL = 4294967040L;
    public static final int PRIMARY_STEP_MASK = 127;
    public static final int SEC_TER_DELTA_FLAG = 128;
    private long[] elements;

    static {
        f48assertionsDisabled = !CollationRootElements.class.desiredAssertionStatus();
    }

    public CollationRootElements(long[] rootElements) {
        this.elements = rootElements;
    }

    public int getTertiaryBoundary() {
        return (((int) this.elements[4]) << 8) & Normalizer2Impl.JAMO_VT;
    }

    long getFirstTertiaryCE() {
        return this.elements[(int) this.elements[0]] & (-129);
    }

    long getLastTertiaryCE() {
        return this.elements[((int) this.elements[1]) - 1] & (-129);
    }

    public int getLastCommonSecondary() {
        return (((int) this.elements[4]) >> 16) & Normalizer2Impl.JAMO_VT;
    }

    public int getSecondaryBoundary() {
        return (((int) this.elements[4]) >> 8) & Normalizer2Impl.JAMO_VT;
    }

    long getFirstSecondaryCE() {
        return this.elements[(int) this.elements[1]] & (-129);
    }

    long getLastSecondaryCE() {
        return this.elements[((int) this.elements[2]) - 1] & (-129);
    }

    long getFirstPrimary() {
        return this.elements[(int) this.elements[2]];
    }

    long getFirstPrimaryCE() {
        return Collation.makeCE(getFirstPrimary());
    }

    long lastCEWithPrimaryBefore(long p) {
        long p2;
        long secTer;
        long q;
        long p3;
        if (p == 0) {
            return 0L;
        }
        if (!f48assertionsDisabled) {
            if (!(p > this.elements[(int) this.elements[2]])) {
                throw new AssertionError();
            }
        }
        int index = findP(p);
        long q2 = this.elements[index];
        if (p == (PRIMARY_SENTINEL & q2)) {
            if (!f48assertionsDisabled) {
                if (!((127 & q2) == 0)) {
                    throw new AssertionError();
                }
            }
            secTer = this.elements[index - 1];
            if ((128 & secTer) == 0) {
                p2 = secTer & PRIMARY_SENTINEL;
                secTer = 83887360;
            } else {
                int index2 = index - 2;
                while (true) {
                    p3 = this.elements[index2];
                    if ((128 & p3) == 0) {
                        break;
                    }
                    index2--;
                }
                p2 = p3 & PRIMARY_SENTINEL;
            }
        } else {
            p2 = q2 & PRIMARY_SENTINEL;
            secTer = 83887360;
            while (true) {
                index++;
                q = this.elements[index];
                if ((128 & q) == 0) {
                    break;
                }
                secTer = q;
            }
            if (!f48assertionsDisabled) {
                if (!((127 & q) == 0)) {
                    throw new AssertionError();
                }
            }
        }
        return (p2 << 32) | ((-129) & secTer);
    }

    long firstCEWithPrimaryAtLeast(long p) {
        if (p == 0) {
            return 0L;
        }
        int index = findP(p);
        if (p != (this.elements[index] & PRIMARY_SENTINEL)) {
            do {
                index++;
                p = this.elements[index];
            } while ((128 & p) != 0);
            if (!f48assertionsDisabled) {
                if (!((127 & p) == 0)) {
                    throw new AssertionError();
                }
            }
        }
        return (p << 32) | 83887360;
    }

    long getPrimaryBefore(long p, boolean isCompressible) {
        int step;
        long p2;
        int index = findPrimary(p);
        long q = this.elements[index];
        if (p == (q & PRIMARY_SENTINEL)) {
            step = ((int) q) & 127;
            if (step == 0) {
                do {
                    index--;
                    p2 = this.elements[index];
                } while ((128 & p2) != 0);
                return p2 & PRIMARY_SENTINEL;
            }
        } else {
            long nextElement = this.elements[index + 1];
            if (!f48assertionsDisabled && !isEndOfPrimaryRange(nextElement)) {
                throw new AssertionError();
            }
            step = ((int) nextElement) & 127;
        }
        if ((65535 & p) == 0) {
            return Collation.decTwoBytePrimaryByOneStep(p, isCompressible, step);
        }
        return Collation.decThreeBytePrimaryByOneStep(p, isCompressible, step);
    }

    int getSecondaryBefore(long p, int s) {
        int index;
        int previousSec;
        int sec;
        if (p == 0) {
            index = (int) this.elements[1];
            previousSec = 0;
            sec = (int) (this.elements[index] >> 16);
        } else {
            index = findPrimary(p) + 1;
            previousSec = 256;
            sec = ((int) getFirstSecTerForPrimary(index)) >>> 16;
        }
        if (!f48assertionsDisabled) {
            if (!(s >= sec)) {
                throw new AssertionError();
            }
        }
        int index2 = index;
        while (s > sec) {
            previousSec = sec;
            if (!f48assertionsDisabled) {
                if (!((this.elements[index2] & 128) != 0)) {
                    throw new AssertionError();
                }
            }
            sec = (int) (this.elements[index2] >> 16);
            index2++;
        }
        if (!f48assertionsDisabled) {
            if (!(sec == s)) {
                throw new AssertionError();
            }
        }
        return previousSec;
    }

    int getTertiaryBefore(long p, int s, int t) {
        int index;
        int previousTer;
        long secTer;
        if (!f48assertionsDisabled) {
            if (!((t & (-16192)) == 0)) {
                throw new AssertionError();
            }
        }
        if (p == 0) {
            if (s == 0) {
                index = (int) this.elements[0];
                previousTer = 0;
            } else {
                index = (int) this.elements[1];
                previousTer = 256;
            }
            secTer = this.elements[index] & (-129);
        } else {
            index = findPrimary(p) + 1;
            previousTer = 256;
            secTer = getFirstSecTerForPrimary(index);
        }
        long st = (((long) s) << 16) | ((long) t);
        while (true) {
            int index2 = index;
            if (st > secTer) {
                if (((int) (secTer >> 16)) == s) {
                    previousTer = (int) secTer;
                }
                if (!f48assertionsDisabled) {
                    if (!((this.elements[index2] & 128) != 0)) {
                        throw new AssertionError();
                    }
                }
                index = index2 + 1;
                secTer = this.elements[index2] & (-129);
            } else {
                if (!f48assertionsDisabled) {
                    if (!(secTer == st)) {
                        throw new AssertionError();
                    }
                }
                return 65535 & previousTer;
            }
        }
    }

    int findPrimary(long p) {
        boolean z = true;
        if (!f48assertionsDisabled) {
            if (!((255 & p) == 0)) {
                throw new AssertionError();
            }
        }
        int index = findP(p);
        if (!f48assertionsDisabled) {
            if (!isEndOfPrimaryRange(this.elements[index + 1]) && p != (this.elements[index] & PRIMARY_SENTINEL)) {
                z = false;
            }
            if (!z) {
                throw new AssertionError();
            }
        }
        return index;
    }

    long getPrimaryAfter(long p, int index, boolean isCompressible) {
        int step;
        if (!f48assertionsDisabled) {
            if (!(p != (this.elements[index] & PRIMARY_SENTINEL) ? isEndOfPrimaryRange(this.elements[index + 1]) : true)) {
                throw new AssertionError();
            }
        }
        int index2 = index + 1;
        long q = this.elements[index2];
        if ((128 & q) == 0 && (step = ((int) q) & 127) != 0) {
            if ((65535 & p) == 0) {
                return Collation.incTwoBytePrimaryByOffset(p, isCompressible, step);
            }
            return Collation.incThreeBytePrimaryByOffset(p, isCompressible, step);
        }
        while ((128 & q) != 0) {
            index2++;
            q = this.elements[index2];
        }
        if (!f48assertionsDisabled) {
            if (!((127 & q) == 0)) {
                throw new AssertionError();
            }
        }
        return q;
    }

    int getSecondaryAfter(int index, int s) {
        long secTer;
        int secLimit;
        if (index == 0) {
            if (!f48assertionsDisabled) {
                if (!(s != 0)) {
                    throw new AssertionError();
                }
            }
            index = (int) this.elements[1];
            secTer = this.elements[index];
            secLimit = 65536;
        } else {
            if (!f48assertionsDisabled) {
                if (!(index >= ((int) this.elements[2]))) {
                    throw new AssertionError();
                }
            }
            secTer = getFirstSecTerForPrimary(index + 1);
            secLimit = getSecondaryBoundary();
        }
        do {
            int sec = (int) (secTer >> 16);
            if (sec > s) {
                return sec;
            }
            index++;
            secTer = this.elements[index];
        } while ((128 & secTer) != 0);
        return secLimit;
    }

    int getTertiaryAfter(int index, int s, int t) {
        long secTer;
        int terLimit;
        if (index == 0) {
            if (s == 0) {
                if (!f48assertionsDisabled) {
                    if (!(t != 0)) {
                        throw new AssertionError();
                    }
                }
                index = (int) this.elements[0];
                terLimit = 16384;
            } else {
                index = (int) this.elements[1];
                terLimit = getTertiaryBoundary();
            }
            secTer = this.elements[index] & (-129);
        } else {
            if (!f48assertionsDisabled) {
                if (!(index >= ((int) this.elements[2]))) {
                    throw new AssertionError();
                }
            }
            secTer = getFirstSecTerForPrimary(index + 1);
            terLimit = getTertiaryBoundary();
        }
        long st = ((((long) s) & 4294967295L) << 16) | ((long) t);
        while (secTer <= st) {
            index++;
            long secTer2 = this.elements[index];
            if ((128 & secTer2) == 0 || (secTer2 >> 16) > s) {
                return terLimit;
            }
            secTer = secTer2 & (-129);
        }
        if (!f48assertionsDisabled) {
            if (!((secTer >> 16) == ((long) s))) {
                throw new AssertionError();
            }
        }
        return ((int) secTer) & 65535;
    }

    private long getFirstSecTerForPrimary(int index) {
        long secTer = this.elements[index];
        if ((128 & secTer) == 0) {
            return 83887360L;
        }
        long secTer2 = secTer & (-129);
        if (secTer2 > 83887360) {
            return 83887360L;
        }
        return secTer2;
    }

    private int findP(long p) {
        if (!f48assertionsDisabled) {
            if (!((p >> 24) != 254)) {
                throw new AssertionError();
            }
        }
        int start = (int) this.elements[2];
        if (!f48assertionsDisabled) {
            if (!(p >= this.elements[start])) {
                throw new AssertionError();
            }
        }
        int limit = this.elements.length - 1;
        if (!f48assertionsDisabled) {
            if (!(this.elements[limit] >= PRIMARY_SENTINEL)) {
                throw new AssertionError();
            }
        }
        if (!f48assertionsDisabled) {
            if (!(p < this.elements[limit])) {
                throw new AssertionError();
            }
        }
        while (start + 1 < limit) {
            int i = (start + limit) / 2;
            long q = this.elements[i];
            if ((128 & q) != 0) {
                int j = i + 1;
                while (true) {
                    if (j == limit) {
                        break;
                    }
                    q = this.elements[j];
                    if ((128 & q) == 0) {
                        i = j;
                        break;
                    }
                    j++;
                }
                if ((128 & q) != 0) {
                    int j2 = i - 1;
                    while (true) {
                        if (j2 == start) {
                            break;
                        }
                        q = this.elements[j2];
                        if ((128 & q) == 0) {
                            i = j2;
                            break;
                        }
                        j2--;
                    }
                    if ((128 & q) != 0) {
                        break;
                    }
                }
            }
            if (p < (PRIMARY_SENTINEL & q)) {
                limit = i;
            } else {
                start = i;
            }
        }
        return start;
    }

    private static boolean isEndOfPrimaryRange(long q) {
        return (128 & q) == 0 && (127 & q) != 0;
    }
}
