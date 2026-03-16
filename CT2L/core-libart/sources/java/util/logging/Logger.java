package java.util.logging;

import dalvik.system.DalvikLogHandler;
import dalvik.system.DalvikLogging;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.concurrent.CopyOnWriteArrayList;

public class Logger {
    private final String androidTag;
    private Filter filter;
    volatile Level levelObjVal;
    private volatile String name;
    Logger parent;
    private volatile ResourceBundle resourceBundle;
    private volatile String resourceBundleName;
    private static final DalvikLogHandler GENERAL_LOG_HANDLER = new DalvikLogHandler() {
        @Override
        public void publish(Logger source, String tag, Level level, String message) {
            LogRecord record = new LogRecord(level, message);
            record.setLoggerName(source.name);
            source.setResourceBundle(record);
            source.log(record);
        }
    };
    public static final String GLOBAL_LOGGER_NAME = "global";

    @Deprecated
    public static final Logger global = new Logger(GLOBAL_LOGGER_NAME, null);
    private static final Handler[] EMPTY_HANDLERS_ARRAY = new Handler[0];
    volatile int levelIntVal = Level.INFO.intValue();
    private final List<Handler> handlers = new CopyOnWriteArrayList();
    private boolean notifyParentHandlers = true;
    private boolean isNamed = true;
    final List<Logger> children = new ArrayList();
    private volatile DalvikLogHandler dalvikLogHandler = GENERAL_LOG_HANDLER;

    void updateDalvikLogHandler() {
        DalvikLogHandler newLogHandler = GENERAL_LOG_HANDLER;
        Logger parent = this.parent;
        if (getClass() == Logger.class) {
            if (parent == null) {
                Iterator<Handler> h = this.handlers.iterator();
                if (h.hasNext()) {
                    Object obj = (Handler) h.next();
                    if (!h.hasNext() && (obj instanceof DalvikLogHandler)) {
                        newLogHandler = (DalvikLogHandler) obj;
                    }
                }
            } else if (this.handlers.isEmpty() && this.notifyParentHandlers) {
                newLogHandler = parent.dalvikLogHandler;
            }
        }
        if (newLogHandler != this.dalvikLogHandler) {
            this.dalvikLogHandler = newLogHandler;
            for (Logger logger : this.children) {
                logger.updateDalvikLogHandler();
            }
        }
    }

    protected Logger(String name, String resourceBundleName) {
        this.name = name;
        initResourceBundle(resourceBundleName);
        this.androidTag = DalvikLogging.loggerNameToTag(name);
        updateDalvikLogHandler();
    }

