package libcore.reflect;

import java.lang.annotation.Annotation;
import java.lang.annotation.IncompleteAnnotationException;
import java.lang.annotation.Repeatable;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;

public final class AnnotatedElements {
    public static <T extends Annotation> T getDeclaredAnnotation(AnnotatedElement annotatedElement, Class<T> cls) {
        if (cls == null) {
            throw new NullPointerException("annotationClass");
        }
        Annotation[] declaredAnnotations = annotatedElement.getDeclaredAnnotations();
        if (declaredAnnotations == null) {
            return null;
        }
        for (int i = 0; i < declaredAnnotations.length; i++) {
            if (cls.isInstance(declaredAnnotations[i])) {
                return (T) declaredAnnotations[i];
            }
        }
        return null;
    }

    public static <T extends Annotation> T[] getDeclaredAnnotationsByType(AnnotatedElement annotatedElement, Class<T> cls) {
        if (cls == null) {
            throw new NullPointerException("annotationClass");
        }
        Annotation[] declaredAnnotations = annotatedElement.getDeclaredAnnotations();
        ArrayList arrayList = new ArrayList();
        Class<? extends Annotation> repeatableAnnotationContainerClassFor = getRepeatableAnnotationContainerClassFor(cls);
        for (int i = 0; i < declaredAnnotations.length; i++) {
            if (cls.isInstance(declaredAnnotations[i])) {
                arrayList.add(declaredAnnotations[i]);
            } else if (repeatableAnnotationContainerClassFor != null && repeatableAnnotationContainerClassFor.isInstance(declaredAnnotations[i])) {
                insertAnnotationValues(declaredAnnotations[i], cls, arrayList);
            }
        }
        return (T[]) ((Annotation[]) arrayList.toArray((Annotation[]) Array.newInstance((Class<?>) cls, 0)));
    }

    private static <T extends Annotation> void insertAnnotationValues(Annotation annotation, Class<T> annotationClass, ArrayList<T> arrayList) {
        ((Annotation[]) Array.newInstance((Class<?>) annotationClass, 0)).getClass();
        try {
            Method valuesMethod = annotation.getClass().getDeclaredMethod("value", new Class[0]);
            if (!valuesMethod.getReturnType().isArray()) {
                throw new AssertionError("annotation container = " + annotation + "annotation element class = " + annotationClass + "; value() doesn't return array");
            }
            if (!annotationClass.equals(valuesMethod.getReturnType().getComponentType())) {
                throw new AssertionError("annotation container = " + annotation + "annotation element class = " + annotationClass + "; value() returns incorrect type");
            }
            try {
                for (Annotation annotation2 : (Annotation[]) valuesMethod.invoke(annotation, new Object[0])) {
                    arrayList.add(annotation2);
                }
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new AssertionError(e);
            }
        } catch (NoSuchMethodException e2) {
            throw new AssertionError("annotation container = " + annotation + "annotation element class = " + annotationClass + "; missing value() method");
        } catch (SecurityException e3) {
            throw new IncompleteAnnotationException(annotation.getClass(), "value");
        }
    }

    private static <T extends Annotation> Class<? extends Annotation> getRepeatableAnnotationContainerClassFor(Class<T> annotationClass) {
        Repeatable repeatableAnnotation = (Repeatable) annotationClass.getDeclaredAnnotation(Repeatable.class);
        if (repeatableAnnotation == null) {
            return null;
        }
        return repeatableAnnotation.value();
    }

    public static <T extends Annotation> T[] getAnnotationsByType(AnnotatedElement annotatedElement, Class<T> cls) {
        if (cls == null) {
            throw new NullPointerException("annotationClass");
        }
        T[] tArr = (T[]) annotatedElement.getDeclaredAnnotationsByType(cls);
        if (tArr == null) {
            throw new AssertionError("annotations must not be null");
        }
        return tArr;
    }

    private AnnotatedElements() {
        throw new AssertionError("Instances of AnnotatedElements not allowed");
    }
}
