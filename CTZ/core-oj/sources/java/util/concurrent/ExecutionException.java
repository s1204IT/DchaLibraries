package java.util.concurrent;

public class ExecutionException extends Exception {
    private static final long serialVersionUID = 7830266012832686185L;

    protected ExecutionException() {
    }

    protected ExecutionException(String str) {
        super(str);
    }

    public ExecutionException(String str, Throwable th) {
        super(str, th);
    }

    public ExecutionException(Throwable th) {
        super(th);
    }
}
