package org.apache.xml.utils;

import org.apache.xpath.axes.WalkerFactory;

public class IntVector implements Cloneable {
    protected int m_blocksize;
    protected int m_firstFree;
    protected int[] m_map;
    protected int m_mapSize;

    public IntVector() {
        this.m_firstFree = 0;
        this.m_blocksize = 32;
        this.m_mapSize = this.m_blocksize;
        this.m_map = new int[this.m_blocksize];
    }

    public IntVector(int i) {
        this.m_firstFree = 0;
        this.m_blocksize = i;
        this.m_mapSize = i;
        this.m_map = new int[i];
    }

    public IntVector(int i, int i2) {
        this.m_firstFree = 0;
        this.m_blocksize = i2;
        this.m_mapSize = i;
        this.m_map = new int[i];
    }

    public IntVector(IntVector intVector) {
        this.m_firstFree = 0;
        this.m_map = new int[intVector.m_mapSize];
        this.m_mapSize = intVector.m_mapSize;
        this.m_firstFree = intVector.m_firstFree;
        this.m_blocksize = intVector.m_blocksize;
        System.arraycopy(intVector.m_map, 0, this.m_map, 0, this.m_firstFree);
    }

    public final int size() {
        return this.m_firstFree;
    }

    public final void setSize(int i) {
        this.m_firstFree = i;
    }

    public final void addElement(int i) {
        if (this.m_firstFree + 1 >= this.m_mapSize) {
            this.m_mapSize += this.m_blocksize;
            int[] iArr = new int[this.m_mapSize];
            System.arraycopy(this.m_map, 0, iArr, 0, this.m_firstFree + 1);
            this.m_map = iArr;
        }
        this.m_map[this.m_firstFree] = i;
        this.m_firstFree++;
    }

    public final void addElements(int i, int i2) {
        if (this.m_firstFree + i2 >= this.m_mapSize) {
            this.m_mapSize += this.m_blocksize + i2;
            int[] iArr = new int[this.m_mapSize];
            System.arraycopy(this.m_map, 0, iArr, 0, this.m_firstFree + 1);
            this.m_map = iArr;
        }
        for (int i3 = 0; i3 < i2; i3++) {
            this.m_map[this.m_firstFree] = i;
            this.m_firstFree++;
        }
    }

    public final void addElements(int i) {
        if (this.m_firstFree + i >= this.m_mapSize) {
            this.m_mapSize += this.m_blocksize + i;
            int[] iArr = new int[this.m_mapSize];
            System.arraycopy(this.m_map, 0, iArr, 0, this.m_firstFree + 1);
            this.m_map = iArr;
        }
        this.m_firstFree += i;
    }

    public final void insertElementAt(int i, int i2) {
        if (this.m_firstFree + 1 >= this.m_mapSize) {
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

    public final void removeAllElements() {
        for (int i = 0; i < this.m_firstFree; i++) {
            this.m_map[i] = Integer.MIN_VALUE;
        }
        this.m_firstFree = 0;
    }

    public final boolean removeElement(int i) {
        for (int i2 = 0; i2 < this.m_firstFree; i2++) {
            if (this.m_map[i2] == i) {
                int i3 = i2 + 1;
                if (i3 < this.m_firstFree) {
                    System.arraycopy(this.m_map, i3, this.m_map, i2 - 1, this.m_firstFree - i2);
                } else {
                    this.m_map[i2] = Integer.MIN_VALUE;
                }
                this.m_firstFree--;
                return true;
            }
        }
        return false;
    }

    public final void removeElementAt(int i) {
        if (i > this.m_firstFree) {
            System.arraycopy(this.m_map, i + 1, this.m_map, i, this.m_firstFree);
        } else {
            this.m_map[i] = Integer.MIN_VALUE;
        }
        this.m_firstFree--;
    }

    public final void setElementAt(int i, int i2) {
        this.m_map[i2] = i;
    }

    public final int elementAt(int i) {
        return this.m_map[i];
    }

    public final boolean contains(int i) {
        for (int i2 = 0; i2 < this.m_firstFree; i2++) {
            if (this.m_map[i2] == i) {
                return true;
            }
        }
        return false;
    }

    public final int indexOf(int i, int i2) {
        while (i2 < this.m_firstFree) {
            if (this.m_map[i2] != i) {
                i2++;
            } else {
                return i2;
            }
        }
        return WalkerFactory.BIT_MATCH_PATTERN;
    }

    public final int indexOf(int i) {
        for (int i2 = 0; i2 < this.m_firstFree; i2++) {
            if (this.m_map[i2] == i) {
                return i2;
            }
        }
        return WalkerFactory.BIT_MATCH_PATTERN;
    }

    public final int lastIndexOf(int i) {
        for (int i2 = this.m_firstFree - 1; i2 >= 0; i2--) {
            if (this.m_map[i2] == i) {
                return i2;
            }
        }
        return WalkerFactory.BIT_MATCH_PATTERN;
    }

    public Object clone() throws CloneNotSupportedException {
        return new IntVector(this);
    }
}
