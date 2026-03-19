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
    public static <T extends Annotation> T[] getDirectOrIndirectAnnotationsByType(AnnotatedElement annotatedElement, Class<T> cls) {
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

    private static <T extends Annotation> void insertAnnotationValues(Annotation annotation, Class<T> cls, ArrayList<T> arrayList) {
        ((Annotation[]) Array.newInstance((Class<?>) cls, 0)).getClass();
        try {
            Method declaredMethod = annotation.getClass().getDeclaredMethod("value", new Class[0]);
            if (!declaredMethod.getReturnType().isArray()) {
                throw new AssertionError("annotation container = " + annotation + "annotation element class = " + cls + "; value() doesn't return array");
            }
            if (!cls.equals(declaredMethod.getReturnType().getComponentType())) {
                throw new AssertionError("annotation container = " + annotation + "annotation element class = " + cls + "; value() returns incorrect type");
            }
            try {
                for (Annotation annotation2 : (Annotation[]) declaredMethod.invoke(annotation, new Object[0])) {
                    arrayList.add(annotation2);
                }
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new AssertionError(e);
            }
        } catch (NoSuchMethodException e2) {
            throw new AssertionError("annotation container = " + annotation + "annotation element class = " + cls + "; missing value() method");
        } catch (SecurityException e3) {
            throw new IncompleteAnnotationException(annotation.getClass(), "value");
        }
    }

    private static <T extends Annotation> Class<? extends Annotation> getRepeatableAnnotationContainerClassFor(Class<T> cls) {
        Repeatable repeatable = (Repeatable) cls.getDeclaredAnnotation(Repeatable.class);
        if (repeatable == null) {
            return null;
        }
        return repeatable.value();
    }

    private AnnotatedElements() {
    }
}
