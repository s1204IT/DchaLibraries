package java.lang;

import dalvik.bytecode.Opcodes;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.Charsets;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Formatter;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.regex.Splitter;
import libcore.util.EmptyArray;

public final class String implements Serializable, Comparable<String>, CharSequence {
    private static final char REPLACEMENT_CHAR = 65533;
    private static final long serialVersionUID = -6849794470754667710L;
    private final int count;
    private int hashCode;
    private final int offset;
    private final char[] value;
    public static final Comparator<String> CASE_INSENSITIVE_ORDER = new CaseInsensitiveComparator();
    private static final char[] ASCII = new char[128];

    private native int fastIndexOf(int i, int i2);

    @Override
    public native int compareTo(String str);

    public native String intern();

    private static final class CaseInsensitiveComparator implements Comparator<String>, Serializable {
        private static final long serialVersionUID = 8575799808933029326L;

        private CaseInsensitiveComparator() {
        }

        @Override
        public int compare(String o1, String o2) {
            return o1.compareToIgnoreCase(o2);
        }
    }

    static {
        for (int i = 0; i < ASCII.length; i++) {
            ASCII[i] = (char) i;
        }
    }

    public String() {
        this.value = EmptyArray.CHAR;
        this.offset = 0;
        this.count = 0;
    }

    @FindBugsSuppressWarnings({"DM_DEFAULT_ENCODING"})
    public String(byte[] data) {
        this(data, 0, data.length);
    }

    @Deprecated
    public String(byte[] data, int high) {
        this(data, high, 0, data.length);
    }

    public String(byte[] data, int offset, int byteCount) {
        this(data, offset, byteCount, Charset.defaultCharset());
    }

    @Deprecated
    public String(byte[] data, int high, int offset, int byteCount) {
        if ((offset | byteCount) < 0 || byteCount > data.length - offset) {
            throw failedBoundsCheck(data.length, offset, byteCount);
        }
        this.offset = 0;
        this.value = new char[byteCount];
        this.count = byteCount;
        int high2 = high << 8;
        int i = 0;
        while (i < this.count) {
            this.value[i] = (char) ((data[offset] & Opcodes.OP_CONST_CLASS_JUMBO) + high2);
            i++;
            offset++;
        }
    }

    public String(byte[] data, int offset, int byteCount, String charsetName) throws UnsupportedEncodingException {
        this(data, offset, byteCount, Charset.forNameUEE(charsetName));
    }

    public String(byte[] data, String charsetName) throws UnsupportedEncodingException {
        this(data, 0, data.length, Charset.forNameUEE(charsetName));
    }

