package java.util.logging;

import java.beans.PropertyChangeListener;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.Permission;
import java.security.PrivilegedAction;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.WeakHashMap;
import sun.util.logging.PlatformLogger;

public class LogManager {
    static final boolean $assertionsDisabled = false;
    public static final String LOGGING_MXBEAN_NAME = "java.util.logging:type=Logging";
    private static final int MAX_ITERATIONS = 400;
    private WeakHashMap<Object, LoggerContext> contextsMap;
    private final Permission controlPermission;
    private boolean deathImminent;
    private volatile boolean initializationDone;
    private boolean initializedCalled;
    private boolean initializedGlobalHandlers;
    private final Map<Object, Integer> listenerMap;
    private final ReferenceQueue<Logger> loggerRefQueue;
    private volatile Properties props;
    private volatile boolean readPrimordialConfiguration;
    private volatile Logger rootLogger;
    private final LoggerContext systemContext;
    private final LoggerContext userContext;
    private static final Level defaultLevel = Level.INFO;
    private static final LogManager manager = (LogManager) AccessController.doPrivileged(new PrivilegedAction<LogManager>() {
        @Override
        public LogManager run() {
            String property;
            LogManager logManager = null;
            try {
                property = System.getProperty("java.util.logging.manager");
                if (property != null) {
                    try {
                        logManager = (LogManager) LogManager.getClassInstance(property).newInstance();
                    } catch (Exception e) {
                        e = e;
                        System.err.println("Could not load Logmanager \"" + property + "\"");
                        e.printStackTrace();
                    }
                }
            } catch (Exception e2) {
                e = e2;
                property = null;
            }
            if (logManager == null) {
                return new LogManager();
            }
            return logManager;
        }
    });
    private static LoggingMXBean loggingMXBean = null;

    private class Cleaner extends Thread {
        private Cleaner() {
            setContextClassLoader(null);
        }

        @Override
        public void run() {
            LogManager unused = LogManager.manager;
            synchronized (LogManager.this) {
                LogManager.this.deathImminent = true;
                LogManager.this.initializedGlobalHandlers = true;
            }
            LogManager.this.reset();
        }
    }

    protected LogManager() {
        this(checkSubclassPermissions());
    }

    private LogManager(Void r3) {
        this.props = new Properties();
        this.listenerMap = new HashMap();
        this.systemContext = new SystemLoggerContext();
        this.userContext = new LoggerContext();
        this.initializedGlobalHandlers = true;
        this.initializedCalled = $assertionsDisabled;
        this.initializationDone = $assertionsDisabled;
        this.contextsMap = null;
        this.loggerRefQueue = new ReferenceQueue<>();
        this.controlPermission = new LoggingPermission("control", null);
        try {
            Runtime.getRuntime().addShutdownHook(new Cleaner());
        } catch (IllegalStateException e) {
        }
    }

    private static Void checkSubclassPermissions() {
        SecurityManager securityManager = System.getSecurityManager();
        if (securityManager != null) {
            securityManager.checkPermission(new RuntimePermission("shutdownHooks"));
            securityManager.checkPermission(new RuntimePermission("setContextClassLoader"));
            return null;
        }
        return null;
    }

    final void ensureLogManagerInitialized() {
        boolean z;
        if (this.initializationDone || this != manager) {
            return;
        }
        synchronized (this) {
            if (!this.initializedCalled) {
                z = $assertionsDisabled;
            } else {
                z = true;
            }
            if (!z && !this.initializationDone) {
                this.initializedCalled = true;
                try {
                    AccessController.doPrivileged(new PrivilegedAction<Object>() {
                        static final boolean $assertionsDisabled = false;

                        @Override
                        public Object run() {
                            this.readPrimordialConfiguration();
                            LogManager logManager = this;
                            LogManager logManager2 = this;
                            Objects.requireNonNull(logManager2);
                            logManager.rootLogger = new RootLogger();
                            this.addLogger(this.rootLogger);
                            if (!this.rootLogger.isLevelInitialized()) {
                                this.rootLogger.setLevel(LogManager.defaultLevel);
                            }
                            this.addLogger(Logger.global);
                            return null;
                        }
                    });
                } finally {
                    this.initializationDone = true;
                }
            }
        }
    }

