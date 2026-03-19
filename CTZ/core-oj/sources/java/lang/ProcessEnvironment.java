package java.lang;

import java.util.AbstractCollection;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

final class ProcessEnvironment {
    static final int MIN_NAME_LENGTH = 0;
    private static final HashMap<Variable, Value> theEnvironment;
    private static final Map<String, String> theUnmodifiableEnvironment;

    private static native byte[][] environ();

    static {
        byte[][] bArrEnviron = environ();
        theEnvironment = new HashMap<>((bArrEnviron.length / 2) + 3);
        for (int length = bArrEnviron.length - 1; length > 0; length -= 2) {
            theEnvironment.put(Variable.valueOf(bArrEnviron[length - 1]), Value.valueOf(bArrEnviron[length]));
        }
        theUnmodifiableEnvironment = Collections.unmodifiableMap(new StringEnvironment(theEnvironment));
    }

    static String getenv(String str) {
        return theUnmodifiableEnvironment.get(str);
    }

    static Map<String, String> getenv() {
        return theUnmodifiableEnvironment;
    }

    static Map<String, String> environment() {
        return new StringEnvironment((Map) theEnvironment.clone());
    }

    static Map<String, String> emptyEnvironment(int i) {
        return new StringEnvironment(new HashMap(i));
    }

    private ProcessEnvironment() {
    }

    private static void validateVariable(String str) {
        if (str.indexOf(61) != -1 || str.indexOf(0) != -1) {
            throw new IllegalArgumentException("Invalid environment variable name: \"" + str + "\"");
        }
    }

    private static void validateValue(String str) {
        if (str.indexOf(0) != -1) {
            throw new IllegalArgumentException("Invalid environment variable value: \"" + str + "\"");
        }
    }

    private static abstract class ExternalData {
        protected final byte[] bytes;
        protected final String str;

        protected ExternalData(String str, byte[] bArr) {
            this.str = str;
            this.bytes = bArr;
        }

        public byte[] getBytes() {
            return this.bytes;
        }

        public String toString() {
            return this.str;
        }

        public boolean equals(Object obj) {
            return (obj instanceof ExternalData) && ProcessEnvironment.arrayEquals(getBytes(), ((ExternalData) obj).getBytes());
        }

        public int hashCode() {
            return ProcessEnvironment.arrayHash(getBytes());
        }
    }

    private static class Variable extends ExternalData implements Comparable<Variable> {
        protected Variable(String str, byte[] bArr) {
            super(str, bArr);
        }

        public static Variable valueOfQueryOnly(Object obj) {
            return valueOfQueryOnly((String) obj);
        }

        public static Variable valueOfQueryOnly(String str) {
            return new Variable(str, str.getBytes());
        }

        public static Variable valueOf(String str) {
            ProcessEnvironment.validateVariable(str);
            return valueOfQueryOnly(str);
        }

        public static Variable valueOf(byte[] bArr) {
            return new Variable(new String(bArr), bArr);
        }

        @Override
        public int compareTo(Variable variable) {
            return ProcessEnvironment.arrayCompare(getBytes(), variable.getBytes());
        }

        @Override
        public boolean equals(Object obj) {
            return (obj instanceof Variable) && super.equals(obj);
        }
    }

    private static class Value extends ExternalData implements Comparable<Value> {
        protected Value(String str, byte[] bArr) {
            super(str, bArr);
        }

        public static Value valueOfQueryOnly(Object obj) {
            return valueOfQueryOnly((String) obj);
        }

        public static Value valueOfQueryOnly(String str) {
            return new Value(str, str.getBytes());
        }

        public static Value valueOf(String str) {
            ProcessEnvironment.validateValue(str);
            return valueOfQueryOnly(str);
        }

        public static Value valueOf(byte[] bArr) {
            return new Value(new String(bArr), bArr);
        }

        @Override
        public int compareTo(Value value) {
            return ProcessEnvironment.arrayCompare(getBytes(), value.getBytes());
        }

        @Override
        public boolean equals(Object obj) {
            return (obj instanceof Value) && super.equals(obj);
        }
    }

    private static class StringEnvironment extends AbstractMap<String, String> {
        private Map<Variable, Value> m;