    static ResourceBundle loadResourceBundle(String resourceBundleName) {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl != null) {
            try {
                return ResourceBundle.getBundle(resourceBundleName, Locale.getDefault(), cl);
            } catch (MissingResourceException e) {
            }
        }
        ClassLoader cl2 = ClassLoader.getSystemClassLoader();
        if (cl2 != null) {
            try {
                return ResourceBundle.getBundle(resourceBundleName, Locale.getDefault(), cl2);
            } catch (MissingResourceException e2) {
            }
        }
        throw new MissingResourceException("Failed to load the specified resource bundle \"" + resourceBundleName + "\"", resourceBundleName, null);
    }

    public static Logger getAnonymousLogger() {
        return getAnonymousLogger(null);
    }

    public static Logger getAnonymousLogger(String resourceBundleName) {
        Logger result = new Logger(null, resourceBundleName);
        result.isNamed = false;
        LogManager logManager = LogManager.getLogManager();
        logManager.setParent(result, logManager.getLogger(""));
        return result;
    }

    private synchronized void initResourceBundle(String resourceBundleName) {
        String current = this.resourceBundleName;
        if (current != null) {
            if (!current.equals(resourceBundleName)) {
                throw new IllegalArgumentException("Resource bundle name '" + resourceBundleName + "' is inconsistent with the existing '" + current + "'");
            }
        } else if (resourceBundleName != null) {
            this.resourceBundle = loadResourceBundle(resourceBundleName);
            this.resourceBundleName = resourceBundleName;
        }
    }

    public static Logger getLogger(String name) {
        return LogManager.getLogManager().getOrCreate(name, null);
    }

    public static Logger getLogger(String name, String resourceBundleName) {
        Logger result = LogManager.getLogManager().getOrCreate(name, resourceBundleName);
        result.initResourceBundle(resourceBundleName);
        return result;
    }

    public static Logger getGlobal() {
        return global;
    }

    public void addHandler(Handler handler) {
        if (handler == null) {
            throw new NullPointerException("handler == null");
        }
        if (this.isNamed) {
            LogManager.getLogManager().checkAccess();
        }
        this.handlers.add(handler);
        updateDalvikLogHandler();
    }

    void setManager(LogManager manager) {
        String levelProperty = manager.getProperty(this.name + ".level");
        if (levelProperty != null) {
            try {
                manager.setLevelRecursively(this, Level.parse(levelProperty));
            } catch (IllegalArgumentException invalidLevel) {
                invalidLevel.printStackTrace();
            }
        }
        String handlersPropertyName = this.name.isEmpty() ? "handlers" : this.name + ".handlers";
        String handlersProperty = manager.getProperty(handlersPropertyName);
        if (handlersProperty != null) {
            String[] arr$ = handlersProperty.split(",|\\s");
            for (String handlerName : arr$) {
                if (!handlerName.isEmpty()) {
                    try {
                        Handler handler = (Handler) LogManager.getInstanceByClass(handlerName);
                        try {
                            String level = manager.getProperty(handlerName + ".level");
                            if (level != null) {
                                handler.setLevel(Level.parse(level));
                            }
                        } catch (Exception invalidLevel2) {
                            invalidLevel2.printStackTrace();
                        }
                        this.handlers.add(handler);
                    } catch (Exception invalidHandlerName) {
                        invalidHandlerName.printStackTrace();
                    }
                }
            }
        }
        updateDalvikLogHandler();
    }

    public Handler[] getHandlers() {
        return (Handler[]) this.handlers.toArray(EMPTY_HANDLERS_ARRAY);
    }

    public void removeHandler(Handler handler) {
        if (this.isNamed) {
            LogManager.getLogManager().checkAccess();
        }
        if (handler != null) {
            this.handlers.remove(handler);
            updateDalvikLogHandler();
        }
    }

    public Filter getFilter() {
        return this.filter;
    }

    public void setFilter(Filter newFilter) {
        if (this.isNamed) {
            LogManager.getLogManager().checkAccess();
        }
        this.filter = newFilter;
    }

    public Level getLevel() {
        return this.levelObjVal;
    }

    public void setLevel(Level newLevel) {
        LogManager logManager = LogManager.getLogManager();
        if (this.isNamed) {
            logManager.checkAccess();
        }
        logManager.setLevelRecursively(this, newLevel);
    }

    public boolean getUseParentHandlers() {
        return this.notifyParentHandlers;
    }

    public void setUseParentHandlers(boolean notifyParentHandlers) {
        if (this.isNamed) {
            LogManager.getLogManager().checkAccess();
        }
        this.notifyParentHandlers = notifyParentHandlers;
        updateDalvikLogHandler();
    }

    public Logger getParent() {
        return this.parent;
    }

    public void setParent(Logger parent) {
        if (parent == null) {
            throw new NullPointerException("parent == null");
        }
        LogManager logManager = LogManager.getLogManager();
        logManager.checkAccess();
        logManager.setParent(this, parent);
    }

    public String getName() {
        return this.name;
    }

    public ResourceBundle getResourceBundle() {
        return this.resourceBundle;
    }

    public String getResourceBundleName() {
        return this.resourceBundleName;
    }

    private boolean internalIsLoggable(Level l) {
        int effectiveLevel = this.levelIntVal;
        return effectiveLevel != Level.OFF.intValue() && l.intValue() >= effectiveLevel;
    }

    public boolean isLoggable(Level l) {
        return internalIsLoggable(l);
    }

    private void setResourceBundle(LogRecord record) {
        for (Logger p = this; p != null; p = p.parent) {
            String resourceBundleName = p.resourceBundleName;
            if (resourceBundleName != null) {
                record.setResourceBundle(p.resourceBundle);
                record.setResourceBundleName(resourceBundleName);
                return;
            }
        }
    }

    public void entering(String sourceClass, String sourceMethod) {
        if (internalIsLoggable(Level.FINER)) {
            LogRecord record = new LogRecord(Level.FINER, "ENTRY");
            record.setLoggerName(this.name);
            record.setSourceClassName(sourceClass);
            record.setSourceMethodName(sourceMethod);
            setResourceBundle(record);
            log(record);
        }
    }

    public void entering(String sourceClass, String sourceMethod, Object param) {
        if (internalIsLoggable(Level.FINER)) {
            LogRecord record = new LogRecord(Level.FINER, "ENTRY {0}");
            record.setLoggerName(this.name);
            record.setSourceClassName(sourceClass);
            record.setSourceMethodName(sourceMethod);
            record.setParameters(new Object[]{param});
            setResourceBundle(record);
            log(record);
        }
    }

    public void entering(String sourceClass, String sourceMethod, Object[] params) {
        if (internalIsLoggable(Level.FINER)) {
            String msg = "ENTRY";
            if (params != null) {
                StringBuilder msgBuffer = new StringBuilder("ENTRY");
                for (int i = 0; i < params.length; i++) {
                    msgBuffer.append(" {").append(i).append("}");
                }
                msg = msgBuffer.toString();
            }
            LogRecord record = new LogRecord(Level.FINER, msg);
            record.setLoggerName(this.name);
            record.setSourceClassName(sourceClass);
            record.setSourceMethodName(sourceMethod);
            record.setParameters(params);
            setResourceBundle(record);
            log(record);
        }
    }

    public void exiting(String sourceClass, String sourceMethod) {
        if (internalIsLoggable(Level.FINER)) {
            LogRecord record = new LogRecord(Level.FINER, "RETURN");
            record.setLoggerName(this.name);
            record.setSourceClassName(sourceClass);
            record.setSourceMethodName(sourceMethod);
            setResourceBundle(record);
            log(record);
        }
    }

    public void exiting(String sourceClass, String sourceMethod, Object result) {
        if (internalIsLoggable(Level.FINER)) {
            LogRecord record = new LogRecord(Level.FINER, "RETURN {0}");
            record.setLoggerName(this.name);
            record.setSourceClassName(sourceClass);
            record.setSourceMethodName(sourceMethod);
            record.setParameters(new Object[]{result});
            setResourceBundle(record);
            log(record);
        }
    }

    public void throwing(String sourceClass, String sourceMethod, Throwable thrown) {
        if (internalIsLoggable(Level.FINER)) {
            LogRecord record = new LogRecord(Level.FINER, "THROW");
            record.setLoggerName(this.name);
            record.setSourceClassName(sourceClass);
            record.setSourceMethodName(sourceMethod);
            record.setThrown(thrown);
            setResourceBundle(record);
            log(record);
        }
    }

    public void severe(String msg) {
        log(Level.SEVERE, msg);
    }

    public void warning(String msg) {
        log(Level.WARNING, msg);
    }

    public void info(String msg) {
        log(Level.INFO, msg);
    }

    public void config(String msg) {
        log(Level.CONFIG, msg);
    }

    public void fine(String msg) {
        log(Level.FINE, msg);
    }

    public void finer(String msg) {
        log(Level.FINER, msg);
    }

    public void finest(String msg) {
        log(Level.FINEST, msg);
    }

    public void log(Level logLevel, String msg) {
        if (internalIsLoggable(logLevel)) {
            this.dalvikLogHandler.publish(this, this.androidTag, logLevel, msg);
        }
    }

    public void log(Level logLevel, String msg, Object param) {
        if (internalIsLoggable(logLevel)) {
            LogRecord record = new LogRecord(logLevel, msg);
            record.setLoggerName(this.name);
            record.setParameters(new Object[]{param});
            setResourceBundle(record);
            log(record);
        }
    }

    public void log(Level logLevel, String msg, Object[] params) {
        if (internalIsLoggable(logLevel)) {
            LogRecord record = new LogRecord(logLevel, msg);
            record.setLoggerName(this.name);
            record.setParameters(params);
            setResourceBundle(record);
            log(record);
        }
    }

    public void log(Level logLevel, String msg, Throwable thrown) {
        if (internalIsLoggable(logLevel)) {
            LogRecord record = new LogRecord(logLevel, msg);
            record.setLoggerName(this.name);
            record.setThrown(thrown);
            setResourceBundle(record);
            log(record);
        }
    }

    public void log(LogRecord record) {
        if (internalIsLoggable(record.getLevel())) {
            Filter f = this.filter;
            if (f == null || f.isLoggable(record)) {
                Handler[] allHandlers = getHandlers();
                for (Handler element : allHandlers) {
                    element.publish(record);
                }
                Logger temp = this;
                Logger theParent = temp.parent;
                while (theParent != null && temp.getUseParentHandlers()) {
                    Handler[] ha = theParent.getHandlers();
                    for (Handler element2 : ha) {
                        element2.publish(record);
                    }
                    temp = theParent;
                    theParent = temp.parent;
                }
            }
        }
    }

    public void logp(Level logLevel, String sourceClass, String sourceMethod, String msg) {
        if (internalIsLoggable(logLevel)) {
            LogRecord record = new LogRecord(logLevel, msg);
            record.setLoggerName(this.name);
            record.setSourceClassName(sourceClass);
            record.setSourceMethodName(sourceMethod);
            setResourceBundle(record);
            log(record);
        }
    }

    public void logp(Level logLevel, String sourceClass, String sourceMethod, String msg, Object param) {
        if (internalIsLoggable(logLevel)) {
            LogRecord record = new LogRecord(logLevel, msg);
            record.setLoggerName(this.name);
            record.setSourceClassName(sourceClass);
            record.setSourceMethodName(sourceMethod);
            record.setParameters(new Object[]{param});
            setResourceBundle(record);
            log(record);
        }
    }

    public void logp(Level logLevel, String sourceClass, String sourceMethod, String msg, Object[] params) {
        if (internalIsLoggable(logLevel)) {
            LogRecord record = new LogRecord(logLevel, msg);
            record.setLoggerName(this.name);
            record.setSourceClassName(sourceClass);
            record.setSourceMethodName(sourceMethod);
            record.setParameters(params);
            setResourceBundle(record);
            log(record);
        }
    }

    public void logp(Level logLevel, String sourceClass, String sourceMethod, String msg, Throwable thrown) {
        if (internalIsLoggable(logLevel)) {
            LogRecord record = new LogRecord(logLevel, msg);
            record.setLoggerName(this.name);
            record.setSourceClassName(sourceClass);
            record.setSourceMethodName(sourceMethod);
            record.setThrown(thrown);
            setResourceBundle(record);
            log(record);
        }
    }

    public void logrb(Level logLevel, String sourceClass, String sourceMethod, String bundleName, String msg) {
        if (internalIsLoggable(logLevel)) {
            LogRecord record = new LogRecord(logLevel, msg);
            if (bundleName != null) {
                try {
                    record.setResourceBundle(loadResourceBundle(bundleName));
                } catch (MissingResourceException e) {
                }
                record.setResourceBundleName(bundleName);
            }
            record.setLoggerName(this.name);
            record.setSourceClassName(sourceClass);
            record.setSourceMethodName(sourceMethod);
            log(record);
        }
    }

    public void logrb(Level logLevel, String sourceClass, String sourceMethod, String bundleName, String msg, Object param) {
        if (internalIsLoggable(logLevel)) {
            LogRecord record = new LogRecord(logLevel, msg);
            if (bundleName != null) {
                try {
                    record.setResourceBundle(loadResourceBundle(bundleName));
                } catch (MissingResourceException e) {
                }
                record.setResourceBundleName(bundleName);
            }
            record.setLoggerName(this.name);
            record.setSourceClassName(sourceClass);
            record.setSourceMethodName(sourceMethod);
            record.setParameters(new Object[]{param});
            log(record);
        }
    }

    public void logrb(Level logLevel, String sourceClass, String sourceMethod, String bundleName, String msg, Object[] params) {
        if (internalIsLoggable(logLevel)) {
            LogRecord record = new LogRecord(logLevel, msg);
            if (bundleName != null) {
                try {
                    record.setResourceBundle(loadResourceBundle(bundleName));
                } catch (MissingResourceException e) {
                }
                record.setResourceBundleName(bundleName);
            }
            record.setLoggerName(this.name);
            record.setSourceClassName(sourceClass);
            record.setSourceMethodName(sourceMethod);
            record.setParameters(params);
            log(record);
        }
    }

    public void logrb(Level logLevel, String sourceClass, String sourceMethod, String bundleName, String msg, Throwable thrown) {
        if (internalIsLoggable(logLevel)) {
            LogRecord record = new LogRecord(logLevel, msg);
            if (bundleName != null) {
                try {
                    record.setResourceBundle(loadResourceBundle(bundleName));
                } catch (MissingResourceException e) {
                }
                record.setResourceBundleName(bundleName);
            }
            record.setLoggerName(this.name);
            record.setSourceClassName(sourceClass);
            record.setSourceMethodName(sourceMethod);
            record.setThrown(thrown);
            log(record);
        }
    }

    void reset() {
        this.levelObjVal = null;
        this.levelIntVal = Level.INFO.intValue();
        for (Handler handler : this.handlers) {
            try {
                if (this.handlers.remove(handler)) {
                    handler.close();
                }
            } catch (Exception e) {
            }
        }
        updateDalvikLogHandler();
    }
}
