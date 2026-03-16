package java.lang.annotation;

import java.lang.reflect.Method;

public class AnnotationTypeMismatchException extends RuntimeException {
    private static final long serialVersionUID = 8125925355765570191L;
    private Method element;
    private String foundType;

    public AnnotationTypeMismatchException(Method element, String foundType) {
        super("The annotation element " + element + " doesn't match the type " + foundType);
        this.element = element;
        this.foundType = foundType;
    }

    public Method element() {
        return this.element;
    }

    public String foundType() {
        return this.foundType;
    }
}
