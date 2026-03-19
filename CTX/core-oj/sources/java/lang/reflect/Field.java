package java.lang.reflect;

import dalvik.annotation.optimization.FastNative;
import java.lang.annotation.Annotation;
import java.util.Objects;
import libcore.reflect.AnnotatedElements;
import libcore.reflect.GenericSignatureParser;
import sun.reflect.CallerSensitive;

public final class Field extends AccessibleObject implements Member {
    private int accessFlags;
    private Class<?> declaringClass;
    private int dexFieldIndex;
    private int offset;
    private Class<?> type;

    @FastNative
    private native <A extends Annotation> A getAnnotationNative(Class<A> cls);

    @FastNative
    private native String getNameInternal();

    @FastNative
    private native String[] getSignatureAnnotation();

    @FastNative
    private native boolean isAnnotationPresentNative(Class<? extends Annotation> cls);

    @FastNative
    @CallerSensitive
    public native Object get(Object obj) throws IllegalAccessException, IllegalArgumentException;

    @FastNative
    public native long getArtField();

    @FastNative
    @CallerSensitive
    public native boolean getBoolean(Object obj) throws IllegalAccessException, IllegalArgumentException;

    @FastNative
    @CallerSensitive
    public native byte getByte(Object obj) throws IllegalAccessException, IllegalArgumentException;

    @FastNative
    @CallerSensitive
    public native char getChar(Object obj) throws IllegalAccessException, IllegalArgumentException;

    @Override
    @FastNative
    public native Annotation[] getDeclaredAnnotations();

    @FastNative
    @CallerSensitive
    public native double getDouble(Object obj) throws IllegalAccessException, IllegalArgumentException;

    @FastNative
    @CallerSensitive
    public native float getFloat(Object obj) throws IllegalAccessException, IllegalArgumentException;

    @FastNative
    @CallerSensitive
    public native int getInt(Object obj) throws IllegalAccessException, IllegalArgumentException;

    @FastNative
    @CallerSensitive
    public native long getLong(Object obj) throws IllegalAccessException, IllegalArgumentException;

    @FastNative
    @CallerSensitive
    public native short getShort(Object obj) throws IllegalAccessException, IllegalArgumentException;

    @FastNative
    @CallerSensitive
    public native void set(Object obj, Object obj2) throws IllegalAccessException, IllegalArgumentException;

    @FastNative
    @CallerSensitive
    public native void setBoolean(Object obj, boolean z) throws IllegalAccessException, IllegalArgumentException;

    @FastNative
    @CallerSensitive
    public native void setByte(Object obj, byte b) throws IllegalAccessException, IllegalArgumentException;

    @FastNative
    @CallerSensitive
    public native void setChar(Object obj, char c) throws IllegalAccessException, IllegalArgumentException;

    @FastNative
    @CallerSensitive
    public native void setDouble(Object obj, double d) throws IllegalAccessException, IllegalArgumentException;

    @FastNative
    @CallerSensitive
    public native void setFloat(Object obj, float f) throws IllegalAccessException, IllegalArgumentException;

    @FastNative
    @CallerSensitive
    public native void setInt(Object obj, int i) throws IllegalAccessException, IllegalArgumentException;

    @FastNative
    @CallerSensitive
    public native void setLong(Object obj, long j) throws IllegalAccessException, IllegalArgumentException;

    @FastNative
    @CallerSensitive
    public native void setShort(Object obj, short s) throws IllegalAccessException, IllegalArgumentException;

    private Field() {
    }

    @Override
    public Class<?> getDeclaringClass() {
        return this.declaringClass;
    }

    @Override
    public String getName() {
        if (this.dexFieldIndex == -1) {
            if (!this.declaringClass.isProxy()) {
                throw new AssertionError();
            }
            return "throws";
        }
        return getNameInternal();
    }

    @Override
    public int getModifiers() {
        return this.accessFlags & 65535;
    }

    public boolean isEnumConstant() {
        return (getModifiers() & 16384) != 0;
    }

    @Override
    public boolean isSynthetic() {
        return Modifier.isSynthetic(getModifiers());
    }

    public Class<?> getType() {
        return this.type;
    }

    public Type getGenericType() {
        String signatureAttribute = getSignatureAttribute();
        GenericSignatureParser genericSignatureParser = new GenericSignatureParser(this.declaringClass.getClassLoader());
        genericSignatureParser.parseForField(this.declaringClass, signatureAttribute);
        Type type = genericSignatureParser.fieldType;
        if (type == null) {
            return getType();
        }
        return type;
    }

    private String getSignatureAttribute() {
        String[] signatureAnnotation = getSignatureAnnotation();
        if (signatureAnnotation == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (String str : signatureAnnotation) {
            sb.append(str);
        }
        return sb.toString();
    }

    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof Field)) {
            return false;
        }
        Field field = (Field) obj;
        return getDeclaringClass() == field.getDeclaringClass() && getName() == field.getName() && getType() == field.getType();
    }

    public int hashCode() {
        return getDeclaringClass().getName().hashCode() ^ getName().hashCode();
    }

    public String toString() {
        String str;
        int modifiers = getModifiers();
        StringBuilder sb = new StringBuilder();
        if (modifiers == 0) {
            str = "";
        } else {
            str = Modifier.toString(modifiers) + " ";
        }
        sb.append(str);
        sb.append(getType().getTypeName());
        sb.append(" ");
        sb.append(getDeclaringClass().getTypeName());
        sb.append(".");
        sb.append(getName());
        return sb.toString();
    }

    public String toGenericString() {
        String str;
        int modifiers = getModifiers();
        Type genericType = getGenericType();
        StringBuilder sb = new StringBuilder();
        if (modifiers == 0) {
            str = "";
        } else {
            str = Modifier.toString(modifiers) + " ";
        }
        sb.append(str);
        sb.append(genericType.getTypeName());
        sb.append(" ");
        sb.append(getDeclaringClass().getTypeName());
        sb.append(".");
        sb.append(getName());
        return sb.toString();
    }

    @Override
    public <T extends Annotation> T getAnnotation(Class<T> cls) {
        Objects.requireNonNull(cls);
        return (T) getAnnotationNative(cls);
    }

    @Override
    public <T extends Annotation> T[] getAnnotationsByType(Class<T> cls) {
        return (T[]) AnnotatedElements.getDirectOrIndirectAnnotationsByType(this, cls);
    }

    @Override
    public boolean isAnnotationPresent(Class<? extends Annotation> cls) {
        if (cls == null) {
            throw new NullPointerException("annotationType == null");
        }
        return isAnnotationPresentNative(cls);
    }

    public int getDexFieldIndex() {
        return this.dexFieldIndex;
    }

    public int getOffset() {
        return this.offset;
    }
}
