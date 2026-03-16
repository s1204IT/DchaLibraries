package java.lang;

import java.io.InvalidObjectException;
import java.util.Arrays;
import java.util.Locale;
import libcore.util.EmptyArray;

abstract class AbstractStringBuilder {
    static final int INITIAL_CAPACITY = 16;
    private int count;
    private boolean shared;
    private char[] value;

    final char[] getValue() {
        return this.value;
    }

    final char[] shareValue() {
        this.shared = true;
        return this.value;
    }

    final void set(char[] val, int len) throws InvalidObjectException {
        if (val == null) {
            val = EmptyArray.CHAR;
        }
        if (val.length < len) {
            throw new InvalidObjectException("count out of range");
        }
        this.shared = false;
        this.value = val;
        this.count = len;
    }

    AbstractStringBuilder() {
        this.value = new char[16];
    }

    AbstractStringBuilder(int capacity) {
        if (capacity < 0) {
            throw new NegativeArraySizeException(Integer.toString(capacity));
        }
        this.value = new char[capacity];
    }

    AbstractStringBuilder(String string) {
        this.count = string.length();
        this.shared = false;
        this.value = new char[this.count + 16];
        string._getChars(0, this.count, this.value, 0);
    }

    private void enlargeBuffer(int min) {
        int newCount = (this.value.length >> 1) + this.value.length + 2;
        if (min <= newCount) {
            min = newCount;
        }
        char[] newData = new char[min];
        System.arraycopy(this.value, 0, newData, 0, this.count);
        this.value = newData;
        this.shared = false;
    }

    final void appendNull() {
        int newCount = this.count + 4;
        if (newCount > this.value.length) {
            enlargeBuffer(newCount);
        }
        char[] cArr = this.value;
        int i = this.count;
        this.count = i + 1;
        cArr[i] = 'n';
        char[] cArr2 = this.value;
        int i2 = this.count;
        this.count = i2 + 1;
        cArr2[i2] = Locale.UNICODE_LOCALE_EXTENSION;
        char[] cArr3 = this.value;
        int i3 = this.count;
        this.count = i3 + 1;
        cArr3[i3] = 'l';
        char[] cArr4 = this.value;
        int i4 = this.count;
        this.count = i4 + 1;
        cArr4[i4] = 'l';
    }

    final void append0(char[] chars) {
        int newCount = this.count + chars.length;
        if (newCount > this.value.length) {
            enlargeBuffer(newCount);
        }
        System.arraycopy(chars, 0, this.value, this.count, chars.length);
        this.count = newCount;
    }

    final void append0(char[] chars, int offset, int length) {
        Arrays.checkOffsetAndCount(chars.length, offset, length);
        int newCount = this.count + length;
        if (newCount > this.value.length) {
            enlargeBuffer(newCount);
        }
        System.arraycopy(chars, offset, this.value, this.count, length);
        this.count = newCount;
    }

    final void append0(char ch) {
        if (this.count == this.value.length) {
            enlargeBuffer(this.count + 1);
        }
        char[] cArr = this.value;
        int i = this.count;
        this.count = i + 1;
        cArr[i] = ch;
    }

    final void append0(String string) {
        if (string == null) {
            appendNull();
            return;
        }
        int length = string.length();
        int newCount = this.count + length;
        if (newCount > this.value.length) {
            enlargeBuffer(newCount);
        }
        string._getChars(0, length, this.value, this.count);
        this.count = newCount;
    }

    final void append0(CharSequence s, int start, int end) {
        if (s == null) {
            s = "null";
        }
        if ((start | end) < 0 || start > end || end > s.length()) {
            throw new IndexOutOfBoundsException();
        }
        int length = end - start;
        int newCount = this.count + length;
        if (newCount > this.value.length) {
            enlargeBuffer(newCount);
        } else if (this.shared) {
            this.value = (char[]) this.value.clone();
            this.shared = false;
        }
        if (s instanceof String) {
            ((String) s)._getChars(start, end, this.value, this.count);
        } else if (s instanceof AbstractStringBuilder) {
            AbstractStringBuilder other = (AbstractStringBuilder) s;
            System.arraycopy(other.value, start, this.value, this.count, length);
        } else {
            int j = this.count;
            int i = start;
            int j2 = j;
            while (i < end) {
                this.value[j2] = s.charAt(i);
                i++;
                j2++;
            }
        }
        this.count = newCount;
    }

