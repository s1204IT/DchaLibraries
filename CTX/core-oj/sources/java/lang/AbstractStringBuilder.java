package java.lang;

import java.util.Arrays;
import sun.misc.FloatingDecimal;

abstract class AbstractStringBuilder implements Appendable, CharSequence {
    private static final int MAX_ARRAY_SIZE = 2147483639;
    int count;
    char[] value;

    @Override
    public abstract String toString();

    AbstractStringBuilder() {
    }

    AbstractStringBuilder(int i) {
        this.value = new char[i];
    }

    @Override
    public int length() {
        return this.count;
    }

    public int capacity() {
        return this.value.length;
    }

    public void ensureCapacity(int i) {
        if (i > 0) {
            ensureCapacityInternal(i);
        }
    }

    private void ensureCapacityInternal(int i) {
        if (i - this.value.length > 0) {
            this.value = Arrays.copyOf(this.value, newCapacity(i));
        }
    }

    private int newCapacity(int i) {
        int length = (this.value.length << 1) + 2;
        if (length - i < 0) {
            length = i;
        }
        if (length > 0 && MAX_ARRAY_SIZE - length >= 0) {
            return length;
        }
        return hugeCapacity(i);
    }

    private int hugeCapacity(int i) {
        if (Integer.MAX_VALUE - i >= 0) {
            return i > MAX_ARRAY_SIZE ? i : MAX_ARRAY_SIZE;
        }
        throw new OutOfMemoryError();
    }

    public void trimToSize() {
        if (this.count < this.value.length) {
            this.value = Arrays.copyOf(this.value, this.count);
        }
    }

    public void setLength(int i) {
        if (i < 0) {
            throw new StringIndexOutOfBoundsException(i);
        }
        ensureCapacityInternal(i);
        if (this.count < i) {
            Arrays.fill(this.value, this.count, i, (char) 0);
        }
        this.count = i;
    }

    @Override
    public char charAt(int i) {
        if (i < 0 || i >= this.count) {
            throw new StringIndexOutOfBoundsException(i);
        }
        return this.value[i];
    }

    public int codePointAt(int i) {
        if (i < 0 || i >= this.count) {
            throw new StringIndexOutOfBoundsException(i);
        }
        return Character.codePointAtImpl(this.value, i, this.count);
    }

    public int codePointBefore(int i) {
        int i2 = i - 1;
        if (i2 < 0 || i2 >= this.count) {
            throw new StringIndexOutOfBoundsException(i);
        }
        return Character.codePointBeforeImpl(this.value, i, 0);
    }

    public int codePointCount(int i, int i2) {
        if (i < 0 || i2 > this.count || i > i2) {
            throw new IndexOutOfBoundsException();
        }
        return Character.codePointCountImpl(this.value, i, i2 - i);
    }

    public int offsetByCodePoints(int i, int i2) {
        if (i < 0 || i > this.count) {
            throw new IndexOutOfBoundsException();
        }
        return Character.offsetByCodePointsImpl(this.value, 0, this.count, i, i2);
    }

    public void getChars(int i, int i2, char[] cArr, int i3) {
        if (i < 0) {
            throw new StringIndexOutOfBoundsException(i);
        }
        if (i2 < 0 || i2 > this.count) {
            throw new StringIndexOutOfBoundsException(i2);
        }
        if (i > i2) {
            throw new StringIndexOutOfBoundsException("srcBegin > srcEnd");
        }
        System.arraycopy((Object) this.value, i, (Object) cArr, i3, i2 - i);
    }

    public void setCharAt(int i, char c) {
        if (i < 0 || i >= this.count) {
            throw new StringIndexOutOfBoundsException(i);
        }
        this.value[i] = c;
    }

    public AbstractStringBuilder append(Object obj) {
        return append(String.valueOf(obj));
    }

    public AbstractStringBuilder append(String str) {
        if (str == null) {
            return appendNull();
        }
        int length = str.length();
        ensureCapacityInternal(this.count + length);
        str.getChars(0, length, this.value, this.count);
        this.count += length;
        return this;
    }

    public AbstractStringBuilder append(StringBuffer stringBuffer) {
        if (stringBuffer == null) {
            return appendNull();
        }
        int length = stringBuffer.length();
        ensureCapacityInternal(this.count + length);
        stringBuffer.getChars(0, length, this.value, this.count);
        this.count += length;
        return this;
    }

