package java.lang.reflect;

import java.lang.annotation.Annotation;
import java.util.Objects;
import libcore.reflect.AnnotatedElements;

public interface AnnotatedElement {
    <T extends Annotation> T getAnnotation(Class<T> cls);

    Annotation[] getAnnotations();

    Annotation[] getDeclaredAnnotations();

    default boolean isAnnotationPresent(Class<? extends Annotation> cls) {
        return getAnnotation(cls) != null;
    }

    default <T extends Annotation> T[] getAnnotationsByType(Class<T> cls) {
        return (T[]) AnnotatedElements.getDirectOrIndirectAnnotationsByType(this, cls);
    }

    default <T extends Annotation> T getDeclaredAnnotation(Class<T> cls) {
        Objects.requireNonNull(cls);
        for (Annotation annotation : getDeclaredAnnotations()) {
            if (cls.equals(annotation.annotationType())) {
                return cls.cast(annotation);
            }
        }
        return null;
    }

    default <T extends Annotation> T[] getDeclaredAnnotationsByType(Class<T> cls) {
        return (T[]) AnnotatedElements.getDirectOrIndirectAnnotationsByType(this, cls);
    }
}
