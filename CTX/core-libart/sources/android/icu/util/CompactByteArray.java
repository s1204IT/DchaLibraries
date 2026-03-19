package android.icu.util;

import android.icu.impl.Utility;

@Deprecated
public final class CompactByteArray implements Cloneable {
    private static final int BLOCKCOUNT = 128;
    private static final int BLOCKMASK = 127;
    private static final int BLOCKSHIFT = 7;
    private static final int INDEXCOUNT = 512;
    private static final int INDEXSHIFT = 9;

    @Deprecated
    public static final int UNICODECOUNT = 65536;
    byte defaultValue;
    private int[] hashes;
    private char[] indices;
    private boolean isCompact;
    private byte[] values;

    @Deprecated
    public CompactByteArray() {
        this((byte) 0);
    }

    @Deprecated
    public CompactByteArray(byte b) {
        this.values = new byte[65536];
        this.indices = new char[512];
        this.hashes = new int[512];
        for (int i = 0; i < 65536; i++) {
            this.values[i] = b;
        }
        for (int i2 = 0; i2 < 512; i2++) {
            this.indices[i2] = (char) (i2 << 7);
            this.hashes[i2] = 0;
        }
        this.isCompact = false;
        this.defaultValue = b;
    }

    @Deprecated
    public CompactByteArray(char[] cArr, byte[] bArr) {
        if (cArr.length != 512) {
            throw new IllegalArgumentException("Index out of bounds.");
        }
        for (int i = 0; i < 512; i++) {
            if (cArr[i] >= bArr.length + 128) {
                throw new IllegalArgumentException("Index out of bounds.");
            }
        }
        this.indices = cArr;
        this.values = bArr;
        this.isCompact = true;
    }

    @Deprecated
    public CompactByteArray(String str, String str2) {
        this(Utility.RLEStringToCharArray(str), Utility.RLEStringToByteArray(str2));
    }

    @Deprecated
    public byte elementAt(char c) {
        return this.values[(this.indices[c >> 7] & 65535) + (c & 127)];
    }

    @Deprecated
    public void setElementAt(char c, byte b) {
        if (this.isCompact) {
            expand();
        }
        this.values[c] = b;
        touchBlock(c >> 7, b);
    }

    @Deprecated
    public void setElementAt(char c, char c2, byte b) {
        if (this.isCompact) {
            expand();
        }
        while (c <= c2) {
            this.values[c] = b;
            touchBlock(c >> 7, b);
            c++;
        }
    }

    @Deprecated
    public void compact() {
        compact(false);
    }

    @Deprecated
    public void compact(boolean z) {
        if (!this.isCompact) {
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
                    while (true) {
                        if (i4 < i2) {
                            if (this.hashes[i] != this.hashes[i4] || !arrayRegionMatches(this.values, i3, this.values, i5, 128)) {
                                i4++;
                                i5 += 128;
                            } else {
                                this.indices[i] = (char) i5;
                                break;
                            }
                        } else {
                            break;
                        }
                    }
                    if (this.indices[i] == 65535) {
                        System.arraycopy(this.values, i3, this.values, i5, 128);
                        char c2 = (char) i5;
                        this.indices[i] = c2;
                        this.hashes[i4] = this.hashes[i];
                        i2++;
                        if (!zBlockTouched) {
                            c = c2;
                        }
                    }
                }
                i++;
                i3 += 128;
            }
            int i6 = i2 * 128;
            byte[] bArr = new byte[i6];
            System.arraycopy(this.values, 0, bArr, 0, i6);
            this.values = bArr;
            this.isCompact = true;
            this.hashes = null;
        }
    }

    static final boolean arrayRegionMatches(byte[] bArr, int i, byte[] bArr2, int i2, int i3) {
        int i4 = i3 + i;
        int i5 = i2 - i;
        while (i < i4) {
            if (bArr[i] == bArr2[i + i5]) {
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
    public byte[] getValueArray() {
        return this.values;
    }

    @Deprecated
    public Object clone() {
        try {
            CompactByteArray compactByteArray = (CompactByteArray) super.clone();
            compactByteArray.values = (byte[]) this.values.clone();
            compactByteArray.indices = (char[]) this.indices.clone();
            if (this.hashes != null) {
                compactByteArray.hashes = (int[]) this.hashes.clone();
            }
            return compactByteArray;
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
        CompactByteArray compactByteArray = (CompactByteArray) obj;
        for (int i = 0; i < 65536; i++) {
            char c = (char) i;
            if (elementAt(c) != compactByteArray.elementAt(c)) {
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
            this.hashes = new int[512];
            byte[] bArr = new byte[65536];
            for (int i = 0; i < 65536; i++) {
                byte bElementAt = elementAt((char) i);
                bArr[i] = bElementAt;
                touchBlock(i >> 7, bElementAt);
            }
            for (int i2 = 0; i2 < 512; i2++) {
                this.indices[i2] = (char) (i2 << 7);
            }
            this.values = null;
            this.values = bArr;
            this.isCompact = false;
        }
    }
}
