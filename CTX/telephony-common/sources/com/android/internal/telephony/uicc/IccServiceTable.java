package com.android.internal.telephony.uicc;

import android.telephony.Rlog;

public abstract class IccServiceTable {
    protected final byte[] mServiceTable;

    protected abstract String getTag();

    protected abstract Object[] getValues();

    protected IccServiceTable(byte[] bArr) {
        this.mServiceTable = bArr;
    }

    public boolean isAvailable(int i) {
        int i2 = i / 8;
        if (i2 < this.mServiceTable.length) {
            return ((1 << (i % 8)) & this.mServiceTable[i2]) != 0;
        }
        Rlog.e(getTag(), "isAvailable for service " + (i + 1) + " fails, max service is " + (this.mServiceTable.length * 8));
        return false;
    }

    public String toString() {
        Object[] values = getValues();
        int length = this.mServiceTable.length;
        StringBuilder sb = new StringBuilder(getTag());
        sb.append('[');
        sb.append(length * 8);
        sb.append("]={ ");
        int i = 0;
        boolean z = false;
        while (i < length) {
            byte b = this.mServiceTable[i];
            boolean z2 = z;
            for (int i2 = 0; i2 < 8; i2++) {
                if (((1 << i2) & b) != 0) {
                    if (z2) {
                        sb.append(", ");
                    } else {
                        z2 = true;
                    }
                    int i3 = (i * 8) + i2;
                    if (i3 < values.length) {
                        sb.append(values[i3]);
                    } else {
                        sb.append('#');
                        sb.append(i3 + 1);
                    }
                }
            }
            i++;
            z = z2;
        }
        sb.append(" }");
        return sb.toString();
    }
}