    AbstractStringBuilder append(AbstractStringBuilder abstractStringBuilder) {
        if (abstractStringBuilder == null) {
            return appendNull();
        }
        int length = abstractStringBuilder.length();
        ensureCapacityInternal(this.count + length);
        abstractStringBuilder.getChars(0, length, this.value, this.count);
        this.count += length;
        return this;
    }

    @Override
    public AbstractStringBuilder append(CharSequence charSequence) {
        if (charSequence == null) {
            return appendNull();
        }
        if (charSequence instanceof String) {
            return append((String) charSequence);
        }
        if (charSequence instanceof AbstractStringBuilder) {
            return append((AbstractStringBuilder) charSequence);
        }
        return append(charSequence, 0, charSequence.length());
    }

    private AbstractStringBuilder appendNull() {
        int i = this.count;
        ensureCapacityInternal(i + 4);
        char[] cArr = this.value;
        int i2 = i + 1;
        cArr[i] = 'n';
        int i3 = i2 + 1;
        cArr[i2] = 'u';
        int i4 = i3 + 1;
        cArr[i3] = 'l';
        cArr[i4] = 'l';
        this.count = i4 + 1;
        return this;
    }

    @Override
    public AbstractStringBuilder append(CharSequence charSequence, int i, int i2) {
        if (charSequence == null) {
            charSequence = "null";
        }
        if (i < 0 || i > i2 || i2 > charSequence.length()) {
            throw new IndexOutOfBoundsException("start " + i + ", end " + i2 + ", s.length() " + charSequence.length());
        }
        int i3 = i2 - i;
        ensureCapacityInternal(this.count + i3);
        int i4 = this.count;
        while (i < i2) {
            this.value[i4] = charSequence.charAt(i);
            i++;
            i4++;
        }
        this.count += i3;
        return this;
    }

    public AbstractStringBuilder append(char[] cArr) {
        int length = cArr.length;
        ensureCapacityInternal(this.count + length);
        System.arraycopy((Object) cArr, 0, (Object) this.value, this.count, length);
        this.count += length;
        return this;
    }

    public AbstractStringBuilder append(char[] cArr, int i, int i2) {
        if (i2 > 0) {
            ensureCapacityInternal(this.count + i2);
        }
        System.arraycopy((Object) cArr, i, (Object) this.value, this.count, i2);
        this.count += i2;
        return this;
    }

    public AbstractStringBuilder append(boolean z) {
        if (z) {
            ensureCapacityInternal(this.count + 4);
            char[] cArr = this.value;
            int i = this.count;
            this.count = i + 1;
            cArr[i] = 't';
            char[] cArr2 = this.value;
            int i2 = this.count;
            this.count = i2 + 1;
            cArr2[i2] = 'r';
            char[] cArr3 = this.value;
            int i3 = this.count;
            this.count = i3 + 1;
            cArr3[i3] = 'u';
            char[] cArr4 = this.value;
            int i4 = this.count;
            this.count = i4 + 1;
            cArr4[i4] = 'e';
        } else {
            ensureCapacityInternal(this.count + 5);
            char[] cArr5 = this.value;
            int i5 = this.count;
            this.count = i5 + 1;
            cArr5[i5] = 'f';
            char[] cArr6 = this.value;
            int i6 = this.count;
            this.count = i6 + 1;
            cArr6[i6] = 'a';
            char[] cArr7 = this.value;
            int i7 = this.count;
            this.count = i7 + 1;
            cArr7[i7] = 'l';
            char[] cArr8 = this.value;
            int i8 = this.count;
            this.count = i8 + 1;
            cArr8[i8] = 's';
            char[] cArr9 = this.value;
            int i9 = this.count;
            this.count = i9 + 1;
            cArr9[i9] = 'e';
        }
        return this;
    }

    @Override
    public AbstractStringBuilder append(char c) {
        ensureCapacityInternal(this.count + 1);
        char[] cArr = this.value;
        int i = this.count;
        this.count = i + 1;
        cArr[i] = c;
        return this;
    }

    public AbstractStringBuilder append(int i) {
        if (i == Integer.MIN_VALUE) {
            append("-2147483648");
            return this;
        }
        int iStringSize = this.count + (i < 0 ? Integer.stringSize(-i) + 1 : Integer.stringSize(i));
        ensureCapacityInternal(iStringSize);
        Integer.getChars(i, iStringSize, this.value);
        this.count = iStringSize;
        return this;
    }

