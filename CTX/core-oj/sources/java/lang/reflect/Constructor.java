package java.lang.reflect;

import dalvik.annotation.optimization.FastNative;
import java.lang.annotation.Annotation;
import java.util.Comparator;
import libcore.util.EmptyArray;
import sun.reflect.CallerSensitive;

public final class Constructor<T> extends Executable {
    private static final Comparator<Method> ORDER_BY_SIGNATURE = null;
    private final Class<?> serializationClass;
    private final Class<?> serializationCtor;

    @FastNative
    private native T newInstance0(Object... objArr) throws IllegalAccessException, InstantiationException, IllegalArgumentException, InvocationTargetException;

    @FastNative
    private static native Object newInstanceFromSerialization(Class<?> cls, Class<?> cls2) throws InstantiationException, IllegalArgumentException, InvocationTargetException;

    @Override
    @FastNative
    public native Class<?>[] getExceptionTypes();

    private Constructor() {
        this(null, null);
    }

    private Constructor(Class<?> cls, Class<?> cls2) {
        this.serializationCtor = cls;
        this.serializationClass = cls2;
    }

    public Constructor<T> serializationCopy(Class<?> cls, Class<?> cls2) {
        return new Constructor<>(cls, cls2);
    }

    @Override
    boolean hasGenericInformation() {
        return super.hasGenericInformationInternal();
    }

    @Override
    public Class<T> getDeclaringClass() {
        return (Class<T>) super.getDeclaringClassInternal();
    }

    @Override
    public String getName() {
        return getDeclaringClass().getName();
    }

    @Override
    public int getModifiers() {
        return super.getModifiersInternal();
    }

    @Override
    public TypeVariable<Constructor<T>>[] getTypeParameters() {
        return (TypeVariable[]) getMethodOrConstructorGenericInfoInternal().formalTypeParameters.clone();
    }

    @Override
    public Class<?>[] getParameterTypes() {
        Class<?>[] parameterTypesInternal = super.getParameterTypesInternal();
        if (parameterTypesInternal == null) {
            return EmptyArray.CLASS;
        }
        return parameterTypesInternal;
    }

    @Override
    public int getParameterCount() {
        return super.getParameterCountInternal();
    }

    @Override
    public Type[] getGenericParameterTypes() {
        return super.getGenericParameterTypes();
    }

    @Override
    public Type[] getGenericExceptionTypes() {
        return super.getGenericExceptionTypes();
    }

    public boolean equals(Object obj) {
        if (obj != null && (obj instanceof Constructor)) {
            Constructor constructor = (Constructor) obj;
            if (getDeclaringClass() == constructor.getDeclaringClass()) {
                return equalParamTypes(getParameterTypes(), constructor.getParameterTypes());
            }
            return false;
        }
        return false;
    }

    public int hashCode() {
        return getDeclaringClass().getName().hashCode();
    }

    public String toString() {
        return sharedToString(Modifier.constructorModifiers(), false, getParameterTypes(), getExceptionTypes());
    }

    @Override
    void specificToStringHeader(StringBuilder sb) {
        sb.append(getDeclaringClass().getTypeName());
    }

    @Override
    public String toGenericString() {
        return sharedToGenericString(Modifier.constructorModifiers(), false);
    }

    @Override
    void specificToGenericStringHeader(StringBuilder sb) {
        specificToStringHeader(sb);
    }

    @CallerSensitive
    public T newInstance(Object... objArr) throws IllegalAccessException, InstantiationException, IllegalArgumentException, InvocationTargetException {
        if (this.serializationClass == null) {
            return newInstance0(objArr);
        }
        return (T) newInstanceFromSerialization(this.serializationCtor, this.serializationClass);
    }

    @Override
    public boolean isVarArgs() {
        return super.isVarArgs();
    }

    @Override
    public boolean isSynthetic() {
        return super.isSynthetic();
    }

    @Override
    public Annotation getAnnotation(Class cls) {
        return super.getAnnotation(cls);
    }

    @Override
    public Annotation[] getDeclaredAnnotations() {
        return super.getDeclaredAnnotations();
    }

    @Override
    public Annotation[][] getParameterAnnotations() {
        return super.getParameterAnnotationsInternal();
    }
}
