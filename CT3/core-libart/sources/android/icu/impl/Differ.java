package android.icu.impl;

public final class Differ<T> {
    private int EQUALSIZE;
    private int STACKSIZE;
    private T[] a;
    private T[] b;
    private T last = null;
    private T next = null;
    private int aCount = 0;
    private int bCount = 0;
    private int aLine = 1;
    private int bLine = 1;
    private int maxSame = 0;
    private int aTop = 0;
    private int bTop = 0;

    public Differ(int i, int i2) {
        this.STACKSIZE = i;
        this.EQUALSIZE = i2;
        this.a = (T[]) new Object[i + i2];
        this.b = (T[]) new Object[i + i2];
    }

    public void add(T aStr, T bStr) {
        addA(aStr);
        addB(bStr);
    }

    public void addA(T aStr) {
        flush();
        T[] tArr = this.a;
        int i = this.aCount;
        this.aCount = i + 1;
        tArr[i] = aStr;
    }

    public void addB(T bStr) {
        flush();
        T[] tArr = this.b;
        int i = this.bCount;
        this.bCount = i + 1;
        tArr[i] = bStr;
    }

    public int getALine(int offset) {
        return this.aLine + this.maxSame + offset;
    }

    public T getA(int offset) {
        return offset < 0 ? this.last : offset > this.aTop - this.maxSame ? this.next : this.a[offset];
    }

    public int getACount() {
        return this.aTop - this.maxSame;
    }

    public int getBCount() {
        return this.bTop - this.maxSame;
    }

    public int getBLine(int offset) {
        return this.bLine + this.maxSame + offset;
    }

    public T getB(int offset) {
        return offset < 0 ? this.last : offset > this.bTop - this.maxSame ? this.next : this.b[offset];
    }

    public void checkMatch(boolean finalPass) {
        int max = this.aCount;
        if (max > this.bCount) {
            max = this.bCount;
        }
        int i = 0;
        while (i < max && this.a[i].equals(this.b[i])) {
            i++;
        }
        this.maxSame = i;
        int i2 = this.maxSame;
        this.bTop = i2;
        this.aTop = i2;
        if (this.maxSame > 0) {
            this.last = this.a[this.maxSame - 1];
        }
        this.next = null;
        if (finalPass) {
            this.aTop = this.aCount;
            this.bTop = this.bCount;
            this.next = null;
            return;
        }
        if (this.aCount - this.maxSame < this.EQUALSIZE || this.bCount - this.maxSame < this.EQUALSIZE) {
            return;
        }
        int match = find(this.a, this.aCount - this.EQUALSIZE, this.aCount, this.b, this.maxSame, this.bCount);
        if (match != -1) {
            this.aTop = this.aCount - this.EQUALSIZE;
            this.bTop = match;
            this.next = this.a[this.aTop];
            return;
        }
        int match2 = find(this.b, this.bCount - this.EQUALSIZE, this.bCount, this.a, this.maxSame, this.aCount);
        if (match2 != -1) {
            this.bTop = this.bCount - this.EQUALSIZE;
            this.aTop = match2;
            this.next = this.b[this.bTop];
        } else {
            if (this.aCount < this.STACKSIZE && this.bCount < this.STACKSIZE) {
                return;
            }
            this.aCount = (this.aCount + this.maxSame) / 2;
            this.bCount = (this.bCount + this.maxSame) / 2;
            this.next = null;
        }
    }

    public int find(T[] aArr, int aStart, int aEnd, T[] bArr, int bStart, int bEnd) {
        int len = aEnd - aStart;
        int bEndMinus = bEnd - len;
        int i = bStart;
        while (i <= bEndMinus) {
            for (int j = 0; j < len; j++) {
                if (!bArr[i + j].equals(aArr[aStart + j])) {
                    break;
                }
            }
            return i;
        }
        return -1;
    }

    private void flush() {
        if (this.aTop != 0) {
            int newCount = this.aCount - this.aTop;
            System.arraycopy(this.a, this.aTop, this.a, 0, newCount);
            this.aCount = newCount;
            this.aLine += this.aTop;
            this.aTop = 0;
        }
        if (this.bTop == 0) {
            return;
        }
        int newCount2 = this.bCount - this.bTop;
        System.arraycopy(this.b, this.bTop, this.b, 0, newCount2);
        this.bCount = newCount2;
        this.bLine += this.bTop;
        this.bTop = 0;
    }
}