    public static LogManager getLogManager() {
        if (manager != null) {
            manager.ensureLogManagerInitialized();
        }
        return manager;
    }

    private void readPrimordialConfiguration() {
        if (!this.readPrimordialConfiguration) {
            synchronized (this) {
                if (!this.readPrimordialConfiguration) {
                    if (System.out == null) {
                        return;
                    }
                    this.readPrimordialConfiguration = true;
                    try {
                        AccessController.doPrivileged(new PrivilegedExceptionAction<Void>() {
                            @Override
                            public Void run() throws Exception {
                                LogManager.this.readConfiguration();
                                PlatformLogger.redirectPlatformLoggers();
                                return null;
                            }
                        });
                    } catch (Exception e) {
                    }
                }
            }
        }
    }

    @Deprecated
    public void addPropertyChangeListener(PropertyChangeListener propertyChangeListener) throws SecurityException {
        PropertyChangeListener propertyChangeListener2 = (PropertyChangeListener) Objects.requireNonNull(propertyChangeListener);
        checkPermission();
        synchronized (this.listenerMap) {
            Integer num = this.listenerMap.get(propertyChangeListener2);
            int iIntValue = 1;
            if (num != null) {
                iIntValue = 1 + num.intValue();
            }
            this.listenerMap.put(propertyChangeListener2, Integer.valueOf(iIntValue));
        }
    }

    @Deprecated
    public void removePropertyChangeListener(PropertyChangeListener propertyChangeListener) throws SecurityException {
        checkPermission();
        if (propertyChangeListener != null) {
            synchronized (this.listenerMap) {
                Integer num = this.listenerMap.get(propertyChangeListener);
                if (num != null) {
                    int iIntValue = num.intValue();
                    if (iIntValue != 1) {
                        this.listenerMap.put(propertyChangeListener, Integer.valueOf(iIntValue - 1));
                    } else {
                        this.listenerMap.remove(propertyChangeListener);
                    }
                }
            }
        }
    }

    private LoggerContext getUserContext() {
        return this.userContext;
    }

    final LoggerContext getSystemContext() {
        return this.systemContext;
    }

    private List<LoggerContext> contexts() {
        ArrayList arrayList = new ArrayList();
        arrayList.add(getSystemContext());
        arrayList.add(getUserContext());
        return arrayList;
    }

    Logger demandLogger(String str, String str2, Class<?> cls) {
        Logger logger = getLogger(str);
        if (logger == null) {
            Logger logger2 = new Logger(str, str2, cls, this, $assertionsDisabled);
            while (!addLogger(logger2)) {
                logger = getLogger(str);
                if (logger != null) {
                }
            }
            return logger2;
        }
        return logger;
    }

    Logger demandSystemLogger(String str, String str2) {
        final Logger logger;
        final Logger loggerDemandLogger = getSystemContext().demandLogger(str, str2);
        do {
            if (!addLogger(loggerDemandLogger)) {
                logger = getLogger(str);
            } else {
                logger = loggerDemandLogger;
            }
        } while (logger == null);
        if (logger != loggerDemandLogger && loggerDemandLogger.accessCheckedHandlers().length == 0) {
            AccessController.doPrivileged(new PrivilegedAction<Void>() {
                @Override
                public Void run() {
                    for (Handler handler : logger.accessCheckedHandlers()) {
                        loggerDemandLogger.addHandler(handler);
                    }
                    return null;
                }
            });
        }
        return loggerDemandLogger;
    }

    private static Class getClassInstance(String str) {
        if (str == null) {
            return null;
        }
        try {
            return ClassLoader.getSystemClassLoader().loadClass(str);
        } catch (ClassNotFoundException e) {
            try {
                return Thread.currentThread().getContextClassLoader().loadClass(str);
            } catch (ClassNotFoundException e2) {
                return null;
            }
        }
    }

    class LoggerContext {
        static final boolean $assertionsDisabled = false;
        private final Hashtable<String, LoggerWeakRef> namedLoggers;
        private final LogNode root;

        private LoggerContext() {
            this.namedLoggers = new Hashtable<>();
            this.root = new LogNode(null, this);
        }

        final boolean requiresDefaultLoggers() {
            boolean z = getOwner() == LogManager.manager ? true : LogManager.$assertionsDisabled;
            if (z) {
                getOwner().ensureLogManagerInitialized();
            }
            return z;
        }

