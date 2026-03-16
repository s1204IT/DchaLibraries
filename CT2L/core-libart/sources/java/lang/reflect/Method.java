package java.lang.reflect;

import java.lang.annotation.Annotation;
import java.lang.reflect.AbstractMethod;
import java.util.Comparator;
import java.util.List;
import libcore.reflect.AnnotationAccess;
import libcore.reflect.Types;

public final class Method extends AbstractMethod implements GenericDeclaration, Member {
    public static final Comparator<Method> ORDER_BY_SIGNATURE = new Comparator<Method>() {
        @Override
        public int compare(Method a, Method b) {
            if (a == b) {
                return 0;
            }
            int comparison = a.getName().compareTo(b.getName());
            if (comparison == 0) {
                int comparison2 = a.artMethod.findOverriddenMethodIfProxy().compareParameters(b.getParameterTypes());
                if (comparison2 == 0) {
                    Class<?> aReturnType = a.getReturnType();
                    Class<?> bReturnType = b.getReturnType();
                    if (aReturnType == bReturnType) {
                        return 0;
                    }
                    return aReturnType.getName().compareTo(bReturnType.getName());
                }
                return comparison2;
            }
            return comparison;
        }
    };

    private native Class<?>[] getExceptionTypesNative();

    private native Object invoke(Object obj, Object[] objArr, boolean z) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException;

    public Method(ArtMethod artMethod) {
        super(artMethod);
    }

    ArtMethod getArtMethod() {
        return this.artMethod;
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
    public boolean isBridge() {
        return super.isBridge();
    }

    @Override
    public boolean isSynthetic() {
        return super.isSynthetic();
    }

    @Override
    public String getName() {
        return ArtMethod.getMethodName(this.artMethod);
    }

    @Override
    public Class<?> getDeclaringClass() {
        return super.getDeclaringClass();
    }

    public Class<?>[] getExceptionTypes() {
        return getDeclaringClass().isProxy() ? getExceptionTypesNative() : AnnotationAccess.getExceptions(this);
    }

    @Override
    public Class<?>[] getParameterTypes() {
        return this.artMethod.findOverriddenMethodIfProxy().getParameterTypes();
    }

    public Class<?> getReturnType() {
        return this.artMethod.findOverriddenMethodIfProxy().getReturnType();
    }

    public int hashCode() {
        return getDeclaringClass().getName().hashCode() ^ getName().hashCode();
    }

    @Override
    public boolean equals(Object other) {
        return super.equals(other);
    }

    boolean equalNameAndParameters(Method m) {
        return getName().equals(m.getName()) && ArtMethod.equalMethodParameters(this.artMethod, m.getParameterTypes());
    }

    @Override
    public String toGenericString() {
        return super.toGenericString();
    }

    @Override
    public TypeVariable<Method>[] getTypeParameters() {
        AbstractMethod.GenericInfo info = getMethodOrConstructorGenericInfo();
        return (TypeVariable[]) info.formalTypeParameters.clone();
    }

    @Override
    public Type[] getGenericParameterTypes() {
        return Types.getTypeArray(getMethodOrConstructorGenericInfo().genericParameterTypes, false);
    }

    @Override
    public boolean isAnnotationPresent(Class<? extends Annotation> annotationType) {
        if (annotationType == null) {
            throw new NullPointerException("annotationType == null");
        }
        return AnnotationAccess.isDeclaredAnnotationPresent(this, annotationType);
    }

    @Override
    public Type[] getGenericExceptionTypes() {
        return Types.getTypeArray(getMethodOrConstructorGenericInfo().genericExceptionTypes, false);
    }

    public Type getGenericReturnType() {
        return Types.getType(getMethodOrConstructorGenericInfo().genericReturnType);
    }

    @Override
    public Annotation[] getDeclaredAnnotations() {
        List<Annotation> result = AnnotationAccess.getDeclaredAnnotations(this);
        return (Annotation[]) result.toArray(new Annotation[result.size()]);
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
        return this.artMethod.findOverriddenMethodIfProxy().getParameterAnnotations();
    }

    public Object getDefaultValue() {
        return AnnotationAccess.getDefaultValue(this);
    }

    public Object invoke(Object receiver, Object... args) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        return invoke(receiver, args, isAccessible());
    }

    public String toString() {
        StringBuilder result = new StringBuilder(Modifier.toString(getModifiers()));
        if (result.length() != 0) {
            result.append(' ');
        }
        result.append(getReturnType().getName());
        result.append(' ');
        result.append(getDeclaringClass().getName());
        result.append('.');
        result.append(getName());
        result.append("(");
        Class<?>[] parameterTypes = getParameterTypes();
        result.append(Types.toString(parameterTypes));
        result.append(")");
        Class<?>[] exceptionTypes = getExceptionTypes();
        if (exceptionTypes.length != 0) {
            result.append(" throws ");
            result.append(Types.toString(exceptionTypes));
        }
        return result.toString();
    }

    @Override
    String getSignature() {
        StringBuilder result = new StringBuilder();
        result.append('(');
        Class<?>[] parameterTypes = getParameterTypes();
        for (Class<?> parameterType : parameterTypes) {
            result.append(Types.getSignature(parameterType));
        }
        result.append(')');
        result.append(Types.getSignature(getReturnType()));
        return result.toString();
    }
}