    public AbstractStringBuilder append(long j) {
        if (j == Long.MIN_VALUE) {
            append("-9223372036854775808");
            return this;
        }
        int iStringSize = this.count + (j < 0 ? Long.stringSize(-j) + 1 : Long.stringSize(j));
        ensureCapacityInternal(iStringSize);
        Long.getChars(j, iStringSize, this.value);
        this.count = iStringSize;
        return this;
    }

    public AbstractStringBuilder append(float f) {
        FloatingDecimal.appendTo(f, (Appendable) this);
        return this;
    }

    public AbstractStringBuilder append(double d) {
        FloatingDecimal.appendTo(d, this);
        return this;
    }

    public AbstractStringBuilder delete(int i, int i2) {
        if (i < 0) {
            throw new StringIndexOutOfBoundsException(i);
        }
        if (i2 > this.count) {
            i2 = this.count;
        }
        if (i > i2) {
            throw new StringIndexOutOfBoundsException();
        }
        int i3 = i2 - i;
        if (i3 > 0) {
            System.arraycopy((Object) this.value, i + i3, (Object) this.value, i, this.count - i2);
            this.count -= i3;
        }
        return this;
    }

    public AbstractStringBuilder appendCodePoint(int i) {
        int i2 = this.count;
        if (Character.isBmpCodePoint(i)) {
            int i3 = i2 + 1;
            ensureCapacityInternal(i3);
            this.value[i2] = (char) i;
            this.count = i3;
        } else if (Character.isValidCodePoint(i)) {
            int i4 = i2 + 2;
            ensureCapacityInternal(i4);
            Character.toSurrogates(i, this.value, i2);
            this.count = i4;
        } else {
            throw new IllegalArgumentException();
        }
        return this;
    }

    public AbstractStringBuilder deleteCharAt(int i) {
        if (i < 0 || i >= this.count) {
            throw new StringIndexOutOfBoundsException(i);
        }
        System.arraycopy((Object) this.value, i + 1, (Object) this.value, i, (this.count - i) - 1);
        this.count--;
        return this;
    }

    public AbstractStringBuilder replace(int i, int i2, String str) {
        if (i < 0) {
            throw new StringIndexOutOfBoundsException(i);
        }
        if (i > this.count) {
            throw new StringIndexOutOfBoundsException("start > length()");
        }
        if (i > i2) {
            throw new StringIndexOutOfBoundsException("start > end");
        }
        if (i2 > this.count) {
            i2 = this.count;
        }
        int length = str.length();
        int i3 = (this.count + length) - (i2 - i);
        ensureCapacityInternal(i3);
        System.arraycopy((Object) this.value, i2, (Object) this.value, length + i, this.count - i2);
        str.getChars(this.value, i);
        this.count = i3;
        return this;
    }

    public String substring(int i) {
        return substring(i, this.count);
    }

    @Override
    public CharSequence subSequence(int i, int i2) {
        return substring(i, i2);
    }

    public String substring(int i, int i2) {
        if (i < 0) {
            throw new StringIndexOutOfBoundsException(i);
        }
        if (i2 > this.count) {
            throw new StringIndexOutOfBoundsException(i2);
        }
        if (i > i2) {
            throw new StringIndexOutOfBoundsException(i2 - i);
        }
        return new String(this.value, i, i2 - i);
    }

    public AbstractStringBuilder insert(int i, char[] cArr, int i2, int i3) {
        if (i < 0 || i > length()) {
            throw new StringIndexOutOfBoundsException(i);
        }
        if (i2 < 0 || i3 < 0 || i2 > cArr.length - i3) {
            throw new StringIndexOutOfBoundsException("offset " + i2 + ", len " + i3 + ", str.length " + cArr.length);
        }
        ensureCapacityInternal(this.count + i3);
        System.arraycopy((Object) this.value, i, (Object) this.value, i + i3, this.count - i);
        System.arraycopy((Object) cArr, i2, (Object) this.value, i, i3);
        this.count += i3;
        return this;
    }

    public AbstractStringBuilder insert(int i, Object obj) {
        return insert(i, String.valueOf(obj));
    }

