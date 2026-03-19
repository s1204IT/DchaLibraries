package org.apache.xml.utils;

import java.io.Serializable;

public class StringVector implements Serializable {
    static final long serialVersionUID = 4995234972032919748L;
    protected int m_blocksize;
    protected int m_firstFree;
    protected String[] m_map;
    protected int m_mapSize;

    public StringVector() {
        this.m_firstFree = 0;
        this.m_blocksize = 8;
        this.m_mapSize = this.m_blocksize;
        this.m_map = new String[this.m_blocksize];
    }

    public StringVector(int i) {
        this.m_firstFree = 0;
        this.m_blocksize = i;
        this.m_mapSize = i;
        this.m_map = new String[i];
    }

    public int getLength() {
        return this.m_firstFree;
    }

    public final int size() {
        return this.m_firstFree;
    }

    public final void addElement(String str) {
        if (this.m_firstFree + 1 >= this.m_mapSize) {
            this.m_mapSize += this.m_blocksize;
            String[] strArr = new String[this.m_mapSize];
            System.arraycopy(this.m_map, 0, strArr, 0, this.m_firstFree + 1);
            this.m_map = strArr;
        }
        this.m_map[this.m_firstFree] = str;
        this.m_firstFree++;
    }

    public final String elementAt(int i) {
        return this.m_map[i];
    }

    public final boolean contains(String str) {
        if (str == null) {
            return false;
        }
        for (int i = 0; i < this.m_firstFree; i++) {
            if (this.m_map[i].equals(str)) {
                return true;
            }
        }
        return false;
    }

    public final boolean containsIgnoreCase(String str) {
        if (str == null) {
            return false;
        }
        for (int i = 0; i < this.m_firstFree; i++) {
            if (this.m_map[i].equalsIgnoreCase(str)) {
                return true;
            }
        }
        return false;
    }

    public final void push(String str) {
        if (this.m_firstFree + 1 >= this.m_mapSize) {
            this.m_mapSize += this.m_blocksize;
            String[] strArr = new String[this.m_mapSize];
            System.arraycopy(this.m_map, 0, strArr, 0, this.m_firstFree + 1);
            this.m_map = strArr;
        }
        this.m_map[this.m_firstFree] = str;
        this.m_firstFree++;
    }

    public final String pop() {
        if (this.m_firstFree <= 0) {
            return null;
        }
        this.m_firstFree--;
        String str = this.m_map[this.m_firstFree];
        this.m_map[this.m_firstFree] = null;
        return str;
    }

    public final String peek() {
        if (this.m_firstFree <= 0) {
            return null;
        }
        return this.m_map[this.m_firstFree - 1];
    }
}
