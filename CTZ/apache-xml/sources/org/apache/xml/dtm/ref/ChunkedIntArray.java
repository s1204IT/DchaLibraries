package org.apache.xml.dtm.ref;

import java.io.PrintStream;
import java.util.Objects;
import org.apache.xml.res.XMLErrorResources;
import org.apache.xml.res.XMLMessages;

final class ChunkedIntArray {
    static final int chunkalloc = 1024;
    static final int lowbits = 10;
    static final int lowmask = 1023;
    final int slotsize = 4;
    ChunksVector chunks = new ChunksVector();
    final int[] fastArray = new int[1024];
    int lastUsed = 0;

    ChunkedIntArray(int i) {
        Objects.requireNonNull(this);
        if (4 < i) {
            throw new ArrayIndexOutOfBoundsException(XMLMessages.createXMLMessage(XMLErrorResources.ER_CHUNKEDINTARRAY_NOT_SUPPORTED, new Object[]{Integer.toString(i)}));
        }
        Objects.requireNonNull(this);
        if (4 > i) {
            PrintStream printStream = System.out;
            StringBuilder sb = new StringBuilder();
            sb.append("*****WARNING: ChunkedIntArray(");
            sb.append(i);
            sb.append(") wasting ");
            Objects.requireNonNull(this);
            sb.append(4 - i);
            sb.append(" words per slot");
            printStream.println(sb.toString());
        }
        this.chunks.addElement(this.fastArray);
    }

    int appendSlot(int i, int i2, int i3, int i4) {
        int i5 = (this.lastUsed + 1) * 4;
        int i6 = i5 >> 10;
        int i7 = i5 & lowmask;
        if (i6 > this.chunks.size() - 1) {
            this.chunks.addElement(new int[1024]);
        }
        int[] iArrElementAt = this.chunks.elementAt(i6);
        iArrElementAt[i7] = i;
        iArrElementAt[i7 + 1] = i2;
        iArrElementAt[i7 + 2] = i3;
        iArrElementAt[i7 + 3] = i4;
        int i8 = this.lastUsed + 1;
        this.lastUsed = i8;
        return i8;
    }

    int readEntry(int i, int i2) throws ArrayIndexOutOfBoundsException {
        if (i2 >= 4) {
            throw new ArrayIndexOutOfBoundsException(XMLMessages.createXMLMessage(XMLErrorResources.ER_OFFSET_BIGGER_THAN_SLOT, null));
        }
        int i3 = i * 4;
        return this.chunks.elementAt(i3 >> 10)[(i3 & lowmask) + i2];
    }

    int specialFind(int i, int i2) {
        while (i > 0) {
            int i3 = i * 4;
            i = this.chunks.elementAt(i3 >> 10)[(i3 & lowmask) + 1];
            if (i == i2) {
                break;
            }
        }
        if (i <= 0) {
            return i2;
        }
        return -1;
    }

    int slotsUsed() {
        return this.lastUsed;
    }

    void discardLast() {
        this.lastUsed--;
    }

    void writeEntry(int i, int i2, int i3) throws ArrayIndexOutOfBoundsException {
        if (i2 >= 4) {
            throw new ArrayIndexOutOfBoundsException(XMLMessages.createXMLMessage(XMLErrorResources.ER_OFFSET_BIGGER_THAN_SLOT, null));
        }
        int i4 = i * 4;
        this.chunks.elementAt(i4 >> 10)[(i4 & lowmask) + i2] = i3;
    }

    void writeSlot(int i, int i2, int i3, int i4, int i5) {
        int i6 = i * 4;
        int i7 = i6 >> 10;
        int i8 = i6 & lowmask;
        if (i7 > this.chunks.size() - 1) {
            this.chunks.addElement(new int[1024]);
        }
        int[] iArrElementAt = this.chunks.elementAt(i7);
        iArrElementAt[i8] = i2;
        iArrElementAt[i8 + 1] = i3;
        iArrElementAt[i8 + 2] = i4;
        iArrElementAt[i8 + 3] = i5;
    }

    void readSlot(int i, int[] iArr) {
        int i2 = i * 4;
        int i3 = i2 >> 10;
        int i4 = i2 & lowmask;
        if (i3 > this.chunks.size() - 1) {
            this.chunks.addElement(new int[1024]);
        }
        System.arraycopy(this.chunks.elementAt(i3), i4, iArr, 0, 4);
    }

    class ChunksVector {
        final int BLOCKSIZE = 64;
        int[][] m_map = new int[64][];
        int m_mapSize = 64;
        int pos = 0;

        ChunksVector() {
        }

        final int size() {
            return this.pos;
        }

        void addElement(int[] iArr) {
            if (this.pos >= this.m_mapSize) {
                int i = this.m_mapSize;
                while (this.pos >= this.m_mapSize) {
                    this.m_mapSize += 64;
                }
                int[][] iArr2 = new int[this.m_mapSize][];
                System.arraycopy(this.m_map, 0, iArr2, 0, i);
                this.m_map = iArr2;
            }
            this.m_map[this.pos] = iArr;
            this.pos++;
        }

        final int[] elementAt(int i) {
            return this.m_map[i];
        }
    }
}