        final LogManager getOwner() {
            return LogManager.this;
        }

        final Logger getRootLogger() {
            return getOwner().rootLogger;
        }

        final Logger getGlobalLogger() {
            return Logger.global;
        }

        Logger demandLogger(String str, String str2) {
            return getOwner().demandLogger(str, str2, null);
        }

        private void ensureInitialized() {
            if (requiresDefaultLoggers()) {
                ensureDefaultLogger(getRootLogger());
                ensureDefaultLogger(getGlobalLogger());
            }
        }

        synchronized Logger findLogger(String str) {
            ensureInitialized();
            LoggerWeakRef loggerWeakRef = this.namedLoggers.get(str);
            if (loggerWeakRef == null) {
                return null;
            }
            Logger logger = loggerWeakRef.get();
            if (logger == null) {
                loggerWeakRef.dispose();
            }
            return logger;
        }

        private void ensureAllDefaultLoggers(Logger logger) {
            if (requiresDefaultLoggers()) {
                String name = logger.getName();
                if (!name.isEmpty()) {
                    ensureDefaultLogger(getRootLogger());
                    if (!Logger.GLOBAL_LOGGER_NAME.equals(name)) {
                        ensureDefaultLogger(getGlobalLogger());
                    }
                }
            }
        }

        private void ensureDefaultLogger(Logger logger) {
            if (requiresDefaultLoggers() && logger != null) {
                if ((logger == Logger.global || logger == LogManager.this.rootLogger) && !this.namedLoggers.containsKey(logger.getName())) {
                    addLocalLogger(logger, LogManager.$assertionsDisabled);
                }
            }
        }

        boolean addLocalLogger(Logger logger) {
            return addLocalLogger(logger, requiresDefaultLoggers());
        }

        boolean addLocalLogger(Logger logger, LogManager logManager) {
            return addLocalLogger(logger, requiresDefaultLoggers(), logManager);
        }

        boolean addLocalLogger(Logger logger, boolean z) {
            return addLocalLogger(logger, z, LogManager.manager);
        }

        synchronized boolean addLocalLogger(Logger logger, boolean z, LogManager logManager) {
            if (z) {
                try {
                    ensureAllDefaultLoggers(logger);
                } catch (Throwable th) {
                    throw th;
                }
            }
            String name = logger.getName();
            if (name == null) {
                throw new NullPointerException();
            }
            LoggerWeakRef loggerWeakRef = this.namedLoggers.get(name);
            if (loggerWeakRef != null) {
                if (loggerWeakRef.get() == null) {
                    loggerWeakRef.dispose();
                } else {
                    return LogManager.$assertionsDisabled;
                }
            }
            LogManager owner = getOwner();
            logger.setLogManager(owner);
            Objects.requireNonNull(owner);
            LoggerWeakRef loggerWeakRef2 = owner.new LoggerWeakRef(logger);
            this.namedLoggers.put(name, loggerWeakRef2);
            Logger logger2 = null;
            Level levelProperty = owner.getLevelProperty(name + ".level", null);
            if (levelProperty != null && !logger.isLevelInitialized()) {
                LogManager.doSetLevel(logger, levelProperty);
            }
            processParentHandlers(logger, name);
            LogNode node = getNode(name);
            node.loggerRef = loggerWeakRef2;
            for (LogNode logNode = node.parent; logNode != null; logNode = logNode.parent) {
                LoggerWeakRef loggerWeakRef3 = logNode.loggerRef;
                if (loggerWeakRef3 != null && (logger2 = loggerWeakRef3.get()) != null) {
                    break;
                }
            }
            if (logger2 != null) {
                LogManager.doSetParent(logger, logger2);
            }
            node.walkAndSetParent(logger);
            loggerWeakRef2.setNode(node);
            return true;
        }

        synchronized void removeLoggerRef(String str, LoggerWeakRef loggerWeakRef) {
            this.namedLoggers.remove(str, loggerWeakRef);
        }

        synchronized Enumeration<String> getLoggerNames() {
            ensureInitialized();
            return this.namedLoggers.keys();
        }