    public String(byte[] data, int offset, int byteCount, Charset charset) {
        int s;
        if ((offset | byteCount) < 0 || byteCount > data.length - offset) {
            throw failedBoundsCheck(data.length, offset, byteCount);
        }
        String canonicalCharsetName = charset.name();
        if (canonicalCharsetName.equals("UTF-8")) {
            char[] v = new char[byteCount];
            int last = offset + byteCount;
            int s2 = 0;
            int idx = offset;
            while (idx < last) {
                int idx2 = idx + 1;
                byte b0 = data[idx];
                if ((b0 & Byte.MIN_VALUE) == 0) {
                    int val = b0 & Character.DIRECTIONALITY_UNDEFINED;
                    s = s2 + 1;
                    v[s2] = (char) val;
                } else if ((b0 & 224) == 192 || (b0 & 240) == 224 || (b0 & 248) == 240 || (b0 & 252) == 248 || (b0 & 254) == 252) {
                    int utfCount = 1;
                    if ((b0 & 240) == 224) {
                        utfCount = 2;
                    } else if ((b0 & 248) == 240) {
                        utfCount = 3;
                    } else if ((b0 & 252) == 248) {
                        utfCount = 4;
                    } else if ((b0 & 254) == 252) {
                        utfCount = 5;
                    }
                    if (idx2 + utfCount > last) {
                        v[s2] = REPLACEMENT_CHAR;
                        s2++;
                        idx = idx2;
                    } else {
                        int val2 = b0 & (31 >> (utfCount - 1));
                        int i = 0;
                        while (true) {
                            idx = idx2;
                            if (i < utfCount) {
                                idx2 = idx + 1;
                                byte b = data[idx];
                                if ((b & 192) != 128) {
                                    v[s2] = REPLACEMENT_CHAR;
                                    s2++;
                                    idx = idx2 - 1;
                                    break;
                                }
                                val2 = (val2 << 6) | (b & 63);
                                i++;
                            } else if (utfCount != 2 && val2 >= 55296 && val2 <= 57343) {
                                v[s2] = REPLACEMENT_CHAR;
                                s2++;
                            } else if (val2 > 1114111) {
                                v[s2] = REPLACEMENT_CHAR;
                                s2++;
                            } else {
                                if (val2 < 65536) {
                                    s = s2 + 1;
                                    v[s2] = (char) val2;
                                } else {
                                    int x = val2 & 65535;
                                    int u = (val2 >> 16) & 31;
                                    int w = (u - 1) & 65535;
                                    int hi = 55296 | (w << 6) | (x >> 10);
                                    int lo = 56320 | (x & 1023);
                                    int s3 = s2 + 1;
                                    v[s2] = (char) hi;
                                    v[s3] = (char) lo;
                                    s = s3 + 1;
                                }
                                idx2 = idx;
                            }
                        }
                    }
                } else {
                    s = s2 + 1;
                    v[s2] = REPLACEMENT_CHAR;
                }
                s2 = s;
                idx = idx2;
            }
            if (s2 == byteCount) {
                this.offset = 0;
                this.value = v;
                this.count = s2;
                return;
            } else {
                this.offset = 0;
                this.value = new char[s2];
                this.count = s2;
                System.arraycopy(v, 0, this.value, 0, s2);
                return;
            }
        }
        if (canonicalCharsetName.equals("ISO-8859-1")) {
            this.offset = 0;
            this.value = new char[byteCount];
            this.count = byteCount;
            Charsets.isoLatin1BytesToChars(data, offset, byteCount, this.value);
            return;
        }
        if (canonicalCharsetName.equals("US-ASCII")) {
            this.offset = 0;
            this.value = new char[byteCount];
            this.count = byteCount;
            Charsets.asciiBytesToChars(data, offset, byteCount, this.value);
            return;
        }
        CharBuffer cb = charset.decode(ByteBuffer.wrap(data, offset, byteCount));
        this.offset = 0;
        this.count = cb.length();
        if (this.count > 0) {
            this.value = new char[this.count];
            System.arraycopy(cb.array(), 0, this.value, 0, this.count);
        } else {
            this.value = EmptyArray.CHAR;
        }
    }

    public String(byte[] data, Charset charset) {
        this(data, 0, data.length, charset);
    }

    public String(char[] data) {
        this(data, 0, data.length);
    }

    public String(char[] data, int offset, int charCount) {
        if ((offset | charCount) < 0 || charCount > data.length - offset) {
            throw failedBoundsCheck(data.length, offset, charCount);
        }
        this.offset = 0;
        this.value = new char[charCount];
        this.count = charCount;
        System.arraycopy(data, offset, this.value, 0, this.count);
    }

    String(int offset, int charCount, char[] chars) {
        this.value = chars;
        this.offset = offset;
        this.count = charCount;
    }

    public String(String toCopy) {
        this.value = toCopy.value.length == toCopy.count ? toCopy.value : Arrays.copyOfRange(toCopy.value, toCopy.offset, toCopy.offset + toCopy.length());
        this.offset = 0;
        this.count = this.value.length;
    }

    public String(StringBuffer stringBuffer) {
        this.offset = 0;
        synchronized (stringBuffer) {
            this.value = stringBuffer.shareValue();
            this.count = stringBuffer.length();
        }
    }

    public String(int[] codePoints, int offset, int count) {
        if (codePoints == null) {
            throw new NullPointerException("codePoints == null");
        }
        if ((offset | count) < 0 || count > codePoints.length - offset) {
            throw failedBoundsCheck(codePoints.length, offset, count);
        }
        this.offset = 0;
        this.value = new char[count * 2];
        int end = offset + count;
        int c = 0;
        for (int i = offset; i < end; i++) {
            c += Character.toChars(codePoints[i], this.value, c);
        }
        this.count = c;
    }

