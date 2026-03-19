package org.apache.xml.utils;

import java.io.Serializable;

public class NodeVector implements Serializable, Cloneable {
    static final long serialVersionUID = -713473092200731870L;
    private int m_blocksize;
    protected int m_firstFree;
    private int[] m_map;
    private int m_mapSize;

    public NodeVector() {
        this.m_firstFree = 0;
        this.m_blocksize = 32;
        this.m_mapSize = 0;
    }

    public NodeVector(int i) {
        this.m_firstFree = 0;
        this.m_blocksize = i;
        this.m_mapSize = 0;
    }

    public Object clone() throws CloneNotSupportedException {
        NodeVector nodeVector = (NodeVector) super.clone();
        if (this.m_map != null && this.m_map == nodeVector.m_map) {
            nodeVector.m_map = new int[this.m_map.length];
            System.arraycopy(this.m_map, 0, nodeVector.m_map, 0, this.m_map.length);
        }
        return nodeVector;
    }

    public int size() {
        return this.m_firstFree;
    }

    public void addElement(int i) {
        if (this.m_firstFree + 1 >= this.m_mapSize) {
            if (this.m_map == null) {
                this.m_map = new int[this.m_blocksize];
                this.m_mapSize = this.m_blocksize;
            } else {
                this.m_mapSize += this.m_blocksize;
                int[] iArr = new int[this.m_mapSize];
                System.arraycopy(this.m_map, 0, iArr, 0, this.m_firstFree + 1);
                this.m_map = iArr;
            }
        }
        this.m_map[this.m_firstFree] = i;
        this.m_firstFree++;
    }

    public final void push(int i) {
        int i2 = this.m_firstFree;
        int i3 = i2 + 1;
        if (i3 >= this.m_mapSize) {
            if (this.m_map == null) {
                this.m_map = new int[this.m_blocksize];
                this.m_mapSize = this.m_blocksize;
            } else {
                this.m_mapSize += this.m_blocksize;
                int[] iArr = new int[this.m_mapSize];
                System.arraycopy(this.m_map, 0, iArr, 0, i3);
                this.m_map = iArr;
            }
        }
        this.m_map[i2] = i;
        this.m_firstFree = i3;
    }

    public final int pop() {
        this.m_firstFree--;
        int i = this.m_map[this.m_firstFree];
        this.m_map[this.m_firstFree] = -1;
        return i;
    }

    public final int popAndTop() {
        this.m_firstFree--;
        this.m_map[this.m_firstFree] = -1;
        if (this.m_firstFree == 0) {
            return -1;
        }
        return this.m_map[this.m_firstFree - 1];
    }

    public final void popQuick() {
        this.m_firstFree--;
        this.m_map[this.m_firstFree] = -1;
    }

    public final int peepOrNull() {
        if (this.m_map == null || this.m_firstFree <= 0) {
            return -1;
        }
        return this.m_map[this.m_firstFree - 1];
    }

    public final void pushPair(int i, int i2) {
        if (this.m_map == null) {
            this.m_map = new int[this.m_blocksize];
            this.m_mapSize = this.m_blocksize;
        } else if (this.m_firstFree + 2 >= this.m_mapSize) {
            this.m_mapSize += this.m_blocksize;
            int[] iArr = new int[this.m_mapSize];
            System.arraycopy(this.m_map, 0, iArr, 0, this.m_firstFree);
            this.m_map = iArr;
        }
        this.m_map[this.m_firstFree] = i;
        this.m_map[this.m_firstFree + 1] = i2;
        this.m_firstFree += 2;
    }

    public final void popPair() {
        this.m_firstFree -= 2;
        this.m_map[this.m_firstFree] = -1;
        this.m_map[this.m_firstFree + 1] = -1;
    }

    public final void setTail(int i) {
        this.m_map[this.m_firstFree - 1] = i;
    }

    public final void setTailSub1(int i) {
        this.m_map[this.m_firstFree - 2] = i;
    }

    public final int peepTail() {
        return this.m_map[this.m_firstFree - 1];
    }

    public final int peepTailSub1() {
        return this.m_map[this.m_firstFree - 2];
    }

    public void insertInOrder(int i) {
        for (int i2 = 0; i2 < this.m_firstFree; i2++) {
            if (i < this.m_map[i2]) {
                insertElementAt(i, i2);
                return;
            }
        }
        addElement(i);
    }