        private void processParentHandlers(final Logger logger, final String str) {
            final LogManager owner = getOwner();
            AccessController.doPrivileged(new PrivilegedAction<Void>() {
                @Override
                public Void run() {
                    if (logger != owner.rootLogger) {
                        if (!owner.getBooleanProperty(str + ".useParentHandlers", true)) {
                            logger.setUseParentHandlers(LogManager.$assertionsDisabled);
                            return null;
                        }
                        return null;
                    }
                    return null;
                }
            });
            int i = 1;
            while (true) {
                int iIndexOf = str.indexOf(".", i);
                if (iIndexOf >= 0) {
                    String strSubstring = str.substring(0, iIndexOf);
                    if (owner.getProperty(strSubstring + ".level") == null) {
                        if (owner.getProperty(strSubstring + ".handlers") != null) {
                            demandLogger(strSubstring, null);
                        }
                    }
                    i = iIndexOf + 1;
                } else {
                    return;
                }
            }
        }

        LogNode getNode(String str) {
            String strSubstring;
            if (str == null || str.equals("")) {
                return this.root;
            }
            LogNode logNode = this.root;
            while (str.length() > 0) {
                int iIndexOf = str.indexOf(".");
                if (iIndexOf > 0) {
                    String strSubstring2 = str.substring(0, iIndexOf);
                    strSubstring = str.substring(iIndexOf + 1);
                    str = strSubstring2;
                } else {
                    strSubstring = "";
                }
                if (logNode.children == null) {
                    logNode.children = new HashMap<>();
                }
                LogNode logNode2 = logNode.children.get(str);
                if (logNode2 == null) {
                    logNode2 = new LogNode(logNode, this);
                    logNode.children.put(str, logNode2);
                }
                logNode = logNode2;
                str = strSubstring;
            }
            return logNode;
        }
    }

    final class SystemLoggerContext extends LoggerContext {
        SystemLoggerContext() {
            super();
        }

        @Override
        Logger demandLogger(String str, String str2) {
            Logger loggerFindLogger = findLogger(str);
            if (loggerFindLogger == null) {
                Logger logger = new Logger(str, str2, null, getOwner(), true);
                do {
                    if (!addLocalLogger(logger)) {
                        loggerFindLogger = findLogger(str);
                    } else {
                        loggerFindLogger = logger;
                    }
                } while (loggerFindLogger == null);
            }
            return loggerFindLogger;
        }
    }

    private void loadLoggerHandlers(final Logger logger, String str, final String str2) {
        AccessController.doPrivileged(new PrivilegedAction<Object>() {
            @Override
            public Object run() {
                for (String str3 : LogManager.this.parseClassNames(str2)) {
                    try {
                        Handler handler = (Handler) LogManager.getClassInstance(str3).newInstance();
                        String property = LogManager.this.getProperty(str3 + ".level");
                        if (property != null) {
                            Level levelFindLevel = Level.findLevel(property);
                            if (levelFindLevel != null) {
                                handler.setLevel(levelFindLevel);
                            } else {
                                System.err.println("Can't set level for " + str3);
                            }
                        }
                        logger.addHandler(handler);
                    } catch (Exception e) {
                        System.err.println("Can't load log handler \"" + str3 + "\"");
                        System.err.println("" + ((Object) e));
                        e.printStackTrace();
                    }
                }
                return null;
            }
        });
    }

    final class LoggerWeakRef extends WeakReference<Logger> {
        private boolean disposed;
        private String name;
        private LogNode node;
        private WeakReference<Logger> parentRef;

        LoggerWeakRef(Logger logger) {
            super(logger, LogManager.this.loggerRefQueue);
            this.disposed = LogManager.$assertionsDisabled;
            this.name = logger.getName();
        }

        void dispose() {
            synchronized (this) {
                if (this.disposed) {
                    return;
                }
                this.disposed = true;
                LogNode logNode = this.node;
                if (logNode != null) {
                    synchronized (logNode.context) {
                        logNode.context.removeLoggerRef(this.name, this);
                        this.name = null;
                        if (logNode.loggerRef == this) {
                            logNode.loggerRef = null;
                        }
                        this.node = null;
                    }
                }
                if (this.parentRef != null) {
                    Logger logger = this.parentRef.get();
                    if (logger != null) {
                        logger.removeChildLogger(this);
                    }
                    this.parentRef = null;
                }
            }
        }

        void setNode(LogNode logNode) {
            this.node = logNode;
        }

        void setParentRef(WeakReference<Logger> weakReference) {
            this.parentRef = weakReference;
        }
    }

