package org.apache.xml.utils;

import org.apache.xml.dtm.DTMFilter;

public class SuballocatedIntVector {
    protected static final int NUMBLOCKS_DEFAULT = 32;
    protected int m_MASK;
    protected int m_SHIFT;
    protected int m_blocksize;
    protected int[] m_buildCache;
    protected int m_buildCacheStartIndex;
    protected int m_firstFree;
    protected int[][] m_map;
    protected int[] m_map0;
    protected int m_numblocks;

    public SuballocatedIntVector() {
        this(DTMFilter.SHOW_NOTATION);
    }

    public SuballocatedIntVector(int i, int i2) {
        this.m_numblocks = 32;
        this.m_firstFree = 0;
        this.m_SHIFT = 0;
        while (true) {
            i >>>= 1;
            if (i == 0) {
                this.m_blocksize = 1 << this.m_SHIFT;
                this.m_MASK = this.m_blocksize - 1;
                this.m_numblocks = i2;
                this.m_map0 = new int[this.m_blocksize];
                this.m_map = new int[i2][];
                this.m_map[0] = this.m_map0;
                this.m_buildCache = this.m_map0;
                this.m_buildCacheStartIndex = 0;
                return;
            }
            this.m_SHIFT++;
        }
    }

    public SuballocatedIntVector(int i) {
        this(i, 32);
    }

    public int size() {
        return this.m_firstFree;
    }

    public void setSize(int i) {
        if (this.m_firstFree > i) {
            this.m_firstFree = i;
        }
    }

    public void addElement(int i) {
        int i2 = this.m_firstFree - this.m_buildCacheStartIndex;
        if (i2 >= 0 && i2 < this.m_blocksize) {
            this.m_buildCache[i2] = i;
            this.m_firstFree++;
            return;
        }
        int i3 = this.m_firstFree >>> this.m_SHIFT;
        int i4 = this.m_firstFree & this.m_MASK;
        if (i3 >= this.m_map.length) {
            int[][] iArr = new int[this.m_numblocks + i3][];
            System.arraycopy(this.m_map, 0, iArr, 0, this.m_map.length);
            this.m_map = iArr;
        }
        int[] iArr2 = this.m_map[i3];
        if (iArr2 == null) {
            int[][] iArr3 = this.m_map;
            int[] iArr4 = new int[this.m_blocksize];
            iArr3[i3] = iArr4;
            iArr2 = iArr4;
        }
        iArr2[i4] = i;
        this.m_buildCache = iArr2;
        this.m_buildCacheStartIndex = this.m_firstFree - i4;
        this.m_firstFree++;
    }

    private void addElements(int i, int i2) {
        int i3;
        if (this.m_firstFree + i2 < this.m_blocksize) {
            for (int i4 = 0; i4 < i2; i4++) {
                int[] iArr = this.m_map0;
                int i5 = this.m_firstFree;
                this.m_firstFree = i5 + 1;
                iArr[i5] = i;
            }
            return;
        }
        int i6 = this.m_firstFree >>> this.m_SHIFT;
        int i7 = this.m_firstFree & this.m_MASK;
        this.m_firstFree += i2;
        while (i2 > 0) {
            if (i6 >= this.m_map.length) {
                int[][] iArr2 = new int[this.m_numblocks + i6][];
                System.arraycopy(this.m_map, 0, iArr2, 0, this.m_map.length);
                this.m_map = iArr2;
            }
            int[] iArr3 = this.m_map[i6];
            if (iArr3 == null) {
                int[][] iArr4 = this.m_map;
                int[] iArr5 = new int[this.m_blocksize];
                iArr4[i6] = iArr5;
                iArr3 = iArr5;
            }
            if (this.m_blocksize - i7 < i2) {
                i3 = this.m_blocksize - i7;
            } else {
                i3 = i2;
            }
            i2 -= i3;
            while (true) {
                int i8 = i3 - 1;
                if (i3 > 0) {
                    iArr3[i7] = i;
                    i7++;
                    i3 = i8;
                }
            }
            i6++;
            i7 = 0;
        }
    }

    private void addElements(int i) {
        int i2 = this.m_firstFree + i;
        if (i2 > this.m_blocksize) {
            int i3 = this.m_firstFree >>> this.m_SHIFT;
            int i4 = (this.m_firstFree + i) >>> this.m_SHIFT;
            while (true) {
                i3++;
                if (i3 > i4) {
                    break;
                } else {
                    this.m_map[i3] = new int[this.m_blocksize];
                }
            }
        }
        this.m_firstFree = i2;
    }

