package org.tukaani.xz.index;

import org.tukaani.xz.XZIOException;
import org.tukaani.xz.common.Util;

abstract class IndexBase {
    private final XZIOException invalidIndexException;
    long blocksSum = 0;
    long uncompressedSum = 0;
    long indexListSize = 0;
    long recordCount = 0;

    IndexBase(XZIOException xZIOException) {
        this.invalidIndexException = xZIOException;
    }

    private long getUnpaddedIndexSize() {
        return ((long) (1 + Util.getVLISize(this.recordCount))) + this.indexListSize + 4;
    }

    public long getIndexSize() {
        return (getUnpaddedIndexSize() + 3) & (-4);
    }

    public long getStreamSize() {
        return this.blocksSum + 12 + getIndexSize() + 12;
    }

    int getIndexPaddingSize() {
        return (int) (3 & (4 - getUnpaddedIndexSize()));
    }

    void add(long j, long j2) throws XZIOException {
        this.blocksSum += (3 + j) & (-4);
        this.uncompressedSum += j2;
        this.indexListSize += (long) (Util.getVLISize(j) + Util.getVLISize(j2));
        this.recordCount++;
        if (this.blocksSum < 0 || this.uncompressedSum < 0 || getIndexSize() > 17179869184L || getStreamSize() < 0) {
            throw this.invalidIndexException;
        }
    }
}