    final void drainLoggerRefQueueBounded() {
        LoggerWeakRef loggerWeakRef;
        for (int i = 0; i < 400 && this.loggerRefQueue != null && (loggerWeakRef = (LoggerWeakRef) this.loggerRefQueue.poll()) != null; i++) {
            loggerWeakRef.dispose();
        }
    }

    public boolean addLogger(Logger logger) {
        String name = logger.getName();
        if (name == null) {
            throw new NullPointerException();
        }
        drainLoggerRefQueueBounded();
        if (getUserContext().addLocalLogger(logger, this)) {
            loadLoggerHandlers(logger, name, name + ".handlers");
            return true;
        }
        return $assertionsDisabled;
    }

    private static void doSetLevel(final Logger logger, final Level level) {
        if (System.getSecurityManager() == null) {
            logger.setLevel(level);
        } else {
            AccessController.doPrivileged(new PrivilegedAction<Object>() {
                @Override
                public Object run() {
                    logger.setLevel(level);
                    return null;
                }
            });
        }
    }

    private static void doSetParent(final Logger logger, final Logger logger2) {
        if (System.getSecurityManager() == null) {
            logger.setParent(logger2);
        } else {
            AccessController.doPrivileged(new PrivilegedAction<Object>() {
                @Override
                public Object run() {
                    logger.setParent(logger2);
                    return null;
                }
            });
        }
    }

    public Logger getLogger(String str) {
        return getUserContext().findLogger(str);
    }

    public Enumeration<String> getLoggerNames() {
        return getUserContext().getLoggerNames();
    }

    public void readConfiguration() throws Exception {
        InputStream resourceAsStream;
        checkPermission();
        String property = System.getProperty("java.util.logging.config.class");
        if (property != null) {
            try {
                getClassInstance(property).newInstance();
                return;
            } catch (Exception e) {
                System.err.println("Logging configuration class \"" + property + "\" failed");
                System.err.println("" + ((Object) e));
            }
        }
        String property2 = System.getProperty("java.util.logging.config.file");
        if (property2 == null) {
            String property3 = System.getProperty("java.home");
            if (property3 == null) {
                throw new Error("Can't find java.home ??");
            }
            property2 = new File(new File(property3, "lib"), "logging.properties").getCanonicalPath();
        }
        try {
            resourceAsStream = new FileInputStream(property2);
        } catch (Exception e2) {
            resourceAsStream = LogManager.class.getResourceAsStream("logging.properties");
            if (resourceAsStream == null) {
                throw e2;
            }
        }
        try {
            readConfiguration(new BufferedInputStream(resourceAsStream));
        } finally {
            if (resourceAsStream != null) {
                resourceAsStream.close();
            }
        }
    }

    public void reset() throws SecurityException {
        checkPermission();
        synchronized (this) {
            this.props = new Properties();
            this.initializedGlobalHandlers = true;
        }
        for (LoggerContext loggerContext : contexts()) {
            Enumeration<String> loggerNames = loggerContext.getLoggerNames();
            while (loggerNames.hasMoreElements()) {
                Logger loggerFindLogger = loggerContext.findLogger(loggerNames.nextElement());
                if (loggerFindLogger != null) {
                    resetLogger(loggerFindLogger);
                }
            }
        }
    }

    private void resetLogger(Logger logger) {
        for (Handler handler : logger.getHandlers()) {
            logger.removeHandler(handler);
            try {
                handler.close();
            } catch (Exception e) {
            }
        }
        String name = logger.getName();
        if (name != null && name.equals("")) {
            logger.setLevel(defaultLevel);
        } else {
            logger.setLevel(null);
        }
    }

    private String[] parseClassNames(String str) {
        String property = getProperty(str);
        int i = 0;
        if (property == null) {
            return new String[0];
        }
        String strTrim = property.trim();
        ArrayList arrayList = new ArrayList();
        while (i < strTrim.length()) {
            int i2 = i;
            while (i2 < strTrim.length() && !Character.isWhitespace(strTrim.charAt(i2)) && strTrim.charAt(i2) != ',') {
                i2++;
            }
            String strSubstring = strTrim.substring(i, i2);
            int i3 = i2 + 1;
            String strTrim2 = strSubstring.trim();
            if (strTrim2.length() != 0) {
                arrayList.add(strTrim2);
            }
            i = i3;
        }
        return (String[]) arrayList.toArray(new String[arrayList.size()]);
    }

