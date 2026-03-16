package org.apache.xml.dtm.ref;

import java.io.PrintStream;
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

    ChunkedIntArray(int slotsize) {
        getClass();
        if (4 < slotsize) {
            throw new ArrayIndexOutOfBoundsException(XMLMessages.createXMLMessage(XMLErrorResources.ER_CHUNKEDINTARRAY_NOT_SUPPORTED, new Object[]{Integer.toString(slotsize)}));
        }
        getClass();
        if (4 > slotsize) {
            PrintStream printStream = System.out;
            StringBuilder sbAppend = new StringBuilder().append("*****WARNING: ChunkedIntArray(").append(slotsize).append(") wasting ");
            getClass();
            printStream.println(sbAppend.append(4 - slotsize).append(" words per slot").toString());
        }
        this.chunks.addElement(this.fastArray);
    }

    int appendSlot(int w0, int w1, int w2, int w3) {
        int newoffset = (this.lastUsed + 1) * 4;
        int chunkpos = newoffset >> 10;
        int slotpos = newoffset & lowmask;
        if (chunkpos > this.chunks.size() - 1) {
            this.chunks.addElement(new int[1024]);
        }
        int[] chunk = this.chunks.elementAt(chunkpos);
        chunk[slotpos] = w0;
        chunk[slotpos + 1] = w1;
        chunk[slotpos + 2] = w2;
        chunk[slotpos + 3] = w3;
        int i = this.lastUsed + 1;
        this.lastUsed = i;
        return i;
    }

    int readEntry(int position, int offset) throws ArrayIndexOutOfBoundsException {
        if (offset >= 4) {
            throw new ArrayIndexOutOfBoundsException(XMLMessages.createXMLMessage(XMLErrorResources.ER_OFFSET_BIGGER_THAN_SLOT, null));
        }
        int position2 = position * 4;
        int chunkpos = position2 >> 10;
        int slotpos = position2 & lowmask;
        int[] chunk = this.chunks.elementAt(chunkpos);
        return chunk[slotpos + offset];
    }

    int specialFind(int startPos, int position) {
        int ancestor = startPos;
        while (ancestor > 0) {
            int ancestor2 = ancestor * 4;
            int chunkpos = ancestor2 >> 10;
            int slotpos = ancestor2 & lowmask;
            int[] chunk = this.chunks.elementAt(chunkpos);
            ancestor = chunk[slotpos + 1];
            if (ancestor == position) {
                break;
            }
        }
        if (ancestor <= 0) {
            return position;
        }
        return -1;
    }

    int slotsUsed() {
        return this.lastUsed;
    }

    void discardLast() {
        this.lastUsed--;
    }

    void writeEntry(int position, int offset, int value) throws ArrayIndexOutOfBoundsException {
        if (offset >= 4) {
            throw new ArrayIndexOutOfBoundsException(XMLMessages.createXMLMessage(XMLErrorResources.ER_OFFSET_BIGGER_THAN_SLOT, null));
        }
        int position2 = position * 4;
        int chunkpos = position2 >> 10;
        int slotpos = position2 & lowmask;
        int[] chunk = this.chunks.elementAt(chunkpos);
        chunk[slotpos + offset] = value;
    }

    void writeSlot(int position, int w0, int w1, int w2, int w3) {
        int position2 = position * 4;
        int chunkpos = position2 >> 10;
        int slotpos = position2 & lowmask;
        if (chunkpos > this.chunks.size() - 1) {
            this.chunks.addElement(new int[1024]);
        }
        int[] chunk = this.chunks.elementAt(chunkpos);
        chunk[slotpos] = w0;
        chunk[slotpos + 1] = w1;
        chunk[slotpos + 2] = w2;
        chunk[slotpos + 3] = w3;
    }

    void readSlot(int position, int[] buffer) {
        int position2 = position * 4;
        int chunkpos = position2 >> 10;
        int slotpos = position2 & lowmask;
        if (chunkpos > this.chunks.size() - 1) {
            this.chunks.addElement(new int[1024]);
        }
        int[] chunk = this.chunks.elementAt(chunkpos);
        System.arraycopy(chunk, slotpos, buffer, 0, 4);
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

        void addElement(int[] value) {
            if (this.pos >= this.m_mapSize) {
                int orgMapSize = this.m_mapSize;
                while (this.pos >= this.m_mapSize) {
                    this.m_mapSize += 64;
                }
                int[][] newMap = new int[this.m_mapSize][];
                System.arraycopy(this.m_map, 0, newMap, 0, orgMapSize);
                this.m_map = newMap;
            }
            this.m_map[this.pos] = value;
            this.pos++;
        }

        final int[] elementAt(int pos) {
            return this.m_map[pos];
        }
    }
}
