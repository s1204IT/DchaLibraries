package com.android.org.bouncycastle.crypto.modes.gcm;

import com.android.org.bouncycastle.util.Arrays;
import java.util.Vector;

public class Tables1kGCMExponentiator implements GCMExponentiator {
    private Vector lookupPowX2;

    @Override
    public void init(byte[] bArr) {
        int[] iArrAsInts = GCMUtil.asInts(bArr);
        if (this.lookupPowX2 != null && Arrays.areEqual(iArrAsInts, (int[]) this.lookupPowX2.elementAt(0))) {
            return;
        }
        this.lookupPowX2 = new Vector(8);
        this.lookupPowX2.addElement(iArrAsInts);
    }

    @Override
    public void exponentiateX(long j, byte[] bArr) {
        int[] iArrOneAsInts = GCMUtil.oneAsInts();
        int i = 0;
        while (j > 0) {
            if ((1 & j) != 0) {
                ensureAvailable(i);
                GCMUtil.multiply(iArrOneAsInts, (int[]) this.lookupPowX2.elementAt(i));
            }
            i++;
            j >>>= 1;
        }
        GCMUtil.asBytes(iArrOneAsInts, bArr);
    }

    private void ensureAvailable(int i) {
        int size = this.lookupPowX2.size();
        if (size <= i) {
            int[] iArrClone = (int[]) this.lookupPowX2.elementAt(size - 1);
            do {
                iArrClone = Arrays.clone(iArrClone);
                GCMUtil.multiply(iArrClone, iArrClone);
                this.lookupPowX2.addElement(iArrClone);
                size++;
            } while (size <= i);
        }
    }
}
