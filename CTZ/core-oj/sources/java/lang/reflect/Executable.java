package java.lang.reflect;

import dalvik.annotation.optimization.FastNative;
import java.lang.annotation.Annotation;
import java.util.Objects;
import libcore.reflect.AnnotatedElements;
import libcore.reflect.GenericSignatureParser;
import libcore.reflect.ListOfTypes;
import libcore.reflect.Types;

public abstract class Executable extends AccessibleObject implements Member, GenericDeclaration {
    private int accessFlags;
    private long artMethod;
    private Class<?> declaringClass;
    private Class<?> declaringClassOfOverriddenMethod;
    private int dexMethodIndex;
    private volatile transient boolean hasRealParameterData;
    private volatile transient Parameter[] parameters;

    @FastNative
    private native <T extends Annotation> T getAnnotationNative(Class<T> cls);

    @FastNative
    private native Annotation[] getDeclaredAnnotationsNative();

    @FastNative
    private native Annotation[][] getParameterAnnotationsNative();

    @FastNative
    private native Parameter[] getParameters0();

    @FastNative
    private native String[] getSignatureAnnotation();

    @FastNative
    private native boolean isAnnotationPresentNative(Class<? extends Annotation> cls);

    @FastNative
    native int compareMethodParametersInternal(Method method);

    public abstract Class<?> getDeclaringClass();

    public abstract Class<?>[] getExceptionTypes();

    @FastNative
    final native String getMethodNameInternal();

    @FastNative
    final native Class<?> getMethodReturnTypeInternal();

    public abstract int getModifiers();

    public abstract String getName();

    public abstract Annotation[][] getParameterAnnotations();

    @FastNative
    final native int getParameterCountInternal();

    public abstract Class<?>[] getParameterTypes();

    @FastNative
    final native Class<?>[] getParameterTypesInternal();

    public abstract TypeVariable<?>[] getTypeParameters();

    abstract boolean hasGenericInformation();

    abstract void specificToGenericStringHeader(StringBuilder sb);

    abstract void specificToStringHeader(StringBuilder sb);

    public abstract String toGenericString();

    Executable() {
    }

    boolean equalParamTypes(Class<?>[] clsArr, Class<?>[] clsArr2) {
        if (clsArr.length != clsArr2.length) {
            return false;
        }
        for (int i = 0; i < clsArr.length; i++) {
            if (clsArr[i] != clsArr2[i]) {
                return false;
            }
        }
        return true;
    }

    void separateWithCommas(Class<?>[] clsArr, StringBuilder sb) {
        for (int i = 0; i < clsArr.length; i++) {
            sb.append(clsArr[i].getTypeName());
            if (i < clsArr.length - 1) {
                sb.append(",");
            }
        }
    }

    void printModifiersIfNonzero(StringBuilder sb, int i, boolean z) {
        int modifiers = i & getModifiers();
        if (modifiers != 0 && !z) {
            sb.append(Modifier.toString(modifiers));
            sb.append(' ');
            return;
        }
        int i2 = modifiers & 7;
        if (i2 != 0) {
            sb.append(Modifier.toString(i2));
            sb.append(' ');
        }
        if (z) {
            sb.append("default ");
        }
        int i3 = modifiers & (-8);
        if (i3 != 0) {
            sb.append(Modifier.toString(i3));
            sb.append(' ');
        }
    }

    String sharedToString(int i, boolean z, Class<?>[] clsArr, Class<?>[] clsArr2) {
        try {
            StringBuilder sb = new StringBuilder();
            printModifiersIfNonzero(sb, i, z);
            specificToStringHeader(sb);
            sb.append('(');
            separateWithCommas(clsArr, sb);
            sb.append(')');
            if (clsArr2.length > 0) {
                sb.append(" throws ");
                separateWithCommas(clsArr2, sb);
            }
            return sb.toString();
        } catch (Exception e) {
            return "<" + ((Object) e) + ">";
        }
    }

