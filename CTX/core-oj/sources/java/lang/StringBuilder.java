package java.lang;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public final class StringBuilder extends AbstractStringBuilder implements Serializable, CharSequence {
    static final long serialVersionUID = 4383685877147921099L;

    @Override
    public AbstractStringBuilder append(char c) {
        append(c);
        return this;
    }

    @Override
    public AbstractStringBuilder append(double d) {
        append(d);
        return this;
    }

    @Override
    public AbstractStringBuilder append(float f) {
        append(f);
        return this;
    }

    @Override
    public AbstractStringBuilder append(int i) {
        append(i);
        return this;
    }

    @Override
    public AbstractStringBuilder append(long j) {
        append(j);
        return this;
    }

    @Override
    public AbstractStringBuilder append(CharSequence charSequence) {
        append(charSequence);
        return this;
    }

    @Override
    public AbstractStringBuilder append(CharSequence charSequence, int i, int i2) {
        append(charSequence, i, i2);
        return this;
    }

    @Override
    public AbstractStringBuilder append(Object obj) {
        append(obj);
        return this;
    }

    @Override
    public AbstractStringBuilder append(String str) {
        append(str);
        return this;
    }

    @Override
    public AbstractStringBuilder append(StringBuffer stringBuffer) {
        append(stringBuffer);
        return this;
    }

    @Override
    public AbstractStringBuilder append(boolean z) {
        append(z);
        return this;
    }

    @Override
    public AbstractStringBuilder append(char[] cArr) {
        append(cArr);
        return this;
    }

    @Override
    public AbstractStringBuilder append(char[] cArr, int i, int i2) {
        append(cArr, i, i2);
        return this;
    }

    @Override
    public Appendable append(char c) throws IOException {
        append(c);
        return this;
    }

    @Override
    public Appendable append(CharSequence charSequence) throws IOException {
        append(charSequence);
        return this;
    }

    @Override
    public Appendable append(CharSequence charSequence, int i, int i2) throws IOException {
        append(charSequence, i, i2);
        return this;
    }

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
        super(16);
    }

    public StringBuilder(int i) {
        super(i);
    }

    public StringBuilder(String str) {
        super(str.length() + 16);
        append(str);
    }

    public StringBuilder(CharSequence charSequence) {
        this(charSequence.length() + 16);
        append(charSequence);
    }

    @Override
    public StringBuilder append(Object obj) {
        append(String.valueOf(obj));
        return this;
    }

    @Override
    public StringBuilder append(String str) {
        super.append(str);
        return this;
    }

    @Override
    public StringBuilder append(StringBuffer stringBuffer) {
        super.append(stringBuffer);
        return this;
    }

    @Override
    public StringBuilder append(CharSequence charSequence) {
        super.append(charSequence);
        return this;
    }

    @Override
    public StringBuilder append(CharSequence charSequence, int i, int i2) {
        super.append(charSequence, i, i2);
        return this;
    }

    @Override
    public StringBuilder append(char[] cArr) {
        super.append(cArr);
        return this;
    }

    @Override
    public StringBuilder append(char[] cArr, int i, int i2) {
        super.append(cArr, i, i2);
        return this;
    }

    @Override
    public StringBuilder append(boolean z) {
        super.append(z);
        return this;
    }

    @Override
    public StringBuilder append(char c) {
        super.append(c);
        return this;
    }

    @Override
    public StringBuilder append(int i) {
        super.append(i);
        return this;
    }

    @Override
    public StringBuilder append(long j) {
        super.append(j);
        return this;
    }

    @Override
    public StringBuilder append(float f) {
        super.append(f);
        return this;
    }

    @Override
    public StringBuilder append(double d) {
        super.append(d);
        return this;
    }

    @Override
    public StringBuilder appendCodePoint(int i) {
        super.appendCodePoint(i);
        return this;
    }

    @Override
    public StringBuilder delete(int i, int i2) {
        super.delete(i, i2);
        return this;
    }

    @Override
    public StringBuilder deleteCharAt(int i) {
        super.deleteCharAt(i);
        return this;
    }

    @Override
    public StringBuilder replace(int i, int i2, String str) {
        super.replace(i, i2, str);
        return this;
    }

    @Override
    public StringBuilder insert(int i, char[] cArr, int i2, int i3) {
        super.insert(i, cArr, i2, i3);
        return this;
    }

    @Override
    public StringBuilder insert(int i, Object obj) {
        super.insert(i, obj);
        return this;
    }

    @Override
    public StringBuilder insert(int i, String str) {
        super.insert(i, str);
        return this;
    }

    @Override
    public StringBuilder insert(int i, char[] cArr) {
        super.insert(i, cArr);
        return this;
    }

    @Override
    public StringBuilder insert(int i, CharSequence charSequence) {
        super.insert(i, charSequence);
        return this;
    }

    @Override
    public StringBuilder insert(int i, CharSequence charSequence, int i2, int i3) {
        super.insert(i, charSequence, i2, i3);
        return this;
    }

    @Override
    public StringBuilder insert(int i, boolean z) {
        super.insert(i, z);
        return this;
    }

    @Override
    public StringBuilder insert(int i, char c) {
        super.insert(i, c);
        return this;
    }

    @Override
    public StringBuilder insert(int i, int i2) {
        super.insert(i, i2);
        return this;
    }

    @Override
    public StringBuilder insert(int i, long j) {
        super.insert(i, j);
        return this;
    }

    @Override
    public StringBuilder insert(int i, float f) {
        super.insert(i, f);
        return this;
    }

    @Override
    public StringBuilder insert(int i, double d) {
        super.insert(i, d);
        return this;
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
    public StringBuilder reverse() {
        super.reverse();
        return this;
    }

    @Override
    public String toString() {
        if (this.count == 0) {
            return "";
        }
        return StringFactory.newStringFromChars(0, this.count, this.value);
    }

    private void writeObject(ObjectOutputStream objectOutputStream) throws IOException {
        objectOutputStream.defaultWriteObject();
        objectOutputStream.writeInt(this.count);
        objectOutputStream.writeObject(this.value);
    }

    private void readObject(ObjectInputStream objectInputStream) throws IOException, ClassNotFoundException {
        objectInputStream.defaultReadObject();
        this.count = objectInputStream.readInt();
        this.value = (char[]) objectInputStream.readObject();
    }
}
