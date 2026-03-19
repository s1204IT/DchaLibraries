package libcore.reflect;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.annotation.AnnotationTypeMismatchException;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.Arrays;

public final class AnnotationMember implements Serializable {
    protected static final char ARRAY = '[';
    protected static final char ERROR = '!';
    protected static final Object NO_VALUE = DefaultValues.NO_VALUE;
    protected static final char OTHER = '*';
    protected transient Method definingMethod;
    protected transient Class<?> elementType;
    protected final String name;
    protected final char tag;
    protected final Object value;

    private enum DefaultValues {
        NO_VALUE;

        public static DefaultValues[] valuesCustom() {
            return values();
        }
    }

    public AnnotationMember(String name, Object val) {
        this.name = name;
        this.value = val == null ? NO_VALUE : val;
        if (this.value instanceof Throwable) {
            this.tag = ERROR;
        } else if (this.value.getClass().isArray()) {
            this.tag = ARRAY;
        } else {
            this.tag = OTHER;
        }
    }

    public AnnotationMember(String name, Object val, Class type, Method m) {
        this(name, val);
        this.definingMethod = m;
        if (type == Integer.TYPE) {
            this.elementType = Integer.class;
            return;
        }
        if (type == Boolean.TYPE) {
            this.elementType = Boolean.class;
            return;
        }
        if (type == Character.TYPE) {
            this.elementType = Character.class;
            return;
        }
        if (type == Float.TYPE) {
            this.elementType = Float.class;
            return;
        }
        if (type == Double.TYPE) {
            this.elementType = Double.class;
            return;
        }
        if (type == Long.TYPE) {
            this.elementType = Long.class;
            return;
        }
        if (type == Short.TYPE) {
            this.elementType = Short.class;
        } else if (type == Byte.TYPE) {
            this.elementType = Byte.class;
        } else {
            this.elementType = type;
        }
    }

    protected AnnotationMember setDefinition(AnnotationMember copy) {
        this.definingMethod = copy.definingMethod;
        this.elementType = copy.elementType;
        return this;
    }

