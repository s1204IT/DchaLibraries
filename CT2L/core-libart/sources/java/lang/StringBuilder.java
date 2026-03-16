package java.lang;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public final class StringBuilder extends AbstractStringBuilder implements Appendable, CharSequence, Serializable {
    private static final long serialVersionUID = 4383685877147921099L;

    @Override
    public int capacity() {
        return super.capacity();
    }

    @Override
    public char charAt(int i) {
        return super.charAt(i);
    }

    @Override
    public int codePointAt(int i) {
        return super.codePointAt(i);
    }

    @Override
    public int codePointBefore(int i) {
        return super.codePointBefore(i);
    }

    @Override
    public int codePointCount(int i, int i2) {
        return super.codePointCount(i, i2);
    }

    @Override
    public void ensureCapacity(int i) {
        super.ensureCapacity(i);
    }

    @Override
    public void getChars(int i, int i2, char[] cArr, int i3) {
        super.getChars(i, i2, cArr, i3);
    }

    @Override
    public int indexOf(String str) {
        return super.indexOf(str);
    }

    @Override
    public int indexOf(String str, int i) {
        return super.indexOf(str, i);
    }

    @Override
    public int lastIndexOf(String str) {
        return super.lastIndexOf(str);
    }

    @Override
    public int lastIndexOf(String str, int i) {
        return super.lastIndexOf(str, i);
    }

    @Override
    public int length() {
        return super.length();
    }

    @Override
    public int offsetByCodePoints(int i, int i2) {
        return super.offsetByCodePoints(i, i2);
    }

    @Override
    public void setCharAt(int i, char c) {
        super.setCharAt(i, c);
    }

    @Override
    public void setLength(int i) {
        super.setLength(i);
    }

    @Override
    public CharSequence subSequence(int i, int i2) {
        return super.subSequence(i, i2);
    }

    @Override
    public String substring(int i) {
        return super.substring(i);
    }

    @Override
    public String substring(int i, int i2) {
        return super.substring(i, i2);
    }

    @Override
    public void trimToSize() {
        super.trimToSize();
    }

    public StringBuilder() {
    }

    public StringBuilder(int capacity) {
        super(capacity);
    }

    public StringBuilder(CharSequence seq) {
        super(seq.toString());
    }

    public StringBuilder(String str) {
        super(str);
    }

    public StringBuilder append(boolean b) {
        append0(b ? "true" : "false");
        return this;
    }

    @Override
    public StringBuilder append(char c) {
        append0(c);
        return this;
    }

    public StringBuilder append(int i) {
        IntegralToString.appendInt(this, i);
        return this;
    }

    public StringBuilder append(long l) {
        IntegralToString.appendLong(this, l);
        return this;
    }

    public StringBuilder append(float f) {
        RealToString.getInstance().appendFloat(this, f);
        return this;
    }

    public StringBuilder append(double d) {
        RealToString.getInstance().appendDouble(this, d);
        return this;
    }

    public StringBuilder append(Object obj) {
        if (obj == null) {
            appendNull();
        } else {
            append0(obj.toString());
        }
        return this;
    }

    public StringBuilder append(String str) {
        append0(str);
        return this;
    }

    public StringBuilder append(StringBuffer sb) {
        if (sb == null) {
            appendNull();
        } else {
            append0(sb.getValue(), 0, sb.length());
        }
        return this;
    }

    public StringBuilder append(char[] chars) {
        append0(chars);
        return this;
    }

    public StringBuilder append(char[] str, int offset, int len) {
        append0(str, offset, len);
        return this;
    }

    @Override
    public StringBuilder append(CharSequence csq) {
        if (csq == null) {
            appendNull();
        } else {
            append0(csq, 0, csq.length());
        }
        return this;
    }

    @Override
    public StringBuilder append(CharSequence csq, int start, int end) {
        append0(csq, start, end);
        return this;
    }

    public StringBuilder appendCodePoint(int codePoint) {
        append0(Character.toChars(codePoint));
        return this;
    }

    public StringBuilder delete(int start, int end) {
        delete0(start, end);
        return this;
    }

    public StringBuilder deleteCharAt(int index) {
        deleteCharAt0(index);
        return this;
    }

    public StringBuilder insert(int offset, boolean b) {
        insert0(offset, b ? "true" : "false");
        return this;
    }

    public StringBuilder insert(int offset, char c) {
        insert0(offset, c);
        return this;
    }

    public StringBuilder insert(int offset, int i) {
        insert0(offset, Integer.toString(i));
        return this;
    }

    public StringBuilder insert(int offset, long l) {
        insert0(offset, Long.toString(l));
        return this;
    }

    public StringBuilder insert(int offset, float f) {
        insert0(offset, Float.toString(f));
        return this;
    }

    public StringBuilder insert(int offset, double d) {
        insert0(offset, Double.toString(d));
        return this;
    }

    public StringBuilder insert(int offset, Object obj) {
        insert0(offset, obj == null ? "null" : obj.toString());
        return this;
    }

    public StringBuilder insert(int offset, String str) {
        insert0(offset, str);
        return this;
    }

    public StringBuilder insert(int offset, char[] ch) {
        insert0(offset, ch);
        return this;
    }

    public StringBuilder insert(int offset, char[] str, int strOffset, int strLen) {
        insert0(offset, str, strOffset, strLen);
        return this;
    }

    public StringBuilder insert(int offset, CharSequence s) {
        insert0(offset, s == null ? "null" : s.toString());
        return this;
    }

    public StringBuilder insert(int offset, CharSequence s, int start, int end) {
        insert0(offset, s, start, end);
        return this;
    }

    public StringBuilder replace(int start, int end, String string) {
        replace0(start, end, string);
        return this;
    }

    public StringBuilder reverse() {
        reverse0();
        return this;
    }

    @Override
    public String toString() {
        return super.toString();
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        int count = in.readInt();
        char[] value = (char[]) in.readObject();
        set(value, count);
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        out.writeInt(length());
        out.writeObject(getValue());
    }
}
