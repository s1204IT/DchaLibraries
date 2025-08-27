package com.google.common.collect;

/* loaded from: classes.dex */
final class Hashing {
    private static int MAX_TABLE_SIZE = 1073741824;

    static int smear(int i) {
        return 461845907 * Integer.rotateLeft(i * (-862048943), 15);
    }

    static int smearedHash(Object obj) {
        return smear(obj == null ? 0 : obj.hashCode());
    }

    static int closedTableSize(int i, double d) {
        int iMax = Math.max(i, 2);
        int iHighestOneBit = Integer.highestOneBit(iMax);
        if (iMax > ((int) (d * iHighestOneBit))) {
            int i2 = iHighestOneBit << 1;
            return i2 > 0 ? i2 : MAX_TABLE_SIZE;
        }
        return iHighestOneBit;
    }

    static boolean needsResizing(int i, int i2, double d) {
        return ((double) i) > d * ((double) i2) && i2 < MAX_TABLE_SIZE;
    }
}
