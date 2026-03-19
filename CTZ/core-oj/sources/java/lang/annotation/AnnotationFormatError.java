package java.lang.annotation;

public class AnnotationFormatError extends Error {
    private static final long serialVersionUID = -4256701562333669892L;

    public AnnotationFormatError(String str) {
        super(str);
    }

    public AnnotationFormatError(String str, Throwable th) {
        super(str, th);
    }

    public AnnotationFormatError(Throwable th) {
        super(th);
    }
}
