package java.io;

import dalvik.bytecode.Opcodes;
import java.lang.ref.WeakReference;

public class ObjectStreamField implements Comparable<Object> {
    private boolean isDeserialized;
    private String name;
    int offset;
    private Object type;
    private String typeString;
    private boolean unshared;

    public ObjectStreamField(String name, Class<?> cl) {
        if (name == null) {
            throw new NullPointerException("name == null");
        }
        if (cl == null) {
            throw new NullPointerException("cl == null");
        }
        this.name = name;
        this.type = new WeakReference(cl);
    }

    public ObjectStreamField(String str, Class<?> cls, boolean z) {
        if (str == null) {
            throw new NullPointerException("name == null");
        }
        if (cls == null) {
            throw new NullPointerException("cl == null");
        }
        this.name = str;
        this.type = cls.getClassLoader() != null ? new WeakReference(cls) : cls;
        this.unshared = z;
    }

    ObjectStreamField(String signature, String name) {
        if (name == null) {
            throw new NullPointerException("name == null");
        }
        this.name = name;
        this.typeString = signature.replace('.', '/').intern();
        defaultResolve();
        this.isDeserialized = true;
    }

    @Override
    public int compareTo(Object o) {
        ObjectStreamField f = (ObjectStreamField) o;
        boolean thisPrimitive = isPrimitive();
        boolean fPrimitive = f.isPrimitive();
        if (thisPrimitive != fPrimitive) {
            return thisPrimitive ? -1 : 1;
        }
        return getName().compareTo(f.getName());
    }

    public String getName() {
        return this.name;
    }

    public int getOffset() {
        return this.offset;
    }

    Class<?> getTypeInternal() {
        return this.type instanceof WeakReference ? (Class) ((WeakReference) this.type).get() : (Class) this.type;
    }

    public Class<?> getType() {
        Class<?> cl = getTypeInternal();
        if (this.isDeserialized && !cl.isPrimitive()) {
            return Object.class;
        }
        return cl;
    }

    public char getTypeCode() {
        return typeCodeOf(getTypeInternal());
    }

    private char typeCodeOf(Class<?> type) {
        if (type == Integer.TYPE) {
            return 'I';
        }
        if (type == Byte.TYPE) {
            return 'B';
        }
        if (type == Character.TYPE) {
            return 'C';
        }
        if (type == Short.TYPE) {
            return 'S';
        }
        if (type == Boolean.TYPE) {
            return 'Z';
        }
        if (type == Long.TYPE) {
            return 'J';
        }
        if (type == Float.TYPE) {
            return 'F';
        }
        if (type == Double.TYPE) {
            return 'D';
        }
        if (type.isArray()) {
            return '[';
        }
        return 'L';
    }

    public String getTypeString() {
        if (isPrimitive()) {
            return null;
        }
        if (this.typeString == null) {
            Class<?> t = getTypeInternal();
            String typeName = t.getName().replace('.', '/');
            String str = t.isArray() ? typeName : "L" + typeName + ';';
            this.typeString = str.intern();
        }
        return this.typeString;
    }

    public boolean isPrimitive() {
        Class<?> t = getTypeInternal();
        return t != null && t.isPrimitive();
    }

    boolean writeField(DataOutputStream out) throws IOException {
        Class<?> t = getTypeInternal();
        out.writeByte(typeCodeOf(t));
        out.writeUTF(this.name);
        return t != null && t.isPrimitive();
    }

    protected void setOffset(int newValue) {
        this.offset = newValue;
    }

    public String toString() {
        return getClass().getName() + '(' + getName() + ':' + getTypeInternal() + ')';
    }

    void resolve(ClassLoader classLoader) {
        if (this.typeString == null && isPrimitive()) {
            this.typeString = String.valueOf(getTypeCode());
        }
        if (this.typeString.length() != 1 || !defaultResolve()) {
            String strReplace = this.typeString.replace('/', '.');
            if (strReplace.charAt(0) == 'L') {
                strReplace = strReplace.substring(1, strReplace.length() - 1);
            }
            try {
                Class<?> cls = Class.forName(strReplace, false, classLoader);
                ClassLoader classLoader2 = cls.getClassLoader();
                Object weakReference = cls;
                if (classLoader2 != null) {
                    weakReference = new WeakReference(cls);
                }
                this.type = weakReference;
            } catch (ClassNotFoundException e) {
            }
        }
    }

    public boolean isUnshared() {
        return this.unshared;
    }

    void setUnshared(boolean unshared) {
        this.unshared = unshared;
    }

    private boolean defaultResolve() {
        switch (this.typeString.charAt(0)) {
            case 'B':
                this.type = Byte.TYPE;
                return true;
            case 'C':
                this.type = Character.TYPE;
                return true;
            case Opcodes.OP_AGET:
                this.type = Double.TYPE;
                return true;
            case 'F':
                this.type = Float.TYPE;
                return true;
            case Opcodes.OP_AGET_CHAR:
                this.type = Integer.TYPE;
                return true;
            case Opcodes.OP_AGET_SHORT:
                this.type = Long.TYPE;
                return true;
            case Opcodes.OP_IGET_WIDE:
                this.type = Short.TYPE;
                return true;
            case Opcodes.OP_IPUT_WIDE:
                this.type = Boolean.TYPE;
                return true;
            default:
                this.type = Object.class;
                return false;
        }
    }
}
