package java.lang.reflect;

import java.lang.annotation.Annotation;

public class AccessibleObject implements AnnotatedElement {
    boolean override;

    public static void setAccessible(AccessibleObject[] accessibleObjectArr, boolean z) throws SecurityException {
        for (AccessibleObject accessibleObject : accessibleObjectArr) {
            setAccessible0(accessibleObject, z);
        }
    }

    public void setAccessible(boolean z) throws SecurityException {
        setAccessible0(this, z);
    }

    private static void setAccessible0(AccessibleObject accessibleObject, boolean z) throws SecurityException {
        if ((accessibleObject instanceof Constructor) && z) {
            Constructor constructor = (Constructor) accessibleObject;
            Class declaringClass = constructor.getDeclaringClass();
            if (constructor.getDeclaringClass() == Class.class) {
                throw new SecurityException("Can not make a java.lang.Class constructor accessible");
            }
            if (declaringClass == Method.class) {
                throw new SecurityException("Can not make a java.lang.reflect.Method constructor accessible");
            }
            if (declaringClass == Field.class) {
                throw new SecurityException("Can not make a java.lang.reflect.Field constructor accessible");
            }
        }
        accessibleObject.override = z;
    }

    public boolean isAccessible() {
        return this.override;
    }

    protected AccessibleObject() {
    }

    @Override
    public <T extends Annotation> T getAnnotation(Class<T> cls) {
        throw new AssertionError((Object) "All subclasses should override this method");
    }

    @Override
    public boolean isAnnotationPresent(Class<? extends Annotation> cls) {
        return super.isAnnotationPresent(cls);
    }

    @Override
    public <T extends Annotation> T[] getAnnotationsByType(Class<T> cls) {
        throw new AssertionError((Object) "All subclasses should override this method");
    }

    @Override
    public Annotation[] getAnnotations() {
        return getDeclaredAnnotations();
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
    public Annotation[] getDeclaredAnnotations() {
        throw new AssertionError((Object) "All subclasses should override this method");
    }
}
