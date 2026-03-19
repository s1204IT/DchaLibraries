package android.icu.util;

import android.icu.impl.Utility;

@Deprecated
public final class CompactCharArray implements Cloneable {
    static final int BLOCKCOUNT = 32;
    static final int BLOCKMASK = 31;

    @Deprecated
    public static final int BLOCKSHIFT = 5;
    static final int INDEXCOUNT = 2048;
    static final int INDEXSHIFT = 11;

    @Deprecated
    public static final int UNICODECOUNT = 65536;
    char defaultValue;
    private int[] hashes;
    private char[] indices;
    private boolean isCompact;
    private char[] values;

    @Deprecated
    public CompactCharArray() {
        this((char) 0);
    }

    @Deprecated
    public CompactCharArray(char c) {
        this.values = new char[65536];
        this.indices = new char[2048];
        this.hashes = new int[2048];
        for (int i = 0; i < 65536; i++) {
            this.values[i] = c;
        }
        for (int i2 = 0; i2 < 2048; i2++) {
            this.indices[i2] = (char) (i2 << 5);
            this.hashes[i2] = 0;
        }
        this.isCompact = false;
        this.defaultValue = c;
    }

    @Deprecated
    public CompactCharArray(char[] cArr, char[] cArr2) {
        if (cArr.length != 2048) {
            throw new IllegalArgumentException("Index out of bounds.");
        }
        for (int i = 0; i < 2048; i++) {
            if (cArr[i] >= cArr2.length + 32) {
                throw new IllegalArgumentException("Index out of bounds.");
            }
        }
        this.indices = cArr;
        this.values = cArr2;
        this.isCompact = true;
    }

    @Deprecated
    public CompactCharArray(String str, String str2) {
        this(Utility.RLEStringToCharArray(str), Utility.RLEStringToCharArray(str2));
    }

    @Deprecated
    public char elementAt(char c) {
        int i = (this.indices[c >> 5] & 65535) + (c & 31);
        return i >= this.values.length ? this.defaultValue : this.values[i];
    }

    @Deprecated
    public void setElementAt(char c, char c2) {
        if (this.isCompact) {
            expand();
        }
        this.values[c] = c2;
        touchBlock(c >> 5, c2);
    }

    @Deprecated
    public void setElementAt(char c, char c2, char c3) {
        if (this.isCompact) {
            expand();
        }
        while (c <= c2) {
            this.values[c] = c3;
            touchBlock(c >> 5, c3);
            c++;
        }
    }

    @Deprecated
    public void compact() {
        compact(true);
    }

    @Deprecated
    public void compact(boolean z) {
        int iFindOverlappingPosition;
        if (!this.isCompact) {
            char[] cArr = z ? new char[65536] : this.values;
            char c = 65535;
            int i = 0;
            int i2 = 0;
            int i3 = 0;
            while (i < this.indices.length) {
                this.indices[i] = 65535;
                boolean zBlockTouched = blockTouched(i);
                if (!zBlockTouched && c != 65535) {
                    this.indices[i] = c;
                } else {
                    int i4 = 0;
                    int i5 = 0;
                    while (i4 < i) {
                        if (this.hashes[i] == this.hashes[i4] && arrayRegionMatches(this.values, i3, this.values, i5, 32)) {
                            this.indices[i] = this.indices[i4];
                        }
                        i4++;
                        i5 += 32;
                    }
                    if (this.indices[i] == 65535) {
                        if (z) {
                            iFindOverlappingPosition = FindOverlappingPosition(i3, cArr, i2);
                        } else {
                            iFindOverlappingPosition = i2;
                        }
                        int i6 = iFindOverlappingPosition + 32;
                        if (i6 > i2) {
                            while (i2 < i6) {
                                cArr[i2] = this.values[(i3 + i2) - iFindOverlappingPosition];
                                i2++;
                            }
                            i2 = i6;
                        }
                        this.indices[i] = (char) iFindOverlappingPosition;
                        if (!zBlockTouched) {
                            c = (char) i5;
                        }
                    }
                }
                i++;
                i3 += 32;
            }
            char[] cArr2 = new char[i2];
            System.arraycopy(cArr, 0, cArr2, 0, i2);
            this.values = cArr2;
            this.isCompact = true;
            this.hashes = null;
        }
    }

    private int FindOverlappingPosition(int i, char[] cArr, int i2) {
        int i3;
        for (int i4 = 0; i4 < i2; i4++) {
            if (i4 + 32 > i2) {
                i3 = i2 - i4;
            } else {
                i3 = 32;
            }
            if (arrayRegionMatches(this.values, i, cArr, i4, i3)) {
                return i4;
            }
        }
        return i2;
    }

    static final boolean arrayRegionMatches(char[] cArr, int i, char[] cArr2, int i2, int i3) {
        int i4 = i3 + i;
        int i5 = i2 - i;
        while (i < i4) {
            if (cArr[i] == cArr2[i + i5]) {
                i++;
            } else {
                return false;
            }
        }
        return true;
    }

    private final void touchBlock(int i, int i2) {
        this.hashes[i] = (this.hashes[i] + (i2 << 1)) | 1;
    }

    private final boolean blockTouched(int i) {
        return this.hashes[i] != 0;
    }

    @Deprecated
    public char[] getIndexArray() {
        return this.indices;
    }

    @Deprecated
    public char[] getValueArray() {
        return this.values;
    }

    @Deprecated
    public Object clone() {
        try {
            CompactCharArray compactCharArray = (CompactCharArray) super.clone();
            compactCharArray.values = (char[]) this.values.clone();
            compactCharArray.indices = (char[]) this.indices.clone();
            if (this.hashes != null) {
                compactCharArray.hashes = (int[]) this.hashes.clone();
            }
            return compactCharArray;
        } catch (CloneNotSupportedException e) {
            throw new ICUCloneNotSupportedException(e);
        }
    }

    @Deprecated
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        CompactCharArray compactCharArray = (CompactCharArray) obj;
        for (int i = 0; i < 65536; i++) {
            char c = (char) i;
            if (elementAt(c) != compactCharArray.elementAt(c)) {
                return false;
            }
        }
        return true;
    }

    @Deprecated
    public int hashCode() {
        int iMin = Math.min(3, this.values.length / 16);
        int i = 0;
        for (int i2 = 0; i2 < this.values.length; i2 += iMin) {
            i = (i * 37) + this.values[i2];
        }
        return i;
    }

    private void expand() {
        if (this.isCompact) {
            this.hashes = new int[2048];
            char[] cArr = new char[65536];
            for (int i = 0; i < 65536; i++) {
                cArr[i] = elementAt((char) i);
            }
            for (int i2 = 0; i2 < 2048; i2++) {
                this.indices[i2] = (char) (i2 << 5);
            }
            this.values = null;
            this.values = cArr;
            this.isCompact = false;
        }
    }
}
