package java.lang.annotation;

import java.lang.reflect.Method;

public class AnnotationTypeMismatchException extends RuntimeException {
    private static final long serialVersionUID = 8125925355765570191L;
    private final Method element;
    private final String foundType;

    public AnnotationTypeMismatchException(Method method, String str) {
        super("Incorrectly typed data found for annotation element " + ((Object) method) + " (Found data of type " + str + ")");
        this.element = method;
        this.foundType = str;
    }

    public Method element() {
        return this.element;
    }

    public String foundType() {
        return this.foundType;
    }
}
