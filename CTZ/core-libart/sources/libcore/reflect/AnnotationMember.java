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
        NO_VALUE
    }

    public AnnotationMember(String str, Object obj) {
        this.name = str;
        this.value = obj == null ? NO_VALUE : obj;
        if (this.value instanceof Throwable) {
            this.tag = ERROR;
        } else if (this.value.getClass().isArray()) {
            this.tag = ARRAY;
        } else {
            this.tag = OTHER;
        }
    }

    public AnnotationMember(String str, Object obj, Class cls, Method method) {
        this(str, obj);
        this.definingMethod = method;
        if (cls == Integer.TYPE) {
            this.elementType = Integer.class;
            return;
        }
        if (cls == Boolean.TYPE) {
            this.elementType = Boolean.class;
            return;
        }
        if (cls == Character.TYPE) {
            this.elementType = Character.class;
            return;
        }
        if (cls == Float.TYPE) {
            this.elementType = Float.class;
            return;
        }
        if (cls == Double.TYPE) {
            this.elementType = Double.class;
            return;
        }
        if (cls == Long.TYPE) {
            this.elementType = Long.class;
            return;
        }
        if (cls == Short.TYPE) {
            this.elementType = Short.class;
        } else if (cls == Byte.TYPE) {
            this.elementType = Byte.class;
        } else {
            this.elementType = cls;
        }
    }

    protected AnnotationMember setDefinition(AnnotationMember annotationMember) {
        this.definingMethod = annotationMember.definingMethod;
        this.elementType = annotationMember.elementType;
        return this;
    }

    public String toString() {
        if (this.tag == '[') {
            StringBuilder sb = new StringBuilder(80);
            sb.append(this.name);
            sb.append("=[");
            int length = Array.getLength(this.value);
            for (int i = 0; i < length; i++) {
                if (i != 0) {
                    sb.append(", ");
                }
                sb.append(Array.get(this.value, i));
            }
            sb.append("]");
            return sb.toString();
        }
        return this.name + "=" + this.value;
    }

    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof AnnotationMember) {
            AnnotationMember annotationMember = (AnnotationMember) obj;
            if (this.name.equals(annotationMember.name) && this.tag == annotationMember.tag) {
                if (this.tag == '[') {
                    return equalArrayValue(annotationMember.value);
                }
                if (this.tag == '!') {
                    return false;
                }
                return this.value.equals(annotationMember.value);
            }
        }
        return false;
    }

    public boolean equalArrayValue(Object obj) {
        if ((this.value instanceof Object[]) && (obj instanceof Object[])) {
            return Arrays.equals((Object[]) this.value, (Object[]) obj);
        }
        Class<?> cls = this.value.getClass();
        if (cls != obj.getClass()) {
            return false;
        }
        if (cls == int[].class) {
            return Arrays.equals((int[]) this.value, (int[]) obj);
        }
        if (cls == byte[].class) {
            return Arrays.equals((byte[]) this.value, (byte[]) obj);
        }
        if (cls == short[].class) {
            return Arrays.equals((short[]) this.value, (short[]) obj);
        }
        if (cls == long[].class) {
            return Arrays.equals((long[]) this.value, (long[]) obj);
        }
        if (cls == char[].class) {
            return Arrays.equals((char[]) this.value, (char[]) obj);
        }
        if (cls == boolean[].class) {
            return Arrays.equals((boolean[]) this.value, (boolean[]) obj);
        }
        if (cls == float[].class) {
            return Arrays.equals((float[]) this.value, (float[]) obj);
        }
        if (cls == double[].class) {
            return Arrays.equals((double[]) this.value, (double[]) obj);
        }
        return false;
    }

    public int hashCode() {
        int iHashCode = this.name.hashCode() * 127;
        if (this.tag == '[') {
            Class<?> cls = this.value.getClass();
            if (cls == int[].class) {
                return iHashCode ^ Arrays.hashCode((int[]) this.value);
            }
            if (cls == byte[].class) {
                return iHashCode ^ Arrays.hashCode((byte[]) this.value);
            }
            if (cls == short[].class) {
                return iHashCode ^ Arrays.hashCode((short[]) this.value);
            }
            if (cls == long[].class) {
                return iHashCode ^ Arrays.hashCode((long[]) this.value);
            }
            if (cls == char[].class) {
                return iHashCode ^ Arrays.hashCode((char[]) this.value);
            }
            if (cls == boolean[].class) {
                return iHashCode ^ Arrays.hashCode((boolean[]) this.value);
            }
            if (cls == float[].class) {
                return iHashCode ^ Arrays.hashCode((float[]) this.value);
            }
            if (cls == double[].class) {
                return iHashCode ^ Arrays.hashCode((double[]) this.value);
            }
            return iHashCode ^ Arrays.hashCode((Object[]) this.value);
        }
        return iHashCode ^ this.value.hashCode();
    }

    public void rethrowError() throws Throwable {
        if (this.tag == '!') {
            if (this.value instanceof TypeNotPresentException) {
                TypeNotPresentException typeNotPresentException = (TypeNotPresentException) this.value;
                throw new TypeNotPresentException(typeNotPresentException.typeName(), typeNotPresentException.getCause());
            }
            if (this.value instanceof EnumConstantNotPresentException) {
                EnumConstantNotPresentException enumConstantNotPresentException = (EnumConstantNotPresentException) this.value;
                throw new EnumConstantNotPresentException(enumConstantNotPresentException.enumType(), enumConstantNotPresentException.constantName());
            }
            if (this.value instanceof ArrayStoreException) {
                throw new ArrayStoreException(((ArrayStoreException) this.value).getMessage());
            }
            Throwable th = (Throwable) this.value;
            StackTraceElement[] stackTrace = th.getStackTrace();
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(stackTrace == null ? 512 : (stackTrace.length + 1) * 80);
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
            objectOutputStream.writeObject(th);
            objectOutputStream.flush();
            objectOutputStream.close();
            ObjectInputStream objectInputStream = new ObjectInputStream(new ByteArrayInputStream(byteArrayOutputStream.toByteArray()));
            Throwable th2 = (Throwable) objectInputStream.readObject();
            objectInputStream.close();
            throw th2;
        }
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
