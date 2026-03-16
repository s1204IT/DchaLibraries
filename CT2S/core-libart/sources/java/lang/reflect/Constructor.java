package java.lang.reflect;

import java.lang.annotation.Annotation;
import java.lang.reflect.AbstractMethod;
import java.util.Comparator;
import java.util.List;
import libcore.reflect.AnnotationAccess;
import libcore.reflect.Types;

public final class Constructor<T> extends AbstractMethod implements GenericDeclaration, Member {
    private static final Comparator<Method> ORDER_BY_SIGNATURE = null;

    public native T newInstance(Object[] objArr, boolean z) throws IllegalAccessException, InstantiationException, IllegalArgumentException, InvocationTargetException;

    public Constructor(ArtMethod artMethod) {
        super(artMethod);
    }

    @Override
    public Annotation[] getAnnotations() {
        return super.getAnnotations();
    }

    @Override
    public int getModifiers() {
        return super.getModifiers();
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
    public String getName() {
        return getDeclaringClass().getName();
    }

    @Override
    public Class<T> getDeclaringClass() {
        return (Class<T>) super.getDeclaringClass();
    }

    public Class<?>[] getExceptionTypes() {
        return AnnotationAccess.getExceptions(this);
    }

    @Override
    public Class<?>[] getParameterTypes() {
        return super.getParameterTypes();
    }

    public int hashCode() {
        return getDeclaringClass().getName().hashCode();
    }

    @Override
    public boolean equals(Object other) {
        return super.equals(other);
    }

    @Override
    public TypeVariable<Constructor<T>>[] getTypeParameters() {
        AbstractMethod.GenericInfo info = getMethodOrConstructorGenericInfo();
        return (TypeVariable[]) info.formalTypeParameters.clone();
    }

    @Override
    public String toGenericString() {
        return super.toGenericString();
    }

    @Override
    public Type[] getGenericParameterTypes() {
        return super.getGenericParameterTypes();
    }

    @Override
    public Type[] getGenericExceptionTypes() {
        return super.getGenericExceptionTypes();
    }

    @Override
    public Annotation[] getDeclaredAnnotations() {
        List<Annotation> result = AnnotationAccess.getDeclaredAnnotations(this);
        return (Annotation[]) result.toArray(new Annotation[result.size()]);
    }

    @Override
    public boolean isAnnotationPresent(Class<? extends Annotation> annotationType) {
        if (annotationType == null) {
            throw new NullPointerException("annotationType == null");
        }
        return AnnotationAccess.isDeclaredAnnotationPresent(this, annotationType);
    }

    @Override
    public <A extends Annotation> A getAnnotation(Class<A> cls) {
        if (cls == null) {
            throw new NullPointerException("annotationType == null");
        }
        return (A) AnnotationAccess.getDeclaredAnnotation(this, cls);
    }

    @Override
    public Annotation[][] getParameterAnnotations() {
        return this.artMethod.getParameterAnnotations();
    }

    @Override
    String getSignature() {
        StringBuilder result = new StringBuilder();
        result.append('(');
        Class<?>[] parameterTypes = getParameterTypes();
        for (Class<?> parameterType : parameterTypes) {
            result.append(Types.getSignature(parameterType));
        }
        result.append(")V");
        return result.toString();
    }

    public T newInstance(Object... args) throws IllegalAccessException, InstantiationException, IllegalArgumentException, InvocationTargetException {
        return newInstance(args, isAccessible());
    }

    public String toString() {
        StringBuilder result = new StringBuilder(Modifier.toString(getModifiers()));
        if (result.length() != 0) {
            result.append(' ');
        }
        result.append(getDeclaringClass().getName());
        result.append("(");
        Class<?>[] parameterTypes = getParameterTypes();
        result.append(Types.toString(parameterTypes));
        result.append(")");
        Class<?>[] exceptionTypes = getExceptionTypes();
        if (exceptionTypes.length > 0) {
            result.append(" throws ");
            result.append(Types.toString(exceptionTypes));
        }
        return result.toString();
    }
}
