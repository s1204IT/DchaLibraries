package org.apache.http.util;

import org.apache.http.protocol.HTTP;

@Deprecated
public final class CharArrayBuffer {
    private char[] buffer;
    private int len;

    public CharArrayBuffer(int i) {
        if (i < 0) {
            throw new IllegalArgumentException("Buffer capacity may not be negative");
        }
        this.buffer = new char[i];
    }

    private void expand(int i) {
        char[] cArr = new char[Math.max(this.buffer.length << 1, i)];
        System.arraycopy(this.buffer, 0, cArr, 0, this.len);
        this.buffer = cArr;
    }

    public void append(char[] cArr, int i, int i2) {
        int i3;
        if (cArr == null) {
            return;
        }
        if (i < 0 || i > cArr.length || i2 < 0 || (i3 = i + i2) < 0 || i3 > cArr.length) {
            throw new IndexOutOfBoundsException();
        }
        if (i2 == 0) {
            return;
        }
        int i4 = this.len + i2;
        if (i4 > this.buffer.length) {
            expand(i4);
        }
        System.arraycopy(cArr, i, this.buffer, this.len, i2);
        this.len = i4;
    }

    public void append(String str) {
        if (str == null) {
            str = "null";
        }
        int length = str.length();
        int i = this.len + length;
        if (i > this.buffer.length) {
            expand(i);
        }
        str.getChars(0, length, this.buffer, this.len);
        this.len = i;
    }

    public void append(CharArrayBuffer charArrayBuffer, int i, int i2) {
        if (charArrayBuffer == null) {
            return;
        }
        append(charArrayBuffer.buffer, i, i2);
    }

    public void append(CharArrayBuffer charArrayBuffer) {
        if (charArrayBuffer == null) {
            return;
        }
        append(charArrayBuffer.buffer, 0, charArrayBuffer.len);
    }

    public void append(char c) {
        int i = this.len + 1;
        if (i > this.buffer.length) {
            expand(i);
        }
        this.buffer[this.len] = c;
        this.len = i;
    }

    public void append(byte[] bArr, int i, int i2) {
        int i3;
        if (bArr == 0) {
            return;
        }
        if (i < 0 || i > bArr.length || i2 < 0 || (i3 = i + i2) < 0 || i3 > bArr.length) {
            throw new IndexOutOfBoundsException();
        }
        if (i2 == 0) {
            return;
        }
        int i4 = this.len;
        int i5 = i2 + i4;
        if (i5 > this.buffer.length) {
            expand(i5);
        }
        while (i4 < i5) {
            int i6 = bArr[i];
            if (i6 < 0) {
                i6 += 256;
            }
            this.buffer[i4] = (char) i6;
            i++;
            i4++;
        }
        this.len = i5;
    }

    public void append(ByteArrayBuffer byteArrayBuffer, int i, int i2) {
        if (byteArrayBuffer == null) {
            return;
        }
        append(byteArrayBuffer.buffer(), i, i2);
    }

    public void append(Object obj) {
        append(String.valueOf(obj));
    }

    public void clear() {
        this.len = 0;
    }

    public char[] toCharArray() {
        char[] cArr = new char[this.len];
        if (this.len > 0) {
            System.arraycopy(this.buffer, 0, cArr, 0, this.len);
        }
        return cArr;
    }

    public char charAt(int i) {
        return this.buffer[i];
    }

    public char[] buffer() {
        return this.buffer;
    }

    public int capacity() {
        return this.buffer.length;
    }

    public int length() {
        return this.len;
    }

    public void ensureCapacity(int i) {
        if (i > this.buffer.length - this.len) {
            expand(this.len + i);
        }
    }

    public void setLength(int i) {
        if (i < 0 || i > this.buffer.length) {
            throw new IndexOutOfBoundsException();
        }
        this.len = i;
    }

    public boolean isEmpty() {
        return this.len == 0;
    }

    public boolean isFull() {
        return this.len == this.buffer.length;
    }

    public int indexOf(int i, int i2, int i3) {
        if (i2 < 0) {
            i2 = 0;
        }
        if (i3 > this.len) {
            i3 = this.len;
        }
        if (i2 > i3) {
            return -1;
        }
        while (i2 < i3) {
            if (this.buffer[i2] != i) {
                i2++;
            } else {
                return i2;
            }
        }
        return -1;
    }

    public int indexOf(int i) {
        return indexOf(i, 0, this.len);
    }

    public String substring(int i, int i2) {
        if (i < 0) {
            throw new IndexOutOfBoundsException();
        }
        if (i2 > this.len) {
            throw new IndexOutOfBoundsException();
        }
        if (i > i2) {
            throw new IndexOutOfBoundsException();
        }
        return new String(this.buffer, i, i2 - i);
    }

    public String substringTrimmed(int i, int i2) {
        if (i < 0) {
            throw new IndexOutOfBoundsException();
        }
        if (i2 > this.len) {
            throw new IndexOutOfBoundsException();
        }
        if (i > i2) {
            throw new IndexOutOfBoundsException();
        }
        while (i < i2 && HTTP.isWhitespace(this.buffer[i])) {
            i++;
        }
        while (i2 > i && HTTP.isWhitespace(this.buffer[i2 - 1])) {
            i2--;
        }
        return new String(this.buffer, i, i2 - i);
    }

    public String toString() {
        return new String(this.buffer, 0, this.len);
    }
}