    String sharedToGenericString(int i, boolean z) {
        String string;
        try {
            StringBuilder sb = new StringBuilder();
            printModifiersIfNonzero(sb, i, z);
            TypeVariable<?>[] typeParameters = getTypeParameters();
            if (typeParameters.length > 0) {
                sb.append('<');
                int length = typeParameters.length;
                int i2 = 0;
                boolean z2 = true;
                while (i2 < length) {
                    TypeVariable<?> typeVariable = typeParameters[i2];
                    if (!z2) {
                        sb.append(',');
                    }
                    sb.append(typeVariable.toString());
                    i2++;
                    z2 = false;
                }
                sb.append("> ");
            }
            specificToGenericStringHeader(sb);
            sb.append('(');
            Type[] genericParameterTypes = getGenericParameterTypes();
            for (int i3 = 0; i3 < genericParameterTypes.length; i3++) {
                String typeName = genericParameterTypes[i3].getTypeName();
                if (isVarArgs() && i3 == genericParameterTypes.length - 1) {
                    typeName = typeName.replaceFirst("\\[\\]$", "...");
                }
                sb.append(typeName);
                if (i3 < genericParameterTypes.length - 1) {
                    sb.append(',');
                }
            }
            sb.append(')');
            Type[] genericExceptionTypes = getGenericExceptionTypes();
            if (genericExceptionTypes.length > 0) {
                sb.append(" throws ");
                for (int i4 = 0; i4 < genericExceptionTypes.length; i4++) {
                    if (genericExceptionTypes[i4] instanceof Class) {
                        string = ((Class) genericExceptionTypes[i4]).getName();
                    } else {
                        string = genericExceptionTypes[i4].toString();
                    }
                    sb.append(string);
                    if (i4 < genericExceptionTypes.length - 1) {
                        sb.append(',');
                    }
                }
            }
            return sb.toString();
        } catch (Exception e) {
            return "<" + ((Object) e) + ">";
        }
    }

    public int getParameterCount() {
        throw new AbstractMethodError();
    }

    public Type[] getGenericParameterTypes() {
        return Types.getTypeArray(getMethodOrConstructorGenericInfoInternal().genericParameterTypes, false);
    }

    Type[] getAllGenericParameterTypes() {
        if (!hasGenericInformation()) {
            return getParameterTypes();
        }
        boolean zHasRealParameterData = hasRealParameterData();
        Type[] genericParameterTypes = getGenericParameterTypes();
        Class<?>[] parameterTypes = getParameterTypes();
        Type[] typeArr = new Type[parameterTypes.length];
        Parameter[] parameters = getParameters();
        if (!zHasRealParameterData) {
            return genericParameterTypes.length == parameterTypes.length ? genericParameterTypes : parameterTypes;
        }
        int i = 0;
        for (int i2 = 0; i2 < typeArr.length; i2++) {
            Parameter parameter = parameters[i2];
            if (parameter.isSynthetic() || parameter.isImplicit()) {
                typeArr[i2] = parameterTypes[i2];
            } else {
                typeArr[i2] = genericParameterTypes[i];
                i++;
            }
        }
        return typeArr;
    }

    public Parameter[] getParameters() {
        return (Parameter[]) privateGetParameters().clone();
    }

    private Parameter[] synthesizeAllParams() {
        int parameterCount = getParameterCount();
        Parameter[] parameterArr = new Parameter[parameterCount];
        for (int i = 0; i < parameterCount; i++) {
            parameterArr[i] = new Parameter("arg" + i, 0, this, i);
        }
        return parameterArr;
    }

    private void verifyParameters(Parameter[] parameterArr) {
        if (getParameterTypes().length != parameterArr.length) {
            throw new MalformedParametersException("Wrong number of parameters in MethodParameters attribute");
        }
        for (Parameter parameter : parameterArr) {
            String realName = parameter.getRealName();
            int modifiers = parameter.getModifiers();
            if (realName != null && (realName.isEmpty() || realName.indexOf(46) != -1 || realName.indexOf(59) != -1 || realName.indexOf(91) != -1 || realName.indexOf(47) != -1)) {
                throw new MalformedParametersException("Invalid parameter name \"" + realName + "\"");
            }
            if (modifiers != (36880 & modifiers)) {
                throw new MalformedParametersException("Invalid parameter modifiers");
            }
        }
    }

