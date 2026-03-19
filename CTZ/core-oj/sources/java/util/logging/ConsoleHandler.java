package java.util.logging;

public class ConsoleHandler extends StreamHandler {
    private void configure() {
        LogManager logManager = LogManager.getLogManager();
        String name = getClass().getName();
        setLevel(logManager.getLevelProperty(name + ".level", Level.INFO));
        setFilter(logManager.getFilterProperty(name + ".filter", null));
        setFormatter(logManager.getFormatterProperty(name + ".formatter", new SimpleFormatter()));
        try {
            setEncoding(logManager.getStringProperty(name + ".encoding", null));
        } catch (Exception e) {
            try {
                setEncoding(null);
            } catch (Exception e2) {
            }
        }
    }

    public ConsoleHandler() {
        this.sealed = false;
        configure();
        setOutputStream(System.err);
        this.sealed = true;
    }

    @Override
    public void publish(LogRecord logRecord) {
        super.publish(logRecord);
        flush();
    }

    @Override
    public void close() {
        flush();
    }
}
