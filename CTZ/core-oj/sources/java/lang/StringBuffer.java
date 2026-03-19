package java.lang;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamField;
import java.io.Serializable;
import java.util.Arrays;

public final class StringBuffer extends AbstractStringBuilder implements Serializable, CharSequence {
    private static final ObjectStreamField[] serialPersistentFields = {new ObjectStreamField("value", char[].class), new ObjectStreamField("count", Integer.TYPE), new ObjectStreamField("shared", Boolean.TYPE)};
    static final long serialVersionUID = 3388685877147921107L;
    private transient char[] toStringCache;

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

    public StringBuffer() {
        super(16);
    }

    public StringBuffer(int i) {
        super(i);
    }

    public StringBuffer(String str) {
        super(str.length() + 16);
        append(str);
    }

    public StringBuffer(CharSequence charSequence) {
        this(charSequence.length() + 16);
        append(charSequence);
    }

    @Override
    public synchronized int length() {
        return this.count;
    }

    @Override
    public synchronized int capacity() {
        return this.value.length;
    }

    @Override
    public synchronized void ensureCapacity(int i) {
        super.ensureCapacity(i);
    }

    @Override
    public synchronized void trimToSize() {
        super.trimToSize();
    }

    @Override
    public synchronized void setLength(int i) {
        this.toStringCache = null;
        super.setLength(i);
    }

    @Override
    public synchronized char charAt(int i) {
        if (i >= 0) {
            if (i < this.count) {
            }
        }
        throw new StringIndexOutOfBoundsException(i);
        return this.value[i];
    }

    @Override
    public synchronized int codePointAt(int i) {
        return super.codePointAt(i);
    }

    @Override
    public synchronized int codePointBefore(int i) {
        return super.codePointBefore(i);
    }

    @Override
    public synchronized int codePointCount(int i, int i2) {
        return super.codePointCount(i, i2);
    }

    @Override
    public synchronized int offsetByCodePoints(int i, int i2) {
        return super.offsetByCodePoints(i, i2);
    }

    @Override
    public synchronized void getChars(int i, int i2, char[] cArr, int i3) {
        super.getChars(i, i2, cArr, i3);
    }

    @Override
    public synchronized void setCharAt(int i, char c) {
        if (i >= 0) {
            if (i < this.count) {
                this.toStringCache = null;
                this.value[i] = c;
            }
        }
        throw new StringIndexOutOfBoundsException(i);
    }

    @Override
    public synchronized StringBuffer append(Object obj) {
        this.toStringCache = null;
        super.append(String.valueOf(obj));
        return this;
    }

    @Override
    public synchronized StringBuffer append(String str) {
        this.toStringCache = null;
        super.append(str);
        return this;
    }

    @Override
    public synchronized StringBuffer append(StringBuffer stringBuffer) {
        this.toStringCache = null;
        super.append(stringBuffer);
        return this;
    }

    @Override
    synchronized StringBuffer append(AbstractStringBuilder abstractStringBuilder) {
        this.toStringCache = null;
        super.append(abstractStringBuilder);
        return this;
    }

    @Override
    public synchronized StringBuffer append(CharSequence charSequence) {
        this.toStringCache = null;
        super.append(charSequence);
        return this;
    }

    @Override
    public synchronized StringBuffer append(CharSequence charSequence, int i, int i2) {
        this.toStringCache = null;
        super.append(charSequence, i, i2);
        return this;
    }

    @Override
    public synchronized StringBuffer append(char[] cArr) {
        this.toStringCache = null;
        super.append(cArr);
        return this;
    }

    @Override
    public synchronized StringBuffer append(char[] cArr, int i, int i2) {
        this.toStringCache = null;
        super.append(cArr, i, i2);
        return this;
    }

    @Override
    public synchronized StringBuffer append(boolean z) {
        this.toStringCache = null;
        super.append(z);
        return this;
    }

    @Override
    public synchronized StringBuffer append(char c) {
        this.toStringCache = null;
        super.append(c);
        return this;
    }

    @Override
    public synchronized StringBuffer append(int i) {
        this.toStringCache = null;
        super.append(i);
        return this;
    }

    @Override
    public synchronized StringBuffer appendCodePoint(int i) {
        this.toStringCache = null;
        super.appendCodePoint(i);
        return this;
    }

