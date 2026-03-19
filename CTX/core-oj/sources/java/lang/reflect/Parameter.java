package java.lang.reflect;

import dalvik.annotation.optimization.FastNative;
import java.lang.annotation.Annotation;
import java.util.Objects;
import libcore.reflect.AnnotatedElements;

public final class Parameter implements AnnotatedElement {
    private final Executable executable;
    private final int index;
    private final int modifiers;
    private final String name;
    private volatile transient Type parameterTypeCache = null;
    private volatile transient Class<?> parameterClassCache = null;

    @FastNative
    private static native <A extends Annotation> A getAnnotationNative(Executable executable, int i, Class<A> cls);

    Parameter(String str, int i, Executable executable, int i2) {
        this.name = str;
        this.modifiers = i;
        this.executable = executable;
        this.index = i2;
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof Parameter)) {
            return false;
        }
        Parameter parameter = (Parameter) obj;
        return parameter.executable.equals(this.executable) && parameter.index == this.index;
    }

    public int hashCode() {
        return this.executable.hashCode() ^ this.index;
    }

    public boolean isNamePresent() {
        return this.executable.hasRealParameterData() && this.name != null;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        String typeName = getParameterizedType().getTypeName();
        sb.append(Modifier.toString(getModifiers()));
        if (this.modifiers != 0) {
            sb.append(' ');
        }
        if (isVarArgs()) {
            sb.append(typeName.replaceFirst("\\[\\]$", "..."));
        } else {
            sb.append(typeName);
        }
        sb.append(' ');
        sb.append(getName());
        return sb.toString();
    }

    public Executable getDeclaringExecutable() {
        return this.executable;
    }

    public int getModifiers() {
        return this.modifiers;
    }

    public String getName() {
        if (this.name == null || this.name.equals("")) {
            return "arg" + this.index;
        }
        return this.name;
    }

    String getRealName() {
        return this.name;
    }

    public Type getParameterizedType() {
        Type type = this.parameterTypeCache;
        if (type == null) {
            Type type2 = this.executable.getAllGenericParameterTypes()[this.index];
            this.parameterTypeCache = type2;
            return type2;
        }
        return type;
    }

    public Class<?> getType() {
        Class<?> cls = this.parameterClassCache;
        if (cls == null) {
            Class<?> cls2 = this.executable.getParameterTypes()[this.index];
            this.parameterClassCache = cls2;
            return cls2;
        }
        return cls;
    }

    public boolean isImplicit() {
        return Modifier.isMandated(getModifiers());
    }

    public boolean isSynthetic() {
        return Modifier.isSynthetic(getModifiers());
    }

    public boolean isVarArgs() {
        return this.executable.isVarArgs() && this.index == this.executable.getParameterCount() - 1;
    }

    @Override
    public <T extends Annotation> T getAnnotation(Class<T> cls) {
        Objects.requireNonNull(cls);
        return (T) getAnnotationNative(this.executable, this.index, cls);
    }

    @Override
    public <T extends Annotation> T[] getAnnotationsByType(Class<T> cls) {
        return (T[]) AnnotatedElements.getDirectOrIndirectAnnotationsByType(this, cls);
    }

    @Override
    public Annotation[] getDeclaredAnnotations() {
        return this.executable.getParameterAnnotations()[this.index];
    }

    @Override
    public <T extends Annotation> T getDeclaredAnnotation(Class<T> cls) {
        return (T) getAnnotation(cls);
    }

    @Override
    public <T extends Annotation> T[] getDeclaredAnnotationsByType(Class<T> cls) {
        return (T[]) getAnnotationsByType(cls);
    }

    @Override
    public Annotation[] getAnnotations() {
        return getDeclaredAnnotations();
    }
}