    public int capacity() {
        return this.value.length;
    }

    public char charAt(int index) {
        if (index < 0 || index >= this.count) {
            throw indexAndLength(index);
        }
        return this.value[index];
    }

    private StringIndexOutOfBoundsException indexAndLength(int index) {
        throw new StringIndexOutOfBoundsException(this.count, index);
    }

    private StringIndexOutOfBoundsException startEndAndLength(int start, int end) {
        throw new StringIndexOutOfBoundsException(this.count, start, end - start);
    }

    final void delete0(int start, int end) {
        if (end > this.count) {
            end = this.count;
        }
        if (start < 0 || start > this.count || start > end) {
            throw startEndAndLength(start, end);
        }
        if (end != start) {
            int length = this.count - end;
            if (length >= 0) {
                if (!this.shared) {
                    System.arraycopy(this.value, end, this.value, start, length);
                } else {
                    char[] newData = new char[this.value.length];
                    System.arraycopy(this.value, 0, newData, 0, start);
                    System.arraycopy(this.value, end, newData, start, length);
                    this.value = newData;
                    this.shared = false;
                }
            }
            this.count -= end - start;
        }
    }

    final void deleteCharAt0(int index) {
        if (index < 0 || index >= this.count) {
            throw indexAndLength(index);
        }
        delete0(index, index + 1);
    }

    public void ensureCapacity(int min) {
        if (min > this.value.length) {
            int ourMin = (this.value.length * 2) + 2;
            enlargeBuffer(Math.max(ourMin, min));
        }
    }

    public void getChars(int start, int end, char[] dst, int dstStart) {
        if (start > this.count || end > this.count || start > end) {
            throw startEndAndLength(start, end);
        }
        System.arraycopy(this.value, start, dst, dstStart, end - start);
    }

    final void insert0(int index, char[] chars) {
        if (index < 0 || index > this.count) {
            throw indexAndLength(index);
        }
        if (chars.length != 0) {
            move(chars.length, index);
            System.arraycopy(chars, 0, this.value, index, chars.length);
            this.count += chars.length;
        }
    }

    final void insert0(int index, char[] chars, int start, int length) {
        if (index >= 0 && index <= this.count && start >= 0 && length >= 0 && length <= chars.length - start) {
            if (length != 0) {
                move(length, index);
                System.arraycopy(chars, start, this.value, index, length);
                this.count += length;
                return;
            }
            return;
        }
        throw new StringIndexOutOfBoundsException("this.length=" + this.count + "; index=" + index + "; chars.length=" + chars.length + "; start=" + start + "; length=" + length);
    }

    final void insert0(int index, char ch) {
        if (index < 0 || index > this.count) {
            throw new ArrayIndexOutOfBoundsException(this.count, index);
        }
        move(1, index);
        this.value[index] = ch;
        this.count++;
    }

    final void insert0(int index, String string) {
        if (index >= 0 && index <= this.count) {
            if (string == null) {
                string = "null";
            }
            int min = string.length();
            if (min != 0) {
                move(min, index);
                string._getChars(0, min, this.value, index);
                this.count += min;
                return;
            }
            return;
        }
        throw indexAndLength(index);
    }

    final void insert0(int index, CharSequence s, int start, int end) {
        if (s == null) {
            s = "null";
        }
        if ((index | start | end) < 0 || index > this.count || start > end || end > s.length()) {
            throw new IndexOutOfBoundsException();
        }
        insert0(index, s.subSequence(start, end).toString());
    }

    public int length() {
        return this.count;
    }

    private void move(int size, int index) {
        int newCount;
        if (this.value.length - this.count >= size) {
            if (!this.shared) {
                System.arraycopy(this.value, index, this.value, index + size, this.count - index);
                return;
            }
            newCount = this.value.length;
        } else {
            newCount = Math.max(this.count + size, (this.value.length * 2) + 2);
        }
        char[] newData = new char[newCount];
        System.arraycopy(this.value, 0, newData, 0, index);
        System.arraycopy(this.value, index, newData, index + size, this.count - index);
        this.value = newData;
        this.shared = false;
    }