    @Override
    public synchronized StringBuffer append(long j) {
        this.toStringCache = null;
        super.append(j);
        return this;
    }

    @Override
    public synchronized StringBuffer append(float f) {
        this.toStringCache = null;
        super.append(f);
        return this;
    }

    @Override
    public synchronized StringBuffer append(double d) {
        this.toStringCache = null;
        super.append(d);
        return this;
    }

    @Override
    public synchronized StringBuffer delete(int i, int i2) {
        this.toStringCache = null;
        super.delete(i, i2);
        return this;
    }

    @Override
    public synchronized StringBuffer deleteCharAt(int i) {
        this.toStringCache = null;
        super.deleteCharAt(i);
        return this;
    }

    @Override
    public synchronized StringBuffer replace(int i, int i2, String str) {
        this.toStringCache = null;
        super.replace(i, i2, str);
        return this;
    }

    @Override
    public synchronized String substring(int i) {
        return substring(i, this.count);
    }

    @Override
    public synchronized CharSequence subSequence(int i, int i2) {
        return super.substring(i, i2);
    }

    @Override
    public synchronized String substring(int i, int i2) {
        return super.substring(i, i2);
    }

    @Override
    public synchronized StringBuffer insert(int i, char[] cArr, int i2, int i3) {
        this.toStringCache = null;
        super.insert(i, cArr, i2, i3);
        return this;
    }

    @Override
    public synchronized StringBuffer insert(int i, Object obj) {
        this.toStringCache = null;
        super.insert(i, String.valueOf(obj));
        return this;
    }

    @Override
    public synchronized StringBuffer insert(int i, String str) {
        this.toStringCache = null;
        super.insert(i, str);
        return this;
    }

    @Override
    public synchronized StringBuffer insert(int i, char[] cArr) {
        this.toStringCache = null;
        super.insert(i, cArr);
        return this;
    }

    @Override
    public StringBuffer insert(int i, CharSequence charSequence) {
        super.insert(i, charSequence);
        return this;
    }

    @Override
    public synchronized StringBuffer insert(int i, CharSequence charSequence, int i2, int i3) {
        this.toStringCache = null;
        super.insert(i, charSequence, i2, i3);
        return this;
    }

    @Override
    public StringBuffer insert(int i, boolean z) {
        super.insert(i, z);
        return this;
    }

    @Override
    public synchronized StringBuffer insert(int i, char c) {
        this.toStringCache = null;
        super.insert(i, c);
        return this;
    }

    @Override
    public StringBuffer insert(int i, int i2) {
        super.insert(i, i2);
        return this;
    }

    @Override
    public StringBuffer insert(int i, long j) {
        super.insert(i, j);
        return this;
    }

    @Override
    public StringBuffer insert(int i, float f) {
        super.insert(i, f);
        return this;
    }

    @Override
    public StringBuffer insert(int i, double d) {
        super.insert(i, d);
        return this;
    }

    @Override
    public int indexOf(String str) {
        return super.indexOf(str);
    }

    @Override
    public synchronized int indexOf(String str, int i) {
        return super.indexOf(str, i);
    }

    @Override
    public int lastIndexOf(String str) {
        return lastIndexOf(str, this.count);
    }

    @Override
    public synchronized int lastIndexOf(String str, int i) {
        return super.lastIndexOf(str, i);
    }

    @Override
    public synchronized StringBuffer reverse() {
        this.toStringCache = null;
        super.reverse();
        return this;
    }

    @Override
    public synchronized String toString() {
        if (this.toStringCache == null) {
            this.toStringCache = Arrays.copyOfRange(this.value, 0, this.count);
        }
        return new String(this.toStringCache, 0, this.count);
    }

    private synchronized void writeObject(ObjectOutputStream objectOutputStream) throws IOException {
        ObjectOutputStream.PutField putFieldPutFields = objectOutputStream.putFields();
        putFieldPutFields.put("value", this.value);
        putFieldPutFields.put("count", this.count);
        putFieldPutFields.put("shared", false);
        objectOutputStream.writeFields();
    }

    private void readObject(ObjectInputStream objectInputStream) throws IOException, ClassNotFoundException {
        ObjectInputStream.GetField fields = objectInputStream.readFields();
        this.value = (char[]) fields.get("value", (Object) null);
        this.count = fields.get("count", 0);
    }
}
