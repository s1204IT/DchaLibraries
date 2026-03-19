package org.apache.xml.utils;

import java.util.EmptyStackException;

public class ObjectStack extends ObjectVector {
    public ObjectStack() {
    }

    public ObjectStack(int i) {
        super(i);
    }

    public ObjectStack(ObjectStack objectStack) {
        super(objectStack);
    }

    public Object push(Object obj) {
        if (this.m_firstFree + 1 >= this.m_mapSize) {
            this.m_mapSize += this.m_blocksize;
            Object[] objArr = new Object[this.m_mapSize];
            System.arraycopy(this.m_map, 0, objArr, 0, this.m_firstFree + 1);
            this.m_map = objArr;
        }
        this.m_map[this.m_firstFree] = obj;
        this.m_firstFree++;
        return obj;
    }

    public Object pop() {
        Object[] objArr = this.m_map;
        int i = this.m_firstFree - 1;
        this.m_firstFree = i;
        Object obj = objArr[i];
        this.m_map[this.m_firstFree] = null;
        return obj;
    }

    public void quickPop(int i) {
        this.m_firstFree -= i;
    }

    public Object peek() {
        try {
            return this.m_map[this.m_firstFree - 1];
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new EmptyStackException();
        }
    }

    public Object peek(int i) {
        try {
            return this.m_map[this.m_firstFree - (1 + i)];
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new EmptyStackException();
        }
    }

    public void setTop(Object obj) {
        try {
            this.m_map[this.m_firstFree - 1] = obj;
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new EmptyStackException();
        }
    }

    public boolean empty() {
        return this.m_firstFree == 0;
    }

    public int search(Object obj) {
        int iLastIndexOf = lastIndexOf(obj);
        if (iLastIndexOf >= 0) {
            return size() - iLastIndexOf;
        }
        return -1;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        return (ObjectStack) super.clone();
    }
}
