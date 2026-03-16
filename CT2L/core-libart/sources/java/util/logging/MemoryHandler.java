package java.util.logging;

public class MemoryHandler extends Handler {
    private static final int DEFAULT_SIZE = 1000;
    private LogRecord[] buffer;
    private int cursor;
    private final LogManager manager = LogManager.getLogManager();
    private Level push;
    private int size;
    private Handler target;

    public MemoryHandler() {
        this.size = 1000;
        this.push = Level.SEVERE;
        String className = getClass().getName();
        String targetName = this.manager.getProperty(className + ".target");
        try {
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            Class<?> targetClass = (loader == null ? ClassLoader.getSystemClassLoader() : loader).loadClass(targetName);
            this.target = (Handler) targetClass.newInstance();
            String sizeString = this.manager.getProperty(className + ".size");
            if (sizeString != null) {
                try {
                    this.size = Integer.parseInt(sizeString);
                    if (this.size <= 0) {
                        this.size = 1000;
                    }
                } catch (Exception e) {
                    printInvalidPropMessage(className + ".size", sizeString, e);
                }
            }
            String pushName = this.manager.getProperty(className + ".push");
            if (pushName != null) {
                try {
                    this.push = Level.parse(pushName);
                } catch (Exception e2) {
                    printInvalidPropMessage(className + ".push", pushName, e2);
                }
            }
            initProperties("ALL", null, "java.util.logging.SimpleFormatter", null);
            this.buffer = new LogRecord[this.size];
        } catch (Exception e3) {
            throw new RuntimeException("Cannot load target handler '" + targetName + "'");
        }
    }

    public MemoryHandler(Handler target, int size, Level pushLevel) {
        this.size = 1000;
        this.push = Level.SEVERE;
        if (size <= 0) {
            throw new IllegalArgumentException("size <= 0");
        }
        target.getLevel();
        pushLevel.intValue();
        this.target = target;
        this.size = size;
        this.push = pushLevel;
        initProperties("ALL", null, "java.util.logging.SimpleFormatter", null);
        this.buffer = new LogRecord[size];
    }

    @Override
    public void close() {
        this.manager.checkAccess();
        this.target.close();
        setLevel(Level.OFF);
    }

    @Override
    public void flush() {
        this.target.flush();
    }

    @Override
    public synchronized void publish(LogRecord record) {
        if (isLoggable(record)) {
            if (this.cursor >= this.size) {
                this.cursor = 0;
            }
            LogRecord[] logRecordArr = this.buffer;
            int i = this.cursor;
            this.cursor = i + 1;
            logRecordArr[i] = record;
            if (record.getLevel().intValue() >= this.push.intValue()) {
                push();
            }
        }
    }

    public Level getPushLevel() {
        return this.push;
    }

    @Override
    public boolean isLoggable(LogRecord record) {
        return super.isLoggable(record);
    }

    public void push() {
        for (int i = this.cursor; i < this.size; i++) {
            if (this.buffer[i] != null) {
                this.target.publish(this.buffer[i]);
            }
            this.buffer[i] = null;
        }
        for (int i2 = 0; i2 < this.cursor; i2++) {
            if (this.buffer[i2] != null) {
                this.target.publish(this.buffer[i2]);
            }
            this.buffer[i2] = null;
        }
        this.cursor = 0;
    }

    public void setPushLevel(Level newLevel) {
        this.manager.checkAccess();
        newLevel.intValue();
        this.push = newLevel;
    }
}