    final void replace0(int start, int end, String string) {
        if (start >= 0) {
            if (end > this.count) {
                end = this.count;
            }
            if (end > start) {
                int stringLength = string.length();
                int diff = (end - start) - stringLength;
                if (diff > 0) {
                    if (!this.shared) {
                        System.arraycopy(this.value, end, this.value, start + stringLength, this.count - end);
                    } else {
                        char[] newData = new char[this.value.length];
                        System.arraycopy(this.value, 0, newData, 0, start);
                        System.arraycopy(this.value, end, newData, start + stringLength, this.count - end);
                        this.value = newData;
                        this.shared = false;
                    }
                } else if (diff < 0) {
                    move(-diff, end);
                } else if (this.shared) {
                    this.value = (char[]) this.value.clone();
                    this.shared = false;
                }
                string._getChars(0, stringLength, this.value, start);
                this.count -= diff;
                return;
            }
            if (start == end) {
                if (string == null) {
                    throw new NullPointerException("string == null");
                }
                insert0(start, string);
                return;
            }
        }
        throw startEndAndLength(start, end);
    }

    final void reverse0() {
        char low;
        if (this.count >= 2) {
            if (!this.shared) {
                int end = this.count - 1;
                char frontHigh = this.value[0];
                char endLow = this.value[end];
                boolean allowFrontSur = true;
                boolean allowEndSur = true;
                int i = 0;
                int mid = this.count / 2;
                while (i < mid) {
                    char frontLow = this.value[i + 1];
                    char endHigh = this.value[end - 1];
                    boolean surAtFront = allowFrontSur && frontLow >= 56320 && frontLow <= 57343 && frontHigh >= 55296 && frontHigh <= 56319;
                    if (!surAtFront || this.count >= 3) {
                        boolean surAtEnd = allowEndSur && endHigh >= 55296 && endHigh <= 56319 && endLow >= 56320 && endLow <= 57343;
                        allowEndSur = true;
                        allowFrontSur = true;
                        if (surAtFront == surAtEnd) {
                            if (surAtFront) {
                                this.value[end] = frontLow;
                                this.value[end - 1] = frontHigh;
                                this.value[i] = endHigh;
                                this.value[i + 1] = endLow;
                                frontHigh = this.value[i + 2];
                                endLow = this.value[end - 2];
                                i++;
                                end--;
                            } else {
                                this.value[end] = frontHigh;
                                this.value[i] = endLow;
                                frontHigh = frontLow;
                                endLow = endHigh;
                            }
                        } else if (surAtFront) {
                            this.value[end] = frontLow;
                            this.value[i] = endLow;
                            endLow = endHigh;
                            allowFrontSur = false;
                        } else {
                            this.value[end] = frontHigh;
                            this.value[i] = endHigh;
                            frontHigh = frontLow;
                            allowEndSur = false;
                        }
                        i++;
                        end--;
                    } else {
                        return;
                    }
                }
                if ((this.count & 1) == 1) {
                    if (!allowFrontSur || !allowEndSur) {
                        char[] cArr = this.value;
                        if (!allowFrontSur) {
                            endLow = frontHigh;
                        }
                        cArr[end] = endLow;
                        return;
                    }
                    return;
                }
                return;
            }
            char[] newData = new char[this.value.length];
            int i2 = 0;
            int end2 = this.count;
            while (i2 < this.count) {
                char high = this.value[i2];
                if (i2 + 1 < this.count && high >= 55296 && high <= 56319 && (low = this.value[i2 + 1]) >= 56320 && low <= 57343) {
                    end2--;
                    newData[end2] = low;
                    i2++;
                }
                end2--;
                newData[end2] = high;
                i2++;
            }
            this.value = newData;
            this.shared = false;
        }
    }

    public void setCharAt(int index, char ch) {
        if (index < 0 || index >= this.count) {
            throw indexAndLength(index);
        }
        if (this.shared) {
            this.value = (char[]) this.value.clone();
            this.shared = false;
        }
        this.value[index] = ch;
    }