    private Parameter[] privateGetParameters() {
        Parameter[] parameters0 = this.parameters;
        if (parameters0 == null) {
            try {
                parameters0 = getParameters0();
                if (parameters0 == null) {
                    this.hasRealParameterData = false;
                    parameters0 = synthesizeAllParams();
                } else {
                    this.hasRealParameterData = true;
                    verifyParameters(parameters0);
                }
                this.parameters = parameters0;
            } catch (IllegalArgumentException e) {
                MalformedParametersException malformedParametersException = new MalformedParametersException("Invalid parameter metadata in class file");
                malformedParametersException.initCause(e);
                throw malformedParametersException;
            }
        }
        return parameters0;
    }

    boolean hasRealParameterData() {
        if (this.parameters == null) {
            privateGetParameters();
        }
        return this.hasRealParameterData;
    }

    public Type[] getGenericExceptionTypes() {
        return Types.getTypeArray(getMethodOrConstructorGenericInfoInternal().genericExceptionTypes, false);
    }

    public boolean isVarArgs() {
        return (this.accessFlags & 128) != 0;
    }

    public boolean isSynthetic() {
        return (this.accessFlags & 4096) != 0;
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
    public Annotation[] getDeclaredAnnotations() {
        return getDeclaredAnnotationsNative();
    }

    private static int fixMethodFlags(int i) {
        if ((i & 1024) != 0) {
            i &= -257;
        }
        int i2 = i & (-33);
        if ((131072 & i2) != 0) {
            i2 |= 32;
        }
        return i2 & 65535;
    }

    final int getModifiersInternal() {
        return fixMethodFlags(this.accessFlags);
    }

    final Class<?> getDeclaringClassInternal() {
        return this.declaringClass;
    }

    @Override
    public final boolean isAnnotationPresent(Class<? extends Annotation> cls) {
        Objects.requireNonNull(cls);
        return isAnnotationPresentNative(cls);
    }

    final Annotation[][] getParameterAnnotationsInternal() {
        Annotation[][] parameterAnnotationsNative = getParameterAnnotationsNative();
        if (parameterAnnotationsNative == null) {
            return (Annotation[][]) Array.newInstance((Class<?>) Annotation.class, getParameterTypes().length, 0);
        }
        return parameterAnnotationsNative;
    }

    public final int getAccessFlags() {
        return this.accessFlags;
    }

    public final long getArtMethod() {
        return this.artMethod;
    }

    static final class GenericInfo {
        final TypeVariable<?>[] formalTypeParameters;
        final ListOfTypes genericExceptionTypes;
        final ListOfTypes genericParameterTypes;
        final Type genericReturnType;

        GenericInfo(ListOfTypes listOfTypes, ListOfTypes listOfTypes2, Type type, TypeVariable<?>[] typeVariableArr) {
            this.genericExceptionTypes = listOfTypes;
            this.genericParameterTypes = listOfTypes2;
            this.genericReturnType = type;
            this.formalTypeParameters = typeVariableArr;
        }
    }

    final boolean hasGenericInformationInternal() {
        return getSignatureAnnotation() != null;
    }

    final GenericInfo getMethodOrConstructorGenericInfoInternal() {
        String signatureAttribute = getSignatureAttribute();
        Class<?>[] exceptionTypes = getExceptionTypes();
        GenericSignatureParser genericSignatureParser = new GenericSignatureParser(getDeclaringClass().getClassLoader());
        if (this instanceof Method) {
            genericSignatureParser.parseForMethod(this, signatureAttribute, exceptionTypes);
        } else {
            genericSignatureParser.parseForConstructor(this, signatureAttribute, exceptionTypes);
        }
        return new GenericInfo(genericSignatureParser.exceptionTypes, genericSignatureParser.parameterTypes, genericSignatureParser.returnType, genericSignatureParser.formalTypeParameters);
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

    final boolean equalNameAndParametersInternal(Method method) {
        return getName().equals(method.getName()) && compareMethodParametersInternal(method) == 0;
    }

    final boolean isDefaultMethodInternal() {
        return (this.accessFlags & Modifier.DEFAULT) != 0;
    }

    final boolean isBridgeMethodInternal() {
        return (this.accessFlags & 64) != 0;
    }
}
