package android.test;

public class AssertionFailedError extends Error {
    public AssertionFailedError() {
    }

    public AssertionFailedError(String errorMessage) {
        super(errorMessage);
    }
}
