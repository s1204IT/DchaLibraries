package com.android.internal.app.procstats;

import com.android.internal.app.procstats.SparseMappingTable;

public class PssTable extends SparseMappingTable.Table {
    public PssTable(SparseMappingTable sparseMappingTable) {
        super(sparseMappingTable);
    }

    public void mergeStats(PssTable pssTable) {
        int keyCount = pssTable.getKeyCount();
        for (int i = 0; i < keyCount; i++) {
            int keyAt = pssTable.getKeyAt(i);
            mergeStats(SparseMappingTable.getIdFromKey(keyAt), (int) pssTable.getValue(keyAt, 0), pssTable.getValue(keyAt, 1), pssTable.getValue(keyAt, 2), pssTable.getValue(keyAt, 3), pssTable.getValue(keyAt, 4), pssTable.getValue(keyAt, 5), pssTable.getValue(keyAt, 6), pssTable.getValue(keyAt, 7), pssTable.getValue(keyAt, 8), pssTable.getValue(keyAt, 9));
        }
    }

    public void mergeStats(int i, int i2, long j, long j2, long j3, long j4, long j5, long j6, long j7, long j8, long j9) {
        int orAddKey = getOrAddKey((byte) i, 10);
        long value = getValue(orAddKey, 0);
        if (value == 0) {
            setValue(orAddKey, 0, i2);
            setValue(orAddKey, 1, j);
            setValue(orAddKey, 2, j2);
            setValue(orAddKey, 3, j3);
            setValue(orAddKey, 4, j4);
            setValue(orAddKey, 5, j5);
            setValue(orAddKey, 6, j6);
            setValue(orAddKey, 7, j7);
            setValue(orAddKey, 8, j8);
            setValue(orAddKey, 9, j9);
            return;
        }
        long j10 = ((long) i2) + value;
        setValue(orAddKey, 0, j10);
        if (getValue(orAddKey, 1) > j) {
            setValue(orAddKey, 1, j);
        }
        double d = value;
        double d2 = i2;
        double d3 = j10;
        setValue(orAddKey, 2, (long) (((getValue(orAddKey, 2) * d) + (j2 * d2)) / d3));
        if (getValue(orAddKey, 3) < j3) {
            setValue(orAddKey, 3, j3);
        }
        if (getValue(orAddKey, 4) > j4) {
            setValue(orAddKey, 4, j4);
        }
        double d4 = j5 * d2;
        setValue(orAddKey, 5, (long) (((getValue(orAddKey, 5) * d) + d4) / d3));
        if (getValue(orAddKey, 6) < j6) {
            setValue(orAddKey, 6, j6);
        }
        if (getValue(orAddKey, 7) > j4) {
            setValue(orAddKey, 7, j4);
        }
        setValue(orAddKey, 8, (long) (((getValue(orAddKey, 8) * d) + d4) / d3));
        if (getValue(orAddKey, 9) < j6) {
            setValue(orAddKey, 9, j6);
        }
    }
}