    public void readConfiguration(InputStream inputStream) throws IOException, SecurityException {
        HashMap map;
        checkPermission();
        reset();
        this.props.load(inputStream);
        for (String str : parseClassNames("config")) {
            try {
                getClassInstance(str).newInstance();
            } catch (Exception e) {
                System.err.println("Can't load config class \"" + str + "\"");
                System.err.println("" + ((Object) e));
            }
        }
        setLevelsOnExistingLoggers();
        synchronized (this.listenerMap) {
            if (!this.listenerMap.isEmpty()) {
                map = new HashMap(this.listenerMap);
            } else {
                map = null;
            }
        }
        if (map != null) {
            Object objNewPropertyChangeEvent = Beans.newPropertyChangeEvent(LogManager.class, null, null, null);
            Iterator it = map.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry entry = (Map.Entry) it.next();
                Object key = entry.getKey();
                int iIntValue = ((Integer) entry.getValue()).intValue();
                for (int i = 0; i < iIntValue; i++) {
                    Beans.invokePropertyChange(key, objNewPropertyChangeEvent);
                }
            }
        }
        synchronized (this) {
            this.initializedGlobalHandlers = $assertionsDisabled;
        }
    }

    public String getProperty(String str) {
        return this.props.getProperty(str);
    }

    String getStringProperty(String str, String str2) {
        String property = getProperty(str);
        if (property == null) {
            return str2;
        }
        return property.trim();
    }

    int getIntProperty(String str, int i) {
        String property = getProperty(str);
        if (property == null) {
            return i;
        }
        try {
            return Integer.parseInt(property.trim());
        } catch (Exception e) {
            return i;
        }
    }

    boolean getBooleanProperty(String str, boolean z) {
        String property = getProperty(str);
        if (property == null) {
            return z;
        }
        String lowerCase = property.toLowerCase();
        if (lowerCase.equals("true") || lowerCase.equals("1")) {
            return true;
        }
        if (lowerCase.equals("false") || lowerCase.equals("0")) {
            return $assertionsDisabled;
        }
        return z;
    }

    Level getLevelProperty(String str, Level level) {
        String property = getProperty(str);
        if (property == null) {
            return level;
        }
        Level levelFindLevel = Level.findLevel(property.trim());
        return levelFindLevel != null ? levelFindLevel : level;
    }

    Filter getFilterProperty(String str, Filter filter) {
        String property = getProperty(str);
        if (property != null) {
            try {
                return (Filter) getClassInstance(property).newInstance();
            } catch (Exception e) {
            }
        }
        return filter;
    }

    Formatter getFormatterProperty(String str, Formatter formatter) {
        String property = getProperty(str);
        if (property != null) {
            try {
                return (Formatter) getClassInstance(property).newInstance();
            } catch (Exception e) {
            }
        }
        return formatter;
    }

    private synchronized void initializeGlobalHandlers() {
        if (this.initializedGlobalHandlers) {
            return;
        }
        this.initializedGlobalHandlers = true;
        if (this.deathImminent) {
            return;
        }
        loadLoggerHandlers(this.rootLogger, null, "handlers");
    }

    void checkPermission() {
        SecurityManager securityManager = System.getSecurityManager();
        if (securityManager != null) {
            securityManager.checkPermission(this.controlPermission);
        }
    }

    public void checkAccess() throws SecurityException {
        checkPermission();
    }

    private static class LogNode {
        HashMap<String, LogNode> children;
        final LoggerContext context;
        LoggerWeakRef loggerRef;
        LogNode parent;

        LogNode(LogNode logNode, LoggerContext loggerContext) {
            this.parent = logNode;
            this.context = loggerContext;
        }

        void walkAndSetParent(Logger logger) {
            if (this.children == null) {
                return;
            }
            for (LogNode logNode : this.children.values()) {
                LoggerWeakRef loggerWeakRef = logNode.loggerRef;
                Logger logger2 = loggerWeakRef == null ? null : loggerWeakRef.get();
                if (logger2 != null) {
                    LogManager.doSetParent(logger2, logger);
                } else {
                    logNode.walkAndSetParent(logger);
                }
            }
        }
    }

    private final class RootLogger extends Logger {
        private RootLogger() {
            super("", null, null, LogManager.this, true);
        }

        @Override
        public void log(LogRecord logRecord) {
            LogManager.this.initializeGlobalHandlers();
            super.log(logRecord);
        }

        @Override
        public void addHandler(Handler handler) {
            LogManager.this.initializeGlobalHandlers();
            super.addHandler(handler);
        }

        @Override
        public void removeHandler(Handler handler) {
            LogManager.this.initializeGlobalHandlers();
            super.removeHandler(handler);
        }

        @Override
        Handler[] accessCheckedHandlers() {
            LogManager.this.initializeGlobalHandlers();
            return super.accessCheckedHandlers();
        }
    }

    private synchronized void setLevelsOnExistingLoggers() {
        Enumeration<?> enumerationPropertyNames = this.props.propertyNames();
        while (enumerationPropertyNames.hasMoreElements()) {
            String str = (String) enumerationPropertyNames.nextElement();
            if (str.endsWith(".level")) {
                String strSubstring = str.substring(0, str.length() - 6);
                Level levelProperty = getLevelProperty(str, null);
                if (levelProperty == null) {
                    System.err.println("Bad level value for property: " + str);
                } else {
                    Iterator<LoggerContext> it = contexts().iterator();
                    while (it.hasNext()) {
                        Logger loggerFindLogger = it.next().findLogger(strSubstring);
                        if (loggerFindLogger != null) {
                            loggerFindLogger.setLevel(levelProperty);
                        }
                    }
                }
            }
        }
    }

    public static synchronized LoggingMXBean getLoggingMXBean() {
        if (loggingMXBean == null) {
            loggingMXBean = new Logging();
        }
        return loggingMXBean;
    }

    private static class Beans {
        private static final Class<?> propertyChangeListenerClass = getClass("java.beans.PropertyChangeListener");
        private static final Class<?> propertyChangeEventClass = getClass("java.beans.PropertyChangeEvent");
        private static final Method propertyChangeMethod = getMethod(propertyChangeListenerClass, "propertyChange", propertyChangeEventClass);
        private static final Constructor<?> propertyEventCtor = getConstructor(propertyChangeEventClass, Object.class, String.class, Object.class, Object.class);

        private Beans() {
        }

        private static Class<?> getClass(String str) {
            try {
                return Class.forName(str, true, Beans.class.getClassLoader());
            } catch (ClassNotFoundException e) {
                return null;
            }
        }

        private static Constructor<?> getConstructor(Class<?> cls, Class<?>... clsArr) {
            if (cls == null) {
                return null;
            }
            try {
                return cls.getDeclaredConstructor(clsArr);
            } catch (NoSuchMethodException e) {
                throw new AssertionError(e);
            }
        }

        private static Method getMethod(Class<?> cls, String str, Class<?>... clsArr) {
            if (cls == null) {
                return null;
            }
            try {
                return cls.getMethod(str, clsArr);
            } catch (NoSuchMethodException e) {
                throw new AssertionError(e);
            }
        }

        static boolean isBeansPresent() {
            if (propertyChangeListenerClass == null || propertyChangeEventClass == null) {
                return LogManager.$assertionsDisabled;
            }
            return true;
        }

        static Object newPropertyChangeEvent(Object obj, String str, Object obj2, Object obj3) {
            try {
                return propertyEventCtor.newInstance(obj, str, obj2, obj3);
            } catch (IllegalAccessException | InstantiationException e) {
                throw new AssertionError(e);
            } catch (InvocationTargetException e2) {
                Throwable cause = e2.getCause();
                if (cause instanceof Error) {
                    throw ((Error) cause);
                }
                if (cause instanceof RuntimeException) {
                    throw ((RuntimeException) cause);
                }
                throw new AssertionError(e2);
            }
        }

        static void invokePropertyChange(Object obj, Object obj2) {
            try {
                propertyChangeMethod.invoke(obj, obj2);
            } catch (IllegalAccessException e) {
                throw new AssertionError(e);
            } catch (InvocationTargetException e2) {
                Throwable cause = e2.getCause();
                if (cause instanceof Error) {
                    throw ((Error) cause);
                }
                if (cause instanceof RuntimeException) {
                    throw ((RuntimeException) cause);
                }
                throw new AssertionError(e2);
            }
        }
    }
}