    public String(StringBuilder stringBuilder) {
        if (stringBuilder == null) {
            throw new NullPointerException("stringBuilder == null");
        }
        this.offset = 0;
        this.count = stringBuilder.length();
        this.value = new char[this.count];
        stringBuilder.getChars(0, this.count, this.value, 0);
    }

    @Override
    public char charAt(int index) {
        if (index < 0 || index >= this.count) {
            throw indexAndLength(index);
        }
        return this.value[this.offset + index];
    }

    private StringIndexOutOfBoundsException indexAndLength(int index) {
        throw new StringIndexOutOfBoundsException(this, index);
    }

    private StringIndexOutOfBoundsException startEndAndLength(int start, int end) {
        throw new StringIndexOutOfBoundsException(this, start, end - start);
    }

    private StringIndexOutOfBoundsException failedBoundsCheck(int arrayLength, int offset, int count) {
        throw new StringIndexOutOfBoundsException(arrayLength, offset, count);
    }

    private char foldCase(char ch) {
        if (ch >= 128) {
            return Character.toLowerCase(Character.toUpperCase(ch));
        }
        if ('A' <= ch && ch <= 'Z') {
            return (char) (ch + ' ');
        }
        return ch;
    }

    public int compareToIgnoreCase(String string) {
        int o1 = this.offset;
        int o2 = string.offset;
        int end = this.offset + (this.count < string.count ? this.count : string.count);
        char[] target = string.value;
        int o22 = o2;
        int o12 = o1;
        while (o12 < end) {
            int o13 = o12 + 1;
            char c1 = this.value[o12];
            int o23 = o22 + 1;
            char c2 = target[o22];
            if (c1 == c2) {
                o22 = o23;
                o12 = o13;
            } else {
                int result = foldCase(c1) - foldCase(c2);
                if (result != 0) {
                    return result;
                }
                o22 = o23;
                o12 = o13;
            }
        }
        return this.count - string.count;
    }

    public String concat(String string) {
        if (string.count <= 0 || this.count <= 0) {
            return this.count != 0 ? this : string;
        }
        char[] buffer = new char[this.count + string.count];
        System.arraycopy(this.value, this.offset, buffer, 0, this.count);
        System.arraycopy(string.value, string.offset, buffer, this.count, string.count);
        return new String(0, buffer.length, buffer);
    }

    public static String copyValueOf(char[] data) {
        return new String(data, 0, data.length);
    }

    public static String copyValueOf(char[] data, int start, int length) {
        return new String(data, start, length);
    }

