package java.lang.reflect;

import java.lang.annotation.Annotation;

public class AccessibleObject implements AnnotatedElement {
    private boolean flag = false;

    protected AccessibleObject() {
    }

    public boolean isAccessible() {
        return this.flag;
    }

    public void setAccessible(boolean flag) {
        try {
            if (equals(Class.class.getDeclaredConstructor(new Class[0]))) {
                throw new SecurityException("Can't make class constructor accessible");
            }
            this.flag = flag;
        } catch (NoSuchMethodException e) {
            throw new AssertionError("Couldn't find class constructor");
        }
    }

    public static void setAccessible(AccessibleObject[] objects, boolean flag) {
        for (AccessibleObject object : objects) {
            object.flag = flag;
        }
    }

    @Override
    public boolean isAnnotationPresent(Class<? extends Annotation> annotationType) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Annotation[] getDeclaredAnnotations() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Annotation[] getAnnotations() {
        return getDeclaredAnnotations();
    }

    @Override
    public <T extends Annotation> T getAnnotation(Class<T> annotationType) {
        throw new UnsupportedOperationException();
    }
}