    public String toString() {
        if (this.tag == '[') {
            StringBuilder sb = new StringBuilder(80);
            sb.append(this.name).append("=[");
            int len = Array.getLength(this.value);
            for (int i = 0; i < len; i++) {
                if (i != 0) {
                    sb.append(", ");
                }
                sb.append(Array.get(this.value, i));
            }
            return sb.append("]").toString();
        }
        return this.name + "=" + this.value;
    }

    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof AnnotationMember) {
            AnnotationMember that = (AnnotationMember) obj;
            if (this.name.equals(that.name) && this.tag == that.tag) {
                if (this.tag == '[') {
                    return equalArrayValue(that.value);
                }
                if (this.tag == '!') {
                    return false;
                }
                return this.value.equals(that.value);
            }
        }
        return false;
    }

    public boolean equalArrayValue(Object otherValue) {
        if ((this.value instanceof Object[]) && (otherValue instanceof Object[])) {
            return Arrays.equals((Object[]) this.value, (Object[]) otherValue);
        }
        Class<?> cls = this.value.getClass();
        if (cls != otherValue.getClass()) {
            return false;
        }
        if (cls == int[].class) {
            return Arrays.equals((int[]) this.value, (int[]) otherValue);
        }
        if (cls == byte[].class) {
            return Arrays.equals((byte[]) this.value, (byte[]) otherValue);
        }
        if (cls == short[].class) {
            return Arrays.equals((short[]) this.value, (short[]) otherValue);
        }
        if (cls == long[].class) {
            return Arrays.equals((long[]) this.value, (long[]) otherValue);
        }
        if (cls == char[].class) {
            return Arrays.equals((char[]) this.value, (char[]) otherValue);
        }
        if (cls == boolean[].class) {
            return Arrays.equals((boolean[]) this.value, (boolean[]) otherValue);
        }
        if (cls == float[].class) {
            return Arrays.equals((float[]) this.value, (float[]) otherValue);
        }
        if (cls == double[].class) {
            return Arrays.equals((double[]) this.value, (double[]) otherValue);
        }
        return false;
    }

    public int hashCode() {
        int hash = this.name.hashCode() * 127;
        if (this.tag == '[') {
            Class<?> cls = this.value.getClass();
            if (cls == int[].class) {
                return Arrays.hashCode((int[]) this.value) ^ hash;
            }
            if (cls == byte[].class) {
                return Arrays.hashCode((byte[]) this.value) ^ hash;
            }
            if (cls == short[].class) {
                return Arrays.hashCode((short[]) this.value) ^ hash;
            }
            if (cls == long[].class) {
                return Arrays.hashCode((long[]) this.value) ^ hash;
            }
            if (cls == char[].class) {
                return Arrays.hashCode((char[]) this.value) ^ hash;
            }
            if (cls == boolean[].class) {
                return Arrays.hashCode((boolean[]) this.value) ^ hash;
            }
            if (cls == float[].class) {
                return Arrays.hashCode((float[]) this.value) ^ hash;
            }
            if (cls == double[].class) {
                return Arrays.hashCode((double[]) this.value) ^ hash;
            }
            return Arrays.hashCode((Object[]) this.value) ^ hash;
        }
        return this.value.hashCode() ^ hash;
    }

    public void rethrowError() throws Throwable {
        if (this.tag != '!') {
            return;
        }
        if (this.value instanceof TypeNotPresentException) {
            TypeNotPresentException tnpe = (TypeNotPresentException) this.value;
            throw new TypeNotPresentException(tnpe.typeName(), tnpe.getCause());
        }
        if (this.value instanceof EnumConstantNotPresentException) {
            EnumConstantNotPresentException ecnpe = (EnumConstantNotPresentException) this.value;
            throw new EnumConstantNotPresentException(ecnpe.enumType(), ecnpe.constantName());
        }
        if (this.value instanceof ArrayStoreException) {
            ArrayStoreException ase = (ArrayStoreException) this.value;
            throw new ArrayStoreException(ase.getMessage());
        }
        Throwable error = (Throwable) this.value;
        StackTraceElement[] ste = error.getStackTrace();
        ByteArrayOutputStream bos = new ByteArrayOutputStream(ste == null ? 512 : (ste.length + 1) * 80);
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(error);
        oos.flush();
        oos.close();
        ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
        ObjectInputStream ois = new ObjectInputStream(bis);
        Throwable error2 = (Throwable) ois.readObject();
        ois.close();
        throw error2;
    }

    public Object validateValue() throws Throwable {
        if (this.tag == '!') {
            rethrowError();
        }
        if (this.value == NO_VALUE) {
            return null;
        }
        if (this.elementType == this.value.getClass() || this.elementType.isInstance(this.value)) {
            return copyValue();
        }
        throw new AnnotationTypeMismatchException(this.definingMethod, this.value.getClass().getName());
    }

    public Object copyValue() throws Throwable {
        if (this.tag != '[' || Array.getLength(this.value) == 0) {
            return this.value;
        }
        Class<?> cls = this.value.getClass();
        if (cls == int[].class) {
            return ((int[]) this.value).clone();
        }
        if (cls == byte[].class) {
            return ((byte[]) this.value).clone();
        }
        if (cls == short[].class) {
            return ((short[]) this.value).clone();
        }
        if (cls == long[].class) {
            return ((long[]) this.value).clone();
        }
        if (cls == char[].class) {
            return ((char[]) this.value).clone();
        }
        if (cls == boolean[].class) {
            return ((boolean[]) this.value).clone();
        }
        if (cls == float[].class) {
            return ((float[]) this.value).clone();
        }
        if (cls == double[].class) {
            return ((double[]) this.value).clone();
        }
        return ((Object[]) this.value).clone();
    }
}