    public void setLength(int length) {
        if (length < 0) {
            throw new StringIndexOutOfBoundsException("length < 0: " + length);
        }
        if (length > this.value.length) {
            enlargeBuffer(length);
        } else if (this.shared) {
            char[] newData = new char[this.value.length];
            System.arraycopy(this.value, 0, newData, 0, this.count);
            this.value = newData;
            this.shared = false;
        } else if (this.count < length) {
            Arrays.fill(this.value, this.count, length, (char) 0);
        }
        this.count = length;
    }

    public String substring(int start) {
        if (start < 0 || start > this.count) {
            throw indexAndLength(start);
        }
        return start == this.count ? "" : new String(this.value, start, this.count - start);
    }

    public String substring(int start, int end) {
        if (start < 0 || start > end || end > this.count) {
            throw startEndAndLength(start, end);
        }
        return start == end ? "" : new String(this.value, start, end - start);
    }

    public String toString() {
        if (this.count == 0) {
            return "";
        }
        int wasted = this.value.length - this.count;
        if (wasted >= 256 || (wasted >= 16 && wasted >= (this.count >> 1))) {
            return new String(this.value, 0, this.count);
        }
        this.shared = true;
        return new String(0, this.count, this.value);
    }

    public CharSequence subSequence(int start, int end) {
        return substring(start, end);
    }

    public int indexOf(String string) {
        return indexOf(string, 0);
    }

    public int indexOf(String subString, int start) {
        if (start < 0) {
            start = 0;
        }
        int subCount = subString.length();
        if (subCount > 0) {
            if (subCount + start > this.count) {
                return -1;
            }
            char firstChar = subString.charAt(0);
            while (true) {
                int i = start;
                boolean found = false;
                while (true) {
                    if (i >= this.count) {
                        break;
                    }
                    if (this.value[i] != firstChar) {
                        i++;
                    } else {
                        found = true;
                        break;
                    }
                }
                if (!found || subCount + i > this.count) {
                    break;
                }
                int o1 = i;
                int o2 = 0;
                do {
                    o2++;
                    if (o2 >= subCount) {
                        break;
                    }
                    o1++;
                } while (this.value[o1] == subString.charAt(o2));
                if (o2 != subCount) {
                    start = i + 1;
                } else {
                    return i;
                }
            }
        } else {
            return (start < this.count || start == 0) ? start : this.count;
        }
    }

    public int lastIndexOf(String string) {
        return lastIndexOf(string, this.count);
    }

    public int lastIndexOf(String subString, int start) {
        int subCount = subString.length();
        if (subCount > this.count || start < 0) {
            return -1;
        }
        if (subCount > 0) {
            if (start > this.count - subCount) {
                start = this.count - subCount;
            }
            char firstChar = subString.charAt(0);
            while (true) {
                int i = start;
                boolean found = false;
                while (true) {
                    if (i < 0) {
                        break;
                    }
                    if (this.value[i] != firstChar) {
                        i--;
                    } else {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    return -1;
                }
                int o1 = i;
                int o2 = 0;
                do {
                    o2++;
                    if (o2 >= subCount) {
                        break;
                    }
                    o1++;
                } while (this.value[o1] == subString.charAt(o2));
                if (o2 != subCount) {
                    start = i - 1;
                } else {
                    return i;
                }
            }
        } else {
            int i2 = start < this.count ? start : this.count;
            return i2;
        }
    }

    public void trimToSize() {
        if (this.count < this.value.length) {
            char[] newValue = new char[this.count];
            System.arraycopy(this.value, 0, newValue, 0, this.count);
            this.value = newValue;
            this.shared = false;
        }
    }

    public int codePointAt(int index) {
        if (index < 0 || index >= this.count) {
            throw indexAndLength(index);
        }
        return Character.codePointAt(this.value, index, this.count);
    }

    public int codePointBefore(int index) {
        if (index < 1 || index > this.count) {
            throw indexAndLength(index);
        }
        return Character.codePointBefore(this.value, index);
    }

    public int codePointCount(int start, int end) {
        if (start < 0 || end > this.count || start > end) {
            throw startEndAndLength(start, end);
        }
        return Character.codePointCount(this.value, start, end - start);
    }

    public int offsetByCodePoints(int index, int codePointOffset) {
        return Character.offsetByCodePoints(this.value, 0, this.count, index, codePointOffset);
    }
}
