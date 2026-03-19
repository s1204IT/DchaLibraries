package dalvik.system;

public final class CloseGuard {
    private Throwable allocationSite;
    private static final CloseGuard NOOP = new CloseGuard();
    private static volatile boolean ENABLED = true;
    private static volatile Reporter REPORTER = new DefaultReporter(null);

    public interface Reporter {
        void report(String str, Throwable th);
    }

    public static CloseGuard get() {
        if (!ENABLED) {
            return NOOP;
        }
        return new CloseGuard();
    }

    public static void setEnabled(boolean enabled) {
        ENABLED = enabled;
    }

    public static void setReporter(Reporter reporter) {
        if (reporter == null) {
            throw new NullPointerException("reporter == null");
        }
        REPORTER = reporter;
    }

    public static Reporter getReporter() {
        return REPORTER;
    }

    private CloseGuard() {
    }

    public void open(String closer) {
        if (closer == null) {
            throw new NullPointerException("closer == null");
        }
        if (this == NOOP || !ENABLED) {
            return;
        }
        String message = "Explicit termination method '" + closer + "' not called";
        this.allocationSite = new Throwable(message);
    }

    public void close() {
        this.allocationSite = null;
    }

    public void warnIfOpen() {
        if (this.allocationSite == null || !ENABLED) {
            return;
        }
        REPORTER.report("A resource was acquired at attached stack trace but never released. See java.io.Closeable for information on avoiding resource leaks.", this.allocationSite);
    }

    private static final class DefaultReporter implements Reporter {
        DefaultReporter(DefaultReporter defaultReporter) {
            this();
        }

        private DefaultReporter() {
        }

        @Override
        public void report(String message, Throwable allocationSite) {
            System.logW(message, allocationSite);
        }
    }
}