    private void insertElementAt(int i, int i2) {
        int i3;
        if (i2 == this.m_firstFree) {
            addElement(i);
            return;
        }
        if (i2 > this.m_firstFree) {
            int i4 = i2 >>> this.m_SHIFT;
            if (i4 >= this.m_map.length) {
                int[][] iArr = new int[this.m_numblocks + i4][];
                System.arraycopy(this.m_map, 0, iArr, 0, this.m_map.length);
                this.m_map = iArr;
            }
            int[] iArr2 = this.m_map[i4];
            if (iArr2 == null) {
                int[][] iArr3 = this.m_map;
                int[] iArr4 = new int[this.m_blocksize];
                iArr3[i4] = iArr4;
                iArr2 = iArr4;
            }
            int i5 = i2 & this.m_MASK;
            iArr2[i5] = i;
            this.m_firstFree = i5 + 1;
            return;
        }
        int i6 = i2 >>> this.m_SHIFT;
        int i7 = this.m_firstFree >>> this.m_SHIFT;
        this.m_firstFree++;
        int i8 = i2 & this.m_MASK;
        while (i6 <= i7) {
            int i9 = (this.m_blocksize - i8) - 1;
            int[] iArr5 = this.m_map[i6];
            if (iArr5 == null) {
                int[][] iArr6 = this.m_map;
                iArr5 = new int[this.m_blocksize];
                iArr6[i6] = iArr5;
                i3 = 0;
            } else {
                i3 = iArr5[this.m_blocksize - 1];
                System.arraycopy(iArr5, i8, iArr5, i8 + 1, i9);
            }
            iArr5[i8] = i;
            i6++;
            i8 = 0;
            i = i3;
        }
    }

    public void removeAllElements() {
        this.m_firstFree = 0;
        this.m_buildCache = this.m_map0;
        this.m_buildCacheStartIndex = 0;
    }

    private boolean removeElement(int i) {
        int iIndexOf = indexOf(i, 0);
        if (iIndexOf < 0) {
            return false;
        }
        removeElementAt(iIndexOf);
        return true;
    }

    private void removeElementAt(int i) {
        if (i < this.m_firstFree) {
            int i2 = i >>> this.m_SHIFT;
            int i3 = this.m_firstFree >>> this.m_SHIFT;
            int i4 = i & this.m_MASK;
            while (i2 <= i3) {
                int i5 = (this.m_blocksize - i4) - 1;
                int[] iArr = this.m_map[i2];
                if (iArr == null) {
                    int[][] iArr2 = this.m_map;
                    iArr = new int[this.m_blocksize];
                    iArr2[i2] = iArr;
                } else {
                    System.arraycopy(iArr, i4 + 1, iArr, i4, i5);
                }
                if (i2 >= i3) {
                    iArr[this.m_blocksize - 1] = 0;
                } else {
                    int[] iArr3 = this.m_map[i2 + 1];
                    if (iArr3 != null) {
                        iArr[this.m_blocksize - 1] = iArr3 != null ? iArr3[0] : 0;
                    }
                }
                i2++;
                i4 = 0;
            }
        }
        this.m_firstFree--;
    }

    public void setElementAt(int i, int i2) {
        if (i2 < this.m_blocksize) {
            this.m_map0[i2] = i;
        } else {
            int i3 = i2 >>> this.m_SHIFT;
            int i4 = this.m_MASK & i2;
            if (i3 >= this.m_map.length) {
                int[][] iArr = new int[this.m_numblocks + i3][];
                System.arraycopy(this.m_map, 0, iArr, 0, this.m_map.length);
                this.m_map = iArr;
            }
            int[] iArr2 = this.m_map[i3];
            if (iArr2 == null) {
                int[][] iArr3 = this.m_map;
                int[] iArr4 = new int[this.m_blocksize];
                iArr3[i3] = iArr4;
                iArr2 = iArr4;
            }
            iArr2[i4] = i;
        }
        if (i2 >= this.m_firstFree) {
            this.m_firstFree = i2 + 1;
        }
    }

    public int elementAt(int i) {
        if (i < this.m_blocksize) {
            return this.m_map0[i];
        }
        return this.m_map[i >>> this.m_SHIFT][i & this.m_MASK];
    }

    private boolean contains(int i) {
        return indexOf(i, 0) >= 0;
    }

    public int indexOf(int i, int i2) {
        if (i2 >= this.m_firstFree) {
            return -1;
        }
        int i3 = i2 & this.m_MASK;
        int i4 = this.m_firstFree >>> this.m_SHIFT;
        for (int i5 = i2 >>> this.m_SHIFT; i5 < i4; i5++) {
            int[] iArr = this.m_map[i5];
            if (iArr != null) {
                while (i3 < this.m_blocksize) {
                    if (iArr[i3] != i) {
                        i3++;
                    } else {
                        return i3 + (i5 * this.m_blocksize);
                    }
                }
            }
            i3 = 0;
        }
        int i6 = this.m_firstFree & this.m_MASK;
        int[] iArr2 = this.m_map[i4];
        while (i3 < i6) {
            if (iArr2[i3] != i) {
                i3++;
            } else {
                return i3 + (i4 * this.m_blocksize);
            }
        }
        return -1;
    }

    public int indexOf(int i) {
        return indexOf(i, 0);
    }

    private int lastIndexOf(int i) {
        int i2 = this.m_firstFree & this.m_MASK;
        for (int i3 = this.m_firstFree >>> this.m_SHIFT; i3 >= 0; i3--) {
            int[] iArr = this.m_map[i3];
            if (iArr != null) {
                while (i2 >= 0) {
                    if (iArr[i2] == i) {
                        return i2 + (i3 * this.m_blocksize);
                    }
                    i2--;
                }
            }
            i2 = 0;
        }
        return -1;
    }

    public final int[] getMap0() {
        return this.m_map0;
    }

    public final int[][] getMap() {
        return this.m_map;
    }
}
