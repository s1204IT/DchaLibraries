package libcore.io;

public final class EventLogger {
    private static volatile Reporter REPORTER = new DefaultReporter();

    public interface Reporter {
        void report(int i, Object... objArr);
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

    private static final class DefaultReporter implements Reporter {
        private DefaultReporter() {
        }

        @Override
        public void report(int i, Object... objArr) {
            StringBuilder sb = new StringBuilder();
            sb.append(i);
            for (Object obj : objArr) {
                sb.append(",");
                sb.append(obj.toString());
            }
            System.out.println(sb);
        }
    }

    public static void writeEvent(int i, Object... objArr) {
        getReporter().report(i, objArr);
    }
}
