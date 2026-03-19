package java.lang.reflect;

import dalvik.annotation.optimization.FastNative;
import java.lang.annotation.Annotation;
import java.util.Comparator;
import libcore.reflect.Types;
import libcore.util.EmptyArray;
import sun.reflect.CallerSensitive;

public final class Method extends Executable {
    public static final Comparator<Method> ORDER_BY_SIGNATURE = new Comparator<Method>() {
        @Override
        public int compare(Method method, Method method2) {
            if (method == method2) {
                return 0;
            }
            int iCompareTo = method.getName().compareTo(method2.getName());
            if (iCompareTo == 0 && (iCompareTo = method.compareMethodParametersInternal(method2)) == 0) {
                Class<?> returnType = method.getReturnType();
                Class<?> returnType2 = method2.getReturnType();
                if (returnType == returnType2) {
                    return 0;
                }
                return returnType.getName().compareTo(returnType2.getName());
            }
            return iCompareTo;
        }
    };

    @FastNative
    public native Object getDefaultValue();

    @Override
    @FastNative
    public native Class<?>[] getExceptionTypes();

    @FastNative
    @CallerSensitive
    public native Object invoke(Object obj, Object... objArr) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException;

    private Method() {
    }

    @Override
    boolean hasGenericInformation() {
        return super.hasGenericInformationInternal();
    }

    @Override
    public Class<?> getDeclaringClass() {
        return super.getDeclaringClassInternal();
    }

    @Override
    public String getName() {
        return getMethodNameInternal();
    }

    @Override
    public int getModifiers() {
        return super.getModifiersInternal();
    }

    @Override
    public TypeVariable<Method>[] getTypeParameters() {
        return (TypeVariable[]) getMethodOrConstructorGenericInfoInternal().formalTypeParameters.clone();
    }

    public Class<?> getReturnType() {
        return getMethodReturnTypeInternal();
    }

    public Type getGenericReturnType() {
        return Types.getType(getMethodOrConstructorGenericInfoInternal().genericReturnType);
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
        if (obj != null && (obj instanceof Method)) {
            Method method = (Method) obj;
            if (getDeclaringClass() != method.getDeclaringClass() || getName() != method.getName() || !getReturnType().equals(method.getReturnType())) {
                return false;
            }
            return equalParamTypes(getParameterTypes(), method.getParameterTypes());
        }
        return false;
    }

    public int hashCode() {
        return getDeclaringClass().getName().hashCode() ^ getName().hashCode();
    }

    public String toString() {
        return sharedToString(Modifier.methodModifiers(), isDefault(), getParameterTypes(), getExceptionTypes());
    }

    @Override
    void specificToStringHeader(StringBuilder sb) {
        sb.append(getReturnType().getTypeName());
        sb.append(' ');
        sb.append(getDeclaringClass().getTypeName());
        sb.append('.');
        sb.append(getName());
    }

    @Override
    public String toGenericString() {
        return sharedToGenericString(Modifier.methodModifiers(), isDefault());
    }

    @Override
    void specificToGenericStringHeader(StringBuilder sb) {
        sb.append(getGenericReturnType().getTypeName());
        sb.append(' ');
        sb.append(getDeclaringClass().getTypeName());
        sb.append('.');
        sb.append(getName());
    }

    public boolean isBridge() {
        return super.isBridgeMethodInternal();
    }

    @Override
    public boolean isVarArgs() {
        return super.isVarArgs();
    }

    @Override
    public boolean isSynthetic() {
        return super.isSynthetic();
    }

    public boolean isDefault() {
        return super.isDefaultMethodInternal();
    }

    @Override
    public <T extends Annotation> T getAnnotation(Class<T> cls) {
        return (T) super.getAnnotation(cls);
    }

    @Override
    public Annotation[] getDeclaredAnnotations() {
        return super.getDeclaredAnnotations();
    }

    @Override
    public Annotation[][] getParameterAnnotations() {
        return super.getParameterAnnotationsInternal();
    }

    boolean equalNameAndParameters(Method method) {
        return equalNameAndParametersInternal(method);
    }
}