    public boolean endsWith(String suffix) {
        return regionMatches(this.count - suffix.count, suffix, 0, suffix.count);
    }

    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (!(other instanceof String)) {
            return false;
        }
        String s = (String) other;
        int count = this.count;
        if (s.count == count && hashCode() == s.hashCode()) {
            char[] value1 = this.value;
            int offset1 = this.offset;
            char[] value2 = s.value;
            int offset2 = s.offset;
            int end = offset1 + count;
            while (offset1 < end) {
                if (value1[offset1] != value2[offset2]) {
                    return false;
                }
                offset1++;
                offset2++;
            }
            return true;
        }
        return false;
    }

    @FindBugsSuppressWarnings({"ES_COMPARING_PARAMETER_STRING_WITH_EQ"})
    public boolean equalsIgnoreCase(String string) {
        if (string == this) {
            return true;
        }
        if (string == null || this.count != string.count) {
            return false;
        }
        int o1 = this.offset;
        int o2 = string.offset;
        int end = this.offset + this.count;
        char[] target = string.value;
        int o22 = o2;
        int o12 = o1;
        while (o12 < end) {
            int o13 = o12 + 1;
            char c1 = this.value[o12];
            int o23 = o22 + 1;
            char c2 = target[o22];
            if (c1 != c2 && foldCase(c1) != foldCase(c2)) {
                return false;
            }
            o22 = o23;
            o12 = o13;
        }
        return true;
    }

    @Deprecated
    public void getBytes(int start, int end, byte[] data, int index) {
        if (start >= 0 && start <= end && end <= this.count) {
            int end2 = end + this.offset;
            try {
                int i = this.offset + start;
                int index2 = index;
                while (i < end2) {
                    index = index2 + 1;
                    data[index2] = (byte) this.value[i];
                    i++;
                    index2 = index;
                }
                return;
            } catch (ArrayIndexOutOfBoundsException e) {
                throw failedBoundsCheck(data.length, index, end2 - start);
            }
        }
        throw startEndAndLength(start, end);
    }

    public byte[] getBytes() {
        return getBytes(Charset.defaultCharset());
    }

    public byte[] getBytes(String charsetName) throws UnsupportedEncodingException {
        return getBytes(Charset.forNameUEE(charsetName));
    }

    public byte[] getBytes(Charset charset) {
        String canonicalCharsetName = charset.name();
        if (canonicalCharsetName.equals("UTF-8")) {
            return Charsets.toUtf8Bytes(this.value, this.offset, this.count);
        }
        if (canonicalCharsetName.equals("ISO-8859-1")) {
            return Charsets.toIsoLatin1Bytes(this.value, this.offset, this.count);
        }
        if (canonicalCharsetName.equals("US-ASCII")) {
            return Charsets.toAsciiBytes(this.value, this.offset, this.count);
        }
        if (canonicalCharsetName.equals("UTF-16BE")) {
            return Charsets.toBigEndianUtf16Bytes(this.value, this.offset, this.count);
        }
        CharBuffer chars = CharBuffer.wrap(this.value, this.offset, this.count);
        ByteBuffer buffer = charset.encode(chars.asReadOnlyBuffer());
        byte[] bytes = new byte[buffer.limit()];
        buffer.get(bytes);
        return bytes;
    }

    public void getChars(int start, int end, char[] buffer, int index) {
        if (start >= 0 && start <= end && end <= this.count) {
            System.arraycopy(this.value, this.offset + start, buffer, index, end - start);
            return;
        }
        throw startEndAndLength(start, end);
    }

    void _getChars(int start, int end, char[] buffer, int index) {
        System.arraycopy(this.value, this.offset + start, buffer, index, end - start);
    }

    public int hashCode() {
        int hash = this.hashCode;
        if (hash == 0) {
            if (this.count == 0) {
                return 0;
            }
            int end = this.count + this.offset;
            char[] chars = this.value;
            for (int i = this.offset; i < end; i++) {
                hash = (hash * 31) + chars[i];
            }
            this.hashCode = hash;
        }
        return hash;
    }

    public int indexOf(int c) {
        return c > 65535 ? indexOfSupplementary(c, 0) : fastIndexOf(c, 0);
    }

    public int indexOf(int c, int start) {
        return c > 65535 ? indexOfSupplementary(c, start) : fastIndexOf(c, start);
    }

    private int indexOfSupplementary(int c, int start) {
        if (!Character.isSupplementaryCodePoint(c)) {
            return -1;
        }
        char[] chars = Character.toChars(c);
        String needle = new String(0, chars.length, chars);
        return indexOf(needle, start);
    }

    public int indexOf(String string) {
        int start = 0;
        int subCount = string.count;
        int _count = this.count;
        if (subCount > 0) {
            if (subCount > _count) {
                return -1;
            }
            char[] target = string.value;
            int subOffset = string.offset;
            char firstChar = target[subOffset];
            int end = subOffset + subCount;
            while (true) {
                int i = indexOf(firstChar, start);
                if (i == -1 || subCount + i > _count) {
                    break;
                }
                int o1 = this.offset + i;
                int o2 = subOffset;
                char[] _value = this.value;
                do {
                    o2++;
                    if (o2 >= end) {
                        break;
                    }
                    o1++;
                } while (_value[o1] == target[o2]);
                if (o2 != end) {
                    start = i + 1;
                } else {
                    return i;
                }
            }
        } else {
            if (0 < _count) {
                _count = 0;
            }
            return _count;
        }
    }

    public int indexOf(String subString, int start) {
        if (start < 0) {
            start = 0;
        }
        int subCount = subString.count;
        int _count = this.count;
        if (subCount > 0) {
            if (subCount + start > _count) {
                return -1;
            }
            char[] target = subString.value;
            int subOffset = subString.offset;
            char firstChar = target[subOffset];
            int end = subOffset + subCount;
            while (true) {
                int i = indexOf(firstChar, start);
                if (i == -1 || subCount + i > _count) {
                    break;
                }
                int o1 = this.offset + i;
                int o2 = subOffset;
                char[] _value = this.value;
                do {
                    o2++;
                    if (o2 >= end) {
                        break;
                    }
                    o1++;
                } while (_value[o1] == target[o2]);
                if (o2 != end) {
                    start = i + 1;
                } else {
                    return i;
                }
            }
        } else {
            if (start < _count) {
                _count = start;
            }
            return _count;
        }
    }

    public boolean isEmpty() {
        return this.count == 0;
    }

    public int lastIndexOf(int c) {
        if (c > 65535) {
            return lastIndexOfSupplementary(c, Integer.MAX_VALUE);
        }
        int _count = this.count;
        int _offset = this.offset;
        char[] _value = this.value;
        for (int i = (_offset + _count) - 1; i >= _offset; i--) {
            if (_value[i] == c) {
                return i - _offset;
            }
        }
        return -1;
    }

    public int lastIndexOf(int c, int start) {
        if (c > 65535) {
            return lastIndexOfSupplementary(c, start);
        }
        int _count = this.count;
        int _offset = this.offset;
        char[] _value = this.value;
        if (start >= 0) {
            if (start >= _count) {
                start = _count - 1;
            }
            for (int i = _offset + start; i >= _offset; i--) {
                if (_value[i] == c) {
                    return i - _offset;
                }
            }
        }
        return -1;
    }

    private int lastIndexOfSupplementary(int c, int start) {
        if (!Character.isSupplementaryCodePoint(c)) {
            return -1;
        }
        char[] chars = Character.toChars(c);
        String needle = new String(0, chars.length, chars);
        return lastIndexOf(needle, start);
    }

    public int lastIndexOf(String string) {
        return lastIndexOf(string, this.count);
    }

    public int lastIndexOf(String subString, int start) {
        int subCount = subString.count;
        if (subCount > this.count || start < 0) {
            return -1;
        }
        if (subCount > 0) {
            if (start > this.count - subCount) {
                start = this.count - subCount;
            }
            char[] target = subString.value;
            int subOffset = subString.offset;
            char firstChar = target[subOffset];
            int end = subOffset + subCount;
            while (true) {
                int i = lastIndexOf(firstChar, start);
                if (i == -1) {
                    return -1;
                }
                int o1 = this.offset + i;
                int o2 = subOffset;
                do {
                    o2++;
                    if (o2 >= end) {
                        break;
                    }
                    o1++;
                } while (this.value[o1] == target[o2]);
                if (o2 != end) {
                    start = i - 1;
                } else {
                    return i;
                }
            }
        } else {
            return start < this.count ? start : this.count;
        }
    }

    @Override
    public int length() {
        return this.count;
    }

    public boolean regionMatches(int thisStart, String string, int start, int length) {
        if (string == null) {
            throw new NullPointerException("string == null");
        }
        if (start < 0 || string.count - start < length) {
            return false;
        }
        if (thisStart < 0 || this.count - thisStart < length) {
            return false;
        }
        if (length <= 0) {
            return true;
        }
        int o1 = this.offset + thisStart;
        int o2 = string.offset + start;
        char[] value1 = this.value;
        char[] value2 = string.value;
        for (int i = 0; i < length; i++) {
            if (value1[o1 + i] != value2[o2 + i]) {
                return false;
            }
        }
        return true;
    }

    public boolean regionMatches(boolean ignoreCase, int thisStart, String string, int start, int length) {
        if (!ignoreCase) {
            return regionMatches(thisStart, string, start, length);
        }
        if (string == null) {
            throw new NullPointerException("string == null");
        }
        if (thisStart < 0 || length > this.count - thisStart || start < 0 || length > string.count - start) {
            return false;
        }
        int thisStart2 = thisStart + this.offset;
        int start2 = start + string.offset;
        int end = thisStart2 + length;
        char[] target = string.value;
        int start3 = start2;
        int thisStart3 = thisStart2;
        while (thisStart3 < end) {
            int thisStart4 = thisStart3 + 1;
            char c1 = this.value[thisStart3];
            int start4 = start3 + 1;
            char c2 = target[start3];
            if (c1 != c2 && foldCase(c1) != foldCase(c2)) {
                return false;
            }
            start3 = start4;
            thisStart3 = thisStart4;
        }
        return true;
    }

    public String replace(char oldChar, char newChar) {
        char[] buffer = this.value;
        int _offset = this.offset;
        int _count = this.count;
        int idx = _offset;
        int last = _offset + _count;
        boolean copied = false;
        while (idx < last) {
            if (buffer[idx] == oldChar) {
                if (!copied) {
                    char[] newBuffer = new char[_count];
                    System.arraycopy(buffer, _offset, newBuffer, 0, _count);
                    buffer = newBuffer;
                    idx -= _offset;
                    last -= _offset;
                    copied = true;
                }
                buffer[idx] = newChar;
            }
            idx++;
        }
        return copied ? new String(0, this.count, buffer) : this;
    }

    public String replace(CharSequence target, CharSequence replacement) {
        if (target == null) {
            throw new NullPointerException("target == null");
        }
        if (replacement == null) {
            throw new NullPointerException("replacement == null");
        }
        String targetString = target.toString();
        int matchStart = indexOf(targetString, 0);
        if (matchStart != -1) {
            String replacementString = replacement.toString();
            int targetLength = targetString.length();
            if (targetLength == 0) {
                int resultLength = this.count + ((this.count + 1) * replacementString.length());
                StringBuilder result = new StringBuilder(resultLength);
                result.append(replacementString);
                int end = this.offset + this.count;
                for (int i = this.offset; i != end; i++) {
                    result.append(this.value[i]);
                    result.append(replacementString);
                }
                return result.toString();
            }
            StringBuilder result2 = new StringBuilder(this.count);
            int searchStart = 0;
            do {
                result2.append(this.value, this.offset + searchStart, matchStart - searchStart);
                result2.append(replacementString);
                searchStart = matchStart + targetLength;
                matchStart = indexOf(targetString, searchStart);
            } while (matchStart != -1);
            result2.append(this.value, this.offset + searchStart, this.count - searchStart);
            return result2.toString();
        }
        return this;
    }

    public boolean startsWith(String prefix) {
        return startsWith(prefix, 0);
    }

    public boolean startsWith(String prefix, int start) {
        return regionMatches(start, prefix, 0, prefix.count);
    }

    public String substring(int start) {
        if (start != 0) {
            if (start >= 0 && start <= this.count) {
                return new String(this.offset + start, this.count - start, this.value);
            }
            throw indexAndLength(start);
        }
        return this;
    }

    public String substring(int start, int end) {
        if (start != 0 || end != this.count) {
            if (start >= 0 && start <= end && end <= this.count) {
                return new String(this.offset + start, end - start, this.value);
            }
            throw startEndAndLength(start, end);
        }
        return this;
    }

    public char[] toCharArray() {
        char[] buffer = new char[this.count];
        System.arraycopy(this.value, this.offset, buffer, 0, this.count);
        return buffer;
    }

    public String toLowerCase() {
        return CaseMapper.toLowerCase(Locale.getDefault(), this, this.value, this.offset, this.count);
    }

    public String toLowerCase(Locale locale) {
        return CaseMapper.toLowerCase(locale, this, this.value, this.offset, this.count);
    }

    @Override
    public String toString() {
        return this;
    }

    public String toUpperCase() {
        return CaseMapper.toUpperCase(Locale.getDefault(), this, this.value, this.offset, this.count);
    }

    public String toUpperCase(Locale locale) {
        return CaseMapper.toUpperCase(locale, this, this.value, this.offset, this.count);
    }

    public String trim() {
        int start = this.offset;
        int last = (this.offset + this.count) - 1;
        int end = last;
        while (start <= end && this.value[start] <= ' ') {
            start++;
        }
        while (end >= start && this.value[end] <= ' ') {
            end--;
        }
        return (start == this.offset && end == last) ? this : new String(start, (end - start) + 1, this.value);
    }

    public static String valueOf(char[] data) {
        return new String(data, 0, data.length);
    }

    public static String valueOf(char[] data, int start, int length) {
        return new String(data, start, length);
    }

    public static String valueOf(char value) {
        String s;
        if (value < 128) {
            s = new String(value, 1, ASCII);
        } else {
            s = new String(0, 1, new char[]{value});
        }
        s.hashCode = value;
        return s;
    }

    public static String valueOf(double value) {
        return Double.toString(value);
    }

    public static String valueOf(float value) {
        return Float.toString(value);
    }

    public static String valueOf(int value) {
        return Integer.toString(value);
    }

    public static String valueOf(long value) {
        return Long.toString(value);
    }

    public static String valueOf(Object value) {
        return value != null ? value.toString() : "null";
    }

    public static String valueOf(boolean value) {
        return value ? "true" : "false";
    }

    public boolean contentEquals(StringBuffer sb) {
        boolean zRegionMatches = false;
        synchronized (sb) {
            int size = sb.length();
            if (this.count == size) {
                zRegionMatches = regionMatches(0, new String(0, size, sb.getValue()), 0, size);
            }
        }
        return zRegionMatches;
    }

    public boolean contentEquals(CharSequence cs) {
        if (cs == null) {
            throw new NullPointerException("cs == null");
        }
        int len = cs.length();
        if (len != this.count) {
            return false;
        }
        if (len == 0 && this.count == 0) {
            return true;
        }
        return regionMatches(0, cs.toString(), 0, len);
    }

    public boolean matches(String regularExpression) {
        return Pattern.matches(regularExpression, this);
    }

    public String replaceAll(String regularExpression, String replacement) {
        return Pattern.compile(regularExpression).matcher(this).replaceAll(replacement);
    }

    public String replaceFirst(String regularExpression, String replacement) {
        return Pattern.compile(regularExpression).matcher(this).replaceFirst(replacement);
    }

    public String[] split(String regularExpression) {
        return split(regularExpression, 0);
    }

    public String[] split(String regularExpression, int limit) {
        String[] result = Splitter.fastSplit(regularExpression, this, limit);
        return result != null ? result : Pattern.compile(regularExpression).split(this, limit);
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        return substring(start, end);
    }

    public int codePointAt(int index) {
        if (index < 0 || index >= this.count) {
            throw indexAndLength(index);
        }
        return Character.codePointAt(this.value, this.offset + index, this.offset + this.count);
    }

    public int codePointBefore(int index) {
        if (index < 1 || index > this.count) {
            throw indexAndLength(index);
        }
        return Character.codePointBefore(this.value, this.offset + index, this.offset);
    }

    public int codePointCount(int start, int end) {
        if (start < 0 || end > this.count || start > end) {
            throw startEndAndLength(start, end);
        }
        return Character.codePointCount(this.value, this.offset + start, end - start);
    }

    public boolean contains(CharSequence cs) {
        if (cs == null) {
            throw new NullPointerException("cs == null");
        }
        return indexOf(cs.toString()) >= 0;
    }

    public int offsetByCodePoints(int index, int codePointOffset) {
        int s = index + this.offset;
        int r = Character.offsetByCodePoints(this.value, this.offset, this.count, s, codePointOffset);
        return r - this.offset;
    }

    public static String format(String format, Object... args) {
        return format(Locale.getDefault(), format, args);
    }

    public static String format(Locale locale, String format, Object... args) {
        if (format == null) {
            throw new NullPointerException("format == null");
        }
        int bufferSize = format.length() + (args == null ? 0 : args.length * 10);
        Formatter f = new Formatter(new StringBuilder(bufferSize), locale);
        return f.format(format, args).toString();
    }

    @FindBugsSuppressWarnings({"UPM_UNCALLED_PRIVATE_METHOD"})
    private static int indexOf(String haystackString, String needleString, int cache, int md2, char lastChar) {
        char[] haystack = haystackString.value;
        int haystackOffset = haystackString.offset;
        int haystackLength = haystackString.count;
        char[] needle = needleString.value;
        int needleOffset = needleString.offset;
        int needleLength = needleString.count;
        int needleLengthMinus1 = needleLength - 1;
        int haystackEnd = haystackOffset + haystackLength;
        int i = haystackOffset + needleLengthMinus1;
        while (i < haystackEnd) {
            if (lastChar == haystack[i]) {
                for (int j = 0; j < needleLengthMinus1; j++) {
                    if (needle[j + needleOffset] != haystack[(i + j) - needleLengthMinus1]) {
                        int skip = 1;
                        if (((1 << haystack[i]) & cache) == 0) {
                            skip = 1 + j;
                        }
                        i += Math.max(md2, skip);
                    }
                }
                return (i - needleLengthMinus1) - haystackOffset;
            }
            if (((1 << haystack[i]) & cache) == 0) {
                i += needleLengthMinus1;
            }
            i++;
        }
        return -1;
    }
}
