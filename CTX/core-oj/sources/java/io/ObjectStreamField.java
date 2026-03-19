package java.io;

import java.lang.reflect.Field;
import java.sql.Types;
import sun.reflect.CallerSensitive;

public class ObjectStreamField implements Comparable<Object> {
    private final Field field;
    private final String name;
    private int offset;
    private final String signature;
    private final Class<?> type;
    private final boolean unshared;

    public ObjectStreamField(String str, Class<?> cls) {
        this(str, cls, false);
    }

    public ObjectStreamField(String str, Class<?> cls, boolean z) {
        this.offset = 0;
        if (str == null) {
            throw new NullPointerException();
        }
        this.name = str;
        this.type = cls;
        this.unshared = z;
        this.signature = getClassSignature(cls).intern();
        this.field = null;
    }

    ObjectStreamField(String str, String str2, boolean z) {
        this.offset = 0;
        if (str == null) {
            throw new NullPointerException();
        }
        this.name = str;
        this.signature = str2.intern();
        this.unshared = z;
        this.field = null;
        switch (str2.charAt(0)) {
            case 'B':
                this.type = Byte.TYPE;
                return;
            case 'C':
                this.type = Character.TYPE;
                return;
            case 'D':
                this.type = Double.TYPE;
                return;
            case Types.DATALINK:
                this.type = Float.TYPE;
                return;
            case 'I':
                this.type = Integer.TYPE;
                return;
            case 'J':
                this.type = Long.TYPE;
                return;
            case 'L':
            case Types.DATE:
                this.type = Object.class;
                return;
            case 'S':
                this.type = Short.TYPE;
                return;
            case 'Z':
                this.type = Boolean.TYPE;
                return;
            default:
                throw new IllegalArgumentException("illegal signature");
        }
    }

    ObjectStreamField(Field field, boolean z, boolean z2) {
        this.offset = 0;
        this.field = field;
        this.unshared = z;
        this.name = field.getName();
        Class<?> type = field.getType();
        this.type = (z2 || type.isPrimitive()) ? type : Object.class;
        this.signature = getClassSignature(type).intern();
    }

    public String getName() {
        return this.name;
    }

    @CallerSensitive
    public Class<?> getType() {
        return this.type;
    }

    public char getTypeCode() {
        return this.signature.charAt(0);
    }

    public String getTypeString() {
        if (isPrimitive()) {
            return null;
        }
        return this.signature;
    }

    public int getOffset() {
        return this.offset;
    }

    protected void setOffset(int i) {
        this.offset = i;
    }

    public boolean isPrimitive() {
        char cCharAt = this.signature.charAt(0);
        return (cCharAt == 'L' || cCharAt == '[') ? false : true;
    }

    public boolean isUnshared() {
        return this.unshared;
    }

    @Override
    public int compareTo(Object obj) {
        ObjectStreamField objectStreamField = (ObjectStreamField) obj;
        boolean zIsPrimitive = isPrimitive();
        if (zIsPrimitive != objectStreamField.isPrimitive()) {
            return zIsPrimitive ? -1 : 1;
        }
        return this.name.compareTo(objectStreamField.name);
    }

    public String toString() {
        return this.signature + ' ' + this.name;
    }

    Field getField() {
        return this.field;
    }

    String getSignature() {
        return this.signature;
    }

    private static String getClassSignature(Class<?> cls) {
        StringBuilder sb = new StringBuilder();
        while (cls.isArray()) {
            sb.append('[');
            cls = cls.getComponentType();
        }
        if (cls.isPrimitive()) {
            if (cls == Integer.TYPE) {
                sb.append('I');
            } else if (cls == Byte.TYPE) {
                sb.append('B');
            } else if (cls == Long.TYPE) {
                sb.append('J');
            } else if (cls == Float.TYPE) {
                sb.append('F');
            } else if (cls == Double.TYPE) {
                sb.append('D');
            } else if (cls == Short.TYPE) {
                sb.append('S');
            } else if (cls == Character.TYPE) {
                sb.append('C');
            } else if (cls == Boolean.TYPE) {
                sb.append('Z');
            } else if (cls == Void.TYPE) {
                sb.append('V');
            } else {
                throw new InternalError();
            }
        } else {
            sb.append('L' + cls.getName().replace('.', '/') + ';');
        }
        return sb.toString();
    }
}
