package java.util.jar;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.jar.Manifest;
import sun.misc.ASCIICaseInsensitiveComparator;
import sun.util.logging.PlatformLogger;

public class Attributes implements Map<Object, Object>, Cloneable {
    protected Map<Object, Object> map;

    public Attributes() {
        this(11);
    }

    public Attributes(int i) {
        this.map = new HashMap(i);
    }

    public Attributes(Attributes attributes) {
        this.map = new HashMap(attributes);
    }

    @Override
    public Object get(Object obj) {
        return this.map.get(obj);
    }

    public String getValue(String str) {
        return (String) get(new Name(str));
    }

    public String getValue(Name name) {
        return (String) get(name);
    }

    @Override
    public Object put(Object obj, Object obj2) {
        return this.map.put((Name) obj, (String) obj2);
    }

    public String putValue(String str, String str2) {
        return (String) put(new Name(str), str2);
    }

    @Override
    public Object remove(Object obj) {
        return this.map.remove(obj);
    }

    @Override
    public boolean containsValue(Object obj) {
        return this.map.containsValue(obj);
    }

    @Override
    public boolean containsKey(Object obj) {
        return this.map.containsKey(obj);
    }

    @Override
    public void putAll(Map<?, ?> map) {
        if (!Attributes.class.isInstance(map)) {
            throw new ClassCastException();
        }
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public void clear() {
        this.map.clear();
    }

    @Override
    public int size() {
        return this.map.size();
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
    public Collection<Object> values() {
        return this.map.values();
    }

    @Override
    public Set<Map.Entry<Object, Object>> entrySet() {
        return this.map.entrySet();
    }

    @Override
    public boolean equals(Object obj) {
        return this.map.equals(obj);
    }

    @Override
    public int hashCode() {
        return this.map.hashCode();
    }

    public Object clone() {
        return new Attributes(this);
    }

    void write(DataOutputStream dataOutputStream) throws IOException {
        for (Map.Entry<Object, Object> entry : entrySet()) {
            StringBuffer stringBuffer = new StringBuffer(((Name) entry.getKey()).toString());
            stringBuffer.append(": ");
            String str = (String) entry.getValue();
            if (str != null) {
                byte[] bytes = str.getBytes("UTF8");
                str = new String(bytes, 0, 0, bytes.length);
            }
            stringBuffer.append(str);
            stringBuffer.append("\r\n");
            Manifest.make72Safe(stringBuffer);
            dataOutputStream.writeBytes(stringBuffer.toString());
        }
        dataOutputStream.writeBytes("\r\n");
    }

    void writeMain(DataOutputStream dataOutputStream) throws IOException {
        String string = Name.MANIFEST_VERSION.toString();
        String value = getValue(string);
        if (value == null) {
            string = Name.SIGNATURE_VERSION.toString();
            value = getValue(string);
        }
        if (value != null) {
            dataOutputStream.writeBytes(string + ": " + value + "\r\n");
        }
        for (Map.Entry<Object, Object> entry : entrySet()) {
            String string2 = ((Name) entry.getKey()).toString();
            if (value != null && !string2.equalsIgnoreCase(string)) {
                StringBuffer stringBuffer = new StringBuffer(string2);
                stringBuffer.append(": ");
                String str = (String) entry.getValue();
                if (str != null) {
                    byte[] bytes = str.getBytes("UTF8");
                    str = new String(bytes, 0, 0, bytes.length);
                }
                stringBuffer.append(str);
                stringBuffer.append("\r\n");
                Manifest.make72Safe(stringBuffer);
                dataOutputStream.writeBytes(stringBuffer.toString());
            }
        }
        dataOutputStream.writeBytes("\r\n");
    }

    void read(Manifest.FastInputStream fastInputStream, byte[] bArr) throws IOException {
        String str;
        byte[] bArr2;
        String str2 = null;
        byte[] bArr3 = null;
        while (true) {
            int line = fastInputStream.readLine(bArr);
            if (line == -1) {
                return;
            }
            int i = line - 1;
            if (bArr[i] != 10) {
                throw new IOException("line too long");
            }
            if (i > 0 && bArr[i - 1] == 13) {
                i--;
            }
            if (i == 0) {
                return;
            }
            boolean z = false;
            if (bArr[0] != 32) {
                int i2 = 0;
                while (true) {
                    int i3 = i2 + 1;
                    if (bArr[i2] == 58) {
                        int i4 = i3 + 1;
                        if (bArr[i3] != 32) {
                            throw new IOException("invalid header field");
                        }
                        String str3 = new String(bArr, 0, 0, i4 - 2);
                        if (fastInputStream.peek() == 32) {
                            int i5 = i - i4;
                            bArr3 = new byte[i5];
                            System.arraycopy(bArr, i4, bArr3, 0, i5);
                            str2 = str3;
                        } else {
                            String str4 = new String(bArr, i4, i - i4, "UTF8");
                            bArr2 = bArr3;
                            str2 = str3;
                            str = str4;
                        }
                    } else {
                        if (i3 >= i) {
                            throw new IOException("invalid header field");
                        }
                        i2 = i3;
                    }
                }
            } else {
                if (str2 == null) {
                    throw new IOException("misplaced continuation line");
                }
                byte[] bArr4 = new byte[(bArr3.length + i) - 1];
                System.arraycopy(bArr3, 0, bArr4, 0, bArr3.length);
                System.arraycopy(bArr, 1, bArr4, bArr3.length, i - 1);
                if (fastInputStream.peek() == 32) {
                    bArr3 = bArr4;
                } else {
                    str = new String(bArr4, 0, bArr4.length, "UTF8");
                    bArr2 = null;
                    z = true;
                    try {
                        if (putValue(str2, str) != null && !z) {
                            PlatformLogger.getLogger("java.util.jar").warning("Duplicate name in Manifest: " + str2 + ".\nEnsure that the manifest does not have duplicate entries, and\nthat blank lines separate individual sections in both your\nmanifest and in the META-INF/MANIFEST.MF entry in the jar file.");
                        }
                        bArr3 = bArr2;
                    } catch (IllegalArgumentException e) {
                        throw new IOException("invalid header field name: " + str2);
                    }
                }
            }
        }
    }

    public static class Name {
        private int hashCode = -1;
        private String name;
        public static final Name MANIFEST_VERSION = new Name("Manifest-Version");
        public static final Name SIGNATURE_VERSION = new Name("Signature-Version");
        public static final Name CONTENT_TYPE = new Name("Content-Type");
        public static final Name CLASS_PATH = new Name("Class-Path");
        public static final Name MAIN_CLASS = new Name("Main-Class");
        public static final Name SEALED = new Name("Sealed");
        public static final Name EXTENSION_LIST = new Name("Extension-List");
        public static final Name EXTENSION_NAME = new Name("Extension-Name");

        @Deprecated
        public static final Name EXTENSION_INSTALLATION = new Name("Extension-Installation");
        public static final Name IMPLEMENTATION_TITLE = new Name("Implementation-Title");
        public static final Name IMPLEMENTATION_VERSION = new Name("Implementation-Version");
        public static final Name IMPLEMENTATION_VENDOR = new Name("Implementation-Vendor");

        @Deprecated
        public static final Name IMPLEMENTATION_VENDOR_ID = new Name("Implementation-Vendor-Id");

        @Deprecated
        public static final Name IMPLEMENTATION_URL = new Name("Implementation-URL");
        public static final Name SPECIFICATION_TITLE = new Name("Specification-Title");
        public static final Name SPECIFICATION_VERSION = new Name("Specification-Version");
        public static final Name SPECIFICATION_VENDOR = new Name("Specification-Vendor");
        public static final Name NAME = new Name("Name");

        public Name(String str) {
            if (str == null) {
                throw new NullPointerException("name");
            }
            if (!isValid(str)) {
                throw new IllegalArgumentException(str);
            }
            this.name = str.intern();
        }

        private static boolean isValid(String str) {
            int length = str.length();
            if (length > 70 || length == 0) {
                return false;
            }
            for (int i = 0; i < length; i++) {
                if (!isValid(str.charAt(i))) {
                    return false;
                }
            }
            return true;
        }

        private static boolean isValid(char c) {
            return isAlpha(c) || isDigit(c) || c == '_' || c == '-';
        }

        private static boolean isAlpha(char c) {
            return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
        }

        private static boolean isDigit(char c) {
            return c >= '0' && c <= '9';
        }

        public boolean equals(Object obj) {
            return (obj instanceof Name) && ASCIICaseInsensitiveComparator.CASE_INSENSITIVE_ORDER.compare(this.name, ((Name) obj).name) == 0;
        }

        public int hashCode() {
            if (this.hashCode == -1) {
                this.hashCode = ASCIICaseInsensitiveComparator.lowerCaseHashCode(this.name);
            }
            return this.hashCode;
        }

        public String toString() {
            return this.name;
        }
    }
}