    public void insertElementAt(int i, int i2) {
        if (this.m_map == null) {
            this.m_map = new int[this.m_blocksize];
            this.m_mapSize = this.m_blocksize;
        } else if (this.m_firstFree + 1 >= this.m_mapSize) {
            this.m_mapSize += this.m_blocksize;
            int[] iArr = new int[this.m_mapSize];
            System.arraycopy(this.m_map, 0, iArr, 0, this.m_firstFree + 1);
            this.m_map = iArr;
        }
        if (i2 <= this.m_firstFree - 1) {
            System.arraycopy(this.m_map, i2, this.m_map, i2 + 1, this.m_firstFree - i2);
        }
        this.m_map[i2] = i;
        this.m_firstFree++;
    }

    public void appendNodes(NodeVector nodeVector) {
        int size = nodeVector.size();
        if (this.m_map == null) {
            this.m_mapSize = this.m_blocksize + size;
            this.m_map = new int[this.m_mapSize];
        } else if (this.m_firstFree + size >= this.m_mapSize) {
            this.m_mapSize += this.m_blocksize + size;
            int[] iArr = new int[this.m_mapSize];
            System.arraycopy(this.m_map, 0, iArr, 0, this.m_firstFree + size);
            this.m_map = iArr;
        }
        System.arraycopy(nodeVector.m_map, 0, this.m_map, this.m_firstFree, size);
        this.m_firstFree += size;
    }

    public void removeAllElements() {
        if (this.m_map == null) {
            return;
        }
        for (int i = 0; i < this.m_firstFree; i++) {
            this.m_map[i] = -1;
        }
        this.m_firstFree = 0;
    }

    public void RemoveAllNoClear() {
        if (this.m_map == null) {
            return;
        }
        this.m_firstFree = 0;
    }

    public boolean removeElement(int i) {
        if (this.m_map == null) {
            return false;
        }
        for (int i2 = 0; i2 < this.m_firstFree; i2++) {
            if (this.m_map[i2] == i) {
                if (i2 > this.m_firstFree) {
                    System.arraycopy(this.m_map, i2 + 1, this.m_map, i2 - 1, this.m_firstFree - i2);
                } else {
                    this.m_map[i2] = -1;
                }
                this.m_firstFree--;
                return true;
            }
        }
        return false;
    }

    public void removeElementAt(int i) {
        if (this.m_map == null) {
            return;
        }
        if (i > this.m_firstFree) {
            System.arraycopy(this.m_map, i + 1, this.m_map, i - 1, this.m_firstFree - i);
        } else {
            this.m_map[i] = -1;
        }
    }

    public void setElementAt(int i, int i2) {
        if (this.m_map == null) {
            this.m_map = new int[this.m_blocksize];
            this.m_mapSize = this.m_blocksize;
        }
        if (i2 == -1) {
            addElement(i);
        }
        this.m_map[i2] = i;
    }

    public int elementAt(int i) {
        if (this.m_map == null) {
            return -1;
        }
        return this.m_map[i];
    }

    public boolean contains(int i) {
        if (this.m_map == null) {
            return false;
        }
        for (int i2 = 0; i2 < this.m_firstFree; i2++) {
            if (this.m_map[i2] == i) {
                return true;
            }
        }
        return false;
    }

    public int indexOf(int i, int i2) {
        if (this.m_map == null) {
            return -1;
        }
        while (i2 < this.m_firstFree) {
            if (this.m_map[i2] != i) {
                i2++;
            } else {
                return i2;
            }
        }
        return -1;
    }

    public int indexOf(int i) {
        if (this.m_map == null) {
            return -1;
        }
        for (int i2 = 0; i2 < this.m_firstFree; i2++) {
            if (this.m_map[i2] == i) {
                return i2;
            }
        }
        return -1;
    }

    public void sort(int[] iArr, int i, int i2) throws Exception {
        if (i >= i2) {
            return;
        }
        if (i == i2 - 1) {
            if (iArr[i] > iArr[i2]) {
                int i3 = iArr[i];
                iArr[i] = iArr[i2];
                iArr[i2] = i3;
                return;
            }
            return;
        }
        int i4 = (i + i2) / 2;
        int i5 = iArr[i4];
        iArr[i4] = iArr[i2];
        iArr[i2] = i5;
        int i6 = i;
        int i7 = i2;
        while (i6 < i7) {
            while (iArr[i6] <= i5 && i6 < i7) {
                i6++;
            }
            while (i5 <= iArr[i7] && i6 < i7) {
                i7--;
            }
            if (i6 < i7) {
                int i8 = iArr[i6];
                iArr[i6] = iArr[i7];
                iArr[i7] = i8;
            }
        }
        iArr[i2] = iArr[i7];
        iArr[i7] = i5;
        sort(iArr, i, i6 - 1);
        sort(iArr, i7 + 1, i2);
    }

    public void sort() throws Exception {
        sort(this.m_map, 0, this.m_firstFree - 1);
    }
}
