package java.util.logging;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class LogRecord implements Serializable {
    private static final int MAJOR = 1;
    private static final int MINOR = 4;
    private static long currentSequenceNumber = 0;
    private static ThreadLocal<Integer> currentThreadId = new ThreadLocal<>();
    private static int initThreadId = 0;
    private static final long serialVersionUID = 5372048053134512534L;
    private Level level;
    private String loggerName;
    private String message;
    private long millis;
    private transient Object[] parameters;
    private transient ResourceBundle resourceBundle;
    private String resourceBundleName;
    private long sequenceNumber;
    private String sourceClassName;
    private transient boolean sourceInitialized;
    private String sourceMethodName;
    private int threadID;
    private Throwable thrown;

    public LogRecord(Level level, String msg) {
        if (level == null) {
            throw new NullPointerException("level == null");
        }
        this.level = level;
        this.message = msg;
        this.millis = System.currentTimeMillis();
        synchronized (LogRecord.class) {
            long j = currentSequenceNumber;
            currentSequenceNumber = 1 + j;
            this.sequenceNumber = j;
            Integer id = currentThreadId.get();
            if (id == null) {
                this.threadID = initThreadId;
                ThreadLocal<Integer> threadLocal = currentThreadId;
                int i = initThreadId;
                initThreadId = i + 1;
                threadLocal.set(Integer.valueOf(i));
            } else {
                this.threadID = id.intValue();
            }
        }
        this.sourceClassName = null;
        this.sourceMethodName = null;
        this.loggerName = null;
        this.parameters = null;
        this.resourceBundle = null;
        this.resourceBundleName = null;
        this.thrown = null;
    }

    public Level getLevel() {
        return this.level;
    }

    public void setLevel(Level level) {
        if (level == null) {
            throw new NullPointerException("level == null");
        }
        this.level = level;
    }

    public String getLoggerName() {
        return this.loggerName;
    }

    public void setLoggerName(String loggerName) {
        this.loggerName = loggerName;
    }

    public String getMessage() {
        return this.message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public long getMillis() {
        return this.millis;
    }

    public void setMillis(long millis) {
        this.millis = millis;
    }

    public Object[] getParameters() {
        return this.parameters;
    }

    public void setParameters(Object[] parameters) {
        this.parameters = parameters;
    }

    public ResourceBundle getResourceBundle() {
        return this.resourceBundle;
    }

    public void setResourceBundle(ResourceBundle resourceBundle) {
        this.resourceBundle = resourceBundle;
    }

    public String getResourceBundleName() {
        return this.resourceBundleName;
    }

    public void setResourceBundleName(String resourceBundleName) {
        this.resourceBundleName = resourceBundleName;
    }

    public long getSequenceNumber() {
        return this.sequenceNumber;
    }

    public void setSequenceNumber(long sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    public String getSourceClassName() {
        initSource();
        return this.sourceClassName;
    }

    private void initSource() {
        if (!this.sourceInitialized) {
            boolean sawLogger = false;
            StackTraceElement[] arr$ = new Throwable().getStackTrace();
            int len$ = arr$.length;
            int i$ = 0;
            while (true) {
                if (i$ >= len$) {
                    break;
                }
                StackTraceElement element = arr$[i$];
                String current = element.getClassName();
                if (current.startsWith(Logger.class.getName())) {
                    sawLogger = true;
                } else if (sawLogger) {
                    this.sourceClassName = element.getClassName();
                    this.sourceMethodName = element.getMethodName();
                    break;
                }
                i$++;
            }
            this.sourceInitialized = true;
        }
    }

    public void setSourceClassName(String sourceClassName) {
        this.sourceInitialized = true;
        this.sourceClassName = sourceClassName;
    }

    public String getSourceMethodName() {
        initSource();
        return this.sourceMethodName;
    }

    public void setSourceMethodName(String sourceMethodName) {
        this.sourceInitialized = true;
        this.sourceMethodName = sourceMethodName;
    }

    public int getThreadID() {
        return this.threadID;
    }

    public void setThreadID(int threadID) {
        this.threadID = threadID;
    }

    public Throwable getThrown() {
        return this.thrown;
    }

    public void setThrown(Throwable thrown) {
        this.thrown = thrown;
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        out.writeByte(1);
        out.writeByte(4);
        if (this.parameters == null) {
            out.writeInt(-1);
            return;
        }
        out.writeInt(this.parameters.length);
        Object[] arr$ = this.parameters;
        int len$ = arr$.length;
        for (int i$ = 0; i$ < len$; i$++) {
            Object element = arr$[i$];
            out.writeObject(element == null ? null : element.toString());
        }
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        byte major = in.readByte();
        byte minor = in.readByte();
        if (major != 1) {
            throw new IOException("Different version " + Byte.valueOf(major) + "." + Byte.valueOf(minor));
        }
        int length = in.readInt();
        if (length >= 0) {
            this.parameters = new Object[length];
            for (int i = 0; i < this.parameters.length; i++) {
                this.parameters[i] = in.readObject();
            }
        }
        if (this.resourceBundleName != null) {
            try {
                this.resourceBundle = Logger.loadResourceBundle(this.resourceBundleName);
            } catch (MissingResourceException e) {
                this.resourceBundle = null;
            }
        }
    }
}