        private static String toString(Value value) {
            if (value == null) {
                return null;
            }
            return value.toString();
        }

        public StringEnvironment(Map<Variable, Value> map) {
            this.m = map;
        }

        @Override
        public int size() {
            return this.m.size();
        }

        @Override
        public boolean isEmpty() {
            return this.m.isEmpty();
        }

        @Override
        public void clear() {
            this.m.clear();
        }

        @Override
        public boolean containsKey(Object obj) {
            return this.m.containsKey(Variable.valueOfQueryOnly(obj));
        }

        @Override
        public boolean containsValue(Object obj) {
            return this.m.containsValue(Value.valueOfQueryOnly(obj));
        }

        @Override
        public String get(Object obj) {
            return toString(this.m.get(Variable.valueOfQueryOnly(obj)));
        }

        @Override
        public String put(String str, String str2) {
            return toString(this.m.put(Variable.valueOf(str), Value.valueOf(str2)));
        }

        @Override
        public String remove(Object obj) {
            return toString(this.m.remove(Variable.valueOfQueryOnly(obj)));
        }

        @Override
        public Set<String> keySet() {
            return new StringKeySet(this.m.keySet());
        }

        @Override
        public Set<Map.Entry<String, String>> entrySet() {
            return new StringEntrySet(this.m.entrySet());
        }

        @Override
        public Collection<String> values() {
            return new StringValues(this.m.values());
        }

        public byte[] toEnvironmentBlock(int[] iArr) {
            int size = this.m.size() * 2;
            for (Map.Entry<Variable, Value> entry : this.m.entrySet()) {
                size = size + entry.getKey().getBytes().length + entry.getValue().getBytes().length;
            }
            byte[] bArr = new byte[size];
            int length = 0;
            for (Map.Entry<Variable, Value> entry2 : this.m.entrySet()) {
                byte[] bytes = entry2.getKey().getBytes();
                byte[] bytes2 = entry2.getValue().getBytes();
                System.arraycopy(bytes, 0, bArr, length, bytes.length);
                int length2 = length + bytes.length;
                int i = length2 + 1;
                bArr[length2] = 61;
                System.arraycopy(bytes2, 0, bArr, i, bytes2.length);
                length = bytes2.length + 1 + i;
            }
            iArr[0] = this.m.size();
            return bArr;
        }
    }

    static byte[] toEnvironmentBlock(Map<String, String> map, int[] iArr) {
        if (map == null) {
            return null;
        }
        return ((StringEnvironment) map).toEnvironmentBlock(iArr);
    }

    private static class StringEntry implements Map.Entry<String, String> {
        private final Map.Entry<Variable, Value> e;

        public StringEntry(Map.Entry<Variable, Value> entry) {
            this.e = entry;
        }

        @Override
        public String getKey() {
            return this.e.getKey().toString();
        }

        @Override
        public String getValue() {
            return this.e.getValue().toString();
        }

        @Override
        public String setValue(String str) {
            return this.e.setValue(Value.valueOf(str)).toString();
        }

        public String toString() {
            return getKey() + "=" + getValue();
        }

        @Override
        public boolean equals(Object obj) {
            return (obj instanceof StringEntry) && this.e.equals(((StringEntry) obj).e);
        }

        @Override
        public int hashCode() {
            return this.e.hashCode();
        }
    }

    private static class StringEntrySet extends AbstractSet<Map.Entry<String, String>> {
        private final Set<Map.Entry<Variable, Value>> s;

        public StringEntrySet(Set<Map.Entry<Variable, Value>> set) {
            this.s = set;
        }

        @Override
        public int size() {
            return this.s.size();
        }

        @Override
        public boolean isEmpty() {
            return this.s.isEmpty();
        }

        @Override
        public void clear() {
            this.s.clear();
        }

        @Override
        public Iterator<Map.Entry<String, String>> iterator() {
            return new Iterator<Map.Entry<String, String>>() {
                Iterator<Map.Entry<Variable, Value>> i;

                {
                    this.i = StringEntrySet.this.s.iterator();
                }

                @Override
                public boolean hasNext() {
                    return this.i.hasNext();
                }

                @Override
                public Map.Entry<String, String> next() {
                    return new StringEntry(this.i.next());
                }

                @Override
                public void remove() {
                    this.i.remove();
                }
            };
        }

