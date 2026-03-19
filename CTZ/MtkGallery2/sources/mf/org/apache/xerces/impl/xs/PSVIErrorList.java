package mf.org.apache.xerces.impl.xs;

import java.util.AbstractList;
import mf.org.apache.xerces.xs.StringList;

final class PSVIErrorList extends AbstractList implements StringList {
    private final String[] fArray;
    private final int fLength;
    private final int fOffset;

    public PSVIErrorList(String[] strArr, boolean z) {
        this.fArray = strArr;
        this.fLength = this.fArray.length >> 1;
        this.fOffset = !z ? 1 : 0;
    }

    @Override
    public boolean contains(String item) {
        if (item == null) {
            for (int i = 0; i < this.fLength; i++) {
                if (this.fArray[(i << 1) + this.fOffset] == null) {
                    return true;
                }
            }
            return false;
        }
        for (int i2 = 0; i2 < this.fLength; i2++) {
            if (item.equals(this.fArray[(i2 << 1) + this.fOffset])) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int getLength() {
        return this.fLength;
    }

    @Override
    public String item(int index) {
        if (index < 0 || index >= this.fLength) {
            return null;
        }
        return this.fArray[(index << 1) + this.fOffset];
    }

    @Override
    public Object get(int index) {
        if (index >= 0 && index < this.fLength) {
            return this.fArray[(index << 1) + this.fOffset];
        }
        throw new IndexOutOfBoundsException("Index: " + index);
    }

    @Override
    public int size() {
        return getLength();
    }
}
