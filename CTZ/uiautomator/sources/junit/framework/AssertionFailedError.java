package junit.framework;

public class AssertionFailedError extends AssertionError {
    private static final long serialVersionUID = 1;

    public AssertionFailedError() {
    }

    public AssertionFailedError(String str) {
        super(defaultString(str));
    }

    private static String defaultString(String str) {
        return str == null ? "" : str;
    }
}
