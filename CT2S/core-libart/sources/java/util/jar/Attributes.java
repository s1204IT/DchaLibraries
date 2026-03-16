package java.util.jar;

import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class Attributes implements Cloneable, Map<Object, Object> {
    protected Map<Object, Object> map;

    public static class Name {
        private final String name;
        public static final Name CLASS_PATH = new Name("Class-Path");
        public static final Name MANIFEST_VERSION = new Name("Manifest-Version");
        public static final Name MAIN_CLASS = new Name("Main-Class");
        public static final Name SIGNATURE_VERSION = new Name("Signature-Version");
        public static final Name CONTENT_TYPE = new Name("Content-Type");
        public static final Name SEALED = new Name("Sealed");
        public static final Name IMPLEMENTATION_TITLE = new Name("Implementation-Title");
        public static final Name IMPLEMENTATION_VERSION = new Name("Implementation-Version");
        public static final Name IMPLEMENTATION_VENDOR = new Name("Implementation-Vendor");
        public static final Name SPECIFICATION_TITLE = new Name("Specification-Title");
        public static final Name SPECIFICATION_VERSION = new Name("Specification-Version");
        public static final Name SPECIFICATION_VENDOR = new Name("Specification-Vendor");
        public static final Name EXTENSION_LIST = new Name("Extension-List");
        public static final Name EXTENSION_NAME = new Name("Extension-Name");
        public static final Name EXTENSION_INSTALLATION = new Name("Extension-Installation");
        public static final Name IMPLEMENTATION_VENDOR_ID = new Name("Implementation-Vendor-Id");
        public static final Name IMPLEMENTATION_URL = new Name("Implementation-URL");
        public static final Name NAME = new Name("Name");

        public Name(String name) {
            if (name.isEmpty() || name.length() > 70) {
                throw new IllegalArgumentException(name);
            }
            for (int i = 0; i < name.length(); i++) {
                char ch = name.charAt(i);
                if ((ch < 'a' || ch > 'z') && ((ch < 'A' || ch > 'Z') && ch != '_' && ch != '-' && (ch < '0' || ch > '9'))) {
                    throw new IllegalArgumentException(name);
                }
            }
            this.name = name;
        }

        String getName() {
            return this.name;
        }

        public boolean equals(Object object) {
            return (object instanceof Name) && ((Name) object).name.equalsIgnoreCase(this.name);
        }

        public int hashCode() {
            return this.name.toLowerCase(Locale.US).hashCode();
        }

        public String toString() {
            return this.name;
        }
    }

    public Attributes() {
        this.map = new HashMap();
    }

    public Attributes(Attributes attrib) {
        this.map = (Map) ((HashMap) attrib.map).clone();
    }

    public Attributes(int size) {
        this.map = new HashMap(size);
    }

    @Override
    public void clear() {
        this.map.clear();
    }

    @Override
    public boolean containsKey(Object key) {
        return this.map.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return this.map.containsValue(value);
    }

    @Override
    public Set<Map.Entry<Object, Object>> entrySet() {
        return this.map.entrySet();
    }

    @Override
    public Object get(Object key) {
        return this.map.get(key);
    }

    @Override
    public boolean isEmpty() {
        return this.map.isEmpty();
    }

    @Override
    public Set<Object> keySet() {
        return this.map.keySet();
    }

    @Override
    public Object put(Object key, Object value) {
        return this.map.put((Name) key, (String) value);
    }

    @Override
    public void putAll(Map<?, ?> attrib) {
        if (attrib == null) {
            throw new NullPointerException("attrib == null");
        }
        if (!(attrib instanceof Attributes)) {
            throw new ClassCastException(attrib.getClass().getName() + " not an Attributes");
        }
        this.map.putAll(attrib);
    }

    @Override
    public Object remove(Object key) {
        return this.map.remove(key);
    }

    @Override
    public int size() {
        return this.map.size();
    }

    @Override
    public Collection<Object> values() {
        return this.map.values();
    }

    public Object clone() {
        try {
            Attributes clone = (Attributes) super.clone();
            clone.map = (Map) ((HashMap) this.map).clone();
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public int hashCode() {
        return this.map.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof Attributes) {
            return this.map.equals(((Attributes) obj).map);
        }
        return false;
    }

    public String getValue(Name name) {
        return (String) this.map.get(name);
    }

    public String getValue(String name) {
        return getValue(new Name(name));
    }

    public String putValue(String name, String value) {
        return (String) this.map.put(new Name(name), value);
    }
}