    public AbstractStringBuilder insert(int i, String str) {
        if (i < 0 || i > length()) {
            throw new StringIndexOutOfBoundsException(i);
        }
        if (str == null) {
            str = "null";
        }
        int length = str.length();
        ensureCapacityInternal(this.count + length);
        System.arraycopy((Object) this.value, i, (Object) this.value, i + length, this.count - i);
        str.getChars(this.value, i);
        this.count += length;
        return this;
    }

    public AbstractStringBuilder insert(int i, char[] cArr) {
        if (i < 0 || i > length()) {
            throw new StringIndexOutOfBoundsException(i);
        }
        int length = cArr.length;
        ensureCapacityInternal(this.count + length);
        System.arraycopy((Object) this.value, i, (Object) this.value, i + length, this.count - i);
        System.arraycopy((Object) cArr, 0, (Object) this.value, i, length);
        this.count += length;
        return this;
    }

    public AbstractStringBuilder insert(int i, CharSequence charSequence) {
        if (charSequence == null) {
            charSequence = "null";
        }
        if (charSequence instanceof String) {
            return insert(i, (String) charSequence);
        }
        return insert(i, charSequence, 0, charSequence.length());
    }

    public AbstractStringBuilder insert(int i, CharSequence charSequence, int i2, int i3) {
        if (charSequence == null) {
            charSequence = "null";
        }
        if (i < 0 || i > length()) {
            throw new IndexOutOfBoundsException("dstOffset " + i);
        }
        if (i2 < 0 || i3 < 0 || i2 > i3 || i3 > charSequence.length()) {
            throw new IndexOutOfBoundsException("start " + i2 + ", end " + i3 + ", s.length() " + charSequence.length());
        }
        int i4 = i3 - i2;
        ensureCapacityInternal(this.count + i4);
        System.arraycopy((Object) this.value, i, (Object) this.value, i + i4, this.count - i);
        while (i2 < i3) {
            this.value[i] = charSequence.charAt(i2);
            i2++;
            i++;
        }
        this.count += i4;
        return this;
    }

    public AbstractStringBuilder insert(int i, boolean z) {
        return insert(i, String.valueOf(z));
    }

    public AbstractStringBuilder insert(int i, char c) {
        ensureCapacityInternal(this.count + 1);
        System.arraycopy((Object) this.value, i, (Object) this.value, i + 1, this.count - i);
        this.value[i] = c;
        this.count++;
        return this;
    }

    public AbstractStringBuilder insert(int i, int i2) {
        return insert(i, String.valueOf(i2));
    }

    public AbstractStringBuilder insert(int i, long j) {
        return insert(i, String.valueOf(j));
    }

    public AbstractStringBuilder insert(int i, float f) {
        return insert(i, String.valueOf(f));
    }

    public AbstractStringBuilder insert(int i, double d) {
        return insert(i, String.valueOf(d));
    }

    public int indexOf(String str) {
        return indexOf(str, 0);
    }

    public int indexOf(String str, int i) {
        return String.indexOf(this.value, 0, this.count, str.toCharArray(), 0, str.length(), i);
    }

    public int lastIndexOf(String str) {
        return lastIndexOf(str, this.count);
    }

    public int lastIndexOf(String str, int i) {
        return String.lastIndexOf(this.value, 0, this.count, str.toCharArray(), 0, str.length(), i);
    }

    public AbstractStringBuilder reverse() {
        int i = this.count - 1;
        boolean z = false;
        for (int i2 = (i - 1) >> 1; i2 >= 0; i2--) {
            int i3 = i - i2;
            char c = this.value[i2];
            char c2 = this.value[i3];
            this.value[i2] = c2;
            this.value[i3] = c;
            if (Character.isSurrogate(c) || Character.isSurrogate(c2)) {
                z = true;
            }
        }
        if (z) {
            reverseAllValidSurrogatePairs();
        }
        return this;
    }

    private void reverseAllValidSurrogatePairs() {
        int i = 0;
        while (i < this.count - 1) {
            char c = this.value[i];
            if (Character.isLowSurrogate(c)) {
                int i2 = i + 1;
                char c2 = this.value[i2];
                if (Character.isHighSurrogate(c2)) {
                    this.value[i] = c2;
                    this.value[i2] = c;
                    i = i2;
                }
            }
            i++;
        }
    }

    final char[] getValue() {
        return this.value;
    }
}
