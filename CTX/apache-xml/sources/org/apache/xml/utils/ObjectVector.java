package org.apache.xml.utils;

import org.apache.xpath.axes.WalkerFactory;

public class ObjectVector implements Cloneable {
    protected int m_blocksize;
    protected int m_firstFree;
    protected Object[] m_map;
    protected int m_mapSize;

    public ObjectVector() {
        this.m_firstFree = 0;
        this.m_blocksize = 32;
        this.m_mapSize = this.m_blocksize;
        this.m_map = new Object[this.m_blocksize];
    }

    public ObjectVector(int i) {
        this.m_firstFree = 0;
        this.m_blocksize = i;
        this.m_mapSize = i;
        this.m_map = new Object[i];
    }

    public ObjectVector(int i, int i2) {
        this.m_firstFree = 0;
        this.m_blocksize = i2;
        this.m_mapSize = i;
        this.m_map = new Object[i];
    }

    public ObjectVector(ObjectVector objectVector) {
        this.m_firstFree = 0;
        this.m_map = new Object[objectVector.m_mapSize];
        this.m_mapSize = objectVector.m_mapSize;
        this.m_firstFree = objectVector.m_firstFree;
        this.m_blocksize = objectVector.m_blocksize;
        System.arraycopy(objectVector.m_map, 0, this.m_map, 0, this.m_firstFree);
    }

    public final int size() {
        return this.m_firstFree;
    }

    public final void setSize(int i) {
        this.m_firstFree = i;
    }

    public final void addElement(Object obj) {
        if (this.m_firstFree + 1 >= this.m_mapSize) {
            this.m_mapSize += this.m_blocksize;
            Object[] objArr = new Object[this.m_mapSize];
            System.arraycopy(this.m_map, 0, objArr, 0, this.m_firstFree + 1);
            this.m_map = objArr;
        }
        this.m_map[this.m_firstFree] = obj;
        this.m_firstFree++;
    }

    public final void addElements(Object obj, int i) {
        if (this.m_firstFree + i >= this.m_mapSize) {
            this.m_mapSize += this.m_blocksize + i;
            Object[] objArr = new Object[this.m_mapSize];
            System.arraycopy(this.m_map, 0, objArr, 0, this.m_firstFree + 1);
            this.m_map = objArr;
        }
        for (int i2 = 0; i2 < i; i2++) {
            this.m_map[this.m_firstFree] = obj;
            this.m_firstFree++;
        }
    }

    public final void addElements(int i) {
        if (this.m_firstFree + i >= this.m_mapSize) {
            this.m_mapSize += this.m_blocksize + i;
            Object[] objArr = new Object[this.m_mapSize];
            System.arraycopy(this.m_map, 0, objArr, 0, this.m_firstFree + 1);
            this.m_map = objArr;
        }
        this.m_firstFree += i;
    }

    public final void insertElementAt(Object obj, int i) {
        if (this.m_firstFree + 1 >= this.m_mapSize) {
            this.m_mapSize += this.m_blocksize;
            Object[] objArr = new Object[this.m_mapSize];
            System.arraycopy(this.m_map, 0, objArr, 0, this.m_firstFree + 1);
            this.m_map = objArr;
        }
        if (i <= this.m_firstFree - 1) {
            System.arraycopy(this.m_map, i, this.m_map, i + 1, this.m_firstFree - i);
        }
        this.m_map[i] = obj;
        this.m_firstFree++;
    }

    public final void removeAllElements() {
        for (int i = 0; i < this.m_firstFree; i++) {
            this.m_map[i] = null;
        }
        this.m_firstFree = 0;
    }

    public final boolean removeElement(Object obj) {
        for (int i = 0; i < this.m_firstFree; i++) {
            if (this.m_map[i] == obj) {
                int i2 = i + 1;
                if (i2 < this.m_firstFree) {
                    System.arraycopy(this.m_map, i2, this.m_map, i - 1, this.m_firstFree - i);
                } else {
                    this.m_map[i] = null;
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
            this.m_map[i] = null;
        }
        this.m_firstFree--;
    }

    public final void setElementAt(Object obj, int i) {
        this.m_map[i] = obj;
    }

    public final Object elementAt(int i) {
        return this.m_map[i];
    }

    public final boolean contains(Object obj) {
        for (int i = 0; i < this.m_firstFree; i++) {
            if (this.m_map[i] == obj) {
                return true;
            }
        }
        return false;
    }

    public final int indexOf(Object obj, int i) {
        while (i < this.m_firstFree) {
            if (this.m_map[i] != obj) {
                i++;
            } else {
                return i;
            }
        }
        return WalkerFactory.BIT_MATCH_PATTERN;
    }

    public final int indexOf(Object obj) {
        for (int i = 0; i < this.m_firstFree; i++) {
            if (this.m_map[i] == obj) {
                return i;
            }
        }
        return WalkerFactory.BIT_MATCH_PATTERN;
    }

    public final int lastIndexOf(Object obj) {
        for (int i = this.m_firstFree - 1; i >= 0; i--) {
            if (this.m_map[i] == obj) {
                return i;
            }
        }
        return WalkerFactory.BIT_MATCH_PATTERN;
    }

    public final void setToSize(int i) {
        Object[] objArr = new Object[i];
        System.arraycopy(this.m_map, 0, objArr, 0, this.m_firstFree);
        this.m_mapSize = i;
        this.m_map = objArr;
    }

    public Object clone() throws CloneNotSupportedException {
        return new ObjectVector(this);
    }
}
