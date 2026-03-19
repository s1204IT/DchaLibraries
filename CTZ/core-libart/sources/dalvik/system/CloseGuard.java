package dalvik.system;

public final class CloseGuard {
    private Object closerNameOrAllocationInfo;
    private static volatile boolean stackAndTrackingEnabled = true;
    private static volatile Reporter reporter = new DefaultReporter();
    private static volatile Tracker currentTracker = null;

    public interface Reporter {
        void report(String str, Throwable th);
    }

    public interface Tracker {
        void close(Throwable th);

        void open(Throwable th);
    }

    public static CloseGuard get() {
        return new CloseGuard();
    }

    public static void setEnabled(boolean z) {
        stackAndTrackingEnabled = z;
    }

    public static boolean isEnabled() {
        return stackAndTrackingEnabled;
    }

    public static void setReporter(Reporter reporter2) {
        if (reporter2 == null) {
            throw new NullPointerException("reporter == null");
        }
        reporter = reporter2;
    }

    public static Reporter getReporter() {
        return reporter;
    }

    public static void setTracker(Tracker tracker) {
        currentTracker = tracker;
    }

    public static Tracker getTracker() {
        return currentTracker;
    }

    private CloseGuard() {
    }

    public void open(String str) {
        if (str == null) {
            throw new NullPointerException("closer == null");
        }
        if (!stackAndTrackingEnabled) {
            this.closerNameOrAllocationInfo = str;
            return;
        }
        Throwable th = new Throwable("Explicit termination method '" + str + "' not called");
        this.closerNameOrAllocationInfo = th;
        Tracker tracker = currentTracker;
        if (tracker != null) {
            tracker.open(th);
        }
    }

    public void close() {
        Tracker tracker = currentTracker;
        if (tracker != null && (this.closerNameOrAllocationInfo instanceof Throwable)) {
            tracker.close((Throwable) this.closerNameOrAllocationInfo);
        }
        this.closerNameOrAllocationInfo = null;
    }

    public void warnIfOpen() {
        if (this.closerNameOrAllocationInfo != null) {
            if (this.closerNameOrAllocationInfo instanceof String) {
                System.logW("A resource failed to call " + ((String) this.closerNameOrAllocationInfo) + ". ");
                return;
            }
            reporter.report("A resource was acquired at attached stack trace but never released. See java.io.Closeable for information on avoiding resource leaks.", (Throwable) this.closerNameOrAllocationInfo);
        }
    }

    private static final class DefaultReporter implements Reporter {
        private DefaultReporter() {
        }

        @Override
        public void report(String str, Throwable th) {
            System.logW(str, th);
        }
    }
}
