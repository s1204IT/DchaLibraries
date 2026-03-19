package android.icu.impl;

import java.nio.ByteBuffer;

public final class UResource {

    public interface Array {
        int getSize();

        boolean getValue(int i, Value value);
    }

    public static abstract class Sink {
        public abstract void put(Key key, Value value, boolean z);
    }

    public interface Table {
        boolean getKeyAndValue(int i, Key key, Value value);

        int getSize();
    }

    public static final class Key implements CharSequence, Cloneable, Comparable<Key> {
        static final boolean $assertionsDisabled = false;
        private byte[] bytes;
        private int length;
        private int offset;
        private String s;

        public Key() {
            this.s = "";
        }

        public Key(String str) {
            setString(str);
        }

        private Key(byte[] bArr, int i, int i2) {
            this.bytes = bArr;
            this.offset = i;
            this.length = i2;
        }

        public Key setBytes(byte[] bArr, int i) {
            this.bytes = bArr;
            this.offset = i;
            int i2 = 0;
            while (true) {
                this.length = i2;
                if (bArr[this.length + i] == 0) {
                    this.s = null;
                    return this;
                }
                i2 = this.length + 1;
            }
        }

        public Key setToEmpty() {
            this.bytes = null;
            this.length = 0;
            this.offset = 0;
            this.s = "";
            return this;
        }

        public Key setString(String str) {
            if (str.isEmpty()) {
                setToEmpty();
            } else {
                this.bytes = new byte[str.length()];
                this.offset = 0;
                this.length = str.length();
                for (int i = 0; i < this.length; i++) {
                    char cCharAt = str.charAt(i);
                    if (cCharAt <= 127) {
                        this.bytes[i] = (byte) cCharAt;
                    } else {
                        throw new IllegalArgumentException('\"' + str + "\" is not an ASCII string");
                    }
                }
                this.s = str;
            }
            return this;
        }

        public Key m0clone() {
            try {
                return (Key) super.clone();
            } catch (CloneNotSupportedException e) {
                return null;
            }
        }

        @Override
        public char charAt(int i) {
            return (char) this.bytes[this.offset + i];
        }

        @Override
        public int length() {
            return this.length;
        }

        @Override
        public Key subSequence(int i, int i2) {
            return new Key(this.bytes, this.offset + i, i2 - i);
        }

        @Override
        public String toString() {
            if (this.s == null) {
                this.s = internalSubString(0, this.length);
            }
            return this.s;
        }

        private String internalSubString(int i, int i2) {
            StringBuilder sb = new StringBuilder(i2 - i);
            while (i < i2) {
                sb.append((char) this.bytes[this.offset + i]);
                i++;
            }
            return sb.toString();
        }

        public String substring(int i) {
            return internalSubString(i, this.length);
        }

        public String substring(int i, int i2) {
            return internalSubString(i, i2);
        }

        private boolean regionMatches(byte[] bArr, int i, int i2) {
            for (int i3 = 0; i3 < i2; i3++) {
                if (this.bytes[this.offset + i3] != bArr[i + i3]) {
                    return false;
                }
            }
            return true;
        }

        private boolean regionMatches(int i, CharSequence charSequence, int i2) {
            for (int i3 = 0; i3 < i2; i3++) {
                if (this.bytes[this.offset + i + i3] != charSequence.charAt(i3)) {
                    return false;
                }
            }
            return true;
        }

        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof Key)) {
                return false;
            }
            Key key = (Key) obj;
            if (this.length != key.length || !regionMatches(key.bytes, key.offset, this.length)) {
                return false;
            }
            return true;
        }

        public boolean contentEquals(CharSequence charSequence) {
            if (charSequence == null) {
                return false;
            }
            return this == charSequence || (charSequence.length() == this.length && regionMatches(0, charSequence, this.length));
        }

        public boolean startsWith(CharSequence charSequence) {
            int length = charSequence.length();
            return length <= this.length && regionMatches(0, charSequence, length);
        }

        public boolean endsWith(CharSequence charSequence) {
            int length = charSequence.length();
            return length <= this.length && regionMatches(this.length - length, charSequence, length);
        }

        public boolean regionMatches(int i, CharSequence charSequence) {
            int length = charSequence.length();
            return length == this.length - i && regionMatches(i, charSequence, length);
        }

        public int hashCode() {
            if (this.length == 0) {
                return 0;
            }
            int i = this.bytes[this.offset];
            for (int i2 = 1; i2 < this.length; i2++) {
                i = this.bytes[this.offset] + (37 * i);
            }
            return i;
        }

        @Override
        public int compareTo(Key key) {
            return compareTo((CharSequence) key);
        }

        public int compareTo(CharSequence charSequence) {
            int length = charSequence.length();
            int i = this.length <= length ? this.length : length;
            for (int i2 = 0; i2 < i; i2++) {
                int iCharAt = charAt(i2) - charSequence.charAt(i2);
                if (iCharAt != 0) {
                    return iCharAt;
                }
            }
            return this.length - length;
        }
    }

    public static abstract class Value {
        public abstract String getAliasString();

        public abstract Array getArray();

        public abstract ByteBuffer getBinary();

        public abstract int getInt();

        public abstract int[] getIntVector();

        public abstract String getString();

        public abstract String[] getStringArray();

        public abstract String[] getStringArrayOrStringAsArray();

        public abstract String getStringOrFirstOfArray();

        public abstract Table getTable();

        public abstract int getType();

        public abstract int getUInt();

        public abstract boolean isNoInheritanceMarker();

        protected Value() {
        }

        public String toString() {
            int type = getType();
            if (type != 14) {
                switch (type) {
                    case 0:
                        return getString();
                    case 1:
                        return "(binary blob)";
                    case 2:
                        return "(table)";
                    default:
                        switch (type) {
                            case 7:
                                return Integer.toString(getInt());
                            case 8:
                                return "(array)";
                            default:
                                return "???";
                        }
                }
            }
            int[] intVector = getIntVector();
            StringBuilder sb = new StringBuilder("[");
            sb.append(intVector.length);
            sb.append("]{");
            if (intVector.length != 0) {
                sb.append(intVector[0]);
                for (int i = 1; i < intVector.length; i++) {
                    sb.append(", ");
                    sb.append(intVector[i]);
                }
            }
            sb.append('}');
            return sb.toString();
        }
    }
}
