package java.lang.reflect;

import java.lang.annotation.Annotation;
import java.util.List;
import libcore.reflect.AnnotationAccess;
import libcore.reflect.GenericSignatureParser;
import libcore.reflect.ListOfTypes;
import libcore.reflect.Types;

public abstract class AbstractMethod extends AccessibleObject {
    protected final ArtMethod artMethod;

    public abstract String getName();

    public abstract Annotation[][] getParameterAnnotations();

    abstract String getSignature();

    protected AbstractMethod(ArtMethod artMethod) {
        if (artMethod == null) {
            throw new NullPointerException("artMethod == null");
        }
        this.artMethod = artMethod;
    }

    @Override
    public <T extends Annotation> T getAnnotation(Class<T> cls) {
        return (T) super.getAnnotation(cls);
    }

    private static int fixMethodFlags(int flags) {
        if ((flags & 1024) != 0) {
            flags &= -257;
        }
        int flags2 = flags & (-33);
        if ((flags2 & 131072) != 0) {
            flags2 |= 32;
        }
        return 65535 & flags2;
    }

    int getModifiers() {
        return fixMethodFlags(this.artMethod.getAccessFlags());
    }

    boolean isVarArgs() {
        return (this.artMethod.getAccessFlags() & 128) != 0;
    }

    boolean isBridge() {
        return (this.artMethod.getAccessFlags() & 64) != 0;
    }

    boolean isSynthetic() {
        return (this.artMethod.getAccessFlags() & 4096) != 0;
    }

    public final int getAccessFlags() {
        return this.artMethod.getAccessFlags();
    }

    Class<?> getDeclaringClass() {
        return this.artMethod.getDeclaringClass();
    }

    public final int getDexMethodIndex() {
        return this.artMethod.getDexMethodIndex();
    }

    Class<?>[] getParameterTypes() {
        return this.artMethod.getParameterTypes();
    }

    public boolean equals(Object other) {
        return (other instanceof AbstractMethod) && this.artMethod == ((AbstractMethod) other).artMethod;
    }

    String toGenericString() {
        return toGenericStringHelper();
    }

    Type[] getGenericParameterTypes() {
        return Types.getTypeArray(getMethodOrConstructorGenericInfo().genericParameterTypes, false);
    }

    Type[] getGenericExceptionTypes() {
        return Types.getTypeArray(getMethodOrConstructorGenericInfo().genericExceptionTypes, false);
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
    public Annotation[] getAnnotations() {
        return super.getAnnotations();
    }

    static final class GenericInfo {
        final TypeVariable<?>[] formalTypeParameters;
        final ListOfTypes genericExceptionTypes;
        final ListOfTypes genericParameterTypes;
        final Type genericReturnType;

        GenericInfo(ListOfTypes exceptions, ListOfTypes parameters, Type ret, TypeVariable<?>[] formal) {
            this.genericExceptionTypes = exceptions;
            this.genericParameterTypes = parameters;
            this.genericReturnType = ret;
            this.formalTypeParameters = formal;
        }
    }

    final GenericInfo getMethodOrConstructorGenericInfo() {
        Member member;
        Class<?>[] exceptionTypes;
        String signatureAttribute = AnnotationAccess.getSignature(this);
        boolean method = this instanceof Method;
        if (method) {
            Method m = (Method) this;
            member = m;
            exceptionTypes = m.getExceptionTypes();
        } else {
            Constructor<?> c = (Constructor) this;
            member = c;
            exceptionTypes = c.getExceptionTypes();
        }
        GenericSignatureParser parser = new GenericSignatureParser(member.getDeclaringClass().getClassLoader());
        if (method) {
            parser.parseForMethod((GenericDeclaration) this, signatureAttribute, exceptionTypes);
        } else {
            parser.parseForConstructor((GenericDeclaration) this, signatureAttribute, exceptionTypes);
        }
        return new GenericInfo(parser.exceptionTypes, parser.parameterTypes, parser.returnType, parser.formalTypeParameters);
    }

    final String toGenericStringHelper() {
        StringBuilder sb = new StringBuilder(80);
        GenericInfo info = getMethodOrConstructorGenericInfo();
        int modifiers = ((Member) this).getModifiers();
        if (modifiers != 0) {
            sb.append(Modifier.toString(modifiers & (-129))).append(' ');
        }
        if (info.formalTypeParameters != null && info.formalTypeParameters.length > 0) {
            sb.append('<');
            for (int i = 0; i < info.formalTypeParameters.length; i++) {
                Types.appendGenericType(sb, info.formalTypeParameters[i]);
                if (i < info.formalTypeParameters.length - 1) {
                    sb.append(",");
                }
            }
            sb.append("> ");
        }
        Class<?> declaringClass = ((Member) this).getDeclaringClass();
        if (this instanceof Constructor) {
            Types.appendTypeName(sb, declaringClass);
        } else {
            Types.appendGenericType(sb, Types.getType(info.genericReturnType));
            sb.append(' ');
            Types.appendTypeName(sb, declaringClass);
            sb.append(".").append(((Method) this).getName());
        }
        sb.append('(');
        Types.appendArrayGenericType(sb, info.genericParameterTypes.getResolvedTypes());
        sb.append(')');
        Type[] genericExceptionTypeArray = Types.getTypeArray(info.genericExceptionTypes, false);
        if (genericExceptionTypeArray.length > 0) {
            sb.append(" throws ");
            Types.appendArrayGenericType(sb, genericExceptionTypeArray);
        }
        return sb.toString();
    }
}
