package dalvik.system;

final class TransactionAbortError extends InternalError {
    private TransactionAbortError() {
    }

    private TransactionAbortError(String str) {
        super(str);
    }

    private TransactionAbortError(String str, Throwable th) {
        super(str);
        initCause(th);
    }

    private TransactionAbortError(Throwable th) {
        this(th == null ? null : th.toString(), th);
    }
}
