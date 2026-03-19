package java.util.logging;

public class MemoryHandler extends Handler {
    private static final int DEFAULT_SIZE = 1000;
    private LogRecord[] buffer;
    int count;
    private volatile Level pushLevel;
    private int size;
    int start;
    private Handler target;

    private void configure() {
        LogManager logManager = LogManager.getLogManager();
        String name = getClass().getName();
        this.pushLevel = logManager.getLevelProperty(name + ".push", Level.SEVERE);
        this.size = logManager.getIntProperty(name + ".size", 1000);
        if (this.size <= 0) {
            this.size = 1000;
        }
        setLevel(logManager.getLevelProperty(name + ".level", Level.ALL));
        setFilter(logManager.getFilterProperty(name + ".filter", null));
        setFormatter(logManager.getFormatterProperty(name + ".formatter", new SimpleFormatter()));
    }

    public MemoryHandler() {
        this.sealed = false;
        configure();
        this.sealed = true;
        LogManager logManager = LogManager.getLogManager();
        String name = getClass().getName();
        String property = logManager.getProperty(name + ".target");
        if (property == null) {
            throw new RuntimeException("The handler " + name + " does not specify a target");
        }
        try {
            this.target = (Handler) ClassLoader.getSystemClassLoader().loadClass(property).newInstance();
        } catch (Exception e) {
            try {
                this.target = (Handler) Thread.currentThread().getContextClassLoader().loadClass(property).newInstance();
            } catch (Exception e2) {
                throw new RuntimeException("MemoryHandler can't load handler target \"" + property + "\"", e2);
            }
        }
        init();
    }

    private void init() {
        this.buffer = new LogRecord[this.size];
        this.start = 0;
        this.count = 0;
    }

    public MemoryHandler(Handler handler, int i, Level level) {
        if (handler == null || level == null) {
            throw new NullPointerException();
        }
        if (i <= 0) {
            throw new IllegalArgumentException();
        }
        this.sealed = false;
        configure();
        this.sealed = true;
        this.target = handler;
        this.pushLevel = level;
        this.size = i;
        init();
    }

    @Override
    public synchronized void publish(LogRecord logRecord) {
        if (isLoggable(logRecord)) {
            this.buffer[(this.start + this.count) % this.buffer.length] = logRecord;
            if (this.count < this.buffer.length) {
                this.count++;
            } else {
                this.start++;
                this.start %= this.buffer.length;
            }
            if (logRecord.getLevel().intValue() >= this.pushLevel.intValue()) {
                push();
            }
        }
    }

    public synchronized void push() {
        for (int i = 0; i < this.count; i++) {
            this.target.publish(this.buffer[(this.start + i) % this.buffer.length]);
        }
        this.start = 0;
        this.count = 0;
    }

    @Override
    public void flush() {
        this.target.flush();
    }

    @Override
    public void close() throws SecurityException {
        this.target.close();
        setLevel(Level.OFF);
    }

    public synchronized void setPushLevel(Level level) throws SecurityException {
        if (level == null) {
            throw new NullPointerException();
        }
        checkPermission();
        this.pushLevel = level;
    }

    public Level getPushLevel() {
        return this.pushLevel;
    }

    @Override
    public boolean isLoggable(LogRecord logRecord) {
        return super.isLoggable(logRecord);
    }
}
