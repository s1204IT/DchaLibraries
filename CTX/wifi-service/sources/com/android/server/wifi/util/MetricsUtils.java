package com.android.server.wifi.util;

import android.util.SparseIntArray;

public class MetricsUtils {

    public static class GenericBucket {
        public int count;
        public long end;
        public long start;
    }

    public static class LogHistParms {
        public int b;
        public double[] bb;
        public int m;
        public double mLog;
        public int n;
        public int p;
        public int s;
        public double[] sbw;

        public LogHistParms(int i, int i2, int i3, int i4, int i5) {
            this.b = i;
            this.p = i2;
            this.m = i3;
            this.s = i4;
            this.n = i5;
            double d = i3;
            this.mLog = Math.log(d);
            this.bb = new double[i5];
            this.sbw = new double[i5];
            this.bb[0] = i + i2;
            this.sbw[0] = (((double) i2) * (d - 1.0d)) / ((double) i4);
            for (int i6 = 1; i6 < i5; i6++) {
                int i7 = i6 - 1;
                double d2 = i;
                this.bb[i6] = ((this.bb[i7] - d2) * d) + d2;
                this.sbw[i6] = this.sbw[i7] * d;
            }
        }
    }

    public static int addValueToLogHistogram(long j, SparseIntArray sparseIntArray, LogHistParms logHistParms) {
        int iLog;
        double d = (j - ((long) logHistParms.b)) / ((double) logHistParms.p);
        if (d > 0.0d) {
            iLog = (int) (Math.log(d) / logHistParms.mLog);
        } else {
            iLog = -1;
        }
        int i = 0;
        if (iLog >= 0) {
            if (iLog >= logHistParms.n) {
                iLog = logHistParms.n - 1;
                i = logHistParms.s - 1;
            } else {
                double d2 = j;
                i = (int) ((d2 - logHistParms.bb[iLog]) / logHistParms.sbw[iLog]);
                if (i >= logHistParms.s) {
                    iLog++;
                    if (iLog >= logHistParms.n) {
                        iLog = logHistParms.n - 1;
                        i = logHistParms.s - 1;
                    } else {
                        i = (int) ((d2 - logHistParms.bb[iLog]) / logHistParms.sbw[iLog]);
                    }
                }
            }
        } else {
            iLog = 0;
        }
        int i2 = (iLog * logHistParms.s) + i;
        int i3 = sparseIntArray.get(i2) + 1;
        sparseIntArray.put(i2, i3);
        return i3;
    }

    public static GenericBucket[] logHistogramToGenericBuckets(SparseIntArray sparseIntArray, LogHistParms logHistParms) {
        GenericBucket[] genericBucketArr = new GenericBucket[sparseIntArray.size()];
        for (int i = 0; i < sparseIntArray.size(); i++) {
            int iKeyAt = sparseIntArray.keyAt(i);
            genericBucketArr[i] = new GenericBucket();
            genericBucketArr[i].start = (long) (logHistParms.bb[iKeyAt / logHistParms.s] + (logHistParms.sbw[iKeyAt / logHistParms.s] * ((double) (iKeyAt % logHistParms.s))));
            genericBucketArr[i].end = (long) (genericBucketArr[i].start + logHistParms.sbw[iKeyAt / logHistParms.s]);
            genericBucketArr[i].count = sparseIntArray.valueAt(i);
        }
        return genericBucketArr;
    }

    public static int addValueToLinearHistogram(int i, SparseIntArray sparseIntArray, int[] iArr) {
        int length = iArr.length;
        int i2 = 0;
        for (int i3 = 0; i3 < length && i >= iArr[i3]; i3++) {
            i2++;
        }
        int i4 = sparseIntArray.get(i2) + 1;
        sparseIntArray.put(i2, i4);
        return i4;
    }

    public static GenericBucket[] linearHistogramToGenericBuckets(SparseIntArray sparseIntArray, int[] iArr) {
        GenericBucket[] genericBucketArr = new GenericBucket[sparseIntArray.size()];
        for (int i = 0; i < sparseIntArray.size(); i++) {
            int iKeyAt = sparseIntArray.keyAt(i);
            genericBucketArr[i] = new GenericBucket();
            if (iKeyAt == 0) {
                genericBucketArr[i].start = -2147483648L;
                genericBucketArr[i].end = iArr[0];
            } else if (iKeyAt != iArr.length) {
                genericBucketArr[i].start = iArr[iKeyAt - 1];
                genericBucketArr[i].end = iArr[iKeyAt];
            } else {
                genericBucketArr[i].start = iArr[iArr.length - 1];
                genericBucketArr[i].end = 2147483647L;
            }
            genericBucketArr[i].count = sparseIntArray.valueAt(i);
        }
        return genericBucketArr;
    }
}
