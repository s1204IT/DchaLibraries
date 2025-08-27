package com.google.common.primitives;

import java.util.Comparator;

/* loaded from: classes.dex */
public final class UnsignedBytes {
    public static int toInt(byte b) {
        return b & 255;
    }

    public static int compare(byte b, byte b2) {
        return toInt(b) - toInt(b2);
    }

    static Comparator<byte[]> lexicographicalComparatorJavaImpl() {
        return LexicographicalComparatorHolder.PureJavaComparator.INSTANCE;
    }

    static class LexicographicalComparatorHolder {
        static final Comparator<byte[]> BEST_COMPARATOR = UnsignedBytes.lexicographicalComparatorJavaImpl();

        LexicographicalComparatorHolder() {
        }

        enum PureJavaComparator implements Comparator<byte[]> {
            INSTANCE;

            /* JADX DEBUG: Method merged with bridge method: compare(Ljava/lang/Object;Ljava/lang/Object;)I */
            @Override // java.util.Comparator
            public int compare(byte[] bArr, byte[] bArr2) {
                int iMin = Math.min(bArr.length, bArr2.length);
                for (int i = 0; i < iMin; i++) {
                    int iCompare = UnsignedBytes.compare(bArr[i], bArr2[i]);
                    if (iCompare != 0) {
                        return iCompare;
                    }
                }
                return bArr.length - bArr2.length;
            }
        }
    }
}
