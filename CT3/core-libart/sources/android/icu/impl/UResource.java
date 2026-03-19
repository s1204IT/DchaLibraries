package android.icu.impl;

import java.nio.ByteBuffer;

public final class UResource {

    public static final class Key implements CharSequence, Cloneable, Comparable<Key> {

        static final boolean f26assertionsDisabled;
        private byte[] bytes;
        private int length;
        private int offset;
        private String s;

        static {
            f26assertionsDisabled = !Key.class.desiredAssertionStatus();
        }

        public Key() {
        }

        private Key(byte[] keyBytes, int keyOffset, int keyLength) {
            this.bytes = keyBytes;
            this.offset = keyOffset;
            this.length = keyLength;
        }

        public void setBytes(byte[] keyBytes, int keyOffset) {
            this.bytes = keyBytes;
            this.offset = keyOffset;
            this.length = 0;
            while (keyBytes[this.length + keyOffset] != 0) {
                this.length++;
            }
            this.s = null;
        }

        public void setToEmpty() {
            this.bytes = null;
            this.length = 0;
            this.offset = 0;
            this.s = null;
        }

        public Key m69clone() {
            try {
                return (Key) super.clone();
            } catch (CloneNotSupportedException e) {
                return null;
            }
        }

        @Override
        public char charAt(int i) {
            boolean z = false;
            if (!f26assertionsDisabled) {
                if (i >= 0 && i < this.length) {
                    z = true;
                }
                if (!z) {
                    throw new AssertionError();
                }
            }
            return (char) this.bytes[this.offset + i];
        }

        @Override
        public int length() {
            return this.length;
        }

        @Override
        public Key subSequence(int start, int end) {
            if (!f26assertionsDisabled) {
                if (!(start >= 0 && start < this.length)) {
                    throw new AssertionError();
                }
            }
            if (!f26assertionsDisabled) {
                if (!(start <= end && end <= this.length)) {
                    throw new AssertionError();
                }
            }
            return new Key(this.bytes, this.offset + start, end - start);
        }

        @Override
        public String toString() {
            if (this.s == null) {
                this.s = internalSubString(0, this.length);
            }
            return this.s;
        }

        private String internalSubString(int start, int end) {
            StringBuilder sb = new StringBuilder(end - start);
            for (int i = start; i < end; i++) {
                sb.append((char) this.bytes[this.offset + i]);
            }
            return sb.toString();
        }

        public String substring(int start) {
            boolean z = false;
            if (!f26assertionsDisabled) {
                if (start >= 0 && start < this.length) {
                    z = true;
                }
                if (!z) {
                    throw new AssertionError();
                }
            }
            return internalSubString(start, this.length);
        }

        public String substring(int start, int end) {
            if (!f26assertionsDisabled) {
                if (!(start >= 0 && start < this.length)) {
                    throw new AssertionError();
                }
            }
            if (!f26assertionsDisabled) {
                if (!(start <= end && end <= this.length)) {
                    throw new AssertionError();
                }
            }
            return internalSubString(start, end);
        }

        private boolean regionMatches(byte[] otherBytes, int otherOffset, int n) {
            for (int i = 0; i < n; i++) {
                if (this.bytes[this.offset + i] != otherBytes[otherOffset + i]) {
                    return false;
                }
            }
            return true;
        }

        private boolean regionMatches(int start, CharSequence cs, int n) {
            for (int i = 0; i < n; i++) {
                if (this.bytes[this.offset + start + i] != cs.charAt(i)) {
                    return false;
                }
            }
            return true;
        }

        public boolean equals(Object other) {
            if (other == null) {
                return false;
            }
            if (this == other) {
                return true;
            }
            if (!(other instanceof Key)) {
                return false;
            }
            Key otherKey = (Key) other;
            if (this.length == otherKey.length) {
                return regionMatches(otherKey.bytes, otherKey.offset, this.length);
            }
            return false;
        }

        public boolean contentEquals(CharSequence cs) {
            if (cs == null) {
                return false;
            }
            if (this == cs) {
                return true;
            }
            if (cs.length() == this.length) {
                return regionMatches(0, cs, this.length);
            }
            return false;
        }

        public boolean startsWith(CharSequence cs) {
            int csLength = cs.length();
            if (csLength <= this.length) {
                return regionMatches(0, cs, csLength);
            }
            return false;
        }

        public boolean endsWith(CharSequence cs) {
            int csLength = cs.length();
            if (csLength <= this.length) {
                return regionMatches(this.length - csLength, cs, csLength);
            }
            return false;
        }

        public boolean regionMatches(int start, CharSequence cs) {
            int csLength = cs.length();
            if (csLength == this.length - start) {
                return regionMatches(start, cs, csLength);
            }
            return false;
        }

        public int hashCode() {
            if (this.length == 0) {
                return 0;
            }
            int i = this.bytes[this.offset];
            for (int i2 = 1; i2 < this.length; i2++) {
                i = (i * 37) + this.bytes[this.offset];
            }
            return i;
        }

        @Override
        public int compareTo(Key other) {
            return compareTo((CharSequence) other);
        }

        public int compareTo(CharSequence cs) {
            int csLength = cs.length();
            int minLength = this.length <= csLength ? this.length : csLength;
            for (int i = 0; i < minLength; i++) {
                int diff = charAt(i) - cs.charAt(i);
                if (diff != 0) {
                    return diff;
                }
            }
            return this.length - csLength;
        }
    }

    public static abstract class Value {
        public abstract String getAliasString();

        public abstract ByteBuffer getBinary();

        public abstract int getInt();

        public abstract int[] getIntVector();

        public abstract String getString();

        public abstract int getType();

        public abstract int getUInt();

        protected Value() {
        }

        public String toString() {
            switch (getType()) {
                case 0:
                    return getString();
                case 1:
                    return "(binary blob)";
                case 2:
                    return "(table)";
                case 7:
                    return Integer.toString(getInt());
                case 8:
                    return "(array)";
                case 14:
                    int[] iv = getIntVector();
                    StringBuilder sb = new StringBuilder("[");
                    sb.append(iv.length).append("]{");
                    if (iv.length != 0) {
                        sb.append(iv[0]);
                        for (int i = 1; i < iv.length; i++) {
                            sb.append(", ").append(iv[i]);
                        }
                    }
                    return sb.append('}').toString();
                default:
                    return "???";
            }
        }
    }

    public static class ArraySink {
        public void put(int index, Value value) {
        }

        public ArraySink getOrCreateArraySink(int index, int size) {
            return null;
        }

        public TableSink getOrCreateTableSink(int index, int initialSize) {
            return null;
        }

        public void leave() {
        }
    }

    public static class TableSink {
        public void put(Key key, Value value) {
        }

        public void putNoFallback(Key key) {
        }

        public ArraySink getOrCreateArraySink(Key key, int size) {
            return null;
        }

        public TableSink getOrCreateTableSink(Key key, int initialSize) {
            return null;
        }

        public void leave() {
        }
    }
}