        private static Map.Entry<Variable, Value> vvEntry(final Object obj) {
            if (obj instanceof StringEntry) {
                return ((StringEntry) obj).e;
            }
            return new Map.Entry<Variable, Value>() {
                @Override
                public Variable getKey() {
                    return Variable.valueOfQueryOnly(((Map.Entry) obj).getKey());
                }

                @Override
                public Value getValue() {
                    return Value.valueOfQueryOnly(((Map.Entry) obj).getValue());
                }

                @Override
                public Value setValue(Value value) {
                    throw new UnsupportedOperationException();
                }
            };
        }

        @Override
        public boolean contains(Object obj) {
            return this.s.contains(vvEntry(obj));
        }

        @Override
        public boolean remove(Object obj) {
            return this.s.remove(vvEntry(obj));
        }

        @Override
        public boolean equals(Object obj) {
            return (obj instanceof StringEntrySet) && this.s.equals(((StringEntrySet) obj).s);
        }

        @Override
        public int hashCode() {
            return this.s.hashCode();
        }
    }

    private static class StringValues extends AbstractCollection<String> {
        private final Collection<Value> c;

        public StringValues(Collection<Value> collection) {
            this.c = collection;
        }

        @Override
        public int size() {
            return this.c.size();
        }

        @Override
        public boolean isEmpty() {
            return this.c.isEmpty();
        }

        @Override
        public void clear() {
            this.c.clear();
        }

        @Override
        public Iterator<String> iterator() {
            return new Iterator<String>() {
                Iterator<Value> i;

                {
                    this.i = StringValues.this.c.iterator();
                }

                @Override
                public boolean hasNext() {
                    return this.i.hasNext();
                }

                @Override
                public String next() {
                    return this.i.next().toString();
                }

                @Override
                public void remove() {
                    this.i.remove();
                }
            };
        }

        @Override
        public boolean contains(Object obj) {
            return this.c.contains(Value.valueOfQueryOnly(obj));
        }

        @Override
        public boolean remove(Object obj) {
            return this.c.remove(Value.valueOfQueryOnly(obj));
        }

        @Override
        public boolean equals(Object obj) {
            return (obj instanceof StringValues) && this.c.equals(((StringValues) obj).c);
        }

        @Override
        public int hashCode() {
            return this.c.hashCode();
        }
    }

    private static class StringKeySet extends AbstractSet<String> {
        private final Set<Variable> s;

        public StringKeySet(Set<Variable> set) {
            this.s = set;
        }

        @Override
        public int size() {
            return this.s.size();
        }

        @Override
        public boolean isEmpty() {
            return this.s.isEmpty();
        }

        @Override
        public void clear() {
            this.s.clear();
        }

        @Override
        public Iterator<String> iterator() {
            return new Iterator<String>() {
                Iterator<Variable> i;

                {
                    this.i = StringKeySet.this.s.iterator();
                }

                @Override
                public boolean hasNext() {
                    return this.i.hasNext();
                }

                @Override
                public String next() {
                    return this.i.next().toString();
                }

                @Override
                public void remove() {
                    this.i.remove();
                }
            };
        }

        @Override
        public boolean contains(Object obj) {
            return this.s.contains(Variable.valueOfQueryOnly(obj));
        }

        @Override
        public boolean remove(Object obj) {
            return this.s.remove(Variable.valueOfQueryOnly(obj));
        }
    }

    private static int arrayCompare(byte[] bArr, byte[] bArr2) {
        int length = bArr.length < bArr2.length ? bArr.length : bArr2.length;
        for (int i = 0; i < length; i++) {
            if (bArr[i] != bArr2[i]) {
                return bArr[i] - bArr2[i];
            }
        }
        return bArr.length - bArr2.length;
    }

    private static boolean arrayEquals(byte[] bArr, byte[] bArr2) {
        if (bArr.length != bArr2.length) {
            return false;
        }
        for (int i = 0; i < bArr.length; i++) {
            if (bArr[i] != bArr2[i]) {
                return false;
            }
        }
        return true;
    }

    private static int arrayHash(byte[] bArr) {
        int i = 0;
        for (byte b : bArr) {
            i = b + (31 * i);
        }
        return i;
    }
}
